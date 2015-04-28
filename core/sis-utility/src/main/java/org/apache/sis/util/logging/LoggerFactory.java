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
import org.apache.sis.util.collection.WeakValueHashMap;


/**
 * A factory for Java {@link Logger} wrapping an other logging framework.
 * This factory is used only when an application wants to redirect SIS logs to an other framework
 * than JDK logging. An instance of {@code LoggerFactory} can be registered to SIS in two ways:
 *
 * <ul>
 *   <li>By declaring the fully qualified classname of the {@code LoggerFactory} implementation
 *       in the {@code META-INF/services/org.apache.sis.util.logging.LoggerFactory} file.
 *       Note that the {@code sis-logging-commons.jar} and {@code sis-logging-log4j.jar}
 *       files provide such declaration.</li>
 *   <li>By explicit invocation of {@link Logging#setLoggerFactory(LoggerFactory)}
 *       at application initialization time.</li>
 * </ul>
 *
 * The {@link #getLogger(String)} method shall return some {@link Logger} subclass
 * (typically {@link LoggerAdapter}) which forwards directly all log methods to the other framework.
 *
 * <div class="section">Thread safety</div>
 * This base class is safe for multi-threads usage. Subclasses registered in {@code META-INF/services/}
 * shall make sure that any overridden methods remain safe to call from multiple threads.
 *
 * @param <L> The type of loggers used for the implementation backend.
 *            This is the type used by external frameworks like Log4J.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see Logging
 * @see LoggerAdapter
 */
public abstract class LoggerFactory<L> {
    /**
     * The logger class. We ask for this information right at construction time in order to
     * force a {@link NoClassDefFoundError} early rather than only the first time a message
     * is logged.
     */
    private final Class<L> loggerClass;

    /**
     * The loggers created up to date.
     */
    private final WeakValueHashMap<String,Logger> loggers;

    /**
     * Creates a new factory.
     *
     * @param loggerClass The class of the wrapped logger.
     */
    protected LoggerFactory(final Class<L> loggerClass) {
        this.loggerClass = loggerClass;
        loggers = new WeakValueHashMap<String,Logger>(String.class);
    }

    /**
     * Returns the name of the logging framework.
     *
     * @return The logging framework name.
     */
    public abstract String getName();

    /**
     * Returns the logger of the specified name, or {@code null} if the JDK logging framework
     * should be used.
     *
     * @param  name The name of the logger.
     * @return The logger, or {@code null} if the JDK logging framework should be used.
     */
    public Logger getLogger(final String name) {
        final L target = getImplementation(name);
        if (target == null) {
            return null;
        }
        synchronized (loggers) {
            Logger logger = loggers.get(name);
            if (logger == null || !target.equals(unwrap(logger))) {
                logger = wrap(name, target);
                loggers.put(name, logger);
            }
            return logger;
        }
    }

    /**
     * Returns the base class of objects to be returned by {@link #getImplementation(String)}.
     * The class depends on the underlying logging framework (Log4J, SLF4J, <i>etc.</i>).
     *
     * @return The type of loggers used for the implementation backend.
     */
    public Class<L> getImplementationClass() {
        return loggerClass;
    }

    /**
     * Returns the implementation to use for the logger of the specified name. The object to be
     * returned depends on the logging framework (Log4J, SLF4J, <i>etc.</i>). If the target
     * framework redirects logging events to JDK logging, then this method shall return
     * {@code null} since we should not use wrapper at all.
     *
     * @param  name The name of the logger.
     * @return The logger as an object of the target logging framework (Log4J, SLF4J,
     *         <i>etc.</i>), or {@code null} if the target framework would redirect
     *         to the JDK logging framework.
     */
    protected abstract L getImplementation(String name);

    /**
     * Wraps the specified {@linkplain #getImplementation(String) implementation} in a JDK logger.
     *
     * @param  name The name of the logger.
     * @param  implementation An implementation returned by {@link #getImplementation(String)}.
     * @return A new logger wrapping the specified implementation.
     */
    protected abstract Logger wrap(String name, L implementation);

    /**
     * Returns the {@linkplain #getImplementation(String) implementation} wrapped by the specified
     * logger, or {@code null} if none. If the specified logger is not an instance of the expected
     * class, then this method should returns {@code null}.
     *
     * @param  logger The logger to test.
     * @return The implementation wrapped by the specified logger, or {@code null} if none.
     */
    protected abstract L unwrap(Logger logger);
}
