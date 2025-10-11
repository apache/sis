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
package org.apache.sis.metadata.iso.acquisition;

import java.util.Collection;
import java.time.temporal.Temporal;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.acquisition.Plan;
import org.opengis.metadata.acquisition.Priority;
import org.opengis.metadata.acquisition.RequestedDate;
import org.opengis.metadata.acquisition.Requirement;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.ISOMetadata;

// Specific to the main and geoapi-3.1 branches:
import java.util.Date;
import org.opengis.metadata.citation.ResponsibleParty;
import org.apache.sis.temporal.TemporalDate;


/**
 * Requirement to be satisfied by the planned data acquisition.
 * The following properties are mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MI_Requirement}
 * {@code   ├─identifier………………………………………………………} Unique name, or code, for the requirement.
 * {@code   │   └─code……………………………………………………………} Alphanumeric value identifying an instance in the namespace.
 * {@code   ├─requestor…………………………………………………………} Origin of requirement.
 * {@code   │   ├─party…………………………………………………………} Information about the parties.
 * {@code   │   │   └─name…………………………………………………} Name of the party.
 * {@code   │   └─role……………………………………………………………} Function performed by the responsible party.
 * {@code   ├─recipient…………………………………………………………} Person(s), or body(ies), to receive results of requirement.
 * {@code   ├─priority……………………………………………………………} Relative ordered importance, or urgency, of the requirement.
 * {@code   ├─requestedDate………………………………………………} Required or preferred acquisition date and time.
 * {@code   │   ├─requestedDateOfCollection……} Preferred date and time of collection.
 * {@code   │   └─latestAcceptableDate…………………} Latest date and time collection must be completed.
 * {@code   └─expiryDate………………………………………………………} Date and time after which collection is no longer valid.</div>
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
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.3
 */
@XmlType(name = "MI_Requirement_Type", propOrder = {
    "citation",
    "identifier",
    "requestors",
    "recipients",
    "priority",
    "requestedDate",
    "expiryDate",
    "satisfiedPlans"
})
@XmlRootElement(name = "MI_Requirement")
public class DefaultRequirement extends ISOMetadata implements Requirement {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3698085104012907473L;

    /**
     * Identification of reference or guidance material for the requirement.
     */
    @SuppressWarnings("serial")
    private Citation citation;

    /**
     * Origin of requirement.
     */
    @SuppressWarnings("serial")
    private Collection<ResponsibleParty> requestors;

    /**
     * Person(s), or body(ies), to receive results of requirement.
     */
    @SuppressWarnings("serial")
    private Collection<ResponsibleParty> recipients;

    /**
     * Relative ordered importance, or urgency, of the requirement.
     */
    private Priority priority;

    /**
     * Required or preferred acquisition date and time.
     */
    @SuppressWarnings("serial")
    private RequestedDate requestedDate;

    /**
     * Date and time after which collection is no longer valid.
     */
    @SuppressWarnings("serial")     // Standard Java implementations are serializable.
    private Temporal expiryDate;

    /**
     * Plan that identifies solution to satisfy the requirement.
     */
    @SuppressWarnings("serial")
    private Collection<Plan> satisfiedPlans;

