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
import java.util.HashMap;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stax.StAXResult;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.xml.Namespaces;


/**
 * Base class of Apache SIS writers of XML files using STAX writer.
 * This is a helper class for {@link org.apache.sis.storage.DataStore} implementations.
 * Writers for a given specification should extend this class and provide appropriate write methods.
 *
 * <p>Example:</p>
 * {@preformat java
 *     public class UserWriter extends StaxStreamWriter {
 *         public void write(User user) throws XMLStreamException {
 *             // Actual STAX write operations.
 *             writer.writeStartElement(…);
 *         }
 *     }
 * }
 *
 * And should be used like below:
 *
 * {@preformat java
 *     try (UserWriter instance = new UserWriter()) {
 *         instance.setOutput(stream);
 *         instance.write(aUser);
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
    private XMLStreamWriter writer;

    /**
     * The namespaces for which no prefix are mapped. In such case, default prefixes will be created.
     * Keys are namespaces URL and values are prefixes.
     */
    private final Map<String,String> unknownNamespaces = new HashMap<>();

    /**
     * Creates a new XML writer from the given file, URL, stream or writer object.
     * The {@code output} argument shall be an instance of {@link XMLStreamWriter}, {@link XMLEventWriter},
     * {@link ContentHandler}, {@link OutputStream}, {@link Writer}, {@link Result}, {@link Node} or an
     * object convertible to {@link Path}, otherwise a {@link DataStoreException} will be thrown.
     *
     * @param  owner     the data store for which this writer is created.
     * @param  output    where to write the XML file.
     * @param  encoding  the document encoding (usually {@code "UTF-8"}). May be ignored.
     * @throws DataStoreException if the output type is not recognized.
     * @throws IOException if an error occurred while creating the file.
     * @throws XMLStreamException if an error occurred while opening the XML file.
     */
    protected StaxStreamWriter(final StaxDataStore owner, final Object output, final String encoding)
            throws DataStoreException, IOException, XMLStreamException
    {
        super(owner);
        ArgumentChecks.ensureNonNull("output", output);
        if      (output instanceof XMLStreamWriter) writer = (XMLStreamWriter) output;
        else if (output instanceof XMLEventWriter)  writer = factory().createXMLStreamWriter(new StAXResult((XMLEventWriter) output));
        else if (output instanceof ContentHandler)  writer = factory().createXMLStreamWriter(new SAXResult((ContentHandler) output));
        else if (output instanceof OutputStream)    writer = factory().createXMLStreamWriter((OutputStream) output, encoding);
        else if (output instanceof Writer)          writer = factory().createXMLStreamWriter((Writer) output);
        else if (output instanceof Result)          writer = factory().createXMLStreamWriter((Result) output);
        else if (output instanceof Node)            writer = factory().createXMLStreamWriter(new DOMResult((Node) output));
        else try {
            final Path path = ObjectConverters.convert(output, Path.class);
            final BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(path));
            writer = factory().createXMLStreamWriter(out, encoding);
        } catch (UnconvertibleObjectException e) {
            throw new UnsupportedStorageException(errors().getString(Errors.Keys.IllegalOutputTypeForWriter_2,
                                                  owner.getFormatName(), Classes.getClass(output)), e);
        }
    }

    /**
     * Convenience method invoking {@link StaxDataStore#outputFactory()}.
     */
    private XMLOutputFactory factory() {
        return owner.outputFactory();
    }

    /**
     * Returns the XML stream writer if it is not closed.
     *
     * @return the XML stream writer (never null).
     * @throws XMLStreamException if this XML writer has been closed.
     */
    protected final XMLStreamWriter getWriter() throws XMLStreamException {
        if (writer != null) {
            return writer;
        }
        throw new XMLStreamException(errors().getString(Errors.Keys.ClosedWriter_1, "XML"));
    }

    /**
     * Writes a new tag with the given value and no attribute.
     * If the given value is null, then this method does nothing.
     *
     * @param  namespace  namespace (URL) of the tag to write.
     * @param  localName  local name of the tag to write.
     * @param  value      text to write inside the tag.
     * @throws XMLStreamException if the underlying STAX writer raised an error.
     */
    protected final void writeSimpleTag(final String namespace, final String localName, final Object value) throws XMLStreamException {
        if (value != null) {
            writer.writeStartElement(namespace, localName);
            writer.writeCharacters(value.toString());
            writer.writeEndElement();
        }
    }

    /**
     * Returns the prefix for the given namespace. If the given namespace is one of the
     * {@linkplain Namespaces well-known namespaces}, then this method returns the prefix
     * commonly used for that namespace. Otherwise a new prefix is generated.
     *
     * @param  namespace  the namespace (URL) for which to get the prefix.
     * @return prefix for the given namespace.
     */
    protected final String getPrefix(final String namespace) {
        String prefix = Namespaces.getPreferredPrefix(namespace, null);
        if (prefix == null) {
            prefix = unknownNamespaces.get(namespace);
            if (prefix == null) {
                prefix = "ns" + (unknownNamespaces.size() + 1);
                unknownNamespaces.put(namespace, prefix);
            }
        }
        return prefix;
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
        if (writer != null) {
            writer.close();
            writer = null;
        }
        super.close();
    }
}
