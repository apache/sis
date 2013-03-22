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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Address;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.Telephone;
import org.opengis.metadata.citation.OnlineResource;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Information required to enable contact with the responsible person and/or organization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "CI_Contact_Type", propOrder = {
    "phone",
    "address",
    "onlineResource",
    "hoursOfService",
    "contactInstructions"
})
@XmlRootElement(name = "CI_Contact")
public class DefaultContact extends ISOMetadata implements Contact {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3283637180253117382L;

    /**
     * Telephone numbers at which the organization or individual may be contacted.
     */
    private Telephone phone;

    /**
     * Physical and email address at which the organization or individual may be contacted.
     */
    private Address address;

    /**
     * On-line information that can be used to contact the individual or organization.
     */
    private OnlineResource onlineResource;

    /**
     * Time period (including time zone) when individuals can contact the organization or individual.
     */
    private InternationalString hoursOfService;

    /**
     * Supplemental instructions on how or when to contact the individual or organization.
     */
    private InternationalString contactInstructions;

    /**
     * Constructs an initially empty contact.
     */
    public DefaultContact() {
    }

    /**
     * Constructs a contact initialized to the specified online resource.
     *
     * @param resource The on-line information that can be used to contact the individual or
     *        organization, or {@code null} if none.
     */
    public DefaultContact(final OnlineResource resource) {
        this.onlineResource = resource;
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultContact castOrCopy(final Contact object) {
        if (object == null || object instanceof DefaultContact) {
            return (DefaultContact) object;
        }
        final DefaultContact copy = new DefaultContact();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns telephone numbers at which the organization or individual may be contacted.
     */
    @Override
    @XmlElement(name = "phone")
    public synchronized Telephone getPhone() {
        return phone;
    }

    /**
     * Sets telephone numbers at which the organization or individual may be contacted.
     *
     * @param newValue The new telephone, or {@code null} if none.
     */
    public synchronized void setPhone(final Telephone newValue) {
        checkWritePermission();
        phone = newValue;
    }

    /**
     * Returns the physical and email address at which the organization or individual may be contacted.
     */
    @Override
    @XmlElement(name = "address")
    public synchronized Address getAddress() {
        return address;
    }

    /**
     * Sets the physical and email address at which the organization or individual may be contacted.
     *
     * @param newValue The new address, or {@code null} if none.
     */
    public synchronized void setAddress(final Address newValue) {
        checkWritePermission();
        address = newValue;
    }

    /**
     * Return on-line information that can be used to contact the individual or organization.
     */
    @Override
    @XmlElement(name = "onlineResource")
    public synchronized OnlineResource getOnlineResource() {
        return onlineResource;
    }

    /**
     * Sets on-line information that can be used to contact the individual or organization.
     *
     * @param newValue The new online resource, or {@code null} if none.
     */
    public synchronized void setOnlineResource(final OnlineResource newValue) {
        checkWritePermission();
        onlineResource = newValue;
    }

    /**
     * Returns the time period (including time zone) when individuals can contact the organization
     * or individual.
     */
    @Override
    @XmlElement(name = "hoursOfService")
    public synchronized InternationalString getHoursOfService() {
        return hoursOfService;
    }

    /**
     * Sets time period (including time zone) when individuals can contact the organization or
     * individual.
     *
     * @param newValue The new hours of service, or {@code null} if none.
     */
    public synchronized void setHoursOfService(final InternationalString newValue) {
        checkWritePermission();
        hoursOfService = newValue;
    }

    /**
     * Returns supplemental instructions on how or when to contact the individual or organization.
     */
    @Override
    @XmlElement(name = "contactInstructions")
    public synchronized InternationalString getContactInstructions() {
        return contactInstructions;
    }

    /**
     * Sets supplemental instructions on how or when to contact the individual or organization.
     *
     * @param newValue The new contact instructions, or {@code null} if none.
     */
    public synchronized void setContactInstructions(final InternationalString newValue) {
        checkWritePermission();
        contactInstructions = newValue;
    }
}
