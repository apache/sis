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
import java.util.Objects;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.NumberFormat;
import static java.lang.Math.*;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.Quantities;
import org.apache.sis.geometry.CoordinateFormat;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.operation.provider.MapProjection;
import org.apache.sis.referencing.internal.PositionTransformer;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.Bezier;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.referencing.privy.ShapeUtilities;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.io.TableAppender;
import static org.apache.sis.metadata.privy.ReferencingServices.NAUTICAL_MILE;
import static org.apache.sis.referencing.operation.provider.ModifiedAzimuthalEquidistant.LATITUDE_OF_ORIGIN;
import static org.apache.sis.referencing.operation.provider.ModifiedAzimuthalEquidistant.LONGITUDE_OF_ORIGIN;


/**
 * Performs geodetic calculations on a sphere or an ellipsoid. This class computes the distance between two points,
 * or conversely the point located at a given distance from another point when navigating in a given direction.
 * The distance depends on the path (or track) on Earth surface connecting the two points.
 * The track can be great circles (shortest path between two points) or rhumb lines (path with constant heading).
 *
 * <p>This class uses the following information:</p>
 * <ul>
 *   <li>The {@linkplain #setStartPoint(DirectPosition) start point}, which is always considered valid after the first call
 *     to {@code setStartPoint(…)}. Its value can only be changed by another call to {@code setStartPoint(…)}.</li>
 *   <li>One of the followings (the latest specified properties override other properties and determines what will be calculated):
 *     <ul>
 *       <li>the {@linkplain #setEndPoint(DirectPosition) end point}, or</li>
 *       <li>the {@linkplain #setStartingAzimuth(double) azimuth at start point} together with
 *           the {@linkplain #setGeodesicDistance(double) geodesic distance} from that point.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Algorithms</h2>
 * {@code GeodeticCalculator} uses two set of formulas, depending if the figure of the Earth
 * {@linkplain Ellipsoid#isSphere() is a sphere} or an ellipsoid.
 * Publications relevant to this class are:
 *
 * <ul>
 *   <li>Wikipedia, <a href="https://en.wikipedia.org/wiki/Great-circle_navigation">Great-circle navigation</a>
 *       for spherical formulas.</li>
 *   <li>Wikipedia, <a href="https://en.wikipedia.org/wiki/Rhumb_line">Rhumb line</a>
 *       for spherical formulas.</li>
 *   <li>Charles F. F. Karney (2013), <a href="https://doi.org/10.1007/s00190-012-0578-z">Algorithms for geodesics</a>
 *       for ellipsoidal formulas.</li>
 *   <li>G.G. Bennett, 1996. <a href="https://doi.org/10.1017/S0373463300013151">Practical Rhumb Line Calculations
 *       on the Spheroid</a> for ellipsoidal formulas.</li>
 *   <li>Charles F. F. Karney (2010), <a href="http://doi.org/10.5281/zenodo.32156">Test set for geodesics</a>
 *       for {@code GeodeticCalculator} tests.</li>
 *   <li>Charles F. F. Karney, <a href="https://geographiclib.sourceforge.io/">GeographicLib</a>
 *       for the reference implementation.</li>
 * </ul>
 *
 * <h2>Accuracy</h2>
 * {@code GeodeticCalculator} aims for a positional accuracy of one centimetre.
 * The accuracy is often better (about one millimetre), but not everywhere.
 * Azimuthal accuracy corresponds to an error of one centimetre at a distance of one kilometer,
 * except for nearly antipodal points (less than 1° of longitude and latitude from antipode)
 * and points close to the poles where the azimuthal errors are larger.
 * Karney's GeographicLib should be used if better accuracy is desired.
 * Apache SIS accuracy does not go as far as GeographicLib because the rest of Apache SIS
 * library (map projections, <i>etc.</i>) aims for an one centimetre accuracy anyway.
 *
 * <h2>Limitations</h2>
 * Current implementation cannot compute the geodesics in some cases.
 * In particular, calculation may fail for antipodal points on an ellipsoid.
 * Karney's algorithm should cover those cases,
 * but this {@code GeodeticCalculator} implementation may not be sufficiently tuned.
 * See <a href="https://issues.apache.org/jira/browse/SIS-467">SIS-467</a> for more information.
 *
 * <h2>Thread safety</h2>
 * This class is not thread-safe. If geodetic calculations are needed in a multi-threads environment,
 * then a distinct instance of {@code GeodeticCalculator} needs to be created for each thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 */
public class GeodeticCalculator {
    /**
     * Maximal difference (in radians) between two latitudes for enabling the use of simplified formulas.
     * This is used in two contexts:
     *
     * <ul>
     *   <li>Maximal difference between latitude φ₁ and equator for using the equatorial approximation.</li>
     *   <li>Maximal difference between |β₁| and |β₂| for enabling the use of Karney's equation 47.</li>
     * </ul>
     *
     * Those special cases are needed when general formulas produce indeterminations like 0/0.
     * Current angular value corresponds to a distance of 1 millimetre on a planet of the size
     * of Earth, which is about 1.57E-10 radians. This value is chosen empirically by trying to
     * minimize the number of "No convergence errors" reported by {@code GeodesicsOnEllipsoidTest}
     * in the {@code compareAgainstDataset()} method.
     *
     * <p><b>Note:</b> this is an angular tolerance threshold, but is also used with sine and cosine values
     * because sin(θ) ≈ θ for small angles.</p>
     */
    static final double LATITUDE_THRESHOLD = 0.001 / (NAUTICAL_MILE*60) * (PI/180);

    /**
     * The transform from user coordinates to geodetic coordinates used in computation.
     * This object also holds the following information:
     *
     * <ul>
     *   <li>{@link PositionTransformer#defaultCRS} is the default CRS for all methods receiving a
     *       {@link DirectPosition} argument if the given position does not specify its own CRS.</li>
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
     * Length of the semi-major axis. For a sphere, this is the radius of the sphere.
     */
    final double semiMajorAxis;

