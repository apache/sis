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
import javax.xml.bind.annotation.XmlType;


/**
 * Wraps an "unlimited" integer value in an {@code <gco:UnlimitedInteger>} element.
 * The ISO 19115-3 standard requires most types to be wrapped by an element representing the value type.
 * The JAXB default behavior is to marshal primitive Java types directly, without such wrapper element.
 * The role of this class is to add the {@code <gco:…>} wrapper element required by ISO 19115-3.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see GO_Integer
 * @see GO_Integer64
 *
 * @since 1.0
 * @module
 */
@XmlType(name = "UnlimitedInteger_PropertyType")
final class GO_UnlimitedInteger extends PropertyType<GO_UnlimitedInteger, UnlimitedInteger> {
    /**
     * Empty constructor used only by JAXB.
     */
    GO_UnlimitedInteger() {
    }

    /**
     * Constructs a wrapper for the given value.
     *
     * @param  value  the value.
     */
    private GO_UnlimitedInteger(final UnlimitedInteger value) {
        super(value, false);
    }

    /**
     * Returns the Java type which is bound by this adapter.
     *
     * @return {@code UnlimitedInteger.class}
     */
    @Override
    protected final Class<UnlimitedInteger> getBoundType() {
        return UnlimitedInteger.class;
    }

    /**
     * Allows JAXB to change the result of the marshalling process, according to the
     * ISO 19115-3 standard and its requirements about primitive types.
     *
     * @param  value  the integer value we want to wrap in an element representing its type.
     * @return a wrapper for the integer value, that is to say an integer value wrapped
     *         by {@code <gco:UnlimitedInteger>} element.
     */
    @Override
    protected GO_UnlimitedInteger wrap(final UnlimitedInteger value) {
        return new GO_UnlimitedInteger(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual value to write.
     *
     * @return the value to be marshalled.
     */
    @XmlElement(name = "UnlimitedInteger")
    public final UnlimitedInteger getElement() {
        return metadata;
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  metadata  the unmarshalled value.
     */
    public final void setElement(final UnlimitedInteger metadata) {
        this.metadata = metadata;
    }
}
