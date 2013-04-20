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

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Address;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Location of the responsible individual or organization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "CI_Address_Type", propOrder = {
    "deliveryPoints",
    "city",
    "administrativeArea",
    "postalCode",
    "country",
    "electronicMailAddresses"
})
@XmlRootElement(name = "CI_Address")
public class DefaultAddress extends ISOMetadata implements Address {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1357443146723845129L;

    /**
     * State, province of the location.
     */
    private InternationalString administrativeArea;

    /**
     * The city of the location
     */
    private InternationalString city;

   /**
     * Country of the physical address.
     */
    private InternationalString country;

    /**
     * ZIP or other postal code.
     */
    private String postalCode;

    /**
     * Address line for the location (as described in ISO 11180, Annex A).
     */
    private Collection<String> deliveryPoints;

    /**
     * Address of the electronic mailbox of the responsible organization or individual.
     */
    private Collection<String> electronicMailAddresses;

    /**
     * Constructs an initially empty address.
     */
    public DefaultAddress() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(Address)
     */
    public DefaultAddress(final Address object) {
        super(object);
        deliveryPoints          = copyCollection(object.getDeliveryPoints(), String.class);
        city                    = object.getCity();
        administrativeArea      = object.getAdministrativeArea();
        postalCode              = object.getPostalCode();
        country                 = object.getCountry();
        electronicMailAddresses = copyCollection(object.getElectronicMailAddresses(), String.class);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultAddress}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultAddress} instance is created using the
     *       {@linkplain #DefaultAddress(Address) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultAddress castOrCopy(final Address object) {
        if (object == null || object instanceof DefaultAddress) {
            return (DefaultAddress) object;
        }
        return new DefaultAddress(object);
    }

    /**
     * Return the state, province of the location.
     */
    @Override
    @XmlElement(name = "administrativeArea")
    public InternationalString getAdministrativeArea() {
        return administrativeArea;
    }

    /**
     * Sets the state, province of the location.
     *
     * @param newValue The new administrative area.
     */
    public void setAdministrativeArea(final InternationalString newValue) {
        checkWritePermission();
        administrativeArea = newValue;
    }

    /**
     * Returns the city of the location.
     */
    @Override
    @XmlElement(name = "city")
    public InternationalString getCity() {
        return city;
    }

    /**
     * Sets the city of the location.
     *
     * @param newValue The new city, or {@code null} if none.
     */
    public void setCity(final InternationalString newValue) {
        checkWritePermission();
        city = newValue;
    }

    /**
     * Returns the country of the physical address.
     */
    @Override
    @XmlElement(name = "country")
    public InternationalString getCountry() {
        return country;
    }

    /**
     * Sets the country of the physical address.
     *
     * @param newValue The new country, or {@code null} if none.
     */
    public void setCountry(final InternationalString newValue) {
        checkWritePermission();
        country = newValue;
    }

    /**
     * Returns the address line for the location (as described in ISO 11180, Annex A).
     */
    @Override
    @XmlElement(name = "deliveryPoint")
    public Collection<String> getDeliveryPoints() {
        return deliveryPoints = nonNullCollection(deliveryPoints, String.class);
    }

    /**
     * Sets the address line for the location (as described in ISO 11180, Annex A).
     *
     * @param newValues The new delivery points, or {@code null} if none.
     */
    public void setDeliveryPoints(final Collection<? extends String> newValues) {
        deliveryPoints = writeCollection(newValues, deliveryPoints, String.class);
    }

    /**
     * Returns the address of the electronic mailbox of the responsible organization or individual.
     */
    @Override
    @XmlElement(name = "electronicMailAddress")
    public Collection<String> getElectronicMailAddresses() {
        return electronicMailAddresses = nonNullCollection(electronicMailAddresses, String.class);
    }

    /**
     * Sets the address of the electronic mailbox of the responsible organization or individual.
     *
     * @param newValues The new electronic mail addresses, or {@code null} if none.
     */
    public void setElectronicMailAddresses(final Collection<? extends String> newValues) {
        electronicMailAddresses = writeCollection(newValues, electronicMailAddresses, String.class);
    }

    /**
     * Returns ZIP or other postal code.
     */
    @Override
    @XmlElement(name = "postalCode")
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * Sets ZIP or other postal code.
     *
     * @param newValue The new postal code, or {@code null} if none.
     */
    public void setPostalCode(final String newValue) {
        checkWritePermission();
        postalCode = newValue;
    }
}
