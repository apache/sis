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
package org.apache.sis.internal.xml;

import java.util.Map;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.xml.MarshallerPool;


/**
 * Base class of Apache SIS writers of XML files using STAX writer.
 * This class is itself a consumer of {@code Feature} instances to write in the XML file.
 *
 * <p>This is a helper class for {@link org.apache.sis.storage.DataStore} implementations.
 * Writers for a given specification should extend this class and implement methods as
 * in the following example:</p>
 *
 * <p>Example:</p>
 * {@preformat java
 *     public class UserObjectWriter extends StaxStreamWriter {
 *         public void accept(Feature f) throws BackingStoreException {
 *             // Actual STAX write operations.
 *             writer.writeStartElement(…);
 *         }
 *     }
 * }
 *
 * Writers can be used like below:
 *
 * {@preformat java
 *     try (UserObjectWriter writer = new UserObjectWriter(dataStore)) {
 *         writer.accept(feature);
 *     }
 * }
 *
 * <div class="section">Multi-threading</div>
 * This class and subclasses are not tread-safe. Synchronization shall be done by the {@code DataStore}
 * that contains the {@code StaxStreamIO} instances.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public abstract class StaxStreamWriter extends StaxStreamIO {
    /**
     * The XML stream writer.
     */
    protected final XMLStreamWriter writer;

    /**
     * Creates a new XML writer for the given data store.
     *
     * @param  owner  the data store for which this writer is created.
     * @throws DataStoreException if the output type is not recognized or the data store is closed.
     * @throws XMLStreamException if an error occurred while opening the XML file.
     * @throws IOException if an error occurred while preparing the output stream.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    protected StaxStreamWriter(final StaxDataStore owner) throws DataStoreException, XMLStreamException, IOException {
        super(owner);
        writer = owner.createWriter(this);      // Okay because will not store the 'this' reference.
        Charset encoding = owner.encoding;
        if (encoding == null) {
            encoding = Charset.defaultCharset();
        }
        writer.writeStartDocument(encoding.name());
    }




    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                ////////
    ////////                Convenience methods for subclass implementations                ////////
    ////////                                                                                ////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Writes a new tag with the given value and no attribute.
     * If the given value is null, then this method does nothing.
     *
     * @param  localName  local name of the tag to write.
     * @param  value      text to write inside the tag.
     * @throws XMLStreamException if the underlying STAX writer raised an error.
     */
    protected final void writeSimpleTag(final String localName, final Object value) throws XMLStreamException {
        if (value != null) {
            writer.writeStartElement(localName);
            writer.writeCharacters(value.toString());
            writer.writeEndElement();
        }
    }

    /**
     * Delegates to JAXB the marshalling of a part of XML document.
     *
     * @param  object  the object to marshall, or {@code null} if none.
     * @throws XMLStreamException if the XML stream is closed.
     * @throws JAXBException if an error occurred during marshalling.
     *
     * @see javax.xml.bind.Marshaller#marshal(Object, XMLStreamWriter)
     */
    protected final void marshal(final Object object) throws XMLStreamException, JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        for (final Map.Entry<String,?> entry : ((Map<String,?>) owner.configuration).entrySet()) {
            marshaller.setProperty(entry.getKey(), entry.getValue());
        }
        marshaller.marshal(object, writer);
        pool.recycle(marshaller);
    }

    /**
     * Closes the output stream and releases any resources used by this XML writer.
     * This writer can not be used anymore after this method has been invoked.
     *
     * @throws XMLStreamException if an error occurred while releasing XML writer resources.
     * @throws IOException if an error occurred while closing the output stream.
     */
    @Override
    public void close() throws Exception {
        writer.writeEndDocument();
        writer.close();
        super.close();
    }
}
