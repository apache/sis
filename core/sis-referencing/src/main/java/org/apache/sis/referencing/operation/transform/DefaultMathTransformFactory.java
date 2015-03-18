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
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.converter.ConversionException;

import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;

import org.apache.sis.internal.util.LazySet;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.j2d.ParameterizedAffine;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;


/**
 * Low level factory for creating {@linkplain AbstractMathTransform math transforms}.
 * The objects created by this factory do not know what the source and target coordinate systems mean.
 * Because of this low semantic value, high level GIS applications usually do not need to use this factory directly.
 * They can use the static convenience methods in the {@link org.apache.sis.referencing.CRS}
 * or {@link MathTransforms} classes instead.
 *
 *
 * {@section Standard parameters}
 * {@code MathTransform} instances are created from {@linkplain org.apache.sis.parameter.DefaultParameterValueGroup
 * parameter values}. The parameters expected by each operation available in a default Apache SIS installation is
 * <a href="http://sis.apache.org/CoordinateOperationMethods.html">listed here</a>.
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
 * by OGC, while EPSG uses various names like "<cite>False easting</cite>" or "<cite>Easting at
 * false origin</cite>".</p>
 *
 * {@section Dynamic parameters}
 * A few non-standard parameters are defined for compatibility reasons,
 * but delegates their work to standard parameters. Those dynamic parameters are not listed in the
 * {@linkplain org.apache.sis.parameter.DefaultParameterValueGroup#values() parameter values}.
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
 * <a name="Obligation">{@section Mandatory and optional parameters}</a>
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
 * {@section Operation methods discovery}
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
 * {@section Thread safety}
 * This class is safe for multi-thread usage if all referenced {@code OperationMethod} instances are thread-safe.
 * There is typically only one {@code MathTransformFactory} instance for the whole application.
 *
 * @author  Martin Desruisseaux (Geomatys, IRD)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see MathTransformProvider
 * @see AbstractMathTransform
 */
public class DefaultMathTransformFactory extends AbstractFactory implements MathTransformFactory {
    /*
     * NOTE FOR JAVADOC WRITER:
     * The "method" word is ambiguous here, because it can be "Java method" or "coordinate operation method".
     * In this class, we reserve the "method" word for "coordinate operation method" as much as possible.
     * For Java methods, we rather use "constructor" or "function".
     */

    /**
     * The separator character between an identifier and its namespace in the argument given to
     * {@link #getOperationMethod(String)}. For example this is the separator in {@code "EPSG:9807"}.
     *
     * This is defined as a constant for now, but we may make it configurable in a future version.
     */
    private static final char IDENTIFIER_SEPARATOR = DefaultNameSpace.DEFAULT_SEPARATOR;

