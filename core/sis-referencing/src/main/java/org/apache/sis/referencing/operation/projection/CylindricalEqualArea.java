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
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.referencing.provider.Mercator1SP;
import org.apache.sis.internal.referencing.provider.LambertCylindricalEqualAreaSpherical;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.util.Workaround;

import static java.lang.Math.*;
import static org.apache.sis.internal.util.DoubleDouble.verbatim;
import static org.apache.sis.internal.referencing.provider.LambertCylindricalEqualArea.*;


/**
 * <cite>Cylindrical Equal Area</cite> projection (EPSG codes 9834, 9835).
 * This is the simplest equal-area projection.
 * This projection has various names depending on its standard parallel:
 *
 * <table class="sis">
 *   <caption>Non-exhaustive list of variants</caption>
 *   <tr><th>Name</th>                              <th>Standard parallel</th></tr>
 *   <tr><td>Lambert cylindrical equal-area</td>    <td>0°</td></tr>
 *   <tr><td>Behrmann cylindrical equal-area</td>   <td>30°</td></tr>
 *   <tr><td>Gall orthographic</td>                 <td>45°</td></tr>
 *   <tr><td>Balthasart</td>                        <td>50°</td></tr>
 * </table>
 *
 * <div class="section">Description</div>
 * The parallels and the meridians are straight lines and cross at right angles.
 * The scale is true along standard parallels, but distortion increase greatly at other locations.
 * Distortions are so great that there is little use of this projection for world mapping purposes.
 * However this projection may be useful for computing areas.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class CylindricalEqualArea extends EqualAreaProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8840395516658904421L;

    /**
     * Returns the variant of the projection based on the name and identifier of the given operation method.
     * See {@link #variant} for the list of possible values.
     */
    private static byte getVariant(final OperationMethod method) {
        if (identMatch(method, "(?i).*\\bSpherical\\b.*", LambertCylindricalEqualAreaSpherical.IDENTIFIER)) {
            return Initializer.AUTHALIC_RADIUS;
        }
        return 0;
    }

    /**
     * The type of Cylindrical Equal Area projection. Possible values are:
     *
     * <ul>
     *   <li>0 if this projection is a default variant.</li>
     *   <li>{@link Initializer#AUTHALIC_RADIUS} if this projection is the "Lambert Cylindrical Equal Area (Spherical)"
     *       case, in which case the semi-major and semi-minor axis lengths should be replaced by the authalic radius
     *       (this replacement is performed by the {@link Initializer} constructor).</li>
     * </ul>
     *
     * Other cases may be added in the future.
     *
     * @see #getVariant(OperationMethod)
     */
    private final byte variant;

    /**
     * Creates a Cylindrical Equal Area projection from the given parameters.
     *
     * @param method     Description of the projection parameters.
     * @param parameters The parameter values of the projection to create.
     */
    public CylindricalEqualArea(final OperationMethod method, final Parameters parameters) {
        this(initializer(method, parameters));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @SuppressWarnings("fallthrough")
    @Workaround(library="JDK", version="1.7")
    private static Initializer initializer(final OperationMethod method, final Parameters parameters) {
        final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
        /*
         * "Longitude of origin" and "scale factor" are intentionally omitted from this map because they will
         * be handled in a special way. See comments in Mercator.initializer(…) method for more details.
         */
        roles.put(ParameterRole.FALSE_EASTING,  FALSE_EASTING);
        roles.put(ParameterRole.FALSE_NORTHING, FALSE_NORTHING);
        return new Initializer(method, parameters, roles, getVariant(method));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private CylindricalEqualArea(final Initializer initializer) {
        super(initializer);
        variant = initializer.variant;
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        /*
         * The longitude of origin is normally subtracted in the 'normalize' matrix. But in the particular of case
         * of this map projection we can apply -λ₀ on any matrix.  So we apply that operation on 'denormalize' for
         * consistency with the Mercator projection and for increasing the chances to have cancellation when
         * multiplying matrices together.
         */
        final double λ0 = initializer.getAndStore(LONGITUDE_OF_ORIGIN);
        if (λ0 != 0) {
            final DoubleDouble offset = DoubleDouble.createDegreesToRadians();
            offset.multiply(-λ0);
            denormalize.convertBefore(0, null, offset);
        }
        /*
         * Compute the scale factor as k₀ = cosφ₁/√(1 - ℯ²⋅sin²φ₁), multiplied by user-specified scale factor if any.
         * Explicit scale factor is not formally a Cylindrical Equal Area parameter (it is rather computed from φ₁),
         * but we nevertheless support it.
         */
        final double φ1 = toRadians(initializer.getAndStore(STANDARD_PARALLEL));
        final DoubleDouble k0 = verbatim(initializer.scaleAtφ(sin(φ1), cos(φ1)));
        k0.multiply(initializer.getAndStore(Mercator1SP.SCALE_FACTOR));
        /*
         * In most Apache SIS map projection implementations, the scale factor is handled by the super-class by
         * specifying a ParameterRole.SCALE_FACTOR. However in the case of this CylindricalEqualArea we rather
         * handle the scale factor ourselves, because we do not perform the same multiplication on both axes:
         *
         *      x shall be multiplied by k₀
         *      y shall be divided by k₀
         *
         * Furthermore we also multiply y by (1-ℯ²)/2 for avoiding the need to recompute this constant during
         * the projection of every point.
         */
        final DoubleDouble ik = new DoubleDouble(1, 0);
        ik.subtract(initializer.eccentricitySquared);
        ik.multiply(0.5, 0);                 // This line need to be cancelled when using spherical formulas.
        ik.divide(k0);
        denormalize.convertAfter(0, k0, null);
        denormalize.convertAfter(1, ik, null);
        super.computeCoefficients();
    }

    /**
     * Creates a new projection initialized to the same parameters than the given one.
     */
    CylindricalEqualArea(final CylindricalEqualArea other) {
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
        CylindricalEqualArea kernel = this;
        if (variant == Initializer.AUTHALIC_RADIUS || eccentricity == 0) {
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
            dstPts[dstOff  ] = srcPts[srcOff];      // Multiplication by k₀ will be applied by the denormalization matrix.
            dstPts[dstOff+1] = qm_ellipsoid(sinφ);  // Multiplication by (1-ℯ²)/(2k₀) will be applied by the denormalization matrix.
        }
        /*
         * End of map projection. Now compute the derivative, if requested.
         */
        return derivate ? new Matrix2(1, 0, 0, dqm_dφ(sinφ, cos(φ))) : null;
    }

    /**
     * Converts a list of coordinate point ordinal values.
     *
     * <div class="note"><b>Note:</b>
     * We override the super-class method only as an optimization in the special case where the target coordinates
     * are written at the same locations than the source coordinates. In such case, we can take advantage of the
     * fact that the λ values are not modified by the normalized Cylindrical Equal Area projection.</div>
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
                final double φ = dstPts[dstOff += 2];                   // Same as srcPts[srcOff + 1].
                dstPts[dstOff] = qm_ellipsoid(sin(φ));                  // Part of Synder equation (10-15)
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
        final double y   = srcPts[srcOff+1];            // Must be before writing x.
        dstPts[dstOff  ] = srcPts[srcOff  ];            // Must be before writing y.
        dstPts[dstOff+1] = φ(y);
        /*
         * Equation 10-26 of Synder gives β = asin(2y⋅k₀/(a⋅qPolar)).
         * In our case it simplifies to sinβ = (y/qmPolar) because:
         *
         *   - y is already multiplied by 2k₀/a because of the denormalization matrix
         *   - the missing (1-ℯ²) term in qmPolar (compared to qPolar) is in the denormalization matrix.
         *   - taking the arc sine of β is left to φ(double) function.
         */
    }


    /**
     * Provides the transform equations for the spherical case of the Cylindrical Equal Area projection.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.8
     * @version 0.8
     * @module
     */
    static final class Spherical extends CylindricalEqualArea {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 1063449347697947732L;

        /**
         * Constructs a new map projection from the parameters of the given projection.
         *
         * @param other The other projection (usually ellipsoidal) from which to copy the parameters.
         */
        Spherical(final CylindricalEqualArea other) {
            super(other);
            context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION).convertAfter(1, 2, null);
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
                dstPts[dstOff  ] = srcPts[srcOff];
                dstPts[dstOff+1] = sin(φ);
            }
            return derivate ? new Matrix2(1, 0, 0, cos(φ)) : null;
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
                    final double φ = dstPts[dstOff += 2];           // Same as srcPts[srcOff + 1].
                    dstPts[dstOff] = sin(φ);
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
            final double y = srcPts[srcOff+1];                      // Must be before writing x.
            dstPts[dstOff  ] = srcPts[srcOff];                      // Must be before writing y.
            dstPts[dstOff+1] = asin(y);
        }
    }
}
