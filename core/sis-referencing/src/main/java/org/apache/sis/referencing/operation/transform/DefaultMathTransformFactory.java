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
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;


/**
 * Low level factory for creating {@linkplain AbstractMathTransform math transforms}.
 * High level GIS applications usually do not need to use this factory directly.
 * They can use the static convenience methods in the {@link org.apache.sis.referencing.CRS}
 * or {@link MathTransforms} classes instead.
 *
 * {@section Math transforms discovery}
 * Unless {@linkplain #DefaultMathTransformFactory(Iterable) specified explicitely at construction time},
 * {@code OperationMethod} implementations shall be listed in the following file:
 *
 * {@preformat text
 *     META-INF/services/org.opengis.referencing.operation.OperationMethod
 * }
 *
 * {@code DefaultMathTransformFactory} parses the above-cited files in all JAR files in order to find all available
 * operation methods. By default, only operation methods that implement the {@link MathTransformProvider} interface
 * can be used by the {@code create(…)} methods in this class.
 *
 * {@section Thread safety}
 * This class is safe for multi-thread usage if all referenced {@code OperationMethod} instances are thread-safe.
 * There is typically only one {@code MathTransformFactory} instance for the whole application.
 *
 * @author  Martin Desruisseaux (Geomatys, IRD)
 * @since   0.6
 * @version 0.6
 * @module
 */
public abstract class DefaultMathTransformFactory extends AbstractFactory implements MathTransformFactory {
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
     * The last method used by a {@code create(…)} method.
     */
    private final ThreadLocal<OperationMethod> lastMethod;

    /**
     * The math transforms created so far. This pool is used in order
     * to return instances of existing math transforms when possible.
     */
    private final WeakHashSet<MathTransform> pool;

    /**
     * Creates a new factory which will discover operation methods with a {@link ServiceLoader}.
     * {@code OperationMethod} implementations shall be listed in the following file:
     *
     * {@preformat text
     *     META-INF/services/org.opengis.referencing.operation.OperationMethod
     * }
     *
     * {@code DefaultMathTransformFactory} parses the above-cited files in all JAR files in order to find all available
     * operation methods. By default, only operation methods that implement the {@link MathTransformProvider} interface
     * can be used by the {@code create(…)} methods in this class.
     */
    public DefaultMathTransformFactory() {
        this(ServiceLoader.load(OperationMethod.class));
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
     * This method may conservatively return more {@code OperationMethod} elements than requested
     * if it does not support filtering by the given type.
     *
     * @param  type <code>{@linkplain SingleOperation}.class</code> for fetching all operation methods,
     *         <code>{@linkplain Projection}.class</code> for fetching only map projection methods, <i>etc</i>.
     * @return Methods available in this factory for coordinate operations of the given type.
     *
     * @see #getDefaultParameters(String)
     * @see #createParameterizedTransform(ParameterValueGroup)
     * @see DefaultOperationMethod#getOperationType()
     */
    @Override
    public Set<OperationMethod> getAvailableMethods(final Class<? extends SingleOperation> type) {
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
     * @return The operation method for the given name or identifier.
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
                throw new NoSuchIdentifierException(Errors.format(Errors.Keys.NoSuchOperationMethod_1, method), identifier);
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
     * This method is private and static for now, but we may consider to make it a protected member in a future
     * SIS version in order to give users a chance to override the matching criterion. We don't do that yet
     * because we would like to have at least one use case before doing so.
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
     * Returns the default parameter values for a math transform using the given method.
     * The method argument is the name of any operation method returned by the {@link #getAvailableMethods(Class)} method.
     * A typical example is
     * "<a href="http://www.remotesensing.org/geotiff/proj_list/transverse_mercator.html">Transverse Mercator</a>").
     *
     * <p>This method creates new parameter instances at every call. The returned object is intended to be modified
     * by the user before to be passed to <code>{@linkplain #createParameterizedTransform(ParameterValueGroup)
     * createParameterizedTransform}(parameters)</code>.</p>
     *
     * @param  method The name or identifier of the operation method to search.
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
     * <p><b>NOTE:</b> Do not merge this method with {@code ensureSet(…)}. We keep those two methods
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
        }
        return true;
    }

    /**
     * Creates a {@linkplain #createParameterizedTransform(ParameterValueGroup) parameterized transform}
     * from a base CRS to a derived CS. This convenience method {@linkplain #createConcatenatedTransform
     * concatenates} the parameterized transform with any other transform required for performing units
     * changes and ordinates swapping.
     *
     * In addition, this method infers the {@code "semi_major"} and {@code "semi_minor"} parameters values
     * from the {@linkplain org.apache.sis.referencing.datum.DefaultEllipsoid ellipsoid} associated to the
     * {@code baseCRS}, if those parameters are not explicitly given and if they are applicable (typically
     * for cartographic projections).
     *
     * <p>The {@code OperationMethod} instance used by this method can be obtained by a call to
     * {@link #getLastMethodUsed()}.</p>
     *
     * @param  baseCRS    The source coordinate reference system.
     * @param  parameters The parameter values for the transform.
     * @param  derivedCS  The target coordinate system.
     * @return The parameterized transform.
     * @throws NoSuchIdentifierException if there is no transform registered for the method.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
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
                final ParameterValue<?> semiMajor = parameters.parameter("semi_major");
                final ParameterValue<?> semiMinor = parameters.parameter("semi_minor");
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
            baseToDerived = createBaseToDerived(baseCRS, baseToDerived, derivedCS);
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
     * Creates a transform from a base CRS to a derived CS. This method expects a "raw" transform without
     * unit conversion or axis swapping. Such "raw" transforms are typically map projections working on
     * (<cite>longitude</cite>, <cite>latitude</cite>) axes in degrees and (<cite>x</cite>, <cite>y</cite>)
     * axes in metres. This method inspects the coordinate systems and prepend or append the unit conversions
     * and axis swapping automatically.
     *
     * @param  baseCRS    The source coordinate reference system.
     * @param  projection The "raw" <cite>base to derived</cite> transform.
     * @param  derivedCS  the target coordinate system.
     * @return The parameterized transform.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     */
    public abstract MathTransform createBaseToDerived(final CoordinateReferenceSystem baseCRS,
            final MathTransform projection, final CoordinateSystem derivedCS)
            throws FactoryException;

    /**
     * Creates a transform by concatenating two existing transforms.
     * A concatenated transform acts in the same way as applying two transforms, one after the other.
     *
     * <p>The dimension of the output space of the first transform must match the dimension of the input space
     * in the second transform. In order to concatenate more than two transforms, use this method repeatedly.</p>
     *
     * @param  transform1 The first transform to apply to points.
     * @param  transform2 The second transform to apply to points.
     * @return The concatenated transform.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public MathTransform createConcatenatedTransform(final MathTransform transform1,
                                                     final MathTransform transform2)
            throws FactoryException
    {
        MathTransform tr;
        try {
            tr = MathTransforms.concatenate(transform1, transform2);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        tr = pool.unique(tr);
        return tr;
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
     * @param  subTransform          Transform to use for affected ordinates.
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
        MathTransform tr;
        try {
            tr = PassThroughTransform.create(firstAffectedOrdinate, subTransform, numTrailingOrdinates);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        tr = pool.unique(tr);
        return tr;
    }

    /**
     * Returns the operation method used for the latest call to
     * {@link #createParameterizedTransform(ParameterValueGroup)} in the currently running thread.
     * Returns {@code null} if not applicable.
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
            if (methods instanceof ServiceLoader<?>) {
                ((ServiceLoader<?>) methods).reload();
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
