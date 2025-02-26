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

INSERT INTO metadata."Citation" ("ID", "alternateTitle", "citedResponsibleParty", "title") VALUES
  ('GeoTIFF', 'GeoTIFF',    'OGC',  'GeoTIFF Coverage Encoding Profile'),
  ('GeoHEIF', 'GeoHEIF',     NULL,  'GeoHEIF Coverage Encoding'),
  ('NetCDF',  'NetCDF',     'OGC',  'NetCDF Classic and 64-bit Offset Format'),
  ('PNG',     'PNG',         NULL,  'PNG (Portable Network Graphics) Specification'),
  ('RAWGRD',   NULL,        'ESRI', 'BIL, BIP, and BSQ raster files'),
  ('ASCGRD',  'ASCII Grid', 'ESRI', 'ESRI ArcInfo ASCII Grid format'),
  ('CSV',     'CSV',         NULL,  'Common Format and MIME Type for Comma-Separated Values (CSV) Files'),
  ('CSV-MF',  'CSV',        'OGC',  'OGC Moving Features Encoding Extension: Simple Comma-Separated Values (CSV)'),
  ('GPX',     'GPX',         NULL,  'GPS Exchange Format');

INSERT INTO metadata."Format" ("ID", "formatSpecificationCitation") VALUES
  ('GeoTIFF', 'GeoTIFF'),
  ('GeoHEIF', 'GeoHEIF'),
  ('NetCDF',  'NetCDF'),
  ('PNG',     'PNG'),
  ('RAWGRD',  'RAWGRD'),
  ('ASCGRD',  'ASCGRD'),
  ('CSV',     'CSV'),
  ('CSV-MF',  'CSV-MF'),
  ('GPX',     'GPX');



--
-- Description of bands in rasters. Not used directly by "org.apache.sis.metadata" module,
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

CREATE TABLE metadata."Band" (
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
  "transferFunctionType" metadata."TransferFunctionTypeCode",
  "boundMin"             DOUBLE PRECISION,
  "boundMax"             DOUBLE PRECISION,
  "boundUnits"           VARCHAR(15),
  "peakResponse"         DOUBLE PRECISION)
INHERITS (metadata."SampleDimension");

--
-- TODO: move those declarations in sis-earthobservation module.
-- https://issues.apache.org/jira/browse/SIS-338
--
INSERT INTO metadata."Band" ("ID", "sequenceIdentifier", "description", "boundMin", "boundMax", "boundUnits", "peakResponse") VALUES
  ('Landsat 8-01',  '1', 'Ultra Blue (coastal/aerosol)',         435.0,  451.0, 'nm',   443.0),
  ('Landsat 8-02',  '2', 'Blue',                                 452.0,  512.1, 'nm',   482.0),
  ('Landsat 8-03',  '3', 'Green',                                532.7,  590.1, 'nm',   561.4),
  ('Landsat 8-04',  '4', 'Red',                                  635.9,  673.3, 'nm',   654.6),
  ('Landsat 8-05',  '5', 'Near-Infrared',                        850.5,  878.8, 'nm',   864.7),
  ('Landsat 8-06',  '6', 'Short Wavelength Infrared (SWIR) 1',  1566.5, 1651.2, 'nm',  1608.9),
  ('Landsat 8-07',  '7', 'Short Wavelength Infrared (SWIR) 2',  2107.4, 2294.1, 'nm',  2200.7),
  ('Landsat 8-08',  '8', 'Panchromatic',                         503.3,  675.7, 'nm',   589.5),
  ('Landsat 8-09',  '9', 'Cirrus',                              1363.2, 1383.6, 'nm',  1373.4),
  ('Landsat 8-10', '10', 'Thermal Infrared Sensor (TIRS) 1',     10.60,  11.19, 'µm',  10.800),
  ('Landsat 8-11', '11', 'Thermal Infrared Sensor (TIRS) 2',     11.50,  12.51, 'µm',  12.000);
