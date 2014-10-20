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
package org.apache.sis.internal.shapefile.jdbc;

import static org.junit.Assert.*;

import java.io.*;
import java.sql.*;
import java.text.*;

import org.junit.*;
import org.opengis.test.*;

/**
 * ResultSet test.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class ResultSetTest extends TestCase {
	/** Database file. */
	private File dbfFile;

	/**
	 * Read the first record.
	 * @throws SQLException if an SQL error occurs.
	 */
	@Test public void readFirstRecord() throws SQLException {
		Driver driver = new DBFDriver();

		Connection connection = driver.connect(dbfFile.getAbsolutePath(), null);
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("Dummy"); // We don't care currently of the request : we are returning everything each time.

		rs.next();
		assertEquals("The record has not the expected field value.", "336TH ST", rs.getString("ST_NAME"));

		rs.close();
		stmt.close();
		connection.close();
	}

	/**
	 * Read all the DBF records.
	 * @throws SQLException if an SQL error occurs.
	 */
	@Test public void readAllRecords() throws SQLException {
		Driver driver = new DBFDriver();

		Connection connection = driver.connect(dbfFile.getAbsolutePath(), null);
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("Dummy"); // We don't care currently of the request : we are returning everything each time.

		int count = 0;

		while(rs.next())
			count ++;

		assertTrue("Less than one record was readed.", count > 1);

		rs.close();
		stmt.close();
		connection.close();
	}

	/**
	 * Test setup.
	 */
	@Before public void setup() {
		dbfFile = new File("src/test/resources/org/apache/sis/storage/shapefile/SignedBikeRoute_4326_clipped.dbf");
		assertTrue(MessageFormat.format("The database file ''{0}'' used for testing doesn''t exist." , dbfFile.getAbsolutePath()), dbfFile.exists());
	}

}
