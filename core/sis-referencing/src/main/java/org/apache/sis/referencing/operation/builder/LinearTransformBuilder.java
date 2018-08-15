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
import java.io.IOException;
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
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;
import org.apache.sis.internal.referencing.Resources;
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
     * The arrays of source ordinate values. Accessed with indices in that order: {@code sources[dimension][point]}.
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
     * The arrays of target ordinate values. Accessed with indices in that order: {@code targets[dimension][point]}.
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
     * Note that the "valid" positions may contain {@link Double#NaN} ordinate values.
     * This field is only indicative if this {@code LinearTransformBuilder} instance
     * has been created by {@link #LinearTransformBuilder(int...)}.
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
     * This constructor notifies {@code LinearTransformBuilder} that ordinate values of all source positions will
     * be integers in the [0 … {@code gridSize[0]}-1] range for the first dimension (typically column indices),
     * in the [0 … {@code gridSize[1]}-1] range for the second dimension (typically row indices), <i>etc.</i>
     * The dimension of all source positions is the length of the given {@code gridSize} array.
     *
     * <p>An empty array is equivalent to invoking the no-argument constructor,
     * i.e. no restriction is put on the source coordinates.</p>
     *
     * @param  gridSize  the number of integer ordinate values in each grid dimension.
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
     * can not be used.
     */
    private int search(final int[] source) {
        assert gridSize == null;         // This method should not be invoked for points distributed on a grid.
search: for (int j=0; j<numPoints; j++) {
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
     * @throws IllegalArgumentException if an ordinate value is illegal.
     */
    private int flatIndex(final int[] source) {
        assert sources == null;               // This method should not be invoked for randomly distributed points.
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
     * @throws IllegalArgumentException if an ordinate value is illegal.
     */
    private int flatIndex(final DirectPosition source) {
        assert sources == null;               // This method should not be invoked for randomly distributed points.
        int offset = 0;
        for (int i = gridSize.length; i != 0;) {
            final int size = gridSize[--i];
            final double ordinate = source.getOrdinate(i);
            final int index = (int) ordinate;
            if (index != ordinate) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.NotAnInteger_1, ordinate));
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
     * that positions are stored in this builder as they are read from user-provided collection.
     */
    private String mismatchedDimension(final String name, final int expected, final int actual) {
        return Errors.format(Errors.Keys.MismatchedDimension_3, name + '[' + numPoints + ']', expected, actual);
    }

    /**
     * Returns the error message to be given to {@link IllegalStateException} when there is no data.
     */
    private static String noData() {
        return Resources.format(Resources.Keys.MissingValuesInLocalizationGrid);
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
     * Returns the envelope of source points. The lower and upper values are inclusive.
     *
     * @return the envelope of source points.
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
        for (int i=0; i <dim; i++) {
            final double[] data = points[i];
            double lower = Double.POSITIVE_INFINITY;
            double upper = Double.NEGATIVE_INFINITY;
            for (final double value : data) {
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
     *
     * <p>All source positions shall have the same number of dimensions (the <cite>source dimension</cite>),
     * and all target positions shall have the same number of dimensions (the <cite>target dimension</cite>).
     * However the source dimension does not need to be the same the target dimension.
     * Apache SIS currently supports only one- or two-dimensional source positions,
     * together with arbitrary target dimension.</p>
     *
     * <p>If this builder has been created with the {@link #LinearTransformBuilder(int...)} constructor,
     * then the ordinate values of all source positions shall be integers in the [0 … {@code gridSize[0]}-1]
     * range for the first dimension (typically column indices), in the [0 … {@code gridSize[1]}-1] range for
     * the second dimension (typically row indices), <i>etc</i>. This constraint does not apply for builders
     * created with the {@link #LinearTransformBuilder()} constructor.</p>
     *
     * @param  sourceToTarget  a map of source positions to target positions.
     *         Source positions are assumed precise and target positions are assumed uncertain.
     * @throws IllegalArgumentException if this builder has been {@linkplain #LinearTransformBuilder(int...)
     *         created for a grid} but some source ordinates are not indices in that grid.
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
                    throw new MismatchedDimensionException(mismatchedDimension("target", 2, tgtDim));
                }
                if (gridSize == null) {
                    srcDim = src.getDimension();
                    if (srcDim <= 0) {
                        throw new MismatchedDimensionException(mismatchedDimension("source", 2, srcDim));
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
            if ((d = src.getDimension()) != srcDim) throw new MismatchedDimensionException(mismatchedDimension("source", srcDim, d));
            if ((d = tgt.getDimension()) != tgtDim) throw new MismatchedDimensionException(mismatchedDimension("target", tgtDim, d));
            int index;
            if (gridSize != null) {
                index = flatIndex(src);
            } else {
                index = numPoints;
                for (int i=0; i<srcDim; i++) {
                    sources[i][index] = src.getOrdinate(i);
                }
            }
            for (int i=0; i<tgtDim; i++) {
                targets[i][index] = tgt.getOrdinate(i);
            }
            numPoints++;
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
     *         but some source ordinates are out of index range.
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
        for (int i=0; i<tgtDim; i++) {
            targets[i][index] = target[i];
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
     *         but some source ordinates are out of index range.
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
        boolean isNaN = true;
        final double[] target = new double[targets.length];
        for (int i=0; i<target.length; i++) {
            isNaN &= Double.isNaN(target[i] = targets[i][index]);
        }
        return isNaN ? null : target;
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
                throw new AssertionError(e);        // Should never happen since we wrote into a StringBuilder.
            }
        }
        return buffer.toString();
    }
}
