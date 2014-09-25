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
import org.opengis.metadata.acquisition.Trigger;
import org.apache.sis.internal.jaxb.gmd.CodeListAdapter;
import org.apache.sis.internal.jaxb.gmd.CodeListProxy;
import org.apache.sis.xml.Namespaces;


/**
 * JAXB adapter for {@link Trigger}, in order to integrate the value in an element respecting
 * the ISO-19139 standard. See package documentation for more information about the handling
 * of {@code CodeList} in ISO-19139.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-3.02)
 * @version 0.3
 * @module
 */
public final class MI_TriggerCode extends CodeListAdapter<MI_TriggerCode, Trigger> {
    /**
     * Empty constructor for JAXB only.
     */
    public MI_TriggerCode() {
    }

    /**
     * Creates a new adapter for the given proxy.
     */
    private MI_TriggerCode(final CodeListProxy proxy) {
        super(proxy);
    }

    /**
     * {@inheritDoc}
     *
     * @return The wrapper for the code list value.
     */
    @Override
    protected MI_TriggerCode wrap(CodeListProxy proxy) {
        return new MI_TriggerCode(proxy);
    }

    /**
     * {@inheritDoc}
     *
     * @return The code list class.
     */
    @Override
    protected Class<Trigger> getCodeListClass() {
        return Trigger.class;
    }

    /**
     * Invoked by JAXB on marshalling.
     *
     * @return The value to be marshalled.
     */
    @Override
    @XmlElement(name = "MI_TriggerCode", namespace = Namespaces.GMI)
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
