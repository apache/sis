--
-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
-- http://www.apache.org/licenses/LICENSE-2.0
--


--
-- Corrections to some deprecated Coordinate Reference Systems (CRS).  The errors that we fix here are known to EPSG,
-- but were fixed by deprecating the erroneous CRS and creating a new one. This approach allows reproductive behavior
-- of applications that used the erroneous CRS. However in a few cases, Apache SIS cannot instantiate the deprecated
-- object without some minimal corrections. The following UPDATEs perform the minimal amount of change needed by SIS.
--

-- "Scale factor at natural origin" (EPSG:8805) shall be dimensionless (EPSG:9201), not in "m" units (EPSG:9001).
UPDATE "Coordinate_Operation Parameter Value" SET uom_code = 9201 WHERE parameter_code = 8805 AND uom_code = 9001;

-- Value 44°87′ is illegal for DMS units (EPSG:9110). A value equivalent to the erroneous DMS value is 45°27′.
UPDATE "Coordinate_Operation Parameter Value" SET parameter_value = 45.27 WHERE parameter_value = 44.87 AND uom_code = 9110;



--
-- Additional indexes for the EPSG database. Those indexes are not declared
-- in the SQL scripts distributed by EPSG. They are not required for proper
-- working of the EPSG factory, but can significantly improve performances.
--

-----------------------------------------------------------------------------
-- Indexes for queries used by EPSGDataAccess.createFoo(epsgcode) methods. --
-- Indexed fields are numeric values used mainly in equality comparisons.  --
-----------------------------------------------------------------------------
CREATE INDEX ix_alias_object_code                ON "Alias"                                (object_code);
CREATE INDEX ix_crs_datum_code                   ON "Coordinate Reference System"          (datum_code);
CREATE INDEX ix_crs_projection_code              ON "Coordinate Reference System"          (projection_conv_code);
CREATE INDEX ix_coordinate_axis_code             ON "Coordinate Axis"                      (coord_axis_code);
CREATE INDEX ix_coordinate_axis_sys_code         ON "Coordinate Axis"                      (coord_sys_code);
CREATE INDEX ix_coordinate_operation_crs         ON "Coordinate_Operation"                 (source_crs_code, target_crs_code);
CREATE INDEX ix_coordinate_operation_method_code ON "Coordinate_Operation"                 (coord_op_method_code);
CREATE INDEX ix_parameter_usage_method_code      ON "Coordinate_Operation Parameter Usage" (coord_op_method_code);
CREATE INDEX ix_parameter_values                 ON "Coordinate_Operation Parameter Value" (coord_op_code, coord_op_method_code);
CREATE INDEX ix_parameter_value_code             ON "Coordinate_Operation Parameter Value" (parameter_code);
CREATE INDEX ix_path_concat_operation_code       ON "Coordinate_Operation Path"            (concat_operation_code);
CREATE INDEX ix_supersession_object_code         ON "Supersession"                         (object_code);


-----------------------------------------------------------------------------
-- Indexes for queries used by EPSGDataAccess.createFoo(epsgcode) methods. --
-- Indexed fields are numeric values used in ORDER BY clauses.             --
-----------------------------------------------------------------------------
CREATE INDEX ix_coordinate_axis_order            ON "Coordinate Axis"                      (coord_axis_order);
CREATE INDEX ix_coordinate_operation_accuracy    ON "Coordinate_Operation"                 (coord_op_accuracy);
CREATE INDEX ix_parameter_order                  ON "Coordinate_Operation Parameter Usage" (sort_order);
CREATE INDEX ix_path_concat_operation_step       ON "Coordinate_Operation Path"            (op_path_step);
CREATE INDEX ix_datum_member_order               ON "Datum Ensemble Member"                (datum_sequence);
CREATE INDEX ix_supersession_object_year         ON "Supersession"                         (supersession_year);
CREATE INDEX ix_version_history_date             ON "Version History"                      (version_date);


