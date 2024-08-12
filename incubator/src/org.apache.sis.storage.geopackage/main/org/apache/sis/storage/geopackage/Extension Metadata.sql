-- Licensed under the Apache License, Version 2.0 (the "License").
-- https://www.ogc.org/about-ogc/policies/software-licenses/
--
-- Extension schema of OGC Geopackage, optional in Geopackage files.
-- The original schema is published by OGC in the Geopackage specification.
-- The schema below contains the following modifications, applied by Apache SIS:
--
--   * Formatting and comments.
--   * Missing NOT NULL constraint added on a primary key.
--   * TODO: can we replace strftime by (datetime('now'))?
--

-- TODO: not yet used by SIS.
CREATE TABLE gpkg_metadata (
  id              INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  md_scope        TEXT    NOT NULL DEFAULT 'dataset',
  md_standard_uri TEXT    NOT NULL,
  mime_type       TEXT    NOT NULL DEFAULT 'text/xml',
  metadata        TEXT    NOT NULL DEFAULT ''
);

-- TODO: not yet used by SIS.
CREATE TABLE gpkg_metadata_reference (
  reference_scope TEXT     NOT NULL,
  table_name      TEXT,
  column_name     TEXT,
  row_id_value    INTEGER,
  timestamp       DATETIME NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
  md_file_id      INTEGER  NOT NULL,
  md_parent_id    INTEGER,
  CONSTRAINT crmr_mfi_fk FOREIGN KEY (md_file_id)   REFERENCES gpkg_metadata(id),
  CONSTRAINT crmr_mpi_fk FOREIGN KEY (md_parent_id) REFERENCES gpkg_metadata(id)
);

-- TODO: needs to ensure that the gpkg_extensions table exits.
INSERT INTO gpkg_extensions (table_name, extension_name, definition, scope) VALUES
('gpkg_metadata',           'gpkg_metadata', 'https://www.geopackage.org/spec/#extension_metadata', 'read-write'),
('gpkg_metadata_reference', 'gpkg_metadata', 'https://www.geopackage.org/spec/#extension_metadata', 'read-write');
