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
import java.util.logging.Level;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Locales;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.jaxb.metadata.CI_Citation;
import org.apache.sis.internal.jaxb.metadata.ReferenceSystemMetadata;
import org.apache.sis.internal.jaxb.gco.StringAdapter;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.opengis.referencing.IdentifiedObject.REMARKS_KEY;

// Related to JDK7
import org.apache.sis.internal.jdk7.Objects;


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
 * @version 0.3
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
        ensureNonNull("code", code);
        this.code      = code;
        this.codeSpace = codeSpace;
        this.authority = authority;
        this.version   = version;
        this.remarks   = remarks;
    }

    /**
     * Constructs an identifier from a set of properties. Keys are strings from the table below.
     * Keys are case-insensitive, and leading and trailing spaces are ignored. The map given in
     * argument shall contains at least a {@code "code"} property. Other properties listed in
     * the table below are optional.
     *
     * <table class="sis">
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Value given to</th>
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
        Object code      = null;
        Object codeSpace = null;
        Object version   = null;
        Object authority = null;
        Object remarks   = null;
        DefaultInternationalString localized = null;
        /*
         * Iterate through each map entry. This have two purposes:
         *
         *   1) Ignore case (a call to properties.get("foo") can't do that)
         *   2) Find localized remarks.
         *
         * This algorithm is sub-optimal if the map contains a lot of entries of no interest to
         * this identifier. Hopefully, most users will fill a map with only useful entries.
         */
        for (final Map.Entry<String,?> entry : properties.entrySet()) {
            String key   = entry.getKey().trim().toLowerCase();
            Object value = entry.getValue();
            /*switch (key)*/ { // This is a "string in switch" on the JDK7 branch.
                if (key.equals(CODE_KEY)) {
                    code = value;
                    continue;
                }
                else if (key.equals(CODESPACE_KEY)) {
                    codeSpace = value;
                    continue;
                }
                else if (key.equals(VERSION_KEY)) {
                    version = value;
                    continue;
                }
                else if (key.equals(AUTHORITY_KEY)) {
                    if (value instanceof String) {
                        value = Citations.fromName((String) value);
                    }
                    authority = value;
                    continue;
                }
                else if (key.equals(REMARKS_KEY)) {
                    if (value instanceof String) {
                        value = new SimpleInternationalString((String) value);
                    }
                    remarks = value;
                    continue;
                }
            }
            /*
             * Search for additional locales (e.g. "remarks_fr").
             */
            final Locale locale = Locales.parseSuffix(REMARKS_KEY, key);
            if (locale != null) {
                if (localized == null) {
                    if (remarks instanceof DefaultInternationalString) {
                        localized = (DefaultInternationalString) remarks;
                    } else {
                        localized = new DefaultInternationalString();
                        if (remarks instanceof CharSequence) { // String or InternationalString.
                            localized.add(Locale.ROOT, remarks.toString());
                            remarks = null;
                        }
                    }
                }
                localized.add(locale, (String) value);
            }
        }
        /*
         * Get the localized remarks, if it was not yet set. If a user specified remarks
         * both as InternationalString and as String for some locales (which is a weird
         * usage...), then current implementation discards the later with a warning.
         */
        if (localized != null && !localized.getLocales().isEmpty()) {
            if (remarks == null) {
                remarks = localized;
            } else {
                Logging.log(ImmutableIdentifier.class, "<init>",
                    Messages.getResources(null).getLogRecord(Level.WARNING, Messages.Keys.LocalesDiscarded));
            }
        }
        /*
         * Complete the code space if it was not explicitly set. We take the first
         * identifier if there is any, otherwise we take the shortest title.
         */
        if (codeSpace == null && authority instanceof Citation) {
            codeSpace = Citations.getIdentifier((Citation) authority);
        }
        /*
         * Store the definitive reference to the attributes. Note that casts are performed only
         * there (not before). This is a wanted feature, since we want to catch ClassCastExceptions
         * and rethrown them as more informative exceptions.
         */
        String key   = null;
        Object value = null;
        try {
            key=      CODE_KEY; this.code      = (String)              (value = code);
            key=   VERSION_KEY; this.version   = (String)              (value = version);
            key= CODESPACE_KEY; this.codeSpace = (String)              (value = codeSpace);
            key= AUTHORITY_KEY; this.authority = (Citation)            (value = authority);
            key=   REMARKS_KEY; this.remarks   = (InternationalString) (value = remarks);
        } catch (ClassCastException exception) {
            final InvalidParameterValueException e = new InvalidParameterValueException(
                    Errors.format(Errors.Keys.IllegalArgumentValue_2, key, value), key, value);
            e.initCause(exception);
            throw e;
        }
        ensureNonNull(CODE_KEY, code);
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
        return ReferenceSystemMetadata.toString("IDENTIFIER", authority, codeSpace, code, isDeprecated());
    }
}
