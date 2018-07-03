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

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * The {@code <gco:UnlimitedInteger>} value, used for {@link MultiplicityRange} implementation.
 * Despite its name, this {@code UnlimitedInteger} implementation does not handle integers of
 * arbitrary size. The only difference with an ordinary integer is its capability to express
 * infinity.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@XmlType(name = "UnlimitedInteger_Type")
@XmlRootElement(name = "UnlimitedInteger")
final class UnlimitedInteger {
    /**
     * The value, limited to 32 bits integer for now. A null value is interpreted as missing value,
     * unless {@link #isInfinite} is {@code true}.
     */
    @XmlValue
    @XmlSchemaType(name = "nonNegativeInteger")
    private Integer value;

    /**
     * Whether the value should be considered infinite.
     * An infinite value implies {@code xsi:nil = true}.
     */
    @XmlAttribute(name = "isInfinite")
    private Boolean isInfinite;

    /**
     * Creates a new {@code gco:UnlimitedInteger} for a missing value.
     */
    UnlimitedInteger() {
    }

    /**
     * Creates a new {@code gco:UnlimitedInteger} for the given value.
     * A null value is interpreted as infinity (i.e. no bound).
     */
    UnlimitedInteger(Integer value, final boolean inclusive) {
        if (value == null) {
            isInfinite = Boolean.TRUE;
        } else {
            if (!inclusive) {
                value = Math.decrementExact(value);
            }
            this.value = value;
        }
    }

    /**
     * Returns whether the value should be considered infinite.
     */
    final boolean isInfinite() {
        return (isInfinite != null) && isInfinite;
    }

    /**
     * Returns the value, or {@code null} if the value is infinite.
     * This method does not verify if the value is valid (non-nil).
     */
    final Integer value() {
        return isInfinite() ? null : value;
    }

    /**
     * Returns whether the value should be considered unspecified. An infinite value is considered nil.
     * This method never returns {@code false}; if the value is not nil, then {@code null} is returned.
     */
    @XmlAttribute(name = "nil", namespace = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI)
    private Boolean getNil() {
        return (value == null) ? Boolean.TRUE : null;
    }

    /**
     * Sets whether the value should be considered unspecified.
     * This method is invoked by JAXB at unmarshalling time.
     */
    private void setNil(final Boolean nil) {
        if (nil != null && nil) {
            value = null;
            // Leave 'isInfinite' unchanged since an infinite value is also nil.
        }
    }

    /**
     * Returns a string representation for debugging purpose only.
     */
    @Override
    public String toString() {
        return isInfinite() ? "∞" : (value == null) ? "nil" : value.toString();
    }
}
