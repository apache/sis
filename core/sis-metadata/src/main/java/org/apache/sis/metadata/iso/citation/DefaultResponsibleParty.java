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

import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Role;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.Types;
import org.apache.sis.internal.metadata.LegacyPropertyAdapter;


/**
 * Identification of, and means of communication with, person(s) and
 * organizations associated with the dataset.
 *
 * <div class="warning"><b>Upcoming API change — deprecation</b><br>
 * As of ISO 19115:2014, the {@code ResponsibleParty} type has been replaced by {@code Responsibility}
 * to allow more flexible associations of individuals, organisations, and roles.
 * This {@code ResponsibleParty} interface may be deprecated in GeoAPI 4.0.
 * </div>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.5
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
public class DefaultResponsibleParty extends DefaultResponsibility implements ResponsibleParty {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1022635486627088812L;

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
        super(role, null, null);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     */
    public DefaultResponsibleParty(final DefaultResponsibility object) {
        super(object);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(ResponsibleParty)
     */
    public DefaultResponsibleParty(final ResponsibleParty object) {
        super(object);
        if (object != null && !(object instanceof DefaultResponsibility)) {
            setIndividualName(object.getIndividualName());
            setOrganisationName(object.getOrganisationName());
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
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
     * Returns the name or the position of the first individual. If no individual is found in the list of parties,
     * then this method will search in the list of organization members. The later structure is used by our NetCDF
     * reader.
     *
     * @param  position {@code true} for returning the position name instead than individual name.
     * @return The name or position of the first individual, or {@code null}.
     *
     * @see #getIndividualName()
     * @see #getPositionName()
     */
    private InternationalString getIndividual(final boolean position) {
        final Collection<AbstractParty> parties = getParties();
        InternationalString name = getName(parties, DefaultIndividual.class, position);
        if (name == null && parties != null) {
            for (final AbstractParty party : parties) {
                if (party instanceof DefaultOrganisation) {
                    name = getName(((DefaultOrganisation) party).getIndividual(), DefaultIndividual.class, position);
                    if (name != null) {
                        break;
                    }
                }
            }
        }
        return name;
    }

    /**
     * Returns the name of the first party of the given type, or {@code null} if none.
     *
     * @param  position {@code true} for returning the position name instead than individual name.
     * @return The name or position of the first individual, or {@code null}.
     *
     * @see #getOrganisationName()
     * @see #getIndividualName()
     * @see #getPositionName()
     */
    private static InternationalString getName(final Collection<? extends AbstractParty> parties,
            final Class<? extends AbstractParty> type, final boolean position)
    {
        InternationalString name = null;
        if (parties != null) { // May be null on marshalling.
            for (final AbstractParty party : parties) {
                if (type.isInstance(party)) {
                    if (name != null) {
                        LegacyPropertyAdapter.warnIgnoredExtraneous(type, DefaultResponsibleParty.class,
                                position ? "getPositionName" : (type == DefaultIndividual.class)
                                         ? "getIndividualName" : "getOrganisationName");
                        break;
                    }
                    name = position ? ((DefaultIndividual) party).getPositionName() : party.getName();
                }
            }
        }
        return name;
    }

    /**
     * Sets the name of the first party of the given type.
     *
     * @return {@code true} if the name has been set, or {@code false} otherwise.
     */
    private boolean setName(final Class<? extends AbstractParty> type, final boolean position, final InternationalString name) {
        checkWritePermission();
        final Iterator<AbstractParty> it = getParties().iterator();
        while (it.hasNext()) {
            final AbstractParty party = it.next();
            if (type.isInstance(party)) {
                if (position) {
                    ((DefaultIndividual) party).setPositionName(name);
                } else {
                    party.setName(name);
                }
                if (party.isEmpty()) {
                    it.remove();
                }
                return true;
            }
        }
        return name == null; // If no party and name is null, there is nothing to set.
    }

    /**
     * Returns the name of the responsible person- surname, given name, title separated by a delimiter.
     * Only one of {@code individualName}, {@link #getOrganisationName() organisationName}
     * and {@link #getPositionName() positionName} shall be provided.
     *
     * <p>This implementation returns the name of the first {@link Individual} found in the collection of
     * {@linkplain #getParties() parties}. If no individual is found in the parties, then this method fallbacks
     * on the first {@linkplain Organisation#getIndividual() organisation member}.</p>
     *
     * @return Name, surname, given name and title of the responsible person, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@code getName()} in {@link DefaultIndividual}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "individualName")
    public String getIndividualName() {
        final InternationalString name = getIndividual(false);
        return (name != null) ? name.toString() : null;
    }

    /**
     * Sets the name of the responsible person- surname, given name, title separated by a delimiter.
     * Only one of {@code individualName}, {@link #getOrganisationName() organisationName}
     * and {@link #getPositionName() positionName} shall be provided.
     *
     * <p>This implementation sets the name of the first {@link Individual} found in the collection of
     * {@linkplain #getParties() parties}, or create a new individual if no existing instance was found.</p>
     *
     * @param newValue The new individual name, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@code setName(InternationalString)} in {@link DefaultIndividual}.
     */
    @Deprecated
    public void setIndividualName(final String newValue) {
        if (!setName(DefaultIndividual.class, false, Types.toInternationalString(newValue))) {
            getParties().add(new DefaultIndividual(newValue, null, null));
        }
    }

    /**
     * Returns the name of the responsible organization. Only one of
     * {@link #getIndividualName() individualName}, {@code organisationName}
     * and {@link #getPositionName() positionName} shall be provided.
     *
     * <p>This implementation returns the name of the first {@link Organisation}
     * found in the collection of {@linkplain #getParties() parties}.</p>
     *
     * @return Name of the responsible organization, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@code getName()} in {@link DefaultOrganisation}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "organisationName")
    public InternationalString getOrganisationName() {
        return getName(getParties(), DefaultOrganisation.class, false);
    }

    /**
     * Sets the name of the responsible organization. Only one of
     * {@link #getIndividualName() individualName}, {@code organisationName}
     * and {@link #getPositionName() positionName} shall be provided.
     *
     * <p>This implementation sets the name of the first {@link Organisation} found in the collection of
     * {@linkplain #getParties() parties}, or create a new organization if no existing instance was found.</p>
     *
     * @param newValue The new organization name, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@code setName(InternationalString)} in {@link DefaultOrganisation}.
     */
    @Deprecated
    public void setOrganisationName(final InternationalString newValue) {
        if (!setName(DefaultOrganisation.class, false, Types.toInternationalString(newValue))) {
            getParties().add(new DefaultOrganisation(newValue, null, null, null));
        }
    }

    /**
     * Returns the role or position of the responsible person Only one of
     * {@link #getIndividualName() individualName}, {@link #getOrganisationName() organisationName}
     * and {@code positionName} shall be provided.
     *
     * <p>This implementation returns the position of the first {@link Individual} found in the collection of
     * {@linkplain #getParties() parties}. If no individual is found in the parties, then this method fallbacks
     * on the first {@linkplain Organisation#getIndividual() organisation member}.</p>
     *
     * @return Role or position of the responsible person, or {@code null}
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link DefaultIndividual#getPositionName()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "positionName")
    public InternationalString getPositionName() {
        return getIndividual(true);
    }

    /**
     * set the role or position of the responsible person Only one of
     * {@link #getIndividualName() individualName}, {@link #getOrganisationName() organisationName}
     * and {@code positionName} shall be provided.
     *
     * <p>This implementation sets the position name of the first {@link Individual} found in the collection of
     * {@linkplain #getParties() parties}, or create a new individual if no existing instance was found.</p>
     *
     * @param newValue The new position name, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link DefaultIndividual#setPositionName(InternationalString)}.
     */
    @Deprecated
    public void setPositionName(final InternationalString newValue) {
        if (!setName(DefaultIndividual.class, true, newValue)) {
            getParties().add(new DefaultIndividual(null, newValue, null));
        }
    }

    /**
     * Returns the address of the responsible party.
     *
     * <p>This implementation returns the first non-null contact found in the collection of
     * {@linkplain #getParties() parties}.</p>
     *
     * @return Address of the responsible party, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link AbstractParty#getContactInfo()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "contactInfo")
    public Contact getContactInfo() {
        final Collection<AbstractParty> parties = getParties();
        if (parties != null) { // May be null on marshalling.
            for (final AbstractParty party : parties) {
                final Collection<? extends Contact> contacts = party.getContactInfo();
                if (contacts != null) { // May be null on marshalling.
                    for (final Contact contact : contacts) {
                        if (contact != null) { // Paranoiac check.
                            return contact;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Sets the address of the responsible party.
     *
     * <p>This implementation sets the contact info in the first party found in the collection of
     * {@linkplain #getParties() parties}.</p>
     *
     * @param newValue The new contact info, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link AbstractParty#setContactInfo(Collection)}.
     */
    @Deprecated
    public void setContactInfo(final Contact newValue) {
        checkWritePermission();
        final Iterator<AbstractParty> it = getParties().iterator();
        while (it.hasNext()) {
            final AbstractParty party = it.next();
            party.setContactInfo(newValue != null ? Collections.singleton(newValue) : null);
            if (party.isEmpty()) {
                it.remove();
            }
            return;
        }
        /*
         * If no existing AbstractParty were found, add a new one. However there is no way to know if
         * it should be an individual or an organization. Arbitrarily choose an individual for now.
         */
        if (newValue != null) {
            getParties().add(new DefaultIndividual(null, null, newValue));
        }
    }

    /**
     * Returns the function performed by the responsible party.
     *
     * @return Function performed by the responsible party.
    */
    @Override
    @XmlElement(name = "role", required = true)
    public Role getRole() {
        return super.getRole();
    }

    /**
     * Sets the function performed by the responsible party.
     *
     * @param newValue The new role.
     */
    @Override
    public void setRole(final Role newValue) {
        super.setRole(newValue);
    }
}
