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
package org.apache.sis.internal.storage;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.logging.WarningListener;


/**
 * Base class of storage classes which may produce warnings. Warnings are emitted by invoking one of the
 * {@code warning(…)} methods and encapsulated in {@link LogRecord} instances. All warnings ultimately
 * go through the {@link #sendWarning(LogRecord)} method, thus providing a single method to override if
 * some additional handling is needed. When a warning is emitted, there is a choice:
 *
 * <ul>
 *   <li>If this {@code WarningProducer} is part of a larger process represented by an other {@code WarningProducer}
 *       instance (i.e. if this instance has a non-null {@link #sink}), then the {@link #sendWarning(LogRecord)}
 *       method will delegate its work to that other {@code WarningProducer} instance (the sink).</li>
 *
 *   <li>Otherwise, if there is any {@link WarningListener}, then those listeners are notified and the warning is
 *       <strong>not</strong> logged. This case is actually implemented by the {@link WarningConsumer} subclass.</li>
 *
 *   <li>Otherwise the warning is logged to the logger given to the {@link WarningConsumer} constructor.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.08)
 * @version 0.3
 * @module
 */
public class WarningProducer {
    /**
     * Where to send the warnings, or {@code null} if none. This field is always {@code null} for
     * {@link WarningConsumer}, since the later will redirect warnings to the listeners (if any).
     */
    public final WarningProducer sink;

    /**
     * Creates a new instance which will send the warnings to the given object.
     *
     * @param sink Where to send the warnings, or {@code null} if none.
     */
    protected WarningProducer(final WarningProducer sink) {
        this.sink = sink;
    }

    /**
     * Reports a warning represented by the given log record. The default implementation delegates to the
     * {@link #sink} if any, or logs the message to a default logger otherwise. The {@link WarningConsumer}
     * subclass overrides this method in order to notify listeners or use a different logger.
     *
     * @param record The warning as a log record.
     */
    void sendWarning(final LogRecord record) {
        if (sink != null) {
            sink.sendWarning(record);
        } else {
            record.setLoggerName("org.apache.sis.storage");
            Logging.getLogger("org.apache.sis.storage").log(record);
        }
    }

    /**
     * Reports a warning represented by the given message.
     *
     * @param methodName The name of the method in which the warning occurred.
     * @param message    The message to log.
     */
    protected final void warning(final String methodName, final String message) {
        final LogRecord record = new LogRecord(Level.WARNING, message);
        record.setSourceClassName(getClass().getCanonicalName());
        record.setSourceMethodName(methodName);
        sendWarning(record);
    }

    /**
     * Reports a warning represented by the given exception.
     *
     * @param methodName The name of the method in which the warning occurred.
     * @param exception  The exception to log.
     */
    protected final void warning(final String methodName, final Exception exception) {
        warning(methodName, Exceptions.formatChainedMessages(null, null, exception));
    }
}
