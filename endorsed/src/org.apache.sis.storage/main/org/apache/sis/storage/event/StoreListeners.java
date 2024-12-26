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
import java.util.Set;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.IdentityHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Filter;
import java.util.concurrent.ExecutionException;
import java.lang.reflect.Method;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.pending.jdk.JDK19;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.base.StoreUtilities;


/**
 * Holds a list of {@link StoreListener} instances and provides convenience methods for sending events.
 * This is a helper class for {@link DataStore} and {@link Resource} implementations.
 *
 * <p>Observers can {@linkplain #addListener add listeners} for being notified about events,
 * and producers can invoke one of the {@code warning(…)} and other methods for emitting events.
 *
 * <h2>Warning events</h2>
 * All warnings are given to the listeners as {@link LogRecord} instances (this allows localizable messages
 * and additional information like {@linkplain LogRecord#getThrown() stack trace}, timestamp, <i>etc.</i>).
 * This {@code StoreListeners} class provides convenience methods like {@link #warning(String, Exception)},
 * which build {@code LogRecord} from an exception or from a string. But all those {@code warning(…)} methods
 * ultimately delegate to {@link #warning(LogRecord, Filter)}, thus providing a single point that subclasses
 * can override. When a warning is emitted, the default behavior is:
 *
 * <ul>
 *   <li>Notify all listeners that are registered for a given {@link WarningEvent} type in this {@code StoreListeners}
 *       and in the parent resource or data store. Each listener will be notified only once, even if the same listener
 *       is registered in two or more places.</li>
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
 * <h2>Thread safety</h2>
 * The same {@code StoreListeners} instance can be safely used by many threads without synchronization
 * on the part of the caller. Subclasses should make sure that any overridden methods remain safe to call
 * from multiple threads.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.0
 */
public class StoreListeners implements Localized {
    /**
     * Parent set of listeners to notify in addition to this set of listeners, or {@code null} if none.
     * This is used when a resource is created for reading components of a larger data store.
     */
    private final StoreListeners parent;

    /**
     * The declared source of events. This is not necessarily the real source,
     * but this is the source that the implementer wants to declare as public API.
     * Shall not be {@code null}.
     */
    private final Resource source;

    /**
     * The head of a chained list of listeners, or {@code null} if none.
     * Each element in this chain contains all listeners for a given even type.
     */
    private volatile ForType<?> listeners;

    /**
     * All types of of events that may be fired, or {@code null} if no restriction.
     * This is a <i>copy on write</i> set: no elements are modified after a set has been created.
     *
     * @see #setUsableEventTypes(Class...)
     */
    private volatile Set<Class<? extends StoreEvent>> permittedEventTypes;

    /**
     * Frequently used value for {@link #permittedEventTypes}.
     *
     * @see #useReadOnlyEvents()
     */
    private static final Set<Class<? extends StoreEvent>> READ_EVENT_TYPES =
                         Set.of(WarningEvent.class, CloseEvent.class);

    /**
     * The {@link CascadedStoreEvent.ParentListener}s registered on {@link #parent}.
     * This is created the first time that a {@link CascadedStoreEvent} listener is registered on a resource
     * which is not the root resource. Those listeners are handled in a special way, because a closing event
     * on the root resource should cause all children to also fire their own {@link CloseEvent}.
     */
    private Map<Class<?>, StoreListener<?>> cascadedListeners;

    /**
     * All listeners for a given even type.
     *
     * @param  <E>  the type of events of interest to the listeners.
     */
    private static final class ForType<E extends StoreEvent> {
        /**
         * The types for which listeners have been registered.
         */
        final Class<E> type;

