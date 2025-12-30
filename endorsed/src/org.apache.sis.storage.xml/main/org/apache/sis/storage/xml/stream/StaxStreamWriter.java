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

import java.util.Map;
import java.util.Date;
import java.util.function.Consumer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Errors;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;


/**
 * Base class of Apache SIS writers of XML files using STAX writer.
 * This class is itself a consumer of {@code Feature} instances to write in the XML file.
 *
 * <p>This is a helper class for {@link org.apache.sis.storage.DataStore} implementations.
 * Writers for a given specification should extend this class and implement methods as in
 * the following example:</p>
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 *     public class UserObjectWriter extends StaxStreamWriter {
 *         UserObjectWriter(StaxDataStore owner, Metadata metadata) throws ... {
 *             super(owner);
 *         }
 *
 *         @Override
 *         public void writeStartDocument() throws Exception {
 *             super.writeStartDocument();
 *             // Write header (typically metadata) here.
 *         }
 *
 *         @Override
 *         public void write(Feature f) throws Exception {
 *             // Actual STAX write operations.
 *             writer.writeStartElement(…);
 *         }
 *     }
 *     }
 *
 * Writers can be used like below:
 *
 * {@snippet lang="java" :
 *     try (UserObjectWriter writer = new UserObjectWriter(dataStore, metadata)) {
 *         writer.writeStartDocument();
 *         writer.write(feature1);
 *         writer.write(feature2);
 *         writer.write(feature3);
 *         // etc.
 *         writer.writeEndDocument();
 *     }
 *     }
 *
 * <h2>Multi-threading</h2>
 * This class and subclasses are not tread-safe. Synchronization shall be done by the {@code DataStore}
 * that contains the {@code StaxStreamIO} instances.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class StaxStreamWriter extends StaxStreamIO implements Consumer<AbstractFeature> {
    /**
     * The XML stream writer.
     */
    protected final XMLStreamWriter writer;

    /**
     * The marshaller reserved to this writer usage,
     * created only when first needed and kept until this writer is closed.
     *
     * @see #marshal(String, String, Class, Object)
     */
    private Marshaller marshaller;

    /**
     * Creates a new XML writer for the given data store.
     *
     * @param  owner      the data store for which this writer is created.
     * @param  temporary  the temporary stream where to write, or {@code null} for the main storage.
     * @throws DataStoreException if the output type is not recognized or the data store is closed.
     * @throws XMLStreamException if an error occurred while opening the XML file.
     * @throws IOException if an error occurred while preparing the output stream.
     */
    protected StaxStreamWriter(final StaxDataStore owner, final OutputStream temporary)
            throws DataStoreException, XMLStreamException, IOException
    {
        super(owner);
        writer = owner.createWriter(this, temporary);      // Okay because will not store the `this` reference.
    }

    /**
     * Writes the XML declaration with the data store encoding and default XML version (1.0).
     * The encoding is specified as an option of the {@link org.apache.sis.storage.StorageConnector}
     * given at {@link StaxDataStore} construction time.
     *
     * <p>Subclasses should overwrite this method if they need to write metadata in the XML document before
     * the features. The overwritten method shall begin by a call to {@code super.writeStartDocument()}.
     * Example:</p>
     *
     * {@snippet lang="java" :
     *     @Override
     *     public void writeStartDocument() throws Exception {
     *         super.writeStartDocument();
     *         writer.setDefaultNamespace(namespace);
     *         writer.writeStartElement(rootElement);
     *         writer.writeDefaultNamespace(namespace);
     *     }
     * }
     *
     * @throws Exception if an error occurred while writing to the XML file.
     *         Possible subtypes include {@link XMLStreamException},
     *         but also {@link JAXBException} if JAXB is used for marshalling metadata objects,
     *         {@link DataStoreException}, {@link ClassCastException}, <i>etc.</i>
     */
    public void writeStartDocument() throws Exception {
        final Charset encoding = owner.getEncoding();
        if (encoding != null) {
            writer.writeStartDocument(encoding.name(), null);
        } else {
            writer.writeStartDocument();
        }
    }

    /**
     * Closes any start tags and writes corresponding end tags.
     * Subclasses should overwrite this method if they need to write some elements before the end tags.
     *
     * @throws Exception if an error occurred while writing to the XML file.
     */
    public void writeEndDocument() throws Exception {
        writer.writeEndDocument();
    }

    /**
     * Writes the given features to the XML document.
     *
     * @param  feature  the feature to write.
     * @throws Exception if an error occurred while writing to the XML file.
     *         Possible subtypes include {@link XMLStreamException},
     *         but also {@link JAXBException} if JAXB is used for marshalling metadata objects,
     *         {@link DataStoreException}, {@link ClassCastException}, <i>etc.</i>
     */
    public abstract void write(AbstractFeature feature) throws Exception;

    /**
     * Delegates to {@code write(Feature)}, wrapping {@code Exception} into unchecked {@code BackingStoreException}.
     *
     * @param  feature  the feature to write.
     * @throws BackingStoreException if an error occurred while writing to the XML file.
     */
    @Override
    public void accept(final AbstractFeature feature) throws BackingStoreException {
        try {
            write(feature);
        } catch (BackingStoreException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof UncheckedIOException) {
                e = ((UncheckedIOException) e).getCause();
            }
            throw new BackingStoreException(errors().getString(Errors.Keys.CanNotWriteFile_2,
                    owner.getFormatName(), owner.getDisplayName()), e);
        }
    }




    //  ╔════════════════════════════════════════════════════════════════════════════════╗
    //  ║                                                                                ║
    //  ║                Convenience methods for subclass implementations                ║
    //  ║                                                                                ║
    //  ╚════════════════════════════════════════════════════════════════════════════════╝

    /**
     * Writes a new element with the given value and no attribute.
     * If the given value is null, then this method does nothing.
     *
     * @param  localName  local name of the tag to write.
     * @param  value      text to write inside the element.
     * @throws XMLStreamException if the underlying STAX writer raised an error.
     */
    protected final void writeSingleValue(final String localName, final Object value) throws XMLStreamException {
        if (value != null) {
            writer.writeStartElement(localName);
            writer.writeCharacters(value.toString());
            writer.writeEndElement();
        }
    }

    /**
     * Writes a new element with the given date and no attribute.
     * If the given date is null, then this method does nothing.
     *
     * @param  localName  local name of the tag to write.
     * @param  value      date to write inside the element.
     * @throws XMLStreamException if the underlying STAX writer raised an error.
     */
    protected final void writeSingle(final String localName, final Date value) throws XMLStreamException {
        if (value != null) {
            writeSingleValue(localName, value.toInstant());
        }
    }

    /**
     * Writes the given list of values, ignoring null values.
     * If the given list is null, then this method does nothing.
     *
     * @param  localName  local name of the tag to write.
     * @param  values     values to write inside the element.
     * @throws XMLStreamException if the underlying STAX writer raised an error.
     */
    protected final void writeList(final String localName, final Iterable<?> values) throws XMLStreamException {
        if (values != null) {
            final StringBuilder buffer = new StringBuilder();
            for (final Object value : values) {
                if (value != null) {
                    final int length = buffer.length();
                    if (length != 0 && buffer.charAt(length - 1) != ' ') {
                        buffer.append(' ');
                    }
                    buffer.append(value);
                }
            }
            writeSingleValue(localName, buffer.toString());
        }
    }

    /**
     * Delegates to JAXB the marshalling of a part of XML document.
     * The XML content will be written in an element of the given name with no namespace (see below).
     *
     * <h4>Hiding namespace</h4>
     * The {@code hideNS} argument, if non-null, gives a namespace to remove in the marshalling result.
     * There are two reasons why we may want to hide a namespace. The most straightforward reason is to
     * simplify the XML document when the {@linkplain jakarta.xml.bind.annotation.XmlElement#namespace()
     * namespace of elements} to marshal is the {@linkplain XMLStreamWriter#setDefaultNamespace(String)
     * default namespace}. Since some JAXB implementation systematically inserts a prefix no matter if
     * the namespace is the default one or not, we have to manually erase the namespace when it is the
     * default one.
     *
     * <p>But a more convolved reason is to reuse an element defined for another version of the file format.
     * For example, some elements may be identical in 1.0 and 1.1 versions of a file format, so we may want
     * to define only one JAXB annotated class for both versions. In that case the {@code hideNS} argument is
     * <strong>not</strong> necessarily the {@linkplain XMLStreamWriter#setDefaultNamespace(String) default namespace}.
     * It is rather the namespace of the JAXB element that we want to erase (for example {@code "foo/1.1"}),
     * in order to pretend that it is the element of a different version specified by the default namespace
     * (for example defined by {@code xmlns = "foo/1.0"}).</p>
     *
     * @param  <T>     compile-time value of the {@code type} argument.
     * @param  hideNS  the namespace to erase from the marshalling output, or {@code null} if none.
     * @param  name    the XML tag to write.
     * @param  type    the Java class that define the XML schema of the object to marshal.
     * @param  object  the object to marshal, or {@code null} if none.
     * @throws XMLStreamException if the XML stream is closed.
     * @throws JAXBException if an error occurred during marshalling.
     *
     * @see jakarta.xml.bind.Marshaller#marshal(Object, XMLStreamWriter)
     */
    protected final <T> void marshal(final String hideNS, final String name, final Class<T> type, final T object)
            throws XMLStreamException, JAXBException
    {
        Marshaller m = marshaller;
        if (m == null) {
            m = getMarshallerPool().acquireMarshaller();
            m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);     // Formatting will be done by FormattedWriter.
            final Charset encoding = owner.getEncoding();
            if (encoding != null) {
                m.setProperty(Marshaller.JAXB_ENCODING, encoding.name());
            }
            for (final Map.Entry<String,?> entry : ((Map<String,?>) owner.configuration).entrySet()) {
                m.setProperty(entry.getKey(), entry.getValue());
            }
        }
        final QName qn;
        XMLStreamWriter out = writer;
        if (hideNS != null) {
            out = new NamespaceEraser(out, hideNS);
            qn  = new QName(hideNS, name);
        } else {
            qn  = new QName(name);
        }
        marshaller = null;
        m.marshal(new JAXBElement<>(qn, type, object), out);
        marshaller = m;                                                   // Allow reuse or recycling only on success.
    }

    /**
     * Closes the output stream and releases any resources used by this XML writer.
     * This writer cannot be used anymore after this method has been invoked.
     *
     * @throws JAXBException if an error occurred while releasing JAXB resources.
     * @throws XMLStreamException if an error occurred while releasing XML writer resources.
     * @throws IOException if an error occurred while closing the output stream.
     */
    @Override
    public void close() throws JAXBException, XMLStreamException, IOException {
        final Marshaller m = marshaller;
        if (m != null) {
            marshaller = null;
            getMarshallerPool().recycle(m);
        }
        writer.close();                         // Implies a call to stream.flush().
        IOUtilities.truncate(stream);
        super.close();
    }
}
