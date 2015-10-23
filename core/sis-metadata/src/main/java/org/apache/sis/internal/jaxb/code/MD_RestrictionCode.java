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

import javax.xml.bind.annotation.XmlElement;
import org.opengis.metadata.constraint.Restriction;
import org.apache.sis.internal.jaxb.gmd.CodeListAdapter;
import org.apache.sis.internal.jaxb.gmd.CodeListUID;


/**
 * JAXB adapter for {@link Restriction}, in order to integrate the value in an element
 * complying with ISO-19139 standard. See package documentation for more information about
 * the handling of {@code CodeList} in ISO-19139.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class MD_RestrictionCode extends CodeListAdapter<MD_RestrictionCode, Restriction> {
    /**
     * Empty constructor for JAXB only.
     */
    public MD_RestrictionCode() {
    }

    /**
     * Creates a new adapter for the given value.
     */
    private MD_RestrictionCode(final CodeListUID value) {
        super(value);
    }

    /**
     * Fix the spelling of words that changed between ISO 19115:2003 and ISO 19115:2014,
     * then wraps the value into an adapter.
     *
     * <p>The spelling of "license" was changed to "licence" in latest standard, but XML
     * marshalling shall use the previous spelling until XML schema are updated.</p>
     *
     * @param value The value version of {@link CodeList}, to be marshalled.
     * @return The wrapper for the code list value.
     */
    @Override
    protected MD_RestrictionCode wrap(final CodeListUID value) {
        if ("licence".equals(value.codeListValue)) {
            value.codeListValue = "license";
        }
        return new MD_RestrictionCode(value);
    }

    /**
     * Returns the class of code list wrapped by this adapter.
     *
     * @return The code list class.
     */
    @Override
    protected Class<Restriction> getCodeListClass() {
        return Restriction.class;
    }

    /**
     * Invoked by JAXB on marshalling.
     *
     * @return The value to be marshalled.
     */
    @Override
    @XmlElement(name = "MD_RestrictionCode")
    public CodeListUID getElement() {
        return identifier;
    }

    /**
     * Invoked by JAXB on unmarshalling.
     *
     * @param value The unmarshalled value.
     */
    public void setElement(final CodeListUID value) {
        identifier = value;
    }
}
