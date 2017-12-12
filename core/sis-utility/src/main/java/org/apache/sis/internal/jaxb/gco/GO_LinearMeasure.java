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
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.internal.jaxb.gml.Measure;
import org.apache.sis.measure.Units;


/**
 * The ISO-19103 {@code Measure} with a unit of measure defined in the {@code gco} namespace
 * associated to the {@code http://www.isotc211.org/2005/gco} URL.
 *
 * <p>This class is identical to {@link GO_Distance} except for the name of the element,
 * which is {@code "Measure"}.</p>
 *
 * Used only for ISO 19139 metadata.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class GO_LinearMeasure extends XmlAdapter<GO_LinearMeasure, Double> {
    /**
     * A proxy representation of the {@code <gco:Measure>} element.
     */
    @XmlElement(name = "Measure")
    private Measure measure;

    /**
     * Empty constructor used only by JAXB.
     */
    public GO_LinearMeasure() {
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
    private GO_LinearMeasure(final Double value) {
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
    public Double unmarshal(final GO_LinearMeasure value) {
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
     * ISO-19139 standard and its requirements about {@code measures}.
     *
     * @param  value  the double value we want to integrate into a {@code <gco:Measure>} element.
     * @return an adaptation of the double value, that is to say a double value surrounded
     *         by {@code <gco:Measure>} element, with an {@code uom} attribute.
     */
    @Override
    public GO_LinearMeasure marshal(final Double value) {
        return (value != null) ? new GO_LinearMeasure(value) : null;
    }
}
