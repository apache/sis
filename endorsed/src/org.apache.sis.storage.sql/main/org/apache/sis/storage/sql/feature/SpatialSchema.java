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
package org.apache.sis.storage.sql.feature;

import java.util.Map;


/**
 * Information about table names and column names used for the spatial schema.
 * There is many standards, with nearly identical content but different names.
 *
 * <ul>
 *   <li>Geopackage</li>
 *   <li>ISO-13249 SQL/MM</li>
 *   <li>ISO 19125 / OGC Simple feature access part 2</li>
 * </ul>
 *
 * The presence of tables for each standard will be tested in enumeration order.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum SpatialSchema {
    /**
     * Table and column names as specified by GeoPackage. This is the same thing as {@link #SQL_MM}
     * except for table names, for the case (Geopackage uses lower case) and for the addition of a
     * {@code geometry_type_name} column.
     */
    GEOPACKAGE(
            "GeoPackage",                           // Human-readable name of this spaial schema.
            "gpkg_spatial_ref_sys",                 // Table for Spatial Reference System definitions.
            "srs_name",                             // Column for CRS names.
            "srs_id",                               // Column for CRS identifiers.
            "organization",                         // Column for CRS authority names.
            "organization_coordsys_id",             // Column for CRS authority codes.
            Map.of(CRSEncoding.WKT1, "definition",  // Columns for CRS definitions in WKT format.
                   CRSEncoding.WKT2, "definition_12_063"),
            "description",                          // Column for the CRS description.
            "gpkg_geometry_columns",                // Table enumerating the geometry columns.
            "table_catalog",                        // Column where the catalog of each geometry column is stored.
            "table_schema",                         // Column where the schema of each geometry column is stored.
            "table_name",                           // Column where the table of each geometry column is stored.
            "column_name",                          // Column where the column of each geometry column is stored.
            "geometry_type_name",                   // Column where the type of each geometry column is stored.
            GeometryTypeEncoding.TEXTUAL),          // How geometry types are encoded in the above-cited type column.

    /**
     * Table and column names as specified by ISO-13249 SQL/MM. This is similar to {@link #SIMPLE_FEATURE}
     * with different names and no {@code GEOMETRY_TYPE} column. The table definition for CRS is:
     *
     * {@snippet lang="sql" :
     * CREATE TABLE ST_SPATIAL_REFERENCE_SYSTEMS(
     *   SRS_NAME CHARACTER VARYING(ST_MaxSRSNameLength) NOT NULL,
     *   SRS_ID INTEGER NOT NULL,
     *   ORGANIZATION CHARACTER VARYING(ST_MaxOrganizationNameLength),
     *   ORGANIZATION_COORDSYS_ID INTEGER,
     *   DEFINITION CHARACTER VARYING(ST_MaxSRSDefinitionLength) NOT NULL,
     *   DESCRIPTION CHARACTER VARYING(ST_MaxDescriptionLength))
     * }
     *
     * In Geopackage, this table is named {@code "gpkg_spatial_ref_sys"} but otherwise has identical content
     * except for the case (Geopackage uses lower case).
     */
    SQL_MM("ISO-13249 SQL/MM",                      // Human-readable name of this spaial schema.
           "ST_SPATIAL_REFERENCE_SYSTEMS",          // Table for Spatial Reference System definitions.
           "SRS_NAME",                              // Column for CRS names.
           "SRS_ID",                                // Column for CRS identifiers.
           "ORGANIZATION",                          // Column for CRS authority names.
           "ORGANIZATION_COORDSYS_ID",              // Column for CRS authority codes.
           Map.of(CRSEncoding.WKT1, "DEFINITION"),  // Columns for CRS definitions in WKT format.
           "DESCRIPTION",                           // Column for the CRS description.
           "ST_GEOMETRY_COLUMNS",                   // Table enumerating the geometry columns.
           "TABLE_CATALOG",                         // Column where the catalog of each geometry column is stored.
           "TABLE_SCHEMA",                          // Column where the schema of each geometry column is stored.
           "TABLE_NAME",                            // Column where the table of each geometry column is stored.
           "COLUMN_NAME",                           // Column where the column of each geometry column is stored.
           null,                                    // Column where the type of each geometry column is stored, or null if none.
           null),                                   // How geometry types are encoded in the above-cited type column.

    /**
     * Table and column names as specified by ISO 19125 / OGC Simple feature access part 2.
     * Note that the standard specifies table names in upper-case letters, which is also the default case
     * specified by the SQL standard. However, some databases use lower cases instead. This table name can
     * be used unquoted for letting the database engine converts the case. The table definition for CRS is:
     *
     * {@snippet lang="sql" :
     * CREATE TABLE SPATIAL_REF_SYS (
     *   SRID INTEGER NOT NULL PRIMARY KEY,
     *   AUTH_NAME CHARACTER VARYING,
     *   AUTH_SRID INTEGER,
     *   SRTEXT CHARACTER VARYING(2048))
     * }
     *
     * <h4>PostGIS special case</h4>
     * PostGIS uses these table and column names (in lower cases), except the {@code GEOMETRY_TYPE} column
     * which is named only {@code TYPE} in PostGIS. There is no enumeration value for PostGIS special case.
     * Instead, it is handled by {@code InfoStatements.completeIntrospection(…)} method overriding.
     */
    SIMPLE_FEATURE(
            "ISO 19125 / OGC Simple feature",       // Human-readable name of this spaial schema.
            "SPATIAL_REF_SYS",                      // Table for Spatial Reference System definitions.
            null,                                   // Column for CRS names, or `null` if none.
            "SRID",                                 // Column for CRS identifiers.
            "AUTH_NAME",                            // Column for CRS authority names.
            "AUTH_SRID",                            // Column for CRS authority codes.
            Map.of(CRSEncoding.WKT1, "SRTEXT"),     // Columns for CRS definitions in WKT format.
            null,                                   // Column for the CRS description, or `null` if none.
            "GEOMETRY_COLUMNS",                     // Table enumerating the geometry columns.
            "F_TABLE_CATALOG",                      // Column where the catalog of each geometry column is stored.
            "F_TABLE_SCHEMA",                       // Column where the schema of each geometry column is stored.
            "F_TABLE_NAME",                         // Column where the table of each geometry column is stored.
            "F_GEOMETRY_COLUMN",                    // Column where the column of each geometry column is stored.
            "GEOMETRY_TYPE",                        // Column where the type of each geometry column is stored.
            GeometryTypeEncoding.NUMERIC);          // How geometry types are encoded in the above-cited type column.

    /**
     * The name of this spatial schema.
     */
    final String name;

    /**
     * Name of the table for Spatial Reference System definitions.
     * Example: {@code "SPATIAL_REF_SYS"}, {@code "ST_SPATIAL_REFERENCE_SYSTEMS"}.
     */
    final String crsTable;

    /**
     * Name of the column for CRS name, or {@code null} if none.
     * Example: {@code "SRS_NAME"}.
     */
    final String crsNameColumn;

    /**
     * Name of the column for CRS identifiers.
     * Example: {@code "SRID"}, {@code "SRS_ID"}.
     * Also used in the geometry columns table.
     */
    final String crsIdentifierColumn;

    /**
     * Name of the column for CRS authority names.
     * Example: {@code "AUTH_NAME"}, {@code "ORGANIZATION"}.
     */
    final String crsAuthorityNameColumn;

    /**
     * Name of the column for CRS authority codes.
     * Example: {@code "AUTH_SRID"}, {@code "ORGANIZATION_COORDSYS_ID"}.
     */
    final String crsAuthorityCodeColumn;

    /**
     * Name of the column for CRS definitions in Well-Known Text (<abbr>WKT</abbr>) format.
     * Example: {@code "SRTEXT"}, {@code "DEFINITION"}. Entries are in no particular order.
     * The priority order is not defined by this map, but by the {@link CRSEncoding} enumeration.
     */
    final Map<CRSEncoding, String> crsDefinitionColumn;

    /**
     * Name of the column for the CRS description, or {@code null} if none.
     */
    final String crsDescriptionColumn;

    /**
     * Name of the table enumerating the geometry columns.
     */
    final String geometryColumns;

    /**
     * Name of the column where the catalog of each geometry column is stored.
     * Example: {@code "F_TABLE_CATALOG"}, {@code "TABLE_CATALOG"}.
     */
    final String geomCatalogColumn;

    /**
     * Name of the column where the schema of each geometry column is stored.
     * Example: {@code "F_TABLE_SCHEMA"}, {@code "TABLE_SCHEMA"}.
     */
    final String geomSchemaColumn;

    /**
     * Name of the column where the table of each geometry column is stored.
     * Example: {@code "F_TABLE_NAME"}, {@code "TABLE_NAME"}.
     */
    final String geomTableColumn;

    /**
     * Name of the column where the column of each geometry column is stored.
     * Example: {@code "F_GEOMETRY_COLUMN"}, {@code "COLUMN_NAME"}.
     */
    final String geomColNameColumn;

    /**
     * Name of the column where the type of each geometry column is stored, or {@code null} if none.
     * Example: {@code "GEOMETRY_TYPE"}.
     */
    final String geomTypeColumn;

    /**
     * Specifies how geometry types are encoded in the {@link #geomTypeColumn}.
     */
    final GeometryTypeEncoding typeEncoding;

    /**
     * Creates a new enumeration value.
     *
     * @param name                    name of this spatial schema.
     * @param crsTable                name of the table for Spatial Reference System definitions.
     * @param crsNameColumn           name of the column for CRS names, or {@code null} if none.
     * @param crsIdentifierColumn     name of the column for CRS identifiers.
     * @param crsAuthorityNameColumn  name of the column for CRS authority names.
     * @param crsAuthorityCodeColumn  name of the column for CRS authority codes.
     * @param crsDefinitionColumn     name of the columns for CRS definitions in <abbr>WKT</abbr> format.
     * @param crsDescriptionColumn    name of the column for the CRS description, or {@code null} if none.
     * @param geometryColumns         name of the table enumerating the geometry columns.
     * @param geomCatalogColumn       name of the column where the catalog of each geometry column is stored.
     * @param geomSchemaColumn        name of the column where the schema of each geometry column is stored.
     * @param geomTableColumn         name of the column where the table of each geometry column is stored.
     * @param geomColNameColumn       name of the column where the column of each geometry column is stored.
     * @param geomTypeColumn          name of the column where the type of each geometry column is stored, or null if none.
     * @param typeEncoding            how geometry types are encoded in the {@link #geomTypeColumn}.
     */
    private SpatialSchema(String name, String crsTable, String crsNameColumn, String crsIdentifierColumn,
                          String crsAuthorityNameColumn, String crsAuthorityCodeColumn,
                          Map<CRSEncoding,String> crsDefinitionColumn, String crsDescriptionColumn,
                          String geometryColumns, String geomCatalogColumn, String geomSchemaColumn,
                          String geomTableColumn, String geomColNameColumn, String geomTypeColumn,
                          GeometryTypeEncoding typeEncoding)
    {
        this.name                   = name;
        this.crsTable               = crsTable;
        this.crsNameColumn          = crsNameColumn;
        this.crsIdentifierColumn    = crsIdentifierColumn;
        this.crsAuthorityNameColumn = crsAuthorityNameColumn;
        this.crsAuthorityCodeColumn = crsAuthorityCodeColumn;
        this.crsDefinitionColumn    = crsDefinitionColumn;
        this.crsDescriptionColumn   = crsDescriptionColumn;
        this.geometryColumns        = geometryColumns;
        this.geomCatalogColumn      = geomCatalogColumn;
        this.geomSchemaColumn       = geomSchemaColumn;
        this.geomTableColumn        = geomTableColumn;
        this.geomColNameColumn      = geomColNameColumn;
        this.geomTypeColumn         = geomTypeColumn;
        this.typeEncoding           = typeEncoding;
    }
}
