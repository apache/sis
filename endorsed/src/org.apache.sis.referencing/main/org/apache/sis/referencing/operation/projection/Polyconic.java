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
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.Workaround;
import static org.apache.sis.referencing.operation.provider.Polyconic.*;


/**
 * <cite>American Polyconic</cite> projection (EPSG codes 9818).
 * This projection has the following properties:
 * <ul>
 *   <li>Neither conformal nor equal-area.</li>
 *   <li>Parallels of latitude (except for Equator) are arcs of circles, but are not concentrics.</li>
 *   <li>Central Meridian and Equator are straight lines; all other meridians are complex curves.</li>
 *   <li>Scale is true along each parallel and along the central meridian, but no parallel is "standard".</li>
 *   <li>Free of distortion only along the central meridian.</li>
 * </ul>
 *
 * <h2>References</h2>
 * <ul>
 *   <li>John P. Snyder (Map Projections - A Working Manual,<br>
 *       U.S. Geological Survey Professional Paper 1395, 1987)</li>
 *   <li>"Coordinate Conversions and Transformations including Formulas",<br>
 *       EPSG Guidance Note Number 7.</li>
 * </ul>
 *
 * @author  Simon Reynard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 */
public class Polyconic extends MeridianArcBased {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -808283103170618880L;

    /**
     * Coefficients for reverse projection. Snyder 18-17 gives:
     *
     * <pre class="math">
     *     c₀ =    (-5/256 ⋅e⁶  +  -3/64 ⋅e⁴  +  -1/4⋅e²  +  1)
     *     c₂ = -2⋅(45/1024⋅e⁶  +   3/32 ⋅e⁴  +   3/8⋅e²)
     *     c₄ = +4⋅(45/1024⋅e⁶  +  15/256⋅e⁴)
     *     c₆ = -6⋅(35/3072⋅e⁶)
     *     M′  = c₀ + c₂cos(2φ) + c₄cos(4φ) + c₆cos(6φ)</pre>
     *
     * but using trigonometric identities we rewrite as:
     *
     * <pre class="math">
     *     c₀ =    1 - e²
     *     c₂ = - 3/2 ⋅e⁴  +   3/2⋅e²
     *     c₄ = -15/8 ⋅e⁶  +  15/8⋅e⁴
     *     c₆ =  35/16⋅e⁶
     *     M′  = c₀ + sin²φ⋅(c₂ + sin²φ⋅(c₄ + sin²φ⋅c₆))</pre>
     *
     * @see <a href="https://svn.apache.org/repos/asf/sis/analysis/Map%20projection%20formulas.ods">Coefficients derivation</a>
     */
    private final double ci2, ci4, ci6;

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="8", fixed="25")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, LONGITUDE_OF_ORIGIN);
        roles.put(ParameterRole.FALSE_EASTING,    FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,   FALSE_NORTHING);
        return new Initializer(method, parameters, roles, null);
    }

    /**
     * Creates a Polyconic projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>American Polyconic</q>.</li>
     * </ul>
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public Polyconic(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="8", fixed="25")
    private Polyconic(final Initializer initializer) {
        super(initializer);
        final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        final double e2 = eccentricitySquared;
        final double e4 = e2 * e2;
        final double e6 = e2 * e4;
        ci2 =  -3./2  * e4 +  3./2 * e2;
        ci4 = -15./8  * e6 + 15./8 * e4;
        ci6 =  35./16 * e6;
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(1, null, -distance(φ0, sin(φ0), cos(φ0)));
    }

    /**
     * Creates a new projection initialized to the same parameters as the given one.
     */
    Polyconic(final Polyconic other) {
        super(other);
        ci2 = other.ci2;
        ci4 = other.ci4;
        ci6 = other.ci6;
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
        Polyconic kernel = this;
        if (eccentricity == 0) {
            kernel = new Spherical(this);
        }
        return context.completeTransform(parameters.getFactory(), kernel);
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
        final double λ     = srcPts[srcOff  ];
        final double φ     = srcPts[srcOff+1];
        final double sinφ  = sin(φ);
        final double cosφ  = cos(φ);
        final double νcosφ = cosφ / sqrt(1 - eccentricitySquared*(sinφ*sinφ));
        final double νcotφ = νcosφ / sinφ;
        if (Double.isInfinite(νcotφ)) {                 // May happen if φ == 0.
            if (dstPts != null) {
                dstPts[dstOff  ] = λ;
                dstPts[dstOff+1] = φ;
            }
            return derivate ? new Matrix2() : null;
        }
        final double L    = λ*sinφ;                     // Named E in Snyder 18-2.
        final double sinL = sin(L);
        final double cosL = cos(L);
        if (dstPts != null) {
            dstPts[dstOff  ] = νcotφ * sinL;
            dstPts[dstOff+1] = νcotφ*(1 - cosL) + distance(φ, sinφ, cosφ);
        }
        if (!derivate) {
            return null;
        }
        final double cotφ = cosφ / sinφ;
        double dνcotφ_dφ = νcosφ * eccentricity;
        dνcotφ_dφ = (dνcotφ_dφ - 1) * (dνcotφ_dφ + 1) / cotφ - cotφ;
        final double dL_dφ = λ*cosφ;
        return new Matrix2(
                νcosφ * cosL,  νcotφ * (dL_dφ * cosL + dνcotφ_dφ * (  sinL)),
                νcosφ * sinL,  νcotφ * (dL_dφ * sinL + dνcotφ_dφ * (1-cosL)) + dM_dφ(sinφ*sinφ));
    }

    /**
     * Transforms the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the coordinates cannot be converted.
     */
    @Override
    protected void inverseTransform(double[] srcPts, int srcOff,
                                    double[] dstPts, int dstOff) throws ProjectionException
    {
        final double x = srcPts[srcOff  ];
        final double y = srcPts[srcOff+1];
        double φ = y;                           // A = (M₀ + (N-FE)/a)      — Snyder 18-18 with M₀=0, FE=0 and a=1.
        final double B = y*y + x*x;             // B = A² + ((E-FE)²/a²)    — Snyder 18-19 with FE=0 and a=1.
        final double ome = 1 - eccentricitySquared;
        int i = MAXIMUM_ITERATIONS;
        double dφ;
        do {
            if (--i < 0) {
                throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
            }
            final double cosφ  = cos(φ);
            final double sinφ  = sin(φ);
            final double sinφ2 = sinφ*sinφ;
            final double rν    = sqrt(1 - eccentricitySquared * sinφ2);
            final double C     = rν * sinφ/cosφ;
            final double M     = distance(φ, sinφ, cosφ);
            final double Mp    = ome + sinφ2*(ci2 + sinφ2*(ci4 + sinφ2*ci6));     // Derived from Snyder 18-17
            final double M2B   = M*M + B;
            final double sin2φ = sin(2*φ);
            /*
             * Following monster is Snyder 18-21 simplified with A=y and Ma=M.
             * This is also EPSG formula with A=y and J=M and H=Mp.
             *
             * Note for comparison purposes: the spherical formula is:
             *
             *   dφ = (y*(φ*tanφ + 1) - φ - 0.5*(φ*φ + B)*tanφ) / ((φ - y) / tanφ - 1)
             */
            dφ = (y*(M*C + 1) - M - 0.5*M2B*C) /
                        (eccentricitySquared*sin2φ*(M2B - 2*y*M)/(4*C) + (y - M)*(C*Mp - 2/sin2φ) - Mp);
            φ -= dφ;
        } while (abs(dφ) > ITERATION_TOLERANCE);
        final double sinφ = sin(φ);
        double λ = asin(x*tan(φ) * sqrt(1 - eccentricitySquared*(sinφ*sinφ))) / sinφ;
        /*
         * If y=0, then we got some 0/0 in above formula, either in calculation of φ or
         * in calculation of λ (which itself uses φ, so a NaN in φ will propagate in λ).
         * We test only after the loop because those cases should be rare, and we want a
         * continuous behavior for values close to zero but not close enough for causing
         * those divisions by zero.
         */
        if (!Double.isFinite(λ) && abs(y) <= ANGULAR_TOLERANCE && Double.isFinite(x)) {
            λ = x;
            φ = y;
        }
        dstPts[dstOff  ] = λ;
        dstPts[dstOff+1] = φ;
    }


    /**
     * Provides the transform equations for the spherical case of the Polyconic projection.
     *
     * @author  Simon Reynard (Geomatys)
     * @author  Martin Desruisseaux (Geomatys)
     */
    private static final class Spherical extends Polyconic {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 8500881467002808593L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param  other  the other projection (usually ellipsoidal) from which to copy the parameters.
         */
        Spherical(final Polyconic other) {
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
            final double λ    = srcPts[srcOff  ];
            final double φ    = srcPts[srcOff+1];
            final double sinφ = sin(φ);
            final double cosφ = cos(φ);
            final double cotφ = cosφ / sinφ;
            if (Double.isInfinite(cotφ)) {                  // May happen if φ == 0.
                if (dstPts != null) {
                    dstPts[dstOff  ] = λ;
                    dstPts[dstOff+1] = φ;
                }
                return derivate ? new Matrix2() : null;
            }
            final double E    = λ * sinφ;
            final double sinE = sin(E);
            final double cosE = cos(E);
            if (dstPts != null) {
                dstPts[dstOff  ] = sinE * cotφ;
                dstPts[dstOff+1] = φ + cotφ * (1 - cosE);
            }
            if (!derivate) {
                return null;
            }
            /*
             * Derivation of:
             *
             *   x = sin(λ * sin(φ)) * cotφ
             *   y = φ + cot(φ) * (1 - cos(λ * sin(φ)))
             */
            final double c2λ = cosφ*cosφ * λ;
            return new Matrix2(
                    cosφ * cosE,                                // ∂x/∂λ
                    (c2λ*cosE - sinE/sinφ)/sinφ,                // ∂x/∂λ
                    cosφ * sinE,                                // ∂y/∂λ
                    (c2λ*sinE - (1 - cosE)/sinφ)/sinφ + 1);     // ∂y/∂φ
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void inverseTransform(final double[] srcPts, final int srcOff,
                                        final double[] dstPts, final int dstOff)
                throws ProjectionException
        {
            final double x = srcPts[srcOff  ];
            final double y = srcPts[srcOff+1];
            double φ = y;                           // A = φ₀ + y/R         — Snyder 18-7 with φ₀=0 and R=1.
            final double B = x*x + y*y;             // B = x²/R² + A²       — Snyder 18-8 with A=φ and R=1.
            int i = MAXIMUM_ITERATIONS;
            double dφ;
            do {
                if (--i < 0) {
                    throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
                }
                final double tanφ = tan(φ);
                // Snyder 18-9 simplified with A=y and Ma=M.
                dφ = (y*(φ*tanφ + 1) - φ - 0.5*(φ*φ + B) * tanφ) / ((φ - y) / tanφ - 1);
                φ -= dφ;
            } while (abs(dφ) > ITERATION_TOLERANCE);
            double λ = asin(x*tan(φ)) / sin(φ);
            /*
             * If y=0, then we got some 0/0 in above formula, either in calculation of φ or
             * in calculation of λ (which itself uses φ, so a NaN in φ will propagate in λ).
             * We test only after the loop because those cases should be rare, and we want a
             * continuous behavior for values close to zero but not close enough for causing
             * those divisions by zero.
             */
            if (!Double.isFinite(λ) && abs(y) <= ANGULAR_TOLERANCE && Double.isFinite(x)) {
                λ = x;
                φ = y;
            }
            dstPts[dstOff  ] = λ;
            dstPts[dstOff+1] = φ;
        }
    }
}
