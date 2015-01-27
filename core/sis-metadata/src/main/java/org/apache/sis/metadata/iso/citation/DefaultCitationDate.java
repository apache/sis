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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.apache.sis.metadata.iso.ISOMetadata;

import static org.apache.sis.internal.metadata.MetadataUtilities.toDate;
import static org.apache.sis.internal.metadata.MetadataUtilities.toMilliseconds;


/**
 * Reference date and event used to describe it.
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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "CI_Date_Type", propOrder = {
    "date",
    "dateType"
})
@XmlRootElement(name = "CI_Date")
public class DefaultCitationDate extends ISOMetadata implements CitationDate {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5140213754542273710L;

    /**
     * Reference date for the cited resource in milliseconds elapsed sine January 1st, 1970,
     * or {@link Long#MIN_VALUE} if none.
     */
    private long date = Long.MIN_VALUE;

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
     * @param date     The reference date for the cited resource.
     * @param dateType The event used for reference date.
     */
    public DefaultCitationDate(final Date date, final DateType dateType) {
        this.date = toMilliseconds(date);
        this.dateType = dateType;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(CitationDate)
     */
    public DefaultCitationDate(final CitationDate object) {
        super(object);
        if (object != null) {
            date     = toMilliseconds(object.getDate());
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
     *       {@linkplain #DefaultCitationDate(CitationDate) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     * @return Reference date for the cited resource, or {@code null}.
     */
    @Override
    @XmlElement(name = "date", required = true)
    public Date getDate() {
        return toDate(date);
    }

    /**
     * Sets the reference date for the cited resource.
     *
     * @param newValue The new date.
     */
    public void setDate(final Date newValue) {
        checkWritePermission();
        date = toMilliseconds(newValue);
    }

    /**
     * Returns the event used for reference date.
     *
     * @return Event used for reference date, or {@code null}.
     */
    @Override
    @XmlElement(name = "dateType", required = true)
    public DateType getDateType() {
        return dateType;
    }

    /**
     * Sets the event used for reference date.
     *
     * @param newValue The new event.
     */
    public void setDateType(final DateType newValue) {
        checkWritePermission();
        dateType = newValue;
    }
}
