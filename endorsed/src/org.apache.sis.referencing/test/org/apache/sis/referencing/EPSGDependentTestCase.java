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
package org.apache.sis.referencing;

// Test dependencies
import org.junit.jupiter.api.BeforeAll;
import org.apache.sis.test.TestCase;


/**
 * Base class of Apache SIS tests that may depend on the EPSG database (except GeoAPI tests).
 * This base class is used for checking in a single place whether the EPSG database exists on
 * the local machine. Note that this is not necessarily for tests that <em>require</em> EPSG.
 * This base class is also for tests that <em>may</em> use EPSG if present, sometime indirectly.
 * For example, even when the test is using {@link org.apache.sis.referencing.crs.HardCodedCRS}
 * constants, SIS may still look for EPSG data for validation or for searching coordinate operations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class EPSGDependentTestCase extends TestCase {
    /**
     * Creates a new test case.
     */
    protected EPSGDependentTestCase() {
    }

    /**
     * Performs an arbitrary operation which will force Apache SIS to check whether the EPSG database exists.
     * This is done before any test is started in order to avoid race conditions causing the same message to
     * be logged many times when tests are run in parallel. Note that those race conditions do not break the
     * tests (results are still correct). It only pollutes the logs, and may cause random test failures when
     * the test verifies the logs.
     */
    @BeforeAll
    public static void forceCheckForEPSG() {
        CommonCRS.defaultGeographic();
    }
}
