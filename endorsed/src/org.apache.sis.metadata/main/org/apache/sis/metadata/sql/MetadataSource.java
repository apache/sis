/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.metadata.sql;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.io.IOException;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.sql.SQLNonTransientException;
import java.sql.PreparedStatement;
import org.opengis.annotation.UML;
import org.opengis.util.CodeList;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.system.Modules;
import org.apache.sis.system.SystemListener;
import org.apache.sis.system.DelayedExecutor;
import org.apache.sis.system.DelayedRunnable;
import org.apache.sis.system.Configuration;
import org.apache.sis.system.Loggers;
import org.apache.sis.metadata.sql.internal.shared.Initializer;
import org.apache.sis.metadata.sql.internal.shared.Reflection;
import org.apache.sis.metadata.sql.internal.shared.SQLBuilder;
import org.apache.sis.metadata.internal.shared.ReferencingServices;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Classes;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.internal.shared.CodeLists;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.Types;

// Specific to the main branch:
import org.apache.sis.pending.geoapi.evolution.Interim;


/**
 * A connection to a metadata database in read-only mode. It can be either the database
 * {@linkplain #getProvided() provided by Apache SIS} with predefined ISO 19115 metadata,
 * or another database specified at construction time.
 * Metadata instances can be obtained as in the example below:
 *
 * {@snippet lang="java" :
 *     MetadataSource metadata = MetadataSource.getProvided();
 *     Format format = source.lookup(Format.class, "PNG");
 *     }
 *
 * where {@code id} is the primary key value for the desired record in the {@code Format} table.
 *
 * <h2>Properties</h2>
 * The constructor expects three Java arguments (the {@linkplain MetadataStandard metadata standard},
 * the {@linkplain DataSource data source} and the database schema) completed by an arbitrary amount
 * of optional arguments given as a map of properties.
 * The following keys are recognized by {@code MetadataSource} and all other entries are ignored:
 *
 * <table class="sis">
 *   <caption>Optional properties at construction time</caption>
 *   <tr><th>Key</th>                     <th>Value type</th>          <th>Description</th></tr>
 *   <tr><td>{@code "catalog"}</td>       <td>{@link String}</td>      <td>The database catalog where the metadata schema is stored.</td></tr>
 *   <tr><td>{@code "classloader"}</td>   <td>{@link ClassLoader}</td> <td>The class loader to use for creating {@link Proxy} instances.</td></tr>
 *   <tr><td>{@code "maxStatements"}</td> <td>{@link Integer}</td>     <td>Maximal number of {@link PreparedStatement}s that can be kept simultaneously open.</td></tr>
 * </table>
 *
 * <h2>Concurrency</h2>
 * {@code MetadataSource} is thread-safe but is not concurrent. If concurrency is desired,
 * multiple instances of {@code MetadataSource} can be created for the same {@link DataSource}.
 * The {@link #MetadataSource(MetadataSource)} convenience constructor can be used for this purpose.
 *
 * @author  Touraïvane (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 * @since   0.8
 */
public class MetadataSource implements AutoCloseable {
    /**
     * The policy for column names. We use UML identifiers (e.g. {@code "title"}) as defined by the metadata standard
     * (typically ISO 19115).
     */
    static final KeyNamePolicy NAME_POLICY = KeyNamePolicy.UML_IDENTIFIER;

    /**
     * The column name used for the identifiers.
     */
    static final String ID_COLUMN = "ID";

    /**
     * The timeout before to close a prepared statement, in nanoseconds. This is set to 2 seconds,
     * which is a bit short but should be okay if the {@link DataSource} creates pooled connections.
     * In case there is no connection pool, then the mechanism defined in this package will hopefully
     * keeps the performance at a reasonable level.
     *
     * @see #closeExpired()
     */
    @Configuration
    private static final long TIMEOUT = 2000_000000;

    /**
     * An extra delay to add to the {@link #TIMEOUT} in order to increase the chances to
     * close many statements at once.
     */
    @Configuration
    private static final int EXTRA_DELAY = 500_000000;

    /**
     * The metadata standard to be used for constructing the database schema.
     */
    protected final MetadataStandard standard;

    /**
     * The data source object for fetching the connection to the database.
     * This is specified at construction time.
     */
    private final DataSource dataSource;

    /**
     * The connection to the database, or {@code null} if not yet created or if closed.
     * This field is set to a non-null value when {@link #connection()} is invoked, then
     * closed and set to {@code null} after all {@linkplain #statements cached statements}
     * have been closed.
     *
     * @see #connection()
     */
    private Connection connection;

    /**
     * A pool of prepared statements with a maximal capacity equals to the array length.
     * The array length should be reasonably small. The array may contain null element anywhere.
     * Inactive statements are closed after some timeout.
     *
     * <div class="note"><b>Note:</b>
     * this array duplicates the work done by statement pools in modern JDBC drivers. Nevertheless
     * it still useful in our case since we retain some additional JDBC resources together with the
     * {@link PreparedStatement}, for example the {@link ResultSet} created from that statement.</div>
     *
     * Every access to this array <strong>must</strong> be synchronized on {@code MetadataSource.this}.
     * Execution of a prepared statement may also need to be done inside the synchronized block,
     * because a single JDBC connection cannot be assumed thread-safe.
     *
     * <p>Usage example:</p>
     * {@snippet lang="java" :
     *     Class<?> type = …;
     *     synchronized (this) {
     *         // Get an entry, or create a new one if no entry is available.
     *         CachedStatement statement = take(type, preferredIndex);
     *         if (statement == null) {
     *             statement = new CachedStatement(someStatement);
     *         }
     *         // Use the statement and give it back to the pool once we are done.
     *         // We do not put it back in case of SQLException.
     *         Object value = statement.getValue(…);
     *         preferredIndex = recycle(statement, preferredIndex);
     *     }
     *     }
     *
     * @see #prepareStatement(Class, String, int)
     * @see #recycle(CachedStatement, int)
     */
    private final CachedStatement[] statements;

