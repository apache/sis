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
package org.apache.sis.internal.metadata.sql;

import java.sql.Connection;
import javax.sql.DataSource;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestStep;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ScriptRunner}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class ScriptRunnerTest extends TestCase {
    /**
     * Tests {@link ScriptRunner} with an in-memory Derby database.
     * This method delegates its work to all other methods in this class that expect a {@link ScriptRunner} argument.
     *
     * @throws Exception if an error occurred while executing the script runner.
     */
    @Test
    public void testOnDerby() throws Exception {
        final DataSource ds = TestDatabase.create("ScriptRunner");
        try (Connection c = ds.getConnection()) {
            final ScriptRunner sr = new ScriptRunner(c, 3);
            testSupportedFlags(sr);
            testRegularExpressions(sr);
        } finally {
            TestDatabase.drop(ds);
        }
    }

    /**
     * Verifies the values of {@code is*Supported} flags in the given script runner.
     *
     * @param  sr  the script runner for which to verify flag values.
     */
    @TestStep
    public static void testSupportedFlags(final ScriptRunner sr) {
        assertFalse("isCatalogSupported",       sr.isCatalogSupported);
        assertTrue ("isSchemaSupported",        sr.isSchemaSupported);
        assertFalse("isGrantOnSchemaSupported", sr.isGrantOnSchemaSupported);
        assertFalse("isGrantOnTableSupported",  sr.isGrantOnTableSupported);
        assertFalse("isEnumTypeSupported",      sr.isEnumTypeSupported);
        assertFalse("isCommentSupported",       sr.isCommentSupported);
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
        assertFalse(sr.isSupported("GRANT SELECT ON TABLE epsg_coordinatereferencesystem TO PUBLIC"));
        assertFalse(sr.isSupported("COMMENT ON SCHEMA metadata IS 'ISO 19115 metadata'"));
    }
}
