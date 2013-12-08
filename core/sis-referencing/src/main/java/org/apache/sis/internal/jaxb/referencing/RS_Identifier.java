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

import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.internal.util.URIParser;


/**
 * JAXB adapter mapping the GeoAPI {@link ReferenceIdentifier} to an implementation class that can
 * be marshalled. See the package documentation for more information about JAXB and interfaces.
 *
 * <p>Note that a class of the same name is defined in the {@link org.apache.sis.internal.jaxb.metadata}
 * package, which serve the same purpose (wrapping exactly the same interface) but using the ISO 19139
 * syntax instead. The ISO 19139 syntax represents the code and codespace as XML elements, while in this
 * GML representation the code is a XML value and the codespace is a XML attribute.</p>
 *
 * {@section Marshalling}
 * Identifiers are typically marshalled as below:
 *
 * {@preformat xml
 *   <gml:identifier codeSpace="EPSG">4326</gml:identifier>
 * }
 *
 * If the {@code ReferenceIdentifier} to marshal contains a {@linkplain ReferenceIdentifier#getVersion() version},
 * then this adapter concatenates the version to the codespace in a "URI-like" way like below:
 *
 * {@preformat xml
 *   <gml:identifier codeSpace="EPSG:8.3">4326</gml:identifier>
 * }
 *
 * {@section Unmarshalling}
 * Some data producers put a URN instead than a simple code value, as in the example below:
 *
 * {@preformat xml
 *   <gml:identifier codeSpace="OGP">urn:ogc:def:crs:EPSG::4326</gml:identifier>
 * }
 *
 * In such case this class takes the codespace as the {@linkplain ReferenceIdentifier#getAuthority() authority}
 * ("OGP" in above example), and the 3 last URI elements are parsed as the codespace, version (optional) and
 * code values respectively.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.00)
 * @version 0.4
 * @module
 */
public final class RS_Identifier extends XmlAdapter<RS_Identifier.Value, ReferenceIdentifier> {
    /**
     * The wrapper for GML identifier marshalled as a code value with a codespace attribute.
     * Defined in a separated class because JAXB does not allow usage of {@code XmlValue} in
     * a class that inherit an other class.
     */
    public static final class Value {
        /**
         * The identifier code.
         *
         * <p><b>Note:</b> GML (the target of this class) represents that code as an XML value, while
         * {@link org.apache.sis.metadata.iso.ImmutableIdentifier} represents it as an XML element.</p>
         */
        @XmlValue
        String code;

        /**
         * The code space, which is often {@code "EPSG"} with the version in use.
         *
         * <p><b>Note:</b> GML (the target of this class) represents that code as an XML attribute, while
         * {@link org.apache.sis.metadata.iso.ImmutableIdentifier} represents it as an XML element.</p>
         */
        @XmlAttribute
        String codeSpace;

        /**
         * Empty constructor for JAXB only.
         */
        public Value() {
        }

        /**
         * Creates a wrapper initialized to the values of the given identifier.
         * Version number, if presents, will be appended after the codespace with a semicolon separator.
         * The {@link #getIdentifier()} method shall be able to perform the opposite operation (split the
         * above in separated codespace and version attributes).
         *
         * @param identifier The identifier from which to get the values.
         */
        Value(final ReferenceIdentifier identifier) {
            code      = identifier.getCode();
            codeSpace = identifier.getCodeSpace();
            String version = identifier.getVersion();
            if (version != null) {
                final StringBuilder buffer = new StringBuilder();
                if (codeSpace != null) {
                    buffer.append(codeSpace);
                }
                codeSpace = buffer.append(URIParser.SEPARATOR).append(version).toString();
            }
        }

        /**
         * Returns the identifier for this value. This method is the converse of the constructor.
         * If the {@link #codeSpace} contains a semicolon, then the part after the last semicolon
         * will be taken as the authority version number. This is for consistency with what the
         * constructor does.
         */
        ReferenceIdentifier getIdentifier() {
            String c = code;
            if (c == null) {
                return null;
            }
            Citation authority = null;
            String version = null, cs = codeSpace;
            final URIParser parsed = URIParser.parse(c);
            if (parsed != null) {
                authority = Citations.fromName(cs); // May be null.
                cs        = parsed.authority;
                version   = parsed.version;
                c         = parsed.code;
            } else if (cs != null) {
                final int s = cs.lastIndexOf(URIParser.SEPARATOR);
                if (s >= 0) {
                    version = cs.substring(s+1);
                    cs = cs.substring(0, s);
                }
                authority = Citations.fromName(cs);
            }
            return new ImmutableIdentifier(authority, cs, c, version, null);
        }
    }

    /**
     * Substitutes the wrapper value read from an XML stream by the object which will
     * represents the identifier. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value The wrapper for this metadata value.
     * @return An identifier which represents the value.
     */
    @Override
    public ReferenceIdentifier unmarshal(final Value value) {
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
    public Value marshal(final ReferenceIdentifier value) {
        return (value != null) ? new Value(value) : null;
    }
}
