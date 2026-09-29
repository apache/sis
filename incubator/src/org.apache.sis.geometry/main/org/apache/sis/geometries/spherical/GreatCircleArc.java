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

import org.apache.sis.geometries.solid.Sphere;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.ReadOnly;


/**
 * A great circle arc on a sphere.
 *
 * @see https://mathworld.wolfram.com/GreatCircle.html
 * @author Johann Sorel (Geomatys)
 */
public final class GreatCircleArc {

    /**
     * base sphere.
     */
    private final Sphere sphere;
    /**
     * first arc point, as a unit direction vector from the sphere center
     */
    private final ReadOnly.Vector<?> vecA;
    /**
     * second arc point, as a unit direction vector from the sphere center
     */
    private final ReadOnly.Vector<?> vecB;

    public GreatCircleArc(Sphere sphere, ReadOnly.Vector<?> vecA, ReadOnly.Vector<?> vecB) {
        this.sphere = sphere;
        this.vecA = vecA;
        this.vecB = vecB;
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
    public ReadOnly.Vector<?> getA() {
        return vecA;
    }

    /**
     * @return second arc point, as a unit direction vector from the sphere center
     */
    public ReadOnly.Vector<?> getB() {
        return vecB;
    }

    /**
     * @return arc length, using the sphere radius
     */
    public double getLength() {
        final double cosAngle = Maths.clamp(vecA.dot(vecB),-1,1);
        return sphere.getRadius() * Math.acos(cosAngle);
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
    public ReadOnly.Vector<?> pointAt(double fraction) {
        return interpolate(vecA, vecB, fraction);
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
     * @see org.apache.sis.maths.Vector#slerp(ReadOnly.Tuple, double)
     */
    public static ReadOnly.Vector<?> interpolate(ReadOnly.Vector<?> vecA, ReadOnly.Vector<?> vecB, double fraction) {
        /*
         * Slerp already keeps the length of unit vectors, but normalizing removes the
         * rounding it leaves behind. Worth the cost because subdividing a cell feeds the
         * result back in, so the drift would otherwise pile up over the levels.
         */
        return vecA.copy().slerp(vecB, fraction).normalize();
    }

}
