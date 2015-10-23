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
package org.apache.sis.internal.jaxb.gml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.util.CodeList;
import org.apache.sis.util.iso.Types;


/**
 * JAXB adapter for GML code lists, in order to integrate the value in an element
 * complying with GML standard. A subclass shall exist for each code list.
 *
 * @param <BoundType> The code list being wrapped.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract class CodeListAdapter<BoundType extends CodeList<BoundType>> extends XmlAdapter<CodeListUID,BoundType> {
    /**
     * Empty constructor for subclasses only.
     */
    protected CodeListAdapter() {
    }

    /**
     * Returns the class of code list wrapped by this adapter.
     *
     * @return The code list class.
     */
    protected abstract Class<BoundType> getCodeListClass();

    /**
     * Returns the default code space for the wrapped code list.
     * The default implementation returns {@code null}.
     *
     * @return The default code space, or {@code null}.
     */
    protected String getCodeSpace() {
        return null;
    }

    /**
     * Substitutes the adapter value read from an XML stream by the object which will
     * contain the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  identifier The code space and identifier.
     * @return A code list which represents the GML value.
     */
    @Override
    public final BoundType unmarshal(final CodeListUID identifier) {
        return (identifier != null) ? Types.forCodeName(getCodeListClass(), identifier.value, true) : null;
    }

    /**
     * Substitutes the code list by the proxy to be marshalled into an XML file
     * or stream. JAXB calls automatically this method at marshalling time.
     *
     * @param  code The code list value.
     * @return The proxy for the given code list.
     */
    @Override
    public final CodeListUID marshal(final BoundType code) {
        return (code != null) ? new CodeListUID(getCodeSpace(), code) : null;
    }
}
