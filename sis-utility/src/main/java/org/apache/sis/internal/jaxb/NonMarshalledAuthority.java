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
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.lang.reflect.Field;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.xml.IdentifierSpace;


/**
 * The {@linkplain Identifier#getAuthority() authority of identifiers} that are not expected to be
 * marshalled in a {@code MD_Identifier} XML element. Those identifiers are also excluded from the
 * tree formatted by {@link org.apache.sis.metadata.AbstractMetadata#asTree()}.
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
    private static final long serialVersionUID = 6299502270649111201L;

    /**
     * Sets to {@code true} if {@link #getCitation(String)} has already logged a warning.
     * This is used in order to avoid flooding the logs with the same message.
     */
    private static volatile boolean warningLogged;

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
        if (id != null) {
            identifiers.add(id);
        }
    }

    /**
     * If marshalling, filters the given collection of identifiers in order to omit any identifiers
     * for which the authority is an instance of {@code NonMarshalledAuthority}. This should exclude
     * all {@link org.apache.sis.xml.IdentifierSpace} constants.
     *
     * @param  identifiers The identifiers to filter, or {@code null}.
     * @return The identifiers to marshal, or {@code null} if none.
     */
    public static Collection<Identifier> excludeOnMarshalling(Collection<Identifier> identifiers) {
        if (identifiers != null && Context.isMarshalling()) {
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
     * Returns a collection containing only the identifiers having a {@code NonMarshalledAuthority}.
     * This method is invoked for saving the identifiers that are conceptually stored in distinct fields
     * (XML identifier, UUID, ISBN, ISSN) before to overwrite the collection of all identifiers in
     * a metadata object.
     *
     * <p>This method is invoked from {@code setIdentifiers(Collection<Identifier>)} implementation
     * in {@link org.apache.sis.metadata.iso.ISOMetadata} subclasses as below:</p>
     *
     * {@preformat java
     *     final Collection<Identifier> oldIds = NonMarshalledAuthority.filteredCopy(identifiers);
     *     identifiers = writeCollection(newValues, identifiers, Identifier.class);
     *     NonMarshalledAuthority.replace(identifiers, oldIds);
     * }
     *
     * @param  <T> The type of object used as identifier values.
     * @param  identifiers The metadata internal identifiers collection, or {@code null} if none.
     * @return The new list containing the filtered identifiers, or {@code null} if none.
     */
    public static <T extends Identifier> Collection<T> filteredCopy(final Collection<T> identifiers) {
        Collection<T> filtered = null;
        if (identifiers != null) {
            int remaining = identifiers.size();
            for (final T candidate : identifiers) {
                if (candidate != null && candidate.getAuthority() instanceof NonMarshalledAuthority<?>) {
                    if (filtered == null) {
                        filtered = new ArrayList<>(remaining);
                    }
                    filtered.add(candidate);
                }
                remaining--;
            }
        }
        return filtered;
    }

    /**
     * Replaces all identifiers in the {@code identifiers} collection having the same
     * {@linkplain Identifier#getAuthority() authority} than the ones in {@code oldIds}.
     * More specifically:
     *
     * <ul>
     *   <li>First, remove all {@code identifiers} elements having the same authority
     *       than one of the elements in {@code oldIds}.</li>
     *   <li>Next, add all {@code oldIds} elements to {@code identifiers}.</li>
     * </ul>
     *
     * @param <T> The type of object used as identifier values.
     * @param identifiers The metadata internal identifiers collection, or {@code null} if none.
     * @param oldIds The previous filtered identifiers returned by {@link #filteredCopy(Collection)},
     *               or {@code null} if none.
     */
    public static <T extends Identifier> void replace(final Collection<T> identifiers, final Collection<T> oldIds) {
        if (oldIds != null && identifiers != null) {
            for (final T old : oldIds) {
                final Citation authority = old.getAuthority();
                for (final Iterator<T> it=identifiers.iterator(); it.hasNext();) {
                    final T id = it.next();
                    if (id == null || id.getAuthority() == authority) {
                        it.remove();
                    }
                }
            }
            identifiers.addAll(oldIds);
        }
    }

    /**
     * Returns one of the constants in the {@link DefaultCitation} class, or {@code null} if none.
     * We need to use Java reflection because the {@code sis-metadata} module may not be in the
     * classpath.
     */
    private static IdentifierSpace<?> getCitation(final String name) throws ObjectStreamException {
        try {
            final Field field = Class.forName("org.apache.sis.metadata.iso.citation.DefaultCitation").getDeclaredField(name);
            field.setAccessible(true);
            return (IdentifierSpace<?>) field.get(null);
        } catch (ReflectiveOperationException e) {
            if (!warningLogged) {
                warningLogged = true;
                final LogRecord record = Errors.getResources(null).getLogRecord(Level.WARNING,
                        Errors.Keys.MissingRequiredModule_1, "sis-metadata");
                record.setThrown(e);
                Logging.log(NonMarshalledAuthority.class, "readResolve", record);
            }
        }
        return null;
    }

    /**
     * Invoked at deserialization time in order to replace the deserialized instance
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
