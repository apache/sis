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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.lang.reflect.Array;
import javax.xml.bind.annotation.XmlElementRef;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.parameter.AbstractParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.NamedIdentifier;
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
     *   <li>We ignore aliases and identifiers because they are collections, which require
     *       handling in a special way.</li>
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
     * Verifies that the given descriptor is non-null and contains at least a name.
     * This method is used after unmarshalling.
     */
    static boolean isValid(final GeneralParameterDescriptor descriptor) {
        return descriptor != null && descriptor.getName() != null;
    }

    /**
     * Returns {@code true} if the given descriptor is restricted to a constant value.
     * This constraint exists in some pre-defined map projections.
     *
     * <div class="note"><b>Example:</b>
     * the <cite>"Latitude of natural origin"</cite> parameter of <cite>"Mercator (1SP)"</cite> projection
     * is provided for completeness, but should never be different than zero in this particular projection
     * (otherwise it would be a <cite>"Mercator (variant C)"</cite> projection).  But if this parameter is
     * nevertheless provided, the SIS implementation will use it. From this point of view, SIS is tolerant
     * to non-zero value.
     *
     * <p>If the GML document declares explicitely a restricted parameter, maybe it intends to use it with
     * a non-zero value. Consequently the {@code merge(…)} method will not propagate this restriction.</p>
     * </div>
     */
    private static boolean isRestricted(final ParameterDescriptor<?> descriptor) {
        final Comparable<?> min = descriptor.getMinimumValue();
        if (min instanceof Number) {
            final Comparable<?> max = descriptor.getMaximumValue();
            if (max instanceof Number) {
                // Compare as 'double' because we want (-0 == +0) to be true.
                return ((Number) min).doubleValue() == ((Number) max).doubleValue();
            }
        }
        return false;
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
                                    && expected.entrySet().containsAll(actual.entrySet())
                                    && containsAll(complete.getAlias(), provided.getAlias())
                                    && containsAll(complete.getIdentifiers(), provided.getIdentifiers());
        if (canSubstitute && !isGroup) {
            /*
             * The pre-defined or ParameterValue descriptor contains at least all the information found
             * in the descriptor parsed from the GML document. We can use the existing instance directly,
             * assuming that the additional properties are acceptable.
             *
             * We make an exception to the above rule if the existing instance put a possibly too strong
             * restriction on the parameter values. See 'isRestricted(…)' for more information.
             */
            if (!isRestricted((ParameterDescriptor<?>) complete)) {
                return complete;
            }
        }
        /*
         * Collect the properties specified in the GML document and complete with the properties provided
         * by the 'complete' descriptor. If the descriptor is a group, then this 'replacement' method will
         * be invoked recursively for each parameter in the group.
         */
        final Map<String,Object> merged = new HashMap<String,Object>(expected);
        merged.putAll(actual);  // May overwrite pre-defined properties.
        mergeArrays(GeneralParameterDescriptor.ALIAS_KEY,       GenericName.class, provided.getAlias(), merged, complete.getName());
        mergeArrays(GeneralParameterDescriptor.IDENTIFIERS_KEY, ReferenceIdentifier.class, provided.getIdentifiers(), merged, null);
        if (isGroup) {
            final List<GeneralParameterDescriptor> descriptors = ((ParameterDescriptorGroup) provided).descriptors();
            return merge(DefaultParameterValueGroup.class, merged, merged, minimumOccurs, maximumOccurs,
                    descriptors.toArray(new GeneralParameterDescriptor[descriptors.size()]),
                    (ParameterDescriptorGroup) complete, canSubstitute);
        } else {
            return create(merged, (ParameterDescriptor<?>) provided, (ParameterDescriptor<?>) complete);
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
        final Set<GeneralParameterDescriptor> included =
                new HashSet<GeneralParameterDescriptor>(Containers.hashMapCapacity(provided.length));
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
     * <p>It is the caller's responsibility to construct the {@code merged} properties as a merge of the properties
     * of the two given descriptors. This can be done with the help of {@link #mergeArrays(String, Class, Collection,
     * Map, Identifier)} among others.</p>
     */
    private static <T> ParameterDescriptor<T> create(final Map<String,?>          merged,
                                                     final ParameterDescriptor<?> provided,
                                                     final ParameterDescriptor<T> complete)
    {
        final Class<T> valueClass = complete.getValueClass();
        return new DefaultParameterDescriptor<T>(merged,
                provided.getMinimumOccurs(),
                provided.getMaximumOccurs(),
                // Values below this point are not provided in GML documents,
                // so they must be inferred from the pre-defined descriptor.
                valueClass,
                Parameters.getValueDomain(complete),
                CollectionsExt.toArray(complete.getValidValues(), valueClass),
                complete.getDefaultValue());
    }

    /**
     * Returns {@code true} if the {@code complete} collection contains all elements in the {@code provided}
     * collection, where each element have been converted to the canonical {@link NamedIdentifier} implementation
     * for comparison purpose.
     *
     * @param  <T>      The type of elements in the collection.
     * @param  complete The collection which is expected to contains all elements.
     * @param  provided The collection which may be a subset of {@code complete}.
     * @return {@code true} if {@code complete} contains all {@code provided} elements.
     */
    private static <T> boolean containsAll(final Collection<T> complete, final Collection<T> provided) {
        if (!provided.isEmpty()) {
            final int size = complete.size();
            if (size == 0) {
                return false;
            }
            final Set<NamedIdentifier> c = new HashSet<NamedIdentifier>(Containers.hashMapCapacity(size));
            for (final T e : complete) {
                c.add(toNamedIdentifier(e));
            }
            for (final T e : provided) {
                if (!c.contains(toNamedIdentifier(e))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Merges the property of type {@code Collection} identified by the given key.
     * This is used when we can not just substitute one collection by the other.
     *
     * @param <T>           The type of elements in the array or collection.
     * @param key           The key where to fetch or store the array in the {@code merged} map.
     * @param componentType The type of elements in the array or collection.
     * @param provided      The elements unmarshalled from the XML document.
     * @param merged        The map used for completing missing information.
     */
    @SuppressWarnings("unchecked")
    private static <T> void mergeArrays(final String key, final Class<T> componentType,
            Collection<T> provided, final Map<String,Object> merged, final Identifier remove)
    {
        if (!provided.isEmpty()) {
            T[] complete = (T[]) merged.get(key);
            if (complete != null) {
                /*
                 * Add the 'provided' values before 'complete' for two reasons:
                 *   1) Use the same insertion order than the declaration order in the GML file.
                 *   2) Replace 'provided' instances by 'complete' instances, since the later
                 *      are sometime pre-defined instances defined as static final constants.
                 */
                final Map<NamedIdentifier,T> c = new LinkedHashMap<NamedIdentifier,T>();
                for (final T e : provided) c.put(toNamedIdentifier(e), e);
                for (final T e : complete) c.put(toNamedIdentifier(e), e);
                c.remove(toNamedIdentifier(remove));
                provided = c.values();
            }
            complete = provided.toArray((T[]) Array.newInstance(componentType, provided.size()));
            merged.put(key, complete);
        }
    }

    /**
     * Given an {@link Identifier} or {@link GenericName} instance, returns that instance as a {@link NamedIdentifier}
     * implementation. The intend is to allow {@code Object.equals(Object)} and hash code to correctly recognize two
     * name or identifier as equal even if they are of different implementations.
     *
     * <p>Note that {@link NamedIdentifier} is the type of unmarshalled names, aliases and identifiers.
     * So this method should not create any new object in a majority of cases.</p>
     */
    private static NamedIdentifier toNamedIdentifier(final Object name) {
        if (name == null || name.getClass() == NamedIdentifier.class) {
            return (NamedIdentifier) name;
        } else if (name instanceof ReferenceIdentifier) {
            return new NamedIdentifier((ReferenceIdentifier) name);
        } else {
            return new NamedIdentifier((GenericName) name);
        }
    }
}
