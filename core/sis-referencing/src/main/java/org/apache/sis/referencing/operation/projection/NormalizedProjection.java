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

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.util.FactoryException;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform2D;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Numerics;

import static java.lang.Math.*;
import static java.lang.Double.*;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import java.util.Objects;


/**
 * Base class for conversion services between ellipsoidal and cartographic projections.
 * This conversion works on a normalized spaces, where angles are expressed in radians and
 * computations are performed for a sphere having a semi-major axis of 1. More specifically:
 *
 * <ul class="verbose">
 *   <li>On input, the {@link #transform(double[], int, double[], int, boolean) transform(…)} method
 *   expects (<var>longitude</var>, <var>latitude</var>) angles in <strong>radians</strong>.
 *   Longitudes have the <cite>central meridian</cite> (λ₀) removed before the transform method is invoked.
 *   The conversion from degrees to radians and the longitude rotation are applied by the
 *   {@linkplain ContextualParameters#normalizeGeographicInputs normalization} affine transform.</li>
 *
 *   <li>On output, the {@link #transform(double[],int,double[],int,boolean) transform(…)} method returns
 *   (<var>x</var>, <var>y</var>) values on a sphere or ellipse having a semi-major axis length (<var>a</var>) of 1.
 *   The multiplication by the scale factor (<var>k</var>₀) and the translation by false easting (FE) and false
 *   northing (FN) are applied by the {@linkplain ContextualParameters#scaleAndTranslate2D denormalization}
 *   affine transform.</li>
 * </ul>
 *
 * The normalization and denormalization steps are represented below by the matrices immediately on the left and right
 * sides of {@code NormalizedProjection} respectively. Those matrices show only the basic parameters common to most projections.
 * Some projections will put more elements in those matrices.
 *
 * <center>
 *   <table class="compact" style="td {vertical-align: middle}" summary="Decomposition of a map projection">
 *     <tr>
 *       <td>{@include ../transform/formulas.html#SwapAxes}</td>
 *       <td>→</td>
 *       <td>{@include ../transform/formulas.html#NormalizeGeographic}</td>
 *       <td>→</td>
 *       <td>{@code NormalizedProjection}</td>
 *       <td>→</td>
 *       <td>{@include ../transform/formulas.html#DenormalizeCartesian}</td>
 *     </tr>
 *   </table>
 * </center>
 *
 * <div class="note"><b>Note:</b>
 * The first matrix on the left side is for {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes
 * swapping axes} from (<var>latitude</var>, <var>longitude</var>) to (<var>longitude</var>, <var>latitude</var>) order.
 * This matrix is shown here for completeness, but is not managed by this projection package. Axes swapping is managed
 * at a {@linkplain org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#createBaseToDerived(
 * org.opengis.referencing.cs.CoordinateSystem, org.opengis.referencing.operation.MathTransform,
 * org.opengis.referencing.cs.CoordinateSystem) higher level}.</div>
 *
 * {@code NormalizedProjection} does not store the above cited parameters (central meridian, scale factor, <i>etc.</i>)
 * on intend, in order to make clear that those parameters are not used by subclasses.
 * The ability to recognize two {@code NormalizedProjection}s as {@linkplain #equals(Object, ComparisonMode) equivalent}
 * without consideration for the scale factor (among other) allow more efficient concatenation in some cases
 * (typically some combinations of inverse projection followed by a direct projection).
 *
 * <p>All angles (either fields, method parameters or return values) in this class and subclasses are
 * in radians. This is the opposite of {@link Parameters} where all angles are in CRS-dependent units,
 * typically decimal degrees.</p>
 *
 * <div class="section">Serialization</div>
 * Serialization of this class is appropriate for short-term storage or RMI use, but may not be compatible
 * with future versions. For long term storage, WKT (Well Know Text) or XML are more appropriate.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  André Gosselin (MPO)
 * @author  Rueben Schulz (UBC)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://mathworld.wolfram.com/MapProjection.html">Map projections on MathWorld</a>
 */
