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
package org.apache.sis.test.suite;

import org.apache.sis.test.TestSuite;
import org.junit.BeforeClass;
import org.junit.runners.Suite;


/**
 * All tests from the {@code sis-portrayal} module, in rough dependency order.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
@Suite.SuiteClasses({
    org.apache.sis.portrayal.MapLayersTest.class,
    org.apache.sis.internal.map.SEPortrayerTest.class,
})
public final strictfp class PortrayalTestSuite extends TestSuite {
    /**
     * Verifies the list of tests before to run the suite.
     * See {@link #verifyTestList(Class, Class[])} for more information.
     */
    @BeforeClass
    public static void verifyTestList() {
        assertNoMissingTest(PortrayalTestSuite.class);
        verifyTestList(PortrayalTestSuite.class);
    }
}