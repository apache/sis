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


/**
 * Contract implemented by all version-specific GML geometry readers ({@link GML2Reader}, {@link GML3Reader}),
 * allowing {@link GMLReader} to hold a reference to whichever concrete reader it dispatches to.
 *
 * @author  Johann Sorel (Geomatys)
 */
interface GMLGeometryReader extends AutoCloseable {
    /**
     * Reads the next GML geometry element.
     *
     * @return the geometry read from the underlying StAX cursor.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     * @throws DataStoreContentException if the content is not a supported GML geometry.
     * @throws DataStoreReferencingException if a {@code srsName} attribute cannot be resolved.
     */
    Geometry readGeometry() throws XMLStreamException, DataStoreContentException, DataStoreReferencingException;

    @Override
    public abstract void close() throws XMLStreamException;

}
