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
package org.apache.sis.referencing.operation.transform;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.io.Serializable;
import static java.lang.Math.*;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.DoubleDouble;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.referencing.privy.DirectPositionView;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.provider.MapProjection;
import org.apache.sis.referencing.operation.provider.GeocentricToGeographic;
import org.apache.sis.referencing.operation.provider.GeographicToGeocentric;
import org.apache.sis.referencing.operation.provider.Geographic3Dto2D;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.metadata.iso.citation.Citations;
import static org.apache.sis.referencing.operation.provider.MapProjection.ECCENTRICITY;
import static org.apache.sis.referencing.operation.provider.GeocentricAffineBetweenGeographic.DIMENSION;


/**
 * Transform from two- or three- dimensional ellipsoidal coordinates to geocentric or planetocentric coordinates.
 * This transform is usually (but not necessarily) part of a conversion from
 * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic} to
 * {@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS geocentric} coordinates.
 * Each input coordinate tuple is expected to contain:
 * <ol>
 *   <li>longitude (λ) relative to the prime meridian (usually Greenwich),</li>
 *   <li>latitude (φ),</li>
 *   <li>optionally height above the ellipsoid (h).</li>
 * </ol>
 *
 * Output coordinates are as below:
 * <ul>
 *   <li>In the Cartesian case:
 *     <ol>
 *       <li>distance from Earth center on the X axis (toward the intersection of prime meridian and equator),</li>
 *       <li>distance from Earth center on the Y axis (toward the intersection of 90°E meridian and equator),</li>
 *       <li>distance from Earth center on the Z axis (toward North pole).</li>
 *     </ol>
 *   </li><li>In the spherical case:
 *     <ol>
 *       <li>spherical longitude (same as the geodetic longitude given in input),</li>
 *       <li>spherical latitude (slightly different than the geodetic latitude),</li>
 *       <li>distance from Earth center (radius).</li>
 *     </ol>
 *   </li>
 * </ul>
 *
 * The units of measurements depend on how the {@code MathTransform} has been created:
 * <ul>
 *   <li>{@code EllipsoidToCentricTransform} instances created directly by the constructor expect (λ,φ) values
 *       in radians and compute (X,Y,Z) values in units of an ellipsoid having a semi-major axis length of 1.</li>
 *   <li>Transforms created by the {@link #createGeodeticConversion createGeodeticConversion(…)} static method expect
 *       (λ,φ) values in degrees and compute (X,Y,Z) values in units of the ellipsoid axes (usually metres).</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 * @since   0.7
 */
