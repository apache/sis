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
package org.apache.sis.geometries.math;

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

        assertEquals(new Vector2D.Double(0,1), cnt.get(0));
        assertEquals(new Vector2D.Double(2,3), cnt.get(1));
        assertEquals(new Vector2D.Double(4,5), cnt.get(2));
        assertEquals(new Vector2D.Double(6,7), cnt.get(3));
        assertEquals(new Vector2D.Double(8,9), cnt.get(4));
        assertEquals(new Vector2D.Double(10,11), cnt.get(5));

        cnt.set(0, new Vector2D.Double(100,101));
        cnt.set(1, new Vector2D.Double(102,103));
        cnt.set(2, new Vector2D.Double(104,105));
        cnt.set(3, new Vector2D.Double(106,107));
        cnt.set(4, new Vector2D.Double(108,109));
        cnt.set(5, new Vector2D.Double(110,111));
        assertEquals(new Vector2D.Double(100,101), cnt.get(0));
        assertEquals(new Vector2D.Double(102,103), cnt.get(1));
        assertEquals(new Vector2D.Double(104,105), cnt.get(2));
        assertEquals(new Vector2D.Double(106,107), cnt.get(3));
        assertEquals(new Vector2D.Double(108,109), cnt.get(4));
        assertEquals(new Vector2D.Double(110,111), cnt.get(5));
        //check base arrays are modified
        assertEquals(new Vector2D.Double(100,101), array1.get(0));
        assertEquals(new Vector2D.Double(102,103), array1.get(1));
        assertEquals(new Vector2D.Double(104,105), array2.get(0));
        assertEquals(new Vector2D.Double(106,107), array2.get(1));
        assertEquals(new Vector2D.Double(108,109), array3.get(0));
        assertEquals(new Vector2D.Double(110,111), array3.get(1));
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
        assertEquals(new Vector2D.Double(0,1), cursor.samples());
        assertTrue(cursor.next());
        assertEquals(1, cursor.coordinate());
        assertEquals(new Vector2D.Double(2,3), cursor.samples());
        assertTrue(cursor.next());
        assertEquals(2, cursor.coordinate());
        assertEquals(new Vector2D.Double(4,5), cursor.samples());
        assertTrue(cursor.next());
        assertEquals(3, cursor.coordinate());
        assertEquals(new Vector2D.Double(6,7), cursor.samples());
        assertTrue(cursor.next());
        assertEquals(4, cursor.coordinate());
        assertEquals(new Vector2D.Double(8,9), cursor.samples());
        assertTrue(cursor.next());
        assertEquals(5, cursor.coordinate());
        assertEquals(new Vector2D.Double(10,11), cursor.samples());
        assertFalse(cursor.next());
    }

}
