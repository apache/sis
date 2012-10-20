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
package org.apache.sis.io;

import java.io.Writer;
import java.io.Flushable;
import java.io.Closeable;
import java.io.IOException;
import org.apache.sis.util.Decorator;
import org.apache.sis.util.ArgumentChecks;


/**
 * Base class for writing filtered characters to another {@link Appendable}.
 * This base class performs a work similar to the {@link java.io.FilterWriter} work,
 * except for the following:
 *
 * <ul>
 *   <li>The filtered output is sent to an arbitrary {@link Appendable} instead than
 *       to the {@link java.io.Writer} sub-type.</li>
 *   <li>No synchronization is performed.</li>
 * </ul>
 *
 * If needed, this {@code FilteredAppendable} can be viewed as a synchronized {@link Writer}
 * by invoking the {@link #asWriter()} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see java.io.FilterWriter
 */
@Decorator(Appendable.class)
public abstract class FilteredAppendable implements Appendable {
    /**
     * The underlying character output stream or buffer.
     */
    protected final Appendable out;

    /**
     * Creates a new filtered formatter which will send its output to the given stream or buffer.
     *
     * @param out The underlying character output stream or buffer.
     */
    protected FilteredAppendable(final Appendable out) {
        ArgumentChecks.ensureNonNull("out", out);
        this.out = out;
    }

    /**
     * Appends the specified character sequence.
     * The default implementation delegates to {@link #append(CharSequence, int, int)}.
     *
     * @param  sequence The character sequence to append, or {@code null}.
     * @return A reference to this {@code Appendable}.
     * @throws IOException If an I/O error occurred.
     */
    @Override
    public Appendable append(CharSequence sequence) throws IOException {
        if (sequence == null) {
            sequence = "null";
        }
        append(sequence, 0, sequence.length());
        return this;
    }

    /**
     * If the given {@code out} argument implements {@link Flushable}, or is a
     * {@code FilteredAppendable} wrapper around a flusheable object, delegates
     * to that object. Otherwise do nothing.
     */
    static void flush(Appendable out) throws IOException {
        while (!(out instanceof Flushable)) {
            if (!(out instanceof FilteredAppendable)) {
                return;
            }
            out = ((FilteredAppendable) out).out;
        }
        ((Flushable) out).flush();
    }

    /**
     * If the given {@code out} argument implements {@link Closeable}, or is a
     * {@code FilteredAppendable} wrapper around a closeable object, delegates
     * to that object. Otherwise do nothing.
     */
    static void close(Appendable out) throws IOException {
        while (!(out instanceof Closeable)) {
            if (out instanceof Flushable) {
                ((Flushable) out).flush();
            }
            if (!(out instanceof FilteredAppendable)) {
                return;
            }
            out = ((FilteredAppendable) out).out;
        }
        ((Closeable) out).close();
    }

    /**
     * Returns a view of this {@code Appendable} as a writer. Any write operations performed
     * on the writer will be forwarded to this {@code Appendable} in a synchronized block.
     *
     * @return A view of this {@code Appendable} as a writer.
     */
    public Writer asWriter() {
        // No need to cache this instance, since creating AppendableAdapter
        // is cheap and AppendableAdapter does not hold any internal state.
        return new AppendableAdapter(this);
    }
}
