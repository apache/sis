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
import org.opengis.metadata.identification.DistributedComputingPlatform;
import org.apache.sis.internal.jaxb.gmd.CodeListAdapter;
import org.apache.sis.internal.jaxb.gmd.CodeListProxy;
import org.apache.sis.xml.Namespaces;


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
public final class DCPList extends CodeListAdapter<DCPList, DistributedComputingPlatform> {
    /**
     * Empty constructor for JAXB only.
     */
    public DCPList() {
    }

    /**
     * Creates a new adapter for the given proxy.
     */
    private DCPList(final CodeListProxy proxy) {
        super(proxy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DCPList wrap(CodeListProxy proxy) {
        return new DCPList(proxy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<DistributedComputingPlatform> getCodeListClass() {
        return DistributedComputingPlatform.class;
    }

    /**
     * Invoked by JAXB on marshalling.
     *
     * @return The value to be marshalled.
     */
    @Override
    @XmlElement(name = "DCPList", namespace = Namespaces.SRV)
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
