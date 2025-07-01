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
package org.apache.sis.geometries.mesh;

import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MeshPrimitiveIndexes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * In those tests created index may change because of hash maps and set sorting.
 *
 * @author Johann Sorel (Geomatys)
 */
public class PrimitiveIndexesTest {

    /**
     * Check the solution creation methods generated the expected solutions.
     */
    @Test
    public void testCreateSolutions() {

        //case where a winding extra triangle is needed
        final int[] s1 = new int[]{0,1,2};
        final int[] s2 = new int[]{3,4,5};
        final int[] s3 = new int[]{6,7,8};

        final List<int[]> solutions1 = createSolutions(s1, s2, s3);
        assertEquals(6, solutions1.size());
        assertArrayEquals(new int[]{0,1,2, 2,2,3, 3,4,5, 5,5,6, 6,7,8}, solutions1.get(0));
        assertArrayEquals(new int[]{0,1,2, 2,2,6, 6,7,8, 8,8,3, 3,4,5}, solutions1.get(1));
        assertArrayEquals(new int[]{3,4,5, 5,5,0, 0,1,2, 2,2,6, 6,7,8}, solutions1.get(2));
        assertArrayEquals(new int[]{3,4,5, 5,5,6, 6,7,8, 8,8,0, 0,1,2}, solutions1.get(3));
        assertArrayEquals(new int[]{6,7,8, 8,8,0, 0,1,2, 2,2,3, 3,4,5}, solutions1.get(4));
        assertArrayEquals(new int[]{6,7,8, 8,8,3, 3,4,5, 5,5,0, 0,1,2}, solutions1.get(5));


        //case where a winding extra triangle is not needed
        final int[] s4 = new int[]{0,1,2,3};
        final int[] s5 = new int[]{4,5,6,7};

        final List<int[]> solutions2 = createSolutions(s4, s5);
        assertEquals(2, solutions2.size());
        assertArrayEquals(new int[]{0,1,2,3, 3,4, 4,5,6,7}, solutions2.get(0));
        assertArrayEquals(new int[]{4,5,6,7, 7,0, 0,1,2,3}, solutions2.get(1));
    }

    /**
     * Single triangle sanity test case.
     *
     *   1
     *   +
     *   |\
     *   | \
     *   +--+
     *   0  2
     *
     *   possible solutions : [0,1,2] or [1,2,0] or [2,0,1]
     */
    @Test
    public void testToStripSingleTriangle() {

        final TupleArray index = TupleArrays.of(1, 0, 1, 2);

        final int[] ts = MeshPrimitiveIndexes.toTriangleStrip(index, MeshPrimitive.Type.TRIANGLES).toArrayInt();

        assertTrue(matchAny(ts,
                new int[]{0,1,2},
                new int[]{1,2,0},
                new int[]{2,0,1}
                ));
    }

    /**
     * Simplest case with two triangles already in strip order.
     * triangles are in clockwise direction.
     *
     *   1  3
     *   +--+
     *   |\ |
     *   | \|
     *   +--+
     *   0  2
     *
     *   possible solutions : [0,1,2,3] or [3,2,1,0]
     */
    @Test
    public void testToStripTwoNeighorTriangles() {

        final TupleArray index = TupleArrays.of(1,
                0, 1, 2,
                2, 1, 3);

        final int[] ts = MeshPrimitiveIndexes.toTriangleStrip(index, MeshPrimitive.Type.TRIANGLES).toArrayInt();

        assertTrue(matchAny(ts,
                new int[]{0,1,2,3},
                new int[]{3,2,1,0}
        ));
    }

    /**
     * Two complete separate triangles.
     *
     *   1     4
     *   +     +
     *   |\    |\
     *   | \   | \
     *   +--+  +--+
     *   0  2  3  5
     *
     *   possible solutions :
     *       [0,1,2] or [1,2,0] or [2,0,1]
     *       and
     *       [3,4,5] or [4,5,3] or [5,3,4]
     *
     */
    @Test
    public void testToStripTwoSeparateTriangles() {

        final TupleArray index = TupleArrays.of(1,
                0, 1, 2,
                3, 4, 5);

        final int[] ts = MeshPrimitiveIndexes.toTriangleStrip(index, MeshPrimitive.Type.TRIANGLES).toArrayInt();

        final int[] s1_0 = new int[]{0,1,2};
        final int[] s1_1 = new int[]{1,2,0};
        final int[] s1_2 = new int[]{2,0,1};
        final int[] s2_0 = new int[]{3,4,5};
        final int[] s2_1 = new int[]{4,5,3};
        final int[] s2_2 = new int[]{5,3,4};
        assertTrue(
                matchAny(ts, createSolutions(s1_0, s2_0)) ||
                matchAny(ts, createSolutions(s1_0, s2_1)) ||
                matchAny(ts, createSolutions(s1_0, s2_2)) ||
                matchAny(ts, createSolutions(s1_1, s2_0)) ||
                matchAny(ts, createSolutions(s1_1, s2_1)) ||
                matchAny(ts, createSolutions(s1_1, s2_2)) ||
                matchAny(ts, createSolutions(s1_2, s2_0)) ||
                matchAny(ts, createSolutions(s1_2, s2_1)) ||
                matchAny(ts, createSolutions(s1_2, s2_2))
        );
    }

