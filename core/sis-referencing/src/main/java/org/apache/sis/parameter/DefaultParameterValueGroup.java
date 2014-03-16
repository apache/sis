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
import java.io.Serializable;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterCardinalityException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Debug;

import static org.apache.sis.referencing.IdentifiedObjects.isHeuristicMatchForName;

// Related to JDK7
import java.util.Objects;


/**
 * A group of related parameter values. {@code ParameterValueGroup} instances are typically created by calls to
 * <code>descriptor.{@linkplain DefaultParameterDescriptorGroup#createValue() createValue()}</code> on a descriptor
 * supplied by a map projection or process provider. New instances are initialized with a {@linkplain #values() list
 * of values} containing all mandatory parameters, and no optional parameter. The values list is modifiable, but all
 * operations will first ensure that the modification would not violate the cardinality constraints (i.e. the minimum
 * and maximum occurrences of that parameter allowed by the descriptor). If a cardinality constraint is violated, then
 * an {@link InvalidParameterCardinalityException} will be thrown.
 *
 * <p>After a new {@code ParameterValueGroup} instance has been created, the parameter values can be set by chaining
 * calls to {@link #parameter(String)} with one of the {@code setValue(…)} methods defined in the returned object
 * (see the {@linkplain DefaultParameterValue table of setter methods}). The {@code parameter(String)} method can
 * be invoked regardless of whether the parameter is mandatory or optional: if the parameter was optional and not
 * yet present in this group, it will be created.</p>
 *
 * <div class="note"><b>Example:</b>
 * Assuming the descriptor defined in the {@link DefaultParameterDescriptorGroup} example,
 * one can set <cite>Mercator (variant A)</cite> projection parameters as below:
 *
 * {@preformat java
 *     ParameterValueGroup mercator = Mercator.PARAMETERS.createValue();
 *     mercator.parameter("Longitude of natural origin").setValue(-60, NonSI.DEGREE_ANGLE);  // 60°W
 *     mercator.parameter("Latitude of natural origin") .setValue( 40, NonSI.DEGREE_ANGLE);  // 40°N
 *     // Keep default values for other parameters.
 * }
 * </div>
 *
 * Alternatively, if all parameters were created elsewhere and the user wants to copy them in a new
 * parameter group, the {@link List#addAll(Collection)} method can been invoked on the values list.
 *
 * <div class="note"><b>Example:</b>
 * {@preformat java
 *     ParameterValue<?>[] parameter = ...; // Defined elsewhere.
 *     ParameterValueGroup mercator = Mercator.PARAMETERS.createValue();
 *     mercator.values().addAll(Arrays.asList(parameters));
 * }
 * </div>
 *
 * Optional parameters can be removed by the usual {@link List#remove(int)} or {@link List#remove(Object)}
 * operations on the values list. But attempts to remove a mandatory parameter will cause an
 * {@link InvalidParameterCardinalityException} to be thrown.
 *
 * <p>Calls to {@code values().clear()} restore this {@code DefaultParameterValueGroup} to its initial state.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 *
 * @see DefaultParameterDescriptorGroup
 * @see DefaultParameterValue
 */
