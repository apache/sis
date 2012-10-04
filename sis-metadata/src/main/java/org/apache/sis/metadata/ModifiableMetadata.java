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

import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;

import org.opengis.util.CodeList;


/**
 * Base class for metadata that may (or may not) be modifiable. Implementations will typically
 * provide {@code set*(...)} methods for each corresponding {@code get*()} method. An initially
 * modifiable metadata may become unmodifiable at a later stage (typically after its construction
 * is completed) by the call to the {@link #freeze()} method.
 *
 * {@section Guidline for implementors}
 * Subclasses should follow the pattern below for every {@code get} and {@code set} methods,
 * with a different processing for singleton value or for {@linkplain Collection collections}.
 * <p>
 * For singleton value:
 *
 * {@preformat java
 *     private Foo property;
 *
 *     public synchronized Foo getProperty() {
 *         return property;
 *     }
 *
 *     public synchronized void setProperty(Foo newValue) {
 *         checkWritePermission();
 *         property = newValue;
 *     }
 * }
 *
 * For collections (note that the call to {@link #checkWritePermission()} is implicit):
 *
 * {@preformat java
 *     private Collection<Foo> properties;
 *
 *     public synchronized Collection<Foo> getProperties() {
 *         return properties = nonNullCollection(properties, Foo.class);
 *     }
 *
 *     public synchronized void setProperties(Collection<Foo> newValues) {
 *         properties = copyCollection(newValues, properties, Foo.class);
 *     }
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
public abstract class ModifiableMetadata {
    /**
     * Constructs an initially empty metadata.
     */
    protected ModifiableMetadata() {
        super();
    }

    /**
     * Returns {@code true} if this metadata is modifiable. This method returns
     * {@code false} if {@link #freeze()} has been invoked on this object.
     *
     * @return {@code true} if this metadata is modifiable.
     *
     * @see #freeze()
     * @see #checkWritePermission()
     */
    public final boolean isModifiable() {
        return true; // To be implemented later.
    }

    /**
     * Checks if changes in the metadata are allowed. All {@code setFoo(...)} methods in
     * subclasses should invoke this method (directly or indirectly) before to apply any
     * change.
     *
     * @throws UnmodifiableMetadataException if this metadata is unmodifiable.
     *
     * @see #isModifiable()
     * @see #freeze()
     */
    protected void checkWritePermission() throws UnmodifiableMetadataException {
        assert Thread.holdsLock(this);
        if (!isModifiable()) {
            throw new UnmodifiableMetadataException("Unmodifiable metadata"); // TODO: localize
        }
    }

    /**
     * Copies the content of one list ({@code source}) into an other ({@code target}).
     * This method performs the following steps:
     * <p>
     * <ul>
     *   <li>Invokes {@link #checkWritePermission()} in order to ensure that this metadata is
     *       modifiable.</li>
     *   <li>If {@code source} is {@linkplain XCollections#isNullOrEmpty(Collection) null or
     *       empty}, returns {@code null} (meaning that the metadata is not provided).</li>
     *   <li>If {@code target} is null, creates a new {@link List}.</li>
     *   <li>Copies the content of the given {@code source} into the target.</li>
     * </ul>
     *
     * @param  <E>         The type of elements in the list.
     * @param  source      The source list, or {@code null}.
     * @param  target      The target list, or {@code null} if not yet created.
     * @param  elementType The base type of elements to put in the list.
     * @return A list (possibly the {@code target} instance) containing the {@code source}
     *         elements, or {@code null} if the source was null.
     * @throws UnmodifiableMetadataException if this metadata is unmodifiable.
     *
     * @see #nonNullList(List, Class)
     */
    @SuppressWarnings("unchecked")
    protected final <E> List<E> copyList(final Collection<? extends E> source,
            List<E> target, final Class<E> elementType)
            throws UnmodifiableMetadataException
    {
        // See the comments in copyCollection(...) for implementation notes.
        if (source != target) {
            checkWritePermission();
            if (source == null) {
                target = null;
            } else {
                if (target != null) {
                    target.clear();
                } else {
                    target = new ArrayList<E>(source.size());
                }
                target.addAll(source);
            }
        }
        return target;
    }

    /**
     * Copies the content of one Set ({@code source}) into an other ({@code target}).
     * This method performs the following steps:
     * <p>
     * <ul>
     *   <li>Invokes {@link #checkWritePermission()} in order to ensure that this metadata is
     *       modifiable.</li>
     *   <li>If {@code source} is {@linkplain XCollections#isNullOrEmpty(Collection) null or
     *       empty}, returns {@code null} (meaning that the metadata is not provided).</li>
     *   <li>If {@code target} is null, creates a new {@link Set}.</li>
     *   <li>Copies the content of the given {@code source} into the target.</li>
     * </ul>
     *
     * @param  <E>         The type of elements in the set.
     * @param  source      The source set, or {@code null}.
     * @param  target      The target set, or {@code null} if not yet created.
     * @param  elementType The base type of elements to put in the set.
     * @return A set (possibly the {@code target} instance) containing the {@code source}
     *         elements, or {@code null} if the source was null.
     * @throws UnmodifiableMetadataException if this metadata is unmodifiable.
     *
     * @see #nonNullSet(Set, Class)
     */
    @SuppressWarnings("unchecked")
    protected final <E> Set<E> copySet(final Collection<? extends E> source,
            Set<E> target, final Class<E> elementType)
            throws UnmodifiableMetadataException
    {
        // See the comments in copyCollection(...) for implementation notes.
        if (source != target) {
            checkWritePermission();
            if (source == null) {
                target = null;
            } else {
                if (target != null) {
                    target.clear();
                } else {
                    target = new LinkedHashSet<E>(source.size());
                }
                target.addAll(source);
            }
        }
        return target;
    }

    /**
     * Copies the content of one collection ({@code source}) into an other ({@code target}).
     * This method performs the following steps:
     * <p>
     * <ul>
     *   <li>Invokes {@link #checkWritePermission()} in order to ensure that this metadata is
     *       modifiable.</li>
     *   <li>If {@code source} is {@linkplain XCollections#isNullOrEmpty(Collection) null or
     *       empty}, returns {@code null} (meaning that the metadata is not provided).</li>
     *   <li>If {@code target} is null, creates a new {@link Set} or a new {@link List}
     *       depending on the value returned by {@link #collectionType(Class)}.</li>
     *   <li>Copies the content of the given {@code source} into the target.</li>
     * </ul>
     *
     * {@section Choosing a collection type}
     * Implementations shall invoke {@link #copyList copyList} or {@link #copySet copySet} methods
     * instead than this method when the collection type is enforced by ISO specification.
     * When the type is not enforced by the specification, some freedom are allowed at
     * implementor choice. The default implementation invokes {@link #collectionType(Class)}
     * in order to get a hint about whether a {@link List} or a {@link Set} should be used.
     *
     * @param  <E>         The type of elements in the collection.
     * @param  source      The source collection, or {@code null}.
     * @param  target      The target collection, or {@code null} if not yet created.
     * @param  elementType The base type of elements to put in the collection.
     * @return A collection (possibly the {@code target} instance) containing the {@code source}
     *         elements, or {@code null} if the source was null.
     * @throws UnmodifiableMetadataException if this metadata is unmodifiable.
     */
    @SuppressWarnings("unchecked")
    protected final <E> Collection<E> copyCollection(final Collection<? extends E> source,
            Collection<E> target, final Class<E> elementType)
            throws UnmodifiableMetadataException
    {
        /*
         * It is not worth to copy the content if the current and the new instance are the
         * same. This is safe only using the != operator, not the !equals(Object) method.
         * This optimization is required for efficient working of PropertyAccessor.set(...).
         */
        if (source != target) {
            checkWritePermission();
            if (source == null) {
                target = null;
            } else {
                if (target != null) {
                    target.clear();
                } else {
                    final int capacity = source.size();
                    if (useSet(elementType)) {
                        target = new LinkedHashSet<E>(capacity);
                    } else {
                        target = new ArrayList<E>(capacity);
                    }
                }
                target.addAll(source);
            }
        }
        return target;
    }

    /**
     * Returns the specified list, or a new one if {@code c} is null.
     * This is a convenience method for implementation of {@code getFoo()}
     * methods.
     *
     * @param  <E> The type of elements in the list.
     * @param  c The list to checks.
     * @param  elementType The element type (used only if {@code c} is null).
     * @return {@code c}, or a new list if {@code c} is null.
     */
    // See the comments in nonNullCollection(...) for implementation notes.
    protected final <E> List<E> nonNullList(final List<E> c, final Class<E> elementType) {
        assert Thread.holdsLock(this);
        if (c != null) {
            return c;
        }
        if (isModifiable()) {
            return new ArrayList<E>();
        }
        return Collections.emptyList();
    }

    /**
     * Returns the specified set, or a new one if {@code c} is null.
     * This is a convenience method for implementation of {@code getFoo()}
     * methods.
     *
     * @param  <E> The type of elements in the set.
     * @param  c The set to checks.
     * @param  elementType The element type (used only if {@code c} is null).
     * @return {@code c}, or a new set if {@code c} is null.
     */
    // See the comments in nonNullCollection(...) for implementation notes.
    protected final <E> Set<E> nonNullSet(final Set<E> c, final Class<E> elementType) {
        assert Thread.holdsLock(this);
        if (c != null) {
            return c;
        }
        if (isModifiable()) {
            return new LinkedHashSet<E>();
        }
        return Collections.emptySet();
    }

    /**
     * Returns the specified collection, or a new one if {@code c} is null.
     * This is a convenience method for implementation of {@code getFoo()}
     * methods.
     *
     * {@section Choosing a collection type}
     * Implementations shall invoke {@link #nonNullList nonNullList} or {@link #nonNullSet
     * nonNullSet} instead than this method when the collection type is enforced by ISO
     * specification. When the type is not enforced by the specification, some freedom are
     * allowed at implementor choice. The default implementation invokes
     * {@link #collectionType(Class)} in order to get a hint about whether a {@link List}
     * or a {@link Set} should be used.
     *
     * @param  <E> The type of elements in the collection.
     * @param  c The collection to checks.
     * @param  elementType The element type (used only if {@code c} is null).
     * @return {@code c}, or a new collection if {@code c} is null.
     */
    // Despite the javadoc claims, we do not yet return null during copy operations.
    // However a future version may do so if it appears worth on a performance point of view.
    protected final <E> Collection<E> nonNullCollection(final Collection<E> c, final Class<E> elementType) {
        assert Thread.holdsLock(this);
        if (c != null) {
            assert collectionType(elementType).isAssignableFrom(c.getClass());
            return c;
        }
        final boolean isModifiable = isModifiable();
        if (useSet(elementType)) {
            if (isModifiable) {
                return new LinkedHashSet<E>();
            } else {
                return Collections.emptySet();
            }
        } else {
            if (isModifiable) {
                return new ArrayList<E>();
            } else {
                return Collections.emptyList();
            }
        }
    }

    /**
     * Returns {@code true} if we should use a {@link Set} instead than a {@link List}
     * for elements of the given type.
     */
    private <E> boolean useSet(final Class<E> elementType) {
        final Class<? extends Collection<E>> type = collectionType(elementType);
        if (Set.class.isAssignableFrom(type)) {
            return true;
        }
        if (List.class.isAssignableFrom(type)) {
            return false;
        }
        throw new NoSuchElementException("Unsupported data type");
    }

    /**
     * Returns the type of collection to use for the given type. The current implementation can
     * return only two values: <code>{@linkplain Set}.class</code> if the attribute should not
     * accept duplicated values, or <code>{@linkplain List}.class</code> otherwise. Future SIS
     * versions may accept other types.
     * <p>
     * The default implementation returns <code>{@linkplain Set}.class</code> if the element type
     * is assignable to {@link Enum} or {@link CodeList}, and <code>{@linkplain List}.class</code>
     * otherwise. Subclasses can override this method for choosing different kind of collections.
     * <em>Note however that {@link Set} should be used only with immutable element types</em>,
     * for {@linkplain Object#hashCode() hash code} stability.
     *
     * @param  <E> The type of elements in the collection to be created.
     * @param  elementType The type of elements in the collection to be created.
     * @return {@code List.class} or {@code Set.class} depending on whether the
     *         attribute shall accept duplicated values or not.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    protected <E> Class<? extends Collection<E>> collectionType(final Class<E> elementType) {
        return (Class) (CodeList.class.isAssignableFrom(elementType) ||
                            Enum.class.isAssignableFrom(elementType) ? Set.class : List.class);
    }
}
