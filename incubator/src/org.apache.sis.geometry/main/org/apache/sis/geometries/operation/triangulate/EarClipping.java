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
package org.apache.sis.geometries.operation.triangulate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.util.ArraysExt;


/**
 * Origin : Adapted from Unlicense-Lib
 *
 * Triangulation using ear clipping algorithm.
 * http://en.wikipedia.org/wiki/Polygon_triangulation
 *
 * <p>The holes of the polygon are first joined to its exterior ring by {@link HoleToExterior},
 * then the ring is triangulated. The triangulation works on the indices of the positions of
 * the polygon: the triangles reference the vertices instead of copying their positions, so that
 * all the attributes of the vertices are kept.</p>
 *
 * @author Johann Sorel
 */
public class EarClipping {

    /**
     * Result of a triangulation: the vertices of the triangles, and the triangles as indices of those vertices.
     *
     * @param vertices   the vertices used by the triangles, with all the attributes of the polygon.
     * @param triangles  the indices in {@code vertices} of the three corners of each triangle, one triangle after the other.
     */
    public record Triangulation(DataPoints vertices, int[] triangles) {
    }

    /**
     * Type of the angle made by the ring at a vertex, computed when first needed.
     */
    private enum Angle {
        UNKNOWN,
        CONVEX,
        CONCAVE
    }

    /**
     * Positions of the polygon to triangulate, those of the exterior ring followed by those of the holes.
     */
    private DataPoints source;

    /**
     * Positions of the polygon, with their coordinates in the order used by the triangulation.
     * The triangulation works on the two first coordinates, in the plane where the polygon has its largest area.
     */
    private Vector<?>[] working;

    /**
     * Indices in {@link #source} of the vertices of the ring joining the holes to the exterior ring, without
     * repeated consecutive positions and without the closing position. The ring is clockwise in the plane
     * of the triangulation.
     */
    private int[] ring;

    /**
     * For each vertex of the {@linkplain #ring}, the previous and next vertices which are not cut yet.
     * Cutting an ear unlinks its vertex from the ring.
     */
    private int[] previous, next;

    /**
     * The type of angle made by the ring at each vertex, stored to avoid computing it each time.
     */
    private Angle[] angles;

    /**
     * Indices in {@link #source} of the three vertices of each triangle found so far.
     */
    private final List<int[]> triangles = new ArrayList<>();

