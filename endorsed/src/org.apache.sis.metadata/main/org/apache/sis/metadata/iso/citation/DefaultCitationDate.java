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

import java.util.Date;
import java.time.temporal.Temporal;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.temporal.TemporalDate;


/**
 * Reference date and event used to describe it.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code CI_Date}
 * {@code   ├─date………………} Reference date for the cited resource.
 * {@code   └─dateType……} Event used for reference date.</div>
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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @version 1.5
 * @since   0.3
 */
@TitleProperty(name = "referenceDate")
@XmlType(name = "CI_Date_Type", propOrder = {
    "referenceDate",
    "dateType"
})
@XmlRootElement(name = "CI_Date")
public class DefaultCitationDate extends ISOMetadata implements CitationDate {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1032356967666782327L;

    /**
     * Reference date for the cited resource.
     */
    @SuppressWarnings("serial")     // Most implementations are serializable.
    private Temporal date;

    /**
     * Event used for reference date.
     */
    private DateType dateType;

    /**
     * Constructs an initially empty citation date.
     */
    public DefaultCitationDate() {
    }

    /**
     * Constructs a citation date initialized to the given date.
     *
     * @param date      the reference date for the cited resource, or {@code null} if unknown.
     * @param dateType  the event used for reference date, or {@code null} if unknown.
     *
     * @since 1.5
     */
    public DefaultCitationDate(final Temporal date, final DateType dateType) {
        this.date = date;
        this.dateType = dateType;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(CitationDate)
     */
    public DefaultCitationDate(final CitationDate object) {
        super(object);
        if (object != null) {
            date     = object.getReferenceDate();
            dateType = object.getDateType();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultCitationDate}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultCitationDate} instance is created using the
     *       {@linkplain #DefaultCitationDate(CitationDate) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultCitationDate castOrCopy(final CitationDate object) {
        if (object == null || object instanceof DefaultCitationDate) {
            return (DefaultCitationDate) object;
        }
        return new DefaultCitationDate(object);
    }

    /**
     * Returns the reference date for the cited resource.
     *
     * @return reference date for the cited resource, or {@code null}.
     *
     * @deprecated Replaced by {@link #getReferenceDate()}.
     */
    @Override
    @Deprecated(since="1.5")
    public Date getDate() {
        return TemporalDate.toDate(getReferenceDate());
    }

    /**
     * Sets the reference date for the cited resource.
     *
     * @param  newValue  the new date.
     *
     * @deprecated Replaced by {@link #setReferenceDate(Temporal)}.
     */
    @Deprecated(since="1.5")
    public void setDate(final Date newValue) {
        setReferenceDate(TemporalDate.toTemporal(newValue));
    }

    /**
     * Returns the reference date for the cited resource.
     *
     * @return reference date for the cited resource, or {@code null}.
     *
     * @since 1.5
     */
    @Override
    @XmlElement(name = "date", required = true)
    public Temporal getReferenceDate() {
        return date;
    }

    /**
     * Sets the reference date for the cited resource.
     * The specified value should be an instance of {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
     * {@link java.time.OffsetDateTime} or {@link java.time.ZonedDateTime}, depending whether hours are defined
     * and how the timezone (if any) is defined. But other types are also allowed.
     * For example, a citation date may be merely a {@link java.time.Year}.
     *
     * @param  newValue  the new date.
     *
     * @since 1.5
     */
    public void setReferenceDate(final Temporal newValue) {
        checkWritePermission(date);
        date = newValue;
    }

    /**
     * Returns the event used for reference date.
     *
     * @return event used for reference date, or {@code null}.
     */
    @Override
    @XmlElement(name = "dateType", required = true)
    public DateType getDateType() {
        return dateType;
    }

    /**
     * Sets the event used for reference date.
     *
     * @param  newValue  the new event.
     */
    public void setDateType(final DateType newValue) {
        checkWritePermission(dateType);
        dateType = newValue;
    }
}
