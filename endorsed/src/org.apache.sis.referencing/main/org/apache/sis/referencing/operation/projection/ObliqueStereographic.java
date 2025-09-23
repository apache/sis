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
import static java.lang.Math.*;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.provider.PolarStereographicA;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.util.Workaround;
import static org.apache.sis.referencing.internal.shared.Formulas.fastHypot;
import static org.apache.sis.referencing.operation.provider.ObliqueStereographic.*;


/**
 * <cite>Oblique Stereographic</cite> projection (EPSG code 9809).
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Stereographic_projection">Stereographic projection or Wikipedia</a></li>
 *   <li><a href="https://mathworld.wolfram.com/StereographicProjection.html">Stereographic projection or MathWorld</a></li>
 * </ul>
 *
 * <h2>Description</h2>
 * The directions starting from the central point are true, but the areas and the lengths become
 * increasingly deformed as one moves away from the center. This projection is frequently used
 * for mapping polar areas, but can also be used for other limited areas centered on a point.
 *
 * <p>This projection involves two steps: first a conversion of <em>geodetic</em> coordinates to <em>conformal</em>
 * coordinates (i.e. latitudes and longitudes on a conformal sphere), then a spherical stereographic projection.
 * For this reason this projection method is sometimes known as <q>Double Stereographic</q>.</p>
 *
 * <h2>Related methods</h2>
 * There is another method known as <q>Oblique Stereographic Alternative</q> or sometimes just
 * <q>Stereographic</q>. That alternative uses a simplified conversion computing the conformal latitude
 * of each point on the ellipsoid. Both methods are considered valid but produce slightly different results.
 * For this reason EPSG considers them as different projection methods.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Emmanuel Giasson (Thales)
 */
