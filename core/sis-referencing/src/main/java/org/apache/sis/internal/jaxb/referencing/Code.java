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
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.DefinitionURI;
import org.apache.sis.internal.metadata.NameMeaning;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;

import static org.apache.sis.internal.util.Citations.getCodeSpace;


/**
 * The {@code gml:CodeType}, which is made of a code space and a code value.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
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
     * Empty constructor for JAXB.
     */
    Code() {
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
     *
     * @return The identifier, or {@code null} if none.
     */
    public ReferenceIdentifier getIdentifier() {
        String c = code;
        if (c == null) {
            return null;
        }
        Citation authority = null;
        String version = null, cs = codeSpace;
        final DefinitionURI parsed = DefinitionURI.parse(c);
        if (parsed != null && parsed.code != null) {
            /*
             * Case where the URN has been successfully parsed. The OGC's URN contains an "authority" component,
             * which we take as the Identifier.codeSpace value (not Identifier.authority despite what the names
             * would suggest).
             *
             * The GML document may also provide a 'codeSpace' attribute separated from the URN, which we take
             * as the authority.  This is the opposite of what the names would suggest, but we can not map the
             * 'codeSpace' attribute to Identifier.codeSpace  because the 'codeSpace' attribute value found in
             * practice is often "IOGP" while the 'Identifier.description' example provided in ISO 19115-1 for
             * an EPSG code has the "EPSG" codespace. Example:
             *
             *    - XML: <gml:identifier codeSpace="IOGP">urn:ogc:def:crs:EPSG::4326</gml:identifier>
             *    - ISO: For "EPSG:4326", Identifier.codeSpace = "EPSG" and Identifier.code = "4326".
             *
             * Apache SIS attempts to organize this apparent contradiction by considering IOGP as the codespace of
             * the EPSG codespace, but this interpretation is not likely to be widely used by libraries other than
             * SIS. For now, a special handling is hard-coded below: if codeSpace = "IOGP" and authority = "EPSG",
             * then we take the authority as the Citations.EPSG constant, which has a "IOGP:EPSG" identifier.
             *
             * A symmetrical special handling for EPSG is done in the 'forIdentifiedObject(…)' method of this class.
             */
            if (org.apache.sis.internal.util.Citations.isEPSG(cs, parsed.authority)) {
                authority = Citations.EPSG;
            } else {
                authority = Citations.fromName(cs);     // May be null.
            }
            cs      = parsed.authority;
            version = parsed.version;
            c       = parsed.code;
        } else if (cs != null) {
            /*
             * Case where the URN can not be parsed but a 'codeSpace' attribute exists. We take this 'codeSpace'
             * as both the code space and the authority. As a special case, if there is a semi-colon, we take all
             * text after that semi-color as the version number.
             */
            final int s = cs.lastIndexOf(DefinitionURI.SEPARATOR);
            if (s >= 0) {
                version = cs.substring(s+1);
                cs = cs.substring(0, s);
            }
            authority = Citations.fromName(cs);
        }
        return new NamedIdentifier(authority, cs, c, version, null);
    }

    /**
     * Returns a {@code <gml:identifier>} for the given identified object, or {@code null} if none.
     * This method searches for the following identifiers, in preference order:
     * <ul>
     *   <li>The first identifier having a code that begin with {@code "urn:"}.</li>
     *   <li>The first identifier having a code that begin with {@code "http:"}.</li>
     *   <li>The first identifier in the {@code "EPSG"} codespace, converted to the {@code "urn:} syntax.</li>
     *   <li>The first identifier in other codespace, converted to the {@code "urn:} syntax if possible.</li>
     * </ul>
     *
     * @param  type The type of the identified object.
     * @param  identifiers The object identifiers, or {@code null} if none.
     * @return The {@code <gml:identifier>} as a {@code Code} instance, or {@code null} if none.
     */
    public static Code forIdentifiedObject(final Class<?> type, final Iterable<? extends ReferenceIdentifier> identifiers) {
        if (identifiers != null) {
            boolean isHTTP = false;
            boolean isEPSG = false;
            ReferenceIdentifier fallback = null;
            for (final ReferenceIdentifier identifier : identifiers) {
                final String code = identifier.getCode();
                if (code == null) continue; // Paranoiac check.
                if (code.regionMatches(true, 0, "urn:", 0, 4)) {
                    return new Code(identifier);
                }
                if (!isHTTP) {
                    isHTTP = code.regionMatches(true, 0, "http:", 0, 5);
                    if (isHTTP) {
                        fallback = identifier;
                    } else if (!isEPSG) {
                        isEPSG = Constants.EPSG.equalsIgnoreCase(identifier.getCodeSpace());
                        if (isEPSG || fallback == null) {
                            fallback = identifier;
                        }
                    }
                }
            }
            /*
             * If no "urn:" or "http:" form has been found, try to create a "urn:" form from the first identifier.
             * For example "EPSG:4326" may be converted to "urn:ogc:def:crs:EPSG:8.2:4326". If the first identifier
             * can not be converted to a "urn:" form, then it will be returned as-is.
             */
            if (fallback != null) {
                if (!isHTTP) {
                    final String urn = NameMeaning.toURN(type, fallback.getCodeSpace(), fallback.getVersion(), fallback.getCode());
                    if (urn != null) {
                        final Code code = new Code();
                        /*
                         * Rational for EPSG special case below:
                         * -------------------------------------
                         * Apache SIS already formats the Identifier.getCodeSpace() value in the URN.
                         * This value is "EPSG" for IdentifiedObject instances from the EPSG database.
                         * But GML additionally have a "codeSpace" attribute, and common usage seems to
                         * give the "OGP" or "IOGP" value to that attribute as in the following example:
                         *
                         *     <gml:identifier codeSpace="IOGP">urn:ogc:def:crs:EPSG::4326</gml:identifier>
                         *
                         * A discussion can be found in the comments of https://issues.apache.org/jira/browse/SIS-196
                         *
                         * Where to take this "IOGP" value from? It is not the Identifier.getCodeSpace() String value
                         * since ISO 19115-1 clearly uses the "EPSG" value in their example.  We could consider using
                         * the Identifier.getAuthority() value, which is a Citation. But the "EPSG" part in above URN
                         * is named "the authority" in URN specification, which suggest that Identifier.getAuthority()
                         * should return a citation for the "EPSG Geodetic Parameter Dataset" rather than for the IOGP
                         * organisation.
                         *
                         * Apache SIS declares IOGP as the codespace of the EPSG codespace, i.e. the identifier of the
                         * EPSG authority is "IOGP:EPSG". So the code below searches for the "IOGP" part of the above.
                         * However there is no indication at this time that objects from other sources than SIS would
                         * follow such convention, so we also keep a hard-coded "IOGP" default value for now.
                         *
                         * A symmetrical special handling for EPSG is done in the 'getIdentifier()' method of this class.
                         *
                         * See https://issues.apache.org/jira/browse/SIS-199
                         */
                        final Citation authority = fallback.getAuthority();
                        if (isEPSG) {
                            code.codeSpace = Constants.IOGP;    // Default value if we do not find a codespace below.
                            if (authority != null) {
                                for (final Identifier id : authority.getIdentifiers()) {
                                    if (Constants.EPSG.equalsIgnoreCase(id.getCode())) {
                                        if (id instanceof ReferenceIdentifier) {
                                            final String cs = ((ReferenceIdentifier) id).getCodeSpace();
                                            if (cs != null) {
                                                code.codeSpace = cs;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            code.codeSpace = getCodeSpace(authority);
                        }
                        code.code = urn;
                        return code;
                    }
                }
                return new Code(fallback);
            }
        }
        return null;
    }
}
