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
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;


/**
 * Designations for the measuring instruments.
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
@XmlType(name = "MI_Instrument_Type", propOrder = {
    "citations",
    "identifier",
    "type",
    "description",
    "mountedOn"
})
@XmlRootElement(name = "MI_Instrument")
public class DefaultInstrument extends ISOMetadata implements Instrument {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7439143424271079960L;

    /**
     * Complete citation of the instrument.
     */
    private Collection<Citation> citations;

    /**
     * Name of the type of instrument. Examples: framing, line-scan, push-broom, pan-frame.
     */
    private InternationalString type;

    /**
     * Textual description of the instrument.
     */
    private InternationalString description;

    /**
     * Platform on which the instrument is mounted.
     */
    private Platform mountedOn;

    /**
     * Constructs an initially empty instrument.
     */
    public DefaultInstrument() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Instrument)
     */
    public DefaultInstrument(final Instrument object) {
        super(object);
        if (object != null) {
            citations   = copyCollection(object.getCitations(), Citation.class);
            identifiers = singleton(object.getIdentifier(), Identifier.class);
            type        = object.getType();
            description = object.getDescription();
            mountedOn   = object.getMountedOn();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultInstrument}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultInstrument} instance is created using the
     *       {@linkplain #DefaultInstrument(Instrument) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultInstrument castOrCopy(final Instrument object) {
        if (object == null || object instanceof DefaultInstrument) {
            return (DefaultInstrument) object;
        }
        return new DefaultInstrument(object);
    }

    /**
     * Returns the complete citation of the instrument.
     *
     * @return Complete citation of the instrument.
     */
    @Override
    @XmlElement(name = "citation")
    public Collection<Citation> getCitations() {
        return citations = nonNullCollection(citations, Citation.class);
    }

    /**
     * Sets the complete citation of the instrument.
     *
     * @param newValues The new citation values.
     */
    public void setCitations(final Collection<? extends Citation> newValues) {
        citations = writeCollection(newValues, citations, Citation.class);
    }

    /**
     * Returns the unique identification of the instrument.
     *
     * @return Unique identification of the instrument, or {@code null}.
     */
    @Override
    @XmlElement(name = "identifier", required = true)
    public Identifier getIdentifier() {
        return NonMarshalledAuthority.getMarshallable(identifiers);
    }

    /**
     * Sets the unique identification of the instrument.
     *
     * @param newValue The new identifier value.
     */
    public void setIdentifier(final Identifier newValue) {
        checkWritePermission();
        identifiers = nonNullCollection(identifiers, Identifier.class);
        NonMarshalledAuthority.setMarshallable(identifiers, newValue);
    }

    /**
     * Returns the name of the type of instrument. Examples: framing, line-scan, push-broom, pan-frame.
     *
     * @return Type of instrument, or {@code null}.
     */
    @Override
    @XmlElement(name = "type", required = true)
    public InternationalString getType() {
        return type;
    }

    /**
     * Sets the name of the type of instrument. Examples: framing, line-scan, push-broom, pan-frame.
     *
     * @param newValue The new type value.
     */
    public void setType(final InternationalString newValue) {
        checkWritePermission();
        type = newValue;
    }

    /**
     * Returns the textual description of the instrument. {@code null} if unspecified.
     *
     * @return Textual description, or {@code null}.
     */
    @Override
    @XmlElement(name = "description")
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the textual description of the instrument.
     *
     * @param newValue The new description value.
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Returns the platform on which the instrument is mounted. {@code null} if unspecified.
     *
     * @return Platform on which the instrument is mounted, or {@code null}.
     */
    @Override
    @XmlElement(name = "mountedOn")
    public Platform getMountedOn() {
        return mountedOn;
    }

    /**
     * Sets the platform on which the instrument is mounted.
     *
     * @param newValue The new platform value.
     */
    public void setMountedOn(final Platform newValue) {
        checkWritePermission();
        mountedOn = newValue;
    }
}
