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
package org.apache.sis.internal.jaxb.referencing;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import javax.xml.bind.annotation.XmlElement;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class CC_OperationParameterGroup extends PropertyType<CC_OperationParameterGroup,ParameterDescriptorGroup> {
    /**
     * Empty constructor for JAXB only.
     */
    public CC_OperationParameterGroup() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code ParameterDescriptorGroup.class}
     */
    @Override
    protected Class<ParameterDescriptorGroup> getBoundType() {
        return ParameterDescriptorGroup.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CC_OperationParameterGroup(final ParameterDescriptorGroup parameter) {
        super(parameter);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:OperationParameterGroup>} XML element.
     *
     * @param  parameter The element to marshall.
     * @return A {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected CC_OperationParameterGroup wrap(final ParameterDescriptorGroup parameter) {
        return new CC_OperationParameterGroup(parameter);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:OperationParameter>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The element to be marshalled.
     */
    @XmlElement(name = "OperationParameterGroup")
    public DefaultParameterDescriptorGroup getElement() {
        return DefaultParameterDescriptorGroup.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param parameter The unmarshalled element.
     */
    public void setElement(final DefaultParameterDescriptorGroup parameter) {
        metadata = parameter;
    }

    /**
     * Invoked by {@link DefaultParameterDescriptorGroup#setDescriptors(GeneralParameterDescriptor[])}
     * for merging into a single set the descriptors which are repeated twice in a GML document.
     *
     * <p>The {@code descriptors} argument gives the descriptors listed explicitely inside a
     * {@code <gml:OperationParameterGroup>} or {@code <gml:OperationMethod>} element. Those
     * descriptors are said "incomplete" (from SIS point of view) because they are missing the
     * {@link ParameterDescriptor#getValueClass()} property, which does not exist in GML but
     * is mandatory for us. However an exception to this "incompleteness" happen when SIS has
     * been able to match the {@code <gml:OperationMethod>} parent to one of the pre-defined
     * operations in the {@link org.apache.sis.internal.referencing.provider} package.</p>
     *
     * <p>The {@code fromValues} argument gives the descriptors declared in each {@code <gml:ParameterValue>}
     * instances of a {@code <gml:ParameterValueGroup>} or {@code <gml:AbstractSingleOperation>} element.
     * Contrarily to the {@code descriptors} argument, the {@code fromValues} instances should have non-null
     * {@link ParameterDescriptor#getValueClass()} property inferred by SIS from the parameter value.</p>
     *
     * <p>So the preferred descriptors from more complete to less complete are:</p>
     * <ol>
     *   <li>{@code descriptors} if and only if they contain pre-defined parameters inferred by SIS from the {@code <gml:OperationMethod>} name.</li>
     *   <li>{@code fromValues}, which contain the descriptors declared in the {@code <gml:ParameterValue>} instances.</li>
     *   <li>{@code descriptors}, which contain the descriptor listed in {@code <gml:OperationParameterGroup>} or {@code <gml:OperationMethod>}.</li>
     * </ol>
     *
     * <div class="note"><b>Note:</b>
     * this code is defined in this {@code CC_OperationParameterGroup} class instead than in the
     * {@link DefaultParameterDescriptorGroup} class in the hope to reduce the amount of code
     * processed by the JVM in the common case where JAXB (un)marshalling is not needed.</div>
     *
     * @param  descriptors  The descriptors declared in the {@code ParameterDescriptorGroup}.
     * @param  fromValues   The descriptors declared in the {@code ParameterValue} instances.
     *                      They are said "valid" because they contain the mandatory {@code valueClass} property.
     * @param  replacements An {@code IdentityHashMap} where to store the replacements that the caller needs
     *                      to apply in the {@code GeneralParameterValue} instances.
     * @return A sequence containing the merged set of parameter descriptors.
     *
     * @see <a href="http://issues.apache.org/jira/browse/SIS-290">SIS-290</a>
     */
    public static GeneralParameterDescriptor[] merge(
            final List<GeneralParameterDescriptor> descriptors,
            final GeneralParameterDescriptor[] fromValues,
            final Map<GeneralParameterDescriptor,GeneralParameterDescriptor> replacements)
    {
        if (descriptors.isEmpty()) {
            return fromValues;
        }
        final Map<String,GeneralParameterDescriptor> union =
                new LinkedHashMap<String,GeneralParameterDescriptor>(Containers.hashMapCapacity(descriptors.size()));
        /*
         * Collect the descriptors declared explicitely in the ParameterDescriptorGroup. We should never have
         * two descriptors of the same name since the DefaultParameterDescriptorGroup constructor checked for
         * name ambiguity. If a name collision is nevertheless detected, this would mean that a descriptor's
         * name mutated.
         */
        for (final GeneralParameterDescriptor p : descriptors) {
            final String name = p.getName().getCode();
            if (union.put(name, p) != null) {
                throw new CorruptedObjectException(name);
            }
        }
        /*
         * Verify if any descriptors found in the ParameterValue instances could replace the descriptors in the group.
         * We give precedence to the descriptors having a non-null 'valueClass' property, which normally appear in the
         * 'fromValues' array.
         */
        for (final GeneralParameterDescriptor valueDescriptor : fromValues) {
            final String name = valueDescriptor.getName().getCode();
            GeneralParameterDescriptor complete = valueDescriptor;
            GeneralParameterDescriptor previous = union.put(name, complete);
            if (previous != null) {
                if (previous instanceof ParameterDescriptor<?>) {
                    verifyEquivalence(name, complete instanceof ParameterDescriptor<?>);
                    final Class<?> valueClass = ((ParameterDescriptor<?>) previous).getValueClass();
                    if (valueClass != null) {
                        /*
                         * This may happen if the 'descriptors' argument contain the parameters of a pre-defined
                         * method from the 'org.apache.sis.internal.referencing.provider' package instead than a
                         * descriptor from the GML file.  In such case, presume that 'previous' is actually more
                         * complete than 'complete'.
                         *
                         * Note that 'r' should never be null unless JAXB unmarshalled the elements in reverse
                         * order (e.g. <gml:ParameterValue> before <gml:OperationMethod>). Since this behavior
                         * may depend on JAXB implementation, we are better to check for such case.
                         */
                        final Class<?> r = ((ParameterDescriptor<?>) complete).getValueClass();
                        if (r != null) {
                            verifyEquivalence(name, valueClass == r);
                        }
                        // Restore the previous value in the map and swap 'previous' with 'replacement'.
                        previous = union.put(name, complete = previous);
                    }
                } else if (previous instanceof ParameterDescriptorGroup) {
                    verifyEquivalence(name, complete instanceof ParameterDescriptorGroup);
                }
                /*
                 * Verify that the replacement contains at least all the information provided by the previous
                 * descriptor. The replacement is allowed to contain more information however.
                 */
                final GeneralParameterDescriptor replacement = CC_GeneralOperationParameter.merge(previous, complete);
                if (replacement != valueDescriptor) {
                    union.put(name, replacement);
                    if (replacements.put(valueDescriptor, replacement) != null) {
                        // Should never happen, unless the parameter name changed during execution of this loop.
                        throw new CorruptedObjectException(name);
                    }
                }
            }
        }
        return union.values().toArray(new GeneralParameterDescriptor[union.size()]);
    }

    /**
     * Throws an exception for mismatched descriptor if a condition is false.
     * This is used for verifying that a descriptors has the expected properties.
     */
    private static void verifyEquivalence(final String name, final boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedParameterDescriptor_1, name));
        }
    }
}
