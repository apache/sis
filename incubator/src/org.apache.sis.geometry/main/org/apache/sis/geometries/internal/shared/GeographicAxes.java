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
package org.apache.sis.geometries.internal.shared;

import javax.measure.UnitConverter;
import javax.measure.quantity.Angle;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.datum.DatumOrEnsemble;


/**
 * Longitude and latitude axes of a reference system, with conversions between their values and degrees.
 * <p>
 * Values in degrees are positive east and north, whatever the axis directions and units.
 * <ul>
 *   <li>Only the axes of a geographic component are supported. Other ellipsoidal or spherical
 *       coordinate systems, such as rotated poles, are rejected.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
final class GeographicAxes {

    /**
     * Number of degrees in a full turn.
     */
    private static final double TURN = 360;

    /**
     * The geographic component of the reference system.
     */
    private final GeographicCRS geographic;

    private final int longitudeDimension;
    private final int latitudeDimension;
    private final CoordinateSystemAxis longitudeAxis;
    private final UnitConverter longitudeToDegrees;
    private final UnitConverter latitudeToDegrees;
    private final UnitConverter degreesToLongitude;
    private final UnitConverter degreesToLatitude;

    /**
     * Whether the longitude axis is oriented west.
     */
    private final boolean westward;

    /**
     * Whether the latitude axis is oriented south.
     */
    private final boolean southward;

    /**
     * Locates the axes of the given geographic component.
     *
     * @param  geographic  the geographic component.
     * @param  offset      index of the first dimension of the component in the full reference system.
     */
    private GeographicAxes(final GeographicCRS geographic, final int offset) {
        this.geographic = geographic;
        final CoordinateSystem cs = geographic.getCoordinateSystem();
        int longitude = -1;
        int latitude  = -1;
        for (int i = 0; i < cs.getDimension(); i++) {
            final AxisDirection direction = cs.getAxis(i).getDirection();
            if (direction == AxisDirection.EAST || direction == AxisDirection.WEST) {
                longitude = i;
            } else if (direction == AxisDirection.NORTH || direction == AxisDirection.SOUTH) {
                latitude = i;
            }
        }
        if (longitude < 0 || latitude < 0) {
            throw new UnsupportedOperationException("No longitude and latitude axes in " + geographic.getName() + '.');
        }
        longitudeAxis      = cs.getAxis(longitude);
        final CoordinateSystemAxis latitudeAxis = cs.getAxis(latitude);
        longitudeDimension = offset + longitude;
        latitudeDimension  = offset + latitude;
        westward           = longitudeAxis.getDirection() == AxisDirection.WEST;
        southward          = latitudeAxis.getDirection()  == AxisDirection.SOUTH;
        longitudeToDegrees = longitudeAxis.getUnit().asType(Angle.class).getConverterTo(Units.DEGREE);
        latitudeToDegrees  = latitudeAxis .getUnit().asType(Angle.class).getConverterTo(Units.DEGREE);
        degreesToLongitude = longitudeToDegrees.inverse();
        degreesToLatitude  = latitudeToDegrees.inverse();
    }

    /**
     * Returns the geographic axes of the given reference system.
     *
     * @param  crs        the reference system, or {@code null}.
     * @param  curveType  name of the curve type, for the error message.
     * @return the axes, or {@code null} if the reference system has no ellipsoidal or spherical axes.
     * @throws UnsupportedOperationException if the ellipsoidal or spherical axes are not those of a geographic component.
     */
    static GeographicAxes of(final CoordinateReferenceSystem crs, final String curveType) {
        if (crs == null) {
            return null;
        }
        int offset = 0;
        for (final SingleCRS component : CRS.getSingleComponents(crs)) {
            final CoordinateSystem cs = component.getCoordinateSystem();
            if (cs instanceof EllipsoidalCS || cs instanceof SphericalCS) {
                if (component instanceof GeographicCRS geographic && cs instanceof EllipsoidalCS) {
                    return new GeographicAxes(geographic, offset);
                }
                throw new UnsupportedOperationException(curveType + " curves in " + component.getName()
                        + " are not supported yet.");
            }
            offset += cs.getDimension();
        }
        return null;
    }

    /**
     * Returns the geographic component of the reference system.
     */
    GeographicCRS geographic() {
        return geographic;
    }

    /**
     * Returns the ratio of the semi-minor axis to the semi-major axis of the ellipsoid, which is 1 − flattening.
     */
    double axisRatio() {
        final Ellipsoid ellipsoid = DatumOrEnsemble.getEllipsoid(geographic).orElseThrow(
                () -> new UnsupportedOperationException("No ellipsoid in " + geographic.getName() + '.'));
        return ellipsoid.getSemiMinorAxis() / ellipsoid.getSemiMajorAxis();
    }

    /**
     * Returns the longitude of the given position, in degrees east.
     */
    double longitude(final Vector<?> position) {
        final double value = longitudeToDegrees.convert(position.get(longitudeDimension));
        return westward ? -value : value;
    }

    /**
     * Returns the latitude of the given position, in degrees north.
     */
    double latitude(final Vector<?> position) {
        final double value = latitudeToDegrees.convert(position.get(latitudeDimension));
        return southward ? -value : value;
    }

    /**
     * Starts the bounds of a curve at its first data point.
     * A first point on a pole takes the longitude of the first point outside the poles.
     *
     * @param  points  data points of the curve, not empty.
     * @return bounds containing the first point.
     */
    HorizontalBounds start(final DataPoints points) {
        final double latitude = latitude(points.getPosition(0));
        double longitude = longitude(points.getPosition(0));
        for (int i = 0, n = points.size(); i < n; i++) {
            final Vector<?> position = points.getPosition(i);
            if (!HorizontalBounds.isPole(latitude(position))) {
                longitude = longitude(position);
                break;
            }
        }
        return new HorizontalBounds(longitude, latitude);
    }

    /**
     * Returns the envelope of data points with the longitude and latitude ranges replaced by the given bounds.
     * <p>
     * The longitude range is fitted in the range of the longitude axis. It crosses the anti-meridian
     * (lower &gt; upper) when the bounds do and the axis wraps around. Otherwise it covers the whole axis.
     *
     * @param  pointsEnvelope  envelope of the data points, which gives the ranges of the other dimensions.
     * @param  bounds          longitude and latitude ranges swept by the curve.
     * @return envelope of the curve.
     */
    GeneralEnvelope toEnvelope(final Envelope pointsEnvelope, final HorizontalBounds bounds) {
        final GeneralEnvelope envelope = new GeneralEnvelope(pointsEnvelope);
        final double south = degreesToLatitude.convert(southward ? -bounds.north() : bounds.south());
        final double north = degreesToLatitude.convert(southward ? -bounds.south() : bounds.north());
        envelope.setRange(latitudeDimension, Math.min(south, north), Math.max(south, north));

        final double turn = Math.abs(degreesToLongitude.convert(TURN) - degreesToLongitude.convert(0));
        double lower = degreesToLongitude.convert(westward ? -bounds.east() : bounds.west());
        double upper = degreesToLongitude.convert(westward ? -bounds.west() : bounds.east());
        final double axisMinimum = longitudeAxis.getMinimumValue();
        final double axisMaximum = longitudeAxis.getMaximumValue();
        final boolean wraps = longitudeAxis.getRangeMeaning() == RangeMeaning.WRAPAROUND
                && Double.isFinite(axisMinimum) && Double.isFinite(axisMaximum);
        if (bounds.spansAllLongitudes()) {
            if (wraps) {
                lower = axisMinimum;
                upper = axisMaximum;
            }
        } else if (wraps) {
            double span = upper - lower;
            if (span < 0) span += turn;
            lower -= turn * Math.floor((lower - axisMinimum) / turn);
            upper  = lower + span;
            if (upper > axisMinimum + turn) upper -= turn;
        }
        if (lower > upper && !wraps) {
            lower = Math.min(degreesToLongitude.convert(-TURN / 2), degreesToLongitude.convert(TURN / 2));
            upper = Math.max(degreesToLongitude.convert(-TURN / 2), degreesToLongitude.convert(TURN / 2));
        }
        envelope.setRange(longitudeDimension, lower, upper);
        return envelope;
    }
}
