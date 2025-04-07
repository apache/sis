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
package org.apache.sis.storage.sql;

import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.lang.reflect.Method;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.WarningEvent;
import org.apache.sis.storage.sql.feature.Analyzer;
import org.apache.sis.storage.sql.feature.Database;
import org.apache.sis.storage.sql.feature.Resources;
import org.apache.sis.storage.sql.feature.SchemaModifier;
import org.apache.sis.storage.sql.feature.InfoStatements;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.io.stream.InternalOptionKey;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Version;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.setup.OptionKey;


/**
 * An abstract data store for reading or writing resources from/to a spatial database.
 * {@code SQLStore} requires a {@link DataSource} to be specified (indirectly) at construction time.
 * While not mandatory, a pooled data source is recommended because {@code SQLStore} may open and
 * close connections many times.
 *
 * <p>This class provides basic support for ISO 19125-2, also known as
 * <a href="https://www.ogc.org/standards/sfs">OGC Simple feature access - Part 2: SQL option</a>:
 * selected tables, views and queries can be viewed as {@link org.apache.sis.storage.FeatureSet} resources.
 * This selection is specified by implementing the {@link #readResourceDefinitions(DataAccess)} method.
 * The mapping from table structures to feature types is described in the package Javadoc.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.0
 */
public abstract class SQLStore extends DataStore implements Aggregate {
    /**
     * Names of possible public getter methods for fetching a metadata title from a {@link DataSource}.
     * Elements are sorted in preference order. The return value shall be a {@link String}.
     *
     * @see #getMetadata()
     */
    private static final String[] TITLE_GETTERS = {
            "getApplicationName",       // PostgreSQL
            "getDataSourceName",        // Derby
            "getDatabaseName"           // Derby, PostgreSQL, SQL Server, SQLite
    };

    /**
     * Names of possible public getter methods for fetching an identifier from a {@link DataSource}.
     * Elements are sorted in preference order. The return value shall be a {@link String}.
     *
     * @see #getIdentifier()
     */
    private static final String[] IDENTIFIER_GETTERS = {
            "getDatabaseName",
            "getDataSourceName"
    };

    /**
     * Names of possible public getter methods for fetching additional citation details.
     * Elements are sorted in preference order. The return value shall be a {@link String}.
     * On PostgreSQL, the return value is a hard-coded string such as
     * "Non-Pooling DataSource from PostgreSQL JDBC Driver 42.7.3"
     *
     * @see #getIdentifier()
     */
    private static final String[] DETAILS_GETTERS = {
            "getDescription"            // PostgreSQL, SQL Server
    };

    /**
     * The data source to use for obtaining connections to the database.
     * This is the storage given (indirectly, through the {@link StorageConnector} argument) at construction time.
     *
     * @see #getDataSource()
     *
     * @since 1.5
     */
    protected final DataSource source;

    /**
     * An identifier inferred from the data source, or {@code null} if none.
     *
     * @see #getIdentifier()
     */
    private final GenericName identifier;

    /**
     * The library to use for creating geometric objects, or {@code null} for system default.
     */
    private final GeometryLibrary geomLibrary;

    /**
     * The result of inspecting database schema for deriving {@link org.opengis.feature.FeatureType}s.
     * Created when first needed. May be discarded and recreated if the store needs a refresh.
     *
     * @see #model()
     * @see #model(Connection)
     * @see #refresh()
     */
    private volatile Database<?> model;

    /**
     * Fully qualified names (including catalog and schema) of the tables to include in this store.
     * The names shall be qualified names of 1, 2 or 3 components.
     * The name components can be {@code <catalog>.<schema pattern>.<table pattern>} where:
     *
     * <ul>
     *   <li>{@code <catalog>}, if present, is the name of a catalog as stored in the database.</li>
     *   <li>{@code <schema pattern>}, if present, is the pattern of a schema.
     *       The pattern can use {@code '_'} and {@code '%'} wildcards characters.</li>
     *   <li>{@code <table pattern>} (mandatory) is the pattern of a table.
     *       The pattern can use {@code '_'} and {@code '%'} wildcards characters.</li>
     * </ul>
     *
     * Only the main tables need to be specified, dependencies will be followed automatically.
     * If {@code null}, then the array will be created when first needed.
     *
     * @see #setModelSources(ResourceDefinition[])
     * @see #tableNames()
     */
    private GenericName[] tableNames;

