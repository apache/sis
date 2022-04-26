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
package org.apache.sis.metadata.sql;

import java.util.Map;
import java.util.HashMap;
import org.apache.sis.util.iso.Types;


/**
 * Utility methods for handling the inheritance between tables.
 * This features is partially supported in PostgreSQL database.
 *
 * <p>This class is a work around for databases that support table inheritances,
 * but not yet index inheritance. For example in PostgreSQL 9.5.13, we can not yet declare
 * a foreigner key to the super table and find the entries in inherited tables that way.</p>
 *
 * <p>An alternative to current workaround would be to repeat a search in all child tables.
 * We could use {@link java.sql.DatabaseMetaData#getSuperTables(String, String, String)} for
 * getting the list of child tables.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class TableHierarchy {
    /**
     * Delimiter characters for the table name in identifier. Table names are prefixed to identifiers only if
     * the type represented by the table is a subtype. For example since {@code Organisation} is a subtype of
     * {@code Party}, identifiers for organizations need to be prefixed by {@code {Organisation}} in order to
     * allow {@code MetadataSource} to know in which table to search for such party.
     *
     * @see MetadataWriter#isReservedChar(int)
     */
    static final char TYPE_OPEN = '{', TYPE_CLOSE = '}';

    /**
     * Abbreviations for commonly-used tables. We use those abbreviations because table names like
     * {@code "VectorSpatialRepresentation"} consume a lot of space, which leave few spaces left
     * for actual identifiers when we want to limit the length to a relatively small value.
     */
    private static final Map<String,String> ABBREVIATIONS = new HashMap<>(25);

    /**
     * The reverse of {@link #ABBREVIATIONS}.
     */
    private static final Map<String,String> TABLES = new HashMap<>(25);
    static {
        add("Individual",                  "ind");
        add("Organisation",                "org");
        add("ResponsibleParty",            "rp");
        add("VectorSpatialRepresentation", "vec");
        add("GridSpatialRepresentation",   "grd");
        add("Georectified",                "rtf");
        add("Georeferenceable",            "cbl");
        add("DataIdentification",          "data");
        add("ServiceIdentification",       "srv");
        add("FeatureCatalogueDescription", "cat");
        add("CoverageDescription",         "cov");
        add("ImageDescription",            "img");
        add("SampleDimension",             "sd");
        add("Band",                        "band");
        add("LegalConstraints",            "legal");
        add("SecurityConstraints",         "secu");
        add("GeographicBoundingBox",       "bbox");
        add("BoundingPolygon",             "poly");
        add("GeographicDescription",       "gdsc");
        add("SpatialTemporalExtent",       "ste");
        add("GCPCollection",               "gcp");
        // TODO: missing quality package.
    }

    /**
     * Adds an abbreviation. For class initialization only.
     */
    private static void add(final String table, final String abbreviation) {
        ABBREVIATIONS.put(table, abbreviation);
        TABLES.put(abbreviation, table);
    }

    /**
     * Do not allow instantiation of this class.
     */
    private TableHierarchy() {
    }

    /**
     * Encode table name in the given identifier.
     */
    static String encode(String table, final String identifier) {
        table = ABBREVIATIONS.getOrDefault(table, table);
        return TYPE_OPEN + table + TYPE_CLOSE + identifier;
    }

    /**
     * If the given identifier specifies a subtype of the given type, then returns that subtype.
     * For example if the given type is {@code Party.class} and the given identifier is
     * {@code "{Organisation}EPSG"}, then this method returns {@code Organisation.class}.
     * Otherwise this method returns {@code type} unchanged.
     *
     * @param  type        base metadata type.
     * @param  identifier  primary key in the database.
     * @return actual type of the metadata object.
     */
    static Class<?> subType(Class<?> type, final String identifier) {
        if (identifier.charAt(0) == TYPE_OPEN) {
            final int i = identifier.indexOf(TYPE_CLOSE);
            if (i >= 0) {
                String table = identifier.substring(1, i);
                table = TABLES.getOrDefault(table, table);
                final Class<?> subType = Types.forStandardName(table);
                if (subType != null && type.isAssignableFrom(subType)) {
                    type = subType;
                }
            }
        }
        return type;
    }
}
