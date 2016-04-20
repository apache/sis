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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterNameException;
import org.apache.sis.internal.jaxb.referencing.CC_OperationParameterGroup;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;

import static org.apache.sis.util.Utilities.deepEquals;


/**
 * The definition of a group of related parameters used by an operation method.
 * {@code DefaultParameterDescriptorGroup} instances are immutable and thus thread-safe.
 *
 * <div class="section">Instantiation</div>
 * Parameter descriptors are usually pre-defined by the SIS library and available through the following methods:
 *
 * <ul>
 *   <li>{@link org.apache.sis.referencing.operation.DefaultOperationMethod#getParameters()}</li>
 * </ul>
 *
 * If nevertheless a {@code ParameterDescriptorGroup} needs to be instantiated directly,
 * then the {@link ParameterBuilder} class may make the task easier.
 *
 * <div class="note"><b>Example:</b>
 * The following example declares the parameters for a <cite>Mercator (variant A)</cite> projection method
 * valid from 80°S to 84°N on all the longitude range (±180°).
 *
 * {@preformat java
 *     class Mercator {
 *         static final ParameterDescriptorGroup PARAMETERS;
 *         static {
 *             ParameterBuilder builder = new ParameterBuilder();
 *             builder.setCodeSpace(Citations.EPSG, "EPSG").setRequired(true);
 *             ParameterDescriptor<?>[] parameters = {
 *                 builder.addName("Latitude of natural origin")    .createBounded( -80,  +84, 0, NonSI.DEGREE_ANGLE),
 *                 builder.addName("Longitude of natural origin")   .createBounded(-180, +180, 0, NonSI.DEGREE_ANGLE),
 *                 builder.addName("Scale factor at natural origin").createStrictlyPositive(1, Unit.ONE),
 *                 builder.addName("False easting")                 .create(0, SI.METRE),
 *                 builder.addName("False northing")                .create(0, SI.METRE)
 *             };
 *             builder.addIdentifier("9804")                    // Primary key in EPSG database.
 *                    .addName("Mercator (variant A)")          // EPSG name since October 2010.
 *                    .addName("Mercator (1SP)")                // EPSG name prior October 2010.
 *                    .addName(Citations.OGC, "Mercator_1SP");  // Name found in some OGC specifications.
 *             PARAMETERS = builder.createGroup(parameters);
 *         }
 *     }
 * }
 * </div>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 *
 * @see DefaultParameterValueGroup
 * @see DefaultParameterDescriptor
 */
@XmlType(name = "OperationParameterGroupType")
@XmlRootElement(name = "OperationParameterGroup")
public class DefaultParameterDescriptorGroup extends AbstractParameterDescriptor implements ParameterDescriptorGroup {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6058599597772994456L;

    /**
     * The {@linkplain #descriptors() parameter descriptors} for this group.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setDescriptors(GeneralParameterDescriptor[])}</p>
     *
     * @see #descriptors()
     */
    private List<GeneralParameterDescriptor> descriptors;

    /**
     * Constructs a parameter group from a set of properties. The properties map is given unchanged to the
     * {@linkplain AbstractParameterDescriptor#AbstractParameterDescriptor(Map, int, int) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
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
     *     <td>{@value org.opengis.metadata.Identifier#DESCRIPTION_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getDescription()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param properties    The properties to be given to the new parameter group.
     * @param minimumOccurs The {@linkplain #getMinimumOccurs() minimum number of times} that values
     *                      for this parameter group are required, or 0 if no restriction.
     * @param maximumOccurs The {@linkplain #getMaximumOccurs() maximum number of times} that values
     *                      for this parameter group are required, or {@link Integer#MAX_VALUE} if no restriction.
     * @param parameters    The {@linkplain #descriptors() parameter descriptors} for this group.
     *
     * @throws InvalidParameterNameException If a parameter name is duplicated.
     */
    public DefaultParameterDescriptorGroup(final Map<String,?> properties,
            final int minimumOccurs, final int maximumOccurs, GeneralParameterDescriptor... parameters)
    {
        super(properties, minimumOccurs, maximumOccurs);
        ArgumentChecks.ensureNonNull("parameters", parameters);
        verifyNames(properties, parameters = parameters.clone());
        descriptors = asList(parameters);
    }

