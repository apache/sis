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
import java.io.Serializable;
import static java.lang.Math.*;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.operation.provider.NorthPoleRotation;
import org.apache.sis.referencing.operation.provider.SouthPoleRotation;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Debug;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.matrix.Matrix2;


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
 */
public class PoleRotation extends AbstractMathTransform2D implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8355693495724373931L;

    /**
     * Index of parameter declared in {@link SouthPoleRotation} and {@link NorthPoleRotation}.
     *
     * @see #setValue(int, double)
     */
    private static final int POLE_LATITUDE = 0, POLE_LONGITUDE = 1, AXIS_ANGLE = 2;

    /**
     * The maximal value of axis rotation before switching to a different algorithm which will
     * reduce that rotation. The intent it to have axis rotation (applied on longitude values)
     * small enough for increasing the chances that output longitudes are in [-180 … 180]° range.
     */
    private static final double MAX_AXIS_ROTATION = 90;

    /**
     * The parameters used for creating this transform.
     * They are used for formatting <i>Well Known Text</i> (WKT).
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
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private MathTransform2D inverse;

    /**
     * Creates the inverse of the given forward operation. In principle, the latitude φ<sub>p</sub>
     * should be unchanged and the longitude λ<sub>p</sub> should be 180° (ignoring the axis angle)
     * in order to go back in the direction of geographical South pole. The longitudes computed by
     * this approach have an offset of 180°, which can be compensated with the axis angle (see the
     * {@link #inverseParameter(Parameters, ParameterValue)} method for more details).
     *
     * However, we can get a mathematically equivalent effect without the 180° longitude offset by
     * setting the new pole latitude to unrealistic φ<sub>p</sub> = (180° − φ<sub>forward</sub>) value.
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
     * Computes the value of the given parameter for the inverse of "South pole rotation".
     * This method is invoked for each parameter of the inverse transform to initialize.
     * The parameters of the inverse transform is defined as below:
     *
     * <ul>
     *   <li><b>Latitude</b> is unchanged. For example if the rotated pole was located at 60° of latitude
     *       relative to the geographic pole, then conversely the geographic pole is still located at 60°
     *       of latitude relative to the rotated pole.</li>
     *   <li><b>Longitude</b> is 180° (ignoring axis rotation) in the South pole case because by definition
     *       the 180° rotated meridian runs through both the geographical and the rotated South pole.</li>
     *   <li><b>Axis rotation</b> is 180° (ignoring λ<sub>p</sub> in forward transform) in the South pole
     *       case for compensating the 180° offset of λ<sub>p</sub> in the inverse transform.</li>
     *   <li>If a non-zero λ<sub>p</sub> was specified in the forward transform,
     *       then an axis rotation in opposite direction must be added to the inverse transform.
     *       Conversely if an axis rotation was defined in the forward transform,
     *       then a λ<sub>p</sub> rotation in opposite direction must be added to the inverse transform.</li>
     * </ul>
     *
     * @param  forward  the forward operation.
     * @param  target   parameter to initialize.
     * @return whether to accept the parameter (always {@code true}).
     *
     * @see #inverse()
     */
    private static boolean inverseParameter(final Parameters forward, final ParameterValue<?> target) {
        final ParameterDescriptorGroup descriptor = forward.getDescriptor();
        int i = descriptor.descriptors().indexOf(target.getDescriptor());
        if (i < 0) {
            return false;                               // Should never happen.
        }
        if (i != POLE_LATITUDE) {
            /*
             * For assigning a value to the "grid_south_pole_longitude" parameter at index 1,
             * we derive the value from the "grid_south_pole_angle" parameter at index 2.
             * And conversely.
             */
            i = (AXIS_ANGLE + POLE_LONGITUDE) - i;      // AXIS_ANGLE - (i - POLE_LONGITUDE)
        }
        Number value = getValue(forward, i);
        if (i != POLE_LATITUDE && SouthPoleRotation.PARAMETERS.equals(descriptor)) {
            double λp = value.doubleValue();
            value = copySign(180, λp) - λp;             // Negative of antipodal longitude.
        }
        target.setValue(value);
        return true;
    }

    /**
     * Returns the value for the parameter at the given index.
     * This is the converse of {@link #setValue(int, double)}.
     */
    private static Number getValue(final Parameters context, final int index) {
        return ((Number) ((ParameterValue<?>) context.values().get(index)).getValue());
    }

    /**
     * Sets the value of the parameter at the given index.
     * In the rotated south pole case, parameter 0 to 2 (inclusive) are:
     * {@code "grid_south_pole_latitude"},
     * {@code "grid_south_pole_longitude"} and
     * {@code "grid_south_pole_angle"} in that order.
     */
    private void setValue(final int index, final double value) {
        final var p = (ParameterDescriptor<?>) context.getDescriptor().descriptors().get(index);
        context.parameter(p.getName().getCode()).setValue(value);
    }

    /**
     * Creates the non-linear part of a rotated pole operation.
     * This transform does not include the conversion between degrees and radians and the longitude rotations.
     * For a complete transform, use one of the static factory methods.
     *
     * @param  south  {@code true} for a south pole rotation, or {@code false} for a north pole rotation.
     * @param  φp     geographic  latitude in degrees of the southern pole of the coordinate system.
     * @param  λp     geographic longitude in degrees of the southern pole of the coordinate system.
     * @param  θp     angle of rotation in degrees about the new polar axis measured clockwise when
     *                looking from the rotated pole to the Earth center.
     */
    protected PoleRotation(final boolean south, double φp, double λp, double θp) {
        context = new ContextualParameters(
                south ? SouthPoleRotation.PARAMETERS
                      : NorthPoleRotation.PARAMETERS, DIMENSION, DIMENSION);
        setValue(POLE_LATITUDE,  φp);       // grid_south_pole_latitude   or  grid_north_pole_latitude
        setValue(POLE_LONGITUDE, λp);       // grid_south_pole_longitude  or  grid_north_pole_longitude
        setValue(AXIS_ANGLE,     θp);       // grid_south_pole_angle      or  north_pole_grid_longitude
        if (south) {
            θp  = -θp;
        } else {
            φp  = -φp;
            λp -= copySign(180, λp);        // Antipodal point.
        }
        double sign = 1;
        if (abs(θp) > MAX_AXIS_ROTATION) {
            /*
             * Inverting the sign of sin(φp), cos(φp) and λ (in normalization matrix) will cause the formula to
             * compute the antipodal point, which allows us to remove 180° from `θp` and make it closer to zero.
             * Transform will produce final longitude results that are closer to the [-180 … +180]° range.
             */
            sign = -1;
            θp -= copySign(180, θp);
            context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION)  .convertAfter (0, -1, null);  // Invert λ sign.
            context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION).convertBefore(1, -1, null);  // Invert φ sign.
        }
        final double φ = toRadians(φp);
        sinφp = sin(φ) * sign;
        cosφp = cos(φ) * sign;
        context.normalizeGeographicInputs(λp);
        context.denormalizeGeographicOutputs(θp);
    }

    /**
     * Creates a new rotated south pole operation. The rotations are applied by first rotating the sphere
     * through λ<sub>p</sub> about the geographic polar axis, then rotating through (φ<sub>p</sub> − (−90°))
     * degrees so that the southern pole moved along the (previously rotated) Greenwich meridian, and finally
     * by rotating θ<sub>p</sub> degrees clockwise when looking from the southern to the northern rotated pole.
     * In the case where θ<sub>p</sub>=0, the 180° rotated meridian runs through both the geographical
     * and the rotated South pole.
     *
     * @param  factory  the factory to use for creating the transform.
     * @param  φp       geographic  latitude in degrees of the southern pole of the coordinate system.
     * @param  λp       geographic longitude in degrees of the southern pole of the coordinate system.
     * @param  θp       angle of rotation in degrees about the new polar axis measured clockwise when
     *                  looking from the southern to the northern pole.
     * @return the conversion doing a south pole rotation.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static MathTransform rotateSouthPole(final MathTransformFactory factory,
            final double φp, final double λp, final double θp) throws FactoryException
    {
        final var kernel = new PoleRotation(true, φp, λp, θp);
        return kernel.context.completeTransform(factory, kernel);
    }

    /**
     * Creates a new rotated north pole operation. The rotations are applied by first rotating the sphere
     * through λ<sub>p</sub> about the geographic polar axis, then rotating through (φ<sub>p</sub> − 90°)
     * degrees so that the northern pole moved along the (previously rotated) Greenwich meridian, and finally
     * by rotating θ<sub>p</sub> degrees clockwise when looking from the northern to the southern rotated pole.
     * In the case where θ<sub>p</sub>=0, the 0° rotated meridian is defined as the meridian that runs through
     * both the geographical and the rotated North pole.
     *
     * <div class="warning">
     * The sign of the {@code θp} argument is not yet well determined.
     * Should it be a rotation clockwise or anti-clockwise?
     * Looking from northern to southern pole or the opposite direction?
     * The sign may change in the future if we find an authoritative definition.
     * In the meantime, it is safer to keep the {@code θp} value equal to zero.
     * </div>
     *
     * @param  factory  the factory to use for creating the transform.
     * @param  φp       geographic  latitude in degrees of the northern pole of the coordinate system.
     * @param  λp       geographic longitude in degrees of the northern pole of the coordinate system.
     * @param  θp       angle of rotation in degrees about the new polar axis measured clockwise when
     *                  looking from the northern to the southern pole.
     * @return the conversion doing a north pole rotation.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static MathTransform rotateNorthPole(final MathTransformFactory factory,
            final double φp, final double λp, final double θp) throws FactoryException
    {
        final var kernel = new PoleRotation(false, φp, λp, θp);
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
        var name = new ImmutableIdentifier(Citations.SIS, Constants.SIS, "Rotated Latitude/longitude (radians domain)");
        return new DefaultParameterDescriptorGroup(Map.of(ParameterDescriptorGroup.NAME_KEY, name),
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
     * @return the parameter values for the <i>normalize</i> → {@code this} → <i>denormalize</i> chain of transforms.
     */
    @Override
    protected ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Transforms a single coordinate tuple in an array,
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
        if (dstPts != null) {
            dstPts[dstOff]   = atan2(y, xt);
            dstPts[dstOff+1] = asin(zt);
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
        final double r2  = xt*xt + y2;          // yt = y in ihis algorithm.
        final double zr  = sqrt(1 - zt*zt);
        final double dxφ = cosλ * zsinφp  +  cosφ * cosφp;
        final double dyφ = cosλ * zcosφp  -  cosφ * sinφp;
        return new Matrix2(
                (xt*x - y2*sinφp) / r2,     // ∂x/∂λ
               -(xt*z*sinλ + y*dxφ) / r2,   // ∂x/∂φ
                y*cosφp / zr,               // ∂y/∂λ
                dyφ / zr);                  // ∂y/∂φ
    }

    /**
     * Converts a list of coordinate tuples. This method performs the same calculation as above
     * {@link #transform(double[], int, double[], int, boolean)} method, but is overridden for efficiency.
     *
     * @throws TransformException if a point cannot be converted.
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
            dstPts[dstOff++] = atan2(y, xt);
            dstPts[dstOff++] = asin(zt);
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
            final PoleRotation simple = new PoleRotation(this);
            final ContextualParameters inverseParameters = simple.context;
            final double θp = inverseParameters.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION).getElement(0, 2);
            if (abs(θp) > MAX_AXIS_ROTATION) {
                /*
                 * If the θp value added to output longitude values is greater than 90°,
                 * create an alternative operation which will keep that value below 90°.
                 * The intent is to keep output λ closer to the [-180 … +180]° range.
                 */
                final PoleRotation alternative = new PoleRotation(
                        SouthPoleRotation.PARAMETERS.equals(inverseParameters.getDescriptor()),
                        getValue(inverseParameters, POLE_LATITUDE).doubleValue(),
                        getValue(inverseParameters, POLE_LONGITUDE).doubleValue(),
                        getValue(inverseParameters, AXIS_ANGLE).doubleValue());     // Not necessarily equals to θp.
                /*
                 * The caller of this method expects a chain of operations where a normalization is applied before
                 * the pole rotation, and a denormalization is applied after. Those expected (de)normalization are
                 * specified by `inverse.context`. But the actual normalization and denormalization needed by the
                 * alternative pole rotation are a little bit different. So we need to cancel the old normalization
                 * before to apply the new one, and to cancel the old denormalization after we applied to new one.
                 */
                final ContextualParameters actualParameters = alternative.context;
                inverse = MathTransforms.concatenate(
                        concatenate(inverseParameters, ContextualParameters.MatrixRole.INVERSE_NORMALIZATION,
                                    actualParameters,  ContextualParameters.MatrixRole.NORMALIZATION),
                        alternative,
                        concatenate(actualParameters,  ContextualParameters.MatrixRole.DENORMALIZATION,
                                    inverseParameters, ContextualParameters.MatrixRole.INVERSE_DENORMALIZATION));
            } else {
                inverse = simple;
            }
        }
        return inverse;
    }

    /**
     * Returns the concatenation of transform {@code p1.r1} followed by {@code p2.r2}.
     */
    private static MathTransform2D concatenate(
            final ContextualParameters p1, final ContextualParameters.MatrixRole r1,
            final ContextualParameters p2, final ContextualParameters.MatrixRole r2)
    {
        return (MathTransform2D) MathTransforms.linear(p2.getMatrix(r2).multiply(p1.getMatrix(r1)));
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
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            final var other = (PoleRotation) object;
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
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() + Double.hashCode(cosφp) + Double.hashCode(sinφp);
    }
}
