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

import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.sis.geometries.Geometry;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;


/**
 * Version-detecting facade over {@link GML2Reader} and {@link GML3Reader}: inspects the root
 * geometry element's namespace and local name, then delegates to whichever concrete reader
 * understands it, so that a caller does not need to know a document's GML version in advance.
 *
 * <p>Detection needs no lookahead into the document's content: the root element's own namespace
 * URI and local name — already available the instant the StAX cursor reaches its
 * {@link XMLStreamConstants#START_ELEMENT START_ELEMENT} event — are sufficient, because
 * {@link GML3Reader} is a strict capability superset of {@link GML2Reader} for every geometry
 * element common to both (same coordinate/boundary spelling tolerance, same CRS handling, same
 * return types). As a direct, intentional consequence, every reachable branch of the
 * dispatch below resolves to {@link GML3Reader}: {@link GML2Reader} is never actually selected
 * by this facade, since nothing ever needs it to be. {@code GML2Reader} remains directly usable
 * on its own by callers who explicitly want its leaner, GML-2.0-only surface.</p>
 *
 * <p>Usage:</p>
 * {@snippet lang="java" :
 *     try (GMLReader reader = new GMLReader(inputStream)) {
 *         Geometry geometry = reader.readGeometry();
 *     }
 *     }
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GMLReader implements AutoCloseable {
    /**
     * The concrete reader chosen for the document, never {@code null} after construction.
     */
    private final GMLGeometryReader delegate;

    /**
     * Creates a new reader using the given StAX cursor, choosing the concrete GML version reader
     * as soon as the root geometry element is reached. The cursor can be positioned anywhere
     * before that root element.
     *
     * @param  reader  the StAX cursor to read from.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     * @throws DataStoreContentException if no root element could be found.
     */
    public GMLReader(final XMLStreamReader reader) throws XMLStreamException, DataStoreContentException {
        moveToGeometryRoot(reader);
        delegate = chooseReader(reader);
    }

    /**
     * Creates a new reader for the given input stream.
     *
     * @param  in  the stream to read from.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     * @throws DataStoreContentException if no root element could be found.
     */
    public GMLReader(final InputStream in) throws XMLStreamException, DataStoreContentException {
        this(XMLInputFactory.newInstance().createXMLStreamReader(in));
    }

    /**
     * Moves the cursor to the first {@code START_ELEMENT}, skipping the XML prologue
     * (declaration, comments, DTD, etc.). Does nothing if the cursor is already there.
     */
    private static void moveToGeometryRoot(final XMLStreamReader reader) throws XMLStreamException, DataStoreContentException {
        int type = reader.getEventType();
        while (type != XMLStreamConstants.START_ELEMENT) {
            if (!reader.hasNext()) {
                throw new DataStoreContentException("No GML geometry element found.");
            }
            type = reader.next();
        }
    }

    /**
     * Chooses the concrete reader to delegate to, based solely on the root element's own
     * namespace URI and local name (the cursor is not advanced any further by this method).
     */
    private static GMLGeometryReader chooseReader(final XMLStreamReader reader) {
        final String ns = reader.getNamespaceURI();
        if (GML3Tags.NAMESPACE_3.equals(ns)) {
            return new GML3Reader(reader);                        // Versioned 3.2 namespace: unambiguous.
        }
        switch (reader.getLocalName()) {
            case GML3Tags.CURVE:              case GML3Tags.SURFACE:
            case GML3Tags.RING:               case GML3Tags.COMPOSITE_CURVE:
            case GML3Tags.COMPOSITE_SURFACE:  case GML3Tags.ORIENTABLE_CURVE:
            case GML3Tags.ORIENTABLE_SURFACE: case GML3Tags.SOLID:
            case GML3Tags.COMPOSITE_SOLID:    case GML3Tags.MULTI_CURVE:
            case GML3Tags.MULTI_SURFACE:
                return new GML3Reader(reader);                    // GML3-only name: unambiguous.
            default:
                /*
                 * A name common to GML2/GML3 (Point, LineString, …, Box, MultiGeometry) or an
                 * unrecognized name. GML3Reader is a superset of GML2Reader for every common
                 * name (see class javadoc), so this choice is always correct.
                 */
                return new GML3Reader(reader);
        }
    }

    /**
     * Reads the next GML geometry element, delegating to whichever concrete reader was chosen.
     *
     * @return the geometry read from the underlying StAX cursor.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     * @throws DataStoreContentException if the content is not a supported GML geometry.
     * @throws DataStoreReferencingException if a {@code srsName} attribute cannot be resolved.
     */
    public Geometry readGeometry() throws XMLStreamException, DataStoreContentException, DataStoreReferencingException {
        return delegate.readGeometry();
    }

    /**
     * Closes the underlying StAX cursor.
     *
     * @throws XMLStreamException if an error occurred while closing the cursor.
     */
    @Override
    public void close() throws XMLStreamException {
        delegate.close();
    }
}
