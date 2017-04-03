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
package org.apache.sis.internal.jaxb.metadata;

import javax.xml.bind.annotation.XmlElementRef;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.apache.sis.metadata.iso.quality.AbstractPositionalAccuracy;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.6
 * @since   0.6
 * @module
 */
public final class DQ_PositionalAccuracy extends PropertyType<DQ_PositionalAccuracy, PositionalAccuracy> {
    /**
     * Empty constructor for JAXB only.
     */
    public DQ_PositionalAccuracy() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code PositionalAccuracy.class}
     */
    @Override
    protected Class<PositionalAccuracy> getBoundType() {
        return PositionalAccuracy.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private DQ_PositionalAccuracy(final PositionalAccuracy metadata) {
        super(metadata);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <gmd:DQ_PositionalAccuracy>} XML element.
     *
     * @param  metadata  the metadata element to marshall.
     * @return a {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected DQ_PositionalAccuracy wrap(final PositionalAccuracy metadata) {
        return new DQ_PositionalAccuracy(metadata);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <gmd:DQ_PositionalAccuracy>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElementRef
    public AbstractPositionalAccuracy getElement() {
        return AbstractPositionalAccuracy.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  metadata  the unmarshalled metadata.
     */
    public void setElement(final AbstractPositionalAccuracy metadata) {
        this.metadata = metadata;
    }
}