    /**
     * Queries to expose as resources, or an empty array if none.
     * If {@code null}, then the array will be created when first needed.
     *
     * @see #setModelSources(ResourceDefinition[])
     * @see #queries()
     */
    private ResourceDefinition[] queries;

    /**
     * The metadata, created when first requested.
     */
    private Metadata metadata;

    /**
     * The locale to use for international texts to write in a database table.
     * This is not necessarily the same locale as {@link #getLocale()}, because the
     * latter is for warning and error messages to shown in user applications.
     *
     * <p><b>Example:</b> if a new <abbr>CRS</abbr> needs to be added in the {@code "SPATIAL_REF_SYS"} table
     * and if that table contains a {@code "description"} column (as in the Geopackage format), then the text
     * to write in that column will be localized with this locale.</p>
     *
     * <p>If the value is {@code null}, then a default locale is used.</p>
     *
     * @see OptionKey#LOCALE
     *
     * @since 1.5
     */
    protected final Locale contentLocale;

    /**
     * The user-specified method for customizing the schema inferred by table analysis.
     * This is {@code null} if there is none.
     */
    private final SchemaModifier customizer;

    /**
     * The lock for read or write operations in the SQL database, or {@code null} if none.
     * The read or write lock should be obtained before to get a connection for executing
     * a statement, and released after closing the connection. Locking is assumed unneeded
     * for obtaining database metadata.
     *
     * <p>This field should be null if the database manages concurrent transactions by itself.
     * It is non-null only as a workaround for databases that do not support concurrency.</p>
     *
     * <p>This lock is not used for cache integrity. The {@code SQLStore} caches are protected
     * by classical synchronized statements.</p>
     *
     * @see Database#transactionLocks
     */
    final ReadWriteLock transactionLocks;

