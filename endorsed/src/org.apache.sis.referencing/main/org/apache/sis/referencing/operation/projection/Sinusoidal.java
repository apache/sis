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
import java.util.regex.Pattern;
import static java.lang.Math.*;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.Workaround;
import static org.apache.sis.referencing.operation.provider.Sinusoidal.*;


/**
 * <cite>Sinusoidal equal-area</cite> projection, also known as <q>Sanson-Flamsteed</q>.
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Sinusoidal_projection">Sinusoidal projection on Wikipedia</a></li>
 *   <li><a href="https://mathworld.wolfram.com/SinusoidalProjection.html">Sinusoidal projection on MathWorld</a></li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class Sinusoidal extends MeridianArcBased {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7908925241331303236L;

    /**
     * Variants of Sinusoidal projection. Those variants modify the way the projections are constructed
     * (e.g. in the way parameters are interpreted), but formulas are basically the same after construction.
     *
     * <p>We do not provide such codes in public API because they duplicate the functionality of
     * {@link OperationMethod} instances. We use them only for constructors convenience.</p>
     */
    private enum Variant implements ProjectionVariant {
        // Declaration order matter. Patterns are matched in that order.

        /** The <q>Pseudo sinusoidal equal-area</q> projection. */
        PSEUDO(".*\\bPseudo.*");

        /** Name pattern for this variant. */
        private final Pattern operationName;

        /** Creates a new enumeration value.  */
        private Variant(final String operationName) {
            this.operationName = Pattern.compile(operationName, Pattern.CASE_INSENSITIVE);
        }

        /** The expected name pattern of an operation method for this variant. */
        @Override public Pattern getOperationNamePattern() {
            return operationName;
        }
    }

    /**
     * The type of sinusoidal projection. Possible values are:
     * <ul>
     *   <li>{@link Variant#PSEUDO} if this projection is the "Pseudo sinusoidal equal-area" case.</li>
     *   <li>{@code null} for the standard case.</li>
     * </ul>
     *
     * Other cases may be added in the future.
     */
    private final Variant variant;

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="8", fixed="25")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final Variant variant = variant(method, Variant.values(), null);
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, CENTRAL_MERIDIAN);
        roles.put(ParameterRole.FALSE_EASTING,    FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,   FALSE_NORTHING);
        return new Initializer(method, parameters, roles, variant);
    }

    /**
     * Creates a Sinusoidal projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>Sinusoidal</q>, also known as <q>Sanson-Flamsteed</q>.</li>
     * </ul>
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public Sinusoidal(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="7", fixed="25")
    private Sinusoidal(final Initializer initializer) {
        super(initializer);
        variant = (Variant) initializer.variant;
    }

    /**
     * Creates a new projection initialized to the same parameters as the given one.
     */
    Sinusoidal(final Sinusoidal other) {
        super(other);
        variant = other.variant;
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
        Sinusoidal kernel = this;
        if (eccentricity == 0 || variant == Variant.PSEUDO) {
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
        final double cosφ  = cos(φ);
        final double sinφ  = sin(φ);
        final double sinφ2 = sinφ * sinφ;
        final double rν2   = 1 - eccentricitySquared * sinφ2;
        final double rν    = sqrt(rν2);                                 // Reciprocal of the radius of curvature.
        final double dx_dλ = cosφ / rν;                                 // Part of Snyder 30-8.
        /*
         * Note: in theory x/cos(φ) is indeterminate at φ=±π/2. However, in this code,
         * that indetermination never happen because there is no exact representation
         * of π/2 in base 2, so cos(φ) can never return 0.
         */
        if (dstPts != null) {
            dstPts[dstOff  ] = dx_dλ * λ;
            dstPts[dstOff+1] = distance(φ, sinφ, cosφ);
        }
        if (!derivate) {
            return null;
        }
        final double dx_dφ = λ * sinφ * (eccentricitySquared*(cosφ*cosφ) / rν2 - 1) / rν;
        return new Matrix2(dx_dλ, dx_dφ, 0, dM_dφ(sinφ2));
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
    {
        final double x    = srcPts[srcOff  ];
        final double y    = srcPts[srcOff+1];
        final double φ    = latitude(y);
        final double sinφ = sin(φ);
        dstPts[dstOff  ]  = x * sqrt(1 - eccentricitySquared * (sinφ*sinφ)) / cos(φ);           // Part of Snyder 30-11
        dstPts[dstOff+1]  = φ;
    }


    /**
     * Provides the transform equations for the spherical case of the Sinusoidal projection.
     *
     * @author  Martin Desruisseaux (Geomatys)
     */
    private static final class Spherical extends Sinusoidal {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5843301120207230310L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param  other  the other projection (usually ellipsoidal) from which to copy the parameters.
         */
        Spherical(final Sinusoidal other) {
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
            final double cosφ = cos(φ);
            if (dstPts != null) {
                dstPts[dstOff  ] = λ * cosφ;            // Part of Snyder 30-1
                dstPts[dstOff+1] = φ;                   // Part of Snyder 30-2
            }
            return derivate ? new Matrix2(cosφ, -λ*sin(φ), 0, 1) : null;
        }

        /**
         * Converts a list of coordinate tuples. This method performs the same calculation as above
         * {@link #transform(double[], int, double[], int, boolean)} method, but is overridden for efficiency.
         */
        @Override
        public void transform(final double[] srcPts, int srcOff,
                              final double[] dstPts, int dstOff, int numPts)
                throws TransformException
        {
            if (srcPts != dstPts || srcOff != dstOff) {
                super.transform(srcPts, srcOff, dstPts, dstOff, numPts);
            } else {
                while (--numPts >= 0) {
                    dstPts[dstOff] *= cos(dstPts[dstOff+1]);
                    dstOff += DIMENSION;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void inverseTransform(final double[] srcPts, final int srcOff,
                                        final double[] dstPts, final int dstOff)
        {
            final double x = srcPts[srcOff  ];
            final double φ = srcPts[srcOff+1];
            /*
             * Note: in theory x/cos(φ) is indeterminate at φ=±π/2. However, in this code,
             * that indetermination never happen because there is no exact representation
             * of π/2 in base 2, so cos(φ) can never return 0.
             */
            dstPts[dstOff  ] = x / cos(φ);              // Part of Snyder 30-5
            dstPts[dstOff+1] = φ;                       // Part of Snyder 30-6
        }
    }
}
