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

import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.internal.referencing.provider.Mercator2SP;
import org.apache.sis.internal.referencing.provider.PseudoMercator;
import org.apache.sis.internal.referencing.provider.MillerCylindrical;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.*;
import static java.lang.Double.*;


/**
 * <cite>Mercator Cylindrical</cite> projection (EPSG codes 9804, 9805, 1026, 1024, <span class="deprecated">9841</span>).
 * See the <a href="http://mathworld.wolfram.com/MercatorProjection.html">Mercator projection on MathWorld</a> for an overview.
 *
 * <div class="section">Description</div>
 * The parallels and the meridians are straight lines and cross at right angles; this projection thus produces
 * rectangular charts. The scale is true along the equator (by default) or along two parallels equidistant of the
 * equator (if a scale factor other than 1 is used).
 *
 * <p>This projection is used to represent areas close to the equator. It is also often used for maritime navigation
 * because all the straight lines on the chart are <cite>loxodrome</cite> lines, i.e. a ship following this line would
 * keep a constant azimuth on its compass.</p>
 *
 * <p>This implementation handles both the 1 and 2 standard parallel cases.
 * For <cite>Mercator (variant A)</cite> (EPSG code 9804), the line of contact is the equator.
 * For <cite>Mercator (variant B)</cite> (EPSG code 9805) lines of contact are symmetrical about the equator.</p>
 *
 * <div class="section">Behavior at poles</div>
 * The projection of 90°N gives {@linkplain Double#POSITIVE_INFINITY positive infinity}.
 * The projection of 90°S gives {@linkplain Double#NEGATIVE_INFINITY negative infinity}.
 * Projection of a latitude outside the [-90 … 90]° range produces {@linkplain Double#NaN NaN}.
 *
 * @author  André Gosselin (MPO)
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @author  Simon Reynard (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see TransverseMercator
 * @see ObliqueMercator
 */
