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

import org.opengis.util.MemberName;


/**
 * Same as {@link GO_GenericName}, but for cases where the element type is declared as {@code MemberName}
 * instead than {@code GenericName}. This adapter does not provide any new functionality; its sole purpose
 * is to declare types matching JAXB expectation.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class GO_MemberName extends NameAdapter<GO_MemberName, MemberName> {
    /**
     * Empty constructor for JAXB only.
     */
    public GO_MemberName() {
    }

    /**
     * Wraps a name at marshalling-time.
     */
    private GO_MemberName(final MemberName value) {
        name = value;
    }

    /**
     * Replaces a generic name by its wrapper.
     * JAXB calls automatically this method at marshalling time.
     *
     * @param  value  the implementing class for this metadata value.
     * @return an wrapper which contains the metadata value.
     */
    @Override
    public GO_MemberName marshal(final MemberName value) {
        return (value != null) ? new GO_MemberName(value) : null;
    }

    /**
     * Unwraps the generic name from the given element.
     * JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value  the wrapper, or {@code null} if none.
     * @return the implementing class.
     */
    @Override
    public MemberName unmarshal(final GO_MemberName value) {
        return (value != null) ? (MemberName) value.name : null;
    }
}
