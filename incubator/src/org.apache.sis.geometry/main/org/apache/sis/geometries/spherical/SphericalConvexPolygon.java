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

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.geometries.surface.Sphere;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.util.ArgumentChecks;

/**
 * A convex polygon on a sphere.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SphericalConvexPolygon {

    /**
     * base sphere.
     */
    private final Sphere sphere;
    /**
     * Serie of points as a unit direction vector from the sphere center.
     * First point equels last point
     */
    private final Array points;
    /**
     * The polygon corners, without the closing point.
     */
    private final Vector<?>[] vertices;

    /**
     * @param sphere base sphere
     * @param points corners as unit direction vectors from the sphere center, counter-clockwise
     *        seen from outside the sphere, the first point being repeated at the end
     */
    public SphericalConvexPolygon(Sphere sphere, Array points) {
        this.sphere = sphere;
        this.points = points;
        this.vertices = vertices(points);
    }

    /**
     * Returns the corners of a closed ring, without the closing point.
     */
    static Vector<?>[] vertices(Array ring) {
        return ring.toArray(0, Math.max(0, ring.getLength() - 1));
    }

    /**
     * @return the base sphere
     */
    public Sphere getSphere() {
        return sphere;
    }

    /**
     * Test if given vector is contained in this polygon with default epsilon -1e-9.
     *
     * @param vecP vector to test
     * @return true if vector is inside triangle
     */
    public boolean contains(Vector<?> vecP) {
        return contains(vecP, -1e-9);
    }

    /**
     * Test if given vector is contained in this polygon.
     *
     * @param vecP vector to test
     * @param epsilon edge tolerance, should be a negative value close to zero
     * @return true if vector is inside the polygon
     */
    public boolean contains(Vector<?> vecP, double epsilon) {
        for (long a = 0, n = points.getLength()-1; a < n; a++) {
            if (vecP.dot(points.get(a).cross(points.get(a + 1))) < epsilon) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes how far the given vector is inside this polygon.
     * At each corner, the directions toward the two adjacent corners and toward the vector are
     * projected on the plane tangent to the sphere. The result is the smallest sine of the angles
     * between the vector direction and the edge directions, signed by the side of the edge.
     *
     * @param vecP unit vector to test
     * @return positive inside, zero on the boundary, negative outside;
     *         the further from zero, the further from the boundary
     * @see "Locating a point on a spherical surface relative to a spherical polygon, condition of necessary strike"
     */
    public double margin(Vector<?> vecP) {
        return margin(vertices, vecP);
    }

    /**
     * Computes the area of this polygon, as a fan of triangles around its center.
     *
     * @return polygon area, in the sphere radius squared units
     */
    public double getArea() {
        final double radius = sphere.getRadius();
        return fanExcess(vertices) * radius * radius;
    }

    /**
     * Returns the boundary of this polygon, each edge divided in arcs of equal length.
     *
     * @param segments number of arcs per edge, must be 1 or more
     * @return closed ring of unit vectors, starting and ending on the first corner
     * @throws IllegalArgumentException if {@code segments} is less than 1
     */
    public Array getBoundary(int segments) {
        return boundary(vertices, segments);
    }

    /**
     * Implementation of {@link #margin(Vector)} on the given convex ring corners.
     */
    static double margin(Vector<?>[] vertices, Vector<?> vecP) {
        final int n = vertices.length;
        double margin = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            final Vector<?> v = vertices[i];
            // A vector on the corner has no direction from it, and lies on the boundary
            Vector<?> vp = vecP.subtract(v);
            if (vp.lengthSquare() > 0) {
                vp = vp.normalize();
            }
            final Vector<?> va = vertices[(i + 1) % n].subtract(v).normalize();
            final Vector<?> vb = vertices[(i + n - 1) % n].subtract(v).normalize();
            margin = Math.min(Math.min(margin, v.dot(va.cross(vp))), v.dot(vp.cross(vb)));
        }
        return margin;
    }

    /**
     * Unsigned spherical excess of the given convex ring corners, as a fan of triangles around their center.
     */
    static double fanExcess(Vector<?>[] vertices) {
        final int n = vertices.length;
        if (n < 3) {
            return 0;
        }
        if (n == 3) {
            return Math.abs(SphericalTriangle.signedExcess(vertices[0], vertices[1], vertices[2]));
        }
        Vector<?> center = vertices[0];
        for (int i = 1; i < n; i++) {
            center = center.add(vertices[i]);
        }
        center = center.normalize();
        double excess = 0;
        for (int i = 0; i < n; i++) {
            final double triangle = SphericalTriangle.signedExcess(center, vertices[i], vertices[(i + 1) % n]);
            if (!Double.isNaN(triangle)) {
                excess += triangle;
            }
        }
        return Math.abs(excess);
    }

    /**
     * Implementation of {@link #getBoundary(int)} on the given ring corners.
     */
    static Array boundary(Vector<?>[] vertices, int segments) {
        ArgumentChecks.ensureStrictlyPositive("segments", segments);
        final int n = vertices.length;
        final List<Vector<?>> points = new ArrayList<>(n * segments + 1);
        for (int i = 0; i < n; i++) {
            final Vector<?> start = vertices[i];
            final Vector<?> end = vertices[(i + 1) % n];
            points.add(start);
            for (int j = 1; j < segments; j++) {
                points.add(GreatCircleArc.interpolate(start, end, j / (double) segments));
            }
        }
        points.add(vertices[0]);
        return NDArrays.of(points, vertices[0].getDimension(), DataType.DOUBLE);
    }
}
