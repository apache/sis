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

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.annotation.UML;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.Citations;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Value uniquely identifying an object within a namespace.
 * One or more {@code Identifier} instances can be associated to some metadata objects like
 * {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultOperation operation},
 * {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlatform platform},
 * {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultInstrument instrument},
 * {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultEvent event},
 * {@linkplain org.apache.sis.metadata.iso.lineage.DefaultProcessing processing},
 * {@linkplain org.apache.sis.metadata.iso.lineage.DefaultSource source},
 * {@linkplain org.apache.sis.metadata.iso.content.DefaultImageDescription image description},
 * {@linkplain org.apache.sis.metadata.iso.extent.DefaultGeographicDescription geographic description}
 * and more.
 *
 * <p>Referencing objects like
 * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis coordinate system axis},
 * {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum geodetic datum},
 * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS} and more
 * rather use the {@link ImmutableIdentifier} implementation, which is a class unrelated to the usual
 * {@code org.apache.metadata} hierarchy because of the immutable nature of referencing objects.</p>
 *
 * <div class="section">Text, URN and XML representations</div>
 * The XML representation of {@link DefaultIdentifier} is similar to the {@link ImmutableIdentifier}
 * one except for the {@code "MD_"} prefix. Example:
 *
 * {@preformat xml
 *   <gmd:MD_Identifier
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
 *   </gmd:MD_Identifier>
 * }
 *
 * <div class="section">Limitations</div>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see ImmutableIdentifier
 * @see org.apache.sis.referencing.IdentifiedObjects#toURN(Class, Identifier)
 */
@XmlType(name = "MD_Identifier_Type", propOrder = {
    "authority",
    "code"
})
@XmlRootElement(name = "MD_Identifier")
public class DefaultIdentifier extends ISOMetadata implements Identifier {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -23375776954553866L;

    /**
     * Person or party responsible for maintenance of the namespace.
     *
     * @see #getAuthority()
     */
    private Citation authority;

    /**
     * Alphanumeric value identifying an instance in the namespace.
     *
     * @see #getCode()
     */
    private String code;

    /**
     * Identifier or namespace in which the code is valid.
     *
     * @see #getCodeSpace()
     */
    private String codeSpace;

    /**
     * Version identifier for the namespace, as specified by the code authority.
     * This version is included only when the {@linkplain #getCode code} uses versions.
     * When appropriate, the edition is identified by the effective date, coded using ISO 8601 date format.
     *
     * @see #getVersion()
     */
    private String version;

    /**
     * Natural language description of the meaning of the code value.
     *
     * @see #getDescription()
     */
    private InternationalString description;

    /**
     * Construct an initially empty identifier.
     */
    public DefaultIdentifier() {
    }

    /**
     * Creates an identifier initialized to the given code.
     *
     * @param code The alphanumeric value identifying an instance in the namespace,
     *             or {@code null} if none.
     */
    public DefaultIdentifier(final String code) {
        this.code = code;
    }

