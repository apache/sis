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
package org.apache.sis.pending.jdk;

import java.nio.ByteBuffer;


/**
 * Place holder for some functionalities defined in a JDK more recent than Java 11.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class JDK13 {
    /**
     * Do not allow instantiation of this class.
     */
    private JDK13() {
    }

    /**
     * Place holder for {@code ByteBuffer.get(int, byte[])}.
     *
     * @param  b     the buffer from which to get bytes.
     * @param  index index from which the first byte will be read.
     * @param  dst   destination array
     */
    public static void get(final ByteBuffer b, int index, final byte[] dst) {
        get(b, index, dst, 0, dst.length);
    }

    /**
     * Place holder for {@code ByteBuffer.get(int, byte[], int, int)}.
     *
     * @param  b       the buffer from which to get bytes.
     * @param  index   index from which the first byte will be read.
     * @param  dst     destination array
     * @param  offset  offset in the array of the first byte to write.
     * @param  length  number of bytes to write.
     */
    public static void get(final ByteBuffer b, final int index, final byte[] dst, final int offset, final int length) {
        for (int i=0; i<length; i++) {
            dst[offset + i] = b.get(index + i);
        }
    }
}
