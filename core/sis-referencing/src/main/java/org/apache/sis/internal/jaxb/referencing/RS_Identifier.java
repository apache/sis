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
import org.apache.sis.util.StringBuilders;


/**
 * JAXB adapter mapping the GeoAPI {@link ReferenceIdentifier} to an implementation class that can
 * be marshalled. See the package documentation for more information about JAXB and interfaces.
 *
 * <p>The XML produced by this adapter uses the GML syntax. The {@link RS_IdentifierCode} class
 * performs a similar mapping, but in which only the code (without codespace) is marshalled.</p>
 *
 * <p>Note that a class of the same name is defined in the {@link org.apache.sis.internal.jaxb.metadata}
 * package, which serve the same purpose (wrapping exactly the same interface) but using the ISO 19139
 * syntax instead. The ISO 19139 syntax represents the code and codespace as XML elements, while in this
 * GML representation the code is a XML value and the codespace is a XML attribute.</p>
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
        private String code;

        /**
         * The code space, which is often {@code "EPSG"} with the version in use.
         *
         * <p><b>Note:</b> GML (the target of this class) represents that code as an XML attribute, while
         * {@link org.apache.sis.metadata.iso.ImmutableIdentifier} represents it as an XML element.</p>
         */
        @XmlAttribute
        private String codeSpace;

        /**
         * Empty constructor for JAXB only.
         */
        public Value() {
        }

        /**
         * Creates a wrapper initialized to the values of the given identifier.
         *
         * @param identifier The identifier from which to get the values.
         */
        Value(final ReferenceIdentifier identifier) {
            code      = identifier.getCode();
            codeSpace = identifier.getCodeSpace();
            String version = identifier.getVersion();
            if (version != null) {
                final StringBuilder buffer = new StringBuilder(codeSpace);
                if (buffer.length() != 0) {
                    buffer.append('_');
                }
                StringBuilders.remove(buffer.append('v').append(version), ".");
                codeSpace = buffer.toString();
            }
        }

        /**
         * Returns the identifier for this value. This method is the converse of the constructor.
         */
        ReferenceIdentifier getIdentifier() {
            final Citation authority = Citations.fromName(codeSpace); // May be null.
            return new ImmutableIdentifier(authority, Citations.getIdentifier(authority), code);
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
