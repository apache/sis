-- Licensed under the Apache License, Version 2.0 (the "License").
-- https://www.ogc.org/about-ogc/policies/software-licenses/
--
-- Core schema of OGC Geopackage, mandatory in every Geopackage files.
-- The original schema is published by OGC in the Geopackage specification.
-- The schema below contains the following modifications, applied by Apache SIS:
--
--   * Formatting and comments.
--   * Integration of WKT-2 extension.
--   * Missing NOT NULL constraint added on a primary key.
--   * TODO: can we replace strftime by (datetime('now'))?
--

--
-- C.1. Spatial Reference Systems + WKT-2 extension of geopackage
--
CREATE TABLE gpkg_spatial_ref_sys (
  srs_name                 TEXT    NOT NULL,
  srs_id                   INTEGER NOT NULL PRIMARY KEY,
  organization             TEXT    NOT NULL,
  organization_coordsys_id INTEGER NOT NULL,
  definition               TEXT    NOT NULL,
  description              TEXT,
  definition_12_063        TEXT    NOT NULL
);

--
-- C.2. contents
--
CREATE TABLE gpkg_contents (
  table_name  TEXT     NOT NULL PRIMARY KEY,
  data_type   TEXT     NOT NULL,
  identifier  TEXT     UNIQUE,
  description TEXT     DEFAULT '',
  last_change DATETIME NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
  min_x       DOUBLE,
  min_y       DOUBLE,
  max_x       DOUBLE,
  max_y       DOUBLE,
  srs_id      INTEGER,
  CONSTRAINT fk_gc_r_srs_id FOREIGN KEY (srs_id) REFERENCES gpkg_spatial_ref_sys(srs_id)
);

--
-- C.3. Geometry columns
--
CREATE TABLE gpkg_geometry_columns (
  table_name         TEXT    NOT NULL,
  column_name        TEXT    NOT NULL,
  geometry_type_name TEXT    NOT NULL,
  srs_id             INTEGER NOT NULL,
  z                  TINYINT NOT NULL,
  m                  TINYINT NOT NULL,
  CONSTRAINT pk_geom_cols     PRIMARY KEY (table_name, column_name),
  CONSTRAINT uk_gc_table_name UNIQUE      (table_name),
  CONSTRAINT fk_gc_tn         FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name),
  CONSTRAINT fk_gc_srs        FOREIGN KEY (srs_id)     REFERENCES gpkg_spatial_ref_sys(srs_id)
);

--
-- C.5. Tile matrix set
-- TODO: Not yet supported by SIS.
-- TODO: This table is required only if the file contains a pyramid. Should be created only when first needed.
--
CREATE TABLE gpkg_tile_matrix_set (
  table_name TEXT    NOT NULL PRIMARY KEY,
  srs_id     INTEGER NOT NULL,
  min_x      DOUBLE  NOT NULL,
  min_y      DOUBLE  NOT NULL,
  max_x      DOUBLE  NOT NULL,
  max_y      DOUBLE  NOT NULL,
  CONSTRAINT fk_gtms_table_name FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name),
  CONSTRAINT fk_gtms_srs        FOREIGN KEY (srs_id)     REFERENCES gpkg_spatial_ref_sys(srs_id)
);

--
-- C.6. Tile matrix
-- TODO: Not yet supported by SIS.
-- TODO: This table is required only if the file contains a pyramid. Should be created only when first needed.
-- TODO: If zoom interval is not a factor of 2, we must add a "gpkg_zoom_other" row in "gpkg_extensions".
--       See §F.6. Zoom Other Intervals.
--
CREATE TABLE gpkg_tile_matrix (
  table_name    TEXT    NOT NULL,
  zoom_level    INTEGER NOT NULL,
  matrix_width  INTEGER NOT NULL,
  matrix_height INTEGER NOT NULL,
  tile_width    INTEGER NOT NULL,
  tile_height   INTEGER NOT NULL,
  pixel_x_size  DOUBLE  NOT NULL,
  pixel_y_size  DOUBLE  NOT NULL,
  CONSTRAINT pk_ttm            PRIMARY KEY (table_name, zoom_level),
  CONSTRAINT fk_tmm_table_name FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name)
);

--
-- C.8. Extensions
-- TODO: Not yet used by SIS.
-- TODO: This table is optional. Should be created only when first needed.
--
CREATE TABLE gpkg_extensions (
  table_name     TEXT,
  column_name    TEXT,
  extension_name TEXT NOT NULL,
  definition     TEXT NOT NULL,
  scope          TEXT NOT NULL,
  CONSTRAINT ge_tce UNIQUE (table_name, column_name, extension_name)
);

-- Extension applied by default in Apache SIS.
INSERT INTO gpkg_extensions (table_name, column_name, extension_name, definition, scope) VALUES
('gpkg_spatial_ref_sys', 'definition_12_063', 'gpkg_crs_wkt', 'http://www.geopackage.org/spec/#extension_crs_wkt', 'read-write');

--
-- Predefined Coordinate Reference Systems.
-- Mandatory according Geopackage specification.
--
INSERT INTO gpkg_spatial_ref_sys(srs_name, srs_id, organization, organization_coordsys_id, definition, definition_12_063, description) VALUES
('Undefined cartesian SRS', -1, 'NONE', -1, 'undefined', 'undefined', 'Undefined Cartesian coordinate reference system.'),
('Undefined geographic SRS', 0, 'NONE',  0, 'undefined', 'undefined', 'Undefined geographic coordinate reference system.'),
('WGS 84 geodetic', 4326, 'EPSG', 4326,
 'GEOGCS["WGS 84", DATUM["World Geodetic System 1984", SPHEROID["WGS 84", 6378137, 298.257223563], AUTHORITY["EPSG","6326"]], PRIMEM["Greenwich",0], UNIT["degree", 0.017453292519943295]]',
 'GeodeticCRS["WGS 84", Datum["World Geodetic System 1984", Ellipsoid["WGS 84", 6378137, 298.257223563], Id["EPSG",6326]], CS[ellipsoidal,2], Axis["Longitude (L)", east], Axis["Latitude (B)", north], Unit["degree", 0.017453292519943295]]',
 'Longitude/latitude coordinates in decimal degrees on the WGS 84 datum ensemble.');

-- TODO: missing triggers (annex D)
