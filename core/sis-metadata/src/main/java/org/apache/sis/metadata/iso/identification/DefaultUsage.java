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
package org.apache.sis.metadata.iso.identification;

import java.util.Date;
import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.annotation.UML;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.identification.Usage;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.iso.Types;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;
import static org.apache.sis.internal.metadata.MetadataUtilities.toDate;
import static org.apache.sis.internal.metadata.MetadataUtilities.toMilliseconds;


/**
 * Brief description of ways in which the resource(s) is/are currently or has been used.
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
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_Usage_Type", propOrder = {
    "specificUsage",
    "usageDate",
    "userDeterminedLimitations",
    "userContactInfo"
})
@XmlRootElement(name = "MD_Usage")
public class DefaultUsage extends ISOMetadata implements Usage {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 7464000583573398579L;

    /**
     * Brief description of the resource and/or resource series usage.
     */
    private InternationalString specificUsage;

    /**
     * Date and time of the first use or range of uses of the resource and/or resource series.
     * Values are milliseconds elapsed since January 1st, 1970,
     * or {@link Long#MIN_VALUE} if this value is not set.
     */
    private long usageDate = Long.MIN_VALUE;

    /**
     * Applications, determined by the user for which the resource and/or resource series
     * is not suitable.
     */
    private InternationalString userDeterminedLimitations;

    /**
     * Identification of and means of communicating with person(s) and organization(s)
     * using the resource(s).
     */
    private Collection<ResponsibleParty> userContactInfo;

    /**
     * Responses to the user-determined limitations.
     */
    private Collection<InternationalString> responses;

    /**
     * Publication that describe usage of data.
     */
    private Collection<Citation> additionalDocumentation;

    /**
     * Citation of a description of known issues associated with the resource
     * along with proposed solutions if available.
     */
    private Collection<Citation> identifiedIssues;

    /**
     * Constructs an initially empty usage.
     */
    public DefaultUsage() {
    }

    /**
     * Creates an usage initialized to the specified values.
     *
     * @param specificUsage   Brief description of the resource and/or resource series usage, or {@code null} if none.
     * @param userContactInfo Means of communicating with person(s) and organization(s), or {@code null} if none.
     */
    public DefaultUsage(final CharSequence specificUsage,
                        final ResponsibleParty userContactInfo)
    {
        this.specificUsage   = Types.toInternationalString(specificUsage);
        this.userContactInfo = singleton(userContactInfo, ResponsibleParty.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Usage)
     */
    public DefaultUsage(final Usage object) {
        super(object);
        if (object != null) {
            specificUsage             = object.getSpecificUsage();
            usageDate                 = toMilliseconds(object.getUsageDate());
            userDeterminedLimitations = object.getUserDeterminedLimitations();
            userContactInfo           = copyCollection(object.getUserContactInfo(), ResponsibleParty.class);
            if (object instanceof DefaultUsage) {
                final DefaultUsage c = (DefaultUsage) object;
                responses                 = copyCollection(c.getResponses(), InternationalString.class);
                additionalDocumentation   = copyCollection(c.getAdditionalDocumentation(), Citation.class);
                identifiedIssues          = copyCollection(c.getIdentifiedIssues(), Citation.class);
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
     *       {@code DefaultUsage}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultUsage} instance is created using the
     *       {@linkplain #DefaultUsage(Usage) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultUsage castOrCopy(final Usage object) {
        if (object == null || object instanceof DefaultUsage) {
            return (DefaultUsage) object;
        }
        return new DefaultUsage(object);
    }

    /**
     * Returns a brief description of the resource and/or resource series usage.
     *
     * @return Description of the resource usage, or {@code null}.
     */
    @Override
    @XmlElement(name = "specificUsage", required = true)
    public InternationalString getSpecificUsage() {
        return specificUsage;
    }

    /**
     * Sets a brief description of the resource and/or resource series usage.
     *
     * @param newValue The new specific usage.
     */
    public void setSpecificUsage(final InternationalString newValue) {
        checkWritePermission();
        specificUsage = newValue;
    }

    /**
     * Returns the date and time of the first use or range of uses of the resource and/or resource series.
     *
     * @return Date of the first use of the resource, or {@code null}.
     */
    @Override
    @XmlElement(name = "usageDateTime")
    public Date getUsageDate() {
        return toDate(usageDate);
    }

    /**
     * Sets the date and time of the first use.
     *
     * @param newValue The new usage date.
     */
    public void setUsageDate(final Date newValue)  {
        checkWritePermission();
        usageDate = toMilliseconds(newValue);
    }

    /**
     * Returns applications, determined by the user for which the resource and/or resource series is not suitable.
     *
     * @return Applications for which the resource and/or resource series is not suitable, or {@code null}.
     */
    @Override
    @XmlElement(name = "userDeterminedLimitations")
    public InternationalString getUserDeterminedLimitations() {
        return userDeterminedLimitations;
    }

    /**
     * Sets applications, determined by the user for which the resource and/or resource series is not suitable.
     *
     * @param newValue The new user determined limitations.
     */
    public void setUserDeterminedLimitations(final InternationalString newValue) {
        checkWritePermission();
        this.userDeterminedLimitations = newValue;
    }

    /**
     * Returns identification of and means of communicating with person(s) and organization(s) using the resource(s).
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@link Responsibility} parent interface.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @return Means of communicating with person(s) and organization(s) using the resource(s).
     */
    @Override
    @XmlElement(name = "userContactInfo", required = true)
    public Collection<ResponsibleParty> getUserContactInfo() {
        return userContactInfo = nonNullCollection(userContactInfo, ResponsibleParty.class);
    }

    /**
     * Sets identification of and means of communicating with person(s) and organization(s) using the resource(s).
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@link Responsibility} parent interface.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @param newValues The new user contact info.
     */
    public void setUserContactInfo(final Collection<? extends ResponsibleParty> newValues) {
        userContactInfo = writeCollection(newValues, userContactInfo, ResponsibleParty.class);
    }

    /**
     * Responses to the user-determined limitations.
     *
     * @return Response to the user-determined limitations.
     *
     * @since 0.5
     */
/// @XmlElement(name = "response")
    @UML(identifier="response", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<? extends InternationalString> getResponses() {
        return responses = nonNullCollection(responses, InternationalString.class);
    }

    /**
     * Sets a new response to the user-determined limitations.
     *
     * @param newValues The new response to the user-determined limitations.
     *
     * @since 0.5
     */
    public void setResponses(final Collection<? extends InternationalString> newValues) {
        responses = writeCollection(newValues, responses, InternationalString.class);
    }

    /**
     * Publications that describe usage of data.
     *
     * @return Publications that describe usage of data.
     *
     * @since 0.5
     */
/// @XmlElement(name = "additionalDocumentation")
    @UML(identifier="additionalDocumentation", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Citation> getAdditionalDocumentation() {
        return additionalDocumentation = nonNullCollection(additionalDocumentation, Citation.class);
    }

    /**
     * Sets the publications that describe usage of data.
     *
     * @param newValues The new publications.
     *
     * @since 0.5
     */
    public void setAdditionalDocumentation(final Collection<? extends Citation> newValues) {
        additionalDocumentation = writeCollection(newValues, additionalDocumentation, Citation.class);
    }

    /**
     * Citation of a description of known issues associated with the resource
     * along with proposed solutions if available.
     *
     * @return Citation of a description of known issues associated with the resource.
     *
     * @since 0.5
     */
/// @XmlElement(name = "identifiedIssues")
    @UML(identifier="identifiedIssues", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<? extends Citation> getIdentifiedIssues() {
        return identifiedIssues = nonNullCollection(identifiedIssues, Citation.class);
    }

    /**
     * Sets a new citation of a description of known issues associated with the resource
     * along with proposed solutions if available.
     *
     * @param newValues The new citation of a description.
     *
     * @since 0.5
     */
    public void setIdentifiedIssues(final Collection<? extends Citation> newValues) {
        identifiedIssues = writeCollection(newValues, identifiedIssues, Citation.class);
    }
}
