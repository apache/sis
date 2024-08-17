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
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import javax.sql.DataSource;
import java.sql.Connection;
import java.lang.reflect.Method;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.WarningEvent;
import org.apache.sis.storage.sql.feature.Database;
import org.apache.sis.storage.sql.feature.Resources;
import org.apache.sis.storage.sql.feature.InfoStatements;
import org.apache.sis.storage.sql.feature.SchemaModifier;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.io.stream.InternalOptionKey;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.setup.OptionKey;


/**
 * A data store capable to read and create features from a spatial database.
 * {@code SQLStore} requires a {@link DataSource} to be specified (indirectly) at construction time.
 * The {@code DataSource} should provide pooled connections, because {@code SQLStore} will frequently
 * opens and closes them.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.0
 */
public class SQLStore extends DataStore implements Aggregate {
    /**
     * Names of possible public getter methods for data source title, in preference order.
     */
    private static final String[] NAME_GETTERS = {
            "getDescription",           // PostgreSQL, SQL Server
            "getDataSourceName",        // Derby
            "getDatabaseName",          // Derby, PostgreSQL, SQL Server
            "getUrl",                   // PostgreSQL
            "getURL"                    // SQL Server
    };

    /**
     * The data source to use for obtaining connections to the database.
     *
     * @see #getDataSource()
     */
    private final DataSource source;

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
     * Only the main tables need to be specified; dependencies will be followed automatically.
     */
    private final GenericName[] tableNames;

    /**
     * Queries to expose as resources, or an empty array if none.
     */
    private final ResourceDefinition[] queries;

    /**
     * The metadata, created when first requested.
     */
    private Metadata metadata;

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
     * @see #lock(boolean)
     * @see Database#transactionLocks
     */
    private final ReadWriteLock transactionLocks;

    /**
     * Creates a new {@code SQLStore} for the given data source and tables, views or queries.
     * The given {@code connector} shall contain a {@link DataSource} instance.
     * Tables or views to include in the store are specified by the {@code resources} argument.
     * Only the main tables need to be specified; dependencies will be followed automatically.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (JDBC data source, <i>etc</i>).
     * @param  resources  tables, views or queries to include in this store.
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     *
     * @since 1.5
     */
    public SQLStore(final DataStoreProvider provider, final StorageConnector connector, final ResourceDefinition... resources)
            throws DataStoreException
    {
        super(provider, connector);
        ArgumentChecks.ensureNonEmpty("resources", resources);
        source      = connector.getStorageAs(DataSource.class);
        geomLibrary = connector.getOption(OptionKey.GEOMETRY_LIBRARY);
        customizer  = connector.getOption(SchemaModifier.OPTION);

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final GenericName[] tableNames = new GenericName[resources.length];
        int tableCount = 0;

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ResourceDefinition[] queries = new ResourceDefinition[resources.length];
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
        this.tableNames  = ArraysExt.resize(tableNames, tableCount);
        this.queries     = ArraysExt.resize(queries,    queryCount);
        transactionLocks = connector.getOption(InternalOptionKey.LOCKS);
    }

    /**
     * The data source to use for obtaining connections to the database.
     * This is the data source specified at construction time.
     *
     * @return the data source to use for obtaining connections to the database.
     * @since 1.5
     */
    public final DataSource getDataSource() {
        return source;
    }

