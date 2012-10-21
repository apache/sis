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
import java.util.ArrayList;
import java.util.Collection;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.lang.reflect.Field;
import org.opengis.metadata.Identifier;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.IdentifierSpace;

import static org.apache.sis.util.collection.Collections.addIfNonNull;


/**
 * The {@linkplain Identifier#getAuthority() authority of identifiers} that are not expected to be
 * marshalled in a {@code MD_Identifier} XML element. Those identifiers are also excluded from the
 * tree formatted by {@link org.apache.sis.metadata.AbstractMetadata#asTree()}.
 *
 * <p>There is two kinds of non-marshalled identifiers:</p>
 *
 * <ul>
 *   <li>The XML attributes declared by ISO 19139 specification in the {@code gco:PropertyType}
 *       element: {@code gml:id}, {@code gco:uuid} and {@code xlink:href}. Those attributes are not
 *       part of the ISO 19115 specification. Those authorities are declared in the
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
 * @since   0.3 (derived from geotk-3.19)
 * @version 0.3
 * @module
 *
 * @see IdentifierSpace
 */
public final class NonMarshalledAuthority<T> extends SimpleCitation implements IdentifierSpace<T>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6309485399210742418L;

    /**
     * Ordinal values for switch statements. The constant defined here shall
     * mirror the constants defined in the {@link IdentifierSpace} interface
     * and {@link org.apache.sis.metadata.iso.citation.DefaultCitation} class.
     */
    public static final int ID=0, UUID=1, HREF=2, XLINK=3, ISSN=4, ISBN=5;
    // If more codes are added, please update readResolve() below.

    /**
     * Ordinal values for switch statements, as one of the {@link #ID}, {@link #UUID},
     * <i>etc.</i> constants.
     *
     * <p>This value is not serialized because its value may not be consistent between different
     * versions of the SIS library (the attribute name is more reliable). This instance should
     * be replaced by one of the exiting constants at deserialization time anyway.</p>
     */
    final transient int ordinal;

    /**
     * Creates a new enum for the given attribute.
     *
     * @param attribute The XML attribute name, to be returned by {@link #getName()}.
     * @param ordinal   Ordinal value for switch statement, as one of the {@link #ID},
     *                  {@link #UUID}, <i>etc.</i> constants.
     */
    public NonMarshalledAuthority(final String attribute, final int ordinal) {
        super(attribute);
        this.ordinal = ordinal;
    }

    /**
     * Returns the XML attribute name with its prefix. Attribute names can be {@code "gml:id"},
     * {@code "gco:uuid"} or {@code "xlink:href"}.
     */
    @Override
    public String getName() {
        return title;
    }

    /**
     * Returns a string representation of this identifier space.
     */
    @Override
    public String toString() {
        return "IdentifierSpace[" + title + ']';
    }

    /**
     * Returns the first marshallable identifier from the given collection. This method omits
     * "special" identifiers (ISO 19139 attributes, ISBN codes...), which are recognized by
     * the implementation class of their authority.
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
     * @param <T> The type of object used as identifier values.
     * @param identifiers The collection in which to add the identifier.
     * @param id The identifier to add, or {@code null}.
     */
    public static <T extends Identifier> void setMarshallable(final Collection<T> identifiers, final T id) {
        final Iterator<T> it = identifiers.iterator();
        while (it.hasNext()) {
            final T old = it.next();
            if (old != null) {
                if (old.getAuthority() instanceof NonMarshalledAuthority<?>) {
                    continue; // Don't touch this identifier.
                }
            }
            it.remove();
        }
        addIfNonNull(identifiers, id);
    }

    /**
     * Returns a collection containing only the identifiers having a {@code NonMarshalledAuthority}.
     *
     * @param  <T> The type of object used as identifier values.
     * @param  identifiers The identifiers to getIdentifiers, or {@code null} if none.
     * @return The filtered identifiers, or {@code null} if none.
     */
    public static <T extends Identifier> Collection<T> getIdentifiers(final Collection<? extends T> identifiers) {
        Collection<T> filtered = null;
        if (identifiers != null) {
            int remaining = identifiers.size();
            for (final T candidate : identifiers) {
                if (candidate != null && candidate.getAuthority() instanceof NonMarshalledAuthority<?>) {
                    if (filtered == null) {
                        filtered = new ArrayList<T>(remaining);
                    }
                    filtered.add(candidate);
                }
                remaining--;
            }
        }
        return filtered;
    }

    /**
     * Removes from the given collection every identifiers having a {@code NonMarshalledAuthority},
     * then adds the previously filtered identifiers (if any).
     *
     * @param <T> The type of object used as identifier values.
     * @param identifiers The collection from which to remove identifiers, or {@code null}.
     * @param filtered The previous filtered identifiers returned by {@link #getIdentifiers}.
     */
    public static <T extends Identifier> void setIdentifiers(final Collection<T> identifiers, final Collection<T> filtered) {
        if (identifiers != null) {
            for (final Iterator<T> it=identifiers.iterator(); it.hasNext();) {
                final T id = it.next();
                if (id == null || id.getAuthority() instanceof NonMarshalledAuthority<?>) {
                    it.remove();
                }
            }
            if (filtered != null) {
                identifiers.addAll(filtered);
            }
        }
    }

    /**
     * Returns one of the constants in the {@link DefaultCitation} class.
     */
    private static IdentifierSpace<?> getCitation(final String name) throws ObjectStreamException {
        try {
            final Field field = Class.forName("org.apache.sis.metadata.iso.citation.DefaultCitation").getDeclaredField(name);
            field.setAccessible(true);
            return (IdentifierSpace<?>) field.get(null);
        } catch (Exception e) {
            Logging.unexpectedException(NonMarshalledAuthority.class, "readResolve", e);
        }
        return null;
    }

    /**
     * Invoked at deserialization time in order to setIdentifiers the deserialized instance
     * by the appropriate instance defined in the {@link IdentifierSpace} interface.
     */
    private Object readResolve() throws ObjectStreamException {
        int code = 0;
        while (true) {
            final IdentifierSpace<?> candidate;
            switch (code) {
                case ID:    candidate = IdentifierSpace.ID;    break;
                case UUID:  candidate = IdentifierSpace.UUID;  break;
                case HREF:  candidate = IdentifierSpace.HREF;  break;
                case XLINK: candidate = IdentifierSpace.XLINK; break;
                case ISBN:  candidate = getCitation("ISBN");   break;
                case ISSN:  candidate = getCitation("ISSN");   break;
                default: return this;
            }
            if (candidate instanceof NonMarshalledAuthority<?> &&
                    ((NonMarshalledAuthority<?>) candidate).title.equals(title))
            {
                return candidate;
            }
            code++;
        }
    }
}
