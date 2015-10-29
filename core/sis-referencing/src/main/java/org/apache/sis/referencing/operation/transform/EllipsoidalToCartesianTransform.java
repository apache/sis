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
import java.io.Serializable;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.provider.GeographicToGeocentric;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.*;
import static org.apache.sis.internal.referencing.provider.MapProjection.SEMI_MAJOR;
import static org.apache.sis.internal.referencing.provider.MapProjection.SEMI_MINOR;
import static org.apache.sis.internal.referencing.provider.MapProjection.EXCENTRICITY;
import static org.apache.sis.internal.referencing.provider.AbridgedMolodensky.DIMENSION;


/**
 * Transform from two- or three- dimensional ellipsoidal coordinates to Cartesian coordinates.
 * This transform is usually part of a conversion from
 * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic} to
 * {@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS geocentric} coordinates.
 *
 * <p>Input coordinates are expected to contain:</p>
 * <ol>
 *   <li>longitudes in <strong>radians</strong> relative to the prime meridian (usually Greenwich),</li>
 *   <li>latitudes in <strong>radians</strong>,</li>
 *   <li>optionally heights above the ellipsoid, in units of an ellipsoid having a semi-major axis length of 1.</li>
 * </ol>
 *
 * Output coordinates are as below, in units of an ellipsoid having a semi-major axis length of 1:
 * <ol>
 *   <li>distance from Earth center on the X axis (toward the intersection of prime meridian and equator),</li>
 *   <li>distance from Earth center on the Y axis (toward the intersection of 90°E meridian and equator),</li>
 *   <li>distance from Earth center on the Z axis (toward North pole).</li>
 * </ol>
 *
 * <div class="section">Geographic to geocentric conversions</div>
 * For converting geographic coordinates to geocentric coordinates, {@code EllipsoidalToCartesianTransform} instances
 * need to be concatenated with the following affine transforms:
 *
 * <ul>
 *   <li><cite>Normalization</cite> before {@code EllipsoidalToCartesianTransform}:<ul>
 *     <li>Conversion of (λ,φ) from degrees to radians</li>
 *     <li>Division of (h) by the semi-major axis length</li>
 *   </ul></li>
 *   <li><cite>Denormalization</cite> after {@code EllipsoidalToCartesianTransform}:<ul>
 *     <li>Multiplication of (X,Y,Z) by the semi-major axis length</li>
 *   </ul></li>
 * </ul>
 *
 * The full conversion chain including the above affine transforms
 * can be created by {@link #createGeodeticConversion(MathTransformFactory)}.
 * Alternatively, the {@link #createGeodeticConversion(Ellipsoid, boolean)} convenience method can also be used.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class EllipsoidalToCartesianTransform extends AbstractMathTransform implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3352045463953828140L;

    /**
     * Internal parameter descriptor, used only for debugging purpose.
     * Created only when first needed.
     *
     * @see #getParameterDescriptors()
     */
    @Debug
    private static ParameterDescriptorGroup DESCRIPTOR;

    /**
     * Minimal excentricity value before to consider that the approximated φ value should be made more accurate
     * by the use of an iterative method. The iterative method is not needed for a planet of Earth excentricity,
     * but become useful for planets of higher excentricity.
     *
     * <p>Actually the need for iteration is not just a matter of excentricity. It is also a matter of
     * <var>h</var> values. But empirical tests suggest that with Earth's excentricity (about 0.082),
     * the limit for <var>h</var> is quite high (close to 2000 km for a point at 30°N). This limit is
     * reduced to about 200 km for an excentricity of 0.16. It may be possible to find a formula for
     * the limit of <var>h</var> as a function of ℯ and φ, but this has not been explored yet.</p>
     *
     * @see org.apache.sis.referencing.operation.projection.ConformalProjection#EXCENTRICITY_THRESHOLD
     */
    private static final double EXCENTRICITY_THRESHOLD = 0.16;

    /**
     * The square of excentricity: ℯ² = (a²-b²)/a² where
     * <var>a</var> is the <cite>semi-major</cite> axis length and
     * <var>b</var> is the <cite>semi-minor</cite> axis length.
     */
    protected final double excentricitySquared;

    /**
     * The b/a ratio where
     * <var>a</var> is the <cite>semi-major</cite> axis length and
     * <var>b</var> is the <cite>semi-minor</cite> axis length.
     * Since the {@code EllipsoidalToCartesianTransform} class works on an ellipsoid where a = 1
     * (because of the work performed by the normalization matrices), we just drop <var>a</var>
     * in the formulas - so this field can be written as just <var>b</var>.
     *
     * <p>This value is related to the ε value used in EPSG guide by ε = ℯ²/b² (assuming a=1).</p>
     */
    private final double b;

    /**
     * {@code true} if the excentricity value is greater than or equals to {@link #EXCENTRICITY_THRESHOLD},
     * in which case the calculation of φ will need to use an iterative method.
     */
    private final boolean useIterations;

    /**
     * {@code true} if ellipsoidal coordinates include an ellipsoidal height (i.e. are 3-D).
     * If {@code false}, then the input coordinates are expected to be two-dimensional and
     * the ellipsoidal height is assumed to be 0.
     */
    final boolean withHeight;

    /**
     * The parameters used for creating this conversion.
     * They are used for formatting <cite>Well Known Text</cite> (WKT) and error messages.
     *
     * @see #getContextualParameters()
     */
    private final ContextualParameters context;

    /**
     * The inverse of this transform.
     * Created at construction time because needed soon anyway.
     */
    private final AbstractMathTransform inverse;

    /**
     * Creates a transform from an ellipsoid of semi-major axis length of 1.
     * Angular units of input coordinates are <strong>radians</strong>.
     *
     * <p>For a conversion from angles in degrees and height in metres, see the
     * {@link #createGeodeticConversion(MathTransformFactory)} method.</p>
     *
     * @param semiMajor  The semi-major axis length.
     * @param semiMinor  The semi-minor axis length.
     * @param unit       The unit of measurement for the semi-axes and the ellipsoidal height.
     * @param withHeight {@code true} if geographic coordinates include an ellipsoidal height (i.e. are 3-D),
     *                   or {@code false} if they are only 2-D.
     */
    public EllipsoidalToCartesianTransform(final double semiMajor, final double semiMinor, final Unit<Length> unit, final boolean withHeight) {
        ArgumentChecks.ensureStrictlyPositive("semiMajor", semiMajor);
        ArgumentChecks.ensureStrictlyPositive("semiMinor", semiMinor);
        b = semiMinor / semiMajor;
        excentricitySquared = 1 - (b * b);
        useIterations = (excentricitySquared >= EXCENTRICITY_THRESHOLD * EXCENTRICITY_THRESHOLD);
        this.withHeight = withHeight;
        context = new ContextualParameters(GeographicToGeocentric.PARAMETERS, withHeight ? 4 : 3, 4);
        /*
         * Copy parameters to the ContextualParameter. Those parameters are not used directly
         * by EllipsoidToCartesian, but we need to store them in case the user asks for them.
         */
        context.getOrCreate(SEMI_MAJOR).setValue(semiMajor, unit);
        context.getOrCreate(SEMI_MINOR).setValue(semiMinor, unit);
        if (!withHeight) {
            context.getOrCreate(DIMENSION).setValue(2);
        }
        /*
         * Prepare two affine transforms to be executed before and after this EllipsoidalToCartesianTransform:
         *
         *   - A "normalization" transform for conversing degrees to radians and normalizing the height,
         *   - A "denormalization" transform for scaling (X,Y,Z) to the semi-major axis length.
         */
        context.normalizeGeographicInputs(0);
        final DoubleDouble a = new DoubleDouble(semiMajor);
        final MatrixSIS denormalize = context.getMatrix(false);
        for (int i=0; i<3; i++) {
            denormalize.convertAfter(i, a, null);
        }
        if (withHeight) {
            a.inverseDivide(1, 0);
            context.getMatrix(true).convertBefore(2, a, null);    // Divide ellipsoidal height by a.
        }
        inverse = new Inverse();
    }

    /**
     * Creates a transform from geographic to geocentric coordinates. This convenience method combines the
     * {@code EllipsoidalToCartesianTransform} instance with the steps needed for converting degrees to radians and
     * expressing the results in units of the given ellipsoid.
     *
     * <p>Input coordinates are expected to contain:</p>
     * <ol>
     *   <li>longitudes in degrees relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in degrees,</li>
     *   <li>optionally heights above the ellipsoid, in units of the ellipsoid axis (usually metres).</li>
     * </ol>
     *
     * Output coordinates are as below, in units of the ellipsoid axis (usually metres):
     * <ol>
     *   <li>distance from Earth center on the X axis (toward the intersection of prime meridian and equator),</li>
     *   <li>distance from Earth center on the Y axis (toward the intersection of 90°E meridian and equator),</li>
     *   <li>distance from Earth center on the Z axis (toward North pole).</li>
     * </ol>
     *
     * @param ellipsoid  The ellipsoid of source coordinates.
     * @param withHeight {@code true} if geographic coordinates include an ellipsoidal height (i.e. are 3-D),
     *                   or {@code false} if they are only 2-D.
     * @return The conversion from geographic to geocentric coordinates.
     */
    public static MathTransform createGeodeticConversion(final Ellipsoid ellipsoid, final boolean withHeight) {
        ArgumentChecks.ensureNonNull("ellipsoid", ellipsoid);
        try {
            return new EllipsoidalToCartesianTransform(
                    ellipsoid.getSemiMajorAxis(), ellipsoid.getSemiMinorAxis(), ellipsoid.getAxisUnit(), withHeight)
                    .createGeodeticConversion(DefaultFactories.forBuildin(MathTransformFactory.class));
        } catch (FactoryException e) {
            /*
             * Should not happen with SIS factory implementation. If it happen anyway,
             * maybe we got some custom factory implementation with limited functionality.
             */
            throw new IllegalStateException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Returns the sequence of <cite>normalization</cite> → {@code this} → <cite>denormalization</cite>
     * transforms as a whole. The transform returned by this method expects input coordinate having the
     * following values:
     *
     * <ol>
     *   <li>longitudes in degrees relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in degrees,</li>
     *   <li>optionally heights above the ellipsoid, in the units given to the constructor (usually metres).</li>
     * </ol>
     *
     * The converted coordinates will be lengths in the units given to the constructor (usually metres).
     *
     * @param  factory The factory to use for creating the transform.
     * @return The conversion from geographic to geocentric coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     *
     * @see ContextualParameters#completeTransform(MathTransformFactory, MathTransform)
     * @see org.apache.sis.referencing.operation.projection.NormalizedProjection#createMapProjection(MathTransformFactory)
     */
    public MathTransform createGeodeticConversion(final MathTransformFactory factory) throws FactoryException {
        return context.completeTransform(factory, this);
    }

    /**
     * Returns the parameters used for creating the complete conversion. Those parameters describe a sequence
     * of <cite>normalize</cite> → {@code this} → <cite>denormalize</cite> transforms, <strong>not</strong>
     * including {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes axis swapping}.
     * Those parameters are used for formatting <cite>Well Known Text</cite> (WKT) and error messages.
     *
     * @return The parameters values for the sequence of
     *         <cite>normalize</cite> → {@code this} → <cite>denormalize</cite> transforms.
     */
    @Override
    protected final ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Returns a copy of internal parameter values of this {@code EllipsoidalToCartesianTransform} transform.
     * The returned group contains parameter values for the number of dimensions and the excentricity.
     *
     * <div class="note"><b>Note:</b>
     * This method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters}.</div>
     *
     * @return A copy of the internal parameter values for this transform.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        final ParameterValueGroup pg = getParameterDescriptors().createValue();
        pg.parameter("excentricity").setValue(sqrt(excentricitySquared));
        pg.parameter("dim").setValue(getSourceDimensions());
        return pg;
    }

    /**
     * Returns a description of the internal parameters of this {@code EllipsoidalToCartesianTransform} transform.
     * The returned group contains parameter descriptors for the number of dimensions and the excentricity.
     *
     * @return A description of the internal parameters.
     */
    @Debug
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        synchronized (EllipsoidalToCartesianTransform.class) {
            if (DESCRIPTOR == null) {
                DESCRIPTOR = new ParameterBuilder().setCodeSpace(Citations.SIS, Constants.SIS)
                        .addName("Ellipsoidal to Cartesian").createGroup(1, 1, DIMENSION, EXCENTRICITY);
            }
            return DESCRIPTOR;
        }
    }

    /**
     * Gets the dimension of input points, which is 2 or 3.
     *
     * @return 2 or 3.
     */
    @Override
    public final int getSourceDimensions() {
        return withHeight ? 3 : 2;
    }

    /**
     * Gets the dimension of output points, which is 3.
     *
     * @return Always 3.
     */
    @Override
    public final int getTargetDimensions() {
        return 3;
    }

    /**
     * Converts the (λ,φ) or (λ,φ,<var>h</var>) geodetic coordinates to
     * to (<var>X</var>,<var>Y</var>,<var>Z</var>) geocentric coordinates,
     * and optionally returns the derivative at that location.
     *
     * @return {@inheritDoc}
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate)
    {
        return transform(srcPts, srcOff, dstPts, dstOff, derivate, withHeight);
    }

    /**
     * Implementation of {@link #transform(double[], int, double[], int, boolean)}
     * with possibility to override the {@link #withHeight} value.
     */
    final Matrix transform(final double[] srcPts, final int srcOff,
                           final double[] dstPts, final int dstOff,
                           final boolean derivate, final boolean withHeight)
    {
        final double λ    = srcPts[srcOff  ];                            // Longitude (radians)
        final double φ    = srcPts[srcOff+1];                            // Latitude (radians)
        final double h    = withHeight ? srcPts[srcOff+2] : 0;           // Height above the ellipsoid
        final double cosλ = cos(λ);
        final double sinλ = sin(λ);
        final double cosφ = cos(φ);
        final double sinφ = sin(φ);
        final double ν2   = 1 / (1 - excentricitySquared*(sinφ*sinφ));   // Square of ν (see below)
        final double ν    = sqrt(ν2);                                    // Prime vertical radius of curvature at latitude φ
        final double r    = ν + h;
        final double νℯ   = ν * (1 - excentricitySquared);
        if (dstPts != null) {
            final double rcosφ = r * cosφ;
            dstPts[dstOff  ] = rcosφ  * cosλ;                            // X: Toward prime meridian
            dstPts[dstOff+1] = rcosφ  * sinλ;                            // Y: Toward 90° east
            dstPts[dstOff+2] = (νℯ+h) * sinφ;                            // Z: Toward north pole
        }
        if (derivate) {
            final double sdφ   = νℯ * ν2 + h;
            final double dX_dh = cosφ * cosλ;
            final double dY_dh = cosφ * sinλ;
            final double dX_dλ = -r * dY_dh;
            final double dY_dλ =  r * dX_dh;
            final double dX_dφ = -sdφ * (sinφ * cosλ);
            final double dY_dφ = -sdφ * (sinφ * sinλ);
            final double dZ_dφ =  sdφ * cosφ;
            if (withHeight) {
                return new Matrix3(dX_dλ, dX_dφ, dX_dh,
                                   dY_dλ, dY_dφ, dY_dh,
                                       0, dZ_dφ, sinφ);
            } else {
                return Matrices.create(3, 2, new double[] {
                        dX_dλ, dX_dφ,
                        dY_dλ, dY_dφ,
                            0, dZ_dφ});
            }
        } else {
            return null;
        }
    }

    /**
     * Converts the (λ,φ) or (λ,φ,<var>h</var>) geodetic coordinates to
     * to (<var>X</var>,<var>Y</var>,<var>Z</var>) geocentric coordinates.
     *
     * This method performs the same conversion than {@link #transform(double[], int, double[], int, boolean)},
     * but the formulas are repeated here for performance reasons.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts) {
        int srcInc = 0;
        int dstInc = 0;
        if (srcPts == dstPts) {
            final int dimSource = getSourceDimensions();
            switch (IterationStrategy.suggest(srcOff, dimSource, dstOff, 3, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * dimSource;
                    dstOff += (numPts - 1) * 3;         // Target dimension is fixed to 3.
                    srcInc = -2 * dimSource;
                    dstInc = -6;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*dimSource);
                    srcOff = 0;
                    break;
                }
            }
        }
        while (--numPts >= 0) {
            final double λ     = srcPts[srcOff++];                                 // Longitude
            final double φ     = srcPts[srcOff++];                                 // Latitude
            final double h     = withHeight ? srcPts[srcOff++] : 0;                // Height above the ellipsoid
            final double sinφ  = sin(φ);
            final double ν     = 1/sqrt(1 - excentricitySquared * (sinφ*sinφ));    // Prime vertical radius of curvature at latitude φ
            final double rcosφ = (ν + h) * cos(φ);
            dstPts[dstOff++]   = rcosφ * cos(λ);                                   // X: Toward prime meridian
            dstPts[dstOff++]   = rcosφ * sin(λ);                                   // Y: Toward 90° east
            dstPts[dstOff++]   = (ν * (1 - excentricitySquared) + h) * sinφ;       // Z: Toward north pole
            srcOff += srcInc;
            dstOff += dstInc;
        }
    }

    /*
     * NOTE: we do not bother to override the methods expecting a 'float' array because those methods should
     *       be rarely invoked. Since there is usually LinearTransforms before and after this transform, the
     *       conversion between float and double will be handle by those LinearTransforms.   If nevertheless
     *       this EllipsoidalToCartesianTransform is at the beginning or the end of a transformation chain,
     *       the method inherited from the subclass will work (even if slightly slower).
     */

    /**
     * Converts Cartesian coordinates (<var>X</var>,<var>Y</var>,<var>Z</var>) to ellipsoidal coordinates
     * (λ,φ) or (λ,φ,<var>h</var>). This method is invoked by the transform returned by {@link #inverse()}.
     *
     * @param  srcPts The array containing the source point coordinates.
     * @param  srcOff The offset to the first point to be transformed in the source array.
     * @param  dstPts The array into which the transformed point coordinates are returned.
     *                May be the same than {@code srcPts}.
     * @param  dstOff The offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts The number of point objects to be transformed.
     * @throws TransformException if the calculation does not converge.
     */
    protected void inverseTransform(final double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        int srcInc = 0;
        int dstInc = 0;
        if (srcPts == dstPts) {
            final int dimTarget = getSourceDimensions();
            switch (IterationStrategy.suggest(srcOff, 3, dstOff, dimTarget, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * 3;             // Source dimension is fixed to 3.
                    dstOff += (numPts - 1) * dimTarget;
                    srcInc = -6;
                    dstInc = -2 * dimTarget;
                    break;
                }
                default: {
                    dstPts = Arrays.copyOfRange(dstPts, dstOff, dstOff + numPts*dimTarget);
                    dstOff = 0;
                    break;
                }
            }
        }
next:   while (--numPts >= 0) {
            final double X = srcPts[srcOff++];
            final double Y = srcPts[srcOff++];
            final double Z = srcPts[srcOff++];
            final double p = hypot(X, Y);
            /*
             * EPSG guide gives  q = atan((Z⋅a) / (p⋅b))
             * where in this class  a = 1  because of the normalization matrix.
             * Since the formulas use only  sin(q)  and  cos(q), we rewrite as
             *
             *    cos²(q) = 1/(1 + tan²(q))         and  cos(q)  is always positive
             *    sin²(q) = 1 - cos²(q)             and  sin(q)  has the sign of tan(q).
             */
            final double tanq  = Z / (p*b);
            final double cos2q = 1/(1 + tanq*tanq);
            final double sin2q = 1 - cos2q;
            double φ = atan((Z + copySign(excentricitySquared * sin2q*sqrt(sin2q), tanq) / b) /
                            (p -          excentricitySquared * cos2q*sqrt(cos2q)));
            /*
             * The above is an approximation of φ. Usually we are done with a good approximation for
             * a planet of the excentricity of Earth. Code below is the one that will be executed in
             * the vast majority of cases.
             */
            if (!useIterations) {
                dstPts[dstOff++] = atan2(Y, X);
                dstPts[dstOff++] = φ;
                if (withHeight) {
                    final double sinφ = sin(φ);
                    final double ν = 1/sqrt(1 - excentricitySquared * (sinφ*sinφ));
                    dstPts[dstOff++] = p/cos(φ) - ν;
                }
                srcOff += srcInc;
                dstOff += dstInc;
            } else {
                /*
                 * If this code is used on a planet with high excentricity,
                 * the φ value may need to be improved by an iterative method.
                 */
                for (int it=0; it<Formulas.MAXIMUM_ITERATIONS; it++) {
                    final double sinφ = sin(φ);
                    final double ν = 1/sqrt(1 - excentricitySquared * (sinφ*sinφ));
                    final double Δφ = φ - (φ = atan((Z + excentricitySquared * ν * sinφ) / p));
                    if (!(abs(Δφ) >= Formulas.ANGULAR_TOLERANCE * (PI/180) * 0.25)) {   // Use ! for accepting NaN.
                        dstPts[dstOff++] = atan2(Y, X);
                        dstPts[dstOff++] = φ;
                        if (withHeight) {
                            dstPts[dstOff++] = p/cos(φ) - ν;
                        }
                        srcOff += srcInc;
                        dstOff += dstInc;
                        continue next;
                    }
                }
                throw new TransformException(Errors.format(Errors.Keys.NoConvergence));
            }
        }
    }

    /**
     * Returns the inverse of this transform. The default implementation returns a transform
     * that will delegate its work to {@link #inverseTransform(double[], int, double[], int, int)}.
     *
     * @return The conversion from Cartesian to ellipsoidal coordinates.
     */
    @Override
    public MathTransform inverse() {
        return inverse;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() + Numerics.hashCode(Double.doubleToLongBits(b));
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
        if (super.equals(object, mode)) {
            final EllipsoidalToCartesianTransform that = (EllipsoidalToCartesianTransform) object;
            return (withHeight == that.withHeight) && Numerics.equals(b, that.b);
        }
        return false;
    }




    /**
     * Converts Cartesian coordinates (<var>X</var>,<var>Y</var>,<var>Z</var>)
     * to ellipsoidal coordinates (λ,φ) or (λ,φ,<var>h</var>).
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    private final class Inverse extends AbstractMathTransform.Inverse implements Serializable {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = 6942084702259211803L;

        /**
         * Creates the inverse of the enclosing transform.
         */
        Inverse() {
        }

        /**
         * Compute the derivative at the given location. We need to override this method because
         * we will inverse 3×2 matrices in a special way, with the knowledge that <var>h</var>
         * can be set to 0.
         */
        @Override
        public Matrix derivative(final DirectPosition point) throws TransformException {
            ArgumentChecks.ensureNonNull("point", point);
            final double[] coordinate = point.getCoordinate();
            ArgumentChecks.ensureDimensionMatches("point", 3, coordinate);
            return this.transform(coordinate, 0, coordinate, 0, true);
        }

        /**
         * Inverse transforms a single coordinate in a list of ordinal values,
         * and optionally returns the derivative at that location.
         *
         * <p>This method delegates the derivative computation to the enclosing class, then inverses the result.
         * This is much easier than trying to compute the derivative from the formulas of this inverse transform.</p>
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final boolean derivate) throws TransformException
        {
            final double[] point;
            final int offset;
            if (derivate && (dstPts == null || !withHeight)) {
                point  = new double[3];
                offset = 0;
            } else {
                point  = dstPts;
                offset = dstOff;
            }
            inverseTransform(srcPts, srcOff, point, offset, 1);
            if (!derivate) {
                return null;
            }
            if (dstPts != point && dstPts != null) {
                dstPts[dstOff  ] = point[0];
                dstPts[dstOff+1] = point[1];
            }
            Matrix matrix = Matrices.inverse(EllipsoidalToCartesianTransform.this.transform(point, offset, null, 0, true, true));
            if (!withHeight) {
                matrix = Matrices.removeRow(matrix, 2);     // Drop height
            }
            return matrix;
        }

        /**
         * Transforms the given array of points.
         */
        @Override
        public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts)
                throws TransformException
        {
            inverseTransform(srcPts, srcOff, dstPts, dstOff, numPts);
        }
    }
}
