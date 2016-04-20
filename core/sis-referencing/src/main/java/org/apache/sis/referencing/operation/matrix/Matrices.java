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
package org.apache.sis.referencing.operation.matrix;

import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem; // For javadoc
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.util.Static;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.JDK7;
import org.apache.sis.internal.jdk7.Objects;


/**
 * {@link Matrix} factory methods and utilities.
 * This class provides the following methods:
 *
 * <ul>
 *   <li>Creating new matrices of arbitrary size:
 *       {@link #createIdentity createIdentity},
 *       {@link #createDiagonal createDiagonal},
 *       {@link #createZero     createZero},
 *       {@link #create         create},
 *       {@link #copy           copy}.
 *   </li>
 *   <li>Creating new matrices for coordinate operation steps:
 *       {@link #createTransform(Envelope, AxisDirection[], Envelope, AxisDirection[]) createTransform},
 *       {@link #createDimensionSelect createDimensionSelect},
 *       {@link #createPassThrough     createPassThrough}.
 *   </li>
 *   <li>Information:
 *       {@link #isAffine   isAffine},
 *       {@link #isIdentity isIdentity},
 *       {@link #equals(Matrix, Matrix, double, boolean) equals},
 *       {@link #toString(Matrix) toString}.
 *   </li>
 *   <li>Miscellaneous:
 *       {@link #multiply     multiply},
 *       {@link #inverse      inverse},
 *       {@link #unmodifiable unmodifiable},
 *   </li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.parameter.TensorParameters
 */
public final class Matrices extends Static {
    /**
     * Number of spaces to put between columns formatted by {@link #toString(Matrix)}.
     */
    private static final int SPACING = 2;

    /**
     * Do not allows instantiation of this class.
     */
    private Matrices() {
    }

    /**
     * Creates a square identity matrix of size {@code size} × {@code size}.
     * Elements on the diagonal (<var>j</var> == <var>i</var>) are set to 1.
     *
     * <div class="note"><b>Implementation note:</b>
     * For sizes between {@value org.apache.sis.referencing.operation.matrix.Matrix1#SIZE} and
     * {@value org.apache.sis.referencing.operation.matrix.Matrix4#SIZE} inclusive, the matrix
     * is guaranteed to be an instance of one of {@link Matrix1} … {@link Matrix4} subtypes.</div>
     *
     * @param  size Numbers of row and columns. For an affine transform matrix, this is the number of
     *         {@linkplain MathTransform#getSourceDimensions() source} and
     *         {@linkplain MathTransform#getTargetDimensions() target} dimensions + 1.
     * @return An identity matrix of the given size.
     */
    public static MatrixSIS createIdentity(final int size) {
        switch (size) {
            case 1:  return new Matrix1();
            case 2:  return new Matrix2();
            case 3:  return new Matrix3();
            case 4:  return new Matrix4();
            default: return new GeneralMatrix(size, size, true, 1);
        }
    }

    /**
     * Creates a matrix of size {@code numRow} × {@code numCol}.
     * Elements on the diagonal (<var>j</var> == <var>i</var>) are set to 1.
     * The result is an identity matrix if {@code numRow} = {@code numCol}.
     *
     * <div class="note"><b>Implementation note:</b>
     * For {@code numRow} == {@code numCol} with a value between
     * {@value org.apache.sis.referencing.operation.matrix.Matrix1#SIZE} and
     * {@value org.apache.sis.referencing.operation.matrix.Matrix4#SIZE} inclusive, the matrix
     * is guaranteed to be an instance of one of {@link Matrix1} … {@link Matrix4} subtypes.</div>
     *
     * @param numRow For a math transform, this is the number of {@linkplain MathTransform#getTargetDimensions() target dimensions} + 1.
     * @param numCol For a math transform, this is the number of {@linkplain MathTransform#getSourceDimensions() source dimensions} + 1.
     * @return An identity matrix of the given size.
     */
    public static MatrixSIS createDiagonal(final int numRow, final int numCol) {
        if (numRow == numCol) {
            return createIdentity(numRow);
        } else {
            return new NonSquareMatrix(numRow, numCol, true, 1);
        }
    }

    /**
     * Creates a matrix of size {@code numRow} × {@code numCol} filled with zero values.
     * This constructor is convenient when the caller wants to initialize the matrix elements himself.
     *
     * <div class="note"><b>Implementation note:</b>
     * For {@code numRow} == {@code numCol} with a value between
     * {@value org.apache.sis.referencing.operation.matrix.Matrix1#SIZE} and
     * {@value org.apache.sis.referencing.operation.matrix.Matrix4#SIZE} inclusive, the matrix
     * is guaranteed to be an instance of one of {@link Matrix1} … {@link Matrix4} subtypes.</div>
     *
     * @param numRow For a math transform, this is the number of {@linkplain MathTransform#getTargetDimensions() target dimensions} + 1.
     * @param numCol For a math transform, this is the number of {@linkplain MathTransform#getSourceDimensions() source dimensions} + 1.
     * @return A matrix of the given size with only zero values.
     */
    public static MatrixSIS createZero(final int numRow, final int numCol) {
        if (numRow == numCol) switch (numRow) {
            case 1:  return new Matrix1(false);
            case 2:  return new Matrix2(false);
            case 3:  return new Matrix3(false);
            case 4:  return new Matrix4(false);
            default: return new GeneralMatrix(numRow, numCol, false, 1);
        }
        return new NonSquareMatrix(numRow, numCol, false, 1);
    }

    /**
     * Creates a matrix filled with zero values, using double-double precision if {@code precision} is {@code true}.
     */
    static MatrixSIS createZero(final int numRow, final int numCol, final boolean precision) {
        return precision ? GeneralMatrix.createExtendedPrecision(numRow, numCol, false) : createZero(numRow, numCol);
    }

    /**
     * Creates a matrix of size {@code numRow} × {@code numCol} initialized to the given elements.
     * The elements array size must be equals to {@code numRow*numCol}. Column indices vary fastest.
     *
     * <div class="note"><b>Implementation note:</b>
     * For {@code numRow} == {@code numCol} with a value between
     * {@value org.apache.sis.referencing.operation.matrix.Matrix1#SIZE} and
     * {@value org.apache.sis.referencing.operation.matrix.Matrix4#SIZE} inclusive, the matrix
     * is guaranteed to be an instance of one of {@link Matrix1} … {@link Matrix4} subtypes.</div>
     *
     * @param  numRow   Number of rows.
     * @param  numCol   Number of columns.
     * @param  elements The matrix elements in a row-major array. Column indices vary fastest.
     * @return A matrix initialized to the given elements.
     *
     * @see MatrixSIS#setElements(double[])
     */
    public static MatrixSIS create(final int numRow, final int numCol, final double[] elements) {
        if (numRow == numCol) switch (numRow) {
            case 1:  return new Matrix1(elements);
            case 2:  return new Matrix2(elements);
            case 3:  return new Matrix3(elements);
            case 4:  return new Matrix4(elements);
            default: return new GeneralMatrix(numRow, numCol, elements);
        }
        return new NonSquareMatrix(numRow, numCol, elements);
    }

