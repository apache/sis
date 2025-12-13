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

import java.io.Writer;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.nio.charset.Charset;
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
 */
enum OutputType {
    /**
     * The output is already an instance of {@link XMLStreamWriter}.
     * That output is returned directly.
     */
    STAX(XMLStreamWriter.class, InputType.STAX) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) {
            return (XMLStreamWriter) s;
        }
    },

    /**
     * The output is an instance of Java I/O {@link OutputStream}.
     * Encoding depends on the {@linkplain StaxDataStore#encoding data store character encoding}.
     */
    STREAM(OutputStream.class, InputType.STREAM) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException {
            final XMLOutputFactory f = ds.outputFactory();
            final Charset encoding = ds.getEncoding();
            return (encoding != null) ? f.createXMLStreamWriter((OutputStream) s, encoding.name())
                                      : f.createXMLStreamWriter((OutputStream) s);
        }
        @Override Closeable snapshot(final Object s) {
            if (s instanceof ByteArrayOutputStream) {
                return new ByteArrayInputStream(((ByteArrayOutputStream) s).toByteArray());
            }
            return super.snapshot(s);
        }
    },

    /**
     * The output is an instance of Java I/O {@link Writer}.
     */
    CHARACTERS(Writer.class, InputType.CHARACTERS) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.outputFactory().createXMLStreamWriter((Writer) s);
        }
        @Override Closeable snapshot(final Object s) {
            if (s instanceof StringWriter) {
                return new StringReader(s.toString());
            }
            return super.snapshot(s);
        }
    },

    /**
     * The output is an instance of XML {@link Result}, which is itself a wrapper around another kind of result.
     */
    RESULT(Result.class, InputType.SOURCE) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.outputFactory().createXMLStreamWriter((Result) s);
        }
    },

    /**
     * The output is an instance of DOM {@link Node}.
     */
    NODE(Node.class, InputType.NODE) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.outputFactory().createXMLStreamWriter(new DOMResult((Node) s));
        }
    },

    /**
     * The output is an instance of SAX {@link ContentHandler}.
     */
    SAX(ContentHandler.class, InputType.SAX) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.outputFactory().createXMLStreamWriter(new SAXResult((ContentHandler) s));
        }
    },

    /**
     * The output is an instance of STAX {@link XMLEventWriter}.
     */
    EVENT(XMLEventWriter.class, InputType.EVENT) {
        @Override XMLStreamWriter create(StaxDataStore ds, Object s) throws XMLStreamException {
            return ds.outputFactory().createXMLStreamWriter(new StAXResult((XMLEventWriter) s));
        }
    };

    /**
     * The kind of output that this enumeration can handle.
     */
    private final Class<?> outputType;

    /**
     * The input type from the same framework (StAX, DOM, <i>etc</i>) than this output type.
     */
    final InputType inputType;

    /**
     * Creates a new enumeration for the given type of output.
     */
    private OutputType(final Class<?> outputType, final InputType inputType) {
        this.inputType  = inputType;
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
     * Returns a reader for the data written by the given writer, or {@code null} if we cannot read the data.
     * If non-null, the value returned by this method is a snapshot of the given stream content, i.e. changes
     * in the output stream will not affect the returned input stream or reader. In particular, contrarily to
     * {@link org.apache.sis.io.stream.IOUtilities#toInputStream(AutoCloseable)} this method does not
     * invalidate the output stream.
     *
     * <p>The returned input can be used by {@link #inputType}.</p>
     *
     * @param  s  the output from which to get the data that we wrote.
     * @return the input for the data written by the given stream, or {@code null} if none.
     */
    Closeable snapshot(final Object s) {
        return null;
    }

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
