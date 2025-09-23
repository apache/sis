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
import java.util.Optional;
import java.util.regex.Pattern;
import static java.lang.Math.*;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.DomainDefinition;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.provider.PolarStereographicA;
import org.apache.sis.referencing.operation.provider.PolarStereographicB;
import org.apache.sis.referencing.operation.provider.PolarStereographicC;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.measure.Latitude;
import org.apache.sis.math.MathFunctions;
import static org.apache.sis.referencing.internal.shared.Formulas.fastHypot;


/**
 * <cite>Polar Stereographic</cite> projection (EPSG codes 9810, 9829, 9830).
 * This is a special case of {@link ObliqueStereographic} when the projection origin is at a pole.
 *
 * <p>EPSG defines three variants for this projection, <i>A</i>, <i>B</i> and <i>C</i>,
 * which differ by the way the parameters are specified. The <q>Polar Stereographic (variant B)</q>
 * projection includes a <q>Latitude of standard parallel</q> parameter where is effective the scale factor
 * (normally 1). The <q>Polar Stereographic (variant A)</q> forces its <q>Latitude of natural origin</q>
 * parameter to ±90°, depending on the hemisphere.</p>
 *
 * @author  Gerald Evenden (USGS)
 * @author  André Gosselin (MPO)
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @author  Rémi Maréchal (Geomatys)
 *
 * @see ObliqueStereographic
 */
