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
package org.apache.sis.internal.storage.wkt;

import java.util.Arrays;
import java.io.Reader;
import java.io.IOException;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.CRS;

import static java.util.Collections.singleton;


/**
 * A data store which creates data objects from a WKT definition.
 * This {@code DataStore} implementation is basically a facade for the {@link CRS#fromWKT(String)} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class Store extends DataStore {
    /**
     * The file name.
     */
    private final String name;

    /**
     * The reader, set by the constructor and cleared when no longer needed.
     */
    private Reader source;

    /**
     * The parsed object, initialized only when first needed.
     * May still {@code null} if the parsing failed.
     */
    private Object object;

    /**
     * The metadata object, created when first needed.
     */
    private Metadata metadata;

    /**
     * Creates a new WKT store from the given file, URL or stream.
     *
     * @param  connector Information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException If an error occurred while opening the stream.
     */
    public Store(final StorageConnector connector) throws DataStoreException {
        name = connector.getStorageName();
        source = connector.getStorageAs(Reader.class);
        connector.closeAllExcept(source);
        if (source == null) {
            throw new DataStoreException(Errors.format(Errors.Keys.CanNotOpen_1, name));
        }
    }

    /**
     * Parses the object, if not already done. Note that {@link #object} may still be null
     * if an exception has been thrown at this invocation time or in previous invocation.
     *
     * @return The metadata associated to the parsed object, or {@code null} if none.
     * @throws DataStoreException If an error occurred during the parsing process.
     */
    @Override
    public Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final Reader in = source;
            source = null;                      // Cleared first in case of error.
            if (in != null) try {
                char[] buffer = new char[StoreProvider.READ_AHEAD_LIMIT];
                int length = 0;
                try {
                    int n;
                    while ((n = in.read(buffer, length, buffer.length - length)) >= 0) {
                        if ((length += n) >= buffer.length) {
                            if (n >= Integer.MAX_VALUE / 1024) {     // Arbitrary size limit.
                                throw new DataStoreException(Errors.format(Errors.Keys.ExcessiveStringSize));
                            }
                            buffer = Arrays.copyOf(buffer, n << 1);
                        }
                    }
                } finally {
                    in.close();
                }
                object = CRS.fromWKT(String.valueOf(buffer, 0, length));
            } catch (IOException e) {
                throw new DataStoreException(Errors.format(Errors.Keys.CanNotRead_1, name), e);
            } catch (FactoryException e) {
                throw new DataStoreException(Errors.format(Errors.Keys.CanNotRead_1, name), e);
            }
            if (object instanceof ReferenceSystem) {
                final DefaultMetadata d = new DefaultMetadata();
                d.setReferenceSystemInfo(singleton((ReferenceSystem) object));
                metadata = d;
            }
        }
        return metadata;
    }

    /**
     * Closes this data store and releases any underlying resources.
     *
     * @throws DataStoreException If an error occurred while closing this data store.
     */
    @Override
    public void close() throws DataStoreException {
        final Reader s = source;
        source = null;                  // Cleared first in case of failure.
        object = null;
        if (s != null) try {
            s.close();
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }
}
