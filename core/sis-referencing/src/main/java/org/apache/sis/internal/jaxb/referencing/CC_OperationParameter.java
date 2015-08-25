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

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.parameter.DefaultParameterDescriptor;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * <p>This class tries also to resolve another difficulty: our implementation class needs to know
 * the base class of values, but this information is not provided in the XML documents. We have to
 * infer it from the actual value.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class CC_OperationParameter extends PropertyType<CC_OperationParameter, ParameterDescriptor<?>> {
    /**
     * The class of the value of the enclosing {@code <gml:ParameterValue>} element, or {@code null} if unknown.
     *
     * <p>This is set by the private {@code beforeMarshal(…)} method at unmarshalling time
     * and read by the {@link DefaultParameterDescriptor} default constructor.</p>
     */
    public Class<?> valueClass;

    /**
     * The domain of values of the enclosing {@code <gml:ParameterValue>} element, or {@code null} if unknown.
     * If non-null, typically only the unit of measurement is relevant in this object.
     *
     * <p>This is set by the private {@code beforeUnmarshal(…)} method at unmarshalling time
     * and read by the {@link DefaultParameterDescriptor} default constructor.</p>
     */
    public MeasurementRange<?> valueDomain;

    /**
     * Empty constructor for JAXB only.
     */
    public CC_OperationParameter() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code ParameterDescriptor.class}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Class<ParameterDescriptor<?>> getBoundType() {
        return (Class) ParameterDescriptor.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CC_OperationParameter(final ParameterDescriptor<?> parameter) {
        super(parameter);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:OperationParameter>} XML element.
     *
     * @param  parameter The element to marshall.
     * @return A {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected CC_OperationParameter wrap(final ParameterDescriptor<?> parameter) {
        return new CC_OperationParameter(parameter);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:OperationParameter>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The element to be marshalled.
     */
    @XmlElement(name = "OperationParameter")
    public DefaultParameterDescriptor<?> getElement() {
        return DefaultParameterDescriptor.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param parameter The unmarshalled element.
     */
    public void setElement(final DefaultParameterDescriptor<?> parameter) {
        metadata = parameter;
    }

    /**
     * Invoked by JAXB during unmarshalling of the enclosing {@code <gml:OperationParameter>},
     * before the child {@link DefaultParameterDescriptor}. This method stores the class and
     * the unit of measurement of the parameter descriptor to create. Those information will
     * be used by the {@link DefaultParameterDescriptor} private constructor.
     *
     * @param unmarshaller The unmarshaller.
     * @param parent The enclosing {@link ParameterValue} instance being unmarshalled.
     */
    private void beforeUnmarshal(final Unmarshaller unmarshaller, final Object parent) {
        if (parent instanceof ParameterValue<?>) {
            final Object value = ((ParameterValue<?>) parent).getValue();
            if (value != null) {
                valueClass = value.getClass();
                Unit<?> unit = ((ParameterValue<?>) parent).getUnit();
                if (unit != null) {
                    unit = unit.toSI();
                    if (SI.RADIAN.equals(unit)) {
                        unit = NonSI.DEGREE_ANGLE;
                    }
                    assert (valueClass == Double.class) || (valueClass == double[].class) : valueClass;
                    valueDomain = MeasurementRange.create(Double.NEGATIVE_INFINITY, false,
                                                          Double.POSITIVE_INFINITY, false, unit);
                }
                Context.setWrapper(Context.current(), this);
            }
        }
    }

    /**
     * Clears the value class and units of measurement after {@code <gml:OperationParameter>} unmarshalling
     * for avoiding that those information are wrongly used for an unrelated {@link ParameterDescriptor}.
     */
    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        Context.setWrapper(Context.current(), null);
    }
}
