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
package org.apache.sis.geometries.operation;

import org.apache.sis.geometries.math.Maths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ShewchukPredicatesTest {

    @Test
    public void testOrient2d() {
        //counterclockwise triangle : positive
        assertEquals(1.0, ShewchukPredicates.orient2d(0,0, 1,0, 0,1), 0.0);
        //clockwise triangle : negative
        assertEquals(-1.0, ShewchukPredicates.orient2d(0,0, 0,1, 1,0), 0.0);
        //collinear points : exactly zero
        assertEquals(0.0, ShewchukPredicates.orient2d(0,0, 1,1, 2,2), 0.0);
    }

    @Test
    public void testOrient3d() {
        //a,b,c,d form a positively oriented tetrahedron
        assertEquals(-1.0, ShewchukPredicates.orient3d(0,0,0, 1,0,0, 0,1,0, 0,0,1), 0.0);
        //swapping a and b reverses the orientation
        assertEquals(1.0, ShewchukPredicates.orient3d(0,0,0, 0,1,0, 1,0,0, 0,0,1), 0.0);
        //4 coplanar points : exactly zero
        assertEquals(0.0, ShewchukPredicates.orient3d(0,0,0, 1,0,0, 0,1,0, 1,1,0), 0.0);
    }

    @Test
    public void testIncircle() {
        //a,b,c on the unit circle, counterclockwise
        final double ax=1,ay=0, bx=0,by=1, cx=-1,cy=0;
        //origin is inside the circle
        assertEquals(2.0, ShewchukPredicates.inCircle(ax,ay, bx,by, cx,cy, 0,0), 0.0);
        //(2,2) is clearly outside the circle
        assertEquals(-14.0, ShewchukPredicates.inCircle(ax,ay, bx,by, cx,cy, 2,2), 0.0);
        //(0,-1) lies exactly on the circle
        assertEquals(0.0, ShewchukPredicates.inCircle(ax,ay, bx,by, cx,cy, 0,-1), 0.0);
    }

    @Test
    public void testInsphere() {
        //a,b,c,d on the unit sphere
        final double ax= 1,ay=0,az=0,
                     bx=-1,by=0,bz=0, 
                     cx= 0,cy=1,cz=0,
                     dx= 0,dy=0,dz=1;
        //origin is inside the sphere
        assertEquals(2.0, ShewchukPredicates.inSphere(ax,ay,az, bx,by,bz, cx,cy,cz, dx,dy,dz, 0,0,0), 0.0);
        //(2,2,2) is clearly outside the sphere
        assertEquals(-22.0, ShewchukPredicates.inSphere(ax,ay,az, bx,by,bz, cx,cy,cz, dx,dy,dz, 2,2,2), 0.0);
        //(0,-1,0) lies exactly on the sphere
        assertEquals(0.0, ShewchukPredicates.inSphere(ax,ay,az, bx,by,bz, cx,cy,cz, dx,dy,dz, 0,-1,0), 0.0);
    }

    /**
     * Ill-conditioned (nearly collinear) case where the naive double-precision
     * {@link Maths#lineSideFast} formula collapses to exactly 0 (falsely reporting
     * the points as collinear), while the true signed area is not zero.
     * {@link ShewchukPredicates#orient2d} must still find the correct sign.
     */
    @Test
    public void testOrient2dRobustness() {
        final double ax = -2552049145.4853754, ay = 954889314.1911564;
        final double bx = -8744220500.533537,  by = -8807976600.675346;
        final double cx = -3827380787.3278,    cy = -1055857983.0624254;

        //naive double arithmetic wrongly concludes the points are collinear
        assertEquals(0.0, Maths.lineSideFast(ax,ay, bx,by, cx,cy), 0.0);

        //the robust predicate finds the true, small but non-zero, signed area
        assertEquals(126.54970229522473, ShewchukPredicates.orient2d(ax,ay, bx,by, cx,cy), 0.1);
        assertTrue(ShewchukPredicates.orient2d(ax,ay, bx,by, cx,cy) > 0);
    }

    /**
     * Ill-conditioned (nearly cocircular) case where the naive double-precision
     * {@link Maths#inCircleFast} formula flips sign due to cancellation and wrongly
     * reports the point as inside the circle, while it is actually outside.
     * {@link ShewchukPredicates#inCircle} must still find the correct sign.
     */
    @Test
    public void testIncircleRobustness() {
        final double ax = -20114.112322591813, ay = 85477.98024350358;
        final double bx = 25853.639318987436,  by = -11990.83860428851;
        final double cx = 57484.70593103298,   cy = -79275.3977388386;
        final double dx = -24494980.530095316, dy = -92015557.8766704;

        //naive double arithmetic wrongly concludes the point is inside the circle
        assertTrue(Maths.inCircleFast(ax,ay, bx,by, cx,cy, dx,dy));

        //the robust predicate finds the true, negative (outside) value
        assertEquals(-2243068334.102574, ShewchukPredicates.inCircle(ax,ay, bx,by, cx,cy, dx,dy), 100.0);
        assertFalse(ShewchukPredicates.inCircle(ax,ay, bx,by, cx,cy, dx,dy) > 0);
    }

}
