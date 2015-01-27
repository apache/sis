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

import java.io.IOException;


/**
 * Delegates calls to {@code append(CharSequence, int, int)} to a sequence of calls
 * to {@code append(char)}. This is used in order to explore more code paths in
 * {@link Appender} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class SingleCharAppendable extends Appender {
    /**
     * Constructs a {@code SingleCharAppendable} object delegating to the given {@code Appendable}.
     *
     * @param out The underlying stream or buffer.
     */
    public SingleCharAppendable(final Appendable out) {
        super(out);
    }

    /**
     * Forwards to {@link #out}.
     */
    @Override
    public Appendable append(final char c) throws IOException {
        out.append(c);
        return this;
    }

    /**
     * Writes a portion of a character sequence.
     *
     * @param  sequence The character sequence to be written.
     * @param  start    Index from which to start reading characters.
     * @param  end      Index of the character following the last character to read.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public Appendable append(final CharSequence sequence, int start, final int end) throws IOException {
        while (start < end) {
            out.append(sequence.charAt(start++));
        }
        return this;
    }
}
