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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.metadata.sql.Dialect;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.storage.DataStore;


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
     * Relations to other tables found while doing introspection on a table.
     * The value tells whether the table in the relation has already been analyzed.
     * Only the catalog, schema and table names are taken in account for the keys in this map.
     */
    private final Map<TableReference,Boolean> dependencies;

    /**
     * Iterator over {@link #dependencies} entries, or {@code null} if none.
     * This field may be set to {@code null} in the middle of an iteration if
     * the {@link #dependencies} map is modified concurrently.
     */
    private Iterator<Map.Entry<TableReference,Boolean>> depIter;

    /**
     * Warnings found while analyzing a database structure. Duplicated warnings are omitted.
     */
    private final Set<InternationalString> warnings;

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
        dependencies = new LinkedHashMap<>();
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
     * Returns a namespace for the given catalog and schema names.
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
                namespace = nameFactory.createNameSpace(name, TableReference.NAMESPACE_PROPERTIES);
            } else {
                namespace = null;
            }
            this.catalog = catalog;
            this.schema  = schema;
        }
        return namespace;
    }

    /**
     * Declares that a relation to a foreigner table has been found. Only the catalog, schema and table names
     * are taken in account. If a dependency for the same table has already been declared before or if that
     * table has already been analyzed, then this method does nothing. Otherwise if the table has not yet
     * been analyzed, then this method remembers that the foreigner table will need to be analyzed later.
     */
    final void addDependency(final TableReference foreigner) {
        if (dependencies.putIfAbsent(foreigner, Boolean.FALSE) == null) {
            depIter = null;         // Will need to fetch a new iterator.
        }
    }

    /**
     * Returns the next table to visit, or {@code null} if there is no more.
     */
    final TableReference nextDependency() {
        if (depIter == null) {
            depIter = dependencies.entrySet().iterator();
        }
        while (depIter.hasNext()) {
            final Map.Entry<TableReference,Boolean> e = depIter.next();
            if (!e.setValue(Boolean.TRUE)) {
                return e.getKey();
            }
        }
        return null;
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
}
