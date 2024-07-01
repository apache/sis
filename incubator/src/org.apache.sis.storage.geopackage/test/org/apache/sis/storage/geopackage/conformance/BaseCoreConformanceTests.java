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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.geopackage.GpkgStore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;

/**
 * JUnit tests transposed from :
 * https://www.geopackage.org/spec121/index.html#abstract_test_suite
 *
 * @author Johann Sorel (Geomatys)
 */
public class BaseCoreConformanceTests {

    /**
     * File Format
     *
     * Test Case ID : /base/core/container/data/file_format
     * Test Purpose : Verify that the GeoPackage is an SQLite version_3 database
     * Test Method : Pass if the first 16 bytes of the file contain "SQLite format 3" in ASCII.
     * Reference : Clause 1.1.1.1.1 Req 1
     * Test Type : Basic
     */
    @Test
    public void __base__core__container__data__file_format() throws IOException, DataStoreException {
        final Path file = Files.createTempFile("database", ".gpkg");
        Files.deleteIfExists(file);
        try {
            final GpkgStore store = new GpkgStore(file);
            store.close();

            try (InputStream stream = Files.newInputStream(file)) {
                final DataInputStream ds = new DataInputStream(stream);
                final byte[] signature = new byte[16];
                ds.readFully(signature);
                assertTrue(Arrays.equals(new byte[]{'S','Q','L','i','t','e',' ','f','o','r','m','a','t',' ','3',0x00}, signature));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /**
     * File Format
     *
     * Test Case ID : /base/core/container/data/file_format/application_id
     * Test Purpose : Verify that the SQLite database header application id field indicates GeoPackage version 1.0
     * Test Method :
     *      1. Retrieve the bytes at the application id of the SQLite database header
     *      2. If the string is "GP10" in ASCII, fall back to the tests for GeoPackage 1.0.
     *      3. If the string is "GP11" in ASCII, fall back to the tests for GeoPackage 1.1.
     *      4. If the string is "GPKG" in ASCII
     *          a. PRAGMA user_version
     *          b. Fail if the integer representation of the user_version string is less than 10200.
     *      5. Fail if the string is not one of those values     *
     * Reference : Clause Clause 1.1.1.1.1 Req 2
     * Test Type : Basic
     */
    @Test
    public void __base__core__container__data__file_format__application_id() throws DataStoreException, IOException {
        final Path file = Files.createTempFile("database", ".gpkg");
        Files.deleteIfExists(file);
        try {
            final GpkgStore store = new GpkgStore(file);
            store.close();

            try (InputStream stream = Files.newInputStream(file)) {
                final DataInputStream ds = new DataInputStream(stream);
                final byte[] array = new byte[68+4];
                ds.readFully(array);
                assertTrue(Arrays.equals(new byte[]{'G','P','K','G'}, Arrays.copyOfRange(array, 68, 72)));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /**
     * File Extension Name
     *
     * Test Case ID : /base/core/container/data/file_extension_name
     * Test Purpose : Verify that the GeoPackage extension is ".gpkg"
     * Test Method : Pass if the GeoPackage file extension is ".gpkg"
     * Reference : Clause 1.1.1.1.2 Req 3
     * Test Type : Basic
     */
    @Test
    public void __base__core__container__data__file_extension_name() throws IOException, DataStoreException {
        final Path file = Files.createTempFile("database", ".gpkg");
        Files.deleteIfExists(file);
        try {
            final GpkgStore store = new GpkgStore(file);
            store.close();
            assertTrue(file.getFileName().toString().endsWith(".gpkg"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /**
     * File Contents
     *
     * Test Case ID : /base/core/container/data/file_contents
     * Test Purpose : Verify that the GeoPackage only contains specified contents
     * Test Method :
     *      1. SELECT COUNT(*) from gpkg_extensions;
     *      2. Not testable if table exists and count > 0
     *      3. For each gpkg_* table_name
     *          a. PRAGMA table_info(table_name)
     *          b. Continue if returns an empty result set
     *          c. Fail if column definitions returned by "PRAGMA table_info" do not match column definitions for the table in Annex C.
     *      4. Pass if no fails
     * Reference : Clause 1.1.1.1.3 Req 4
     * Test Type : Basic
     */
    @Test
    @Disabled
    public void __base__core__container__data__file_contents() throws DataStoreException, IOException, SQLException {
        final Path file = Files.createTempFile("database", ".gpkg");
        Files.deleteIfExists(file);
        try (final GpkgStore store = new GpkgStore(file);
             Connection cnx = store.getDataSource().getConnection();) {
            Statement stmt = cnx.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) from gpkg_extensions;");
            rs.next();
            int cnt = rs.getInt(1);
            assertTrue(cnt == 0);

            //TODO check each table fields, must match SQL definition
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /**
     * File Contents
     *
     * Test Case ID : /base/core/container/data/table_data_types
     * Test Purpose : Verify that the data types of GeoPackage columns include only the types specified by GeoPackage Data Types.
     * Test Method :
     *      1. SELECT table_name FROM gpkg_contents WHERE data_type IN ('tiles','features','attributes')
     *      2. Not testable if returns empty set
     *      3. For each row table name from step 1
     *          a. PRAGMA table_info(table_name)
     *          b. For each row type column value
     *              i. Fail if value is not one of the data type names specified by GeoPackage Data Types
     *      4. Pass if no fails
     * Reference : GeoPackage Data Types Req 5
     * Test Type : Basic
     */
    @Test
    @Disabled
    public void __base__core__container__data__table_data_types() {
        //TODO need a filled database to test this
    }

    /**
     * Integrity Check
     *
     * Test Case ID : /base/core/container/data/file_integrity
     * Test Purpose : Verify that the GeoPackage passes the SQLite integrity check.
     * Test Method : Pass if "PRAGMA integrity_check" returns "ok"
     * Reference : Clause File Integrity Req 6
     * Test Type : Capability
     */
    @Test
    public void __base__core__container__data__file_integrity() throws DataStoreException, IOException, SQLException {
        final Path file = Files.createTempFile("database", ".gpkg");
        Files.deleteIfExists(file);
        try {
            GpkgStore store = new GpkgStore(file);
            store.close();

            store = new GpkgStore(file);
            try (Connection cnx = store.getDataSource().getConnection()) {
                ResultSet rs = cnx.createStatement().executeQuery("PRAGMA integrity_check");
                rs.next();
                String result = rs.getString(1);
                assertEquals("ok", result);
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /**
     * Integrity Check
     *
     * Test Case ID : /base/core/container/data/foreign_key_integrity
     * Test Purpose : Verify that the GeoPackage passes the SQLite foreign_key_check.
     * Test Method : Pass if "PRAGMA foreign_key_check" (with no parameter value) returns an empty result set
     * Reference : Clause File Integrity Req 7
     * Test Type : Capability
     */
    @Test
    public void __base__core__container__data__foreign_key_integrity() throws IOException, DataStoreException, SQLException {
        final Path file = Files.createTempFile("database", ".gpkg");
        Files.deleteIfExists(file);
        try {
            GpkgStore store = new GpkgStore(file);
            store.close();

            store = new GpkgStore(file);
            try (Connection cnx = store.getDataSource().getConnection()) {
                ResultSet rs = cnx.createStatement().executeQuery("PRAGMA foreign_key_check");
                assertFalse(rs.next());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /**
     * Structured Query Language
     *
     * Test Case ID : /base/core/container/api/sql
     * Test Purpose : Test that the GeoPackage SQLite Extension provides the SQLite SQL API interface.
     * Test Method :
     *      1. sqlite3_exec('SELECT * FROM sqlite_master;)
     *      2. Fail if returns an SQL error.
     *      3. Pass otherwise
     * Reference : Clause 1.1.1.2.1 Req 8
     * Test Type : Capability
     */
    @Test
    public void __base__core__container__api__sql() throws IOException, DataStoreException, SQLException {
        final Path file = Files.createTempFile("database", ".gpkg");
        Files.deleteIfExists(file);
        try {
            GpkgStore store = new GpkgStore(file);
            store.close();

            store = new GpkgStore(file);
            try (Connection cnx = store.getDataSource().getConnection()) {
                ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM sqlite_master;");
            } catch (SQLException ex) {
                fail(ex.getMessage());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /**
     * Table Definition
     *
     * Test Case ID : /base/core/gpkg_spatial_ref_sys/data/table_def
     * Test Purpose : Verify that the gpkg_spatial_ref_sys table exists and has the correct definition.
     * Test Method :
     *      1. SELECT sql FROM sqlite_master WHERE type = 'table' AND tbl_name = 'gpkg_spatial_ref_sys'
     *      2. Fail if returns an empty result set
     *      3. Pass if column names and column definitions in the returned CREATE TABLE statement in the sql column value, including data type, nullability, and primary key constraints match all of those in the contents of C.1 Table 15.
     *         Column order, check constraint and trigger definitions, and other column definitions in the returned sql are irrelevant.
     *      4. Fail otherwise.
     * Reference : Clause 1.1.2.1.1 Req 10
     * Test Type : Basic
     */
    @Test
    @Disabled
    public void __base__core__gpkg_spatial_ref_sys__data__table_def() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /base/core/gpkg_spatial_ref_sys/data_values_default
     * Test Purpose : Verify that the spatial_ref_sys table contains the required default contents.
     * Test Method :
     *      1. SELECT srs_id, organization, organization_coordsys_id, description FROM gpkg_spatial_ref_sys WHERE srs_id = -1 returns -1 "NONE" -1 "undefined", AND
     *      2. SELECT srs_id, organization, organization_coordsys_id, description FROM gpkg_spatial_ref_sys WHERE srs_id = 0 returns 0 "NONE" 0 "undefined", AND
     *      3. SELECT definition FROM gpkg_spatial_ref_sys WHERE organization IN ("epsg","EPSG") AND organization_coordsys_id 4326
     *          a. Confirm that this is a valid CRS
     *      4. Pass if tests 1-3 are met
     *      5. Fail otherwise
     * Reference : Clause 1.1.2.1.2 Requirement 11
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __base__core__gpkg_spatial_ref_sys__data_values_default() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /base/core/spatial_ref_sys/data_values_required
     * Test Purpose : Verify that the spatial_ref_sys table contains rows to define all srs_id values used by features and tiles in a GeoPackage.
     * Test Method :
     *      1.SELECT DISTINCT gc.srs_id, srs.srs_id FROM gpkg_contents AS gc LEFT OUTER JOIN gpkg_spatial_ref_sys AS srs ON srs.srs_id = gc.srs_id WHERE gc.data_type IN ('tiles', 'features')
     *      2. Pass if no returned srs.srs_id values are NULL.
     *      3. Fail otherwise
     * Reference : Clause Clause 1.1.2.1.2 Req 12
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __base__core__spatial_ref_sys__data_values_required() {
    }

    /**
     * Table Definition
     *
     * Test Case ID : /base/core/contents/data/table_def
     * Test Purpose : Verify that the gpkg_contents table exists and has the correct definition.
     * Test Method :
     *      1. SELECT sql FROM sqlite_master WHERE type = 'table' AND tbl_name = 'gpkg_contents'
     *      2. Fail if returns an empty result set.
     *      3. Pass if the column names and column definitions in the returned CREATE TABLE statement, including data type, nullability, default values and primary, foreign and unique key constraints match all of those in the contents of C.2 Table gpkg_contents Table Definition SQL. Column order, check constraint and trigger definitions, and other column definitions in the returned sql are irrelevant.
     *      4. Fail Otherwise
     * Reference : Clause 1.1.3.1.1 Req 13
     * Test Type : Basic
     */
    @Test
    @Disabled
    public void __base__core__contents__data__table_def() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /base/core/contents/data/data_values_table_name
     * Test Purpose : Verify that the table_name column values in the gpkg_contents table are valid.
     * Test Method :
     *      1. SELECT DISTINCT table_name FROM gpkg_contents WHERE table_name NOT IN (SELECT name FROM sqlite_master)
     *      2. Fail if there are any results
     *      3. Pass otherwise.
     * Reference : Clause 1.1.3.1.2 Req 14
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __base__core__contents__data__data_values_table_name() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /base/core/contents/data/data_values_last_change
     * Test Purpose : Verify that the gpkg_contents table last_change column values are in ISO 8601 [29]format containing a complete date plus UTC hours, minutes, seconds and a decimal fraction of a second, with a 'Z' ("zulu") suffix indicating UTC.
     * Test Method :
     *      1. SELECT last_change from gpkg_contents.
     *      2. Not testable if returns an empty result set.
     *      3. For each row from step 1
     *          a. Fail if format of returned value does not match yyyy-mm-ddThh:mm:ss.hhhZ
     *      4. Pass if no fails.
     * Reference : Clause 1.1.3.1.2 Req 15
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __base__core__contents__data__data_values_last_change() {
    }

    /**
     * Table Data Values
     *
     * Test Case ID : /base/core/contents/data/data_values_srs_id
     * Test Purpose : Verify that the gpkg_contents table srs_id column values reference gpkg_spatial_ref_sys srs_id column values.
     * Test Method :
     *      1. PRAGMA foreign_key_check('gpkg_contents')
     *      2. Fail if does not return an empty result set
     * Reference : Clause 1.1.3.1.2 Req 16
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __base__core__contents__data__data_values_srs_id() {
    }

    /**
     * Options
     *
     * Test Case ID : /opt/valid_geopackage
     * Test Purpose : Verify that a GeoPackage contains a features or tiles table and gpkg_contents table row describing it.
     * Test Method :
     *      1. SELECT COUNT(*) FROM gpkg_contents WHERE data_type IN ('tiles', 'features')
     *      2. Pass if result > 0
     *      3. Fail otherwise
     * Reference : Clause 2 Req 17
     * Test Type : Capability
     */
    @Test
    @Disabled
    public void __opt__valid_geopackage() {
    }

}
