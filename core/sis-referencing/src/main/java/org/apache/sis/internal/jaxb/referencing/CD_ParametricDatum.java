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
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.referencing.datum.DefaultParametricDatum;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class CD_ParametricDatum extends PropertyType<CD_ParametricDatum, DefaultParametricDatum> {
    /**
     * Empty constructor for JAXB only.
     */
    public CD_ParametricDatum() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code ParametricDatum.class}
     */
    @Override
    protected Class<DefaultParametricDatum> getBoundType() {
        return DefaultParametricDatum.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CD_ParametricDatum(final DefaultParametricDatum datum) {
        super(datum);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:ParametricDatum>} XML element.
     *
     * @param  datum The element to marshall.
     * @return A {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected CD_ParametricDatum wrap(final DefaultParametricDatum datum) {
        return new CD_ParametricDatum(datum);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:ParametricDatum>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The element to be marshalled.
     */
    @XmlElement(name = "ParametricDatum")
    public DefaultParametricDatum getElement() {
        return metadata;
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param datum The unmarshalled element.
     */
    public void setElement(final DefaultParametricDatum datum) {
        metadata = datum;
    }
}
