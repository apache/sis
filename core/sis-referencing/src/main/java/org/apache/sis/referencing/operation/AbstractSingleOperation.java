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
     * Returns the operation method (non-overrideable).
     */
    @Override
    final OperationMethod method() {
        return method;
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
    public ParameterValueGroup getParameterValues() throws UnsupportedOperationException {
        return (parameters != null) ? parameters.clone() : super.getParameterValues();
    }
}
