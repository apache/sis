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
package org.apache.sis.storage.xml.stream;

import java.util.Objects;
import java.io.Closeable;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.util.resources.Errors;


/**
 * Common base class for {@code StaxStreamReader} and {@code StaxStreamWriter}.
 * {@code StaxStreamIO} subclasses are not used directly (they are Apache SIS internal mechanic);
 * they are rather used as helper classes for {@link org.apache.sis.storage.DataStore} implementations.
 * Those {@code DataStore}s will typically manage {@code StaxStreamReader} and {@code StaxStreamWriter}
 * instances on which they delegate their read and write operations.
 *
 * <h2>Multi-threading</h2>
 * This class and subclasses are not tread-safe. Synchronization shall be done by the {@code DataStore}
 * that contains the {@code StaxStreamIO} instances.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class StaxStreamIO implements AutoCloseable {
    /**
     * The data store for which this reader or writer has been created.
     */
    protected final StaxDataStore owner;

    /**
     * The underlying stream to close when this {@code StaxStreamIO} reader or writer is closed,
     * or {@code null} if none. This may be the same reference as {@link StaxDataStore#stream},
     * but not necessarily if we had to create a new stream for reading the data one more time.
     */
    Closeable stream;

    /**
     * The (un)marshaller pool, fetched when first needed. The same pool is shared by all {@code StaxStreamIO}
     * instances created by the same {@link StaxDataStoreProvider}, but we nevertheless store a reference here
     * in order to reduce the number of synchronizations done every time we need a (un)marshaller.
     */
    private MarshallerPool jaxb;

    /**
     * For sub-classes constructors.
     *
     * @param owner  the data store for which this reader or writer is created.
     */
    StaxStreamIO(final StaxDataStore owner) {
        this.owner = Objects.requireNonNull(owner);
    }

    /**
     * Returns the shared marshaller pool.
     */
    final MarshallerPool getMarshallerPool() throws JAXBException {
        if (jaxb == null) {
            final StaxDataStoreProvider provider = owner.getProvider();
            if (provider != null) {
                jaxb = provider.getMarshallerPool();
            }
            if (jaxb == null) {
                throw new JAXBException(errors().getString(Errors.Keys.MissingJAXBContext));
            }
        }
        return jaxb;
    }

    /**
     * Closes the input or output stream and releases any resources used by this XML reader or writer.
     * This reader or writer cannot be used anymore after this method has been invoked.
     *
     * @throws JAXBException if an error occurred while releasing JAXB resources.
     * @throws XMLStreamException if an error occurred while releasing XML reader/writer resources.
     * @throws IOException if an error occurred while closing the input or output stream.
     */
    @Override
    public void close() throws JAXBException, XMLStreamException, IOException {
        final Closeable s = stream;
        stream = null;
        if (s != null && owner.canClose(s)) {
            s.close();
        }
    }

    /**
     * Returns the error resources in the current locale.
     */
    protected final Errors errors() {
        return Errors.forLocale(owner.getLocale());
    }
}
