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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link BBox}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class BBoxTest extends GeometryTest {
    /**
     * The topological dimension of a box is the number of axes of its coordinate reference
     * system, since a non-empty box occupies the whole of that space locally.
     */
    @Test
    public void testGetTopologicDimension() {
        assertEquals(2, new BBox(CRS_2D, 0,0, 10,10).getTopologicDimension());
        assertEquals(3, new BBox(CRS_3D, 0,0,0, 10,10,10).getTopologicDimension());
    }

    /**
     * The topological dimension of an empty box is −1, consistently with {@link Empty}.
     */
    @Test
    public void testGetTopologicDimensionOfEmptyBox() {
        final BBox bbox = new BBox(CRS_2D);
        assertEquals(-1, bbox.getTopologicDimension());
    }

    /**
     * The centroid of a box is its median, including across the anti-meridian.
     */
    @Test
    public void testGetCentroid() {
        final Vector<?> centroid = new BBox(CRS_3D, 0,10,100, 10,30,200).getCentroid().getPosition();
        assertEquals(  5, centroid.get(0), TOLERANCE);
        assertEquals( 20, centroid.get(1), TOLERANCE);
        assertEquals(150, centroid.get(2), TOLERANCE);
        final Vector<?> crossing = new BBox(CRS_2D, 170,0, -170,10).getCentroid().getPosition();
        assertEquals(180, Math.abs(crossing.get(0)), TOLERANCE);
        assertEquals(  5, crossing.get(1), TOLERANCE);
        assertNull(new BBox(CRS_2D).getCentroid());
    }

    /**
     * The area of a box is its interior in 2 dimensions and its six faces in 3 dimensions.
     */
    @Test
    public void testGetArea() {
        assertEquals(20,  new BBox(CRS_2D, 0,0, 4,5).getArea().getValue().doubleValue(), TOLERANCE);
        assertEquals(200, new BBox(CRS_2D, 170,0, -170,10).getArea().getValue().doubleValue(), TOLERANCE);
        assertEquals(0,   new BBox(CRS_2D).getArea().getValue().doubleValue());
        assertEquals(2 * (2*3 + 3*4 + 4*2), new BBox(CRS_3D, 0,0,0, 2,3,4).getArea().getValue().doubleValue(), TOLERANCE);
        assertEquals(2 * 2*3, new BBox(CRS_3D, 0,0,0, 2,3,0).getArea().getValue().doubleValue(), TOLERANCE);
        assertThrows(UnsupportedOperationException.class, () -> new BBox(4).getArea());
    }

    /**
     * The volume of a box is defined in 3 dimensions only.
     */
    @Test
    public void testGetVolume() {
        assertEquals(24, new BBox(CRS_3D, 0,0,0, 2,3,4).getVolume().getValue().doubleValue(), TOLERANCE);
        assertEquals(0,  new BBox(CRS_3D, 0,0,0, 2,3,0).getVolume().getValue().doubleValue());
        assertThrows(UnsupportedOperationException.class, () -> new BBox(CRS_2D, 0,0, 4,5).getVolume());
    }
}