    /**
     * Two triangles with incompatible winding.
     * first is clockwise, the second is counter-clockwise.
     *
     *   1  3
     *   +--+
     *   |\ |
     *   | \|
     *   +--+
     *   0  2
     *
     *   possible solutions :
     *       [0,1,2] or [1,2,0] or [2,0,1]
     *       and
     *       [1,2,3] or [2,3,1] or [3,1,2]
     *
     */
    @Test
    public void testToStripTwoIncompatibleTriangles() {

        final TupleArray index = TupleArrays.of(1,
                0, 1, 2,
                1, 2, 3);

        final int[] ts = MeshPrimitiveIndexes.toTriangleStrip(index, MeshPrimitive.Type.TRIANGLES).toArrayInt();

        final int[] s1_0 = new int[]{0,1,2};
        final int[] s1_1 = new int[]{1,2,0};
        final int[] s1_2 = new int[]{2,0,1};
        final int[] s2_0 = new int[]{1,2,3};
        final int[] s2_1 = new int[]{2,3,1};
        final int[] s2_2 = new int[]{3,1,2};
        assertTrue(
                matchAny(ts, createSolutions(s1_0, s2_0)) ||
                matchAny(ts, createSolutions(s1_0, s2_1)) ||
                matchAny(ts, createSolutions(s1_0, s2_2)) ||
                matchAny(ts, createSolutions(s1_1, s2_0)) ||
                matchAny(ts, createSolutions(s1_1, s2_1)) ||
                matchAny(ts, createSolutions(s1_1, s2_2)) ||
                matchAny(ts, createSolutions(s1_2, s2_0)) ||
                matchAny(ts, createSolutions(s1_2, s2_1)) ||
                matchAny(ts, createSolutions(s1_2, s2_2))
        );
    }

    /**
     * Triangle triangles linked as a single strip.
     * triangles are in clockwise direction.
     *
     *   6  3  0  2
     *   +--+--+--+
     *   |\ |\ |\ |
     *   | \| \| \|
     *   +--+--+--+
     *   4  1  5  7
     *
     *   Because winding order must be preserved
     *   expected indexes can be :
     *   - [4,6,1,3,5,0,7,2]
     *   - [2,7,0,5,3,1,6,4]
     */
    @Test
    public void testToStripNeighborTriangles() {
        final TupleArray index = TupleArrays.of(1,
                7, 0, 2,
                5, 0, 7,
                5, 3, 0,
                1, 3, 5,
                1, 6, 3,
                4, 6, 1);

        final int[] ts = MeshPrimitiveIndexes.toTriangleStrip(index, MeshPrimitive.Type.TRIANGLES).toArrayInt();

        final int[] s0 = new int[]{4,6,1,3,5,0,7,2};
        final int[] s1 = new int[]{2,7,0,5,3,1,6,4};

        assertTrue(matchAny(ts,s0, s1));
    }

    /**
     * Triangle strips side by side must be linked with degenerated triangles.
     * triangles are in counterclockwise direction.
     *
     *   6  3  0  2
     *   +--+--+--+
     *   |\ |\ |\ |
     *   | \| \| \|
     *   +--+--+--+
     *   4  1  5  7
     *
     *   Because winding order must be preserved
     *   expected indexes are 3 strips :
     *   - [4,1,6,3] or [3,6,1,4]
     *   - [1,5,3,0] or [0,3,5,1]
     *   - [5,7,0,2] or [2,0,7,5]
     *   each connected by a degenerated triangle
     */
    @Test
    public void testToStripNeighborTrianglesDegenerate() {
        final TupleArray index = TupleArrays.of(1,
                7, 2, 0,
                5, 7, 0,
                5, 0, 3,
                1, 5, 3,
                1, 3, 6,
                4, 1, 6);

        final int[] ts = MeshPrimitiveIndexes.toTriangleStrip(index, MeshPrimitive.Type.TRIANGLES).toArrayInt();

        final int[] s1_0 = new int[]{4,1,6,3};
        final int[] s1_1 = new int[]{3,6,1,4};
        final int[] s2_0 = new int[]{1,5,3,0};
        final int[] s2_1 = new int[]{0,3,5,1};
        final int[] s3_0 = new int[]{5,7,0,2};
        final int[] s3_1 = new int[]{2,0,7,5};

        assertTrue(
                matchAny(ts,createSolutions(s1_0, s2_0, s3_0)) ||
                matchAny(ts,createSolutions(s1_0, s2_0, s3_1)) ||
                matchAny(ts,createSolutions(s1_0, s2_1, s3_0)) ||
                matchAny(ts,createSolutions(s1_0, s2_1, s3_1)) ||
                matchAny(ts,createSolutions(s1_1, s2_0, s3_0)) ||
                matchAny(ts,createSolutions(s1_1, s2_0, s3_1)) ||
                matchAny(ts,createSolutions(s1_1, s2_1, s3_0)) ||
                matchAny(ts,createSolutions(s1_1, s2_1, s3_1)
        ));
    }

