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
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.lang.ref.WeakReference;
import java.io.PrintWriter;
import javax.measure.unit.Unit;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.NameFactory;
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
import org.apache.sis.internal.referencing.NilReferencingObject;
import org.apache.sis.internal.system.DelayedExecutor;
import org.apache.sis.internal.system.DelayedRunnable;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.resources.Errors;


/**
 * A concurrent authority factory that caches all objects created by another factory.
 * All {@code createFoo(String)} methods first check if a previously created object exists for the given code.
 * If such object exists, it is returned. Otherwise, the object creation is delegated to another factory given
 * by {@link #createBackingStore()} and the result is cached in this factory.
 *
 * <p>{@code ConcurrentAuthorityFactory} delays the call to {@code createBackingStore()} until first needed,
 * and {@linkplain Disposable#dispose() dispose} it after some timeout. This approach allows to establish
 * a connection to a database (for example) and keep it only for a relatively short amount of time.</p>
 *
 * <div class="section">Caching strategy</div>
 * Objects are cached by strong references, up to the amount of objects specified at construction time.
 * If a greater amount of objects are cached, then the oldest ones will be retained through a
 * {@linkplain WeakReference weak reference} instead of a strong one.
 * This means that this caching factory will continue to return those objects as long as they are in use somewhere
 * else in the Java virtual machine, but will be discarded (and recreated on the fly if needed) otherwise.
 *
 * <div class="section">Multi-threading</div>
 * The cache managed by this class is concurrent. However the backing stores are assumed non-concurrent.
 * If two or more threads are accessing this factory in same time, then two or more backing store instances
 * may be created. The maximal amount of instances to create is specified at {@code ConcurrentAuthorityFactory}
 * construction time. If more backing store instances are needed, some of the threads will block until an
 * instance become available.
 *
 * <div class="section">Note for subclasses</div>
 * This abstract class does not implement any of the {@link DatumAuthorityFactory}, {@link CSAuthorityFactory},
 * {@link CRSAuthorityFactory} and {@link CoordinateOperationAuthorityFactory} interfaces.
 * Subclasses should select the interfaces that they choose to implement.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class ConcurrentAuthorityFactory extends GeodeticAuthorityFactory implements Disposable {
    /**
     * The authority, cached after first requested.
     */
    private transient volatile Citation authority;

    /**
     * The pool of cached objects.
     */
    private final Cache<Key,Object> cache;

    /**
     * The pool of objects identified by {@link Finder#find(IdentifiedObject)} for each comparison modes.
     * Values may be {@link NilReferencingObject} if an object has been searched but has not been found.
     *
     * <p>Every access to this pool must be synchronized on {@code findPool}.</p>
     */
    private final Map<IdentifiedObject, IdentifiedObject> findPool = new WeakHashMap<>();

    /**
     * Holds the reference to a backing store used by {@link ConcurrentAuthorityFactory} together with information
     * about its usage. In a mono-thread application, there is typically only one {@code BackingStore} instance
     * at a given time. However if more than one than one thread are requesting new objects concurrently, then
     * many instances may exist for the same {@code ConcurrentAuthorityFactory}.
     *
     * <p>If the backing store is currently in use, then {@code BackingStore} counts how many recursive invocations
     * of a {@link #factory} {@code createFoo(String)} method is under way in the current thread.
     * This information is used in order to reuse the same factory instead than creating new instances
     * when a {@code GeodeticAuthorityFactory} implementation invokes itself indirectly through the
     * {@link ConcurrentAuthorityFactory}. This assumes that factory implementations are reentrant.</p>
     *
     * <p>If the backing store has been released, then {@code BackingStore} keep the release timestamp.
     * This information is used for prioritize the backing stores to dispose.</p>
     */
    private static final class BackingStore {
        /**
         * The factory used as a backing store.
         */
        final GeodeticAuthorityFactory factory;

        /**
         * Incremented on every call to {@link ConcurrentAuthorityFactory#getBackingStore()} and decremented on every call
         * to {@link ConcurrentAuthorityFactory#release()}. When this value reach zero, the factory is really released.
         */
        int depth;

        /**
         * The timestamp (<strong>not</strong> relative to epoch) at the time the backing store factory has been
         * released. This timestamp shall be obtained by a call to {@link System#nanoTime()} for consistency with
         * {@link DelayedRunnable}.
         */
        long timestamp;

        /**
         * Creates new backing store information for the given factory.
         */
        BackingStore(final GeodeticAuthorityFactory factory) {
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
                text = "%s in use with at depth %d";
                value = depth;
            } else {
                text = "%s made available %d seconds ago";
                value = (System.nanoTime() - timestamp) / 1E+9;   // Convert nanoseconds to seconds.
            }
            return String.format(text, Classes.getShortClassName(factory), value);
        }
    }

    /**
     * The backing store in use by the current thread.
     */
    private final ThreadLocal<BackingStore> currentStore = new ThreadLocal<>();

    /**
     * The backing store instances previously created and released for future reuse.
     * Last used factories must be {@linkplain Deque#addLast(Object) added last}.
     * This is used as a LIFO stack.
     */
    private final Deque<BackingStore> availableStores = new LinkedList<>();

    /**
     * The amount of backing stores that can still be created. This number is decremented in a block synchronized
     * on {@link #availableStores} every time a backing store is in use, and incremented once released.
     */
    private int remainingBackingStores;

    /**
     * {@code true} if the call to {@link #disposeExpired()} is scheduled for future execution in the background
     * cleaner thread.  A value of {@code true} implies that this factory contains at least one active backing store.
     * However the reciprocal is not true: this field may be set to {@code false} while a worker factory is currently
     * in use because this field is set to {@code true} only when a worker factory is {@linkplain #release() released}.
     *
     * <p>Note that we can not use {@code !stores.isEmpty()} as a replacement of {@code isActive}
     * because the queue is empty if all backing stores are currently in use.</p>
     *
     * <p>Every access to this field must be performed in a block synchronized on {@link #availableStores}.</p>
     */
    private boolean isActive;

    /**
     * {@code true} if {@link #dispose()} has been invoked.
     *
     * <p>Every access to this field must be performed in a block synchronized on {@link #availableStores}.</p>
     */
    private boolean isDisposed;

    /**
     * The delay of inactivity (in nanoseconds) before to close a backing store.
     * Every access to this field must be performed in a block synchronized on {@link #availableStores}.
     *
     * @see #getTimeout(TimeUnit)
     */
    private long timeout = 60_000_000_000L;     // One minute

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
     * @param nameFactory The factory to use for parsing authority code as {@link GenericName} instances.
     */
    protected ConcurrentAuthorityFactory(final NameFactory nameFactory) {
        this(nameFactory, 100, 8);
        /*
         * NOTE: if the default maximum number of backing stores (currently 8) is augmented,
         * make sure to augment the number of runner threads in the "StressTest" class to a greater amount.
         */
    }

    /**
     * Constructs an instance with the specified number of entries to keep by strong references.
     * If a number of object greater than {@code maxStrongReferences} are created, then the strong references
     * for the eldest ones will be replaced by weak references.
     *
     * @param nameFactory The factory to use for parsing authority code as {@link GenericName} instances.
     * @param maxStrongReferences The maximum number of objects to keep by strong reference.
     * @param maxConcurrentQueries The maximal amount of backing stores to use concurrently.
     *        If more than this amount of threads are querying this {@code ConcurrentAuthorityFactory} concurrently,
     *        additional threads will be blocked until a backing store become available.
     */
    protected ConcurrentAuthorityFactory(final NameFactory nameFactory,
            final int maxStrongReferences, final int maxConcurrentQueries)
    {
        super(nameFactory);
        ArgumentChecks.ensurePositive("maxStrongReferences", maxStrongReferences);
        ArgumentChecks.ensureStrictlyPositive("maxConcurrentQueries", maxConcurrentQueries);
        remainingBackingStores = maxConcurrentQueries;
        cache = new Cache<>(20, maxStrongReferences, false);
        cache.setKeyCollisionAllowed(true);
        /*
         * Key collision is usually an error. But in this case we allow them in order to enable recursivity.
         * If during the creation of an object the program asks to this ConcurrentAuthorityFactory for the same
         * object (using the same key), then the default Cache implementation considers that situation as an
         * error unless the above property has been set to 'true'.
         */
    }

    /**
     * Returns the number of backing stores. This count does not include the backing stores
     * that are currently under execution. This method is used only for testing purpose.
     */
    @Debug
    final int countBackingStores() {
        synchronized (availableStores) {
            return availableStores.size();
        }
    }

    /**
     * Creates a new backing store authority factory. This method is invoked the first time a {@code createFoo(String)}
     * method is invoked. It may also be invoked again if additional factories are needed in different threads,
     * or if all factories have been disposed after the timeout.
     *
     * <div class="section">Multi-threading</div>
     * This method (but not necessarily the returned factory) needs to be thread-safe;
     * {@code ConcurrentAuthorityFactory} does not hold any lock when invoking this method.
     * Subclasses are responsible to apply their own synchronization if needed,
     * but are encouraged to avoid doing so if possible.
     * In addition, implementations should not invoke other {@code ConcurrentAuthorityFactory}
     * methods during this method execution in order to avoid never-ending loop.
     *
     * @return The backing store to uses in {@code createFoo(String)} methods.
     * @throws UnavailableFactoryException if the backing store is unavailable because an optional resource is missing.
     * @throws FactoryException if the creation of backing store failed for another reason.
     */
    protected abstract GeodeticAuthorityFactory createBackingStore() throws UnavailableFactoryException, FactoryException;

    /**
     * Returns a backing store authority factory. This method <strong>must</strong>
     * be used together with {@link #release()} in a {@code try ... finally} block.
     *
     * @return The backing store to use in {@code createXXX(…)} methods.
     * @throws FactoryException if the backing store creation failed.
     */
    private GeodeticAuthorityFactory getBackingStore() throws FactoryException {
        /*
         * First checks if the current thread is already using a factory. If yes, we will
         * avoid creating new factories on the assumption that factories are reentrant.
         */
        BackingStore usage = currentStore.get();
        if (usage == null) {
            synchronized (availableStores) {
                if (isDisposed) {
                    throw new UnavailableFactoryException(Errors.format(Errors.Keys.DisposedFactory));
                }
                /**
                 * If we have reached the maximal amount of backing stores allowed, wait for a backing store
                 * to become available. In theory the 0.2 second timeout is not necessary, but we put it as a
                 * safety in case we fail to invoke a notify() matching this wait(), for example someone else
                 * is waiting on this monitor or because the release(…) method threw an exception.
                 */
                while (remainingBackingStores == 0) {
                    try {
                        availableStores.wait(TIMEOUT_RESOLUTION);
                    } catch (InterruptedException e) {
                        // Someone does not want to let us sleep.
                        throw new FactoryException(e.getLocalizedMessage(), e);
                    }
                }
                /*
                 * Reuse the most recently used factory, if available. If there is no factory available for reuse,
                 * creates a new one. We do not add it to the queue now; it will be done by the release(…) method.
                 */
                usage = availableStores.pollLast();
                remainingBackingStores--;       // Should be done last when we are sure to not fail.
            }
            /*
             * If there is a need to create a new factory, do that outside the synchronized block because this
             * creation may involve a lot of client code. This is better for reducing the dead-lock risk.
             * Subclasses are responsible of synchronizing their createBackingStore() method if necessary.
             */
            try {
                if (usage == null) {
                    final GeodeticAuthorityFactory factory = createBackingStore();
                    if (factory == null) {
                        throw new UnavailableFactoryException(Errors.format(
                                Errors.Keys.FactoryNotFound_1, GeodeticAuthorityFactory.class));
                    }
                    usage = new BackingStore(factory);
                    currentStore.set(usage);
                }
                assert usage.depth == 0 : usage;
            } finally {
                /*
                 * If any kind of error occurred, restore the 'remainingBackingStores' field as if no code were executed.
                 * This code would not have been needed if we were allowed to decrement 'remainingBackingStores' only as
                 * the very last step (when we know that everything else succeed). But it needed to be decremented inside
                 * the synchronized block.
                 */
                if (usage == null) {
                    synchronized (availableStores) {
                        remainingBackingStores++;
                    }
                }
            }
        }
        /*
         * Increment below is safe even if outside the synchronized block,
         * because each thread own exclusively its BackingStore instance
         */
        usage.depth++;
        return usage.factory;
    }

    /**
     * Releases the backing store previously obtained with {@link #getBackingStore()}.
     * This method marks the factory as available for reuse by other threads.
     */
    private void release() {
        final BackingStore usage = currentStore.get();     // A null value here would be an error in our algorithm.
        if (--usage.depth == 0) {
            synchronized (availableStores) {
                if (isDisposed) return;
                remainingBackingStores++;       // Must be done first in case an exception happen after this point.
                usage.timestamp = System.nanoTime();
                availableStores.addLast(usage);
                /*
                 * If the backing store we just released is the first one, awake the
                 * disposer thread which was waiting for an indefinite amount of time.
                 */
                if (!isActive) {
                    isActive = true;
                    DelayedExecutor.schedule(new DisposeTask(usage.timestamp + timeout));
                }
                availableStores.notify();    // We released only one backing store, so awake only one thread - not all of them.
            }
        }
        assert usage.depth >= 0 : usage;
    }

    /**
     * A task for invoking {@link ConcurrentAuthorityFactory#disposeExpired()} after a delay.
     */
    private final class DisposeTask extends DelayedRunnable {
        /**
         * Creates a new task to be executed at the given time,
         * in nanoseconds relative to {@link System#nanoTime()}.
         */
        DisposeTask(final long timestamp) {
            super(timestamp);
        }

        /** Invoked when the delay expired. */
        @Override public void run() {
            disposeExpired();
        }
    }

    /**
     * Disposes the expired backing stores. This method should be invoked from a background task only.
     * This method may reschedule the task again for an other execution if it appears that at least one
     * backing store was not ready for disposal.
     *
     * @see #dispose()
     */
    final void disposeExpired() {
        int count = 0;
        final Disposable[] toDispose;
        synchronized (availableStores) {
            toDispose = new Disposable[availableStores.size()];
            final Iterator<BackingStore> it = availableStores.iterator();
            final long nanoTime = System.nanoTime();
            while (it.hasNext()) {
                final BackingStore store = it.next();
                /*
                 * Computes how much time we need to wait again before we can dispose the factory.
                 * If this time is greater than some arbitrary amount, do not dispose the factory
                 * and wait again.
                 */
                final long nextTime = store.timestamp + timeout;
                if (nextTime - nanoTime > TIMEOUT_RESOLUTION) {
                    /*
                     * Found a factory which is not expired. Stop the search,
                     * since the iteration is expected to be ordered.
                     */
                    DelayedExecutor.schedule(new DisposeTask(nextTime));
                    break;
                }
                /*
                 * Found an expired factory. Adds it to the list of
                 * factories to dispose and search for other factories.
                 */
                it.remove();
                if (store.factory instanceof Disposable) {
                    toDispose[count++] = (Disposable) store.factory;
                }
            }
            /*
             * The stores list is empty if all worker factories in the queue have been disposed.
             * Note that some worker factories may still be active outside the queue, because the
             * workers are added to the queue only after completion of their work.
             * In the later case, release() will reschedule a new task.
             */
            isActive = !availableStores.isEmpty();
        }
        /*
         * We must dispose the factories from outside the synchronized block.
         */
        while (--count >= 0) {
            toDispose[count].dispose();
        }
    }

    /**
     * Returns the amount of time that {@code ConcurrentAuthorityFactory} will wait before to dispose a backing store.
     * This delay is measured from the last time the backing store has been used by a {@code createFoo(String)} method.
     *
     * @param unit The desired unit of measurement for the timeout.
     * @return The current timeout in the given unit of measurement.
     */
    public long getTimeout(final TimeUnit unit) {
        synchronized (availableStores) {
            return unit.convert(timeout, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Sets a timer for disposing the backing store after the specified amount of time of inactivity.
     * If a new backing store is needed after the disposal of the last one, then the {@link #createBackingStore()}
     * method will be invoked again.
     *
     * @param delay The delay of inactivity before to close a backing store.
     * @param unit  The unit of measurement of the given delay.
     */
    public void setTimeout(long delay, final TimeUnit unit) {
        ArgumentChecks.ensureStrictlyPositive("delay", delay);
        delay = unit.toNanos(delay);
        synchronized (availableStores) {
            timeout = delay;                // Will be taken in account after the next factory to dispose.
        }
    }

    /**
     * Returns the organization or party responsible for definition and maintenance of the underlying database.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached value if it exists.</li>
     *   <li>Otherwise:
     *     <ol>
     *       <li>get an instance of the backing store,</li>
     *       <li>delegate to its {@link GeodeticAuthorityFactory#getAuthority()} method,</li>
     *       <li>release the backing store,</li>
     *       <li>cache the result.</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * If this method can not get a backing store factory (for example because no database connection is available),
     * then this method returns {@code null}.
     *
     * @return The organization responsible for definition of the database, or {@code null} if unavailable.
     */
    @Override
    public Citation getAuthority() {
        Citation c = authority;
        if (c == null) try {
            final GeodeticAuthorityFactory factory = getBackingStore();
            try {
                // Cache only in case of success. If we failed, we
                // will try again next time this method is invoked.
                authority = c = factory.getAuthority();
            } finally {
                release();
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
     *   <li>get an instance of the backing store,</li>
     *   <li>delegate to its {@link GeodeticAuthorityFactory#getAuthorityCodes(Class)} method,</li>
     *   <li>release the backing store.</li>
     * </ol>
     *
     * @param  type The spatial reference objects type (e.g. {@code ProjectedCRS.class}).
     * @return The set of authority codes for spatial reference objects of the given type.
     *         If this factory does not contains any object of the given type, then this method returns an empty set.
     * @throws FactoryException if access to the underlying database failed.
     */
    @Override
    public Set<String> getAuthorityCodes(final Class<? extends IdentifiedObject> type) throws FactoryException {
        final GeodeticAuthorityFactory factory = getBackingStore();
        try {
            return factory.getAuthorityCodes(type);
            /*
             * In the particular case of EPSG factory, the returned Set maintains a live connection to the database.
             * But it still okay to release the factory anyway because our implementation will really close
             * the connection only when the iteration is over or the iterator has been garbage-collected.
             */
        } finally {
            release();
        }
    }

    /**
     * Gets a description of the object corresponding to a code.
     * The default implementation performs the following steps:
     * <ol>
     *   <li>get an instance of the backing store,</li>
     *   <li>delegate to its {@link GeodeticAuthorityFactory#getDescriptionText(String)} method,</li>
     *   <li>release the backing store.</li>
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
        final GeodeticAuthorityFactory factory = getBackingStore();
        try {
            return factory.getDescriptionText(code);
        } finally {
            release();
        }
    }

    /**
     * Returns an arbitrary object from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise:
     *     <ol>
     *       <li>get an instance of the backing store,</li>
     *       <li>delegate to its {@link GeodeticAuthorityFactory#createObject(String)} method,</li>
     *       <li>release the backing store,</li>
     *       <li>cache the result.</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * @return The object for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createCoordinateReferenceSystem(String)
     * @see #createDatum(String)
     * @see #createCoordinateSystem(String)
     */
    @Override
    public IdentifiedObject createObject(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.OBJECT, code);
    }

    /**
     * Returns an arbitrary coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCoordinateReferenceSystem(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeographicCRS(String)
     * @see #createProjectedCRS(String)
     * @see #createVerticalCRS(String)
     * @see #createTemporalCRS(String)
     */
    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.CRS, code);
    }

    /**
     * Returns a geographic coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createGeographicCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeodeticDatum(String)
     * @see #createEllipsoidalCS(String)
     * @see org.apache.sis.referencing.crs.DefaultGeographicCRS
     */
    @Override
    public GeographicCRS createGeographicCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.GEOGRAPHIC_CRS, code);
    }

    /**
     * Returns a geocentric coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createGeocentricCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeodeticDatum(String)
     * @see #createCartesianCS(String)
     * @see #createSphericalCS(String)
     * @see org.apache.sis.referencing.crs.DefaultGeocentricCRS
     */
    @Override
    public GeocentricCRS createGeocentricCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.GEOCENTRIC_CRS, code);
    }

    /**
     * Returns a projected coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createProjectedCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeographicCRS(String)
     * @see #createCartesianCS(String)
     * @see org.apache.sis.referencing.crs.DefaultProjectedCRS
     */
    @Override
    public ProjectedCRS createProjectedCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.PROJECTED_CRS, code);
    }

    /**
     * Returns a vertical coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createVerticalCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createVerticalDatum(String)
     * @see #createVerticalCS(String)
     * @see org.apache.sis.referencing.crs.DefaultVerticalCRS
     */
    @Override
    public VerticalCRS createVerticalCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.VERTICAL_CRS, code);
    }

    /**
     * Returns a temporal coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createTemporalCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createTemporalDatum(String)
     * @see #createTimeCS(String)
     * @see org.apache.sis.referencing.crs.DefaultTemporalCRS
     */
    @Override
    public TemporalCRS createTemporalCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.TEMPORAL_CRS, code);
    }

    /**
     * Returns a 3D or 4D coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCompoundCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createVerticalCRS(String)
     * @see #createTemporalCRS(String)
     * @see org.apache.sis.referencing.crs.DefaultCompoundCRS
     */
    @Override
    public CompoundCRS createCompoundCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.COMPOUND_CRS, code);
    }

    /**
     * Returns a derived coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createDerivedCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.crs.DefaultDerivedCRS
     */
    @Override
    public DerivedCRS createDerivedCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.DERIVED_CRS, code);
    }

    /**
     * Returns an engineering coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createEngineeringCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createEngineeringDatum(String)
     * @see org.apache.sis.referencing.crs.DefaultEngineeringCRS
     */
    @Override
    public EngineeringCRS createEngineeringCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.ENGINEERING_CRS, code);
    }

    /**
     * Returns an image coordinate reference system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createImageCRS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate reference system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createImageDatum(String)
     * @see org.apache.sis.referencing.crs.DefaultImageCRS
     */
    @Override
    public ImageCRS createImageCRS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.IMAGE_CRS, code);
    }

    /**
     * Returns an arbitrary datum from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createDatum(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeodeticDatum(String)
     * @see #createVerticalDatum(String)
     * @see #createTemporalDatum(String)
     */
    @Override
    public Datum createDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.DATUM, code);
    }

    /**
     * Returns a geodetic datum from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createGeodeticDatum(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createEllipsoid(String)
     * @see #createPrimeMeridian(String)
     * @see #createGeographicCRS(String)
     * @see #createGeocentricCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultGeodeticDatum
     */
    @Override
    public GeodeticDatum createGeodeticDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.GEODETIC_DATUM, code);
    }

    /**
     * Returns a vertical datum from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createVerticalDatum(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createVerticalCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultVerticalDatum
     */
    @Override
    public VerticalDatum createVerticalDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.VERTICAL_DATUM, code);
    }

    /**
     * Returns a temporal datum from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createTemporalDatum(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createTemporalCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultTemporalDatum
     */
    @Override
    public TemporalDatum createTemporalDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.TEMPORAL_DATUM, code);
    }

    /**
     * Returns an engineering datum from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createEngineeringDatum(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createEngineeringCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultEngineeringDatum
     */
    @Override
    public EngineeringDatum createEngineeringDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.ENGINEERING_DATUM, code);
    }

    /**
     * Returns an image datum from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createImageDatum(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The datum for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createImageCRS(String)
     * @see org.apache.sis.referencing.datum.DefaultImageDatum
     */
    @Override
    public ImageDatum createImageDatum(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.IMAGE_DATUM, code);
    }

    /**
     * Returns an ellipsoid from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createEllipsoid(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The ellipsoid for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeodeticDatum(String)
     * @see #createEllipsoidalCS(String)
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid
     */
    @Override
    public Ellipsoid createEllipsoid(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.ELLIPSOID, code);
    }

    /**
     * Returns a prime meridian from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createPrimeMeridian(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The prime meridian for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeodeticDatum(String)
     * @see org.apache.sis.referencing.datum.DefaultPrimeMeridian
     */
    @Override
    public PrimeMeridian createPrimeMeridian(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.PRIME_MERIDIAN, code);
    }

    /**
     * Returns an extent (usually a domain of validity) from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createExtent(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The extent for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createCoordinateReferenceSystem(String)
     * @see #createDatum(String)
     * @see org.apache.sis.metadata.iso.extent.DefaultExtent
     */
    @Override
    public Extent createExtent(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.EXTENT, code);
    }

    /**
     * Returns an arbitrary coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCoordinateSystem(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createCoordinateSystemAxis(String)
     * @see #createEllipsoidalCS(String)
     * @see #createCartesianCS(String)
     */
    @Override
    public CoordinateSystem createCoordinateSystem(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.COORDINATE_SYSTEM, code);
    }

    /**
     * Returns an ellipsoidal coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createEllipsoidalCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createEllipsoid(String)
     * @see #createGeodeticDatum(String)
     * @see #createGeographicCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultEllipsoidalCS
     */
    @Override
    public EllipsoidalCS createEllipsoidalCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.ELLIPSOIDAL_CS, code);
    }

    /**
     * Returns a vertical coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createVerticalCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createVerticalDatum(String)
     * @see #createVerticalCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultVerticalCS
     */
    @Override
    public VerticalCS createVerticalCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.VERTICAL_CS, code);
    }

    /**
     * Returns a temporal coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createTimeCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createTemporalDatum(String)
     * @see #createTemporalCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultTimeCS
     */
    @Override
    public TimeCS createTimeCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.TIME_CS, code);
    }

    /**
     * Returns a Cartesian coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCartesianCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createProjectedCRS(String)
     * @see #createGeocentricCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultCartesianCS
     */
    @Override
    public CartesianCS createCartesianCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.CARTESIAN_CS, code);
    }

    /**
     * Returns a spherical coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createSphericalCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createGeocentricCRS(String)
     * @see org.apache.sis.referencing.cs.DefaultSphericalCS
     */
    @Override
    public SphericalCS createSphericalCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.SPHERICAL_CS, code);
    }

    /**
     * Returns a cylindrical coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCylindricalCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.cs.DefaultCylindricalCS
     */
    @Override
    public CylindricalCS createCylindricalCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.CYLINDRICAL_CS, code);
    }

    /**
     * Returns a polar coordinate system from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createPolarCS(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The coordinate system for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.cs.DefaultPolarCS
     */
    @Override
    public PolarCS createPolarCS(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.POLAR_CS, code);
    }

    /**
     * Returns a coordinate system axis from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCoordinateSystemAxis(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The axis for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see #createCoordinateSystem(String)
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis
     */
    @Override
    public CoordinateSystemAxis createCoordinateSystemAxis(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.AXIS, code);
    }

    /**
     * Returns an unit of measurement from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createUnit(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The unit of measurement for the given code.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Unit<?> createUnit(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.UNIT, code);
    }

    /**
     * Returns a parameter descriptor from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createParameterDescriptor(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The parameter descriptor for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.parameter.DefaultParameterDescriptor
     */
    @Override
    public ParameterDescriptor<?> createParameterDescriptor(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.PARAMETER, code);
    }

    /**
     * Returns an operation method from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createOperationMethod(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The operation method for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.operation.DefaultOperationMethod
     */
    @Override
    public OperationMethod createOperationMethod(final String code) throws FactoryException {
        return create(AuthorityFactoryProxy.METHOD, code);
    }

    /**
     * Returns an operation from a code.
     * The default implementation performs the following steps:
     * <ul>
     *   <li>Returns the cached instance for the given code if such instance already exists.</li>
     *   <li>Otherwise delegate to the {@link GeodeticAuthorityFactory#createCoordinateOperation(String)}
     *       method of a backing store and cache the result for future use.</li>
     * </ul>
     *
     * @return The operation for the given code.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.operation.AbstractCoordinateOperation
     */
    @Override
    public CoordinateOperation createCoordinateOperation(final String code) throws FactoryException {
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
     * creation is delegated to the {@linkplain #getBackingStore() backing store}.
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
        final Key key = new Key(type, trimAuthority(code));
        Object value = cache.peek(key);
        if (!type.isInstance(value)) {
            final Cache.Handler<Object> handler = cache.lock(key);
            try {
                value = handler.peek();
                if (!type.isInstance(value)) {
                    final T result;
                    final GeodeticAuthorityFactory factory = getBackingStore();
                    try {
                        result = proxy.create(factory, code);
                    } finally {
                        release();
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
     *       <li>get an instance of the backing store,</li>
     *       <li>delegate to its {@link GeodeticAuthorityFactory#createFromCoordinateReferenceSystemCodes(String, String)} method,</li>
     *       <li>release the backing store — <em>this step assumes that the collection obtained at step 2
     *           is still valid after the backing store has been released</em>,</li>
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
        final Key key = new Key(trimAuthority(sourceCRS), trimAuthority(targetCRS));
        Object value = cache.peek(key);
        if (!(value instanceof Set<?>)) {
            final Cache.Handler<Object> handler = cache.lock(key);
            try {
                value = handler.peek();
                if (!(value instanceof Set<?>)) {
                    final GeodeticAuthorityFactory factory = getBackingStore();
                    try {
                        value = factory.createFromCoordinateReferenceSystemCodes(sourceCRS, targetCRS);
                    } finally {
                        release();
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
     * The default implementation delegates lookup to the underlying backing store and caches the result.
     *
     * @return A finder to use for looking up unidentified objects.
     * @throws FactoryException if the finder can not be created.
     */
    @Override
    public IdentifiedObjectFinder createIdentifiedObjectFinder(final Class<? extends IdentifiedObject> type)
            throws FactoryException
    {
        return new Finder(this, type);
    }

    /**
     * An implementation of {@link IdentifiedObjectFinder} which delegates
     * the work to the underlying backing store and caches the result.
     *
     * <div class="section">Implementation note</div>
     * we will create objects using directly the underlying backing store, not using the cache.
     * This is because hundred of objects may be created during a scan while only one will be typically retained.
     * We do not want to flood the cache with every false candidates that we encounter during the scan.
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
         * Creates a finder for the given type of objects.
         */
        Finder(final ConcurrentAuthorityFactory factory, final Class<? extends IdentifiedObject> type) {
            super(factory, type);
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
                final GeodeticAuthorityFactory delegate = ((ConcurrentAuthorityFactory) factory).getBackingStore();
                /*
                 * Set 'acquireCount' only after we succeed in fetching the factory, and before any operation on it.
                 * The intend is to get ConcurrentAuthorityFactory.release() invoked if and only if the getBackingStore()
                 * method succeed, no matter what happen after this point.
                 */
                acquireCount = 1;
                finder = delegate.createIdentifiedObjectFinder(getObjectType());
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
                // May happen only if a failure occurred during getBackingStore() execution.
                return;
            }
            if (--acquireCount == 0) {
                finder = null;
                ((ConcurrentAuthorityFactory) factory).release();
            }
        }

        /**
         * Returns the authority of the factory examined by this finder.
         */
        @Override
        public synchronized Citation getAuthority() throws FactoryException {
            try {
                acquire();
                return finder.getAuthority();
            } finally {
                release();
            }
        }

        /**
         * Returns a set of authority codes that <strong>may</strong> identify the same
         * object than the specified one. This method delegates to the backing finder.
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
         * Looks up an object from this authority factory which is approximatively equal to the specified object.
         * The default implementation performs the same lookup than the backing store and caches the result.
         */
        @Override
        public IdentifiedObject find(final IdentifiedObject object) throws FactoryException {
            final Map<IdentifiedObject, IdentifiedObject> findPool = ((ConcurrentAuthorityFactory) factory).findPool;
            synchronized (findPool) {
                final IdentifiedObject candidate = findPool.get(object);
                if (candidate != null) {
                    return (candidate == NilReferencingObject.INSTANCE) ? null : candidate;
                }
            }
            /*
             * Nothing has been found in the cache. Delegates the search to the backing store.
             * We must delegate to 'finder' (not to 'super') in order to take advantage of overridden methods.
             */
            final IdentifiedObject candidate;
            synchronized (this) {
                try {
                    acquire();
                    candidate = finder.find(object);
                } finally {
                    release();
                }
            }
            /*
             * If the full scan was allowed, then stores the result even if null so
             * we can remember that no object has been found for the given argument.
             */
            if (candidate != null || isFullScanAllowed()) {
                synchronized (findPool) {
                    findPool.put(object, (candidate == null) ? NilReferencingObject.INSTANCE : candidate);
                }
            }
            return candidate;
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
     * Releases resources immediately instead of waiting for the garbage collector.
     * Once a factory has been disposed, further {@code createFoo(String)} method invocations
     * may throw a {@link FactoryException}. Disposing a previously-disposed factory, however, has no effect.
     */
    @Override
    public void dispose() {
        try {
            int count = 0;
            final Disposable[] factories;
            synchronized (availableStores) {
                isDisposed = true;
                remainingBackingStores = 0;
                factories = new Disposable[availableStores.size()];
                BackingStore store;
                while ((store = availableStores.pollFirst()) != null) {
                    if (store.factory instanceof Disposable) {
                        factories[count++] = (Disposable) store.factory;
                    }
                }
            }
            /*
             * Factory disposal must be done outside the synchronized block.
             */
            while (--count >= 0) {
                factories[count].dispose();
            }
        } finally {
            synchronized (findPool) {
                findPool.clear();
            }
            cache.clear();
            authority = null;
        }
    }
}
