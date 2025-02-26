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
package org.apache.sis.storage.isobmff.mpeg;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the {@link ComponentType} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class ComponentTypeTest {
    /**
     * Creates a new test case.
     */
    public ComponentTypeTest() {
    }

    /**
     * Verifies the {@link ComponentType} ordinal values.
     * Those values shall match the values specified in table 1 of ISO 23001-17:2024.
     */
    @Test
    public void verifyOrdinalValues() {
        assertEquals( 0, ComponentType.MONOCHROME.ordinal());
        assertEquals( 1, ComponentType.LUMA_Y.ordinal());
        assertEquals( 2, ComponentType.CHROMA_CB_U.ordinal());
        assertEquals( 3, ComponentType.CHROMA_CR_V.ordinal());
        assertEquals( 4, ComponentType.RED.ordinal());
        assertEquals( 5, ComponentType.GREEN.ordinal());
        assertEquals( 6, ComponentType.BLUE.ordinal());
        assertEquals( 7, ComponentType.ALPHA.ordinal());
        assertEquals( 8, ComponentType.DEPTH.ordinal());
        assertEquals( 9, ComponentType.DISPARITY.ordinal());
        assertEquals(10, ComponentType.PALETTE.ordinal());
        assertEquals(11, ComponentType.FILTER.ordinal());
        assertEquals(12, ComponentType.PADDED.ordinal());
        assertEquals(13, ComponentType.CYAN.ordinal());
        assertEquals(14, ComponentType.MAGENTA.ordinal());
        assertEquals(15, ComponentType.YELLOW.ordinal());
        assertEquals(16, ComponentType.KEY.ordinal());
    }
}
