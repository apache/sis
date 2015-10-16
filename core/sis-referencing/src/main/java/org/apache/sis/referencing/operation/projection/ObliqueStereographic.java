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
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.util.FactoryException;

import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.Workaround;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.*;
import static org.apache.sis.internal.referencing.provider.ObliqueStereographic.*;


/**
 * <cite>Oblique Stereographic</cite> projection (EPSG code 9809).
 * The formulas used below are from the EPSG guide.
 *
 * <div class="section">References</div>
 * <ul>
 *   <li>{@code libproj4} is available at
 *       <a href = http://www.iogp.org/pubs/373-07-2.pdf>EPSG guide</a>.<br>
 *        Relevant files are: {@code PJ_sterea.c}, {@code pj_gauss.c},
 *        {@code pj_fwd.c}, {@code pj_inv.c} and {@code lib_proj.h}</li>
 * </ul>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
public class ObliqueStereographic extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1454098847621943639L;

    /**
     * Conformal latitude of origin only use
     * into {@link #inverseTransform(double[], int, double[], int) }.
     */
    private final double χ0;

    /**
     * Value of sin(χ0) only use
     * into {@link #transform(double[], int, double[], int, boolean)  }.
     *
     * @see #χ0
     */
    private final double sinχ0;

    /**
     * Value of cos(χ0) only use
     * into {@link #transform(double[], int, double[], int, boolean)  }.
     *
     * @see #χ0
     */
    private final double cosχ0;

    /**
     * c, internaly parameter used to define conformal sphere, used
     * into {@link #transform(double[], int, double[], int, boolean)  }
     * and {@link #inverseTransform(double[], int, double[], int) }.
     */
    private final double c;

    /**
     * n, internaly parameter used to define conformal sphere, used
     * into {@link #transform(double[], int, double[], int, boolean)  }
     * and {@link #inverseTransform(double[], int, double[], int) }.
     */
    private final double n;

    /**
     * g, internaly parameter used to define conformal sphere coordinate conversion,
     * during {@link #inverseTransform(double[], int, double[], int) }.
     * More precisely g is used to compute i and j parameters and i and j,
     * are used to compute only conformal longitude.
     */
    private final double g;

    /**
     * h, internaly parameter used to define conformal sphere coordinate conversion,
     * during {@link #inverseTransform(double[], int, double[], int) }.
     * More precisely h is used to compute i and j parameters and i and j,
     * are used to compute only conformal longitude.
     */
    private final double h;

    /**
     * A convenient computing for 1 - {@link #excentricitySquared}.
     */
    private final double eS1;

    /**
     * Creates a Oblique Stereographic projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Oblique Stereographic"</cite>.</li>
     * </ul>
     *
     * @param method     Description of the projection parameters.
     * @param parameters The parameter values of the projection to create.
     */
    public ObliqueStereographic(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.SCALE_FACTOR,     SCALE_FACTOR);
        roles.put(ParameterRole.FALSE_EASTING,    FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,   FALSE_NORTHING);
        return new Initializer(method, parameters, roles, (byte) 0);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    private ObliqueStereographic(final Initializer initializer) {
        super(initializer);

        eS1 = 1 - excentricitySquared;

        final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));

        final double cosφ0   = cos(φ0);
        final double cos4_φ0 = pow(cosφ0, 4);
        n = sqrt((1 + ((excentricitySquared * cos4_φ0) / eS1)));

        final double sinφ0  = sin(φ0);
        final double esinφ0 = excentricity * sinφ0;

        final double s1 = (1 +  sinφ0) / (1 -  sinφ0);
        final double s2 = (1 - esinφ0) / (1 + esinφ0);
        final double w1 = pow(s1 * pow(s2, excentricity), n);

        /*
         * Original formula : sinχ0 = ...
         * To avoid confusion with χ0 conformal latitude of origin,
         * renamed sinχ0 into sinχc.
         */
        final double sinχc = (w1 - 1) / (w1 + 1);
        c = (n + sinφ0) * (1 - sinχc) / ((n - sinφ0) * (1 + sinχc));

        //-- for invert formula
        final double w2 = c * w1;
        χ0 = asin((w2 - 1) / (w2 + 1));

        sinχ0 = sin(χ0);
        cosχ0 = cos(χ0);

        final double R = initializer.radiusOfConformalSphere(sinφ0);

        g = tan(PI / 4 - χ0 / 2);
        h = 2 * tan(χ0) + g;

        final MatrixSIS normalize   = context.getMatrix(true);
        final MatrixSIS denormalize = context.getMatrix(false);
        normalize.convertAfter(0, n, null);

        final double R2 = 2 * R;
        denormalize.convertBefore(0, R2, null);
        denormalize.convertBefore(1, R2, null);
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    ObliqueStereographic(final ObliqueStereographic other) {
        super(other);
        χ0    = other.χ0;
        sinχ0 = other.sinχ0;
        cosχ0 = other.cosχ0;
        c     = other.c;
        n     = other.n;
        g     = other.g;
        h     = other.h;
        eS1   = other.eS1;
    }

    /**
     * Returns the sequence of <cite>normalization</cite> → {@code this} → <cite>denormalization</cite> transforms
     * as a whole. The transform returned by this method expects (<var>longitude</var>, <var>latitude</var>)
     * coordinates in <em>degrees</em> and returns (<var>x</var>,<var>y</var>) coordinates in <em>metres</em>.
     *
     * <p>The non-linear part of the returned transform will be {@code this} transform, except if the ellipsoid
     * is spherical. In the later case, {@code this} transform will be replaced by a simplified implementation.</p>
     *
     * @param  factory The factory to use for creating the transform.
     * @return The map projection from (λ,φ) to (<var>x</var>,<var>y</var>) coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMapProjection(final MathTransformFactory factory) throws FactoryException {
        ObliqueStereographic kernel = this;
        if (excentricity == 0) {
//            kernel = new Spherical(this);     // not implemented yet
        }
        return context.completeTransform(factory, kernel);
    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
     *
     * @return The matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinate can not be converted.
     */
    @Override
    public Matrix transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, boolean derivate) throws ProjectionException {
        final double φ = srcPts[srcOff + 1];
        final double λ = srcPts[srcOff];

        final double sinφ      = sin(φ);
        final double esinφ     = excentricity * sinφ;
        final double v1_sinφ   = 1 - sinφ;
        final double v1esinφ   = 1 + esinφ;
        final double Sa        = (1 + sinφ)  / v1_sinφ;
        final double Sb        = (1 - esinφ) / v1esinφ;
        final double sbpowex   = pow(Sb, excentricity);
        final double sasbpowex = Sa * sbpowex;
        final double w         = c * pow(sasbpowex, n);
        final double w1        = w + 1;
        final double w1_w1     = (w - 1) / w1;
        /*
         * Sometimes to compute projection coordinates values, computing pass by a
         * "conformal sphere" to approximate as better, destination projection coordinates.
         */
        //-- latitude coordinate into conformal sphere space.
        final double χ    = asin(w1_w1);
        final double cosχ = cos(χ);
        final double sinχ = sin(χ);
        /*
         * Longitude coordinate into conformal sphere space is Λ = n(λ–ΛO)+ ΛO.
         * But in our case, all of this linears computing are delegate into
         * normalize matrix. See contructor for more precisions.
         * We work directly with λ.
         */
        final double cosλ = cos(λ);
        final double sinλ = sin(λ);
        /*
         * Now transform conformal sphere coordinates
         * into projection destination space
         */
        final double sinχsinχ0 = sinχ * sinχ0;
        final double cosχcosχ0 = cosχ * cosχ0;
        final double cosχsinλ  = cosχ * sinλ;

        final double B = 1 + sinχsinχ0 + cosχcosχ0 * cosλ;

        final double y = (sinχ * cosχ0 - cosχ * sinχ0 * cosλ) / B;
        final double x =  cosχsinλ / B;

        if (dstPts != null) {
            dstPts[dstOff  ] = x;
            dstPts[dstOff+1] = y;
        }

        if (!derivate) {
            return null;
        }

        final double cosφ   = cos(φ);
        final double B2     = B * B;

        //-- derivative code
        //-- dSa_dλ = 0;
        final double dSa_dφ = 2 * cosφ / (v1_sinφ * v1_sinφ);

        //-- dSb_dλ = 0;
        final double dSb_dφ = - 2 * excentricity * cosφ / (v1esinφ * v1esinφ);

        //-- dsasbpowex_dλ = 0;
        final double dsasbpowex_dφ = dSa_dφ * sbpowex + Sa * excentricity * dSb_dφ * pow(Sb, excentricity - 1);

        //-- dw_dλ = 0;
        final double dw_dφ = c * n * dsasbpowex_dφ * pow(sasbpowex, n - 1);

        //-- dχ_dλ = 0;
        final double dχ_dφ = dw_dφ / (w1 * sqrt(w));

        final double addsinχsinχ0 = sinχ + sinχ0;

        //-- Jacobian coefficients
        final double dx_dλ = cosχ * (cosλ * (1 + sinχsinχ0) + cosχcosχ0) / B2;

        final double dx_dφ = - dχ_dφ * sinλ * addsinχsinχ0 / B2;

        final double dy_dλ = cosχsinλ * addsinχsinχ0 / B2;

        final double dy_dφ = dχ_dφ * (cosχcosχ0 + cosλ * (sinχsinχ0 + 1)) / B2;

        return new Matrix2(dx_dλ, dx_dφ,
                           dy_dλ, dy_dφ);
    }

    /**
     * Transforms the specified (x, y) coordinates and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the point can not be converted.
     */
    @Override
    protected void inverseTransform(double[] srcPts, int srcOff, double[] dstPts, int dstOff) throws ProjectionException {
        final double x = srcPts[srcOff];
        final double y = srcPts[srcOff + 1];

        final double i = atan(x / (h + y));
        final double j = atan(x / (g - y)) - i;
        /*
         * Longitude coordinate into conformal sphere space is Λ = j + 2 * i
         * Where λ = Λ + Λ0, but Λ0 is added into normalize matrix which regroup all linears operations.
         * Also in our particularity case Geodetic longitude λ is the same.
         */
        final double λ    = j + 2*i;

        //-- latitude coordinate into conformal sphere space.
        final double χ    = χ0 + 2*atan((y - x*tan(j/2)));
        final double sinχ = sin(χ);

        final double ψ = log((1 + sinχ) / (c * (1 - sinχ))) / (2 * n);

        double φi_1 = 2*atan(exp(ψ)) - PI/2;

        for (int it = 0; it < MAXIMUM_ITERATIONS; it++) {
            final double sinφi_1  = sin(φi_1);
            final double esinφi_1 = excentricity*sinφi_1;

            double ψi_1 = log(tan(φi_1/2 + PI/4) * pow((1 - esinφi_1) / (1 + esinφi_1), excentricity / 2));

            final double φi = φi_1 - (ψi_1 - ψ) * cos(φi_1) * (1 - esinφi_1 * esinφi_1) / eS1;

            if (abs(φi - φi_1) <= ITERATION_TOLERANCE) {
                dstPts[dstOff]     = λ;
                dstPts[dstOff + 1] = φi;
                return;
            }
            φi_1 = φi;
        }
        throw new ProjectionException(Errors.Keys.NoConvergence);
    }
}
