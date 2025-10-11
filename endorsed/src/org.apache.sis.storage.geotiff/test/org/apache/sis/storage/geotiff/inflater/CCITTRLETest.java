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

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.lang.reflect.Field;
import org.apache.sis.io.stream.ChannelDataInput;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.test.TestCase;


/**
 * Verifies the table of words for {@link CCITTRLE} implementation of TIFF Modified Huffman Compression.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CCITTRLETest extends TestCase {
    /**
     * Step between 2 "Make-up codes" values.
     */
    private static final int MAKEUP_CODES_STEP = 64;

    /**
     * Termination codes and make-up codes for run of white color.
     * Copied verbatim from table published in TIFF specification.
     *
     * @see #BLACK_RUN_LENGTHS
     */
    private static final String WHITE_RUN_LENGTHS =
           "0 00110101\n" +
           "1 000111\n"   +
           "2 0111\n"     +
           "3 1000\n"     +
           "4 1011\n"     +
           "5 1100\n"     +
           "6 1110\n"     +
           "7 1111\n"     +
           "8 10011\n"    +
           "9 10100\n"    +
          "10 00111\n"    +
          "11 01000\n"    +
          "12 001000\n"   +
          "13 000011\n"   +
          "14 110100\n"   +
          "15 110101\n"   +
          "16 101010\n"   +
          "17 101011\n"   +
          "18 0100111\n"  +
          "19 0001100\n"  +
          "20 0001000\n"  +
          "21 0010111\n"  +
          "22 0000011\n"  +
          "23 0000100\n"  +
          "24 0101000\n"  +
          "25 0101011\n"  +
          "26 0010011\n"  +
          "27 0100100\n"  +
          "28 0011000\n"  +
          "29 00000010\n" +
          "30 00000011\n" +
          "31 00011010\n" +
          "32 00011011\n" +
          "33 00010010\n" +
          "34 00010011\n" +
          "35 00010100\n" +
          "36 00010101\n" +
          "37 00010110\n" +
          "38 00010111\n" +
          "39 00101000\n" +
          "40 00101001\n" +
          "41 00101010\n" +
          "42 00101011\n" +
          "43 00101100\n" +
          "44 00101101\n" +
          "45 00000100\n" +
          "46 00000101\n" +
          "47 00001010\n" +
          "48 00001011\n" +
          "49 01010010\n" +
          "50 01010011\n" +
          "51 01010100\n" +
          "52 01010101\n" +
          "53 00100100\n" +
          "54 00100101\n" +
          "55 01011000\n" +
          "56 01011001\n" +
          "57 01011010\n" +
          "58 01011011\n" +
          "59 01001010\n" +
          "60 01001011\n" +
          "61 00110010\n" +
          "62 00110011\n" +
          "63 00110100\n" +
          "64 11011\n"    +         // First "Make-up" code.
         "128 10010\n"    +
         "192 010111\n"   +
         "256 0110111\n"  +
         "320 00110110\n" +
         "384 00110111\n" +
         "448 01100100\n" +
         "512 01100101\n" +
         "576 01101000\n" +
         "640 01100111\n" +
         "704 011001100\n" +
         "768 011001101\n" +
         "832 011010010\n" +
         "896 011010011\n" +
         "960 011010100\n" +
        "1024 011010101\n" +
        "1088 011010110\n" +
        "1152 011010111\n" +
        "1216 011011000\n" +
        "1280 011011001\n" +
        "1344 011011010\n" +
        "1408 011011011\n" +
        "1472 010011000\n" +
        "1536 010011001\n" +
        "1600 010011010\n" +
        "1664 011000\n"    +
        "1728 010011011\n" +
        "1792 00000001000\n"  +
        "1856 00000001100\n"  +
        "1920 00000001101\n"  +
        "1984 000000010010\n" +
        "2048 000000010011\n" +
        "2112 000000010100\n" +
        "2176 000000010101\n" +
        "2240 000000010110\n" +
        "2304 000000010111\n" +
        "2368 000000011100\n" +
        "2432 000000011101\n" +
        "2496 000000011110\n" +
        "2560 000000011111\n";
    //   "EOL 000000000001"

    /**
     * Termination codes and make-up codes for run of black color.
     * Copied verbatim from table published in TIFF specification.
     *
     * @see #WHITE_RUN_LENGTHS
     */
    private static final String BLACK_RUN_LENGTHS =
           "0 0000110111\n"   +
           "1 010\n"          +
           "2 11\n"           +
           "3 10\n"           +
           "4 011\n"          +
           "5 0011\n"         +
           "6 0010\n"         +
           "7 00011\n"        +
           "8 000101\n"       +
           "9 000100\n"       +
          "10 0000100\n"      +
          "11 0000101\n"      +
          "12 0000111\n"      +
          "13 00000100\n"     +
          "14 00000111\n"     +
          "15 000011000\n"    +
          "16 0000010111\n"   +
          "17 0000011000\n"   +
          "18 0000001000\n"   +
          "19 00001100111\n"  +
          "20 00001101000\n"  +
          "21 00001101100\n"  +
          "22 00000110111\n"  +
          "23 00000101000\n"  +
          "24 00000010111\n"  +
          "25 00000011000\n"  +
          "26 000011001010\n" +
          "27 000011001011\n" +
          "28 000011001100\n" +
          "29 000011001101\n" +
          "30 000001101000\n" +
          "31 000001101001\n" +
          "32 000001101010\n" +
          "33 000001101011\n" +
          "34 000011010010\n" +
          "35 000011010011\n" +
          "36 000011010100\n" +
          "37 000011010101\n" +
          "38 000011010110\n" +
          "39 000011010111\n" +
          "40 000001101100\n" +
          "41 000001101101\n" +
          "42 000011011010\n" +
          "43 000011011011\n" +
          "44 000001010100\n" +
          "45 000001010101\n" +
          "46 000001010110\n" +
          "47 000001010111\n" +
          "48 000001100100\n" +
          "49 000001100101\n" +
          "50 000001010010\n" +
          "51 000001010011\n" +
          "52 000000100100\n" +
          "53 000000110111\n" +
          "54 000000111000\n" +
          "55 000000100111\n" +
          "56 000000101000\n" +
          "57 000001011000\n" +
          "58 000001011001\n" +
          "59 000000101011\n" +
          "60 000000101100\n" +
          "61 000001011010\n" +
          "62 000001100110\n" +
          "63 000001100111\n" +
          "64 0000001111\n"   +     // First "Make-up" code.
         "128 000011001000\n" +
         "192 000011001001\n" +
         "256 000001011011\n" +
         "320 000000110011\n" +
         "384 000000110100\n" +
         "448 000000110101\n" +
         "512 0000001101100\n" +
         "576 0000001101101\n" +
         "640 0000001001010\n" +
         "704 0000001001011\n" +
         "768 0000001001100\n" +
         "832 0000001001101\n" +
         "896 0000001110010\n" +
         "960 0000001110011\n" +
        "1024 0000001110100\n" +
        "1088 0000001110101\n" +
        "1152 0000001110110\n" +
        "1216 0000001110111\n" +
        "1280 0000001010010\n" +
        "1344 0000001010011\n" +
        "1408 0000001010100\n" +
        "1472 0000001010101\n" +
        "1536 0000001011010\n" +
        "1600 0000001011011\n" +
        "1664 0000001100100\n" +
        "1728 0000001100101\n" +
        "1792 00000001000\n"   +
        "1856 00000001100\n"   +
        "1920 00000001101\n"   +
        "1984 000000010010\n"  +
        "2048 000000010011\n"  +
        "2112 000000010100\n"  +
        "2176 000000010101\n"  +
        "2240 000000010110\n"  +
        "2304 000000010111\n"  +
        "2368 000000011100\n"  +
        "2432 000000011101\n"  +
        "2496 000000011110\n"  +
        "2560 000000011111\n";
    //   "EOL 00000000000"

    /**
     * Last run length value declared in {@link #WHITE_RUN_LENGTHS} and {@link #BLACK_RUN_LENGTHS}.
     */
    private static final int LAST_RUNLENGTH = 2560;

    /**
     * A node in the tree of run lengths. A tree of nodes is built by parsing a binary string (e.g. "0000110111").
     * A node can be in only 1 of those 3 states (mutually exclusive):
     *
     * <ul>
     *   <li>A node with 1 or 2 children: one child for bit 0 and a second child for bit 1.</li>
     *   <li>A leaf with the "run length" value, stored only after we traversed all bits of a word.</li>
     * </ul>
     */
    private static final class Node {
        /**
         * Child for bits 0 and 1, or {@code null} if none.
         * If the two children are {@code null}, then this node is a leaf.
         */
        Node child0, child1;

        /**
         * Run length for this leaf in the {@link #WHITE_RUN_LENGTHS} or {@link #BLACK_RUN_LENGTHS} list.
         * Valid only if {@link #child0} and {@link #child1} are both null.
         */
        int runLength;

        /**
         * Creates an initially empty code.
         */
        Node() {
            runLength = -1;
        }

        /**
         * Traverses this node for the given bit value. New nodes are created as needed.
         * This method is invoked for each bit in a word such as "0000110111",
         * from left to right, until we reach the last bit.
         *
         * @param  bit  {@code '0'} or {@code '1'}.
         * @return child of this node for the given bit.
         */
        final Node child(final char bit) {
            assertEquals(-1, runLength, "Node shall be a leaf. Index value: ");
            Node child;
            switch (bit) {
                case '0': child = child0; break;
                case '1': child = child1; break;
                default:  throw new AssertionError(bit);
            }
            if (child == null) {
                child = new Node();
                switch (bit) {
                    case '0': child0 = child; break;
                    case '1': child1 = child; break;
                }
            }
            return child;
        }

        /**
         * Declares that this node is a leaf for the given run length.
         * This method is invoked after the last bit in a word such as "0000110111".
         */
        final void leaf(final int length) {
            assertNull(child0, "child0");
            assertNull(child1, "child1");
            assertEquals(-1, runLength, "Node shall be a leaf.");
            runLength = length;
        }

        /**
         * Stores the tree in the given {@code codes} array. The format is:
         *
         * <ul>
         *   <li>If this node is a leaf, store {@code ~runLength} (a negative value).</li>
         *   <li>Otherwise the index (positive) where to continue the tree traversal if the bit is 1.
         *       If the bit is 0, then traversal will instead continue at {@code offset + 1}.</li>
         * </ul>
         *
         * Value {@code 0} means that the child is {@code null}.
         *
         * @param  codes   where to store the tree.
         * @param  offset  index of the first element to write in the {@code codes} array.
         * @return index after the last element written in the {@code codes} array.
         *
         * @see #tree
         */
        final int toTree(final short[] codes, int offset) {
            assertEquals(0, codes[offset]);
            if (runLength >= 0) {
                assertNull(child0, "child0");
                assertNull(child1, "child1");
                assertTrue(runLength <= Short.MAX_VALUE);
                codes[offset++] = (short) ~runLength;
            } else {
                final int p0 = offset + 1;                                              // Position of child 0.
                final int p1 = (child0 != null) ? child0.toTree(codes, p0) : p0+1;      // Position of child 1
                final int p2 = (child1 != null) ? child1.toTree(codes, p1) : p1+1;      // Next free position.
                assertTrue(p1 >= 0 && p1 <= Short.MAX_VALUE);
                codes[offset] = (short) p1;
                offset = p2;
            }
            assertTrue(offset >= 0 && offset <= 0xFF);
            return offset;
        }

        /**
         * Lists the words that are unused.
         *
         * @param  word   an initially empty buffer used for building the words.
         * @param  addTo  where to add unused words.
         */
        final void printUnusedWords(final StringBuilder word, final List<String> addTo) {
            if (runLength < 0) {
                final int length = word.length();
                word.append('0');
                if (child0 != null) {
                    child0.printUnusedWords(word, addTo);
                } else {
                    addTo.add(word.toString());
                }
                word.setCharAt(length, '1');
                if (child1 != null) {
                    child1.printUnusedWords(word, addTo);
                } else {
                    addTo.add(word.toString());
                }
                word.setLength(length);
            }
        }
    }

    /**
     * Words encoded together with run lengths as described in {@link Node#toTree(short[], int)}.
     * This is the expected content of {@link CCITTRLE#WHITE_RUNLENGTH_TREE} or
     * {@link CCITTRLE#BLACK_RUNLENGTH_TREE}, depending which array is tested.
     *
     * @see Node#toTree(short[], int)
     */
    private final short[] tree;

    /**
     * Buffer where to encode all words in increasing order. The content if filled by {@link #createTree(String)}.
     * It is later used by {@link #verifyReading(short[])} for testing the decoding.
     */
    private final ByteBuffer sequenceOfAllWords;

    /**
     * Creates a new test.
     */
    public CCITTRLETest() {
        tree = new short[209];                              // Empirical value of final array length.
        sequenceOfAllWords = ByteBuffer.allocate(194);      // Capacity determined empirically.
    }

    /**
     * Fills {@link #tree} and {@link #sequenceOfAllWords} with values computed from the given table.
     * Some assertions are verified in this process, for example that there are no duplicated values.
     *
     * @param  codes  {@link #WHITE_RUN_LENGTHS} or {@link #BLACK_RUN_LENGTHS}.
     * @param  zero   the word for the run length of 0.
     */
    private void createTree(final String codes, final String zero) {
        final Node root = new Node();
        int   bits      = 0;
        int   numBits   = 0;
        int   runLength = 0;
        for (final String line : codes.split("\n")) {
            int s = line.indexOf(' ');
            assertEquals(runLength, Integer.parseInt(line.substring(0, s)), "indexToRunlength");
            /*
             * Parsing the binary value is not sufficient. The number of leading zeros is significant.
             * We parse the binary as text and create a node for each bit, including leading zeros.
             * The tree construction intentionally fails if any duplicated value is found.
             */
            String word = line.substring(++s);
            Node node = root;
            for (int p=0; p < word.length(); p++) {
                node = node.child(word.charAt(p));
            }
            node.leaf(runLength);
            /*
             * Compute the expected run length for next iteration. The increment to add depends on whether
             * the next value is a terminating code or a "make up" code. All make-up codes must be followed
             * by a termination code. We use zero for that purpose for this test.
             */
            if (runLength < CCITTRLE.TERMINATING_LIMIT) {
                assertEquals(runLength++ == 0, word.equals(zero));
            } else {
                runLength += MAKEUP_CODES_STEP;
                word += zero;
            }
            /*
             * Copy the word bits in a buffer for test by `verifyReading()` later.
             */
            for (int p=0; p < word.length(); p++) {
                bits = (bits << 1) | (word.charAt(p) - '0');
                if (++numBits >= Byte.SIZE) {
                    sequenceOfAllWords.put((byte) bits);
                    bits    = 0;
                    numBits = 0;
                }
            }
        }
        assertEquals(LAST_RUNLENGTH + MAKEUP_CODES_STEP, runLength);
        if (numBits != 0) {
            sequenceOfAllWords.put((byte) (bits << (Byte.SIZE - numBits)));
        }
        sequenceOfAllWords.flip();
        assertEquals(tree.length, root.toTree(tree, 0));
        /*
         * A run length should be allocated to all possible branches,
         * with the only exception of the prefix for EOL words.
         *
         * Prefix:     00000000
         * White EOL:  000000000001
         * Black EOL:  00000000000
         */
        final List<String> unused = new ArrayList<>(2);
        root.printUnusedWords(new StringBuilder(), unused);
        assertEquals("00000000", assertSingleton(unused));
    }

    /**
     * Decodes an encoded stream which is expected to contains all run length values in increasing order.
     * Expected values after decompression are 0, 1, 2, 3, …, 64, 128, <i>etc</i>.
     */
    private void verifyReading() throws IOException {
        final CCITTRLE decoder = new CCITTRLE(
                new ChannelDataInput("sequenceOfAllWords", null, sequenceOfAllWords, true),
                null, sequenceOfAllWords.limit());
        decoder.setInputRegion(0, sequenceOfAllWords.limit());
        int runLength = 0;
        do {
            assertEquals(decoder.getRunLength(tree), runLength);
            runLength += (runLength < CCITTRLE.TERMINATING_LIMIT) ? 1 : MAKEUP_CODES_STEP;
        } while (runLength <= LAST_RUNLENGTH);
    }

    /**
     * Compares the {@linkplain #tree} built by this class with the tree encoded in a static variable
     * of {@link CCITTRLE}. Reflection is used for allowing us to keep the static arrays private.
     *
     * @param  name  name of the {@link CCITTRLE} static variable to verify.
     */
    private void compareWithStaticArray(final String name) throws ReflectiveOperationException {
        final Field field = CCITTRLE.class.getDeclaredField(name);
        field.setAccessible(true);
        final short[] actual = (short[]) field.get(null);
        assertArrayEquals(tree, actual);
    }

    /**
     * Prints the tree. This is used only if the {@link CCITTRLE} static arrays need to be rebuilt.
     *
     * @see CCITTRLE#WHITE_RUNLENGTH_TREE
     * @see CCITTRLE#BLACK_RUNLENGTH_TREE
     */
    private void printTree() {
        final StringBuilder sb = new StringBuilder(1050);
        int lastCut = 0;
        for (int v : tree) {
            if (v < 0) {
                v = ~v;
                sb.append('~');
            }
            sb.append(v).append(',');
            if (sb.length() - lastCut > 100) {
                lastCut = sb.length();
                sb.append('\n');
            } else {
                sb.append(' ');
            }
        }
        sb.setLength(sb.length() - 2);      // Remove trailing comma.
        System.out.println(sb);
    }

    /**
     * Verifies {@link CCITTRLE#WHITE_RUNLENGTH_TREE}.
     *
     * @throws IOException should never happen since we read and write in memory.
     * @throws ReflectiveOperationException if the {@link CCITTRLE} static variable is not found.
     */
    @Test
    public void verifyWhiteRuns() throws IOException, ReflectiveOperationException {
        createTree(WHITE_RUN_LENGTHS, "00110101");
        verifyReading();
        compareWithStaticArray("WHITE_RUNLENGTH_TREE");
    }

    /**
     * Verifies {@link CCITTRLE#BLACK_RUNLENGTH_TREE}.
     *
     * @throws IOException should never happen since we read and write in memory.
     * @throws ReflectiveOperationException if the {@link CCITTRLE} static variable is not found.
     */
    @Test
    public void verifyBlackRuns() throws IOException, ReflectiveOperationException {
        createTree(BLACK_RUN_LENGTHS, "0000110111");
        verifyReading();
        compareWithStaticArray("BLACK_RUNLENGTH_TREE");
    }
}
