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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.measure.quantity.Duration;
import javax.measure.converter.UnitConverter;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.datum.TemporalDatum;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.measure.Units;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A 1D coordinate reference system used for the recording of time.
 * The Apache SIS implementation provides the following methods in addition to the OGC/ISO properties:
 *
 * <ul>
 *   <li>{@link #toDate(double)} for converting a temporal position to a {@link Date}.</li>
 *   <li>{@link #toValue(Date)} for converting a {@link Date} to a temporal position.</li>
 * </ul>
 *
 * <p><b>Used with coordinate system type:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultTimeCS Time}.
 * </p>
 *
 * {@section Immutability and thread safety}
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.referencing.datum.DefaultTemporalDatum
 * @see org.apache.sis.referencing.cs.DefaultTimeCS
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
     */
    @XmlElement(name = "temporalDatum")
    private final TemporalDatum datum;

    /**
     * A converter from values in this CRS to values in milliseconds.
     * Will be constructed only when first needed.
     */
    private transient UnitConverter toMillis;

    /**
     * The {@linkplain TemporalDatum#getOrigin origin} in milliseconds since January 1st, 1970.
     * This field could be implicit in the {@link #toMillis} converter, but we still handle it
     * explicitly in order to use integer arithmetic.
     */
    private transient long origin;

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultTemporalCRS() {
        datum = null;
    }

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#SCOPE_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * @param properties The properties to be given to the coordinate reference system.
     * @param datum The datum.
     * @param cs The coordinate system.
     */
    public DefaultTemporalCRS(final Map<String,?> properties,
                              final TemporalDatum datum,
                              final TimeCS        cs)
    {
        super(properties, cs);
        ensureNonNull("datum", datum);
        this.datum = datum;
    }

    /**
     * Constructs a new coordinate reference system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param crs The coordinate reference system to copy.
     *
     * @see #castOrCopy(TemporalCRS)
     */
    protected DefaultTemporalCRS(final TemporalCRS crs) {
        super(crs);
        datum = crs.getDatum();
    }

    /**
     * Returns a SIS coordinate reference system implementation with the same values than the given
     * arbitrary implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultTemporalCRS castOrCopy(final TemporalCRS object) {
        return (object == null || object instanceof DefaultTemporalCRS)
                ? (DefaultTemporalCRS) object : new DefaultTemporalCRS(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code TemporalCRS.class}.
     *
     * {@note Subclasses usually do not need to override this method since GeoAPI does not define
     *        <code>TemporalCRS</code> sub-interface. Overriding possibility is left mostly for
     *        implementors who wish to extend GeoAPI with their own set of interfaces.}
     *
     * @return {@code TemporalCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends TemporalCRS> getInterface() {
        return TemporalCRS.class;
    }

    /**
     * Initialize the fields required for {@link #toDate} and {@link #toValue} operations.
     */
    private void initializeConverter() {
        origin   = datum.getOrigin().getTime();
        toMillis = getCoordinateSystem().getAxis(0).getUnit().asType(Duration.class).getConverterTo(Units.MILLISECOND);
    }

    /**
     * Returns the datum.
     *
     * @return The datum.
     */
    @Override
    public final TemporalDatum getDatum() {
        return datum;
    }

    /**
     * Returns the coordinate system.
     *
     * @return The coordinate system.
     */
    @Override
    @XmlElement(name = "timeCS")
    public TimeCS getCoordinateSystem() {
        return (TimeCS) super.getCoordinateSystem();
    }

    /**
     * Used by JAXB only (invoked by reflection).
     */
    private void setCoordinateSystem(final TimeCS cs) {
        super.setCoordinateSystem("timeCS", cs);
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
     * Convert the given value into a {@link Date} object.
     * If the given value is {@link Double#NaN NaN} or infinite, then this method returns {@code null}.
     *
     * <p>This method is the converse of {@link #toValue(Date)}.</p>
     *
     * @param  value A value in this axis unit.
     * @return The value as a {@linkplain Date date}, or {@code null} if the given value is NaN or infinite.
     */
    public Date toDate(final double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        if (toMillis == null) {
            initializeConverter();
        }
        return new Date(Math.round(toMillis.convert(value)) + origin);
    }

    /**
     * Convert the given {@linkplain Date date} into a value in this axis unit.
     * If the given time is {@code null}, then this method returns {@link Double#NaN NaN}.
     *
     * <p>This method is the converse of {@link #toDate(double)}.</p>
     *
     * @param  time The value as a {@linkplain Date date}, or {@code null}.
     * @return value A value in this axis unit, or {@link Double#NaN NaN} if the given time is {@code null}.
     */
    public double toValue(final Date time) {
        if (time == null) {
            return Double.NaN;
        }
        if (toMillis == null) {
            initializeConverter();
        }
        return toMillis.inverse().convert(time.getTime() - origin);
    }

    /**
     * Formats this CRS as a <cite>Well Known Text</cite> {@code TimeCRS[…]} element.
     *
     * {@note <code>TimeCRS</code> is defined in the WKT 2 specification only.}
     *
     * @return {@code "TimeCRS"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        /*
         * Note: super.formatTo(formatter) will usually format a DefaultTemporalDatum instance,
         * which will declare this WKT has invalid if the formatter convention is a WKT 1 one.
         * So we do not redo this check here.
         */
        super.formatTo(formatter);
        return "TimeCRS";
    }
}
