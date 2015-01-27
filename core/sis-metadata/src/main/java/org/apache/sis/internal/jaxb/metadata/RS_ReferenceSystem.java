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
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.internal.jaxb.metadata.replace.ReferenceSystemMetadata;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final class RS_ReferenceSystem extends PropertyType<RS_ReferenceSystem, ReferenceSystem> {
    /**
     * Empty constructor for JAXB only.
     */
    public RS_ReferenceSystem() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code ReferenceSystem.class}
     */
    @Override
    protected Class<ReferenceSystem> getBoundType() {
        return ReferenceSystem.class;
    }

    /**
     * Wraps a Reference System value in a {@code MD_ReferenceSystem} element at marshalling-time.
     *
     * @param metadata The metadata value to marshal.
     */
    protected RS_ReferenceSystem(final ReferenceSystem metadata) {
        super(metadata);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <gmd:RS_ReferenceSystem>} XML element.
     *
     * @param  metadata The metadata element to marshall.
     * @return A {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected RS_ReferenceSystem wrap(ReferenceSystem metadata) {
        return new RS_ReferenceSystem(metadata);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <gmd:RS_ReferenceSystem>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The metadata to be marshalled.
     */
    @XmlElementRef
    public ReferenceSystemMetadata getElement() {
        final ReferenceSystem metadata = this.metadata;
        if (metadata == null) {
            return null;
        } else if (metadata instanceof ReferenceSystemMetadata) {
            return (ReferenceSystemMetadata) metadata;
        } else {
            return new ReferenceSystemMetadata(metadata);
        }
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param metadata The unmarshalled metadata.
     */
    public void setElement(final ReferenceSystemMetadata metadata) {
        this.metadata = metadata;
    }
}