public class PolarStereographic extends ConformalProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6635298308431138524L;

    /**
     * Variants of Polar Stereographic projection. Those variants modify the way the projections are constructed
     * (e.g. in the way parameters are interpreted), but formulas are basically the same after construction.
     * Those variants are not exactly the same as variants A, B and C used by EPSG, but they are closely related.
     *
     * <p>We do not provide such codes in public API because they duplicate the functionality of
     * {@link OperationMethod} instances. We use them only for constructors convenience.</p>
     *
     * <p>The default case is {@link #B}.</p>
     */
    private enum Variant implements ProjectionVariant {
        /** The <q>Polar Stereographic (Variant A)</q> projection. */
        A(".*\\bvariant\\s*A\\b.*",  PolarStereographicA.IDENTIFIER),

        /** The <q>Polar Stereographic (Variant B)</q> projection. */
        B(".*\\bvariant\\s*B\\b.*",  PolarStereographicB.IDENTIFIER),

        /** The <q>Polar Stereographic (Variant C)</q> projection. */
        C(".*\\bvariant\\s*C\\b.*",  PolarStereographicC.IDENTIFIER),

        /** <q>Stereographic North Pole</q> projection (ESRI). */
        NORTH(".*\\bNorth\\b.*", null),

        /** <q>Stereographic South Pole</q> projection (ESRI). */
        SOUTH(".*\\bSouth\\b.*", null);

        /** Name pattern for this variant.    */ private final Pattern operationName;
        /** EPSG identifier for this variant. */ private final String  identifier;
        /** Creates a new enumeration value.  */
        private Variant(final String operationName, final String identifier) {
            this.operationName = Pattern.compile(operationName, Pattern.CASE_INSENSITIVE);
            this.identifier    = identifier;
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
     * Creates a Polar Stereographic projection from the given parameters.
     * The {@code method} argument can be the description of one of the following:
     *
     * <ul>
     *   <li><q>Polar Stereographic (Variant A)</q> (EPSG:9810).</li>
     *   <li><q>Polar Stereographic (Variant B)</q> (EPSG:9829).</li>
     *   <li><q>Polar Stereographic (Variant C)</q> (EPSG:9830).</li>
     *   <li><q>Stereographic North Pole</q> (ESRI).</li>
     *   <li><q>Stereographic South Pole</q> (ESRI).</li>
     * </ul>
     *
     * @param method      description of the projection parameters.
     * @param parameters  the parameter values of the projection to create.
     */
    public PolarStereographic(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final Variant variant = variant(method, Variant.values(), Variant.B);
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        ParameterDescriptor<Double> falseEasting  = PolarStereographicA.FALSE_EASTING;
        ParameterDescriptor<Double> falseNorthing = PolarStereographicA.FALSE_NORTHING;
        if (variant == Variant.C) {
            falseEasting  = PolarStereographicC.EASTING_AT_FALSE_ORIGIN;
            falseNorthing = PolarStereographicC.NORTHING_AT_FALSE_ORIGIN;
        }
        roles.put(ParameterRole.FALSE_EASTING,    falseEasting);
        roles.put(ParameterRole.FALSE_NORTHING,   falseNorthing);
        roles.put(ParameterRole.SCALE_FACTOR,     PolarStereographicA.SCALE_FACTOR);
        roles.put(ParameterRole.CENTRAL_MERIDIAN, (variant == Variant.A)
                ? PolarStereographicA.LONGITUDE_OF_ORIGIN
                : PolarStereographicB.LONGITUDE_OF_ORIGIN);
        return new Initializer(method, parameters, roles, variant);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private PolarStereographic(final Initializer initializer) {
        super(initializer);
        final Variant variant = (Variant) initializer.variant;
        /*
         * "Standard parallel" and "Latitude of origin" should be mutually exclusive,
         * but this is not a strict requirement for the constructor.
         * Expected parameter:
         *
         *   ┌───────────────────────────────────┬────────────────────┬─────────────┐
         *   │ Projection                        │ Parameter          │ Pole        │
         *   ├───────────────────────────────────┼────────────────────┼─────────────┤
         *   │ Polar Stereographic (variant A)   │ Latitude of origin │ auto detect │
         *   │ Polar Stereographic (variant B)   │ Standard Parallel  │ auto detect │
         *   │ Polar Stereographic (variant C)   │ Standard Parallel  │ auto detect │
         *   │ Stereographic North Pole          │ Standard Parallel  │ North pole  │
         *   │ Stereographic South Pole          │ Standard Parallel  │ South pole  │
         *   └───────────────────────────────────┴────────────────────┴─────────────┘
         */
        double φ0;
        if (variant == Variant.A) {
            φ0 = initializer.getAndStore(PolarStereographicA.LATITUDE_OF_ORIGIN);   // Mandatory
        } else {
            φ0 = initializer.getAndStore(PolarStereographicA.LATITUDE_OF_ORIGIN,    // Optional (should not be present)
                    (variant == Variant.NORTH) ? Latitude.MAX_VALUE :
                    (variant == Variant.SOUTH) ? Latitude.MIN_VALUE : Double.NaN);
        }
        if (abs(abs(φ0) - Latitude.MAX_VALUE) > Formulas.ANGULAR_TOLERANCE) {       // Can be only -90°, +90° or NaN
            throw new IllegalArgumentException(Resources.format(Resources.Keys.IllegalParameterValue_2,
                    PolarStereographicA.LATITUDE_OF_ORIGIN.getName(), φ0));
        }
        double φ1;
        if (variant == Variant.B || variant == Variant.C || Double.isNaN(φ0)) {
            φ1 = initializer.getAndStore(PolarStereographicB.STANDARD_PARALLEL);        // Mandatory
        } else {
            φ1 = initializer.getAndStore(PolarStereographicB.STANDARD_PARALLEL, φ0);    // Optional
        }
        /*
         * At this point we should ensure that the sign of φ0 is the same as the sign of φ1,
         * since opposite signs produce too large deformations. But we do not verify because
         * it is normally not possible to specify both φ0 and φ1 with the SIS parameter descriptors.
         * It may be possible to specify φ0 and φ1 if the caller used his own parameter descriptor,
         * in which case maybe he really wanted different sign (e.g. for testing purpose).
         */
        final boolean isNorth = MathFunctions.isPositive(φ1);
        if (isNorth) {
            /*
             * The South case has the most "natural" formulas. For the North case, we use the same formulas
             * with only the sign reversed before and after projection. This sign reversal is done in the
             * (de)normalization matrices at the end of this method. But we need to apply the same politic
             * on the parameters that we will use below.
             */
            φ1 = -φ1;
        }
        φ1 = toRadians(φ1);     // May be anything in [-π/2 … 0] range.
        final Number ρ, ρF;     // This ρF is actually -ρF in EPSG guide.
        if (abs(φ1 + PI/2) < ANGULAR_TOLERANCE) {
            /*
             * Polar Stereographic (variant A)
             * True scale at pole (part of Snyder 21-33). From EPSG guide (April 2015) §1.3.7.2:
             *
             *    ρ = 2⋅a⋅k₀⋅t / √[(1+ℯ)^(1+ℯ) ⋅ (1–ℯ)^(1–ℯ)]
             *
             * In this implementation, we omit:
             *    - the `a` and `k₀` factors, because they are handled outside this class,
             *    - the `t` factor, because it needs to be computed in the transform(…) method.
             *
             * In the spherical case, should give ρ == 2.
             */
            ρ = 2 / sqrt(pow(1+eccentricity, 1+eccentricity)
                       * pow(1-eccentricity, 1-eccentricity));
            ρF = null;
        } else {
            /*
             * Polar Stereographic (variant B or C)
             * Derived from Snyder 21-32 and 21-33. From EPSG guide (April 2015) §1.3.7.2:
             *
             *   tF = tan(π/4 + φ1/2) / {[(1 + ℯ⋅sinφ1) / (1 – ℯ⋅sinφ1)]^(ℯ/2)}
             *   mF = cosφ1 / √[1 – ℯ²⋅sin²φ1]
             *   k₀ = mF⋅√[(1+ℯ)^(1+ℯ) ⋅ (1–ℯ)^(1–ℯ)] / (2⋅tF)
             *
             * In our case:
             *
             *   tF = expΨ(φ1, ℯ⋅sinφ1)
             *   mF = cos(φ1) / rν(sinφ1)
             *   ρ  = mF / tF
             *   k₀ = ρ⋅√[…]/2  but we do not need that value.
             *
             * In the spherical case, should give ρ = 1 + sinφ1   (Snyder 21-7 and 21-11).
             */
            final double sinφ1 = sin(φ1);
            final double mF = initializer.scaleAtφ(sinφ1, cos(φ1));
            ρ  = mF / expΨ(φ1, eccentricity*sinφ1);
            ρF = (variant == Variant.C) ? -mF : null;
        }
        /*
         * At this point, all parameters have been processed. Now process to their
         * validation and the initialization of (de)normalize affine transforms.
         */
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        denormalize.convertBefore(0, ρ, null);
        denormalize.convertBefore(1, ρ, ρF);
        if (isNorth) {
            final MatrixSIS normalize = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
            final Number reverseSign = DoubleDouble.of(-1);
            normalize  .convertAfter (1, reverseSign, null);
            denormalize.convertBefore(1, reverseSign, null);
        }
    }

    /**
     * Creates a new projection initialized to the same parameters as the given one.
     */
    PolarStereographic(final PolarStereographic other) {
        super(other);
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
        PolarStereographic kernel = this;
        if (eccentricity == 0) {
            kernel = new Spherical(this);
        }
        return context.completeTransform(parameters.getFactory(), kernel);
    }

    /**
     * Returns the domain of input coordinates.
     * The limits defined by this method are arbitrary and may change in any future implementation.
     * Current implementation sets a longitude range of ±180° (i.e. the world) and a latitude range
     * from pole to equator in the hemisphere of the projection.
     */
    @Override
    public Optional<Envelope> getDomain(final DomainDefinition criteria) {
        return Optional.of(new Envelope2D(null, -PI, -PI/2, 2*PI, PI/2));
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
        /*
         * Note: formulas below are very similar to LambertConicConformal.transform(…) with n = -1.
         */
        final double θ    = srcPts[srcOff  ];   // θ = λ - λ₀
        final double φ    = srcPts[srcOff+1];   // Sign may be reversed
        final double sinθ = sin(θ);
        final double cosθ = cos(θ);
        final double sinφ = sin(φ);
        /*
         * From EPSG guide:
         *
         *    t = tan(π/4 + φ/2) / {[(1 + ℯ⋅sinφ)  /  (1 – ℯ⋅sinφ)]^(ℯ/2)}
         *
         * The next step is to compute ρ = 2⋅a⋅k₀⋅t / …, but those steps are
         * applied by the denormalization matrix and shall not be done here.
         */
        final double t = expΨ(φ, eccentricity*sinφ);
        final double x = t * sinθ;
        final double y = t * cosθ;
        if (dstPts != null) {
            dstPts[dstOff  ] = x;
            dstPts[dstOff+1] = y;
        }
        if (!derivate) {
            return null;
        }
        /*
         * End of map projection. Now compute the derivative.
         */
        final double dt = t * dy_dφ(sinφ, cos(φ));
        return new Matrix2(y, dt*sinθ,   // ∂x/∂λ , ∂x/∂φ
                          -x, dt*cosθ);  // ∂y/∂λ , ∂y/∂φ
    }

    /**
     * Converts the specified (x,y) coordinates and stores the result in {@code dstPts} (angles in radians).
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
        dstPts[dstOff  ] = atan2(x, y);         // Really (x,y), not (y,x)
        dstPts[dstOff+1] = -φ(fastHypot(x, y));
    }


    /**
     * Provides the transform equations for the spherical case of the polar stereographic projection.
     *
     * @author  Gerald Evenden (USGS)
     * @author  André Gosselin (MPO)
     * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
     * @author  Rueben Schulz (UBC)
     */
    static final class Spherical extends PolarStereographic {
        /**
         * For compatibility with different versions during deserialization.
         */
        private static final long serialVersionUID = 1655096575897215547L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param  other  the other projection (usually ellipsoidal) from which to copy the parameters.
         */
        protected Spherical(final PolarStereographic other) {
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
            final double θ    = srcPts[srcOff  ];       // θ = λ - λ₀
            final double φ    = srcPts[srcOff+1];
            final double sinθ = sin(θ);
            final double cosθ = cos(θ);
            final double t    = tan(PI/4 + 0.5*φ);
            final double x    = t * sinθ;               // Snyder 21-5
            final double y    = t * cosθ;               // Snyder 21-6
            if (dstPts != null) {
                dstPts[dstOff  ] = x;
                dstPts[dstOff+1] = y;
            }
            if (!derivate) {
                return null;
            }
            final double dt = t / cos(φ);
            return new Matrix2(y, dt*sinθ,              // ∂x/∂λ , ∂x/∂φ
                              -x, dt*cosθ);             // ∂y/∂λ , ∂y/∂φ
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void inverseTransform(final double[] srcPts, final int srcOff,
                                        final double[] dstPts, final int dstOff)
        {
            double x = srcPts[srcOff  ];
            double y = srcPts[srcOff+1];
            final double ρ = hypot(x, y);
            dstPts[dstOff  ] = atan2(x, y);        // Really (x,y), not (y,x);
            dstPts[dstOff+1] = 2*atan(ρ) - PI/2;   // (20-14) with φ1=90° and cos(y) = sin(π/2 + y).;
        }
    }
}
