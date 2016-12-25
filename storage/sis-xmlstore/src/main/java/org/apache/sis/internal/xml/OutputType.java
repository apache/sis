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

import java.io.Writer;
import java.io.OutputStream;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stax.StAXResult;
import org.xml.sax.ContentHandler;
import org.w3c.dom.Node;


/**
 * Creates {@link XMLStreamWriter} from a given output. This enumeration allows to analyze the output type
 * only once before to create as many instances of {@link XMLStreamWriter} as needed for that output.
 * The enumeration order is the preference order (i.e. we will test if the object already implements the
 * {@link XMLStreamWriter} interface before to test for {@link OutputStream}, {@link Writer}, <i>etc.</i>).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
enum OutputType {
    /**
     * The output is already an instance of {@link XMLStreamWriter}.
     * That output is returned directly.
     */
    STAX(XMLStreamWriter.class) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) {
            return (XMLStreamWriter) s;
        }
    },

    /**
     * The output is an instance of Java I/O {@link OutputStream}.
     * Encoding depends on the {@linkplain StaxDataStore#encoding data store character encoding}.
     */
    STREAM(OutputStream.class) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException {
            final XMLOutputFactory f = ds.outputFactory();
            return (ds.encoding != null) ? f.createXMLStreamWriter((OutputStream) s, ds.encoding.name())
                                         : f.createXMLStreamWriter((OutputStream) s);
        }
    },

    /**
     * The output is an instance of Java I/O {@link Writer}.
     */
    CHARACTERS(Writer.class) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.outputFactory().createXMLStreamWriter((Writer) s);
        }
    },

    /**
     * The output is an instance of XML {@link Result}, which is itself a wrapper around another kind of result.
     */
    SOURCE(Result.class) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.outputFactory().createXMLStreamWriter((Result) s);
        }
    },

    /**
     * The output is an instance of DOM {@link Node}.
     */
    NODE(Node.class) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.outputFactory().createXMLStreamWriter(new DOMResult((Node) s));
        }
    },

    /**
     * The output is an instance of SAX {@link ContentHandler}.
     */
    SAX(ContentHandler.class) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.outputFactory().createXMLStreamWriter(new SAXResult((ContentHandler) s));
        }
    },

    /**
     * The output is an instance of STAX {@link XMLEventWriter}.
     */
    EVENT(XMLEventWriter.class) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.outputFactory().createXMLStreamWriter(new StAXResult((XMLEventWriter) s));
        }
    };

    /**
     * The kind of output that this enumeration can handle.
     */
    private final Class<?> outputType;

    /**
     * Creates a new enumeration for the given type of output.
     */
    private OutputType(final Class<?> outputType) {
        this.outputType = outputType;
    }

    /**
     * Creates a XML writer for the given output.
     *
     * @param  ds  the data store for which to create writer instances.
     * @param  s   the output stream or the storage object (URL, <i>etc</i>).
     * @return the XML writer.
     * @throws XMLStreamException if the XML writer creation failed.
     */
    abstract XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException;

    /**
     * Returns a {@code WriterFactory} for the given output type. The {@code type} argument given to this method
     * shall be the class of the {@code s} argument to be given in {@link #create(StaxDataStore, Object)} calls.
     *
     * @param  type  the type of the output stream or storage object (URL, <i>etc</i>).
     * @return a factory for the given stream or storage type, or {@code null} if the given type is not recognized.
     */
    static OutputType forType(final Class<?> type) {
        for (final OutputType c : values()) {
            if (c.outputType.isAssignableFrom(type)) {
                return c;
            }
        }
        return null;
    }
}
