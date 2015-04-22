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
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.referencing.OperationMethods;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;

import static org.apache.sis.util.Utilities.deepEquals;

// Branch-dependent imports
import java.util.Objects;


/**
 * Shared implementation for {@link DefaultConversion} and {@link DefaultTransformation}.
 * Does not need to be public, as users should handle only conversions or transformations.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
class AbstractSingleOperation extends AbstractCoordinateOperation implements SingleOperation, Parameterized {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2635450075620911309L;

    /**
     * The operation method.
     */
    private final OperationMethod method;

    /**
     * The parameter values, or {@code null} for inferring it from the math transform.
     */
    private final ParameterValueGroup parameters;

    /**
     * Creates a coordinate operation from the given properties.
     */
    public AbstractSingleOperation(final Map<String,?>             properties,
                                   final CoordinateReferenceSystem sourceCRS,
                                   final CoordinateReferenceSystem targetCRS,
                                   final CoordinateReferenceSystem interpolationCRS,
                                   final OperationMethod           method,
                                   final MathTransform             transform)
    {
        super(properties, sourceCRS, targetCRS, interpolationCRS, transform);
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        ArgumentChecks.ensureNonNull("method",    method);
        ArgumentChecks.ensureNonNull("transform", transform);
        this.method = method;
        /*
         * Undocumented property. We do not document it because parameters are usually either inferred from
         * the MathTransform, or specified explicitely in a DefiningConversion. However there is a few cases,
         * for example the Molodenski transform, where none of the above can apply, because the operation is
         * implemented by a concatenation of math transforms, and concatenations do not have ParameterValueGroup.
         */
        parameters = Containers.property(properties, OperationMethods.PARAMETERS_KEY, ParameterValueGroup.class);
        // No clone since this is a SIS internal property and SIS does not modify those values after construction.
    }

    /**
     * Returns the operation method.
     *
     * @return The operation method.
     */
    @Override
    public OperationMethod getMethod() {
        return method;
    }

    /**
     * Returns a description of the parameters. The default implementation tries to infer the
     * description from the {@linkplain #getMathTransform() math transform} itself before to
     * fallback on the {@linkplain DefaultOperationMethod#getParameters() method parameters}.
     *
     * <div class="note"><b>Note:</b>
     * the two parameter descriptions (from the {@code MathTransform} or from the {@code OperationMethod})
     * should be very similar. If they differ, it should be only in minor details like remarks, default
     * values or units of measurement.</div>
     *
     * @return A description of the parameters.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return (parameters != null) ? parameters.getDescriptor() : super.getParameterDescriptors();
    }

    /**
     * Returns the parameter values. The default implementation infers the parameter values from the
     * {@linkplain #getMathTransform() math transform}, if possible.
     *
     * @return The parameter values.
     * @throws UnsupportedOperationException if the parameter values can not be determined
     *         for the current math transform implementation.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return (parameters != null) ? parameters.clone() : super.getParameterValues();
    }

    /**
     * Compares this coordinate operation with the specified object for equality. If the {@code mode} argument
     * is {@link ComparisonMode#STRICT} or {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available
     * properties are compared including the {@linkplain #getDomainOfValidity() domain of validity} and the
     * {@linkplain #getScope() scope}.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;   // Slight optimization.
        }
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final AbstractSingleOperation that = (AbstractSingleOperation) object;
                return Objects.equals(method,     that.method) &&
                       Objects.equals(parameters, that.parameters);
            }
            case BY_CONTRACT: {
                final SingleOperation that = (SingleOperation) object;
                return deepEquals(getMethod(),          that.getMethod(),          mode) &&
                       deepEquals(getParameterValues(), that.getParameterValues(), mode);
            }
        }
        /*
         * We consider the operation method as metadata. One could argue that OperationMethod's 'sourceDimension' and
         * 'targetDimension' are not metadata, but their values should be identical to the 'sourceCRS' and 'targetCRS'
         * dimensions, already checked below. We could also argue that 'OperationMethod.parameters' are not metadata,
         * but their values should have been taken in account for the MathTransform creation, compared below.
         *
         * Comparing the MathTransforms instead of parameters avoid the problem of implicit parameters. For example in
         * a ProjectedCRS, the "semiMajor" and "semiMinor" axis lengths are sometime provided as explicit parameters,
         * and sometime inferred from the geodetic datum. The two cases would be different set of parameters from the
         * OperationMethod's point of view, but still result in the creation of identical MathTransforms.
         *
         * An other rational for treating OperationMethod as metadata is that SIS's MathTransform providers extend
         * DefaultOperationMethod. Consequently there is a wide range of subclasses, which make the comparisons more
         * difficult. For example Mercator1SP and Mercator2SP providers are two different ways to describe the same
         * projection. The SQL-backed EPSG factory uses yet an other implementation.
         *
         * NOTE: A previous Geotk implementation made this final check:
         *
         *     return nameMatches(this.method, that.method);
         *
         * but it was not strictly necessary since it was redundant with the comparisons of MathTransforms.
         * Actually it was preventing to detect that two CRS were equivalent despite different method names
         * (e.g. "Mercator (1SP)" and "Mercator (2SP)" when the parameters are properly chosen).
         */
        return true;
    }
}
