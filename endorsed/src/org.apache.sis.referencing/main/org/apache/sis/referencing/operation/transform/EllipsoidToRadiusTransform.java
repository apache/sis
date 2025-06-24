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

import java.util.Map;
import java.util.Arrays;
import java.io.Serializable;
import static java.lang.Math.*;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.provider.MapProjection;
import org.apache.sis.referencing.operation.provider.Spherical2Dto3D;
import org.apache.sis.referencing.operation.provider.Spherical3Dto2D;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.DoubleDouble;
import org.apache.sis.util.privy.Numerics;


/**
 * Conversion of spherical coordinates from (λ,Ω) to (λ,Ω,R) with points assumed to be on an ellipsoid surface.
 * For each coordinate tuple, this transform copies the spherical longitude (λ) and the spherical latitude (Ω),
 * then appends a <var>R</var> coordinate which is the distance from the sphere center to the ellipsoid surface
 * at the spherical latitude Ω. Notes:
 *
 * <ul>
 *   <li>For a biaxial ellipsoid, the spherical longitude is the same as the geodetic longitude.
 *       For this reason, the same symbol (λ) is reused.</li>
 *   <li>This transform does <em>not</em> convert the spherical latitude (Ω) to geodetic latitude (φ).
 *       That conversion is done by the inverse of {@link EllipsoidToCentricTransform}.</li>
 * </ul>
 *
 * <h2>Related operations</h2>
 * This transform is also named <q>Spherical 2D to 3D conversion</q> by Apache <abbr>SIS</abbr>.
 * This transform is similar to <q>Geographic 2D to 3D conversion</q> (inverse of EPSG:9659 method),
 * except that this spherical case sets <var>R</var> to a non-zero value that depends on the Ω value.
 * By contrast, the geographic case unconditionally sets <var>h</var> to zero.
 *
 * <p>This transform is also related to the {@link DefaultEllipsoid#getGeocentricRadius(double)} method,
 * except that the latter expects <em>geodetic</em> latitudes
 * while this transform expects <em>geocentric</em> latitudes.</p>
 *
 * <h2>Purpose</h2>
 * This transform is used together with the inverse of {@link EllipsoidToCentricTransform} when the
 * source coordinate system is spherical ({@link EllipsoidToCentricTransform.TargetType#SPHERICAL}).
 * The conversion from spherical latitude (Ω) to geodetic latitude (φ) depends on <var>R</var>.
 * When the latter is not specified, it is often assumed to be on the ellipsoid surface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see DefaultEllipsoid#getGeocentricRadius(double)
 *
 * @since 1.5
 */
