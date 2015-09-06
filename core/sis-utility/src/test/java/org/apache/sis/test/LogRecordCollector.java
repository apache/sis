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
import java.util.logging.LogRecord;
import java.io.IOException;
import org.apache.sis.io.TableAppender;
import org.junit.runner.Description;

// Branch-specific imports
import org.apache.sis.internal.jdk7.JDK7;


/**
 * Collects who emitted logging messages during the execution of a test suite.
 * Those emitters will be reported after the test suite completion.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class LogRecordCollector extends Handler {
    /**
     * The unique instance.
     */
    static final LogRecordCollector INSTANCE = new LogRecordCollector();

    /**
     * All log records collected during the test execution.
     * Contains tuples of elements in the following order:
     * <ul>
     *   <li>Test class name</li>
     *   <li>Test method name</li>
     *   <li>Target logger</li>
     *   <li>Logging level</li>
     * </ul>
     */
    private final List<String> records = new ArrayList<String>();

    /**
     * The description of the test currently running.
     */
    private Description currentTest;

    /**
     * For the {@link TestCase} static initializer only.
     */
    private LogRecordCollector() {
        setLevel(Level.INFO);
    }

    /**
     * Sets the description of the tests which is currently running, or {@code null} if the test finished.
     */
    final void setCurrentTest(final Description description) {
        synchronized (records) {
            currentTest = description;
        }
    }

    /**
     * Invoked when an Apache SIS method emitted a warning. This method stores information about records
     * having {@link Level#INFO} or higher.
     */
    @Override
    public void publish(final LogRecord record) {
        synchronized (records) {
            String cname;
            String method;
            if (currentTest != null) {
                cname  = currentTest.getClassName();
                method = currentTest.getMethodName();
            } else {
                /*
                 * If the test does not extent TestCase, we are not notified about its execution.
                 * So we will use the stack trace. This happen mostly when execution GeoAPI tests.
                 */
                cname  = "<unknown>";
                method = "<unknown>";
                for (final StackTraceElement t : Thread.currentThread().getStackTrace()) {
                    final String c = t.getClassName();
                    if (c.startsWith("org.apache.sis.") && c.endsWith(TestSuite.CLASSNAME_SUFFIX)) {
                        cname  = c;
                        method = t.getMethodName();
                        break;
                    }
                }
            }
            records.add(cname);
            records.add(method);
            records.add(record.getLoggerName());
            records.add(record.getLevel().getLocalizedName());
        }
    }

    /**
     * If any tests has emitted log records, report them.
     */
    final void report(final Appendable out) throws IOException {
        synchronized (records) {
            final String lineSeparator = JDK7.lineSeparator();
            if (!records.isEmpty()) {
                out.append(lineSeparator)
                   .append("The following tests have logged messages at level INFO or higher:").append(lineSeparator)
                   .append("See 'org.apache.sis.test.LoggingWatcher' for information about logging during tests.").append(lineSeparator);
                final TableAppender table = new TableAppender(out);
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
}
