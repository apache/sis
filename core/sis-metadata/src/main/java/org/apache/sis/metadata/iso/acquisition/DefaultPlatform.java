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
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
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
    private static final long serialVersionUID = -1607271701134014369L;

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
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Platform)
     */
    public DefaultPlatform(final Platform object) {
        super(object);
        if (object != null) {
            citation    = object.getCitation();
            identifiers = singleton(object.getIdentifier(), Identifier.class);
            description = object.getDescription();
            sponsors    = copyCollection(object.getSponsors(), ResponsibleParty.class);
            instruments = copyCollection(object.getInstruments(), Instrument.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultPlatform}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultPlatform} instance is created using the
     *       {@linkplain #DefaultPlatform(Platform) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultPlatform castOrCopy(final Platform object) {
        if (object == null || object instanceof DefaultPlatform) {
            return (DefaultPlatform) object;
        }
        return new DefaultPlatform(object);
    }

    /**
     * Returns the source where information about the platform is described. {@code null} if unspecified.
     *
     * @return Source where information about the platform is described, or {@code null}.
     */
    @Override
    @XmlElement(name = "citation")
    public Citation getCitation() {
        return citation;
    }

    /**
     * Sets the source where information about the platform is described.
     *
     * @param newValue The new citation value.
     */
    public void setCitation(final Citation newValue) {
        checkWritePermission();
        citation = newValue;
    }

    /**
     * Returns the unique identification of the platform.
     *
     * @return Unique identification of the platform, or {@code null}.
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
    public void setIdentifier(final Identifier newValue) {
        checkWritePermission();
        identifiers = nonNullCollection(identifiers, Identifier.class);
        NonMarshalledAuthority.setMarshallable(identifiers, newValue);
    }

    /**
     * Gets the narrative description of the platform supporting the instrument.
     *
     * @return Narrative description of the platform, or {@code null}.
     */
    @Override
    @XmlElement(name = "description", required = true)
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the narrative description of the platform supporting the instrument.
     *
     * @param newValue The new description value.
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Returns the organization responsible for building, launch, or operation of the platform.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@link Responsibility} parent interface.
     * This change will be tentatively applied in GeoAPI 4.0.
     * </div>
     *
     * @return Organization responsible for building, launch, or operation of the platform.
     */
    @Override
    @XmlElement(name = "sponsor")
    public Collection<ResponsibleParty> getSponsors() {
        return sponsors = nonNullCollection(sponsors, ResponsibleParty.class);
    }

    /**
     * Sets the organization responsible for building, launch, or operation of the platform.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@link Responsibility} parent interface.
     * This change will be tentatively applied in GeoAPI 4.0.
     * </div>
     *
     * @param newValues The new sponsors values;
     */
    public void setSponsors(final Collection<? extends ResponsibleParty> newValues) {
        sponsors = writeCollection(newValues, sponsors, ResponsibleParty.class);
    }

    /**
     * Gets the instrument(s) mounted on a platform.
     *
     * @return Instrument(s) mounted on a platform.
     */
    @Override
    @XmlElement(name = "instrument", required = true)
    public Collection<Instrument> getInstruments() {
        return instruments = nonNullCollection(instruments, Instrument.class);
    }

    /**
     * Sets the instrument(s) mounted on a platform.
     *
     * @param newValues The new instruments values.
     */
    public void setInstruments(final Collection<? extends Instrument> newValues) {
        instruments = writeCollection(newValues, instruments, Instrument.class);
    }
}
