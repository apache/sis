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

import javax.xml.bind.annotation.XmlElement;
import org.opengis.referencing.cs.VerticalCS;
import org.apache.sis.referencing.cs.DefaultVerticalCS;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class CS_VerticalCS extends PropertyType<CS_VerticalCS, VerticalCS> {
    /**
     * Empty constructor for JAXB only.
     */
    public CS_VerticalCS() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code VerticalCS.class}
     */
    @Override
    protected Class<VerticalCS> getBoundType() {
        return VerticalCS.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CS_VerticalCS(final VerticalCS cs) {
        super(cs);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:VerticalCS>} XML element.
     *
     * @param  cs The element to marshall.
     * @return A {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected CS_VerticalCS wrap(final VerticalCS cs) {
        return new CS_VerticalCS(cs);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:VerticalCS>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The element to be marshalled.
     */
    @XmlElement(name = "VerticalCS")
    public DefaultVerticalCS getElement() {
        return DefaultVerticalCS.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param cs The unmarshalled element.
     */
    public void setElement(final DefaultVerticalCS cs) {
        metadata = cs;
    }
}
