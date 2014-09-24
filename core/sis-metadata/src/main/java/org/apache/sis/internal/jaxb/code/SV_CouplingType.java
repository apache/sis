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
import org.opengis.metadata.identification.CouplingType;
import org.apache.sis.internal.jaxb.gmd.CodeListAdapter;
import org.apache.sis.internal.jaxb.gmd.CodeListProxy;
import org.apache.sis.xml.Namespaces;


/**
 * JAXB adapter for {@link CouplingType}, in order to integrate the value in an element respecting
 * the ISO-19139 standard. See package documentation for more information about the handling
 * of {@code CodeList} in ISO-19139.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class SV_CouplingType extends CodeListAdapter<SV_CouplingType, CouplingType> {
    /**
     * Empty constructor for JAXB only.
     */
    public SV_CouplingType() {
    }

    /**
     * Creates a new adapter for the given proxy.
     */
    private SV_CouplingType(final CodeListProxy proxy) {
        super(proxy);
    }

    /**
     * {@inheritDoc}
     *
     * @return The wrapper for the code list value.
     */
    @Override
    protected SV_CouplingType wrap(CodeListProxy proxy) {
        return new SV_CouplingType(proxy);
    }

    /**
     * {@inheritDoc}
     *
     * @return The code list class.
     */
    @Override
    protected Class<CouplingType> getCodeListClass() {
        return CouplingType.class;
    }

    /**
     * Invoked by JAXB on marshalling.
     *
     * @return The value to be marshalled.
     */
    @Override
    @XmlElement(name = "SV_CouplingType", namespace = Namespaces.SRV)
    public CodeListProxy getElement() {
        return proxy;
    }

    /**
     * Invoked by JAXB on unmarshalling.
     *
     * @param proxy The unmarshalled value.
     */
    public void setElement(final CodeListProxy proxy) {
        this.proxy = proxy;
    }
}
