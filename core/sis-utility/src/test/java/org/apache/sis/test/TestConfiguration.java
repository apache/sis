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

import org.apache.sis.util.Static;


/**
 * Information about the configuration of tests
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public final strictfp class TestConfiguration extends Static {
    /**
     * The {@value} system property for enabling more extensive tests.
     * If this {@linkplain System#getProperties() system property} is set to {@code true},
     * then Apache SIS will run some tests which were normally skipped because they are slow.
     */
    public static final String EXTENSIVE_TESTS_KEY = "org.apache.sis.test.extensive";

    /**
     * The {@value} system property for enabling verbose outputs.
     * If this {@linkplain System#getProperties() system property} is set to {@code true},
     * then the content sent to the {@link TestCase#out} field will be printed after each test.
     */
    public static final String VERBOSE_OUTPUT_KEY = "org.apache.sis.test.verbose";

    /**
     * The {@value} system property for setting the output encoding.
     * This property is used only if the {@link #VERBOSE_OUTPUT_KEY} property
     * is set to "{@code true}". If this property is not set, then the system
     * encoding will be used.
     */
    public static final String OUTPUT_ENCODING_KEY = "org.apache.sis.test.encoding";

    /**
     * Do not allow instantiation of this class.
     */
    private TestConfiguration() {
    }

    /**
     * Returns {@code true} if tests that may depend on the garbage collector activity are allowed.
     * Those tests are a little bit dangerous since they may randomly fail on a server too busy for
     * running the garbage collector as fast as expected.
     *
     * @return {@code true} if tests that may depend on garbage collector activity are allowed.
     */
    public static boolean allowGarbageCollectorDependentTests() {
        return true;
    }
}
