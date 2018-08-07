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
import java.util.Collection;
import javax.xml.bind.annotation.XmlTransient;
import javax.measure.Unit;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.referencing.SignReversalComment;
import org.apache.sis.internal.referencing.provider.AbstractProvider;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.Deprecable;


/**
 * Description of the inverse of another method. This class should be used only when no operation is defined
 * for the inverse, or when the inverse operation can not be represented by inverting the sign of parameters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
@XmlTransient
final class InverseOperationMethod extends DefaultOperationMethod {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6395008927817202180L;

    /**
     * The original operation method for which this {@code InverseOperationMethod} is the inverse.
     */
    private final OperationMethod inverse;

    /**
     * Creates the inverse of the given method.
     */
    private InverseOperationMethod(final Map<String,?> properties, final OperationMethod method) {
        super(properties, method.getTargetDimensions(), method.getSourceDimensions(), method.getParameters());
        inverse = method;
    }

    /**
     * Returns {@code true} if the given method flags itself as invertible.
     */
    private static boolean isInvertible(final OperationMethod method) {
        return method instanceof AbstractProvider && ((AbstractProvider) method).isInvertible();
    }

    /**
     * Returns or create the inverse of the given operation method. If the same operation method can be used
     * for the inverse operation either with the exact same parameter values or with the sign of some values
     * reversed, then the given method is returned as-is. Otherwise a synthetic method is created.
     */
    static OperationMethod create(final OperationMethod method) {
        if (method instanceof InverseOperationMethod) {
            return ((InverseOperationMethod) method).inverse;
        }
        if (!isInvertible(method)) {
            boolean useSameParameters = false;
            for (final GeneralParameterDescriptor descriptor : method.getParameters().descriptors()) {
                useSameParameters = (descriptor.getRemarks() instanceof SignReversalComment);
                if (!useSameParameters) break;
            }
            if (!useSameParameters) {
                Identifier name = method.getName();
                name = new ImmutableIdentifier(null, null, "Inverse of " + name.getCode());
                final Map<String,Object> properties = new HashMap<>(6);
                properties.put(NAME_KEY,    name);
                properties.put(FORMULA_KEY, method.getFormula());
                properties.put(REMARKS_KEY, method.getRemarks());
                if (method instanceof Deprecable) {
                    properties.put(DEPRECATED_KEY, ((Deprecable) method).isDeprecated());
                }
                return new InverseOperationMethod(properties, method);
            }
        }
        return method;
    }

    /**
     * Infers the properties to give to an inverse coordinate operation.
     * The returned map will contains three kind of information:
     *
     * <ul>
     *   <li>Metadata (domain of validity, accuracy)</li>
     *   <li>Parameter values, if possible</li>
     * </ul>
     *
     * This method copies accuracy and domain of validity metadata from the given operation.
     * We presume that the inverse operation has the same accuracy than the direct operation.
     *
     * <div class="note"><b>Note:</b>
     * in many cases, the inverse operation is numerically less accurate than the direct operation because it
     * uses approximations like series expansions or iterative methods. However the numerical errors caused by
     * those approximations are not of interest here, because they are usually much smaller than the inaccuracy
     * due to the stochastic nature of coordinate transformations (not to be confused with coordinate conversions;
     * see ISO 19111 for more information).</div>
     *
     * If the inverse of the given operation can be represented by inverting the sign of all numerical
     * parameter values, then this method copies also those parameters in a {@code "parameters"} entry.
     *
     * @param source  the operation for which to get the inverse parameters.
     * @param target  where to store the inverse parameters.
     */
    static void properties(final SingleOperation source, final Map<String,Object> target) {
        target.put(SingleOperation.DOMAIN_OF_VALIDITY_KEY, source.getDomainOfValidity());
        final Collection<PositionalAccuracy> accuracy = source.getCoordinateOperationAccuracy();
        if (!Containers.isNullOrEmpty(accuracy)) {
            target.put(SingleOperation.COORDINATE_OPERATION_ACCURACY_KEY,
                    accuracy.toArray(new PositionalAccuracy[accuracy.size()]));
        }
        /*
         * If the inverse of the given operation can be represented by inverting the sign of all numerical
         * parameter values, copies those parameters in a "parameters" entry in the properties map.
         * Otherwise does nothing.
         */
        final ParameterValueGroup parameters = source.getParameterValues();
        final ParameterValueGroup copy = parameters.getDescriptor().createValue();
        for (final GeneralParameterValue gp : parameters.values()) {
            if (gp instanceof ParameterValue<?>) {
                final ParameterValue<?> src = (ParameterValue<?>) gp;
                final Object value = src.getValue();
                if (value instanceof Number) {
                    final ParameterDescriptor<?> descriptor = src.getDescriptor();
                    final InternationalString remarks = descriptor.getRemarks();
                    if (remarks != SignReversalComment.SAME) {
                        if (remarks != SignReversalComment.OPPOSITE) {
                            /*
                             * The parameter descriptor does not specify whether the values for the inverse operation
                             * have the same sign or opposite sign. We could heuristically presume that we can invert
                             * the sign if the minimum value has the opposite sign than the maximum value  (as in the
                             * [-10 … 10] range), but such assumption is dangerous. For example the values in a matrix
                             * could be bounded to a range like [-1 … 1], which would mislead above heuristic rule.
                             *
                             * Note that abandoning here does not mean that we will never know the parameter values.
                             * As a fallback, AbstractCoordinateOperation will try to get the parameter values from
                             * the MathTransform. This is the appropriate thing to do at least for Affine operation.
                             */
                            return;
                        }
                        /*
                         * The parameter value of the inverse operation is (or is presumed to be) the negative of
                         * the parameter value of the source operation.  We need to preserve units of measurement
                         * if they were specified.
                         */
                        final ParameterValue<?> tgt = copy.parameter(descriptor.getName().getCode());
                        final Unit<?> unit = src.getUnit();
                        if (unit != null) {
                            tgt.setValue(-src.doubleValue(), unit);
                        } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
                            tgt.setValue(-src.intValue());
                        } else {
                            tgt.setValue(-src.doubleValue());
                        }
                        // No need to add 'tgt' to 'copy' since it was done by the call to copy.parameter(…).
                        continue;
                    }
                }
            }
            copy.values().add(gp);
        }
        target.put(ReferencingServices.PARAMETERS_KEY, copy);
    }
}
