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
package org.apache.sis.geometries.internal.shared.topology;

import java.util.Arrays;
import org.apache.sis.geometries.operation.Contains;
import org.apache.sis.maths.Maths;
import static org.apache.sis.geometries.internal.shared.topology.IndexedComplex.index;


/**
 * Geometric predicates on the current state of an {@link IndexedComplex}, with the tolerance of the complex.
 * They are used for verifying that the interiors of the primitives are disjoint, when building or editing.
 * All coordinates are in three dimensions, with z = 0 for a complex in two dimensions.
 *
 * @author Johann Sorel (Geomatys)
 */
final class ComplexGeometry {

    /**
     * The complex on which the predicates are evaluated.
     */
    private final IndexedComplex complex;

    /**
     * Largest distance between two positions considered the same.
     */
    final double tolerance;

    /**
     * Creates the predicates for the given complex.
     */
    ComplexGeometry(final IndexedComplex complex) {
        this.complex   = complex;
        this.tolerance = complex.tolerance;
    }

    /**
     * The plane of a face, or of rings of nodes, with the axes of a projection which keeps the orientation.
     */
    static final class Plane {
        /** Nodes of each ring, the exterior ring first, in traversal order. */
        final int[][] rings;

        /** Unit normal, toward which the exterior ring is seen counterclockwise. */
        final double[] normal;

        /** Axes of the projection plane, then the axis left out. */
        final int[] axes;

        /** A point of the plane. */
        final double[] origin;

        Plane(final int[][] rings, final double[] normal, final int[] axes, final double[] origin) {
            this.rings  = rings;
            this.normal = normal;
            this.axes   = axes;
            this.origin = origin;
        }
    }

    /**
     * Returns the coordinates of the given node in three dimensions.
     */
    double[] point(final int node) {
        return complex.position(node);
    }

    /**
     * Returns the plane of the given face.
     *
     * @throws IllegalArgumentException if the face has no area.
     */
    Plane plane(final int face) {
        final int[][] refs = complex.faceRings[face];
        final int[][] rings = new int[refs.length][];
        for (int r = 0; r < refs.length; r++) {
            rings[r] = complex.nodes(refs[r]);
        }
        final Plane plane = plane(rings);
        if (plane == null) {
            throw new IllegalArgumentException("The " + describeFace(face) + " has no area.");
        }
        return plane;
    }

    /**
     * Returns the plane of the given rings of nodes, the exterior ring first,
     * or {@code null} if the exterior ring has no area.
     */
    Plane plane(final int[][] rings) {
        final double[] n = newell(rings[0]);
        final double length = Math.sqrt(n[0]*n[0] + n[1]*n[1] + n[2]*n[2]);
        if (!(length > 0)) {
            return null;
        }
        return new Plane(rings, new double[] {n[0] / length, n[1] / length, n[2] / length},
                         Maths.projectionAxes(n), point(rings[0][0]));
    }

    /**
     * Returns the normal of the given ring of nodes with Newell's method, pointing toward an observer
     * who sees the ring counterclockwise. Its length is twice the area of the ring.
     */
    double[] newell(final int[] ring) {
        final double[] xyz = complex.xyz;
        final double[] normal = new double[3];
        for (int i = 0; i < ring.length; i++) {
            final int a = ring[i] * 3, b = ring[(i + 1) % ring.length] * 3;
            normal[0] += (xyz[a+1] - xyz[b+1]) * (xyz[a+2] + xyz[b+2]);
            normal[1] += (xyz[a+2] - xyz[b+2]) * (xyz[a]   + xyz[b]);
            normal[2] += (xyz[a]   - xyz[b])   * (xyz[a+1] + xyz[b+1]);
        }
        return normal;
    }

    /**
     * Returns whether the given ring of nodes is seen counterclockwise from the side toward which the normal
     * of the given plane points.
     */
    boolean counterclockwise(final Plane plane, final int[] ring) {
        final double[] n = newell(ring);
        return n[0] * plane.normal[0] + n[1] * plane.normal[1] + n[2] * plane.normal[2] > 0;
    }

    /**
     * Returns the signed distance from the given plane to the given point.
     */
    double distance(final Plane plane, final double[] p) {
        final double[] n = plane.normal, o = plane.origin;
        return n[0] * (p[0] - o[0]) + n[1] * (p[1] - o[1]) + n[2] * (p[2] - o[2]);
    }

