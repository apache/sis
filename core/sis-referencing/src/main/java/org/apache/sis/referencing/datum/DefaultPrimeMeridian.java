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

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.datum.PrimeMeridian;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Immutable;

import static org.apache.sis.util.ArgumentChecks.ensureFinite;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Related to JDK7
import org.apache.sis.internal.jdk7.Objects;


/**
 * A prime meridian defines the origin from which longitude values are determined.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 */
@Immutable
@XmlType(name = "PrimeMeridianType")
@XmlRootElement(name = "PrimeMeridian")
public class DefaultPrimeMeridian extends AbstractIdentifiedObject implements PrimeMeridian {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 541978454643213305L;;

    /**
     * The Greenwich meridian (EPSG:8901), with angular measurements in decimal degrees.
     */
    public static final DefaultPrimeMeridian GREENWICH;
    static {
        final Map<String,Object> properties = new HashMap<String,Object>(4);
        properties.put(NAME_KEY, "Greenwich"); // Name is fixed by ISO 19111.
        properties.put(IDENTIFIERS_KEY, new NamedIdentifier(Citations.EPSG, "8901"));
        GREENWICH = new DefaultPrimeMeridian(properties, 0, NonSI.DEGREE_ANGLE);
    }

    /**
     * Longitude of the prime meridian measured from the Greenwich meridian, positive eastward.
     */
    @XmlElement(required = true)
    private final double greenwichLongitude;

    /**
     * The angular unit of the {@linkplain #getGreenwichLongitude() Greenwich longitude}.
     */
    private final Unit<Angle> angularUnit;

    /**
     * Constructs a new object in which every attributes are set to a default value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultPrimeMeridian() {
        this(GREENWICH);
    }

    /**
     * Constructs a new prime meridian with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param meridian The prime meridian to copy.
     */
    public DefaultPrimeMeridian(final PrimeMeridian meridian) {
        super(meridian);
        greenwichLongitude = meridian.getGreenwichLongitude();
        angularUnit        = meridian.getAngularUnit();
    }

    /**
     * Constructs a prime meridian from a name and Greenwich longitude.
     * The {@code greenwichLongitude} value is assumed in {@linkplain NonSI#DEGREE_ANGLE decimal degrees}.
     *
     * @param name The datum name.
     * @param greenwichLongitude The longitude value relative to the Greenwich Meridian, in degrees.
     */
    public DefaultPrimeMeridian(final String name, final double greenwichLongitude) {
        this(name, greenwichLongitude, NonSI.DEGREE_ANGLE);
    }

    /**
     * Constructs a prime meridian from a name, Greenwich longitude and angular unit.
     *
     * @param name                The datum name.
     * @param greenwichLongitude  The longitude value relative to the Greenwich Meridian.
     * @param angularUnit         The angular unit of the longitude, in degrees.
     */
    public DefaultPrimeMeridian(final String name, final double greenwichLongitude, final Unit<Angle> angularUnit) {
        this(Collections.singletonMap(NAME_KEY, name), greenwichLongitude, angularUnit);
    }

    /**
     * Constructs a prime meridian from a set of properties. The properties map is given
     * unchanged to the {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map)
     * super-class constructor}.
     *
     * @param properties          Set of properties. Should contains at least {@code "name"}.
     * @param greenwichLongitude  The longitude value relative to the Greenwich Meridian.
     * @param angularUnit         The angular unit of the longitude.
     */
    public DefaultPrimeMeridian(final Map<String,?> properties, final double greenwichLongitude,
                                final Unit<Angle> angularUnit)
    {
        super(properties);
        ensureFinite("greenwichLongitude", greenwichLongitude);
        ensureNonNull("angularUnit", angularUnit);
        this.greenwichLongitude = greenwichLongitude;
        this.angularUnit = angularUnit;
    }

