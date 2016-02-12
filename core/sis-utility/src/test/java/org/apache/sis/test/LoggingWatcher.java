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
import java.util.logging.Filter;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import static org.junit.Assert.*;


/**
 * Watches the logs sent to the given logger.
 * For using, create a rule in the JUnit test class like below:
 *
 * {@preformat java
 *     &#64;Rule
 *     public final LoggingWatcher loggings = new LoggingWatcher(Logging.getLogger(Loggers.XML));
 * }
 *
 * In every tests that do not expect loggings, invoke the following method last:
 *
 * {@preformat java
 *     loggings.assertNoUnexpectedLogging(0);
 * }
 *
 * In tests that are expected to emit warnings, add the following lines
 * (replace 1 by a higher value if more than one logging is expected):
 *
 * {@preformat java
 *     // Do the test here.
 *     loggings.assertLoggingContains(0, "Some keywords that are expected to be found in the message");
 *     loggings.assertNoUnexpectedLogging(1);
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
public final strictfp class LoggingWatcher extends TestWatcher implements Filter {
    /**
     * The logged messages.
     */
    public final List<String> messages = new ArrayList<>();

    /**
     * The logger to watch.
     */
    @SuppressWarnings("NonConstantLogger")
    private final Logger logger;

    /**
     * The formatter to use for formatting log messages.
     */
    private final SimpleFormatter formatter = new SimpleFormatter();

    /**
     * Creates a new watcher for the given logger.
     *
     * @param logger The logger to watch.
     */
    public LoggingWatcher(final Logger logger) {
        this.logger = logger;
    }

    /**
     * Creates a new watcher for the given logger.
     *
     * @param logger The name of logger to watch.
     */
    public LoggingWatcher(final String logger) {
        this.logger = Logger.getLogger(logger);
    }

    /**
     * Invoked when a test is about to start. This method installs this {@link Filter}
     * for the log messages before the tests are run. This installation will cause the
     * {@link #isLoggable(LogRecord)} method to be invoked when a message is logged.
     *
     * @param description A description of the JUnit test which is starting.
     *
     * @see #isLoggable(LogRecord)
     */
    @Override
    protected final void starting(final Description description) {
        assertNull(logger.getFilter());
        logger.setFilter(this);
    }

    /**
     * Invoked when a test method finishes (whether passing or failing)
     * This method removes the filter which had been set for testing purpose.
     *
     * @param description A description of the JUnit test that finished.
     */
    @Override
    protected final void finished(final Description description) {
        logger.setFilter(null);
    }

    /**
     * Invoked (indirectly) when a tested method has emitted a log message.
     * This method adds the logging message to the {@link #messages} list.
     */
    @Override
    public final boolean isLoggable(final LogRecord record) {
        messages.add(formatter.formatMessage(record));
        return TestCase.VERBOSE;
    }

    /**
     * Verifies that a logging message exists at the given index and contains the given keywords.
     *
     * @param index    Index of the logging message to verify.
     * @param keywords The keywords that we are expected to find in the logging message.
     */
    public void assertLoggingContains(final int index, final String... keywords) {
        final int size = messages.size();
        if (index >= size) {
            fail("Expected at least " + (index + 1) + " logging messages but got " + size);
        }
        final String message = messages.get(index);
        for (final String word : keywords) {
            if (!message.contains(word)) {
                fail("Expected the logging message to contains the “"+ word + "” word but got:\n" + message);
            }
        }
    }

    /**
     * Verifies that no more than {@code maxCount} messages have been logged.
     *
     * @param maxCount The maximum number of logging messages.
     */
    public void assertNoUnexpectedLogging(final int maxCount) {
        if (messages.size() > maxCount) {
            fail("Unexpected logging message: " + messages.get(maxCount));
        }
    }
}
