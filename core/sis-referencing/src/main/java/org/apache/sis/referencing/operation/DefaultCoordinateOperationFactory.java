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
import java.util.Collections;
import java.util.List;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.datum.Datum;
import org.apache.sis.internal.referencing.MergedProperties;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.Utilities;


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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
public class DefaultCoordinateOperationFactory extends AbstractFactory implements CoordinateOperationFactory {
    /**
     * Whether this class is allowed to use the EPSG authority factory for searching coordinate operation paths.
     * This flag should always be {@code true}, except temporarily for testing purposes.
     */
    static final boolean USE_EPSG_FACTORY = true;

    /**
     * The default properties, or an empty map if none. This map shall not change after construction in
     * order to allow usage without synchronization in multi-thread context. But we do not need to wrap
     * in a unmodifiable map since {@code DefaultCoordinateOperationFactory} does not provide public
     * access to it.
     */
    private final Map<String,?> defaultProperties;

    /**
     * The factory to use if {@link CoordinateOperationFinder} needs to create CRS for intermediate steps.
     * Will be created only when first needed.
     *
     * @see #getCRSFactory()
     */
    private volatile CRSFactory crsFactory;

    /**
     * The factory to use if {@link CoordinateOperationFinder} needs to create CS for intermediate steps.
     * Will be created only when first needed.
     *
     * @see #getCSFactory()
     */
    private volatile CSFactory csFactory;

    /**
     * The math transform factory. Will be created only when first needed.
     *
     * @see #getMathTransformFactory()
     */
    private volatile MathTransformFactory mtFactory;

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
     * Constructs a factory with no default properties.
     */
    public DefaultCoordinateOperationFactory() {
        this(null, null);
    }

