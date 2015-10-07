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

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Collection;
import java.util.Collections;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.simple.CitationConstant;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.xml.IdentifierSpace;


/**
 * The {@linkplain Identifier#getAuthority() authority of identifiers} that are not expected to be
 * marshalled in a {@code MD_Identifier} XML element. Those identifiers are also excluded from the
 * tree formatted by {@link org.apache.sis.metadata.AbstractMetadata#asTreeTable()}.
 *
 * <p>There is two kinds of non-marshalled identifiers:</p>
 *
 * <ul>
 *   <li>The XML attributes declared by ISO 19139 specification in the {@code gco:PropertyType}
 *       element: {@code gml:id}, {@code gco:uuid} and {@code xlink:href}. Those attributes are
 *       not part of the ISO 19115 specification. Those authorities are declared in the
 *       {@link IdentifierSpace} interfaces.</li>
 *
 *   <li>ISO 19115 attributes that we choose, for the SIS implementation, to merge with
 *       other identifiers: ISBN and ISSN codes. Those attributes are declared in the
 *       {@link org.apache.sis.metadata.iso.citation.Citations} class.</li>
 * </ul>
 *
 * In the current SIS library, there is different places where identifiers are filtered on the
 * basis of this class, as below:
 *
 * {@preformat java
 *     if (identifier.getAuthority() instanceof NonMarshalledAuthority<?>) {
 *         // Omit that identifier.
 *     }
 * }
 *
 * @param <T> The type of object used as identifier values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see IdentifierSpace
 */
