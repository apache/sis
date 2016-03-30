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
package org.apache.sis.referencing.operation.transform;

import java.util.Arrays;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Quantity;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.LinearConverter;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.measure.Units;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.internal.referencing.provider.NTv2;
import org.apache.sis.internal.referencing.provider.DatumShiftGridFile;

// Branch-specific imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Transforms between two CRS by performing translations interpolated from a grid file.
 * The source and target coordinate reference systems are typically, but not necessarily,
 * two-dimensional {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic CRS}.
 * The actual number of dimensions is determined by {@link DatumShiftGrid#getTranslationDimensions()}.
 *
 * <div class="note"><b>Example:</b>
 * this transform is used for example with NADCON and NTv2 datum shift grids.</div>
 *
 * <div class="section">Input and output coordinates</div>
 * First, <cite>"real world"</cite> input coordinates (<var>x</var>,<var>y</var>) are converted to
 * <cite>grid</cite> coordinates (<var>gridX</var>, <var>gridY</var>), which are zero-based indices
 * in the two-dimensional grid. This conversion is applied by an affine transform <em>before</em>
 * to be passed to the {@code transform} methods of this {@code InterpolatedTransform} class.
 *
 * <p>Translation vectors are stored in the datum shift grid at the specified grid indices.
 * If the grid indices are non-integer values, then the translations are interpolated using a bilinear interpolation.
 * If the grid indices are outside the grid domain ([0 … <var>width</var>-2] × [0 … <var>height</var>-2]
 * where <var>width</var> and <var>height</var> are the number of columns and rows in the grid),
 * then the translations are extrapolated. The translation is then added to the input coordinates.</p>
 *
 * <p>The input and output coordinates can have any number of dimensions, provided that they are the same
 * than the number of {@linkplain DatumShiftGrid#getTranslationDimensions() translation dimensions}.
 * However current implementation uses only the two first dimensions for interpolations in the grid.</p>
 *
 * @author  Rémi Eve (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Simon Reynard (Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class InterpolatedTransform extends DatumShiftTransform {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8962688502524486475L;

    /**
     * Number of dimensions used for interpolating in the datum shift grid.
     * This is not necessarily the same than the number of dimensions of interpolated values.
     * In current SIS implementation, this number of dimensions is fixed by the {@link DatumShiftGrid} API.
     */
    private static final int GRID_DIMENSION = 2;

    /**
     * The value of {@link DatumShiftGrid#getTranslationDimensions()}, stored for efficiency.
     */
    private final int dimension;

    /**
     * The inverse of this interpolated transform.
     *
     * @see #inverse()
     */
    private final InterpolatedTransform.Inverse inverse;

    /**
     * Creates a transform for the given interpolation grid.
     * This {@code InterpolatedTransform} class works with ordinate values in <em>units of grid cell</em>
     * For example input coordinate (4,5) is the position of the center of the cell at grid index (4,5).
     * The output units are the same than the input units.
     *
     * <p>For converting geodetic coordinates, {@code InterpolatedTransform} instances need to be concatenated
     * with the following affine transforms:
     *
     * <ul>
     *   <li><cite>Normalization</cite> before {@code InterpolatedTransform}
     *     for converting the geodetic coordinates into grid coordinates.</li>
     *   <li><cite>Denormalization</cite> after {@code InterpolatedTransform}
     *     for converting grid coordinates into geodetic coordinates.</li>
     * </ul>
     *
     * After {@code InterpolatedTransform} construction,
     * the full conversion chain including the above affine transforms can be created by
     * <code>{@linkplain #getContextualParameters()}.{@linkplain ContextualParameters#completeTransform
     * completeTransform}(factory, this)}</code>.
     *
     * @param  <T> Dimension of the coordinate and the translation unit.
     * @param  grid The grid of datum shifts from source to target datum.
     * @throws NoninvertibleMatrixException if the conversion from geodetic coordinates
     *         to grid indices can not be inverted.
     *
     * @see #createGeodeticTransformation(MathTransformFactory, DatumShiftGrid)
     */
    @SuppressWarnings("OverridableMethodCallDuringObjectConstruction")
    protected <T extends Quantity> InterpolatedTransform(final DatumShiftGrid<T,T> grid)
            throws NoninvertibleMatrixException
    {
        /*
         * Create the contextual parameters using the descriptor of the provider that created the datum shift grid.
         * If that provider is unknown, default (for now) to NTv2. This default may change in any future SIS version.
         */
        super((grid instanceof DatumShiftGridFile<?,?>) ? ((DatumShiftGridFile<?,?>) grid).descriptor : NTv2.PARAMETERS, grid);
        if (!grid.isCellValueRatio()) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalParameterValue_2, "isCellValueRatio", Boolean.FALSE));
        }
        final Unit<T> unit = grid.getTranslationUnit();
        if (unit != grid.getCoordinateUnit()) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalUnitFor_2, "translation", unit));
        }
        dimension = grid.getTranslationDimensions();
        if (grid instanceof DatumShiftGridFile<?,?>) {
            ((DatumShiftGridFile<?,?>) grid).setFileParameters(context);
        }
        /*
         * Set the normalization matrix to the conversion from grid coordinates (e.g. seconds of angle)
         * to grid indices. This will allow us to invoke DatumShiftGrid.interpolateAtCell(x, y, vector)
         * directly in the transform(…) methods.
         */
        final MatrixSIS normalize = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        normalize.setMatrix(grid.getCoordinateToGrid().getMatrix());
        /*
         * NADCON and NTv2 datum shift grids expect geographic coordinates in seconds of angle, while
         * MathTransform instances created by DefaultMathTransformFactory.createParameterized(…) must
         * expect coordinates in standardized units (degrees of angle, metres, seconds, etc.).
         * We concatenate the unit conversion with above "coordinate to grid" conversion.
         */
        @SuppressWarnings("unchecked")
        final Unit<T> normalized = Units.isAngular(unit) ? (Unit<T>) NonSI.DEGREE_ANGLE : unit.toSI();
        if (!unit.equals(normalized)) {
            final UnitConverter converter = normalized.getConverterTo(unit);
            if (!(converter instanceof LinearConverter)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.NonLinearUnitConversion_2, normalized, unit));
            }
            final Double offset = converter.convert(0);
            final Double scale  = Units.derivative(converter, 0);
            for (int j=0; j<dimension; j++) {
                normalize.convertBefore(j, scale, offset);
            }
        }
        /*
         * Denormalization is the inverse of all above conversions.
         */
        context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION).setMatrix(normalize.inverse());
        inverse = createInverse();
    }

    /**
     * Creates a transformation between two geodetic CRS. This factory method combines the
     * {@code InterpolatedTransform} instance with the steps needed for converting values between
     * geodetic and grid coordinates.
     *
     * <div class="section">Unit of measurement</div>
     * The unit of measurement is determined by {@link DatumShiftGrid#getCoordinateUnit()}:
     * <ul>
     *   <li>If the datum shift unit {@linkplain Units#isAngular(Unit) is angular}, then the transform
     *       will work with input and output coordinates in degrees of angle.</li>
     *   <li>If the datum shift unit {@linkplain Units#isLinear(Unit) is linear}, then the transform
     *       will work with input and output coordinates in metres.</li>
     *   <li>If the datum shift unit {@linkplain Units#isTemporal(Unit) is temporal}, then the transform
     *       will work with input and output coordinates in seconds.</li>
     *   <li>Generally for all units other than angular, the transform will work with input and output
     *       coordinates in the unit given by {@link Unit#toSI()}.</li>
     * </ul>
     *
     * @param <T>      Dimension of the coordinate and the translation unit.
     * @param factory  The factory to use for creating the transform.
     * @param grid     The grid of datum shifts from source to target datum.
     *                 The {@link DatumShiftGrid#interpolateInCell DatumShiftGrid.interpolateInCell(…)}
     *                 method shall compute translations from <em>source</em> to <em>target</em> as
     *                 {@linkplain DatumShiftGrid#isCellValueRatio() ratio of offsets divided by cell sizes}.
     * @return The transformation between geodetic coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static <T extends Quantity> MathTransform createGeodeticTransformation(
            final MathTransformFactory factory, final DatumShiftGrid<T,T> grid) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("grid", grid);
        final InterpolatedTransform tr;
        try {
            if (grid.getTranslationDimensions() == 2) {
                tr = new InterpolatedTransform2D(grid);
            } else {
                tr = new InterpolatedTransform(grid);
            }
        } catch (NoninvertibleMatrixException e) {
            throw new FactoryException(e.getLocalizedMessage(), e);
        }
        return tr.context.completeTransform(factory, tr);
    }

    /**
     * Returns the number of input dimensions.
     * This fixed to {@link DatumShiftGrid#getTranslationDimensions()}.
     *
     * @return The dimension of input points.
     */
    @Override
    public final int getSourceDimensions() {
        return dimension;
    }

    /**
     * Returns the number of target dimensions.
     * This fixed to {@link DatumShiftGrid#getTranslationDimensions()}.
     *
     * @return The dimension of output points.
     */
    @Override
    public final int getTargetDimensions() {
        return dimension;
    }

    /**
     * Applies the datum shift on a coordinate and optionally returns the derivative at that location.
     *
     * @return {@inheritDoc}
     * @throws TransformException if the point can not be transformed or
     *         if a problem occurred while calculating the derivative.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        final double x = srcPts[srcOff  ];
        final double y = srcPts[srcOff+1];
        if (dstPts != null) {
            final double[] vector = new double[dimension];
            grid.interpolateInCell(x, y, vector);
            if (dimension > GRID_DIMENSION) {
                System.arraycopy(srcPts, srcOff + GRID_DIMENSION,
                                 dstPts, dstOff + GRID_DIMENSION,
                                      dimension - GRID_DIMENSION);
                /*
                 * We can not use srcPts[srcOff + i] = dstPts[dstOff + i] + offset[i]
                 * because the arrays may overlap. The contract said that this method
                 * must behave as if all input ordinate values have been read before
                 * we write outputs, which is the reason for System.arraycopy(…) call.
                 */
                int i = dimension;
                do dstPts[dstOff + --i] += vector[i];
                while (i > GRID_DIMENSION);
            }
            dstPts[dstOff+1] = y + vector[1];
            dstPts[dstOff  ] = x + vector[0];      // Shall not be done before above loop.
        }
        if (!derivate) {
            return null;
        }
        return grid.derivativeInCell(x, y);
    }

    /**
     * Transforms an arbitrary amount of coordinates.
     *
     * @throws TransformException if a point can not be transformed.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        int inc = dimension;
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, inc, dstOff, inc, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts-1) * inc;
                    dstOff += (numPts-1) * inc;
                    inc = -inc;
                    break;
                }
                default: {  // BUFFER_SOURCE, but also a reasonable default for any case.
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*inc);
                    srcOff = 0;
                    break;
                }
            }
        }
        final double[] vector = new double[dimension];
        while (--numPts >= 0) {
            final double x = srcPts[srcOff  ];
            final double y = srcPts[srcOff+1];
            grid.interpolateInCell(x, y, vector);
            if (dimension > GRID_DIMENSION) {
                System.arraycopy(srcPts, srcOff + GRID_DIMENSION,
                                 dstPts, dstOff + GRID_DIMENSION,
                                      dimension - GRID_DIMENSION);
                /*
                 * We can not use srcPts[srcOff + i] = dstPts[dstOff + i] + offset[i]
                 * because the arrays may overlap. The contract said that this method
                 * must behave as if all input ordinate values have been read before
                 * we write outputs, which is the reason for System.arraycopy(…) call.
                 */
                int i = dimension;
                do dstPts[dstOff + --i] += vector[i];
                while (i > GRID_DIMENSION);
            }
            dstPts[dstOff+1] = y + vector[1];
            dstPts[dstOff  ] = x + vector[0];      // Shall not be done before above loop.
            dstOff += inc;
            srcOff += inc;
        }
    }

    /*
     * NOTE: we do not bother to override the methods expecting a 'float' array because those methods should
     *       be rarely invoked. Since there is usually LinearTransforms before and after this transform, the
     *       conversion between float and double will be handled by those LinearTransforms.  If nevertheless
     *       this MolodenskyTransform is at the beginning or the end of a transformation chain,  the methods
     *       inherited from the subclass will work (but may be slightly slower).
     */

    /**
     * Returns the inverse of this interpolated transform.
     * The source ellipsoid of the returned transform will be the target ellipsoid of this transform, and conversely.
     *
     * @return A transform from the target ellipsoid to the source ellipsoid of this transform.
     */
    @Override
    public MathTransform inverse() {
        return inverse;
    }

    /**
     * Invoked at construction time for creating the inverse transform.
     * To overridden by the two-dimensional transform case.
     */
    InterpolatedTransform.Inverse createInverse() {
        return new Inverse();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() + Objects.hashCode(grid);
    }

    /**
     * Compares the specified object with this math transform for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            // Slight optimization
            return true;
        }
        return super.equals(object, mode) && Objects.equals(grid, ((InterpolatedTransform) object).grid);
        // No need to compare the contextual parameters since this is done by super-class.
    }

    /**
     * Transforms target coordinates to source coordinates. This is done by iteratively finding a target coordinate
     * that shifts to the input coordinate. The input coordinate is used as the first approximation.
     *
     * @author  Rueben Schulz (UBC)
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @version 0.7
     * @since   0.7
     * @module
     */
    class Inverse extends AbstractMathTransform.Inverse {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = -6779719408779847014L;

        /**
         * Difference allowed in iterative computations, in units of grid cell size.
         */
        private final double tolerance;

        /**
         * Creates an inverse transform.
         */
        Inverse() {
            InterpolatedTransform.this.super();
            tolerance = grid.getCellPrecision();
            if (!(tolerance > 0)) {         // Use ! for catching NaN.
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.ValueNotGreaterThanZero_2, "grid.cellPrecision", tolerance));
            }
        }

        /**
         * Transforms a single coordinate in a list of ordinal values,
         * and optionally returns the derivative at that location.
         *
         * @throws TransformException If there is no convergence.
         */
        @Override
        public final Matrix transform(final double[] srcPts, final int srcOff, double[] dstPts, int dstOff,
                                      final boolean derivate) throws TransformException
        {
            if (dstPts == null) {
                dstPts = new double[dimension];
                dstOff = 0;
            }
            double xi, yi;
            final double x = xi = srcPts[srcOff  ];
            final double y = yi = srcPts[srcOff+1];
            final double[] vector = new double[dimension];
            int it = Formulas.MAXIMUM_ITERATIONS;
            do {
                grid.interpolateInCell(xi, yi, vector);
                final double ox = xi;
                final double oy = yi;
                xi = x - vector[0];
                yi = y - vector[1];
                if (!(Math.abs(xi - ox) > tolerance ||    // Use '!' for catching NaN.
                      Math.abs(yi - oy) > tolerance))
                {
                    if (dimension > GRID_DIMENSION) {
                        System.arraycopy(srcPts, srcOff + GRID_DIMENSION,
                                         dstPts, dstOff + GRID_DIMENSION,
                                              dimension - GRID_DIMENSION);
                        /*
                         * We can not use srcPts[srcOff + i] = dstPts[dstOff + i] + offset[i]
                         * because the arrays may overlap. The contract said that this method
                         * must behave as if all input ordinate values have been read before
                         * we write outputs, which is the reason for System.arraycopy(…) call.
                         */
                        int i = dimension;
                        do dstPts[dstOff + --i] += vector[i];
                        while (i > GRID_DIMENSION);
                    }
                    dstPts[dstOff  ] = xi;      // Shall not be done before above loop.
                    dstPts[dstOff+1] = yi;
                    if (derivate) {
                        return Matrices.inverse(InterpolatedTransform.this.derivative(
                                new DirectPositionView(dstPts, dstOff, dimension)));
                    }
                    return null;
                }
            } while (--it >= 0);
            throw new TransformException(Errors.format(Errors.Keys.NoConvergence));
        }

        /**
         * Transforms an arbitrary amount of coordinates.
         *
         * @throws TransformException if a point can not be transformed.
         */
        @Override
        public final void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts)
                throws TransformException
        {
            int inc = dimension;
            if (srcPts == dstPts) {
                switch (IterationStrategy.suggest(srcOff, inc, dstOff, inc, numPts)) {
                    case ASCENDING: {
                        break;
                    }
                    case DESCENDING: {
                        srcOff += (numPts-1) * inc;
                        dstOff += (numPts-1) * inc;
                        inc = -inc;
                        break;
                    }
                    default: {  // BUFFER_SOURCE, but also a reasonable default for any case.
                        srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*inc);
                        srcOff = 0;
                        break;
                    }
                }
            }
            final double[] vector = new double[dimension];
