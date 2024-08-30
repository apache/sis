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

import java.util.Optional;
import java.util.Collection;
import java.util.LinkedHashMap;
import javax.sql.DataSource;
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.util.ArgumentChecks;


/**
 * A concrete data store capable to read and write features from/to a spatial <abbr>SQL</abbr> database.
 * All resources created by this class are {@link FeatureSet}s.
 * The data are specified by two main arguments given at construction time:
 *
 * <ul>
 *   <li>A {@link DataSource} (specified indirectly) providing connections to the database. While not mandatory,
 *       a pooled data source is recommended because {@code SimpleFeatureStore} may open and close connections
 *       many times.</li>
 *   <li>A list of tables, views or queries to view as {@link FeatureSet}s. This list is provided by
 *       {@link ResourceDefinition} objects. Only the main tables need to be specified. Dependencies
 *       inferred by foreigner keys will be followed automatically.</li>
 * </ul>
 *
 * Despite the {@code SimpleFeatureStore} class name, this class supports <dfn>complex features</dfn>,
 * i.e. features having associations to other features.
 * The associations are discovered automatically by following the foreigner keys.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see <a href="https://www.ogc.org/standards/sfs">OGC Simple feature access - Part 2: SQL option</a>
 *
 * @since 1.5
 */
public class SimpleFeatureStore extends SQLStore {
    /**
     * Creates a new {@code SimpleFeatureStore} for the given data source and tables, views or queries.
     * The given {@code connector} shall contain a {@link DataSource} instance.
     * Tables or views to include in the store are specified by the {@code resources} argument.
     * Only the main tables need to be specified, as dependencies will be followed automatically.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (JDBC data source, <i>etc</i>).
     * @param  resources  tables, views or queries to include in this store.
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    @SuppressWarnings("this-escape")    // `setModelSources(…)` is final and part of initialization.
    public SimpleFeatureStore(final DataStoreProvider provider, final StorageConnector connector, final ResourceDefinition... resources)
            throws DataStoreException
    {
        super(provider, connector);
        ArgumentChecks.ensureNonEmpty("resources", resources);
        setModelSources(resources);
    }

    /**
     * Returns parameters that can be used for opening this Simple Features data store.
     * The parameters are described by {@link SQLStoreProvider#getOpenParameters()} and
     * can contain some or all of the following:
     *
     * <ul>
     *   <li>A parameter named {@value SQLStoreProvider#LOCATION} with a {@link DataSource} value.</li>
     *   <li>{@link SQLStoreProvider#TABLES_PARAM} with {@link ResourceDefinition}s specified at construction time for tables.</li>
     *   <li>{@link SQLStoreProvider#QUERIES_PARAM} with {@link ResourceDefinition}s specified at construction time for queries.</li>
     * </ul>
     *
     * @return parameters used for opening this data store.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        final Optional<ParameterValueGroup> opg = super.getOpenParameters();
        opg.ifPresent((pg) -> {
            final GenericName[] tableNames = tableNames();
            if (tableNames.length != 0) {
                pg.parameter(SQLStoreProvider.TABLES).setValue(tableNames);
            }
            final ResourceDefinition[] queries = queries();
            if (queries.length != 0) {
                final var m = new LinkedHashMap<GenericName,String>();
                for (final ResourceDefinition query : queries) {
                    m.put(query.getName(), query.query);
                }
                pg.parameter(SQLStoreProvider.QUERIES).setValue(m);
            }
        });
        return opg;
    }

    /**
     * Returns the tables (feature sets) in this SQL store.
     * The collection contains only the tables matching a {@link ResourceDefinition} given at construction time.
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
     * The given identifier shall match the name of one of the tables, views or queries inferred at construction time.
     * It may be a table named explicitly at construction time, or a dependency inferred by following foreigner keys.
     * The given identifier may be qualified with the schema name, or may be only the table name if there is no ambiguity.
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
     * Returns the resource definitions equivalent to the ones specified at construction time.
     * This method is defined for completness, but is not used by {@code SimpleFeatureStore}.
     *
     * @param  dao  ignored.
     */
    @Override
    protected ResourceDefinition[] readResourceDefinitions(final DataAccess dao) {
        final GenericName[] tableNames = tableNames();
        final ResourceDefinition[] queries = queries();
        final var definitions = new ResourceDefinition[tableNames.length + queries.length];
        for (int i=0; i<tableNames.length; i++) {
            definitions[i] = new ResourceDefinition(tableNames[i], null);
        }
        System.arraycopy(queries, 0, definitions, tableNames.length, queries.length);
        return definitions;
    }

    /**
     * Clears the cache so that next operations will recreate the list
     * of tables from the patterns specified at construction time.
     *
     * @hidden
     */
    @Override
    public synchronized void refresh() {
        clearModel();
    }
}
