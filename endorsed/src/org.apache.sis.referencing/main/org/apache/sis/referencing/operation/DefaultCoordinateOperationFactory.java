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
package org.apache.sis.referencing.operation;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.cs.CSFactory;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.MergedProperties;
import org.apache.sis.referencing.internal.ParameterizedTransformBuilder;
import org.apache.sis.referencing.internal.shared.CoordinateOperations;
import org.apache.sis.referencing.internal.shared.ReferencingFactoryContainer;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.resources.Errors;


/**
 * Creates {@linkplain AbstractCoordinateOperation operations} capable to transform coordinates
 * from a given source CRS to a given target CRS. This factory provides two ways to create such
 * operations:
 *
 * <ul>
 *   <li>By fetching or building explicitly each components of the operation:
 *     <ul>
 *       <li>The {@link DefaultOperationMethod operation method}, which can be
 *         {@linkplain #getOperationMethod fetched from a set of predefined methods} or
 *         {@linkplain #createOperationMethod built explicitly}.</li>
 *       <li>A single {@linkplain #createDefiningConversion defining conversion}.</li>
 *       <li>A {@linkplain #createConcatenatedOperation concatenation} of other operations.</li>
 *     </ul>
 *   </li>
 *   <li>By {@linkplain #createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem) giving only the source
 *     and target CRS}, then let the Apache SIS referencing engine infers by itself the coordinate operation
 *     (with the help of an EPSG database if available).</li>
 * </ul>
 *
 * The second approach is the most frequently used.
 *
 *
 * <h2>Thread safety</h2>
 * This class is safe for multi-thread usage if all referenced factories are thread-safe.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 * @since   0.6
 */
public class DefaultCoordinateOperationFactory extends AbstractFactory implements CoordinateOperationFactory {
    /**
     * Whether this class is allowed to use the EPSG authority factory for searching coordinate operation paths.
     * This flag should always be {@code true}, except temporarily for testing purposes.
     */
    @Debug
    static final boolean USE_EPSG_FACTORY = true;

    /**
     * The default properties, or an empty map if none. This map shall be immutable
     * in order to allow usage without synchronization in multi-thread context.
     */
    private final Map<String,?> defaultProperties;

    /**
     * The factory to use if {@link CoordinateOperationFinder} needs to create CRS for intermediate steps.
     */
    final CRSFactory crsFactory;

    /**
     * The factory to use if {@link CoordinateOperationFinder} needs to create CS for intermediate steps.
     */
    final CSFactory csFactory;

    /**
     * The math transform factory.
     *
     * @see #getMathTransformFactory()
     */
    private final MathTransformFactory mtFactory;

    /**
     * Weak references to existing objects.
     * This set is used in order to return a pre-existing object instead of creating a new one.
     * This applies to objects created explicitly, not to coordinate operations inferred by a
     * call to {@link #createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem)}.
     */
    private final WeakHashSet<IdentifiedObject> pool;

    /**
     * The cache of coordinate operations found for a given pair of source and target CRS.
     * If current implementation, we cache only operations found without context (otherwise
     * we would need to take in account the area of interest and desired accuracy in the key).
     *
     * @see #createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, CoordinateOperationContext)
     */
    final Cache<CRSPair,CoordinateOperation> cache;

    /**
     * The default factory instance.
     */
    private static final DefaultCoordinateOperationFactory INSTANCE = new DefaultCoordinateOperationFactory();

    /**
     * Returns the default provider of {@code CoordinateOperation} instances.
     * This is the factory used by the Apache SIS library when no non-null
     * {@link CoordinateOperationFactory} has been explicitly specified.
     * This method can be invoked directly, or indirectly through
     * {@code ServiceLoader.load(CoordinateOperationFactory.class)}.
     *
     * @return the default provider of coordinate operations.
     *
     * @see java.util.ServiceLoader
     * @since 1.4
     */
    public static DefaultCoordinateOperationFactory provider() {
        return INSTANCE;
    }

    /**
     * Constructs a factory with no default properties.
     *
     * @see #provider()
     */
    public DefaultCoordinateOperationFactory() {
        this(null, null);
    }

