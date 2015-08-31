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
import java.util.Collections;
import javax.xml.bind.annotation.XmlElement;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.provider.MapProjection;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.ArraysExt;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
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
     * @param  method The element to marshall.
     * @return A {@code PropertyType} wrapping the given the element.
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
     * @return The element to be marshalled.
     */
    @XmlElement(name = "OperationMethod")
    public DefaultOperationMethod getElement() {
        return DefaultOperationMethod.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param method The unmarshalled element.
     */
    public void setElement(final DefaultOperationMethod method) {
        metadata = method;
    }

    /**
     * Returns the given descriptors, excluding the implicit {@link MapProjection} parameters.
     *
     * @param  array The parameters to filter.
     * @return The filtered parameters.
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
     * @param  array The parameters to filter.
     * @return The filtered parameters.
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
     * @param  descriptor The parameter descriptor to test.
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
     * <div class="note"><b>Note:</b>
     * this code is defined in this {@code CC_OperationMethod} class instead than in the
     * {@link DefaultOperationMethod} class in the hope to reduce the amount of code processed
     * by the JVM in the common case where JAXB (un)marshalling is not needed.</div>
     *
     * @param  name        The operation method name, to be also given to the descriptor group.
     * @param  descriptors The parameter descriptors to wrap in a group. This array will be modified in-place.
     * @return A parameter group containing at least the given descriptors, or equivalent descriptors.
     */
    public static ParameterDescriptorGroup group(final Identifier name, final GeneralParameterDescriptor[] descriptors) {
        final CoordinateOperationFactory factory = DefaultFactories.forClass(CoordinateOperationFactory.class);
        OperationMethod method = null;
        if (factory != null) try {
            method = factory.getOperationMethod(name.getCode());
        } catch (FactoryException e) {
            Context.warningOccured(Context.current(), DefaultOperationMethod.class, "setDescriptors", e, true);
        }
        final Map<String,?> properties = Collections.singletonMap(ParameterDescriptorGroup.NAME_KEY, name);
        if (method != null) {
            /*
             * Verify that the pre-defined operation method contains at least all the parameters specified by
             * the 'descriptors' array. If this is the case, then the pre-defined parameters will be used in
             * replacement of the given ones.
             */
            final ParameterDescriptorGroup parameters = method.getParameters();
            return CC_GeneralOperationParameter.merge(properties,
                    IdentifiedObjects.getProperties(parameters),
                    1, 1, descriptors, parameters, true);
        }
        return new DefaultParameterDescriptorGroup(properties, 1, 1, descriptors);
    }
}
