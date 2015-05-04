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
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.opengis.util.FactoryException;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.Utilities.deepEquals;

// Branch-dependent imports
import java.util.Objects;


/**
 * An ordered sequence of two or more single coordinate operations. The sequence of operations is constrained
 * by the requirement that the source coordinate reference system of step (<var>n</var>+1) must be the same as
 * the target coordinate reference system of step (<var>n</var>). The source coordinate reference system of the
 * first step and the target coordinate reference system of the last step are the source and target coordinate
 * reference system associated with the concatenated operation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public class DefaultConcatenatedOperation extends AbstractCoordinateOperation implements ConcatenatedOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4199619838029045700L;

    /**
     * The sequence of operations.
     */
    private final List<SingleOperation> operations;

    /**
     * Constructs a concatenated operation from a set of properties and a
     * {@linkplain MathTransformFactory math transform factory}.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractCoordinateOperation#AbstractCoordinateOperation(Map, CoordinateReferenceSystem,
     * CoordinateReferenceSystem, CoordinateReferenceSystem, MathTransform) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
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
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties The properties to be given to the identified object.
     * @param  operations The sequence of operations. Shall contains at least two operations.
     * @param  factory    The math transform factory to use for math transforms concatenation.
     * @throws FactoryException if the factory can not concatenate the math transforms.
     */
    public DefaultConcatenatedOperation(final Map<String,?> properties,
                                        final CoordinateOperation[] operations,
                                        final MathTransformFactory factory)
            throws FactoryException
    {
        this(properties, new ArrayList<>(operations.length), operations, factory);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    private DefaultConcatenatedOperation(final Map<String,?> properties,
                                         final ArrayList<SingleOperation> list,
                                         final CoordinateOperation[] operations,
                                         final MathTransformFactory factory)
            throws FactoryException
    {
        this(properties, expand(operations, list, factory, true), list);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    private DefaultConcatenatedOperation(final Map<String,?> properties,
                                         final MathTransform transform,
                                         final List<SingleOperation> operations)
    {
        super(mergeAccuracy(properties, operations),
              operations.get(0).getSourceCRS(),
              operations.get(operations.size() - 1).getTargetCRS(),
              null, transform);

        this.operations = UnmodifiableArrayList.wrap(operations.toArray(new SingleOperation[operations.size()]));
    }

    /**
     * Transforms the list of operations into a list of single operations.
     * This method also checks for null value and makes sure that all CRS dimensions match.
     *
     * @param  operations    The array of operations to expand.
     * @param  target        The destination list in which to add {@code SingleOperation}.
     * @param  factory       The math transform factory to use.
     * @param  wantTransform {@code true} if the concatenated math transform should be computed.
     *         This is set to {@code false} only when this method invokes itself recursively.
     * @return The concatenated math transform, or {@code null} if {@code wantTransform} was {@code false}.
     * @throws FactoryException if the factory can not concatenate the math transforms.
     */
    private static MathTransform expand(final CoordinateOperation[] operations,
                                        final List<SingleOperation> target,
                                        final MathTransformFactory  factory,
                                        final boolean wantTransform)
            throws FactoryException
    {
        MathTransform transform = null;
        ArgumentChecks.ensureNonNull("operations", operations);
        for (int i=0; i<operations.length; i++) {
            ArgumentChecks.ensureNonNullElement("operations", i, operations);
            final CoordinateOperation op = operations[i];
            if (op instanceof SingleOperation) {
                target.add((SingleOperation) op);
            } else if (op instanceof ConcatenatedOperation) {
                final ConcatenatedOperation cop = (ConcatenatedOperation) op;
                final List<SingleOperation> cops = cop.getOperations();
                expand(cops.toArray(new CoordinateOperation[cops.size()]), target, factory, false);
            } else {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.IllegalArgumentClass_2, "operations[" + i + ']', op.getClass()));
            }
            /*
             * Checks the CRS dimensions.
             */
            if (i != 0) {
                final CoordinateReferenceSystem previous = operations[i-1].getTargetCRS();
                if (previous != null) {
                    final CoordinateReferenceSystem next = op.getSourceCRS();
                    if (next != null) {
                        final int dim1 = previous.getCoordinateSystem().getDimension();
                        final int dim2 = next.getCoordinateSystem().getDimension();
                        if (dim1 != dim2) {
                            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedDimension_3,
                                    "operations[" + i + "].sourceCRS", dim1, dim2));
                        }
                    }
                }
            }
            /*
             * Concatenates the math transform.
             */
            if (wantTransform) {
                final MathTransform step = op.getMathTransform();
                if (transform == null) {
                    transform = step;
                } else {
                    transform = factory.createConcatenatedTransform(transform, step);
                }
            }
        }
        if (wantTransform && target.size() <= 1) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.TooFewOccurrences_2, 2, CoordinateOperation.class));
        }
        return transform;
    }

    /**
     * If no accuracy were specified in the given properties map, adds all accuracies found in the operations
     * to concatenate. This method considers only {@link Transformation} components and ignores all conversions.
     *
     * <div class="note"><b>Why we ignore conversions:</b>
     * if a concatenated operation contains a datum shift (i.e. a transformation) with unknown accuracy,
     * and a projection (i.e. a conversion) with a declared 0 meter error, we don't want to declare this
     * 0 meter error as the concatenated operation  accuracy; it would be a false information.
     *
     * <p>An other reason is that a concatenated operation typically contains an arbitrary amount of conversions,
     * but only one transformation. So considering only transformations usually means to pickup only one operation
     * in the given {@code operations} list, which make things clearer.</p></div>
     *
     * <div class="note"><b>Note:</b>
     * according ISO 19111, the accuracy attribute is allowed only for transformations. However this restriction
     * is not enforced everywhere. For example the EPSG database declares an accuracy of 0 meter for conversions,
     * which is conceptually exact. In this class we are departing from strict interpretation of the specification
     * since we are adding accuracy informations to a concatenated operation. This departure should be considered
     * as a convenience feature only; accuracies are really relevant in transformations only.</div>
     */
    private static Map<String,?> mergeAccuracy(final Map<String,?> properties,
            final List<? extends CoordinateOperation> operations)
    {
        if (!properties.containsKey(COORDINATE_OPERATION_ACCURACY_KEY)) {
            Set<PositionalAccuracy> accuracy = null;
            for (final CoordinateOperation op : operations) {
                if (op instanceof Transformation) {
                    // See javadoc for a rational why we take only transformations in account.
                    Collection<PositionalAccuracy> candidates = op.getCoordinateOperationAccuracy();
                    if (!Containers.isNullOrEmpty(candidates)) {
                        if (accuracy == null) {
                            accuracy = new LinkedHashSet<>();
                        }
                        accuracy.addAll(candidates);
                    }
                }
            }
            if (accuracy != null) {
                final Map<String,Object> merged = new HashMap<>(properties);
                merged.put(COORDINATE_OPERATION_ACCURACY_KEY,
                        accuracy.toArray(new PositionalAccuracy[accuracy.size()]));
                return merged;
            }
        }
        return properties;
    }

    /**
     * Creates a new coordinate operation with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param operation The coordinate operation to copy.
     *
     * @see #castOrCopy(ConcatenatedOperation)
     */
    protected DefaultConcatenatedOperation(final ConcatenatedOperation operation) {
        super(operation);
        operations = operation.getOperations();
    }

    /**
     * Returns a SIS coordinate operation implementation with the values of the given arbitrary implementation.
     * If the given object is already an instance of {@code DefaultConcatenatedOperation}, then it is returned
     * unchanged. Otherwise a new {@code DefaultConcatenatedOperation} instance is created using the
     * {@linkplain #DefaultConcatenatedOperation(ConcatenatedOperation) copy constructor} and returned.
     * Note that this is a <cite>shallow</cite> copy operation, since the other properties contained in the given
     * object are not recursively copied.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultConcatenatedOperation castOrCopy(final ConcatenatedOperation object) {
        return (object == null) || (object instanceof DefaultConcatenatedOperation)
                ? (DefaultConcatenatedOperation) object : new DefaultConcatenatedOperation(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code ConcatenatedOperation.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code ConcatenatedOperation}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their
     * own set of interfaces.</div>
     *
     * @return {@code ConcatenatedOperation.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends ConcatenatedOperation> getInterface() {
        return ConcatenatedOperation.class;
    }

    /**
     * Returns the sequence of operations.
     *
     * @return The sequence of operations.
     */
    @Override
    public List<SingleOperation> getOperations() {
        return operations;
    }

    /**
     * Compares this concatenated operation with the specified object for equality. If the {@code mode} argument
     * is {@link ComparisonMode#STRICT} or {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available
     * properties are compared including the {@linkplain #getDomainOfValidity() domain of validity} and the
     * {@linkplain #getScope() scope}.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        if (super.equals(object, mode)) {
            if (mode == ComparisonMode.STRICT) {
                return Objects.equals(operations, ((DefaultConcatenatedOperation) object).operations);
            } else {
                return deepEquals(getOperations(), ((ConcatenatedOperation) object).getOperations(), mode);
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + 37 * operations.hashCode();
    }
}
