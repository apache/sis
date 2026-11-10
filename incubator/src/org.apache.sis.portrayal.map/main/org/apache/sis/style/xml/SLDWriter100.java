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
package org.apache.sis.style.xml;

import java.io.OutputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


/**
 * Writer of Styled Layer Descriptor 1.0 documents.
 * In this version, the symbology elements are in the SLD namespace, the filters
 * are encoded with Filter Encoding 1.0 and the geometries are encoded in GML 2.
 * The symbology elements are written by a {@link SEWriter100}.
 * Elements which exist only in version 1.1 are not written, and a warning is emitted.
 * This writer is used by the {@link StyleWriter} facade.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class SLDWriter100 extends SLDWriter {
    /**
     * The writer of the symbology elements of Styled Layer Descriptor 1.0.
     */
    private final SEWriter100 seWriter;

    /**
     * Creates a new writer using the given StAX cursor.
     * The XML document header is written immediately by this constructor.
     *
     * @param  writer  the StAX cursor to write to.
     * @throws XMLStreamException if the XML document header cannot be written.
     */
    SLDWriter100(final XMLStreamWriter writer) throws XMLStreamException {
        super(writer);
        seWriter = new SEWriter100(this);
    }

    /**
     * Creates a new writer for the given output stream.
     *
     * @param  out  the stream to write to.
     * @throws XMLStreamException if the StAX cursor cannot be created.
     */
    SLDWriter100(final OutputStream out) throws XMLStreamException {
        this(XMLOutputFactory.newInstance().createXMLStreamWriter(out, "UTF-8"));
    }

    /**
     * Returns the writer of the symbology elements of Styled Layer Descriptor 1.0.
     */
    @Override
    SEWriter100 seWriter() {
        return seWriter;
    }

    /**
     * Returns the version written by this writer, which is SLD 1.0.0.
     */
    @Override
    StyleVersion getVersion() {
        return StyleVersion.V1_0_0;
    }
}
