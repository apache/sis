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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.internal.referencing.provider.Mercator2SP;
import org.apache.sis.internal.referencing.provider.MercatorSpherical;
import org.apache.sis.internal.referencing.provider.RegionalMercator;
import org.apache.sis.internal.referencing.provider.PseudoMercator;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.Workaround;

import static java.lang.Math.*;
import static java.lang.Double.*;
import static org.apache.sis.math.MathFunctions.isPositive;
import static org.apache.sis.internal.util.DoubleDouble.verbatim;


/**
 * <cite>Mercator Cylindrical</cite> projection (EPSG codes 9804, 9805, 1026, 1024, 1044, <span class="deprecated">9841</span>).
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
public class Mercator extends ConformalProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2564172914329253286L;

    /**
     * Codes for variants of Mercator projection. Those variants modify the way the projections are constructed
     * (e.g. in the way parameters are interpreted), but formulas are basically the same after construction.
     * Those variants are not exactly the same than variants A, B and C used by EPSG, but they are related.
     *
     * <p>We do not provide such codes in public API because they duplicate the functionality of
     * {@link OperationMethod} instances. We use them only for constructors convenience.</p>
     *
     * <p><b>CONVENTION:</b> <strong>Spherical cases must be odd, all other cases must be even.</strong>
     * This allow us to perform quick checks for all spherical cases using {@code if ((type & SPHERICAL) != 0)}.</p>
     *
     * @see #getVariant(OperationMethod)
     */
    private static final byte SPHERICAL = 1, PSEUDO = 3,    // Must be odd and SPHERICAL must be 1.
                              REGIONAL  = 2, MILLER = 4;    // Must be even.

    /**
     * Returns the variant of the projection based on the name and identifier of the given operation method.
     */
    private static byte getVariant(final OperationMethod method) {
        if (identMatch(method, "(?i).*\\bvariant\\s*C\\b.*", RegionalMercator .IDENTIFIER)) return REGIONAL;
        if (identMatch(method, "(?i).*\\bSpherical\\b.*",    MercatorSpherical.IDENTIFIER)) return SPHERICAL;
        if (identMatch(method, "(?i).*\\bPseudo.*",          PseudoMercator   .IDENTIFIER)) return PSEUDO;
        if (identMatch(method, "(?i).*\\bMiller.*",          null))                         return MILLER;
        return 0;
    }

    /**
     * The type of Mercator projection. Possible values are:
     * <ul>
     *   <li>0                  if this projection is a Mercator variant A or B.</li>
     *   <li>{@link #REGIONAL}  if this projection is the "Mercator (variant C)" case.</li>
     *   <li>{@link #SPHERICAL} if this projection is the "Mercator (Spherical)" case.</li>
     *   <li>{@link #PSEUDO}    if this projection is the "Pseudo Mercator" case.</li>
     *   <li>{@link #MILLER}    if this projection is the "Miller Cylindrical" case.</li>
     * </ul>
     *
     * Other cases may be added in the future.
     *
     * @see #getVariant(OperationMethod)
     */
    private final byte variant;

    /**
     * Creates a Mercator projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><cite>"Mercator (variant A)"</cite>, also known as <cite>"Mercator (1SP)"</cite>.</li>
     *   <li><cite>"Mercator (variant B)"</cite>, also known as <cite>"Mercator (2SP)"</cite>.</li>
     *   <li><cite>"Mercator (variant C)"</cite>.</li>
     *   <li><cite>"Mercator (Spherical)"</cite>.</li>
     *   <li><cite>"Popular Visualisation Pseudo Mercator"</cite>.</li>
     *   <li><cite>"Miller Cylindrical"</cite>.</li>
     * </ul>
     *
     * @param method     Description of the projection parameters.
     * @param parameters The parameter values of the projection to create.
     */
    public Mercator(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @SuppressWarnings("fallthrough")
    @Workaround(library="JDK", version="1.7")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final byte variant = getVariant(method);
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles =
                new EnumMap<ParameterRole, ParameterDescriptor<Double>>(ParameterRole.class);
        /*
         * "Longitude of origin" is a parameter of all Mercator projections, but is intentionally omitted from
         * this map because it will be handled in a special way by the Mercator constructor. The "scale factor"
         * is not formally a "Mercator 2SP" argument, but we accept it anyway for all Mercator projections
         * since it may be used in some Well Known Text (WKT).
         */
        roles.put(ParameterRole.SCALE_FACTOR, Mercator1SP.SCALE_FACTOR);
        switch (variant) {
            case REGIONAL: {
                roles.put(ParameterRole.FALSE_EASTING,  RegionalMercator.EASTING_AT_FALSE_ORIGIN);
                roles.put(ParameterRole.FALSE_NORTHING, RegionalMercator.NORTHING_AT_FALSE_ORIGIN);
                break;
            }
            case SPHERICAL: {
                /*
                 * According to EPSG guide, the latitude of conformal sphere radius should be the latitude of origin.
                 * However that origin is fixed to 0° by EPSG guide, which makes radius calculation ineffective when
                 * using the official parameters. We could fallback on the standard parallel (φ1) if φ0 is not set,
                 * but for now we wait to see for real cases. Some arguments that may be worth consideration:
                 *
                 *   - The standard parallel is not an EPSG parameter for Spherical case.
                 *   - Users who set the standard parallel anyway may expect that latitude to be used for radius
                 *     calculation, since standard parallels are also known as "latitude of true scale".
                 *   - Using the standard parallel instead than the latitude of origin would be consistent
                 *     with what EPSG does for the Equirectangular projection.
                 *
                 * Anyway, this choice matters only when the user request explicitely spherical formulas applied
                 * on an ellipsoidal figure of the Earth, which should be very rare.
                 */
                roles.put(ParameterRole.LATITUDE_OF_CONFORMAL_SPHERE_RADIUS, Mercator1SP.LATITUDE_OF_ORIGIN);
                // Fall through
            }
            default: {
                roles.put(ParameterRole.FALSE_EASTING,  Mercator1SP.FALSE_EASTING);
                roles.put(ParameterRole.FALSE_NORTHING, Mercator1SP.FALSE_NORTHING);
                break;
            }
        }
        return new Initializer(method, parameters, roles, variant);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private Mercator(final Initializer initializer) {
        super(initializer);
        super.computeCoefficients();
        this.variant = initializer.variant;
        /*
         * The "Longitude of natural origin" parameter is found in all Mercator projections and is mandatory.
         * Since this is usually the Greenwich meridian, the default value is 0°. We keep the value in degrees
         * for now; it will be converted to radians later.
         */
        final double λ0 = initializer.getAndStore(Mercator1SP.LONGITUDE_OF_ORIGIN);
        /*
         * The "Latitude of natural origin" is not formally a parameter of Mercator projection. But the parameter
         * is included for completeness in CRS labelling, with the restriction (specified in EPSG documentation)
         * that the value must be zero. The EPSG dataset provides this parameter for "Mercator variant A" (1SP),
         * but Apache SIS accepts it also for other projections because we found some Well Known Text (WKT) strings
         * containing it.
         *
         * According EPSG documentation, the only exception to the above paragraph is "Mercator variant C", where
         * the parameter is named "Latitude of false origin" and can have any value. While strictly speaking the
         * "Latitude of origin" can not have a non-zero value, if it still have non-zero value we will process as
         * for "Latitude of false origin".
         */
        final double φ0 = toRadians(initializer.getAndStore((variant == REGIONAL)
                ? RegionalMercator.LATITUDE_OF_FALSE_ORIGIN : Mercator1SP.LATITUDE_OF_ORIGIN));
        /*
         * In theory, the "Latitude of 1st standard parallel" and the "Scale factor at natural origin" parameters
         * are mutually exclusive. The former is for projections of category "2SP" (namely variant B and C) while
         * the later is for projections "1SP" (namely variant A and spherical). However we let users specify both
         * if they really want, since we sometime see such CRS definitions.
         */
        final double φ1 = toRadians(initializer.getAndStore(Mercator2SP.STANDARD_PARALLEL));
        final Number k0 = verbatim(initializer.scaleAtφ(sin(φ1), cos(φ1)));
        /*
         * In principle we should rotate the central meridian (λ0) in the normalization transform, as below:
         *
         *     context.normalizeGeographicInputs(λ0);   // Actually done by the super-class constructor.
         *
         * However in the particular case of Mercator projection, we will apply the longitude rotation in the
         * denormalization matrix instead.   This is possible only for this particular projection because the
         * 'transform(…)' methods pass the longitude values unchanged. By keeping the normalization affine as
         * simple as possible, we increase the chances of efficient concatenation of an inverse with a forward
         * projection.
         */
        final MatrixSIS normalize   = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(0, k0, null);
        denormalize.convertBefore(1, k0, null);
        if (λ0 != 0) {
            /*
             * Use double-double arithmetic here for consistency with the work done in the normalization matrix.
             * The intend is to have exact value at 'double' precision when computing Matrix.invert(). Note that
             * there is no such goal for other parameters computed from sine or consine functions.
             */
            final DoubleDouble offset = DoubleDouble.createDegreesToRadians();
            offset.multiply(-λ0);
            denormalize.convertBefore(0, null, offset);
        }
        if (φ0 != 0) {
            denormalize.convertBefore(1, null, verbatim(-log(expOfNorthing(φ0, eccentricity * sin(φ0)))));
        }
        if (variant == MILLER) {
            normalize  .convertBefore(1, 0.80, null);
            denormalize.convertBefore(1, 1.25, null);
        }
        /*
         * At this point we are done, but we add here a little bit a maniac precision hunting.
         * The Mercator equations have naturally a very slight better precision in the South
         * hemisphere, because of the following term:
         *
         *     tan(π/4 + φ/2)        which implies        tan( 0 )   when   φ = -90°    (south pole)
         *                                                tan(π/2)   when   φ = +90°    (north pole)
         *
         * The case for the North pole has no exact representation. Furthermore IEEE 754 arithmetic has
         * better precision for values close to zero, which favors the South hemisphere in the above term.
         * The code below reverses the sign of latitudes before the map projection, then reverses the sign
         * of results after the projection. This has the effect of interchanging the favorized hemisphere.
         * But we do that only if the latitude given in parameter is positive. In other words, we favor the
         * hemisphere of the latitude given in parameters.
         *
         * Other libraries do something equivalent by rewriting the equations, e.g. using  tan(π/4 - φ/2)
         * and modifying the other equations accordingly. This favors the North hemisphere instead of the
         * South one, but this favoritism is hard-coded. The Apache SIS design with normalization matrices
         * allows a more dynamic approach, which we apply here.
         *
         * This "precision hunting" is optional. If something seems to go wrong, it is safe to comment all
         * those remaning lines of code.
         */
        if (φ0 == 0 && isPositive(φ1 != 0 ? φ1 : φ0)) {
            final Number reverseSign = verbatim(-1);
            normalize  .convertBefore(1, reverseSign, null);
            denormalize.convertBefore(1, reverseSign, null);  // Must be before false easting/northing.
        }
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    Mercator(final Mercator other) {
        super(other);
        variant = other.variant;
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
        Mercator kernel = this;
        if ((variant & SPHERICAL) != 0 || eccentricity == 0) {
            kernel = new Spherical(this);
        }
        return context.completeTransform(factory, kernel);
    }

    /**
     * Converts the specified (λ,φ) coordinate (units in radians) and stores the result in {@code dstPts}
     * (linear distance on a unit sphere). In addition, opportunistically computes the projection derivative
     * if {@code derivate} is {@code true}.
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
        final double φ    = srcPts[srcOff+1];
        final double sinφ = sin(φ);
        if (dstPts != null) {
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
                    y = log(expOfNorthing(φ, eccentricity * sinφ));     // Snyder (7-7)
                } else {
                    y = copySign(a <= (PI/2 + ANGULAR_TOLERANCE) ? POSITIVE_INFINITY : NaN, φ);
                }
            }
            dstPts[dstOff  ] = srcPts[srcOff];   // Scale will be applied by the denormalization matrix.
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
                        y = log(expOfNorthing(φ, eccentricity * sin(φ)));
                    } else {
                        y = copySign(a <= (PI/2 + ANGULAR_TOLERANCE) ? POSITIVE_INFINITY : NaN, φ);
                    }
                    dstPts[dstOff] = y;
                }
            }
        }
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
     * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
     * @author  Rueben Schulz (UBC)
     * @since   0.6
     * @version 0.6
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
         * @param other The other projection (usually ellipsoidal) from which to copy the parameters.
         */
        Spherical(final Mercator other) {
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
            final double φ = srcPts[srcOff+1];
            if (dstPts != null) {
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
                dstPts[dstOff  ] = srcPts[srcOff];
                dstPts[dstOff+1] = y;
            }
            return derivate ? new Matrix2(1, 0, 0, 1/cos(φ)) : null;
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
            final double y = srcPts[srcOff+1];           // Must be before writing x.
            dstPts[dstOff  ] = srcPts[srcOff];           // Must be before writing y.
            dstPts[dstOff+1] = PI/2 - 2*atan(exp(-y));  // Part of Snyder (7-4);
        }
    }
}
