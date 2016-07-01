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
 * @version 0.8
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

    /**
     * Creates a new reader for the given object. The given object should be one of the types
     * expected by a {@code createXMLStreamReader(…)} method defined in this class.
     *
     * <p>Note that this method does <strong>not</strong> open streams from files, paths or URLs.
     * Creating input streams and closing them after usage are caller's responsibility.</p>
     *
     * @param  in where to read from.
     * @return the reader, or {@code null} if the given file where not recognized.
     * @throws XMLStreamException if the type of the given input is one recognized types,
     *         but despite that the reader can not be created.
     *
     * @since 0.8
     */
    public static XMLStreamReader createFromAny(final Object in) throws XMLStreamException {
        if (in instanceof XMLStreamReader) return                      ((XMLStreamReader) in);
        if (in instanceof XMLEventReader)  return createXMLStreamReader((XMLEventReader)  in);
        if (in instanceof InputSource)     return createXMLStreamReader((InputSource)     in);
        if (in instanceof InputStream)     return createXMLStreamReader((InputStream)     in);
        if (in instanceof Reader)          return createXMLStreamReader((Reader)          in);
        if (in instanceof Source)          return createXMLStreamReader((Source)          in);
        if (in instanceof Node)            return createXMLStreamReader((Node)            in);
        return null;
    }

    /*
     * Do not provide convenience method for java.io.File, because the caller needs to close the created
     * input stream himself (this is not done by XMLInputFactory.close(), despite its method name).
     */

    /**
     * Creates a new reader for the given stream.
     *
     * @param  in where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader can not be created.
     */
    public static XMLStreamReader createXMLStreamReader(final InputStream in) throws XMLStreamException {
        return FACTORY.createXMLStreamReader(in);
    }

    /**
     * Creates a new reader for the given stream.
     *
     * @param  in where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader can not be created.
     */
    public static XMLStreamReader createXMLStreamReader(final Reader in) throws XMLStreamException {
        return FACTORY.createXMLStreamReader(in);
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader can not be created.
     */
    public static XMLStreamReader createXMLStreamReader(final InputSource in) throws XMLStreamException {
        return FACTORY.createXMLStreamReader(new SAXSource(in));
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader can not be created.
     */
    public static XMLStreamReader createXMLStreamReader(final XMLEventReader in) throws XMLStreamException {
        return FACTORY.createXMLStreamReader(new StAXSource(in));
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader can not be created.
     */
    public static XMLStreamReader createXMLStreamReader(final Node in) throws XMLStreamException {
        return FACTORY.createXMLStreamReader(new DOMSource(in));
    }

    /**
     * Creates a new reader for the given source.
     *
     * @param  in where to read from.
     * @return the reader.
     * @throws XMLStreamException if the reader can not be created.
     */
    public static XMLStreamReader createXMLStreamReader(final Source in) throws XMLStreamException {
        return FACTORY.createXMLStreamReader(in);
    }
}
