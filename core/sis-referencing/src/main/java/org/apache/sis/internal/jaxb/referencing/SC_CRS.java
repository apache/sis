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

import javax.xml.bind.annotation.XmlElementRef;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
public final class SC_CRS extends PropertyType<SC_CRS, CoordinateReferenceSystem> {
    /**
     * Empty constructor for JAXB only.
     */
    public SC_CRS() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code CoordinateReferenceSystem.class}
     */
    @Override
    protected Class<CoordinateReferenceSystem> getBoundType() {
        return CoordinateReferenceSystem.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private SC_CRS(final CoordinateReferenceSystem crs) {
        super(crs);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:AbstractCRS>} XML element.
     *
     * @param  crs The element to marshall.
     * @return A {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected SC_CRS wrap(final CoordinateReferenceSystem crs) {
        return new SC_CRS(crs);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:AbstractCRS>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The element to be marshalled.
     */
    @XmlElementRef
    public AbstractCRS getElement() {
        return AbstractCRS.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * <div class="note"><b>Note:</b>
     * the unmarshalled CRS may be of {@code GeodeticCRS} type, which is not the most specific GeoAPI type.
     * But the {@code GeographicCRS} and {@code GeocentricCRS} sub-types are currently not part of ISO 19111.
     * We could substitute the CRS by a more specific type here, but this would break the references specified
     * by {@code xlink:href} attributes. For now we live with the {@code GeodeticCRS} as-is — most of Apache SIS
     * should be able to work with that.</div>
     *
     * @param crs The unmarshalled element.
     */
    public void setElement(final AbstractCRS crs) {
        metadata = crs;
        if (crs.getCoordinateSystem() == null) {
            incomplete((crs instanceof CompoundCRS) ? "componentReferenceSystem" : "coordinateSystem");
        }
        if (crs instanceof SingleCRS && ((SingleCRS) crs).getDatum() == null) {
            incomplete("datum");
        }
    }
}