    /**
     * Creates a matrix of size {@code numRow} × {@code numCol} initialized to the given numbers.
     * The elements array size must be equals to {@code numRow*numCol}. Column indices vary fastest.
     *
     * @param  numRow   Number of rows.
     * @param  numCol   Number of columns.
     * @param  elements The matrix elements in a row-major array. Column indices vary fastest.
     * @return A matrix initialized to the given elements.
     */
    public static MatrixSIS create(final int numRow, final int numCol, final Number[] elements) {
        ArgumentChecks.ensureNonNull("elements", elements);
        if (elements == ExtendedPrecisionMatrix.IDENTITY) { // Intentionally undocumented features.
            return GeneralMatrix.createExtendedPrecision(numRow, numCol, true);
        }
        final GeneralMatrix matrix = GeneralMatrix.createExtendedPrecision(numRow, numCol, false);
        if (matrix.setElements(elements)) {
            /*
             * At least one org.apache.sis.internal.util.DoubleDouble instance has been found,
             * in which case the matrix uses double-double arithmetic.  This case is the main
             * purpose of this method.
             */
            return matrix;
        }
        return create(numRow, numCol, matrix.getElements());
    }

    /**
     * Implementation of {@code createTransform(…)} public methods expecting envelopes and/or axis directions.
     * Argument validity shall be verified by the caller.
     *
     * @param useEnvelopes {@code true} if source and destination envelopes shall be taken in account.
     *        If {@code false}, then source and destination envelopes will be ignored and can be null.
     */
    @SuppressWarnings("null")
    private static MatrixSIS createTransform(final Envelope srcEnvelope, final AxisDirection[] srcAxes,
                                             final Envelope dstEnvelope, final AxisDirection[] dstAxes,
                                             final boolean useEnvelopes)
    {
        final DirectPosition dstCorner, srcCorner, srcOppositeCorner;
        if (useEnvelopes) {
            dstCorner         = dstEnvelope.getLowerCorner();
            srcCorner         = srcEnvelope.getLowerCorner();
            srcOppositeCorner = srcEnvelope.getUpperCorner();
        } else {
            dstCorner = srcCorner = srcOppositeCorner = null;
        }
        final MatrixSIS matrix = createZero(dstAxes.length+1, srcAxes.length+1);
        /*
         * Maps source axes to destination axes. If no axis is moved (for example if the user
         * want to transform (NORTH,EAST) to (SOUTH,EAST)), then source and destination index
         * will be equal.   If some axes are moved (for example if the user want to transform
         * (NORTH,EAST) to (EAST,NORTH)), then ordinates at index {@code srcIndex} will have
         * to be moved at index {@code dstIndex}.
         */
        for (int dstIndex = 0; dstIndex < dstAxes.length; dstIndex++) {
            boolean hasFound = false;
            final AxisDirection dstDir = dstAxes[dstIndex];
            final AxisDirection search = AxisDirections.absolute(dstDir);
            for (int srcIndex = 0; srcIndex < srcAxes.length; srcIndex++) {
                final AxisDirection srcDir = srcAxes[srcIndex];
                if (search.equals(AxisDirections.absolute(srcDir))) {
                    if (hasFound) {
                        throw new IllegalArgumentException(Errors.format(
                                Errors.Keys.ColinearAxisDirections_2, srcDir, dstDir));
                    }
                    hasFound = true;
                    /*
                     * Set the matrix elements. Some matrix elements will never be set.
                     * They will be left to zero, which is their desired value.
                     */
                    final boolean same = srcDir.equals(dstDir);
                    double scale = same ? +1 : -1;
                    double translate = 0;
                    if (useEnvelopes) {
                        // See the comment in transform(Envelope, Envelope) for an explanation about why
                        // we use the lower/upper corners instead than getMinimum()/getMaximum() methods.
                        translate  = dstCorner.getOrdinate(dstIndex);
                        scale     *= dstEnvelope.getSpan(dstIndex) /
                                     srcEnvelope.getSpan(srcIndex);
                        translate -= scale * (same ? srcCorner : srcOppositeCorner).getOrdinate(srcIndex);
                    }
                    matrix.setElement(dstIndex, srcIndex,       scale);
                    matrix.setElement(dstIndex, srcAxes.length, translate);
                }
            }
            if (!hasFound) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.CanNotMapAxisToDirection_2, "srcAxes", dstAxes[dstIndex]));
            }
        }
        matrix.setElement(dstAxes.length, srcAxes.length, 1);
        return matrix;
    }

    /**
     * Creates a transform matrix mapping the given source envelope to the given destination envelope.
     * The given envelopes can have any dimensions, which are handled as below:
     *
     * <ul>
     *   <li>If the two envelopes have the same {@linkplain Envelope#getDimension() dimension},
     *       then the transform is {@linkplain #isAffine(Matrix) affine}.</li>
     *   <li>If the destination envelope has less dimensions than the source envelope,
     *       then trailing dimensions are silently dropped.</li>
     *   <li>If the target envelope has more dimensions than the source envelope,
     *       then the transform will append trailing ordinates with the 0 value.</li>
     * </ul>
     *
     * This method ignores the {@linkplain Envelope#getCoordinateReferenceSystem() envelope CRS}, which may be null.
     * Actually this method is used more often for {@linkplain org.opengis.coverage.grid.GridEnvelope grid envelopes}
     * (which have no CRS) than geodetic envelopes.
     *
     * <div class="section">Spanning the anti-meridian of a Geographic CRS</div>
     * If the given envelopes cross the date line, then this method requires their {@code getSpan(int)} method
     * to behave as documented in the {@link org.apache.sis.geometry.AbstractEnvelope#getSpan(int)} javadoc.
     * Furthermore the matrix created by this method will produce expected results only for source or destination
     * points before the date line, since the wrap around operation can not be represented by an affine transform.
     *
     * <div class="section">Example</div>
     * Given a source envelope of size 100 × 200 (the units do not matter for this method) and a destination
     * envelope of size 300 × 500, and given {@linkplain Envelope#getLowerCorner() lower corner} translation
     * from (-20, -40) to (-10, -25), then the following method call:
     *
     * {@preformat java
     *   matrix = Matrices.createTransform(
     *           new Envelope2D(null, -20, -40, 100, 200),
     *           new Envelope2D(null, -10, -25, 300, 500));
     * }
     *
     * will return the following square matrix. The transform of the lower corner is given as an example:
     *
     * {@preformat math
     *   ┌     ┐   ┌              ┐   ┌     ┐
     *   │ -10 │   │ 3.0  0    50 │   │ -20 │       // 3.0 is the scale factor from width of 100 to 300
     *   │ -25 │ = │ 0    2.5  75 │ × │ -40 │       // 2.5 is the scale factor from height of 200 to 500
     *   │   1 │   │ 0    0     1 │   │   1 │
     *   └     ┘   └              ┘   └     ┘
     * }
     *
     * @param  srcEnvelope The source envelope.
     * @param  dstEnvelope The destination envelope.
     * @return The transform from the given source envelope to target envelope.
     *
     * @see #createTransform(AxisDirection[], AxisDirection[])
     * @see #createTransform(Envelope, AxisDirection[], Envelope, AxisDirection[])
     * @see org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)
     */
    public static MatrixSIS createTransform(final Envelope srcEnvelope, final Envelope dstEnvelope) {
        ArgumentChecks.ensureNonNull("srcEnvelope", srcEnvelope);
        ArgumentChecks.ensureNonNull("dstEnvelope", dstEnvelope);
        /*
         * Following code is a simplified version of above createTransform(Envelope, AxisDirection[], ...) method.
         * We need to make sure that those two methods are consistent and compute the matrix values in the same way.
         */
        final int srcDim = srcEnvelope.getDimension();
        final int dstDim = dstEnvelope.getDimension();
        final DirectPosition srcCorner = srcEnvelope.getLowerCorner();
        final DirectPosition dstCorner = dstEnvelope.getLowerCorner();
        final MatrixSIS matrix = createZero(dstDim+1, srcDim+1);
        for (int i = Math.min(srcDim, dstDim); --i >= 0;) {
            /*
             * Note on envelope spanning over the anti-meridian: the GeoAPI javadoc does not mandate the
             * precise behavior of getSpan(int) in such situation.  In the particular case of Apache SIS
             * implementations, the envelope will compute the span correctly (taking in account the wrap
             * around behavior). For non-SIS implementations, we can not know.
             *
             * For the translation term, we really need the lower corner, NOT envelope.getMinimum(i),
             * because we need the starting point, which is not the minimal value when spanning over
             * the anti-meridian.
             */
            final double scale     = dstEnvelope.getSpan(i)   / srcEnvelope.getSpan(i);
            final double translate = dstCorner.getOrdinate(i) - srcCorner.getOrdinate(i)*scale;
            matrix.setElement(i, i,      scale);
            matrix.setElement(i, srcDim, translate);
        }
        matrix.setElement(dstDim, srcDim, 1);
        return matrix;
    }

    /**
     * Creates a transform matrix changing axis order and/or direction. For example the transform may convert
     * (<i>northing</i>, <i>westing</i>) coordinates into (<i>easting</i>, <i>northing</i>) coordinates.
     * This method tries to associate each {@code dstAxes} direction to either an equals {@code srcAxis}
     * direction, or to an opposite {@code srcAxis} direction.
     *
     * <ul>
     *   <li>If some {@code srcAxes} directions can not be mapped to {@code dstAxes} directions, then the transform
     *       will silently drops the ordinates associated to those extra source axis directions.</li>
     *   <li>If some {@code dstAxes} directions can not be mapped to {@code srcAxes} directions,
     *       then an exception will be thrown.</li>
     * </ul>
     *
     * <div class="note"><b>Example:</b>
     * it is legal to transform from (<i>easting</i>, <i>northing</i>, <i>up</i>) to
     * (<i>easting</i>, <i>northing</i>) — this is the first above case — but illegal
     * to transform (<i>easting</i>, <i>northing</i>) to (<i>easting</i>, <i>up</i>).</div>
     *
     * <div class="section">Example</div>
     * The following method call:
     *
     * {@preformat java
     *   matrix = Matrices.createTransform(
     *           new AxisDirection[] {AxisDirection.NORTH, AxisDirection.WEST},
     *           new AxisDirection[] {AxisDirection.EAST, AxisDirection.NORTH});
     * }
     *
     * will return the following square matrix, which can be used in coordinate conversions as below:
     *
     * {@preformat math
     *   ┌    ┐   ┌         ┐   ┌    ┐
     *   │ +x │   │ 0 -1  0 │   │  y │
     *   │  y │ = │ 1  0  0 │ × │ -x │
     *   │  1 │   │ 0  0  1 │   │  1 │
     *   └    ┘   └         ┘   └    ┘
     * }
     *
     * @param  srcAxes The ordered sequence of axis directions for source coordinate system.
     * @param  dstAxes The ordered sequence of axis directions for destination coordinate system.
     * @return The transform from the given source axis directions to the given target axis directions.
     * @throws IllegalArgumentException If {@code dstAxes} contains at least one axis not found in {@code srcAxes},
     *         or if some colinear axes were found.
     *
     * @see #createTransform(Envelope, Envelope)
     * @see #createTransform(Envelope, AxisDirection[], Envelope, AxisDirection[])
     * @see org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)
     */
    public static MatrixSIS createTransform(final AxisDirection[] srcAxes, final AxisDirection[] dstAxes) {
        ArgumentChecks.ensureNonNull("srcAxes", srcAxes);
        ArgumentChecks.ensureNonNull("dstAxes", dstAxes);
        return createTransform(null, srcAxes, null, dstAxes, false);
    }

    /**
     * Creates a transform matrix mapping the given source envelope to the given destination envelope,
     * combined with changes in axis order and/or direction.
     * Invoking this method is equivalent to concatenating the following matrix transforms:
     *
     * <ul>
     *   <li><code>{@linkplain #createTransform(Envelope, Envelope) createTransform}(srcEnvelope, dstEnvelope)</code></li>
     *   <li><code>{@linkplain #createTransform(AxisDirection[], AxisDirection[]) createTransform}(srcAxes, dstAxes)</code></li>
     * </ul>
     *
     * This method ignores the {@linkplain Envelope#getCoordinateReferenceSystem() envelope CRS}, which may be null.
     * Actually this method is used more often for {@linkplain org.opengis.coverage.grid.GridEnvelope grid envelopes}
     * (which have no CRS) than geodetic envelopes.
     *
     * <div class="section">Spanning the anti-meridian of a Geographic CRS</div>
     * If the given envelopes cross the date line, then this method requires their {@code getSpan(int)} method
     * to behave as documented in the {@link org.apache.sis.geometry.AbstractEnvelope#getSpan(int)} javadoc.
     * Furthermore the matrix created by this method will produce expected results only for source or destination
     * points on one side of the date line (depending on whether axis direction is reversed), since the wrap around
     * operation can not be represented by an affine transform.
     *
     * <div class="note"><b>Example:</b>
     * combining the examples documented in the above {@code createTransform(…)} methods, the following method call:
     *
     * {@preformat java
     *   matrix = Matrices.createTransform(
     *           new Envelope2D(null, -40, +20, 200, 100), new AxisDirection[] {AxisDirection.NORTH, AxisDirection.WEST},
     *           new Envelope2D(null, -10, -25, 300, 500), new AxisDirection[] {AxisDirection.EAST, AxisDirection.NORTH});
     * }
     *
     * will return the following square matrix. The transform of a corner is given as an example.
     * Note that the input ordinate values are swapped because of the (<i>North</i>, <i>West</i>) axis directions,
     * and the lower-left corner of the destination envelope is the lower-<em>right</em> corner of the source envelope
     * because of the opposite axis direction.
     *
     * {@preformat math
     *   ┌     ┐   ┌               ┐   ┌     ┐
     *   │ -10 │   │ 0   -3.0  350 │   │ -40 │
     *   │ -25 │ = │ 2.5  0     75 │ × │ 120 │       // 120 is the westernmost source ordinate: (x=20) + (width=100)
     *   │   1 │   │ 0    0      1 │   │   1 │
     *   └     ┘   └               ┘   └     ┘
     * }
     * </div>
     *
     * @param  srcEnvelope The source envelope.
     * @param  srcAxes     The ordered sequence of axis directions for source coordinate system.
     * @param  dstEnvelope The destination envelope.
     * @param  dstAxes     The ordered sequence of axis directions for destination coordinate system.
     * @return The transform from the given source envelope and axis directions
     *         to the given envelope and target axis directions.
     * @throws MismatchedDimensionException If an envelope {@linkplain Envelope#getDimension() dimension} does not
     *         match the length of the axis directions sequence.
     * @throws IllegalArgumentException If {@code dstAxes} contains at least one axis not found in {@code srcAxes},
     *         or if some colinear axes were found.
     *
     * @see #createTransform(Envelope, Envelope)
     * @see #createTransform(AxisDirection[], AxisDirection[])
     * @see org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)
     */
    public static MatrixSIS createTransform(final Envelope srcEnvelope, final AxisDirection[] srcAxes,
                                            final Envelope dstEnvelope, final AxisDirection[] dstAxes)
    {
        ArgumentChecks.ensureNonNull("srcEnvelope", srcEnvelope);
        ArgumentChecks.ensureNonNull("dstEnvelope", dstEnvelope);
        ArgumentChecks.ensureDimensionMatches("srcEnvelope", srcAxes.length, srcEnvelope);
        ArgumentChecks.ensureDimensionMatches("dstEnvelope", dstAxes.length, dstEnvelope);
        return createTransform(srcEnvelope, srcAxes, dstEnvelope, dstAxes, true);
    }

    /**
     * Creates a matrix for a transform that keep only a subset of source ordinate values.
     * The matrix size will be ({@code selectedDimensions.length} + 1) × ({@code sourceDimensions} + 1).
     * The matrix will contain only zero elements, except for the following cells which will contain 1:
     *
     * <ul>
     *   <li>The last column in the last row.</li>
     *   <li>For any row <var>j</var> other than the last row, the column {@code selectedDimensions[j]}.</li>
     * </ul>
     *
     * <div class="note"><b>Example:</b>
     * given (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>) ordinate values, if one wants to keep
     * (<var>y</var>,<var>x</var>,<var>t</var>) ordinates (note the <var>x</var> ↔ <var>y</var> swapping)
     * and discard the <var>z</var> values, then the indices of source ordinates to select are 1 for <var>y</var>,
     * 0 for <var>x</var> and 3 for <var>t</var>. One can use the following method call:
     *
     * {@preformat java
     *   matrix = Matrices.createDimensionSelect(4, new int[] {1, 0, 3});
     * }
     *
     * The above method call will create the following 4×5 matrix,
     * which can be used for converting coordinates as below:
     *
     * {@preformat math
     *   ┌   ┐   ┌           ┐   ┌   ┐
     *   │ y │   │ 0 1 0 0 0 │   │ x │
     *   │ x │   │ 1 0 0 0 0 │   │ y │
     *   │ t │ = │ 0 0 0 1 0 │ × │ z │
     *   │ 1 │   │ 0 0 0 0 1 │   │ t │
     *   └   ┘   └           ┘   │ 1 │
     *                           └   ┘
     * }
     * </div>
     *
     * The inverse of the matrix created by this method will put {@link Double#NaN} values in the extra dimensions.
     * Other dimensions will work as expected.
     *
     * @param  sourceDimensions The number of dimensions in source coordinates.
     * @param  selectedDimensions The 0-based indices of source ordinate values to keep.
     *         The length of this array will be the number of dimensions in target coordinates.
     * @return An affine transform matrix keeping only the given source dimensions, and discarding all others.
     * @throws IllegalArgumentException if a value of {@code selectedDimensions} is lower than 0
     *         or not smaller than {@code sourceDimensions}.
     *
     * @see org.apache.sis.referencing.operation.transform.TransformSeparator
     */
    public static MatrixSIS createDimensionSelect(final int sourceDimensions, final int[] selectedDimensions) {
        final int numTargetDim = selectedDimensions.length;
        final MatrixSIS matrix = createZero(numTargetDim+1, sourceDimensions+1);
        for (int j=0; j<numTargetDim; j++) {
            final int i = selectedDimensions[j];
            ArgumentChecks.ensureValidIndex(sourceDimensions, i);
            matrix.setElement(j, i, 1);
        }
        matrix.setElement(numTargetDim, sourceDimensions, 1);
        return matrix;
    }

    /**
     * Creates a matrix which converts a subset of ordinates using the transform given by another matrix.
     * For example giving (<var>latitude</var>, <var>longitude</var>, <var>height</var>) coordinates,
     * a pass through operation can convert the height values from feet to metres without affecting
     * the (<var>latitude</var>, <var>longitude</var>) values.
     *
     * <p>The given sub-matrix shall have the following properties:</p>
     * <ul>
     *   <li>The last row often (but not necessarily) contains 0 values everywhere except in the last column.</li>
     *   <li>Values in the last column are translation terms, except in the last row.</li>
     *   <li>All other values are scale or shear terms.</li>
     * </ul>
     *
     * A square matrix complying with the above conditions is often {@linkplain #isAffine(Matrix) affine},
     * but this is not mandatory
     * (for example a <cite>perspective transform</cite> may contain non-zero values in the last row).
     *
     * <p>This method builds a new matrix with the following content:</p>
     * <ul>
     *   <li>An amount of {@code firstAffectedOrdinate} rows and columns are inserted before the first
     *       row and columns of the sub-matrix. The elements for the new rows and columns are set to 1
     *       on the diagonal, and 0 elsewhere.</li>
     *   <li>The sub-matrix - except for its last row and column - is copied in the new matrix starting
     *       at index ({@code firstAffectedOrdinate}, {@code firstAffectedOrdinate}).</li>
     *   <li>An amount of {@code numTrailingOrdinates} rows and columns are appended after the above sub-matrix.
     *       Their elements are set to 1 on the pseudo-diagonal ending in the lower-right corner, and 0 elsewhere.</li>
     *   <li>The last sub-matrix row is copied in the last row of the new matrix, and the last sub-matrix column
     *       is copied in the last column of the sub-matrix.</li>
     * </ul>
     *
     * <div class="note"><b>Example:</b>
     * given the following sub-matrix which converts height values from feet to metres before to subtracts 25 metres:
     *
     * {@preformat math
     *   ┌    ┐   ┌             ┐   ┌   ┐
     *   │ z' │ = │ 0.3048  -25 │ × │ z │
     *   │ 1  │   │ 0         1 │   │ 1 │
     *   └    ┘   └             ┘   └   ┘
     * }
     *
     * Then a call to {@code Matrices.createPassThrough(2, subMatrix, 1)} will return the following matrix,
     * which can be used for converting the height (<var>z</var>) without affecting the other ordinate values
     * (<var>x</var>,<var>y</var>,<var>t</var>):
     *
     * {@preformat math
     *   ┌    ┐   ┌                      ┐   ┌   ┐
     *   │ x  │   │ 1  0  0       0    0 │   │ x │
     *   │ y  │   │ 0  1  0       0    0 │   │ y │
     *   │ z' │ = │ 0  0  0.3048  0  -25 │ × │ z │
     *   │ t  │   │ 0  0  0       1    0 │   │ t │
     *   │ 1  │   │ 0  0  0       0    1 │   │ 1 │
     *   └    ┘   └                      ┘   └   ┘
     * }
     * </div>
     *
     * @param  firstAffectedOrdinate The lowest index of the affected ordinates.
     * @param  subMatrix The matrix to use for affected ordinates.
     * @param  numTrailingOrdinates Number of trailing ordinates to pass through.
     * @return A matrix for the same transform than the given matrix,
     *         augmented with leading and trailing pass-through coordinates.
     *
     * @see org.apache.sis.referencing.operation.DefaultMathTransformFactory#createPassThroughTransform(int, MathTransform, int)
     */
    public static MatrixSIS createPassThrough(final int firstAffectedOrdinate,
            final Matrix subMatrix, final int numTrailingOrdinates)
    {
        ArgumentChecks.ensureNonNull ("subMatrix",             subMatrix);
        ArgumentChecks.ensurePositive("firstAffectedOrdinate", firstAffectedOrdinate);
        ArgumentChecks.ensurePositive("numTrailingOrdinates",  numTrailingOrdinates);
        final int  expansion = firstAffectedOrdinate + numTrailingOrdinates;
        int sourceDimensions = subMatrix.getNumCol();           // Will become the number of dimensions later.
        int targetDimensions = subMatrix.getNumRow();
        /*
         * Get data from the source matrix, together with the error terms if present.
         * The 'stride' and 'length' values will be used for computing indices in that array.
         * The DoubleDouble temporary object is used only if the array contains error terms.
         */
        final int      stride  = sourceDimensions;
        final int      length  = sourceDimensions * targetDimensions;
        final double[] sources = getExtendedElements(subMatrix);
        final DoubleDouble transfer = (sources.length > length) ? new DoubleDouble() : null;
        final MatrixSIS matrix = createZero(targetDimensions-- + expansion,
                                            sourceDimensions-- + expansion,
                                            transfer != null);
        /*
         * Following code processes from upper row to lower row.
         * First, set the diagonal elements on leading new dimensions.
         */
        for (int j=0; j<firstAffectedOrdinate; j++) {
            matrix.setElement(j, j, 1);
        }
        /*
         * Copy the sub-matrix, with special case for the translation terms
         * which are unconditionally stored in the last column.
         */
        final int lastColumn = sourceDimensions + expansion;
        matrix.setElements(sources, length, stride, transfer,
                0,                     0,                           // Source (row, colum)
                firstAffectedOrdinate, firstAffectedOrdinate,       // Target (row, column)
                targetDimensions,      sourceDimensions);           // Number of rows and columns to copy.
        matrix.setElements(sources, length, stride, transfer,
                0,                     sourceDimensions,            // Source (row, colum):  last column
                firstAffectedOrdinate, lastColumn,                  // Target (row, column): part of last column
                targetDimensions,      1);                          // Copy some rows of only 1 column.
        /*
         * Set the pseudo-diagonal elements on the trailing new dimensions.
         * 'diff' is zero for a square matrix and non-zero for rectangular matrix.
         */
        final int diff = targetDimensions - sourceDimensions;
        for (int i=lastColumn - numTrailingOrdinates; i<lastColumn; i++) {
            matrix.setElement(diff + i, i, 1);
        }
        /*
         * Copy the last row from the sub-matrix. In the usual affine transform,
         * this row contains only 0 element except for the last one, which is 1.
         */
        final int lastRow = targetDimensions + expansion;
        matrix.setElements(sources, length, stride, transfer,
                targetDimensions, 0,                                // Source (row, colum):  last row
                lastRow,          firstAffectedOrdinate,            // Target (row, column): part of last row
                1,                sourceDimensions);                // Copy some columns of only 1 row.
        matrix.setElements(sources, length, stride, transfer,
                targetDimensions, sourceDimensions,
                lastRow,          lastColumn,
                1,                1);
        return matrix;
    }

    /**
     * Returns a matrix with the same content than the given matrix but a different size, assuming an affine transform.
     * This method can be invoked for adding or removing the <strong>last</strong> dimensions of an affine transform.
     * More specifically:
     *
     * <ul class="verbose">
     *   <li>If the given {@code numCol} is <var>n</var> less than the number of columns in the given matrix,
     *       then the <var>n</var> columns <em>before the last column</em> are removed.
     *       The last column is left unchanged because it is assumed to contain the translation terms.</li>
     *   <li>If the given {@code numCol} is <var>n</var> more than the number of columns in the given matrix,
     *       then <var>n</var> columns are inserted <em>before the last column</em>.
     *       All values in the new columns will be zero.</li>
     *   <li>If the given {@code numRow} is <var>n</var> less than the number of rows in the given matrix,
     *       then the <var>n</var> rows <em>before the last row</em> are removed.
     *       The last row is left unchanged because it is assumed to contain the usual [0 0 0 … 1] terms.</li>
     *   <li>If the given {@code numRow} is <var>n</var> more than the number of rows in the given matrix,
     *       then <var>n</var> rows are inserted <em>before the last row</em>.
     *       The corresponding offset and scale factors will be 0 and 1 respectively.
     *       In other words, new dimensions are propagated unchanged.</li>
     * </ul>
     *
     * @param  matrix  the matrix to resize. This matrix will never be changed.
     * @param  numRow  the new number of rows. This is equal to the desired number of target dimensions plus 1.
     * @param  numCol  the new number of columns. This is equal to the desired number of source dimensions plus 1.
     * @return a new matrix of the given size, or the given {@code matrix} if no resizing was needed.
     */
    public static Matrix resizeAffine(final Matrix matrix, int numRow, int numCol) {
        ArgumentChecks.ensureNonNull         ("matrix", matrix);
        ArgumentChecks.ensureStrictlyPositive("numRow", numRow);
        ArgumentChecks.ensureStrictlyPositive("numCol", numCol);
        int srcRow = matrix.getNumRow();
        int srcCol = matrix.getNumCol();
        if (numRow == srcRow && numCol == srcCol) {
            return matrix;
        }
        final int      stride  = srcCol;
        final int      length  = srcCol * srcRow;
        final double[] sources = getExtendedElements(matrix);
        final DoubleDouble transfer = (sources.length > length) ? new DoubleDouble() : null;
        final MatrixSIS resized = createZero(numRow, numCol, transfer != null);
        final int copyRow = Math.min(--numRow, --srcRow);
        final int copyCol = Math.min(--numCol, --srcCol);
        for (int j=copyRow; j<numRow; j++) {
            resized.setElement(j, j, 1);
        }
        resized.setElements(sources, length, stride, transfer, 0,      0,       0,      0,       copyRow, copyCol);    // Shear and scale terms.
        resized.setElements(sources, length, stride, transfer, 0,      srcCol,  0,      numCol,  copyRow, 1);          // Translation column.
        resized.setElements(sources, length, stride, transfer, srcRow, 0,       numRow, 0,       1,       copyCol);    // Last row.
        resized.setElements(sources, length, stride, transfer, srcRow, srcCol,  numRow, numCol,  1,       1);          // Last row.
        return resized;
    }

    /**
     * Returns {@code true} if the given matrix is likely to use extended precision.
     * A value of {@code true} is not a guarantee that the matrix uses extended precision,
     * but a value of {@code false} is a guarantee that it does not.
     */
    private static boolean isExtendedPrecision(final Matrix matrix) {
        return (matrix instanceof MatrixSIS) ? ((MatrixSIS) matrix).isExtendedPrecision() :
               (matrix instanceof ExtendedPrecisionMatrix);  // Not guarantee that the matrix really uses double-double.
    }

    /**
     * Returns the elements of the given matrix, together with error terms if available.
     */
    private static double[] getExtendedElements(final Matrix matrix) {
        if (matrix instanceof ExtendedPrecisionMatrix) {
            return ((ExtendedPrecisionMatrix) matrix).getExtendedElements();
        } else {
            return MatrixSIS.castOrCopy(matrix).getElements();
        }
    }

    /**
     * Creates a new matrix which is a copy of the given matrix.
     *
     * <div class="note"><b>Implementation note:</b>
     * For square matrix with a size between {@value org.apache.sis.referencing.operation.matrix.Matrix1#SIZE}
     * and {@value org.apache.sis.referencing.operation.matrix.Matrix4#SIZE} inclusive, the returned matrix is
     * usually an instance of one of {@link Matrix1} … {@link Matrix4} subtypes.</div>
     *
     * @param matrix The matrix to copy, or {@code null}.
     * @return A copy of the given matrix, or {@code null} if the given matrix was null.
     *
     * @see MatrixSIS#clone()
     * @see MatrixSIS#castOrCopy(Matrix)
     */
    public static MatrixSIS copy(final Matrix matrix) {
        if (matrix == null) {
            return null;
        }
        final int size = matrix.getNumRow();
        if (size != matrix.getNumCol()) {
            return new NonSquareMatrix(matrix);
        }
        if (!isExtendedPrecision(matrix)) {
            switch (size) {
                case 1: return new Matrix1(matrix);
                case 2: return new Matrix2(matrix);
                case 3: return new Matrix3(matrix);
                case 4: return new Matrix4(matrix);
            }
        }
        return new GeneralMatrix(matrix);
    }

    /**
     * Returns an unmodifiable view of the given matrix. The returned matrix is immutable
     * only if the given {@code matrix} is not modified anymore after this method call.
     *
     * @param matrix The matrix for which to get an unmodifiable view, or {@code null}.
     * @return A unmodifiable view of the given matrix, or {@code null} if the given matrix was null.
     *
     * @since 0.6
     */
    public static MatrixSIS unmodifiable(final Matrix matrix) {
        if (matrix == null || matrix instanceof UnmodifiableMatrix) {
            return (MatrixSIS) matrix;
        } else {
            return new UnmodifiableMatrix(matrix);
        }
    }

    /**
     * Returns a new matrix which is the result of multiplying the first matrix with the second one.
     * In other words, returns {@code m1} × {@code m2}.
     *
     * @param  m1 The first matrix to multiply.
     * @param  m2 The second matrix to multiply.
     * @return The result of {@code m1} × {@code m2}.
     * @throws MismatchedMatrixSizeException if the number of columns in {@code m1} is not equals to the
     *         number of rows in {@code m2}.
     *
     * @see MatrixSIS#multiply(Matrix)
     *
     * @since 0.6
     */
    public static MatrixSIS multiply(final Matrix m1, final Matrix m2) throws MismatchedMatrixSizeException {
        if (m1 instanceof MatrixSIS) {
            return ((MatrixSIS) m1).multiply(m2);           // Maybe the subclass overrides that method.
        }
        final int nc = m2.getNumCol();
        MatrixSIS.ensureNumRowMatch(m1.getNumCol(), m2.getNumRow(), nc);
        final GeneralMatrix result = GeneralMatrix.createExtendedPrecision(m1.getNumRow(), nc, false);
        result.setToProduct(m1, m2);
        return result;
    }

    /**
     * Returns the inverse of the given matrix.
     *
     * @param  matrix The matrix to inverse, or {@code null}.
     * @return The inverse of this matrix, or {@code null} if the given matrix was null.
     * @throws NoninvertibleMatrixException if the given matrix is not invertible.
     *
     * @see MatrixSIS#inverse()
     *
     * @since 0.6
     */
    public static MatrixSIS inverse(final Matrix matrix) throws NoninvertibleMatrixException {
        if (matrix == null) {
            return null;
        } else if (matrix instanceof MatrixSIS) {
            return ((MatrixSIS) matrix).inverse();  // Maybe the subclass override that method.
        } else if (matrix.getNumRow() != matrix.getNumCol()) {
            return new NonSquareMatrix(matrix).inverse();
        } else {
            return Solver.inverse(matrix, true);
        }
    }

    /**
     * Returns {@code true} if the given matrix represents an affine transform.
     * A transform is affine if the matrix is square and its last row contains
     * only zeros, except in the last column which contains 1.
     *
     * @param matrix The matrix to test.
     * @return {@code true} if the matrix represents an affine transform.
     *
     * @see MatrixSIS#isAffine()
     */
    public static boolean isAffine(final Matrix matrix) {
        if (matrix instanceof MatrixSIS) {
            return ((MatrixSIS) matrix).isAffine();
        } else {
            return MatrixSIS.isAffine(matrix);
        }
    }

    /**
     * Returns {@code true} if the given matrix represents a translation.
     * This method returns {@code true} if the given matrix {@linkplain #isAffine(Matrix) is affine}
     * and differs from the identity matrix only in the last column.
     *
     * @param matrix The matrix to test.
     * @return {@code true} if the matrix represents a translation.
     *
     * @since 0.7
     */
    public static boolean isTranslation(final Matrix matrix) {
        if (!isAffine(matrix)) {
            return false;
        }
        final int numRow = matrix.getNumRow() - 1;      // Excluding translation column.
        final int numCol = matrix.getNumCol() - 1;      // Excluding last row in affine transform.
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                if (matrix.getElement(j,i) != ((i == j) ? 1 : 0)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the given matrix is close to an identity matrix, given a tolerance threshold.
     * This method is equivalent to computing the difference between the given matrix and an identity matrix
     * of identical size, and returning {@code true} if and only if all differences are smaller than or equal
     * to {@code tolerance}.
     *
     * <p><b>Caution:</b> {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa-Wolf parameters},
     * when represented as a matrix, are close to an identity transform and could easily be confused with rounding
     * errors. In case of doubt, it is often safer to use the strict {@link MatrixSIS#isIdentity()} method instead
     * than this one.</p>
     *
     * @param  matrix The matrix to test for identity.
     * @param  tolerance The tolerance value, or 0 for a strict comparison.
     * @return {@code true} if this matrix is close to the identity matrix given the tolerance threshold.
     *
     * @see MatrixSIS#isIdentity()
     */
    public static boolean isIdentity(final Matrix matrix, final double tolerance) {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        if (numRow != numCol) {
            return false;
        }
        for (int j=0; j<numRow; j++) {
            for (int i=0; i<numCol; i++) {
                double e = matrix.getElement(j,i);
                if (i == j) {
                    e--;
                }
                if (!(Math.abs(e) <= tolerance)) {  // Uses '!' in order to catch NaN values.
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compares the given matrices for equality, using the given relative or absolute tolerance threshold.
     * The matrix elements are compared as below:
     *
     * <ul>
     *   <li>{@link Double#NaN} values are considered equals to all other NaN values</li>
     *   <li>Infinite values are considered equal to other infinite values of the same sign</li>
     *   <li>All other values are considered equal if the absolute value of their difference is
     *       smaller than or equals to the threshold described below.</li>
     * </ul>
     *
     * If {@code relative} is {@code true}, then for any pair of values <var>v1</var><sub>j,i</sub>
     * and <var>v2</var><sub>j,i</sub> to compare, the tolerance threshold is scaled by
     * {@code max(abs(v1), abs(v2))}. Otherwise the threshold is used as-is.
     *
     * @param  m1       The first matrix to compare, or {@code null}.
     * @param  m2       The second matrix to compare, or {@code null}.
     * @param  epsilon  The tolerance value.
     * @param  relative If {@code true}, then the tolerance value is relative to the magnitude
     *         of the matrix elements being compared.
     * @return {@code true} if the values of the two matrix do not differ by a quantity
     *         greater than the given tolerance threshold.
     *
     * @see MatrixSIS#equals(Matrix, double)
     */
    public static boolean equals(final Matrix m1, final Matrix m2, final double epsilon, final boolean relative) {
        if (m1 != m2) {
            if (m1 == null || m2 == null) {
                return false;
            }
            final int numRow = m1.getNumRow();
            if (numRow != m2.getNumRow()) {
                return false;
            }
            final int numCol = m1.getNumCol();
            if (numCol != m2.getNumCol()) {
                return false;
            }
            for (int j=0; j<numRow; j++) {
                for (int i=0; i<numCol; i++) {
                    final double v1 = m1.getElement(j, i);
                    final double v2 = m2.getElement(j, i);
                    double tolerance = epsilon;
                    if (relative) {
                        tolerance *= Math.max(Math.abs(v1), Math.abs(v2));
                    }
                    if (!(Math.abs(v1 - v2) <= tolerance)) {
                        if (Numerics.equals(v1, v2)) {
                            // Special case for NaN and infinite values.
                            continue;
                        }
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Compares the given matrices for equality, using the given comparison strictness level.
     * To be considered equal, the two matrices must meet the following conditions, which depend
     * on the {@code mode} argument:
     *
     * <ul>
     *   <li>{@link ComparisonMode#STRICT STRICT}:
     *       the two matrices must be of the same class, have the same size and the same element values.</li>
     *   <li>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT}:
     *       the two matrices must have the same size and the same element values,
     *       but are not required to be the same implementation class (any {@link Matrix} is okay).</li>
     *   <li>{@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA}: same as {@code BY_CONTRACT},
     *       since matrices have no metadata.</li>
     *   <li>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE}:
     *       the two matrices must have the same size, but the element values can differ up to some threshold.
     *       The threshold value is determined empirically and may change in any future SIS versions.
     *       For more control, use {@link #equals(Matrix, Matrix, double, boolean)} instead.</li>
     * </ul>
     *
     * @param  m1  The first matrix to compare, or {@code null}.
     * @param  m2  The second matrix to compare, or {@code null}.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both matrices are equal.
     *
     * @see MatrixSIS#equals(Object, ComparisonMode)
     */
    public static boolean equals(final Matrix m1, final Matrix m2, final ComparisonMode mode) {
        switch (mode) {
            case STRICT:          return Objects.equals(m1, m2);
            case BY_CONTRACT:     // Fall through
            case IGNORE_METADATA: return equals(m1, m2, 0, false);
            case DEBUG:           // Fall through
            case ALLOW_VARIANT:   // Fall through
            case APPROXIMATIVE:   return equals(m1, m2, Numerics.COMPARISON_THRESHOLD, true);
            default: throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UnknownEnumValue_2, ComparisonMode.class, mode));
        }
    }

    /**
     * Returns a unlocalized string representation of the given matrix.
     * For each column, the numbers are aligned on the decimal separator.
     *
     * <p>The current implementation formats ±0 and ±1 without trailing {@code ".0"}, and all other values with
     * a per-column uniform number of fraction digits. The ±0 and ±1 values are treated especially because they
     * usually imply a <cite>"no scale"</cite>, <cite>"no translation"</cite> or <cite>"orthogonal axes"</cite>
     * meaning. A matrix in SIS is often populated mostly by ±0 and ±1 values, with a few "interesting" values.
     * The simpler ±0 and ±1 formatting makes easier to spot the "interesting" values.</p>
     *
     * <p>The following example shows the string representation of an affine transform which swap
     * (<var>latitude</var>, <var>longitude</var>) axes, converts degrees to radians and converts
     * height values from feet to metres:</p>
     *
     * {@preformat math
     *   ┌                                                       ┐
     *   │ 0                     0.017453292519943295  0       0 │
     *   │ 0.017453292519943295  0                     0       0 │
     *   │ 0                     0                     0.3048  0 │
     *   │ 0                     0                     0       1 │
     *   └                                                       ┘
     * }
     *
     * <div class="note"><b>Note:</b>
     * Formatting on a per-column basis is convenient for the kind of matrices used in referencing by coordinates,
     * because each column is typically a displacement vector in a different dimension of the source coordinate
     * reference system. In addition, the last column is often a translation vector having a magnitude very
     * different than the other columns.</div>
     *
     * @param  matrix The matrix for which to get a string representation.
     * @return A string representation of the given matrix.
     */
    public static String toString(final Matrix matrix) {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        final String[]  elements            = new String [numCol * numRow]; // String representation of matrix values.
        final boolean[] noFractionDigits    = new boolean[numCol * numRow]; // Whether to remove the trailing ".0" for a given number.
        final boolean[] hasDecimalSeparator = new boolean[numCol];          // Whether the column has at least one number where fraction digits are shown.
        final byte[] maximumFractionDigits  = new byte   [numCol];          // The greatest amount of fraction digits found in a column.
        final byte[] maximumPaddingZeros    = new byte   [numCol * numRow]; // Maximal amount of zeros that we can append before to exceed the IEEE 754 accuracy.
        final byte[] widthBeforeFraction    = new byte   [numCol];          // Number of characters before the fraction digits: spacing + ('-') + integerDigits + '.'
        final byte[] columnWidth            = new byte   [numCol];          // Total column width.
        int totalWidth = 1;
        /*
         * Create now the string representation of all matrix elements and measure the width
         * of the integer field and the fraction field, then the total width of each column.
         */
        int spacing = 1; // Spacing is 1 before the first column only, then SPACING for other columns.
        for (int i=0; i<numCol; i++) {
            for (int j=0; j<numRow; j++) {
                final int flatIndex = j*numCol + i;
                final double value  = matrix.getElement(j,i);
                String element = Double.toString(value);
                final int width;
                /*
                 * Special case for ±0 and ±1 (because those values appear very often and have
                 * a particular meaning): for those values, we will ignore the fraction digits.
                 * For all other values, we will format all fraction digits.
                 */
                if (value == -1 || value == 0 || value == +1) {
                    noFractionDigits[flatIndex] = true;
                    width = spacing + element.length() - 2; // The -2 is for ignoring the trailing ".0"
                    widthBeforeFraction[i] = (byte) Math.max(widthBeforeFraction[i], width);
                } else {
                    /*
                     * All values other than ±0 and ±1. If the values is NaN or infinity (in which case there is
                     * no decimal separator), give all spaces to the "before fraction" side for right-alignment.
                     */
                    int s = element.lastIndexOf('.');
                    if (s < 0) {
                        element = CharSequences.replace(element, "Infinity", "∞").toString();
                        width = spacing + element.length();
                        widthBeforeFraction[i] = (byte) Math.max(widthBeforeFraction[i], width);
                    } else {
                        /*
                         * All values other than ±0, ±1, NaN and infinity. We store separately the width before
                         * and after the decimal separator. The width before the separator contains the spacing
                         * between cells.
                         */
                        hasDecimalSeparator[i] = true;
                        final int numFractionDigits = element.length() - ++s;
                        width = (widthBeforeFraction  [i] = (byte) Math.max(widthBeforeFraction  [i], spacing + s))
                              + (maximumFractionDigits[i] = (byte) Math.max(maximumFractionDigits[i], numFractionDigits));
                        /*
                         * If the number use exponential notation, we will not be allowed to append any zero.
                         * Otherwise we will append some zeros for right-alignment, but without exceeding the
                         * IEEE 754 'double' accuracy for not giving a false sense of precision.
                         */
                        if (element.indexOf('E') < 0) {
                            final int accuracy = (int) Math.ceil(-Math.log10(Math.ulp(value)));
                            maximumPaddingZeros[flatIndex] = (byte) (accuracy - numFractionDigits);
                        }
                    }
                }
                columnWidth[i] = (byte) Math.max(columnWidth[i], width);
                elements[flatIndex] = element;
            }
            totalWidth += columnWidth[i];
            spacing = SPACING; // Specing before all columns after the first one.
        }
        /*
         * Now append the formatted elements with the appropriate amount of spaces before each value,
         * and trailling zeros after each value except ±0, ±1, NaN and infinities.
         */
        final String   lineSeparator = JDK7.lineSeparator();
        final CharSequence whiteLine = CharSequences.spaces(totalWidth);
        final StringBuilder   buffer = new StringBuilder((totalWidth + 2 + lineSeparator.length()) * (numRow + 2));
        buffer.append('┌').append(whiteLine).append('┐').append(lineSeparator);
        int flatIndex = 0;
        for (int j=0; j<numRow; j++) {
            buffer.append('│');
            for (int i=0; i<numCol; i++) {
                final String element = elements[flatIndex];
                final int width = element.length();
                int spaces, s = element.lastIndexOf('.');
                if (s >= 0) {
                    if (hasDecimalSeparator[i]) s++;
                    spaces = widthBeforeFraction[i] - s; // Number of spaces for alignment on the decimal separator
                } else {
                    spaces = columnWidth[i] - width; // Number of spaces for right alignment (NaN or ∞ cases)
                }
                buffer.append(CharSequences.spaces(spaces)).append(element);
                if (s >= 0) {
                    /*
                     * Append trailing spaces for ±0 and ±1 values,
                     * or trailing zeros for all other real values.
                     */
                    s += maximumFractionDigits[i] - width;
                    if (noFractionDigits[flatIndex]) {
                        buffer.setLength(buffer.length() - 2); // Erase the trailing ".0"
                        s += 2;
                    } else {
                        int n = Math.min(s, maximumPaddingZeros[flatIndex]);
                        s -= n;
                        while (--n >= 0) {
                            buffer.append('0');
                        }
                    }
                    buffer.append(CharSequences.spaces(s));
                }
                flatIndex++;
            }
            buffer.append(" │").append(lineSeparator);
        }
        return buffer.append('└').append(whiteLine).append('┘').append(lineSeparator).toString();
    }
}
