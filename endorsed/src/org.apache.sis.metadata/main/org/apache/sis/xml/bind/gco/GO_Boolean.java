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
package org.apache.sis.xml.bind.gco;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import org.apache.sis.metadata.AbstractMetadata;


/**
 * Surrounds boolean value by {@code <gco:Boolean>}.
 * The ISO 19115-3 standard requires most types to be wrapped by an element representing the value type.
 * The JAXB default behavior is to marshal primitive Java types directly, without such wrapper element.
 * The role of this class is to add the {@code <gco:…>} wrapper element required by ISO 19115-3.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlType(name = "Boolean_PropertyType")
public final class GO_Boolean extends PropertyType<GO_Boolean, Boolean> {
    /**
     * Empty constructor used only by JAXB.
     */
    public GO_Boolean() {
    }

    /**
     * Builds a wrapper for the specified value, which may be nil.
     *
     * @param  owner      the metadata providing the value object.
     * @param  property   UML identifier of the property for which a value is provided.
     * @param  value      the property value, or {@code null} if none.
     * @param  mandatory  whether a value is mandatory.
     */
    public GO_Boolean(AbstractMetadata owner, String property, Boolean value, boolean mandatory) {
        super(owner, property, value, mandatory);
    }

    /**
     * Constructs a wrapper for the given value.
     *
     * @param  value  the value.
     */
    @SuppressWarnings("NumberEquality")
    private GO_Boolean(final Boolean value) {
        super(value, false);
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
     * ISO 19115-3 standard and its requirements about primitive types.
     *
     * @param  value  the boolean value we want to surround by an element representing its type.
     * @return an adaptation of the boolean value, that is to say a boolean value surrounded
     *         by {@code <gco:Boolean>} element.
     */
    @Override
    protected GO_Boolean wrap(final Boolean value) {
        return new GO_Boolean(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual value to write.
     *
     * @return the value to be marshalled.
     */
    @XmlElement(name = "Boolean")
    @XmlSchemaType(name = "boolean")
    public Boolean getElement() {
        return metadata;
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  metadata  the unmarshalled value.
     */
    public void setElement(final Boolean metadata) {
        this.metadata = metadata;
    }
}
