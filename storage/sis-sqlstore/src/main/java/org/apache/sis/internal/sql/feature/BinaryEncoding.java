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
package org.apache.sis.internal.sql.feature;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.sis.internal.jdk9.HexFormat;


/**
 * The way binary data are encoded in a table column.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public enum BinaryEncoding {
    /**
     * The bytes returned by the JDBC driver in a query are directly the binary data.
     */
    RAW,

    /**
     * The bytes returned by the JDBC driver are encoded as hexadecimal numbers.
     */
    HEXADECIMAL() {
        /** Returns the value in the specified column as an array of decoded bytes. */
        @Override public byte[] getBytes(final ResultSet results, final int columnIndex) throws SQLException {
            final String value = results.getString(columnIndex);
            return (value != null) ? HexFormat.of().parseHex(value) : null;
        }

        /** Returns an input stream decoding bytes on-the-fly. */
        @Override public InputStream decode(final InputStream source) {
            return new FromHex(source);
        }
    };

    /**
     * Returns the value in the specified column as an array of decoded bytes.
     * If the bytes returned by the JDBC driver are encoded, this method decode them.
     *
     * @param  results       the result set from which to get the values.
     * @param  columnIndex   column from which to get the values.
     * @return the column values, or {@code null} if none.
     * @throws SQLException if an error occurred while fetching column values.
     *
     * @see ResultSet#getBytes(int)
     */
    public byte[] getBytes(final ResultSet results, final int columnIndex) throws SQLException {
        return results.getBytes(columnIndex);
    }

    /**
     * Returns an input stream decoding bytes on-the-fly.
     *
     * @param  source  the stream of data in their encoded format.
     * @return a stream of decoded bytes.
     *
     * @see ResultSet#getBinaryStream(int)
     */
    public InputStream decode(final InputStream source) {
        return source;
    }

    /**
     * An input stream which converts hexadecimal string on-the-fly.
     */
    private static final class FromHex extends InputStream {
        /** The input stream providing hexadecimal digits. */
        private final InputStream source;

        /** Creates a new input stream which will decode the given source. */
        FromHex(final InputStream source) {
            this.source = source;
        }

        /** Returns the next decoded byte. */
        @Override public int read() throws IOException {
            final int hi = source.read(); if (hi < 0) return -1;
            final int lo = source.read(); if (lo < 0) throw new EOFException();
            return (HexFormat.fromHexDigit(hi) << 4) | HexFormat.fromHexDigit(lo);
        }

        /** Skips over and discards <var>n</var> bytes of data. */
        @Override public long skip(long n) throws IOException {
            if ((n & 0xC000000000000000L) == 0) n <<= 1;
            n = source.skip(n);
            if ((n & 1) != 0 && source.read() >= 0) n++;
            return n >> 1;
        }

        /** Returns an estimate of the number of bytes that can be read. */
        @Override public int available() throws IOException {
            return source.available() >> 1;
        }

        /** Tests if this input stream supports the mark and reset methods. */
        @Override public boolean markSupported() {
            return source.markSupported();
        }

        /** Marks the current position in this input stream. */
        @Override public void mark(int n) {
            if ((n & 0xC0000000) == 0) n <<= 1;
            else n = Integer.MAX_VALUE;
            source.mark(n);
        }

        /** Repositions this stream to the position of the mark. */
        @Override public void reset() throws IOException {
            source.reset();
        }

        /** Closes this input stream. */
        @Override public void close() throws IOException {
            source.close();
        }
    }
}
