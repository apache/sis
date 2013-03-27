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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.acquisition.Instrument;
import org.opengis.metadata.acquisition.Platform;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;


/**
 * Designation of the platform used to acquire the dataset.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 */
@XmlType(name = "MI_Platform_Type", propOrder = {
    "citation",
    "identifier",
    "description",
    "sponsors",
    "instruments"
})
@XmlRootElement(name = "MI_Platform")
public class DefaultPlatform extends ISOMetadata implements Platform {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6870357428019309408L;

    /**
     * Source where information about the platform is described.
     */
    private Citation citation;

    /**
     * Narrative description of the platform supporting the instrument.
     */
    private InternationalString description;

    /**
     * Organization responsible for building, launch, or operation of the platform.
     */
    private Collection<ResponsibleParty> sponsors;

    /**
     * Instrument(s) mounted on a platform.
     */
    private Collection<Instrument> instruments;

    /**
     * Constructs an initially empty platform.
     */
    public DefaultPlatform() {
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
    public static DefaultPlatform castOrCopy(final Platform object) {
        if (object == null || object instanceof DefaultPlatform) {
            return (DefaultPlatform) object;
        }
        final DefaultPlatform copy = new DefaultPlatform();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the source where information about the platform is described. {@code null}
     * if unspecified.
     */
    @Override
    @XmlElement(name = "citation")
    public synchronized Citation getCitation() {
        return citation;
    }

    /**
     * Sets the source where information about the platform is described.
     *
     * @param newValue The new citation value.
     */
    public synchronized void setCitation(final Citation newValue) {
        checkWritePermission();
        citation = newValue;
    }

    /**
     * Returns the unique identification of the platform.
     */
    @Override
    @XmlElement(name = "identifier", required = true)
    public Identifier getIdentifier() {
        return NonMarshalledAuthority.getMarshallable(identifiers);
    }

    /**
     * Sets the unique identification of the platform.
     *
     * @param newValue The new identifier value.
     */
    public synchronized void setIdentifier(final Identifier newValue) {
        checkWritePermission();
        identifiers = nonNullCollection(identifiers, Identifier.class);
        NonMarshalledAuthority.setMarshallable(identifiers, newValue);
    }

    /**
     * Gets the narrative description of the platform supporting the instrument.
     */
    @Override
    @XmlElement(name = "description", required = true)
    public synchronized InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the narrative description of the platform supporting the instrument.
     *
     * @param newValue The new description value.
     */
    public synchronized void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Returns the organization responsible for building, launch, or operation of the platform.
     */
    @Override
    @XmlElement(name = "sponsor")
    public synchronized Collection<ResponsibleParty> getSponsors() {
        return sponsors = nonNullCollection(sponsors, ResponsibleParty.class);
    }

    /**
     * Sets the organization responsible for building, launch, or operation of the platform.
     *
     * @param newValues The new sponsors values;
     */
    public synchronized void setSponsors(final Collection<? extends ResponsibleParty> newValues) {
        sponsors = writeCollection(newValues, sponsors, ResponsibleParty.class);
    }

    /**
     * Gets the instrument(s) mounted on a platform.
     */
    @Override
    @XmlElement(name = "instrument", required = true)
    public synchronized Collection<Instrument> getInstruments() {
        return instruments = nonNullCollection(instruments, Instrument.class);
    }

    /**
     * Sets the instrument(s) mounted on a platform.
     *
     * @param newValues The new instruments values.
     */
    public synchronized void setInstruments(final Collection<? extends Instrument> newValues) {
        instruments = writeCollection(newValues, instruments, Instrument.class);
    }
}
