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
import javax.xml.bind.annotation.XmlSeeAlso;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Contact;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.util.iso.Types;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about the individual and / or organization of the party.
 * The following property is conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code CI_Party}
 * {@code   └─name……} Name of the party.</div>
 *
 * <div class="warning"><b>Note on International Standard versions</b><br>
 * This class is derived from a new type defined in the ISO 19115 international standard published in 2014,
 * while GeoAPI 3.0 is based on the version published in 2003. Consequently this implementation class does
 * not yet implement a GeoAPI interface, but is expected to do so after the next GeoAPI releases.
 * When the interface will become available, all references to this implementation class in Apache SIS will
 * be replaced be references to the {@code Party} interface.
 * </div>
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
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.5
 */
@TitleProperty(name = "name")
@XmlType(name = "AbstractCI_Party_Type", propOrder = {
    "name",
    "contactInfo",
    "identifiers"
})
@XmlRootElement(name = "AbstractCI_Party")
@XmlSeeAlso({
    DefaultIndividual.class,
    DefaultOrganisation.class
})
@UML(identifier="CI_Party", specification=ISO_19115)
public class AbstractParty extends ISOMetadata {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 1486981243884830979L;

    /**
     * Name of the party.
     */
    @SuppressWarnings("serial")
    private InternationalString name;

    /**
     * Contact information for the party.
     */
    @SuppressWarnings("serial")
    private Collection<Contact> contactInfo;

    /**
     * Constructs an initially empty party.
     */
    public AbstractParty() {
    }

    /**
     * Constructs a party initialized with the specified name and contact information.
     *
     * @param name         name of the party, or {@code null} if none.
     * @param contactInfo  contact information for the party, or {@code null} if none.
     */
    public AbstractParty(final CharSequence name, final Contact contactInfo) {
        this.name        = Types.toInternationalString(name);
        this.contactInfo = singleton(contactInfo, Contact.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     */
    public AbstractParty(final AbstractParty object) {
        super(object);
        if (object != null) {
            name        = object.getName();
            contactInfo = copyList(object.getContactInfo(), Contact.class);
        }
    }

    /**
     * Return the name of the party.
     *
     * @return name of the party.
     */
    @XmlElement(name = "name")
    @UML(identifier="name", obligation=CONDITIONAL, specification=ISO_19115)
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the name of the party.
     *
     * @param  newValue  the new name of the party.
     */
    public void setName(final InternationalString newValue) {
       checkWritePermission(name);
       name = newValue;
    }

    /**
     * Identifiers of the party.
     *
     * <h4>Unified identifiers view</h4>
     * In this SIS implementation, the collection returned by this method includes the XML identifiers
     * ({@linkplain IdentifierSpace#ID ID}, {@linkplain IdentifierSpace#UUID UUID}, <i>etc.</i>),
     * thus providing a unified view of every kind of identifiers associated to this party.
     *
     * <h4>XML marshalling note</h4>
     * The {@code <mac:identifier>} element marshalled to XML will exclude all the above cited identifiers,
     * for compliance with ISO 19115 model. Those identifiers will appear in other XML elements or attributes.
     *
     * @return identifiers of the party, or an empty collection if none.
     *
     * @since 1.3
     */
    @Override
    @XmlElement(name = "partyIdentifier")
    public Collection<Identifier> getIdentifiers() {
        return NonMarshalledAuthority.filterOnMarshalling(super.getIdentifiers());
    }

    /**
     * Sets the identifiers of the party.
     *
     * <p>XML identifiers ({@linkplain IdentifierSpace#ID ID}, {@linkplain IdentifierSpace#UUID UUID}, <i>etc.</i>),
     * are not affected by this method, unless they are explicitly provided in the given collection.</p>
     *
     * @param  newValues  the new identifiers values.
     *
     * @since 1.3
     */
    public void setIdentifiers(Collection<? extends Identifier> newValues) {
        newValues = NonMarshalledAuthority.setMarshallables(identifiers, newValues);
        identifiers = writeCollection(newValues, identifiers, Identifier.class);
    }

    /**
     * Returns the contact information for the party.
     *
     * @return contact information for the party.
     */
    @XmlElement(name = "contactInfo")
    @UML(identifier="contactInfo", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Contact> getContactInfo() {
        return contactInfo = nonNullCollection(contactInfo, Contact.class);
    }

    /**
     * Sets the contact information for the party.
     *
     * @param  newValues  the new contact information for the party.
     */
    public void setContactInfo(final Collection<? extends Contact> newValues) {
        contactInfo = writeCollection(newValues, contactInfo, Contact.class);
    }
}
