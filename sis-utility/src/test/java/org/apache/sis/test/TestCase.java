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
import java.io.Console;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import org.apache.sis.util.logging.Logging;
import org.junit.runner.RunWith;

import static org.apache.sis.test.TestConfiguration.VERBOSE_OUTPUT_KEY;
import static org.apache.sis.test.TestConfiguration.OUTPUT_ENCODING_KEY;


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
 * @since   0.3 (derived from geotk-3.16)
 * @version 0.3
 * @module
 */
@RunWith(TestRunner.class)
public abstract strictfp class TestCase {
    /**
     * A flag for code that are pending next GeoAPI release before to be enabled.
     * This flag is always set to {@code false}, except occasionally just before
     * a GeoAPI release for testing purpose. It shall be used as below:
     *
     * {@preformat java
     *     if (PENDING_NEXT_GEOAPI_RELEASE) {
     *         // Do some stuff here.
     *     }
     * }
     *
     * The intend is to make easier to identify test cases that fail with the current version
     * of the {@code geoapi-conformance} module, but should pass with the development snapshot.
     */
    public static final boolean PENDING_NEXT_GEOAPI_RELEASE = false;

    /**
     * The output writer where to print debugging information (never {@code null}).
     * Texts sent to this printer will be show only if the test fails, or if the
     * {@value org.apache.sis.test.TestConfiguration#VERBOSE_OUTPUT_KEY} system property
     * is set to {@code true}. This writer will use the system default encoding, unless
     * the {@value org.apache.sis.test.TestConfiguration#OUTPUT_ENCODING_KEY} system
     * property has been set to a different value.
     *
     * @see org.apache.sis.test
     * @see #flushVerboseOutput()
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
    static final boolean verbose;

    /**
     * Sets the {@link #out} writer and its underlying {@link #buffer}.
     */
    static {
        verbose = Boolean.getBoolean(VERBOSE_OUTPUT_KEY);
        out = new PrintWriter(buffer = new StringWriter());
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
        final String encoding = System.getProperty(OUTPUT_ENCODING_KEY);
        if (encoding != null) try {
            for (Logger logger=Logger.getLogger("org.apache.sis"); logger!=null; logger=logger.getParent()) {
                for (final Handler handler : logger.getHandlers()) {
                    if (handler instanceof ConsoleHandler) {
                        ((ConsoleHandler) handler).setEncoding(encoding);
                    }
                }
                if (!logger.getUseParentHandlers()) {
                    break;
                }
            }
        } catch (UnsupportedEncodingException e) {
            Logging.recoverableException(TestCase.class, "<clinit>", e);
        }
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
            final String encoding = System.getProperty(OUTPUT_ENCODING_KEY);
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
