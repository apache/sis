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
package org.apache.sis.xml;

import java.net.URI;
import java.util.UUID;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;


/**
 * Some identifier namespaces that are handled in a special way. The identifier namespaces are
 * usually defined as authorities in the {@link org.apache.sis.metadata.iso.citation.Citations}
 * class. However a few identifiers defined in the {@code gco:ObjectIdentification} XML attribute
 * group are handled in a special way. For example identifiers associated to the {@link #HREF}
 * space are marshalled in the outer property element, as in the example below:
 *
 * {@preformat xml
 *   <gmd:CI_Citation>
 *     <gmd:series xlink:href="http://myReference">
 *       <gmd:CI_Series>
 *         <gmd:name>...</gmd:name>
 *       </gmd:CI_Series>
 *     </gmd:series>
 *   </gmd:CI_Citation>
 * }
 *
 * The values defined in this interface can be used as keys in the map returned by
 * {@link IdentifiedObject#getIdentifierMap()}.
 *
 * @param <T> The type of object used as identifier values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.metadata.iso.citation.Citations
 * @see IdentifiedObject
 * @see IdentifierMap
 */
public interface IdentifierSpace<T> extends Citation {
    /**
     * A standard GML attribute available on every object-with-identity.
     * Its type is {@code "xs:ID"} - i.e. it is a fragment identifier, unique within document scope only,
     * for internal cross-references. It is not useful by itself as a persistent unique identifier.
     *
     * <p>The XML {@linkplain #getName() attribute name} is {@code "gml:id"}, but is also used
     * for {@code "gco:id"} in metadata documents. However the {@code "gco:"} prefix is omitted
     * in XML documents (i.e. the {@code gco:id} attribute is <cite>unqualified</cite>).</p>
     *
     * <p>Elements with {@code gml:id} or {@code gco:id} attribute can be referenced from other XML elements
     * using the {@code xlink:href} attribute. This is done automatically by Apache SIS implementations at
     * marshalling and unmarshalling time. If many of {@code gml:id}, {@code gco:uuid} and {@code xlink:href}
     * attributes are used, then {@code gml:id} has precedence.</p>
     *
     * @see javax.xml.bind.annotation.XmlID
     */
    IdentifierSpace<String> ID = new NonMarshalledAuthority<String>("gml:id", NonMarshalledAuthority.ID);

    /**
     * An optional attribute available on every object-with-identity provided in the GMD schemas
     * that implement ISO 19115 in XML. May be used as a persistent unique identifier, but only
     * available within GMD context.
     *
     * <p>The XML {@linkplain #getName() attribute name} is {@code "gco:uuid"}. However the
     * {@code "gco:"} prefix is omitted in XML documents (i.e. the {@code gco:uuid} attribute
     * is <cite>unqualified</cite>).</p>
     *
     * <p>Elements with {@code gco:uuid} attribute can be referenced from other XML elements using the
     * {@code gco:uuidref} attribute. However this is not done automatically by Apache SIS. Users need
     * to manage their set of UUIDs in their own {@link ReferenceResolver} subclass.</p>
     *
     * @see UUID
     */
    IdentifierSpace<UUID> UUID = new NonMarshalledAuthority<UUID>("gco:uuid", NonMarshalledAuthority.UUID);

    /**
     * An optional attribute for URN to an external resources, or to an other part of a XML
     * document, or an identifier. This is one of the many attributes available in the
     * {@link #XLINK} identifier space, but is provided as a special constant because
     * {@code href} is the most frequently used {@code xlink} attribute.
     *
     * <p>The XML {@linkplain #getName() attribute name} is {@code "xlink:href"}.</p>
     *
     * @see XLink#getHRef()
     */
    IdentifierSpace<URI> HREF = new NonMarshalledAuthority<URI>("xlink:href", NonMarshalledAuthority.HREF);

    /**
     * Any XML attributes defined by OGC in the
     * <a href="http://schemas.opengis.net/xlink/1.0.0/xlinks.xsd">xlink</a> schema.
     * Note that the above {@link #HREF} identifier space is a special case of this
     * {@code xlink} identifier space.
     *
     * @see XLink
     */
    IdentifierSpace<XLink> XLINK = new NonMarshalledAuthority<XLink>("xlink", NonMarshalledAuthority.XLINK);

    /**
     * Returns the name of this identifier space.
     *
     * <ul>
     *   <li>For the constants defined in this {@code IdentifierSpace} interface, this is
     *       the XML attribute name with its prefix. Attribute names can be {@code "gml:id"},
     *       {@code "gco:uuid"} or {@code "xlink:href"}.</li>
     *
     *   <li>For the constants defined in the {@link org.apache.sis.metadata.iso.citation.Citations}
     *       class, this is the identifier namespace. They are usually not XML attribute name because those
     *       identifiers are marshalled as {@code <MD_Identifier>} XML elements rather than attributes.</li>
     * </ul>
     *
     * @return The name of this identifier space (may be XML attribute name).
     */
    String getName();
}
