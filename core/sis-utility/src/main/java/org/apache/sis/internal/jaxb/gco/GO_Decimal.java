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
package org.apache.sis.internal.jaxb.gco;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Surrounds double values by {@code <gco:Decimal>}.
 * The ISO-19139 standard requires most types to be surrounded by an element representing the value type.
 * The JAXB default behavior is to marshal primitive Java types directly, without such wrapper element.
 * The role of this class is to add the {@code <gco:…>} wrapper element required by ISO 19139.
 *
 * <div class="section">Relationship with {@code GO_Real}</div>
 * This adapter is identical to {@link GO_Real} except for the element name, which is {@code "Decimal"} instead
 * than {@code "Real"}. This adapter is used for the {@code westBoundLongitude}, {@code eastBoundLongitude},
 * {@code southBoundLatitude} and {@code northBoundLatitude} properties of {@code EX_DefaultGeographicBoundingBox}.
 * The {@code GO_Real} adapter is used for about everything else.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see GO_Real
 * @see GO_Decimal32
 */
@XmlType(name = "Decimal_PropertyType")
public final class GO_Decimal extends PropertyType<GO_Decimal, Double> {
    /**
     * Empty constructor used only by JAXB.
     */
    public GO_Decimal() {
    }

    /**
     * Constructs a wrapper for the given value.
     *
     * @param value The value.
     */
    private GO_Decimal(final Double value) {
        super(value, value.isNaN());
    }

    /**
     * Returns the Java type which is bound by this adapter.
     *
     * @return {@code Double.class}
     */
    @Override
    protected Class<Double> getBoundType() {
        return Double.class;
    }

    /**
     * Allows JAXB to change the result of the marshalling process, according to the
     * ISO-19139 standard and its requirements about primitive types.
     *
     * @param value The double value we want to surround by an element representing its type.
     * @return An adaptation of the double value, that is to say a double value surrounded
     *         by {@code <gco:Decimal>} element.
     */
    @Override
    public GO_Decimal wrap(final Double value) {
        return new GO_Decimal(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual value to write.
     *
     * @return The value to be marshalled.
     */
    @XmlElement(name = "Decimal")
    @XmlSchemaType(name = "decimal")
    public Double getElement() {
        return metadata;
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param metadata The unmarshalled value.
     */
    public void setElement(final Double metadata) {
        this.metadata = metadata;
    }
}
