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
package org.apache.sis.test;

import java.util.Map;
import java.util.IdentityHashMap;
import org.apache.sis.util.Classes;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static org.junit.Assert.*;


/**
 * Base class of Apache SIS test suites (except the ones that extend GeoAPI suites).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.16)
 * @version 0.3
 * @module
 */
@RunWith(Suite.class)
public abstract strictfp class TestSuite {
    /**
     * The default set of base classes that all test cases are expected to extends.
     * This is the usual argument value to the {@link #verifyTestList(Class, Class[])} method.
     */
    protected static final Class<?>[] BASE_TEST_CLASSES = {
        TestCase.class,
        org.opengis.test.TestCase.class
    };

    /**
     * Creates a new test suite.
     */
    protected TestSuite() {
    }

    /**
     * Verifies the list of tests before the suite is run.
     * This method verifies the following conditions:
     *
     * <ul>
     *   <li>Every class shall extend either the SIS {@link TestCase} or the GeoAPI {@link org.opengis.test.TestCase}.</li>
     *   <li>No class shall be declared twice.</li>
     *   <li>If a test depends on another test, then the other test shall be before the dependant test.</li>
     * </ul>
     *
     * Subclasses shall invoke this method as below:
     *
     * {@preformat java
     *    &#64;BeforeClass
     *    public static void verifyTestList() {
     *        verifyTestList(MetadataTestSuite.class, BASE_TEST_CLASSES);
     *    }
     * }
     *
     * @param suite The suite for which to verify order.
     * @param baseTestClasses The set of base classes that all test cases are expected to extends.
     *        This is usually {@link #BASE_TEST_CLASSES}.
     */
    protected static void verifyTestList(final Class<? extends TestSuite> suite, final Class<?>[] baseTestClasses) {
        final Class<?>[] testCases = suite.getAnnotation(Suite.SuiteClasses.class).value();
        final Map<Class<?>,Boolean> done = new IdentityHashMap<>(testCases.length);
        for (final Class<?> testCase : testCases) {
            if (!Classes.isAssignableToAny(testCase, baseTestClasses)) {
                fail("Class " + testCase.getCanonicalName() + " does not extends TestCase.");
            }
            final DependsOn dependencies = testCase.getAnnotation(DependsOn.class);
            if (dependencies != null) {
                for (final Class<?> dependency : dependencies.value()) {
                    if (!done.containsKey(dependency)) {
                        fail("Class " + testCase.getCanonicalName() + " depends on " + dependency.getCanonicalName()
                                + ", but the dependency has not been found before the test.");
                    }
                }
            }
            if (done.put(testCase, Boolean.TRUE) != null) {
                fail("Class " + testCase.getCanonicalName() + " is declared twice.");
            }
        }
    }
}
