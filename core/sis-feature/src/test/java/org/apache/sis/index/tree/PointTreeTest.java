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

import java.util.Set;
import java.util.HashSet;
import java.util.Random;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assert.assertSetEquals;


/**
 * Tests {@link PointTree}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@DependsOn(PointTreeNode.class)
public final strictfp class PointTreeTest extends TestCase {
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
    private PointTree<Element> tree;

    /**
     * All data added to the {@link #tree}, for comparison purpose.
     */
    private Set<Element> data;

    /**
     * The elements to be added in the {@link PointTree} to test.
     * This element extends {@link DirectPosition2D} for convenience, but this is not a requirement.
     * The point is unmodifiable; attempt to modify a coordinate will cause the test to fail.
     */
    @SuppressWarnings("serial")
    private static final class Element extends DirectPosition2D {
        /** Creates a new element with random coordinates. */
        Element(final Random random) {
            x = random.nextInt(XMAX - XMIN) + XMIN;
            y = random.nextInt(YMAX - YMIN) + YMIN;
        }

        @Override public double getX()     {return x;}
        @Override public double getY()     {return y;}
        @Override public String toString() {return "P(" + x + ", " + y + ')';}

        @Override public void setLocation(double x, double y) {
            fail("Location should not be modified.");
        }

        @Override public DirectPosition2D clone() {
            fail("Location should not be cloned.");
            return super.clone();
        }
    }

    /**
     * Creates a tree filled with random values.
     */
    private void createTree() {
        final Envelope2D region = new Envelope2D();
        region.x      = XMIN;
        region.y      = YMIN;
        region.width  = XMAX - XMIN;
        region.height = YMAX - YMIN;
        random = TestUtilities.createRandomNumberGenerator();
        tree = new PointTree<>(Element.class, region, Element::getCoordinate, 5);
        int count = random.nextInt(100) + 200;
        data = new HashSet<>(Containers.hashMapCapacity(count));
        while (--count >= 0) {
            final Element e = new Element(random);
            assertEquals(data.add(e), tree.add(e));
            assertEquals(data.size(), tree.size());
        }
        assertSetEquals(data, tree);
    }

    /**
     * Tests {@link PointTree#queryByBoundingBox(Envelope)} with random coordinates.
     * This method performs some searches in random regions and compare the results
     * against searches performed by raw force.
     */
    @Test
    public void testQueryByBoundingBox() {
        createTree();
        final Set<Element> expected = new HashSet<>();
        final Envelope2D region = new Envelope2D();
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