        /**
         * The listeners for the {@linkplain #type event type}, or {@code null} if none.
         * This is a <i>copy on write</i> array: no elements are modified after an array has been created.
         */
        @SuppressWarnings("VolatileArrayField")
        private volatile StoreListener<? super E>[] listeners;

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
        ForType(final Class<E> type, final ForType<?> next) {
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
        final void add(final StoreListener<? super E> listener) {
            final StoreListener<? super E>[] list = listeners;
            final int length = (list != null) ? list.length : 0;
            @SuppressWarnings({"unchecked", "rawtypes"}) // Generic array creation.
            final StoreListener<? super E>[] copy = new StoreListener[length + 1];
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
         *
         * @param  listener  the listener to remove.
         * @return {@code true} if the list of listeners is empty after this method call.
         */
        final boolean remove(final StoreListener<? super E> listener) {
            StoreListener<? super E>[] list = listeners;
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
            return list == null;
        }

        /**
         * Removes all listeners which will never receive any kind of events.
         *
         * Note: ideally we would remove the whole {@code ForType} object, but it would require to rebuild the whole
         * {@link #listeners} chain. It is not worth because this method should never be invoked if callers invoked
         * the {@link #setUsableEventTypes(Class...)} at construction time (a recommended practice).
         */
        static void removeUnreachables(ForType<?> listeners, final Set<Class<? extends StoreEvent>> permittedEventTypes) {
            while (listeners != null) {
                if (!isPossibleEvent(permittedEventTypes, listeners.type)) {
                    listeners.listeners = null;
                }
                listeners = listeners.next;
            }
        }

        /**
         * Returns {@code true} if this element contains the given listener.
         */
        final boolean hasListener(final StoreListener<?> listener) {
            return ArraysExt.containsIdentity(listeners, listener);
        }

        /**
         * Returns the number of listeners.
         */
        final int count() {
            return (listeners != null) ? listeners.length : 0;
        }

        /**
         * Sends the given event to all listeners registered in this element.
         *
         * @param  event  the event to send to listeners.
         * @param  done   listeners who were already notified, for avoiding to notify them twice.
         * @return the {@code done} map, created when first needed.
         * @throws ExecutionException if at least one listener failed to execute.
         */
        final Map<StoreListener<?>,Boolean> eventOccured(final E event, Map<StoreListener<?>,Boolean> done)
                throws ExecutionException
        {
            RuntimeException error = null;
            final StoreListener<? super E>[] list = listeners;
            if (list != null) {
                if (done == null) {
                    done = new IdentityHashMap<>(list.length);
                }
                for (final StoreListener<? super E> listener : list) {
                    if (event.isConsumed()) break;
                    if (done.put(listener, Boolean.TRUE) == null) try {
                        listener.eventOccured(event);
                    } catch (RuntimeException ex) {
                        if (error == null) error = ex;
                        else error.addSuppressed(ex);
                    }
                }
            }
            if (error != null) {
                throw new ExecutionException(Resources.format(Resources.Keys.ExceptionInListener_1, type), error);
            }
            return done;
        }
    }

    /**
     * Creates a new instance with the given parent and initially no listener.
     * The parent is typically the {@linkplain DataStore#listeners listeners}
     * of the {@link DataStore} that created a resource.
     * When an event is {@linkplain #fire fired}, listeners registered in the parent
     * will be notified as well as listeners registered in this {@code StoreListeners}.
     * Each listener will be notified only once even if it has been registered in two places.
     *
     * <h4>Permitted even types</h4>
     * If the parent restricts the usable event types to a subset of {@link StoreEvent} subtypes,
     * then this {@code StoreListeners} inherits those restrictions. The list of usable types can
     * be {@linkplain #setUsableEventTypes rectricted more} but cannot be relaxed.
     *
     * @param parent  the manager to notify in addition to this manager, or {@code null} if none.
     * @param source  the source of events. Cannot be null.
     */
    public StoreListeners(final StoreListeners parent, Resource source) {
        this.source = Objects.requireNonNull(source);
        this.parent = parent;
        if (parent != null) {
            permittedEventTypes = parent.permittedEventTypes;
        }
    }

    /**
     * Returns the parent set of listeners that are notified in addition to this set of listeners.
     * This is the value of the {@code parent} argument given to the constructor.
     *
     * @return parent set of listeners that are notified in addition to this set of listeners.
     *
     * @since 1.3
     */
    public Optional<StoreListeners> getParent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Returns the source of events. This value is specified at construction time.
     *
     * @return the source of events (never {@code null}).
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
            if (source instanceof StoreResource) {
                final DataStore ds = ((StoreResource) source).getOriginator();
                if (ds != null) return ds;
            }
            if (source instanceof DataStore) {      // Fallback if not explicitly specified.
                return (DataStore) source;
            }
            m = m.parent;
        } while (m != null);
        return null;
    }

