-- Licensed under the Apache License, Version 2.0 (the "License").
-- https://www.ogc.org/about-ogc/policies/software-licenses/
--
-- Extension schema of OGC Geopackage, optional in Geopackage files.
-- The original schema is published by OGC in the Geopackage specification.
-- The schema below contains the following modifications, applied by Apache SIS:
--
--   * Formatting and comments.
--
-- Reference: http://www.geopackage.org/guidance/extensions/tiled_gridded_coverage_data.html

-- TODO: not yet used by SIS.
CREATE TABLE IF NOT EXISTS gpkg_2d_gridded_coverage_ancillary (
    id                   INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    tile_matrix_set_name TEXT    NOT NULL UNIQUE,
    datatype             TEXT    NOT NULL DEFAULT 'integer',
    scale                REAL    NOT NULL DEFAULT 1.0,
    offset               REAL    NOT NULL DEFAULT 0.0,
    precision            REAL             DEFAULT 1.0,
    data_null            REAL,
    grid_cell_encoding   TEXT DEFAULT 'grid-value-is-center',
    uom                  TEXT,
    field_name           TEXT DEFAULT 'Height',
    quantity_definition  TEXT DEFAULT 'Height',
    CONSTRAINT fk_g2dgtct_name FOREIGN KEY('tile_matrix_set_name') REFERENCES gpkg_tile_matrix_set(table_name)
    CHECK (datatype in ('integer','float'))
);

-- TODO: not yet used by SIS.
CREATE TABLE IF NOT EXISTS gpkg_2d_gridded_tile_ancillary (
    id         INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    tpudt_name TEXT    NOT NULL,
    tpudt_id   INTEGER NOT NULL,
    scale      REAL    NOT NULL DEFAULT 1.0,
    offset     REAL    NOT NULL DEFAULT 0.0,
    min        REAL    DEFAULT NULL,
    max        REAL    DEFAULT NULL,
    mean       REAL    DEFAULT NULL,
    std_dev    REAL    DEFAULT NULL,
    CONSTRAINT fk_g2dgtat_name FOREIGN KEY (tpudt_name) REFERENCES gpkg_contents(table_name),
    UNIQUE (tpudt_name, tpudt_id)
);

-- TODO: needs to ensure that the gpkg_extensions table exits.
INSERT INTO gpkg_extensions(table_name, column_name, extension_name, definition, scope) VALUES
('gpkg_2d_gridded_coverage_ancillary', null, 'gpkg_2d_gridded_coverage', 'http://docs.opengeospatial.org/is/17-066r1/17-066r1.html', 'read-write'),
('gpkg_2d_gridded_tile_ancillary',     null, 'gpkg_2d_gridded_coverage', 'http://docs.opengeospatial.org/is/17-066r1/17-066r1.html', 'read-write');
