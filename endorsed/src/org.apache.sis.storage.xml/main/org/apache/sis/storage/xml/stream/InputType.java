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

import java.io.Reader;
import java.io.InputStream;
import java.nio.charset.Charset;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import org.xml.sax.InputSource;
import org.w3c.dom.Node;


/**
 * Creates {@link XMLStreamReader} from a given input. This enumeration allows to analyze the input type
 * only once before to create as many instances of {@link XMLStreamReader} as needed for that input.
 * The enumeration order is the preference order (i.e. we will test if the object already implements the
 * {@link XMLStreamReader} interface before to test for {@link InputStream}, {@link Reader}, <i>etc.</i>).
 *
 * <p>Some kinds of inputs can be used many time (for example {@link Node}).
 * Other inputs can be used only once (for example {@link XMLEventReader}).
 * For some inputs, it depends on whether the stream support marks.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum InputType {
    /**
     * The input is already an instance of {@link XMLStreamReader}.
     * That input is returned directly and can be used only once.
     */
    STAX(XMLStreamReader.class) {
        @Override XMLStreamReader create(StaxDataStore ds, Object s) {
            return (XMLStreamReader) s;
        }
    },

    /**
     * The input is an instance of Java I/O {@link InputStream}.
     * Decoding may depend on the {@linkplain StaxDataStore#encoding data store character encoding}.
     */
    STREAM(InputStream.class) {
        @Override XMLStreamReader create(StaxDataStore ds, Object s) throws XMLStreamException {
            final XMLInputFactory f = ds.inputFactory();
            final Charset encoding = ds.getEncoding();
            return (encoding != null) ? f.createXMLStreamReader((InputStream) s, encoding.name())
                                      : f.createXMLStreamReader((InputStream) s);
        }
    },

    /**
     * The input is an instance of Java I/O {@link Reader}.
     */
    CHARACTERS(Reader.class) {
        @Override XMLStreamReader create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.inputFactory().createXMLStreamReader((Reader) s);
        }
    },

    /**
     * The input is an instance of XML {@link Source}, which is itself a wrapper around another kind of source.
     */
    SOURCE(Source.class) {
        @Override XMLStreamReader create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.inputFactory().createXMLStreamReader((Source) s);
        }
    },

    /**
     * The input is an instance of DOM {@link Node}.
     */
    NODE(Node.class) {
        @Override XMLStreamReader create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.inputFactory().createXMLStreamReader(new DOMSource((Node) s));
        }
    },

    /**
     * The input is an instance of SAX {@link InputSource}.
     */
    SAX(InputSource.class) {
        @Override XMLStreamReader create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.inputFactory().createXMLStreamReader(new SAXSource((InputSource) s));
        }
    },

    /**
     * The input is an instance of STAX {@link XMLEventReader}.
     */
    EVENT(XMLEventReader.class) {
        @Override XMLStreamReader create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.inputFactory().createXMLStreamReader(new StAXSource((XMLEventReader) s));
        }
    };

    /**
     * The kind of input that this enumeration can handle.
     */
    private final Class<?> inputType;

    /**
     * Creates a new enumeration for the given type of input.
     */
    private InputType(final Class<?> inputType) {
        this.inputType = inputType;
    }

    /**
     * Creates a XML reader for the given input.
     *
     * @param  ds  the data store for which to create reader instances.
     * @param  s   the input stream or the storage object (URL, <i>etc</i>).
     * @return the XML reader.
     * @throws XMLStreamException if the XML reader creation failed.
     */
    abstract XMLStreamReader create(StaxDataStore ds, Object s) throws XMLStreamException;

    /**
     * Returns a {@code ReaderFactory} for the given input type. The {@code type} argument given to this method
     * shall be the class of the {@code s} argument to be given in {@link #create(StaxDataStore, Object)} calls.
     *
     * @param  type  the type of the input stream or storage object (URL, <i>etc</i>).
     * @return a factory for the given stream or storage type, or {@code null} if the given type is not recognized.
     */
    static InputType forType(final Class<?> type) {
        for (final InputType c : values()) {
            if (c.inputType.isAssignableFrom(type)) {
                return c;
            }
        }
        return null;
    }
}
