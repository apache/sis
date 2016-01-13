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
package org.apache.sis.util.collection;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.ref.SoftReference;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.DelayedRunnable;
import org.apache.sis.internal.system.DelayedExecutor;
import org.apache.sis.internal.system.ReferenceQueueConsumer;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Supplier;


/**
 * A concurrent cache mechanism. This implementation is thread-safe and supports concurrency.
 * A cache entry can be locked when an object is in process of being created. The steps
 * are as below:
 *
 * <ol>
 *   <li>Check if the value is already available in the map.
 *       If it is, return it immediately and we are done.</li>
 *   <li>Otherwise, get a lock and check again if the value is already available in the map
 *       (because the value could have been computed by an other thread between step 1 and
 *       the obtention of the lock). If it is, release the lock and we are done.</li>
 *   <li>Otherwise compute the value, store the result and release the lock.</li>
 * </ol>
 *
 * The easiest way (except for exception handling) to use this class is to prepare a
 * {@link Callable} statement to be executed only if the object is not in the cache,
 * and to invoke the {@link #getOrCreate getOrCreate} method. Example:
 *
 * {@preformat java
 *     private final Cache<String,MyObject> cache = new Cache<String,MyObject>();
 *
 *     public MyObject getMyObject(final String key) throws MyCheckedException {
 *         try {
 *             return cache.getOrCreate(key, new Callable<MyObject>() {
 *                 MyObject call() throws FactoryException {
 *                     return createMyObject(key);
 *                 }
 *             });
 *         } catch (MyCheckedException e) {
 *             throw e;
 *         } catch (RuntimeException e) {
 *             throw e;
 *         } catch (Exception e) {
 *             throw new UndeclaredThrowableException(e);
 *         }
 *     }
 * }
 *
 * An alternative is to perform explicitly all the steps enumerated above. This alternative
 * avoid the creation of a temporary {@code Callable} statement which may never be executed,
 * and avoid the exception handling due to the {@code throws Exception} clause. Note that the
 * call to {@link Handler#putAndUnlock putAndUnlock} <strong>must</strong> be in the {@code finally}
 * block of a {@code try} block beginning immediately after the call to {@link #lock lock},
 * no matter what the result of the computation is (including {@code null}).
 *
 * {@preformat java
 *     private final Cache<String,MyObject> cache = new Cache<String,MyObject>();
 *
 *     public MyObject getMyObject(final String key) throws MyCheckedException {
 *         MyObject value = cache.peek(key);
 *         if (value == null) {
 *             final Cache.Handler<MyObject> handler = cache.lock(key);
 *             try {
 *                 value = handler.peek();
 *                 if (value == null) {
 *                     value = createMyObject(key);
 *                 }
 *             } finally {
 *                 handler.putAndUnlock(value);
 *             }
 *         }
 *         return value;
 *     }
 * }
 *
 *
 * <div class="section">Eviction of eldest values</div>
 *
 * <ul>
 *   <li>The <cite>cost</cite> of a value is the value returned by {@link #cost}. The default
 *       implementation returns 1 in all cases, but subclasses can override this method for
 *       more elaborated cost computation.</li>
 *   <li>The <cite>total cost</cite> is the sum of the cost of all values held by strong
 *       reference in this cache. The total cost does not include the cost of values held
 *       by {@linkplain Reference weak or soft reference}.</li>
 *   <li>The <cite>cost limit</cite> is the maximal value allowed for the total cost. If
 *       the total cost exceed this value, then strong references to the eldest values are
 *       replaced by {@linkplain Reference weak or soft references} until the total cost
 *       become equals or lower than the cost limit.</li>
 * </ul>
 *
 * The total cost is given at construction time. If the {@link #cost} method has not been
 * overridden, then the total cost is the maximal amount of values to keep by strong references.
 *
 *
 * <div class="section">Circular dependencies</div>
 *
 * This implementation assumes that there is no circular dependencies (or cyclic graph) between
 * the values in the cache. For example if creating <var>A</var> implies creating <var>B</var>,
 * then creating <var>B</var> is not allowed to implies (directly or indirectly) the creation of
 * <var>A</var>. If this rule is not meet, deadlock may occur randomly.
 *
 * @param <K> The type of key objects.
 * @param <V> The type of value objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public class Cache<K,V> extends AbstractMap<K,V> {
    /**
     * The map that contains the cached values. If a value is under the process of being
     * calculated, then the value will be a temporary instance of {@link Handler}. The
     * value may also be weak or soft {@link Reference} objects.
     */
    private final ConcurrentMap<K,Object> map;

    /**
     * The keys of values that are retained in the {@linkplain #map} by strong references,
     * together with an estimation of their cost. This map is <strong>not</strong> thread
     * safe. For this reason, it must be used by a single thread at a given time, even
     * for read-only operations.
     *
     * <p>Entries in this map are ordered from least-recently accessed to most-recently accessed.</p>
     */
    private final Map<K,Integer> costs;

    /**
     * The sum of all values in the {@link #costs} map. This field must be used in the
     * same thread than {@link #costs}.
     */
    private long totalCost;

    /**
     * The maximal cost allowed. If the {@link #totalCost} is above that limit, then the eldest
     * strong references will be replaced by {@linkplain Reference weak or soft references}.
     */
    private final long costLimit;

    /**
     * If {@code true}, use {@link SoftReference} instead of {@link WeakReference}.
     */
    private final boolean soft;

    /**
     * {@code true} if different values may be assigned to the same key. This is usually
     * an error, so the default {@code Cache} behavior is to thrown an exception in such
     * case.
     *
     * @see #isKeyCollisionAllowed()
     */
    private volatile boolean isKeyCollisionAllowed;

    /**
     * A view over the entries in the cache.
     */
    private volatile transient Set<Entry<K,V>> entries;

    /**
     * Creates a new cache with a default initial capacity and cost limit of 100.
     * The oldest objects will be hold by {@linkplain WeakReference weak references}.
     */
    public Cache() {
        this(12, 100, false);
    }

    /**
     * Creates a new cache using the given initial capacity and cost limit. The initial capacity
     * is the expected number of values to be stored in this cache. More values are allowed, but
     * a little bit of CPU time may be saved if the expected capacity is known before the cache
     * is created.
     *
     * <p>The <cite>cost limit</cite> is the maximal value of the <cite>total cost</cite> (the sum
     * of the {@linkplain #cost cost} of all values) before to replace eldest strong references by
     * {@linkplain Reference weak or soft references}.</p>
     *
     * @param initialCapacity the initial capacity.
     * @param costLimit The maximum number of objects to keep by strong reference.
     * @param soft If {@code true}, use {@link SoftReference} instead of {@link WeakReference}.
     */
    public Cache(int initialCapacity, final long costLimit, final boolean soft) {
        ArgumentChecks.ensureStrictlyPositive("initialCapacity", initialCapacity);
        ArgumentChecks.ensurePositive("costLimit", costLimit);
        initialCapacity = Containers.hashMapCapacity(initialCapacity);
        this.map        = new ConcurrentHashMap<K,Object>(initialCapacity);
        this.costs      = new LinkedHashMap<K,Integer>((int) Math.min(initialCapacity, costLimit), 0.75f, true);
        this.costLimit  = costLimit;
        this.soft       = soft;
    }

    /**
     * Clears the content of this cache.
     */
    @Override
    public void clear() {
        map.clear();
        // Do not update "costs" and "totalCost". Instead let adjustReferences(...)
        // do its job, which needs to be done in a different thread.
    }

    /**
     * Returns {@code true} if this cache is empty.
     *
     * @return {@code true} if this cache do not contains any element.
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns the number of elements in this cache. The count includes values keep by strong,
     * {@linkplain SoftReference soft} or {@linkplain WeakReference weak} references, and the
     * values under computation at the time this method is invoked.
     *
     * @return The number of elements currently cached.
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Returns {@code true} if this map contains the specified key.
     *
     * @param  key The key to check for existence.
     * @return {@code true} if the given key still exist in this cache.
     */
    @Override
    public boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    /**
     * Returns {@code true} if the given value is an instance of one of the reserved types
     * used internally by this class.
     */
    static boolean isReservedType(final Object value) {
        return (value instanceof Handler<?> || value instanceof Reference<?>);
    }

    /**
     * Returns the value of the given object, unwrapping it if possible.
     */
    @SuppressWarnings("unchecked")
    private static <V> V valueOf(final Object value) {
        if (value instanceof Reference<?>) {
            return ((Reference<V>) value).get();
        }
        if (value instanceof Handler<?>) {
            return ((Supplier<V>) value).get();
            /*
             * A ClassCastException on the above line would be a bug in this class.
             * See the comment in Cache.lock(K) method for more explanations.
             */
        }
        return (V) value;
    }

    /**
     * Puts the given value in cache.
     *
     * @param  key   The key for which to set a value.
     * @param  value The value to store.
     * @return The value previously stored at the given key, or {@code null} if none.
     */
    @Override
    public V put(final K key, final V value) {
        if (isReservedType(value)) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentClass_2, "value", value.getClass()));
        }
        final Object previous;
        if (value != null) {
            previous = map.put(key, value);
            DelayedExecutor.schedule(new Strong(key, value));
        } else {
            previous = map.remove(key);
        }
        return Cache.<V>valueOf(previous);
    }

    /**
     * Removes the value associated to the given key in the cache.
     *
     * @param  key The key of the value to removed.
     * @return The value that were associated to the given key, or {@code null} if none.
     */
    @Override
    public V remove(final Object key) {
        return Cache.<V>valueOf(map.remove(key));
    }

    /**
     * Returns the value associated to the given key in the cache. This method is similar to
     * {@link #peek} except that it blocks if the value is currently under computation in an
     * other thread.
     *
     * @param  key The key of the value to get.
     * @return The value associated to the given key, or {@code null} if none.
     */
    @Override
    public V get(final Object key) {
        return Cache.<V>valueOf(map.get(key));
    }

    /**
     * Returns the value for the given key. If a value already exists in the cache, then it
     * is returned immediately. Otherwise the {@code creator.call()} method is invoked and
     * its result is saved in this cache for future reuse.
     *
     * @param  key The key for which to get the cached or created value.
     * @param  creator A method for creating a value, to be invoked only if no value are
     *         cached for the given key.
     * @return The value for the given key, which may have been created as a result of this
     *         method call.
     * @throws Exception If an exception occurred during the execution of {@code creator.call()}.
     */
    public V getOrCreate(final K key, final Callable<? extends V> creator) throws Exception {
        V value = peek(key);
        if (value == null) {
            final Handler<V> handler = lock(key);
            try {
                value = handler.peek();
                if (value == null) {
                    value = creator.call();
                }
            } finally {
                handler.putAndUnlock(value);
            }
        }
        return value;
    }

    /**
     * If a value is already cached for the given key, returns it. Otherwise returns {@code null}.
     * This method is similar to {@link #get(Object)} except that it doesn't block if the value is
     * in process of being computed in an other thread; it returns {@code null} in such case.
     *
     * @param  key The key for which to get the cached value.
     * @return The cached value for the given key, or {@code null} if there is none.
     */
    public V peek(final K key) {
        final Object value = map.get(key);
        if (value instanceof Handler<?>) {
            // The value is under computation. We will not wait for it since it is
            // not the purpose of this method (we should use lock(key) for that).
            return null;
        }
        if (value instanceof Reference<?>) {
            @SuppressWarnings("unchecked")
            final Reference<V> ref = (Reference<V>) value;
            final V result = ref.get();
            if (result != null && map.replace(key, ref, result)) {
                ref.clear();                        // Prevents the reference from being enqueued.
                DelayedExecutor.schedule(new Strong(key, result));
            }
            return result;
        }
        @SuppressWarnings("unchecked")
        final V result = (V) value;
        return result;
    }

    /**
     * Invoked from the a background thread after a {@linkplain WeakReference weak}
     * or {@linkplain SoftReference soft} reference has been replaced by a strong one. It will
     * looks for older strong references to replace by weak references so that the total cost
     * stay below the cost limit.
     */
    private final class Strong extends DelayedRunnable.Immediate {
        private final K key;
        private final V value;

        Strong(final K key, final V value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Process to the replacement of eldest strong references by weak references.
         * This method should be invoked from the background thread only.
         */
        @Override public void run() {
            Cache.this.adjustReferences(key, value);
        }
    }

    /**
     * Gets a lock for the entry at the given key and returns a handler to be used by the caller
     * for unlocking and storing the result. This method <strong>must</strong> be used together
     * with a {@link Handler#putAndUnlock(Object) putAndUnlock} call in {@code try} … {@code catch}
     * blocks as in the example below:
     *
     * {@preformat java
     *     Cache.Handler handler = cache.lock();
     *     try {
     *         // Compute the result...
     *     } finally {
     *         handler.putAndUnlock(result);
     *     }
     * }
     *
     * @param  key The key for the entry to lock.
     * @return A handler to use for unlocking and storing the result.
     */
    public Handler<V> lock(final K key) {
        final Work handler = new Work(key);
        boolean unlock = true;
        handler.lock.lock();
        Object value;
        try {
            do {
                /*
                 * Put the handler in the map, providing that the entry is not already occupied.
                 * Note that the handler must be locked BEFORE we attempt to add it to the map.
                 */
                value = map.putIfAbsent(key, handler);
                if (value == null) {
                    /*
                     * We succeed in adding the handler in the map (we know that because all our
                     * map.put(...) or map.replace(...) operations are guaranteed to put non-null
                     * values). We are done. But before to leave, declare that we do not want to
                     * unlock in the finally clause (we want the lock to still active).
                     */
                    unlock = false;
                    return handler;
                }
                /*
                 * If the value is a strong reference or other handler, stop the loop and release the lock.
                 * We will process that value after the finally block.
                 */
                if (!(value instanceof Reference<?>)) {
                    break;
                }
                @SuppressWarnings("unchecked")
                final Reference<V> ref = (Reference<V>) value;
                final V result = ref.get();
                if (result != null) {
                    /*
                     * The value is a valid weak reference. Replaces it by a strong reference
                     * before to return it in a wrapper. Note: the call to ref.clear() is for
                     * preventing the reference from being enqueued for ReferenceQueueConsumer
                     * processing. It would not hurt if we let the processing happen, but it
                     * would be useless.
                     */
                    if (map.replace(key, ref, result)) {
                        ref.clear();                        // Prevents the reference from being enqueued.
                        DelayedExecutor.schedule(new Strong(key, result));
                    }
                    return new Simple<V>(result);
                }
                /*
                 * The weak reference is invalid but not yet discarded (it looks like that this
                 * thread is going faster than ReferenceQueueConsumer). Try to replace it by our
                 * handler.
                 */
                if (map.replace(key, ref, handler)) {
                    unlock = false;
                    return handler;
                }
                // The map content changed. Try again.
            } while (true);
        } finally {
            if (unlock) {
                handler.lock.unlock();
            }
        }
        /*
         * From this point, we abandon our handler.
         */
        if (value instanceof Handler<?>) {
            /*
             * A value is already under computation. Returns a handler which will wait for the
             * completion of the worker thread and returns its result. Note that the handler
             * should never be any kind other than Work because this is the only kind we put
             * in the map (see a few lines above in this method), so if we get a ClassCastException
             * here this is a bug in this class.
             */
            @SuppressWarnings("unchecked")
            final Work work = (Work) value;
            if (work.lock.isHeldByCurrentThread()) {
                if (isKeyCollisionAllowed()) {
                    /*
                     * Example of key collision: the EPSG database defines the CoordinateOperation
                     * 8653 ("ED50 to WGS84" using polynomial equations).  The EPSG factory sets a
                     * lock for this code, then searches for OperationParameters associated to this
                     * operation. One of those parameters ("Bu0v4") has the same key (EPSG:8653).
                     * So we get a key collision. If we ignore the second occurrence, its value will
                     * not be cached. This is okay since the value that we really want to cache is
                     * CoordinateOperation, which is associated to the first occurrence of that key.
                     */
                    return new Simple<V>(null);
                }
                throw new IllegalStateException(Errors.format(Errors.Keys.RecursiveCreateCallForKey_1, key));
            }
            return work.new Wait();
        }
        /*
         * A calculation has already been completed. Returns a wrapper
         * which will just return the result without any processing.
         */
        assert !isReservedType(value) : value;
        @SuppressWarnings("unchecked")
        final V result = (V) value;
        return new Simple<V>(result);
    }

    /**
     * The handler returned by {@link Cache#lock}, to be used for unlocking and storing the
     * result. This handler should be used as below (note the {@code try} … {@code catch}
     * blocks, which are <strong>mandatory</strong>):
     *
     * {@preformat java
     *     Value V = null;
     *     Cache.Handler<V> handler = cache.lock(key);
     *     try {
     *         value = handler.peek();
     *         if (value == null) {
     *             value = createMyObject(key);
     *         }
     *     } finally {
     *         handler.putAndUnlock(value);
     *     }
     * }
     *
     * See the {@link Cache} javadoc for a more complete example.
     *
     * @param <V> The type of value objects.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    public interface Handler<V> {
        /**
         * If the value is already in the cache, returns it. Otherwise returns {@code null}.
         * This method should be invoked after the {@code Handler} creation in case a value
         * has been computed in an other thread.
         *
         * @return The value from the cache, or {@code null} if none.
         */
        V peek();

        /**
         * Stores the given value in the cache and release the lock. This method
         * <strong>must</strong> be invoked in a {@code finally} block, no matter
         * what the result is.
         *
         * @param result The result to store in the cache, or {@code null} for removing
         *        the entry from the cache. If an entry is removed, a new computation
         *        will be attempted the next time a handler is created for the same key.
         *
         * @throws IllegalStateException May be thrown if this method is not invoked in
         *         the pattern described in class javadoc, or if a key collision occurs.
         */
        void putAndUnlock(V result) throws IllegalStateException;
    }

    /**
     * A simple handler implementation wrapping an existing value. This implementation
     * is used when the value has been fully calculated in an other thread before this
     * thread could start its work.
     */
    private final class Simple<V> implements Handler<V> {
        /**
         * The result calculated in an other thread.
         */
        private final V value;

        /**
         * Creates a new handler wrapping the given value.
         */
        Simple(final V value) {
            this.value = value;
        }

        /**
         * Returns the calculated value.
         */
        @Override
        public V peek() {
            return value;
        }

        /**
         * Do nothing (except checking for programming error), since we don't hold any lock.
         *
         * <div class="note"><b>Implementation note:</b>
         * An alternative would have been to store the result in the map anyway.
         * But doing so is unsafe because we have no lock; we have no guarantee that nothing
         * has happened in an other thread between {@code peek} and {@code putAndUnlock}.</div>
         */
        @Override
        public void putAndUnlock(final V result) throws IllegalStateException {
            if (result != value && !isKeyCollisionAllowed()) {
                throw new IllegalStateException(Errors.format(Errors.Keys.KeyCollision_1, "<unknown>"));
            }
        }
    }

    /**
     * A handler implementation used for telling to other threads that the current thread is
     * computing a value.
     */
    final class Work extends DelayedRunnable.Immediate implements Handler<V>, Supplier<V> {
        /**
         * The synchronization lock.
         */
        final ReentrantLock lock;

        /**
         * The key to use for storing the result in the map.
         */
        final K key;

        /**
         * The result. This is initially null, as we expect since the value has not yet
         * been created. When it will get a value, this value should not change anymore.
         */
        private V value;

        /**
         * Creates a new handler which will store the result in the given map at the given key.
         */
        Work(final K key) {
            lock = new ReentrantLock();
            this.key = key;
        }

        /**
         * Waits for the completion of the value computation and returns this result. This
         * method should be invoked only from an other thread than the one doing the calculation.
         */
        @Override
        public V get() {
            if (lock.isHeldByCurrentThread()) {
                return null;
            }
            final V v;
            lock.lock();
            v = value;
            lock.unlock();
            return v;
        }

        /**
         * Usually returns {@code null} since the value is not yet computed. May returns the
         * result if this method is invoked again after the computation, but this is not the
         * typical use case.
         */
        @Override
        public V peek() {
            return value;
        }

        /**
         * Stores the result and release the lock.
         *
         * @throws IllegalStateException If the current thread does not hold the lock.
         */
        @Override
        public void putAndUnlock(final V result) throws IllegalStateException {
            final boolean done;
            try {
                if (isReservedType(result)) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.IllegalArgumentClass_2, "result", result.getClass()));
                }
                // Assignation of 'value' must happen before we release the lock.
                value = result;
                if (result != null) {
                    done = map.replace(key, this, result);
                } else {
                    done = map.remove(key, this);
                }
            } finally {
                lock.unlock();
            }
            if (done) {
                DelayedExecutor.schedule(this);
            }
        }

        /**
         * A handler implementation used when the value is in process of being computed in an
         * other thread. At the difference of the {@code Simple} handler, the computation is
         * not yet completed, so this handler has to wait.
         */
        final class Wait implements Handler<V> {
            /**
             * Waits that the worker finish its work and returns its value.
             */
            @Override
            public V peek() {
                return get();
            }

            /**
             * Do nothing (except checking for programming error), since we don't hold any lock.
             *
             * <div class="note"><b>Implementation note:</b>
             * An alternative would have been to store the result in the map anyway.
             * But doing so is unsafe because we have no lock; we have no guarantee that nothing
             * has happened in an other thread between {@code peek} and {@code putAndUnlock}.</div>
             */
            @Override
            public void putAndUnlock(final V result) throws IllegalStateException {
                if (result != null && !isKeyCollisionAllowed() && result != get()) {
                    throw new IllegalStateException(Errors.format(Errors.Keys.KeyCollision_1, key));
                }
            }
        }

        /**
         * Invoked in a background thread after a value has been set in the map.
         * This method computes a cost estimation of the new value. If the total cost is greater
         * than the cost limit, then oldest strong references are replaced by weak references.
         */
        @Override
        public void run() {
            final V value = this.value;
            if (value != null) {
                Cache.this.adjustReferences(key, value);
            }
        }
    }

    /**
     * Invoked in a background thread after a value has been set in the map.
     * This method computes a cost estimation of the new value. If the total cost is greater
     * than the cost limit, then oldest strong references are replaced by weak references.
     */
    final void adjustReferences(final K key, final V value) {
        int cost = cost(value);
        synchronized (costs) { // Should not be needed, but done as a safety.
            final Integer old = costs.put(key, cost);
            if (old != null) {
                cost -= old;
            }
            if ((totalCost += cost) > costLimit) {
                final Iterator<Map.Entry<K,Integer>> it = costs.entrySet().iterator();
                while (it.hasNext()) {
                    /*
                     * Converts the current entry from strong reference to weak/soft reference.
                     * We perform this conversion even if the entry is for the value just added
                     * to the cache, if it happen that the cost is higher than the maximal one.
                     * That entry should not be garbage collected to early anyway because the
                     * caller should still have a strong reference to the value he just created.
                     */
                    final Map.Entry<K,Integer> entry = it.next();
                    final K oldKey = entry.getKey();
                    final Object oldValue = map.get(oldKey);
                    if (oldValue != null && !isReservedType(oldValue)) {
                        @SuppressWarnings("unchecked")
                        final Reference<V> ref = soft ? new Soft<K,V>(map, oldKey, (V) oldValue)
                                                      : new Weak<K,V>(map, oldKey, (V) oldValue);
                        if (!map.replace(oldKey, oldValue, ref)) {
                            ref.clear(); // Prevents the reference to be enqueued.
                        }
                    }
                    it.remove();
                    if ((totalCost -= entry.getValue()) <= costLimit) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * A soft reference which remove itself from the concurrent map when the reference
     * is garbage-collected.
     */
    private static final class Soft<K,V> extends SoftReference<V> implements Disposable {
        /** The key of the referenced value.      */ private final K key;
        /** The map which contains the reference. */ private final ConcurrentMap<K,Object> map;

        /** Creates a references to be stored in the given map under the given key. */
        Soft(final ConcurrentMap<K,Object> map, final K key, final V value) {
            super(value, ReferenceQueueConsumer.QUEUE);
            this.map = map;
            this.key = key;
        }

        /** Removes the reference from the map. */
        @Override public void dispose() {
            map.remove(key, this);
            // There is nothing to remove from the cost map, since the later
            // contains only the keys of objects hold by strong reference.
        }
    }

    /**
     * A weak reference which remove itself from the concurrent map when the reference
     * is garbage-collected.
     */
    private static final class Weak<K,V> extends WeakReference<V> implements Disposable {
        /** The key of the referenced value.      */ private final K key;
        /** The map which contains the reference. */ private final ConcurrentMap<K,Object> map;

        /** Creates a references to be stored in the given map under the given key. */
        Weak(final ConcurrentMap<K,Object> map, final K key, final V value) {
            super(value, ReferenceQueueConsumer.QUEUE);
            this.map = map;
            this.key = key;
        }

        /** Removes the reference from the map. */
        @Override public void dispose() {
            map.remove(key, this);
            // There is nothing to remove from the cost map, since the later
            // contains only the keys of objects hold by strong reference.
        }
    }

    /**
     * Returns the set of keys in this cache. The returned set is subjects to the same caution
     * than the ones documented in the {@link ConcurrentHashMap#keySet()} method.
     *
     * @return The set of keys in this cache.
     */
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * Returns the set of entries in this cache. The returned set is subjects to the same caution
     * than the ones documented in the {@link ConcurrentHashMap#entrySet()} method, except that
     * it doesn't support removal of elements (including through the {@link Iterator#remove}
     * method call).
     *
     * @return A view of the entries contained in this map.
     */
    @Override
    public Set<Entry<K,V>> entrySet() {
        final Set<Entry<K,V>> es = entries;
        return (es != null) ? es : (entries = new CacheEntries<K,V>(map.entrySet()));
    }

    /**
     * Returns {@code true} if different values may be assigned to the same key.
     * The default value is {@code false}.
     *
     * @return {@code true} if key collisions are allowed.
     */
    public boolean isKeyCollisionAllowed() {
        return isKeyCollisionAllowed;
    }

    /**
     * If set to {@code true}, different values may be assigned to the same key. This is usually an
     * error, so the default {@code Cache} behavior is to thrown an {@link IllegalStateException}
     * in such cases, typically when {@link Handler#putAndUnlock(Object)} is invoked. However in
     * some cases we may want to relax this check. For example the EPSG database sometime assigns
     * the same key to different kind of objects.
     *
     * <p>If key collisions are allowed and two threads invoke {@link #lock(Object)} concurrently
     * for the same key, then the value to be stored in the map will be the one computed by the
     * first thread who got the lock. The value computed by any other concurrent thread will be
     * ignored by this {@code Cache} class. However those threads still return their computed
     * values to their callers.</p>
     *
     * <p>This property can also be set in order to allow some recursivity. If during the creation
     * of an object, the program asks to this {@code Cache} for the same object (using the same key),
     * then the default {@code Cache} implementation will consider this situation as a key collision
     * unless this property has been set to {@code true}.</p>
     *
     * @param allowed {@code true} if key collisions should be allowed.
     */
    public void setKeyCollisionAllowed(final boolean allowed) {
        isKeyCollisionAllowed = allowed;
    }

    /**
     * Computes an estimation of the cost of the given value. The default implementation returns 1
     * in all cases. Subclasses should override this method if they have some easy way to measure
     * the relative cost of value objects.
     *
     * @param  value The object for which to get an estimation of its cost.
     * @return The estimated cost of the given object.
     *
     * @see java.lang.instrument.Instrumentation#getObjectSize(Object)
     */
    protected int cost(final V value) {
        return 1;
    }
}
