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
package org.apache.sis.referencing.factory;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Deque;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.WeakHashMap;
import java.util.IdentityHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;
import java.lang.ref.WeakReference;
import java.lang.ref.PhantomReference;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import javax.measure.unit.Unit;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.internal.system.ReferenceQueueConsumer;
import org.apache.sis.internal.system.DelayedExecutor;
import org.apache.sis.internal.system.DelayedRunnable;
import org.apache.sis.internal.system.Shutdown;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.util.logging.PerformanceLevel;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.JDK7;
import org.apache.sis.internal.jdk8.JDK8;
import org.apache.sis.internal.jdk7.AutoCloseable;


/**
 * A concurrent authority factory that caches all objects created by another factory.
 * All {@code createFoo(String)} methods first check if a previously created object exists for the given code.
 * If such object exists, it is returned. Otherwise, the object creation is delegated to another factory given
 * by {@link #newDataAccess()} and the result is cached in this factory.
 *
 * <p>{@code ConcurrentAuthorityFactory} delays the call to {@code newDataAccess()} until first needed,
 * and {@linkplain AutoCloseable#close() closes} the factory used as a <cite>Data Access Object</cite>
 * (DAO) after some timeout. This approach allows to establish a connection to a database (for example)
 * and keep it only for a relatively short amount of time.</p>
 *
 * <div class="section">Caching strategy</div>
 * Objects are cached by strong references, up to the amount of objects specified at construction time.
 * If a greater amount of objects are cached, then the oldest ones will be retained through a
 * {@linkplain WeakReference weak reference} instead of a strong one.
 * This means that this caching factory will continue to return those objects as long as they are in use somewhere
 * else in the Java virtual machine, but will be discarded (and recreated on the fly if needed) otherwise.
 *
 * <div class="section">Multi-threading</div>
 * The cache managed by this class is concurrent. However the Data Access Objects (DAO) are assumed non-concurrent.
 * If two or more threads are accessing this factory in same time, then two or more Data Access Object instances
 * may be created. The maximal amount of instances to create is specified at {@code ConcurrentAuthorityFactory}
 * construction time. If more Data Access Object instances are needed, some of the threads will block until an
 * instance become available.
 *
 * <div class="section">Note for subclasses</div>
 * This abstract class does not implement any of the {@link DatumAuthorityFactory}, {@link CSAuthorityFactory},
 * {@link CRSAuthorityFactory} and {@link CoordinateOperationAuthorityFactory} interfaces.
 * Subclasses should select the interfaces that they choose to implement.
 *
 * @param <DAO> the type of factory used as Data Access Object (DAO)
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@AutoCloseable
public abstract class ConcurrentAuthorityFactory<DAO extends GeodeticAuthorityFactory>
        extends GeodeticAuthorityFactory
{
    /**
     * Duration of data access operations that should be logged, in nanoseconds.
     * Any operation that take longer than this amount of time to execute will have a message logged.
     * The log level depends on the execution duration as specified in {@link PerformanceLevel}.
     */
    private static final long DURATION_FOR_LOGGING = 10000000L;       // 10 milliseconds.

    /**
     * The authority, cached after first requested.
     */
    private transient volatile Citation authority;

    /**
     * The {@code createFoo(String)} methods that are <strong>not</strong> overridden in the Data Access Object (DAO).
     * This map is created at construction time and should not be modified after construction.
     *
     * @see #isDefault(Class)
     */
    private final Map<Class<?>,Boolean> inherited = new IdentityHashMap<Class<?>,Boolean>();

    /**
     * The pool of cached objects.
     */
    private final Cache<Key,Object> cache;

    /**
     * The pool of objects identified by {@link Finder#find(IdentifiedObject)}.
     * Values may be an empty set if an object has been searched but has not been found.
     *
     * <p>Every access to this pool must be synchronized on {@code findPool}.</p>
     */
    private final Map<IdentifiedObject,FindEntry> findPool = new WeakHashMap<IdentifiedObject,FindEntry>();

    /**
     * Holds the reference to a Data Access Object used by {@link ConcurrentAuthorityFactory}, together with
     * information about its usage. In a mono-thread application, there is typically only one {@code DataAccessRef}
     * instance at a given time. However if more than one than one thread are requesting new objects concurrently,
     * then many instances may exist for the same {@code ConcurrentAuthorityFactory}.
     *
     * <p>If the Data Access Object is currently in use, then {@code DataAccessRef} counts how many recursive
     * invocations of a {@link #factory} {@code createFoo(String)} method is under way in the current thread.
     * This information is used in order to reuse the same factory instead than creating new instances
     * when a {@code GeodeticAuthorityFactory} implementation invokes itself indirectly through the
     * {@link ConcurrentAuthorityFactory}. This assumes that factory implementations are reentrant.</p>
     *
     * <p>If the Data Access Object has been released, then {@code DataAccessRef} keep the release timestamp.
     * This information is used for prioritize the Data Access Objects to close.</p>
     */
    private static final class DataAccessRef<DAO extends GeodeticAuthorityFactory> {
        /**
         * The factory used for data access.
         */
        final DAO factory;

        /**
         * Incremented on every call to {@link ConcurrentAuthorityFactory#getDataAccess()} and decremented on every call
         * to {@link ConcurrentAuthorityFactory#release()}. When this value reach zero, the factory is really released.
         */
        int depth;

        /**
         * The timestamp (<strong>not</strong> relative to epoch) at the time the Data Access Object has been released.
         * This timestamp shall be obtained by {@link System#nanoTime()} for consistency with {@link DelayedRunnable}.
         */
        long timestamp;

        /**
         * Creates new Data Access Object information for the given factory.
         */
        DataAccessRef(final DAO factory) {
            this.factory = factory;
        }

        /**
         * Returns a string representation for debugging purpose only.
         */
        @Debug
        @Override
        public String toString() {
            final String text;
            final Number value;
            if (depth != 0) {
                text = "%s in use at depth %d";
                value = depth;
            } else {
                text = "%s made available %d seconds ago";
                value = Math.round((System.nanoTime() - timestamp) / 1E+9);   // Convert nanoseconds to seconds.
            }
            return String.format(text, Classes.getShortClassName(factory), value);
        }
    }

    /**
     * The Data Access Object in use by the current thread.
     */
    private final ThreadLocal<DataAccessRef<DAO>> currentDAO = new ThreadLocal<DataAccessRef<DAO>>();

    /**
     * The Data Access Object instances previously created and released for future reuse.
     * Last used factories must be {@linkplain Deque#addLast(Object) added last}.
     * This is used as a LIFO stack.
     */
    private final Deque<DataAccessRef<DAO>> availableDAOs = new LinkedList<DataAccessRef<DAO>>();

    /**
     * The amount of Data Access Objects that can still be created. This number is decremented in a block
     * synchronized on {@link #availableDAOs} every time a Data Access Object is in use, and incremented
     * once released.
     */
    private int remainingDAOs;

    /**
     * {@code true} if the call to {@link #closeExpired()} is scheduled for future execution in the background
     * cleaner thread. A value of {@code true} implies that this factory contains at least one active data access.
     * However the reciprocal is not true: this field may be set to {@code false} while a DAO is currently in use
     * because this field is set to {@code true} only when a worker factory is {@linkplain #release released}.
     *
     * <p>Note that we can not use {@code !availableDAOs.isEmpty()} as a replacement of {@code isCleanScheduled}
     * because the queue is empty if all Data Access Objects are currently in use.</p>
     *
     * <p>Every access to this field must be performed in a block synchronized on {@link #availableDAOs}.</p>
     *
     * @see #isCleanScheduled()
     */
    private boolean isCleanScheduled;

    /**
     * The delay of inactivity (in nanoseconds) before to close a Data Access Object.
     * Every access to this field must be performed in a block synchronized on {@link #availableDAOs}.
     *
     * @see #getTimeout(TimeUnit)
     */
    private long timeout = 60000000000L;     // 1 minute

    /**
     * The maximal difference between the scheduled time and the actual time in order to perform the factory disposal,
     * in nanoseconds. This is used as a tolerance value for possible wait time inaccuracy.
     */
    static final long TIMEOUT_RESOLUTION = 200000000L;    // 0.2 second

    /**
     * Constructs an instance with a default number of threads and a default number of entries to keep
     * by strong references. Note that those default values may change in any future SIS versions based
     * on experience gained.
     *
     * @param dataAccessClass The class of Data Access Object (DAO) created by {@link #newDataAccess()}.
     */
    protected ConcurrentAuthorityFactory(Class<DAO> dataAccessClass) {
        this(dataAccessClass, 100, 8);
        /*
         * NOTE: if the default maximum number of Data Access Objects (currently 8) is augmented,
         * make sure to augment the number of runner threads in the "StressTest" class to a greater amount.
         */
    }

    /**
     * Constructs an instance with the specified number of entries to keep by strong references.
     * If a number of object greater than {@code maxStrongReferences} are created, then the strong references
     * for the eldest objects will be replaced by weak references.
     *
     * @param dataAccessClass The class of Data Access Object (DAO) created by {@link #newDataAccess()}.
     * @param maxStrongReferences The maximum number of objects to keep by strong reference.
     * @param maxConcurrentQueries The maximal amount of Data Access Objects to use concurrently.
     *        If more than this amount of threads are querying this {@code ConcurrentAuthorityFactory} concurrently,
     *        additional threads will be blocked until a Data Access Object become available.
     */
    protected ConcurrentAuthorityFactory(final Class<DAO> dataAccessClass,
            final int maxStrongReferences, final int maxConcurrentQueries)
    {
        ArgumentChecks.ensureNonNull("dataAccessClass", dataAccessClass);
        ArgumentChecks.ensurePositive("maxStrongReferences", maxStrongReferences);
        ArgumentChecks.ensureStrictlyPositive("maxConcurrentQueries", maxConcurrentQueries);
        /*
         * Detect which methods in the DAO have been overridden.
         */
        for (final Method method : dataAccessClass.getMethods()) {
            if (method.getDeclaringClass() == GeodeticAuthorityFactory.class && method.getName().startsWith("create")) {
                final Class<?>[] p = method.getParameterTypes();
                if (p.length == 1 && p[0] == String.class) {
                    inherited.put(method.getReturnType(), Boolean.TRUE);
                }
            }
        }
        /*
         * Create a cache allowing key collisions.
         * Key collision is usually an error. But in this case we allow them in order to enable recursivity.
         * If during the creation of an object the program asks to this ConcurrentAuthorityFactory for the same
         * object (using the same key), then the default Cache implementation considers that situation as an
         * error unless the above property has been set to 'true'.
         */
        remainingDAOs = maxConcurrentQueries;
        cache = new Cache<Key,Object>(20, maxStrongReferences, false);
        cache.setKeyCollisionAllowed(true);
        /*
         * The shutdown hook serves two purposes:
         *
         *   1) Closes the Data Access Objects when the garbage collector determined
         *      that this ConcurrentAuthorityFactory is no longer in use.
         *
         *   2) Closes the Data Access Objects at JVM shutdown time if the application is standalone,
         *      or when the bundle is uninstalled if running inside an OSGi or Servlet container.
         */
        Shutdown.register(new ShutdownHook<DAO>(this));
    }

    /**
     * Returns the number of Data Access Objects available for reuse. This count does not include the
     * Data Access Objects that are currently in use. This method is used only for testing purpose.
     *
     * @see #isCleanScheduled()
     */
    @Debug
    final int countAvailableDataAccess() {
        synchronized (availableDAOs) {
            return availableDAOs.size();
        }
    }

    /**
     * Creates a factory which will perform the actual geodetic object creation work.
     * This method is invoked the first time a {@code createFoo(String)} method is invoked.
     * It may also be invoked again if additional factories are needed in different threads,
     * or if all factories have been closed after the timeout.
     *
     * <div class="section">Multi-threading</div>
     * This method (but not necessarily the returned factory) needs to be thread-safe;
     * {@code ConcurrentAuthorityFactory} does not hold any lock when invoking this method.
     * Subclasses are responsible to apply their own synchronization if needed,
     * but are encouraged to avoid doing so if possible.
     * In addition, implementations should not invoke other {@code ConcurrentAuthorityFactory}
     * methods during this method execution in order to avoid never-ending loop.
     *
     * @return Data Access Object (DAO) to use in {@code createFoo(String)} methods.
     * @throws UnavailableFactoryException if the Data Access Object is unavailable because an optional resource is missing.
     * @throws FactoryException if the creation of Data Access Object failed for another reason.
     */
    protected abstract DAO newDataAccess() throws UnavailableFactoryException, FactoryException;

    /**
     * Returns a Data Access Object. This method <strong>must</strong>
     * be used together with {@link #release(String, Class, String)}
     * in a {@code try ... finally} block.
     *
     * @return Data Access Object (DAO) to use in {@code createFoo(String)} methods.
     * @throws FactoryException if the Data Access Object creation failed.
     */
    @SuppressWarnings("null")
    private DAO getDataAccess() throws FactoryException {
        /*
         * First checks if the current thread is already using a factory. If yes, we will
         * avoid creating new factories on the assumption that factories are reentrant.
         */
        DataAccessRef<DAO> usage = currentDAO.get();
        if (usage == null) {
            synchronized (availableDAOs) {
                /*
                 * If we have reached the maximal amount of Data Access Objects allowed, wait for an instance
                 * to become available. In theory the 0.2 second timeout is not necessary, but we put it as a
                 * safety in case we fail to invoke a notify() matching this wait(), for example someone else
                 * is waiting on this monitor or because the release(…) method threw an exception.
                 */
                while (remainingDAOs == 0) {
                    try {
                        availableDAOs.wait(TIMEOUT_RESOLUTION);
                    } catch (InterruptedException e) {
                        // Someone does not want to let us sleep.
                        throw new FactoryException(e.getLocalizedMessage(), e);
                    }
                }
                /*
                 * Reuse the most recently used factory, if available. If there is no factory available for reuse,
                 * creates a new one. We do not add it to the queue now; it will be done by the release(…) method.
                 */
                usage = availableDAOs.pollLast();
                remainingDAOs--;       // Should be done last when we are sure to not fail.
            }
            /*
             * If there is a need to create a new factory, do that outside the synchronized block because this
             * creation may involve a lot of client code. This is better for reducing the dead-lock risk.
             * Subclasses are responsible of synchronizing their newDataAccess() method if necessary.
             */
            try {
                if (usage == null) {
                    final DAO factory = newDataAccess();
                    if (factory == null) {
                        UnavailableFactoryException e = new UnavailableFactoryException(Errors.format(
                                Errors.Keys.FactoryNotFound_1, GeodeticAuthorityFactory.class));
                        e.setUnavailableFactory(this);
                        throw e;
                    }
                    usage = new DataAccessRef<DAO>(factory);
                }
                assert usage.depth == 0 : usage;
                usage.timestamp = System.nanoTime();
            } catch (Throwable e) {
                /*
                 * If any kind of error occurred, restore the 'remainingDAO' field as if no code were executed.
                 * This code would not have been needed if we were allowed to decrement 'remainingDAO' only as
                 * the very last step (when we know that everything else succeed).
                 * But it needed to be decremented inside the synchronized block.
                 */
                synchronized (availableDAOs) {
                    remainingDAOs++;
                }
                if (e instanceof FactoryException) throw (FactoryException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                if (e instanceof Error)            throw (Error) e;
                throw new RuntimeException(e);  // Should never happen.
            }
            currentDAO.set(usage);
        }
        /*
         * Increment below is safe even if outside the synchronized block,
         * because each thread own exclusively its DataAccessRef instance.
         */
        usage.depth++;
        return usage.factory;
    }

    /**
     * Releases the Data Access Object previously obtained with {@link #getDataAccess()}.
     * This method marks the factory as available for reuse by other threads.
     *
     * <p>All arguments given to this method are for logging purpose only.</p>
     *
     * @param caller The caller method, or {@code null} for {@code "create" + type.getSimpleName()}.
     * @param type   The type of the created object, or {@code null} for performing no logging.
     * @param code   The code of the created object, or {@code null} if none.
     */
    private void release(String caller, final Class<?> type, final String code) {
        final DataAccessRef<DAO> usage = currentDAO.get();  // A null value here would be an error in our algorithm.
        if (--usage.depth == 0) {
            currentDAO.remove();
            long time = usage.timestamp;
            synchronized (availableDAOs) {
                remainingDAOs++;            // Must be done first in case an exception happen after this point.
                recycle(usage);
                availableDAOs.notify();     // We released only one data access, so awake only one thread - not all of them.
                time = usage.timestamp - time;
            }
            /*
             * Log only events that take longer than the threshold (e.g. 10 milliseconds).
             */
            if (time >= DURATION_FOR_LOGGING && type != null) {
                if (caller == null) {
                    caller = "create".concat(type.getSimpleName());
                }
                final Double duration = time / 1E+9;
                final PerformanceLevel level = PerformanceLevel.forDuration(time, TimeUnit.NANOSECONDS);
                final Messages resources = Messages.getResources(null);
                final LogRecord record;
                if (code != null) {
                    record = resources.getLogRecord(level, Messages.Keys.CreateDurationFromIdentifier_3, type, code, duration);
                } else {
                    record = resources.getLogRecord(level, Messages.Keys.CreateDuration_2, type, duration);
                }
                record.setLoggerName(Loggers.CRS_FACTORY);
                Logging.log(getClass(), caller, record);
            }
        }
        assert usage.depth >= 0 : usage;
    }

    /**
     * Pushes the given DAO in the list of objects available for reuse.
     */
    private void recycle(final DataAccessRef<DAO> usage) {
        usage.timestamp = System.nanoTime();
        availableDAOs.addLast(usage);
        /*
         * If the Data Access Object we just released is the first one, awake the
         * cleaner thread which was waiting for an indefinite amount of time.
         */
        if (!isCleanScheduled) {
            isCleanScheduled = true;
            DelayedExecutor.schedule(new CloseTask(usage.timestamp + timeout));
        }
    }

    /**
     * {@code true} if the call to {@link #closeExpired()} is scheduled for future execution in the background
     * cleaner thread. A value of {@code true} implies that this factory contains at least one active data access.
     * However the reciprocal is not true: this field may be set to {@code false} while a DAO is currently in use
     * because this field is set to {@code true} only when a worker factory is {@linkplain #release released}.
     *
     * <p>This method is used only for testing purpose.</p>
     *
     * @see #countAvailableDataAccess()
     */
    @Debug
    final boolean isCleanScheduled() {
        synchronized (availableDAOs) {
            return isCleanScheduled;
        }
    }

    /**
     * Confirms that the given factories can be closed. If any factory is still in use,
     * it will be removed from that {@code factories} list and re-injected in the {@link #availableDAOs} queue.
     */
    private void confirmClose(final List<DAO> factories) {
        assert !Thread.holdsLock(availableDAOs);
        for (final Iterator<DAO> it = factories.iterator(); it.hasNext();) {
            final DAO factory = it.next();
            try {
                if (canClose(factory)) {
                    continue;
                }
            } catch (Exception e) {
                unexpectedException("canClose", e);
                continue;                               // Keep the factory on the list of factories to close.
            }
            // Cancel closing for that factory.
            it.remove();
            synchronized (availableDAOs) {
                recycle(new DataAccessRef<DAO>(factory));
            }
        }
    }

    /**
     * A task for invoking {@link ConcurrentAuthorityFactory#closeExpired()} after a delay.
     */
    private final class CloseTask extends DelayedRunnable {
        /**
         * Creates a new task to be executed at the given time,
         * in nanoseconds relative to {@link System#nanoTime()}.
         */
        CloseTask(final long timestamp) {
            super(timestamp);
        }

        /** Invoked when the delay expired. */
        @Override public void run() {
            closeExpired();
        }
    }

    /**
     * Closes the expired Data Access Objects. This method should be invoked from a background task only.
     * This method may reschedule the task again for an other execution if it appears that at least one
     * Data Access Object was not ready for disposal.
     *
     * @see #close()
     */
    final void closeExpired() {
        final List<DAO> factories;
        final boolean isEmpty;
        synchronized (availableDAOs) {
            factories = new ArrayList<DAO>(availableDAOs.size());
            final Iterator<DataAccessRef<DAO>> it = availableDAOs.iterator();
            final long nanoTime = System.nanoTime();
            while (it.hasNext()) {
                final DataAccessRef<DAO> dao = it.next();
                /*
                 * Computes how much time we need to wait again before we can close the factory.
                 * If this time is greater than some arbitrary amount, do not close the factory
                 * and wait again.
                 */
                final long nextTime = dao.timestamp + timeout;
                if (nextTime - nanoTime > TIMEOUT_RESOLUTION) {
                    /*
                     * Found a factory which is not expired. Stop the search,
                     * since the iteration is expected to be ordered.
                     */
                    DelayedExecutor.schedule(new CloseTask(nextTime));
                    break;
                }
                /*
                 * Found an expired factory. Adds it to the list of
                 * factories to close and search for other factories.
                 */
                factories.add(dao.factory);
                it.remove();
            }
            /*
             * The DAOs list is empty if all Data Access Objects in the queue have been closed.
             * Note that some DAOs may still be in use outside the queue, because the DAOs are
             * added to the queue only after completion of their work.
             * In the later case, release() will reschedule a new task.
             */
            isCleanScheduled = !(isEmpty = availableDAOs.isEmpty());
        }
        /*
         * We must close the factories from outside the synchronized block.
         */
        confirmClose(factories);
        try {
            close(factories);
        } catch (Exception exception) {
            unexpectedException("closeExpired", exception);
        }
        /*
         * If the queue of Data Access Objects (DAO) become empty, this means that this ConcurrentAuthorityFactory
         * has not created new object for a while (at least the amount of time given by the timeout), ignoring any
         * request which may be under execution in another thread right now. Reduce the amount of objects retained
         * in the cache of IdentifiedObjectFinder.find(…) results by removing all results containing more than one
         * element, except for results of CoordinateReferenceSystem and CoordinateOperation lookups. The reason is
         * that IdentifiedObjectFinder is almost always used for resolving CoordinateReferenceSystem objects, and
         * all other kind of elements in the cache were dependencies searched as a side effect of the CRS search.
         * Since we have the result of the CRS search, we often do not need anymore the result of dependency search.
         *
         * Touching 'findPool' also has the desired side-effect of letting WeakHashMap expunges stale entries.
         */
        if (isEmpty) {
            synchronized (findPool) {
                final Iterator<FindEntry> it = findPool.values().iterator();
                while (it.hasNext()) {
                    if (it.next().cleanup()) {
                        it.remove();
                    }
                }
            }
        }
    }

    /**
     * Invoked when an exception occurred while closing a factory and we can not propagate the exception to the user.
     * This situation happen when the factories are closed in a background thread. There is not much we can do except
     * logging the problem. {@code ConcurrentAuthorityFactory} should be able to continue its work normally since the
     * factory that we failed to close will not be used anymore.
     *
     * @param method     The name of the method to report as the source of the problem.
     * @param exception  The exception that occurred while closing a Data Access Object.
     */
    static void unexpectedException(final String method, final Exception exception) {
        Logging.unexpectedException(Logging.getLogger(Loggers.CRS_FACTORY),
                ConcurrentAuthorityFactory.class, method, exception);
    }

    /**
     * Returns {@code true} if the given Data Access Object (DAO) can be closed. This method is invoked automatically
     * after the {@linkplain #getTimeout timeout} if the given DAO has been idle during all that time.
     * Subclasses can override this method and return {@code false} if they want to prevent the DAO disposal
     * under some circumstances.
     *
     * <p>The default implementation always returns {@code true}.</p>
     *
     * @param factory The Data Access Object which is about to be closed.
     * @return {@code true} if the given Data Access Object can be closed.
     */
    protected boolean canClose(DAO factory) {
        return true;
    }

    /**
     * Returns the amount of time that {@code ConcurrentAuthorityFactory} will wait before to close a Data Access Object.
     * This delay is measured from the last time the Data Access Object has been used by a {@code createFoo(String)} method.
     *
     * @param unit The desired unit of measurement for the timeout.
     * @return The current timeout in the given unit of measurement.
     */
    public long getTimeout(final TimeUnit unit) {
        synchronized (availableDAOs) {
            return unit.convert(timeout, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Sets a timer for closing the Data Access Object after the specified amount of time of inactivity.
     * If a new Data Access Object is needed after the disposal of the last one, then the {@link #newDataAccess()}
     * method will be invoked again.
     *
     * @param delay The delay of inactivity before to close a Data Access Object.
     * @param unit  The unit of measurement of the given delay.
     */
    public void setTimeout(long delay, final TimeUnit unit) {
        ArgumentChecks.ensureStrictlyPositive("delay", delay);
        delay = unit.toNanos(delay);
        synchronized (availableDAOs) {
            timeout = delay;                // Will be taken in account after the next factory to close.
        }
    }

    /**
     * Returns the database or specification that defines the codes recognized by this factory.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached value if it exists.</li>
     *   <li>Otherwise:
     *     <ol>
     *       <li>get an instance of the Data Access Object,</li>
     *       <li>delegate to its {@link GeodeticAuthorityFactory#getAuthority()} method,</li>
     *       <li>release the Data Access Object,</li>
     *       <li>cache the result.</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * If this method can not get a Data Access Object (for example because no database connection is available),
     * then this method returns {@code null}.
     *
     * @return The organization responsible for definition of the database, or {@code null} if unavailable.
     */
    @Override
    public Citation getAuthority() {
        Citation c = authority;
        if (c == null) try {
            final DAO factory = getDataAccess();
            try {
                // Cache only in case of success. If we failed, we
                // will try again next time this method is invoked.
                authority = c = factory.getAuthority();
            } finally {
                release("getAuthority", Citation.class, null);
            }
        } catch (FactoryException e) {
            Logging.unexpectedException(Logging.getLogger(Loggers.CRS_FACTORY),
                    ConcurrentAuthorityFactory.class, "getAuthority", e);
        }
        return c;
    }

    /**
     * Returns the set of authority codes for objects of the given type.
     * The default implementation performs the following steps:
     * <ol>
     *   <li>get an instance of the Data Access Object,</li>
     *   <li>delegate to its {@link GeodeticAuthorityFactory#getAuthorityCodes(Class)} method,</li>
     *   <li>release the Data Access Object.</li>
     * </ol>
     *
     * @param  type The spatial reference objects type (e.g. {@code ProjectedCRS.class}).
     * @return The set of authority codes for spatial reference objects of the given type.
     *         If this factory does not contains any object of the given type, then this method returns an empty set.
     * @throws FactoryException if access to the underlying database failed.
     */
    @Override
    public Set<String> getAuthorityCodes(final Class<? extends IdentifiedObject> type) throws FactoryException {
        final DAO factory = getDataAccess();
        try {
            return factory.getAuthorityCodes(type);
            /*
             * In the particular case of EPSG factory, the returned Set maintains a live connection to the database.
             * But it still okay to release the factory anyway because our implementation will really close
             * the connection only when the iteration is over or the iterator has been garbage-collected.
             */
        } finally {
            release("getAuthorityCodes", Set.class, null);
        }
    }

    /**
     * Gets a description of the object corresponding to a code.
     * The default implementation performs the following steps:
     * <ol>
     *   <li>get an instance of the Data Access Object,</li>
     *   <li>delegate to its {@link GeodeticAuthorityFactory#getDescriptionText(String)} method,</li>
     *   <li>release the Data Access Object.</li>
     * </ol>
     *
     * @param  code Value allocated by authority.
     * @return A description of the object, or {@code null} if the object
     *         corresponding to the specified {@code code} has no description.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the query failed for some other reason.
     */
    @Override
    public InternationalString getDescriptionText(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        final DAO factory = getDataAccess();
        try {
            return factory.getDescriptionText(code);
        } finally {
            release("getDescriptionText", InternationalString.class, code);
        }
    }

    /**
     * Returns {@code true} if the Data Access Object (DAO) does not provide a {@code create} method for the
     * given type of object. The intend is to decide if the caller should delegate to the DAO or delegate to
     * a more generic method of this class (e.g. {@code createCoordinateReferenceSystem(String)} instead of
     * {@code createGeographicCRS(String)}) in order to give to {@code ConcurrentAuthorityFactory} a chance
     * to reuse a value presents in the cache.
     */
    private boolean isDefault(final Class<?> type) {
        return inherited.containsKey(type);
    }

    /**
     * Returns a code equivalent to the given code but with unnecessary elements eliminated.
     * The normalized code is used as the key in the cache, and is also the code which will
     * be passed to the {@linkplain #newDataAccess() Data Access Object} (DAO).
     *
     * <p>The default implementation performs the following steps:</p>
     * <ol>
     *   <li>Removes the namespace if presents. For example if the {@linkplain #getCodeSpaces() codespace}
     *       is EPSG and the given code starts with the {@code "EPSG:"} prefix, then that prefix is removed.</li>
     *   <li>Removes leading and trailing spaces.</li>
     * </ol>
     *
     * Subclasses can override this method for performing a different normalization work.
     * It is okay to return internal codes completely different than the given codes,
     * provided that the Data Access Objects will understand those internal codes.
     *
     * @param  code The code to normalize.
     * @return The normalized code.
     * @throws FactoryException if an error occurred while normalizing the given code.
     */
    protected String normalizeCode(String code) throws FactoryException {
        return trimNamespace(code);
    }

    /**
     * Returns an arbitrary object from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise:
     *     <ol>
     *       <li>get an instance of the Data Access Object,</li>
     *       <li>delegate to its {@link GeodeticAuthorityFactory#createObject(String)} method,</li>
     *       <li>release the Data Access Object,</li>
     *       <li>cache the result.</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * @return The object for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public IdentifiedObject createObject(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.OBJECT, code);
    }

    /**
     * Returns an arbitrary coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createCoordinateReferenceSystem(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCoordinateReferenceSystem(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createObject(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(final String code) throws FactoryException {
        if (isDefault(CoordinateReferenceSystem.class)) {
            return super.createCoordinateReferenceSystem(code);
        }
        return create(AuthorityFactoryProxy.CRS, code);
    }

    /**
     * Returns a 2- or 3-dimensional coordinate reference system based on an ellipsoidal approximation of the geoid.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createGeographicCRS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createGeographicCRS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateReferenceSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public GeographicCRS createGeographicCRS(final String code) throws FactoryException {
        if (isDefault(GeographicCRS.class)) {
            return super.createGeographicCRS(code);
        }
        return create(AuthorityFactoryProxy.GEOGRAPHIC_CRS, code);
    }

    /**
     * Returns a 3-dimensional coordinate reference system with the origin at the approximate centre of mass of the earth.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createGeocentricCRS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createGeocentricCRS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateReferenceSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public GeocentricCRS createGeocentricCRS(final String code) throws FactoryException {
        if (isDefault(GeocentricCRS.class)) {
            return super.createGeocentricCRS(code);
        }
        return create(AuthorityFactoryProxy.GEOCENTRIC_CRS, code);
    }

    /**
     * Returns a 2-dimensional coordinate reference system used to approximate the shape of the earth on a planar surface.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createProjectedCRS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createProjectedCRS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateReferenceSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public ProjectedCRS createProjectedCRS(final String code) throws FactoryException {
        if (isDefault(ProjectedCRS.class)) {
            return super.createProjectedCRS(code);
        }
        return create(AuthorityFactoryProxy.PROJECTED_CRS, code);
    }

    /**
     * Returns a 1-dimensional coordinate reference system used for recording heights or depths.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createVerticalCRS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createVerticalCRS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateReferenceSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public VerticalCRS createVerticalCRS(final String code) throws FactoryException {
        if (isDefault(VerticalCRS.class)) {
            return super.createVerticalCRS(code);
        }
        return create(AuthorityFactoryProxy.VERTICAL_CRS, code);
    }

    /**
     * Returns a 1-dimensional coordinate reference system used for the recording of time.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createTemporalCRS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createTemporalCRS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateReferenceSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public TemporalCRS createTemporalCRS(final String code) throws FactoryException {
        if (isDefault(TemporalCRS.class)) {
            return super.createTemporalCRS(code);
        }
        return create(AuthorityFactoryProxy.TEMPORAL_CRS, code);
    }

    /**
     * Returns a CRS describing the position of points through two or more independent coordinate reference systems.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createCompoundCRS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCompoundCRS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateReferenceSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CompoundCRS createCompoundCRS(final String code) throws FactoryException {
        if (isDefault(CompoundCRS.class)) {
            return super.createCompoundCRS(code);
        }
        return create(AuthorityFactoryProxy.COMPOUND_CRS, code);
    }

    /**
     * Returns a CRS that is defined by its coordinate conversion from another CRS (not by a datum).
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createDerivedCRS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createDerivedCRS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateReferenceSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public DerivedCRS createDerivedCRS(final String code) throws FactoryException {
        if (isDefault(DerivedCRS.class)) {
            return super.createDerivedCRS(code);
        }
        return create(AuthorityFactoryProxy.DERIVED_CRS, code);
    }

    /**
     * Returns a 1-, 2- or 3-dimensional contextually local coordinate reference system.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createEngineeringCRS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createEngineeringCRS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateReferenceSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public EngineeringCRS createEngineeringCRS(final String code) throws FactoryException {
        if (isDefault(EngineeringCRS.class)) {
            return super.createEngineeringCRS(code);
        }
        return create(AuthorityFactoryProxy.ENGINEERING_CRS, code);
    }

    /**
     * Returns a 2-dimensional engineering coordinate reference system applied to locations in images.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createImageCRS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createImageCRS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateReferenceSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public ImageCRS createImageCRS(final String code) throws FactoryException {
        if (isDefault(ImageCRS.class)) {
            return super.createImageCRS(code);
        }
        return create(AuthorityFactoryProxy.IMAGE_CRS, code);
    }

    /**
     * Returns an arbitrary datum from a code. The returned object will typically be an
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createDatum(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createDatum(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createObject(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Datum createDatum(final String code) throws FactoryException {
        if (isDefault(Datum.class)) {
            return super.createDatum(code);
        }
        return create(AuthorityFactoryProxy.DATUM, code);
    }

    /**
     * Returns a datum defining the location and orientation of an ellipsoid that approximates the shape of the earth.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createGeodeticDatum(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createGeodeticDatum(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createDatum(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public GeodeticDatum createGeodeticDatum(final String code) throws FactoryException {
        if (isDefault(GeodeticDatum.class)) {
            return super.createGeodeticDatum(code);
        }
        return create(AuthorityFactoryProxy.GEODETIC_DATUM, code);
    }

    /**
     * Returns a datum identifying a particular reference level surface used as a zero-height surface.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createVerticalDatum(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createVerticalDatum(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createDatum(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public VerticalDatum createVerticalDatum(final String code) throws FactoryException {
        if (isDefault(VerticalDatum.class)) {
            return super.createVerticalDatum(code);
        }
        return create(AuthorityFactoryProxy.VERTICAL_DATUM, code);
    }

    /**
     * Returns a datum defining the origin of a temporal coordinate reference system.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createTemporalDatum(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createTemporalDatum(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createDatum(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public TemporalDatum createTemporalDatum(final String code) throws FactoryException {
        if (isDefault(TemporalDatum.class)) {
            return super.createTemporalDatum(code);
        }
        return create(AuthorityFactoryProxy.TEMPORAL_DATUM, code);
    }

    /**
     * Returns a datum defining the origin of an engineering coordinate reference system.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createEngineeringDatum(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createEngineeringDatum(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createDatum(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public EngineeringDatum createEngineeringDatum(final String code) throws FactoryException {
        if (isDefault(EngineeringDatum.class)) {
            return super.createEngineeringDatum(code);
        }
        return create(AuthorityFactoryProxy.ENGINEERING_DATUM, code);
    }

    /**
     * Returns a datum defining the origin of an image coordinate reference system.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createImageDatum(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createImageDatum(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createDatum(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public ImageDatum createImageDatum(final String code) throws FactoryException {
        if (isDefault(ImageDatum.class)) {
            return super.createImageDatum(code);
        }
        return create(AuthorityFactoryProxy.IMAGE_DATUM, code);
    }

    /**
     * Returns a geometric figure that can be used to describe the approximate shape of the earth.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createEllipsoid(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createEllipsoid(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createObject(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The ellipsoid for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Ellipsoid createEllipsoid(final String code) throws FactoryException {
        if (isDefault(Ellipsoid.class)) {
            return super.createEllipsoid(code);
        }
        return create(AuthorityFactoryProxy.ELLIPSOID, code);
    }

    /**
     * Returns a prime meridian defining the origin from which longitude values are determined.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createPrimeMeridian(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createPrimeMeridian(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createObject(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The prime meridian for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public PrimeMeridian createPrimeMeridian(final String code) throws FactoryException {
        if (isDefault(PrimeMeridian.class)) {
            return super.createPrimeMeridian(code);
        }
        return create(AuthorityFactoryProxy.PRIME_MERIDIAN, code);
    }

    /**
     * Returns information about spatial, vertical, and temporal extent (usually a domain of validity) from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createExtent(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createExtent(String)}
     *       method in the parent class.</li>
     * </ul>
     *
     * @return The extent for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Extent createExtent(final String code) throws FactoryException {
        if (isDefault(Extent.class)) {
            return super.createExtent(code);
        }
        return create(AuthorityFactoryProxy.EXTENT, code);
    }

    /**
     * Returns an arbitrary coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createCoordinateSystem(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCoordinateSystem(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createObject(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateSystem createCoordinateSystem(final String code) throws FactoryException {
        if (isDefault(CoordinateSystem.class)) {
            return super.createCoordinateSystem(code);
        }
        return create(AuthorityFactoryProxy.COORDINATE_SYSTEM, code);
    }

    /**
     * Returns a 2- or 3-dimensional coordinate system for geodetic latitude and longitude, sometime with ellipsoidal height.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createEllipsoidalCS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createEllipsoidalCS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public EllipsoidalCS createEllipsoidalCS(final String code) throws FactoryException {
        if (isDefault(EllipsoidalCS.class)) {
            return super.createEllipsoidalCS(code);
        }
        return create(AuthorityFactoryProxy.ELLIPSOIDAL_CS, code);
    }

    /**
     * Returns a 1-dimensional coordinate system for heights or depths of points.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createVerticalCS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createVerticalCS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public VerticalCS createVerticalCS(final String code) throws FactoryException {
        if (isDefault(VerticalCS.class)) {
            return super.createVerticalCS(code);
        }
        return create(AuthorityFactoryProxy.VERTICAL_CS, code);
    }

    /**
     * Returns a 1-dimensional coordinate system for heights or depths of points.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createTimeCS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createTimeCS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public TimeCS createTimeCS(final String code) throws FactoryException {
        if (isDefault(TimeCS.class)) {
            return super.createTimeCS(code);
        }
        return create(AuthorityFactoryProxy.TIME_CS, code);
    }

    /**
     * Returns a 2- or 3-dimensional Cartesian coordinate system made of straight orthogonal axes.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createCartesianCS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCartesianCS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CartesianCS createCartesianCS(final String code) throws FactoryException {
        if (isDefault(CartesianCS.class)) {
            return super.createCartesianCS(code);
        }
        return create(AuthorityFactoryProxy.CARTESIAN_CS, code);
    }

    /**
     * Returns a 3-dimensional coordinate system with one distance measured from the origin and two angular coordinates.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createSphericalCS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createSphericalCS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public SphericalCS createSphericalCS(final String code) throws FactoryException {
        if (isDefault(SphericalCS.class)) {
            return super.createSphericalCS(code);
        }
        return create(AuthorityFactoryProxy.SPHERICAL_CS, code);
    }

    /**
     * Returns a 3-dimensional coordinate system made of a polar coordinate system
     * extended by a straight perpendicular axis.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createCylindricalCS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCylindricalCS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CylindricalCS createCylindricalCS(final String code) throws FactoryException {
        if (isDefault(CylindricalCS.class)) {
            return super.createCylindricalCS(code);
        }
        return create(AuthorityFactoryProxy.CYLINDRICAL_CS, code);
    }

    /**
     * Returns a 2-dimensional coordinate system for coordinates represented by a distance from the origin
     * and an angle from a fixed direction.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createPolarCS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createPolarCS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public PolarCS createPolarCS(final String code) throws FactoryException {
        if (isDefault(PolarCS.class)) {
            return super.createPolarCS(code);
        }
        return create(AuthorityFactoryProxy.POLAR_CS, code);
    }

    /**
     * Returns a coordinate system axis with name, direction, unit and range of values.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createCoordinateSystemAxis(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCoordinateSystemAxis(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createObject(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The axis for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateSystemAxis createCoordinateSystemAxis(final String code) throws FactoryException {
        if (isDefault(CoordinateSystemAxis.class)) {
            return super.createCoordinateSystemAxis(code);
        }
        return create(AuthorityFactoryProxy.AXIS, code);
    }

    /**
     * Returns an unit of measurement from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createUnit(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createUnit(String)}
     *       method in the parent class.</li>
     * </ul>
     *
     * @return The unit of measurement for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Unit<?> createUnit(final String code) throws FactoryException {
        if (isDefault(Unit.class)) {
            return super.createUnit(code);
        }
        return create(AuthorityFactoryProxy.UNIT, code);
    }

    /**
     * Returns a definition of a single parameter used by an operation method.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createParameterDescriptor(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createParameterDescriptor(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createObject(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The parameter descriptor for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public ParameterDescriptor<?> createParameterDescriptor(final String code) throws FactoryException {
        if (isDefault(ParameterDescriptor.class)) {
            return super.createParameterDescriptor(code);
        }
        return create(AuthorityFactoryProxy.PARAMETER, code);
    }

    /**
     * Returns a description of the algorithm and parameters used to perform a coordinate operation.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createOperationMethod(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createOperationMethod(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createObject(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The operation method for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public OperationMethod createOperationMethod(final String code) throws FactoryException {
        if (isDefault(OperationMethod.class)) {
            return super.createOperationMethod(code);
        }
        return create(AuthorityFactoryProxy.METHOD, code);
    }

    /**
     * Returns an operation for transforming coordinates in the source CRS to coordinates in the target CRS.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createCoordinateOperation(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCoordinateOperation(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createObject(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return The operation for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateOperation createCoordinateOperation(final String code) throws FactoryException {
        if (isDefault(CoordinateOperation.class)) {
            return super.createCoordinateOperation(code);
        }
        return create(AuthorityFactoryProxy.OPERATION, code);
    }

    /**
     * The key objects to use in the {@link ConcurrentAuthorityFactory#cache}.
     * This is one of the following pairs of values:
     * <ul>
     *   <li>For all {@code ConcurrentAuthorityFactory.createFoo(String)} methods:
     *     <ol>
     *       <li>The {@code Foo} {@link Class} of the cached object.</li>
     *       <li>The authority code of the cached object.</li>
     *     </ol>
     *   </li>
     *   <li>For {@link ConcurrentAuthorityFactory#createFromCoordinateReferenceSystemCodes(String, String)}:
     *     <ol>
     *       <li>The authority code of source CRS (stored in the "type" field even if the name is not right).</li>
     *       <li>The authority code of target CRS (stored in the "code" field).</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-2">GEOTK-2</a>
     */
    private static final class Key {
        /** The type of the cached object.    */ final Object type;
        /** The cached object authority code. */ final String code;

        /** Creates a new key for the given type and code. */
        Key(final Object type, final String code) {
            this.type = type;
            this.code = code;
        }

        /** Returns the hash code value for this key. */
        @Override public int hashCode() {
            return type.hashCode() ^ code.hashCode();
        }

        /** Compares this key with the given object for equality .*/
        @Override public boolean equals(final Object other) {
            if (other instanceof Key) {
                final Key that = (Key) other;
                return type.equals(that.type) && code.equals(that.code);
            }
            return false;
        }

        /** String representation used by {@link CacheRecord}. */
        @Override @Debug public String toString() {
            final StringBuilder buffer = new StringBuilder();
            if (type instanceof Class<?>) {
                buffer.append("Code[“").append(code);
                if (buffer.length() > 15) { // Arbitrary limit in string length.
                    buffer.setLength(15);
                    buffer.append('…');
                }
                buffer.append("” : ").append(((Class<?>) type).getSimpleName());
            } else {
                buffer.append("CodePair[“").append(type).append("” → “").append(code).append('”');
            }
            return buffer.append(']').toString();
        }
    }

    /**
     * Returns an object from a code using the given proxy. This method first checks in the cache.
     * If no object exists in the cache for the given code, then a lock is created and the object
     * creation is delegated to the {@linkplain #getDataAccess() Data Access Object}.
     * The result is then stored in the cache and returned.
     *
     * @param  <T>   The type of the object to be returned.
     * @param  proxy The proxy to use for creating the object.
     * @param  code  The code of the object to create.
     * @return The object extracted from the cache or created.
     * @throws FactoryException If an error occurred while creating the object.
     */
    private <T> T create(final AuthorityFactoryProxy<T> proxy, final String code) throws FactoryException {
        ArgumentChecks.ensureNonNull("code", code);
        final Class<T> type = proxy.type;
        final Key key = new Key(type, normalizeCode(code));
        Object value = cache.peek(key);
        if (!type.isInstance(value)) {
            final Cache.Handler<Object> handler = cache.lock(key);
            try {
                value = handler.peek();
                if (!type.isInstance(value)) {
                    final T result;
                    final DAO factory = getDataAccess();
                    try {
                        result = proxy.create(factory, key.code);
                    } finally {
                        release(null, type, code);
                    }
                    value = result;     // For the finally block below.
                    return result;
                }
            } finally {
                handler.putAndUnlock(value);
            }
        }
        return type.cast(value);
    }

    /**
     * Returns operations from source and target coordinate reference system codes.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached collection for the given pair of codes if such collection already exists.</li>
     *   <li>Otherwise:
     *     <ol>
     *       <li>get an instance of the Data Access Object,</li>
     *       <li>delegate to its {@link GeodeticAuthorityFactory#createFromCoordinateReferenceSystemCodes(String, String)} method,</li>
     *       <li>release the Data Access Object — <em>this step assumes that the collection obtained at step 2
     *           is still valid after the Data Access Object has been released</em>,</li>
     *       <li>cache the result — <em>this step assumes that the collection obtained at step 2 is immutable</em>.</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * @return The operations from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<CoordinateOperation> createFromCoordinateReferenceSystemCodes(
            final String sourceCRS, final String targetCRS) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        final Key key = new Key(normalizeCode(sourceCRS), normalizeCode(targetCRS));
        Object value = cache.peek(key);
        if (!(value instanceof Set<?>)) {
            final Cache.Handler<Object> handler = cache.lock(key);
            try {
                value = handler.peek();
                if (!(value instanceof Set<?>)) {
                    final DAO factory = getDataAccess();
                    try {
                        value = factory.createFromCoordinateReferenceSystemCodes(sourceCRS, targetCRS);
                    } finally {
                        release("createFromCoordinateReferenceSystemCodes", CoordinateOperation.class, null);
                    }
                }
            } finally {
                handler.putAndUnlock(value);
            }
        }
        return (Set<CoordinateOperation>) value;
    }

    /**
     * Returns a finder which can be used for looking up unidentified objects.
     * The default implementation delegates lookup to the underlying Data Access Object and caches the result.
     *
     * @return A finder to use for looking up unidentified objects.
     * @throws FactoryException if the finder can not be created.
     */
    @Override
    public IdentifiedObjectFinder newIdentifiedObjectFinder() throws FactoryException {
        return new Finder(this);
    }

    /**
     * An implementation of {@link IdentifiedObjectFinder} which delegates
     * the work to the underlying Data Access Object and caches the result.
     *
     * <div class="section">Synchronization note</div>
     * our public API claims that {@link IdentifiedObjectFinder}s are not thread-safe.
     * Nevertheless we synchronize this particular implementation for safety, because the consequence of misuse
     * are more dangerous than other implementations. Furthermore this is also a way to assert that no code path
     * go to the {@link #create(AuthorityFactoryProxy, String)} method from a non-overridden public method.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    private static final class Finder extends IdentifiedObjectFinder {
        /**
         * The finder on which to delegate the work. This is acquired by {@link #acquire()}
         * <strong>and must be released</strong> by call to {@link #release()} once finished.
         */
        private transient IdentifiedObjectFinder finder;

        /**
         * Number of time that {@link #acquire()} has been invoked.
         * When this count reaches zero, the {@linkplain #finder} is released.
         */
        private transient int acquireCount;

        /**
         * The object in process of being searched, for information purpose only.
         */
        private transient IdentifiedObject searching;

        /**
         * Creates a finder for the given type of objects.
         */
        Finder(final ConcurrentAuthorityFactory<?> factory) {
            super(factory);
        }

        /**
         * Acquires a new {@linkplain #finder}.
         * The {@link #release()} method must be invoked in a {@code finally} block after the call to {@code acquire}.
         * The pattern must be as below (note that the call to {@code acquire()} is inside the {@code try} block):
         *
         * {@preformat java
         *     try {
         *         acquire();
         *         (finder or proxy).doSomeStuff();
         *     } finally {
         *         release();
         *     }
         * }
         */
        private void acquire() throws FactoryException {
            assert Thread.holdsLock(this);
            assert (acquireCount == 0) == (finder == null) : acquireCount;
            if (acquireCount == 0) {
                final GeodeticAuthorityFactory delegate = ((ConcurrentAuthorityFactory<?>) factory).getDataAccess();
                /*
                 * Set 'acquireCount' only after we succeed in fetching the factory, and before any operation on it.
                 * The intend is to get ConcurrentAuthorityFactory.release() invoked if and only if the getDataAccess()
                 * method succeed, no matter what happen after this point.
                 */
                acquireCount = 1;
                finder = delegate.newIdentifiedObjectFinder();
                finder.setWrapper(this);
            } else {
                acquireCount++;
            }
        }

        /**
         * Releases the {@linkplain #finder}.
         */
        private void release() {
            assert Thread.holdsLock(this);
            if (acquireCount == 0) {
                // May happen only if a failure occurred during getDataAccess() execution.
                return;
            }
            if (--acquireCount == 0) {
                finder = null;
                ((ConcurrentAuthorityFactory<?>) factory).release(null, null, null);
            }
        }

        /**
         * Returns a set of authority codes that <strong>may</strong> identify the same
         * object than the specified one. This method delegates to the data access object.
         */
        @Override
        protected synchronized Set<String> getCodeCandidates(final IdentifiedObject object) throws FactoryException {
            try {
                acquire();
                return finder.getCodeCandidates(object);
            } finally {
                release();
            }
        }

        /**
         * Returns the cached value for the given object, or {@code null} if none.
         */
        @Override
        final Set<IdentifiedObject> getFromCache(final IdentifiedObject object) {
            final Map<IdentifiedObject,FindEntry> findPool = ((ConcurrentAuthorityFactory<?>) factory).findPool;
            synchronized (findPool) {
                final FindEntry entry = findPool.get(object);
                if (entry != null) {
                    // 'finder' may be null if this method is invoked directly by this Finder.
                    return entry.get((finder != null ? finder : this).isIgnoringAxes());
                }
            }
            return null;
        }

        /**
         * Stores the given result in the cache.
         * This method shall be invoked only when {@link #getSearchDomain()} is not {@link Domain#DECLARATION}.
         */
        @Override
        final Set<IdentifiedObject> cache(final IdentifiedObject object, Set<IdentifiedObject> result) {
            final Map<IdentifiedObject,FindEntry> findPool = ((ConcurrentAuthorityFactory<?>) factory).findPool;
            result = CollectionsExt.unmodifiableOrCopy(result);
            FindEntry entry = new FindEntry();
            synchronized (findPool) {
                final FindEntry c = JDK8.putIfAbsent(findPool, object, entry);
                if (c != null) {
                    entry = c;      // May happen if the same set has been computed in another thread.
                }
                // 'finder' should never be null since this method is not invoked directly by this Finder.
                result = entry.set(finder.isIgnoringAxes(), result, object == searching);
            }
            return result;
        }

        /**
         * Looks up an object from this authority factory which is approximatively equal to the specified object.
         * The default implementation performs the same lookup than the Data Access Object and caches the result.
         */
        @Override
        public Set<IdentifiedObject> find(final IdentifiedObject object) throws FactoryException {
            Set<IdentifiedObject> candidate = getFromCache(object);
            if (candidate == null) {
                /*
                 * Nothing has been found in the cache. Delegates the search to the Data Access Object.
                 * Note that the Data Access Object will itself callbacks our 'cache(…)' method, so there
                 * is no need that we cache the result here.
                 */
                synchronized (this) {
                    try {
                        acquire();
                        searching = object;
                        candidate = finder.find(object);
                    } finally {
                        searching = null;
                        release();
                    }
                }
            }
            return candidate;
        }
    }

    /**
     * Cache for the result of {@link IdentifiedObjectFinder#find(IdentifiedObject)} operations.
     * All access to this object must be done in a block synchronized on {@link #findPool}.
     */
    private static final class FindEntry {
        /** Result of the search with our without ignoring axes. */
        private Set<IdentifiedObject> strict, lenient;

        /** Whether the cache is the result of an explicit request instead than a dependency search. */
        private boolean explicitStrict, explicitLenient;

        /** Returns the cached instance. */
        Set<IdentifiedObject> get(final boolean ignoreAxes) {
            return ignoreAxes ? lenient : strict;
        }

        /** Cache an instance, or return previous instance if computed concurrently. */
        @SuppressWarnings({"AssignmentToCollectionOrArrayFieldFromParameter", "ReturnOfCollectionOrArrayField"})
        Set<IdentifiedObject> set(final boolean ignoreAxes, Set<IdentifiedObject> result, final boolean explicit) {
            if (ignoreAxes) {
                if (lenient != null) {
                    result = lenient;
                } else {
                    lenient = result;
                }
                explicitLenient |= explicit;
            } else {
                if (strict != null) {
                    result = strict;
                } else {
                    strict = result;
                }
                explicitStrict |= explicit;
            }
            return result;
        }

        /** Forgets the set that were not explicitely requested. */
        boolean cleanup() {
            if (!explicitStrict)  strict  = null;
            if (!explicitLenient) lenient = null;
            return (strict == null) && (lenient == null);
        }
    }

    /**
     * Prints the cache content to the given writer.
     * Keys are sorted by numerical order if possible, or alphabetical order otherwise.
     * This method is used for debugging purpose only.
     *
     * @param out The output printer, or {@code null} for the {@linkplain System#out standard output stream}.
     */
    @Debug
    public void printCacheContent(final PrintWriter out) {
        CacheRecord.printCacheContent(cache, out);
    }

    /**
     * A hook to be executed either when the {@link ConcurrentAuthorityFactory} is collected by the garbage collector,
     * when the Java Virtual Machine is shutdown, or when the module is uninstalled by the OSGi or Servlet container.
     *
     * <p><strong>Do not keep reference to the enclosing factory</strong> - in particular,
     * this class must not be static - otherwise the factory would never been garbage collected.</p>
     */
    private static final class ShutdownHook<DAO extends GeodeticAuthorityFactory>           // MUST be static!
            extends PhantomReference<ConcurrentAuthorityFactory<DAO>> implements Disposable, Callable<Object>
    {
        /**
         * The {@link ConcurrentAuthorityFactory#availableDAOs} queue.
         */
        private final Deque<DataAccessRef<DAO>> availableDAOs;

        /**
         * Creates a new shutdown hook for the given factory.
         */
        ShutdownHook(final ConcurrentAuthorityFactory<DAO> factory) {
            super(factory, ReferenceQueueConsumer.QUEUE);
            availableDAOs = factory.availableDAOs;
        }

        /**
         * Invoked indirectly by the garbage collector when the {@link ConcurrentAuthorityFactory} is disposed.
         */
        @Override
        public void dispose() {
            Shutdown.unregister(this);
            try {
                call();
            } catch (Exception exception) {
                /*
                 * Pretend that the exception is logged by ConcurrentAuthorityFactory.finalize().
                 * This is not true, but carries the idea that the error occurred while cleaning
                 * ConcurrentAuthorityFactory after garbage collection.
                 */
                unexpectedException("finalize", exception);
            }
        }

        /**
         * Invoked at JVM shutdown time, or when the container (OSGi or Servlet) uninstall the bundle containing SIS.
         */
        @Override
        public Object call() throws Exception {
            final List<DAO> factories;
            synchronized (availableDAOs) {
                factories = ConcurrentAuthorityFactory.clear(availableDAOs);
            }
            // No call to confirmClose(List<DAO>) as we want to force close.
            close(factories);
            return null;
        }
    }

    /**
     * Clears the given queue and returns all DAO instances that it contained.
     * The given queue shall be the {@link ConcurrentAuthorityFactory#availableDAOs} queue.
     *
     * @param availableDAOs The queue of factories to close.
     */
    static <DAO extends GeodeticAuthorityFactory> List<DAO> clear(final Deque<DataAccessRef<DAO>> availableDAOs) {
        assert Thread.holdsLock(availableDAOs);
        final List<DAO> factories = new ArrayList<DAO>(availableDAOs.size());
        DataAccessRef<DAO> dao;
        while ((dao = availableDAOs.pollFirst()) != null) {
            factories.add(dao.factory);
        }
        return factories;
    }

    /**
     * Invokes {@link AutoCloseable#close()} on all the given factories.
     * Exceptions will be collected and rethrown only after all factories have been closed.
     *
     * @param  factories The factories to close.
     * @param  count Number of valid elements in the {@code factories} array.
     * @throws Exception the exception thrown by the first factory that failed to close.
     */
    static <DAO extends GeodeticAuthorityFactory> void close(final List<DAO> factories) throws Exception {
        Exception exception = null;
        for (int i=factories.size(); --i>=0;) {
            final DAO factory = factories.get(i);
            if (JDK7.isAutoCloseable(factory)) try {
                JDK7.close(factory);
            } catch (Exception e) {
                if (exception == null) {
                    exception = e;
                } else {
                    // exception.addSuppressed(e) on the JDK7 branch.
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Immediately closes all Data Access Objects that are closeable.
     * This method does not clear the cache and does not disallow further usage of this factory:
     * this {@code ConcurrentAuthorityFactory} can still be used as usual after it has been "closed".
     * {@linkplain #newDataAccess() New Data Access Objects} will be created if needed for replacing
     * the ones closed by this method.
     *
     * <p>The main purpose of this method is to force immediate release of JDBC connections or other kind of resources
     * that Data Access Objects may hold. If this method is not invoked, Data Access Objects will be closed
     * when this {@code ConcurrentAuthorityFactory} will be garbage collected or at JVM shutdown time,
     * depending which event happen first.</p>
     *
     * @throws FactoryException if an error occurred while closing the Data Access Objects.
     */
    public void close() throws FactoryException {
        try {
            final List<DAO> factories;
            synchronized (availableDAOs) {
                factories = clear(availableDAOs);
            }
            confirmClose(factories);
            close(factories);                       // Must be invoked outside the synchronized block.
        } catch (Exception e) {
            if (e instanceof FactoryException) {
                throw (FactoryException) e;
            } else {
                throw new FactoryException(e);
            }
        }
    }

    /**
     * Returns a string representation of this factory for debugging purpose only.
     * The string returned by this method may change in any future SIS version.
     *
     * @return A string representation for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        final String s = super.toString();
        DataAccessRef<DAO> usage = currentDAO.get();
        if (usage == null) {
            synchronized (availableDAOs) {
                usage = availableDAOs.peekLast();
            }
            if (usage == null) {
                return s;
            }
        }
        return s + JDK7.lineSeparator() + usage;
    }

    /**
     * Completes the string representation of this factory for debugging purpose only.
     * The string formatted by this method may change in any future SIS version.
     */
    @Debug
    @Override
    final void toString(final StringBuilder buffer) {
        buffer.append(", cache=").append(cache.size()).append(", DAO=");
        synchronized (availableDAOs) {
            buffer.append(availableDAOs.size());
            if (remainingDAOs <= 0) {
                buffer.append(" (limit reached)");
            }
        }
    }
}
