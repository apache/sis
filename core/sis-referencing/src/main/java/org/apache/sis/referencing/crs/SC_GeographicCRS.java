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
package org.apache.sis.referencing.crs;

import javax.xml.bind.annotation.XmlElement;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter for {@link GeographicCRS}, in order to integrate the value in an element
 * complying with OGC/ISO standard.
 *
 * <p><b>Note:</b> JAXB adapters are usually declared in the {@link org.apache.sis.internal.jaxb.referencing}
 * package, but this one is an exception because it needs access to package-privated {@link DefaultGeodeticCRS}
 * class.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class SC_GeographicCRS extends PropertyType<SC_GeographicCRS, GeographicCRS> {
    /**
     * Empty constructor for JAXB only.
     */
    public SC_GeographicCRS() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code GeographicCRS.class}
     */
    @Override
    protected Class<GeographicCRS> getBoundType() {
        return GeographicCRS.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private SC_GeographicCRS(final GeographicCRS cs) {
        super(cs);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:GeodeticCRS>} XML element.
     *
     * @param  cs The element to marshall.
     * @return A {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected SC_GeographicCRS wrap(final GeographicCRS cs) {
        return new SC_GeographicCRS(cs);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:GeodeticCRS>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The element to be marshalled.
     */
    @XmlElement(name = "GeodeticCRS")
    public DefaultGeodeticCRS getElement() {
        final GeographicCRS metadata = this.metadata;
        if (metadata == null || metadata instanceof DefaultGeodeticCRS) {
            return (DefaultGeodeticCRS) metadata;
        } else {
            return new DefaultGeodeticCRS(metadata);
        }
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param cs The unmarshalled element.
     */
    public void setElement(final DefaultGeodeticCRS cs) {
        if (cs == null || cs instanceof GeographicCRS) {
            metadata = (GeographicCRS) cs;
        } else {
            metadata = new DefaultGeographicCRS(cs);
        }
    }
}
