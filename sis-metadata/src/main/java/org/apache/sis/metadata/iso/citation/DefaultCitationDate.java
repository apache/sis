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


/**
 * Reference date and event used to describe it.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
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
    private static final long serialVersionUID = -2884791484254008454L;

    /**
     * Reference date for the cited resource in milliseconds elapsed sine January 1st, 1970,
     * or {@link Long#MIN_VALUE} if none.
     */
    private long date;

    /**
     * Event used for reference date.
     */
    private DateType dateType;

    /**
     * Constructs an initially empty citation date.
     */
    public DefaultCitationDate() {
        date = Long.MIN_VALUE;
    }

    /**
     * Constructs a citation date initialized to the given date.
     *
     * @param date     The reference date for the cited resource.
     * @param dateType The event used for reference date.
     */
    public DefaultCitationDate(final Date date, final DateType dateType) {
        this.date = (date != null) ? date.getTime() : Long.MIN_VALUE;
        this.dateType = dateType;
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
    public static DefaultCitationDate castOrCopy(final CitationDate object) {
        if (object == null || object instanceof DefaultCitationDate) {
            return (DefaultCitationDate) object;
        }
        final DefaultCitationDate copy = new DefaultCitationDate();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the reference date for the cited resource.
     */
    @Override
    @XmlElement(name = "date", required = true)
    public synchronized Date getDate() {
        return (date != Long.MIN_VALUE) ? new Date(date) : null;
    }

    /**
     * Sets the reference date for the cited resource.
     *
     * @param newValue The new date.
     */
    public synchronized void setDate(final Date newValue) {
        checkWritePermission();
        date = (newValue != null) ? newValue.getTime() : Long.MIN_VALUE;
    }

    /**
     * Returns the event used for reference date.
     */
    @Override
    @XmlElement(name = "dateType", required = true)
    public synchronized DateType getDateType() {
        return dateType;
    }

    /**
     * Sets the event used for reference date.
     *
     * @param newValue The new event.
     */
    public synchronized void setDateType(final DateType newValue) {
        checkWritePermission();
        dateType = newValue;
    }
}