    /**
     * Minimal precision of ellipsoid semi-major and semi-minor axis lengths, in metres.
     * If the length difference between the axis of two ellipsoids is greater than this threshold,
     * we will report a mismatch. This is used for logging purpose only and do not have any impact
     * on the {@code MathTransform} objects to be created by this factory.
     */
    private static final double ELLIPSOID_PRECISION = Formulas.LINEAR_TOLERANCE;

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
        this(new LazySet<>(ServiceLoader.load(OperationMethod.class).iterator()));
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
        methodsByName = new ConcurrentHashMap<>();
        methodsByType = new IdentityHashMap<>();
        lastMethod    = new ThreadLocal<>();
        pool          = new WeakHashSet<>(MathTransform.class);
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
     * @see #createParameterizedTransform(ParameterValueGroup)
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
                previous = methodsByType.putIfAbsent(type, set);
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
     * non-{@linkplain Deprecable#isDeprecated() deprecated} matching method is returned. If all matching
     * methods are deprecated, the first one is returned.</p>
     *
     * @param  identifier The name or identifier of the operation method to search.
     * @return The coordinate operation method for the given name or identifier.
     * @throws NoSuchIdentifierException if there is no operation method registered for the specified identifier.
     */
    public OperationMethod getOperationMethod(String identifier) throws NoSuchIdentifierException {
        identifier = CharSequences.trimWhitespaces(identifier);
        ArgumentChecks.ensureNonEmpty("identifier", identifier);
        OperationMethod method = methodsByName.get(identifier);
        if (method == null) {
            for (final OperationMethod m : methods) {
                if (matches(m, identifier)) {
                    /*
                     * Stop the iteration at the first non-deprecated method.
                     * If we find only deprecated methods, take the first one.
                     */
                    final boolean isDeprecated = (m instanceof Deprecable) && ((Deprecable) m).isDeprecated();
                    if (!isDeprecated || method == null) {
                        method = m;
                        if (!isDeprecated) {
                            break;
                        }
                    }
                }
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
     * Returns {@code true} if the name or an identifier of the given method matches the given {@code identifier}.
     *
     * This function is private and static for now, but we may consider to make it a protected member
     * in a future SIS version in order to give users a chance to override the matching criterion.
     * We don't do that yet because we would like to have at least one use case before doing so.
     *
     * @param  method     The method to test for a match.
     * @param  identifier The name or identifier of the operation method to search.
     * @return {@code true} if the given method is a match for the given identifier.
     */
    private static boolean matches(final OperationMethod method, final String identifier) {
        if (IdentifiedObjects.isHeuristicMatchForName(method, identifier)) {
            return true;
        }
        for (int s = identifier.indexOf(IDENTIFIER_SEPARATOR); s >= 0;
                 s = identifier.indexOf(IDENTIFIER_SEPARATOR, s))
        {
            final String codespace = identifier.substring(0, s).trim();
            final String code = identifier.substring(++s).trim();
            for (final Identifier id : method.getIdentifiers()) {
                if (codespace.equalsIgnoreCase(id.getCodeSpace()) && code.equalsIgnoreCase(id.getCode())) {
                    return true;
                }
            }
        }
        return false;
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
     * {@link #createParameterizedTransform createParameterizedTransform(…)} constructor.</p>
     *
     * @param  method The case insensitive name of the coordinate operation method to search for.
     * @return A new group of parameter values for the {@code OperationMethod} identified by the given name.
     * @throws NoSuchIdentifierException if there is no method registered for the given name or identifier.
     *
     * @see #getAvailableMethods(Class)
     * @see #createParameterizedTransform(ParameterValueGroup)
     * @see org.apache.sis.referencing.operation.transform.AbstractMathTransform#getParameterValues()
     */
    @Override
    public ParameterValueGroup getDefaultParameters(final String method) throws NoSuchIdentifierException {
        return getOperationMethod(method).getParameters().createValue();
    }

    /**
     * Returns the value of the given parameter in the given unit, or {@code NaN} if the parameter is not set.
     *
     * <p><b>NOTE:</b> Do not merge this function with {@code ensureSet(…)}. We keep those two methods
     * separated in order to give to {@code createBaseToDerived(…)} a "all or nothing" behavior.</p>
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
     * Creates a transform from a base CRS to a derived CS using the given parameters.
     * This convenience constructor:
     *
     * <ol>
     *   <li>Infers the {@code "semi_major"} and {@code "semi_minor"} parameters values from the
     *       {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid ellipsoid} associated
     *       to the {@code baseCRS}, if those parameters are not explicitly given and if they are
     *       applicable (typically for cartographic projections).</li>
     *   <li>{@linkplain #createConcatenatedTransform Concatenates} the
     *       {@linkplain #createParameterizedTransform(ParameterValueGroup) parameterized transform}
     *       with any other transform required for performing units changes and ordinates swapping.</li>
     * </ol>
     *
     * The {@code OperationMethod} instance used by this constructor can be obtained by a call to
     * {@link #getLastMethodUsed()}.
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
     * @see org.apache.sis.parameter.ParameterBuilder#createGroupForMapProjection(ParameterDescriptor...)
     */
    @Override
    public MathTransform createBaseToDerived(final CoordinateReferenceSystem baseCRS,
            final ParameterValueGroup parameters, final CoordinateSystem derivedCS)
            throws NoSuchIdentifierException, FactoryException
    {
        /*
         * If the user's parameters do not contain semi-major and semi-minor axis lengths, infer
         * them from the ellipsoid. We have to do that because those parameters are often omitted,
         * since the standard place where to provide this information is in the ellipsoid object.
         */
        RuntimeException failure = null;
        final Ellipsoid ellipsoid = ReferencingUtilities.getEllipsoidOfGeographicCRS(baseCRS);
        if (ellipsoid != null) {
            ParameterValue<?> mismatchedParam = null;
            double mismatchedValue = 0;
            try {
                final ParameterValue<?> semiMajor = parameters.parameter(Constants.SEMI_MAJOR);
                final ParameterValue<?> semiMinor = parameters.parameter(Constants.SEMI_MINOR);
                final Unit<Length>      axisUnit  = ellipsoid.getAxisUnit();
                /*
                 * The two calls to getOptional(…) shall succeed before we write anything, in order to have a
                 * "all or nothing" behavior as much as possible. Note that Ellipsoid.getSemi**Axis() have no
                 * reason to fail, so we don't take precaution for them.
                 */
                final double a   = getValue(semiMajor, axisUnit);
                final double b   = getValue(semiMinor, axisUnit);
                final double tol = SI.METRE.getConverterTo(axisUnit).convert(ELLIPSOID_PRECISION);
                if (ensureSet(semiMajor, a, ellipsoid.getSemiMajorAxis(), axisUnit, tol)) {
                    mismatchedParam = semiMajor;
                    mismatchedValue = a;
                }
                if (ensureSet(semiMinor, b, ellipsoid.getSemiMinorAxis(), axisUnit, tol)) {
                    mismatchedParam = semiMinor;
                    mismatchedValue = b;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                /*
                 * Parameter not found, or is not numeric, or unit of measurement is not linear.
                 * Those exceptions should never occur with map projections, but may happen for
                 * some other operations like Molodenski¹.
                 *
                 * Do not touch to the parameters. We will see if createParameterizedTransform(…)
                 * can do something about that. If it can not, createParameterizedTransform(…) is
                 * the right place to throw the exception.
                 *
                 *  ¹ The actual Molodenski parameter names are "src_semi_major" and "src_semi_minor".
                 *    But we do not try to set them because we have no way to set the corresponding
                 *    "tgt_semi_major" and "tgt_semi_minor" parameters anyway.
                 */
                failure = e;
            }
            if (mismatchedParam != null) {
                Logging.log(DefaultMathTransformFactory.class, "createBaseToDerived",
                        Messages.getResources((Locale) null).getLogRecord(Level.WARNING,
                                Messages.Keys.MismatchedEllipsoidAxisLength_3, ellipsoid.getName().getCode(),
                                mismatchedParam.getDescriptor().getName().getCode(), mismatchedValue));
            }
        }
        MathTransform baseToDerived;
        try {
            baseToDerived = createParameterizedTransform(parameters);
            final OperationMethod method = lastMethod.get();
            baseToDerived = createBaseToDerived(baseCRS.getCoordinateSystem(), baseToDerived, derivedCS);
            lastMethod.set(method);
        } catch (FactoryException e) {
            if (failure != null) {
                e.addSuppressed(failure);
            }
            throw e;
        }
        return baseToDerived;
    }

    /**
     * Creates a transform from a base to a derived CS using an existing parameterized transform.
     * This convenience constructor {@linkplain #createConcatenatedTransform concatenates} the given parameterized
     * transform with any other transform required for performing units changes and ordinates swapping.
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
     * @param  baseCS        The source coordinate system.
     * @param  parameterized A <cite>base to derived</cite> transform for normalized input and output coordinates.
     * @param  derivedCS     The target coordinate system.
     * @return The transform from {@code baseCS} to {@code derivedCS}, including unit conversions and axis swapping.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @see org.apache.sis.referencing.cs.AxesConvention#NORMALIZED
     */
    public MathTransform createBaseToDerived(final CoordinateSystem baseCS,
            final MathTransform parameterized, final CoordinateSystem derivedCS)
            throws FactoryException
    {
        /*
         * Computes matrix for swapping axis and performing units conversion.
         * There is one matrix to apply before projection on (longitude,latitude)
         * coordinates, and one matrix to apply after projection on (easting,northing)
         * coordinates.
         */
        final Matrix swap1, swap3;
        try {
            swap1 = CoordinateSystems.swapAndScaleAxes(baseCS, CoordinateSystems.normalize(baseCS));
            swap3 = CoordinateSystems.swapAndScaleAxes(CoordinateSystems.normalize(derivedCS), derivedCS);
        } catch (IllegalArgumentException | ConversionException cause) {
            throw new FactoryException(cause);
        }
        /*
         * Prepares the concatenation of the matrices computed above and the projection.
         * Note that at this stage, the dimensions between each step may not be compatible.
         * For example the projection (step2) is usually two-dimensional while the source
         * coordinate system (step1) may be three-dimensional if it has a height.
         */
        MathTransform step1 = createAffineTransform(swap1);
        MathTransform step3 = createAffineTransform(swap3);
        MathTransform step2 = parameterized;
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
        if (parameterized instanceof ParameterizedAffine) {
            mt = ((ParameterizedAffine) parameterized).newTransform(mt);
        }
        return mt;
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
     *     MathTransform mt = factory.createParameterizedTransform(group);
     * }
     *
     * @param  parameters The parameter values. The {@linkplain ParameterDescriptorGroup#getName() parameter group name}
     *         shall be the name of the desired {@linkplain DefaultOperationMethod operation method}.
     * @return The transform created from the given parameters.
     * @throws NoSuchIdentifierException if there is no method for the given parameter group name.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @see #getDefaultParameters(String)
     * @see #getAvailableMethods(Class)
     * @see #getLastMethodUsed()
     */
    @Override
    public MathTransform createParameterizedTransform(ParameterValueGroup parameters)
            throws NoSuchIdentifierException, FactoryException
    {
        final String methodName = parameters.getDescriptor().getName().getCode();
        OperationMethod method = null;
        try {
            method = getOperationMethod(methodName);
            if (!(method instanceof MathTransformProvider)) {
                throw new NoSuchIdentifierException(Errors.format( // For now, handle like an unknown operation.
                        Errors.Keys.UnsupportedImplementation_1, Classes.getClass(method)), methodName);
            }
            /*
             * If the "official" parameter descriptor was used, that descriptor should have already
             * enforced argument validity. Consequently, there is no need to performs the check and
             * we will avoid it as a performance enhancement.
             */
            final ParameterDescriptorGroup expected = method.getParameters();
            final boolean isConform = expected.equals(parameters.getDescriptor());
            MathTransform transform;
            try {
                if (!isConform) {
                    /*
                     * Copies all values from the user-supplied group to the provider-supplied group.
                     * The later should perform all needed checks. It is supplier's responsibility to
                     * know about alias (e.g. OGC, EPSG, ESRI),  since the caller probably used names
                     * from only one authority.
                     */
                    final ParameterValueGroup copy = expected.createValue();
                    Parameters.copy(parameters, copy);
                    parameters = copy;
                }
                transform  = ((MathTransformProvider) method).createMathTransform(parameters);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                /*
                 * Catch only exceptions which may be the result of improper parameter
                 * usage (e.g. a value out of range). Do not catch exception caused by
                 * programming errors (e.g. null pointer exception).
                 */
                throw new FactoryException(exception);
            }
            transform = unique(transform);
            method = DefaultOperationMethod.redimension(method,
                    transform.getSourceDimensions(), transform.getTargetDimensions());
            return transform;
        } finally {
            lastMethod.set(method); // May be null in case of failure, which is intended.
        }
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
        lastMethod.remove(); // To be strict, we should set the ProjectiveTransform provider
        return unique(MathTransforms.linear(matrix));
    }

    /**
     * Creates a transform by concatenating two existing transforms.
     * A concatenated transform acts in the same way as applying two transforms, one after the other.
     *
     * <p>The dimension of the output space of the first transform must match the dimension of the input space
     * in the second transform. In order to concatenate more than two transforms, use this constructor repeatedly.</p>
     *
     * @param  transform1 The first transform to apply to points.
     * @param  transform2 The second transform to apply to points.
     * @return The concatenated transform.
     * @throws FactoryException if the object creation failed.
     *
     * @see MathTransforms#concatenate(MathTransform, MathTransform)
     */
    @Override
    public MathTransform createConcatenatedTransform(final MathTransform transform1,
                                                     final MathTransform transform2)
            throws FactoryException
    {
        lastMethod.remove();
        final MathTransform tr;
        try {
            tr = MathTransforms.concatenate(transform1, transform2);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
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
            throw new FactoryException(exception);
        }
        return unique(tr);
    }

    /**
     * Creates a math transform object from a XML string. The default implementation
     * always throws an exception, since this constructor is not yet implemented.
     *
     * @param  xml Math transform encoded in XML format.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public MathTransform createFromXML(String xml) throws FactoryException {
        lastMethod.remove();
        throw new FactoryException("Not yet implemented.");
    }

    /**
     * Creates a math transform object from a
     * <a href="http://www.geoapi.org/snapshot/javadoc/org/opengis/referencing/doc-files/WKT.html"><cite>Well
     * Known Text</cite> (WKT)</a>.
     *
     * @param  text Math transform encoded in Well-Known Text format.
     * @return The math transform (never {@code null}).
     * @throws FactoryException if the Well-Known Text can't be parsed,
     *         or if the math transform creation failed from some other reason.
     */
    @Override
    public MathTransform createFromWKT(final String text) throws FactoryException {
        lastMethod.remove();
        throw new FactoryException("Not yet implemented.");
    }

    /**
     * Replaces the given transform by a unique instance, if one already exists.
     */
    final MathTransform unique(final MathTransform tr) {
        return pool.unique(tr);
    }

    /**
     * Returns the operation method used by the latest call to a {@code create(…)} constructor
     * in the currently running thread. Returns {@code null} if not applicable.
     *
     * <p>Invoking {@code getLastMethodUsed()} can be useful after a call to
     * {@link #createParameterizedTransform createParameterizedTransform(…)}, or after a call to another
     * constructor that delegates its work to {@code createParameterizedTransform(…)}, for example
     * {@link #createBaseToDerived createBaseToDerived(…)}.</p>
     *
     * @return The last method used by a {@code create(…)} constructor, or {@code null} if unknown of unsupported.
     *
     * @see #createParameterizedTransform(ParameterValueGroup)
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
            Iterable<? extends OperationMethod> m = methods;
            if (m instanceof LazySet<?>) { // Workaround for JDK bug. See DefaultMathTransformFactory() constructor.
                ((LazySet<?>) m).reload();
                m = ((LazySet<? extends OperationMethod>) m).source;
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