    /**
     * Constructs a factory with the given default properties.
     * The new factory will fallback on the map given to this constructor
     * for any property not present in the map given to a {@code createFoo(Map<String,?>, …)} method.
     *
     * @param properties  the default properties, or {@code null} if none.
     * @param factory     the factory to use for creating {@linkplain AbstractMathTransform math transforms},
     *                    or {@code null} for the default factory.
     */
    @SuppressWarnings({"LocalVariableHidesMemberVariable", "UnusedAssignment"})
    public DefaultCoordinateOperationFactory(Map<String,?> properties, MathTransformFactory factory) {
        final CSFactory  csFactory;
        final CRSFactory crsFactory;
        if (properties == null || properties.isEmpty()) {
            properties = Map.of();
            crsFactory = null;
            csFactory  = null;
        } else {
            String key   = null;
            Object value = null;
            properties   = new HashMap<>(properties);
            final MathTransformFactory mtFactory;
            /*
             * Following use of properties is an undocumented feature for now. Current version documents only
             * MathTransformFactory because math transforms are intimately related to coordinate operations.
             */
            try {
                crsFactory = (CRSFactory)           (value = properties.remove(key = ReferencingFactoryContainer.CRS_FACTORY));
                csFactory  = (CSFactory)            (value = properties.remove(key = ReferencingFactoryContainer.CS_FACTORY));
                mtFactory  = (MathTransformFactory) (value = properties.remove(key = ReferencingFactoryContainer.MT_FACTORY));
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(Errors.forProperties(properties)
                        .getString(Errors.Keys.IllegalPropertyValueClass_2, key, Classes.getClass(value)));
            }
            properties.remove(ReferencingFactoryContainer.DATUM_FACTORY);
            properties = Map.copyOf(properties);
            if (factory == null) {
                factory = mtFactory;
            }
        }
        this.mtFactory  = (   factory != null) ?    factory : DefaultMathTransformFactory.provider();
        this.csFactory  = ( csFactory != null) ?  csFactory : GeodeticObjectFactory.provider();
        this.crsFactory = (crsFactory != null) ? crsFactory : GeodeticObjectFactory.provider();
        defaultProperties = properties;
        pool = new WeakHashSet<>(IdentifiedObject.class);
        cache = new Cache<>(12, 50, true);
    }

    /**
     * Returns the union of the given {@code properties} map with the default properties given at
     * {@linkplain #DefaultCoordinateOperationFactory(Map, MathTransformFactory) construction time}.
     * Entries in the given properties map have precedence, even if their
     * {@linkplain java.util.Map.Entry#getValue() value} is {@code null}
     * (i.e. a null value "erase" the default property value).
     * Entries with null value after the union will be omitted.
     *
     * <p>This method is invoked by all {@code createFoo(Map<String,?>, …)} methods for determining
     * the properties to give to {@linkplain AbstractCoordinateOperation#AbstractCoordinateOperation(Map,
     * CoordinateReferenceSystem, CoordinateReferenceSystem, CoordinateReferenceSystem, MathTransform)
     * coordinate operation constructor}.</p>
     *
     * @param  properties  the user supplied properties.
     * @return the union of the given properties with the default properties.
     */
    protected Map<String,?> complete(final Map<String,?> properties) {
        ArgumentChecks.ensureNonNull("properties", properties);
        return new MergedProperties(properties, defaultProperties);
    }

    /**
     * Returns the underlying math transform factory. This factory is used for constructing the {@link MathTransform}
     * instances doing the actual mathematical work of {@linkplain AbstractCoordinateOperation coordinate operations}
     * instances.
     *
     * @return the underlying math transform factory.
     *
     * @since 1.1
     */
    public final MathTransformFactory getMathTransformFactory() {
        return mtFactory;
    }

