--
-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
-- http://www.apache.org/licenses/LICENSE-2.0
--


--
-- Creates views for manual inspections of EPSG database. We recommand to execute
-- those scripts on a PostgreSQL database installation after an EPSG update. This
-- file is not actually used in any JUnit test.
--

SET client_encoding = 'UTF8';
SET search_path = epsg;


--
-- The dimensions (number of axes) of all Coordinate Reference Systems found in the EPSG database.
-- This view is used by the next view below. The CRS names are included for information purpose.
--
CREATE VIEW "CRS dimension" AS
 SELECT CRS.COORD_REF_SYS_CODE, CRS.COORD_REF_SYS_NAME, COUNT(CA.COORD_SYS_CODE) AS DIMENSION
   FROM "Coordinate Axis" AS CA
 RIGHT JOIN "Coordinate Reference System" AS CRS ON CA.COORD_SYS_CODE = CRS.COORD_SYS_CODE
 GROUP BY COORD_REF_SYS_CODE, COORD_REF_SYS_NAME ORDER BY COORD_REF_SYS_CODE;

COMMENT ON VIEW "CRS dimension" IS 'Dimensions of all Coordinate Reference Systems.';


--
-- The source and target dimensions of all Operation Methods found in the EPSG database.
--
CREATE VIEW "OperationMethod dimension" AS
 SELECT COM.COORD_OP_METHOD_CODE, COORD_OP_METHOD_NAME, COORD_OP_TYPE, IS_CONVERSION,
        SOURCE_MIN_DIM, SOURCE_MAX_DIM, TARGET_MIN_DIM, TARGET_MAX_DIM
 FROM
    (SELECT COORD_OP_METHOD_CODE, COORD_OP_TYPE, IS_CONVERSION,
        MIN(DS.DIMENSION) AS SOURCE_MIN_DIM,
        MAX(DS.DIMENSION) AS SOURCE_MAX_DIM,
        MIN(DT.DIMENSION) AS TARGET_MIN_DIM,
        MAX(DT.DIMENSION) AS TARGET_MAX_DIM
     FROM

       (SELECT COORD_OP_METHOD_CODE, COORD_OP_TYPE, FALSE AS IS_CONVERSION, SOURCE_CRS_CODE, TARGET_CRS_CODE
          FROM "Coordinate_Operation" WHERE SOURCE_CRS_CODE IS NOT NULL OR TARGET_CRS_CODE IS NOT NULL
     UNION
        SELECT COORD_OP_METHOD_CODE, COORD_OP_TYPE, TRUE AS IS_CONVERSION, SOURCE_GEOGCRS_CODE AS SOURCE_CRS_CODE, COORD_REF_SYS_CODE AS TARGET_CRS_CODE
          FROM "Coordinate Reference System" AS CRS INNER JOIN "Coordinate_Operation" AS CO ON CRS.PROJECTION_CONV_CODE = CO.COORD_OP_CODE) AS P

     LEFT JOIN "CRS dimension" AS DS ON DS.COORD_REF_SYS_CODE = P.SOURCE_CRS_CODE
     LEFT JOIN "CRS dimension" AS DT ON DT.COORD_REF_SYS_CODE = P.TARGET_CRS_CODE
     GROUP BY COORD_OP_METHOD_CODE, COORD_OP_TYPE, IS_CONVERSION, DS.DIMENSION, DT.DIMENSION) AS DIM
 RIGHT JOIN "Coordinate_Operation Method" AS COM ON COM.COORD_OP_METHOD_CODE = DIM.COORD_OP_METHOD_CODE
 ORDER BY COM.COORD_OP_METHOD_CODE;

COMMENT ON VIEW "OperationMethod dimension" IS 'Dimensions of most Operation Methods.';


--
-- Summary of Operation Method dimensions, grouped by types (projection or not).
--
CREATE VIEW "OperationMethodType dimension" AS
 SELECT COORD_OP_TYPE, IS_CONVERSION,
    MIN(OD.SOURCE_MIN_DIM) AS SOURCE_MIN_DIM,
    MAX(OD.SOURCE_MAX_DIM) AS SOURCE_MAX_DIM,
    MIN(OD.TARGET_MIN_DIM) AS TARGET_MIN_DIM,
    MAX(OD.TARGET_MAX_DIM) AS TARGET_MAX_DIM
 FROM "OperationMethod dimension" AS OD
 GROUP BY COORD_OP_TYPE, IS_CONVERSION
 ORDER BY IS_CONVERSION;

COMMENT ON VIEW "OperationMethodType dimension" IS 'Dimensions of Operation Methods types.';
