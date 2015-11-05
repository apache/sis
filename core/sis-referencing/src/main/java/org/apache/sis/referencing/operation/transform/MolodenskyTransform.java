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
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.internal.referencing.provider.Molodensky;
import org.apache.sis.internal.referencing.provider.AbridgedMolodensky;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Debug;

import static java.lang.Math.*;


/**
 * Two- or three-dimensional datum shift using the (potentially abridged) Molodensky transformation.
 * The Molodensky transformation (EPSG:9604) and the abridged Molodensky transformation (EPSG:9605)
 * transform geographic points from one geographic coordinate reference system to another (a datum shift).
 * The Molodensky formulas are approximations of <cite>Geocentric translation (geographic domain)</cite>
 * transformations (EPSG:1035 and 9603), but performed directly on geographic coordinates without
 * Geographic/Geocentric conversions.
 *
 * <p>{@code MolodenskyTransform}s works conceptually on three-dimensional coordinates, but the ellipsoidal height
 * can be omitted resulting in two-dimensional coordinates. No dimension other than 2 or 3 are allowed.</p>
 * <ul>
 *   <li>If the height is omitted from the input coordinates ({@code isSource3D} = {@code false}),
 *       then the {@linkplain #getSourceDimensions() source dimensions} is 2 and the height is
 *       assumed to be zero.</li>
 *   <li>If the height is omitted from the output coordinates ({@code isTarget3D} = {@code false}),
 *       then the {@linkplain #getTargetDimensions() target dimensions} is 2 and the computed
 *       height (typically non-zero even if the input height was zero) is lost.</li>
 * </ul>
 *
 * The transform expect ordinate values if the following order:
 * <ol>
 *   <li>longitudes (λ) relative to the prime meridian (usually Greenwich),</li>
 *   <li>latitudes (φ),</li>
 *   <li>optionally heights above the ellipsoid (h).</li>
 * </ol>
 *
 * The units of measurements depend on how the {@code MathTransform} has been created:
 * <ul>
 *   <li>{@code MolodenskyTransform} instances created directly by the constructor work with angular values in radians.
 *       That constructor is reserved for subclasses only.</li>
 *   <li>Transforms created by the {@link #createGeodeticTransformation createGeodeticTransformation(…)} static method
 *       work with angular values in degrees and heights in the same units than the <strong>source</strong> ellipsoid
 *       axes (usually metres).</li>
 * </ul>
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class MolodenskyTransform extends AbstractMathTransform implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7206439437113286122L;

    /**
     * The value of 1/sin(1″) multiplied by the conversion factor from arc-seconds to radians (π/180)/(60⋅60).
     * This is the final multiplication factor for Δλ and Δφ.
     */
    private static final double ANGULAR_SCALE = 1.00000000000391744;

    /**
     * A mask value for {@link #type}.
     * <ul>
     *   <li>If set, the source coordinates are three-dimensional.</li>
     *   <li>If unset, the source coordinates are two-dimensional.</li>
     * </ul>
     */
    private static final byte SOURCE_DIMENSION_MASK = 1;

    /**
     * A mask value for {@link #type}.
     * <ul>
     *   <li>If set, the target coordinates are three-dimensional.</li>
     *   <li>If unset, the target coordinates are two-dimensional.</li>
     * </ul>
     */
    private static final byte TARGET_DIMENSION_MASK = 2;

    /**
     * A mask value for {@link #type}.
     * <ul>
     *   <li>If set, the transform uses the abridged formulas.</li>
     *   <li>If unset, the transform uses the complete formulas.</li>
     * </ul>
     */
    private static final byte ABRIDGED_MASK = 4;

    /**
     * Bitwise combination of the {@code *_MASK} constants.
     */
    private final byte type;

    /**
     * X,Y,Z shifts in units of the semi-major axis of the source ellipsoid.
     */
    private final double tX, tY, tZ;

    /**
     * Semi-major axis length (<var>a</var>) of the source ellipsoid.
     */
    private final double semiMajor;

    /**
     * The square of excentricity of the source ellipsoid.
     * This can be computed by ℯ² = (a²-b²)/a² where
     * <var>a</var> is the <cite>semi-major</cite> axis length and
     * <var>b</var> is the <cite>semi-minor</cite> axis length.
     */
    private final double excentricitySquared;

    /**
     * Difference in the semi-major axes of the target and source ellipsoids: {@code Δa = target a - source a}.
     * This field is needed explicitely only for the non-abridged form of Molodensky transformation.
     */
    private final double Δa;

    /**
     * Difference between the flattening of the target and source ellipsoids (Δf), opportunistically modified
     * with additional terms. The value depends on whether this Molodensky transform is abridged or not:
     *
     * <ul>
     *   <li>For Molodensky, this field is set to (b⋅Δf).</li>
     *   <li>For Abridged Molodensky, this field is set to (a⋅Δf) + (f⋅Δa).</li>
     * </ul>
     *
     * where Δf = <var>target flattening</var> - <var>source flattening</var>.
     */
    private final double Δfmod;

    /**
     * The parameters used for creating this conversion.
     * They are used for formatting <cite>Well Known Text</cite> (WKT) and error messages.
     *
     * @see #getContextualParameters()
     */
    private final ContextualParameters context;

    /**
     * Creates a Molodensky transform from the specified parameters.
     * This {@code MolodenskyTransform} class expects ordinate values if the following order and units:
     * <ol>
     *   <li>longitudes in <strong>radians</strong> relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in <strong>radians</strong>,</li>
     *   <li>optionally heights above the ellipsoid, in same units than the source ellipsoids axes.</li>
     * </ol>
     *
     * For converting geographic coordinates in degrees, {@code MolodenskyTransform} instances
     * need to be concatenated with the following affine transforms:
     *
     * <ul>
     *   <li><cite>Normalization</cite> before {@code MolodenskyTransform}:<ul>
     *     <li>Conversion of (λ,φ) from degrees to radians.</li>
     *     <li>Any implementation-dependent linear conversion of <var>h</var>.</li>
     *   </ul></li>
     *   <li><cite>Denormalization</cite> after {@code MolodenskyTransform}:<ul>
     *     <li>Conversion of (λ,φ) from radians to degrees.</li>
     *     <li>Any implementation-dependent linear conversion of <var>h</var>.</li>
     *   </ul></li>
     * </ul>
     *
     * After {@code MolodenskyTransform} construction,
     * the full conversion chain including the above affine transforms can be created by
     * <code>{@linkplain #getContextualParameters()}.{@linkplain ContextualParameters#completeTransform
     * completeTransform}(factory, this)}</code>.
     *
     * @param source      The source ellipsoid.
     * @param isSource3D  {@code true} if the source coordinates have a height.
     * @param target      The target ellipsoid.
     * @param isTarget3D  {@code true} if the target coordinates have a height.
     * @param tX          The geocentric <var>X</var> translation in same units than the source ellipsoid axes.
     * @param tY          The geocentric <var>Y</var> translation in same units than the source ellipsoid axes.
     * @param tZ          The geocentric <var>Z</var> translation in same units than the source ellipsoid axes.
     * @param isAbridged  {@code true} for the abridged formula, or {@code false} for the complete one.
     *
     * @see #createGeodeticTransformation(MathTransformFactory, Ellipsoid, boolean, Ellipsoid, boolean, double, double, double, boolean)
     */
    protected MolodenskyTransform(final Ellipsoid source, final boolean isSource3D,
                                  final Ellipsoid target, final boolean isTarget3D,
                                  final double tX, final double tY, final double tZ,
                                  final boolean isAbridged)
    {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        final DefaultEllipsoid src = DefaultEllipsoid.castOrCopy(source);
        byte type = isAbridged ? ABRIDGED_MASK : 0;
        if (isSource3D) type |= SOURCE_DIMENSION_MASK;
        if (isTarget3D) type |= TARGET_DIMENSION_MASK;
        this.type      = type;
        this.semiMajor = src.getSemiMajorAxis();
        this.Δa        = src.semiMajorDifference(target);
        this.tX        = tX;
        this.tY        = tY;
        this.tZ        = tZ;

        final double semiMinor   = src.getSemiMinorAxis();
        final double ΔFlattening = src.flatteningDifference(target);
        excentricitySquared      = src.getEccentricitySquared();
        Δfmod = isAbridged ? (semiMajor * ΔFlattening) + (semiMajor - semiMinor) * (Δa / semiMajor)
                           : (semiMinor * ΔFlattening);
        /*
         * Copy parameters to the ContextualParameter. Those parameters are not used directly
         * by EllipsoidToCartesian, but we need to store them in case the user asks for them.
         * When both EPSG and OGC parameters exist for equivalent information, we use EPSG ones.
         */
        context = new ContextualParameters(isAbridged ? AbridgedMolodensky.PARAMETERS : Molodensky.PARAMETERS,
                                           isSource3D ? 4 : 3, isTarget3D ? 4 : 3);
        if (isSource3D == isTarget3D) {
            context.getOrCreate(Molodensky.DIMENSION).setValue(isSource3D ? 3 : 2);
        }
        final Unit<Length> unit = src.getAxisUnit();
        context.getOrCreate(Molodensky.TX)                    .setValue(tX, unit);
        context.getOrCreate(Molodensky.TY)                    .setValue(tY, unit);
        context.getOrCreate(Molodensky.TZ)                    .setValue(tZ, unit);
        context.getOrCreate(Molodensky.AXIS_LENGTH_DIFFERENCE).setValue(Δa, unit);
        context.getOrCreate(Molodensky.FLATTENING_DIFFERENCE) .setValue(ΔFlattening, Unit.ONE);
        context.getOrCreate(Molodensky.SRC_SEMI_MAJOR)        .setValue(semiMajor,   unit);
        context.getOrCreate(Molodensky.SRC_SEMI_MINOR)        .setValue(semiMinor,   unit);
        /*
         * Prepare two affine transforms to be executed before and after this MolodenskyTransform:
         *
         *   - A "normalization" transform for converting degrees to radians,
         *   - A "denormalization" transform for for converting radians to degrees.
         */
        context.normalizeGeographicInputs(0);
        context.denormalizeGeographicOutputs(0);
        /*
         * In the particular case of abridged Molondensky, there is some more terms than we can
         * delegate to the matrices. We can not do that for the non-abridged formulas however.
         */
        if (isTarget3D && isAbridged) {
            context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION).convertAfter(2, null, -Δa);
        }
    }

    /**
     * Creates a transformation between two from geographic CRS. This factory method combines the
     * {@code MolodenskyTransform} instance with the steps needed for converting values between
     * degrees to radians. The transform works with input and output coordinates in the following units:
     *
     * <ol>
     *   <li>longitudes in degrees relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in degrees,</li>
     *   <li>optionally heights above the ellipsoid, in same units than the source ellipsoids axes.</li>
     * </ol>
     *
     * @param factory     The factory to use for creating the transform.
     * @param source      The source ellipsoid.
     * @param isSource3D  {@code true} if the source coordinates have a height.
     * @param target      The target ellipsoid.
     * @param isTarget3D  {@code true} if the target coordinates have a height.
     * @param tX          The geocentric <var>X</var> translation in same units than the source ellipsoid axes.
     * @param tY          The geocentric <var>Y</var> translation in same units than the source ellipsoid axes.
     * @param tZ          The geocentric <var>Z</var> translation in same units than the source ellipsoid axes.
     * @param isAbridged  {@code true} for the abridged formula, or {@code false} for the complete one.
     * @return The transformation between geographic coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static MathTransform createGeodeticTransformation(final MathTransformFactory factory,
            final Ellipsoid source, final boolean isSource3D,
            final Ellipsoid target, final boolean isTarget3D,
            final double tX, final double tY, final double tZ,
            final boolean isAbridged) throws FactoryException
    {
        final MolodenskyTransform tr;
        if (!isSource3D && !isTarget3D) {
            tr = new MolodenskyTransform2D(source, target, tX, tY, tZ, isAbridged);
        } else {
            tr = new MolodenskyTransform(source, isSource3D, target, isTarget3D, tX, tY, tZ, isAbridged);
        }
        return tr.context.completeTransform(factory, tr);
    }

    /**
     * Returns the parameters used for creating the complete transformation. Those parameters describe a sequence
     * of <cite>normalize</cite> → {@code this} → <cite>denormalize</cite> transforms, <strong>not</strong>
     * including {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes axis swapping}.
     * Those parameters are used for formatting <cite>Well Known Text</cite> (WKT) and error messages.
     *
     * @return The parameters values for the sequence of
     *         <cite>normalize</cite> → {@code this} → <cite>denormalize</cite> transforms.
     */
    @Override
    protected ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Returns a copy of internal parameter values of this {@code MolodenskyTransform} transform.
     * The returned group contains parameter values for the number of dimensions and the excentricity.
     *
     * <div class="note"><b>Note:</b>
     * this method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters}.</div>
     *
     * @return A copy of the internal parameter values for this transform.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        final Parameters pg = Parameters.castOrWrap(getParameterDescriptors().createValue());
        final int dimension = getSourceDimensions();
        if (dimension == getTargetDimensions()) {
            pg.getOrCreate(Molodensky.DIMENSION).setValue(dimension);
        }
        pg.getOrCreate(MapProjection.EXCENTRICITY).setValue(sqrt(excentricitySquared));
        // TODO: add other parameters
        return pg;
    }

    /**
     * Returns a description of the internal parameters of this {@code MolodenskyTransform} transform.
     * The returned group contains parameter descriptors for the number of dimensions and the excentricity.
     *
     * @return A description of the internal parameters.
     */
    @Debug
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return null; // TODO
    }

    /**
     * Returns {@code true} if this transform is the identity one.
     * Molodensky transform is considered identity (minus rounding errors) if:
     *
     * <ul>
     *   <li>the X,Y,Z shift are zero,</li>
     *   <li>difference between semi-major axis lengths (Δa) is zero,</li>
     *   <li>difference between flattening factors (Δf) is zero,</li>
     *   <li>the input and output dimension are the same.</li>
     * </ul>
     *
     * @return {@code true} if this transform is the identity transform.
     */
    @Override
    public boolean isIdentity() {
        return tX == 0 && tY == 0 && tZ == 0 && Δa == 0 && Δfmod == 0 && getSourceDimensions() == getTargetDimensions();
    }

    /**
     * Returns {@code true} if this Molodensky transform uses abridged formulas instead than the complete ones.
     * This is the value of the {@code isAbridged} boolean argument given to the constructor.
     *
     * @return {@code true} if this transform uses abridged formulas.
     */
    public boolean isAbridged() {
        return (type & ABRIDGED_MASK) != 0;
    }

    /**
     * Gets the dimension of input points.
     *
     * @return The input dimension, which is 2 or 3.
     */
    @Override
    public final int getSourceDimensions() {
        return (type & SOURCE_DIMENSION_MASK) != 0 ? 3 : 2;
    }

    /**
     * Gets the dimension of output points.
     *
     * @return The output dimension, which is 2 or 3.
     */
    @Override
    public final int getTargetDimensions() {
        return (type & TARGET_DIMENSION_MASK) != 0 ? 3 : 2;
    }

    /**
     * Computes the derivative at the given location.
     * This method relaxes a little bit the {@code MathTransform} contract by accepting two- or three-dimensional
     * points even if the number of dimensions does not match the {@link #getSourceDimensions()} or
     * {@link #getTargetDimensions()} values.
     *
     * @param  point The coordinate point where to evaluate the derivative.
     * @return The derivative at the specified point (never {@code null}).
     * @throws TransformException if the derivative can not be evaluated at the specified point.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        final int dim = point.getDimension();
        final boolean withHeight;
        final double h;
        switch (dim) {
            default: throw mismatchedDimension("point", getSourceDimensions(), dim);
            case 3:  withHeight = true;  h = point.getOrdinate(2); break;
            case 2:  withHeight = false; h = 0; break;
        }
        return transform(point.getOrdinate(0), point.getOrdinate(1), h, withHeight, null, 0, withHeight, true);
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
        final boolean isSource3D = (type & SOURCE_DIMENSION_MASK) != 0;
        final boolean isTarget3D = (type & TARGET_DIMENSION_MASK) != 0;
        return transform(srcPts[srcOff], srcPts[srcOff+1], isSource3D ? srcPts[srcOff+2] : 0,
                         isSource3D, dstPts, dstOff, isTarget3D, derivate);
    }

    /**
     * Implementation of {@link #transform(double[], int, double[], int, boolean)} with possibility
     * to override whether the source and target coordinates are two- or three-dimensional.
     *
     * @param λ        Longitude (radians).
     * @param φ        Latitude (radians).
     * @param h        Height above the ellipsoid in unit of semi-major axis.
     * @param dstPts   The array into which the transformed coordinate is returned.
     *                 May be {@code null} if only the derivative matrix is desired.
     * @param dstOff   The offset to the location of the transformed point that is stored in the destination array.
     * @param derivate {@code true} for computing the derivative, or {@code false} if not needed.
     * @throws TransformException if a point can not be transformed.
     */
    private Matrix transform(final double λ, final double φ, final double h, final boolean isSource3D,
                             final double[] dstPts, int dstOff, final boolean isTarget3D,
                             final boolean derivate) throws TransformException
    {
        final boolean abridged = (type & ABRIDGED_MASK) != 0;
        /*
         * Abridged Molodensky formulas from EPSG guidance note:
         *
         *     ν   = a / √(1 - ℯ²⋅sin²φ)                        : radius of curvature in the prime vertical
         *     ρ   = a⋅(1 – ℯ²) / (1 – ℯ²⋅sin²φ)^(3/2)          : radius of curvature in the meridian
         *     Δλ″ = (-tX⋅sinλ + tY⋅cosλ) / (ν⋅cosφ⋅sin1″)
         *     Δφ″ = (-tX⋅sinφ⋅cosλ - tY⋅sinφ⋅sinλ + tZ⋅cosφ + [a⋅Δf + f⋅Δa]⋅sin(2φ)) / (ρ⋅sin1″)
         *     Δh  = tX⋅cosφ⋅cosλ + tY⋅cosφ⋅sinλ + tZ⋅sinφ + (a⋅Δf + f⋅Δa)⋅sin²φ - Δa
         *
         * we set:
         *
         *    dfm     = (a⋅Δf + f⋅Δa) in abridged case (b⋅Δf in non-abridged case)
         *    sin(2φ) = 2⋅sin(φ)⋅cos(φ)
         */
        final double sinλ  = sin(λ);
        final double cosλ  = cos(λ);
        final double sinφ  = sin(φ);
        final double cosφ  = cos(φ);
        final double sin2φ = sinφ * sinφ;
              double ρden  = 1 - excentricitySquared * sin2φ;               // Denominator of ρ (completed later)
        final double νden  = sqrt(ρden);                                    // Denominator of ν
        double ρ = semiMajor * (1 - excentricitySquared) / (ρden *= νden);  // (also complete calculation of ρden)
        double ν = semiMajor / νden;
        double t = Δfmod * 2;                                               // A term in the calculation of Δφ
        if (!abridged) {
            ρ += h;
            ν += h;
            t = t*(0.5/νden + 0.5/ρden) + Δa*excentricitySquared/νden;
        }
        final double tXY = tY*sinλ + tX*cosλ;
        dstPts[dstOff++] = λ + ANGULAR_SCALE * (tY*cosλ - tX*sinλ) / (ν*cosφ);
        dstPts[dstOff++] = φ + ANGULAR_SCALE * ((t*cosφ - tXY)*sinφ + tZ*cosφ) / ρ;
        if (isTarget3D) {
            t = Δfmod * sin2φ;                                              // A term in the calculation of Δh
            if (!abridged) {
                t = t/νden - Δa*νden;
                // Note: in abridged Molodensky case, 'Δa' subtracted by the denormalization matrix.
            }
            dstPts[dstOff++] = h + tXY*cosφ + tZ*sinφ + t;
        }
        if (!derivate) {
            return null;
        }
        return null;  // TODO
    }

    /**
     * Transforms the (λ,φ) or (λ,φ,<var>h</var>) coordinates between two geographic CRS.
     * This method performs the same transformation than {@link #transform(double[], int, double[], int, boolean)},
     * but the formulas are repeated here for performance reasons.
     *
     * @throws TransformException if a point can not be transformed.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        final boolean abridged = (type & ABRIDGED_MASK)         != 0;
        final boolean isSource3D = (type & SOURCE_DIMENSION_MASK) != 0;
        final boolean isTarget3D = (type & TARGET_DIMENSION_MASK) != 0;
        int srcInc = 0;
        int dstInc = 0;
        int offFinal = 0;
        double[] dstFinal  = null;
        if (srcPts == dstPts) {
            final int srcDim = isSource3D ? 3 : 2;
            final int dstDim = isTarget3D ? 3 : 2;
            switch (IterationStrategy.suggest(srcOff, srcDim, dstOff, dstDim, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts-1) * srcDim;
                    dstOff += (numPts-1) * dstDim;
                    srcInc = -2 * srcDim;
                    dstInc = -2 * dstDim;
                    break;
                }
                default: {  // BUFFER_SOURCE, but also a reasonable default for any case.
                    final int upper = srcOff + numPts*srcDim;
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, upper);
                    srcOff = 0;
                    break;
                }
                case BUFFER_TARGET: {
                    dstFinal = dstPts;
                    dstPts = new double[numPts * dstDim];
                    offFinal = dstOff;
                    dstOff = 0;
                    break;
                }
            }
        }
        /*
         * The code in the following loop is basically a copy-and-paste of the code in the above
         * transform(λ, φ, h, isSource3D, dstPts, dstOff, isTarget3D, false) method, but without the
         * code for computing the derivative matrix.
         */
        while (--numPts >= 0) {
            final double λ     = srcPts[srcOff++];
            final double φ     = srcPts[srcOff++];
            final double h     = isSource3D ? srcPts[srcOff++] : 0;
            final double sinλ  = sin(λ);
            final double cosλ  = cos(λ);
            final double sinφ  = sin(φ);
            final double cosφ  = cos(φ);
            final double sin2φ = sinφ * sinφ;
                  double ρden  = 1 - excentricitySquared * sin2φ;
            final double νden  = sqrt(ρden);
            double ρ = semiMajor * (1 - excentricitySquared) / (ρden *= νden);
            double ν = semiMajor / νden;
            double t = Δfmod * 2;
            if (!abridged) {
                ρ += h;
                ν += h;
                t = t*(0.5/νden + 0.5/ρden) + Δa*excentricitySquared/νden;
            }
            final double tXY = tY*sinλ + tX*cosλ;
            dstPts[dstOff++] = λ + ANGULAR_SCALE * (tY*cosλ - tX*sinλ) / (ν*cosφ);
            dstPts[dstOff++] = φ + ANGULAR_SCALE * ((t*cosφ - tXY)*sinφ + tZ*cosφ) / ρ;
            if (isTarget3D) {
                t = Δfmod * sin2φ;
                if (!abridged) {
                    t = t/νden - Δa*νden;
                }
                dstPts[dstOff++] = h + tXY*cosφ + tZ*sinφ + t;
            }
            srcOff += srcInc;
            dstOff += dstInc;
        }
        /*
         * If the transformation result has been stored in a temporary
         * array, copies the array content to its final location now.
         */
        if (dstFinal != null) {
            System.arraycopy(dstPts, 0, dstFinal, offFinal, dstPts.length);
        }
    }

    /*
     * NOTE: we do not bother to override the methods expecting a 'float' array because those methods should
     *       be rarely invoked. Since there is usually LinearTransforms before and after this transform, the
     *       conversion between float and double will be handle by those LinearTransforms.   If nevertheless
     *       this EllipsoidalToCartesianTransform is at the beginning or the end of a transformation chain,
     *       the method inherited from the subclass will work (even if slightly slower).
     */

}
