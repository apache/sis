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
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gmd.CodeListUID;
import org.apache.sis.internal.geoapi.evolution.UnsupportedCodeListAdapter;


/**
 * JAXB adapter for {@link DistributedComputingPlatform}, in order to integrate the value in an element
 * respecting the ISO-19139 standard. See package documentation for more information about the handling
 * of {@code CodeList} in ISO-19139.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class DCPList extends UnsupportedCodeListAdapter<DCPList> {
    /**
     * Empty constructor for JAXB only.
     */
    public DCPList() {
    }

    /**
     * Creates a new adapter for the given value.
     */
    private DCPList(final CodeListUID value) {
        super(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return The wrapper for the code list value.
     */
    @Override
    protected DCPList wrap(final CodeListUID value) {
        return new DCPList(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return The code list class name.
     */
    @Override
    protected String getCodeListName() {
        return "DCPList";
    }

    /**
     * Converts the given Java constant name to something hopefully close to the UML identifier,
     * or close to the textual value to put in the XML.
     *
     * @param  name    The Java constant name (e.g. {@code WEB_SERVICES}).
     * @param  buffer  An initially empty buffer to use for creating the identifier.
     * @param  isValue {@code false} for the {@code codeListValue} attribute, or {@code true} for the XML value.
     * @return The identifier (e.g. {@code "WebServices"} or {@code "Web services"}).
     */
    @Override
    protected String toIdentifier(final String name, final StringBuilder buffer, final boolean isValue) {
        if (name.startsWith("WEB_")) {
            super.toIdentifier(name, buffer, isValue);
            buffer.setCharAt(0, 'W');
            return buffer.toString();
        } else {
            // Other names are abbreviations (e.g. XML, SQL, FTP, etc.), so return unchanged.
            return name;
        }
    }

    /**
     * Invoked by JAXB on marshalling.
     *
     * @return The value to be marshalled.
     */
    @Override
    @XmlElement(name = "DCPList", namespace = Namespaces.SRV)
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
