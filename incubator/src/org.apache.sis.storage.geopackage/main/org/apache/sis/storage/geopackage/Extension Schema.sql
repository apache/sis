-- Licensed under the Apache License, Version 2.0 (the "License").
-- https://www.ogc.org/about-ogc/policies/software-licenses/
--
-- Extension schema of OGC Geopackage, optional in Geopackage files.
-- The original schema is published by OGC in the Geopackage specification.
-- The schema below contains the following modifications, applied by Apache SIS:
--
--   * Formatting and comments.
--

-- TODO: not yet used by SIS.
CREATE TABLE gpkg_data_columns (
  table_name      TEXT NOT NULL,
  column_name     TEXT NOT NULL,
  name            TEXT,
  title           TEXT,
  description     TEXT,
  mime_type       TEXT,
  constraint_name TEXT,
  CONSTRAINT pk_gdc PRIMARY KEY (table_name, column_name),
  CONSTRAINT gdc_tn UNIQUE      (table_name, name)
);

-- TODO: not yet used by SIS.
CREATE TABLE gpkg_ data_column_constraints (
  constraint_name  TEXT NOT NULL,
  constraint_type  TEXT NOT NULL,
  value            TEXT,
  min              NUMERIC,
  min_is_inclusive BOOLEAN,
  max              NUMERIC,
  max_is_inclusive BOOLEAN,
  description      TEXT,
  CONSTRAINT gdcc_ntv UNIQUE (constraint_name, constraint_type, value)
);

-- TODO: needs to ensure that the gpkg_extensions table exits.
INSERT INTO gpkg_extensions (table_name, extension_name, definition, scope) VALUES
('gpkg_data_columns',            'gpkg_schema', 'http://www.geopackage.org/spec/#extension_schema', 'read-write'),
('gpkg_data_column_constraints', 'gpkg_schema', 'http://www.geopackage.org/spec/#extension_schema', 'read-write');
