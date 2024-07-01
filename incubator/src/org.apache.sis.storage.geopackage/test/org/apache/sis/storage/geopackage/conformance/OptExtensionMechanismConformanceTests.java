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
public class OptExtensionMechanismConformanceTests {

    /**
     * Test Case ID : /opt/extension_mechanism/data/table_def
     * Test Purpose : Verify that a gpkg_extensions table exists and has the correct definition.
     * Test Method :
     *      1. SELECT sql FROM sqlite_master WHERE type = 'table' AND tbl_name = 'gpkg_extensions'
     *      2. Fail if returns an empty result set.
     *      3. Pass if the column names and column definitions in the returned Create TABLE statement in the sql column value, including data type, nullability, default values and primary, foreign and unique key constraints match all of those in the contents of Table Definition. Column order, check constraint and trigger definitions, and other column definitions in the returned sql are irrelevant.
     *      4. Fail otherwise.
     * Reference : Clause 2.3.2.1.1 Req 58
     * Test Type : Basic
     */
    @Test
    @Disabled
    public void __opt__extension_mechanism__data__table_def() {
    }

    /**
     * Test Case ID : /opt/extension_mechanism/data/data_values_for_extensions
     * Test Purpose : Verify that every extension of a GeoPackage is registered in a row in the gpkg_extensions table
     * Test Method :
     *      1. Manual inspection
     * Reference : Clause 2.3.2.1.2 Req 59
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__extension_mechanism__data__data_values_for_extensions() {
    }

    /**
     * Test Case ID : /opt/extension_mechanism/data/data_values_table_name
     * Test Purpose : Verify that the table_name column values in the gpkg_extensions table are valid.
     * Test Method :
     *      1. SELECT lower(table_name) AS table_name, column_name FROM gpkg_extensions;
     *      2. Not testable if table does not exist or query returns an empty result set.
     *      3. For each row from step one
     *          a. "SELECT DISTINCT lower(ge.table_name) AS ge_table, lower(sm.tbl_name) AS tbl_name FROM gpkg_extensions AS ge LEFT OUTER JOIN sqlite_master AS sm ON lower(ge.table_name) = lower(sm.tbl_name);
     *          b. Fail if ge_table and tbl_name are not equal (or both null).
     *      4. Pass if no fails.
     * Reference : Clause 2.3.2.1.2 Req 60
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__extension_mechanism__data__data_values_table_name() {
    }

    /**
     * Test Case ID : /opt/extension_mechanism/data/data_values_column_name
     * Test Purpose : Verify that the column_name column values in the gpkg_extensions table are valid.
     * Test Method :
     *      1. SELECT table_name, column_name FROM gpkg_extensions WHERE column_name IS NOT NULL
     *      2. Pass if returns an empty result set
     *      3. For each row from step 3
     *          a. SELECT count(column_name) FROM table_name
     *              i. Fail if query is invalid, suggesting an invalid column name
     *          b. Log pass otherwise
     *      4. Pass if logged pass and no fails.
     * Reference : Clause 2.3.2.1.2 Req 61
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__extension_mechanism__data__data_values_column_name() {
    }

    /**
     * Test Case ID : /opt/extension_mechanism/data/data_values_extension_name
     * Test Purpose : Verify that the extension_name column values in the gpkg_extensions table are valid.
     * Test Method :
     *      1. SELECT extension_name FROM gpkg_extensions
     *      2. Not testable if returns an empty result set
     *      3. For each row returned from step 1
     *          a. Log pass if extension_name is one of those listed in Annex F.
     *          b. Separate extension_name into <author> and <extension> at the first "_"
     *          c. Fail if <author> is "gpkg"
     *          d. Fail if <author> contains characters other than [a-zA-Z0-9]
     *          e. Fail if <extension> contains characters other than [a-zA-Z0-9_]
     *          f. Log pass otherwise
     *      4. Pass if logged pass and no fails.
     * Reference : Clause 2.3.2.1.2 Req 62
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__extension_mechanism__data__data_values_extension_name() {
    }

    /**
     * Test Case ID : /opt/extension_mechanism/data/data_values_definition
     * Test Purpose : Verify that the definition column value contains or references extension documentation
     * Test Method :
     *      1. SELECT definition FROM gpkg_extensions
     *      2. Not testable if returns an empty result set
     *      3. For each row returned from step 1
     *          a. Inspect if definition value is not like "Annex %", or "http%" or mailto:% or "Extension Title%"
     *          b. Fail if definition value does not contain or reference extension documentation
     *      4. Pass if no fails
     * Reference : Clause 2.3.2.1.2 Req 63
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__extension_mechanism__data__data_values_definition() {
    }

    /**
     * Test Case ID : /opt/extension_mechanism/data/data_values_scope
     * Test Purpose : Verify that the scope column value is "read-write" or "write-only"
     * Test Method :
     *      1. SELECT scope FROM gpkg_extensions
     *      2. Not testable if returns an empty result set
     *      3. For each row returned from step 1
     *          a. Fail if value is not "read-write" or "write-only"
     *      4. Pass if no fails
     * Reference : Clause 2.3.2.1.2 Req 64
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__extension_mechanism__data__data_values_scope() {
    }

    /**
     * Test Case ID : /opt/attributes/contents/data/attributes_row
     * Test Purpose : Verify that the gpkg_contents table_name value table exists and is apparently an attributes table for every row with a data_type column value of "attributes".
     * Test Method :
    SELECT table_name FROM gpkg_contents WHERE data_type = "attributes"
    Not testable if returns empty result set
    For each row from step 1
        PRAGMA table_info(table_name)
        Fail if returns an empty result set
        Fail if result set does not contain one row where the pk column value is 1 and the notnull column value is 1 and the type column value is "INTEGER" and the name column value is "id"
    Pass if no fails.
     * Reference : Clause 2.4.2.1.1 Req 64a, Clause 2.4.3.1.1 Req 64b
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__attributes__contents__data__attributes_row() {
    }

}
