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
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.sis.storage.DataStoreException;


/**
 * Writer of Styled Layer Descriptor and Symbology Encoding documents, using the StAX cursor API.
 * Unlike {@link StyleReader}, there is nothing to detect when writing: the caller states which
 * {@link StyleVersion} to target, and this class delegates to the writer for that version.
 *
 * <p>The object to write can be any of the objects returned by {@link StyleReader#read()}.
 * Elements or properties which cannot be represented in the target version are not written,
 * and are reported in the {@linkplain #getWarnings() warnings}. Values which are equal to the
 * default values are not written.</p>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>The content of {@code <InlineFeature>} is not written.</li>
 *   <li>The geometries of spatial filters shall be envelopes or {@link org.apache.sis.geometries.Geometry}
 *       instances of the Apache SIS geometry library.</li>
 * </ul>
 *
 * <p>Usage:</p>
 * {@snippet lang="java" :
 *     try (StyleWriter writer = new StyleWriter(outputStream, StyleVersion.V1_1_0)) {
 *         writer.write(styledLayerDescriptor);
 *     }
 *     }
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class StyleWriter implements AutoCloseable {
    /**
     * The concrete writer chosen for the requested {@link StyleVersion}, never {@code null}.
     */
    private final SLDWriter delegate;

    /**
     * Creates a new writer using the given StAX cursor, targeting the given version.
     * The XML document header is written immediately by this constructor.
     *
     * @param  writer   the StAX cursor to write to.
     * @param  version  the version of the standards to target.
     * @throws XMLStreamException if the XML document header cannot be written.
     */
    public StyleWriter(final XMLStreamWriter writer, final StyleVersion version) throws XMLStreamException {
        switch (version) {
            case V1_0_0: delegate = new SLDWriter100(writer); break;
            case V1_1_0: delegate = new SLDWriter110(writer); break;
            default: throw new AssertionError(version);
        }
    }

    /**
     * Creates a new writer for the given output stream, targeting the given version.
     * The document is encoded in UTF-8.
     *
     * @param  out      the stream to write to.
     * @param  version  the version of the standards to target.
     * @throws XMLStreamException if the StAX cursor cannot be created.
     */
    public StyleWriter(final OutputStream out, final StyleVersion version) throws XMLStreamException {
        switch (version) {
            case V1_0_0: delegate = new SLDWriter100(out); break;
            case V1_1_0: delegate = new SLDWriter110(out); break;
            default: throw new AssertionError(version);
        }
    }

    /**
     * Writes the given object as the root element of the document.
     * This method shall be invoked only once, since a XML document has only one root element.
     * The given object can be an instance of any type listed in the {@link StyleReader} class Javadoc.
     *
     * @param  object  the object to write.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     * @throws DataStoreException if the object cannot be written in the target version.
     */
    public void write(final Object object) throws XMLStreamException, DataStoreException {
        delegate.write(object);
    }

    /**
     * Returns the warnings emitted while writing the document.
     * Warnings are emitted for elements or properties which cannot be written in the target version.
     *
     * @return the warnings, or an empty list if none.
     */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(delegate.warnings());
    }

    /**
     * Writes the end of the XML document and closes the underlying StAX cursor.
     *
     * @throws XMLStreamException if an error occurred while writing or closing the cursor.
     */
    @Override
    public void close() throws XMLStreamException {
        delegate.close();
    }
}
