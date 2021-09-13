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

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Inflater} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class InflaterTest extends TestCase {
    /**
     * Verifies that the inflater constructor makes sure to have a chunk size
     * not greater than maximal buffer capacity.
     */
    @Test
    public void testChunkSizeLimit() {
        // With chunk size below size limit.
        Mock inflater = new Mock(131, 5, null, 1000);
        assertEquals(  1, inflater.chunksPerRow);
        assertEquals(655, inflater.elementsPerChunk);
        assertNull  (     inflater.skipAfterChunks);

        // With chunk size above size limit.
        inflater = new Mock(131, 5, null, 400);
        assertEquals(  5, inflater.chunksPerRow);
        assertEquals(131, inflater.elementsPerChunk);
        assertNull  (     inflater.skipAfterChunks);
    }

    /**
     * A dummy inflater implementation doing nothing.
     */
    private static final class Mock extends Inflater {
        Mock(int elementsPerRow, int samplesPerElement, int[] skipAfterElements, int maxChunkSize) {
            super(null, elementsPerRow, samplesPerElement, skipAfterElements, 1, maxChunkSize);
        }

        @Override public void uncompressRow() {}
        @Override public void skip(long n) {}
    }
}