    /**
     * Returns whether the given point is inside the face of the given plane, excluding its boundary.
     *
     * @param checkPlane  whether to verify that the point is in the plane. Otherwise the point is projected.
     */
    boolean insideFace(final Plane plane, final double[] p, final boolean checkPlane) {
        if (checkPlane && Math.abs(distance(plane, p)) > tolerance) {
            return false;
        }
        return insideRings(plane, plane.rings, p);
    }

    /**
     * Returns whether the given point, projected on the given plane, is inside the given rings of nodes
     * with the even-odd rule, excluding the boundaries of the rings.
     */
    boolean insideRings(final Plane plane, final int[][] rings, final double[] p) {
        final double[] xyz = complex.xyz;
        final int u = plane.axes[0], v = plane.axes[1];
        final double x = p[u], y = p[v];
        boolean inside = false;
        for (final int[] ring : rings) {
            for (int i = 0; i < ring.length; i++) {
                final int a = ring[i] * 3, b = ring[(i + 1) % ring.length] * 3;
                final double ax = xyz[a+u], ay = xyz[a+v], bx = xyz[b+u], by = xyz[b+v];
                if (nearSegment(x, y, ax, ay, bx, by)) {
                    return false;                       // on a boundary.
                }
                if ((ay > y) != (by > y) && x < ax + (y - ay) * (bx - ax) / (by - ay)) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    /**
     * Returns whether the point (x,y) is at a distance not greater than the tolerance from the given segment.
     */
    private boolean nearSegment(final double x, final double y, final double ax, final double ay, final double bx, final double by) {
        final double dx = bx - ax, dy = by - ay;
        final double length2 = dx*dx + dy*dy;
        double t = (length2 > 0) ? ((x - ax)*dx + (y - ay)*dy) / length2 : 0;
        t = Math.max(0, Math.min(1, t));
        final double ex = ax + t*dx - x, ey = ay + t*dy - y;
        return ex*ex + ey*ey <= tolerance * tolerance;
    }

    /**
     * Returns whether two points are at a distance not greater than the tolerance.
     */
    boolean coincide(final double[] p, final double[] q) {
        final double dx = p[0] - q[0], dy = p[1] - q[1], dz = p[2] - q[2];
        return dx*dx + dy*dy + dz*dz <= tolerance * tolerance;
    }

    /**
     * Returns whether the given point is on the segment from {@code a} to {@code b}, excluding its ends.
     */
    boolean insideSegment(final double[] p, final double[] a, final double[] b) {
        final double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        final double length2 = dx*dx + dy*dy + dz*dz;
        if (length2 == 0) {
            return false;
        }
        final double t = ((p[0] - a[0])*dx + (p[1] - a[1])*dy + (p[2] - a[2])*dz) / length2;
        final double length = Math.sqrt(length2);
        if (t * length <= tolerance || (1 - t) * length <= tolerance) {
            return false;
        }
        final double ex = a[0] + t*dx - p[0];
        final double ey = a[1] + t*dy - p[1];
        final double ez = a[2] + t*dz - p[2];
        return ex*ex + ey*ey + ez*ez <= tolerance * tolerance;
    }

    /**
     * Returns whether the segments from {@code a0} to {@code a1} and from {@code b0} to {@code b1} cross at a point
     * inside both of them. Parallel segments are considered not crossing: if they overlap, an end of one is inside
     * the other.
     */
    boolean cross(final double[] a0, final double[] a1, final double[] b0, final double[] b1) {
        final double[] d1 = new double[3], d2 = new double[3], r = new double[3];
        for (int k = 0; k < 3; k++) {
            d1[k] = a1[k] - a0[k];
            d2[k] = b1[k] - b0[k];
            r[k]  = a0[k] - b0[k];
        }
        final double a = dot(d1, d1), e = dot(d2, d2), b = dot(d1, d2), c = dot(d1, r), f = dot(d2, r);
        final double denominator = a*e - b*b;
        if (!(denominator > 1E-12 * a * e)) {
            return false;
        }
        // Parameters of the closest points on each line.
        final double s = (b*f - c*e) / denominator;
        final double t = (a*f - b*c) / denominator;
        final double la = Math.sqrt(a), le = Math.sqrt(e);
        if (s * la <= tolerance || (1 - s) * la <= tolerance || t * le <= tolerance || (1 - t) * le <= tolerance) {
            return false;
        }
        double distance2 = 0;
        for (int k = 0; k < 3; k++) {
            final double d = r[k] + s * d1[k] - t * d2[k];
            distance2 += d * d;
        }
        return distance2 <= tolerance * tolerance;
    }

    private static double dot(final double[] a, final double[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    /**
     * Returns whether the segment from {@code a} to {@code b}, excluding its ends, meets the interior of the face
     * of the given plane. A segment in the plane of the face is tested by its middle, since it does not cross
     * the boundary of the face if the edges have been verified. A segment crossing the plane is tested
     * by its crossing point.
     */
    boolean meetsFace(final Plane plane, final double[] a, final double[] b) {
        final double da = distance(plane, a);
        final double db = distance(plane, b);
        final double t;
        if (Math.abs(da) <= tolerance && Math.abs(db) <= tolerance) {
            t = 0.5;
        } else if ((da > tolerance && db < -tolerance) || (da < -tolerance && db > tolerance)) {
            t = da / (da - db);
        } else {
            return false;               // touches the plane at an end at most.
        }
        final double[] p = new double[3];
        for (int k = 0; k < 3; k++) {
            p[k] = a[k] + t * (b[k] - a[k]);
        }
        return insideFace(plane, p, false);
    }

    /**
     * Returns a point inside the face of the given plane. The point is on a line of the projection plane passing
     * through the middle of the largest gap between the nodes, in the middle of the widest interval of that line
     * inside the face. The point is then lifted to the plane.
     */
    double[] interiorPoint(final Plane plane) {
        final double[] xyz = complex.xyz;
        final int u = plane.axes[0], v = plane.axes[1], w = plane.axes[2];
        int count = 0;
        for (final int[] ring : plane.rings) {
            count += ring.length;
        }
        final double[] ys = new double[count];
        count = 0;
        for (final int[] ring : plane.rings) {
            for (final int node : ring) {
                ys[count++] = xyz[node * 3 + v];
            }
        }
        Arrays.sort(ys);
        double y = ys[0], gap = -1;
        for (int i = 1; i < count; i++) {
            if (ys[i] - ys[i - 1] > gap) {
                gap = ys[i] - ys[i - 1];
                y = (ys[i] + ys[i - 1]) / 2;
            }
        }
        final double[] xs = new double[count];
        int n = 0;
        for (final int[] ring : plane.rings) {
            for (int i = 0; i < ring.length; i++) {
                final int a = ring[i] * 3, b = ring[(i + 1) % ring.length] * 3;
                final double ay = xyz[a+v], by = xyz[b+v];
                if ((ay > y) != (by > y)) {
                    xs[n++] = xyz[a+u] + (y - ay) * (xyz[b+u] - xyz[a+u]) / (by - ay);
                }
            }
        }
        Arrays.sort(xs, 0, n);
        double x = (n != 0) ? xs[0] : plane.origin[u], width = -1;
        for (int i = 0; i + 1 < n; i += 2) {
            if (xs[i + 1] - xs[i] > width) {
                width = xs[i + 1] - xs[i];
                x = (xs[i] + xs[i + 1]) / 2;
            }
        }
        final double[] normal = plane.normal, o = plane.origin;
        final double[] p = new double[3];
        p[u] = x;
        p[v] = y;
        p[w] = o[w] - (normal[u] * (x - o[u]) + normal[v] * (y - o[v])) / normal[w];
        return p;
    }

    /**
     * Returns the faces of the given shells as triangles, three node indices per triangle, oriented as the shells
     * use the faces. Each ring is split in a fan of triangles from its first node, which gives the right winding
     * number for planar rings, convex or not.
     */
    int[] triangles(final int[][] shells) {
        int count = 0;
        for (final int[] shell : shells) {
            for (final int ref : shell) {
                for (final int[] ring : complex.faceRings[index(ref)]) {
                    count += ring.length - 2;
                }
            }
        }
        final int[] triangles = new int[count * 3];
        int t = 0;
        for (final int[] shell : shells) {
            for (final int ref : shell) {
                final boolean reverse = ref < 0;
                for (final int[] refs : complex.faceRings[index(ref)]) {
                    final int[] ring = complex.nodes(refs);
                    for (int k = 1; k + 1 < ring.length; k++) {
                        triangles[t++] = ring[0];
                        triangles[t++] = ring[reverse ? k + 1 : k];
                        triangles[t++] = ring[reverse ? k : k + 1];
                    }
                }
            }
        }
        return triangles;
    }

    /**
     * Returns the bounding box of the given shells, as the minimal then maximal coordinates.
     */
    double[] box(final int[][] shells) {
        final double[] xyz = complex.xyz;
        final double[] box = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                              Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (final int[] shell : shells) {
            for (final int ref : shell) {
                for (final int edge : complex.faceRings[index(ref)][0]) {
                    final int node = complex.startNode(edge) * 3;
                    for (int k = 0; k < 3; k++) {
                        box[k]     = Math.min(box[k],     xyz[node + k]);
                        box[k + 3] = Math.max(box[k + 3], xyz[node + k]);
                    }
                }
            }
        }
        return box;
    }

    /**
     * Returns whether the given point is inside the solid of the given box and triangles.
     * The point shall not be on the boundary of the solid.
     */
    boolean insideSolid(final double[] box, final int[] triangles, final double[] p) {
        return inBox(box, p) && Math.abs(Contains.windingNumber(complex.xyz, triangles, p)) > 0.5;
    }

    /**
     * Returns whether the given point is inside the solid of the given shells.
     * The point shall not be on the boundary of the solid.
     */
    boolean insideSolid(final int[][] shells, final double[] p) {
        return inBox(box(shells), p) && Math.abs(Contains.windingNumber(complex.xyz, triangles(shells), p)) > 0.5;
    }

    /**
     * Returns whether the given point is in the given box, within the tolerance.
     */
    private boolean inBox(final double[] box, final double[] p) {
        for (int k = 0; k < 3; k++) {
            if (p[k] < box[k] - tolerance || p[k] > box[k + 3] + tolerance) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the volume enclosed by the given faces, positive if they are oriented outward.
     * The volume is the sum of the tetrahedra joining a reference node to the triangles
     * of a fan over each ring, which is exact for planar rings, convex or not.
     */
    double signedVolume(final int[] shell) {
        final double[] xyz = complex.xyz;
        final int o = complex.startNode(complex.faceRings[index(shell[0])][0][0]) * 3;
        double volume = 0;
        for (final int ref : shell) {
            double sum = 0;
            for (final int[] refs : complex.faceRings[index(ref)]) {
                final int[] ring = complex.nodes(refs);
                final int a = ring[0] * 3;
                for (int i = 1; i + 1 < ring.length; i++) {
                    final int b = ring[i] * 3, c = ring[i + 1] * 3;
                    final double ax = xyz[a] - xyz[o], ay = xyz[a+1] - xyz[o+1], az = xyz[a+2] - xyz[o+2];
                    final double bx = xyz[b] - xyz[o], by = xyz[b+1] - xyz[o+1], bz = xyz[b+2] - xyz[o+2];
                    final double cx = xyz[c] - xyz[o], cy = xyz[c+1] - xyz[o+1], cz = xyz[c+2] - xyz[o+2];
                    sum += ax * (by*cz - bz*cy) + ay * (bz*cx - bx*cz) + az * (bx*cy - by*cx);
                }
            }
            volume += (ref < 0) ? -sum : sum;
        }
        return volume / 6;
    }

    /**
     * Returns the coordinates of the given node, for error messages.
     */
    String describeNode(final int node) {
        return Arrays.toString(complex.getCoordinates(node));
    }

    /**
     * Returns the ends of the given edge, for error messages.
     */
    String describeEdge(final int edge) {
        return "edge from " + describeNode(complex.edgeNodes[2*edge]) + " to " + describeNode(complex.edgeNodes[2*edge + 1]);
    }

    /**
     * Returns the first corners of the given face, for error messages.
     */
    String describeFace(final int face) {
        final int[] ring = complex.nodes(complex.faceRings[face][0]);
        final var buffer = new StringBuilder("face with corners ");
        for (int i = 0; i < Math.min(ring.length, 4); i++) {
            if (i != 0) buffer.append(", ");
            buffer.append(describeNode(ring[i]));
        }
        if (ring.length > 4) {
            buffer.append(", …");
        }
        return buffer.toString();
    }
}
