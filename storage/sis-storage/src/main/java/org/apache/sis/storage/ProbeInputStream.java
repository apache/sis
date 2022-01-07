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
import java.io.InputStream;
import java.io.FilterInputStream;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.storage.Resources;


/**
 * A temporary input stream used for probing purposes.
 * This stream does not allow mark/reset operations because the mark is reserved for this class.
 * The {@link #close()} method closes this stream but not the wrapped stream, which is only reset.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see ProbeReader
 * @see DataStoreProvider#probeContent(StorageConnector, Class, Prober)
 *
 * @since 1.2
 * @module
 */
final class ProbeInputStream extends FilterInputStream {
    /**
     * Creates a new input stream which delegates everything to the given input except the mark/reset operations.
     */
    ProbeInputStream(final StorageConnector owner, final InputStream input) throws IOException, DataStoreException {
        super(input);
        if (!input.markSupported()) {
            throw new DataStoreException(Resources.format(Resources.Keys.MarkNotSupported_1, owner.getStorageName()));
        }
        input.mark(StorageConnector.DEFAULT_BUFFER_SIZE);
    }

    /**
     * Notifies the caller that marks are not supported on this input stream.
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Does nothing since marks are not supported on this input stream.
     * Note that doing nothing is the behavior of the default {@link InputStream#mark(int)} implementation.
     * In particular, we can not declare the checked {@link IOException} here.
     */
    @Override
    public void mark(int readlimit) {
    }

    /**
     * Throws an exception since marks are not supported on this input stream.
     */
    @Override
    public void reset() throws IOException {
        throw new IOException(Errors.format(Errors.Keys.UnsupportedOperation_1, "reset"));
    }

    /**
     * Closes this stream and resets the wrapped stream to its original position.
     */
    @Override
    public void close() throws IOException {
        final InputStream input = in;
        in = null;
        if (input != null) {
            input.reset();
        }
    }
}