public final class NonMarshalledAuthority<T> extends CitationConstant.Authority<T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6299502270649111201L;

    /**
     * Ordinal values for switch statements. The constant defined here shall
     * mirror the constants defined in the {@link IdentifierSpace} interface
     * and {@link org.apache.sis.metadata.iso.citation.DefaultCitation} class.
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    public static final byte ID=0, UUID=1, HREF=2, XLINK=3, ISSN=4, ISBN=5;
    // If more codes are added, please update readResolve() below.

    /**
     * Ordinal values for switch statements, as one of the {@link #ID}, {@link #UUID},
     * <i>etc.</i> constants.
     *
     * <p>This value is not serialized because its value may not be consistent between different
     * versions of the SIS library (the attribute name is more reliable). This instance should
     * be replaced by one of the exiting constants at deserialization time anyway.</p>
     */
    final transient byte ordinal;

    /**
     * Creates a new citation for the given XML attribute name.
     *
     * @param attribute The XML attribute name, to be returned by {@link #getName()}.
     * @param ordinal   Ordinal value for switch statement, as one of the {@link #ID},
     *                  {@link #UUID}, <i>etc.</i> constants.
     */
    public NonMarshalledAuthority(final String attribute, final byte ordinal) {
        super(attribute);
        this.ordinal = ordinal;
    }

    /**
     * Returns the first marshallable identifier from the given collection. This method omits
     * "special" identifiers (ISO 19139 attributes, ISBN codes...), which are recognized by
     * the implementation class of their authority.
     *
     * <p>This method is used for implementation of {@code getIdentifier()} methods (singular form)
     * in public metadata objects.</p>
     *
     * @param  <T> The type of object used as identifier values.
     * @param  identifiers The collection from which to get identifiers, or {@code null}.
     * @return The first identifier, or {@code null} if none.
     */
    public static <T extends Identifier> T getMarshallable(final Collection<? extends T> identifiers) {
        if (identifiers != null) {
            for (final T id : identifiers) {
                if (id != null && !(id.getAuthority() instanceof NonMarshalledAuthority<?>)) {
                    return id;
                }
            }
        }
        return null;
    }

    /**
     * Sets the given identifier in the given collection. This method removes all identifiers
     * that are not ISO 19139 identifiers before to adds the given one in the collection. This
     * method is used when the given collection is expected to contains only one ISO 19115
     * identifier.
     *
     * <p>This method is used for implementation of {@code setIdentifier(Identifier)} methods
     * in public metadata objects.</p>
     *
     * @param <T>         The type of object used as identifier values.
     * @param identifiers The collection in which to add the identifier.
     * @param newValue    The identifier to add, or {@code null}.
     *
     * @see #setMarshallables(Collection, Collection)
     */
    public static <T extends Identifier> void setMarshallable(final Collection<T> identifiers, final T newValue) {
        final Iterator<T> it = identifiers.iterator();
        while (it.hasNext()) {
            final T old = it.next();
            if (old != null && old.getAuthority() instanceof NonMarshalledAuthority<?>) {
                continue; // Don't touch this identifier.
            }
            it.remove();
        }
        if (newValue != null) {
            identifiers.add(newValue);
        }
    }

    /**
     * If marshalling, filters the given collection of identifiers in order to omit any identifiers
     * for which the authority is an instance of {@code NonMarshalledAuthority}. This should exclude
     * all {@link org.apache.sis.xml.IdentifierSpace} constants.
     *
     * <p>This method is used for implementation of {@code getIdentifiers()} methods (plural form)
     * in public metadata objects. Note that those methods override
     * {@link org.apache.sis.xml.IdentifiedObject#getIdentifiers()}, which is expected to return
     * all identifiers in normal (non-marshalling) usage.</p>
     *
     * @param  identifiers The identifiers to filter, or {@code null}.
     * @return The identifiers to marshal, or {@code null} if none.
     */
    public static Collection<Identifier> filterOnMarshalling(Collection<Identifier> identifiers) {
        if (identifiers != null && Context.isFlagSet(Context.current(), Context.MARSHALLING)) {
            int count = identifiers.size();
            if (count != 0) {
                final Identifier[] copy = identifiers.toArray(new Identifier[count]);
                for (int i=count; --i>=0;) {
                    final Identifier id = copy[i];
                    if (id == null || (id.getAuthority() instanceof NonMarshalledAuthority)) {
                        System.arraycopy(copy, i+1, copy, i, --count - i);
                    }
                }
                identifiers = (count != 0) ? UnmodifiableArrayList.wrap(copy, 0, count) : null;
            }
        }
        return identifiers;
    }

    /**
     * Returns a collection containing all marshallable values of {@code newValues}, together with unmarshallable
     * values of {@code identifiers}. This method is invoked for preserving the identifiers that are conceptually
     * stored in distinct fields (XML identifier, UUID, ISBN, ISSN) when setting the collection of all identifiers
     * in a metadata object.
     *
     * <p>This method is used for implementation of {@code setIdentifiers(Collection)} methods
     * in public metadata objects.</p>
     *
     * @param  identifiers The metadata internal identifiers collection, or {@code null} if none.
     * @param  newValues   The identifiers to add, or {@code null}.
     * @return The collection to set (may be {@code newValues}.
     *
     * @see #setMarshallable(Collection, Identifier)
     */
    @SuppressWarnings("null")
    public static Collection<? extends Identifier> setMarshallables(
            final Collection<Identifier> identifiers, final Collection<? extends Identifier> newValues)
    {
        int remaining;
        if (identifiers == null || (remaining = identifiers.size()) == 0) {
            return newValues;
        }
        /*
         * If there is any identifiers that need to be preserved (XML identifier, UUID, ISBN, etc.),
         * remember them. Otherwise there is nothing special to do and we can return the new values directly.
         */
        List<Identifier> toPreserve = null;
        for (final Identifier id : identifiers) {
            if (id != null && id.getAuthority() instanceof NonMarshalledAuthority<?>) {
                if (toPreserve == null) {
                    toPreserve = new ArrayList<Identifier>(remaining);
                }
                toPreserve.add(id);
            }
            remaining--;
        }
        if (toPreserve == null) {
            return newValues;
        }
        /*
         * We find at least one identifier that may need to be preserved.
         * We need to create a combination of the two collections.
         */
        final Map<Citation,Identifier> authorities = new IdentityHashMap<Citation,Identifier>(4);
        final List<Identifier> merged = new ArrayList<Identifier>(newValues.size());
        for (final Identifier id : newValues) {
            merged.add(id);
            if (id != null) {
                final Citation authority = id.getAuthority();
                if (authority instanceof NonMarshalledAuthority<?>) {
                    authorities.put(authority, id);
                }
            }
        }
        for (final Identifier id : toPreserve) {
            if (!authorities.containsKey(id.getAuthority())) {
                merged.add(id);
            }
        }
        /*
         * Wraps in an unmodifiable list in case the caller is creating an unmodifiable metadata.
         */
        switch (merged.size()) {
            case 0:  return Collections.emptyList();
            case 1:  return Collections.singletonList(merged.get(0));
            default: return Containers.unmodifiableList(CollectionsExt.toArray(merged, Identifier.class));
        }
    }

    /**
     * Invoked at deserialization time in order to replace the deserialized instance
     * by the appropriate instance defined in the {@link IdentifierSpace} interface.
     *
     * @return The instance to use, as an unique instance if possible.
     */
    @Override
    protected Object readResolve() {
        final String name = getName();
        IdentifierSpace<?> candidate;
        int code = 0;
        do {
            switch (code++) {
                case ID:    candidate = IdentifierSpace.ID;    break;
                case UUID:  candidate = IdentifierSpace.UUID;  break;
                case HREF:  candidate = IdentifierSpace.HREF;  break;
                case XLINK: candidate = IdentifierSpace.XLINK; break;
                default: return super.readResolve();
            }
        } while (!((NonMarshalledAuthority<?>) candidate).getName().equals(name));
        return candidate;
    }
}
