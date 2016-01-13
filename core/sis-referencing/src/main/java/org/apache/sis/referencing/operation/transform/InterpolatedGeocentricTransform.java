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

import javax.measure.unit.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.DatumShiftGridFile;
import org.apache.sis.internal.referencing.provider.FranceGeocentricInterpolation;
import org.apache.sis.internal.referencing.provider.Molodensky;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.util.ArgumentChecks;


/**
 * Transforms between two geographic CRS by performing geocentric translations interpolated from a grid file.
 * This transform is used mainly for <cite>"France geocentric interpolation"</cite> (ESPG:9655) datum shifts,
 * but Apache SIS implementation allows the use for other regions.
 *
 * <div class="section">Algorithm</div>
 * This class transforms two- or three- dimensional coordinates from a geographic CRS to another geographic CRS.
 * The changes between source and target coordinates are small (usually less than 400 metres), but vary for every
 * position. Those changes are provided in a {@linkplain DatumShiftGrid datum shift grid}, usually loaded from one
 * or two files.
 *
 * <p>Many datum shift grids like NADCON and NTv2 apply the changes directly on geographic coordinates.
 * This relatively simple case is handled by {@link InterpolatedTransform}.
 * But the {@code InterpolatedGeocentricTransform} algorithm uses the grid in a more complex way:</p>
 *
 * <ol>
 *   <li>Convert input geographic coordinate (λ,φ) to geocentric coordinate (X,Y,Z).</li>
 *   <li>Ask {@link DatumShiftGrid} for the offset to apply for coordinate (λ,φ).
 *       But instead of returning a (Δλ, Δφ) offset, the grid shall return a (ΔX, ΔY, ΔZ) offset.</li>
 *   <li>Convert the shifted geocentric coordinate (X+ΔX, Y+ΔY, Z+ΔZ) back to a geographic coordinate.</li>
 * </ol>
 *
 * <div class="note"><b>Source:</b> IGN document {@code NTG_88.pdf},
 * <cite>"Grille de paramètres de transformation de coordonnées"</cite>
 * at <a href="http://www.ign.fr">http://www.ign.fr</a>.
 * Note however that the signs of (ΔX, ΔY, ΔZ) values expected by this class are the opposite of the
 * signs used in NTG_88 document. This is because NTG_88 grid defines shifts from target to source,
 * while this class expects shifts from source to target.
 *
 * <p><b>Note:</b> this algorithm is not the same as a (theoretical) {@link EllipsoidToCentricTransform} →
 * {@link InterpolatedTransform} → (inverse of {@code EllipsoidToCentricTransform}) concatenation
 * because the {@code DatumShiftGrid} inputs are geographic coordinates even if the interpolated
 * grid values are in geocentric space.</p></div>
 *
 * <div class="section">Performance consideration</div>
 * {@link InterpolatedMolodenskyTransform} performs the same calculation more efficiently at the cost of
 * a few centimetres error. Both classes are instantiated in the same way and expect the same inputs.
 *
 * @author  Simon Reynard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see InterpolatedMolodenskyTransform
 */
public class InterpolatedGeocentricTransform extends DatumShiftTransform {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5503722845441653093L;

    /**
     * Parameter descriptor to use with the contextual parameters for the forward transformation.
     * We do not use the <cite>"France geocentric interpolation"</cite> (ESPG:9655) descriptor
     * because their "forward" transformation is our "inverse" transformation, and conversely.
     * The {@code DESCRIPTOR} defined here is non-standard, but allows this class to be used
     * for other geographic areas than France.
     */
    static final ParameterDescriptorGroup DESCRIPTOR;

    /**
     * Parameter descriptor to use with the contextual parameters for the inverse transformation.
     * We do not use the <cite>"France geocentric interpolation"</cite> (ESPG:9655) descriptor
     * because it is specific to a single country, has hard-coded parameters and uses a sign
     * convention for (ΔX,ΔY,ΔZ) translations different than the one used in this class.
     * The {@code INVERSE} descriptor defined here is non-standard, but allows this class
     * to be used for other geographic areas than France.
     */
    private static final ParameterDescriptorGroup INVERSE;
    static {
        final ParameterBuilder builder = new ParameterBuilder()
                .setRequired(true).setCodeSpace(Citations.SIS, Constants.SIS);
        final ParameterDescriptor<?>[] param = new ParameterDescriptor<?>[] {
                Molodensky.DIMENSION,
                Molodensky.SRC_SEMI_MAJOR,
                Molodensky.SRC_SEMI_MINOR,
                Molodensky.TGT_SEMI_MAJOR,
                Molodensky.TGT_SEMI_MINOR,
                FranceGeocentricInterpolation.FILE
        };
        DESCRIPTOR = builder.addName("Geocentric interpolation").createGroup(param);
        INVERSE = builder.addName("Geocentric inverse interpolation").createGroup(param);
    }

