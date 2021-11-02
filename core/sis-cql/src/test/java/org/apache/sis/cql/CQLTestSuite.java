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
package org.apache.sis.cql;

import org.apache.sis.test.TestSuite;
import org.junit.BeforeClass;
import org.junit.runners.Suite;


/**
 * All tests from the {@code sis-cql} module.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@Suite.SuiteClasses({
    org.apache.sis.cql.ExpressionReadingTest.class,
    org.apache.sis.cql.ExpressionWritingTest.class,
    org.apache.sis.cql.FilterReadingTest.class,
    org.apache.sis.cql.FilterWritingTest.class,
    org.apache.sis.cql.QueryReadingTest.class,
    org.apache.sis.cql.QueryWritingTest.class,
})
public final strictfp class CQLTestSuite extends TestSuite {
    /**
     * Verifies the list of tests before to run the suite.
     * See {@link #verifyTestList(Class, Class[])} for more information.
     */
    @BeforeClass
    public static void verifyTestList() {
        assertNoMissingTest(CQLTestSuite.class);
        verifyTestList(CQLTestSuite.class);
    }
}
