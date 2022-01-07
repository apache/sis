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
package org.apache.sis.storage;

import java.io.IOException;
import java.io.Reader;
import java.io.FilterReader;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.storage.Resources;


/**
 * A temporary character reader used for probing purposes.
 * This reader does not allow mark/reset operations because the mark is reserved for this class.
 * The {@link #close()} method closes this reader but not the wrapped reader, which is only reset.
 *
 * <p>Note: this wrapper is not used if the reader is an instance of
 * {@link org.apache.sis.internal.storage.io.RewindableLineReader}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see ProbeInputStream
 * @see DataStoreProvider#probeContent(StorageConnector, Class, Prober)
 *
 * @since 1.2
 * @module
 */
final class ProbeReader extends FilterReader {
    /**
     * Creates a new reader which delegates everything to the given reader except the mark/reset operations.
     */
    ProbeReader(final StorageConnector owner, final Reader input) throws IOException, DataStoreException {
        super(input);
        if (!input.markSupported()) {
            throw new DataStoreException(Resources.format(Resources.Keys.MarkNotSupported_1, owner.getStorageName()));
        }
        input.mark(StorageConnector.DEFAULT_BUFFER_SIZE);
    }

    /**
     * Notifies the caller that marks are not supported on this reader.
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Throws an exception since marks are not supported on this reader.
     */
    @Override
    public void mark(int readlimit) throws IOException {
        throw new IOException(Errors.format(Errors.Keys.UnsupportedOperation_1, "mark"));
    }

    /**
     * Throws an exception since marks are not supported on this reader.
     */
    @Override
    public void reset() throws IOException {
        throw new IOException(Errors.format(Errors.Keys.UnsupportedOperation_1, "reset"));
    }

    /**
     * Closes this reader and resets the wrapped reader to its original position.
     */
    @Override
    public void close() throws IOException {
        final Reader input = in;
        in = null;
        if (input != null) {
            input.reset();
        }
    }
}
