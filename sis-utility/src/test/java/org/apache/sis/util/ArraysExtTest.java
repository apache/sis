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
package org.apache.sis.util;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link ArraysExt} utility methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3 (derived from geotk-3.04)
 * @version 0.3
 * @module
 */
public final strictfp class ArraysExtTest extends TestCase {
    /**
     * Tests {@link ArraysExt#removeDuplicated(Object[])}.
     */
    @Test
    public void testRemoveDuplicated() {
        final Integer[] array = new Integer[] {2, 8, 4, 8, 1, 2, 8};
        assertArrayEquals(new Integer[] {2, 8, 4, 1},
                ArraysExt.resize(array, ArraysExt.removeDuplicated(array)));
    }

    /**
     * Tests {@link ArraysExt#reverse(int[])}.
     * The test uses an array of even length, then an array of odd length.
     */
    @Test
    public void testReverse() {
        int[] array = new int[] {2, 4, 8, 10};
        ArraysExt.reverse(array);
        assertArrayEquals(new int[] {10, 8, 4, 2}, array);

        array = new int[] {2, 4, 8, 10, 11};
        ArraysExt.reverse(array);
        assertArrayEquals(new int[] {11, 10, 8, 4, 2}, array);
    }

    /**
     * Tests {@link ArraysExt#unionOfSorted(int[], int[])}.
     */
    @Test
    public void testUnionOfSorted() {
        final int[] array1 = new int[] {2, 4, 6, 9, 12};
        final int[] array2 = new int[] {1, 2, 3, 12, 13, 18, 22};
        final int[] union = ArraysExt.unionOfSorted(array1, array2);
        assertArrayEquals(new int[] {1, 2, 3, 4, 6, 9, 12, 13, 18, 22}, union);
    }
}
