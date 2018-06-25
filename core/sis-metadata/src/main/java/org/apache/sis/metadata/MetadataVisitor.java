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
package org.apache.sis.metadata;

import java.util.Map;
import java.util.IdentityHashMap;
import java.util.ConcurrentModificationException;
import org.apache.sis.internal.system.Semaphores;


/**
 * A visitor of metadata properties with a safety against infinite recursivity.
 * The visitor may compute a result, for example a hash code value or a boolean
 * testing whether the metadata is empty. Each {@code MetadataVisitor} instance
 * is used by one thread; this class does not need to be thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @param  <R>  the type of result of walking in the metadata.
 *
 * @since 1.0
 * @module
 */
abstract class MetadataVisitor<R> {
    /**
     * Sentinel value that may be returned by {@link #visit(Class, Object)} for meaning that a property value
     * should be set to {@code null}. If the property type is a collection, then "null" value is interpreted
     * as an instruction to {@linkplain java.util.Collection#clear() clear} the collection.
     *
     * <div class="note"><b>Note:</b> a sentinel value is required because {@code visit(…)} already uses
     * the {@code null} return value for meaning that the property value shall not be modified.</div>
     */
    static final Object CLEAR = Void.TYPE;                              // The choice of this type is arbitrary.

    /**
     * Sentinel value that may be returned by {@link #visit(Class, Object)} for notifying the walker to stop.
     * This value causes {@link #walk walk(…)} to stop its iteration, but does not stop iteration by the parent
     * if {@code walk(…)} has been invoked recursively. The {@link #result()} method shall return a valid result
     * even if the iteration has been terminated.
     */
    static final Object SKIP_SIBLINGS = InterruptedException.class;     // The choice of this type is arbitrary.

    /**
     * A guard against infinite recursivity in {@link #walk(MetadataStandard, Class, Object, boolean)}.
     * Keys are visited metadata instances and values are computed value. The value may be null if
     * the computation is in progress.
     *
     * <div class="section">The problem</div>
     * Cyclic associations can exist in ISO 19115 metadata. For example {@code Instrument} has a reference
     * to the platform it is mounted on, and the {@code Platform} has a list of instruments mounted on it.
     * Consequently walking down the metadata tree can cause infinite recursivity, unless we keep trace of
     * previously visited metadata objects in order to avoid visiting them again.
     *
     * We use an {@link IdentityHashMap} for that purpose, since the recursivity problem exists only when revisiting
     * the exact same instance. Furthermore, {@code HashMap} would not suit since it invokes {@code equals(Object)}
     * and {@code hashCode()}, which are among the methods that we want to avoid invoking twice.
     */
    private final Map<Object,R> visited;

    /**
     * Count of nested calls to {@link #walk(MetadataStandard, Class, Object, boolean)} method.
     * When this count reach zero, the visitor should be removed from the thread local variable.
     */
    private int nestedCount;

    /**
     * Value of the {@link Semaphores#NULL_COLLECTION} flag when we started the walk.
     * The {@code NULL_COLLECTION} flag prevents creation of new empty collections by getter methods
     * (a consequence of lazy instantiation). The intent is to avoid creation of unnecessary objects
     * for all unused properties. Users should not see behavioral difference, except if they override
     * some getters with an implementation invoking other getters. However in such cases, users would
     * have been exposed to null values at XML marshalling time anyway.
     */
    private boolean allowNull;

    /**
     * Creates a new visitor.
     */
    protected MetadataVisitor() {
        visited = new IdentityHashMap<>();
    }

    /**
     * The thread-local variable that created this {@code MetadataVisitor} instance.
     * This is usually a static final {@code VISITORS} constant defined in the subclass.
     */
    abstract ThreadLocal<? extends MetadataVisitor<?>> creator();

