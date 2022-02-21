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
package org.apache.sis.coverage.grid;

import java.util.Arrays;
import java.awt.image.RenderedImage;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.internal.referencing.WraparoundAxesFinder;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.CRS;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.coverage.CannotEvaluateException;
import org.apache.sis.coverage.PointOutsideCoverageException;


/**
 * Computes or interpolates values of sample dimensions at given positions.
 * Values are computed by calls to {@link #apply(DirectPosition)} and are returned as {@code double[]}.
 *
 * <h2>Multi-threading</h2>
 * Evaluators are not thread-safe. An instance of {@code GridEvaluator} should be created
 * for each thread that need to compute sample values.
 *
 * <h2>Limitations</h2>
 * Current implementation performs nearest-neighbor sampling only. A future version will provide interpolations.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see GridCoverage#evaluator()
 *
 * @since 1.1
 * @module
 */
public class GridEvaluator implements GridCoverage.Evaluator {
    /**
     * The coverage in which to evaluate sample values.
     */
    private final GridCoverage coverage;

    /**
     * The source coordinate reference system of the converter,
     * or {@code null} if assumed the same than the coverage CRS.
     */
    private CoordinateReferenceSystem sourceCRS;

    /**
     * The transform from {@link #sourceCRS} to grid coordinates.
     * This is cached for avoiding the costly process of fetching a coordinate operation
     * in the common case where the coordinate reference systems did not changed.
     */
    private MathTransform sourceToGrid;

    /**
     * Grid coordinates after {@link #sourceToGrid} conversion.
     *
     * @see #toGridPosition(DirectPosition)
     */
    private FractionalGridCoordinates.Position position;

    /**
     * Array where to store sample values computed by {@link #apply(DirectPosition)}.
     * For performance reasons, the same array may be recycled on every method call.
     */
    double[] values;

    /**
     * Whether to return {@code null} instead of throwing an exception if given point
     * is outside coverage bounds.
     *
     * @see #isNullIfOutside()
     */
    private boolean nullIfOutside;


    // ―――――――― Following fields are for the handling of wraparound axes. ―――――――――――――――――――――

    /**
     * A bitmask of grid dimensions that need to be verified for wraparound axes.
     */
    private long wraparoundAxes;

    /**
     * Coverage extent converted to floating point numbers, only for the grid dimensions having
     * a bit set to 1 in {@link #wraparoundAxes} bitmask. The length of this array is the number
     * of bits set in {@link #wraparoundAxes} multiplied by 2. Elements are (lower, upper) tuples.
     */
    private double[] wraparoundExtent;

    /**
     * Transform from grid coordinates to the CRS where wraparound axes may exist.
     * It is sometime the same transform than {@code gridToCRS} but not always.
     * It may differ for example if a projected CRS has been replaced by a geographic CRS.
     */
    private MathTransform gridToWraparound;

    /**
     * The span (maximum - minimum) of wraparound axes, with 0 value for axes that are not wraparound.
     * The length of this array may be shorter than the CRS number of dimensions if all remaining axes
     * are not wraparound axes.
     */
    private double[] periods;

    /**
     * Creates a new evaluator for the given coverage. This constructor is protected for allowing
     * {@link GridCoverage} subclasses to provide their own {@code GridEvaluator} implementations.
     * For using an evaluator, invoke {@link GridCoverage#evaluator()} instead.
     *
     * @param  coverage  the coverage for which to create an evaluator.
     *
     * @see GridCoverage#evaluator()
     */
    protected GridEvaluator(final GridCoverage coverage) {
        ArgumentChecks.ensureNonNull("coverage", coverage);
        this.coverage = coverage;
    }

    /**
     * Returns the coverage from which this evaluator is fetching sample values.
     *
     * @return the source of sample values for this evaluator.
     */
    @Override
    public GridCoverage getCoverage() {
        return coverage;
    }

    /**
     * Returns {@code true} if this evaluator is allowed to wraparound coordinates that are outside the grid.
     * The initial value is {@code false}. This method may continue to return {@code false} even after a call
     * to {@code setWraparoundEnabled(true)} if no wraparound axis has been found in the coverage CRS.
     *
     * @return {@code true} if this evaluator may wraparound coordinates that are outside the grid.
     *
     * @since 1.2
     */
    public boolean isWraparoundEnabled() {
        return (wraparoundAxes != 0);
    }

