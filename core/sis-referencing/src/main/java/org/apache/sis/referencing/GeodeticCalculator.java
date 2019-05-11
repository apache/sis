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

import java.util.Locale;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.measure.Unit;
import javax.measure.quantity.Length;

import org.opengis.referencing.datum.Ellipsoid;
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
 *   <li>The {@linkplain #setStartPoint(Position) start point}, which is always considered valid.
 *     It is initially set at (0,0) and can only be changed to another valid value.</li>
 *   <li>One of the followings (the latest specified property overrides other properties and determines what will be calculated):
 *     <ul>
 *       <li>the {@linkplain #setEndPoint(Position) end point}, or</li>
 *       <li>an {@linkplain #setDirection(double, double) azimuth and distance}.</li>
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
    protected final Ellipsoid ellipsoid;

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
     */
    private double φ1, λ1;

    /**
     * The (<var>latitude</var>, <var>longitude</var>) coordinates of the end point <strong>in radians</strong>.
     * This point is set by {@link #setEndPoint(double, double)}.
     */
    private double φ2, λ2;

    /**
     * The azimuth at start point and end point, in radians between -π and +π.
     * 0° point toward North and values are increasing clockwise.
     */
    private double α1, α2;

    /**
     * The distance from the starting point ({@link #φ1},{@link #λ1}) to the end point ({@link #φ2},{@link #λ2}).
     * The distance is in the same units than ellipsoid axes and the azimuth is in radians.
     *
     * @see #getDistanceUnit()
     */
    private double distance;

    /**
     * Tells if the end point is valid.
     * This is {@code false} if {@link #φ2} and {@link #λ2} need to be computed.
     */
    private boolean isEndPointValid;

    /**
     * Tells if the azimuths and the distance are valid.
     * If {@code false} then {@link #distance}, {@link #α1} and {@link #α2} need to be computed.
     */
    private boolean isCourseValid;

    /**
     * Constructs a new geodetic calculator expecting coordinates in the supplied CRS.
     * The geodetic formulas implemented by this {@code GeodeticCalculator} base class assume a spherical model.
     * This constructor is for subclasses computing geodesy on an ellipsoid or other figure of the Earth.
     * Users should invoke {@link #create(CoordinateReferenceSystem)} instead, which will choose a subtype
     * based on the given coordinate reference system.
     *
     * @param  crs  the reference system for the {@link Position} arguments and return values.
     */
    protected GeodeticCalculator(final CoordinateReferenceSystem crs) {
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
     * Creates a new geodetic calculator associated with default ellipsoid.
     * The ellipsoid and the geodetic datum of {@linkplain #getPositionCRS() position CRS}
     * are the ones associated to {@link CommonCRS#defaultGeographic()}.
     * The axes are (<var>latitude</var>, <var>longitude</var>) in degrees.
     *
     * @return a new geodetic calculator using the default geodetic datum.
     */
    public static GeodeticCalculator create() {
        return new GeodeticCalculator(CommonCRS.DEFAULT.geographic());
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
        isEndPointValid = false;
        isCourseValid = false;
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
     *
     * @see #getEndPoint()
     */
    public DirectPosition getStartPoint() throws TransformException {
        return geographic(φ1, λ1).inverseTransform();
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
     * The azimuth and geodesic distance values will be updated as an effect of this call.
     * They will be recomputed next time that {@link #getStartingAzimuth()}, {@link #getEndingAzimuth()}
     * or {@link #getGeodesicDistance()} is invoked.
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
        isEndPointValid = true;
        isCourseValid = false;
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
     * Returns or computes the destination in the CRS specified at construction time. This method returns the
     * point specified in the last call to a {@link #setEndPoint(double, double) setEndPoint(…)} method, unless
     * {@link #setDirection(double, double) setDirection(…)} has been invoked more recently. In the later case,
     * the end point will be computed from the {@linkplain #getStartPoint() starting point} and the specified
     * azimuth and distance.
     *
     * @return the destination (end point) represented in the CRS specified at construction time.
     * @throws TransformException if the coordinates can not be transformed to {@linkplain #getPositionCRS() position CRS}.
     * @throws IllegalStateException if the destination point, azimuth or distance have not been set.
     *
     * @see #getStartPoint()
     */
    public DirectPosition getEndPoint() throws TransformException {
        if (!isEndPointValid) {
            computeEndPoint();
        }
        return geographic(φ2, λ2).inverseTransform();
    }

    /**
     * Sets the azimuth and the distance from the {@linkplain #getStartPoint() starting point}.
     * The direction is relative to geographic North, with values increasing clockwise.
     * The distance is usually a positive number, but negative numbers are accepted
     * as courses in direction opposite to the given azimuth.
     *
     * <p>The {@linkplain #getEndPoint() end point} will be updated as an effect of this method call;
     * it will be recomputed the next time that {@link #getEndPoint()} is invoked.</p>
     *
     * @param  azimuth   the starting azimuth in degrees, with 0° toward north and values increasing clockwise.
     * @param  distance  the geodesic distance in unit of measurement given by {@link #getDistanceUnit()}.
     *
     * @see #getStartingAzimuth()
     * @see #getGeodesicDistance()
     */
    public void setDirection(double azimuth, double distance) {
        ArgumentChecks.ensureFinite("azimuth",  azimuth);
        ArgumentChecks.ensureFinite("distance", distance);
        if (distance < 0) {
            distance = -distance;
            azimuth += 180;
        }
        α1              = toRadians(IEEEremainder(azimuth, 360));
        this.distance   = distance;
        isEndPointValid = false;
        isCourseValid   = true;
    }

    /**
     * Returns the angular heading (relative to geographic North) at the starting point.
     * This method returns the azimuth normalized to [-180 … +180]° range given in last call to
     * <code>{@linkplain #setDirection(double, double) setDirection}(azimuth, …)</code> method,
     * unless the {@link #setEndPoint(double, double) setEndPoint(…)} method has been invoked more recently.
     * In the later case, the azimuth will be computed from the {@linkplain #getStartPoint start point}.
     *
     * @return the azimuth in degrees from -180° to +180°. 0° is toward North and values are increasing clockwise.
     * @throws IllegalStateException if the destination point, azimuth or distance have not been set.
     */
    public double getStartingAzimuth() {
        if (!isCourseValid) {
            computeCourse();
        }
        return toDegrees(α1);
    }

    /**
     * Returns the angular heading (relative to geographic North) at the ending point.
     *
     * @return the azimuth in degrees from -180° to +180°. 0° is toward North and values are increasing clockwise.
     * @throws IllegalStateException if the destination point, azimuth or distance have not been set.
     */
    public double getEndingAzimuth() {
        if (!isCourseValid) {
            computeCourse();
        } else if (!isEndPointValid) {
            computeEndPoint();
        }
        return toDegrees(α2);
    }

    /**
     * Returns the shortest distance from start point to end point.
     * This is sometime called "great circle" or "orthodromic" distance.
     * This method returns the absolute distance value set by the last call to
     * {@link #setDirection(double,double) setDirection(…, distance)} method,
     * unless the {@link #setEndPoint(double, double) setEndPoint(…)} method has been invoked more recently.
     * In the later case, the distance will be computed from the {@linkplain #getStartPoint start point} to the end point.
     *
     * @return The shortest distance in the unit of measurement given by {@link #getDistanceUnit()}.
     * @throws IllegalStateException if the destination point has not been set.
     *
     * @see #getDistanceUnit()
     */
    public double getGeodesicDistance() {
        if (!isCourseValid) {
            computeCourse();
        }
        return distance;
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
     * Computes the end point from the start point, the azimuth and the geodesic distance.
     * This method should be invoked if the end point or ending azimuth is requested while
     * {@link #isEndPointValid} is {@code false}.
     *
     * <p>The default implementation computes {@link #φ2}, {@link #λ2} and {@link #α2} using
     * spherical formulas. Subclasses should override if they can provide ellipsoidal formulas.</p>
     *
     * @throws IllegalStateException if the azimuth and the distance have not been set.
     */
    void computeEndPoint() {
        if (!isCourseValid) {
            throw new IllegalStateException(Resources.format(Resources.Keys.AzimuthAndDistanceNotSet));
        }
        final double Δσ    = distance / radius;
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
        isEndPointValid = true;
    }

    /**
     * Computes the geodetic distance and azimuths from the start point and end point.
     * This method should be invoked if the distance or an azimuth is requested while
     * {@link #isCourseValid} is {@code false}.
     *
     * @throws IllegalStateException if the distance or azimuth has not been set.
     */
    private void computeCourse() {
        if (!isEndPointValid) {
            throw new IllegalStateException(Resources.format(Resources.Keys.EndPointNotSet));
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
        distance = radius * Δσ;
        isCourseValid = true;
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
        buffer.append(String.format(locale, " %f %s", distance, getDistanceUnit()));
        return buffer.toString();
    }
}
