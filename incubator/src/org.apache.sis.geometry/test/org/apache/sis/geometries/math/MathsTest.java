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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class MathsTest {

    private static final double EPSILON = 1e-6;

    @Test
    public void testProjectionRatio() {

        double x1,y1,x2,y2;

        /*
          + +

          S-E
        */
        x1 = 0.0;
        y1 = 0.0;
        x2 = 1.0;
        y2 = 0.0;
        assertEquals(-7.0, Maths.projectionRatio(x1, y1, x2, y2,-7.0, 0.0), EPSILON);
        assertEquals(-2.0, Maths.projectionRatio(x1, y1, x2, y2,-2.0, 0.0), EPSILON);
        assertEquals( 0.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 0.0), EPSILON);
        assertEquals( 0.5, Maths.projectionRatio(x1, y1, x2, y2, 0.5, 0.0), EPSILON);
        assertEquals( 1.0, Maths.projectionRatio(x1, y1, x2, y2, 1.0, 0.0), EPSILON);
        assertEquals( 2.0, Maths.projectionRatio(x1, y1, x2, y2, 2.0, 0.0), EPSILON);
        assertEquals( 7.0, Maths.projectionRatio(x1, y1, x2, y2, 7.0, 0.0), EPSILON);
        /*
          E +
          |
          S +
        */
        x1 = 0.0;
        y1 = 0.0;
        x2 = 0.0;
        y2 = 1.0;
        assertEquals(-7.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0,-7.0), EPSILON);
        assertEquals(-2.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0,-2.0), EPSILON);
        assertEquals( 0.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 0.0), EPSILON);
        assertEquals( 0.5, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 0.5), EPSILON);
        assertEquals( 1.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 1.0), EPSILON);
        assertEquals( 2.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 2.0), EPSILON);
        assertEquals( 7.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 7.0), EPSILON);
        /*
          + E
           /
          S +
        */
        x1 = 0.0;
        y1 = 0.0;
        x2 = 1.0;
        y2 = 1.0;
        assertEquals(-7.0, Maths.projectionRatio(x1, y1, x2, y2,-7.0,-7.0), EPSILON);
        assertEquals(-2.0, Maths.projectionRatio(x1, y1, x2, y2,-2.0,-2.0), EPSILON);
        assertEquals( 0.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 0.0), EPSILON);
        assertEquals( 1.0, Maths.projectionRatio(x1, y1, x2, y2, 1.0, 1.0), EPSILON);
        assertEquals( 2.0, Maths.projectionRatio(x1, y1, x2, y2, 2.0, 2.0), EPSILON);
        assertEquals( 7.0, Maths.projectionRatio(x1, y1, x2, y2, 7.0, 7.0), EPSILON);
        /*
          + S
           /
          E +
        */
        x1 = 10.0;
        y1 = 15.0;
        x2 = 5.0;
        y2 = 10.0;
        assertEquals(-1.0, Maths.projectionRatio(x1, y1, x2, y2,15.0,20.0), EPSILON);
        assertEquals( 0.0, Maths.projectionRatio(x1, y1, x2, y2,10.0,15.0), EPSILON);
        assertEquals( 1.0, Maths.projectionRatio(x1, y1, x2, y2, 5.0,10.0), EPSILON);
        assertEquals( 2.0, Maths.projectionRatio(x1, y1, x2, y2, 0.0, 5.0), EPSILON);

    }

    @Test
    public void testDistanceSquare() {

        { /* Point exactly on the segment (same Y), to ensure line detection works,
             otherwise we obtain a distance of 1.4551915228366852E-11 */
            double distance = Maths.distanceSquare(
                976136.1298301395,5118355.120854909,
                976587.6962741626,5118355.120854909,
                976420.4186063276,5118355.120854909);
            assertEquals(0.0, distance, 0.0);
        }

        { /* Point exactly on the segment (same X), to ensure line detection works,
             otherwise we obtain a distance of 1.4551915228366852E-11 */
            double distance = Maths.distanceSquare(
                5118355.120854909,976136.1298301395,
                5118355.120854909,976587.6962741626,
                5118355.120854909,976420.4186063276);
            assertEquals(0.0, distance, 0.0);
        }

    }

    @Test
    public void testNormalizedByte() {
        assertEquals(-1.0, Maths.normalizedByte(Byte.MIN_VALUE), 0.0);
        assertEquals(-1.0, Maths.normalizedByte((byte)-127), 0.0);
        assertEquals(0.0, Maths.normalizedByte((byte) 0), 0.0);
        assertEquals(1.0, Maths.normalizedByte(Byte.MAX_VALUE), 0.0);
    }

    @Test
    public void testNormalizedUByte() {
        assertEquals(0.0, Maths.normalizedUByte((byte) 0), 0.0);
        assertEquals(1.0, Maths.normalizedUByte((byte) 0xFF), 0.0);
    }

    @Test
    public void testNormalizedShort() {
        assertEquals(-1.0, Maths.normalizedShort(Short.MIN_VALUE), 0.0);
        assertEquals(-1.0, Maths.normalizedShort((short)-32767), 0.0);
        assertEquals(0.0, Maths.normalizedShort((short) 0), 0.0);
        assertEquals(1.0, Maths.normalizedShort(Short.MAX_VALUE), 0.0);
    }

    @Test
    public void testNormalizedUShort() {
        assertEquals(0.0, Maths.normalizedUShort((short) 0), 0.0);
        assertEquals(1.0, Maths.normalizedUShort((short) 0xFFFF), 0.0);
    }

    @Test
    public void testNormalizedInt() {
        assertEquals(-1.0, Maths.normalizedInt(Integer.MIN_VALUE), 0.0);
        assertEquals(-1.0, Maths.normalizedInt(Integer.MIN_VALUE+1), 0.0);
        assertEquals(0.0, Maths.normalizedInt(0), 0.0);
        assertEquals(1.0, Maths.normalizedInt(Integer.MAX_VALUE), 0.0);
    }

    @Test
    public void testNormalizedUInt() {
        assertEquals(0.0, Maths.normalizedUInt(0), 0.0);
        assertEquals(1.0, Maths.normalizedUInt(0xFFFFFFFF), 0.0);
    }

    @Test
    public void testNormalizedLong() {
        assertEquals(-1.0, Maths.normalizedLong(Long.MIN_VALUE), 0.0);
        assertEquals(-1.0, Maths.normalizedLong(Long.MIN_VALUE+1), 0.0);
        assertEquals(0.0, Maths.normalizedLong(0), 0.0);
        assertEquals(1.0, Maths.normalizedLong(Long.MAX_VALUE), 0.0);
    }

    @Test
    public void testNormalizedULong() {
        assertEquals(0.0, Maths.normalizedULong(0L), 0.0);
        assertEquals(1.0, Maths.normalizedULong(~0L), 0.0);
    }

    @Test
    public void testToNormalizedByte() {
        assertEquals(Byte.MIN_VALUE+1, Maths.toNormalizedByte(-1.0));
        assertEquals(0, Maths.toNormalizedByte(0.0));
        assertEquals(Byte.MAX_VALUE, Maths.toNormalizedByte(1.0));
    }

    @Test
    public void testToNormalizedUByte() {
        assertEquals((byte) 0, Maths.toNormalizedUByte(0.0));
        assertEquals((byte) 0xFF, Maths.toNormalizedUByte(1.0));
    }

    @Test
    public void testToNormalizedShort() {
        assertEquals(Short.MIN_VALUE+1, Maths.toNormalizedShort(-1.0));
        assertEquals((short) 0, Maths.toNormalizedByte(0.0));
        assertEquals(Short.MAX_VALUE, Maths.toNormalizedShort(1.0));
    }

    @Test
    public void testToNormalizedUShort() {
        assertEquals((short) 0, Maths.toNormalizedUShort(0.0));
        assertEquals((short) 0xFFFF, Maths.toNormalizedUShort(1.0));
    }

    @Test
    public void testToNormalizedInt() {
        assertEquals(Integer.MIN_VALUE+1, Maths.toNormalizedInt(-1.0));
        assertEquals(0, Maths.toNormalizedInt(0.0));
        assertEquals(Integer.MAX_VALUE, Maths.toNormalizedInt(1.0));
    }

    @Test
    public void testToNormalizedUInt() {
        assertEquals(0, Maths.toNormalizedUInt(0.0));
        assertEquals(0xFFFFFFFF, Maths.toNormalizedUInt(1.0));
    }

    @Test
    public void testToNormalizedLong() {
        assertEquals(Long.MIN_VALUE, Maths.toNormalizedLong(-1.0));
        assertEquals(0L, Maths.toNormalizedLong(0.0));
        assertEquals(Long.MAX_VALUE, Maths.toNormalizedLong(1.0));
    }

    @Test
    public void testToNormalizedULong() {
        assertEquals(0, Maths.toNormalizedULong(0.0));
        assertEquals(Long.MIN_VALUE, Maths.toNormalizedULong(0.5));
        assertEquals(0xFFFFFFFFFFFFFFFFL, Maths.toNormalizedULong(1.0));
    }

    @Test
    public void testInTriangle() {

    }

    /**
     * A known mathematical problem.
     * TODO : find a more robust way to compute this operation.
     */
    @Test
    @Disabled
    public void testInCircle() {

        {
            final double ax = 8.64735;
            final double ay = 41.76673;
            final double bx = 8.64841;
            final double by = 41.7668;
            final double cx = 8.64642;
            final double cy = 41.76692;
            final double dx = 8.64685;
            final double dy = 41.7668;

            System.out.println(Maths.inCircle(ax, ay, bx, by, dx, dy, cx, cy));
            System.out.println(Maths.inCircle(bx, by, dx, dy, ax, ay, cx, cy));
            System.out.println(Maths.inCircle(dx, dy, ax, ay, bx, by, cx, cy));
            System.out.println(Maths.inCircle(ax, ay, dx, dy, cx, cy, bx, by));
            System.out.println(Maths.inCircle(dx, dy, cx, cy, ax, ay, bx, by));
            System.out.println(Maths.inCircle(cx, cy, ax, ay, dx, dy, bx, by));
        }

        {
            final double ax = 8.68629;
            final double ay = 41.90324;
            final double bx = 8.6874;
            final double by = 41.90363;
            final double cx = 8.6875;
            final double cy = 41.90367;
            final double dx = 8.68772;
            final double dy = 41.90366;

            System.out.println(Maths.inCircle(ax, ay, bx, by, dx, dy, cx, cy));
            System.out.println(Maths.inCircle(bx, by, dx, dy, ax, ay, cx, cy));
            System.out.println(Maths.inCircle(dx, dy, ax, ay, bx, by, cx, cy));
            System.out.println(Maths.inCircle(ax, ay, dx, dy, cx, cy, bx, by));
            System.out.println(Maths.inCircle(dx, dy, cx, cy, ax, ay, bx, by));
            System.out.println(Maths.inCircle(cx, cy, ax, ay, dx, dy, bx, by));
        }
    }

    /**
     * A known mathematical problem.
     * TODO : find a more robust way to compute this operation.
     */
    @Test
    @Disabled
    public void testIsPointInTriangle_BaryAlgo() {
        //POLYGON ((-88.4521713256836 19.713144302368164, -77.766901 -10.770846, -77.766711 -10.770966, -88.4521713256836 19.713144302368164))
        //POINT (-77.766791 -10.770866)

        assertTrue(Maths.isPointInTriangle_BaryAlgo(
                    -88.4521713256836, 19.713144302368164,
                    -77.766901, -10.770846,
                    -77.766711, -10.770966,
                    -77.766791, -10.770866
            ));
    }

    @Test
    public void testIsPointInTriangle_SideAlgo() {

        assertTrue(Maths.isPointInTriangle_SideAlgo(
                    -88.4521713256836, 19.713144302368164,
                    -77.766901, -10.770846,
                    -77.766711, -10.770966,
                    -77.766791, -10.770866
            ));
    }
}
