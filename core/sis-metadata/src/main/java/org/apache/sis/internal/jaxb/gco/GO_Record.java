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
import org.opengis.util.Record;
import org.apache.sis.util.iso.DefaultRecord;


/**
 * JAXB wrapper in order to map implementing class with the GeoAPI interface.
 * See package documentation for more information about JAXB and interface.
 *
 * @author  Cullen Rombach (Image Matters)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class GO_Record extends PropertyType<GO_Record, Record> {
    /**
     * Empty constructor for JAXB only.
     */
    GO_Record() {
    }

    /**
     * Wraps a {@code Record} value with a {@code gco:Record} element at marshalling-time.
     *
     * @param  metadata  the metadata value to marshal.
     */
    private GO_Record(final Record metadata) {
        super(metadata);
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     *
     * @return {@code Record.class}
     */
    @Override
    protected final Class<Record> getBoundType() {
        return Record.class;
    }

    /**
     * Returns a wrapper for the given {@code Record} element.
     *
     * @param  value  the value to marshal.
     * @return the wrapper around the given metadata value.
     */
    @Override
    protected GO_Record wrap(final Record value) {
        return new GO_Record(value);
    }

    /**
     * Returns the {@link DefaultRecord} generated from the metadata value.
     * This method is systematically called at marshalling-time by JAXB.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElement(name = "Record")
    public final DefaultRecord getElement() {
        return DefaultRecord.castOrCopy(metadata);
    }

    /**
     * Sets the value for the {@link DefaultRecord}.
     * This method is systematically called at unmarshalling-time by JAXB.
     *
     * @param  metadata  the unmarshalled metadata.
     */
    public final void setElement(final DefaultRecord metadata) {
        if (!metadata.getAttributes().isEmpty()) {
            this.metadata = metadata;
        }
    }

    /**
     * Wraps the value only if marshalling ISO 19115-3 element.
     * Otherwise (i.e. if marshalling a legacy ISO 19139:2007 document), omit the element.
     */
    public static final class Since2014 extends GO_Record {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override protected GO_Record wrap(final Record value) {
            return accept2014() ? super.wrap(value) : null;
        }
    }
}
