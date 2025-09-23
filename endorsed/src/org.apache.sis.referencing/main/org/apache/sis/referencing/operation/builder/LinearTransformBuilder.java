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
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Locale;
import java.util.Objects;
import java.text.NumberFormat;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.DirectPosition1D;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.io.TableAppender;
import org.apache.sis.math.Line;
import org.apache.sis.math.Plane;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.ExtendedPrecisionMatrix;
import org.apache.sis.referencing.internal.shared.DirectPositionView;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Classes;
import org.apache.sis.util.internal.shared.AbstractMap;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;


/**
 * Creates an affine transform which will map approximately the given source positions to the given target positions.
 * In many cases, the <em>source</em> positions are grid indices and the <em>target</em> positions are geographic or
 * projected coordinates, but this is not mandatory. If the source positions are known to be grid indices,
 * then a builder created by the {@link #LinearTransformBuilder(int...)} constructor will be more efficient.
 * Otherwise a builder created by the {@link #LinearTransformBuilder()} constructor will be able to handle
 * randomly distributed coordinates.
 *
 * <p>Builders are not thread-safe. Builders can be used only once;
 * points cannot be added or modified after {@link #create(MathTransformFactory)} has been invoked.
 * The transform coefficients are determined using a <i>least squares</i> estimation method,
 * with the assumption that source positions are exact and all the uncertainty is in the target positions.</p>
 *
 * <h2>Linearizers</h2>
 * Consider the following situation (commonly found with {@linkplain org.apache.sis.storage.netcdf netCDF files}):
 * the <i>sources</i> coordinates are pixel indices and the <i>targets</i> are (longitude, latitude) coordinates,
 * but we suspect that the <i>sources to targets</i> transform is some undetermined map projection, maybe Mercator.
 * A linear approximation between those coordinates will give poor results; the results would be much better if all
 * (longitude, latitude) coordinates were converted to the right projection first. However, that map projection may
 * not be known, but we can try to guess it by trials-and-errors using a set of plausible projections.
 * That set can be specified by {@link #addLinearizers(Map, int...)}.
 * If the {@link #create(MathTransformFactory)} method finds that one of the specified projections seems a good fit,
 * it will automatically convert all target coordinates to that projection.
 * That selected projection is given by {@link #linearizer()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see LocalizationGridBuilder
 * @see LinearTransform
 * @see Line
 * @see Plane
 *
 * @since 0.5
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
     *
     * <h4>Implementation note</h4>
     * We could use a flat array with (x₀, y₀), (x₁, y₁), (x₂, y₂), <i>etc.</i> coordinate tuples instead.
     * Such flat array would be more convenient for some coordinate conversions with {@link MathTransform}.
     * But using array of arrays is more convenient for other calculations working on one dimension at time,
     * make data more local for CPU, and also allows handling of more points.
     */
    private double[][] targets;

    /**
     * The product of all {@link #gridSize} values, or 0 if none or if {@link #gridSize} is null.
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
     * If the user suspects that the transform may be linear when the target is another space than the space of
     * {@link #targets} coordinates, projections toward spaces to try. The {@link #create(MathTransformFactory)}
     * method will try to apply those transforms on {@link #targets} and check if they produce better fits.
     *
     * @see #addLinearizers(Map, int[])
     * @see #linearizer()
     */
    private List<ProjectedTransformTry> linearizers;

    /**
     * If one of the {@linkplain #linearizers} have been applied, that linearizer. Otherwise {@code null}.
     *
     * @see #linearizer()
     */
    private transient ProjectedTransformTry appliedLinearizer;

    /**
     * The transform created by the last call to {@link #create(MathTransformFactory)}.
     * A non-null value means that this builder became unmodifiable.
     */
    private transient LinearTransform transform;

    /**
     * An estimation of the Pearson correlation coefficient for each target dimension.
     * This is {@code null} if not yet computed.
     */
    private transient double[] correlations;

    /**
     * Creates a temporary builder with all source fields from the given builder and no target arrays.
     * Calculated fields, namely {@link #correlations} and {@link #transform}, are left uninitialized.
     * Arrays are copied by references and their content shall not be modified. The new builder should
     * not be made accessible to users since changes in this builder would be reflected in the source
     * values of original builder. This constructor is reserved to {@link #create(MathTransformFactory)}
     * internal usage.
     *
     * @param original  the builder from which to take array references of source values.
     */
    private LinearTransformBuilder(final LinearTransformBuilder original) {
        gridSize   = original.gridSize;
        sources    = original.sources;
        gridLength = original.gridLength;
        numPoints  = original.numPoints;
    }

    /**
     * Creates a new linear transform builder for randomly distributed positions.
     *
     * <h4>Performance note</h4>
     * If the source coordinates are grid indices, then
     * the {@link #LinearTransformBuilder(int...)} constructor will create a more efficient builder.
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
        ArgumentChecks.ensureNonEmptyBounded("gridSize", false, 1, Integer.MAX_VALUE, gridSize);
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
     * Returns a linear approximation of the given transform for the specified domain.
     * The source positions are integer coordinates included in the given envelope.
     * The target positions are the results of transforming source coordinates with
     * the given {@code gridToCRS} transform.
     *
     * @param  gridToCRS  the transform from source coordinates (grid indices) to target coordinates.
     * @param  domain     domain of integer source coordinates for which to get a linear approximation.
     *                    Both lower and upper coordinate values are <em>inclusive</em>.
     * @return a linear approximation of given transform for the specified domain.
     * @throws FactoryException if the transform approximation cannot be computed.
     *
     * @see #setControlPoints(MathTransform)
     * @see org.apache.sis.coverage.grid.DomainLinearizer
     *
     * @since 1.1
     */
    public static LinearTransform approximate(final MathTransform gridToCRS, final Envelope domain) throws FactoryException {
        ArgumentChecks.ensureNonNull("gridToCRS", gridToCRS);
        ArgumentChecks.ensureNonNull("domain",    domain);
        try {
            return (LinearTransform) Linearizer.approximate(gridToCRS, domain);
        } catch (TransformException e) {
            throw new LocalizationGridException(e);
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
     * All target values need to be initialized to NaN because we cannot rely on {@link #numPoints}.
     *
     * <p>If this builder has been created for randomly distributed source points, then the allocation
     * should rather be performed as below:</p>
     *
     * {@snippet lang="java" :
     *     sources = new double[srcDim][capacity];
     *     targets = new double[tgtDim][capacity];
     *     }
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
     * cannot be used. Callers is responsible to ensure that the number of dimensions match.
     *
     * @see ControlPoints#search(double[][], double[])
     */
    private int search(final int[] source) {
        assert gridSize == null;         // This method cannot be invoked for points distributed on a grid.
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
        assert sources == null;               // This method cannot be invoked for randomly distributed points.
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
        assert sources == null;               // This method cannot be invoked for randomly distributed points.
        int offset = 0;
        for (int i = gridSize.length; i != 0;) {
            final int size = gridSize[--i];
            final double coordinate = source.getCoordinate(i);
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
     * Returns {@code true} if {@link #create(MathTransformFactory)} has not yet been invoked.
     */
    final boolean isModifiable() {
        return transform == null;
    }

    /**
     * Throws {@link IllegalStateException} if this builder cannot be modified anymore.
     */
    private void ensureModifiable() throws IllegalStateException {
        if (transform != null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.UnmodifiableObject_1, LinearTransformBuilder.class));
        }
    }

    /**
     * Verifies that the given number of dimensions is equal to the expected value.
     * No verification are done if the source point is the first point of randomly distributed points.
     */
    private void verifySourceDimension(final int actual) throws MismatchedDimensionException {
        final int expected;
        if (gridSize != null) {
            expected = gridSize.length;
        } else if (sources != null) {
            expected = sources.length;
        } else {
            return;
        }
        if (actual != expected) {
            throw new org.opengis.geometry.MismatchedDimensionException(
                    Errors.format(Errors.Keys.MismatchedDimension_3, "source", expected, actual));
        }
    }

    /**
     * Builds the exception message for an unexpected position dimension. This method assumes
     * that positions are stored in this builder as they are read from user-provided collection,
     * with {@link #numPoints} the index of the next point that we failed to add.
     */
    private MismatchedDimensionException mismatchedDimension(final String name, final int expected, final int actual) {
        return new org.opengis.geometry.MismatchedDimensionException(
                Errors.format(Errors.Keys.MismatchedDimension_3,
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
     * Returns the number of dimensions in source positions. This is the length of the {@code source} array given in argument
     * to {@link #getControlPoint(int[]) get}/{@link #setControlPoint(int[], double[]) setControlPoint(int[], …)} methods.
     * This is also the number of dimensions of the {@link DirectPosition} <em>keys</em> in the {@link Map} exchanged by
     * {@link #getControlPoints() get}/{@link #setControlPoints(Map)} methods.
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
     * Returns the number of dimensions in target positions. This is the length of the {@code target} array exchanged by
     * {@link #getControlPoint(int[]) get}/{@link #setControlPoint(int[], double[]) setControlPoint(…, double[])} methods.
     * This is also the number of dimensions of the {@link DirectPosition} <em>values</em> in the {@link Map} exchanged by
     * {@link #getControlPoints() get}/{@link #setControlPoints(Map)} methods.
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
     * Returns the envelope of source points (<em>keys</em> of the map returned by {@link #getControlPoints()}).
     * The number of dimensions is equal to {@link #getSourceDimensions()}.
     * This method returns the known minimum and maximum values (inclusive) for each dimension,
     * <strong>not</strong> expanded to encompass full cell surfaces. In other words, the returned envelope encompasses only
     * {@linkplain org.apache.sis.coverage.grid.PixelInCell#CELL_CENTER cell centers}.
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
            final var envelope = new GeneralEnvelope(dim);
            for (int i=0; i <dim; i++) {
                envelope.setRange(i, 0, gridSize[i] - 1);
            }
            return envelope;
        } else {
            return envelope(sources, numPoints);
        }
    }

    /**
     * Returns the envelope of target points (<em>values</em> of the map returned by {@link #getControlPoints()}).
     * The number of dimensions is equal to {@link #getTargetDimensions()}. The lower and upper values are inclusive.
     * If a {@linkplain #linearizer() linearizer has been applied}, then coordinates of the returned envelope
     * are projected by that linearizer.
     *
     * @return the envelope of target points.
     * @throws IllegalStateException if the target points are not yet known.
     *
     * @since 1.0
     */
    public Envelope getTargetEnvelope() {
        return envelope(targets, (gridLength != 0) ? gridLength : numPoints);
    }

    /**
     * Implementation of {@link #getSourceEnvelope()} and {@link #getTargetEnvelope()}.
     */
    private static Envelope envelope(final double[][] points, final int numPoints) {
        if (points == null) {
            throw new IllegalStateException(noData());
        }
        final int dim = points.length;
        final var envelope = new GeneralEnvelope(dim);
        for (int i=0; i<dim; i++) {
            final double[] values = points[i];
            double lower = Double.POSITIVE_INFINITY;
            double upper = Double.NEGATIVE_INFINITY;
            for (int j=0; j<numPoints; j++) {
                final double value = values[j];
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
     * Sets all control point (source, target) pairs, overwriting any previous setting.
     * The source positions are all integer coordinates in a rectangle from (0, 0, …) inclusive
     * to {@code gridSize} exclusive where {@code gridSize} is an {@code int[]} array specified
     * {@linkplain #LinearTransformBuilder(int...) at construction time}. The target positions are
     * the results of transforming all source coordinates with the given {@code gridToCRS} transform.
     *
     * @param  gridToCRS  the transform from source coordinates (grid indices) to target coordinates.
     * @throws TransformException if a coordinate value cannot be transformed.
     *
     * @see #approximate(MathTransform, Envelope)
     *
     * @since 1.1
     */
    public void setControlPoints(final MathTransform gridToCRS) throws TransformException {
        if (gridSize == null) {
            throw new IllegalStateException(Resources.format(Resources.Keys.PointsAreNotOnRegularGrid));
        }
        final int srcDim = gridToCRS.getSourceDimensions();
        if (srcDim != gridSize.length) {
            throw new org.opengis.geometry.MismatchedDimensionException(Errors.format(
                    Errors.Keys.MismatchedTransformDimension_4, "gridToCRS", 0, gridSize.length, srcDim));
        }
        final int tgtDim = gridToCRS.getTargetDimensions();
        allocate(tgtDim);
        final int width       = gridSize[0];        // We will transform points by chunks of 1 line.
        final int widthTarget = width * tgtDim;
        final double[] buffer = new double[Math.max(srcDim, tgtDim) * width];
        final double[] point  = new double[srcDim];
        for (int j=0; j<gridLength; j += width) {
            // Prepare one row of coordinates with only x varying: (0,y,z), (1,y,z), (2,y,z), (3,y,z), etc.
            for (int i=0; i<width; i++) {
                point[0] = i;
                System.arraycopy(point, 0, buffer, i*srcDim, srcDim);
            }
            // Transform and store the result in the target arrays.
            gridToCRS.transform(buffer, 0, buffer, 0, width);
            for (int dim=0; dim<tgtDim; dim++) {
                final double[] target = targets[dim];
                int i = j;
                for (int t=dim; t < widthTarget; t += tgtDim) {
                    target[i++] = buffer[t];
                }
            }
            // Increment y coordinaate, then z (if any) if needed.
            for (int i=1; i<gridSize.length; i++) {
                if (point[i]++ < gridSize[i]) break;
                point[i] = 0;
            }
        }
    }

    /**
     * Sets all control point (source, target) pairs, overwriting any previous setting.
     * The source positions are the keys in given map, and the target positions are the associated values.
     * The map should not contain two entries with the same source position.
     * Coordinate reference systems are ignored.
     * Null positions are silently ignored.
     * Positions with NaN or infinite coordinates cause an exception to be thrown.
     *
     * <p>All source positions shall have the same number of dimensions (the <i>source dimension</i>),
     * and all target positions shall have the same number of dimensions (the <i>target dimension</i>).
     * However, the source dimension does not need to be the same as the target dimension.
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
     * @throws IllegalStateException if {@link #create(MathTransformFactory) create(…)} has already been invoked.
     * @throws IllegalArgumentException if the given positions contain NaN or infinite coordinate values.
     * @throws IllegalArgumentException if this builder has been {@linkplain #LinearTransformBuilder(int...)
     *         created for a grid} but some source coordinates are not indices in that grid.
     * @throws MismatchedDimensionException if some positions do not have the expected number of dimensions.
     *
     * @since 0.8
     */
    public void setControlPoints(final Map<? extends DirectPosition, ? extends DirectPosition> sourceToTarget)
            throws MismatchedDimensionException
    {
        ensureModifiable();
        sources    = null;
        targets    = null;
        numPoints  = 0;
        int srcDim = 0;
        int tgtDim = 0;
        for (final Map.Entry<? extends DirectPosition, ? extends DirectPosition> entry : sourceToTarget.entrySet()) {
            final DirectPosition src = entry.getKey();   if (src == null) continue;
            final DirectPosition tgt = entry.getValue(); if (tgt == null) continue;
            /*
             * The first time that we get a non-null source and target coordinate, allocate the arrays.
             * The sources arrays are allocated only if the source coordinates are randomly distributed.
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
                    isValid &= Double.isFinite(sources[i][index] = src.getCoordinate(i));
                }
            }
            for (int i=0; i<tgtDim; i++) {
                isValid &= Double.isFinite(targets[i][index] = tgt.getCoordinate(i));
            }
            /*
             * If the point contains some NaN or infinite coordinate values, it is okay to leave it as-is
             * (without incrementing `numPoints`) provided that we ensure that at least one value is NaN.
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
     * <p>If {@link #linearizer()} returns a non-empty value,
     * then the values in the returned map are projected using that linearizer.
     * This may happen only after {@link #create(MathTransformFactory) create(…)} has been invoked.</p>
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
            final var pos = new GeneralDirectPosition(data.length);
            for (int i=0; i<data.length; i++) pos.setCoordinate(i, data[i][offset]);
            return pos;
        }

        /**
         * Returns the number of points to consider when searching in {@link #sources} or {@link #targets} arrays.
         * For gridded data we cannot rely on {@link #numPoints} because the coordinate values may be at any index,
         * not necessarily at consecutive indices.
         */
        int domain() {
            return gridLength;
        }

        /**
         * Returns the index of the given coordinates in the given data array (source or target coordinates).
         * This method is a copy of {@link LinearTransformBuilder#search(int[])}, but working on real values
         * instead of integers and capable to work on {@link #targets} as well as {@link #sources}.
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
                        if (coord[i] != data[i][j]) {           // Intentionally want `false` for NaN values.
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
            return (value instanceof DirectPosition) && search(targets, ((DirectPosition) value).getCoordinates()) >= 0;
        }

        /**
         * Returns {@code true} if the given value is one of the source coordinates.
         * This method is fast on gridded data, but requires linear scan on non-gridded data.
         */
        @Override
        public final boolean containsKey(final Object key) {
            return (key instanceof DirectPosition) && flatIndex((DirectPosition) key) >= 0;
        }

        /**
         * Returns the target point for the given source point.
         * This method is fast on gridded data, but requires linear scan on non-gridded data.
         */
        @Override
        public final DirectPosition get(final Object key) {
            if (key instanceof DirectPosition) {
                final int index = flatIndex((DirectPosition) key);
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
                        final double coordinate = source.getCoordinate(i);
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
                    final var pos = new GeneralDirectPosition(dim);
                    int offset = index;
                    for (int i=0; i<dim; i++) {
                        final int size = gridSize[i];
                        pos.setCoordinate(i, offset % size);
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
            return search(sources, source.getCoordinates());
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
     * <h4>Performance note</h4>
     * Current implementation is efficient for builders {@linkplain #LinearTransformBuilder(int...) created for a grid}
     * but inefficient for builders {@linkplain #LinearTransformBuilder() created for randomly distributed points}.
     * In the latter case, the {@link #setControlPoints(Map)} method is a more efficient alternative.
     *
     * @param  source  the source coordinates. If this builder has been created with the {@link #LinearTransformBuilder(int...)} constructor,
     *                 then for every index <var>i</var> the {@code source[i]} value shall be in the [0 … {@code gridSize[i]}-1] range inclusive.
     *                 If this builder has been created with the {@link #LinearTransformBuilder()} constructor, then no constraint apply.
     * @param  target  the target coordinates, assumed uncertain.
     * @throws IllegalStateException if {@link #create(MathTransformFactory) create(…)} has already been invoked.
     * @throws IllegalArgumentException if this builder has been {@linkplain #LinearTransformBuilder(int...) created for a grid}
     *         but some source coordinates are out of index range, or if {@code target} contains NaN of infinite numbers.
     * @throws MismatchedDimensionException if the source or target position does not have the expected number of dimensions.
     *
     * @since 0.8
     */
    public void setControlPoint(final int[] source, final double[] target) {
        ensureModifiable();
        verifySourceDimension(source.length);
        final int tgtDim = target.length;
        if (targets != null && tgtDim != targets.length) {
            throw new org.opengis.geometry.MismatchedDimensionException(Errors.format(
                    Errors.Keys.MismatchedDimension_3, "target", targets.length, tgtDim));
        }
        int index;
        if (gridSize != null) {
            index = flatIndex(source);        // Invoked first for validating argument before to allocate arrays.
            if (targets == null) {
                allocate(tgtDim);
            }
            if (Double.isNaN(targets[0][index])) {
                numPoints++;
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
        if (!isValid) {
            numPoints--;
            for (int i=0; i<tgtDim; i++) {
                targets[i][index] = Double.NaN;
            }
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalMapping_2,
                                               source, new DirectPositionView.Double(target)));
        }
    }

    /**
     * Returns a single target coordinate for the given source coordinate, or {@code null} if none.
     * This method can be used for retrieving points set by previous calls to
     * {@link #setControlPoint(int[], double[])} or {@link #setControlPoints(Map)}.
     *
     * <p>If {@link #linearizer()} returns a non-empty value, then the returned values are projected using that linearizer.
     * This may happen only if this method is invoked after {@link #create(MathTransformFactory) create(…)}.</p>
     *
     * <h4>Performance note</h4>
     * Current implementation is efficient for builders {@linkplain #LinearTransformBuilder(int...) created for a grid}
     * but inefficient for builders {@linkplain #LinearTransformBuilder() created for randomly distributed points}.
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
         * Note that the coordinate may have only one NaN value, not necessarily
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
     * Sets all control points. This method can be invoked only for points on a grid.
     * The length of given vectors must be equal to the total number of cells in the grid.
     * The first vector provides the <var>x</var> coordinates; the second vector provides the <var>y</var> coordinates,
     * <i>etc.</i>. Coordinates are stored in row-major order (column index varies faster, followed by row index).
     *
     * @param  coordinates coordinates in each target dimensions, stored in row-major order.
     * @throws IllegalStateException if {@link #create(MathTransformFactory) create(…)} has already been invoked.
     */
    final void setControlPoints(final Vector[] coordinates) {
        // ensureModifiable() invoked by LocalizationGridBuilder; it does not need to be invoked again here.
        assert gridSize != null;
        final int tgtDim = coordinates.length;
        final double[][] result = new double[tgtDim][];
        for (int i=0; i<tgtDim; i++) {
            final Vector c = coordinates[i];
            ArgumentChecks.ensureNonNullElement("coordinates", i, c);
            int size = c.size();
            if (size == gridLength) {
                size = (result[i] = c.doubleValues()).length;
                if (size == gridLength) {                       // Paranoiac check in case user overwrite Vector.size().
                    continue;
                }
            }
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnexpectedArrayLength_2, gridLength, size));
        }
        targets = result;
        numPoints = gridLength;
    }

    /**
     * More straightforward version of {@link #getControlPoint(int[])} for the case where this
     * {@code LinearTransformBuilder} is known to have been built for grid source coordinates.
     * This method is for {@link LocalizationGridBuilder#create(MathTransformFactory)} internal usage.
     *
     * @param  source  the source coordinates.
     * @param  target  where to store target coordinates for a full row.
     */
    final void getControlRow(final int[] source, final double[] target) {
        assert gridSize != null;
        final int start  = flatIndex(source);
        final int stop   = start + gridSize[0];
        final int tgtDim = targets.length;
        for (int j=0; j<tgtDim; j++) {
            int index = j;
            final double[] row = targets[j];
            for (int i=start; i<stop; i++) {
                target[index] = row[i];
                index += tgtDim;
            }
        }
    }

    /**
     * Returns the coordinates of a single row or column in the given dimension. This method can be invoked
     * only when this {@code LinearTransformBuilder} is known to have been built for grid source coordinates.
     * While this method is primarily for row and columns, it can be generalized to more dimensions.
     *
     * <p>The returned vector is a view; changes in the returned vector will be reflected in this builder.</p>
     *
     * @param  dimension  the dimension of source point for which to get coordinate values.
     * @param  start      index of the first coordinate value to get.
     * @param  direction  0 for getting a row, 1 for getting a column.
     * @return coordinate values for specified row or column in the given dimension.
     */
    final Vector getTransect(final int dimension, final int[] start, final int direction) {
        final int first = flatIndex(start);
        int step = 1;
        for (int i=0; i<direction; i++) {
            step *= gridSize[i];
        }
        return Vector.create(targets[dimension]).subSampling(first, step, gridSize[direction] - start[direction]);
    }

    /**
     * Tries to remove discontinuities in coordinates values caused by anti-meridian crossing. This is the implementation of
     * {@link LocalizationGridBuilder#resolveWraparoundAxis(int, int, double)} public method. See that method for javadoc.
     *
     * @param  dimension  the dimension to process, from 0 inclusive to {@link #getTargetDimensions()} exclusive.
     *                    This is 0 for longitude dimension in a (<var>longitudes</var>, <var>latitudes</var>) grid.
     * @param  direction  the direction to walk through: 0 for columns or 1 for rows (higher dimensions are also possible).
     *                    Value can be from 0 inclusive to {@link #getSourceDimensions()} exclusive.
     *                    The recommended direction is the direction of most stable values, typically 1 (rows) for longitudes.
     * @param  period     that wraparound range (typically 360° for longitudes). Must be strictly positive.
     * @return the range of coordinate values in the specified dimension after correction for wraparound values.
     * @throws IllegalStateException if {@link #create(MathTransformFactory) create(…)} has already been invoked.
     */
    final NumberRange<Double> resolveWraparoundAxis(final int dimension, final int direction, final double period) {
        // ensureModifiable() invoked by LocalizationGridBuilder; it does not need to be invoked again here.
        final double[] coordinates = targets[dimension];
        int stride = 1;
        for (int i=0; i<direction; i++) {
            stride *= gridSize[i];                              // Index offset for moving to next value in the specified direction.
        }
        final int page = stride * gridSize[direction];          // Index offset for moving to next row or whatever is the next dimension.
        final double threshold = period / 2;
        double minValue = Double.POSITIVE_INFINITY;
        double maxValue = Double.NEGATIVE_INFINITY;
        double minAfter = Double.POSITIVE_INFINITY;
        double maxAfter = Double.NEGATIVE_INFINITY;
        double previous = coordinates[0];
        for (int x=0; x<stride; x++) {                          // For iterating over dimensions lower than `dimension`.
            for (int y=0; y<gridLength; y += page) {            // For iterating over dimensions greater than `dimension`.
                final int stop = y + page;
                for (int i = x+y; i<stop; i += stride) {
                    double value = coordinates[i];
                    if (value < minValue) minValue = value;
                    if (value > maxValue) maxValue = value;
                    double delta = value - previous;
                    if (Math.abs(delta) > threshold) {
                        delta = Math.rint(delta / period) * period;
                        value -= delta;
                        coordinates[i] = value;
                    }
                    previous = value;
                    if (value < minAfter) minAfter = value;
                    if (value > maxAfter) maxAfter = value;
                }
                /*
                 * For the next scan, use as a reference the first value of this scan. If our scan direction is 0
                 * (each value compared with the value in previous column), then the first value of next row will
                 * be compared with the first value of this row. This is illustrated by index -1 below:
                 *
                 *    ┌───┬───┬───┬───┬───┬───┐
                 *    │-1 │   │   │   │   │   │      coordinates[x]
                 *    ├───┼───┼───┼───┼───┼───┤
                 *    │ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │      next row to be scanned
                 *    └───┴───┴───┴───┴───┴───┘
                 *
                 * Since the direction given in argument is the direction of most stable values, the perpendicular
                 * direction used for coordinates[x] may have more variation. We assume that those variations are
                 * still small enough for taking that nearby value as a reference.
                 */
                previous = coordinates[x];
            }
        }
        /*
         * If some coordinates have been shifted, the range may become unreasonable. For example, we may get a range
         * of [-440 … -160]° of longitude. Shift again in the direction that provide the best intersection with the
         * original range. Note that original range itself is sometimes "unreasonable". In that case we fallback on
         * values centered around zero, which matches common practice and reduces the risk of rounding errors.
         */
        double shift = 0;
        if (maxValue - minValue > period) {
            minValue = -period;
            maxValue = +period;
        }
        final double Δmin = minValue - minAfter;
        final double Δmax = maxValue - maxAfter;
        if (Δmin != 0 || Δmax != 0) {
            double intersection = 0;
            final double minCycles = Math.floor(Math.min(Δmin, Δmax) / period);
            final double maxCycles = Math.ceil (Math.max(Δmin, Δmax) / period);
            for (double cycles = minCycles; cycles <= maxCycles; cycles++) {
                final double s = cycles * period;
                final double p = Math.min(maxValue, maxAfter + s) - Math.max(minValue, minAfter + s);
                if (p > intersection) {
                    intersection = p;
                    shift = s;
                }
            }
            if (shift != 0) {
                for (int i=0; i<coordinates.length; i++) {
                    coordinates[i] += shift;
                }
            }
        }
        return NumberRange.create(minAfter + shift, true, maxAfter + shift, true);
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
     * Adds transforms to potentially apply on target control points before to compute the linear transform.
     * This method can be invoked when the <i>source to target</i> transform would possibly be more
     * linear if <i>target</i> was another space than the {@linkplain #getTargetEnvelope() current one}.
     * If linearizers have been specified, then the {@link #create(MathTransformFactory)} method will try to
     * apply each transform on target coordinates and check which one get the best
     * {@linkplain #correlation() correlation} coefficients.
     *
     * <p>Exactly one of the specified transforms will be selected. If applying no transform is an acceptable solution,
     * then an {@linkplain org.apache.sis.referencing.operation.transform.MathTransforms#identity(int) identity transform}
     * should be included in the given {@code projections} map. The transform selected by {@code LinearTransformBuilder}
     * will be given by {@link #linearizer()}.</p>
     *
     * <p>Linearizers are specified as a collection of {@link MathTransform}s from current {@linkplain #getTargetEnvelope()
     * target coordinates} to some other spaces where <i>sources to new targets</i> transforms may be more linear.
     * Keys in the map are arbitrary identifiers.
     * Values in the map should be non-linear transforms; {@link LinearTransform}s (other than identity)
     * should be avoided because they will consume processing power for no correlation improvement.</p>
     *
     * <h4>Error handling</h4>
     * If a {@link org.opengis.referencing.operation.TransformException} occurred or if some transform results
     * were NaN or infinite, then the {@link MathTransform} that failed will be ignored. If all transforms fail,
     * then a {@link FactoryException} will be thrown by the {@code create(…)} method.
     *
     * <h4>Dimensions mapping</h4>
     * The {@code projToGrid} argument maps {@code projections} dimensions to target dimensions of this builder.
     * For example if {@code projToGrid} array is {@code {2,1}}, then coordinate values in target dimensions 2 and 1
     * of this grid will be used as source coordinates in dimensions 0 and 1 respectively for all given projections.
     * Likewise, the projection results in dimensions 0 and 1 of all projections will be stored in target dimensions
     * 2 and 1 respectively of this grid.
     *
     * <p>The {@code projToGrid} argument can be omitted or null, in which case {0, 1, 2 …
     * {@link #getTargetDimensions()} - 1} is assumed. All given {@code projections} shall have
     * a number of source and target dimensions equals to the length of the {@code projToGrid} array.
     * It is possible to invoke this method many times with different {@code projToGrid} argument values.</p>
     *
     * @param  projections  projections from current target coordinates to other spaces which may result in more linear transforms.
     * @param  projToGrid   the target dimensions to project, or null or omitted for projecting all target dimensions in same order.
     * @throws IllegalStateException if {@link #create(MathTransformFactory) create(…)} has already been invoked.
     * @throws MismatchedDimensionException if a projection does not have the expected number of dimensions.
     *
     * @see #linearizer()
     * @see #correlation()
     *
     * @since 1.0
     */
    public void addLinearizers(final Map<String,MathTransform> projections, final int... projToGrid) {
        addLinearizers(Objects.requireNonNull(projections), false, projToGrid);
    }

    /**
     * Implementation of {@link #addLinearizers(Map, int...)} with a flag telling whether the inverse of selected projection
     * shall be concatenated to the final transform. This method is non-public because the {@code reverseAfterLinearization}
     * flag has no purpose for this {@link LinearTransformBuilder} class; it is useful only for {@link LocalizationGridBuilder}.
     *
     * @see ProjectedTransformTry#reverseAfterLinearization
     */
    final void addLinearizers(final Map<String,MathTransform> projections, final boolean compensate, int[] projToGrid) {
        ensureModifiable();
        final int tgtDim = getTargetDimensions();
        if (projToGrid == null || projToGrid.length == 0) {
            projToGrid = ArraysExt.range(0, tgtDim);
        } else {
            long defined = 0;
            projToGrid = projToGrid.clone();
            for (final int d : projToGrid) {
                if (defined == (defined |= Numerics.bitmask(Objects.checkIndex(d, tgtDim)))) {
                    // Note: if d ≥ 64, there will be no check (mask = 0).
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedNumber_1, d));
                }
            }
        }
        if (linearizers == null) {
            linearizers = new ArrayList<>(projections.size());
        }
        for (final Map.Entry<String,MathTransform> entry : projections.entrySet()) {
            linearizers.add(new ProjectedTransformTry(entry.getKey(), entry.getValue(), projToGrid, tgtDim, compensate));
        }
    }

    /**
     * Sets the linearizers to a copy of those of the given builder.
     * This is used by copy constructors.
     *
     * @see LocalizationGridBuilder#LocalizationGridBuilder(LinearTransformBuilder)
     */
    final void setLinearizers(final LinearTransformBuilder other) {
        if (other.linearizers != null) {
            linearizers = new ArrayList<>(other.linearizers);
            linearizers.replaceAll(ProjectedTransformTry::new);
        }
    }

    /**
     * Creates a linear transform approximation from the source positions to the target positions.
     * This method assumes that source positions are precise and that all uncertainties are in target positions.
     * If {@linkplain #addLinearizers linearizers have been specified}, then this method will project all target
     * coordinates using one of those linearizers in order to get a more linear transform.
     * If such projection is applied, then {@link #linearizer()} will return a non-empty value after this method call.
     *
     * <p>If this method is invoked more than once, the previously created transform instance is returned.</p>
     *
     * @param  factory  the factory to use for creating the transform, or {@code null} for the default factory.
     *                  The {@link MathTransformFactory#createAffineTransform(Matrix)} method of that factory
     *                  shall return {@link LinearTransform} instances.
     * @return the fitted linear transform.
     * @throws FactoryException if the transform cannot be created,
     *         for example because the source or target points have not be specified.
     *
     * @since 0.8
     */
    @Override
    public LinearTransform create(final MathTransformFactory factory) throws FactoryException {
        if (transform == null) {
            MatrixSIS bestTransform;
            if (linearizers == null || linearizers.isEmpty()) {
                bestTransform = fit();
            } else {
                /*
                 * We are going to try to project target coordinates in search for the most linear transform.
                 * If a projection allows better results, the following variables will be set to values to assign
                 * to this `LinearTransformBuilder` after the loop. We do not assign new values to this builder
                 * directly (as we find them) in the loop because the search for a better transform requires the
                 * original values.
                 */
                           bestTransform     = null;
                double     bestCorrelation   = 0;
                double[]   bestCorrelations  = null;
                double[][] transformedArrays = null;
                final double sqrtCorrLength  = Math.sqrt(targets.length);   // For `bestCorrelation` calculation.
                /*
                 * If one of the transforms is identity, we can do the computation directly on `this` because the
                 * `targets` arrays do not need to be transformed. This special case avoids the need to allocate
                 * arrays from the `pool` and to copy data.
                 */
                ProjectedTransformTry identity = null;
                for (final ProjectedTransformTry alt : linearizers) {
                    if (alt.projection.isIdentity()) {
                        bestTransform     = fit();
                        bestCorrelations  = correlations;
                        bestCorrelation   = rms(bestCorrelations, sqrtCorrLength);
                        transformedArrays = targets;
                        appliedLinearizer = alt;
                        identity          = alt;
                        alt.correlation   = (float) bestCorrelation;
                        break;
                    }
                }
                /*
                 * `tmp` and `pool` are temporary objects for this computation only. We use a pool because the
                 * `double[]` arrays may be large (e.g. megabytes) and we want to avoid creating new arrays of
                 * such size for each projection to try.
                 */
                final var pool = new ArrayDeque<double[]>();
                final var tmp = new LinearTransformBuilder(this);
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final int numPoints = (gridLength != 0) ? gridLength : this.numPoints;
                boolean needTargetReplace = false;
                for (final ProjectedTransformTry alt : linearizers) {
                    if (alt == identity || (tmp.targets = alt.transform(targets, numPoints, pool)) == null) {
                        continue;
                    }
                    /*
                     * At this point, a transformation has been successfully applied on the target arrays of `tmp`.
                     * If we never invoked `fit()` before, its first call must be done with all dimensions in `tmp`,
                     * not only the dimensions on which we apply the `MathTransform`.
                     */
                    if (bestTransform == null) {
                        transformedArrays = tmp.targets = alt.replaceTransformed(targets, tmp.targets);
                        bestTransform     = tmp.fit();
                        bestCorrelations  = tmp.correlations;
                        bestCorrelation   = rms(bestCorrelations, sqrtCorrLength);
                        alt.correlation   = (float) bestCorrelation;
                        appliedLinearizer = alt;
                    } else {
                        /*
                         * For all invocations of `fit()` after the first one (including the identity case if any),
                         * we need to do calculation only on the dimensions on which `MathTransform` operates because
                         * calculation on other dimensions will be unchanged.
                         */
                        final MatrixSIS altTransform    = tmp.fit();
                        final double[]  altCorrelations = alt.replaceTransformed(bestCorrelations, tmp.correlations);
                        final double    altCorrelation  = rms(altCorrelations, sqrtCorrLength);
                        alt.correlation = (float) altCorrelation;
                        if (altCorrelation > bestCorrelation) {
                            ProjectedTransformTry.recycle(transformedArrays, pool);
                            transformedArrays = tmp.targets;
                            bestCorrelation   = altCorrelation;
                            bestCorrelations  = altCorrelations;
                            bestTransform     = alt.replaceTransformed(bestTransform, altTransform);
                            appliedLinearizer = alt;
                            needTargetReplace = true;
                        } else {
                            ProjectedTransformTry.recycle(tmp.targets, pool);
                        }
                    }
                }
                /*
                 * Finished to try all transforms. If all of them failed, wrap the `TransformException`.
                 * We use a sub-type of `FactoryException` which allows callers to add their own information.
                 * For example, the caller may know that the grid was possibly out of CRS domain of validity
                 * and wanted to try anyway (it can be difficult to predict in advance if it will work).
                 */
                if (bestTransform == null) {
                    throw new LocalizationGridException(Resources.format(Resources.Keys.CanNotLinearizeLocalizationGrid),
                                                        ProjectedTransformTry.getError(linearizers));
                }
                if (needTargetReplace) {
                    transformedArrays = appliedLinearizer.replaceTransformed(targets, transformedArrays);
                }
                targets      = transformedArrays;
                correlations = bestCorrelations;
            }
            // Set only on success.
            transform = (LinearTransform) nonNull(factory).createAffineTransform(bestTransform);
        }
        return transform;
    }

    /**
     * Computes the matrix of the linear approximation. This is the implementation of {@link #create(MathTransformFactory)}
     * without the step creating the {@link LinearTransform} from a matrix. The {@link #correlations} field is set as a side
     * effect of this method call.
     *
     * <p>In current implementation, the transform represented by the returned matrix is always affine
     * (i.e. the last row is fixed to [0 0 … 0 1]). If this is no longer the case in a future version,
     * some codes may not work anymore. Search for {@code isAffine()} statements for locating codes
     * that depend on affine transform assumption.</p>
     */
    private MatrixSIS fit() throws FactoryException {
        // Protect `sources` and `targets` against accidental changes.
        @SuppressWarnings("LocalVariableHidesMemberVariable") final double[][] sources = this.sources;
        @SuppressWarnings("LocalVariableHidesMemberVariable") final double[][] targets = this.targets;
        if (targets == null) {
            throw new InvalidGeodeticParameterException(noData());
        }
        final int sourceDim = (sources != null) ? sources.length : gridSize.length;
        final int targetDim = targets.length;
        correlations = new double[targetDim];
        final MatrixSIS matrix = Matrices.create(targetDim + 1, sourceDim + 1,  ExtendedPrecisionMatrix.CREATE_ZERO);
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
                                     Vector.create(targets[j]));
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
                        c = plan.fit(gridSize[0], gridSize[1], Vector.create(targets[j]));
                    } catch (IllegalArgumentException e) {
                        // This may happen if the z vector still contain some "NaN" values.
                        throw new InvalidGeodeticParameterException(noData(), e);
                    }
                    break;
                }
                default: {
                    throw new InvalidGeodeticParameterException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, sourceDim));
                }
            }
            correlations[j] = c;
        }
        return matrix;
    }

    /**
     * Wraps the given array in a vector of length {@link #numPoints}. This method should be
     * invoked only when this builder has been created by {@link #LinearTransformBuilder()}.
     * This can be identified by {@code sources != null} or {@code gridSize == null}.
     */
    private Vector vector(final double[] data) {
        assert gridSize == null;
        return Vector.create(data).subList(0, numPoints);
    }

    /**
     * Returns a global estimation of correlation by computing the root mean square of values.
     */
    private static double rms(final double[] correlations, final double sqrtCorrLength) {
        return org.apache.sis.math.MathFunctions.magnitude(correlations) / sqrtCorrLength;
    }

    /**
     * If target coordinates have been projected to another space, returns that projection.
     * This method returns a non-empty value if {@link #addLinearizers(Map, int...)} has been
     * invoked with a non-empty map, followed by a {@link #create(MathTransformFactory)} call.
     * In such case, {@code LinearTransformBuilder} selects a linearizer identified by the returned
     * <var>key</var> - <var>value</var> entry. The entry key is one of the keys of the maps given
     * to {@code addLinearizers(…)}. The entry value is the associated {@code MathTransform},
     * possibly modified as described in the <i>axis order</i> section below.
     *
     * <p>The envelope returned by {@link #getTargetEnvelope()} and all control points
     * returned by {@link #getControlPoint(int[])} are projected by the selected transform.
     * Consequently, if the target coordinates of original control points are desired,
     * then the transform returned by {@code create(…)} needs to be concatenated with
     * the {@linkplain MathTransform#inverse() inverse} of the transform returned by
     * this {@code linearizer()} method.</p>
     *
     * <h4>Axis order</h4>
     * The source coordinates expected by the returned transform are the {@linkplain #getControlPoint(int[])
     * control points target coordinates}. The returned transform will contain an operation step performing
     * axis filtering and swapping implied by the {@code projToGrid} argument that was given to the
     * <code>{@linkplain #addLinearizers(Map, int...) addLinearizers}(…, projToGrid)}</code> method.
     * Consequently, if the {@code projToGrid} argument was not an arithmetic progression,
     * then the transform returned by this method will not be one of the instances given to
     * {@code addLinearizers(…)}.
     *
     * @return the projection applied on target coordinates before to compute a linear transform.
     *
     * @since 1.1
     */
    public Optional<Map.Entry<String,MathTransform>> linearizer() {
        return Optional.ofNullable(appliedLinearizer);
    }

    /**
     * Returns linearizer which has been applied, or {@code null} if none.
     */
    final ProjectedTransformTry appliedLinearizer() {
        return appliedLinearizer;
    }

    /**
     * Returns the Pearson correlation coefficients of the transform created by {@link #create create(…)}.
     * The closer those coefficients are to +1 or -1, the better the fit.
     * This method returns {@code null} if {@code create(…)} has not yet been invoked.
     * If non-null, the array length is equal to the number of target dimensions.
     *
     * @return estimation of Pearson correlation coefficients for each target dimension,
     *         or {@code null} if {@code create(…)} has not been invoked yet.
     */
    public double[] correlation() {
        return (correlations != null) ? correlations.clone() : null;
    }

    /**
     * Returns the transform computed by {@link #create(MathTransformFactory)},
     * of {@code null} if that method has not yet been invoked.
     */
    final LinearTransform transform() {
        return transform;
    }

    /**
     * Returns a string representation of this builder for debugging purpose.
     * Current implementation shows the following information:
     *
     * <ul>
     *   <li>Number of points.</li>
     *   <li>Linearizers and their correlation coefficients (if available).</li>
     *   <li>The linear transform (if already computed).</li>
     * </ul>
     *
     * The string representation may change in any future version.
     *
     * @return a string representation of this builder.
     */
    @Override
    public String toString() {
        final var buffer = new StringBuilder(400);
        final String lineSeparator;
        try {
            lineSeparator = appendTo(buffer, getClass(), null, Vocabulary.Keys.Result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Strings.insertLineInLeftMargin(buffer, lineSeparator);
        return buffer.toString();
    }

    /**
     * Appends a string representation of this builder into the given buffer.
     *
     * @param  buffer     where to append the string representation.
     * @param  caller     the class name to report.
     * @param  locale     the locale for formatting messages and some numbers, or {@code null} for the default.
     * @param  resultKey  either {@code Vocabulary.Keys.Result} or {@code Vocabulary.Keys.LinearTransformation}.
     * @return the line separator, for convenience of callers who wants to append more content.
     * @throws IOException should never happen because we write in a {@link StringBuilder}.
     */
    final String appendTo(final StringBuilder buffer, final Class<?> caller, final Locale locale, final short resultKey) throws IOException {
        final String lineSeparator = System.lineSeparator();
        final Vocabulary vocabulary = Vocabulary.forLocale(locale);
        buffer.append(Classes.getShortName(caller)).append('[').append(numPoints).append(" points");
        if (gridSize != null) {
            String separator = " on ";
            for (final int size : gridSize) {
                buffer.append(separator).append(size);
                separator = " × ";
            }
            buffer.append(" grid");
        }
        buffer.append(']').append(lineSeparator);
        /*
         * Example (from LinearTransformBuilderTest):
         * ┌────────────┬─────────────┐
         * │ Conversion │ Correlation │
         * ├────────────┼─────────────┤
         * │ x³ y²      │ 1.000000    │
         * │ x² y³      │ 0.997437    │
         * │ Identité   │ 0.995969    │
         * └────────────┴─────────────┘
         */
        if (linearizers != null) {
            final var alternatives = linearizers.toArray(ProjectedTransformTry[]::new);
            Arrays.sort(alternatives);
            buffer.append(Strings.CONTINUATION_ITEM);
            vocabulary.appendLabel(Vocabulary.Keys.Preprocessing, buffer);
            buffer.append(lineSeparator);
            NumberFormat nf = null;
            final var table = new TableAppender(buffer, " │ ");
            table.appendHorizontalSeparator();
            table.append(vocabulary.getString(Vocabulary.Keys.Conversion)).nextColumn();
            table.append(vocabulary.getString(Vocabulary.Keys.Correlation)).nextLine();
            table.appendHorizontalSeparator();
            for (final ProjectedTransformTry alt : alternatives) {
                nf = alt.summarize(table, nf, locale);
            }
            table.appendHorizontalSeparator();
            table.flush();
        }
        /*
         * Example:
         * Result:
         * ┌               ┐                 ┌        ┐
         * │ 2.0  0    3.0 │   Correlation = │ 0.9967 │
         * │ 0    1.0  1.0 │                 │ 0.9950 │
         * │ 0    0    1   │                 └        ┘
         * └               ┘
         */
        if (transform != null) {
            buffer.append(Strings.CONTINUATION_ITEM);
            vocabulary.appendLabel(resultKey, buffer);
            buffer.append(lineSeparator);
            final var table = new TableAppender(buffer, " ");
            table.setMultiLinesCells(true);
            table.append(Matrices.toString(transform.getMatrix())).nextColumn();
            table.append(lineSeparator).append("  ")
                 .append(vocabulary.getString(Vocabulary.Keys.Correlation)).append(" =").nextColumn();
            table.append(Matrices.create(correlations.length, 1, correlations).toString());
            table.flush();
        }
        return lineSeparator;
    }
}
