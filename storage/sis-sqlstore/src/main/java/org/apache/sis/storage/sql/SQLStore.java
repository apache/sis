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

import java.util.Collection;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.apache.sis.internal.sql.feature.Database;
import org.apache.sis.internal.sql.feature.Resources;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Exceptions;


/**
 * A data store capable to read and create features from a spatial database.
 * {@code SQLStore} requires a {@link DataSource} to be specified (indirectly) at construction time.
 * The {@code DataSource} should provide pooled connections, since connections will be frequently
 * opened and closed.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class SQLStore extends DataStore implements Aggregate {
    /**
     * The data source to use for obtaining connections to the database.
     */
    private final DataSource source;

    /**
     * The result of inspecting database schema for deriving {@link org.opengis.feature.FeatureType}s.
     * Created when first needed. May be discarded and recreated if the store needs a refresh.
     */
    private Database model;

    /**
     * Fully qualified names (including catalog and schema) of the tables to include in this store.
     */
    private final GenericName[] tableNames;

    /**
     * The metadata, created when first requested.
     */
    private Metadata metadata;

    /**
     * Creates a new instance for the given storage.
     * The given {@code connector} shall contain a {@link DataSource}.
     * The given table names shall be qualified names of 1, 2 or 3 components.
     * The name components are {@code <catalog>.<schema pattern>.<table pattern>} where:
     *
     * <ul>
     *   <li>{@code <catalog>}, if present, is the name of a catalog as stored in the database.</li>
     *   <li>{@code <schema pattern>}, if present, is the pattern of a schema.
     *       The pattern can use {@code '_'} and {@code '%'} wildcards characters.</li>
     *   <li>{@code <table pattern>} (mandatory) is the pattern of a table.
     *       The pattern can use {@code '_'} and {@code '%'} wildcards characters.</li>
     * </ul>
     *
     * Qualified table names can be created by the {@link SQLStoreProvider#createTableName(String, String, String)}
     * convenience method. Only the main tables need to be specified; dependencies will be followed automatically.
     *
     * @param  provider    the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector   information about the storage (JDBC data source, <i>etc</i>).
     * @param  tableNames  fully qualified names (including catalog and schema) of the tables to include in this store.
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    protected SQLStore(final SQLStoreProvider provider, final StorageConnector connector, GenericName... tableNames)
            throws DataStoreException
    {
        super(provider, connector);
        source = connector.getStorageAs(DataSource.class);
        ArgumentChecks.ensureNonNull("tableNames", tableNames);
        tableNames = tableNames.clone();
        for (int i=0; i<tableNames.length; i++) {
            final GenericName name = tableNames[i];
            ArgumentChecks.ensureNonNullElement("tableNames", i, tableNames);
            final int depth = name.depth();
            if (depth < 1 || depth > 3) {
                throw new IllegalNameException(Resources.format(Resources.Keys.IllegalQualifiedName_1, name));
            }
        }
        this.tableNames = tableNames;
    }

    /**
     * Returns the parameters used to open this netCDF data store.
     * The parameters are described by {@link SQLStoreProvider#getOpenParameters()} and contains
     * at least a parameter named {@value SQLStoreProvider#LOCATION} with a {@link DataSource} value.
     *
     * @return parameters used for opening this data store.
     */
    @Override
    public ParameterValueGroup getOpenParameters() {
        if (provider == null) {
            return null;
        }
        final ParameterValueGroup pg = provider.getOpenParameters().createValue();
        pg.parameter(SQLStoreProvider.LOCATION).setValue(source);
        pg.parameter(SQLStoreProvider.TABLES).setValue(tableNames);
        return pg;
    }

    /**
     * Returns the database model, analyzing the database schema when first needed.
     */
    private synchronized Database model() throws DataStoreException {
        if (model == null) {
            try (Connection c = source.getConnection()) {
                model = new Database(this, c, tableNames, listeners);
            } catch (SQLException e) {
                throw new DataStoreException(Exceptions.unwrap(e));
            }
        }
        return model;
    }

    /**
     * Returns the database model, analyzing the database schema when first needed.
     * This method performs the same work than {@link #model()}, but using an existing connection.
     *
     * @param c  connection to the database.
     */
    private synchronized Database model(final Connection c) throws DataStoreException, SQLException {
        if (model == null) {
            model = new Database(this, c, tableNames, listeners);
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
                final Database model = model(c);
                if (model.hasGeometry) {
                    builder.addSpatialRepresentation(SpatialRepresentationType.VECTOR);
                }
                model.listTables(c.getMetaData(), builder);
            } catch (SQLException e) {
                throw new DataStoreException(Exceptions.unwrap(e));
            }
            metadata = builder.build(true);
        }
        return metadata;
    }

    /**
     * Returns the resources (features or coverages) in this SQL store.
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
    public Resource findResource(final String identifier) throws DataStoreException {
        return model().findTable(this, identifier);
    }

    /**
     * Ignored in current implementation, since this resource produces no events.
     *
     * @param  <T>        {@inheritDoc}
     * @param  listener   {@inheritDoc}
     * @param  eventType  {@inheritDoc}
     */
    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    /**
     * Ignored in current implementation, since this resource produces no events.
     *
     * @param  <T>        {@inheritDoc}
     * @param  listener   {@inheritDoc}
     * @param  eventType  {@inheritDoc}
     */
    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    /**
     * Closes this SQL store and releases any underlying resources.
     *
     * @throws DataStoreException if an error occurred while closing the SQL store.
     */
    @Override
    public void close() throws DataStoreException {
    }
}
