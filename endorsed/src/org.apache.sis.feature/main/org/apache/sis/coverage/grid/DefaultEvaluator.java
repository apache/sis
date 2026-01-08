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

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Objects;
import java.util.Collection;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.internal.shared.DirectPositionView;
import org.apache.sis.referencing.internal.shared.WraparoundAxesFinder;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.geometry.CoordinateFormat;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.Containers;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.coordinate.MismatchedDimensionException;


/**
 * Default implementation of {@link GridCoverage.Evaluator} for interpolating values at given positions.
 * Values are computed by calls to {@link #apply(DirectPosition)} and are returned as {@code double[]}.
 *
 * <h2>Multi-threading</h2>
 * Evaluators are not thread-safe. An instance of {@code DefaultEvaluator} should be created
 * for each thread that need to compute sample values.
 *
 * <h2>Limitations</h2>
 * Current implementation performs nearest-neighbor sampling only.
 * A future version may provide interpolations.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see GridCoverage#evaluator()
 * @see <a href="https://issues.apache.org/jira/browse/SIS-576">SIS-576</a>
 */
abstract class DefaultEvaluator implements GridCoverage.Evaluator {
    /**
     * The coordinate reference system of input points given to this converter,
     * or {@code null} if assumed to be the same as the coverage <abbr>CRS</abbr>.
     * Used by {@link #toGridPosition(DirectPosition)} for checking if {@link #inputToGrid} needs to be recomputed.
     * As long at the evaluated points have the same <abbr>CRS</abbr>, the same transform is reused.
     */
    private CoordinateReferenceSystem inputCRS;

    /**
     * The transform from {@link #inputCRS} to grid coordinates.
     * This is cached for avoiding the costly process of fetching a coordinate operation
     * in the common case where the coordinate reference systems did not changed.
     */
    private MathTransform inputToGrid;

    /**
     * Grid coordinates, created when first needed and then recycled.
     * This is the result of applying the {@link #inputToGrid} transform.
     */
    private DirectPosition gridCoordinates;

    /**
     * Array where to store sample values computed by {@link #apply(DirectPosition)}.
     * For performance reasons, the same array may be recycled on every method call.
     * This array shall <em>not</em> used by {@link #stream(Collection, boolean)},
     * in order to allow parallel execution.
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
     * It is sometimes the same transform as {@code gridToCRS} but not always.
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
     * The slice where to perform evaluation, or {@code null} if not yet computed.
     * This information allows to specify for example two-dimensional points for
     * evaluating in a three-dimensional data cube. This is used for completing
     * the missing coordinate values.
     *
     * @see #getDefaultSlice()
     * @see #setDefaultSlice(Map)
     */
    private Map<Integer, Long> slice;

    /**
     * The format to use for writing error messages for point outside grid domain.
     * Created only when first needed.
     */
    private CoordinateFormat coordinateFormat;

    /**
     * Creates a new evaluator.
     *
     * @see GridCoverage#evaluator()
     */
    protected DefaultEvaluator() {
    }

