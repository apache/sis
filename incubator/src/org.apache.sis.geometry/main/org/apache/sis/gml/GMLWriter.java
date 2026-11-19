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
package org.apache.sis.gml;

import java.io.OutputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;


/**
 * Version-selecting facade over {@link GML2Writer} and {@link GML3Writer}. Unlike {@link GMLReader},
 * there is nothing to detect when writing — the caller simply states which {@link GMLVersion} to
 * target, and this class delegates to the matching concrete writer.
 *
 * <p>Usage:</p>
 * {@snippet lang="java" :
 *     try (GMLWriter writer = new GMLWriter(outputStream, GMLVersion.V3)) {
 *         writer.writeGeometry(geometry);
 *     }
 *     }
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GMLWriter implements AutoCloseable {
    /**
     * The concrete writer chosen for the requested {@link GMLVersion}.
     */
    private final GMLGeometryWriter delegate;

    /**
     * Creates a new writer using the given StAX cursor, targeting the given GML version.
     *
     * @param  writer   the StAX cursor to write to.
     * @param  version  the GML version to target.
     * @throws XMLStreamException if the XML document header cannot be written.
     */
    public GMLWriter(final XMLStreamWriter writer, final GMLVersion version) throws XMLStreamException {
        delegate = create(writer, version, false);
    }

    /**
     * Creates a new writer for the given output stream, targeting the given GML version.
     *
     * @param  out      the stream to write to.
     * @param  version  the GML version to target.
     * @throws XMLStreamException if the StAX cursor cannot be created.
     */
    public GMLWriter(final OutputStream out, final GMLVersion version) throws XMLStreamException {
        this(XMLOutputFactory.newInstance().createXMLStreamWriter(out, "UTF-8"), version);
    }

    /**
     * Creates a new writer delegating to the given concrete writer.
     */
    private GMLWriter(final GMLGeometryWriter delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates a writer of geometry elements inside a XML document owned by the caller.
     * The returned writer does not write the XML document header, and its {@link #close()}
     * method neither writes the end of the document nor closes the given cursor.
     * This is useful for writing geometries embedded in other XML documents,
     * for example in the spatial operators of a filter.
     *
     * <p>Each geometry element declares the GML namespace as its default namespace,
     * so the enclosing document does not need to declare the GML namespace.</p>
     *
     * @param  writer   the StAX cursor to write to, positioned where the geometry shall be written.
     * @param  version  the GML version to target.
     * @return a writer of geometry elements inside the given document.
     * @throws XMLStreamException if the writer cannot be created.
     */
    public static GMLWriter fragment(final XMLStreamWriter writer, final GMLVersion version) throws XMLStreamException {
        return new GMLWriter(create(writer, version, true));
    }

    /**
     * Creates the concrete writer for the given GML version.
     *
     * @param  writer    the StAX cursor to write to.
     * @param  version   the GML version to target.
     * @param  fragment  whether to write geometries inside a document owned by the caller.
     */
    private static GMLGeometryWriter create(final XMLStreamWriter writer, final GMLVersion version, final boolean fragment)
            throws XMLStreamException
    {
        switch (version) {
            case V2:   return new GML2Writer(writer, fragment);
            case V3:   return new GML3Writer(writer, GML3Tags.NAMESPACE_3, fragment);
            case V3_1: return new GML3Writer(writer, GML2Tags.NAMESPACE_2, fragment);
            default:   throw new AssertionError(version);
        }
    }

    /**
     * Writes the given geometry as a GML element. The {@code srsName} attribute is derived from
     * {@link Geometry#getCoordinateReferenceSystem()}; no {@code srsName} attribute is written
     * when that CRS is {@linkplain org.apache.sis.geometries.Geometries#isUndefined undefined}.
     *
     * @param  geometry  the geometry to write.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     * @throws DataStoreContentException if the given geometry type is not supported.
     * @throws DataStoreReferencingException if the CRS cannot be expressed as a {@code srsName} value.
     */
    public void writeGeometry(final Geometry geometry)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        delegate.writeGeometry(geometry);
    }

    /**
     * Writes the given geometry as a GML element, using the given CRS for the {@code srsName}
     * attribute instead of the one attached to the geometry (if any).
     *
     * @param  geometry  the geometry to write.
     * @param  crs       the coordinate reference system to write as {@code srsName}, or {@code null} if none.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     * @throws DataStoreContentException if the given geometry type is not supported.
     * @throws DataStoreReferencingException if the CRS cannot be expressed as a {@code srsName} value.
     */
    public void writeGeometry(final Geometry geometry, final CoordinateReferenceSystem crs)
            throws XMLStreamException, DataStoreContentException, DataStoreReferencingException
    {
        delegate.writeGeometry(geometry, crs);
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
