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
package org.apache.sis.storage.geopose;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Reads and writes OGC GeoPose 1.0 documents.
 *
 * <p>The standardization target of a document is not self-describing: a GeoPose document carries
 * no type member, so the expected target must be given by the caller.</p>
 *
 * <ul>
 *   <li>The target type is one of the classes of the
 *       {@code org.apache.sis.storage.geopose.binding} package.</li>
 *   <li>Reading a document into the wrong target yields unknown members rather than an error.</li>
 *   <li>Instances are safe for concurrent use.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0 Data Exchange Standard"
 */
public class GeoPose {

    /**
     * The Jackson mapper doing the actual work.
     */
    private final ObjectMapper mapper;

    /**
     * Creates a reader and writer using a default Jackson mapper.
     */
    public GeoPose() {
        this(new ObjectMapper());
    }

    /**
     * Creates a reader and writer using the given Jackson mapper.
     *
     * @param  mapper  the mapper to use, for example one configured to indent its output
     */
    public GeoPose(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Reads a GeoPose document of the given standardization target.
     *
     * @param  <T>     the target type
     * @param  input   the stream to read, not closed by this method
     * @param  target  the class of the expected standardization target
     * @return the document content
     * @throws IOException if the stream cannot be read or its content is not valid
     */
    public <T> T read(final InputStream input, final Class<T> target) throws IOException {
        return mapper.readValue(input, target);
    }

    /**
     * Reads a GeoPose document of the given standardization target.
     *
     * @param  <T>     the target type
     * @param  json    the document to read
     * @param  target  the class of the expected standardization target
     * @return the document content
     * @throws IOException if the content is not valid
     */
    public <T> T read(final String json, final Class<T> target) throws IOException {
        return mapper.readValue(json, target);
    }

    /**
     * Writes a GeoPose document.
     *
     * @param  pose    the standardization target instance to write
     * @param  output  the stream to write to, not closed by this method
     * @throws IOException if the stream cannot be written
     */
    public void write(final Object pose, final OutputStream output) throws IOException {
        mapper.writeValue(output, pose);
    }

    /**
     * Returns the given GeoPose document as a character string.
     *
     * @param  pose  the standardization target instance to write
     * @return the JSON encoding of the given instance
     * @throws IOException if the instance cannot be encoded
     */
    public String write(final Object pose) throws IOException {
        return mapper.writeValueAsString(pose);
    }
}
