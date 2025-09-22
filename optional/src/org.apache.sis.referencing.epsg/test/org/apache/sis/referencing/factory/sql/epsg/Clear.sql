--
-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
-- http://www.apache.org/licenses/LICENSE-2.0
--


--
-- Delete all EPSG tables. Useful for repeated tests of data insertions.
-- The statements are provided in two versions: first with table names as
-- distributed by EPSG, then with table names as used by Apache SIS.
--
DROP TABLE "epsg_alias" CASCADE;
DROP TABLE "epsg_change" CASCADE;
DROP TABLE "epsg_conventionalrs" CASCADE;
DROP TABLE "epsg_coordinateaxis" CASCADE;
DROP TABLE "epsg_coordinateaxisname" CASCADE;
DROP TABLE "epsg_coordinatereferencesystem" CASCADE;
DROP TABLE "epsg_coordinatesystem" CASCADE;
DROP TABLE "epsg_coordoperation" CASCADE;
DROP TABLE "epsg_coordoperationmethod" CASCADE;
DROP TABLE "epsg_coordoperationparam" CASCADE;
DROP TABLE "epsg_coordoperationparamusage" CASCADE;
DROP TABLE "epsg_coordoperationparamvalue" CASCADE;
DROP TABLE "epsg_coordoperationpath" CASCADE;
DROP TABLE "epsg_datum" CASCADE;
DROP TABLE "epsg_datumensemble" CASCADE;
DROP TABLE "epsg_datumensemblemember" CASCADE;
DROP TABLE "epsg_datumrealizationmethod" CASCADE;
DROP TABLE "epsg_definingoperation" CASCADE;
DROP TABLE "epsg_deprecation" CASCADE;
DROP TABLE "epsg_ellipsoid" CASCADE;
DROP TABLE "epsg_extent" CASCADE;
DROP TABLE "epsg_namingsystem" CASCADE;
DROP TABLE "epsg_primemeridian" CASCADE;
DROP TABLE "epsg_scope" CASCADE;
DROP TABLE "epsg_supersession" CASCADE;
DROP TABLE "epsg_unitofmeasure" CASCADE;
DROP TABLE "epsg_usage" CASCADE;
DROP TABLE "epsg_versionhistory" CASCADE;

-- Version with table names as used by Apache SIS.
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
