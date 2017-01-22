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
import org.opengis.referencing.datum.EngineeringDatum;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class CD_EngineeringDatum extends PropertyType<CD_EngineeringDatum, EngineeringDatum> {
    /**
     * Empty constructor for JAXB only.
     */
    public CD_EngineeringDatum() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code EngineeringDatum.class}
     */
    @Override
    protected Class<EngineeringDatum> getBoundType() {
        return EngineeringDatum.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CD_EngineeringDatum(final EngineeringDatum datum) {
        super(datum);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:EngineeringDatum>} XML element.
     *
     * @param  datum  the element to marshall.
     * @return a {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected CD_EngineeringDatum wrap(final EngineeringDatum datum) {
        return new CD_EngineeringDatum(datum);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:EngineeringDatum>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the element to be marshalled.
     */
    @XmlElement(name = "EngineeringDatum")
    public DefaultEngineeringDatum getElement() {
        return DefaultEngineeringDatum.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  datum  the unmarshalled element.
     */
    public void setElement(final DefaultEngineeringDatum datum) {
        metadata = datum;
    }
}
