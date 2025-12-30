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
package org.apache.sis.xml.bind.referencing;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.lang.reflect.Array;
import jakarta.xml.bind.annotation.XmlElementRef;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
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
import org.apache.sis.referencing.GeodeticException;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.gco.PropertyType;
import org.apache.sis.pending.jdk.JDK19;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.ReferenceIdentifier;


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
 */
public final class CC_GeneralOperationParameter extends PropertyType<CC_GeneralOperationParameter, GeneralParameterDescriptor> {
    /**
     * The default value of {@code minimumOccurs} and {@code maximumOccurs} if the XML element is not provided.
     */
    public static final short DEFAULT_OCCURRENCE = 1;

    /**
     * The properties to ignore in the descriptor parsed from GML when this descriptor is merged with a
     * predefined descriptor. Remarks:
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
     * Constructor for the {@link #wrap(GeneralParameterDescriptor)} method only.
     */
    private CC_GeneralOperationParameter(final GeneralParameterDescriptor parameter) {
        super(parameter);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value in a
     * {@code <gml:OperationParameter>} or {@code <gml:OperationParameterGroup>} XML element.
     *
     * @param  parameter  the element to marshal.
     * @return a {@code PropertyType} wrapping the given the element.
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
     * @return the element to be marshalled.
     *
     * @see CC_GeneralParameterValue#getElement()
     */
    @XmlElementRef
    public AbstractParameterDescriptor getElement() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
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
     * @param  parameter  the unmarshalled element.
     */
    public void setElement(final AbstractParameterDescriptor parameter) {
        metadata = parameter;
    }

    /**
     * Verifies that the given descriptor is non-null and contains at least a name.
     * This method is used after unmarshalling.
     * We do this validation because parameter descriptors are mandatory and SIS classes need them.
     * So we provide an error message here instead of waiting for a {@link NullPointerException}
     * to occur in some arbitrary place.
     *
     * @param  descriptor  the descriptor to validate.
     * @param  parent      the name of the element to report as the parent of {@code property}.
     * @param  property    the name of the property to report as missing if an exception is thrown.
     * @throws GeodeticException if the parameters are missing or invalid.
     */
    static void validate(final GeneralParameterDescriptor descriptor, final String parent, final String property) {
        if (descriptor == null || descriptor.getName() == null) {
            short key = Errors.Keys.MissingComponentInElement_2;
            String[] args = {parent, property};
            /*
             * The exception thrown by this method must be unchecked,
             * otherwise JAXB just reports is without propagating it.
             */
            if (descriptor instanceof IdentifiedObject) {
                final String link = ((IdentifiedObject) descriptor).getIdentifierMap().get(IdentifierSpace.XLINK);
                if (link != null) {
                    key = Errors.Keys.NotABackwardReference_1;
                    args = new String[] {link};
                }
            }
            throw new GeodeticException(Errors.format(key, args));
        }
    }

    /**
     * Returns {@code true} if the given descriptor is restricted to a constant value.
     * This constraint exists in some predefined map projections.
     *
     * <h4>Example</h4>
     * The <q>Latitude of natural origin</q> parameter of <q>Mercator (1SP)</q> projection
     * is provided for completeness, but should never be different than zero in this particular projection
     * (otherwise it would be a <q>Mercator (variant C)</q> projection).  But if this parameter is
     * nevertheless provided, the SIS implementation will use it. From this point of view, SIS is tolerant
     * to non-zero value.
     *
     * <p>If the GML document declares explicitly a restricted parameter, maybe it intends to use it with
     * a non-zero value. Consequently, the {@code merge(…)} method will not propagate this restriction.</p>
     */
    private static boolean isRestricted(final ParameterDescriptor<?> descriptor) {
        final Comparable<?> min = descriptor.getMinimumValue();
        if (min instanceof Number) {
            final Comparable<?> max = descriptor.getMaximumValue();
            if (max instanceof Number) {
                // Compare as `double` because we want (-0 == +0) to be true.
                return ((Number) min).doubleValue() == ((Number) max).doubleValue();
            }
        }
        return false;
    }

    /**
     * Returns a descriptor with the same properties as the {@code provided} one, but completed with information
     * not found in GML. Those missing information are given by the {@code complete} descriptor, which may come from
     * two sources:
     *
     * <ul>
     *   <li>The descriptor for a {@code <gml:ParameterValue>} element. Those descriptors are more complete than the
     *       ones provided by {@code <gml:OperationParameter>} elements alone because the parameter value allows SIS
     *       to infer the {@code valueClass}.</li>
     *   <li>A predefined parameter descriptor from the {@code org.apache.sis.referencing.operation.provider} package.</li>
     * </ul>
     *
     * @param  provided  the descriptor unmarshalled from the GML document.
     * @param  complete  the descriptor to use for completing missing information.
     * @return the descriptor to use. May be one of the arguments given to this method, or a new instance.
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
             * predefined descriptors provided by SIS. But it could happen with a predefined descriptor
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
             * The predefined or ParameterValue descriptor contains at least all the information found
             * in the descriptor parsed from the GML document. We can use the existing instance directly,
             * assuming that the additional properties are acceptable.
             *
             * We make an exception to the above rule if the existing instance put a possibly too strong
             * restriction on the parameter values. See `isRestricted(…)` for more information.
             */
            if (!isRestricted((ParameterDescriptor<?>) complete)) {
                return complete;
            }
        }
        /*
         * Collect the properties specified in the GML document and complete with the properties provided
         * by the `complete` descriptor. If the descriptor is a group, then this `replacement` method will
         * be invoked recursively for each parameter in the group.
         */
        final Map<String,Object> merged = new HashMap<>(expected);
        merged.putAll(actual);                                      // May overwrite predefined properties.
        mergeArrays(GeneralParameterDescriptor.ALIAS_KEY,       GenericName.class, provided.getAlias(), merged, complete.getName());
        mergeArrays(GeneralParameterDescriptor.IDENTIFIERS_KEY, ReferenceIdentifier.class, provided.getIdentifiers(), merged, null);
        if (isGroup) {
            final List<GeneralParameterDescriptor> descriptors = ((ParameterDescriptorGroup) provided).descriptors();
            return merge(DefaultParameterValueGroup.class, merged, merged, minimumOccurs, maximumOccurs,
                    descriptors.toArray(GeneralParameterDescriptor[]::new),
                    (ParameterDescriptorGroup) complete, canSubstitute);
        } else {
            return create(merged, (ParameterDescriptor<?>) provided, (ParameterDescriptor<?>) complete);
        }
    }

    /**
     * Returns a descriptor with the given properties, completed with information not found in GML.
     * Those extra information are given by the {@code complete} descriptor.
     *
     * @param  caller         the public source class to report if a log message need to be emitted.
     * @param  properties     properties as declared in the GML document, to be used if {@code complete} is incompatible.
     * @param  merged         more complete properties, to be used if {@code complete} is compatible.
     * @param  minimumOccurs  value to assign to {@link DefaultParameterDescriptorGroup#getMinimumOccurs()}.
     * @param  maximumOccurs  value to assign to {@link DefaultParameterDescriptorGroup#getMaximumOccurs()}.
     * @param  provided       parameter descriptors declared in the GML document. This array will be overwritten.
     * @param  complete       more complete parameter descriptors.
     * @param  canSubstitute  {@code true} if this method is allowed to return {@code complete}.
     * @return the parameter descriptor group to use (may be the {@code complete} instance).
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
        final Set<GeneralParameterDescriptor> included = JDK19.newHashSet(provided.length);
        for (int i=0; i<provided.length; i++) {
            final GeneralParameterDescriptor p = provided[i];
            try {
                /*
                 * Replace the descriptors provided in the GML document by descriptors from the `complete` instance,
                 * if possible. Keep trace of the complete descriptors that we found in this process.
                 */
                GeneralParameterDescriptor predefined = complete.descriptor(p.getName().getCode());
                if (predefined != null) {   // Safety in case `complete` is a user's implementation.
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
             * If a parameter was not found in the `complete` descriptor, we will not be able to use that descriptor.
             * But we may still be able to use its properties (name, alias, identifier) provided that the parameter
             * not found was optional.
             */
            isCompatible &= p.getMinimumOccurs() == 0;
            canSubstitute = false;
        }
        if (isCompatible) {
            /*
             * At this point, we determined that all mandatory parameters in the GML document exist in the `complete`
             * descriptor. However, the converse is not necessarily true. Verify that all parameters missing in the GML
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
     * Creates a new descriptor with the same properties as the {@code provided} one, but completed with
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
        return new DefaultParameterDescriptor<>(merged,
                provided.getMinimumOccurs(),
                provided.getMaximumOccurs(),
                // Values below this point are not provided in GML documents,
                // so they must be inferred from the predefined descriptor.
                valueClass,
                Parameters.getValueDomain(complete),
                toArray(complete.getValidValues(), valueClass),
                complete.getDefaultValue());
    }

    /**
     * Returns {@code true} if the {@code complete} collection contains all elements in the {@code provided}
     * collection, where each element have been converted to the canonical {@link NamedIdentifier} implementation
     * for comparison purpose.
     *
     * @param  <T>       the type of elements in the collection.
     * @param  complete  the collection which is expected to contains all elements.
     * @param  provided  the collection which may be a subset of {@code complete}.
     * @return {@code true} if {@code complete} contains all {@code provided} elements.
     */
    private static <T> boolean containsAll(final Collection<T> complete, final Collection<T> provided) {
        if (!provided.isEmpty()) {
            final int size = complete.size();
            if (size == 0) {
                return false;
            }
            final Set<NamedIdentifier> c = JDK19.newHashSet(size);
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
     * This is used when we cannot just substitute one collection by the other.
     *
     * @param <T>            the type of elements in the array or collection.
     * @param key            the key where to fetch or store the array in the {@code merged} map.
     * @param componentType  the type of elements in the array or collection.
     * @param provided       the elements unmarshalled from the XML document.
     * @param merged         the map used for completing missing information.
     */
    @SuppressWarnings("unchecked")
    private static <T> void mergeArrays(final String key, final Class<T> componentType,
            Collection<T> provided, final Map<String,Object> merged, final Identifier remove)
    {
        if (!provided.isEmpty()) {
            T[] complete = (T[]) merged.get(key);
            if (complete != null) {
                /*
                 * Add the `provided` values before `complete` for two reasons:
                 *   1) Use the same insertion order as the declaration order in the GML file.
                 *   2) Replace `provided` instances by `complete` instances, since the latter
                 *      are sometimes predefined instances defined as static final constants.
                 */
                final Map<NamedIdentifier,T> c = new LinkedHashMap<>();
                for (final T e : provided) c.put(toNamedIdentifier(e), e);
                for (final T e : complete) c.put(toNamedIdentifier(e), e);
                c.remove(toNamedIdentifier(remove));
                provided = c.values();
            }
            complete = toArray(provided, componentType);
            merged.put(key, complete);
        }
    }

    /**
     * Returns the elements of the given collection as an array.
     * This method can be used when the {@code valueClass} argument is not known at compile-time.
     *
     * @param  <T>         the compile-time value of {@code valueClass}.
     * @param  collection  the collection from which to get the elements.
     * @param  valueClass  the runtime type of collection elements.
     * @return the collection elements as an array, or {@code null} if {@code collection} is null.
     */
    @SuppressWarnings("unchecked")
    static <T> T[] toArray(final Collection<? extends T> collection, final Class<T> valueClass) {
        if (collection != null) {
            return collection.toArray((T[]) Array.newInstance(valueClass, collection.size()));
        }
        return null;
    }

    /**
     * Given an {@link Identifier} or {@link GenericName} instance, returns that instance as a {@link NamedIdentifier}
     * implementation. The intent is to allow {@code Object.equals(Object)} and hash code to correctly recognize two
     * names or identifiers as equal even if they are of different implementations.
     *
     * <p>Note that {@link NamedIdentifier} is the type of unmarshalled names, aliases and identifiers.
     * So this method should not create any new object in a majority of cases.</p>
     */
    private static NamedIdentifier toNamedIdentifier(final Object name) {
        if (name == null || name.getClass() == NamedIdentifier.class) {
            return (NamedIdentifier) name;
        } else if (name instanceof Identifier) {
            return new NamedIdentifier((Identifier) name);
        } else {
            return new NamedIdentifier((GenericName) name);
        }
    }
}
