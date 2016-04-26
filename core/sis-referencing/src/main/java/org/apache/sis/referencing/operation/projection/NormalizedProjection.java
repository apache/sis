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
import java.util.HashMap;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.SingleOperation;
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
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Utilities;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;
import org.opengis.referencing.ReferenceIdentifier;


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
 *   The multiplication by the scale factor (<var>k₀</var>) and the translation by false easting (FE) and false
 *   northing (FN) are applied by the {@linkplain ContextualParameters#getMatrix denormalization} affine transform.</li>
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
 * at a {@linkplain org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#createParameterizedTransform
 * higher level}.</div>
 *
 * {@code NormalizedProjection} does not store the above cited parameters (central meridian, scale factor, <i>etc.</i>)
 * on intend (except indirectly), in order to make clear that those parameters are not used by subclasses.
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
 * @version 0.7
 * @module
 *
 * @see ContextualParameters
 * @see <a href="http://mathworld.wolfram.com/MapProjection.html">Map projections on MathWorld</a>
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
    // But the later is only 0.07% lower than the current value.

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
     * The internal parameter descriptors. Keys are implementation classes.  Values are parameter descriptor groups
     * containing at least a parameter for the {@link #eccentricity} value, and optionally other internal parameter
     * added by some subclasses.
     *
     * <p>Entries are created only when first needed. Those descriptors are usually never created since they are
     * used only by {@link #getParameterDescriptors()}, which is itself invoked mostly for debugging purpose.</p>
     */
    @Debug
    private static final Map<Class<?>,ParameterDescriptorGroup> DESCRIPTORS = new HashMap<Class<?>,ParameterDescriptorGroup>();

    /**
     * The parameters used for creating this projection. They are used for formatting <cite>Well Known Text</cite> (WKT)
     * and error messages. Subclasses shall not use the values defined in this object for computation purpose, except at
     * construction time.
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
     * <var>a</var> is the <cite>semi-major</cite> axis length and
     * <var>b</var> is the <cite>semi-minor</cite> axis length.
     */
    protected final double eccentricitySquared;

    /**
     * The inverse of this map projection.
     *
     * <div class="note"><b>Note:</b>
     * creation of this object is not deferred to the first call to the {@link #inverse()} method because this
     * object is lightweight and typically needed soon anyway (may be as soon as {@code ConcatenatedTransform}
     * construction time). In addition this field is part of serialization form in order to preserve the
     * references graph.</div>
     */
    private final MathTransform2D inverse;

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
     * @since   0.6
     * @version 0.6
     * @module
     *
     * @see NormalizedProjection#NormalizedProjection(OperationMethod, Parameters, Map)
     */
    protected static enum ParameterRole {
        /**
         * Maps the <cite>semi-major axis length</cite> parameter (symbol: <var>a</var>).
         * This value is used for computing {@link NormalizedProjection#eccentricity},
         * and is also a multiplication factor for the denormalization matrix.
         *
         * <p>Unless specified otherwise, this is always mapped to a parameter named {@code "semi_major"}.
         * {@code NormalizedProjection} subclasses typically do not need to provide a value for this key.</p>
         */
        SEMI_MAJOR,

        /**
         * Maps the <cite>semi-minor axis length</cite> parameter (symbol: <var>b</var>).
         * This value is used for computing {@link NormalizedProjection#eccentricity}.
         *
         * <p>Unless specified otherwise, this is always mapped to a parameter named {@code "semi_minor"}.
         * {@code NormalizedProjection} subclasses typically do not need to provide a value for this key.</p>
         */
        SEMI_MINOR,

        /**
         * Maps the parameter for the latitude where to compute the <cite>radius of conformal sphere</cite>
         * (symbol: <var>R</var><sub>c</sub>). If this parameter is provided, then the radius of the conformal
         * sphere at latitude φ will be used instead than the semi-major axis length in the denormalisation matrix.
         * In other words, if provided then <var>a</var> is replaced by <var>R</var><sub>c</sub> below:
         *
         * <center>{@include ../transform/formulas.html#DenormalizeCartesian}</center>
         *
         * <p>This enumeration shall be used <strong>only</strong> when the user requested explicitely spherical
         * formulas, for example the <cite>"Mercator (Spherical)"</cite> projection (EPSG:1026), but the figure
         * of the Earth may be an ellipsoid rather than a sphere. In the majority of cases, this enumeration should
         * not be used.</p>
         */
        LATITUDE_OF_CONFORMAL_SPHERE_RADIUS,

        /**
         * Maps the <cite>central meridian</cite> parameter (symbol: λ₀).
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
         * Maps the <cite>scale factor</cite> parameter (symbol: <var>k₀</var>).
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
         * Maps the <cite>false easting</cite> parameter (symbol: <var>FE</var>).
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
         * Maps the <cite>false westing</cite> parameter (symbol: <var>FW</var>).
         * This is the same <var>x</var> translation than {@link #FALSE_EASTING}, but of opposite sign.
         *
         * <p>Actually, there is usually no parameter named "false westing" in a map projection.
         * But some projections like <cite>"Lambert Conic Conformal (West Orientated)"</cite> are
         * defined in such a way that their "false easting" parameter is effectively a "false westing".
         * This enumeration value can be used for informing {@link NormalizedProjection} about that fact.</p>
         */
        FALSE_WESTING,

        /**
         * Maps the <cite>false northing</cite> parameter (symbol: <var>FN</var>).
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
         * Maps the <cite>false southing</cite> parameter (symbol: <var>FS</var>).
         * This is the same <var>y</var> translation than {@link #FALSE_NORTHING}, but of opposite sign.
         *
         * <p>Actually, there is usually no parameter named "false southing" in a map projection.
         * But some projections like <cite>"Transverse Mercator (South Orientated)"</cite> are
         * defined in such a way that their "false northing" parameter is effectively a "false southing".
         * This enumeration value can be used for informing {@link NormalizedProjection} about that fact.</p>
         */
        FALSE_SOUTHING
    }

    /**
     * Constructs a new map projection from the supplied parameters.
     * This constructor applies the following operations on the {@link ContextualParameter}:
     *
     * <ul>
     *   <li>On the <b>normalization</b> matrix (to be applied before {@code this} transform):
     *     <ul>
     *       <li>{@linkplain ContextualParameters#normalizeGeographicInputs(double) Subtract}
     *           the <cite>central meridian</cite> value.</li>
     *       <li>Convert from degrees to radians.</li>
     *     </ul>
     *   </li>
     *   <li>On the <b>denormalization</b> matrix (to be applied after {@code this} transform):
     *     <ul>
     *       <li>{@linkplain MatrixSIS#convertAfter(int, Number, Number) Scale} by the <cite>semi-major</cite> axis length.</li>
     *       <li>If a scale factor is present (not all map projections have a scale factor), apply that scale.</li>
     *       <li>Translate by the <cite>false easting</cite> and <cite>false northing</cite> (after the scale).</li>
     *     </ul>
     *   </li>
     *   <li>On the <b>contextual parameters</b> (not the parameters of {@code this} transform):
     *     <ul>
     *       <li>Store the values for <cite>semi-major</cite> axis length, <cite>semi-minor</cite> axis length,
     *         <cite>scale factor</cite> (if present), <cite>central meridian</cite>,
     *         <cite>false easting</cite> and <cite>false northing</cite> values.</li>
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
     * <div class="section">Which parameters are considered</div>
     * The {@code roles} map specifies which parameters to look for <cite>central meridian</cite>,
     * <cite>scale factor</cite>, <cite>false easting</cite>, <cite>false northing</cite> and other values.
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
     * @param method     Description of the map projection parameters.
     * @param parameters The parameters of the projection to be created.
     * @param roles Parameters to look for <cite>central meridian</cite>, <cite>scale factor</cite>,
     *        <cite>false easting</cite>, <cite>false northing</cite> and other values.
     */
    protected NormalizedProjection(final OperationMethod method, final Parameters parameters,
            final Map<ParameterRole, ? extends ParameterDescriptor<? extends Number>> roles)
    {
        this(new Initializer(method, parameters, roles, (byte) 0));
    }

    /**
     * Creates a new normalized projection from the parameters computed by the given initializer.
     *
     * @param initializer The initializer for computing map projection internal parameters.
     */
    NormalizedProjection(final Initializer initializer) {
        context             = initializer.context;
        eccentricitySquared = initializer.eccentricitySquared.value;
        eccentricity        = sqrt(eccentricitySquared);  // DoubleDouble.sqrt() does not make any difference here.
        inverse             = new Inverse();
    }

    /**
     * Creates a new projection initialized to the values of the given one. This constructor may be invoked after
     * we determined that the default implementation can be replaced by an other one, for example using spherical
     * formulas instead than the ellipsoidal ones. This constructor allows to transfer all parameters to the new
     * instance without recomputing them.
     */
    NormalizedProjection(final NormalizedProjection other) {
        context             = other.context;
        eccentricity        = other.eccentricity;
        eccentricitySquared = other.eccentricitySquared;
        inverse             = new Inverse();
    }

    /**
     * Returns {@code true} if the projection specified by the given method has the given keyword or identifier.
     * If non-null, the given identifier is presumed in the EPSG namespace and has precedence over the keyword.
     *
     * <div class="note"><b>Implementation note:</b>
     * Since callers usually give a constant string for the {@code regex} argument, it would be more efficient to
     * compile the {@link java.util.regex.Pattern} once for all. However the regular expression is used only as a
     * fallback if the descriptor does not contain EPSG identifier, which should be rare. Usually, the regular
     * expression will never be compiled.</div>
     *
     * @param  parameters The user-specified parameters.
     * @param  regex      The regular expression to use when using the operation name as the criterion.
     * @param  identifier The identifier to compare against the operation method name.
     * @return {@code true} if the name of the given operation method contains the given keyword
     *         or has an EPSG identifier equals to the given identifier.
     */
    static boolean identMatch(final OperationMethod method, final String regex, final String identifier) {
        if (identifier != null) {
            for (final ReferenceIdentifier id : method.getIdentifiers()) {
                if (Constants.EPSG.equals(id.getCodeSpace())) {
                    return identifier.equals(id.getCode());
                }
            }
        }
        return method.getName().getCode().replace('_',' ').matches(regex);
    }

    /**
     * Returns the sequence of <cite>normalization</cite> → {@code this} → <cite>denormalization</cite> transforms
     * as a whole. The transform returned by this method expects (<var>longitude</var>, <var>latitude</var>)
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
     * ellipsoid is actually spherical.
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
     * If this map projection can not handle the parameters given by the user but an other projection could, delegates
     * to the other projection. This method can be invoked by some {@link #createMapProjection(MathTransformFactory)}
     * implementations when the other projection can be seen as a special case.
     *
     * <div class="note"><b>Example:</b>
     * the {@link ObliqueStereographic} formulas do not work anymore when the latitude of origin is 90°N or 90°S,
     * because some internal coefficients become infinite. However the {@link PolarStereographic} implementation
     * is designed especially for those special cases. So the {@code ObliqueStereographic.createMapProjection(…)}
     * method can redirect to {@code PolarStereographic.createMapProjection(…)} when it detects such cases.</div>
     *
     * It is caller's responsibility to choose an alternative method that can understand the parameters which were
     * given to this original projection.
     *
     * @param  factory The factory given to {@link #createMapProjection(MathTransformFactory)}.
     * @param  name    The name of the alternative map projection to use.
     * @return The alternative projection.
     * @throws FactoryException if an error occurred while creating the alternative projection.
     *
     * @since 0.7
     */
    final MathTransform delegate(final MathTransformFactory factory, final String name) throws FactoryException {
        final OperationMethod method;
        if (factory instanceof DefaultMathTransformFactory) {
            method = ((DefaultMathTransformFactory) factory).getOperationMethod(name);
        } else {
            method = ReferencingServices.getInstance().getOperationMethod(
                    factory.getAvailableMethods(SingleOperation.class), name);
        }
        if (method instanceof MathTransformProvider) {
            return ((MathTransformProvider) method).createMathTransform(factory, context);
        } else {
            throw new FactoryException(Errors.format(Errors.Keys.UnsupportedImplementation_1,
                    (method != null ? method : factory).getClass()));
        }
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
     * Returns a copy of non-linear internal parameter values of this {@code NormalizedProjection}.
     * The returned group contains at least the {@link #eccentricity} parameter value.
     * Some subclasses add more non-linear parameters, but most of them do not because many parameters
     * like the <cite>scale factor</cite> or the <cite>false easting/northing</cite> are handled by the
     * {@linkplain ContextualParameters#getMatrix (de)normalization affine transforms} instead.
     *
     * <div class="note"><b>Note:</b>
     * This method is mostly for {@linkplain org.apache.sis.io.wkt.Convention#INTERNAL debugging purposes}
     * since the isolation of non-linear parameters in this class is highly implementation dependent.
     * Most GIS applications will instead be interested in the {@linkplain #getContextualParameters()
     * contextual parameters}.</div>
     *
     * @return A copy of the internal parameter values for this normalized projection.
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
     * @return A description of the internal parameters.
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
                if (Utilities.isSIS(type)) {
                    builder.setCodeSpace(Citations.SIS, Constants.SIS);
                }
                final String[] names = getInternalParameterNames();
                final ParameterDescriptor<?>[] parameters = new ParameterDescriptor<?>[names.length + 1];
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
     * that this projection has. The length of this array must be the same than the length of the
     * {@link #getInternalParameterValues()} array, if the later is non-null.
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
     * Computes a hash code value for this {@code NormalizedProjection}.
     *
     * @return The hash code value.
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
        return super.computeHashCode() ^ Numerics.hashCode(c);
    }

    /**
     * Compares the given object with this transform for equivalence. The default implementation checks if
     * {@code object} is an instance of the same class than {@code this}, then compares the eccentricity.
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
                 * There is no need to compare both 'eccentricity' and 'eccentricitySquared' since the former
                 * is computed from the later. We are better to compare 'eccentricitySquared' since it is the
                 * original value from which the other value is derived.
                 */
                if (!Numerics.equals(eccentricitySquared, that.eccentricitySquared)) {
                    return false;
                }
                break;
            }
            default: {
                /*
                 * We want to compare the eccentricity with a tolerance threshold corresponding approximatively
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
        final double[] parameters = getInternalParameterValues();
        if (parameters != null) {
            /*
             * super.equals(…) guarantees that the two objects are of the same class.
             * So in SIS implementation, this implies that the arrays have the same length.
             */
            final double[] others = that.getInternalParameterValues();
            assert others.length == parameters.length;
            for (int i=0; i<parameters.length; i++) {
                if (!Numerics.epsilonEqual(parameters[i], others[i], mode)) {
                    assert (mode != ComparisonMode.DEBUG) : Numerics.messageForDifference(
                            getInternalParameterNames()[i], parameters[i], others[i]);
                    return false;
                }
            }
        }
        return true;
    }
}