    /**
     * Returns the operation method of the given name. The given argument shall be either a method
     * {@linkplain DefaultOperationMethod#getName() name} (e.g. <q>Transverse Mercator</q>)
     * or one of its {@linkplain DefaultOperationMethod#getIdentifiers() identifiers} (e.g. {@code "EPSG:9807"}).
     * The search is case-insensitive and comparisons against method names can be
     * {@linkplain DefaultOperationMethod#isHeuristicMatchForName(String) heuristic}.
     *
     * <p>If more than one method match the given name, then the first (according iteration order)
     * non-{@linkplain org.apache.sis.util.Deprecable#isDeprecated() deprecated} matching method is returned.
     * If all matching methods are deprecated, the first one is returned.</p>
     *
     * @param  name  the name of the operation method to fetch.
     * @return the operation method of the given name.
     * @throws FactoryException if the requested operation method cannot be fetched.
     *
     * @see DefaultMathTransformFactory#getOperationMethod(String)
     *
     * @deprecated Use {@link DefaultMathTransformFactory} instead.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public OperationMethod getOperationMethod(String name) throws FactoryException {
        return CoordinateOperations.findMethod(mtFactory, name);
    }

    /**
     * Creates an operation method from a set of properties and a descriptor group.
     * The source and target dimensions may be {@code null} if the method can work
     * with any number of dimensions (e.g. <i>Affine Transform</i>).
     *
     * <p>The properties given in argument follow the same rules as for the
     * {@linkplain DefaultOperationMethod#DefaultOperationMethod(Map, ParameterDescriptorGroup)
     * operation method} constructor. The following table is a reminder of main (not all) properties:</p>
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link DefaultOperationMethod#getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link DefaultOperationMethod#getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link DefaultOperationMethod#getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.operation.OperationMethod#FORMULA_KEY}</td>
     *     <td>{@link Formula}, {@link org.opengis.metadata.citation.Citation} or {@link CharSequence}</td>
     *     <td>{@link DefaultOperationMethod#getFormula()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  set of properties. Shall contain at least {@code "name"}.
     * @param  parameters  description of parameters expected by this operation.
     * @return the operation method created from the given arguments.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultOperationMethod#DefaultOperationMethod(Map, ParameterDescriptorGroup)
     *
     * @since 1.4
     */
    @Override
    public OperationMethod createOperationMethod(final Map<String,?> properties,
            final ParameterDescriptorGroup parameters) throws FactoryException
    {
        final OperationMethod method;
        try {
            method = new DefaultOperationMethod(properties, parameters);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
        }
        return pool.unique(method);
    }

    /**
     * @deprecated The dimensions attributes have been removed in ISO 19111:2019 revision.
     */
    @Deprecated(since="1.4", forRemoval=true)
    public OperationMethod createOperationMethod(final Map<String,?> properties,
            final Integer sourceDimensions, final Integer targetDimensions,
            ParameterDescriptorGroup parameters) throws FactoryException
    {
        return createOperationMethod(properties, parameters);
    }

    /**
     * Creates a defining conversion from the given operation parameters.
     * This conversion has no source and target CRS since those elements are usually unknown at this stage.
     * The source and target CRS will become known later, at the
     * {@linkplain org.apache.sis.referencing.crs.DefaultDerivedCRS Derived CRS} or
     * {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS Projected CRS}
     * construction time.
     *
     * <p>The properties given in argument follow the same rules as for the
     * {@linkplain DefaultConversion#DefaultConversion(Map, CoordinateReferenceSystem, CoordinateReferenceSystem,
     * CoordinateReferenceSystem, OperationMethod, MathTransform) coordinate conversion} constructor.
     * The following table is a reminder of main (not all) properties:</p>
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link DefaultConversion#getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link DefaultConversion#getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.ObjectDomain#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain#getDomainOfValidity()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  method      the operation method.
     * @param  parameters  the parameter values.
     * @return the defining conversion created from the given arguments.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultConversion#DefaultConversion(Map, OperationMethod, MathTransform, ParameterValueGroup)
     */
    @Override
    public Conversion createDefiningConversion(
            final Map<String,?>       properties,
            final OperationMethod     method,
            final ParameterValueGroup parameters) throws FactoryException
    {
        final Conversion conversion;
        try {
            conversion = new DefaultConversion(properties, method, null, parameters);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
        }
        // We do no invoke unique(conversion) because defining conversions are usually short-lived objects.
        return conversion;
    }

