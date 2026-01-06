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
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.ref.SoftReference;
import org.apache.sis.pending.jdk.JDK16;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.system.DelayedRunnable;
import org.apache.sis.system.DelayedExecutor;
import org.apache.sis.system.ReferenceQueueConsumer;


/**
 * A concurrent map capable to locks entries for which the value is in process of being computed.
 * This map is intended for use as a cache, with a goal of avoiding to compute the same values twice.
 * This implementation is thread-safe and supports concurrency.
 * {@code Cache} is based on {@link ConcurrentHashMap} with the addition of three main capabilities:
 *
 * <ul>
 *   <li>Lock an entry when its value is under computation in a thread.</li>
 *   <li>Block other threads requesting the value of that particular entry until computation is completed.</li>
 *   <li>Retain oldest values by soft or weak references instead of strong references.</li>
 * </ul>
 *
 * The easiest way to use this class is to invoke {@link #computeIfAbsent computeIfAbsent(…)}
 * or {@link #getOrCreate getOrCreate(…)} with lambda functions as below:
 *
 * {@snippet lang="java" :
 *     class MyClass {
 *         private final Cache<String,MyObject> cache = new Cache<String,MyObject>();
 *
 *         public MyObject getMyObject(String key) {
 *             return cache.computeIfAbsent(key, (k) -> createMyObject(k));
 *         }
 *     }
 *     }
 *
 * Alternatively, one can handle explicitly the locks.
 * This alternative sometimes provides more flexibility, for example in exception handling.
 * The steps are as below:
 *
 * <ol>
 *   <li>Check if the value is already available in the map.
 *       If it is, return it immediately and we are done.</li>
 *   <li>Otherwise, get a lock and check again if the value is already available in the map
 *       (because the value could have been computed by another thread between step 1 and
 *       the obtention of the lock). If it is, release the lock and we are done.</li>
 *   <li>Otherwise compute the value, store the result and release the lock.</li>
 * </ol>
 *
 * Code example is shown below.
 * Note that the call to {@link Handler#putAndUnlock putAndUnlock(…)} <strong>must</strong>
 * be inside the {@code finally} block of a {@code try} block beginning immediately after the call
 * to {@link #lock lock(…)}, no matter what the result of the computation is (including {@code null}).
 *
 * {@snippet lang="java" :
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
 * <h2>Eviction of eldest values</h2>
 *
 * <ul>
 *   <li>The <dfn>cost</dfn> of a value is the value returned by {@link #cost}. The default
 *       implementation returns 1 in all cases, but subclasses can override this method for
 *       more elaborated cost computation.</li>
 *   <li>The <dfn>total cost</dfn> is the sum of the cost of all values held by strong
 *       reference in this cache. The total cost does not include the cost of values held
 *       by {@linkplain Reference weak or soft reference}.</li>
 *   <li>The <dfn>cost limit</dfn> is the maximal value allowed for the total cost. If
 *       the total cost exceed this value, then strong references to the eldest values are
 *       replaced by {@linkplain Reference weak or soft references} until the total cost
 *       become equals or lower than the cost limit.</li>
 * </ul>
 *
 * The total cost is given at construction time. If the {@link #cost} method has not been
 * overridden, then the total cost is the maximal number of values to keep by strong references.
 *
 *
 * <h2>Circular dependencies</h2>
 *
 * This implementation assumes that there are no circular dependencies (or cyclic graph) between
 * the values in the cache. For example if creating <var>A</var> implies creating <var>B</var>,
 * then creating <var>B</var> is not allowed to implies (directly or indirectly) the creation of
 * <var>A</var>. If this condition is not met, deadlock may occur randomly.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.3
 *
 * @param <K>  the type of key objects.
 * @param <V>  the type of value objects.
 *
 * @since 0.3
 */
public class Cache<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V> {
    /**
     * The map that contains the cached values. If a value is in the process of being computed,
     * then the value is a temporary instance of {@link Handler}. Otherwise the value is a weak
     * or soft {@link Reference} objects, or otherwise a strong {@code <V>} reference.
     *
     * @see #isReservedType(Object)
     */
    private final ConcurrentMap<K,Object> map;

    /**
     * The keys of values that are retained in the {@linkplain #map} by strong references,
     * together with an estimation of their cost. This map is <strong>not</strong> thread safe;
     * it shall be used by a single thread at any given time, even for read-only operations.
     *
     * <p>Entries in this map are ordered from least-recently accessed to most-recently accessed.</p>
     */
    private final Map<K,Integer> costs;

