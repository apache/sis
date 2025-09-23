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
import java.util.function.Function;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Role;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.Types;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.metadata.iso.legacy.LegacyPropertyAdapter;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.valueIfDefined;


/**
 * Identification of, and means of communication with, person(s) and
 * organizations associated with the dataset.
 * The following properties are mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code CI_ResponsibleParty}
 * {@code   ├─role……………………………} Function performed by the responsible party.
 * {@code   └─party…………………………} Information about the parties.
 * {@code       └─name…………………} Name of the party.</div>
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
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "CI_ResponsibleParty_Type", namespace = LegacyNamespaces.GMD, propOrder = {
    "individualName",
    "organisationName",
    "positionName",
    "contactInfo",
    "role"
})
@XmlRootElement(name = "CI_ResponsibleParty", namespace = LegacyNamespaces.GMD)
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
     * @param role  the function performed by the responsible party, or {@code null}.
     */
    public DefaultResponsibleParty(final Role role) {
        super(role, null, null);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
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
     *       {@linkplain #DefaultResponsibleParty(ResponsibleParty) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
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
     * then this method will search in the list of organization members. The latter structure is used by our netCDF
     * reader.
     *
     * @param  position {@code true} for returning the position name instead of individual name.
     * @return the name or position of the first individual, or {@code null}.
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
     * @param  position {@code true} for returning the position name instead of individual name.
     * @return the name or position of the first individual, or {@code null}.
     *
     * @see #getOrganisationName()
     * @see #getIndividualName()
     * @see #getPositionName()
     */
    private static InternationalString getName(final Collection<? extends AbstractParty> parties,
            final Class<? extends AbstractParty> type, final boolean position)
    {
        InternationalString name = null;
        if (parties != null) {                              // May be null on marshalling.
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
     * If no existing party is found, generate a new party using the given creator.
     */
    private void setName(final Class<? extends AbstractParty> type, final boolean position, final InternationalString name,
                         final Function<InternationalString,AbstractParty> creator)
    {
        final Collection<AbstractParty> parties = getParties();
        checkWritePermission(valueIfDefined(parties));
        if (parties != null) {                                  // May be null on unmarshalling.
            final Iterator<AbstractParty> it = parties.iterator();
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
                    return;
                }
            }
        }
        if (name != null) {                             // If no party and name is null, there is nothing to set.
            final AbstractParty party = creator.apply(name);
            if (parties != null) {                      // May be null on unmarshalling.
                parties.add(party);
            } else {
                setParties(Collections.singletonList(party));
            }
        }
    }

    /**
     * Returns the name of the responsible person- surname, given name, title separated by a delimiter.
     * Only one of {@code individualName}, {@link #getOrganisationName() organisationName}
     * and {@link #getPositionName() positionName} shall be provided.
     *
     * <p>This implementation returns the name of the first {@code Individual} found in the collection of
     * {@linkplain #getParties() parties}. If no individual is found in the parties, then this method fallbacks
     * on the first organisation member.</p>
     *
     * @return name, surname, given name and title of the responsible person, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@code getName()} in {@link DefaultIndividual}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getParties")
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
     * <p>This implementation sets the name of the first {@code Individual} found in the collection of
     * {@linkplain #getParties() parties}, or create a new individual if no existing instance was found.</p>
     *
     * @param  newValue  the new individual name, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@code setName(InternationalString)} in {@link DefaultIndividual}.
     */
    @Deprecated(since="1.0")
    public void setIndividualName(final String newValue) {
        setName(DefaultIndividual.class, false, Types.toInternationalString(newValue), DefaultResponsibleParty::individual);
    }

    /**
     * Generates a new individual from the given name.
     */
    private static AbstractParty individual(final InternationalString name) {
        return new DefaultIndividual(name, null, null);
    }

    /**
     * Returns the name of the responsible organization. Only one of
     * {@link #getIndividualName() individualName}, {@code organisationName}
     * and {@link #getPositionName() positionName} shall be provided.
     *
     * <p>This implementation returns the name of the first {@code Organisation}
     * found in the collection of {@linkplain #getParties() parties}.</p>
     *
     * @return name of the responsible organization, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@code getName()} in {@link DefaultOrganisation}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getParties")
    @XmlElement(name = "organisationName")
    public InternationalString getOrganisationName() {
        return getName(getParties(), DefaultOrganisation.class, false);
    }

    /**
     * Sets the name of the responsible organization. Only one of
     * {@link #getIndividualName() individualName}, {@code organisationName}
     * and {@link #getPositionName() positionName} shall be provided.
     *
     * <p>This implementation sets the name of the first {@code Organisation} found in the collection of
     * {@linkplain #getParties() parties}, or create a new organization if no existing instance was found.</p>
     *
     * @param  newValue  the new organization name, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@code setName(InternationalString)} in {@link DefaultOrganisation}.
     */
    @Deprecated(since="1.0")
    public void setOrganisationName(final InternationalString newValue) {
        setName(DefaultOrganisation.class, false, newValue, DefaultResponsibleParty::organisation);
    }

    /**
     * Generates a new organization from the given name.
     */
    private static AbstractParty organisation(final InternationalString name) {
        return new DefaultOrganisation(name, null, null, null);
    }

    /**
     * Returns the role or position of the responsible person Only one of
     * {@link #getIndividualName() individualName}, {@link #getOrganisationName() organisationName}
     * and {@code positionName} shall be provided.
     *
     * <p>This implementation returns the position of the first {@code Individual} found in the collection of
     * {@linkplain #getParties() parties}. If no individual is found in the parties, then this method fallbacks
     * on the first organisation member.</p>
     *
     * @return role or position of the responsible person, or {@code null}
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link DefaultIndividual#getPositionName()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getParties")
    @XmlElement(name = "positionName")
    public InternationalString getPositionName() {
        return getIndividual(true);
    }

    /**
     * set the role or position of the responsible person Only one of
     * {@link #getIndividualName() individualName}, {@link #getOrganisationName() organisationName}
     * and {@code positionName} shall be provided.
     *
     * <p>This implementation sets the position name of the first {@code Individual} found in the collection of
     * {@linkplain #getParties() parties}, or create a new individual if no existing instance was found.</p>
     *
     * @param  newValue  the new position name, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link DefaultIndividual#setPositionName(InternationalString)}.
     */
    @Deprecated(since="1.0")
    public void setPositionName(final InternationalString newValue) {
        setName(DefaultIndividual.class, true, newValue, DefaultResponsibleParty::position);
    }

    /**
     * Generates a new position from the given name.
     */
    private static AbstractParty position(final InternationalString name) {
        return new DefaultIndividual(null, name, null);
    }

    /**
     * Returns the address of the responsible party.
     *
     * <p>This implementation returns the first non-null contact found in the collection of
     * {@linkplain #getParties() parties}.</p>
     *
     * @return address of the responsible party, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link AbstractParty#getContactInfo()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getParties")
    @XmlElement(name = "contactInfo")
    public Contact getContactInfo() {
        final Collection<AbstractParty> parties = getParties();
        if (parties != null) {                                          // May be null on marshalling.
            for (final AbstractParty party : parties) {
                final Collection<? extends Contact> contacts = party.getContactInfo();
                if (contacts != null) {                                 // May be null on marshalling.
                    for (final Contact contact : contacts) {
                        if (contact != null) {                          // Paranoiac check.
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
     * @param  newValue  the new contact info, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link AbstractParty#setContactInfo(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setContactInfo(final Contact newValue) {
        final Collection<AbstractParty> parties = getParties();
        checkWritePermission(valueIfDefined(parties));
        if (parties != null) {                                  // May be null on unmarshalling.
            final Iterator<AbstractParty> it = parties.iterator();
            while (it.hasNext()) {
                final AbstractParty party = it.next();
                party.setContactInfo(newValue != null ? Collections.singleton(newValue) : null);
                if (party.isEmpty()) {
                    it.remove();
                }
                return;
            }
        }
        /*
         * If no existing AbstractParty were found, add a new one. However, there is no way to know if
         * it should be an individual or an organization. Arbitrarily choose an individual for now.
         */
        if (newValue != null) {
            final AbstractParty party = new DefaultIndividual(null, null, newValue);
            if (parties != null) {
                parties.add(party);
            } else {
                setParties(Collections.singletonList(party));
            }
        }
    }

    /**
     * Returns the function performed by the responsible party.
     *
     * @return function performed by the responsible party.
     */
    @Override
    @XmlElement(name = "role", required = true)
    public Role getRole() {
        return super.getRole();
    }

    /**
     * Sets the function performed by the responsible party.
     *
     * @param  newValue  the new role.
     */
    @Override
    public void setRole(final Role newValue) {
        super.setRole(newValue);
    }
}
