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
package org.apache.sis.internal.jaxb.metadata.direct;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.apache.sis.internal.jaxb.name.NameValue;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 *
 * @see org.apache.sis.internal.jaxb.name.GO_GenericName
 *
 * @since 0.5
 * @module
 */
public final class GO_ScopedName extends XmlAdapter<NameValue.Scoped, ScopedName> {
    /**
     * Converts a GeoAPI interface to the SIS implementation for XML marshalling.
     *
     * @param  name  the bound type value, here the GeoAPI interface.
     * @return the adapter for the given value, here the SIS implementation.
     */
    @Override
    public NameValue.Scoped marshal(final ScopedName name) {
        if (name != null) {
            final NameValue.Scoped code = new NameValue.Scoped();
            code.setName(name);
            return code;
        }
        return null;
    }

    /**
     * Returns the scope name from the given string.
     *
     * @param  code  the metadata value.
     * @return the value to marshal (which is the same).
     */
    @Override
    public ScopedName unmarshal(final NameValue.Scoped code) {
        if (code != null) {
            final GenericName parsed = code.getName();
            if (parsed instanceof ScopedName) {
                return (ScopedName) parsed;
            }
        }
        return null;
    }
}
