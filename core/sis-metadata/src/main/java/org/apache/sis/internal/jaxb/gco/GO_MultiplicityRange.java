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
import org.apache.sis.measure.NumberRange;


/**
 * Adapter for a component of a multiplicity, consisting of an non-negative lower bound,
 * and a potentially infinite upper bound.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class GO_MultiplicityRange extends PropertyType<GO_MultiplicityRange, NumberRange<Integer>> {
    /**
     * Empty constructor used only by JAXB.
     */
    GO_MultiplicityRange() {
    }

    /**
     * Constructs a wrapper for the given value.
     *
     * @param  value  the value.
     */
    private GO_MultiplicityRange(final NumberRange<Integer> value) {
        super(value, false);
    }

    /**
     * Returns the Java type which is bound by this adapter.
     *
     * @return {@code MultiplicityRange.class}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected final Class<NumberRange<Integer>> getBoundType() {
        return (Class) NumberRange.class;
    }

    /**
     * Allows JAXB to change the result of the marshalling process, according to the
     * ISO 19115-3 standard and its requirements about primitive types.
     *
     * @param  value  the integer range we want to wrap in an element representing its type.
     * @return a wrapper for the integer range, that is to say an integer value wrapped
     *         by {@code <gco:MultiplicityRange>} element.
     */
    @Override
    protected GO_MultiplicityRange wrap(final NumberRange<Integer> value) {
        return new GO_MultiplicityRange(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual value to write.
     *
     * @return the value to be marshalled.
     */
    @XmlElement(name = "MultiplicityRange")
    private MultiplicityRange getElement() {
        return MultiplicityRange.wrap(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  metadata  the unmarshalled value.
     */
    private void setElement(final MultiplicityRange metadata) {
        if (metadata != null) {
            this.metadata = metadata.value();
        }
    }
}
