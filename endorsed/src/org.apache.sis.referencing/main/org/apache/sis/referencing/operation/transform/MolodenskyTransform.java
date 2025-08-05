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
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.provider.Molodensky;
import org.apache.sis.referencing.operation.provider.AbridgedMolodensky;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.measure.Units;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Debug;
import org.apache.sis.util.privy.Numerics;


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
 * The transform expect coordinate values if the following order:
 * <ol>
 *   <li>longitudes (λ) relative to the prime meridian (usually Greenwich),</li>
 *   <li>latitudes (φ),</li>
 *   <li>optionally heights above the ellipsoid (h).</li>
 * </ol>
 *
 * The units of measurements depend on how the {@code MathTransform} has been created:
 * <ul>
 *   <li>{@code MolodenskyTransform} instances created directly by the constructor work with angular values in radians.</li>
 *   <li>Transforms created by the {@link #createGeodeticTransformation createGeodeticTransformation(…)} static method
 *       work with angular values in degrees and heights in the same units as the <strong>source</strong> ellipsoid
 *       axes (usually metres).</li>
 * </ul>
 *
 * <h2>Comparison of Molodensky and geocentric translation</h2>
 * Compared to the <q>Geocentric translation (geographic domain)</q> method,
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
 * @version 1.5
 * @since   0.7
 */
public class MolodenskyTransform extends DatumShiftTransform {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1741638055238886288L;

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
    static final double ANGULAR_SCALE = 1.00000000000391744;

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
    final double Δfmod;

    /**
     * Semi-major axis length (<var>a</var>) of the source ellipsoid.
     */
    protected final double semiMajor;

    /**
     * The square of eccentricity of the source ellipsoid.
     * This can be computed by ℯ² = (a²-b²)/a² where
     * <var>a</var> is the <i>semi-major</i> axis length and
     * <var>b</var> is the <i>semi-minor</i> axis length.
     *
     * @see DefaultEllipsoid#getEccentricitySquared()
     */
    protected final double eccentricitySquared;

    /**
     * The inverse of this Molodensky transform.
     * Shall be considered final.
     *
     * @see #inverse()
     */
    private MolodenskyTransform inverse;

    /**
     * Creates a Molodensky transform from the specified parameters.
     * This {@code MolodenskyTransform} class expects coordinate values in the following order and units:
     * <ol>
     *   <li>longitudes in <strong>radians</strong> relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in <strong>radians</strong>,</li>
     *   <li>optionally heights above the ellipsoid, in same units as the source ellipsoid axes.</li>
     * </ol>
     *
     * <h4>Unit conversions</h4>
     * For converting geographic coordinates in degrees, {@code MolodenskyTransform} instances
     * need to be concatenated with the following affine transforms:
     *
     * <ul>
     *   <li><i>Normalization</i> before {@code MolodenskyTransform}:<ul>
     *     <li>Conversion of (λ,φ) from degrees to radians.</li>
     *   </ul></li>
     *   <li><i>Denormalization</i> after {@code MolodenskyTransform}:<ul>
     *     <li>Conversion of (λ,φ) from radians to degrees.</li>
     *   </ul></li>
     * </ul>
     *
     * After {@code MolodenskyTransform} construction,
     * the full conversion chain including the above affine transforms can be created by
     * <code>{@linkplain #getContextualParameters()}.{@linkplain ContextualParameters#completeTransform
     * completeTransform}(factory, this)}</code>.
     *
     * @param source       the source ellipsoid.
     * @param isSource3D   {@code true} if the source coordinates have a height.
     * @param target       the target ellipsoid.
     * @param isTarget3D   {@code true} if the target coordinates have a height.
     * @param tX           the geocentric <var>X</var> translation in same units as the source ellipsoid axes.
     * @param tY           the geocentric <var>Y</var> translation in same units as the source ellipsoid axes.
     * @param tZ           the geocentric <var>Z</var> translation in same units as the source ellipsoid axes.
     * @param isAbridged   {@code true} for the abridged formula, or {@code false} for the complete one.
     *
     * @see #createGeodeticTransformation(MathTransformFactory, Ellipsoid, boolean, Ellipsoid, boolean, double, double, double, boolean)
     */
    @SuppressWarnings("this-escape")
    public MolodenskyTransform(final Ellipsoid source, final boolean isSource3D,
                               final Ellipsoid target, final boolean isTarget3D,
                               final double tX, final double tY, final double tZ,
                               final boolean isAbridged)
    {
        this(source, isSource3D, target, isTarget3D, tX, tY, tZ, null, isAbridged,
             isAbridged ? AbridgedMolodensky.PARAMETERS : Molodensky.PARAMETERS);

        if (!isSource3D && !isTarget3D) {
            if (isAbridged && tX == 0 && tY == 0 && tZ == 0) {
                inverse = new AbridgedMolodenskyTransform2D(this, source, target);
            } else {
                inverse = new MolodenskyTransform2D(this, source, target);
            }
        } else {
            inverse = new MolodenskyTransform(this, source, target);
        }
    }

    /**
     * Constructs the inverse of a Molodensky transform.
     *
     * @param inverse  the transform for which to create the inverse.
     * @param source   the source ellipsoid of the given {@code inverse} transform.
     * @param target   the target ellipsoid of the given {@code inverse} transform.
     */
    MolodenskyTransform(final MolodenskyTransform inverse, final Ellipsoid source, final Ellipsoid target) {
        this(target, inverse.isTarget3D, source, inverse.isSource3D,
             -inverse.tX, -inverse.tY, -inverse.tZ, inverse.grid,
             inverse.isAbridged, inverse.context.getDescriptor());

        this.inverse = inverse;
    }

    /**
     * Constructs a Molodensky transform with the same parameters as the given instance,
     * except the number of dimensions.
     *
     * @param other          the other instance to copy.
     * @param isSource3D     {@code true} if the new source coordinates shall have a height.
     * @param isTarget3D     {@code true} if the new target coordinates shall have a height.
     * @param createInverse  whether to invoke {@link #redimension} for creating the inverse.
     */
    private MolodenskyTransform(final MolodenskyTransform other,
            final boolean isSource3D, final boolean isTarget3D, final boolean createInverse)
    {
        super(other, isSource3D ? 3 : 2, isTarget3D ? 3 : 2);
        this.isSource3D = isSource3D;
        this.isTarget3D = isTarget3D;
        isAbridged = other.isAbridged;
        tX = other.tX;
        tY = other.tY;
        tZ = other.tZ;
        Δa = other.Δa;
        Δfmod = other.Δfmod;
        semiMajor = other.semiMajor;
        eccentricitySquared = other.eccentricitySquared;
        if (createInverse) {
            inverse = new MolodenskyTransform(other.inverse, isTarget3D, isSource3D, false);
            inverse.inverse = this;
        }
    }

    /**
     * Creates a Molodensky transform from the specified parameters.
     * If a non-null {@code grid} is specified, it is caller's responsibility to verify its validity.
     *
     * @param source      the source ellipsoid.
     * @param isSource3D  {@code true} if the source coordinates have a height.
     * @param target      the target ellipsoid.
     * @param isTarget3D  {@code true} if the target coordinates have a height.
     * @param tX          the geocentric <var>X</var> translation in same units as the source ellipsoid axes.
     * @param tY          the geocentric <var>Y</var> translation in same units as the source ellipsoid axes.
     * @param tZ          the geocentric <var>Z</var> translation in same units as the source ellipsoid axes.
     * @param grid        interpolation grid in geocentric coordinates, or {@code null} if none.
     * @param isAbridged  {@code true} for the abridged formula, or {@code false} for the complete one.
     * @param descriptor  the contextual parameter descriptor.
     */
    private MolodenskyTransform(final Ellipsoid source, final boolean isSource3D,
                                final Ellipsoid target, final boolean isTarget3D,
                                final double tX, final double tY, final double tZ,
                                final DatumShiftGrid<?,?> grid, final boolean isAbridged,
                                final ParameterDescriptorGroup descriptor)
    {
        super(descriptor, isSource3D, isTarget3D, grid);
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
         * by MolodenskyTransform, but we need to store them in case the user asks for them.
         */
        final Unit<Length> unit = src.getAxisUnit();
        setContextParameters(semiMajor, semiMinor, unit, target);
        completeParameters(context, unit, Δf);
        /*
         * Prepare two affine transforms to be executed before and after the MolodenskyTransform:
         *
         *   - A "normalization" transform for converting degrees to radians,
         *   - A "denormalization" transform for converting radians to degrees.
         */
        context.normalizeGeographicInputs(0);
        context.denormalizeGeographicOutputs(0);
    }

    /**
     * Sets parameter values in the given group for parameters other than axis lengths.
     * This method is invoked for both completing contextual parameters ({@code pg == context}) and
     * for completing internal parameters ({@code pg != context}). When this method is invoked, the
     * following parameters are already set:
     *
     * <ul>
     *   <li>{@code "src_semi_major"}</li>
     *   <li>{@code "src_semi_minor"}</li>
     *   <li>{@code "tgt_semi_major"} (contextual parameters only)</li>
     *   <li>{@code "tgt_semi_minor"} (contextual parameters only)</li>
     * </ul>
     *
     * This method sets the following parameters:
     *
     * <ul>
     *   <li><q>dim</q></li>
     *   <li><q>X-axis translation</q> (Molodensky only)</li>
     *   <li><q>Y-axis translation</q> (Molodensky only)</li>
     *   <li><q>Z-axis translation</q> (Molodensky only)</li>
     *   <li><q>Geocentric translation file</q> (Geocentric interpolations only)</li>
     *   <li><q>Semi-major axis length difference</q> (Always for Molodensky, internal WKT only for geocentric interpolations)</li>
     *   <li><q>Flattening difference</q> (Always for Molodensky, internal WKT only for geocentric interpolations)</li>
     * </ul>
     *
     * @param  pg    where to set the parameters.
     * @param  unit  the unit of measurement to declare.
     * @param  Δf    the flattening difference to set, or NaN if this method should fetch that value itself.
     */
    private void completeParameters(final Parameters pg, final Unit<?> unit, double Δf) {
        if (Double.isNaN(Δf)) {
            Δf = context.doubleValue(Molodensky.FLATTENING_DIFFERENCE);
        }
        /*
         * Unconditionally set the "dim" parameters to the number of source dimensions (do not check for consistency
         * with the number of target dimensions) because source dimensions determine the value of ellipsoidal heights,
         * which may change the horizontal numerical values. By contrast, the number of target dimensions does not have
         * any impact on numerical values (it can just causes a drop of the third value).
         */
        pg.getOrCreate(Molodensky.DIMENSION).setValue(getSourceDimensions());
        pg.getOrCreate(Molodensky.TX)                    .setValue(tX, unit);
        pg.getOrCreate(Molodensky.TY)                    .setValue(tY, unit);
        pg.getOrCreate(Molodensky.TZ)                    .setValue(tZ, unit);
        pg.getOrCreate(Molodensky.AXIS_LENGTH_DIFFERENCE).setValue(Δa, unit);
        pg.getOrCreate(Molodensky.FLATTENING_DIFFERENCE) .setValue(Δf, Units.UNITY);
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
     *   <li>optionally heights above the ellipsoid, in same units as the source ellipsoids axes.</li>
     * </ol>
     *
     * @param  factory      the factory to use for creating the transform.
     * @param  source       the source ellipsoid.
     * @param  isSource3D   {@code true} if the source coordinates have a height.
     * @param  target       the target ellipsoid.
     * @param  isTarget3D   {@code true} if the target coordinates have a height.
     * @param  tX           the geocentric <var>X</var> translation in same units as the source ellipsoid axes.
     * @param  tY           the geocentric <var>Y</var> translation in same units as the source ellipsoid axes.
     * @param  tZ           the geocentric <var>Z</var> translation in same units as the source ellipsoid axes.
     * @param  isAbridged   {@code true} for the abridged formula, or {@code false} for the complete one.
     * @return the transformation between geographic coordinates in degrees.
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
            if (isAbridged && tX == 0 && tY == 0 && tZ == 0) {
                tr = new AbridgedMolodenskyTransform2D(source, target);
            } else {
                tr = new MolodenskyTransform2D(source, target, tX, tY, tZ, isAbridged);
            }
        } else {
            tr = new MolodenskyTransform(source, isSource3D, target, isTarget3D, tX, tY, tZ, isAbridged);
        }
        tr.inverse.context.completeTransform(factory, null);
        return tr.context.completeTransform(factory, tr);
    }

    /**
     * If this transform expects three-dimensional inputs or outputs, and if the transform before or after
     * this one unconditionally sets the height to zero, replaces this transform by a two-dimensional one.
     *
     * @param  context  information about the neighbor transforms, and the object where to set the result.
     * @throws FactoryException if an error occurred while combining the transforms.
     *
     * @since 1.5
     */
    @Override
    protected void tryConcatenate(final TransformJoiner context) throws FactoryException {
        if (!(isSource3D && context.removeUnusedDimensions(-1, 2, 3, (d) -> (d == 2) ? new MolodenskyTransform(this, false, isTarget3D, true) : null)) &&
            !(isTarget3D && context.removeUnusedDimensions(+1, 2, 3, (d) -> (d == 2) ? new MolodenskyTransform(this, isSource3D, false, true) : null)))
        {
            super.tryConcatenate(context);
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
     * @return the input dimension, which is 2 or 3.
     */
    @Override
    public final int getSourceDimensions() {
        return isSource3D ? 3 : 2;
    }

    /**
     * Gets the dimension of output points.
     *
     * @return the output dimension, which is 2 or 3.
     */
    @Override
    public final int getTargetDimensions() {
        return isTarget3D ? 3 : 2;
    }

    /**
     * Transforms the (λ,φ) or (λ,φ,<var>h</var>) coordinates between two geographic CRS,
     * and optionally returns the derivative at that location.
     *
     * @param  dstPts    the array into which the transformed coordinate is returned, or {@code null}.
     * @param  srcOff    the offset to the point to be transformed in the source array.
     * @param  dstPts    the array into which the transformed coordinates is returned.
     * @param  dstOff    the offset to the location of the transformed point that is stored in the destination array.
     * @param  derivate  {@code true} for computing the derivative, or {@code false} if not needed.
     * @return {@inheritDoc}
     * @throws TransformException if the point cannot be transformed or
     *         if a problem occurred while calculating the derivative.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts,       int dstOff,
                            final boolean derivate) throws TransformException
    {
        final double λ = srcPts[srcOff];
        final double φ = srcPts[srcOff+1];
        final double h = isSource3D ? srcPts[srcOff+2] : 0;
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
        final double spcλ = tY*sinλ + tX*cosλ;                  // "spc" stands for "sin plus cos"
        final double cmsλ = tY*cosλ - tX*sinλ;                  // "cms" stands for "cos minus sin"
        final double cmsφ = (tZ + t*sinφ)*cosφ - spcλ*sinφ;
        final double scaleX = ANGULAR_SCALE / (ν*cosφ);
        final double scaleY = ANGULAR_SCALE / ρ;
        final double λt = λ + (cmsλ * scaleX);          // The target geographic coordinates
        final double φt = φ + (cmsφ * scaleY);
        if (dstPts != null) {
            dstPts[dstOff++] = λt;
            dstPts[dstOff++] = φt;
            if (isTarget3D) {
                double t1 = Δfmod * sin2φ;              // A term in the calculation of Δh
                double t2 = Δa;
                if (!isAbridged) {
                    t1 /= νden;                         // = Δf⋅(b/a)⋅ν⋅sin²φ
                    t2 *= νden;                         // = Δa⋅(a/ν)
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
     * This method performs the same transformation as {@link #transform(double[], int, double[], int, boolean)},
     * but the formulas are repeated here for performance reasons.
     *
     * @throws TransformException if a point cannot be transformed.
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
         * MolodenskyTransform.transform(λ, φ, h, …) method, but without derivative matrix
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
     * @return a Molodensky transform from the target ellipsoid to the source ellipsoid of this transform.
     */
    @Override
    public MathTransform inverse() {
        return inverse;
    }

    /**
     * Returns a description of the internal parameters of this {@code MolodenskyTransform} transform.
     * The returned group contains parameter descriptors for the number of dimensions and the eccentricity.
     *
     * <h4>Usage note</h4>
     * This method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters}.
     *
     * @return a description of the internal parameters.
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
     * Returns a copy of internal parameter values of this transform.
     * The returned group contains parameters for the source ellipsoid semi-axis lengths
     * and the differences between source and target ellipsoid parameters.
     *
     * <h4>Usage note</h4>
     * This method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters}.
     *
     * @return a copy of the internal parameter values for this transform.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        final Unit<?> unit = context.getOrCreate(Molodensky.SRC_SEMI_MAJOR).getUnit();
        final double semiMinor = context.getOrCreate(Molodensky.SRC_SEMI_MINOR).doubleValue(unit);
        final Parameters pg = Parameters.castOrWrap(getParameterDescriptors().createValue());
        pg.getOrCreate(Molodensky.SRC_SEMI_MAJOR).setValue(semiMajor, unit);
        pg.getOrCreate(Molodensky.SRC_SEMI_MINOR).setValue(semiMinor, unit);
        completeParameters(pg, unit, Double.NaN);
        return pg;
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    protected int computeHashCode() {
        int code = super.computeHashCode() + Long.hashCode(
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
     * @hidden because nothing new to said.
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
                && Numerics.epsilonEqual(tX,                  that.tX,                  mode)
                && Numerics.epsilonEqual(tY,                  that.tY,                  mode)
                && Numerics.epsilonEqual(tZ,                  that.tZ,                  mode)
                && Numerics.epsilonEqual(Δa,                  that.Δa,                  mode)
                && Numerics.epsilonEqual(Δfmod,               that.Δfmod,               mode)
                && Numerics.epsilonEqual(semiMajor,           that.semiMajor,           mode)
                && Numerics.epsilonEqual(eccentricitySquared, that.eccentricitySquared, mode);
                // No need to compare the contextual parameters since this is done by super-class.
        }
        return false;
    }
}
