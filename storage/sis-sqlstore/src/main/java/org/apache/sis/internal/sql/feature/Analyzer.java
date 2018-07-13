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
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.util.GenericName;
import org.apache.sis.internal.metadata.sql.Dialect;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.util.resources.ResourceInternationalString;
import org.apache.sis.util.resources.Errors;


/**
 * Helper methods for creating {@code FeatureType}s from database structure.
 * An instance of this class is created temporarily when starting the analysis
 * of a database structure, and discarded once the analysis is finished.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Analyzer {
    /**
     * Information about the database as a whole.
     * Used for fetching tables, columns, primary keys <i>etc.</i>
     */
    final DatabaseMetaData metadata;

    /**
     * Functions that may be specific to the geospatial database in use.
     */
    final SpatialFunctions functions;

    /**
     * The factory for creating {@code FeatureType} names.
     */
    final NameFactory nameFactory;

    /**
     * The string to insert before wildcard characters ({@code '_'} or {@code '%'}) to escape.
     * This is used by {@link #escape(String)} before to pass argument values (e.g. table name)
     * to {@link DatabaseMetaData} methods expecting a pattern.
     */
    private final String escape;

    /**
     * Names of tables to ignore when inspecting a database schema.
     * Those tables are used for database internal working (for example by PostGIS).
     */
    private final Set<String> ignoredTables;

    /**
     * All tables created by analysis of the database structure. A {@code null} value means that the table
     * is in process of being created. This may happen if there is cyclic dependencies between tables.
     */
    private final Map<GenericName,Table> tables;

    /**
     * Warnings found while analyzing a database structure. Duplicated warnings are omitted.
     */
    private final Set<ResourceInternationalString> warnings;

    /**
     * Where to send warnings after we finished to collect them, or when reading the feature instances.
     */
    final WarningListeners<DataStore> listeners;

    /**
     * The locale for warning messages.
     */
    final Locale locale;

    /**
     * The last catalog and schema used for creating {@link #namespace}.
     * Used for determining if {@link #namespace} is still valid.
     */
    private transient String catalog, schema;

    /**
     * The namespace created with {@link #catalog} and {@link #schema}.
     */
    private transient NameSpace namespace;

    /**
     * Creates a new analyzer for the database described by given metadata.
     */
    Analyzer(final DatabaseMetaData metadata, final WarningListeners<DataStore> listeners, final Locale locale)
            throws SQLException
    {
        this.metadata    = metadata;
        this.listeners   = listeners;
        this.locale      = locale;
        this.escape      = metadata.getSearchStringEscape();
        this.functions   = new SpatialFunctions(metadata);
        this.nameFactory = DefaultFactories.forBuildin(NameFactory.class);
        /*
         * The following tables are defined by ISO 19125 / OGC Simple feature access part 2.
         * Note that the standard specified those names in upper-case letters, which is also
         * the default case specified by the SQL standard.  However some databases use lower
         * cases instead.
         */
        String crs  = "SPATIAL_REF_SYS";
        String geom = "GEOMETRY_COLUMNS";
        if (metadata.storesLowerCaseIdentifiers()) {
            crs  = crs .toLowerCase(Locale.US).intern();
            geom = geom.toLowerCase(Locale.US).intern();
        }
        ignoredTables = new HashSet<>(4);
        ignoredTables.add(crs);
        ignoredTables.add(geom);
        final Dialect dialect = Dialect.guess(metadata);
        if (dialect == Dialect.POSTGRESQL) {
            ignoredTables.add("geography_columns");     // Postgis 1+
            ignoredTables.add("raster_columns");        // Postgis 2
            ignoredTables.add("raster_overviews");
        }
        /*
         * Information to be collected during table analysis.
         */
        tables   = new HashMap<>();
        warnings = new LinkedHashSet<>();
    }

    /**
     * Returns the given pattern with {@code '_'} and {@code '%'} characters escaped by the database-specific
     * escape characters. This method should be invoked for escaping the values of all {@link DatabaseMetaData}
     * method arguments with a name ending by {@code "Pattern"}. Note that not all arguments are pattern; please
     * checks carefully {@link DatabaseMetaData} javadoc for each method.
     *
     * <div class="note"><b>Example:</b> if a method expects an argument named {@code tableNamePattern},
     * then that argument value should be escaped. But if the argument name is only {@code tableName},
     * then the value should not be escaped.</div>
     */
    final String escape(final String pattern) {
        if (pattern != null) {
            StringBuilder buffer = null;
            for (int i = pattern.length(); --i >= 0;) {
                final char c = pattern.charAt(i);
                if (c == '_' || c == '%') {
                    if (buffer == null) {
                        buffer = new StringBuilder(pattern);
                    }
                    buffer.insert(i, escape);
                }
            }
            if (buffer != null) {
                return buffer.toString();
            }
        }
        return pattern;
    }

    /**
     * Returns whether a table is reserved for database internal working.
     * If this method returns {@code false}, then the given table is a candidate
     * for use as a {@code FeatureType}.
     *
     * @param  name  database table name to test (case sensitive).
     * @return {@code true} if the named table should be ignored when looking for feature types.
     */
    final boolean isIgnoredTable(final String name) {
        return ignoredTables.contains(name);
    }

    /**
     * Returns a namespace for the given catalog and schema names, or {@code null} if all arguments are null.
     * The namespace sets the name separator to {@code '.'} instead of {@code ':'}.
     */
    final NameSpace namespace(final String catalog, final String schema) {
        if (!Objects.equals(this.schema, schema) || !Objects.equals(this.catalog, catalog)) {
            if (schema != null) {
                final GenericName name;
                if (catalog == null) {
                    name = nameFactory.createLocalName(null, schema);
                } else {
                    name = nameFactory.createGenericName(null, catalog, schema);
                }
                namespace = nameFactory.createNameSpace(name, Collections.singletonMap("separator", "."));
            } else {
                namespace = null;
            }
            this.catalog = catalog;
            this.schema  = schema;
        }
        return namespace;
    }

    /**
     * Returns the feature of the given name if it exists, or creates it otherwise.
     * This method may be invoked recursively if the table to create as a dependency
     * to another table. If a cyclic dependency is detected, then this method return
     * {@code null} for one of the tables.
     *
     * @param  id    identification of the table to create.
     * @param  name  the value of {@code id.getName(analyzer)}
     *               (as an argument for avoiding re-computation when already known by the caller).
     * @return the table, or {@code null} if there is a cyclic dependency and the table of the given
     *         name is already in process of being created.
     */
    final Table analyze(final TableReference id, final GenericName name) throws SQLException, DataStoreException {
        Table table = tables.get(name);
        if (table == null && !tables.containsKey(name)) {
            tables.put(name, null);                       // Mark the feature as in process of being created.
            table = new Table(this, id);
            if (tables.put(name, table) != null) {
                // Should never happen. If thrown, we have a bug (e.g. synchronization) in this package.
                throw new DataStoreException(Errors.format(Errors.Keys.UnexpectedChange_1, name));
            }
        }
        return table;
    }

    /**
     * Reports a warning. Duplicated warnings will be ignored.
     *
     * @param  key       one of {@link Resources.Keys} values.
     * @param  argument  the value to substitute to {0} tag in the warning message.
     */
    final void warning(final short key, final Object argument) {
        warnings.add(Resources.formatInternational(key, argument));
    }

    /**
     * Invoked after we finished to create all tables. This method flush the warnings
     * (omitting duplicated warnings), then returns all tables including dependencies.
     */
    final Collection<Table> finish() {
        for (final ResourceInternationalString warning : warnings) {
            final LogRecord record = warning.toLogRecord(Level.WARNING);
            record.setSourceClassName(SQLStore.class.getName());
            record.setSourceMethodName("components");                // Main public API trigging the database analysis.
            listeners.warning(record);
        }
        return tables.values();
    }
}
