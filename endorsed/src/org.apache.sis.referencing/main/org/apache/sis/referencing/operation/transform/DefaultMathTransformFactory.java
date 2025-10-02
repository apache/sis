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

import java.util.Set;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.ServiceLoader;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Constructor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.apache.sis.io.wkt.Parser;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.referencing.internal.ParameterizedTransformBuilder;
import org.apache.sis.referencing.internal.shared.CoordinateOperations;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.system.Reflect;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.apache.sis.referencing.operation.matrix.Matrices;


/**
 * Low level factory for creating {@linkplain AbstractMathTransform math transforms}.
 * The objects created by this factory do not know what the source and target coordinate systems mean.
 * Because of this low semantic value, high level GIS applications usually do not need to use this factory directly.
 * They can use the static convenience methods in the {@link org.apache.sis.referencing.CRS}
 * or {@link MathTransforms} classes instead.
 *
 *
 * <h2>Standard parameters</h2>
 * {@code MathTransform} instances are created from {@linkplain DefaultParameterValueGroup parameter values}.
 * The parameters expected by each operation available in a default Apache SIS installation is
 * <a href="https://sis.apache.org/tables/CoordinateOperationMethods.html">listed here</a>.
 * The set of parameters varies for each operation or projection, but the following can be considered typical:
 *
 * <ul>
 *   <li>A <var>semi-major</var> and <var>semi-minor</var> axis length in metres.</li>
 *   <li>A <var>central meridian</var> and <var>latitude of origin</var> in decimal degrees.</li>
 *   <li>A <var>scale factor</var>, which default to 1.</li>
 *   <li>A <var>false easting</var> and <var>false northing</var> in metres, which default to 0.</li>
 * </ul>
 *
 * <p>Each descriptor has many aliases, and those aliases may vary between different projections.
 * For example, the <i>false easting</i> parameter is usually called {@code "false_easting"}
 * by OGC, while EPSG uses various names like <q>False easting</q> or <q>Easting at
 * false origin</q>.</p>
 *
 * <h2>Dynamic parameters</h2>
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
 * without duplicating the most commonly used conventions. The alternative ways are used in netCDF
 * files for example, which often use spherical models instead of ellipsoidal ones.</p>
 *
 *
 * <h2><a id="Obligation">Mandatory and optional parameters</a></h2>
 * Parameters are flagged as either <i>mandatory</i> or <i>optional</i>.
 * A parameter may be mandatory and still have a default value. In the context of this package, "mandatory"
 * means that the parameter is an essential part of the projection defined by standards.
 * Such mandatory parameters will always appears in any <i>Well Known Text</i> (WKT) formatting,
 * even if not explicitly set by the user. For example, the central meridian is typically a mandatory
 * parameter with a default value of 0° (the Greenwich meridian).
 *
 * <p>Optional parameters, on the other hand, are often non-standard extensions.
 * They will appear in WKT formatting only if the user defined explicitly a value which is different than the
 * default value.</p>
 *
 *
 * <h2>Operation methods discovery</h2>
 * {@link OperationMethod} describes all the parameters expected for instantiating a particular kind of
 * math transform. The set of operation methods known to this factory can be obtained in two ways:
 *
 * <ul>
 *   <li>{@linkplain #DefaultMathTransformFactory(Iterable) specified explicitly at construction time}, or</li>
 *   <li>{@linkplain #DefaultMathTransformFactory() discovered by scanning the module path}.</li>
 * </ul>
 *
 * The default way is to scan the module path. See {@link MathTransformProvider} for indications about how to add
 * custom coordinate operation methods in a default Apache SIS installation.
 *
 *
 * <h2>Thread safety</h2>
 * This class is safe for multi-thread usage if all referenced {@code OperationMethod} instances are thread-safe.
 * There is typically only one {@code MathTransformFactory} instance for the whole application.
 *
 * @author  Martin Desruisseaux (Geomatys, IRD)
 * @version 1.6
 *
 * @see MathTransformProvider
 * @see AbstractMathTransform
 *
 * @since 0.6
 */
