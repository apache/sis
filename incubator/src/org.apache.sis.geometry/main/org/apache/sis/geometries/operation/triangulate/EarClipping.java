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
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.ReadOnly;
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
 * <p>The triangles have the orientation of the exterior ring of the polygon: seen from any side,
 * they turn in the same direction as the exterior ring, so a face of a solid keeps its normal.</p>
 *
 * @author Johann Sorel
 */
public class EarClipping {

    /**
     * Largest area, relative to the square of the size of the polygon, enclosed by vertices left without ear.
     * Those vertices are aligned or along the cuts joining the holes, and the area is the rounding errors.
     */
    private static final double AREA_TOLERANCE = 1E-10;

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
     * Positions of the vertices, with their coordinates in the order used by the triangulation.
     * The triangulation works on the two first coordinates. For a polygon, those positions are
     * the ones of {@link #source} in the plane where the polygon has its largest area.
     */
    private ReadOnly.Vector<?>[] working;

    /**
     * Indices in {@link #working} of the vertices of the ring to triangulate, without repeated consecutive
     * positions and without the closing position. For a polygon, this is the ring joining the holes to the
     * exterior ring, which has the orientation of the exterior ring.
     */
    private int[] ring;

    /**
     * Whether the {@linkplain #ring} is clockwise in the plane of the triangulation.
     * The triangles are cut in the direction of the ring, so they have the same orientation.
     */
    private boolean clockwise;

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
     * Indices in {@link #working} of the three vertices of each triangle found so far.
     */
    private final List<int[]> triangles = new ArrayList<>();

    /**
     * Triangulates the given polygon.
     *
     * @param geometry geometry to triangulate
     * @return the vertices of the triangles and their indices.
     * @throws OperationException if the polygon cannot be triangulated, which happens with an invalid polygon
     *         such as a polygon whose exterior ring intersects itself.
     */
    public Triangulation triangulateIndexed(Polygon geometry) throws OperationException {
        source = geometry.getDataPoints();
        final int[] axes = workingAxes(geometry.getExteriorRing().getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION));
        working = new ReadOnly.Vector<?>[source.size()];
        Arrays.setAll(working, (i) -> source.getPosition(i).swizzle(axes));
        triangles.clear();
        run(HoleToExterior.ring(geometry));
        return result();
    }

    /**
     * Triangulates a simple ring given by the indices of its vertices in an array of positions.
     * The triangulation works on the two first coordinates of the positions: a ring in three dimensions
     * shall be projected first, for example with {@link ReadOnly.Vector#swizzle(int...)} and the axes given by
     * {@link Maths#projectionAxes(double[])}. Consecutive vertices at the same position are a single vertex.
     *
     * @param positions  positions of the vertices, of which the ring may use only some.
     * @param indices    indices in {@code positions} of the vertices of the ring, in order. The ring can be closed,
     *                   with its last vertex at the location of the first one, or not.
     * @return indices in {@code positions} of the three corners of each triangle, one triangle after the other.
     *         The triangles have the orientation of the ring.
     * @throws OperationException if the ring cannot be triangulated, for example because it intersects itself.
     */
    public int[] triangulate(final ReadOnly.Vector<?>[] positions, int[] indices) throws OperationException {
        working = positions;
        triangles.clear();
        final int n = indices.length;
        if (n != 0 && !positions[indices[0]].equals(positions[indices[n - 1]])) {
            //the triangulation expects a closed ring.
            indices = Arrays.copyOf(indices, n + 1);
            indices[n] = indices[0];
        }
        run(indices);
        return triangles.stream().flatMapToInt(Arrays::stream).toArray();
    }

    /**
     * Triangulates the given polygon into an indexed mesh holding all the attributes of the polygon.
     *
     * @throws OperationException if the polygon cannot be triangulated.
     */
    public MeshPrimitive.Triangles toMesh(Polygon polygon) throws OperationException {
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
     * to be given to {@link ReadOnly.Vector#swizzle(int...)}. The triangulation works on the two first coordinates
     * of that order. With three or more dimensions, the three first axes are in the {@linkplain Maths#projectionAxes
     * projection order} of the {@linkplain Maths#calculatePolygonNormal(Array) normal of the polygon}: the axis along
     * which the normal is the largest goes after the two axes of the projection plane, so that a vertical polygon is
     * not degenerate, and the polygon is counterclockwise in that plane. The other axes keep their order.
     * In two dimensions, the order is unchanged.
     */
    static int[] workingAxes(final Array positions) {
        final int[] axes = ArraysExt.range(0, positions.getDimension());
        if (axes.length >= 3) {
            System.arraycopy(Maths.projectionAxes(Maths.calculatePolygonNormal(positions)), 0, axes, 0, 3);
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
     * @param joined  indices in {@link #working} of the ring positions, the last one at the location of the first one.
     * @throws OperationException if the vertices left without ear still enclose an area.
     */
    private void run(final int[] joined) throws OperationException {
        //remove the repeated consecutive positions, and the closing position which repeats the first one.
        ring = IntStream.range(0, joined.length - 1).filter((i) -> !working[joined[i]].equals(working[joined[i + 1]]))
                        .map((i) -> joined[i]).toArray();
        clockwise = Maths.isClockWise(Arrays.stream(joined).mapToObj((i) -> working[i]).toList());
        final int n = ring.length;
        previous = new int[n];
        next     = new int[n];
        for (int i = 0; i < n; i++) {
            previous[i] = (i + n - 1) % n;
            next[i]     = (i + 1) % n;
        }
        angles = new Angle[n];
        Arrays.fill(angles, Angle.UNKNOWN);

        /*
         * Walk around the ring, until a whole turn finds no ear. With a valid polygon, this happens only when
         * the vertices left enclose no area, for example when they are aligned along the cut joining a hole.
         */
        int vertex = 0, remaining = n;
        for (int attempts = 0; remaining >= 3 && attempts < remaining; vertex = next[vertex]) {
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
        if (remaining >= 3 && !isDegenerate(vertex)) {
            throw new OperationException("Cannot triangulate the polygon: no ear found among the " + remaining
                    + " vertices left, the polygon may be invalid, for example with a ring intersecting itself.");
        }
    }

    /**
     * Returns whether the vertices which are not cut yet enclose no area, compared to the size of the polygon.
     * The area is computed relatively to one of those vertices, for reducing the rounding errors.
     *
     * @param start  a vertex which is not cut yet.
     */
    private boolean isDegenerate(final int start) {
        final ReadOnly.Vector<?> origin = position(start);
        final double ox = origin.get(0), oy = origin.get(1);
        double area = 0;            // twice the signed area.
        int vertex = start;
        do {
            final ReadOnly.Vector<?> a = position(vertex), b = position(next[vertex]);
            area += (a.get(0) - ox) * (b.get(1) - oy) - (b.get(0) - ox) * (a.get(1) - oy);
            vertex = next[vertex];
        } while (vertex != start);
        double xmin = Double.POSITIVE_INFINITY, xmax = Double.NEGATIVE_INFINITY;
        double ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
        for (final int index : ring) {
            final ReadOnly.Vector<?> p = working[index];
            xmin = Math.min(xmin, p.get(0)); xmax = Math.max(xmax, p.get(0));
            ymin = Math.min(ymin, p.get(1)); ymax = Math.max(ymax, p.get(1));
        }
        final double size = Math.max(xmax - xmin, ymax - ymin);
        return Math.abs(area) / 2 <= AREA_TOLERANCE * size * size;
    }

    /**
     * Returns the working position of the given vertex of the ring.
     */
    private ReadOnly.Vector<?> position(final int vertex) {
        return working[ring[vertex]];
    }

    /**
     * @return true if the ring is convex at the given vertex
     */
    private boolean isConvex(final int vertex) {
        if (angles[vertex] == Angle.UNKNOWN) {
            final boolean convex = Maths.isConvex(position(previous[vertex]), position(vertex), position(next[vertex]), clockwise);
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
        final ReadOnly.Vector<?> t1 = position(previous[vertex]);
        final ReadOnly.Vector<?> t2 = position(vertex);
        final ReadOnly.Vector<?> t3 = position(next[vertex]);
        for (int i = next[next[vertex]]; i != previous[vertex]; i = next[i]) {
            final ReadOnly.Vector<?> p = position(i);
            if (!isConvex(i) && !p.equals(t1) && !p.equals(t2) && !p.equals(t3) && Maths.inTriangle(t1, t2, t3, p)) {
                return false;
            }
        }
        return true;
    }
}