public class DefaultParameterValueGroup implements ParameterValueGroup, Serializable, Cloneable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1985309386356545126L;

    /**
     * Contains the descriptor and the {@linkplain #values() parameter values} for this group.
     *
     * <p>Consider this field as final. It is not for the purpose of {@link #clone()}.</p>
     */
    private ParameterValueList values;

    /**
     * Constructs a parameter group from the specified descriptor.
     *
     * <p><b>Usage note:</b> {@code ParameterValueGroup} are usually not instantiated directly. Instead, consider
     * invoking <code>descriptor.{@linkplain DefaultParameterDescriptorGroup#createValue() createValue()}</code>
     * on a descriptor supplied by a map projection or process provider.</p>
     *
     * @param descriptor The descriptor for this group.
     */
    public DefaultParameterValueGroup(final ParameterDescriptorGroup descriptor) {
        ArgumentChecks.ensureNonNull("descriptor", descriptor);
        values = new ParameterValueList(descriptor);
    }

    /**
     * Returns the abstract definition of this group of parameters.
     *
     * @return The abstract definition of this group of parameters.
     */
    @Override
    public ParameterDescriptorGroup getDescriptor() {
        return values.descriptor;
    }

    /**
     * Returns the values in this group. The returned list is <cite>live</cite>:
     * changes in this list are reflected on this {@code ParameterValueGroup}, and conversely.
     *
     * {@section Restrictions}
     * All write operations must comply to the following conditions:
     *
     * <ul>
     *   <li>Parameters added to the list shall have one of the descriptors listed by {@link #getDescriptor()}.</li>
     *   <li>Adding or removing parameters shall not violate the parameter cardinality constraints.</li>
     * </ul>
     *
     * The list will verify those conditions and throws {@link org.opengis.parameter.InvalidParameterNameException},
     * {@link org.opengis.parameter.InvalidParameterCardinalityException} or other runtime exceptions if a condition
     * is not meet.
     */
    @Override
    public List<GeneralParameterValue> values() {
        return values;
    }

    /**
     * Returns the value in this group for the specified name.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If this group contains a parameter value of the given name, then that parameter is returned.</li>
     *   <li>Otherwise if a {@linkplain DefaultParameterDescriptorGroup#descriptor(String) descriptor} of the
     *       given name exists, then a new {@code ParameterValue} instance is
     *       {@linkplain DefaultParameterDescriptor#createValue() created}, added to this group and returned.</li>
     *   <li>Otherwise a {@code ParameterNotFoundException} is thrown.</li>
     * </ul>
     *
     * This convenience method provides a way to get and set parameter values by name.
     * For example the following idiom fetches a floating point value for the <cite>False easting</cite>
     * and <cite>False northing</cite> parameters and set a new value for the <cite>False easting</cite> one:
     *
     * {@preformat java
     *     double easting  = parameter("False easting" ).doubleValue();
     *     double northing = parameter("False northing").doubleValue();
     *     parameter("False easting").setValue(500000.0);
     * }
     *
     * <div class="note"><b>API note:</b> there is no <code>parameter<b><u>s</u></b>(String)</code> method
     * returning a list of parameter values because the ISO 19111 standard fixes the {@code ParameterValue}
     * {@linkplain DefaultParameterDescriptor#getMaximumOccurs() maximum occurrence} to 1.</div>
     *
     * {@section Parameters subgroups}
     * This method does not search recursively in subgroups. This is because more than one subgroup
     * may exist for the same {@linkplain ParameterDescriptorGroup descriptor}. The user have to
     * {@linkplain #groups(String) query all subgroups} and select explicitly the appropriate one.
     *
     * @param  name The name of the parameter to search for.
     * @return The parameter value for the given name.
     * @throws ParameterNotFoundException if there is no parameter value for the given name.
     */
    @Override
    public ParameterValue<?> parameter(final String name) throws ParameterNotFoundException {
        ArgumentChecks.ensureNonNull("name", name);
        final ParameterValueList values = this.values; // Protect against accidental changes.

        // Quick search for an exact match.
        final int size = values.size();
        for (int i=0; i<size; i++) {
            final GeneralParameterDescriptor descriptor = values.descriptor(i);
            if (descriptor instanceof ParameterDescriptor<?>) {
                if (name.equals(descriptor.getName().toString())) {
                    return (ParameterValue<?>) values.get(i);
                }
            }
        }
        // More costly search before to give up.
        int fallback = -1, ambiguity = -1;
        for (int i=0; i<size; i++) {
            final GeneralParameterDescriptor descriptor = values.descriptor(i);
            if (descriptor instanceof ParameterDescriptor<?>) {
                if (isHeuristicMatchForName(descriptor, name)) {
                    if (fallback < 0) {
                        fallback = i;
                    } else {
                        ambiguity = i;
                    }
                }
            }
        }
        if (fallback >= 0) {
            if (ambiguity < 0) {
                return (ParameterValue<?>) values.get(fallback);
            }
            throw new ParameterNotFoundException(Errors.format(Errors.Keys.AmbiguousName_3,
                    values.descriptor(fallback).getName(), values.descriptor(ambiguity).getName(), name), name);
        }
        /*
         * No existing parameter found. The parameter may be optional. Check if a descriptor exists.
         * If such a descriptor is found, create the parameter, add it to the values list and returns it.
         */
        final GeneralParameterDescriptor descriptor = values.descriptor.descriptor(name);
        if (descriptor instanceof ParameterDescriptor<?> && descriptor.getMaximumOccurs() != 0) {
            final ParameterValue<?> value = ((ParameterDescriptor<?>) descriptor).createValue();
            values.addUnchecked(value);
            return value;
        }
        throw new ParameterNotFoundException(Errors.format(Errors.Keys.ParameterNotFound_2,
                values.descriptor.getName(), name), name);
    }

    /**
     * Returns all subgroups with the specified name.
     *
     * <p>This method do not create new groups: if the requested group is optional (i.e.
     * <code>{@linkplain DefaultParameterDescriptor#getMinimumOccurs() minimumOccurs} == 0</code>)
     * and no value were defined previously, then this method returns an empty set.</p>
     *
     * @param  name The name of the parameter to search for.
     * @return The set of all parameter group for the given name.
     * @throws ParameterNotFoundException If no descriptor was found for the given name.
     */
    @Override
    public List<ParameterValueGroup> groups(final String name) throws ParameterNotFoundException {
        ArgumentChecks.ensureNonNull("name", name);
        final ParameterValueList values = this.values; // Protect against accidental changes.
        final List<ParameterValueGroup> groups = new ArrayList<>(4);
        final int size = values.size();
        for (int i=0; i<size; i++) {
            final GeneralParameterDescriptor descriptor = values.descriptor(i);
            if (descriptor instanceof ParameterDescriptorGroup) {
                if (isHeuristicMatchForName(descriptor, name)) {
                    groups.add((ParameterValueGroup) values.get(i));
                }
            }
        }
        /*
         * No groups were found. Check if the group actually exists (i.e. is declared in the
         * descriptor). If it doesn't exists, then an exception is thrown. If it exists (i.e.
         * it is simply an optional group not yet defined), then returns an empty list.
         */
        if (groups.isEmpty()) {
            final ParameterDescriptorGroup descriptor = values.descriptor;
            if (!(descriptor.descriptor(name) instanceof ParameterDescriptorGroup)) {
                throw new ParameterNotFoundException(Errors.format(
                        Errors.Keys.ParameterNotFound_2, descriptor.getName(), name), name);
            }
        }
        return groups;
    }

    /**
     * Creates a new subgroup of the specified name, and adds it to the list of subgroups.
     * The argument shall be the name of a {@linkplain DefaultParameterDescriptorGroup descriptor group}
     * which is a child of this group.
     *
     * <div class="note"><b>API note:</b>
     * There is no {@code removeGroup(String)} method. To remove a group, users shall inspect the
     * {@link #values()} list, decide which occurrences to remove if there is many of them for the
     * same name, and whether to iterate recursively into sub-groups or not.</div>
     *
     * @param  name The name of the parameter group to create.
     * @return A newly created parameter group for the given name.
     * @throws ParameterNotFoundException If no descriptor was found for the given name.
     * @throws InvalidParameterCardinalityException If this parameter group already contains the
     *         {@linkplain ParameterDescriptorGroup#getMaximumOccurs() maximum number of occurrences}
     *         of subgroups of the given name.
     */
    @Override
    public ParameterValueGroup addGroup(final String name)
            throws ParameterNotFoundException, InvalidParameterCardinalityException
    {
        final ParameterValueList values = this.values; // Protect against accidental changes.
        final ParameterDescriptorGroup descriptor = values.descriptor;
        final GeneralParameterDescriptor child = descriptor.descriptor(name);
        if (!(child instanceof ParameterDescriptorGroup)) {
            throw new ParameterNotFoundException(Errors.format(
                    Errors.Keys.ParameterNotFound_2, descriptor.getName(), name), name);
        }
        final ParameterValueGroup value = ((ParameterDescriptorGroup) child).createValue();
        values.add(value);
        return value;
    }

    /**
     * Compares the specified object with this parameter for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && getClass() == object.getClass()) {
            final DefaultParameterValueGroup that = (DefaultParameterValueGroup) object;
            return Objects.equals(values.descriptor, that.values.descriptor) &&
                   Objects.equals(values, that.values);
        }
        return false;
    }

    /**
     * Returns a hash value for this parameter.
     *
     * @return The hash code value. This value doesn't need to be the same
     *         in past or future versions of this class.
     */
    @Override
    public int hashCode() {
        return values.descriptor.hashCode() ^ values.hashCode();
    }

    /**
     * Returns a deep copy of this group of parameter values.
     * Included parameter values and subgroups are cloned recursively.
     *
     * @return A copy of this group of parameter values.
     */
    @Override
    @SuppressWarnings("unchecked")
    public DefaultParameterValueGroup clone() {
        final DefaultParameterValueGroup copy;
        try {
            copy = (DefaultParameterValueGroup) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        copy.values = new ParameterValueList(copy.values);
        return copy;
    }

    /**
     * Returns a string representation of this group.
     * The default implementation delegates to {@link ParameterFormat}.
     *
     * <p>This method is for information purpose only and may change in future SIS version.</p>
     */
    @Debug
    @Override
    public String toString() {
        return ParameterFormat.sharedFormat(this);
    }

    /**
     * Prints a string representation of this group to the {@linkplain System#out standard output stream}.
     * If a {@linkplain java.io.Console console} is attached to the running JVM (i.e. if the application
     * is run from the command-line and the output is not redirected to a file) and if Apache SIS thinks
     * that the console supports the ANSI escape codes (a.k.a. X3.64), then a syntax coloring will be applied.
     *
     * <p>This is a convenience method for debugging purpose and for console applications.</p>
     */
    @Debug
    public void print() {
        ParameterFormat.print(this);
    }
}
