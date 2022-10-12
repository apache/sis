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
import org.opengis.metadata.quality.SourceReference;
import org.apache.sis.metadata.iso.quality.DefaultSourceReference;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public final class DQM_SourceReference extends PropertyType<DQM_SourceReference, SourceReference> {
    /**
     * Empty constructor for JAXB only.
     */
    public DQM_SourceReference() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code SourceReference.class}
     */
    @Override
    protected Class<SourceReference> getBoundType() {
        return SourceReference.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private DQM_SourceReference(final SourceReference metadata) {
        super(metadata);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <dqm:DQ_SourceReference>} XML element.
     *
     * @param  metadata  the metadata element to marshal.
     * @return a {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected DQM_SourceReference wrap(final SourceReference metadata) {
        return new DQM_SourceReference(metadata);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <dqm:DQ_SourceReference>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElementRef
    public DefaultSourceReference getElement() {
        return DefaultSourceReference.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  metadata  the unmarshalled metadata.
     */
    public void setElement(final DefaultSourceReference metadata) {
        this.metadata = metadata;
    }
}