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
import java.util.Optional;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.WeakHashMap;
import java.util.IdentityHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.lang.ref.WeakReference;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import javax.measure.Unit;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Printable;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.logging.PerformanceLevel;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.privy.CollectionsExt;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.system.Cleaners;
import org.apache.sis.system.DelayedExecutor;
import org.apache.sis.system.DelayedRunnable;
import org.apache.sis.system.Configuration;
import org.apache.sis.system.Shutdown;

// Specific to the main and geoapi-3.1 branches:
import org.apache.sis.util.collection.BackingStoreException;


/**
 * A concurrent authority factory that caches all objects created by another factory.
 * All {@code createFoo(String)} methods first check if a previously created object exists for the given code.
 * If such object exists, it is returned. Otherwise, the object creation is delegated to another factory given
 * by {@link #newDataAccess()} and the result is cached in this factory.
 *
 * <p>{@code ConcurrentAuthorityFactory} delays the call to {@code newDataAccess()} until first needed,
 * and {@linkplain AutoCloseable#close() closes} the factory used as a <i>Data Access Object</i>
 * (DAO) after some timeout. This approach allows to establish a connection to a database (for example)
 * and keep it only for a relatively short amount of time.</p>
 *
 * <h2>Caching strategy</h2>
 * Objects are cached by strong references, up to the number of objects specified at construction time.
 * If a greater number of objects are cached, then the oldest ones will be retained through a
 * {@linkplain WeakReference weak reference} instead of a strong one.
 * This means that this caching factory will continue to return those objects as long as they are in use somewhere
 * else in the Java virtual machine, but will be discarded (and recreated on the fly if needed) otherwise.
 *
 * <h2>Multi-threading</h2>
 * The cache managed by this class is concurrent. However, the Data Access Objects (DAO) are assumed non-concurrent.
 * If two or more threads are accessing this factory at the same time, then two or more Data Access Object instances
 * may be created. The maximal number of instances to create is specified at {@code ConcurrentAuthorityFactory}
 * construction time. If more Data Access Object instances are needed, some of the threads will block until an
 * instance become available.
 *
 * <h2>Note for subclasses</h2>
 * This abstract class does not implement any of the {@link DatumAuthorityFactory}, {@link CSAuthorityFactory},
 * {@link CRSAuthorityFactory} and {@link CoordinateOperationAuthorityFactory} interfaces.
 * Subclasses should select the interfaces that they choose to implement.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @param <DAO>  the type of factory used as Data Access Object (DAO).
 *
 * @since 0.7
 */