    /**
     * Creates a new {@code SQLStore} for the given data source. The given {@code connector} shall contain
     * a {@link DataSource} instance. Tables or views to include in the store will be specified by the
     * {@link #readResourceDefinitions(DataAccess)} method, which will be invoked when first needed.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (JDBC data source, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     *
     * @since 1.5
     */
    protected SQLStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        source           = connector.commit(DataSource.class, "SQL");
        geomLibrary      = connector.getOption(OptionKey.GEOMETRY_LIBRARY);
        contentLocale    = connector.getOption(OptionKey.LOCALE);
        customizer       = connector.getOption(SchemaModifier.OPTION_KEY);
        transactionLocks = connector.getOption(InternalOptionKey.LOCKS);
        identifier       = getDataSourceProperty(IDENTIFIER_GETTERS)
                            .map((id) -> Names.createLocalName(null, null, id)).orElse(null);
    }

    /**
     * Declares the tables or queries to use as the sources of feature resources.
     *
     * @param  resources  tables, views or queries to include in this store.
     * @throws DataStoreException if an error occurred while processing the given resources.
     */
    final void setModelSources(final ResourceDefinition[] resources) throws DataStoreException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final var tableNames = new GenericName[resources.length];
        int tableCount = 0;

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final var queries = new ResourceDefinition[resources.length];
        int queryCount = 0;

        for (int i=0; i<resources.length; i++) {
            final ResourceDefinition resource = resources[i];
            ArgumentChecks.ensureNonNullElement("resources", i, resource);
            final GenericName name = resource.getName();
            final int depth = name.depth();
            if (depth < 1 || depth > 3) {
                throw new IllegalNameException(Resources.format(Resources.Keys.IllegalQualifiedName_1, name));
            }
            if (resource.query == null) {
                tableNames[tableCount++] = name;
            } else {
                queries[queryCount++] = resource;
            }
        }
        this.tableNames = ArraysExt.resize(tableNames, tableCount);
        this.queries    = ArraysExt.resize(queries,    queryCount);
    }

    /**
     * Invoked the first time that {@code SQLStore} opens a connection on the database, or after refresh.
     * The default implementation does nothing. Subclasses can override this method if they need to create
     * tables in an empty database, or for fetching more information.
     *
     * <p>If {@link #refresh()} has been invoked, then this initialization method will be invoked again
     * the next time that a connection is needed. This method is invoked <em>before</em>
     * {@link #readResourceDefinitions(DataAccess)}.</p>
     *
     * @param  connection  a connection to the database.
     * @throws DataStoreException if an error occurred during the initialization.
     * @throws SQLException if an error occurred during the execution of an <abbr>SQL</abbr> query.
     *
     * @since 1.5
     */
    protected void initialize(Connection connection) throws DataStoreException, SQLException {
    }

    /**
     * Returns the data source to use for obtaining connections to the database.
     *
     * @return the data source to use for obtaining connections to the database.
     * @since 1.5
     */
    public DataSource getDataSource() {
        return source;
    }

    /**
     * Returns parameters that can be used for opening this SQL data store.
     * The parameters are described by {@link SQLStoreProvider#getOpenParameters()} and contains
     * at least a parameter named {@value SQLStoreProvider#LOCATION} with a {@link DataSource} value.
     *
     * @return parameters used for opening this data store.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        if (provider == null) {
            return Optional.empty();
        }
        final ParameterValueGroup pg = provider.getOpenParameters().createValue();
        pg.parameter(SQLStoreProvider.LOCATION).setValue(source);
        /*
         * Do not include `tableNames` and `queries` because they are initially null
         * and determined dynamically when first needed. Because potentially dynamic,
         * their values cannot be in parameters.
         */
        return Optional.of(pg);
    }

    /**
     * Returns an identifier for the root resource of this SQL store, or an empty value if none.
     * The default implementation returns the database name if this property can be found in the
     * {@link DataSource} implementation.
     *
     * @return an identifier for the root resource of this SQL store.
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.ofNullable(identifier);
    }

    /**
     * Returns the fully qualified names of the tables to include in this store.
     * The returned array shall be considered read-only.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final GenericName[] tableNames() {
        return tableNames;
    }

    /**
     * Returns the queries to expose as resources.
     * The returned array shall be considered read-only.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final ResourceDefinition[] queries() {
        return queries;
    }

    /**
     * Clears the cached model. The model will be reloaded when first needed.
     * This method shall be invoked in a block synchronized on {@code this}.
     */
    final void clearModel() {
        model    = null;
        metadata = null;
    }

    /**
     * Returns the database model if it already exists, or {@code null} otherwise.
     * This method is thread-safe.
     */
    final Database<?> modelOrNull() {
        return model;
    }

    /**
     * Returns the database model, analyzing the database schema when first needed.
     * This method is thread-safe.
     */
    final Database<?> model() throws DataStoreException {
        Database<?> current = model;
        if (current == null) {
            synchronized (this) {
                try (Connection c = source.getConnection()) {
                    current = model(c);
                } catch (Exception e) {
                    throw cannotExecute(e);
                }
            }
        }
        return current;
    }

    /**
     * Returns the database model, analyzing the database schema when first needed.
     * This method performs the same work as {@link #model()}, but using an existing connection.
     * Callers must own a synchronization lock on {@code this}.
     *
     * @param c  connection to the database.
     */
    final Database<?> model(final Connection c) throws Exception {
        assert Thread.holdsLock(this);
        Database<?> current = model;
        if (current == null) {
            initialize(c);
            final DatabaseMetaData md = c.getMetaData();
            final var analyzer = new Analyzer(source, md, geomLibrary, contentLocale, listeners, transactionLocks);
            current = analyzer.database;
            try (final InfoStatements spatialInformation = current.createInfoStatements(c)) {
                if (tableNames == null) {
                    final DataAccess dao = newDataAccess(false);
                    dao.setConnection(c, spatialInformation);
                    setModelSources(readResourceDefinitions(dao));
                    // Do not close the DAO, because we still use the connection and the info statements.
                }
                current.analyze(this, analyzer, tableNames, queries, customizer, spatialInformation);
            }
            model = current;
        }
        return current;
    }

    /**
     * Returns the version of the database software, together with versions of extensions if any.
     * For example, in the case of a database on PostgreSQL, this map may contain two entries:
     * the first one with the "PostgreSQL" key, optionally followed by an entry with the "PostGIS" key.
     *
     * @return version of the database software as the first entry, followed by versions of extensions if any.
     * @throws DataStoreException if an error occurred while fetching the metadata.
     *
     * @since 1.5
     */
    public Map<String, Version> getDatabaseSoftwareVersions() throws DataStoreException {
        return model().getDatabaseSoftwareVersions();
    }

    /**
     * Returns information about the dataset as a whole. The returned metadata object can contain information
     * such as the list of feature types.
     *
     * @return information about the dataset.
     * @throws DataStoreException if an error occurred while fetching the metadata.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final var builder = new MetadataBuilder();
            builder.addIdentifier(identifier, MetadataBuilder.Scope.RESOURCE);
            getDataSourceProperty(TITLE_GETTERS).ifPresent(builder::addTitle);
            getDataSourceProperty(DETAILS_GETTERS).ifPresent(builder::addOtherCitationDetails);
            try (Connection c = source.getConnection()) {
                model(c).metadata(c.getMetaData(), builder);
            } catch (Exception e) {
                throw cannotExecute(e);
            }
            metadata = builder.buildAndFreeze();
        }
        return metadata;
    }

    /**
     * Tries to get a property from the data source by invoking all given public getter methods
     * until a method exists and returns a non-null value.
     *
     * @param  methodNames {@link #TITLE_GETTERS} or {@link #IDENTIFIER_GETTERS}.
     * @return the first value found.
     * @throws DataStoreException if an unexpected reflective operation occurred.
     */
    private Optional<String> getDataSourceProperty(final String[] methodNames) throws DataStoreException {
        for (final String c : methodNames) try {
            final Method method = source.getClass().getMethod(c);
            if (method.getReturnType() == String.class) {
                String name = Strings.trimOrNull((String) method.invoke(source));
                if (name != null) {
                    return Optional.of(name);
                }
            }
        } catch (NoSuchMethodException | SecurityException e) {
            // Ignore - try the next method.
        } catch (ReflectiveOperationException e) {
            throw cannotExecute(e);
        }
        return Optional.empty();
    }

    /**
     * Returns the resources (feature set or coverages) in this SQL store.
     * The collection of resources should be constructed only when first needed and cached
     * for future invocations of this method. The cache can be cleared by {@link #refresh()}.
     * This method should not load immediately the whole data of the {@code Resource} elements,
     * as {@link Resource} sub-interfaces provide the <abbr>API</abbr> for deferred loading.
     *
     * <h4>Default implementation</h4>
     * By default, the collection contains one {@link org.apache.sis.storage.FeatureSet} per table, view or
     * query matching a {@link ResourceDefinition} returned by {@link #readResourceDefinitions(DataAccess)}.
     *
     * @return children resources that are components of this SQL store.
     * @throws DataStoreException if an error occurred while fetching the components.
     */
    @Override
    public Collection<? extends Resource> components() throws DataStoreException {
        return model().tables();
    }

    /**
     * Searches for a resource identified by the given identifier.
     * This method shall recognize at least the {@linkplain Resource#getIdentifier() identifiers} of the
     * resources returned by {@link #components()}, but may also (optionally) recognize the identifiers
     * of auxiliary resources such as component dependencies (e.g., tables referenced by foreigner keys).
     *
     * <h4>Default implementation</h4>
     * By default, this method searches for a table, view or query with a name matching the given identifier.
     * The scope of the search includes the tables, views or queries matching a {@link ResourceDefinition},
     * together with other tables referenced by foreigner keys (the dependencies).
     * The given identifier may be qualified with the schema name,
     * or may be only the table name if there is no ambiguity.
     *
     * @param  identifier  identifier of the resource to fetch. Must be non-null.
     * @return resource associated to the given identifier (never {@code null}).
     * @throws IllegalNameException if no resource is found for the given identifier, or if more than one resource is found.
     * @throws DataStoreException if another kind of error occurred while searching resources.
     */
    @Override
    public Resource findResource(final String identifier) throws DataStoreException {
        return model().findTable(this, identifier);
    }

    /**
     * A callback for providing the resource definitions of a database, typically from a content table.
     * {@code SQLStore} will invoke this method when first needed after construction or after calls to
     * {@link #refresh()}. Implementations can use the <abbr>SQL</abbr> connection and methods provided
     * by the {@code dao} argument. This method does not need to cache the result.
     *
     * <div class="note"><b>Example:</b> in a database conform to the Geopackage standard,
     * the resource definitions are provided by the {@code "gpkg_contents"} table.
     * Therefore, the {@link org.apache.sis.storage.geopackage.GpkgStore} subclass
     * will read the content of that table every times this method is invoked.</div>
     *
     * @param  dao  low-level access (such as <abbr>SQL</abbr> connection) to the database.
     * @return tables or views to include in the store. Only the main tables need to be specified.
     *         Dependencies (inferred from the foreigner keys) will be followed automatically.
     * @throws DataStoreException if an error occurred while fetching the resource definitions.
     *
     * @see #initialize(Connection)
     *
     * @since 1.5
     */
    protected abstract ResourceDefinition[] readResourceDefinitions(DataAccess dao) throws DataStoreException;

    /**
     * Creates a new low-level data access object. Each {@code DataAccess} instance can provide a single
     * <abbr>SQL</abbr> {@link Connection} to the database (sometime protected by a read or write lock),
     * together with methods for fetching or adding <abbr>CRS</abbr> definitions from/into the
     * {@code SPATIAL_REF_SYS} table.
     *
     * <p>The returned object shall be used in a {@code try ... finally} block.
     * This is needed not only for closing the connection, but also for releasing read or write lock.
     * Example:</p>
     *
     * {@snippet lang="java" :
     *   try (DataAccess dao = newDataAccess(false)) {
     *       try (Statement stmt = dao.getConnection().createStatement()) {
     *           // Perform some SQL queries here.
     *       }
     *   }
     *   }
     *
     * @param  write  whether write operations may be performed.
     * @return an object provider low-level access (e.g. through <abbr>SQL</abbr> queries) to the data.
     *
     * @since 1.5
     */
    public DataAccess newDataAccess(final boolean write) {
        return new DataAccess(this, write);
    }

    /**
     * Registers a listener to notify when the specified kind of event occurs in this data store.
     * The current implementation of this data store can emit only {@link WarningEvent}s;
     * any listener specified for another kind of events will be ignored.
     */
    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        // If an argument is null, we let the parent class throws (indirectly) NullPointerException.
        if (listener == null || eventType == null || eventType.isAssignableFrom(WarningEvent.class)) {
            super.addListener(eventType, listener);
        }
    }

    /**
     * Returns the exception to throw for the given cause.
     * The cause is typically a {@link java.sql.SQLException}, but not necessarily.
     * This method provides a central point where we may specialize the type of the
     * returned exception depending on the type of the cause.
     *
     * @param  cause  the cause of the error. Cannot be null.
     * @return the data store exception to throw. May be {@code cause} itself.
     */
    static DataStoreException cannotExecute(final Exception cause) {
        final Exception unwrap = Exceptions.unwrap(cause);
        if (unwrap instanceof DataStoreException) {
            return (DataStoreException) unwrap;
        } else {
            return new DataStoreException(cause.getMessage(), unwrap);
        }
    }

    /**
     * Executes the {@code "VACUUM"} statement on the database.
     * This is a non-standard feature that exists on some databases such as PostgreSQL and SQLite.
     * This method executes the statement only if the {@linkplain DatabaseMetaData#getSQLKeywords()
     * <abbr>SQL</abbr> keywords declared in the database metadata} contains {@code "VACUUM"}, ignoring case.
     *
     * <p>This is a potentially costly operation which should be rarely executed.
     * A typical use case if to compress a newly created Geopackage file (which are SQLite database).</p>
     *
     * @throws DataStoreException if an error occurred while executing the vacuum.
     *
     * @since 1.5
     */
    public void vacuum() throws DataStoreException {
        try (Connection c = source.getConnection()) {
            var keywords = (String[]) CharSequences.split(c.getMetaData().getSQLKeywords(), ',');
            if (ArraysExt.containsIgnoreCase(keywords, "VACUUM")) {
                Lock lock = null;
                if (transactionLocks != null) {
                    lock = transactionLocks.writeLock();
                    lock.lock();
                }
                try (Statement stmt = c.createStatement()) {
                    stmt.executeUpdate("VACUUM");
                } finally {
                    if (lock != null) {
                        lock.unlock();
                    }
                }
            }
        } catch (Exception e) {
            throw cannotExecute(e);
        }
    }

    /**
     * Clears the cache so that next operations will reload all needed information from the database.
     * This method can be invoked when the database content has been modified by a process other than
     * the methods in this class.
     *
     * @since 1.5
     */
    public synchronized void refresh() {
        clearModel();
        queries    = null;
        tableNames = null;
    }

    /**
     * Closes this SQL store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing the SQL store.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        listeners.close();      // Should never fail.
        // There is no JDBC connection to close here.
        clearModel();
    }
}
