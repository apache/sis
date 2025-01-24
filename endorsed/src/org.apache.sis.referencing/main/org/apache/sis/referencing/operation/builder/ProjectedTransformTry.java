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
import java.util.Queue;
import java.util.Locale;
import java.util.Collections;
import java.util.Objects;
import java.text.NumberFormat;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.io.TableAppender;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.geometry.MismatchedDimensionException;


/**
 * Information about an attempt to transform coordinates to some projection before to compute a linear approximation.
 * This class contains only the projection to be attempted and a summary of the result. We do not keep new coordinates
 * in order to avoid consuming too much memory when many attempts are made; {@link LinearTransformBuilder} needs only
 * to keep the best attempt.
 *
 * <h2>Purpose</h2>
 * <p>Localization grids in netCDF files contain (<var>longitude</var>, <var>latitude</var>) values for all pixels.
 * {@link LocalizationGridBuilder} first computes a linear (affine) approximation of a localization grid, then stores the residuals.
 * This approach works well when the residuals are small. However if the localization grid is non-linear, then the affine transform
 * is a poor approximation of that grid and the residuals are high. High residuals make inverse transforms hard to compute, which
 * sometimes cause a {@link TransformException} with <q>no convergence</q> error message.</p>
 *
 * <p>In practice, localization grids in netCDF files are often used for storing the results of a map projection, e.g. Mercator.
 * This class allows {@link LocalizationGridBuilder} to try to transform the grid using a given list of map projections and see
 * if one of those projections results in a grid closer to an affine transform. In other words, we use this class for trying to
 * guess what the projection may be. It is okay if the guess is not a perfect match; if the residuals become smalls enough,
 * it will resolve the "no convergence" errors.</p>
 *
 * <h2>Note</h2>
 * {@link #compareTo(ProjectedTransformTry)} is inconsistent with {@link #equals(Object)}.
 * The fact that {@link ProjectedTransformTry} instances are comparable should not be visible in public API.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ProjectedTransformTry implements Comparable<ProjectedTransformTry>, Map.Entry<String,MathTransform> {
    /**
     * Number of points in the temporary buffer used for transforming data.
     * The buffer length will be this capacity multiplied by the number of dimensions.
     *
     * @see org.apache.sis.referencing.operation.transform.AbstractMathTransform#MAXIMUM_BUFFER_SIZE
     */
    private static final int BUFFER_CAPACITY = 512;

    /**
     * A name by witch this projection attempt is identified.
     *
     * @see #getKey()
     */
    private final String name;

    /**
     * A conversion from a non-linear grid (typically with longitude and latitude values) to
     * something that may be more linear (typically, but not necessarily, a map projection).
     *
     * @see #getValue()
     */
    final MathTransform projection;

    /**
     * Maps {@link #projection} dimensions to {@link LinearTransformBuilder} target dimensions.
     * For example if this array is {@code {2,1}}, then dimensions 0 and 1 of {@link #projection}
     * (both source and target dimensions) will map dimensions 2 and 1 of {@link LinearTransformBuilder#targets}.
     * The length of this array shall be equal to the number of {@link #projection} source dimensions.
     */
    private final int[] projToGrid;

    /**
     * Whether the inverse of {@link #projection} shall be concatenated to the
     * final {@linkplain LocalizationGridBuilder#create interpolated transform}.
     * If {@code true}, the {@code projection} effect will be cancelled in the final result,
     * i.e. the target coordinates will be approximately the same as if no projection were applied.
     * In such case, the advantage of applying a projection is to improve numerical stability with
     * a better linear approximation in the first step of the coordinate transformation process.
     */
    final boolean reverseAfterLinearization;

    /**
     * A global correlation factor, stored for information purpose only.
     */
    float correlation;

    /**
     * If an error occurred during coordinate operations, the error. Otherwise {@code null}.
     *
     * @see #getError(List)
     */
    private TransformException error;

    /**
     * Creates a new instance initialized to a copy of the given instance but without result.
     * This is used by copy constructors.
     *
     * @see LocalizationGridBuilder#LocalizationGridBuilder(LinearTransformBuilder)
     */
    ProjectedTransformTry(final ProjectedTransformTry other) {
        name       = other.name;
        projection = other.projection;
        projToGrid = other.projToGrid;
        reverseAfterLinearization = other.reverseAfterLinearization;
    }

    /**
     * Prepares a new attempt to project a localization grid.
     * All arguments are stored as-is (arrays are not cloned).
     *
     * @param name               a name by witch this projection attempt is identified.
     * @param projection         conversion from non-linear grid to something that may be more linear.
     * @param projToGrid         maps {@code projection} dimensions to {@link LinearTransformBuilder} target dimensions.
     * @param expectedDimension  number of {@link LinearTransformBuilder} target dimensions.
     * @throws MismatchedDimensionException if the projection does not have the expected number of dimensions.
     */
    ProjectedTransformTry(final String name, final MathTransform projection, final int[] projToGrid, int expectedDimension,
                          final boolean reverseAfterLinearization)
    {
        this.name       = Objects.requireNonNull(name);
        this.projection = Objects.requireNonNull(projection);
        this.projToGrid = projToGrid;
        this.reverseAfterLinearization = reverseAfterLinearization;
        int side = 0;                           // 0 = problem with source dimensions, 1 = problem with target dimensions.
        int actual = projection.getSourceDimensions();
        if (actual <= expectedDimension) {
            expectedDimension = projToGrid.length;
            if (actual == expectedDimension) {
                actual = projection.getTargetDimensions();
                if (actual == expectedDimension) {
                    return;
                }
                side = 1;
            }
        }
        throw new MismatchedDimensionException(Errors.format(
                Errors.Keys.MismatchedTransformDimension_4, name, side, expectedDimension, actual));
    }

    /**
     * Returns the name by witch this projection attempt is identified.
     */
    @Override
    public String getKey() {
        return name;
    }

    /**
     * Returns the projection, taking in account axis swapping if {@link #projToGrid} is not an arithmetic progression.
     *
     * @todo We needs a pass-through transform if the number of grid dimensions
     *       is greater than the number of projection dimensions.
     */
    @Override
    public MathTransform getValue() {
        MathTransform mt = MathTransforms.linear(Matrices.createDimensionSelect(projToGrid.length, projToGrid));
        return MathTransforms.concatenate(mt, projection);
    }

    /**
     * Do not allow modification of this entry.
     */
    @Override
    public MathTransform setValue(MathTransform value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Transforms target coordinates of a localization grid. The {@code coordinates} argument is the value
     * of {@link LinearTransformBuilder#targets}, without clone (this method will only read those arrays).
     * Only arrays at indices given by {@link #projToGrid} will be read; the other arrays will be ignored.
     * The coordinate operation result will be stored in arrays of size {@code [numDimensions][numPoints]}
     * where {@code numDimensions} is the length of the {@link #projToGrid} array. Indices are as below,
     * with 0 ≤ <var>d</var> ≤ {@code numDimensions}:
     *
     * <ol>
     *   <li>{@code results[d]}    contains the coordinates in dimension <var>d</var>.</li>
     *   <li>{@code results[d][i]} is a coordinate of the point at index <var>i</var>.</li>
     * </ol>
     *
     * The returned target shall be given to {@link #replaceTransformed(double[][], double[][])} before
     * final storage in {@link LinearTransformBuilder}.
     *
     * <h4>Pool of arrays</h4>
     * The {@code pool} queue is initially empty. Arrays created by this method and later discarded will be added to
     * that queue, for recycling if this method is invoked again for another {@code ProjectedTransformTry} instance.
     *
     * @param  coordinates  the {@link LinearTransformBuilder#targets} arrays of coordinates to transform.
     * @param  numPoints    number of points to transform: {@code numPoints} ≤ {@code coordinates[i].length}.
     * @param  pool         pre-allocated arrays of length {@code numPoints} that can be recycled.
     * @return results of coordinate operations (see method javadoc), or {@code null} if an error occurred.
     */
    final double[][] transform(final double[][] coordinates, final int numPoints, final Queue<double[]> pool) {
        final int numDimensions = projToGrid.length;
        final double[][] results = new double[numDimensions][];
        for (int i=0; i<numDimensions; i++) {
            if ((results[i] = pool.poll()) == null) {
                results[i] = new double[numPoints];
            }
        }
        /*
         * Allocate the destination arrays for coordinates to transform as (x₀,y₀), (x₁,y₁), (x₂,y₂)… tuples.
         * In the particular case of one-dimensional transforms (not necessarily one-dimensional coordinates)
         * we can transform arrays directly without the need for a temporary buffer.
         */
        try {
            if (numDimensions == 1) {
                projection.transform(coordinates[projToGrid[0]], 0, results[0], 0, numPoints);
            } else {
                final int bufferCapacity = Math.min(numPoints, BUFFER_CAPACITY);                 // In number of points.
                final double[] buffer = new double[bufferCapacity * numDimensions];
                int dataOffset = 0;
                while (dataOffset < numPoints) {
                    final int start = dataOffset;
                    final int stop = Math.min(start + bufferCapacity, numPoints);
                    /*
                     * Copies coordinates in a single interleaved array before to transform them.
                     * Coordinates start at index 0 and the number of valid points is stop - start.
                     */
                    for (int d=0; d<numDimensions; d++) {
                        final double[] data = coordinates[projToGrid[d]];
                        dataOffset = start;
                        int dst = d;
                        do {
                            buffer[dst] = data[dataOffset];
                            dst += numDimensions;
                        } while (++dataOffset < stop);
                    }
                    /*
                     * Transform coordinates and save the result. If any coordinate result is NaN,
                     * we cannot use that projection (LinearTransformBuilder requires all points).
                     */
                    projection.transform(buffer, 0, buffer, 0, stop - start);
                    for (int d=0; d<numDimensions; d++) {
                        @SuppressWarnings("MismatchedReadAndWriteOfArray")
                        final double[] data = results[d];
                        dataOffset = start;
                        int dst = d;
                        do {
                            if (!Double.isFinite(data[dataOffset] = buffer[dst])) {
                                recycle(results, pool);         // Make arrays available for other transforms.
                                return null;
                            }
                            dst += numDimensions;
                        } while (++dataOffset < stop);
                    }
                }
            }
        } catch (TransformException e) {
            error = e;
            recycle(results, pool);         // Make arrays available for other transforms.
            return null;
        }
        return results;
    }

    /**
     * Makes the given arrays available for reuse by other transforms.
     */
    static void recycle(final double[][] arrays, final Queue<double[]> pool) {
        if (arrays != null) {
            Collections.addAll(pool, arrays);
        }
    }

    /**
     * Returns {@code true} if {@code projToGrid[i] == i} for all <var>i</var>.
     */
    private boolean useSameDimensions() {
        return ArraysExt.isRange(0, projToGrid);
    }

    /**
     * Replaces old target arrays by new values in the dimensions where a projection has been applied.
     * The {@code targets} array is copied if necessary, then values are replaced in the copied array.
     * May return {@code newValues} directly if suitable.
     *
     * @param  targets    the original targets values. This array will not be modified.
     * @param  newValues  targets computed by {@link #transform transform(…)} for the dimensions specified at construction time.
     * @return a copy of the given {@code targets} array with new values overwriting the old values.
     */
    final double[][] replaceTransformed(double[][] targets, final double[][] newValues) {
        if (newValues.length == targets.length && useSameDimensions()) {
            return newValues;
        }
        targets = targets.clone();
        for (int j=0; j<projToGrid.length; j++) {
            targets[projToGrid[j]] = newValues[j];
        }
        return targets;
    }

    /**
     * Replaces old correlation values by new values in the dimensions where a projection has been applied.
     * The {@code correlations} array is copied if necessary, then values are replaced in the copied array.
     * May return {@code newValues} directly if suitable.
     *
     * @param  correlations  the original correlation values. This array will not be modified.
     * @param  newValues     correlations computed by {@link LinearTransformBuilder} for the dimensions specified at construction time.
     * @return a copy of the given {@code correlation} array with new values overwriting the old values.
     */
    final double[] replaceTransformed(double[] correlations, final double[] newValues) {
        if (newValues.length == correlations.length && useSameDimensions()) {
            return newValues;
        }
        correlations = correlations.clone();
        for (int j=0; j<projToGrid.length; j++) {
            correlations[projToGrid[j]] = newValues[j];
        }
        return correlations;
    }

    /**
     * Replaces old transform coefficients by new values in the dimensions where a projection has been applied.
     * The {@code transform} matrix is copied if necessary, then values are replaced in the copied matrix.
     * May return {@code newValues} directly if suitable.
     *
     * @param  transform  the original affine transform. This matrix will not be modified.
     * @param  newValues  coefficients computed by {@link LinearTransformBuilder} for the dimensions specified at construction time.
     * @return a copy of the given {@code transform} matrix with new coefficients overwriting the old values.
     */
    final MatrixSIS replaceTransformed(MatrixSIS transform, final MatrixSIS newValues) {
        /*
         * The two matrices shall have the same number of columns because they were computed with
         * LinearTransformBuilder instances having the same sources. However, the two matrices may
         * have a different number of rows since the number of target dimensions may differ.
         */
        assert newValues.getNumCol() == transform.getNumCol();
        if (newValues.getNumRow() == transform.getNumRow() && useSameDimensions()) {
            return newValues;
        }
        transform = transform.clone();
        for (int j=0; j<projToGrid.length; j++) {
            final int d = projToGrid[j];
            for (int i = transform.getNumCol(); --i >= 0;) {
                transform.setNumber(d, i, newValues.getNumber(j, i));
            }
        }
        return transform;
    }

    /**
     * Returns the first error in the given list of linearizers. Errors after the first one are added
     * as suppressed exceptions. If no error are found, this method returns {@code null}.
     */
    static TransformException getError(final List<ProjectedTransformTry> linearizers) {
        TransformException error = null;
        for (final ProjectedTransformTry alt : linearizers) {
            final TransformException e = alt.error;
            if (e != null) {
                if (error == null) {
                    error = e;
                } else {
                    error.addSuppressed(e);
                }
            }
        }
        return error;
    }

    /**
     * Orders by the inverse of correlation coefficients. Highest coefficients (best correlations)
     * are first, lower coefficients are next, {@link Float#NaN} values are last.
     *
     * <p><b>Note:</b> this comparison is inconsistent with {@link #equals(Object)}.
     * The fact that {@link ProjectedTransformTry} instances are comparable should
     * not be visible in public API.</p>
     */
    @Override
    public int compareTo(final ProjectedTransformTry other) {
        return Float.compare(-correlation, -other.correlation);
    }

    /**
     * Implements the {@link Map.Entry#equals(Object)} contract.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Map.Entry<?,?>) {
            final var other = (Map.Entry<?,?>) obj;
            return name.equals(other.getKey()) && projection.equals(other.getValue());
        }
        return false;
    }

    /**
     * Implements the {@link Map.Entry#hashCode()} contract.
     */
    @Override
    public int hashCode() {
        return name.hashCode() ^ projection.hashCode();
    }

    /**
     * Formats a summary of this projection attempt. This method formats the following columns:
     *
     * <ol>
     *   <li>The projection name.</li>
     *   <li>The corelation coefficient, or the error message if an error occurred.</li>
     * </ol>
     *
     * @param  table   the table where to write a row.
     * @param  nf      format to use for writing coefficients, or {@code null} if not yet created.
     * @param  locale  the locale to use for messages or if a number format must be created.
     * @return format used for writing coefficients, or {@code null}.
     */
    final NumberFormat summarize(final TableAppender table, NumberFormat nf, final Locale locale) {
        table.append(name).nextColumn();
        String message = "";
        if (error != null) {
            message = Exceptions.getLocalizedMessage(error, locale);
            if (message == null) {
                message = Classes.getShortClassName(error);
            }
        } else if (correlation > 0) {
            if (nf == null) {
                nf = (locale != null) ? NumberFormat.getInstance(locale) : NumberFormat.getInstance();
                nf.setMinimumFractionDigits(6);         // Math.ulp(1f) ≈ 1.2E-7
                nf.setMaximumFractionDigits(6);
            }
            message = nf.format(correlation);
        }
        table.append(message).nextLine();
        return nf;
    }

    /**
     * Returns a string representation of this projection attempt for debugging purpose.
     */
    @Override
    public String toString() {
        final var buffer = new TableAppender("  ");
        summarize(buffer, null, null);
        return buffer.toString();
    }
}
