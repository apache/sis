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
package org.apache.sis.metadata.sql.privy;

import java.sql.Connection;
import java.sql.SQLException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestStep;
import org.apache.sis.metadata.sql.TestDatabase;


/**
 * Tests {@link ScriptRunner}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ScriptRunnerTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ScriptRunnerTest() {
    }

    /**
     * Tests {@link ScriptRunner} with an in-memory Derby database.
     * This method delegates its work to all other methods in this class that expect a {@link ScriptRunner} argument.
     *
     * @throws SQLException if an error occurred while executing the script runner.
     */
    @Test
    public void testOnDerby() throws SQLException {
        try (TestDatabase db = TestDatabase.create("ScriptRunner");
             Connection c = db.source.getConnection())
        {
            final ScriptRunner sr = new ScriptRunner(c, null, 3);
            testSupportedFlags(sr);
            testRegularExpressions(sr);
        }
    }

    /**
     * Verifies the values of {@code is*Supported} flags in the given script runner.
     *
     * @param  sr  the script runner for which to verify flag values.
     */
    @TestStep
    public static void testSupportedFlags(final ScriptRunner sr) {
        assertFalse(sr.isEnumTypeSupported);
    }

    /**
     * Verifies the regular expressions used by the script runner.
     * This method tests the values returned by {@link ScriptRunner#isSupported(CharSequence)}
     *
     * @param  sr  the script runner to use for testing regular expressions.
     */
    @TestStep
    public static void testRegularExpressions(final ScriptRunner sr) {
        assertFalse(sr.isSupported("CREATE TYPE CI_DateTypeCode AS ENUM ('creation', 'publication')"));
        assertFalse(sr.isSupported("CREATE CAST (VARCHAR AS CI_DateTypeCode) WITH INOUT AS ASSIGNMENT"));
        assertTrue (sr.isSupported("CREATE TABLE CI_Citation (…)"));
        assertFalse(sr.isSupported("GRANT USAGE ON SCHEMA metadata TO PUBLIC"));
        assertFalse(sr.isSupported("GRANT SELECT ON TABLE \"Coordinate Reference System\" TO PUBLIC"));
        assertFalse(sr.isSupported("COMMENT ON SCHEMA metadata IS 'ISO 19115 metadata'"));
    }
}
