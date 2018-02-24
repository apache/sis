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

import org.opengis.util.GenericName;
import org.apache.sis.internal.jaxb.FilterByVersion;


/**
 * JAXB wrapper in order to map implementing class with the GeoAPI interface.
 * This adapter is used for all the following mutually exclusive properties
 * (only one can be defined at time):
 *
 * <ul>
 *   <li>{@code LocalName}</li>
 *   <li>{@code ScopedName}</li>
 *   <li>{@code TypeName}</li>
 *   <li>{@code MemberName}</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public class GO_GenericName extends NameAdapter<GO_GenericName, GenericName> {
    /**
     * Empty constructor for JAXB only.
     */
    public GO_GenericName() {
    }

    /**
     * Wraps a name at marshalling-time.
     */
    private GO_GenericName(final GenericName value) {
        name = value;
    }

    /**
     * Replaces a generic name by its wrapper.
     * JAXB calls automatically this method at marshalling-time.
     *
     * @param  value  the implementing class for this metadata value.
     * @return an wrapper which contains the metadata value.
     */
    @Override
    public GO_GenericName marshal(final GenericName value) {
        return (value != null) ? new GO_GenericName(value) : null;
    }

    /**
     * Unwraps the generic name from the given element.
     * JAXB calls automatically this method at unmarshalling-time.
     *
     * @param  value  the wrapper, or {@code null} if none.
     * @return the implementing class.
     */
    @Override
    public final GenericName unmarshal(final GO_GenericName value) {
        return (value != null) ? value.name : null;
    }

    /**
     * Wraps the value only if marshalling ISO 19115-3 element.
     * Otherwise (i.e. if marshalling a legacy ISO 19139:2007 document), omit the element.
     */
    public static final class Since2014 extends GO_GenericName {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override public GO_GenericName marshal(final GenericName value) {
            return FilterByVersion.CURRENT_METADATA.accept() ? super.marshal(value) : null;
        }
    }
}