    /**
     * Invokes {@link #visit(Class, Object)} for all elements of the given metadata if that metadata has not
     * already been visited. The computation result is returned (may be the result of a previous computation).
     *
     * <p>This method is typically invoked recursively while we iterate down the metadata tree.
     * It creates a map of visited nodes when the iteration begin, and deletes that map when the
     * iteration ends.</p>
     *
     * @param  standard   the metadata standard implemented by the object to visit.
     * @param  type       the standard interface implemented by the metadata object, or {@code null} if unknown.
     * @param  metadata   the metadata to visit.
     * @param  mandatory  {@code true} for throwing an exception for unsupported metadata type, or {@code false} for ignoring.
     * @return the value of {@link #result()} after all elements of the given metadata have been visited.
     *         If the given metadata instance has already been visited, then this is the previously computed value.
     *         If the computation is in progress (e.g. a cyclic graph), then this method returns {@code null}.
     */
    final R walk(final MetadataStandard standard, final Class<?> type, final Object metadata, final boolean mandatory) {
        if (!visited.containsKey(metadata)) {               // Reminder: the associated value may be null.
            final PropertyAccessor accessor = standard.getAccessor(new CacheKey(metadata.getClass(), type), mandatory);
            if (accessor != null) {
                preVisit(accessor.type);
                if (visited.put(metadata, null) != null) {
                    // Should never happen, unless this method is invoked concurrently in another thread.
                    throw new ConcurrentModificationException();
                }
                if (nestedCount++ == 0) {
                    /*
                     * The NULL_COLLECTION semaphore prevents creation of new empty collections by getter methods
                     * (a consequence of lazy instantiation). The intent is to avoid creation of unnecessary objects
                     * for all unused properties. Users should not see behavioral difference, except if they override
                     * some getters with an implementation invoking other getters. However in such cases, users would
                     * have been exposed to null values at XML marshalling time anyway.
                     */
                    allowNull = Semaphores.queryAndSet(Semaphores.NULL_COLLECTION);
                }
                try {
                    accessor.walk(this, metadata);
                } finally {
                    if (--nestedCount == 0) {
                        if (!allowNull) {
                            Semaphores.clear(Semaphores.NULL_COLLECTION);
                        }
                        creator().remove();
                    }
                }
                final R result = result();
                if (visited.put(metadata, result) != null) {
                    throw new ConcurrentModificationException();
                }
                return result;
            }
        }
        return visited.get(metadata);
    }

    /**
     * Invoked when a new metadata is about to be visited. After this method has been invoked,
     * {@link #visit(Class, Object)} will be invoked for each property in the metadata object.
     *
     * @param  type  the standard interface implemented by the metadata instance being visited.
     */
    void preVisit(Class<?> type) {
    }

    /**
     * Invoked when a new metadata property is being visited. The current property value is given in argument.
     * The return value is interpreted as below:
     *
     * <ul>
     *   <li>{@link #SKIP_SIBLINGS}: do not iterate over other elements of current metadata,
     *       but continue iteration over elements of the parent metadata.</li>
     *   <li>{@link #CLEAR}: clear the property value (e.g. by setting it to {@code null}),
     *       then continue with next sibling property.</li>
     *   <li>Any other non-null value: set the property value to the given value,
     *       then continue with next sibling property.</li>
     *   <li>{@code null}: continue with next sibling property without setting any value.</li>
     * </ul>
     *
     * @param  type   the type of elements. Note that this is not necessarily the type
     *                of given {@code value} argument if the later is a collection.
     * @param  value  value of the metadata property being visited.
     * @return one of the sentinel values ({@link #CLEAR} or {@link #SKIP_SIBLINGS}),
     *         or the new property value to set, or {@code null} for leaving the property value unchanged.
     */
    abstract Object visit(Class<?> type, Object value);

    /**
     * Returns the result of visiting all elements in a metadata instance.
     * This method is invoked after all metadata properties have been visited,
     * or after a {@link #visit(Class, Object)} method call returned {@link #SKIP_SIBLINGS}.
     * The value returned by this method will be cached in case the same metadata instance is revisited again.
     */
    abstract R result();
}
