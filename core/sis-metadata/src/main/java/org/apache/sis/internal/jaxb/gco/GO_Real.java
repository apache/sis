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
 * Wraps double values in {@code <gco:Real>} element.
 * The ISO 19115-3 standard requires most types to be wrapped by an element representing the value type.
 * The JAXB default behavior is to marshal primitive Java types directly, without such wrapper element.
 * The role of this class is to add the {@code <gco:…>} wrapper element required by ISO 19115-3.
 *
 * <h2>Relationship with {@code GO_Decimal}</h2>
 * This adapter is identical to {@link GO_Decimal} except for the element name, which is {@code "Real"}
 * instead of {@code "Decimal"}. This adapter is the most widely used one in IS 19139 XML schema.
 * The few exceptions are documented in {@link GO_Decimal}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
@XmlType(name = "Real_PropertyType")
public class GO_Real extends PropertyType<GO_Real, Double> {
    /**
     * Empty constructor used only by JAXB.
     */
    GO_Real() {
    }

    /**
     * Constructs a wrapper for the given value.
     *
     * @param  value  the value.
     */
    private GO_Real(final Double value) {
        super(value, value.isNaN());
    }

    /**
     * Returns the Java type which is bound by this adapter.
     *
     * @return {@code Double.class}
     */
    @Override
    protected final Class<Double> getBoundType() {
        return Double.class;
    }

    /**
     * Allows JAXB to change the result of the marshalling process, according to the
     * ISO 19115-3 standard and its requirements about primitive types.
     *
     * @param  value  the double value we want to wrap in an element representing its type.
     * @return a wrapper for the double value, that is to say a double value wrapped
     *         by {@code <gco:Real>} element.
     */
    @Override
    protected GO_Real wrap(final Double value) {
        return new GO_Real(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual value to write.
     *
     * @return the value to be marshalled.
     */
    @XmlElement(name = "Real")
    @XmlSchemaType(name = "double")
    public final Double getElement() {
        return metadata;
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  metadata  the unmarshalled value.
     */
    public final void setElement(final Double metadata) {
        this.metadata = metadata;
    }

    /**
     * Wraps the value only if marshalling ISO 19115-3 element.
     * Otherwise (i.e. if marshalling a legacy ISO 19139:2007 document), omit the element.
     */
    public static final class Since2014 extends GO_Real {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override protected GO_Real wrap(final Double value) {
            return accept2014() ? super.wrap(value) : null;
        }
    }
}