    /**
     * Semi-major axis length (<var>a</var>) of the source ellipsoid.
     */
    final double semiMajor;

    /**
     * Semi-major axis length of the source ellipsoid divided by semi-major axis length of the target ellipsoid.
     * Used for converting normalized coordinates between the two geocentric coordinate reference systems.
     *
     * <p>This is a dimensionless quantity: the ellipsoid axis lengths must have been converted to the same unit
     * before to compute this ratio.</p>
     */
    final double scale;

    /**
     * The transform to apply before and after the geocentric translation. Shall be instance of
     * {@link EllipsoidToCentricTransform} and {@code EllipsoidToCentricTransform.Inverse} respectively.
     */
    final AbstractMathTransform ellipsoidToCentric, centricToEllipsoid;

    /**
     * The inverse of this interpolated geocentric transform.
     *
     * @see #inverse()
     */
    private final InterpolatedGeocentricTransform inverse;

    /**
     * Constructs the inverse of an interpolated geocentric transform.
     *
     * @param inverse The transform for which to create the inverse.
     * @param source  The source ellipsoid of the given {@code inverse} transform.
     * @param target  The target ellipsoid of the given {@code inverse} transform.
     */
    InterpolatedGeocentricTransform(final InterpolatedGeocentricTransform inverse, Ellipsoid source, Ellipsoid target) {
        this(target, inverse.getTargetDimensions() > 2,
             source, inverse.getSourceDimensions() > 2,
             inverse.grid, inverse);
    }

    /**
     * Creates a transform from the specified parameters.
     * This {@code InterpolatedGeocentricTransform} class expects ordinate values in the following order and units:
     * <ol>
     *   <li>longitudes in <strong>radians</strong> relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in <strong>radians</strong>,</li>
     *   <li>optionally heights above the ellipsoid, in same units than the source ellipsoid axes.</li>
     * </ol>
     *
     * For converting geographic coordinates in degrees, {@code InterpolatedGeocentricTransform} instances
     * need to be concatenated with the following affine transforms:
     *
     * <ul>
     *   <li><cite>Normalization</cite> before {@code InterpolatedGeocentricTransform}:<ul>
     *     <li>Conversion of (λ,φ) from degrees to radians.</li>
     *   </ul></li>
     *   <li><cite>Denormalization</cite> after {@code InterpolatedGeocentricTransform}:<ul>
     *     <li>Conversion of (λ,φ) from radians to degrees.</li>
     *   </ul></li>
     * </ul>
     *
     * After {@code InterpolatedGeocentricTransform} construction,
     * the full conversion chain including the above affine transforms can be created by
     * <code>{@linkplain #getContextualParameters()}.{@linkplain ContextualParameters#completeTransform
     * completeTransform}(factory, this)}</code>.
     *
     * @param source      The source ellipsoid.
     * @param isSource3D  {@code true} if the source coordinates have a height.
     * @param target      The target ellipsoid.
     * @param isTarget3D  {@code true} if the target coordinates have a height.
     * @param grid        The grid of datum shifts from source to target datum.
     *                    The {@link DatumShiftGrid#interpolateInCell DatumShiftGrid.interpolateInCell(…)} method
     *                    shall compute (ΔX, ΔY, ΔZ) translations from <em>source</em> to <em>target</em> in the
     *                    unit of source ellipsoid axes.
     *
     * @see #createGeodeticTransformation(MathTransformFactory, Ellipsoid, boolean, Ellipsoid, boolean, DatumShiftGrid)
     */
    protected InterpolatedGeocentricTransform(final Ellipsoid source, final boolean isSource3D,
                                              final Ellipsoid target, final boolean isTarget3D,
                                              final DatumShiftGrid<Angle,Length> grid)
    {
        this(source, isSource3D, target, isTarget3D, grid, null);
    }

