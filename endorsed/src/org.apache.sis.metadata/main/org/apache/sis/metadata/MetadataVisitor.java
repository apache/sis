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
import org.apache.sis.system.Semaphores;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.collection.Containers;


/**
 * A visitor of metadata properties with a safety against infinite recursion.
 * The visitor may compute a result, for example a hash code value or a Boolean
 * testing whether the metadata is empty. Each {@code MetadataVisitor} instance
 * is used by one thread, so this class does not need to be thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of result of walking in the metadata.
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
     * A guard against infinite recursion in {@link #walk(MetadataStandard, Class, Object, boolean)}.
     * Keys are visited metadata instances and values are computed value.
     * The value may be null if the computation is in progress.
     *
     * <h4>The problem</h4>
     * Cyclic associations can exist in ISO 19115 metadata. For example, {@code Instrument} has a reference
     * to the platform it is mounted on, and the {@code Platform} has a list of instruments mounted on it.
     * Consequently, walking down the metadata tree can cause infinite recursion, unless we keep trace of
     * previously visited metadata objects in order to avoid visiting them again.
     *
     * We use an {@link IdentityHashMap} for that purpose, since the recursion problem exists only when revisiting
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
     * When this count reaches zero, the visitor should be removed from the thread local variable.
     *
     * @see #creator()
     */
    private int nestedCount;

    /**
     * Whether to clear the {@link Semaphores#NULL_FOR_EMPTY_COLLECTION} flag after the walk of whole tree.
     * The {@code NULL_FOR_EMPTY_COLLECTION} flag prevents creation of empty collections by getter methods
     * (a consequence of lazy instantiation). The intent is to avoid creation of unnecessary objects
     * for all unused properties. Users should not see behavioral difference, except if they override
     * some getters with an implementation invoking other getters.
     */
    private boolean needFlagReset;

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
     *
     * <p>If this method returns a non-null value, then {@link ThreadLocal#remove()} will be invoked
     * after {@link MetadataVisitor} finished to walk through a metadata and all its children.</p>
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
        return Containers.viewAsUnmodifiableList(propertyPath, 0, nestedCount);
    }

    /**
     * Invokes {@link #visit(Class, Object)} for all elements of the given metadata if that metadata has not
     * already been visited. The computation result is returned (may be the result of a previous computation).
     *
     * <p>This method is typically invoked recursively while we iterate down the metadata tree.
     * It maintains a map of visited nodes for preventing the same node to be visited twice.</p>
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
                final Filter filter = preVisit(accessor);       // NONE, NON_EMPTY, WRITABLE or WRITABLE_RESULT.
                final boolean preconstructed;                   // Whether to write in instance provided by `result()`.
                final R sentinel;                               // Value in the map for telling that visit started.
                switch (filter) {
                    case NONE:            return null;
                    case WRITABLE_RESULT: preconstructed = true;  sentinel = result(); break;
                    default:              preconstructed = false; sentinel = null;     break;
                }
                if (visited.put(metadata, sentinel) != null) {
                    // Should never happen, unless this method is invoked concurrently in another thread.
                    throw new ConcurrentModificationException();
                }
                /*
                 * Name of properties walked from root node to the node being visited by current method invocation.
                 * The path is provided by `propertyPath` and the number of valid elements is given by `nestedCount`,
                 * which is 1 during the visit of first element.
                 */
                if (nestedCount >= propertyPath.length) {
                    propertyPath = Arrays.copyOf(propertyPath, nestedCount * 2);
                }
                if (nestedCount++ == 0) {
                    needFlagReset = Semaphores.NULL_FOR_EMPTY_COLLECTION.set();
                }
                /*
                 * Actual visiting. The `accessor.walk(this, metadata)` method calls below will callback the abstract
                 * `visit(type, value)` method declared in this class.
                 */
                try {
                    switch (filter) {
                        case NON_EMPTY:       accessor.walkReadable(this, metadata); break;
                        case WRITABLE:        accessor.walkWritable(this, metadata, metadata); break;
                        case WRITABLE_RESULT: accessor.walkWritable(this, metadata, sentinel); break;
                    }
                } catch (MetadataVisitorException e) {
                    throw e;
                } catch (Exception e) {
                    e = Exceptions.unwrap(e);
                    throw new MetadataVisitorException(Arrays.copyOf(propertyPath, nestedCount), accessor.type, e);
                } finally {
                    if (--nestedCount == 0) {
                        /*
                         * We are back to the root metadata (i.e. we finished walking through all children).
                         * Restore thread local variables to their initial state.
                         */
                        Semaphores.NULL_FOR_EMPTY_COLLECTION.clearIfTrue(needFlagReset);
                        final ThreadLocal<? extends MetadataVisitor<?>> creator = creator();
                        if (creator != null) creator.remove();
                    }
                }
                /*
                 * Cache the result in case this node is visited again (e.g. if the metadata graph contains
                 * cycles or if the same child node is referenced from many places).
                 */
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
         * are visited instead of after.
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
     *                of given {@code value} argument if the latter is a collection.
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
