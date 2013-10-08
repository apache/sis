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

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.net.URL;
import java.net.URISyntaxException;
import org.apache.sis.util.Classes;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static org.junit.Assert.*;


/**
 * Base class of Apache SIS test suites (except the ones that extend GeoAPI suites).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.16)
 * @version 0.4
 * @module
 */
@RunWith(Suite.class)
public abstract strictfp class TestSuite {
    /**
     * The default set of base classes that all test cases are expected to extends.
     * This is the default argument value for {@link #verifyTestList(Class)} method.
     */
    private static final Class<?>[] BASE_TEST_CLASSES = {
        TestCase.class,
        org.opengis.test.TestCase.class
    };

    /**
     * Creates a new test suite.
     */
    protected TestSuite() {
    }

    /**
     * Verifies that we did not forgot to declare some test classes in the given suite.
     * This method scans the directory for {@code *Test.class} files.
     *
     * @param suite The suite for which to check for missing tests.
     */
    protected static void assertNoMissingTest(final Class<? extends TestSuite> suite) {
        final ClassLoader loader = suite.getClassLoader();
        final URL url = loader.getResource(suite.getName().replace('.', '/') + ".class");
        assertNotNull("Test suite class not found.", url);
        File root;
        try {
            root = new File(url.toURI());
        } catch (URISyntaxException | IllegalArgumentException e) {
            // If not a file, then it is probably an entry in a JAR file.
            fail(e.toString());
            return;
        }
        for (File c = new File(suite.getName().replace('.', File.separatorChar)); (c = c.getParentFile()) != null;) {
            root = root.getParentFile();
            assertNotNull("Unexpected directory structure.", root);
            assertEquals("Unexpected directory structure.", c.getName(), root.getName());
        }
        /*
         * At this point, we found the root "org" package. Verifies if we are in the Maven target directory.
         * In some IDE configuration, all the ".class" files are in the same directory, in which case the
         * verification performed by this method become irrelevant.
         */
        if (!new File(root.getParent(), ".." + File.separatorChar + "pom.xml").isFile()) {
            return;
        }
        /*
         * Now scan all "*Test.class" in the "target/org" directory and and sub-directories,
         * and fail on the first missing test file if any.
         */
        final Set<Class<?>> tests = new HashSet<>(Arrays.asList(suite.getAnnotation(Suite.SuiteClasses.class).value()));
        removeExistingTests(loader, root, new StringBuilder(120).append(root.getName()), tests);
        assertTrue(tests.toString(), tests.isEmpty());
    }

    /**
     * Ensures that all tests in the given directory and sub-directories exit in the given set.
     * This method invokes itself recursively for scanning the sub-directories.
     */
    private static void removeExistingTests(final ClassLoader loader, final File directory,
            final StringBuilder path, final Set<Class<?>> tests)
    {
        final int length = path.append('.').length();
        for (final File file : directory.listFiles()) {
            if (!file.isHidden()) {
                final String name = file.getName();
                if (!name.startsWith(".")) {
                    path.append(name);
                    if (file.isDirectory()) {
                        removeExistingTests(loader, file, path, tests);
                    } else {
                        if (name.endsWith("Test.class")) {
                            path.setLength(path.length() - 6); // Remove trailing ".class"
                            final String classname = path.toString();
                            final Class<?> test;
                            try {
                                test = Class.forName(classname, false, loader);
                            } catch (ClassNotFoundException e) {
                                fail(e.toString());
                                return;
                            }
                            assertTrue(classname, tests.remove(test));
                        }
                    }
                    path.setLength(length);
                }
            }
        }
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
     *        assertNoMissingTest(MyTestSuite.class);
     *        verifyTestList(MyTestSuite.class);
     *    }
     * }
     *
     * @param suite The suite for which to verify test order.
     */
    protected static void verifyTestList(final Class<? extends TestSuite> suite) {
        verifyTestList(suite, BASE_TEST_CLASSES);
    }

    /**
     * Same verification than {@link #verifyTestList(Class)}, except that the set of base classes
     * is explicitely specified. This method is preferred to {@code verifyTestList(Class)} only in
     * the rare cases where some test cases need to extend something else than geoapi-conformance
     * or Apache SIS test class.
     *
     * @param suite The suite for which to verify test order.
     * @param baseTestClasses The set of base classes that all test cases are expected to extends.
     */
    protected static void verifyTestList(final Class<? extends TestSuite> suite, final Class<?>[] baseTestClasses) {
        final Class<?>[] testCases = suite.getAnnotation(Suite.SuiteClasses.class).value();
        final Set<Class<?>> done = new HashSet<>(testCases.length);
        for (final Class<?> testCase : testCases) {
            if (!Classes.isAssignableToAny(testCase, baseTestClasses)) {
                fail("Class " + testCase.getCanonicalName() + " does not extends TestCase.");
            }
            final DependsOn dependencies = testCase.getAnnotation(DependsOn.class);
            if (dependencies != null) {
                for (final Class<?> dependency : dependencies.value()) {
                    if (!done.contains(dependency)) {
                        fail("Class " + testCase.getCanonicalName() + " depends on " + dependency.getCanonicalName()
                                + ", but the dependency has not been found before the test.");
                    }
                }
            }
            if (!done.add(testCase)) {
                fail("Class " + testCase.getCanonicalName() + " is declared twice.");
            }
        }
    }
}
