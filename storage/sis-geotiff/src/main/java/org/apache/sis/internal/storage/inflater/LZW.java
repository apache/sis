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
package org.apache.sis.internal.storage.inflater;

import java.util.Arrays;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.storage.io.ChannelDataInput;


/**
 * Inflater for values encoded with the LZW compression.
 * This compression is described in section 13 of TIFF 6 specification. "LZW Compression".
 * Each code is written using at least 9 bits and at most 12 bits.
 *
 * <h2>Legal note</h2>
 * Unisys's patent on the LZW algorithm expired in 2004.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
final class LZW extends CompressionChannel {
    /**
     * A 12 bits code meaning that we have exhausted the 4093 available codes
     * and most reset the table to the initial set of 9 bits code.
     */
    private static final int CLEAR_CODE = 256;

    /**
     * End of information. This code appears at the end of a strip.
     */
    private static final int EOI_CODE = 257;

    /**
     * First code which is not one of the predefined nodes.
     */
    private static final int FIRST_ADAPTATIVE_CODE = 258;

    /**
     * For computing value of {@link #nextAvailableEntry} when {@link #codeSize} needs to be incremented.
     * TIFF specification said that the size needs to be incremented after codes 510, 1022 and 2046 are
     * added to the {@link #sequencesForCodes} table.
     */
    private static final int OFFSET_TO_MAXIMUM = FIRST_ADAPTATIVE_CODE + 1;

    /**
     * Initial number of bits in a code.
     */
    private static final int MIN_CODE_SIZE = Byte.SIZE + 1;

    /**
     * Maximum number of bits in a code, inclusive.
     */
    private static final int MAX_CODE_SIZE = 12;

    /**
     * Sequences of bytes associated to codes equals or greater than {@value #FIRST_ADAPTATIVE_CODE}.
     * Codes below {@value #FIRST_ADAPTATIVE_CODE} are implicit and not stored in this table.
     */
    private final byte[][] sequencesForCodes;

    /**
     * The sequence of bytes associated to the code in previous iteration.
     */
    private byte[] previousSequence;

    /**
     * Index of the next entry available in {@link #sequencesForCodes}.
     * This is related to the next available code by {@code code = nextAvailableEntry + FIRST_ADAPTATIVE_CODE}.
     */
    private int nextAvailableEntry;

    /**
     * Number of bits to read for the next code. This number starts at 9 and increases until {@value #MAX_CODE_SIZE}.
     * After {@value #MAX_CODE_SIZE} bits, a {@link #CLEAR_CODE} should occurs in the stream of LZW data.
     */
    private int codeSize;

    /**
     * If some bytes could not be written in previous {@code read(…)} execution because the target buffer was full,
     * those bytes. Otherwise {@code null}.
     */
    private byte[] pending;

    /**
     * Whether the {@link #EOI_CODE} has been found.
     */
    private boolean done;

    /**
     * Creates a new channel which will decompress data from the given input.
     * The {@link #setInputRegion(long, long)} method must be invoked after construction
     * before a reading process can start.
     *
     * @param  input  the source of data to decompress.
     */
    public LZW(final ChannelDataInput input) {
        super(input);
        sequencesForCodes = new byte[(1 << MAX_CODE_SIZE) - FIRST_ADAPTATIVE_CODE][];
    }

    /**
     * Prepares this inflater for reading a new tile or a new band of a tile.
     *
     * @param  start      stream position where to start reading.
     * @param  byteCount  number of byte to read from the input.
     * @throws IOException if the stream can not be seek to the given start position.
     */
    @Override
    public void setInputRegion(final long start, final long byteCount) throws IOException {
        super.setInputRegion(start, byteCount);
        previousSequence   = ArraysExt.EMPTY_BYTE;
        codeSize           = MIN_CODE_SIZE;
        nextAvailableEntry = 0;
        pending            = null;
        done               = false;
    }

    /**
     * Decompresses some bytes from the {@linkplain #input} into the given destination buffer.
     *
     * @param  target  the buffer into which bytes are to be transferred.
     * @return the number of bytes read, or -1 if end-of-stream.
     * @throws IOException if some other I/O error occurs.
     */
    @Override
    public int read(final ByteBuffer target) throws IOException {
        final int start = target.position();
        /*
         * If a previous invocation of this method was unable to write some data
         * because the target buffer was full, write these remaining data first.
         */
        if (pending != null) {
            final int r = target.remaining();
            if (pending.length <= r) {
                target.put(pending);
                pending = null;
                if (done) return r;
            } else {
                target.put(pending, 0, r);
                pending = Arrays.copyOfRange(pending, r, pending.length);
                return r;   // Can not write more than what we just wrote.
            }
        } else {
            done |= finished();
        }
        if (done) {
            return -1;
        }
        /*
         * Below is adapted from TIFF version 6 specification, section 13, LZW Decoding.
         * Comments such as "InitializeTable()" refer to methods in TIFF specification.
         * The body is a little bit more complex because codes in [0 … 255] range are
         * handled as a special case instead of stored in the `sequencesForCodes` table.
         */
        byte[] write     = previousSequence;
        previousSequence = null;
        int maximumIndex = (1 << codeSize) - OFFSET_TO_MAXIMUM;
        int code;
        while ((code = (int) input.readBits(codeSize)) != EOI_CODE) {       // GetNextCode()
            if (code == CLEAR_CODE) {
                Arrays.fill(sequencesForCodes, null);                       // InitializeTable()
                nextAvailableEntry = 0;
                write              = ArraysExt.EMPTY_BYTE;
                codeSize           = MIN_CODE_SIZE;
                maximumIndex       = (1 << MIN_CODE_SIZE) - OFFSET_TO_MAXIMUM;
                /*
                 * We should not have consecutive clear codes, but it is easy to check for safety.
                 * The first valid code after `CLEAR_CODE` shall be a byte.
                 */
                do code = (int) input.readBits(MIN_CODE_SIZE);              // GetNextCode()
                while (code == CLEAR_CODE);
                if (code == EOI_CODE) break;
                if ((code & ~0xFF) != 0) {
                    throw unexpectedData();
                }
                /*
                 * The code to add to the table is a single byte in the [0 … 255] range.
                 * Those codes are already implicitly in the table, so we need to skip
                 * the `sequencesForCodes[nextAvailableEntry]` update. Following lines
                 * reproduce the lines at the end of the loop for a single char.
                 */
                write = new byte[] {(byte) code};       // OldCode = Code
                if (target.hasRemaining()) {
                    target.put((byte) code);            // WriteString(StringFromCode(Code))
                } else {
                    pending = write;
                    break;
                }
            } else {
                /*
                 * Case for all codes after the first code following `CLEAR_CODE`.
                 * All those cases are going to add a new entry in the table.
                 */
                final int n = write.length;
                final byte[] addToTable = Arrays.copyOf(write, n+1);
                if ((code & ~0xFF) == 0) {                   // Codes [0 … 255] are implicitly in the table.
                    addToTable[n] = (byte) code;             // StringFromCode(OldCode) + FirstChar(StringFromCode(Code))
                    write = new byte[] {(byte) code};
                } else {
                    final byte[] sequenceForCode = sequencesForCodes[code - FIRST_ADAPTATIVE_CODE];
                    if (sequenceForCode != null) {           // if (IsInTable(Code))
                        addToTable[n] = sequenceForCode[0];  // StringFromCode(OldCode) + FirstChar(StringFromCode(Code))
                        write = sequenceForCode;
                    } else if (n != 0) {
                        addToTable[n] = write[0];            // StringFromCode(OldCode) + FirstChar(StringFromCode(OldCode))
                        write = addToTable;
                    } else {
                        throw unexpectedData();
                    }
                }
                sequencesForCodes[nextAvailableEntry] = addToTable;
                if (++nextAvailableEntry == maximumIndex) {
                    maximumIndex = (1 << ++codeSize) - OFFSET_TO_MAXIMUM;
                    if (codeSize > MAX_CODE_SIZE) {
                        throw new IOException();
                    }
                }
                /*
                 * If the sequence is too long for space available in target buffer,
                 * the writing will be deferred to next invocation of this method.
                 */
                if (write.length <= target.remaining()) {
                    target.put(write);
                } else {
                    pending = write;
                    break;
                }
            }
        }
        previousSequence = write;
        done = (code == EOI_CODE && pending != null);
        return target.position() - start;
    }

    /**
     * The exception to throw if the decompression process encounters data that it can not process.
     */
    private IOException unexpectedData() {
        return new IOException(resources().getString(Resources.Keys.CorruptedCompression_2, input.filename, "LZW"));
    }
}
