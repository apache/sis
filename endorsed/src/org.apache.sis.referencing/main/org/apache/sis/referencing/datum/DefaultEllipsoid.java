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
import java.util.Objects;
import static java.lang.Double.*;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Length;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.privy.DoubleDouble;
import org.apache.sis.xml.bind.gml.Measure;
import org.apache.sis.xml.bind.referencing.SecondDefiningParameter;
import org.apache.sis.metadata.privy.ImplementationHelper;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.measure.Units;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Geometric figure that can be used to describe the approximate shape of the earth.
 * In mathematical terms, it is a surface formed by the rotation of an ellipse about
 * its minor axis. An ellipsoid requires two defining parameters:
 *
 * <ul>
 *   <li>{@linkplain #getSemiMajorAxis() semi-major axis} and
 *       {@linkplain #getInverseFlattening() inverse flattening}, or</li>
 *   <li>{@linkplain #getSemiMajorAxis() semi-major axis} and
 *       {@linkplain #getSemiMinorAxis() semi-minor axis}.</li>
 * </ul>
 *
 * Some numerical values derived from the above properties are:
 *
 * <ul>
 *   <li>{@linkplain #getAuthalicRadius() authalic radius}</li>
 *   <li>{@linkplain #getEccentricity() eccentricity}</li>
 * </ul>
 *
 * <h2>Creating new ellipsoid instances</h2>
 * New instances can be created either directly by specifying all information to a factory method (choices 3
 * and 4 below), or indirectly by specifying the identifier of an entry in a database (choices 1 and 2 below).
 * In particular, the <a href="https://epsg.org/">EPSG repository</a> provides definitions for many ellipsoids,
 * and Apache SIS provides convenience shortcuts for some of them.
 *
 * <p>Choice 1 in the following list is the easiest but most restrictive way to get an ellipsoid.
 * The other choices provide more freedom. Each choice delegates its work to the subsequent items
 * (in the default configuration), so this list can be seen as <i>top to bottom</i> API.</p>
 *
 * <ol>
 *   <li>Create an {@code Ellipsoid} from one of the static convenience shortcuts listed in
 *       {@link org.apache.sis.referencing.CommonCRS#ellipsoid()}.</li>
 *   <li>Create an {@code Ellipsoid} from an identifier in a database by invoking
 *       {@link org.opengis.referencing.datum.DatumAuthorityFactory#createEllipsoid(String)}.</li>
 *   <li>Create an {@code Ellipsoid} by invoking the {@code DatumFactory.createEllipsoid(…)} or {@code createFlattenedSphere(…)}
 *       method (implemented for example by {@link org.apache.sis.referencing.factory.GeodeticObjectFactory}).</li>
 *   <li>Create a {@code DefaultEllipsoid} by invoking the
 *       {@link #createEllipsoid(Map, double, double, Unit) createEllipsoid(…)} or
 *       {@link #createFlattenedSphere(Map, double, double, Unit) createFlattenedSphere(…)}
 *       static methods defined in this class.</li>
 * </ol>
 *
 * <b>Example:</b> the following code gets the WGS84 ellipsoid:
 *
 * {@snippet lang="java" :
 *     Ellipsoid e = CommonCRS.WGS84.ellipsoid();
 *     }
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructors are also immutable. Unless otherwise noted in the javadoc, this condition holds if all
 * components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.referencing.CommonCRS#ellipsoid()
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createEllipsoid(String)
 *
 * @since 0.4
 */
@XmlType(name = "EllipsoidType", propOrder = {
    "semiMajorAxisMeasure",
    "secondDefiningParameter"
})
@XmlRootElement(name = "Ellipsoid")
public class DefaultEllipsoid extends AbstractIdentifiedObject implements Ellipsoid {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1149451543954764081L;

    /**
     * The equatorial radius. This field should be considered as final.
     * It is modified only by JAXB at unmarshalling time.
     *
     * @see #getSemiMajorAxis()
     */
    private double semiMajorAxis;

    /**
     * The polar radius. This field should be considered as final.
     * It is modified only by JAXB at unmarshalling time.
     *
     * @see #getSemiMinorAxis()
     */
    private double semiMinorAxis;

    /**
     * The inverse of the flattening value, or {@link Double#POSITIVE_INFINITY} if the ellipsoid is a sphere.
     * This field shall be considered as final. It is modified only by JAXB at unmarshalling time.
     *
     * @see #getInverseFlattening()
     */
    private double inverseFlattening;

    /**
     * Tells if the Inverse Flattening is definitive for this ellipsoid.
     * This field shall be considered as final. It is modified only by JAXB at unmarshalling time.
     *
     * @see #isIvfDefinitive()
     */
    private boolean ivfDefinitive;

    /**
     * The units of the semi-major and semi-minor axis values.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private Unit<Length> unit;

    /**
     * Creates a new ellipsoid using the specified axis length.
     * The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties         the properties to be given to the identified object.
     * @param  semiMajorAxis      the equatorial radius.
     * @param  semiMinorAxis      the polar radius.
     * @param  inverseFlattening  the inverse of the flattening value.
     * @param  ivfDefinitive      {@code true} if the inverse flattening is definitive.
     * @param  unit               the units of the semi-major and semi-minor axis values.
     *
     * @see #createEllipsoid(Map, double, double, Unit)
     * @see #createFlattenedSphere(Map, double, double, Unit)
     */
    protected DefaultEllipsoid(final Map<String,?> properties,
                               final double  semiMajorAxis,
                               final double  semiMinorAxis,
                               final double  inverseFlattening,
                               final boolean ivfDefinitive,
                               final Unit<Length> unit)
    {
        super(properties);
        ArgumentChecks.ensureNonNull("unit", unit);
        ArgumentChecks.ensureStrictlyPositive("semiMajorAxis", semiMajorAxis);
        ArgumentChecks.ensureStrictlyPositive("semiMinorAxis", semiMinorAxis);
        ArgumentChecks.ensureBetween("inverseFlattening", 1, Double.POSITIVE_INFINITY, inverseFlattening);
        this.unit              = unit;
        this.semiMajorAxis     = semiMajorAxis;
        this.semiMinorAxis     = semiMinorAxis;
        this.inverseFlattening = inverseFlattening;
        this.ivfDefinitive     = ivfDefinitive;
    }

    /**
     * Creates a new ellipsoid with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  ellipsoid  the ellipsoid to copy.
     *
     * @see #castOrCopy(Ellipsoid)
     */
    protected DefaultEllipsoid(final Ellipsoid ellipsoid) {
        super(ellipsoid);
        semiMajorAxis     = ellipsoid.getSemiMajorAxis();
        semiMinorAxis     = ellipsoid.getSemiMinorAxis();
        inverseFlattening = ellipsoid.getInverseFlattening();
        ivfDefinitive     = ellipsoid.isIvfDefinitive();
        unit              = ellipsoid.getAxisUnit();
    }

    /**
     * Creates a new ellipsoid using the specified properties and axis length.
     * The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     *
     * @param  properties     the properties to be given to the identified object.
     * @param  semiMajorAxis  the equatorial radius in the given unit.
     * @param  semiMinorAxis  the polar radius in the given unit.
     * @param  unit           the units of the semi-major and semi-minor axis values.
     * @return an ellipsoid with the given axis length.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createEllipsoid(Map, double, double, Unit)
     */
    public static DefaultEllipsoid createEllipsoid(final Map<String,?> properties,
                                                   final double semiMajorAxis,
                                                   final double semiMinorAxis,
                                                   final Unit<Length> unit)
    {
        if (semiMajorAxis == semiMinorAxis) {
            return new Sphere(properties, semiMajorAxis, false, unit);
        } else {
            return new DefaultEllipsoid(properties, semiMajorAxis, semiMinorAxis,
                       Formulas.getInverseFlattening(semiMajorAxis, semiMinorAxis), false, unit);
        }
    }

    /**
     * Creates a new ellipsoid using the specified properties, axis length and inverse flattening value.
     * The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     *
     * @param  properties         the properties to be given to the identified object.
     * @param  semiMajorAxis      the equatorial radius in the given unit.
     * @param  inverseFlattening  the inverse flattening value.
     * @param  unit               the units of the semi-major and semi-minor axis values.
     * @return an ellipsoid with the given axis length.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createFlattenedSphere(Map, double, double, Unit)
     */
    public static DefaultEllipsoid createFlattenedSphere(final Map<String,?> properties,
                                                         final double semiMajorAxis,
                                                         final double inverseFlattening,
                                                         final Unit<Length> unit)
    {
        if (isInfinite(inverseFlattening)) {
            return new Sphere(properties, semiMajorAxis, true, unit);
        } else {
            return new DefaultEllipsoid(properties, semiMajorAxis,
                    Formulas.getSemiMinor(semiMajorAxis, inverseFlattening),
                    inverseFlattening, true, unit);
        }
    }

    /**
     * Returns a SIS ellipsoid implementation with the same values as the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultEllipsoid castOrCopy(final Ellipsoid object) {
        if (object == null || object instanceof DefaultEllipsoid) {
            return (DefaultEllipsoid) object;
        }
        final Map<String,?> properties = IdentifiedObjects.getProperties(object);
        final double        semiMajor  = object.getSemiMajorAxis();
        final Unit<Length>  unit       = object.getAxisUnit();
        return object.isIvfDefinitive() ?
                createFlattenedSphere(properties, semiMajor, object.getInverseFlattening(), unit) :
                createEllipsoid      (properties, semiMajor, object.getSemiMinorAxis(),     unit);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code Ellipsoid.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code Ellipsoid}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with
     * their own set of interfaces.
     *
     * @return {@code Ellipsoid.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends Ellipsoid> getInterface() {
        return Ellipsoid.class;
    }

    /**
     * Returns the linear unit of the {@linkplain #getSemiMajorAxis() semi-major}
     * and {@linkplain #getSemiMinorAxis() semi-minor} axis values.
     *
     * @return the axis linear unit.
     *
     * @see #convertTo(Unit)
     */
    @Override
    public Unit<Length> getAxisUnit() {
        return unit;
    }

    /**
     * Length of the semi-major axis of the ellipsoid.
     * This is the equatorial radius in {@linkplain #getAxisUnit() axis linear unit}.
     *
     * @return length of semi-major axis.
     */
    @Override
    public double getSemiMajorAxis() {
        return semiMajorAxis;
    }

    /**
     * Length of the semi-minor axis of the ellipsoid. This is the
     * polar radius in {@linkplain #getAxisUnit() axis linear unit}.
     *
     * @return length of semi-minor axis.
     */
    @Override
    public double getSemiMinorAxis() {
        return semiMinorAxis;
    }

    /**
     * Returns the radius of a hypothetical sphere having the same surface as this ellipsoid.
     * The radius is expressed in {@linkplain #getAxisUnit() axis linear unit}.
     *
     * @return the radius of a sphere having the same surface as this ellipsoid.
     *
     * @see org.apache.sis.referencing.CommonCRS#SPHERE
     */
    public double getAuthalicRadius() {
        return Formulas.getAuthalicRadius(getSemiMajorAxis(), getSemiMinorAxis());
    }

    /**
     * Returns the geocentric radius at the given geodetic latitude.
     * Special cases:
     *
     * <ul>
     *   <li>If φ =   0°, then this is the same value as {@link #getSemiMajorAxis()}.</li>
     *   <li>If φ = ±90°, then this is the same value as {@link #getSemiMinorAxis()}.</li>
     *   <li>If φ is NaN, then this method returns NaN.</li>
     * </ul>
     *
     * @param  φ  geodetic latitude in degrees, from -90° to +90° inclusive.
     * @return geocentric radius at the geodetic latitude φ°.
     *
     * @see org.apache.sis.referencing.operation.transform.EllipsoidToRadiusTransform
     * @see <a href="https://en.wikipedia.org/wiki/Earth_radius#Geocentric_radius">Geocentric radius on Wikipedia</a>
     *
     * @since 1.4
     */
    public double getGeocentricRadius(final double φ) {
        return Formulas.geocentricRadius(this, Math.toRadians(φ));
    }

    /**
     * @deprecated Renamed {@link #getGeocentricRadius(double)}.
     *
     * @param  φ  latitude in degrees, from -90° to +90° inclusive.
     * @return geocentric radius at the given latitude.
     *
     * @since 1.3
     */
    @Deprecated(since="1.4", forRemoval=true)
    public double getRadius(final double φ) {
        return getGeocentricRadius(φ);
    }

    /**
     * The ratio of the distance between the center and a focus of the ellipse to the length of its semi-major axis.
     * The eccentricity can alternately be computed from the equation: ℯ = √(2f - f²) where <var>f</var> is the
     * flattening factor (not inverse).
     *
     * @return ℯ, the eccentricity of this ellipsoid.
     */
    public double getEccentricity() {
        return eccentricitySquared().sqrt().doubleValue();
    }

    /**
     * Returns the square of the {@link #getEccentricity() eccentricity} value.
     * This convenience method is provided because ℯ² is frequently used in coordinate operations,
     * actually more often than ℯ. This convenience method avoids the cost of computing the square
     * root when not needed.
     *
     * @return ℯ², the square of the eccentricity value.
     *
     * @since 0.7
     */
    public double getEccentricitySquared() {
        return eccentricitySquared().doubleValue();
    }

    /**
     * Computes the square of the eccentricity value with ℯ² = 2f - f².
     *
     * <h4>Implementation note</h4>
     * We use the flattening factor for this computation because the inverse flattening factor is usually the
     * second defining parameter.  But even if the second defining parameter of this ellipsoid was rather the
     * semi-minor axis, the fact that we use double-double arithmetic should give the same result anyway.
     */
    private DoubleDouble eccentricitySquared() {
        final DoubleDouble f = flattening(this);
        return f.scalb(1).subtract(f.square());
    }

    /**
     * Computes the flattening factor (not inverse) of the given ellipsoid.
     * This method chooses the formula depending on whether the defining parameter is the inverse flattening factor
     * or the semi-minor axis length. The defining parameters are presumed fully accurate in base 10 (even if this
     * is of course not possible in the reality), because those parameters are definitions given by authorities.
     *
     * <h4>Analogy</h4>
     * The conversion factor from inches to centimetres is 2.54 <em>by definition</em>. Even if we could find a more
     * accurate value matching historical measurements, the 2.54 value is the internationally agreed value for all
     * conversions. This value is (by convention) defined in base 10 and has no exact {@code double} representation.
     */
    private static DoubleDouble flattening(final Ellipsoid e) {
        final boolean decimal = true;       // Parameters are presumed accurate in base 10 (not 2) by definition.
        if (e.isIvfDefinitive()) {
            return DoubleDouble.ONE.divide(e.getInverseFlattening(), decimal);
        } else {
            var a = DoubleDouble.of(e.getSemiMajorAxis(), decimal);
            return a.subtract(e.getSemiMinorAxis(), decimal).divide(a);
        }
    }

    /**
     * Returns the value of the inverse of the flattening constant. Flattening is a value
     * used to indicate how closely an ellipsoid approaches a spherical shape. The inverse
     * flattening is related to the equatorial/polar radius by the formula:
     *
     * <blockquote>
     * <var>ivf</var> = <var>r</var><sub>e</sub> / (<var>r</var><sub>e</sub> - <var>r</var><sub>p</sub>).
     * </blockquote>
     *
     * For perfect spheres (i.e. if {@link #isSphere()} returns {@code true}),
     * the {@link Double#POSITIVE_INFINITY} value is used.
     *
     * @return the inverse flattening value.
     */
    @Override
    public double getInverseFlattening() {
        return inverseFlattening;
    }

    /**
     * Indicates if the {@linkplain #getInverseFlattening() inverse flattening} is definitive for
     * this ellipsoid. Some ellipsoids use the IVF as the defining value, and calculate the polar
     * radius whenever asked. Other ellipsoids use the polar radius to calculate the IVF whenever
     * asked. This distinction can be important to avoid floating-point rounding errors.
     *
     * @return {@code true} if the {@linkplain #getInverseFlattening inverse flattening} is definitive,
     *         or {@code false} if the {@linkplain #getSemiMinorAxis() polar radius} is definitive.
     */
    @Override
    public boolean isIvfDefinitive() {
        return ivfDefinitive;
    }

    /**
     * {@code true} if the ellipsoid is degenerate and is actually a sphere.
     * The sphere is completely defined by the {@linkplain #getSemiMajorAxis() semi-major axis},
     * which is the radius of the sphere.
     *
     * @return {@code true} if the ellipsoid is degenerate and is actually a sphere.
     */
    @Override
    public boolean isSphere() {
        return semiMajorAxis == semiMinorAxis;
    }

    /**
     * Returns the difference between the semi-major axis length of two ellipsoids.
     * If the two ellipsoid does not use the same unit of measurement, than the axis
     * length of the other ellipsoid is converted into the units of this ellipsoid axis.
     *
     * <h4>Example</h4>
     * {@code WGS84.semiMajorAxisDifference(ED50)} returns 251 metres. This information is a parameter of
     * {@linkplain org.apache.sis.referencing.operation.transform.MolodenskyTransform Molodensky transformations}.
     *
     * @param  other  the other ellipsoid from which to get semi-major axis length difference.
     * @return (<var>other</var> ellipsoid semi-major axis) - (<var>this</var> ellipsoid semi-major axis).
     *
     * @since 0.7
     */
    public double semiMajorAxisDifference(final Ellipsoid other) {
        double semiMajor = other.getSemiMajorAxis();
        semiMajor = other.getAxisUnit().getConverterTo(getAxisUnit()).convert(semiMajor);            // Often a no-op.
        // Presumed accurate in base 10 (not 2) by definition.
        final DoubleDouble a = DoubleDouble.of(semiMajor, true);
        return a.subtract(getSemiMajorAxis(), true).doubleValue();
    }

    /**
     * Returns the difference between the flattening factor of two ellipsoids.
     * This method returns 0 if the two ellipsoids are equal.
     *
     * <h4>Example</h4>
     * {@code WGS84.flatteningDifference(ED50)} returns approximately 1.41927E-05. This information is a parameter of
     * {@linkplain org.apache.sis.referencing.operation.transform.MolodenskyTransform Molodensky transformations}.
     *
     * @param  other  the other ellipsoid from which to get flattening difference.
     * @return (<var>other</var> ellipsoid flattening) - (<var>this</var> ellipsoid flattening).
     *
     * @since 0.7
     */
    public double flatteningDifference(final Ellipsoid other) {
        return flattening(other).subtract(flattening(this)).doubleValue();
    }

    /**
     * Returns the properties to use for the ellipsoid created by {@link #convertTo(Unit)}.
     *
     * @param  target  the desired unit of measurement.
     * @return properties of the derived ellipsoid to create.
     */
    final Map<String,?> properties(final Unit<Length> target) {
        return Map.of(NAME_KEY, '“' + getName().getCode() + "” converted to " + target,
                      DOMAINS_KEY, getDomains());
    }

    /**
     * Returns an ellipsoid of the same shape as this ellipsoid but using the specified unit of measurement.
     * If the given unit of measurement is equivalent to the unit used by this ellipsoid, then this method
     * returns {@code this}. Otherwise, a new ellipsoid with an arbitrary name is returned.
     *
     * @param  target  the desired unit of measurement.
     * @return ellipsoid of the same shape using the given unit of measurement.
     *
     * @see #getAxisUnit()
     * @since 1.5
     */
    public DefaultEllipsoid convertTo(final Unit<Length> target) {
        final UnitConverter c = unit.getConverterTo(target);
        if (c.isIdentity()) {
            return this;
        }
        return new DefaultEllipsoid(properties(target),
                c.convert(semiMajorAxis),
                c.convert(semiMinorAxis),
                inverseFlattening,
                ivfDefinitive,
                target);
    }

    /**
     * Compares this ellipsoid with the specified object for equality.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     *
     * @hidden because nothing new to said.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final var that = (DefaultEllipsoid) object;
                return ivfDefinitive == that.ivfDefinitive &&
                       Numerics.equals(this.semiMajorAxis,     that.semiMajorAxis)     &&
                       Numerics.equals(this.semiMinorAxis,     that.semiMinorAxis)     &&
                       Numerics.equals(this.inverseFlattening, that.inverseFlattening) &&
                        Objects.equals(this.unit,              that.unit);
            }
            case BY_CONTRACT: {
                /*
                 * isIvfDefinitive has no incidence on calculation using ellipsoid parameters,
                 * so we consider it as metadata that can be ignored in IGNORE_METADATA mode.
                 */
                if (isIvfDefinitive() != ((Ellipsoid) object).isIvfDefinitive()) {
                    return false;
                }
                // Fall through
            }
            case COMPATIBILITY:
            case IGNORE_METADATA: {
                /*
                 * "Inverse flattening factor" and "semi-minor axis length" are computed from each other,
                 * so we do not need to compare both of them. But in non-approximated mode we nevertheless
                 * compare both as a safety against rounding errors.
                 */
                if (!Numerics.equals(getInverseFlattening(), ((Ellipsoid) object).getInverseFlattening())) {
                    return false;
                }
                // Fall through
            }
            default: {
                /*
                 * Note: `DefaultPrimeMeridian.equals(object, IGNORE_METADATA)` ignores the unit.
                 * But we do not perform the same relaxation here because the ellipsoid unit will
                 * become the linear unit of map projections if the user does not overwrite them
                 * with an explicit CoordinateSystem declaration.
                 */
                final var that = (Ellipsoid) object;
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Unit<Length> unit = getAxisUnit();  // In case the user override this method.
                if (!Utilities.deepEquals(unit, that.getAxisUnit(), mode)) {
                    return false;
                }
                final UnitConverter c = mode.isApproximate() ? unit.getConverterTo(Units.METRE) : null;
                boolean isMinor = false;
                double v1 = this.getSemiMajorAxis();
                double v2 = that.getSemiMajorAxis();
                if (c == null ? Numerics.equals(v1, v2) : Numerics.epsilonEqual(
                        c.convert(v1), c.convert(v2), Formulas.LINEAR_TOLERANCE))
                {
                    isMinor = true;
                    v1 = this.getSemiMinorAxis();
                    v2 = that.getSemiMinorAxis();
                    if (c == null ? Numerics.equals(v1, v2) : Numerics.epsilonEqual(
                            c.convert(v1), c.convert(v2), Formulas.LINEAR_TOLERANCE))
                    {
                        return true;
                    }
                }
                assert (mode != ComparisonMode.DEBUG)
                        : Numerics.messageForDifference(isMinor ? "semiMinorAxis" : "semiMajorAxis", v1, v2);
                return false;
            }
        }
    }

    /**
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     * See {@link org.apache.sis.referencing.AbstractIdentifiedObject#computeHashCode()}
     * for more information.
     *
     * @return the hash code value. This value may change in any future Apache SIS version.
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Double.doubleToLongBits(semiMajorAxis) +
               31 * Double.doubleToLongBits(ivfDefinitive ? inverseFlattening : semiMinorAxis);
    }

    /**
     * Formats this ellipsoid as a <i>Well Known Text</i> {@code Ellipsoid[…]} element.
     *
     * @return {@code "Ellipsoid"} (WKT 2) or {@code "Spheroid"} (WKT 1).
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#52">WKT 2 specification §8.2.1</a>
     */
    @Override
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        final Convention   convention = formatter.getConvention();
        final boolean      isWKT1     = convention.majorVersion() == 1;
        final Unit<Length> unit       = getAxisUnit();  // Gives to users a chance to override properties.
        double length = getSemiMajorAxis();
        if (isWKT1) {
            length = unit.getConverterTo(Units.METRE).convert(length);
        }
        formatter.append(length);
        final double inverseFlattening = getInverseFlattening();  // Gives to users a chance to override properties.
        formatter.append(isInfinite(inverseFlattening) ? 0 : inverseFlattening);
        if (isWKT1) {
            return WKTKeywords.Spheroid;
        }
        if (!convention.isSimplified() || !Units.METRE.equals(unit)) {
            formatter.append(unit);
        }
        return WKTKeywords.Ellipsoid;
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    private DefaultEllipsoid() {
        super(org.apache.sis.referencing.privy.NilReferencingObject.INSTANCE);
        /*
         * We need to let the DefaultEllipsoid fields unitialized because afterUnmarshal(…)
         * will check for zero values. We cannot thrown an exception from 'afterUnmarshal'
         * because it would cause the whole unmarshalling to fail. But the CD_Ellipsoid
         * adapter does some verifications.
         */
    }

    /**
     * After the unmarshalling process, only one value between {@link #semiMinorAxis} and
     * {@link #inverseFlattening} has been defined. Since the {@link #semiMajorAxis} has
     * been defined, it is now possible to calculate the value of the missing parameter
     * using the values of those that are set.
     *
     * @see #setSemiMajorAxisMeasure(Measure)
     * @see #setSecondDefiningParameter(SecondDefiningParameter)
     */
    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        if (ivfDefinitive) {
            if (semiMinorAxis == 0) {
                semiMinorAxis = Formulas.getSemiMinor(semiMajorAxis, inverseFlattening);
            }
        } else {
            if (inverseFlattening == 0) {
                inverseFlattening = Formulas.getInverseFlattening(semiMajorAxis, semiMinorAxis);
            } else if (inverseFlattening == Double.POSITIVE_INFINITY && semiMinorAxis == 0) {
                semiMinorAxis = semiMajorAxis;
            }
        }
        if (unit == null) {
            unit = Units.METRE;
            Measure.missingUOM(DefaultEllipsoid.class, "semiMajorAxis");
        }
    }

    /**
     * Returns the semi-major axis value as a measurement.
     * This method is invoked by JAXB for XML marshalling.
     */
    @XmlElement(name = "semiMajorAxis", required = true)
    private Measure getSemiMajorAxisMeasure() {
        return new Measure(semiMajorAxis, unit);
    }

    /**
     * Sets the semi-major axis value.
     * This method is invoked by JAXB at unmarshalling time only.
     *
     * @see #setSecondDefiningParameter(SecondDefiningParameter)
     * @see #afterUnmarshal(Unmarshaller, Object)
     */
    private void setSemiMajorAxisMeasure(final Measure measure) {
        if (semiMajorAxis == 0) {
            final Unit<Length> uom = unit; // In case semi-minor were defined before semi-major.
            ArgumentChecks.ensureStrictlyPositive("semiMajorAxis", semiMajorAxis = measure.value);
            unit = measure.getUnit(Length.class);
            harmonizeAxisUnits(uom);
        } else {
            ImplementationHelper.propertyAlreadySet(DefaultEllipsoid.class, "setSemiMajorAxisMeasure", "semiMajorAxis");
        }
    }

    /**
     * Returns the object to be marshalled as the {@code SecondDefiningParameter} XML element. The
     * returned object contains the values for {@link #semiMinorAxis} or {@link #inverseFlattening},
     * according to the {@link #isIvfDefinitive()} value. This method is for JAXB marshalling only.
     */
    @XmlElement(name = "secondDefiningParameter", required = true)
    private SecondDefiningParameter getSecondDefiningParameter() {
        return new SecondDefiningParameter(this, true);
    }

    /**
     * Sets the second defining parameter value, either the inverse of the flattening
     * value or the semi minor axis value, according to what have been defined in the
     * second defining parameter given. This is for JAXB unmarshalling process only.
     *
     * @see #setSemiMajorAxisMeasure(Measure)
     * @see #afterUnmarshal(Unmarshaller, Object)
     */
    private void setSecondDefiningParameter(SecondDefiningParameter second) {
        if (second.secondDefiningParameter != null) {
            second = second.secondDefiningParameter;
        }
        boolean duplicate = false;
        if (Boolean.TRUE.equals(second.isSphere)) {
            duplicate = (inverseFlattening != 0);
            if (!duplicate) {
                inverseFlattening = Double.POSITIVE_INFINITY;
            }
        }
        final Measure measure = second.measure;
        if (measure != null) {
            final boolean isIvfDefinitive = second.isIvfDefinitive();
            duplicate |= (isIvfDefinitive ? inverseFlattening : semiMinorAxis) != 0;
            if (!duplicate) {
                ivfDefinitive = isIvfDefinitive;
                double value = measure.value;
                if (isIvfDefinitive) {
                    /*
                     * Interpreting an inverse flattening factor of 0 as synonymous of infinity
                     * is a Well-Known Text (WKT) convention, not part of GML standard. However
                     * in practice some software do that.
                     */
                    if (value == 0) {
                        value = Double.POSITIVE_INFINITY;
                    }
                    ArgumentChecks.ensureBetween("inverseFlattening", 1, Double.POSITIVE_INFINITY, inverseFlattening = value);
                } else {
                    ArgumentChecks.ensureStrictlyPositive("semiMinorAxis", semiMinorAxis = value);
                    harmonizeAxisUnits(measure.getUnit(Length.class));
                }
            }
        }
        if (duplicate) {
            ImplementationHelper.propertyAlreadySet(DefaultEllipsoid.class,
                    "setSecondDefiningParameter", "secondDefiningParameter");
        }
    }

    /**
     * Ensures that the semi-minor axis uses the same unit as the semi-major one.
     * The {@link #unit} field shall be set to the semi-major axis unit before this method call.
     *
     * @param  uom  the semi-minor axis unit.
     */
    private void harmonizeAxisUnits(final Unit<Length> uom) {
        if (unit == null) {
            unit = uom;
        } else if (uom != null && uom != unit) {
            semiMinorAxis = uom.getConverterTo(unit).convert(semiMinorAxis);
        }
    }
}
