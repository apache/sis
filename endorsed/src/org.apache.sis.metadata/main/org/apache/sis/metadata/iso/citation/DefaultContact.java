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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Address;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.Telephone;
import org.opengis.metadata.citation.OnlineResource;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.gco.InternationalStringAdapter;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.metadata.iso.legacy.LegacyPropertyAdapter;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.util.internal.shared.CollectionsExt;

// Specific to the main branch:
import org.opengis.util.CodeList;
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;
import org.apache.sis.pending.geoapi.evolution.UnsupportedCodeList;


/**
 * Information required to enable contact with the responsible person and/or organization.
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
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "CI_Contact_Type", propOrder = {
    "phone",
    "phoneList",
    "address",
    "addressList",
    "onlineResource",
    "onlineResourceList",
    "hoursOfService",
    "contactInstructions",
    "contactType"
})
@XmlRootElement(name = "CI_Contact")
public class DefaultContact extends ISOMetadata implements Contact {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -969735574940462381L;

    /**
     * Telephone numbers at which the organization or individual may be contacted.
     */
    @SuppressWarnings("serial")
    private Collection<Telephone> phones;

    /**
     * Physical and email addresses at which the organization or individual may be contacted.
     */
    @SuppressWarnings("serial")
    private Collection<Address> addresses;

    /**
     * On-line information that can be used to contact the individual or organization.
     */
    @SuppressWarnings("serial")
    private Collection<OnlineResource> onlineResources;

    /**
     * Time period (including time zone) when individuals can contact the organization or individual.
     */
    @SuppressWarnings("serial")
    private InternationalString hoursOfService;

    /**
     * Supplemental instructions on how or when to contact the individual or organization.
     */
    @SuppressWarnings("serial")
    private InternationalString contactInstructions;

    /**
     * Type of the contact.
     */
    @SuppressWarnings("serial")
    private InternationalString contactType;

    /**
     * Constructs an initially empty contact.
     */
    public DefaultContact() {
    }

    /**
     * Constructs a contact initialized to the specified online resource.
     *
     * @param resource  the on-line information that can be used to contact the individual or organization,
     *                  or {@code null} if none.
     */
    public DefaultContact(final OnlineResource resource) {
        this.onlineResources = singleton(resource, OnlineResource.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Contact)
     */
    public DefaultContact(final Contact object) {
        super(object);
        if (object != null) {
            hoursOfService      = object.getHoursOfService();
            contactInstructions = object.getContactInstructions();
            if (object instanceof DefaultContact) {
                final DefaultContact c = (DefaultContact) object;
                phones          = copyCollection(c.getPhones(), Telephone.class);
                addresses       = copyCollection(c.getAddresses(), Address.class);
                onlineResources = copyCollection(c.getOnlineResources(), OnlineResource.class);
                contactType     = c.getContactType();
            } else {
                phones          = singleton(object.getPhone(), Telephone.class);
                addresses       = singleton(object.getAddress(), Address.class);
                onlineResources = singleton(object.getOnlineResource(), OnlineResource.class);
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
     *       {@code DefaultContact}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultContact} instance is created using the
     *       {@linkplain #DefaultContact(Contact) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultContact castOrCopy(final Contact object) {
        if (object == null || object instanceof DefaultContact) {
            return (DefaultContact) object;
        }
        return new DefaultContact(object);
    }

    /**
     * Returns telephone numbers at which the organization or individual may be contacted.
     *
     * @return telephone numbers at which the organization or individual may be contacted.
     *
     * @since 0.5
     */
    // @XmlElement at the end of this class.
    @UML(identifier="phone", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Telephone> getPhones() {
        return phones = nonNullCollection(phones, Telephone.class);
    }

    /**
     * Sets telephone numbers at which the organization or individual may be contacted.
     *
     * @param  newValues  the new telephones.
     *
     * @since 0.5
     */
    public void setPhones(Collection<? extends Telephone> newValues) {
        phones = writeCollection(newValues, phones, Telephone.class);
        /*
         * Code below this point will be deleted after we removed the deprecated methods in DefaultTelephone.
         * This code notifies all DefaultTelephone instances about the list of phones in order to allow the
         * deprecated Telephone.getVoices() and Telephone.getFacsimiles() methods to fetches information from
         * the phones list.
         */
        if (phones != null) {
            boolean modified = false;
            final Telephone[] p = phones.toArray(new Telephone[newValues.size()]);
            for (int i=0; i<p.length; i++) {
                final Telephone phone = p[i];
                if (phone instanceof DefaultTelephone) {
                    p[i] = ((DefaultTelephone) phone).setOwner(phones);
                    modified |= (p[i] != phone);
                }
            }
            if (modified) {
                phones.clear();
                Collections.addAll(phones, p);
            }
        }
    }

    /**
     * Returns telephone numbers at which the organization or individual may be contacted.
     * This method returns the first telephone number associated to {@code TelephoneType.VOICE}
     * or {@code TelephoneType.FACSIMILE FACSIMILE}.
     *
     * @return telephone numbers at which the organization or individual may be contacted, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getPhones()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getPhones")
    @XmlElement(name = "phone", namespace = LegacyNamespaces.GMD)
    public Telephone getPhone() {
        Telephone phone = null;
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            final Collection<Telephone> phones = getPhones();
            if (phones != null) { // May be null on marshalling.
                CodeList<?> ignored = null;
                for (final Telephone c : phones) {
                    if (c instanceof DefaultTelephone) {
                        String name;
                        final CodeList<?> type = ((DefaultTelephone) c).numberType;
                        if (type != null && ("VOICE".equals(name = type.name()) || "FACSIMILE".equals(name))) {
                            if (phone == null) {
                                phone = c;
                            }
                        } else if (ignored == null) {
                            ignored = type;
                        }
                    }
                }
                if (ignored != null) {
                    /*
                     * Log a warning for ignored property using a call to `ignored.toString()` instead of `ignored`
                     * because we want the property to appear as "TelephoneType[FOO]" instead of "FOO".
                     */
                    Context.warningOccured(Context.current(), DefaultContact.class, "getPhone",
                            Messages.class, Messages.Keys.IgnoredPropertyAssociatedTo_1, ignored);
                }
            }
        }
        return phone;
    }

    /**
     * Sets telephone numbers at which the organization or individual may be contacted.
     * This method delegates to {@link #setPhones(Collection)}.
     *
     * @param  newValue  the new telephone, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setPhones(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setPhone(Telephone newValue) {
        Collection<Telephone> newValues = null;
        if (newValue != null) {
            if (newValue instanceof DefaultTelephone) {
                newValues = ((DefaultTelephone) newValue).getOwner();
            } else {
                newValues = new ArrayList<>(4);
                for (String number : newValue.getVoices()) {
                    newValues.add(new DefaultTelephone(number, UnsupportedCodeList.VOICE));
                }
                for (String number : newValue.getFacsimiles()) {
                    newValues.add(new DefaultTelephone(number, UnsupportedCodeList.FACSIMILE));
                }
            }
        }
        setPhones(newValues);
    }

    /**
     * Returns the physical and email addresses at which the organization or individual may be contacted.
     *
     * @return physical and email addresses at which the organization or individual may be contacted, or {@code null}.
     *
     * @since 0.5
     */
    // @XmlElement at the end of this class.
    @UML(identifier="address", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Address> getAddresses() {
        return addresses = nonNullCollection(addresses, Address.class);
    }

    /**
     * Sets the physical and email addresses at which the organization or individual may be contacted.
     *
     * @param  newValues  the new addresses.
     *
     * @since 0.5
     */
    public void setAddresses(final Collection<? extends Address> newValues) {
        addresses = writeCollection(newValues, addresses, Address.class);
    }

    /**
     * Returns the physical and email address at which the organization or individual may be contacted.
     * This method returns the first {@link #getAddresses() adress} element, or null if none.
     *
     * @return physical and email address at which the organization or individual may be contacted, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getAddresses()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getAddresses")
    @XmlElement(name = "address", namespace = LegacyNamespaces.GMD)
    public Address getAddress() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            return LegacyPropertyAdapter.getSingleton(getAddresses(), Address.class, null, DefaultContact.class, "getAddress");
        }
        return null;                // Marshalling newer ISO 19115-3
    }

    /**
     * Sets the physical and email address at which the organization or individual may be contacted.
     * This method delegates to {@link #setAddresses(Collection)}.
     *
     * @param  newValue  the new address, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setAddresses(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setAddress(final Address newValue) {
        setAddresses(CollectionsExt.singletonOrEmpty(newValue));
    }

    /**
     * Returns on-line information that can be used to contact the individual or organization.
     *
     * @return on-line information that can be used to contact the individual or organization.
     *
     * @since 0.5
     */
    // @XmlElement at the end of this class.
    @UML(identifier="onlineResource", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<OnlineResource> getOnlineResources() {
        return onlineResources = nonNullCollection(onlineResources, OnlineResource.class);
    }

    /**
     * Sets on-line information that can be used to contact the individual or organization.
     *
     * @param  newValues  the new online resources.
     *
     * @since 0.5
     */
    public void setOnlineResources(final Collection<? extends OnlineResource> newValues) {
        onlineResources = writeCollection(newValues, onlineResources, OnlineResource.class);
    }

    /**
     * Returns on-line information that can be used to contact the individual or organization.
     * This method returns the first {@link #getOnlineResources() online resource} element, or null if none.
     *
     * @return on-line information that can be used to contact the individual or organization, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getOnlineResources()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getOnlineResources")
    @XmlElement(name = "onlineResource", namespace = LegacyNamespaces.GMD)
    public OnlineResource getOnlineResource() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            return LegacyPropertyAdapter.getSingleton(getOnlineResources(), OnlineResource.class, null, DefaultContact.class, "getOnlineResource");
        }
        return null;                // Marshalling newer ISO 19115-3
    }

    /**
     * Sets on-line information that can be used to contact the individual or organization.
     * This method delegates to {@link #setOnlineResources(Collection)}.
     *
     * @param  newValue  the new online resource, or {@code null} if none.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setOnlineResources(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setOnlineResource(final OnlineResource newValue) {
        setOnlineResources(CollectionsExt.singletonOrEmpty(newValue));
    }

    /**
     * Returns the time period (including time zone) when individuals can contact the organization or individual.
     *
     * <div class="warning"><b>Upcoming API change — multiplicity</b><br>
     * As of ISO 19115:2014, this singleton has been replaced by a collection.
     * This change will tentatively be applied in GeoAPI 4.0.
     * </div>
     *
     * @return time period when individuals can contact the organization or individual.
     */
    @Override
    @XmlElement(name = "hoursOfService")
    public InternationalString getHoursOfService() {
        return hoursOfService;
    }

    /**
     * Sets time period (including time zone) when individuals can contact the organization or individual.
     *
     * <div class="warning"><b>Upcoming API change — multiplicity</b><br>
     * As of ISO 19115:2014, this singleton has been replaced by a collection.
     * This change will tentatively be applied in GeoAPI 4.0.
     * </div>
     *
     * @param  newValue  the new hours of service.
     */
    public void setHoursOfService(final InternationalString newValue) {
        checkWritePermission(hoursOfService);
        hoursOfService = newValue;
    }

    /**
     * Returns supplemental instructions on how or when to contact the individual or organization.
     *
     * @return supplemental instructions on how or when to contact the individual or organization, or {@code null}.
     */
    @Override
    @XmlElement(name = "contactInstructions")
    public InternationalString getContactInstructions() {
        return contactInstructions;
    }

    /**
     * Sets supplemental instructions on how or when to contact the individual or organization.
     *
     * @param  newValue  the new contact instructions, or {@code null} if none.
     */
    public void setContactInstructions(final InternationalString newValue) {
        checkWritePermission(contactInstructions);
        contactInstructions = newValue;
    }

    /**
     * Type of the contact.
     * Returns {@code null} if none.
     *
     * @return type of the contact, or {@code null} if none.
     *
     * @since 0.5
     */
    @XmlElement(name = "contactType")
    @XmlJavaTypeAdapter(InternationalStringAdapter.Since2014.class)
    @UML(identifier="contactType", obligation=OPTIONAL, specification=ISO_19115)
    public InternationalString getContactType() {
        return contactType;
    }

    /**
     * Sets new type of the contact.
     *
     * @param  newValue  the new type of the contact.
     *
     * @since 0.5
     */
    public void setContactType(final InternationalString newValue) {
        checkWritePermission(contactType);
        contactType = newValue;
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
    @XmlElement(name = "phone")
    private Collection<Telephone> getPhoneList() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getPhones() : null;
    }

    @XmlElement(name = "address")
    private Collection<Address> getAddressList() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getAddresses() : null;
    }

    @XmlElement(name = "onlineResource")
    private Collection<OnlineResource> getOnlineResourceList() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getOnlineResources() : null;
    }
}