-----------------------------------------------------------------------------
-- Indexes on the object names for finding an EPSG code from a name.       --
-----------------------------------------------------------------------------
CREATE INDEX ix_name_crs            ON "Coordinate Reference System"    (coord_ref_sys_name);
CREATE INDEX ix_name_cs             ON "Coordinate System"              (coord_sys_name);
CREATE INDEX ix_name_axis           ON "Coordinate Axis Name"           (coord_axis_name);
CREATE INDEX ix_name_datum          ON "Datum"                          (datum_name);
CREATE INDEX ix_name_ellipsoid      ON "Ellipsoid"                      (ellipsoid_name);
CREATE INDEX ix_name_prime_meridian ON "Prime Meridian"                 (prime_meridian_name);
CREATE INDEX ix_name_coord_op       ON "Coordinate_Operation"           (coord_op_name);
CREATE INDEX ix_name_method         ON "Coordinate_Operation Method"    (coord_op_method_name);
CREATE INDEX ix_name_parameter      ON "Coordinate_Operation Parameter" (parameter_name);
CREATE INDEX ix_name_unit           ON "Unit of Measure"                (unit_of_meas_name);
CREATE INDEX ix_alias               ON "Alias"                          (object_table_name, alias);


-----------------------------------------------------------------------------
-- Indexes used by EPSGDataAccess.Finder for reverse operation.            --
-----------------------------------------------------------------------------
CREATE INDEX ix_major_ellipsoid ON "Ellipsoid"                   (semi_major_axis);
CREATE INDEX ix_geogcrs_crs     ON "Coordinate Reference System" (base_crs_code);
CREATE INDEX ix_ellipsoid_datum ON "Datum"                       (ellipsoid_code);


-----------------------------------------------------------------------------
-- Grants read permission to all tables in the EPSG schema.                --
-----------------------------------------------------------------------------
GRANT SELECT ON TABLE "Alias" TO PUBLIC;
GRANT SELECT ON TABLE "Conventional RS" TO PUBLIC;
GRANT SELECT ON TABLE "Coordinate Axis" TO PUBLIC;
GRANT SELECT ON TABLE "Coordinate Axis Name" TO PUBLIC;
GRANT SELECT ON TABLE "Coordinate Reference System" TO PUBLIC;
GRANT SELECT ON TABLE "Coordinate System" TO PUBLIC;
GRANT SELECT ON TABLE "Coordinate_Operation" TO PUBLIC;
GRANT SELECT ON TABLE "Coordinate_Operation Method" TO PUBLIC;
GRANT SELECT ON TABLE "Coordinate_Operation Parameter" TO PUBLIC;
GRANT SELECT ON TABLE "Coordinate_Operation Parameter Usage" TO PUBLIC;
GRANT SELECT ON TABLE "Coordinate_Operation Parameter Value" TO PUBLIC;
GRANT SELECT ON TABLE "Coordinate_Operation Path" TO PUBLIC;
GRANT SELECT ON TABLE "Datum" TO PUBLIC;
GRANT SELECT ON TABLE "Datum Ensemble" TO PUBLIC;
GRANT SELECT ON TABLE "Datum Ensemble Member" TO PUBLIC;
GRANT SELECT ON TABLE "Datum Realization Method" TO PUBLIC;
GRANT SELECT ON TABLE "Defining Operation" TO PUBLIC;
GRANT SELECT ON TABLE "Deprecation" TO PUBLIC;
GRANT SELECT ON TABLE "Ellipsoid" TO PUBLIC;
GRANT SELECT ON TABLE "Extent" TO PUBLIC;
GRANT SELECT ON TABLE "Naming System" TO PUBLIC;
GRANT SELECT ON TABLE "Prime Meridian" TO PUBLIC;
GRANT SELECT ON TABLE "Scope" TO PUBLIC;
GRANT SELECT ON TABLE "Supersession" TO PUBLIC;
GRANT SELECT ON TABLE "Unit of Measure" TO PUBLIC;
GRANT SELECT ON TABLE "Usage" TO PUBLIC;
GRANT SELECT ON TABLE "Version History" TO PUBLIC;
