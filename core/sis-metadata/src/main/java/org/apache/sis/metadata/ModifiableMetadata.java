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
import java.util.Locale;
import java.util.Currency;
import java.util.NoSuchElementException;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.CodeList;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.internal.util.CheckedHashSet;
import org.apache.sis.internal.util.CheckedArrayList;
import org.apache.sis.internal.system.Semaphores;

import static org.apache.sis.util.collection.Containers.isNullOrEmpty;


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
 * (typically after its construction is completed) by the call to {@code apply(State.FINAL)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
@XmlTransient
public abstract class ModifiableMetadata extends AbstractMetadata {
    /**
     * Initial capacity of sets. We use a small value because collections will typically
     * contain few elements (often just a singleton).
     */
    private static final int INITIAL_CAPACITY = 4;

    /**
     * The {@link #state} value meaning that the metadata is modifiable.
     * This is the default state when new {@link ModifiableMetadata} instances are created.
     */
    private static final byte EDITABLE = 0;

    /**
     * A bitmask for {@link #state} meaning that {@code apply(State.FINAL)} has been invoked.
     */
    private static final byte FINAL = 1;

    /**
     * See https://issues.apache.org/jira/browse/SIS-81 - not yet committed.
     */
    private static final byte STAGED = 2;

    /**
     * A value for {@link #state} meaning that execution of {@code apply(State.FINAL)} is in progress.
     */
    private static final byte FREEZING = FINAL | STAGED;

    /**
     * Whether this metadata has been made unmodifiable, as one of {@link #EDITABLE}, {@link #FREEZING}
     * or {@link #FINAL} values.
     *
     * <p>This field is not yet serialized because we are not sure to keep this information as a byte in
     * the future. We could for example use an {@code int} and use remaining bits for caching hash-code
     * value of final metadata.</p>
     */
    private transient byte state;

    /**
     * An unmodifiable copy of this metadata, created only when first needed.
     * If {@code null}, then no unmodifiable entity is available.
     * If {@code this}, then this entity is itself unmodifiable.
     *
     * @deprecated to be deleted after the removal of {@link #unmodifiable()}.
     */
    @Deprecated
    private transient ModifiableMetadata unmodifiable;

    /**
     * Constructs an initially empty metadata.
     * The initial state is {@link State#EDITABLE}.
     */
    protected ModifiableMetadata() {
    }

    /**
     * Whether the metadata is still editable or has been made final.
     * New {@link ModifiableMetadata} instances are initially {@link #EDITABLE}
     * and can be made {@link #FINAL} after construction by a call to {@link ModifiableMetadata#apply(State)}.
     *
     * <div class="note"><b>Note:</b>
     * more states may be added in future Apache SIS versions. On possible candidate is {@code STAGED}.
     * See <a href="https://issues.apache.org/jira/browse/SIS-81">SIS-81</a>.</div>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.0
     * @since   1.0
     * @module
     */
    public enum State {
        /**
         * The metadata is modifiable.
         * This is the default state when new {@link ModifiableMetadata} instances are created.
         * Note that a modifiable metadata instance does <strong>not</strong> imply that all
         * properties contained in that instance are also editable.
         */
        EDITABLE,

        /**
         * The metadata is unmodifiable.
         * When a metadata is final, it can not be moved back to an editable state
         * (but it is still possible to create a modifiable copy with {@link MetadataCopier}).
         * Invoking any setter method on an unmodifiable metadata cause an
         * {@link UnmodifiableMetadataException} to be thrown.
         */
        FINAL;

        /**
         * Mapping from {@link ModifiableMetadata} private flags to {@code State} enumeration.
         * A mapping exists because {@code ModifiableMetadata} does not use the same set of enumeration values
         * (e.g. it has an internal {@link #FREEZING} value), and because future versions may use a bitmask.
         */
        private static final State[] VALUES = new State[ModifiableMetadata.FREEZING + 1];
        static {
            VALUES[ModifiableMetadata.EDITABLE] = EDITABLE;
            VALUES[ModifiableMetadata.STAGED]   = EDITABLE;
            VALUES[ModifiableMetadata.FREEZING] = FINAL;
            VALUES[ModifiableMetadata.FINAL]    = FINAL;
        }
    }

    /**
     * Tells whether this instance of metadata is editable.
     * This is initially {@link State#EDITABLE} for new {@code ModifiableMetadata} instances,
     * but can be changed by a call to {@link #apply(State)}.
     *
     * <p>{@link State#FINAL} implies that all properties are also final.
     * This recursivity does not necessarily apply to other states. For example {@link State#EDITABLE}
     * does <strong>not</strong> imply that all {@code ModifiableMetadata} children are also editable.</p>
     *
     * <div class="note"><b>API note:</b>
     * the {@code ModifiableMetadata} state is not a metadata per se, but rather an information about
     * this particular instance of a metadata class. Two metadata instances may be in different states
     * but still have the same metadata content. For this reason, this method does not have {@code get}
     * prefix for avoiding confusion with getter and setter methods of metadata properties.</div>
     *
     * @return the state (editable or final) of this {@code ModifiableMetadata} instance.
     *
     * @since 1.0
     */
    public State state() {
        return State.VALUES[state];
    }

    /**
     * Applies a state transition on this metadata instance and (potentially) all its children.
     * The action performed by this method depends on the {@linkplain #state() current state},
     * as listed in the following table:
     *
     * <table class="sis">
     *   <caption>State transitions</caption>
     *   <tr>
     *     <th>Current state</th>
     *     <th>Target state</th>
     *     <th>Action</th>
     *   </tr><tr>
     *     <td><var>Any</var></td>
     *     <td><var>Same</var></td>
     *     <td>Does nothing and returns {@code false}.</td>
     *   </tr><tr>
     *     <td>{@link State#EDITABLE}</td>
     *     <td>{@link State#FINAL}</td>
     *     <td>Marks this metadata and all children as unmodifiable.</td>
     *   </tr><tr>
     *     <td>{@link State#FINAL}</td>
     *     <td>{@link State#EDITABLE}</td>
     *     <td>Throws {@link UnmodifiableMetadataException}.</td>
     *   </tr>
     * </table>
     *
     * The effect of invoking this method may be recursive. For example transitioning to {@link State#FINAL}
     * implies transitioning all children {@code ModifiableMetadata} instances to the final state too.
     *
     * @param  target  the desired new state.
     * @return {@code true} if the state of this {@code ModifiableMetadata} changed as a result of this method call.
     * @throws UnmodifiableMetadataException if a transition from {@link State#FINAL} to {@link State#EDITABLE} was attempted.
     *
     * @since 1.0
     */
    public boolean apply(final State target) {
        switch (target) {
            case EDITABLE: {
                if ((state & FINAL) == 0) break;
                throw new UnmodifiableMetadataException(Errors.format(Errors.Keys.UnmodifiableMetadata));
            }
            case FINAL: {
                if ((state & FINAL) != 0) break;
                byte result = state;
                try {
                    state = FREEZING;
                    StateChanger.applyTo(target, this);
                    result = FINAL;
                } finally {
                    state = result;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this metadata is modifiable. This method returns
     * {@code false} if {@link #freeze()} has been invoked on this object.
     *
     * @return {@code true} if this metadata is modifiable.
     *
     * @see #freeze()
     * @see #checkWritePermission()
     *
     * @deprecated Replaced by <code>{@linkplain #state()} != State.FINAL</code>.
     *             See <a href="https://issues.apache.org/jira/browse/SIS-81">SIS-81</a>.
     */
    @Deprecated
    public final boolean isModifiable() {
        return (state & FINAL) == 0;
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
     *   <li>If this metadata is itself unmodifiable, then this method returns {@code this} unchanged.</li>
     *   <li>Otherwise this method {@linkplain #clone() clone} this metadata and
     *       {@linkplain #freeze() freeze} the clone before to return it.</li>
     * </ul>
     *
     * @return an unmodifiable copy of this metadata.
     *
     * @see MetadataCopier
     *
     * @deprecated Replaced by {@code MetadataCopier.forModifiable(getStandard()).copy(this).apply(State.FINAL)}.
     */
    @Deprecated
    public AbstractMetadata unmodifiable() {
        if ((state & FINAL) != 0) {
            unmodifiable = this;
        }
        /*
         * The 'unmodifiable' field is reset to null by checkWritePermission().
         * However this is not sufficient since the setter method of some child
         * could have been invoked without invoking any setter method on 'this'.
         * So we also need to perform an equality check.
         */
        if (unmodifiable == null || !equals(unmodifiable)) {
            final ModifiableMetadata candidate = (ModifiableMetadata) MetadataCopier.forModifiable(getStandard()).copy(this);
            candidate.freeze();
            /*
             * Set the field only after success. The 'unmodifiable' field must
             * stay null if an exception occurred during clone() or freeze().
             */
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
     * <p>Subclasses usually do not need to override this method since the default implementation
     * performs its work using Java reflection.</p>
     *
     * @see #state()
     * @see #checkWritePermission()
     *
     * @deprecated Replaced by {@code apply(State.FINAL)}.
     */
    @Deprecated
    public void freeze() {
        apply(State.FINAL);
    }

    /**
     * Checks if changes in the metadata are allowed. All {@code setFoo(…)} methods in subclasses
     * shall invoke this method (directly or indirectly) before to apply any change.
     *
     * @throws UnmodifiableMetadataException if this metadata is unmodifiable.
     *
     * @see #state()
     */
    protected void checkWritePermission() throws UnmodifiableMetadataException {
        if (state == FINAL) {
            throw new UnmodifiableMetadataException(Errors.format(Errors.Keys.UnmodifiableMetadata));
        } else {
            unmodifiable = null;                    // Discard since this metadata is going to change.
        }
    }

    /**
     * Writes the content of the {@code source} collection into the {@code target} list,
     * creating it if needed. This method performs the following steps:
     *
     * <ul>
     *   <li>Invokes {@link #checkWritePermission()} in order to ensure that this metadata is modifiable.</li>
     *   <li>If {@code source} is null or empty, returns {@code null}
     *       (meaning that the metadata property is not provided).</li>
     *   <li>If {@code target} is null, creates a new {@link List}.</li>
     *   <li>Copies the content of the given {@code source} into the target.</li>
     * </ul>
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  source       the source list, or {@code null}.
     * @param  target       the target list, or {@code null} if not yet created.
     * @param  elementType  the base type of elements to put in the list.
     * @return a list (possibly the {@code target} instance) containing the {@code source}
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
        // See the comments in writeCollection(…) for implementation notes.
        if (source != target) {
            if (state == FREEZING) {
                return (List<E>) source;
            }
            checkWritePermission();
            if (isNullOrEmpty(source)) {
                target = null;
            } else {
                if (target != null) {
                    target.clear();
                } else {
                    target = new CheckedArrayList<>(elementType, source.size());
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
     *   <li>Invokes {@link #checkWritePermission()} in order to ensure that this metadata is modifiable.</li>
     *   <li>If {@code source} is null or empty, returns {@code null}
     *       (meaning that the metadata property is not provided).</li>
     *   <li>If {@code target} is null, creates a new {@link Set}.</li>
     *   <li>Copies the content of the given {@code source} into the target.</li>
     * </ul>
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  source       the source set, or {@code null}.
     * @param  target       the target set, or {@code null} if not yet created.
     * @param  elementType  the base type of elements to put in the set.
     * @return a set (possibly the {@code target} instance) containing the {@code source} elements,
     *         or {@code null} if the source was null.
     * @throws UnmodifiableMetadataException if this metadata is unmodifiable.
     *
     * @see #nonNullSet(Set, Class)
     */
    @SuppressWarnings("unchecked")
    protected final <E> Set<E> writeSet(final Collection<? extends E> source,
            Set<E> target, final Class<E> elementType)
            throws UnmodifiableMetadataException
    {
        // See the comments in writeCollection(…) for implementation notes.
        if (source != target) {
            if (state == FREEZING) {
                return (Set<E>) source;
            }
            checkWritePermission();
            if (isNullOrEmpty(source)) {
                target = null;
            } else {
                if (target != null) {
                    target.clear();
                } else {
                    target = new CheckedHashSet<>(elementType, source.size());
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
     *   <li>Invokes {@link #checkWritePermission()} in order to ensure that this metadata is modifiable.</li>
     *   <li>If {@code source} is null or empty, returns {@code null}
     *       (meaning that the metadata property is not provided).</li>
     *   <li>If {@code target} is null, creates a new {@link Set} or a new {@link List}
     *       depending on the value returned by {@link #collectionType(Class)}.</li>
     *   <li>Copies the content of the given {@code source} into the target.</li>
     * </ul>
     *
     * <div class="section">Choosing a collection type</div>
     * Implementations shall invoke {@link #writeList writeList} or {@link #writeSet writeSet} methods
     * instead than this method when the collection type is enforced by ISO specification.
     * When the type is not enforced by the specification, some freedom are allowed at
     * implementor choice. The default implementation invokes {@link #collectionType(Class)}
     * in order to get a hint about whether a {@link List} or a {@link Set} should be used.
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  source       the source collection, or {@code null}.
     * @param  target       the target collection, or {@code null} if not yet created.
     * @param  elementType  the base type of elements to put in the collection.
     * @return a collection (possibly the {@code target} instance) containing the {@code source} elements,
     *         or {@code null} if the source was null.
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
         * This optimization is required for efficient working of PropertyAccessor.set(…)
         * and JAXB unmarshalling.
         */
        if (source != target) {
            if (state == FREEZING) {
                /*
                 * apply(State.FINAL) is under progress. The source collection is already
                 * an unmodifiable instance created by StageChanger.
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
                        target = new CheckedArrayList<>(elementType, capacity);
                    }
                }
                target.addAll(source);
            }
        }
        return target;
    }

    /**
     * Creates a list with the content of the {@code source} collection,
     * or returns {@code null} if the source is {@code null} or empty.
     * This is a convenience method for copying fields in subclass copy constructors.
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  source       the source collection, or {@code null}.
     * @param  elementType  the base type of elements to put in the list.
     * @return a list containing the {@code source} elements,
     *         or {@code null} if the source was null or empty.
     */
    protected final <E> List<E> copyList(final Collection<? extends E> source, final Class<E> elementType) {
        if (isNullOrEmpty(source)) {
            return null;
        }
        final List<E> target = new CheckedArrayList<>(elementType, source.size());
        target.addAll(source);
        return target;
    }

    /**
     * Creates a set with the content of the {@code source} collection,
     * or returns {@code null} if the source is {@code null} or empty.
     * This is a convenience method for copying fields in subclass copy constructors.
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  source       the source collection, or {@code null}.
     * @param  elementType  the base type of elements to put in the set.
     * @return a set containing the {@code source} elements,
     *         or {@code null} if the source was null or empty.
     */
    protected final <E> Set<E> copySet(final Collection<? extends E> source, final Class<E> elementType) {
        if (isNullOrEmpty(source)) {
            return null;
        }
        final Set<E> target = new CheckedHashSet<>(elementType, source.size());
        target.addAll(source);
        return target;
    }

    /**
     * Creates a list or set with the content of the {@code source} collection,
     * or returns {@code null} if the source is {@code null} or empty.
     * This is a convenience method for copying fields in subclass copy constructors.
     *
     * <p>The collection type is selected as described in the
     * {@link #nonNullCollection(Collection, Class)}.</p>
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  source       the source collection, or {@code null}.
     * @param  elementType  the base type of elements to put in the collection.
     * @return a collection containing the {@code source} elements,
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
            target = new CheckedArrayList<>(elementType, capacity);
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
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  value        the singleton value to put in the returned collection, or {@code null}.
     * @param  elementType  the element type (used only if {@code value} is non-null).
     * @return a new modifiable collection containing the given value,
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
            collection = new CheckedArrayList<>(elementType, 1);
        }
        collection.add(value);
        return collection;
    }

    /**
     * Returns {@code true} if empty collection should be returned as {@code null} value.
     * This is usually not a behavior that we allow in public API. However this behavior
     * is sometime desired internally, for example when marshalling with JAXB or when
     * performing a {@code equals}, {@code isEmpty} or {@code prune} operation
     * (for avoiding creating unnecessary collections).
     */
    private static boolean emptyCollectionAsNull() {
        return Semaphores.query(Semaphores.NULL_COLLECTION);
    }

    /**
     * Returns the specified list, or a new one if {@code c} is null.
     * This is a convenience method for implementation of {@code getFoo()} methods.
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  c            the existing list, or {@code null} if the list has not yet been created.
     * @param  elementType  the element type (used only if {@code c} is null).
     * @return {@code c}, or a new list if {@code c} is null.
     */
    protected final <E> List<E> nonNullList(final List<E> c, final Class<E> elementType) {
        if (c != null) {
            return c.isEmpty() && emptyCollectionAsNull() ? null : c;
        }
        if (emptyCollectionAsNull()) {
            return null;
        }
        if ((state & FINAL) == 0) {
            /*
             * Do not specify an initial capacity, because the list will stay empty in a majority of cases
             * (i.e. the users will want to iterate over the list elements more often than they will want
             * to add elements). JDK implementation of ArrayList has a lazy instantiation mechanism for
             * initially empty lists, but as of JDK8 this lazy instantiation works only for list having
             * the default capacity.
             */
            return new CheckedArrayList<>(elementType);
        }
        return Collections.emptyList();
    }

    /**
     * Returns the specified set, or a new one if {@code c} is null.
     * This is a convenience method for implementation of {@code getFoo()} methods.
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  c            the existing set, or {@code null} if the set has not yet been created.
     * @param  elementType  the element type (used only if {@code c} is null).
     * @return {@code c}, or a new set if {@code c} is null.
     */
    protected final <E> Set<E> nonNullSet(final Set<E> c, final Class<E> elementType) {
        if (c != null) {
            return c.isEmpty() && emptyCollectionAsNull() ? null : c;
        }
        if (emptyCollectionAsNull()) {
            return null;
        }
        if ((state & FINAL) == 0) {
            return createSet(elementType, INITIAL_CAPACITY);
        }
        return Collections.emptySet();
    }

    /**
     * Returns the specified collection, or a new one if {@code c} is null.
     * This is a convenience method for implementation of {@code getFoo()} methods.
     *
     * <div class="section">Choosing a collection type</div>
     * Implementations shall invoke {@link #nonNullList nonNullList(…)} or {@link #nonNullSet nonNullSet(…)}
     * instead than this method when the collection type is enforced by ISO specification.
     * When the type is not enforced by the specification, some freedom are allowed at implementor choice.
     * The default implementation invokes {@link #collectionType(Class)} in order to get a hint about whether
     * a {@link List} or a {@link Set} should be used.
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  c            the existing collection, or {@code null} if the collection has not yet been created.
     * @param  elementType  the element type (used only if {@code c} is null).
     * @return {@code c}, or a new collection if {@code c} is null.
     */
    protected final <E> Collection<E> nonNullCollection(final Collection<E> c, final Class<E> elementType) {
        if (c != null) {
            assert collectionType(elementType).isInstance(c);
            return c.isEmpty() && emptyCollectionAsNull() ? null : c;
        }
        if (emptyCollectionAsNull()) {
            return null;
        }
        final boolean isModifiable = (state & FINAL) == 0;
        if (useSet(elementType)) {
            if (isModifiable) {
                return createSet(elementType, INITIAL_CAPACITY);
            } else {
                return Collections.emptySet();
            }
        } else {
            if (isModifiable) {
                // Do not specify an initial capacity for the reason explained in nonNullList(…).
                return new CheckedArrayList<>(elementType);
            } else {
                return Collections.emptyList();
            }
        }
    }

    /**
     * Creates a modifiable set for elements of the given type. This method will create an {@link EnumSet},
     * {@link CodeListSet} or {@link java.util.LinkedHashSet} depending on the {@code elementType} argument.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private <E> Set<E> createSet(final Class<E> elementType, final int capacity) {
        if (Enum.class.isAssignableFrom(elementType)) {
            return EnumSet.noneOf((Class) elementType);
        }
        if (CodeList.class.isAssignableFrom(elementType) && Modifier.isFinal(elementType.getModifiers())) {
            return new CodeListSet(elementType);
        }
        return new CheckedHashSet<>(elementType, capacity);
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
     * is assignable to {@link CodeList}, {@link Enum}, {@link String}, {@link Charset},
     * {@link Locale} or {@link Currency}, and <code>{@linkplain List}.class</code> otherwise.
     * Subclasses can override this method for choosing different kind of collections.
     * <em>Note however that {@link Set} should be used only with immutable element types</em>,
     * for {@linkplain Object#hashCode() hash code} stability.</p>
     *
     * @param  <E>          the type of elements in the collection to be created.
     * @param  elementType  the type of elements in the collection to be created.
     * @return {@code List.class} or {@code Set.class} depending on whether the
     *         property shall accept duplicated values or not.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    protected <E> Class<? extends Collection<E>> collectionType(final Class<E> elementType) {
        return (Class) (CodeList.class.isAssignableFrom(elementType)
                ||          Enum.class.isAssignableFrom(elementType)
                ||       Charset.class.isAssignableFrom(elementType)
                ||        String.class ==               elementType
                ||        Locale.class ==               elementType
                ||      Currency.class ==               elementType
                ? Set.class : List.class);
    }
}
