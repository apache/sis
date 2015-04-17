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
import java.util.Map;
import java.util.EnumMap;
import java.io.Serializable;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
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
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform2D;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.util.DoubleDouble;
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
 * at a {@linkplain org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#createBaseToDerived(
 * org.opengis.referencing.cs.CoordinateSystem, org.opengis.referencing.operation.MathTransform,
 * org.opengis.referencing.cs.CoordinateSystem) higher level}.</div>
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
 * @version 0.6
 * @module
 *
 * @see ContextualParameters
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
     * Maps the parameters to be used for initializing {@link NormalizedProjection} and its
     * {@linkplain ContextualParameters#getMatrix(boolean) normalization / denormalization} matrices.
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
         * This value is used for computing {@link NormalizedProjection#excentricity},
         * and is also a multiplication factor for the denormalization matrix.
         *
         * <p>Unless specified otherwise, this is always mapped to a parameter named {@code "semi_major"}.
         * {@code NormalizedProjection} subclasses typically do not need to provide a value for this key.</p>
         */
        SEMI_MAJOR(Constants.SEMI_MAJOR),

        /**
         * Maps the <cite>semi-minor axis length</cite> parameter (symbol: <var>b</var>).
         * This value is used for computing {@link NormalizedProjection#excentricity}.
         *
         * <p>Unless specified otherwise, this is always mapped to a parameter named {@code "semi_minor"}.
         * {@code NormalizedProjection} subclasses typically do not need to provide a value for this key.</p>
         */
        SEMI_MINOR(Constants.SEMI_MINOR),

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
        LATITUDE_OF_CONFORMAL_SPHERE_RADIUS(null),

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
        CENTRAL_MERIDIAN(Constants.CENTRAL_MERIDIAN),

        /**
         * Maps the <cite>scale factor</cite> parameter (symbol: <var>k</var>₀).
         * This is a multiplication factor for the (<var>x</var>,<var>y</var>) values obtained after map projections.
         *
         * <p>Some common names for this parameter are:</p>
         * <ul>
         *   <li>Scale factor at natural origin</li>
         *   <li>Scale factor on initial line</li>
         *   <li>Scale factor on pseudo standard parallel</li>
         * </ul>
         */
        SCALE_FACTOR(Constants.SCALE_FACTOR),

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
        FALSE_EASTING(Constants.FALSE_EASTING),

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
        FALSE_NORTHING(Constants.FALSE_NORTHING);

        /**
         * The OGC name for this parameter. This is used only when inferring automatically the role map.
         * We use the OGC name instead than the EPSG name because OGC names are identical for a wider
         * range of projections (e.g. {@code "scale_factor"} for almost all projections).
         */
        private final String name;

        /**
         * Creates a new parameter role associated to the given OGC name.
         */
        private ParameterRole(final String name) {
            this.name = name;
        }

        /**
         * Provides default (<var>role</var> → <var>parameter</var>) associations for the given map projection.
         * This is a convenience method for a typical set of parameters found in map projections.
         * This method expects a {@code projection} argument containing descriptors for the given parameters
         * (using OGC names):
         *
         * <ul>
         *   <li>{@code "semi_major"}</li>
         *   <li>{@code "semi_minor"}</li>
         *   <li>{@code "central_meridian"}</li>
         *   <li>{@code "scale_factor"}</li>
         *   <li>{@code "false_easting"}</li>
         *   <li>{@code "false_northing"}</li>
         * </ul>
         *
         * <div class="note"><b>Note:</b>
         * Apache SIS uses EPSG names as much as possible, but this method is an exception to this rule.
         * In this particular case we use OGC names because they are identical for a wide range of projections.
         * For example there is at least {@linkplain #SCALE_FACTOR three different EPSG names} for the
         * <cite>"scale factor"</cite> parameter, which OGC defines only {@code "scale_factor"} for all of them.</div>
         *
         * @param  projection The map projection method for which to infer (<var>role</var> → <var>parameter</var>) associations.
         * @return The parameters associated to most role in this enumeration.
         * @throws ParameterNotFoundException if one of the above-cited parameters is not found in the given projection method.
         * @throws ClassCastException if a parameter has been found but is not an instance of {@code ParameterDescriptor<Double>}.
         */
        public static Map<ParameterRole, ParameterDescriptor<Double>> defaultMap(final OperationMethod projection)
                throws ParameterNotFoundException, ClassCastException
        {
            final ParameterDescriptorGroup parameters = projection.getParameters();
            final EnumMap<ParameterRole, ParameterDescriptor<Double>> roles = new EnumMap<>(ParameterRole.class);
            for (final ParameterRole role : values()) {
                if (role.name != null) {
                    final GeneralParameterDescriptor p = parameters.descriptor(role.name);
                    roles.put(role, Parameters.cast((ParameterDescriptor<?>) p, Double.class));
                }
            }
            return roles;
        }
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
     *        <cite>false easting</cite>, <cite>false northing</cite> and other values, or {@code null}
     *        for the {@linkplain ParameterRole#defaultMap(OperationMethod) default associations}.
     */
    protected NormalizedProjection(final OperationMethod method, final Parameters parameters,
            Map<ParameterRole, ? extends ParameterDescriptor<Double>> roles)
    {
        ensureNonNull("method", method);
        ensureNonNull("parameters", parameters);
        if (roles == null) {
            roles = ParameterRole.defaultMap(method);
        }
        context = new ContextualParameters(method);
        /*
         * Note: we do not use Map.getOrDefault(K,V) below because the user could have explicitly associated
         * a null value to keys (we are paranoiac...) and because it conflicts with the "? extends" part of
         * in this constructor signature.
         */
        ParameterDescriptor<Double> semiMajor = roles.get(ParameterRole.SEMI_MAJOR);
        ParameterDescriptor<Double> semiMinor = roles.get(ParameterRole.SEMI_MINOR);
        if (semiMajor == null) semiMajor = MapProjection.SEMI_MAJOR;
        if (semiMinor == null) semiMinor = MapProjection.SEMI_MINOR;

              double a  = getAndStore(parameters, semiMajor);
        final double b  = getAndStore(parameters, semiMinor);
        final double λ0 = getAndStore(parameters, roles.get(ParameterRole.CENTRAL_MERIDIAN));
        final double fe = getAndStore(parameters, roles.get(ParameterRole.FALSE_EASTING));
        final double fn = getAndStore(parameters, roles.get(ParameterRole.FALSE_NORTHING));
        final double rs = b / a;
        excentricitySquared = 1 - (rs * rs);
        excentricity = sqrt(excentricitySquared);
        if (excentricitySquared != 0) {
            final ParameterDescriptor<Double> radius = roles.get(ParameterRole.LATITUDE_OF_CONFORMAL_SPHERE_RADIUS);
            if (radius != null) {
                /*
                 * EPSG said: R is the radius of the sphere and will normally be one of the CRS parameters.
                 * If the figure of the earth used is an ellipsoid rather than a sphere then R should be calculated
                 * as the radius of the conformal sphere at the projection origin at latitude φ₀ using the formula
                 * for Rc given in section 1.2, table 3.
                 *
                 * Table 3 gives:
                 * Radius of conformal sphere Rc = a √(1 – ℯ²) / (1 – ℯ²⋅sin²φ)
                 *
                 * Using √(1 – ℯ²) = b/a we rewrite as: Rc = b / (1 – ℯ²⋅sin²φ)
                 */
                final double sinφ = sin(toRadians(parameters.doubleValue(radius)));
                a = b / (1 - excentricitySquared * (sinφ*sinφ));
            }
        }
        context.normalizeGeographicInputs(λ0);
        final DoubleDouble k = new DoubleDouble(a);
        final ParameterDescriptor<Double> scaleFactor = roles.get(ParameterRole.SCALE_FACTOR);
        if (scaleFactor != null) {
            k.multiply(getAndStore(parameters, scaleFactor));
        }
        final MatrixSIS denormalize = context.getMatrix(false);
        denormalize.convertAfter(0, k, new DoubleDouble(fe));
        denormalize.convertAfter(1, k, new DoubleDouble(fn));
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
     * Returns {@code true} if the projection specified by the given parameters has the given keyword or identifier.
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
     * @param  identifier The identifier to compare against the parameter group name.
     * @return {@code true} if the given parameter group name contains the given keyword
     *         or has an EPSG identifier equals to the given identifier.
     */
    static boolean identMatch(final ParameterDescriptorGroup parameters, final String regex, final String identifier) {
        if (identifier != null) {
            for (final Identifier id : parameters.getIdentifiers()) {
                if (Constants.EPSG.equals(id.getCodeSpace())) {
                    return identifier.equals(id.getCode());
                }
            }
        }
        return parameters.getName().getCode().matches(regex);
    }

    /**
     * Gets a parameter value identified by the given descriptor and stores it in the {@link #context}.
     * A "contextual parameter" is a parameter that apply to the normalize → {@code this} → denormalize
     * chain as a whole. It does not really apply to this {@code NormalizedProjection} instance when taken alone.
     *
     * <p>This method performs the following actions:</p>
     * <ul>
     *   <li>Convert the value to the units specified by the descriptor.</li>
     *   <li>Ensure that the value is contained in the range specified by the descriptor.</li>
     *   <li>Store the value only if different than the default value.</li>
     * </ul>
     *
     * This method shall be invoked at construction time only.
     */
    final double getAndStore(final Parameters parameters, final ParameterDescriptor<Double> descriptor) {
        if (descriptor == null) {
            return 0;   // Default value for all parameters except scale factor.
        }
        final double value = parameters.doubleValue(descriptor);    // Apply a unit conversion if needed.
        final Double defaultValue = descriptor.getDefaultValue();
        if (defaultValue == null || !defaultValue.equals(value)) {
            MapProjection.validate(descriptor, value);
            context.parameter(descriptor.getName().getCode()).setValue(value);
        }
        return value;
    }

    /**
     * Same as {@link #getAndStore(Parameters, ParameterDescriptor)}, but returns the given default value
     * if the parameter is not specified.  This method shall be used only for parameters having a default
     * value more complex than what we can represent in {@link ParameterDescriptor#getDefaultValue()}.
     */
    final double getAndStore(final Parameters parameters, final ParameterDescriptor<Double> descriptor,
            final double defaultValue)
    {
        final Double value = parameters.getValue(descriptor);   // Apply a unit conversion if needed.
        if (value == null) {
            return defaultValue;
        }
        MapProjection.validate(descriptor, value);
        context.parameter(descriptor.getName().getCode()).setValue(value);
        return value;
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
        return getParameterValues(new String[] {
            Constants.SEMI_MAJOR,
            Constants.SEMI_MINOR
        });
    }

    /**
     * Filters the parameter descriptor in order to retain only the parameters of the given names, and
     * sets the semi-major and semi-minor axis lengths. The specified parameters list should contains at
     * least the {@code "semi_major"} and {@code "semi_minor"} strings.
     *
     * <p>This filtered descriptor is used for displaying the parameter values of this non-linear kernel only,
     * not for displaying the {@linkplain #getContextualParameters() contextual parameters}. Since displaying
     * the kernel parameter values is for debugging purpose only, it is not worth to cache this descriptor.</p>
     */
    @Debug
    final ParameterValueGroup getParameterValues(final String[] nonLinearParameters) {
        ParameterDescriptorGroup descriptor = getParameterDescriptors();
        final List<GeneralParameterDescriptor> filtered = new ArrayList<>(nonLinearParameters.length);
        for (final GeneralParameterDescriptor p : descriptor.descriptors()) {
            for (final String name : nonLinearParameters) {
                if (IdentifiedObjects.isHeuristicMatchForName(p, name)) {
                    filtered.add(p);
                    break;
                }
            }
        }
        descriptor = new DefaultParameterDescriptorGroup(IdentifiedObjects.getProperties(descriptor),
                1, 1, filtered.toArray(new GeneralParameterDescriptor[filtered.size()]));
        /*
         * Parameter values for the ellipsoid semi-major and semi-minor axis lengths are 1 and <= 1
         * respectively because the denormalization (e.g. multiplication by a scale factor) will be
         * applied by an affine transform after this NormalizedProjection.
         */
        final ParameterValueGroup values = descriptor.createValue();
        for (final GeneralParameterDescriptor desc : filtered) {
            final String name = desc.getName().getCode();
            final ParameterValue<?> p = values.parameter(name);
            switch (name) {
                case Constants.SEMI_MAJOR: p.setValue(1.0); break;
                case Constants.SEMI_MINOR: p.setValue(sqrt(1 - excentricitySquared)); break;
                default: p.setValue(context.parameter(name).getValue());
            }
        }
        return values;
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