    /**
     * Simplest case with two triangles strip.
     * triangles are in clockwise direction.
     *
     *   1  3
     *   +--+
     *   |\ |
     *   | \|
     *   +--+
     *   0  2
     *
     *   possible solutions : [0,1,2,3] or [3,2,1,0]
     */
    @Test
    public void testToTriangles() {

        final TupleArray index = TupleArrays.of(1,
                0, 1, 2, 3);

        final int[] ts = MeshPrimitiveIndexes.toTriangles(index, MeshPrimitive.Type.TRIANGLE_STRIP).toArrayInt();
        assertArrayEquals(new int[]{0,1,2,1,3,2}, ts);
    }

    /**
     * Simplest case with two triangles.
     * triangles are in clockwise direction.
     *
     *   1  3
     *   +--+
     *   |\ |
     *   | \|
     *   +--+
     *   0  2
     *
     * a possible solution : [0,2,1,2,3,1]
     */
    @Test
    public void testReverseTriangles() {

        final TupleArray index = TupleArrays.of(1,
                0, 1, 2,
                2, 1, 3);

        final int[] ts = MeshPrimitiveIndexes.reverseTriangles(index, MeshPrimitive.Type.TRIANGLES).toArrayInt();
        assertArrayEquals(new int[]{0,2,1,2,3,1}, ts);
    }

    /**
     * Simplest case with two triangles strip already in strip order.
     * triangles are in clockwise direction.
     *
     *   1  3
     *   +--+
     *   |\ |
     *   | \|
     *   +--+
     *   0  2
     *
     *   possible solutions : [0,1,2,3] or [3,2,1,0]
     */
    @Test
    public void testReverseTriangleStrip() {

        final TupleArray index = TupleArrays.of(1,
                0, 1, 2, 3);

        final int[] ts = MeshPrimitiveIndexes.reverseTriangles(index, MeshPrimitive.Type.TRIANGLE_STRIP).toArrayInt();
        matchAny(ts, new int[]{0,2,1,3});
    }

    private static boolean matchAny(int[] result, int[] ... solutions) {
        return matchAny(result, Arrays.asList(solutions));
    }

    private static boolean matchAny(int[] result, List<int[]> solutions) {
        for (int[] solution : solutions) {
                if (Arrays.equals(result, solution)) {
                    return true;
                }
        }
        return false;
    }

    private static List<int[]> createSolutions(int[] ... strips) {
        return createSolutions(Arrays.asList(strips));
    }

    private static List<int[]> createSolutions(List<int[]> strips) {
        final List<int[]> solutions = new ArrayList<>();

        if (strips.size() == 1) {
            solutions.add(strips.get(0));
            return solutions;
        }

        for (int i = 0, n = strips.size(); i < n; i++) {
            final int[] strip = strips.get(i);
            List<int[]> subsolutions = new ArrayList<>();
            subsolutions.addAll(strips.subList(0, i));
            subsolutions.addAll(strips.subList(i+1, strips.size()));
            subsolutions = createSolutions(subsolutions);

            for (int[] solution : subsolutions) {
                solutions.add(degeneratedStrip(strip, solution));
            }
        }

        return solutions;
    }

    /**
     * Connect each strip with a degenerated triangle.
     */
    private static int[] degeneratedStrip(int[] ... strips) {
        final List<Integer> ds = new ArrayList<>();
        for (int[] strip : strips) {
            if (!ds.isEmpty()) {
                if (ds.size() % 2 != 0) {
                    //extra triangle to preserve winding
                    ds.add(ds.get(ds.size()-1));
                }
                //add degenerated triangle
                ds.add(ds.get(ds.size()-1));
                ds.add(strip[0]);
            }
            for (int i : strip) ds.add(i);
        }
        final int[] array = new int[ds.size()];
        for (int i = 0; i < array.length; i++) array[i] = ds.get(i);
        return array;
    }

}
