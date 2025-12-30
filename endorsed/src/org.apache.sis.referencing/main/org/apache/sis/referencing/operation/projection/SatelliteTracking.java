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
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.measure.Latitude;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.resources.Errors;
import static org.apache.sis.referencing.operation.provider.SatelliteTracking.*;


/**
 * <cite>Satellite-Tracking</cite> projection.
 * This projection has been developed in 1977 by Snyder and has no associated EPSG code.
 * This projection is neither conformal or equal-area, but has the property that ground tracks
 * for satellites orbiting the Earth with the same orbital parameters are shown as straight lines
 * on the map. Other properties are (Snyder 1987):
 *
 * <ul>
 *   <li>All meridians are equally spaced straight lines.
 *       They are parallel on cylindrical form and converging to a common point on conical form.</li>
 *   <li>All parallels are straight but unequally spaced.
 *       They are parallel on cylindrical form and are concentric circular arcs on conical form.</li>
 *   <li>Conformality occurs along two chosen parallels. Scale is correct along one of these parameters
 *       on the conical form and along both on the cylindrical form.</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * This map projection supports only circular orbits. The Earth is assumed spherical.
 * Areas close to poles cannot be mapped.
 *
 * <h2>References</h2>
 * John P. Snyder., 1987. <u>Map Projections - A Working Manual</u>
 * chapter 28: <cite>Satellite-tracking projections</cite>.
 *
 * @author  Matthieu Bastianelli (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class SatelliteTracking extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 859940667477896653L;

    /**
     * Sines and cosines of inclination between the plane of the Earth's Equator and the plane
     * of the satellite orbit. The angle variable name is <var>i</var> in Snyder's book.
     *
     * @see org.apache.sis.referencing.operation.provider.SatelliteTracking#SATELLITE_ORBIT_INCLINATION
     */
    private final double cos_i, sin_i, cos2_i;

    /**
     * Ratio of satellite orbital period (P₂) over ascending node period (P₁).
     *
     * @see org.apache.sis.referencing.operation.provider.SatelliteTracking#SATELLITE_ORBITAL_PERIOD
     * @see org.apache.sis.referencing.operation.provider.SatelliteTracking#ASCENDING_NODE_PERIOD
     */
    private final double p2_on_p1;

    /**
     * Coefficients for the Conic Satellite-Tracking Projection.
     * Those values are {@link Double#NaN} in the cylindrical case.
     */
    private final double n, s0;

    /**
     * {@code true} if this projection is conic, or {@code false} if cylindrical or unknown.
     */
    private final boolean isConic;

    /**
     * Work around for RFE #4093999 in Sun's bug database ("Relax constraint on
     * placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="8", fixed="25")
    static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<NormalizedProjection.ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(NormalizedProjection.ParameterRole.class);
        roles.put(NormalizedProjection.ParameterRole.CENTRAL_MERIDIAN, CENTRAL_MERIDIAN);
        roles.put(ParameterRole.LATITUDE_OF_CONFORMAL_SPHERE_RADIUS, LATITUDE_OF_ORIGIN);
        return new Initializer(method, parameters, roles, null);
    }

    /**
     * Creates a Satellite Tracking projection from the given parameters.
     *
     * @param  method      description of the projection parameters.
     * @param  parameters  the parameter values of the projection to create.
     * @throws InvalidParameterValueException if some parameters have incompatible values.
     */
    public SatelliteTracking(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="8", fixed="25")
    private SatelliteTracking(final Initializer initializer) {
        super(initializer, null);
        final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        final double φ1 = toRadians(initializer.getAndStore(STANDARD_PARALLEL_1));
        final double φ2 = toRadians(initializer.getAndStore(STANDARD_PARALLEL_2));
        final double i  = toRadians(initializer.getAndStore(SATELLITE_ORBIT_INCLINATION));
        p2_on_p1 =                  initializer.getAndStore(SATELLITE_ORBITAL_PERIOD) /
                                    initializer.getAndStore(ASCENDING_NODE_PERIOD);
        /*
         * Symbols from Snyder used in the constructor:
         *
         *   i  =  angle of inclination between the plane of Earth Equator and the plane of satellite orbit.
         *   P₁ =  length of Earth's rotation with respect to precessed ascending node.
         *   P₂ =  time required for revolution of the satellite.
         *   φ₁ =  standard parallel.
         *   φ₂ =  second standard parallel. Equals to -φ₁ in the cylindrical case.
         *   F₁ =  angle on the globe between the ground-track and the meridian at latitude φ₁.
         *   n  :  used for conic projection only.
         *   s₀ :  user for conic projection only.
         *
         * Next terms below are common to both cylindrical and conic sattelite tracking projections.
         */
        sin_i   = sin(i);
        cos_i   = cos(i);
        cos2_i  = cos_i * cos_i;
        isConic = Math.abs(φ2 + φ1) > ANGULAR_TOLERANCE;
        final double cosφ1   = cos(φ1);
        final double cos2_φ1 = cosφ1 * cosφ1;
        /*
         * Some terms will be stored as scale factor and offset applied by matrix before projection
         * (normalization) and after projection (denormalization).  Those factors will be different
         * for cylindric and conic projections.
         */
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        if (isConic) {
            final double sinφ1 = sin(φ1);
            final double L0 =  L(sin(φ0), LATITUDE_OF_ORIGIN);
            final double L1 =  L(sinφ1,   STANDARD_PARALLEL_1);
            final double F1 =  F(cos2_φ1, STANDARD_PARALLEL_1);
            /*
             * For conic projection with one standard parallel (φ₁ = φ₂), the general case implemented by
             * equation 28-10 become indeterminate. Equation 28-17 below resolves that indetermination.
             * If furthermore φ₁ is equal to the upper tracking limit π-i, that equation 28-17 could be
             * simplified by equation 28-18:
             *
             *     f = p2_on_p1 * cos_i - 1;
             *     n = sin_i / (f*f);                                       // Snyder equation 28-18.
             *
             * However, since equation 28-17 still work, we keep it for avoiding discontinuity.
             */
            if (abs(φ2 - φ1) < ANGULAR_TOLERANCE) {
                n = sinφ1 * (p2_on_p1 * (2*cos2_i - cos2_φ1) - cos_i)
                          / (p2_on_p1 * (           cos2_φ1) - cos_i);      // Equation 28-17.
            } else {
                final double cosφ2 = cos(φ2);
                final double F2 = F(cosφ2*cosφ2, STANDARD_PARALLEL_2);
                final double L2 = L(sin(φ2),     STANDARD_PARALLEL_2);
                n = (F2 - F1) / (L2 - L1);                                  // Snyder equation 28-10.
            }
            s0 = F1 - n*L1;                                                 // Snyder equation 28-11.
            final double ρf = cosφ1 * sin(F1) / n;                          // Part of Snyder 28-12 fraction.
            final double ρ0 = ρf / sin(n*L0 + s0);                          // Remaining of Snyder 28-12 without R.
            if (!Double.isFinite(ρf) || ρf == 0) {
                throw invalid(STANDARD_PARALLEL_1);
            }
            final MatrixSIS normalize = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
            normalize  .convertAfter (0,  n,  null);
            denormalize.convertBefore(0, +ρf, null);
            denormalize.convertBefore(1, -ρf, ρ0);
        } else {
            /*
             * Cylindrical projection case. The equations are (ignoring R and λ₀):
             *
             *     x   =  λ⋅cosφ₁
             *     y   =  L⋅cosφ₁/F₁′  where  L depends on φ but F₁′ is constant.
             *     F₁′ =  tan(F₁)
             *
             * The cosφ₁ (for x at dimension 0) and cosφ₁/F₁′ (for y at dimension 1) factors are computed
             * in advance and stored below. The remaining factor to compute in transform(…) method is L.
             */
            n = s0 = Double.NaN;
            final double cotF = sqrt(cos2_φ1 - cos2_i) / (p2_on_p1*cos2_φ1 - cos_i);    // Cotangente of F₁.
            denormalize.convertBefore(0, cosφ1,      null);
            denormalize.convertBefore(1, cosφ1*cotF, null);
            if (!Double.isFinite(cotF) || cotF == 0) {
                throw invalid(STANDARD_PARALLEL_1);
            }
        }
    }

    /**
     * Returns the sequence of <i>normalization</i> → {@code this} → <i>denormalization</i> transforms as a whole.
     * The transform returned by this method expects (<var>longitude</var>, <var>latitude</var>) coordinates
     * in <em>degrees</em> and returns (<var>x</var>,<var>y</var>) coordinates in <em>metres</em>.
     *
     * @param  parameters  parameters and the factory to use for creating the transform.
     * @return the map projection from (λ,φ) to (<var>x</var>,<var>y</var>) coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMapProjection(final MathTransformProvider.Context parameters) throws FactoryException {
        if (isConic) {
            return completeWithWraparound(parameters);
        } else {
            return super.createMapProjection(parameters);
        }
    }

    /**
     * Returns the names of additional internal parameters which need to be taken in account when
     * comparing two {@code SatelliteTracking} projections or formatting them in debug mode.
     */
    @Override
    final String[] getInternalParameterNames() {
        return new String[] {"i", "P₂∕P₁"};
    }

    /**
     * Returns the values of additional internal parameters which need to be taken in account when
     * comparing two {@code SatelliteTracking} projections or formatting them in debug mode.
     */
    @Override
    final double[] getInternalParameterValues() {
        return new double[] {
            (cos_i < PI/4) ? acos(cos_i) : asin(sin_i),
            p2_on_p1
        };
    }

    /**
     * Computes the F₁ or F₂ coefficient using Snyder equation 28-9. Note that this is the same equation
     * than F₁′ in above cylindrical case, but with the addition of arc-tangent. This value is constant
     * after construction time.
     *
     * @param  cos2_φ  square of cosine of φ₁ or φ₂.
     * @param  source  description of φ argument. Used for error message only.
     * @return F coefficient for the given φ latitude.
     */
    private double F(final double cos2_φ, final ParameterDescriptor<Double> source) {
        final double F = atan((p2_on_p1*cos2_φ - cos_i) / sqrt(cos2_φ - cos2_i));
        if (Double.isFinite(F)) {
            return F;
        }
        throw invalid(source);
    }

    /**
     * Computes the L₀, L₁ or L₂ coefficient using Snyder equation 28-2a to 28-4a.
     * This value is constant after construction time.
     *
     * @param  sinφ    sine of φ₀, φ₁ or φ₂.
     * @param  source  description of φ argument. Used for error message only.
     * @return L coefficient for the given φ latitude.
     */
    private double L(final double sinφ, final ParameterDescriptor<Double> source) {
        final double λp = -asin(sinφ / sin_i);                      // λ′ in Snyder equation 28-2a.
        final double L = atan(tan(λp) * cos_i) - p2_on_p1 * λp;     // Snyder equation 28-3a and 28-4a.
        if (Double.isFinite(L)) {
            return L;
        }
        throw invalid(source);
    }

    /**
     * Returns an exception for a latitude parameter out of range.
     * The range is assumed given by satellite orbit inclination.
     *
     * @param  source  description of invalid φ argument.
     */
    private InvalidParameterValueException invalid(final ParameterDescriptor<Double> source) {
        final String name  =     source.getName().getCode();
        final double value =     context.doubleValue(source);
        final double limit = abs(context.doubleValue(SATELLITE_ORBIT_INCLINATION));
        return new InvalidParameterValueException(Errors.format(Errors.Keys.ValueOutOfRange_4,
                                                  name, -limit, limit, new Latitude(value)), name, value);
    }

    /**
     * Projects the specified (λ,φ) coordinates and stores the result in {@code dstPts}.
     * In addition, opportunistically computes the projection derivative if {@code derivate} is {@code true}.
     * The units of measurement are implementation-specific (see super-class javadoc).
     * The results must be multiplied by the denormalization matrix before to get linear distances.
     *
     * <p>The <var>y</var> axis lies along the central meridian λ₀, <var>y</var> increasing northerly, and
     * <var>x</var> axis intersects perpendicularly at latitude of origin φ₀, <var>x</var> increasing easterly.</p>
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
        final double φ         = srcPts[srcOff + 1];
        final double sinφ_sini = sin(φ) / sin_i;
              double λpm       = -asin(sinφ_sini);          // Initialized to λ′ (Snyder equation 28-2), to be modified later.
        final double tanλp     = tan(λpm);                  // tan(λ′), saved because also used in derivative computation.
        final double λt        = atan(tanλp * cos_i);       // λₜ in Snyder equation 28-3.
        /*
         * The (x,y) coordinates below are all is needed for the cylindrical case.
         * With R and cosφ₁ omitted because multiplied outside this method:
         *
         *     x = λ
         *     y = L    with L = λₜ − (P₂/P₁)λ′     (Snyder equation 28-6)
         *
         * Only in conic case, we need to continue with some additional computation.
         * Note that λpm is NaN if latitude φ is closer to pole than tracking limit.
         */
        double x = srcPts[srcOff];
        double y = λt - p2_on_p1 * λpm;
        if (isConic) {
            λpm = n*y + s0;                     // Use this variable for a new purpose. Needed for derivative.
            if ((Double.doubleToRawLongBits(λpm) ^ Double.doubleToRawLongBits(n)) < 0) {
                /*
                 * if λpm does not have the sign than n, the (x,y) values computed below would suddenly
                 * change their sign. The y values lower or greater (depending of n sign) than -s0/n can
                 * not be plotted. Snyder suggests to use another projection if cosmetic output is wanted.
                 * For now, we just set the results to NaN (meaning "no result", which is not the same as
                 * TransformException which means that a result exists but cannot be computed).
                 */
                λpm = Double.NaN;
            }
            final double iρ = sin(λpm);         // Inverse of ρ and without the ρf = cos(φ₁)⋅sin(F₁)/n part.
            y = cos(x) / iρ;                    // x already multiplied by n (was done by normalization matrix).
            x = sin(x) / iρ;                    // Must be last.
        }
        if (dstPts != null) {
            dstPts[dstOff    ] = x;
            dstPts[dstOff + 1] = y;
        }
        if (!derivate) {
            return null;
        }
        /*
         * Create a derivative matrix initializez with ∂L/∂φ. For cylindrical case where y = L (in this method),
         * this is all we need to do. For the conic case, we need to multiply the last column ∂x/∂L and ∂y/∂L.
         */
        final Matrix2 d = new Matrix2();
        d.m11 = ((cos(φ) / sin_i) / sqrt(1 - sinφ_sini*sinφ_sini))
              * (p2_on_p1 - ((1 + tanλp*tanλp)*cos_i/(1 + λt*λt)));
        if (isConic) {
            final double dρ_dφ = -n / tan(λpm);
            d.m00  = +y;                            // ∂x/∂λ
            d.m10  = -x;                            // ∂y/∂λ
            d.m01  =  x * dρ_dφ * d.m11;            // ∂x/∂φ  =  ∂x/∂L ⋅ ∂L/∂φ
            d.m11 *=  y * dρ_dφ;                    // ∂y/∂φ  =  ∂y/∂L ⋅ ∂L/∂φ
        }
        return d;
    }

    /**
     * Transforms the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the coordinates cannot be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff) throws ProjectionException
    {
        double x = srcPts[srcOff];
        double L = srcPts[srcOff + 1];                  // Multiplication e.g. by F₁′/(R⋅cosφ₁) already done.
        if (isConic) {
            final double ρ = copySign(hypot(x,L), n);
            x = atan(x / L);                            // Undefined if x = y = 0.
            L = (asin(1 / ρ) - s0) / n;                 // Equation 28-26 in Snyder with R=1.
        }
        /*
         * Approximation of the latitude associated with L coefficient by applying Newton-Raphson method.
         * Equations 28-24, 28-25 and then 28-22 in Snyder's book.
         */
        final double ic  = 1 / cos2_i;
        final double pc = p2_on_p1 * cos_i;
        double λp = -PI/2, Δλp;
        int iter = Formulas.MAXIMUM_ITERATIONS;
        do {
            if (--iter < 0) {
                throw new ProjectionException(Resources.format(Resources.Keys.NoConvergence));
            }
            final double A   = tan(L + p2_on_p1 * λp) / cos_i;          // Snyder equation 28-24.
            final double A2  = A*A;
            Δλp = (atan(A) - λp) / (1 - pc*((A2 + ic) / (A2 + 1)));     // Snyder equation 28-25.
            λp += Δλp;
        } while (abs(Δλp) >= ANGULAR_TOLERANCE);
        dstPts[dstOff  ] = x;
        dstPts[dstOff+1] = -asin(sin(λp) * sin_i);                      // Snyder equation 28-22.
    }
}