nextPoint:  while (--numPts >= 0) {
                double xi, yi;
                final double x = xi = srcPts[srcOff  ];
                final double y = yi = srcPts[srcOff+1];
                int it = Formulas.MAXIMUM_ITERATIONS;
                do {
                    grid.interpolateInCell(xi, yi, vector);
                    final double ox = xi;
                    final double oy = yi;
                    xi = x - vector[0];
                    yi = y - vector[1];
                    if (!(Math.abs(xi - ox) > tolerance ||    // Use '!' for catching NaN.
                          Math.abs(yi - oy) > tolerance))
                    {
                        if (dimension > GRID_DIMENSION) {
                            System.arraycopy(srcPts, srcOff + GRID_DIMENSION,
                                             dstPts, dstOff + GRID_DIMENSION,
                                                  dimension - GRID_DIMENSION);
                            /*
                             * We can not use srcPts[srcOff + i] = dstPts[dstOff + i] + offset[i]
                             * because the arrays may overlap. The contract said that this method
                             * must behave as if all input ordinate values have been read before
                             * we write outputs, which is the reason for System.arraycopy(…) call.
                             */
                            int i = dimension;
                            do dstPts[dstOff + --i] += vector[i];
                            while (i > GRID_DIMENSION);
                        }
                        dstPts[dstOff  ] = xi;      // Shall not be done before above loop.
                        dstPts[dstOff+1] = yi;
                        dstOff += inc;
                        srcOff += inc;
                        continue nextPoint;
                    }
                } while (--it >= 0);
                throw new TransformException(Errors.format(Errors.Keys.NoConvergence));
            }
        }
    }
}
