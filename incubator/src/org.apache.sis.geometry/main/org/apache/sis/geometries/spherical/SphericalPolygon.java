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
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Vector;


/**
 * A polygon on a sphere, possibly concave and with holes, whose edges are great circle arcs.
 * <p>
 * Rings are given as closed arrays of unit direction vectors from the sphere center. The static methods
 * work on ring corners without the closing point, for callers which already have them as vectors.
 * <p>
 * Containment is decided by the sum of the angles under which the ring edges are seen from the tested point.
 * Like every test based on angles, it cannot distinguish a point from its antipode when the polygon is larger
 * than a hemisphere: callers testing such polygons should reject far points first, for example with a cap.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SphericalPolygon {

    /**
     * base sphere.
     */
    private final Sphere sphere;
    /**
     * Exterior ring corners, without the closing point.
     */
    private final Vector<?>[] exterior;
    /**
     * Interior rings corners (holes), without the closing points.
     */
    private final Vector<?>[][] interiors;

    /**
     * @param sphere base sphere
     * @param exterior exterior ring, unit direction vectors from the sphere center, the first point repeated at the end
     * @param interiors holes, as closed rings like the exterior
     */
    public SphericalPolygon(Sphere sphere, Array exterior, Array... interiors) {
        this.sphere = sphere;
        this.exterior = SphericalConvexPolygon.vertices(exterior);
        this.interiors = new Vector<?>[interiors.length][];
        for (int i = 0; i < interiors.length; i++) {
            this.interiors[i] = SphericalConvexPolygon.vertices(interiors[i]);
        }
    }

    /**
     * @return the base sphere
     */
    public Sphere getSphere() {
        return sphere;
    }

    /**
     * @return exterior ring corners, without the closing point
     */
    public Vector<?>[] getExterior() {
        return exterior.clone();
    }

    /**
     * @return holes corners, without the closing points
     */
    public Vector<?>[][] getInteriors() {
        final Vector<?>[][] copy = new Vector<?>[interiors.length][];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = interiors[i].clone();
        }
        return copy;
    }

    /**
     * Test if given vector is inside the exterior ring and outside every hole.
     *
     * @param vecP unit vector to test
     * @return true if vector is inside the polygon
     */
    public boolean contains(Vector<?> vecP) {
        if (!ringContains(exterior, vecP)) {
            return false;
        }
        for (final Vector<?>[] hole : interiors) {
            if (ringContains(hole, vecP)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes the area of this polygon : the area of the exterior ring minus the areas of the holes.
     * Concave rings are measured correctly, whatever their winding.
     *
     * @return polygon area, in the sphere radius squared units
     */
    public double getArea() {
        double excess = Math.abs(excess(exterior));
        for (final Vector<?>[] hole : interiors) {
            excess -= Math.abs(excess(hole));
        }
        final double radius = sphere.getRadius();
        return excess * radius * radius;
    }

    /**
     * Test if given vector is inside the given ring, by summing the angles under which the ring edges
     * are seen from the vector: about ±2π inside, about zero outside. Works for concave rings, whatever
     * their winding.
     *
     * @param ring ring corners as unit vectors, without the closing point
     * @param vecP unit vector to test
     * @return true if the vector is inside the ring
     */
    public static boolean ringContains(Vector<?>[] ring, Vector<?> vecP) {
        double angleSum = 0;
        for (int i = 0; i < ring.length; i++) {
            final Vector<?> a = ring[i];
            final Vector<?> b = ring[(i + 1) % ring.length];
            // Projections of the corners on the plane tangent to the sphere at the tested vector
            final Vector<?> ap = a.combine(1.0, vecP, -vecP.dot(a));
            final Vector<?> bp = b.combine(1.0, vecP, -vecP.dot(b));
            angleSum += Math.atan2(ap.cross(bp).dot(vecP), ap.dot(bp));
        }
        return Math.abs(angleSum) > Math.PI;
    }

    /**
     * Returns the winding of the given ring around its center:
     * the sum of (v<sub>i</sub> × v<sub>i+1</sub>) · center over the ring edges.
     *
     * @param ring ring corners as unit vectors, without the closing point
     * @return 1 if the ring is counter-clockwise seen from outside the sphere, -1 otherwise
     */
    public static int orientation(Vector<?>[] ring) {
        Vector<?> center = ring[0];
        for (int i = 1; i < ring.length; i++) {
            center = center.add(ring[i]);
        }
        center = center.normalize();
        double sum = 0;
        for (int i = 0; i < ring.length; i++) {
            sum += center.dot(ring[i].cross(ring[(i + 1) % ring.length]));
        }
        return sum > 0 ? 1 : -1;
    }

    /**
     * Returns the normals of the great circle planes of the ring edges.
     *
     * @param ring ring corners as unit vectors, without the closing point
     * @return one normal per edge (not normalized), edge i going from corner i to corner i+1
     */
    public static Vector<?>[] edgeNormals(Vector<?>[] ring) {
        final Vector<?>[] normals = new Vector<?>[ring.length];
        for (int i = 0; i < ring.length; i++) {
            normals[i] = ring[i].cross(ring[(i + 1) % ring.length]);
        }
        return normals;
    }

    /**
     * Computes the signed spherical excess of the given ring, as a fan of triangles from its first corner.
     * Concave rings are measured correctly.
     *
     * @param ring ring corners as unit vectors, without the closing point
     * @return spherical excess in radians, positive if the ring is counter-clockwise seen from outside the sphere
     * @see SphericalTriangle#signedExcess(Vector, Vector, Vector)
     */
    public static double excess(Vector<?>[] ring) {
        double excess = 0;
        for (int i = 1; i + 1 < ring.length; i++) {
            excess += SphericalTriangle.signedExcess(ring[0], ring[i], ring[i + 1]);
        }
        return excess;
    }
}
