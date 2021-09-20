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
import org.opengis.metadata.identification.KeywordClass;
import org.apache.sis.metadata.iso.identification.DefaultKeywordClass;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.5
 * @module
 */
public class MD_KeywordClass extends PropertyType<MD_KeywordClass, KeywordClass> {
    /**
     * Empty constructor for JAXB only.
     */
    public MD_KeywordClass() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code KeywordClass.class}
     */
    @Override
    protected final Class<KeywordClass> getBoundType() {
        return KeywordClass.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private MD_KeywordClass(final KeywordClass value) {
        super(value);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <mri:MD_KeywordClass>} XML element.
     *
     * @param  value  the metadata element to marshal.
     * @return a {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected MD_KeywordClass wrap(final KeywordClass value) {
        return new MD_KeywordClass(value);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <mri:MD_KeywordClass>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return the metadata to be marshalled.
     */
    @XmlElementRef
    public final DefaultKeywordClass getElement() {
        return DefaultKeywordClass.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param  value  the unmarshalled metadata.
     */
    public final void setElement(final DefaultKeywordClass value) {
        metadata = value;
    }

    /**
     * Wraps the value only if marshalling an element from the ISO 19115:2003 metadata model.
     * Otherwise (i.e. if marshalling according legacy ISO 19115:2014 model), omits the element.
     */
    public static final class Since2014 extends MD_KeywordClass {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override protected MD_KeywordClass wrap(final KeywordClass value) {
            return accept2014() ? super.wrap(value) : null;
        }
    }
}
