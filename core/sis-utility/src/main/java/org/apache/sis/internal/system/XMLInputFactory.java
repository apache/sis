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
package org.apache.sis.internal.system;

import java.io.Reader;
import java.io.InputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.apache.sis.util.Static;


/**
 * Provides access to {@link javax.xml.stream.XMLInputFactory} methods as static methods working on
 * a SIS-wide instance. This convenience is provided in a separated class in order to allow the JVM
 * to instantiate the factory only when first needed, when initializing this class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class XMLInputFactory extends Static {
    /**
     * The SIS-wide factory.
     */
    private static final javax.xml.stream.XMLInputFactory FACTORY = javax.xml.stream.XMLInputFactory.newInstance();

    /**
     * Do not allow instantiation of this class.
     */
    private XMLInputFactory() {
    }

    /*
     * Do not provide convenience method for java.io.File, because the caller needs to close the created
     * input stream himself (this is not done by XMLInputFactory.close(), despite its method name).
     */

    /**
     * Creates a new reader for the given stream.
     *
     * @param  in Where to read from.
     * @return The reader.
     * @throws XMLStreamException If the reader can not be created.
     */
    public static XMLStreamReader createXMLStreamReader(final InputStream in) throws XMLStreamException {
        return FACTORY.createXMLStreamReader(in);
    }

    /**
     * Creates a new reader for the given stream.
     *
     * @param  in Where to read from.
     * @return The reader.
     * @throws XMLStreamException If the reader can not be created.
     */
    public static XMLStreamReader createXMLStreamReader(final Reader in) throws XMLStreamException {
        return FACTORY.createXMLStreamReader(in);
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in Where to read from.
     * @return The reader.
     * @throws XMLStreamException If the reader can not be created.
     */
    public static XMLStreamReader createXMLStreamReader(final InputSource in) throws XMLStreamException {
        return FACTORY.createXMLStreamReader(new SAXSource(in));
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in Where to read from.
     * @return The reader.
     * @throws XMLStreamException If the reader can not be created.
     */
    public static XMLStreamReader createXMLStreamReader(final XMLEventReader in) throws XMLStreamException {
        return FACTORY.createXMLStreamReader(new StAXSource(in));
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in Where to read from.
     * @return The reader.
     * @throws XMLStreamException If the reader can not be created.
     */
    public static XMLStreamReader createXMLStreamReader(final Node in) throws XMLStreamException {
        return FACTORY.createXMLStreamReader(new DOMSource(in));
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in Where to read from.
     * @return The reader.
     * @throws XMLStreamException If the reader can not be created.
     */
    public static XMLStreamReader createXMLStreamReader(final Source in) throws XMLStreamException {
        return FACTORY.createXMLStreamReader(in);
    }
}
