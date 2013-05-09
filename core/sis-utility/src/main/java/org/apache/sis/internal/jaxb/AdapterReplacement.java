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
package org.apache.sis.internal.jaxb;

import java.util.ServiceLoader;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.XmlAdapter;


/**
 * An interface for {@link XmlAdapter} to be used in replacement of the instance created by JAXB.
 * This interface provides a way to replace <cite>default</cite> adapters by <cite>configured</cite>
 * ones. It does not allow the addition of new adapters (i.e. it can not be used in replacement of
 * the {@link javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter} annotation).
 *
 * <p>This interface is mostly for handling extensions to metadata profile provided as extension,
 * like the {@code FRA} extension for France provided in the {@code sis-metadata-fra} module.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 *
 * @see Marshaller#setAdapter(XmlAdapter)
 * @see Unmarshaller#setAdapter(XmlAdapter)
 */
public interface AdapterReplacement {
    /**
     * The system-wide provider of {@code AdapterReplacement} instances.
     * <strong>Every usage of this service loader must be synchronized.</strong>
     * This loader is public for allowing modules to unregister their instance
     * when the module is unloaded (e.g. from an OSGi container), as below:
     *
     * {@preformat java
     *     synchronized (AdapterReplacement.PROVIDER) {
     *         AdapterReplacement.PROVIDER.reload();
     *     }
     * }
     */
    ServiceLoader<AdapterReplacement> PROVIDER = ServiceLoader.load(AdapterReplacement.class);

    /**
     * Invoked when a new adapter is created by {@link org.apache.sis.xml.MarshallerPool}.
     * Typical implementations will be as below:
     *
     * {@preformat java
     *     marshaller.setAdapter(MyParent.class, this);
     * }
     *
     * @param  marshaller The marshaller to be configured.
     * @throws JAXBException If the given marshaller can not be configured.
     */
    void register(Marshaller marshaller) throws JAXBException;

    /**
     * Invoked when a new adapter is created by {@link org.apache.sis.xml.MarshallerPool}.
     * Typical implementations will be as below:
     *
     * {@preformat java
     *     unmarshaller.setAdapter(MyParent.class, this);
     * }
     *
     * @param  unmarshaller The unmarshaller to be configured.
     * @throws JAXBException If the given unmarshaller can not be configured.
     */
    void register(Unmarshaller unmarshaller) throws JAXBException;
}
