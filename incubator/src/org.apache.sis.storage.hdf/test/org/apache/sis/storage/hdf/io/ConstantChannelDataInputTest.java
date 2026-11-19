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
package org.apache.sis.storage.hdf.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ConstantChannelDataInputTest {

    @Test
    public void testReadSingle() throws IOException {
        final ConstantSeekableByteChannel is = new ConstantSeekableByteChannel(100, new byte[]{5});

        final byte[] array = new byte[200];
        final ByteBuffer bb = ByteBuffer.wrap(array);
        int nb = is.read(bb);
        Assertions.assertEquals(100, nb);
        for(int i=  0;i<100;i++) Assertions.assertEquals(5, array[i]);
        for(int i=100;i<200;i++) Assertions.assertEquals(0, array[i]);
    }

    @Test
    public void testReadPattern() throws IOException {

        { //test read pattern
            final ConstantSeekableByteChannel is = new ConstantSeekableByteChannel(5, new byte[]{5,6,7});
            final byte[] array = new byte[200];
            final ByteBuffer bb = ByteBuffer.wrap(array);
            int nb = is.read(bb);
            Assertions.assertEquals(5, nb);
            Assertions.assertEquals(5, array[0]);
            Assertions.assertEquals(6, array[1]);
            Assertions.assertEquals(7, array[2]);
            Assertions.assertEquals(5, array[3]);
            Assertions.assertEquals(6, array[4]);
            for (int i = 5; i < 200; i++) Assertions.assertEquals(0, array[i]);
        }

        { //test read pattern small buffer with offset
            final ConstantSeekableByteChannel is = new ConstantSeekableByteChannel(20, new byte[]{5,6,7,8,9});
            final byte[] array = new byte[5];
            final ByteBuffer bb = ByteBuffer.wrap(array);
            is.position(2);
            int nb = is.read(bb);
            Assertions.assertEquals(5, nb);
            Assertions.assertEquals(7, array[0]);
            Assertions.assertEquals(8, array[1]);
            Assertions.assertEquals(9, array[2]);
            Assertions.assertEquals(5, array[3]);
            Assertions.assertEquals(6, array[4]);
        }
    }
}