    /**
     * Specifies whether this evaluator is allowed to wraparound coordinates that are outside the grid.
     * If {@code true} and if a given coordinate is outside the grid, then this evaluator may translate
     * the point along a wraparound axis in an attempt to get the point inside the grid. For example if
     * the coverage CRS has a longitude axis, then the evaluator may translate the longitude value by a
     * multiple of 360°.
     *
     * @param  allow  whether to allow wraparound of coordinates that are outside the grid.
     *
     * @since 1.2
     */
    public void setWraparoundEnabled(final boolean allow) {
        wraparoundAxes = 0;
        if (allow) try {
            final WraparoundAxesFinder f = new WraparoundAxesFinder(coverage.getCoordinateReferenceSystem());
            if ((periods = f.periods()) != null) {
                final GridGeometry gridGeometry = coverage.getGridGeometry();
                final GridExtent extent = gridGeometry.getExtent();
                MathTransform gridToCRS = gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER);
                gridToWraparound = MathTransforms.concatenate(gridToCRS, f.preferredToSpecified.inverse());
                final Matrix m = gridToWraparound.derivative(new DirectPositionView.Double(extent.getPointOfInterest()));
                /*
                 * `gridToWraparound` is the transform from grid coordinates to a CRS where wraparound axes exist.
                 * It may be the coverage CRS or its base CRS. The wraparound axes are identified by `periods`.
                 * The Jacobian matrix tells us which grid axes are dependencies of the wraparound axes.
                 *
                 * Note: the use of this matrix is not fully reliable with non-linear `gridToCRS` transforms
                 * because it is theoretically possible that a coefficient is zero at the point of interest
                 * by pure coincidence but would be non-zero at another location,
                 * But we think that it would require a transform with unlikely properties
                 * (e.g. transforming parallels to vertical straight lines at some places).
                 */
                for (int j = periods.length; --j >= 0;) {
                    if (periods[j] > 0) {                       // Find target dimensions (CRS) having wraparound axis.
                        for (int i = Math.min(m.getNumCol(), Long.SIZE); --i >= 0;) {
                            if (m.getElement(j, i) != 0) {
                                wraparoundAxes |= (1L << i);    // Mark sources (grid dimensions) dependent of target (CRS dimensions).
                            }
                        }
                    }
                }
                /*
                 * Get the grid extent only for the grid axes that are connected to wraparound CRS axes.
                 * There is at least one such axis, otherwise `periods` would have been null.
                 */
                wraparoundExtent = new double[Long.bitCount(wraparoundAxes) << 1];
                long axes = wraparoundAxes;
                int j = 0;
                do {
                    final int i = Long.numberOfTrailingZeros(axes);
                    wraparoundExtent[j++] = extent.getLow(i);
                    wraparoundExtent[j++] = extent.getHigh(i);
                    axes &= ~(1L << i);
                } while (axes != 0);
                assert wraparoundExtent.length == j : j;
            }
        } catch (TransformException e) {
            recoverableException("setWraparoundEnabled", e);
        }
    }

    /**
     * Returns whether to return {@code null} instead of throwing an exception if a point is outside coverage bounds.
     * The default value is {@code false}, which means that the default {@link #apply(DirectPosition)} behavior is to
     * throw {@link PointOutsideCoverageException} for points outside bounds.
     *
     * @return whether {@link #apply(DirectPosition)} return {@code null} for points outside coverage bounds.
     */
    @Override
    public boolean isNullIfOutside() {
        return nullIfOutside;
    }

    /**
     * Sets whether to return {@code null} instead of throwing an exception if a point is outside coverage bounds.
     * The default value is {@code false}. Setting this flag to {@code true} may improve performances if the caller
     * expects that many points will be outside coverage bounds, since it reduces the amount of exceptions to be
     * created.
     *
     * @param  flag  whether {@link #apply(DirectPosition)} should use {@code null} return value instead of
     *               {@link PointOutsideCoverageException} for signaling that a point is outside coverage bounds.
     */
    @Override
    public void setNullIfOutside(final boolean flag) {
        nullIfOutside = flag;
    }

    /**
     * Returns a sequence of double values for a given point in the coverage.
     * The CRS of the given point may be any coordinate reference system;
     * coordinate conversions will be applied as needed.
     * If the CRS of the point is undefined, then it is assumed to be the {@linkplain #getCoverage() coverage} CRS.
     * The returned sequence includes a value for each {@linkplain SampleDimension sample dimension}.
     *
     * <p>The default interpolation type used when accessing grid values for points which fall between
     * grid cells is nearest neighbor. This default interpolation method may change in future version.</p>
     *
     * <p>The default implementation invokes {@link GridCoverage#render(GridExtent)} for a small region
     * around the point. Subclasses should override with more efficient implementation.</p>
     *
     * @param  point   the coordinate point where to evaluate.
     * @return the sample values at the specified point, or {@code null} if the point is outside the coverage.
     *         For performance reason, this method may return the same array
     *         on every method call by overwriting previous values.
     *         Callers should not assume that the array content stay valid for a long time.
     * @throws PointOutsideCoverageException if the evaluation failed because the input point
     *         has invalid coordinates and the {@link #isNullIfOutside()} flag is {@code false}.
     * @throws CannotEvaluateException if the values can not be computed at the specified coordinates
     *         for another reason. This exception may be thrown if the coverage data type can not be
     *         converted to {@code double} by an identity or widening conversion.
     *         Subclasses may relax this constraint if appropriate.
     */
    @Override
    public double[] apply(final DirectPosition point) throws CannotEvaluateException {
        /*
         * TODO: instead of restricting to a single point, keep the automatic size (1 or 2),
         * invoke render for each slice, then interpolate. We would keep a value of 1 in the
         * size array if we want to disable interpolation in some particular axis (e.g. time).
         */
        final GridGeometry gridGeometry = coverage.gridGeometry;
        final long[] size = new long[gridGeometry.getDimension()];
        java.util.Arrays.fill(size, 1);
        try {
            final FractionalGridCoordinates gc = toGridPosition(point);
            try {
                final GridExtent subExtent = gc.toExtent(gridGeometry.extent, size, nullIfOutside);
                if (subExtent != null) {
                    return evaluate(coverage.render(subExtent), 0, 0);
                }
            } catch (ArithmeticException | IndexOutOfBoundsException | DisjointExtentException ex) {
                if (!nullIfOutside) {
                    throw (PointOutsideCoverageException) new PointOutsideCoverageException(
                            gc.pointOutsideCoverage(gridGeometry.extent)).initCause(ex);
                }
            }
        } catch (PointOutsideCoverageException ex) {
            throw ex;
        } catch (RuntimeException | TransformException ex) {
            throw new CannotEvaluateException(ex.getMessage(), ex);
        }
        return null;        // May reach this point only if `nullIfOutside` is true.
    }

    /**
     * Gets sample values from the given image at the given index. This method does not verify
     * explicitly if the coordinates are out of bounds; we rely on the checks performed by the
     * image and sample model implementations.
     *
     * @param  data  the data from which to get the sample values.
     * @param  x     column index of the value to get.
     * @param  y     row index of the value to get.
     * @return the sample values. The same array may be recycled on every method call.
     * @throws ArithmeticException if an integer overflow occurred while computing indices.
     * @throws IndexOutOfBoundsException if a coordinate is out of bounds.
     */
    final double[] evaluate(final RenderedImage data, final int x, final int y) {
        final int tx = ImageUtilities.pixelToTileX(data, x);
        final int ty = ImageUtilities.pixelToTileY(data, y);
        return values = data.getTile(tx, ty).getPixel(x, y, values);
    }

    /**
     * Converts the specified geospatial position to grid coordinates. If the given position
     * is associated to a non-null coordinate reference system (CRS) different than the
     * {@linkplain #coverage} CRS, then this method automatically transforms that position to the
     * {@linkplain GridCoverage#getCoordinateReferenceSystem() coverage CRS} before to compute grid coordinates.
     *
     * <p>This method does not put any restriction on the grid coordinates result.
     * The result may be outside the {@linkplain GridGeometry#getExtent() grid extent}
     * if the {@linkplain GridGeometry#getGridToCRS(PixelInCell) grid to CRS} transform allows it.</p>
     *
     * @param  point  geospatial coordinates (in arbitrary CRS) to transform to grid coordinates.
     * @return the grid coordinates for the given geospatial coordinates.
     * @throws IncompleteGridGeometryException if the {@linkplain GridCoverage#getGridGeometry() grid geometry}
     *         does not define a "grid to CRS" transform, or if the given point has a non-null CRS but the
     *         {@linkplain #coverage} does not {@linkplain GridCoverage#getCoordinateReferenceSystem() have a CRS}.
     * @throws TransformException if the given coordinates can not be transformed.
     *
     * @see FractionalGridCoordinates#toPosition(MathTransform)
     */
    public FractionalGridCoordinates toGridCoordinates(final DirectPosition point) throws TransformException {
        ArgumentChecks.ensureNonNull("point", point);
        return new FractionalGridCoordinates(toGridPosition(point));
    }

    /**
     * Updates the grid {@linkplain #position} with the given geospatial position.
     * This is the implementation of {@link #toGridCoordinates(DirectPosition)} except
     * that it avoid creating a new {@link FractionalGridCoordinates} on each method call.
     *
     * @param  point  the geospatial position.
     * @return the given position converted to grid coordinates (possibly out of grid bounds).
     * @throws TransformException if the given position can not be converted.
     */
    final FractionalGridCoordinates.Position toGridPosition(final DirectPosition point) throws TransformException {
        final CoordinateReferenceSystem crs = point.getCoordinateReferenceSystem();
        if (crs != sourceCRS || sourceToGrid == null) {
            final GridGeometry gridGeometry = coverage.getGridGeometry();
            MathTransform tr = gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER).inverse();
            if (crs != null) try {
                CoordinateOperation op = CRS.findOperation(crs,
                        coverage.getCoordinateReferenceSystem(),
                        gridGeometry.geographicBBox());
                tr = MathTransforms.concatenate(op.getMathTransform(), tr);
            } catch (FactoryException e) {
                throw new TransformException(e.getMessage(), e);
            }
            position     = new FractionalGridCoordinates.Position(tr.getTargetDimensions());
            sourceCRS    = crs;
            sourceToGrid = tr;
        }
        /*
         * Transform geospatial coordinates to grid coordinates. Result is unconditionally stored
         * in the `position` object, which will be copied by the caller if needed for public API.
         */
        final DirectPosition result = sourceToGrid.transform(point, position);
        if (result != position) {
            // Should not happen, but be paranoiac.
            final double[] coordinates = position.coordinates;
            System.arraycopy(result.getCoordinate(), 0, coordinates, 0, coordinates.length);
        }
        /*
         * If most cases, the work of this method ends here. The remaining code in this method
         * is for handling wraparound axes. If a coordinate is outside the coverage extent,
         * check if a wraparound on some axes would bring the coordinates inside the extent.
         * The first step is to get the point closest to the extent.
         */
        long axes = wraparoundAxes;
        if (axes != 0) {
            double[] coordinates = position.coordinates;
            long outsideAxes = 0;
            int j = 0;
            do {
                final int i = Long.numberOfTrailingZeros(axes);
                final double c = coordinates[i];
                double border;
                if (c < (border = wraparoundExtent[j++]) || c > (border = wraparoundExtent[j])) {
                    if (outsideAxes == 0) {
                        final int n = coordinates.length;
                        coordinates = Arrays.copyOf(coordinates, 2*Math.max(n, gridToWraparound.getTargetDimensions()));
                        System.arraycopy(coordinates, 0, coordinates, n, n);
                    }
                    coordinates[i] = border;
                    outsideAxes |= (1L << i);
                }
                j++;    // Outside above `if (…)` statement because increment needs to be unconditional.
                axes &= ~(1L << i);
            } while (axes != 0);
            assert wraparoundExtent.length == j : j;
            /*
             * If a coordinate was found outside the grid, transform to a CRS where we can apply shift.
             * It may be the same CRS than the coverage CRS or the source CRS, but not necessarily.
             * Current version does not try to optimize by checking if `point` argument can be reused.
             */
            if (outsideAxes != 0) {
                gridToWraparound.transform(coordinates, 0, coordinates, 0, 2);
                final int s = gridToWraparound.getTargetDimensions();
                for (int i = periods.length; --i >= 0;) {
                    final double period = periods[i];
                    if (period > 0) {
                        /*
                         * Compute the shift that was necessary for moving the point inside the grid,
                         * then round that shift to an integer amount of periods. Modify the original
                         * coordinate by applying that modified translation.
                         */
                        final int oi = i + s;
                        double shift = coordinates[i] - coordinates[oi];
                        shift = Math.copySign(Math.ceil(Math.abs(shift) / period), shift) * period;
                        coordinates[oi] += shift;
                    }
                }
                /*
                 * Convert back the shifted point to grid coordinates, then check again if the new point
                 * is inside the grid extent. If this is not the case, we will return the old position
                 * on the assumption that it will be less confusing to the user.
                 */
                gridToWraparound.inverse().transform(coordinates, s, coordinates, 0, 1);
                j = 0;
                do {
                    final int i = Long.numberOfTrailingZeros(outsideAxes);
                    final double c = coordinates[i];
                    if (c < wraparoundExtent[j++] || c > wraparoundExtent[j++]) {
                        return position;
                    }
                    outsideAxes &= ~(1L << i);
                } while (outsideAxes != 0);
                System.arraycopy(coordinates, 0, position.coordinates, 0, position.coordinates.length);
            }
        }
        return position;
    }

    /**
     * Invoked when a recoverable exception occurred.
     * Those exceptions must be minor enough that they can be silently ignored in most cases.
     *
     * @param  caller     the method where exception occurred.
     * @param  exception  the exception that occurred.
     */
    private static void recoverableException(final String caller, final TransformException exception) {
        Logging.recoverableException(Logging.getLogger(Modules.RASTER), GridEvaluator.class, caller, exception);
    }
}
