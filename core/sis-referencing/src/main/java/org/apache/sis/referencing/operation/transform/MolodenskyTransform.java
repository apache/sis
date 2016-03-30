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
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.Molodensky;
import org.apache.sis.internal.referencing.provider.AbridgedMolodensky;
import org.apache.sis.parameter.Parameters;
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
 * <div class="section">Comparison of Molodensky and geocentric translation</div>
 * Compared to the <cite>"Geocentric translation (geographic domain)"</cite> method,
 * the Molodensky method has errors usually within a few centimetres.
 * The Abridged Molodensky method has more noticeable errors, of a few tenths of centimetres.
 *
 * <p>Another difference between Molodensky and geocentric translation methods is their behavior when crossing the
 * anti-meridian. If a datum shift causes a longitude to cross the anti-meridian (e.g. 179.999° become 180.001°),
 * the Molodensky method will keep 180.001° as-is while the geocentric translation method will wrap the longitude
 * to -179.999°. Such wrap-around behavior may or may not be desired, depending on the applications.</p>
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class MolodenskyTransform extends MolodenskyFormula {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7206439437113286122L;

    /**
     * Internal parameter descriptor, used only for debugging purpose.
     * Created only when first needed.
     *
     * @see #getParameterDescriptors()
     */
    @Debug
    private static ParameterDescriptorGroup DESCRIPTOR;

    /**
     * The inverse of this Molodensky transform.
     *
     * @see #inverse()
     */
    private final MolodenskyTransform inverse;

    /**
     * Creates a Molodensky transform from the specified parameters.
     * This {@code MolodenskyTransform} class expects ordinate values in the following order and units:
     * <ol>
     *   <li>longitudes in <strong>radians</strong> relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in <strong>radians</strong>,</li>
     *   <li>optionally heights above the ellipsoid, in same units than the source ellipsoid axes.</li>
     * </ol>
     *
     * For converting geographic coordinates in degrees, {@code MolodenskyTransform} instances
     * need to be concatenated with the following affine transforms:
     *
     * <ul>
     *   <li><cite>Normalization</cite> before {@code MolodenskyTransform}:<ul>
     *     <li>Conversion of (λ,φ) from degrees to radians.</li>
     *   </ul></li>
     *   <li><cite>Denormalization</cite> after {@code MolodenskyTransform}:<ul>
     *     <li>Conversion of (λ,φ) from radians to degrees.</li>
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
        super(source, isSource3D, target, isTarget3D, tX, tY, tZ, null, isAbridged,
                isAbridged ? AbridgedMolodensky.PARAMETERS : Molodensky.PARAMETERS);
        if (!isSource3D && !isTarget3D) {
            inverse = new MolodenskyTransform2D(this, source, target);
        } else {
            inverse = new MolodenskyTransform(this, source, target);
        }
    }

    /**
     * Constructs the inverse of a Molodensky transform.
     *
     * @param inverse The transform for which to create the inverse.
     * @param source  The source ellipsoid of the given {@code inverse} transform.
     * @param target  The target ellipsoid of the given {@code inverse} transform.
     */
    MolodenskyTransform(final MolodenskyTransform inverse, final Ellipsoid source, final Ellipsoid target) {
        super(inverse, source, target, inverse.context.getDescriptor());
        this.inverse = inverse;
    }

    /**
     * Invoked by constructor and by {@link #getParameterValues()} for setting all parameters other than axis lengths.
     *
     * @param pg         Where to set the parameters.
     * @param semiMinor  Ignored.
     * @param unit       The unit of measurement to declare.
     * @param Δf         The flattening difference to set, or NaN if this method should fetch that value itself.
     */
    @Override
    final void completeParameters(final Parameters pg, final double semiMinor, final Unit<?> unit, double Δf) {
        if (Double.isNaN(Δf)) {
            Δf = context.doubleValue(Molodensky.FLATTENING_DIFFERENCE);
        }
        super.completeParameters(pg, semiMinor, unit, Δf);
        pg.getOrCreate(Molodensky.TX)                    .setValue(tX, unit);
        pg.getOrCreate(Molodensky.TY)                    .setValue(tY, unit);
        pg.getOrCreate(Molodensky.TZ)                    .setValue(tZ, unit);
        pg.getOrCreate(Molodensky.AXIS_LENGTH_DIFFERENCE).setValue(Δa, unit);
        pg.getOrCreate(Molodensky.FLATTENING_DIFFERENCE) .setValue(Δf, Unit.ONE);
        if (pg != context) {
            pg.parameter("abridged").setValue(isAbridged);  // Only in internal parameters.
        }
    }

    /**
     * Creates a transformation between two from geographic CRS. This factory method combines the
     * {@code MolodenskyTransform} instance with the steps needed for converting values between
     * degrees to radians. The transform works with input and output coordinates in the following units:
     *
     * <ol>
     *   <li>longitudes in <strong>degrees</strong> relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in <strong>degrees</strong>,</li>
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
     * @return The transformation between geographic coordinates in degrees.
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
        tr.inverse.context.completeTransform(factory, null);
        return tr.context.completeTransform(factory, tr);
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
        return transform(srcPts[srcOff], srcPts[srcOff+1], isSource3D ? srcPts[srcOff+2] : 0,
                         dstPts, dstOff, tX, tY, tZ, null, derivate);
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
         * The code in the following loop is basically a copy-and-paste of the code in the
         * MolodenskyFormula.transform(λ, φ, h, …) method, but without derivative matrix
         * computation and without support for interpolation of (tX,tY,tZ) values in a grid.
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
                  double ρden  = 1 - eccentricitySquared * sin2φ;               // Denominator of ρ (completed later)
            final double νden  = sqrt(ρden);                                    // Denominator of ν
            double ρ = semiMajor * (1 - eccentricitySquared) / (ρden *= νden);  // (also complete calculation of ρden)
            double ν = semiMajor / νden;                                        // Other notation: Rm = ρ and Rn = ν
            double t = Δfmod * 2;                                               // A term in the calculation of Δφ
            if (!isAbridged) {
                ρ += h;
                ν += h;
                t = t*(0.5/νden + 0.5/ρden)                 // = Δf⋅[ν⋅(b/a) + ρ⋅(a/b)]     (without the +h in ν and ρ)
                        + Δa*eccentricitySquared/νden;      // = Δa⋅[ℯ²⋅ν/a]
            }
            final double spcλ = tY*sinλ + tX*cosλ;
            dstPts[dstOff++] = λ + ANGULAR_SCALE * (tY*cosλ - tX*sinλ) / (ν*cosφ);
            dstPts[dstOff++] = φ + ANGULAR_SCALE * ((t*cosφ - spcλ)*sinφ + tZ*cosφ) / ρ;
            if (isTarget3D) {
                t = Δfmod * sin2φ;                          // A term in the calculation of Δh
                double d = Δa;
                if (!isAbridged) {
                    t /= νden;                              // = Δf⋅(b/a)⋅ν⋅sin²φ
                    d *= νden;                              // = Δa⋅(a/ν)
                }
                dstPts[dstOff++] = h + spcλ*cosφ + tZ*sinφ + t - d;
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
     *       conversion between float and double will be handled by those LinearTransforms.  If nevertheless
     *       this MolodenskyTransform is at the beginning or the end of a transformation chain,  the methods
     *       inherited from the subclass will work (but may be slightly slower).
     */

    /**
     * Returns the inverse of this Molodensky transform. The source ellipsoid of the returned transform will
     * be the target ellipsoid of this transform, and conversely.
     *
     * @return A Molodensky transform from the target ellipsoid to the source ellipsoid of this transform.
     */
    @Override
    public MathTransform inverse() {
        return inverse;
    }

    /**
     * Returns a description of the internal parameters of this {@code MolodenskyTransform} transform.
     * The returned group contains parameter descriptors for the number of dimensions and the eccentricity.
     *
     * <div class="note"><b>Note:</b>
     * this method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters}.</div>
     *
     * @return A description of the internal parameters.
     */
    @Debug
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        synchronized (MolodenskyTransform.class) {
            if (DESCRIPTOR == null) {
                DESCRIPTOR = Molodensky.internal();
            }
            return DESCRIPTOR;
        }
    }
}
