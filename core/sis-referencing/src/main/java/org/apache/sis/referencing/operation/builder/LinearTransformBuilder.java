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
package org.apache.sis.referencing.operation.builder;

import java.util.Map;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.geometry.coordinate.Position;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.io.TableAppender;
import org.apache.sis.math.Line;
import org.apache.sis.math.Plane;
import org.apache.sis.math.Vector;
import org.apache.sis.geometry.DirectPosition1D;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.internal.util.AbstractMap;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Classes;


/**
 * Creates an affine transform which will map approximately the given source positions to the given target positions.
 * In many cases, the <em>source</em> positions are grid indices and the <em>target</em> positions are geographic or
 * projected coordinates, but this is not mandatory. If the source positions are known to be grid indices,
 * then a builder created by the {@link #LinearTransformBuilder(int...)} constructor will be more efficient.
 * Otherwise a builder created by the {@link #LinearTransformBuilder()} constructor will be able to handle
 * randomly distributed coordinates.
 *
 * <p>The transform coefficients are determined using a <cite>least squares</cite> estimation method,
 * with the assumption that source positions are exact and all the uncertainty is in the target positions.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see LocalizationGridBuilder
 * @see LinearTransform
 * @see Line
 * @see Plane
 *
 * @since 0.5
 * @module
 */
public class LinearTransformBuilder extends TransformBuilder {
    /**
     * Number of grid columns and rows, or {@code null} if the coordinates are not distributed on a regular grid.
     * If the grid size is known, then the {@link #sources} coordinates do not need to be specified.
     */
    private final int[] gridSize;

    /**
     * The arrays of source coordinate values. Accessed with indices in that order: {@code sources[dimension][point]}.
     * This layout allows to create only a few (typically two) large arrays instead of a multitude of small arrays.
     * Example: {x[], y[]}.
     *
     * <p>In the special case where {@link #gridSize} is non-null, then this array does not need to be specified
     * and can be {@code null}. In such case, the source coordinates are implicitly:</p>
     *
     * <blockquote>
     * (0,0), (1,0), (2,0), (3,0) … ({@link #gridSize}[0]-1, 0),<br>
     * (0,1), (1,1), (2,1), (3,1) … ({@link #gridSize}[0]-1, 1),<br>
     * (0,2), (1,2), (2,2), (3,2) … ({@link #gridSize}[0]-1, 2),<br>
     * (0,{@link #gridSize}[1]-1) … ({@link #gridSize}[0]-1, {@link #gridSize}[1]-1).
     * </blockquote>
     */
    private double[][] sources;

    /**
     * The arrays of target coordinate values. Accessed with indices in that order: {@code targets[dimension][point]}.
     * This layout allows to create only a few (typically two) large arrays instead of a multitude of small arrays.
     * Example: {x[], y[], z[]}.
     * This is {@code null} if not yet specified.
     */
    private double[][] targets;

    /**
     * The product of all {@link #gridSize} values, or 0 if none if {@link #gridSize} is null.
     * If non-zero, then this is the length of {@link #targets} arrays to create.
     */
    final int gridLength;

    /**
     * Number of valid positions in the {@link #sources} or {@link #targets} arrays.
     * Note that the "valid" positions may contain {@link Double#NaN} coordinate values.
     * This field is only indicative if this {@code LinearTransformBuilder} instance
     * has been created by {@link #LinearTransformBuilder(int...)} because we do not
     * try to detect if user adds a new point or overwrites an existing one.
     */
    private int numPoints;

    /**
     * The transform created by the last call to {@link #create(MathTransformFactory)}.
     * This is reset to {@code null} when coordinates are modified.
     */
    private transient LinearTransform transform;

    /**
     * An estimation of the Pearson correlation coefficient for each target dimension.
     * This is {@code null} if not yet computed.
     */
    private transient double[] correlation;

    /**
     * Creates a new linear transform builder for randomly distributed positions.
     *
     * <div class="note"><b>Tip:</b>
     * if the source coordinates are grid indices, then
     * the {@link #LinearTransformBuilder(int...)} constructor will create a more efficient builder.
     * </div>
     */
    public LinearTransformBuilder() {
        gridSize = null;
        gridLength = 0;
    }

