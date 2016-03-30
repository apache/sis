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

import java.util.Queue;
import java.util.LinkedList;
import java.util.ConcurrentModificationException;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import static org.junit.Assert.*;

// Branch-specific imports
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;


/**
 * Watches the logs sent to the given logger.
 * For using, create a rule in the JUnit test class like below:
 *
 * {@preformat java
 *     &#64;Rule
 *     public final LoggingWatcher loggings = new LoggingWatcher(Logging.getLogger(Loggers.XML));
 * }
 *
 * Recommended but not mandatory, ensure that there is no unexpected logging in any tests:
 *
 * {@preformat java
 *     &#64;After
 *     public void assertNoUnexpectedLog() {
 *         loggings.assertNoUnexpectedLog();
 *     }
 * }
 *
 * In tests that are expected to emit warnings, add the following lines:
 *
 * {@preformat java
 *     // Do the test here.
 *     loggings.assertNextLogContains("Some keywords", "that are expected", "to be found in the message");
 *     loggings.assertNoUnexpectedLog();
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
public final strictfp class LoggingWatcher extends TestWatchman implements Filter {
    /**
     * The logged messages.
     */
    private final Queue<String> messages = new LinkedList<String>();

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
    public final void starting(final FrameworkMethod description) {
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
    public final void finished(final FrameworkMethod description) {
        logger.setFilter(null);
    }

    /**
     * Invoked (indirectly) when a tested method has emitted a log message.
     * This method adds the logging message to the {@link #messages} list.
     */
    @Override
    public final boolean isLoggable(final LogRecord record) {
        if (record.getLevel().intValue() >= Level.INFO.intValue()) {
            messages.add(formatter.formatMessage(record));
        }
        return TestCase.VERBOSE;
    }

    /**
     * Skips the next log messages if it contains all the given keywords.
     * This method is used instead of {@link #assertNextLogContains(String...)} when a log message may or
     * may not be emitted during a test, depending on circumstances that the test method does not control.
     *
     * @param keywords The keywords that are expected to exist in the next log message
     *        if that log message has been emitted.
     */
    public void skipNextLogIfContains(final String... keywords) {
        final String message = messages.peek();
        if (message != null) {
            for (final String word : keywords) {
                if (!message.contains(word)) {
                    return;
                }
            }
            if (messages.remove() != message) {
                throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Verifies that the next logging message contains the given keywords.
     * Each call of this method advances to the next log message.
     *
     * @param keywords The keywords that are expected to exist in the next log message.
     */
    public void assertNextLogContains(final String... keywords) {
        if (messages.isEmpty()) {
            fail("Expected a logging messages but got no more.");
        }
        final String message = messages.remove();
        for (final String word : keywords) {
            if (!message.contains(word)) {
                fail("Expected the logging message to contains the “"+ word + "” word but got:\n" + message);
            }
        }
    }

    /**
     * Verifies that there is no more log message.
     */
    public void assertNoUnexpectedLog() {
        final String message = messages.peek();
        if (message != null) {
            fail("Unexpected logging message: " + message);
        }
    }

    /**
     * Discards all logging messages.
     */
    public void clear() {
        messages.clear();
    }
}
