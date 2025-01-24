--
-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
-- http://www.apache.org/licenses/LICENSE-2.0
--

--
-- This script creates some tables needed for SIS predefined Citation constants.
-- We do not need to create all tables or all table columns here; missing tables
-- and columns will be added on-the-fly by SIS as needed. Enumeration values are
-- replaced by VARCHAR on databases that do not support the ENUM type.
--
-- In this schema, the following arbitrary lengths are used:
--
--   VARCHAR(15)  for primary keys or foreigner keys.
--   VARCHAR(120) for character sequences.
--
CREATE SCHEMA metadata;
GRANT USAGE ON SCHEMA metadata TO PUBLIC;
COMMENT ON SCHEMA metadata IS 'ISO 19115 metadata';



--
-- All URLs referenced in this SQL file. Unless otherwise specified, the function of all those URLs
-- is 'information'. URLs may appear in citations or in contact information of responsible parties.
--
CREATE TYPE metadata."OnLineFunctionCode" AS ENUM (
  'download', 'information', 'offlineAccess', 'order', 'search', 'completeMetadata', 'browseGraphic',
  'upload', 'emailService', 'browsing', 'fileAccess');

CREATE TABLE metadata."OnlineResource" (
  "ID"       VARCHAR(15) NOT NULL PRIMARY KEY,
  "linkage"  VARCHAR(120),
  "function" metadata."OnLineFunctionCode");

INSERT INTO metadata."OnlineResource" ("ID", "linkage") VALUES
  ('EPSG',    'https://epsg.org/'),
  ('ESRI',    'https://www.esri.com/'),
  ('GDAL',    'https://gdal.org/'),
  ('GeoTIFF', 'https://www.ogc.org/publications/standard/geotiff/'),
  ('IHO',     'https://www.iho.int/'),
  ('IOGP',    'https://www.iogp.org/'),
  ('ISBN',    'https://www.isbn-international.org/'),
  ('ISSN',    'https://www.issn.org/'),
  ('ISO',     'https://www.iso.org/'),
  ('NetCDF',  'https://www.unidata.ucar.edu/software/netcdf/'),
  ('OGC',     'https://www.ogc.org/'),
  ('OGCNA',   'https://www.ogc.org/ogcna'),
  ('Oracle',  'https://www.oracle.com/'),
  ('OSGeo',   'https://www.osgeo.org/'),
  ('PostGIS', 'https://postgis.net/'),
  ('PROJ',    'https://proj.org/'),
  ('SIS',     'https://sis.apache.org/'),
  ('Unidata', 'https://www.unidata.ucar.edu/'),
  ('WMO',     'https://www.wmo.int/'),
  ('WMS',     'https://www.ogc.org/standards/wms');

UPDATE metadata."OnlineResource" SET "function" = 'information';



--
-- The "ID" and "name" columns in "Organisation" table are inherited from the "Party" parent table.
-- But we nevertheless repeat those columns for databases that do not support table inheritance.
-- On PostgreSQL, those duplicated declarations are merged in single columns.
--
CREATE TABLE metadata."Party" (
  "ID"   VARCHAR(15) NOT NULL PRIMARY KEY,
  "name" VARCHAR(120));

CREATE TABLE metadata."Organisation" (
  "ID"   VARCHAR(15) NOT NULL PRIMARY KEY,
  "name" VARCHAR(120))
INHERITS (metadata."Party");

CREATE TYPE metadata."RoleCode" AS ENUM (
  'resourceProvider', 'custodian', 'owner', 'user', 'distributor', 'originator', 'pointOfContact',
  'principalInvestigator', 'processor', 'publisher', 'author', 'sponsor', 'coAuthor', 'collaborator',
  'editor', 'mediator', 'rightsHolder', 'contributor', 'funder', 'stakeholder');



--
-- Foreigner key should reference the "Party" parent table, but it does not yet work on PostgreSQL
-- (tested on 9.5.13). For the purpose of this file, we need to reference "Organisation" only.
-- This constraint can be dropped at end of the installation scripts.
--
CREATE TABLE metadata."Responsibility" (
  "ID"    VARCHAR(15) NOT NULL PRIMARY KEY,
  "role"  metadata."RoleCode",
  "party" VARCHAR(15) REFERENCES metadata."Organisation" ("ID") ON UPDATE RESTRICT ON DELETE RESTRICT);