public class DefaultMathTransformFactory extends AbstractFactory implements MathTransformFactory, Parser {
    /*
     * NOTE FOR JAVADOC WRITER:
     * The "method" word is ambiguous here, because it can be "Java method" or "coordinate operation method".
     * In this class, we reserve the "method" word for "coordinate operation method" as much as possible.
     * For Java methods, we rather use "constructor" or "function".
     */

    /**
     * The constructor for WKT parsers, fetched when first needed. The WKT parser is defined in the
     * same module as this class, so we will hopefully not have security issues.  But we have to
     * use reflection because the parser class is not yet public (because we do not want to commit
     * its API yet).
     */
    private static volatile Constructor<? extends Parser> parserConstructor;

    /**
     * All methods specified at construction time or found on the module path.
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
     * <h4>Implementation note</h4>
     * We do not use a concurrent map here because the number of entries is expected to be very small
     * (about 2 entries), which make concurrent algorithms hardly efficient. Furthermore, this map is
     * not used often.
     */
    private final Map<Class<?>, OperationMethodSet> methodsByType;

    /**
     * The math transforms created so far. This pool is used in order to return instances of existing
     * math transforms when possible. If {@code null}, then no pool should be used. A null value is
     * preferable when the transforms are known to be short-lived, for avoiding the cost of caching them.
     *
     * @see #unique(MathTransform)
     */
    private final WeakHashSet<MathTransform> pool;

    /**
     * The <i>Well Known Text</i> parser for {@code MathTransform} instances.
     * This parser is not thread-safe, so we need to prevent two threads from using
     * the same instance at the same time.
     */
    private final AtomicReference<Parser> parser;

    /**
     * The factory with opposite caching factory, or {@code null} if not yet created.
     *
     * @see #caching(boolean)
     */
    private DefaultMathTransformFactory oppositeCachingPolicy;

    /**
     * The default factory instance.
     */
    private static final DefaultMathTransformFactory INSTANCE = new DefaultMathTransformFactory();

    /**
     * Returns the default provider of {@code MathTransform} instances.
     * This is the factory used by the Apache SIS library when no non-null
     * {@link MathTransformFactory} has been explicitly specified.
     * This method can be invoked directly, or indirectly through
     * {@code ServiceLoader.load(MathTransformFactory.class)}.
     *
     * @return the default provider of math transforms.
     *
     * @see java.util.ServiceLoader
     * @since 1.4
     */
    public static DefaultMathTransformFactory provider() {
        return INSTANCE;
    }

    /**
     * Creates a new factory which will discover operation methods with a {@link ServiceLoader}.
     * The {@link OperationMethod} implementations shall be listed in {@code module-info.java}
     * as provider of the {@code org.opengis.referencing.operation.OperationMethod} service.
     * {@code DefaultMathTransformFactory} scans the providers of all modules in order to list
     * all available operation methods.
     * Currently, only operation methods implementing the {@link MathTransformProvider} interface
     * can be used by the {@code create(…)} methods in this class.
     *
     * @see #provider()
     * @see #reload()
     */
    public DefaultMathTransformFactory() {
        this(operations());
    }

    /** Temporary method to be removed after upgrade to JDK24. */
    private static ServiceLoader<OperationMethod> operations() {
        try {
            return ServiceLoader.load(OperationMethod.class, Reflect.getContextClassLoader());
        } catch (SecurityException e) {
            Reflect.log(DefaultMathTransformFactory.class, "<init>", e);
            return ServiceLoader.load(OperationMethod.class);
        }
    }

