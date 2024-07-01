--
--  WKT-2 extension of geopackage.
--  Reference : https://www.geopackage.org/spec120/index.html#extension_crs_wkt
--  Author:  Johann Sorel
--


-- CREATE TABLE gpkg_spatial_ref_sys (
--   srs_name TEXT NOT NULL,
--   srs_id INTEGER NOT NULL PRIMARY KEY,
--   organization TEXT NOT NULL,
--   organization_coordsys_id INTEGER NOT NULL,
--   definition  TEXT NOT NULL DEFAULT 'undefined',
--   description TEXT,
--   definition_12_063 TEXT NOT NULL DEFAULT 'undefined'
-- );


ALTER TABLE gpkg_spatial_ref_sys
ADD COLUMN definition_12_063 TEXT NOT NULL DEFAULT 'undefined';