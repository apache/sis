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
package org.apache.sis.internal.sql.feature;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.opengis.util.GenericName;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.FeatureNaming;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.Debug;


/**
 * Represent the structure of features in the database.
 * The work done here is similar to reverse engineering.
 * Instances of this class are thread-safe after construction;
 * if the database schema changes, then a new {@code Database} instance shall be created.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class Database {
    /**
     * The SQL wildcard for any characters. A string containing only this wildcard
     * means "any value" and can sometime be replaced by {@code null}.
     */
    public static final String WILDCARD = "%";

    /**
     * All tables known to this {@code Database}. Populated in the constructor,
     * and shall not be modified after construction for preserving thread-safety.
     */
    private final FeatureNaming<Table> tablesByNames;

    /**
     * All tables known to this {@code Database} in declaration order.
     * This table contains only the tables specified at construction time, not the dependencies.
     */
    private final Table[] tables;

    /**
     * Functions that may be specific to the geospatial database in use.
     */
    final SpatialFunctions functions;

    /**
     * {@code true} if this database contains at least one geometry column.
     */
    public final boolean hasGeometry;

    /**
     * Creates a new model about the specified tables in a database.
     * This constructor requires a list of tables to include in the model,
     * but this list should not include the dependencies; this constructor
     * will follow foreigner keys automatically.
     *
     * <p>The table names shall be qualified names of 1, 2 or 3 components.
     * The components are {@code <catalog>.<schema pattern>.<table pattern>} where:</p>
     *
     * <ul>
     *   <li>{@code <catalog>}, if present, shall be the name of a catalog as it is stored in the database.</li>
     *   <li>{@code <schema pattern>}, if present, shall be the pattern of a schema.
     *       The pattern can use {@code '_'} and {@code '%'} wildcards characters.</li>
     *   <li>{@code <table pattern>} (mandatory) shall be the pattern of a table.
     *       The pattern can use {@code '_'} and {@code '%'} wildcards characters.</li>
     * </ul>
     *
     * @param  store        the data store for which we are creating a model. Used only in case of error.
     * @param  connection   connection to the database. Sometime the caller already has a connection at hand.
     * @param  source       provider of (pooled) connections to the database. Specified by users at construction time.
     * @param  tableNames   qualified name of the tables. Specified by users at construction time.
     * @param  listeners    where to send the warnings. This is the value of {@code store.listeners}.
     * @throws SQLException if a database error occurred while reading metadata.
     * @throws DataStoreException if a logical error occurred while analyzing the database structure.
     */
    public Database(final DataStore store, final Connection connection, final DataSource source,
            final GenericName[] tableNames, final StoreListeners listeners)
            throws SQLException, DataStoreException
    {
        final Analyzer analyzer = new Analyzer(source, connection, listeners, store.getLocale());
        final String[] tableTypes = getTableTypes(analyzer.metadata);
        final Set<TableReference> declared = new LinkedHashSet<>();
        for (final GenericName tableName : tableNames) {
            final String[] names = TableReference.splitName(tableName);
            try (ResultSet reflect = analyzer.metadata.getTables(names[2], names[1], names[0], tableTypes)) {
                while (reflect.next()) {
                    final String table = analyzer.getUniqueString(reflect, Reflection.TABLE_NAME);
                    if (analyzer.isIgnoredTable(table)) {
                        continue;
                    }
                    declared.add(new TableReference(
                            analyzer.getUniqueString(reflect, Reflection.TABLE_CAT),
                            analyzer.getUniqueString(reflect, Reflection.TABLE_SCHEM), table,
                            analyzer.getUniqueString(reflect, Reflection.REMARKS)));
                }
            }
        }
        /*
         * At this point we got the list of tables requested by the user. Now create the Table objects for each
         * specified name. During this iteration, we may discover new tables to analyze because of dependencies
         * (foreigner keys).
         */
        final List<Table> tableList;
        tableList = new ArrayList<>(tableNames.length);
        for (final TableReference reference : declared) {
            // Adds only the table explicitly required by the user.
            tableList.add(analyzer.table(reference, reference.getName(analyzer), null));
        }
        /*
         * At this point we finished to create the table explicitly requested by the users.
         * Register all tables only at this point, because other tables (dependencies) may
         * have been analyzed as a side-effect of above loop.
         */
        boolean hasGeometry = false;
        tablesByNames = new FeatureNaming<>();
        for (final Table table : analyzer.finish()) {
            tablesByNames.add(store, table.featureType.getName(), table);
            hasGeometry |= table.hasGeometry;
        }
        this.tables      = tableList.toArray(new Table[tableList.size()]);
        this.functions   = analyzer.functions;
        this.hasGeometry = hasGeometry;
    }

    /**
     * Returns the "TABLE" and "VIEW" keywords for table type, with unsupported keywords omitted.
     */
    private static String[] getTableTypes(final DatabaseMetaData metadata) throws SQLException {
        final Set<String> types = new HashSet<>(4);
        try (ResultSet reflect = metadata.getTableTypes()) {
            while (reflect.next()) {
                final String type = reflect.getString(Reflection.TABLE_TYPE);
                if ("TABLE".equalsIgnoreCase(type) || "VIEW".equalsIgnoreCase(type)) {
                    types.add(type);
                }
            }
        }
        return types.toArray(new String[types.size()]);
    }

    /**
     * Stores information about tables in the given metadata.
     * Only tables explicitly requested by the user are listed.
     *
     * @param  metadata  information about the database.
     * @param  builder   where to add information about the tables.
     * @throws SQLException if an error occurred while fetching table information.
     */
    public final void listTables(final DatabaseMetaData metadata, final MetadataBuilder builder) throws SQLException {
        for (final Table table : tables) {
            builder.addFeatureType(table.featureType, table.countRows(metadata, false));
        }
    }

    /**
     * Returns all tables in declaration order.
     * The list contains only the tables explicitly requested at construction time.
     *
     * @return all tables in an unmodifiable list.
     */
    public final List<Resource> tables() {
        return UnmodifiableArrayList.wrap(tables);
    }

    /**
     * Returns the table for the given name.
     * The given name may be one of the tables specified at construction time, or one of its dependencies.
     *
     * @param  store  the data store for which we are fetching a table. Used only in case of error.
     * @param  name   name of the table to fetch.
     * @return the table (never null).
     * @throws IllegalNameException if no table of the given name is found or if the name is ambiguous.
     */
    public final FeatureSet findTable(final DataStore store, final String name) throws IllegalNameException {
        return tablesByNames.get(store, name);
    }

    /**
     * Creates a tree representation of this table for debugging purpose.
     *
     * @param  parent  the parent node where to add the tree representation.
     */
    @Debug
    final void appendTo(final TreeTable.Node parent) {
        for (final Table child : tables) {
            child.appendTo(parent);
        }
    }

    /**
     * Formats a graphical representation of this database for debugging purpose. This representation can
     * be printed to the {@linkplain System#out standard output stream} (for example) if the output device
     * uses a monospaced font and supports Unicode.
     *
     * @return string representation of this database.
     */
    @Override
    public String toString() {
        return TableReference.toString(this, (n) -> appendTo(n));
    }

    /**
     * Acquire a connection over parent database, forcing a few parameters to ensure optimal read performance and
     * limiting user rights :
     * <ul>
     *     <li>{@link Connection#setAutoCommit(boolean) auto-commit} to false</li>
     *     <li>{@link Connection#setReadOnly(boolean) querying read-only}</li>
     * </ul>
     *
     * @param source Database pointer to create connection from.
     * @return A new connection to database, with deactivated auto-commit.
     * @throws SQLException If we cannot create a new connection. See {@link DataSource#getConnection()} for details.
     */
    public static Connection connectReadOnly(final DataSource source) throws SQLException {
        final Connection c = source.getConnection();
        try {
            c.setAutoCommit(false);
            c.setReadOnly(true);
        } catch (SQLException e) {
            try {
                c.close();
            } catch (RuntimeException | SQLException bis) {
                e.addSuppressed(bis);
            }
            throw e;
        }
        return c;
    }
}