public class EllipsoidToCentricTransform extends AbstractMathTransform implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4692632613706143460L;

    /**
     * Index of the vertical dimension in source or target coordinates.
     * This is the dimension of <var>h</var> in the geographic case and
     * the dimension of <var>R</var> in the spherical case.
     *
     * <p><b>Note for maintainers:</b> do not use this constant for every occurrences of the {@code 2} literal,
     * but only in occurrences where the value 2 is really the dimension index of <var>h</var> or <var>R</var>.
     * Do not use this constant for a <em>number</em> of dimensions equals to 2.</p>
     */
    private static final int VERTICAL_DIM = 2;

    /**
     * Number of target dimensions on which this transform operates.
     * This is fixed to 3, contrarily to the source dimensions which may be 2 or 3 depending on {@link #withHeight}.
     * This constant is also used for the <em>source</em> number of dimensions of the <em>inverse</em> transform.
     */
    private static final int NUM_CENTRIC_DIM = 3;

    /**
     * Whether the output coordinate system is Cartesian or spherical.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.5
     * @since   0.7
     */
    public enum TargetType {
        /**
         * Indicates conversions from ellipsoidal to Cartesian coordinate system.
         * Axis order is:
         *
         * <ul>
         *   <li>Geocentric <var>X</var> (toward prime meridian)</li>
         *   <li>Geocentric <var>Y</var> (toward 90° east)</li>
         *   <li>Geocentric <var>Z</var> (toward north pole)</li>
         * </ul>
         *
         * @see org.opengis.referencing.cs.EllipsoidalCS
         * @see org.opengis.referencing.cs.CartesianCS
         */
        CARTESIAN,

        /**
         * Indicates conversions from ellipsoidal to spherical coordinate system.
         * Axis order is as below (note that this is <em>not</em> the convention
         * used neither in physics (ISO 80000-2:2009) or in mathematics).
         *
         * <ul>
         *   <li>Spherical longitude (θ), also noted Ω or λ.</li>
         *   <li>Spherical latitude (Ω), also noted θ or φ′.</li>
         *   <li>Spherical radius (R).</li>
         * </ul>
         *
         * The spherical latitude is related to geodetic latitude φ by {@literal Ω(φ) = atan((1-ℯ²)⋅tan(φ))}.
         *
         * @see org.opengis.referencing.cs.EllipsoidalCS
         * @see org.opengis.referencing.cs.SphericalCS
         *
         * @since 1.5
         */
        SPHERICAL;

        /**
         * Returns the enumeration value for the given type of coordinate system.
         * The {@code cs} argument should be {@code CartesianCS.class}, {@code SphericalCS.class}
         * or a subclass of those types.
         *
         * @param  csType  the coordinate system type.
         * @return enumeration value associated to the given type.
         * @throws IllegalArgumentException if the given {@code csType} is not one of the above-documented types.
         * @since  1.5
         */
        public static TargetType of(final Class<? extends CoordinateSystem> csType) {
            if (CartesianCS.class.isAssignableFrom(csType)) return CARTESIAN;
            if (SphericalCS.class.isAssignableFrom(csType)) return SPHERICAL;
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, csType));
        }
    }

    /**
     * Internal parameter descriptor, used only for debugging purpose.
     * Created when first needed.
     *
     * @see #getParameterDescriptors()
     */
    @Debug
    private static ParameterDescriptorGroup DESCRIPTOR;

    /**
     * Minimal eccentricity value before to consider that the approximated φ value should be made more accurate
     * by the use of an iterative method. The iterative method is not needed for a planet of Earth eccentricity,
     * but become useful for planets of higher eccentricity.
     *
     * <p>Actually, the need for iteration is not just a matter of eccentricity. It is also a matter of
     * <var>h</var> values. But empirical tests suggest that with Earth's eccentricity (about 0.082),
     * the limit for <var>h</var> is quite high (close to 2000 km for a point at 30°N). This limit is
     * reduced to about 200 km for an eccentricity of 0.16. It may be possible to find a formula for
     * the limit of <var>h</var> as a function of ℯ and φ, but this has not been explored yet.</p>
     *
     * @see org.apache.sis.referencing.operation.projection.LambertConicConformal#ECCENTRICITY_THRESHOLD
     */
    private static final double ECCENTRICITY_THRESHOLD = 0.16;

    /**
     * The square of the eccentricity.
     * This is defined as ℯ² = (a²-b²)/a² where
     * <var>a</var> is the <dfn>semi-major</dfn> axis length and
     * <var>b</var> is the <dfn>semi-minor</dfn> axis length.
     */
    protected final double eccentricitySquared;

    /**
     * The <var>b</var>/<var>a</var> ratio where
     * <var>a</var> is the <i>semi-major</i> axis length and
     * <var>b</var> is the <i>semi-minor</i> axis length.
     * Since the {@code EllipsoidToCentricTransform} class works on an ellipsoid where <var>a</var> = 1
     * (because of the work performed by the normalization matrices), we just drop <var>a</var>
     * in the formulas - so this field could be written as just <var>b</var>.
     *
     * <p>This value is related to {@link #eccentricitySquared} and to the ε value used in EPSG guide
     * by (assuming <var>a</var>=1):</p>
     * <ul>
     *   <li>ℯ² = 1 - b²</li>
     *   <li>ε = ℯ²/b²</li>
     * </ul>
     */
    private final double axisRatio;

    /**
     * Whether calculation of φ should use an iterative method after the first φ approximation.
     * The current implementation sets this field to {@code true} at construction time when the eccentricity value
     * is greater than or equals to {@link #ECCENTRICITY_THRESHOLD} or when <var>h</var> needs to be computed,
     * but this policy may change in any future SIS version.
     */
    private final boolean useIterations;

    /**
     * Whether the ellipsoidal coordinate tuples include an ellipsoidal height (3D case).
     * If {@code false}, then the input coordinate tuples are expected to be two-dimensional
     * and the ellipsoidal height is assumed to be 0.
     *
     * @see #getSourceDimensions()
     * @since 1.5
     */
    protected final boolean withHeight;

    /**
     * Whether the target coordinate system is spherical rather than Cartesian.
     * If {@code true}, then axes are <var>longitude</var> usually in radians,
     * <var>latitude</var> in radians and <var>radius</var> in metres.
     *
     * @see #getTargetType()
     */
    final boolean toSphericalCS;

    /**
     * The parameters used for creating this conversion.
     * They are used for formatting <i>Well Known Text</i> (<abbr>WKT</abbr>) and error messages.
     *
     * @see #getContextualParameters()
     */
    final ContextualParameters context;

    /**
     * The inverse of this transform.
     *
     * <h4>Implementation note</h4>
     * Creation of this object is not deferred to the first call to the {@link #inverse()} method because this
     * object is lightweight and typically needed soon anyway (may be as soon as {@code ConcatenatedTransform}
     * construction time). In addition, this field is part of serialization form in order to preserve the
     * references graph.
     */
    private final Inverse inverse;

    /**
     * Creates a transform with the same parameters as the given transform,
     * but expecting two-dimensional inputs instead of three-dimensional.
     */
    private EllipsoidToCentricTransform(final EllipsoidToCentricTransform source) {
        eccentricitySquared = source.eccentricitySquared;
        axisRatio           = source.axisRatio;
        useIterations       = source.useIterations;
        toSphericalCS       = source.toSphericalCS;
        context             = source.context.redimension(2, NUM_CENTRIC_DIM);
        withHeight          = false;
        inverse             = new Inverse();
    }

    /**
     * Creates a transform from angles in radians on an ellipsoid having a semi-major axis length of 1.
     *
     * @param semiMajor   the semi-major axis length.
     * @param semiMinor   the semi-minor axis length.
     * @param unit        the unit of measurement for the semi-axes and the ellipsoidal height.
     * @param withHeight  {@code true} if source geographic coordinates include an ellipsoidal height
     *                    (i.e. are 3-D), or {@code false} if they are only 2-D.
     * @param csType      whether the target coordinate system shall be Cartesian or spherical.
     *
     * @deprecated Replaced by {@link #EllipsoidToCentricTransform(Ellipsoid, boolean, TargetType)}.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public EllipsoidToCentricTransform(final double semiMajor, final double semiMinor,
            final Unit<Length> unit, final boolean withHeight, final TargetType csType)
    {
        this(DefaultEllipsoid.createEllipsoid(Map.of(Ellipsoid.NAME_KEY, "source"), semiMajor, semiMinor, unit), withHeight, csType);
    }

    /**
     * Creates a transform from angles in radians on an ellipsoid having a semi-major axis length of 1.
     * While a full ellipsoid is specified to this constructor, only the ratio of axis lengths is used.
     * {@code EllipsoidToCentricTransform} instances expect input coordinate tuples as below:
     *
     * <ol>
     *   <li>longitudes in <strong>radians</strong> (unit constraint is relaxed in the spherical case),</li>
     *   <li>latitudes in <strong>radians</strong>,</li>
     *   <li>optionally heights above the ellipsoid, in units of an ellipsoid having a semi-major axis length of 1.</li>
     * </ol>
     *
     * Output coordinates depends on the {@linkplain #getTargetType() target type}.
     * For a Cartesian coordinate system, the output are as below,
     * in units of an ellipsoid having a semi-major axis length of 1:
     * <ol>
     *   <li>distance from Earth center on the X axis (toward the intersection of prime meridian and equator),</li>
     *   <li>distance from Earth center on the Y axis (toward the intersection of 90°E meridian and equator),</li>
     *   <li>distance from Earth center on the Z axis (toward North pole).</li>
     * </ol>
     *
     * For a spherical coordinate system, the output are spherical longitude, spherical latitude and radius with
     * longitudes in any unit of measurement since they are copied verbatim without being used in calculations.
     * It is okay to keep longitudes in degrees for avoiding rounding errors during conversions.
     *
     * <h4>Geographic to geocentric conversions</h4>
     * For converting geographic coordinates to geocentric coordinates, {@code EllipsoidToCentricTransform}
     * instances need to be concatenated with the following affine transforms:
     *
     * <ul>
     *   <li><i>Normalization</i> before {@code EllipsoidToCentricTransform}:<ul>
     *     <li>Conversion of (λ,φ) from degrees to radians</li>
     *     <li>Division of (h) by the semi-major axis length</li>
     *   </ul></li>
     *   <li><i>Denormalization</i> after {@code EllipsoidToCentricTransform}:<ul>
     *     <li>Multiplication of (X,Y,Z) by the semi-major axis length</li>
     *   </ul></li>
     * </ul>
     *
     * After {@code EllipsoidToCentricTransform} construction,
     * the full conversion chain including the above affine transforms can be created by
     * <code>{@linkplain #getContextualParameters()}.{@linkplain ContextualParameters#completeTransform
     * completeTransform}(factory, this)}</code>.
     *
     * @param source      the ellipsoid of the geographic <abbr>CRS</abbr> of the coordinates to convert.
     * @param withHeight  {@code true} if source geographic coordinates include an ellipsoidal height
     *                    (i.e. are 3-D), or {@code false} if they are only 2-D.
     * @param csType      whether the target coordinate system shall be Cartesian or spherical.
     *
     * @see #createGeodeticConversion(MathTransformFactory, Ellipsoid, boolean, TargetType)
     *
     * @since 1.5
     */
    public EllipsoidToCentricTransform(final Ellipsoid source, final boolean withHeight, final TargetType csType) {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("csType", csType);
        toSphericalCS = (csType == TargetType.SPHERICAL);
        final DefaultEllipsoid ellipsoid = DefaultEllipsoid.castOrCopy(source);
        final double semiMajor = ellipsoid.getSemiMajorAxis();
        final double semiMinor = ellipsoid.getSemiMinorAxis();
        axisRatio = semiMinor / semiMajor;
        eccentricitySquared = ellipsoid.getEccentricitySquared();
        this.withHeight = withHeight;
        useIterations = withHeight || (eccentricitySquared >= ECCENTRICITY_THRESHOLD * ECCENTRICITY_THRESHOLD);
        /*
         * Copy parameters to the ContextualParameter. Those parameters are not used directly by
         * EllipsoidToCentricTransform, but we need to store them in case the user asks for them.
         *
         * Note: There is no `DIMENSION` parameter here because no such parameter is defined by OGC 01-009.
         * Instead, this transform should be thought as always operating in 3 dimensions with a "2D to 3D"
         * step prefixed if needed. The WKT is handled in a special way for inserting that step if needed.
         */
        final Unit<Length> unit = ellipsoid.getAxisUnit();
        context = new ContextualParameters(GeographicToGeocentric.PARAMETERS, withHeight ? 3 : 2, NUM_CENTRIC_DIM);
        context.getOrCreate(MapProjection.SEMI_MAJOR).setValue(semiMajor, unit);
        context.getOrCreate(MapProjection.SEMI_MINOR).setValue(semiMinor, unit);
        /*
         * Prepare two affine transforms to be executed before and after this EllipsoidToCentricTransform:
         *
         *   - A "normalization" transform for converting degrees to radians and normalizing the height,
         *   - A "denormalization" transform for scaling (X,Y,Z) to the semi-major axis length.
         *
         * In the spherical case, the above step are modified as below:
         *
         *   - Normalize only the latitude. Longitude does not need normalization as it will pass through.
         *   - Denormalization needs to also convert radians to degrees.
         */
        final MatrixSIS normalize;
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        if (toSphericalCS) {
            normalize = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
            normalize  .convertBefore(1, DoubleDouble.DEGREES_TO_RADIANS, null);
            denormalize.convertAfter (1, DoubleDouble.RADIANS_TO_DEGREES, null);
        } else {
            normalize = context.normalizeGeographicInputs(0);
        }
        final DoubleDouble a = DoubleDouble.of(semiMajor, true);
        if (withHeight) {
            normalize.convertBefore(VERTICAL_DIM, a.inverse(), null);   // Divide ellipsoidal height by a.
        }
        for (int i = toSphericalCS ? VERTICAL_DIM : 0; i < 3; i++) {
            denormalize.convertAfter(i, a, null);
        }
        inverse = new Inverse();
    }

    /**
     * Creates a transform from geographic to geocentric coordinates.
     *
     * @param  factory     the factory to use for creating and concatenating the affine transforms.
     * @param  semiMajor   the semi-major axis length.
     * @param  semiMinor   the semi-minor axis length.
     * @param  unit        the unit of measurement for the semi-axes and the ellipsoidal height.
     * @param  withHeight  {@code true} if source geographic coordinates include an ellipsoidal height
     *                     (i.e. are 3-D), or {@code false} if they are only 2-D.
     * @param  csType      whether the target coordinate system shall be Cartesian or spherical.
     * @return the conversion from geographic to geocentric coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     *
     * @deprecated Replaced by {@link #createGeodeticConversion(MathTransformFactory, Ellipsoid, boolean, TargetType)}.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public static MathTransform createGeodeticConversion(final MathTransformFactory factory,
            final double semiMajor, final double semiMinor, final Unit<Length> unit,
            final boolean withHeight, final TargetType csType) throws FactoryException
    {
        var source = DefaultEllipsoid.createEllipsoid(Map.of(Ellipsoid.NAME_KEY, "source"), semiMajor, semiMinor, unit);
        return createGeodeticConversion(factory, source, withHeight, csType);
    }

    /**
     * Creates a transform from geographic to geocentric coordinates. This factory method combines the
     * {@code EllipsoidToCentricTransform} instance with the steps needed for converting degrees to
     * radians and expressing the results in units of the given ellipsoid.
     *
     * <p>Input coordinates are expected to contain:</p>
     * <ol>
     *   <li>longitudes in <strong>degrees</strong> (unit constraint is relaxed in the spherical case),</li>
     *   <li>latitudes in <strong>degrees</strong>,</li>
     *   <li>optionally heights above the ellipsoid, in units of the ellipsoid axis (usually metres).</li>
     * </ol>
     *
     * Output coordinates depends on the {@linkplain #getTargetType() target type}.
     * For a Cartesian coordinate system, the output coordinates are as below,
     * in units of the ellipsoid axis (usually metres):
     * <ol>
     *   <li>distance from Earth center on the X axis (toward the intersection of prime meridian and equator),</li>
     *   <li>distance from Earth center on the Y axis (toward the intersection of 90°E meridian and equator),</li>
     *   <li>distance from Earth center on the Z axis (toward North pole).</li>
     * </ol>
     *
     * For a spherical coordinate system, the output are spherical longitude, spherical latitude and radius with
     * longitude in any unit of measurement since they are copied verbatim without being used in calculations.
     *
     * @param  factory     the factory to use for creating and concatenating the affine transforms.
     * @param  source      the ellipsoid of the geographic <abbr>CRS</abbr> of the coordinates to convert.
     * @param  withHeight  {@code true} if source geographic coordinates include an ellipsoidal height
     *                     (i.e. are 3-D), or {@code false} if they are only 2-D.
     * @param  target      whether the target coordinate system shall be Cartesian or spherical.
     * @return the conversion from geographic to geocentric coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     *
     * @since 1.5
     */
    public static MathTransform createGeodeticConversion(final MathTransformFactory factory,
            final Ellipsoid source, final boolean withHeight, final TargetType target) throws FactoryException
    {
        if (Formulas.isEllipsoidal(source)) {
            var kernel = new EllipsoidToCentricTransform(source, withHeight, target);
            return kernel.context.completeTransform(factory, kernel);
        }
        /*
         * If semi-major axis length is almost equal to semi-minor axis length, uses spherical equations instead.
         * We need to add the sphere radius to the elevation before to perform spherical to Cartesian conversion.
         */
        final MatrixSIS m = Matrices.createDiagonal(NUM_CENTRIC_DIM+1, withHeight ? 4 : 3);
        m.setElement(VERTICAL_DIM, withHeight ? 3 : 2, source.getSemiMajorAxis());
        if (!withHeight) {
            m.setElement(NUM_CENTRIC_DIM, 2, 1);
        }
        final MathTransform toSpherical = factory.createAffineTransform(m);
        final MathTransform sphericalToTarget;
        switch (target) {
            case SPHERICAL: return toSpherical;
            case CARTESIAN: sphericalToTarget = SphericalToCartesian.INSTANCE.completeTransform(factory); break;
            default: throw new AssertionError(target);
        }
        return factory.createConcatenatedTransform(toSpherical, sphericalToTarget);
    }

    /**
     * Creates a transform from geographic to Cartesian geocentric coordinates (convenience method).
     * This is a shortcut for the {@linkplain #createGeodeticConversion(MathTransformFactory, Ellipsoid,
     * boolean, TargetType) above method} with a default parameter value or {@link TargetType#CARTESIAN}.
     * The Cartesian coordinate system is the most usual target of the <q>Geographic/geocentric conversions</q>
     * (EPSG:9602) operation and is the target assumed by Well Known Text (<abbr>WKT</abbr>).
     *
     * @param  factory     the factory to use for creating and concatenating the affine transforms.
     * @param  ellipsoid   the semi-major and semi-minor axis lengths with their unit of measurement.
     * @param  withHeight  {@code true} if source geographic coordinates include an ellipsoidal height
     *                     (i.e. are 3-D), or {@code false} if they are only 2-D.
     * @return the conversion from geographic to Cartesian geocentric coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static MathTransform createGeodeticConversion(final MathTransformFactory factory,
            final Ellipsoid ellipsoid, final boolean withHeight) throws FactoryException
    {
        return createGeodeticConversion(factory, ellipsoid, withHeight, TargetType.CARTESIAN);
    }

    /**
     * Returns the parameters used for creating the complete conversion. Those parameters describe a sequence
     * of <i>normalize</i> → {@code this} → <i>denormalize</i> transforms, <strong>not</strong>
     * including {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes axis swapping}.
     * Those parameters are used for formatting <i>Well Known Text</i> (<abbr>WKT</abbr>) and error messages.
     *
     * @return the parameter values for the <i>normalize</i> → {@code this} → <i>denormalize</i> chain of transforms.
     */
    @Override
    protected ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Returns a copy of internal parameter values of this {@code EllipsoidToCentricTransform} transform.
     * The returned group contains parameter values for the number of dimensions, the eccentricity and
     * the target type (Cartesian or spherical).
     *
     * <h4>Usage note</h4>
     * This method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most <abbr>GIS</abbr> applications will instead be interested in the
     * {@linkplain #getContextualParameters() contextual parameters}.
     *
     * @return a copy of the internal parameter values for this transform.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        final Parameters pg = Parameters.castOrWrap(getParameterDescriptors().createValue());
        pg.getOrCreate(ECCENTRICITY).setValue(sqrt(eccentricitySquared));
        pg.parameter("csType").setValue(getTargetType());
        pg.getOrCreate(DIMENSION).setValue(getSourceDimensions());
        return pg;
    }

    /**
     * Returns a description of the internal parameters of this {@code EllipsoidToCentricTransform} transform.
     * The returned group contains parameter descriptors for the number of dimensions and the eccentricity.
     *
     * @return a description of the internal parameters.
     */
    @Debug
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        synchronized (EllipsoidToCentricTransform.class) {
            if (DESCRIPTOR == null) {
                final ParameterBuilder builder = new ParameterBuilder().setCodeSpace(Citations.SIS, Constants.SIS);
                final ParameterDescriptor<TargetType> target = builder.setRequired(true)
                        .addName("csType").create(TargetType.class, TargetType.CARTESIAN);
                DESCRIPTOR = builder.addName("Ellipsoid (radians domain) to centric")
                        .createGroup(1, 1, ECCENTRICITY, target, DIMENSION);
            }
            return DESCRIPTOR;
        }
    }

    /**
     * Gets the dimension of input points, which is 2 or 3.
     *
     * @return 2 or 3.
     */
    @Override
    public final int getSourceDimensions() {
        return withHeight ? 3 : 2;
    }

    /**
     * Gets the dimension of output points, which is 3.
     *
     * @return always 3.
     */
    @Override
    public final int getTargetDimensions() {
        return NUM_CENTRIC_DIM;
    }

    /**
     * Returns whether the target coordinate system is Cartesian or spherical.
     *
     * @return whether the target coordinate system is Cartesian or spherical.
     */
    public final TargetType getTargetType() {
        return toSphericalCS ? TargetType.SPHERICAL : TargetType.CARTESIAN;
    }

    /**
     * Computes the derivative at the given location.
     * This method relaxes a little bit the {@code MathTransform} contract by accepting two- or three-dimensional
     * points even if the number of dimensions does not match the {@link #getSourceDimensions()} value.
     *
     * <h4>Rational</h4>
     * That flexibility on the number of dimensions is required for calculation of {@linkplain #inverse() inverse}
     * transform derivative, because that calculation needs to inverse a square matrix with all terms in it before
     * to drop the last row in the two-dimensional case.
     *
     * @param  point  the position where to evaluate the derivative.
     * @return the derivative at the specified point (never {@code null}).
     * @throws TransformException if the derivative cannot be evaluated at the specified point.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        final int dim = point.getDimension();
        final boolean wh;
        final double h;
        switch (dim) {
            default: throw mismatchedDimension("point", getSourceDimensions(), dim);
            case 3:  wh = true;  h = point.getCoordinate(VERTICAL_DIM); break;
            case 2:  wh = false; h = 0; break;
        }
        return transform(point.getCoordinate(0), point.getCoordinate(1), h, null, 0, true, wh);
    }

    /**
     * Converts the (λ,φ) or (λ,φ,<var>h</var>) geodetic coordinates to
     * to (<var>X</var>,<var>Y</var>,<var>Z</var>) geocentric coordinates,
     * and optionally returns the derivative at that location.
     *
     * @return {@inheritDoc}
     * @throws TransformException if the point cannot be transformed or
     *         if a problem occurred while calculating the derivative.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        return transform(srcPts[srcOff], srcPts[srcOff+1], withHeight ? srcPts[srcOff + VERTICAL_DIM] : 0,
                dstPts, dstOff, derivate, withHeight);
    }

    /**
     * Implementation of {@link #transform(double[], int, double[], int, boolean)}
     * with possibility to override the {@link #withHeight} value.
     *
     * @param  λ         longitude (radians, except in the spherical case which keep degrees).
     * @param  φ         latitude (radians).
     * @param  h         height above the ellipsoid divided by the length of semi-major axis.
     * @param  dstPts    the array into which the transformed coordinates are returned.
     *                   May be {@code null} if only the derivative matrix is desired.
     * @param  dstOff    the offset to the location of the transformed point that is stored in the destination array.
     * @param  derivate  {@code true} for computing the derivative, or {@code false} if not needed.
     * @param  wh        {@code true} for a 3×3 matrix, or {@code false} for a 3×2 matrix excluding the height elements.
     */
    private Matrix transform(final double λ, final double φ, final double h,
                             final double[] dstPts, final int dstOff,
                             final boolean derivate, final boolean wh)
    {
        final double cosφ  = cos(φ);
        final double sinφ  = sin(φ);
        final double ν2    = 1 / (1 - eccentricitySquared*(sinφ*sinφ));  // Square of ν (see below)
        final double ν     = sqrt(ν2);                                   // Prime vertical radius of curvature at latitude φ
        final double νℯ    = ν * (1 - eccentricitySquared);
        final double r     = ν + h;
        final double rcosφ = r * cosφ;
        final double Z     = (νℯ+h) * sinφ;
        if (!toSphericalCS) {
            final double cosλ = cos(λ);
            final double sinλ = sin(λ);
            if (dstPts != null) {
                dstPts[dstOff  ] = rcosφ  * cosλ;       // X: Toward prime meridian
                dstPts[dstOff+1] = rcosφ  * sinλ;       // Y: Toward 90° east
                dstPts[dstOff+2] = Z;                   // Z: Toward north pole
            }
            if (!derivate) {
                return null;
            }
            final double sdφ   = νℯ * ν2 + h;
            final double dX_dh = cosφ * cosλ;
            final double dY_dh = cosφ * sinλ;
            final double dX_dλ = -r * dY_dh;
            final double dY_dλ =  r * dX_dh;
            final double dX_dφ = -sdφ * (sinφ * cosλ);
            final double dY_dφ = -sdφ * (sinφ * sinλ);
            final double dZ_dφ =  sdφ * cosφ;
            if (wh) {
                return new Matrix3(dX_dλ, dX_dφ, dX_dh,
                                   dY_dλ, dY_dφ, dY_dh,
                                       0, dZ_dφ, sinφ);
            } else {
                return Matrices.create(NUM_CENTRIC_DIM, 2, new double[] {
                        dX_dλ, dX_dφ,
                        dY_dλ, dY_dφ,
                            0, dZ_dφ});
            }
        }
        /*
         * Case of spherical output coordinates.
         * This case is less common than above Cartesian case.
         */
        final double R = hypot(Z, rcosφ);
        if (dstPts != null) {
            dstPts[dstOff  ] = λ;                   // Longitude
            dstPts[dstOff+1] = atan(Z / rcosφ);     // Latitude = atan((1-ℯ²)⋅tan(φ)) when h = 0.
            dstPts[dstOff+2] = R;                   // Radius
        }
        if (!derivate) {
            return null;
        }
        final MatrixSIS derivative = Matrices.createDiagonal(NUM_CENTRIC_DIM, wh ? 3 : 2);
        final double R2 = R * R;    // May underflow.
        if (R2 != 0) {
            final double ℯ2ν2   = eccentricitySquared*ν2;
            final double rsinφ  = r * sinφ;
            final double Zsinφ  = Z * sinφ;
            final double rcos2φ = rcosφ * cosφ;
            derivative.setElement(1, 1, ((ℯ2ν2*sinφ*(νℯ*rsinφ - ν*Z) + (νℯ+h)*r)*(cosφ*cosφ) + Z*rsinφ) / R2);  // ∂Ω/∂φ
            derivative.setElement(2, 1, ((ℯ2ν2*(νℯ*Zsinφ + ν*rcos2φ) - r*r)*sinφ + Z*(νℯ+h))*cosφ / R);         // ∂R/∂φ
            if (wh) {
                derivative.setElement(1, 2, cosφ*(rsinφ - Z) / R2);     // ∂Ω/∂h
                derivative.setElement(2, 2, (Zsinφ + rcos2φ) / R);      // ∂R/∂h
            }
        }
        return derivative;
    }

    /**
     * Converts the (λ,φ) or (λ,φ,<var>h</var>) geodetic coordinates to
     * to (<var>X</var>,<var>Y</var>,<var>Z</var>) geocentric coordinates.
     * This method performs the same conversion as {@link #transform(double[], int, double[], int, boolean)},
     * but the formulas are repeated here for performance reasons.
     *
     * @throws TransformException if a point cannot be transformed.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        int srcInc = 0;
        int dstInc = 0;
        if (srcPts == dstPts) {
            final int dimSource = getSourceDimensions();
            switch (IterationStrategy.suggest(srcOff, dimSource, dstOff, NUM_CENTRIC_DIM, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * dimSource;
                    dstOff += (numPts - 1) * NUM_CENTRIC_DIM;
                    srcInc = -2 * dimSource;
                    dstInc = -6;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*dimSource);
                    srcOff = 0;
                    break;
                }
            }
        }
        while (--numPts >= 0) {
            final double λ     = srcPts[srcOff++];                                 // Longitude
            final double φ     = srcPts[srcOff++];                                 // Latitude
            final double h     = withHeight ? srcPts[srcOff++] : 0;                // Height above the ellipsoid
            final double sinφ  = sin(φ);
            final double ν     = 1/sqrt(1 - eccentricitySquared * (sinφ*sinφ));    // Prime vertical radius of curvature at latitude φ
            final double rcosφ = (ν + h) * cos(φ);
            final double Z     = (h + ν * (1 - eccentricitySquared)) * sinφ;
            if (toSphericalCS) {
                dstPts[dstOff++] = λ;                           // Longitude
                dstPts[dstOff++] = atan(Z / rcosφ);             // Latitude = atan((1-ℯ²)⋅tan(φ)) when h = 0.
                dstPts[dstOff++] = hypot(Z, rcosφ);             // Radius
            } else {
                dstPts[dstOff++] = rcosφ * cos(λ);              // X: Toward prime meridian
                dstPts[dstOff++] = rcosφ * sin(λ);              // Y: Toward 90° east
                dstPts[dstOff++] = Z;                           // Z: Toward north pole
            }
            srcOff += srcInc;
            dstOff += dstInc;
        }
    }

    /*
     * NOTE: we do not bother to override the methods expecting a `float` array because those methods should
     *       be rarely invoked. Since there is usually LinearTransforms before and after this transform, the
     *       conversion between float and double will be handled by those LinearTransforms.  If nevertheless
     *       this EllipsoidToCentricTransform is at the beginning or the end of a transformation chain,
     *       the methods inherited from the subclass will work (but may be slightly slower).
     */

    /**
     * Converts Cartesian coordinates (<var>X</var>,<var>Y</var>,<var>Z</var>) to ellipsoidal coordinates
     * (λ,φ) or (λ,φ,<var>h</var>). This method is invoked by the transform returned by {@link #inverse()}.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     *                 May be the same as {@code srcPts}.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws TransformException if the calculation does not converge.
     */
    protected void inverseTransform(final double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        int srcInc = 0;
        int dstInc = getSourceDimensions();
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, NUM_CENTRIC_DIM, dstOff, dstInc, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * NUM_CENTRIC_DIM;
                    dstOff += (numPts - 1) * dstInc;
                    srcInc = -2 * NUM_CENTRIC_DIM;
                    dstInc = -dstInc;
                    break;
                }
                default: {
                    dstPts = Arrays.copyOfRange(dstPts, dstOff, dstOff + numPts*dstInc);
                    dstOff = 0;
                    break;
                }
            }
        }
        while (--numPts >= 0) {
            final double p, λ, Z;
            if (toSphericalCS) {
                final double Ω, R;
                λ = srcPts[srcOff++];       // Spherical longitude
                Ω = srcPts[srcOff++];       // Spherical latitude
                R = srcPts[srcOff++];       // Spherical radius
                p = R * cos(Ω);             // Projection of radius in equatorial plane
                Z = R * sin(Ω);             // Toward north pole
            } else {
                final double X, Y;
                X = srcPts[srcOff++];       // Toward prime meridian
                Y = srcPts[srcOff++];       // Toward 90° east
                Z = srcPts[srcOff++];       // Toward north pole
                p = hypot(X, Y);            // Projection of radius in equatorial plane
                λ = atan2(Y, X);            // Spherical and geodetic longitude
            }
            /*
             * EPSG guide gives  q = atan((Z⋅a) / (p⋅b))
             * where in this class  a = 1  because of the normalization matrix.
             * Since the formulas use only  sin(q)  and  cos(q), we rewrite as
             *
             *    cos²(q) = 1/(1 + tan²(q))         and  cos(q)  is always positive
             *    sin²(q) = 1 - cos²(q)             and  sin(q)  has the sign of tan(q).
             */
            final double tanq  = Z / (p*axisRatio);
            final double cos2q = 1/(1 + tanq*tanq);
            final double sin2q = 1 - cos2q;
            double φ = atan((Z + copySign(eccentricitySquared * (sqrt(sin2q) * sin2q), tanq) / axisRatio) /
                            (p -          eccentricitySquared * (sqrt(cos2q) * cos2q)));
            /*
             * The above is an approximation of φ. Usually, we are done with a good approximation for a planet
             * of the same as eccentricity than Earth. Code below will be executed in a minority of cases when
             * the value of φ needs to be improved. It will also be executed if the value of h is requested,
             * because in such case we also need the value of ν.
             */
            if (useIterations) {
                int it = Formulas.MAXIMUM_ITERATIONS;
                double sinφ, iν, Δφ;
                do {
                    if (--it < 0) {
                        throw new TransformException(Resources.format(Resources.Keys.NoConvergence));
                    }
                    sinφ = sin(φ);
                    iν = sqrt(1 - eccentricitySquared * (sinφ*sinφ));
                    Δφ = φ - (φ = atan((Z + eccentricitySquared * sinφ / iν) / p));
                } while (abs(Δφ) >= Formulas.ANGULAR_TOLERANCE * (PI/180) * 0.25);
                if (withHeight) {
                    /*
                     * Close to a pole (sin(φ) = ±1), the p/cos(φ) expression become a division between
                     * two numbers very close to zero (around 10⁻¹⁴), which can result in errors of some
                     * kilometres. But the height value at the pole is simply Z minus polar axis length.
                     */
                    dstPts[dstOff+2] = (abs(sinφ) == 1) ? (abs(Z) - axisRatio) : (p/cos(φ) - 1/iν);
                }
            }
            dstPts[dstOff  ] = λ;
            dstPts[dstOff+1] = φ;
            srcOff += srcInc;
            dstOff += dstInc;
        }
    }

    /**
     * Returns the inverse of this transform. The default implementation returns a transform
     * that will delegate its work to {@link #inverseTransform(double[], int, double[], int, int)}.
     *
     * @return the conversion from geocentric or planetocentric to ellipsoidal coordinates.
     */
    @Override
    public MathTransform inverse() {
        return inverse;
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    protected int computeHashCode() {
        int code = super.computeHashCode() + Double.hashCode(eccentricitySquared);
        if (toSphericalCS) code += 71;
        if (withHeight)    code += 37;
        return code;
    }

    /**
     * Compares the specified object with this math transform for equality.
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;                            // Slight optimization
        }
        if (super.equals(object, mode)) {
            final var that = (EllipsoidToCentricTransform) object;
            return (withHeight    == that.withHeight)    &&
                   (toSphericalCS == that.toSphericalCS) &&
                   Numerics.equals(axisRatio, that.axisRatio) &&
                   Numerics.equals(eccentricitySquared, that.eccentricitySquared);
            // No need to compare the contextual parameters since this is done by super-class.
        }
        return false;
    }




    /**
     * The descriptor of the inverse transform.
     *
     * @todo Move inside {@link Inverse} after migration to JDK17 (which allows static fields in inner classes).
     */
    @Debug
    private static ParameterDescriptorGroup INVERSE_DESCRIPTOR;

    /**
     * Converts geocentric coordinates (Cartesian or spherical) to ellipsoidal coordinates (λ,φ) or (λ,φ,<var>h</var>).
     * The implementation of the inverse transform is actually provided by the enclosing class.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     */
    private final class Inverse extends AbstractMathTransform.Inverse implements Serializable {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = 6859079700241660726L;

        /**
         * Creates the inverse of the enclosing transform.
         */
        Inverse() {
        }

        /**
         * Returns the inverse of this math transform.
         */
        @Override
        public MathTransform inverse() {
            return EllipsoidToCentricTransform.this;
        }

        /**
         * Returns the same contextual parameters as in the enclosing class,
         * but with a different method name and the (de)normalization matrices inverted.
         */
        @Override
        protected ContextualParameters getContextualParameters() {
            return context.inverse(GeocentricToGeographic.PARAMETERS, null);
            // Caching is done by `context`. No need to cache in this class.
        }

        /**
         * Returns a description of the internal parameters of this inverse transform.
         */
        @Debug
        @Override
        public ParameterDescriptorGroup getParameterDescriptors() {
            synchronized (EllipsoidToCentricTransform.class) {
                if (INVERSE_DESCRIPTOR == null) {
                    INVERSE_DESCRIPTOR = ReferencingUtilities.rename(
                            EllipsoidToCentricTransform.this.getParameterDescriptors(),
                            "Centric to ellipsoid (radians domain)");
                }
                return INVERSE_DESCRIPTOR;
            }
        }

        /**
         * Returns the internal parameter values.
         * This is used only for debugging purpose.
         */
        @Debug
        @Override
        public ParameterValueGroup getParameterValues() {
            final ParameterValueGroup pg = getParameterDescriptors().createValue();
            pg.values().addAll(EllipsoidToCentricTransform.this.getParameterValues().values());
            return pg;
        }

        /**
         * Computes the derivative at the given location. We need to override this method because
         * we will inverse 3×2 matrices in a special way, with the knowledge that <var>h</var> can be set to 0.
         */
        @Override
        public Matrix derivative(final DirectPosition point) throws TransformException {
            final double[] coordinates = point.getCoordinates();
            ArgumentChecks.ensureDimensionMatches("point", NUM_CENTRIC_DIM, coordinates);
            return this.transform(coordinates, 0, coordinates, 0, true);
        }

        /**
         * Inverse transforms a single position in a list of coordinate values,
         * and optionally returns the derivative at that location.
         *
         * <p>This method delegates the derivative computation to the enclosing class, then inverses the result.
         * This is much easier than trying to compute the derivative from the formulas of this inverse transform.</p>
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                final double[] dstPts, final int dstOff,
                                final boolean derivate) throws TransformException
        {
            final double[] point;
            final int offset;
            if (derivate && (dstPts == null || !withHeight)) {
                point  = new double[NUM_CENTRIC_DIM];
                offset = 0;
            } else {
                point  = dstPts;
                offset = dstOff;
            }
            inverseTransform(srcPts, srcOff, point, offset, 1);
            if (!derivate) {
                return null;
            }
            if (dstPts != point && dstPts != null) {
                dstPts[dstOff  ] = point[0];
                dstPts[dstOff+1] = point[1];
            }
            // We need to keep h during matrix inversion because (λ,φ,h) values in the result are not independent.
            var p = new DirectPositionView.Double(point, offset, NUM_CENTRIC_DIM);
            Matrix derivative = EllipsoidToCentricTransform.this.derivative(p);
            derivative = Matrices.inverse(derivative);
            if (!withHeight) {
                // Can drop the height only after matrix inversion has been done.
                derivative = MatrixSIS.castOrCopy(derivative).removeRows(VERTICAL_DIM, VERTICAL_DIM + 1);
            }
            return derivative;
        }

        /**
         * Transforms the given array of points from geocentric to geographic coordinates.
         * This method delegates the work to the enclosing class.
         */
        @Override
        public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts)
                throws TransformException
        {
            inverseTransform(srcPts, srcOff, dstPts, dstOff, numPts);
        }

        /**
         * If this transform returns three-dimensional outputs, and if the transform just after this one
         * just drops the height values, then replaces this transform by a two-dimensional one.
         * The intent is to handle the following sequence of operations defined in the EPSG database:
         *
         * <ol>
         *   <li>Inverse of <cite>Geographic/geocentric conversions</cite> (EPSG:9602)</li>
         *   <li><cite>Geographic 3D to 2D conversion</cite> (EPSG:9659)</li>
         * </ol>
         *
         * Replacing the above sequence by a two-dimensional {@code EllipsoidToCentricTransform} instance
         * allow the following optimizations:
         *
         * <ul>
         *   <li>Avoid computation of <var>h</var> value.</li>
         *   <li>Allow use of the more efficient {@link java.awt.geom.AffineTransform} after this transform
         *       instead of a transform based on a matrix of size 3×4.</li>
         * </ul>
         */
        @Override
        protected void tryConcatenate(final Joiner context) throws FactoryException {
            if (!reduce(context, +1)) {
                super.tryConcatenate(context);
            }
        }

        /**
         * Given a transformation chain to format in WKT, inserts a "Geographic 3D to 2D" pseudo-conversion
         * after this transform (normally {@code transforms.get(index)}) if this conversion computes no height.
         *
         * @param  transforms  the full chain of concatenated transforms.
         * @param  index       the index of this transform in the {@code transforms} chain.
         * @return index of this transform in the {@code transforms} chain after processing.
         */
        @Override
        final int beforeFormat(final List<Object> transforms, int index, final boolean inverse) {
            index = super.beforeFormat(transforms, index, inverse);
            if (!withHeight) {
                transforms.add(++index, new Geographic3Dto2D.WKT(false));
            }
            return index;
        }
    }

    /**
     * Given a transformation chain to format in WKT, inserts a "Geographic 2D to 3D" pseudo-conversion
     * before this transform (normally {@code transforms.get(index)}) if this conversion expects no height.
     *
     * @param  transforms  the full chain of concatenated transforms.
     * @param  index       the index of this transform in the {@code transforms} chain.
     * @return index of this transform in the {@code transforms} chain after processing.
     */
    @Override
    final int beforeFormat(final List<Object> transforms, int index, final boolean inverse) {
        index = super.beforeFormat(transforms, index, inverse);
        if (!withHeight) {
            transforms.add(index++, new Geographic3Dto2D.WKT(true));
        }
        return index;
    }

    /**
     * If this transform expects three-dimensional inputs, and if the transform just before this one
     * unconditionally sets the height to zero, then replaces this transform by a two-dimensional one.
     * The intent is to handle the following sequence of operations defined in the EPSG database:
     *
     * <ol>
     *   <li>Inverse of <cite>Geographic 3D to 2D conversion</cite> (EPSG:9659)</li>
     *   <li><cite>Geographic/geocentric conversions</cite> (EPSG:9602)</li>
     * </ol>
     *
     * Replacing the above sequence by a two-dimensional {@code EllipsoidToCentricTransform} instance
     * allow the following optimizations:
     *
     * <ul>
     *   <li>Avoid computation of <var>h</var> value.</li>
     *   <li>Allow use of the more efficient {@link java.awt.geom.AffineTransform} before this transform
     *       instead of a transform based on a matrix of size 4×3.</li>
     * </ul>
     *
     * @param  context  information about the neighbor transforms, and the object where to set the result.
     * @throws FactoryException if an error occurred while combining the transforms.
     */
    @Override
    protected void tryConcatenate(final Joiner context) throws FactoryException {
        if (!reduce(context, -1)) {
            super.tryConcatenate(context);
        }
    }

    /**
     * If this transform expects three-dimensional inputs, and if the neighbor transform
     * ignores the vertical dimension, then replaces this transform by a two-dimensional one.
     *
     * @param  context  information about the neighbor transforms, and the object where to set the result.
     * @param  checkAt  relative matrix index: -1 for the forward transform, or +1 for the inverse transform.
     * @throws FactoryException if an error occurred while combining the transforms.
     */
    private boolean reduce(final Joiner context, final int checkAt) throws FactoryException {
        return withHeight && context.removeUnusedDimensions(checkAt, VERTICAL_DIM, VERTICAL_DIM + 1, (dimension) -> {
            if (dimension != 2) {
                return null;
            }
            var reduced = new EllipsoidToCentricTransform(this);
            return (checkAt >= 0) ? reduced.inverse() : reduced;
        });
    }
}
