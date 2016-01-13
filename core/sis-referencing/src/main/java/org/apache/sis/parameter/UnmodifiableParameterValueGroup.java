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
import java.util.ArrayList;
import java.util.Map;
import java.util.IdentityHashMap;
import java.io.Serializable;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.UnmodifiableArrayList;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * A parameter value group which can not be modified. This is especially important for parameters of
 * <cite>defining conversions</cite> since the same instance can be used for various source and target CRS.
 * Since {@link org.apache.sis.referencing.factory.sql.EPSGFactory} caches the {@code Conversion} instances,
 * unexpected behavior results if the parameters of a cached conversion have been modified, for example with
 * the addition of {@code semi_major} and {@code semi_minor} parameters by {@code DefaultMathTransformFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class UnmodifiableParameterValueGroup extends Parameters implements LenientComparable, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7980778116268449513L;

    /**
     * The parameter descriptor.
     *
     * @see #getDescriptor()
     */
    private final ParameterDescriptorGroup descriptor;

    /**
     * The parameter values.
     *
     * @see #values()
     */
    private final List<GeneralParameterValue> values;

    /**
     * Creates a new unmodifiable parameter group.
     *
     * @param group The group of values to copy.
     * @param done  An initially empty map used for protection against circular references.
     *
     * @see #create(ParameterValueGroup)
     */
    private UnmodifiableParameterValueGroup(final ParameterValueGroup group, final Map<ParameterValueGroup,Boolean> done) {
        if (done.put(group, Boolean.TRUE) != null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.CircularReference));
        }
        descriptor = group.getDescriptor();
        final List<GeneralParameterValue> values = group.values();
        final GeneralParameterValue[] array = new GeneralParameterValue[values.size()];
        for (int i=0; i<array.length; i++) {
            GeneralParameterValue value = values.get(i);
            ArgumentChecks.ensureNonNullElement("values", i, value);
            if (value instanceof ParameterValue<?>) {
                value = UnmodifiableParameterValue.create((ParameterValue<?>) value);
            } else if (value instanceof ParameterValueGroup) {
                value = new UnmodifiableParameterValueGroup((ParameterValueGroup) value, done);
            }
            array[i] = value;
        }
        this.values = UnmodifiableArrayList.wrap(array);
    }

    /**
     * Creates a new unmodifiable parameter group.
     *
     * @param  group The group of values to copy, or {@code null}.
     * @return The unmodifiable parameter group, or {@code null} if the given argument was null.
     */
    static UnmodifiableParameterValueGroup create(final ParameterValueGroup group) {
        if (group == null || group instanceof UnmodifiableParameterValueGroup) {
            return (UnmodifiableParameterValueGroup) group;
        }
        return new UnmodifiableParameterValueGroup(group, new IdentityHashMap<ParameterValueGroup,Boolean>(4));
    }

    /**
     * Returns the abstract definition of this group of parameters.
     */
    @Override
    public ParameterDescriptorGroup getDescriptor() {
        return descriptor;
    }

    /**
     * Returns the values in this group.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<GeneralParameterValue> values() {
        return values;                                  // Unmodifiable
    }

    /**
     * Specifies that this class does not override {@link #parameter(String)} in a way incompatible with
     * {@link #parameterIfExist(String)}.
     */
    @Override
    boolean isKnownImplementation() {
        return true;
    }

    /**
     * Returns the value in this group for the specified name.
     */
    @Override
    public ParameterValue<?> parameter(final String name) throws ParameterNotFoundException {
        ArgumentChecks.ensureNonNull("name", name);
        final ParameterValue<?> value = parameterIfExist(name);
        if (value != null) {
            return value;
        }
        throw new ParameterNotFoundException(Errors.format(Errors.Keys.ParameterNotFound_2,
                Verifier.getDisplayName(descriptor), name), name);
    }

    /**
     * Returns all subgroups with the specified name.
     */
    @Override
    public List<ParameterValueGroup> groups(final String name) throws ParameterNotFoundException {
        ArgumentChecks.ensureNonNull("name", name);
        final List<ParameterValueGroup> groups = new ArrayList<ParameterValueGroup>(4);
        for (final GeneralParameterValue value : values) {
            if (value instanceof ParameterValueGroup) {
                if (IdentifiedObjects.isHeuristicMatchForName(value.getDescriptor(), name)) {
                    groups.add((ParameterValueGroup) value);
                }
            }
        }
        if (groups.isEmpty()) {
            if (!(descriptor.descriptor(name) instanceof ParameterDescriptorGroup)) {
                throw new ParameterNotFoundException(Errors.format(Errors.Keys.ParameterNotFound_2,
                        Verifier.getDisplayName(descriptor), name), name);
            }
        }
        return groups;
    }

    /**
     * Operation not allowed.
     */
    @Override
    public ParameterValueGroup addGroup(final String name) throws IllegalStateException {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, ParameterValueGroup.class));
    }

    /**
     * Returns a modifiable copy of this parameter value group.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Parameters clone() {
        final DefaultParameterValueGroup copy = new DefaultParameterValueGroup(descriptor);
        Parameters.copy(this, copy);
        return copy;
    }

    /**
     * Compares the specified object with this parameter for equality.
     * The strictness level is controlled by the second argument:
     *
     * <ul>
     *   <li>{@link ComparisonMode#STRICT} and {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}
     *       take in account the parameter order.</li>
     *   <li>{@link ComparisonMode#IGNORE_METADATA} and {@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE}
     *       ignore the order of parameter values (but not necessarily the order of parameter descriptors).</li>
     * </ul>
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both objects are equal according the given comparison mode.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            // Slight optimization
            return true;
        }
        if (object != null) {
            if (mode == ComparisonMode.STRICT) {
                if (getClass() == object.getClass()) {
                    final UnmodifiableParameterValueGroup that = (UnmodifiableParameterValueGroup) object;
                    return Objects.equals(descriptor, that.descriptor) &&
                           Objects.equals(values,     that.values);
                }
            } else if (object instanceof ParameterValueGroup) {
                return DefaultParameterValueGroup.equals(this, (ParameterValueGroup) object, mode);
            }
        }
        return false;
    }

    /**
     * Compares the specified object with this parameter for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public final boolean equals(final Object object) {
        return equals(object, ComparisonMode.STRICT);
    }

    /**
     * Returns a hash value for this parameter.
     */
    @Override
    public int hashCode() {
        return descriptor.hashCode() ^ values.hashCode();
    }
}
