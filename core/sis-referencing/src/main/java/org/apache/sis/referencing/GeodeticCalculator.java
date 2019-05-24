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
import java.awt.geom.Path2D;
import java.util.Locale;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.NumberFormat;
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
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Units;
import org.apache.sis.geometry.CoordinateFormat;
import org.apache.sis.internal.referencing.PositionTransformer;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.j2d.ShapeUtilities;
import org.apache.sis.internal.referencing.j2d.Bezier;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.Numerics;
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
     * The azimuth at start point and end point as vector components.
     * Angles can be obtained as below (reminder: tan(π/2 − α) = 1/tan(α)):
     *
     * <ul>
     *   <li>Arithmetic angle: {@code atan2(dφ, dλ)} (radians increasing anticlockwise)</li>
     *   <li>Geographic angle: {@code atan2(dλ, dφ)} gives the azimuth in radians between -π and +π
     *       with 0° pointing toward North and values increasing clockwise.</li>
     * </ul>
     *
     * Those vectors may not be normalized to unitary vectors. This is often not needed because most formulas are
     * written in a way that cancel the magnitude. If nevertheless needed, normalization will be applied in formulas.
     *
     * @see #STARTING_AZIMUTH
     * @see #ENDING_AZIMUTH
     */
    private double dφ1, dλ1, dφ2, dλ2;

    /**
     * The shortest distance from the starting point ({@link #φ1},{@link #λ1}) to the end point ({@link #φ2},{@link #λ2}).
     * The distance is in the same units than ellipsoid axes and the azimuth is in radians.
     *
     * @see #GEODESIC_DISTANCE
     * @see #getDistanceUnit()
     */
    private double geodesicDistance;

    /**
     * Length of the rhumb line from the starting point ({@link #φ1},{@link #λ1}) to the end point ({@link #φ2},{@link #λ2}).
     * The distance is in the same units than ellipsoid axes.
     *
     * @see #RHUMBLINE_LENGTH
     * @see #getDistanceUnit()
     */
    private double rhumblineLength;

    /**
     * A bitmask specifying which information are valid. For example if the {@link #END_POINT} bit is not set,
     * then {@link #φ2} and {@link #λ2} need to be computed, which implies the computation of ∂φ/∂λ as well.
     * If the {@link #GEODESIC_DISTANCE} bit is not set, then {@link #geodesicDistance} needs to be computed,
     * which implies recomputation of ∂φ/∂λ as well.
     */
    private int validity;

    /**
     * Bitmask specifying which information are valid.
     */
    private static final int START_POINT = 1, END_POINT = 2, STARTING_AZIMUTH = 4, ENDING_AZIMUTH = 8,
            GEODESIC_DISTANCE = 16, RHUMBLINE_LENGTH = 32;

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
     * <p><b>Limitation:</b>
     * current implementation uses only spherical formulas.
     * Implementation using ellipsoidal formulas will be provided in a future Apache SIS release.</p>
     *
     * @param  crs  the reference system for the {@link Position} objects.
     * @return a new geodetic calculator using the specified CRS.
     */
    public static GeodeticCalculator create(final CoordinateReferenceSystem crs) {
        return new GeodeticCalculator(crs);
    }

    /**
     * Returns {@code true} if at least one of the properties identified by the given mask is invalid.
     */
    private boolean isInvalid(final int mask) {
        return (validity & mask) != mask;
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
        return userToGeodetic.defaultCRS;
    }

    /**
     * Returns the coordinate reference system for all methods expecting (φ,λ) as {@code double} values.
     * This CRS always has (<var>latitude</var>, <var>longitude</var>) axes, in that order and in degrees.
     * The CRS may contain an additional axis for ellipsoidal height.
     *
     * @return the coordinate reference system of (φ,λ) coordinates.
     */
    public GeographicCRS getGeographicCRS() {
        return (GeographicCRS) userToGeodetic.getCoordinateReferenceSystem();
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
     * Returns the starting point in the CRS specified at construction time.
     * This method returns the last point given to a {@code setStartPoint(…)} method,
     * transformed to the {@linkplain #getPositionCRS() position CRS}.
     *
     * @return the starting point represented in the CRS specified at construction time.
     * @throws TransformException if the coordinates can not be transformed to {@linkplain #getPositionCRS() position CRS}.
     * @throws IllegalStateException if the start point has not yet been specified.
     *
     * @see #getEndPoint()
     */
    public DirectPosition getStartPoint() throws TransformException {
        if (isInvalid(START_POINT)) {
            throw new IllegalStateException(Resources.format(Resources.Keys.StartOrEndPointNotSet_1, 0));
        }
        return geographic(φ1, λ1).inverseTransform();
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
     * Sets the starting point as geographic (<var>latitude</var>, <var>longitude</var>) coordinates.
     * The {@linkplain #getStartingAzimuth() starting} and {@linkplain #getEndingAzimuth() ending azimuths},
     * the {@linkplain #getEndPoint() end point}, the {@linkplain #getGeodesicDistance() geodesic distance}
     * and the {@linkplain #getRhumblineLength() rhumb line length}
     * are discarded by this method call; some of them will need to be specified again.
     *
     * @param  latitude   the latitude in degrees between {@value Latitude#MIN_VALUE}° and {@value Latitude#MAX_VALUE}°.
     * @param  longitude  the longitude in degrees.
     *
     * @see #setEndPoint(double, double)
     * @see #moveToEndPoint()
     */
    public void setStartPoint(final double latitude, final double longitude) {
        ArgumentChecks.ensureFinite("latitude",  latitude);
        ArgumentChecks.ensureFinite("longitude", longitude);
        φ1 = toRadians(max(Latitude.MIN_VALUE, min(Latitude.MAX_VALUE, latitude)));
        λ1 = toRadians(longitude);
        validity = START_POINT;
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
        if (isInvalid(END_POINT)) {
            computeEndPoint();
        }
        return geographic(φ2, λ2).inverseTransform();
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
     * Sets the destination as geographic (<var>latitude</var>, <var>longitude</var>) coordinates.
     * The {@linkplain #getStartingAzimuth() starting azimuth}, {@linkplain #getEndingAzimuth() ending azimuth}
     * {@linkplain #getGeodesicDistance() geodesic distance} and {@linkplain #getRhumblineLength() rhumb line length}
     * will be updated as an effect of this call.
     *
     * @param  latitude   the latitude in degrees between {@value Latitude#MIN_VALUE}° and {@value Latitude#MAX_VALUE}°.
     * @param  longitude  the longitude in degrees.
     *
     * @see #setStartPoint(double, double)
     */
    public void setEndPoint(final double latitude, final double longitude) {
        ArgumentChecks.ensureFinite("latitude",  latitude);
        ArgumentChecks.ensureFinite("longitude", longitude);
        φ2 = toRadians(max(Latitude.MIN_VALUE, min(Latitude.MAX_VALUE, latitude)));
        λ2 = toRadians(longitude);
        validity |= END_POINT;
        validity &= ~(STARTING_AZIMUTH | ENDING_AZIMUTH | GEODESIC_DISTANCE | RHUMBLINE_LENGTH);
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
        if (isInvalid(STARTING_AZIMUTH)) {
            computeDistance();
        }
        return toDegrees(atan2(dλ1, dφ1));                  // tan(π/2 − θ)  =  1/tan(θ)
    }

    /**
     * Sets the angular heading (relative to geographic North) at the starting point.
     * Azimuth is relative to geographic North with values increasing clockwise.
     * The {@linkplain #getEndingAzimuth() ending azimuth}, {@linkplain #getEndPoint() end point}
     * and {@linkplain #getRhumblineLength() rhumb line length}
     * will be updated as an effect of this method call.
     *
     * @param  azimuth  the starting azimuth in degrees, with 0° toward north and values increasing clockwise.
     *
     * @see #setGeodesicDistance(double)
     */
    public void setStartingAzimuth(double azimuth) {
        ArgumentChecks.ensureFinite("azimuth", azimuth);
        azimuth = toRadians(azimuth);
        dφ1 = cos(azimuth);                                 // sin(π/2 − θ)  =  cos(θ)
        dλ1 = sin(azimuth);                                 // cos(π/2 − θ)  =  sin(θ)
        validity |= STARTING_AZIMUTH;
        validity &= ~(END_POINT | ENDING_AZIMUTH | RHUMBLINE_LENGTH);
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
        if (isInvalid(ENDING_AZIMUTH)) {
            if (isInvalid(END_POINT)) {
                computeEndPoint();                      // Compute also ending azimuth from start point and distance.
            } else {
                computeDistance();                         // Compute also ending azimuth from start point and end point.
            }
        }
        return toDegrees(atan2(dλ2, dφ2));
    }

    /**
     * Returns or computes the shortest distance from start point to end point. This is sometime called "great circle"
     * or "orthodromic" distance. This method returns the value given in last call to {@link #setGeodesicDistance(double)},
     * unless the {@link #setEndPoint(Position) setEndPoint(…)} method has been invoked more recently. In the later case,
     * the distance will be computed from the {@linkplain #getStartPoint() start point} and current end point.
     *
     * @return the shortest distance in the unit of measurement given by {@link #getDistanceUnit()}.
     * @throws IllegalStateException if a point has not been set.
     *
     * @see #getDistanceUnit()
     */
    public double getGeodesicDistance() {
        if (isInvalid(GEODESIC_DISTANCE)) {
            computeDistance();
        }
        return geodesicDistance;
    }

    /**
     * Sets the geodesic distance from the start point to the end point. The {@linkplain #getEndPoint() end point},
     * {@linkplain #getEndingAzimuth() ending azimuth} and {@linkplain #getRhumblineLength() rhumb line length}
     * will be updated as an effect of this method call.
     *
     * @param  distance  the geodesic distance in unit of measurement given by {@link #getDistanceUnit()}.
     *
     * @see #setStartingAzimuth(double)
     */
    public void setGeodesicDistance(final double distance) {
        ArgumentChecks.ensurePositive("distance", distance);
        geodesicDistance = distance;
        validity |= GEODESIC_DISTANCE;
        validity &= ~(END_POINT | ENDING_AZIMUTH | RHUMBLINE_LENGTH);
    }

    /**
     * Returns or computes the length of rhumb line (part of constant heading) from start point to end point.
     * This is sometime called "loxodrome". This is <strong>not</strong> the shortest path between two points.
     *
     * @return length of rhumb line in the unit of measurement given by {@link #getDistanceUnit()}.
     * @throws IllegalStateException if a point has not been set.
     */
    public double getRhumblineLength() {
        if (isInvalid(RHUMBLINE_LENGTH)) {
            computeRhumbLine();
        }
        return rhumblineLength;
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
     * Computes the length of rhumb line from start point to end point.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Rhumb_line">Rhumb line on Wikipedia</a>
     *
     * @todo if {@literal Δλ > 180}, must split in two segments.
     */
    private void computeRhumbLine() {
        if (isInvalid(START_POINT | END_POINT)) {
            throw new IllegalStateException(Resources.format(
                    Resources.Keys.StartOrEndPointNotSet_1, Integer.signum(validity & START_POINT)));
        }
        final double Δλ = λ2 - λ1;
        final double Δφ = φ2 - φ1;
        final double factor;
        if (abs(Δφ) < Formulas.ANGULAR_TOLERANCE) {
            factor = Δλ * cos((φ1 + φ2)/2);
        } else {
            /*
             * Inverse of Gudermannian function is log(tan(π/4 + φ/2)).
             * The loxodrome formula involves the following difference:
             *
             *   ΔG  =  log(tan(π/4 + φ₁/2)) - log(tan(π/4 + φ₂/2))
             *       =  log(tan(π/4 + φ₁/2) / tan(π/4 + φ₂/2))
             *
             * Note that ΔG=0 if φ₁=φ₂, which implies cos(α)=0.
             */
            final double ΔG = log(tan(PI/4 + φ1/2) / tan(PI/4 + φ2/2));
            final double α = atan(Δλ / ΔG);
            factor = Δφ / cos(α);
        }
        rhumblineLength = radius * abs(factor);
        validity |= RHUMBLINE_LENGTH;
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
        if (isInvalid(START_POINT | END_POINT)) {
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
        dλ1 = cosφ2*sinΔλ;
        dλ2 = cosφ1*sinΔλ;
        dφ1 = cosφ1_sinφ2 - cosφ2_sinφ1*cosΔλ;
        dφ2 = cosφ1_sinφ2*cosΔλ - cosφ2_sinφ1;
        /*
         * Δσ = acos(sinφ₁⋅sinφ₂ + cosφ₁⋅cosφ₂⋅cosΔλ) is a first estimation inaccurate for small distances.
         * Δσ = atan2(…) computes the same value but with better accuracy.
         */
        double Δσ = sinφ1*sinφ2 + cosφ1*cosφ2*cosΔλ;        // Actually Δσ = acos(…).
        Δσ = atan2(hypot(dλ1, dφ1), Δσ);
        geodesicDistance = radius * Δσ;
        validity |= (STARTING_AZIMUTH | ENDING_AZIMUTH | GEODESIC_DISTANCE);
    }

    /**
     * Computes the end point from the start point, the azimuth and the geodesic distance.
     * This method should be invoked if the end point or ending azimuth is requested while
     * {@link #END_POINT} validity flag is not set.
     *
     * <p>The default implementation computes {@link #φ2}, {@link #λ2} and ∂φ/∂λ derivatives using
     * spherical formulas. Subclasses should override if they can provide ellipsoidal formulas.</p>
     *
     * @throws IllegalStateException if the azimuth and the distance have not been set.
     */
    void computeEndPoint() {
        if (isInvalid(START_POINT | STARTING_AZIMUTH | GEODESIC_DISTANCE)) {
            throw new IllegalStateException(isInvalid(START_POINT)
                    ? Resources.format(Resources.Keys.StartOrEndPointNotSet_1, 0)
                    : Resources.format(Resources.Keys.AzimuthAndDistanceNotSet));
        }
        final double vm    = hypot(dφ1, dλ1);
        final double Δσ    = geodesicDistance / radius;
        final double sinΔσ = sin(Δσ);
        final double cosΔσ = cos(Δσ);
        final double sinφ1 = sin(φ1);
        final double cosφ1 = cos(φ1);
        final double sinα1 = dλ1 / vm;          // α₁ is the azimuth at starting point.
        final double cosα1 = dφ1 / vm;
        final double sinΔσ_cosα1 = sinΔσ * cosα1;
        final double Δλy = sinΔσ * sinα1;
        final double Δλx = cosΔσ*cosφ1 - sinφ1*sinΔσ_cosα1;
        final double Δλ  = atan2(Δλy, Δλx);

        final double sinφ2 = sinφ1*cosΔσ + cosφ1*sinΔσ_cosα1;       // First estimation of φ2.
        φ2  = atan(sinφ2 / hypot(Δλx, Δλy));                        // Improve accuracy close to poles.
        λ2  = IEEEremainder(λ1 + Δλ, 2*PI);
        dφ2 = cosΔσ*cosα1 - sinφ1/cosφ1 * sinΔσ;
        dλ2 = sinα1;
        validity |= END_POINT;
    }

    /**
     * Sets the start point and starting azimuth to the current end point and ending azimuth values.
     * The {@linkplain #getEndingAzimuth() ending azimuths}, the {@linkplain #getGeodesicDistance()
     * geodesic distance} and the {@linkplain #getEndPoint() end point} are discarded by this method call;
     * some of them will need to be specified again.
     *
     * @see #setStartPoint(double, double)
     */
    public void moveToEndPoint() {
        if (isInvalid(END_POINT)) {
            computeEndPoint();
        }
        φ1  = φ2;  dφ1 = dφ2;
        λ1  = λ2;  dλ1 = dλ2;
        validity = START_POINT | STARTING_AZIMUTH;
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
     *   <li>The point B(½) in the middle of the Bézier curve is a point of the geodesic path.</li>
     *   <li>The end of the curve (more specifically, the tangent at ending point) is oriented toward the direction given by
     *       {@linkplain #getEndingAzimuth()}, adjusted for the map projection (if any) deformation at that location.</li>
     *   <li>The last point is {@link #getEndPoint()}, potentially with 360° added or subtracted to the longitude.</li>
     * </ol>
     *
     * This method tries to stay within the given tolerance threshold of the geodesic track.
     * The {@code tolerance} parameter should not be too small for avoiding creation of unreasonably long chain of Bézier curves.
     * For example a value of 1/10 of geodesic length may be sufficient.
     *
     * <b>Limitations:</b>
     * This method depends on the presence of {@code java.desktop} module. This limitations may be addressed
     * in a future Apache SIS version (see <a href="https://issues.apache.org/jira/browse/SIS-453">SIS-453</a>).
     *
     * @param  tolerance  maximal error between the approximated curve and actual geodesic track
     *                    in the units of measurement given by {@link #getDistanceUnit()}.
     *                    This is approximate; the actual errors may vary around that value.
     * @return an approximation of geodesic track as Bézier curves in a Java2D object.
     * @throws TransformException if the coordinates can not be transformed to {@linkplain #getPositionCRS() position CRS}.
     * @throws IllegalStateException if some required properties have not been specified.
     */
    public Shape createGeodesicPath2D(final double tolerance) throws TransformException {
        ArgumentChecks.ensureStrictlyPositive("tolerance", tolerance);
        if (isInvalid(START_POINT | STARTING_AZIMUTH | END_POINT | ENDING_AZIMUTH | GEODESIC_DISTANCE)) {
            if (isInvalid(END_POINT)) {
                computeEndPoint();
            } else {
                computeDistance();
            }
        }
        final PathBuilder bezier = new PathBuilder(tolerance);
        final Path2D path;
        try {
            path = bezier.build();
        } finally {
            bezier.reset();
        }
        return ShapeUtilities.toPrimitive(path);
    }

    /**
     * Creates an approximation of the region at a constant geodesic distance around the start point.
     * The returned shape is circlelike with the {@linkplain #getStartPoint() start point} in its center.
     * The coordinates are expressed in the coordinate reference system specified at creation time.
     * The approximation uses cubic Bézier curves.
     *
     * <p>This method tries to stay within the given tolerance threshold of the geodesic track.
     * The {@code tolerance} parameter should not be too small for avoiding creation of unreasonably long chain of Bézier curves.
     * For example a value of 1/10 of geodesic length may be sufficient.</p>
     *
     * <b>Limitations:</b>
     * This method depends on the presence of {@code java.desktop} module. This limitations may be addressed
     * in a future Apache SIS version (see <a href="https://issues.apache.org/jira/browse/SIS-453">SIS-453</a>).
     *
     * @param  tolerance  maximal error in the units of measurement given by {@link #getDistanceUnit()}.
     *                    This is approximate; the actual errors may vary around that value.
     * @return an approximation of circular region as a Java2D object.
     * @throws TransformException if the coordinates can not be transformed to {@linkplain #getPositionCRS() position CRS}.
     * @throws IllegalStateException if some required properties have not been specified.
     */
    public Shape createCircularRegion2D(final double tolerance) throws TransformException {
        ArgumentChecks.ensureStrictlyPositive("tolerance", tolerance);
        if (isInvalid(START_POINT | GEODESIC_DISTANCE)) {
            computeDistance();
        }
        final CircularPath bezier = new CircularPath(tolerance);
        final Path2D path;
        try {
            path = bezier.build();
        } finally {
            bezier.reset();
        }
        path.closePath();
        return path;
    }

    /**
     * Builds a geodesic path as a sequence of Bézier curves. The start point and end points are the points
     * in enclosing {@link GeodeticCalculator} at the time this class is instantiated. The start coordinates
     * given by {@link #φ1} and {@link #λ1} shall never change for this whole builder lifetime. However the
     * end coordinates ({@link #φ2}, {@link #λ2}) will vary at each step.
     */
    private class PathBuilder extends Bezier {
        /**
         * The initial (i) and final (f) coordinates and derivatives, together with geodesic and loxodromic distances.
         * Saved for later restoration by {@link #reset()}.
         */
        private final double dφi, dλi, dφf, dλf, φf, λf, distance, length;

        /**
         * {@link #validity} flags at the time {@code PathBuilder} is instantiated.
         * Saved for later restoration by {@link #reset()}.
         */
        private final int flags;

        /**
         * Angular tolerance at equator in degrees.
         */
        private final double tolerance;

        /**
         * Creates a builder for the given tolerance at equator in metres.
         */
        PathBuilder(final double εx) {
            super(ReferencingUtilities.getDimension(userToGeodetic.defaultCRS));
            dφi = dφ1;  dλi = dλ1;
            dφf = dφ2;  dλf = dλ2;  φf = φ2;  λf = λ2;
            tolerance = toDegrees(εx / radius);
            distance  = geodesicDistance;
            length    = rhumblineLength;
            flags     = validity;
        }

        /**
         * Invoked for computing a new point on the Bézier curve. This method is invoked with a <var>t</var> value varying from
         * 0 (start point) to 1 (end point) inclusive. This method stores the coordinates in the {@link #point} array and the
         * derivative (∂y/∂x) in the {@linkplain #dx dx} and {@linkplain #dy dy} fields.
         *
         * @param  t  desired point on the curve, from 0 (start point) to 1 (end point) inclusive.
         * @throws TransformException if the point coordinates can not be computed.
         */
        @Override
        protected void evaluateAt(final double t) throws TransformException {
            if (t == 0) {
                φ2 = φ1;  dφ2 = dφ1;            // Start point requested.
                λ2 = λ1;  dλ2 = dλ1;
            } else if (t == 1) {
                φ2 = φf;  dφ2 = dφf;            // End point requested.
                λ2 = λf;  dλ2 = dλf;
            } else {
                geodesicDistance = distance * t;
                validity |= GEODESIC_DISTANCE;
                computeEndPoint();
            }
            evaluateAtEndPoint();
        }

        /**
         * Implementation of {@link #evaluateAt(double)} using the current φ₂, λ₂ and ∂φ₂/∂λ₂ values.
         * This method stores the projected coordinates in the {@link #point} array and stores the
         * derivative ∂y/∂x in the {@link #dx}, {@link #dy} fields.
         */
        final void evaluateAtEndPoint() throws TransformException {
            if ((λ2 - λ1) * dλ1 < 0) {            // Reminder: Δλ or dλ₁ may be zero.
                λ2 += 2*PI * signum(dλ1);         // We need λ₁ < λ₂ if heading east, or λ₁ > λ₂ if heading west.
            }
            final Matrix d = geographic(φ2, λ2).inverseTransform(point);    // Coordinates and Jacobian of point.
            final double m00 = d.getElement(0,0);
            final double m01 = d.getElement(0,1);
            final double m10 = d.getElement(1,0);
            final double m11 = d.getElement(1,1);
            εy = tolerance / cos(abs(φ2));                                  // Tolerance for λ (second coordinate).
            εx = m00*tolerance + m01*εy;                                    // Tolerance for x in user CRS.
            εy = m10*tolerance + m11*εy;                                    // Tolerance for y in user CRS.
            /*
             * Returns the tangent of the ending azimuth converted to the user CRS space.
             * d is the Jacobian matrix from (φ,λ) to the user coordinate reference system.
             */
            dx = m00*dφ2 + m01*dλ2;                                         // Reminder: coordinates in (φ,λ) order.
            dy = m10*dφ2 + m11*dλ2;
        }

        /**
         * Returns whether the point at given (<var>x</var>, <var>y</var>) coordinates is close to the geodesic path.
         * This method is invoked when the {@link Bezier} helper class thinks that the point is not on the path, but
         * could be wrong because of the difficulty to evaluate the Bézier <var>t</var> parameter of closest point.
         *
         * @see <a href="https://issues.apache.org/jira/browse/SIS-455">SIS-455</a>
         */
        @Override
        protected boolean isValid(final double x, final double y) throws TransformException {
            point[0] = x;
            point[1] = y;
            for (int i=2; i<point.length; i++) {
                point[i] = 0;
            }
            userToGeodetic.transform(point);
            setEndPoint(point[0], point[1]);
            /*
             * Computes the azimuth to the given point. This azimuth may be different than the azimuth of the
             * geodesic path we are building. Compute the point that we would have if the azimuth was correct
             * and check the distance between those two points.
             */
            computeDistance();
            dφ1 = dφi;
            dλ1 = dλi;
            computeEndPoint();
            final DirectPosition p = geographic(φ2, λ2).inverseTransform();
            return abs(p.getOrdinate(0) - x) <= εx &&
                   abs(p.getOrdinate(1) - y) <= εy;
        }

        /**
         * Restores the enclosing {@link GeodeticCalculator} to the state that it has at {@code PathBuilder} instantiation time.
         */
        final void reset() {
            dφ1 = dφi;  dφ2 = dφf;  φ2 = φf;
            dλ1 = dλi;  dλ2 = dλf;  λ2 = λf;
            geodesicDistance = distance;
            rhumblineLength  = length;
            validity         = flags;
        }
    }

    /**
     * Builds a circular region around the start point. The shape is created as a sequence of Bézier curves.
     */
    private final class CircularPath extends PathBuilder {
        /**
         * Creates a builder for the given tolerance at equator in degrees.
         */
        CircularPath(final double εx) {
            super(εx);
        }

        /**
         * Invoked for computing a new point on the circular path. This method is invoked with a <var>t</var> value varying from
         * 0 to 1 inclusive. The <var>t</var> value is multiplied by 2π for getting an angle. This method stores the coordinates
         * in the {@link #point} array and the derivative (∂y/∂x) in the {@linkplain #dx dx} and {@linkplain #dy dy} fields.
         *
         * @param  t  angle fraction from 0 to 1 inclusive.
         * @throws TransformException if the point coordinates can not be computed.
         */
        @Override
        protected void evaluateAt(final double t) throws TransformException {
            double α1 = IEEEremainder((t - 0.5) * (2*PI), 2*PI);
            dλ1 = sin(α1);
            dφ1 = cos(α1);
            validity |= STARTING_AZIMUTH;
            computeEndPoint();
            evaluateAtEndPoint();
            if (depth <= 1) {
                // Force division of the curve in two smaller curves. We want at least 4 Bézier curves in an ellipse.
                εx = εy = -1;
            }
            final double d = dx;
            dx = dy;
            dy = -d;        // Perpendicular direction.
        }

        /**
         * No additional test (compared to {@link Bezier} base class) for determining if the point is close enough.
         */
        @Override
        protected boolean isValid(final double x, final double y) throws TransformException {
            return false;
        }
    }

    /**
     * Returns a string representation of start point, end point, azimuths and distance.
     * The text representation is implementation-specific and may change in any future version.
     * Current implementation is like below:
     *
     * {@preformat text
     *   Coordinate reference system: Unspecified datum based upon the GRS 1980 Authalic Sphere
     *   ┌─────────────┬─────────────────┬──────────────────┬─────────────┐
     *   │             │    Latitude     │    Longitude     │   Azimuth   │
     *   │ Start point │  9°39′06.1120″N │ 132°37′37.1248″W │  -17°10′37″ │
     *   │ End point   │ 70°32′45.0206″N │ 109°50′05.0533″E │ -119°03′12″ │
     *   └─────────────┴─────────────────┴──────────────────┴─────────────┘
     *   Geodesic distance: 9,967,530.74 m
     * }
     *
     * @return a string representation of this calculator state.
     */
    @Override
    public String toString() {
        final StringBuilder buffer        = new StringBuilder();
        final Locale        locale        = Locale.getDefault();
        final Vocabulary    resources     = Vocabulary.getResources(locale);
        final String        lineSeparator = System.lineSeparator();
        final CoordinateReferenceSystem crs = getPositionCRS();
        try {
            /*
             * Header: name of the Coordinate Reference System.
             */
            resources.appendLabel(Vocabulary.Keys.CoordinateRefSys, buffer);
            buffer.append(' ').append(crs.getName().getCode()).append(lineSeparator);
            /*
             * Start point and end point together with their azimuth, formatted as a table.
             */
            if ((validity & (START_POINT | STARTING_AZIMUTH | END_POINT | ENDING_AZIMUTH)) != 0) {
                final String[] axes = ReferencingUtilities.getShortAxisNames(resources, crs);
                final AngleFormat    azimuthFormat = new AngleFormat("DD°MM′SS″", locale);
                final CoordinateFormat pointFormat = new CoordinateFormat(locale, null);
                pointFormat.setSeparator("\t");      // For distributing coordinate values on different columns.
                pointFormat.setDefaultCRS(crs);
                pointFormat.setPrecision(Formulas.LINEAR_TOLERANCE, Units.METRE);
                final TableAppender table = new TableAppender(buffer, " │ ");
                table.setCellAlignment(TableAppender.ALIGN_CENTER);
                table.appendHorizontalSeparator();
                for (final String axis : axes) {
                    table.nextColumn();
                    table.append(axis);
                }
                table.nextColumn();
                table.append(resources.getString(Vocabulary.Keys.Azimuth)).nextLine();
                boolean endPoint = false;
                do {
                    table.setCellAlignment(TableAppender.ALIGN_LEFT);
                    table.append(resources.getString(endPoint ? Vocabulary.Keys.EndPoint : Vocabulary.Keys.StartPoint)).nextColumn();
                    table.setCellAlignment(TableAppender.ALIGN_RIGHT);
                    try {
                        pointFormat.format(endPoint ? getEndPoint() : getStartPoint(), table);
                        table.nextColumn();
                        table.append(azimuthFormat.format(endPoint ? getEndingAzimuth() : getStartingAzimuth()));
                    } catch (IllegalStateException | TransformException e) {
                        // Ignore.
                    }
                    table.nextLine();
                } while ((endPoint = !endPoint) == true);
                table.appendHorizontalSeparator();
                table.flush();
            }
            /*
             * Distances, formatted with a number of decimal fraction digits suitable for at least 1 centimetre precision.
             */
            try {
                final Unit<Length> unit = getDistanceUnit();
                final double distance   = getGeodesicDistance();
                final double precision  = Units.METRE.getConverterTo(unit).convert(Formulas.LINEAR_TOLERANCE);
                final NumberFormat nf = NumberFormat.getNumberInstance(locale);
                nf.setMaximumFractionDigits(max(Numerics.suggestFractionDigits(precision), 0));
                resources.appendLabel(Vocabulary.Keys.GeodesicDistance, buffer);
                buffer.append(' ').append(nf.format(distance))
                      .append(' ').append(unit).append(lineSeparator);
            } catch (IllegalStateException e) {
                // Ignore.
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);      // Should never happen since we are writting in a StringBuilder.
        }
        return buffer.toString();
    }
}
