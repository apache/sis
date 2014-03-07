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
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Collections;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterNameException;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;

import static org.apache.sis.util.Utilities.deepEquals;


/**
 * The definition of a group of related parameters used by an operation method.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 *
 * @see DefaultParameterValueGroup
 * @see DefaultParameterDescriptor
 */
public class DefaultParameterDescriptorGroup extends AbstractIdentifiedObject implements ParameterDescriptorGroup {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4613190550542423839L;

    /**
     * The minimum number of times that values for this parameter group are required.
     */
    private final int minimumOccurs;

    /**
     * The maximum number of times that values for this parameter group are required.
     */
    private final int maximumOccurs;

    /**
     * The {@linkplain #descriptors() parameter descriptors} for this group.
     */
    private final List<GeneralParameterDescriptor> descriptors;

    /**
     * Constructs a parameter group from a set of properties. The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param properties    The properties to be given to the identified object.
     * @param minimumOccurs The {@linkplain #getMinimumOccurs() minimum number of times}
     *                      that values for this parameter group are required.
     * @param maximumOccurs The {@linkplain #getMaximumOccurs() maximum number of times}
     *                      that values for this parameter group are required.
     * @param parameters    The {@linkplain #descriptors() parameter descriptors} for this group.
     */
    public DefaultParameterDescriptorGroup(final Map<String,?> properties,
            final int minimumOccurs, final int maximumOccurs, GeneralParameterDescriptor... parameters)
    {
        super(properties);
        this.minimumOccurs = minimumOccurs;
        this.maximumOccurs = maximumOccurs;
        if (minimumOccurs < 0  || minimumOccurs > maximumOccurs) {
            throw new IllegalArgumentException(Errors.getResources(properties).getString(
                    Errors.Keys.IllegalRange_2, minimumOccurs, maximumOccurs));
        }
        ArgumentChecks.ensureNonNull("parameters", parameters);
        parameters = parameters.clone();
        for (int i=0; i<parameters.length; i++) {
            ArgumentChecks.ensureNonNullElement("parameters", i, parameters);
            final String name = parameters[i].getName().getCode();
            for (int j=0; j<i; j++) {
                if (IdentifiedObjects.isHeuristicMatchForName(parameters[j], name)) {
                    throw new InvalidParameterNameException(Errors.getResources(properties).getString(
                            Errors.Keys.DuplicatedParameterName_4, parameters[j].getName().getCode(), j, name, i),
                            name);
                }
            }
        }
        descriptors = asList(parameters);
    }

    /**
     * Creates a new descriptor with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one or a
     * user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param descriptor The descriptor to shallow copy.
     */
    protected DefaultParameterDescriptorGroup(final ParameterDescriptorGroup descriptor) {
        super(descriptor);
        minimumOccurs = descriptor.getMinimumOccurs();
        maximumOccurs = descriptor.getMaximumOccurs();
        final List<GeneralParameterDescriptor> c = descriptor.descriptors();
        if (descriptor instanceof DefaultParameterDescriptorGroup &&
                ((DefaultParameterDescriptorGroup) descriptor).descriptors == c)
        {
            descriptors = c; // Share the immutable instance (no need to clone).
        } else {
            descriptors = asList(c.toArray(new GeneralParameterDescriptor[c.size()]));
        }
    }

    /**
     * Returns the given array of parameters as an unmodifiable list.
     */
    private static List<GeneralParameterDescriptor> asList(final GeneralParameterDescriptor[] parameters) {
        switch (parameters.length) {
            case 0:  return Collections.emptyList();
            case 1:  return Collections.singletonList(parameters[0]);
            case 2:  // fall through
            case 3:  return UnmodifiableArrayList.wrap(parameters);
            default: return new AsList(parameters);
        }
    }

    /**
     * The {@link DefaultParameterDescriptorGroup#descriptors} as an unmodifiable list.
     * This class overrides {@link #contains(Object)} with a faster implementation based on {@link HashSet}.
     * This optimizations is helpful for map projection implementations, which test often for a parameter validity.
     */
    private static final class AsList extends UnmodifiableArrayList<GeneralParameterDescriptor> {
        /** For compatibility with different versions. */
        private static final long serialVersionUID = -2116304004367396735L;