public class ObliqueStereographic extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1454098847621943639L;

    /**
     * Conformal latitude of origin (χ₀), together with its sine and cosine.
     * In the spherical case, χ₀ = φ₀ (the geodetic latitude of origin).
     */
    final double χ0, sinχ0, cosχ0;

    /**
     * Parameters used in the conformal sphere definition. Those parameters are used in both the
     * {@linkplain #transform(double[], int, double[], int, boolean) forward} and
     * {@linkplain #inverseTransform(double[], int, double[], int) inverse} projection.
     * If the user supplied ellipsoid is already a sphere, then those parameters are equal to 1.
     */
    private final double c, n;

    /**
     * Parameters used in the {@linkplain #inverseTransform(double[], int, double[], int) inverse} projection.
     * More precisely <var>g</var> and <var>h</var> are used to compute intermediate parameters <var>i</var>
     * and <var>j</var>, which are themselves used to compute conformal latitude and longitude.
     */
    private final double g, h;

    /**
     * Creates an Oblique Stereographic projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>Oblique Stereographic</q>, also known as <q>Roussilhe</q>.</li>
     * </ul>
     *
     * @param  method      description of the projection parameters.
     * @param  parameters  the parameter values of the projection to create.
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
        return new Initializer(method, parameters, roles, null);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    private ObliqueStereographic(final Initializer initializer) {
        super(initializer, null);
        final double φ0     = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        final double sinφ0  = sin(φ0);
        final double ℯsinφ0 = eccentricity * sinφ0;
        n = sqrt(1 + ((eccentricitySquared * pow(cos(φ0), 4)) / (1 - eccentricitySquared)));
        /*
         * Following variables use upper-case because they are written that way in the EPSG guide.
         */
        final double S1 = (1 +  sinφ0) / (1 -  sinφ0);
        final double S2 = (1 - ℯsinφ0) / (1 + ℯsinφ0);
        final double w1 = pow(S1 * pow(S2, eccentricity), n);
        /*
         * The χ₁ variable below was named χ₀ in the EPSG guide. We use the χ₁ name in order to avoid confusion with
         * the conformal latitude of origin, which is also named χ₀ in the EPSG guide. Mathematically, χ₀ and χ₁ are
         * computed in the same way except that χ₁ is computed with w₁ and χ₀ is computed with w₀.
         */
        final double sinχ1 = (w1 - 1) / (w1 + 1);
        c = ((n + sinφ0) * (1 - sinχ1)) /
            ((n - sinφ0) * (1 + sinχ1));
        /*
         * Convert the geodetic latitude of origin φ₀ to the conformal latitude of origin χ₀.
         */
        final double w2 = c * w1;
        sinχ0 = (w2 - 1) / (w2 + 1);
        χ0    = asin(sinχ0);
        cosχ0 = cos(χ0);
        /*
         * Following variables are used only by the reverse projection.
         */
        g = tan(PI/4 - χ0/2);
        h = 2*tan(χ0) + g;
        /*
         * One of the first steps performed by the stereographic projection is to multiply the longitude by n.
         * Since this is a linear operation, we can combine it with other linear operations performed by the
         * normalization matrix.
         */
        final MatrixSIS normalize   = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        normalize.convertAfter(0, n, null);
        /*
         * One of the last steps performed by the stereographic projection is to multiply the easting and northing
         * by 2 times the radius of the conformal sphere. Since this is a linear operation, we combine it with other
         * linear operations performed by the denormalization matrix.
         */
        final var R2 = initializer.radiusOfConformalSphere(sinφ0).scalb(1);
        denormalize.convertBefore(0, R2, null);
        denormalize.convertBefore(1, R2, null);
    }

    /**
     * Creates a new projection initialized to the same parameters as the given one.
     */
    ObliqueStereographic(final ObliqueStereographic other) {
        super(null, other);
        χ0    = other.χ0;
        sinχ0 = other.sinχ0;
        cosχ0 = other.cosχ0;
        c     = other.c;
        n     = other.n;
        g     = other.g;
        h     = other.h;
    }

    /**
     * Returns the names of additional internal parameters which need to be taken in account when
     * comparing two {@code ObliqueStereographic} projections or formatting them in debug mode.
     *
     * <p>We could report any of the internal parameters. But since they are all derived from φ₀ and
     * the {@linkplain #eccentricity eccentricity} and since the eccentricity is already reported by
     * the super-class, we report only χ₀ as a representative of the internal parameters.</p>
     */
    @Override
    final String[] getInternalParameterNames() {
        return new String[] {"χ₀"};
    }

    /**
     * Returns the values of additional internal parameters which need to be taken in account when
     * comparing two {@code ObliqueStereographic} projections or formatting them in debug mode.
     */
    @Override
    final double[] getInternalParameterValues() {
        return new double[] {χ0};
    }

    /**
     * Returns the sequence of <i>normalization</i> → {@code this} → <i>denormalization</i> transforms as a whole.
     * The transform returned by this method expects (<var>longitude</var>, <var>latitude</var>) coordinates
     * in <em>degrees</em> and returns (<var>x</var>,<var>y</var>) coordinates in <em>metres</em>.
     * The non-linear part of the returned transform will be {@code this} transform, except if the ellipsoid
     * is spherical. In the latter case, {@code this} transform is replaced by a simplified implementation.
     *
     * @param  parameters  parameters and the factory to use for creating the transform.
     * @return the map projection from (λ,φ) to (<var>x</var>,<var>y</var>) coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMapProjection(final MathTransformProvider.Context parameters) throws FactoryException {
        if (Double.isNaN(χ0)) {
            /*
             * The oblique stereographic formulas cannot handle the polar case.
             * If the user gave us a latitude of origin of ±90°, delegate to the
             * polar stereographic which is designed especially for those cases.
             *
             * Note: the GeoTIFF parameter name and code for the central meridian is different.
             * But the delegation should still work if EPSG, ESRI or OGC names are also present.
             * It should be the case if the parameter descriptor group was provided by SIS.
             */
            final Double φ0 = context.getValue(LATITUDE_OF_ORIGIN);
            if (φ0 != null && abs(abs(φ0) - 90) < Formulas.ANGULAR_TOLERANCE) {
                return PolarStereographicA.provider().createMathTransform(parameters);
            }
        }
        if (eccentricity == 0) {
            return context.completeTransform(parameters.getFactory(), new Spherical(this));
        }
        return completeWithWraparound(parameters);
    }

    /**
     * Projects the specified (Λ,φ) coordinates and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
     * The units of measurement are implementation-specific (see super-class javadoc).
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
        final double Λ     = srcPts[srcOff  ];      // Λ = λ⋅n  (see below), ignoring longitude of origin.
        final double φ     = srcPts[srcOff+1];
        final double sinφ  = sin(φ);
        final double ℯsinφ = eccentricity * sinφ;
        final double Sa    = (1 +  sinφ) / (1 -  sinφ);
        final double Sb    = (1 - ℯsinφ) / (1 + ℯsinφ);
        final double w     = c * pow(Sa * pow(Sb, eccentricity), n);
        /*
         * Convert the geodetic coordinates (φ,λ) to conformal coordinates (χ,Λ) before to apply the
         * actual stereographic projection.  The geodetic and conformal coordinates will be the same
         * if the ellipsoid is already a sphere.  The original formulas were:
         *
         *    χ = asin((w - 1) / (w + 1))
         *
         * But since the projection needs only sin(χ) and cos(χ), we avoid the costly asin(…) function.
         */
        final double sinχ = (w - 1) / (w + 1);
        final double cosχ = sqrt(1 - sinχ*sinχ);
        /*
         * The conformal longitude is  Λ = n⋅(λ - λ₀) + Λ₀  where λ is the geodetic longitude.
         * But in Apache SIS implementation, the multiplication by  n  has been merged in the
         * constructor with other linear operations performed by the "normalization" matrix.
         * Consequently, the value obtained at srcPts[srcOff] is already Λ - Λ₀, not λ - λ₀.
         */
        final double cosΛ = cos(Λ);
        final double sinΛ = sin(Λ);
        /*
         * Now apply the stereographic projection on the conformal sphere using the (χ,Λ) coordinates.
         * Note that the formulas below are the same as the formulas in the Spherical inner class.
         * The only significant difference is that the spherical case does not contain all the above
         * code which converted (φ,λ) into (χ,Λ).
         */
        final double sinχsinχ0 = sinχ * sinχ0;
        final double cosχcosχ0 = cosχ * cosχ0;
        final double B = 1 + sinχsinχ0 + cosχcosχ0*cosΛ;
        if (dstPts != null) {
            dstPts[dstOff  ] = cosχ*sinΛ / B;                           // Easting (x)
            dstPts[dstOff+1] = (sinχ*cosχ0 - cosχ*sinχ0*cosΛ) / B;      // Northing (y)
        }
        if (!derivate) {
            return null;
        }
        /*
         * Now compute the derivative, if the user asked for it.
         * Notes:
         *
         *     ∂Sa/∂λ = 0
         *     ∂Sb/∂λ = 0
         *      ∂w/∂λ = 0
         *      ∂χ/∂λ = 0
         *     ∂Sa/∂φ =  2⋅cosφ   / (1 -  sinφ)²
         *     ∂Sb/∂φ = -2⋅ℯ⋅cosφ / (1 - ℯ⋅sinφ)²
         *      ∂w/∂φ =  2⋅n⋅w⋅(1/cosφ - ℯ²⋅cosφ/(1 - ℯ²⋅sin²φ));
         */
        final double cosφ = cos(φ);
        final double dχ_dφ = (1/cosφ - cosφ*eccentricitySquared/(1 - ℯsinφ*ℯsinφ)) * 2*n*sqrt(w) / (w + 1);
        /*
         * Above ∂χ/∂φ is equal to 1 in the spherical case.
         * Remaining formulas below are the same as in the spherical case.
         */
        final double B2 = B * B;
        final double d = (cosχcosχ0 + cosΛ * (sinχsinχ0 + 1)) / B2;     // Matrix diagonal
        final double t = sinΛ * (sinχ + sinχ0) / B2;                    // Matrix anti-diagonal
        /*                   ┌              ┐
         *                   │ ∂x/∂λ, ∂x/∂φ │
         * Jacobian        = │              │
         *    (Proj(λ,φ))    │ ∂y/∂λ, ∂y/∂φ │
         *                   └              ┘
         */
        return new Matrix2(d*cosχ,  -t*dχ_dφ,
                           t*cosχ,   d*dχ_dφ);
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates and stores the result in {@code dstPts}
     * (angles in radians).
     *
     * @throws ProjectionException if the point cannot be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double x = srcPts[srcOff  ];
        final double y = srcPts[srcOff+1];
        final double i = atan2(x, h + y);
        final double j = atan2(x, g - y) - i;
        /*
         * The conformal longitude is  Λ = j + 2i + Λ₀.  In the particular case of stereographic projection,
         * the geodetic longitude λ is equal to Λ. Furthermore, in Apache SIS implementation, Λ₀ is added by
         * the denormalization matrix and shall not be handled here. The only remaining part is λ = j + 2i.
         */
        final double λ = j + 2*i;
        /*
         * Calculation of geodetic latitude φ involves first the calculation of conformal latitude χ,
         * then calculation of isometric latitude ψ, and finally calculation of φ by an iterative method.
         */
        final double sinχ = sin(χ0 + 2*atan(y - x*tan(j/2)));
        final double ψ = log((1 + sinχ) / ((1 - sinχ)*c)) / (2*n);
        double φ = 2*atan(exp(ψ)) - PI/2;                               // First approximation
        final double he = eccentricity/2;
        final double ome = 1 - eccentricitySquared;
        for (int it=0; it<MAXIMUM_ITERATIONS; it++) {
            final double ℯsinφ = eccentricity * sin(φ);
            final double ψi = log(tan(φ/2 + PI/4) * pow((1 - ℯsinφ) / (1 + ℯsinφ), he));
            final double Δφ = (ψ - ψi) * cos(φ) * (1 - ℯsinφ*ℯsinφ) / ome;
            φ += Δφ;
            if (!(abs(Δφ) > ITERATION_TOLERANCE)) {     // Use `!` for accepting NaN.
                dstPts[dstOff  ] = λ;
                dstPts[dstOff+1] = φ;
                return;
            }
        }
        throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
    }




    /**
     * Provides the transform equations for the spherical case of the Oblique Stereographic projection.
     * This implementation can be used when {@link #eccentricity} = 0.
     *
     * @author  Rémi Maréchal (Geomatys)
     * @author  Martin Desruisseaux (Geomatys)
     */
    static final class Spherical extends ObliqueStereographic {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -1454098847621943639L;

        /**
         * Creates a new projection initialized to the same parameters as the given one.
         */
        protected Spherical(ObliqueStereographic other) {
            super(other);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final boolean derivate)
        {
            // No need to enforce [−n⋅π … n⋅π] range here because n=1.
            final double λ = srcPts[srcOff  ];
            final double φ = srcPts[srcOff+1];
            /*
             * Formulas below are the same as the elliptical formulas after the geodetic coordinates
             * have been converted to conformal coordinates.  In this spherical case we do not need to
             * perform such conversion. Instead, we have directly   χ = φ  and  Λ = λ.  The simplified
             * EPSG formulas then become the same as Snyder formulas for the spherical case.
             */
            final double sinφ      = sin(φ);
            final double cosφ      = cos(φ);
            final double sinλ      = sin(λ);
            final double cosλ      = cos(λ);
            final double sinφsinφ0 = sinφ * sinχ0;
            final double cosφcosφ0 = cosφ * cosχ0;
            final double cosφsinλ  = cosφ * sinλ;
            final double B = 1 + sinφsinφ0 + cosφcosφ0*cosλ;                    // Snyder 21-4
            if (dstPts != null) {
                dstPts[dstOff  ] = cosφsinλ / B;                                // Snyder 21-2
                dstPts[dstOff+1] = (sinφ*cosχ0 - cosφ*sinχ0*cosλ) / B;          // Snyder 21-3
            }
            if (!derivate) {
                return null;
            }
            final double B2 = B * B;
            final double d = (cosφcosφ0 + cosλ * (sinφsinφ0 + 1)) / B2;
            final double t = sinλ * (sinφ + sinχ0) / B2;
            return new Matrix2(d*cosφ, -t,
                               t*cosφ,  d);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void inverseTransform(final double[] srcPts, final int srcOff,
                                        final double[] dstPts, final int dstOff)
        {
            final double x = srcPts[srcOff  ];
            final double y = srcPts[srcOff+1];
            final double ρ = fastHypot(x, y);
            final double λ, φ;
            if (ρ == 0) {         // Exact comparison is okay here. Values > 0 (even tiny) work with complete formula.
                λ = 0.0;
                φ = χ0;
            } else {
                final double c    = 2*atan(ρ);
                final double cosc = cos(c);
                final double sinc = sin(c);
                λ = atan2(x * sinc,
                          cosc*cosχ0*ρ - y*sinc*sinχ0  );
                φ = asin( cosc*sinχ0   + y*sinc*cosχ0/ρ);
            }
            dstPts[dstOff]   = λ;
            dstPts[dstOff+1] = φ;
        }
    }
}
