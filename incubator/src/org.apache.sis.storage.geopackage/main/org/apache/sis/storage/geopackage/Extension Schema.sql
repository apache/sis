--
--  Schema extension of geopackage.
--  Reference : https://www.geopackage.org/spec120/index.html#extension_metadata
--  Author:  Johann Sorel
--

CREATE TABLE gpkg_data_columns (
  table_name TEXT NOT NULL,
  column_name TEXT NOT NULL,
  name TEXT UNIQUE,
  title TEXT,
  description TEXT,
  mime_type TEXT,
  constraint_name TEXT,
  CONSTRAINT pk_gdc PRIMARY KEY (table_name, column_name),
  CONSTRAINT fk_gdc_tn FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name)
);

CREATE TABLE gpkg_data_column_constraints (
  constraint_name TEXT NOT NULL,
  constraint_type TEXT NOT NULL, // 'range' | 'enum' | 'glob'
  value TEXT,
  min NUMERIC,
  min_is_inclusive BOOLEAN, // 0 = false, 1 = true
  max NUMERIC,
  max_is_inclusive BOOLEAN, // 0 = false, 1 = true
  description TEXT,
  CONSTRAINT gdcc_ntv UNIQUE (constraint_name, constraint_type, value)
)