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
package org.apache.sis.internal.jaxb;

import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.AbstractSet;
import java.util.AbstractMap;
import java.util.NoSuchElementException;
import java.io.Serializable;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;

import static org.apache.sis.util.collection.CollectionsExt.hashMapCapacity;

// Related to JDK7
import org.apache.sis.internal.jdk7.Objects;


/**
 * A map of identifiers which can be used as a helper class for
 * {@link org.apache.sis.xml.IdentifiedObject} implementations.
 *
 * <p>This class works as a wrapper around a collection of identifiers. Because all operations
 * are performed by an iteration over the collection elements, this implementation is suitable
 * only for small maps (less than 10 elements). Given that objects typically have only one or
 * two identifiers, this is considered acceptable.</p>
 *
 * {@section Handling of duplicated authorities}
 * The collection shall not contain more than one identifier for the same
 * {@linkplain Identifier#getAuthority() authority}. However duplications may happen if the user
 * has direct access to the list, for example through {@link Citation#getIdentifiers()}. If such
 * duplication is found, then this map implementation applies the following rules:
 *
 * <ul>
 *   <li>All getter methods (including the iterators and the values returned by the {@code put}
 *       and {@code remove} methods) return only the identifier code associated to the first
 *       occurrence of each authority. Any subsequent occurrences of the same authorities are
 *       silently ignored.</li>
 *   <li>All setter methods <em>may</em> affect <em>all</em> identifiers previously associated to
 *       the given authority, not just the first occurrence. The only guarantee is that the list
 *       is update in such a way that the effect of setter methods are visible to subsequent calls
 *       to getter methods.</li>
 * </ul>
 *
 * {@section Handling of null identifiers}
 * The collection of identifiers shall not contains any null element. This is normally ensured by
 * the {@link org.apache.sis.metadata.ModifiableMetadata} internal collection implementations.
 * This class performs opportunist null checks as an additional safety. However because we perform
 * those checks only in opportunist ways, the following inconsistencies remain:
 *
 * <ul>
 *   <li>{@link #isEmpty()} may return {@code false} when the more accurate {@link #size()}
 *       method returns 0.</li>
 * </ul>
 *
 * {@section Thread safety}
 * This class is thread safe if the underlying identifier collection is thread safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.18)
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.xml.IdentifiedObject
 */
