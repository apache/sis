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
import java.util.Set;
import java.util.List;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Currency;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.util.CodeList;
import org.opengis.metadata.Metadata;               // For javadoc
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.util.internal.shared.CheckedHashSet;
import org.apache.sis.util.internal.shared.CheckedArrayList;
import org.apache.sis.metadata.internal.Resources;
import org.apache.sis.system.Semaphores;
import org.apache.sis.pending.jdk.JDK19;
import static org.apache.sis.util.collection.Containers.isNullOrEmpty;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.valueIfDefined;


/**
 * Base class of metadata having an editable content.
 * Newly created {@code ModifiableMetadata} are initially in {@linkplain State#EDITABLE editable} state.
 * The metadata can be populated using the setter methods provided by subclasses, then transition to the
 * {@linkplain State#FINAL final} state for making it safe to share by many consumers.
 *
 * <h2>Tip for subclass implementations</h2>
 * Subclasses can follow the pattern below for every {@code get} and {@code set} methods,
 * with a different processing for singleton value or for {@linkplain Collection collections}.
 *
 * {@snippet lang="java" :
 *     public class MyMetadata {
 *
 *         // ==== Example for a singleton value =============================
 *
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
 *
 *         // ==== Example for a collection ==================================
 *
 *         private Collection<Foo> properties;
 *
 *         public Collection<Foo> getProperties() {
 *             return properties = nonNullCollection(properties, Foo.class);
 *         }
 *
 *         public void setProperties(Collection<Foo> newValues) {
 *             // the call to checkWritePermission() is implicit
 *             properties = writeCollection(newValues, properties, Foo.class);
 *         }
 *     }
 *     }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.3
 */
@XmlTransient
public abstract class ModifiableMetadata extends AbstractMetadata {
    /**
     * The {@link #state} value meaning that the metadata is modifiable.
     * This is the default state when new {@link ModifiableMetadata} instances are created.
     */
    private static final byte EDITABLE = 0;

    /**
     * See https://issues.apache.org/jira/browse/SIS-81 - not yet committed.
     */
    private static final byte STAGED = 1;

    /**
     * A value for {@link #state} meaning that execution of {@code transitionTo(…)} is in progress.
     * Must be greater than all other values except {@link #COMPLETABLE} and {@link #FINAL}.
     */
    private static final byte FREEZING = 2;

    /**
     * The {@link #state} value meaning that missing properties can be set,
     * but no existing properties can be modified (including collections).
     * This is a kind of semi-final state.
     */
    private static final byte COMPLETABLE = 3;

    /**
     * A value for {@link #state} meaning that {@code transitionTo(State.FINAL)} has been invoked.
     * Must be greater than all other values.
     */
    private static final byte FINAL = 4;

    /**
     * Whether this metadata has been made unmodifiable, as one of {@link #EDITABLE}, {@link #FREEZING}
     * {@link #COMPLETABLE} or {@link #FINAL} values.
     *
     * <h4>Serialization</h4>
     * This field must be declared transient for preventing JAXB to inherit it in subclasses annotated
     * with {@code @XmlAccessorType(XmlAccessType.FIELD)}. Furthermore, serializing the byte value would
     * not be future-proof.
     */
    private transient byte state;

    /**
     * Constructs an initially empty metadata.
     * The initial state is {@link State#EDITABLE}.
     */
    protected ModifiableMetadata() {
    }

    /**
     * Creates an initially empty metadata with nil reasons copied from the given object.
     * The given object should be a metadata of the same class.
     *
     * @param  source  the metadata from which to copy nil reasons, or {@code null} if none.
     *
     * @since 1.5
     */
    protected ModifiableMetadata(final Object source) {
        super(source);
    }

