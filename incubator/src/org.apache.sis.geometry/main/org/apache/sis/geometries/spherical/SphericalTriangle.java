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
import org.apache.sis.util.ArgumentChecks;


/**
 * A triangle on a sphere.
 *
 * @see https://mathworld.wolfram.com/SphericalTriangle.html
 * @author Johann Sorel (Geomatys)
 */
public final class SphericalTriangle {

    /**
     * base sphere.
     */
    private final Sphere sphere;
    /**
     * first triangle point, as a unit direction vector from the sphere center
     */
    private final ReadOnly.Vector<?> vecA;
    /**
     * second triangle point, as a unit direction vector from the sphere center
     */
    private final ReadOnly.Vector<?> vecB;
    /**
     * third triangle point, as a unit direction vector from the sphere center
     */
    private final ReadOnly.Vector<?> vecC;

    /**
     * Create a new spherical triangle on given sphere.
     * <p>
     * Vectors A,B,C must be in CCW order (viewed from outside the sphere)
     *
     * @param sphere sphere the triangle is on
     * @param vecA first triangle point, as a unit direction vector from the sphere center
     * @param vecB second triangle point, as a unit direction vector from the sphere center
     * @param vecC third triangle point, as a unit direction vector from the sphere center
     */
    public SphericalTriangle(Sphere sphere,
            ReadOnly.Vector<?> vecA,
            ReadOnly.Vector<?> vecB,
            ReadOnly.Vector<?> vecC) {
        this.sphere = sphere;
        this.vecA = vecA;
        this.vecB = vecB;
        this.vecC = vecC;
    }

    /**
     * @return the base sphere
     */
    public Sphere getSphere() {
        return sphere;
    }

    /**
     * @return first triangle point, as a unit direction vector from the sphere center
     */
    public ReadOnly.Vector<?> getA() {
        return vecA;
    }

    /**
     * @return second triangle point, as a unit direction vector from the sphere center
     */
    public ReadOnly.Vector<?> getB() {
        return vecB;
    }

    /**
     * @return third triangle point, as a unit direction vector from the sphere center
     */
    public ReadOnly.Vector<?> getC() {
        return vecC;
    }

    /**
     * @return triangle centroid unit direction vector from the sphere center
     */
    public ReadOnly.Vector<?> getCentroidVector() {
        return vecA.copy().add(vecB).add(vecC).normalize();
    }

    /**
     * Compute the spherical excess of this triangle : the sum of its three
     * angles minus PI.
     *
     * @see <a href="https://mathworld.wolfram.com/SphericalExcess.html">Spherical Excess</a>
     * @return spherical excess in radians
     */
    public double getSphericalExcess() {
        final double cosA = Maths.clamp(vecB.dot(vecC),-1,1);
        final double cosB = Maths.clamp(vecC.dot(vecA),-1,1);
        final double cosC = Maths.clamp(vecA.dot(vecB),-1,1);
        final double sinA = Math.sqrt(1 - cosA * cosA);
        final double sinB = Math.sqrt(1 - cosB * cosB);
        final double sinC = Math.sqrt(1 - cosC * cosC);
        final double angleA = Math.acos(Maths.clamp((cosA - cosB * cosC) / (sinB * sinC),-1,1));
        final double angleB = Math.acos(Maths.clamp((cosB - cosC * cosA) / (sinC * sinA),-1,1));
        final double angleC = Math.acos(Maths.clamp((cosC - cosA * cosB) / (sinA * sinB),-1,1));
        return angleA + angleB + angleC - Math.PI;
    }

    /**
     * Compute the area of this triangle, using the sphere radius.
     *
     * @return triangle area, in the sphere radius squared units
     */
    public double getArea() {
        final double radius = sphere.getRadius();
        return getSphericalExcess() * radius * radius;
    }

    /**
     * Test if given vector is contained in this triangle with default epsilon -1e-9.
     *
     * @param vecP vector to test
     * @return true if vector is inside triangle
     */
    public boolean contains(ReadOnly.Vector<?> vecP) {
        return contains(vecP, -1e-9);
    }

