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
package org.apache.sis.metadata.iso.extent;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.GeographicDescription;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.iso.Types;


/**
 * Description of the geographic area using identifiers.
 * The area is given by a {@linkplain #getGeographicIdentifier() geographic identifier},
 * which may be a code in the codespace of some authority (for example an EPSG code).
 * In addition, the geographic identifier can optionally have a
 * {@linkplain DefaultIdentifier#getDescription() natural language description}.
 *
 * <div class="note"><b>Example:</b>
 * a geographic area may be identified by the {@code 1731} code in the {@code EPSG} codespace.
 * The natural language description for {@code EPSG:1731} can be <cite>“France – mainland north of 48.15°N”</cite>.
 * </div>
 *
 * <p><b>Limitations:</b></p>
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
 * @version 0.6
 * @module
 */
@XmlType(name = "EX_GeographicDescription_Type")
@XmlRootElement(name = "EX_GeographicDescription")
public class DefaultGeographicDescription extends AbstractGeographicExtent implements GeographicDescription {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7876194854687370299L;

    /**
     * The identifier used to represent a geographic area.
     */
    private Identifier geographicIdentifier;

    /**
     * Constructs an initially empty geographic description.
     */
    public DefaultGeographicDescription() {
    }

    /**
     * Creates an inclusive geographic description initialized to the given identifier.
     * This constructor sets the {@linkplain #getInclusion() inclusion} property to {@code true}.
     *
     * <p><b>Usage note:</b> if the description is a sentence like “Netherlands offshore”, it may not be suitable
     * for the {@code code} argument. Callers may consider using the {@linkplain DefaultIdentifier#getDescription()
     * identifier description} as an alternative and keep the code for a more compact string (often a primary key).</p>
     *
     * <div class="note"><b>Example:</b>
     * <code>new DefaultGeographicDescription({@link org.apache.sis.metadata.iso.citation.Citations#EPSG}, "1731")</code>
     * can stand for <cite>“France – mainland north of 48.15°N”</cite>.</div>
     *
     * @param authority The authority of the identifier code, or {@code null} if none.
     * @param code The identifier code used to represent a geographic area, or {@code null} if none.
     */
    public DefaultGeographicDescription(final Citation authority, final String code) {
        super(true);
        if (authority != null || code != null) {
            geographicIdentifier = new DefaultIdentifier(authority, code);
        }
    }

    /**
     * Creates an inclusive geographic description initialized to the given natural language description.
     * This constructor sets the {@linkplain #getInclusion() inclusion} property to {@code true} and the
     * {@linkplain DefaultIdentifier#getCode() identifier code} to one of the following choices:
     *
     * <ul>
     *   <li>the given {@code description} string if it is a valid
     *       {@linkplain CharSequences#isUnicodeIdentifier(CharSequence) Unicode identifier},</li>
     *   <li>otherwise an {@linkplain CharSequences#camelCaseToAcronym(CharSequence) acronym}
     *       of the given {@code description}.</li>
     * </ul>
     *
     * @param description The natural language description of the meaning of the code value, or {@code null} if none.
     *
     * @since 0.6
     */
    public DefaultGeographicDescription(final CharSequence description) {
        super(true);
        if (description != null) {
            final DefaultIdentifier id = new DefaultIdentifier();
            if (CharSequences.isUnicodeIdentifier(description)) {
                id.setCode(description.toString());
                if (description instanceof InternationalString) {
                    id.setDescription((InternationalString) description);
                }
            } else {
                id.setCode(CharSequences.camelCaseToAcronym(description).toString());
                id.setDescription(Types.toInternationalString(description));
            }
            geographicIdentifier = id;
        }
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(GeographicDescription)
     */
    public DefaultGeographicDescription(final GeographicDescription object) {
        super(object);
        if (object != null) {
            geographicIdentifier = object.getGeographicIdentifier();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultGeographicDescription}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultGeographicDescription} instance is created using the
     *       {@linkplain #DefaultGeographicDescription(GeographicDescription) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultGeographicDescription castOrCopy(final GeographicDescription object) {
        if (object == null || object instanceof DefaultGeographicDescription) {
            return (DefaultGeographicDescription) object;
        }
        return new DefaultGeographicDescription(object);
    }

    /**
     * Returns the identifier used to represent a geographic area.
     *
     * <div class="note"><b>Example:</b>
     * an identifier with the following properties:
     * <ul>
     *   <li>the {@code "EPSG"} code space,</li>
     *   <li>the {@code "1731"} code, and</li>
     *   <li>the <cite>“France – mainland north of 48.15°N”</cite> description.</li>
     * </ul></div>
     *
     * @return The identifier used to represent a geographic area, or {@code null}.
     */
    @Override
    @XmlElement(name = "geographicIdentifier", required = true)
    public Identifier getGeographicIdentifier() {
        return geographicIdentifier;
    }

    /**
     * Sets the identifier used to represent a geographic area.
     *
     * @param newValue The new geographic identifier.
     */
    public void setGeographicIdentifier(final Identifier newValue) {
        checkWritePermission();
        geographicIdentifier = newValue;
    }
}
