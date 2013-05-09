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
package org.apache.sis.metadata.iso.citation;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Role;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Identification of, and means of communication with, person(s) and
 * organizations associated with the dataset.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "CI_ResponsibleParty_Type", propOrder = {
    "individualName",
    "organisationName",
    "positionName",
    "contactInfo",
    "role"
})
@XmlRootElement(name = "CI_ResponsibleParty")
public class DefaultResponsibleParty extends ISOMetadata implements ResponsibleParty {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3429257224445006902L;

    /**
     * Name of the responsible person- surname, given name, title separated by a delimiter.
     */
    private String individualName;

    /**
     * Name of the responsible organization.
     */
    private InternationalString organisationName;

    /**
     * Role or position of the responsible person
     */
    private InternationalString positionName;

    /**
     * Address of the responsible party.
     */
    private Contact contactInfo;

    /**
     * Function performed by the responsible party.
     */
    private Role role;

    /**
     * Constructs an initially empty responsible party.
     */
    public DefaultResponsibleParty() {
    }

    /**
     * Constructs a responsibility party with the given role.
     *
     * @param role The function performed by the responsible party, or {@code null}.
     */
    public DefaultResponsibleParty(final Role role) {
        this.role = role;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(ResponsibleParty)
     */
    public DefaultResponsibleParty(final ResponsibleParty object) {
        super(object);
        individualName   = object.getIndividualName();
        organisationName = object.getOrganisationName();
        positionName     = object.getPositionName();
        contactInfo      = object.getContactInfo();
        role             = object.getRole();
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultResponsibleParty}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultResponsibleParty} instance is created using the
     *       {@linkplain #DefaultResponsibleParty(ResponsibleParty) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultResponsibleParty castOrCopy(final ResponsibleParty object) {
        if (object == null || object instanceof DefaultResponsibleParty) {
            return (DefaultResponsibleParty) object;
        }
        return new DefaultResponsibleParty(object);
    }

    /**
     * Returns the name of the responsible person- surname, given name, title separated by a delimiter.
     * Only one of {@code individualName}, {@link #getOrganisationName organisationName}
     * and {@link #getPositionName positionName} should be provided.
     */
    @Override
    @XmlElement(name = "individualName")
    public String getIndividualName() {
        return individualName;
    }

    /**
     * Sets the name of the responsible person- surname, given name, title separated by a delimiter.
     * Only one of {@code individualName}, {@link #getOrganisationName organisationName}
     * and {@link #getPositionName positionName} should be provided.
     *
     * @param newValue The new individual name, or {@code null} if none.
     */
    public void setIndividualName(final String newValue) {
        checkWritePermission();
        individualName = newValue;
    }

    /**
     * Returns the name of the responsible organization. Only one of
     * {@link #getIndividualName individualName}, {@code organisationName}
     * and {@link #getPositionName positionName} should be provided.
     */
    @Override
    @XmlElement(name = "organisationName")
    public InternationalString getOrganisationName() {
        return organisationName;
    }

    /**
     * Sets the name of the responsible organization. Only one of
     * {@link #getIndividualName individualName}, {@code organisationName}
     * and {@link #getPositionName positionName} should be provided.
     *
     * @param newValue The new organisation name, or {@code null} if none.
     */
    public void setOrganisationName(final InternationalString newValue) {
        checkWritePermission();
        organisationName = newValue;
    }

    /**
     * Returns the role or position of the responsible person Only one of
     * {@link #getIndividualName individualName}, {@link #getOrganisationName organisationName}
     * and {@code positionName} should be provided.
     */
    @Override
    @XmlElement(name = "positionName")
    public InternationalString getPositionName() {
        return positionName;
    }

    /**
     * set the role or position of the responsible person Only one of
     * {@link #getIndividualName individualName}, {@link #getOrganisationName organisationName}
     * and {@code positionName} should be provided.
     *
     * @param newValue The new position name, or {@code null} if none.
     */
    public void setPositionName(final InternationalString newValue) {
        checkWritePermission();
        positionName = newValue;
    }

    /**
     * Returns the address of the responsible party.
     */
    @Override
    @XmlElement(name = "contactInfo")
    public Contact getContactInfo() {
        return contactInfo;
    }

    /**
     * Sets the address of the responsible party.
     *
     * @param newValue The new contact info, or {@code null} if none.
     */
    public void setContactInfo(final Contact newValue) {
        checkWritePermission();
        contactInfo = newValue;
    }

    /**
     * Returns the function performed by the responsible party.
     */
    @Override
    @XmlElement(name = "role", required = true)
    public Role getRole() {
        return role;
    }

    /**
     * Sets the function performed by the responsible party.
     *
     * @param newValue The new role, or {@code null} if none.
     */
    public void setRole(final Role newValue) {
        checkWritePermission();
        role = newValue;
    }
}
