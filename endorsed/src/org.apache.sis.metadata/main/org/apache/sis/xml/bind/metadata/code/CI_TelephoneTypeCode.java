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
package org.apache.sis.xml.bind.metadata.code;

import jakarta.xml.bind.annotation.XmlElement;
import org.apache.sis.pending.geoapi.evolution.UnsupportedCodeListAdapter;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.cat.CodeListUID;
import org.apache.sis.xml.Namespaces;


/**
 * JAXB adapter for {@code TelephoneType}
 * in order to wrap the value in an XML element as specified by ISO 19115-3 standard.
 * See package documentation for more information about the handling of {@code CodeList} in ISO 19115-3.
 *
 * @author  Cullen Rombach (Image Matters)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.0
 */
public class CI_TelephoneTypeCode extends UnsupportedCodeListAdapter<CI_TelephoneTypeCode> {
    /**
     * Empty constructor for JAXB only.
     */
    public CI_TelephoneTypeCode() {
    }

    /**
     * Creates a new adapter for the given value.
     */
    private CI_TelephoneTypeCode(final CodeListUID value) {
        super(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return the wrapper for the code list value.
     */
    @Override
    protected CI_TelephoneTypeCode wrap(final CodeListUID value) {
        return new CI_TelephoneTypeCode(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return the code list class.
     */
    @Override
    protected String getCodeListName() {
        return "CI_TelephoneTypeCode";
    }

    /**
     * Invoked by JAXB on marshalling.
     *
     * @return the value to be marshalled.
     */
    @Override
    @XmlElement(name = "CI_TelephoneTypeCode", namespace = Namespaces.CIT)
    public final CodeListUID getElement() {
        return identifier;
    }

    /**
     * Invoked by JAXB on unmarshalling.
     *
     * @param  value  the unmarshalled value.
     */
    public final void setElement(final CodeListUID value) {
        identifier = value;
    }

    /**
     * Wraps the value only if marshalling an element from the ISO 19115:2014 metadata model.
     * Otherwise (i.e. if marshalling according legacy ISO 19115:2003 model), omits the element.
     */
    public static final class Since2014 extends CI_TelephoneTypeCode {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override protected CI_TelephoneTypeCode wrap(final CodeListUID value) {
            return FilterByVersion.CURRENT_METADATA.accept() ? super.wrap(value) : null;
        }
    }
}
