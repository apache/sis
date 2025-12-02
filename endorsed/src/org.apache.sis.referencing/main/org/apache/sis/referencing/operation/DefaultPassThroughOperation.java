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
import java.util.Objects;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.PassThroughOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.GeodeticException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.metadata.internal.shared.ImplementationHelper;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import static org.apache.sis.util.Utilities.deepEquals;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.parameter.ParameterValueGroup;


/**
 * Specifies that a subset of a coordinate tuple is subject to a specific coordinate operation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 * @since   0.6
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
    private static final long serialVersionUID = 3516394762777350439L;

    /**
     * The operation to apply on the subset of a coordinate tuple.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@code setOperation(CoordinateOperation)}</p>
     *
     * @see #getOperation()
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private SingleOperation operation;

    /**
     * Zero-based indices of the modified source coordinates.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setIndices(int[])}</p>
     *
     * @see #getModifiedCoordinates()
     */
    private int[] modifiedCoordinates;

    /**
     * Constructs a pass-through operation from a set of properties.
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
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties               the properties to be given to the identified object.
     * @param  sourceCRS                the source CRS.
     * @param  targetCRS                the target CRS.
     * @param  operation                the operation to apply on the subset of a coordinate tuple.
     * @param  firstAffectedCoordinate  index of the first affected coordinate.
     * @param  numTrailingCoordinates   number of trailing coordinates to pass through.
     */
    public DefaultPassThroughOperation(final Map<String,?>            properties,
                                       final CoordinateReferenceSystem sourceCRS,
                                       final CoordinateReferenceSystem targetCRS,
                                       final SingleOperation           operation,
                                       final int firstAffectedCoordinate,
                                       final int numTrailingCoordinates)
    {
        this(properties, sourceCRS, targetCRS, operation, operation.getMathTransform(), firstAffectedCoordinate, numTrailingCoordinates);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    private DefaultPassThroughOperation(final Map<String,?>            properties,
                                        final CoordinateReferenceSystem sourceCRS,
                                        final CoordinateReferenceSystem targetCRS,
                                        final SingleOperation           operation,
                                        final MathTransform          subTransform,
                                        final int firstAffectedCoordinate,
                                        final int numTrailingCoordinates)
    {
        super(properties, sourceCRS, targetCRS, null,
              MathTransforms.passThrough(firstAffectedCoordinate, subTransform, numTrailingCoordinates));
        this.operation = operation;
        modifiedCoordinates = ArraysExt.range(firstAffectedCoordinate,
                                              firstAffectedCoordinate + subTransform.getSourceDimensions());
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
     * @see #castOrCopy(PassThroughOperation)
     */
    protected DefaultPassThroughOperation(final PassThroughOperation operation) {
        super(operation);
        this.operation = operation.getOperation();
        modifiedCoordinates = operation.getModifiedCoordinates();
    }

    /**
     * Returns a SIS coordinate operation implementation with the values of the given arbitrary implementation.
     * If the given object is already an instance of {@code DefaultPassThroughOperation}, then it is returned
     * unchanged. Otherwise a new {@code DefaultPassThroughOperation} instance is created using the
     * {@linkplain #DefaultPassThroughOperation(PassThroughOperation) copy constructor} and returned.
     * Note that this is a <em>shallow</em> copy operation,
     * because the other properties contained in the given object are not recursively copied.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
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
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code PassThroughOperation}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.
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
     * This is necessary for supporting usage of {@code PassThroughOperation} with {@code ConcatenatedOperation}.
     * </div>
     *
     * @return the operation to apply on the subset of a coordinate tuple.
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
     * @return zero-based indices of the modified source coordinates.
     *
     * @see PassThroughTransform#getModifiedCoordinates()
     */
    @Override
    public int[] getModifiedCoordinates() {
        return modifiedCoordinates.clone();
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
            return true;                                                    // Slight optimization.
        }
        if (super.equals(object, mode)) {
            if (mode == ComparisonMode.STRICT) {
                final var other = (DefaultPassThroughOperation) object;
                return Arrays.equals(modifiedCoordinates, other.modifiedCoordinates)
                        && Objects.equals(operation, other.operation);
            } else {
                final var other = (PassThroughOperation) object;
                return Arrays.equals(getModifiedCoordinates(), other.getModifiedCoordinates())
                        && deepEquals(getOperation(), other.getOperation(), mode);
            }
        }
        return false;
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + 37 * operation.hashCode() + Arrays.hashCode(modifiedCoordinates);
    }

    /**
     * Formats this coordinate operation in a pseudo-Well Known Text (WKT) format.
     * Current format is specific to Apache SIS and may change in any future version
     * if a standard format for pass through operations is defined.
     *
     * @param  formatter  the formatter to use.
     * @return currently {@code "PassThroughOperation"} (may change in any future version).
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
    private DefaultPassThroughOperation() {
        /*
         * A sub-operation is mandatory for SIS working. We do not verify its presence here because the verification
         * would have to be done in an `afterMarshal(…)` method and throwing an exception in that method causes the
         * whole unmarshalling to fail. But the `CC_CoordinateOperation` adapter does some verifications.
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
            ImplementationHelper.propertyAlreadySet(DefaultPassThroughOperation.class, "setOperation", "coordOperation");
        }
    }

    /**
     * Invoked by JAXB at marshalling time for getting the modified coordinates.
     * This method converts the zero-based indices to 1-based indices.
     *
     * @return one-based indices of the modified source coordinates.
     *
     * @see #getModifiedCoordinates()
     */
    @XmlElement(name = "modifiedCoordinate", required = true)
    private int[] getIndices() {
        final int[] dimensions = getModifiedCoordinates();
        for (int i=0; i<dimensions.length; i++) {
            dimensions[i]++;
        }
        return dimensions;
    }

    /**
     * Invoked by JAXB at unmarshalling time for setting the modified coordinates.
     *
     * @param  indices  one-based indices of the modified source coordinates.
     */
    private void setIndices(int[] dimensions) {
        if (modifiedCoordinates == null) {
            if (dimensions.length != 0) {
                modifiedCoordinates = dimensions = dimensions.clone();
                for (int i=0; i<dimensions.length; i++) {
                    dimensions[i]--;
                }
            }
        } else {
            ImplementationHelper.propertyAlreadySet(DefaultPassThroughOperation.class, "setIndices", "modifiedCoordinates");
        }
    }

    /**
     * Invoked by JAXB after unmarshalling. If needed, this method tries to infer source/target CRS
     * of the nested operation from the source/target CRS of the enclosing pass-through operation.
     */
    @Override
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    final void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        super.afterUnmarshal(unmarshaller, parent);
        /*
         * State validation. The `missing` string will be used in exception message
         * at the end of this method if a required component is reported missing.
         */
        final int[] modifiedCoordinates = this.modifiedCoordinates;
        FactoryException cause = null;
        String missing = "modifiedCoordinate";
        if (modifiedCoordinates.length != 0) {
            missing = "sourceCRS";
            final CoordinateReferenceSystem sourceCRS = super.sourceCRS;
            if (sourceCRS != null) {
                missing = "targetCRS";
                final CoordinateReferenceSystem targetCRS = super.targetCRS;
                if (targetCRS != null) {
                    missing = "coordOperation";
                    if (operation != null) {
                        /*
                         * If the operation is a defining operation, we need to replace it by a full operation.
                         */
                        MathTransform subTransform = operation.getMathTransform();
                        if (operation instanceof Conversion) {
                            CoordinateReferenceSystem sourceSub = operation.getSourceCRS();
                            CoordinateReferenceSystem targetSub = operation.getTargetCRS();
                            if (subTransform == null || sourceSub == null || targetSub == null) try {
                                if (sourceSub == null) sourceSub = CRS.selectDimensions(sourceCRS, modifiedCoordinates);
                                if (targetSub == null) targetSub = CRS.selectDimensions(targetCRS, modifiedCoordinates);
                                operation = DefaultConversion.castOrCopy((Conversion) operation)
                                            .specialize(sourceSub, targetSub, null);
                                subTransform = operation.getMathTransform();
                            } catch (FactoryException e) {
                                cause = e;
                            }
                        }
                        if (subTransform != null) {
                            final int resultDim = sourceCRS.getCoordinateSystem().getDimension();
                            transform = MathTransforms.passThrough(modifiedCoordinates, subTransform, resultDim);
                            return;
                        }
                    }
                }
            }
        }
        throw new GeodeticException(Errors.format(Errors.Keys.MissingComponentInElement_2, "PassThroughOperation", missing), cause);
    }
}
