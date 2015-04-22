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
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.PassThroughOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.transform.PassThroughTransform;
import org.apache.sis.util.UnsupportedImplementationException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;

import static org.apache.sis.util.Utilities.deepEquals;

// Branch-dependent imports
import java.util.Objects;


/**
 * Specifies that a subset of a coordinate tuple is subject to a specific coordinate operation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public class DefaultPassThroughOperation extends AbstractCoordinateOperation implements PassThroughOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4308173919747248695L;

    /**
     * The operation to apply on the subset of a coordinate tuple.
     */
    private final SingleOperation operation;

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
     * @return The operation to apply on the subset of a coordinate tuple.
     *
     * @see PassThroughTransform#getSubTransform()
     */
    @Override
    public SingleOperation getOperation() {
        return operation;
    }

    /**
     * Returns the ordered sequence of indices in a source coordinate tuple of the coordinates
     * affected by this pass-through operation.
     *
     * @return Indices of the modified source coordinates.
     *
     * @see PassThroughTransform#getModifiedCoordinates()
     */
    @Override
    public int[] getModifiedCoordinates() {
        final MathTransform transform = super.getMathTransform();
        if (transform instanceof PassThroughTransform) {
            return ((PassThroughTransform) transform).getModifiedCoordinates();
        } else {
            // Should not happen since the constructor created the transform itself.
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
}
