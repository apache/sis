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
package org.apache.sis.image;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.image.ImagingOpException;
import static java.lang.Math.abs;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform2D;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.system.Configuration;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.io.wkt.Formatter;


/**
 * A grid of precomputed pixel coordinates in source images. This grid is used during
 * image resampling operations for avoiding to project the coordinates of every pixels
 * when a bilinear interpolation between nearby pixels would be sufficient. Coordinate
 * conversions applied by this class are from <em>target</em> grid cell <em>centers</em>
 * to <em>source</em> grid cell centers.
 *
 * <p>{@code ResamplingGrid} operates on a delimited space specified by a {@link Rectangle}.
 * This space is subdivided into "tiles" (not necessarily coincident with image tiles) where
 * each tile provides its own coefficients for bilinear interpolations.
 * All coordinates inside the same tile are interpolated using the same coefficients.</p>
 *
 * <p>{@link ResampledImage} implements {@link MathTransform2D} for allowing usage by {@link ResampledImage}
 * but is not a full featured transform. For example, it does not support the {@link #inverse()} operation.
 * For this reason this class should not be public and instance of this class should not be accessible
 * outside {@link ResampledImage}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 */
final class ResamplingGrid extends AbstractMathTransform2D {
    /**
     * The minimal tile width and height in pixels. If a tile width or height is less than this threshold,
     * then this class abandons the attempt to create a {@link ResamplingGrid} instance.
     */
    private static final int MIN_TILE_SIZE = 4;

    /**
     * The maximal error allowed, in pixel units.
     * This is the maximal difference allowed between a coordinate transformed
     * using the original transform and the same coordinate transformed using this grid.
     * This maximum is honored on a best-effort basis; it may happen that for some pixels
     * the error is slightly higher.
     */
    @Configuration
    static final double TOLERANCE = 0.125;

    /**
     * Cache of previously computed {@link ResamplingGrid} instances. The cache may retain up to
     * 256 kb (32 kb × {@value Double#BYTES}) of grids by strong references. After that amount,
     * grids are retained by weak references.
     */
    private static final Cache<Key,MathTransform2D> CACHE = new Cache<Key,MathTransform2D>(12, 32L * 1024, false) {
        /** Returns a memory usage estimation divided by {@value Double#BYTES}. */
        @Override protected int cost(final MathTransform2D value) {
            if (value instanceof ResamplingGrid) {
                return ((ResamplingGrid) value).coordinates.length;
            } else {
                return 50;          // Arbitrary value: 2 × the size of a 5×5 matrix.
            }
        }
    };

    /**
     * Number of tiles in this grid. A {@link ResamplingGrid} tile is a rectangular region inside which
     * bilinear interpolations can be used with acceptable errors. {@link ResamplingGrid} tiles are not
     * necessarily coincident with image tiles.
     */
    final int numXTiles, numYTiles;

    /**
     * Number of pixels in a tile row or column.
     * Those values are integers, but stored as {@code double} values for avoiding type conversions.
     */
    private final double tileWidth, tileHeight;

    /**
     * The (x,y) coordinates of the pixel in the upper-left corner of the target image.
     * The coordinate system is the one of inputs given to {@code transform(…)} methods.
     * Those values are integers, but stored as {@code double} values for avoiding type conversions.
     */
    private final double xmin, ymin;

    /**
     * Sequence of (x,y) grid coordinates for all tiles in this grid, stored in row-major fashion.
     * The coordinate system is the one of outputs computed by {@code transform(…)} methods.
     */
    private final double[] coordinates;

