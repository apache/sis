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
package org.apache.sis.internal.jaxb.referencing;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.opengis.referencing.crs.VerticalCRS;
import org.apache.sis.internal.jaxb.AdapterReplacement;
import org.apache.sis.referencing.crs.DefaultVerticalCRS;


/**
 * JAXB adapter for {@link VerticalCRS}, in order to integrate the value in an element
 * complying with OGC/ISO standard. Note that the CRS is formatted using the GML schema,
 * not the ISO 19139 one.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class SC_VerticalCRS extends org.apache.sis.internal.jaxb.gml.SC_VerticalCRS implements AdapterReplacement {
    /**
     * Empty constructor for JAXB only.
     */
    public SC_VerticalCRS() {
    }

    /**
     * Wraps a Vertical CRS value in a {@code <gml:VerticalCRS>} element at marshalling-time.
     *
     * @param crs The value to marshall.
     */
    private SC_VerticalCRS(final VerticalCRS crs) {
        super(crs);
    }

    /**
     * Replaces the {@code sis-metadata} adapter by this adapter.
     */
    @Override
    public void register(final Marshaller marshaller) {
        marshaller.setAdapter(org.apache.sis.internal.jaxb.gml.SC_VerticalCRS.class, this);
    }

    /**
     * Replaces the {@code sis-metadata} adapter by this adapter.
     */
    @Override
    public void register(final Unmarshaller unmarshaller) {
        unmarshaller.setAdapter(org.apache.sis.internal.jaxb.gml.SC_VerticalCRS.class, this);
    }

    /**
     * Returns the Vertical CRS value wrapped by a {@code <gml:VerticalCRS>} element.
     *
     * @param value The value to marshal.
     * @return The wrapper for the metadata value.
     */
    @Override
    protected org.apache.sis.internal.jaxb.gml.SC_VerticalCRS wrap(final VerticalCRS value) {
        return new SC_VerticalCRS(value);
    }

    /**
     * Returns the {@link DefaultVerticalCRS} created from the metadata value.
     * This method is systematically called at marshalling-time by JAXB.
     *
     * @return The CRS to be marshalled.
     */
    @Override
    public Object getElement() {
        return DefaultVerticalCRS.castOrCopy(metadata);
    }
}
