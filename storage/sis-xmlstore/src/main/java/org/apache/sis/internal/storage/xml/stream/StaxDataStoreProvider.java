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
package org.apache.sis.internal.storage.xml.stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.internal.storage.xml.AbstractProvider;


/**
 * The provider of {@link StaxStreamReader} instances.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public abstract class StaxDataStoreProvider extends AbstractProvider {
    /**
     * Pool of JAXB marshallers shared by all data stores created by this provider.
     * This pool is created only when first needed; it will never be instantiated
     * if the data stores do not use JAXB.
     */
    private volatile MarshallerPool jaxb;

    /**
     * Creates a new provider. Subclasses shall populate the {@link #mimeForNameSpaces}
     * map with a mapping from their namespace to the MIME type to declare.
     *
     * @param  name  the primary key to use for searching in the {@code MD_Format} table, or {@code null} if none.
     */
    protected StaxDataStoreProvider(final String name) {
        super(name);
    }

    /**
     * Returns the JAXB context for the data store, or {@code null} if the data stores
     * {@linkplain #open created} by this provided do not use JAXB.
     *
     * <p>The default implementation returns {@code null}.</p>
     *
     * @return the JAXB context, or {@code null} if none.
     * @throws JAXBException if an error occurred while creating the JAXB context.
     */
    protected JAXBContext getJAXBContext() throws JAXBException {
        return null;
    }

    /**
     * Returns the (un)marshaller pool, creating it when first needed.
     * If the subclass does not define a JAXB context, then this method returns {@code null}.
     */
    final MarshallerPool getMarshallerPool() throws JAXBException {
        MarshallerPool pool = jaxb;
        if (pool == null) {
            synchronized (this) {
                pool = jaxb;
                if (pool == null) {
                    final JAXBContext context = getJAXBContext();
                    if (context != null) {
                        jaxb = pool = new MarshallerPool(context, null);
                    }
                }
            }
        }
        return pool;
    }
}
