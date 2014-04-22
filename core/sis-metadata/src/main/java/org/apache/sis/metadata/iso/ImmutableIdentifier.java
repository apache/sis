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
package org.apache.sis.metadata.iso;

import java.util.Map;
import java.util.Locale;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.Types;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.metadata.ReferencingUtilities;
import org.apache.sis.internal.util.DefinitionURI;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.ElementKind;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.CharSequences.trimWhitespaces;
import static org.apache.sis.util.collection.Containers.property;
import static org.opengis.referencing.IdentifiedObject.REMARKS_KEY;

// Related to JDK7
import org.apache.sis.internal.jdk7.Objects;


/**
 * Immutable value uniquely identifying an object within a namespace, together with a version.
 * This kind of identifier is primarily used for identification of
 * {@link org.opengis.referencing.crs.CoordinateReferenceSystem} objects.
 *
 *
 * {@section Immutability and thread safety}
 * This class is immutable and thus inherently thread-safe if the {@link Citation} and {@link InternationalString}
 * arguments given to the constructor are also immutable. It is caller's responsibility to ensure that those
 * conditions hold, for example by invoking {@link org.apache.sis.metadata.iso.citation.DefaultCitation#freeze()
 * DefaultCitation.freeze()} before passing the arguments to the constructor.
 * Subclasses shall make sure that any overridden methods remain safe to call from multiple threads and do not change
 * any public {@code ImmutableIdentifier} state.
 *
 *
 * {@section Text, URN and XML representations}
 * Identifiers are represented in various ways depending on the context. In particular identifiers are
 * marshalled differently depending on whether they appear in a metadata object or a referencing object.
 * The following examples show an identifier for a Geographic Coordinate Reference System (CRS)
 * identified by code 4326 in the "EPSG" code space:
 *
 * <ul><li><p><b><cite>Well Known Text</cite> (WKT) version 1</b></p>
 * The WKT 1 format contains only the {@linkplain #getCodeSpace() code space} and the {@linkplain #getCode() code}.
 * If there is no code space, then the {@linkplain #getAuthority() authority} abbreviation is used as a fallback.
 * Example:
 *
 * {@preformat wkt
 *   AUTHORITY["EPSG", "4326"]
 * }
 *
 * </li><li><p><b><cite>Well Known Text</cite> (WKT) version 2</b></p>
 * The WKT 2 format contains the {@linkplain #getCodeSpace() code space}, the {@linkplain #getCode() code},
 * the {@linkplain #getVersion() version} and the {@linkplain #getAuthority() authority} citation if available.
 * The WKT can optionally provides a {@code URI} element, which expresses the same information in a different way
 * (the URN syntax is described in the next item below).
 * Example:
 *
 * {@preformat wkt
 *   ID["EPSG", 4326, URI["urn:ogc:def:crs:EPSG::4326"]]
 * }
 *
 * </li><li><p><b>XML in referencing objects</b></p>
 * The <cite>Definition identifier URNs in OGC namespace</cite> paper defines a syntax for identifiers commonly
 * found in Geographic Markup Language (GML) documents. Example:
 *
 * {@preformat xml
 *   <gml:identifier codeSpace="OGP">urn:ogc:def:crs:EPSG::4326</gml:identifier>
 * }
 *
 * In Apache SIS, the GML {@code codeSpace} attribute - despite its name - is mapped to the identifier
 * {@linkplain #getAuthority() authority}. The components of the URN value are mapped as below:
 *
 * <blockquote><code>
 * urn:ogc:def:&lt;type&gt;:&lt;{@linkplain #getCodeSpace() codespace}&gt;:&lt;{@linkplain #getVersion() version}&gt;:&lt;{@linkplain #getCode() code}&gt;
 * </code></blockquote>
 *
 * </li><li><p><b>XML in metadata objects</b></p>
 * The XML representation of {@link ImmutableIdentifier} in a metadata is similar to the {@link DefaultIdentifier}
 * one except for the {@code "RS_"} prefix:
 *
 * {@preformat xml
 *   <gmd:RS_Identifier>
 *     <gmd:code>
 *       <gco:CharacterString>4326</gco:CharacterString>
 *     </gmd:code>
 *     <gmd:authority>
 *       <gmd:CI_Citation>
 *         <gmd:title>
 *           <gco:CharacterString>EPSG</gco:CharacterString>
 *         </gmd:title>
 *       </gmd:CI_Citation>
 *     </gmd:authority>
 *   </gmd:RS_Identifier>
 * }
 * </li></ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.4
 * @module
 *
 * @see DefaultIdentifier
 */
