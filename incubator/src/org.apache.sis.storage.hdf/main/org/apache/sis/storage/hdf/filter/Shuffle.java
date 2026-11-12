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
package org.apache.sis.storage.hdf.filter;

/**
 * This filter is in reality something like interleaved to band sample model.
 * it reorders bytes.
 * <p>
 * Shuffling groups together the bytes having the same rank within an element :
 * all the first bytes, then all the second bytes, and so on. It compresses
 * better because bytes of equal significance correlate. Un-shuffling puts the
 * elements back together.
 * <p>
 * Trailing bytes that do not fill a whole element take no part in the reordering
 * and are copied as they are.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Shuffle implements Filter {

    private final int dataSize;

    /**
     * Creates a shuffle filter for elements of the given size.
     *
     * @param dataSize element size in bytes, values lower than 2 disable the filter
     */
    public Shuffle(int dataSize) {
        this.dataSize = dataSize;
    }

    /**
     * Group the bytes of equal rank together.
     */
    @Override
    public byte[] encode(byte[] data) {
        final int size = dataSize;
        if (size < 2) return data;
        final int length = data.length;
        final int count = length / size;
        if (count == 0) return data;

        final byte[] out = new byte[length];
        final int reordered = count * size;
        /*
        Read one byte rank at a time, with the stride of an element, and write
        the resulting plane sequentially.
        */
        for (int i = 0, dst = 0; i < size; i++) {
            for (int src = i; src < reordered; src += size, dst++) {
                out[dst] = data[src];
            }
        }
        copyTrailingBytes(data, out, reordered);
        return out;
    }

    /**
     * Put the bytes of each element back together.
     */
    @Override
    public byte[] decode(byte[] data) {
        final int size = dataSize;
        if (size < 2) return data;
        final int length = data.length;
        final int count = length / size;
        if (count == 0) return data;

        final byte[] out = new byte[length];
        /*
        One element is written per iteration, so the output is written once from
        start to end. The inputs are one sequential stream per byte rank, which
        the hardware prefetcher follows well.
        */
        switch (size) {
            case 2: {
                final int p1 = count;
                for (int x = 0, dst = 0; x < count; x++, dst += 2) {
                    out[dst    ] = data[x];
                    out[dst + 1] = data[p1 + x];
                }
                break;
            }
            case 4: {
                final int p1 = count, p2 = p1 + count, p3 = p2 + count;
                for (int x = 0, dst = 0; x < count; x++, dst += 4) {
                    out[dst    ] = data[x];
                    out[dst + 1] = data[p1 + x];
                    out[dst + 2] = data[p2 + x];
                    out[dst + 3] = data[p3 + x];
                }
                break;
            }
            case 8: {
                final int p1 = count,    p2 = p1 + count, p3 = p2 + count,
                          p4 = p3 + count, p5 = p4 + count, p6 = p5 + count,
                          p7 = p6 + count;
                for (int x = 0, dst = 0; x < count; x++, dst += 8) {
                    out[dst    ] = data[x];
                    out[dst + 1] = data[p1 + x];
                    out[dst + 2] = data[p2 + x];
                    out[dst + 3] = data[p3 + x];
                    out[dst + 4] = data[p4 + x];
                    out[dst + 5] = data[p5 + x];
                    out[dst + 6] = data[p6 + x];
                    out[dst + 7] = data[p7 + x];
                }
                break;
            }
            default: {
                /*
                Uncommon element sizes read one plane at a time, which keeps a
                single sequential input stream without needing a cursor per rank.
                */
                final int reordered = count * size;
                for (int i = 0, src = 0; i < size; i++) {
                    for (int dst = i; dst < reordered; dst += size, src++) {
                        out[dst] = data[src];
                    }
                }
                break;
            }
        }
        copyTrailingBytes(data, out, count * size);
        return out;
    }

    private static void copyTrailingBytes(byte[] data, byte[] out, int reordered) {
        if (reordered < data.length) {
            System.arraycopy(data, reordered, out, reordered, data.length - reordered);
        }
    }
}
