--
-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
-- http://www.apache.org/licenses/LICENSE-2.0
--


--
-- Corrections to some deprecated Coordinate Reference Systems (CRS).  The errors that we fix here are known to EPSG,
-- but were fixed by deprecating the erroneous CRS and creating a new one. This approach allows reproductive behavior
-- of applications that used the erroneous CRS. However in a few cases, Apache SIS can not instantiate the deprecated
-- object without some minimal corrections. The following UPDATEs perform the minimal amount of changes needed by SIS.
--

-- "Scale factor at natural origin" (EPSG:8805) shall be dimensionless (EPSG:9201), not in "m" units (EPSG:9001).
UPDATE epsg_coordoperationparamvalue SET uom_code = 9201 WHERE parameter_code = 8805 AND uom_code = 9001;

-- Value 44°87′ is illegal for DMS units (EPSG:9110). A value equivalent to the erroneous DMS value is 45°27′.
UPDATE epsg_coordoperationparamvalue SET parameter_value = 45.27 WHERE parameter_value = 44.87 AND uom_code = 9110;



---
--- Extensions to EPSG dataset 8.9 for helping Apache SIS to find some coordinate operation paths.
---
---     NTF Paris (EPSG:4807)  →  NTF (EPSG:4275)  →  RGF93 (EPSG:4171)
---
INSERT INTO epsg_coordoperation VALUES (48094, 'NTF (Paris) to RGF93 (1)', 'concatenated operation',
 4807, 4171, NULL, 1, 3694, '?', 1, NULL, NULL, NULL, NULL, NULL, 'Apache SIS', '2016-04-22', NULL, FALSE, FALSE);
INSERT INTO epsg_coordoperationpath VALUES
 (48094, 1763, 1),
 (48094, 1053, 2);



--
-- Additional indexes for the EPSG database. Those indexes are not declared
-- in the SQL scripts distributed by EPSG. They are not required for proper
-- working of the EPSG factory, but can significantly improve performances.
--

-----------------------------------------------------------------------------
-- Indexes for queries used by EPSGDataAccess.createFoo(epsgcode) methods. --
-- Indexed fields are numeric values used mainly in equality comparisons.  --
-----------------------------------------------------------------------------
CREATE INDEX ix_alias_object_code                ON epsg_alias                     (object_code);
CREATE INDEX ix_crs_datum_code                   ON epsg_coordinatereferencesystem (datum_code);
CREATE INDEX ix_crs_projection_code              ON epsg_coordinatereferencesystem (projection_conv_code);
CREATE INDEX ix_coordinate_axis_code             ON epsg_coordinateaxis            (coord_axis_code);
CREATE INDEX ix_coordinate_axis_sys_code         ON epsg_coordinateaxis            (coord_sys_code);
CREATE INDEX ix_coordinate_operation_crs         ON epsg_coordoperation            (source_crs_code, target_crs_code);
CREATE INDEX ix_coordinate_operation_method_code ON epsg_coordoperation            (coord_op_method_code);
CREATE INDEX ix_parameter_usage_method_code      ON epsg_coordoperationparamusage  (coord_op_method_code);
CREATE INDEX ix_parameter_values                 ON epsg_coordoperationparamvalue  (coord_op_code, coord_op_method_code);
CREATE INDEX ix_parameter_value_code             ON epsg_coordoperationparamvalue  (parameter_code);
CREATE INDEX ix_path_concat_operation_code       ON epsg_coordoperationpath        (concat_operation_code);
CREATE INDEX ix_supersession_object_code         ON epsg_supersession              (object_code);


-----------------------------------------------------------------------------
-- Indexes for queries used by EPSGDataAccess.createFoo(epsgcode) methods. --
-- Indexed fields are numeric values used in ORDER BY clauses.             --
-----------------------------------------------------------------------------
CREATE INDEX ix_coordinate_axis_order            ON epsg_coordinateaxis           (coord_axis_order);
CREATE INDEX ix_coordinate_operation_accuracy    ON epsg_coordoperation           (coord_op_accuracy);
CREATE INDEX ix_parameter_order                  ON epsg_coordoperationparamusage (sort_order);
CREATE INDEX ix_path_concat_operation_step       ON epsg_coordoperationpath       (op_path_step);
CREATE INDEX ix_supersession_object_year         ON epsg_supersession             (supersession_year);
CREATE INDEX ix_version_history_date             ON epsg_versionhistory           (version_date);


