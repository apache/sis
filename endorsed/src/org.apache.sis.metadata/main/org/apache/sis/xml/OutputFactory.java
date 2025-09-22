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
package org.apache.sis.xml;

import java.io.OutputStream;
import java.io.Writer;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stax.StAXResult;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.apache.sis.xml.privy.StreamWriterDelegate;


/**
 * Provides access to {@link XMLOutputFactory} methods as static methods working on a SIS-wide instance.
 * This convenience is provided in a separated class in order to allow the JVM to instantiate the factory
 * only when first needed, when initializing this class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class OutputFactory {
    /**
     * The SIS-wide factory. This factory can be specified by the user, for example using the
     * {@code javax.xml.stream.XMLOutputFactory} system property.
     *
     * <div class="note"><b>Note:</b>
     * {@code XMLOutputFactory} has an {@code newDefaultFactory()} method which bypass user settings.</div>
     */
    private static final XMLOutputFactory FACTORY = XMLOutputFactory.newInstance();

    /**
     * Do not allow instantiation of this class.
     */
    private OutputFactory() {
    }

    /*
     * Do not provide convenience method for java.io.File, because the caller needs to close the created
     * output stream himself (this is not done by XMLEventWriter.close(), despite its method name).
     */

    /**
     * Creates a new writer for the given stream.
     * It is caller's responsibility to close the given output stream after usage
     * (it will <strong>not</strong> be done by {@link XMLEventWriter#close()}).
     *
     * @param  out       where to write to.
     * @param  encoding  the document encoding (usually {@code "UTF-8"}).
     * @return the writer.
     * @throws XMLStreamException if the writer cannot be created.
     */
    public static XMLEventWriter createXMLEventWriter(OutputStream out, String encoding) throws XMLStreamException {
        return FACTORY.createXMLEventWriter(out, encoding);
    }

    /**
     * Creates a new writer for the given stream.
     * It is caller's responsibility to close the given writer after usage
     * (it will <strong>not</strong> be done by {@link XMLEventWriter#close()}).
     *
     * @param  out  where to write to.
     * @return the writer.
     * @throws XMLStreamException if the writer cannot be created.
     */
    public static XMLEventWriter createXMLEventWriter(final Writer out) throws XMLStreamException {
        return FACTORY.createXMLEventWriter(out);
    }

    /**
     * Creates a new writer for the JAXP result.
     * Note that this method is identified as <em>optional</em> in JSE javadoc.
     *
     * @param  out  where to write to.
     * @return the writer.
     * @throws XMLStreamException if the writer cannot be created.
     */
    public static XMLEventWriter createXMLEventWriter(final ContentHandler out) throws XMLStreamException {
        return FACTORY.createXMLEventWriter(new SAXResult(out));
    }

    /**
     * Creates a new writer for the JAXP result.
     * Note that this method is identified as <em>optional</em> in JSE javadoc.
     *
     * @param  out  where to write to.
     * @return the writer.
     * @throws XMLStreamException if the writer cannot be created.
     */
    public static XMLEventWriter createXMLEventWriter(final Node out) throws XMLStreamException {
        return FACTORY.createXMLEventWriter(new DOMResult(out));
    }

    /**
     * Creates a new writer for the JAXP result.
     * Note that this method is identified as <em>optional</em> in JSE javadoc.
     *
     * @param  out  where to write to.
     * @return the writer.
     * @throws XMLStreamException if the writer cannot be created.
     */
    public static XMLEventWriter createXMLEventWriter(final Result out) throws XMLStreamException {
        return FACTORY.createXMLEventWriter(out);
    }

    /**
     * Creates a new writer for the JAXP result.
     * Note that this method is identified as <em>optional</em> in JSE javadoc.
     * It is caller's responsibility to close the given stream writer after usage
     * (it will <strong>not</strong> be done by {@link XMLEventWriter#close()}).
     *
     * @param  out  where to write to.
     * @return the writer.
     * @throws XMLStreamException if the writer cannot be created.
     */
    public static XMLEventWriter createXMLEventWriter(final XMLStreamWriter out) throws XMLStreamException {
        return FACTORY.createXMLEventWriter(new StAXResult(new StreamWriterDelegate(out) {
            @Override public void close() throws XMLStreamException {
                /*
                 * Do not close the XMLStreamWriter because user may continue writing to it.
                 * Do not flush neither; the default XMLStreamWriterImpl does nothing more
                 * than forwarding to java.io.Writer.flush() and flushing an output stream
                 * have a performance impact. If the user really wants to flush, (s)he can
                 * invoke XMLStreamWriter.flush() himself.
                 */
            }
        }));
    }
}
