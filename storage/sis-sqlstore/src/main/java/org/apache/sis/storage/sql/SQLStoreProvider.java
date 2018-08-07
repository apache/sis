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
import java.util.HashMap;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.sql.feature.Resources;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalOpenParameterException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.ArgumentChecks;

import static org.apache.sis.internal.sql.feature.Database.WILDCARD;


/**
 * Provider of {@code SQLStore} instances.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class SQLStoreProvider extends DataStoreProvider {
    /**
     * The format name.
     */
    private static final String NAME = "SQL";

    /**
     * Name of the parameter for the list of qualified table names.
     * Values of this parameter are {@code GenericName[]}.
     */
    static final String TABLES = "tables";

    /**
     * Description of the {@value #LOCATION} parameter.
     */
    private static final ParameterDescriptor<DataSource> SOURCE_PARAM;

    /**
     * Description of the {@code "tables"} parameter.
     */
    private static final ParameterDescriptor<GenericName[]> TABLES_PARAM;

    /**
     * The parameter descriptor to be returned by {@link #getOpenParameters()}.
     */
    private static final ParameterDescriptorGroup OPEN_DESCRIPTOR;
    static {
        final ParameterBuilder builder = new ParameterBuilder();
        SOURCE_PARAM = builder.addName(LOCATION).setRequired(true)
                              .setDescription(Resources.formatInternational(Resources.Keys.DataSource))
                              .create(DataSource.class, null);
        TABLES_PARAM = builder.addName(TABLES).setRequired(true)
                              .setDescription(Resources.formatInternational(Resources.Keys.QualifiedTableNames))
                              .create(GenericName[].class, null);
        OPEN_DESCRIPTOR = builder.addName(NAME).createGroup(SOURCE_PARAM, TABLES_PARAM);
    }

    /**
     * The namespace for table names, created when first needed.
     * Used for specifying the name separator, which is {@code '.'}.
     */
    private static volatile NameSpace tableNS;

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
    @SuppressWarnings("fallthrough")
    public static GenericName createTableName(final String catalog, String schemaPattern, final String tablePattern) {
        ArgumentChecks.ensureNonNull("tablePattern", tablePattern);
        final int numParts;
        if (catalog != null) {
            numParts = 3;
            if (schemaPattern == null) {
                schemaPattern = WILDCARD;
            }
        } else if (schemaPattern != null && !schemaPattern.equals(WILDCARD)) {
            numParts = 2;
        } else {
            numParts = 1;
        }
        final String[] names = new String[numParts];
        int i = 0;
        switch (numParts) {
            default: throw new AssertionError(numParts);
            case 3: names[i++] = catalog;           // Fall through
            case 2: names[i++] = schemaPattern;     // Fall through
            case 1: names[i]   = tablePattern;
        }
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        NameSpace ns = tableNS;
        if (ns == null) {
            final Map<String,String> properties = new HashMap<>(4);     // TODO: use Map.of with JDK9.
            properties.put("separator",      ".");
            properties.put("separator.head", ":");
            tableNS = ns = factory.createNameSpace(factory.createLocalName(null, "database"), properties);
        }
        return factory.createGenericName(ns, names);
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
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        final DataSource ds = connector.getStorageAs(DataSource.class);
        if (ds != null) {
            try (Connection c = ds.getConnection()) {
                return ProbeResult.SUPPORTED;
            } catch (SQLException e) {
                final String state = e.getSQLState();
                if (!"08001".equals(state) || !"3D000".equals(state)) {
                    throw new DataStoreException(e);
                }
            }
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Returns a {@link SQLStore} implementation associated with this provider.
     *
     * @param  connector  information about the storage (data source).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        return new SQLStore(this, connector, createTableName(null, null, WILDCARD));
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
        ArgumentChecks.ensureNonNull("parameters", parameters);
        try {
            final Parameters p = Parameters.castOrWrap(parameters);
            final StorageConnector connector = new StorageConnector(p.getValue(SOURCE_PARAM));
            final GenericName[] tableNames = p.getValue(TABLES_PARAM);
            return new SQLStore(this, connector, tableNames);
        } catch (ParameterNotFoundException | UnconvertibleObjectException e) {
            throw new IllegalOpenParameterException(e.getMessage(), e);
        }
    }
}