    /**
     * The catalog, or {@code null} if none.
     */
    final String catalog;

    /**
     * The database schema where metadata are stored, or {@code null} if none. In the metadata source
     * {@linkplain #getProvided() provided by SIS}, this is {@code "metadata"} or {@code "METADATA"},
     * depending on the database convention regarding lower case / upper case identifiers.
     *
     * <p>Consider this field as final. This field is modified only by {@link #install()} for taking
     * in account the lower case or upper case convention of the database engine.</p>
     */
    private String schema;

    /**
     * Whether the {@link #helper} should quote schemas in SQL statements. A value of {@code false} let the database
     * engine chooses its own lower case / upper case policy. This flag is {@code true} if the schema was specified
     * by the user, or {@code false} if using the metadata {@linkplain #getProvided() provided by SIS}.
     *
     * <p>Consider this field as final. This field is modified only by {@link #install()}.</p>
     *
     * @see #helper()
     */
    private boolean quoteSchema;

    /**
     * A helper class used for constructing SQL statements. This helper is created when first needed,
     * then kept until the connection is closed.
     *
     * @see #helper()
     */
    private transient SQLBuilder helper;

    /**
     * All columns found in tables that have been queried or created up to date.
     * Keys are table names and values are the columns defined for that table.
     *
     * @see #getExistingColumns(String)
     */
    private final Map<String, Set<String>> tableColumns;

    /**
     * The class loader to use for creating {@link Proxy} instances.
     *
     * @see #lookup(Class, String)
     */
    private final ClassLoader classloader;

    /**
     * The objects which have been created by a previous call to {@link #lookup(Class, String)}.
     * Used in order to share existing instances for the same interface and primary key.
     *
     * @see #lookup(Class, String)
     */
    private final WeakValueHashMap<CacheKey,Object> pool;

    /**
     * Some information about last used objects. Cached on assumption that the same information
     * will be used more than once before to move to another metadata object.
     */
    private final ThreadLocal<LookupInfo> lastUsed;

    /**
     * Where to report the warnings before to eventually log them.
     *
     * @see #readColumn(LookupInfo, Method, Dispatcher)
     */
    private volatile Filter logFilter;

    /**
     * Whether at least one {@link CloseTask} is scheduled for execution.
     *
     * @see #scheduleCloseTask()
     */
    private boolean isCloseScheduled;

    /**
     * The instance connected to the {@code "jdbc/SpatialMetadata"} database,
     * created when first needed and cleared when the module path change.
     * May be {@link MetadataFallback#INSTANCE} if we failed to establish
     * a connection to the database for a non-transient reason.
     */
    private static MetadataSource instance;
    static {
        SystemListener.add(new SystemListener(Modules.METADATA) {
            @Override protected void classpathChanged() {
                synchronized (MetadataSource.class) {
                    instance = null;
                }
            }
        });
    }