    /**
     * Whether the metadata is still editable or has been made final.
     * New {@link ModifiableMetadata} instances are initially {@link #EDITABLE}
     * and can be made {@link #FINAL} after construction by a call to {@link ModifiableMetadata#transitionTo(State)}.
     *
     * <h2>Future evolution</h2>
     * More states may be added in future Apache SIS versions. On possible candidate is {@code STAGED}.
     * See <a href="https://issues.apache.org/jira/browse/SIS-81">SIS-81</a>.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.4
     *
     * @see ModifiableMetadata#state()
     *
     * @since 1.0
     */
    public enum State {
        /**
         * The metadata is modifiable.
         * This is the default state when new {@link ModifiableMetadata} instances are created.
         * Note that a modifiable metadata instance does <strong>not</strong> imply that all
         * properties contained in that instance are also editable.
         */
        EDITABLE(ModifiableMetadata.EDITABLE),

        /**
         * The metadata allows missing values to be set, but does not allow existing values to be modified.
         * This state is not appendable, i.e. it does not allow adding elements in a collection.
         */
        COMPLETABLE(ModifiableMetadata.COMPLETABLE),

        /**
         * The metadata is unmodifiable.
         * When a metadata is final, it cannot be moved back to an editable state
         * (but it is still possible to create a modifiable copy with {@link MetadataCopier}).
         * Invoking any setter method on an unmodifiable metadata cause an
         * {@link UnmodifiableMetadataException} to be thrown.
         */
        FINAL(ModifiableMetadata.FINAL);

        /**
         * Mapping from {@link ModifiableMetadata} private flags to {@code State} enumeration.
         * A mapping exists because {@code ModifiableMetadata} does not use the same set of enumeration values
         * (e.g. it has an internal {@link #FREEZING} value), and because future versions may use a bitmask.
         */
        private static final State[] VALUES = new State[ModifiableMetadata.FINAL + 1];
        static {
            VALUES[ModifiableMetadata.EDITABLE]    = EDITABLE;
            VALUES[ModifiableMetadata.STAGED]      = EDITABLE;
            VALUES[ModifiableMetadata.FREEZING]    = FINAL;
            VALUES[ModifiableMetadata.COMPLETABLE] = COMPLETABLE;
            VALUES[ModifiableMetadata.FINAL]       = FINAL;
        }

        /**
         * The numerical code associated to this enumeration value. It serves similar purpose to the
         * {@link #ordinal()} value, but is nevertheless provided for the reasons given in {@link #VALUES}.
         */
        final byte code;

        /**
         * Creates a new state associated to the given code numerical code.
         */
        private State(final byte code) {
            this.code = code;
        }

        /**
         * Whether this enumeration represents a state where data cannot be modified anymore.
         */
        final boolean isUnmodifiable() {
            return code >= ModifiableMetadata.FINAL;
        }
    }

    /**
     * Tells whether this instance of metadata is editable.
     * This is initially {@link State#EDITABLE} for new {@code ModifiableMetadata} instances,
     * but can be changed by a call to {@link #transitionTo(State)}.
     *
     * <p>{@link State#FINAL} implies that all properties are also final.
     * This recursion does not necessarily apply to other states. For example, {@link State#EDITABLE}
     * does <strong>not</strong> imply that all {@code ModifiableMetadata} children are also editable.</p>
     *
     * <div class="note"><b>API note:</b>
     * the {@code ModifiableMetadata} state is not a metadata per se, but rather an information about
     * this particular instance of a metadata class. Two metadata instances may be in different states
     * but still have the same metadata content. For this reason, this method does not have {@code get}
     * prefix for avoiding confusion with getter and setter methods of metadata properties.</div>
     *
     * @return the state (editable, completable or final) of this {@code ModifiableMetadata} instance.
     *
     * @since 1.0
     */
    public State state() {
        return State.VALUES[state];
    }

