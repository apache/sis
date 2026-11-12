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
package org.apache.sis.storage.hdf.filter;

import java.io.IOException;

/**
 * A filter of the HDF-5 filter pipeline, applied on the bytes of each chunk.
 * <p>
 * Filters are applied in pipeline order when writing, and reversed in the
 * opposite order when reading.
 *
 * @author Johann Sorel (Geomatys)
 * @see org.apache.sis.storage.hdf.message.FilterPipelineMessage
 */
public interface Filter {

    /**
     * Reverses this filter on the bytes of a chunk as stored in the file.
     *
     * @param chunkdata filtered chunk bytes
     * @return decoded bytes, may be the given array if the filter has no effect
     * @throws IOException if the bytes could not be decoded.
     */
    byte[] decode(byte[] chunkdata) throws IOException;

    /**
     * Applies this filter on the raw bytes of a chunk.
     *
     * @param chunkdata raw chunk bytes
     * @return encoded bytes, may be the given array if the filter has no effect
     * @throws IOException if the bytes could not be encoded.
     */
    byte[] encode(byte[] chunkdata) throws IOException;

}
