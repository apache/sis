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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.apache.sis.parameter.DefaultParameterValue;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.internal.jaxb.gco.PropertyType;
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
public final class CC_GeneralParameterValue extends PropertyType<CC_GeneralParameterValue, GeneralParameterValue> {
    /**
     * Empty constructor for JAXB only.
     */
    public CC_GeneralParameterValue() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code GeneralParameterValue.class}
     */
    @Override
    protected Class<GeneralParameterValue> getBoundType() {
        return GeneralParameterValue.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CC_GeneralParameterValue(final GeneralParameterValue parameter) {
        super(parameter);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:ParameterValue>} or {@code <gml:ParameterValueGroup>} XML element.
     *
     * @param  parameter The element to marshall.
     * @return A {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected CC_GeneralParameterValue wrap(final GeneralParameterValue parameter) {
        return new CC_GeneralParameterValue(parameter);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:parameterValue>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The element to be marshalled.
     *
     * @see CC_GeneralOperationParameter#getElement()
     */
    @XmlElements({  // We can not use @XmlElementRef because we have no public AbstractParameterValue parent class.
        @XmlElement(name = "ParameterValue",      type = DefaultParameterValue.class),
        @XmlElement(name = "ParameterValueGroup", type = DefaultParameterValueGroup.class)
    })
    @SuppressWarnings({"rawtypes", "unchecked"})
    public GeneralParameterValue getElement() {
        final GeneralParameterValue metadata = this.metadata;
        if (metadata instanceof DefaultParameterValue<?>) {
            return (DefaultParameterValue<?>) metadata;
        }
        if (metadata instanceof DefaultParameterValueGroup) {
            return (DefaultParameterValueGroup) metadata;
        }
        if (metadata instanceof ParameterValue) {
            return new DefaultParameterValue((ParameterValue<?>) metadata);
        }
        if (metadata instanceof ParameterValueGroup) {
            return new DefaultParameterValueGroup((ParameterValueGroup) metadata);
        }
        return null;    // Unknown types are currently not marshalled (we may revisit that in a future SIS version).
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param parameter The unmarshalled element.
     */
    public void setElement(final GeneralParameterValue parameter) {
        if (!CC_GeneralOperationParameter.isValid(parameter.getDescriptor())) {
            /*
             * Descriptors are mandatory and SIS classes need them. Provide an error message
             * here instead than waiting for a NullPointerException in some arbitrary place.
             */
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MissingValueForProperty_1, "operationParameter"));
        }
        metadata = parameter;
    }
}