    /**
     * Returns the default slice where to perform evaluation, or an empty map if unspecified.
     * Keys are dimensions from 0 inclusive to {@link GridGeometry#getDimension()} exclusive,
     * and values are the grid coordinates of the slice in the dimension specified by the key.
     *
     * <p>This information allows to invoke {@link #apply(DirectPosition)} with for example two-dimensional points
     * even if the underlying coverage is three-dimensional. The missing coordinate values are replaced by the
     * values provided in the map.</p>
     *
     * @return the default slice where to perform evaluation, or an empty map if unspecified.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Because the map is unmodifiable.
    public final Map<Integer, Long> getDefaultSlice() {
        if (slice == null) {
            final GridCoverage coverage = getCoverage();
            final GridExtent extent = coverage.getGridGeometry().getExtent();
            slice = Containers.unmodifiable(extent.getSliceCoordinates());
        }
        return slice;
    }

    /**
     * Sets the default slice where to perform evaluation when the points do not have enough dimensions.
     * A {@code null} argument restores the default value, which is to infer the slice from the coverage
     * grid geometry.
     *
     * @param  slice  the default slice where to perform evaluation, or an empty map if none.
     * @throws IllegalArgumentException if the map contains an illegal dimension or grid coordinate value.
     *
     * @see GridExtent#getSliceCoordinates()
     */
    @Override
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setDefaultSlice(Map<Integer, Long> slice) {
        if (!Objects.equals(this.slice, slice)) {
            if (slice != null) {
                slice = Containers.unmodifiable(new TreeMap<>(slice));
                final GridCoverage coverage = getCoverage();
                final GridExtent extent = coverage.getGridGeometry().getExtent();
                final int max = extent.getDimension() - 1;
                for (final Map.Entry<Integer, Long> entry : slice.entrySet()) {
                    final int dim = entry.getKey();
                    ArgumentChecks.ensureBetween("slice.key", 0, max, dim);
                    ArgumentChecks.ensureBetween(extent.getAxisIdentification(dim, dim).toString(),
                                        extent.getLow(dim), extent.getHigh(dim), entry.getValue());
                }
            }
            this.slice  = slice;
            inputCRS    = null;
            inputToGrid = null;
        }
    }

    /**
     * Returns {@code true} if this evaluator is allowed to wraparound coordinates that are outside the grid.
     * The initial value is {@code false}. This method may continue to return {@code false} even after a call
     * to {@code setWraparoundEnabled(true)} if no wraparound axis has been found in the coverage CRS.
     *
     * @return {@code true} if this evaluator may wraparound coordinates that are outside the grid.
     */
    @Override
    public boolean isWraparoundEnabled() {
        return (wraparoundAxes != 0);
    }

