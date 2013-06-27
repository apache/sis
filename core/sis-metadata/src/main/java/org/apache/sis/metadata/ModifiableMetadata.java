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

import java.util.Set;
import java.util.List;
import java.util.EnumSet;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.lang.reflect.Modifier;
import org.opengis.util.CodeList;
import org.apache.sis.util.ThreadSafe;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.internal.util.CheckedHashSet;
import org.apache.sis.internal.util.CheckedArrayList;

import static org.apache.sis.util.collection.Containers.isNullOrEmpty;
import static org.apache.sis.internal.jaxb.Context.isMarshalling;


/**
 * Provides convenience methods for support of modifiable properties in metadata implementations.
 * Implementations typically provide {@code set*(…)} methods for each corresponding {@code get*()}
 * method. Subclasses can follow the pattern below for every {@code get} and {@code set} methods,
 * with a different processing for singleton value or for {@linkplain Collection collections}.
 *
 * <p>For singleton value:</p>
 *
 * {@preformat java
 *     public class MyMetadata {
 *         private Foo property;
 *
 *         public Foo getProperty() {
 *             return property;
 *         }
 *
 *         public void setProperty(Foo newValue) {
 *             checkWritePermission();
 *             property = newValue;
 *         }
 *     }
 * }
 *
 * For collections (note that the call to {@link #checkWritePermission()} is implicit):
 *
 * {@preformat java
 *     public class MyMetadata {
 *         private Collection<Foo> properties;
 *
 *         public Collection<Foo> getProperties() {
 *             return properties = nonNullCollection(properties, Foo.class);
 *         }
 *
 *         public void setProperties(Collection<Foo> newValues) {
 *             properties = writeCollection(newValues, properties, Foo.class);
 *         }
 *     }
 * }
 *
 * An initially modifiable metadata may become unmodifiable at a later stage
 * (typically after its construction is completed) by the call to the
 * {@link #freeze()} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@ThreadSafe
public abstract class ModifiableMetadata extends AbstractMetadata implements Cloneable {
    /**
     * Initial capacity of lists and sets. We use a small value because those
     * collections will typically contain few elements (often just a singleton).
     */
    private static final int INITIAL_CAPACITY = 4;

    /**
     * A null implementation for the {@link #FREEZING} constant.
     */
    private static final class Null extends ModifiableMetadata {
        @Override public MetadataStandard getStandard() {
            return null;
        }
    }

    /**
     * A flag used for {@link #unmodifiable} in order to specify that
     * {@link #freeze()} is under way.
     */
    private static final ModifiableMetadata FREEZING = new Null();

    /**
     * An unmodifiable copy of this metadata, created only when first needed.
     * If {@code null}, then no unmodifiable entity is available.
     * If {@code this}, then this entity is itself unmodifiable.
     *
     * @see #unmodifiable()
     */
    private transient ModifiableMetadata unmodifiable;

    /**
     * Constructs an initially empty metadata.
     */
    protected ModifiableMetadata() {
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
        return unmodifiable != this;
    }

    /**
     * Returns an unmodifiable copy of this metadata. Any attempt to modify a property of the
     * returned object will throw an {@link UnmodifiableMetadataException}. The state of this
     * object is not modified.
     *
     * <p>This method is useful for reusing the same metadata object as a template.
     * For example:</p>
     *
     * {@preformat java
     *     DefaultCitation myCitation = new DefaultCitation();
     *     myCitation.setTitle(new SimpleInternationalString("The title of my book"));
     *     myCitation.setEdition(new SimpleInternationalString("First edition"));
     *     final Citation firstEdition = (Citation) myCitation.unmodifiable();
     *
     *     myCitation.setEdition(new SimpleInternationalString("Second edition"));
     *     final Citation secondEdition = (Citation) myCitation.unmodifiable();
     *     // The title of the second edition is unchanged compared to the first edition.
     * }
     *
     * The default implementation makes the following choice:
     *
     * <ul>
     *   <li>If this metadata is itself unmodifiable, then this method returns {@code this}
     *       unchanged.</li>
     *   <li>Otherwise this method {@linkplain #clone() clone} this metadata and
     *       {@linkplain #freeze() freeze} the clone before to return it.</li>
     * </ul>
     *
     * @return An unmodifiable copy of this metadata.
     */
    public AbstractMetadata unmodifiable() {
        // Reminder: 'unmodifiable' is reset to null by checkWritePermission().
        if (unmodifiable == null) {
            final ModifiableMetadata candidate;
            try {
                /*
                 * Need a SHALLOW copy of this metadata, because some properties
                 * may already be unmodifiable and we don't want to clone them.
                 */
                candidate = clone();
            } catch (CloneNotSupportedException exception) {
                /*
                 * The metadata is not cloneable for some reason left to the user
                 * (for example it may be backed by some external database).
                 * Assumes that the metadata is unmodifiable.
                 */
                Logging.unexpectedException(LOGGER, getClass(), "unmodifiable", exception);
                return this;
            }
            candidate.freeze();
            // Set the field only after success. The 'unmodifiable' field must
            // stay null if an exception occurred during clone() or freeze().
            unmodifiable = candidate;
        }
        assert !unmodifiable.isModifiable();
        return unmodifiable;
    }

    /**
     * Declares this metadata and all its properties as unmodifiable. Any attempt to modify a
     * property after this method call will throw an {@link UnmodifiableMetadataException}.
     * If this metadata is already unmodifiable, then this method does nothing.
     *
     * <p>Subclasses usually don't need to override this method since the default implementation
     * performs its work using Java reflection.</p>
     *
     * @see #isModifiable()
     * @see #checkWritePermission()
     */
    public void freeze() {
        if (isModifiable()) {
            ModifiableMetadata success = null;
            try {
                unmodifiable = FREEZING;
                getStandard().freeze(this);
                success = this;
            } finally {
                unmodifiable = success;
            }
        }
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
        if (!isModifiable()) {
            throw new UnmodifiableMetadataException(Errors.format(Errors.Keys.UnmodifiableMetadata));
        }
        unmodifiable = null;
    }

    /**
     * Writes the content of the {@code source} collection into the {@code target} list,
     * creating it if needed. This method performs the following steps:
     *
     * <ul>
     *   <li>Invokes {@link #checkWritePermission()} in order to ensure that this metadata is
     *       modifiable.</li>
     *   <li>If {@code source} is null or empty, returns {@code null}
     *       (meaning that the metadata property is not provided).</li>
     *   <li>If {@code target} is null, creates a new {@link List}.</li>
     *   <li>Copies the content of the given {@code source} into the target.</li>
     * </ul>
     *
     * @param  <E>         The type represented by the {@code Class} argument.
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
    protected final <E> List<E> writeList(final Collection<? extends E> source,
            List<E> target, final Class<E> elementType)
            throws UnmodifiableMetadataException
    {
        // See the comments in writeCollection(...) for implementation notes.
        if (source != target) {
            if (unmodifiable == FREEZING) {
                return (List<E>) source;
            }
            checkWritePermission();
            if (isNullOrEmpty(source)) {
                target = null;
            } else {
                if (target != null) {
                    target.clear();
                } else {
                    target = new CheckedArrayList<E>(elementType, source.size());
                }
                target.addAll(source);
            }
        }
        return target;
    }

    /**
     * Writes the content of the {@code source} collection into the {@code target} set,
     * creating it if needed. This method performs the following steps:
     *
     * <ul>
     *   <li>Invokes {@link #checkWritePermission()} in order to ensure that this metadata is
     *       modifiable.</li>
     *   <li>If {@code source} is null or empty, returns {@code null}
     *       (meaning that the metadata property is not provided).</li>
     *   <li>If {@code target} is null, creates a new {@link Set}.</li>
     *   <li>Copies the content of the given {@code source} into the target.</li>
     * </ul>
     *
     * @param  <E>         The type represented by the {@code Class} argument.
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
    protected final <E> Set<E> writeSet(final Collection<? extends E> source,
            Set<E> target, final Class<E> elementType)
            throws UnmodifiableMetadataException
    {
        // See the comments in writeCollection(...) for implementation notes.
        if (source != target) {
            if (unmodifiable == FREEZING) {
                return (Set<E>) source;
            }
            checkWritePermission();
            if (isNullOrEmpty(source)) {
                target = null;
            } else {
                if (target != null) {
                    target.clear();
                } else {
                    target = new CheckedHashSet<E>(elementType, source.size());
                }
                target.addAll(source);
            }
        }
        return target;
    }

    /**
     * Writes the content of the {@code source} collection into the {@code target} list or set,
     * creating it if needed. This method performs the following steps:
     *
     * <ul>
     *   <li>Invokes {@link #checkWritePermission()} in order to ensure that this metadata is
     *       modifiable.</li>
     *   <li>If {@code source} is null or empty, returns {@code null}
     *       (meaning that the metadata property is not provided).</li>
     *   <li>If {@code target} is null, creates a new {@link Set} or a new {@link List}
     *       depending on the value returned by {@link #collectionType(Class)}.</li>
     *   <li>Copies the content of the given {@code source} into the target.</li>
     * </ul>
     *
     * {@section Choosing a collection type}
     * Implementations shall invoke {@link #writeList writeList} or {@link #writeSet writeSet} methods
     * instead than this method when the collection type is enforced by ISO specification.
     * When the type is not enforced by the specification, some freedom are allowed at
     * implementor choice. The default implementation invokes {@link #collectionType(Class)}
     * in order to get a hint about whether a {@link List} or a {@link Set} should be used.
     *
     * @param  <E>         The type represented by the {@code Class} argument.
     * @param  source      The source collection, or {@code null}.
     * @param  target      The target collection, or {@code null} if not yet created.
     * @param  elementType The base type of elements to put in the collection.
     * @return A collection (possibly the {@code target} instance) containing the {@code source}
     *         elements, or {@code null} if the source was null.
     * @throws UnmodifiableMetadataException if this metadata is unmodifiable.
     */
    @SuppressWarnings("unchecked")
    protected final <E> Collection<E> writeCollection(final Collection<? extends E> source,
            Collection<E> target, final Class<E> elementType)
            throws UnmodifiableMetadataException
    {
        /*
         * It is not worth to copy the content if the current and the new instance are the
         * same. This is safe only using the != operator, not the !equals(Object) method.
         * This optimization is required for efficient working of PropertyAccessor.set(...).
         */
        if (source != target) {
            if (unmodifiable == FREEZING) {
                /*
                 * freeze() method is under progress. The source collection is already
                 * an unmodifiable instance created by unmodifiable(Object).
                 */
                assert collectionType(elementType).isInstance(source);
                return (Collection<E>) source;
            }
            checkWritePermission();
            if (isNullOrEmpty(source)) {
                target = null;
            } else {
                if (target != null) {
                    target.clear();
                } else {
                    final int capacity = source.size();
                    if (useSet(elementType)) {
                        target = createSet(elementType, capacity);
                    } else {
                        target = new CheckedArrayList<E>(elementType, capacity);
                    }
                }
                target.addAll(source);
            }
        }
        return target;
    }

    /**
     * Creates a list with the content of the {@code source} collection,
     * or returns {@code null} if the source is {@code null} or empty.
     * This is a convenience method for copying fields in subclass copy constructors.
     *
     * @param  <E>         The type represented by the {@code Class} argument.
     * @param  source      The source collection, or {@code null}.
     * @param  elementType The base type of elements to put in the list.
     * @return A list containing the {@code source} elements,
     *         or {@code null} if the source was null or empty.
     */
    protected final <E> List<E> copyList(final Collection<? extends E> source, final Class<E> elementType) {
        if (isNullOrEmpty(source)) {
            return null;
        }
        final List<E> target = new CheckedArrayList<E>(elementType, source.size());
        target.addAll(source);
        return target;
    }

    /**
     * Creates a set with the content of the {@code source} collection,
     * or returns {@code null} if the source is {@code null} or empty.
     * This is a convenience method for copying fields in subclass copy constructors.
     *
     * @param  <E>         The type represented by the {@code Class} argument.
     * @param  source      The source collection, or {@code null}.
     * @param  elementType The base type of elements to put in the set.
     * @return A set containing the {@code source} elements,
     *         or {@code null} if the source was null or empty.
     */
    protected final <E> Set<E> copySet(final Collection<? extends E> source, final Class<E> elementType) {
        if (isNullOrEmpty(source)) {
            return null;
        }
        final Set<E> target = new CheckedHashSet<E>(elementType, source.size());
        target.addAll(source);
        return target;
    }

    /**
     * Creates a list or set with the content of the {@code source} collection,
     * or returns {@code null} if the source is {@code null} or empty.
     * This is a convenience method for copying fields in subclass copy constructors.
     *
     * <p>The collection type is selected as described in the
     * {@link #nonNullCollection(Collection, Class)}.</p>
     *
     * @param  <E>         The type represented by the {@code Class} argument.
     * @param  source      The source collection, or {@code null}.
     * @param  elementType The base type of elements to put in the collection.
     * @return A collection containing the {@code source} elements,
     *         or {@code null} if the source was null or empty.
     */
    protected final <E> Collection<E> copyCollection(final Collection<? extends E> source, final Class<E> elementType) {
        if (isNullOrEmpty(source)) {
            return null;
        }
        final Collection<E> target;
        final int capacity = source.size();
        if (useSet(elementType)) {
            target = createSet(elementType, capacity);
        } else {
            target = new CheckedArrayList<E>(elementType, capacity);
        }
        target.addAll(source);
        return target;
    }

    /**
     * Creates a singleton list or set containing only the given value, if non-null.
     * This is a convenience method for initializing fields in subclass constructors.
     *
     * <p>The collection type is selected as described in the
     * {@link #nonNullCollection(Collection, Class)}.</p>
     *
     * @param  <E>         The type represented by the {@code Class} argument.
     * @param  value       The singleton value to put in the returned collection, or {@code null}.
     * @param  elementType The element type (used only if {@code value} is non-null).
     * @return A new modifiable collection containing the given value,
     *         or {@code null} if the given value was null.
     */
    protected final <E> Collection<E> singleton(final E value, final Class<E> elementType) {
        if (value == null) {
            return null;
        }
        final Collection<E> collection;
        if (useSet(elementType)) {
            collection = createSet(elementType, INITIAL_CAPACITY);
        } else {
            collection = new CheckedArrayList<E>(elementType, INITIAL_CAPACITY);
        }
        collection.add(value);
        return collection;
    }

    /**
     * Returns the specified list, or a new one if {@code c} is null.
     * This is a convenience method for implementation of {@code getFoo()} methods.
     *
     * @param  <E> The type represented by the {@code Class} argument.
     * @param  c The existing list, or {@code null} if the list has not yet been created.
     * @param  elementType The element type (used only if {@code c} is null).
     * @return {@code c}, or a new list if {@code c} is null.
     */
    protected final <E> List<E> nonNullList(final List<E> c, final Class<E> elementType) {
        if (c != null) {
            return c.isEmpty() && isMarshalling() ? null : c;
        }
        if (isMarshalling()) {
            return null;
        }
        if (isModifiable()) {
            return new CheckedArrayList<E>(elementType, INITIAL_CAPACITY);
        }
        return Collections.emptyList();
    }

    /**
     * Returns the specified set, or a new one if {@code c} is null.
     * This is a convenience method for implementation of {@code getFoo()} methods.
     *
     * @param  <E> The type represented by the {@code Class} argument.
     * @param  c The existing set, or {@code null} if the set has not yet been created.
     * @param  elementType The element type (used only if {@code c} is null).
     * @return {@code c}, or a new set if {@code c} is null.
     */
    protected final <E> Set<E> nonNullSet(final Set<E> c, final Class<E> elementType) {
        if (c != null) {
            return c.isEmpty() && isMarshalling() ? null : c;
        }
        if (isMarshalling()) {
            return null;
        }
        if (isModifiable()) {
            return new CheckedHashSet<E>(elementType, INITIAL_CAPACITY);
        }
        return Collections.emptySet();
    }

    /**
     * Returns the specified collection, or a new one if {@code c} is null.
     * This is a convenience method for implementation of {@code getFoo()} methods.
     *
     * {@section Choosing a collection type}
     * Implementations shall invoke {@link #nonNullList nonNullList(…)} or {@link #nonNullSet
     * nonNullSet(…)} instead than this method when the collection type is enforced by ISO
     * specification. When the type is not enforced by the specification, some freedom are
     * allowed at implementor choice. The default implementation invokes
     * {@link #collectionType(Class)} in order to get a hint about whether a {@link List}
     * or a {@link Set} should be used.
     *
     * @param  <E> The type represented by the {@code Class} argument.
     * @param  c The existing collection, or {@code null} if the collection has not yet been created.
     * @param  elementType The element type (used only if {@code c} is null).
     * @return {@code c}, or a new collection if {@code c} is null.
     */
    protected final <E> Collection<E> nonNullCollection(final Collection<E> c, final Class<E> elementType) {
        if (c != null) {
            assert collectionType(elementType).isInstance(c);
            return c.isEmpty() && isMarshalling() ? null : c;
        }
        if (isMarshalling()) {
            return null;
        }
        final boolean isModifiable = isModifiable();
        if (useSet(elementType)) {
            if (isModifiable) {
                return createSet(elementType, INITIAL_CAPACITY);
            } else {
                return Collections.emptySet();
            }
        } else {
            if (isModifiable) {
                return new CheckedArrayList<E>(elementType, INITIAL_CAPACITY);
            } else {
                return Collections.emptyList();
            }
        }
    }

    /**
     * Creates a modifiable set for elements of the given type. This method will create an {@link EnumSet},
     * {@link CodeListSet} or {@link LinkedHashSet} depending on the {@code elementType} argument.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private <E> Set<E> createSet(final Class<E> elementType, final int capacity) {
        if (Enum.class.isAssignableFrom(elementType)) {
            return EnumSet.noneOf((Class) elementType);
        }
        if (CodeList.class.isAssignableFrom(elementType) && Modifier.isFinal(elementType.getModifiers())) {
            return new CodeListSet((Class) elementType);
        }
        return new CheckedHashSet<E>(elementType, capacity);
    }

    /**
     * Returns {@code true} if we should use a {@link Set} instead than a {@link List}
     * for elements of the given type.
     */
    private <E> boolean useSet(final Class<E> elementType) {
        final Class<? extends Collection<E>> type = collectionType(elementType);
        if (Set .class == (Class) type) return true;
        if (List.class == (Class) type) return false;
        throw new NoSuchElementException(Errors.format(Errors.Keys.UnsupportedType_1, type));
    }

    /**
     * Returns the type of collection to use for the given type. The current implementation can
     * return only two values: <code>{@linkplain Set}.class</code> if the property should not
     * accept duplicated values, or <code>{@linkplain List}.class</code> otherwise. Future SIS
     * versions may accept other types.
     *
     * <p>The default implementation returns <code>{@linkplain Set}.class</code> if the element type
     * is assignable to {@link Enum} or {@link CodeList}, and <code>{@linkplain List}.class</code>
     * otherwise. Subclasses can override this method for choosing different kind of collections.
     * <em>Note however that {@link Set} should be used only with immutable element types</em>,
     * for {@linkplain Object#hashCode() hash code} stability.</p>
     *
     * @param  <E> The type of elements in the collection to be created.
     * @param  elementType The type of elements in the collection to be created.
     * @return {@code List.class} or {@code Set.class} depending on whether the
     *         property shall accept duplicated values or not.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    protected <E> Class<? extends Collection<E>> collectionType(final Class<E> elementType) {
        return (Class) (CodeList.class.isAssignableFrom(elementType) ||
                            Enum.class.isAssignableFrom(elementType) ? Set.class : List.class);
    }

    /**
     * Returns a shallow copy of this metadata.
     * While {@linkplain Cloneable cloneable}, this class does not provides the {@code clone()}
     * operation as part of the public API. The clone operation is required for the internal
     * working of the {@link #unmodifiable()} method, which needs <em>shallow</em>
     * copies of metadata entities. The default {@link Object#clone()} implementation is
     * sufficient in most cases.
     *
     * @return A <em>shallow</em> copy of this metadata.
     * @throws CloneNotSupportedException if the clone is not supported.
     */
    @Override
    protected ModifiableMetadata clone() throws CloneNotSupportedException {
        return (ModifiableMetadata) super.clone();
    }
}
