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

import java.util.Set;
import org.apache.sis.util.logging.MonolineFormatter;

// Test dependencies
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * Base class of Apache SIS tests (except the ones that extend GeoAPI tests).
 * This base class provides an {@link #out} field that sub-classes can use
 * for printing debugging information. That {@code out} field should be used
 * instead of {@link System#out} for the following reasons:
 *
 * <ul>
 *   <li>By default, the contents sent to {@code out} are discarded after successful tests
 *     and printed only when a test fails. This avoids polluting the console output during
 *     successful builds, and prints only the information related to failed tests.</li>
 *   <li>The printing of information for all tests can be enabled if a system property
 *     is set as described in the {@linkplain org.apache.sis.test package javadoc}.</li>
 *   <li>The outputs are collected and printed only after test completion.
 *     This strategy avoids the problem of logging messages interleaved with the outputs.
 *     If such interleaving is really wanted, then use the logging framework instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
@ExtendWith(FailureDetailsReporter.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public abstract class TestCase {
    /**
     * Installs Apache SIS monoline formatter for easier identification of Apache SIS log messages among Maven outputs.
     */
    static {
        MonolineFormatter f = MonolineFormatter.install();
        f.setHeader(null);
        f.setTimeFormat(null);
        f.setSourceFormat("class.method");
    }

    /**
     * Whether to run more extensive tests.
     * If {@code true}, then the test suite will include some tests that were normally skipped because they are slow.
     *
     * @see #assumeExtensiveTestsEnabled()
     */
    public static final boolean RUN_EXTENSIVE_TESTS;

    /**
     * Whether the tests shall use the <abbr>EPSG</abbr> geodetic dataset.
     * If {@code false}, an absence of <abbr>EPSG</abbr> database will not cause test failure.
     *
     * @see #assumeConnectionToEPSG(boolean)
     */
    public static final boolean REQUIRE_EPSG_DATABASE;

    /**
     * Whether the tests can use the PostgreSQL database on the local host.
     * If {@code true}, then the {@code "SpatialMetadataTest"} database will be used if present.
     */
    public static final boolean USE_POSTGRESQL;

    /**
     * Whether the tests is allowed to popup a widget.
     * Only a few tests provide visualization widget.
     */
    public static final boolean SHOW_WIDGET;

    /**
     * Whether the tests should print debugging information.
     * If {@code true}, then the content of {@link #out}
     * will be sent to the standard output stream after each test.
     *
     * @see #out
     */
    public static final boolean VERBOSE;

    /**
     * Sets the values of all test configuration flags.
     */
    static {
        String env = System.getenv("SIS_TEST_OPTIONS");
        final Set<String> options = (env != null) ? Set.of(env.split(",")) : Set.of();
        RUN_EXTENSIVE_TESTS   = isEnabled(options, "extensive");
        REQUIRE_EPSG_DATABASE = isEnabled(options, "epsg");
        USE_POSTGRESQL        = isEnabled(options, "postgresql");
        SHOW_WIDGET           = isEnabled(options, "widget");
        VERBOSE               = isEnabled(options, "verbose");
    }

    /**
     * Returns whether the user has enabled an option.
     *
     * @param  options   {@code SIS_TEST_OPTIONS} environment variable content.
     * @param  keyword   keyword to search in {@code options}.
     * @param  property  system property to search.
     * @return whether the specified option is enabled.
     */
    private static boolean isEnabled(final Set<String> options, final String keyword) {
        final String value = System.getProperty("org.apache.sis.test." + keyword);
        if (value != null) {
            return Boolean.parseBoolean(value);
        } else {
            return options.contains(keyword);
        }
    }

    /**
     * The output writer where to print debugging information (never {@code null}).
     * Texts sent to this printer will be shown only if a test fails, or if the tests are in verbose mode.
     *
     * @see #verbose()
     */
    public static final Printer out = new Printer();

    /**
     * Creates a new test case.
     */
    protected TestCase() {
    }

    /**
     * Invoked as the first line of test methods that are expensive.
     * If {@link #RUN_EXTENSIVE_TESTS} has not been set to {@code true}, the test is interrupted.
     */
    protected static void assumeExtensiveTestsEnabled() {
        assumeTrue(RUN_EXTENSIVE_TESTS, "Extensive tests are not enabled.");
    }

    /**
     * Invoked for all tests that require a connection to the <abbr>EPSG</abbr> geodetic dataset.
     * Different tests may have different way to determine whether the <abbr>EPSG</abbr> database
     * is available. If the {@code available} argument is {@code false}, the test will fail if the
     * {@link #REQUIRE_EPSG_DATABASE} flag is {@code true}, or skipped otherwise.
     *
     * @param  available  whether the caller found a connection to the <abbr>EPSG</abbr> database.
     */
    public static void assumeConnectionToEPSG(final boolean available) {
        if (REQUIRE_EPSG_DATABASE) {
            assertTrue(available, "No connection to EPSG geodetic dataset.");
        } else {
            assumeTrue(available, "No connection to EPSG geodetic dataset.");
        }
    }
}
