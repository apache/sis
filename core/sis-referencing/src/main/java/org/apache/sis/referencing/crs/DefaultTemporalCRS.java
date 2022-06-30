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
package org.apache.sis.referencing.crs;

import java.util.Map;
import java.util.Date;
import java.time.Instant;
import java.time.Duration;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.measure.quantity.Time;
import javax.measure.UnitConverter;
import javax.measure.Unit;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.datum.TemporalDatum;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.referencing.WKTKeywords;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.measure.Units;
import org.apache.sis.math.Fraction;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.internal.util.StandardDateFormat.NANOS_PER_SECOND;
import static org.apache.sis.internal.util.StandardDateFormat.MILLIS_PER_SECOND;


/**
 * A 1-dimensional coordinate reference system used for the recording of time.
 * The Apache SIS implementation provides the following methods in addition to the OGC/ISO properties:
 *
 * <ul>
 *   <li>{@link #toInstant(double)} for converting a temporal position to a {@link Date}.</li>
 *   <li>{@link #toValue(Instant)} for converting a {@link Instant} to a temporal position.</li>
 * </ul>
 *
 * <p><b>Used with datum type:</b>
 *   {@linkplain org.apache.sis.referencing.datum.DefaultTemporalDatum Temporal}.<br>
 * <b>Used with coordinate system type:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultTimeCS Time}.
 * </p>
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.3
 *
 * @see org.apache.sis.referencing.datum.DefaultTemporalDatum
 * @see org.apache.sis.referencing.cs.DefaultTimeCS
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createTemporalCRS(String)
 *
 * @since 0.4
 * @module
 */
