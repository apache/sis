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
package org.apache.sis.referencing.datum;

import java.util.Date;
import java.util.Map;
import java.util.Collections;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.datum.TemporalDatum;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Immutable;

import static org.apache.sis.internal.util.Numerics.hash;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Related to JDK7
import java.util.Objects;


/**
 * A temporal datum defines the origin of a temporal coordinate reference system.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 */
@Immutable
@XmlType(name = "TemporalDatumType")
@XmlRootElement(name = "TemporalDatum")
public class DefaultTemporalDatum extends AbstractDatum implements TemporalDatum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3357241732140076884L;

    /**
     * Datum for time measured since January 1st, 4713 BC at 12:00 UTC.
     *
     * @see org.apache.sis.referencing.crs.DefaultTemporalCRS#JULIAN
     */
    public static final DefaultTemporalDatum JULIAN = new DefaultTemporalDatum(
            name(Vocabulary.Keys.Julian), new Date(-2440588 * (24*60*60*1000L) + (12*60*60*1000L)));

    /**
     * Datum for time measured since November 17, 1858 at 00:00 UTC.
     * A <cite>Modified Julian day</cite> (MJD) is defined relative to
     * <cite>Julian day</cite> (JD) as {@code MJD = JD − 2400000.5}.
     *
     * @see org.apache.sis.referencing.crs.DefaultTemporalCRS#MODIFIED_JULIAN
     */
    public static final DefaultTemporalDatum MODIFIED_JULIAN = new DefaultTemporalDatum(
            name(Vocabulary.Keys.ModifiedJulian), new Date(-40587 * (24*60*60*1000L)));

    /**
     * Datum for time measured since May 24, 1968 at 00:00 UTC.
     * This epoch was introduced by NASA for the space program.
     * A <cite>Truncated Julian day</cite> (TJD) is defined relative to
     * <cite>Julian day</cite> (JD) as {@code TJD = JD − 2440000.5}.
     *
     * @see org.apache.sis.referencing.crs.DefaultTemporalCRS#TRUNCATED_JULIAN
     */
    public static final DefaultTemporalDatum TRUNCATED_JULIAN = new DefaultTemporalDatum(
            name(Vocabulary.Keys.TruncatedJulian), new Date(-587 * (24*60*60*1000L)));

    /**
     * Datum for time measured since December 31, 1899 at 12:00 UTC.
     * A <cite>Dublin Julian day</cite> (DJD) is defined relative to
     * <cite>Julian day</cite> (JD) as {@code DJD = JD − 2415020}.
     *
     * @see org.apache.sis.referencing.crs.DefaultTemporalCRS#DUBLIN_JULIAN
     */
    public static final DefaultTemporalDatum DUBLIN_JULIAN = new DefaultTemporalDatum(
            name(Vocabulary.Keys.DublinJulian), new Date(-25568 * (24*60*60*1000L) + (12*60*60*1000L)));

    /**
     * Default datum for time measured since January 1st, 1970 at 00:00 UTC.
     *
     * @see org.apache.sis.referencing.crs.DefaultTemporalCRS#UNIX
     * @see org.apache.sis.referencing.crs.DefaultTemporalCRS#JAVA
     */
    public static final DefaultTemporalDatum UNIX = new DefaultTemporalDatum("UNIX", new Date(0));

    /**
     * The date and time origin of this temporal datum.
     */
    private final long origin;

    /**
     * Constructs a new datum with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param datum The datum to copy.
     */
    public DefaultTemporalDatum(final TemporalDatum datum) {
        super(datum);
        origin = datum.getOrigin().getTime();
    }

    /**
     * Constructs a temporal datum from a name.
     *
     * @param name   The datum name.
     * @param origin The date and time origin of this temporal datum.
     */
    public DefaultTemporalDatum(final String name, final Date origin) {
        this(Collections.singletonMap(NAME_KEY, name), origin);
    }

    /**
     * Constructs a temporal datum from a set of properties. The properties map is given
     * unchanged to the {@linkplain AbstractDatum#AbstractDatum(Map) super-class constructor}.
     *
     * @param properties Set of properties. Should contains at least {@code "name"}.
     * @param origin The date and time origin of this temporal datum.
     */
    public DefaultTemporalDatum(final Map<String,?> properties, final Date origin) {
        super(properties);
        ensureNonNull("origin", origin);
        this.origin = origin.getTime();
    }

    /**
     * Returns a SIS datum implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultTemporalDatum castOrCopy(final TemporalDatum object) {
        return (object == null) || (object instanceof DefaultTemporalDatum) ?
                (DefaultTemporalDatum) object : new DefaultTemporalDatum(object);
    }

    /**
     * Returns the date and time origin of this temporal datum.
     *
     * @return The date and time origin of this temporal datum.
     */
    @Override
    public Date getOrigin() {
        return new Date(origin);
    }

    /**
     * Compares this temporal datum with the specified object for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    final DefaultTemporalDatum that = (DefaultTemporalDatum) object;
                    return this.origin == that.origin;
                }
                default: {
                    if (!(object instanceof TemporalDatum)) break;
                    final TemporalDatum that = (TemporalDatum) object;
                    return Objects.equals(getOrigin(), that.getOrigin());
                }
            }
        }
        return false;
    }

    /**
     * Computes a hash value consistent with the given comparison mode.
     */
    @Override
    public int hashCode(final ComparisonMode mode) throws IllegalArgumentException {
        /*
         * The "^ (int) serialVersionUID" is an arbitrary change applied to the hash code value in order to
         * differentiate this TemporalDatum implementation from implementations of other GeoAPI interfaces.
         */
        int code = super.hashCode(mode) ^ (int) serialVersionUID;
        switch (mode) {
            case STRICT: {
                code = hash(origin, code);
                break;
            }
            default: {
                code += Objects.hashCode(getOrigin());
                break;
            }
        }
        return code;
    }
}
