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

import java.util.List;
import java.util.Collections;
import java.io.Serializable;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.internal.referencing.provider.NorthPoleRotation;
import org.apache.sis.internal.referencing.provider.SouthPoleRotation;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Debug;

import static java.lang.Math.*;
import static org.apache.sis.internal.referencing.Formulas.fastHypot;


/**
 * Computes latitudes and longitudes on a sphere where the south pole has been moved to given geographic coordinates.
 * The parameter values of this transform use the conventions defined in template 3.1 of GRIB2 format published by the
 * <a href="https://www.wmo.int/">World Meteorological Organization</a> (WMO):
 *
 * <ol>
 *   <li><b>φ<sub>p</sub>:</b> geographic  latitude in degrees of the southern pole of the coordinate system.</li>
 *   <li><b>λ<sub>p</sub>:</b> geographic longitude in degrees of the southern pole of the coordinate system.</li>
 *   <li>Angle of rotation in degrees about the new polar axis measured clockwise when looking from the rotated
 *       pole to the Earth center.</li>
 * </ol>
 *
 * The rotations are applied by first rotating the sphere through λ<sub>p</sub> about the geographic polar axis,
 * and then rotating through (φ<sub>p</sub> − (−90°)) degrees so that the southern pole moved along the
 * (previously rotated) Greenwich meridian.
 *
 * <h2>Coordinate order</h2>
 * Source and target axis order in {@code transform(…)} methods is (<var>longitude</var>, <var>latitude</var>).
 * This is the usual axis order used by Apache SIS for <em>internal</em> calculations
 * (but not the <em>parameter</em> order in factory methods).
 * If a different axis order is desired (for example for showing coordinates to the user),
 * an affine transform can be concatenated to this transform.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public class PoleRotation extends AbstractMathTransform2D implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8355693495724373931L;

    /**
     * The parameters used for creating this transform.
     * They are used for formatting <cite>Well Known Text</cite> (WKT).
     *
     * @see #getContextualParameters()
     */
    private final ContextualParameters context;

    /**
     * Sine and cosine of the geographic latitude of the southern pole of the coordinate system.
     * The rotation angle to apply is (φ<sub>p</sub> − (−90°)) degrees for the south pole (−90°),
     * but we use the following trigonometric identities:
     *
     * <p>For the south pole:</p>
     * <ul>
     *   <li>sin(φ + 90°) =  cos(φ)</li>
     *   <li>cos(φ + 90°) = −sin(φ)</li>
     * </ul>
     *
     * <p>For the north pole:</p>
     * <ul>
     *   <li>sin(φ − 90°) = −cos(φ)</li>
     *   <li>cos(φ − 90°) =  sin(φ)</li>
     * </ul>
     *
     * By convention those fields contain the sine and cosine for the south pole case,
     * and values with opposite sign for the north pole case.
     */
    private final double sinφp, cosφp;

    /**
     * The inverse of this operation, computed when first needed.
     *
     * @see #inverse()
     */
    private MathTransform2D inverse;

    /**
     * Creates the inverse of the given forward operation.
     * The new pole latitude is φ<sub>p</sub> = (180° − φ<sub>forward</sub>).
     * We get this effect be inverting the sign of {@link #cosφp} while keeping {@link #sinφp} unchanged.
     * Note that this is compatible with {@link #isIdentity()} implementation.
     *
     * @see #inverse()
     */
    private PoleRotation(final PoleRotation forward) {
        context =  forward.context.inverse(forward.context.getDescriptor(), PoleRotation::inverseParameter);
        sinφp   =  forward.sinφp;
        cosφp   = -forward.cosφp;
        inverse =  forward;
    }

    /**
     * Computes the value of the given parameter for the inverse operation.
     * This method is invoked for each parameter.
     *
     * @param  forward  the forward operation.
     * @param  target   parameter to initialize.
     * @return whether to accept the parameter (always {@code true}).
     */
    private static boolean inverseParameter(final Parameters forward, final ParameterValue<?> target) {
        final ParameterDescriptor<?> descriptor = target.getDescriptor();
        final List<GeneralParameterValue> values = forward.values();
        for (int i = values.size(); --i >= 0;) {
            if (descriptor.equals(values.get(i).getDescriptor())) {
                if (i != 0) {
                    /*
                     * For assigning a value to the "grid_south_pole_longitude" parameter at index 1,
                     * we derive the value from the "grid_south_pole_angle" parameter at index 2.
                     * And conversely.
                     */
                    i = 3 - i;
                }
                double value = ((Number) ((ParameterValue<?>) values.get(i)).getValue()).doubleValue();
                if (i == 0) {
                    value = IEEEremainder(180 - value, 360);
                } else if (SouthPoleRotation.PARAMETERS.equals(forward.getDescriptor())) {
                    value = -value;
                }
                target.setValue(value);
                return true;
            }
        }
        return false;       // Should never happen.
    }

    /**
     * Creates the non-linear part of a rotated pole operation.
     * This transform does not include the conversion between degrees and radians and the longitude rotations.
     * For a complete transform, use one of the static factory methods.
     *
     * @param  south  {@code true} for a south pole rotation, or {@code false} for a north pole rotation.
     * @param  φp     geographic  latitude in degrees of the southern pole of the coordinate system.
     * @param  λp     geographic longitude in degrees of the southern pole of the coordinate system.
     * @param  pa     angle of rotation in degrees about the new polar axis measured clockwise when
     *                looking from the rotated pole to the Earth center.
     */
    protected PoleRotation(final boolean south, final double φp, final double λp, final double pa) {
        context = new ContextualParameters(
                south ? SouthPoleRotation.PARAMETERS
                      : NorthPoleRotation.PARAMETERS, DIMENSION, DIMENSION);
        setValue(0, φp);        // grid_south_pole_latitude   or  grid_north_pole_latitude
        setValue(1, λp);        // grid_south_pole_longitude  or  grid_north_pole_longitude
        setValue(2, pa);        // grid_south_pole_angle      or  north_pole_grid_longitude
        final double φ = toRadians(φp);
        final double sign = south ? 1 : -1;
        sinφp = sin(φ) * sign;
        cosφp = cos(φ) * sign;
        context.normalizeGeographicInputs(λp);
        context.denormalizeGeographicOutputs(south ? -pa : pa);
    }

    /**
     * Sets the value of the parameter at the given index.
     * In the rotated south pole case, parameter 0 to 2 (inclusive) are:
     * {@code "grid_south_pole_latitude"},
     * {@code "grid_south_pole_longitude"} and
     * {@code "grid_south_pole_angle"} in that order.
     */
    private void setValue(final int index, final double value) {
        final ParameterDescriptor<?> p = (ParameterDescriptor<?>) context.getDescriptor().descriptors().get(index);
        context.parameter(p.getName().getCode()).setValue(value);
    }

    /**
     * Creates a new rotated south pole operation.
     *
     * @param  factory  the factory to use for creating the transform.
     * @param  φp       geographic  latitude in degrees of the southern pole of the coordinate system.
     * @param  λp       geographic longitude in degrees of the southern pole of the coordinate system.
     * @param  pa       angle of rotation in degrees about the new polar axis measured clockwise when
     *                  looking from the southern to the northern pole.
     * @return the conversion doing a south pole rotation.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static MathTransform rotateSouthPole(final MathTransformFactory factory,
            final double φp, final double λp, final double pa) throws FactoryException
    {
        final PoleRotation kernel = new PoleRotation(true, φp, λp, pa);
        return kernel.context.completeTransform(factory, kernel);
    }

    /**
     * Creates a new rotated north pole operation.
     *
     * @param  factory  the factory to use for creating the transform.
     * @param  φp       geographic  latitude in degrees of the northern pole of the coordinate system.
     * @param  λp       geographic longitude in degrees of the northern pole of the coordinate system.
     * @param  pa       angle of rotation in degrees about the new polar axis measured clockwise when
     *                  looking from the northern to the southern pole.
     * @return the conversion doing a north pole rotation.
     * @throws FactoryException if an error occurred while creating a transform.
     *
     * @todo Current implementation does not accept non-zero {@code pa} argument value,
     *       because we have not yet resolved an ambiguity about the sign of this parameter.
     *       Should it be a rotation clockwise or anti-clockwise? Looking from northern to
     *       southern pole or the opposite direction?
     */
    public static MathTransform rotateNorthPole(final MathTransformFactory factory,
            final double φp, final double λp, final double pa) throws FactoryException
    {
        if (pa != 0) {
            throw new IllegalArgumentException("Non-zero axis rotation not yet accepted.");
        }
        final PoleRotation kernel = new PoleRotation(false, φp, λp, pa);
        return kernel.context.completeTransform(factory, kernel);
    }

    /**
     * Returns a description of the parameters of this transform. The group of parameters contains only the grid
     * (north or south) pole latitude. It does not contain the grid pole longitude or the grid angle of rotation
     * because those parameters are handled by affine transforms pre- or post-concatenated to this transform.
     *
     * @return the parameter descriptors for this math transform.
     */
    @Debug
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        // We assume that it is not worth to cache this descriptor.
        ImmutableIdentifier name = new ImmutableIdentifier(Citations.SIS, Constants.SIS, "Rotated Latitude/longitude (radians domain)");
        return new DefaultParameterDescriptorGroup(Collections.singletonMap(ParameterDescriptorGroup.NAME_KEY, name),
                1, 1, (ParameterDescriptor<?>) context.getDescriptor().descriptors().get(0));
    }

    /**
     * Returns a copy of the parameter values of this transform.
     * The group contains the values of the parameters described by {@link #getParameterDescriptors()}.
     * This method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes};
     * most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters} instead.
     *
     * @return the parameter values for this math transform.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        final ParameterValueGroup values = getParameterDescriptors().createValue();
        values.values().add(context.values().get(0));           // First parameter is grid pole latitude.
        return values;
    }

    /**
     * Returns the parameters used for creating the complete operation. The returned group contains not only
     * the grid pole latitude (which is handled by this transform), but also the grid pole longitude and the
     * grid angle of rotation (which are handled by affine transforms before or after this transform).
     *
     * @return the parameter values for the sequence of <cite>normalize</cite> →
     *         {@code this} → <cite>denormalize</cite> transforms.
     */
    @Override
    protected ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Transforms a single coordinate point in an array,
     * and optionally computes the transform derivative at that location.
     * Source and target axis order is (<var>longitude</var>, <var>latitude</var>).
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        /*
         * Convert latitude and longitude coordinates to (x,y,z) Cartesian coordinates on a sphere of radius 1.
         * Note that the rotation around the Z axis has been performed in geographic coordinates by the affine
         * transform pre-concatenated to this transform, simply by subtracting λp from the longitude value.
         * This is simpler than performing the rotation in Cartesian coordinates.
         */
        final double λ    = srcPts[srcOff];
        final double φ    = srcPts[srcOff+1];
        final double z    = sin(φ);
        final double cosφ = cos(φ);
        final double sinλ = sin(λ);
        final double cosλ = cos(λ);
        final double x    = cosφ * cosλ;
        final double y    = cosφ * sinλ;
        final double y2   = y * y;
        /*
         * Apply the rotation around Y axis (so the y value stay unchanged)
         * and convert back to spherical coordinates.
         */
        final double xsinφp = x * sinφp;
        final double xcosφp = x * cosφp;
        final double zsinφp = z * sinφp;
        final double zcosφp = z * cosφp;
        final double xt  =  zcosφp - xsinφp;
        final double zt  = -xcosφp - zsinφp;
        final double r2  = xt*xt + y2;          // yt = y in ihis algorithm.
        final double r   = sqrt(r2);            // The slower hypot(…) is not needed because values are close to 1.
        if (dstPts != null) {
            dstPts[dstOff]   = atan2(y, xt);
            dstPts[dstOff+1] = atan2(zt, r);
        }
        if (!derivate) {
            return null;
        }
        /*
         * We used WxMaxima for a first derivation of formulas below,
         * then simplified the formulas by hand.
         *
         * https://svn.apache.org/repos/asf/sis/analysis/Rotated%20pole.wxmx
         */
        final double dxφ = cosλ * zsinφp  +  cosφ * cosφp;
        final double dyφ = cosλ * zcosφp  -  cosφ * sinφp;
        final double zλ  = z * sinλ;
        final double zr  = zt / r;
        final double rc  = r2 + zt*zt;
        return new Matrix2(
                (xt*x  - y2*sinφp)               / r2,      // ∂x/∂λ
               -(xt*zλ + y*dxφ)                  / r2,      // ∂x/∂φ
                (r*cosφp - zr*(x  + sinφp*xt))*y / rc,      // ∂y/∂λ
                (r*dyφ   + zr*(y*zλ - dxφ*xt))   / rc);     // ∂y/∂φ
    }

    /**
     * Converts a list of coordinate points. This method performs the same calculation than above
     * {@link #transform(double[], int, double[], int, boolean)} method, but is overridden for efficiency.
     *
     * @throws TransformException if a point can not be converted.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        if ((srcPts == dstPts && srcOff < dstOff) || getClass() != PoleRotation.class) {
            super.transform(srcPts, srcOff, dstPts, dstOff, numPts);
            return;
        }
        while (--numPts >= 0) {
            double λ    = srcPts[srcOff++];
            double φ    = srcPts[srcOff++];
            double z    = sin(φ);
            double cosφ = cos(φ);
            double y    = sin(λ) * cosφ;
            double x    = cos(λ) * cosφ;
            double xt   =  cosφp * z - sinφp * x;
            double zt   = -cosφp * x - sinφp * z;
            double r    = fastHypot(xt, y);
            dstPts[dstOff++] = atan2(y, xt);
            dstPts[dstOff++] = atan2(zt, r);
        }
    }

    /**
     * Returns the inverse transform of this object.
     *
     * @return the inverse of this transform.
     */
    @Override
    public synchronized MathTransform2D inverse() {
        if (inverse == null) {
            inverse = new PoleRotation(this);
        }
        return inverse;
    }

    /**
     * Tests whether this transform does not move any points.
     *
     * @return {@code true} if this transform is (at least approximately) the identity transform.
     */
    @Override
    public boolean isIdentity() {
        return sinφp == -1;
        /*
         * We do not test `cosφp` because that value can be small but not zero, because
         * there is no way to compute exactly `cos(π/2)` with `Math.PI` approximation.
         * Testing `sinφp == -1` is a way to allow for a small tolerance around π/2.
         * This policy is also needed for consistency with `PoleRotation(PoleRotation)` implementation.
         */
    }

    /**
     * Compares the specified object with this math transform for equality.
     *
     * @param  object  the object to compare with this transform.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if the given object is considered equals to this math transform.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            final PoleRotation other = (PoleRotation) object;
            if (mode.isApproximate()) {
                return Numerics.epsilonEqual(sinφp, other.sinφp, Formulas.ANGULAR_TOLERANCE * (PI/180)) &&
                       Numerics.epsilonEqual(cosφp, other.cosφp, Formulas.ANGULAR_TOLERANCE * (PI/180));
            } else {
                return Numerics.equals(sinφp, other.sinφp) &&
                       Numerics.equals(cosφp, other.cosφp);
            }
        }
        return false;
    }

    /**
     * Computes a hash value for this transform. This method is invoked by {@link #hashCode()} when first needed.
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() + Double.hashCode(cosφp) + Double.hashCode(sinφp);
    }
}
