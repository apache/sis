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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.io.Serializable;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.converter.ConversionException;

import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;

import org.apache.sis.io.wkt.Parser;
import org.apache.sis.internal.util.LazySet;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.j2d.ParameterizedAffine;
import org.apache.sis.internal.referencing.provider.AbstractProvider;
import org.apache.sis.internal.referencing.provider.VerticalOffset;
import org.apache.sis.internal.referencing.provider.Providers;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;

// Branch-specific imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Low level factory for creating {@linkplain AbstractMathTransform math transforms}.
 * The objects created by this factory do not know what the source and target coordinate systems mean.
 * Because of this low semantic value, high level GIS applications usually do not need to use this factory directly.
 * They can use the static convenience methods in the {@link org.apache.sis.referencing.CRS}
 * or {@link MathTransforms} classes instead.
 *
 *
 * <div class="section">Standard parameters</div>
 * {@code MathTransform} instances are created from {@linkplain DefaultParameterValueGroup parameter values}.
 * The parameters expected by each operation available in a default Apache SIS installation is
 * <a href="http://sis.apache.org/book/tables/CoordinateOperationMethods.html">listed here</a>.
 * The set of parameters varies for each operation or projection, but the following can be considered typical:
 *
 * <ul>
 *   <li>A <cite>semi-major</cite> and <cite>semi-minor</cite> axis length in metres.</li>
 *   <li>A <cite>central meridian</cite> and <cite>latitude of origin</cite> in decimal degrees.</li>
 *   <li>A <cite>scale factor</cite>, which default to 1.</li>
 *   <li>A <cite>false easting</cite> and <cite>false northing</cite> in metres, which default to 0.</li>
 * </ul>
 *
 * <p>Each descriptor has many aliases, and those aliases may vary between different projections.
 * For example the <cite>false easting</cite> parameter is usually called {@code "false_easting"}
 * by OGC, while EPSG uses various names like <cite>"False easting"</cite> or <cite>"Easting at
 * false origin"</cite>.</p>
 *
 * <div class="section">Dynamic parameters</div>
 * A few non-standard parameters are defined for compatibility reasons,
 * but delegates their work to standard parameters. Those dynamic parameters are not listed in the
 * {@linkplain DefaultParameterValueGroup#values() parameter values}.
 * Dynamic parameters are:
 *
 * <ul>
 *   <li>{@code "earth_radius"}, which copy its value to the {@code "semi_major"} and
 *       {@code "semi_minor"} parameter values.</li>
 *   <li>{@code "inverse_flattening"}, which compute the {@code "semi_minor"} value from
 *       the {@code "semi_major"} parameter value.</li>
 *   <li>{@code "standard_parallel"} expecting an array of type {@code double[]}, which copy
 *       its elements to the {@code "standard_parallel_1"} and {@code "standard_parallel_2"}
 *       parameter scalar values.</li>
 * </ul>
 *
 * <p>The main purpose of those dynamic parameters is to support some less commonly used conventions
 * without duplicating the most commonly used conventions. The alternative ways are used in NetCDF
 * files for example, which often use spherical models instead than ellipsoidal ones.</p>
 *
 *
 * <div class="section"><a name="Obligation">Mandatory and optional parameters</a></div>
 * Parameters are flagged as either <cite>mandatory</cite> or <cite>optional</cite>.
 * A parameter may be mandatory and still have a default value. In the context of this package, "mandatory"
 * means that the parameter is an essential part of the projection defined by standards.
 * Such mandatory parameters will always appears in any <cite>Well Known Text</cite> (WKT) formatting,
 * even if not explicitly set by the user. For example the central meridian is typically a mandatory
 * parameter with a default value of 0° (the Greenwich meridian).
 *
 * <p>Optional parameters, on the other hand, are often non-standard extensions.
 * They will appear in WKT formatting only if the user defined explicitly a value which is different than the
 * default value.</p>
 *
 *
 * <div class="section">Operation methods discovery</div>
 * {@link OperationMethod} describes all the parameters expected for instantiating a particular kind of
 * math transform. The set of operation methods known to this factory can be obtained in two ways:
 *
 * <ul>
 *   <li>{@linkplain #DefaultMathTransformFactory(Iterable) specified explicitely at construction time}, or</li>
 *   <li>{@linkplain #DefaultMathTransformFactory() discovered by scanning the classpath}.</li>
 * </ul>
 *
 * The default way is to scan the classpath. See {@link MathTransformProvider} for indications about how to add
 * custom coordinate operation methods in a default Apache SIS installation.
 *
 *
 * <div class="section">Thread safety</div>
 * This class is safe for multi-thread usage if all referenced {@code OperationMethod} instances are thread-safe.
 * There is typically only one {@code MathTransformFactory} instance for the whole application.
 *
 * @author  Martin Desruisseaux (Geomatys, IRD)
 * @since   0.6
 * @version 0.7
 * @module
 *
 * @see MathTransformProvider
 * @see AbstractMathTransform
 */
public class DefaultMathTransformFactory extends AbstractFactory implements MathTransformFactory, Parser {
    /*
     * NOTE FOR JAVADOC WRITER:
     * The "method" word is ambiguous here, because it can be "Java method" or "coordinate operation method".
     * In this class, we reserve the "method" word for "coordinate operation method" as much as possible.
     * For Java methods, we rather use "constructor" or "function".
     */

    /**
     * Minimal precision of ellipsoid semi-major and semi-minor axis lengths, in metres.
     * If the length difference between the axis of two ellipsoids is greater than this threshold,
     * we will report a mismatch. This is used for logging purpose only and do not have any impact
     * on the {@code MathTransform} objects to be created by this factory.
     */
    private static final double ELLIPSOID_PRECISION = Formulas.LINEAR_TOLERANCE;

    /**
     * The constructor for WKT parsers, fetched when first needed. The WKT parser is defined in the
     * same module than this class, so we will hopefully not have security issues.  But we have to
     * use reflection because the parser class is not yet public (because we do not want to commit
     * its API yet).
     */
    private static volatile Constructor<? extends Parser> parserConstructor;

    /**
     * All methods specified at construction time or found on the classpath.
     * If the iterable is an instance of {@link ServiceLoader}, then it will
     * be reloaded when {@link #reload()} is invoked.
     *
     * <p>All uses of this field shall be synchronized on {@code methods}.</p>
     */
    private final Iterable<? extends OperationMethod> methods;

    /**
     * The methods by name, cached for faster lookup and for avoiding some
     * synchronizations on {@link #methods} and {@link #pool}.
     */
    private final ConcurrentMap<String, OperationMethod> methodsByName;

    /**
     * The methods by type. All uses of this map shall be synchronized on {@code methodsByType}.
     *
     * <div class="note"><b>Note:</b>
     * we do not use a concurrent map here because the number of entries is expected to be very small
     * (about 2 entries), which make concurrent algorithms hardly efficient. Furthermore this map is
     * not used often.
     * </div>
     */
    private final Map<Class<?>, OperationMethodSet> methodsByType;

    /**
     * The last coordinate operation method used by a {@code create(…)} constructor.
     */
    private final ThreadLocal<OperationMethod> lastMethod;

    /**
     * The math transforms created so far. This pool is used in order
     * to return instances of existing math transforms when possible.
     */
    private final WeakHashSet<MathTransform> pool;

    /**
     * The <cite>Well Known Text</cite> parser for {@code MathTransform} instances.
     * This parser is not thread-safe, so we need to prevent two threads from using
     * the same instance in same time.
     */
    private final AtomicReference<Parser> parser;

    /**
     * Creates a new factory which will discover operation methods with a {@link ServiceLoader}.
     * The {@link OperationMethod} implementations shall be listed in the following file:
     *
     * {@preformat text
     *     META-INF/services/org.opengis.referencing.operation.OperationMethod
     * }
     *
     * {@code DefaultMathTransformFactory} parses the above-cited files in all JAR files in order to find all available
     * operation methods. By default, only operation methods that implement the {@link MathTransformProvider} interface
     * can be used by the {@code create(…)} methods in this class.
     *
     * @see #reload()
     */
    public DefaultMathTransformFactory() {
        /*
         * WORKAROUND for a JDK bug: ServiceLoader does not support usage of two Iterator instances
         * before the first iteration is finished. Steps to reproduce:
         *
         *     ServiceLoader<?> loader = ServiceLoader.load(OperationMethod.class);
         *
         *     Iterator<?> it1 = loader.iterator();
         *     assertTrue   ( it1.hasNext() );
         *     assertNotNull( it1.next())   );
         *
         *     Iterator<?> it2 = loader.iterator();
         *     assertTrue   ( it1.hasNext()) );
         *     assertTrue   ( it2.hasNext()) );
         *     assertNotNull( it1.next())    );
         *     assertNotNull( it2.next())    );     // ConcurrentModificationException here !!!
         *
         * Wrapping the ServiceLoader in a LazySet avoid this issue.
         */
        this(new Providers());
    }

    /**
     * Creates a new factory which will use the given operation methods. The given iterable is stored by reference —
     * its content is <strong>not</strong> copied — in order to allow deferred {@code OperationMethod} constructions.
     * Note that by default, only operation methods that implement the {@link MathTransformProvider} interface can be
     * used by the {@code create(…)} methods in this class.
     *
     * <p><b>Requirements:</b></p>
     * <ul>
     *   <li>The given iterable should not contain duplicated elements.</li>
     *   <li>The given iterable shall be stable: all elements returned by the first iteration must also be
     *       returned by any subsequent iterations, unless {@link #reload()} has been invoked.</li>
     *   <li>{@code OperationMethod} instances should also implement {@link MathTransformProvider}.</li>
     *   <li>All {@code OperationMethod} instances shall be thread-safe.</li>
     *   <li>The {@code Iterable} itself does not need to be thread-safe since all usages will be synchronized as below:
     *
     *       {@preformat java
     *           synchronized (methods) {
     *               for (OperationMethod method : methods) {
     *                   // Use the method here.
     *               }
     *           }
     *       }
     *   </li>
     * </ul>
     *
     * @param methods The operation methods to use, stored by reference (not copied).
     */
    public DefaultMathTransformFactory(final Iterable<? extends OperationMethod> methods) {
        ArgumentChecks.ensureNonNull("methods", methods);
        this.methods  = methods;
        methodsByName = new ConcurrentHashMap<String, OperationMethod>();
        methodsByType = new IdentityHashMap<Class<?>, OperationMethodSet>();
        lastMethod    = new ThreadLocal<OperationMethod>();
        pool          = new WeakHashSet<MathTransform>(MathTransform.class);
        parser        = new AtomicReference<Parser>();
    }

    /**
     * Returns a set of available methods for coordinate operations of the given type.
     * The {@code type} argument can be used for filtering the kind of operations described by the returned
     * {@code OperationMethod}s. The argument is usually (but not restricted to) one of the following types:
     *
     * <ul>
     *   <li>{@link org.opengis.referencing.operation.Transformation}
     *       for coordinate operations described by empirically derived parameters.</li>
     *   <li>{@link org.opengis.referencing.operation.Conversion}
     *       for coordinate operations described by definitions.</li>
     *   <li>{@link org.opengis.referencing.operation.Projection}
     *       for conversions from geodetic latitudes and longitudes to plane (map) coordinates.</li>
     *   <li>{@link SingleOperation} for all coordinate operations.</li>
     * </ul>
     *
     * The returned set may conservatively contain more {@code OperationMethod} elements than requested
     * if this {@code MathTransformFactory} does not support filtering by the given type.
     *
     * @param  type <code>{@linkplain SingleOperation}.class</code> for fetching all operation methods,
     *         <code>{@linkplain org.opengis.referencing.operation.Projection}.class</code> for fetching
     *         only map projection methods, <i>etc</i>.
     * @return Methods available in this factory for coordinate operations of the given type.
     *
     * @see #getDefaultParameters(String)
     * @see #createParameterizedTransform(ParameterValueGroup, Context)
     * @see DefaultOperationMethod#getOperationType()
     */
    @Override
    public Set<OperationMethod> getAvailableMethods(final Class<? extends SingleOperation> type) {
        ArgumentChecks.ensureNonNull("type", type);
        OperationMethodSet set;
        synchronized (methodsByType) {
            set = methodsByType.get(type);
        }
        if (set == null) {
            /*
             * Implementation note: we are better to avoid holding a lock on 'methods' and 'methodsByType'
             * in same time because the 'methods' iterator could be a user's implementation which callback
             * this factory.
             */
            synchronized (methods) {
                set = new OperationMethodSet(type, methods);
            }
            final OperationMethodSet previous;
            synchronized (methodsByType) {
                previous = JDK8.putIfAbsent(methodsByType, type, set);
            }
            if (previous != null) {
                set = previous;
            }
        }
        return set;
    }

    /**
     * Returns the operation method for the specified name or identifier. The given argument shall be either
     * a method {@linkplain DefaultOperationMethod#getName() name} (e.g. <cite>"Transverse Mercator"</cite>)
     * or one of its {@linkplain DefaultOperationMethod#getIdentifiers() identifiers} (e.g. {@code "EPSG:9807"}).
     *
     * <p>The search is case-insensitive. Comparisons against method names can be
     * {@linkplain DefaultOperationMethod#isHeuristicMatchForName(String) heuristic}.</p>
     *
     * <p>If more than one method match the given identifier, then the first (according iteration order)
     * non-{@linkplain org.apache.sis.util.Deprecable#isDeprecated() deprecated} matching method is returned.
     * If all matching methods are deprecated, the first one is returned.</p>
     *
     * @param  identifier The name or identifier of the operation method to search.
     * @return The coordinate operation method for the given name or identifier.
     * @throws NoSuchIdentifierException if there is no operation method registered for the specified identifier.
     *
     * @see org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory#getOperationMethod(String)
     */
    public OperationMethod getOperationMethod(String identifier) throws NoSuchIdentifierException {
        identifier = CharSequences.trimWhitespaces(identifier);
        ArgumentChecks.ensureNonEmpty("identifier", identifier);
        OperationMethod method = methodsByName.get(identifier);
        if (method == null) {
            final ReferencingServices services = ReferencingServices.getInstance();
            synchronized (methods) {
                method = services.getOperationMethod(methods, identifier);
            }
            if (method == null) {
                throw new NoSuchIdentifierException(Errors.format(Errors.Keys.NoSuchOperationMethod_1, identifier), identifier);
            }
            /*
             * Remember the method we just found, for faster check next time.
             */
            final OperationMethod previous = methodsByName.putIfAbsent(identifier.intern(), method);
            if (previous != null) {
                method = previous;
            }
        }
        return method;
    }

    /**
     * Returns the default parameter values for a math transform using the given operation method.
     * The {@code method} argument is the name of any {@code OperationMethod} instance returned by
     * <code>{@link #getAvailableMethods(Class) getAvailableMethods}({@linkplain SingleOperation}.class)</code>.
     * A typical example is
     * "<a href="http://www.remotesensing.org/geotiff/proj_list/transverse_mercator.html">Transverse Mercator</a>").
     *
     * <p>This function creates new parameter instances at every call.
     * Parameters are intended to be modified by the user before to be given to the
     * {@link #createParameterizedTransform createParameterizedTransform(…)} method.</p>
     *
     * @param  method The case insensitive name of the coordinate operation method to search for.
     * @return A new group of parameter values for the {@code OperationMethod} identified by the given name.
     * @throws NoSuchIdentifierException if there is no method registered for the given name or identifier.
     *
     * @see #getAvailableMethods(Class)
     * @see #createParameterizedTransform(ParameterValueGroup, Context)
     * @see AbstractMathTransform#getParameterValues()
     */
    @Override
    public ParameterValueGroup getDefaultParameters(final String method) throws NoSuchIdentifierException {
        return getOperationMethod(method).getParameters().createValue();
    }

    /**
     * Creates a transform from a group of parameters.
     * The set of expected parameters varies for each operation.
     *
     * @param  parameters The parameter values. The {@linkplain ParameterDescriptorGroup#getName() parameter group name}
     *         shall be the name of the desired {@linkplain DefaultOperationMethod operation method}.
     * @return The transform created from the given parameters.
     * @throws NoSuchIdentifierException if there is no method for the given parameter group name.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @deprecated Replaced by {@link #createParameterizedTransform(ParameterValueGroup, Context)}
     *             where the {@code Context} argument can be null.
     */
    @Override
    @Deprecated
    public MathTransform createParameterizedTransform(final ParameterValueGroup parameters)
            throws NoSuchIdentifierException, FactoryException
    {
        return createParameterizedTransform(parameters, null);
    }

    /**
     * Source and target coordinate systems for which a new parameterized transform is going to be used.
     * {@link DefaultMathTransformFactory} uses this information for:
     *
     * <ul>
     *   <li>Completing some parameters if they were not provided. In particular, the {@linkplain #getSourceEllipsoid()
     *       source ellipsoid} can be used for providing values for the {@code "semi_major"} and {@code "semi_minor"}
     *       parameters in map projections.</li>
     *   <li>{@linkplain CoordinateSystems#swapAndScaleAxes Swapping and scaling axes} if the source or the target
     *       coordinate systems are not {@linkplain AxesConvention#NORMALIZED normalized}.</li>
     * </ul>
     *
     * By default this class does <strong>not</strong> handle change of
     * {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum#getPrimeMeridian() prime meridian}
     * or anything else related to datum. Datum changes have dedicated {@link OperationMethod},
     * for example <cite>"Longitude rotation"</cite> (EPSG:9601) for changing the prime meridian.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 0.7
     * @since   0.7
     * @module
     */
    public static class Context implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 6963581151055917955L;

        /**
         * Coordinate system of the source or target points.
         */
        private CoordinateSystem sourceCS, targetCS;

        /**
         * The ellipsoid of the source or target ellipsoidal coordinate system, or {@code null} if it does not apply.
         * Valid only if {@link #sourceCS} or {@link #targetCS} is an instance of {@link EllipsoidalCS}.
         */
        private Ellipsoid sourceEllipsoid, targetEllipsoid;

        /**
         * The provider that created the parameterized {@link MathTransform} instance, or {@code null}
         * if this information does not apply. This field is used for transferring information between
         * {@code createParameterizedTransform(…)} and {@code swapAndScaleAxes(…)}.
         *
         * @todo We could make this information public as a replacement of {@link #getLastMethodUsed()}.
         */
        OperationMethod provider;

        /**
         * The parameters actually used.
         *
         * @see #getCompletedParameters()
         */
        ParameterValueGroup parameters;

        /**
         * Creates a new context with all properties initialized to {@code null}.
         */
        public Context() {
        }

        /**
         * Sets the source coordinate system to the given value.
         * The source ellipsoid is unconditionally set to {@code null}.
         *
         * @param cs The coordinate system to set as the source (can be {@code null}).
         */
        public void setSource(final CoordinateSystem cs) {
            sourceCS = cs;
            sourceEllipsoid = null;
        }

        /**
         * Sets the source coordinate system and its associated ellipsoid to the given value.
         *
         * <div class="note"><b>Design note:</b>
         * ellipsoidal coordinate systems and ellipsoids are associated indirectly, through a geodetic CRS.
         * However this method expects those two components to be given explicitely instead than inferring
         * them from a {@code CoordinateReferenceSystem} for making clear that {@code MathTransformFactory}
         * does not perform any {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum geodetic
         * datum} analysis. For coordinate operations that take datum changes in account (including change
         * of prime meridian), see {@link org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory}.
         * This policy helps to enforce a separation of concerns.</div>
         *
         * @param cs The coordinate system to set as the source, or {@code null}.
         * @param ellipsoid The ellipsoid associated to the given coordinate system, or {@code null}.
         */
        public void setSource(final EllipsoidalCS cs, final Ellipsoid ellipsoid) {
            sourceCS = cs;
            sourceEllipsoid = ellipsoid;
        }

        /**
         * Sets the target coordinate system to the given value.
         * The target ellipsoid is unconditionally set to {@code null}.
         *
         * @param cs The coordinate system to set as the target (can be {@code null}).
         */
        public void setTarget(final CoordinateSystem cs) {
            targetCS = cs;
            targetEllipsoid = null;
        }

        /**
         * Sets the target coordinate system and its associated ellipsoid to the given value.
         *
         * <div class="note"><b>Design note:</b>
         * see {@link #setSource(EllipsoidalCS, Ellipsoid)}.</div>
         *
         * @param cs The coordinate system to set as the source, or {@code null}.
         * @param ellipsoid The ellipsoid associated to the given coordinate system, or {@code null}.
         */
        public void setTarget(final EllipsoidalCS cs, final Ellipsoid ellipsoid) {
            targetCS = cs;
            targetEllipsoid = ellipsoid;
        }

        /**
         * Returns the source coordinate system, or {@code null} if unspecified.
         *
         * @return The source coordinate system, or {@code null}.
         */
        public CoordinateSystem getSourceCS() {
            return sourceCS;
        }

        /**
         * Returns the ellipsoid of the source ellipsoidal coordinate system, or {@code null} if it does not apply.
         * This information is valid only if {@link #getSourceCS()} returns an instance of {@link EllipsoidalCS}.
         *
         * @return the ellipsoid of the source ellipsoidal coordinate system, or {@code null} if it does not apply.
         */
        public Ellipsoid getSourceEllipsoid() {
            return sourceEllipsoid;
        }

        /**
         * Returns the target coordinate system, or {@code null} if unspecified.
         *
         * @return The target coordinate system, or {@code null}.
         */
        public CoordinateSystem getTargetCS() {
            return targetCS;
        }

        /**
         * Returns the ellipsoid of the target ellipsoidal coordinate system, or {@code null} if it does not apply.
         * This information is valid only if {@link #getTargetCS()} returns an instance of {@link EllipsoidalCS}.
         *
         * @return the ellipsoid of the target ellipsoidal coordinate system, or {@code null} if it does not apply.
         */
        public Ellipsoid getTargetEllipsoid() {
            return targetEllipsoid;
        }

        /**
         * Returns the matrix that represent the affine transform to concatenate before or after
         * the parameterized transform. The {@code role} argument specifies which matrix is desired:
         *
         * <ul class="verbose">
         *   <li>{@link org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole#NORMALIZATION
         *       NORMALIZATION} for the conversion from the {@linkplain #getSourceCS() source coordinate system} to
         *       a {@linkplain AxesConvention#NORMALIZED normalized} coordinate system, usually with
         *       (<var>longitude</var>, <var>latitude</var>) axis order in degrees or
         *       (<var>easting</var>, <var>northing</var>) in metres.
         *       This normalization needs to be applied <em>before</em> the parameterized transform.</li>
         *
         *   <li>{@link org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole#DENORMALIZATION
         *       DENORMALIZATION} for the conversion from a normalized coordinate system to the
         *       {@linkplain #getTargetCS() target coordinate system}, for example with
         *       (<var>latitude</var>, <var>longitude</var>) axis order.
         *       This denormalization needs to be applied <em>after</em> the parameterized transform.</li>
         *
         *   <li>{@link org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole#INVERSE_NORMALIZATION INVERSE_NORMALIZATION} and
         *       {@link org.apache.sis.referencing.operation.transform.ContextualParameters.MatrixRole#INVERSE_DENORMALIZATION INVERSE_DENORMALIZATION}
         *       are also supported but rarely used.</li>
         * </ul>
         *
         * This method is invoked by {@link DefaultMathTransformFactory#swapAndScaleAxes(MathTransform, Context)}.
         * Users an override this method if they need to customize the normalization process.
         *
         * @param  role Whether the normalization or denormalization matrix is desired.
         * @return The requested matrix, or {@code null} if this {@code Context} has no information about the coordinate system.
         * @throws FactoryException if an error occurred while computing the matrix.
         *
         * @see DefaultMathTransformFactory#createAffineTransform(Matrix)
         * @see DefaultMathTransformFactory#createParameterizedTransform(ParameterValueGroup, Context)
         */
        @SuppressWarnings("fallthrough")
        public Matrix getMatrix(final ContextualParameters.MatrixRole role) throws FactoryException {
            final CoordinateSystem specified;
            boolean inverse = false;
            switch (role) {
                default: throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "role", role));
                case INVERSE_NORMALIZATION:   inverse   = true;          // Fall through
                case NORMALIZATION:           specified = getSourceCS(); break;
                case INVERSE_DENORMALIZATION: inverse   = true;          // Fall through
                case DENORMALIZATION:         inverse   = !inverse;
                                              specified = getTargetCS(); break;
            }
            if (specified == null) {
                return null;
            }
            final CoordinateSystem normalized = CoordinateSystems.replaceAxes(specified, AxesConvention.NORMALIZED);
            try {
                if (inverse) {
                    return CoordinateSystems.swapAndScaleAxes(normalized, specified);
                } else {
                    return CoordinateSystems.swapAndScaleAxes(specified, normalized);
                }
            } catch (IllegalArgumentException cause) {
                throw new InvalidGeodeticParameterException(cause.getLocalizedMessage(), cause);
            } catch (ConversionException cause) {
                throw new InvalidGeodeticParameterException(cause.getLocalizedMessage(), cause);
            }
        }

        /**
         * Returns the parameter values used for the math transform creation, including the parameters completed
         * by the factory.
         *
         * @return The parameter values used by the factory.
         * @throws IllegalStateException if {@link DefaultMathTransformFactory#createParameterizedTransform(ParameterValueGroup, Context)}
         *         has not yet been invoked.
         */
        public ParameterValueGroup getCompletedParameters() {
            if (parameters != null) {
                return parameters;
            }
            throw new IllegalStateException(Errors.format(Errors.Keys.UnspecifiedParameterValues));
        }

        /**
         * If the parameters given by the user were not created by {@code getDefaultParameters(String)}
         * or something equivalent, copies those parameters into the structure expected by the provider.
         * The intend is to make sure that we have room for the parameters that {@code setEllipsoids(…)}
         * may write.
         *
         * <p>A side effect of this method is that the copy operation may perform a check of
         * parameter value validity. This may result in an {@link InvalidParameterNameException}
         * or {@link InvalidParameterValueException} to be thrown.</p>
         *
         * @param writable {@code true} if this method should also check that the parameters group is not
         *        an instance of {@link UnmodifiableParameterValueGroup}. Current implementation assumes
         *        that modifiable parameters are instances of {@link DefaultParameterValueGroup}.
         * @throws IllegalArgumentException if the copy can not be performed because a parameter has
         *         a unrecognized name or an illegal value.
         */
        private void ensureCompatibleParameters(final boolean writable) throws IllegalArgumentException {
            final ParameterDescriptorGroup expected = provider.getParameters();
            if (parameters.getDescriptor() != expected ||
                    (writable &&  (parameters instanceof Parameters)
                              && !(parameters instanceof DefaultParameterValueGroup)))
            {
                final ParameterValueGroup copy = expected.createValue();
                Parameters.copy(parameters, copy);
                parameters = copy;
            }
        }

        /**
         * Returns the value of the given parameter in the given unit, or {@code NaN} if the parameter is not set.
         *
         * <p><b>NOTE:</b> Do not merge this function with {@code ensureSet(…)}. We keep those two methods
         * separated in order to give to {@code createParameterizedTransform(…)} a "all or nothing" behavior.</p>
         */
        private static double getValue(final ParameterValue<?> parameter, final Unit<Length> unit) {
            return (parameter.getValue() != null) ? parameter.doubleValue(unit) : Double.NaN;
        }

        /**
         * Ensures that a value is set in the given parameter.
         *
         * <ul>
         *   <li>If the parameter has no value, then it is set to the given value.<li>
         *   <li>If the parameter already has a value, then the parameter is left unchanged
         *       but its value is compared to the given one for consistency.</li>
         * </ul>
         *
         * @param parameter The parameter which must have a value.
         * @param actual    The current parameter value, or {@code NaN} if none.
         * @param expected  The expected parameter value, derived from the ellipsoid.
         * @param unit      The unit of {@code value}.
         * @param tolerance Maximal difference (in unit of {@code unit}) for considering the two values as equivalent.
         * @return {@code true} if there is a mismatch between the actual value and the expected one.
         */
        private static boolean ensureSet(final ParameterValue<?> parameter, final double actual,
                final double expected, final Unit<?> unit, final double tolerance)
        {
            if (Math.abs(actual - expected) <= tolerance) {
                return false;
            }
            if (Double.isNaN(actual)) {
                parameter.setValue(expected, unit);
                return false;
            }
            return true;
        }

        /**
         * Completes the parameter group with information about source or target ellipsoid axis lengths,
         * if available. This method writes semi-major and semi-minor parameter values only if they do not
         * already exists in the given parameters.
         *
         * @param  ellipsoid  The ellipsoid from which to get axis lengths of flattening factor, or {@code null}.
         * @param  semiMajor  {@code "semi_major}, {@code "src_semi_major} or {@code "tgt_semi_major} parameter name.
         * @param  semiMinor  {@code "semi_minor}, {@code "src_semi_minor} or {@code "tgt_semi_minor} parameter name.
         * @param  inverseFlattening {@code true} if this method can try to set the {@code "inverse_flattening"} parameter.
         * @return The exception if the operation failed, or {@code null} if none. This exception is not thrown now
         *         because the caller may succeed in creating the transform anyway, or otherwise may produce a more
         *         informative exception.
         */
        private RuntimeException setEllipsoid(final Ellipsoid ellipsoid, final String semiMajor, final String semiMinor,
                final boolean inverseFlattening, RuntimeException failure)
        {
            /*
             * Note: we could also consider to set the "dim" parameter here based on the number of dimensions
             * of the coordinate system. But except for the Molodensky operation, this would be SIS-specific.
             * A more portable way is to concatenate a "Geographic 3D to 2D" operation after the transform if
             * we see that the dimensions do not match. It also avoid attempt to set a "dim" parameter on map
             * projections, which is not allowed.
             */
            if (ellipsoid != null) {
                ensureCompatibleParameters(true);
                ParameterValue<?> mismatchedParam = null;
                double mismatchedValue = 0;
                try {
                    final ParameterValue<?> ap = parameters.parameter(semiMajor);
                    final ParameterValue<?> bp = parameters.parameter(semiMinor);
                    final Unit<Length> unit = ellipsoid.getAxisUnit();
                    /*
                     * The two calls to getValue(…) shall succeed before we write anything, in order to have a
                     * "all or nothing" behavior as much as possible. Note that Ellipsoid.getSemi**Axis() have
                     * no reason to fail, so we do not take precaution for them.
                     */
                    final double a   = getValue(ap, unit);
                    final double b   = getValue(bp, unit);
                    final double tol = SI.METRE.getConverterTo(unit).convert(ELLIPSOID_PRECISION);
                    if (ensureSet(ap, a, ellipsoid.getSemiMajorAxis(), unit, tol)) {
                        mismatchedParam = ap;
                        mismatchedValue = a;
                    }
                    if (ensureSet(bp, b, ellipsoid.getSemiMinorAxis(), unit, tol)) {
                        mismatchedParam = bp;
                        mismatchedValue = b;
                    }
                } catch (RuntimeException e) {  // (IllegalArgumentException | IllegalStateException) on the JDK7 branch.
                    /*
                     * Parameter not found, or is not numeric, or unit of measurement is not linear.
                     * Do not touch to the parameters. We will see if createParameterizedTransform(…)
                     * can do something about that. If it can not, createParameterizedTransform(…) is
                     * the right place to throw the exception.
                     */
                    if (failure == null) {
                        failure = e;
                    } else {
                        // failure.addSuppressed(e) on the JDK7 branch.
                    }
                }
                final boolean isIvfDefinitive;
                if (mismatchedParam != null) {
                    final LogRecord record = Messages.getResources(null).getLogRecord(Level.WARNING,
                            Messages.Keys.MismatchedEllipsoidAxisLength_3, ellipsoid.getName().getCode(),
                            mismatchedParam.getDescriptor().getName().getCode(), mismatchedValue);
                    record.setLoggerName(Loggers.COORDINATE_OPERATION);
                    Logging.log(DefaultMathTransformFactory.class, "createParameterizedTransform", record);
                    isIvfDefinitive = false;
                } else {
                    isIvfDefinitive = inverseFlattening && ellipsoid.isIvfDefinitive();
                }
                /*
                 * Following is specific to Apache SIS. We use this non-standard API for allowing the
                 * NormalizedProjection class (our base class for all map projection implementations)
                 * to known that the ellipsoid definitive parameter is the inverse flattening factor
                 * instead than the semi-major axis length. It makes a small difference in the accuracy
                 * of the eccentricity parameter.
                 */
                if (isIvfDefinitive) try {
                    parameters.parameter(Constants.INVERSE_FLATTENING).setValue(ellipsoid.getInverseFlattening());
                } catch (ParameterNotFoundException e) {
                    /*
                     * Should never happen with Apache SIS implementation, but may happen if the given parameters come
                     * from another implementation. We can safely abandon our attempt to set the inverse flattening value,
                     * since it was redundant with semi-minor axis length.
                     */
                    Logging.recoverableException(Logging.getLogger(Loggers.COORDINATE_OPERATION),
                            DefaultMathTransformFactory.class, "createParameterizedTransform", e);
                }
            }
            return failure;
        }

        /**
         * Completes the parameter group with information about source and target ellipsoid axis lengths,
         * if available. This method writes semi-major and semi-minor parameter values only if they do not
         * already exists in the given parameters.
         *
         * <p>The given method and parameters are stored in the {@link #provider} and {@link #parameters}
         * fields respectively. The actual stored values may differ from the values given to this method.</p>
         *
         * @param  method Description of the transform to be created, or {@code null} if unknown.
         * @return The exception if the operation failed, or {@code null} if none. This exception is not thrown now
         *         because the caller may succeed in creating the transform anyway, or otherwise may produce a more
         *         informative exception.
         * @throws IllegalArgumentException if the operation fails because a parameter has a unrecognized name or an
         *         illegal value.
         *
         * @see #getCompletedParameters()
         */
        @SuppressWarnings("null")
        final RuntimeException completeParameters(OperationMethod method, final ParameterValueGroup userParams)
                throws IllegalArgumentException
        {
            provider   = method;
            parameters = userParams;
            /*
             * Get the operation method for the appropriate number of dimensions. For example the default Molodensky
             * operation expects two-dimensional source and target CRS. If a given CRS is three-dimensional, we need
             * a provider variant which will not concatenate a "geographic 3D to 2D" operation before the Molodensky
             * one. It is worth to perform this check only if the provider is a subclass of DefaultOperationMethod,
             * since it needs to override the 'redimension(int, int)' method.
             */
            if (method instanceof DefaultOperationMethod && method.getClass() != DefaultOperationMethod.class) {
                final Integer sourceDim = (sourceCS != null) ? sourceCS.getDimension() : method.getSourceDimensions();
                final Integer targetDim = (targetCS != null) ? targetCS.getDimension() : method.getTargetDimensions();
                if (sourceDim != null && targetDim != null) {
                    method = ((DefaultOperationMethod) method).redimension(sourceDim, targetDim);
                    if (method instanceof MathTransformProvider) {
                        provider = method;
                    }
                }
            }
            ensureCompatibleParameters(false);      // Invoke only after we set 'provider' to its final instance.
            /*
             * Get a mask telling us if we need to set parameters for the source and/or target ellipsoid.
             * This information should preferably be given by the provider. But if the given provider is
             * not a SIS implementation, use as a fallback whether ellipsoids are provided. This fallback
             * may be less reliable.
             */
            int n;
            if (provider instanceof AbstractProvider) {
                n = ((AbstractProvider) provider).getEllipsoidsMask();
            } else {
                n = 0;
                if (sourceEllipsoid != null) n  = 1;
                if (targetEllipsoid != null) n |= 2;
            }
            /*
             * Set the ellipsoid axis-length parameter values. Those parameters may appear in the source
             * ellipsoid, in the target ellipsoid or in both ellipsoids.
             */
            switch (n) {
                case 0: return null;
                case 1: return setEllipsoid(getSourceEllipsoid(), Constants.SEMI_MAJOR, Constants.SEMI_MINOR, true, null);
                case 2: return setEllipsoid(getTargetEllipsoid(), Constants.SEMI_MAJOR, Constants.SEMI_MINOR, true, null);
                case 3: {
                    RuntimeException failure = null;
                    if (sourceCS != null) try {
                        ensureCompatibleParameters(true);
                        final ParameterValue<?> p = parameters.parameter("dim");    // Really 'parameters', not 'userParams'.
                        if (p.getValue() == null) {
                            p.setValue(sourceCS.getDimension());
                        }
                    } catch (RuntimeException e) {  // (IllegalArgumentException | IllegalStateException e) on the JDK7 branch.
                        failure = e;
                    }
                    failure = setEllipsoid(getSourceEllipsoid(), "src_semi_major", "src_semi_minor", false, failure);
                    failure = setEllipsoid(getTargetEllipsoid(), "tgt_semi_major", "tgt_semi_minor", false, failure);
                    return failure;
                }
                default: throw new AssertionError(n);
            }
        }
    }

    /**
     * Creates a transform from a group of parameters.
     * The set of expected parameters varies for each operation.
     * The easiest way to provide parameter values is to get an initially empty group for the desired
     * operation by calling {@link #getDefaultParameters(String)}, then to fill the parameter values.
     * Example:
     *
     * {@preformat java
     *     ParameterValueGroup group = factory.getDefaultParameters("Transverse_Mercator");
     *     group.parameter("semi_major").setValue(6378137.000);
     *     group.parameter("semi_minor").setValue(6356752.314);
     *     MathTransform mt = factory.createParameterizedTransform(group, null);
     * }
     *
     * Sometime the {@code "semi_major"} and {@code "semi_minor"} parameter values are not explicitly provided,
     * but rather inferred from the {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum geodetic
     * datum} of the source Coordinate Reference System. If the given {@code context} argument is non-null,
     * then this method will use those contextual information for:
     *
     * <ol>
     *   <li>Inferring the {@code "semi_major"}, {@code "semi_minor"}, {@code "src_semi_major"},
     *       {@code "src_semi_minor"}, {@code "tgt_semi_major"} or {@code "tgt_semi_minor"} parameters values
     *       from the {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid ellipsoids} associated to
     *       the source or target CRS, if those parameters are not explicitly given and if they are relevant
     *       for the coordinate operation method.</li>
     *   <li>{@linkplain #createConcatenatedTransform Concatenating} the parameterized transform
     *       with any other transforms required for performing units changes and ordinates swapping.</li>
     * </ol>
     *
     * The complete group of parameters, including {@code "semi_major"}, {@code "semi_minor"} or other calculated values,
     * can be obtained by a call to {@link Context#getCompletedParameters()} after {@code createParameterizedTransform(…)}
     * returned. Note that the completed parameters may only have additional parameters compared to the given parameter
     * group; existing parameter values should not be modified.
     *
     * <p>The {@code OperationMethod} instance used by this constructor can be obtained by a call to
     * {@link #getLastMethodUsed()}.</p>
     *
     * @param  parameters The parameter values. The {@linkplain ParameterDescriptorGroup#getName() parameter group name}
     *         shall be the name of the desired {@linkplain DefaultOperationMethod operation method}.
     * @param  context Information about the context (for example source and target coordinate systems)
     *         in which the new transform is going to be used, or {@code null} if none.
     * @return The transform created from the given parameters.
     * @throws NoSuchIdentifierException if there is no method for the given parameter group name.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @see #getDefaultParameters(String)
     * @see #getAvailableMethods(Class)
     * @see #getLastMethodUsed()
     * @see org.apache.sis.parameter.ParameterBuilder#createGroupForMapProjection(ParameterDescriptor...)
     */
    public MathTransform createParameterizedTransform(ParameterValueGroup parameters,
            final Context context) throws NoSuchIdentifierException, FactoryException
    {
        OperationMethod  method  = null;
        RuntimeException failure = null;
        MathTransform transform;
        try {
            ArgumentChecks.ensureNonNull("parameters", parameters);
            final ParameterDescriptorGroup descriptor = parameters.getDescriptor();
            final String methodName = descriptor.getName().getCode();
            String methodIdentifier = IdentifiedObjects.toString(IdentifiedObjects.getIdentifier(descriptor, Citations.EPSG));
            if (methodIdentifier == null) {
                methodIdentifier = methodName;
            }
            /*
             * Get the MathTransformProvider of the same name or identifier than the given parameter group.
             * We give precedence to EPSG identifier because operation method names are sometime ambiguous
             * (e.g. "Lambert Azimuthal Equal Area (Spherical)"). If we fail to find the method by its EPSG code,
             * we will try searching by method name. As a side effect, this second attempt will produce a better
             * error message if the method is really not found.
             */
            try {
                method = getOperationMethod(methodIdentifier);
            } catch (NoSuchIdentifierException exception) {
                if (methodIdentifier.equals(methodName)) {
                    throw exception;
                }
                method = getOperationMethod(methodName);
                Logging.recoverableException(Logging.getLogger(Loggers.COORDINATE_OPERATION),
                        DefaultMathTransformFactory.class, "createParameterizedTransform", exception);
            } catch (IllegalStateException e) {
                failure = e;
            }
            if (!(method instanceof MathTransformProvider)) {
                throw new NoSuchIdentifierException(Errors.format(          // For now, handle like an unknown operation.
                        Errors.Keys.UnsupportedImplementation_1, Classes.getClass(method)), methodName);
            }
            /*
             * Will catch only exceptions that may be the result of improper parameter usage (e.g. a value out
             * of range). Do not catch exceptions caused by programming errors (e.g. null pointer exception).
             */
            try {
                /*
                 * If the user's parameters do not contain semi-major and semi-minor axis lengths, infer
                 * them from the ellipsoid. We have to do that because those parameters are often omitted,
                 * since the standard place where to provide this information is in the ellipsoid object.
                 */
                if (context != null) {
                    failure = context.completeParameters(method, parameters);
                    parameters = context.parameters;
                    method     = context.provider;
                }
                transform = ((MathTransformProvider) method).createMathTransform(this, parameters);
            } catch (RuntimeException exception) {  // (IllegalArgumentException | IllegalStateException) on the JDK7 branch.
                throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
            }
            /*
             * Cache the transform that we just created and make sure that the number of dimensions
             * is compatible with the OperationMethod instance. Then make final adjustment for axis
             * directions and units of measurement.
             */
            transform = unique(transform);
            method = DefaultOperationMethod.redimension(method, transform.getSourceDimensions(),
                                                                transform.getTargetDimensions());
            if (context != null) {
                transform = swapAndScaleAxes(transform, context);
            }
        } catch (FactoryException e) {
            if (failure != null) {
                // e.addSuppressed(failure) on the JDK7 branch.
            }
            throw e;
        } finally {
            lastMethod.set(method);     // May be null in case of failure, which is intended.
            if (context != null) {
                context.provider = null;
                // For now we conservatively reset the provider information to null. But if we choose to make
                // that information public in a future SIS version, then we would remove this code.
            }
        }
        return transform;
    }

    /**
     * Given a transform between normalized spaces,
     * creates a transform taking in account axis directions, units of measurement and longitude rotation.
     * This method {@linkplain #createConcatenatedTransform concatenates} the given parameterized transform
     * with any other transform required for performing units changes and ordinates swapping.
     *
     * <p>The given {@code parameterized} transform shall expect
     * {@linkplain org.apache.sis.referencing.cs.AxesConvention#NORMALIZED normalized} input coordinates and
     * produce normalized output coordinates. See {@link org.apache.sis.referencing.cs.AxesConvention} for more
     * information about what Apache SIS means by "normalized".</p>
     *
     * <div class="note"><b>Example:</b>
     * The most typical examples of transforms with normalized inputs/outputs are normalized
     * map projections expecting (<cite>longitude</cite>, <cite>latitude</cite>) inputs in degrees
     * and calculating (<cite>x</cite>, <cite>y</cite>) coordinates in metres,
     * both of them with ({@linkplain org.opengis.referencing.cs.AxisDirection#EAST East},
     * {@linkplain org.opengis.referencing.cs.AxisDirection#NORTH North}) axis orientations.</div>
     *
     * <div class="section">Controlling the normalization process</div>
     * Users who need a different normalized space than the default one way find more convenient to
     * override the {@link Context#getMatrix Context.getMatrix(ContextualParameters.MatrixRole)} method.
     *
     * @param  parameterized A transform for normalized input and output coordinates.
     * @param  context Source and target coordinate systems in which the transform is going to be used.
     * @return A transform taking in account unit conversions and axis swapping.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.cs.AxesConvention#NORMALIZED
     * @see org.apache.sis.referencing.operation.DefaultConversion#DefaultConversion(Map, OperationMethod, MathTransform, ParameterValueGroup)
     *
     * @since 0.7
     */
    public MathTransform swapAndScaleAxes(final MathTransform parameterized, final Context context) throws FactoryException {
        ArgumentChecks.ensureNonNull("parameterized", parameterized);
        ArgumentChecks.ensureNonNull("context", context);
        /*
         * Computes matrix for swapping axis and performing units conversion.
         * There is one matrix to apply before projection on (longitude,latitude)
         * coordinates, and one matrix to apply after projection on (easting,northing)
         * coordinates.
         */
        final Matrix swap1 = context.getMatrix(ContextualParameters.MatrixRole.NORMALIZATION);
        final Matrix swap3 = context.getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        /*
         * Prepares the concatenation of the matrices computed above and the projection.
         * Note that at this stage, the dimensions between each step may not be compatible.
         * For example the projection (step2) is usually two-dimensional while the source
         * coordinate system (step1) may be three-dimensional if it has a height.
         */
        MathTransform step1 = (swap1 != null) ? createAffineTransform(swap1) : MathTransforms.identity(parameterized.getSourceDimensions());
        MathTransform step3 = (swap3 != null) ? createAffineTransform(swap3) : MathTransforms.identity(parameterized.getTargetDimensions());
        MathTransform step2 = parameterized;
        /*
         * Special case for the way EPSG handles reversal of axis direction. For now the "Vertical Offset" (EPSG:9616)
         * method is the only one for which we found a need for special case. But if more special cases are added in a
         * future SIS version, then we should replace the static method by a non-static one defined in AbstractProvider.
         */
        if (context.provider instanceof VerticalOffset) {
            step2 = VerticalOffset.postCreate(step2, swap3);
        }
        /*
         * If the target coordinate system has a height, instructs the projection to pass
         * the height unchanged from the base CRS to the target CRS. After this block, the
         * dimensions of 'step2' and 'step3' should match.
         */
        final int numTrailingOrdinates = step3.getSourceDimensions() - step2.getTargetDimensions();
        if (numTrailingOrdinates > 0) {
            step2 = createPassThroughTransform(0, step2, numTrailingOrdinates);
        }
        /*
         * If the source CS has a height but the target CS doesn't, drops the extra coordinates.
         * After this block, the dimensions of 'step1' and 'step2' should match.
         */
        final int sourceDim = step1.getTargetDimensions();
        final int targetDim = step2.getSourceDimensions();
        if (sourceDim > targetDim) {
            final Matrix drop = Matrices.createDiagonal(targetDim+1, sourceDim+1);
            drop.setElement(targetDim, sourceDim, 1); // Element in the lower-right corner.
            step1 = createConcatenatedTransform(createAffineTransform(drop), step1);
        }
        MathTransform mt = createConcatenatedTransform(createConcatenatedTransform(step1, step2), step3);
        /*
         * At this point we finished to create the transform.  But before to return it, verify if the
         * parameterized transform given in argument had some custom parameters. This happen with the
         * Equirectangular projection, which can be simplified as an AffineTransform while we want to
         * continue to describe it with the "semi_major", "semi_minor", etc. parameters  instead than
         * "elt_0_0", "elt_0_1", etc.  The following code just forwards those parameters to the newly
         * created transform; it does not change the operation.
         */
        if (parameterized instanceof ParameterizedAffine && !(mt instanceof ParameterizedAffine)) {
            mt = ((ParameterizedAffine) parameterized).newTransform(mt);
        }
        return mt;
    }

    /**
     * Creates a transform from a base CRS to a derived CS using the given parameters.
     * If this method needs to set the values of {@code "semi_major"} and {@code "semi_minor"} parameters,
     * then it sets those values directly on the given {@code parameters} instance – not on a clone – for
     * allowing the caller to get back the complete parameter values.
     * However this method only fills missing values, it never modify existing values.
     *
     * @param  baseCRS    The source coordinate reference system.
     * @param  parameters The parameter values for the transform.
     * @param  derivedCS  The target coordinate system.
     * @return The parameterized transform from {@code baseCRS} to {@code derivedCS},
     *         including unit conversions and axis swapping.
     * @throws NoSuchIdentifierException if there is no transform registered for the coordinate operation method.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @deprecated Replaced by {@link #createParameterizedTransform(ParameterValueGroup, Context)}.
     */
    @Override
    @Deprecated
    public MathTransform createBaseToDerived(final CoordinateReferenceSystem baseCRS,
            final ParameterValueGroup parameters, final CoordinateSystem derivedCS)
            throws NoSuchIdentifierException, FactoryException
    {
        ArgumentChecks.ensureNonNull("baseCRS",    baseCRS);
        ArgumentChecks.ensureNonNull("parameters", parameters);
        ArgumentChecks.ensureNonNull("derivedCS",  derivedCS);
        final Context context = ReferencingUtilities.createTransformContext(baseCRS, null, null);
        context.setTarget(derivedCS);
        return createParameterizedTransform(parameters, context);
    }

    /**
     * Creates a transform from a base to a derived CS using an existing parameterized transform.
     * The given {@code parameterized} transform shall expect
     * {@linkplain org.apache.sis.referencing.cs.AxesConvention#NORMALIZED normalized} input coordinates and
     * produce normalized output coordinates.
     *
     * @param  baseCS        The source coordinate system.
     * @param  parameterized A <cite>base to derived</cite> transform for normalized input and output coordinates.
     * @param  derivedCS     The target coordinate system.
     * @return The transform from {@code baseCS} to {@code derivedCS}, including unit conversions and axis swapping.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @deprecated Replaced by {@link #swapAndScaleAxes(MathTransform, Context)}.
     */
    @Deprecated
    public MathTransform createBaseToDerived(final CoordinateSystem baseCS,
            final MathTransform parameterized, final CoordinateSystem derivedCS)
            throws FactoryException
    {
        ArgumentChecks.ensureNonNull("baseCS",        baseCS);
        ArgumentChecks.ensureNonNull("parameterized", parameterized);
        ArgumentChecks.ensureNonNull("derivedCS",     derivedCS);
        final Context context = new Context();
        context.setSource(baseCS);
        context.setTarget(derivedCS);
        return swapAndScaleAxes(parameterized, context);
    }

    /**
     * Creates a math transform that represent a change of coordinate system.
     *
     * @param source the source coordinate system.
     * @param target the target coordinate system.
     * @return a conversion from the given source to the given target coordinate system.
     * @throws FactoryException if the conversion can not be created.
     *
     * @since 0.7
     */
    public MathTransform createCoordinateSystemChange(final CoordinateSystem source, final CoordinateSystem target)
            throws FactoryException
    {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        return CoordinateSystemTransform.create(this, source, target);
        // No need to use unique(…) here.
    }

    /**
     * Creates an affine transform from a matrix. If the transform input dimension is {@code M},
     * and output dimension is {@code N}, then the matrix will have size {@code [N+1][M+1]}. The
     * +1 in the matrix dimensions allows the matrix to do a shift, as well as a rotation. The
     * {@code [M][j]} element of the matrix will be the j'th ordinate of the moved origin. The
     * {@code [i][N]} element of the matrix will be 0 for <var>i</var> less than {@code M}, and 1
     * for <var>i</var> equals {@code M}.
     *
     * @param  matrix The matrix used to define the affine transform.
     * @return The affine transform.
     * @throws FactoryException if the object creation failed.
     *
     * @see MathTransforms#linear(Matrix)
     */
    @Override
    public MathTransform createAffineTransform(final Matrix matrix) throws FactoryException {
        /*
         * Performance note: we could set lastMethod to the "Affine" operation method provider, but we do not
         * because setting this value is not free (e.g. it depends on matrix size) and it is rarely needed.
         */
        lastMethod.remove();
        return unique(MathTransforms.linear(matrix));
    }

    /**
     * Creates a transform by concatenating two existing transforms.
     * A concatenated transform acts in the same way as applying two transforms, one after the other.
     *
     * <p>The dimension of the output space of the first transform must match the dimension of the input space
     * in the second transform. In order to concatenate more than two transforms, use this constructor repeatedly.</p>
     *
     * @param  tr1 The first transform to apply to points.
     * @param  tr2 The second transform to apply to points.
     * @return The concatenated transform.
     * @throws FactoryException if the object creation failed.
     *
     * @see MathTransforms#concatenate(MathTransform, MathTransform)
     */
    @Override
    public MathTransform createConcatenatedTransform(final MathTransform tr1,
                                                     final MathTransform tr2)
            throws FactoryException
    {
        lastMethod.remove();
        ArgumentChecks.ensureNonNull("tr1", tr1);
        ArgumentChecks.ensureNonNull("tr2", tr2);
        final MathTransform tr;
        try {
            tr = ConcatenatedTransform.create(tr1, tr2, this);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
        }
        assert MathTransforms.isValid(MathTransforms.getSteps(tr)) : tr;
        return unique(tr);
    }

    /**
     * Creates a transform which passes through a subset of ordinates to another transform.
     * This allows transforms to operate on a subset of ordinates.
     *
     * <div class="note"><b>Example:</b>
     * Giving (<var>latitude</var>, <var>longitude</var>, <var>height</var>) coordinates,
     * a pass through transform can convert the height values from meters to feet without
     * affecting the (<var>latitude</var>, <var>longitude</var>) values.</div>
     *
     * The resulting transform will have the following dimensions:
     *
     * {@preformat java
     *     Source: firstAffectedOrdinate + subTransform.getSourceDimensions() + numTrailingOrdinates
     *     Target: firstAffectedOrdinate + subTransform.getTargetDimensions() + numTrailingOrdinates
     * }
     *
     * @param  firstAffectedOrdinate The lowest index of the affected ordinates.
     * @param  subTransform Transform to use for affected ordinates.
     * @param  numTrailingOrdinates  Number of trailing ordinates to pass through. Affected ordinates will range
     *         from {@code firstAffectedOrdinate} inclusive to {@code dimTarget-numTrailingOrdinates} exclusive.
     * @return A pass through transform.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public MathTransform createPassThroughTransform(final int firstAffectedOrdinate,
                                                    final MathTransform subTransform,
                                                    final int numTrailingOrdinates)
            throws FactoryException
    {
        lastMethod.remove();
        final MathTransform tr;
        try {
            tr = PassThroughTransform.create(firstAffectedOrdinate, subTransform, numTrailingOrdinates);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
        }
        return unique(tr);
    }

    /**
     * There is no XML format for math transforms.
     *
     * @param  xml Math transform encoded in XML format.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    @Deprecated
    public MathTransform createFromXML(String xml) throws FactoryException {
        lastMethod.remove();
        throw new FactoryException(Errors.format(Errors.Keys.UnsupportedOperation_1, "createFromXML"));
    }

    /**
     * Creates a math transform object from a
     * <a href="http://www.geoapi.org/snapshot/javadoc/org/opengis/referencing/doc-files/WKT.html"><cite>Well
     * Known Text</cite> (WKT)</a>.
     *
     * @param  text Math transform encoded in Well-Known Text format.
     * @return The math transform (never {@code null}).
     * @throws FactoryException if the Well-Known Text can not be parsed,
     *         or if the math transform creation failed from some other reason.
     */
    @Override
    public MathTransform createFromWKT(final String text) throws FactoryException {
        lastMethod.remove();
        Parser p = parser.getAndSet(null);
        if (p == null) try {
            Constructor<? extends Parser> c = parserConstructor;
            if (c == null) {
                c = Class.forName("org.apache.sis.io.wkt.MathTransformParser").asSubclass(Parser.class)
                         .getConstructor(MathTransformFactory.class);
                final Constructor<?> cp = c;     // For allowing use in inner class or lambda expression.
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override public Void run() {
                        cp.setAccessible(true);
                        return null;
                    }
                });
                parserConstructor = c;
            }
            p = c.newInstance(this);
        } catch (Exception e) {                     // (ReflectiveOperationException) on JDK7 branch.
            throw new FactoryException(e);
        }
        /*
         * No need to check the type of the parsed object, because MathTransformParser
         * should return only instance of MathTransform.
         */
        final Object object;
        try {
            object = p.createFromWKT(text);
        } catch (FactoryException e) {
            /*
             * The parsing may fail because a operation parameter is not known to SIS. If this happen, replace
             * the generic exception thrown be the parser (which is FactoryException) by a more specific one.
             * Note that InvalidGeodeticParameterException is defined only in this sis-referencing module,
             * so we could not throw it from the sis-metadata module that contain the parser.
             */
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof ParameterNotFoundException) {
                    throw new InvalidGeodeticParameterException(e.getMessage(), cause);     // More accurate exception.
                }
                cause = cause.getCause();
            }
            throw e;
        }
        parser.set(p);
        return (MathTransform) object;
    }

    /**
     * Replaces the given transform by a unique instance, if one already exists.
     */
    private MathTransform unique(final MathTransform tr) {
        return pool.unique(tr);
    }

    /**
     * Returns the operation method used by the latest call to a {@code create(…)} constructor
     * in the currently running thread. Returns {@code null} if not applicable.
     *
     * <p>Invoking {@code getLastMethodUsed()} can be useful after a call to
     * {@link #createParameterizedTransform createParameterizedTransform(…)}.</p>
     *
     * @return The last method used by a {@code create(…)} constructor, or {@code null} if unknown of unsupported.
     *
     * @see #createParameterizedTransform(ParameterValueGroup, Context)
     */
    @Override
    public OperationMethod getLastMethodUsed() {
        return lastMethod.get();
    }

    /**
     * Notifies this factory that the elements provided by the {@code Iterable<OperationMethod>} may have changed.
     * This method performs the following steps:
     *
     * <ul>
     *   <li>Clears all caches.</li>
     *   <li>If the {@code Iterable} given at construction time is an instance of {@link ServiceLoader},
     *       invokes its {@code reload()} method.</li>
     * </ul>
     *
     * This method is useful to sophisticated applications which dynamically make new plug-ins available at runtime,
     * for example following changes of the application classpath.
     *
     * @see #DefaultMathTransformFactory(Iterable)
     * @see ServiceLoader#reload()
     */
    public void reload() {
        synchronized (methods) {
            methodsByName.clear();
            final Iterable<? extends OperationMethod> m = methods;
            if (m instanceof LazySet<?>) { // Workaround for JDK bug. See DefaultMathTransformFactory() constructor.
                ((LazySet<? extends OperationMethod>) m).reload();
            }
            if (m instanceof ServiceLoader<?>) {
                ((ServiceLoader<?>) m).reload();
            }
            synchronized (methodsByType) {
                for (final OperationMethodSet c : methodsByType.values()) {
                    c.reset();
                }
            }
            pool.clear();
        }
    }
}
