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
package org.apache.sis.storage.geotiff.inflater;

import java.util.Arrays;
import java.io.IOException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import org.apache.sis.storage.geotiff.base.Resources;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.event.StoreListeners;


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
     * Position of the lowest bit in an {@link #entriesForCodes} element where the offset is stored.
     * The position is chosen for leaving {@value #MAX_CODE_SIZE} bits for storing the length before
     * the offset value.
     *
     * <h4>Rational</h4>
     * Even in the worst case scenario where the same byte is always appended to the sequence,
     * the maximal length cannot exceeded the dictionary size because a {@link #CLEAR_CODE}
     * will be emitted when the dictionary is full.
     */
    private static final int LOWEST_OFFSET_BIT = MAX_CODE_SIZE;

    /**
     * The mask to apply on an {@link #entriesForCodes} element for getting the length.
     */
    private static final int LENGTH_MASK = (1 << LOWEST_OFFSET_BIT) - 1;

    /**
     * Number of bits in an offset that are always 0 and consequently do not need to be stored.
     * An intentional consequence of this restriction is that size of blocks allocated in the
     * {@link #stringsFromCode} array must be multiples of {@literal (1 << STRING_ALIGNMENT)}.
     * It makes possible to use the extra size for growing a string up to that number of bytes
     * without copying it.
     *
     * <h4>Performance note</h4>
     * Doing allocations only by blocks of 2² = 4 bytes may seem a waste of memory, but actually
     * it reduces memory usage a lot (almost a factor 4) because of the copies avoided.
     * We tried with alignment values 1, 2, 3 and found that 2 seems optimal.
     */
    private static final int STRING_ALIGNMENT = 2;

    /**
     * Mask for a bit in an {@link #entriesForCodes} element for telling whether the extra space allocated in the
     * {@link #stringsFromCode} array has already been used by another entry. If yes (1), then that space cannot
     * be used by new entry. Instead, the new entry will need to allocate a new space.
     *
     * <p>Note: {@link #newEntryNeedsAllocation(int)} implementation assumes that this bit is the sign bit.</p>
     */
    private static final int PREALLOCATED_SPACE_IS_USED_MASK = 1 << (Integer.SIZE - 1);

    /**
     * The mask to apply on an {@link #entriesForCodes} element for getting the compressed offset (before shifting).
     */
    private static final int OFFSET_MASK = (PREALLOCATED_SPACE_IS_USED_MASK - 1) & ~LENGTH_MASK;

    /**
     * The shift to apply on a compressed offset (after application of {@link #OFFSET_MASK})
     * for getting the uncompressed offset.
     */
    private static final int OFFSET_SHIFT = LOWEST_OFFSET_BIT - STRING_ALIGNMENT;

    /**
     * Extracts the number of bytes of an entry stored in the {@link #stringsFromCode} array.
     *
     * @param  element  an element of the {@link #entriesForCodes} array.
     * @return number of consecutive bytes to read in {@link #stringsFromCode} array.
     */
    private static int length(final int element) {
        return element & LENGTH_MASK;
    }

    /**
     * Extracts the index of the first byte of an entry stored in the {@link #stringsFromCode} array.
     *
     * @param  element  an element of the {@link #entriesForCodes} array.
     * @return index of the first byte to read in {@link #stringsFromCode} array.
     */
    private static int offset(final int element) {
        return (element & OFFSET_MASK) >>> OFFSET_SHIFT;
    }

    /**
     * Encodes an offset together with its length.
     */
    private static int offsetAndLength(final int offset, final int length) {
        final int element = (offset << OFFSET_SHIFT) | length;
        assert offset(element) == offset : offset;
        assert length(element) == length : length;
        return element;
    }

    /**
     * Maximal value + 1 that the offset can take. The compressed offset takes all the bits after the length,
     * minus one bit that we keep for the {@link #PREALLOCATED_SPACE_IS_USED_MASK} flag. Note that compressed
     * offsets are multiplied by {@literal 1 << STRING_ALIGNMENT} for getting the actual offset.
     */
    private static final int OFFSET_LIMIT = 1 << (Integer.SIZE - 1 - OFFSET_SHIFT);

    /**
     * A mask used for detecting when a new allocation is required.
     * If {@code (length & LENGTH_MASK_FOR_ALLOCATE) == 0} and assuming that
     * length is always incremented by 1, then a new allocation is necessary.
     */
    private static final int LENGTH_MASK_FOR_ALLOCATE = (1 << STRING_ALIGNMENT) - 1;

    /**
     * Returns {@code true} if all the space allocated for the given entry is already used.
     * This is true if at least one of the following conditions is true:
     *
     * <ul>
     *   <li>The {@link #PREALLOCATED_SPACE_IS_USED_MASK} is set, in which case value is negative.</li>
     *   <li>All the extra-space allowed by {@link #STRING_ALIGNMENT} is used, in which case the lowest
     *       bits of the length are all zero.</li>
     * </ul>
     *
     * @param  element  an element of the {@link #entriesForCodes} array.
     * @return whether all the space for that entry is already used.
     */
    private static boolean newEntryNeedsAllocation(final int element) {
        return (element & (PREALLOCATED_SPACE_IS_USED_MASK | LENGTH_MASK_FOR_ALLOCATE)) <= 0;
    }

    /**
     * Pointers to byte sequences for a code in the {@link #entriesForCodes} array.
     * Each element is a value encoded by {@link #offsetAndLength(int, int)} method.
     * Elements are decoded by {@link #offset(int)} and {@link #length(int)} methods.
     */
    private final int[] entriesForCodes;

    /**
     * Last code found in previous iteration. This is a valid index in the {@link #entriesForCodes} array.
     * A {@link #EOI_CODE} value means that the decompression is finished.
     */
    private int previousCode;

    /**
     * If some bytes could not be written in previous {@code read(…)} execution because the target buffer was full,
     * offset and length of those bytes. Otherwise 0.
     */
    private int pendingOffset, pendingLength;

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
     * Creates a new channel which will decompress data from the given input.
     * The {@link #setInputRegion(long, long)} method must be invoked after construction
     * before a reading process can start.
     *
     * @param  input      the source of data to decompress.
     * @param  listeners  object where to report warnings.
     */
    public LZW(final ChannelDataInput input, final StoreListeners listeners) {
        super(input, listeners);
        indexOfFreeEntry = FIRST_ADAPTATIVE_CODE;
        entriesForCodes  = new int[(1 << MAX_CODE_SIZE) - OFFSET_TO_MAXIMUM];
        stringsFromCode  = new byte[3 << (MAX_CODE_SIZE + STRING_ALIGNMENT)];   // Dynamically expanded if needed.
        for (int i=0; i < (1 << Byte.SIZE); i++) {
            stringsFromCode[i << STRING_ALIGNMENT] = (byte) i;
        }
    }

    /**
     * Prepares this inflater for reading a new tile or a new band of a tile.
     *
     * @param  start      stream position where to start reading.
     * @param  byteCount  number of bytes to read from the input.
     * @throws IOException if the stream cannot be seek to the given start position.
     */
    @Override
    public void setInputRegion(final long start, final long byteCount) throws IOException {
        super.setInputRegion(start, byteCount);
        clearTable();
        previousCode  = 0;
        pendingOffset = 0;
        pendingLength = 0;
    }

    /**
     * Clears the {@link #entriesForCodes} table.
     */
    private void clearTable() {
        for (int i=0; i<CLEAR_CODE; i++) {
            entriesForCodes[i] = (i << LOWEST_OFFSET_BIT) | 1;
        }
        Arrays.fill(entriesForCodes, FIRST_ADAPTATIVE_CODE, indexOfFreeEntry, 0);
        indexOfFreeEntry  = FIRST_ADAPTATIVE_CODE;
        indexOfFreeString = (1 << Byte.SIZE) << STRING_ALIGNMENT;
        codeSize          = MIN_CODE_SIZE;
    }

    /**
     * Reads {@link #codeSize} bits from the stream.
     *
     * @return the value of the next bits from the stream.
     * @throws IOException if an error occurred while reading.
     */
    public final int readNextCode() throws IOException {
        try {
            return (int) input.readBits(codeSize);
        } catch (EOFException e) {
            if (finished()) {
                listeners.warning(null, e);
                return EOI_CODE;
            } else {
                throw e;
            }
        }
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
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        int previousCode = this.previousCode;
        /*
         * If a previous invocation of this method was unable to write some data
         * because the target buffer was full, write these remaining data first.
         */
        if (pendingLength != 0) {
            final int n = Math.min(pendingLength, target.remaining());
            target.put(stringsFromCode, pendingOffset, n);
            pendingOffset += n;
            pendingLength -= n;
            if (pendingLength != 0 || previousCode == EOI_CODE) {
                return n;
            }
        } else if (previousCode == EOI_CODE || finished()) {
            return -1;
        }
        /*
         * Below is adapted from TIFF version 6 specification, section 13, LZW Decoding.
         * Comments such as "InitializeTable()" refer to methods in TIFF specification.
         */
        int entryForCode = entriesForCodes[previousCode];
        int stringOffset = offset(entryForCode);
        int stringLength = length(entryForCode);
        int maximumIndex = (1 << codeSize) - OFFSET_TO_MAXIMUM;
        int code;
        while ((code = readNextCode()) != EOI_CODE) {                       // GetNextCode()
            if (code == CLEAR_CODE) {
                clearTable();                                               // InitializeTable()
                maximumIndex = (1 << MIN_CODE_SIZE) - OFFSET_TO_MAXIMUM;
                /*
                 * We should not have consecutive clear codes, but it is easy to check for safety.
                 * The first valid code after `CLEAR_CODE` shall be a byte. If we reached the end
                 * of strip, the EOI code should be mandatory but appears to be sometimes missing.
                 */
                do code = readNextCode();                                   // GetNextCode()
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
                stringOffset = code << STRING_ALIGNMENT;
                entryForCode = entriesForCodes[code];
                assert offsetAndLength(stringOffset, 1) == entryForCode : code;
                assert Byte.toUnsignedInt(stringsFromCode[stringOffset]) == code : code;
                if (target.hasRemaining()) {
                    target.put((byte) code);                // WriteString(StringFromCode(Code))
                } else {
                    pendingOffset = stringOffset;
                    pendingLength = stringLength;
                    break;
                }
            } else {
                assert entryForCode == entriesForCodes[previousCode] : previousCode;
                assert stringOffset == offset(entryForCode) : stringOffset;
                assert stringLength == length(entryForCode) : stringLength;
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
                 *
                 * Conceptually, creating a new entry requires copying the old entry before to append a byte.
                 * However, we try to avoid some copies by pre-allocating extra spaces with each new entry.
                 * The `PREALLOCATED_SPACE_IS_USED_MASK` bit tells if we can append a byte in-place after
                 * `OldCode` (without copy).
                 */
                final boolean allocate    = newEntryNeedsAllocation(entryForCode);
                final int     newOffset   = allocate ? indexOfFreeString : stringOffset;
                final int     lastNewByte = newOffset + stringLength;
                if (allocate) {
                    indexOfFreeString = lastNewByte + (~lastNewByte & LENGTH_MASK_FOR_ALLOCATE) + 1;
                    assert (indexOfFreeString & LENGTH_MASK_FOR_ALLOCATE) == 0 : stringLength;
                    if (indexOfFreeString > stringsFromCode.length) {
                        final int capacity = Math.min(indexOfFreeString * 2, OFFSET_LIMIT);
                        if (indexOfFreeString >= capacity) {
                            throw new IOException("Dictionary overflow");
                        }
                        stringsFromCode = Arrays.copyOf(stringsFromCode, capacity);
                    }
                    System.arraycopy(stringsFromCode, stringOffset, stringsFromCode, newOffset, stringLength);
                }
                entriesForCodes[previousCode] = entryForCode | PREALLOCATED_SPACE_IS_USED_MASK;
                /*
                 * Determine the sequence of bytes to write.
                 * Pseudo-codes in TIFF specification for the 2 conditional branches:
                 *
                 *     WriteString(StringFromCode(Code))
                 *     WriteString(<the entry to be added in the table>)
                 */
                final int newLength = stringLength + 1;
                final int newEntry  = offsetAndLength(newOffset, newLength);
                entryForCode = entriesForCodes[code];
                if (entryForCode != 0) {                                        // if (IsInTable(Code))
                    stringOffset = offset(entryForCode);                        // StringFromCode(Code)
                    stringLength = length(entryForCode);
                } else {
                    stringOffset = newOffset;       // StringFromCode(OldCode) + FirstChar(StringFromCode(OldCode)
                    stringLength = newLength;
                    entryForCode = newEntry;
                    /*
                     * In well-formed LZW stream, we should have `code == indexOfFreeEntry` here.
                     * However, some invalid values are found in practice. We need `code` to refer
                     * to the entry that we add to the dictionary, otherwise inconsistencies will
                     * happen during the next iteration (when using `previousCode`).
                     */
                    code = indexOfFreeEntry;
                }
                /*
                 * Add the missing byte in the new entry. That byte is `FirstChar(StringFromCode(Code | OldCode)))`.
                 * In the case of the first branch, `Code` is the string that we are about to write. In the case of
                 * the second branch, the first characters of `OldCode` is the same as in the new entry (because of
                 * the copy done above), which is also the string that we are about to write. So at this point, the
                 * two branches can be executed by the same code.
                 */
                assert stringsFromCode[newOffset] == stringsFromCode[offset(entriesForCodes[previousCode])] : code;
                stringsFromCode[lastNewByte] = stringsFromCode[stringOffset];     // + FirstChar(StringFromCode(…))
                try {
                    entriesForCodes[indexOfFreeEntry] = newEntry;
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw (IOException) unexpectedData().initCause(e);            // Overflow 12 bit codes.
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
                final int n = Math.min(stringLength, target.remaining());
                target.put(stringsFromCode, stringOffset, n);
                if (n != stringLength) {
                    pendingOffset = stringOffset + n;
                    pendingLength = stringLength - n;
                    break;
                }
            }
            previousCode = code;            // OldCode = Code
        }
        this.previousCode = code;
        return target.position() - start;
    }

    /**
     * The exception to throw if the decompression process encounters data that it cannot process.
     */
    private IOException unexpectedData() {
        return new IOException(resources().getString(Resources.Keys.CorruptedCompression_2, input.filename, "LZW"));
    }
}
