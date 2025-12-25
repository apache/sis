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
package org.apache.sis.referencing.operation.projection;

import java.util.Map;
import java.util.EnumMap;
import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.toRadians;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.provider.Equirectangular;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.transform.TransformJoiner;


/**
 * <cite>Equidistant Cylindrical</cite> projection (EPSG code 1028).
 * This class implements only the ellipsoidal case. There is no specialization for the spherical case,
 * because the latter can be implemented by an affine transform instead of {@code NormalizedProjection}.
 *
 * <h4>Limitations</h4>
 * The trigonometric series used in this implementation is adequate for a flattening of 1/290 or less.
 * The series has not yet been optimized with Clenshaw summation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class EquidistantCylindrical extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8049874881401710767L;

    /**
     * Whether to allow the use of Clenshaw summation. This flag should be {@code true} for performance reason,
     * as it reduces the number of calls to trigonometric functions. A value of {@code false} causes this class
     * to use the formulas as published by EPSG guidance notes instead, which may be useful if we suspect a bug
     * in our application of Clenshaw summation.
     */
    @Debug
    private static final boolean ALLOW_TRIGONOMETRIC_IDENTITIES = true;

    /**
     * Coefficients for the forward (f) and inverse (i) projection.
     * This is used in a trigonometric series with multiple angles.
     */
    private final double c0,
            cf1, cf2, cf3, cf4, cf5, cf6, cf7,
            ci1, ci2, ci3, ci4, ci5, ci6, ci7;

    /**
     * Creates an Equidistant Cylindrical projection from the given parameters.
     *
     * @param method     description of the projection parameters.
     * @param parameters the parameter values of the projection to create.
     */
    public EquidistantCylindrical(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @SuppressWarnings("fallthrough")
    @Workaround(library="JDK", version="7", fixed="25")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.FALSE_EASTING,    Equirectangular.FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,   Equirectangular.FALSE_NORTHING);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, Equirectangular.LONGITUDE_OF_ORIGIN);
        return new Initializer(method, parameters, roles, null);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="7", fixed="25")
    private EquidistantCylindrical(final Initializer initializer) {
        super(initializer, null);
        final double φ1 = toRadians(initializer.getAndStore(Equirectangular.STANDARD_PARALLEL));
        final DoubleDouble sx = initializer.rν2(sin(φ1)).sqrt().multiply(cos(φ1), false);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(0, sx, null);
        /*
         * Coefficients for the forward projection. The formulas are available in two variants:
         * as published by EPSG guidance notes, or modified with Clenshaw summation for reducing
         * the number of calls to trigonometric functions. Coefficients for the modified form were
         * computed by `org.apache.sis.referencing.ClenshawSummation.equidistantCylindrical(boolean)`
         * in the test packages.
         */
        final double e2  = eccentricitySquared;
        final double e4  = e2*e2;
        final double e6  = e2*e4;
        final double e8  = e4*e4;
        final double e10 = e4*e6;
        final double e12 = e6*e6;
        final double e14 = e6*e8;
        if (ALLOW_TRIGONOMETRIC_IDENTITIES) {
            // Formulas modified with Clenshaw summation.
            c0  = ( -14157./ 4194304)*e14 + (-4851./1048576)*e12 + ( -441./ 65536)*e10 + (-175./16384)*e8 + ( -5./256)*e6 + (-3./ 64)*e4 + (-1./4)*e2 + 1;
            cf1 = (  16159./18350080)*e14 + (  -77./ 327680)*e12 + ( -273./ 81920)*e10 + ( -35./ 3072)*e8 + (-25./768)*e6 + (-3./ 32)*e4 + (-3./8)*e2;
            cf2 = (  75075./ 8388608)*e14 + (35805./2097152)*e12 + ( 4095./131072)*e10 + (1785./32768)*e8 + ( 45./512)*e6 + (15./128)*e4;
            cf3 = (-464893./18350080)*e14 + (-6083./ 163840)*e12 + (-2037./ 40960)*e10 + (-175./ 3072)*e8 + (-35./768)*e6;
            cf4 = ( 145145./ 4194304)*e14 + (39655./1048576)*e12 + ( 2205./ 65536)*e10 + ( 315./16384)*e8;
            cf5 = (-480051./18350080)*e14 + (-6237./ 327680)*e12 + ( -693./ 81920)*e10;
            cf6 = (  11011./ 1048576)*e14 + ( 1001./ 262144)*e12;
            cf7 = (  -6435./ 3670016)*e14;
        } else {
            // Formulas as published by EPSG guidance notes.
            c0  = 1 + (-1./4)*e2 + (-3./ 64)*e4 + ( -5./ 256)*e6 + (-175./ 16384)*e8 + ( -441./  65536)*e10 + ( -4851./1048576)*e12 + ( -14157./  4194304)*e14;
            cf1 =   + (-3./8)*e2 + (-3./ 32)*e4 + (-45./1024)*e6 + (-105./  4096)*e8 + (-2205./ 131072)*e10 + ( -6237./ 524288)*e12 + (-297297./ 33554432)*e14;
            cf2 =                  (15./256)*e4 + ( 45./1024)*e6 + ( 525./ 16384)*e8 + ( 1575./  65536)*e10 + (155925./8388608)*e12 + ( 495495./ 33554432)*e14;
            cf3 =                                 (-35./3072)*e6 + (-175./ 12288)*e8 + (-3675./ 262144)*e10 + (-13475./1048576)*e12 + (-385385./ 33554432)*e14;
            cf4 =                                                + ( 315./131072)*e8 + ( 2205./ 524288)*e10 + ( 43659./8388608)*e12 + ( 189189./ 33554432)*e14;
            cf5 =                                                                      ( -693./1310720)*e10 + ( -6237./5242880)*e12 + (-297297./167772160)*e14;
            cf6 =                                                                                             (  1001./8388608)*e12 + (  11011./ 33554432)*e14;
            cf7 =                                                                                                                     (  -6435./234881024)*e14;
        }
        /*
         * Coefficients for the inverse projection, available in two variants.
         * See the forward case above.
         */
        final double n1 = initializer.axisLengthRatio().ratio_1m_1p().doubleValue();
        final double n2 = n1*n1;
        final double n3 = n1*n2;
        final double n4 = n2*n2;
        final double n5 = n2*n3;
        final double n6 = n3*n3;
        final double n7 = n3*n4;
        if (ALLOW_TRIGONOMETRIC_IDENTITIES) {
            // Formulas modified with Clenshaw summation.
            ci1 = (-5112013./215040)*n7  +  (   553./ 80)*n5  +  (-29./12)*n3  +  (3./2)*n1;
            ci2 = (  143969./  2560)*n6  +  ( -1537./128)*n4  +  ( 21./ 8)*n2;
            ci3 = ( 3074943./  8960)*n7  +  (-32373./640)*n5  +  (151./24)*n3;
            ci4 = ( -386651./  1920)*n6  +  (  1097./ 64)*n4;
            ci5 = (-2927011./  3584)*n7  +  (  8011./160)*n5;
            ci6 = (  293393./  1920)*n6;
            ci7 = ( 6845701./ 13440)*n7;
        } else {
            // Formulas as published by EPSG guidance notes.
            ci1 = (3./2)*n1 + (-27./32)*n3 + ( 269./ 512)*n5 + (  -6607./ 24576)*n7;
            ci2 =             ( 21./16)*n2 + ( -55./  32)*n4 + (   6759./  4096)*n6;
            ci3 =             (151./96)*n3 + (-417./ 128)*n5 + (  87963./ 20480)*n7;
            ci4 =                            (1097./ 512)*n4 + ( -15543./  2560)*n6;
            ci5 =                            (8011./2560)*n5 + ( -69119./  6144)*n7;
            ci6 =                                              ( 293393./ 61440)*n6;
            ci7 =                                              (6845701./860160)*n7;
        }
    }

    /**
     * Returns the sequence of <i>normalization</i> → {@code this} → <i>denormalization</i> transforms as a whole.
     * The transform returned by this method expects (<var>longitude</var>, <var>latitude</var>) coordinates
     * in <em>degrees</em> and returns (<var>x</var>,<var>y</var>) coordinates in <em>metres</em>.
     * If the ellipsoid is spherical, this map projection is replaced by an affine transform.
     *
     * @param  parameters  parameters and the factory to use for creating the transform.
     * @return the map projection from (λ,φ) to (<var>x</var>,<var>y</var>) coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMapProjection(final MathTransformProvider.Context parameters) throws FactoryException {
        if (eccentricity == 0) {
            return Equirectangular.provider().createMathTransform(parameters);
        }
        return completeWithWraparound(parameters);
    }

    /**
     * Projects the specified (λ,φ) coordinates (units in radians) and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
     * The results must be multiplied by the denormalization matrix before to get linear distances.
     *
     * @return the matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinates cannot be converted.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws ProjectionException
    {
        final double φ = srcPts[srcOff+1];
        final double sinθ = sin(2*φ);
        final double cosθ = cos(2*φ);
        if (dstPts != null) {
            final double y;
            if (ALLOW_TRIGONOMETRIC_IDENTITIES) {
                y = ((((((cf7*cosθ + cf6)*cosθ + cf5)*cosθ + cf4)*cosθ + cf3)*cosθ + cf2)*cosθ + cf1)*sinθ + c0*φ;
            } else {
                y = cf7*sin(14*φ) + cf6*sin(12*φ) + cf5*sin(10*φ) + cf4*sin(8*φ)
                  + cf3*sin( 6*φ) + cf2*sin( 4*φ) + cf1*sinθ      + c0*φ;
            }
            dstPts[dstOff  ] = srcPts[srcOff];
            dstPts[dstOff+1] = y;
        }
        if (!derivate) {
            return null;
        }
        final var derivative = new Matrix2();
        final double d;
        if (ALLOW_TRIGONOMETRIC_IDENTITIES) {
            d = (((((( cf7*cosθ +   cf6)*cosθ +   cf5)*cosθ +   cf4)*cosθ +   cf3)*cosθ + cf2)*cosθ + cf1)*cosθ
              - (((((6*cf7*cosθ + 5*cf6)*cosθ + 4*cf5)*cosθ + 3*cf4)*cosθ + 2*cf3)*cosθ + cf2)*(sinθ*sinθ);
        } else {
            d = 7*cf7*cos(14*φ) + 6*cf6*cos(12*φ) + 5*cf5*cos(10*φ) + 4*cf4*cos(8*φ)
              + 3*cf3*cos( 6*φ) + 2*cf2*cos( 4*φ) +   cf1*cosθ;
        }
        derivative.m11 = 2*d + c0;
        return derivative;
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the point cannot be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double μ = srcPts[srcOff+1] / c0;
        final double sinθ = sin(2*μ);
        final double cosθ = cos(2*μ);
        final double y;
        if (ALLOW_TRIGONOMETRIC_IDENTITIES) {
            y = ((((((ci7*cosθ + ci6)*cosθ + ci5)*cosθ + ci4)*cosθ + ci3)*cosθ + ci2)*cosθ + ci1)*sinθ;
        } else {
            y = ci7*sin(14*μ) + ci6*sin(12*μ) + ci5*sin(10*μ) + ci4*sin(8*μ)
              + ci3*sin( 6*μ) + ci2*sin( 4*μ) + ci1*sinθ;
        }
        dstPts[dstOff  ] = srcPts[srcOff];
        dstPts[dstOff+1] = y + μ;
    }

    /**
     * Allows longitude conversions to be done before or after the map projection.
     * The conversion can be moved because the longitude value is not used in this projection.
     */
    @Override
    final boolean tryInverseConcatenate(TransformJoiner context) throws FactoryException {
        return context.replacePassThrough(Map.of(0, 0));
    }

    /**
     * Allows longitude conversions to be done before or after the map projection.
     * The conversion can be moved because the longitude value is not used in this projection.
     *
     * @param  context  information about the neighbor transforms, and the object where to set the result.
     * @throws FactoryException if an error occurred while combining the transforms.
     */
    @Override
    protected final void tryConcatenate(final TransformJoiner context) throws FactoryException {
        if (!tryInverseConcatenate(context)) {
            super.tryConcatenate(context);
        }
    }
}