    /**
     * Returns the parameters used to open this netCDF data store.
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
        if (tableNames != null) {
            pg.parameter(SQLStoreProvider.TABLES).setValue(tableNames);
        }
        if (queries != null) {
            final Map<GenericName,String> m = new LinkedHashMap<>();
            for (final ResourceDefinition query : queries) {
                m.put(query.getName(), query.query);
            }
            pg.parameter(SQLStoreProvider.QUERIES).setValue(m);
        }
        return Optional.of(pg);
    }

    /**
     * SQL data store root resource has no identifier.
     *
     * @return empty.
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.empty();
    }

    /**
     * Returns the database model, analyzing the database schema when first needed.
     * This method is thread-safe.
     */
    private Database<?> model() throws DataStoreException {
        Database<?> current = model;
        if (current == null) {
            synchronized (this) {
                try (Connection c = source.getConnection()) {
                    current = model(c);
                } catch (DataStoreException e) {
                    throw e;
                } catch (Exception e) {
                    throw new DataStoreException(Exceptions.unwrap(e));
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
    private Database<?> model(final Connection c) throws Exception {
        assert Thread.holdsLock(this);
        Database<?> current = model;
        if (current == null) {
            current = Database.create(this, source, c, geomLibrary, tableNames, queries, customizer, listeners, transactionLocks);
            model = current;
        }
        return current;
    }

    /**
     * Returns information about the dataset as a whole. The returned metadata object can contain information
     * such as the list of feature types.
     *
     * @return information about the dataset.
     * @throws DataStoreException if an error occurred while reading the data.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final var builder = new MetadataBuilder();
            builder.addSpatialRepresentation(SpatialRepresentationType.TEXT_TABLE);
            try (Connection c = source.getConnection()) {
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Database<?> model = model(c);
                model.metadata(c.getMetaData(), builder);
            } catch (DataStoreException e) {
                throw e;
            } catch (Exception e) {
                throw new DataStoreException(Exceptions.unwrap(e));
            }
            /*
             * Try to find a title from the data source description.
             */
            for (final String c : NAME_GETTERS) {
                try {
                    final Method method = source.getClass().getMethod(c);
                    if (method.getReturnType() == String.class) {
                        final String name = Strings.trimOrNull((String) method.invoke(source));
                        if (name != null) {
                            builder.addTitle(name);
                            break;
                        }
                    }
                } catch (NoSuchMethodException | SecurityException e) {
                    // Ignore - try the next method.
                } catch (ReflectiveOperationException e) {
                    throw new DataStoreException(Exceptions.unwrap(e));
                }
            }
            metadata = builder.buildAndFreeze();
        }
        return metadata;
    }

    /**
     * Returns the tables (feature sets) in this SQL store.
     * The list contains only the tables explicitly named at construction time.
     *
     * @return children resources that are components of this SQL store.
     * @throws DataStoreException if an error occurred while fetching the components.
     */
    @Override
    public Collection<Resource> components() throws DataStoreException {
        return model().tables();
    }

    /**
     * Searches for a resource identified by the given identifier.
     * The given identifier should match one of the table names.
     * It may be one of the tables named at construction time, or one of the dependencies.
     * The given name may be qualified with the schema name, or may be only the table name if there is no ambiguity.
     *
     * @param  identifier  identifier of the resource to fetch. Must be non-null.
     * @return resource associated to the given identifier (never {@code null}).
     * @throws IllegalNameException if no resource is found for the given identifier, or if more than one resource is found.
     * @throws DataStoreException if another kind of error occurred while searching resources.
     */
    @Override
    public FeatureSet findResource(final String identifier) throws DataStoreException {
        return model().findTable(this, identifier);
    }

    /**
     * Returns the coordinate reference system associated to the given identifier. The spatial reference
     * system identifiers (<abbr>SRID</abbr>) are the primary keys of the {@code "SPATIAL_REF_SYS"} table
     * (the name of that table may vary depending on which spatial schema standard is used).
     * Those identifiers are specific to each database and are not necessarily related to EPSG codes.
     * They should be considered as opaque identifiers.
     *
     * <h4>Undefined <abbr>CRS</abbr></h4>
     * Some standards such as Geopackage define 0 as "undefined geographic <abbr>CRS</abbr>" and -1 as
     * "undefined Cartesian <abbr>CRS</abbr>". This method returns {@code null} for all undefined <abbr>CRS</abbr>,
     * regardless their type. No default value is returned because this class cannot guess the datum and units of
     * measurement of an undefined <abbr>CRS</abbr>. All <abbr>SRID</abbr> equal or less than zero are considered
     * undefined.
     *
     * <h4>Axis order</h4>
     * Some standards such as Geopackage mandate (east, north) axis order. {@code SQLStore} uses the axis order
     * as defined in the <abbr>WKT</abbr> descriptions of the {@code "SPATIAL_REF_SYS"} table. No reordering is
     * applied. It is data producer's responsibility to provide definitions with the expected axis order.
     *
     * @param  srid  a primary key value of the {@code "SPATIAL_REF_SYS"} table.
     * @return the <abbr>CRS</abbr> associated to the given <abbr>SRID</abbr>, or {@code null} if the given
     *         <abbr>SRID</abbr> is a code explicitly associated to an undefined <abbr>CRS</abbr>.
     * @throws NoSuchDataException if no <abbr>CRS</abbr> is associated to the given <abbr>SRID</abbr>.
     * @throws DataStoreException if the query failed for another reason.
     *
     * @since 1.5
     */
    public CoordinateReferenceSystem findCRS(final int srid) throws DataStoreException {
        if (srid <= 0) {
            return null;
        }
        Database<?> database = model;
        CoordinateReferenceSystem crs;
        if (database == null || (crs = database.getCachedCRS(srid)) == null) {
            final Lock lock = lock(false);
            try {
                try (Connection c = source.getConnection()) {
                    if (database == null) {
                        synchronized (this) {
                            database = model(c);
                        }
                    }
                    try (InfoStatements info = database.createInfoStatements(c)) {
                        crs = info.fetchCRS(srid);
                    }
                } catch (DataStoreContentException e) {
                    throw new NoSuchDataException(e.getMessage(), e.getCause());
                } catch (DataStoreException e) {
                    throw e;
                } catch (Exception e) {
                    throw new DataStoreException(Exceptions.unwrap(e));
                }
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
        return crs;
    }

    /**
     * Returns the <abbr>SRID</abbr> associated to the given spatial reference system.
     * This method is the converse of {@link #findCRS(int)}.
     *
     * @param  crs       the CRS for which to find a SRID, or {@code null}.
     * @param  allowAdd  whether to allow the addition of a new row in the {@code "SPATIAL_REF_SYS"} table
     *                   if the given <abbr>CRS</abbr> is not found.
     * @return SRID for the given <abbr>CRS</abbr>, or 0 if the given <abbr>CRS</abbr> was null.
     * @throws DataStoreException if an <abbr>SQL</abbr> error, parsing error or other error occurred.
     *
     * @since 1.5
     */
    public int findSRID(final CoordinateReferenceSystem crs, final boolean allowAdd) throws DataStoreException {
        if (crs == null) {
            return 0;
        }
        Database<?> database = model;
        if (database != null) {
            Integer srid = database.getCachedSRID(crs);
            if (srid != null) return srid;
        }
        final int srid;
        final Lock lock = lock(allowAdd);
        try {
            try (Connection c = source.getConnection()) {
                if (database == null) {
                    synchronized (this) {
                        database = model(c);
                    }
                }
                if (!allowAdd && database.dialect.supportsReadOnlyUpdate()) {
                    c.setReadOnly(true);
                }
                try (InfoStatements info = database.createInfoStatements(c)) {
                    srid = info.findSRID(crs);
                }
            } catch (DataStoreException e) {
                throw e;
            } catch (Exception e) {
                throw new DataStoreException(Exceptions.unwrap(e));
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
        return srid;
    }

    /**
     * Returns a read or write lock, or {@code null} if none.
     * The call to {@link Lock#lock()} will be already performed.
     *
     * @param  write  {@code true} for the write lock, or {@code false} for the read lock.
     * @return the requested lock, or {@code null} if none.
     */
    private Lock lock(final boolean write) {
        if (transactionLocks == null) {
            return null;
        }
        final Lock lock = write ? transactionLocks.writeLock() : transactionLocks.readLock();
        lock.lock();
        return lock;
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
     * Clears the cache so that next operations will reload all needed information from the database.
     * This method can be invoked when the database content has been modified by a process other than
     * the methods in this class.
     *
     * @since 1.5
     */
    public synchronized void refresh() {
        model    = null;
        metadata = null;
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
        model    = null;
        metadata = null;
    }
}
