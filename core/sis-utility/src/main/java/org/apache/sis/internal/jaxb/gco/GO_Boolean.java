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
 * Surrounds boolean value by {@code <gco:Boolean>}.
 * The ISO-19139 standard requires most types to be surrounded by an element representing the value type.
 * The JAXB default behavior is to marshal primitive Java types directly, without such wrapper element.
 * The role of this class is to add the {@code <gco:…>} wrapper element required by ISO 19139.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@XmlType(name = "Boolean_PropertyType")
public final class GO_Boolean extends PropertyType<GO_Boolean, Boolean> {
    /**
     * Empty constructor used only by JAXB.
     */
    public GO_Boolean() {
    }

    /**
     * Constructs a wrapper for the given value.
     *
     * @param value The value.
     */
    @SuppressWarnings("NumberEquality")
    private GO_Boolean(final Boolean value) {
        super(value, !value && value != Boolean.FALSE);
    }

    /**
     * Returns the Java type which is bound by this adapter.
     *
     * @return {@code Boolean.class}
     */
    @Override
    protected Class<Boolean> getBoundType() {
        return Boolean.class;
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
    protected GO_Boolean wrap(final Boolean value) {
        return new GO_Boolean(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual value to write.
     *
     * @return The value to be marshalled.
     */
    @XmlElement(name = "Boolean")
    @XmlSchemaType(name = "boolean")
    public Boolean getElement() {
        return metadata;
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param metadata The unmarshalled value.
     */
    public void setElement(final Boolean metadata) {
        this.metadata = metadata;
    }
}