    /**
     * Constructs a group with the same parameters than another group. This is a convenience constructor for
     * operations that expect the same parameters than another operation, but perform a different process.
     *
     * <div class="note"><b>Example:</b>
     * the various <cite>"Coordinate Frame Rotation"</cite> variants (EPSG codes 1032, 1038 and 9607)
     * expect the same parameters than their <cite>"Position Vector transformation"</cite> counterpart
     * (EPSG codes 1033, 1037 and 9606) but perform the rotation in the opposite direction.</div>
     *
     * @param properties The properties to be given to the new parameter group.
     * @param parameters The existing group from which to copy the {@linkplain #descriptors() parameter descriptors}.
     *
     * @since 0.7
     */
    public DefaultParameterDescriptorGroup(final Map<String,?> properties, final ParameterDescriptorGroup parameters) {
        super(properties, parameters.getMinimumOccurs(), parameters.getMaximumOccurs());
        descriptors = parameters.descriptors();    // We will share the same instance if it is safe.
        if (!(parameters instanceof DefaultParameterDescriptorGroup)
            || ((DefaultParameterDescriptorGroup) parameters).descriptors != descriptors)
        {
            // Note sure where the list come from, we are better to copy its content.
            final GeneralParameterDescriptor[] p = descriptors.toArray(new GeneralParameterDescriptor[descriptors.size()]);
            verifyNames(properties, p);
            descriptors = asList(p);
        }
    }

    /**
     * Creates a mandatory parameter group without cloning the given array. This constructor shall
     * be used only when we know that the given array is already a copy of the user-provided array.
     */
    DefaultParameterDescriptorGroup(final Map<String,?> properties, final GeneralParameterDescriptor[] parameters) {
        super(properties, 1, 1);
        verifyNames(properties, parameters);
        descriptors = asList(parameters);
    }

    /**
     * Ensures that the given name array does not contain duplicate values.
     *
     * @param properties The properties given to the constructor, or {@code null} if unknown.
     */
    private static void verifyNames(final Map<String,?> properties, final GeneralParameterDescriptor[] parameters) {
        for (int i=0; i<parameters.length; i++) {
            final GeneralParameterDescriptor parameter = parameters[i];
            ArgumentChecks.ensureNonNullElement("parameters", i, parameter);
            final String name = parameter.getName().getCode();
            for (int j=0; j<i; j++) {
                if (IdentifiedObjects.isHeuristicMatchForName(parameters[j], name)) {
                    throw new InvalidParameterNameException(Errors.getResources(properties).getString(
                            Errors.Keys.DuplicatedParameterName_4, Verifier.getDisplayName(parameters[j]), j, name, i),
                            name);
                }
            }
        }
    }