    /**
     * The (<var>latitude</var>, <var>longitude</var>) coordinates of the start point <strong>in radians</strong>.
     * This point is set by {@link #setStartGeographicPoint(double, double)}.
     *
     * @see #START_POINT
     */
    double φ1, λ1;

    /**
     * The (<var>latitude</var>, <var>longitude</var>) coordinates of the end point <strong>in radians</strong>.
     * This point is set by {@link #setEndGeographicPoint(double, double)}.
     *
     * @see #END_POINT
     */
    double φ2, λ2;

    /**
     * The azimuth at start point (α₁) and at end point (α₂) as vector components.
     * Angles can be obtained as below, with α a <em>geographic</em> (not arithmetic) angle:
     *
     * <ul>
     *   <li>Geographic angle: {@code atan2(msinα, mcosα)} gives the azimuth in radians between -π and +π
     *       with 0° pointing toward North and values increasing clockwise.</li>
     *   <li>Arithmetic angle: {@code atan2(mcosα, msinα)} (radians increasing anticlockwise).
     *       Obtained using the tan(π/2 − α) = 1/tan(α) identity.</li>
     * </ul>
     *
     * Sine and cosine of azimuth can are related to vector components as below:
     *
     * <ul>
     *   <li>m⋅sin(α) is proportional to a displacement in the λ direction.</li>
     *   <li>m⋅cos(α) is proportional to a displacement in the φ direction.
     *     The unit of measurement is the unit of any conformal projection.
     *     For representing a displacement in degrees, divide by {@linkplain #dφ_dy(double) ∂y/∂φ}.</li>
     * </ul>
     *
     * Those vectors may not be normalized to unitary vectors. For example, {@code msinα} is {@code sinα} multiplied
     * by an unknown constant <var>m</var>. It is often not needed to know <var>m</var> value because most formulas
     * are written in a way that cancel the magnitude. If nevertheless needed, normalization is applied by dividing
     * those fields by {@code m = hypot(msinα, mcosα)}.
     *
     * @see #STARTING_AZIMUTH
     * @see #ENDING_AZIMUTH
     */
    double msinα1, mcosα1, msinα2, mcosα2;

    /**
     * The shortest distance from the starting point ({@link #φ1},{@link #λ1}) to the end point ({@link #φ2},{@link #λ2}).
     * The distance is in the same units as ellipsoid axes and the azimuth is in radians.
     *
     * @see #GEODESIC_DISTANCE
     * @see #getDistanceUnit()
     */
    double geodesicDistance;

    /**
     * Length of the rhumb line from the starting point ({@link #φ1},{@link #λ1}) to the end point ({@link #φ2},{@link #λ2}).
     * The distance is in the same units as ellipsoid axes.
     *
     * @see #RHUMBLINE_LENGTH
     * @see #getDistanceUnit()
     */
    double rhumblineLength;

    /**
     * Constant bearing on the rhumb line path, in radians.
     *
     * @see #getConstantAzimuth()
     */
    double rhumblineAzimuth;

    /**
     * A bitmask specifying which information are valid. For example if the {@link #END_POINT} bit is not set,
     * then {@link #φ2} and {@link #λ2} need to be computed, which implies the computation of ∂φ/∂λ as well.
     * If the {@link #GEODESIC_DISTANCE} bit is not set, then {@link #geodesicDistance} needs to be computed,
     * which implies recomputation of ∂φ/∂λ as well.
     *
     * @see #isInvalid(int)
     * @see #setValid(int)
     */
    private int validity;