    /**
     * Requests this metadata instance and (potentially) all its children to transition to a new state.
     * The action performed by this method depends on the {@linkplain #state() source state} and the
     * given target state, as listed in the following table:
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
     *     <td>{@link State#COMPLETABLE}</td>
     *     <td>Marks this metadata and all children as completable.</td>
     *   </tr><tr>
     *     <td>Any</td>
     *     <td>{@link State#FINAL}</td>
     *     <td>Marks this metadata and all children as unmodifiable.</td>
     *   </tr><tr>
     *     <td>{@link State#FINAL}</td>
     *     <td>Any other</td>
     *     <td>Throws {@link UnmodifiableMetadataException}.</td>
     *   </tr>
     * </table>
     *
     * The effect of invoking this method may be recursive. For example, transitioning to {@link State#FINAL}
     * implies transitioning all children {@code ModifiableMetadata} instances to the final state too.
     *
     * @param  target  the desired new state (editable, completable or final).
     * @return {@code true} if the state of this {@code ModifiableMetadata} changed as a result of this method call.
     * @throws UnmodifiableMetadataException if a transition to a less restrictive state
     *         (e.g. from {@link State#FINAL} to {@link State#EDITABLE}) was attempted.
     *
     * @since 1.0
     */
    public boolean transitionTo(final State target) {
        if (target.code < state) {
            throw new UnmodifiableMetadataException(Resources.format(Resources.Keys.UnmodifiableMetadata));
        }
        if (target.code == state || state == FREEZING) {
            return false;
        }
        byte result = state;
        try {
            state = FREEZING;
            StateChanger.applyTo(target, this);
            result = target.code;
        } finally {
            state = result;
        }
        return true;
    }

    /**
     * Copies (if necessary) this metadata and all its children.
     * Changes in the returned metadata will not affect this {@code ModifiableMetadata} instance, and conversely.
     * The returned metadata will be in the {@linkplain #state() state} specified by the {@code target} argument.
     * The state of this {@code ModifiableMetadata} instance stay unchanged.
     *
     * <p>As a special case, this method returns {@code this} if and only if the specified target is {@link State#FINAL}
     * and this {@code ModifiableMetadata} instance is already in final state. In that particular case, copies are not
     * needed for protecting metadata against changes because neither {@code this} or the returned value can be modified.</p>
     *
     * <p>This method is typically invoked for getting a modifiable metadata from an unmodifiable one:</p>
     *
     * {@snippet lang="java" :
     *     Metadata source  = ...;          // Any implementation.
     *     DefaultMetadata md = DefaultMetadata.castOrCopy(source);
     *     md = (DefaultMetadata) md.deepCopy(DefaultMetadata.State.EDITABLE);
     *     }
     *
     * <h4>Alternative</h4>
     * If unconditional copy is desired, or if the metadata to copy may be arbitrary implementations
     * of GeoAPI interfaces (i.e. not necessarily a {@code ModifiableMetadata} subclass),
     * then the following code can be used instead:
     *
     * {@snippet lang="java" :
     *     MetadataCopier copier = new MetadataCopier(MetadataStandard.ISO_19115);
     *     Metadata source = ...;                           // Any implementation.
     *     Metadata copy = copier.copy(Metadata.class, source);
     *     }
     *
     * The {@code Metadata} type in the above example can be replaced by any other ISO 19115 type.
     * Types from other standards can also be used if the {@link MetadataStandard#ISO_19115} constant
     * is replaced accordingly.
     *
     * @param  target  the desired state (editable, completable or final).
     * @return a copy (except in above-cited special case) of this metadata in the specified state.
     *
     * @see MetadataCopier
     * @see org.apache.sis.metadata.iso.DefaultMetadata#deepCopy(Metadata)
     *
     * @since 1.1
     */
    public ModifiableMetadata deepCopy(final State target) {
        final MetadataCopier copier;
        if (target.isUnmodifiable()) {
            if (state >= FINAL) {
                return this;
            }
            copier = MetadataCopier.forModifiable(getStandard());
        } else {
            copier = new MetadataCopier(getStandard());
        }
        final ModifiableMetadata md = (ModifiableMetadata) copier.copyRecursively(getInterface(), this);
        if (target.code > EDITABLE) {
            md.transitionTo(target);
        }
        return md;
    }

