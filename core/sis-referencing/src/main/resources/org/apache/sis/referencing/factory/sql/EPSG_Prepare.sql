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
CREATE TYPE epsg_datum_kind AS ENUM ('geodetic', 'vertical', 'temporal', 'engineering');
CREATE TYPE epsg_crs_kind   AS ENUM ('geocentric', 'geographic 2D', 'geographic 3D', 'projected', 'vertical', 'temporal', 'compound', 'engineering');
CREATE TYPE epsg_cs_kind    AS ENUM ('ellipsoidal', 'spherical', 'Cartesian', 'vertical', 'gravity-related', 'time', 'linear', 'polar', 'cylindrical', 'affine');
CREATE TYPE epsg_table_name AS ENUM
   ('Alias',
    'Area',
    'Change',
    'Coordinate Axis',
    'Coordinate Axis Name',
    'Coordinate_Operation',
    'Coordinate_Operation Method',
    'Coordinate_Operation Parameter',
    'Coordinate_Operation Parameter Usage',
    'Coordinate_Operation Parameter Value',
    'Coordinate_Operation Path',
    'Coordinate Reference System',
    'Coordinate System',
    'Datum',
    'Deprecation',
    'Ellipsoid',
    'Naming System',
    'Prime Meridian',
    'Supersession',
    'Unit of Measure',
    'Version History');

--
-- Those casts allow to use enumerated values as if they were VARCHAR elements.
--
CREATE CAST (VARCHAR AS epsg_datum_kind) WITH INOUT AS ASSIGNMENT;
CREATE CAST (VARCHAR AS epsg_crs_kind)   WITH INOUT AS ASSIGNMENT;
CREATE CAST (VARCHAR AS epsg_cs_kind)    WITH INOUT AS ASSIGNMENT;
CREATE CAST (VARCHAR AS epsg_table_name) WITH INOUT AS ASSIGNMENT;