@XmlType(name = "TemporalCRSType", propOrder = {
    "coordinateSystem",
    "datum"
})
@XmlRootElement(name = "TemporalCRS")
public class DefaultTemporalCRS extends AbstractCRS implements TemporalCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3000119849197222007L;

    /**
     * The datum.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setDatum(TemporalDatum)}</p>
     *
     * @see #getDatum()
     */
    private TemporalDatum datum;

    /**
     * Conversion factor from values in this CRS to values in seconds. We use {@link UnitConverter}
     * instead of {@code double} because the SIS implementation of {@code UnitConverter} performs
     * some extra steps against rounding errors.
     *
     * @see #initializeConverter()
     */
    private transient UnitConverter toSeconds;

    /**
     * The {@linkplain TemporalDatum#getOrigin origin} in seconds since January 1st, 1970.
     * This field could be implicit in the {@link #toSeconds} converter, but we still handle
     * it explicitly in order to use integer arithmetic.
     */
    private transient long origin;

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#SCOPE_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the coordinate reference system.
     * @param  datum       the datum.
     * @param  cs          the coordinate system.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createTemporalCRS(Map, TemporalDatum, TimeCS)
     */
    public DefaultTemporalCRS(final Map<String,?> properties,
                              final TemporalDatum datum,
                              final TimeCS        cs)
    {
        super(properties, cs);
        ensureNonNull("datum", datum);
        this.datum = datum;
        initializeConverter();
    }

    /**
     * Constructs a new coordinate reference system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  crs  the coordinate reference system to copy.
     *
     * @see #castOrCopy(TemporalCRS)
     */
    protected DefaultTemporalCRS(final TemporalCRS crs) {
        super(crs);
        datum = crs.getDatum();
        initializeConverter();
    }

    /**
     * Returns a SIS coordinate reference system implementation with the same values than the given
     * arbitrary implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultTemporalCRS castOrCopy(final TemporalCRS object) {
        return (object == null || object instanceof DefaultTemporalCRS)
                ? (DefaultTemporalCRS) object : new DefaultTemporalCRS(object);
    }

    /**
     * Invoked on deserialization for restoring the transient fields.
     *
     * @param  in  the input stream from which to deserialize an attribute.
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initializeConverter();
    }

    /**
     * Initialize the fields required for {@link #toInstant(double)} and {@link #toValue(Instant)} operations.
     */
    private void initializeConverter() {
        toSeconds = getUnit().getConverterTo(Units.SECOND);
        long t = datum.getOrigin().getTime();
        origin = t / MILLIS_PER_SECOND;
        t %= MILLIS_PER_SECOND;
        if (t != 0) {
            /*
             * The origin is usually an integer amount of days or hours. It rarely has a fractional amount of seconds.
             * If it happens anyway, put the fractional amount of seconds in the converter instead of adding another
             * field in this class for such very rare situation. Accuracy should be okay since the offset is small.
             */
            UnitConverter c = Units.converter(null, new Fraction((int) t, MILLIS_PER_SECOND).simplify());
            toSeconds = c.concatenate(toSeconds);
        }
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code TemporalCRS.class}.
     *
     * <div class="note"><b>Note for implementers:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code TemporalCRS}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.</div>
     *
     * @return {@code TemporalCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends TemporalCRS> getInterface() {
        return TemporalCRS.class;
    }

    /**
     * Returns the datum.
     *
     * @return the datum.
     */
    @Override
    @XmlElement(name = "temporalDatum", required = true)
    public TemporalDatum getDatum() {
        return datum;
    }

    /**
     * Returns the coordinate system.
     *
     * @return the coordinate system.
     */
    @Override
    @XmlElement(name = "timeCS", required = true)
    public TimeCS getCoordinateSystem() {
        return (TimeCS) super.getCoordinateSystem();
    }

    /**
     * Returns the unit of measurement of temporal measurement in the coordinate reference system.
     * This is a convenience method for {@link org.opengis.referencing.cs.CoordinateSystemAxis#getUnit()}
     * on the unique axis of this coordinate reference system. The unit of measurement returned by this method
     * is the unit of the value expected in argument by {@link #toInstant(double)} and {@link #toDate(double)},
     * and the unit of the value returned by {@code toValue(…)} methods.
     *
     * <div class="note"><b>Implementation note:</b>
     * this method is declared final and does not invoke overridden {@link #getCoordinateSystem()} method
     * because this {@code getUnit()} method is invoked indirectly by constructors. Another reason is that
     * the overriding point is the {@code CoordinateSystemAxis.getUnit()} method and we want to avoid
     * introducing another overriding point that could be inconsistent with above method.</div>
     *
     * @return the temporal unit of measurement of coordinates in this CRS.
     *
     * @since 1.0
     */
    public final Unit<Time> getUnit() {
        return super.getCoordinateSystem().getAxis(0).getUnit().asType(Time.class);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public DefaultTemporalCRS forConvention(final AxesConvention convention) {
        return (DefaultTemporalCRS) super.forConvention(convention);
    }

    /**
     * Returns a coordinate reference system of the same type than this CRS but with different axes.
     */
    @Override
    final AbstractCRS createSameType(final Map<String,?> properties, final CoordinateSystem cs) {
        return new DefaultTemporalCRS(properties, datum, (TimeCS) cs);
    }

    /**
     * Converts the given value into an instant object.
     * If the given value {@linkplain Double#isNaN is NaN} or infinite, then this method returns {@code null}.
     * This method is the converse of {@link #toValue(Instant)}.
     *
     * @param  value  a value in this axis. Unit of measurement is given by {@link #getUnit()}.
     * @return the value as an instant, or {@code null} if the given value is NaN or infinite.
     *
     * @since 1.0
     */
    public Instant toInstant(double value) {
        if (Double.isFinite(value)) {
            value = toSeconds.convert(value);
            final long t = Math.round(value);                           // In seconds.
            return Instant.ofEpochSecond(Math.addExact(t, origin),      // Number of seconds since epoch.
                    Math.round((value - t) * NANOS_PER_SECOND));        // Nanoseconds adjustment.
        } else {
            return null;
        }
    }

    /**
     * Converts the given value into a {@link Date} object.
     * If the given value is {@linkplain Double#isNaN is NaN} or infinite, then this method returns {@code null}.
     * This method is the converse of {@link #toValue(Date)}.
     *
     * <p>This method is provided for interoperability with legacy {@code java.util.Date} object.
     * New code should use {@link #toInstant(double)} instead.</p>
     *
     * @param  value  a value in this axis unit. Unit of measurement is given by {@link #getUnit()}.
     * @return the value as a {@linkplain Date date}, or {@code null} if the given value is NaN or infinite.
     */
    public Date toDate(double value) {
        if (Double.isFinite(value)) {
            value = toSeconds.convert(value);
            final long t = Math.round(value);                                       // In seconds.
            long ms = Math.addExact(t, origin);                                     // Number of seconds since epoch.
            ms = Math.multiplyExact(ms, MILLIS_PER_SECOND);                         // Number of milliseconds since epoch.
            ms = Math.addExact(Math.round((value - t) * MILLIS_PER_SECOND), ms);    // Milliseconds adjustment.
            return new Date(ms);
        } else {
            return null;
        }
    }

    /**
     * Converts the given value difference into a duration object.
     * If the given value {@linkplain Double#isNaN is NaN} or infinite,
     * or if the conversion is non-linear, then this method returns {@code null}.
     * This method is the converse of {@link #toValue(Duration)}.
     *
     * @param  delta  a difference of values in this axis. Unit of measurement is given by {@link #getUnit()}.
     * @return the value difference as a duration, or {@code null} if the duration can not be computed.
     *
     * @since 1.3
     */
    public Duration toDuration(double delta) {
        delta *= Units.derivative(toSeconds, Double.NaN);
        if (Double.isFinite(delta)) {
            final long t = Math.round(delta);
            return Duration.ofSeconds(t, Math.round((delta - t) * NANOS_PER_SECOND));
        } else {
            return null;
        }
    }

    /**
     * Converts the given instant into a value in this axis unit.
     * If the given instant is {@code null}, then this method returns {@link Double#NaN}.
     * This method is the converse of {@link #toInstant(double)}.
     *
     * @param  time  the value as an instant, or {@code null}.
     * @return the value in this axis unit, or {@link Double#NaN} if the given instant is {@code null}.
     *         Unit of measurement is given by {@link #getUnit()}.
     *
     * @since 1.0
     */
    public double toValue(final Instant time) {
        if (time != null) {
            double t = Math.subtractExact(time.getEpochSecond(), origin);
            t += time.getNano() / (double) NANOS_PER_SECOND;
            return toSeconds.inverse().convert(t);
        } else {
            return Double.NaN;
        }
    }

    /**
     * Converts the given {@linkplain Date date} into a value in this axis unit.
     * If the given time is {@code null}, then this method returns {@link Double#NaN}.
     * This method is the converse of {@link #toDate(double)}.
     *
     * <p>This method is provided for interoperability with legacy {@code java.util.Date} object.
     * New code should use {@link #toValue(Instant)} instead.</p>
     *
     * @param  time  the value as a {@linkplain Date date}, or {@code null}.
     * @return the value in this axis unit, or {@link Double#NaN} if the given time is {@code null}.
     *         Unit of measurement is given by {@link #getUnit()}.
     */
    public double toValue(final Date time) {
        if (time != null) {
            long ms = time.getTime();                       // Number of milliseconds since epoch.
            long t = ms / MILLIS_PER_SECOND;                // Number of seconds since epoch.
            ms %= MILLIS_PER_SECOND;                        // Milliseconds adjustment.
            t = Math.subtractExact(t, origin);              // Time in seconds.
            return toSeconds.inverse().convert(t + ms / (double) MILLIS_PER_SECOND);
        } else {
            return Double.NaN;
        }
    }

    /**
     * Converts the given duration into a difference of values in this axis unit.
     * If the given duration is {@code null}, or if the conversion is non-linear,
     * then this method returns {@link Double#NaN}.
     * This method is the converse of {@link #toDuration(double)}.
     *
     * @param  delta  the difference of values as a duration, or {@code null}.
     * @return the value difference in this axis unit, or {@link Double#NaN} if it can not be computed.
     *         Unit of measurement is given by {@link #getUnit()}.
     *
     * @since 1.3
     */
    public double toValue(final Duration delta) {
        if (delta != null) {
            double t = delta.getSeconds();
            t += delta.getNano() / (double) NANOS_PER_SECOND;
            t *= Units.derivative(toSeconds.inverse(), Double.NaN);
            return t;
        } else {
            return Double.NaN;
        }
    }

    /**
     * Formats this CRS as a <cite>Well Known Text</cite> {@code TimeCRS[…]} element.
     *
     * <div class="note"><b>Compatibility note:</b>
     * {@code TimeCRS} is defined in the WKT 2 specification only.</div>
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return {@code "TimeCRS"}.
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#88">WKT 2 specification §14</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        if (formatter.getConvention().majorVersion() == 1) {
            formatter.setInvalidWKT(this, null);
        }
        return isBaseCRS(formatter) ? WKTKeywords.BaseTimeCRS : WKTKeywords.TimeCRS;
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultTemporalCRS() {
        /*
         * The datum and the coordinate system are mandatory for SIS working. We do not verify their presence
         * here because the verification would have to be done in an 'afterMarshal(…)' method and throwing an
         * exception in that method causes the whole unmarshalling to fail.  But the SC_CRS adapter does some
         * verifications.
         */
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     *
     * @see #getDatum()
     */
    private void setDatum(final TemporalDatum value) {
        if (datum == null) {
            datum = value;
            if (super.getCoordinateSystem() != null) {
                initializeConverter();
            }
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultVerticalCRS.class, "setDatum", "temporalDatum");
        }
    }

    /**
     * Used by JAXB only (invoked by reflection).
     *
     * @see #getCoordinateSystem()
     */
    private void setCoordinateSystem(final TimeCS cs) {
        setCoordinateSystem("timeCS", cs);
        if (toSeconds == null && datum != null) {
            initializeConverter();
        }
    }
}
