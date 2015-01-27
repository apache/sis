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
package org.apache.sis.util.logging;

import java.util.logging.Logger;


/**
 * Redirects logging to two loggers. This is used only when more than one {@link LoggerFactory}
 * is found on the classpath. This should never happen, but if it happen anyway we will send the
 * log records to all registered loggers in order to have a behavior slightly more determinist
 * than picking an arbitrary logger.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class DualLoggerFactory extends LoggerFactory<DualLogger> {
    /**
     * The factories of loggers on which to delegate logging events.
     */
    private final LoggerFactory<?> first, second;

    /**
     * Creates a new factory which will delegate the logging events to the loggers
     * created by the two given factories.
     */
    DualLoggerFactory(final LoggerFactory<?> first, final LoggerFactory<?> second) {
        super(DualLogger.class);
        this.first  = first;
        this.second = second;
    }

    /**
     * Returns a comma-separated list of the logging frameworks.
     */
    @Override
    public String getName() {
        return first.getName() + ", " + second.getName();
    }

    /**
     * Returns the implementation to use for the logger of the specified name,
     * or {@code null} if the logger would delegates to Java logging anyway.
     */
    @Override
    protected DualLogger getImplementation(final String name) {
        return new DualLogger(name, first.getLogger(name), second.getLogger(name));
    }

    /**
     * Wraps the specified {@linkplain #getImplementation implementation} in a Java logger.
     */
    @Override
    protected Logger wrap(final String name, final DualLogger implementation) {
        return implementation;
    }

    /**
     * Returns the {@linkplain #getImplementation implementation} wrapped by the specified logger,
     * or {@code null} if none.
     */
    @Override
    protected DualLogger unwrap(final Logger logger) {
        if (logger instanceof DualLogger) {
            return (DualLogger) logger;
        }
        return null;
    }
}