    /**
     * Creates a new factory which will use the given operation methods. The given iterable is stored by reference —
     * its content is <strong>not</strong> copied — in order to allow deferred {@code OperationMethod} constructions.
     * Note that by default, only operation methods that implement the {@link MathTransformProvider} interface can be
     * used by the {@code create(…)} methods in this class.
     *
     * <h4>Requirements</h4>
     * <ul>
     *   <li>The given iterable should not contain duplicated elements.</li>
     *   <li>The given iterable shall be stable: all elements returned by the first iteration must also be
     *       returned by any subsequent iterations, unless {@link #reload()} has been invoked.</li>
     *   <li>{@code OperationMethod} instances should also implement {@link MathTransformProvider}.</li>
     *   <li>All {@code OperationMethod} instances shall be thread-safe.</li>
     *   <li>The {@code Iterable} itself does not need to be thread-safe since all usages will be synchronized as below:
     *
     *       {@snippet lang="java" :
     *           synchronized (methods) {
     *               for (OperationMethod method : methods) {
     *                   // Use the method here.
     *               }
     *           }
     *           }
     *   </li>
     * </ul>
     *
     * @param  methods  the operation methods to use, stored by reference (not copied).
     */
    public DefaultMathTransformFactory(final Iterable<? extends OperationMethod> methods) {
        this.methods  = Objects.requireNonNull(methods);
        methodsByName = new ConcurrentHashMap<>();
        methodsByType = new IdentityHashMap<>();
        pool          = new WeakHashSet<>(MathTransform.class);
        parser        = new AtomicReference<>();
    }

    /**
     * Creates a new factory with the same configuration as given factory but without caching.
     */
    private DefaultMathTransformFactory(final DefaultMathTransformFactory parent) {
        methods       = parent.methods;
        methodsByName = parent.methodsByName;
        methodsByType = parent.methodsByType;
        pool          = null;
        parser        = parent.parser;
        oppositeCachingPolicy = parent;
    }

    /**
     * Returns a factory for the same transforms as this factory, but with caching potentially disabled.
     * By default, {@code DefaultMathTransformFactory} caches the {@link MathTransform} instances for sharing
     * existing instances when transforms are created many times with the same set of parameters.
     * However, this caching may be unnecessarily costly when the transforms to create are known to be short lived.
     * This method allows to get a factory better suited for short-lived objects.
     *
     * <p>This method does not modify the state of this factory. Instead, different factory instances for the
     * different caching policy are returned.</p>
     *
     * @param  enabled  whether caching should be enabled.
     * @return a factory for the given caching policy.
     *
     * @since 1.1
     */
    public DefaultMathTransformFactory caching(final boolean enabled) {
        if (enabled) {
            return this;
        }
        synchronized (this) {
            if (oppositeCachingPolicy == null) {
                oppositeCachingPolicy = new NoCache(this);
            }
            return oppositeCachingPolicy;
        }
    }

    /**
     * Accessor for {@link NoCache} implementation.
     */
    final DefaultMathTransformFactory oppositeCachingPolicy() {
        return oppositeCachingPolicy;
    }

    /**
     * A factory performing no caching.
     * This factory shares the same operation methods as the parent factory.
     */
    private static final class NoCache extends DefaultMathTransformFactory {
        /** Creates a new factory with the same configuration as given factory. */
        NoCache(final DefaultMathTransformFactory parent) {
            super(parent);
        }

        /** Returns a factory for the same transforms but given caching policy. */
        @Override public DefaultMathTransformFactory caching(final boolean enabled) {
            return enabled ? oppositeCachingPolicy() : this;
        }

        /** Notifies parent factory that the set of operation methods may have changed. */
        @Override public void reload() {
            oppositeCachingPolicy().reload();
        }
    }

