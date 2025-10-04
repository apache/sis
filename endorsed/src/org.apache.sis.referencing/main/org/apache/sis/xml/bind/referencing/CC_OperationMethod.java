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

import java.util.Map;
import java.util.Collection;
import jakarta.xml.bind.annotation.XmlElement;
import javax.measure.Unit;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.gco.PropertyType;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.provider.MapProjection;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.parameter.DefaultParameterValue;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.util.ArraysExt;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CC_OperationMethod extends PropertyType<CC_OperationMethod, OperationMethod> {
    /**
     * Empty constructor for JAXB only.
     */
    public CC_OperationMethod() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code OperationMethod.class}
     */
    @Override
    protected Class<OperationMethod> getBoundType() {
        return OperationMethod.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CC_OperationMethod(final OperationMethod method) {
        super(method);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:OperationMethod>} XML element.
     *
     * @param  method  the element to marshal.
     * @return a {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected CC_OperationMethod wrap(final OperationMethod method) {
        return new CC_OperationMethod(method);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:OperationMethod>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the element to be marshalled.
     */
    @XmlElement(name = "OperationMethod")
    public DefaultOperationMethod getElement() {
        return DefaultOperationMethod.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  method  the unmarshalled element.
     */
    public void setElement(final DefaultOperationMethod method) {
        metadata = method;
        CC_GeneralOperationParameter.validate(method.getParameters(), "OperationMethod", "parameter");
    }

    /**
     * Returns the given descriptors, excluding the implicit {@link MapProjection} parameters.
     *
     * @param  array  the parameters to filter.
     * @return the filtered parameters.
     */
    public static GeneralParameterValue[] filterImplicit(final GeneralParameterValue[] array) {
        int n = 0;
        for (final GeneralParameterValue value : array) {
            if (!CC_OperationMethod.isImplicitParameter(value.getDescriptor())) {
                array[n++] = value;
            }
        }
        return ArraysExt.resize(array, n);
    }

    /**
     * Returns the given descriptors, excluding the implicit {@link MapProjection} parameters.
     *
     * @param  array  the parameters to filter.
     * @return the filtered parameters.
     */
    public static GeneralParameterDescriptor[] filterImplicit(final GeneralParameterDescriptor[] array) {
        int n = 0;
        for (final GeneralParameterDescriptor descriptor : array) {
            if (!CC_OperationMethod.isImplicitParameter(descriptor)) {
                array[n++] = descriptor;
            }
        }
        return ArraysExt.resize(array, n);
    }

    /**
     * Returns {@code true} if the given descriptor is for an implicit parameter which should be excluded from GML.
     *
     * @param  descriptor  the parameter descriptor to test.
     * @return {@code true} if the given parameter should be omitted in the GML document.
     */
    static boolean isImplicitParameter(final GeneralParameterDescriptor descriptor) {
        return descriptor == MapProjection.SEMI_MAJOR
            || descriptor == MapProjection.SEMI_MINOR;
    }

    /**
     * Wraps the given descriptors in a descriptor group of the given name. If the given name can be matched
     * to the name of one of the predefined operation method, then the predefined parameters will be used.
     *
     * <p>We try to use predefined parameters if possible because they contain information, especially the
     * {@link org.opengis.parameter.ParameterDescriptor#getValueClass()} property, which are not available
     * in the GML document.</p>
     *
     * <h4>Implementation note</h4>
     * This code is defined in this {@code CC_OperationMethod} class instead of in the
     * {@link DefaultOperationMethod} class in the hope to reduce the amount of code processed
     * by the JVM in the common case where JAXB (un)marshalling is not needed.
     *
     * @param  name         the operation method name, to be also given to the descriptor group.
     * @param  descriptors  the parameter descriptors to wrap in a group. This array will be modified in-place.
     * @return a parameter group containing at least the given descriptors, or equivalent descriptors.
     */
    public static ParameterDescriptorGroup group(final Identifier name, final GeneralParameterDescriptor[] descriptors) {
        OperationMethod method;
        try {
            method = DefaultMathTransformFactory.provider().getOperationMethod(name.getCode());
        } catch (FactoryException e) {
            // Use DefaultOperationMethod as the source class because it is the first public class in callers.
            Context.warningOccured(Context.current(), DefaultOperationMethod.class, "setDescriptors", e, true);
            method = null;
        }
        final Map<String,?> properties = Map.of(ParameterDescriptorGroup.NAME_KEY, name);
        if (method != null) {
            /*
             * Verify that the predefined operation method contains at least all the parameters specified by
             * the `descriptors` array. If this is the case, then the predefined parameters will be used in
             * replacement of the given ones.
             */
            final ParameterDescriptorGroup parameters = method.getParameters();
            return CC_GeneralOperationParameter.merge(DefaultOperationMethod.class,
                    properties, IdentifiedObjects.getProperties(parameters),
                    1, 1, descriptors, parameters, true);
        }
        return new DefaultParameterDescriptorGroup(properties, 1, 1, descriptors);
    }

    /**
     * Stores the given {@code parameters} into the given {@code addTo} collection.
     * This method copies only the <em>references</em> if possible. However, in some
     * cases the values may need to be copied in new parameter instances.
     *
     * <h4>Implementation note</h4>
     * This code is defined in this {@code CC_OperationMethod} class instead of in the
     * {@link DefaultOperationMethod} class in the hope to reduce the amount of code processed
     * by the JVM in the common case where JAXB (un)marshalling is not needed.
     *
     * @param  parameters    the parameters to add to the {@code addTo} collection.
     * @param  addTo         where to store the {@code parameters}.
     * @param  replacements  the replacements to apply in the {@code GeneralParameterValue} instances.
     */
    public static void store(final GeneralParameterValue[] parameters,
                             final Collection<GeneralParameterValue> addTo,
                             final Map<GeneralParameterDescriptor,GeneralParameterDescriptor> replacements)
    {
        for (GeneralParameterValue p : parameters) {
            final GeneralParameterDescriptor replacement = replacements.get(p.getDescriptor());
            if (replacement != null) {
                if (p instanceof ParameterValue<?>) {
                    final var source = (ParameterValue<?>) p;
                    final var target = new DefaultParameterValue<>((ParameterDescriptor<?>) replacement);
                    final Object value = source.getValue();
                    final Unit<?> unit = source.getUnit();
                    if (unit == null) {
                        target.setValue(value);
                    } else if (value instanceof double[]) {
                        target.setValue((double[]) value, unit);
                    } else {
                        target.setValue(((Number) value).doubleValue(), unit);
                    }
                    if (source instanceof DefaultParameterValue<?>) {
                        ((DefaultParameterValue<?>) source).getSourceFile().ifPresent(target::setSourceFile);
                    }
                    p = target;
                } else if (p instanceof ParameterValueGroup) {
                    final ParameterValueGroup source = (ParameterValueGroup) p;
                    final ParameterValueGroup target = new DefaultParameterValueGroup((ParameterDescriptorGroup) replacement);
                    final Collection<GeneralParameterValue> values = source.values();
                    store(values.toArray(GeneralParameterValue[]::new), target.values(), replacements);
                    p = target;
                }
            }
            addTo.add(p);
        }
    }
}
