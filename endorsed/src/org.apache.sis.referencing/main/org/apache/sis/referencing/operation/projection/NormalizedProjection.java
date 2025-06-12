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

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Pattern;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import static java.lang.Math.*;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.util.FactoryException;
import org.apache.sis.util.Debug;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform2D;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.transform.DomainDefinition;
import org.apache.sis.referencing.operation.transform.TransformJoiner;
import org.apache.sis.referencing.operation.provider.MapProjection;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.system.Modules;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.Numerics;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.Identifier;


/**
 * Base class for conversion services between ellipsoidal and cartographic projections.
 * This conversion works on a normalized spaces, where angles are expressed in radians and
 * computations are performed for a sphere having a semi-major axis of 1. More specifically:
 *
 * <ul class="verbose">
 *   <li>On input, the {@link #transform(double[], int, double[], int, boolean) transform(…)} method
 *   expects (<var>longitude</var>, <var>latitude</var>) angles in <strong>radians</strong>,
 *   sometimes pre-multiplied by other projection-specific factors (see point #3 below).
 *   Longitudes have the <i>central meridian</i> (λ₀) removed before the transform method is invoked.
 *   The conversion from degrees to radians and the longitude rotation are applied by the
 *   {@linkplain ContextualParameters#normalizeGeographicInputs normalization} affine transform.</li>
 *
 *   <li>On output, the {@link #transform(double[],int,double[],int,boolean) transform(…)} method returns
 *   (<var>x</var>, <var>y</var>) values on a sphere or ellipse having a semi-major axis length (<var>a</var>) of 1,
 *   sometimes divided by other projection-specific factors (see point #3 below).
 *   The multiplication by the scale factor (<var>k₀</var>) and the translation by false easting (FE) and false
 *   northing (FN) are applied by the {@linkplain ContextualParameters#getMatrix denormalization} affine transform.</li>
 *
 *   <li>In addition to above-cited conversions, subclasses may opportunistically concatenate other linear operations
 *   (scales and translations). They do that by changing the normalization and denormalization matrices shown below.
 *   When such changes are applied, the {@code transform(…)} inputs are no longer angles in radians but some other
 *   derived values.</li>
 * </ul>
 *
 * The normalization and denormalization steps are represented below by the matrices immediately on the left and right
 * sides of {@code NormalizedProjection} respectively. Those matrices show only the basic parameters common to most projections.
 * Some projections will put more elements in those matrices.
 *
 * <div class="horizontal-flow" style="align-items:center">
 *   <div>{@include ../transform/formulas.html#SwapAxes}</div>
 *   <div>→</div>
 *   <div>{@include ../transform/formulas.html#NormalizeGeographic}</div>
 *   <div>→</div>
 *   <div>{@code NormalizedProjection}</div>
 *   <div>→</div>
 *   <div>{@include ../transform/formulas.html#DenormalizeCartesian}</div>
 * </div>
 *
 * <div class="note"><b>Note:</b>
 * The first matrix on the left side is for {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes
 * swapping axes} from (<var>latitude</var>, <var>longitude</var>) to (<var>longitude</var>, <var>latitude</var>) order.
 * This matrix is shown here for completeness, but is not managed by this projection package. Axes swapping is managed
 * at a {@linkplain org.apache.sis.referencing.internal.ParameterizedTransformBuilder higher level}.</div>
 *
 * {@code NormalizedProjection} does not store the above cited parameters (central meridian, scale factor, <i>etc.</i>)
 * on intent (except indirectly), in order to make clear that those parameters are not used by subclasses.
 * The ability to recognize two {@code NormalizedProjection}s as {@linkplain #equals(Object, ComparisonMode) equivalent}
 * without consideration for the scale factor (among other) allow more efficient concatenation in some cases
 * (typically some combinations of reverse projection followed by a direct projection).
 *
 * <p>All angles (either fields, method parameters or return values) in this class and subclasses are
 * in radians. This is the opposite of {@link Parameters} where all angles are in CRS-dependent units,
 * typically decimal degrees.</p>
 *
 * <h2>Serialization</h2>
 * Serialization of this class is appropriate for short-term storage or RMI use, but may not be compatible
 * with future versions. For long term storage, WKT (Well Know Text) or XML are more appropriate.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  André Gosselin (MPO)
 * @author  Rueben Schulz (UBC)
 * @author  Rémi Maréchal (Geomatys)
 *
 * @see ContextualParameters
 * @see <a href="https://mathworld.wolfram.com/MapProjection.html">Map projections on MathWorld</a>
 */
