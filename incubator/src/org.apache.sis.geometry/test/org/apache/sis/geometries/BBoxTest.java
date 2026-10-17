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

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
