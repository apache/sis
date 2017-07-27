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
import java.util.NoSuchElementException;
import java.util.logging.Level;
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
import java.sql.SQLNonTransientException;
import java.sql.PreparedStatement;
import org.opengis.annotation.UML;
import org.opengis.util.CodeList;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.system.DelayedExecutor;
import org.apache.sis.internal.system.DelayedRunnable;
import org.apache.sis.internal.metadata.sql.Initializer;
import org.apache.sis.internal.metadata.sql.SQLBuilder;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.Types;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;
import org.apache.sis.internal.geoapi.evolution.Interim;


/**
 * A connection to a metadata database in read-only mode. It can be either the database
 * {@linkplain #getProvided() provided by Apache SIS} with pre-defined ISO 19115 metadata,
 * or another database specified at construction time.
 * Metadata instances can be obtained as in the example below:
 *
 * {@preformat java
 *   MetadataSource metadata = MetadataSource.getProvided();
 *   Format format = source.lookup(Format.class, "PNG");
 * }
 *
 * where {@code id} is the primary key value for the desired record in the {@code MD_Format} table.
 *
 * <div class="section">Properties</div>
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
 * <div class="section">Concurrency</div>
 * {@code MetadataSource} is thread-safe but is not concurrent. If concurrency is desired,
 * multiple instances of {@code MetadataSource} can be created for the same {@link DataSource}.
 * The {@link #MetadataSource(MetadataSource)} convenience constructor can be used for this purpose.
 *
 * @author  Touraïvane (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class MetadataSource implements AutoCloseable {
    /**
     * The policy for column names. We use UML identifiers (e.g. {@code "title"}) as defined by the metadata standard
     * (typically ISO 19115).
     */
    static final KeyNamePolicy NAME_POLICY = KeyNamePolicy.UML_IDENTIFIER;

    /**
     * The column name used for the identifiers. We do not quote this identifier;
     * we will let the database uses its own lower-case / upper-case convention.
     */
    static final String ID_COLUMN = "ID";

    /**
     * Delimiter characters for the table name in identifier. Table names are prefixed to identifiers only if
     * the type represented by the table is a subtype. For example since {@code CI_Organisation} is a subtype
     * of {@code CI_Party}, identifiers for organizations need to be prefixed by {@code {CI_Organisation}} in
     * order allow {@code MetadataSource} to know in which table to search for such party.
     *
     * @see MetadataWriter#isReservedChar(int)
     */
    static final char TYPE_OPEN = '{', TYPE_CLOSE = '}';

    /**
     * The timeout before to close a prepared statement, in nanoseconds. This is set to 2 seconds,
     * which is a bit short but should be okay if the {@link DataSource} creates pooled connections.
     * In case there is no connection pool, then the mechanism defined in this package will hopefully
     * keeps the performance at a reasonable level.
     *
     * @see #closeExpired()
     */
    private static final long TIMEOUT = 2000_000000;

    /**
     * An extra delay to add to the {@link #TIMEOUT} in order to increase the chances to
     * close many statements at once.
     */
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
     * because a single JDBC connection can not be assumed thread-safe.
     *
     * <p>Usage example:</p>
     * {@preformat java
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
     * }
     *
     * @see #take(Class, int)
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
     * Where to report the warnings. This is not necessarily a logger, since users can register listeners.
     *
     * @see #readColumn(LookupInfo, Method, Dispatcher)
     */
    private final WarningListeners<MetadataSource> listeners;

    /**
     * Whether at least one {@link CloseTask} is scheduled for execution.
     *
     * @see #scheduleCloseTask()
     */
    private boolean isCloseScheduled;

    /**
     * The instance connected to the {@code "jdbc/SpatialMetadata"} database,
     * created when first needed and cleared when the classpath change.
     */
    private static volatile MetadataSource instance;
    static {
        SystemListener.add(new SystemListener(Modules.METADATA) {
            @Override protected void classpathChanged() {
                instance = null;
            }
        });
    }

    /**
     * Returns the metadata source connected to the {@code "jdbc/SpatialMetadata"} database.
     * In a default Apache SIS installation, this metadata source contains pre-defined records
     * for some commonly used {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation
     * citations} and {@linkplain org.apache.sis.metadata.iso.distribution.DefaultFormat formats}
     * among others.
     *
     * @return source of pre-defined metadata records from the {@code "jdbc/SpatialMetadata"} database.
     * @throws MetadataStoreException if this method can not connect to the database.
     */
    public static MetadataSource getProvided() throws MetadataStoreException {
        MetadataSource ms = instance;
        if (ms == null) {
            final DataSource dataSource;
            try {
                dataSource = Initializer.getDataSource();
            } catch (Exception e) {
                throw new MetadataStoreException(Errors.format(Errors.Keys.CanNotConnectTo_1, Initializer.JNDI), e);
            }
            if (dataSource == null) {
                throw new MetadataStoreException(Initializer.unspecified(null));
            }
            synchronized (MetadataSource.class) {
                ms = instance;
                if (ms == null) {
                    ms = new MetadataSource(MetadataStandard.ISO_19115, dataSource, "metadata", null);
                    ms.install();
                    instance = ms;
                }
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
        this.listeners    = new WarningListeners<>(this);
        this.lastUsed     = new ThreadLocal<LookupInfo>() {
            @Override protected LookupInfo initialValue() {
                return new LookupInfo();
            }
        };
    }

    /**
     * Creates a new metadata source with the same configuration than the given source.
     * The two {@code MetadataSource} instances will share the same {@code DataSource}
     * but will use their own {@link Connection}.
     * This constructor is useful when concurrency is desired.
     *
     * <p>The new {@code MetadataSource} initially contains all {@linkplain #addWarningListener warning listeners}
     * declared in the given {@code source}. But listeners added or removed in a {@code MetadataSource} after the
     * construction will not impact the other {@code MetadataSource} instance.</p>
     *
     * @param  source  the source from which to copy the configuration.
     */
    public MetadataSource(final MetadataSource source) {
        ArgumentChecks.ensureNonNull("source", source);
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
        listeners    = new WarningListeners<>(this, source.listeners);
    }

    /**
     * If the metadata schema does not exist in the database, creates it and inserts the pre-defined metadata values.
     * The current implementation has the following restrictions:
     *
     * <ul>
     *   <li>Metadata standard must be {@link MetadataStandard#ISO_19115} or compatible.</li>
     *   <li>The schema name must be {@code "metadata"}, as this is the name used unquoted in SQL scripts.</li>
     * </ul>
     *
     * @throws MetadataStoreException if an error occurred while inserting the metadata.
     */
    final synchronized void install() throws MetadataStoreException {
        try {
            final Connection connection = connection();
            final DatabaseMetaData md = connection.getMetaData();
            if (md.storesUpperCaseIdentifiers()) {
                schema = schema.toUpperCase(Locale.US);
            } else if (md.storesLowerCaseIdentifiers()) {
                schema = schema.toLowerCase(Locale.US);
            }
            quoteSchema = false;
            try (ResultSet result = md.getTables(catalog, schema, "CI_Citation", null)) {
                if (result.next()) {
                    return;
                }
            }
            final Installer installer = new Installer(connection);
            installer.run();
        } catch (IOException | SQLException e) {
            /*
             * Derby sometime wraps SQLException into another SQLException.  For making the stack strace a
             * little bit simpler, keep only the root cause provided that the exception type is compatible.
             */
            throw new MetadataStoreException(e.getLocalizedMessage(), Exceptions.unwrap(e));
        }
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
            Logging.log(MetadataSource.class, "lookup", Initializer.connected(c.getMetaData()));
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
     * Returns a statement that can be reused for the given interface, or {@code null} if none.
     *
     * @param  type            the interface for which to reuse a prepared statement.
     * @param  preferredIndex  index in the cache array where to search first. This is only a hint for increasing
     *         the chances to find quickly a {@code CachedStatement} instance for the right type and identifier.
     */
    private CachedStatement take(final Class<?> type, final int preferredIndex) {
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
        return null;
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
                     * If we reach this point, this means that the 'statements' pool has reached its maximal capacity.
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
     * Returns the table name for the specified class.
     * This is usually the ISO 19115 name.
     */
    static String getTableName(final Class<?> type) {
        final UML annotation = type.getAnnotation(UML.class);
        if (annotation == null) {
            return type.getSimpleName();
        }
        final String name = annotation.identifier();
        return name.substring(name.lastIndexOf('.') + 1);
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
        ArgumentChecks.ensureNonNull("metadata", metadata);
        String identifier = proxy(metadata);
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
     * @param  helper    an helper class for creating the SQL query.
     * @return the identifier of the given metadata, or {@code null} if none.
     * @throws SQLException if an error occurred while searching in the database.
     */
    final String search(final String table, Set<String> columns, final Map<String,Object> metadata,
            final Statement stmt, final SQLBuilder helper) throws SQLException
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
                            dependency = search(getTableName(standard.getInterface(type)),
                                    null, asValueMap(value), stmt, new SQLBuilder(helper));
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
                helper.append("SELECT ").append(ID_COLUMN).append(" FROM ")
                        .appendIdentifier(schema, table).append(" WHERE ");
            } else {
                helper.append(" AND ");
            }
            helper.appendIdentifier(column).appendCondition(value);
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
                        warning(MetadataSource.class, "search", Errors.getResources((Locale) null).getLogRecord(
                                Level.WARNING, Errors.Keys.DuplicatedElement_1, candidate));
                        break;
                    }
                }
            }
        }
        return identifier;
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
             * it does not mean "no schema" (the later is specified by an empty string). This match better what we
             * want because if we do not specify a schema in a SELECT statement, then the actual schema used depends
             * on the search path specified in the database environment variables.
             */
            final DatabaseMetaData md = connection().getMetaData();
            try (ResultSet rs = md.getColumns(catalog, schema, table, null)) {
                while (rs.next()) {
                    if (!columns.add(rs.getString("COLUMN_NAME"))) {
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
     * If the given identifier specifies a subtype of the given type, then returns that subtype.
     * For example if the given type is {@code Party.class} and the given identifier is
     * {@code "{CI_Organisation}EPSG"}, then this method returns {@code Organisation.class}.
     * Otherwise this method returns {@code type} unchanged.
     */
    private static Class<?> subType(Class<?> type, final String identifier) {
        if (identifier.charAt(0) == TYPE_OPEN) {
            final int i = identifier.indexOf(TYPE_CLOSE);
            if (i >= 0) {
                final Class<?> subType = Types.forStandardName(identifier.substring(1, i));
                if (subType != null && type.isAssignableFrom(subType)) {
                    type = subType;
                }
            }
        }
        return type;
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
     * @throws MetadataStoreException if a SQL query failed.
     */
    public <T> T lookup(final Class<T> type, final String identifier) throws MetadataStoreException {
        ArgumentChecks.ensureNonNull("type", type);
        ArgumentChecks.ensureNonEmpty("identifier", identifier);
        Object value;
        if (CodeList.class.isAssignableFrom(type)) {
            value = getCodeList(type, identifier);
        } else {
            final CacheKey key = new CacheKey(type, identifier);
            /*
             * IMPLEMENTATION NOTE: be careful to not invoke any method that may synchronize on 'this'
             * inside the block synchronized on 'pool'.
             */
            synchronized (pool) {
                value = pool.get(key);
                if (value == null && type.isInterface()) {
                    value = Proxy.newProxyInstance(classloader,
                            new Class<?>[] {type, MetadataProxy.class}, new Dispatcher(identifier, this));
                    pool.put(key, value);
                }
            }
            /*
             * At this point, a null value means that the given type is a class rather than an interface.
             * This may happen when a new type defined by a standard has not yet been defined in GeoAPI.
             * In such case, we only have the implementation class in Apache SIS, not yet the interface.
             * Since we can not create a Proxy, we have to fetch all property values now. This is not
             * very efficient and may waste a little bit of memory, but it should not happen too often.
             */
            if (value == null) {
                Method method = null;
                final Class<?> subType = subType(type, identifier);
                final Dispatcher toSearch = new Dispatcher(identifier, this);
                try {
                    value = subType.newInstance();
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
     *
     * @param  info      the interface type (together with cached information).
     *                   This is mapped to the table name in the database.
     * @param  method    the method invoked. This is mapped to the column name in the database.
     * @param  toSearch  contains the identifier and preferred index of the record to search.
     * @return the value of the requested attribute.
     * @throws SQLException if the SQL query failed.
     * @throws MetadataStoreException if a value was not found or can not be converted to the expected type.
     */
    final Object readColumn(final LookupInfo info, final Method method, final Dispatcher toSearch)
            throws SQLException, MetadataStoreException
    {
        /*
         * If the identifier is prefixed with a table name as in "{CI_Organisation}identifier",
         * the name between bracket is a subtype of the given 'type' argument.
         */
        final Class<?> type           = subType(info.getMetadataType(), toSearch.identifier);
        final Class<?> returnType     = Interim.getReturnType(method);
        final boolean  wantCollection = Collection.class.isAssignableFrom(returnType);
        final Class<?> elementType    = wantCollection ? Classes.boundOfParameterizedProperty(method) : returnType;
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
                 * Note that the usage of 'result' must stay inside this synchronized block
                 * because we can not assume that JDBC connections are thread-safe.
                 */
                CachedStatement result = take(type, JDK8.toUnsignedInt(toSearch.preferredIndex));
                if (result == null) {
                    final SQLBuilder helper = helper();
                    final String query = helper.clear().append("SELECT * FROM ")
                            .appendIdentifier(schema, tableName).append(" WHERE ")
                            .append(ID_COLUMN).append("=?").toString();
                    result = new CachedStatement(type, connection().prepareStatement(query), listeners);
                }
                value = result.getValue(toSearch.identifier, columnName);
                isArray = (value instanceof java.sql.Array);
                if (isArray) {
                    final java.sql.Array array = (java.sql.Array) value;
                    value = array.getArray();
                    array.free();
                }
                toSearch.preferredIndex = (byte) recycle(result, JDK8.toUnsignedInt(toSearch.preferredIndex));
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
                        element = lookup(elementType, element.toString());
                    } else try {
                        element = info.convert(elementType, element);
                    } catch (UnconvertibleObjectException e) {
                        throw new MetadataStoreException(Errors.format(Errors.Keys.IllegalPropertyValueClass_3,
                                columnName + '[' + i + ']', elementType, element.getClass()), e);
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
         * Now converts the value to its final type. To be strict, we should convert null values into empty collections
         * if the return type is a collection type. But we leave this task to the caller (which is the Dispatcher class)
         * for making easier to detect when a value is absent, for allowing Dispatcher to manage its cache.
         */
        if (value != null) {
            if (isMetadata) {
                value = lookup(elementType, value.toString());
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
     */
    @SuppressWarnings("unchecked")
    private static CodeList<?> getCodeList(final Class<?> type, final String name) {
        return Types.forCodeName(type.asSubclass(CodeList.class), name, true);
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
            enumeration = new CodeListSet((Class) elementType);
        } else if (Enum.class.isAssignableFrom(elementType)) {
            enumeration = EnumSet.noneOf((Class) elementType);
        } else {
            /*
             * If 'returnType' is Collection.class, do not copy into a Set since a List
             * is probably good enough. Copy only if a Set is explicitely requested.
             */
            if (Set.class.isAssignableFrom(returnType)) {
                if (SortedSet.class.isAssignableFrom(returnType)) {
                    if (collection.isEmpty()) {
                        collection = CollectionsExt.emptySortedSet();
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
        listeners.warning(record);
    }

    /**
     * Adds a listener to be notified when a warning occurred while reading from or writing metadata.
     * When a warning occurs, there is a choice:
     *
     * <ul>
     *   <li>If this metadata source has no warning listener, then the warning is logged at {@link Level#WARNING}.</li>
     *   <li>If this metadata source has at least one warning listener, then all listeners are notified
     *       and the warning is <strong>not</strong> logged by this metadata source instance.</li>
     * </ul>
     *
     * Consider invoking this method in a {@code try} … {@code finally} block if the {@code MetadataSource}
     * lifetime is longer than the listener lifetime, as below:
     *
     * {@preformat java
     *     source.addWarningListener(listener);
     *     try {
     *         // Do some work...
     *     } finally {
     *         source.removeWarningListener(listener);
     *     }
     * }
     *
     * @param  listener  the listener to add.
     * @throws IllegalArgumentException if the given listener is already registered in this metadata source.
     */
    public void addWarningListener(final WarningListener<? super MetadataSource> listener)
            throws IllegalArgumentException
    {
        listeners.addWarningListener(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * @param  listener  the listener to remove.
     * @throws NoSuchElementException if the given listener is not registered in this metadata source.
     */
    public void removeWarningListener(final WarningListener<? super MetadataSource> listener)
            throws NoSuchElementException
    {
        listeners.removeWarningListener(listener);
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
            // Some statements can not be disposed yet.
            DelayedExecutor.schedule(new CloseTask(currentTime + delay + EXTRA_DELAY));
            isCloseScheduled = true;
        } else {
            // No more prepared statements.
            final Connection c = this.connection;
            connection = null;
            helper = null;
            closeQuietly(c);
        }
    }

    /**
     * Closes the given resource without throwing exception. In case of failure while closing the resource,
     * the message is logged but the process continue since we are not supposed to use the resource anymore.
     * This method is invoked from methods that can not throw a SQL exception.
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
