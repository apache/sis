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
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.iso.Types;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.jaxb.metadata.CI_Citation;
import org.apache.sis.internal.jaxb.gco.StringAdapter;
import org.apache.sis.internal.simple.SimpleIdentifiedObject;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.collection.Containers.property;
import static org.opengis.referencing.IdentifiedObject.REMARKS_KEY;

// Related to JDK7
import java.util.Objects;


/**
 * Immutable value uniquely identifying an object within a namespace, together with a version.
 * This kind of identifier is primarily used for identification of
 * {@link org.opengis.referencing.crs.CoordinateReferenceSystem} objects.
 *
 * {@note While <code>ImmutableIdentifier</code> objects are immutable, they may contain references to
 *        <code>Citation</code> and <code>InternationalString</code> objects which are not guaranteed
 *        to be immutable. For better safety, factory codes are encouraged to pass only immutable
 *        citations and immutable international strings to the constructors.}
 *
 * @author Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.4
 * @module
 *
 * @see DefaultIdentifier
 */
@Immutable
@XmlRootElement(name = "RS_Identifier")
public class ImmutableIdentifier implements ReferenceIdentifier, Deprecable, Serializable {
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
    @XmlJavaTypeAdapter(StringAdapter.class)
    private final String code;

    /**
     * Name or identifier of the person or organization responsible for namespace, or
     * {@code null} if not available. This is often an abbreviation of the authority name.
     *
     * @see #getCodeSpace()
     */
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(StringAdapter.class)
    private final String codeSpace;

    /**
     * Organization or party responsible for definition and maintenance of the code space or code,
     * or {@code null} if not available.
     *
     * @see #getAuthority()
     */
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CI_Citation.class)
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
        if (identifier instanceof ImmutableIdentifier) {
            remarks = ((ImmutableIdentifier) identifier).getRemarks();
        } else {
            remarks = null;
        }
        validate();
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
        validate();
    }

    /**
     * Constructs an identifier from a set of properties. Keys are strings from the table below.
     * The map given in argument shall contain an entry at least for the
     * {@value org.opengis.metadata.Identifier#CODE_KEY} key.
     * Other properties listed in the table below are optional.
     *
     * <table class="sis">
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
     * </table>
     *
     * {@code "remarks"} is a localizable attributes which may have a language and country
     * code suffix. For example the {@code "remarks_fr"} property stands for remarks in
     * {@linkplain Locale#FRENCH French} and the {@code "remarks_fr_CA"} property stands
     * for remarks in {@linkplain Locale#CANADA_FRENCH French Canadian}.
     *
     * @param  properties The properties to be given to this identifier.
     * @throws InvalidParameterValueException if a property has an invalid value.
     * @throws IllegalArgumentException if a property is invalid for some other reason.
     */
    public ImmutableIdentifier(final Map<String,?> properties) throws IllegalArgumentException {
        ensureNonNull("properties", properties);
        code      = property(properties, CODE_KEY,      String.class);
        version   = property(properties, VERSION_KEY,   String.class);
        remarks   = Types.toInternationalString(properties, REMARKS_KEY);
        /*
         * Map String authority to one of the pre-defined constants (typically EPSG or OGC).
         */
        Object value = properties.get(AUTHORITY_KEY);
        if (value instanceof String) {
            authority = Citations.fromName((String) value);
        } else if (value == null || value instanceof Citation) {
            authority = (Citation) value;
        } else {
            throw illegalPropertyType(AUTHORITY_KEY, value);
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
            codeSpace = (String) value;
        } else {
            throw illegalPropertyType(CODESPACE_KEY, value);
        }
        validate();
    }

    /**
     * Ensures that the properties of this {@code ImmutableIdentifier} are valid.
     */
    private void validate() {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException(Errors.format((code == null)
                    ? Errors.Keys.MissingValueForProperty_1
                    : Errors.Keys.EmptyProperty_1, CODE_KEY));
        }
    }

    /**
     * Returns the exception to be thrown when a property if of illegal type.
     */
    private static IllegalArgumentException illegalPropertyType(final String key, final Object value) {
        return new IllegalArgumentException(Errors.format(Errors.Keys.IllegalPropertyClass_2, key, value.getClass()));
    }

    /**
     * Returns a SIS identifier implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
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
     * @see NamedIdentifier#tip()
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
     * @see NamedIdentifier#head()
     * @see NamedIdentifier#scope()
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
        final String codeSpace = Citations.getIdentifier(authority);
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
     * {@linkplain #getCode code}.
     *
     * @return The authority, or {@code null} if not available.
     *
     * @see org.apache.sis.metadata.iso.citation.Citations#EPSG
     */
    @Override
    public Citation getAuthority() {
        return authority;
    }

    /**
     * Identifier of the version of the associated code space or code, as specified by the
     * code authority. This version is included only when the {@linkplain #getCode code}
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
     * @see AbstractIdentifiedObject#isDeprecated()
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
     * Returns a string representation of this identifier.
     * The default implementation returns a pseudo-WKT format.
     *
     * {@note The <code>NamedIdentifier</code> subclass overrides this method with a different
     *        behavior, in order to be compliant with the contract of the <code>GenericName</code>
     *        interface.}
     *
     * @see IdentifiedObjects#toString(Identifier)
     * @see NamedIdentifier#toString()
     */
    @Override
    public String toString() {
        return SimpleIdentifiedObject.toString("IDENTIFIER", authority, codeSpace, code, isDeprecated());
    }
}