--
-- All parties referenced in this SQL file. We currently have only organisations, no individuals.
-- This SQL file has a one-to-one relationship between "Party" (organisation) and "Responsibility"
-- but sometimes with different identifiers for emphasising on the product rather than the company.
--
INSERT INTO metadata."Organisation" ("ID", "name") VALUES
  ('{org}Apache', 'The Apache Software Foundation'),
  ('{org}ESRI',   'ESRI'),
  ('{org}IHO',    'International Hydrographic Organization'),
  ('{org}IOGP',   'International Association of Oil & Gas producers'),
  ('{org}ISBN',   'International ISBN Agency'),
  ('{org}ISSN',   'The International Centre for the registration of serial publications'),
  ('{org}ISO',    'International Organization for Standardization'),
  ('{org}MIF',    'Precisely'),
  ('{org}NATO',   'North Atlantic Treaty Organization'),
  ('{org}OGC',    'Open Geospatial Consortium'),
  ('{org}OSGeo',  'The Open Source Geospatial Foundation'),
  ('{org}UCAR',   'University Corporation for Atmospheric Research'),
  ('{org}WMO',    'World Meteorological Organization');

INSERT INTO metadata."Responsibility" ("ID", "party", "role") VALUES
  ('Apache',  '{org}Apache', 'resourceProvider'),
  ('ESRI',    '{org}ESRI',   'principalInvestigator'),
  ('IHO',     '{org}IHO',    'principalInvestigator'),
  ('IOGP',    '{org}IOGP',   'principalInvestigator'),
  ('ISBN',    '{org}ISBN',   'principalInvestigator'),
  ('ISSN',    '{org}ISSN',   'principalInvestigator'),
  ('ISO',     '{org}ISO',    'principalInvestigator'),
  ('MapInfo', '{org}MIF',    'principalInvestigator'),
  ('NATO',    '{org}NATO',   'principalInvestigator'),
  ('OGC',     '{org}OGC',    'principalInvestigator'),
  ('OSGeo',   '{org}OSGeo',  'resourceProvider'),
  ('UCAR',    '{org}UCAR',   'resourceProvider'),
  ('WMO',     '{org}WMO',    'principalInvestigator');



--
-- Definition of the Citations and its dependencies.
--
CREATE TYPE metadata."DateTypeCode" AS ENUM (
  'creation', 'publication', 'revision', 'expiry', 'lastUpdate', 'lastRevision', 'nextUpdate',
  'unavailable', 'inForce', 'adopted', 'deprecated', 'superseded', 'validityBegins', 'validityExpires',
  'released', 'distribution');

CREATE TABLE metadata."Date" (
  "ID"       VARCHAR(15) NOT NULL PRIMARY KEY,
  "date"     TIMESTAMP,
  "dateType" metadata."DateTypeCode");

CREATE TYPE metadata."PresentationFormCode" AS ENUM (
  'documentDigital', 'documentHardcopy',
  'imageDigital',    'imageHardcopy',
  'mapDigital',      'mapHardcopy',
  'modelDigital',    'modelHardcopy',
  'profileDigital',  'profileHardcopy',
  'tableDigital',    'tableHardcopy',
  'videoDigital',    'videoHardcopy');

CREATE TABLE metadata."Identifier" (
  "ID"        VARCHAR(15) NOT NULL PRIMARY KEY,
  "authority" VARCHAR(15),
  "code"      VARCHAR(120),
  "codeSpace" VARCHAR(120),
  "version"   VARCHAR(120));

CREATE TABLE metadata."Citation" (
  "ID"                    VARCHAR(15) NOT NULL PRIMARY KEY,
  "title"                 VARCHAR(120),
  "alternateTitle"        VARCHAR(120),
  "date"                  VARCHAR(15) REFERENCES metadata."Date" ("ID") ON UPDATE RESTRICT ON DELETE RESTRICT,
  "edition"               VARCHAR(120),
  "editionDate"           TIMESTAMP,
  "identifier"            VARCHAR(15) REFERENCES metadata."Identifier"     ("ID") ON UPDATE RESTRICT ON DELETE RESTRICT,
  "citedResponsibleParty" VARCHAR(15) REFERENCES metadata."Responsibility" ("ID") ON UPDATE RESTRICT ON DELETE RESTRICT,
  "presentationForm"      metadata."PresentationFormCode",
  "onlineResource"        VARCHAR(15) REFERENCES metadata."OnlineResource" ("ID") ON UPDATE RESTRICT ON DELETE RESTRICT);

ALTER TABLE metadata."Identifier" ADD CONSTRAINT fk_identifier_citation
FOREIGN KEY ("authority") REFERENCES metadata."Citation" ("ID") ON UPDATE RESTRICT ON DELETE RESTRICT;



