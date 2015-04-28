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

import org.opengis.util.LocalName;


/**
 * JAXB wrapper in order to map implementing class with the GeoAPI interface.
 * This adapter is used for all the following mutually exclusive properties
 * (only one can be defined at time):
 *
 * <ul>
 *   <li>{@code LocalName}</li>
 *   <li>{@code TypeName}</li>
 *   <li>{@code MemberName}</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class GO_LocalName extends NameAdapter<GO_LocalName, LocalName> {
    /**
     * Empty constructor for JAXB only.
     */
    public GO_LocalName() {
    }

    /**
     * Wraps a name at marshalling-time.
     */
    private GO_LocalName(final LocalName value) {
        name = value;
    }

    /**
     * Does the link between an {@link AbstractName} and the adapter associated.
     * JAXB calls automatically this method at marshalling-time.
     *
     * @param  value The implementing class for this metadata value.
     * @return An wrapper which contains the metadata value.
     */
    @Override
    public GO_LocalName marshal(final LocalName value) {
        return (value != null) ? new GO_LocalName(value) : null;
    }

    /**
     * Does the link between adapters and the way they will be unmarshalled.
     * JAXB calls automatically this method at unmarshalling-time.
     *
     * @param  value The wrapper, or {@code null} if none.
     * @return The implementing class.
     */
    @Override
    public LocalName unmarshal(final GO_LocalName value) {
        return (value != null) ? (LocalName) value.name : null;
    }
}
