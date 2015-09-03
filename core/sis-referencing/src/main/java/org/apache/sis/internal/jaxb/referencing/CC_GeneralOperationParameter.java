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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import javax.xml.bind.annotation.XmlElementRef;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.parameter.AbstractParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.internal.jaxb.Context;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * <p>This class provides additional {@code merge(…)} methods for building a unique descriptor
 * instance when the same descriptor is declared in more than one place in the GML document.
 * Some examples of duplications are:</p>
 *
 * <ul>
 *   <li>The descriptors listed under the {@code <gml:group>} element, which duplicate the descriptors listed
 *       under each {@code <gml:parameterValue>} element.</li>
 *   <li>The descriptors declared in each parameter value of a {@code SingleOperation}, which duplicate the
 *       descriptors declared in the associated {@code OperationMethod}.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class CC_GeneralOperationParameter extends PropertyType<CC_GeneralOperationParameter, GeneralParameterDescriptor> {
    /**
     * The default value of {@code minimumOccurs} and {@code maximumOccurs} if the XML element is not provided.
     */
    public static final short DEFAULT_OCCURRENCE = 1;

    /**
     * The properties to ignore in the descriptor parsed from GML when this descriptor is merged with a
     * pre-defined descriptor. Remarks:
     *
     * <ul>
     *   <li>We ignore the name because the comparisons shall be performed by the caller with
     *       {@link IdentifiedObjects#isHeuristicMatchForName} or something equivalent.</li>
     *   <li>We ignore aliases and identifiers for now for avoiding the additional complexity
     *       of dealing with collections, and because the Apache SIS pre-defined descriptors
     *       will be more complete in a majority of case.
     *
     *       <b>TODO - </b> this may be revised in any future SIS version.</li>
     * </ul>
     */
    private static final String[] IGNORE_DURING_MERGE = {
        GeneralParameterDescriptor.NAME_KEY,
        GeneralParameterDescriptor.ALIAS_KEY,
        GeneralParameterDescriptor.IDENTIFIERS_KEY
    };

    /**
     * Empty constructor for JAXB only.
     */
    public CC_GeneralOperationParameter() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code GeneralParameterDescriptor.class}
     */
    @Override
    protected Class<GeneralParameterDescriptor> getBoundType() {
        return GeneralParameterDescriptor.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CC_GeneralOperationParameter(final GeneralParameterDescriptor parameter) {
        super(parameter);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value in a
     * {@code <gml:OperationParameter>} or {@code <gml:OperationParameterGroup>} XML element.
     *
     * @param  parameter The element to marshall.
     * @return A {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected CC_GeneralOperationParameter wrap(final GeneralParameterDescriptor parameter) {
        return new CC_GeneralOperationParameter(parameter);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:parameter>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The element to be marshalled.
     *
     * @see CC_GeneralParameterValue#getElement()
     */
    @XmlElementRef
    public AbstractParameterDescriptor getElement() {
        final GeneralParameterDescriptor metadata = this.metadata;
        if (metadata instanceof AbstractParameterDescriptor) {
            return (AbstractParameterDescriptor) metadata;
        }
        if (metadata instanceof ParameterDescriptor) {
            return DefaultParameterDescriptor.castOrCopy((ParameterDescriptor<?>) metadata);
        }
        if (metadata instanceof ParameterDescriptorGroup) {
            return DefaultParameterDescriptorGroup.castOrCopy((ParameterDescriptorGroup) metadata);
        }
        return null;    // Unknown types are currently not marshalled (we may revisit that in a future SIS version).
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param parameter The unmarshalled element.
     */
    public void setElement(final AbstractParameterDescriptor parameter) {
        metadata = parameter;
    }

    /**
     * Returns a descriptor with the same properties than the {@code provided} one, but completed with information
     * not found in GML. Those missing information are given by the {@code complete} descriptor, which may come from
     * two sources:
     *
     * <ul>
     *   <li>The descriptor for a {@code <gml:ParameterValue>} element. Those descriptors are more complete than the
     *       ones provided by {@code <gml:OperationParameter>} elements alone because the parameter value allows SIS
     *       to infer the {@code valueClass}.</li>
     *   <li>A pre-defined parameter descriptor from the {@link org.apache.sis.internal.referencing.provider} package.</li>
     * </ul>
     *
     * @param  provided The descriptor unmarshalled from the GML document.
     * @param  complete The descriptor to use for completing missing information.
     * @return The descriptor to use. May be one of the arguments given to this method, or a new instance.
     *
     * @see <a href="http://issues.apache.org/jira/browse/SIS-290">SIS-290</a>
     */
    static GeneralParameterDescriptor merge(final GeneralParameterDescriptor provided,
                                            final GeneralParameterDescriptor complete)
    {
        if (provided == complete) {
            return complete;
        }
        final boolean isGroup;
        if (provided instanceof ParameterDescriptor<?> && complete instanceof ParameterDescriptor<?>) {
            isGroup = false;    // This is by far the most usual case.
        } else if (provided instanceof ParameterDescriptorGroup && complete instanceof ParameterDescriptorGroup) {
            isGroup = true;
        } else {
            /*
             * Mismatched or unknown type. It should not happen with descriptors parsed by JAXB and with
             * pre-defined descriptors provided by SIS. But it could happen with a pre-defined descriptor
             * found in a user-provided OperationMethod with malformed parameters.
             * Return the descriptor found in the GML document as-is.
             */
            return provided;
        }
        final int minimumOccurs = provided.getMinimumOccurs();
        final int maximumOccurs = provided.getMaximumOccurs();
        final Map<String,?> expected = IdentifiedObjects.getProperties(complete);
        final Map<String,?> actual   = IdentifiedObjects.getProperties(provided, IGNORE_DURING_MERGE);
        final boolean canSubstitute  = complete.getMinimumOccurs() == minimumOccurs
                                    && complete.getMaximumOccurs() == maximumOccurs
                                    && expected.entrySet().containsAll(actual.entrySet());
        if (canSubstitute && !isGroup) {
            /*
             * The pre-defined or ParameterValue descriptor contains at least all the information found
             * in the descriptor parsed from the GML document, ignoring IGNORE_DURING_MERGE properties.
             * So we can use the existing instance directly, assuming that the additional properties and
             * the difference in ignored properties are acceptable.
             */
            return complete;
        }
        /*
         * Collect the properties specified in the GML document and complete with the properties provided
         * by the 'complete' descriptor. If the descriptor is a group, then this 'replacement' method will
         * be invoked recursively for each parameter in the group.
         */
        final Map<String,Object> merged = new HashMap<>(expected);
        merged.putAll(actual);  // May overwrite pre-defined properties.
        if (isGroup) {
            final List<GeneralParameterDescriptor> descriptors = ((ParameterDescriptorGroup) provided).descriptors();
            return merge(DefaultParameterValueGroup.class, merged, merged, minimumOccurs, maximumOccurs,
                    descriptors.toArray(new GeneralParameterDescriptor[descriptors.size()]),
                    (ParameterDescriptorGroup) complete, canSubstitute);
        } else {
            return merge(merged, (ParameterDescriptor<?>) provided, (ParameterDescriptor<?>) complete);
        }
    }

    /**
     * Returns a descriptor with the given properties, completed with information not found in GML.
     * Those extra information are given by the {@code complete} descriptor.
     *
     * @param  caller        The public source class to report if a log message need to be emitted.
     * @param  properties    Properties as declared in the GML document, to be used if {@code complete} is incompatible.
     * @param  merged        More complete properties, to be used if {@code complete} is compatible.
     * @param  minimumOccurs Value to assign to {@link DefaultParameterDescriptorGroup#getMinimumOccurs()}.
     * @param  maximumOccurs Value to assign to {@link DefaultParameterDescriptorGroup#getMaximumOccurs()}.
     * @param  provided      Parameter descriptors declared in the GML document. This array will be overwritten.
     * @param  complete      More complete parameter descriptors.
     * @param  canSubstitute {@code true} if this method is allowed to return {@code complete}.
     * @return The parameter descriptor group to use (may be the {@code complete} instance).
     *
     * @see <a href="http://issues.apache.org/jira/browse/SIS-290">SIS-290</a>
     */
    static ParameterDescriptorGroup merge(final Class<?>                     caller,
                                          final Map<String,?>                properties,
                                          final Map<String,?>                merged,
                                          final int                          minimumOccurs,
                                          final int                          maximumOccurs,
                                          final GeneralParameterDescriptor[] provided,
                                          final ParameterDescriptorGroup     complete,
                                          boolean                            canSubstitute)
    {
        boolean isCompatible = true;
        final Set<GeneralParameterDescriptor> included = new HashSet<>(Containers.hashMapCapacity(provided.length));
        for (int i=0; i<provided.length; i++) {
            final GeneralParameterDescriptor p = provided[i];
            try {
                /*
                 * Replace the descriptors provided in the GML document by descriptors from the 'complete' instance,
                 * if possible. Keep trace of the complete descriptors that we found in this process.
                 */
                GeneralParameterDescriptor predefined = complete.descriptor(p.getName().getCode());
                if (predefined != null) {   // Safety in case 'complete' is a user's implementation.
                    canSubstitute &= (provided[i] = merge(p, predefined)) == predefined;
                    if (!included.add(predefined)) {
                        throw new CorruptedObjectException(predefined);  // Broken hashCode/equals, or object mutated.
                    }
                    continue;
                }
            } catch (ParameterNotFoundException e) {
                /*
                 * Log at Level.WARNING for the first parameter (canSubstitute == true) and at Level.FINE
                 * for all other (canSubstitute == false).  We do not use CC_GeneralOperationParameter as
                 * the source class because this is an internal class. We rather use the first public class
                 * in the caller hierarchy, which is either DefaultParameterValueGroup or DefaultOperationMethod.
                 */
                Context.warningOccured(Context.current(), caller,
                        (caller == DefaultParameterValueGroup.class) ? "setValues" : "setDescriptors", e, canSubstitute);
            }
            /*
             * If a parameter was not found in the 'complete' descriptor, we will not be able to use that descriptor.
             * But we may still be able to use its properties (name, alias, identifier) provided that the parameter
             * not found was optional.
             */
            isCompatible &= p.getMinimumOccurs() == 0;
            canSubstitute = false;
        }
        if (isCompatible) {
            /*
             * At this point, we determined that all mandatory parameters in the GML document exist in the 'complete'
             * descriptor. However the converse is not necessarily true. Verify that all parameters missing in the GML
             * document were optional.
             */
            for (final GeneralParameterDescriptor descriptor : complete.descriptors()) {
                if (!included.contains(descriptor) && descriptor.getMinimumOccurs() != 0
                        && !CC_OperationMethod.isImplicitParameter(descriptor))
                {
                    canSubstitute = false;
                    isCompatible  = false;
                    break;
                }
            }
        }
        if (canSubstitute) {
            return complete;
        } else {
            return new DefaultParameterDescriptorGroup(isCompatible ? merged : properties,
                    minimumOccurs, maximumOccurs, provided);
        }
    }

    /**
     * Creates a new descriptor with the same properties than the {@code provided} one, but completed with
     * information not found in GML. Those extra information are given by the {@code complete} descriptor.
     *
     * <p>It is the caller's responsibility to construct the {@code properties} map as a merge
     * of the properties of the two given descriptors.</p>
     */
    private static <T> ParameterDescriptor<T> merge(final Map<String,?>          merged,
                                                    final ParameterDescriptor<?> provided,
                                                    final ParameterDescriptor<T> complete)
    {
        final Class<T> valueClass = complete.getValueClass();
        return new DefaultParameterDescriptor<>(merged,
                provided.getMinimumOccurs(),
                provided.getMaximumOccurs(),
                // Values below this point are not provided in GML documents,
                // so they must be inferred from the pre-defined descriptor.
                valueClass,
                Parameters.getValueDomain(complete),
                CollectionsExt.toArray(complete.getValidValues(), valueClass),
                complete.getDefaultValue());
    }
}