@XmlRootElement(name = "RS_Identifier")
public class ImmutableIdentifier extends FormattableObject implements ReferenceIdentifier, Deprecable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7681717592582493409L;

    /**
     * Identifier code or name, optionally from a controlled list or pattern defined by a code space.
     *
     * @see #getCode()
     */
    @XmlElement(required = true)
    private final String code;

    /**
     * Name or identifier of the person or organization responsible for namespace, or
     * {@code null} if not available. This is often an abbreviation of the authority name.
     *
     * @see #getCodeSpace()
     */
    @XmlElement(required = true)
    private final String codeSpace;

    /**
     * Organization or party responsible for definition and maintenance of the code space or code,
     * or {@code null} if not available.
     *
     * @see #getAuthority()
     */
    @XmlElement(required = true)
    private final Citation authority;

    /**
     * Identifier of the version of the associated code space or code as specified
     * by the code space or code authority, or {@code null} if not available. This
     * version is included only when the {@linkplain #getCode() code} uses versions.
     * When appropriate, the edition is identified by the effective date, coded using
     * ISO 8601 date format.
     *
     * @see #getVersion()
     */
    @XmlElement
    private final String version;

    /**
     * Comments on or information about this identifier, or {@code null} if none.
     *
     * @see #getRemarks()
     */
    private final InternationalString remarks;

    /**
     * Empty constructor for JAXB.
     */
    private ImmutableIdentifier() {
        code      = null;
        codeSpace = null;
        authority = null;
        version   = null;
        remarks   = null;
    }

    /**
     * Creates a new identifier from the specified one. This is a copy constructor
     * which will get the code, codespace, authority, version and (if available)
     * the remarks from the given identifier.
     *
     * @param identifier The identifier to copy.
     */
    public ImmutableIdentifier(final ReferenceIdentifier identifier) {
        ensureNonNull("identifier", identifier);
        code      = identifier.getCode();
        codeSpace = identifier.getCodeSpace();
        authority = identifier.getAuthority();
        version   = identifier.getVersion();
        if (identifier instanceof Deprecable) {
            remarks = ((Deprecable) identifier).getRemarks();
        } else {
            remarks = null;
        }
        validate(null);
    }

    /**
     * Creates a new identifier from the specified code and authority.
     *
     * @param authority
     *          Organization or party responsible for definition and maintenance of the code
     *          space or code.
     * @param codeSpace
     *          Name or identifier of the person or organization responsible for namespace.
     *          This is often an abbreviation of the authority name.
     * @param code
     *          Identifier code or name, optionally from a controlled list or pattern defined by
     *          a code space. The code can not be null.
     */
    public ImmutableIdentifier(final Citation authority, final String codeSpace, final String code) {
        this(authority, codeSpace, code, null, null);
    }

    /**
     * Creates a new identifier from the specified code and authority,
     * with an optional version number and remarks.
     *
     * @param authority
     *          Organization or party responsible for definition and maintenance of the code
     *          space or code, or {@code null} if not available.
     * @param codeSpace
     *          Name or identifier of the person or organization responsible for namespace, or
     *          {@code null} if not available. This is often an abbreviation of the authority name.
     * @param code
     *          Identifier code or name, optionally from a controlled list or pattern defined by
     *          a code space. The code can not be null.
     * @param version
     *          The version of the associated code space or code as specified by the code authority,
     *          or {@code null} if none.
     * @param remarks
     *          Comments on or information about this identifier, or {@code null} if none.
     */
    public ImmutableIdentifier(final Citation authority, final String codeSpace,
            final String code, final String version, final InternationalString remarks)
    {
        this.code      = code;
        this.codeSpace = codeSpace;
        this.authority = authority;
        this.version   = version;
        this.remarks   = remarks;
        validate(null);
    }

    /**
     * Constructs an identifier from the given properties. Keys are strings from the table below.
     * The map given in argument shall contain an entry at least for the
     * {@value org.opengis.metadata.Identifier#CODE_KEY} key.
     * Other properties listed in the table below are optional.
     *
     * <table class="sis">
     *   <caption>Recognized properties</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.metadata.Identifier#CODE_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link #getCode()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceIdentifier#CODESPACE_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link #getCodeSpace()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.metadata.Identifier#AUTHORITY_KEY}</td>
     *     <td>{@link String} or {@link Citation}</td>
     *     <td>{@link #getAuthority()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceIdentifier#VERSION_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link #getVersion()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link String} or {@link InternationalString}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.referencing.AbstractIdentifiedObject#LOCALE_KEY}</td>
     *     <td>{@link Locale}</td>
     *     <td>(none)</td>
     *   </tr>
     * </table>
     *
     * {@section Localization}
     * {@code "remarks"} is a localizable attributes which may have a language and country
     * code suffix. For example the {@code "remarks_fr"} property stands for remarks in
     * {@linkplain Locale#FRENCH French} and the {@code "remarks_fr_CA"} property stands
     * for remarks in {@linkplain Locale#CANADA_FRENCH French Canadian}.
     *
     * <p>The {@code "locale"} property applies only to exception messages, if any.
     * After successful construction, {@code ImmutableIdentifier} instances do not keep the locale
     * since localizations are deferred to the {@link InternationalString#toString(Locale)} method.</p>
     *
     * @param  properties The properties to be given to this identifier.
     * @throws InvalidParameterValueException if a property has an invalid value.
     * @throws IllegalArgumentException if a property is invalid for some other reason.
     */
    public ImmutableIdentifier(final Map<String,?> properties) throws IllegalArgumentException {
        ensureNonNull("properties", properties);
        code    = trimWhitespaces(property(properties, CODE_KEY,    String.class));
        version = trimWhitespaces(property(properties, VERSION_KEY, String.class));
        remarks = Types.toInternationalString(properties, REMARKS_KEY);
        /*
         * Map String authority to one of the pre-defined constants (typically EPSG or OGC).
         */
        Object value = properties.get(AUTHORITY_KEY);
        if (value instanceof String) {
            authority = Citations.fromName((String) value);
        } else if (value == null || value instanceof Citation) {
            authority = (Citation) value;
        } else {
            throw illegalPropertyType(properties, AUTHORITY_KEY, value);
        }
        /*
         * Complete the code space if it was not explicitly set. We take a short identifier (preferred) or title
         * (as a fallback), with precedence given to Unicode identifier (see Citations.getIdentifier(…) for more
         * information). Then the getCodeSpace(…) method applies additional restrictions in order to reduce the
         * risk of false code space.
         */
        value = properties.get(CODESPACE_KEY);
        if (value == null && !properties.containsKey(CODESPACE_KEY)) {
            codeSpace = getCodeSpace(authority);
        } else if (value instanceof String) {
            codeSpace = trimWhitespaces((String) value);
        } else {
            throw illegalPropertyType(properties, CODESPACE_KEY, value);
        }
        validate(properties);
    }

    /**
     * Ensures that the properties of this {@code ImmutableIdentifier} are valid.
     */
    private void validate(final Map<String,?> properties) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException(Errors.getResources(properties)
                    .getString((code == null) ? Errors.Keys.MissingValueForProperty_1
                                              : Errors.Keys.EmptyProperty_1, CODE_KEY));
        }
    }

    /**
     * Returns the exception to be thrown when a property if of illegal type.
     */
    private static IllegalArgumentException illegalPropertyType(
            final Map<String,?> properties, final String key, final Object value)
    {
        return new IllegalArgumentException(Errors.getResources(properties)
                .getString(Errors.Keys.IllegalPropertyClass_2, key, value.getClass()));
    }

    /**
     * Returns a SIS identifier implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code ImmutableIdentifier}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code ImmutableIdentifier} instance is created using the
     *       {@linkplain #ImmutableIdentifier(ReferenceIdentifier) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static ImmutableIdentifier castOrCopy(final ReferenceIdentifier object) {
        if (object == null || object instanceof ImmutableIdentifier) {
            return (ImmutableIdentifier) object;
        }
        return new ImmutableIdentifier(object);
    }

    /**
     * Identifier code or name, optionally from a controlled list or pattern.
     *
     * @return The code, never {@code null}.
     *
     * @see org.apache.sis.referencing.NamedIdentifier#tip()
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * Name or identifier of the person or organization responsible for namespace.
     *
     * @return The code space, or {@code null} if not available.
     *
     * @see org.apache.sis.referencing.NamedIdentifier#head()
     * @see org.apache.sis.referencing.NamedIdentifier#scope()
     */
    @Override
    public String getCodeSpace() {
        return codeSpace;
    }

    /**
     * Infers a code space from the given authority. First, this method takes a short identifier or title with
     * preference for Unicode identifier - see {@link Citations#getIdentifier(Citation)} for more information.
     * Next this method applies additional restrictions in order to reduce the risk of undesired code space.
     * Those restrictions are arbitrary and may change in any future SIS version. Currently, the restriction
     * is to accept only letters or digits.
     *
     * @param  authority The authority for which to get a code space.
     * @return The code space, or {@code null} if none.
     *
     * @see Citations#getIdentifier(Citation)
     */
    private static String getCodeSpace(final Citation authority) {
        final String codeSpace = Citations.getIdentifier(authority); // Whitespaces trimed by Citations.
        if (codeSpace != null) {
            final int length = codeSpace.length();
            if (length != 0) {
                int i = 0;
                do {
                    final int c = codeSpace.charAt(i);
                    if (!Character.isLetterOrDigit(c)) {
                        return null;
                    }
                    i += Character.charCount(c);
                } while (i < length);
                return codeSpace;
            }
        }
        return null;
    }

    /**
     * Organization or party responsible for definition and maintenance of the
     * {@linkplain #getCode() code}.
     *
     * <div class="note"><b>Example:</b> Coordinate Reference System (CRS) identified by an EPSG code will return
     * {@link Citations#OGP}, since OGP is the organization maintaining the EPSG geodetic database.</div>
     *
     * @return The authority, or {@code null} if not available.
     */
    @Override
    public Citation getAuthority() {
        return authority;
    }

    /**
     * Identifier of the version of the associated code space or code, as specified by the
     * code authority. This version is included only when the {@linkplain #getCode() code}
     * uses versions. When appropriate, the edition is identified by the effective date,
     * coded using ISO 8601 date format.
     *
     * @return The version, or {@code null} if not available.
     */
    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Comments on or information about this identifier, or {@code null} if none.
     *
     * @return Optional comments about this identifier, or {@code null} if none.
     */
    @Override
    public InternationalString getRemarks() {
        return remarks;
    }

    /**
     * Returns {@code true} if the object represented by this identifier is deprecated. In such
     * case, the {@linkplain #getRemarks() remarks} may contains the new identifier to use.
     *
     * <p>The default implementation returns {@code false} in all cases.</p>
     *
     * @see org.apache.sis.referencing.AbstractIdentifiedObject#isDeprecated()
     *
     * @return {@code true} if this code is deprecated.
     */
    @Override
    public boolean isDeprecated() {
        return false;
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = (int) serialVersionUID;
        if (code != null) {
            hash ^= code.hashCode();
        }
        if (codeSpace != null) {
            hash = hash*31 + codeSpace.hashCode();
        }
        return hash;
    }

    /**
     * Compares this object with the given one for equality.
     *
     * @param object The object to compare with this identifier.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            final ImmutableIdentifier that = (ImmutableIdentifier) object;
            return Objects.equals(code,      that.code)      &&
                   Objects.equals(codeSpace, that.codeSpace) &&
                   Objects.equals(authority, that.authority) &&
                   Objects.equals(version,   that.version)   &&
                   Objects.equals(remarks,   that.remarks);
        }
        return false;
    }

    /**
     * Formats this identifier as a <cite>Well Known Text</cite> {@code Id[…]} element.
     * See class javadoc for more information on the WKT format.
     *
     * @param  formatter The formatter where to format the inner content of this WKT element.
     * @return {@code "Id"} (WKT 2) or {@code "Authority"} (WKT 1).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        String keyword = null;
        if (code != null) {
            String citation = Citations.getIdentifier(authority);
            String cs = codeSpace;
            if (cs == null) {
                cs = citation;
                citation  = null;
            }
            if (cs != null) {
                final Convention convention = formatter.getConvention();
                if (convention.majorVersion() == 1) {
                    keyword = "Authority";
                    formatter.append(cs, ElementKind.IDENTIFIER);
                    formatter.append(code, ElementKind.IDENTIFIER);
                } else {
                    keyword = "Id";
                    formatter.append(cs, ElementKind.IDENTIFIER);
                    appendCode(formatter, code);
                    if (version != null) {
                        appendCode(formatter, version);
                    }
                    if (citation != null && !citation.equals(cs)) {
                        formatter.append(new Cite(citation));
                    }
                    /*
                     * Do not format the optional URI element for internal convention,
                     * because this property is currently computed rather than stored.
                     * Other conventions format only for the ID[…] of root element.
                     */
                    if (convention != Convention.INTERNAL && formatter.getEnclosingElement(2) == null) {
                        final FormattableObject parent = formatter.getEnclosingElement(1);
                        if (parent != null && ReferencingUtilities.usesURN(cs)) {
                            final String type = ReferencingUtilities.toURNType(parent.getClass());
                            if (type != null) {
                                formatter.append(new URI(type, cs, version, code));
                            }
                        }
                    }
                }
            }
        }
        return keyword;
    }

    /**
     * Appends the given code or version number as an integer if possible, or as a text otherwise.
     *
     * <div class="note"><b>Implementation note:</b>
     * ISO 19162 specifies "number or text". In Apache SIS, we restrict the numbers to integers
     * because handling version numbers like "8.2" as floating point numbers can be confusing.</div>
     */
    private static void appendCode(final Formatter formatter, final String text) {
        if (text != null) {
            final long n;
            try {
                n = Long.parseLong(text);
            } catch (NumberFormatException e) {
                formatter.append(text, ElementKind.IDENTIFIER);
                return;
            }
            formatter.append(n);
        }
    }

    /**
     * The {@code CITATION[…]} element inside an {@code ID[…]}.
     */
    private static final class Cite extends FormattableObject {
        /** The component of the citation to format. */
        private final String identifier;

        /** Creates a new citation with the given component. */
        Cite(final String identifier) {
            this.identifier = identifier;
        }

        /** Formats the citation. */
        @Override
        protected String formatTo(final Formatter formatter) {
            formatter.append(identifier, ElementKind.CITATION);
            return "Citation";
        }
    }

    /**
     * The {@code URI[…]} element inside an {@code ID[…]}.
     */
    private static final class URI extends FormattableObject {
        /** The components of the URI to format. */
        private final String type, codeSpace, version, code;

        /** Creates a new URI with the given components. */
        URI(final String type, final String codeSpace, final String version, final String code) {
            this.type      = type;
            this.codeSpace = codeSpace;
            this.version   = version;
            this.code      = code;
        }

        /** Formats the URI. */
        @Override
        protected String formatTo(final Formatter formatter) {
            final StringBuilder buffer = new StringBuilder(DefinitionURI.PREFIX)
                    .append(DefinitionURI.SEPARATOR).append(type)
                    .append(DefinitionURI.SEPARATOR).append(codeSpace)
                    .append(DefinitionURI.SEPARATOR);
            if (version != null) {
                buffer.append(version);
            }
            buffer.append(DefinitionURI.SEPARATOR).append(code);
            formatter.append(buffer.toString(), null);
            return "URI";
        }
    }
}