    /**
     * Triangulates the given polygon.
     *
     * @param geometry geometry to triangulate
     * @return the vertices of the triangles and their indices.
     */
    public Triangulation triangulateIndexed(Polygon geometry) {
        source = geometry.getDataPoints();
        final int[] axes = workingAxes(geometry.getExteriorRing().getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION));
        working = new Vector<?>[source.size()];
        Arrays.setAll(working, (i) -> source.getPosition(i).swizzle(axes));
        triangles.clear();
        run(HoleToExterior.ring(geometry));
        return result();
    }

    /**
     * Triangulates the given polygon into an indexed mesh holding all the attributes of the polygon.
     */
    public MeshPrimitive.Triangles toMesh(Polygon polygon) {
        final Triangulation triangulation = triangulateIndexed(polygon);
        final DataPoints vertices = triangulation.vertices();
        final MeshPrimitive.Triangles mesh = new MeshPrimitive.Triangles();
        for (final String name : vertices.getType().getAttributeNames()) {
            mesh.setAttribute(name, vertices.getAttributeArray(name));
        }
        mesh.setIndex(NDArrays.packIntegerDataType(NDArrays.of(1, triangulation.triangles())));
        return mesh;
    }

    /**
     * Returns the order of the coordinates to use for triangulating a polygon with the given positions,
     * to be given to {@link Vector#swizzle(int...)}. The triangulation works on the two first coordinates
     * of that order. The axis along which the {@linkplain Maths#calculatePolygonNormal(Array) normal of the
     * polygon} is the largest is moved after the two axes of the projection plane, so that a vertical polygon
     * is not degenerate. If the polygon can be triangulated in its (x,y) plane, the order is unchanged.
     */
    static int[] workingAxes(final Array positions) {
        final int dim = positions.getDimension();
        final int[] axes = ArraysExt.range(0, dim);
        if (dim >= 3) {
            final double[] normal = Maths.calculatePolygonNormal(positions);
            final double nx = Math.abs(normal[0]), ny = Math.abs(normal[1]), nz = Math.abs(normal[2]);
            final int drop = (nz >= nx && nz >= ny) ? 2 : (nx >= ny) ? 0 : 1;
            //shift the following axes of the plane to the place of the dropped axis, which goes after them.
            System.arraycopy(axes, drop + 1, axes, drop, 2 - drop);
            axes[2] = drop;
        }
        return axes;
    }

    /**
     * Builds the result from the triangles found. Only the vertices used by a triangle are kept, in the
     * order of their first use. The positions repeated by the cuts joining the holes have the same index,
     * so they are a single vertex.
     */
    private Triangulation result() {
        final Map<Integer,Integer> vertices = new LinkedHashMap<>();      // index in the source → index in the result.
        final int[] indices = new int[triangles.size() * 3];
        int k = 0;
        for (final int[] triangle : triangles) {
            for (final int vertex : triangle) {
                indices[k++] = vertices.computeIfAbsent(vertex, (key) -> vertices.size());
            }
        }
        return new Triangulation(source.subset(vertices.keySet().stream().mapToInt(Integer::intValue).toArray()), indices);
    }

    /**
     * Triangulates the given ring by cutting its ears one after the other. An ear is a convex vertex whose
     * triangle with its two neighbors contains no other vertex. Cutting it removes the vertex from the
     * ring, which stays a valid polygon, until only one triangle is left.
     *
     * @param joined  indices in {@link #source} of the ring positions, the last one at the location of the first one.
     */
    private void run(final int[] joined) {
        //remove the repeated consecutive positions, and the closing position which repeats the first one.
        ring = IntStream.range(0, joined.length - 1).filter((i) -> !working[joined[i]].equals(working[joined[i + 1]]))
                        .map((i) -> joined[i]).toArray();
        if (!Maths.isClockWise(Arrays.stream(joined).mapToObj((i) -> working[i]).toList())) {
            ArraysExt.reverse(ring);
        }
        final int n = ring.length;
        previous = new int[n];
        next     = new int[n];
        for (int i = 0; i < n; i++) {
            previous[i] = (i + n - 1) % n;
            next[i]     = (i + 1) % n;
        }
        angles = new Angle[n];
        Arrays.fill(angles, Angle.UNKNOWN);

        //walk around the ring, until a whole turn finds no ear, which should not happen if the geometry is correct.
        int vertex = 0;
        for (int remaining = n, attempts = 0; remaining >= 3 && attempts < remaining; vertex = next[vertex]) {
            if (isConvex(vertex) && isEar(vertex)) {
                triangles.add(new int[] {ring[previous[vertex]], ring[vertex], ring[next[vertex]]});
                //unlink the vertex, the angles at its neighbors may change.
                next[previous[vertex]] = next[vertex];
                previous[next[vertex]] = previous[vertex];
                angles[previous[vertex]] = Angle.UNKNOWN;
                angles[next[vertex]]     = Angle.UNKNOWN;
                remaining--;
                attempts = 0;
            } else {
                attempts++;
            }
        }
    }

    /**
     * Returns the working position of the given vertex of the ring.
     */
    private Vector<?> position(final int vertex) {
        return working[ring[vertex]];
    }

    /**
     * @return true if the ring is convex at the given vertex
     */
    private boolean isConvex(final int vertex) {
        if (angles[vertex] == Angle.UNKNOWN) {
            final boolean convex = Maths.isConvex(position(previous[vertex]), position(vertex), position(next[vertex]), true);
            angles[vertex] = convex ? Angle.CONVEX : Angle.CONCAVE;
        }
        return angles[vertex] == Angle.CONVEX;
    }

    /**
     * Returns whether the triangle made by the given vertex and its two neighbors contains no other vertex
     * of the ring. Only the concave vertices need to be tested. A cut joining a hole repeats its end points,
     * so a copy located on a corner of the triangle does not prevent the ear.
     */
    private boolean isEar(final int vertex) {
        final Vector<?> t1 = position(previous[vertex]);
        final Vector<?> t2 = position(vertex);
        final Vector<?> t3 = position(next[vertex]);
        for (int i = next[next[vertex]]; i != previous[vertex]; i = next[i]) {
            final Vector<?> p = position(i);
            if (!isConvex(i) && !p.equals(t1) && !p.equals(t2) && !p.equals(t3) && Maths.inTriangle(t1, t2, t3, p)) {
                return false;
            }
        }
        return true;
    }
}
