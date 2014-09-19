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
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class CI_Responsibility extends PropertyType<CI_Responsibility, DefaultResponsibility> {
    /**
     * Empty constructor for JAXB only.
     */
    public CI_Responsibility() {
    }

    /**
     * Returns the type which is bound by this adapter.
     *
     * @return {@code Responsibility.class}
     */
    @Override
    protected Class<DefaultResponsibility> getBoundType() {
        return DefaultResponsibility.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CI_Responsibility(final DefaultResponsibility metadata) {
        super(metadata);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <gmd:CI_Responsibility>} XML element.
     *
     * @param  metadata The metadata element to marshall.
     * @return A {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected CI_Responsibility wrap(final DefaultResponsibility metadata) {
        return new CI_Responsibility(metadata);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <gmd:CI_Responsibility>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The metadata to be marshalled.
     */
    @XmlElementRef
    @SuppressWarnings("deprecation")
    public DefaultResponsibility getElement() {
        if (LEGACY_XML && !(metadata instanceof DefaultResponsibleParty)) {
            return new DefaultResponsibleParty(metadata);
        } else {
            return metadata;
        }
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param metadata The unmarshalled metadata.
     */
    public void setElement(final DefaultResponsibility metadata) {
        this.metadata = metadata;
    }
}
