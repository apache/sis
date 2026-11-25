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

import org.apache.sis.maths.Vector;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link OBBox}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class OBBoxTest extends GeometryTest {

    /**
     * Creates a box of size 2√2 × 2√2 × 4 centered on (1, 2, 3), turned by 45° around the third axis.
     */
    private static OBBox box() {
        final OBBox box = new OBBox(Geometries.getUndefinedCRS(3));
        box.setCenter(Vector.of(1, 2, 3));
        box.setXAxis(Vector.of(1, 1, 0));
        box.setYAxis(Vector.of(-1, 1, 0));
        box.setZAxis(Vector.of(0, 0, 2));
        return box;
    }

    @Test
    public void testGetCentroid() {
        final Vector<?> centroid = box().getCentroid().getPosition();
        assertEquals(1, centroid.get(0), TOLERANCE);
        assertEquals(2, centroid.get(1), TOLERANCE);
        assertEquals(3, centroid.get(2), TOLERANCE);
    }

    @Test
    public void testGetArea() {
        assertEquals(2 * (8 + 2 * 8 * Math.sqrt(2)), box().getArea().getValue().doubleValue(), TOLERANCE);
    }

    /**
     * The volume is the same whether the axes are orthogonal or sheared.
     */
    @Test
    public void testGetVolume() {
        final OBBox box = box();
        assertEquals(32, box.getVolume().getValue().doubleValue(), TOLERANCE);
        box.setZAxis(Vector.of(1, 0, 2));
        assertEquals(32, box.getVolume().getValue().doubleValue(), TOLERANCE);
    }
}
