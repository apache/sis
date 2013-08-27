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
import org.junit.runners.Suite;
import org.junit.BeforeClass;


/**
 * All tests from the {@code sis-referencing} module, in approximative dependency order.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@Suite.SuiteClasses({
    org.apache.sis.geometry.AbstractDirectPositionTest.class,
    org.apache.sis.geometry.GeneralDirectPositionTest.class,
    org.apache.sis.geometry.DirectPosition1DTest.class,
    org.apache.sis.geometry.DirectPosition2DTest.class,
    org.apache.sis.geometry.AbstractEnvelopeTest.class,
    org.apache.sis.geometry.GeneralEnvelopeTest.class,
    org.apache.sis.geometry.SubEnvelopeTest.class,
    org.apache.sis.geometry.ImmutableEnvelopeTest.class,
    org.apache.sis.geometry.Envelope2DTest.class,
    org.apache.sis.io.wkt.ConventionTest.class,
    org.apache.sis.io.wkt.SymbolsTest.class,
    org.apache.sis.io.wkt.FormatterTest.class
})
public final strictfp class ReferencingTestSuite extends TestSuite {
    /**
     * Verifies the list of tests before to run the suite.
     * See {@link #verifyTestList(Class, Class[])} for more information.
     */
    @BeforeClass
    public static void verifyTestList() {
        verifyTestList(ReferencingTestSuite.class, BASE_TEST_CLASSES);
    }
}
