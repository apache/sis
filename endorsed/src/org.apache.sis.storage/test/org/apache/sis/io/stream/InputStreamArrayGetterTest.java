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
package org.apache.sis.io.stream;

import java.io.IOException;
import java.io.ByteArrayInputStream;

// Test dependencies
import org.junit.Test;
import static org.junit.Assert.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link InputStreamArrayGetter} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class InputStreamArrayGetterTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public InputStreamArrayGetterTest() {
    }

    /**
     * Tests the creation of a channel data input which uses directly the array.
     *
     * @throws IOException if an error occurred while creating the channel data input.
     */
    @Test
    public void testDirectChannel() throws IOException {
        final var array = new byte[20];
        for (int i=0; i<array.length; i++) {
            array[i] = (byte) ((i ^ 5772) * 37);
        }
        final int offset = 7;
        final var input  = new ByteArrayInputStream(array, offset, 9);
        final ChannelDataInput data = InputStreamArrayGetter.channel("Test", input, () -> {
            throw new AssertionError("Should not create new buffer.");
        });
        /*
         * Replace a few values in the backing array AFTER the `ChannelDataInput` creation.
         * If the data input does not wrap the array, the changes below would be unnoticed.
         */
        array[offset + 1] = 99;     // Replace   20.
        array[offset + 4] = 100;    // Replace -125.
        final byte[] expected = {23, 99, 57, 94, 100, -128};
        for (int i=0; i<expected.length; i++) {
            assertEquals(expected[i], data.readByte());
        }
    }
}
