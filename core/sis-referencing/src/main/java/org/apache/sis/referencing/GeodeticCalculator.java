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
package org.apache.sis.referencing;

import java.awt.Shape;
import java.util.Locale;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.measure.Unit;
import javax.measure.quantity.Length;

import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.geometry.coordinate.Position;
import org.opengis.geometry.DirectPosition;

import org.apache.sis.io.TableAppender;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.Latitude;
import org.apache.sis.geometry.CoordinateFormat;
import org.apache.sis.internal.referencing.PositionTransformer;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.j2d.ShapeUtilities;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;

import static java.lang.Math.*;


/**
 * Performs geodetic calculations on a sphere or an ellipsoid. This class computes the distance between two points,
 * or conversely the point located at a given distance from another point when navigating in a given direction.
 * The distance depends on the path (or track) on Earth surface connecting the two points.
 * The track can be great circles (shortest path between two points) or rhumb lines (path with constant heading).
 *
 * <p>This class uses the following information:</p>
 * <ul>
 *   <li>The {@linkplain #setStartPoint(Position) start point}, which is always considered valid after the first call
 *     to {@code setStartPoint(…)}. Its value can only be changed by another call to {@code setStartPoint(…)}.</li>
 *   <li>One of the followings (the latest specified properties override other properties and determines what will be calculated):
 *     <ul>
 *       <li>the {@linkplain #setEndPoint(Position) end point}, or</li>
 *       <li>the {@linkplain #setStartingAzimuth(double) azimuth at start point} together with
 *           the {@linkplain #setGeodesicDistance(double) geodesic distance} from that point.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * This class is not thread-safe. If geodetic calculations are needed in a multi-threads environment,
 * then a distinct instance of {@code GeodeticCalculator} needs to be created for each thread.
 *
 * @version 1.0
 *
 * @see <a href="https://en.wikipedia.org/wiki/Great-circle_navigation">Great-circle navigation on Wikipedia</a>
 *
 * @since 1.0
 * @module
 */
public class GeodeticCalculator {
    /**
     * The transform from user coordinates to geodetic coordinates used in computation.
     * This object also holds the following information:
     *
     * <ul>
     *   <li>{@link PositionTransformer#defaultCRS} is the default CRS for all methods receiving a
     *       {@link Position} argument if the given position does not specify its own CRS.</li>
     *   <li>{@link PositionTransformer#getCoordinateReferenceSystem()} is the CRS of all methods
     *       receiving (φ,λ) arguments as {@code double} values.</li>
     * </ul>
     */
    private final PositionTransformer userToGeodetic;

    /**
     * The ellipsoid on which geodetic computations are performed.
     * This ellipsoid is inferred from the coordinate reference system specified at construction time.
     */
    final Ellipsoid ellipsoid;

    /**
     * The radius of a hypothetical sphere having the same surface than the {@linkplain #ellipsoid}.
     * Used for the approximation using spherical formulas.
     * Subclasses using ellipsoidal formulas will ignore this field.
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius()
     */
    private final double radius;

    /**
     * The (<var>latitude</var>, <var>longitude</var>) coordinates of the start point <strong>in radians</strong>.
     * This point is set by {@link #setStartPoint(double, double)}.
     *
     * @see #START_POINT
     */
    private double φ1, λ1;

    /**
     * The (<var>latitude</var>, <var>longitude</var>) coordinates of the end point <strong>in radians</strong>.
     * This point is set by {@link #setEndPoint(double, double)}.
     *
     * @see #END_POINT
     */
    private double φ2, λ2;

    /**
     * The azimuth at start point and end point, in radians between -π and +π.
     * 0° point toward North and values are increasing clockwise.
     *
     * @see #STARTING_AZIMUTH
     * @see #ENDING_AZIMUTH
     */
    private double α1, α2;

    /**
     * The distance from the starting point ({@link #φ1},{@link #λ1}) to the end point ({@link #φ2},{@link #λ2}).
     * The distance is in the same units than ellipsoid axes and the azimuth is in radians.
     *
     * @see #GEODESIC_DISTANCE
     * @see #getDistanceUnit()
     */
    private double geodesicDistance;

