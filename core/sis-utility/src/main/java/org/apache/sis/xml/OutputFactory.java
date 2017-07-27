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
import org.apache.sis.util.Static;


/**
 * Provides access to {@link XMLOutputFactory} methods as static methods working on a SIS-wide instance.
 * This convenience is provided in a separated class in order to allow the JVM to instantiate the factory
 * only when first needed, when initializing this class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.4
 * @module
 */
final class OutputFactory extends Static {
    /**
     * The SIS-wide factory.
     */
    private static final XMLOutputFactory FACTORY = XMLOutputFactory.newInstance();

    /**
     * Do not allow instantiation of this class.
     */
    private OutputFactory() {
    }

    /*
     * Do not provide convenience method for java.io.File, because the caller needs to close the created
     * output stream himself (this is not done by XMLStreamWriter.close(), despite its method name).
     */

    /**
     * Creates a new writer for the given stream.
     *
     * @param  out       where to write to.
     * @param  encoding  the document encoding (usually {@code "UTF-8"}).
     * @return the writer.
     * @throws XMLStreamException if the writer can not be created.
     */
    public static XMLStreamWriter createXMLStreamWriter(OutputStream out, String encoding) throws XMLStreamException {
        return FACTORY.createXMLStreamWriter(out, encoding);
    }

    /**
     * Creates a new writer for the given stream.
     *
     * @param  out  where to write to.
     * @return the writer.
     * @throws XMLStreamException if the writer can not be created.
     */
    public static XMLStreamWriter createXMLStreamWriter(final Writer out) throws XMLStreamException {
        return FACTORY.createXMLStreamWriter(out);
    }

    /**
     * Creates a new writer for the JAXP result.
     * Note that this method is identified as <em>optional</em> in JSE javadoc.
     *
     * @param  out  where to write to.
     * @return the writer.
     * @throws XMLStreamException if the writer can not be created.
     */
    public static XMLStreamWriter createXMLStreamWriter(final ContentHandler out) throws XMLStreamException {
        return FACTORY.createXMLStreamWriter(new SAXResult(out));
    }

    /**
     * Creates a new writer for the JAXP result.
     * Note that this method is identified as <em>optional</em> in JSE javadoc.
     *
     * @param  out  where to write to.
     * @return the writer.
     * @throws XMLStreamException if the writer can not be created.
     */
    public static XMLStreamWriter createXMLStreamWriter(final Node out) throws XMLStreamException {
        return FACTORY.createXMLStreamWriter(new DOMResult(out));
    }

    /**
     * Creates a new writer for the JAXP result.
     * Note that this method is identified as <em>optional</em> in JSE javadoc.
     *
     * @param  out  where to write to.
     * @return the writer.
     * @throws XMLStreamException if the writer can not be created.
     */
    public static XMLStreamWriter createXMLStreamWriter(final XMLEventWriter out) throws XMLStreamException {
        return FACTORY.createXMLStreamWriter(new StAXResult(out));
    }

    /**
     * Creates a new writer for the JAXP result.
     * Note that this method is identified as <em>optional</em> in JSE javadoc.
     *
     * @param  out  where to write to.
     * @return the writer.
     * @throws XMLStreamException if the writer can not be created.
     */
    public static XMLStreamWriter createXMLStreamWriter(final Result out) throws XMLStreamException {
        return FACTORY.createXMLStreamWriter(out);
    }
}
