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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.NoSuchElementException;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * Holds a list of {@link WarningListener} instances and provides convenience methods for emitting warnings.
 * This is a helper class for {@link org.apache.sis.storage.DataStore} implementations or for other services
 * susceptible to emit warnings.
 * Observers can {@linkplain #addWarningListener can add listeners} for being notified about warnings, and
 * processes can invoke one of the {@code warning(…)} methods for emitting warnings. All warnings are given
 * to the listeners as {@link LogRecord} instances (this allows localizable messages and additional information
 * like {@linkplain LogRecord#getMillis() timestamp} and {@linkplain LogRecord#getThrown() stack trace}).
 * This {@code WarningListeners} class provides convenience methods like {@link #warning(String, Exception)},
 * which builds {@code LogRecord} from an exception or from a string, but all those {@code warning(…)} methods
 * ultimately delegate to {@link #warning(LogRecord)}, thus providing a single point that subclasses can override.
 * When a warning is emitted, the default behavior is:
 *
 * <ul>
 *   <li>If at least one {@link WarningListener} is registered,
 *       then all listeners are notified and the warning is <strong>not</strong> logged.
 *   <li>Otherwise if the value returned by {@link LogRecord#getLoggerName()} is non-null,
 *       then the warning will be logged to that named logger.</li>
 *   <li>Otherwise the warning is logged to the logger returned by {@link #getLogger()}.</li>
 * </ul>
 *
 * <div class="section">Thread safety</div>
 * The same {@code WarningListeners} instance can be safely used by many threads without synchronization
 * on the part of the caller. Subclasses should make sure that any overridden methods remain safe to call
 * from multiple threads.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @param <S>  the type of the source of warnings.
 *
 * @see WarningListener
 * @see org.apache.sis.storage.DataStore#listeners
 *
 * @since 0.3
 * @module
 *
 * @deprecated Replaced by {@link org.apache.sis.storage.event.StoreListeners}.
 */
@Deprecated
public class WarningListeners<S> implements Localized {
    /**
     * The declared source of warnings. This is not necessarily the real source,
     * but this is the source that the implementer wants to declare as public API.
     */
    private final S source;

    /**
     * The listeners, or {@code null} if none. This is a <cite>copy on write</cite> array:
     * no elements are modified once the array have been created.
     */
    private WarningListener<? super S>[] listeners;

    /**
     * Creates a new instance with initially no listener.
     * Warnings will be logger to the destination given by {@link #getLogger()},
     * unless at least one listener is {@linkplain #addWarningListener registered}.
     *
     * @param source  the declared source of warnings. This is not necessarily the real source,
     *                but this is the source that the implementer wants to declare as public API.
     */
    public WarningListeners(final S source) {
        this.source = source;
    }

    /**
     * Creates a new instance initialized with the same listeners than the given instance.
     * This constructor is useful when a {@code DataStore} or other data producer needs to
     * be duplicated for concurrency reasons.
     *
     * @param source  the declared source of warnings. This is not necessarily the real source,
     *                but this is the source that the implementer wants to declare as public API.
     * @param other   the existing instance from which to copy the listeners, or {@code null} if none.
     *
     * @since 0.8
     */
    public WarningListeners(final S source, final WarningListeners<? super S> other) {
        this(source);
        if (other != null) {
            listeners = other.listeners;
        }
    }

    /**
     * Returns the source declared source of warnings.
     * This value is specified at construction time.
     *
     * @return the declared source of warnings.
     *
     * @since 0.8
     */
    public S getSource() {
        return source;
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
     * Returns the logger where to send the warnings when no other destination is specified.
     * This logger is used when:
     *
     * <ul>
     *   <li>no listener has been {@linkplain #addWarningListener registered}, and</li>
     *   <li>the {@code LogRecord} does not {@linkplain LogRecord#getLoggerName() specify a logger}.</li>
     * </ul>
     *
     * The default implementation infers a logger name from the package name of the {@code source} object.
     * Subclasses should override this method if they can provide a more determinist logger instance,
     * typically from a static final constant.
     *
     * @return the logger where to send the warnings when there is no other destination.
     */
    public Logger getLogger() {
        return Logging.getLogger(source.getClass());
    }

    /**
     * Reports a warning represented by the given log record. The default implementation forwards
     * the given record to <strong>one</strong> of the following destinations, in preference order:
     *
     * <ol>
     *   <li><code>{@linkplain WarningListener#warningOccured WarningListener.warningOccured}(source, record)</code>
     *       on all {@linkplain #addWarningListener registered listeners} it at least one such listener exists.</li>
     *   <li><code>{@linkplain Logging#getLogger(String) Logging.getLogger}(record.{@linkplain LogRecord#getLoggerName
     *       getLoggerName()}).{@linkplain Logger#log(LogRecord) log}(record)</code> if the logger name is non-null.</li>
     *   <li><code>{@linkplain #getLogger()}.{@linkplain Logger#log(LogRecord) log}(record)</code> otherwise.</li>
     * </ol>
     *
     * @param record  the warning as a log record.
     */
    public void warning(final LogRecord record) {
        final WarningListener<? super S>[] current;
        synchronized (this) {
            current = listeners;
        }
        if (current != null) {
            for (final WarningListener<? super S> listener : current) {
                listener.warningOccured(source, record);
            }
        } else {
            final String name = record.getLoggerName();
            final Logger logger;
            if (name != null) {
                logger = Logging.getLogger(name);
            } else {
                logger = getLogger();
                record.setLoggerName(logger.getName());
            }
            if (record instanceof QuietLogRecord) {
                ((QuietLogRecord) record).clearThrown();
            }
            logger.log(record);
        }
    }

    /**
     * Reports a warning represented by the given message and exception.
     * At least one of {@code message} and {@code exception} shall be non-null.
     * If both are non-null, then the exception message will be concatenated after the given message.
     * If the exception is non-null, its stack trace will be omitted at logging time for avoiding to
     * pollute console output (keeping in mind that this method should be invoked only for non-fatal
     * warnings). See {@linkplain #warning(Level, String, Exception) below} for more explanation.
     *
     * <p>This method is a shortcut for <code>{@linkplain #warning(Level, String, Exception)
     * warning}({@linkplain Level#WARNING}, message, exception)</code>.
     *
     * @param message    the message to log, or {@code null} if none.
     * @param exception  the exception to log, or {@code null} if none.
     */
    public void warning(String message, Exception exception) {
        warning(Level.WARNING, message, exception);
    }

    /**
     * Reports a warning at the given level represented by the given message and exception.
     * At least one of {@code message} and {@code exception} shall be non-null.
     * If both are non-null, then the exception message will be concatenated after the given message.
     *
     * <div class="section">Stack trace omission</div>
     * If there is no registered listener, then the {@link #warning(LogRecord)} method will send the record to the
     * {@linkplain #getLogger() logger}, but <em>without</em> the stack trace. This is done that way because stack
     * traces consume lot of space in the logging files, while being considered implementation details in the context
     * of {@code WarningListeners} (on the assumption that the logging message provides sufficient information).
     * If the stack trace is desired, then users can either:
     * <ul>
     *   <li>invoke {@code warning(LogRecord)} directly, or</li>
     *   <li>override {@code warning(LogRecord)} and invoke {@link LogRecord#setThrown(Throwable)} explicitly, or</li>
     *   <li>register a listener which will log the record itself.</li>
     * </ul>
     *
     * @param level      the warning level.
     * @param message    the message to log, or {@code null} if none.
     * @param exception  the exception to log, or {@code null} if none.
     */
    public void warning(final Level level, String message, final Exception exception) {
        ArgumentChecks.ensureNonNull("level", level);
        final LogRecord record;
        final StackTraceElement[] trace;
        if (exception != null) {
            trace = exception.getStackTrace();
            message = Exceptions.formatChainedMessages(getLocale(), message, exception);
            if (message == null) {
                message = exception.toString();
            }
            record = new QuietLogRecord(level, message, exception);
        } else {
            ArgumentChecks.ensureNonEmpty("message", message);
            trace = Thread.currentThread().getStackTrace();
            record = new LogRecord(level, message);
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
     * We do not document this feature in public Javadoc because it is based on heuristic rules that may change.
     *
     * <p>The current implementation compares the class name against a hard-coded list of classes to hide.
     * This implementation may change in any future SIS version.</p>
     *
     * @param  e  a stack trace element.
     * @return {@code true} if the class and method specified by the given element can be considered public API.
     */
    static boolean isPublic(final StackTraceElement e) {
        final String classname = e.getClassName();
        if (classname.startsWith("java") || classname.startsWith(Modules.INTERNAL_CLASSNAME_PREFIX) ||
            classname.indexOf('$') >= 0 || e.getMethodName().indexOf('$') >= 0)
        {
            return false;
        }
        if (classname.startsWith(Modules.CLASSNAME_PREFIX + "util.logging.")) {
            return classname.endsWith("Test");      // Consider JUnit tests as public.
        }
        return true;    // TODO: with StackWalker on JDK9, check if the class is public.
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
     * @param  listener  the listener to add.
     * @throws IllegalArgumentException if the given listener is already registered.
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
     * @param  listener  the listener to remove.
     * @throws NoSuchElementException if the given listener is not registered.
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
     * Returns all registered warning listeners, or an empty list if none.
     * This method returns an unmodifiable snapshot of the listener list at the time this method is invoked.
     *
     * @return immutable list of all registered warning listeners.
     *
     * @since 0.8
     */
    public List<WarningListener<? super S>> getListeners() {
        final WarningListener<? super S>[] current;
        synchronized (this) {
            current = listeners;
        }
        return (current != null) ? UnmodifiableArrayList.wrap(current) : Collections.emptyList();
    }

}
