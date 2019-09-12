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
package org.apache.sis.storage.event;

import java.util.Map;
import java.util.Locale;
import java.util.IdentityHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.lang.reflect.Method;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.storage.StoreResource;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.Resource;


/**
 * Holds a list of {@link StoreListener} instances and provides convenience methods for sending events.
 * This is a helper class for {@link DataStore} and {@link Resource} implementations.
 *
 * <p>Observers can {@linkplain #addListener add listeners} for being notified about events,
 * and producers can invoke one of the {@code warning(…)} and other methods for emitting events.
 *
 * <div class="section">Warning events</div>
 * All warnings are given to the listeners as {@link LogRecord} instances (this allows localizable messages
 * and additional information like {@linkplain LogRecord#getThrown() stack trace}, timestamp, <i>etc.</i>).
 * This {@code StoreListeners} class provides convenience methods like {@link #warning(String, Exception)},
 * which build {@code LogRecord} from an exception or from a string. But all those {@code warning(…)} methods
 * ultimately delegate to {@link #warning(LogRecord)}, thus providing a single point that subclasses can override.
 * When a warning is emitted, the default behavior is:
 *
 * <ul>
 *   <li>Notify all listeners registered for {@link WarningEvent} type
 *       in this {@code StoreListeners} and in the parent managers.</li>
 *   <li>If previous step found no listener registered for {@code WarningEvent},
 *       then log the warning in the first logger found in following choices:
 *     <ol>
 *       <li>The logger specified by {@link LogRecord#getLoggerName()} if non-null.</li>
 *       <li>Otherwise the logger specified by {@link org.apache.sis.storage.DataStoreProvider#getLogger()}
 *           if the provider can be found.</li>
 *       <li>Otherwise a logger whose name is the source {@link DataStore} package name.</li>
 *     </ol>
 *   </li>
 * </ul>
 *
 * <div class="section">Thread safety</div>
 * The same {@code StoreListeners} instance can be safely used by many threads without synchronization
 * on the part of the caller. Subclasses should make sure that any overridden methods remain safe to call
 * from multiple threads.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public class StoreListeners implements Localized {
    /**
     * Parent manager to notify in addition to this manager.
     */
    private final StoreListeners parent;

    /**
     * The declared source of events. This is not necessarily the real source,
     * but this is the source that the implementer wants to declare as public API.
     */
    private final Resource source;

    /**
     * The head of a chained list of listeners, or {@code null} if none.
     * Each element in this chain contains all listeners for a given even type.
     */
    private volatile ForType<?> listeners;

    /**
     * All listeners for a given even type.
     *
     * @param  <T>  the type of events of interest to the listeners.
     */
    private static final class ForType<T extends StoreEvent> {
        /**
         * The types for which listeners have been registered.
         */
        final Class<T> type;

        /**
         * The listeners for the {@linkplain #type event type}, or {@code null} if none.
         * This is a <cite>copy on write</cite> array: no elements are modified after an array has been created.
         */
        @SuppressWarnings("VolatileArrayField")
        private volatile StoreListener<? super T>[] listeners;

        /**
         * Next element in the chain of listeners. Intentionally final; if we want to remove an element
         * then we need to recreate all previous elements with new {@code next} values. We do that for
         * avoiding the need to synchronize iterations over the elements.
         */
        final ForType<?> next;

        /**
         * Creates a new element in the chained list of listeners.
         *
         * @param type  type of events of interest for listeners in this element.
         * @param next  the next element in the chained list, or {@code null} if none.
         */
        ForType(final Class<T> type, final ForType<?> next) {
            this.type = type;
            this.next = next;
        }

        /**
         * Adds the given listener to the list of listeners for this type.
         * This method does not check if the given listener was already registered;
         * it a listener is registered twice, it will need to be removed twice.
         *
         * <p>It is caller responsibility to perform synchronization and to verify that the listener is non-null.</p>
         */
        final void add(final StoreListener<? super T> listener) {
            final StoreListener<? super T>[] list = listeners;
            final int length = (list != null) ? list.length : 0;
            @SuppressWarnings({"unchecked", "rawtypes"}) // Generic array creation.
            final StoreListener<? super T>[] copy = new StoreListener[length + 1];
            if (list != null) {
                System.arraycopy(list, 0, copy, 0, length);
            }
            copy[length] = listener;
            listeners = copy;
        }

        /**
         * Removes a previously registered listener.
         * It the listener has been registered twice, only the most recent registration is removed.
         *
         * <p>It is caller responsibility to perform synchronization.</p>
         */
        final void remove(final StoreListener<? super T> listener) {
            StoreListener<? super T>[] list = listeners;
            if (list != null) {
                for (int i=list.length; --i >= 0;) {
                    if (list[i] == listener) {
                        if (list.length == 1) {
                            list = null;
                        } else {
                            list = ArraysExt.remove(list, i, 1);
                        }
                        listeners = list;
                        break;
                    }
                }
            }
        }

        /**
         * Returns {@code true} if this element has at least one listener.
         */
        final boolean hasListeners() {
            return listeners != null;
        }

        /**
         * Sends the given event to all listeners registered in this element.
         *
         * @param  event  the event to send to listeners.
         * @param  done   listeners who were already notified, for avoiding to notify them twice.
         * @return the {@code done} map, created when first needed.
         */
        final Map<StoreListener<?>,Boolean> eventOccured(final T event, Map<StoreListener<?>,Boolean> done) {
            final StoreListener<? super T>[] list = listeners;
            if (list != null) {
                if (done == null) {
                    done = new IdentityHashMap<>(list.length);
                }
                for (final StoreListener<? super T> listener : list) {
                    if (done.put(listener, Boolean.TRUE) == null) {
                        listener.eventOccured(event);
                    }
                }
            }
            return done;
        }
    }

    /**
     * Creates a new instance with the given parent and initially no listener.
     * The parent is typically the listeners of the {@link DataStore} that created a resource.
     *
     * @param parent  the manager to notify in addition to this manager, or {@code null} if none.
     * @param source  the source of events. Can not be null.
     */
    public StoreListeners(final StoreListeners parent, Resource source) {
        /*
         * Undocumented feature for allowing subclass to specify `this` as the source resource.
         * This is used as a convenience by AbstractResource internal class. We need this hack
         * because subclasses can not reference `this` before super-class constructor completed.
         */
        if (source == null && this instanceof Resource) {
            source = (Resource) this;
        } else {
            ArgumentChecks.ensureNonNull("source", source);
        }
        this.source = source;
        this.parent = parent;
    }

    /**
     * Returns the source of events. This value is specified at construction time.
     *
     * @return the source of events.
     */
    public Resource getSource() {
        return source;
    }

    /**
     * Returns the data store of the source, or {@code null} if unknown.
     */
    private static DataStore getDataStore(StoreListeners m) {
        do {
            final Resource source = m.source;
            if (source instanceof DataStore) {
                return (DataStore) source;
            }
            if (source instanceof StoreResource) {
                final DataStore ds = ((StoreResource) source).getOriginator();
                if (ds != null) return ds;
            }
            m = m.parent;
        } while (m != null);
        return null;
    }

    /**
     * Returns a short name or label for the source. It may be the name of the file opened by a data store.
     * The returned name can be useful in warning messages for identifying the problematic source.
     *
     * <p>The default implementation {@linkplain DataStore#getDisplayName() fetches that name from the data store},
     * or returns an arbitrary name if it can get it otherwise.</p>
     *
     * @return a short name of label for the source (never {@code null}).
     *
     * @see DataStore#getDisplayName()
     */
    public String getSourceName() {
        final DataStore ds = getDataStore(this);
        if (ds != null) {
            String name = ds.getDisplayName();
            if (name != null) {
                return name;
            }
            final DataStoreProvider provider = ds.getProvider();
            if (provider != null) {
                name = provider.getShortName();
                if (name != null) {
                    return name;
                }
            }
        }
        return Classes.getShortClassName(source);
    }

    /**
     * Returns the locale used by this manager, or {@code null} if unspecified.
     * That locale is typically inherited from the {@link DataStore} locale
     * and can be used for formatting messages.
     *
     * @return the locale for messages (typically specified by the data store), or {@code null} if unknown.
     *
     * @see DataStore#getLocale()
     * @see StoreEvent#getLocale()
     */
    @Override
    public Locale getLocale() {
        StoreListeners m = this;
        do {
            final Resource src = m.source;
            if (src != this && src != m && src instanceof Localized) {
                final Locale locale = ((Localized) src).getLocale();
                if (locale != null) return locale;
            }
            m = m.parent;
        } while (m != null);
        return null;
    }

    /**
     * Returns the logger where to send warnings when no other destination is specified.
     * This method tries to get the logger from {@link DataStoreProvider#getLogger()}.
     * If that logger can not be found, then this method infers a logger name from the
     * package name of the source data store. The returned logger is used when:
     *
     * <ul>
     *   <li>no listener has been {@linkplain #addListener registered} for the {@link WarningEvent} type, and</li>
     *   <li>the {@code LogRecord} does not {@linkplain LogRecord#getLoggerName() specify a logger}.</li>
     * </ul>
     *
     * @return the logger where to send the warnings when there is no other destination.
     */
    private Logger getLogger() {
        Resource src = source;
        final DataStore ds = getDataStore(this);
        if (ds != null) {
            final DataStoreProvider provider = ds.getProvider();
            if (provider != null) {
                final Logger logger = provider.getLogger();
                if (logger != null) {
                    return logger;
                }
            }
            src = ds;
        }
        return Logging.getLogger(src.getClass());
    }

    /**
     * Reports a warning described by the given message.
     *
     * <p>This method is a shortcut for <code>{@linkplain #warning(Level, String, Exception)
     * warning}({@linkplain Level#WARNING}, message, null)</code>.
     *
     * @param  message  the warning message to report.
     */
    public void warning(final String message) {
        ArgumentChecks.ensureNonNull("message", message);
        warning(Level.WARNING, message, null);
    }

    /**
     * Reports a warning described by the given exception.
     * The exception stack trace will be omitted at logging time for avoiding to pollute console output
     * (keeping in mind that this method should be invoked only for non-fatal warnings).
     * See {@linkplain #warning(Level, String, Exception) below} for more explanation.
     *
     * <p>This method is a shortcut for <code>{@linkplain #warning(Level, String, Exception)
     * warning}({@linkplain Level#WARNING}, null, exception)</code>.
     *
     * @param  exception  the exception to report.
     */
    public void warning(final Exception exception) {
        ArgumentChecks.ensureNonNull("exception", exception);
        warning(Level.WARNING, null, exception);
    }

    /**
     * Reports a warning described by the given message and exception.
     * At least one of {@code message} and {@code exception} arguments shall be non-null.
     * If both are non-null, then the exception message will be concatenated after the given message.
     * If the exception is non-null, its stack trace will be omitted at logging time for avoiding to
     * pollute console output (keeping in mind that this method should be invoked only for non-fatal
     * warnings). See {@linkplain #warning(Level, String, Exception) below} for more explanation.
     *
     * <p>This method is a shortcut for <code>{@linkplain #warning(Level, String, Exception)
     * warning}({@linkplain Level#WARNING}, message, exception)</code>.
     *
     * @param  message    the warning message to report, or {@code null} if none.
     * @param  exception  the exception to report, or {@code null} if none.
     */
    public void warning(String message, Exception exception) {
        warning(Level.WARNING, message, exception);
    }

    /**
     * Reports a warning at the given level represented by the given message and exception.
     * At least one of {@code message} and {@code exception} arguments shall be non-null.
     * If both are non-null, then the exception message will be concatenated after the given message.
     *
     * <div class="section">Stack trace omission</div>
     * If there is no registered listener for the {@link WarningEvent} type, then the {@link #warning(LogRecord)}
     * method will send the record to a logger but <em>without</em> the stack trace.
     * This is done that way because stack traces consume lot of space in the logging files, while being considered
     * implementation details in the context of {@code StoreListeners} (on the assumption that the logging message
     * provides sufficient information). If the stack trace is desired, then users can either:
     * <ul>
     *   <li>invoke {@code warning(LogRecord)} directly, or</li>
     *   <li>override {@code warning(LogRecord)} and invoke {@link LogRecord#setThrown(Throwable)} explicitly, or</li>
     *   <li>register a listener which will log the record itself.</li>
     * </ul>
     *
     * @param  level      the warning level.
     * @param  message    the message to log, or {@code null} if none.
     * @param  exception  the exception to log, or {@code null} if none.
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
            trace = Thread.currentThread().getStackTrace();         // TODO: on JDK9, use StackWalker instead.
            record = new LogRecord(level, message);
        }
        try {
            for (final StackTraceElement e : trace) {
                if (setPublicSource(record, Class.forName(e.getClassName()), e.getMethodName())) {
                    break;
                }
            }
        } catch (ClassNotFoundException | SecurityException e) {
            Logging.ignorableException(Logging.getLogger(Modules.STORAGE), StoreListeners.class, "warning", e);
        }
        warning(record);
    }

    /**
     * Eventually sets the class name and method name in the given record,
     * and returns {@code true} if the method is public resource method.
     *
     * @param  record      the record where to set the source class/method name.
     * @param  type        the source class. This method does nothing if the class is not a {@link Resource}.
     * @param  methodName  the source method.
     * @return whether the source is a public method of a {@link Resource}.
     * @throws SecurityException if this method is not allowed to get the list of public methods.
     */
    private static boolean setPublicSource(final LogRecord record, final Class<?> type, final String methodName) {
        if (Resource.class.isAssignableFrom(type)) {
            record.setSourceClassName(type.getCanonicalName());
            record.setSourceMethodName(methodName);
            for (final Method m : type.getMethods()) {          // List of public methods, ignoring parameters.
                if (methodName.equals(m.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Reports a warning described by the given log record. The default implementation forwards
     * the given record to one of the following destinations, in preference order:
     *
     * <ul>
     *   <li><code>{@linkplain StoreListener#eventOccured StoreListener.eventOccured}(new
     *       {@linkplain WarningEvent}(source, record))</code> on all listeners registered for this kind of event.</li>
     *   <li>Only if above step found no listener, then <code>{@linkplain Logging#getLogger(String)
     *       Logging.getLogger}(record.loggerName).{@linkplain Logger#log(LogRecord) log}(record)</code>
     *       where {@code loggerName} is one of the following:
     *     <ul>
     *       <li><code>record.{@linkplain LogRecord#getLoggerName() getLoggerName()}</code> if that value is non-null.</li>
     *       <li>Otherwise the value of {@link DataStoreProvider#getLogger()} if the provider is found.</li>
     *       <li>Otherwise the source {@link DataStore} package name.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param  description  warning details provided as a log record.
     */
    @SuppressWarnings("unchecked")
    public void warning(final LogRecord description) {
        if (!fire(new WarningEvent(source, description), WarningEvent.class)) {
            final String name = description.getLoggerName();
            final Logger logger;
            if (name != null) {
                logger = Logging.getLogger(name);
            } else {
                logger = getLogger();
                description.setLoggerName(logger.getName());
            }
            if (description instanceof QuietLogRecord) {
                ((QuietLogRecord) description).clearImplicitThrown();
            }
            logger.log(description);
        }
    }

    /**
     * Sends the given event to all listeners registered for the given type or for a super-type.
     * This method first notifies the listeners registered in this {@code StoreListeners}, then
     * notifies listeners registered in parent {@code StoreListeners}s. Each listener will be
     * notified only once even if it has been registered many times.
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  event      the event to fire.
     * @param  eventType  the type of events to be fired.
     * @return {@code true} if the event has been sent to at least one listener.
     */
    @SuppressWarnings("unchecked")
    public <T extends StoreEvent> boolean fire(final T event, final Class<T> eventType) {
        ArgumentChecks.ensureNonNull("event", event);
        ArgumentChecks.ensureNonNull("eventType", eventType);
        Map<StoreListener<?>,Boolean> done = null;
        StoreListeners m = this;
        do {
            for (ForType<?> e = m.listeners; e != null; e = e.next) {
                if (e.type.isAssignableFrom(eventType)) {
                    done = ((ForType<? super T>) e).eventOccured(event, done);
                }
            }
            m = m.parent;
        } while (m != null);
        return (done != null) && !done.isEmpty();
    }

    /**
     * Registers a listener to notify when the specified kind of event occurs.
     * Registering a listener for a given {@code eventType} also register the listener for all event sub-types.
     * The same listener can be registered many times, but its {@link StoreListener#eventOccured(StoreEvent)}
     * method will be invoked only once per event. This filtering applies even if the listener is registered
     * on different resources in the same tree, for example a parent and its children.
     *
     * <div class="section">Warning events</div>
     * If {@code eventType} is assignable from <code>{@linkplain WarningEvent}.class</code>,
     * then registering that listener turns off logging of warning messages for this manager.
     * This side-effect is applied on the assumption that the registered listener will handle
     * warnings in its own way, for example by showing warnings in a widget.
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  listener   listener to notify about events.
     * @param  eventType  type of {@link StoreEvent} to listen (can not be {@code null}).
     *
     * @see Resource#addListener(StoreListener, Class)
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends StoreEvent> void addListener(final StoreListener<? super T> listener, final Class<T> eventType) {
        ArgumentChecks.ensureNonNull("listener",  listener);
        ArgumentChecks.ensureNonNull("eventType", eventType);
        ForType<T> ce = null;
        for (ForType<?> e = listeners; e != null; e = e.next) {
            if (e.type.equals(eventType)) {
                ce = (ForType<T>) e;
                break;
            }
        }
        if (ce == null) {
            ce = new ForType<>(eventType, listeners);
            listeners = ce;
        }
        ce.add(listener);
    }

    /**
     * Unregisters a listener previously added for the given type of events.
     * The {@code eventType} must be the exact same class than the one given to the {@code addListener(…)} method;
     * this method does not remove listeners registered for subclasses and does not remove listeners registered in
     * parent manager.
     *
     * <p>If the same listener has been registered many times for the same even type, then this method removes only
     * the most recent registration. In other words if {@code addListener(ls, type)} has been invoked twice, then
     * {@code removeListener(ls, type)} needs to be invoked twice in order to remove all instances of that listener.
     * If the given listener is not found, then this method does nothing (no exception is thrown).</p>
     *
     * <div class="section">Warning events</div>
     * If {@code eventType} is <code>{@linkplain WarningEvent}.class</code> and if, after this method invocation,
     * there is no remaining listener for warning events, then this {@code StoreListeners} will send future warnings
     * to the loggers.
     *
     * @param  <T>        compile-time value of the {@code eventType} argument.
     * @param  listener   listener to stop notifying about events.
     * @param  eventType  type of {@link StoreEvent} which were listened (can not be {@code null}).
     *
     * @see Resource#removeListener(StoreListener, Class)
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends StoreEvent> void removeListener(StoreListener<? super T> listener, Class<T> eventType) {
        ArgumentChecks.ensureNonNull("listener",  listener);
        ArgumentChecks.ensureNonNull("eventType", eventType);
        for (ForType<?> e = listeners; e != null; e = e.next) {
            if (e.type.equals(eventType)) {
                ((ForType<T>) e).remove(listener);
                break;
            }
        }
    }

    /**
     * Returns {@code true} if this object or its parent contains at least one listener for the given type of event.
     *
     * @param  eventType  the type of event for which to check listener presence.
     * @return {@code true} if this object contains at least one listener for given event type, {@code false} otherwise.
     */
    public boolean hasListeners(final Class<? extends StoreEvent> eventType) {
        ArgumentChecks.ensureNonNull("eventType", eventType);
        StoreListeners m = this;
        do {
            for (ForType<?> e = m.listeners; e != null; e = e.next) {
                if (eventType.isAssignableFrom(e.type)) {
                    if (e.hasListeners()) {
                        return true;
                    }
                    break;
                }
            }
            m = m.parent;
        } while (m != null);
        return false;
    }
}
