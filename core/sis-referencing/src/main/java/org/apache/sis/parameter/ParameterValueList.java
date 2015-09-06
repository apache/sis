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
import java.util.Arrays;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.parameter.InvalidParameterCardinalityException;
import org.opengis.metadata.Identifier;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.IdentifiedObjects;

// Related to JDK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * The list to be returned by {@link DefaultParameterValueGroup#values()}.
 * This class performs checks on the parameter values to be added or removed.
 * This implementation supports {@code set(…)}, {@code add(…)} and {@code remove(…)} operations.
 *
 * <p><b>Implementation note:</b> this class reproduces some {@link java.util.ArrayList} functionalities.
 * However we do <strong>not</strong> extend {@code ArrayList} because we really need the default method
 * implementations provided by {@code AbstractList} — the optimizations performed by {@code ArrayList}
 * are not suitable here.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
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
     *
     * <p>This descriptor shall not be used in {@link #equals(Object)} and {@link #hashCode()}
     * implementations in order to stay consistent with the {@link List} contract.</p>
     */
    final ParameterDescriptorGroup descriptor;

    /**
     * The parameter values in the group. The length of this array is the list capacity.
     * This array will growth as needed.
     */
    private GeneralParameterValue[] values;

    /**
     * Number of valid elements in the {@link #values} array.
     */
    private int size;

    /**
     * Constructs an initially empty parameter list.
     *
     * @param descriptor The descriptor for this list.
     */
    ParameterValueList(final ParameterDescriptorGroup descriptor) {
        this.descriptor = descriptor;
        final List<GeneralParameterDescriptor> elements = descriptor.descriptors();
        values = new GeneralParameterValue[elements.size()];
        initialize(elements);
    }

    /**
     * Constructs a parameter list initialized to a copy of the given one.
     */
    ParameterValueList(final ParameterValueList other) {
        descriptor = other.descriptor;
        values = new GeneralParameterValue[size = other.size];
        for (int i=0; i<size; i++) {
            values[i] = other.values[i].clone();
        }
    }

    /**
     * Adds all mandatory parameters to this list. This method can been invoked only after
     * construction or after a call to {@link #clear()}.
     */
    private void initialize(final List<GeneralParameterDescriptor> elements) {
        for (final GeneralParameterDescriptor child : elements) {
            for (int count=child.getMinimumOccurs(); --count>=0;) {
                addUnchecked(new UninitializedParameter(child));
            }
        }
    }

    /**
     * Clears this list, then recreate the mandatory parameters.
     */
    @Override
    public void clear() {
        Arrays.fill(values, 0, size, null);
        size = 0;
        initialize(descriptor.descriptors());
    }

    /**
     * Returns the number of parameters in this list.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns the descriptor at the given index. This method is preferable to {@code get(i).getDescriptor()}
     * when the caller does not need the replacement of {@link UninitializedParameter} instances.
     */
    final GeneralParameterDescriptor descriptor(final int index) {
        return values[index].getDescriptor();
    }

    /**
     * Returns the parameter value at the given index. If the parameter at the given index is a
     * mandatory parameter pending creation of the actual value, the value will be created now.
     */
    @Override
    public GeneralParameterValue get(int index) {
        ArgumentChecks.ensureValidIndex(size, index);
        GeneralParameterValue value = values[index];
        if (value instanceof UninitializedParameter) {
            values[index] = value = value.getDescriptor().createValue();
        }
        return value;
    }

    /**
     * Sets the parameter at the given index. The descriptor of the given parameter must be one of those
     * in the {@link DefaultParameterDescriptorGroup#descriptors()} list, and storing that parameter must
     * be allowed by the cardinality constraints.
     */
    @Override
    public GeneralParameterValue set(final int index, final GeneralParameterValue parameter) {
        ArgumentChecks.ensureValidIndex(size, index);
        final GeneralParameterValue value = values[index];
        ArgumentChecks.ensureNonNull("parameter", parameter);
        final GeneralParameterDescriptor desc = parameter.getDescriptor();
        if (!value.getDescriptor().equals(desc)) {
            ensureDescriptorExists(desc);
            ensureCanRemove(desc);
            ensureCanAdd(desc);
        }
        values[index] = parameter;
        return value;
    }

    /**
     * Adds a {@link ParameterValue} or an other {@link ParameterValueGroup} to this list.
     * If an existing parameter is already included for the same name and adding the new
     * parameter would increase the number past what is allowable by {@code maximumOccurs},
     * then an {@link InvalidParameterCardinalityException} will be thrown.
     *
     * @param  parameter New parameter to be added to this group.
     * @return Always {@code true} since this object changes as a result of this call.
     * @throws IllegalArgumentException if the specified parameter is not allowable by the groups descriptor.
     * @throws InvalidParameterCardinalityException if adding this parameter would result in more parameters
     *         than allowed by {@code maximumOccurs}.
     */
    @Override
    public boolean add(final GeneralParameterValue parameter) {
        ArgumentChecks.ensureNonNull("parameter", parameter);
        final GeneralParameterDescriptor desc = parameter.getDescriptor();
        ensureDescriptorExists(desc);
        /*
         * If we had an uninitialized parameter (a parameter created by the DefaultParameterValueGroup constructor
         * and never been queried or set by the user), then the given parameter will replace the uninitialized.
         * The intend is to allow users to set its own parameters by a call to group.values().addAll(myParam).
         * Otherwise the given parameter will be added, in which case we need to check the cardinality.
         */
        final Identifier name = desc.getName();
        int count = 0;
        for (int i=0; i<size; i++) {
            final GeneralParameterValue value = values[i];
            if (name.equals(value.getDescriptor().getName())) {
                if (value instanceof UninitializedParameter) {
                    values[i] = parameter;
                    return true;
                }
                count++;
            }
        }
        final int max = desc.getMaximumOccurs();
        if (count >= max) {
            throw new InvalidParameterCardinalityException(Errors.format(
                    Errors.Keys.TooManyOccurrences_2, max, name), name.getCode());
        }
        addUnchecked(parameter);
        modCount++;
        return true;
    }

    /**
     * Unconditionally adds the given parameter to this list without any validity check.
     * The internal array will growth as needed.
     */
    final void addUnchecked(final GeneralParameterValue parameter) {
        if (size == values.length) {
            values = Arrays.copyOf(values, size*2);
        }
        values[size++] = parameter;
    }

    /**
     * Verifies the given descriptor exists in the {@link DefaultParameterDescriptorGroup#descriptors()} list.
     */
    final void ensureDescriptorExists(final GeneralParameterDescriptor desc) {
        final List<GeneralParameterDescriptor> descriptors = descriptor.descriptors();
        if (!descriptors.contains(desc)) {
            /*
             * For a more accurate error message, check if the operation failed because the
             * parameter name was not found, or the parameter descriptor does not matches.
             */
            final Identifier name = desc.getName();
            final String code = name.getCode();
            for (final GeneralParameterDescriptor descriptor : descriptors) {
                if (IdentifiedObjects.isHeuristicMatchForName(descriptor, code)) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.MismatchedParameterDescriptor_1, name));
                }
            }
            throw new InvalidParameterNameException(Errors.format(Errors.Keys.ParameterNotFound_2,
                    Verifier.getDisplayName(descriptor), name), code);
        }
    }

    /**
     * Verifies if adding a parameter with the given descriptor is allowed by the cardinality constraints. If adding
     * the parameter would result in more occurrences than {@link DefaultParameterDescriptor#getMaximumOccurs()},
     * then this method throws an {@link InvalidParameterCardinalityException}.
     */
    private void ensureCanAdd(final GeneralParameterDescriptor desc) {
        final Identifier name = desc.getName();
        int count = 0;
        for (int i=0; i<size; i++) {
            if (name.equals(values[i].getDescriptor().getName())) {
                count++;
            }
        }
        final int max = desc.getMaximumOccurs();
        if (count >= max) {
            throw new InvalidParameterCardinalityException(Errors.format(
                    Errors.Keys.TooManyOccurrences_2, max, name), name.getCode());
        }
    }

    /**
     * Verifies if removing the given value is allowed by the cardinality constraints. If removing the parameter
     * would result in less occurrences than {@link DefaultParameterDescriptor#getMinimumOccurs()},
     * then this method throws an {@link InvalidParameterCardinalityException}.
     */
    private void ensureCanRemove(final GeneralParameterDescriptor desc) {
        final int min = desc.getMinimumOccurs();
        if (min != 0) { // Optimization for a common case.
            final Identifier name = desc.getName();
            int count = 0;
            for (int i=0; i<size; i++) {
                if (name.equals(values[i].getDescriptor().getName())) {
                    if (++count > min) {
                        return;
                    }
                }
            }
            throw new InvalidParameterCardinalityException(Errors.format(
                    Errors.Keys.TooFewOccurrences_2, min, name), name.getCode());
        }
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
        ArgumentChecks.ensureValidIndex(size, index);
        final GeneralParameterValue value = values[index];
        ensureCanRemove(value.getDescriptor());
        System.arraycopy(values, index + 1, values, index, --size - index);
        values[size] = null;
        modCount++;
        return value;
    }

    /**
     * Returns the parameters in an array.
     */
    @Override
    public GeneralParameterValue[] toArray() {
        return Arrays.copyOf(values, size);
    }

    /**
     * Returns a string representation of this list.
     */
    @Override
    public String toString() {
        if (size == 0) {
            return "[]";
        }
        final String lineSeparator = JDK7.lineSeparator();
        final StringBuilder buffer = new StringBuilder();
        for (int i=0; i<size; i++) {
            buffer.append(values[i]).append(lineSeparator);
        }
        return buffer.toString();
    }

    /**
     * Trims the array to its capacity before to serialize.
     *
     * @param  out The output stream where to serialize this object.
     * @throws IOException If an I/O error occurred while writing.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        values = ArraysExt.resize(values, size);
        out.defaultWriteObject();
    }
}
