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
package org.apache.sis.geometries.spherical;

import org.apache.sis.geometries.internal.shared.DefaultSphere;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vector3D;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link GreatCircleArc}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class GreatCircleArcTest {

    private static final double TOLERANCE = 1e-12;

    private final Vector<?> a = new Vector3D.Double(1, 0, 0);
    private final Vector<?> b = new Vector3D.Double(0, 1, 0);

    /**
     * Length test.
     * The arc between (1,0,0) and (0,1,0) spans a quarter of a great circle,
     * so its length is PI/2 * radius.
     */
    @Test
    public void getLengthTest() {
        final GreatCircleArc arc = new GreatCircleArc(new DefaultSphere(3), a, b);
        assertEquals(Math.PI / 2, arc.getLength(), TOLERANCE);

        final GreatCircleArc biggerArc = new GreatCircleArc(new DefaultSphere(3, 2.0), a, b);
        assertEquals(Math.PI, biggerArc.getLength(), TOLERANCE);

        // arc between antipodal points spans half a great circle
        final GreatCircleArc halfArc = new GreatCircleArc(new DefaultSphere(3), a, new Vector3D.Double(-1, 0, 0));
        assertEquals(Math.PI, halfArc.getLength(), TOLERANCE);

        // arc between a point and itself has no length
        final GreatCircleArc emptyArc = new GreatCircleArc(new DefaultSphere(3), a, a);
        assertEquals(0, emptyArc.getLength(), TOLERANCE);
    }

    /**
     * Length of short arcs on the Earth, where the arc cosine of the dot product loses most digits.
     */
    @Test
    public void getLengthShortTest() {
        final double radius = 6371007.2;
        for (final double length : new double[] {1000, 10, 0.1}) {
            final double angle = length / radius;
            final GreatCircleArc arc = new GreatCircleArc(new DefaultSphere(3, radius), a,
                    new Vector3D.Double(Math.cos(angle), Math.sin(angle), 0));
            assertEquals(length, arc.getLength(), length * 1e-12);
        }
    }

    /**
     * Interpolation must land on the arc ends, on the arc middle, and space the points
     * evenly in angle along the way.
     */
    @Test
    public void pointAtTest() {
        final GreatCircleArc arc = new GreatCircleArc(new DefaultSphere(3), a, b);

        assertArrayEquals(a.toArrayDouble(), arc.pointAt(0).toArrayDouble(), TOLERANCE);
        assertArrayEquals(b.toArrayDouble(), arc.pointAt(1).toArrayDouble(), TOLERANCE);
        //fractions outside the range are clamped
        assertArrayEquals(a.toArrayDouble(), arc.pointAt(-0.5).toArrayDouble(), TOLERANCE);
        assertArrayEquals(b.toArrayDouble(), arc.pointAt(1.5).toArrayDouble(), TOLERANCE);

        //the middle of a quarter circle is at 45 degrees
        final double h = Math.sqrt(0.5);
        assertArrayEquals(new double[] {h, h, 0}, arc.pointAt(0.5).toArrayDouble(), TOLERANCE);

        //a third of the way is at 30 degrees, which a straight line interpolation would miss
        final double third = Math.PI / 6;
        assertArrayEquals(new double[] {Math.cos(third), Math.sin(third), 0},
                          arc.pointAt(1.0 / 3).toArrayDouble(), TOLERANCE);

        //every interpolated point must be a unit vector at the expected angle from the start
        for (int i = 0; i <= 10; i++) {
            final double fraction = i / 10.0;
            final Vector<?> point = arc.pointAt(fraction);
            assertEquals(1, point.length(), TOLERANCE, "Interpolated points must be unit vectors");
            assertEquals(fraction * Math.PI / 2, Math.acos(point.dot(a)), TOLERANCE,
                         "Points must be evenly spaced in angle");
        }
    }

    /**
     * Coincident points interpolate to themselves, and antipodal points must be rejected
     * rather than silently produce NaN : they do not define a unique great circle.
     */
    @Test
    public void pointAtDegenerateTest() {
        final GreatCircleArc coincident = new GreatCircleArc(new DefaultSphere(3), a, a);
        assertArrayEquals(a.toArrayDouble(), coincident.pointAt(0.0).toArrayDouble(), TOLERANCE);
        assertArrayEquals(a.toArrayDouble(), coincident.pointAt(0.5).toArrayDouble(), TOLERANCE);
        assertArrayEquals(a.toArrayDouble(), coincident.pointAt(1.0).toArrayDouble(), TOLERANCE);

        final GreatCircleArc antipodal = new GreatCircleArc(new DefaultSphere(3), a, new Vector3D.Double(-1, 0, 0));
        //the ends themselves are still well defined
        assertArrayEquals(a.toArrayDouble(), antipodal.pointAt(0.0).toArrayDouble(), TOLERANCE);
        assertThrows(IllegalArgumentException.class, () -> antipodal.pointAt(0.5));
        assertThrows(IllegalArgumentException.class, () -> antipodal.pointAt(0.25));
    }

    /**
     * Sampling returns the interior points at regular fractions of the arc, at most the spacing apart.
     */
    @Test
    public void sampleTest() {
        final GreatCircleArc arc = new GreatCircleArc(new DefaultSphere(3), a, b);
        final Vector<?>[] samples = arc.sample(Math.PI / 8);
        assertEquals(3, samples.length);
        for (int i = 0; i < samples.length; i++) {
            final double angle = (i + 1) * Math.PI / 8;
            assertArrayEquals(new double[] {Math.cos(angle), Math.sin(angle), 0}, samples[i].toArrayDouble(), TOLERANCE);
        }
        // a spacing longer than the arc gives no interior point
        assertEquals(0, arc.sample(Math.PI).length);
        // the spacing is in the units of the sphere radius
        assertEquals(3, new GreatCircleArc(new DefaultSphere(3, 2), a, b).sample(Math.PI / 4).length);
    }
}