    /**
     * Constructor for the forward and inverse transformations.
     * If {@code inverse} is {@code null}, then this constructor creates the forward transformation.
     * Otherwise this constructor creates the inverse of the given {@code inverse} transformation.
     */
    private InterpolatedGeocentricTransform(final Ellipsoid source, final boolean isSource3D,
                                            final Ellipsoid target, final boolean isTarget3D,
                                            final DatumShiftGrid<?,?> grid,
                                            InterpolatedGeocentricTransform inverse)
    {
        super((inverse != null) ? INVERSE : DESCRIPTOR, isSource3D, isTarget3D, grid);
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        ArgumentChecks.ensureNonNull("grid",   grid);
        final Unit<Length> unit = source.getAxisUnit();
        ensureGeocentricTranslation(grid, unit);
        /*
         * Store the source and target axis lengths in the contextual parameters.
         * We do not need to store all those information directly in the field of this class,
         * but we need to have them in ContextualParameters for Well Known Text formatting.
         */
        final double semiMinor;
        semiMajor = source.getSemiMajorAxis();
        semiMinor = source.getSemiMinorAxis();
        setContextParameters(semiMajor, semiMinor, unit, target);
        context.getOrCreate(Molodensky.DIMENSION).setValue(isSource3D ? 3 : 2);
        if (grid instanceof DatumShiftGridFile<?,?>) {
            ((DatumShiftGridFile<?,?>) grid).setFileParameters(context);
        }
        /*
         * The above setContextParameters(…) method converted the axis lengths of target ellipsoid in the same units
         * than source ellipsoid. Opportunistically fetch that value, so we don't have to convert the values ourselves.
         */
        scale = semiMajor / context.doubleValue(Molodensky.TGT_SEMI_MAJOR);
        /*
         * Creates the Geographic ↔ Geocentric conversions. Note that the target ellipsoid keeps the unit specified
         * by the user; we do not convert target axis length. This is okay since our work will be already finished
         * when the conversion to target units will apply.
         */
        if (inverse == null) {
            ellipsoidToCentric = new EllipsoidToCentricTransform(semiMajor, semiMinor, unit, isSource3D,
                    EllipsoidToCentricTransform.TargetType.CARTESIAN);

            centricToEllipsoid = (AbstractMathTransform) new EllipsoidToCentricTransform(
                    target.getSemiMajorAxis(),
                    target.getSemiMinorAxis(),
                    target.getAxisUnit(), isTarget3D,
                    EllipsoidToCentricTransform.TargetType.CARTESIAN).inverse();
        } else try {
            ellipsoidToCentric = (AbstractMathTransform) inverse.centricToEllipsoid.inverse();
            centricToEllipsoid = (AbstractMathTransform) inverse.ellipsoidToCentric.inverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException(e);      // Should never happen.
        }
        /*
         * Usually, this is where we would initialize the normalization and denormalization matrices
         * to degrees ↔ radians conversions. But in for this class we will rather copy the work done
         * by EllipsoidToCentricTransform. Especially since the later performs its own adjustment on
         * height values.
         */
        context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION).setMatrix(
                ellipsoidToCentric.getContextualParameters().getMatrix(ContextualParameters.MatrixRole.NORMALIZATION));
        context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION).setMatrix(
                centricToEllipsoid.getContextualParameters().getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION));
        /*
         * Inverse transformation must be created only after everything else has been initialized.
         */
        if (inverse == null) {
            if (isSource3D || isTarget3D) {
                inverse = new Inverse(this, source, target);
            } else {
                inverse = new InterpolatedGeocentricTransform2D.Inverse(this, source, target);
            }
        }
        this.inverse = inverse;
    }

    /**
     * Delegates to {@link ContextualParameters#completeTransform(MathTransformFactory, MathTransform)}
     * for this transformation and for its dependencies as well.
     */
    private MathTransform completeTransform(final MathTransformFactory factory, final boolean create) throws FactoryException {
        ellipsoidToCentric.getContextualParameters().completeTransform(factory, null);
        centricToEllipsoid.getContextualParameters().completeTransform(factory, null);
        return context.completeTransform(factory, create ? this : null);
    }

    /**
     * Creates a transformation between two geographic CRS. This factory method combines the
     * {@code InterpolatedGeocentricTransform} instance with the steps needed for converting values between
     * degrees to radians. The transform works with input and output coordinates in the following units:
     *
     * <ol>
     *   <li>longitudes in <strong>degrees</strong> relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in <strong>degrees</strong>,</li>
     *   <li>optionally heights above the ellipsoid, in same units than the source ellipsoids axes.</li>
     * </ol>
     *
     * Note however that the given {@code grid} instance shall expect geographic coordinates (λ,φ)
     * in <strong>radians</strong>.
     *
     * @param factory     The factory to use for creating the transform.
     * @param source      The source ellipsoid.
     * @param isSource3D  {@code true} if the source coordinates have a height.
     * @param target      The target ellipsoid.
     * @param isTarget3D  {@code true} if the target coordinates have a height.
     * @param grid        The grid of datum shifts from source to target datum.
     *                    The {@link DatumShiftGrid#interpolateInCell DatumShiftGrid.interpolateInCell(…)} method
     *                    shall compute (ΔX, ΔY, ΔZ) translations from <em>source</em> to <em>target</em> in the
     *                    unit of source ellipsoid axes.
     * @return The transformation between geographic coordinates in degrees.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static MathTransform createGeodeticTransformation(final MathTransformFactory factory,
            final Ellipsoid source, final boolean isSource3D,
            final Ellipsoid target, final boolean isTarget3D,
            final DatumShiftGrid<Angle,Length> grid) throws FactoryException
    {
        final InterpolatedGeocentricTransform tr;
        if (isSource3D || isTarget3D) {
            tr = new InterpolatedGeocentricTransform(source, isSource3D, target, isTarget3D, grid);
        } else {
            tr = new InterpolatedGeocentricTransform2D(source, target, grid);
        }
        tr.inverse.completeTransform(factory, false);
        return tr.completeTransform(factory, true);
    }

    /**
     * Gets the dimension of input points.
     *
     * @return The input dimension, which is 2 or 3.
     */
    @Override
    public int getSourceDimensions() {
        return ellipsoidToCentric.getSourceDimensions();
    }

    /**
     * Gets the dimension of output points.
     *
     * @return The output dimension, which is 2 or 3.
     */
    @Override
    public int getTargetDimensions() {
        return centricToEllipsoid.getTargetDimensions();
    }

    /**
     * Transforms the (λ,φ) or (λ,φ,<var>h</var>) coordinates between two geographic CRS,
     * and optionally returns the derivative at that location.
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
        /*
         * Interpolate the geocentric translation (ΔX,ΔY,ΔZ) for the source coordinate (λ,φ).
         * The translation that we got is in metres, which we convert into normalized units.
         */
        final double[] vector = new double[3];
        grid.interpolateInCell(grid.normalizedToGridX(srcPts[srcOff]),              // In radians
                               grid.normalizedToGridY(srcPts[srcOff+1]), vector);
        final double tX = vector[0] / semiMajor;
        final double tY = vector[1] / semiMajor;
        final double tZ = vector[2] / semiMajor;
        /*
         * Geographic → Geocentric conversion. Result stored in a temporary array
         * because we do not check if the target points are two or three dimensional.
         */
        final Matrix m1 = ellipsoidToCentric.transform(srcPts, srcOff, vector, 0, derivate);
        /*
         * Apply the geocentric translation on the geocentric coordinates. If the coordinates were in metres,
         * we would have only additions. But since we use normalized units, we also need to convert from the
         * normalization relative to the source ellipsoid to normalization relative to the target ellipsoid.
         * This is the purpose of the scale factor.
         */
        vector[0] = (vector[0] + tX) * scale;
        vector[1] = (vector[1] + tY) * scale;
        vector[2] = (vector[2] + tZ) * scale;
        /*
         * Final Geocentric → Geographic conversion, and compute the derivative matrix if the user asked for it.
         */
        final Matrix m2 = centricToEllipsoid.transform(vector, 0, dstPts, dstOff, derivate);
        if (m1 == null || m2 == null) {
            return null;
        }
        return concatenate(m1, m2);
    }

    /**
     * Computes the derivative by concatenating the "geographic to geocentric" and "geocentric to geographic" matrix,
     * with the {@linkplain #scale} factor between them.
     *
     * <div class="note"><b>Note:</b>
     * we could improve a little bit the precision by computing the derivative in the interpolation grid:
     *
     * {@preformat java
     *     grid.derivativeInCell(grid.normalizedToGridX(λ), grid.normalizedToGridY(φ));
     * }
     *
     * But this is a little bit complicated (need to convert to normalized units and divide by the grid
     * cell size) for a very small difference. For now we neglect that part.</div>
     *
     * @param m1 The derivative computed by the "geographic to geocentric" conversion.
     * @param m2 The derivative computed by the "geocentric to geographic" conversion.
     * @return The derivative for the "interpolated geocentric" transformation.
     */
    final Matrix concatenate(final Matrix m1, final Matrix m2) {
        for (int i = m1.getNumCol(); --i >= 0;) {   // Number of columns can be 2 or 3.
            for (int j = 3; --j >= 0;) {            // Number of rows can not be anything else than 3.
                m1.setElement(j, i, m1.getElement(j, i) * scale);
            }
        }
        return Matrices.multiply(m2, m1);
    }

    /**
     * Returns the inverse of this interpolated geocentric transform.
     * The source ellipsoid of the returned transform will be the target ellipsoid of this transform, and conversely.
     *
     * @return A transform from the target ellipsoid to the source ellipsoid of this transform.
     */
    @Override
    public MathTransform inverse() {
        return inverse;
    }





    /**
     * The inverse of the enclosing {@link InterpolatedGeocentricTransform}.
     * This transform applies an algorithm similar to the one documented in the enclosing class,
     * with the following differences:
     *
     * <ol>
     *   <li>First, target coordinates are estimated using the ({@link #tX}, {@link #tY}, {@link #tZ}) translation.</li>
     *   <li>A new ({@code ΔX}, {@code ΔY}, {@code ΔZ}) translation is interpolated at the geographic coordinates found
     *       in above step, and target coordinates are recomputed again using that new translation.</li>
     * </ol>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    static class Inverse extends InterpolatedGeocentricTransform {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = -3481207454803064521L;

        /**
         * Initial translation values to use before to improve the accuracy with the interpolation.
         */
        private final double tX, tY, tZ;

        /**
         * Constructs the inverse of an interpolated geocentric transform.
         *
         * @param inverse The transform for which to create the inverse.
         * @param source  The source ellipsoid of the given {@code inverse} transform.
         * @param target  The target ellipsoid of the given {@code inverse} transform.
         */
        Inverse(final InterpolatedGeocentricTransform inverse, final Ellipsoid source, final Ellipsoid target) {
           super(inverse, source, target);
           tX = grid.getCellMean(0) / semiMajor;
           tY = grid.getCellMean(1) / semiMajor;
           tZ = grid.getCellMean(2) / semiMajor;
        }

        /**
         * Transforms the (λ,φ) or (λ,φ,<var>h</var>) coordinates between two geographic CRS,
         * and optionally returns the derivative at that location.
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
            /*
             * Geographic → Geocentric conversion. Result stored in a temporary array
             * because we do not check if the target points are two or three dimensional.
             */
            final double[] vector = new double[3];
            final Matrix m1 = ellipsoidToCentric.transform(srcPts, srcOff, vector, 0, derivate);
            final double x = vector[0];
            final double y = vector[1];
            final double z = vector[2];
            /*
             * First approximation of geocentric translation, by using average values or values
             * specified by the authority (e.g. as in the "France Geocentric Interpolation" method).
             */
            vector[0] = x - tX;
            vector[1] = y - tY;
            vector[2] = z - tZ;
            centricToEllipsoid.transform(vector, 0, vector, 0, derivate);
            /*
             * We got a (λ,φ) using an approximative geocentric translation. Now interpolate the "real"
             * geocentric interpolation at that location and get the (λ,φ) again. In theory, we just
             * iterate until we got the desired precision. But in practice a single interation is enough.
             */
            grid.interpolateInCell(grid.normalizedToGridX(vector[0]),
                                   grid.normalizedToGridY(vector[1]), vector);
            vector[0] = (x - vector[0] / semiMajor) * scale;
            vector[1] = (y - vector[1] / semiMajor) * scale;
            vector[2] = (z - vector[2] / semiMajor) * scale;
            /*
             * Final Geocentric → Geographic conversion, and compute the derivative matrix if the user asked for it.
             * We neglect the derivative provided by grid.derivativeInCell(…).
             */
            final Matrix m2 = centricToEllipsoid.transform(vector, 0, dstPts, dstOff, derivate);
            if (m1 == null || m2 == null) {
                return null;
            }
            return concatenate(m1, m2);
        }
    }
}
