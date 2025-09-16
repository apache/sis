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


/**
 * Information about the configuration of tests
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TestConfiguration {
    /**
     * Environment variable to use as a fallback if a system property is not set.
     * This is a comma-separated list of the following keywords, without spaces:
     * {@code extensive}, {@code postgresql}, {@code widget}, {@code verbose}.
     *
     * @see TestCase#RUN_EXTENSIVE_TESTS
     * @see TestCase#VERBOSE
     * @see TestCase#SHOW_WIDGET
     */
    public static final String SIS_TEST_OPTIONS = "SIS_TEST_OPTIONS";

    /**
     * The {@systemProperty org.apache.sis.test.extensive} system property for enabling more extensive tests.
     * If this {@linkplain System#getProperties() system property} is set to {@code true},
     * then Apache SIS will run some tests which were normally skipped because they are slow.
     *
     * <p>Alternatively, the extensive tests can also be enabled by setting the
     * {@value #SIS_TEST_OPTIONS} environment variable to {@code "extensive"}.</p>
     *
     * @see #SIS_TEST_OPTIONS
     * @see TestCase#RUN_EXTENSIVE_TESTS
     */
    public static final String EXTENSIVE_TESTS_KEY = "org.apache.sis.test.extensive";

    /**
     * The {@systemProperty org.apache.sis.test.postgresql} system property for enabling tests
     * on the PostgreSQL database. If this {@linkplain System#getProperties() system property}
     * is set to {@code true}, then the {@code "SpatialMetadataTest"} database will be used.
     *
     * <p>Alternatively, the tests on PostgreSQL can also be enabled by setting the
     * {@value #SIS_TEST_OPTIONS} environment variable to {@code "postgresql"}.</p>
     *
     * @see #SIS_TEST_OPTIONS
     * @see TestCase#USE_POSTGRESQL
     * @see org.apache.sis.metadata.sql.TestDatabase
     */
    public static final String USE_POSTGRESQL_KEY = "org.apache.sis.test.postgresql";

    /**
     * The {@systemProperty org.apache.sis.test.verbose} system property for enabling verbose outputs.
     * If this {@linkplain System#getProperties() system property} is set to {@code true},
     * then the content sent to the {@link TestCase#out} field will be printed after each test.
     *
     * <p>Alternatively, the verbose outputs can also be enabled by setting the
     * {@value #SIS_TEST_OPTIONS} environment variable to {@code "verbose"}.</p>
     *
     * @see #SIS_TEST_OPTIONS
     * @see TestCase#VERBOSE
     */
    public static final String VERBOSE_OUTPUT_KEY = "org.apache.sis.test.verbose";

    /**
     * The {@systemProperty org.apache.sis.test.gui.show} system property
     * for enabling display of test images or widgets.
     *
     * <p>Alternatively, the widgets display can also be enabled by setting the
     * {@value #SIS_TEST_OPTIONS} environment variable to {@code "widget"}.</p>
     *
     * @see #SIS_TEST_OPTIONS
     * @see TestCase#SHOW_WIDGET
     */
    public static final String SHOW_WIDGET_KEY = "org.apache.sis.test.gui.show";

    /**
     * The {@systemProperty org.apache.sis.test.encoding} system property for setting the output encoding.
     * This property is used only if the {@link #VERBOSE_OUTPUT_KEY} property
     * is set to "{@code true}". If this property is not set, then the system
     * encoding will be used.
     *
     * @see TestCase#out
     */
    public static final String OUTPUT_ENCODING_KEY = "org.apache.sis.test.encoding";

    /**
     * Do not allow instantiation of this class.
     */
    private TestConfiguration() {
    }
}
