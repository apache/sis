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
package org.apache.sis.internal.jaxb.gco;

import javax.xml.bind.annotation.XmlElement;
import org.opengis.util.RecordType;
import org.apache.sis.util.iso.DefaultRecordType;


/**
 * JAXB wrapper in order to map implementing class with the GeoAPI interface.
 * See package documentation for more information about JAXB and interface.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class GO_RecordType extends PropertyType<GO_RecordType, RecordType> {
    /**
     * Empty constructor for JAXB only.
     */
    public GO_RecordType() {
    }

    /**
     * Wraps a {@code RecordType} value with a {@code gco:RecordType} tags at marshalling-time.
     *
     * @param metadata The metadata value to marshal.
     */
    private GO_RecordType(final RecordType metadata) {
        super(metadata);
    }

    /**
     * Returns a wrapper for the given {@code RecordType} element.
     *
     * @param  value The value to marshal.
     * @return The wrapper around the given metadata value.
     */
    @Override
    protected GO_RecordType wrap(final RecordType value) {
        return new GO_RecordType(value);
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     *
     * @return {@code RecordType.class}
     */
    @Override
    protected Class<RecordType> getBoundType() {
        return RecordType.class;
    }

    /**
     * Returns the {@link DefaultRecordType} generated from the metadata value.
     * This method is systematically called at marshalling-time by JAXB.
     *
     * @return The metadata to be marshalled.
     */
    @XmlElement(name = "RecordType")
    public DefaultRecordType getElement() {
        return DefaultRecordType.castOrCopy(metadata);
    }

    /**
     * Sets the value for the {@link DefaultRecordType}.
     * This method is systematically called at unmarshalling-time by JAXB.
     *
     * @param metadata The unmarshalled metadata.
     */
    public void setElement(final DefaultRecordType metadata) {
        this.metadata = metadata;
    }
}
