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
import java.util.List;
import java.util.Collection;
import java.time.temporal.Temporal;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Usage;
import org.opengis.temporal.TemporalPrimitive;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.gml.TM_Primitive;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.temporal.TemporalObjects;
import org.apache.sis.temporal.TemporalDate;
import org.apache.sis.util.iso.Types;

// Specific to the geoapi-4.0 branch:
import org.opengis.metadata.citation.Responsibility;


/**
 * Brief description of ways in which the resource(s) is/are currently or has been used.
 * The following properties are mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_Usage}
 * {@code   ├─specificUsage…………} Brief description of the resource and/or resource series usage.
 * {@code   └─userContactInfo……} Identification of and means of communicating with person(s) and organisation(s).
 * {@code       ├─party……………………} Information about the parties.
 * {@code       │   └─name……………} Name of the party.
 * {@code       └─role………………………} Function performed by the responsible party.</div>
 *
 * <h2>Limitations</h2>
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
 * @author  Cullen Rombach (Image Matters)
 * @version 1.5
 * @since   0.3
 */
@TitleProperty(name = "specificUsage")
@XmlType(name = "MD_Usage_Type", propOrder = {
    "specificUsage",
    "usageDate",
    "usageDates",
    "userDeterminedLimitations",
    "userContactInfo",
    "response",                     // New in ISO 19115:2014
    "additionalDocumentations",     // Ibid.
    "issues"                        // Ibid. Actually "identifiedIssues"
})
@XmlRootElement(name = "MD_Usage")
public class DefaultUsage extends ISOMetadata implements Usage {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -685588625450110348L;

    /**
     * Brief description of the resource and/or resource series usage.
     */
    @SuppressWarnings("serial")
    private InternationalString specificUsage;

    /**
     * Date and time of the first use or range of uses of the resource and/or resource series.
     */
    @SuppressWarnings("serial")
    private Collection<TemporalPrimitive> usageDates;

    /**
     * Applications, determined by the user for which the resource and/or resource series
     * is not suitable.
     */
    @SuppressWarnings("serial")
    private InternationalString userDeterminedLimitations;

    /**
     * Identification of and means of communicating with person(s) and organization(s) using the resource(s).
     */
    @SuppressWarnings("serial")
    private Collection<Responsibility> userContactInfo;

    /**
     * Responses to the user-determined limitations.
     */
    @SuppressWarnings("serial")
    private Collection<InternationalString> responses;

    /**
     * Publication that describe usage of data.
     */
    @SuppressWarnings("serial")
    private Collection<Citation> additionalDocumentation;

    /**
     * Citation of a description of known issues associated with the resource
     * along with proposed solutions if available.
     */
    @SuppressWarnings("serial")
    private Collection<Citation> identifiedIssues;

    /**
     * Constructs an initially empty usage.
     */
    public DefaultUsage() {
    }