    /**
     * The sum of all values in the {@link #costs} map.
     * This field shall be read and updated in the same thread as {@link #costs}.
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
     * {@code true} if different values may be assigned to the same key.
     * This is usually an error, so the default {@code Cache} behavior
     * is to throw an exception in such cases.
     *
     * @see #isKeyCollisionAllowed()
     */
    private volatile boolean isKeyCollisionAllowed;

    /**
     * A view over the entries in the cache.
     *
     * @see #entrySet()
     */
    private transient volatile Set<Entry<K,V>> entries;

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
     * <p>The <dfn>cost limit</dfn> is the maximal value of the <dfn>total cost</dfn> (the sum
     * of the {@linkplain #cost cost} of all values) before to replace eldest strong references
     * by {@linkplain Reference weak or soft references}.</p>
     *
     * @param initialCapacity  the initial capacity.
     * @param costLimit        the maximum cost (inclusive) of objects to keep by strong reference.
     * @param soft             if {@code true}, use {@link SoftReference} instead of {@link WeakReference}.
     */
    public Cache(int initialCapacity, final long costLimit, final boolean soft) {
        ArgumentChecks.ensureStrictlyPositive("initialCapacity", initialCapacity);
        ArgumentChecks.ensurePositive("costLimit", costLimit);
        initialCapacity = Containers.hashMapCapacity(initialCapacity);
        this.map        = new ConcurrentHashMap<>(initialCapacity);
        this.costs      = new LinkedHashMap<>((int) Math.min(initialCapacity, costLimit), 0.75f, true);
        this.costLimit  = costLimit;
        this.soft       = soft;
    }

    /**
     * Clears the content of this cache.
     */
    @Override
    public void clear() {
        synchronized (costs) {
            map.clear();
            costs.clear();
            totalCost = 0;
        }
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
     * @return the number of elements currently cached.
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Returns the value mapped to the given key in the cache, potentially waiting for computation to complete.
     * This method is similar to {@link #peek(Object)} except that it blocks if the value is currently under
     * computation in another thread.
     *
     * @param  key  the key of the value to get.
     * @return the value mapped to the given key, or {@code null} if none.
     *
     * @see #peek(Object)
     * @see #containsKey(Object)
     * @see #computeIfAbsent(Object, Function)
     */
    @Override
    public V get(final Object key) {
        return valueOf(map.get(key));
    }

    /**
     * Returns the value for the given key if it exists, or computes it otherwise.
     * If a value already exists in the cache, then it is returned immediately.
     * Otherwise the {@code creator.call()} method is invoked and its result is saved in this cache for future reuse.
     *
     * <p>This method is similar to {@link #computeIfAbsent(Object, Function)} except that it can propagate checked exceptions.
     * If the {@code creator} function does not throw any checked exception,
     * then invoking {@code computeIfAbsent(…)} is simpler.</p>
     *
     * <h4>Example</h4>
     * the following example shows how this method can be used.
     * In particular, it shows how to propagate {@code MyCheckedException}:
     *
     * {@snippet lang="java" :
     *     private final Cache<String,MyObject> cache = new Cache<String,MyObject>();
     *
     *     public MyObject getMyObject(final String key) throws MyCheckedException {
     *         try {
     *             return cache.getOrCreate(key, new Callable<MyObject>() {
     *                 public MyObject call() throws MyCheckedException {
     *                     return createMyObject(key);
     *                 }
     *             });
     *         } catch (MyCheckedException | RuntimeException e) {
     *             throw e;
     *         } catch (Exception e) {
     *             throw new UndeclaredThrowableException(e);
     *         }
     *     }
     * }
     *
     * @param  key      the key for which to get the cached or created value.
     * @param  creator  a method for creating a value, to be invoked only if no value are cached for the given key.
     * @return the value for the given key, which may have been created as a result of this method call.
     * @throws Exception if an exception occurred during the execution of {@code creator.call()}.
     *
     * @see #get(Object)
     * @see #peek(Object)
     * @see #computeIfAbsent(Object, Function)
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
     * Returns the value for the given key if it exists, or computes it otherwise.
     * If a value already exists in the cache, then it is returned immediately.
     * Otherwise the {@code creator.apply(Object)} method is invoked and its result
     * is saved in this cache for future reuse.
     *
     * <p>This method is similar to {@link #getOrCreate(Object, Callable)}, but without checked exceptions.</p>
     *
     * <h4>Example</h4>
     * below is the same code as {@link #getOrCreate(Object, Callable)} example,
     * but without the need for any checked exception handling:
     *
     * {@snippet lang="java" :
     *     private final Cache<String,MyObject> cache = new Cache<String,MyObject>();
     *
     *     public MyObject getMyObject(final String key) {
     *         return cache.computeIfAbsent(key, (k) -> createMyObject(k));
     *     }
     * }
     *
     * @param  key      the key for which to get the cached or created value.
     * @param  creator  a method for creating a value, to be invoked only if no value are cached for the given key.
     * @return the value already mapped to the key, or the newly computed value.
     *
     * @since 1.0
     *
     * @see #peek(Object)
     * @see #containsKey(Object)
     * @see #getOrCreate(Object, Callable)
     * @see #computeIfPresent(Object, BiFunction)
     */
    @Override
    public V computeIfAbsent(final K key, final Function<? super K, ? extends V> creator) {
        V value = peek(key);
        if (value == null) {
            final Handler<V> handler = lock(key);
            try {
                value = handler.peek();
                if (value == null) {
                    value = creator.apply(key);
                }
            } finally {
                handler.putAndUnlock(value);
            }
        }
        return value;
    }

    /**
     * Returns {@code true} if the given value is null or a cleared reference.
     *
     * @param  value  the value to test.
     * @return whether the given value is null or a cleared reference.
     */
    private static boolean isNull(final Object value) {
        return (value == null) || (value instanceof Reference<?> && JDK16.refersTo((Reference<?>) value, null));
    }

    /**
     * Returns {@code true} if the given value is an instance of one of the reserved types
     * used internally by this class.
     */
    private static boolean isReservedType(final Object value) {
        return (value instanceof Handler<?> || value instanceof Reference<?>);
    }

    /**
     * Ensures that the given value is not an instance of a reserved type.
     */
    private static void ensureValidType(final Object value) throws IllegalArgumentException {
        if (isReservedType(value)) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentClass_2, "value", value.getClass()));
        }
    }

    /**
     * Returns the value of the given object, unwrapping it if possible.
     * If the value is under computation in another thread, this method
     * will block until the computation is completed.
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
     * Returns the value of the given object if it is not under computation.
     * This method is similar to {@link #valueOf(Object)} except that it does
     * not block if the value is under computation.
     */
    @SuppressWarnings("unchecked")
    private static <V> V immediateValueOf(final Object value) {
        if (value instanceof Reference<?>) {
            return ((Reference<V>) value).get();
        }
        if (value instanceof Handler<?>) {
            return null;
        }
        return (V) value;
    }

    /**
     * Notifies this {@code Cache} instance that an entry has changed.
     * This method is invoked after {@code put(…)}, {@code replace(…)} and {@code remove(…)} operations.
     * It does not need to be atomic with above-cited operations because this method performs its work
     * in a background thread anyway. If the value for the specified key was retained by weak reference,
     * it become a value retained by strong reference. Conversely some values previously retained by strong
     * references may be retained by weak references after the cost adjustment performed by this method.
     *
     * @param  key    key of the entry that changed.
     * @param  value  the new value. May be {@code null}.
     */
    private void notifyChange(final K key, final V value) {
        DelayedExecutor.schedule(new Strong(key, value));
    }

    /**
     * If no value is already mapped and no value is under computation for the given key, puts the given value
     * in the cache. Otherwise returns the current value (potentially blocking until the computation finishes).
     * A null {@code value} argument is equivalent to a no-op. Otherwise a {@code null} return value means that
     * the given {@code value} has been stored in the {@code Cache}.
     *
     * @param  key    the key to associate with a value.
     * @param  value  the value to associate with the given key if no value already exists, or {@code null}.
     * @return the existing value mapped to the given key, or {@code null} if none existed before this method call.
     *
     * @see #get(Object)
     * @see #computeIfAbsent(Object, Function)
     *
     * @since 1.0
     */
    @Override
    public V putIfAbsent(final K key, final V value) {
        if (value == null) {
            return null;
        }
        ensureValidType(value);
        final Object previous = map.putIfAbsent(key, value);
        if (previous == null) {
            // A non-null value means that `putIfAbsent` did nothing.
            notifyChange(key, value);
        }
        return valueOf(previous);
    }

    /**
     * Puts the given value in cache and immediately returns the old value.
     * A null {@code value} argument removes the entry. If a different value is under computation in another thread,
     * then the other thread may fail with an {@link IllegalStateException} unless {@link #isKeyCollisionAllowed()}
     * returns {@code true}. For more safety, consider using {@link #putIfAbsent putIfAbsent(…)} instead.
     *
     * @param  key    the key to associate with a value.
     * @param  value  the value to associate with the given key, or {@code null} for removing the mapping.
     * @return the value previously mapped to the given key, or {@code null} if no value existed before this
     *         method call or if the value was under computation in another thread.
     *
     * @see #get(Object)
     * @see #putIfAbsent(Object, Object)
     */
    @Override
    public V put(final K key, final V value) {
        ensureValidType(value);
        final Object previous = (value != null) ? map.put(key, value) : map.remove(key);
        if (previous != value) {
            notifyChange(key, value);
        }
        return immediateValueOf(previous);
    }

    /**
     * If the given key is mapped to any value, replaces that value with the given new value.
     * Otherwise does nothing. A null {@code value} argument removes the entry.
     * If a different value is under computation in another thread, then the other thread may fail with
     * an {@link IllegalStateException} unless {@link #isKeyCollisionAllowed()} returns {@code true}.
     *
     * @param  key    key of the value to replace.
     * @param  value  the new value to use in replacement of the previous one, or {@code null} for removing the mapping.
     * @return the value previously mapped to the given key, or {@code null} if no value existed before this
     *         method call or if the value was under computation in another thread.
     *
     * @see #replace(Object, Object, Object)
     *
     * @since 1.0
     */
    @Override
    public V replace(final K key, final V value) {
        ensureValidType(value);
        final Object previous = (value != null) ? map.replace(key, value) : map.remove(key);
        if (previous != null) {
            // A null value means that `replace` did nothing.
            notifyChange(key, value);
        }
        return immediateValueOf(previous);
    }

    /**
     * If the given key is mapped to the given old value, replaces that value with the given new value.
     * Otherwise does nothing. A null {@code value} argument removes the entry if the condition matches.
     * If a value is under computation in another thread, then this method unconditionally returns {@code false}.
     *
     * @param  key       key of the value to replace.
     * @param  oldValue  previous value expected to be mapped to the given key.
     * @param  newValue  the new value to put if the condition matches, or {@code null} for removing the mapping.
     * @return {@code true} if the value has been replaced, {@code false} otherwise.
     *
     * @since 1.0
     */
    @Override
    public boolean replace(final K key, final V oldValue, final V newValue) {
        ensureValidType(newValue);
        final boolean done;
        if (oldValue != null) {
            done = (newValue != null) ? map.replace(key, oldValue, newValue) : map.remove(key, oldValue);
        } else {
            done = (newValue != null) && map.putIfAbsent(key, newValue) == null;
        }
        if (done) {
            notifyChange(key, newValue);
        }
        return done;
    }

    /**
     * Iterates over all entries in the cache and replaces their value with the one provided by the given function.
     * If the function throws an exception, the iteration is stopped and the exception is propagated. If any value
     * is under computation in other threads, then the iteration will block on that entry until its computation is
     * completed.
     *
     * @param  remapping  the function computing new values from the old ones.
     *
     * @since 1.0
     */
    @Override
    public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> remapping) {
        final var adapter = new ReplaceAdapter(remapping);
        map.replaceAll(adapter);
        Deferred.notifyChanges(this, adapter.changes);
    }