    /**
     * Returns a short name or label for the source. It may be the name of the file opened by a data store.
     * The returned name can be useful in warning messages for identifying the problematic source.
     *
     * <p>The default implementation {@linkplain DataStore#getDisplayName() fetches a name from the data store},
     * or returns an arbitrary name if no better name is found.</p>
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
        return Vocabulary.forLocale(getLocale()).getString(Vocabulary.Keys.Unnamed);
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
        final DataStore ds = getDataStore(this);
        return (ds != null) ? ds.getLocale() : null;
    }

    /**
     * Returns the logger where to send warnings when no other destination is specified.
     * This method tries to get the logger from {@link DataStoreProvider#getLogger()}.
     * If that logger cannot be found, then this method infers a logger name from the
     * package name of the source data store. The returned logger is used when:
     *
     * <ul>
     *   <li>no listener has been {@linkplain #addListener registered} for the {@link WarningEvent} type, and</li>
     *   <li>the {@code LogRecord} does not {@linkplain LogRecord#getLoggerName() specify a logger}.</li>
     * </ul>
     *
     * @return the logger where to send the warnings when there is no other destination.
     *
     * @see DataStoreProvider#getLogger()
     *
     * @since 1.1
     */
    public Logger getLogger() {
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
     * warning}({@linkplain Level#WARNING}, message, null)</code>.</p>
     *
     * @param  message  the warning message to report.
     */
    public void warning(final String message) {
        // Null value check done by invoked method.
        warning(Level.WARNING, message, null);
    }

    /**
     * Reports a warning described by the given exception.
     * The exception stack trace will be omitted at logging time for avoiding to pollute console output
     * (keeping in mind that this method should be invoked only for non-fatal warnings).
     * See {@linkplain #warning(Level, String, Exception) below} for more explanation.
     *
     * <p>This method is a shortcut for <code>{@linkplain #warning(Level, String, Exception)
     * warning}({@linkplain Level#WARNING}, null, exception)</code>.</p>
     *
     * @param  exception  the exception to report.
     */
    public void warning(final Exception exception) {
        warning(Level.WARNING, null, Objects.requireNonNull(exception));
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
     * warning}({@linkplain Level#WARNING}, message, exception)</code>.</p>
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
     * <h4>Stack trace omission</h4>
     * If there are no registered listeners for the {@link WarningEvent} type,
     * then the {@link LogRecord} will be sent to a {@link Logger} but <em>without</em> the stack trace.
     * This is done that way because stack traces consume lot of space in the logging files, while being considered
     * implementation details in the context of {@code StoreListeners} (on the assumption that the logging message
     * provides sufficient information).
     *
     * @param  level      the warning level.
     * @param  message    the message to log, or {@code null} if none.
     * @param  exception  the exception to log, or {@code null} if none.
     */
    public void warning(final Level level, String message, final Exception exception) {
        ArgumentChecks.ensureNonNull("level", level);
        if (exception == null) {
            ArgumentChecks.ensureNonEmpty("message", message);
        } else {
            if (message == null) {
                message = Exceptions.getLocalizedMessage(exception, getLocale());
                if (message == null) {
                    message = Classes.getShortClassName(exception);
                }
            }
        }
        final var record = new LogRecord(level, message);
        if (exception == null) try {
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk((stream) -> stream.filter(
                   (frame) -> setPublicSource(record, frame.getDeclaringClass(), frame.getMethodName())).findFirst());
         } catch (SecurityException e) {
            // Temporary catch to be removed after Apache SIS requires Java 24.
            Logging.ignorableException(StoreUtilities.LOGGER, StoreListeners.class, "warning", e);
         } else try {
            record.setThrown(exception);
            for (final StackTraceElement e : exception.getStackTrace()) {
                if (setPublicSource(record, Class.forName(e.getClassName()), e.getMethodName())) {
                    break;
                }
            }
        } catch (ClassNotFoundException e) {
            Logging.ignorableException(StoreUtilities.LOGGER, StoreListeners.class, "warning", e);
        }
        warning(record, StoreUtilities.removeStackTraceInLogs());
    }

    /**
     * Eventually sets the class name and method name in the given record,
     * and returns {@code true} if the method is a public resource method.
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
     * Reports a warning described by the given log record. Invoking this method is
     * equivalent to invoking {@link #warning(LogRecord, Filter)} with a null filter.
     *
     * @param  description  warning details provided as a log record.
     */
    public void warning(final LogRecord description) {
        warning(description, null);
    }

    /**
     * Reports a warning described by the given log record. The default implementation forwards
     * the given record to one of the following destinations, in preference order:
     *
     * <ol>
     *   <li><code>{@linkplain StoreListener#eventOccured StoreListener.eventOccured}(new
     *       {@linkplain WarningEvent}(source, record))</code> on all listeners registered for this kind of event.</li>
     *   <li><code>{@linkplain Filter#isLoggable(LogRecord) onUnhandled.isLoggable(description)}</code>
     *       if above step found no listener and the {@code onUnhandled} filter is non-null.</li>
     *   <li><code>{@linkplain Logger#getLogger(String)
     *       Logger.getLogger}(record.loggerName).{@linkplain Logger#log(LogRecord) log}(record)</code>
     *       if the filter in above step returned {@code true} (or if the filter is null).
     *       In that case, {@code loggerName} is one of the following:
     *     <ul>
     *       <li><code>record.{@linkplain LogRecord#getLoggerName() getLoggerName()}</code> if that value is non-null.</li>
     *       <li>Otherwise the value of {@link DataStoreProvider#getLogger()} if the provider is found.</li>
     *       <li>Otherwise the source {@link DataStore} package name.</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param  description  warning details provided as a log record.
     * @param  onUnhandled  filter invoked if the record has not been handled by a {@link StoreListener},
     *         or {@code null} if none. This filter determines whether the record should be sent to the
     *         logger returned by {@link #getLogger()}.
     *
     * @since 1.2
     */
    @SuppressWarnings("unchecked")
    public void warning(final LogRecord description, final Filter onUnhandled) {
        if (!fire(WarningEvent.class, new WarningEvent(source, description)) &&
                (onUnhandled == null || onUnhandled.isLoggable(description)))
        {
            final String name = description.getLoggerName();
            final Logger logger;
            if (name != null) {
                logger = Logger.getLogger(name);
            } else {
                logger = getLogger();
                description.setLoggerName(logger.getName());
            }
            logger.log(description);
        }
    }

    /**
     * Invoked if an error occurred in a least one listener during the propagation of an event.
     * The {@linkplain ExecutionException#getCause() cause} of the exception is a {@link RuntimeException}.
     * If exceptions occurred in more than one listener, all exceptions after the first one are specified
     * as {@linkplain ExecutionException#getSuppressed() suppressed exceptions} of the cause.
     *
     * <p>This method should not delegate to {@link #warning(Exception)} because the error is not with the
     * data store itself. Furthermore, the exception may have occurred during {@code warning(…)} execution,
     * in which case the exception is a kind of "warning about warning report".</p>
     *
     * @param  method  name of the method invoking this method.
     * @param  error   the exception that occurred.
     */
    static void canNotNotify(final String method, final ExecutionException error) {
        Logging.unexpectedException(StoreUtilities.LOGGER, StoreListeners.class, method, error);
    }

    /**
     * Sends the given event to all listeners registered for the given type or for a super-type.
     * This method first notifies the listeners registered in this {@code StoreListeners}, then
     * notifies listeners registered in parent {@code StoreListeners}s. Each listener will be
     * notified only once even if it has been registered many times.
     *
     * <p>If one or many {@link StoreListener#eventOccured(StoreEvent)} implemetations throw a
     * {@link RuntimeException}, those exceptions will be collected and reported in a single
     * {@linkplain Logging#unexpectedException(Logger, Class, String, Throwable) log record}.
     * Runtime exceptions in listeners do not cause this method to fail.</p>
     *
     * @param  <E>        compile-time value of the {@code eventType} argument.
     * @param  eventType  the type of the event to be fired.
     * @param  event      the event to fire.
     * @return {@code true} if the event has been sent to at least one listener.
     * @throws IllegalArgumentException if the given event type is not one of the types of events
     *         that this {@code StoreListeners} can fire.
     *
     * @see #close()
     *
     * @since 1.3
     */
    public <E extends StoreEvent> boolean fire(final Class<E> eventType, final E event) {
        ArgumentChecks.ensureNonNull("event", event);
        ArgumentChecks.ensureNonNull("eventType", eventType);
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Set<Class<? extends StoreEvent>> permittedEventTypes = this.permittedEventTypes;
        if (permittedEventTypes != null && !permittedEventTypes.contains(eventType)) {
            throw illegalEventType(eventType);
        }
        try {
            return fire(this, eventType, event);
        } catch (ExecutionException ex) {
            canNotNotify("fire", ex);
            return true;
        }
    }

    /**
     * Sends the given event to all listeners registered in the given set of listeners and its parent.
     * This method does not perform any argument validation; they must be done by the caller.
     *
     * <p>This method does not need (and should not) be synchronized.</p>
     *
     * @param  <E>        compile-time value of the {@code eventType} argument.
     * @param  m          the set of listeners that may be interested in the event.
     * @param  eventType  the type of the event to be fired.
     * @param  event      the event to fire.
     * @return {@code true} if the event has been sent to at least one listener.
     * @throws ExecutionException if an exception is thrown inside {@link StoreListener#eventOccured(StoreEvent)}.
     *         All other listeners continue to receive the event before {@code ExecutionException} is thrown.
     */
    @SuppressWarnings("unchecked")
    static <E extends StoreEvent> boolean fire(StoreListeners m, final Class<E> eventType, final E event)
            throws ExecutionException
    {
        Map<StoreListener<?>,Boolean> done = null;
        ExecutionException error = null;
        do {
            for (ForType<?> e = m.listeners; e != null; e = e.next) {
                if (e.type.isAssignableFrom(eventType)) try {
                    done = ((ForType<? super E>) e).eventOccured(event, done);
                } catch (ExecutionException ex) {
                    if (error == null) error = ex;
                    else error.getCause().addSuppressed(ex.getCause());
                }
            }
            if (event.isConsumedForParent()) break;
            m = m.parent;
        } while (m != null);
        if (error != null) {
            throw error;
        }
        return (done != null) && !done.isEmpty();
    }

    /**
     * Returns the exception to throw for an event type which is not in the set of permitted types.
     */
    private IllegalArgumentException illegalEventType(final Class<?> type) {
        return new IllegalArgumentException(Resources.forLocale(getLocale())
                .getString(Resources.Keys.IllegalEventType_1, type));
    }

    /**
     * Verifies if a listener interested in the specified type of events could receive some events
     * from this {@code StoreListeners}.
     *
     * @param  eventType  type of events to listen.
     * @return whether a listener could receive events of the specified type.
     *
     * @see #setUsableEventTypes(Class...)
     */
    private static boolean isPossibleEvent(final Set<Class<? extends StoreEvent>> permittedEventTypes, final Class<?> eventType) {
        if (permittedEventTypes == null) {
            return true;
        }
        for (final Class<? extends StoreEvent> type : permittedEventTypes) {
            if (eventType.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registers a listener to notify when the specified kind of event occurs.
     * Registering a listener for a given {@code eventType} also register the listener for all event sub-types.
     * The same listener can be registered many times, but its {@link StoreListener#eventOccured(StoreEvent)}
     * method will be invoked only once per event. This filtering applies even if the listener is registered
     * on different resources in the same tree, for example a parent and its children.
     *
     * <h4>Warning events</h4>
     * If {@code eventType} is assignable from <code>{@linkplain WarningEvent}.class</code>,
     * then registering that listener turns off logging of warning messages for this manager.
     * This side-effect is applied on the assumption that the registered listener will handle
     * warnings in its own way, for example by showing warnings in a widget.
     *
     * @param  <E>        compile-time value of the {@code eventType} argument.
     * @param  eventType  type of {@link StoreEvent} to listen (cannot be {@code null}).
     * @param  listener   listener to notify about events.
     *
     * @see Resource#addListener(Class, StoreListener)
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    public synchronized <E extends StoreEvent> void addListener(
            final Class<E> eventType, final StoreListener<? super E> listener)
    {
        ArgumentChecks.ensureNonNull("listener",  listener);
        ArgumentChecks.ensureNonNull("eventType", eventType);
        if (isPossibleEvent(permittedEventTypes, eventType)) {
            ForType<E> ce = null;
            for (ForType<?> e = listeners; e != null; e = e.next) {
                if (e.type == eventType) {
                    ce = (ForType<E>) e;
                    break;
                }
            }
            if (ce == null) {
                ce = new ForType<>(eventType, listeners);
                listeners = ce;
            }
            ce.add(listener);
            /*
             * If we are adding a listener for `CascadedStoreEvent`, we may need
             * to register a listener in the parent for cascading the events.
             */
            if (parent != null && CascadedStoreEvent.class.isAssignableFrom(eventType)) {
                if (cascadedListeners == null) {
                    cascadedListeners = new IdentityHashMap<>(4);
                }
                StoreListener cascade = cascadedListeners.get(eventType);
                if (cascade == null) {
                    cascade = new CascadedStoreEvent.ParentListener(eventType, parent, this);
                    cascadedListeners.put(eventType, cascade);
                    parent.addListener(eventType, cascade);
                }
            }
        }
    }

    /**
     * Unregisters a listener previously added for the given type of events.
     * The {@code eventType} must be the exact same class as the one given to the {@code addListener(…)} method;
     * this method does not remove listeners registered for subclasses and does not remove listeners registered in
     * parent manager.
     *
     * <p>If the same listener has been registered many times for the same even type, then this method removes only
     * the most recent registration. In other words if {@code addListener(type, ls)} has been invoked twice, then
     * {@code removeListener(type, ls)} needs to be invoked twice in order to remove all instances of that listener.
     * If the given listener is not found, then this method does nothing (no exception is thrown).</p>
     *
     * <h4>Warning events</h4>
     * If {@code eventType} is <code>{@linkplain WarningEvent}.class</code> and if, after this method invocation,
     * there are no remaining listeners for warning events, then this {@code StoreListeners} will send future
     * warnings to the loggers.
     *
     * @param  <E>        compile-time value of the {@code eventType} argument.
     * @param  eventType  type of {@link StoreEvent} which were listened (cannot be {@code null}).
     * @param  listener   listener to stop notifying about events.
     *
     * @see Resource#removeListener(Class, StoreListener)
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    public synchronized <E extends StoreEvent> void removeListener(
            final Class<E> eventType, final StoreListener<? super E> listener)
    {
        ArgumentChecks.ensureNonNull("listener",  listener);
        ArgumentChecks.ensureNonNull("eventType", eventType);
        for (ForType<?> e = listeners; e != null; e = e.next) {
            if (e.type == eventType) {
                if (((ForType<E>) e).remove(listener) && cascadedListeners != null) {
                    final StoreListener cascade = cascadedListeners.remove(eventType);
                    if (cascade != null) {
                        parent.removeListener(eventType, cascade);
                    }
                }
                break;
            }
        }
    }

    /**
     * Returns {@code true} if the given listener is registered for the given type or a super-type.
     * This method may unconditionally return {@code false} if the given type of event is never fired
     * by this {@code StoreListeners}, because calls to {@code addListener(eventType, …)} are free to
     * ignore the listeners for those types.
     *
     * @param  <E>        compile-time value of the {@code eventType} argument.
     * @param  eventType  type of {@link StoreEvent} to check (cannot be {@code null}).
     * @param  listener   listener to check for registration.
     * @return {@code true} if this object contains the specified listener for given event type, {@code false} otherwise.
     *
     * @since 1.3
     */
    public <E extends StoreEvent> boolean hasListener(final Class<E> eventType, final StoreListener<? super E> listener) {
        // No need to synchronize this method.
        ArgumentChecks.ensureNonNull("listener",  listener);
        ArgumentChecks.ensureNonNull("eventType", eventType);
        for (ForType<?> e = listeners; e != null; e = e.next) {
            if (e.type == eventType && e.hasListener(listener)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if at least one listener is registered for the given type or a super-type.
     * This method may unconditionally return {@code false} if the given type of event is never fired
     * by this {@code StoreListeners}, because calls to {@code addListener(eventType, …)} are free to
     * ignore the listeners for those types.
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
                    if (e.count() != 0) {
                        return true;
                    }
                }
            }
            m = m.parent;
        } while (m != null);
        return false;
    }

    /**
     * Notifies this {@code StoreListeners} that only events of the specified types will be fired.
     * With this knowledge, {@code StoreListeners} will not retain any reference to listeners that
     * are not listening to events of those types or to events of a parent type.
     * This restriction allows the garbage collector to dispose unnecessary listeners.
     *
     * <p>The argument shall enumerate all permitted types, including sub-types (they are not automatically accepted).
     * All types given in argument must be types that were accepted before the invocation of this method.
     * In other words, this method can be invoked for reducing the set of permitted types but not for expanding it.</p>
     *
     * <h4>Example</h4>
     * an application may unconditionally register listeners for being notified about additions of new data.
     * If a {@link DataStore} implementation is read-only, then such listeners would never receive any notification.
     * As a slight optimization, the {@code DataStore} constructor can invoke this method for example as below:
     *
     * {@snippet lang="java" :
     *     listeners.setUsableEventTypes(WarningEvent.class);
     *     }
     *
     * With this configuration, calls to {@code addListener(DataAddedEvent.class, foo)} will be ignored,
     * thus avoiding this instance to retain a never-used reference to the {@code foo} listener.
     *
     * @param  permitted  type of events that are permitted. Permitted sub-types shall be explicitly enumerated as well.
     * @throws IllegalArgumentException if one of the given types was not permitted before invocation of this method.
     *
     * @see #useReadOnlyEvents()
     *
     * @since 1.2
     */
    @SuppressWarnings("unchecked")
    public synchronized void setUsableEventTypes(final Class<?>... permitted) {
        ArgumentChecks.ensureNonEmpty("permitted", permitted);
        final Set<Class<? extends StoreEvent>> current = permittedEventTypes;
        final Set<Class<? extends StoreEvent>> types = JDK19.newHashSet(permitted.length);
        for (final Class<?> type : permitted) {
            if (current != null ? current.contains(type) : StoreEvent.class.isAssignableFrom(type)) {
                types.add((Class<? extends StoreEvent>) type);
            } else {
                throw illegalEventType(type);
            }
        }
        permittedEventTypes = READ_EVENT_TYPES.equals(types) ? READ_EVENT_TYPES : Set.copyOf(types);
        ForType.removeUnreachables(listeners, types);
    }

    /**
     * Notifies this {@code StoreListeners} that it will fire only {@link WarningEvent}s and {@link CloseEvent}.
     * This method is a shortcut for <code>{@linkplain setUsableEventTypes setUsableEventTypes}(WarningEvent.class,
     * CloseEvent.class)}</code>, provided because frequently used by read-only data store implementations.
     *
     * <p>Declaring a root resource (typically a {@link DataStore}) as read-only implies that all children
     * (e.g. {@linkplain org.apache.sis.storage.Aggregate#components() components of an aggregate})
     * are also read-only.</p>
     *
     * @see #setUsableEventTypes(Class...)
     * @see WarningEvent
     * @see CloseEvent
     *
     * @since 1.3
     */
    public synchronized void useReadOnlyEvents() {
        final Set<Class<? extends StoreEvent>> current = permittedEventTypes;
        if (current == null) {
            permittedEventTypes = READ_EVENT_TYPES;
        } else if (!READ_EVENT_TYPES.equals(current)) {
            throw illegalEventType(WarningEvent.class);
        }
        ForType.removeUnreachables(listeners, READ_EVENT_TYPES);
    }

    /**
     * Sends a {@link CloseEvent} to all listeners registered for that kind of event,
     * then discards listeners in this instance (but not in parents).
     * Because listeners are discarded, invoking this method many times
     * on the same instance has no effect after the first invocation.
     *
     * <p>If one or many {@link StoreListener#eventOccured(StoreEvent)} implementations throw
     * a {@link RuntimeException}, those exceptions will be collected and reported in a single
     * {@linkplain Logging#unexpectedException(Logger, Class, String, Throwable) log record}.
     * Runtime exceptions in listeners do not cause this method to fail.</p>
     *
     * @see #fire(Class, StoreEvent)
     * @see DataStore#close()
     * @see CloseEvent
     *
     * @since 1.3
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    public void close() {
        try {
            /*
             * We use the private static method instead of `fire(Class, StoreEvent)` public method
             * because calls to `close()` should never fail (except with `java.lang.Error` because
             * we do not want to hide serious errors), so we bypass argument validation and method
             * overriding as a safety.
             */
            fire(this, CloseEvent.class, new CloseEvent(source));
        } catch (ExecutionException ex) {
            canNotNotify("close", ex);
        }
        /*
         * This `StoreListeners` may not be garbage-collected immediately if the data store has been closed
         * asynchronously. So clearing the following fields may help to garbage-collect some more resources.
         */
        synchronized (this) {
            cascadedListeners = null;
            listeners = null;
        }
    }

    /**
     * Returns a string representation for debugging purposes.
     *
     * @return a debug string.
     */
    @Override
    public String toString() {
        int count = 0;
        for (ForType<?> e = listeners; e != null; e = e.next) {
            count += e.count();
        }
        return Strings.toString(getClass(),
                "parent", (parent != null) ? Classes.getShortClassName(parent.source) : null,
                "source", Classes.getShortClassName(source),
                "count",  count);
    }
}