    /**
     * Creates a new grid of precomputed values using the given transform applied on the specified region.
     * The region is subdivided into a number of sub-regions. The number of sub-divisions is specified by
     * the {@code depth} argument. A value of 1 means that the region is split in two parts. A value of
     * 2 means that each part is itself split in 2 smaller parts (so the original grid is split in 4),
     * <i>etc.</i> with recursive splits like a QuadTree.
     *
     * <p>Determining an optimal value of {@code depth} argument is the most tricky part of this class.
     * This work is done by {@link #create(MathTransform2D, Rectangle)} static method which expects the
     * {@code toSourceCenter} and {@code bounds} arguments and computes the {@code depth} one.</p>
     *
     * @param  toSourceCenter  conversion from target cell centers to source cell centers.
     * @param  bounds  pixel coordinates of target images for which to create a grid of source coordinates.
     * @param  depth   number of recursive divisions by 2.
     */
    ResamplingGrid(MathTransform2D toSourceCenter, final Rectangle bounds, final Dimension depth) throws TransformException {
        this.xmin   = bounds.x;
        this.ymin   = bounds.y;
        tileWidth   = Math.scalb(bounds.width,  -depth.width);
        tileHeight  = Math.scalb(bounds.height, -depth.height);
        numXTiles   = 1 << depth.width;
        numYTiles   = 1 << depth.height;
        coordinates = new double[(numXTiles+1) * (numYTiles+1) * DIMENSION];
        int p = 0;
        for (int y=0; y<=numYTiles; y++) {
            for (int x=0; x<=numXTiles; x++) {
                coordinates[p++] = x;
                coordinates[p++] = y;
            }
        }
        toSourceCenter = MathTransforms.concatenate(new AffineTransform2D(tileWidth, 0, 0, tileHeight, xmin, ymin), toSourceCenter);
        toSourceCenter.transform(coordinates, 0, coordinates, 0, p/DIMENSION);
    }