    /**
     * Checks if changes in the metadata are allowed. All {@code setFoo(…)} methods in subclasses
     * shall invoke this method (directly or indirectly) before to apply any change.
     * The current property value should be specified in argument.
     *
     * @param  current  the current value, or {@code null} if none.
     * @throws UnmodifiableMetadataException if this metadata is unmodifiable.
     *
     * @see #state()
     *
     * @since 1.0
     */
    protected void checkWritePermission(Object current) throws UnmodifiableMetadataException {
        if (state != COMPLETABLE) {
            if (state == FINAL) {
                throw new UnmodifiableMetadataException(Resources.format(Resources.Keys.UnmodifiableMetadata));
            }
        } else if (current != null) {
            final MetadataStandard standard;
            if (current instanceof AbstractMetadata) {
                standard = ((AbstractMetadata) current).getStandard();
            } else {
                standard = getStandard();
            }
            final Object c = standard.getTitle(current);
            if (c != null) current = c;
            throw new UnmodifiableMetadataException(Resources.format(Resources.Keys.ElementAlreadyInitialized_1, current));
        }
    }

    /**
     * Writes the content of the {@code source} collection into the {@code target} list,
     * creating it if needed. This method performs the following steps:
     *
     * <ul>
     *   <li>Invokes {@link #checkWritePermission(Object)} in order to ensure that this metadata is modifiable.</li>
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
    protected final <E> List<E> writeList(Collection<? extends E> source, List<E> target, Class<E> elementType)
            throws UnmodifiableMetadataException
    {
        return (List<E>) write(source, target, elementType, List.class);
    }

    /**
     * Writes the content of the {@code source} collection into the {@code target} set,
     * creating it if needed. This method performs the following steps:
     *
     * <ul>
     *   <li>Invokes {@link #checkWritePermission(Object)} in order to ensure that this metadata is modifiable.</li>
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
    protected final <E> Set<E> writeSet(Collection<? extends E> source, Set<E> target, Class<E> elementType)
            throws UnmodifiableMetadataException
    {
        return (Set<E>) write(source, target, elementType, Set.class);
    }

    /**
     * Writes the content of the {@code source} collection into the {@code target} list or set,
     * creating it if needed. This method performs the following steps:
     *
     * <ul>
     *   <li>Invokes {@link #checkWritePermission(Object)} in order to ensure that this metadata is modifiable.</li>
     *   <li>If {@code source} is null or empty, returns {@code null}
     *       (meaning that the metadata property is not provided).</li>
     *   <li>If {@code target} is null, creates a new {@link Set} or a new {@link List}
     *       depending on the value returned by {@link #collectionType(Class)}.</li>
     *   <li>Copies the content of the given {@code source} into the target.</li>
     * </ul>
     *
     * <h4>Choosing a collection type</h4>
     * Implementations shall invoke {@link #writeList writeList} or {@link #writeSet writeSet} methods
     * instead of this method when the collection type is enforced by ISO specification.
     * When the type is not enforced by the specification, some freedom are allowed at
     * implementer choice. The default implementation invokes {@link #collectionType(Class)}
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
    protected final <E> Collection<E> writeCollection(Collection<? extends E> source, Collection<E> target, Class<E> elementType)
            throws UnmodifiableMetadataException
    {
        return write(source, target, elementType, null);
    }

    /**
     * Writes the content of the {@code source} collection into the {@code target} list or set,
     * creating it if needed.
     *
     * @param  collectionType  {@code Set.class}, {@code List.class} or null for automatic choice.
     */
    @SuppressWarnings("unchecked")
    private <E> Collection<E> write(final Collection<? extends E> source, Collection<E> target,
            final Class<E> elementType, Class<?> collectionType) throws UnmodifiableMetadataException
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
                 * transitionTo(State.FINAL) is under progress. The source collection is already
                 * an unmodifiable instance created by StateChanger.
                 */
                assert (collectionType != null) || collectionType(elementType).isInstance(source) : elementType;
                return (Collection<E>) source;
            }
            checkWritePermission(valueIfDefined(target));
            if (isNullOrEmpty(source)) {
                target = null;
            } else {
                /*
                 * Reuse the existing collection if available, except in State.COMPLETABLE case
                 * since that collection may the Collection.EMPTY_SET or EMPTY_LIST.
                 */
                if (target != null && state != COMPLETABLE) {
                    target.clear();
                } else {
                    if (collectionType == null) {
                        collectionType = collectionType(elementType);
                    }
                    if (collectionType == Set.class) {
                        target = createSet(elementType, source);
                    } else if (collectionType == List.class) {
                        target = createList(elementType, source);
                    } else {
                        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedType_1, collectionType));
                    }
                }
                target.addAll(source);
                if (state == COMPLETABLE) {
                    if (collectionType == Set.class) {
                        target = CollectionsExt.unmodifiableOrCopy((Set<E>) target);
                    } else {
                        target = CollectionsExt.unmodifiableOrCopy((List<E>) target);
                    }
                }
            }
        }
        return target;
    }

    /**
     * Writes the content of the {@code source} map into the {@code target} map,
     * creating it if needed. This method performs the following steps:
     *
     * <ul>
     *   <li>Invokes {@link #checkWritePermission(Object)} in order to ensure that this metadata is modifiable.</li>
     *   <li>If {@code source} is null or empty, returns {@code null}
     *       (meaning that the metadata property is not provided).</li>
     *   <li>If {@code target} is null, creates a new {@link Map}.</li>
     *   <li>Copies the content of the given {@code source} into the target.</li>
     * </ul>
     *
     * @param  <K>      the type of keys represented by the {@code Class} argument.
     * @param  <V>      the type of values in the map.
     * @param  source   the source map, or {@code null}.
     * @param  target   the target map, or {@code null} if not yet created.
     * @param  keyType  the base type of keys to put in the map.
     * @return a map (possibly the {@code target} instance) containing the {@code source} entries,
     *         or {@code null} if the source was null.
     * @throws UnmodifiableMetadataException if this metadata is unmodifiable.
     *
     * @see #nonNullMap(Map, Class)
     *
     * @since 1.0
     */
    @SuppressWarnings("unchecked")
    protected final <K,V> Map<K,V> writeMap(final Map<? extends K, ? extends V> source, Map<K,V> target,
            Class<K> keyType) throws UnmodifiableMetadataException
    {
        /*
         * Code in this method is a copy of write(Collection, Collection, Class) with some calls inlined.
         * See the comments inside that write(…) method body for more information on the logic.
         */
        if (source != target) {
            if (state == FREEZING) {
                return (Map<K,V>) source;
            }
            checkWritePermission((target == null) || target.isEmpty() ? null : target);
            if (isNullOrEmpty(source)) {
                target = null;
            } else {
                if (target != null && state != COMPLETABLE) {
                    target.clear();
                } else {
                    target = createMap(keyType, source);
                }
                target.putAll(source);
                if (state == COMPLETABLE) {
                    target = CollectionsExt.unmodifiableOrCopy(target);
                }
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
    protected static <E> List<E> copyList(final Collection<? extends E> source, final Class<E> elementType) {
        if (isNullOrEmpty(source)) {
            return null;
        }
        final List<E> target = createList(elementType, source);
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
    protected static <E> Set<E> copySet(final Collection<? extends E> source, final Class<E> elementType) {
        if (isNullOrEmpty(source)) {
            return null;
        }
        final Set<E> target = createSet(elementType, source);
        target.addAll(source);
        return target;
    }

    /**
     * Creates a list or set with the content of the {@code source} collection,
     * or returns {@code null} if the source is {@code null} or empty.
     * This is a convenience method for copying fields in subclass copy constructors.
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  source       the source collection, or {@code null}.
     * @param  elementType  the base type of elements to put in the collection.
     * @return a collection containing the {@code source} elements,
     *         or {@code null} if the source was null or empty.
     */
    protected static <E> Collection<E> copyCollection(final Collection<? extends E> source, final Class<E> elementType) {
        if (isNullOrEmpty(source)) {
            return null;
        }
        final Collection<E> target;
        if (useSet(elementType)) {
            target = createSet(elementType, source);
        } else {
            target = createList(elementType, source);
        }
        target.addAll(source);
        return target;
    }

    /**
     * Creates a map with the content of the {@code source} map,
     * or returns {@code null} if the source is {@code null} or empty.
     * This is a convenience method for copying fields in subclass copy constructors.
     *
     * @param  <K>      the type of keys represented by the {@code Class} argument.
     * @param  <V>      the type of values in the map.
     * @param  source   the source map, or {@code null}.
     * @param  keyType  the base type of keys to put in the map.
     * @return a map containing the {@code source} entries,
     *         or {@code null} if the source was null or empty.
     *
     * @since 1.0
     */
    protected static <K,V> Map<K,V> copyMap(final Map<? extends K, ? extends V> source, final Class<K> keyType) {
        if (isNullOrEmpty(source)) {
            return null;
        }
        final Map<K,V> target = createMap(keyType, source);
        target.putAll(source);
        return target;
    }

    /**
     * Creates a singleton list or set containing only the given value, if non-null.
     * This is a convenience method for initializing fields in subclass constructors.
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  value        the singleton value to put in the returned collection, or {@code null}.
     * @param  elementType  the element type (used only if {@code value} is non-null).
     * @return a new modifiable collection containing the given value,
     *         or {@code null} if the given value was null.
     */
    protected static <E> Collection<E> singleton(final E value, final Class<E> elementType) {
        if (value == null) {
            return null;
        }
        final Collection<E> collection;
        if (useSet(elementType)) {
            collection = createSet(elementType, null);
        } else {
            collection = new CheckedArrayList<>(elementType, 1);
        }
        collection.add(value);
        return collection;
    }

    /**
     * Returns {@code true} if empty collection should be returned as {@code null} value.
     * This is usually not a behavior that we allow in public API. However, this behavior
     * is sometimes desired internally, for example when marshalling with JAXB or when
     * performing a {@code equals}, {@code isEmpty} or {@code prune} operation
     * (for avoiding creating unnecessary collections).
     */
    private static boolean emptyCollectionAsNull() {
        return Semaphores.query(Semaphores.NULL_COLLECTION);
    }

    /**
     * Returns the specified list, or a new one if {@code current} is null.
     * This is a convenience method for implementation of {@code getFoo()} methods.
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  current      the existing list, or {@code null} if the list has not yet been created.
     * @param  elementType  the element type (used only if {@code current} is null).
     * @return {@code current}, or a new list if {@code current} is null.
     */
    protected final <E> List<E> nonNullList(final List<E> current, final Class<E> elementType) {
        if (current != null) {
            return current.isEmpty() && emptyCollectionAsNull() ? null : current;
        }
        if (emptyCollectionAsNull()) {
            return null;
        }
        if (state < FREEZING) {
            return createList(elementType, current);        // `current` given as a matter of principle even if null.
        }
        return Collections.emptyList();
    }

    /**
     * Returns the specified set, or a new one if {@code current} is null.
     * This is a convenience method for implementation of {@code getFoo()} methods.
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  current      the existing set, or {@code null} if the set has not yet been created.
     * @param  elementType  the element type (used only if {@code current} is null).
     * @return {@code current}, or a new set if {@code current} is null.
     */
    protected final <E> Set<E> nonNullSet(final Set<E> current, final Class<E> elementType) {
        if (current != null) {
            return current.isEmpty() && emptyCollectionAsNull() ? null : current;
        }
        if (emptyCollectionAsNull()) {
            return null;
        }
        if (state < FREEZING) {
            return createSet(elementType, current);     // `current` given as a matter of principle even if null.
        }
        return Collections.emptySet();
    }

    /**
     * Returns the specified collection, or a new one if {@code current} is null.
     * This is a convenience method for implementation of {@code getFoo()} methods.
     *
     * <h4>Choosing a collection type</h4>
     * Implementations shall invoke {@link #nonNullList nonNullList(…)} or {@link #nonNullSet nonNullSet(…)}
     * instead of this method when the collection type is enforced by ISO specification.
     * When the type is not enforced by the specification, some freedom are allowed at implementer choice.
     * The default implementation invokes {@link #collectionType(Class)} in order to get a hint about whether
     * a {@link List} or a {@link Set} should be used.
     *
     * @param  <E>          the type represented by the {@code Class} argument.
     * @param  current      the existing collection, or {@code null} if the collection has not yet been created.
     * @param  elementType  the element type (used only if {@code current} is null).
     * @return {@code current}, or a new collection if {@code current} is null.
     */
    protected final <E> Collection<E> nonNullCollection(final Collection<E> current, final Class<E> elementType) {
        if (current != null) {
            assert collectionType(elementType).isInstance(current);
            return current.isEmpty() && emptyCollectionAsNull() ? null : current;
        }
        if (emptyCollectionAsNull()) {
            return null;
        }
        final boolean isModifiable = (state < FREEZING);
        final Class<?> collectionType = collectionType(elementType);
        if (collectionType == Set.class) {
            if (isModifiable) {
                return createSet(elementType, current);         // `current` given as a matter of principle even if null.
            } else {
                return Collections.emptySet();
            }
        } else if (collectionType == List.class) {
            if (isModifiable) {
                return createList(elementType, current);        // `current` given as a matter of principle even if null.
            } else {
                return Collections.emptyList();
            }
        } else {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedType_1, collectionType));
        }
    }

    /**
     * Returns the specified map, or a new one if {@code current} is null.
     * This is a convenience method for implementation of {@code getFoo()} methods.
     *
     * @param  <K>      the type of keys represented by the {@code Class} argument.
     * @param  <V>      the type of values in the map.
     * @param  current  the existing map, or {@code null} if the map has not yet been created.
     * @param  keyType  the key type (used only if {@code current} is null).
     * @return {@code current}, or a new map if {@code current} is null.
     *
     * @since 1.0
     */
    protected final <K,V> Map<K,V> nonNullMap(final Map<K,V> current, final Class<K> keyType) {
        if (current != null) {
            return current.isEmpty() && emptyCollectionAsNull() ? null : current;
        }
        if (emptyCollectionAsNull()) {
            return null;
        }
        if (state < FREEZING) {
            return createMap(keyType, current);         // `current` given as a matter of principle even if null.
        }
        return Collections.emptyMap();
    }

    /**
     * Creates a modifiable list for elements of the given type. This method is defined mostly
     * for consistency with {@link #createSet(Class, Collection)}.
     *
     * @param  source  the collection to be copied in the new list. This method uses this information
     *                 only for computing initial capacity; it does not perform the actual copy.
     */
    private static <E> List<E> createList(final Class<E> elementType, final Collection<?> source) {
        if (source == null) {
            /*
             * Do not specify an initial capacity, because the list will stay empty in a majority of cases
             * (i.e. the users will want to iterate over the list elements more often than they will want
             * to add elements). JDK implementation of ArrayList has a lazy instantiation mechanism for
             * initially empty lists, but as of JDK 10 this lazy instantiation works only for list having
             * the default capacity.
             */
            return new CheckedArrayList<>(elementType);
        }
        return new CheckedArrayList<>(elementType, source.size());
    }

    /**
     * Creates a modifiable set for elements of the given type. This method will create an {@link EnumSet},
     * {@link CodeListSet} or {@link java.util.LinkedHashSet} depending on the {@code elementType} argument.
     * The set must have a stable iteration order (this is needed by {@link TreeTableView}).
     *
     * @param  source  the collection to be copied in the new set, or {@code null} if unknown.
     *                 This method uses this information only for computing initial capacity;
     *                 it does not perform the actual copy.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static <E> Set<E> createSet(final Class<E> elementType, final Collection<?> source) {
        if (Enum.class.isAssignableFrom(elementType)) {
            return EnumSet.noneOf((Class) elementType);
        }
        if (CodeList.class.isAssignableFrom(elementType) && Modifier.isFinal(elementType.getModifiers())) {
            return new CodeListSet(elementType);
        }
        /*
         * If we cannot compute an initial capacity from the size of an existing source, use an arbitrary
         * small value (currently 4). We use a small value because collections will typically contain few
         * elements (often just a singleton).
         */
        return new CheckedHashSet<>(elementType, (source != null) ? Containers.hashMapCapacity(source.size()) : 4);
    }

    /**
     * Creates a modifiable map for elements of the given type.
     * The map must have a stable iteration order (this is needed by {@link TreeTableView}).
     *
     * @param  source  the map to be copied in the new map. This method uses this information
     *                 only for computing initial capacity; it does not perform the actual copy.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static <K,V> Map<K,V> createMap(final Class<K> keyType, final Map<?,?> source) {
        if (Enum.class.isAssignableFrom(keyType)) {
            return new EnumMap(keyType);
        } else {
            // Must be LinkedHashMap, not HashMap, because TreeTableView needs stable iteration order.
            return (source != null) ? JDK19.newLinkedHashMap(source.size()) : new LinkedHashMap<>(4);
        }
    }

    /**
     * Returns {@code true} if we should use a {@link Set} instead of a {@link List}
     * for elements of the given type.
     */
    private static boolean useSet(final Class<?> elementType) {
        return CodeList.class.isAssignableFrom(elementType)
                ||          Enum.class.isAssignableFrom(elementType)
                ||       Charset.class.isAssignableFrom(elementType)
                ||        String.class ==               elementType
                ||        Locale.class ==               elementType
                ||      Currency.class ==               elementType;
    }

    /**
     * Returns the type of collection to use for the given type. The current implementation can
     * return only two values: <code>{@linkplain Set}.class</code> if the property should not
     * accept duplicated values, or <code>{@linkplain List}.class</code> otherwise.
     * Future SIS versions may accept other types.
     *
     * <p>The default implementation returns <code>{@linkplain Set}.class</code> if the element
     * type is assignable to {@link CodeList}, {@link Enum}, {@link String}, {@link Charset},
     * {@link Locale} or {@link Currency}, and <code>{@linkplain List}.class</code> otherwise.
     * Subclasses can override this method for choosing different kind of collections.
     * As a general rule, implementations should return the {@link Set} type
     * only when the set elements are immutable.
     * This is needed for {@linkplain Object#hashCode() hash code} stability.</p>
     *
     * @param  <E>          the type of elements in the collection to be created.
     * @param  elementType  the type of elements in the collection to be created.
     * @return {@code List.class} or {@code Set.class} depending on whether the
     *         property shall accept duplicated values or not.
     *
     * @deprecated This method will be removed because it can cause {@code this} to escape at construction time.
     */
    @Deprecated(since="1.5", forRemoval=true)
    @SuppressWarnings({"rawtypes","unchecked"})
    protected <E> Class<? extends Collection<E>> collectionType(final Class<E> elementType) {
        return (Class) (useSet(elementType) ? Set.class : List.class);
    }
}
