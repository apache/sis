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

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.internal.jaxb.gml.Measure;
import org.apache.sis.measure.Units;


/**
 * The ISO 19103 {@code Measure} with a unit of measure defined in the {@code gco} namespace
 * associated to the {@code http://www.isotc211.org/2005/gco} URL.
 *
 * <p>This class is identical to {@link GO_Distance} except for the name of the element,
 * which is {@code "Measure"}.</p>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
@XmlType(name = "Measure_PropertyType")
public final class GO_Measure extends XmlAdapter<GO_Measure, Double> {
    /**
     * A proxy representation of the {@code <gco:Measure>} element.
     */
    @XmlElement(name = "Measure")
    private Measure measure;

    /**
     * Empty constructor used only by JAXB.
     */
    public GO_Measure() {
    }

    /**
     * Constructs an adapter for the given value before marshalling.
     *
     * @param  value  the value.
     *
     * @todo The unit of measurement is fixed to metres for now because we do not have this information
     *       in current metadata interface. This will need to be revisited in a future SIS version if we
     *       replace the Double type by some quantity type.
     */
    private GO_Measure(final Double value) {
        measure = new Measure(value, Units.METRE);
        measure.asXPointer = true;
    }

    /**
     * Allows JAXB to generate a Double object using the value found in the adapter.
     *
     * @param  value  the value wrapped in an adapter.
     * @return the double value extracted from the adapter.
     */
    @Override
    public Double unmarshal(final GO_Measure value) {
        if (value != null) {
            final Measure measure = value.measure;
            if (measure != null) {
                return measure.value;
            }
        }
        return null;
    }

    /**
     * Allows JAXB to change the result of the marshalling process, according to the
     * ISO 19115-3 standard and its requirements about {@code measures}.
     *
     * @param  value  the double value we want to wrap into a {@code <gco:Measure>} element.
     * @return a double value wrapped by {@code <gco:Measure>} element with an {@code uom} attribute.
     */
    @Override
    public GO_Measure marshal(final Double value) {
        return (value != null) ? new GO_Measure(value) : null;
    }
}
