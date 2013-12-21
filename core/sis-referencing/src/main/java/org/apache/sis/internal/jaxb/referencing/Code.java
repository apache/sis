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

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAttribute;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.util.DefinitionURI;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The {@code gml:CodeType}, which is made of a code space and a code value.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.00)
 * @version 0.4
 * @module
 */
@XmlType(name = "CodeType")
public final class Code {
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
    public Code() {
    }

    /**
     * Creates a wrapper initialized to the values of the given identifier.
     * Version number, if presents, will be appended after the codespace with a semicolon separator.
     * The {@link #getIdentifier()} method shall be able to perform the opposite operation (split the
     * above in separated codespace and version attributes).
     *
     * @param identifier The identifier from which to get the values.
     */
    Code(final ReferenceIdentifier identifier) {
        code      = identifier.getCode();
        codeSpace = identifier.getCodeSpace();
        String version = identifier.getVersion();
        if (version != null) {
            final StringBuilder buffer = new StringBuilder();
            if (codeSpace != null) {
                buffer.append(codeSpace);
            }
            codeSpace = buffer.append(DefinitionURI.SEPARATOR).append(version).toString();
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
        final DefinitionURI parsed = DefinitionURI.parse(c);
        if (parsed != null) {
            authority = Citations.fromName(cs); // May be null.
            cs        = parsed.authority;
            version   = parsed.version;
            c         = parsed.code;
        } else if (cs != null) {
            final int s = cs.lastIndexOf(DefinitionURI.SEPARATOR);
            if (s >= 0) {
                version = cs.substring(s+1);
                cs = cs.substring(0, s);
            }
            authority = Citations.fromName(cs);
        }
        return new ImmutableIdentifier(authority, cs, c, version, null);
    }
}
