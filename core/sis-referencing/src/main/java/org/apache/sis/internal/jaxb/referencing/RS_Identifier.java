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
package org.apache.sis.internal.jaxb.referencing;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.metadata.Identifier;


/**
 * JAXB adapter mapping the GeoAPI {@link ReferenceIdentifier} to an implementation class that can
 * be marshalled. See the package documentation for more information about JAXB and interfaces.
 *
 * <p>Note that a class of the same name is defined in the {@link org.apache.sis.internal.jaxb.metadata}
 * package, which serve the same purpose (wrapping exactly the same interface) but using the ISO 19139
 * syntax instead. The ISO 19139 syntax represents the code and codespace as XML elements, while in this
 * GML representation the code is a XML value and the codespace is a XML attribute.</p>
 *
 * <div class="section">Marshalling</div>
 * Identifiers are typically marshalled as below:
 *
 * {@preformat xml
 *   <gml:identifier codeSpace="EPSG">4326</gml:identifier>
 * }
 *
 * If the {@code Identifier} to marshal contains a {@linkplain ReferenceIdentifier#getVersion() version},
 * then this adapter concatenates the version to the codespace in a "URI-like" way like below:
 *
 * {@preformat xml
 *   <gml:identifier codeSpace="EPSG:8.3">4326</gml:identifier>
 * }
 *
 * <div class="section">Unmarshalling</div>
 * Some data producers put a URN instead than a simple code value, as in the example below:
 *
 * {@preformat xml
 *   <gml:identifier codeSpace="IOGP">urn:ogc:def:crs:EPSG::4326</gml:identifier>
 * }
 *
 * In such case this class takes the codespace as the {@linkplain Identifier#getAuthority() authority}
 * ("IOGP" in above example), and the 3 last URI elements are parsed as the codespace, version (optional)
 * and code values respectively.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class RS_Identifier extends XmlAdapter<Code, ReferenceIdentifier> {
    /**
     * Substitutes the wrapper value read from an XML stream by the object which will
     * represents the identifier. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value The wrapper for this metadata value.
     * @return An identifier which represents the value.
     */
    @Override
    public ReferenceIdentifier unmarshal(final Code value) {
        return (value != null) ? value.getIdentifier() : null;
    }

    /**
     * Substitutes the identifier by the wrapper to be marshalled into an XML file or stream.
     * JAXB calls automatically this method at marshalling time.
     *
     * @param  value The metadata value.
     * @return The adapter for the given metadata.
     */
    @Override
    public Code marshal(final ReferenceIdentifier value) {
        return (value != null) ? new Code(value) : null;
    }
}
