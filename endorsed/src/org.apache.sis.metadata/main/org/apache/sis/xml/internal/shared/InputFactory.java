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
package org.apache.sis.xml.internal.shared;

import java.io.Reader;
import java.io.InputStream;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import org.apache.sis.system.Loggers;
import org.apache.sis.util.logging.Logging;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;


/**
 * Provides access to {@link XMLInputFactory} methods as static methods working on a SIS-wide instance.
 * This convenience is provided in a separated class in order to allow the JVM to instantiate the factory
 * only when first needed, when initializing this class.
 *
 * <h2>Security</h2>
 * Unless the user has configured the {@code javax.xml.accessExternalDTD} property to something else
 * than {@code "all"}, this class disallows external DTDs referenced by the {@code "file"} protocols.
 * Allowed protocols are <abbr>HTTP</abbr> and <abbr>HTTPS</abbr> (list may be expanded if needed).
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://openjdk.org/jeps/185">JEP 185: Restrict Fetching of External XML Resources</a>
 */
public final class InputFactory {
    /**
     * The SIS-wide factory. This factory can be specified by the user, for example using the
     * {@code javax.xml.stream.XMLInputFactory} system property or with {@code META-INF/services}.
     *
     * @see org.apache.sis.storage.xml.stream.StaxDataStore#inputFactory()
     */
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();
    static {
        try {
            if (FACTORY.isPropertySupported(XMLConstants.FEATURE_SECURE_PROCESSING)) {
                FACTORY.setProperty(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            }
            if ("all".equals(FACTORY.getProperty(XMLConstants.ACCESS_EXTERNAL_DTD))) {
                FACTORY.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            }
        } catch (IllegalArgumentException e) {
            /*
             * `ACCESS_EXTERNAL_DTD` is clearly documented as a mandatory property since JAXP 1.5 in Java 7.
             * But Jackson 2.19.1, despite being released 14 years after Java 7, still doesn't support this
             * property.
             */
            Logging.unexpectedException(Logger.getLogger(Loggers.XML), InputFactory.class, "createXMLEventReader", e);
        }
    }

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
     * @throws XMLStreamException if the reader cannot be created.
     */
    public static XMLEventReader createXMLEventReader(final InputStream in) throws XMLStreamException {
        return FACTORY.createXMLEventReader(in, "UTF-8");
    }

    /**
     * Creates a new reader for the given stream.
     * It is caller's responsibility to close the given reader after usage
     * (it will <strong>not</strong> be done by {@link XMLEventReader#close()}).
     *
     * @param  in  where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader cannot be created.
     */
    public static XMLEventReader createXMLEventReader(final Reader in) throws XMLStreamException {
        return FACTORY.createXMLEventReader(in);
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in  where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader cannot be created.
     */
    public static XMLEventReader createXMLEventReader(final InputSource in) throws XMLStreamException {
        return FACTORY.createXMLEventReader(new SAXSource(in));
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in  where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader cannot be created.
     */
    public static XMLEventReader createXMLEventReader(final Node in) throws XMLStreamException {
        return FACTORY.createXMLEventReader(new DOMSource(in));
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in  where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader cannot be created.
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
     * @throws XMLStreamException if the reader cannot be created.
     */
    public static XMLEventReader createXMLEventReader(final XMLStreamReader in) throws XMLStreamException {
        return FACTORY.createXMLEventReader(new StreamReaderDelegate(in) {
            @Override public void close() throws XMLStreamException {
                // Do not close the XMLStreamReader because user may continue reading from it.
            }
        });
    }
}
