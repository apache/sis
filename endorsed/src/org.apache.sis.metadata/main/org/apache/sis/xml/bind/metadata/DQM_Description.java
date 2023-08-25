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
package org.apache.sis.xml.bind.metadata;

import jakarta.xml.bind.annotation.XmlElementRef;
import org.apache.sis.metadata.iso.quality.DefaultMeasureDescription;
import org.apache.sis.xml.bind.gco.PropertyType;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.quality.Description;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.3
 */
public final class DQM_Description extends PropertyType<DQM_Description, Description> {
    /**
     * Empty constructor for JAXB only.
     */
    public DQM_Description() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code Description.class}
     */
    @Override
    protected Class<Description> getBoundType() {
        return Description.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private DQM_Description(final Description metadata) {
        super(metadata);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <dqm:DQ_Description>} XML element.
     *
     * @param  metadata  the metadata element to marshal.
     * @return a {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected DQM_Description wrap(final Description metadata) {
        return new DQM_Description(metadata);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <dqm:DQ_Description>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElementRef
    public DefaultMeasureDescription getElement() {
        return DefaultMeasureDescription.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  metadata  the unmarshalled metadata.
     */
    public void setElement(final DefaultMeasureDescription metadata) {
        this.metadata = metadata;
    }
}
