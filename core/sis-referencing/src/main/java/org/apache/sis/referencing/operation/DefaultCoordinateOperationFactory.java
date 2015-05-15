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
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.referencing.MergedProperties;
import org.apache.sis.internal.referencing.OperationMethods;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;


/**
 * Creates {@linkplain AbstractCoordinateOperation coordinate operations}.
 * This factory is capable to find coordinate {@linkplain DefaultConversion conversions}
 * or {@linkplain DefaultTransformation transformations} between two
 * {@linkplain org.apache.sis.referencing.crs.AbstractCRS coordinate reference systems}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public class DefaultCoordinateOperationFactory extends AbstractFactory implements CoordinateOperationFactory {
    /**
     * The default properties, or an empty map if none. This map shall not change after construction in
     * order to allow usage without synchronization in multi-thread context. But we do not need to wrap
     * in a unmodifiable map since {@code DefaultCoordinateOperationFactory} does not provide public
     * access to it.
     */
    private final Map<String,?> defaultProperties;

    /**
     * The math transform factory. Will be created only when first needed.
     *
     * @see #getMathTransformFactory()
     */
    private volatile MathTransformFactory mtFactory;

    /**
     * Weak references to existing objects.
     * This set is used in order to return a pre-existing object instead of creating a new one.
     */
    private final WeakHashSet<IdentifiedObject> pool;

    /**
     * Constructs a factory with no default properties.
     */
    public DefaultCoordinateOperationFactory() {
        this(null);
    }

    /**
     * Constructs a factory with the given default properties.
     * {@code DefaultCoordinateOperationFactory} will fallback on the map given to this constructor
     * for any property not present in the map provided to a {@code createFoo(Map<String,?>, …)} method.
     *
     * @param properties The default properties, or {@code null} if none.
     */
    public DefaultCoordinateOperationFactory(Map<String,?> properties) {
        if (properties == null || properties.isEmpty()) {
            properties = Collections.emptyMap();
        } else {
            properties = CollectionsExt.compact(new HashMap<String,Object>(properties));
        }
        defaultProperties = properties;
        mtFactory = Containers.property(properties, OperationMethods.MT_FACTORY, MathTransformFactory.class);
        pool = new WeakHashSet<>(IdentifiedObject.class);
    }

    /**
     * Returns the union of the given {@code properties} map with the default properties
     * given at {@linkplain #DefaultCoordinateOperationFactory(Map) construction time}.
     * Entries in the given properties map have precedence, even if their
     * {@linkplain java.util.Map.Entry#getValue() value} is {@code null}
     * (i.e. a null value "erase" the default property value).
     * Entries with null value after the union will be omitted.
     *
     * <p>This method is invoked by all {@code createFoo(Map<String,?>, …)} methods.</p>
     *
     * @param  properties The user-supplied properties.
     * @return The union of the given properties with the default properties.
     */
    protected Map<String,?> complete(final Map<String,?> properties) {
        ArgumentChecks.ensureNonNull("properties", properties);
        return new MergedProperties(properties, defaultProperties);
    }

    /**
     * Returns the underlying math transform factory. This factory is used for constructing {@link MathTransform}
     * dependencies for all {@linkplain AbstractCoordinateOperation coordinate operations} instances.
     *
     * @return The underlying math transform factory.
     */
    private MathTransformFactory getMathTransformFactory() {
        MathTransformFactory factory = mtFactory;
        if (factory == null) {
            mtFactory = factory = DefaultFactories.forBuildin(MathTransformFactory.class);
        }
        return factory;
    }

    /**
     * Returns the operation method of the given name.
     *
     * @param  name The name of the operation method to fetch.
     * @return The operation method of the given name.
     * @throws FactoryException if the requested operation method can not be fetched.
     *
     * @see DefaultMathTransformFactory#getOperationMethod(String)
     */
    @Override
    public OperationMethod getOperationMethod(final String name) throws FactoryException {
        final MathTransformFactory mtFactory = getMathTransformFactory();
        if (mtFactory instanceof DefaultMathTransformFactory) {
            return ((DefaultMathTransformFactory) mtFactory).getOperationMethod(name);
        }
        for (final OperationMethod method : mtFactory.getAvailableMethods(SingleOperation.class)) {
            if (IdentifiedObjects.isHeuristicMatchForName(method, name)) {
                return method;
            }
        }
        throw new NoSuchIdentifierException(Errors.getResources(defaultProperties)
                .getString(Errors.Keys.NoSuchOperationMethod_1, name), name);
    }

    /**
     * Constructs a defining conversion.
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
            throw new FactoryException(exception);
        }
        return pool.unique(conversion);
    }

    /**
     * Creates an operation method from a set of properties and a descriptor group.
     * The source and target dimensions may be {@code null} if the method can work
     * with any number of dimensions (e.g. <cite>Affine Transform</cite>).
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
    @Override
    public OperationMethod createOperationMethod(final Map<String,?> properties,
            final Integer sourceDimensions, final Integer targetDimensions,
            ParameterDescriptorGroup parameters) throws FactoryException
    {
        final OperationMethod method;
        try {
            method = new DefaultOperationMethod(properties, sourceDimensions, targetDimensions, parameters);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(method);
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
     * CoordinateReferenceSystem, CoordinateReferenceSystem, MathTransform) coordinate operation}.
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
        final CoordinateOperation op;
        try {
            op = new DefaultConcatenatedOperation(properties, operations, getMathTransformFactory());
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        return pool.unique(op);
    }

    /**
     * Not yet implemented.
     *
     * @param  sourceCRS Input coordinate reference system.
     * @param  targetCRS Output coordinate reference system.
     * @param  method the algorithmic method for conversion or transformation.
     * @return A coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation creation failed.
     */
    @Override
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS,
                                               final OperationMethod method)
            throws FactoryException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Not yet implemented.
     *
     * @param  sourceCRS Input coordinate reference system.
     * @param  targetCRS Output coordinate reference system.
     * @return A coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws OperationNotFoundException if no operation path was found from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation creation failed for some other reason.
     */
    @Override
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS)
            throws OperationNotFoundException, FactoryException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