public class EllipsoidToRadiusTransform extends AbstractMathTransform implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3724386806353023273L;

    /**
     * Number of dimensions of source coordinate tuples.
     */
    private static final int SRC_DIM = 2;

    /**
     * Number of dimensions of target coordinate tuples.
     */
    private static final int TGT_DIM = 3;

    /**
     * Internal parameter descriptor, used only for debugging purpose.
     * Created when first needed.
     *
     * @see #getParameterDescriptors()
     */
    @Debug
    private static ParameterDescriptorGroup DESCRIPTOR;

    /**
     * The parameters used for creating this conversion.
     * They are used for formatting <i>Well Known Text</i> (<abbr>WKT</abbr>) and error messages.
     *
     * @see #getContextualParameters()
     */
    final ContextualParameters context;

    /**
     * The square of the eccentricity.
     * This is defined as ℯ² = (a²-b²)/a² where
     * <var>a</var> is the <dfn>semi-major</dfn> axis length and
     * <var>b</var> is the <dfn>semi-minor</dfn> axis length.
     */
    protected final double eccentricitySquared;

    /**
     * The inverse of this transform.
     */
    private final Inverse inverse;

    /**
     * Creates a transform for an ellipsoid having a semi-major axis length of 1.
     * {@code EllipsoidToRadiusTransform} instances expect input coordinates as below:
     *
     * <ol>
     *   <li>Spherical longitudes in any unit (they will pass-through),</li>
     *   <li>Spherical latitudes in <strong>radians</strong>.</li>
     * </ol>
     *
     * Output coordinates are the same as the input coordinates,
     * with the addition of the distance from sphere center to ellipsoid surface.
     *
     * <h4>Unit conversions</h4>
     * For a complete chain of transforms, {@code EllipsoidToRadiusTransform}
     * instances need to be concatenated with the following affine transforms:
     *
     * <ul>
     *   <li><i>Normalization</i> before {@code EllipsoidToRadiusTransform}:<ul>
     *     <li>Conversion of spherical latitudes from degrees to radians</li>
     *   </ul></li>
     *   <li><i>Denormalization</i> after {@code EllipsoidToRadiusTransform}:<ul>
     *     <li>Multiplication of <var>R</var> by the semi-minor axis length</li>
     *   </ul></li>
     * </ul>
     *
     * After {@code EllipsoidToRadiusTransform} construction,
     * the full conversion chain including the above affine transforms can be created by
     * <code>{@linkplain #getContextualParameters()}.{@linkplain ContextualParameters#completeTransform
     * completeTransform}(factory, this)}</code>.
     *
     * @param surface  the ellipsoid of the spherical coordinates for which to add a radius.
     *
     * @see #createGeodeticConversion(MathTransformFactory, Ellipsoid)
     */
    public EllipsoidToRadiusTransform(final Ellipsoid surface) {
        ArgumentChecks.ensureNonNull("surface", surface);
        final DefaultEllipsoid ellipsoid = DefaultEllipsoid.castOrCopy(surface);
        eccentricitySquared = ellipsoid.getEccentricitySquared();
        /*
         * Copy parameters to the ContextualParameter. Those parameters are not used directly by
         * EllipsoidToRadiusTransform, but we need to store them in case the user asks for them.
         */
        final Unit<Length> unit = ellipsoid.getAxisUnit();
        context = new ContextualParameters(Spherical2Dto3D.PARAMETERS, SRC_DIM, TGT_DIM);
        context.getOrCreate(MapProjection.SEMI_MAJOR).setValue(ellipsoid.getSemiMajorAxis(), unit);
        context.getOrCreate(MapProjection.SEMI_MINOR).setValue(ellipsoid.getSemiMinorAxis(), unit);
        /*
         * Prepare two affine transforms to be executed before and after this EllipsoidToRadiusTransform:
         *
         *   - A "normalization" transform for converting degrees to radians.
         *   - A "denormalization" transform for converting back to degrees
         *     and for applying the last multiplication factor on the radius.
         */
        final MatrixSIS normalize   = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        normalize  .convertBefore(1, DoubleDouble.DEGREES_TO_RADIANS, null);
        denormalize.convertAfter (1, DoubleDouble.RADIANS_TO_DEGREES, null);
        denormalize.convertAfter (2, DoubleDouble.of(ellipsoid.getSemiMinorAxis(), true), null);
        inverse = new Inverse();
    }

    /**
     * Creates a transform for the given ellipsoid. This factory method combines the
     * {@code EllipsoidToRadiusTransform} instance with the steps needed for converting
     * degrees to radians and expressing the results in units of the given ellipsoid.
     *
     * @param  factory  the factory to use for creating and concatenating the affine transforms.
     * @param  surface  the ellipsoid of the spherical coordinates for which to add a radius.
     * @return the conversion from two-dimensional to three-dimensional spherical coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static MathTransform createGeodeticConversion(final MathTransformFactory factory, final Ellipsoid surface)
            throws FactoryException
    {
        if (Formulas.isEllipsoidal(surface)) try {
            final var kernel = new EllipsoidToRadiusTransform(surface);
            final MathTransform complete = kernel.context.completeTransform(factory, kernel);
            /*
             * The "ellipsoid to radius" transform is non-linear. However, the inverse is a linear transform
             * because it does nothing else than dropping the R dimension. Problem: that inverse transform is
             * implemented by a `LinearTransform`, and the inverse of a linear transform is by design another
             * linear transform. Therefore, `inverse.inverse` cannot return `this`. For making possible to get
             * back the original transform, we need to wrap the inverse in an `OnewayLinearTransform` instance;
             */
            final MathTransform inverse = complete.inverse();
            if (inverse instanceof LinearTransform) {
                ConcatenatedTransform.setInverse(complete,
                        new OnewayLinearTransform.Concatenated((LinearTransform) inverse, kernel.inverse, complete));
            }
            return complete;
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);      // Should never happen.
        }
        /*
         * Spherical case: the radius value is a constant. In this case,
         * there is no need for above "one way linear transform" trick.
         */
        final Matrix m = Matrices.createDiagonal(TGT_DIM+1, SRC_DIM+1);
        m.setElement(TGT_DIM,   SRC_DIM, 1);
        m.setElement(TGT_DIM-1, SRC_DIM, surface.getSemiMajorAxis());
        return new ProjectiveTransform(m);
    }

    /**
     * Returns the parameters used for creating the complete conversion. Those parameters describe a sequence of
     * <i>normalize</i> → {@code this} → <i>denormalize</i> transforms, <strong>not</strong> including axis swapping.
     *
     * @return the parameter values for the <i>normalize</i> → {@code this} → <i>denormalize</i> chain of transforms.
     */
    @Override
    protected ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Returns a copy of internal parameter values of this {@code EllipsoidToRadiusTransform} transform.
     * This method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most <abbr>GIS</abbr> applications will instead be interested in the
     * {@linkplain #getContextualParameters() contextual parameters}.
     *
     * @return a copy of the internal parameter values for this transform.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        final Parameters pg = Parameters.castOrWrap(getParameterDescriptors().createValue());
        pg.getOrCreate(MapProjection.ECCENTRICITY).setValue(sqrt(eccentricitySquared));
        return pg;
    }

    /**
     * Returns a description of the internal parameters of this {@code EllipsoidToRadiusTransform} transform.
     *
     * @return a description of the internal parameters.
     */
    @Debug
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        synchronized (EllipsoidToRadiusTransform.class) {
            if (DESCRIPTOR == null) {
                final ParameterBuilder builder = new ParameterBuilder().setCodeSpace(Citations.SIS, Constants.SIS);
                DESCRIPTOR = builder.addName("Spherical2D to 3D (radians domain)")
                        .createGroup(1, 1, MapProjection.ECCENTRICITY);
            }
            return DESCRIPTOR;
        }
    }

    /**
     * Gets the dimension of input points, which is 2.
     *
     * @return always 2.
     */
    @Override
    public final int getSourceDimensions() {
        return SRC_DIM;
    }

    /**
     * Gets the dimension of output points, which is 3.
     *
     * @return always 3.
     */
    @Override
    public final int getTargetDimensions() {
        return TGT_DIM;
    }

    /**
     * Converts the (λ,Ω) spherical coordinates to (λ,Ω,<var>R</var>),
     * and optionally returns the derivative at that location.
     *
     * @return {@inheritDoc}
     * @throws TransformException if the point cannot be transformed or
     *         if a problem occurred while calculating the derivative.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        final double Ω = srcPts[srcOff+1];
        final double cosΩ = cos(Ω);
        final double r2 = 1 - eccentricitySquared*(cosΩ*cosΩ);
        final double r  = sqrt(r2);     // Inverse of radius.
        if (dstPts != null) {
            dstPts[dstOff  ] = srcPts[srcOff];
            dstPts[dstOff+1] = Ω;
            dstPts[dstOff+2] = 1 / r;
        }
        if (!derivate) {
            return null;
        }
        final MatrixSIS derivative = Matrices.createDiagonal(TGT_DIM, SRC_DIM);
        derivative.setElement(2, 1, eccentricitySquared*cosΩ*sin(Ω) / (r*r2));
        return derivative;
    }

    /**
     * Converts the (λ,Ω) spherical coordinates to (λ,Ω,<var>R</var>) in a sequence of coordinate tuples.
     * This method performs the same conversion as {@link #transform(double[], int, double[], int, boolean)},
     * but the formulas are repeated here for performance reasons.
     *
     * @throws TransformException if a point cannot be transformed.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        int srcInc = SRC_DIM;
        int dstInc = TGT_DIM;
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, SRC_DIM, dstOff, TGT_DIM, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * SRC_DIM;
                    dstOff += (numPts - 1) * TGT_DIM;
                    srcInc = -SRC_DIM;
                    dstInc = -TGT_DIM;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*SRC_DIM);
                    srcOff = 0;
                    break;
                }
            }
        }
        while (--numPts >= 0) {
            final double Ω = srcPts[srcOff+1];  // Must be saved before to write in the array.
            final double cosΩ = cos(Ω);
            dstPts[dstOff  ] = srcPts[srcOff];
            dstPts[dstOff+1] = Ω;
            dstPts[dstOff+2] = 1 / sqrt(1 - eccentricitySquared*(cosΩ*cosΩ));
            srcOff += srcInc;
            dstOff += dstInc;
        }
    }

    /**
     * Returns the inverse of this transform.
     * The inverse transform drops the radius.
     *
     * @return the conversion from 3D to 2D spherical coordinates.
     */
    @Override
    public MathTransform inverse() {
        return inverse;
    }

    /**
     * The inverse transform doing the actual work.
     *
     * @todo Move inside {@link Inverse} after migration to JDK17 (which allows static fields in inner classes).
     */
    private static final CopyTransform DELEGATE = new CopyTransform(TGT_DIM, new int[] {0, 1});

    /**
     * The descriptor of the inverse transform.
     *
     * @todo Move inside {@link Inverse} after migration to JDK17 (which allows static fields in inner classes).
     */
    @Debug
    private static ParameterDescriptorGroup INVERSE_DESCRIPTOR;

    /**
     * Drops the radius when doing the inverse operation. This operation is almost linear.
     * The "almost" is because {@link #inverse()} returns a non-linear transform.
     * For that reason, this class cannot implement {@link LinearTransform}.
     */
    private final class Inverse extends OnewayLinearTransform implements Parameterized {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = -5804154207223293418L;

        /**
         * Creates the inverse of the enclosing transform.
         */
        Inverse() {
            super(DELEGATE);
        }

        /**
         * Returns the inverse of this math transform.
         */
        @Override
        public MathTransform inverse() {
            return EllipsoidToRadiusTransform.this;
        }

        /**
         * Returns the same contextual parameters as in the enclosing class,
         * but with a different method name and the (de)normalization matrices inverted.
         */
        @Override
        protected ContextualParameters getContextualParameters() {
            return context.inverse(Spherical3Dto2D.PARAMETERS, null);
            // Caching is done by `context`. No need to cache in this class.
        }

        /**
         * Returns a description of the internal parameters of this inverse transform.
         */
        @Debug
        @Override
        public ParameterDescriptorGroup getParameterDescriptors() {
            synchronized (EllipsoidToRadiusTransform.class) {
                if (INVERSE_DESCRIPTOR == null) {
                    INVERSE_DESCRIPTOR = ReferencingUtilities.rename(
                            EllipsoidToRadiusTransform.this.getParameterDescriptors(),
                            "Spherical3D to 2D (radians domain)");
                }
                return INVERSE_DESCRIPTOR;
            }
        }

        /**
         * Returns the internal parameter values.
         * This is used only for debugging purpose.
         */
        @Debug
        @Override
        public ParameterValueGroup getParameterValues() {
            final ParameterValueGroup pg = getParameterDescriptors().createValue();
            pg.values().addAll(EllipsoidToRadiusTransform.this.getParameterValues().values());
            return pg;
        }
    }

    /**
     * If the transform after this transform is dropping the third coordinate, removes this transform.
     * Otherwise, allows {@link TransformJoiner} to move the operations applied on the longitude value
     * if this move can simplify the chain of transforms.
     *
     * @param  context  information about the neighbor transforms, and the object where to set the result.
     * @throws FactoryException if an error occurred while combining the transforms.
     */
    @Override
    protected void tryConcatenate(final TransformJoiner context) throws FactoryException {
        if (!context.removeUnusedDimensions(1, SRC_DIM, SRC_DIM + 1, MathTransforms::identity)) {
            // Try to move operation on the longitude value.
            if (!context.replacePassThrough(Map.of(0, 0))) {
                super.tryConcatenate(context);
            }
        }
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() + Double.hashCode(eccentricitySquared);
    }

    /**
     * Compares the specified object with this math transform for equality.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;        // Slight optimization
        }
        if (super.equals(object, mode)) {
            final var that = (EllipsoidToRadiusTransform) object;
            return Numerics.equals(eccentricitySquared, that.eccentricitySquared);
        }
        return false;
    }
}
