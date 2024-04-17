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

import jakarta.xml.bind.annotation.XmlElement;
import org.opengis.referencing.crs.GeodeticCRS;
import org.apache.sis.xml.bind.gco.PropertyType;


/**
 * JAXB adapter for {@link GeodeticCRS}, in order to integrate the value in an element
 * complying with OGC/ISO standard.
 *
 * <p><b>Note:</b> JAXB adapters are usually declared in the {@link org.apache.sis.xml.bind.referencing} package,
 * but this one is an exception because it needs access to package-private {@link DefaultGeodeticCRS} class.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class SC_GeodeticCRS extends PropertyType<SC_GeodeticCRS, GeodeticCRS> {
    /**
     * Empty constructor for JAXB only.
     */
    public SC_GeodeticCRS() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code GeodeticCRS.class}
     */
    @Override
    protected Class<GeodeticCRS> getBoundType() {
        return GeodeticCRS.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    SC_GeodeticCRS(final GeodeticCRS crs) {
        super(crs);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:GeodeticCRS>} XML element.
     *
     * @param  crs  the element to marshal.
     * @return a {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected SC_GeodeticCRS wrap(final GeodeticCRS crs) {
        return new SC_GeodeticCRS(crs);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:GeodeticCRS>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the element to be marshalled.
     */
    @XmlElement(name = "GeodeticCRS")
    public DefaultGeodeticCRS getElement() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final GeodeticCRS metadata = this.metadata;
        if (metadata == null || metadata instanceof DefaultGeodeticCRS) {
            return (DefaultGeodeticCRS) metadata;
        } else {
            return new DefaultGeodeticCRS(metadata);
        }
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  crs  the unmarshalled element.
     */
    public void setElement(final DefaultGeodeticCRS crs) {
        metadata = crs;
    }
}
