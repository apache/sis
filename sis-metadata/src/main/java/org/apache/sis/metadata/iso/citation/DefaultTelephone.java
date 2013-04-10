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
import org.opengis.metadata.citation.Telephone;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Telephone numbers for contacting the responsible individual or organization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "CI_Telephone_Type", propOrder = {
    "voices",
    "facsimiles"
})
@XmlRootElement(name = "CI_Telephone")
public class DefaultTelephone extends ISOMetadata implements Telephone {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1920621361930970869L;

    /**
     * Telephone numbers by which individuals can speak to the responsible organization or individual.
     */
    private Collection<String> voices;

    /**
     * Telephone numbers of a facsimile machine for the responsible organization or individual.
     */
    private Collection<String> facsimiles;

    /**
     * Constructs a default telephone.
     */
    public DefaultTelephone() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(Telephone)
     */
    public DefaultTelephone(final Telephone object) {
        super(object);
        voices     = copyCollection(object.getVoices(), String.class);
        facsimiles = copyCollection(object.getFacsimiles(), String.class);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultTelephone}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultTelephone} instance is created using the
     *       {@linkplain #DefaultTelephone(Telephone) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultTelephone castOrCopy(final Telephone object) {
        if (object == null || object instanceof DefaultTelephone) {
            return (DefaultTelephone) object;
        }
        return new DefaultTelephone(object);
    }

    /**
     * Returns the telephone numbers by which individuals can speak to the responsible
     * organization or individual.
     */
    @Override
    @XmlElement(name = "voice")
    public synchronized Collection<String> getVoices() {
        return voices = nonNullCollection(voices, String.class);
    }

    /**
     * Sets the telephone numbers by which individuals can speak to the responsible
     * organization or individual.
     *
     * @param newValues The new telephone numbers, or {@code null} if none.
     */
    public synchronized void setVoices(final Collection<? extends String> newValues) {
        voices = writeCollection(newValues, voices, String.class);
    }

    /**
     * Returns the telephone numbers of a facsimile machine for the responsible organization
     * or individual.
     */
    @Override
    @XmlElement(name = "facsimile")
    public synchronized Collection<String> getFacsimiles() {
        return facsimiles = nonNullCollection(facsimiles, String.class);
    }

    /**
     * Sets the telephone number of a facsimile machine for the responsible organization
     * or individual.
     *
     * @param newValues The new telephone number, or {@code null} if none.
     */
    public synchronized void setFacsimiles(final Collection<? extends String> newValues) {
        facsimiles = writeCollection(newValues, facsimiles, String.class);
    }
}
