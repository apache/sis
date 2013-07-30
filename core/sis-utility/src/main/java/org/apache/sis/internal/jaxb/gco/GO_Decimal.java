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


/**
 * Surrounds double values by {@code <gco:Decimal>}.
 * The ISO-19139 standard requires most types to be surrounded by an element representing the value type.
 * The JAXB default behavior is to marshal primitive Java types directly, without such wrapper element.
 * The role of this class is to add the {@code <gco:…>} wrapper element required by ISO 19139.
 *
 * {@section Relationship with <code>GO_Real</code>}
 * This adapter is identical to {@link GO_Real} except for the element name, which is {@code "Decimal"} instead
 * than {@code "Real"}. This adapter is used for the {@code westBoundLongitude}, {@code eastBoundLongitude},
 * {@code southBoundLatitude} and {@code northBoundLatitude} properties of {@code EX_DefaultGeographicBoundingBox}.
 * The {@code GO_Real} adapter is used for about everything else.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 *
 * @see GO_Real
 * @see AsFloat
 */
public final class GO_Decimal extends XmlAdapter<GO_Decimal, Double> {
    /**
     * Frequently used constants.
     */
    private static final GO_Decimal
            P0   = new GO_Decimal(   0.0),
            P1   = new GO_Decimal(   1.0),
            N1   = new GO_Decimal(  -1.0),
            P45  = new GO_Decimal(  45.0),
            N45  = new GO_Decimal( -45.0),
            P90  = new GO_Decimal(  90.0),
            N90  = new GO_Decimal( -90.0),
            P180 = new GO_Decimal( 180.0),
            N180 = new GO_Decimal(-180.0),
            P360 = new GO_Decimal( 360.0),
            N360 = new GO_Decimal(-360.0);

    /**
     * The double value to handle.
     */
    @XmlElement(name = "Decimal")
    public Double value;

    /**
     * Empty constructor used only by JAXB.
     */
    public GO_Decimal() {
    }

    /**
     * Constructs an adapter for the given value.
     *
     * @param value The value.
     */
    private GO_Decimal(final Double value) {
        this.value = value;
    }

    /**
     * Allows JAXB to generate a Double object using the value found in the adapter.
     *
     * @param value The value wrapped in an adapter.
     * @return The double value extracted from the adapter.
     */
    @Override
    public Double unmarshal(final GO_Decimal value) {
        return (value != null) ? value.value : null;
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
    public GO_Decimal marshal(final Double value) {
        if (value == null) {
            return null;
        }
        final GO_Decimal c;
        final int index = value.intValue();
        if (index == value.doubleValue()) {
            switch (index) {
                case    0: c = P0;   break;
                case    1: c = P1;   break;
                case   -1: c = N1;   break;
                case   45: c = P45;  break;
                case  -45: c = N45;  break;
                case   90: c = P90;  break;
                case  -90: c = N90;  break;
                case  180: c = P180; break;
                case -180: c = N180; break;
                case  360: c = P360; break;
                case -360: c = N360; break;
                default: c = new GO_Decimal(value);
            }
        } else {
            c = new GO_Decimal(value);
        }
        assert value.equals(c.value) : value;
        return c;
    }




    /**
     * Surrounds float values by {@code <gco:Decimal>}.
     * The ISO-19139 standard specifies that primitive types have to be surrounded by an element
     * which represents the type of the value, using the namespace {@code gco} linked to the
     * {@code http://www.isotc211.org/2005/gco} URL. The JAXB default behavior is to marshal
     * primitive Java types directly "as is", without wrapping the value in the required element.
     * The role of this class is to add such wrapping.
     *
     * @author  Cédric Briançon (Geomatys)
     * @since   0.3 (derived from geotk-2.5)
     * @version 0.3
     * @module
     */
    public static final class AsFloat extends XmlAdapter<AsFloat, Float> {
        /**
         * The float value to handle.
         */
        @XmlElement(name = "Decimal")
        public Float value;

        /**
         * Empty constructor used only by JAXB.
         */
        public AsFloat() {
        }

        /**
         * Constructs an adapter for the given value.
         *
         * @param value The value.
         */
        private AsFloat(final Float value) {
            this.value = value;
        }

        /**
         * Allows JAXB to generate a Float object using the value found in the adapter.
         *
         * @param value The value wrapped in an adapter.
         * @return The float value extracted from the adapter.
         */
        @Override
        public Float unmarshal(final AsFloat value) {
            return (value != null) ? value.value : null;
        }

        /**
         * Allows JAXB to change the result of the marshalling process, according to the
         * ISO-19139 standard and its requirements about primitive types.
         *
         * @param value The float value we want to surround by an element representing its type.
         * @return An adaptation of the float value, that is to say a float value surrounded
         *         by {@code <gco:Decimal>} element.
         */
        @Override
        public AsFloat marshal(final Float value) {
            return (value != null) ? new AsFloat(value) : null;
        }
    }
}
