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

import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.geometry.coordinate.Position;
import org.opengis.geometry.DirectPosition;

import org.apache.sis.measure.Latitude;
import org.apache.sis.internal.referencing.PositionTransformer;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;

import static java.lang.Math.*;


/**
 * Performs geodetic calculations on a sphere or an ellipsoid.
 * This class calculates the following properties:
 *
 * <ul>
 *   <li>Distance and azimuth between two points.</li>
 *   <li>Point located at a given distance and azimuth from another point.</li>
 * </ul>
 *
 * The calculation uses the following information:
 *
 * <ul>
 *   <li>The {@linkplain #setStartingPosition(Position) starting position}, which is always considered valid.
 *     It is initially set at (0,0) and can only be changed to another valid value.</li>
 *   <li>One of the following
 *     (the latest specified property overrides the other property and determines what will be calculated):
 *     <ul>
 *       <li>the {@linkplain #setDestinationPosition(Position) destination position}, or</li>
 *       <li>an {@linkplain #setDirection(double, double) azimuth and distance}.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * This class is not thread-safe. If geodetic calculations are needed in a multi-threads environment,
 * then a distinct instance of {@code GeodeticCalculator} needs to be created for each thread.
 *
 * @version 1.0
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
     * The (<var>latitude</var>, <var>longitude</var>) coordinates of the first point <strong>in radians</strong>.
     * This point is set by {@link #setStartingPosition(double, double)}.
     */
    private double φ1, λ1;

    /**
     * The (<var>latitude</var>, <var>longitude</var>) coordinates of the destination point <strong>in radians</strong>.
     * This point is set by {@link #setDestinationPosition(double, double)}.
     */
    private double φ2, λ2;

    /**
     * The distance and azimuth from the starting point ({@link #φ1},{@link #λ1}) to the destination point ({@link #φ2},{@link #λ2}).
     * The distance is in the same units than ellipsoid axes and the azimuth is in radians.
     */
    private double distance, azimuth;

    /**
     * Tells if the destination point is valid.
     * This is {@code false} if {@link #φ2} and {@link #λ2} need to be computed.
     */
    private boolean isDestinationValid;

    /**
     * Tells if the azimuth and the distance are valid.
     * This is {@code false} if {@link #distance} and {@link #azimuth} need to be computed.
     */
    private boolean isDirectionValid;

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
     * Returns the default CRS of {@link Position} instances. For every method expecting a {@code Position} argument,
     * if the {@linkplain DirectPosition#getCoordinateReferenceSystem() CRS of that position} is unspecified,
     * then the CRS given by this method is assumed.
     * Conversely every method returning a {@code Position} value will use this CRS.
     *
     * <p>This is the CRS specified at construction time.
     * This CRS is not necessarily geographic; it may be projected or geocentric.</p>
     *
     * @return the default CRS for {@link Position} instances.
     */
    public CoordinateReferenceSystem getPositionCRS() {
        return userToGeodetic.getCoordinateReferenceSystem();
    }

    /**
     * Returns the coordinate reference system for all methods expecting (φ,λ) as {@code double} values.
     * This CRS always use (<var>latitude</var>, <var>longitude</var>) axis order in degrees.
     *
     * @return the coordinate reference system of (φ,λ) coordinates.
     */
    public GeographicCRS getGeographicCRS() {
        return (GeographicCRS) userToGeodetic.defaultCRS;
    }

    /**
     * Sets the starting point as geographic coordinates. The azimuth and geodesic distance values
     * will be updated as a side effect of this call. They will be recomputed the next time that
     * {@link #getAzimuth()} or {@link #getGeodesicDistance()} are invoked.
     *
     * @param  latitude   the latitude in decimal degrees between -90 and  +90°
     * @param  longitude  the longitude in decimal degrees.
     * @throws IllegalArgumentException if the latitude is out of bounds.
     */
    public void setStartingPosition(final double latitude, final double longitude) {
        ArgumentChecks.ensureBetween("latitude", Latitude.MIN_VALUE, Latitude.MAX_VALUE, latitude);
        ArgumentChecks.ensureFinite("longitude", longitude);
        φ1 = toRadians(latitude);
        λ1 = toRadians(longitude);
        isDestinationValid = false;
        isDirectionValid   = false;
    }

    /**
     * Sets the starting point in any CRS. The coordinates will be transformed to geographic coordinates and
     * given to {@link #setStartingPosition(double, double)}. If the given point is not associated to a CRS,
     * then the CRS specified at construction time is assumed.
     *
     * @param  position  the starting point in any coordinate reference system.
     * @throws TransformException if the coordinates can not be transformed.
     */
    public void setStartingPosition(final Position position) throws TransformException {
        final DirectPosition p = userToGeodetic.transform(position.getDirectPosition());
        setStartingPosition(p.getOrdinate(0), p.getOrdinate(1));
    }

    /**
     * Returns the starting point in the CRS specified at construction time.
     *
     * @return the starting point in user CRS.
     * @throws TransformException if the coordinates can not be transformed to user CRS.
     */
    public DirectPosition getStartingPosition() throws TransformException {
        userToGeodetic.setOrdinate(0, toDegrees(φ1));
        userToGeodetic.setOrdinate(1, toDegrees(λ1));
        return userToGeodetic.inverseTransform();
    }

    /**
     * Sets the destination point as geographic coordinates. The azimuth and geodesic distance values
     * will be updated as a side effect of this call. They will be recomputed the next time that
     * {@link #getAzimuth()} or {@link #getGeodesicDistance()} are invoked.
     *
     * @param  latitude   the latitude in decimal degrees between -90 and  +90°
     * @param  longitude  the longitude in decimal degrees.
     * @throws IllegalArgumentException if the latitude is out of bounds.
     */
    public void setDestinationPosition(final double latitude, final double longitude) {
        ArgumentChecks.ensureBetween("latitude", Latitude.MIN_VALUE, Latitude.MAX_VALUE, latitude);
        ArgumentChecks.ensureFinite("longitude", longitude);
        φ2 = toRadians(latitude);
        λ2 = toRadians(longitude);
        isDestinationValid = false;
        isDirectionValid   = false;
    }

    /**
     * Sets the destination point in any CRS. The coordinates will be transformed to geographic coordinates and
     * given to {@link #setDestinationPosition(double, double)}. If the given point is not associated to a CRS,
     * then the CRS specified at construction time is assumed.
     *
     * @param  position  the destination point in any coordinate reference system.
     * @throws TransformException if the coordinates can not be transformed.
     */
    public void setDestinationPosition(final Position position) throws TransformException {
        final DirectPosition p = userToGeodetic.transform(position.getDirectPosition());
        setDestinationPosition(p.getOrdinate(0), p.getOrdinate(1));
    }
}