public abstract class ConcurrentAuthorityFactory<DAO extends GeodeticAuthorityFactory>
        extends GeodeticAuthorityFactory implements AutoCloseable, Printable
{
    /**
     * Duration of data access operations that should be logged, in nanoseconds.
     * Any operation that take longer than this amount of time to execute will have a message logged.
     * The log level depends on the execution duration as specified in {@link PerformanceLevel}.
     *
     * <h4>Rational</h4>
     * We do not unconditionally log all creation messages because they are redundant with more detailed
     * logs produced by {@link GeodeticObjectFactory}. Their only additional information is the creation
     * duration, which is not very useful if too close to zero.
     */
    @Configuration
    private static final long DURATION_FOR_LOGGING = 10_000_000L;       // 10 milliseconds.

    /**
     * The authority, cached after first requested.
     *
     * @see #getAuthority()
     */
    private transient volatile Citation authority;

    /**
     * The {@code createFoo(String)} methods that are <strong>not</strong> overridden in the Data Access Object (DAO).
     * This map is created at construction time and should not be modified after construction.
     *
     * @see #isDefault(Class)
     */
    private final Map<Class<?>,Boolean> inherited = new IdentityHashMap<>();

    /**
     * The pool of cached objects. Keys are (type, code) tuples; the type is stored because the same code
     * may be used for different kinds of objects. Values are usually instances of {@link IdentifiedObject},
     * but can also be instances of unrelated types such as {@link Extent}.
     */
    private final Cache<Key,Object> cache;

    /**
     * The pool of objects identified by {@link Finder#find(IdentifiedObject)}.
     * Values may be an empty set if an object has been searched but has not been found.
     *
     * <p>Keys are typically "foreigner" objects (not objects retained in the {@linkplain #cache}),
     * otherwise we would not need to search for their authority code. Because keys are retained by
     * weak references, some strong references to the identified objects should be kept outside.
     * This is the purpose of {@link #findPoolLatestQueries}.</p>
     *
     * <p>Every access to this pool must be synchronized on {@code findPool}.</p>
     */
    private final Map<IdentifiedObject, Set<IdentifiedObject>[]> findPool = new WeakHashMap<>();

    /**
     * The most recently used objects stored or accessed in {@link #findPool}, retained by strong references for
     * preventing too early garbage collection. Instances put there are generally not in the {@linkplain #cache}
     * because they are "foreigner" objects, possibly created by different authorities. Those strong references
     * are automatically clearer in a background thread after an arbitrary delay.
     */
    private final ReferenceKeeper findPoolLatestQueries = new ReferenceKeeper();

    /**
     * Holds the reference to a Data Access Object used by {@link ConcurrentAuthorityFactory}, together with
     * information about its usage. In a mono-thread application, there is typically only one {@code DataAccessRef}
     * instance at a given time. However if more than one than one thread are requesting new objects concurrently,
     * then many instances may exist for the same {@code ConcurrentAuthorityFactory}.
     *
     * <p>If the Data Access Object is currently in use, then {@code DataAccessRef} counts how many recursive
     * invocations of a {@link #factory} {@code createFoo(String)} method is under way in the current thread.
     * This information is used in order to reuse the same factory instead of creating new instances
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
         * Incremented on every call to {@link ConcurrentAuthorityFactory#getDataAccess()} and decremented on every
         * call to {@link ConcurrentAuthorityFactory#release(String, Class, String)}. When this value reach zero,
         * the factory is really released.
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
        @Override
        public String toString() {
            final String text;
            final Number value;
            if (depth != 0) {
                text = "%s in use at depth %d";
                value = depth;
            } else {
                text = "%s made available %d seconds ago";
                value = Math.round((System.nanoTime() - timestamp) / (double) Constants.NANOS_PER_SECOND);
            }
            return String.format(text, Classes.getShortClassName(factory), value);
        }
    }

    /**
     * The Data Access Object in use by the current thread.
     */
    private final ThreadLocal<DataAccessRef<DAO>> currentDAO = new ThreadLocal<>();

    /**
     * The Data Access Object instances previously created and released for future reuse.
     * Last used factories must be {@linkplain Deque#addLast(Object) added last}.
     * This is used as a LIFO stack.
     */
    private final Deque<DataAccessRef<DAO>> availableDAOs = new LinkedList<>();

    /**
     * The number of Data Access Objects that can still be created. This number is decremented in a block
     * synchronized on {@link #availableDAOs} every time a Data Access Object is in use, and incremented
     * once released.
     */
    private int remainingDAOs;

    /**
     * {@code true} if the call to {@link #closeExpired()} is scheduled for future execution in the background
     * cleaner thread. A value of {@code true} implies that this factory contains at least one active data access.
     * However, the reciprocal is not true: this field may be set to {@code false} while a DAO is currently in use
     * because this field is set to {@code true} only when a worker factory is {@linkplain #release released}.
     *
     * <p>Note that we cannot use {@code !availableDAOs.isEmpty()} as a replacement of {@code isCleanScheduled}
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
     * @see #setTimeout(long, TimeUnit)
     */
    @Configuration(writeAccess = Configuration.Access.INSTANCE)
    private long timeout = 60_000_000_000L;     // 1 minute

    /**
     * The maximal difference between the scheduled time and the actual time in order to perform the factory disposal,
     * in nanoseconds. This is used as a tolerance value for possible wait time inaccuracy.
     */
    static final long TIMEOUT_RESOLUTION = 200_000_000L;    // 0.2 second

    /**
     * Constructs an instance with a default number of threads and a default number of entries to keep
     * by strong references. Note that those default values may change in any future SIS versions based
     * on experience gained.
     *
     * @param  dataAccessClass  the class of Data Access Object (DAO) created by {@link #newDataAccess()}.
     */
    protected ConcurrentAuthorityFactory(Class<DAO> dataAccessClass) {
        this(dataAccessClass, 100, 8);
        /*
         * NOTE 1: the number of strong references (100) seems high compared to the number of CRSs typically used,
         * but a lot of objects are created when using the EPSG database (datum, prime meridian, parameters, axes,
         * extents, etc).
         *
         * NOTE 2: if the default maximum number of Data Access Objects (currently 8) is augmented,
         * make sure to augment the number of runner threads in the "StressTest" class to a greater amount.
         */
    }

    /**
     * Constructs an instance with the specified number of entries to keep by strong references.
     * If a number of object greater than {@code maxStrongReferences} are created, then the strong references
     * for the eldest objects will be replaced by weak references.
     *
     * @param dataAccessClass       the class of Data Access Object (DAO) created by {@link #newDataAccess()}.
     * @param maxStrongReferences   the maximum number of objects to keep by strong reference.
     * @param maxConcurrentQueries  the maximal number of Data Access Objects to use concurrently.
     *        If more than this number of threads are querying this {@code ConcurrentAuthorityFactory} concurrently,
     *        additional threads will be blocked until a Data Access Object become available.
     */
    @SuppressWarnings("this-escape")        // Phantom reference.
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
         * Create a cache using soft references and allowing key collisions.
         *
         * NOTE 1: key collision is usually an error. But in this case we allow them in order to enable recursion.
         * If during the creation of an object the program asks to this ConcurrentAuthorityFactory for the same object
         * (using the same key), then the default Cache implementation considers that situation as an error unless the
         * above property has been set to `true`.
         *
         * NOTE 2: the number of temporary objects created during a `search` operation is high (can be thousands),
         * and the use of weak references may cause the next search to be almost as costly as the first search if
         * temporary objects have been already garbage collected. We use soft references instead of weak references
         * for avoiding that problem, because search operations occur often.
         */
        remainingDAOs = maxConcurrentQueries;
        cache = new Cache<>(20, maxStrongReferences, true);
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
        final var closer = new ShutdownHook<>(availableDAOs);
        Cleaners.SHARED.register(this, closer);
        Shutdown.register(closer);
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
     * <h4>Multi-threading</h4>
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
    private DAO getDataAccess() throws FactoryException {
        /*
         * First checks if the current thread is already using a factory. If yes, we will
         * avoid creating new factories on the assumption that factories are reentrant.
         */
        DataAccessRef<DAO> usage = currentDAO.get();
        if (usage == null) {
            synchronized (availableDAOs) {
                /*
                 * If we have reached the maximal number of Data Access Objects allowed, wait for an instance
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
                remainingDAOs--;                            // Should be done last when we are sure to not fail.
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
                        var e = new UnavailableFactoryException(
                                Errors.format(Errors.Keys.FactoryNotFound_1, GeodeticAuthorityFactory.class));
                        e.setUnavailableFactory(this);
                        throw e;
                    }
                    usage = new DataAccessRef<>(factory);
                }
                assert usage.depth == 0 : usage;
                usage.timestamp = System.nanoTime();
            } catch (Throwable e) {
                /*
                 * If any kind of error occurred, restore the `remainingDAO` field as if no code were executed.
                 * This code would not have been needed if we were allowed to decrement `remainingDAO` only as
                 * the very last step (when we know that everything else succeed).
                 * But it needed to be decremented inside the synchronized block.
                 */
                synchronized (availableDAOs) {
                    remainingDAOs++;
                }
                throw e;
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
     * @param  caller  the caller method, or {@code null} for {@code "create" + type.getSimpleName()}.
     * @param  type    the type of the created object, or {@code null} for performing no logging.
     * @param  code    the code of the created object, or {@code null} if none.
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
             * Log the event. Note: there is no need to check for `Semaphores.FINER_OBJECT_CREATION_LOGS`
             * because this method is not invoked, or is invoked with `type = null`, during execution of
             * `IdentifiedObjectFinder` search operations. The only new information in this log compared
             * to `GeodeticObjectFactory` logs is the creation duration, not useful if too close to zero
             * and always useful if too long.
             */
            if (time >= DURATION_FOR_LOGGING && type != null) {
                if (caller == null) {
                    caller = "create".concat(type.getSimpleName());
                }
                final Level level = PerformanceLevel.forDuration(time, TimeUnit.NANOSECONDS);
                final Double duration = time / (double) Constants.NANOS_PER_SECOND;
                final Messages resources = Messages.forLocale(null);
                final LogRecord record;
                if (code != null) {
                    record = resources.createLogRecord(level, Messages.Keys.CreateDurationFromIdentifier_3, type, code, duration);
                } else {
                    record = resources.createLogRecord(level, Messages.Keys.CreateDuration_2, type, duration);
                }
                Logging.completeAndLog(LOGGER, getClass(), caller, record);
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
     * However, the reciprocal is not true: this field may be set to {@code false} while a DAO is currently in use
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
                recycle(new DataAccessRef<>(factory));
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
     * This method may reschedule the task again for another execution if it appears that at least one
     * Data Access Object was not ready for disposal.
     *
     * @see #close()
     */
    private void closeExpired() {
        final List<DAO> factories;
        final boolean isEmpty;
        synchronized (availableDAOs) {
            factories = new ArrayList<>(availableDAOs.size());
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
             * In the latter case, release() will reschedule a new task.
             */
            isCleanScheduled = !(isEmpty = availableDAOs.isEmpty());
        }
        /*
         * We must close the factories from outside the synchronized block.
         */
        try {
            confirmClose(factories);
            close(factories);
        } catch (Exception exception) {
            unexpectedException("closeExpired", Exceptions.unwrap(exception));
        }
        /*
         * If the queue of Data Access Objects (DAO) become empty, this means that this `ConcurrentAuthorityFactory`
         * has not created new objects for a while (at least the amount of time given by the timeout), ignoring any
         * request which may be under execution in another thread right now. We may use this opportunity for reducing
         * the number of objects retained in the `IdentifiedObjectFinder.find(…)` cache (maybe in a future version).
         *
         * Touching `findPool` also has the desired side-effect of letting WeakHashMap expunges stale entries.
         */
        if (isEmpty) {
            synchronized (findPool) {
                findPool.size();        // Cause a call to `expungeStaleEntries()`.
            }
        }
    }

    /**
     * Invoked when an exception occurred while closing a factory and we cannot propagate the exception to the user.
     * This situation happen when the factories are closed in a background thread. There is not much we can do except
     * logging the problem. {@code ConcurrentAuthorityFactory} should be able to continue its work normally since the
     * factory that we failed to close will not be used anymore.
     *
     * @param  method     the name of the method to report as the source of the problem.
     * @param  exception  the exception that occurred while closing a Data Access Object.
     */
    static void unexpectedException(final String method, final Exception exception) {
        Logging.unexpectedException(LOGGER, ConcurrentAuthorityFactory.class, method, exception);
    }

    /**
     * Returns {@code true} if the given Data Access Object (DAO) can be closed. This method is invoked automatically
     * after the {@linkplain #getTimeout timeout} if the given DAO has been idle during all that time.
     * Subclasses can override this method and return {@code false} if they want to prevent the DAO disposal
     * under some circumstances.
     *
     * <p>The default implementation always returns {@code true}.</p>
     *
     * @param  factory  the Data Access Object which is about to be closed.
     * @return {@code true} if the given Data Access Object can be closed.
     *
     * @see #close()
     */
    protected boolean canClose(DAO factory) {
        return true;
    }

    /**
     * Returns the amount of time that {@code ConcurrentAuthorityFactory} will wait before to close a Data Access Object.
     * This delay is measured from the last time the Data Access Object has been used by a {@code createFoo(String)} method.
     *
     * @param  unit  the desired unit of measurement for the timeout.
     * @return the current timeout in the given unit of measurement.
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
     * @param  delay  the delay of inactivity before to close a Data Access Object.
     * @param  unit   the unit of measurement of the given delay.
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
     * @return the organization responsible for definition of the database, or {@code null} if unavailable.
     */
    @Override
    public Citation getAuthority() {
        Citation c = authority;
        if (c == null) try {
            final DAO factory = getDataAccess();
            try {
                /*
                 * Cache only in case of success. If we failed, we
                 * will try again next time this method is invoked.
                 */
                authority = c = factory.getAuthority();
            } finally {
                release("getAuthority", Citation.class, null);
            }
        } catch (FactoryException e) {
            throw new BackingStoreException(e);
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
     * @param  type  the spatial reference objects type (e.g. {@code ProjectedCRS.class}).
     * @return the set of authority codes for spatial reference objects of the given type.
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
     *   <li>delegate to its {@link GeodeticAuthorityFactory#getDescriptionText(Class, String)} method,</li>
     *   <li>release the Data Access Object.</li>
     * </ol>
     *
     * @param  code  value allocated by authority.
     * @return a description of the object, or {@code null} if the object
     *         corresponding to the specified {@code code} has no description.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the query failed for some other reason.
     *
     * @since 1.5
     */
    @Override
    public Optional<InternationalString> getDescriptionText(Class<? extends IdentifiedObject> type, String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        final DAO factory = getDataAccess();
        try {
            return factory.getDescriptionText(type, code);
        } finally {
            release("getDescriptionText", InternationalString.class, code);
        }
    }

    /**
     * Returns {@code true} if the Data Access Object (DAO) does not provide a {@code create} method for the
     * given type of object. The intent is to decide if the caller should delegate to the DAO or delegate to
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
     * @param  code  the code to normalize.
     * @return the normalized code.
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
     * @return the object for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    @SuppressWarnings("removal")
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
     * @return the coordinate reference system for the given code.
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
     * @return the coordinate reference system for the given code.
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
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createGeodeticCRS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createGeodeticCRS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateReferenceSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @since 1.5
     */
    @Override
    public GeodeticCRS createGeodeticCRS(final String code) throws FactoryException {
        if (isDefault(GeodeticCRS.class)) {
            return super.createGeodeticCRS(code);
        }
        return create(AuthorityFactoryProxy.GEODETIC_CRS, code);
    }

    /**
     * Returns a 3-dimensional coordinate reference system with the origin at the approximate centre of mass of the earth.
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @deprecated ISO 19111:2019 does not define an explicit class for geocentric CRS.
     * Use {@link #createGeodeticCRS(String)} instead.
     */
    @Override
    @Deprecated(since = "2.0")  // Temporary version number until this branch is released.
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
     * @return the coordinate reference system for the given code.
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
     * @return the coordinate reference system for the given code.
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
     * @return the coordinate reference system for the given code.
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
     * Returns a 1-dimensional coordinate reference system which uses parameter values or functions.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createParametricCRS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createParametricCRS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateReferenceSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @since 1.4
     */
    @Override
    public ParametricCRS createParametricCRS(final String code) throws FactoryException {
        if (isDefault(ParametricCRS.class)) {
            return super.createParametricCRS(code);
        }
        return create(AuthorityFactoryProxy.PARAMETRIC_CRS, code);
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
     * @return the coordinate reference system for the given code.
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
     * @return the coordinate reference system for the given code.
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
     * @return the coordinate reference system for the given code.
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
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @deprecated The {@code ImageCRS} class has been removed in ISO 19111:2019.
     *             It is replaced by {@code EngineeringCRS}.
     */
    @Override
    @Deprecated(since = "1.5")
    public ImageCRS createImageCRS(final String code) throws FactoryException {
        if (isDefault(ImageCRS.class)) {
            return super.createImageCRS(code);
        }
        return create(AuthorityFactoryProxy.IMAGE_CRS, code);
    }

    /**
     * Returns an arbitrary datum ensemble from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createDatumEnsemble(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createDatumEnsemble(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createObject(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return the datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @since 1.5
     */
    @Override
    public DatumEnsemble<?> createDatumEnsemble(final String code) throws FactoryException {
        if (isDefault(DatumEnsemble.class)) {
            return super.createDatumEnsemble(code);
        }
        return create(AuthorityFactoryProxy.ENSEMBLE, code);
    }

    /**
     * Returns an arbitrary datum from a code. The returned object will typically be a
     * {@link GeodeticDatum}, {@link VerticalDatum}, {@link TemporalDatum} or {@link EngineeringDatum}.
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
     * @return the datum for the given code.
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
     * @return the datum for the given code.
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
     * @return the datum for the given code.
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
     * @return the datum for the given code.
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
     * Returns an identification of a reference surface used as the origin of a parametric coordinate system.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createParametricDatum(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createParametricDatum(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createDatum(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return the datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @since 1.4
     */
    @Override
    public ParametricDatum createParametricDatum(final String code) throws FactoryException {
        if (isDefault(ParametricDatum.class)) {
            return super.createParametricDatum(code);
        }
        return create(AuthorityFactoryProxy.PARAMETRIC_DATUM, code);
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
     * @return the datum for the given code.
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
     * @return the datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @deprecated The {@code ImageDatum} class has been removed in ISO 19111:2019.
     *             It is replaced by {@code EngineeringDatum}.
     */
    @Override
    @Deprecated(since = "1.5")
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
     * @return the ellipsoid for the given code.
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
     * @return the prime meridian for the given code.
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
     * @return the extent for the given code.
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
     * @return the coordinate system for the given code.
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
     * Returns a 2- or 3-dimensional coordinate system for geodetic latitude and longitude, sometimes with ellipsoidal height.
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
     * @return the coordinate system for the given code.
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
     * @return the coordinate system for the given code.
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
     * @return the coordinate system for the given code.
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
     * Returns a 1-dimensional coordinate system containing a single axis.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Return the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise if the Data Access Object (DAO) overrides the {@code createParametricCS(String)}
     *       method, invoke that method and cache the result for future use.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createParametricCS(String)}
     *       method in the parent class. This allows to check if the more generic
     *       {@link #createCoordinateSystem(String)} method cached a value before to try that method.</li>
     * </ul>
     *
     * @return the coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @since 1.4
     */
    @Override
    public ParametricCS createParametricCS(final String code) throws FactoryException {
        if (isDefault(ParametricCS.class)) {
            return super.createParametricCS(code);
        }
        return create(AuthorityFactoryProxy.PARAMETRIC_CS, code);
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
     * @return the coordinate system for the given code.
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
     * @return the coordinate system for the given code.
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
     * @return the coordinate system for the given code.
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
     * @return the coordinate system for the given code.
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
     * @return the axis for the given code.
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
     * @return the unit of measurement for the given code.
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
     * @return the parameter descriptor for the given code.
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
     * @return the operation method for the given code.
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
     * @return the operation for the given code.
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
        @Override public String toString() {
            final var buffer = new StringBuilder();
            if (type instanceof Class<?>) {
                buffer.append("Code[“").append(code);
                if (buffer.length() > 15) {                     // Arbitrary limit in string length.
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
     * @param  <T>    the type of the object to be returned.
     * @param  proxy  the proxy to use for creating the object.
     * @param  code   the code of the object to create.
     * @return the object extracted from the cache or created.
     * @throws FactoryException if an error occurred while creating the object.
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
                    if (isCacheable(code, result)) {
                        value = result;                                 // For the finally block below.
                    }
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
     * @return the operations from {@code sourceCRS} to {@code targetCRS}.
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
     * @return a finder to use for looking up unidentified objects.
     * @throws FactoryException if the finder cannot be created.
     */
    @Override
    public IdentifiedObjectFinder newIdentifiedObjectFinder() throws FactoryException {
        return new Finder(this);
    }

    /**
     * An implementation of {@link IdentifiedObjectFinder} which delegates
     * the work to the underlying Data Access Object and caches the result.
     *
     * <h4>Synchronization note</h4>
     * Our public API claims that {@link IdentifiedObjectFinder}s are not thread-safe.
     * Nevertheless we synchronize this particular implementation for safety, because the consequence of misuse
     * are more dangerous than other implementations. Furthermore, this is also a way to assert that no code path
     * go to the {@link #create(AuthorityFactoryProxy, String)} method from a non-overridden public method.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     */
    private static final class Finder extends IdentifiedObjectFinder {
        /**
         * Number of values in the {@link IdentifiedObjectFinder.Domain} enumeration.
         * Hard-coded for efficiency. Value is verified using reflection by the test
         * {@code ConcurrentAuthorityFactoryTest.verifyDomainCount()}.
         */
        private static final int DOMAIN_COUNT = 4;

        /**
         * The finder to which to delegate the work. This is acquired by {@link #acquire()}
         * <strong>and must be released</strong> by call to {@link #release()} once finished.
         */
        private transient IdentifiedObjectFinder finder;

        /**
         * Number of time that {@link #acquire()} has been invoked.
         * When this count reaches zero, the {@linkplain #finder} is released.
         */
        private transient int acquireCount;

        /**
         * Creates a finder for the given type of objects.
         */
        Finder(final ConcurrentAuthorityFactory<?> factory) {
            super(factory);
        }

        /**
         * Returns the factory given at construction time.
         */
        private ConcurrentAuthorityFactory<?> factory() {
            return (ConcurrentAuthorityFactory<?>) factory;
        }

        /**
         * Acquires a new {@linkplain #finder}.
         * The {@link #release()} method must be invoked in a {@code finally} block after the call to {@code acquire}.
         * The pattern must be as below (note that the call to {@code acquire()} is inside the {@code try} block):
         *
         * {@snippet lang="java" :
         *     try {
         *         acquire();
         *         (finder or proxy).doSomeStuff();
         *     } finally {
         *         release();
         *     }
         *     }
         */
        private void acquire() throws FactoryException {
            assert Thread.holdsLock(this);
            assert (acquireCount == 0) == (finder == null) : acquireCount;
            if (acquireCount == 0) {
                final GeodeticAuthorityFactory delegate = factory().getDataAccess();
                /*
                 * Set `acquireCount` only after we succeed in fetching the factory, and before any operation on it.
                 * The intent is to get ConcurrentAuthorityFactory.release() invoked if and only if the getDataAccess()
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
                factory().release(null, null, null);
            }
        }

        /**
         * Returns a set of authority codes that <strong>may</strong> identify the same
         * object than the specified one. This method delegates to the data access object.
         */
        @Override
        protected synchronized Iterable<String> getCodeCandidates(final IdentifiedObject object) throws FactoryException {
            try {
                acquire();
                return finder.getCodeCandidates(object);
            } finally {
                release();
            }
        }

        /**
         * Returns the index in the cached {@code Set<IdentifiedObject>[]} array for a result using the given finder.
         * The index depends on the finder configuration. The argument should be either {@link #finder} if non-null,
         * or {@code this} otherwise.
         */
        private static int index(final IdentifiedObjectFinder finder) {
            int index = finder.getSearchDomain().ordinal();
            if (finder.isIgnoringAxes()) index += DOMAIN_COUNT;
            return index;
        }

        /**
         * Returns the cached value for the given object, or {@code null} if none.
         * This is checked by {@link #find(IdentifiedObject)} before actual search.
         * The returned set (if non-null) is unmodifiable.
         *
         * @param  object  the user-specified object to search.
         * @return the cached result of the find operation, or {@code null} if none.
         */
        @Override
        final Set<IdentifiedObject> getFromCache(final IdentifiedObject object) {
            // `finder` should never be null since this method is not invoked directly by this Finder.
            return getFromCache(object, index(finder));
        }

        /**
         * Implementation of {@link #getFromCache(IdentifiedObject)} with the specified index to use in the cache.
         * The index depends on the finder configuration.
         *
         * @param  object  the user-specified object to search.
         * @param  index   value of {@link #index(IdentifiedObjectFinder)} or custom slot.
         * @return the cached result of the find operation, or {@code null} if none.
         */
        private Set<IdentifiedObject> getFromCache(final IdentifiedObject object, final int index) {
            final Map<IdentifiedObject, Set<IdentifiedObject>[]> findPool = factory().findPool;
            synchronized (findPool) {
                final Set<IdentifiedObject>[] entry = findPool.get(object);
                if (entry != null) {
                    return entry[index];
                }
            }
            return null;
        }

        /**
         * Stores the given result in the cache. This method copies the given set in a new unmodifiable
         * set and returns the result. The copy is needed because, with the current implementation of
         * <abbr>EPSG</abbr> factory, {@code result} may be a lazy set with a connection to the database.
         *
         * @param  object  the user-specified object which was searched.
         * @param  result  the search result. It will be copied.
         * @return a set with the same content as {@code result}.
         */
        @Override
        final Set<IdentifiedObject> cache(final IdentifiedObject object, final Set<IdentifiedObject> result) {
            // `finder` should never be null since this method is not invoked directly by this Finder.
            return cache(object, CollectionsExt.copyPreserveOrder(result), index(finder));
        }

        /**
         * Implementation of {@link #cache(IdentifiedObject, Set)} with the specified index to use in the cache.
         * The index depends on the finder configuration.
         *
         * @param  object  the user-specified object which was searched.
         * @param  result  the search result. The copy, if needed, shall be done by the caller.
         * @param  index   value of {@link #index(IdentifiedObjectFinder)} or custom slot.
         * @return a set with the same content as {@code result}.
         */
        private Set<IdentifiedObject> cache(final IdentifiedObject object, Set<IdentifiedObject> result, final int index) {
            final Map<IdentifiedObject, Set<IdentifiedObject>[]> findPool = factory().findPool;
            synchronized (findPool) {
                final Set<IdentifiedObject>[] entry = findPool.computeIfAbsent(object, Finder::createCacheEntry);
                final Set<IdentifiedObject> existing = entry[index];
                if (existing != null) {
                    return existing;
                }
                for (Set<IdentifiedObject> other : entry) {
                    if (result.equals(other)) {
                        result = other;             // Share existing instance.
                        break;
                    }
                }
                entry[index] = result;
            }
            return result;
        }

        /**
         * Creates an initially empty cache entry for the given object.
         * Used in lambda expression and defined as a separated method because of generic type.
         * The {@code object} argument is present only for having the required method signature.
         *
         * <p>The array length is {@value #DOMAIN_COUNT} × 2 for whether axes are ignored or not,
         * and ×2 again for whether a singleton is searched instead of the collection.</p>
         *
         * @param  object  the user-specified object which was searched.
         * @return a new array to use as a cache for the specified object.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})            // Generic array creation.
        private static Set<IdentifiedObject>[] createCacheEntry(IdentifiedObject object) {
            return new Set[DOMAIN_COUNT * 4];
        }

        /**
         * Looks up an object from this authority factory which is approximately equal to the specified object.
         * The default implementation performs the same lookup as the Data Access Object and caches the result.
         *
         * @param  object  the object looked up.
         * @return the identified object, or {@code null} if none or ambiguous.
         */
        @Override
        public IdentifiedObject findSingleton(final IdentifiedObject object) throws FactoryException {
            final int index = index(this) + 2*DOMAIN_COUNT;
            Set<IdentifiedObject> result = getFromCache(object, index);
            if (result == null) {
                synchronized (this) {
                    try {
                        acquire();
                        result = CollectionsExt.singletonOrEmpty(finder.findSingleton(object));
                    } finally {
                        release();
                    }
                }
                cache(object, result, index);
            }
            factory().findPoolLatestQueries.markAsUsed(object);
            return CollectionsExt.first(result);
        }

        /**
         * Looks up objects from this authority factory which are approximately equal to the specified object.
         * The default implementation performs the same lookup as the Data Access Object and caches the result.
         *
         * @param  object  the object looked up.
         * @return the identified objects, or an empty set if not found.
         */
        @Override
        public Set<IdentifiedObject> find(final IdentifiedObject object) throws FactoryException {
            Set<IdentifiedObject> candidate = getFromCache(object, index(this));
            if (candidate == null) {
                /*
                 * Nothing has been found in the cache. Delegates the search to the Data Access Object.
                 * Note that the Data Access Object will itself callbacks our `cache(…)` method,
                 * so there is no need that we cache the result here.
                 */
                synchronized (this) {
                    try {
                        acquire();
                        candidate = finder.find(object);
                    } finally {
                        release();
                    }
                }
            }
            /*
             * Keep a strong reference to the given object, potentially overwriting oldest strong reference.
             * The purpose is only to prevent too early invalidation of weak references in `findPool` cache.
             * We keep a strong reference only for the top-level object, not for the intermediate searches,
             * because strong references to intermediate objects already exist in the top-level object.
             */
            factory().findPoolLatestQueries.markAsUsed(object);
            return candidate;
        }
    }

    /**
     * Returns whether the given object can be cached. This method is invoked after the
     * {@linkplain #newDataAccess() Data Access Object} created a new object not previously in the cache.
     * If this {@code isCacheable(…)} method returns {@code true}, then the newly created object will be cached so
     * that next calls to the same {@code createFoo(String)} method with the same code may return the same object.
     * If this method returns {@code false}, then the newly created object will not be cached and next call to
     * the {@code createFoo(String)} method with the same code will return a new object.
     *
     * <p>The default implementation always returns {@code true}.
     * Subclasses can override this method for filtering the objects to store in the cache.</p>
     *
     * @param  code    the authority code specified by the caller for creating an object.
     * @param  object  the object created for the given authority code.
     * @return whether the given object should be cached.
     *
     * @see #printCacheContent(PrintWriter)
     *
     * @since 0.8
     */
    protected boolean isCacheable(String code, Object object) {
        return true;
    }

    /**
     * Prints the cache content to the given writer.
     * Keys are sorted by numerical order if possible, or alphabetical order otherwise.
     * This method is used for debugging purpose only.
     *
     * @param  out  the output printer, or {@code null} for the {@linkplain System#out standard output stream}.
     *
     * @see #isCacheable(String, Object)
     */
    @Debug
    public void printCacheContent(final PrintWriter out) {
        CacheRecord.printCacheContent(cache, out);
    }

    /**
     * Prints the cache content to the standard output stream.
     * Keys are sorted by numerical order if possible, or alphabetical order otherwise.
     * This method is used for debugging purpose only.
     *
     * @see #isCacheable(String, Object)
     */
    @Debug
    @Override
    public void print() {
        printCacheContent(null);
    }

    /**
     * A hook to be executed either when the {@link ConcurrentAuthorityFactory} is collected by the garbage collector,
     * when the Java Virtual Machine is shutdown, or when the module is uninstalled by the OSGi or Servlet container.
     *
     * <p><strong>Do not keep reference to the enclosing factory</strong> - in particular,
     * this class must be static - otherwise the factory would never been garbage collected.</p>
     */
    private static final class ShutdownHook<DAO extends GeodeticAuthorityFactory> implements Runnable, Callable<Object> {
        /**
         * The {@link ConcurrentAuthorityFactory#availableDAOs} queue.
         */
        private final Deque<DataAccessRef<DAO>> availableDAOs;

        /**
         * Creates a new shutdown hook.
         */
        ShutdownHook(final Deque<DataAccessRef<DAO>> availableDAOs) {
            this.availableDAOs = availableDAOs;
        }

        /**
         * Invoked indirectly by the garbage collector when the {@link ConcurrentAuthorityFactory} is disposed.
         */
        @Override
        public void run() {
            Shutdown.unregister(this);
            try {
                call();
            } catch (Exception exception) {
                /*
                 * Pretend that the exception is logged by `ConcurrentAuthorityFactory.dispose()`.
                 * This is not true, but carries the idea that the error occurred while cleaning
                 * ConcurrentAuthorityFactory after garbage collection.
                 */
                unexpectedException("dispose", exception);
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
     * @param  availableDAOs  the queue of factories to close.
     */
    private static <DAO extends GeodeticAuthorityFactory> List<DAO> clear(final Deque<DataAccessRef<DAO>> availableDAOs) {
        assert Thread.holdsLock(availableDAOs);
        final var factories = new ArrayList<DAO>(availableDAOs.size());
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
     * @param  factories  the factories to close.
     * @throws Exception the exception thrown by the first factory that failed to close.
     */
    private static <DAO extends GeodeticAuthorityFactory> void close(final List<DAO> factories) throws Exception {
        Exception exception = null;
        for (int i = factories.size(); --i >= 0;) {
            final DAO factory = factories.get(i);
            if (factory instanceof AutoCloseable) try {
                ((AutoCloseable) factory).close();
            } catch (Exception e) {
                if (exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
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
     *
     * @see #canClose(GeodeticAuthorityFactory)
     */
    @Override
    public void close() throws FactoryException {
        try {
            final List<DAO> factories;
            synchronized (availableDAOs) {
                factories = clear(availableDAOs);
            }
            confirmClose(factories);
            close(factories);                       // Must be invoked outside the synchronized block.
        } catch (Exception e) {
            e = Exceptions.unwrap(e);
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
     * @return a string representation for debugging purpose.
     *
     * @see #printCacheContent(PrintWriter)
     */
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
        return s + System.lineSeparator() + "└─" + usage;
    }

    /**
     * Completes the string representation of this factory for debugging purpose only.
     * The string formatted by this method may change in any future SIS version.
     */
    @Debug
    @Override
    final void appendStringTo(final StringBuilder buffer) {
        buffer.append(", cache=").append(cache.size()).append(", DAO=");
        synchronized (availableDAOs) {
            buffer.append(availableDAOs.size());
            if (remainingDAOs <= 0) {
                buffer.append(" (limit reached)");
            }
        }
    }
}
