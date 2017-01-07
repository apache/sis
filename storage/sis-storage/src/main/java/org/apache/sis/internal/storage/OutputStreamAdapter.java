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

import java.io.OutputStream;
import java.io.IOException;
import java.io.DataOutput;
import java.io.Flushable;
import java.io.Closeable;


/**
 * Wraps a {@link DataOutput} as a standard {@link OutputStream}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see InputStreamAdapter
 */
public final class OutputStreamAdapter extends OutputStream {
    /**
     * The underlying data output stream. In principle, public access to this field breaks encapsulation.
     * But since {@code OutputStreamAdapter} does not hold any state and just forwards every method calls
     * to that {@code DataOutput}, using on object or the other does not make a difference.
     */
    public final DataOutput output;

    /**
     * Constructs a new output stream.
     *
     * @param output  the stream to wrap.
     */
    public OutputStreamAdapter(final DataOutput output) {
        this.output = output;
    }

    /**
     * Writes the specified byte to the output stream.
     *
     * @param  b  the byte to write.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(final int b) throws IOException {
        output.write(b);
    }

    /**
     * Writes the specified bytes to the output stream.
     *
     * @param  b  the bytes to write.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(final byte[] b) throws IOException {
        output.write(b);
    }

    /**
     * Writes the specified sub-array to the output stream.
     *
     * @param  b    the bytes to write.
     * @param  off  the start offset in the data.
     * @param  len  the number of bytes to write.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        output.write(b, off, len);
    }

    /**
     * Forces any buffered output bytes to be written out.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        if (output instanceof Flushable) {
            ((Flushable) output).flush();
        }
    }

    /**
     * Releases any system resources associated with the output stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (output instanceof Closeable) {
            ((Closeable) output).close();
        }
    }
}
