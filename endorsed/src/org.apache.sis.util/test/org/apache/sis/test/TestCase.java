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
import java.io.Console;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.logging.MonolineFormatter;

// Test dependencies
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Base class of Apache SIS tests (except the ones that extend GeoAPI tests).
 * This base class provides an {@link #out} field that sub-classes can use
 * for printing debugging information. This {@code out} field shall be used
 * instead of {@link System#out} for the following reasons:
 *
 * <ul>
 *   <li>By default, the contents sent to {@code out} are ignored for successful tests
 *     and printed only when a test fail. This avoid polluting the console output during
 *     successful Maven builds, and print only the information related to failed tests.</li>
 *   <li>Printing information for all tests can be enabled if a system property is set as
 *     described in the {@linkplain org.apache.sis.test package javadoc}.</li>
 *   <li>The outputs are collected and printed only after test completion.
 *     This avoid the problem of logging messages interleaved with the output.
 *     If such interleaving is really wanted, then use the logging framework instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@ExtendWith(FailureDetailsReporter.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public abstract class TestCase {
    /**
     * A flag for code that are pending future SIS development before to be enabled.
     * This flag is always set to {@code false}. It shall be used as below:
     *
     * {@snippet lang="java" :
     *     if (PENDING_FUTURE_SIS_VERSION) {
     *         // Do some stuff here.
     *     }
     *     }
     *
     * The intent is to make easier to identify test cases that fail with the current version
     * of SIS (e.g. because of unsupported operations), but should pass in a future version.
     *
     * <p>Note: sometimes the work is actually pending future GeoAPI development. But we still
     * use that flag for those cases because the {@code "geoapi"} branches of Apache SIS follow
     * closely GeoAPI developments.</p>
     */
    public static final boolean PENDING_FUTURE_SIS_VERSION = false;

    /**
     * Tolerance threshold for strict comparisons of floating point numbers.
     * This value can be used in {@code assert} method calls when the method
     * signature mandate a tolerance threshold.
     */
    public static final double STRICT = 0;

    /**
     * Tag for tests that are slow.
     * All tests annotated with this tag should check for the {@link #RUN_EXTENSIVE_TESTS} flag.
     *
     * @see Benchmark#TAG
     */
    public static final String TAG_SLOW = "Slow";

    /**
     * Whether to run more extensive tests. This is set to the value specified by the
     * {@value org.apache.sis.test.TestConfiguration#EXTENSIVE_TESTS_KEY} system property if defined,
     * or otherwise this is {@code true} if the {@value org.apache.sis.test.TestConfiguration#SIS_TEST_OPTIONS}
     * environment variable contains the "extensive" word. If {@code true}, then Apache SIS will run some tests
     * which were normally skipped because they are slow.
     *
     * <p>All tests using this condition should be annotated with {@code @Tag(TAG_SLOW)}.</p>
     */
    public static final boolean RUN_EXTENSIVE_TESTS;

    /**
     * Whether the tests can use the PostgreSQL database on the local host.
     */
    public static final boolean USE_POSTGRESQL;

    /**
     * Whether the tests should print debugging information. This is set to the value specified by the
     * {@value org.apache.sis.test.TestConfiguration#VERBOSE_OUTPUT_KEY} system property if defined,
     * or otherwise this is {@code true} if the {@value org.apache.sis.test.TestConfiguration#SIS_TEST_OPTIONS}
     * environment variable contains the "verbose" word. If {@code true}, then the content of {@link #out} will
     * be sent to the standard output stream after each test.
     *
     * @see #out
     */
    public static final boolean VERBOSE;

    /**
     * Whether the tests can allowed to popup a widget. This is set to the value specified by the
     * {@value org.apache.sis.test.TestConfiguration#SHOW_WIDGET_KEY} system property if defined,
     * or otherwise this is {@code true} if the {@value org.apache.sis.test.TestConfiguration#SIS_TEST_OPTIONS}
     * environment variable contains the "widget" word. Only a few tests provide visualization widget.
     */
    public static final boolean SHOW_WIDGET;

    /**
     * The output writer where to print debugging information (never {@code null}).
     * Texts sent to this printer will be shown only if the test fails, or if the
     * {@link #VERBOSE} flag is {@code true}. This writer will use the system default encoding,
     * unless the {@value org.apache.sis.test.TestConfiguration#OUTPUT_ENCODING_KEY} system property
     * has been set to a different value.
     *
     * @see TestUtilities#forceFlushOutput()
     */
    public static final PrintWriter out;

    /**
     * The buffer which is backing the {@linkplain #out} stream.
     */
    private static final StringWriter buffer;

    /**
     * Sets the {@link #out} writer and its underlying {@link #buffer}.
     */
    static {
        out = new PrintWriter(buffer = new StringWriter());
        final Set<String> options = Set.of(Strings.orEmpty(System.getenv(TestConfiguration.SIS_TEST_OPTIONS)).split(","));
        RUN_EXTENSIVE_TESTS = isEnabled(options, "extensive",  TestConfiguration.EXTENSIVE_TESTS_KEY);
        USE_POSTGRESQL      = isEnabled(options, "postgresql", TestConfiguration.USE_POSTGRESQL_KEY);
        SHOW_WIDGET         = isEnabled(options, "widget",     TestConfiguration.SHOW_WIDGET_KEY);
        VERBOSE             = isEnabled(options, "verbose",    TestConfiguration.VERBOSE_OUTPUT_KEY);
        if (VERBOSE) {
            System.setErr(System.out);      // For avoiding log records to be interleaved with block of text.
        }
    }

    /**
     * Returns whether the user has enabled an option.
     *
     * @param  options   {@value org.apache.sis.test.TestConfiguration#SIS_TEST_OPTIONS} environment variable content.
     * @param  keyword   keyword to search in {@code options}.
     * @param  property  system property to search.
     * @return whether the specified option is enabled.
     */
    private static boolean isEnabled(final Set<String> options, final String keyword, final String property) {
        final String value = System.getProperty(property);
        if (value != null) {
            return Boolean.parseBoolean(value);
        } else {
            return options.contains(keyword);
        }
    }

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
     * Creates a new test case.
     */
    protected TestCase() {
    }

    /**
     * Invoked by {@link TestRunner} in order to clear the buffer before a new test begin.
     * This is necessary when the previous test succeeded and the {@link #VERBOSE} flag is
     * {@code false}, since the {@link #flushOutput()} method has not been invoked in such
     * case.
     */
    static void clearBuffer() {
        synchronized (buffer) {             // This is the lock used by the 'out' PrintWriter.
            out.flush();
            buffer.getBuffer().setLength(0);
        }
    }

    /**
     * Invoked by {@link TestRunner} in order to flush the {@link #out} stream.
     * The stream content will be flushed to the {@linkplain System#console() console}
     * if available, or to the {@linkplain System#out standard output stream} otherwise.
     * This method clears the stream buffer.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    static void flushOutput() {
        System.out.flush();
        System.err.flush();
        synchronized (buffer) {             // This is the lock used by the 'out' PrintWriter.
            out.flush();
            /*
             * Get the text content and remove the trailing spaces
             * (including line feeds), if any.
             */
            String content = buffer.toString();
            int length = content.length();
            do if (length == 0) return;
            while (Character.isWhitespace(content.charAt(--length)));
            content = content.substring(0, ++length);
            /*
             * Get the output writer, using the specified encoding if any.
             */
            PrintWriter writer = null;
            final String encoding = System.getProperty(TestConfiguration.OUTPUT_ENCODING_KEY);
            if (encoding == null) {
                final Console console = System.console();
                if (console != null) {
                    writer = console.writer();
                }
            }
            if (writer == null) {
                if (encoding != null) try {
                    writer = new PrintWriter(new OutputStreamWriter(System.out, encoding));
                } catch (UnsupportedEncodingException e) {
                    // Ignore. We will use the default encoding.
                }
                if (writer == null) {
                    writer = new PrintWriter(System.out);
                }
            }
            writer.println(content);
            writer.flush();
            buffer.getBuffer().setLength(0);
        }
    }
}