    /**
     * Specifies whether this evaluator is allowed to wraparound coordinates that are outside the grid.
     * If {@code true} and if a given coordinate is outside the grid, then this evaluator may translate
     * the point along a wraparound axis in an attempt to get the point inside the grid. For example, if
     * the coverage CRS has a longitude axis, then the evaluator may translate the longitude value by a
     * multiple of 360°.
     *
     * @param  allow  whether to allow wraparound of coordinates that are outside the grid.
     */
    @Override
    public void setWraparoundEnabled(final boolean allow) {
        wraparoundAxes = 0;
        if (allow) try {
            final GridCoverage coverage = getCoverage();
            final var f = new WraparoundAxesFinder(coverage.getCoordinateReferenceSystem());
            if ((periods = f.periods()) != null) {
                final GridGeometry gridGeometry = coverage.getGridGeometry();
                final GridExtent extent = gridGeometry.getExtent();
                MathTransform gridToCRS = gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER);
                gridToWraparound = MathTransforms.concatenate(gridToCRS, f.preferredToSpecified.inverse());
                final Matrix m = gridToWraparound.derivative(new DirectPositionView.Double(extent.getPointOfInterest(PixelInCell.CELL_CENTER)));
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
     * expects that many points will be outside coverage bounds, since it reduces the number of exceptions to be
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
     * This method checks the <abbr>CRS</abbr> and performs coordinate operations if needed.
     *
     * @param  point  the position where to evaluate.
     * @return the sample values at the specified point, or {@code null} if the point is outside the coverage.
     * @throws PointOutsideCoverageException if the evaluation failed because the input point
     *         has invalid coordinates and the {@link #isNullIfOutside()} flag is {@code false}.
     * @throws CannotEvaluateException if the values cannot be computed for another reason.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public double[] apply(final DirectPosition point) throws CannotEvaluateException {
        try {
            final double[] gridCoords = toGridPosition(point);
            final IntFunction<PointOutsideCoverageException> ifOutside;
            if (nullIfOutside) {
                ifOutside = null;
            } else {
                ifOutside = (index) -> pointOutsideCoverage(point);
            }
            if (ValuesAtPointIterator.create(getCoverage(), gridCoords, 1, ifOutside).tryAdvance((t) -> values = t)) {
                return values;
            }
        } catch (PointOutsideCoverageException ex) {
            ex.setOffendingLocation(point);
            throw ex;
        } catch (RuntimeException | FactoryException | TransformException ex) {
            throw new CannotEvaluateException(ex.getMessage(), ex);
        }
        return null;        // May reach this point only if `nullIfOutside` is true.
    }

    /**
     * Returns a stream of sample values for each point of the given collection.
     * The values in the returned stream are traversed in the iteration order of the given collection.
     * The returned stream behave as if {@link #apply(DirectPosition)} was invoked for each point.
     * {@link CannotEvaluateException} may be thrown when this method is invoked or during the traversal.
     *
     * @param  points   the positions where to evaluate.
     * @param  parallel {@code true} for a parallel stream, or {@code false} for a sequential stream.
     * @return the sample values at the specified positions.
     */
    @Override
    public Stream<double[]> stream(final Collection<? extends DirectPosition> points, final boolean parallel) {
        final GridCoverage coverage = getCoverage();
        final int dimension = coverage.gridGeometry.getDimension();
        double[] coordinates = ArraysExt.EMPTY_DOUBLE;
        CoordinateReferenceSystem crs = inputCRS;
        MathTransform toGrid = inputToGrid;
        int srcDim = (toGrid == null) ? 0 : toGrid.getSourceDimensions();
        int inputCoordinateOffset = 0;
        int firstCoordToTransform = 0;
        int numPointsToTransform  = 0;
        try {
            /*
             * Convert the "real world " coordinates to grid coordinates.
             * The CRS of each point is verified. Consecutive points with
             * the same CRS will be transformed in a single operation.
             */
            for (final DirectPosition point : points) {
                if (crs != (crs = point.getCoordinateReferenceSystem())) {
                    if (numPointsToTransform > 0) {     // Because `toGrid` may be null.
                        assert toGrid.getTargetDimensions() == dimension;
                        toGrid.transform(coordinates, firstCoordToTransform,
                                         coordinates, firstCoordToTransform,
                                         numPointsToTransform);
                    }
                    firstCoordToTransform += numPointsToTransform * dimension;
                    inputCoordinateOffset = firstCoordToTransform;
                    numPointsToTransform = 0;
                    toGrid = getInputToGrid(crs);
                    srcDim = toGrid.getSourceDimensions();
                }
                int offset = inputCoordinateOffset;
                if ((inputCoordinateOffset += srcDim) > coordinates.length) {
                    int n = firstCoordToTransform / dimension;      // Number of points already transformed.
                    n = points.size() - n + numPointsToTransform;   // Number of points left to transform.
                    coordinates = Arrays.copyOf(coordinates, Math.multiplyExact(n, Math.max(srcDim, dimension)) + offset);
                }
                for (int i=0; i<srcDim; i++) {
                    coordinates[offset++] = point.getCoordinate(i);
                }
                numPointsToTransform++;
            }
            /*
             * Transforms the remaining sequence of points.
             * Actually, in most cases, all points are transformed here.
             */
            if (numPointsToTransform > 0) {     // Because `toGrid` may be null.
                assert toGrid.getTargetDimensions() == dimension;
                toGrid.transform(coordinates, firstCoordToTransform,
                                 coordinates, firstCoordToTransform,
                                 numPointsToTransform);
            }
            final int numPoints = firstCoordToTransform / dimension + numPointsToTransform;
            postTransform(coordinates, 0, numPoints);
            /*
             * Create the iterator. The `ValuesAtPointIterator.create(…)` method will identify the slices in
             * n-dimensional coverage, get the rendered images for the regions of interest and get the tiles.
             */
            final IntFunction<PointOutsideCoverageException> ifOutside;
            if (nullIfOutside) {
                ifOutside = null;
            } else {
                final var listOfPoints = (points instanceof List<?>) ? (List<? extends DirectPosition>) points : null;
                ifOutside = (index) -> {
                    DirectPosition point = null;
                    if (listOfPoints != null) try {
                        point = listOfPoints.get(index);
                    } catch (IndexOutOfBoundsException e) {
                        recoverableException("pointOutsideCoverage", e);
                    }
                    if (point != null) {
                        return pointOutsideCoverage(point);
                    }
                    return new PointOutsideCoverageException(Resources.format(Resources.Keys.PointOutsideCoverageDomain_1, "#" + index));
                };
            }
            return StreamSupport.stream(ValuesAtPointIterator.create(coverage, coordinates, numPoints, ifOutside), parallel);
        } catch (CannotEvaluateException ex) {
            throw ex;
        } catch (RuntimeException | FactoryException | TransformException ex) {
            throw new CannotEvaluateException(ex.getMessage(), ex);
        }
    }

    /**
     * Converts the specified geospatial position to grid coordinates.
     * If the given position is associated to a non-null coordinate reference system (CRS) different than the
     * {@linkplain #getCoverage() coverage} CRS, then this method automatically transforms that position to the
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
     *         coverage does not {@linkplain GridCoverage#getCoordinateReferenceSystem() have a CRS}.
     * @throws TransformException if the given coordinates cannot be transformed.
     *
     * @see FractionalGridCoordinates#toPosition(MathTransform)
     */
    @Override
    public FractionalGridCoordinates toGridCoordinates(final DirectPosition point) throws TransformException {
        try {
            final double[] gridCoords = toGridPosition(point);
            final int dimension = inputToGrid.getTargetDimensions();
            return new FractionalGridCoordinates(ArraysExt.resize(gridCoords, dimension));
        } catch (FactoryException e) {
            throw new TransformException(e.getMessage(), e);
        }
    }

    /**
     * Returns the grid coordinates of the given "real world" coordinates.
     * This method may return an array longer than the grid dimension.
     * Extra dimensions should be ignored.
     *
     * @param  point  the geospatial position.
     * @return the given position converted to grid coordinates (possibly out of grid bounds).
     * @throws FactoryException if no operation is found form given point CRS to coverage CRS.
     * @throws TransformException if the given position cannot be converted.
     */
    final double[] toGridPosition(final DirectPosition point) throws FactoryException, TransformException {
        /*
         * If the `inputToGrid` transform has not yet been computed or is outdated, compute now.
         * The result will be cached and reused as long as the `inputCRS` is the same.
         */
        gridCoordinates = getInputToGrid(point.getCoordinateReferenceSystem()).transform(point, gridCoordinates);
        final int dimension = inputToGrid.getTargetDimensions();
        final double[] coordinates = point.getCoordinates();
        final double[] gridCoords = (dimension <= coordinates.length) ? coordinates : new double[dimension];
        inputToGrid.transform(coordinates, 0, gridCoords, 0, 1);
        postTransform(gridCoords, 0, 1);
        return gridCoords;
    }

    /**
     * Post-processing on grid coordinates after conversions from <abbr>CRS</abbr> coordinates.
     * If a coordinate is outside the coverage's extent, this method checks if a wraparound on
     * some axes would bring the coordinates inside the extent. Coordinates are adjusted in-place.
     *
     * @param  gridCoords  the grid coordinates.
     * @param  offset      index of the first grid coordinate value.
     * @param  numPoints   number of points in the array.
     */
    private void postTransform(final double[] gridCoords, int offset, int numPoints) throws TransformException {
        if (wraparoundAxes == 0) {
            return;
        }
        double[]  twoPoints = null;   // Created only if needed.
        final int dimension = inputToGrid.getTargetDimensions();
        final int dimOfWrap = gridToWraparound.getTargetDimensions();
next:   while (--numPoints >= 0) {
            long axesToCheck = wraparoundAxes;
            long outsideAxes = 0;
            int  limitIndex  = 0;     // Even indices are lower values, odd indices are upper values.
            do {
                final int  axis = Long.numberOfTrailingZeros(axesToCheck);
                final long mask = 1L << axis;
                final double c  = gridCoords[offset + axis];
                double limit;
                if (c < (limit = wraparoundExtent[limitIndex]) || c > (limit = wraparoundExtent[limitIndex + 1])) {
                    /*
                     * Detected that the point is outside the grid extent along an axis where wraparound is possible.
                     * We will compute 2 points with the same coordinates except on axes where the point is outside.
                     * On those axes, the coordinate of the copied point is set to the closest limit of the grid.
                     */
                    if (outsideAxes == 0) {
                        if (twoPoints == null) {
                            twoPoints = new double[Math.max(dimension, dimOfWrap * 2)];
                        }
                        System.arraycopy(gridCoords, offset, twoPoints, 0, dimension);
                    }
                    twoPoints[axis] = limit;
                    outsideAxes |= mask;
                }
                axesToCheck &= ~mask;
                limitIndex  += 2;
            } while (axesToCheck != 0);
            assert wraparoundExtent.length == limitIndex : limitIndex;
            /*
             * If a coordinate was found outside the grid, transform to a CRS where we can apply shift.
             * It may be the same CRS as the coverage CRS or as the source CRS, but not necessarily.
             * For example if the CRS is projected, then we need to use a geographic intermediate CRS.
             */
            if (outsideAxes != 0) {
                gridToWraparound.transform(twoPoints, 0, twoPoints, dimOfWrap, 1);  // Clipped coordinates.
                gridToWraparound.transform(gridCoords, offset, twoPoints, 0, 1);    // Original coordinates.
                for (int axis = periods.length; --axis >= 0;) {
                    final double period = periods[axis];
                    if (period > 0) {
                        /*
                         * Compute the shift that was necessary for moving the point inside the grid,
                         * then round that shift to an integer number of periods. Modify the original
                         * coordinate by applying that modified translation.
                         */
                        double shift = twoPoints[axis + dimOfWrap] - twoPoints[axis];
                        shift = Math.copySign(Math.ceil(Math.abs(shift) / period), shift) * period;
                        twoPoints[axis] += shift;
                    }
                }
                /*
                 * Convert back the shifted point to grid coordinates, then check again if the new point
                 * is inside the grid extent. If this is not the case, we will keep the old position
                 * on the assumption that it will be less confusing to the user.
                 */
                gridToWraparound.inverse().transform(twoPoints, 0, twoPoints, 0, 1);
                limitIndex = 0;
                do {
                    final int axis = Long.numberOfTrailingZeros(outsideAxes);
                    final double c = twoPoints[axis];
                    if (c < wraparoundExtent[limitIndex++] || c > wraparoundExtent[limitIndex++]) {
                        offset += dimension;
                        continue next;
                    }
                    outsideAxes &= ~(1L << axis);
                } while (outsideAxes != 0);
                /*
                 * Copy shifted coordinate values, except the NaN values.
                 * NaN values may exist if the given points have less dimensions than the grid geometry, in which
                 * case missing values have been replaced by `slice` values in the `target` array but not in the
                 * `coordinates` array. We want to keep the `slice` values in the `target` array.
                 *
                 * TODO: to be strict, we should skip the copy only if `slice.containsKey(i)` is true, because it
                 * could happen that a transform resulted in NaN values in other dimensions. But that check would
                 * be costly, so we avoid it for now.
                 */
                for (int i=0; i<dimension; i++) {
                    final double value = twoPoints[i];
                    if (!Double.isNaN(value)) {
                        gridCoords[offset] = value;
                    }
                    offset++;
                }
            }
        }
    }

    /**
     * Recomputes the {@link #inputToGrid} field if the <abbr>CRS</abbr> changed.
     * This method should be invoked when the transform has not yet been computed
     * or may became outdated because {@link #inputCRS} needs to be changed.
     *
     * <h4>Thread safety</h4>
     * While {@code DefaultEvaluator} is not multi-thread, we nevertheless need to synchronize
     * this method because it may be invoked by {@link #pointOutsideCoverage(DirectPosition)},
     * which may be invoked from any thread if a stream is executed in parallel.
     *
     * @param  crs  the new value to assign to {@link #inputCRS}. Can be {@code null}.
     * @return the new {@link #inputToGrid} value.
     */
    private synchronized MathTransform getInputToGrid(final CoordinateReferenceSystem crs)
            throws FactoryException, NoninvertibleTransformException
    {
        if (crs == inputCRS && inputToGrid != null) {
            return inputToGrid;
        }
        final GridCoverage coverage = getCoverage();
        final GridGeometry gridGeometry = coverage.getGridGeometry();
        MathTransform gridToCRS = gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER);
        MathTransform crsToGrid = TranslatedTransform.resolveNaN(gridToCRS.inverse(), gridGeometry);
        if (crs != null) {
            final CoordinateReferenceSystem stepCRS = coverage.getCoordinateReferenceSystem();
            final GeographicBoundingBox areaOfInterest = gridGeometry.geographicBBox();
            try {
                CoordinateOperation op = CRS.findOperation(crs, stepCRS, areaOfInterest);
                crsToGrid = MathTransforms.concatenate(op.getMathTransform(), crsToGrid);
            } catch (FactoryException main) {
                /*
                 * Above block tried to compute a "CRS to grid" transform in the most direct way.
                 * It covers the usual case where the point has the required number of dimensions,
                 * and fixes the case when the point has more dimensions (extra dimensions are ignored).
                 * The following block covers the opposite case, where the point does not have enough
                 * dimensions. We try to fill missing dimensions with the help of the `slice` map.
                 */
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final Map<Integer, Long> slice = getDefaultSlice();
                try {
                    CoordinateOperation op = CRS.findOperation(stepCRS, crs, areaOfInterest);
                    gridToCRS = MathTransforms.concatenate(gridToCRS, op.getMathTransform());
                    final var ts = new TransformSeparator(gridToCRS);
                    final int  crsDim = gridToCRS.getTargetDimensions();
                    final int gridDim = gridToCRS.getSourceDimensions();
                    int[] mandatory = new int[gridDim];
                    int n = 0;
                    for (int i=0; i<gridDim; i++) {
                        if (!slice.containsKey(i)) {
                            mandatory[n++] = i;
                        }
                    }
                    mandatory = ArraysExt.resize(mandatory, n);
                    ts.addSourceDimensions(mandatory);          // Retain grid dimensions having no default value.
                    ts.setSourceExpandable(true);               // Retain more grid dimensions if they are required.
                    ts.addTargetDimensionRange(0, crsDim);      // Force retention of all CRS dimensions.
                    gridToCRS = ts.separate();
                    crsToGrid = gridToCRS.inverse();            // With less source dimensions, may be invertible now.
                    mandatory = ts.getSourceDimensions();       // Output grid dimensions computed by `crsToGrid`.
                    final int valueColumn = mandatory.length;   // Matrix column where to write default values.
                    final MatrixSIS m = Matrices.createZero(gridDim+1, valueColumn+1);
                    m.setElement(gridDim, valueColumn, 1);
                    n = 0;
                    for (int j=0; j<gridDim; j++) {
                        if (Arrays.binarySearch(mandatory, j) >= 0) {
                            m.setElement(j, n++, 1);            // Computed value to pass through.
                        } else {
                            final Long value = slice.get(j);
                            if (value == null) {
                                final GridExtent extent = gridGeometry.extent;
                                throw new FactoryException(Resources.format(Resources.Keys.NoNDimensionalSlice_3,
                                                crsDim, extent.getAxisIdentification(j, j), extent.getSize(j)));
                            }
                            m.setElement(j, valueColumn, value);
                        }
                    }
                    crsToGrid = MathTransforms.concatenate(crsToGrid, MathTransforms.linear(m));
                } catch (RuntimeException | FactoryException | NoninvertibleTransformException ex) {
                    main.addSuppressed(ex);
                    throw main;
                }
            }
        }
        // Modify fields only after everything else succeeded.
        inputCRS    = crs;
        inputToGrid = crsToGrid;
        return crsToGrid;
    }

    /**
     * Creates an exception for a grid coordinates out of bounds.
     * This method tries to detect the dimension of the out-of-bounds
     * coordinate by searching for the dimension with largest error.
     *
     * <h4>Thread safety</h4>
     * This method may be invoked during parallel execution of the return value of {@link #stream(Collection, boolean)}.
     * Therefore, it needs to be thread-safe even if {@link GridCoverage.Evaluator} is documented as not thread safe.
     *
     * @param  point  the point which is outside the grid.
     * @return the exception to throw
     */
    final synchronized PointOutsideCoverageException pointOutsideCoverage(final DirectPosition point) {
        String details = null;
        final var buffer = new StringBuilder();
        final GridCoverage coverage = getCoverage();
        final GridExtent extent = coverage.gridGeometry.extent;
        if (extent != null) try {
            gridCoordinates = getInputToGrid(point.getCoordinateReferenceSystem()).transform(point, gridCoordinates);
            int    axis     = 0;
            long   validMin = 0;
            long   validMax = 0;
            double distance = 0;
            final int dimension = Math.min(gridCoordinates.getDimension(), extent.getDimension());
            for (int i=0; i<dimension; i++) {
                final long low  = extent.getLow(i);
                final long high = extent.getHigh(i);
                final double c  = gridCoordinates.getCoordinate(i);
                double d;   // Reminder: value may be NaN.
                if ((d = low - c) > distance || (d = c - high) > distance) {
                    axis     = i;
                    validMin = low;
                    validMax = high;
                    distance = d;
                }
            }
            /*
             * Formats grid coordinates. Those coordinates are, in principle, integers.
             * However if there is a fractional part, keep only the first non-zero digit.
             * This is sufficient for seeing if the coordinate is outside because of that.
             * Intentionally truncate, not round, the fraction digits for easier analysis.
             *
             * Note: if `distance` is zero, the point is not really outside. It should not happen,
             * but if it happens anyway the error message written in this block would be misleading.
             */
            if (distance > 0) {
                for (int i=0; i<dimension; i++) {
                    if (i != 0) buffer.append(' ');
                    int s = buffer.length();
                    StringBuilders.trimFractionalPart(buffer.append(gridCoordinates.getCoordinate(i)));
                    if ((s = buffer.indexOf(".", s)) >= 0) {
                        while (++s < buffer.length()) {
                            if (buffer.charAt(s) != '0') {
                                buffer.setLength(s + 1);
                                break;
                            }
                        }
                    }
                }
                details = Resources.format(Resources.Keys.GridCoordinateOutsideCoverage_4,
                        extent.getAxisIdentification(axis, axis), validMin, validMax, buffer);
            }
        } catch (MismatchedDimensionException | FactoryException | TransformException e) {
            recoverableException("pointOutsideCoverage", e);
        }
        /*
         * If non-null, `message` provides details about the problem using grid coordinates.
         * Also format a simpler message using the "real world" coordinates.
         */
        buffer.setLength(0);
        if (coordinateFormat == null) {
            coordinateFormat = coverage.createCoordinateFormat();
        }
        coordinateFormat.format(point, buffer);
        String message = Resources.format(Resources.Keys.PointOutsideCoverageDomain_1, buffer);
        if (details != null) {
            message = message + System.lineSeparator() + details;
        }
        return new PointOutsideCoverageException(message, point);
    }

    /**
     * Invoked when a recoverable exception occurred.
     * Those exceptions must be minor enough that they can be silently ignored in most cases.
     *
     * @param  caller     the method where exception occurred.
     * @param  exception  the exception that occurred.
     */
    private static void recoverableException(final String caller, final Exception exception) {
        Logging.recoverableException(GridExtent.LOGGER, DefaultEvaluator.class, caller, exception);
    }
}