    /**
     * Interpolates a single grid coordinate tuple. This method is required by parent class but its implementation
     * just delegates to {@link #transform(double[], int, double[], int, int)}. Since this method is not invoked by
     * {@link ResampledImage}, its performance does not matter.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        if (derivate) {
            throw new TransformException(Errors.format(Errors.Keys.UnsupportedOperation_1, "derivative"));
        }
        transform(srcPts, srcOff, dstPts, dstOff, 1);
        return null;
    }

    /**
     * Interpolates a sequence of grid coordinate tuples. Input and output values are pixel coordinates
     * with integer values located in pixel centers. When this method is invoked by {@link ResampledImage},
     * input coordinates are always integers but output coordinates are generally fractional.
     * All input coordinates must be inside the region specified to constructor.
     *
     * @throws TransformException if an input coordinate is outside the domain of this transform.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts) throws TransformException
    {
        if (srcOff < dstOff && srcPts == dstPts && numPts > 1) {
            super.transform(srcPts, srcOff, dstPts, dstOff, numPts);
            return;
        }
        final int lineStride = numXTiles + 1;
        while (--numPts >= 0) {
            double  x  = (srcPts[srcOff++] - xmin) / tileWidth;
            double  y  = (srcPts[srcOff++] - ymin) / tileHeight;
            double txf = Math.floor(x); x -= txf;
            double tyf = Math.floor(y); y -= tyf;
            int    tx  = (int) txf;
            int    ty  = (int) tyf;
            if (tx < 0 || tx >= numXTiles || ty < 0 || ty >= numYTiles) {
                throw new TransformException(Errors.format(Errors.Keys.OutsideDomainOfValidity));
            }
            final int p00 = (tx + ty * lineStride) * DIMENSION;
            final int p01 = p00 + DIMENSION;
            final int p10 = p00 + DIMENSION * lineStride;
            final int p11 = p10 + DIMENSION;
            final double mx = 1 - x;
            final double my = 1 - y;
            dstPts[dstOff++] = my * (mx*coordinates[p00    ] + x*coordinates[p01    ])
                             +  y * (mx*coordinates[p10    ] + x*coordinates[p11    ]);
            dstPts[dstOff++] = my * (mx*coordinates[p00 | 1] + x*coordinates[p01 | 1])
                             +  y * (mx*coordinates[p10 | 1] + x*coordinates[p11 | 1]);
            /*
             * Note: the |1 above is a cheap way to compute +1 when all `p` indices
             * are known to be even. This is true because `DIMENSION` is even.
             */
        }
    }

    /**
     * Key of cached {@link ResamplingGrid} instances.
     */
    private static final class Key {
        /** transform from target grid center to source grid center. */
        private final MathTransform2D toSourceCenter;

        /** pixel coordinates of target images for which to create a grid of source coordinates. */
        private final int x, y, width, height;

        /** Creates a new key. */
        Key(final MathTransform2D toSourceCenter, final Rectangle bounds) {
            this.toSourceCenter = toSourceCenter;
            x      = bounds.x;
            y      = bounds.y;
            width  = bounds.width;
            height = bounds.height;
        }

        /** Computes a hash code value for this key. */
        @Override public int hashCode() {
            return (((x * 31) + y * 31) + width) * 31 + height + toSourceCenter.hashCode();
        }

        /** Compares the given object with this key for equality. */
        @Override public boolean equals(final Object obj) {
            if (obj instanceof Key) {
                final Key other = (Key) obj;
                return x      == other.x      &&
                       y      == other.y      &&
                       width  == other.width  &&
                       height == other.height &&
                       toSourceCenter.equals(other.toSourceCenter);
            }
            return false;
        }
    }

    /**
     * Creates a grid for the given domain of validity.
     * The {@code toSourceCenter} argument is the transform used for computing the coordinate values in the grid.
     * The {@code toSourceCorner} variable is the transform used for computing derivatives (Jacobian matrices)
     * in order to determine when the grid has enough tiles.
     *
     * @param  toSourceCenter  transform from target grid center to source grid center.
     * @param  bounds          pixel coordinates of target images for which to create a grid of source coordinates.
     * @return a precomputed grid for the given transform.
     * @throws TransformException if a derivative cannot be computed or a point cannot be transformed.
     * @throws ImagingOpException if the grid would be too big for being useful.
     */
    static MathTransform2D getOrCreate(final MathTransform2D toSourceCenter, final Rectangle bounds) throws TransformException {
        final Key key = new Key(toSourceCenter, bounds);
        MathTransform2D grid = CACHE.peek(key);
        if (grid == null) {
            final Cache.Handler<MathTransform2D> handler = CACHE.lock(key);
            try {
                grid = handler.peek();
                if (grid == null) {
                    grid = create(toSourceCenter, bounds);
                }
            } finally {
                handler.putAndUnlock(grid);
            }
        }
        return grid;
    }

    /**
     * Implementation of {@link #getOrCreate(MathTransform2D, Rectangle)} but without using cached instances.
     * Non-private for testing purpose only.
     */
    static MathTransform2D create(final MathTransform2D toSourceCenter, final Rectangle bounds) throws TransformException {
        // transform from target grid corner to source grid corner.
        final MathTransform2D toSourceCorner = (MathTransform2D) MathTransforms.concatenate(
                MathTransforms.translation(-0.5, -0.5), toSourceCenter,
                MathTransforms.translation(+0.5, +0.5));

        final double xmin = bounds.getMinX();
        final double xmax = bounds.getMaxX();
        final double ymin = bounds.getMinY();
        final double ymax = bounds.getMaxY();
        final Point2D.Double point = new Point2D.Double();              // Multi-purpose buffer.
        final Matrix2 upperLeft, upperRight, lowerLeft, lowerRight;
        point.x = xmin; point.y = ymax; upperLeft  = derivative(toSourceCorner, point);
        point.x = xmax; point.y = ymax; upperRight = derivative(toSourceCorner, point);
        point.x = xmin; point.y = ymin; lowerLeft  = derivative(toSourceCorner, point);
        point.x = xmax; point.y = ymin; lowerRight = derivative(toSourceCorner, point);
        /*
         * The tolerance factor is scaled as below. This comment describes a one-dimensional
         * case, but the two dimensional case works on the same principle.
         *
         * Let assume that we computed the derivative of y=f(x) at two locations: x₁ and x₃.
         * The derivative values (the slopes of the y=f(x) function) at those locations are
         * m₁ and m₃.
         *
         *          /          _/
         *         / x₁      _/ x₂      ─── x₃
         *        / m₁=1    /  m₂≈½        m₃=0
         *
         * ResamplingGrid will interpolate the y values between x₁ and x₃. The interpolated results
         * should be exact at locations x₁ and x₃ and have some errors between those two end points.
         *
         * HYPOTHESIS:
         *  1) We presume that the greatest error will be located mid-way between x₁ and x₃.
         *     The x₂ point above represents that location.
         *  2) We presume that the derivative between x₁ and x₃ varies continuously from m₁ to m₃.
         *     The derivative at x₂ may be something close to m₂ ≈ (m₁ + m₃) / 2, but we don't know for sure.
         *
         * Let compute linear approximations of y=f(x) using the two slopes m₁ and m₃. If the hypothesis #2 is true,
         * then the real y values are somewhere between the two approximations. The formulas below uses the x₁ point,
         * but we would get the same final equation if we used the x₃ instead (we don't use both x₁ and x₃ since
         * solving such equation produce 0=0).
         *
         * Given f₁(x) = y₁ + (x − x₁)⋅m₁
         *   and f₃(x) = y₁ + (x − x₁)⋅m₃
         *
         * then the error ε = f₃(x) − f₁(x) at location x=x₂ is (x₂−x₁)⋅(m₃−m₁).
         * Given x₂ = (x₁+x₃)/2, we get ε = (x₃−x₁)/2 ⋅ (m₃−m₁).
         *
         * If we rearange the terms, we get:  (m₃−m₁) = 2⋅ε / (x₃−x₁).
         * The (m₃ − m₁) value is the maximal difference to be accepted
         * in the coefficients of the derivative matrix to be compared.
         */
        final Dimension depth = depth(toSourceCorner, point,
                new Point2D.Double(2 * TOLERANCE / (xmax - xmin),
                                   2 * TOLERANCE / (ymax - ymin)),
                xmin, xmax, ymin, ymax, upperLeft, upperRight, lowerLeft, lowerRight);
        /*
         * At this point we got the `depth` argument we need for instantiating `ResamplingGrid`.
         * The remaining code in this method is only a check for potential optimization.
         */
affine: if (depth.width == 0 && depth.height == 0) {
            /*
             * The transform is approximately affine. Compute the matrix coefficients using the points projected
             * on the four borders of the domain, in order to get a kind of average coefficient values. We don't
             * use the derivative matrix in the center location, because it may not be the best "average" value
             * and some map projection implementations use approximation derived from spherical formulas.
             * The difference is big enough for causing test failure.
             */
            final double xcnt = bounds.getCenterX();
            final double ycnt = bounds.getCenterY();
            double m00, m10, m01, m11;
            Point2D p;
            point.x=xmax; point.y=ycnt; p=toSourceCenter.transform(point, point); m00  = p.getX(); m10  = p.getY();
            point.x=xmin; point.y=ycnt; p=toSourceCenter.transform(point, point); m00 -= p.getX(); m10 -= p.getY();
            point.x=xcnt; point.y=ymax; p=toSourceCenter.transform(point, point); m01  = p.getX(); m11  = p.getY();
            point.x=xcnt; point.y=ymin; p=toSourceCenter.transform(point, point); m01 -= p.getX(); m11 -= p.getY();
            point.x=xcnt; point.y=ycnt; p=toSourceCenter.transform(point, point);
            final double width  = bounds.getWidth();
            final double height = bounds.getHeight();
            final AffineTransform tr = new AffineTransform(m00 / width,  m10 / width,
                                                           m01 / height, m11 / height,
                                                           p.getX(),     p.getY());
            tr.translate(-xcnt, -ycnt);
            /*
             * Since the affine transform that we built is a kind of average computed from bounds center,
             * we need to compare with the four corners for making sure that its precision is sufficient.
             */
            p = null;
            for (int i=0; i<4; i++) {
                point.x = (i & 1) == 0 ? xmin : xmax;
                point.y = (i & 2) == 0 ? ymin : ymax;
                p = tr.transform(point, p);
                final Point2D expected = toSourceCenter.transform(point, point);
                if (!(abs(p.getX() - expected.getX()) <= TOLERANCE &&
                      abs(p.getY() - expected.getY()) <= TOLERANCE))
                {
                    break affine;
                }
            }
            return new AffineTransform2D(tr);
        }
        /*
         * Non-affine transform. Create a grid using the cell size computed (indirectly)
         * by the `depth(…)` method.
         */
        return new ResamplingGrid(toSourceCenter, bounds, depth);
    }

    /**
     * Computes the number of subdivisions (in power of 2) to apply in order to get a good
     * {@link ResamplingGrid} approximation. The {@code width} and {@code height} fields in
     * the returned value have the following meaning:
     *
     * <ul>
     *   <li>0 means that the transform is approximately affine in the region of interest.</li>
     *   <li>1 means that we should split the grid in two parts horizontally and/or vertically.</li>
     *   <li>2 means that we should split the grid in four parts horizontally and/or vertically.</li>
     *   <li><i>etc.</i></li>
     * </ul>
     *
     * @param  toSourceCorner  the transform for which to compute the depth.
     * @param  point           any {@code Point2D.Double} instance, to be written by this method.
     *                         This is provided in argument only for reducing object allocations.
     * @param  tolerance       the tolerance value to use in comparisons of matrix coefficients,
     *                         along the X axis and along the Y axis. The distance between the location
     *                         of the matrix being compared is half the size of the region of interest.
     * @param  xmin            the minimal <var>x</var> coordinate.
     * @param  xmax            the maximal <var>x</var> coordinate.
     * @param  ymin            the minimal <var>y</var> coordinate.
     * @param  ymax            the maximal <var>y</var> coordinate.
     * @param  upperLeft       the transform derivative at {@code (xmin,ymax)}.
     * @param  upperRight      the transform derivative at {@code (xmax,ymax)}.
     * @param  lowerLeft       the transform derivative at {@code (xmin,ymin)}.
     * @param  lowerRight      the transform derivative at {@code (xmax,ymin)}.
     * @return the number of subdivision along each axis.
     * @throws TransformException if a derivative cannot be computed.
     * @throws ImagingOpException if the grid would be too big for being useful.
     */
    private static Dimension depth(final MathTransform2D toSourceCorner,
                                   final Point2D.Double  point,
                                   final Point2D.Double  tolerance,
                                   final double xmin,       final double xmax,
                                   final double ymin,       final double ymax,
                                   final Matrix2 upperLeft, final Matrix2 upperRight,
                                   final Matrix2 lowerLeft, final Matrix2 lowerRight)
            throws TransformException
    {
        if (!(xmax - xmin >= MIN_TILE_SIZE) || !(ymax - ymin >= MIN_TILE_SIZE)) {       // Use ! for catching NaN.
            throw new ImagingOpException(null);
        }
        /*
         * All derivatives will be compared to the derivative at (centerX, centerY).
         * Consequently, the distance between the derivatives are half the distance
         * between [x|y]min and [x|y]max (approximately — we ignore the diagonal).
         * Consequently, the tolerance threshold can be augmented by the same factor.
         */
        final double oldTolX = tolerance.x;
        final double oldTolY = tolerance.y;
        tolerance.x *= 2;
        tolerance.y *= 2;
        final double centerX = point.x = 0.5 * (xmin + xmax);
        final double centerY = point.y = 0.5 * (ymin + ymax);
        final Matrix2 center = Matrix2.castOrCopy(toSourceCorner.derivative(point));
        point.x = xmin;    point.y = centerY; final Matrix2 centerLeft  = derivative(toSourceCorner, point);
        point.x = xmax;    point.y = centerY; final Matrix2 centerRight = derivative(toSourceCorner, point);
        point.x = centerX; point.y = ymin;    final Matrix2 centerLower = derivative(toSourceCorner, point);
        point.x = centerX; point.y = ymax;    final Matrix2 centerUpper = derivative(toSourceCorner, point);
        final boolean cl = equals(center, centerLeft,  tolerance);
        final boolean cr = equals(center, centerRight, tolerance);
        final boolean cb = equals(center, centerLower, tolerance);
        final boolean cu = equals(center, centerUpper, tolerance);
        int nx=0, ny=0;
        /*
         *   upperLeft  ┌──────┬─ centerUpper
         *              │      │
         *   centerLeft ├──────┼─ center
         */
        if (!((cl & cu) && equals(center, upperLeft, tolerance))) {
            final Dimension depth = depth(toSourceCorner, point, tolerance, xmin, centerX, centerY, ymax,
                                          upperLeft, centerUpper, centerLeft, center);
            incrementNonAffineDimension(cl, cu, depth);
            nx = depth.width;
            ny = depth.height;
        }
        /*
         *   centerUpper ─┬──────┐ upperRight
         *                │      │
         *   center      ─┼──────┤ centerRight
         */
        if (!((cr & cu) && equals(center, upperRight, tolerance))) {
            final Dimension depth = depth(toSourceCorner, point, tolerance, centerX, xmax, centerY, ymax,
                                          centerUpper, upperRight, center, centerRight);
            incrementNonAffineDimension(cr, cu, depth);
            nx = Math.max(nx, depth.width);
            ny = Math.max(ny, depth.height);
        }
        /*
         *   centerLeft ├──────┼─ center
         *              │      │
         *   lowerLeft  └──────┴─ centerLower
         */
        if (!((cl & cb) && equals(center, lowerLeft, tolerance))) {
            final Dimension depth = depth(toSourceCorner, point, tolerance, xmin, centerX, ymin, centerY,
                                          centerLeft, center, lowerLeft, centerLower);
            incrementNonAffineDimension(cl, cb, depth);
            nx = Math.max(nx, depth.width);
            ny = Math.max(ny, depth.height);
        }
        /*
         *   center      ─┼──────┤ centerRight
         *                │      │
         *   centerLower ─┴──────┘ lowerRight
         */
        if (!((cr & cb) && equals(center, lowerRight, tolerance))) {
            final Dimension depth = depth(toSourceCorner, point, tolerance, centerX, xmax, ymin, centerY,
                                          center, centerRight, centerLower, lowerRight);
            incrementNonAffineDimension(cr, cb, depth);
            nx = Math.max(nx, depth.width);
            ny = Math.max(ny, depth.height);
        }
        tolerance.x = oldTolX;
        tolerance.y = oldTolY;
        return new Dimension(nx, ny);
    }

    /**
     * Increments the width, the height or both values in the given dimension, depending on which
     * dimension are not affine. This method <strong>must</strong> be invoked using the following
     * pattern, where {@code center} is the matrix of the transform derivative in the center of
     * the region of interest. Note: the order of operations in the {@code if} statement matter!
     *
     * {@code java
     *     he  =  center.equals(matrixOnTheSameHorizontalLine, tolerance);
     *     ve  =  center.equals(matrixOnTheSameVerticalLine,   tolerance);
     *     if (!((he & ve) && center.equals(matrixOnADiagonal, tolerance))) {
     *         incrementNonAffineDimension(he, ve, depth);
     *     }
     * }
     *
     * @param he    {@code true} if the matrix on the horizontal line are equal.
     * @param ve    {@code true} if the matrix on the vertical line are equal.
     * @param depth the dimension in which to increment the width, height or both.
     */
    private static void incrementNonAffineDimension(boolean he, boolean ve, Dimension depth) {
        if (he == ve) {
            /*
             * Both dimensions are not affine: either (he,ve) == false (the obvious case),
             * or (he,ve) == true in which case this method has been invoked only if the
             * last `center.equals(…)` test in the `if` statement returned false.
             */
            depth.width++;
            depth.height++;
        } else if (ve) {
            // Implies (he == false): horizontal dimension is not affine.
            // Don't touch to the vertical dimension since it is affine.
            depth.width++;
        } else {
            // Implies (he == true): horizontal dimension is affine, don't touch it.
            depth.height++;
        }
    }

    /**
     * Computes the derivative of the given transform at the given location and returns the result as a 2×2 matrix.
     * This method invokes the {@link MathTransform2D#derivative(Point2D)} and converts or casts the result to a
     * {@link Matrix2} instance.
     *
     * <p>In Apache SIS implementations, matrices returned by {@code derivative(Point2D)} methods are already
     * instances of {@link Matrix2}. Consequently, in most cases this method will just cast the result.</p>
     *
     * @param  toSourceCenter  the transform for which to compute the derivative.
     * @param  point           the location where to compute the derivative.
     * @return the derivative at the given location as a 2×2 matrix.
     * @throws TransformException if the derivative cannot be computed.
     */
    private static Matrix2 derivative(final MathTransform2D toSourceCenter, final Point2D point) throws TransformException {
        return Matrix2.castOrCopy(toSourceCenter.derivative(point));
    }

    /**
     * Returns {@code true} if the given matrices are equal, up to the given tolerance thresholds.
     * The thresholds can be different for the X and Y axes. This allows to break the loop sooner
     * (resulting in smaller grids) inside the {@link #depth depth(…)} method.
     */
    private static boolean equals(final Matrix2 center, final Matrix2 corner, final Point2D.Double tolerance) {
        return abs(center.m00 - corner.m00) + abs(center.m01 - corner.m01) <= tolerance.x &&
               abs(center.m10 - corner.m10) + abs(center.m11 - corner.m11) <= tolerance.y;
    }

    /**
     * Formats a pseudo-WKT representation of this transform for debugging purpose.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatter.append(numXTiles);
        formatter.append(numYTiles);
        formatter.setInvalidWKT(ResamplingGrid.class, null);
        return "ResamplingGrid";
    }
}