    /**
     * Replaces the value mapped to the given key by a new value computed from the old value.
     * If a value for the given key is under computation in another thread, then this method
     * blocks until that computation is completed. This is equivalent to the work performed
     * by {@link #replaceAll replaceAll(…)} but on a single entry.
     *
     * @param  key        key of the value to replace.
     * @param  remapping  the function computing new values from the old ones.
     * @return the new value associated with the given key.
     *
     * @see #computeIfAbsent(Object, Function)
     *
     * @since 1.0
     */
    @Override
    public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remapping) {
        final var adapter = new ReplaceAdapter(remapping);
        final Object value = map.computeIfPresent(key, adapter);
        Deferred.notifyChanges(this, adapter.changes);
        return valueOf(value);
    }

    /**
     * Replaces the value mapped to the given key by a new value computed from the old value.
     * If there is no value for the given key, then the "old value" is taken as {@code null}.
     * If a value for the given key is under computation in another thread, then this method
     * blocks until that computation is completed. This method is equivalent to
     * {@link #computeIfPresent computeIfPresent(…)} except that a new value will be computed
     * even if no value existed for the key before this method call.
     *
     * @param  key        key of the value to replace.
     * @param  remapping  the function computing new values from the old ones, or from a {@code null} value.
     * @return the new value associated with the given key.
     *
     * @see #computeIfAbsent(Object, Function)
     *
     * @since 1.0
     */
    @Override
    public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remapping) {
        final var adapter = new ReplaceAdapter(remapping);
        final Object value = map.compute(key, adapter);
        Deferred.notifyChanges(this, adapter.changes);
        return valueOf(value);
    }

    /**
     * Maps the given value to the given key if no mapping existed before this method call,
     * or computes a new value otherwise. If a value for the given key is under computation
     * in another thread, then this method blocks until that computation is completed.
     *
     * @param  key        key of the value to replace.
     * @param  value      the value to associate with the given key if no value already exists, or {@code null}.
     * @param  remapping  the function computing a new value by merging the exiting value
     *                    with the {@code value} argument given to this method.
     * @return the new value associated with the given key.
     *
     * @since 1.0
     */
    @Override
    public V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remapping) {
        ensureValidType(value);

        /** Similar to {@link Cache.ReplaceAdapter}, but adapted to the merge case. */
        final class Adapter implements BiFunction<Object, Object, Object> {
            /** Forwards {@link Cache#map} calls to the user-provided function. */
            @Override public Object apply(final Object oldValue, final Object givenValue) {
                final V toReplace = valueOf(oldValue);
                final V newValue = remapping.apply(toReplace, valueOf(givenValue));
                ensureValidType(newValue);
                if (newValue != toReplace) {
                    changes = new Deferred<>(key, newValue, changes);
                }
                return newValue;
            }

            /** The new values for which to send notifications. */
            Deferred<K,V> changes;
        }
        final Adapter adapter = new Adapter();
        final Object newValue = map.merge(key, value, adapter);
        Deferred.notifyChanges(this, adapter.changes);
        return valueOf(newValue);
    }

    /**
     * A callback for {@link Cache#map} which forwards the calls to the {@code remapping} function provided by user.
     * Before to forward the calls, {@code ReplaceAdapter} verifies if the value is under computation. If yes, then
     * this adapter blocks until the value is available for forwarding it to the user.
     */
    private final class ReplaceAdapter implements BiFunction<K, Object, Object> {
        /** The new values for which to send notifications. */
        private Deferred<K,V> changes;

        /** The user-providing function. */
        private final BiFunction<? super K, ? super V, ? extends V> remapping;

        /** Creates a new adapter for the given user-provided function. */
        ReplaceAdapter(final BiFunction<? super K, ? super V, ? extends V> remapping) {
            this.remapping = remapping;
        }

        /** Forwards {@link Cache#map} calls to the user-provided function. */
        @Override public Object apply(final K key, final Object oldValue) {
            final V toReplace = valueOf(oldValue);
            final V newValue = remapping.apply(key, toReplace);
            ensureValidType(newValue);
            if (newValue != toReplace) {
                changes = new Deferred<>(key, newValue, changes);
            }
            return newValue;
        }
    }

    /**
     * Key-value pairs of new entries created during {@link Cache.ReplaceAdapter} execution, as a chained list.
     * Calls to {@link Cache#notifyChange(Object, Object)} for those entries need to be deferred until operation
     * on {@link Cache#map} completed because {@link Cache#adjustReferences(Object, Object)} needs the new values
     * to be present in the map.
     */
    private static final class Deferred<K,V> {
        private final K key;
        private final V value;
        private final Deferred<K,V> next;

        /** Creates a new notification to be sent after the {@link Cache#map} operation completed. */
        Deferred(final K key, final V value, final Deferred<K,V> next) {
            this.key   = key;
            this.value = value;
            this.next  = next;
        }

        /** Sends all deferred notifications, starting with the given one. */
        static <K,V> void notifyChanges(final Cache<K,V> cache, Deferred<K,V> entry) {
            while (entry != null) {
                cache.notifyChange(entry.key, entry.value);
                entry = entry.next;
            }
        }
    }

    /**
     * Removes the value mapped to the given key in the cache. If a value is under computation in another thread,
     * then the other thread may fail with an {@link IllegalStateException} unless {@link #isKeyCollisionAllowed()}
     * returns {@code true}. For more safety, consider using {@link #remove(Object, Object)} instead.
     *
     * @param  key  the key of the value to removed.
     * @return the value previously mapped to the given key, or {@code null} if no value existed before this
     *         method call or if the value was under computation in another thread.
     *
     * @see #get(Object)
     * @see #remove(Object, Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public V remove(final Object key) {
        final Object oldValue = map.remove(key);
        if (oldValue != null) {
            notifyChange((K) key, null);
        }
        return immediateValueOf(oldValue);
    }

    /**
     * If the given key is mapped to the given old value, removes that value. Otherwise does nothing.
     * If a value is under computation in another thread, then this method unconditionally returns {@code false}.
     *
     * @param  key      key of the value to remove.
     * @param  oldValue previous value expected to be mapped to the given key.
     * @return {@code true} if the value has been removed, {@code false} otherwise.
     *
     * @see #get(Object)
     *
     * @since 1.0
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(final Object key, final Object oldValue) {
        final boolean done = map.remove(key, oldValue);
        if (done) {
            notifyChange((K) key, null);
        }
        return done;
    }

    /**
     * Returns {@code true} if this map contains the specified key.
     * If the value is under computation in another thread, this method returns {@code true}
     * without waiting for the computation result. This behavior is consistent with other
     * {@code Map} methods in the following ways:
     *
     * <ul>
     *   <li>{@link #get(Object)} blocks until the computation is completed.</li>
     *   <li>{@link #put(Object, Object)} returns {@code null} for values under computation,
     *       i.e. behaves as if keys are temporarily mapped to the {@code null} value until
     *       the computation is completed.</li>
     * </ul>
     *
     * @param  key  the key to check for existence.
     * @return {@code true} if the given key is mapped to an existing value or a value under computation.
     *
     * @see #get(Object)
     * @see #peek(Object)
     */
    @Override
    public boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    /**
     * If a value is already cached for the given key, returns it. Otherwise returns {@code null}.
     * This method is similar to {@link #get(Object)} except that it doesn't block if the value is
     * in process of being computed in another thread; it returns {@code null} in such case.
     *
     * @param  key  the key for which to get the cached value.
     * @return the cached value for the given key, or {@code null} if there is none.
     *
     * @see #get(Object)
     * @see #lock(Object)
     */
    public V peek(final K key) {
        final Object value = map.get(key);
        if (value instanceof Handler<?>) {
            /*
             * The value is under computation. We will not wait for it since it is
             * not the purpose of this method (we should use lock(key) for that).
             */
            return null;
        }
        if (value instanceof Reference<?>) {
            @SuppressWarnings("unchecked")
            final var ref = (Reference<V>) value;
            final V result = ref.get();
            if (result != null && map.replace(key, ref, result)) {
                ref.clear();                        // Prevents the reference from being enqueued.
                notifyChange(key, result);
            }
            return result;
        }
        @SuppressWarnings("unchecked")
        final V result = (V) value;
        return result;
    }

    /**
     * Invoked from the a background thread after a {@linkplain WeakReference weak}
     * or {@linkplain SoftReference soft} reference has been replaced by a strong one.
     * It will looks for older strong references to replace by weak references so that
     * the total cost stay below the cost limit.
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
            adjustReferences(key, value);
        }
    }

    /**
     * Gets a lock for the entry at the given key and returns a handler to be used by the caller
     * for unlocking and storing the result. This method <strong>must</strong> be used together
     * with a {@link Handler#putAndUnlock(Object) putAndUnlock} call in {@code try} … {@code catch}
     * blocks as in the example below:
     *
     * {@snippet lang="java" :
     *     Cache.Handler handler = cache.lock();
     *     try {
     *         // Compute the result...
     *     } finally {
     *         handler.putAndUnlock(result);
     *     }
     *     }
     *
     * @param  key  the key for the entry to lock.
     * @return a handler to use for unlocking and storing the result.
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
                     * map.put(…) or map.replace(…) operations are guaranteed to put non-null values).
                     * We are done. But before to leave, declare that we do not want to unlock in the
                     * `finally` clause (we want the lock to still active).
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
                final var ref = (Reference<V>) value;
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
                        notifyChange(key, result);
                    }
                    return new Simple<>(result);
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
                    return new Simple<>(null);
                }
                throw new IllegalStateException(Errors.format(Errors.Keys.RecursiveCreateCallForKey_1, key));
            }
            return work.new Wait();
        }
        /*
         * A computation has already been completed. Returns a wrapper
         * which will just return the result without any processing.
         */
        assert !isReservedType(value) : value;
        @SuppressWarnings("unchecked")
        final V result = (V) value;
        return new Simple<>(result);
    }

    /**
     * The handler returned by {@link Cache#lock}, to be used for unlocking and storing the result.
     * This handler should be used as below (the {@code try} … {@code finally} statements are important):
     *
     * {@snippet lang="java" :
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
     *     }
     *
     * See the {@link Cache} javadoc for a more complete example.
     *
     * @param  <V>  the type of value objects.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 0.3
     * @since   0.3
     */
    public interface Handler<V> {
        /**
         * If the value is already in the cache, returns it. Otherwise returns {@code null}.
         * This method should be invoked after the {@code Handler} creation in case a value
         * has been computed in another thread.
         *
         * @return the value from the cache, or {@code null} if none.
         */
        V peek();

        /**
         * Stores the given value in the cache and release the lock. This method
         * <strong>must</strong> be invoked in a {@code finally} block, no matter
         * what the result is.
         *
         * @param result  the result to store in the cache, or {@code null} for removing
         *        the entry from the cache. If an entry is removed, a new computation
         *        will be attempted the next time a handler is created for the same key.
         *
         * @throws IllegalStateException may be thrown if this method is not invoked in
         *         the pattern described in class javadoc, or if a key collision occurs.
         */
        void putAndUnlock(V result) throws IllegalStateException;
    }

    /**
     * A simple handler implementation wrapping an existing value. This implementation
     * is used when the value has been fully computed in another thread before this
     * thread could start its work.
     */
    private final class Simple<V> implements Handler<V> {
        /**
         * The result computed in another thread.
         */
        private final V value;

        /**
         * Creates a new handler wrapping the given value.
         */
        Simple(final V value) {
            this.value = value;
        }

        /**
         * Returns the computed value.
         */
        @Override
        public V peek() {
            return value;
        }

        /**
         * Do nothing (except checking for programming error), since we don't hold any lock.
         *
         * <h4>Implementation note</h4>
         * An alternative would have been to store the result in the map anyway.
         * But doing so is unsafe because we have no lock; we have no guarantee that nothing
         * has happened in another thread between {@code peek} and {@code putAndUnlock}.
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
         * Waits for the completion of the value computation and returns this result.
         * This method should be invoked only from another thread than the one doing the computation.
         */
        @Override
        @SuppressWarnings("LockAcquiredButNotSafelyReleased")
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
         * @throws IllegalStateException if the current thread does not hold the lock.
         */
        @Override
        public void putAndUnlock(final V result) throws IllegalStateException {
            final boolean done;
            try {
                if (isReservedType(result)) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.IllegalArgumentClass_2, "result", result.getClass()));
                }
                // Assignation of `value` must happen before we release the lock.
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
            } else if (!isKeyCollisionAllowed()) {
                throw new IllegalStateException(Errors.format(Errors.Keys.KeyCollision_1, key));
            }
        }

        /**
         * A handler implementation used when the value is in process of being computed in another thread.
         * At the difference of the {@code Simple} handler, the computation is not yet completed, so this
         * handler has to wait.
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
             * <h4>Implementation note</h4>
             * An alternative would have been to store the result in the map anyway.
             * But doing so is unsafe because we have no lock; we have no guarantee that nothing
             * has happened in another thread between {@code peek} and {@code putAndUnlock}.
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
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final V value = this.value;
            if (value != null) {
                adjustReferences(key, value);
            }
        }
    }

    /**
     * Invoked in a background thread after a value has been set in the map.
     * This method computes a cost estimation of the new value. If the total cost is greater
     * than the cost limit, then oldest strong references are replaced by weak references
     * until the cost of entries kept by strong references become lower than the threshold.
     */
    private void adjustReferences(final K key, final V value) {
        int cost = (value != null) ? cost(value) : 0;
        synchronized (costs) {
            final Integer old = (cost > 0) ? costs.put(key, cost) : costs.remove(key);
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
                     * That entry should not be garbage collected too early anyway because the
                     * caller should still have a strong reference to the value he just created.
                     */
                    final Map.Entry<K,Integer> entry = it.next();
                    final K oldKey = entry.getKey();
                    final Object oldValue = map.get(oldKey);
                    if (oldValue != null && !isReservedType(oldValue)) {
                        @SuppressWarnings("unchecked")
                        final Reference<V> ref = soft ? new Soft(oldKey, (V) oldValue)
                                                      : new Weak(oldKey, (V) oldValue);
                        if (!map.replace(oldKey, oldValue, ref)) {
                            ref.clear();                // Prevents the reference to be enqueued.
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
     * A soft reference which removes itself from the cache when the reference is garbage-collected.
     */
    private final class Soft extends SoftReference<V> implements Disposable {
        /** The key of the referenced value. */
        private final K key;

        /** Creates a references to be stored in the cache under the given key. */
        Soft(final K key, final V value) {
            super(value, ReferenceQueueConsumer.QUEUE);
            this.key = key;
        }

        /** Removes the reference from the map. */
        @Override public void dispose() {
            removeKey(key, this);
        }
    }

    /**
     * A weak reference which removes itself from the cache when the reference is garbage-collected.
     */
    private final class Weak extends WeakReference<V> implements Disposable {
        /** The key of the referenced value. */
        private final K key;

        /** Creates a references to be stored in the cache under the given key. */
        Weak(final K key, final V value) {
            super(value, ReferenceQueueConsumer.QUEUE);
            this.key = key;
        }

        /** Removes the reference from the map. */
        @Override public void dispose() {
            removeKey(key, this);
        }
    }

    /**
     * Removes the given key from the map if it is associated to the given value, otherwise do nothing.
     * This method is invoked when the value of a weak or soft reference has been cleared.
     * Theoretically no entry for that key should exist in the {@link #costs} map because
     * that map contains only the keys of objects hold by strong references.
     * But we check anyway for reducing the risk of memory leaks.
     * It may happen if some keys are removed from {@link #keySet()} instead of using {@code Cache} API.
     *
     * @param key    key of the entry to remove.
     * @param value  expected value associated to the given entry.
     */
    private void removeKey(final K key, final Reference<V> value) {
        if (map.remove(key, value)) {
            synchronized (costs) {
                final Integer cost = costs.remove(key);
                if (cost != null) totalCost -= cost;
            }
        }
    }

    /**
     * Returns the set of keys in this cache. The returned set is subjects to the same caution
     * than the ones documented in the {@link ConcurrentHashMap#keySet()} method.
     *
     * <p>If some elements are removed from the key set, then {@link #flush()} should be invoked after removals.
     * This is not done automatically by the returned set. For safety, the {@link #remove(Object)} methods defined
     * in the {@code Cache} class should be used instead.</p>
     *
     * @return the set of keys in this cache.
     *
     * @see #flush()
     */
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * Returns the set of entries in this cache. The returned set is subjects to the same caution
     * than the ones documented in the {@link ConcurrentHashMap#entrySet()} method, except that
     * it does not support removal of elements (including through the {@link Iterator#remove}
     * method call).
     *
     * @return a view of the entries contained in this map.
     */
    @Override
    public Set<Entry<K,V>> entrySet() {
        final Set<Entry<K,V>> es = entries;
        return (es != null) ? es : (entries = new CacheEntries<>(map.entrySet()));
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
     * error, so the default {@code Cache} behavior is to throw an {@link IllegalStateException}
     * in such cases, typically when {@link Handler#putAndUnlock(Object)} is invoked. However, in
     * some cases we may want to relax this check. For example, the EPSG database sometimes assigns
     * the same key to different kinds of objects.
     *
     * <p>If key collisions are allowed and two threads invoke {@link #lock(Object)} concurrently
     * for the same key, then the value to be stored in the map will be the one computed by the
     * first thread who got the lock. The value computed by any other concurrent thread will be
     * ignored by this {@code Cache} class. However, those threads still return their computed
     * values to their callers.</p>
     *
     * <p>This property can also be set in order to allow some recursion. If during the creation
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
     * @param  value  the object for which to get an estimation of its cost.
     * @return the estimated cost of the given object.
     */
    protected int cost(final V value) {
        return 1;
    }

    /**
     * Forces the removal of all garbage collected values in the map.
     * This method should not need to be invoked when using {@code Cache} API.
     * It is provided as a debugging tools when suspecting a memory leak.
     *
     * @return {@code true} if some entries have been removed as a result of this method call.
     *
     * @see #keySet()
     *
     * @since 1.3
     */
    public boolean flush() {
        boolean changed = map.values().removeIf(Cache::isNull);
        synchronized (costs) {
            changed |= costs.keySet().retainAll(map.keySet());
        }
        return changed;
    }
}