        /** The element as a set, created when first needed. */
        private transient volatile Set<GeneralParameterDescriptor> asSet;

        /** Constructs a list for the specified array. */
        public AsList(final GeneralParameterDescriptor[] array) {
            super(array);
        }

        /** Tests for the inclusion of the specified descriptor. */
        @Override public boolean contains(final Object object) {
            Set<GeneralParameterDescriptor> s = asSet;
            if (s == null) {
                asSet = s = new HashSet<>(this); // No synchronization: not a big problem if created twice.
            }
            return s.contains(object);
        }
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code ParameterDescriptorGroup.class}.
     *
     * {@note Subclasses usually do not need to override this method since GeoAPI does not define
     *        <code>ParameterDescriptorGroup</code> sub-interface. Overriding possibility is left mostly
     *        for implementors who wish to extend GeoAPI with their own set of interfaces.}
     *
     * @return {@code ParameterDescriptorGroup.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends ParameterDescriptorGroup> getInterface() {
        return ParameterDescriptorGroup.class;
    }

    /**
     * The minimum number of times that values for this parameter group or parameter are required.
     * A value of 0 means an optional parameter.
     *
     * @return The minimum occurrence.
     */
    @Override
    public int getMinimumOccurs() {
        return minimumOccurs;
    }

    /**
     * The maximum number of times that values for this parameter group are required.
     *
     * @return The maximum occurrence.
     */
    @Override
    public int getMaximumOccurs() {
        return maximumOccurs;
    }

    /**
     * Creates a new instance of {@linkplain DefaultParameterValueGroup parameter value group}
     * initialized with the {@linkplain DefaultParameterDescriptor#getDefaultValue default values}.
     * The {@linkplain DefaultParameterValueGroup#getDescriptor() parameter descriptor} for the
     * created group will be {@code this} object.
     *
     * @return A new parameter instance initialized to the default value.
     */
    @Override
    public ParameterValueGroup createValue() {
        return new DefaultParameterValueGroup(this);
    }

    /**
     * Returns all parameters in this group.
     *
     * @return The parameter descriptors in this group.
     */
    @Override
    public List<GeneralParameterDescriptor> descriptors() {
        return descriptors;
    }

    /**
     * Returns the first parameter in this group for the specified name.
     * This method does not search in sub-groups.
     *
     * @param  name The name of the parameter to search for.
     * @return The parameter for the given identifier name.
     * @throws ParameterNotFoundException if there is no parameter for the given name.
     */
    @Override
    @SuppressWarnings("null")
    public GeneralParameterDescriptor descriptor(final String name) throws ParameterNotFoundException {
        ArgumentChecks.ensureNonNull("name", name);
        GeneralParameterDescriptor fallback = null, ambiguity = null;
        for (final GeneralParameterDescriptor param : descriptors) {
            if (IdentifiedObjects.isHeuristicMatchForName(param, name)) {
                if (name.equals(param.getName().getCode())) {
                    return param;
                } else if (fallback == null) {
                    fallback = param;
                } else {
                    ambiguity = param;
                }
            }
        }
        throw new ParameterNotFoundException(ambiguity != null
                ? Errors.format(Errors.Keys.AmbiguousName_3, fallback.getName(), ambiguity.getName(), name)
                : Errors.format(Errors.Keys.ParameterNotFound_2, getName(), name), name);
    }

    /**
     * Compares the specified object with this parameter group for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) { // Optimization for a common case.
            return true;
        }
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    final DefaultParameterDescriptorGroup that = (DefaultParameterDescriptorGroup) object;
                    return minimumOccurs == that.minimumOccurs &&
                           maximumOccurs == that.maximumOccurs &&
                           descriptors.equals(that.descriptors);
                }
                default: {
                    final ParameterDescriptorGroup that = (ParameterDescriptorGroup) object;
                    return getMinimumOccurs() == that.getMinimumOccurs() &&
                           getMaximumOccurs() == that.getMaximumOccurs() &&
                           deepEquals(descriptors(), that.descriptors(), mode);
                }
            }
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
    }
}
