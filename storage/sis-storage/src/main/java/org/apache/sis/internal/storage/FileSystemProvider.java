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
package org.apache.sis.internal.storage;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;

/**
 * Files-related {@linkplain org.apache.sis.storage.DataStoreProvider provider}.
 * This interface provides additional descriptive informations on the supported
 * file types.
 *
 * @author Johann Sorel (Geomatys)
 * @since 1.0
 * @module
 */
public interface FileSystemProvider {

    /**
     * Get the list of this format mainly used file suffixes.
     * If the provider uses multiple files, this method should return
     * only the entry file suffixes.
     * <p>
     * For example : the shapefile format uses the files shp,shx,dbf,qix,...
     * but this collection only return the shp suffix.
     * </p>
     *
     * @return list of suffix, case insensitive, never null, can be empty.
     */
    Collection<String> getSuffix();

    /**
     * Binary and sometimes text formats often have a special header at the beginning
     * of the file.
     * This part of the file is call Signature or Magic number and is used by
     * file explorers and applications to identify the file type.
     *
     * <p>Some format may declare multiple different signatures. Such case can
     * happen for various reasons like historical evolution and changes on version updates.</p>
     *
     * <p>Default implementation returns an empty collection.</p>
     *
     * @return collection of signatures, never null, can be empty.
     */
    default Collection<byte[]> getSignature() {
        return Collections.EMPTY_LIST;
    }

    /**
     * Specialized implementation of {@linkplain DataStoreProvider#probeContent(org.apache.sis.storage.StorageConnector) probeContent}.
     * This implementation checks the input signature.
     *
     * @param  connector information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable by the {@code DataStore}
     *         instances created by this provider.
     * @throws DataStoreException if an I/O or SQL error occurred. The error shall be unrelated to the logical
     *         structure of the storage.
     */
    default ProbeResult probeContent(StorageConnector connector) throws DataStoreException {

        final Collection<byte[]> signatures = getSignature();
        if (signatures.isEmpty()) return ProbeResult.UNSUPPORTED_STORAGE;

        final ByteBuffer buffer = connector.getStorageAs(ByteBuffer.class);
        if (buffer != null) {
            for (byte[] signature : signatures) {
                try {
                    if (buffer.remaining() < signature.length) {
                        continue;
                    }
                    final byte[] candidate = new byte[signature.length];
                    buffer.get(candidate);

                    //compare signatures
                    if (Arrays.equals(signature, candidate)) {
                        return ProbeResult.SUPPORTED;
                    }
                } finally {
                    buffer.reset();
                }
            }
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

}
