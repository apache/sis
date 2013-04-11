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
import java.util.Collection;
import java.util.logging.Level;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import net.jcip.annotations.Immutable;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Locales;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.internal.jaxb.metadata.CI_Citation;
import org.apache.sis.internal.jaxb.metadata.ReferenceSystemMetadata;
import org.apache.sis.internal.jaxb.gco.StringAdapter;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.opengis.referencing.IdentifiedObject.REMARKS_KEY;

// Related to JDK7
import java.util.Objects;


/**
 * Unmodifiable value uniquely identifying an object within a namespace, together with a version.
 * This kind of identifier is primarily used for identification of
 * {@link org.opengis.referencing.crs.CoordinateReferenceSystem} objects.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 */
@Immutable
@XmlRootElement(name = "RS_Identifier")
public class UnmodifiableIdentifier implements ReferenceIdentifier, Deprecable, Serializable {
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
    private UnmodifiableIdentifier() {
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
    public UnmodifiableIdentifier(final ReferenceIdentifier identifier) {
        ensureNonNull("identifier", identifier);
        code      = identifier.getCode();
        codeSpace = identifier.getCodeSpace();
        authority = identifier.getAuthority();
        version   = identifier.getVersion();
        if (identifier instanceof UnmodifiableIdentifier) {
            remarks = ((UnmodifiableIdentifier) identifier).getRemarks();
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
    public UnmodifiableIdentifier(final Citation authority, final String codeSpace, final String code) {
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
    public UnmodifiableIdentifier(final Citation authority, final String codeSpace,
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
    public UnmodifiableIdentifier(final Map<String,?> properties) throws IllegalArgumentException {
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
            switch (key) {
                case CODE_KEY: {
                    code = value;
                    continue;
                }
                case CODESPACE_KEY: {
                    codeSpace = value;
                    continue;
                }
                case VERSION_KEY: {
                    version = value;
                    continue;
                }
                case AUTHORITY_KEY: {
                    if (value instanceof String) {
                        value = new SimpleCitation((String) value);
                    }
                    authority = value;
                    continue;
                }
                case REMARKS_KEY: {
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
                Logging.log(UnmodifiableIdentifier.class, "<init>",
                    Messages.getResources(null).getLogRecord(Level.WARNING, Messages.Keys.LocalesDiscarded));
            }
        }
        /*
         * Complete the code space if it was not explicitly set. We take the first
         * identifier if there is any, otherwise we take the shortest title.
         */
        if (codeSpace == null && authority instanceof Citation) {
            codeSpace = getCodeSpace((Citation) authority);
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
            throw new InvalidParameterValueException(
                    Errors.format(Errors.Keys.IllegalArgumentValue_2, key, value), exception, key, value);
        }
        ensureNonNull(CODE_KEY, code);
    }

    /**
     * Returns the shortest title inferred from the specified authority.
     * This is used both for creating a generic name, or for inferring a
     * default identifier code space.
     */
    private static InternationalString getShortestTitle(final Citation authority) {
        InternationalString title = authority.getTitle();
        int length = title.length();
        final Collection<? extends InternationalString> alt = authority.getAlternateTitles();
        if (alt != null) {
            for (final InternationalString candidate : alt) {
                final int candidateLength = candidate.length();
                if (candidateLength > 0 && candidateLength < length) {
                    title = candidate;
                    length = candidateLength;
                }
            }
        }
        return title;
    }

    /**
     * Tries to get a code space from the specified authority. This method scans first
     * through the identifier, then through the titles if no suitable identifier were found.
     */
    private static String getCodeSpace(final Citation authority) {
        if (authority != null) {
            final Collection<? extends Identifier> identifiers = authority.getIdentifiers();
            if (identifiers != null) {
                for (final Identifier id : identifiers) {
                    final String identifier = id.getCode();
                    if (CharSequences.isUnicodeIdentifier(identifier)) {
                        return identifier;
                    }
                }
            }
            final String title = getShortestTitle(authority).toString(Locale.ROOT);
            if (CharSequences.isUnicodeIdentifier(title)) {
                return title;
            }
        }
        return null;
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
     * @return Optional comments about this identifier.
     */
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
            final UnmodifiableIdentifier that = (UnmodifiableIdentifier) object;
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
