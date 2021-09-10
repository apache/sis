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
package org.apache.sis.index.tree;

import java.lang.reflect.Field;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link PointTreeNode}. Also contains a few opportunistic tests of {@link NodeIterator}.
 *
 * @author  Chris Mattmann
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.1
 * @module
 */
public final strictfp class PointTreeNodeTest extends TestCase {
    /**
     * Verifies the value of {@link PointTree#MAXIMUM_DIMENSIONS}.
     * That value is restricted by the maximal capacity of {@code long} type
     * of {@link org.apache.sis.index.tree.NodeIterator.Cursor#quadrants}:
     * 2<sup>{@value PointTree#MAXIMUM_DIMENSIONS}</sup> ≤ {@value Long#SIZE}.
     */
    @Test
    public void verifyMaxDimension() {
        assertEquals(Long.SIZE, 1 << PointTree.MAXIMUM_DIMENSIONS);
    }

    /**
     * Tests {@link QuadTreeNode#factor(int,int)} for dimension 0 (X).
     */
    @Test
    public void testFactorX() {
        assertEquals(+0.5, QuadTreeNode.factor(QuadTreeNode.NE, 0), STRICT);
        assertEquals(-0.5, QuadTreeNode.factor(QuadTreeNode.NW, 0), STRICT);
        assertEquals(+0.5, QuadTreeNode.factor(QuadTreeNode.SE, 0), STRICT);
        assertEquals(-0.5, QuadTreeNode.factor(QuadTreeNode.SW, 0), STRICT);
    }

    /**
     * Tests {@link QuadTreeNode#factor(int,int)} for dimension 1 (Y).
     */
    @Test
    public void testFactorY() {
        assertEquals(+0.5, QuadTreeNode.factor(QuadTreeNode.NE, 1), STRICT);
        assertEquals(+0.5, QuadTreeNode.factor(QuadTreeNode.NW, 1), STRICT);
        assertEquals(-0.5, QuadTreeNode.factor(QuadTreeNode.SE, 1), STRICT);
        assertEquals(-0.5, QuadTreeNode.factor(QuadTreeNode.SW, 1), STRICT);
    }

    /**
     * Tests {@link QuadTreeNode#quadrant(double[], double[])}.
     */
    @Test
    public void testQuadrant() {
        final double[] region = new double[] {200, 300, Double.NaN, Double.NaN};
        assertEquals(QuadTreeNode.SW, QuadTreeNode.quadrant(new double[] {195, 285}, region));
        assertEquals(QuadTreeNode.NW, QuadTreeNode.quadrant(new double[] {195, 305}, region));
        assertEquals(QuadTreeNode.SE, QuadTreeNode.quadrant(new double[] {210, 280}, region));
        assertEquals(QuadTreeNode.NE, QuadTreeNode.quadrant(new double[] {215, 305}, region));
    }

    /**
     * Tests {@link QuadTreeNode#enterQuadrant(double[], int)}.
     */
    @Test
    public void testEnterQuadrant() {
        final double[] region = new double[] {200, 300, 100, 60};
        QuadTreeNode.enterQuadrant(region, QuadTreeNode.SE);
        assertArrayEquals(new double[] {225, 285, 50, 30}, region, STRICT);
    }

    /**
     * Verifies {@link org.apache.sis.index.tree.NodeIterator.Cursor#CLEAR_MASKS}.
     *
     * @throws ReflectiveOperationException if this test can not access to private field that we want to verify.
     */
    @Test
    public void verifyClearMasks() throws ReflectiveOperationException {
        final Field f = NodeIterator.class.getDeclaredClasses()[0].getDeclaredField("CLEAR_MASKS");
        f.setAccessible(true);
        final long[] masks = (long[]) f.get(null);
        assertEquals(PointTree.MAXIMUM_DIMENSIONS, masks.length);
        for (int d=0; d<PointTree.MAXIMUM_DIMENSIONS; d++) {
            long expected = 0;
            final long m = 1L << d;
            for (int i=0; i<Long.SIZE; i++) {
                if ((i & m) == 0) {
                    expected |= (1L << i);
                }
            }
            assertEquals(expected, masks[d]);
        }
    }
}
