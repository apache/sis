--
-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
-- http://www.apache.org/licenses/LICENSE-2.0
--


--
-- Enumerated values to be used instead of VARCHAR. Those values match switch cases in the Apache SIS code.
-- If those values change, then the Apache SIS code would likely needs to be updated (see EPSGDataAccess).
-- If enumerated values are not supported by the database, Apache SIS will automatically replace their usage
-- by the VARCHAR type.
--
CREATE TYPE "Datum Kind"        AS ENUM ('geodetic', 'vertical', 'temporal', 'parametric', 'engineering', 'dynamic geodetic', 'ensemble');
CREATE TYPE "CRS Kind"          AS ENUM ('geocentric', 'geographic 2D', 'geographic 3D', 'projected', 'vertical', 'temporal', 'parametric', 'engineering', 'derived', 'compound');
CREATE TYPE "CS Kind"           AS ENUM ('ellipsoidal', 'spherical', 'Cartesian', 'vertical', 'gravity-related', 'time', 'linear', 'polar', 'cylindrical', 'affine', 'ordinal');
CREATE TYPE "Supersession Type" AS ENUM ('Supersession');
CREATE TYPE "Table Name"        AS ENUM
   ('Alias',
    'Area',         -- Deprecated (removed in EPSG 10).
    'Change',
    'Conventional RS',
    'Coordinate Axis',
    'Coordinate Axis Name',
    'Coordinate_Operation',
    'Coordinate_Operation Method',
    'Coordinate_Operation Parameter',
    'Coordinate_Operation Parameter Type',   -- Only in online registry.
    'Coordinate_Operation Parameter Usage',
    'Coordinate_Operation Parameter Value',
    'Coordinate_Operation Path',
    'Coordinate Reference System',
    'Coordinate System',
    'Datum',
    'Datum Ensemble',
    'Datum Ensemble Member',
    'Datum Realization Method',
    'Defining Operation',
    'Deprecation',
    'Ellipsoid',
    'Extent',
    'Naming System',
    'Prime Meridian',
    'Scope',
    'Supersession',
    'Unit of Measure',
    'Usage',
    'Version History');

--
-- Those casts allow to use enumerated values as if they were VARCHAR elements.
--
CREATE CAST (VARCHAR AS "Datum Kind")        WITH INOUT AS ASSIGNMENT;
CREATE CAST (VARCHAR AS "CRS Kind")          WITH INOUT AS ASSIGNMENT;
CREATE CAST (VARCHAR AS "CS Kind")           WITH INOUT AS ASSIGNMENT;
CREATE CAST (VARCHAR AS "Supersession Type") WITH INOUT AS ASSIGNMENT;
CREATE CAST (VARCHAR AS "Table Name")        WITH INOUT AS ASSIGNMENT;

--
-- PostgreSQL: collation using the International Components for Unicode (ICU) library.
-- https://www.postgresql.org/docs/current/collation.html#ICU-COLLATION-SETTINGS
--
CREATE COLLATION "Ignore Accent and Case" (PROVIDER = 'icu', DETERMINISTIC = false, LOCALE = 'en_GB-u-ka-shifted-ks-level1');
