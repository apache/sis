--
-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
-- http://www.apache.org/licenses/LICENSE-2.0
--

--
-- Definition of file formats. Those entries are referenced by DataStore implementations.
-- This script requires "Citations.sql" to be executed first.
--
CREATE TABLE metadata."Format" (
  "ID"                          VARCHAR(15) NOT NULL PRIMARY KEY,
  "formatSpecificationCitation" VARCHAR(15) REFERENCES metadata."Citation" ("ID") ON UPDATE RESTRICT ON DELETE RESTRICT,
  "amendmentNumber"             VARCHAR(120),
  "fileDecompressionTechnique"  VARCHAR(120));

INSERT INTO metadata."Citation" ("ID", "alternateTitle", "title") VALUES
  ('GeoTIFF', 'GeoTIFF', 'GeoTIFF Coverage Encoding Profile'),
  ('NetCDF',  'NetCDF',  'NetCDF Classic and 64-bit Offset Format'),
  ('PNG',     'PNG',     'PNG (Portable Network Graphics) Specification'),
  ('CSV',     'CSV',     'Common Format and MIME Type for Comma-Separated Values (CSV) Files'),
  ('CSV-MF',  'CSV',     'OGC Moving Features Encoding Extension: Simple Comma-Separated Values (CSV)'),
  ('GPX',     'GPX',     'GPS Exchange Format');

INSERT INTO metadata."Format" ("ID", "formatSpecificationCitation") VALUES
  ('GeoTIFF', 'GeoTIFF'),
  ('NetCDF',  'NetCDF'),
  ('PNG',     'PNG'),
  ('CSV',     'CSV'),
  ('CSV-MF',  'CSV-MF'),
  ('GPX',     'GPX');



--
-- Description of bands in rasters. Not used directly by "sis-metadata" module,
-- but used by some storage modules.
--
CREATE TYPE metadata."TransferFunctionTypeCode" AS ENUM (
  'linear', 'logarithmic', 'exponential');

CREATE TABLE metadata."RangeDimension" (
  "ID"                 VARCHAR(15) NOT NULL PRIMARY KEY,
  "sequenceIdentifier" VARCHAR(15),
  "description"        VARCHAR(120)
);

CREATE TABLE metadata."SampleDimension" (
  "ID"                   VARCHAR(15) NOT NULL PRIMARY KEY,
  "sequenceIdentifier"   VARCHAR(15),
  "description"          VARCHAR(120),
  "minValue"             DOUBLE PRECISION,
  "maxValue"             DOUBLE PRECISION,
  "meanValue"            DOUBLE PRECISION,
  "numberOfValues"       INTEGER,
  "standardDeviation"    DOUBLE PRECISION,
  "units"                VARCHAR(15),
  "scaleFactor"          DOUBLE PRECISION,
  "offset"               DOUBLE PRECISION,
  "transferFunctionType" metadata."TransferFunctionTypeCode")
INHERITS (metadata."RangeDimension");
