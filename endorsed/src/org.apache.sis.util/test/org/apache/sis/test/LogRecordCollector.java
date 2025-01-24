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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.ConsoleHandler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import org.apache.sis.system.Loggers;
import org.apache.sis.system.Modules;
import org.apache.sis.io.TableAppender;
import org.apache.sis.util.logging.Logging;

// Test dependencies
import org.junit.jupiter.api.extension.ExtensionContext;


/**
 * Collects who emitted logging messages during the execution of a test suite.
 * Those emitters will be reported after the test suite completion.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class LogRecordCollector extends Handler implements Runnable {
    /**
     * Expected suffix in name of test classes.
     */
    private static final String CLASSNAME_SUFFIX = "Test";

    /**
     * The parent loggers to observe.
     * The need to be retained by strong references.
     */
    private static final Logger[] LOGGERS = {
        Logger.getLogger(Loggers.ROOT),
        Logger.getLogger("ucar.nc2")
    };

    /**
     * The unique instance.
     */
    static final LogRecordCollector INSTANCE = new LogRecordCollector();

    /**
     * Sets the encoding of the console logging handler, if an encoding has been specified.
     * Note that we look specifically for {@link ConsoleHandler}, we do not generalize to
     * {@link StreamHandler} because the log files may not be intended for being shown in
     * the console.
     *
     * <p>In case of failure to use the given encoding, we will just print a short error
     * message and left the encoding unchanged.</p>
     */
    static {
        final String encoding = System.getProperty(TestConfiguration.OUTPUT_ENCODING_KEY);
        if (encoding != null) {
            for (Logger logger : LOGGERS) try {
                while (logger != null) {
                    for (final Handler handler : logger.getHandlers()) {
                        if (handler instanceof ConsoleHandler) {
                            handler.setEncoding(encoding);
                        }
                    }
                    if (!logger.getUseParentHandlers()) {
                        break;
                    }
                    logger = logger.getParent();
                }
            } catch (UnsupportedEncodingException e) {
                Logging.recoverableException(logger, LogRecordCollector.class, "<clinit>", e);
                break;
            }
        }
        for (Logger logger : LOGGERS) {
            logger.addHandler(INSTANCE);
        }
    }

    /**
     * All log records collected during the test execution.
     * Contains tuples of elements in the following order:
     * <ul>
     *   <li>Test class name</li>
     *   <li>Test method name</li>
     *   <li>Target logger</li>
     *   <li>Logging level</li>
     * </ul>
     *
     * All accesses to this list must be synchronized on {@code records}.
     */
    private final List<String> records = new ArrayList<>();

    /**
     * The description of the test currently running.
     */
    private final ThreadLocal<ExtensionContext> currentTest;

    /**
     * Creates a new log record collector.
     */
    private LogRecordCollector() {
        setLevel(Level.INFO);
        currentTest = new ThreadLocal<>();
    }

    /**
     * Sets the description of the tests which is currently running, or {@code null} if the test finished.
     */
    final void setCurrentTest(final ExtensionContext description) {
        if (description != null) {
            currentTest.set(description);
        } else {
            currentTest.remove();
        }
    }

    /**
     * Invoked when an Apache SIS method emitted a warning.
     * This method stores information about records having {@link Level#INFO} or higher.
     */
    @Override
    public void publish(final LogRecord record) {
        String cname;
        String method;
        ExtensionContext source = currentTest.get();
        if (source != null) {
            cname  = source.getRequiredTestClass().getCanonicalName();
            method = source.getRequiredTestMethod().getName();
        } else {
            /*
             * If the test does not extent TestCase, we are not notified about its execution.
             * So we will use the stack trace. This happen mostly when execution GeoAPI tests.
             */
            cname  = "<unknown>";
            method = "<unknown>";
            for (final StackTraceElement t : Thread.currentThread().getStackTrace()) {
                final String c = t.getClassName();
                if (c.startsWith(Modules.CLASSNAME_PREFIX) && c.endsWith(CLASSNAME_SUFFIX)) {
                    cname  = c;
                    method = t.getMethodName();
                    break;
                }
            }
        }
        synchronized (records) {
            if (records.isEmpty()) {
                Runtime.getRuntime().addShutdownHook(new Thread(this));
            }
            records.add(cname);
            records.add(method);
            records.add(record.getLoggerName());
            records.add(record.getLevel().getLocalizedName());
        }
    }

    /**
     * If any tests has emitted log records, report them.
     *
     * @param  out  where to print the report.
     * @throws IOException if an error occurred while writing to {@code out}.
     */
    private void report(final Appendable out) throws IOException {
        final String lineSeparator = System.lineSeparator();
        synchronized (records) {
            if (!records.isEmpty()) {
                out.append(lineSeparator)
                   .append("The following tests have logged messages at level INFO or higher:").append(lineSeparator)
                   .append("See 'org.apache.sis.test.LoggingWatcher' for information about logging during tests.").append(lineSeparator);
                final var table = new TableAppender(out);
                table.setMultiLinesCells(false);
                table.nextLine('═');
                table.append("Test class");    table.nextColumn();
                table.append("Test method");   table.nextColumn();
                table.append("Target logger"); table.nextColumn();
                table.append("Level");
                table.appendHorizontalSeparator();
                final Iterator<String> it = records.iterator();
                do {
                    table.append(it.next()); table.nextColumn();    // Class
                    table.append(it.next()); table.nextColumn();    // Method
                    table.append(it.next()); table.nextColumn();    // Logger
                    table.append(it.next()); table.nextLine();      // Level
                } while (it.hasNext());
                table.nextLine('═');
                table.flush();
                records.clear();
            }
        }
    }

    /**
     * Do nothing since there is nothing to flush.
     */
    @Override
    public void flush() {
    }

    /**
     * Do nothing.
     */
    @Override
    public void close() {
    }

    /**
     * If some tests in the class emitted unexpected log records,
     * prints a table showing which tests caused logging.
     *
     * <p>Note: this was used to be a JUnit {@code afterAll(ExtensionContext)} method.
     * But we want this method to be executed after all tests in the project,
     * not after each class.</p>
     */
    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public final void run() {
        try {
            LogRecordCollector.INSTANCE.report(System.err);     // Same stream as logging console handler.
        } catch (IOException e) {
            throw new UncheckedIOException(e);                  // Should never happen.
        }
    }
}
