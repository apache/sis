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
package org.apache.sis.geometries.surface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.maths.Vector;
import static org.apache.sis.geometries.surface.SphereTest.assertPosition;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests the area and the centroid of {@link Capsule}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CapsuleTest extends GeometryTest {

    private static final CoordinateReferenceSystem CRS = Geometries.getUndefinedCRS(3);

    /**
     * Relative tolerance on the measures computed by numerical integration.
     */
    static final double INTEGRATION_TOLERANCE = 1E-5;

    /**
     * Number of samples on each circle of the capsule profile.
     */
    private static final int SAMPLES = 20000;

    private static Capsule capsule(final double r1, final double r2, final double... axis) {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final Capsule capsule = f.createCapsule(f.createDataPoints(CRS, axis));
        capsule.setRadiusBottom(r1);
        capsule.setRadiusTop(r2);
        return capsule;
    }

    /**
     * Returns the area and the position along the axis of the centroid of the surface of revolution
     * of the given profile, by the midpoint rule on pieces of the profile segments.
     *
     * @param  profile  positions (<var>z</var>, ρ) along the axis and distance from the axis.
     * @return the area, the position along the axis of the centroid and the enclosed volume.
     */
    static double[] revolve(final List<double[]> profile) {
        final double step = 1E-3;
        double area = 0, moment = 0, volume = 0;
        for (int i = 1; i < profile.size(); i++) {
            final double[] a = profile.get(i - 1);
            final double[] b = profile.get(i);
            final double length = Math.hypot(b[0] - a[0], b[1] - a[1]);
            final int n = Math.max(1, (int) Math.ceil(length / step));
            for (int k = 0; k < n; k++) {
                final double t = (k + 0.5) / n;
                final double z   = a[0] + (b[0] - a[0]) * t;
                final double rho = a[1] + (b[1] - a[1]) * t;
                final double part = 2 * Math.PI * rho * length / n;
                area   += part;
                moment += part * z;
                volume += Math.PI * rho * rho * (b[0] - a[0]) / n;
            }
        }
        return new double[] {area, moment / area, volume};
    }

    /**
     * Returns the profile of a capsule: the upper convex hull of points sampled on the two circles
     * of radius {@code r1} centered at <var>z</var> = 0 and of radius {@code r2} centered at {@code d}.
     */
    private static List<double[]> profile(final double r1, final double r2, final double d) {
        final List<double[]> points = new ArrayList<>();
        for (int i = 0; i <= SAMPLES; i++) {
            final double t = Math.PI * i / SAMPLES;
            points.add(new double[] {r1 * Math.cos(t), r1 * Math.sin(t)});
            points.add(new double[] {d + r2 * Math.cos(t), r2 * Math.sin(t)});
        }
        points.sort(Comparator.<double[]>comparingDouble(p -> p[0]).thenComparingDouble(p -> p[1]));
        final List<double[]> hull = new ArrayList<>();
        for (final double[] p : points) {
            while (hull.size() >= 2) {
                final double[] a = hull.get(hull.size() - 2);
                final double[] b = hull.get(hull.size() - 1);
                if ((b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]) >= 0) {
                    hull.remove(hull.size() - 1);       // Not a right turn, so not on the upper hull.
                } else {
                    break;
                }
            }
            hull.add(p);
        }
        return hull;
    }

    /**
     * Verifies the area and the centroid of the given capsule against a numerical integration.
     */
    private static void assertIntegrated(final Capsule capsule) {
        final Vector<?> bottom = capsule.getDataPoints().getPosition(0);
        final Vector<?> top    = capsule.getDataPoints().getPosition(1);
        final double d = capsule.getHeight();
        final double[] expected = revolve(profile(capsule.getRadiusBottom(), capsule.getRadiusTop(), d));
        assertEquals(expected[0], capsule.getArea().getValue().doubleValue(), expected[0] * INTEGRATION_TOLERANCE, "area");
        assertEquals(expected[2], capsule.getEnclosedVolume().getValue().doubleValue(), expected[2] * INTEGRATION_TOLERANCE, "volume");
        final Point centroid = capsule.getCentroid();
        for (int k = 0; k < 3; k++) {
            final double e = bottom.get(k) + (top.get(k) - bottom.get(k)) * expected[1] / d;
            assertEquals(e, centroid.getPosition().get(k), d * INTEGRATION_TOLERANCE, "centroid");
        }
    }

    /**
     * A capsule of equal radii is a cylinder of height <var>d</var> between two half spheres.
     */
    @Test
    public void testEqualRadii() {
        final Capsule capsule = capsule(1, 1, 0,0,0, 0,0,2);
        assertEquals(8 * Math.PI, capsule.getArea().getValue().doubleValue(), TOLERANCE);
        assertEquals(10 * Math.PI / 3, capsule.getEnclosedVolume().getValue().doubleValue(), TOLERANCE);
        assertPosition(0, 0, 1, capsule.getCentroid().getPosition());
        assertIntegrated(capsule);
    }

    /**
     * Capsules of different radii, on axes which are not coordinate axes.
     * The expected area of the first one is 2π⋅4⋅1.5 + 2π⋅1⋅0.5 + π⋅3⋅2⋅0.75 with <var>s</var> = ½.
     */
    @Test
    public void testDifferentRadii() {
        final Capsule capsule = capsule(2, 1, 1,1,1, 1,1,3);
        assertEquals(17.5 * Math.PI, capsule.getArea().getValue().doubleValue(), TOLERANCE);
        assertIntegrated(capsule);
        assertIntegrated(capsule(1.5, 0.5, 0,0,0, 2,1,3));
        assertIntegrated(capsule(0.2, 1.4, 0,0,0, 1,-1,1));
    }

    /**
     * A capsule whose larger sphere contains the smaller one is that larger sphere.
     */
    @Test
    public void testContainedSphere() {
        final Capsule capsule = capsule(1, 3, 0,0,0, 0,0,1);
        assertEquals(36 * Math.PI, capsule.getArea().getValue().doubleValue(), TOLERANCE);
        assertEquals(36 * Math.PI, capsule.getEnclosedVolume().getValue().doubleValue(), TOLERANCE);
        assertPosition(0, 0, 1, capsule.getCentroid().getPosition());
    }

    /**
     * The measures are computed in 3 dimensions only.
     */
    @Test
    public void testNot3D() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final Capsule capsule = f.createCapsule(f.createDataPoints(Geometries.getUndefinedCRS(2), 0,0, 0,2));
        assertThrows(UnsupportedOperationException.class, capsule::getArea);
        assertThrows(UnsupportedOperationException.class, capsule::getCentroid);
        assertThrows(UnsupportedOperationException.class, capsule::getEnclosedVolume);
    }
}
