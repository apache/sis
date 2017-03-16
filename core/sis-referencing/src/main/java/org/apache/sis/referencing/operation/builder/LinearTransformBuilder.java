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
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.geometry.coordinate.Position;
import org.apache.sis.io.TableAppender;
import org.apache.sis.math.Line;
import org.apache.sis.math.Plane;
import org.apache.sis.math.Vector;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;


/**
 * Creates an affine transform which will map approximatively the given source positions to the given target positions.
 * In many cases, the <em>source</em> positions are grid indices and the <em>target</em> positions are geographic or
 * projected coordinates, but this is not mandatory. If the source positions are known to be grid indices,
 * then a builder created by the {@link #LinearTransformBuilder(int...)} constructor will be more efficient.
 * Otherwise a builder created by the {@link #LinearTransformBuilder()} constructor will be able to handle
 * randomly distributed coordinates.
 *
 * <p>The transform coefficients are determined using a <cite>least squares</cite> estimation method,
 * with the assumption that source positions are exact and all the uncertainty is in the target positions.</p>
 *
 * <div class="note"><b>Implementation note:</b>
 * The quantity that current implementation tries to minimize is not strictly the squared Euclidian distance.
 * The current implementation rather processes each <em>target</em> dimension independently, which may not give
 * the same result than if we tried to minimize the squared Euclidian distances by taking all target dimensions
 * in account together. This algorithm may change in future SIS versions.
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.8
 * @module
 *
 * @see LinearTransform
 * @see Line
 * @see Plane
 */
public class LinearTransformBuilder {
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
    private final int gridLength;

    /**
     * Number of valid positions in the {@link #sources} or {@link #targets} arrays.
     * Note that the "valid" positions may contain {@link Double#NaN} ordinate values.
     */
    private int numPoints;

