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
import java.util.Objects;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.CanNotProbeException;
import org.apache.sis.storage.IllegalOpenParameterException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.sql.feature.Resources;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.UnconvertibleObjectException;
import static org.apache.sis.storage.sql.feature.Database.WILDCARD;


/**
 * Provider of {@code SQLStore} instances.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.0
 */
@StoreMetadata(formatName    = SQLStoreProvider.NAME,
               capabilities  = Capability.READ,
               resourceTypes = {Aggregate.class, FeatureSet.class})
public class SQLStoreProvider extends DataStoreProvider {
    /**
     * The format name.
     */
    static final String NAME = "SQL";

    /**
     * The logger used by SQL stores.
     *
     * @see #getLogger()
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.sql");

    /**
     * Name of the parameter for the list of qualified table names.
     * Values of this parameter are {@code GenericName[]}.
     */
    static final String TABLES = "tables";

    /**
     * Name of the parameter for the list of queries.
     * Values of this parameter are {@code Map<GenericName,String>}.
     * Strings are also accepted as keys for convenience.
     */
    static final String QUERIES = "queries";

    /**
     * Description of the {@value #LOCATION} parameter.
     * This parameter is mandatory.
     *
     * @since 1.1
     */
    public static final ParameterDescriptor<DataSource> SOURCE_PARAM;

    /**
     * Description of the parameter providing the list of tables or views to include as resources in the
     * {@link SimpleFeatureStore}. At least one of {@code TABLES_PARAM} or {@link #QUERIES_PARAM} must be provided.
     *
     * @since 1.1
     */
    public static final ParameterDescriptor<GenericName[]> TABLES_PARAM;

    /**
     * Description of the parameter providing the queries to include as resources in the {@link SimpleFeatureStore}.
     * Map keys are the resource names as {@link GenericName} or {@link String} instances.
     * Values are SQL statements (as {@link String} instances) to execute when the associated resource is requested.
     * At least one of {@link #TABLES_PARAM} or {@code QUERIES_PARAM} must be provided.
     *
     * @since 1.1
     */
    public static final ParameterDescriptor<Map<?,?>> QUERIES_PARAM;

    /**
     * The parameter descriptor to be returned by {@link #getOpenParameters()}.
     */
    private static final ParameterDescriptorGroup OPEN_DESCRIPTOR;
    static {
        @SuppressWarnings("unchecked")
        final Class<Map<?,?>> map = (Class) Map.class;
        final ParameterBuilder builder = new ParameterBuilder();
        SOURCE_PARAM = builder.addName(LOCATION).setRequired(true)
                              .setDescription(Resources.formatInternational(Resources.Keys.DataSource))
                              .create(DataSource.class, null);
        TABLES_PARAM = builder.addName(TABLES).setRequired(false)
                              .setDescription(Resources.formatInternational(Resources.Keys.QualifiedTableNames))
                              .create(GenericName[].class, null);
        QUERIES_PARAM = builder.addName(QUERIES)
                              .setDescription(Resources.formatInternational(Resources.Keys.MappedSQLQueries))
                              .create(map, null);
        OPEN_DESCRIPTOR = builder.addName(NAME).createGroup(SOURCE_PARAM, TABLES_PARAM, QUERIES_PARAM);
    }

    /**
     * Creates a new provider.
     */
    public SQLStoreProvider() {
    }

    /**
     * Create a qualified table name. The returned {@code GenericName} can be any of the following:
     *
     * <ul>
     *   <li>{@code catalog.schemaPattern.tablePattern}</li>
     *   <li>{@code schemaPattern.tablePattern}</li>
     *   <li>{@code tablePattern}</li>
     * </ul>
     *
     * The schema and table names (but not the catalog) can contain SQL wildcard characters:
     * {@code '_'} matches any single character and {@code '%'} matches any sequence of characters.
     *
     * @param  catalog        name of a catalog as it is stored in the database, or {@code null} for any catalog.
     * @param  schemaPattern  pattern (with {@code '_'} and {@code '%'} wildcards) of a schema, or {@code null} for any.
     * @param  tablePattern   pattern (with {@code '_'} and {@code '%'} wildcards) of a table.
     * @return the fully qualified name.
     */
    public static GenericName createTableName(final String catalog, String schemaPattern, final String tablePattern) {
        return ResourceDefinition.table(catalog, schemaPattern, tablePattern).getName();
    }

    /**
     * Returns a generic name for this data store, used mostly in warnings or error messages.
     *
     * @return a short name or abbreviation for the data format.
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * Returns a description of all parameters accepted by this provider for opening a connection to the database.
     * The group contains {@link #SOURCE_PARAM}, {@link #TABLES_PARAM} and {@link #QUERIES_PARAM}.
     *
     * @return description of available parameters for opening a connection to a database.
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return OPEN_DESCRIPTOR;
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be supported by {@link SQLStore}.
     * Returning {@code SUPPORTED} from this method does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the connection.
     *
     * @param  connector  information about the storage (data source).
     * @return {@code SUPPORTED} if the given storage seems to be usable by {@code SQLStore} instances.
     * @throws DataStoreException if an SQL error occurred.
     */
    @Override
    @SuppressWarnings("try")
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        final DataSource ds = connector.getStorageAs(DataSource.class);
        if (ds != null) {
            try (Connection c = ds.getConnection()) {
                return ProbeResult.SUPPORTED;
            } catch (SQLException e) {
                final String state = e.getSQLState();
                if (!("08001".equals(state) || "3D000".equals(state))) {
                    throw new CanNotProbeException(this, connector, Exceptions.unwrap(e));
                }
                // SQL-client unable to establish SQL-connection, or invalid catalog name.
            }
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Returns a {@link SQLStore} implementation associated with this provider.
     * The store will provide resources for all tables and views in all schemas and catalogs.
     *
     * @param  connector  information about the storage (data source).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        return new SimpleFeatureStore(this, connector, ResourceDefinition.table(WILDCARD));
    }

    /**
     * Returns a data store implementation associated with this provider for the given parameters.
     *
     * @param  parameters  opening parameters as defined by {@link #getOpenParameters()}.
     * @return a data store implementation associated with this provider for the given parameters.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final ParameterValueGroup parameters) throws DataStoreException {
        try {
            final Parameters p = Parameters.castOrWrap(Objects.requireNonNull(parameters));
            final StorageConnector connector = new StorageConnector(p.getValue(SOURCE_PARAM));
            final GenericName[] tableNames = p.getValue(TABLES_PARAM);
            final Map<?,?> queries = p.getValue(QUERIES_PARAM);
            return new SimpleFeatureStore(this, connector, ResourceDefinition.wrap(tableNames, queries));
        } catch (ParameterNotFoundException | UnconvertibleObjectException e) {
            throw new IllegalOpenParameterException(e.getMessage(), e);
        }
    }

    /**
     * Returns the logger used by SQL stores.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
