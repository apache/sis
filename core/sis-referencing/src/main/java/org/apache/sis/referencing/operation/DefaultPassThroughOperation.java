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
import java.util.Arrays;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.PassThroughOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CompoundCRS;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.util.UnsupportedImplementationException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.Utilities.deepEquals;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;


/**
 * Specifies that a subset of a coordinate tuple is subject to a specific coordinate operation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@XmlType(name = "PassThroughOperationType", propOrder = {
    "indices",
    "operation"
})
@XmlRootElement(name = "PassThroughOperation")
public class DefaultPassThroughOperation extends AbstractCoordinateOperation implements PassThroughOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4308173919747248695L;

    /**
     * The operation to apply on the subset of a coordinate tuple.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setOperation(CoordinateOperation)}</p>
     *
     * @see #getOperation()
     */
    private SingleOperation operation;

    /**
     * Constructs a single operation from a set of properties.
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
     * @param properties The properties to be given to the identified object.
     * @param sourceCRS  The source CRS.
     * @param targetCRS  The target CRS.
     * @param operation  The operation to apply on the subset of a coordinate tuple.
     * @param firstAffectedOrdinate Index of the first affected ordinate.
     * @param numTrailingOrdinates Number of trailing ordinates to pass through.
     */
    public DefaultPassThroughOperation(final Map<String,?>            properties,
                                       final CoordinateReferenceSystem sourceCRS,
                                       final CoordinateReferenceSystem targetCRS,
                                       final SingleOperation           operation,
                                       final int firstAffectedOrdinate,
                                       final int numTrailingOrdinates)
    {
        super(properties, sourceCRS, targetCRS, null, PassThroughTransform.create(
                firstAffectedOrdinate, operation.getMathTransform(), numTrailingOrdinates));
        ArgumentChecks.ensureNonNull("operation", operation);
        this.operation = operation;
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
     * @see #castOrCopy(PassThroughOperation)
     */
    protected DefaultPassThroughOperation(final PassThroughOperation operation) {
        super(operation);
        this.operation = operation.getOperation();
    }

    /**
     * Returns a SIS coordinate operation implementation with the values of the given arbitrary implementation.
     * If the given object is already an instance of {@code DefaultPassThroughOperation}, then it is returned
     * unchanged. Otherwise a new {@code DefaultPassThroughOperation} instance is created using the
     * {@linkplain #DefaultPassThroughOperation(PassThroughOperation) copy constructor} and returned.
     * Note that this is a <cite>shallow</cite> copy operation, since the other properties contained in the given
     * object are not recursively copied.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultPassThroughOperation castOrCopy(final PassThroughOperation object) {
        return (object == null) || (object instanceof DefaultPassThroughOperation)
                ? (DefaultPassThroughOperation) object : new DefaultPassThroughOperation(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code PassThroughOperation.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code PassThroughOperation}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their
     * own set of interfaces.</div>
     *
     * @return {@code PassThroughOperation.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends PassThroughOperation> getInterface() {
        return PassThroughOperation.class;
    }

    /**
     * @deprecated May be removed in GeoAPI 4.0 since it does not apply to pass-through operations.
     *
     * @return {@code null}.
     */
    @Override
    @Deprecated
    public OperationMethod getMethod() {
        return null;
    }

    /**
     * @deprecated May be removed in GeoAPI 4.0 since it does not apply to pass-through operations.
     *
     * @return {@code null}.
     */
    @Override
    @Deprecated
    public ParameterValueGroup getParameterValues() {
        return null;
    }

    /**
     * Returns the operation to apply on the subset of a coordinate tuple.
     *
     * <div class="warning"><b>Upcoming API change</b><br>
     * This method is conformant to ISO 19111:2003. But the ISO 19111:2007 revision changed the type from
     * {@code SingleOperation} to {@link CoordinateOperation}. This change may be applied in GeoAPI 4.0.
     * This is necessary for supporting usage of {@code PassThroughOperation} with {@link ConcatenatedOperation}.
     * </div>
     *
     * @return The operation to apply on the subset of a coordinate tuple.
     *
     * @see PassThroughTransform#getSubTransform()
     */
    @Override
    @XmlElement(name = "coordOperation", required = true)
    public SingleOperation getOperation() {
        return operation;
    }

    /**
     * Returns the ordered sequence of indices in a source coordinate tuple of the coordinates
     * affected by this pass-through operation.
     *
     * @return Zero-based indices of the modified source coordinates.
     *
     * @see PassThroughTransform#getModifiedCoordinates()
     */
    @Override
    public int[] getModifiedCoordinates() {
        final MathTransform transform = super.getMathTransform();
        if (transform instanceof PassThroughTransform) {
            return ((PassThroughTransform) transform).getModifiedCoordinates();
        } else {
            /*
             * Should not happen with objects created by public methods since the constructor created the transform
             * itself. However may happen with operations parsed from GML. As a fallback, search in the components
             * of CompoundCRS. This is not a universal fallback, but work for the most straightforward cases.
             */
            final CoordinateReferenceSystem sourceCRS = super.getSourceCRS();
            if (sourceCRS instanceof CompoundCRS) {
                int firstAffectedOrdinate = 0;
                final CoordinateReferenceSystem search = operation.getSourceCRS();
                for (final CoordinateReferenceSystem c : ((CompoundCRS) sourceCRS).getComponents()) {
                    final int dim = ReferencingUtilities.getDimension(c);
                    if (c == search) {
                        final int[] indices = new int[dim];
                        for (int i=0; i<dim; i++) {
                            indices[i] = firstAffectedOrdinate + i;
                        }
                        return indices;
                    }
                    firstAffectedOrdinate += dim;
                }
            }
            throw new UnsupportedImplementationException(transform.getClass());
        }
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
                return Objects.equals(operation, ((DefaultPassThroughOperation) object).operation);
            } else {
                return deepEquals(getOperation(), ((PassThroughOperation) object).getOperation(), mode);
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
        return super.computeHashCode() + 31 * operation.hashCode();
    }

    /**
     * Formats this coordinate operation in a pseudo-Well Known Text (WKT) format.
     * Current format is specific to Apache SIS and may change in any future version
     * if a standard format for pass through operations is defined.
     *
     * @param  formatter The formatter to use.
     * @return Currently {@code "PassThroughOperation"} (may change in any future version).
     *
     * @since 0.7
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        formatter.append(new FormattableObject() {
            @Override protected String formatTo(final Formatter formatter) {
                for (final int i : getModifiedCoordinates()) {
                    formatter.append(i);
                }
                return "ModifiedCoordinates";
            }
        });
        formatter.newLine();
        formatter.append(castOrCopy(getOperation()));
        formatter.setInvalidWKT(this, null);
        return "PassThroughOperation";
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
    private DefaultPassThroughOperation() {
        /*
         * A sub-operation is mandatory for SIS working. We do not verify its presence here because the verification
         * would have to be done in an 'afterMarshal(…)' method and throwing an exception in that method causes the
         * whole unmarshalling to fail. But the CC_CoordinateOperation adapter does some verifications.
         */
    }

    /**
     * Invoked by JAXB at unmarshalling time for setting the coordinate operation
     * applied on the subset of a coordinate tuple.
     *
     * @see #getOperation()
     */
    private void setOperation(final SingleOperation op) {
        if (operation == null) {
            operation = op;
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultPassThroughOperation.class, "setOperation", "coordOperation");
        }
    }

    /**
     * Invoked by JAXB at marshalling time for getting the modified coordinates.
     * This method converts the zero-based indices to 1-based indices.
     *
     * @see #getModifiedCoordinates()
     */
    @XmlElement(name = "modifiedCoordinate", required = true)
    private int[] getIndices() {
        final int[] indices = getModifiedCoordinates();
        for (int i=0; i<indices.length; i++) {
            indices[i]++;
        }
        return indices;
    }

    /**
     * Invoked by JAXB at unmarshalling time for setting the modified coordinates.
     */
    private void setIndices(final int[] ordinates) {
        String missing = "sourceCRS";
        final CoordinateReferenceSystem sourceCRS = super.getSourceCRS();
        if (sourceCRS != null) {
            missing = "modifiedCoordinate";
            if (ordinates != null && ordinates.length != 0) {
                missing = "coordOperation";
                if (operation != null) {
                    for (int i=1; i<ordinates.length; i++) {
                        final int previous = ordinates[i-1];
                        if (previous < 1 || ordinates[i] != previous + 1) {
                            throw new IllegalArgumentException(Errors.format(
                                    Errors.Keys.CanNotAssign_2, missing, Arrays.toString(ordinates)));
                        }
                    }
                    transform = PassThroughTransform.create(ordinates[0] - 1, operation.getMathTransform(),
                            ReferencingUtilities.getDimension(sourceCRS) - ordinates[ordinates.length - 1]);
                    return;
                }
            }
        }
        throw new IllegalStateException(Errors.format(Errors.Keys.MissingComponentInElement_2, missing, "PassThroughOperation"));
    }
}
