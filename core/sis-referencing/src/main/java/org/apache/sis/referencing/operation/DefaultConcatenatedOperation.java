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
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.internal.referencing.PositionalAccuracyConstant;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.Utilities.deepEquals;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;
import org.opengis.referencing.operation.SingleOperation;


/**
 * An ordered sequence of two or more single coordinate operations. The sequence of operations is constrained
 * by the requirement that the source coordinate reference system of step (<var>n</var>+1) must be the same as
 * the target coordinate reference system of step (<var>n</var>). The source coordinate reference system of the
 * first step and the target coordinate reference system of the last step are the source and target coordinate
 * reference system associated with the concatenated operation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@XmlType(name = "ConcatenatedOperationType")
@XmlRootElement(name = "ConcatenatedOperation")
final class DefaultConcatenatedOperation extends AbstractCoordinateOperation implements ConcatenatedOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4199619838029045700L;

    /**
     * The sequence of operations.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setSteps(CoordinateOperation[])}</p>
     */
    private List<SingleOperation> operations;

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
     * @param  mtFactory  The math transform factory to use for math transforms concatenation.
     * @throws FactoryException if the factory can not concatenate the math transforms.
     */
    public DefaultConcatenatedOperation(final Map<String,?> properties, CoordinateOperation[] operations,
            final MathTransformFactory mtFactory) throws FactoryException
    {
        super(properties);
        ArgumentChecks.ensureNonNull("operations", operations);
        final List<CoordinateOperation> flattened = new ArrayList<CoordinateOperation>(operations.length);
        initialize(properties, operations, flattened, mtFactory, (coordinateOperationAccuracy == null));
        if (flattened.size() < 2) {
            throw new IllegalArgumentException(Errors.getResources(properties).getString(
                    Errors.Keys.TooFewOccurrences_2, 2, CoordinateOperation.class));
        }
        // The array is of kind CoordinateOperation[] on GeoAPI 4.0-M03,
        // but we have to restrict to SingleOperation[] on GeoAPI 3.x.
        operations      = flattened.toArray(new SingleOperation[flattened.size()]);
        this.operations = UnmodifiableArrayList.wrap((SingleOperation[]) operations);
        this.sourceCRS  = operations[0].getSourceCRS();
        this.targetCRS  = operations[operations.length - 1].getTargetCRS();
        checkDimensions(properties);
    }

    /**
     * Performs the part of {@code DefaultConcatenatedOperations} construction that requires an iteration over
     * the sequence of coordinate operations. This method performs the following processing:
     *
     * <ul>
     *   <li>Verify the validity of the {@code operations} argument.</li>
     *   <li>Add the single operations in the {@code flattened} array.</li>
     *   <li>Set the {@link #transform} field to the concatenated transform.</li>
     *   <li>Set the {@link #coordinateOperationAccuracy} field, but only if {@code setAccuracy} is {@code true}.</li>
     * </ul>
     *
     * This method invokes itself recursively if there is nested {@code ConcatenatedOperation} instances
     * in the given list. This should not happen according ISO 19111 standard, but we try to be safe.
     *
     * <div class="section">How coordinate operation accuracy is determined</div>
     * If {@code setAccuracy} is {@code true}, then this method copies accuracy information found in the single
     * {@link Transformation} instance. This method ignores instances of other kinds for the following reason:
     * some {@link Conversion} instances declare an accuracy, which is typically close to zero. If a concatenated
     * operation contains such conversion together with a transformation with unknown accuracy, then we do not want
     * to declare "0 meter" as the concatenated operation accuracy; it would be a false information.
     * An other reason is that a concatenated operation typically contains an arbitrary amount of conversions,
     * but only one transformation. So considering only transformations usually means to pickup only one operation
     * in the given {@code operations} list, which make things clearer.
     *
     * <div class="note"><b>Note:</b>
     * according ISO 19111, the accuracy attribute is allowed only for transformations. However this restriction
     * is not enforced everywhere. For example the EPSG database declares an accuracy of 0 meter for conversions,
     * which is conceptually exact. In this class we are departing from strict interpretation of the specification
     * since we are adding accuracy informations to a concatenated operation. This departure should be considered
     * as a convenience feature only; accuracies are really relevant in transformations only.</div>
     *
     * @param  properties  The properties specified at construction time, or {@code null} if unknown.
     * @param  operations  The operations to concatenate.
     * @param  flattened   The destination list in which to add the {@code SingleOperation} instances.
     * @param  mtFactory   The math transform factory to use, or {@code null} for not performing concatenation.
     * @param  setAccuracy {@code true} for setting the {@link #coordinateOperationAccuracy} field.
     * @throws FactoryException if the factory can not concatenate the math transforms.
     */
    private void initialize(final Map<String,?>             properties,
                            final CoordinateOperation[]     operations,
                            final List<CoordinateOperation> flattened,
                            final MathTransformFactory      mtFactory,
                            boolean                         setAccuracy)
            throws FactoryException
    {
        CoordinateReferenceSystem previous = null;
        for (int i=0; i<operations.length; i++) {
            final CoordinateOperation op = operations[i];
            ArgumentChecks.ensureNonNullElement("operations", i, op);
            /*
             * Verify consistency of user argument: for each coordinate operation, the number of dimensions of the
             * source CRS shall be equals to the number of dimensions of the target CRS in the previous operation.
             */
            if (previous != null) {
                final CoordinateReferenceSystem next = op.getSourceCRS();
                if (next != null) {
                    final int dim1 = previous.getCoordinateSystem().getDimension();
                    final int dim2 = next.getCoordinateSystem().getDimension();
                    if (dim1 != dim2) {
                        throw new IllegalArgumentException(Errors.getResources(properties).getString(
                                Errors.Keys.MismatchedDimension_3, "operations[" + i + "].sourceCRS", dim1, dim2));
                    }
                }
            }
            previous = op.getTargetCRS();   // For next iteration cycle.
            /*
             * Now that we have verified the CRS dimensions, we should be able to concatenate the transforms.
             * If an operation is a nested ConcatenatedOperation (not allowed by ISO 19111, but we try to be
             * safe), we will first try to use the ConcatenatedOperation.transform as a whole.  Only if that
             * concatenated operation does not provide a transform we will concatenate its components.  Note
             * however that we traverse nested concatenated operations unconditionally at least for checking
             * its consistency.
             */
            MathTransform step = op.getMathTransform();
            if (op instanceof ConcatenatedOperation) {
                final List<? extends CoordinateOperation> children = ((ConcatenatedOperation) op).getOperations();
                @SuppressWarnings("SuspiciousToArrayCall")
                final CoordinateOperation[] asArray = children.toArray(new CoordinateOperation[children.size()]);
                initialize(properties, asArray, flattened, (step == null) ? mtFactory : null, setAccuracy);
            } else {
                flattened.add(op);
            }
            if (mtFactory != null) {
                transform = (transform != null) ? mtFactory.createConcatenatedTransform(transform, step) : step;
            }
            /*
             * Optionally copy the coordinate operation accuracy from the transformation (or from a concatenated
             * operation on the assumption that its accuracy was computed by the same algorithm than this method).
             * See javadoc for a rational about why we take only transformations in account. If more than one
             * transformation is found, clear the collection and abandon the attempt to set the accuracy information.
             * Instead the user will get a better result by invoking PositionalAccuracyConstant.getLinearAccuracy(…)
             * since that method conservatively computes the sum of all linear accuracy.
             */
            if (setAccuracy && (op instanceof Transformation || op instanceof ConcatenatedOperation)) {
                if (coordinateOperationAccuracy == null) {
                    setAccuracy = (PositionalAccuracyConstant.getLinearAccuracy(op) > 0);
                    if (setAccuracy) {
                        coordinateOperationAccuracy = op.getCoordinateOperationAccuracy();
                    }
                } else {
                    coordinateOperationAccuracy = null;
                    setAccuracy = false;
                }
            }
        }
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
     * <div class="warning"><b>Upcoming API change</b><br>
     * This method is conformant to ISO 19111:2003. But the ISO 19111:2007 revision changed the element type
     * from {@code SingleOperation} to {@link CoordinateOperation}. This change may be applied in GeoAPI 4.0.
     * This is necessary for supporting usage of {@code PassThroughOperation} with {@link ConcatenatedOperation}.
     * </div>
     *
     * @return The sequence of operations.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
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
        return super.computeHashCode() + 37 * Objects.hashCode(operations);
    }

    /**
     * Formats this coordinate operation in pseudo-WKT. This is specific to Apache SIS since
     * there is no concatenated operation in the Well Known Text (WKT) version 2 format.
     *
     * @param  formatter The formatter to use.
     * @return {@code "ConcatenatedOperation"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        for (final CoordinateOperation component : operations) {
            formatter.newLine();
            formatter.append(castOrCopy(component));
        }
        formatter.setInvalidWKT(this, null);
        return "ConcatenatedOperation";
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultConcatenatedOperation() {
        operations = Collections.emptyList();
    }

    /**
     * Returns the operations to marshal. We use this private methods instead than annotating
     * {@link #getOperations()} in order to force JAXB to invoke the setter method on unmarshalling.
     */
    @SuppressWarnings("SuspiciousToArrayCall")
    @XmlElement(name = "coordOperation", required = true)
    private CoordinateOperation[] getSteps() {
        final List<? extends CoordinateOperation> operations = getOperations();
        return (operations != null) ? operations.toArray(new CoordinateOperation[operations.size()]) : null;
    }

    /**
     * Invoked by JAXB for setting the operations.
     */
    private void setSteps(final CoordinateOperation[] steps) throws FactoryException {
        final List<CoordinateOperation> flattened = new ArrayList<CoordinateOperation>(steps.length);
        initialize(null, steps, flattened, DefaultFactories.forBuildin(MathTransformFactory.class), coordinateOperationAccuracy == null);
        operations = UnmodifiableArrayList.wrap(flattened.toArray(new SingleOperation[flattened.size()]));
    }
}
