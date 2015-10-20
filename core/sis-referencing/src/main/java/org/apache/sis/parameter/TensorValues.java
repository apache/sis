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

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.reflect.Array;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.referencing.operation.Matrix;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Classes;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * The values for a group of tensor parameters. This value group is extensible, i.e. the number of
 * <code>"elt_<var>row</var>_<var>col</var>"</code> parameters depends on the {@code "num_row"} and
 * {@code "num_col"} parameter values. Consequently, this {@code ParameterValueGroup} is also its own
 * mutable {@code ParameterDescriptorGroup}.
 *
 * @param <E> The type of tensor element values.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@XmlTransient
final class TensorValues<E> extends AbstractParameterDescriptor
        implements ParameterDescriptorGroup, ParameterValueGroup, Cloneable
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7747712999115044943L;

    /**
     * A provider of descriptors for matrix parameters. This object is used like a collection of
     * {@link ParameterDescriptor}s, even if it does not implement any standard collection API.
     *
     * @see TensorParameters#descriptor(ParameterDescriptorGroup, String, int[])
     * @see TensorParameters#getAllDescriptors(int[])
     */
    private final TensorParameters<E> descriptors;

    /**
     * The parameter for the number of row, columns and other dimensions in the tensor.
     */
    private final ParameterValue<Integer>[] dimensions;

    /**
     * The parameter values. Each array element is itself an {@code ParameterValue} array,
     * and so on until we have nested {@link TensorParameters#rank()} arrays.
     *
     * <p>Will be constructed only when first requested.
     * May be resized at any moment if a {@link #dimensions} parameter value change.</p>
     */
    private Object[] values;

    /**
     * Constructs a new group of tensor parameters for the given properties.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    TensorValues(final Map<String,?> properties, final TensorParameters<E> descriptors) {
        super(properties, 1, 1);
        this.descriptors = descriptors;
        dimensions = new ParameterValue[descriptors.rank()];
        for (int i=0; i<dimensions.length; i++) {
            dimensions[i] = descriptors.getDimensionDescriptor(i).createValue();
        }
    }

    /**
     * Constructs a copy of the given matrix parameters.
     * If {@code clone} is true, the new group will be a clone of the given group.
     * If {@code clone} is false, the new group will be initialized to default values.
     */
    TensorValues(final TensorValues<E> other, final boolean clone) {
        super(other);
        descriptors = other.descriptors;
        dimensions = other.dimensions.clone();
        for (int i=0; i<dimensions.length; i++) {
            final ParameterValue<Integer> dim = dimensions[i];
            dimensions[i] = clone ? dim.clone() : dim.getDescriptor().createValue();
        }
        if (clone) {
            values = clone(other.values);
        }
    }

    /**
     * Clones the given array of parameters.
     * This method invokes itself for cloning sub-arrays.
     */
    private static Object[] clone(Object[] values) {
        if (values != null) {
            values = values.clone();
            for (int i=0; i<values.length; i++) {
                Object element = values[i];
                if (element instanceof GeneralParameterValue) {
                    element = ((GeneralParameterValue) element).clone();
                } else {
                    element = clone((Object[]) element);
                }
                values[i] = element;
            }
        }
        return values;
    }

    /**
     * Returns a clone of this group.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public ParameterValueGroup clone() {
        return new TensorValues<E>(this, true);
    }

    /**
     * Returns a new group initialized to default values.
     */
    @Override
    public ParameterValueGroup createValue() {
        return new TensorValues<E>(this, false);
    }

    /**
     * Returns a description of this parameter value group. Returns always {@code this}, since
     * the description depends on {@code "num_row"} and {@code "num_col"} parameter values.
     */
    @Override
    public ParameterDescriptorGroup getDescriptor() {
        return this;
    }

    /**
     * Returns the parameters descriptors in this group. The amount of parameters depends
     * on the value of {@code "num_row"} and {@code "num_col"} parameters.
     */
    @Override
    public List<GeneralParameterDescriptor> descriptors() {
        return UnmodifiableArrayList.<GeneralParameterDescriptor>wrap(descriptors.getAllDescriptors(size()));
    }

    /**
     * Returns the current tensor size for each dimensions.
     */
    private int[] size() {
        final int[] indices = new int[dimensions.length];
        for (int i=0; i<indices.length; i++) {
            indices[i] = dimensions[i].intValue();
        }
        return indices;
    }

    /**
     * Returns the parameter descriptor in this group for the specified name.
     *
     * @param  name The name of the parameter to search for.
     * @return The parameter descriptor for the given name.
     * @throws ParameterNotFoundException if there is no parameter for the given name.
     */
    @Override
    public GeneralParameterDescriptor descriptor(String name) throws ParameterNotFoundException {
        name = CharSequences.trimWhitespaces(name);
        ArgumentChecks.ensureNonEmpty("name", name);
        return descriptors.descriptor(this, name, size());
    }

    /**
     * Returns the parameter value in this group for the specified name.
     *
     * @param  name The name of the parameter to search for.
     * @return The parameter value for the given name.
     * @throws ParameterNotFoundException if there is no parameter for the given name.
     */
    @Override
    public ParameterValue<?> parameter(String name) throws ParameterNotFoundException {
        name = CharSequences.trimWhitespaces(name);
        ArgumentChecks.ensureNonEmpty("name", name);
        IllegalArgumentException cause = null;
        int[] indices = null;
        try {
            indices = descriptors.nameToIndices(name);
        } catch (IllegalArgumentException exception) {
            cause = exception;
        }
        if (indices != null) {
            final int[] actualSize = size();
            if (TensorParameters.isInBounds(indices, actualSize)) {
                return parameter(indices, actualSize);
            }
        }
        /*
         * The given name is not a matrix (or tensor) element name.
         * Verify if the requested parameters is one of those that
         * specify the matrix/tensor size ("num_row" or "num_col").
         */
        final int rank = descriptors.rank();
        for (int i=0; i<rank; i++) {
            final ParameterDescriptor<Integer> param = descriptors.getDimensionDescriptor(i);
            if (IdentifiedObjects.isHeuristicMatchForName(param, name)) {
                return dimensions[i];
            }
        }
        throw (ParameterNotFoundException) new ParameterNotFoundException(Errors.format(
                Errors.Keys.ParameterNotFound_2, getName(), name), name).initCause(cause);
    }

    /**
     * Returns the tensor element at the given indices.
     */
    private ParameterValue<E> parameter(final int[] indices, final int[] actualSize) {
        final int rank = dimensions.length;
        if (indices.length != rank) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UnexpectedArrayLength_2, rank, indices.length));
        }
        /*
         * At the end of the following loop, 'element' will be the TensorValues element
         * and 'parent' will be the array which contain it at index indices[rank - 1].
         */
        Object[] parent = null;
        Object element = values;
        for (int i=0; i<rank; i++) {
            if (element == null) {
                /*
                 * Creates new arrays only when first needed.
                 * For rank 2, creates ParameterValue[][];
                 * For rank 3, creates ParameterValue[][][];
                 * etc.
                 */
                final Class<?> componentType = Classes.changeArrayDimension(ParameterValue.class, rank - i - 1);
                element = Array.newInstance(componentType, actualSize[i]);
                if (parent != null) {
                    parent[indices[i-1]] = element;
                } else {
                    values = (Object[]) element;
                }
            } else {
                /*
                 * If we already have an array, makes sure that its length is sufficient. Note that the array
                 * could also be too long if the user reduced some tensor dimensions. We do not trim too long
                 * arrays in order to avoid inconsistent behavior if the user later brings back the sensor
                 * dimension to its old value. The inconsistent behavior would be to discard the references to
                 * existing values above 'actualSize[i]', because we would have some sequences of operations
                 * that discard them and some other sequences of operations that do not discard them.
                 * The easiest strategy is to never discard those extraneous references - may not be ideal,
                 * but at least it keep the behavior consistent for all sequences of operations.
                 */
                if (((Object[]) element).length <= indices[i]) {
                    element = Arrays.copyOf((Object[]) element, actualSize[i]);
                    parent[indices[i-1]] = element;
                }
            }
            parent = (Object[]) element;
            element = parent[indices[i]];
        }
        if (element == null) {
            element = descriptors.getElementDescriptor(indices).createValue();
            parent[indices[rank - 1]] = element;
        }
        return Parameters.cast((ParameterValue<?>) element, descriptors.getElementType());
    }

    /**
     * Returns the parameters values in this group. The amount of parameters depends on the value of
     * {@code "num_row"} and {@code "num_col"} parameters. The parameter array will contains only
     * matrix elements which have been requested at least once by one of {@code parameter(…)} methods.
     * Never requested elements are left to their default value and omitted from the returned array.
     */
    @Override
    public List<GeneralParameterValue> values() {
        final List<GeneralParameterValue> addTo = new ArrayList<GeneralParameterValue>();
        for (final ParameterValue<Integer> dimension : dimensions) {
            if (!isOmitted(dimension)) {
                addTo.add(dimension);
            }
        }
        addValues(values, size(), 0, addTo);
        return Collections.unmodifiableList(addTo);
    }

    /**
     * Implementation of {@link #values()} which adds parameter values to the given list.
     * This method invokes itself recursively.
     */
    private static void addValues(final Object[] values, final int[] actualSize, int j,
            final List<GeneralParameterValue> addTo)
    {
        if (values != null) {
            final int length = Math.min(values.length, actualSize[j]);
            if (++j != actualSize.length) {
                for (int i=0; i<length; i++) {
                    addValues((Object[]) values[i], actualSize, j, addTo);
                }
            } else {
                for (int i=0; i<length; i++) {
                    final ParameterValue<?> parameter = (ParameterValue<?>) values[i];
                    if (parameter != null && !isOmitted(parameter)) {
                        addTo.add(parameter);
                    }
                }
            }
        }
    }

    /**
     * Returns {@code true} if the given parameter can be omitted. A parameter can be omitted
     * if it is not mandatory and has a value equals to the default value.
     */
    private static boolean isOmitted(final ParameterValue<?> parameter) {
        final Object value = parameter.getValue();
        if (value == null) { // Implies that the default value is also null.
            return true;
        }
        final ParameterDescriptor<?> descriptor = parameter.getDescriptor();
        return descriptor.getMinimumOccurs() == 0 && value.equals(descriptor.getDefaultValue());
    }

    /**
     * Always throws an exception since this group does not contain subgroups.
     */
    @Override
    public List<ParameterValueGroup> groups(final String name) throws ParameterNotFoundException {
        throw new ParameterNotFoundException(Errors.format(Errors.Keys.ParameterNotFound_2, getName(), name), name);
    }

    /**
     * Always throws an exception since this group does not contain subgroups.
     */
    @Override
    public ParameterValueGroup addGroup(String name) throws ParameterNotFoundException, IllegalStateException {
        throw new ParameterNotFoundException(Errors.format(Errors.Keys.ParameterNotFound_2, getName(), name), name);
    }

    /**
     * Creates a matrix from this group of parameters.
     * This operation is allowed only for tensors of {@linkplain #rank() rank} 2.
     *
     * @return A matrix created from this group of parameters.
     */
    final Matrix toMatrix() {
        final int numRow = dimensions[0].intValue();
        final int numCol = dimensions[1].intValue();
        final Matrix matrix = Matrices.createDiagonal(numRow, numCol);
        if (values != null) {
            for (int j=0; j<numRow; j++) {
                final ParameterValue<?>[] row = (ParameterValue<?>[]) values[j];
                if (row != null) {
                    for (int i=0; i<numCol; i++) {
                        final ParameterValue<?> element = row[i];
                        if (element != null) {
                            matrix.setElement(j, i, element.doubleValue());
                        }
                    }
                }
            }
        }
        return matrix;
    }

    /**
     * Sets all parameter values to the element value in the specified matrix.
     * After this method call, {@link #values} will returns only the elements
     * different from the default value.
     *
     * @param matrix The matrix to copy in this group of parameters.
     */
    final void setMatrix(final Matrix matrix) {
        final int numRow = matrix.getNumRow();
        final int numCol = matrix.getNumCol();
        dimensions[0].setValue(numRow);
        dimensions[1].setValue(numCol);
        values = null;
        final int[] indices = new int[2];
        for (int j=0; j<numRow; j++) {
            indices[0] = j;
            ParameterValue<?>[] row = null;
            for (int i=0; i<numCol; i++) {
                indices[1] = i;
                ParameterDescriptor<E> descriptor = descriptors.getElementDescriptor(indices);
                final E def = descriptor.getDefaultValue();
                final double element = matrix.getElement(j,i);
                if (!(def instanceof Number) || !Numerics.equalsIgnoreZeroSign(element, ((Number) def).doubleValue())) {
                    final ParameterValue<?> value = descriptor.createValue();
                    value.setValue(element);
                    if (row == null) {
                        row = new ParameterValue<?>[numCol];
                        if (values == null) {
                            values = new ParameterValue<?>[numRow][];
                        }
                        values[j] = row;
                    }
                    row[i] = value;
                }
            }
        }
    }

    /**
     * Compares this object with the specified one for equality.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        if (super.equals(object, mode)) {
            final TensorValues<?> that = (TensorValues<?>) object;
            return Utilities.deepEquals(descriptors, that.descriptors, mode) &&
                   Utilities.deepEquals(values(),    that.values(),    mode);
        }
        return false;
    }

    /**
     * Invoked by {@link #hashCode()} for computing the hash code when first needed.
     *
     * @return {@inheritDoc}
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + descriptors.hashCode();
        // Do not use any field other than descriptors, because they are not immutable.
    }

    /**
     * Formats this group as a pseudo-<cite>Well Known Text</cite> element.
     *
     * @param  formatter The formatter where to format the inner content of this WKT element.
     * @return {@code "ParameterGroup"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        WKTUtilities.appendParamMT(this, formatter);
        return WKTKeywords.ParameterGroup;
    }
}