--
-- Specifications, data or softwares referenced by the "Citations" class.
-- Those citations are not organizations; they are resources published by
-- organizations. Each identifier codespace identifies the organization.
--
-- Some citations are used as "authority" for defining codes in a codespace
-- (for example EPSG codes). The authority codespace is not necessarily the
-- code or codespace of the citation's identifier. The authority codespaces
-- are hard-coded in the Citations class and do not appear here.
--
-- Rows below are sorted in specifications first, then tables, then softwares.
-- There is almost a one-to-one relationship between identifiers and citations.
--
INSERT INTO metadata."Identifier" ("ID", "code", "codeSpace", "version") VALUES
  ('ISO 19115-1', '19115-1', 'ISO',      '2014'),
  ('ISO 19115-2', '19115-2', 'ISO',      '2019'),
  ('IHO S-57',    'S-57',    'IHO',      '3.1'),
  ('WMS',         'WMS',     'OGC',      '1.3'),
  ('EPSG',        'EPSG',    'IOGP',      NULL),
  ('ArcGIS',      'ArcGIS',  'ESRI',      NULL),
  ('MapInfo',     'MapInfo', 'Precisely', NULL),
  ('PROJ',        'PROJ',    'OSGeo',     NULL),
  ('GDAL',        'GDAL',    'OSGeo',     NULL),
  ('SIS',         'SIS',     'Apache',    NULL);

INSERT INTO metadata."Citation" ("ID", "onlineResource", "edition", "citedResponsibleParty", "presentationForm", "alternateTitle" , "title") VALUES
  ('ISBN',       'ISBN',    NULL,              'ISBN',    NULL,             'ISBN',         'International Standard Book Number'),
  ('ISSN',       'ISSN',    NULL,              'ISSN',    NULL,             'ISSN',         'International Standard Serial Number'),
  ('ISO 19115-1', NULL,    'ISO 19115-1:2014', 'ISO',    'documentDigital', 'ISO 19115-1',  'Geographic Information — Metadata Part 1: Fundamentals'),
  ('ISO 19115-2', NULL,    'ISO 19115-2:2019', 'ISO',    'documentDigital', 'ISO 19115-2',  'Geographic Information — Metadata Part 2: Extensions for imagery and gridded data'),
  ('IHO S-57',    NULL,    '3.1',              'IHO',    'documentDigital', 'S-57',         'IHO transfer standard for digital hydrographic data'),
  ('MGRS',        NULL,     NULL,              'NATO',   'documentDigital',  NULL,          'Military Grid Reference System'),
  ('WMS',        'WMS',    '1.3',              'OGC',    'documentDigital', 'WMS',          'Web Map Server'),
  ('EPSG',       'EPSG',    NULL,              'IOGP',   'tableDigital',    'EPSG Dataset', 'EPSG Geodetic Parameter Dataset'),
  ('ArcGIS',     'ESRI',    NULL,              'ESRI',    NULL,              NULL,          'ArcGIS'),
  ('MapInfo',     NULL,     NULL,              'MapInfo', NULL,             'MapInfo',      'MapInfo Pro'),
  ('PROJ',       'PROJ',    NULL,              'OSGeo',   NULL,             'Proj',         'PROJ coordinate transformation software library'),
  ('GDAL',       'GDAL',    NULL,              'OSGeo',   NULL,             'GDAL',         'Geospatial Data Abstraction Library'),
  ('Unidata',    'Unidata', NULL,              'UCAR',    NULL,              NULL,          'Unidata netCDF library'),
  ('SIS',        'SIS',     NULL,              'Apache',  NULL,             'Apache SIS',   'Apache Spatial Information System');



--
-- Citations for organizations. They should not be citations; they are "responsible parties" instead.
-- But we have to declare some organizations as "citations" because this is the kind of object required
-- by the "authority" attribute of factories.
--
-- Instead of repeating the organization name, the title should reference some naming authority
-- in that organization. The identifier should have no codespace, and the identifier code should
-- be the codespace of objects created by the authority represented by that organisation.
--
INSERT INTO metadata."Identifier" ("ID", "code") VALUES
  ('OGC',  'OGC'),
  ('WMO',  'WMO'),
  ('IOGP', 'IOGP');

INSERT INTO metadata."Citation" ("ID", "onlineResource", "citedResponsibleParty", "presentationForm", "title") VALUES
  ('OGC',  'OGCNA', 'OGC',  'documentDigital', 'OGC Naming Authority'),
  ('WMO',  'WMO',   'WMO',  'documentDigital', 'WMO Information System (WIS)'),
  ('IOGP', 'IOGP',  'IOGP', 'documentDigital', 'IOGP Surveying and Positioning Guidance Note 7');

UPDATE metadata."Citation" SET "identifier" = "ID" WHERE "ID"<>'ISBN' AND "ID"<>'ISSN' AND "ID"<>'MGRS' AND "ID"<>'Unidata';
