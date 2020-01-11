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

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;
import java.util.function.Function;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link QuadTree}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@DependsOn(QuadTreeNodeTest.class)
public final strictfp class QuadTreeTest extends TestCase {
    /**
     * Bounds of the region where to create points. Intentionally use asymmetric bounds
     * for increasing the chances to detect bugs in node region computations.
     */
    private static final int XMIN = -1000, YMIN = -2000, XMAX = 1500, YMAX = 3000;

    /**
     * The random number generator to use for generating points and search regions.
     */
    private Random random;

    /**
     * The tree to test.
     */
    private QuadTree<Element> tree;

    /**
     * All data added to the {@link #tree}, for comparison purpose.
     */
    private List<Element> data;

    /**
     * The elements to be added in the {@link QuadTree} to test.
     * This element extends {@link Point2D} for convenience, but this is not a requirement.
     * The point is unmodifiable; attempt to modify a coordinate will cause the test to fail.
     */
    private static final class Element extends Point2D {
        /** The coordinate values. */
        private final int x, y;

        /** Creates a new element with random coordinates. */
        Element(final Random random) {
            x = random.nextInt(XMAX - XMIN) + XMIN;
            y = random.nextInt(YMAX - YMIN) + YMIN;
        }

        @Override public double getX()     {return x;}
        @Override public double getY()     {return y;}
        @Override public String toString() {return "P(" + x + ", " + y + ')';}

        @Override public void setLocation(double x, double y) {
            fail("Location shoulf not be modified.");
        }

        @Override public Object clone() {
            fail("Location should not be cloned.");
            return super.clone();
        }
    }

    /**
     * Creates a tree filled with random values.
     */
    private void createTree() {
        random = TestUtilities.createRandomNumberGenerator();
        tree = new QuadTree<>(new Rectangle(XMIN, YMIN, XMAX - XMIN, YMAX - YMIN), Function.identity(), 5);
        int count = random.nextInt(100) + 200;
        data = new ArrayList<>(count);
        while (--count >= 0) {
            final Element e = new Element(random);
            assertEquals(data.add(e), tree.add(e));
            assertEquals(data.size(), tree.size());
        }
    }

    /**
     * Tests {@link QuadTree#queryByBoundingBox(Rectangle2D)} with random coordinates.
     * This method performs some searches in random regions and compare the results
     * against searches performed by raw force.
     */
    @Test
    public void testQueryByBoundingBox() {
        createTree();
        final Set<Element> expected = new HashSet<>();
        final Rectangle2D.Double region = new Rectangle2D.Double();
        for (int i=0; i<20; i++) {
            final int xmin = random.nextInt(XMAX - XMIN) + XMIN;
            final int ymin = random.nextInt(YMAX - YMIN) + YMIN;
            final int xmax = random.nextInt(XMAX - xmin) + xmin;
            final int ymax = random.nextInt(YMAX - ymin) + ymin;
            region.x      = xmin - 0.25;
            region.y      = ymin - 0.25;
            region.width  = xmax - xmin + 0.5;
            region.height = ymax - ymin + 0.5;
            data.stream().filter(region::contains).forEach(expected::add);
            tree.queryByBoundingBox(region).forEach((p) -> {
                if (!expected.remove(p)) {
                    fail(String.format("Unexpected point in result stream: %s%n"
                            + "Search region is x: [%d … %d] and y: [%d … %d]%n",
                            p, xmin, xmax, ymin, ymax));
                }
            });
            if (!expected.isEmpty()) {
                fail(String.format("Missing points in result stream: %s%n"
                        + "Search region is x: [%d … %d] and y: [%d … %d]%n",
                        expected, xmin, xmax, ymin, ymax));
            }
        }
    }
}