public class Mercator extends NormalizedProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2564172914329253286L;

    /**
     * Codes for special kinds of Mercator projection.
     */
    static final byte PSEUDO = 1, MILLER = 2;

    /**
     * 0 if this projection is a standard Mercator, {@link #PSEUDO} if this projection is the "Pseudo Mercator" case.
     * Other cases may be added in the future.
     */
    final byte type;

    /**
     * Creates a Mercator projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Mercator (variant A)"</cite>, also known as <cite>"Mercator (1SP)"</cite>.</li>
     *   <li><cite>"Mercator (variant B)"</cite>, also known as <cite>"Mercator (2SP)"</cite>.</li>
     *   <li><cite>"Popular Visualisation Pseudo Mercator"</cite>.</li>
     *   <li><cite>"Miller Cylindrical"</cite>.</li>
     * </ul>
     *
     * @param method     Description of the projection parameters.
     * @param parameters The parameter values of the projection to create.
     */
    public Mercator(final OperationMethod method, final Parameters parameters) {
        super(method, parameters, Mercator2SP.FALSE_EASTING, Mercator2SP.FALSE_NORTHING);
        double φ1 = toRadians(getAndStore(parameters, Mercator2SP.STANDARD_PARALLEL));
        double φ0 = parameters.doubleValue(           Mercator2SP.LATITUDE_OF_ORIGIN);  // Do not store this parameter
        double λ0 =           getAndStore(parameters, Mercator2SP.CENTRAL_MERIDIAN);
        double k0 =           getAndStore(parameters, Mercator2SP.SCALE_FACTOR);
        /*
         * The "Latitude of origin" is not formally a parameter of Mercator projection, except in variant A (1SP)
         * where the parameter is included in the EPSG dataset for completeness in CRS labelling but is not allowed
         * to be assigned any value other than zero.
         */
        if (!(abs(φ0) <= Formulas.ANGULAR_TOLERANCE)) {   // Use '!' for catching NaN.
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalParameterValue_2,
                    Mercator2SP.LATITUDE_OF_ORIGIN.getName(), φ0));
        }
        /*
         * A correction that allows us to employ a standard parallel that is not correspondent to the equator,
         * as described in Snyder and al. at page 47. This is the same correction factor than the one applied
         * for the Mercator (2SP) case, constant "ko" in EPSG:9805.
         *
         * The scale correction is multiplied with the global scale, which allows the Apache SIS referencing
         * module to merge this correction with the scale factor in a single multiplication. In principle we
         * should never have both the scale factor and a standard parallel. Nevertheless we sometime see such
         * CRS definitions.
         */
        k0 *= cos(φ1);
        if (!isSpherical()) {
            k0 /= rν(sin(φ1));
        }
        final ParameterDescriptorGroup descriptor = parameters.getDescriptor();
        if (IdentifiedObjects.isHeuristicMatchForName(descriptor, PseudoMercator.NAME)) {
            type = PSEUDO;
        } else if (IdentifiedObjects.isHeuristicMatchForName(descriptor, MillerCylindrical.NAME)) {
            type = MILLER;
            k0 *= 1.25;
        } else {
            type = 0;
        }
        /*
         * In principle we should rotate the central meridian (λ0) in the normalization transform, as below:
         *
         *     context.normalizeGeographicInputs(λ0);
         *     context.scaleAndTranslate2D(false, k0, 0, 0);
         *
         * However in the particular case of Mercator projection, we will apply the longitude rotation in the
         * denormalization matrix instead.   This is possible only for this particular projection because the
         * 'transform(…)' methods pass the longitude values unchanged. By keeping the normalization affine as
         * simple as possible, we increase the chances of efficient concatenation of an inverse with a forward
         * projection.
         */
        final MatrixSIS   normalize = context.normalizeGeographicInputs(0);
        final MatrixSIS denormalize = context.scaleAndTranslate2D(false, k0, 0, 0);
        final DoubleDouble offset = DoubleDouble.createDegreesToRadians();
        offset.multiply(λ0);
        denormalize.concatenate(0, null, offset);
        if (type == MILLER) {
            normalize.concatenate(1, new DoubleDouble(0.8), null);
        }
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    Mercator(final Mercator other) {
        super(other);
        type = other.type;
    }

    /**
     * Returns the transform that {@link #createMapProjection(MathTransformFactory)} should use as the
     * non-linear kernel.
     */
    @Override
    final MathTransform2D kernel() {
        return (isSpherical() || type == PSEUDO) ? new Spherical(this) : this;
    }

    /**
     * Returns the parameter descriptors for this normalized projection.
     * The default implementation returns the descriptor of <cite>"Mercator (variant A)"</cite> in all cases
     * except <cite>"Popular Visualisation Pseudo Mercator"</cite>.
     * This method does not return the descriptor of other other cases because the <cite>"Mercator (variant B)"</cite>
     * case (for example) is implemented as <cite>"Mercator (variant A)"</cite> with a different scale factor.
     *
     * @return The <cite>"Mercator (variant A)"</cite> or <cite>"Popular Visualisation Pseudo Mercator"</cite>
     *         parameter descriptor.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        // No special descriptor even for (type == MILLER).
        return (type == PSEUDO) ? PseudoMercator.PARAMETERS : Mercator1SP.PARAMETERS;
    }

    // No need to override getParameterValues() because no additional
    // parameter are significant to a normalized Mercator projection.


    /**
     * Converts the specified (<var>λ</var>,<var>φ</var>) coordinate (units in radians)
     * and stores the result in {@code dstPts} (linear distance on a unit sphere). In addition,
     * opportunistically computes the projection derivative if {@code derivate} is {@code true}.
     *
     * @return The matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinate can not be converted.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws ProjectionException
    {
        final double λ    = srcPts[srcOff];
        final double φ    = srcPts[srcOff + 1];
        final double sinφ = sin(φ);
        /*
         * Projection of zero is zero. However the formulas below have a slight rounding error
         * which produce values close to 1E-10, so we will avoid them when y=0. In addition of
         * avoiding rounding error, this also preserve the sign (positive vs negative zero).
         */
        final double y;
        if (φ == 0) {
            y = φ;
        } else {
            // See the javadoc of the Spherical inner class for a note
            // about why we perform explicit checks for the pole cases.
            final double a = abs(φ);
            if (a < PI/2) {
                y = log(expOfNorthing(φ, excentricity * sinφ));     // Snyder (7-7)
            } else {
                y = copySign(a <= (PI/2 + ANGULAR_TOLERANCE) ? POSITIVE_INFINITY : NaN, φ);
            }
        }
        if (dstPts != null) {
            dstPts[dstOff]   = λ;
            dstPts[dstOff+1] = y;
        }
        /*
         * End of map projection. Now compute the derivative, if requested.
         */
        return derivate ? new Matrix2(1, 0, 0, dy_dφ(sinφ, cos(φ))) : null;
    }

    /**
     * Converts a list of coordinate point ordinal values.
     *
     * <div class="note"><b>Note:</b>
     * We override the super-class method only as an optimization in the special case where the target coordinates
     * are written at the same locations than the source coordinates. In such case, we can take advantage of the
     * fact that the λ values are not modified by the normalized Mercator projection.</div>
     *
     * @throws TransformException if a point can not be converted.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        if (srcPts != dstPts || srcOff != dstOff) {
            super.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        } else {
            dstOff--;
            while (--numPts >= 0) {
                final double φ = dstPts[dstOff += 2]; // Same as srcPts[srcOff + 1].
                if (φ != 0) {
                    // See the javadoc of the Spherical inner class for a note
                    // about why we perform explicit checks for the pole cases.
                    final double a = abs(φ);
                    final double y;
                    if (a < PI/2) {
                        y = log(expOfNorthing(φ, excentricity * sin(φ)));
                    } else {
                        y = copySign(a <= (PI/2 + ANGULAR_TOLERANCE) ? POSITIVE_INFINITY : NaN, φ);
                    }
                    dstPts[dstOff] = y;
                }
            }
        }
    }

    /**
     * Transforms the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     *
     * @throws ProjectionException if the point can not be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double y   = srcPts[srcOff+1];    // Must be before writing x.
        dstPts[dstOff  ] = srcPts[srcOff  ];    // Must be before writing y.
        dstPts[dstOff+1] = φ(exp(-y));
    }


    /**
     * Provides the transform equations for the spherical case of the Mercator projection.
     *
     * <div class="note"><b>Implementation note:</b>
     * this class contains an explicit check for latitude values at a pole. If floating point arithmetic had infinite
     * precision, such checks would not be necessary since the formulas lead naturally to infinite values at poles,
     * which is the correct answer. In practice the infinite value emerges by itself at only one pole, and the other
     * one produces a high value (approximatively 1E+16). This is because there is no accurate representation of π/2,
     * and consequently {@code tan(π/2)} does not return the infinite value. We workaround this issue with an explicit
     * check for abs(φ) ≊ π/2. Note that:
     *
     * <ul>
     *   <li>The arithmetic is not broken for values close to pole. We check π/2 because this is the result of
     *       converting 90°N to radians, and we presume that the user really wanted to said 90°N. But for most
     *       other values we could let the math do their "natural" work.</li>
     *   <li>For φ = -π/2 our arithmetic already produces negative infinity.</li>
     * </ul>
     * </div>
     *
     * @author Martin Desruisseaux (MPO, IRD, Geomatys)
     * @author Rueben Schulz (UBC)
     * @module
     */
    static final class Spherical extends Mercator {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 2383414176395616561L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param other  The other projection (usually ellipsoidal) from which to copy the parameters.
         */
        Spherical(final Mercator other) {
            super(other);
            if (type != PSEUDO) {
                ensureSpherical();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final boolean derivate) throws ProjectionException
        {
            final double λ = srcPts[srcOff  ];
            final double φ = srcPts[srcOff+1];
            /*
             * Projection of zero is zero. However the formulas below have a slight rounding error
             * which produce values close to 1E-10, so we will avoid them when y=0. In addition of
             * avoiding rounding error, this also preserve the sign (positive vs negative zero).
             */
            final double y;
            if (φ == 0) {
                y = φ;
            } else {
                // See class javadoc for a note about explicit check for poles.
                final double a = abs(φ);
                if (a < PI/2) {
                    y = log(tan(PI/4 + 0.5*φ));    // Part of Snyder (7-2)
                } else {
                    y = copySign(a <= (PI/2 + ANGULAR_TOLERANCE) ? POSITIVE_INFINITY : NaN, φ);
                }
            }
            Matrix derivative = null;
            if (derivate) {
                derivative = new Matrix2(1, 0, 0, 1/cos(φ));
            }
            /*
             * Following part is common to all spherical projections: verify, store and return.
             */
            assert (type == PSEUDO) || (Assertions.checkDerivative(derivative, super.transform(srcPts, srcOff, dstPts, dstOff, derivate))
                    && Assertions.checkTransform(dstPts, dstOff, λ, y)); // dstPts = result from ellipsoidal formulas.
            if (dstPts != null) {
                dstPts[dstOff  ] = λ;
                dstPts[dstOff+1] = y;
            }
            return derivative;
        }

        /**
         * {@inheritDoc}
         *
         * <div class="note"><b>Note:</b>
         * This method must be overridden because the {@link Mercator} class overrides the {@link NormalizedProjection}
         * default implementation.</div>
         */
        @Override
        public void transform(final double[] srcPts, int srcOff,
                              final double[] dstPts, int dstOff, int numPts)
                throws TransformException
        {
            if (srcPts != dstPts || srcOff != dstOff) {
                super.transform(srcPts, srcOff, dstPts, dstOff, numPts);
            } else {
                dstOff--;
                while (--numPts >= 0) {
                    final double φ = dstPts[dstOff += 2];   // Same as srcPts[srcOff + 1].
                    if (φ != 0) {
                        // See class javadoc for a note about explicit check for poles.
                        final double a = abs(φ);
                        final double y;
                        if (a < PI/2) {
                            y = log(tan(PI/4 + 0.5*φ));     // Part of Snyder (7-2)
                        } else {
                            y = copySign(a <= (PI/2 + ANGULAR_TOLERANCE) ? POSITIVE_INFINITY : NaN, φ);
                        }
                        dstPts[dstOff] = y;
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void inverseTransform(final double[] srcPts, final int srcOff,
                                        final double[] dstPts, final int dstOff)
                throws ProjectionException
        {
            double x = srcPts[srcOff  ];
            double y = srcPts[srcOff+1];
            y = PI/2 - 2 * atan(exp(-y));     // Part of Snyder (7-4)
            assert (type == PSEUDO) || checkInverseTransform(srcPts, srcOff, dstPts, dstOff, x, y);
            dstPts[dstOff  ] = x;
            dstPts[dstOff+1] = y;
        }

        /**
         * Computes using ellipsoidal formulas and compare with the
         * result from spherical formulas. Used in assertions only.
         */
        private boolean checkInverseTransform(final double[] srcPts, final int srcOff,
                                              final double[] dstPts, final int dstOff,
                                              final double λ, final double φ)
                throws ProjectionException
        {
            super.inverseTransform(srcPts, srcOff, dstPts, dstOff);
            return Assertions.checkInverseTransform(dstPts, dstOff, λ, φ);
        }
    }
}