    /**
     * Returns the metadata source connected to the {@code "jdbc/SpatialMetadata"} database.
     * In a default Apache SIS installation, this metadata source contains predefined records
     * for some commonly used {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation
     * citations} and {@linkplain org.apache.sis.metadata.iso.distribution.DefaultFormat formats}
     * among others.
     *
     * <p>If connection to the metadata database cannot be established, then this method returns
     * a fallback with a few hard-coded values.</p>
     *
     * @return source of predefined metadata records from the {@code "jdbc/SpatialMetadata"} database.
     */
    public static synchronized MetadataSource getProvided() {
        MetadataSource ms = instance;
        if (ms == null) {
            LogRecord warning = null;
            boolean isTransient = false;
            try {
                final DataSource dataSource = Initializer.getDataSource();
                if (dataSource != null) {
                    ms = new MetadataSource(MetadataStandard.ISO_19115, dataSource, "metadata", null);
                    ms.install();
                } else {
                    warning = (LogRecord) Initializer.unspecified(null, true);
                    ms = MetadataFallback.INSTANCE;
                }
            } catch (Exception e) {
                ms = MetadataFallback.INSTANCE;
                /*
                 * Derby sometimes wraps SQLException into another SQLException.  For making the stack strace a
                 * little bit simpler, keep only the root cause provided that the exception type is compatible.
                 * If the Derby driver was not found at all, reduce the logging level since Derby is optional.
                 */
                warning = Errors.forLocale(null).createLogRecord(Level.WARNING, Errors.Keys.CanNotConnectTo_1, Initializer.JNDI);
                warning.setThrown(Exceptions.unwrap(e));
                if (e instanceof ClassNotFoundException) {
                    warning.setLevel(Level.CONFIG);                         // Derby driver not on the module path.
                }
                /*
                 * If the error is transient or has a transient cause, we will not save MetadataFallback.INSTANCE
                 * in the `instance` field. The intent is to try again next time this method will be invoked, in
                 * case the transient error has disappeared.
                 */
                for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                    if (cause instanceof SQLTransientException) {
                        isTransient = true;
                        break;
                    }
                }
            }
            if (warning != null) {
                Logging.completeAndLog(SystemListener.LOGGER, MetadataSource.class, "getProvided", warning);
            }
            if (!isTransient) {
                instance = ms;
            }
        }
        return ms;
    }

    /**
     * Creates a new metadata source. The metadata standard to implement (typically
     * {@linkplain MetadataStandard#ISO_19115 ISO 19115}, but not necessarily) and
     * the database source are mandatory information.
     * All other information are optional and can be {@code null}.
     *
     * @param  standard    the metadata standard to implement.
     * @param  dataSource  the source for getting a connection to the database.
     * @param  schema      the database schema were metadata tables are stored, or {@code null} if none.
     * @param  properties  additional options, or {@code null} if none. See class javadoc for a description.
     */
    public MetadataSource(final MetadataStandard standard, final DataSource dataSource,
            final String schema, final Map<String,?> properties)
    {
        ArgumentChecks.ensureNonNull("standard",   standard);
        ArgumentChecks.ensureNonNull("dataSource", dataSource);
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        ClassLoader classloader;
        Integer maxStatements;

        catalog       = Containers.property(properties, "catalog",       String.class);
        classloader   = Containers.property(properties, "classloader",   ClassLoader.class);
        maxStatements = Containers.property(properties, "maxStatements", Integer.class);
        if (classloader == null) {
            classloader = getClass().getClassLoader();
        }
        if (maxStatements == null) {
            maxStatements = 10;               // Default value, may change in any future Apache SIS version.
        } else {
            ArgumentChecks.ensureBetween("maxStatements", 2, 0xFF, maxStatements);   // Unsigned byte range.
        }
        this.standard     = standard;
        this.dataSource   = dataSource;
        this.schema       = schema;
        this.quoteSchema  = true;
        this.classloader  = classloader;
        this.statements   = new CachedStatement[maxStatements - 1];
        this.tableColumns = new HashMap<>();
        this.pool         = new WeakValueHashMap<>(CacheKey.class);
        this.lastUsed     = ThreadLocal.withInitial(LookupInfo::new);
    }

    /**
     * Creates a new metadata source with the same configuration as the given source.
     * The two {@code MetadataSource} instances will share the same {@code DataSource}
     * but will use their own {@link Connection}.
     * This constructor is useful when concurrency is desired.
     *
     * <p>The new {@code MetadataSource} initially contains the {@linkplain #setWarningFilter warning filter}
     * declared in the given {@code source}.</p>
     *
     * @param  source  the source from which to copy the configuration.
     */
    public MetadataSource(final MetadataSource source) {
        standard     = source.standard;
        dataSource   = source.dataSource;
        catalog      = source.catalog;
        schema       = source.schema;
        quoteSchema  = source.quoteSchema;
        statements   = new CachedStatement[source.statements.length];
        tableColumns = new HashMap<>();
        classloader  = source.classloader;
        pool         = source.pool;
        lastUsed     = source.lastUsed;
        logFilter    = source.logFilter;
    }

    /**
     * For {@link MetadataFallback} constructor only.
     */
    MetadataSource() {
        standard     = MetadataStandard.ISO_19115;
        dataSource   = null;
        catalog      = null;
        statements   = null;
        tableColumns = null;
        classloader  = getClass().getClassLoader();
        pool         = null;
        lastUsed     = null;
    }

    /**
     * If the metadata schema does not exist in the database, creates it and inserts the predefined metadata values.
     * The current implementation has the following restrictions:
     *
     * <ul>
     *   <li>Metadata standard must be {@link MetadataStandard#ISO_19115} or compatible.</li>
     *   <li>The schema name must be {@code "metadata"}, as this is the name used unquoted in SQL scripts.</li>
     * </ul>
     *
     * Maintenance note: this method is invoked by reflection in the {@code org.apache.sis.referencing.database} module.
     * If we make this method public in a future Apache SIS version, then we can remove the reflection code.
     *
     * @throws SQLException if an error occurred while inserting the metadata.
     */
    final synchronized void install() throws IOException, SQLException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Connection connection = connection();
        final DatabaseMetaData md = connection.getMetaData();
        if (md.storesUpperCaseIdentifiers()) {
            schema = schema.toUpperCase(Locale.US);
        } else if (md.storesLowerCaseIdentifiers()) {
            schema = schema.toLowerCase(Locale.US);
        }
        quoteSchema = false;
        try (ResultSet result = md.getTables(catalog, schema, "Citation", null)) {
            if (result.next()) {
                return;
            }
        }
        final Installer installer = new Installer(connection);
        installer.run();
    }

    /**
     * Returns the connection to the database, creating a new one if needed. This method shall
     * be invoked inside a synchronized block wider than just the scope of this method in order
     * to ensure that the connection is used by only one thread at time. This is also necessary
     * for preventing the background thread to close the connection too early.
     *
     * <p>Callers shall not close the connection returned by this method.
     * The connection will be closed by {@link #closeExpired()} after an arbitrary timeout.</p>
     *
     * @return the connection to the database.
     * @throws SQLException if an error occurred while fetching the connection.
     */
    final Connection connection() throws SQLException {
        assert Thread.holdsLock(this);
        Connection c = connection;
        if (c == null) {
            connection = c = dataSource.getConnection();
            Initializer.connected(c.getMetaData(), MetadataSource.class, "lookup");
            scheduleCloseTask();
        }
        return c;
    }

    /**
     * Returns the database schema where metadata are stored, or {@code null} if none.
     */
    final String schema() {
        return schema;
    }

    /**
     * Returns a helper class for building SQL statements.
     */
    final SQLBuilder helper() throws SQLException {
        assert Thread.holdsLock(this);
        if (helper == null) {
            helper = new SQLBuilder(connection().getMetaData(), quoteSchema);
        }
        return helper;
    }

    /**
     * Returns a statement that can be reused for performing queries on the table for the specified interface.
     * Callers must invoke this method in a block synchronized on {@code this}.
     *
     * @param  type            the interface for which to reuse a prepared statement.
     * @param  tableName       value of {@code getTableName(type)}, or {@code null} for computing by this method.
     * @param  preferredIndex  index in the cache array where to search first. This is only a hint for increasing
     *         the chances to find quickly a {@code CachedStatement} instance for the right type and identifier.
     */
    private CachedStatement prepareStatement(final Class<?> type, String tableName, final int preferredIndex)
            throws SQLException
    {
        assert Thread.holdsLock(this);
        if (preferredIndex >= 0 && preferredIndex < statements.length) {
            final CachedStatement statement = statements[preferredIndex];
            if (statement != null && statement.type == type) {
                statements[preferredIndex] = null;
                return statement;
            }
        }
        for (int i=0; i < statements.length; i++) {
            final CachedStatement statement = statements[i];
            if (statement != null && statement.type == type) {
                statements[i] = null;
                return statement;
            }
        }
        if (tableName == null) {
            tableName = getTableName(type);
        }
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final SQLBuilder helper = helper();
        final String query = helper.clear().append("SELECT * FROM ")
                .appendIdentifier(schema, tableName).append(" WHERE ")
                .appendIdentifier(ID_COLUMN).append("=?").toString();
        return new CachedStatement(type, connection().prepareStatement(query), logFilter);
    }

    /**
     * Flags the given {@code CachedStatement} as available for reuse.
     *
     * @param  statement       the prepared statement to cache.
     * @param  preferredIndex  index in the cache array to use if the corresponding slot is available.
     * @return index in the cache array where the result has been actually stored.
     */
    private int recycle(final CachedStatement statement, int preferredIndex) throws SQLException {
        assert Thread.holdsLock(this);
        final long currentTime = System.nanoTime();
        if (preferredIndex < 0 || preferredIndex >= statements.length || statements[preferredIndex] != null) {
            preferredIndex = 0;
            while (statements[preferredIndex] != null) {
                if (++preferredIndex >= statements.length) {
                    /*
                     * If we reach this point, this means that the `statements` pool has reached its maximal capacity.
                     * Loop again on all statements in order to find the oldest one. We will close that old statement
                     * and cache the given one instead.
                     */
                    long oldest = Long.MIN_VALUE;
                    for (int i=0; i < statements.length; i++) {
                        final long age = currentTime - statements[i].expireTime;
                        if (age >= oldest) {
                            oldest = age;
                            preferredIndex = i;
                        }
                    }
                    statements[preferredIndex].close();
                    break;
                }
            }
        }
        statements[preferredIndex] = statement;
        statement.expireTime = currentTime + TIMEOUT;
        scheduleCloseTask();
        return preferredIndex;
    }

    /**
     * Returns the table name for the specified class. This is usually the ISO 19115 name,
     * but we fallback on the simple class name if the ISO name is not available.
     * The package prefix is omitted (e.g. {@code "CI_"} in {@code "CI_Citation"}
     * since newer ISO standards tend to drop it.
     */
    static String getTableName(final Class<?> type) {
        final UML annotation = type.getAnnotation(UML.class);
        if (annotation == null) {
            return type.getSimpleName();
        }
        final String name = annotation.identifier();
        int s = name.lastIndexOf('.') + 1;
        /*
         * Drop package prefix if present (e.g. "CI_" in "CI_Citation").
         */
        if (name.length() > s+3 && name.charAt(s+2) == '_' && Character.isUpperCase(name.charAt(1))) {
            s += 3;
        }
        return name.substring(s);
    }

    /**
     * If the given metadata is a proxy generated by this {@code MetadataSource}, returns the
     * identifier of that proxy. Such metadata do not need to be inserted again in the database.
     *
     * @param  metadata  the metadata to test.
     * @return the identifier (primary key), or {@code null} if the given metadata is not a proxy.
     */
    final String proxy(final Object metadata) {
        return (metadata instanceof MetadataProxy) ? ((MetadataProxy) metadata).identifier(this) : null;
    }

    /**
     * Returns a view of the given metadata as a map. This method returns always a map using UML identifier
     * and containing all entries including the null ones because the {@code MetadataSource} implementation
     * assumes so.
     *
     * @param  metadata  the metadata object to view as a map.
     * @return a map view over the metadata object.
     * @throws ClassCastException if the metadata object does not implement a metadata interface
     *         of the expected package.
     */
    final Map<String,Object> asValueMap(final Object metadata) throws ClassCastException {
        return standard.asValueMap(metadata, null, NAME_POLICY, ValueExistencePolicy.ALL);
    }

    /**
     * If the given value is a collection, returns the first element in that collection
     * or {@code null} if empty.
     *
     * @param  value  the value to inspect (can be {@code null}).
     * @return the given value, or its first element if the value is a collection,
     *         or {@code null} if the given value is null or an empty collection.
     */
    static Object extractFromCollection(Object value) {
        while (value instanceof Iterable<?>) {
            final Iterator<?> it = ((Iterable<?>) value).iterator();
            if (!it.hasNext()) {
                return null;
            }
            if (value == (value = it.next())) break;
        }
        return value;
    }

    /**
     * Searches for the given metadata in the database. If such metadata is found, then its
     * identifier (primary key) is returned. Otherwise this method returns {@code null}.
     *
     * @param  metadata  the metadata to search for.
     * @return the identifier of the given metadata, or {@code null} if none.
     * @throws MetadataStoreException if the metadata object does not implement a metadata interface
     *         of the expected package, or if an error occurred while searching in the database.
     */
    public String search(final Object metadata) throws MetadataStoreException {
        String identifier = proxy(Objects.requireNonNull(metadata));
        if (identifier == null) {
            /*
             * Code lists do not need to be stored in the database. Some code list tables may
             * be present in the database in order to ensure foreigner key constraints, but
             * those tables are not used in any way by the org.apache.sis.metadata.sql package.
             */
            if (metadata instanceof CodeList<?>) {
                identifier = Types.getCodeName((CodeList<?>) metadata);
            } else if (metadata instanceof Enum<?>) {
                identifier = ((Enum<?>) metadata).name();
            } else {
                final String table;
                final Map<String,Object> asMap;
                try {
                    table = getTableName(standard.getInterface(metadata.getClass()));
                    asMap = asValueMap(metadata);
                } catch (ClassCastException e) {
                    throw new MetadataStoreException(Errors.format(
                            Errors.Keys.IllegalArgumentClass_2, "metadata", metadata.getClass()));
                }
                synchronized (this) {
                    try (Statement stmt = connection().createStatement()) {
                        identifier = search(table, null, asMap, stmt, helper());
                    } catch (SQLException e) {
                        throw new MetadataStoreException(e.getLocalizedMessage(), Exceptions.unwrap(e));
                    } catch (FactoryException e) {
                        throw new MetadataStoreException(e.getLocalizedMessage(), e);
                    }
                }
            }
        }
        return identifier;
    }

    /**
     * Searches for the given metadata in the database. If such metadata is found, then its
     * identifier (primary key) is returned. Otherwise this method returns {@code null}.
     *
     * @param  table     the table where to search.
     * @param  columns   the table columns as given by {@link #getExistingColumns(String)}, or {@code null}.
     * @param  metadata  a map view of the metadata to search for.
     * @param  stmt      the statement to use for executing the query.
     * @param  helper    a helper class for creating the SQL query.
     * @return the identifier of the given metadata, or {@code null} if none.
     * @throws SQLException if an error occurred while searching in the database.
     */
    final String search(final String table, Set<String> columns, final Map<String,Object> metadata,
            final Statement stmt, final SQLBuilder helper) throws SQLException, FactoryException
    {
        assert Thread.holdsLock(this);
        helper.clear();
        for (final Map.Entry<String,Object> entry : metadata.entrySet()) {
            /*
             * Gets the value and the column where this value is stored. If the value is non-null,
             * then the column must exist otherwise the metadata will be considered as not found.
             */
            Object value = extractFromCollection(entry.getValue());
            final String column = entry.getKey();
            if (columns == null) {
                columns = getExistingColumns(table);
            }
            if (!columns.contains(column)) {
                if (value != null) {
                    return null;            // The column was mandatory for the searched metadata.
                } else {
                    continue;               // Do not include a non-existent column in the SQL query.
                }
            }
            /*
             * Tests if the value is another metadata, in which case we will invoke this method recursively.
             * Note that if a metadata dependency is not found, we can stop the whole process immediately.
             */
            if (value != null) {
                if (value instanceof CodeList<?>) {
                    value = Types.getCodeName((CodeList<?>) value);
                } else if (value instanceof Enum<?>) {
                    value = ((Enum<?>) value).name();
                } else {
                    String dependency = proxy(value);
                    if (dependency != null) {
                        value = dependency;
                    } else {
                        final Class<?> type = value.getClass();
                        if (standard.isMetadata(type)) {
                            final var builder = new SQLBuilder(helper);
                            builder.setCatalogAndSchema(stmt.getConnection());
                            final String other = getTableName(standard.getInterface(type));
                            dependency = search(other, null, asValueMap(value), stmt, builder);
                            if (dependency == null) {
                                return null;                    // Dependency not found.
                            }
                            value = dependency;
                        }
                    }
                }
            }
            /*
             * Builds the SQL statement with the resolved value.
             */
            if (helper.isEmpty()) {
                helper.append(SQLBuilder.SELECT).appendIdentifier(ID_COLUMN).append(" FROM ")
                        .appendIdentifier(schema, table).append(" WHERE ");
            } else {
                helper.append(" AND ");
            }
            helper.appendIdentifier(column).appendEqualsValue(toStorableValue(value));
        }
        /*
         * The SQL statement is ready, with metadata dependency (if any) resolved. We can now execute it.
         * If more than one record is found, the identifier of the first one will be selected add a warning
         * will be logged.
         */
        String identifier = null;
        try (ResultSet rs = stmt.executeQuery(helper.toString())) {
            while (rs.next()) {
                final String candidate = rs.getString(1);
                if (candidate != null) {
                    if (identifier == null) {
                        identifier = candidate;
                    } else if (!identifier.equals(candidate)) {
                        warning(MetadataSource.class, "search", Errors.forLocale(null).createLogRecord(
                                Level.WARNING, Errors.Keys.DuplicatedElement_1, candidate));
                        break;
                    }
                }
            }
        }
        return identifier;
    }

    /**
     * Converts the given object to a value that can be stored in the database.
     *
     * @throws FactoryException if an error occurred while using the geodetic database.
     */
    static Object toStorableValue(Object value) throws FactoryException {
        if (value instanceof IdentifiedObject) {
            value = ReferencingServices.getInstance().getPreferredIdentifier((IdentifiedObject) value);
        }
        return value;
    }

    /**
     * Returns the set of all columns in a table, or an empty set if none (never {@code null}).
     * Because each table should have at least the {@value #ID_COLUMN} column, an empty set of
     * columns will be understood as meaning that the table does not exist.
     *
     * <p>This method returns a direct reference to the cached set. The returned set shall be
     * modified in-place if new columns are added in the database table.</p>
     *
     * @param  table  the name of the table for which to get the columns.
     * @return the set of columns, or an empty set if the table has not yet been created.
     * @throws SQLException if an error occurred while querying the database.
     */
    final Set<String> getExistingColumns(final String table) throws SQLException {
        assert Thread.holdsLock(this);
        Set<String> columns = tableColumns.get(table);
        if (columns == null) {
            columns = new HashSet<>();
            /*
             * Note: a null schema in the DatabaseMetadata.getColumns(…) call means "do not take schema in account";
             * it does not mean "no schema" (the latter is specified by an empty string). This match better what we
             * want because if we do not specify a schema in a SELECT statement, then the actual schema used depends
             * on the search path specified in the database environment variables.
             */
            final DatabaseMetaData md = connection().getMetaData();
            try (ResultSet rs = md.getColumns(catalog, schema, table, null)) {
                while (rs.next()) {
                    if (!columns.add(rs.getString(Reflection.COLUMN_NAME))) {
                        // Paranoiac check, but should never happen.
                        throw new SQLNonTransientException(table);
                    }
                }
            }
            tableColumns.put(table, columns);
        }
        return columns;
    }

    /**
     * Returns an implementation of the specified metadata interface filled with the data referenced
     * by the specified identifier. Alternatively, this method can also return a {@link CodeList} or
     * {@link Enum} element.
     *
     * @param  <T>         the parameterized type of the {@code type} argument.
     * @param  type        the interface to implement (e.g. {@link org.opengis.metadata.citation.Citation}),
     *                     or the {@code ControlledVocabulary} type ({@link CodeList} or some {@link Enum}).
     * @param  identifier  the identifier of the record for the metadata entity to be created.
     *                     This is usually the primary key of the record to search for.
     * @return an implementation of the required interface, or the code list element.
     * @throws MetadataStoreException if a SQL query failed or if the metadata has not been found.
     */
    public <T> T lookup(final Class<T> type, final String identifier) throws MetadataStoreException {
        ArgumentChecks.ensureNonNull("type", type);
        ArgumentChecks.ensureNonEmpty("identifier", identifier);
        return type.cast(lookup(type, identifier, true));
    }

    /**
     * Implementation of public {@link #lookup(Class, String)} method.
     *
     * <h4>Deferred database access</h4>
     * This method may or may not query the database immediately, at implementation choice.
     * It the database is not queried immediately, invalid identifiers may not be detected
     * during this method invocation. Instead, an invalid identifier may be detected only
     * when a getter method is invoked on the returned metadata object. In such case,
     * an {@link org.apache.sis.util.collection.BackingStoreException} will be thrown
     * at getter method invocation time.
     *
     * @param  type        the interface to implement or the {@link ControlledVocabulary} type.
     * @param  identifier  the identifier of the record for the metadata entity to be created.
     * @param  verify      whether to check for record existence.
     * @return an implementation of the required interface, or the code list element.
     * @throws MetadataStoreException if a SQL query failed or if the metadata has not been found.
     */
    private Object lookup(final Class<?> type, final String identifier, boolean verify) throws MetadataStoreException {
        Object value;
        if (CodeList.class.isAssignableFrom(type)) {
            try {
                value = getCodeList(type, identifier);
            } catch (IllegalArgumentException e) {
                throw new MetadataStoreException(Errors.format(Errors.Keys.DatabaseError_2, type, identifier), e);
            }
        } else {
            final CacheKey key = new CacheKey(type, identifier);
            /*
             * IMPLEMENTATION NOTE: be careful to not invoke any method that may synchronize on `this`
             * inside a block synchronized on `pool` (implicit synchronization of `pool` method calls).
             */
            value = pool.get(key);
            if (value == null && type.isInterface()) {
                final Dispatcher toSearch = new Dispatcher(identifier, this);
                value = Proxy.newProxyInstance(classloader, new Class<?>[] {type, MetadataProxy.class}, toSearch);
                if (verify) try {
                    /*
                     * If the caller asked to verify whether the record exists, perform a query now.
                     * We trivially request the identifier, so the `getValue(…)` result should be
                     * the identifier itself (this is not verified). If the record does not exist,
                     * a `MetadataStoreException` is thrown by `getValue(…)`.
                     */
                    synchronized (this) {
                        final Class<?> subType = TableHierarchy.subType(type, identifier);
                        CachedStatement result = prepareStatement(subType, null, toSearch.preferredIndex);
                        result.getValue(identifier, ID_COLUMN);               // Check record existence.
                        toSearch.preferredIndex = recycle(result, toSearch.preferredIndex);
                    }
                } catch (SQLException e) {
                    throw new MetadataStoreException(Errors.format(Errors.Keys.DatabaseError_2, type, identifier), e);
                }
                final Object replacement = pool.putIfAbsent(key, value);
                if (replacement != null) {
                    value = replacement;
                }
            }
            /*
             * At this point, a null value means that the given type is a class rather than an interface.
             * This may happen when a new type defined by a standard has not yet been defined in GeoAPI.
             * In such case, we only have the implementation class in Apache SIS, not yet the interface.
             * Since we cannot create a Proxy, we have to fetch all property values now. This is not
             * very efficient and may waste a little bit of memory, but it should not happen too often.
             */
            if (value == null) {
                Method method = null;
                final Class<?> subType = TableHierarchy.subType(type, identifier);
                final Dispatcher toSearch = new Dispatcher(identifier, this);
                try {
                    value = subType.getConstructor().newInstance();
                    final LookupInfo info            = getLookupInfo(subType);
                    final Map<String,Object> map     = asValueMap(value);
                    final Map<String,String> methods = standard.asNameMap(subType, NAME_POLICY, KeyNamePolicy.METHOD_NAME);
                    for (final Map.Entry<String,Object> entry : map.entrySet()) {
                        method = subType.getMethod(methods.get(entry.getKey()));
                        info.setMetadataType(subType);
                        final Object p = readColumn(info, method, toSearch);
                        if (p != null) {
                            entry.setValue(p);
                        }
                    }
                } catch (ReflectiveOperationException e) {
                    throw new MetadataStoreException(Errors.format(Errors.Keys.UnsupportedImplementation_1, subType), e);
                } catch (SQLException e) {
                    throw new MetadataStoreException(toSearch.error(method), e);
                }
            }
        }
        return type.cast(value);
    }

    /**
     * Gets the {@link LookupInfo} instance for call to the {@link #readColumn(LookupInfo, Method, Dispatcher)} method.
     * The call to those two methods must be in the same thread, and no other metadata object shall be queried between
     * the two calls (unless {@link LookupInfo#setMetadataType(Class)} is invoked again).
     *
     * @param  type  the interface class. This is mapped to the table name in the database.
     */
    final LookupInfo getLookupInfo(final Class<?> type) {
        final LookupInfo info = lastUsed.get();
        info.setMetadataType(type);
        return info;
    }

    /**
     * Invoked by {@link MetadataProxy} for fetching an attribute value from a table.
     * It the database table does not contains a column for the property, this method returns {@code null}.
     * A {@code null} value may also mean that the column exists but contains an SQL {@code NULL} value.
     *
     * @param  info      the interface type (together with cached information).
     *                   This is mapped to the table name in the database.
     * @param  method    the method invoked. This is mapped to the column name in the database.
     * @param  toSearch  contains the identifier and preferred index of the record to search.
     * @return the value of the requested attribute, or {@code null} if none.
     * @throws SQLException if the SQL query failed.
     * @throws MetadataStoreException if a value was not found or cannot be converted to the expected type.
     */
    final Object readColumn(final LookupInfo info, final Method method, final Dispatcher toSearch)
            throws SQLException, MetadataStoreException
    {
        /*
         * If the identifier is prefixed with a table name as in "{Organisation}identifier",
         * the name between bracket is a subtype of the given `type` argument.
         */
        final Class<?> type           = TableHierarchy.subType(info.getMetadataType(), toSearch.identifier);
        final Class<?> returnType     = Interim.getReturnType(method);
        final boolean  wantCollection = Collection.class.isAssignableFrom(returnType);
        final Class<?> elementType    = (wantCollection || Classes.isParameterizedProperty(returnType)) ? Classes.boundOfParameterizedProperty(method) : returnType;
        final boolean  isMetadata     = standard.isMetadata(elementType);
        final String   tableName      = getTableName(type);
        final String   columnName     = info.asNameMap(standard).get(method.getName());
        final boolean  isArray;
        Object value;
        synchronized (this) {
            if (!getExistingColumns(tableName).contains(columnName)) {
                value   = null;
                isArray = false;
            } else {
                /*
                 * Prepares the statement and executes the SQL query in this synchronized block.
                 * Note that the usage of `result` must stay inside this synchronized block
                 * because we cannot assume that JDBC connections are thread-safe.
                 */
                CachedStatement result = prepareStatement(type, tableName, toSearch.preferredIndex);
                value = result.getValue(toSearch.identifier, columnName);
                isArray = (value instanceof java.sql.Array);
                if (isArray) {
                    final java.sql.Array array = (java.sql.Array) value;
                    value = array.getArray();
                    array.free();
                }
                toSearch.preferredIndex = recycle(result, toSearch.preferredIndex);
            }
        }
        /*
         * If the value is an array and the return type is anything except an array of primitive type, ensure
         * that the value is converted in an array of type Object[]. In this process, resolve foreigner keys.
         */
        if (isArray && (wantCollection || !elementType.isPrimitive())) {
            final Object[] values = new Object[Array.getLength(value)];
            for (int i=0; i<values.length; i++) {
                Object element = Array.get(value, i);
                if (element != null) {
                    if (isMetadata) {
                        element = lookup(elementType, element.toString(), false);
                    } else try {
                        element = info.convert(elementType, element);
                    } catch (UnconvertibleObjectException e) {
                        throw new MetadataStoreException(Errors.format(Errors.Keys.IllegalPropertyValueClass_3,
                                Strings.toIndexed(columnName, i), elementType, element.getClass()), e);
                    }
                }
                values[i] = element;
            }
            value = values;             // Now a Java array.
            if (wantCollection) {
                value = specialize(UnmodifiableArrayList.wrap(values), returnType, elementType);
            }
        }
        /*
         * Now convert the value to its final type. To be strict, we should convert null values to empty collections
         * if the return type is a collection type. But we leave this task to the caller (which is the `Dispatcher`)
         * for making easier to detect when a value is absent, for allowing `Dispatcher` to manage its cache.
         */
        if (value != null) {
            if (isMetadata) {
                value = lookup(elementType, value.toString(), false);
            } else try {
                value = info.convert(elementType, value);
            } catch (UnconvertibleObjectException e) {
                throw new MetadataStoreException(Errors.format(Errors.Keys.IllegalPropertyValueClass_3,
                        columnName, elementType, value.getClass()), e);
            }
            if (wantCollection) {
                if (Set.class.isAssignableFrom(returnType)) {
                    return Collections.singleton(value);
                } else {
                    return Collections.singletonList(value);
                }
            }
        }
        return value;
    }

    /**
     * Returns the code of the given type and name. This method is defined for avoiding the compiler warning
     * message when the actual class is unknown (it must have been checked dynamically by the caller however).
     *
     * @return the requested code, or {@code null} if the given name is null or empty.
     * @throws IllegalArgumentException if there is no value for the given name and the code cannot be created.
     */
    @SuppressWarnings("unchecked")
    static CodeList<?> getCodeList(final Class<?> type, final String name) {
        return CodeLists.getOrCreate(type.asSubclass(CodeList.class), name);
    }

    /**
     * Copies the given collection into the best {@code Set} implementation if possible,
     * or returns the given collection unchanged otherwise.
     *
     * @param  collection   the collection to copy.
     * @param  returnType   the desired collection type.
     * @param  elementType  the type of elements in the collection.
     * @return the collection of a specialized type if relevant.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static <E> Collection<?> specialize(Collection<?> collection, final Class<?> returnType, final Class<E> elementType) {
        if (!returnType.isAssignableFrom(Set.class)) {
            return collection;
        }
        final Set<E> enumeration;
        if (CodeList.class.isAssignableFrom(elementType)) {
            enumeration = new CodeListSet<>((Class) elementType);
        } else if (Enum.class.isAssignableFrom(elementType)) {
            enumeration = EnumSet.noneOf((Class) elementType);
        } else {
            /*
             * If `returnType` is Collection.class, do not copy into a Set since a List
             * is probably good enough. Copy only if a Set is explicitly requested.
             */
            if (Set.class.isAssignableFrom(returnType)) {
                if (SortedSet.class.isAssignableFrom(returnType)) {
                    if (collection.isEmpty()) {
                        collection = Collections.emptySortedSet();
                    } else {
                        collection = Collections.unmodifiableSortedSet(new TreeSet<>(collection));
                    }
                } else {
                    switch (collection.size()) {
                        case 0:  collection = Collections.emptySet(); break;
                        case 1:  collection = Collections.singleton(CollectionsExt.first(collection)); break;
                        default: collection = Collections.unmodifiableSet(new LinkedHashSet<>(collection)); break;
                    }
                }
            }
            return collection;
        }
        for (final Object e : collection) {
            enumeration.add(elementType.cast(e));
        }
        return Collections.unmodifiableSet(enumeration);
    }

    /**
     * Reports a warning.
     *
     * @param source  the source class, either {@code MetadataSource} or {@code MetadataWriter}.
     * @param method  the method to report as the warning emitter.
     * @param record  the warning to report.
     */
    final void warning(final Class<? extends MetadataSource> source, final String method, final LogRecord record) {
        record.setSourceClassName(source.getCanonicalName());
        record.setSourceMethodName(method);
        record.setLoggerName(Loggers.SQL);
        final Filter filter = logFilter;
        if (filter == null || filter.isLoggable(record)) {
            CachedStatement.LOGGER.log(record);
        }
    }

    /**
     * Sets a filter to be notified when a warning occurred while reading from or writing metadata.
     * When a warning occurs, there is a choice:
     *
     * <ul>
     *   <li>If this metadata source has no warning filter, or if the filter returns {@code true},
     *       then the warning is logged at {@link Level#WARNING}.</li>
     *   <li>Otherwise the warning is not logged by this metadata source instance.
     *       The filter implementation is free to keep a reference to the given record,
     *       for example in order to display it in a graphical user interface.</li>
     * </ul>
     *
     * @param  filter  the filter to set, or {@code null} for removing the filter.
     * @return the previous filter, or {@code null} if none.
     *
     * @since 1.1
     */
    public Filter setWarningFilter(final Filter filter) {
        Filter p = logFilter;
        logFilter = filter;
        return p;
    }

    /**
     * Returns the current warning filter.
     *
     * @return the current filter, or {@code null} if none.
     *
     * @since 1.1
     */
    public Filter getWarningFilter() {
        return logFilter;
    }

    /**
     * Schedules a task for closing the statements and the connection, if no such task is scheduled.
     */
    private void scheduleCloseTask() {
        if (!isCloseScheduled) {
            DelayedExecutor.schedule(new CloseTask(System.nanoTime() + (TIMEOUT + EXTRA_DELAY)));
            isCloseScheduled = true;
        }
    }

    /**
     * A task to be executed later for closing all expired {@link CachedStatement}.
     * A result is expired if {@link CachedStatement#expireTime} is later than {@link System#nanoTime()}.
     */
    private final class CloseTask extends DelayedRunnable {
        /**
         * Creates a new task to be executed later.
         *
         * @param timestamp  time of execution of this task, in nanoseconds relative to {@link System#nanoTime()}.
         */
        CloseTask(final long timestamp) {
            super(timestamp);
        }

        /**
         * Invoked in a background thread for closing all expired {@link CachedStatement} instances.
         */
        @Override public void run() {
            closeExpired();
        }
    }

    /**
     * Executed in a background thread for closing statements after their expiration time.
     * This task will be given to the executor every time the first statement is recycled.
     */
    final synchronized void closeExpired() {
        isCloseScheduled = false;
        long delay = 0;
        final long currentTime = System.nanoTime();
        for (int i=0; i < statements.length; i++) {
            final CachedStatement statement = statements[i];
            if (statement != null) {
                /*
                 * Note: we really need to compute t1 - t0 and compare the delays.
                 * Do not simplify the equations in a way that result in comparisons
                 * like t1 > t0. See System.nanoTime() javadoc for more information.
                 */
                final long wait = statement.expireTime - currentTime;
                if (wait > delay) {
                    delay = wait;
                } else {
                    statements[i] = null;
                    closeQuietly(statement);
                }
            }
        }
        if (delay > 0) {
            // Some statements cannot be disposed yet.
            DelayedExecutor.schedule(new CloseTask(currentTime + delay + EXTRA_DELAY));
            isCloseScheduled = true;
        } else {
            // No more prepared statements.
            final Connection c = connection;
            connection = null;
            helper = null;
            closeQuietly(c);
        }
    }

    /**
     * Closes the given resource without throwing exception. In case of failure while closing the resource,
     * the message is logged but the process continue since we are not supposed to use the resource anymore.
     * This method is invoked from methods that cannot throw a SQL exception.
     */
    private void closeQuietly(final AutoCloseable resource) {
        if (resource != null) try {
            resource.close();
        } catch (Exception e) {
            /*
             * Catch Exception rather than SQLException because this method is invoked from semi-critical code
             * which need to never fail, otherwise some memory leak could occur. Pretend that the message come
             * from closeExpired(), which is the closest we can get to a public API.
             */
            final LogRecord record = new LogRecord(Level.WARNING, e.toString());
            record.setThrown(e);
            warning(MetadataSource.class, "closeExpired", record);
        }
    }

    /**
     * Closes the database connection used by this object.
     *
     * @throws MetadataStoreException if an error occurred while closing the connection.
     */
    @Override
    public synchronized void close() throws MetadataStoreException {
        try {
            for (int i=0; i < statements.length; i++) {
                final CachedStatement statement = statements[i];
                if (statement != null) {
                    statement.close();
                    statements[i] = null;
                }
            }
            if (connection != null) {
                connection.close();
                connection = null;
            }
            helper = null;
        } catch (SQLException e) {
            throw new MetadataStoreException(e.getLocalizedMessage(), Exceptions.unwrap(e));
        }
    }
}
