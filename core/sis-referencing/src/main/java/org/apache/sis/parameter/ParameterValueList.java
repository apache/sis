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
package org.apache.sis.parameter;

import java.util.List;
import java.util.AbstractList;
import java.util.RandomAccess;
import java.io.Serializable;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.parameter.InvalidParameterCardinalityException;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.util.resources.Errors;


/**
 * The list to be returned by {@link DefaultParameterValueGroup#values()}.
 * This class performs checks on the parameter values to be added or removed.
 * This implementation supports {@code add(…)} and {@code remove(…)} operations.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.1)
 * @version 0.4
 * @module
 */
final class ParameterValueList extends AbstractList<GeneralParameterValue> implements RandomAccess, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7446077551686135264L;

    /**
     * The descriptor for the list as a whole.
     */
    final ParameterDescriptorGroup descriptor;

    /**
     * The unchecked list of parameter values for this list.
     */
    final List<GeneralParameterValue> values;

    /**
     * Constructs a parameter list.
     *
     * @param descriptor The descriptor for this list.
     * @param values The parameter values for this list.
     */
    ParameterValueList(final ParameterDescriptorGroup descriptor, final List<GeneralParameterValue> values) {
        this.descriptor = descriptor;
        this.values     = values;
    }

    /*
     * CAUTION: Some methods are NOT forwarded to 'values', and this is on purpose!
     *          This include all modification methods (add, set, remove, etc.).
     *          We must rely on the default AbstractList implementation for them.
     */
    @Override public boolean               isEmpty    ()         {return values.isEmpty    ( );}
    @Override public int                   size       ()         {return values.size       ( );}
    @Override public GeneralParameterValue get        (int i)    {return values.get        (i);}
    @Override public int                   indexOf    (Object o) {return values.indexOf    (o);}
    @Override public int                   lastIndexOf(Object o) {return values.lastIndexOf(o);}
    @Override public boolean               equals     (Object o) {return values.equals     (o);}
    @Override public int                   hashCode   ()         {return values.hashCode   ( );}
    @Override public String                toString   ()         {return values.toString   ( );}

    /**
     * Adds a {@link ParameterValue} or an other {@link ParameterValueGroup} to this list.
     * If an existing parameter is already included for the same name and adding the new
     * parameter would increase the number past what is allowable by {@code maximumOccurs},
     * then an {@link IllegalStateException} will be thrown.
     *
     * @param  parameter New parameter to be added to this group.
     * @return {@code true} if this object changed as a result of this call.
     * @throws IllegalArgumentException if the specified parameter is not allowable by the groups descriptor.
     * @throws InvalidParameterCardinalityException if adding this parameter would result in more parameters
     *         than allowed by {@code maximumOccurs}.
     */
    @Override
    public boolean add(final GeneralParameterValue parameter) {
        final GeneralParameterDescriptor type = parameter.getDescriptor();
        final ReferenceIdentifier name = type.getName();
        final List<GeneralParameterDescriptor> descriptors = descriptor.descriptors();
        if (!descriptors.contains(type)) {
            /*
             * For a more accurate error message, check if the operation failed because the
             * parameter name was not found, or the parameter descriptor does not matches.
             */
            for (final GeneralParameterDescriptor descriptor : descriptors) {
                if (name.equals(descriptor.getName())) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.MismatchedParameterDescriptor_1, name));
                }
            }
            throw new InvalidParameterNameException(Errors.format(
                    Errors.Keys.ParameterNotFound_2, descriptor.getName(), name), name.getCode());
        }
        /*
         * Before to add the parameter, check the cardinality.
         */
        int count = 0;
        for (final GeneralParameterValue value : values) {
            if (name.equals(value.getDescriptor().getName())) {
                count++;
            }
        }
        if (count >= type.getMaximumOccurs()) {
            throw new InvalidParameterCardinalityException(Errors.format(
                    Errors.Keys.TooManyOccurrences_2, count, name), name.getCode());
        }
        modCount++;
        return values.add(parameter);
    }

    /**
     * Removes the value at the specified index, provided that this removal is allowed by the
     * parameter cardinality.
     *
     * @param  index The index of the value to remove.
     * @return The value removed at the given index.
     */
    @Override
    public GeneralParameterValue remove(final int index) {
        final GeneralParameterDescriptor type = values.get(index).getDescriptor();
        final int min = type.getMinimumOccurs();
        if (min != 0) {
            int count = 0;
            final ReferenceIdentifier name = type.getName();
            for (final GeneralParameterValue value : values) {
                if (name.equals(value.getDescriptor().getName())) {
                    if (++count > min) break;
                }
            }
            if (count <= min) {
                throw new InvalidParameterCardinalityException(Errors.format(
                        Errors.Keys.TooFewOccurrences_2, min, name), name.getCode());
            }
        }
        modCount++;
        return values.remove(index);
    }
}
