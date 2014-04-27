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

import java.io.IOException;
import java.io.OutputStream;
import org.apache.sis.util.Workaround;


/**
 * A fix for what seems to be a bug in the standard {@link javax.imageio.stream.MemoryCacheImageOutputStream}.
 * The standard class provided in the JDK lost the last written bits when the stream is flushed or when a seek
 * is performed.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@Workaround(library = "JDK", version = "1.8")
final strictfp class MemoryCacheImageOutputStream extends javax.imageio.stream.MemoryCacheImageOutputStream
        implements java.io.Closeable    // Needed on JDK 6 only.
{
    /**
     * Creates a new instance which will write the data in the given stream.
     */
    MemoryCacheImageOutputStream(final OutputStream stream) {
        super(stream);
    }

    /**
     * Writes the bits (if any) before to seek.
     *
     * <p>In the default {@link javax.imageio.stream.MemoryCacheImageOutputStream} implementation,
     * when we seek position the bits previously written by the {@link #writeBits(long, int)} method
     * are lost. This method overriding fix this issue.</p>
     */
    @Override
    public void seek(final long pos) throws IOException {
        flushBits();
        super.seek(pos);
    }

    /**
     * Writes the bits (if any) before to flush the stream.
     */
    @Override
    public void flush() throws IOException {
        flushBits();
        super.flush();
    }

    /**
     * Writes the bits (if any) before to close the stream.
     */
    @Override
    public void close() throws IOException {
        flushBits();
        super.close();
    }
}
