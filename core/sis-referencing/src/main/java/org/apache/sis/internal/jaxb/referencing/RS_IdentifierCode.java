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
package org.apache.sis.internal.jaxb.referencing;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.ImmutableIdentifier;


/**
 * JAXB adapter in order to map a {@link ReferenceIdentifier} to a {@link String}.
 * Its goal is to keep only the {@code name} part of a {@link ReferenceIdentifier}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.4 (derived from geotk-3.06)
 * @version 0.4
 * @module
 */
public final class RS_IdentifierCode extends XmlAdapter<String, ReferenceIdentifier> {
    /**
     * Substitutes the value read from an XML stream by the object which will
     * contains the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  code The metadata value.
     * @return A metadata which represents the metadata value.
     */
    @Override
    public ReferenceIdentifier unmarshal(final String code) {
        if (code != null) {
            return new ImmutableIdentifier(null, null, code);
        }
        return null;
    }

    /**
     * Substitutes the identifier by the adapter to be marshalled into an XML file
     * or stream. JAXB calls automatically this method at marshalling time.
     *
     * @param  value The code list value.
     * @return The adapter for the given code list.
     */
    @Override
    public String marshal(final ReferenceIdentifier value) {
        return (value != null) ? value.getCode() : null;
    }
}
