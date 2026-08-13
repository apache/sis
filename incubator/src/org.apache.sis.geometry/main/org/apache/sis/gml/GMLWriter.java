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
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.sis.geometries.Geometry;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


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
        switch (version) {
            case V2: delegate = new GML2Writer(writer); break;
            case V3: delegate = new GML3Writer(writer); break;
            default: throw new AssertionError(version);
        }
    }

    /**
     * Creates a new writer for the given output stream, targeting the given GML version.
     *
     * @param  out      the stream to write to.
     * @param  version  the GML version to target.
     * @throws XMLStreamException if the StAX cursor cannot be created.
     */
    public GMLWriter(final OutputStream out, final GMLVersion version) throws XMLStreamException {
        switch (version) {
            case V2: delegate = new GML2Writer(out); break;
            case V3: delegate = new GML3Writer(out); break;
            default: throw new AssertionError(version);
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