    /**
     * Returns {@code true} if the given CRS are using equivalent (ignoring metadata) datum.
     * If the CRS are {@link org.opengis.referencing.crs.CompoundCRS}, then this method verifies that
     * all datum in the target CRS exists in the source CRS, but not necessarily in the same order.
     * The target CRS may have less datum than the source CRS.
     *
     * @param  sourceCRS  the target CRS.
     * @param  targetCRS  the source CRS.
     * @return {@code true} if all datum in the {@code targetCRS} exists in the {@code sourceCRS}.
     */
    private static boolean isConversion(final CoordinateReferenceSystem sourceCRS,
                                        final CoordinateReferenceSystem targetCRS)
    {
        final var components = new ArrayDeque<>(CRS.getSingleComponents(sourceCRS));
next:   for (SingleCRS component : CRS.getSingleComponents(targetCRS)) {
            final Iterator<SingleCRS> it = components.iterator();
            while (it.hasNext()) {
                if (DatumOrEnsemble.ofTarget(component, it.next()).isPresent()) {
                    it.remove();
                    continue next;
                }
            }
            return false;       // Datum from `targetCRS` not found in `sourceCRS`.
        }
        return true;
    }

    /**
     * Creates a transformation or conversion from the given properties.
     * This method infers by itself if the operation to create is a
     * {@link Transformation}, a {@link Conversion} or a {@link Projection}
     * using the {@linkplain DefaultOperationMethod#getOperationType() information provided by the given method}.
     *
     * <p>The properties given in argument follow the same rules as for the
     * {@linkplain AbstractCoordinateOperation#AbstractCoordinateOperation(Map, CoordinateReferenceSystem,
     * CoordinateReferenceSystem, CoordinateReferenceSystem, MathTransform) coordinate operation} constructor.
     * The following table is a reminder of main (not all) properties:</p>
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link DefaultConversion#getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link DefaultConversion#getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.ObjectDomain#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain#getDomainOfValidity()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties        the properties to be given to the identified object.
     * @param  sourceCRS         the source CRS.
     * @param  targetCRS         the target CRS.
     * @param  interpolationCRS  the CRS of additional coordinates needed for the operation, or {@code null} if none.
     * @param  method            the coordinate operation method (mandatory in all cases).
     * @param  transform         transform from positions in the source CRS to positions in the target CRS.
     * @return the coordinate operation created from the given arguments.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultOperationMethod#getOperationType()
     * @see DefaultTransformation
     * @see DefaultConversion
     */
    public SingleOperation createSingleOperation(
            final Map<String,?>             properties,
            final CoordinateReferenceSystem sourceCRS,
            final CoordinateReferenceSystem targetCRS,
            final CoordinateReferenceSystem interpolationCRS,
            final OperationMethod           method,
                  MathTransform             transform) throws FactoryException
    {
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        ArgumentChecks.ensureNonNull("method",    method);
        /*
         * Undocumented (for now) feature: if the `transform` argument is null but parameters are
         * found in the given properties, create the MathTransform instance from those parameters.
         * This is needed for WKT parsing of CoordinateOperation[…] among others.
         */
        if (transform == null) {
            final ParameterValueGroup parameters = Containers.property(properties,
                    CoordinateOperations.PARAMETERS_KEY, ParameterValueGroup.class);
            if (parameters == null) {
                throw new NullPointerException(Errors.format(Errors.Keys.NullArgument_1, "transform"));
            }
            final var builder = new ParameterizedTransformBuilder(getMathTransformFactory(), null);
            builder.setParameters(parameters, false);
            builder.setSourceAxes(sourceCRS);
            builder.setTargetAxes(targetCRS);
            transform = builder.create();
        }
        /*
         * The "operationType" property is currently undocumented. The intent is to help this factory method in
         * situations where the given operation method is not an Apache SIS implementation or does not override
         * getOperationType(), or the method is ambiguous (e.g. "Affine" can be used for both a transformation
         * or a conversion).
         *
         * If we have both a `baseType` and a `Method.getOperationType()`, take the most specific type.
         * An exception will be thrown if the two types are incompatible.
         */
        Class<?> baseType = Containers.property(properties, CoordinateOperations.OPERATION_TYPE_KEY, Class.class);
        if (baseType == null) {
            baseType = SingleOperation.class;
        }
        if (method instanceof DefaultOperationMethod) {
            final Class<? extends SingleOperation> c = ((DefaultOperationMethod) method).getOperationType();
            if (c != null) {                        // Paranoiac check (above method should not return null).
                if (baseType.isAssignableFrom(c)) {
                    baseType = c;
                } else if (!c.isAssignableFrom(baseType)) {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.IncompatiblePropertyValue_1,
                            CoordinateOperations.OPERATION_TYPE_KEY));
                }
            }
        }
        /*
         * If the base type is still abstract (probably because it was not specified neither in the given OperationMethod
         * or in the properties), then try to find a concrete type using the following rules derived from the definitions
         * given in ISO 19111:
         *
         *   - If the two CRS uses the same datum (ignoring metadata), assume that we have a Conversion.
         *   - Otherwise we have a datum change, which implies that we have a Transformation.
         */
        if (baseType == SingleOperation.class) {
            if (isConversion(sourceCRS, targetCRS)) {
                baseType = Conversion.class;
            } else {
                // TODO: handle point motion operation.
                baseType = Transformation.class;
            }
        }
        /*
         * Now create the coordinate operation of the requested type. If we cannot find a concrete class for the
         * requested type, we will instantiate a SingleOperation in last resort. The latter action is a departure
         * from ISO 19111 since `SingleOperation` is conceptually abstract.  But we do that as a way to said that
         * we are missing this important piece of information but still go ahead.
         *
         * It is inconvenient to guarantee that the created operation is an instance of `baseType` since the user
         * could have specified an implementation class or a custom sub-interface. We will perform the type check
         * only after object creation.
         */
        final AbstractSingleOperation op;
        if (Transformation.class.isAssignableFrom(baseType)) {
            op = new DefaultTransformation(properties, sourceCRS, targetCRS, interpolationCRS, method, transform);
        } else if (Conversion.class.isAssignableFrom(baseType)) {
            op = new DefaultConversion(properties, sourceCRS, targetCRS, interpolationCRS, method, transform);
        } else {  // See above comment about this last-resort fallback.
            op = new AbstractSingleOperation(properties, sourceCRS, targetCRS, interpolationCRS, method, transform);
        }
        if (!baseType.isInstance(op)) {
            throw new FactoryException(Resources.format(Resources.Keys.CanNotCreateObjectAsInstanceOf_2, baseType, op.getName()));
        }
        return pool.unique(op);
    }

    /**
     * Creates an ordered sequence of two or more single coordinate operations.
     *
     * @deprecated Replaced by {@linkplain #createConcatenatedOperation(Map, CoordinateReferenceSystem,
     * CoordinateReferenceSystem, CoordinateOperation...) a method with explicit CRS arguments} because
     * of potential <abbr>CRS</abbr> swapping.
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  operations  the sequence of operations. Shall contain at least two operations.
     * @return the concatenated operation created from the given arguments.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    @Deprecated(since="1.5", forRemoval=true)
    public CoordinateOperation createConcatenatedOperation(final Map<String,?> properties,
            final CoordinateOperation... operations) throws FactoryException
    {
        return createConcatenatedOperation(properties, null, null, operations);
    }

    /**
     * Creates an ordered sequence of two or more single coordinate operations.
     * The sequence of operations is constrained by the requirement that the source coordinate reference system
     * of step (<var>n</var>+1) must be the same as the target coordinate reference system of step (<var>n</var>).
     * The source coordinate reference system of the first step and the target coordinate reference system of the
     * last step are the source and target coordinate reference system associated with the concatenated operation.
     *
     * <p>As an exception to the above-cited constraint, a step can swap its source and target <abbr>CRS</abbr>.
     * In such case, the effectively executed operation will be the inverse of that step. The {@code sourceCRS}
     * and {@code targetCRS} arguments of this method are needed for detecting whether such swapping occurred
     * in the first step or in the last step. Those optional arguments can be {@code null} if the caller did
     * not swapped any <abbr>CRS</abbr>.</p>
     *
     * <p>The properties given in argument follow the same rules as for any other
     * {@linkplain AbstractCoordinateOperation#AbstractCoordinateOperation(Map, CoordinateReferenceSystem,
     * CoordinateReferenceSystem, CoordinateReferenceSystem, MathTransform) coordinate operation} constructor.
     * The following table is a reminder of main (not all) properties:</p>
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link AbstractCoordinateOperation#getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link AbstractCoordinateOperation#getIdentifiers()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  sourceCRS   the source <abbr>CRS</abbr>, or {@code null} for the source of the first step.
     * @param  targetCRS   the target <abbr>CRS</abbr>, or {@code null} for the target of the last effective step.
     * @param  operations  the sequence of operations. Shall contain at least two operations.
     * @return the concatenated operation created from the given arguments.
     * @throws FactoryException if the object creation failed.
     *
     * @since 1.5
     */
    public CoordinateOperation createConcatenatedOperation(
            final Map<String,?> properties,
            final CoordinateReferenceSystem sourceCRS,
            final CoordinateReferenceSystem targetCRS,
            final CoordinateOperation... operations) throws FactoryException
    {
        /*
         * If the user specified a single operation, there is no need to create a ConcatenatedOperation;
         * the operation to return will be the specified one. The metadata given in argument are ignored
         * on the assumption that the single operation has more complete metadata (in particular an EPSG
         * code, in which case we do not want to modify any other metadata in order to stay compliant
         * with EPSG definition).
         */
        if (operations.length == 1 && sourceCRS == null && targetCRS == null) {
            return operations[0];
        }
        final var op = new DefaultConcatenatedOperation(properties, sourceCRS, targetCRS, operations, getMathTransformFactory());
        /*
         * Verifies again the number of single operations.  We may have a singleton if some operations
         * were omitted because their associated math transform were identity. This happen for example
         * if a "Geographic 3D to 2D conversion" has been redimensioned to a "3D to 3D" operation.
         */
        final List<? extends CoordinateOperation> co = op.getOperations();
        if (co.size() != 1) {
            return pool.unique(op);
        }
        final CoordinateOperation single = co.get(0);
        if (Objects.equals(single.getSourceCRS(), op.getSourceCRS()) &&
            Objects.equals(single.getTargetCRS(), op.getTargetCRS()))
        {
            // Verify only if CRS are equal because otherwise, `op` transform may be the inverse.
            assert single.getMathTransform().equals(op.getMathTransform()) : op;
        } else {
            /*
             * The CRS of the single operation may be different than the CRS of the concatenated operation
             * for two reasons: optimization when the first or the last operation was an identity operation,
             * or when the operation to apply is the inverse of the single operation (swapped source/target).
             *
             * The first case (optimization) happens, for example, if the sole purpose of an operation step was
             * to change the longitude range from [-180 … +180]° to [0 … 360]°. In such case, the `MathTransform`
             * is identity (because Apache SIS does not handle those changes in `MathTransform`; we handle that
             * elsewhere, for example in the Envelopes utility class), but omitting the transform should not
             * cause the lost of the original CRS with the desired longitude range.
             */
            if (single instanceof SingleOperation) {
                final Map<String,Object> merge = new HashMap<>(
                        IdentifiedObjects.getProperties(single, CoordinateOperation.IDENTIFIERS_KEY));
                merge.put(CoordinateOperations.PARAMETERS_KEY, ((SingleOperation) single).getParameterValues());
                if (single instanceof AbstractIdentifiedObject) {
                    merge.put(CoordinateOperations.OPERATION_TYPE_KEY, ((AbstractIdentifiedObject) single).getInterface());
                }
                merge.putAll(properties);
                return createSingleOperation(merge, op.getSourceCRS(), op.getTargetCRS(),
                        op.getInterpolationCRS().orElse(null),
                        ((SingleOperation) single).getMethod(), op.getMathTransform());
            }
        }
        return single;
    }

    /**
     * Finds or creates an operation for conversion or transformation between two coordinate reference systems.
     * If an operation exists, it is returned. If more than one operation exists, then the operation having the
     * widest intersection between its {@linkplain org.apache.sis.referencing.DefaultObjectDomain#getDomainOfValidity()
     * domain of validity} and the {@linkplain CoordinateOperationContext#getAreaOfInterest() area of interest}
     * is returned.
     *
     * <p>The default implementation performs the following steps:</p>
     * <ul>
     *   <li>If a coordinate operation has been previously cached for the given CRS and context, return it.</li>
     *   <li>Otherwise:
     *     <ol>
     *       <li>Invoke {@link #createOperationFinder(CoordinateOperationAuthorityFactory, CoordinateOperationContext)}.</li>
     *       <li>Invoke {@link CoordinateOperationFinder#createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem)}
     *           on the object returned by the previous step.</li>
     *       <li>Cache the result, then return it.</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * Subclasses can override {@link #createOperationFinder createOperationFinder(…)} if they need more control on
     * the way coordinate operations are inferred.
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @param  context    area of interest and desired accuracy, or {@code null}.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws OperationNotFoundException if no operation path was found from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation creation failed for some other reason.
     *
     * @see CoordinateOperationFinder
     *
     * @since 0.7
     */
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS,
                                               final CoordinateOperationContext context)
            throws OperationNotFoundException, FactoryException
    {
        final Cache.Handler<CoordinateOperation> handler;
        CoordinateOperation op;
        if (context == null) {
            final CRSPair key = new CRSPair(sourceCRS, targetCRS);
            op = cache.peek(key);
            if (op != null) {
                return op;
            }
            handler = cache.lock(key);
        } else {
            // We currently do not cache the operation when the result may depend on the context (see `this.cache` javadoc).
            handler = null;
            op = null;
        }
        boolean canStoreInCache = true;
        try {
            if (handler == null || (op = handler.peek()) == null) {
                final CoordinateOperationFinder finder = createOperationFinder(getFactorySIS(), context);
                op = finder.createOperation(sourceCRS, targetCRS);
                canStoreInCache = finder.canStoreInCache();
            }
        } finally {
            if (handler != null) {
                handler.putAndUnlock(canStoreInCache ? op : null);
            }
        }
        return op;
    }

    /**
     * Finds or creates operations for conversions or transformations between two coordinate reference systems.
     * If at least one operation exists, they are returned in preference order: the operation having the widest
     * intersection between its {@linkplain org.apache.sis.referencing.DefaultObjectDomain#getDomainOfValidity()
     * domain of validity} and the {@linkplain CoordinateOperationContext#getAreaOfInterest() area of interest}
     * is returned.
     *
     * <p>The default implementation performs the following steps:</p>
     * <ul>
     *   <li>Invoke {@link #createOperationFinder(CoordinateOperationAuthorityFactory, CoordinateOperationContext)}.</li>
     *   <li>Invoke {@link CoordinateOperationFinder#createOperations(CoordinateReferenceSystem, CoordinateReferenceSystem)}
     *       on the object returned by the previous step.</li>
     * </ul>
     *
     * Subclasses can override {@link #createOperationFinder createOperationFinder(…)} if they need more control on
     * the way coordinate operations are inferred.
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @param  context    area of interest and desired accuracy, or {@code null}.
     * @return coordinate operations from {@code sourceCRS} to {@code targetCRS}.
     * @throws OperationNotFoundException if no operation path was found from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation creation failed for some other reason.
     *
     * @see CoordinateOperationFinder
     *
     * @since 1.0
     */
    public List<CoordinateOperation> createOperations(final CoordinateReferenceSystem sourceCRS,
                                                      final CoordinateReferenceSystem targetCRS,
                                                      final CoordinateOperationContext context)
            throws OperationNotFoundException, FactoryException
    {
        return createOperationFinder(getFactorySIS(), context).createOperations(sourceCRS, targetCRS);
    }

    /**
     * Returns the Apache <abbr>SIS</abbr> implementation of the <abbr>EPSG</abbr> factory, or {@code null} if none.
     */
    private static CoordinateOperationAuthorityFactory getFactorySIS() throws FactoryException {
        if (USE_EPSG_FACTORY) {
            AuthorityFactory registry = CRS.getAuthorityFactory(Constants.EPSG);
            if (registry instanceof CoordinateOperationAuthorityFactory) {
                return (CoordinateOperationAuthorityFactory) registry;
            }
        }
        return null;
    }

    /**
     * Creates the object which will perform the actual task of finding a coordinate operation path between two CRS.
     * This method is invoked by {@link #createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem,
     * CoordinateOperationContext) createOperation(…)} when no operation was found in the cache.
     * The default implementation is straightforward:
     *
     * {@snippet lang="java" :
     *     return new CoordinateOperationFinder(registry, this, context);
     *     }
     *
     * Subclasses can override this method is they want to modify the way coordinate operations are inferred.
     *
     * @param  registry  the factory to use for creating operations as defined by authority, or {@code null} if none.
     * @param  context   the area of interest and desired accuracy, or {@code null} if none.
     * @return a finder of conversion or transformation path from a source CRS to a target CRS.
     * @throws FactoryException if an error occurred while initializing the {@code CoordinateOperationFinder}.
     *
     * @since 0.8
     */
    protected CoordinateOperationFinder createOperationFinder(
            final CoordinateOperationAuthorityFactory registry,
            final CoordinateOperationContext context) throws FactoryException
    {
        return new CoordinateOperationFinder(registry, this, context);
    }
}
