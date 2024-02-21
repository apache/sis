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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.extension.RegisterExtension;


/**
 * Base class of Apache SIS tests (except the ones that extend GeoAPI tests) that may emit logs.
 * This is a convenience class applying the JUnit extension documented in {@link LoggingWatcher}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@ResourceLock(value=LoggingWatcher.LOCK, mode=ResourceAccessMode.READ)
public abstract class TestCaseWithLogs extends TestCase {
    /**
     * A JUnit extension for listening to log events.
     */
    @RegisterExtension
    public final LoggingWatcher loggings;

    /**
     * Creates a new test case which will listen to logs emitted by the given logger.
     *
     * @param  logger  the logger to listen to.
     */
    protected TestCaseWithLogs(final Logger logger) {
        loggings = new LoggingWatcher(logger);
    }

    /**
     * Creates a new test case which will listen to logs emitted by the logger of the given name.
     *
     * @param logger  name of the logger to listen.
     */
    protected TestCaseWithLogs(final String logger) {
        loggings = new LoggingWatcher(logger);
    }

    /**
     * Base class of Apache SIS tests in which many workers may emit logs.
     * Such tests should be executed in isolation of other tests that may emit logs.
     */
    @Execution(value=ExecutionMode.SAME_THREAD, reason="For verification of logs emitted by worker threads.")
    @ResourceLock(value=LoggingWatcher.LOCK, mode=ResourceAccessMode.READ_WRITE)
    public static abstract class Isolated extends TestCaseWithLogs {
        /**
         * Creates a new test case which will listen to logs emitted by the logger of the given name.
         *
         * @param logger  name of the logger to listen.
         */
        protected Isolated(final String logger) {
            super(logger);
            loggings.setMultiThread();
        }
    }
}
