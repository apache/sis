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
package org.apache.sis.geometries.index;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class MortonIteratorTest {

    /**
     * Test 2D indexing.
     */
    @Test
    public void testIndex2() {
        assertEquals(0b00000101L, MortonIterator.index(0b0011, 0b0000));
        assertEquals(0b01001110L, MortonIterator.index(0b1010, 0b0011));
        assertEquals(0b00110110L, MortonIterator.index(0b0110, 0b0101));
    }

    /**
     * Test 3D indexing.
     */
    @Test
    public void testIndex3() {
        assertEquals(0b100010001L, MortonIterator.index(0b001, 0b010, 0b100));
        assertEquals(0b101101101L, MortonIterator.index(0b111, 0b000, 0b111));
    }

    /**
     * Test 1 bit spacing.
     */
    @Test
    public void testSpace1() {

        { //check correct values
            assertEquals(0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L, MortonIterator.space1bit(0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L));
            assertEquals(0b01010100_01010001_01010101_01010101_00000001_01010101_01010101_00000101L, MortonIterator.space1bit(0b00000000_00000000_00000000_00000000_11101101_11111111_00011111_11110011L));
            assertEquals(0b01010101_01010101_01010101_01010101_01010101_01010101_01010101_01010101L, MortonIterator.space1bit(0b00000000_00000000_00000000_00000000_11111111_11111111_11111111_11111111L));
        }

        { //check incorrect values
            try {
                MortonIterator.space1bit(Long.MAX_VALUE);
                fail("Invalid value should fail");
            } catch (IllegalArgumentException ex) {
                //ok
            }
            try {
                MortonIterator.space1bit(-1);
                fail("Invalid value should fail");
            } catch (IllegalArgumentException ex) {
                //ok
            }
            try {
                MortonIterator.space1bit(0b00000000_00000000_00000000_00000001_11111111_11111111_11111111_11111111L);
                fail("Invalid value should fail");
            } catch (IllegalArgumentException ex) {
                //ok
            }
        }
    }

    /**
     * Test 2 bits spacing.
     */
    @Test
    public void testSpace2() {

        { //check correct values
            assertEquals(0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L, MortonIterator.space2bit(0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L));
            assertEquals(0b00010000_00001001_00100100_10010010_01001001_00100100_00010010_00001001L, MortonIterator.space2bit(0b00000000_00000000_00000000_00000000_00000000_00010011_11111111_11011011L));
            assertEquals(0b00010010_01001001_00100100_10010010_01001001_00100100_10010010_01001001L, MortonIterator.space2bit(0b00000000_00000000_00000000_00000000_00000000_00011111_11111111_11111111L));
        }

        { //check incorrect values
            try {
                MortonIterator.space2bit(Long.MAX_VALUE);
                fail("Invalid value should fail");
            } catch (IllegalArgumentException ex) {
                //ok
            }
            try {
                MortonIterator.space2bit(-1);
                fail("Invalid value should fail");
            } catch (IllegalArgumentException ex) {
                //ok
            }
            try {
                MortonIterator.space2bit(0b00000000_00000000_00000000_00000000_00000000_00111111_11111111_11111111L);
                fail("Invalid value should fail");
            } catch (IllegalArgumentException ex) {
                //ok
            }
        }
    }
}