    /**
     * Creates an identifier initialized to the given authority and code.
     * This constructor automatically initializes the {@linkplain #getCodeSpace() code space} to a value inferred
     * from the given {@code authority}, if a suitable value can be found. This constructor proceeds by searching
     * for the first suitable property in the following list:
     *
     * <ol>
     *   <li>The value of {@link org.apache.sis.xml.IdentifierSpace#getName()}.</li>
     *   <li>A {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation#getIdentifiers() citation identifier}
     *       which is a valid
     *       {@linkplain org.apache.sis.util.CharSequences#isUnicodeIdentifier(CharSequence) unicode identifier}.</li>
     *   <li>Only if the citation has no identifier, a citation title or
     *       {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation#getAlternateTitles() alternate title}
     *       which is a valid
     *       {@linkplain org.apache.sis.util.CharSequences#isUnicodeIdentifier(CharSequence) unicode identifier}.</li>
     * </ol>
     *
     * @param authority The the person or party responsible for maintenance of the namespace, or {@code null} if none.
     * @param code      The alphanumeric value identifying an instance in the namespace, or {@code null} if none.
     *
     * @see org.apache.sis.metadata.iso.citation.Citations#getUnicodeIdentifier(Citation)
     */
    public DefaultIdentifier(final Citation authority, final String code) {
        this.authority = authority;
        this.code = code;
        codeSpace = Citations.getCodeSpace(authority);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Identifier)
     */
    public DefaultIdentifier(final Identifier object) {
        super(object);
        if (object != null) {
            code        = object.getCode();
            authority   = object.getAuthority();
            if (object instanceof DefaultIdentifier) {
                final DefaultIdentifier c = (DefaultIdentifier) object;
                codeSpace   = c.getCodeSpace();
                version     = c.getVersion();
                description = c.getDescription();
            } else if (object instanceof ReferenceIdentifier) {
                final ReferenceIdentifier c = (ReferenceIdentifier) object;
                codeSpace = c.getCodeSpace();
                version   = c.getVersion();
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultIdentifier}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultIdentifier} instance is created using the
     *       {@linkplain #DefaultIdentifier(Identifier) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultIdentifier castOrCopy(final Identifier object) {
        if (object == null || object instanceof DefaultIdentifier) {
            return (DefaultIdentifier) object;
        }
        return new DefaultIdentifier(object);
    }

    /**
     * Returns the person or party responsible for maintenance of the namespace.
     * The organization's abbreviation is often the same than this identifier
     * {@linkplain #getCodeSpace() code space}, but not necessarily.
     *
     * @return Person or party responsible for maintenance of the namespace, or {@code null} if not available.
     */
    @Override
    @XmlElement(name = "authority")
    public Citation getAuthority() {
        return authority;
    }

    /**
     * Sets the person or party responsible for maintenance of the namespace.
     *
     * @param newValue The new authority.
     */
    public void setAuthority(final Citation newValue) {
        checkWritePermission();
        authority = newValue;
    }

    /**
     * Returns the alphanumeric value identifying an instance in the namespace.
     * The code is optionally from a controlled list or pattern.
     *
     * <div class="note"><b>Example:</b> {@code "4326"}.</div>
     *
     * The code is mandatory according ISO specification, but this {@code DefaultIdentifier}
     * implementation does not enforce this restriction.
     *
     * @return Value identifying an instance in the namespace.
     */
    @Override
    @XmlElement(name = "code", required = true)
    public String getCode() {
        return code;
    }

    /**
     * Sets the alphanumeric value identifying an instance in the namespace.
     * Should avoid characters that are not legal in URLs.
     *
     * @param newValue The new code, or {@code null}.
     */
    public void setCode(final String newValue) {
        checkWritePermission();
        code = newValue;
    }

    /**
     * Returns the identifier or namespace in which the code is valid.
     * This is often the {@linkplain #getAuthority() authority}'s abbreviation, but not necessarily.
     *
     * <div class="note"><b>Example:</b> {@code "EPSG"}.</div>
     *
     * @return The identifier or namespace in which the code is valid, or {@code null} if none.
     *
     * @since 0.5
     */
    @UML(identifier="codeSpace", obligation=OPTIONAL, specification=ISO_19115)
    public String getCodeSpace() {
        return codeSpace;
    }

    /**
     * Sets the identifier or namespace in which the code is valid.
     *
     * @param newValue The new code space, or {@code null} if none.
     *
     * @since 0.5
     */
    public void setCodeSpace(final String newValue) {
        checkWritePermission();
        codeSpace = newValue;
    }

    /**
     * Returns the version identifier for the namespace, as specified by the code authority.
     * This version is included only when the {@linkplain #getCode() code} uses versions.
     * When appropriate, the edition is identified by the effective date, coded using ISO 8601 date format.
     *
     * <div class="note"><b>Example:</b> the version of the underlying EPSG database.</div>
     *
     * @return The version identifier for the namespace, or {@code null} if none.
     */
    @UML(identifier="version", obligation=OPTIONAL, specification=ISO_19115)
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version identifier for the namespace.
     *
     * @param newValue The new version, or {@code null} if none.
     */
    public void setVersion(final String newValue) {
        checkWritePermission();
        version = newValue;
    }

    /**
     * Returns the natural language description of the meaning of the code value.
     *
     * <div class="note"><b>Example:</b> "World Geodetic System 1984".</div>
     *
     * @return The natural language description, or {@code null} if none.
     *
     * @since 0.5
     */
    @UML(identifier="description", obligation=OPTIONAL, specification=ISO_19115)
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the natural language description of the meaning of the code value.
     *
     * @param newValue The new natural language description, or {@code null} if none.
     *
     * @since 0.5
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }
}
