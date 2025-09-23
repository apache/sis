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
import java.util.Objects;
import java.util.Locale;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.internal.PositionalAccuracyConstant;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.referencing.internal.shared.WKTUtilities;
import org.apache.sis.referencing.internal.shared.CoordinateOperations;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;


/**
 * An ordered sequence of two or more single coordinate operations. The sequence of operations is constrained
 * by the requirement that the source coordinate reference system of step (<var>n</var>+1) must be the same as
 * the target coordinate reference system of step (<var>n</var>). The source coordinate reference system of the
 * first step and the target coordinate reference system of the last step are the source and target coordinate
 * reference system associated with the concatenated operation.
 *
 * <p>Above requirements are relaxed when the source and target <abbr>CRS</abbr> of a step are swapped.
 * In such case, this step will actually be implemented by the inverse operation.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@XmlType(name = "ConcatenatedOperationType")
@XmlRootElement(name = "ConcatenatedOperation")
final class DefaultConcatenatedOperation extends AbstractCoordinateOperation implements ConcatenatedOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4199619838029045700L;

    /**
     * Optional key for specifying the {@link #transform} value.
     * This property should generally not be specified, as the constructor builds the transform itself.
     * It may be useful if the resulting transform is already known and we want to avoid the construction cost.
     */
    public static final String TRANSFORM_KEY = "transform";

    /**
     * The comparison modes to use for determining if two CRS are equal, in preference order.
     * This is used for determining if an operation need to be inverted.
     */
    private static final ComparisonMode[] CRS_ORDER_CRITERIA = {
        ComparisonMode.BY_CONTRACT,
        ComparisonMode.IGNORE_METADATA,
        ComparisonMode.COMPATIBILITY,
        ComparisonMode.APPROXIMATE
    };

    /**
     * The sequence of operations.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setSteps(CoordinateOperation[])}</p>
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private List<? extends CoordinateOperation> operations;

    /**
     * Constructs a concatenated operation from a set of properties and a
     * {@linkplain MathTransformFactory math transform factory}.
     * The properties given in argument follow the same rules as for the
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
     *   </tr><tr>
     *     <td>{@value #TRANSFORM_KEY}</td>
     *     <td>{@link MathTransform}</td>
     *     <td>{@link #getMathTransform()}</td>
     *   </tr><tr>
     *     <th colspan="3" class="hsep">Defined in parent class (reminder)</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.operation.CoordinateOperation#COORDINATE_OPERATION_ACCURACY_KEY}</td>
     *     <td>{@link PositionalAccuracy} (optionally as array)</td>
     *     <td>{@link #getCoordinateOperationAccuracy()}</td>
     *   </tr>
     * </table>
     *
     * The {@value #TRANSFORM_KEY} property should generally not be provided, as it is automatically computed.
     * That property is available for saving computation cost when the concatenated transform is known in advance,
     * or for overriding the automatic concatenation.
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  sourceCRS   the source <abbr>CRS</abbr>, or {@code null} for the source of the first step.
     * @param  targetCRS   the target <abbr>CRS</abbr>, or {@code null} for the target of the last effective step.
     * @param  operations  the sequence of operations. Shall contain at least two operations.
     * @param  mtFactory   the math transform factory to use for math transforms concatenation.
     * @throws FactoryException if this constructor or the factory cannot concatenate the operation steps.
     */
    public DefaultConcatenatedOperation(final Map<String,?>             properties,
                                        final CoordinateReferenceSystem sourceCRS,
                                        final CoordinateReferenceSystem targetCRS,
                                        final CoordinateOperation[]     operations,
                                        final MathTransformFactory      mtFactory)
            throws FactoryException
    {
        super(properties);
        if (operations.length < 2) {
            throw new InvalidGeodeticParameterException(Errors.forProperties(properties).getString(
                    Errors.Keys.TooFewOccurrences_2, 2, CoordinateOperation.class));
        }
        this.sourceCRS = sourceCRS;
        this.targetCRS = targetCRS;
        transform = Containers.property(properties, TRANSFORM_KEY, MathTransform.class);
        initialize(properties, operations, (transform == null) ? mtFactory : null);
        checkDimensions(properties);
    }

    /**
     * Initializes the {@link #sourceCRS}, {@link #targetCRS} and {@link #operations} fields.
     * If the source and target <abbr>CRS</abbr> are already non-null, leaves them unchanged
     * but verifies that they are consistent with the first and last steps.
     *
     * @param  properties   the properties specified at construction time, or {@code null} if unknown.
     * @param  operations   the operations to concatenate.
     * @param  mtFactory    the math transform factory to use, or {@code null} for not performing concatenation.
     * @throws FactoryException if this constructor or the factory cannot concatenate the operation steps.
     */
    private void initialize(final Map<String,?>         properties,
                            final CoordinateOperation[] operations,
                            final MathTransformFactory  mtFactory)
            throws FactoryException
    {
        final var flattened = new ArrayList<CoordinateOperation>(operations.length);
        final CoordinateReferenceSystem crs = initialize(properties, operations, flattened, mtFactory,
                sourceCRS, (sourceCRS == null), (coordinateOperationAccuracy == null));

        if (targetCRS == null) {
            targetCRS = crs;
        } else if (mtFactory != null) {
            var builder = mtFactory.builder(Constants.COORDINATE_SYSTEM_CONVERSION);
            builder.setSourceAxes(crs.getCoordinateSystem(), DatumOrEnsemble.getEllipsoid(crs).orElse(null));
            builder.setTargetAxes(targetCRS.getCoordinateSystem(), null);
            transform = mtFactory.createConcatenatedTransform(transform, builder.create());
        }
        /*
         * At this point we should have flattened.size() >= 2, except if some operations
         * were omitted because their associated math transform were identity operation.
         */
        this.operations = UnmodifiableArrayList.wrap(flattened.toArray(CoordinateOperation[]::new));
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
     * <h4>How coordinate operation accuracy is determined</h4>
     * If {@code setAccuracy} is {@code true}, then this method copies accuracy information found in the single
     * {@link Transformation} instance. This method ignores instances of other kinds for the following reason:
     * some {@link Conversion} instances declare an accuracy, which is typically close to zero. If a concatenated
     * operation contains such conversion together with a transformation with unknown accuracy, then we do not want
     * to declare "0 meter" as the concatenated operation accuracy; it would be a false information.
     * Another reason is that a concatenated operation typically contains an arbitrary number of conversions,
     * but only one transformation. So considering only transformations usually means to pickup only one operation
     * in the given {@code operations} list, which make things clearer.
     *
     * <h5>Note</h5>
     * According ISO 19111, the accuracy attribute is allowed only for transformations. However, this restriction
     * is not enforced everywhere. For example, the EPSG database declares an accuracy of 0 meter for conversions,
     * which is conceptually exact. In this class we are departing from strict interpretation of the specification
     * since we are adding accuracy information to a concatenated operation. This departure should be considered
     * as a convenience feature only; accuracies are really relevant in transformations only.
     *
     * @param  properties   the properties specified at construction time, or {@code null} if unknown.
     * @param  operations   the operations to concatenate.
     * @param  flattened    the destination list in which to add the {@code SingleOperation} instances.
     * @param  mtFactory    the math transform factory to use, or {@code null} for not performing concatenation.
     * @param  previous     target CRS of the step before the first {@code operations} step, or {@code null}.
     * @param  setSource    {@code true} for setting the {@link #sourceCRS} on the very first CRS (regardless if null or not).
     * @param  setAccuracy  {@code true} for setting the {@link #coordinateOperationAccuracy} field.
     * @return the last target CRS, regardless if null or not.
     * @throws FactoryException if the factory cannot concatenate the math transforms.
     */
    private CoordinateReferenceSystem initialize(
            final Map<String,?>             properties,
            final CoordinateOperation[]     operations,
            final List<CoordinateOperation> flattened,
            final MathTransformFactory      mtFactory,
            CoordinateReferenceSystem       previous,
            boolean setSource,
            boolean setAccuracy) throws FactoryException
    {
        CoordinateReferenceSystem source;                   // Source CRS of current iteration.
        CoordinateReferenceSystem target = null;            // Target CRS of current and last iteration.
        for (int i=0; i<operations.length; i++) {
            CoordinateOperation op = operations[i];
            ArgumentChecks.ensureNonNullElement("operations", i, op);
            /*
             * Verify consistency of user argument: for each coordinate operation, the source CRS
             * should be equal (ignoring metadata) to the target CRS of the previous operation.
             * An exception to this rule is when source and target CRS need to be swapped.
             */
            source = op.getSourceCRS();
            target = op.getTargetCRS();
            final boolean inverse = verifyStepChaining(properties, i, previous, source, target);
            if (inverse) {
                var t  = source;
                source = target;
                target = t;
                // Inverse the operation only if it produces a more natural definition.
                if (CoordinateOperations.getMethod(op) instanceof InverseOperationMethod) {
                    CoordinateOperation natural = getCachedInverse(op);
                    if (natural != null) op = natural;
                }
            }
            if (setSource) {
                setSource = false;
                sourceCRS = source;         // Take even if null.
            }
            /*
             * Now that we have verified the CRS chaining, we should be able to concatenate the transforms.
             * If an operation is a nested `ConcatenatedOperation` (not allowed by ISO 19111, but we try to
             * be safe), we will first try to use the `ConcatenatedOperation.transform` as a whole. Only if
             * that concatenated operation does not provide a transform, we will concatenate its components.
             * Note however that we traverse nested concatenated operations unconditionally at least for
             * checking its consistency.
             */
            NoninvertibleTransformException cause = null;
            MathTransform step = op.getMathTransform();
            if (step != null && inverse) try {
                step = step.inverse();
            } catch (NoninvertibleTransformException e) {
                step = null;
                cause = e;
            }
            if (step == null) {
                // May happen if the operation is a defining operation.
                throw new InvalidGeodeticParameterException(Resources.format(
                        Resources.Keys.OperationHasNoTransform_2, op.getClass(), op.getName()), cause);
            }
            if (op instanceof ConcatenatedOperation) {
                final var nested = ((ConcatenatedOperation) op).getOperations().toArray(CoordinateOperation[]::new);
                previous = initialize(properties, nested, flattened, null, previous, false, setAccuracy);
            } else {
                // Note: operation (source, target) may be in reverse order, but it should be taken as metadata.
                if (!step.isIdentity()) {
                    flattened.add(op);
                }
                previous = target;          // For next iteration cycle.
            }
            if (mtFactory != null) {
                transform = (transform != null) ? mtFactory.createConcatenatedTransform(transform, step) : step;
            }
            /*
             * Optionally copy the coordinate operation accuracy from the transformation (or from a concatenated
             * operation on the assumption that its accuracy was computed by the same algorithm as this method).
             * See javadoc for a rational about why we take only transformations in account. If more than one
             * transformation is found, clear the collection and abandon the attempt to set the accuracy information.
             * Instead, the user will get a better result by invoking PositionalAccuracyConstant.getLinearAccuracy(…)
             * since that method conservatively computes the sum of all linear accuracy.
             */
            if (setAccuracy && (op instanceof Transformation || op instanceof ConcatenatedOperation)
                    && (PositionalAccuracyConstant.getLinearAccuracy(op) != 0))
            {
                if (coordinateOperationAccuracy == null) {
                    coordinateOperationAccuracy = op.getCoordinateOperationAccuracy();
                } else {
                    coordinateOperationAccuracy = null;
                    setAccuracy = false;
                }
            }
        }
        if (mtFactory != null) {
            verifyStepChaining(properties, operations.length, target, targetCRS, null);
            // Else verification will be done by the caller.
        }
        return previous;
    }

    /**
     * Verifies if a step of a concatenated operation can be chained after the previous step.
     *
     * @param  properties  user-specified properties (for the locale of error message), or {@code null} if none.
     * @param  step        index of the operation step, used only in case an exception it thrown.
     * @param  previous    Target CRS of the previous step.
     * @param  source      Source CRS of the current step.
     * @param  target      Target CRS of the current step, or {@code null} if none.
     * @return whether the math transform needs to be inverted.
     * @throws FactoryException if the current operation cannot be chained after the previous operation.
     */
    static boolean verifyStepChaining(
            final Map<String,?> properties, final int step,
            final CoordinateReferenceSystem previous,
            final CoordinateReferenceSystem source,
            final CoordinateReferenceSystem target) throws FactoryException
    {
        if (previous == null || source == null) {
            return false;
        }
        for (final ComparisonMode mode : CRS_ORDER_CRITERIA) {
            if (Utilities.deepEquals(previous, source, mode)) return false;
            if (Utilities.deepEquals(previous, target, mode)) return true;
        }
        Resources resources = Resources.forProperties(properties);
        Locale locale = resources.getLocale();
        throw new InvalidGeodeticParameterException(resources.getString(
                Resources.Keys.MismatchedSourceTargetCRS_3, step,
                IdentifiedObjects.getDisplayName(previous, locale),
                IdentifiedObjects.getDisplayName(source, locale)));
    }

    /**
     * Creates a new coordinate operation with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  operation  the coordinate operation to copy.
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
     * Note that this is a <em>shallow</em> copy operation,
     * since the other properties contained in the given object are not recursively copied.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
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
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code ConcatenatedOperation}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.
     *
     * @return {@code ConcatenatedOperation.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends ConcatenatedOperation> getInterface() {
        return ConcatenatedOperation.class;
    }

    /**
     * Returns the sequence of operations that are steps in this concatenated operation.
     * The sequence can contain {@link org.opengis.referencing.operation.SingleOperation}s
     * or {@link org.opengis.referencing.operation.PassThroughOperation}s.
     *
     * @return the sequence of operations.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<? extends CoordinateOperation> getOperations() {
        return operations;
    }

    /**
     * Compares this concatenated operation with the specified object for equality. If the {@code mode} argument
     * is {@link ComparisonMode#STRICT} or {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available
     * properties are compared including the {@linkplain #getDomains() domains} and the accuracy.
     *
     * @return {@inheritDoc}
     *
     * @hidden because nothing new to said.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;                            // Slight optimization.
        }
        if (super.equals(object, mode)) {
            if (mode == ComparisonMode.STRICT) {
                return Objects.equals(operations, ((DefaultConcatenatedOperation) object).operations);
            } else {
                return Utilities.deepEquals(getOperations(), ((ConcatenatedOperation) object).getOperations(), mode);
            }
        }
        return false;
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + 37 * Objects.hashCode(operations);
    }

    /**
     * Formats this coordinate operation in Well Known Text (WKT) version 2 format.
     *
     * @param  formatter  the formatter to use.
     * @return {@code "ConcatenatedOperation"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        for (final CoordinateOperation component : operations) {
            formatter.newLine();
            formatter.append(new FormattableObject() {
                @Override protected String formatTo(Formatter formatter) {
                    formatter.newLine();
                    formatter.appendFormattable(component, AbstractCoordinateOperation::castOrCopy);
                    return WKTKeywords.Step;
                }
            });
        }
        WKTUtilities.appendElementIfPositive(WKTKeywords.OperationAccuracy, getLinearAccuracy(), formatter);
        if (!formatter.getConvention().supports(Convention.WKT2_2019)) {
            formatter.setInvalidWKT(this, null);
        }
        return WKTKeywords.ConcatenatedOperation;
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    private DefaultConcatenatedOperation() {
        operations = List.of();
    }

    /**
     * Returns the operations to marshal. We use this private methods instead of annotating
     * {@link #getOperations()} in order to force JAXB to invoke the setter method on unmarshalling.
     */
    @XmlElement(name = "coordOperation", required = true)
    private CoordinateOperation[] getSteps() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final List<? extends CoordinateOperation> operations = getOperations();
        return (operations != null) ? operations.toArray(CoordinateOperation[]::new) : null;
    }

    /**
     * Invoked by JAXB for setting the operations.
     */
    private void setSteps(final CoordinateOperation[] steps) throws FactoryException {
        initialize(null, steps, DefaultMathTransformFactory.provider());
    }
}
