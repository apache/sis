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
import javax.measure.converter.UnitConverter;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.internal.referencing.provider.Molodensky;
import org.apache.sis.internal.referencing.provider.AbridgedMolodensky;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
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
     * Internal parameter descriptor, used only for debugging purpose.
     * Created only when first needed.
     *
     * @see #getParameterDescriptors()
     */
    @Debug
    private static ParameterDescriptorGroup DESCRIPTOR;

    /**
     * The value of 1/sin(1″) multiplied by the conversion factor from arc-seconds to radians (π/180)/(60⋅60).
     * This is the final multiplication factor for Δλ and Δφ.
     */
    private static final double ANGULAR_SCALE = 1.00000000000391744;

    /**
     * {@code true} if the source coordinates have a height.
     */
    private final boolean isSource3D;

    /**
     * {@code true} if the target coordinates have a height.
     */
    private final boolean isTarget3D;

    /**
     * {@code true} for the abridged formula, or {@code false} for the complete one.
     */
    private final boolean isAbridged;

    /**
     * Shift along the geocentric X axis (toward prime meridian)
     * in units of the semi-major axis of the source ellipsoid.
     *
     * @see org.apache.sis.referencing.datum.BursaWolfParameters#tX
     */
    protected final double tX;

    /**
     * Shift along the geocentric Y axis (toward 90°E)
     * in units of the semi-major axis of the source ellipsoid.
     *
     * @see org.apache.sis.referencing.datum.BursaWolfParameters#tY
     */
    protected final double tY;

    /**
     * Shift along the geocentric Z axis (toward north pole)
     * in units of the semi-major axis of the source ellipsoid.
     *
     * @see org.apache.sis.referencing.datum.BursaWolfParameters#tZ
     */
    protected final double tZ;

    /**
     * Semi-major axis length (<var>a</var>) of the source ellipsoid.
     */
    private final double semiMajor;

    /**
     * The square of eccentricity of the source ellipsoid.
     * This can be computed by ℯ² = (a²-b²)/a² where
     * <var>a</var> is the <cite>semi-major</cite> axis length and
     * <var>b</var> is the <cite>semi-minor</cite> axis length.
     *
     * @see DefaultEllipsoid#getEccentricitySquared()
     */
    private final double eccentricitySquared;

    /**
     * Difference in the semi-major axes of the target and source ellipsoids: {@code Δa = target a - source a}.
     *
     * @see DefaultEllipsoid#semiMajorAxisDifference(Ellipsoid)
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
     * The inverse of this Molodensky transform.
     *
     * @see #inverse()
     */
    private MolodenskyTransform inverse;

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
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        final DefaultEllipsoid src = DefaultEllipsoid.castOrCopy(source);
        this.isSource3D = isSource3D;
        this.isTarget3D = isTarget3D;
        this.isAbridged = isAbridged;
        this.semiMajor  = src.getSemiMajorAxis();
        this.Δa         = src.semiMajorAxisDifference(target);
        this.tX         = tX;
        this.tY         = tY;
        this.tZ         = tZ;

        final double semiMinor = src.getSemiMinorAxis();
        final double Δf = src.flatteningDifference(target);
        eccentricitySquared = src.getEccentricitySquared();
        Δfmod = isAbridged ? (semiMajor * Δf) + (semiMajor - semiMinor) * (Δa / semiMajor)
                           : (semiMinor * Δf);
        /*
         * Copy parameters to the ContextualParameter. Those parameters are not used directly
         * by EllipsoidToCartesian, but we need to store them in case the user asks for them.
         * When both EPSG and OGC parameters exist for equivalent information, we use EPSG ones.
         */
        final Unit<Length> unit = src.getAxisUnit();
        final UnitConverter c = target.getAxisUnit().getConverterTo(unit);
        context = new ContextualParameters(isAbridged ? AbridgedMolodensky.PARAMETERS : Molodensky.PARAMETERS,
                                           isSource3D ? 4 : 3, isTarget3D ? 4 : 3);
        setEPSG(context, unit, Δf);
        context.getOrCreate(Molodensky.SRC_SEMI_MAJOR).setValue(semiMajor,   unit);
        context.getOrCreate(Molodensky.SRC_SEMI_MINOR).setValue(semiMinor,   unit);
        context.getOrCreate(Molodensky.TGT_SEMI_MAJOR).setValue(c.convert(target.getSemiMajorAxis()), unit);
        context.getOrCreate(Molodensky.TGT_SEMI_MINOR).setValue(c.convert(target.getSemiMinorAxis()), unit);
        /*
         * Prepare two affine transforms to be executed before and after this MolodenskyTransform:
         *
         *   - A "normalization" transform for converting degrees to radians,
         *   - A "denormalization" transform for for converting radians to degrees.
         */
        context.normalizeGeographicInputs(0);
        context.denormalizeGeographicOutputs(0);
    }

    /**
     * Sets the "dim" parameter and the EPSG parameters in the given group.
     * The OGC parameters other than "dim" are not set by this method.
     *
     * @param pg   Where to set the parameters.
     * @param unit The unit of measurement to declare.
     * @param Δf   The flattening difference to set.
     */
    private void setEPSG(final Parameters pg, final Unit<?> unit, final double Δf) {
        final int dim = getSourceDimensions();
        if (dim == getTargetDimensions()) {
            pg.getOrCreate(Molodensky.DIMENSION).setValue(dim);
        }
        pg.getOrCreate(Molodensky.TX)                    .setValue(tX, unit);
        pg.getOrCreate(Molodensky.TY)                    .setValue(tY, unit);
        pg.getOrCreate(Molodensky.TZ)                    .setValue(tZ, unit);
        pg.getOrCreate(Molodensky.AXIS_LENGTH_DIFFERENCE).setValue(Δa, unit);
        pg.getOrCreate(Molodensky.FLATTENING_DIFFERENCE) .setValue(Δf, Unit.ONE);
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
            tr.inverse = new MolodenskyTransform2D(target, source, -tX, -tY, -tZ, isAbridged);
        } else {
            tr = new MolodenskyTransform(source, isSource3D, target, isTarget3D, tX, tY, tZ, isAbridged);
            tr.inverse = new MolodenskyTransform(target, isTarget3D, source, isSource3D, -tX, -tY, -tZ, isAbridged);
        }
        tr.inverse.inverse = tr;
        return tr.context.completeTransform(factory, tr);
    }

    /**
     * Returns the unit of measurement of axis lengths. Current implementation arbitrarily
     * returns the units of {@link #Δa} (this may change in any future SIS version).
     */
    private Unit<?> getLinearUnit() {
        return context.getOrCreate(Molodensky.AXIS_LENGTH_DIFFERENCE).getUnit();
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
     * Returns a copy of internal parameter values of this {@code MolodenskyTransform}.
     * The returned group contains parameter values for the eccentricity and the shift among others.
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
        final Unit<?> unit = getLinearUnit();
        setEPSG(pg, unit, context.doubleValue(Molodensky.FLATTENING_DIFFERENCE));
        pg.getOrCreate(Molodensky.SRC_SEMI_MAJOR).setValue(semiMajor, unit);
        pg.getOrCreate(MapProjection.ECCENTRICITY).setValue(sqrt(eccentricitySquared));
        pg.parameter("abridged").setValue(isAbridged);
        return pg;
    }

    /**
     * Returns a description of the internal parameters of this {@code MolodenskyTransform} transform.
     * The returned group contains parameter descriptors for the number of dimensions and the eccentricity.
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
     * Gets the dimension of input points.
     *
     * @return The input dimension, which is 2 or 3.
     */
    @Override
    public final int getSourceDimensions() {
        return isSource3D ? 3 : 2;
    }

    /**
     * Gets the dimension of output points.
     *
     * @return The output dimension, which is 2 or 3.
     */
    @Override
    public final int getTargetDimensions() {
        return isTarget3D ? 3 : 2;
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
        final double ν2den = 1 - eccentricitySquared*sin2φ;                 // Square of the denominator of ν
        final double νden  = sqrt(ν2den);                                   // Denominator of ν
        final double ρden  = ν2den * νden;                                  // Denominator of ρ
        double ρ = semiMajor * (1 - eccentricitySquared) / ρden;            // Other notation: Rm = ρ
        double ν = semiMajor / νden;                                        // Other notation: Rn = ν
        double t = Δfmod * 2;                                               // A term in the calculation of Δφ
        if (!isAbridged) {
            ρ += h;
            ν += h;
            t = t*(0.5/νden + 0.5/ρden)                 // = Δf⋅[ν⋅(b/a) + ρ⋅(a/b)]     (without the +h in ν and ρ)
                    + Δa*eccentricitySquared/νden;      // = Δa⋅[ℯ²⋅ν/a]
        }
        final double spcλ = tY*sinλ + tX*cosλ;                      // "spc" stands for "sin plus cos"
        final double cmsλ = tY*cosλ - tX*sinλ;                      // "cms" stands for "cos minus sin"
        final double cmsφ = (tZ + t*sinφ)*cosφ - spcλ*sinφ;
        final double scaleX = ANGULAR_SCALE / (ν*cosφ);
        final double scaleY = ANGULAR_SCALE / ρ;
        if (dstPts != null) {
            dstPts[dstOff++] = λ + (cmsλ * scaleX);
            dstPts[dstOff++] = φ + (cmsφ * scaleY);
            if (isTarget3D) {
                double t1 = Δfmod * sin2φ;          // A term in the calculation of Δh
                double t2 = Δa;
                if (!isAbridged) {
                    t1 /= νden;                     // = Δf⋅(b/a)⋅ν⋅sin²φ
                    t2 *= νden;                     // = Δa⋅(a/ν)
                }
                dstPts[dstOff++] = h + spcλ*cosφ + tZ*sinφ + t1 - t2;
            }
        }
        if (!derivate) {
            return null;
        }
        /*
         * At this point the (Abridged) Molodensky transformation is finished.
         * Code below this point is only for computing the derivative, if requested.
         * Note: variable names do not necessarily tell all the terms that they contain.
         */
        final Matrix matrix   = Matrices.createDiagonal(getTargetDimensions(), getSourceDimensions());
        final double sinφcosφ = sinφ * cosφ;
        final double dν       = eccentricitySquared*sinφcosφ / ν2den;
        final double dν3ρ     = 3*dν * (1 - eccentricitySquared) / ν2den;
        //    double dXdλ     = spcλ;
        final double dYdλ     = cmsλ * sinφ;
        final double dZdλ     = cmsλ * cosφ;
              double dXdφ     = dYdλ / cosφ;
              double dYdφ     = -tZ*sinφ - cosφ*spcλ  +  t*(1 - 2*sin2φ);
              double dZdφ     =  tZ*cosφ - sinφ*spcλ;
        if (isAbridged) {
            /*
             *   Δfmod  =  (a⋅Δf) + (f⋅Δa)
             *   t      =  2⋅Δfmod
             *   dXdh   =  0  so no need to set the matrix element.
             *   dYdh   =  0  so no need to set the matrix element.
             */
            dXdφ -= cmsλ * dν;
            dYdφ -= cmsφ * dν3ρ;
            dZdφ += t*cosφ*sinφ;
        } else {
            /*
             *   Δfmod  =  b⋅Δf
             *   t      =  Δf⋅[ν⋅(b/a) + ρ⋅(a/b)]    (real ν and ρ, without + h)
             *   ν         is actually ν + h
             *   ρ         is actually ρ + h
             */
            final double dρ = dν3ρ * νden * (semiMajor / ρ);    // Reminder: that ρ contains a h term.
            dXdφ -= dν * cmsλ * semiMajor / (νden*ν);           // Reminder: that ν contains a h term.
            dYdφ -= dρ * dZdφ - (Δfmod*(dν*2/(1 - eccentricitySquared) + (1 + 1/ν2den)*(dν - dρ))
                                  + Δa*(dν + 1)*eccentricitySquared) * sinφcosφ / νden;
            if (isSource3D) {
                final double dXdh =  cmsλ / ν;
                final double dYdh = -cmsφ / ρ;
                matrix.setElement(0, 2, -dXdh * scaleX);
                matrix.setElement(1, 2, +dYdh * scaleY);
            }
            final double t1 = Δfmod * (dν*sin2φ + 2*sinφcosφ);
            final double t2 = Δa * dν;
            dZdφ += t1/νden + t2*νden;
        }
        matrix.setElement(0, 0, 1 - spcλ * scaleX);
        matrix.setElement(1, 1, 1 + dYdφ * scaleY);
        matrix.setElement(0, 1,   + dXdφ * scaleX);
        matrix.setElement(1, 0,   - dYdλ * scaleY);
        if (isTarget3D) {
            matrix.setElement(2, 0, dZdλ);
            matrix.setElement(2, 1, dZdφ);
        }
        return matrix;
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
                  double ρden  = 1 - eccentricitySquared * sin2φ;               // Denominator of ρ (completed later)
            final double νden  = sqrt(ρden);                                    // Denominator of ν
            double ρ = semiMajor * (1 - eccentricitySquared) / (ρden *= νden);  // (also complete calculation of ρden)
            double ν = semiMajor / νden;                                        // Other notation: Rm = ρ and Rn = ν
            double t = Δfmod * 2;                                               // A term in the calculation of Δφ
            if (!isAbridged) {
                ρ += h;
                ν += h;
                t = t*(0.5/νden + 0.5/ρden) + Δa*eccentricitySquared/νden;
            }
            final double spcλ = tY*sinλ + tX*cosλ;
            dstPts[dstOff++] = λ + ANGULAR_SCALE * (tY*cosλ - tX*sinλ) / (ν*cosφ);
            dstPts[dstOff++] = φ + ANGULAR_SCALE * ((t*cosφ - spcλ)*sinφ + tZ*cosφ) / ρ;
            if (isTarget3D) {
                t = Δfmod * sin2φ;
                double d = Δa;
                if (!isAbridged) {
                    t /= νden;
                    d *= νden;
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
     *       conversion between float and double will be handle by those LinearTransforms.   If nevertheless
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
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        int code = super.computeHashCode() + Numerics.hashCode(
                        Double.doubleToLongBits(Δa)
                +       Double.doubleToLongBits(Δfmod)
                + 31 * (Double.doubleToLongBits(tX)
                + 31 * (Double.doubleToLongBits(tY)
                + 31 * (Double.doubleToLongBits(tZ)))));
        if (isAbridged) code = ~code;
        return code;
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
            final MolodenskyTransform that = (MolodenskyTransform) object;
            return isSource3D == that.isSource3D
                && isTarget3D == that.isTarget3D
                && isAbridged == that.isAbridged
                && Numerics.equals(tX,                  that.tX)
                && Numerics.equals(tY,                  that.tY)
                && Numerics.equals(tZ,                  that.tZ)
                && Numerics.equals(Δa,                  that.Δa)
                && Numerics.equals(Δfmod,               that.Δfmod)
                && Numerics.equals(semiMajor,           that.semiMajor)
                && Numerics.equals(eccentricitySquared, that.eccentricitySquared);
        }
        return false;
    }
}
