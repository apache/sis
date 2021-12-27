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
package org.apache.sis.internal.sql.postgis;

import java.awt.image.DataBuffer;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Band}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final strictfp class BandTest extends TestCase {
    /**
     * Verifies the values returned by {@link Band#bufferToPixelType(int)}
     * by comparing them to the values returned by {@link Band#getDataBufferType()}.
     */
    @Test
    public void testBufferToPixelType() {
        for (int dataType = DataBuffer.TYPE_BYTE; dataType <= DataBuffer.TYPE_DOUBLE; dataType++) {
            final int pixelType = Band.bufferToPixelType(dataType);
            assertEquals(dataType, new Band(pixelType).getDataBufferType());
        }
    }

    /**
     * Verifies the values returned by {@link Band#sizeToPixelType(int)}
     * by comparing them to the values returned by {@link Band#getDataTypeSize()}.
     */
    @Test
    public void testSizeToPixelType() {
        for (int size = 1; size <= Integer.SIZE; size++) {
            final int pixelType = Band.sizeToPixelType(size);
            final int sizeOfType = new Band(pixelType).getDataTypeSize();
            if (Integer.bitCount(size) == 1) {
                assertEquals(size, sizeOfType);
            } else {
                assertTrue(sizeOfType > size);
                assertTrue(sizeOfType < (size << 1));
            }
        }
    }
}
