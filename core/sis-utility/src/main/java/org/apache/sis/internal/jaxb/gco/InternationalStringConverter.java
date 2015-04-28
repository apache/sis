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

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.SimpleInternationalString;


/**
 * JAXB adapter for string values mapped to {@link InternationalString}. At the difference of
 * {@link InternationalStringAdapter}, this converter doesn't wrap the string in a new object.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class InternationalStringConverter extends XmlAdapter<String,InternationalString> {
    /**
     * Empty constructor for JAXB.
     */
    public InternationalStringConverter() {
    }

    /**
     * Converts an object read from a XML stream to an {@link InternationalString}
     * implementation. JAXB invokes automatically this method at unmarshalling time.
     *
     * @param  adapter The adapter for the string value.
     * @return An {@link InternationalString} for the string value.
     */
    @Override
    public InternationalString unmarshal(final String adapter) {
        return (adapter != null) ? new SimpleInternationalString(adapter) : null;
    }

    /**
     * Converts an {@link InternationalString} to an object to formatted into a
     * XML stream. JAXB invokes automatically this method at marshalling time.
     *
     * @param  value The string value.
     * @return The adapter for the string.
     */
    @Override
    public String marshal(final InternationalString value) {
        return StringAdapter.toString(value);
    }
}
