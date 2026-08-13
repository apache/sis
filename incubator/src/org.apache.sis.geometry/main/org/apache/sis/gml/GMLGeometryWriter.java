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

import javax.xml.stream.XMLStreamException;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Contract implemented by all version-specific GML geometry writers ({@link GML2Writer}, {@link GML3Writer}),
 * allowing {@link GMLWriter} to hold a reference to whichever concrete writer it dispatches to.
 *
 * @author  Johann Sorel (Geomatys)
 */
interface GMLGeometryWriter extends AutoCloseable {
    /**
     * Writes the given geometry as a GML element. The {@code srsName} attribute is derived
     * from {@link Geometry#getCoordinateReferenceSystem()}, and omitted when that CRS is
     * {@linkplain org.apache.sis.geometries.Geometries#isUndefined undefined}.
     *
     * @param  geometry  the geometry to write.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     * @throws DataStoreContentException if the given geometry type is not supported.
     * @throws DataStoreReferencingException if the CRS cannot be expressed as a {@code srsName} value.
     */
    void writeGeometry(Geometry geometry) throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

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
    void writeGeometry(Geometry geometry, CoordinateReferenceSystem crs) throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    @Override
    public abstract void close() throws XMLStreamException;
}
