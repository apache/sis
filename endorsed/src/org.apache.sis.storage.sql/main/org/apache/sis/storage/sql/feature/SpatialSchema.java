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
     * Table and column names as specified by Geopackage. This is the same thing as {@link #SQL_MM}
     * except for table names, for the case (Geopackage uses lower case) and for the addition of a
     * {@code geometry_type_name} column.
     */
    GEOPACKAGE("gpkg_spatial_ref_sys", "srs_id", "organization", "organization_coordsys_id",
               Map.of(CRSEncoding.WKT1, "definition",
                      CRSEncoding.WKT2, "definition_12_063"),

               "gpkg_geometry_columns", "table_catalog", "table_schema", "table_name",
               "column_name", "geometry_type_name", GeometryTypeEncoding.TEXTUAL),

    /**
     * Table and column names as specified by ISO-13249 SQL/MM. This is the same thing as {@link #SIMPLE_FEATURE}
     * with only different names. The table definition for CRS is:
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
    SQL_MM("ST_SPATIAL_REFERENCE_SYSTEMS", "SRS_ID", "ORGANIZATION", "ORGANIZATION_COORDSYS_ID",
           Map.of(CRSEncoding.WKT1, "DEFINITION"),
           "ST_GEOMETRY_COLUMNS", "TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "COLUMN_NAME", null, null),

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
     */
    SIMPLE_FEATURE("SPATIAL_REF_SYS", "SRID", "AUTH_NAME", "AUTH_SRID", Map.of(CRSEncoding.WKT1, "SRTEXT"),
                   "GEOMETRY_COLUMNS", "F_TABLE_CATALOG", "F_TABLE_SCHEMA", "F_TABLE_NAME", "F_GEOMETRY_COLUMN",
                   "GEOMETRY_TYPE", GeometryTypeEncoding.NUMERIC);

    /**
     * Name of the table for Spatial Reference System definitions.
     * Example: {@code "SPATIAL_REF_SYS"}, {@code "ST_SPATIAL_REFERENCE_SYSTEMS"}.
     */
    final String crsTable;

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
     * Example: {@code "SRTEXT"}, {@code "DEFINITION"}.
     * Entries are in no particular order.
     */
    final Map<CRSEncoding, String> crsDefinitionColumn;

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
     * @param crsTable                name of the table for Spatial Reference System definitions.
     * @param crsIdentifierColumn     name of the column for CRS identifiers.
     * @param crsAuthorityNameColumn  name of the column for CRS authority names.
     * @param crsAuthorityCodeColumn  name of the column for CRS authority codes.
     * @param crsDefinitionColumn     name of the column for CRS definitions in <abbr>WKT</abbr> format.
     * @param geometryColumns         name of the table enumerating the geometry columns.
     * @param geomCatalogColumn       name of the column where the catalog of each geometry column is stored.
     * @param geomSchemaColumn        name of the column where the schema of each geometry column is stored.
     * @param geomTableColumn         name of the column where the table of each geometry column is stored.
     * @param geomColNameColumn       name of the column where the column of each geometry column is stored.
     * @param geomTypeColumn          name of the column where the type of each geometry column is stored, or null if none.
     * @param typeEncoding            how geometry types are encoded in the {@link #geomTypeColumn}.
     */
    private SpatialSchema(String crsTable, String crsIdentifierColumn, String crsAuthorityNameColumn,
                          String crsAuthorityCodeColumn, Map<CRSEncoding,String> crsDefinitionColumn,
                          String geometryColumns, String geomCatalogColumn, String geomSchemaColumn,
                          String geomTableColumn, String geomColNameColumn, String geomTypeColumn,
                          GeometryTypeEncoding typeEncoding)
    {
        this.crsTable               = crsTable;
        this.crsIdentifierColumn    = crsIdentifierColumn;
        this.crsAuthorityNameColumn = crsAuthorityNameColumn;
        this.crsAuthorityCodeColumn = crsAuthorityCodeColumn;
        this.crsDefinitionColumn    = crsDefinitionColumn;
        this.geometryColumns        = geometryColumns;
        this.geomCatalogColumn      = geomCatalogColumn;
        this.geomSchemaColumn       = geomSchemaColumn;
        this.geomTableColumn        = geomTableColumn;
        this.geomColNameColumn      = geomColNameColumn;
        this.geomTypeColumn         = geomTypeColumn;
        this.typeEncoding           = typeEncoding;
    }
}