public class IdentifierMapAdapter extends AbstractMap<Citation,String> implements IdentifierMap, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1445849218952061605L;

    /**
     * An immutable empty instance.
     */
    public static final IdentifierMap EMPTY = new IdentifierMapAdapter(Collections.<Identifier>emptySet());

    /**
     * The identifiers to wrap in a map view.
     */
    public final Collection<Identifier> identifiers;

    /**
     * A view over the entries, created only when first needed.
     */
    private transient Set<Entry<Citation,String>> entries;

    /**
     * Creates a new map which will be a view over the given identifiers.
     *
     * @param identifiers The identifiers to wrap in a map view.
     */
    IdentifierMapAdapter(final Collection<Identifier> identifiers) {
        this.identifiers = identifiers;
    }

    /**
     * Removes every entries in the underlying collection.
     *
     * @throws UnsupportedOperationException If the collection of identifiers is unmodifiable.
     */
    @Override
    public void clear() throws UnsupportedOperationException {
        identifiers.clear();
    }

    /**
     * Returns {@code true} if the collection of identifiers contains at least one element.
     * This method does not verify if the collection contains null element (it should not).
     * Consequently, this method may return {@code false} even if the {@link #size()} method
     * returns 0.
     */
    @Override
    public boolean isEmpty() {
        return identifiers.isEmpty();
    }

    /**
     * Returns {@code true} if at least one identifier declares the given
     * {@linkplain Identifier#getCode() code}.
     *
     * @param  code The code to search, which should be an instance of {@link String}.
     * @return {@code true} if at least one identifier uses the given code.
     */
    @Override
    public boolean containsValue(final Object code) {
        if (code instanceof String) {
            for (final Identifier identifier : identifiers) {
                if (identifier != null && code.equals(identifier.getCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if at least one identifier declares the given
     * {@linkplain Identifier#getAuthority() authority}.
     *
     * @param  authority The authority to search, which should be an instance of {@link Citation}.
     * @return {@code true} if at least one identifier uses the given authority.
     */
    @Override
    public boolean containsKey(final Object authority) {
        return (authority instanceof Citation) && getIdentifier((Citation) authority) != null;
    }

    /**
     * Returns the identifier for the given key, or {@code null} if none.
     */
    private Identifier getIdentifier(final Citation authority) {
        for (final Identifier identifier : identifiers) {
            if (identifier != null && Objects.equals(authority, identifier.getAuthority())) {
                return identifier;
            }
        }
        return null;
    }

    /**
     * Returns the identifier associated with the given authority,
     * or {@code null} if no specialized identifier was found.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getSpecialized(final IdentifierSpace<T> authority) {
        final Identifier identifier = getIdentifier(authority);
        return (identifier instanceof SpecializedIdentifier<?>) ? ((SpecializedIdentifier<T>) identifier).value : null;
    }

    /**
     * Returns the code of the first identifier associated with the given authority only if
     * if is <strong>not</strong> a specialized identifier. Otherwise returns {@code null}.
     *
     * <p>This is a helper method for {@link IdentifierMapWithSpecialCases#put(Citation, String)},
     * in order to be able to return the old value if that value was a {@link String} rather than
     * the specialized type. We do not return the string for the specialized case in order to avoid
     * the cost of invoking {@code toString()} on the specialized object (some may be costly). Such
     * call would be useless because {@code IdentifierMapWithSpecialCase} discard the value of this
     * method when it found a specialized type.</p>
     */
    final String getUnspecialized(final Citation authority) {
        final Identifier identifier = getIdentifier(authority);
        if (identifier != null && !(identifier instanceof SpecializedIdentifier<?>)) {
            return identifier.getCode();
        }
        return null;
    }

    /**
     * Returns the code of the first identifier associated with the given
     * {@linkplain Identifier#getAuthority() authority}, or {@code null}
     * if no identifier was found.
     *
     * @param  authority The authority to search, which should be an instance of {@link Citation}.
     * @return The code of the identifier for the given authority, or {@code null} if none.
     */
    @Override
    public String get(final Object authority) {
        if (authority instanceof Citation) {
            final Identifier identifier = getIdentifier((Citation) authority);
            if (identifier != null) {
                return identifier.getCode();
            }
        }
        return null;
    }

    /**
     * Removes all identifiers associated with the given {@linkplain Identifier#getAuthority() authority}.
     * The default implementation delegates to {@link #put(Citation, String)} with a {@code null} value.
     *
     * @param  authority The authority to search, which should be an instance of {@link Citation}.
     * @return The code of the identifier for the given authority, or {@code null} if none.
     */
    @Override
    public String remove(final Object authority) {
        return (authority instanceof Citation) ? put((Citation) authority, null) : null;
    }

    /**
     * Sets the code of the identifier having the given authority to the given value.
     * If no identifier is found for the given authority, a new one is created. If
     * more than one identifier is found for the given authority, then all previous
     * identifiers may be removed in order to ensure that the new entry will be the
     * first entry, so it can be find by the {@code get} method.
     *
     * @param  authority The authority for which to set the code.
     * @param  code The new code for the given authority, or {@code null} for removing the entry.
     * @return The previous code for the given authority, or {@code null} if none.
     */
    @Override
    public String put(final Citation authority, final String code)
            throws UnsupportedOperationException
    {
        ArgumentChecks.ensureNonNull("authority", authority);
        String old = null;
        final Iterator<? extends Identifier> it = identifiers.iterator();
        while (it.hasNext()) {
            final Identifier identifier = it.next();
            if (identifier == null) {
                it.remove(); // Opportunist cleaning, but should not happen.
            } else if (Objects.equals(authority, identifier.getAuthority())) {
                if (code != null && identifier instanceof IdentifierMapEntry) {
                    return ((IdentifierMapEntry) identifier).setValue(code);
                    // No need to suppress other occurrences of the key (if any)
                    // because we made a replacement in the first entry, so the
                    // new value will be visible by the getter methods.
                }
                if (old == null) {
                    old = identifier.getCode();
                }
                it.remove();
                // Continue the iteration in order to remove all other occurrences,
                // in order to ensure that the getter methods will see the new value.
            }
        }
        if (code != null) {
            identifiers.add(SpecializedIdentifier.parse(authority, code));
        }
        return old;
    }

    /**
     * Sets the identifier associated with the given authority, and returns the previous value.
     */
    @Override
    public <T> T putSpecialized(final IdentifierSpace<T> authority, final T value)
            throws UnsupportedOperationException
    {
        ArgumentChecks.ensureNonNull("authority", authority);
        T old = null;
        final Iterator<? extends Identifier> it = identifiers.iterator();
        while (it.hasNext()) {
            final Identifier identifier = it.next();
            if (identifier == null) {
                it.remove(); // Opportunist cleaning, but should not happen.
            } else if (Objects.equals(authority, identifier.getAuthority())) {
                if (identifier instanceof SpecializedIdentifier<?>) {
                    @SuppressWarnings("unchecked")
                    final SpecializedIdentifier<T> id = (SpecializedIdentifier<T>) identifier;
                    if (old == null) {
                        old = id.value;
                    }
                    if (value != null) {
                        id.value = value;
                        return old;
                        // No need to suppress other occurrences of the key (if any)
                        // because we made a replacement in the first entry, so the
                        // new value will be visible by the getter methods.
                    }
                }
                it.remove();
                // Continue the iteration in order to remove all other occurrences,
                // in order to ensure that the getter methods will see the new value.
            }
        }
        if (value != null) {
            identifiers.add(new SpecializedIdentifier<T>(authority, value));
        }
        return old;
    }

    /**
     * Returns a view over the collection of identifiers. This view supports removal operation
     * if the underlying collection of identifiers supports the {@link Iterator#remove()} method.
     *
     * @return A view over the collection of identifiers.
     */
    @Override
    public synchronized Set<Entry<Citation,String>> entrySet() {
        if (entries == null) {
            entries = new Entries(identifiers);
        }
        return entries;
    }

    /**
     * The view returned by {@link IdentifierMap#entrySet()}. If the backing identifier
     * collection contains null entries, those entries will be ignored. If the backing
     * collection contains many entries for the same authority, then only the first
     * occurrence is retained.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3 (derived from geotk-3.18)
     * @version 0.3
     * @module
     */
    private static final class Entries extends AbstractSet<Entry<Citation,String>> {
        /**
         * The identifiers to wrap in a set of entries view. This is a reference
         * to the same collection than {@link IdentifierMap#identifiers}.
         */
        private final Collection<? extends Identifier> identifiers;

        /**
         * Creates a new view over the collection of identifiers.
         *
         * @param identifiers The identifiers to wrap in a set of entries view.
         */
        Entries(final Collection<? extends Identifier> identifiers) {
            this.identifiers = identifiers;
        }

        /**
         * Same implementation than {@link IdentifierMapAdapter#clear()}.
         */
        @Override
        public void clear() throws UnsupportedOperationException {
            identifiers.clear();
        }

        /**
         * Same implementation than {@link IdentifierMapAdapter#isEmpty()}.
         */
        @Override
        public boolean isEmpty() {
            return identifiers.isEmpty();
        }

        /**
         * Counts the number of entries, ignoring null elements and duplicated authorities.
         * Because {@code null} elements are ignored, this method may return 0 even if
         * {@link #isEmpty()} returns {@code false}.
         */
        @Override
        public int size() {
            final HashMap<Citation,Boolean> done = new HashMap<Citation,Boolean>(hashMapCapacity(identifiers.size()));
            for (final Identifier identifier : identifiers) {
                if (identifier != null) {
                    done.put(identifier.getAuthority(), null);
                }
            }
            return done.size();
        }

        /**
         * Returns an iterator over the (<var>citation</var>, <var>code</var>) entries.
         */
        @Override
        public Iterator<Entry<Citation, String>> iterator() {
            return new Iter(identifiers);
        }
    }

    /**
     * The iterator over the (<var>citation</var>, <var>code</var>) entries. This iterator is
     * created by the {@link IdentifierMap.Entries} collection. It extends {@link HashMap} as
     * an opportunist implementation strategy, but users don't need to know this detail.
     *
     * <p>This iterator supports the {@link #remove()} operation if the underlying collection
     * supports it.</p>
     *
     * <p>The map entries are used as a safety against duplicated authority values. The map values
     * are non-null only after we iterated over an authority. Then the value is {@link Boolean#TRUE}
     * if the identifier has been removed, of {@code Boolean#FALSE} otherwise.</p>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3 (derived from geotk-3.18)
     * @version 0.3
     * @module
     */
    @SuppressWarnings("serial") // Not intended to be serialized.
    private static final class Iter extends HashMap<Citation,Boolean> implements Iterator<Entry<Citation,String>> {
        /**
         * An iterator over the {@link IdentifierMap#identifiers} collection,
         * or (@code null} if we have reached the iteration end.
         */
        private Iterator<? extends Identifier> identifiers;

        /**
         * The next entry to be returned by {@link #next()}, or {@code null} if not yet computed.
         * This field will be computed only when {@link #next()} or {@link #hasNext()} is invoked.
         */
        private transient Entry<Citation,String> next;

        /**
         * The current authority. Used only for removal operations.
         */
        private transient Citation authority;

        /**
         * Creates a new iterator for the given collection of identifiers.
         */
        Iter(final Collection<? extends Identifier> identifiers) {
            super(hashMapCapacity(identifiers.size()));
            this.identifiers = identifiers.iterator();
        }

        /**
         * Advances to the next non-null identifier, skips duplicated authorities, wraps the
         * identifier in an entry if needed and stores the result in the {@link #next} field.
         * If we reach the iteration end, then this method set the {@link #identifiers}
         * iterator to {@code null}.
         */
        private void toNext() {
            final Iterator<? extends Identifier> it = identifiers;
            if (it != null) {
                while (it.hasNext()) {
                    final Identifier identifier = it.next();
                    if (identifier != null) {
                        final Citation authority = identifier.getAuthority();
                        final Boolean state = put(authority, Boolean.FALSE);
                        if (state == null) {
                            if (identifier instanceof IdentifierMapEntry) {
                                next = (IdentifierMapEntry) identifier;
                            } else {
                                next = new IdentifierMapEntry.Immutable(authority, identifier.getCode());
                            }
                            this.authority = authority;
                            return;
                        }
                        if (state) {
                            // Found a duplicated entry, and user asked for the
                            // removal of that authority.
                            it.remove();
                        }
                    }
                }
                identifiers = null;
            }
        }

        /**
         * If we need to search for the next element, fetches it now.
         * Then returns {@code true} if we didn't reached the iteration end.
         */
        @Override
        public boolean hasNext() {
            if (next == null) {
                toNext();
            }
            return identifiers != null;
        }

        /**
         * If we need to search for the next element, searches it now. Then set {@link #next}
         * to {@code null} as a flag meaning that next invocations will need to search again
         * for an element, and returns the element that we got.
         */
        @Override
        public Entry<Citation,String> next() throws NoSuchElementException {
            Entry<Citation,String> entry = next;
            if (entry == null) {
                toNext();
                entry = next;
            }
            next = null;
            if (identifiers == null) {
                throw new NoSuchElementException();
            }
            return entry;
        }

        /**
         * Removes the last element returned by {@link #next()}. Note that if the {@link #next}
         * field is non-null, that would mean that the iteration has moved since the last call
         * to the {@link #next()} method, in which case the iterator is invalid.
         */
        @Override
        public void remove() throws IllegalStateException {
            final Iterator<? extends Identifier> it = identifiers;
            if (it == null || next != null) {
                throw new IllegalStateException();
            }
            it.remove();
            put(authority, Boolean.TRUE);
        }
    }

    /**
     * Overrides the string representation in order to use only the authority title as keys.
     * We do that because the string representations of {@code DefaultCitation} objects are
     * very big.
     *
     * <p>String examples:</p>
     * <ul>
     *   <li>{gml:id=“myID”}</li>
     *   <li>{gco:uuid=“42924124-032a-4dfe-b06e-113e3cb81cf0”}</li>
     *   <li>{xlink:href=“http://www.mydomain.org/myHREF”}</li>
     * </ul>
     *
     * @see SpecializedIdentifier#toString()
     */
    @Override
    public String toString() {
	final StringBuilder buffer = new StringBuilder(50).append('{');
	for (final Entry<Citation,String> entry : entrySet()) {
	    if (buffer.length() != 1) {
                buffer.append(", ");
            }
            SpecializedIdentifier.format(buffer, entry.getKey(), entry.getValue());
	}
        return buffer.append('}').toString();
    }
}
