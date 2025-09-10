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

import java.util.Map;
import java.util.EnumMap;
import java.util.Optional;
import java.util.regex.Pattern;
import static java.lang.Math.*;
import static java.lang.Double.*;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.operation.provider.Mercator1SP;
import org.apache.sis.referencing.operation.provider.Mercator2SP;
import org.apache.sis.referencing.operation.provider.MercatorSpherical;
import org.apache.sis.referencing.operation.provider.MercatorAuxiliarySphere;
import org.apache.sis.referencing.operation.provider.RegionalMercator;
import org.apache.sis.referencing.operation.provider.PseudoMercator;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.privy.DoubleDouble;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.DomainDefinition;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.transform.TransformJoiner;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Errors;
import static org.apache.sis.math.MathFunctions.isPositive;


/**
 * <cite>Mercator Cylindrical</cite> projection (EPSG codes 9804, 9805, 1026, 1024, 1044, <span class="deprecated">9841</span>).
 * See the following references for an overview:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Mercator_projection">Mercator projection on Wikipedia</a></li>
 *   <li><a href="https://mathworld.wolfram.com/MercatorProjection.html">Mercator projection on MathWorld</a></li>
 * </ul>
 *
 * <h2>Description</h2>
 * The parallels and the meridians are straight lines and cross at right angles; this projection thus produces
 * rectangular charts. The scale is true along the equator (by default) or along two parallels equidistant of the
 * equator (if a scale factor other than 1 is used).
 *
 * <p>This projection is used to represent areas close to the equator. It is also often used for maritime navigation
 * because all the straight lines on the chart are <dfn>loxodrome</dfn> lines, i.e. a ship following this line would
 * keep a constant azimuth on its compass.</p>
 *
 * <p>This implementation handles both the 1 and 2 standard parallel cases.
 * For <cite>Mercator (variant A)</cite> (EPSG code 9804), the line of contact is the equator.
 * For <cite>Mercator (variant B)</cite> (EPSG code 9805) lines of contact are symmetrical about the equator.</p>
 *
 * <h2>Behavior at poles</h2>
 * The projection of 90°N gives {@linkplain Double#POSITIVE_INFINITY positive infinity}.
 * The projection of 90°S gives {@linkplain Double#NEGATIVE_INFINITY negative infinity}.
 * Projection of a latitude outside the [-90 … 90]° range produces {@linkplain Double#NaN NaN}.
 *
 * @author  André Gosselin (MPO)
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @author  Simon Reynard (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 *
 * @see TransverseMercator
 * @see ObliqueMercator
 */
