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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.opengis.metadata.citation.Contact;
///import org.opengis.metadata.citation.Party;
import org.opengis.util.InternationalString;


/**
 * Information about the individual and / or organisation of the party.
 *
 * @author  Remi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlType(name = "CI_Party_Type", propOrder = {
/// "name",
/// "contactInfo"
})
@XmlRootElement(name = "CI_Party")
public class AbstractParty extends ISOMetadata /*implements Party*/ {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 1486981243884830979L;

    /**
     * Name of the party.
     */
    private InternationalString name;

    /**
     * Contact information for the party.
     */
    private Collection<Contact> contactInfo;

    /**
     * Constructs an initially empty party.
     */
    public AbstractParty() {
    }

    /**
     * Constructs a party initialized with the specified name and contact information.
     *
     * @param name        Name of the party, or {@code null} if none.
     * @param contactInfo Contact information for the party, or {@code null} if none.
     */
    public AbstractParty(final InternationalString name, final Contact contactInfo) {
        this.name        = name;
        this.contactInfo = singleton(contactInfo, Contact.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Party)
     */
    public AbstractParty(final AbstractParty object) {
        super(object);
        if (object != null) {
            name        = object.getName();
            contactInfo = copyList(object.getContactInfo(), Contact.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractParty}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractParty} instance is created using the
     *       {@linkplain #AbstractParty(Party) copy constructor} and returned.
     *       Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractParty castOrCopy(final AbstractParty object) {
        if (object == null || object instanceof AbstractParty) {
            return (AbstractParty) object;
        }
        return new AbstractParty(object);
    }

    /**
     * Return the name of the party.
     *
     * @return Name of the party.
     */
/// @Override
/// @XmlElement(name = "name")
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the name of the party.
     *
     * @param newValue The new name of the party.
     */
    public void setName(final InternationalString newValue) {
       checkWritePermission();
       name = newValue;
    }

    /**
     * Returns the contact information for the party, or {@code null} if none.
     *
     * @return Contact information for the party, or {@code null} if none.
     */
/// @Override
/// @XmlElement(name = "contactInfo")
    public Collection<Contact> getContactInfo() {
        return contactInfo = nonNullCollection(contactInfo, Contact.class);
    }

    /**
     * Sets the contact information for the party.
     *
     * @param newValues The new contact information for the party.
     */
    public void setContactInfo(final Collection< ? extends Contact> newValues) {
        contactInfo = writeCollection(newValues, contactInfo, Contact.class);
    }
}