    /**
     * Constructs a factory with the given default properties.
     * {@code DefaultCoordinateOperationFactory} will fallback on the map given to this constructor
     * for any property not present in the map provided to a {@code createFoo(Map<String,?>, …)} method.
     *
     * @param properties the default properties, or {@code null} if none.
     * @param factory the factory to use for creating
     *        {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform math transforms},
     *        or {@code null} for the default factory.
     */
    public DefaultCoordinateOperationFactory(Map<String,?> properties, final MathTransformFactory factory) {
        if (properties == null || properties.isEmpty()) {
            properties = Collections.emptyMap();
        } else {
            String key   = null;
            Object value = null;
            properties   = new HashMap<String,Object>(properties);
            /*
             * Following use of properties is an undocumented feature for now. Current version documents only
             * MathTransformFactory because math transforms are intimately related to coordinate operations.
             */
            try {
                crsFactory = (CRSFactory)           (value = properties.remove(key = ReferencingServices.CRS_FACTORY));
                csFactory  = (CSFactory)            (value = properties.remove(key = ReferencingServices.CS_FACTORY));
                mtFactory  = (MathTransformFactory) (value = properties.remove(key = ReferencingServices.MT_FACTORY));
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(Errors.getResources(properties)
                        .getString(Errors.Keys.IllegalPropertyValueClass_2, key, Classes.getClass(value)));
            }
            properties = CollectionsExt.compact(properties);
        }
        defaultProperties = properties;
        if (factory != null) {
            mtFactory = factory;
        }
        pool = new WeakHashSet<IdentifiedObject>(IdentifiedObject.class);
        cache = new Cache<CRSPair,CoordinateOperation>(12, 50, true);
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
     * @param  properties The user-supplied properties.
     * @return The union of the given properties with the default properties.
     */
    protected Map<String,?> complete(final Map<String,?> properties) {
        ArgumentChecks.ensureNonNull("properties", properties);
        return new MergedProperties(properties, defaultProperties);
    }

    /**
     * Returns the factory to use if {@link CoordinateOperationFinder} needs to create CRS for intermediate steps.
     */
    final CRSFactory getCRSFactory() {
        CRSFactory factory = crsFactory;
        if (factory == null) {
            crsFactory = factory = DefaultFactories.forBuildin(CRSFactory.class);
        }
        return factory;
    }

    /**
     * Returns the factory to use if {@link CoordinateOperationFinder} needs to create CS for intermediate steps.
     */
    final CSFactory getCSFactory() {
        CSFactory factory = csFactory;
        if (factory == null) {
            csFactory = factory = DefaultFactories.forBuildin(CSFactory.class);
        }
        return factory;
    }

    /**
     * Returns the underlying math transform factory. This factory is used for constructing {@link MathTransform}
     * dependencies for all {@linkplain AbstractCoordinateOperation coordinate operations} instances.
     *
     * @return The underlying math transform factory.
     */
    final MathTransformFactory getMathTransformFactory() {
        MathTransformFactory factory = mtFactory;
        if (factory == null) {
            mtFactory = factory = DefaultFactories.forBuildin(MathTransformFactory.class);
        }
        return factory;
    }

    /**
     * Returns the Apache SIS implementation of math transform factory.
     * This method is used only when we need SIS-specific methods.
     */
    final DefaultMathTransformFactory getDefaultMathTransformFactory() {
        MathTransformFactory factory = getMathTransformFactory();
        if (factory instanceof DefaultMathTransformFactory) {
            return (DefaultMathTransformFactory) factory;
        }
        return DefaultFactories.forBuildin(MathTransformFactory.class, DefaultMathTransformFactory.class);
    }

    /**
     * Returns the operation method of the given name. The given argument shall be either a method
     * {@linkplain DefaultOperationMethod#getName() name} (e.g. <cite>"Transverse Mercator"</cite>)
     * or one of its {@linkplain DefaultOperationMethod#getIdentifiers() identifiers} (e.g. {@code "EPSG:9807"}).
     * The search is case-insensitive and comparisons against method names can be
     * {@linkplain DefaultOperationMethod#isHeuristicMatchForName(String) heuristic}.
     *
     * <p>If more than one method match the given name, then the first (according iteration order)
     * non-{@linkplain org.apache.sis.util.Deprecable#isDeprecated() deprecated} matching method is returned.
     * If all matching methods are deprecated, the first one is returned.</p>
     *
     * @param  name The name of the operation method to fetch.
     * @return The operation method of the given name.
     * @throws FactoryException if the requested operation method can not be fetched.
     *
     * @see DefaultMathTransformFactory#getOperationMethod(String)
     */
    public OperationMethod getOperationMethod(String name) throws FactoryException {
        name = CharSequences.trimWhitespaces(name);
        ArgumentChecks.ensureNonEmpty("name", name);
        final MathTransformFactory mtFactory = getMathTransformFactory();
        if (mtFactory instanceof DefaultMathTransformFactory) {
            return ((DefaultMathTransformFactory) mtFactory).getOperationMethod(name);
        }
        final OperationMethod method = ReferencingServices.getInstance().getOperationMethod(
                mtFactory.getAvailableMethods(SingleOperation.class), name);
        if (method != null) {
            return method;
        }
        throw new NoSuchIdentifierException(Errors.getResources(defaultProperties)
                .getString(Errors.Keys.NoSuchOperationMethod_1, name), name);
    }

    /**
     * Creates an operation method from a set of properties and a descriptor group.
     * The source and target dimensions may be {@code null} if the method can work
     * with any number of dimensions (e.g. <cite>Affine Transform</cite>).
     *
     * <p>The properties given in argument follow the same rules than for the
     * {@linkplain DefaultOperationMethod#DefaultOperationMethod(Map, Integer, Integer, ParameterDescriptorGroup)
     * operation method} constructor. The following table is a reminder of main (not all) properties:</p>
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link DefaultOperationMethod#getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link DefaultOperationMethod#getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link DefaultOperationMethod#getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.operation.OperationMethod#FORMULA_KEY}</td>
     *     <td>{@link Formula}, {@link Citation} or {@link CharSequence}</td>
     *     <td>{@link DefaultOperationMethod#getFormula()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties       Set of properties. Shall contain at least {@code "name"}.
     * @param  sourceDimensions Number of dimensions in the source CRS of this operation method, or {@code null}.
     * @param  targetDimensions Number of dimensions in the target CRS of this operation method, or {@code null}.
     * @param  parameters       Description of parameters expected by this operation.
     * @return The operation method created from the given arguments.
     * @throws FactoryException if the object creation failed.
     *
     * @see DefaultOperationMethod#DefaultOperationMethod(Map, Integer, Integer, ParameterDescriptorGroup)
     */
    public OperationMethod createOperationMethod(final Map<String,?> properties,
            final Integer sourceDimensions, final Integer targetDimensions,
            ParameterDescriptorGroup parameters) throws FactoryException
    {
        final OperationMethod method;
        try {
            method = new DefaultOperationMethod(properties, sourceDimensions, targetDimensions, parameters);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
        }
        return pool.unique(method);
    }

    /**
     * Creates a defining conversion from the given operation parameters.
     * This conversion has no source and target CRS since those elements are usually unknown at this stage.
     * The source and target CRS will become known later, at the
     * {@linkplain org.apache.sis.referencing.crs.DefaultDerivedCRS Derived CRS} or
     * {@linkplain org.apache.sis.referencing.crs.DefaultProjectedCRS Projected CRS}
     * construction time.
     *
     * <p>The properties given in argument follow the same rules than for the
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
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link DefaultConversion#getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link DefaultConversion#getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.operation.CoordinateOperation#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link DefaultConversion#getDomainOfValidity()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties The properties to be given to the identified object.
     * @param  method     The operation method.
     * @param  parameters The parameter values.
     * @return The defining conversion created from the given arguments.
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
     * If the CRS are {@link CompoundCRS}, then this method verifies that all datum in the
     * target CRS exists in the source CRS, but not necessarily in the same order.
     * The target CRS may have less datum than the source CRS.
     *
     * @param sourceCRS The target CRS.
     * @param targetCRS The source CRS.
     * @return {@code true} if all datum in the {@code targetCRS} exists in the {@code sourceCRS}.
     */
    private static boolean isConversion(final CoordinateReferenceSystem sourceCRS,
                                        final CoordinateReferenceSystem targetCRS)
    {
        List<SingleCRS> components = CRS.getSingleComponents(sourceCRS);
        int n = components.size();                      // Number of remaining datum from sourceCRS to verify.
        final Datum[] datum = new Datum[n];
        for (int i=0; i<n; i++) {
            datum[i] = components.get(i).getDatum();
        }
        components = CRS.getSingleComponents(targetCRS);
next:   for (int i=components.size(); --i >= 0;) {
            final Datum d = components.get(i).getDatum();
            for (int j=n; --j >= 0;) {
                if (Utilities.equalsIgnoreMetadata(d, datum[j])) {
                    System.arraycopy(datum, j+1, datum, j, --n - j);  // Remove the datum from the list.
                    continue next;
                }
            }
            return false;                               // Datum from 'targetCRS' not found in 'sourceCRS'.
        }
        return true;
    }

    /**
     * Creates a transformation or conversion from the given properties.
     * This method infers by itself if the operation to create is a
     * {@link Transformation}, a {@link Conversion} or a {@link Projection} sub-type
     * ({@link CylindricalProjection}, {@link ConicProjection} or {@link PlanarProjection})
     * using the {@linkplain DefaultOperationMethod#getOperationType() information provided by the given method}.
     *
     * <p>The properties given in argument follow the same rules than for the
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
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link DefaultConversion#getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link DefaultConversion#getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.operation.CoordinateOperation#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link DefaultConversion#getDomainOfValidity()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties The properties to be given to the identified object.
     * @param  sourceCRS  The source CRS.
     * @param  targetCRS  The target CRS.
     * @param  interpolationCRS The CRS of additional coordinates needed for the operation, or {@code null} if none.
     * @param  method     The coordinate operation method (mandatory in all cases).
     * @param  transform  Transform from positions in the source CRS to positions in the target CRS.
     * @return The coordinate operation created from the given arguments.
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
         * Undocumented (for now) feature: if the 'transform' argument is null but parameters are
         * found in the given properties, create the MathTransform instance from those parameters.
         * This is needed for WKT parsing of CoordinateOperation[…] among others.
         */
        if (transform == null) {
            final ParameterValueGroup parameters = Containers.property(properties,
                    ReferencingServices.PARAMETERS_KEY, ParameterValueGroup.class);
            if (parameters == null) {
                throw new NullArgumentException(Errors.format(Errors.Keys.NullArgument_1, "transform"));
            }
            transform = mtFactory.createBaseToDerived(sourceCRS, parameters, targetCRS.getCoordinateSystem());
        }
        /*
         * The "operationType" property is currently undocumented. The intend is to help this factory method in
         * situations where the given operation method is not an Apache SIS implementation or does not override
         * getOperationType(), or the method is ambiguous (e.g. "Affine" can be used for both a transformation
         * or a conversion).
         *
         * If we have both a 'baseType' and a Method.getOperationType(), take the most specific type.
         * An exception will be thrown if the two types are incompatible.
         */
        Class<?> baseType = Containers.property(properties, ReferencingServices.OPERATION_TYPE_KEY, Class.class);
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
                            ReferencingServices.OPERATION_TYPE_KEY));
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
         *
         * In the case of Conversion, we can specialize one step more if the conversion is going from a geographic CRS
         * to a projected CRS. It may seems that we should check if ProjectedCRS.getBaseCRS() is equals (ignoring meta
         * data) to source CRS. But we already checked the datum, which is the important part. The axis order and unit
         * could be different, which we want to allow.
         */
        if (baseType == SingleOperation.class) {
            if (isConversion(sourceCRS, targetCRS)) {
                if (interpolationCRS == null && sourceCRS instanceof GeographicCRS
                                             && targetCRS instanceof ProjectedCRS)
                {
                    baseType = Projection.class;
                } else {
                    baseType = Conversion.class;
                }
            } else {
                baseType = Transformation.class;
            }
        }
        /*
         * Now create the coordinate operation of the requested type. If we can not find a concrete class for the
         * requested type, we will instantiate a SingleOperation in last resort.  The later action is a departure
         * from ISO 19111 since 'SingleOperation' is conceptually abstract.  But we do that as a way to said that
         * we are missing this important piece of information but still go ahead.
         *
         * It is unconvenient to guarantee that the created operation is an instance of 'baseType' since the user
         * could have specified an implementation class or a custom sub-interface. We will perform the type check
         * only after object creation.
         */
        final AbstractSingleOperation op;
        if (Transformation.class.isAssignableFrom(baseType)) {
            op = new DefaultTransformation(properties, sourceCRS, targetCRS, interpolationCRS, method, transform);
        } else if (Projection.class.isAssignableFrom(baseType)) {
            ArgumentChecks.ensureCanCast("sourceCRS", GeographicCRS.class, sourceCRS);
            ArgumentChecks.ensureCanCast("targetCRS", ProjectedCRS .class, targetCRS);
            if (interpolationCRS != null) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.ForbiddenAttribute_2, "interpolationCRS", baseType));
            }
            final GeographicCRS baseCRS = (GeographicCRS) sourceCRS;
            final ProjectedCRS  crs     =  (ProjectedCRS) targetCRS;
            if (CylindricalProjection.class.isAssignableFrom(baseType)) {
                op = new DefaultCylindricalProjection(properties, baseCRS, crs, method, transform);
            } else if (ConicProjection.class.isAssignableFrom(baseType)) {
                op = new DefaultConicProjection(properties, baseCRS, crs, method, transform);
            } else if (PlanarProjection.class.isAssignableFrom(baseType)) {
                op = new DefaultPlanarProjection(properties, baseCRS, crs, method, transform);
            } else {
                op = new DefaultProjection(properties, baseCRS, crs, method, transform);
            }
        } else if (Conversion.class.isAssignableFrom(baseType)) {
            op = new DefaultConversion(properties, sourceCRS, targetCRS, interpolationCRS, method, transform);
        } else {  // See above comment about this last-resort fallback.
            op = new AbstractSingleOperation(properties, sourceCRS, targetCRS, interpolationCRS, method, transform);
        }
        if (!baseType.isInstance(op)) {
            throw new FactoryException(Errors.format(Errors.Keys.CanNotCreateObjectAsInstanceOf_2, baseType, op.getName()));
        }
        return pool.unique(op);
    }

    /**
     * Creates an ordered sequence of two or more single coordinate operations.
     * The sequence of operations is constrained by the requirement that the source coordinate reference system
     * of step (<var>n</var>+1) must be the same as the target coordinate reference system of step (<var>n</var>).
     * The source coordinate reference system of the first step and the target coordinate reference system of the
     * last step are the source and target coordinate reference system associated with the concatenated operation.
     *
     * <p>The properties given in argument follow the same rules than for any other
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
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link AbstractCoordinateOperation#getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link AbstractCoordinateOperation#getIdentifiers()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties The properties to be given to the identified object.
     * @param  operations The sequence of operations. Shall contains at least two operations.
     * @return The concatenated operation created from the given arguments.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateOperation createConcatenatedOperation(final Map<String,?> properties,
            final CoordinateOperation... operations) throws FactoryException
    {
        if (operations != null && operations.length == 1) {
            return operations[0];
        }
        final CoordinateOperation op;
        try {
            op = new DefaultConcatenatedOperation(properties, operations, getMathTransformFactory());
        } catch (IllegalArgumentException exception) {
            throw new InvalidGeodeticParameterException(exception.getLocalizedMessage(), exception);
        }
        return pool.unique(op);
    }

    /**
     * Finds or creates an operation for conversion or transformation between two coordinate reference systems.
     * If an operation exists, it is returned. If more than one operation exists, the operation having the widest
     * domain of validity is returned. If no operation exists, then an exception is thrown.
     *
     * <p>The default implementation delegates to <code>{@linkplain #createOperation(CoordinateReferenceSystem,
     * CoordinateReferenceSystem, CoordinateOperationContext) createOperation}(sourceCRS, targetCRS, null)}</code>.</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws OperationNotFoundException if no operation path was found from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation creation failed for some other reason.
     */
    @Override
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS)
            throws OperationNotFoundException, FactoryException
    {
        return createOperation(sourceCRS, targetCRS, (CoordinateOperationContext) null);
    }

    /**
     * Finds or creates an operation for conversion or transformation between two coordinate reference systems.
     * If an operation exists, it is returned. If more than one operation exists, then the operation having the
     * widest intersection between its {@linkplain AbstractCoordinateOperation#getDomainOfValidity() domain of
     * validity} and the {@linkplain CoordinateOperationContext#getAreaOfInterest() area of interest} is returned.
     *
     * <p>The default implementation is equivalent to the following code
     * (omitting the {@code registry} type check and cast for brevity):</p>
     *
     * {@preformat java
     *   CoordinateOperationAuthorityFactory registry = CRS.getAuthorityFactory("EPSG");    // Actually needs cast
     *   return new CoordinateOperationFinder(registry, this, context).createOperation(sourceCRS, targetCRS);
     * }
     *
     * Subclasses can override this method if they need, for example, to use a custom
     * {@link CoordinateOperationFinder} implementation.
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
            // We currently do not cache the operation when the result may depend on the context (see 'this.cache' javadoc).
            handler = null;
            op = null;
        }
        try {
            if (handler == null || (op = handler.peek()) == null) {
                final AuthorityFactory registry = USE_EPSG_FACTORY ? CRS.getAuthorityFactory(Constants.EPSG) : null;
                op = new CoordinateOperationFinder((registry instanceof CoordinateOperationAuthorityFactory) ?
                        (CoordinateOperationAuthorityFactory) registry : null, this, context).createOperation(sourceCRS, targetCRS);
            }
        } finally {
            if (handler != null) {
                handler.putAndUnlock(op);
            }
        }
        return op;
    }

    /**
     * Returns an operation using a particular method for conversion or transformation between
     * two coordinate reference systems. If an operation exists using the given method, then it
     * is returned. If no operation using the given method is found, then the implementation has
     * the option of inferring the operation from the argument objects.
     *
     * <p>Current implementation ignores the {@code method} argument.
     * This behavior may change in a future Apache SIS version.</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @param  method     the algorithmic method for conversion or transformation.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws OperationNotFoundException if no operation path was found from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation creation failed for some other reason.
     *
     * @deprecated Replaced by {@link #createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, CoordinateOperationContext)}.
     */
    @Override
    @Deprecated
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS,
                                               final OperationMethod method)
            throws FactoryException
    {
        ArgumentChecks.ensureNonNull("method", method);     // As a matter of principle.
        return createOperation(sourceCRS, targetCRS, (CoordinateOperationContext) null);
    }
}