public abstract class NormalizedProjection extends AbstractMathTransform2D implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1969740225939106310L;

    /**
     * Maximum difference allowed when comparing longitudes or latitudes in radians.
     * The current value take the system-wide angular tolerance value (equivalent to
     * about 1 cm on Earth) converted to radians.
     *
     * <p>Some formulas use this tolerance value for testing sines or cosines of an angle.
     * In the sine case, this is justified because sin(θ) ≅ θ when θ is small.
     * Similar reasoning applies to cosine with cos(θ) ≅ θ + π/2 when θ is small.</p>
     */
    static final double ANGULAR_TOLERANCE = Formulas.ANGULAR_TOLERANCE * (PI/180);

    /**
     * Desired accuracy for the result of iterative computations, in radians.
     * This constant defines the desired accuracy of methods like {@link #φ(double)}.
     *
     * <p>The current value is 0.25 time the accuracy derived from {@link Formulas#LINEAR_TOLERANCE}.
     * So if the linear tolerance is 1 cm, then the accuracy that we will seek for is 0.25 cm (about
     * 4E-10 radians). The 0.25 factor is a safety margin for meeting the 1 cm accuracy.</p>
     */
    static final double ITERATION_TOLERANCE = Formulas.ANGULAR_TOLERANCE * (PI/180) * 0.25;

    /**
     * Maximum number of iterations for iterative computations.
     */
    static final int MAXIMUM_ITERATIONS = 15;

    /**
     * The parameters used for creating this projection. They are used for formatting <cite>Well Known Text</cite> (WKT)
     * and error messages. Subclasses shall not use the values defined in this object for computation purpose, except at
     * construction time.
     *
     * @see #getContextualParameters()
     */
    final ContextualParameters context;

    /**
     * Ellipsoid excentricity, equal to <code>sqrt({@linkplain #excentricitySquared})</code>.
     * Value 0 means that the ellipsoid is spherical.
     */
    protected final double excentricity;

    /**
     * The square of excentricity: ℯ² = (a²-b²)/a² where
     * <var>ℯ</var> is the {@linkplain #excentricity excentricity},
     * <var>a</var> is the <cite>semi-major</cite> axis length and
     * <var>b</var> is the <cite>semi-minor</cite> axis length.
     */
    protected final double excentricitySquared;

    /**
     * The inverse of this map projection.
     */
    private final MathTransform2D inverse;

    /**
     * Constructs a new map projection from the supplied parameters.
     * This constructor applies the following operations on the {@link ContextualParameter}:
     *
     * <ul>
     *   <li>On the <b>normalization</b> matrix (to be applied before {@code this} transform):
     *     <ul>
     *       <li>Nothing. Callers shall invoke {@link ContextualParameters#normalizeGeographicInputs(double)} themselves.</li>
     *     </ul>
     *   </li>
     *   <li>On the <b>denormalization</b> matrix (to be applied after {@code this} transform):
     *     <ul>
     *       <li>{@linkplain ContextualParameters#scaleAndTranslate2D(boolean, double, double, double) Scale}
     *           by the <cite>semi-major</cite> axis length.</li>
     *       <li>Translate by the false easting and false northing (after the scale).</li>
     *     </ul>
     *   </li>
     *   <li>On the <b>contextual parameters</b> (not the parameters of {@code this} transform):
     *     <ul>
     *       <li>Store the values for <cite>semi-major</cite> axis length, <cite>semi-minor</cite> axis length,
     *           <cite>false easting</cite> and <cite>false northing</cite>.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param method        Description of the map projection parameters.
     * @param parameters    The parameters of the projection to be created.
     * @param falseEasting  The descriptor for fetching the <cite>false easting</cite> parameter.
     * @param falseNorthing The descriptor for fetching the <cite>false northing</cite> parameter.
     */
    protected NormalizedProjection(final OperationMethod method, final Parameters parameters,
            final ParameterDescriptor<Double> falseEasting,
            final ParameterDescriptor<Double> falseNorthing)
    {
        ensureNonNull("parameters", parameters);
        context = new ContextualParameters(method);
        final double a  = getAndStore(parameters, MapProjection.SEMI_MAJOR);
        final double b  = getAndStore(parameters, MapProjection.SEMI_MINOR);
        final double fe = getAndStore(parameters, falseEasting);
        final double fn = getAndStore(parameters, falseNorthing);
        context.scaleAndTranslate2D(false, a, fe, fn);
        excentricitySquared = 1.0 - (b*b) / (a*a);
        excentricity = sqrt(excentricitySquared);
        inverse = new Inverse();
    }

    /**
     * Creates a new projection initialized to the values of the given one. This constructor may be invoked after
     * we determined that the default implementation can be replaced by an other one, for example using spherical
     * formulas instead than the ellipsoidal ones. This constructor allows to transfer all parameters to the new
     * instance without recomputing them.
     */
    NormalizedProjection(final NormalizedProjection other) {
        context             = other.context;
        excentricity        = other.excentricity;
        excentricitySquared = other.excentricitySquared;
        inverse             = new Inverse();
    }

    /**
     * Returns {@code true} if the projection specified by the given parameters has the given name or identifier.
     * If non-null, the given identifier is presumed in the EPSG namespace and has precedence over the name.
     *
     * @param  parameters The user-specified parameters.
     * @param  name       The name to compare against the parameter group name.
     * @param  identifier The identifier to compare against the parameter group name.
     * @return {@code true} if the given parameter group has a name or EPSG identifier matching the given ones.
     */
    static boolean identMatch(final ParameterDescriptorGroup parameters, final String name, final String identifier) {
        if (identifier != null) {
            for (final Identifier id : parameters.getIdentifiers()) {
                if (identifier.equals(id.getCode()) && Constants.EPSG.equals(id.getCodeSpace())) {
                    return true;
                }
            }
        }
        return IdentifiedObjects.isHeuristicMatchForName(parameters, name);
    }

    /**
     * Returns a parameter descriptor with the same properties than the given one, except that the parameter is
     * optional. This is sometime needed when invoking {@link #getAndStore(Parameters, ParameterDescriptor)} for
     * a parameter value which is mandatory in principle, but may be absent because the parameters for the same
     * projection may be specified in different ways (for example <cite>"Mercator (variant A)"</cite> versus
     * <cite>"Mercator (variant B)"</cite>).
     *
     * @param  <T> The type of parameter value.
     * @param  descriptor The mandatory descriptor to make optional.
     * @return A parameter descriptor with the same properties than the given one, but optional.
     */
    static <T> ParameterDescriptor<T> makeOptional(final ParameterDescriptor<T> descriptor) {
        assert descriptor.getMinimumOccurs() == 1; // This method is useless if minOccurs == 0.
        return new DefaultParameterDescriptor<>(IdentifiedObjects.getProperties(descriptor), 0, 1,
                descriptor.getValueClass(), Parameters.getValueDomain(descriptor), null, descriptor.getDefaultValue());
    }

    /**
     * Gets a parameter value identified by the given descriptor and stores it in the {@link #context}.
     * A "contextual parameter" is a parameter that apply to the normalize → {@code this} → denormalize
     * chain as a whole. It does not really apply to this {@code NormalizedProjection} instance when taken alone.
     *
     * <p>This method shall be invoked at construction time only.</p>
     */
    final double getAndStore(final Parameters parameters, final ParameterDescriptor<Double> descriptor) {
        final double value = parameters.doubleValue(descriptor);    // Apply a unit conversion if needed.
        Comparable<Double> check = descriptor.getMinimumValue();
        if (check instanceof Number && !(value >= ((Number) check).doubleValue())) {
            throw outOfBounds(descriptor, value);
        }
        check = descriptor.getMaximumValue();
        if (check instanceof Number && !(value <= ((Number) check).doubleValue())) {
            throw outOfBounds(descriptor, value);
        }
        check = descriptor.getDefaultValue();
        if (check == null || !check.equals(value)) {
            context.parameter(descriptor.getName().getCode()).setValue(value);
        }
        return value;
    }

    /**
     * Creates the exception for a parameter value out of bounds.
     */
    private static IllegalArgumentException outOfBounds(final ParameterDescriptor<Double> descriptor, double value) {
        return new IllegalArgumentException(Errors.format(Errors.Keys.IllegalParameterValue_2, descriptor.getName(), value));
    }

    /**
     * Returns the sequence of <cite>normalization</cite> → {@code this} → <cite>denormalization</cite> transforms
     * as a whole. The transform returned by this method except (<var>longitude</var>, <var>latitude</var>)
     * coordinates in <em>degrees</em> and returns (<var>x</var>,<var>y</var>) coordinates in <em>metres</em>.
     * Conversion to other units and {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes
     * changes in axis order} are <strong>not</strong> managed by the returned transform.
     *
     * <p>The default implementation is as below:</p>
     * {@preformat java
     *     return getContextualParameters().completeTransform(factory, this);
     * }
     *
     * Subclasses can override this method if they wish to use alternative implementations under some circumstances.
     * For example many subclasses will replace {@code this} by a specialized implementation if they detect that the
     * ellipsoid is actually {@linkplain #isSpherical() spherical}.
     *
     * @param  factory The factory to use for creating the transform.
     * @return The map projection from (λ,φ) to (<var>x</var>,<var>y</var>) coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     *
     * @see ContextualParameters#completeTransform(MathTransformFactory, MathTransform)
     */
    public MathTransform createMapProjection(final MathTransformFactory factory) throws FactoryException {
        return context.completeTransform(factory, this);
    }

    /**
     * Returns the parameters used for creating the complete map projection. Those parameters describe a sequence of
     * <cite>normalize</cite> → {@code this} → <cite>denormalize</cite> transforms, <strong>not</strong> including
     * {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes axis swapping}.
     * Those parameters are used for formatting <cite>Well Known Text</cite> (WKT) and error messages.
     * Subclasses shall not use the values defined in the returned object for computation purpose,
     * except at construction time.
     *
     * @return The parameters values for the sequence of <cite>normalize</cite> → {@code this} → <cite>denormalize</cite>
     *         transforms, or {@code null} if unspecified.
     */
    @Override
    protected final ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Returns a copy of the parameter values for this projection.
     * This base class supplies a value only for the following parameters:
     *
     * <ul>
     *   <li>Semi-major axis length, which is set to 1.</li>
     *   <li>Semi-minor axis length, which is set to
     *       <code>sqrt(1 - {@linkplain #excentricitySquared ℯ²})</code>.</li>
     * </ul>
     *
     * Subclasses must complete if needed. Many projections will not need to complete,
     * because most parameters like the scale factor or the false easting/northing can
     * be handled by the (de)normalization affine transforms.
     *
     * <div class="note"><b>Note:</b>
     * This method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters}.</div>
     *
     * @return A copy of the parameter values for this normalized projection.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        final ParameterDescriptorGroup descriptor = filter(getParameterDescriptors());
        final ParameterValueGroup values = descriptor.createValue();
        values.parameter(Constants.SEMI_MAJOR).setValue(1.0);
        values.parameter(Constants.SEMI_MINOR).setValue(sqrt(1 - excentricitySquared));
        return values;
    }

    /**
     * Filter the given parameter descriptor in order to retain only the semi-major and semi-minor axis lengths.
     * This filtered version is used for displaying the parameter values of this non-linear kernel only, not for
     * displaying the {@linkplain #getContextualParameters() contextual parameters}. Since displaying the kernel
     * parameter values is for debugging purpose only, it is not worth to cache this descriptor.
     */
    private static ParameterDescriptorGroup filter(final ParameterDescriptorGroup descriptor) {
        final List<GeneralParameterDescriptor> filtered = new ArrayList<>(5);
        for (final GeneralParameterDescriptor p : descriptor.descriptors()) {
            if (IdentifiedObjects.isHeuristicMatchForName(p, Constants.SEMI_MAJOR) ||
                IdentifiedObjects.isHeuristicMatchForName(p, Constants.SEMI_MINOR))
            {
                filtered.add(p);
            }
        }
        return new DefaultParameterDescriptorGroup(IdentifiedObjects.getProperties(descriptor),
                1, 1, filtered.toArray(new GeneralParameterDescriptor[filtered.size()]));
    }

    /**
     * Returns {@code true} if this projection is done on a sphere rather than an ellipsoid.
     * Projections on spheres have an {@linkplain #excentricity} equals to zero.
     *
     * @return {@code true} if this projection is on a sphere.
     */
    public final boolean isSpherical() {
        return excentricity == 0;
    }

    /**
     * Ensures that this projection is done on a sphere rather than an ellipsoid.
     * This method is invoked by constructors of classes implementing only spherical formulas.
     *
     * @throws IllegalArgumentException If the projection is not done on a sphere.
     */
    final void ensureSpherical() throws IllegalArgumentException {
        if (!isSpherical()) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EllipticalNotSupported));
        }
    }

    /**
     * Converts a single coordinate in {@code srcPts} at the given offset and stores the result
     * in {@code dstPts} at the given offset. In addition, opportunistically computes the
     * transform derivative if requested.
     *
     * <div class="section">Normalization</div>
     * The input ordinates are (<var>λ</var>,<var>φ</var>) (the variable names for <var>longitude</var> and
     * <var>latitude</var> respectively) angles in radians.
     * Input coordinate shall have the <cite>central meridian</cite> removed from the longitude by the caller
     * before this method is invoked. After this method is invoked, the caller will need to multiply the output
     * coordinate by the global <cite>scale factor</cite>
     * and apply the (<cite>false easting</cite>, <cite>false northing</cite>) offset.
     * This means that projections that implement this method are performed on a sphere or ellipse
     * having a semi-major axis length of 1.
     *
     * <div class="note"><b>Note:</b> in <a href="http://trac.osgeo.org/proj/">Proj.4</a>, the same standardization,
     * described above, is handled by {@code pj_fwd.c}.</div>
     *
     * <div class="section">Argument checks</div>
     * The input longitude and latitude are usually (but not always) in the range [-π … π] and [-π/2 … π/2] respectively.
     * However values outside those ranges are accepted on the assumption that most implementations use those values
     * only in trigonometric functions like {@linkplain Math#sin(double) sine} and {@linkplain Math#cos(double) cosine}.
     * If this assumption is not applicable to a particular subclass, then it is implementor's responsibility to check
     * the range.
     *
     * @param srcPts   The array containing the source point coordinate, as (<var>longitude</var>, <var>latitude</var>)
     *                 angles in <strong>radians</strong>.
     * @param srcOff   The offset of the single coordinate to be converted in the source array.
     * @param dstPts   The array into which the converted coordinate is returned (may be the same than {@code srcPts}).
     *                 Ordinates will be expressed in a dimensionless unit, as a linear distance on a unit sphere or ellipse.
     * @param dstOff   The offset of the location of the converted coordinate that is stored in the destination array.
     * @param derivate {@code true} for computing the derivative, or {@code false} if not needed.
     * @return The matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinate can not be converted.
     */
    @Override
    public abstract Matrix transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, boolean derivate)
            throws ProjectionException;

    /**
     * Inverse converts the single coordinate in {@code srcPts} at the given offset and stores the result in
     * {@code ptDst} at the given offset. The output ordinates are (<var>longitude</var>, <var>latitude</var>)
     * angles in radians, usually (but not necessarily) in the range [-π … π] and [-π/2 … π/2] respectively.
     *
     * <div class="section">Normalization</div>
     * Input coordinate shall have the (<cite>false easting</cite>, <cite>false northing</cite>) removed
     * by the caller and the result divided by the global <cite>scale factor</cite> before this method is invoked.
     * After this method is invoked, the caller will need to add the <cite>central meridian</cite> to the longitude
     * in the output coordinate. This means that projections that implement this method are performed on a sphere
     * or ellipse having a semi-major axis of 1.
     *
     * <div class="note"><b>Note:</b> in <a href="http://trac.osgeo.org/proj/">Proj.4</a>, the same standardization,
     * described above, is handled by {@code pj_inv.c}.</div>
     *
     * @param srcPts The array containing the source point coordinate, as linear distance on a unit sphere or ellipse.
     * @param srcOff The offset of the point to be converted in the source array.
     * @param dstPts The array into which the converted point coordinate is returned (may be the same than {@code srcPts}).
     *               Ordinates will be (<var>longitude</var>, <var>latitude</var>) angles in <strong>radians</strong>.
     * @param dstOff The offset of the location of the converted point that is stored in the destination array.
     * @throws ProjectionException if the point can not be converted.
     */
    protected abstract void inverseTransform(double[] srcPts, int srcOff, double[] dstPts, int dstOff)
            throws ProjectionException;

    /**
     * Returns the inverse of this map projection.
     * Subclasses do not need to override this method, as they should override
     * {@link #inverseTransform(double[], int, double[], int) inverseTransform(…)} instead.
     *
     * @return The inverse of this map projection.
     */
    @Override
    public MathTransform2D inverse() {
        return inverse;
    }

    /**
     * Inverse of a normalized map projection.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.6
     * @version 0.6
     * @module
     */
    private final class Inverse extends AbstractMathTransform2D.Inverse {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -9138242780765956870L;

        /**
         * Default constructor.
         */
        public Inverse() {
            NormalizedProjection.this.super();
        }

        /**
         * Inverse transforms the specified {@code srcPts} and stores the result in {@code dstPts}.
         * If the derivative has been requested, then this method will delegate the derivative
         * calculation to the enclosing class and inverts the resulting matrix.
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                      double[] dstPts,       int dstOff,
                                final boolean derivate) throws TransformException
        {
            if (!derivate) {
                inverseTransform(srcPts, srcOff, dstPts, dstOff);
                return null;
            } else {
                if (dstPts == null) {
                    dstPts = new double[2];
                    dstOff = 0;
                }
                inverseTransform(srcPts, srcOff, dstPts, dstOff);
                return Matrices.inverse(NormalizedProjection.this.transform(dstPts, dstOff, null, 0, true));
            }
        }
    }

    /**
     * Computes a hash code value for this map projection.
     * The default implementation computes a value from the parameters given at construction time.
     *
     * @return The hash code value.
     */
    @Override
    protected int computeHashCode() {
        return context.hashCode() + 31 * super.computeHashCode();
    }

    /**
     * Compares the given object with this transform for equivalence. The default implementation checks if
     * {@code object} is an instance of the same class than {@code this}, then compares the excentricity.
     *
     * <p>If this method returns {@code true}, then for any given identical source position, the two compared map
     * projections shall compute the same target position. Many of the {@linkplain #getContextualParameters()
     * contextual parameters} used for creating the map projections are irrelevant and do not need to be known.
     * Those projection parameters will be compared only if the comparison mode is {@link ComparisonMode#STRICT}
     * or {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}.</p>
     *
     * <div class="note"><b>Example:</b>
     * a {@linkplain Mercator Mercator} projection can be created in the 2SP case with a <cite>standard parallel</cite>
     * value of 60°. The same projection can also be created in the 1SP case with a <cite>scale factor</cite> of 0.5.
     * Nevertheless those two map projections applied on a sphere gives identical results. Considering them as
     * equivalent allows the referencing module to transform coordinates between those two projections more efficiently.
     * </div>
     *
     * @param object The object to compare with this map projection for equivalence.
     * @param mode The strictness level of the comparison. Default to {@link ComparisonMode#STRICT}.
     * @return {@code true} if the given object is equivalent to this map projection.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (super.equals(object, mode)) {
            final double e1, e2;
            final NormalizedProjection that = (NormalizedProjection) object;
            if (mode.ordinal() < ComparisonMode.IGNORE_METADATA.ordinal()) {
                if (!Objects.equals(context, that.context)) {
                    return false;
                }
                e1 = this.excentricitySquared;
                e2 = that.excentricitySquared;
            } else {
                e1 = this.excentricity;
                e2 = that.excentricity;
            }
            /*
             * There is no need to compare both 'excentricity' and 'excentricitySquared' since
             * the former is computed from the later. In strict comparison mode, we are better
             * to compare the 'excentricitySquared' since it is the original value from which
             * the other value is derived. However in approximative comparison mode, we need
             * to use the 'excentricity', otherwise we would need to take the square of the
             * tolerance factor before comparing 'excentricitySquared'.
             */
            return Numerics.epsilonEqual(e1, e2, mode);
        }
        return false;
    }




    //////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                          ////////
    ////////                       FORMULAS FROM EPSG or SNYDER                       ////////
    ////////                                                                          ////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Computes the reciprocal of the radius of curvature of the ellipsoid perpendicular to the meridian at latitude φ.
     * That radius of curvature is:
     *
     * <blockquote>ν = 1 / √(1 - ℯ²⋅sin²φ)</blockquote>
     *
     * This method returns 1/ν.
     *
     * <div class="section">Relationship with Snyder</div>
     * This is related to functions (14-15) from Snyder (used for computation of scale factors
     * at the true scale latitude) as below:
     *
     * <blockquote>m = cosφ / rν</blockquote>
     *
     * Special cases:
     * <ul>
     *   <li>If φ is 0°, then <var>m</var> is 1.</li>
     *   <li>If φ is ±90°, then <var>m</var> is 0 provided that we are not in the spherical case
     *       (otherwise we get {@link Double#NaN}).</li>
     * </ul>
     *
     * @param  sinφ The sine of the φ latitude in radians.
     * @return Reciprocal of the radius of curvature of the ellipsoid perpendicular to the meridian at latitude φ.
     */
    final double rν(final double sinφ) {
        return sqrt(1 - excentricitySquared * (sinφ*sinφ));
    }

    /**
     * Computes the radius of conformal sphere (Rc) at latitude φ. This is used when the user requested
     * explicitely spherical formulas for example with <cite>"Mercator (Spherical)"</cite> projection
     * (EPSG:1026), but the figure of the Earth used is an ellipsoid rather than a sphere.
     *
     * <p><b>Source:</b> <cite>Geomatics Guidance Note Number 7, part 2, version 49</cite> from EPSG:
     * table 3 in section 1.2 and explanation in section 1.3.3.1.</p>
     *
     * @param  sinφ The sine of the φ latitude in radians.
     * @return Radius of conformal sphere at latitude φ.
     */
    final double radiusOfConformalSphere(final double sinφ) {
        return sqrt(1 - excentricitySquared) / (1 - excentricitySquared * (sinφ*sinφ));
    }

    /**
     * Computes part of the Mercator projection for the given latitude. This formula is also part of
     * Lambert Conic Conformal projection, since Mercator can be considered as a special case of that
     * Lambert projection with the equator as the single standard parallel.
     *
     * <p>The Mercator projection is given by the {@linkplain Math#log(double) natural logarithm} of the
     * value returned by this method. This function is <em>almost</em> the converse of {@link #φ(double)}.
     *
     *
     * <div class="section">Properties</div>
     * This function is used with φ values in the [-π/2 … π/2] range and has a periodicity of 2π.
     * The result is always a positive number when the φ argument is inside the above-cited range.
     * If, after removal of any 2π periodicity, φ is still outside the [-π/2 … π/2] range, then the
     * result is a negative number. In a Mercator projection, such negative number will result in NaN.
     *
     * <p>Some values are:</p>
     * <ul>
     *   <li>expOfNorthing(NaN)    =  NaN</li>
     *   <li>expOfNorthing(±∞)     =  NaN</li>
     *   <li>expOfNorthing(-π/2)   =   0</li>
     *   <li>expOfNorthing( 0  )   =   1</li>
     *   <li>expOfNorthing(+π/2)   →   ∞  (actually some large value like 1.633E+16)</li>
     *   <li>expOfNorthing(-φ)     =  1 / expOfNorthing(φ)</li>
     * </ul>
     *
     *
     * <div class="section">The π/2 special case</div>
     * The value at {@code Math.PI/2} is not exactly infinity because there is no exact representation of π/2.
     * However since the conversion of 90° to radians gives {@code Math.PI/2}, we can presume that the user was
     * expecting infinity. The caller should check for the PI/2 special case himself if desired, as this method
     * does nothing special about it.
     *
     * <p>Note that the result for the φ value after {@code Math.PI/2} (as given by {@link Math#nextUp(double)})
     * is still positive, maybe because {@literal PI/2 < π/2 < nextUp(PI/2)}. Only the {@code nextUp(nextUp(PI/2))}
     * value become negative. Callers may need to take this behavior in account: special check for {@code Math.PI/2}
     * is not sufficient, the check needs to include at least the {@code nextUp(Math.PI/2)} case.</p>
     *
     *
     * <div class="section">Relationship with Snyder</div>
     * This function is related to the following functions from Snyder:
     *
     * <ul>
     *   <li>(7-7) in the <cite>Mercator projection</cite> chapter.</li>
     *   <li>Reciprocal of (9-13) in the <cite>Oblique Mercator projection</cite> chapter.</li>
     *   <li>Reciprocal of (15-9) in the <cite>Lambert Conformal Conic projection</cite> chapter.</li>
     * </ul>
     *
     * @param  φ     The latitude in radians.
     * @param  ℯsinφ The sine of the φ argument multiplied by {@link #excentricity}.
     * @return {@code Math.exp} of the Mercator projection of the given latitude.
     *
     * @see #φ(double)
     * @see #dy_dφ(double, double)
     */
    final double expOfNorthing(final double φ, final double ℯsinφ) {
        /*
         * Note:   tan(π/4 - φ/2)  =  1 / tan(π/4 + φ/2)
         */
        return tan(PI/4 + 0.5*φ) * pow((1 - ℯsinφ) / (1 + ℯsinφ), 0.5*excentricity);
    }

    /**
     * Computes the latitude for a value closely related to the <var>y</var> value of a Mercator projection.
     * This formula is also part of other projections, since Mercator can be considered as a special case of
     * Lambert Conic Conformal for instance.
     *
     * <p>This function is <em>almost</em> the converse of the above {@link #expOfNorthing(double, double)} function.
     * In a Mercator inverse projection, the value of the {@code expOfSouthing} argument is {@code exp(-y)}.</p>
     *
     * <p>The input should be a positive number, otherwise the result will be either outside
     * the [-π/2 … π/2] range, or will be NaN. Its behavior at some particular points is:</p>
     *
     * <ul>
     *   <li>φ(0)   =   π/2</li>
     *   <li>φ(1)   =   0</li>
     *   <li>φ(∞)   =  -π/2.</li>
     * </ul>
     *
     * @param  expOfSouthing The <em>reciprocal</em> of the value returned by {@link #expOfNorthing}.
     * @return The latitude in radians.
     * @throws ProjectionException if the iteration does not converge.
     *
     * @see #expOfNorthing(double, double)
     * @see #dy_dφ(double, double)
     */
    final double φ(final double expOfSouthing) throws ProjectionException {
        final double hℯ = 0.5 * excentricity;
        double φ = (PI/2) - 2*atan(expOfSouthing);          // Snyder (7-11)
        for (int i=0; i<MAXIMUM_ITERATIONS; i++) {          // Iteratively solve equation (7-9) from Snyder
            final double ℯsinφ = excentricity * sin(φ);
            final double Δφ = abs(φ - (φ = PI/2 - 2*atan(expOfSouthing * pow((1 - ℯsinφ)/(1 + ℯsinφ), hℯ))));
            if (Δφ <= ITERATION_TOLERANCE) {
                return φ;
            }
        }
        if (isNaN(expOfSouthing)) {
            return NaN;
        }
        throw new ProjectionException(Errors.Keys.NoConvergence);
    }

    /**
     * Computes the partial derivative of a Mercator projection at the given latitude. This formula is also part of
     * other projections, since Mercator can be considered as a special case of Lambert Conic Conformal for instance.
     *
     * <p>In order to get the derivative of the {@link #expOfNorthing(double, double)} function, call can multiply
     * the returned value by by {@code expOfNorthing}.</p>
     *
     * @param  sinφ the sine of latitude.
     * @param  cosφ The cosine of latitude.
     * @return The partial derivative of a Mercator projection at the given latitude.
     *
     * @see #expOfNorthing(double, double)
     * @see #φ(double)
     */
    final double dy_dφ(final double sinφ, final double cosφ) {
        return (1 / cosφ)  -  excentricitySquared * cosφ / (1 - excentricitySquared * (sinφ*sinφ));
    }
}
