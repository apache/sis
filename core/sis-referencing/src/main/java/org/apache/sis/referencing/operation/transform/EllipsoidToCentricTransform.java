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

import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.internal.referencing.provider.GeocentricToGeographic;
import org.apache.sis.internal.referencing.provider.GeographicToGeocentric;
import org.apache.sis.internal.referencing.provider.Geographic3Dto2D;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.*;
import static org.apache.sis.internal.referencing.provider.MapProjection.SEMI_MAJOR;
import static org.apache.sis.internal.referencing.provider.MapProjection.SEMI_MINOR;
import static org.apache.sis.internal.referencing.provider.MapProjection.ECCENTRICITY;
import static org.apache.sis.internal.referencing.provider.GeocentricAffineBetweenGeographic.DIMENSION;


/**
 * Transform from two- or three- dimensional ellipsoidal coordinates to (geo)centric coordinates.
 * This transform is usually (but not necessarily) part of a conversion from
 * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic} to Cartesian
 * {@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS geocentric} coordinates.
 * Each input coordinates is expected to contain:
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
 *   </li>
 * </ul>
 *
 * The units of measurements depend on how the {@code MathTransform} has been created:
 * <ul>
 *   <li>{@code EllipsoidToCentricTransform} instances created directly by the constructor expect (λ,φ) values
 *       in radians and compute (X,Y,Z) values in units of an ellipsoid having a semi-major axis length of 1.
 *       That constructor is reserved for subclasses only.</li>
 *   <li>Transforms created by the {@link #createGeodeticConversion createGeodeticConversion(…)} static method expect
 *       (λ,φ) values in degrees and compute (X,Y,Z) values in units of the ellipsoid axes (usually metres).</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class EllipsoidToCentricTransform extends AbstractMathTransform implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3352045463953828140L;

    /**
     * Whether the output coordinate system is Cartesian or Spherical.
     *
     * <p><b>TODO:</b> The spherical case is not yet implemented.
     * We could also consider supporting the cylindrical case, but its usefulness is not obvious.
     * See <a href="http://issues.apache.org/jira/browse/SIS-302">SIS-302</a>.</p>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    public static enum TargetType {
        /**
         * Indicates conversions from
         * {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS ellipsoidal} to
         * {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian} coordinate system.
         */
        CARTESIAN
    }

    /**
     * Internal parameter descriptor, used only for debugging purpose.
     * Created only when first needed.
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
     * <p>Actually the need for iteration is not just a matter of eccentricity. It is also a matter of
     * <var>h</var> values. But empirical tests suggest that with Earth's eccentricity (about 0.082),
     * the limit for <var>h</var> is quite high (close to 2000 km for a point at 30°N). This limit is
     * reduced to about 200 km for an eccentricity of 0.16. It may be possible to find a formula for
     * the limit of <var>h</var> as a function of ℯ and φ, but this has not been explored yet.</p>
     *
     * @see org.apache.sis.referencing.operation.projection.ConformalProjection#ECCENTRICITY_THRESHOLD
     */
    private static final double ECCENTRICITY_THRESHOLD = 0.16;

    /**
     * The square of eccentricity: ℯ² = (a²-b²)/a² where
     * <var>a</var> is the <cite>semi-major</cite> axis length and
     * <var>b</var> is the <cite>semi-minor</cite> axis length.
     */
    protected final double eccentricitySquared;

    /**
     * The b/a ratio where
     * <var>a</var> is the <cite>semi-major</cite> axis length and
     * <var>b</var> is the <cite>semi-minor</cite> axis length.
     * Since the {@code EllipsoidToCentricTransform} class works on an ellipsoid where a = 1
     * (because of the work performed by the normalization matrices), we just drop <var>a</var>
     * in the formulas - so this field could be written as just <var>b</var>.
     *
     * <p>This value is related to {@link #eccentricitySquared} and to the ε value used in EPSG guide
     * by (assuming a=1):</p>
     * <ul>
     *   <li>ℯ² = 1 - b²</li>
     *   <li>ε = ℯ²/b²</li>
     * </ul>
     *
     * <p><strong>Consider this field as final!</strong>
     * It is not final only for the purpose of {@link #readObject(ObjectInputStream)}.
     * This field is recomputed from {@link #eccentricitySquared} on deserialization.</p>
     */
    private transient double axisRatio;

    /**
     * Whether calculation of φ should use an iterative method after the first φ approximation.
     * The current implementation sets this field to {@code true} at construction time when the eccentricity value
     * is greater than or equals to {@link #ECCENTRICITY_THRESHOLD}, but this policy may change in any future SIS
     * version (for example we do not take the <var>h</var> values in account yet).
     *
     * <p><strong>Consider this field as final!</strong>
     * It is not final only for the purpose of {@link #readObject(ObjectInputStream)}.
     * This field is not serialized because its value may depend on the version of this
     * {@code EllipsoidToCentricTransform} class.</p>
     */
    private transient boolean useIterations;

    /**
     * {@code true} if ellipsoidal coordinates include an ellipsoidal height (i.e. are 3-D).
     * If {@code false}, then the input coordinates are expected to be two-dimensional and
     * the ellipsoidal height is assumed to be 0.
     */
    final boolean withHeight;

    /**
     * The parameters used for creating this conversion.
     * They are used for formatting <cite>Well Known Text</cite> (WKT) and error messages.
     *
     * @see #getContextualParameters()
     */
    final ContextualParameters context;

    /**
     * The inverse of this transform.
     *
     * <div class="note"><b>Note:</b>
     * creation of this object is not deferred to the first call to the {@link #inverse()} method because this
     * object is lightweight and typically needed soon anyway (may be as soon as {@code ConcatenatedTransform}
     * construction time). In addition this field is part of serialization form in order to preserve the
     * references graph.</div>
     */
    private final AbstractMathTransform inverse;

    /**
     * Creates a transform from angles in radians on ellipsoid having a semi-major axis length of 1.
     * More specifically {@code EllipsoidToCentricTransform} instances expect input coordinates
     * as below:
     *
     * <ol>
     *   <li>longitudes in <strong>radians</strong> relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in <strong>radians</strong>,</li>
     *   <li>optionally heights above the ellipsoid, in units of an ellipsoid having a semi-major axis length of 1.</li>
     * </ol>
     *
     * Output coordinates are as below, in units of an ellipsoid having a semi-major axis length of 1:
     * <ol>
     *   <li>distance from Earth center on the X axis (toward the intersection of prime meridian and equator),</li>
     *   <li>distance from Earth center on the Y axis (toward the intersection of 90°E meridian and equator),</li>
     *   <li>distance from Earth center on the Z axis (toward North pole).</li>
     * </ol>
     *
     * <div class="section">Geographic to geocentric conversions</div>
     * For converting geographic coordinates to geocentric coordinates, {@code EllipsoidToCentricTransform}
     * instances need to be concatenated with the following affine transforms:
     *
     * <ul>
     *   <li><cite>Normalization</cite> before {@code EllipsoidToCentricTransform}:<ul>
     *     <li>Conversion of (λ,φ) from degrees to radians</li>
     *     <li>Division of (h) by the semi-major axis length</li>
     *   </ul></li>
     *   <li><cite>Denormalization</cite> after {@code EllipsoidToCentricTransform}:<ul>
     *     <li>Multiplication of (X,Y,Z) by the semi-major axis length</li>
     *   </ul></li>
     * </ul>
     *
     * After {@code EllipsoidToCentricTransform} construction,
     * the full conversion chain including the above affine transforms can be created by
     * <code>{@linkplain #getContextualParameters()}.{@linkplain ContextualParameters#completeTransform
     * completeTransform}(factory, this)}</code>.
     *
     * @param semiMajor  The semi-major axis length.
     * @param semiMinor  The semi-minor axis length.
     * @param unit       The unit of measurement for the semi-axes and the ellipsoidal height.
     * @param withHeight {@code true} if source geographic coordinates include an ellipsoidal height
     *                   (i.e. are 3-D), or {@code false} if they are only 2-D.
     * @param target     Whether the target coordinate shall be Cartesian or Spherical.
     *
     * @see #createGeodeticConversion(MathTransformFactory, double, double, Unit, boolean, TargetType)
     */
    protected EllipsoidToCentricTransform(final double semiMajor, final double semiMinor,
            final Unit<Length> unit, final boolean withHeight, final TargetType target)
    {
        ArgumentChecks.ensureStrictlyPositive("semiMajor", semiMajor);
        ArgumentChecks.ensureStrictlyPositive("semiMinor", semiMinor);
        ArgumentChecks.ensureNonNull("target", target);
        axisRatio = semiMinor / semiMajor;
        eccentricitySquared = 1 - (axisRatio * axisRatio);
        useIterations = (eccentricitySquared >= ECCENTRICITY_THRESHOLD * ECCENTRICITY_THRESHOLD);
        this.withHeight = withHeight;
        /*
         * Copy parameters to the ContextualParameter. Those parameters are not used directly by
         * EllipsoidToCentricTransform, but we need to store them in case the user asks for them.
         */
        context = new ContextualParameters(GeographicToGeocentric.PARAMETERS, withHeight ? 4 : 3, 4);
        context.getOrCreate(SEMI_MAJOR).setValue(semiMajor, unit);
        context.getOrCreate(SEMI_MINOR).setValue(semiMinor, unit);
        /*
         * Prepare two affine transforms to be executed before and after this EllipsoidToCentricTransform:
         *
         *   - A "normalization" transform for converting degrees to radians and normalizing the height,
         *   - A "denormalization" transform for scaling (X,Y,Z) to the semi-major axis length.
         */
        context.normalizeGeographicInputs(0);
        final DoubleDouble a = new DoubleDouble(semiMajor);
        final MatrixSIS denormalize = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        for (int i=0; i<3; i++) {
            denormalize.convertAfter(i, a, null);
        }
        if (withHeight) {
            a.inverseDivide(1, 0);
            final MatrixSIS normalize = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
            normalize.convertBefore(2, a, null);    // Divide ellipsoidal height by a.
        }
        inverse = new Inverse();
    }

    /**
     * Restores transient fields after deserialization.
     *
     * @param  in The input stream from which to deserialize the transform.
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        useIterations = (eccentricitySquared >= ECCENTRICITY_THRESHOLD * ECCENTRICITY_THRESHOLD);
        axisRatio = sqrt(1 - eccentricitySquared);
    }

    /**
     * Creates a transform from geographic to geocentric coordinates. This factory method combines the
     * {@code EllipsoidToCentricTransform} instance with the steps needed for converting degrees to
     * radians and expressing the results in units of the given ellipsoid.
     *
     * <p>Input coordinates are expected to contain:</p>
     * <ol>
     *   <li>longitudes in <strong>degrees</strong> relative to the prime meridian (usually Greenwich),</li>
     *   <li>latitudes in <strong>degrees</strong>,</li>
     *   <li>optionally heights above the ellipsoid, in units of the ellipsoid axis (usually metres).</li>
     * </ol>
     *
     * Output coordinates are as below, in units of the ellipsoid axis (usually metres):
     * <ol>
     *   <li>distance from Earth center on the X axis (toward the intersection of prime meridian and equator),</li>
     *   <li>distance from Earth center on the Y axis (toward the intersection of 90°E meridian and equator),</li>
     *   <li>distance from Earth center on the Z axis (toward North pole).</li>
     * </ol>
     *
     * @param factory    The factory to use for creating and concatenating the affine transforms.
     * @param semiMajor  The semi-major axis length.
     * @param semiMinor  The semi-minor axis length.
     * @param unit       The unit of measurement for the semi-axes and the ellipsoidal height.
     * @param withHeight {@code true} if source geographic coordinates include an ellipsoidal height
     *                   (i.e. are 3-D), or {@code false} if they are only 2-D.
     * @param target     Whether the target coordinate shall be Cartesian or Spherical.
     * @return The conversion from geographic to geocentric coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static MathTransform createGeodeticConversion(final MathTransformFactory factory,
            final double semiMajor, final double semiMinor, final Unit<Length> unit,
            final boolean withHeight, final TargetType target) throws FactoryException
    {
        if (Math.abs(semiMajor - semiMinor) <= semiMajor * (Formulas.LINEAR_TOLERANCE / ReferencingServices.AUTHALIC_RADIUS)) {
            /*
             * If semi-major axis length is almost equal to semi-minor axis length, uses spherical equations instead.
             * We need to add the sphere radius to the elevation before to perform spherical to Cartesian conversion.
             */
            final MatrixSIS translate = Matrices.createDiagonal(4, withHeight ? 4 : 3);
            translate.setElement(2, withHeight ? 3 : 2, semiMajor);
            if (!withHeight) {
                translate.setElement(3, 2, 1);
            }
            final MathTransform tr = SphericalToCartesian.INSTANCE.completeTransform(factory);
            return factory.createConcatenatedTransform(factory.createAffineTransform(translate), tr);
        }
        EllipsoidToCentricTransform tr = new EllipsoidToCentricTransform(semiMajor, semiMinor, unit, withHeight, target);
        return tr.context.completeTransform(factory, tr);
    }

    /**
     * Creates a transform from geographic to Cartesian geocentric coordinates (convenience method).
     * Invoking this method is equivalent to the following:
     *
     * {@preformat java
     *     createGeodeticConversion(factory,
     *             ellipsoid.getSemiMajorAxis(),
     *             ellipsoid.getSemiMinorAxis(),
     *             ellipsoid.getAxisUnit(),
     *             withHeight, TargetType.CARTESIAN);
     * }
     *
     * The target type is assumed Cartesian because this is the most frequently used target.
     *
     * @param factory    The factory to use for creating and concatenating the affine transforms.
     * @param ellipsoid  The semi-major and semi-minor axis lengths with their unit of measurement.
     * @param withHeight {@code true} if source geographic coordinates include an ellipsoidal height
     *                   (i.e. are 3-D), or {@code false} if they are only 2-D.
     * @return The conversion from geographic to Cartesian geocentric coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static MathTransform createGeodeticConversion(final MathTransformFactory factory,
            final Ellipsoid ellipsoid, final boolean withHeight) throws FactoryException
    {
        return createGeodeticConversion(factory, ellipsoid.getSemiMajorAxis(), ellipsoid.getSemiMinorAxis(),
                ellipsoid.getAxisUnit(), withHeight, TargetType.CARTESIAN);
    }

    /**
     * Returns the parameters used for creating the complete conversion. Those parameters describe a sequence
     * of <cite>normalize</cite> → {@code this} → <cite>denormalize</cite> transforms, <strong>not</strong>
     * including {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes axis swapping}.
     * Those parameters are used for formatting <cite>Well Known Text</cite> (WKT) and error messages.
     *
     * @return The parameters values for the sequence of
     *         <cite>normalize</cite> → {@code this} → <cite>denormalize</cite> transforms.
     */
    @Override
    protected ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Returns a copy of internal parameter values of this {@code EllipsoidToCentricTransform} transform.
     * The returned group contains parameter values for the number of dimensions and the eccentricity.
     *
     * <div class="note"><b>Note:</b>
     * this method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters}.</div>
     *
     * @return A copy of the internal parameter values for this transform.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        final Parameters pg = Parameters.castOrWrap(getParameterDescriptors().createValue());
        pg.getOrCreate(ECCENTRICITY).setValue(sqrt(eccentricitySquared));
        pg.parameter("target").setValue(getTargetType());
        pg.getOrCreate(DIMENSION).setValue(getSourceDimensions());
        return pg;
    }

    /**
     * Returns a description of the internal parameters of this {@code EllipsoidToCentricTransform} transform.
     * The returned group contains parameter descriptors for the number of dimensions and the eccentricity.
     *
     * @return A description of the internal parameters.
     */
    @Debug
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        synchronized (EllipsoidToCentricTransform.class) {
            if (DESCRIPTOR == null) {
                final ParameterBuilder builder = new ParameterBuilder().setCodeSpace(Citations.SIS, Constants.SIS);
                final ParameterDescriptor<TargetType> target = builder.setRequired(true)
                        .addName("target").create(TargetType.class, TargetType.CARTESIAN);
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
     * @return Always 3.
     */
    @Override
    public final int getTargetDimensions() {
        return 3;
    }

    /**
     * Returns whether the target coordinate system is Cartesian or Spherical.
     *
     * @return Whether the target coordinate system is Cartesian or Spherical.
     */
    public final TargetType getTargetType() {
        return TargetType.CARTESIAN;
    }

    /**
     * Computes the derivative at the given location.
     * This method relaxes a little bit the {@code MathTransform} contract by accepting two- or three-dimensional
     * points even if the number of dimensions does not match the {@link #getSourceDimensions()} value.
     *
     * <div class="note"><b>Rational:</b>
     * that flexibility on the number of dimensions is required for calculation of {@linkplain #inverse() inverse}
     * transform derivative, because that calculation needs to inverse a square matrix with all terms in it before
     * to drop the last row in the two-dimensional case.</div>
     *
     * @param  point The coordinate point where to evaluate the derivative.
     * @return The derivative at the specified point (never {@code null}).
     * @throws TransformException if the derivative can not be evaluated at the specified point.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        final int dim = point.getDimension();
        final boolean wh;
        final double h;
        switch (dim) {
            default: throw mismatchedDimension("point", getSourceDimensions(), dim);
            case 3:  wh = true;  h = point.getOrdinate(2); break;
            case 2:  wh = false; h = 0; break;
        }
        return transform(point.getOrdinate(0), point.getOrdinate(1), h, null, 0, true, wh);
    }

    /**
     * Converts the (λ,φ) or (λ,φ,<var>h</var>) geodetic coordinates to
     * to (<var>X</var>,<var>Y</var>,<var>Z</var>) geocentric coordinates,
     * and optionally returns the derivative at that location.
     *
     * @return {@inheritDoc}
     * @throws TransformException if the point can not be transformed or
     *         if a problem occurred while calculating the derivative.
     */
    @Override
    public Matrix transform(final double[] srcPts, final int srcOff,
                            final double[] dstPts, final int dstOff,
                            final boolean derivate) throws TransformException
    {
        return transform(srcPts[srcOff], srcPts[srcOff+1], withHeight ? srcPts[srcOff+2] : 0,
                dstPts, dstOff, derivate, withHeight);
    }

    /**
     * Implementation of {@link #transform(double[], int, double[], int, boolean)}
     * with possibility to override the {@link #withHeight} value.
     *
     * @param λ        Longitude (radians).
     * @param φ        Latitude (radians).
     * @param h        Height above the ellipsoid divided by the length of semi-major axis.
     * @param dstPts   The array into which the transformed coordinate is returned.
     *                 May be {@code null} if only the derivative matrix is desired.
     * @param dstOff   The offset to the location of the transformed point that is stored in the destination array.
     * @param derivate {@code true} for computing the derivative, or {@code false} if not needed.
     * @param wh       {@code true} for a 3×3 matrix, or {@code false} for a 3×2 matrix excluding the height elements.
     */
    private Matrix transform(final double λ, final double φ, final double h,
                             final double[] dstPts, final int dstOff,
                             final boolean derivate, final boolean wh)
    {
        final double cosλ = cos(λ);
        final double sinλ = sin(λ);
        final double cosφ = cos(φ);
        final double sinφ = sin(φ);
        final double ν2   = 1 / (1 - eccentricitySquared*(sinφ*sinφ));   // Square of ν (see below)
        final double ν    = sqrt(ν2);                                    // Prime vertical radius of curvature at latitude φ
        final double r    = ν + h;
        final double νℯ   = ν * (1 - eccentricitySquared);
        if (dstPts != null) {
            final double rcosφ = r * cosφ;
            dstPts[dstOff  ] = rcosφ  * cosλ;                            // X: Toward prime meridian
            dstPts[dstOff+1] = rcosφ  * sinλ;                            // Y: Toward 90° east
            dstPts[dstOff+2] = (νℯ+h) * sinφ;                            // Z: Toward north pole
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
            return Matrices.create(3, 2, new double[] {
                    dX_dλ, dX_dφ,
                    dY_dλ, dY_dφ,
                        0, dZ_dφ});
        }
    }

    /**
     * Converts the (λ,φ) or (λ,φ,<var>h</var>) geodetic coordinates to
     * to (<var>X</var>,<var>Y</var>,<var>Z</var>) geocentric coordinates.
     * This method performs the same conversion than {@link #transform(double[], int, double[], int, boolean)},
     * but the formulas are repeated here for performance reasons.
     *
     * @throws TransformException if a point can not be transformed.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, final double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        int srcInc = 0;
        int dstInc = 0;
        if (srcPts == dstPts) {
            final int dimSource = getSourceDimensions();
            switch (IterationStrategy.suggest(srcOff, dimSource, dstOff, 3, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * dimSource;
                    dstOff += (numPts - 1) * 3;         // Target dimension is fixed to 3.
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
            dstPts[dstOff++]   = rcosφ * cos(λ);                                   // X: Toward prime meridian
            dstPts[dstOff++]   = rcosφ * sin(λ);                                   // Y: Toward 90° east
            dstPts[dstOff++]   = (ν * (1 - eccentricitySquared) + h) * sinφ;       // Z: Toward north pole
            srcOff += srcInc;
            dstOff += dstInc;
        }
    }

    /*
     * NOTE: we do not bother to override the methods expecting a 'float' array because those methods should
     *       be rarely invoked. Since there is usually LinearTransforms before and after this transform, the
     *       conversion between float and double will be handled by those LinearTransforms.  If nevertheless
     *       this EllipsoidToCentricTransform is at the beginning or the end of a transformation chain,
     *       the methods inherited from the subclass will work (but may be slightly slower).
     */

    /**
     * Converts Cartesian coordinates (<var>X</var>,<var>Y</var>,<var>Z</var>) to ellipsoidal coordinates
     * (λ,φ) or (λ,φ,<var>h</var>). This method is invoked by the transform returned by {@link #inverse()}.
     *
     * @param  srcPts The array containing the source point coordinates.
     * @param  srcOff The offset to the first point to be transformed in the source array.
     * @param  dstPts The array into which the transformed point coordinates are returned.
     *                May be the same than {@code srcPts}.
     * @param  dstOff The offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts The number of point objects to be transformed.
     * @throws TransformException if the calculation does not converge.
     */
    protected void inverseTransform(final double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts)
            throws TransformException
    {
        int srcInc = 0;
        int dstInc = 0;
        if (srcPts == dstPts) {
            final int dimTarget = getSourceDimensions();
            switch (IterationStrategy.suggest(srcOff, 3, dstOff, dimTarget, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * 3;             // Source dimension is fixed to 3.
                    dstOff += (numPts - 1) * dimTarget;
                    srcInc = -6;
                    dstInc = -2 * dimTarget;
                    break;
                }
                default: {
                    dstPts = Arrays.copyOfRange(dstPts, dstOff, dstOff + numPts*dimTarget);
                    dstOff = 0;
                    break;
                }
            }
        }
next:   while (--numPts >= 0) {
            final double X = srcPts[srcOff++];
            final double Y = srcPts[srcOff++];
            final double Z = srcPts[srcOff++];
            final double p = hypot(X, Y);
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
            double φ = atan((Z + copySign(eccentricitySquared * sin2q*sqrt(sin2q), tanq) / axisRatio) /
                            (p -          eccentricitySquared * cos2q*sqrt(cos2q)));
            /*
             * The above is an approximation of φ. Usually we are done with a good approximation for
             * a planet of the eccentricity of Earth. Code below is the one that will be executed in
             * the vast majority of cases.
             */
            if (!useIterations) {
                dstPts[dstOff++] = atan2(Y, X);
                dstPts[dstOff++] = φ;
                if (withHeight) {
                    final double sinφ = sin(φ);
                    final double ν = 1/sqrt(1 - eccentricitySquared * (sinφ*sinφ));
                    dstPts[dstOff++] = p/cos(φ) - ν;
                }
                srcOff += srcInc;
                dstOff += dstInc;
            } else {
                /*
                 * If this code is used on a planet with high eccentricity,
                 * the φ value may need to be improved by an iterative method.
                 */
                for (int it=0; it<Formulas.MAXIMUM_ITERATIONS; it++) {
                    final double sinφ = sin(φ);
                    final double ν = 1/sqrt(1 - eccentricitySquared * (sinφ*sinφ));
                    final double Δφ = φ - (φ = atan((Z + eccentricitySquared * ν * sinφ) / p));
                    if (!(abs(Δφ) >= Formulas.ANGULAR_TOLERANCE * (PI/180) * 0.25)) {   // Use ! for accepting NaN.
                        dstPts[dstOff++] = atan2(Y, X);
                        dstPts[dstOff++] = φ;
                        if (withHeight) {
                            dstPts[dstOff++] = p/cos(φ) - ν;
                        }
                        srcOff += srcInc;
                        dstOff += dstInc;
                        continue next;
                    }
                }
                throw new TransformException(Errors.format(Errors.Keys.NoConvergence));
            }
        }
    }

    /**
     * Returns the inverse of this transform. The default implementation returns a transform
     * that will delegate its work to {@link #inverseTransform(double[], int, double[], int, int)}.
     *
     * @return The conversion from (geo)centric to ellipsoidal coordinates.
     */
    @Override
    public MathTransform inverse() {
        return inverse;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected int computeHashCode() {
        int code = super.computeHashCode() + Numerics.hashCode(Double.doubleToLongBits(axisRatio));
        if (withHeight) code += 37;
        return code;
    }

    /**
     * Compares the specified object with this math transform for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            // Slight optimization
            return true;
        }
        if (super.equals(object, mode)) {
            final EllipsoidToCentricTransform that = (EllipsoidToCentricTransform) object;
            return (withHeight == that.withHeight) && Numerics.equals(axisRatio, that.axisRatio);
            // No need to compare the contextual parameters since this is done by super-class.
        }
        return false;
    }




    /**
     * Converts Cartesian coordinates (<var>X</var>,<var>Y</var>,<var>Z</var>)
     * to ellipsoidal coordinates (λ,φ) or (λ,φ,<var>h</var>).
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    private final class Inverse extends AbstractMathTransform.Inverse implements Serializable {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = 6942084702259211803L;

        /**
         * Creates the inverse of the enclosing transform.
         */
        Inverse() {
            EllipsoidToCentricTransform.this.super();
        }

        /**
         * Returns the same contextual parameters than in the enclosing class,
         * but with a different method name and the (de)normalization matrices inverted.
         */
        @Override
        protected ContextualParameters getContextualParameters() {
            return context.inverse(GeocentricToGeographic.PARAMETERS);
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
         * Returns a description of the internal parameters of this inverse transform.
         * We do not cache this instance for two reasons:
         *
         * <ul>
         *   <li>it is only for debugging purposes, and</li>
         *   <li>the user may override {@link EllipsoidToCentricTransform#getParameterDescriptors()}.</li>
         * </ul>
         */
        @Debug
        @Override
        public ParameterDescriptorGroup getParameterDescriptors() {
            return new DefaultParameterDescriptorGroup(Collections.singletonMap(ParameterDescriptorGroup.NAME_KEY,
                            new ImmutableIdentifier(Citations.SIS, Constants.SIS, "Centric to ellipsoid (radians domain)")),
                    EllipsoidToCentricTransform.this.getParameterDescriptors());
        }

        /**
         * Computes the derivative at the given location. We need to override this method because
         * we will inverse 3×2 matrices in a special way, with the knowledge that <var>h</var>
         * can be set to 0.
         */
        @Override
        public Matrix derivative(final DirectPosition point) throws TransformException {
            ArgumentChecks.ensureNonNull("point", point);
            final double[] coordinate = point.getCoordinate();
            ArgumentChecks.ensureDimensionMatches("point", 3, coordinate);
            return this.transform(coordinate, 0, coordinate, 0, true);
        }

        /**
         * Inverse transforms a single coordinate in a list of ordinal values,
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
                point  = new double[3];
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
            // We need to keep h during matrix inversion because (λ,φ,h) values are not independent.
            Matrix matrix = EllipsoidToCentricTransform.this.derivative(new DirectPositionView(point, offset, 3));
            matrix = Matrices.inverse(matrix);
            if (!withHeight) {
                matrix = MatrixSIS.castOrCopy(matrix).removeRows(2, 3);     // Drop height only after matrix inversion is done.
            }
            return matrix;
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
         * The intend is to handle the following sequence of operations defined in the EPSG database:
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
         *       instead than a transform based on a matrix of size 3×4.</li>
         * </ul>
         */
        @Override
        final MathTransform concatenate(final MathTransform other, final boolean applyOtherFirst,
                final MathTransformFactory factory) throws FactoryException
        {
            if (!applyOtherFirst && withHeight && other instanceof LinearTransform && other.getTargetDimensions() == 2) {
                /*
                 * Found a 3×4 matrix after this transform. We can reduce to a 3×3 matrix only if no dimension
                 * use the column that we are about to drop (i.e. all coefficients in that column are zero).
                 */
                Matrix matrix = ((LinearTransform) other).getMatrix();
                if (matrix.getElement(0,2) == 0 &&
                    matrix.getElement(1,2) == 0 &&
                    matrix.getElement(2,2) == 0)
                {
                    matrix = MatrixSIS.castOrCopy(matrix).removeColumns(2, 3);
                    final MathTransform tr2D = create2D().inverse();
                    if (factory != null) {
                        return factory.createConcatenatedTransform(tr2D, factory.createAffineTransform(matrix));
                    } else {
                        return ConcatenatedTransform.create(tr2D, MathTransforms.linear(matrix), factory);
                    }
                }
            }
            return super.concatenate(other, applyOtherFirst, factory);
        }

        /**
         * Given a transformation chain to format in WKT, inserts a "Geographic 3D to 2D" pseudo-conversion
         * after this transform (normally {@code transforms.get(index)}) if this conversion computes no height.
         *
         * @param  transforms The full chain of concatenated transforms.
         * @param  index      The index of this transform in the {@code transforms} chain.
         * @return Index of this transform in the {@code transforms} chain after processing.
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
     * @param  transforms The full chain of concatenated transforms.
     * @param  index      The index of this transform in the {@code transforms} chain.
     * @return Index of this transform in the {@code transforms} chain after processing.
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
     * The intend is to handle the following sequence of operations defined in the EPSG database:
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
     *       instead than a transform based on a matrix of size 4×3.</li>
     * </ul>
     */
    @Override
    final MathTransform concatenate(final MathTransform other, final boolean applyOtherFirst,
            final MathTransformFactory factory) throws FactoryException
    {
        if (applyOtherFirst && withHeight && other instanceof LinearTransform && other.getSourceDimensions() == 2) {
            /*
             * Found a 4×3 matrix before this transform. We can reduce to a 3×3 matrix only if the row that we are
             * about to drop unconditionnaly set the height to zero (i.e. all coefficients in that row are zero).
             */
            Matrix matrix = ((LinearTransform) other).getMatrix();
            if (matrix.getElement(2,0) == 0 &&
                matrix.getElement(2,1) == 0 &&
                matrix.getElement(2,2) == 0)
            {
                matrix = MatrixSIS.castOrCopy(matrix).removeRows(2, 3);
                final MathTransform tr2D = create2D();
                if (factory != null) {
                    return factory.createConcatenatedTransform(factory.createAffineTransform(matrix), tr2D);
                } else {
                    return ConcatenatedTransform.create(MathTransforms.linear(matrix), tr2D, factory);
                }
            }
        }
        return super.concatenate(other, applyOtherFirst, factory);
    }

    /**
     * Creates a transform with the same parameters than this transform,
     * but expecting two-dimensional inputs instead than three-dimensional.
     */
    final EllipsoidToCentricTransform create2D() {
        final ParameterValue<Double> p = context.getOrCreate(SEMI_MAJOR);
        final Unit<Length> unit = p.getUnit().asType(Length.class);
        return new EllipsoidToCentricTransform(p.doubleValue(),
                context.getOrCreate(SEMI_MINOR).doubleValue(unit), unit, false, getTargetType());
    }
}