    /**
     * Creates a new descriptor with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one or a
     * user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param descriptor The descriptor to shallow copy.
     *
     * @see #castOrCopy(ParameterDescriptorGroup)
     */
    protected DefaultParameterDescriptorGroup(final ParameterDescriptorGroup descriptor) {
        super(descriptor);
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
                asSet = s = new HashSet<GeneralParameterDescriptor>(this); // No synchronization: not a big problem if created twice.
            }
            return s.contains(object);
        }
    }

    /**
     * Returns a SIS group implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultParameterDescriptorGroup castOrCopy(final ParameterDescriptorGroup object) {
        return (object == null) || (object instanceof DefaultParameterDescriptorGroup)
                ? (DefaultParameterDescriptorGroup) object : new DefaultParameterDescriptorGroup(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code ParameterDescriptorGroup.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code ParameterDescriptorGroup}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their own
     * set of interfaces.</div>
     *
     * @return {@code ParameterDescriptorGroup.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends ParameterDescriptorGroup> getInterface() {
        return ParameterDescriptorGroup.class;
    }

    /**
     * Returns all parameters in this group.
     *
     * @return The parameter descriptors in this group.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<GeneralParameterDescriptor> descriptors() {
        return descriptors;     // Unmodifiable.
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
        // Quick search for an exact match.
        ArgumentChecks.ensureNonNull("name", name);
        for (final GeneralParameterDescriptor param : descriptors) {
            if (name.equals(param.getName().getCode())) {
                return param;
            }
        }
        // More costly search before to give up.
        GeneralParameterDescriptor fallback = null, ambiguity = null;
        for (final GeneralParameterDescriptor param : descriptors) {
            if (IdentifiedObjects.isHeuristicMatchForName(param, name)) {
                if (fallback == null) {
                    fallback = param;
                } else {
                    ambiguity = param;
                }
            }
        }
        if (fallback != null && ambiguity == null) {
            return fallback;
        }
        throw new ParameterNotFoundException(ambiguity != null
                ? Errors.format(Errors.Keys.AmbiguousName_3,
                        IdentifiedObjects.toString(fallback.getName()),
                        IdentifiedObjects.toString(ambiguity.getName()), name)
                : Errors.format(Errors.Keys.ParameterNotFound_2, Verifier.getDisplayName(this), name), name);
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
                    return descriptors.equals(((DefaultParameterDescriptorGroup) object).descriptors);
                }
                default: {
                    return deepEquals(descriptors(), ((ParameterDescriptorGroup) object).descriptors(), mode);
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
     * Constructs a new object in which every attributes are set to a null value or an empty list.
     * <strong>This is not a valid object.</strong> This constructor is strictly reserved to JAXB
     * and to {@link DefaultParameterValueGroup}, which will assign values later.
     *
     * @see #setDescriptors(GeneralParameterDescriptor[])
     */
    DefaultParameterDescriptorGroup() {
        descriptors = Collections.emptyList();
    }

    /**
     * Invoked by JAXB for getting the parameters to marshal.
     */
    @XmlElement(name = "parameter", required = true)
    private GeneralParameterDescriptor[] getDescriptors() {
        final List<GeneralParameterDescriptor> descriptors = descriptors();     // Give to user a chance to override.
        return descriptors.toArray(new GeneralParameterDescriptor[descriptors.size()]);
    }

    /**
     * Invoked by JAXB for setting the unmarshalled parameter descriptors.
     */
    private void setDescriptors(final GeneralParameterDescriptor[] parameters) {
        if (descriptors.isEmpty()) {
            verifyNames(null, parameters);
            descriptors = asList(parameters);
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultParameterValue.class, "setDescriptors", "parameter");
        }
    }

    /**
     * Merges the given parameter descriptors with the descriptors currently in this group.
     * The descriptors are set twice during {@link DefaultParameterValueGroup} unmarshalling:
     *
     * <ol>
     *   <li>First, the descriptors are set during unmarshalling of this {@code DefaultParameterDescriptorGroup}.
     *       But the value class of {@code ParameterDescriptor} components are unknown because this information
     *       is not part of GML.</li>
     *   <li>Next, this method is invoked during unmarshalling of the {@code DefaultParameterValueGroup} enclosing
     *       element with the descriptors found inside the {@code ParameterValue} components. The later do have the
     *       {@code valueClass} information, so we want to use them in replacement of descriptors of step 1.</li>
     * </ol>
     *
     * @param fromValues Descriptors declared in the {@code ParameterValue} instances of a {@code ParameterValueGroup}.
     * @param replacements An {@code IdentityHashMap} where to store the replacements that the caller needs
     *                     to apply in the {@code GeneralParameterValue} instances.
     */
    final void merge(GeneralParameterDescriptor[] fromValues,
            final Map<GeneralParameterDescriptor,GeneralParameterDescriptor> replacements)
    {
        fromValues = CC_OperationParameterGroup.merge(descriptors, fromValues, replacements);
        verifyNames(null, fromValues);
        descriptors = asList(fromValues);
    }
}
