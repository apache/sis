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
 * Surrounds boolean value by {@code <gco:Boolean>}.
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
 */
public final class GO_Boolean extends XmlAdapter<GO_Boolean, Boolean> {
    /**
     * Wraps the {@link Boolean#TRUE} value.
     */
    private static final GO_Boolean TRUE = new GO_Boolean(Boolean.TRUE);

    /**
     * Wraps the {@link Boolean#FALSE} value.
     */
    private static final GO_Boolean FALSE = new GO_Boolean(Boolean.FALSE);

    /**
     * The boolean value to handle.
     */
    @XmlElement(name = "Boolean")
    public Boolean value;

    /**
     * Empty constructor used only by JAXB.
     */
    public GO_Boolean() {
    }

    /**
     * Constructs an adapter for the given value.
     *
     * @param value The value.
     */
    private GO_Boolean(final Boolean value) {
        this.value = value;
    }

    /**
     * Allows JAXB to generate a Boolean object using the value found in the adapter.
     *
     * @param value The value wrapped in an adapter.
     * @return The boolean value extracted from the adapter.
     */
    @Override
    public Boolean unmarshal(final GO_Boolean value) {
        return (value != null) ? value.value : null;
    }

    /**
     * Allows JAXB to change the result of the marshalling process, according to the
     * ISO-19139 standard and its requirements about primitive types.
     *
     * @param value The boolean value we want to surround by an element representing its type.
     * @return An adaptation of the boolean value, that is to say a boolean value surrounded
     *         by {@code <gco:Boolean>} element.
     */
    @Override
    public GO_Boolean marshal(final Boolean value) {
        if (value == null) {
            return null;
        }
        final GO_Boolean c = value ? TRUE : FALSE;
        assert value.equals(c.value) : value;
        return c;
    }
}
