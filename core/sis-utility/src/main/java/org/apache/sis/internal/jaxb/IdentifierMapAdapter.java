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

import java.net.URI;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.AbstractMap;
import java.util.NoSuchElementException;
import java.io.Serializable;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.Debug;
import org.apache.sis.xml.XLink;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.internal.util.SetOfUnknownSize;

import static org.apache.sis.util.collection.Containers.hashMapCapacity;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Implementation of the map of identifiers associated to {@link org.apache.sis.xml.IdentifiedObject} instances.
 * This base class implements an unmodifiable map, but the {@link ModifiableIdentifierMap} subclass add write
 * capabilities.
 *
 * <p>This class works as a wrapper around a collection of identifiers. Because all operations
 * are performed by an iteration over the collection elements, this implementation is suitable
 * only for small maps (less than 10 elements). Given that objects typically have only one or
 * two identifiers, this is considered acceptable.</p>
 *
 * <div class="section">Special cases</div>
 * The identifiers for the following authorities are handled in a special way:
 * <ul>
 *   <li>{@link IdentifierSpace#HREF}: handled as a shortcut to {@link XLink#getHRef()}.</li>
 * </ul>
 *
 * <div class="section">Handling of duplicated authorities</div>
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
 * <div class="section">Handling of null identifiers</div>
 * The collection of identifiers shall not contain any null element. This is normally ensured by
 * the {@link org.apache.sis.metadata.ModifiableMetadata} internal collection implementations.
 * This class performs opportunist null checks as an additional safety, but consistency is not
 * guaranteed. See {@link #size()} for more information.
 *
 * <div class="section">Thread safety</div>
 * This class is thread safe if the underlying identifier collection is thread safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
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
     * Creates a new map which will be a view over the given identifiers.
     *
     * @param identifiers The identifiers to wrap in a map view.
     */
    public IdentifierMapAdapter(final Collection<Identifier> identifiers) {
        this.identifiers = identifiers;
    }

    /**
     * If the given authority is a special case, returns its {@link NonMarshalledAuthority} integer enum.
     * Otherwise returns -1. See javadoc for more information about special cases.
     *
     * @param authority A {@link Citation} constant. The type is relaxed to {@code Object}
     *        because the signature of some {@code Map} methods are that way.
     */
    static int specialCase(final Object authority) {
        if (authority == IdentifierSpace.HREF) return NonMarshalledAuthority.HREF;
        // A future Apache SIS version may add more special cases here.
        return -1;
    }

    /**
     * Extracts the {@code xlink:href} value from the {@link XLink} if presents.
     * This method does not test if an explicit {@code xlink:href} identifier exists;
     * this check must be done by the caller <strong>before</strong> to invoke this method.
     *
     * @see ModifiableIdentifierMap#setHRef(URI)
     */
    private URI getHRef() {
        final Identifier identifier = getIdentifier(IdentifierSpace.XLINK);
        if (identifier instanceof SpecializedIdentifier<?>) {
            final Object link = ((SpecializedIdentifier<?>) identifier).value;
            if (link instanceof XLink) {
                return ((XLink) link).getHRef();
            }
        }
        return null;
    }

    /**
     * Returns the string representation of the given value, or {@code null} if none.
     *
     * @param value The value returned be one of the above {@code getFoo()} methods.
     */
    private static String toString(final Object value) {
        return (value != null) ? value.toString() : null;
    }




    ////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                        ////////
    ////////    END OF SPECIAL CASES.                                               ////////
    ////////                                                                        ////////
    ////////    Implementation of IdentifierMap methods follow. Each method may     ////////
    ////////    have a switch statement over the special cases declared above.      ////////
    ////////                                                                        ////////
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Whether this map support {@code put} and {@code remove} operations.
     */
    boolean isModifiable() {
        return false;
    }

    /**
     * Returns {@code true} if the collection of identifiers contains at least one element.
     * This method does not verify if the collection contains null element (it should not).
     */
    @Override
    public final boolean isEmpty() {
        return identifiers.isEmpty();
    }

    /**
     * Counts the number of entries, ignoring null elements and duplicated authorities.
     *
     * <p>Because {@code null} elements are ignored, this method may return 0 even if {@link #isEmpty()}
     * returns {@code false}. However this inconsistency should not happen in practice because
     * {@link org.apache.sis.metadata.ModifiableMetadata} internal collection implementations
     * do not allow null values.</p>
     */
    @Override
    public final int size() {
        final HashSet<Citation> done = new HashSet<Citation>(hashMapCapacity(identifiers.size()));
        for (final Identifier identifier : identifiers) {
            if (identifier != null) {
                done.add(identifier.getAuthority());
            }
        }
        return done.size();
    }

    /**
     * Returns {@code true} if at least one identifier declares the given {@linkplain Identifier#getCode() code}.
     *
     * @param  code The code to search, which should be an instance of {@link String}.
     * @return {@code true} if at least one identifier uses the given code.
     */
    @Override
    public final boolean containsValue(final Object code) {
        if (code instanceof String) {
            for (final Identifier identifier : identifiers) {
                if (identifier != null && code.equals(identifier.getCode())) {
                    return true;
                }
            }
            return code.equals(toString(getHRef()));
            // A future Apache SIS version may add more special cases here.
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
    public final boolean containsKey(final Object authority) {
        if (authority instanceof Citation) {
            if (getIdentifier((Citation) authority) != null) {
                return true;
            }
            switch (specialCase(authority)) {
                case NonMarshalledAuthority.HREF: return getHRef() != null;
                // A future Apache SIS version may add more special cases here.
            }
        }
        return false;
    }

    /**
     * Returns the identifier for the given key, or {@code null} if none.
     */
    final Identifier getIdentifier(final Citation authority) {
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
    public final <T> T getSpecialized(final IdentifierSpace<T> authority) {
        final Identifier identifier = getIdentifier(authority);
        if (identifier instanceof SpecializedIdentifier<?>) {
            return ((SpecializedIdentifier<T>) identifier).value;
        }
        switch (specialCase(authority)) {
            case NonMarshalledAuthority.HREF: return (T) getHRef();
            // A future Apache SIS version may add more special cases here.
        }
        return null;
    }

    /**
     * Returns the code of the first identifier associated with the given
     * {@linkplain Identifier#getAuthority() authority}, or {@code null} if no identifier was found.
     *
     * @param  authority The authority to search, which should be an instance of {@link Citation}.
     * @return The code of the identifier for the given authority, or {@code null} if none.
     */
    @Override
    public final String get(final Object authority) {
        if (authority instanceof Citation) {
            final Identifier identifier = getIdentifier((Citation) authority);
            if (identifier != null) {
                return identifier.getCode();
            }
            switch (specialCase(authority)) {
                case NonMarshalledAuthority.HREF: return toString(getHRef());
                // A future Apache SIS version may add more special cases here.
            }
        }
        return null;
    }

    /**
     * Removes all identifiers associated with the given {@linkplain Identifier#getAuthority() authority}.
     *
     * @param  authority The authority to search, which should be an instance of {@link Citation}.
     * @return The code of the identifier for the given authority, or {@code null} if none.
     * @throws UnsupportedOperationException if the collection of identifiers is unmodifiable.
     */
    @Override
    public String remove(Object authority) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes every entries in the underlying collection.
     *
     * @throws UnsupportedOperationException if the collection of identifiers is unmodifiable.
     */
    @Override
    public void clear() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the code of the identifier having the given authority to the given value.
     * If no identifier is found for the given authority, a new one is created.
     * If more than one identifier is found for the given authority, then all previous identifiers may be removed
     * in order to ensure that the new entry will be the first entry, so it can be find by the {@code get} method.
     *
     * @param  authority The authority for which to set the code.
     * @param  code The new code for the given authority, or {@code null} for removing the entry.
     * @return The previous code for the given authority, or {@code null} if none.
     * @throws UnsupportedOperationException if the collection of identifiers is unmodifiable.
     */
    @Override
    public String put(Citation authority, String code) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the identifier associated with the given authority, and returns the previous value.
     */
    @Override
    public <T> T putSpecialized(IdentifierSpace<T> authority, T value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a view over the collection of identifiers. This view supports removal operation
     * if the underlying collection of identifiers supports the {@link Iterator#remove()} method.
     *
     * <p>If the backing identifier collection contains null entries, those entries will be ignored.
     * If the backing collection contains many entries for the same authority, then only the first
     * occurrence is included.</p>
     *
     * @return A view over the collection of identifiers.
     */
    @Override
    public Set<Entry<Citation,String>> entrySet() {
        /*
         * Do not cache the entries set because if is very cheap to create and not needed very often.
         * Not caching allows this implementation to be thread-safe without synchronization or volatile
         * fields if the underlying list is thread-safe. Furthermore, IdentifierMapAdapter are temporary
         * objects anyway in the current ISOMetadata implementation.
         */
        return new SetOfUnknownSize<Entry<Citation,String>>() {
            /** Delegates to the enclosing class. */
            @Override public void clear() throws UnsupportedOperationException {
                IdentifierMapAdapter.this.clear();
            }

            /** Delegates to the enclosing class. */
            @Override public boolean isEmpty() {
                return IdentifierMapAdapter.this.isEmpty();
            }

            /** Delegates to the enclosing class. */
            @Override public int size() {
                return IdentifierMapAdapter.this.size();
            }

            /** Returns an iterator over the (<var>citation</var>, <var>code</var>) entries. */
            @Override public Iterator<Entry<Citation, String>> iterator() {
                return new Iter(identifiers, isModifiable());
            }
        };
    }

    /**
     * The iterator over the (<var>citation</var>, <var>code</var>) entries. This iterator is created by
     * the {@link IdentifierMapAdapter.Entries} collection. It extends {@link HashMap} as an opportunist
     * implementation strategy, but users does not need to know this detail.
     *
     * <p>This iterator supports the {@link #remove()} operation if the underlying collection supports it.</p>
     *
     * <p>The map entries are used as a safety against duplicated authority values. The map values
     * are non-null only after we iterated over an authority. Then the value is {@link Boolean#TRUE}
     * if the identifier has been removed, of {@code Boolean#FALSE} otherwise.</p>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.7
     * @module
     */
    @SuppressWarnings("serial") // Not intended to be serialized.
    private static final class Iter extends HashMap<Citation,Boolean> implements Iterator<Entry<Citation,String>> {
        /**
         * An iterator over the {@link IdentifierMapAdapter#identifiers} collection,
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
         * {@code true} if the iterator should support the {@link #remove()} operation.
         */
        private final boolean isModifiable;

        /**
         * Creates a new iterator for the given collection of identifiers.
         */
        Iter(final Collection<? extends Identifier> identifiers, final boolean isModifiable) {
            super(hashMapCapacity(identifiers.size()));
            this.identifiers = identifiers.iterator();
            this.isModifiable = isModifiable;
        }

        /**
         * Advances to the next non-null identifier, skips duplicated authorities, wraps the
         * identifier in an entry if needed and stores the result in the {@link #next} field.
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
            if (!isModifiable) {
                throw new UnsupportedOperationException();
            }
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
    @Debug
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
