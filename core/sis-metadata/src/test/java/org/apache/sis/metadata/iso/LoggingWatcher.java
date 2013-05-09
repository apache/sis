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
package org.apache.sis.metadata.iso;

import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.apache.sis.test.TestCase;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;

import static org.junit.Assert.*;


/**
 * Watches the logs sent to {@link ISOMetadata#LOGGER}. Logs will be allowed only if the test
 * was expected to cause some logging events to occur, otherwise a test failure will occurs.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public strictfp class LoggingWatcher extends TestWatchman implements Filter {
    /**
     * The maximal number of logs expected by the test. If this value is positive, then it is
     * decremented when {@link #isLoggable(LogRecord)} is invoked until the value reach zero.
     * If the value is zero and {@code isLoggable(LogRecord)} is invoked, then a test failure
     * occurs.
     *
     * <p>The initial value of this field is 0. Test cases shall set this field to a non-zero
     * value in order to allow log events.</p>
     */
    public int maximumLogCount;

    /**
     * The formatter to use for formatting log messages.
     */
    private final SimpleFormatter formatter = new SimpleFormatter();

    /**
     * Creates a new watcher.
     */
    public LoggingWatcher() {
    }

    /**
     * Invoked when a test is about to start. This method installs this {@link Filter}
     * for the log messages before the tests are run. This installation will cause the
     * {@link #isLoggable(LogRecord)} method to be invoked when a message is logged.
     *
     * @see #isLoggable(LogRecord)
     */
    @Override
    public final void starting(final FrameworkMethod method) {
        assertNull(ISOMetadata.LOGGER.getFilter());
        ISOMetadata.LOGGER.setFilter(this);
        maximumLogCount = 0;
    }

    /**
     * Invoked when a test method finishes (whether passing or failing)
     * This method removes the filter which had been set for testing purpose.
     */
    @Override
    public final void finished(final FrameworkMethod method) {
        ISOMetadata.LOGGER.setFilter(null);
    }

    /**
     * Invoked (indirectly) when a tested method has emitted a log message. This method verifies
     * if we were expecting a log message, then decrements the {@link #maximumLogCount} value.
     */
    @Override
    public final boolean isLoggable(final LogRecord record) {
        if (maximumLogCount <= 0) {
            fail("Unexpected logging:\n" + formatter.format(record));
        }
        maximumLogCount--;
        verifyMessage(formatter.formatMessage(record));
        return TestCase.verbose;
    }

    /**
     * Invoked by {@link #isLoggable(LogRecord)} when a tested method has emitted a log message.
     * The default implementation does nothing. Subclasses can override this method in order to
     * perform additional check.
     *
     * @param message The logging message.
     */
    protected void verifyMessage(final String message) {
    }
}