    /**
     * Bitmask specifying which information are valid.
     */
    static final int START_POINT = 1, END_POINT = 2, STARTING_AZIMUTH = 4, ENDING_AZIMUTH = 8,
            GEODESIC_DISTANCE = 16, RHUMBLINE_LENGTH = 32, COEFFICIENTS_FOR_START_POINT = 64;

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
     * @param  crs         the reference system for the {@link DirectPosition} arguments and return values.
     * @param  ellipsoid   ellipsoid associated to the geodetic component of given CRS.
     */
    GeodeticCalculator(final CoordinateReferenceSystem crs, final Ellipsoid ellipsoid) {
        final GeographicCRS geographic = ReferencingUtilities.toNormalizedGeographicCRS(crs, true, true);
        if (geographic == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalCRSType_1,
                    ReferencingUtilities.getInterface(CoordinateReferenceSystem.class, crs)));
        }
        this.ellipsoid = ellipsoid;
        semiMajorAxis  = ellipsoid.getSemiMajorAxis();
        userToGeodetic = new PositionTransformer(crs, geographic, null);
    }

    /**
     * Constructs a new geodetic calculator expecting coordinates in the supplied CRS.
     * All {@code GeodeticCalculator} methods having a {@link DirectPosition} argument
     * or return value will use that specified CRS.
     * That CRS is the value returned by {@link #getPositionCRS()}.
     *
     * @param  crs  the reference system for the {@link DirectPosition} objects.
     * @return a new geodetic calculator using the specified CRS.
     */
    public static GeodeticCalculator create(final CoordinateReferenceSystem crs) {
        final Ellipsoid ellipsoid = DatumOrEnsemble.getEllipsoid(Objects.requireNonNull(crs))
                .orElseThrow(() -> new IllegalArgumentException(Errors.format(Errors.Keys.IllegalCRSType_1,
                                ReferencingUtilities.getInterface(CoordinateReferenceSystem.class, crs))));
        if (ellipsoid.isSphere()) {
            return new GeodeticCalculator(crs, ellipsoid);
        } else {
            return new GeodesicsOnEllipsoid(crs, ellipsoid);
        }
    }

    /**
     * Returns {@code true} if at least one of the properties identified by the given mask is invalid.
     */
    final boolean isInvalid(final int mask) {
        return (validity & mask) != mask;
    }

    /**
     * Sets the properties specified by the given bitmask as valid.
     */
    final void setValid(final int mask) {
        validity |= mask;
    }

    /**
     * Returns the Coordinate Reference System (CRS) in which positions are represented, unless otherwise specified.
     * This is the CRS of all {@link DirectPosition} instances returned by methods in this class.
     * This is also the default CRS assumed by methods receiving a {@link DirectPosition} argument
     * when the given position does not specify its CRS.
     * This default CRS is specified at construction time.
     * It is not necessarily geographic; it may be projected or geocentric.
     *
     * @return the default CRS for {@link DirectPosition} instances.
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
        userToGeodetic.setCoordinate(0, toDegrees(φ));
        userToGeodetic.setCoordinate(1, toDegrees(λ));
        for (int i=userToGeodetic.getDimension(); --i >= 2;) {
            userToGeodetic.setCoordinate(i, 0);                   // Set height to ellipsoid surface.
        }
        return userToGeodetic;
    }

    /**
     * Returns the error message to give to {@link GeodeticException} when a {@link TransformException} occurred.
     *
     * @param  toCRS  {@code false} if converting from the position CRS, {@code true} if converting to the position CRS.
     */
    private String transformError(final boolean toCRS) {
        return Resources.format(Resources.Keys.CanNotConvertCoordinates_2,
                toCRS ? 1 : 0, IdentifiedObjects.getDisplayName(getPositionCRS(), null));
    }

    /**
     * Returns the starting point in the CRS specified at construction time.
     * This method returns the last point given to a {@code setStartPoint(…)} method,
     * transformed to the {@linkplain #getPositionCRS() position CRS}.
     *
     * @return the starting point represented in the CRS specified at construction time.
     * @throws IllegalStateException if the start point has not yet been specified.
     * @throws GeodeticException if the coordinates cannot be transformed to {@linkplain #getPositionCRS() position CRS}.
     *
     * @see #getEndPoint()
     */
    public DirectPosition getStartPoint() {
        if (isInvalid(START_POINT)) {
            throw new IllegalStateException(Resources.format(Resources.Keys.StartOrEndPointNotSet_1, 0));
        } else try {
            return geographic(φ1, λ1).inverseTransform();
        } catch (TransformException e) {
            throw new GeodeticException(transformError(true), e);
        }
    }

    /**
     * Sets the starting point as coordinates in arbitrary reference system. This method transforms the given
     * coordinates to geographic coordinates, then delegates to {@link #setStartGeographicPoint(double, double)}.
     * If the given point is not associated to a Coordinate Reference System (CRS), then this method assumes
     * the CRS specified at construction time.
     *
     * @param  point  the starting point in any coordinate reference system.
     * @throws IllegalArgumentException if the given coordinates cannot be transformed.
     *
     * @see #setEndPoint(DirectPosition)
     */
    public void setStartPoint(final DirectPosition point) {
        final DirectPosition p;
        try {
            p = userToGeodetic.transform(point);
        } catch (TransformException e) {
            throw new IllegalArgumentException(transformError(false), e);
        }
        setStartGeographicPoint(p.getOrdinate(0), p.getOrdinate(1));
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
     * @see #setEndGeographicPoint(double, double)
     * @see #moveToEndPoint()
     */
    public void setStartGeographicPoint(final double latitude, final double longitude) {
        ArgumentChecks.ensureFinite("latitude",  latitude);
        ArgumentChecks.ensureFinite("longitude", longitude);
        φ1 = toRadians(max(Latitude.MIN_VALUE, min(Latitude.MAX_VALUE, latitude)));
        λ1 = toRadians(longitude);
        validity = START_POINT;
    }

    /**
     * Returns or computes the destination in the CRS specified at construction time. This method returns
     * the point specified in the last call to a {@link #setEndPoint(DirectPosition) setEndPoint(…)} method,
     * unless the {@linkplain #setStartingAzimuth(double) starting azimuth} and
     * {@linkplain #setGeodesicDistance(double) geodesic distance} have been set more recently.
     * In the latter case, the end point will be computed from the {@linkplain #getStartPoint() start point}
     * and the current azimuth and distance.
     *
     * @return the destination (end point) represented in the CRS specified at construction time.
     * @throws IllegalStateException if the destination point, azimuth or distance have not been set.
     * @throws GeodeticException if the coordinates cannot be computed.
     *
     * @see #getStartPoint()
     */
    public DirectPosition getEndPoint() {
        if (isInvalid(END_POINT)) {
            computeEndPoint();
        }
        try {
            return geographic(φ2, λ2).inverseTransform();
        } catch (TransformException e) {
            throw new GeodeticException(transformError(true), e);
        }
    }

    /**
     * Sets the destination as coordinates in arbitrary reference system. This method transforms the given
     * coordinates to geographic coordinates, then delegates to {@link #setEndGeographicPoint(double, double)}.
     * If the given point is not associated to a Coordinate Reference System (CRS), then this method assumes
     * the CRS specified at construction time.
     *
     * @param  position  the destination (end point) in any coordinate reference system.
     * @throws IllegalArgumentException if the given coordinates cannot be transformed.
     *
     * @see #setStartPoint(DirectPosition)
     */
    public void setEndPoint(final DirectPosition position) {
        final DirectPosition p;
        try {
            p = userToGeodetic.transform(position);
        } catch (TransformException e) {
            throw new IllegalArgumentException(transformError(false), e);
        }
        setEndGeographicPoint(p.getOrdinate(0), p.getOrdinate(1));
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
     * @see #setStartGeographicPoint(double, double)
     */
    public void setEndGeographicPoint(final double latitude, final double longitude) {
        ArgumentChecks.ensureFinite("latitude",  latitude);
        ArgumentChecks.ensureFinite("longitude", longitude);
        φ2 = toRadians(max(Latitude.MIN_VALUE, min(Latitude.MAX_VALUE, latitude)));
        λ2 = toRadians(longitude);
        setValid(END_POINT);
        validity &= ~(STARTING_AZIMUTH | ENDING_AZIMUTH | GEODESIC_DISTANCE | RHUMBLINE_LENGTH | COEFFICIENTS_FOR_START_POINT);
        // Coefficients for starting point are invalidated because the starting azimuth changed.
    }

    /**
     * Returns or computes the angular heading at the starting point of a geodesic path.
     * Azimuth is relative to geographic North with values increasing clockwise.
     * This method returns the azimuth normalized to [-180 … +180]° range given in last call to
     * {@link #setStartingAzimuth(double)} method, unless the {@link #setEndPoint(DirectPosition) setEndPoint(…)}
     * method has been invoked more recently. In the latter case, the azimuth will be computed from the
     * {@linkplain #getStartPoint() start point} and the current end point.
     *
     * @return the azimuth in degrees from -180° to +180°. 0° is toward North and values are increasing clockwise.
     * @throws IllegalStateException if the end point, azimuth or distance have not been set.
     * @throws GeodeticException if the azimuth cannot be computed.
     */
    public double getStartingAzimuth() {
        if (isInvalid(STARTING_AZIMUTH)) {
            computeDistance();
        }
        return toDegrees(atan2(msinα1, mcosα1));
    }

    /**
     * Sets the angular heading at the starting point of a geodesic path.
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
        msinα1  = sin(azimuth);
        mcosα1  = cos(azimuth);
        setValid(STARTING_AZIMUTH);
        validity &= ~(END_POINT | ENDING_AZIMUTH | RHUMBLINE_LENGTH | COEFFICIENTS_FOR_START_POINT);
    }

    /**
     * Computes the angular heading at the ending point of a geodesic path.
     * Azimuth is relative to geographic North with values increasing clockwise.
     * This method computes the azimuth from the current {@linkplain #setStartPoint(DirectPosition) start point}
     * and {@linkplain #setEndPoint(DirectPosition) end point},
     * or from start point and the current {@linkplain #setStartingAzimuth(double) starting azimuth} and
     * {@linkplain #setGeodesicDistance(double) geodesic distance}.
     *
     * @return the azimuth in degrees from -180° to +180°. 0° is toward North and values are increasing clockwise.
     * @throws IllegalStateException if the destination point, azimuth or distance have not been set.
     * @throws GeodeticException if the azimuth cannot be computed.
     */
    public double getEndingAzimuth() {
        if (isInvalid(ENDING_AZIMUTH)) {
            if (isInvalid(END_POINT)) {
                computeEndPoint();                      // Compute also ending azimuth from start point and distance.
            } else {
                computeDistance();                      // Compute also ending azimuth from start point and end point.
            }
        }
        return toDegrees(atan2(msinα2, mcosα2));
    }

    /**
     * Computes the angular heading of a rhumb line path.
     * Azimuth is relative to geographic North with values increasing clockwise.
     *
     * @return the azimuth in degrees from -180° to +180°. 0° is toward North and values are increasing clockwise.
     * @throws IllegalStateException if the start point or end point has not been set.
     * @throws GeodeticException if the azimuth cannot be computed.
     */
    public double getConstantAzimuth() {
        if (isInvalid(RHUMBLINE_LENGTH)) {
            computeRhumbLine();
        }
        return toDegrees(rhumblineAzimuth);
    }

    /**
     * Returns or computes the shortest distance from start point to end point. This is sometimes called "great circle"
     * or "orthodromic" distance. This method returns the value given in last call to {@link #setGeodesicDistance(double)},
     * unless the {@link #setEndPoint(DirectPosition) setEndPoint(…)} method has been invoked more recently.
     * In the latter case, the distance will be computed from the {@linkplain #getStartPoint() start point}
     * and current end point.
     *
     * @return the shortest distance in the unit of measurement given by {@link #getDistanceUnit()}.
     * @throws IllegalStateException if the start point or end point has not been set.
     * @throws GeodeticException if the distance cannot be computed.
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
        setValid(GEODESIC_DISTANCE);
        validity &= ~(END_POINT | ENDING_AZIMUTH | RHUMBLINE_LENGTH);
    }

    /**
     * Returns or computes the length of rhumb line (part of constant heading) from start point to end point.
     * This is sometimes called "loxodrome". This is <strong>not</strong> the shortest path between two points.
     * The rhumb line distance may be up to 50% longer than the geodesic distance.
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
     * Computes (∂y/∂φ)⁻¹ where (∂y/∂φ) is the partial derivative of Northing values in a Mercator projection
     * at the given latitude on an ellipsoid with semi-major axis length of 1. There is no method for partial
     * derivative of Easting values since it is 1 everywhere. This derivative is cos(φ) on a sphere and close
     * but slightly different on an ellipsoid.
     *
     * @param  φ  the latitude in radians.
     * @return the northing derivative of a Mercator projection at the given latitude on an ellipsoid with a=1.
     *
     * @see org.apache.sis.referencing.operation.projection.ConformalProjection#dy_dφ
     */
    double dφ_dy(final double φ) {
        return cos(φ);
    }

    /**
     * Ensures that the start point and end point are set.
     * This method should be invoked at the beginning of {@link #computeDistance()}.
     *
     * @throws IllegalStateException if the start point or end point has not been set.
     */
    final void canComputeDistance() {
        if (isInvalid(START_POINT | END_POINT)) {
            throw new IllegalStateException(Resources.format(
                    Resources.Keys.StartOrEndPointNotSet_1, Integer.signum(validity & START_POINT)));
        }
    }

    /**
     * Computes the length of rhumb line from start point to end point.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Rhumb_line">Rhumb line on Wikipedia</a>
     *
     * @todo if {@literal Δλ > 180}, must split in two segments.
     */
    void computeRhumbLine() {
        canComputeDistance();
        final double Δλ = IEEEremainder(λ2 - λ1, 2*PI);
        final double Δφ = φ2 - φ1;
        final double factor;
        if (abs(Δφ) < LATITUDE_THRESHOLD) {
            factor = Δλ * cos((φ1 + φ2)/2);
            rhumblineAzimuth = copySign(PI/2, Δλ);
        } else {
            /*
             * Inverse of Gudermannian function is log(tan(π/4 + φ/2)).
             * The loxodrome formula involves the following difference:
             *
             *   ΔΨ  =  log(tan(π/4 + φ₁/2)) - log(tan(π/4 + φ₂/2))
             *       =  log(tan(π/4 + φ₁/2) / tan(π/4 + φ₂/2))
             *   α   =  atan2(Δλ, ΔΨ)
             *
             * Note that ΔΨ=0 if φ₁=φ₂, which implies cos(α)=0.
             * Code below replaces cos(α) by ΔΨ/hypot(Δλ, ΔΨ).
             */
            final double ΔΨ = log(tan(PI/4 + φ2/2) / tan(PI/4 + φ1/2));
            factor = Δφ / ΔΨ * hypot(Δλ, ΔΨ);
            rhumblineAzimuth = atan2(Δλ, ΔΨ);
        }
        rhumblineLength = semiMajorAxis * abs(factor);
        setValid(RHUMBLINE_LENGTH);
    }

    /**
     * Computes the geodesic distance and azimuths from the start point and end point.
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
     * @throws GeodeticException if an azimuth or the distance cannot be computed.
     */
    void computeDistance() {
        canComputeDistance();
        final double Δλ    = λ2 - λ1;           // No need to reduce to −π … +π range.
        final double sinΔλ = sin(Δλ);
        final double cosΔλ = cos(Δλ);
        final double sinφ1 = sin(φ1);
        final double cosφ1 = cos(φ1);
        final double sinφ2 = sin(φ2);
        final double cosφ2 = cos(φ2);

        final double cosφ1_sinφ2 = cosφ1 * sinφ2;
        final double cosφ2_sinφ1 = cosφ2 * sinφ1;
        msinα1 = cosφ2*sinΔλ;
        msinα2 = cosφ1*sinΔλ;
        mcosα1 = cosφ1_sinφ2 - cosφ2_sinφ1*cosΔλ;
        mcosα2 = cosφ1_sinφ2*cosΔλ - cosφ2_sinφ1;
        /*
         * Δσ = acos(sinφ₁⋅sinφ₂ + cosφ₁⋅cosφ₂⋅cosΔλ) is a first estimation inaccurate for small distances.
         * Δσ = atan2(…) computes the same value but with better accuracy.
         */
        double Δσ = sinφ1*sinφ2 + cosφ1*cosφ2*cosΔλ;        // Actually Δσ = acos(…).
        Δσ = atan2(hypot(msinα1, mcosα1), Δσ);              // Δσ ∈ [0…π].
        geodesicDistance = semiMajorAxis * Δσ;
        setValid(STARTING_AZIMUTH | ENDING_AZIMUTH | GEODESIC_DISTANCE);
    }

    /**
     * Ensures that the start point, starting azimuth and geodesic distance are set.
     * This method should be invoked at the beginning of {@link #computeEndPoint()}.
     *
     * @throws IllegalStateException if the start point, azimuth or distance has not been set.
     */
    final void canComputeEndPoint() {
        if (isInvalid(START_POINT | STARTING_AZIMUTH | GEODESIC_DISTANCE)) {
            throw new IllegalStateException(isInvalid(START_POINT)
                    ? Resources.format(Resources.Keys.StartOrEndPointNotSet_1, 0)
                    : Resources.format(Resources.Keys.AzimuthAndDistanceNotSet));
        }
    }

    /**
     * Computes the end point from the start point, the azimuth and the geodesic distance.
     * This method should be invoked if the end point or ending azimuth is requested while
     * {@link #END_POINT} validity flag is not set.
     *
     * <p>The default implementation computes {@link #φ2}, {@link #λ2} and ∂φ/∂λ derivatives using
     * spherical formulas. Subclasses should override if they can provide ellipsoidal formulas.</p>
     *
     * @throws IllegalStateException if the start point, azimuth or distance has not been set.
     * @throws GeodeticException if the end point or ending azimuth cannot be computed.
     */
    void computeEndPoint() {
        canComputeEndPoint();                   // May throw IllegalStateException.
        final double m     = hypot(msinα1, mcosα1);
        final double Δσ    = geodesicDistance / semiMajorAxis;
        final double sinΔσ = sin(Δσ);
        final double cosΔσ = cos(Δσ);
        final double sinφ1 = sin(φ1);
        final double cosφ1 = cos(φ1);
        final double sinα1 = msinα1 / m;        // α₁ is the azimuth at starting point as a geographic (not arithmetic) angle.
        final double cosα1 = mcosα1 / m;
        final double sinΔσ_cosα1 = sinΔσ * cosα1;
        final double Δλy   = sinΔσ * sinα1;
        final double Δλx   = cosΔσ*cosφ1 - sinφ1*sinΔσ_cosα1;
        final double Δλ    = atan2(Δλy, Δλx);
        final double sinφ2 = sinφ1*cosΔσ + cosφ1*sinΔσ_cosα1;       // First estimation of φ2.
        φ2     = atan(sinφ2 / hypot(Δλx, Δλy));                     // Improve accuracy close to poles.
        λ2     = IEEEremainder(λ1 + Δλ, 2*PI);
        mcosα2 = cosΔσ*cosα1 - sinφ1/cosφ1 * sinΔσ;
        msinα2 = sinα1;
        setValid(END_POINT | ENDING_AZIMUTH);
    }

    /**
     * Sets the start point and starting azimuth to the current end point and ending azimuth values.
     * The {@linkplain #getEndingAzimuth() ending azimuths}, the {@linkplain #getGeodesicDistance()
     * geodesic distance} and the {@linkplain #getEndPoint() end point} are discarded by this method call;
     * some of them will need to be specified again.
     *
     * @see #setStartGeographicPoint(double, double)
     * @throws GeodeticException if the end point or ending azimuth cannot be computed.
     */
    public void moveToEndPoint() {
        if (isInvalid(END_POINT)) {
            computeEndPoint();
        }
        φ1 = φ2;  mcosα1 = mcosα2;
        λ1 = λ2;  msinα1 = msinα2;
        /*
         * Following assumes that ENDING_AZIMUTH >>> 1 == STARTING_AZIMUTH.
         * This is verified by GeodeticCalculatorTest.testMoveToEndPoint().
         */
        validity = ((validity & ENDING_AZIMUTH) >>> 1) | START_POINT;
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
     * For example, a value of 1/10 of geodesic length may be sufficient.
     *
     * <h4>Dependency note</h4>
     * This method depends on the presence of {@code java.desktop} module. This constraint may be addressed
     * in a future Apache SIS version (see <a href="https://issues.apache.org/jira/browse/SIS-453">SIS-453</a>).
     * The "2D" suffix in the method name represents this relationship with Java2D.
     * The {@code createGeodesicPath(…)} method name (without suffix) is reserved for a future version
     * using ISO curves instead.
     *
     * @param  tolerance  maximal error between the approximated curve and actual geodesic track
     *                    in the units of measurement given by {@link #getDistanceUnit()}.
     *                    This is approximate; the actual errors may vary around that value.
     * @return an approximation of geodesic track as Bézier curves in a Java2D object.
     * @throws IllegalStateException if some required properties have not been specified.
     * @throws GeodeticException if some coordinates cannot be computed.
     */
    public Shape createGeodesicPath2D(final double tolerance) {
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
        } catch (TransformException e) {
            throw new GeodeticException(transformError(true), e);
        } finally {
            bezier.reset();
        }
        return ShapeUtilities.toPrimitive(path);
    }

    /**
     * Creates an approximation of the curve at a constant geodesic distance around the start point.
     * The returned shape is circlelike with the {@linkplain #getStartPoint() start point} in its center.
     * The coordinates are expressed in the coordinate reference system specified at creation time.
     * The approximation uses cubic Bézier curves.
     *
     * <div class="note"><b>Note:</b>
     * some authors define geodesic circle as the curve which enclose the maximum area for a given perimeter.
     * This method adopts a different definition, the locus of points at a fixed geodesic distance from center point.
     * </div>
     *
     * This method tries to stay within the given tolerance threshold of the geodesic track.
     * The {@code tolerance} parameter should not be too small for avoiding creation of unreasonably long chain of Bézier curves.
     * For example, a value of 1/10 of geodesic length may be sufficient.
     *
     * <h4>Dependency note</h4>
     * This method depends on the presence of {@code java.desktop} module. This constraint may be addressed
     * in a future Apache SIS version (see <a href="https://issues.apache.org/jira/browse/SIS-453">SIS-453</a>).
     * The "2D" suffix in the method name represents this relationship with Java2D.
     * The {@code createGeodesicCircle(…)} method name (without suffix) is reserved for a future version
     * using ISO curves instead.
     *
     * @param  tolerance  maximal error in the units of measurement given by {@link #getDistanceUnit()}.
     *                    This is approximate; the actual errors may vary around that value.
     * @return an approximation of circular region as a Java2D object.
     * @throws IllegalStateException if some required properties have not been specified.
     * @throws GeodeticException if some coordinates cannot be computed.
     */
    public Shape createGeodesicCircle2D(final double tolerance) {
        ArgumentChecks.ensureStrictlyPositive("tolerance", tolerance);
        if (isInvalid(START_POINT | GEODESIC_DISTANCE)) {
            computeDistance();
        }
        final CircularPath bezier = new CircularPath(tolerance);
        final Path2D path;
        try {
            path = bezier.build();
        } catch (TransformException e) {
            throw new GeodeticException(transformError(true), e);
        } finally {
            bezier.reset();
        }
        path.closePath();
        return path;
    }

    /**
     * Builds a geodesic path as a sequence of Bézier curves. The start point and end points are the points
     * in enclosing {@link GeodeticCalculator} at the time this class is instantiated. The start coordinates
     * given by {@link #φ1} and {@link #λ1} shall never change for this whole builder lifetime. However, the
     * end coordinates ({@link #φ2}, {@link #λ2}) will vary at each step.
     */
    private class PathBuilder extends Bezier {
        /**
         * The final (f) coordinates and derivatives, together with geodesic and loxodromic distances.
         * Saved for later restoration by {@link #reset()}.
         */
        private final double mcosαf, msinαf, φf, λf, distance, length;

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
            φf        = φ2;
            λf        = λ2;
            msinαf    = msinα2;
            mcosαf    = mcosα2;
            tolerance = toDegrees(εx / semiMajorAxis);
            distance  = geodesicDistance;
            length    = rhumblineLength;
            flags     = validity & (START_POINT | STARTING_AZIMUTH | END_POINT | ENDING_AZIMUTH |
                                    GEODESIC_DISTANCE | RHUMBLINE_LENGTH);
        }

        /**
         * Invoked for computing a new point on the Bézier curve. This method is invoked with a <var>t</var> value varying from
         * 0 (start point) to 1 (end point) inclusive. This method stores the coordinates in the {@link #point} array and the
         * derivative (∂y/∂x) in the {@linkplain #dx dx} and {@linkplain #dy dy} fields.
         *
         * @param  t  desired point on the curve, from 0 (start point) to 1 (end point) inclusive.
         * @throws TransformException if the point coordinates cannot be computed.
         */
        @Override
        protected void evaluateAt(final double t) throws TransformException {
            if (t == 0) {
                φ2 = φ1;  mcosα2 = mcosα1;          // Start point requested.
                λ2 = λ1;  msinα2 = msinα1;
            } else if (t == 1) {
                φ2 = φf;  mcosα2 = mcosαf;          // End point requested.
                λ2 = λf;  msinα2 = msinαf;
            } else {
                geodesicDistance = distance * t;
                setValid(GEODESIC_DISTANCE);
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
            if ((λ2 - λ1) * msinα1 < 0) {            // Reminder: Δλ or sin(α₁) may be zero.
                λ2 += 2*PI * signum(msinα1);         // We need λ₁ < λ₂ if heading east, or λ₁ > λ₂ if heading west.
            }
            final Matrix d = geographic(φ2, λ2).inverseTransform(point);    // `point` coordinates in user-specified CRS.
            final double dφ_dy = dφ_dy(φ2);                                 // ∂φ/∂y = cos(φ) for Mercator on a sphere of radius 1.
            final double m00 = d.getElement(0,0);
            final double m01 = d.getElement(0,1);
            final double m10 = d.getElement(1,0);
            final double m11 = d.getElement(1,1);
            double t = tolerance / dφ_dy;                                   // Tolerance for λ (second coordinate).
            εx = m00*tolerance + m01*t;                                     // Tolerance for x in user CRS.
            εy = m10*tolerance + m11*t;                                     // Tolerance for y in user CRS.
            /*
             * Return the tangent of the ending azimuth converted to the user CRS space. Note that if we draw the shape on
             * screen with (longitude, latitude) axes, the angles seen on screen are not the real angles measured on Earth.
             * In order to see the "real" angles, we need to draw the shape on a conformal projection such as Mercator.
             * Said otherwise, the angle value computed from the (dx,dy) vector is "real" only in a conformal projection.
             * Consequently, if the output CRS is a Mercator projection, then the angle computed from the (dx,dy) vector
             * at the end of this method should be the ending azimuth angle unchanged. We achieve this equivalence by
             * multiplying mcosα2 by a factor which will cancel the ∂y/∂φ factor of Mercator projection at that latitude.
             * Note that there is no need to scale msinα2 since ∂x/∂λ = 1 everywhere on Mercator projection with a=1.
             */
            t  = mcosα2 * dφ_dy;
            dx = m00*t + m01*msinα2;                                        // Reminder: coordinates in (φ,λ) order.
            dy = m10*t + m11*msinα2;
        }

        /**
         * Restores the enclosing {@link GeodeticCalculator} to the state that it has at {@code PathBuilder} instantiation time.
         */
        void reset() {
            λ2 = λf;  msinα2 = msinαf;
            φ2 = φf;  mcosα2 = mcosαf;
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
         * The initial (i) derivatives, saved for later restoration by {@link #reset()}.
         */
        private final double mcosαi, msinαi;

        /**
         * Creates a builder for the given tolerance in degrees at equator.
         */
        CircularPath(final double εx) {
            super(εx);
            msinαi = msinα1;
            mcosαi = mcosα1;
            forceCubic = true;
        }

        /**
         * Invoked for computing a new point on the circular path. This method is invoked with a <var>t</var> value varying from
         * 0 to 1 inclusive. The <var>t</var> value is multiplied by 2π for getting an angle. This method stores the coordinates
         * in the {@link #point} array and the derivative (∂y/∂x) in the {@linkplain #dx dx} and {@linkplain #dy dy} fields.
         *
         * @param  t  angle fraction from 0 to 1 inclusive.
         * @throws TransformException if the point coordinates cannot be computed.
         */
        @Override
        protected void evaluateAt(final double t) throws TransformException {
            final double α1 = t * (2*PI);
            msinα1 = sin(α1);
            mcosα1 = cos(α1);
            setValid(STARTING_AZIMUTH);
            validity &= ~COEFFICIENTS_FOR_START_POINT;
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
         * Restores the enclosing {@link GeodeticCalculator} to the state that it has at {@code PathBuilder} instantiation time.
         */
        @Override
        void reset() {
            msinα1 = msinαi;
            mcosα1 = mcosαi;
            super.reset();
        }
    }

    /**
     * The factory for map projections created by {@link #createProjectionAroundStart()}, fetched when first needed.
     * {@link DefaultMathTransformFactory#caching(boolean) Caching is disabled} on this factory because profiling
     * shows that {@link DefaultMathTransformFactory#unique(MathTransform)} consumes a lot of time when projections
     * are created frequently. Since each projection is specific to current {@linkplain #getStartPoint() start point},
     * they are unlikely to be shared anyway.
     *
     * @see #createProjectionAroundStart()
     */
    private DefaultMathTransformFactory projectionFactory;

    /**
     * The provider of "Azimuthal Equidistant (Spherical)" or "Modified Azimuthal Equidistant" projection.
     * Usually it is not necessary to keep a reference to the provider because {@link #projectionFactory}
     * finds them automatically. However, by keeping a reference to it, we save the search phase.
     *
     * @see #createProjectionAroundStart()
     */
    private MapProjection projectionProvider;

    /**
     * Parameters of the projection created by {@link #createProjectionAroundStart()}, saved for reuse when
     * new projection is requested. Only the "Latitude of natural origin" and "Longitude of natural origin"
     * parameter values will change for different projections.
     *
     * @see #createProjectionAroundStart()
     */
    private Parameters projectionParameters;

    /**
     * Conversion from {@linkplain #getPositionCRS() position CRS} to projection base CRS.
     * Computed when first needed. This transform does not change after creation.
     */
    private MathTransform toProjectionBase;

    /**
     * The operation method to use for creating a map projection. In the spherical case this is
     * "Azimuthal Equidistant (Spherical)". In the ellipsoidal case it become "Modified Azimuthal Equidistant".
     */
    String getProjectionMethod() {
        return "Azimuthal Equidistant (Spherical)";
    }

    /**
     * Creates an <cite>Azimuthal Equidistant</cite> projection centered on current starting point. On input,
     * the {@code MathTransform} expects coordinates expressed in the {@linkplain #getPositionCRS() position CRS}.
     * On output, the {@code MathTransform} produces coordinates in a {@link org.opengis.referencing.crs.ProjectedCRS}
     * having the following characteristics:
     *
     * <ul>
     *   <li>Coordinate system is a two-dimensional {@link org.opengis.referencing.cs.CartesianCS}
     *       with (Easting, Northing) axis order and directions.</li>
     *   <li>Unit of measurement is the same as {@linkplain #getPositionCRS() position CRS}
     *       if those units are linear, or {@link Units#METRE} otherwise.
     *   <li>Projection of the {@linkplain #getStartPoint() start point} results in (0,0).</li>
     *   <li>Distances relative to (0,0) are approximately exact for distances less than 800 km.</li>
     *   <li>Azimuths from (0,0) to other points are approximately exact for points located at less than 800 km.</li>
     * </ul>
     *
     * Given above characteristics, the following calculations are satisfying approximations when using
     * (<var>x</var>, <var>y</var>) coordinates in the output space for <var>D</var> &lt; 800 km:
     *
     * <blockquote>
     * <var>D</var> = √(<var>x</var>² + <var>y</var>²)  — distance from projection center.<br>
     * <var>θ</var> = atan2(<var>y</var>, <var>x</var>) — arithmetic angle from projection center to (<var>x</var>, <var>y</var>).<br>
     * <var>x</var> = <var>D</var>⋅cos <var>θ</var><br>
     * <var>y</var> = <var>D</var>⋅sin <var>θ</var>     — end point for a distance and angle from start point.<br>
     * </blockquote>
     *
     * The following calculations are <strong>not</strong> exacts, because distances and azimuths are approximately
     * exacts only when measured from (0,0) coordinates:
     *
     * <blockquote>
     * <var>D</var> = √[(<var>x₂</var> − <var>x₁</var>)² + (<var>y₂</var> − <var>y₁</var>)²]
     *          — distances between points other then projection center are not valid.<br>
     * <var>θ</var> = atan2(<var>y₂</var> − <var>y₁</var>, <var>x₂</var> − <var>x₁</var>)
     *          — azimuths between points other then projection center are not valid.<br>
     * <i>etc.</i>
     * </blockquote>
     *
     * This method can be invoked repetitively for doing calculations around different points.
     * All returned {@link MathTransform} instances are immutable;
     * changing {@code GeodeticCalculator} state does not affect those transforms.
     *
     * @return transform from {@linkplain #getPositionCRS() position CRS} to <i>Azimuthal Equidistant</i>
     *         projected CRS centered on current {@linkplain #getStartPoint() start point}.
     * @throws IllegalStateException if the start point has not been set.
     * @throws GeodeticException if the projection cannot be computed.
     *
     * @see #moveToEndPoint()
     *
     * @since 1.1
     */
    public MathTransform createProjectionAroundStart() {
        if (isInvalid(START_POINT)) {
            throw new IllegalStateException(Resources.format(Resources.Keys.StartOrEndPointNotSet_1, 0));
        }
        try {
            if (projectionParameters == null) {
                final CoordinateReferenceSystem positionCRS, baseCRS;
                final Unit<?>       crsUnit;
                final UnitConverter toLinearUnit;

                positionCRS           = getPositionCRS();
                baseCRS               = ReferencingUtilities.toNormalizedGeographicCRS(positionCRS, false, false);
                crsUnit               = ReferencingUtilities.getUnit(positionCRS);
                toLinearUnit          = ellipsoid.getAxisUnit().getConverterTo(Units.isLinear(crsUnit) ? crsUnit.asType(Length.class) : Units.METRE);
                toProjectionBase      = CRS.findOperation(positionCRS, baseCRS, null).getMathTransform();
                projectionFactory     = DefaultMathTransformFactory.provider().caching(false);
                projectionProvider    = (MapProjection) projectionFactory.getOperationMethod(getProjectionMethod());
                projectionParameters  = Parameters.castOrWrap(projectionProvider.getParameters().createValue());
                projectionParameters.parameter(Constants.SEMI_MAJOR).setValue(toLinearUnit.convert(ellipsoid.getSemiMajorAxis()));
                projectionParameters.parameter(Constants.SEMI_MINOR).setValue(toLinearUnit.convert(ellipsoid.getSemiMinorAxis()));
            }
            projectionParameters.getOrCreate(LATITUDE_OF_ORIGIN) .setValue(φ1, Units.RADIAN);
            projectionParameters.getOrCreate(LONGITUDE_OF_ORIGIN).setValue(λ1, Units.RADIAN);
            return MathTransforms.concatenate(toProjectionBase,
                    projectionProvider.createMathTransform(projectionFactory, projectionParameters));
        } catch (FactoryException e) {
            throw new GeodeticException(e.getMessage(), e);
        }
    }

    /**
     * Returns a string representation of start point, end point, azimuths and distance.
     * The text representation is implementation-specific and may change in any future version.
     * Current implementation is like below:
     *
     * <pre class="text">
     *   Coordinate reference system: Unspecified datum based upon the GRS 1980 Authalic Sphere
     *   ┌─────────────┬─────────────────┬──────────────────┬─────────────┐
     *   │             │    Latitude     │    Longitude     │   Azimuth   │
     *   │ Start point │  9°39′06.1120″N │ 132°37′37.1248″W │  -17°10′37″ │
     *   │ End point   │ 70°32′45.0206″N │ 109°50′05.0533″E │ -119°03′12″ │
     *   └─────────────┴─────────────────┴──────────────────┴─────────────┘
     *   Geodesic distance: 9,967,530.74 m</pre>
     *
     * @return a string representation of this calculator state.
     */
    @Override
    public String toString() {
        final StringBuilder buffer        = new StringBuilder();
        final Locale        locale        = Locale.getDefault();
        final Vocabulary    resources     = Vocabulary.forLocale(locale);
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
                final var azimuthFormat = new AngleFormat("DD°MM′SS″", locale);
                final var pointFormat = new CoordinateFormat(locale, null);
                pointFormat.setSeparator("\t");      // For distributing coordinate values on different columns.
                pointFormat.setDefaultCRS(crs);
                pointFormat.setGroundPrecision(Quantities.create(Formulas.LINEAR_TOLERANCE, Units.METRE));
                final var table = new TableAppender(buffer, " │ ");
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
                    } catch (IllegalStateException | GeodeticException e) {
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
                nf.setMaximumFractionDigits(Numerics.fractionDigitsForDelta(precision));
                resources.appendLabel(Vocabulary.Keys.GeodesicDistance, buffer);
                buffer.append(' ').append(nf.format(distance))
                      .append(' ').append(unit).append(lineSeparator);
            } catch (IllegalStateException | GeodeticException e) {
                // Ignore.
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);      // Should never happen since we are writting in a StringBuilder.
        }
        return buffer.toString();
    }
}
