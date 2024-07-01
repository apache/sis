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
package org.apache.sis.storage.geopackage.privy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Geopackage database record types.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Record {

    /**
     * CREATE TABLE IF NOT EXISTS gpkg_contents (
     *   table_name TEXT NOT NULL PRIMARY KEY,
     *   data_type TEXT NOT NULL,
     *   identifier TEXT UNIQUE,
     *   description TEXT DEFAULT '',
     *   last_change DATETIME NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
     *   min_x DOUBLE,
     *   min_y DOUBLE,
     *   max_x DOUBLE,
     *   max_y DOUBLE,
     *   srs_id INTEGER,
     *   CONSTRAINT fk_gc_r_srs_id FOREIGN KEY (srs_id) REFERENCES gpkg_spatial_ref_sys(srs_id)
     * );
     */
    public static final class Content {

        public String tableName;
        public String dataType;
        public String identifier;
        public String description;
        public Calendar lastChange;
        public Double minX;
        public Double minY;
        public Double maxX;
        public Double maxY;
        public Integer srsId;

        public Content() {
        }

        /**
         * Create a minimal content description.
         * Bounding box and SRS will be null, date set to now and an empty description.
         *
         * @param name used as table name and identifier
         * @param dataType
         */
        public Content(String name, String dataType) {
            this.tableName = name;
            this.dataType = dataType;
            this.identifier = name;
            this.description = "";
            this.lastChange = Calendar.getInstance();
            this.minX = null;
            this.minY = null;
            this.maxX = null;
            this.maxY = null;
            this.srsId = null;
        }

        public void read(ResultSet rs) throws SQLException {
            tableName = rs.getString("table_name");
            dataType = rs.getString("data_type");
            identifier = rs.getString("identifier");
            description = rs.getString("description");
            String lastChange = rs.getString("last_change");

            if (lastChange != null) {
                final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                format.setTimeZone(TimeZone.getTimeZone("GMT"));

                //set time to now
                this.lastChange = Calendar.getInstance();
                if (!lastChange.isEmpty()) {
                    try {
                        this.lastChange.setTime(format.parse(lastChange));
                    } catch (ParseException ex) {
                        throw new SQLException(ex.getMessage(), ex);
                    }
                }
            } else {
                throw new SQLException("last change date should not be null");
            }

            minX = rs.getDouble("min_x"); if (rs.wasNull()) minX = null;
            minY = rs.getDouble("min_y"); if (rs.wasNull()) minY = null;
            maxX = rs.getDouble("max_x"); if (rs.wasNull()) maxX = null;
            maxY = rs.getDouble("max_y"); if (rs.wasNull()) maxY = null;
            srsId = rs.getInt("srs_id"); if (rs.wasNull()) srsId = null;
        }

        public void create(Connection cnx) throws SQLException {

            String lastChange = "";
            if (this.lastChange != null) {
                final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                format.setTimeZone(TimeZone.getTimeZone("GMT"));
                lastChange = format.format(this.lastChange.getTime());
            }

            try (PreparedStatement stmt = Query.CONTENTS_CREATE.createPreparedStatement(cnx,
                    tableName,
                    dataType,
                    identifier,
                    description,
                    lastChange,
                    minX,
                    minY,
                    maxX,
                    maxY,
                    srsId)) {
                stmt.executeUpdate();
            }
        }

        public void update(Connection cnx) throws SQLException {
            try (PreparedStatement stmt = Query.CONTENTS_UPDATE.createPreparedStatement(cnx,
                    tableName,
                    dataType,
                    identifier,
                    description,
                    lastChange,
                    minX,
                    minY,
                    maxX,
                    maxY,
                    srsId,
                    tableName
                    )) {
                stmt.executeUpdate();
            }
        }
    }

    /**
     * CREATE TABLE IF NOT EXISTS gpkg_tile_matrix_set (
     *   table_name TEXT NOT NULL PRIMARY KEY,
     *   srs_id INTEGER NOT NULL,
     *   min_x DOUBLE NOT NULL,
     *   min_y DOUBLE NOT NULL,
     *   max_x DOUBLE NOT NULL,
     *   max_y DOUBLE NOT NULL,
     *   CONSTRAINT fk_gtms_table_name FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name),
     *   CONSTRAINT fk_gtms_srs FOREIGN KEY (srs_id) REFERENCES gpkg_spatial_ref_sys (srs_id)
     * );
     */
    public static final class TileMatrixSet {
        public String tableName;
        public int srsId;
        public double minX;
        public double minY;
        public double maxX;
        public double maxY;

        public void read(ResultSet rs) throws SQLException {
            tableName   = rs.getString("table_name");
            srsId       = rs.getInt("srs_id");
            minX        = rs.getDouble("min_x");
            minY        = rs.getDouble("min_y");
            maxX        = rs.getDouble("max_x");
            maxY        = rs.getDouble("max_y");
        }

        public void create(Connection cnx) throws SQLException {
            try (PreparedStatement stmt = Query.TILE_MATRIX_SET_CREATE.createPreparedStatement(
                    cnx,
                    tableName,
                    srsId,
                    minX,
                    minY,
                    maxX,
                    maxY)) {
                stmt.executeUpdate();
            }
        }
    }

    /**
     * CREATE TABLE IF NOT EXISTS gpkg_tile_matrix (
     *   table_name TEXT NOT NULL,
     *   zoom_level INTEGER NOT NULL,
     *   matrix_width INTEGER NOT NULL,
     *   matrix_height INTEGER NOT NULL,
     *   tile_width INTEGER NOT NULL,
     *   tile_height INTEGER NOT NULL,
     *   pixel_x_size DOUBLE NOT NULL,
     *   pixel_y_size DOUBLE NOT NULL,
     *   CONSTRAINT pk_ttm PRIMARY KEY (table_name, zoom_level),
     *   CONSTRAINT fk_tmm_table_name FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name)
     * );
     */
    public static final class TileMatrix {
        public String tableName;
        public int zoomLevel;
        public int matrixWidth;
        public int matrixHeight;
        public int tileWidth;
        public int tileHeight;
        public double pixelXSize;
        public double pixelYSize;

        public void read(ResultSet rs) throws SQLException {
            tableName    = rs.getString("table_name");
            zoomLevel    = rs.getInt("zoom_level");
            matrixWidth  = rs.getInt("matrix_width");
            matrixHeight = rs.getInt("matrix_height");
            tileWidth    = rs.getInt("tile_width");
            tileHeight   = rs.getInt("tile_height");
            pixelXSize   = rs.getDouble("pixel_x_size");
            pixelYSize   = rs.getDouble("pixel_y_size");
        }

        public void create(Connection cnx) throws SQLException {
            try (PreparedStatement stmt = Query.TILE_MATRIX_CREATE.createPreparedStatement(
                    cnx,
                    tableName,
                    zoomLevel,
                    matrixWidth,
                    matrixHeight,
                    tileWidth,
                    tileHeight,
                    pixelXSize,
                    pixelYSize)) {
                stmt.executeUpdate();
            }
        }
    }

    /**
     * CREATE TABLE sample_tile_pyramid (
     *   id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
     *   zoom_level INTEGER NOT NULL,
     *   tile_column INTEGER NOT NULL,
     *   tile_row INTEGER NOT NULL,
     *   tile_data BLOB NOT NULL,
     *   UNIQUE (zoom_level, tile_column, tile_row)
     * )
     */
    public static final class Tile {
        public int id;
        public int zoomLevel;
        public int tileColumn;
        public int tileRow;
        public byte[] tileData;

        public void read(ResultSet rs) throws SQLException {
            id          = rs.getInt("id");
            zoomLevel   = rs.getInt("zoom_level");
            tileColumn  = rs.getInt("tile_column");
            tileRow     = rs.getInt("tile_row");
            tileData    = rs.getBytes("tile_data");
        }

        public void create(Connection cnx) throws SQLException {
            try (PreparedStatement stmt = Query.TILE_CREATE.createPreparedStatement(
                    cnx,
                    zoomLevel,
                    tileColumn,
                    tileRow,
                    tileData)) {
                stmt.executeUpdate();
            }
        }
    }

    /**
     * CREATE TABLE IF NOT EXISTS gpkg_geometry_columns (
     *   table_name TEXT NOT NULL,
     *   column_name TEXT NOT NULL,
     *   geometry_type_name TEXT NOT NULL,
     *   srs_id INTEGER NOT NULL,
     *   z TINYINT NOT NULL,
     *   m TINYINT NOT NULL,
     *   CONSTRAINT pk_geom_cols PRIMARY KEY (table_name, column_name),
     *   CONSTRAINT uk_gc_table_name UNIQUE (table_name),
     *   CONSTRAINT fk_gc_tn FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name),
     *   CONSTRAINT fk_gc_srs FOREIGN KEY (srs_id) REFERENCES gpkg_spatial_ref_sys (srs_id)
     * );
     */
    public static final class GeometryColumn {
        public String tableName;
        public String columnName;
        public String geometryType;
        public int srsId;
        public int z;
        public int m;

        public void read(ResultSet rs) throws SQLException {
            tableName       = rs.getString("table_name");
            columnName      = rs.getString("column_name");
            geometryType    = rs.getString("geometry_type_name");
            srsId           = rs.getInt("srs_id");
            z               = rs.getInt("z");
            m               = rs.getInt("m");
        }

        public void write(Connection cnx) throws SQLException {
            try (PreparedStatement stmt = Query.GEOMETRY_COLUMN_CREATE.createPreparedStatement(
                    cnx,
                    tableName,
                    columnName,
                    geometryType,
                    srsId,
                    z,
                    m)) {
                stmt.executeUpdate();
            }
        }
    }

    /**
     * CREATE TABLE IF NOT EXISTS gpkg_spatial_ref_sys (
     *   srs_name TEXT NOT NULL,
     *   srs_id INTEGER NOT NULL PRIMARY KEY,
     *   organization TEXT NOT NULL,
     *   organization_coordsys_id INTEGER NOT NULL,
     *   definition  TEXT NOT NULL,
     *   description TEXT
     *   definition_12_063 TEXT (EXTENSION)
     * );
     */
    public static final class SpatialRefSys {
        public String srsName;
        public int srsId;
        public String organization;
        public int organizationCoordsysId;
        public String definition;
        public String description;
        public String definition_12_063;

        public void read(ResultSet rs) throws SQLException {
            srsName                 = rs.getString("srs_name");
            srsId                   = rs.getInt("srs_id");
            organization            = rs.getString("organization");
            organizationCoordsysId  = rs.getInt("organization_coordsys_id");
            definition              = rs.getString("definition");
            description             = rs.getString("description");
            try {
                definition_12_063       = rs.getString("definition_12_063");
            } catch (SQLException ex) {
                //this field may not exist
            }
        }

        public void create(Connection cnx) throws SQLException {
            try (PreparedStatement stmt = Query.SPATIAL_REF_CREATE.createPreparedStatement(
                    cnx,
                    srsName,
                    srsId,
                    organization,
                    organizationCoordsysId,
                    definition)) {
                stmt.executeUpdate();
            }
        }

        public void createExt(Connection cnx) throws SQLException {
            try (PreparedStatement stmt = Query.SPATIAL_REF_CREATE.createPreparedStatement(
                    cnx,
                    srsName,
                    srsId,
                    organization,
                    organizationCoordsysId,
                    definition,
                    definition_12_063)) {
                stmt.executeUpdate();
            }
        }
    }

    /**
     * CREATE TABLE IF NOT EXISTS gpkg_extensions (
     *   table_name TEXT,
     *   column_name TEXT,
     *   extension_name TEXT NOT NULL,
     *   definition TEXT NOT NULL,
     *   scope TEXT NOT NULL,
     *   CONSTRAINT ge_tce UNIQUE (table_name, column_name, extension_name)
     * );
     */
    public static final class Extension {
        public String tableName;
        public String columnName;
        public String extensionName;
        public String definition;
        public String scope;

        public void read(ResultSet rs) throws SQLException {
            tableName     = rs.getString("table_name");
            columnName    = rs.getString("column_name");
            extensionName = rs.getString("extension_name");
            definition    = rs.getString("definition");
            scope         = rs.getString("scope");
        }

        public void create(Connection cnx) throws SQLException {
            try (PreparedStatement stmt = Query.EXTENSION_CREATE.createPreparedStatement(
                    cnx,
                    tableName,
                    columnName,
                    extensionName,
                    definition,
                    scope)) {
                stmt.executeUpdate();
            }
        }
    }

}