    /**
     * Constructs an initially empty requirement.
     */
    public DefaultRequirement() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Requirement)
     */
    public DefaultRequirement(final Requirement object) {
        super(object);
        if (object != null) {
            citation       = object.getCitation();
            identifiers    = singleton(object.getIdentifier(), Identifier.class);
            requestors     = copyCollection(object.getRequestors(), ResponsibleParty.class);
            recipients     = copyCollection(object.getRecipients(), ResponsibleParty.class);
            priority       = object.getPriority();
            requestedDate  = object.getRequestedDate();
            expiryDate     = TemporalDate.toTemporal(object.getExpiryDate());
            satisfiedPlans = copyCollection(object.getSatisfiedPlans(), Plan.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultRequirement}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultRequirement} instance is created using the
     *       {@linkplain #DefaultRequirement(Requirement) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultRequirement castOrCopy(final Requirement object) {
        if (object == null || object instanceof DefaultRequirement) {
            return (DefaultRequirement) object;
        }
        return new DefaultRequirement(object);
    }

    /**
     * Returns the identification of reference or guidance material for the requirement.
     * {@code null} if unspecified.
     *
     * @return identification of reference or guidance material, or {@code null}.
     */
    @Override
    @XmlElement(name = "citation")
    public Citation getCitation() {
        return citation;
    }

    /**
     * Sets the identification of reference or guidance material for the requirement.
     *
     * @param  newValue  the new citation value.
     */
    public void setCitation(final Citation newValue) {
        checkWritePermission(citation);
        citation = newValue;
    }

    /**
     * Returns the unique name, or code, for the requirement.
     *
     * @return unique name or code, or {@code null}.
     */
    @Override
    @XmlElement(name = "identifier", required = true)
    public Identifier getIdentifier() {
        return super.getIdentifier();
    }

    /**
     * Sets the unique name, or code, for the requirement.
     *
     * @param  newValue  the new identifier value.
     */
    @Override
    public void setIdentifier(final Identifier newValue) {
        super.setIdentifier(newValue);
    }

    /**
     * Returns the origin of requirement.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@code Responsibility} parent interface.
     * This change will be tentatively applied in GeoAPI 4.0.
     * </div>
     *
     * @return origin of requirement.
     */
    @Override
    @XmlElement(name = "requestor", required = true)
    public Collection<ResponsibleParty> getRequestors() {
        return requestors = nonNullCollection(requestors, ResponsibleParty.class);
    }

    /**
     * Sets the origin of requirement.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@code Responsibility} parent interface.
     * This change will be tentatively applied in GeoAPI 4.0.
     * </div>
     *
     * @param  newValues  the new requestors values.
     */
    public void setRequestors(final Collection<? extends ResponsibleParty> newValues) {
        requestors = writeCollection(newValues, requestors, ResponsibleParty.class);
    }

    /**
     * Returns the person(s), or body(ies), to receive results of requirement.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@code Responsibility} parent interface.
     * This change will be tentatively applied in GeoAPI 4.0.
     * </div>
     *
     * @return person(s), or body(ies), to receive results.
     */
    @Override
    @XmlElement(name = "recipient", required = true)
    public Collection<ResponsibleParty> getRecipients() {
        return recipients = nonNullCollection(recipients, ResponsibleParty.class);
    }

    /**
     * Sets the Person(s), or body(ies), to receive results of requirement.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@code Responsibility} parent interface.
     * This change will be tentatively applied in GeoAPI 4.0.
     * </div>
     *
     * @param  newValues  the new recipients values.
     */
    public void setRecipients(final Collection<? extends ResponsibleParty> newValues) {
        recipients = writeCollection(newValues, recipients, ResponsibleParty.class);
    }

    /**
     * Returns the relative ordered importance, or urgency, of the requirement.
     *
     * @return relative ordered importance, or urgency, or {@code null}.
     */
    @Override
    @XmlElement(name = "priority", required = true)
    public Priority getPriority() {
        return priority;
    }

    /**
     * Sets the relative ordered importance, or urgency, of the requirement.
     *
     * @param  newValue  the new priority value.
     */
    public void setPriority(final Priority newValue) {
        checkWritePermission(priority);
        priority = newValue;
    }

    /**
     * Returns the required or preferred acquisition date and time.
     *
     * @return required or preferred acquisition date and time, or {@code null}.
     */
    @Override
    @XmlElement(name = "requestedDate", required = true)
    public RequestedDate getRequestedDate() {
        return requestedDate;
    }

    /**
     * Sets the required or preferred acquisition date and time.
     *
     * @param  newValue  the new requested date value.
     */
    public void setRequestedDate(final RequestedDate newValue) {
        checkWritePermission(requestedDate);
        requestedDate = newValue;
    }

    /**
     * Returns the date and time after which collection is no longer valid.
     *
     * <div class="warning"><b>Upcoming API change</b><br>
     * {@code Date} may be replaced by {@link Temporal} in GeoAPI 4.0.
     * </div>
     *
     * @return date and time after which collection is no longer valid, or {@code null}.
     */
    @Override
    @XmlElement(name = "expiryDate", required = true)
    public Date getExpiryDate() {
        return TemporalDate.toDate(expiryDate);
    }

    /**
     * Sets the date and time after which collection is no longer valid.
     * The specified value should be an instance of {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
     * {@link java.time.OffsetDateTime} or {@link java.time.ZonedDateTime}, depending whether hours are defined
     * and how the timezone (if any) is defined. But other types are also allowed.
     *
     * @param  newValue  the new expiry date.
     *
     * @since 1.5
     */
    public void setExpiryDate(final Temporal newValue) {
        checkWritePermission(expiryDate);
        expiryDate = newValue;
    }

    /**
     * Sets the date and time after which collection is no longer valid.
     *
     * @param  newValue  the new expiry date.
     *
     * @deprecated Replaced by {@link #setExpiryDate(Temporal)}.
     */
    @Deprecated(since="1.5")
    public void setExpiryDate(final Date newValue) {
        setExpiryDate(TemporalDate.toTemporal(newValue));
    }

    /**
     * Returns the plan that identifies solution to satisfy the requirement.
     *
     * @return plan that identifies solution to satisfy the requirement.
     */
    @Override
    @XmlElement(name = "satisifiedPlan")                // Really spelled that way in XSD file.
    public Collection<Plan> getSatisfiedPlans() {
        return satisfiedPlans = nonNullCollection(satisfiedPlans, Plan.class);
    }

    /**
     * Sets the plan that identifies solution to satisfy the requirement.
     *
     * @param  newValues  the new satisfied plans values.
     */
    public void setSatisfiedPlans(final Collection<? extends Plan> newValues) {
        satisfiedPlans = writeCollection(newValues, satisfiedPlans, Plan.class);
    }
}
