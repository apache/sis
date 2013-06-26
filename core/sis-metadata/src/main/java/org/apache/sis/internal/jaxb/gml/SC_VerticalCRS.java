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
package org.apache.sis.internal.jaxb.gml;

import org.opengis.referencing.crs.VerticalCRS;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter for {@link VerticalCRS}, in order to integrate the value in an element
 * complying with OGC/ISO standard. Note that the CRS is formatted using the GML schema,
 * not the ISO 19139 one.
 *
 * <p>This implementation does not contain any WML element, because doing so would require
 * the {@code sis-referencing} module. Module capable to provide an element shall create a
 * subclass like below:</p>
 *
 * {@preformat java
 *     public final class MyClass extends SC_VerticalCRS implements AdapterReplacement {
 *         &#64;Override
 *         public void register(final Marshaller marshaller) {
 *             marshaller.setAdapter(SC_VerticalCRS.class, this);
 *         }
 *
 *         &#64;XmlElement(name = "VerticalCRS")
 *         public DefaultVerticalCRS getElement() {
 *             return skip() ? null : DefaultVerticalCRS.castOrCopy(metadata);
 *         }
 *
 *         public void setElement(final DefaultVerticalCRS metadata) {
 *             this.metadata = metadata;
 *         }
 *     }
 * }
 *
 * The path to {@code MyClass} shall be provided in the module
 * {@code META-INF/services/org.apache.sis.internal.jaxb.AdapterReplacement} file.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.internal.jaxb.AdapterReplacement
 */
public class SC_VerticalCRS extends PropertyType<SC_VerticalCRS, VerticalCRS> {
    /**
     * Empty constructor for JAXB only.
     */
    public SC_VerticalCRS() {
    }

    /**
     * Wraps a Vertical CRS value with a {@code <gml:VerticalCRS>} element at marshalling-time.
     *
     * @param metadata The metadata value to marshal.
     */
    protected SC_VerticalCRS(final VerticalCRS metadata) {
        super(metadata);
    }

    /**
     * Returns the Vertical CRS value wrapped by a {@code <gml:VerticalCRS>} element.
     *
     * @param value The value to marshal.
     * @return The adapter which wraps the metadata value.
     */
    @Override
    protected SC_VerticalCRS wrap(final VerticalCRS value) {
        return new SC_VerticalCRS(value);
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     */
    @Override
    protected final Class<VerticalCRS> getBoundType() {
        return VerticalCRS.class;
    }
}
