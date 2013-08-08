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
package org.apache.sis.internal.storage.xml;

import java.io.Reader;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;


/**
 * The provider of {@link XMLStore} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public class XMLStoreProvider extends DataStoreProvider {
    /**
     * The expected XML header. According XML specification, this declaration is required to appear
     * at the document beginning (no space allowed before the declaration).
     */
    private static final byte[] HEADER = {'<','?','x','m','l',' '};

    /**
     * Creates a new provider.
     */
    public XMLStoreProvider() {
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be supported by {@link XMLStore}.
     * Returning {@code SUPPORTED} from this method does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the storage
     * header.
     */
    @Override
    public ProbeResult canOpen(final StorageConnector storage) throws DataStoreException {
        /*
         * Usual case. This include InputStream, DataInput, File, Path, URL, URI.
         */
        final ByteBuffer buffer = storage.getStorageAs(ByteBuffer.class);
        if (buffer != null) {
            if (buffer.remaining() < HEADER.length) {
                return ProbeResult.INSUFFICIENT_BYTES;
            }
            for (int i=0; i<HEADER.length; i++) {
                if (buffer.get(i) != HEADER[i]) {
                    return ProbeResult.UNSUPPORTED_STORAGE;
                }
            }
            return ProbeResult.SUPPORTED;
        }
        /*
         * We should enter in this block only if the user gave us explicitely a Reader.
         * A common case is a StringReader wrapping a String object.
         */
        final Reader reader = storage.getStorageAs(Reader.class);
        if (reader != null) try {
            reader.mark(HEADER.length);
            for (int i=0; i<HEADER.length; i++) {
                if (reader.read() != HEADER[i]) {
                    reader.reset();
                    return ProbeResult.UNSUPPORTED_STORAGE;
                }
            }
            reader.reset();
            return ProbeResult.SUPPORTED;
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Returns a {@link XMLStore} implementation associated with this provider.
     *
     * @param  storage Information about the storage (URL, stream, <i>etc</i>).
     * @return A data store implementation associated with this provider for the given storage.
     * @throws DataStoreException If an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector storage) throws DataStoreException {
        return new XMLStore(storage);
    }
}
