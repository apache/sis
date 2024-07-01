/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.geopackage.conformance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

/**
 * JUnit tests transposed from :
 * https://www.geopackage.org/spec121/index.html#abstract_test_suite
 *
 * @author Johann Sorel (Geomatys)
 */
public class OptFeaturesConformanceTests {

    /**
     * Contents Table Feature Row
     *
     * Test Case ID : /opt/features/contents/data/features_row
     * Test Purpose : Verify that the gpkg_contents table_name value table exists, and is apparently a feature table for every row with a data_type column value of "features"
     * Test Method :
     *      1. Execute test /opt/features/vector_features/data/feature_table_integer_primary_key
     * Reference : Clause 2.1.2.1.1 Req 18
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__contents__data__features_row() {
    }

    /**
     * BLOB Format
     *
     * Test Case ID : /opt/features/geometry_encoding/data/blob
     * Test Purpose : Verify that geometries stored in feature table geometry columns are encoded in the StandardGeoPackageBinary format.
     * Test Method :
     *      1. SELECT table_name AS tn, column_name AS cn FROM gpkg_geometry_columns WHERE table_name IN (SELECT table_name FROM gpkg_contents WHERE data_type = 'features')
     *      2. Not testable if returns an empty result set
     *      3. For each row from step 1
     *          a. SELECT cn FROM tn
     *          b. Not testable if none found
     *          c. For each cn value from step a
     *              i. Fail if the first two bytes of each gc are not "GP"
     *              ii. Fail if gc.version_number is not 0
     *              iii. Fail if gc.flags.GeopackageBinary type != 0
     *              iv. Fail if cn.flags.E is 5-7
     *              v. *Fail if the geometry is empty but the envelope is not empty (gc.flags.envelope != 0 and envelope values are not NaN)
     *      4. Pass if no fails
     * Reference : Clause 2.1.3.1.1 Req 19
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__geometry_encoding__data__blob() {
    }

    /**
     * Core Types
     *
     * Test Case ID : /opt/features/geometry_encoding/data/core_types_existing_sparse_data
     * Test Purpose : Verify that existing basic simple feature geometries are stored in valid GeoPackageBinary format encodings.
     * Test Method :
     *      1. SELECT table_name FROM gpkg_geometry_columns
     *      2. Not testable if returns an empty result set
     *      3. SELECT table_name AS tn, column_name AS cn FROM gpkg_geometry_columns WHERE table_name IN (SELECT table_name FROM gpkg_contents WHERE data_type = 'features'),
     *      4. Fail if returns an empty result set
     *      5. For each row from step 3
     *          a. SELECT cn FROM tn;
     *          b. For each row from step a, if bytes 2-5 of cn.wkb as uint32 in endianness of gc.wkb byte 1of cn from #1 are a geometry type value from Annex G Table 42, then
     *              i. Log cn.header values, wkb endianness and geometry type
     *              ii. *If cn.wkb is not correctly encoded per ISO 13249-3 clause 5.1.46 then log fail
     *              iii. Otherwise log pass
     *      6. Pass if log contanins pass and no fails
     * Reference : Clause 2.1.4.1.1 Req 20
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__geometry_encoding__data__core_types_existing_sparse_data() {
    }

    /**
     * Table Definition
     *
     * Test Case ID : /opt/features/geometry_columns/data/table_def
     * Test Purpose : Verify that the gpkg_geometry_columns table exists and has the correct definition.
     * Test Method :
     *      1. PRAGMA table_info('gpkg_geometry_columns')
     *      2. Fail if returns an empty result set.
     *      3. Fail if the columns described in Geometry Columns Table Definition are missing or have non-matching definitions. Column order and other column definitions in the returned sql are irrelevant. Primary key constraints are as per gpkg_geometry_columns Table Definition SQL.
     *      4. Pass otherwise.
     * Reference : Clause 2.1.5.1.1 Req 21
     * Test Type : Basic
     */
    @Test
    @Disabled
    public void __opt__features__geometry_columns__data__table_def() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /opt/features/geometry_columns/data/data_values_geometry_columns
     * Test Purpose : Verify that gpkg_geometry_columns contains one row record for each geometry column in each vector feature user data table.
     * Test Method :
     *      1. SELECT table_name FROM gpkg_contents WHERE data_type = \'features'
     *      2. Not testable if returns an empty result set
     *      3. SELECT table_name FROM gpkg_contents WHERE data_type = \'features' AND table_name NOT IN (SELECT table_name FROM gpkg_geometry_columns)
     *      4. Fail if result set is not empty
     * Reference : Clause 2.1.5.1.2 Req 22
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__geometry_columns__data__data_values_geometry_columns() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /opt/features/geometry_columns/data/data_values_table_name
     * Test Purpose : Verify that the table_name column values in the gpkg_geometry_columns table are valid.
     * Test Method :
     *      1. PRAGMA foreign_key_list('gpkg_geometry_columns');
     *      2. Fail if there is no row designating table_name as a foreign key to table_name in gpkg_contents
     * Reference : Clause 2.1.5.1.2 Req 23
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__geometry_columns__data__data_values_table_name() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /opt/features/geometry_columns/data/data_values_column_name
     * Test Purpose : Verify that the column_name column values in the gpkg_geometry_columns table are valid.
     * Test Method :
     *      1. SELECT table_name, column_name FROM gpkg_geometry_columns
     *      2. Not testable if returns an empty result set
     *      3. For each row from step 1
     *          a. PRAGMA table_info(table_name)
     *          b. Fail if gpkg_geometry_columns.column_name value does not equal a name column value returned by PRAGMA table_info.
     *      4. Pass if no fails.
     * Reference : Clause 2.1.5.1.2 Req 24
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__geometry_columns__data__data_values_column_name() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /opt/features/geometry_columns/data/data_values_geometry_type_name
     * Test Purpose : Verify that the geometry_type_name column values in the gpkg_geometry_columns table are valid.
     * Test Method :
     *      1. SELECT DISTINCT geometry_type_name from gpkg_geometry_columns
     *      2. Not testable if returns an empty result set
     *      3. For each row from step 1
     *          a. Fail if a returned geometry_type_name value is not in Table 28 in Annex G
     *      4. Pass if no fails.
     * Reference : Clause 2.1.5.1.2 Req 25
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__geometry_columns__data__data_values_geometry_type_name() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /opt/features/geometry_columns/data/data_values_srs_id
     * Test Purpose : Verify that the gpkg_geometry_columns table srs_id column values are valid.
     * Test Method :
     *      1. PRAGMA foreign_key_check('gpkg_geometry_columns')
     *      2. Fail if returns any rows with a fourth column foreign key index value of 0
     * Reference : Clause 2.1.5.1.2 Req 26
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__geometry_columns__data__data_values_srs_id() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /opt/features/geometry_columns/data/data_values_z
     * Test Purpose : Verify that the gpkg_geometry_columns table z column values are valid.
     * Test Method :
     *      1. SELECT z FROM gpkg_geometry_columns
     *      2. Not testable if returns an empty result set
     *      3. SELECT z FROM gpkg_geometry_columns WHERE z NOT IN (0,1,2)
     *      4. Fail if does not return an empty result set
     *      5. Pass otherwise.
     * Reference : Clause 2.1.5.1.2 Req 27
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__geometry_columns__data__data_values_z() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /opt/features/geometry_columns/data/data_values_m
     * Test Purpose : Verify that the gpkg_geometry_columns table m column values are valid.
     * Test Method :
     *      1. SELECT m FROM gpkg_geometry_columns
     *      2. Not testable if returns an empty result set
     *      3. SELECT m FROM gpkg_geometry_columns WHERE m NOT IN (0,1,2)
     *      4. Fail if does not return an empty result set
     *      5. Pass otherwise.
     * Reference : Clause 2.1.5.1.2 Req 28
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__geometry_columns__data__data_values_m() {
    }

