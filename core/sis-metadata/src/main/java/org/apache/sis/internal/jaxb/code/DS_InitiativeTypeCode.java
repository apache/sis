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

import org.apache.sis.internal.jaxb.gmd.CodeListAdapter;
import org.apache.sis.internal.jaxb.gmd.CodeListProxy;
import javax.xml.bind.annotation.XmlElement;
import org.opengis.metadata.identification.InitiativeType;


/**
 * JAXB adapter for {@link InitiativeType}, in order to integrate the value in a element
 * complying with ISO-19139 standard. See package documentation to have more information
 * about the handling of CodeList in ISO-19139.
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public final class DS_InitiativeTypeCode extends CodeListAdapter<DS_InitiativeTypeCode, InitiativeType> {
    /**
     * Ensures that the adapted code list class is loaded.
     */
    static {
        ensureClassLoaded(InitiativeType.class);
    }

    /**
     * Empty constructor for JAXB only.
     */
    public DS_InitiativeTypeCode() {
    }

    /**
     * Creates a new adapter for the given proxy.
     */
    private DS_InitiativeTypeCode(final CodeListProxy proxy) {
        super(proxy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DS_InitiativeTypeCode wrap(CodeListProxy proxy) {
        return new DS_InitiativeTypeCode(proxy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<InitiativeType> getCodeListClass() {
        return InitiativeType.class;
    }

    /**
     * Invoked by JAXB on marshalling.
     *
     * @return The value to be marshalled.
     */
    @Override
    @XmlElement(name = "DS_InitiativeTypeCode")
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
