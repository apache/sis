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
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ConstantInputStreamTest {

    @Test
    public void testReadSingle() throws IOException {
        final InputStream is = ConstantInputStream.create(100, new byte[]{5});

        final byte[] array = new byte[200];
        int nb = is.read(array, 100, 26);
        Assertions.assertEquals(26, nb);
        for(int i=  0;i<100;i++) Assertions.assertEquals(0, array[i]);
        for(int i=100;i<126;i++) Assertions.assertEquals(5, array[i]);
        for(int i=126;i<200;i++) Assertions.assertEquals(0, array[i]);
    }

    @Test
    public void testReadPattern() throws IOException {

        { //test read at offset
            final InputStream is = ConstantInputStream.create(5, new byte[]{5,6,7});
            final byte[] array = new byte[200];
            int nb = is.read(array, 100, 26);
            Assertions.assertEquals(5, nb);
            for (int i =  0; i < 100; i++) Assertions.assertEquals(0, array[i]);
            Assertions.assertEquals(5, array[100]);
            Assertions.assertEquals(6, array[101]);
            Assertions.assertEquals(7, array[102]);
            Assertions.assertEquals(5, array[103]);
            Assertions.assertEquals(6, array[104]);
            for (int i = 105; i < 200; i++) Assertions.assertEquals(0, array[i]);
        }

        { //test read at offset+1
            final InputStream is = ConstantInputStream.create(5, new byte[]{5,6,7});
            final byte[] array = new byte[200];
            is.read();// displace it
            int nb = is.read(array, 100, 26);
            Assertions.assertEquals(4, nb);
            for (int i =  0; i < 100; i++) Assertions.assertEquals(0, array[i]);
            Assertions.assertEquals(6, array[100]);
            Assertions.assertEquals(7, array[101]);
            Assertions.assertEquals(5, array[102]);
            Assertions.assertEquals(6, array[103]);
            for (int i = 104; i < 200; i++) Assertions.assertEquals(0, array[i]);
        }
    }
}
