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
import javax.xml.bind.annotation.XmlTransient;
import javax.measure.unit.Unit;
import org.opengis.metadata.Identifier;
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
import org.apache.sis.util.Deprecable;


/**
 * Description of the inverse of another method. This class should be used only when no operation is defined
 * for the inverse, or when the inverse operation can not be represented by inverting the sign of parameters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
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
                final Map<String,Object> properties = new HashMap<String,Object>(6);
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
     * If the inverse of the given operation can be represented by inverting the sign of all numerical
     * parameter values, copies those parameters in a {@code "parameters"} entry in the given map.
     * Otherwise does nothing.
     *
     * @param source  the operation for which to get the inverse parameters.
     * @param target  where to store the inverse parameters.
     */
    static void putParameters(final SingleOperation source, final Map<String,Object> target) {
        final boolean isInvertible = isInvertible(source.getMethod());
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
                        boolean isOpposite = (remarks == SignReversalComment.OPPOSITE);
                        if (!isOpposite) {
                            /*
                             * If the parameter descriptor does not contain an information about whether the
                             * inverse operation uses values of opposite sign or not, use heuristic rules.
                             */
                            if (!isInvertible) {
                                return;                 // Can not create inverse parameter values - abandon.
                            }
                            final Comparable<?> minimum = descriptor.getMinimumValue();
                            isOpposite = (minimum == null || (minimum instanceof Number && ((Number) minimum).doubleValue() < 0));
                        }
                        if (isOpposite) {
                            final ParameterValue<?> tgt = copy.parameter(descriptor.getName().getCode());
                            final Unit<?> unit = src.getUnit();
                            if (unit != null) {
                                tgt.setValue(-src.doubleValue(), unit);
                            } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
                                tgt.setValue(-src.intValue());
                            } else {
                                tgt.setValue(-src.doubleValue());
                            }
                            continue;
                        }
                    }
                }
            }
            copy.values().add(gp);
        }
        target.put(ReferencingServices.PARAMETERS_KEY, copy);
    }
}
