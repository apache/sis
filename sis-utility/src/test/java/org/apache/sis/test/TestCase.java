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

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.Console;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;

import org.junit.AfterClass;
import org.junit.runner.RunWith;


/**
 * Base class of Apache SIS tests (except the ones that extend GeoAPI tests).
 * This base class provides some configuration commons to all subclasses.
 *
 * {@section Printing to the console}
 * Subclasses should avoid printing to {@link System#out}. If nevertheless a test method
 * produces some information considered worth to be known, consider using the following
 * pattern instead:
 *
 * {@preformat java
 *     if (out != null) {
 *         out.println("Put here some information of particular interest.");
 *     }
 * }
 *
 * The above example uses the {@link #out} field, which will be set to a non-null value
 * if the following option has been provided on the command line:
 *
 * <blockquote><code>-D{@value #VERBOSE_KEY}=true</code></blockquote>
 *
 * Developers can also optionally provide the following option, which is useful on Windows
 * or MacOS platforms having a console encoding different than the system encoding:
 *
 * <blockquote><code>-D{@value #ENCODING_KEY}=UTF-8</code> (or any other valid encoding name)</blockquote>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.16)
 * @version 0.3
 * @module
 */
@RunWith(TestRunner.class)
public abstract strictfp class TestCase {
    /**
     * The name of a system property for setting whatever the tests should provide verbose output.
     * If this property is set to {@code true}, then the {@link #out} field will be set to a
     * non-null value:
     */
    private static final String VERBOSE_KEY = "org.apache.sis.test.verbose";

    /**
     * The name of a system property for setting the encoding of test output.
     * This property is used only if the {@link #VERBOSE_KEY} property is set
     * to "{@code true}". If this property is not set, then the system encoding
     * will be used.
     */
    private static final String ENCODING_KEY = "org.apache.sis.test.encoding";

    /**
     * If verbose outputs are enabled, the output writer where to print.
     * Otherwise {@code null}.
     * <p>
     * This field will be assigned a non-null value if the {@value #VERBOSE_KEY}
     * {@linkplain System#getProperties() system property} is set to {@code true}.
     * The encoding will by the system-default, unless the {@value #ENCODING_KEY}
     * system property has been set to a different value.
     */
    public static final PrintWriter out;

    /**
     * The buffer which is backing the {@linkplain #out} stream, or {@code null} if none.
     */
    private static final StringWriter buffer;

    /**
     * Sets the {@link #out} writer and its underlying {@link #buffer}.
     */
    static {
        if (Boolean.getBoolean(VERBOSE_KEY)) {
            out = new PrintWriter(buffer = new StringWriter());
        } else {
            buffer = null;
            out = null;
        }
    }

    /**
     * Sets the encoding of the console logging handler, if an encoding has been specified.
     * Note that we look specifically for {@link ConsoleHandler}; we do not generalize to
     * {@link StreamHandler} because the log files may not be intended for being show in
     * the console.
     * <p>
     * In case of failure to use the given encoding, we will just print a short error
     * message and left the encoding unchanged.
     */
    static {
        final String encoding = System.getProperty(ENCODING_KEY);
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
     * Date parser and formatter using the {@code "yyyy-MM-dd HH:mm:ss"} pattern
     * and UTC time zone.
     */
    private static final DateFormat dateFormat;
    static {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormat.setLenient(false);
    };

    /**
     * Creates a new test case.
     */
    protected TestCase() {
    }

    /**
     * Parses the date for the given string using the {@code "yyyy-MM-dd HH:mm:ss"} pattern
     * in UTC timezone.
     *
     * @param  date The date as a {@link String}.
     * @return The date as a {@link Date}.
     */
    public static Date date(final String date) {
        ArgumentChecks.ensureNonNull("date", date);
        try {
            synchronized (dateFormat) {
                return dateFormat.parse(date);
            }
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Formats the given date using the {@code "yyyy-MM-dd HH:mm:ss"} pattern in UTC timezone.
     *
     * @param  date The date to format.
     * @return The date as a {@link String}.
     */
    public static String format(final Date date) {
        ArgumentChecks.ensureNonNull("date", date);
        synchronized (dateFormat) {
            return dateFormat.format(date);
        }
    }

    /**
     * If verbose output is enabled, flushes the {@link #out} stream to the
     * {@linkplain System#console() console} after every tests in the class
     * have been run.
     * <p>
     * This method is invoked automatically by JUnit and doesn't need to be invoked
     * explicitely, unless the developer wants to flush the output at some specific
     * point.
     */
    @AfterClass
    public static void flushVerboseOutput() {
        System.out.flush();
        System.err.flush();
        if (out == null) {
            return;
        }
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
            final String encoding = System.getProperty(ENCODING_KEY);
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
