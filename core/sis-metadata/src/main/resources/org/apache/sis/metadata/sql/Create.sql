--
-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
-- http://www.apache.org/licenses/LICENSE-2.0
--

CREATE SCHEMA metadata;
GRANT USAGE ON SCHEMA metadata TO PUBLIC;
COMMENT ON SCHEMA metadata IS 'ISO 19115 metadata';


--
-- CodeLists are represented as enumeration on PostgreSQL.
-- Those declarations will be omitted on databases that do
-- no support enumerations; VARCHAR is used instead.
--
CREATE TYPE metadata."CI_PresentationFormCode" AS ENUM (
  'documentDigital', 'documentHardcopy',
  'imageDigital',    'imageHardcopy',
  'mapDigital',      'mapHardcopy',
  'modelDigital',    'modelHardcopy',
  'profileDigital',  'profileHardcopy',
  'tableDigital',    'tableHardcopy',
  'videoDigital',    'videoHardcopy');

CREATE TYPE metadata."CI_RoleCode" AS ENUM (
  'resourceProvider', 'custodian', 'owner', 'user', 'distributor', 'originator', 'pointOfContact',
  'principalInvestigator', 'processor', 'publisher', 'author', 'sponsor', 'coAuthor', 'collaborator',
  'editor', 'mediator', 'rightsHolder', 'contributor', 'funder', 'stakeholder');

CREATE TYPE metadata."CI_DateTypeCode" AS ENUM (
  'creation', 'publication', 'revision', 'expiry', 'lastUpdate', 'lastRevision', 'nextUpdate',
  'unavailable', 'inForce', 'adopted', 'deprecated', 'superseded', 'validityBegins', 'validityExpires',
  'released', 'distribution');

CREATE CAST (VARCHAR AS metadata."CI_PresentationFormCode") WITH INOUT AS ASSIGNMENT;
CREATE CAST (VARCHAR AS metadata."CI_RoleCode")             WITH INOUT AS ASSIGNMENT;
CREATE CAST (VARCHAR AS metadata."CI_DateTypeCode")         WITH INOUT AS ASSIGNMENT;


--
-- This script creates some tables needed for SIS pre-defined metadata.
-- We do not need to create all tables or all columns in tables here.
-- Missing tables and columns will be added on-the-fly by SIS as needed.
--
-- VARCHAR(10) are for primary keys or foreigner keys.
-- VARCHAR(80) are for character sequences.
--
CREATE TABLE metadata."MD_Identifier" (
  ID                            VARCHAR(10) NOT NULL PRIMARY KEY,
  "authority"                   VARCHAR(10),
  "code"                        VARCHAR(80),
  "codeSpace"                   VARCHAR(80),
  "version"                     VARCHAR(80));

CREATE TABLE metadata."CI_Party" (
  ID                            VARCHAR(10) NOT NULL PRIMARY KEY,
  "name"                        VARCHAR(80));

CREATE TABLE metadata."CI_Responsibility" (
  ID                            VARCHAR(10) NOT NULL PRIMARY KEY,
  "role"                        metadata."CI_RoleCode",
  "party"                       VARCHAR(10) REFERENCES metadata."CI_Party" (ID) ON UPDATE RESTRICT ON DELETE RESTRICT);

CREATE TABLE metadata."CI_Date" (
  ID                            VARCHAR(10) NOT NULL PRIMARY KEY,
  "date"                        TIMESTAMP,
  "dateType"                    metadata."CI_DateTypeCode");

CREATE TABLE metadata."CI_Citation" (
  ID                            VARCHAR(10) NOT NULL PRIMARY KEY,
  "title"                       VARCHAR(80),
  "alternateTitle"              VARCHAR(80),
  "date"                        VARCHAR(10) REFERENCES metadata."CI_Date" (ID) ON UPDATE RESTRICT ON DELETE RESTRICT,
  "edition"                     VARCHAR(80),
  "editionDate"                 TIMESTAMP,
  "identifier"                  VARCHAR(10) REFERENCES metadata."MD_Identifier"     (ID) ON UPDATE RESTRICT ON DELETE RESTRICT,
  "citedResponsibleParty"       VARCHAR(10) REFERENCES metadata."CI_Responsibility" (ID) ON UPDATE RESTRICT ON DELETE RESTRICT,
  "presentationForm"            metadata."CI_PresentationFormCode");

ALTER TABLE metadata."MD_Identifier" ADD CONSTRAINT fk_identifier_citation
FOREIGN KEY ("authority") REFERENCES metadata."CI_Citation" (ID) ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE TABLE metadata."MD_Format" (
  ID                            VARCHAR(10) NOT NULL PRIMARY KEY,
  "formatSpecificationCitation" VARCHAR(10) REFERENCES metadata."CI_Citation" (ID) ON UPDATE RESTRICT ON DELETE RESTRICT,
  "amendmentNumber"             VARCHAR(80),
  "fileDecompressionTechnique"  VARCHAR(80));


--
-- Metadata about file formats.
--
INSERT INTO metadata."CI_Citation" (ID, "alternateTitle", "title") VALUES
  ('GeoTIFF', 'GeoTIFF', 'GeoTIFF Coverage Encoding Profile'),
  ('NetCDF',  'NetCDF',  'NetCDF Classic and 64-bit Offset Format'),
  ('PNG',     'PNG',     'PNG (Portable Network Graphics) Specification'),
  ('CSV',     'CSV',     'Common Format and MIME Type for Comma-Separated Values (CSV) Files'),
  ('CSV-MF',  'CSV',     'OGC Moving Features Encoding Extension: Simple Comma-Separated Values (CSV)');

INSERT INTO metadata."MD_Format" (ID, "formatSpecificationCitation") VALUES
  ('GeoTIFF', 'GeoTIFF'),
  ('NetCDF',  'NetCDF'),
  ('PNG',     'PNG'),
  ('CSV',     'CSV'),
  ('CSV-MF',  'CSV-MF');