    /**
     * Table Definition
     *
     * Test Case ID : /opt/features/vector_features/data/feature_table_integer_primary_key
     * Test Purpose : Verify that every vector features user data table has an integer primary key.
     * Test Method :
     *      1. SELECT table_name FROM gpkg_contents WHERE data_type = 'features'
     *      2. Not testable if returns an empty result set
     *      3. For each row from step 1
     *          a. PRAGMA table_info(table_name)
     *          b. Fail if returns an empty result set
     *          c. Fail if result set does not contain one row where the pk column value is 1 and the not null column value is 1 and the type column value is "INTEGER"
     *          d. SELECT COUNT(*) - COUNT(DISTINCT id) from table_name
     *          e. Fail if result is nonzero
     *      4. Pass if no fails.
     * Reference : Clause 2.1.6.1.1 Req 29
     * Test Type : Basic
     */
    @Test
    @Disabled
    public void __opt__features__vector_features__data__feature_table_integer_primary_key() {
    }

    /**
     * Table Definition
     *
     * Test Case ID : /opt/features/vector_features/data/feature_table_one_geometry_column
     * Test Purpose : Verify that every vector features user data table has one geometry column.
     * Test Method :
     *      1. SELECT table_name FROM gpkg_contents WERE data_type = 'features'
     *      2. Not testable if returns an empty result set
     *      3. For each row table name from step 1
     *          a. SELECT column_name from gpkg_geometry_columns where table_name = row table name
     *          b. Fail if returns more than one column name
     *      4. Pass if no fails
     * Reference : Clause 2.1.6.1.1 Req 30
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__vector_features__data__feature_table_one_geometry_column() {
    }

    /**
     * Table Definition
     *
     * Test Case ID : /opt/features/vector_features/data/feature_table_geometry_column_type
     * Test Purpose : Verify that the declared SQL type of a feature table geometry column is the uppercase geometry type name from Annex G specified by the geometry_type_name column for that column_name and table_name in the gpkg_geometry_columns table.
     * Test Method :
     *      1. SELECT table_name, column_name, geometry_type_name table_name FROM gpkg_geometry_columns WHERE table_name IN (SELECT table_name FROM gpkg_contents WHERE data_type = 'features')
     *      2. For each row selected in (1):
     *          a. PRAGMA table_info('{selected table_name}')
     *          b. Fail if declared type of column_name selected in (1) is not the geometry_type_name selected in (1)
     *      3. Pass if no fails
     * Reference : Clause 2.1.6.1.1 Req 31
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__vector_features__data__feature_table_geometry_column_type() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /opt/features/vector_features/data/data_values_geometry_type
     * Test Purpose : Verify that the geometry type of feature geometries are of the type or are assignable for the geometry type specified by the gpkg_geometry columns table geometry_type_name column value.
     * Test Method :
     *      1. SELECT table_name AS tn, column_name AS cn, geometry_type_name AS gt_name FROM gpkg_geometry_columns WHERE table_name IN (SELECT table_name FROM gpkg_contents WHERE data_type = 'features')
     *      2. Not testable if returns an empty result set
     *      3. For each row from step 1
     *          a. *Select the set of geometry types in use for the values in cn
     *          b. For each row actual_type_name from step a
     *              i. *Determine if each geometry type is assignable to the actual_type_name
     *              ii. Fail if any are not assignable
     *      4. Pass if no fails
     * Reference : Clause 2.1.6.1.2 Req 32
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__vector_features__data__data_values_geometry_type() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /opt/features/vector_features/data/data_value_geometry_srs_id
     * Test Purpose : Verify the the srs_id of feature geometries are the srs_id specified for the gpkg_geometry_columns table srs_id column value.
     * Test Method :
     *      1. SELECT table_name AS tn, column_name AS cn, srs_id AS gc_srs_id FROM gpkg_geometry_columns WHERE table_name IN (SELECT table_name FROM gpkg_contents where data_type = 'features')
     *      2. Not testable if returns an empty result set
     *      3. For each row from step 1
     *          a. *Select the set of SRIDs in use for the values in cn
     *          b. For each row from step a
     *              i. *Fail if any SRID is not equal to gc_srs_id
     *      4. Pass if no fails
     * Reference : Clause 2.1.6.1.2 Req 33
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__features__vector_features__data__data_value_geometry_srs_id() {
    }

}
