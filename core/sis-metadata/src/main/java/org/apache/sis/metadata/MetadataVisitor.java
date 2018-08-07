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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.ConcurrentModificationException;
import org.apache.sis.internal.system.Semaphores;
import org.apache.sis.internal.util.UnmodifiableArrayList;


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
     * The name of the property being visited as the last element of the queue. If {@code visit} method
     * is invoked recursively, then the properties before the last one are the parent properties.
     * The number of valid elements is {@link #nestedCount}.
     */
    private String[] propertyPath;

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
    MetadataVisitor() {
        visited = new IdentityHashMap<>();
        propertyPath = new String[6];
    }

    /**
     * The thread-local variable that created this {@code MetadataVisitor} instance.
     * This is usually a static final {@code VISITORS} constant defined in the subclass.
     * May be {@code null} if this visitor does not use thread-local instances.
     */
    ThreadLocal<? extends MetadataVisitor<?>> creator() {
        return null;
    }

    /**
     * Sets the name of the method being visited. This is invoked by {@code PropertyAccessor.walk} methods only.
     */
    final void setCurrentProperty(final String name) {
        propertyPath[nestedCount - 1] = name;
    }

    /**
     * Returns the path to the currently visited property.
     * Each element in the list is the UML identifier of a property.
     * Element at index 0 is the name of the property of the root metadata object being visited.
     * Element at index 1 is the name of a property which is a children of above property, <i>etc.</i>
     *
     * <p>The returned list is valid only during {@link #visit(Class, Object)} method execution.
     * The content of this list become undetermined after the {@code visit} method returned.</p>
     *
     * @return the path to the currently visited property.
     */
    List<String> getCurrentPropertyPath() {
        return UnmodifiableArrayList.wrap(propertyPath, 0, nestedCount);
    }

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
                final Filter filter = preVisit(accessor);
                final boolean preconstructed;
                final R sentinel;
                switch (filter) {
                    case NONE:            return null;
                    case WRITABLE_RESULT: preconstructed = true;  sentinel = result(); break;
                    default:              preconstructed = false; sentinel = null;     break;
                }
                if (visited.put(metadata, sentinel) != null) {
                    // Should never happen, unless this method is invoked concurrently in another thread.
                    throw new ConcurrentModificationException();
                }
                if (nestedCount >= propertyPath.length) {
                    propertyPath = Arrays.copyOf(propertyPath, nestedCount * 2);
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
                    switch (filter) {
                        case NON_EMPTY:       accessor.walkReadable(this, metadata); break;
                        case WRITABLE:        accessor.walkWritable(this, metadata, metadata); break;
                        case WRITABLE_RESULT: accessor.walkWritable(this, metadata, sentinel); break;
                    }
                } catch (MetadataVisitorException e) {
                    throw e;
                } catch (Exception e) {
                    throw new MetadataVisitorException(Arrays.copyOf(propertyPath, nestedCount), accessor.type, e);
                } finally {
                    if (--nestedCount == 0) {
                        if (!allowNull) {
                            Semaphores.clear(Semaphores.NULL_COLLECTION);
                        }
                        final ThreadLocal<? extends MetadataVisitor<?>> creator = creator();
                        if (creator != null) creator.remove();
                    }
                }
                final R result = preconstructed ? sentinel : result();
                if (visited.put(metadata, result) != sentinel) {
                    throw new ConcurrentModificationException();
                }
                return result;
            }
        }
        return visited.get(metadata);
    }

    /**
     * Filter the properties to visit. A value of this enumeration is returned by {@link #preVisit(PropertyAccessor)}
     * before the properties of a metadata instance are visited.
     */
    enum Filter {
        /**
         * Do not visit any property (skip completely the metadata).
         */
        NONE,

        /**
         * Visit all non-null and non-empty standard properties.
         */
        NON_EMPTY,

        /**
         * Visit all writable properties. May include some non-standard properties.
         */
        WRITABLE,

        /**
         * Same as {@link #WRITABLE}, but write properties in the object returned by {@link #result()}.
         * This mode implies that {@code result()} is invoked <strong>before</strong> metadata properties
         * are visited instead than after.
         */
        WRITABLE_RESULT
    }

    /**
     * Invoked when a new metadata is about to be visited. After this method has been invoked,
     * {@link #visit(Class, Object)} will be invoked for each property in the metadata object.
     *
     * @param  accessor  information about the standard interface and implementation of the metadata being visited.
     * @return most common values are {@code NON_EMPTY} for visiting all non-empty properties (the default),
     *         or {@code WRITABLE} for visiting only writable properties.
     */
    Filter preVisit(PropertyAccessor accessor) {
        return Filter.NON_EMPTY;
    }

    /**
     * Invoked when a new metadata property is being visited. The current property value is given in argument.
     * The return value is interpreted as below:
     *
     * <ul>
     *   <li>{@link #SKIP_SIBLINGS}: do not iterate over other properties of current metadata,
     *       but continue iteration over properties of the parent metadata.</li>
     *   <li>{@code value}: continue with next sibling property without setting any value.</li>
     *   <li>{@code null}: clear the property value, then continue with next sibling property.
     *       If the property type is a collection, then "null" value is interpreted as an instruction
     *       to {@linkplain java.util.Collection#clear() clear} the collection.</li>
     *   <li>Any other value: set the property value to the given value,
     *       then continue with next sibling property.</li>
     * </ul>
     *
     * @param  type   the type of elements. Note that this is not necessarily the type
     *                of given {@code value} argument if the later is a collection.
     * @param  value  value of the metadata property being visited.
     * @return the new property value to set, or {@link #SKIP_SIBLINGS}.
     * @throws Exception if the visit operation failed.
     */
    abstract Object visit(Class<?> type, Object value) throws Exception;

    /**
     * Returns the result of visiting all elements in a metadata instance.
     * This method is invoked zero or one time per metadata instance.
     * The invocation time depends on the value returned by {@link #preVisit(PropertyAccessor)}:
     *
     * <ul>
     *   <li>If {@link Filter#NONE}, then this method is never invoked for the current metadata instance.</li>
     *   <li>If {@link Filter#NON_EMPTY} or {@link Filter#WRITABLE}, then this method is invoked after all properties
     *       have been visited or after {@link #visit(Class, Object)} returned {@link #SKIP_SIBLINGS}.</li>
     *   <li>If {@link Filter#WRITABLE_RESULT}, then this method is invoked <strong>before</strong> metadata
     *       properties are visited. In such case, this method should return an initially empty instance.</li>
     * </ul>
     *
     * The value returned by this method will be cached in case the same metadata instance is revisited again.
     * This value can be {@code null}.
     */
    R result() {
        return null;
    }
}
