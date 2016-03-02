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
import java.util.LinkedList;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterCardinalityException;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * A group of related parameter values. Parameter groups have some similarities with {@code java.util.Map}:
 *
 * <ul>
 *   <li>{@link #parameter(String)} is similar in purpose to {@link java.util.Map#get(Object)},
 *       with an additional level of indirection in both the argument and the return value.</li>
 *   <li>{@link #values()} is similar in purpose to {@link java.util.Map#entrySet()},
 *       with {@code ParameterValue} playing a role similar to {@code Map.Entry}.</li>
 * </ul>
 *
 * <div class="section">Instantiation and validity constraints</div>
 * {@code ParameterValueGroup} instances are typically created by calls to
 * <code>descriptor.{@linkplain DefaultParameterDescriptorGroup#createValue() createValue()}</code> on a descriptor
 * supplied by a coordinate operation or process provider. New instances are initialized with a {@linkplain #values()
 * list of values} containing all mandatory parameters, and no optional parameter. The values list is modifiable, but
 * all methods will first ensure that the modification would not violate the cardinality constraints (i.e. the minimum
 * and maximum occurrences of that parameter allowed by the descriptor). If a cardinality constraint is violated, then
 * an {@link InvalidParameterCardinalityException} will be thrown.
 *
 * <div class="section">Setting the parameter values</div>
 * After a new {@code ParameterValueGroup} instance has been created, the parameter values can be set by chaining
 * calls to {@link #parameter(String)} with one of the {@code setValue(…)} methods defined in the returned object
 * (see the {@linkplain DefaultParameterValue table of setter methods}). The {@code parameter(String)} method can
 * be invoked regardless of whether the parameter is mandatory or optional: if the parameter was optional and not
 * yet present in this group, it will be created.
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
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see DefaultParameterDescriptorGroup
 * @see DefaultParameterValue
 */
@XmlType(name = "ParameterValueGroupType", propOrder = {
    "values",
    "descriptor"
})
@XmlRootElement(name = "ParameterValueGroup")
public class DefaultParameterValueGroup extends Parameters implements LenientComparable, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1985309386356545126L;

    /**
     * Contains the descriptor and the {@linkplain #values() parameter values} for this group.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only by the {@link #clone()} method and
     * at unmarshalling time by {@link #setValues(GeneralParameterValue[])}</p>
     *
     * @see #values()
     */
    private ParameterValueList values;

    /**
     * Creates a parameter group from the specified descriptor.
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
     * Creates a new instance initialized with all values from the specified parameter group.
     * This is a <em>shallow</em> copy constructor, since the values contained in the given
     * group is not cloned.
     *
     * @param parameters The parameters to copy values from.
     *
     * @see #clone()
     *
     * @since 0.6
     */
    public DefaultParameterValueGroup(final ParameterValueGroup parameters) {
        ArgumentChecks.ensureNonNull("parameters", parameters);
        values = new ParameterValueList(parameters.getDescriptor());
        values.addAll(parameters.values());
    }

    /**
     * Conservatively returns {@code false} if this instance is for a subclass, because we do not know if the
     * subclass overrides {@link #parameter(String)} in a way incompatible with {@link #parameterIfExist(String)}.
     * (note: using {@code Class.getMethod(…).getDeclaringClass()} is presumed not worth the cost.
     */
    @Override
    boolean isKnownImplementation() {
        return getClass() == DefaultParameterValueGroup.class;
    }

    /**
     * Returns the abstract definition of this group of parameters.
     *
     * @return The abstract definition of this group of parameters.
     */
    @Override
    @XmlElement(name = "group")
    public ParameterDescriptorGroup getDescriptor() {
        // The descriptor is not allowed to be null, but this situation
        // may exist temporarily during XML unmarshalling.
        return (values != null) ? values.descriptor : null;
    }

    /**
     * Returns the values in this group. The returned list is <cite>live</cite>:
     * changes in this list are reflected on this {@code ParameterValueGroup}, and conversely.
     *
     * <div class="section">Restrictions</div>
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
     *
     * @return The values in this group.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<GeneralParameterValue> values() {
        return values;                                          // Intentionally modifiable.
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
     * <div class="section">Parameters subgroups</div>
     * This method does not search recursively in subgroups. This is because more than one subgroup
     * may exist for the same {@linkplain ParameterDescriptorGroup descriptor}. The user have to
     * {@linkplain #groups(String) query all subgroups} and select explicitly the appropriate one.
     *
     * @param  name The name of the parameter to search for.
     * @return The parameter value for the given name.
     * @throws ParameterNotFoundException if there is no parameter value for the given name.
     *
     * @see #getValue(ParameterDescriptor)
     */
    @Override
    public ParameterValue<?> parameter(final String name) throws ParameterNotFoundException {
        ArgumentChecks.ensureNonNull("name", name);
        ParameterValue<?> value = parameterIfExist(name);
        if (value == null) {
            /*
             * No existing parameter found. Maybe the parameter is optional and not yet created.
             * Get the descriptor of that parameter. If the descriptor is not found, or is not
             * a descriptor for a single parameter (not a group), or the parameter is disabled
             * (maximum occurrence = 0), behaves as if the parameter was not found.
             */
            final GeneralParameterDescriptor descriptor = values.descriptor.descriptor(name);
            if (!(descriptor instanceof ParameterDescriptor<?>) || descriptor.getMaximumOccurs() == 0) {
                throw new ParameterNotFoundException(Errors.format(Errors.Keys.ParameterNotFound_2,
                        Verifier.getDisplayName(values.descriptor), name), name);
            }
            /*
             * Create the optional parameter and add it to our internal list. Note that this is
             * not the only place were a ParameterValue may be created,  so do not extract just
             * this call to 'createValue()' in a user-overrideable method.
             */
            value = ((ParameterDescriptor<?>) descriptor).createValue();
            values.addUnchecked(value);
        }
        return value;
    }

    /**
     * Returns the value in this group for the specified name if it exists, or {@code null} if none.
     * This method does not create any new {@code ParameterValue} instance.
     *
     * @see #isKnownImplementation()
     */
    @Override
    ParameterValue<?> parameterIfExist(final String name) throws ParameterNotFoundException {
        final ParameterValueList values = this.values; // Protect against accidental changes.
        /*
         * Quick search for an exact match. By invoking 'descriptor(i)' instead of 'get(i)',
         * we avoid the creation of mandatory ParameterValue which was deferred. If we find
         * a matching name, the ParameterValue will be lazily created (if not already done)
         * by the call to 'get(i)'.
         */
        final int size = values.size();
        for (int i=0; i<size; i++) {
            final GeneralParameterDescriptor descriptor = values.descriptor(i);
            if (descriptor instanceof ParameterDescriptor<?>) {
                if (name.equals(descriptor.getName().toString())) {
                    return (ParameterValue<?>) values.get(i);
                }
            }
        }
        /*
         * More costly search, including aliases, before to give up.
         */
        int fallback  = -1;
        int ambiguity = -1;
        for (int i=0; i<size; i++) {
            final GeneralParameterDescriptor descriptor = values.descriptor(i);
            if (descriptor instanceof ParameterDescriptor<?>) {
                if (IdentifiedObjects.isHeuristicMatchForName(descriptor, name)) {
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
                return (ParameterValue<?>) values.get(fallback);   // May lazily create a ParameterValue.
            }
            throw new ParameterNotFoundException(Errors.format(Errors.Keys.AmbiguousName_3,
                    IdentifiedObjects.toString(values.descriptor(fallback) .getName()),
                    IdentifiedObjects.toString(values.descriptor(ambiguity).getName()), name), name);
        }
        return null;
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
     * @throws ParameterNotFoundException if no descriptor was found for the given name.
     */
    @Override
    public List<ParameterValueGroup> groups(final String name) throws ParameterNotFoundException {
        ArgumentChecks.ensureNonNull("name", name);
        final ParameterValueList values = this.values; // Protect against accidental changes.
        final List<ParameterValueGroup> groups = new ArrayList<ParameterValueGroup>(4);
        final int size = values.size();
        for (int i=0; i<size; i++) {
            final GeneralParameterDescriptor descriptor = values.descriptor(i);
            if (descriptor instanceof ParameterDescriptorGroup) {
                if (IdentifiedObjects.isHeuristicMatchForName(descriptor, name)) {
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
                throw new ParameterNotFoundException(Errors.format(Errors.Keys.ParameterNotFound_2,
                        Verifier.getDisplayName(descriptor), name), name);
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
     * @throws ParameterNotFoundException if no descriptor was found for the given name.
     * @throws InvalidParameterCardinalityException if this parameter group already contains the
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
                    final DefaultParameterValueGroup that = (DefaultParameterValueGroup) object;
                    return Objects.equals(values.descriptor, that.values.descriptor) &&
                           Objects.equals(values, that.values);
                }
            } else if (object instanceof ParameterValueGroup) {
                return equals(this, (ParameterValueGroup) object, mode);
            }
        }
        return false;
    }

    /**
     * Compares the given objects for equality, ignoring parameter order in "ignore metadata" mode.
     */
    static boolean equals(final Parameters expected, final ParameterValueGroup actual, final ComparisonMode mode) {
        if (!Utilities.deepEquals(expected.getDescriptor(), actual.getDescriptor(), mode)) {
            return false;
        }
        if (!mode.isIgnoringMetadata()) {
            return Utilities.deepEquals(expected.values(), actual.values(), mode);
        }
        final List<GeneralParameterValue> values = new LinkedList<GeneralParameterValue>(expected.values());
scan:   for (final GeneralParameterValue param : actual.values()) {
            final Iterator<GeneralParameterValue> it = values.iterator();
            while (it.hasNext()) {
                if (Utilities.deepEquals(it.next(), param, mode)) {
                    it.remove();
                    continue scan;
                }
            }
            return false;   // A parameter from 'actual' has not been found in 'expected'.
        }
        return values.isEmpty();
    }

    /**
     * Compares the specified object with this parameter for equality.
     * This method is implemented as below:
     *
     * {@preformat java
     *     return equals(other, ComparisonMode.STRICT);
     * }
     *
     * Subclasses shall override {@link #equals(Object, ComparisonMode)} instead than this method.
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
     *
     * @return The hash code value. This value does not need to be the same
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
     *
     * @see #copy(ParameterValueGroup, ParameterValueGroup)
     */
    @Override
    public DefaultParameterValueGroup clone() {
        final DefaultParameterValueGroup copy = (DefaultParameterValueGroup) super.clone();
        copy.values = new ParameterValueList(copy.values);
        return copy;
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Default constructor for JAXB only. The values list is initialized to {@code null},
     * but will be assigned a value after XML unmarshalling.
     */
    private DefaultParameterValueGroup() {
    }

    /**
     * Invoked by JAXB for setting the group parameter descriptor. Those parameter are redundant with
     * the parameters associated to the values given to {@link #setValues(GeneralParameterValue[])},
     * except the the group identification (name, <i>etc.</i>) and for any optional parameters which
     * were not present in the above {@code GeneralParameterValue} array.
     *
     * @see #getDescriptor()
     */
    private void setDescriptor(final ParameterDescriptorGroup descriptor) {
        if (values == null) {
            values = new ParameterValueList(descriptor);
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultParameterValue.class, "setDescriptor", "group");
        }
    }

    /**
     * Invoked by JAXB for getting the parameters to marshal.
     */
    @XmlElement(name = "parameterValue", required = true)
    private GeneralParameterValue[] getValues() {
        final List<GeneralParameterValue> values = values();   // Gives to user a chance to override.
        return values.toArray(new GeneralParameterValue[values.size()]);
    }

    /**
     * Invoked by JAXB for setting the unmarshalled parameters. This method should be invoked last
     * (after {@link #setDescriptor(ParameterDescriptorGroup)}) even if the {@code parameterValue}
     * elements were first in the XML document. This is the case at least with the JAXB reference
     * implementation, because the property type is an array (it would not work with a list).
     *
     * <p><b>Maintenance note:</b> the {@code "setValues"} method name is also hard-coded in
     * {@link org.apache.sis.internal.jaxb.referencing.CC_GeneralOperationParameter} for logging purpose.</p>
     */
    private void setValues(final GeneralParameterValue[] parameters) {
        ParameterValueList addTo = values;
        if (addTo == null) {
            // Should never happen, unless the XML document is invalid and does not have a 'group' element.
            addTo = new ParameterValueList(new DefaultParameterDescriptorGroup());
        }
        /*
         * Merge the descriptors declared in the <gml:group> element with the descriptors given in each
         * <gml:parameterValue> element. The implementation is known to be DefaultParameterDescriptorGroup
         * because this is the type declared in the JAXBContext and in adapters.
         */
        final Map<GeneralParameterDescriptor,GeneralParameterDescriptor> replacements =
                new IdentityHashMap<GeneralParameterDescriptor,GeneralParameterDescriptor>(4);
        ((DefaultParameterDescriptorGroup) addTo.descriptor).merge(getDescriptors(parameters), replacements);
        addTo.clear();  // Because references to parameter descriptors have changed.
        setValues(parameters, replacements, addTo);
    }

    /**
     * Appends all parameter values. In this process, we may need to update the descriptor of some values
     * if those descriptors changed as a result of the above merge process.
     *
     * @param parameters   The parameters to add, or {@code null} for {@link #values}.
     * @param replacements The replacements to apply in the {@code GeneralParameterValue} instances.
     * @param addTo        Where to store the new values.
     */
    @SuppressWarnings({"unchecked", "AssignmentToCollectionOrArrayFieldFromParameter"})
    private void setValues(GeneralParameterValue[] parameters,
            final Map<GeneralParameterDescriptor,GeneralParameterDescriptor> replacements,
            final ParameterValueList addTo)
    {
        if (parameters == null) {
            parameters = values.toArray();
        }
        for (final GeneralParameterValue p : parameters) {
            final GeneralParameterDescriptor replacement = replacements.get(p.getDescriptor());
            if (replacement != null) {
                if (p instanceof DefaultParameterValue<?>) {
                    ((DefaultParameterValue<?>) p).setDescriptor((ParameterDescriptor) replacement);
                } else if (p instanceof DefaultParameterValueGroup) {
                    ((DefaultParameterValueGroup) p).setValues(null, replacements,
                            new ParameterValueList((ParameterDescriptorGroup) replacement));
                }
            }
            addTo.add(p);
        }
        values = addTo;
    }
}
