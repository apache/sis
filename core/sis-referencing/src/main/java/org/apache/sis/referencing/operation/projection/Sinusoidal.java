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
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.Workaround;

import static java.lang.Math.*;
import static org.apache.sis.internal.referencing.provider.Sinusoidal.*;


/**
 * <cite>Sinusoidal equal-area</cite> projection, also known as <cite>"Sanson-Flamsteed"</cite>.
 * See the <a href="https://en.wikipedia.org/wiki/Sinusoidal_projection">Sinusoidal projection on Wikipedia</a>
 * for an overview.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class Sinusoidal extends MeridionalDistanceBased {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7908925241331303236L;

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, CENTRAL_MERIDIAN);
        roles.put(ParameterRole.FALSE_EASTING,    FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING,   FALSE_NORTHING);
        return new Initializer(method, parameters, roles, (byte) 0);
    }

    /**
     * Creates a Sinusoidal projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Sinusoidal"</cite>, also known as <cite>"Sanson-Flamsteed"</cite>.</li>
     * </ul>
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public Sinusoidal(final OperationMethod method, final Parameters parameters) {
        super(initializer(method, parameters));
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    Sinusoidal(final Sinusoidal other) {
        super(other);
    }

    /**
     * Returns the sequence of <cite>normalization</cite> → {@code this} → <cite>denormalization</cite> transforms
     * as a whole. The transform returned by this method expects (<var>longitude</var>, <var>latitude</var>)
     * coordinates in <em>degrees</em> and returns (<var>x</var>,<var>y</var>) coordinates in <em>metres</em>.
     *
     * <p>The non-linear part of the returned transform will be {@code this} transform, except if the ellipsoid
     * is spherical. In the later case, {@code this} transform will be replaced by a simplified implementation.</p>
     *
     * @param  factory  the factory to use for creating the transform.
     * @return the map projection from (λ,φ) to (<var>x</var>,<var>y</var>) coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    @Override
    public MathTransform createMapProjection(final MathTransformFactory factory) throws FactoryException {
        Sinusoidal kernel = this;
        if (eccentricity == 0) {
            kernel = new Spherical(this);
        }
        return context.completeTransform(factory, kernel);
    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}
     * (linear distance on a unit sphere). In addition, opportunistically computes the projection derivative
     * if {@code derivate} is {@code true}.
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
        final double λ    = srcPts[srcOff  ];
        final double φ    = srcPts[srcOff+1];
        final double cosφ = cos(φ);
        // TODO: replace by ellipsoidal formulas.
        if (dstPts != null) {
            dstPts[dstOff]   = λ * cosφ;
            dstPts[dstOff+1] = φ;
        }
        return derivate ? new Matrix2(cosφ, -λ*sin(φ), 0, 1) : null;
    }

    /**
     * Converts the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
    {
        final double x = srcPts[srcOff  ];
        final double φ = srcPts[srcOff+1];
        // TODO: replace by ellipsoidal formulas.
        dstPts[dstOff  ] = x / cos(φ);
        dstPts[dstOff+1] = φ;
    }


    /**
     * Provides the transform equations for the spherical case of the Sinusoidal projection.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.0
     * @since   1.0
     * @module
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
                                final boolean derivate) throws ProjectionException
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
         * Converts a list of coordinate points. This method performs the same calculation than above
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
            dstPts[dstOff  ] = x / cos(φ);              // Part of Snyder 30-5
            dstPts[dstOff+1] = φ;                       // Part of Snyder 30-6
        }
    }
}
