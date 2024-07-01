--
-- Vector Tiles extension of geopackage.
-- Reference : https://github.com/jyutzler/ogc-tb-15-opf/blob/master/spec/1-vte.adoc
-- Author:  Johann Sorel
--

CREATE TABLE IF NOT EXISTS gpkgext_vt_layers (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    table_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    minzoom REAL NOT NULL DEFAULT 0.0,
    maxzoom REAL NOT NULL DEFAULT 0.0,
    stylable_layer_set TEXT,
    attributes_table_name TEXT,
    CONSTRAINT fk_gpkgext_vt_layers_table_name FOREIGN KEY(table_name) REFERENCES gpkg_contents(table_name)
);

CREATE TABLE IF NOT EXISTS gpkgext_vt_fields (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    layer_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    CONSTRAINT fk_gpkgext_vt_fields_layer_id FOREIGN KEY (layer_id) REFERENCES gpkgext_vt_layers(id),
    UNIQUE (layer_id, name)
    CHECK (type in ('String','Number','Boolean'))
);


INSERT INTO gpkg_extensions(table_name, column_name, extension_name, definition, scope) VALUES
('gpkgext_vt_layers', null, 'im_vector_tiles', 'https://github.com/jyutzler/ogc-tb-15-opf/blob/master/spec/1-vte.adoc', 'read-write'),
('gpkgext_vt_fields', null, 'im_vector_tiles', 'https://github.com/jyutzler/ogc-tb-15-opf/blob/master/spec/1-vte.adoc', 'read-write');