    /**
     * Returns a SIS prime meridian implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultPrimeMeridian castOrCopy(final PrimeMeridian object) {
        return (object == null) || (object instanceof DefaultPrimeMeridian)
                ? (DefaultPrimeMeridian) object : new DefaultPrimeMeridian(object);
    }

    /**
     * Longitude of the prime meridian measured from the Greenwich meridian, positive eastward.
     *
     * @return The prime meridian Greenwich longitude, in {@linkplain #getAngularUnit() angular unit}.
     */
    @Override
    public double getGreenwichLongitude() {
        return greenwichLongitude;
    }

    /**
     * Returns the longitude value relative to the Greenwich Meridian, expressed in the specified units.
     * This convenience method makes it easier to obtain longitude in decimal degrees using the following
     * code, regardless of the underlying angular units of this prime meridian:
     *
     * {@preformat java
     *     double longitudeInDegrees = primeMeridian.getGreenwichLongitude(NonSI.DEGREE_ANGLE);
     * }
     *
     * @param targetUnit The unit in which to express longitude.
     * @return The Greenwich longitude in the given units.
     */
    public double getGreenwichLongitude(final Unit<Angle> targetUnit) {
        return getAngularUnit().getConverterTo(targetUnit).convert(getGreenwichLongitude());
    }

    /**
     * Returns the angular unit of the Greenwich longitude.
     *
     * @return The angular unit of the {@linkplain #getGreenwichLongitude() Greenwich longitude}.
     */
    @Override
    public Unit<Angle> getAngularUnit() {
        return angularUnit;
    }

    /**
     * Compares this prime meridian with the specified object for equality.
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
                    final DefaultPrimeMeridian that = (DefaultPrimeMeridian) object;
                    return Numerics.equals(this.greenwichLongitude, that.greenwichLongitude) &&
                            Objects.equals(this.angularUnit,        that.angularUnit);
                }
                case BY_CONTRACT: {
                    if (!(object instanceof PrimeMeridian)) break;
                    final PrimeMeridian that = (PrimeMeridian) object;
                    return Numerics.equals(getGreenwichLongitude(), that.getGreenwichLongitude()) &&
                            Objects.equals(getAngularUnit(),        that.getAngularUnit());
                }
                default: {
                    if (!(object instanceof PrimeMeridian)) break;
                    final DefaultPrimeMeridian that = castOrCopy((PrimeMeridian) object);
                    return Numerics.epsilonEqual(this.getGreenwichLongitude(NonSI.DEGREE_ANGLE),
                                                 that.getGreenwichLongitude(NonSI.DEGREE_ANGLE), mode);
                    /*
                     * Note: if mode==IGNORE_METADATA, we relax the unit check because EPSG uses
                     *       sexagesimal degrees for the Greenwich meridian. Requirying the same
                     *       unit prevent Geodetic.isWGS84(...) method to recognize EPSG's WGS84.
                     */
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
         * differentiate this PrimeMeridian implementation from implementations of other GeoAPI interfaces.
         */
        int code = super.hashCode(mode) ^ (int) serialVersionUID;
        switch (mode) {
            case STRICT: {
                code += Numerics.hash(greenwichLongitude, Objects.hashCode(angularUnit));
                break;
            }
            default: {
                code += Numerics.hash(getGreenwichLongitude(), Objects.hashCode(getAngularUnit()));
                break;
            }
        }
        return code;
    }

    /**
     * Formats the inner part of a<cite>Well Known Text</cite> (WKT) element.
     *
     * @param  formatter The formatter to use.
     * @return The WKT element name, which is {@code "PRIMEM"}.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public String formatTo(final Formatter formatter) {
        /*
         * If the PrimeMeridian is written inside a "GEOGCS", then OGC say that it must be
         * written in the unit of the enclosing geographic coordinate system. Otherwise,
         * default to decimal degrees. Note that ESRI and GDAL don't follow this rule.
         */
        Unit<Angle> context = formatter.getConvention().getForcedUnit(Angle.class);
        if (context == null) {
            context = formatter.getAngularUnit();
            if (context == null) {
                context = NonSI.DEGREE_ANGLE;
            }
        }
        formatter.append(getGreenwichLongitude(context));
        return "PRIMEM";
    }
}
