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
package org.apache.sis.maths;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ArrayConcatenatedTest extends AbstractArrayTest {

    @Override
    protected int[] getSupportedDimensions() {
        return new int[]{1,2,3};
    }

    @Override
    protected Array create(int dim, int length) {
        final Array[] arrays = new Array[length];
        for (int i = 0; i < length; i++) {
            arrays[i] = NDArrays.of(dim, new double[dim]);
        }
        return NDArrays.concatenate(arrays);
    }

    @Test
    public void testGetSet() {
        final Array array1 = NDArrays.of(2, new double[]{0,1,2,3});
        final Array array2 = NDArrays.of(2, new double[]{4,5,6,7});
        final Array array3 = NDArrays.of(2, new double[]{8,9,10,11});
        final Array cnt = NDArrays.concatenate(array1, array2, array3);

        assertEquals(Vector.of(0.0,1.0), cnt.get(0));
        assertEquals(Vector.of(2.0,3.0), cnt.get(1));
        assertEquals(Vector.of(4.0,5.0), cnt.get(2));
        assertEquals(Vector.of(6.0,7.0), cnt.get(3));
        assertEquals(Vector.of(8.0,9.0), cnt.get(4));
        assertEquals(Vector.of(10.0,11.0), cnt.get(5));

        cnt.set(0, Vector.of(100.0,101.0));
        cnt.set(1, Vector.of(102.0,103.0));
        cnt.set(2, Vector.of(104.0,105.0));
        cnt.set(3, Vector.of(106.0,107.0));
        cnt.set(4, Vector.of(108.0,109.0));
        cnt.set(5, Vector.of(110.0,111.0));
        assertEquals(Vector.of(100.0,101.0), cnt.get(0));
        assertEquals(Vector.of(102.0,103.0), cnt.get(1));
        assertEquals(Vector.of(104.0,105.0), cnt.get(2));
        assertEquals(Vector.of(106.0,107.0), cnt.get(3));
        assertEquals(Vector.of(108.0,109.0), cnt.get(4));
        assertEquals(Vector.of(110.0,111.0), cnt.get(5));
        //check base arrays are modified
        assertEquals(Vector.of(100.0,101.0), array1.get(0));
        assertEquals(Vector.of(102.0,103.0), array1.get(1));
        assertEquals(Vector.of(104.0,105.0), array2.get(0));
        assertEquals(Vector.of(106.0,107.0), array2.get(1));
        assertEquals(Vector.of(108.0,109.0), array3.get(0));
        assertEquals(Vector.of(110.0,111.0), array3.get(1));
    }

    @Test
    public void testCursor() {

        final Array array1 = NDArrays.of(2, new double[]{0,1,2,3});
        final Array array2 = NDArrays.of(2, new double[]{4,5,6,7});
        final Array array3 = NDArrays.of(2, new double[]{8,9,10,11});
        final Array cnt = NDArrays.concatenate(array1, array2, array3);

        final Cursor cursor = cnt.cursor();
        assertTrue(cursor.next());
        assertEquals(0, cursor.coordinate());
        assertEquals(Vector.of(0.0,1.0), cursor.getSamples());
        assertTrue(cursor.next());
        assertEquals(1, cursor.coordinate());
        assertEquals(Vector.of(2.0,3.0), cursor.getSamples());
        assertTrue(cursor.next());
        assertEquals(2, cursor.coordinate());
        assertEquals(Vector.of(4.0,5.0), cursor.getSamples());
        assertTrue(cursor.next());
        assertEquals(3, cursor.coordinate());
        assertEquals(Vector.of(6.0,7.0), cursor.getSamples());
        assertTrue(cursor.next());
        assertEquals(4, cursor.coordinate());
        assertEquals(Vector.of(8.0,9.0), cursor.getSamples());
        assertTrue(cursor.next());
        assertEquals(5, cursor.coordinate());
        assertEquals(Vector.of(10.0,11.0), cursor.getSamples());
        assertFalse(cursor.next());
    }

}