    /**
     * Creates a new linear transform builder for source positions distributed on a regular grid.
     * This constructor notifies {@code LinearTransformBuilder} that coordinate values of all source positions will
     * be integers in the [0 … {@code gridSize[0]}-1] range for the first dimension (typically column indices),
     * in the [0 … {@code gridSize[1]}-1] range for the second dimension (typically row indices), <i>etc.</i>
     * The dimension of all source positions is the length of the given {@code gridSize} array.
     *
     * <p>An empty array is equivalent to invoking the no-argument constructor,
     * i.e. no restriction is put on the source coordinates.</p>
     *
     * @param  gridSize  the number of integer coordinate values in each grid dimension.
     * @throws IllegalArgumentException if a grid size is not strictly positive, or if the product
     *         of all values (∏{@code gridSize}) is greater than {@link Integer#MAX_VALUE}.
     *
     * @since 0.8
     */
    public LinearTransformBuilder(int... gridSize) {
        ArgumentChecks.ensureNonNull("gridSize", gridSize);
        if (gridSize.length == 0) {
            this.gridSize = null;
            this.gridLength = 0;
        } else {
            gridSize = gridSize.clone();
            long length = 1;
            for (int s : gridSize) {
                ArgumentChecks.ensureStrictlyPositive("gridSize", s);
                length = Math.multiplyExact(length, s);
            }
            if (length > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.ValueOutOfRange_4,
                        "∏gridSize", 1, Integer.MAX_VALUE, length));
            }
            this.gridSize = gridSize;
            gridLength = (int) length;
        }
    }

    /**
     * Returns the grid size for the given dimension. It is caller's responsibility to ensure that
     * this method is invoked only on instances created by {@link #LinearTransformBuilder(int...)}.
     *
     * @see #getGridDimensions()
     */
    final int gridSize(final int srcDim) {
        return gridSize[srcDim];
    }

    /**
     * Allocates memory for a builder created for source positions distributed on a grid.
     * All target values need to be initialized to NaN because we can not rely on {@link #numPoints}.
     *
     * <p>If this builder has been created for randomly distributed source points, then the allocation
     * should rather be performed as below:</p>
     *
     * {@preformat java
     *    sources = new double[srcDim][capacity];
     *    targets = new double[tgtDim][capacity];
     * }
     */
    private void allocate(final int tgtDim) {
        targets = new double[tgtDim][gridLength];
        for (final double[] r : targets) {
            Arrays.fill(r, Double.NaN);
        }
    }

    /**
     * Resize all the given arrays to the given capacity. This method should be invoked only for
     * {@code LinearTransformBuilder} instances created for randomly distributed source positions.
     */
    private static void resize(double[][] data, final int capacity) {
        for (int i=0; i<data.length; i++) {
            data[i] = ArraysExt.resize(data[i], capacity);
        }
    }

    /**
     * Returns the offset of the given source grid coordinate, or -1 if none. The algorithm implemented in this
     * method is inefficient, but should rarely be used. This is only a fallback when {@link #flatIndex(int[])}
     * can not be used. Callers is responsible to ensure that the number of dimensions match.
     *
     * @see ControlPoints#search(double[][], double[])
     */
    private int search(final int[] source) {
        assert gridSize == null;         // This method can not be invoked for points distributed on a grid.
search: for (int j=numPoints; --j >= 0;) {
            for (int i=0; i<source.length; i++) {
                if (source[i] != sources[i][j]) {
                    continue search;                            // Search another position for the same source.
                }
            }
            return j;
        }
        return -1;
    }

    /**
     * Returns the offset where to store a target position for the given source position in the flattened array.
     * This method should be invoked only when this {@code LinearTransformBuilder} has been created for a grid
     * of known size. Caller must have verified the array length before to invoke this method.
     *
     * @throws IllegalArgumentException if a coordinate value is illegal.
     */
    private int flatIndex(final int[] source) {
        assert sources == null;               // This method can not be invoked for randomly distributed points.
        int offset = 0;
        for (int i = gridSize.length; i != 0;) {
            final int size = gridSize[--i];
            final int index = source[i];
            if (index < 0 || index >= size) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.ValueOutOfRange_4, "source", 0, size-1, index));
            }
            offset = offset * size + index;
        }
        return offset;
    }

    /**
     * Returns the index where to store a target position for the given source position in the flattened array.
     * This method should be invoked only when this {@code LinearTransformBuilder} has been created for a grid
     * of known size. Callers must have verified the position dimension before to invoke this method.
     *
     * @throws IllegalArgumentException if a coordinate value is illegal.
     *
     * @see ControlPoints#flatIndex(DirectPosition)
     */
    private int flatIndex(final DirectPosition source) {
        assert sources == null;               // This method can not be invoked for randomly distributed points.
        int offset = 0;
        for (int i = gridSize.length; i != 0;) {
            final int size = gridSize[--i];
            final double coordinate = source.getOrdinate(i);
            final int index = (int) coordinate;
            if (index != coordinate) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.NotAnInteger_1, coordinate));
            }
            if (index < 0 || index >= size) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.ValueOutOfRange_4, "source", 0, size-1, index));
            }
            offset = offset * size + index;
        }
        return offset;
    }

    /**
     * Verifies that the given number of dimensions is equal to the expected value.
     * No verification are done if the source point is the first point of randomly distributed points.
     */
    private void verifySourceDimension(final int actual) {
        final int expected;
        if (gridSize != null) {
            expected = gridSize.length;
        } else if (sources != null) {
            expected = sources.length;
        } else {
            return;
        }
        if (actual != expected) {
            throw new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimension_3, "source", expected, actual));
        }
    }

    /**
     * Builds the exception message for an unexpected position dimension. This method assumes
     * that positions are stored in this builder as they are read from user-provided collection,
     * with {@link #numPoints} the index of the next point that we failed to add.
     */
    private MismatchedDimensionException mismatchedDimension(final String name, final int expected, final int actual) {
        return new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimension_3,
                    Strings.toIndexed(name, numPoints), expected, actual));
    }

    /**
     * Returns the error message to be given to {@link IllegalStateException} when there is no data.
     */
    private static String noData() {
        return Resources.format(Resources.Keys.MissingValuesInLocalizationGrid);
    }

    /**
     * Returns the number of dimensions in the source grid, or -1 if this builder is not backed by a grid.
     * Contrarily to the other {@code get*Dimensions()} methods, this method does not throw exception.
     *
     * @see #getSourceDimensions()
     * @see #gridSize(int)
     */
    final int getGridDimensions() {
        return (gridSize != null) ? gridSize.length : -1;
    }

    /**
     * Returns the number of dimensions in source positions.
     *
     * @return the dimension of source points.
     * @throws IllegalStateException if the number of source dimensions is not yet known.
     *
     * @see LinearTransform#getSourceDimensions()
     *
     * @since 0.8
     */
    public int getSourceDimensions() {
        if (gridSize != null) return gridSize.length;
        if (sources  != null) return sources.length;
        throw new IllegalStateException(noData());
    }

    /**
     * Returns the number of dimensions in target positions.
     *
     * @return the dimension of target points.
     * @throws IllegalStateException if the number of target dimensions is not yet known.
     *
     * @see LinearTransform#getTargetDimensions()
     *
     * @since 0.8
     */
    public int getTargetDimensions() {
        if (targets != null) return targets.length;
        throw new IllegalStateException(noData());
    }

    /**
     * Returns the envelope of source points. This method returns the known minimum and maximum values for each dimension,
     * <strong>not</strong> expanded to encompass full cell surfaces. In other words, the returned envelope encompasses only
     * {@linkplain org.opengis.referencing.datum.PixelInCell#CELL_CENTER cell centers}.
     *
     * <p>If a grid size was {@link #LinearTransformBuilder(int...) specified at construction time},
     * then those minimums and maximums are inferred from the grid size and are always integer values.
     * Otherwise, the minimums and maximums are extracted from the control points and may be any floating point values.
     * In any cases, the lower and upper values are inclusive.</p>
     *
     * @return the envelope of source points (cell centers), inclusive.
     * @throws IllegalStateException if the source points are not yet known.
     *
     * @since 1.0
     */
    public Envelope getSourceEnvelope() {
        if (gridSize != null) {
            final int dim = gridSize.length;
            final GeneralEnvelope envelope = new GeneralEnvelope(dim);
            for (int i=0; i <dim; i++) {
                envelope.setRange(i, 0, gridSize[i] - 1);
            }
            return envelope;
        } else {
            return envelope(sources);
        }
    }

    /**
     * Returns the envelope of target points. The lower and upper values are inclusive.
     *
     * @return the envelope of target points.
     * @throws IllegalStateException if the target points are not yet known.
     *
     * @since 1.0
     */
    public Envelope getTargetEnvelope() {
        return envelope(targets);
    }

    /**
     * Implementation of {@link #getSourceEnvelope()} and {@link #getTargetEnvelope()}.
     */
    private static Envelope envelope(final double[][] points) {
        if (points == null) {
            throw new IllegalStateException(noData());
        }
        final int dim = points.length;
        final GeneralEnvelope envelope = new GeneralEnvelope(dim);
        for (int i=0; i<dim; i++) {
            double lower = Double.POSITIVE_INFINITY;
            double upper = Double.NEGATIVE_INFINITY;
            for (final double value : points[i]) {
                if (value < lower) lower = value;
                if (value > upper) upper = value;
            }
            if (lower > upper) {
                lower = upper = Double.NaN;
            }
            envelope.setRange(i, lower, upper);
        }
        return envelope;
    }

    /**
     * Returns the direct position of the given position, or {@code null} if none.
     */
    private static DirectPosition position(final Position p) {
        return (p != null) ? p.getDirectPosition() : null;
    }

    /**
     * Sets all matching control point pairs, overwriting any previous setting. The source positions are the keys in
     * the given map, and the target positions are the associated values in the map. The map should not contain two
     * entries with the same source position. Coordinate reference systems are ignored.
     * Null positions are silently ignored.
     * Positions with NaN or infinite coordinates cause an exception to be thrown.
     *
     * <p>All source positions shall have the same number of dimensions (the <cite>source dimension</cite>),
     * and all target positions shall have the same number of dimensions (the <cite>target dimension</cite>).
     * However the source dimension does not need to be the same the target dimension.
     * Apache SIS currently supports only one- or two-dimensional source positions,
     * together with arbitrary target dimension.</p>
     *
     * <p>If this builder has been created with the {@link #LinearTransformBuilder(int...)} constructor,
     * then the coordinate values of all source positions shall be integers in the [0 … {@code gridSize[0]}-1]
     * range for the first dimension (typically column indices), in the [0 … {@code gridSize[1]}-1] range for
     * the second dimension (typically row indices), <i>etc</i>. This constraint does not apply for builders
     * created with the {@link #LinearTransformBuilder()} constructor.</p>
     *
     * @param  sourceToTarget  a map of source positions to target positions.
     *         Source positions are assumed precise and target positions are assumed uncertain.
     * @throws IllegalArgumentException if the given positions contain NaN or infinite coordinate values.
     * @throws IllegalArgumentException if this builder has been {@linkplain #LinearTransformBuilder(int...)
     *         created for a grid} but some source coordinates are not indices in that grid.
     * @throws MismatchedDimensionException if some positions do not have the expected number of dimensions.
     *
     * @since 0.8
     */
    public void setControlPoints(final Map<? extends Position, ? extends Position> sourceToTarget)
            throws MismatchedDimensionException
    {
        ArgumentChecks.ensureNonNull("sourceToTarget", sourceToTarget);
        transform   = null;
        correlation = null;
        sources     = null;
        targets     = null;
        numPoints   = 0;
        int srcDim  = 0;
        int tgtDim  = 0;
        for (final Map.Entry<? extends Position, ? extends Position> entry : sourceToTarget.entrySet()) {
            final DirectPosition src = position(entry.getKey());   if (src == null) continue;
            final DirectPosition tgt = position(entry.getValue()); if (tgt == null) continue;
            /*
             * The first time that we get a non-null source and target coordinate, allocate the arrays.
             * The sources arrays are allocated only if the source coordiantes are randomly distributed.
             */
            if (targets == null) {
                tgtDim = tgt.getDimension();
                if (tgtDim <= 0) {
                    throw mismatchedDimension("target", 2, tgtDim);
                }
                if (gridSize == null) {
                    srcDim = src.getDimension();
                    if (srcDim <= 0) {
                        throw mismatchedDimension("source", 2, srcDim);
                    }
                    final int capacity = sourceToTarget.size();
                    sources = new double[srcDim][capacity];
                    targets = new double[tgtDim][capacity];
                } else {
                    srcDim = gridSize.length;
                    allocate(tgtDim);
                }
            }
            /*
             * Verify that the source and target coordinates have the expected number of dimensions before to store
             * the coordinates. If the grid size is known, we do not need to store the source coordinates. Instead,
             * we compute its index in the fixed-size target arrays.
             */
            int d;
            if ((d = src.getDimension()) != srcDim) throw mismatchedDimension("source", srcDim, d);
            if ((d = tgt.getDimension()) != tgtDim) throw mismatchedDimension("target", tgtDim, d);
            boolean isValid = true;
            int index;
            if (gridSize != null) {
                index = flatIndex(src);
            } else {
                index = numPoints;
                for (int i=0; i<srcDim; i++) {
                    isValid &= Double.isFinite(sources[i][index] = src.getOrdinate(i));
                }
            }
            for (int i=0; i<tgtDim; i++) {
                isValid &= Double.isFinite(targets[i][index] = tgt.getOrdinate(i));
            }
            /*
             * If the point contains some NaN or infinite coordinate values, it is okay to leave it as-is
             * (without incrementing 'numPoints') provided that we ensure that at least one value is NaN.
             * For convenience, we set only the first coordinate to NaN. The ControlPoints map will check
             * for the first coordinate too, so we need to keep this policy consistent.
             */
            if (isValid) {
                numPoints++;
            } else {
                targets[0][index] = Double.NaN;
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalMapping_2, src, tgt));
            }
        }
    }

    /**
     * Returns all control points as a map. Values are source coordinates and keys are target coordinates.
     * The map is unmodifiable and is guaranteed to contain only non-null keys and values.
     * The map is a view: changes in this builder are immediately reflected in the returned map.
     *
     * @return all control points in this builder.
     *
     * @since 1.0
     */
    public Map<DirectPosition,DirectPosition> getControlPoints() {
        return (gridSize != null) ? new ControlPoints() : new Ungridded();
    }

    /**
     * Implementation of the map returned by {@link #getControlPoints()}. The default implementation
     * is suitable for {@link LinearTransformBuilder} backed by a grid. For non-gridded sources, the
     * {@link Ungridded} subclass shall be used instead.
     */
    private class ControlPoints extends AbstractMap<DirectPosition,DirectPosition> {
        /**
         * Creates a new map view of control points.
         */
        ControlPoints() {
        }

        /**
         * Creates a point from the given data at the given offset. Before to invoke this method,
         * caller should verify index validity and that the coordinate does not contain NaN values.
         */
        final DirectPosition position(final double[][] data, final int offset) {
            switch (data.length) {
                case 1: return new DirectPosition1D(data[0][offset]);
                case 2: return new DirectPosition2D(data[0][offset], data[1][offset]);
            }
            final GeneralDirectPosition pos = new GeneralDirectPosition(data.length);
            for (int i=0; i<data.length; i++) pos.setOrdinate(i, data[i][offset]);
            return pos;
        }

        /**
         * Returns the number of points to consider when searching in {@link #sources} or {@link #targets} arrays.
         * For gridded data we can not rely on {@link #numPoints} because the coordinate values may be at any index,
         * not necessarily at consecutive indices.
         */
        int domain() {
            return gridLength;
        }

        /**
         * Returns the index of the given coordinates in the given data array (source or target coordinates).
         * This method is a copy of {@link LinearTransformBuilder#search(int[])}, but working on real values
         * instead than integers and capable to work on {@link #targets} as well as {@link #sources}.
         *
         * <p>If the given coordinates contain NaN values, then this method will always return -1 even if the
         * given data contains the same NaN values. We want this behavior because NaN mean that the point has
         * not been set. There is no confusion with NaN values that users could have set explicitly because
         * {@code setControlPoint} methods do not allow NaN values.</p>
         *
         * @see LinearTransformBuilder#search(int[])
         */
        final int search(final double[][] data, final double[] coord) {
            if (data != null && coord.length == data.length) {
search:         for (int j=domain(); --j >= 0;) {
                    for (int i=0; i<coord.length; i++) {
                        if (coord[i] != data[i][j]) {           // Intentionally want 'false' for NaN values.
                            continue search;
                        }
                    }
                    return j;
                }
            }
            return -1;
        }

        /**
         * Returns {@code true} if the given value is one of the target coordinates.
         * This method requires a linear scan of the data.
         */
        @Override
        public final boolean containsValue(final Object value) {
            return (value instanceof Position) && search(targets, ((Position) value).getDirectPosition().getCoordinate()) >= 0;
        }

        /**
         * Returns {@code true} if the given value is one of the source coordinates.
         * This method is fast on gridded data, but requires linear scan on non-gridded data.
         */
        @Override
        public final boolean containsKey(final Object key) {
            return (key instanceof Position) && flatIndex(((Position) key).getDirectPosition()) >= 0;
        }

        /**
         * Returns the target point for the given source point.
         * This method is fast on gridded data, but requires linear scan on non-gridded data.
         */
        @Override
        public final DirectPosition get(final Object key) {
            if (key instanceof Position) {
                final int index = flatIndex(((Position) key).getDirectPosition());
                if (index >= 0) return position(targets, index);
            }
            return null;
        }

        /**
         * Returns the index where to fetch a target position for the given source position in the flattened array.
         * This is the same work as {@link LinearTransformBuilder#flatIndex(DirectPosition)}, but without throwing
         * exception if the position is invalid. Instead, -1 is returned as a sentinel value for invalid source
         * (including mismatched number of dimensions).
         *
         * <p>The default implementation assumes a grid. This method must be overridden by {@link Ungridded}.</p>
         *
         * @see LinearTransformBuilder#flatIndex(DirectPosition)
         */
        int flatIndex(final DirectPosition source) {
            final double[][] targets = LinearTransformBuilder.this.targets;
            if (targets != null) {
                final int[] gridSize = LinearTransformBuilder.this.gridSize;
                int i = gridSize.length;
                if (i == source.getDimension()) {
                    int offset = 0;
                    while (i != 0) {
                        final int size = gridSize[--i];
                        final double coordinate = source.getOrdinate(i);
                        final int index = (int) coordinate;
                        if (index < 0 || index >= size || index != coordinate) {
                            return -1;
                        }
                        offset = offset * size + index;
                    }
                    if (!Double.isNaN(targets[0][offset])) return offset;
                }
            }
            return -1;
        }

        /**
         * Returns an iterator over the entries.
         * {@code DirectPosition} instances are created on-the-fly during the iteration.
         *
         * <p>The default implementation assumes a grid. This method must be overridden by {@link Ungridded}.</p>
         */
        @Override
        protected EntryIterator<DirectPosition,DirectPosition> entryIterator() {
            return new EntryIterator<DirectPosition,DirectPosition>() {
                /**
                 * Index in the flat arrays of the next entry to return.
                 */
                private int index = -1;

                /**
                 * Moves to the next entry and returns {@code true} if an entry has been found.
                 * This method skips coordinates having NaN value. Those NaN values may happen
                 * on gridded data (they mean that the point has not yet been set), but should
                 * not happen on non-gridded data.
                 */
                @Override protected boolean next() {
                    final double[][] targets = LinearTransformBuilder.this.targets;
                    if (targets != null) {
                        final double[] x = targets[0];
                        final int gridLength = LinearTransformBuilder.this.gridLength;
                        while (++index < gridLength) {
                            if (!Double.isNaN(x[index])) {
                                return true;
                            }
                        }
                    }
                    return false;
                }

                /**
                 * Reconstructs the source coordinates for the current index.
                 * This method is the converse of {@code ControlPoints.flatIndex(DirectPosition)}.
                 * It assumes gridded data; {@link Ungridded} will have to do a different work.
                 */
                @Override protected DirectPosition getKey() {
                    final int[] gridSize = LinearTransformBuilder.this.gridSize;
                    final int dim = gridSize.length;
                    final GeneralDirectPosition pos = new GeneralDirectPosition(dim);
                    int offset = index;
                    for (int i=0; i<dim; i++) {
                        final int size = gridSize[i];
                        pos.setOrdinate(i, offset % size);
                        offset /= size;
                    }
                    if (offset == 0) {
                        return pos;
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                /**
                 * Returns the target coordinates at current index.
                 */
                @Override protected DirectPosition getValue() {
                    return position(targets, index);
                }
            };
        }
    }

    /**
     * Implementation of the map returned by {@link #getControlPoints()} when no grid is used.
     * This implementation is simpler than the gridded case, but less efficient as some methods
     * require a linear scan.
     */
    private final class Ungridded extends ControlPoints {
        /** Overrides default method with more efficient implementation. */
        @Override public boolean isEmpty() {return numPoints == 0;}
        @Override public int     size()    {return numPoints;}
        @Override        int     domain()  {return numPoints;}

        /**
         * Returns the index where to fetch a target position for the given source position
         * in the flattened array. In non-gridded case, this operation requires linear scan.
         */
        @Override int flatIndex(final DirectPosition source) {
            return search(sources, source.getCoordinate());
        }

        /**
         * Returns an iterator over the entries.
         * {@code DirectPosition} instances are created on-the-fly during the iteration.
         */
        @Override protected EntryIterator<DirectPosition,DirectPosition> entryIterator() {
            return new EntryIterator<DirectPosition,DirectPosition>() {
                private int index = -1;

                @Override protected boolean        next()     {return ++index < numPoints;}
                @Override protected DirectPosition getKey()   {return position(sources, index);}
                @Override protected DirectPosition getValue() {return position(targets, index);}
            };
        }
    }

    /**
     * Sets a single matching control point pair. Source position is assumed precise and target position is assumed uncertain.
     * If the given source position was already associated with another target position, then the old target position is discarded.
     *
     * <div class="note"><b>Performance note:</b>
     * current implementation is efficient for builders {@linkplain #LinearTransformBuilder(int...) created for a grid}
     * but inefficient for builders {@linkplain #LinearTransformBuilder() created for randomly distributed points}.
     * In the later case, the {@link #setControlPoints(Map)} method is a more efficient alternative.</div>
     *
     * @param  source  the source coordinates. If this builder has been created with the {@link #LinearTransformBuilder(int...)} constructor,
     *                 then for every index <var>i</var> the {@code source[i]} value shall be in the [0 … {@code gridSize[i]}-1] range inclusive.
     *                 If this builder has been created with the {@link #LinearTransformBuilder()} constructor, then no constraint apply.
     * @param  target  the target coordinates, assumed uncertain.
     * @throws IllegalArgumentException if this builder has been {@linkplain #LinearTransformBuilder(int...) created for a grid}
     *         but some source coordinates are out of index range, or if {@code target} contains NaN of infinite numbers.
     * @throws MismatchedDimensionException if the source or target position does not have the expected number of dimensions.
     *
     * @since 0.8
     */
    public void setControlPoint(final int[] source, final double[] target) {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        verifySourceDimension(source.length);
        final int tgtDim = target.length;
        if (targets != null && tgtDim != targets.length) {
            throw new MismatchedDimensionException(Errors.format(
                    Errors.Keys.MismatchedDimension_3, "target", targets.length, tgtDim));
        }
        int index;
        if (gridSize != null) {
            index = flatIndex(source);        // Invoked first for validating argument before to allocate arrays.
            if (targets == null) {
                allocate(tgtDim);
            }
        } else {
            /*
             * Case of randomly distributed points. Algorithm used below is inefficient, but Javadoc
             * warns the user that (s)he should use setControlPoints(Map) instead in such case.
             */
            final int srcDim = source.length;
            if (targets == null) {
                targets = new double[tgtDim][20];                   // Arbitrary initial capacity of 20 points.
                sources = new double[srcDim][20];
            }
            index = search(source);
            if (index < 0) {
                index = numPoints++;
                if (numPoints >= targets[0].length) {
                    final int n = Math.multiplyExact(numPoints, 2);
                    resize(sources, n);
                    resize(targets, n);
                }
            }
            for (int i=0; i<srcDim; i++) {
                sources[i][index] = source[i];
            }
        }
        boolean isValid = true;
        for (int i=0; i<tgtDim; i++) {
            isValid &= Double.isFinite(targets[i][index] = target[i]);
        }
        transform   = null;
        correlation = null;
        if (!isValid) {
            if (gridSize == null) numPoints--;
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalMapping_2,
                                               source, new DirectPositionView.Double(target)));
        }
    }

    /**
     * Returns a single target coordinate for the given source coordinate, or {@code null} if none.
     * This method can be used for retrieving points set by previous calls to
     * {@link #setControlPoint(int[], double[])} or {@link #setControlPoints(Map)}.
     *
     * <div class="note"><b>Performance note:</b>
     * current implementation is efficient for builders {@linkplain #LinearTransformBuilder(int...) created for a grid}
     * but inefficient for builders {@linkplain #LinearTransformBuilder() created for randomly distributed points}.</div>
     *
     * @param  source  the source coordinates. If this builder has been created with the {@link #LinearTransformBuilder(int...)} constructor,
     *                 then for every index <var>i</var> the {@code source[i]} value shall be in the [0 … {@code gridSize[i]}-1] range inclusive.
     *                 If this builder has been created with the {@link #LinearTransformBuilder()} constructor, then no constraint apply.
     * @return the target coordinates associated to the given source, or {@code null} if none.
     * @throws IllegalArgumentException if this builder has been {@linkplain #LinearTransformBuilder(int...) created for a grid}
     *         but some source coordinates are out of index range.
     * @throws MismatchedDimensionException if the source position does not have the expected number of dimensions.
     *
     * @since 0.8
     */
    public double[] getControlPoint(final int[] source) {
        ArgumentChecks.ensureNonNull("source", source);
        verifySourceDimension(source.length);
        if (targets == null) {
            return null;
        }
        final int index;
        if (gridSize != null) {
            index = flatIndex(source);
        } else {
            index = search(source);
            if (index < 0) {
                return null;
            }
        }
        /*
         * A coordinate with NaN value means that the point has not been set.
         * Not that the coordinate may have only one NaN value, not necessarily
         * all of them, if the point has been deleted after insertion attempt.
         */
        final double[] target = new double[targets.length];
        for (int i=0; i<target.length; i++) {
            if (Double.isNaN(target[i] = targets[i][index])) {
                return null;
            }
        }
        return target;
    }

    /**
     * More straightforward version of {@link #getControlPoint(int[])} for the case where this
     * {@code LinearTransformBuilder} is known to have been built for grid source coordinates.
     * This method is for {@link LocalizationGridBuilder#create(MathTransformFactory)} internal usage.
     */
    final void getControlPoint2D(final int[] source, final double[] target) {
        assert gridSize != null;
        final int index = flatIndex(source);
        final int tgtDim = targets.length;
        for (int i=0; i<tgtDim; i++) {
            target[i] = targets[i][index];
        }
    }

    /**
     * Returns the vector of source coordinates.
     * It is caller responsibility to ensure that this builder is not backed by a grid.
     */
    final Vector[] sources() {
        if (sources != null) {
            final Vector[] v = new Vector[sources.length];
            for (int i=0; i<v.length; i++) {
                v[i] = vector(sources[i]);
            }
            return v;
        }
        throw new IllegalStateException(noData());
    }

    /**
     * Creates a linear transform approximation from the source positions to the target positions.
     * This method assumes that source positions are precise and that all uncertainty is in the target positions.
     *
     * @param  factory  the factory to use for creating the transform, or {@code null} for the default factory.
     *                  The {@link MathTransformFactory#createAffineTransform(Matrix)} method of that factory
     *                  shall return {@link LinearTransform} instances.
     * @return the fitted linear transform.
     * @throws FactoryException if the transform can not be created,
     *         for example because the source or target points have not be specified.
     *
     * @since 0.8
     */
    @Override
    @SuppressWarnings("serial")
    public LinearTransform create(final MathTransformFactory factory) throws FactoryException {
        if (transform == null) {
            final double[][] sources = this.sources;                    // Protect from changes.
            final double[][] targets = this.targets;
            if (targets == null) {
                throw new InvalidGeodeticParameterException(noData());
            }
            final int sourceDim = (sources != null) ? sources.length : gridSize.length;
            final int targetDim = targets.length;
            correlation = new double[targetDim];
            final MatrixSIS matrix = Matrices.create(targetDim + 1, sourceDim + 1,  ExtendedPrecisionMatrix.ZERO);
            matrix.setElement(targetDim, sourceDim, 1);
            for (int j=0; j < targetDim; j++) {
                final double c;
                switch (sourceDim) {
                    case 1: {
                        final int row = j;
                        final Line line = new Line() {
                            @Override public void setEquation(final Number slope, final Number y0) {
                                super.setEquation(slope, y0);
                                matrix.setNumber(row, 0, slope);    // Preserve the extended precision (double-double).
                                matrix.setNumber(row, 1, y0);
                            }
                        };
                        if (sources != null) {
                            c = line.fit(vector(sources[0]), vector(targets[j]));
                        } else {
                            c = line.fit(Vector.createSequence(0, 1, gridSize[0]),
                                         Vector.create(targets[j], false));
                        }
                        break;
                    }
                    case 2: {
                        final int row = j;
                        final Plane plan = new Plane() {
                            @Override public void setEquation(final Number sx, final Number sy, final Number z0) {
                                super.setEquation(sx, sy, z0);
                                matrix.setNumber(row, 0, sx);       // Preserve the extended precision (double-double).
                                matrix.setNumber(row, 1, sy);
                                matrix.setNumber(row, 2, z0);
                            }
                        };
                        if (sources != null) {
                            c = plan.fit(vector(sources[0]), vector(sources[1]), vector(targets[j]));
                        } else try {
                            c = plan.fit(gridSize[0], gridSize[1], Vector.create(targets[j], false));
                        } catch (IllegalArgumentException e) {
                            // This may happen if the z vector still contain some "NaN" values.
                            throw new InvalidGeodeticParameterException(noData(), e);
                        }
                        break;
                    }
                    default: {
                        throw new FactoryException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, sourceDim));
                    }
                }
                correlation[j] = c;
            }
            transform = (LinearTransform) nonNull(factory).createAffineTransform(matrix);
        }
        return transform;
    }

    /**
     * Wraps the given array in a vector of length {@link #numPoints}. This method should be
     * invoked only when this builder has been created by {@link #LinearTransformBuilder()}.
     * This can be identified by {@code sources != null} or {@code gridSize == null}.
     */
    private Vector vector(final double[] data) {
        assert gridSize == null;
        return Vector.create(data, false).subList(0, numPoints);
    }

    /**
     * Returns the correlation coefficients of the last transform created by {@link #create create(…)},
     * or {@code null} if none. If non-null, the array length is equals to the number of target
     * dimensions.
     *
     * @return estimation of correlation coefficients for each target dimension, or {@code null}.
     */
    public double[] correlation() {
        return (correlation != null) ? correlation.clone() : null;
    }

    /**
     * Returns a string representation of this builder for debugging purpose.
     *
     * @return a string representation of this builder.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(Classes.getShortClassName(this)).append('[');
        if (sources != null) {
            buffer.append(sources[0].length).append(" points");
        }
        buffer.append(']');
        if (transform != null) {
            final String lineSeparator = System.lineSeparator();
            buffer.append(':').append(lineSeparator);
            final TableAppender table = new TableAppender(buffer, " ");
            table.setMultiLinesCells(true);
            table.append(Matrices.toString(transform.getMatrix())).nextColumn();
            table.append(lineSeparator).append("  ")
                 .append(Vocabulary.format(Vocabulary.Keys.Correlation)).append(" =").nextColumn();
            table.append(Matrices.create(correlation.length, 1, correlation).toString());
            try {
                table.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);      // Should never happen since we wrote into a StringBuilder.
            }
        }
        return buffer.toString();
    }
}
