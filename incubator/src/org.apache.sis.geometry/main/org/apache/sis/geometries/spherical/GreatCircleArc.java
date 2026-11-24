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

import org.apache.sis.geometries.surface.Sphere;
import org.apache.sis.maths.Vector;


/**
 * A great circle arc on a sphere.
 * <p>
 * The angle between the arc ends is computed once, so interpolating many points along the arc
 * costs only two sines per point.
 *
 * @see https://mathworld.wolfram.com/GreatCircle.html
 * @author Johann Sorel (Geomatys)
 */
public final class GreatCircleArc {

    /**
     * Below this sine of the arc angle, the arc is indistinguishable from the straight line joining its ends.
     */
    private static final double PARALLEL_SINE = 1E-9;

    /**
     * base sphere.
     */
    private final Sphere sphere;
    /**
     * first arc point, as a unit direction vector from the sphere center
     */
    private final Vector<?> vecA;
    /**
     * second arc point, as a unit direction vector from the sphere center
     */
    private final Vector<?> vecB;
    /**
     * Angle between the arc ends, in radians.
     */
    private final double angle;
    /**
     * Sine of {@link #angle}.
     */
    private final double sinAngle;

    public GreatCircleArc(Sphere sphere, Vector<?> vecA, Vector<?> vecB) {
        this.sphere = sphere;
        this.vecA = vecA;
        this.vecB = vecB;
        this.angle = vecA.shortestAngle(vecB);
        this.sinAngle = Math.sin(angle);
    }

    /**
     * @return the base sphere
     */
    public Sphere getSphere() {
        return sphere;
    }

    /**
     * @return first arc point, as a unit direction vector from the sphere center
     */
    public Vector<?> getA() {
        return vecA;
    }

    /**
     * @return second arc point, as a unit direction vector from the sphere center
     */
    public Vector<?> getB() {
        return vecB;
    }

    /**
     * Computes the arc length from the {@linkplain Vector#shortestAngle(Vector) shortest angle} between the arc ends,
     * which keeps full precision for short arcs.
     *
     * @return arc length, using the sphere radius
     */
    public double getLength() {
        return sphere.getRadius() * angle;
    }

    /**
     * Get the point at the given fraction of the arc, interpolated along the great circle.
     * <p>
     * A fraction of 0 returns {@linkplain #getA() A}, a fraction of 1 returns
     * {@linkplain #getB() B} and 0.5 returns the arc middle. Fractions outside
     * the [0 .. 1] range are clamped.
     *
     * @param fraction position on the arc, in range [0 .. 1]
     * @return point at given fraction, as a unit direction vector from the sphere center
     * @throws IllegalArgumentException if the two arc ends are antipodal, since they do
     *         not define a unique great circle
     */
    public Vector<?> pointAt(double fraction) {
        return interpolate(vecA, vecB, angle, sinAngle, fraction);
    }

    /**
     * Returns points along the arc, spaced evenly and at most {@code spacing} apart.
     * The arc ends are not included.
     *
     * @param spacing maximal distance between consecutive points, in the units of the sphere radius
     * @return interior points as unit direction vectors from the sphere center, possibly empty
     * @throws IllegalArgumentException if the two arc ends are antipodal
     */
    public Vector<?>[] sample(double spacing) {
        final double segments = Math.max(1, Math.ceil(getLength() / spacing));
        final Vector<?>[] samples = new Vector<?>[(int) segments - 1];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = pointAt((i + 1) / segments);
        }
        return samples;
    }

    /**
     * Spherical linear interpolation between two unit direction vectors.
     * <p>
     * The result is a unit vector on the great circle passing through both vectors, at the
     * given fraction of the angle between them. Coincident vectors interpolate to
     * themselves. Antipodal vectors are rejected : every great circle passes through both
     * of them, so there is no arc to interpolate along.
     *
     * @param vecA first point, as a unit direction vector from the sphere center
     * @param vecB second point, as a unit direction vector from the sphere center
     * @param fraction position on the arc, in range [0 .. 1]
     * @return point at given fraction, as a unit direction vector from the sphere center
     * @throws IllegalArgumentException if the two vectors are antipodal
     * @see Vector#slerp(Vector, double)
     */
    public static Vector<?> interpolate(Vector<?> vecA, Vector<?> vecB, double fraction) {
        final double angle = vecA.shortestAngle(vecB);
        return interpolate(vecA, vecB, angle, Math.sin(angle), fraction);
    }

    /**
     * Spherical linear interpolation with the angle between the two vectors already known.
     */
    private static Vector<?> interpolate(Vector<?> vecA, Vector<?> vecB, double angle, double sinAngle, double fraction) {
        if (fraction <= 0) {
            return vecA;
        } else if (fraction >= 1) {
            return vecB;
        }
        /*
         * The result is normalized to remove the rounding left by the interpolation. Worth the cost
         * because subdividing a cell feeds the result back in, so the drift would otherwise pile up.
         */
        if (sinAngle < PARALLEL_SINE) {
            if (angle > Math.PI / 2) {
                throw new IllegalArgumentException("Cannot interpolate between antipodal vectors, "
                        + "they define no unique great circle.");
            }
            // Nearly parallel vectors: the arc is the straight line joining them
            return vecA.lerp(vecB, fraction).normalize();
        }
        final double weightA = Math.sin((1 - fraction) * angle) / sinAngle;
        final double weightB = Math.sin(fraction * angle) / sinAngle;
        return vecA.combine(weightA, vecB, weightB).normalize();
    }
}
