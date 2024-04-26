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

import java.util.EnumMap;
import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.toRadians;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.privy.DoubleDouble;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.provider.Equirectangular;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;


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
    private static final long serialVersionUID = 6598912212024442670L;

    /**
     * Coefficients for the forward (f) and inverse (i) projection.
     * This is used in a trigonometric series with multiple angles.
     *
     * <h4>Missed optimization</h4>
     * We should replace the multiple angles by power polynomials using the Clenshaw summation algorithm.
     * However the {@code org.apache.sis.referencing.ClenshawSummation} class (in test packages) that we
     * used for this purpose in other map projections is limited to 6 terms, and we have 7 terms here.
     */
    private final double cf0, cf2, cf4, cf6, cf8, cf10, cf12, cf14,
                              ci2, ci4, ci6, ci8, ci10, ci12, ci14;

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
    @Workaround(library="JDK", version="1.7")
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
    @Workaround(library="JDK", version="1.7")
    private EquidistantCylindrical(final Initializer initializer) {
        super(initializer, null);
        final double φ1 = toRadians(initializer.getAndStore(Equirectangular.STANDARD_PARALLEL));
        final DoubleDouble sx = initializer.rν2(sin(φ1)).sqrt().multiply(cos(φ1), false);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(0, sx, null);

        // Coefficients for the forward projection.
        final double e2 = eccentricitySquared;
        final double e4 = e2*e2;
        final double e6 = e2*e4;
        final double e8 = e4*e4;
        cf0  = 1 - e2*(1./4 + e2*( 3./64  + e2*( 5./256  + e2*(175./16384  + e2*( 441./65536   + e2*(  4851./1048576 + e2*( 14157./4194304 )))))));
        cf2  =   - e2*(3./8 + e2*( 3./32  + e2*(45./1024 + e2*(105./4096   + e2*(2205./131072  + e2*(  6237./524288  + e2*(297297./33554432)))))));
        cf4  =                e4*(15./256 + e2*(45./1024 + e2*(525./16384  + e2*(1575./65536   + e2*(155925./8388608 + e2*(495495./33554432))))));
        cf6  =                            - e6*(35./3072 + e2*(175./12288  + e2*(3675./262144  + e2*( 13475./1048576 + e2*(385385./33554432)))));
        cf8  =                                           + e8*(315./131072 + e2*(2205./524288  + e2*( 43659./8388608 + e2*(189189./33554432))));
        cf10 =                                                        - (e4*e6)*( 693./1310720 + e2*(  6237./5242880 + e2*(297297./167772160)));
        cf12 =                                                                              (e6*e6)*(  1001./8388608 + e2*( 11011./33554432));
        cf14 =                                                                                                  - (e6*e8)*(  6435./234881024);

        // Coefficients for the inverse projection.
        final double n = initializer.axisLengthRatio().ratio_1m_1p().doubleValue();
        final double n2 = n*n;
        final double n3 = n*n2;
        final double n4 = n2*n2;
        ci2  = n*(3./2 + n2*(-27./32 + n2*(269./512   + n2*(  -6607./24576))));
        ci4  =           n2*( 21./16 + n2*( -55./32   + n2*(   6759./4096)));
        ci6  =           n3*(151./96 + n2*(-417./128  + n2*(  87963./20480)));
        ci8  =                         n4*(1097./512  + n2*( -15543./2560));
        ci10 =                    (n3*n2)*(8011./2560 + n2*( -69119./6144));
        ci12 =                                     (n3*n3)*( 293393./61440);
        ci14 =                                     (n3*n4)*(6845701./860160);
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
        if (dstPts != null) {
            dstPts[dstOff] = srcPts[srcOff];
            dstPts[dstOff+1] = cf14*sin(14*φ) + cf12*sin(12*φ) + cf10*sin(10*φ) + cf8*sin(8*φ)
                             +  cf6*sin( 6*φ)  + cf4*sin( 4*φ) +  cf2*sin( 2*φ) + cf0*φ;
        }
        if (!derivate) {
            return null;
        }
        final var derivative = new Matrix2();
        derivative.m11 = cf14*cos(14*φ)*14 + cf12*cos(12*φ)*12 + cf10*cos(10*φ)*10 + cf8*cos(8*φ)*8
                       +  cf6*cos( 6*φ)*6  +  cf4*cos( 4*φ)*4  +  cf2*cos( 2*φ)*2  + cf0;
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
        final double μ = srcPts[srcOff+1] / cf0;
        dstPts[dstOff] = srcPts[srcOff];
        dstPts[dstOff+1] = ci14*sin(14*μ) + ci12*sin(12*μ) + ci10*sin(10*μ) + ci8*sin(8*μ)
                         +  ci6*sin( 6*μ) +  ci4*sin( 4*μ) +  ci2*sin( 2*μ) + μ;
    }
}