    /**
     * Creates an usage initialized to the specified values.
     *
     * @param specificUsage    brief description of the resource and/or resource series usage, or {@code null} if none.
     * @param userContactInfo  means of communicating with person(s) and organization(s), or {@code null} if none.
     */
    public DefaultUsage(final CharSequence specificUsage,
                        final Responsibility userContactInfo)
    {
        this.specificUsage   = Types.toInternationalString(specificUsage);
        this.userContactInfo = singleton(userContactInfo, Responsibility.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Usage)
     */
    public DefaultUsage(final Usage object) {
        super(object);
        if (object != null) {
            specificUsage             = object.getSpecificUsage();
            usageDates                = copyCollection(object.getUsageDates(), TemporalPrimitive.class);
            userDeterminedLimitations = object.getUserDeterminedLimitations();
            userContactInfo           = copyCollection(object.getUserContactInfo(), Responsibility.class);
            responses                 = copyCollection(object.getResponses(), InternationalString.class);
            additionalDocumentation   = copyCollection(object.getAdditionalDocumentation(), Citation.class);
            identifiedIssues          = copyCollection(object.getIdentifiedIssues(), Citation.class);
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
     *       {@linkplain #DefaultUsage(Usage) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
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
     * @return description of the resource usage, or {@code null}.
     */
    @Override
    @XmlElement(name = "specificUsage", required = true)
    public InternationalString getSpecificUsage() {
        return specificUsage;
    }

    /**
     * Sets a brief description of the resource and/or resource series usage.
     *
     * @param  newValue  the new specific usage.
     */
    public void setSpecificUsage(final InternationalString newValue) {
        checkWritePermission(specificUsage);
        specificUsage = newValue;
    }

    /**
     * Returns the date and time of the first use or range of uses of the resource and/or resource series.
     *
     * @return date of the first use of the resource, or {@code null}.
     *
     * @deprecated Replaced by {@link #getUsageDates()}.
     */
    @Override
    @Deprecated(since="1.5")
    @Dependencies("getUsageDates")
    @XmlElement(name = "usageDateTime", namespace = LegacyNamespaces.GMD)
    public Date getUsageDate() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final Collection<TemporalPrimitive> usageDates = getUsageDates();
            if (usageDates != null) {
                for (TemporalPrimitive t : usageDates) {
                    Date p = TemporalDate.toDate(TemporalObjects.getInstant(t));
                    if (p != null) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Sets the date and time of the first use.
     *
     * @param  newValue  the new usage date.
     *
     * @deprecated Replaced by {@link #setUsageDates(Collection)}.
     */
    @Deprecated(since="1.5")
    public void setUsageDate(final Date newValue)  {
        setUsageDates(newValue == null ? List.of() : List.of(TemporalObjects.createInstant(TemporalDate.toTemporal(newValue))));
    }

    /**
     * Returns the date and time of the first use or range of uses of the resource and/or resource series.
     *
     * @return date of the first use of the resource.
     *
     * @since 1.5
     */
    @Override
    @XmlElement(name = "usageDateTime")
    @XmlJavaTypeAdapter(TM_Primitive.Since2014.class)
    public Collection<TemporalPrimitive> getUsageDates() {
        return usageDates = nonNullCollection(usageDates, TemporalPrimitive.class);
    }

    /**
     * Sets the date and time of the first use or range of uses of the resource and/or resource series.
     *
     * @param  newValues  date of the first use of the resource.
     *
     * @since 1.5
     */
    public void setUsageDates(final Collection<TemporalPrimitive> newValues) {
        usageDates = writeCollection(usageDates, newValues, TemporalPrimitive.class);
    }

    /**
     * Adds a period for the range of uses of the resource and/or resource series.
     * This is a convenience method for adding a temporal period.
     *
     * @param  beginning  the begin instant (inclusive), or {@code null}.
     * @param  ending     the end instant (inclusive), or {@code null}.
     *
     * @since 1.5
     */
    public void addUsageDates(final Temporal beginning, final Temporal ending) {
        TemporalPrimitive period = TemporalObjects.createPeriod(beginning, ending);
        if (period != null) {
            getUsageDates().add(period);
        }
    }

    /**
     * Returns applications, determined by the user for which the resource and/or resource series is not suitable.
     *
     * @return applications for which the resource and/or resource series is not suitable, or {@code null}.
     */
    @Override
    @XmlElement(name = "userDeterminedLimitations")
    public InternationalString getUserDeterminedLimitations() {
        return userDeterminedLimitations;
    }

    /**
     * Sets applications, determined by the user for which the resource and/or resource series is not suitable.
     *
     * @param  newValue  the new user determined limitations.
     */
    public void setUserDeterminedLimitations(final InternationalString newValue) {
        checkWritePermission(userDeterminedLimitations);
        userDeterminedLimitations = newValue;
    }

    /**
     * Returns identification of and means of communicating with person(s) and organization(s) using the resource(s).
     *
     * @return means of communicating with person(s) and organization(s) using the resource(s).
     */
    @Override
    @XmlElement(name = "userContactInfo")
    public Collection<Responsibility> getUserContactInfo() {
        return userContactInfo = nonNullCollection(userContactInfo, Responsibility.class);
    }

    /**
     * Sets identification of and means of communicating with person(s) and organization(s) using the resource(s).
     *
     * @param  newValues  the new user contact info.
     */
    public void setUserContactInfo(final Collection<? extends Responsibility> newValues) {
        userContactInfo = writeCollection(newValues, userContactInfo, Responsibility.class);
    }

    /**
     * Responses to the user-determined limitations.
     *
     * @return response to the user-determined limitations.
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<InternationalString> getResponses() {
        return responses = nonNullCollection(responses, InternationalString.class);
    }

    /**
     * Sets a new response to the user-determined limitations.
     *
     * @param  newValues  the new response to the user-determined limitations.
     *
     * @since 0.5
     */
    public void setResponses(final Collection<? extends InternationalString> newValues) {
        responses = writeCollection(newValues, responses, InternationalString.class);
    }

    /**
     * Publications that describe usage of data.
     *
     * @return publications that describe usage of data.
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<Citation> getAdditionalDocumentation() {
        return additionalDocumentation = nonNullCollection(additionalDocumentation, Citation.class);
    }

    /**
     * Sets the publications that describe usage of data.
     *
     * @param  newValues  the new publications.
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
     * @return citation of a description of known issues associated with the resource.
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<Citation> getIdentifiedIssues() {
        return identifiedIssues = nonNullCollection(identifiedIssues, Citation.class);
    }

    /**
     * Sets a new citation of a description of known issues associated with the resource
     * along with proposed solutions if available.
     *
     * @param  newValues  the new citation of a description.
     *
     * @since 0.5
     */
    public void setIdentifiedIssues(final Collection<? extends Citation> newValues) {
        identifiedIssues = writeCollection(newValues, identifiedIssues, Citation.class);
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19115:2014 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     */
    @XmlElement(name = "response")
    private Collection<InternationalString> getResponse() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getResponses() : null;
    }

    @XmlElement(name = "additionalDocumentation")
    private Collection<Citation> getAdditionalDocumentations() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getAdditionalDocumentation() : null;
    }

    @XmlElement(name = "identifiedIssues")
    private Collection<Citation> getIssues() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getIdentifiedIssues() : null;
    }
}
