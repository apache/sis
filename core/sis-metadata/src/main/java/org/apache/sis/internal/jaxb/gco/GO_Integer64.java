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


/**
 * Wraps a long value in an {@code <gco:Integer>} element.
 * The ISO 19115-3 standard requires most types to be wrapped by an element representing the value type.
 * The JAXB default behavior is to marshal primitive Java types directly, without such wrapper element.
 * The role of this class is to add the {@code <gco:…>} wrapper element required by ISO 19115-3.
 *
 * @author  Cédric Briançon (Geomatys)
 * @version 0.4
 * @since   0.4
 * @module
 */
public final class GO_Integer64 extends PropertyType<GO_Integer64, Long> {
    /**
     * Empty constructor used only by JAXB.
     */
    public GO_Integer64() {
    }

    /**
     * Constructs a wrapper for the given value.
     *
     * @param  value  the value.
     */
    private GO_Integer64(final Long value) {
        super(value, value == 0L);
    }

    /**
     * Returns the Java type which is bound by this adapter.
     *
     * @return {@code Long.class}
     */
    @Override
    protected Class<Long> getBoundType() {
        return Long.class;
    }

    /**
     * Allows JAXB to change the result of the marshalling process, according to the
     * ISO 19115-3 standard and its requirements about primitive types.
     *
     * @param  value  the integer value we want to surround by an element representing its type.
     * @return an adaptation of the integer value, that is to say a integer value surrounded
     *         by {@code <gco:Integer>} element.
     */
    @Override
    protected GO_Integer64 wrap(final Long value) {
        return new GO_Integer64(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual value to write.
     *
     * @return the value to be marshalled.
     */
    @XmlElement(name = "Integer")
    @XmlSchemaType(name = "integer")
    public Long getElement() {
        return metadata;
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  metadata  the unmarshalled value.
     */
    public void setElement(final Long metadata) {
        this.metadata = metadata;
    }
}
