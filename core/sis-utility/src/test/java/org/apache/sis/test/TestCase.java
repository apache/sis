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

import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;
import java.io.Console;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.logging.MonolineFormatter;
import org.junit.runner.RunWith;


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
 * @since   0.3
 * @version 0.7
 * @module
 */
@RunWith(TestRunner.class)
public abstract strictfp class TestCase {
    /**
     * A flag for code that are pending future SIS development before to be enabled.
     * This flag is always set to {@code false}. It shall be used as below:
     *
     * {@preformat java
     *     if (PENDING_FUTURE_SIS_VERSION) {
     *         // Do some stuff here.
     *     }
     * }
     *
     * The intend is to make easier to identify test cases that fail with the current version
     * of SIS (e.g. because of unsupported operations), but should pass in a future version.
     *
     * @since 0.4
     */
    public static final boolean PENDING_FUTURE_SIS_VERSION = false;

    /**
     * Tolerance threshold for strict comparisons of floating point numbers.
     * This constant can be used like below, where {@code expected} and {@code actual} are {@code double} values:
     *
     * {@preformat java
     *     assertEquals(expected, actual, STRICT);
     * }
     */
    protected static final double STRICT = 0;

    /**
     * The seed for the random number generator created by {@link TestUtilities#createRandomNumberGenerator(String)},
     * or 0 if none. This information is used for printing the seed in case of test failure, in order to allow the
     * developer to reproduce the failure.
     */
    static long randomSeed;

    /**
     * The output writer where to print debugging information (never {@code null}).
     * Texts sent to this printer will be shown only if the test fails, or if the
     * {@value org.apache.sis.test.TestConfiguration#VERBOSE_OUTPUT_KEY} system property
     * is set to {@code true}. This writer will use the system default encoding, unless
     * the {@value org.apache.sis.test.TestConfiguration#OUTPUT_ENCODING_KEY} system
     * property has been set to a different value.
     *
     * @see org.apache.sis.test
     */
    public static final PrintWriter out;

    /**
     * The buffer which is backing the {@linkplain #out} stream.
     */
    private static final StringWriter buffer;

    /**
     * {@code true} if the {@value org.apache.sis.test.TestConfiguration#VERBOSE_OUTPUT_KEY}
     * system property is set to {@code true}.
     */
    public static final boolean VERBOSE;

    /**
     * {@code true} if the {@value org.apache.sis.test.TestConfiguration#EXTENSIVE_TESTS_KEY}
     * system property is set to {@code true}.
     * If {@code true}, then Apache SIS will run some tests which were normally skipped because they are slow.
     */
    public static final boolean RUN_EXTENSIVE_TESTS;

    /**
     * Sets the {@link #out} writer and its underlying {@link #buffer}.
     */
    static {
        out = new PrintWriter(buffer = new StringWriter());
        VERBOSE = Boolean.getBoolean(TestConfiguration.VERBOSE_OUTPUT_KEY);
        RUN_EXTENSIVE_TESTS = Boolean.getBoolean(TestConfiguration.EXTENSIVE_TESTS_KEY);
    }

    /**
     * The parent logger of all Apache SIS loggers.
     * Needs to be retained by strong reference.
     */
    static final Logger LOGGER = Logger.getLogger("org.apache.sis");

    /**
     * Initializes {@link MonolineFormatter} if it has been specified in the {@code logging.properties}
     * configuration file.
     */
    static {
        final LogManager manager = LogManager.getLogManager();
        if (MonolineFormatter.class.getName().equals(manager.getProperty(ConsoleHandler.class.getName() + ".formatter"))) {
            MonolineFormatter.install();
        }
    }

    /**
     * Sets the encoding of the console logging handler, if an encoding has been specified.
     * Note that we look specifically for {@link ConsoleHandler}; we do not generalize to
     * {@link StreamHandler} because the log files may not be intended for being show in
     * the console.
     *
     * <p>In case of failure to use the given encoding, we will just print a short error
     * message and left the encoding unchanged.</p>
     */
    static {
        final String encoding = System.getProperty(TestConfiguration.OUTPUT_ENCODING_KEY);
        if (encoding != null) try {
            for (Logger logger=LOGGER; logger!=null; logger=logger.getParent()) {
                for (final Handler handler : logger.getHandlers()) {
                    if (handler instanceof ConsoleHandler) {
                        handler.setEncoding(encoding);
                    }
                }
                if (!logger.getUseParentHandlers()) {
                    break;
                }
            }
        } catch (UnsupportedEncodingException e) {
            Logging.recoverableException(LOGGER, TestCase.class, "<clinit>", e);
        }
        LOGGER.addHandler(LogRecordCollector.INSTANCE);
    }

    /**
     * Creates a new test case.
     */
    protected TestCase() {
    }

    /**
     * Invoked by {@link TestRunner} in order to clear the buffer before a new test begin.
     * This is necessary when the previous test succeeded and the {@link #verbose} flag is
     * {@code false}, since the {@link #flushOutput()} method has not been invoked in such
     * case.
     */
    static void clearBuffer() {
        synchronized (buffer) { // This is the lock used by the 'out' PrintWriter.
            out.flush();
            buffer.getBuffer().setLength(0);
        }
    }

    /**
     * Invoked by {@link TestRunner} in order to flush the {@link #out} stream.
     * The stream content will be flushed to the {@linkplain System#console() console}
     * if available, or to the {@linkplain System#out standard output stream} otherwise.
     * This method clears the stream buffer.
     *
     * @param success {@code true} if this method is invoked on build success,
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    static void flushOutput() {
        System.out.flush();
        System.err.flush();
        synchronized (buffer) { // This is the lock used by the 'out' PrintWriter.
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