-----------------------------------------------------------------------------
-- Indexes on the object names for finding an EPSG code from a name.       --
-----------------------------------------------------------------------------
CREATE INDEX ix_name_crs            ON epsg_coordinatereferencesystem (coord_ref_sys_name);
CREATE INDEX ix_name_cs             ON epsg_coordinatesystem          (coord_sys_name);
CREATE INDEX ix_name_axis           ON epsg_coordinateaxisname        (coord_axis_name);
CREATE INDEX ix_name_datum          ON epsg_datum                     (datum_name);
CREATE INDEX ix_name_ellipsoid      ON epsg_ellipsoid                 (ellipsoid_name);
CREATE INDEX ix_name_prime_meridian ON epsg_primemeridian             (prime_meridian_name);
CREATE INDEX ix_name_coord_op       ON epsg_coordoperation            (coord_op_name);
CREATE INDEX ix_name_method         ON epsg_coordoperationmethod      (coord_op_method_name);
CREATE INDEX ix_name_parameter      ON epsg_coordoperationparam       (parameter_name);
CREATE INDEX ix_name_unit           ON epsg_unitofmeasure             (unit_of_meas_name);


-----------------------------------------------------------------------------
-- Indexes used by EPSGDataAccess.Finder for reverse operation.            --
-----------------------------------------------------------------------------
CREATE INDEX ix_major_ellipsoid ON epsg_ellipsoid                 (semi_major_axis);
CREATE INDEX ix_geogcrs_crs     ON epsg_coordinatereferencesystem (source_geogcrs_code);
CREATE INDEX ix_ellipsoid_datum ON epsg_datum                     (ellipsoid_code);


-----------------------------------------------------------------------------
-- Grants read permission to all tables in the EPSG schema.                --
-----------------------------------------------------------------------------
GRANT SELECT ON TABLE epsg_alias TO PUBLIC;
GRANT SELECT ON TABLE epsg_area TO PUBLIC;
GRANT SELECT ON TABLE epsg_coordinateaxis TO PUBLIC;
GRANT SELECT ON TABLE epsg_coordinateaxisname TO PUBLIC;
GRANT SELECT ON TABLE epsg_coordoperation TO PUBLIC;
GRANT SELECT ON TABLE epsg_coordoperationmethod TO PUBLIC;
GRANT SELECT ON TABLE epsg_coordoperationparam TO PUBLIC;
GRANT SELECT ON TABLE epsg_coordoperationparamusage TO PUBLIC;
GRANT SELECT ON TABLE epsg_coordoperationparamvalue TO PUBLIC;
GRANT SELECT ON TABLE epsg_coordoperationpath TO PUBLIC;
GRANT SELECT ON TABLE epsg_coordinatereferencesystem TO PUBLIC;
GRANT SELECT ON TABLE epsg_coordinatesystem TO PUBLIC;
GRANT SELECT ON TABLE epsg_datum TO PUBLIC;
GRANT SELECT ON TABLE epsg_ellipsoid TO PUBLIC;
GRANT SELECT ON TABLE epsg_namingsystem TO PUBLIC;
GRANT SELECT ON TABLE epsg_primemeridian TO PUBLIC;
GRANT SELECT ON TABLE epsg_supersession TO PUBLIC;
GRANT SELECT ON TABLE epsg_unitofmeasure TO PUBLIC;
GRANT SELECT ON TABLE epsg_versionhistory TO PUBLIC;
GRANT SELECT ON TABLE epsg_change TO PUBLIC;
GRANT SELECT ON TABLE epsg_deprecation TO PUBLIC;