    /**
     * A bitmask specifying which information are valid. For example if the {@link #END_POINT} bit is not set,
     * then {@link #φ2} and {@link #λ2} need to be computed, which implies the computation of {@link #α1} and
     * {@link #α2} as well. If the {@link #GEODESIC_DISTANCE} bit is not set, then {@link #geodesicDistance}
     * needs to be computed, which implies recomputation of {@link #α1} and {@link #α2} as well.
     */
    private int validity;

    /**
     * Bitmask specifying which information are valid.
     */
    private static final int START_POINT = 1, END_POINT = 2, STARTING_AZIMUTH = 4, ENDING_AZIMUTH = 8, GEODESIC_DISTANCE = 16;

    /**
     * Constructs a new geodetic calculator expecting coordinates in the supplied CRS.
     * The geodetic formulas implemented by this {@code GeodeticCalculator} base class assume a spherical model.
     * This constructor is for subclasses computing geodesy on an ellipsoid or other figure of the Earth.
     * Users should invoke {@link #create(CoordinateReferenceSystem)} instead, which will choose a subtype
     * based on the given coordinate reference system.
     *
     * <p>This class is currently not designed for sub-classing outside this package. If in a future version we want to
     * relax this restriction, we should revisit the package-private API in order to commit to a safer protected API.</p>
     *
     * @param  crs  the reference system for the {@link Position} arguments and return values.
     */
    GeodeticCalculator(final CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureNonNull("crs", crs);
        final GeographicCRS geographic = ReferencingUtilities.toNormalizedGeographicCRS(crs, true, true);
        if (geographic == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalCRSType_1, ReferencingUtilities.getInterface(crs)));
        }
        ellipsoid      = ReferencingUtilities.getEllipsoid(crs);
        radius         = Formulas.getAuthalicRadius(ellipsoid);
        userToGeodetic = new PositionTransformer(crs, geographic, null);
    }

    /**
     * Constructs a new geodetic calculator expecting coordinates in the supplied CRS.
     * All {@code GeodeticCalculator} methods having a {@link Position} argument
     * or return value will use that specified CRS.
     * That CRS is the value returned by {@link #getPositionCRS()}.
     *
     * @param  crs  the reference system for the {@link Position} objects.
     * @return a new geodetic calculator using the specified CRS.
     */
    public static GeodeticCalculator create(final CoordinateReferenceSystem crs) {
        return new GeodeticCalculator(crs);
    }

    /**
     * Returns the Coordinate Reference System (CRS) in which {@code Position}s are represented, unless otherwise specified.
     * This is the CRS of all {@link Position} instances returned by methods in this class. This is also the default CRS
     * assumed by methods receiving a {@link Position} argument when the given position does not specify its CRS.
     * This default CRS is specified at construction time.
     * It is not necessarily geographic; it may be projected or geocentric.
     *
     * @return the default CRS for {@link Position} instances.
     */
    public CoordinateReferenceSystem getPositionCRS() {
        return userToGeodetic.getCoordinateReferenceSystem();
    }

    /**
     * Returns the coordinate reference system for all methods expecting (φ,λ) as {@code double} values.
     * This CRS always has (<var>latitude</var>, <var>longitude</var>) axes, in that order and in degrees.
     * The CRS may contain an additional axis for ellipsoidal height.
     *
     * @return the coordinate reference system of (φ,λ) coordinates.
     */
    public GeographicCRS getGeographicCRS() {
        return (GeographicCRS) userToGeodetic.defaultCRS;
    }

    /**
     * Sets the starting point as geographic (<var>latitude</var>, <var>longitude</var>) coordinates.
     * The {@linkplain #getStartingAzimuth() starting} and {@linkplain #getEndingAzimuth() ending azimuths},
     * the {@linkplain #getGeodesicDistance() geodesic distance} and the {@linkplain #getEndPoint() end point}
     * are discarded by this method call; some of them will need to be specified again.
     *
     * @param  latitude   the latitude in degrees between {@value Latitude#MIN_VALUE}° and {@value Latitude#MAX_VALUE}°.
     * @param  longitude  the longitude in degrees.
     * @throws IllegalArgumentException if the latitude is out of bounds.
     *
     * @see #setEndPoint(double, double)
     */
    public void setStartPoint(final double latitude, final double longitude) {
        ArgumentChecks.ensureBetween("latitude", Latitude.MIN_VALUE, Latitude.MAX_VALUE, latitude);
        ArgumentChecks.ensureFinite("longitude", longitude);
        φ1 = toRadians(latitude);
        λ1 = toRadians(longitude);
        validity = START_POINT;
    }

    /**
     * Sets the starting point as coordinates in arbitrary reference system. This method transforms the given
     * coordinates to geographic coordinates, then delegates to {@link #setStartPoint(double, double)}.
     * If the given point is not associated to a Coordinate Reference System (CRS), then this method assumes
     * the CRS specified at construction time.
     *
     * @param  point  the starting point in any coordinate reference system.
     * @throws TransformException if the coordinates can not be transformed.
     *
     * @see #setEndPoint(Position)
     */
    public void setStartPoint(final Position point) throws TransformException {
        final DirectPosition p = userToGeodetic.transform(point.getDirectPosition());
        setStartPoint(p.getOrdinate(0), p.getOrdinate(1));
    }

    /**
     * Returns the starting point in the CRS specified at construction time.
     * This method returns the last point given to a {@code setStartPoint(…)} method,
     * transformed to the {@linkplain #getPositionCRS() position CRS}.
     *
     * @return the starting point represented in the CRS specified at construction time.
     * @throws TransformException if the coordinates can not be transformed to {@linkplain #getPositionCRS() position CRS}.
     * @throws IllegalStateException if the start point has not been specified.
     *
     * @see #getEndPoint()
     */
    public DirectPosition getStartPoint() throws TransformException {
        if ((validity & START_POINT) != 0) {
            return geographic(φ1, λ1).inverseTransform();
        } else {
            throw new IllegalStateException(Resources.format(Resources.Keys.StartOrEndPointNotSet_1, 0));
        }
    }

    /**
     * Sets {@link #userToGeodetic} to the given coordinates.
     * All coordinates in dimension 2 and above (typically the ellipsoidal height) are set to zero.
     *
     * @param  φ  the latitude value to set, in radians.
     * @param  λ  the longitude value to set, in radians.
     * @return {@link #userToGeodetic} for convenience.
     */
    private PositionTransformer geographic(final double φ, final double λ) {
        userToGeodetic.setOrdinate(0, toDegrees(φ));
        userToGeodetic.setOrdinate(1, toDegrees(λ));
        for (int i=userToGeodetic.getDimension(); --i >= 2;) {
            userToGeodetic.setOrdinate(i, 0);                   // Set height to ellipsoid surface.
        }
        return userToGeodetic;
    }

    /**
     * Sets the destination as geographic (<var>latitude</var>, <var>longitude</var>) coordinates.
     * The {@linkplain #getStartingAzimuth() starting azimuth}, {@linkplain #getEndingAzimuth() ending azimuth}
     * and {@linkplain #getGeodesicDistance() geodesic distance} will be updated as an effect of this call.
     *
     * @param  latitude   the latitude in degrees between {@value Latitude#MIN_VALUE}° and {@value Latitude#MAX_VALUE}°.
     * @param  longitude  the longitude in degrees.
     * @throws IllegalArgumentException if the latitude is out of bounds.
     *
     * @see #setStartPoint(double, double)
     */
    public void setEndPoint(final double latitude, final double longitude) {
        ArgumentChecks.ensureBetween("latitude", Latitude.MIN_VALUE, Latitude.MAX_VALUE, latitude);
        ArgumentChecks.ensureFinite("longitude", longitude);
        φ2 = toRadians(latitude);
        λ2 = toRadians(longitude);
        validity |= END_POINT;
        validity &= ~(STARTING_AZIMUTH | ENDING_AZIMUTH | GEODESIC_DISTANCE);
    }

    /**
     * Sets the destination as coordinates in arbitrary reference system. This method transforms the given
     * coordinates to geographic coordinates, then delegates to {@link #setEndPoint(double, double)}.
     * If the given point is not associated to a Coordinate Reference System (CRS), then this method assumes
     * the CRS specified at construction time.
     *
     * @param  position  the destination (end point) in any coordinate reference system.
     * @throws TransformException if the coordinates can not be transformed.
     *
     * @see #setStartPoint(Position)
     */
    public void setEndPoint(final Position position) throws TransformException {
        final DirectPosition p = userToGeodetic.transform(position.getDirectPosition());
        setEndPoint(p.getOrdinate(0), p.getOrdinate(1));
    }

    /**
     * Returns or computes the destination in the CRS specified at construction time. This method returns
     * the point specified in the last call to a {@link #setEndPoint(Position) setEndPoint(…)} method,
     * unless the {@linkplain #setStartingAzimuth(double) starting azimuth} and
     * {@linkplain #setGeodesicDistance(double) geodesic distance} have been set more recently.
     * In the later case, the end point will be computed from the {@linkplain #getStartPoint() start point}
     * and the current azimuth and distance.
     *
     * @return the destination (end point) represented in the CRS specified at construction time.
     * @throws TransformException if the coordinates can not be transformed to {@linkplain #getPositionCRS() position CRS}.
     * @throws IllegalStateException if the destination point, azimuth or distance have not been set.
     *
     * @see #getStartPoint()
     */
    public DirectPosition getEndPoint() throws TransformException {
        if ((validity & END_POINT) == 0) {
            computeEndPoint();
        }
        return geographic(φ2, λ2).inverseTransform();
    }

    /**
     * Sets the angular heading (relative to geographic North) at the starting point.
     * Azimuth is relative to geographic North with values increasing clockwise.
     * The {@linkplain #getEndPoint() end point} and {@linkplain #getEndingAzimuth() ending azimuth}
     * will be updated as an effect of this method call.
     *
     * @param  azimuth  the starting azimuth in degrees, with 0° toward north and values increasing clockwise.
     *
     * @see #setGeodesicDistance(double)
     */
    public void setStartingAzimuth(final double azimuth) {
        ArgumentChecks.ensureFinite("azimuth", azimuth);
        α1 = toRadians(IEEEremainder(azimuth, 360));
        validity |= STARTING_AZIMUTH;
        validity &= ~(END_POINT | ENDING_AZIMUTH);
    }

    /**
     * Returns or computes the angular heading (relative to geographic North) at the starting point.
     * This method returns the azimuth normalized to [-180 … +180]° range given in last call to
     * {@link #setStartingAzimuth(double)} method, unless the {@link #setEndPoint(Position) setEndPoint(…)}
     * method has been invoked more recently. In the later case, the azimuth will be computed from the
     * {@linkplain #getStartPoint() start point} and the current end point.
     *
     * @return the azimuth in degrees from -180° to +180°. 0° is toward North and values are increasing clockwise.
     * @throws IllegalStateException if the end point, azimuth or distance have not been set.
     */
    public double getStartingAzimuth() {
        if ((validity & STARTING_AZIMUTH) == 0) {
            computeDistance();
        }
        return toDegrees(α1);
    }

    /**
     * Computes the angular heading (relative to geographic North) at the ending point. This method computes the azimuth
     * from the current {@linkplain #setStartPoint(Position) start point} and {@linkplain #setEndPoint(Position) end point},
     * or from start point and the current {@linkplain #setStartingAzimuth(double) starting azimuth} and
     * {@linkplain #setGeodesicDistance(double) geodesic distance}.
     *
     * @return the azimuth in degrees from -180° to +180°. 0° is toward North and values are increasing clockwise.
     * @throws IllegalStateException if the destination point, azimuth or distance have not been set.
     */
    public double getEndingAzimuth() {
        if ((validity & ENDING_AZIMUTH) == 0) {
            if ((validity & END_POINT) == 0) {
                computeEndPoint();                      // Compute also ending azimuth from start point and distance.
            } else {
                computeDistance();                         // Compute also ending azimuth from start point and end point.
            }
        }
        return toDegrees(α2);
    }

    /**
     * Sets the geodesic distance from the start point to the end point. The {@linkplain #getEndPoint() end point}
     * and {@linkplain #getEndingAzimuth() ending azimuth} will be updated as an effect of this method call.
     *
     * @param  distance  the geodesic distance in unit of measurement given by {@link #getDistanceUnit()}.
     *
     * @see #setStartingAzimuth(double)
     */
    public void setGeodesicDistance(final double distance) {
        ArgumentChecks.ensurePositive("distance", distance);
        geodesicDistance = distance;
        validity |= GEODESIC_DISTANCE;
        validity &= ~(END_POINT | ENDING_AZIMUTH);
    }

    /**
     * Returns or computes the shortest distance from start point to end point. This is sometime called "great circle"
     * or "orthodromic" distance. This method returns the value given in last call to {@link #setGeodesicDistance(double)},
     * unless the {@link #setEndPoint(Position) setEndPoint(…)} method has been invoked more recently. In the later case,
     * the distance will be computed from the {@linkplain #getStartPoint() start point} and current end point.
     *
     * @return the shortest distance in the unit of measurement given by {@link #getDistanceUnit()}.
     * @throws IllegalStateException if the destination point has not been set.
     *
     * @see #getDistanceUnit()
     */
    public double getGeodesicDistance() {
        if ((validity & GEODESIC_DISTANCE) == 0) {
            computeDistance();
        }
        return geodesicDistance;
    }

    /**
     * Returns the unit of measurement of all distance measurements.
     * This is the {@linkplain Ellipsoid#getAxisUnit() ellipsoid axis unit}.
     *
     * @return the unit of measurement of all distance measurements.
     *
     * @see #getGeodesicDistance()
     */
    public Unit<Length> getDistanceUnit() {
        return ellipsoid.getAxisUnit();
    }

    /**
     * Computes the geodetic distance and azimuths from the start point and end point.
     * This method should be invoked if the distance or an azimuth is requested while
     * {@link #STARTING_AZIMUTH}, {@link #ENDING_AZIMUTH} or {@link #GEODESIC_DISTANCE}
     * validity flag is not set.
     *
     * <p>Note on terminology:</p>
     * <ul>
     *   <li><b>Course:</b> the intended path of travel.</li>
     *   <li><b>Track:</b>  the actual path traveled over ground.</li>
     * </ul>
     *
     * @throws IllegalStateException if the distance or azimuth has not been set.
     */
    private void computeDistance() {
        if ((validity & (START_POINT | END_POINT))
                     != (START_POINT | END_POINT))
        {
            throw new IllegalStateException(Resources.format(
                    Resources.Keys.StartOrEndPointNotSet_1, Integer.signum(validity & START_POINT)));
        }
        final double Δλ    = λ2 - λ1;           // No need to reduce to −π … +π range.
        final double sinΔλ = sin(Δλ);
        final double cosΔλ = cos(Δλ);
        final double sinφ1 = sin(φ1);
        final double cosφ1 = cos(φ1);
        final double sinφ2 = sin(φ2);
        final double cosφ2 = cos(φ2);

        final double cosφ1_sinφ2 = cosφ1 * sinφ2;
        final double cosφ2_sinφ1 = cosφ2 * sinφ1;
        final double α1y = cosφ2 * sinΔλ;
        final double α1x = cosφ1_sinφ2 - cosφ2_sinφ1*cosΔλ;

        α1 = atan2(α1y, α1x);
        α2 = atan2(cosφ1*sinΔλ, cosφ1_sinφ2*cosΔλ - cosφ2_sinφ1);
        /*
         * Δσ = acos(sinφ₁⋅sinφ₂ + cosφ₁⋅cosφ₂⋅cosΔλ) is a first estimation inaccurate for small distances.
         * Δσ = atan2(…) computes the same value but with better accuracy.
         */
        double Δσ = sinφ1*sinφ2 + cosφ1*cosφ2*cosΔλ;        // Actually Δσ = acos(…).
        Δσ = atan2(hypot(α1x, α1y), Δσ);
        geodesicDistance = radius * Δσ;
        validity |= (STARTING_AZIMUTH | ENDING_AZIMUTH | GEODESIC_DISTANCE);
    }

    /**
     * Computes the end point from the start point, the azimuth and the geodesic distance.
     * This method should be invoked if the end point or ending azimuth is requested while
     * {@link #END_POINT} validity flag is not set.
     *
     * <p>The default implementation computes {@link #φ2}, {@link #λ2} and {@link #α2} using
     * spherical formulas. Subclasses should override if they can provide ellipsoidal formulas.</p>
     *
     * @throws IllegalStateException if the azimuth and the distance have not been set.
     */
    void computeEndPoint() {
        if ((validity & (START_POINT | STARTING_AZIMUTH | GEODESIC_DISTANCE))
                     != (START_POINT | STARTING_AZIMUTH | GEODESIC_DISTANCE))
        {
            throw new IllegalStateException((validity & START_POINT) == 0
                    ? Resources.format(Resources.Keys.StartOrEndPointNotSet_1, 0)
                    : Resources.format(Resources.Keys.AzimuthAndDistanceNotSet));
        }
        final double Δσ    = geodesicDistance / radius;
        final double sinΔσ = sin(Δσ);
        final double cosΔσ = cos(Δσ);
        final double sinφ1 = sin(φ1);
        final double cosφ1 = cos(φ1);
        final double sinα1 = sin(α1);
        final double cosα1 = cos(α1);

        final double sinΔσ_cosα1 = sinΔσ * cosα1;
        final double Δλy = sinΔσ * sinα1;
        final double Δλx = cosΔσ*cosφ1 - sinφ1*sinΔσ_cosα1;
        final double Δλ  = atan2(Δλy, Δλx);

        final double sinφ2 = sinφ1*cosΔσ + cosφ1*sinΔσ_cosα1;       // First estimation of φ2.
        φ2 = atan(sinφ2 / hypot(Δλx, Δλy));                         // Improve accuracy close to poles.
        λ2 = IEEEremainder(λ1 + Δλ, 2*PI);
        α2 = atan2(sinα1, cosΔσ*cosα1 - sinφ1/cosφ1 * sinΔσ);
        validity |= END_POINT;
    }

    /**
     * Creates an approximation of the geodesic track from start point to end point as a Java2D object.
     * The coordinates are expressed in the coordinate reference system specified at creation time.
     * The approximation uses linear, quadratic or cubic Bézier curves.
     * The returned path has the following characteristics:
     *
     * <ol>
     *   <li>The first point is {@link #getStartPoint()}.</li>
     *   <li>The beginning of the curve (more specifically, the tangent at starting point) is oriented toward the direction given
     *       by {@linkplain #getStartingAzimuth()}, adjusted for the map projection (if any) deformation at that location.</li>
     *   <li>The curve passes at least by the midway point.</li>
     *   <li>The end of the curve (more specifically, the tangent at ending point) is oriented toward the direction given by
     *       {@linkplain #getEndingAzimuth()}, adjusted for the map projection (if any) deformation at that location.</li>
     *   <li>The last point is {@link #getEndPoint()}.</li>
     * </ol>
     *
     * <b>Limitations:</b>
     * current implementation builds a single linear, quadratic or cubic Bézier curve. It does not yet create a chain
     * of Bézier curves. Consequently the errors may be larger than the given {@code tolerance} threshold. Another
     * limitation is that this method depends on the presence of {@code java.desktop} module. Those limitations may be
     * addressed in a future version (see <a href="https://issues.apache.org/jira/browse/SIS-453">SIS-453</a>).
     *
     * @param  tolerance  maximal error between the approximated curve and actual geodesic track
     *                    in the units of measurement given by {@link #getDistanceUnit()}.
     *                    <em>See limitations above</em>.
     * @return an approximation of geodesic track as Bézier curves in a Java2D object.
     * @throws TransformException if the coordinates can not be transformed to {@linkplain #getPositionCRS() position CRS}.
     * @throws IllegalStateException if some required properties have not been specified.
     */
    public Shape toGeodesicPath2D(double tolerance) throws TransformException {
        if ((validity & (START_POINT | STARTING_AZIMUTH | END_POINT | ENDING_AZIMUTH | GEODESIC_DISTANCE))
                     != (START_POINT | STARTING_AZIMUTH | END_POINT | ENDING_AZIMUTH | GEODESIC_DISTANCE))
        {
            if ((validity & END_POINT) == 0) {
                computeEndPoint();
            } else {
                computeDistance();
            }
        }
        tolerance *= (180/PI) / radius;                                     // Angular tolerance in degrees.
        final double d1, x1, y1, d2, x2, y2;                                // Parameters for the Bezier curve.
        final double[] transformed = new double[ReferencingUtilities.getDimension(userToGeodetic.defaultCRS)];
        d1 = slope(α1, geographic(φ1, λ1).inverseTransform(transformed)); x1 = transformed[0]; y1 = transformed[1];
        d2 = slope(α2, geographic(φ2, λ2).inverseTransform(transformed)); x2 = transformed[0]; y2 = transformed[1];
        final double sφ2 = φ2;                                              // Save setting before modification.
        final double sλ2 = λ2;
        final double sα2 = α2;
        final double sd  = geodesicDistance;
        try {
            geodesicDistance /= 2;
            computeEndPoint();
            final Matrix d = geographic(φ2, λ2).inverseTransform(transformed);      // Coordinates of midway point.
            double εx;                                                              // Tolerance for φ (first coordinate).
            double εy = tolerance / cos(φ2);                                        // Tolerance for λ (second coordinate).
            εx = d.getElement(0,0)*tolerance + d.getElement(0,1)*εy;                // Tolerance for x in user CRS.
            εy = d.getElement(1,0)*tolerance + d.getElement(1,1)*εy;                // Tolerance for y in user CRS.
            return ShapeUtilities.bezier(x1, y1, transformed[0], transformed[1], x2, y2, d1, d2, εx, εy);
        } finally {
            φ2 = sφ2;                                                               // Restore the setting previously saved.
            λ2 = sλ2;
            α2 = sα2;
            geodesicDistance = sd;
        }
    }

    /**
     * Returns the tangent of the given angle converted to the user CRS space.
     *
     * @param  α  azimuth angle in radians, with 0 pointing toward north and values increasing clockwise.
     * @param  d  Jacobian matrix from (φ,λ) to the user coordinate reference system.
     * @return ∂y/∂x.
     */
    private double slope(final double α, final Matrix d) {
        final double dx = cos(α);     // sin(π/2 - α) = -sin(α - π/2) = cos(α)
        final double dy = sin(α);     // cos(π/2 - α) = +cos(α - π/2) = sin(α)
        final double tx = d.getElement(0,0)*dx + d.getElement(0,1)*dy;
        final double ty = d.getElement(1,0)*dx + d.getElement(1,1)*dy;
        return ty / tx;
    }

    /**
     * Returns a string representation of start point, end point, azimuths and distance.
     *
     * @return a string representation of this calculator state.
     */
    @Override
    public String toString() {
        final Locale     locale    = Locale.getDefault();
        final Vocabulary resources = Vocabulary.getResources(locale);
        final StringBuilder buffer = new StringBuilder();
        final String lineSeparator = System.lineSeparator();
        final CoordinateReferenceSystem crs = userToGeodetic.getCoordinateReferenceSystem();
        final boolean isGeographic = crs.equals(userToGeodetic.defaultCRS);
        try {
            resources.appendLabel(Vocabulary.Keys.CoordinateRefSys, buffer);
            buffer.append(' ').append(crs.getName().getCode()).append(lineSeparator);
            final TableAppender table = new TableAppender(buffer, " │ ");
            table.appendHorizontalSeparator();
            table.nextColumn(); if (isGeographic) table.append(resources.getString(Vocabulary.Keys.Latitude));
            table.nextColumn(); if (isGeographic) table.append(resources.getString(Vocabulary.Keys.Longitude));
            for (int i=crs.getCoordinateSystem().getDimension(); --i >= 2;) {
                table.nextColumn();     // Insert space for additional coordinates, e.g. ellipsoidal height.
            }
            table.nextColumn();
            table.append(resources.getString(Vocabulary.Keys.Azimuth)).nextLine();
            final CoordinateFormat cf = new CoordinateFormat(locale, null);
            cf.setSeparator("\t");      // For distributing coordinate values on different columns.
            boolean endPoint = false;
            do {
                table.append(resources.getString(endPoint ? Vocabulary.Keys.EndPoint : Vocabulary.Keys.StartPoint))
                     .nextColumn();
                try {
                    cf.format(endPoint ? getEndPoint() : getStartPoint(), table);
                    table.nextColumn();
                    table.append(new Angle(endPoint ? getEndingAzimuth() : getStartingAzimuth()).toString());
                } catch (IllegalStateException | TransformException e) {
                    // Ignore.
                }
                table.nextLine();
            } while ((endPoint = !endPoint) == true);
            table.appendHorizontalSeparator();
            table.flush();
            resources.appendLabel(Vocabulary.Keys.GeodesicDistance, buffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);      // Should never happen since we are writting in a StringBuilder.
        }
        buffer.append(String.format(locale, " %f %s", geodesicDistance, getDistanceUnit()));
        return buffer.toString();
    }
}
