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
import javax.sql.DataSource;
import java.sql.Connection;
import java.lang.reflect.Method;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.WarningEvent;
import org.apache.sis.internal.sql.feature.Database;
import org.apache.sis.internal.sql.feature.Resources;
import org.apache.sis.internal.sql.feature.SchemaModifier;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Exceptions;


/**
 * A data store capable to read and create features from a spatial database.
 * {@code SQLStore} requires a {@link DataSource} to be specified (indirectly) at construction time.
 * The {@code DataSource} should provide pooled connections, because {@code SQLStore} will frequently
 * opens and closes them.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.0
 * @module
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
     */
    private final DataSource source;

    /**
     * The library to use for creating geometric objects, or {@code null} for system default.
     */
    private final GeometryLibrary geomLibrary;

    /**
     * The result of inspecting database schema for deriving {@link org.opengis.feature.FeatureType}s.
     * Created when first needed. May be discarded and recreated if the store needs a refresh.
     */
    private Database<?> model;

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
     * @since 1.1
     */
    public SQLStore(final SQLStoreProvider provider, final StorageConnector connector, final ResourceDefinition... resources)
            throws DataStoreException
    {
        super(provider, connector);
        ArgumentChecks.ensureNonEmpty("resources", resources);
        source      = connector.getStorageAs(DataSource.class);
        geomLibrary = connector.getOption(OptionKey.GEOMETRY_LIBRARY);
        customizer  = connector.getOption(SchemaModifier.OPTION);
        final GenericName[] tableNames = new GenericName[resources.length];
        final ResourceDefinition[] queries = new ResourceDefinition[resources.length];
        int tableCount = 0;
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
        if (getClass() == SQLStore.class) {
            listeners.useReadOnlyEvents();
        }
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
     */
    private synchronized Database<?> model() throws DataStoreException {
        if (model == null) {
            try (Connection c = source.getConnection()) {
                model = Database.create(this, source, c, geomLibrary, tableNames, queries, customizer, listeners);
            } catch (DataStoreException e) {
                throw e;
            } catch (Exception e) {
                throw new DataStoreException(Exceptions.unwrap(e));
            }
        }
        return model;
    }

    /**
     * Returns the database model, analyzing the database schema when first needed.
     * This method performs the same work than {@link #model()}, but using an existing connection.
     * Callers must own a synchronization lock on {@code this}.
     *
     * @param c  connection to the database.
     */
    private Database<?> model(final Connection c) throws Exception {
        if (model == null) {
            model = Database.create(this, source, c, geomLibrary, tableNames, queries, customizer, listeners);
        }
        return model;
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
            final MetadataBuilder builder = new MetadataBuilder();
            builder.addSpatialRepresentation(SpatialRepresentationType.TEXT_TABLE);
            try (Connection c = source.getConnection()) {
                final Database<?> model = model(c);
                if (model.hasGeometry()) {
                    builder.addSpatialRepresentation(SpatialRepresentationType.VECTOR);
                }
                if (model.hasRaster()) {
                    builder.addSpatialRepresentation(SpatialRepresentationType.GRID);
                }
                model.listTables(c.getMetaData(), builder);
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
    public Collection<FeatureSet> components() throws DataStoreException {
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
     * Registers a listener to notify when the specified kind of event occurs in this data store.
     * The current implementation of this data store can emit only {@link WarningEvent}s;
     * any listener specified for another kind of events will be ignored.
     */
    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        // If an argument is null, we let the parent class throws (indirectly) NullArgumentException.
        if (listener == null || eventType == null || eventType.isAssignableFrom(WarningEvent.class)) {
            super.addListener(eventType, listener);
        }
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
        model = null;
    }
}
