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
package org.apache.sis.internal.jaxb.gco;

import java.util.UUID;

import org.apache.sis.xml.XLink;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.xml.ReferenceResolver;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.internal.jaxb.MarshalContext;
import org.apache.sis.internal.jaxb.IdentifierMapAdapter;
import org.apache.sis.internal.jaxb.SpecializedIdentifier;


/**
 * The {@code gco:ObjectReference} XML attribute group is included by all metadata wrappers defined
 * in the {@link org.apache.sis.internal.jaxb.metadata} package. The attributes of interest defined
 * in this group are {@code uuidref}, {@code xlink:href}, {@code xlink:role}, {@code xlink:arcrole},
 * {@code xlink:title}, {@code xlink:show} and {@code xlink:actuate}.
 *
 * <p>This {@code gco:ObjectReference} group is complementary to {@code gco:ObjectIdentification},
 * which define the {@code id} and {@code uuid} attributes to be supported by all metadata
 * implementations in the public {@link org.apache.sis.metadata.iso} package and sub-packages.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.18)
 * @version 0.3
 * @module
 *
 * @see PropertyType
 * @see ObjectIdentification
 * @see <a href="http://schemas.opengis.net/iso/19139/20070417/gco/gcoBase.xsd">OGC schema</a>
 * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-165">GEOTK-165</a>
 */
final class ObjectReference {
    /**
     * A URN to an external resources, or to an other part of a XML document, or an identifier.
     * The {@code uuidref} attribute is used to refer to an XML element that has a corresponding
     * {@code uuid} attribute.
     *
     * @see <a href="http://www.schemacentral.com/sc/niem21/a-uuidref-1.html">Usage of uuidref</a>
     */
    String anyUUID;

    /**
     * The {@code xlink} attributes, or {@code null} if none.
     */
    XLink xlink;

    /**
     * The parsed value of {@link #anyUUID}, computed when first needed.
     */
    private transient UUID uuid;

    /**
     * Creates an initially empty object reference.
     */
    ObjectReference() {
    }

    /**
     * Creates an object reference initialized to the given value.
     */
    ObjectReference(final UUID uuid, final String anyUUID, final XLink link) {
        this.uuid    = uuid;
        this.anyUUID = anyUUID;
        this.xlink   = link;
    }

    /**
     * Parses the given string as a UUID.
     *
     * @param  context The marshalling context, or {@code null} if none.
     * @param  anyUUID The string to parse, or {@code null}.
     * @return The parsed UUID, or {@code null}.
     * @throws IllegalArgumentException If {@code anyUUID} can not be parsed.
     */
    static UUID toUUID(final MarshalContext context, final String anyUUID) throws IllegalArgumentException {
        return (anyUUID != null) ? MarshalContext.converter(context).toUUID(context, anyUUID) : null;
    }

    /**
     * If the given metadata object is null, tries to get an instance from the identifiers
     * declared in this {@code ObjectReference}. If the given metadata object is non-null,
     * assigns to that object the identifiers declared in this {@code ObjectReference}.
     *
     * <p>This method is invoked at unmarshalling time.</p>
     *
     * @param  <T>       The compile-time type of the {@code type} argument.
     * @param  context   The marshalling context, or {@code null} if none.
     * @param  type      The expected type of the metadata object.
     * @param  metadata  The metadata object, or {@code null}.
     * @return A metadata object for the identifiers, or {@code null}
     * @throws IllegalArgumentException If the {@link #anyUUID} field can not be parsed.
     */
    final <T> T resolve(final MarshalContext context, final Class<T> type, T metadata) throws IllegalArgumentException {
        if (uuid == null) {
            uuid = toUUID(context, anyUUID);
        }
        if (metadata == null) {
            final ReferenceResolver resolver = MarshalContext.resolver(context);
            if ((uuid  == null || (metadata = resolver.resolve(context, type, uuid )) == null) &&
                (xlink == null || (metadata = resolver.resolve(context, type, xlink)) == null))
            {
                // Failed to find an existing metadata instance.
                // Creates an empty instance with the identifiers.
                int count = 0;
                SpecializedIdentifier<?>[] identifiers  = new SpecializedIdentifier<?>[2];
                if (uuid  != null) identifiers[count++] = new SpecializedIdentifier<>(IdentifierSpace.UUID,  uuid);
                if (xlink != null) identifiers[count++] = new SpecializedIdentifier<>(IdentifierSpace.XLINK, xlink);
                identifiers = ArraysExt.resize(identifiers, count);
                metadata = resolver.newIdentifiedObject(context, type, identifiers);
            }
        } else {
            // If principle, the XML should contain a full metadata object OR a uuidref attribute.
            // However if both are present, assign the identifiers to that instance.
            if (metadata instanceof IdentifiedObject) {
                final IdentifierMap map = ((IdentifiedObject) metadata).getIdentifierMap();
                if (uuid  != null) putInto(map, IdentifierSpace.UUID,  uuid);
                if (xlink != null) putInto(map, IdentifierSpace.XLINK, xlink);
            }
        }
        return metadata;
    }

    /**
     * Adds a new identifier into the given map. This method is a shortcut which bypass the check
     * for previous values associated to same the authority. It is okay only when constructing
     * new instances, for example at XML unmarshalling time.
     *
     * @param map       The map in which to write the identifier.
     * @param authority The identifier authority.
     * @param value     The identifier value, or {@code null} if not yet defined.
     */
    private static <T> void putInto(final IdentifierMap map, final IdentifierSpace<T> authority, final T value) {
        if (map instanceof IdentifierMapAdapter) {
            final SpecializedIdentifier<T> identifier = new SpecializedIdentifier<>(authority, value);
            /*
             * If the following assert statement appears to fail in practice, then remove
             * completly this method and use the public putSpecialized(…) method instead.
             * Note: usage of 'put' is for having the compiler to check the key type.
             */
            assert map.put(authority, null) == null : identifier;
            ((IdentifierMapAdapter) map).identifiers.add(identifier);
        } else {
            map.putSpecialized(authority, value);
        }
    }
}
