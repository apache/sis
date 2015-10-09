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

import javax.measure.unit.SI;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.internal.jaxb.gml.Measure;


/**
 * The ISO-19103 {@code Distance} with a {@code unit of measure} defined, using the
 * {@code gco} namespace linked to the {@code http://www.isotc211.org/2005/gco} URL.
 *
 * <p>This class is identical to {@link GO_Measure} except for the name of the element,
 * which is {@code "Distance"}.</p>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "Distance_PropertyType")
public final class GO_Distance extends XmlAdapter<GO_Distance, Double> {
    /**
     * A proxy representation of the {@code <gco:Distance>} element.
     */
    @XmlElement(name = "Distance")
    private Measure distance;

    /**
     * Empty constructor used only by JAXB.
     */
    public GO_Distance() {
    }

    /**
     * Constructs an adapter for the given value before marshalling.
     *
     * @param value The value.
     *
     * @todo The unit of measurement is fixed to metres for now because we do not have this information
     *       in current metadata interface. This will need to be revisited in a future SIS version if we
     *       replace the Double type by some quantity type.
     */
    private GO_Distance(final Double value) {
        distance = new Measure(value, SI.METRE);
        distance.asXPointer = true;
    }

    /**
     * Allows JAXB to generate a Double object using the value found in the adapter.
     *
     * @param value The value wrapped in an adapter.
     * @return The double value extracted from the adapter.
     */
    @Override
    public Double unmarshal(final GO_Distance value) {
        if (value != null) {
            final Measure distance = value.distance;
            if (distance != null) {
                return distance.value;
            }
        }
        return null;
    }

    /**
     * Allows JAXB to change the result of the marshalling process, according to the
     * ISO-19139 standard and its requirements about {@code measures}.
     *
     * @param value The double value we want to integrate into a {@code <gco:Distance>} element.
     * @return An adaptation of the double value, that is to say a double value surrounded
     *         by {@code <gco:Distance>} element, with an {@code uom} attribute.
     */
    @Override
    public GO_Distance marshal(final Double value) {
        return (value != null) ? new GO_Distance(value) : null;
    }
}