public abstract class NormalizedProjection extends AbstractMathTransform2D implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4010883312927645853L;

    /**
     * Maximum difference allowed when comparing longitudes or latitudes in radians.
     * The current value takes the system-wide angular tolerance value (equivalent to
     * about 1 cm on Earth) converted to radians.
     *
     * <p>Some formulas use this tolerance value for testing sines or cosines of an angle.
     * In the sine case, this is justified because sin(θ) ≅ θ when θ is small.
     * Similar reasoning applies to cosine with cos(θ) ≅ θ + π/2 when θ is small.</p>
     *
     * <p>Some formulas may use this tolerance value as a <em>linear</em> tolerance on the unit sphere.
     * This is okay because the arc length for an angular tolerance θ is r⋅θ, but in this class r=1.</p>
     */
    static final double ANGULAR_TOLERANCE = Formulas.ANGULAR_TOLERANCE * (PI/180);
    // Note: an alternative way to compute this value could be Formulas.LINEAR_TOLERANCE / AUTHALIC_RADIUS.
    // But the latter is only 0.07% lower than the current value.

    /**
     * Desired accuracy for the result of iterative computations, in radians.
     * This constant defines the desired accuracy of methods like {@link ConformalProjection#φ(double)}.
     *
     * <p>The current value is 0.25 time the accuracy derived from {@link Formulas#LINEAR_TOLERANCE}.
     * So if the linear tolerance is 1 cm, then the accuracy that we will seek for is 0.25 cm (about
     * 4E-10 radians). The 0.25 factor is a safety margin for meeting the 1 cm accuracy.</p>
     */
    static final double ITERATION_TOLERANCE = ANGULAR_TOLERANCE * 0.25;

    /**
     * Maximum number of iterations for iterative computations.
     * The iterative methods used in subclasses should converge quickly (in 3 or 4 iterations)
     * when used for a planet with an eccentricity similar to Earth. But we allow a high limit
     * in case someone uses SIS for some planet with higher eccentricity.
     */
    static final int MAXIMUM_ITERATIONS = Formulas.MAXIMUM_ITERATIONS;

    /**
     * Arbitrary latitude threshold (in radians) for considering that a point is in the polar area.
     * This is used for implementations of the {@link #getDomain(DomainDefinition)} method.
     */
    static final double POLAR_AREA_LIMIT = PI/2 * (84d/90);

    /**
     * An arbitrarily large longitude value (in radians) for map projections capable to have infinite
     * extent in the east-west direction. This is the case of {@link Mercator} projection for example.
     * Longitudes have no real world meaning outside −180° … +180° range (unless wraparound is applied),
     * but we nevertheless accept large values without wraparound because they make envelope projection easier.
     * This is used for implementations of the {@link #getDomain(DomainDefinition)} method,
     * which use arbitrary limits anyway.
     */
    static final double LARGE_LONGITUDE_LIMIT = 100*PI;

    /**
     * The internal parameter descriptors. Keys are implementation classes.  Values are parameter descriptor groups
     * containing at least a parameter for the {@link #eccentricity} value, and optionally other internal parameter
     * added by some subclasses.
     *
     * <p>Entries are created only when first needed. Those descriptors are usually never created since they are
     * used only by {@link #getParameterDescriptors()}, which is itself invoked mostly for debugging purpose.</p>
     */
    @Debug
    private static final Map<Class<?>,ParameterDescriptorGroup> DESCRIPTORS = new HashMap<>();

    /**
     * The parameters used for creating this projection. They are used for formatting <i>Well Known Text</i> (WKT)
     * and error messages. Subclasses shall not use the values defined in this object for computation purpose,
     * except at construction time.
     *
     * @see #getContextualParameters()
     */
    final ContextualParameters context;

    /**
     * Ellipsoid eccentricity, equals to <code>sqrt({@linkplain #eccentricitySquared})</code>.
     * Value 0 means that the ellipsoid is spherical.
     */
    protected final double eccentricity;

    /**
     * The square of eccentricity: ℯ² = (a²-b²)/a² where
     * <var>ℯ</var> is the {@linkplain #eccentricity eccentricity},
     * <var>a</var> is the <i>semi-major</i> axis length and
     * <var>b</var> is the <i>semi-minor</i> axis length.
     */
    protected final double eccentricitySquared;

    /**
     * The inverse of this map projection.
     *
     * <h4>Implementation note</h4>
     * Creation of this object is not deferred to the first call to the {@link #inverse()} method because this
     * object is lightweight and typically needed soon anyway (may be as soon as {@code ConcatenatedTransform}
     * construction time). In addition this field is part of serialization form in order to preserve the
     * references graph.
     */
    private final Inverse inverse;

    /**
     * Maps the parameters to be used for initializing {@link NormalizedProjection} and its
     * {@linkplain ContextualParameters#getMatrix normalization / denormalization} matrices.
     * This is an enumeration of parameters found in almost every map projections, but under different names.
     * This enumeration allows {@code NormalizedProjection} subclasses to specify which parameter names, ranges
     * and default values should be used by the
     * {@linkplain NormalizedProjection#NormalizedProjection(OperationMethod, Parameters, Map) projection constructor}.
     *
     * <p>{@code NormalizedProjection} subclasses will typically provide values only for the following keys:
     * {@link #CENTRAL_MERIDIAN}, {@link #SCALE_FACTOR}, {@link #FALSE_EASTING} and {@link #FALSE_NORTHING}.</p>
     *
     * @author  Martin Desruisseaux (Geomatys)
     *
     * @see NormalizedProjection#NormalizedProjection(OperationMethod, Parameters, Map)
     */
    protected enum ParameterRole {
        /**
         * Maps the <i>semi-major axis length</i> parameter (symbol: <var>a</var>).
         * This value is used for computing {@link NormalizedProjection#eccentricity},
         * and is also a multiplication factor for the denormalization matrix.
         *
         * <p>Unless specified otherwise, this is always mapped to a parameter named {@code "semi_major"}.
         * {@code NormalizedProjection} subclasses typically do not need to provide a value for this key.</p>
         */
        SEMI_MAJOR,

        /**
         * Maps the <i>semi-minor axis length</i> parameter (symbol: <var>b</var>).
         * This value is used for computing {@link NormalizedProjection#eccentricity}.
         *
         * <p>Unless specified otherwise, this is always mapped to a parameter named {@code "semi_minor"}.
         * {@code NormalizedProjection} subclasses typically do not need to provide a value for this key.</p>
         */
        SEMI_MINOR,

        /**
         * Maps the parameter for the latitude where to compute the <cite>radius of conformal sphere</cite>
         * (symbol: <var>R</var><sub>c</sub>). If this parameter is provided, then the radius of the conformal
         * sphere at latitude φ will be used instead of the semi-major axis length in the denormalisation matrix.
         * In other words, if provided then <var>a</var> is replaced by <var>R</var><sub>c</sub> below:
         *
         * <div style="text-align:center">{@include ../transform/formulas.html#DenormalizeCartesian}</div>
         *
         * <h4>When to use</h4>
         * This enumeration value should be used only when the user requested explicitly the spherical formulas
         * of a conformal projection, for example the <q>Mercator (Spherical)</q> projection (EPSG:1026),
         * but the figure of the Earth may be an ellipsoid rather than a sphere.
         * This enumeration value can also be used for other kinds of projection except Equal Area, in which case
         * the {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid#getAuthalicRadius() authalic radius}
         * is preferred. In the majority of cases, this enumeration value can be ignored.
         */
        LATITUDE_OF_CONFORMAL_SPHERE_RADIUS,

        /**
         * Maps the <i>central meridian</i> parameter (symbol: λ₀).
         * This value is subtracted from the longitude values before the map projections.
         *
         * <p>Some common names for this parameter are:</p>
         * <ul>
         *   <li>Longitude of origin</li>
         *   <li>Longitude of false origin</li>
         *   <li>Longitude of natural origin</li>
         *   <li>Spherical longitude of origin</li>
         *   <li>Longitude of projection centre</li>
         * </ul>
         */
        CENTRAL_MERIDIAN,

        /**
         * Maps the <i>scale factor</i> parameter (symbol: <var>k₀</var>).
         * This is a multiplication factor for the (<var>x</var>,<var>y</var>) values obtained after map projections.
         *
         * <p>Some common names for this parameter are:</p>
         * <ul>
         *   <li>Scale factor at natural origin</li>
         *   <li>Scale factor on initial line</li>
         *   <li>Scale factor on pseudo standard parallel</li>
         * </ul>
         */
        SCALE_FACTOR,

        /**
         * Maps the <i>false easting</i> parameter (symbol: <var>FE</var>).
         * This is a translation term for the <var>x</var> values obtained after map projections.
         *
         * <p>Some common names for this parameter are:</p>
         * <ul>
         *   <li>False easting</li>
         *   <li>Easting at false origin</li>
         *   <li>Easting at projection centre</li>
         * </ul>
         */
        FALSE_EASTING,

        /**
         * Maps the <i>false westing</i> parameter (symbol: <var>FW</var>).
         * This is the same <var>x</var> translation than {@link #FALSE_EASTING}, but of opposite sign.
         *
         * <p>Actually, there is usually no parameter named "false westing" in a map projection.
         * But some projections like <q>Lambert Conic Conformal (West Orientated)</q> are
         * defined in such a way that their "false easting" parameter is effectively a "false westing".
         * This enumeration value can be used for informing {@link NormalizedProjection} about that fact.</p>
         */
        FALSE_WESTING,

        /**
         * Maps the <i>false northing</i> parameter (symbol: <var>FN</var>).
         * This is a translation term for the <var>y</var> values obtained after map projections.
         *
         * <p>Some common names for this parameter are:</p>
         * <ul>
         *   <li>False northing</li>
         *   <li>Northing at false origin</li>
         *   <li>Northing at projection centre</li>
         * </ul>
         */
        FALSE_NORTHING,

        /**
         * Maps the <i>false southing</i> parameter (symbol: <var>FS</var>).
         * This is the same <var>y</var> translation than {@link #FALSE_NORTHING}, but of opposite sign.
         *
         * <p>Actually, there is usually no parameter named "false southing" in a map projection.
         * But some projections like <q>Transverse Mercator (South Orientated)</q> are
         * defined in such a way that their "false northing" parameter is effectively a "false southing".
         * This enumeration value can be used for informing {@link NormalizedProjection} about that fact.</p>
         */
        FALSE_SOUTHING
    }

    /**
     * Constructs a new map projection from the supplied parameters.
     * This constructor applies the following operations on the
     * {@linkplain #getContextualParameters() contextual parameters}:
     *
     * <ul>
     *   <li>On the <b>normalization</b> matrix (to be applied before {@code this} transform):
     *     <ul>
     *       <li>{@linkplain ContextualParameters#normalizeGeographicInputs(double) Subtract}
     *           the <i>central meridian</i> value.</li>
     *       <li>Convert from degrees to radians.</li>
     *     </ul>
     *   </li>
     *   <li>On the <b>denormalization</b> matrix (to be applied after {@code this} transform):
     *     <ul>
     *       <li>{@linkplain MatrixSIS#convertAfter(int, Number, Number) Scale} by the <i>semi-major</i> axis length.</li>
     *       <li>If a scale factor is present (not all map projections have a scale factor), apply that scale.</li>
     *       <li>Translate by the <i>false easting</i> and <i>false northing</i> (after the scale).</li>
     *     </ul>
     *   </li>
     *   <li>On the <b>contextual parameters</b> (not the parameters of {@code this} transform):
     *     <ul>
     *       <li>Store the values for <i>semi-major</i> axis length, <i>semi-minor</i> axis length,
     *         <i>scale factor</i> (if present), <i>central meridian</i>,
     *         <i>false easting</i> and <i>false northing</i> values.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * In matrix form, this constructor creates the following matrices (subclasses are free to modify):
     * <table class="sis">
     *   <caption>Initial matrix coefficients after construction</caption>
     *   <tr>
     *     <th>Normalization</th>
     *     <th class="sep">Denormalization</th>
     *   </tr>
     *   <tr>
     *     <td>{@include ../transform/formulas.html#NormalizeGeographic}</td>
     *     <td class="sep">{@include ../transform/formulas.html#DenormalizeCartesian}</td>
     *   </tr>
     * </table>
     *
     * <h4>Which parameters are considered</h4>
     * The {@code roles} map specifies which parameters to look for <i>central meridian</i>,
     * <i>scale factor</i>, <i>false easting</i>, <i>false northing</i> and other values.
     * All entries in the {@code roles} map are optional.
     * All descriptors in the map shall comply to the following constraints:
     *
     * <ul>
     *   <li>Descriptors associated to {@link ParameterRole#SEMI_MAJOR}, {@link ParameterRole#SEMI_MINOR SEMI_MINOR},
     *     {@link ParameterRole#FALSE_EASTING FALSE_EASTING} and {@link ParameterRole#FALSE_NORTHING FALSE_NORTHING}
     *     shall have the same linear unit of measurement (usually metre).</li>
     *   <li>Descriptors associated to angular measures ({@link ParameterRole#CENTRAL_MERIDIAN} and
     *     {@link ParameterRole#LATITUDE_OF_CONFORMAL_SPHERE_RADIUS LATITUDE_OF_CONFORMAL_SPHERE_RADIUS})
     *     shall use degrees.</li>
     * </ul>
     *
     * Note that users can still use units of their choice in the {@link Parameters} object given in argument to
     * this constructor. But those values will be converted to the units of measurement specified by the parameter
     * descriptors in the {@code roles} map, which must be the above-cited units.
     *
     * @param method      description of the map projection parameters.
     * @param parameters  the parameters of the projection to be created.
     * @param roles       parameters to look for <i>central meridian</i>, <i>scale factor</i>,
     *                    <i>false easting</i>, <i>false northing</i> and other values.
     */
    protected NormalizedProjection(final OperationMethod method, final Parameters parameters,
            final Map<ParameterRole, ? extends ParameterDescriptor<? extends Number>> roles)
    {
        this(new Initializer(method, parameters, roles, null), null);
    }

    /**
     * Creates a new normalized projection from the parameters computed by the given initializer,
     * or from the parameters already computed by another projection.
     * Exactly one of {@code initializer} or {@code other} shall be non-null.
     *
     * The {@code other} argument may be used after we determined that the default implementation can
     * be replaced by another one, for example using spherical formulas instead of the ellipsoidal ones.
     * This constructor allows to transfer all parameters to the new instance without recomputing them.
     *
     * <h4>Design note</h4>
     * We do not define two separated constructors because doing so can force some subclasses
     * to duplicate non-trivial calculations in the two constructors.
     *
     * @param initializer  the initializer for computing map projection internal parameters, or {@code null}.
     * @param other        the other projection from which to compute parameters, or {@code null}.
     */
    NormalizedProjection(final Initializer initializer, final NormalizedProjection other) {
        if (initializer != null) {
            context             = initializer.context;
            eccentricitySquared = initializer.eccentricitySquared.doubleValue();
            eccentricity        = sqrt(eccentricitySquared);
            // Use of DoubleDouble.sqrt() does not make any difference here.
        } else {
            context             = other.context;
            eccentricity        = other.eccentricity;
            eccentricitySquared = other.eccentricitySquared;
        }
        inverse = new Inverse(this);
    }

    /**
     * Returns the variant of the map projection described by the given operation method.
     * Identifiers are tested first because they have precedence over operation names.
     *
     * @param  method        the user-specified projection method.
     * @param  variants      possible variants for the map projection.
     * @param  defaultValue  value to return if no match is found.
     * @return the variant of the given operation method, or {@code defaultValue} if none.
     */
    static <V extends ProjectionVariant> V variant(final OperationMethod method, final V[] variants, final V defaultValue) {
        for (final V variant : variants) {
            final String identifier = variant.getIdentifier();
            if (identifier != null) {
                for (final Identifier id : method.getIdentifiers()) {
                    if (Constants.EPSG.equals(id.getCodeSpace()) && identifier.equals(id.getCode())) {
                        return variant;
                    }
                }
            }
        }
        final String name = method.getName().getCode().replace('_',' ');
        for (final V variant : variants) {
            final Pattern regex = variant.getOperationNamePattern();
            if (regex != null && regex.matcher(name).matches()) {
                return variant;
            }
        }
        return defaultValue;
    }

    /**
     * Returns the sequence of <i>normalization</i> → {@code this} → <i>denormalization</i> transforms as a whole.
     * The transform returned by this method expects (<var>longitude</var>, <var>latitude</var>) coordinates
     * in <em>degrees</em> and returns (<var>x</var>,<var>y</var>) coordinates in <em>metres</em>.
     * Conversion to other units and {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes
     * changes in axis order} are <strong>not</strong> managed by the returned transform.
     *
     * <p>The default implementation is as below:</p>
     * {@snippet lang="java" :
     *     return getContextualParameters().completeTransform(parameters.getFactory(), this);
     *     }
     *
     * Subclasses can override this method if they wish to use alternative implementations under some circumstances.
     * For example, many subclasses will replace {@code this} by a simplified implementation if they detect that
     * the ellipsoid is actually spherical.
     *
     * @param  parameters  parameters and the factory to use for creating the transform.
     * @return the map projection from (λ,φ) to (<var>x</var>,<var>y</var>) coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     *
     * @see ContextualParameters#completeTransform(MathTransformFactory, MathTransform)
     */
    public MathTransform createMapProjection(final MathTransformProvider.Context parameters) throws FactoryException {
        return context.completeTransform(parameters.getFactory(), this);
    }

    /**
     * Returns a transform which may shift scaled longitude θ=n⋅λ inside the [−n⋅π … n⋅π] range.
     * The transform intentionally does <strong>not</strong> force θ to be inside that range in all cases.
     * We avoid explicit wraparounds as much as possible (as opposed to implicit wraparounds performed by
     * trigonometric functions) because they tend to introduce discontinuities. We perform wraparounds only
     * when necessary for the problem of area crossing the anti-meridian (±180°).
     *
     * <div class="note"><b>Example:</b>
     * a CRS for Alaska may have the central meridian at λ₀=−154° of longitude. If the point to project is
     * at λ=177° of longitude, calculations will be performed with Δλ=331° while the correct value that we
     * need to use is Δλ=−29°.</div>
     *
     * In order to avoid wraparound operations as much as possible, we test only the bound where anti-meridian
     * problem may happen; no wraparound will be applied for the opposite bound. Furthermore, we add or subtract
     * 360° only once. Even if the point did many turns around the Earth, the 360° shift will still be applied
     * at most once. The desire to apply the minimal number of shifts is the reason why we do not use
     * {@link Math#IEEEremainder(double, double)}.
     *
     * <h4>When to use</h4>
     * This method is invoked by map projections that multiply the longitude values by some scale factor before
     * to use them in trigonometric functions. Usually we do not explicitly wraparound the longitude values,
     * because trigonometric functions do that automatically for us. However if the longitude is multiplied
     * by some factor before to be used in trigonometric functions, then that implicit wraparound is not the
     * one we expect. The map projection code needs to perform explicit wraparound in such cases.
     *
     * @param  parameters  parameters and the factory to use for creating the normalization/denormalization steps.
     * @return the map projection from (λ,φ) to (<var>x</var>,<var>y</var>) coordinates with wraparound if needed.
     * @throws FactoryException if an error occurred while creating a transform.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-486">SIS-486</a>
     */
    final MathTransform completeWithWraparound(final MathTransformProvider.Context parameters) throws FactoryException {
        MathTransform kernel = this;
        final MatrixSIS normalize = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final double rotation = normalize.getElement(0, DIMENSION);
        if (rotation != 0 && Double.isFinite(rotation)) {                 // Finite check is paranoiac (shall always be true).
            kernel = new LongitudeWraparound(this,
                    LongitudeWraparound.boundOfScaledLongitude(normalize, rotation < 0), rotation);
        }
        return context.completeTransform(parameters.getFactory(), kernel);
    }

    /**
     * Returns the parameters used for creating the complete map projection. Those parameters describe a sequence of
     * <i>normalize</i> → {@code this} → <i>denormalize</i> transforms, <strong>not</strong> including
     * {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes axis swapping}.
     * Those parameters are used for formatting <i>Well Known Text</i> (WKT) and error messages.
     * Subclasses shall not use the values defined in the returned object for computation purpose,
     * except at construction time.
     *
     * @return the parameter values for the sequence of <i>normalize</i> → {@code this} → <i>denormalize</i>
     *         transforms, or {@code null} if unspecified.
     */
    @Override
    protected final ContextualParameters getContextualParameters() {
        return context;
    }

    /**
     * Returns a copy of non-linear internal parameter values of this {@code NormalizedProjection}.
     * The returned group contains at least the {@link #eccentricity} parameter value.
     * Some subclasses add more non-linear parameters, but most of them do not because many parameters
     * like the <i>scale factor</i> or the <i>false easting/northing</i> are handled by the
     * {@linkplain ContextualParameters#getMatrix (de)normalization affine transforms} instead.
     *
     * <div class="note"><b>Note:</b>
     * This method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters}.</div>
     *
     * @return a copy of the internal parameter values for this normalized projection.
     */
    @Debug
    @Override
    public ParameterValueGroup getParameterValues() {
        final ParameterValueGroup group = getParameterDescriptors().createValue();
        group.parameter("eccentricity").setValue(eccentricity);
        final String[] names  = getInternalParameterNames();
        final double[] values = getInternalParameterValues();
        for (int i=0; i<names.length; i++) {
            group.parameter(names[i]).setValue(values[i]);
        }
        return group;
    }

    /**
     * Returns a description of the non-linear internal parameters of this {@code NormalizedProjection}.
     * The returned group contains at least a descriptor for the {@link #eccentricity} parameter.
     * Subclasses may add more parameters.
     *
     * <p>This method is for inspecting the parameter values of this non-linear kernel only,
     * not for inspecting the {@linkplain #getContextualParameters() contextual parameters}.
     * Inspecting the kernel parameter values is usually for debugging purpose only.</p>
     *
     * @return a description of the internal parameters.
     */
    @Debug
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        Class<?> type = getClass();
        while (!Modifier.isPublic(type.getModifiers())) {
            type = type.getSuperclass();
        }
        ParameterDescriptorGroup group;
        synchronized (DESCRIPTORS) {
            group = DESCRIPTORS.get(type);
            if (group == null) {
                final ParameterBuilder builder = new ParameterBuilder().setRequired(true);
                if (type.getName().startsWith(Modules.CLASSNAME_PREFIX)) {
                    builder.setCodeSpace(Citations.SIS, Constants.SIS);
                }
                final String[] names = getInternalParameterNames();
                final var parameters = new ParameterDescriptor<?>[names.length + 1];
                parameters[0] = MapProjection.ECCENTRICITY;
                for (int i=1; i<parameters.length; i++) {
                    parameters[i] = builder.addName(names[i-1]).create(Double.class, null);
                }
                group = builder.addName(CharSequences.camelCaseToSentence(
                        type.getSimpleName()) + " (radians domain)").createGroup(1, 1, parameters);
                DESCRIPTORS.put(type, group);
            }
        }
        return group;
    }

    /**
     * Returns the names of any additional internal parameters (other than {@link #eccentricity})
     * that this projection has. The length of this array must be the same as the length of the
     * {@link #getInternalParameterValues()} array, if the latter is non-null.
     */
    String[] getInternalParameterNames() {
        return CharSequences.EMPTY_ARRAY;
    }

    /**
     * Returns the values of any additional internal parameters (other than {@link #eccentricity}) that
     * this projection has. Those values are also compared by {@link #equals(Object, ComparisonMode)}.
     */
    double[] getInternalParameterValues() {
        return null;
    }

    /**
     * The longitude value where wraparound is, or would be, applied by this map projection.
     * This is typically {@link Math#PI} (180° converted to radians) but not necessarily,
     * because implementations are free to scale the longitude values by an arbitrary factor.
     *
     * <p>The wraparound may not be really applied by the {@code transform(…)} methods.
     * Many map projections implicitly wraparound longitude values through the use of trigonometric functions
     * ({@code sin(λ)}, {@code cos(λ)}, <i>etc</i>). For those map projections, the wraparound is unconditional.
     * But some other map projections are capable to handle longitude values beyond the [−180° … +180°] range
     * as if the world was expanding toward infinity in east and west directions.
     * The most common example is the {@linkplain Mercator} projection.
     * In those latter cases, wraparounds are avoided as much as possible in order to facilitate the projection
     * of envelopes, geometries or rasters, where discontinuities (sudden jumps of 360°) cause artifacts.</p>
     *
     * @return the longitude value where wraparound is or would be applied.
     *
     * @see #getDomain(DomainDefinition)
     */
    final double getWraparoundLongitude() {
        return LongitudeWraparound.boundOfScaledLongitude(
                context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION), false);
    }

    /*
     * TODO: consider adding a sqrt1ms(x) method for sqrt(1 - x*x), which could be implemented as sqrt(fma(x, -x, 1)).
     * The use of Math.fma(…) in this context would be valuable especially when x is close to 1 (to be verified).
     * We may also add a method for sqrt(1 - eccentricitySquared*x*x). Maybe `eccentricitySquared` should be made
     * package private and negative for easier use with fma.
     */

    /**
     * Projects a single coordinate tuple in {@code srcPts} at the given offset
     * and stores the result in {@code dstPts} at the given offset.
     * In addition, opportunistically computes the transform derivative if requested.
     *
     * <h4>Normalization</h4>
     * The input coordinates are (<var>λ</var>,<var>φ</var>) (the variable names for <var>longitude</var> and
     * <var>latitude</var> respectively) angles in radians, eventually pre-multiplied by projection-specific factors.
     * Input coordinates shall have the <i>central meridian</i> removed from the longitude by the caller
     * before this method is invoked. After this method is invoked, the caller will need to multiply the output
     * coordinates by the global <i>scale factor</i>,
     * apply the (<i>false easting</i>, <i>false northing</i>) offset
     * and eventually other projection-specific factors.
     * This means that projections that implement this method are performed on a sphere or ellipse
     * having a semi-major axis length of 1.
     *
     * <div class="note"><b>Note 1:</b> it is generally not necessary to know the projection-specific additional
     * factors applied by subclasses on the input and output values, because {@code NormalizedProjection} should
     * never be used directly. {@code NormalizedProjection} instances are used only indirectly as a step in a
     * concatenated transform that include the <i>normalization</i> and <i>denormalization</i>
     * matrices documented in this class javadoc.</div>
     *
     * <div class="note"><b>Note 2:</b> in the <a href="https://proj.org/">PROJ</a> library, the same standardization,
     * described above, is handled by {@code pj_fwd.c}, except for the projection-specific additional factors.</div>
     *
     * <h4>Argument checks</h4>
     * The input longitude and latitude are usually (but not always) in the range [-π … π] and [-π/2 … π/2] respectively.
     * However, values outside those ranges are accepted on the assumption that most implementations use those values
     * only in trigonometric functions like {@linkplain Math#sin(double) sine} and {@linkplain Math#cos(double) cosine}.
     * If this assumption is not applicable to a particular subclass, then it is implementer responsibility to check
     * the range.
     *
     * @param  srcPts    the array containing the source point coordinates,
     *                   as (<var>longitude</var>, <var>latitude</var>) angles in <strong>radians</strong>.
     * @param  srcOff    the offset of the single coordinate tuple to be converted in the source array.
     * @param  dstPts    the array into which the converted coordinates is returned (may be the same as {@code srcPts}).
     *                   Coordinates will be expressed in a dimensionless unit, as a linear distance on a unit sphere or ellipse.
     *                   This array may be {@code null} if the caller is interested only in the derivative.
     * @param  dstOff    the offset of the location of the converted coordinates that is stored in the destination array.
     * @param  derivate  {@code true} for computing the derivative, or {@code false} if not needed.
     * @return the matrix of the projection derivative at the given source position,
     *         or {@code null} if the {@code derivate} argument is {@code false}.
     * @throws ProjectionException if the coordinates cannot be converted.
     */
    @Override
    public abstract Matrix transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, boolean derivate)
            throws ProjectionException;

    /**
     * Inverse converts the single coordinate tuple in {@code srcPts} at the given offset and stores the result in
     * {@code ptDst} at the given offset. The output coordinates are (<var>longitude</var>, <var>latitude</var>)
     * angles in radians, usually (but not necessarily) in the range [-π … π] and [-π/2 … π/2] respectively.
     *
     * <h4>Normalization</h4>
     * Input coordinates shall have the (<i>false easting</i>, <i>false northing</i>) removed
     * by the caller and the result divided by the global <i>scale factor</i> before this method is invoked.
     * After this method is invoked, the caller will need to add the <i>central meridian</i> to the longitude
     * in the output coordinates. This means that projections that implement this method are performed on a sphere
     * or ellipse having a semi-major axis of 1.
     * Additional projection-specific factors may also need to be applied (see class javadoc).
     *
     * <div class="note"><b>Note:</b> in the <a href="https://proj.org/">PROJ</a> library, the same standardization,
     * described above, is handled by {@code pj_inv.c}, except for the projection-specific additional factors.</div>
     *
     * @param  srcPts  the array containing the source point coordinates, as linear distance on a unit sphere or ellipse.
     * @param  srcOff  the offset of the point to be converted in the source array.
     * @param  dstPts  the array into which the converted point coordinates is returned (may be the same as {@code srcPts}).
     *                 Coordinates will be (<var>longitude</var>, <var>latitude</var>) angles in <strong>radians</strong>.
     * @param  dstOff  the offset of the location of the converted point that is stored in the destination array.
     * @throws ProjectionException if the point cannot be converted.
     */
    protected abstract void inverseTransform(double[] srcPts, int srcOff, double[] dstPts, int dstOff)
            throws ProjectionException;

    /**
     * Returns the inverse of this map projection.
     * Subclasses do not need to override this method, as they should override
     * {@link #inverseTransform(double[], int, double[], int) inverseTransform(…)} instead.
     *
     * @return the inverse of this map projection.
     */
    @Override
    public MathTransform2D inverse() {
        return inverse;
    }

    /**
     * Reverse of a normalized map projection.
     * Note that a slightly modified copy of this class is in {@code LongitudeWraparound.Inverse}.
     * If this is class is modified, consider updating {@code LongitudeWraparound.Inverse} accordingly.
     *
     * @author  Martin Desruisseaux (Geomatys)
     */
    private static final class Inverse extends AbstractMathTransform2D.Inverse implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 6014176098150309651L;

        /**
         * The projection to reverse, which is the enclosing transform.
         */
        private final NormalizedProjection forward;

        /**
         * Creates a reverse projection for the given forward projection.
         */
        Inverse(final NormalizedProjection forward) {
            this.forward = forward;
        }

        /**
         * Returns the inverse of this math transform, which is the forward projection.
         */
        @Override
        public MathTransform2D inverse() {
            return forward;
        }

        /**
         * Reverse projects the specified {@code srcPts} and stores the result in {@code dstPts}.
         * If the derivative has been requested, then this method will delegate the derivative
         * calculation to the enclosing class and inverts the resulting matrix.
         */
        @Override
        public Matrix transform(final double[] srcPts, final int srcOff,
                                      double[] dstPts,       int dstOff,
                                final boolean derivate) throws TransformException
        {
            if (!derivate) {
                forward.inverseTransform(srcPts, srcOff, dstPts, dstOff);
                return null;
            } else {
                if (dstPts == null) {
                    dstPts = new double[DIMENSION];
                    dstOff = 0;
                }
                forward.inverseTransform(srcPts, srcOff, dstPts, dstOff);
                return Matrices.inverse(forward.transform(dstPts, dstOff, null, 0, true));
            }
        }

        /**
         * Inverse transforms an arbitrary number of coordinate tuples. This method optimizes the
         * case where conversions can be applied by a loop with indices in increasing order.
         */
        @Override
        public void transform(final double[] srcPts, int srcOff,
                              final double[] dstPts, int dstOff, int numPts) throws TransformException
        {
            if (srcPts == dstPts && srcOff < dstOff) {
                super.transform(srcPts, srcOff, dstPts, dstOff, numPts);
            } else while (--numPts >= 0) {
                forward.inverseTransform(srcPts, srcOff, dstPts, dstOff);
                srcOff += DIMENSION;
                dstOff += DIMENSION;
            }
        }

        /**
         * Concatenates in an optimized way this reverse projection with its neighbor, if possible.
         * This method delegates to {@link #tryInverseConcatenate(TransformJoiner)}.
         */
        @Override
        protected void tryConcatenate(final TransformJoiner context) throws FactoryException {
            if (!forward.tryInverseConcatenate(context)) {
                super.tryConcatenate(context);
            }
        }
    }

    /**
     * Concatenates in an optimized way this projection with its neighbor, if possible.
     * This method returns whether or not an optimization has been done.
     *
     * @param  context  information about the neighbor transforms, and the object where to set the result.
     * @throws FactoryException if an error occurred while combining the transforms.
     * @return whether or not an optimization has been done.
     */
    boolean tryInverseConcatenate(TransformJoiner context) throws FactoryException {
        return false;
    }

    /**
     * Computes a hash code value for this {@code NormalizedProjection}.
     *
     * @return the hash code value.
     */
    @Override
    protected int computeHashCode() {
        long c = Double.doubleToLongBits(eccentricity);
        final double[] parameters = getInternalParameterValues();
        if (parameters != null) {
            for (int i=0; i<parameters.length; i++) {
                c = c*31 + Double.doubleToLongBits(parameters[i]);
            }
        }
        return super.computeHashCode() ^ Long.hashCode(c);
    }

    /**
     * Compares the given object with this transform for equivalence. The default implementation checks if
     * {@code object} is an instance of the same class as {@code this}, then compares the eccentricity.
     *
     * <p>If this method returns {@code true}, then for any given identical source position, the two compared map
     * projections shall compute the same target position. Many of the {@linkplain #getContextualParameters()
     * contextual parameters} used for creating the map projections are irrelevant and do not need to be known.
     * Those projection parameters will be compared only if the comparison mode is {@link ComparisonMode#STRICT}
     * or {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}.</p>
     *
     * <h4>Example</h4>
     * A {@linkplain Mercator Mercator} projection can be created in the 2SP case with a <i>standard parallel</i>
     * value of 60°. The same projection can also be created in the 1SP case with a <i>scale factor</i> of 0.5.
     * Nevertheless those two map projections applied on a sphere gives identical results. Considering them as
     * equivalent allows the referencing module to transform coordinates between those two projections more efficiently.
     *
     * @param  object  the object to compare with this map projection for equivalence.
     * @param  mode    the strictness level of the comparison. Default to {@link ComparisonMode#STRICT}.
     * @return {@code true} if the given object is equivalent to this map projection.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (!super.equals(object, mode)) {
            return false;
        }
        final NormalizedProjection that = (NormalizedProjection) object;
        switch (mode) {
            case STRICT:
            case BY_CONTRACT: {
                if (!Objects.equals(context, that.context)) {
                    return false;
                }
                // Fall through for comparing the eccentricity.
            }
            case IGNORE_METADATA: {
                /*
                 * There is no need to compare both `eccentricity` and `eccentricitySquared` since the former
                 * is computed from the latter. We are better to compare `eccentricitySquared` since it is the
                 * original value from which the other value is derived.
                 */
                if (!Numerics.equals(eccentricitySquared, that.eccentricitySquared)) {
                    return false;
                }
                break;
            }
            default: {
                /*
                 * We want to compare the eccentricity with a tolerance threshold corresponding approximately
                 * to an error of 1 cm on Earth. The eccentricity for an ellipsoid of semi-major axis a=1 is:
                 *
                 *     ℯ² = 1 - b²
                 *
                 * If we add a slight ε error to the semi-minor axis length (where ε will be our linear tolerance
                 * threshold), we get:
                 *
                 *     (ℯ + ε′)²    =    1 - (b + ε)²    ≈    1 - (b² + 2⋅b⋅ε)    assuming ε ≪ b
                 *
                 * Replacing  1 - b²  by  ℯ²:
                 *
                 *     ℯ² + 2⋅ℯ⋅ε′  ≈   ℯ² - 2⋅b⋅ε
                 *
                 * After a few rearrangements:
                 *
                 *     ε′  ≈   ε⋅(ℯ - 1/ℯ)
                 *
                 * Note that  ε′  is negative for  ℯ < 1  so we actually need to compute  ε⋅(1/ℯ - ℯ)  instead.
                 * The result is less than 2E-8 for the eccentricity of the Earth.
                 */
                final double e = max(eccentricity, that.eccentricity);
                if (!Numerics.epsilonEqual(eccentricity, that.eccentricity, ANGULAR_TOLERANCE * (1/e - e))) {
                    assert (mode != ComparisonMode.DEBUG) : Numerics.messageForDifference(
                            "eccentricity", eccentricity, that.eccentricity);
                    return false;
                }
                break;
            }
        }
        /*
         * Compares the internal parameter names and values. Many implementations have no parameter other
         * than the eccentricity (because other parameters can often be stored in normalization matrices),
         * so the `values` array will often be null. For some implementations offering different variants
         * of a map projection, the number of internal parameters depends on the variant.
         */
        final double[] values = this.getInternalParameterValues();
        final double[] others = that.getInternalParameterValues();
        if (values == null) {
            return others == null;
        }
        if (others != null && values.length == others.length) {
            final String[] names = getInternalParameterNames();
            if (Arrays.equals(names, that.getInternalParameterNames())) {
                for (int i=0; i<values.length; i++) {
                    if (!Numerics.epsilonEqual(values[i], others[i], mode)) {
                        assert (mode != ComparisonMode.DEBUG) : Numerics.messageForDifference(names[i], values[i], others[i]);
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
