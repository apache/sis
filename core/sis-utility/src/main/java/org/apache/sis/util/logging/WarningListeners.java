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

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.NoSuchElementException;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Holds a list of {@link WarningListener} instances and provides convenience methods for emitting warnings.
 * The convenience {@code warning(…)} methods can build {@code LogRecord} from an exception or from a string.
 *
 * <p>In the default implementation, all {@code warning(…)} methods delegate to {@link #warning(LogRecord)},
 * thus providing a single point that subclasses can override for intercepting all warnings.
 * The default behavior is:</p>
 *
 * <ul>
 *   <li>If at least one {@link WarningListener} is registered,
 *       then all listeners are notified and the warning is <strong>not</strong> logged.
 *   <li>Otherwise the warning is logged to the logger returned by {@link #getLogger()}.</li>
 * </ul>
 *
 * <div class="section">Thread safety</div>
 * The same {@code WarningListeners} instance can be safely used by many threads without synchronization
 * on the part of the caller. Subclasses should make sure that any overridden methods remain safe to call
 * from multiple threads.
 *
 * @param <S> The type of the source of warnings.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see WarningListener
 * @see org.apache.sis.storage.DataStore#listeners
 */
public class WarningListeners<S> implements Localized {
    /**
     * The declared source of warnings. This is not necessarily the real source,
     * but this is the source that the implementor wants to declare as public API.
     */
    private final S source;

    /**
     * The listeners, or {@code null} if none. This is a <cite>copy on write</cite> array:
     * no elements are modified once the array have been created.
     */
    private WarningListener<? super S>[] listeners;

    /**
     * Creates a new instance without source. This constructor is for {@link EmptyWarningListeners}
     * usage only, because it requires some method to be overloaded.
     */
    WarningListeners() {
        source = null;
    }

    /**
     * Creates a new instance with initially no listener.
     * Warnings will be logger to the given logger, unless at least one listener is registered.
     *
     * @param source The declared source of warnings. This is not necessarily the real source,
     *               but this is the source that the implementor wants to declare as public API.
     */
    public WarningListeners(final S source) {
        ArgumentChecks.ensureNonNull("source", source);
        this.source = source;
    }

    /**
     * The locale to use for formatting warning messages, or {@code null} for the default locale.
     * If the {@code source} object given to the constructor implements the {@link Localized} interface,
     * then this method delegates to its {@code getLocale()} method. Otherwise this method returns {@code null}.
     */
    @Override
    public Locale getLocale() {
        return (source instanceof Localized) ? ((Localized) source).getLocale() : null;
    }

    /**
     * Returns the logger where to send the warnings. The default implementation returns a logger for
     * the package name of the {@code source} object. Subclasses should override this method if they
     * can provide a fixed logger instance (typically a static final constant).
     *
     * @return The logger where to send the warnings when there is no registered listeners.
     */
    public Logger getLogger() {
        return Logging.getLogger(source.getClass());
    }

    /**
     * Reports a warning represented by the given log record. The default implementation notifies the listeners
     * if any, or logs the message to the logger returned by {@link #getLogger()} otherwise.
     *
     * @param record The warning as a log record.
     */
    public void warning(final LogRecord record) {
        final WarningListener<?>[] current;
        synchronized (this) {
            current = listeners;
        }
        if (current != null) {
            for (final WarningListener<? super S> listener : listeners) {
                listener.warningOccured(source, record);
            }
        } else {
            final Logger logger = getLogger();
            record.setLoggerName(logger.getName());
            if (record instanceof QuietLogRecord) {
                ((QuietLogRecord) record).clearThrown();
            }
            logger.log(record);
        }
    }

    /**
     * Reports a warning represented by the given message and exception.
     * At least one of {@code message} and {@code exception} shall be non-null.
     *
     * <div class="section">Stack trace omission</div>
     * If there is no registered listener, then the {@link #warning(LogRecord)} method will send the record to the
     * {@linkplain #getLogger() logger}, but <em>without</em> the stack trace. This is done that way because stack
     * traces consume lot of space in the logging files, while being considered implementation details in the context
     * of {@code WarningListeners} (on the assumption that the logging message provides sufficient information).
     * If the stack trace is desired, then users can either:
     * <ul>
     *   <li>invoke {@code warning(LogRecord)} directly, or</li>
     *   <li>override {@code warning(LogRecord)} and invoke {@link LogRecord#setThrown(Throwable)} explicitely, or</li>
     *   <li>register a listener which will log the record itself.</li>
     * </ul>
     *
     * @param message    The message to log, or {@code null} if none.
     * @param exception  The exception to log, or {@code null} if none.
     */
    public void warning(String message, final Exception exception) {
        final LogRecord record;
        final StackTraceElement[] trace;
        if (exception != null) {
            trace = exception.getStackTrace();
            message = Exceptions.formatChainedMessages(getLocale(), message, exception);
            if (message == null) {
                message = exception.toString();
            }
            record = new QuietLogRecord(message, exception);
        } else {
            ArgumentChecks.ensureNonEmpty("message", message);
            trace = Thread.currentThread().getStackTrace();
            record = new LogRecord(Level.WARNING, message);
        }
        for (final StackTraceElement e : trace) {
            if (isPublic(e)) {
                record.setSourceClassName(e.getClassName());
                record.setSourceMethodName(e.getMethodName());
                break;
            }
        }
        warning(record);
    }

    /**
     * Returns {@code true} if the given stack trace element describes a method considered part of public API.
     * This method is invoked in order to infer the class and method names to declare in a {@link LogRecord}.
     *
     * <p>The current implementation compares the class name against a hard-coded list of classes to hide.
     * This implementation may change in any future SIS version.</p>
     *
     * @param  e A stack trace element.
     * @return {@code true} if the class and method specified by the given element can be considered public API.
     */
    private static boolean isPublic(final StackTraceElement e) {
        final String classname  = e.getClassName();
        return !classname.equals("org.apache.sis.util.logging.WarningListeners") &&
               !classname.contains(".internal.") && !classname.startsWith("java") &&
                classname.indexOf('$') < 0 && e.getMethodName().indexOf('$') < 0;
    }

    /**
     * Adds a listener to be notified when a warning occurred.
     * When a warning occurs, there is a choice:
     *
     * <ul>
     *   <li>If this object has no warning listener, then the warning is logged at
     *       {@link java.util.logging.Level#WARNING}.</li>
     *   <li>If this object has at least one warning listener, then all listeners are notified
     *       and the warning is <strong>not</strong> logged by this object.</li>
     * </ul>
     *
     * @param  listener The listener to add.
     * @throws IllegalArgumentException If the given listener is already registered.
     */
    public synchronized void addWarningListener(final WarningListener<? super S> listener)
            throws IllegalArgumentException
    {
        ArgumentChecks.ensureNonNull("listener", listener);
        final WarningListener<? super S>[] current = listeners;
        final int length = (current != null) ? current.length : 0;

        @SuppressWarnings({"unchecked", "rawtypes"}) // Generic array creation.
        final WarningListener<? super S>[] copy = new WarningListener[length + 1];
        for (int i=0; i<length; i++) {
            final WarningListener<? super S> c = current[i];
            if (c == listener) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, listener));
            }
            copy[i] = c;
        }
        copy[length] = listener;
        listeners = copy;
    }

    /**
     * Removes a previously registered listener.
     *
     * @param  listener The listener to remove.
     * @throws NoSuchElementException If the given listener is not registered.
     */
    public synchronized void removeWarningListener(final WarningListener<? super S> listener)
            throws NoSuchElementException
    {
        ArgumentChecks.ensureNonNull("listener", listener);
        final WarningListener<? super S>[] current = listeners;
        if (current != null) {
            for (int i=0; i<current.length; i++) {
                if (current[i] == listener) {
                    if (current.length == 1) {
                        listeners = null;
                    } else {
                        @SuppressWarnings({"unchecked", "rawtypes"}) // Generic array creation.
                        final WarningListener<? super S>[] copy = new WarningListener[current.length - 1];
                        System.arraycopy(current, 0, copy, 0, i);
                        System.arraycopy(current, i+1, copy, i, copy.length - i);
                        listeners = copy;
                    }
                    return;
                }
            }
        }
        throw new NoSuchElementException(Errors.format(Errors.Keys.ElementNotFound_1, listener));
    }

    /**
     * Returns {@code true} if this object contains at least one listener.
     *
     * @return {@code true} if this object contains at least one listener, {@code false} otherwise.
     *
     * @since 0.4
     */
    public synchronized boolean hasListeners() {
        return listeners != null;
    }
}