    /**
     * Returns a set of available methods for coordinate operations of the given type.
     * The {@code type} argument can be used for filtering the kind of operations described by the returned
     * {@code OperationMethod}s. The argument is usually (but not restricted to) one of the following types:
     *
     * <ul>
     *   <li>{@link org.opengis.referencing.operation.Conversion}
     *       for coordinate operations described by definitions (including map projections).</li>
     *   <li>{@link org.opengis.referencing.operation.Transformation}
     *       for coordinate operations described by empirically derived parameters.</li>
     *   <li>{@link org.opengis.referencing.operation.PointMotionOperation}
     *       for changes due to the motion of the point between two coordinate epochs.</li>
     *   <li>{@link SingleOperation} for all coordinate operations.</li>
     * </ul>
     *
     * The returned set may conservatively contain more {@code OperationMethod} elements than requested
     * if this {@code MathTransformFactory} does not support filtering by the given type.
     *
     * @param  type  <code>{@linkplain SingleOperation}.class</code> for fetching all operation methods,
     *               <code>{@linkplain org.opengis.referencing.operation.Transformation}.class</code>
     *               for operations defined by empirical parameters, <i>etc</i>.
     * @return methods available in this factory for coordinate operations of the given type.
     *
     * @see #builder(String)
     * @see DefaultOperationMethod#getOperationType()
     */
    @Override
    public Set<OperationMethod> getAvailableMethods(final Class<? extends SingleOperation> type) {
        OperationMethodSet set;
        synchronized (methodsByType) {
            set = methodsByType.get(Objects.requireNonNull(type));
        }
        if (set == null) {
            /*
             * Implementation note: we are better to avoid holding a lock on `methods` and `methodsByType`
             * at the same time because the `methods` iterator could be a user's implementation which callback
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
     * a method {@linkplain DefaultOperationMethod#getName() name} (e.g. <q>Transverse Mercator</q>)
     * or one of its {@linkplain DefaultOperationMethod#getIdentifiers() identifiers} (e.g. {@code "EPSG:9807"}).
     *
     * <p>The search is case-insensitive. Comparisons against method names can be
     * {@linkplain DefaultOperationMethod#isHeuristicMatchForName(String) heuristic}.</p>
     *
     * <p>If more than one method match the given identifier, then the first (according iteration order)
     * non-{@linkplain org.apache.sis.util.Deprecable#isDeprecated() deprecated} matching method is returned.
     * If all matching methods are deprecated, the first one is returned.</p>
     *
     * @param  identifier  the name or identifier of the operation method to search.
     * @return the coordinate operation method for the given name or identifier.
     * @throws NoSuchIdentifierException if there is no operation method registered for the specified identifier.
     *
     * @see org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory#getOperationMethod(String)
     */
    public OperationMethod getOperationMethod(String identifier) throws NoSuchIdentifierException {
        ArgumentChecks.ensureNonEmpty("identifier", identifier = identifier.strip());
        OperationMethod method = methodsByName.get(identifier);
        if (method == null) {
            synchronized (methods) {
                method = CoordinateOperations.findMethod(methods, identifier);
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
     * Returns a builder for a parameterized math transform using the specified operation method.
     * The {@code method} argument should be the name or identifier of an {@link OperationMethod}
     * instance returned by <code>{@link #getAvailableMethods(Class) getAvailableMethods}(null)</code>,
     * with the addition of the following pseudo-methods:
     *
     * <ul>
     *   <li>"Coordinate system conversion"</li>
     * </ul>
     *
     * The returned builder allows to specify not only the operation parameter values,
     * but also some contextual information such as the source and target axes.
     * The builder uses these information for:
     *
     * <ol>
     *   <li>Inferring the {@code "semi_major"}, {@code "semi_minor"}, {@code "src_semi_major"},
     *       {@code "src_semi_minor"}, {@code "tgt_semi_major"} or {@code "tgt_semi_minor"} parameter values
     *       from the {@link Ellipsoid} associated to the source or target <abbr>CRS</abbr>, if these parameters
     *       are not explicitly given and if they are relevant for the coordinate operation method.</li>
     *   <li>{@linkplain #createConcatenatedTransform Concatenating} the parameterized transform
     *       with any other transforms required for performing units changes and coordinates swapping.</li>
     * </ol>
     *
     * The builder does <strong>not</strong> handle change of
     * {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum#getPrimeMeridian() prime meridian}
     * or anything else related to datum. Datum changes have dedicated {@link OperationMethod},
     * for example <q>Longitude rotation</q> (EPSG:9601) for changing the prime meridian.
     *
     * @param  method  the case insensitive name or identifier of the desired coordinate operation method.
     * @return a builder for a meth transform implementing the formulas identified by the given method.
     * @throws NoSuchIdentifierException if there is no supported method for the given name or identifier.
     *
     * @see #getAvailableMethods(Class)
     * @since 1.5
     */
    @Override
    public MathTransform.Builder builder(final String method) throws NoSuchIdentifierException {
        if (method.replace('_', ' ').equalsIgnoreCase(Constants.COORDINATE_SYSTEM_CONVERSION)) {
            return new CoordinateSystemTransformBuilder(this);
        }
        return new ParameterizedTransformBuilder(this, getOperationMethod(method));
    }

    /**
     * Creates an affine transform from a matrix. If the transform input dimension is {@code M},
     * and output dimension is {@code N}, then the matrix will have size {@code [N+1][M+1]}. The
     * +1 in the matrix dimensions allows the matrix to do a shift, as well as a rotation. The
     * {@code [M][j]} element of the matrix will be the j'th coordinate of the moved origin. The
     * {@code [i][N]} element of the matrix will be 0 for <var>i</var> less than {@code M}, and 1
     * for <var>i</var> equals {@code M}.
     *
     * @param  matrix  the matrix used to define the affine transform.
     * @return the affine transform.
     * @throws FactoryException if the object creation failed.
     *
     * @see MathTransforms#linear(Matrix)
     */
    @Override
    public MathTransform createAffineTransform(final Matrix matrix) throws FactoryException {
        return unique(MathTransforms.linear(matrix));
    }

    /**
     * Creates a modifiable matrix of size {@code numRow}&nbsp;×&nbsp;{@code numCol}.
     * Elements on the diagonal (<var>j</var> == <var>i</var>) are set to 1.
     *
     * @param  numRow  number of rows.
     * @param  numCol  number of columns.
     * @return a new matrix of the given size.
     * @throws FactoryException if the matrix creation failed.
     *
     * @since 2.0 (temporary version number until this branch is released)
     */
    @Override
    public Matrix createMatrix(int numRow, int numCol) throws FactoryException {
        return Matrices.createDiagonal(numRow, numCol);
    }

    /**
     * Creates a transform by concatenating two existing transforms.
     * A concatenated transform acts in the same way as applying two transforms, one after the other.
     *
     * <p>The dimension of the output space of the first transform must match the dimension of the input space
     * in the second transform. In order to concatenate more than two transforms, use this constructor repeatedly.</p>
     *
     * @param  tr1  the first transform to apply to points.
     * @param  tr2  the second transform to apply to points.
     * @return the concatenated transform.
     * @throws FactoryException if the object creation failed.
     *
     * @see MathTransforms#concatenate(MathTransform, MathTransform)
     */
    @Override
    public MathTransform createConcatenatedTransform(final MathTransform tr1,
                                                     final MathTransform tr2)
            throws FactoryException
    {
        ArgumentChecks.ensureNonNull("tr1", tr1);
        ArgumentChecks.ensureNonNull("tr2", tr2);
        final MathTransform tr;
        try {
            tr = ConcatenatedTransform.create(this, tr1, tr2);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
        }
        return unique(tr);
    }

    /**
     * Creates a transform which passes through a subset of coordinates to another transform.
     * This allows transforms to operate on a subset of coordinates.
     * The resulting transform will have the following dimensions:
     *
     * {@snippet lang="java" :
     *     int sourceDim = firstAffectedCoordinate + subTransform.getSourceDimensions() + numTrailingCoordinates;
     *     int targetDim = firstAffectedCoordinate + subTransform.getTargetDimensions() + numTrailingCoordinates;
     *     }
     *
     * <h4>Example</h4>
     * Giving (<var>latitude</var>, <var>longitude</var>, <var>height</var>) coordinates,
     * a pass through transform can convert the height values from meters to feet without
     * affecting the (<var>latitude</var>, <var>longitude</var>) values.
     *
     * @param  firstAffectedCoordinate  the lowest index of the affected coordinates.
     * @param  subTransform             transform to use for affected coordinates.
     * @param  numTrailingCoordinates   number of trailing coordinates to pass through. Affected coordinates will range
     *         from {@code firstAffectedCoordinate} inclusive to {@code dimTarget-numTrailingCoordinates} exclusive.
     * @return a pass through transform.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public MathTransform createPassThroughTransform(final int firstAffectedCoordinate,
                                                    final MathTransform subTransform,
                                                    final int numTrailingCoordinates)
            throws FactoryException
    {
        final MathTransform tr;
        try {
            tr = MathTransforms.passThrough(firstAffectedCoordinate, subTransform, numTrailingCoordinates);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
        }
        return unique(tr);
    }

    /**
     * Creates a math transform object from a Well Known Text (<abbr>WKT</abbr>).
     * If the given text contains non-fatal anomalies (unknown or unsupported WKT elements,
     * inconsistent unit definitions, <i>etc.</i>), warnings may be reported in a
     * {@linkplain java.util.logging.Logger logger} named {@code "org.apache.sis.io.wkt"}.
     *
     * <p>Note that the WKT format is not always lossless. A {@code MathTransform} recreated from WKT may be
     * non-invertible even if the original transform was invertible. For example if an "Affine" operation is
     * defined by a non-square matrix, Apache SIS implementation sometimes has "hidden" information about the
     * inverse matrix but those information are lost at WKT formatting time. A similar "hidden" information
     * lost may also happen with {@link WraparoundTransform}, also making that transform non-invertible.</p>
     *
     * @param  wkt  math transform encoded in Well-Known Text format.
     * @return the math transform (never {@code null}).
     * @throws FactoryException if the Well-Known Text cannot be parsed,
     *         or if the math transform creation failed from some other reason.
     */
    @Override
    public MathTransform createFromWKT(final String wkt) throws FactoryException {
        ArgumentChecks.ensureNonEmpty("wkt", wkt);
        Parser p = parser.getAndSet(null);
        if (p == null) try {
            Constructor<? extends Parser> c = parserConstructor;
            if (c == null) {
                c = Class.forName("org.apache.sis.io.wkt.MathTransformParser").asSubclass(Parser.class)
                         .getConstructor(MathTransformFactory.class);
                c.setAccessible(true);
                parserConstructor = c;
            }
            p = c.newInstance(this);
        } catch (ReflectiveOperationException e) {
            throw new FactoryException(e);
        }
        /*
         * No need to check the type of the parsed object, because MathTransformParser
         * should return only instance of MathTransform.
         */
        final Object object;
        try {
            object = p.createFromWKT(wkt);
        } catch (FactoryException e) {
            /*
             * The parsing may fail because a operation parameter is not known to SIS. If this happen, replace
             * the generic exception thrown be the parser (which is FactoryException) by a more specific one.
             * Note that InvalidGeodeticParameterException is defined only in this `org.apache.sis.referencing` module,
             * so we could not throw it from the `org.apache.sis.metadata` module that contain the parser.
             */
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof ParameterNotFoundException) {
                    throw new InvalidGeodeticParameterException(e.getLocalizedMessage(), cause);
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
    final MathTransform unique(final MathTransform tr) {
        return (pool != null) ? pool.unique(tr) : tr;
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
     * for example following changes of the application module path.
     *
     * @see #DefaultMathTransformFactory(Iterable)
     * @see ServiceLoader#reload()
     */
    public void reload() {
        synchronized (methods) {
            methodsByName.clear();
            final Iterable<? extends OperationMethod> m = methods;
            if (m instanceof ServiceLoader<?>) {
                ((ServiceLoader<?>) m).reload();
            }
            synchronized (methodsByType) {
                for (final OperationMethodSet c : methodsByType.values()) {
                    c.reset();
                }
            }
            if (pool != null) {
                pool.clear();
            }
        }
    }
}