    /**
     * Test if given vector is contained in this triangle.
     *
     * @param vecP vector to test
     * @param epsilon edge tolerance, should be a negative value close to zero
     * @return true if vector is inside the triangle
     */
    public boolean contains(ReadOnly.Vector<?> vecP, double epsilon) {
        return vecP.dot(vecA.cross(vecB)) >= epsilon
            && vecP.dot(vecB.cross(vecC)) >= epsilon
            && vecP.dot(vecC.cross(vecA)) >= epsilon;
    }

    /**
     * Subdivide triangle in 4 triangles perfectly overlapping this triangle.
     * The four triangles are equal in size and the 3 new vertices are at the middle of each segment.
     * The first triangle is at corner A, second at corner B, third at corner C and fourth is in the center.
     * The fourth triangle has the opposite direction.
     *
     * @return regular 4 triangle subdivision
     */
    public SphericalTriangle[] quadSubdivide() {
        return subdivide(2);
    }

    /**
     * Subdivide triangle in {@code n*n} triangles perfectly overlapping this triangle.
     * <p>
     * Each edge is divided in {@code n} equal arcs, and the triangle is cut along the
     * lines joining those divisions, following the same construction as
     * {@link #quadSubdivide() } generalized to any ratio. All returned triangles
     * keep the CCW order of this triangle.
     * <p>
     * The returned array lists the {@code n*(n+1)/2} triangles pointing the same way
     * as this triangle first, ordered by rows starting at corner A and, within a row,
     * from the A→B edge toward the A→C edge. The {@code n*(n-1)/2} triangles pointing
     * the opposite way follow, in the same row order. For {@code n = 2} this yields
     * corner A, corner B, corner C then the center triangle.
     *
     * @param n number of divisions of each edge, must be 1 or more
     * @return regular {@code n*n} triangle subdivision
     * @throws IllegalArgumentException if {@code n} is less than 1
     */
    public SphericalTriangle[] subdivide(final int n) {
        ArgumentChecks.ensureStrictlyPositive("n", n);
        if (n == 1) {
            return new SphericalTriangle[]{this};
        }
        /*
         * Build the triangular lattice of vertices. Row r, for r in [0 .. n], holds r+1
         * vertices spread on the arc going from the A→B edge to the A→C edge at fraction
         * r/n. Row 0 degenerates to corner A, row n is the B→C edge.
         */
        final ReadOnly.Vector<?>[][] lattice = new ReadOnly.Vector<?>[n+1][];
        lattice[0] = new ReadOnly.Vector<?>[]{vecA};
        for (int r = 1; r <= n; r++) {
            //the last row ends on the original corners, keep them as they are
            final ReadOnly.Vector<?> left  = (r == n) ? vecB : GreatCircleArc.interpolate(vecA, vecB, r / (double) n);
            final ReadOnly.Vector<?> right = (r == n) ? vecC : GreatCircleArc.interpolate(vecA, vecC, r / (double) n);
            final ReadOnly.Vector<?>[] row = new ReadOnly.Vector<?>[r+1];
            row[0] = left;
            row[r] = right;
            for (int k = 1; k < r; k++) {
                row[k] = GreatCircleArc.interpolate(left, right, k / (double) r);
            }
            lattice[r] = row;
        }

        final SphericalTriangle[] result = new SphericalTriangle[n*n];
        int i = 0;
        //triangles pointing the same way as this triangle
        for (int r = 0; r < n; r++) {
            for (int k = 0; k <= r; k++) {
                result[i++] = new SphericalTriangle(sphere, lattice[r][k], lattice[r+1][k], lattice[r+1][k+1]);
            }
        }
        //triangles pointing the opposite way
        for (int r = 1; r < n; r++) {
            for (int k = 0; k < r; k++) {
                result[i++] = new SphericalTriangle(sphere, lattice[r][k], lattice[r+1][k+1], lattice[r][k+1]);
            }
        }
        return result;
    }

}
