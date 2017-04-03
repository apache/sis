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

import static org.junit.Assert.*;


/**
 * An {@link Appendable} that forward all output to two {@code Appendable}s.
 * This writer can be used for performing an exact copy of what is sent to an other writer.
 * For example, it may be used for echoing to the standard output the content sent to a file.
 * This writer is useful for debugging purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 * @since   0.3
 * @module
 */
public class EchoAppendable extends Appender {
    /**
     * The echo writer.
     */
    private final Appendable echo;

    /**
     * Creates a copy writer for the specified streams or buffers.
     *
     * @param main  the main stream or buffer.
     * @param echo  the echo stream or buffer.
     */
    public EchoAppendable(final Appendable main, final Appendable echo) {
        super(main);
        this.echo = echo;
    }

    /**
     * Writes a single character.
     *
     * @param  c  the character to append.
     * @return {@code this}.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public Appendable append(final char c) throws IOException {
        assertSame(out,  out .append(c));
        assertSame(echo, echo.append(c));
        return this;
    }

    /**
     * Writes a character sequence.
     *
     * @param  sequence  the character sequence to be written.
     * @return {@code this}.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public Appendable append(final CharSequence sequence) throws IOException {
        assertSame(out,  out .append(sequence));
        assertSame(echo, echo.append(sequence));
        return this;
    }

    /**
     * Writes a portion of a character sequence.
     *
     * @param  sequence  the character sequence to be written.
     * @param  start     index from which to start reading characters.
     * @param  end       index of the character following the last character to read.
     * @return {@code this}.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public Appendable append(final CharSequence sequence, final int start, final int end) throws IOException {
        assertSame(out,  out .append(sequence, start, end));
        assertSame(echo, echo.append(sequence, start, end));
        return this;
    }
}
