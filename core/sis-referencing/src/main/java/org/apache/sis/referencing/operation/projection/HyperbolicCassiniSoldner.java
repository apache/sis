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

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.*;
import static org.apache.sis.internal.referencing.provider.CassiniSoldner.LATITUDE_OF_ORIGIN;


/**
 * <cite>Hyperbolic Cassini-Soldner</cite> projection (EPSG code 9833).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class HyperbolicCassiniSoldner extends CassiniSoldner {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4360637175311262375L;

    /**
     * The hyperbolic variants of this projection. {@link #VANUA} is the special case
     * of <cite>Vanua Levu Grid</cite>, which is the only hyperbolic variant for which
     * inverse projection is supported.
     *
     * @see #variant
     */
    private static final byte HYPERBOLIC = 1, VANUA = 2;

    /**
     * The latitude of {@link #VANUA} variant (16°15′S) in radians.
     */
    private static final double VANUA_LATITUDE = -(16 + 15./60) * (PI/180);

    /**
     * The type of Cassini-Soldner projection. Possible values are:
     *
     * <ul>
     *   <li>{@link #STANDARD_VARIANT} if this projection is the standard variant (handled by parent class).</li>
     *   <li>{@link #HYPERBOLIC} if this projection is the "Hyperbolic Cassini-Soldner" case.</li>
     *   <li>{@link #VANUA} if this projection is the "Hyperbolic Cassini-Soldner" case at φ₀=16°15′S.</li>
     * </ul>
     *
     * Other cases may be added in the future.
     */
    private final byte variant;

    /**
     * Meridional distance <var>M</var> from equator to latitude of origin φ₀.
     * This parameter is explicit only for the hyperbolic variants. The standard variant does not need it
     * because {@link #M0} is subtracted in the {@linkplain ContextualParameters.MatrixRole#DENORMALIZATION
     * denormalization matrix}.
     */
    private final double M0;

    /**
     * Creates a Hyperbolic Cassini-Soldner projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Hyperbolic Cassini-Soldner"</cite>.</li>
     * </ul>
     *
     * @param  method      description of the projection parameters.
     * @param  parameters  the parameter values of the projection to create.
     */
    public HyperbolicCassiniSoldner(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters, HYPERBOLIC));
    }

    /**
     * Creates a new Cassini-Soldner projection from the given initializer.
     */
    HyperbolicCassiniSoldner(final Initializer initializer) {
        super(initializer);
        final double φ0 = toRadians(initializer.getAndStore(LATITUDE_OF_ORIGIN));
        variant = (abs(φ0 - VANUA_LATITUDE) <= ANGULAR_TOLERANCE) ? VANUA : initializer.variant;
        M0 = distance(φ0, sin(φ0), cos(φ0));
    }

    /**
     * Returns the names of additional internal parameters which need to be taken in account when
     * comparing two {@code CassiniSoldner} projections or formatting them in debug mode.
     */
    @Override
    final String[] getInternalParameterNames() {
        return new String[] {"M₀"};
    }

    /**
     * Returns the values of additional internal parameters which need to be taken in account when
     * comparing two {@code CassiniSoldner} projections or formatting them in debug mode.
     */
    @Override
    final double[] getInternalParameterValues() {
        return new double[] {M0};
    }

    /**
     * Returns the sequence of <cite>normalization</cite> → {@code this} → <cite>denormalization</cite> transforms
     * as a whole. The transform returned by this method expects (<var>longitude</var>, <var>latitude</var>)
     * coordinates in <em>degrees</em> and returns (<var>x</var>,<var>y</var>) coordinates in <em>metres</em>.
     *
     * @param  factory  the factory to use for creating the transform.
     * @return the map projection from (λ,φ) to (<var>x</var>,<var>y</var>) coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMapProjection(final MathTransformFactory factory) throws FactoryException {
        return context.completeTransform(factory, this);
    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}.
     *
     * @return the matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinate can not be converted.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws ProjectionException
    {
        if (dstPts != null) {
            final double sinφ = sin(srcPts[srcOff+1]);              // Save before to invoke super.transform(…).
            super.transform(srcPts, srcOff, dstPts, dstOff, false);
            final double X    = dstPts[dstOff+1] - M0;
            final double rν2  = 1 - (sinφ*sinφ)*eccentricitySquared;
            /*
             * Offset: X³ / (6ρν)    where    ρ = (1 – ℯ²)⋅ν³
             *       = X³ / (6⋅(1 – ℯ²)⋅ν⁴)
             */
            dstPts[dstOff+1] = X - (X*X*X) / (6*(1 - eccentricitySquared) / (rν2*rν2));
        }
        return null;        // // Derivative not supported for this operation method.
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the point can not be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        if (variant != VANUA) {
            throw new ProjectionException(Errors.format(Errors.Keys.UnsupportedArgumentValue_1, "φ₀≠16°15′S"));
        }
        /*
         * Following symbols are related to EPSG symbols with indices and primes dropped, and some multiplication
         * factors inserted or implicit. EPSG said that this formula is specifically for the Fiji Vanua Levu grid.
         * I presume that the specificity is in the 315320 constant, which needs to be divided by semi-major axis
         * length in our implementation. We take the axis length of Fiji Vanua Levu grid on the assumption that it
         * is the axis length used for computing the 315320 constant.
         */
        double x = srcPts[srcOff  ];
        double y = srcPts[srcOff+1];                                            // = N - FN    (N & FN defined by EPSG)
        final double sinφ = sin(VANUA_LATITUDE + y * (317063.667 / 315320));    // Constants are: a / (value from EPSG)
        final double rν2  = 1 - eccentricitySquared*(sinφ*sinφ);                // = 1/√ν₁′       (ν₁′ defined by EPSG)
        final double ρν   = 6*(1 - eccentricitySquared)/(rν2 * rν2);            // = 6⋅ρ₁′ν₁′/a²  (ρ₁′ defined by EPSG)
        final double q    = y + y*y*y / ρν;                                     // = (N + q′)/a   (q′  defined by EPSG)
        y += q*q*q / ρν;
        dstPts[dstOff  ] = x;
        dstPts[dstOff+1] = y + M0;
        super.inverseTransform(dstPts, dstOff, dstPts, dstOff);
    }
}
