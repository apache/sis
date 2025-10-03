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

import java.time.temporal.Temporal;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.acquisition.RequestedDate;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Range of date validity.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MI_RequestedDate}
 * {@code   ├─requestedDateOfCollection……} Preferred date and time of collection.
 * {@code   └─latestAcceptableDate…………………} Latest date and time collection must be completed.</div>
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
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 * @since   0.3
 */
@XmlType(name = "MI_RequestedDate_Type", propOrder = {
    "requestedDateOfCollection",
    "latestAcceptableDate"
})
@XmlRootElement(name = "MI_RequestedDate")
public class DefaultRequestedDate extends ISOMetadata implements RequestedDate {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8491304736666217656L;

    /**
     * Preferred date and time of collection.
     */
    @SuppressWarnings("serial")     // Standard Java implementations are serializable.
    private Temporal requestedDateOfCollection;

    /**
     * Latest date and time collection must be completed.
     */
    @SuppressWarnings("serial")     // Standard Java implementations are serializable.
    private Temporal latestAcceptableDate;

    /**
     * Constructs an initially empty requested date.
     */
    public DefaultRequestedDate() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(RequestedDate)
     */
    public DefaultRequestedDate(final RequestedDate object) {
        super(object);
        if (object != null) {
            requestedDateOfCollection = object.getRequestedDateOfCollection();
            latestAcceptableDate      = object.getLatestAcceptableDate();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultRequestedDate}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultRequestedDate} instance is created using the
     *       {@linkplain #DefaultRequestedDate(RequestedDate) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultRequestedDate castOrCopy(final RequestedDate object) {
        if (object == null || object instanceof DefaultRequestedDate) {
            return (DefaultRequestedDate) object;
        }
        return new DefaultRequestedDate(object);
    }

    /**
     * Returns the preferred date and time of collection.
     *
     * @return preferred date and time, or {@code null}.
     * @version 2.0
     */
    @Override
    @XmlElement(name = "requestedDateOfCollection", required = true)
    public Temporal getRequestedDateOfCollection() {
        return requestedDateOfCollection;
    }

    /**
     * Sets the preferred date and time of collection.
     * The specified value should be an instance of {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
     * {@link java.time.OffsetDateTime} or {@link java.time.ZonedDateTime}, depending whether hours are defined
     * and how the timezone (if any) is defined. But other types are also allowed.
     *
     * @param  newValue  the new requested date of collection value.
     *
     * @since 1.5
     */
    public void setRequestedDateOfCollection(final Temporal newValue) {
        checkWritePermission(requestedDateOfCollection);
        requestedDateOfCollection = newValue;
    }

    /**
     * Returns the latest date and time collection must be completed.
     *
     * @return latest date and time, or {@code null}.
     * @version 2.0
     */
    @Override
    @XmlElement(name = "latestAcceptableDate", required = true)
    public Temporal getLatestAcceptableDate() {
        return latestAcceptableDate;
    }

    /**
     * Sets the latest date and time collection must be completed.
     * The specified value should be an instance of {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
     * {@link java.time.OffsetDateTime} or {@link java.time.ZonedDateTime}, depending whether hours are defined
     * and how the timezone (if any) is defined. But other types are also allowed.
     *
     * @param  newValue  the new latest acceptable data value.
     *
     * @since 1.5
     */
    public void setLatestAcceptableDate(final Temporal newValue) {
        checkWritePermission(latestAcceptableDate);
        latestAcceptableDate = newValue;
    }
}
