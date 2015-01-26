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

import java.io.IOException;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.io.TableAppender;
import org.apache.sis.math.Line;
import org.apache.sis.math.Plane;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.JDK7;


/**
 * Creates a linear (usually affine) transform which will map approximatively the given source points to
 * the given target points. The transform coefficients are determined using a <cite>least squares</cite>
 * estimation method, with the assumption that source points are precise and all uncertainty is in the
 * target points.
 *
 * <div class="note"><b>Implementation note:</b>
 * The quantity that current implementation tries to minimize is not strictly the squared Euclidian distance.
 * The current implementation rather processes each target dimension independently, which may not give the same
 * result than if we tried to minimize the squared Euclidian distances by taking all dimensions in account together.
 * This algorithm may change in future SIS versions.
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see LinearTransform
 * @see Line
 * @see Plane
 */
public class LinearTransformBuilder {
    /**
     * The arrays of source ordinate values, for example (x[], y[]).
     * This is {@code null} if not yet specified.
     */
    private double[][] sources;

    /**
     * The arrays of target ordinate values, for example (x[], y[], z[]).
     * This is {@code null} if not yet specified.
     */
    private double[][] targets;

    /**
     * The transform created by the last call to {@link #create()}.
     */
    private LinearTransform transform;

    /**
     * An estimation of the Pearson correlation coefficient for each target dimension.
     * This is {@code null} if not yet specified.
     */
    private double[] correlation;

    /**
     * Creates a new linear transform builder.
     */
    public LinearTransformBuilder() {
    }

    /**
     * Extracts the ordinate values of the given points into separated arrays, one for each dimension.
     *
     * @param points The points from which to extract the ordinate values.
     * @param dimension The expected number of dimensions.
     */
    private static double[][] toArrays(final DirectPosition[] points, final int dimension) {
        final int length = points.length;
        final double[][] ordinates = new double[dimension][length];
        for (int j=0; j<length; j++) {
            final DirectPosition p = points[j];
            final int d = p.getDimension();
            if (d != dimension) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "points[" + j + ']', dimension, d));
            }
            for (int i=0; i<dimension; i++) {
                ordinates[i][j] = p.getOrdinate(i);
            }
        }
        return ordinates;
    }

    /**
     * Sets the source points. The number of points shall be the same than the number of target points.
     *
     * <p><b>Limitation:</b> in current implementation, the source points must be one or two-dimensional.
     * This restriction may be removed in a future SIS version.</p>
     *
     * @param  points The source points, assumed precise.
     * @throws MismatchedDimensionException if at least one point does not have the expected number of dimensions.
     */
    public void setSourcePoints(final DirectPosition... points) throws MismatchedDimensionException {
        ArgumentChecks.ensureNonNull("points", points);
        if (points.length != 0) {
            sources = toArrays(points, points[0].getDimension() == 1 ? 1 : 2);
        } else {
            sources = null;
        }
        transform   = null;
        correlation = null;
    }

    /**
     * Sets the target points. The number of points shall be the same than the number of source points.
     * Target points can have any number of dimensions (not necessarily 2), but all points shall have
     * the same number of dimensions.
     *
     * @param  points The target points, assumed uncertain.
     * @throws MismatchedDimensionException if not all points have the same number of dimensions.
     */
    public void setTargetPoints(final DirectPosition... points) throws MismatchedDimensionException {
        ArgumentChecks.ensureNonNull("points", points);
        if (points.length != 0) {
            targets = toArrays(points, points[0].getDimension());
        } else {
            targets = null;
        }
        transform   = null;
        correlation = null;
    }

    /*
     * No getters yet because we did not determined what they should return.
     * Array? Collection? Map<source,target>?
     */

    /**
     * Creates a linear transform approximation from the source points to the target points.
     * This method assumes that source points are precise and all uncertainty is in the target points.
     *
     * @return The fitted linear transform.
     */
    public LinearTransform create() {
        if (transform == null) {
            final double[][] sources = this.sources;  // Protect from changes.
            final double[][] targets = this.targets;
            if (sources == null || targets == null) {
                throw new IllegalStateException(Errors.format(
                        Errors.Keys.MissingValueForProperty_1, (sources == null) ? "sources" : "targets"));
            }
            final int sourceDim = sources.length;
            final int targetDim = targets.length;
            correlation = new double[targetDim];
            final MatrixSIS matrix = Matrices.createZero(targetDim + 1, sourceDim + 1);
            matrix.setElement(targetDim, sourceDim, 1);
            switch (sourceDim) {
                case 1: {
                    final Line line = new Line();
                    for (int j=0; j<targets.length; j++) {
                        correlation[j] = line.fit(sources[0], targets[j]);
                        matrix.setElement(j, 0, line.slope());
                        matrix.setElement(j, 1, line.y0());
                    }
                    break;
                }
                case 2: {
                    final Plane plan = new Plane();
                    for (int j=0; j<targets.length; j++) {
                        correlation[j] = plan.fit(sources[0], sources[1], targets[j]);
                        matrix.setElement(j, 0, plan.slopeX());
                        matrix.setElement(j, 1, plan.slopeY());
                        matrix.setElement(j, 2, plan.z0());
                    }
                    break;
                }
                default: throw new AssertionError(sourceDim); // Should have been verified by setSourcePoints(…) method.
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
     * @return Estimation of correlation coefficients for each target dimension, or {@code null}.
     */
    public double[] correlation() {
        return (correlation != null) ? correlation.clone() : null;
    }

    /**
     * Returns a string representation of this builder for debugging purpose.
     *
     * @return A string representation of this builder.
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
            final String lineSeparator = JDK7.lineSeparator();
            buffer.append(':').append(lineSeparator);
            final TableAppender table = new TableAppender(buffer, " ");
            table.setMultiLinesCells(true);
            table.append(Matrices.toString(transform.getMatrix()));
            table.nextColumn();
            table.append(lineSeparator);
            table.append("  ");
            table.append(Vocabulary.format(Vocabulary.Keys.Correlation));
            table.append(" =");
            table.nextColumn();
            table.append(Matrices.create(correlation.length, 1, correlation).toString());
            try {
                table.flush();
            } catch (IOException e) {
                throw new AssertionError(e); // Should never happen since we wrote into a StringBuilder.
            }
        }
        return buffer.toString();
    }
}
