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
package org.apache.sis.internal.jaxb.code;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.metadata.Obligation;
import org.apache.sis.util.iso.Types;


/**
 * JAXB adapter for {@link Obligation}, in order to wraps the value in an element
 * complying with ISO-19139 standard. See package documentation for more information
 * about the handling of {@code CodeList} in ISO-19139.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class MD_ObligationCode extends XmlAdapter<String, Obligation> {
    /**
     * Returns the obligation enumeration for the given name.
     *
     * @param value The obligation name.
     * @return The obligation enumeration for the given name.
     */
    @Override
    public Obligation unmarshal(String value) {
        return Types.forCodeName(Obligation.class, value, true);
    }

    /**
     * Returns the name of the given obligation.
     *
     * @param value The obligation enumeration.
     * @return The name of the given obligation.
     */
    @Override
    public String marshal(final Obligation value) {
        if (value == null) {
            return null;
        }
        return value.name();
    }
}
