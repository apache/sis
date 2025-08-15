--
-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
-- http://www.apache.org/licenses/LICENSE-2.0
--


--
-- Delete all EPSG tables. Useful for repeated tests of data insertions.
--

DROP TABLE "Alias" CASCADE;
DROP TABLE "Change" CASCADE;
DROP TABLE "Conventional RS" CASCADE;
DROP TABLE "Coordinate Axis" CASCADE;
DROP TABLE "Coordinate Axis Name" CASCADE;
DROP TABLE "Coordinate Reference System" CASCADE;
DROP TABLE "Coordinate System" CASCADE;
DROP TABLE "Coordinate_Operation" CASCADE;
DROP TABLE "Coordinate_Operation Method" CASCADE;
DROP TABLE "Coordinate_Operation Parameter" CASCADE;
DROP TABLE "Coordinate_Operation Parameter Usage" CASCADE;
DROP TABLE "Coordinate_Operation Parameter Value" CASCADE;
DROP TABLE "Coordinate_Operation Path" CASCADE;
DROP TABLE "Datum" CASCADE;
DROP TABLE "Datum Ensemble" CASCADE;
DROP TABLE "Datum Ensemble Member" CASCADE;
DROP TABLE "Datum Realization Method" CASCADE;
DROP TABLE "Defining Operation" CASCADE;
DROP TABLE "Deprecation" CASCADE;
DROP TABLE "Ellipsoid" CASCADE;
DROP TABLE "Extent" CASCADE;
DROP TABLE "Naming System" CASCADE;
DROP TABLE "Prime Meridian" CASCADE;
DROP TABLE "Scope" CASCADE;
DROP TABLE "Supersession" CASCADE;
DROP TABLE "Unit of Measure" CASCADE;
DROP TABLE "Usage" CASCADE;
DROP TABLE "Version History" CASCADE;
DROP TYPE  "Datum Kind" CASCADE;
DROP TYPE  "CRS Kind" CASCADE;
DROP TYPE  "CS Kind" CASCADE;
DROP TYPE  "Supersession Type" CASCADE;
DROP TYPE  "Table Name" CASCADE;
