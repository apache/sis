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

import java.io.Reader;
import java.io.InputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.apache.sis.util.Static;


/**
 * Provides access to {@link XMLInputFactory} methods as static methods working on a SIS-wide instance.
 * This convenience is provided in a separated class in order to allow the JVM to instantiate the factory
 * only when first needed, when initializing this class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.4
 * @module
 */
final class InputFactory extends Static {
    /**
     * The SIS-wide factory. This factory can be specified by the user, for example using the
     * {@code javax.xml.stream.XMLInputFactory} system property.
     *
     * <div class="note"><b>Note:</b>
     * {@code XMLInputFactory} has an {@code newDefaultFactory()} method which bypass user settings.</div>
     */
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    /**
     * Do not allow instantiation of this class.
     */
    private InputFactory() {
    }

    /*
     * Do not provide convenience method for java.io.File, because the caller needs to close the created
     * input stream himself (this is not done by XMLEventReader.close(), despite its method name).
     */

    /**
     * Creates a new reader for the given stream.
     * It is caller's responsibility to close the given input stream after usage
     * (it will <strong>not</strong> be done by {@link XMLEventReader#close()}).
     *
     * @param  in  where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader can not be created.
     */
    public static XMLEventReader createXMLEventReader(final InputStream in) throws XMLStreamException {
        return FACTORY.createXMLEventReader(in);
    }

    /**
     * Creates a new reader for the given stream.
     * It is caller's responsibility to close the given reader after usage
     * (it will <strong>not</strong> be done by {@link XMLEventReader#close()}).
     *
     * @param  in  where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader can not be created.
     */
    public static XMLEventReader createXMLEventReader(final Reader in) throws XMLStreamException {
        return FACTORY.createXMLEventReader(in);
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in  where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader can not be created.
     */
    public static XMLEventReader createXMLEventReader(final InputSource in) throws XMLStreamException {
        return FACTORY.createXMLEventReader(new SAXSource(in));
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in  where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader can not be created.
     */
    public static XMLEventReader createXMLEventReader(final Node in) throws XMLStreamException {
        return FACTORY.createXMLEventReader(new DOMSource(in));
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in  where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader can not be created.
     */
    public static XMLEventReader createXMLEventReader(final Source in) throws XMLStreamException {
        return FACTORY.createXMLEventReader(in);
    }

    /**
     * Creates a new reader for the given source.
     * It is caller's responsibility to close the given stream reader after usage
     * (it will <strong>not</strong> be done by {@link XMLEventReader#close()}).
     *
     * @param  in  where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader can not be created.
     */
    public static XMLEventReader createXMLEventReader(final XMLStreamReader in) throws XMLStreamException {
        return FACTORY.createXMLEventReader(new StreamReaderDelegate(in) {
            @Override public void close() throws XMLStreamException {
                // Do not close the XMLStreamReader because user may continue reading from it.
            }
        });
    }
}