    /**
     * The transform created by the last call to {@link #create()}.
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
     * the {@link #LinearTransformBuilder(int, int)} constructor will create a more efficient builder.
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
     * Returns the offset where to store a target position for the given source position.
     * This method should be invoked only when this {@code LinearTransformBuilder} has
     * been constructed for a grid of known size.
     *
     * @throws IllegalArgumentException if an ordinate value is illegal.
     */
    private int offset(final DirectPosition src) {
        int offset = 1;
        for (int j = gridSize.length; j != 0;) {
            final int s = gridSize[--j];
            final double ordinate = src.getOrdinate(j);
            final int i = (int) ordinate;
            if (i != ordinate) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.NotAnInteger_1, ordinate));
            }
            if (i < 0 || i >= s) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.ValueOutOfRange_4, "source", 0, s-1, i));
            }
            offset = offset * s + i;
        }
        return offset;
    }

    /**
     * Builds the exception message for an unexpected position dimension. This method assumes
     * that positions are stored in this builder as they are read from user-provided collection.
     */
    private String mismatchedDimension(final String name, final int expected, final int actual) {
        return Errors.format(Errors.Keys.MismatchedDimension_3, name + '[' + numPoints + ']', expected, actual);
    }

    /**
     * Returns the direct position of the given position, or {@code null} if none.
     */
    private static DirectPosition position(final Position p) {
        return (p != null) ? p.getDirectPosition() : null;
    }

    /**
     * Sets the source and target positions, overwriting any previous setting. The source positions are the keys in
     * the given map, and the target positions are the associated values in the map. The map should not contain two
     * entries with the same source position. Null positions are silently ignored.
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
     * @throws MismatchedDimensionException if some positions do not have the expected number of dimensions.
     *
     * @since 0.8
     */
    public void setPositions(final Map<? extends Position, ? extends Position> sourceToTarget)
            throws MismatchedDimensionException
    {
        ArgumentChecks.ensureNonNull("sourceToTarget", sourceToTarget);
        pendingSources = null;
        pendingTargets = null;
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
                    final int length = sourceToTarget.size();
                    sources = new double[srcDim][length];
                    targets = new double[tgtDim][length];
                } else {
                    srcDim  = gridSize.length;
                    targets = new double[tgtDim][gridLength];
                    for (final double[] r : targets) {
                        Arrays.fill(r, Double.NaN);
                    }
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
                index = offset(src);
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
     * Sets the source points, overwriting any previous setting. The number of source points will need to be the same
     * than the number of {@linkplain #setTargetPoints target points} when the {@link #create()} method will be invoked.
     * In current Apache SIS implementation, the source points must be one or two-dimensional.
     *
     * <p>If this builder has been created with the {@link #LinearTransformBuilder(int, int)} constructor,
     * then all given points must be two-dimensional and all ordinate values must be integers in the
     * [0 … <var>width</var>-1] or [0 … <var>height</var>-1] range for the first and second dimension
     * respectively. This constraint does not apply if this builder has been created with the
     * {@link #LinearTransformBuilder()} constructor.</p>
     *
     * <p>It is caller's responsibility to ensure that no source point is duplicated.
     * If the same source point is repeated twice, then {@code LinearTransformBuilder} behavior is undefined.</p>
     *
     * @param  points  the source points, assumed precise.
     * @throws MismatchedDimensionException if at least one point does not have the expected number of dimensions.
     *
     * @deprecated Replaced by {@link #setPositions(Map)}.
     */
    @Deprecated
    public void setSourcePoints(final DirectPosition... points) throws MismatchedDimensionException {
        ArgumentChecks.ensureNonNull("points", points);
        transform   = null;
        correlation = null;
        sources     = null;
        targets     = null;
        numPoints   = 0;
        pendingSources = points.clone();
    }

    /**
     * Sets the target points, overwriting any previous setting. The number of target points will need to be the same
     * than the number of {@linkplain #setSourcePoints source points} when the {@link #create()} method will be invoked.
     * Target points can have any number of dimensions (not necessarily 2), but all points shall have
     * the same number of dimensions.
     *
     * @param  points  the target points, assumed uncertain.
     * @throws MismatchedDimensionException if not all points have the same number of dimensions.
     *
     * @deprecated Replaced by {@link #setPositions(Map)}.
     */
    @Deprecated
    public void setTargetPoints(final DirectPosition... points) throws MismatchedDimensionException {
        ArgumentChecks.ensureNonNull("points", points);
        transform   = null;
        correlation = null;
        sources     = null;
        targets     = null;
        numPoints   = 0;
        pendingTargets = points.clone();
    }

    @Deprecated
    private transient DirectPosition[] pendingSources, pendingTargets;

    @Deprecated
    private void processPendings() {
        if (pendingSources != null || pendingTargets != null) {
            if (pendingSources == null || pendingTargets == null) {
                throw new IllegalStateException(Errors.format(
                        Errors.Keys.MissingValueForProperty_1, (pendingSources == null) ? "sources" : "targets"));
            }
            final int length = pendingSources.length;
            if (pendingTargets.length != length) {
                throw new IllegalStateException(Errors.format(Errors.Keys.MismatchedArrayLengths));
            }
            final Map<DirectPosition,DirectPosition> sourceToTarget = new java.util.HashMap<>(length);
            for (int i=0; i<length; i++) {
                sourceToTarget.put(pendingSources[i], pendingTargets[i]);
            }
            setPositions(sourceToTarget);
        }
    }

    /**
     * Creates a linear transform approximation from the source positions to the target positions.
     * This method assumes that source positions are precise and that all uncertainties are in the
     * target positions.
     *
     * @return the fitted linear transform.
     * @throws IllegalStateException if the source or target points have not be specified
     *         or if those two sets do not have the same number of points.
     */
    public LinearTransform create() {
        if (transform == null) {
            processPendings();
            final double[][] sources = this.sources;                    // Protect from changes.
            final double[][] targets = this.targets;
            if (targets == null) {
                throw new IllegalStateException(Errors.format(Errors.Keys.MissingValueForProperty_1, "sourceToTarget"));
            }
            final int sourceDim = (sources != null) ? sources.length : gridSize.length;
            final int targetDim = targets.length;
            correlation = new double[targetDim];
            final MatrixSIS matrix = Matrices.createZero(targetDim + 1, sourceDim + 1);
            matrix.setElement(targetDim, sourceDim, 1);
            for (int j=0; j < targetDim; j++) {
                final double c;
                switch (sourceDim) {
                    case 1: {
                        final Line line = new Line();
                        if (sources != null) {
                            c = line.fit(sources[0], targets[j]);
                        } else {
                            c = line.fit(Vector.createSequence(0, 1, gridSize[0]),
                                         Vector.create(targets[j], false));
                        }
                        matrix.setElement(j, 0, line.slope());
                        matrix.setElement(j, 1, line.y0());
                        break;
                    }
                    case 2: {
                        final Plane plan = new Plane();
                        if (sources != null) {
                            c = plan.fit(sources[0], sources[1], targets[j]);
                        } else {
                            c = plan.fit(gridSize[0], gridSize[1], Vector.create(targets[j], false));
                        }
                        matrix.setElement(j, 0, plan.slopeX());
                        matrix.setElement(j, 1, plan.slopeY());
                        matrix.setElement(j, 2, plan.z0());
                        break;
                    }
                    default: {
                        throw new UnsupportedOperationException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, sourceDim));
                    }
                }
                correlation[j] = c;
            }
            transform = MathTransforms.linear(matrix);
        }
        return transform;
    }

    /**
     * Returns the correlation coefficients of the last transform created by {@link #create()},
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
    @Debug
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
