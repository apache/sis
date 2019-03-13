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
import java.util.Objects;
import java.io.Serializable;
import javax.measure.Unit;
import javax.measure.Quantity;
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
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.internal.referencing.provider.NTv2;
import org.apache.sis.internal.referencing.provider.DatumShiftGridFile;


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
 * If the grid indices are outside the grid domain ([0 … <var>width</var>-1] × [0 … <var>height</var>-1]
 * where <var>width</var> and <var>height</var> are the number of columns and rows in the grid),
 * then the translations are extrapolated. The translation is finally added to the input coordinates.</p>
 *
 * <p>The input and output coordinates can have any number of dimensions, provided that they are the same
 * than the number of {@linkplain DatumShiftGrid#getTranslationDimensions() translation dimensions}.
 * However current implementation uses only the two first dimensions for interpolations in the grid.</p>
 *
 * @author  Rémi Eve (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Simon Reynard (Geomatys)
 * @author  Rueben Schulz (UBC)
 * @version 0.8
 *
 * @see DatumShiftGrid
 * @see org.apache.sis.referencing.operation.builder.LocalizationGridBuilder
 *
 * @since 0.7
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
     * This {@code InterpolatedTransform} class works with coordinate values in <em>units of grid cell</em>
     * For example input coordinates (4,5) is the position of the center of the cell at grid index (4,5).
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
     * @param  <T>   dimension of the coordinate tuples and the translation unit.
     * @param  grid  the grid of datum shifts from source to target datum.
     * @throws NoninvertibleMatrixException if the conversion from geodetic coordinates
     *         to grid indices can not be inverted.
     *
     * @see #createGeodeticTransformation(MathTransformFactory, DatumShiftGrid)
     */
    @SuppressWarnings( {"OverridableMethodCallDuringObjectConstruction", "fallthrough"})
    protected <T extends Quantity<T>> InterpolatedTransform(final DatumShiftGrid<T,T> grid)
            throws NoninvertibleMatrixException
    {
        /*
         * Create the contextual parameters using the descriptor of the provider that created the datum shift grid.
         * If that provider is unknown, default (for now) to NTv2. This default may change in any future SIS version.
         */
        super((grid instanceof DatumShiftGridFile<?,?>) ? ((DatumShiftGridFile<?,?>) grid).descriptor : NTv2.PARAMETERS, grid);
        if (!grid.isCellValueRatio()) {
            throw new IllegalArgumentException(Resources.format(
                    Resources.Keys.IllegalParameterValue_2, "isCellValueRatio", Boolean.FALSE));
        }
        final Unit<T> unit = grid.getTranslationUnit();
        if (unit != grid.getCoordinateUnit()) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.IllegalUnitFor_2, "translation", unit));
        }
        dimension = grid.getTranslationDimensions();
        /*
         * Set the normalization matrix to the conversion from source coordinates (e.g. seconds of angle)
         * to grid indices. This will allow us to invoke DatumShiftGrid.interpolateAtCell(x, y, vector)
         * directly in the transform(…) methods.
         */
        final MatrixSIS normalize = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        normalize.setMatrix(grid.getCoordinateToGrid().getMatrix());
        /*
         * NADCON and NTv2 datum shift grids expect geographic coordinates in seconds of angle, while
         * MathTransform instances created by DefaultMathTransformFactory.createParameterized(…) must
         * expect coordinates in standardized units (degrees of angle, metres, seconds, etc.).
         * We concatenate the unit conversion with above "coordinates to grid indices" conversion.
         */
        @SuppressWarnings("unchecked")
        final Unit<T> normalized = Units.isAngular(unit) ? (Unit<T>) Units.DEGREE : unit.getSystemUnit();
        if (!unit.equals(normalized)) {
            Number scale  = 1.0;
            Number offset = 0.0;
            final Number[] coefficients = Units.coefficients(normalized.getConverterTo(unit));
            switch (coefficients != null ? coefficients.length : -1) {
                case 2:  scale  = coefficients[1];       // Fall through
                case 1:  offset = coefficients[0];       // Fall through
                case 0:  break;
                default: throw new IllegalArgumentException(Resources.format(Resources.Keys.NonLinearUnitConversion_2, normalized, unit));
            }
            for (int j=0; j<dimension; j++) {
                normalize.convertBefore(j, scale, offset);
            }
        }
        /*
         * Denormalization is the inverse of all above conversions in the usual case (NADCON and NTv2) where the
         * source coordinate system is the same than the target coordinate system, for example with axis unit in
         * degrees. However we also use this InterpolatedTransform implementation for other operation, like the
         * one created by LocalizationGridBuilder. Those later operations may require a different denormalization
         * matrix.
         */
        Matrix denormalize = null;
        if (grid instanceof DatumShiftGridFile<?,?>) {
            denormalize = ((DatumShiftGridFile<?,?>) grid).gridToTarget();
        }
        if (denormalize == null) {
            denormalize = normalize.inverse();                      // Normal NACDON and NTv2 case.
        }
        context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION).setMatrix(denormalize);
        inverse = createInverse();
        /*
         * Parameters completed last because some DatumShiftGridFile subclasses (e.g. ResidualGrid) needs the
         * (de)normalization matrices.
         */
        if (grid instanceof DatumShiftGridFile<?,?>) {
            ((DatumShiftGridFile<?,?>) grid).setGridParameters(context);
        }
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
     *       coordinates in the unit given by {@link Unit#getSystemUnit()}.</li>
     * </ul>
     *
     * @param  <T>      dimension of the coordinate tuples and the translation unit.
     * @param  factory  the factory to use for creating the transform.
     * @param  grid     the grid of datum shifts from source to target datum.
     *                  The {@link DatumShiftGrid#interpolateInCell DatumShiftGrid.interpolateInCell(…)}
     *                  method shall compute translations from <em>source</em> to <em>target</em> as
     *                  {@linkplain DatumShiftGrid#isCellValueRatio() ratio of offsets divided by cell sizes}.
     * @return the transformation between geodetic coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static <T extends Quantity<T>> MathTransform createGeodeticTransformation(
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
     * @return the dimension of input points.
     */
    @Override
    public final int getSourceDimensions() {
        return dimension;
    }

    /**
     * Returns the number of target dimensions.
     * This fixed to {@link DatumShiftGrid#getTranslationDimensions()}.
     *
     * @return the dimension of output points.
     */
    @Override
    public final int getTargetDimensions() {
        return dimension;
    }

    /**
     * Applies the datum shift on a coordinate tuple and optionally returns the derivative at that location.
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
                 * must behave as if all input coordinate values have been read before
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
     * Transforms an arbitrary amount of coordinate tuples.
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
                 * must behave as if all input coordinate values have been read before
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
     * @return a transform from the target ellipsoid to the source ellipsoid of this transform.
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
        return new Inverse(this);
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
     * Transforms target coordinates to source coordinates. This is done by iteratively finding target coordinates
     * that shift to the input coordinates. The input coordinates is used as the first approximation.
     *
     * @author  Rueben Schulz (UBC)
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @version 1.0
     * @since   0.7
     * @module
     */
    static class Inverse extends AbstractMathTransform.Inverse implements Serializable {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = 4335801994727826360L;

        /**
         * Maximum number of iterations. This is set to a higher value than {@link Formulas#MAXIMUM_ITERATIONS} because
         * the data used in {@link InterpolatedTransform} grid may come from anywhere, in particular localization grids
         * in netCDF files. Deformations in those grids may be much higher than e.g. {@link DatumShiftTransform} grids.
         * We observed that inverse transformations may converge slowly if the grid is far from a linear approximation.
         * It happens when the grids have very high {@link DatumShiftGrid#interpolateInCell(double, double, double[])}
         * values, for example close to 1000 while we usually expect values smaller than 1. Behavior with such grids may
         * be unpredictable, sometime with the {@code abs(xi - ox)} or {@code abs(yi - oy)} errors staying high for a
         * long time before to suddenly fall to zero.
         *
         * @see #tryAgain(int, double, double)
         */
        private static final int MAXIMUM_ITERATIONS = Formulas.MAXIMUM_ITERATIONS * 4;

        /**
         * The enclosing transform.
         */
        private final InterpolatedTransform forward;

        /**
         * Difference allowed in iterative computations, in units of grid cell size.
         */
        private final double tolerance;

        /**
         * Creates an inverse transform.
         */
        Inverse(final InterpolatedTransform forward) {
            this.forward = forward;
            tolerance = forward.grid.getCellPrecision();
            if (!(tolerance > 0)) {                                     // Use ! for catching NaN.
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.ValueNotGreaterThanZero_2, "grid.cellPrecision", tolerance));
            }
        }

        /**
         * Returns the inverse of this math transform.
         */
        @Override
        public MathTransform inverse() {
            return forward;
        }

        /**
         * Transforms a single coordinate tuple in a list of ordinal values,
         * and optionally returns the derivative at that location.
         *
         * @throws TransformException if there is no convergence.
         */
        @Override
        public final Matrix transform(final double[] srcPts, final int srcOff, double[] dstPts, int dstOff,
                                      final boolean derivate) throws TransformException
        {
            final int dimension = forward.dimension;
            if (dstPts == null) {
                dstPts = new double[dimension];
                dstOff = 0;
            }
            double xi, yi;
            final double x = xi = srcPts[srcOff  ];
            final double y = yi = srcPts[srcOff+1];
            final double[] vector = new double[dimension];
            int it = MAXIMUM_ITERATIONS;
            double tol = tolerance;
            do {
                /*
                 * We want (xi, yi) such as the following conditions hold:
                 *
                 *     xi + vector[0] ≈ x      ⟶      xi ≈ x - vector[0]
                 *     yi + vector[1] ≈ y      ⟶      yi ≈ y - vector[1]
                 */
                forward.grid.interpolateInCell(xi, yi, vector);
                final double ox = xi;
                final double oy = yi;
                xi = x - vector[0];
                yi = y - vector[1];
                if (!(Math.abs(xi - ox) > tol ||                // Use '!' for catching NaN.
                      Math.abs(yi - oy) > tol))
                {
                    if (dimension > GRID_DIMENSION) {
                        System.arraycopy(srcPts, srcOff + GRID_DIMENSION,
                                         dstPts, dstOff + GRID_DIMENSION,
                                              dimension - GRID_DIMENSION);
                        /*
                         * We can not use srcPts[srcOff + i] = dstPts[dstOff + i] + offset[i]
                         * because the arrays may overlap. The contract said that this method
                         * must behave as if all input coordinate values have been read before
                         * we write outputs, which is the reason for System.arraycopy(…) call.
                         */
                        int i = dimension;
                        do dstPts[dstOff + --i] -= vector[i];
                        while (i > GRID_DIMENSION);
                    }
                    dstPts[dstOff  ] = xi;      // Shall not be done before above loop.
                    dstPts[dstOff+1] = yi;
                    if (derivate) {
                        return Matrices.inverse(forward.derivative(
                                new DirectPositionView.Double(dstPts, dstOff, dimension)));
                    }
                    return null;
                }
            } while (--it >= 0 || (tol = tryAgain(it, xi, yi)) > 0);
            throw new TransformException(Resources.format(Resources.Keys.NoConvergence));
        }

        /**
         * Transforms an arbitrary amount of coordinate tuples.
         *
         * @throws TransformException if a point can not be transformed.
         */
        @Override
        public final void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts)
                throws TransformException
        {
            final int dimension = forward.dimension;
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
                int it = MAXIMUM_ITERATIONS;
                double tol = tolerance;
                do {
                    forward.grid.interpolateInCell(xi, yi, vector);
                    final double ox = xi;
                    final double oy = yi;
                    xi = x - vector[0];
                    yi = y - vector[1];
                    if (!(Math.abs(xi - ox) > tol ||                // Use '!' for catching NaN.
                          Math.abs(yi - oy) > tol))
                    {
                        if (dimension > GRID_DIMENSION) {
                            System.arraycopy(srcPts, srcOff + GRID_DIMENSION,
                                             dstPts, dstOff + GRID_DIMENSION,
                                                  dimension - GRID_DIMENSION);
                            /*
                             * We can not use srcPts[srcOff + i] = dstPts[dstOff + i] + offset[i]
                             * because the arrays may overlap. The contract said that this method
                             * must behave as if all input coordinate values have been read before
                             * we write outputs, which is the reason for System.arraycopy(…) call.
                             */
                            int i = dimension;
                            do dstPts[dstOff + --i] -= vector[i];
                            while (i > GRID_DIMENSION);
                        }
                        dstPts[dstOff  ] = xi;          // Shall not be done before above loop.
                        dstPts[dstOff+1] = yi;
                        dstOff += inc;
                        srcOff += inc;
                        continue nextPoint;
                    }
                } while (--it >= 0 || (tol = tryAgain(it, xi, yi)) > 0);
                throw new TransformException(Resources.format(Resources.Keys.NoConvergence));
            }
        }

        /**
         * If iteration did not converge, tells whether we should perform another try with a more permissive threshold.
         * We start relaxing threshold only in last resort, and nevertheless aim for an accuracy of 0.5 of cell size in
         * order to keep some consistency with forward transform. We may relax more in case of extrapolations.
         *
         * @param  it  the iteration counter. Should be negative since we exhausted the normal number of iterations.
         * @param  xi  best <var>x</var> estimation so far.
         * @param  yi  best <var>y</var> estimation so far.
         * @return the new tolerance threshold, or {@link Double#NaN} if no more try should be allowed.
         *
         * @see #MAXIMUM_ITERATIONS
         */
        private double tryAgain(final int it, final double xi, final double yi) {
            double tol = Math.scalb(tolerance, -it);
            if (tol >= 0.5) {
                /*
                 * If the point was inside the grid and the grid is well-formed, we assume that iteration should have converged.
                 * But during extrapolations since there is no authoritative results, we consider that a more approximate result
                 * is okay. In particular it does not make sense to require a 1E-7 accuracy (relative to cell size) if we don't
                 * really know what the answer should be.
                 */
                if (forward.grid.isCellInGrid(xi, yi) || tol > 2) {
                    return Double.NaN;                                      // No more iteration - caller will throw an exception.
                }
            }
            return tol;
        }
    }
}
