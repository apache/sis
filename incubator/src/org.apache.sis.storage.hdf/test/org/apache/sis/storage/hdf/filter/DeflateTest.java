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

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DeflateTest {

    @Test
    public void testEncode() throws IOException {
        final byte[] in = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        final Deflate filter = new Deflate();
        final byte[] out = filter.encode(in);
        Assertions.assertArrayEquals(out, new byte[]{120,-100,99,100,98,102,97,101,99,-25,-32,4,0,0,-82,0,46});
    }

    @Test
    public void testDecode() throws IOException {
        final byte[] in = new byte[]{120,-100,99,100,98,102,97,101,99,-25,-32,4,0,0,-82,0,46};
        final Deflate filter = new Deflate();
        final byte[] out = filter.decode(in);
         Assertions.assertArrayEquals(out, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
    }

}
