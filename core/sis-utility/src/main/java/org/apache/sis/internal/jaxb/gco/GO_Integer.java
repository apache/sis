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
 * Surrounds integer values by {@code <gco:Integer>}.
 * The ISO-19139 standard specifies that primitive types have to be surrounded by an element
 * which represents the type of the value, using the namespace {@code gco} linked to the
 * {@code http://www.isotc211.org/2005/gco} URL. The JAXB default behavior is to marshal
 * primitive Java types directly "as is", without wrapping the value in the required element.
 * The role of this class is to add such wrapping.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 *
 * @see AsLong
 */
public final class GO_Integer extends XmlAdapter<GO_Integer, Integer> {
    /**
     * Frequently used constants.
     */
    private static final GO_Integer[] CONSTANTS = new GO_Integer[5];
    static {
        for (int i=0; i<CONSTANTS.length; i++) {
            CONSTANTS[i] = new GO_Integer(i);
        }
    }

    /**
     * The integer value to handle.
     */
    @XmlElement(name = "Integer")
    public Integer value;

    /**
     * Empty constructor used only by JAXB.
     */
    public GO_Integer() {
    }

    /**
     * Constructs an adapter for the given value.
     *
     * @param value The value.
     */
    private GO_Integer(final Integer value) {
        this.value = value;
    }

    /**
     * Allows JAXB to generate an Integer object using the value found in the adapter.
     *
     * @param value The value wrapped in an adapter.
     * @return The integer value extracted from the adapter.
     */
    @Override
    public Integer unmarshal(final GO_Integer value) {
        return (value != null) ? value.value : null;
    }

    /**
     * Allows JAXB to change the result of the marshalling process, according to the
     * ISO-19139 standard and its requirements about primitive types.
     *
     * @param value The integer value we want to surround by an element representing its type.
     * @return An adaptation of the integer value, that is to say an integer value surrounded
     *         by {@code <gco:Integer>} element.
     */
    @Override
    public GO_Integer marshal(final Integer value) {
        if (value == null) {
            return null;
        }
        final int i = value;
        final GO_Integer c = (i >= 0 && i < CONSTANTS.length) ? CONSTANTS[i] : new GO_Integer(value);
        assert value.equals(c.value) : value;
        return c;
    }




    /**
     * Surrounds long values by {@code <gco:Integer>}.
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
    public static final class AsLong extends XmlAdapter<AsLong, Long> {
        /**
         * The long value to handle.
         */
        @XmlElement(name = "Integer")
        public Long value;

        /**
         * Empty constructor used only by JAXB.
         */
        public AsLong() {
        }

        /**
         * Constructs an adapter for the given value.
         *
         * @param value The value.
         */
        private AsLong(final Long value) {
            this.value = value;
        }

        /**
         * Allows JAXB to generate a Long object using the value found in the adapter.
         *
         * @param value The value wrapped in an adapter.
         * @return The long value extracted from the adapter.
         */
        @Override
        public Long unmarshal(final AsLong value) {
            return (value != null) ? value.value : null;
        }

        /**
         * Allows JAXB to change the result of the marshalling process, according to the
         * ISO-19139 standard and its requirements about primitive types.
         *
         * @param value The integer value we want to surround by an element representing its type.
         * @return An adaptation of the integer value, that is to say a integer value surrounded
         *         by {@code <gco:Integer>} element.
         */
        @Override
        public AsLong marshal(final Long value) {
            return (value != null) ? new AsLong(value) : null;
        }
    }
}
