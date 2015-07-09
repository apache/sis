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
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.internal.jaxb.Context;
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
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see PropertyType
 * @see <a href="ObjectIdentification.html">ObjectIdentification</a>
 * @see <a href="http://schemas.opengis.net/iso/19139/20070417/gco/gcoBase.xsd">OGC schema</a>
 * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-165">GEOTK-165</a>
 */
final class ObjectReference {
    /**
     * A unique identifier to an external resources, or to an other part of a XML document.
     * The {@code uuidref} attribute is used to refer to an XML element that has a corresponding
     * {@code uuid} attribute.
     *
     * @see <a href="http://www.schemacentral.com/sc/niem21/a-uuidref-1.html">Usage of uuidref</a>
     */
    UUID uuid;

    /**
     * The {@code xlink} attributes, or {@code null} if none.
     */
    XLink xlink;

    /**
     * Creates an initially empty object reference.
     */
    ObjectReference() {
    }

    /**
     * Creates an object reference initialized to the given value.
     *
     * @see ReferenceResolver#canSubstituteByReference(MarshalContext, Class, Object, UUID)
     * @see ReferenceResolver#canSubstituteByReference(MarshalContext, Class, Object, XLink)
     */
    ObjectReference(final UUID uuid, final XLink link) {
        this.uuid  = uuid;
        this.xlink = link;
    }

    /**
     * If the given metadata object is null, tries to get an instance from the identifiers
     * declared in this {@code ObjectReference}. If the given metadata object is non-null,
     * assigns to that object the identifiers declared in this {@code ObjectReference}.
     *
     * <p>This method is invoked at unmarshalling time by {@link PropertyType#resolve(Context)}.</p>
     *
     * @param  <T>       The compile-time type of the {@code type} argument.
     * @param  context   The marshalling context, or {@code null} if none.
     * @param  type      The expected type of the metadata object.
     * @param  metadata  The metadata object, or {@code null}.
     * @return A metadata object for the identifiers, or {@code null}
     */
    final <T> T resolve(final Context context, final Class<T> type, T metadata) {
        if (metadata == null) {
            final ReferenceResolver resolver = Context.resolver(context);
            if ((uuid  == null || (metadata = resolver.resolve(context, type, uuid )) == null) &&
                (xlink == null || (metadata = resolver.resolve(context, type, xlink)) == null))
            {
                // Failed to find an existing metadata instance.
                // Creates an empty instance with the identifiers.
                int count = 0;
                SpecializedIdentifier<?>[] identifiers  = new SpecializedIdentifier<?>[2];
                if (uuid  != null) identifiers[count++] = new SpecializedIdentifier<UUID> (IdentifierSpace.UUID,  uuid);
                if (xlink != null) identifiers[count++] = new SpecializedIdentifier<XLink>(IdentifierSpace.XLINK, xlink);
                identifiers = ArraysExt.resize(identifiers, count);
                metadata = resolver.newIdentifiedObject(context, type, identifiers);
            }
        } else {
            // In principle, the XML should contain a full metadata object OR a uuidref attribute.
            // However if both are present, assign the identifiers to that instance.
            if (metadata instanceof IdentifiedObject) {
                final IdentifierMap map = ((IdentifiedObject) metadata).getIdentifierMap();
                putInto(context, map, IdentifierSpace.UUID,  uuid);
                putInto(context, map, IdentifierSpace.XLINK, xlink);
            }
        }
        return metadata;
    }

    /**
     * Adds a new identifier into the given map, if non null. No previous value should exist in normal situation.
     * However a previous value may exit in unusual (probably not very valid) XML, as in the following example:
     *
     * {@preformat xml
     *   <gmd:CI_Citation>
     *     <gmd:series uuidref="f8f5fcb1-d57b-4013-b3a4-4eaa40df6dcf">
     *       <gmd:CI_Series uuid="f8f5fcb1-d57b-4013-b3a4-4eaa40df6dcf">
     *         ...
     *       </gmd:CI_Series>
     *     </gmd:series>
     *   </gmd:CI_Citation>
     * }
     *
     * In such situation, this method is silent if the two identifiers are equal, or logs a warning and restores
     * the previous value if they are not equal. The previous value is the "{@code uuid}" attribute, which is
     * assumed more closely tied to the actual metadata than the {@code uuidref} attribute.
     *
     * @param map       The map in which to write the identifier.
     * @param authority The identifier authority.
     * @param value     The identifier value, or {@code null} if not yet defined.
     */
    private static <T> void putInto(final Context context, final IdentifierMap map,
            final IdentifierSpace<T> authority, final T value)
    {
        if (value != null) {
            final T previous = map.putSpecialized(authority, value);
            if (previous != null && !previous.equals(value)) {
                Context.warningOccured(context, IdentifierMap.class, "putSpecialized",
                        Errors.class, Errors.Keys.InconsistentAttribute_2, authority.getName(), value);
                map.putSpecialized(authority, previous);
            }
        }
    }
}
