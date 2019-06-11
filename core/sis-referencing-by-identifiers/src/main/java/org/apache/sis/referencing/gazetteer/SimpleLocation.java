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
package org.apache.sis.referencing.gazetteer;

import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.coordinate.Position;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.gazetteer.LocationType;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.resources.Errors;


/**
 * A location described by an unmodifiable direct position that defines the centroid of an envelope.
 * This class encompasses most information in a single object, which make it lightweight to create
 * (less pressure on the garbage collector). However this is not a clear separation of responsibility,
 * so this class should not be in public API.
 *
 * <p>Subclasses <strong>must</strong> override the following methods if the above coordinate reference
 * system is not a geographic CRS with (<var>longitude</var>, <var>latitude</var>) axes in degrees:</p>
 * <ul>
 *   <li>{@link #getCoordinateReferenceSystem()}</li>
 *   <li>{@link #getWestBoundLongitude()}</li>
 *   <li>{@link #getEastBoundLongitude()}</li>
 *   <li>{@link #getSouthBoundLatitude()}</li>
 *   <li>{@link #getNorthBoundLatitude()}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
class SimpleLocation extends AbstractLocation implements DirectPosition, Envelope, GeographicBoundingBox {
    /**
     * The westernmost bound of the envelope, or {@code NaN} if not yet computed.
     *
     * @see #getLowerCorner()
     * @see #getWestBoundLongitude()
     */
    protected double minX = Double.NaN;

    /**
     * The southernmost bound of the envelope, or {@code NaN} if not yet computed.
     *
     * @see #getLowerCorner()
     * @see #getSouthBoundLatitude()
     */
    protected double minY = Double.NaN;

    /**
     * The easternmost bound of the envelope, or {@code NaN} if not yet computed.
     *
     * @see #getUpperCorner()
     * @see #getEastBoundLongitude()
     */
    protected double maxX = Double.NaN;

    /**
     * The northernmost bound of the envelope, or {@code NaN} if not yet computed.
     *
     * @see #getUpperCorner()
     * @see #getNorthBoundLatitude()
     */
    protected double maxY = Double.NaN;

    /**
     * Creates a new location for the given geographic identifier.
     * This constructor accepts {@code null} arguments, but this is not recommended.
     *
     * @param type        the description of the nature of this geographic identifier.
     * @param identifier  the geographic identifier to be returned by {@link #getGeographicIdentifier()}.
     */
    SimpleLocation(final LocationType type, final CharSequence identifier) {
        super(type, identifier);
    }

    /**
     * Returns a description of the location instance.
     * In this simple implementation, this instance is its own geographic extent.
     */
    @Override
    public GeographicExtent getGeographicExtent() {
        return this;
    }

    /**
     * Returns an envelope that encompass the location.
     * In this simple implementation, this instance is its own envelope.
     */
    @Override
    public final Envelope getEnvelope() {
        return this;
    }

    /**
     * Returns coordinates of a centroid point for the location instance.
     * In this simple implementation, this instance is its own centroid coordinate.
     */
    @Override
    public final Position getPosition() {
        return this;
    }

    /**
     * Returns the direct position, which is itself.
     */
    @Override
    public final DirectPosition getDirectPosition() {
        return this;
    }

    /**
     * Returns the coordinate reference system the envelope and the position.
     * Default implementation returns {@link CommonCRS#defaultGeographic()}.
     * Subclasses must override this method if they use another CRS.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return CommonCRS.defaultGeographic();
    }

    /**
     * Returns the number of dimensions, which is fixed to 2.
     */
    @Override
    public final int getDimension() {
        return 2;
    }

    /**
     * Returns the coordinates of the centroid.
     */
    @Override
    public final double[] getCoordinate() {
        return new double[] {getOrdinate(0), getOrdinate(1)};
    }

    /**
     * Returns the centroid coordinate value for the specified dimension.
     */
    @Override
    public final double getOrdinate(final int dimension) {
        return getMedian(dimension);
    }

    /**
     * Returns the minimal coordinate value for the specified dimension.
     */
    @Override
    public final double getMinimum(final int dimension) {
        switch (dimension) {
            case 0:  return minX;
            case 1:  return minY;
            default: throw new IndexOutOfBoundsException(indexOutOfBounds(dimension));
        }
    }

    /**
     * Returns the maximal coordinate value for the specified dimension.
     */
    @Override
    public final double getMaximum(final int dimension) {
        switch (dimension) {
            case 0:  return maxX;
            case 1:  return maxY;
            default: throw new IndexOutOfBoundsException(indexOutOfBounds(dimension));
        }
    }

    /**
     * Returns the median coordinate value for the specified dimension.
     */
    @Override
    public final double getMedian(final int dimension) {
        switch (dimension) {
            case 0:  return (minX + maxX) / 2;
            case 1:  return (minY + maxY) / 2;
            default: throw new IndexOutOfBoundsException(indexOutOfBounds(dimension));
        }
    }

    /**
     * Returns the envelope width or height along the specified dimension.
     */
    @Override
    public final double getSpan(final int dimension) {
        switch (dimension) {
            case 0:  return (maxX - minX);
            case 1:  return (maxY - minY);
            default: throw new IndexOutOfBoundsException(indexOutOfBounds(dimension));
        }
    }

    /**
     * Returns the error message for an index out of bounds.
     */
    private static String indexOutOfBounds(final int dimension) {
        return Errors.format(Errors.Keys.IndexOutOfBounds_1, dimension);
    }

    /**
     * Returns a copy of the lower-left corner.
     */
    @Override
    public final DirectPosition getLowerCorner() {
        return new DirectPosition2D(getCoordinateReferenceSystem(), minX, minY);
    }

    /**
     * Returns a copy of the upper-right corner.
     */
    @Override
    public final DirectPosition getUpperCorner() {
        return new DirectPosition2D(getCoordinateReferenceSystem(), maxX, maxY);
    }

    /**
     * Do not allow modification of the direct position.
     */
    @Override
    public final void setOrdinate(int dimension, double value) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, DirectPosition.class));
    }

    /**
     * Indication of whether the bounding polygon encompasses an area covered by the data
     * (<cite>inclusion</cite>) or an area where data is not present (<cite>exclusion</cite>).
     * The value is fixed to {@code true}.
     */
    @Override
    public final Boolean getInclusion() {
        return Boolean.TRUE;
    }

    /**
     * Returns the westernmost longitude in degrees. Default implementation assumes that the
     * {@linkplain #getCoordinateReferenceSystem() coordinate reference system} is geographic
     * with (<var>longitude</var>, <var>latitude</var>) axes in degrees.
     * Subclasses <strong>must</strong> override if this is not the case.
     */
    @Override
    public double getWestBoundLongitude() {
        return minX;
    }

    /**
     * Returns the easternmost longitude in degrees. Default implementation assumes that the
     * {@linkplain #getCoordinateReferenceSystem() coordinate reference system} is geographic
     * with (<var>longitude</var>, <var>latitude</var>) axes in degrees.
     * Subclasses <strong>must</strong> override if this is not the case.
     */
    @Override
    public double getEastBoundLongitude() {
        return maxX;
    }

    /**
     * Returns the southernmost latitude in degrees. Default implementation assumes that the
     * {@linkplain #getCoordinateReferenceSystem() coordinate reference system} is geographic
     * with (<var>longitude</var>, <var>latitude</var>) axes in degrees.
     * Subclasses <strong>must</strong> override if this is not the case.
     */
    @Override
    public double getSouthBoundLatitude() {
        return minY;
    }

    /**
     * Returns the northernmost latitude in degrees. Default implementation assumes that the
     * {@linkplain #getCoordinateReferenceSystem() coordinate reference system} is geographic
     * with (<var>longitude</var>, <var>latitude</var>) axes in degrees.
     * Subclasses <strong>must</strong> override if this is not the case.
     */
    @Override
    public double getNorthBoundLatitude() {
        return maxY;
    }

    /**
     * A {@code SimpleLocation} for non-geographic CRS.
     * Subclasses should invoke {@link #computeGeographicBoundingBox(MathTransform)} after the
     * {@link #minX}, {@link #minY}, {@link #maxX} and {@link #maxY} fields have been set.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 0.8
     * @since   0.8
     * @module
     */
    abstract static class Projected extends SimpleLocation implements GeographicBoundingBox {
        /**
         * The western-most coordinate of the limit of the dataset extent.
         * The value is expressed in longitude in decimal degrees (positive east).
         */
        protected double westBoundLongitude = Double.NaN;

        /**
         * The eastern-most coordinate of the limit of the dataset extent.
         * The value is expressed in longitude in decimal degrees (positive east).
         */
        protected double eastBoundLongitude = Double.NaN;

        /**
         * The southern-most coordinate of the limit of the dataset extent.
         * The value is expressed in latitude in decimal degrees (positive north).
         */
        protected double southBoundLatitude = Double.NaN;

        /**
         * The northern-most, coordinate of the limit of the dataset extent.
         * The value is expressed in latitude in decimal degrees (positive north).
         */
        protected double northBoundLatitude = Double.NaN;

        /**
         * Creates a new location for the given geographic identifier.
         * This constructor accepts {@code null} arguments, but this is not recommended.
         *
         * @param type        the description of the nature of this geographic identifier.
         * @param identifier  the geographic identifier to be returned by {@link #getGeographicIdentifier()}.
         */
        Projected(final LocationType type, final CharSequence identifier) {
            super(type, identifier);
        }

        /**
         * Returns the western-most coordinate of the limit of the dataset extent.
         * The value is expressed in longitude in decimal degrees (positive east).
         */
        @Override
        public final double getWestBoundLongitude() {
            return westBoundLongitude;
        }

        /**
         * Returns the eastern-most coordinate of the limit of the dataset extent.
         * The value is expressed in longitude in decimal degrees (positive east).
         */
        @Override
        public final double getEastBoundLongitude() {
            return eastBoundLongitude;
        }

        /**
         * Returns the southern-most coordinate of the limit of the dataset extent.
         * The value is expressed in latitude in decimal degrees (positive north).
         */
        @Override
        public final double getSouthBoundLatitude()  {
            return southBoundLatitude;
        }

        /**
         * Returns the northern-most, coordinate of the limit of the dataset extent.
         * The value is expressed in latitude in decimal degrees (positive north).
         */
        @Override
        public final double getNorthBoundLatitude()   {
            return northBoundLatitude;
        }

        /**
         * Computes the geographic bounding box from the current values of {@link #minX}, {@link #minY}, {@link #maxX}
         * and {@link #maxY} fields. This method performs a work similar to the {@code Envelopes.transform(…)} methods
         * but using a much simpler (and faster) algorithm: this method projects only the 4 corners, without any check
         * for the number of dimensions, projection of median coordinates,  use of projection derivatives for locating
         * the envelope extremum or special checks for polar cases. This method is okay only when the current envelope
         * is the envelope of a cell of a grid that divide the projection area in a very regular way (for example with
         * the guarantee that the projection central meridian will never be in the middle of grid cell, <i>etc</i>).
         *
         * <p>If a geographic bounding box was already defined before invoking this method, then it will be expanded
         * (if needed) for encompassing the bounding box computed by this method.</p>
         *
         * @param  inverse  the transform from projected coordinates to geographic coordinates.
         * @throws TransformException if a coordinate operation failed.
         *
         * @see org.apache.sis.geometry.Envelopes#transform(MathTransform, Envelope)
         */
        final void computeGeographicBoundingBox(final MathTransform inverse) throws TransformException {
            final double[] points = new double[] {
                minX, minY,
                minX, maxY,
                maxX, minY,
                maxX, maxY
            };
            inverse.transform(points, 0, points, 0, 4);
            for (int i=0; i < points.length;) {
                final double φ = points[i++];
                final double λ = points[i++];
                if (Double.isNaN(φ) || Double.isNaN(λ)) {
                    throw new TransformException(Errors.format(Errors.Keys.CanNotTransformEnvelope));
                }
                if (!(φ >= southBoundLatitude)) southBoundLatitude = φ;     // Use '!' for accepting NaN.
                if (!(φ <= northBoundLatitude)) northBoundLatitude = φ;
                if (!(λ >= westBoundLongitude)) westBoundLongitude = λ;
                if (!(λ <= eastBoundLongitude)) eastBoundLongitude = λ;
            }
        }

        /**
         * Clips the geographic bounding box to the given area. If the bounding box changed as a result of this method
         * call, then caller should consider invoking {@link #clipProjectedEnvelope(MathTransform, double, double)}.
         *
         * @return whether the geographic bounding box changed as a result of this method call.
         */
        final boolean clipGeographicBoundingBox(final double λmin, final double φmin, final double λmax, final double φmax) {
            boolean changed = false;
            if (westBoundLongitude < λmin) {westBoundLongitude = λmin; changed = true;}
            if (eastBoundLongitude > λmax) {eastBoundLongitude = λmax; changed = true;}
            if (southBoundLatitude < φmin) {southBoundLatitude = φmin; changed = true;}
            if (northBoundLatitude > φmax) {northBoundLatitude = φmax; changed = true;}
            return changed;
        }

        /**
         * Projects the geographic bounding box and clips the current envelope to the result of that projection.
         * This method should be invoked when {@link #clipGeographicBoundingBox(double, double, double, double)}
         * returned {@code true}.
         *
         * @param  forward  the transform from geographic coordinates to projected coordinates.
         * @param  tx       tolerance threshold in easting  values for changing {@link #minX} or {@link #maxX}.
         * @param  ty       tolerance threshold in northing values for changing {@link #minY} or {@link #maxY}.
         * @throws TransformException if a coordinate operation failed.
         */
        final void clipProjectedEnvelope(final MathTransform forward, final double tx, final double ty) throws TransformException {
            final double[] points = new double[] {
                southBoundLatitude, westBoundLongitude,
                northBoundLatitude, westBoundLongitude,
                southBoundLatitude, eastBoundLongitude,
                northBoundLatitude, eastBoundLongitude
            };
            forward.transform(points, 0, points, 0, 4);
            double xmin, ymin, xmax, ymax;
            xmin = xmax = points[0];
            ymin = ymax = points[1];
            for (int i=2; i < points.length;) {
                final double x = points[i++];
                final double y = points[i++];
                if (x < xmin) xmin = x;
                if (x > xmax) xmax = x;
                if (y < ymin) ymin = y;
                if (y > ymax) ymax = y;
            }
            if (xmin > minX + tx) minX = xmin;
            if (xmax < maxX - tx) maxX = xmax;
            if (ymin > minY + ty) minY = ymin;
            if (ymax < maxY - ty) maxY = ymax;
        }
    }

    /**
     * Converts the current envelope using the given math transform.
     * The given transform usually performs nothing more than axis swapping or unit conversions.
     *
     * @param  mt      the math transform to use for conversion.
     * @param  buffer  a temporary buffer of length 8 or more.
     * @throws TransformException if an error occurred while converting the points.
     */
    final void convert(final MathTransform mt, final double[] buffer) throws TransformException {
        buffer[3] = buffer[7] = maxY;
        buffer[4] = buffer[6] = maxX;
        buffer[1] = buffer[5] = minY;
        buffer[0] = buffer[2] = minX;
        minX = maxX = minY = maxY = Double.NaN;
        mt.transform(buffer, 0, buffer, 0, 4);
        for (int i=0; i<8;) {
            final double x = buffer[i++];
            final double y = buffer[i++];
            if (Double.isNaN(x) || Double.isNaN(y)) {
                throw new TransformException(Errors.format(Errors.Keys.CanNotTransformEnvelope));
            }
            if (!(x >= minX)) minX = x;     // Use '!' for accepting NaN.
            if (!(x <= maxX)) maxX = x;
            if (!(y >= minY)) minY = y;
            if (!(y <= maxY)) maxY = y;
        }
    }
}
