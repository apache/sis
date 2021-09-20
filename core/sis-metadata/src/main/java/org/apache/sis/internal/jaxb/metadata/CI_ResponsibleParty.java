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
package org.apache.sis.internal.jaxb.metadata;

import javax.xml.bind.annotation.XmlElementRef;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.ResponsibleParty;
import org.apache.sis.metadata.iso.citation.AbstractParty;
import org.apache.sis.metadata.iso.citation.DefaultIndividual;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.internal.jaxb.FilterByVersion;


/**
 * JAXB adapter mapping implementing class to a legacy GeoAPI interface.
 * See package documentation for more information about JAXB and interface.
 *
 * @deprecated This adapter is not used anymore for ISO 19115-3:2014 metadata.
 * However it is needed for branches that depend on GeoAPI 3.x, and is also needed
 * for implementing web services that have not yet been upgraded to latest ISO standard.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
@Deprecated
public final class CI_ResponsibleParty extends PropertyType<CI_ResponsibleParty, ResponsibleParty> {
    /**
     * Empty constructor for JAXB only.
     */
    public CI_ResponsibleParty() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code ResponsibleParty.class}
     */
    @Override
    protected Class<ResponsibleParty> getBoundType() {
        return ResponsibleParty.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CI_ResponsibleParty(final ResponsibleParty value) {
        super(value);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <gmd:CI_ResponsibleParty>} XML element.
     *
     * @param  value  the metadata element to marshal.
     * @return a {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected CI_ResponsibleParty wrap(final ResponsibleParty value) {
        return new CI_ResponsibleParty(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <gmd:CI_ResponsibleParty>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElementRef
    public DefaultResponsibility getElement() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            return DefaultResponsibleParty.castOrCopy(metadata);
        } else if (metadata != null) {
            final DefaultIndividual individual;
            final String name = metadata.getIndividualName();
            Contact contact = metadata.getContactInfo();
            AbstractParty party;
            if (name != null) {
                individual = new DefaultIndividual(name, metadata.getPositionName(), contact);
                party      = individual;
                contact    = null;
            } else {
                individual = null;
                party      = null;
            }
            final InternationalString organisation = metadata.getOrganisationName();
            if (organisation != null) {
                party = new DefaultOrganisation(organisation, null, individual, contact);
            }
            if (party != null) {
                return new DefaultResponsibility(metadata.getRole(), null, party);
            }
        }
        return null;
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  value  the unmarshalled metadata.
     */
    public void setElement(final DefaultResponsibility value) {
        if (value instanceof DefaultResponsibleParty) {
            metadata = (DefaultResponsibleParty) value;
        } else if (value != null) {
            metadata = new DefaultResponsibleParty(value);
        }
    }
}
