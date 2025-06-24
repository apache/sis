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
package org.apache.sis.geometries;

import org.apache.sis.geometries.math.TupleArrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class PrimitiveTest {

    /**
     * Test remove duplicate method.
     * In this test vertex 0 and 3 are duplicates.
     */
    @Test
    public void testRemoveDuplicatesByPosition() {

        final MeshPrimitive primitive = new MeshPrimitive.TriangleStrip();
        primitive.setPositions(TupleArrays.of(Geometries.getUndefinedCRS(3),
                0,0,0, // <- duplicate
                0,1,0,
                1,0,0,
                0,0,0, // <- duplicate
                2,0,0));
        primitive.setNormals(TupleArrays.of(3,
                1,0,0, // <- duplicate
                2,0,0,
                3,0,0,
                4,1,1, // <- duplicate, will not be preserved
                5,0,0));
        primitive.setIndex(TupleArrays.ofUnsigned(1,
                0, 1, 2, 3,4));

        primitive.removeDuplicatesByPosition();

        final int[] positions = primitive.getPositions().toArrayInt();
        final int[] normals = primitive.getNormals().toArrayInt();
        final int[] index = primitive.getIndex().toArrayInt();

        assertArrayEquals(new int[]{0,1,2,0,3}, index);
        assertArrayEquals(new int[]{0,0,0, 0,1,0, 1,0,0, 2,0,0}, positions);
        assertArrayEquals(new int[]{1,0,0, 2,0,0, 3,0,0, 5,0,0}, normals);

    }

}