public class Mercator extends ConformalProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8732555724521630563L;

    /**
     * Variants of Mercator projection. Those variants modify the way the projections are constructed
     * (e.g. in the way parameters are interpreted), but formulas are basically the same after construction.
     * Those variants are not exactly the same as variants A, B and C used by EPSG, but they are related.
     *
     * <p>We do not provide such codes in public API because they duplicate the functionality of
     * {@link OperationMethod} instances. We use them only for constructors convenience.</p>
     */
    private enum Variant implements ProjectionVariant {
        // Declaration order matter. Patterns are matched in that order.

        /** The <q>Mercator (variant A)</q> projection (one standard parallel). */
        ONE_PARALLEL(".*\\bvariant\\s*A\\b.*", Mercator1SP.IDENTIFIER, false),

        /** The <q>Mercator (variant B)</q> projection (two standard parallels). */
        TWO_PARALLELS(".*\\bvariant\\s*B\\b.*", Mercator2SP.IDENTIFIER, false),

        /** The <q>Mercator (variant C)</q> projection. */
        REGIONAL(".*\\bvariant\\s*C\\b.*", RegionalMercator.IDENTIFIER, false),

        /** The <q>Mercator (Spherical)</q> projection. */
        SPHERICAL(".*\\bSpherical\\b.*", MercatorSpherical.IDENTIFIER, true),

        /** The <q>Popular Visualisation Pseudo Mercator</q> projection. */
        PSEUDO(".*\\bPseudo.*", PseudoMercator.IDENTIFIER, true),

        /** The <q>Mercator Auxiliary Sphere</q> projection. */
        AUXILIARY(".*\\bAuxiliary\\s*Sphere\\b.*", null, true),

        /** Miller projection. */
        MILLER(".*\\bMiller.*", null, false);

        /** Name pattern for this variant.    */ private final Pattern operationName;
        /** EPSG identifier for this variant. */ private final String  identifier;
        /** Whether spherical case is used.   */ final boolean spherical;
        /** Creates a new enumeration value.  */
        private Variant(final String operationName, final String identifier, final boolean spherical) {
            this.operationName = Pattern.compile(operationName, Pattern.CASE_INSENSITIVE);
            this.identifier    = identifier;
            this.spherical     = spherical;
        }

        /** The expected name pattern of an operation method for this variant. */
        @Override public Pattern getOperationNamePattern() {
            return operationName;
        }

        /** EPSG identifier of an operation method for this variant. */
        @Override public String getIdentifier() {
            return identifier;
        }
    }

    /**
     * The type of Mercator projection. Possible values are:
     * <ul>
     *   <li>{@link Variant#ONE_PARALLEL}  if this projection is a Mercator variant A.</li>
     *   <li>{@link Variant#TWO_PARALLELS} if this projection is a Mercator variant A.</li>
     *   <li>{@link Variant#REGIONAL}      if this projection is the "Mercator (variant C)" case.</li>
     *   <li>{@link Variant#SPHERICAL}     if this projection is the "Mercator (Spherical)" case.</li>
     *   <li>{@link Variant#PSEUDO}        if this projection is the "Pseudo Mercator" case.</li>
     *   <li>{@link Variant#MILLER}        if this projection is the "Miller Cylindrical" case.</li>
     *   <li>{@link Variant#AUXILIARY}     if this projection is the "Mercator Auxiliary Sphere" case.</li>
     * </ul>
     *
     * Other cases may be added in the future.
     */
    private final Variant variant;

    /**
     * Creates a Mercator projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>Mercator (variant A)</q>, also known as <q>Mercator (1SP)</q>.</li>
     *   <li><q>Mercator (variant B)</q>, also known as <q>Mercator (2SP)</q>.</li>
     *   <li><q>Mercator (variant C)</q>.</li>
     *   <li><q>Mercator (Spherical)</q>.</li>
     *   <li><q>Mercator Auxiliary Sphere</q>.</li>
     *   <li><q>Popular Visualisation Pseudo Mercator</q>.</li>
     *   <li><q>Miller Cylindrical</q>.</li>
     * </ul>
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
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
        final Variant variant = variant(method, Variant.values(), Variant.TWO_PARALLELS);
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        /*
         * The "scale factor" is not formally a "Mercator 2SP" argument, but we accept it anyway
         * for all Mercator projections because it may be used in some Well Known Text (WKT).
         */
        roles.put(ParameterRole.SCALE_FACTOR, Mercator1SP.SCALE_FACTOR);
        roles.put(ParameterRole.CENTRAL_MERIDIAN,
                (variant == Variant.REGIONAL) ? RegionalMercator.LONGITUDE_OF_FALSE_ORIGIN
                                              : Mercator1SP.LONGITUDE_OF_ORIGIN);
        switch (variant) {
            case REGIONAL: {
                roles.put(ParameterRole.FALSE_EASTING,  RegionalMercator.EASTING_AT_FALSE_ORIGIN);
                roles.put(ParameterRole.FALSE_NORTHING, RegionalMercator.NORTHING_AT_FALSE_ORIGIN);
                break;
            }
            case SPHERICAL: {
                /*
                 * According to EPSG guide, the latitude of conformal sphere radius should be the latitude of origin.
                 * However, that origin is fixed to 0° by EPSG guidance notes, which makes this radius equals to the
                 * semi-minor axis length. We could allow more flexibility by using the standard parallel (φ1) if φ0
                 * is not set, but for now we wait to see for real cases.
                 * Some arguments that may be worth consideration:
                 *
                 *   - The standard parallel is not an EPSG parameter for Spherical case.
                 *   - Users who set the standard parallel anyway may expect that latitude to be used for radius
                 *     calculation, since standard parallels are also known as "latitude of true scale".
                 *   - Using the standard parallel instead of the latitude of origin would be consistent
                 *     with what EPSG does for the Equirectangular projection.
                 *
                 * Anyway, this choice matters only when the user request explicitly spherical formulas applied
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
        variant = (Variant) initializer.variant;
        /*
         * The "Latitude of natural origin" is not formally a parameter of Mercator projection. But the parameter
         * is included for completeness in CRS labelling, with the restriction (specified in EPSG documentation)
         * that the value must be zero. The EPSG dataset provides this parameter for "Mercator variant A" (1SP),
         * but Apache SIS accepts it also for other projections because we found some Well Known Text (WKT) strings
         * containing it.
         *
         * According EPSG documentation, the only exception to the above paragraph is "Mercator variant C", where
         * the parameter is named "Latitude of false origin" and can have any value. While strictly speaking the
         * "Latitude of origin" cannot have a non-zero value, if it still have non-zero value we will process as
         * for "Latitude of false origin".
         */
        final double φ0 = toRadians(initializer.getAndStore(
                (variant == Variant.REGIONAL) ? RegionalMercator.LATITUDE_OF_FALSE_ORIGIN
                                              : Mercator1SP.LATITUDE_OF_ORIGIN));
        /*
         * In theory, the "Latitude of 1st standard parallel" and the "Scale factor at natural origin" parameters
         * are mutually exclusive. The former is for projections of category "2SP" (namely variant B and C) while
         * the latter is for projections "1SP" (namely variant A and spherical). However, we let users specify both
         * if they really want, since we sometimes see such CRS definitions.
         */
        final double φ1 = toRadians(initializer.getAndStore(Mercator2SP.STANDARD_PARALLEL));
        final Number k0 = initializer.scaleAtφ(sin(φ1), cos(φ1));
        final MatrixSIS normalize   = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(0, k0, null);
        denormalize.convertBefore(1, k0, null);
        if (φ0 != 0) {
            denormalize.convertBefore(1, null, -log(expΨ(φ0, eccentricity * sin(φ0))));
        }
        /*
         * Variants of the Mercator projection which can be handled by scale factors.
         * In the "Mercator Auxiliary Sphere" case, sphere types are:
         *
         *   0 = use semimajor axis or radius of the geographic coordinate system.
         *   1 = use semiminor axis or radius.
         *   2 = calculate and use authalic radius.
         *   3 = use authalic radius and convert geodetic latitudes to authalic latitudes.
         *       The conversion is not handled by this class and must be done by the caller.
         */
        if (variant == Variant.MILLER) {
            normalize  .convertBefore(1, 0.80, null);
            denormalize.convertBefore(1, 1.25, null);
        } else if (variant == Variant.AUXILIARY) {
            final Number ratio;
            final int type = initializer.getAndStore(MercatorAuxiliarySphere.AUXILIARY_SPHERE_TYPE, 0);
            switch (type) {
                default: {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2,
                            MercatorAuxiliarySphere.AUXILIARY_SPHERE_TYPE.getName().getCode(), type));
                }
                case AuthalicMercator.TYPE:
                case 2: ratio = initializer.authalicRadius();  break;
                case 1: ratio = initializer.axisLengthRatio(); break;
                case 0: ratio = null; break;      // Same as "Popular Visualisation Pseudo Mercator".
            }
            denormalize.convertAfter(0, ratio, null);
            denormalize.convertAfter(1, ratio, null);
        }
        /*
         * At this point we are done, but we add here a little bit a maniac precision hunting.
         * The Mercator equations have naturally a very slight better precision in the South
         * hemisphere, because of the following term:
         *
         *     tan(π/4 + φ/2)        which implies        tan( 0 )   when   φ = -90°    (south pole)
         *                                                tan(π/2)   when   φ = +90°    (north pole)
         *
         * The case for the North pole has no exact representation. Furthermore, IEEE 754 arithmetic has
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
            final Number reverseSign = DoubleDouble.of(-1);
            normalize  .convertBefore(1, reverseSign, null);
            denormalize.convertBefore(1, reverseSign, null);        // Must be before false easting/northing.
        }
    }

    /**
     * Creates a new projection initialized to the same parameters as the given one.
     */
    Mercator(final Mercator other) {
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
        NormalizedProjection kernel = this;
subst:  if (variant.spherical || eccentricity == 0) {
            if (variant == Variant.AUXILIARY && eccentricity != 0) {
                final int type = context.getValue(MercatorAuxiliarySphere.AUXILIARY_SPHERE_TYPE);
                if (type == AuthalicMercator.TYPE) {
                    kernel = new AuthalicMercator(this);
                    break subst;
                }
            }
            kernel = new Spherical(this);
        }
        return kernel.completeWithWraparound(parameters);
    }

    /**
     * Returns the domain of input coordinates. For a Mercator projection other than Miller variant,
     * the limit is arbitrarily set to 84° of latitude North and South. This is consistent with the
     * "World Mercator" domain of validity defined by EPSG:3395, which is 80°S to 84°N.
     *
     * <p>The range of longitude values is set to an arbitrary range larger than −180° … +180°,
     * because the Mercator projection is mathematically capable to handle coordinates beyond that range
     * even if those coordinates have no real world meaning. This expansion can facilitate the projection
     * of envelopes, geometries or rasters.</p>
     */
    @Override
    public Optional<Envelope> getDomain(final DomainDefinition criteria) {
        final double limit = (variant == Variant.MILLER) ? -PI/2 : -POLAR_AREA_LIMIT;
        return Optional.of(new Envelope2D(null, -LARGE_LONGITUDE_LIMIT, limit, 2*LARGE_LONGITUDE_LIMIT, -2*limit));
    }

    /**
     * Projects the specified coordinates (implementation-specific units) and stores the result in {@code dstPts}.
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
        final double φ    = srcPts[srcOff+1];
        final double sinφ = sin(φ);
        if (dstPts != null) {
            /*
             * Projection of zero is zero. However, the formulas below have a slight rounding error
             * which produce values close to 1E-10, so we will avoid them when y=0. In addition of
             * avoiding rounding error, this also preserve the sign (positive vs negative zero).
             */
            final double y;
            if (φ == 0) {
                y = φ;
            } else {
                /*
                 * See the javadoc of the Spherical inner class for a note
                 * about why we perform explicit checks for the pole cases.
                 */
                final double a = abs(φ);
                if (a < PI/2) {
                    y = log(expΨ(φ, eccentricity * sinφ));                      // Snyder (7-7)
                } else if (a <= (PI/2 + ANGULAR_TOLERANCE)) {
                    y = copySign(POSITIVE_INFINITY, φ);
                } else {
                    y = NaN;
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
     * Converts a list of coordinate tuples. This method performs the same calculation as above
     * {@link #transform(double[], int, double[], int, boolean)} method, but is overridden for efficiency.
     *
     * @throws TransformException if a point cannot be converted.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        if (srcPts != dstPts || srcOff != dstOff || getClass() != Mercator.class) {
            super.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        } else {
            /*
             * Override the super-class method only as an optimization in the special case where the target coordinates
             * are written at the same locations as the source coordinates. In such case, we can take advantage of
             * the fact that the λ values are not modified by the normalized Mercator projection.
             */
            dstOff--;
            while (--numPts >= 0) {
                final double φ = dstPts[dstOff += DIMENSION];                   // Same as srcPts[srcOff + 1].
                if (φ != 0) {
                    /*
                     * See the javadoc of the Spherical inner class for a note
                     * about why we perform explicit checks for the pole cases.
                     */
                    final double a = abs(φ);
                    final double y;
                    if (a < PI/2) {
                        y = log(expΨ(φ, eccentricity * sin(φ)));
                    } else if (a <= (PI/2 + ANGULAR_TOLERANCE)) {
                        y = copySign(POSITIVE_INFINITY, φ);
                    } else {
                        y = NaN;
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
     * @throws ProjectionException if the point cannot be converted.
     */
    @Override
    protected void inverseTransform(final double[] srcPts, final int srcOff,
                                    final double[] dstPts, final int dstOff)
            throws ProjectionException
    {
        final double y   = srcPts[srcOff+1];            // Must be before writing x.
        dstPts[dstOff  ] = srcPts[srcOff  ];            // Must be before writing y.
        dstPts[dstOff+1] = φ(exp(-y));
    }


    /**
     * Provides the transform equations for the spherical case of the Mercator projection.
     *
     * <h2>Implementation note</h2>
     * This class contains an explicit check for latitude values at a pole. If floating point arithmetic had infinite
     * precision, such checks would not be necessary since the formulas lead naturally to infinite values at poles,
     * which is the correct answer. In practice the infinite value emerges by itself at only one pole, and the other
     * one produces a high value (approximately 1E+16). This is because there is no accurate representation of π/2,
     * and consequently {@code tan(π/2)} does not return the infinite value. We workaround this issue with an explicit
     * check for abs(φ) ≊ π/2. Note that:
     *
     * <ul>
     *   <li>The arithmetic is not broken for values close to pole. We check π/2 because this is the result of
     *       converting 90°N to radians, and we presume that the user really wanted to said 90°N. But for most
     *       other values we could let the math do their "natural" work.</li>
     *   <li>For φ = -π/2 our arithmetic already produces negative infinity.</li>
     * </ul>
     *
     * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
     * @author  Rueben Schulz (UBC)
     */
    static final class Spherical extends Mercator {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 2383414176395616561L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param  other  the other projection (usually ellipsoidal) from which to copy the parameters.
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
                                final boolean derivate)
        {
            final double φ = srcPts[srcOff+1];
            if (dstPts != null) {
                /*
                 * Projection of zero is zero. However, the formulas below have a slight rounding error
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
                        y = log(tan(PI/4 + 0.5*φ));                             // Part of Snyder (7-2)
                    } else if (a <= (PI/2 + ANGULAR_TOLERANCE)) {
                        y = copySign(POSITIVE_INFINITY, φ);
                    } else {
                        y = NaN;
                    }
                }
                dstPts[dstOff  ] = srcPts[srcOff];
                dstPts[dstOff+1] = y;
            }
            return derivate ? new Matrix2(1, 0, 0, 1/cos(φ)) : null;
        }

        /**
         * Converts a list of coordinate tuples.
         * This method must be overridden because the {@link Mercator} class
         * overrides the {@link NormalizedProjection} default implementation.
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
                    final double φ = dstPts[dstOff += DIMENSION];               // Same as srcPts[srcOff + 1].
                    if (φ != 0) {
                        // See class javadoc for a note about explicit check for poles.
                        final double a = abs(φ);
                        final double y;
                        if (a < PI/2) {
                            y = log(tan(PI/4 + 0.5*φ));                         // Part of Snyder (7-2)
                        } else if (a <= (PI/2 + ANGULAR_TOLERANCE)) {
                            y = copySign(POSITIVE_INFINITY, φ);
                        } else {
                            y = NaN;
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
        {
            final double y = srcPts[srcOff+1];                      // Must be before writing x.
            dstPts[dstOff  ] = srcPts[srcOff];                      // Must be before writing y.
            dstPts[dstOff+1] = PI/2 - 2*atan(exp(-y));              // Part of Snyder (7-4);
        }
    }

    /**
     * Concatenates in an optimized way the reverse projection with its neighbor, if possible.
     * This optimization is symmetrical to the optimization done for the forward projection case.
     *
     * @param  context  information about the neighbor transforms, and the object where to set the result.
     * @throws FactoryException if an error occurred while combining the transforms.
     * @return whether or not an optimization has been done.
     */
    @Override
    final boolean tryInverseConcatenate(TransformJoiner context) throws FactoryException {
        int relativeIndex = +1;
        do {
            if (context.replaceRoundtrip(relativeIndex, (middle) -> {
                final Matrix m = MathTransforms.getMatrix(middle);
                /*
                 * Verify that the latitude row is an identity conversion except for the sign which is allowed to change
                 * (but no scale and no translation are allowed). Ignore the longitude row because it just passes through
                 * this Mercator projection with no impact on any calculation.
                 */
                return (m != null
                        && Matrices.isAffine(m)
                        && m.getNumRow() == DIMENSION+1
                        && m.getNumCol() == DIMENSION+1
                        && m.getElement(1, DIMENSION) == 0
                        && m.getElement(1, 0) == 0
                        && Math.abs(m.getElement(1,1)) == 1) ? middle : null;
            })) return true;
        } while ((relativeIndex = -relativeIndex) < 0);
        return context.replacePassThrough(Map.of(0, 0));
    }

    /**
     * Concatenates in an optimized way this projection with its neighbor, if possible.
     * If the transforms are concatenated in a (projection) → (affine) → (reverse projection) sequence
     * where the (projection) and (reverse projection) steps are the {@linkplain #inverse() inverse}
     * of each other, and if the affine transform in the middle does not change the latitude value,
     * then we can take advantage of the fact that longitude conversion is linear.
     *
     * @param  context  information about the neighbor transforms, and the object where to set the result.
     * @throws FactoryException if an error occurred while combining the transforms.
     */
    @Override
    protected final void tryConcatenate(final TransformJoiner context) throws FactoryException {
        if (!tryInverseConcatenate(context)) {
            super.tryConcatenate(context);
        }
    }
}
