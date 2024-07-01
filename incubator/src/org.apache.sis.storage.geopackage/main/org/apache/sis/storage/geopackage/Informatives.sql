


-- SQL/MM View of gpkg_spatial_ref_sys Definition SQL (Informative)

CREATE VIEW st_spatial_ref_sys AS
   SELECT
     srs_name,
     srs_id,
     organization,
     organization_coordsys_id,
     definition,
     description
   FROM gpkg_spatial_ref_sys;


-- SF/SQL View of gpkg_spatial_ref_sys Definition SQL (Informative)

 CREATE VIEW spatial_ref_sys AS
   SELECT
     srs_id AS srid,
     organization AS auth_name,
     organization_coordsys_id AS auth_srid,
     definition AS srtext
   FROM gpkg_spatial_ref_sys;



-- SQL/MM View of gpkg_geometry_columns Definition SQL (Informative)

 CREATE VIEW st_geometry_columns AS
   SELECT
     table_name,
     column_name,
     "ST_" || geometry_type_name,
     g.srs_id,
     srs_name
   FROM gpkg_geometry_columns as g JOIN gpkg_spatial_ref_sys AS s
   WHERE g.srs_id = s.srs_id;


-- SF/SQL VIEW of gpkg_geometry_columns Definition SQL (Informative)

 CREATE VIEW geometry_columns AS
   SELECT
     table_name AS f_table_name,
     column_name AS f_geometry_column,
     code4name (geometry_type_name) AS geometry_type,
     2 + (CASE z WHEN 1 THEN 1 WHEN 2 THEN 1 ELSE 0 END) + (CASE m WHEN 1 THEN 1 WHEN 2 THEN 1 ELSE 0 END) AS coord_dimension,
     srs_id AS srid
   FROM gpkg_geometry_columns;


-- C.4. sample_feature_table (Informative)

 CREATE TABLE sample_feature_table (
   id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
   geometry GEOMETRY,
   text_attribute TEXT,
   real_attribute REAL,
   boolean_attribute BOOLEAN,
   raster_or_photo BLOB
);

-- EXAMPLE: gpkg_tile_matrix Insert Statement (Informative)

INSERT INTO gpkg_tile_matrix VALUES (
  "sample_tile_pyramid",
  0,
  1,
  1,
  512,
  512,
  2.0,
  2.0
)

-- C.7. sample_tile_pyramid (Informative)

-- EXAMPLE: tiles table Create Table SQL (Informative)

CREATE TABLE sample_tile_pyramid (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  zoom_level INTEGER NOT NULL,
  tile_column INTEGER NOT NULL,
  tile_row INTEGER NOT NULL,
  tile_data BLOB NOT NULL,
  UNIQUE (zoom_level, tile_column, tile_row)
)

-- EXAMPLE: tiles table Insert Statement (Informative)

INSERT INTO sample_matrix_pyramid VALUES (
  1,
  1,
  1,
  1,
  "BLOB VALUE"
)


-- C.9. sample_attributes_table (Informative)

-- EXAMPLE: Attributes table Create Table SQL (Informative)

CREATE TABLE sample_attributes (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  text_attribute TEXT,
  real_attribute REAL,
  boolean_attribute BOOLEAN,
  raster_or_photo BLOB
 )

-- EXAMPLE: attributes table Insert Statement (Informative)

INSERT INTO sample_attributes(text_attribute, real_attribute, boolean_attribute, raster_or_photo) VALUES (
  "place",
  1,
  true,
  "BLOB VALUE"
)
