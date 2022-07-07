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
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.storage.io.ChannelDataInput;


/**
 * Inflater for values encoded with the LZW compression.
 * This compression is described in section 13 of TIFF 6 specification, "LZW Compression".
 * Each code is written using at least 9 bits and at most 12 bits.
 *
 * <h2>Legal note</h2>
 * Unisys's patent on the LZW algorithm expired in 2004.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
final class LZW extends CompressionChannel {
    /**
     * A 12 bits code meaning that we have exhausted the 4093 available codes
     * and must reset the table to the initial set of 9 bits code.
     */
    private static final int CLEAR_CODE = 256;

    /**
     * End of information. This code appears at the end of a strip.
     */
    private static final int EOI_CODE = 257;

    /**
     * First code which is not one of the predefined codes.
     */
    private static final int FIRST_ADAPTATIVE_CODE = 258;

    /**
     * For computing value of {@link #indexOfFreeEntry} when {@link #codeSize} needs to be incremented.
     * TIFF specification said that the size needs to be incremented after codes 510, 1022 and 2046 are
     * added to the {@link #entriesForCodes} table. Those values are a little bit lower than what we
     * would expect if the full integer ranges were used.
     */
    private static final int OFFSET_TO_MAXIMUM = 1;

    /**
     * Initial number of bits in a code. TIFF specification said that the size needs to be
     * incremented after codes 510, 1022 and 2046 are added to the {@link #entriesForCodes} table.
     */
    private static final int MIN_CODE_SIZE = Byte.SIZE + 1;

    /**
     * Maximum number of bits in a code, inclusive.
     */
    private static final int MAX_CODE_SIZE = 12;

    /**
     * Number of bits to read for the next code. This number starts at 9 and increases until {@value #MAX_CODE_SIZE}.
     * After {@value #MAX_CODE_SIZE} bits, a {@link #CLEAR_CODE} should occur in the stream of LZW data.
     */
    private int codeSize;

    /**
     * Position of the lowest bit in an {@link #entriesForCodes} element where the length is stored.
     * The length is extracted from an element by {@code element >>> LOWEST_LENGTH_BIT}.
     * The offset is chosen for allowing {@value #MAX_CODE_SIZE} bits for storing the length.
     *
     * <div class="note"><b>Rational:</b>
     * even in the worst case scenario where the same byte is always appended to the sequence,
     * the maximal length can not exceeded the dictionary size because a {@link #CLEAR_CODE}
     * will be emitted when the dictionary is full.</div>
     */
    private static final int LOWEST_LENGTH_BIT = Integer.SIZE - MAX_CODE_SIZE;

    /**
     * Extracts the number of bytes of an entry stored in the {@link #stringsFromCode} array.
     *
     * @param  element  an element of the {@link #entriesForCodes} array.
     * @return number of consecutive bytes to read in {@link #stringsFromCode} array.
     */
    private static int length(final int element) {
        return element >>> LOWEST_LENGTH_BIT;
    }

    /**
     * The mask to apply on an {@link #entriesForCodes} element for getting the offset.
     * The actual offset is {@code (element & OFFSET_MASK)}.
     */
    private static final int OFFSET_MASK = (1 << LOWEST_LENGTH_BIT) - 1;

    /**
     * Extracts the index of the first byte of an entry stored in the {@link #stringsFromCode} array.
     *
     * @param  element  an element of the {@link #entriesForCodes} array.
     * @return index of the first byte to read in {@link #stringsFromCode} array.
     */
    private static int offset(final int element) {
        return element & OFFSET_MASK;
    }

    /**
     * Encodes an offset together with its length.
     */
    private static int offsetAndLength(final int offset, final int length) {
        final int element = offset | (length << LOWEST_LENGTH_BIT);
        assert offset(element) == offset : offset;
        assert length(element) == length : length;
        return element;
    }

    /**
     * Pointers to byte sequences for a code in the {@link #entriesForCodes} array.
     * Each element is a value encoded by {@link #offsetAndLength(int, int)} method.
     * Elements are decoded by {@link #offset(int)} {@link #length(int)} methods.
     */
    private final int[] entriesForCodes;

    /**
     * Offset and length of the sequence of bytes associated to the code in previous iteration.
     * This is a value from {@link #entriesForCodes} array.
     */
    private int previousEntry;

    /**
     * If some bytes could not be written in previous {@code read(…)} execution because the target buffer was full,
     * offset and length of those bytes. Otherwise 0. This is a value from {@link #entriesForCodes} array.
     */
    private int pendingEntry;

    /**
     * Index of the next entry available in {@link #entriesForCodes}.
     * Shall not be lower than {@value #FIRST_ADAPTATIVE_CODE}.
     */
    private int indexOfFreeEntry;

    /**
     * Index of the next byte available in {@link #stringsFromCode}.
     * Shall not be lower than {@code 1 << Byte.SIZE}.
     */
    private int indexOfFreeString;

    /**
     * Sequences of bytes associated to codes. For a given <var>c</var> code read from the stream,
     * the first uncompressed byte is {@code stringsFromCode(offset(entriesForCodes[c]))} and the
     * number of bytes is {@code length(entriesForCodes[c])}.
     */
    private byte[] stringsFromCode;

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
        indexOfFreeEntry = FIRST_ADAPTATIVE_CODE;
        entriesForCodes  = new int [(1 << MAX_CODE_SIZE) - OFFSET_TO_MAXIMUM];
        stringsFromCode  = new byte[4 << MAX_CODE_SIZE];        // Dynamically expanded if needed.
        for (int i=0; i < (1 << Byte.SIZE); i++) {
            entriesForCodes[i] = i | (1 << LOWEST_LENGTH_BIT);
            stringsFromCode[i] = (byte) i;
        }
    }

    /**
     * Prepares this inflater for reading a new tile or a new band of a tile.
     *
     * @param  start      stream position where to start reading.
     * @param  byteCount  number of bytes to read from the input.
     * @throws IOException if the stream can not be seek to the given start position.
     */
    @Override
    public void setInputRegion(final long start, final long byteCount) throws IOException {
        super.setInputRegion(start, byteCount);
        clearTable();
        previousEntry = 0;
        pendingEntry  = 0;
        done          = false;
    }

    /**
     * Clears the {@link #entriesForCodes} table.
     */
    private void clearTable() {
        Arrays.fill(entriesForCodes, FIRST_ADAPTATIVE_CODE, indexOfFreeEntry, 0);
        indexOfFreeEntry  = FIRST_ADAPTATIVE_CODE;
        indexOfFreeString = 1 << Byte.SIZE;
        codeSize          = MIN_CODE_SIZE;
    }

    /**
     * Decompresses some bytes from the {@linkplain #input input} into the given destination buffer.
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
        if (pendingEntry != 0) {
            final int remaining    = target.remaining();
            final int stringOffset = pendingEntry  &  OFFSET_MASK;
            final int stringLength = pendingEntry >>> LOWEST_LENGTH_BIT;
            target.put(stringsFromCode, stringOffset, Math.min(stringLength, remaining));
            if (stringLength <= remaining) {
                pendingEntry = 0;
                if (done) {
                    return stringLength;
                }
            } else {
                pendingEntry = offsetAndLength(stringOffset + remaining, stringLength - remaining);
                return remaining;           // Can not write more than what we just wrote.
            }
        } else if (done |= finished()) {
            return -1;
        }
        /*
         * Below is adapted from TIFF version 6 specification, section 13, LZW Decoding.
         * Comments such as "InitializeTable()" refer to methods in TIFF specification.
         * The body is a little bit more complex because codes in [0 … 255] range are
         * handled as a special case instead of stored in the `entriesForCodes` table.
         */
        int stringOffset = offset(previousEntry);
        int stringLength = length(previousEntry);
        int maximumIndex = (1 << codeSize) - OFFSET_TO_MAXIMUM;
        int code;
        while ((code = (int) input.readBits(codeSize)) != EOI_CODE) {       // GetNextCode()
            if (code == CLEAR_CODE) {
                clearTable();       // InitializeTable()
                maximumIndex = (1 << MIN_CODE_SIZE) - OFFSET_TO_MAXIMUM;
                /*
                 * We should not have consecutive clear codes, but it is easy to check for safety.
                 * The first valid code after `CLEAR_CODE` shall be a byte. If we reached the end
                 * of strip, the EOI code should be mandatory but appears to be sometime missing.
                 */
                do code = finished() ? EOI_CODE : (int) input.readBits(MIN_CODE_SIZE);      // GetNextCode()
                while (code == CLEAR_CODE);
                if ((code & ~0xFF) != 0) {
                    if (code == EOI_CODE) break;
                    else throw unexpectedData();
                }
                /*
                 * The code to add to the table is a single byte in the [0 … 255] range.
                 * Those codes are already in the table, so there is no entry to add yet.
                 * Following lines reproduce the lines at the end of the enclosing loop,
                 * but for a single byte.
                 */
                stringLength = 1;
                stringOffset = code;                    // OldCode = Code
                if (target.hasRemaining()) {
                    target.put((byte) code);            // WriteString(StringFromCode(Code))
                } else {
                    pendingEntry = offsetAndLength(code, 1);
                    break;
                }
            } else {
                /*
                 * Cases for all codes after the first code following `CLEAR_CODE`.
                 * Those cases will add a new entry in the `stringsFromCode` table.
                 * Pseudo-codes in TIFF specification for the 2 conditional branches:
                 *
                 *     AddStringToTable(StringFromCode(OldCode) + FirstChar(StringFromCode(Code)))
                 *     AddStringToTable(StringFromCode(OldCode) + FirstChar(StringFromCode(OldCode)))
                 *
                 * Those two branches are identical except for the last argument (Code or OldCode).
                 * We do the common part now before `stringOffset` and `stringLength` are modified.
                 * The last byte will be added later, after we determined its value.
                 */
                final int newOffset   = indexOfFreeString;
                final int newLength   = stringLength + 1;
                final int lastNewByte = newOffset + stringLength;
                if (lastNewByte >= stringsFromCode.length) {
                    final int capacity = Math.min(indexOfFreeString * 2, OFFSET_MASK + 1);
                    if (lastNewByte >= capacity) {
                        throw new IOException("Dictionary overflow");
                    }
                    stringsFromCode = Arrays.copyOf(stringsFromCode, capacity);
                }
                System.arraycopy(stringsFromCode, stringOffset, stringsFromCode, newOffset, stringLength);
                /*
                 * Determine the sequence of bytes to write.
                 * Pseudo-codes in TIFF specification for the 2 conditional branches:
                 *
                 *     WriteString(StringFromCode(Code))
                 *     WriteString(<the entry to be added in the table>)
                 */
                final int entryForCode = entriesForCodes[code];
                if (entryForCode != 0) {                                        // if (IsInTable(Code))
                    stringOffset = offset(entryForCode);                        // StringFromCode(Code)
                    stringLength = length(entryForCode);
                } else {
                    stringOffset = newOffset;       // StringFromCode(OldCode) + FirstChar(StringFromCode(OldCode)
                    stringLength = newLength;
                    /*
                     * In well-formed LZW stream, we should have `code == indexOfFreeEntry` here.
                     * However some invalid values are found in practice. We need `code` to refer
                     * to the entry that we add to the dictionary, otherwise inconsistencies will
                     * happen during the next iteration (when using `OldCode`). This is implicitly
                     * ensured by not using `code` directly in next iteration, but reusing instead
                     * `stringOffset` and `stringLength`.
                     */
                }
                /*
                 * Add the missing byte in the new entry. That byte is `FirstChar(StringFromCode(Code | OldCode)))`.
                 * In the case of the first branch, `Code` is the string that we are about to write. In the case of
                 * the second branch, the first characters of `OldCode` is the same as in the new entry (because of
                 * the copy done above), which is also the string that we are about to write. So at this point, the
                 * two branches can be executed by the same code.
                 */
                stringsFromCode[lastNewByte] = stringsFromCode[stringOffset];     // + FirstChar(StringFromCode(…))
                indexOfFreeString = lastNewByte + 1;
                try {
                    entriesForCodes[indexOfFreeEntry] = offsetAndLength(newOffset, newLength);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw (IOException) unexpectedData().initCause(e);
                }
                if (++indexOfFreeEntry == maximumIndex) {
                    if (codeSize < MAX_CODE_SIZE) {
                        maximumIndex = (1 << ++codeSize) - OFFSET_TO_MAXIMUM;
                    } else {
                        /*
                         * Incrementing the size to 13 bits is an error because the TIFF specification
                         * limits the size to 12 bits, but some files encode an EOI_CODE or CLEAR_CODE
                         * immediately after this code. If this is not the case, we will have an index
                         * out of bounds exception in the next iteration, which is caught above.
                         */
                        // Opportunistic check if the size allocated in constructor is right.
                        assert indexOfFreeEntry == entriesForCodes.length : indexOfFreeEntry;
                    }
                }
                /*
                 * If the sequence is too long for space available in target buffer,
                 * the writing will be deferred to next invocation of this method.
                 */
                if (stringLength <= target.remaining()) {
                    target.put(stringsFromCode, stringOffset, stringLength);
                } else {
                    pendingEntry = offsetAndLength(stringOffset, stringLength);
                    break;
                }
            }
        }
        previousEntry = offsetAndLength(stringOffset, stringLength);
        done = (code == EOI_CODE);
        return target.position() - start;
    }

    /**
     * The exception to throw if the decompression process encounters data that it can not process.
     */
    private IOException unexpectedData() {
        return new IOException(resources().getString(Resources.Keys.CorruptedCompression_2, input.filename, "LZW"));
    }
}
