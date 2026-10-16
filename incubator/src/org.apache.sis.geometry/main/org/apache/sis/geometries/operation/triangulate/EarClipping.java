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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryFactory;
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
 * the ring: the triangles reference the vertices instead of copying their positions, so that
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
     * Values of all the attributes of a vertex, for finding the vertices which are identical.
     */
    private record Key(double[] values) {
        @Override public boolean equals(final Object other) {
            return (other instanceof Key k) && Arrays.equals(values, k.values);
        }

        @Override public int hashCode() {
            return Arrays.hashCode(values);
        }
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
     * Positions of the ring to triangulate, the last one at the same position as the first one.
     */
    private DataPoints source;

    /**
     * Positions of the ring, with their coordinates in the order used by the triangulation.
     * The triangulation works on the two first coordinates, in the plane where the polygon has its largest area.
     */
    private Vector<?>[] working;

    /**
     * Indices of the three vertices of each triangle found so far.
     */
    private final List<int[]> triangles = new ArrayList<>();

    /** number of coordinates, may be inferior to coords length. */
    private int nbCoords;
    /** indices of the vertices to triangulate. */
    private int[] coords;
    /**
     * The type of angle made by the ring at each vertex to triangulate,
     * stored to avoid computing it each time.
     */
    private Angle[] coordType;

    //current segment analyzed
    private int indexPrevious;
    private int index;
    private int indexNext;
    private Vector<?> t1; //previous point
    private Vector<?> t2; //point at index
    private Vector<?> t3; //next point

    private void reset(){
        //reset values
        source = null;
        working = null;
        triangles.clear();
        nbCoords = 0;
        coords = null;
        coordType = null;
    }

    /**
     * Triangulates the given polygon.
     *
     * @param geometry geometry to triangulate
     * @return the vertices of the triangles and their indices.
     */
    public Triangulation triangulateIndexed(Polygon geometry) {
        reset();
        source = HoleToExterior.apply(geometry).getExteriorRing().getDataPoints();
        final int[] axes = workingAxes(source.getAttributeArray(DataPointsType.ATT_POSITION));
        working = new Vector<?>[source.size()];
        for (int i = 0; i < working.length; i++) {
            working[i] = source.getPosition(i).swizzle(axes);
        }
        run();
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
     * of that order. The axis along which the normal of the polygon is the largest, computed with Newell's
     * method, is moved after the two axes of the projection plane, so that a vertical polygon is not
     * degenerate. If the polygon can be triangulated in its (x,y) plane, the order is unchanged.
     */
    static int[] workingAxes(final Array positions) {
        final int dim = positions.getDimension();
        final int[] axes = new int[dim];
        for (int i = 0; i < dim; i++) {
            axes[i] = i;
        }
        if (dim < 3) {
            return axes;
        }
        final double[] c = positions.toArrayDouble();
        final int n = c.length / dim;
        double nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < n; i++) {
            final int a = i * dim, b = ((i + 1) % n) * dim;
            nx += (c[a+1] - c[b+1]) * (c[a+2] + c[b+2]);
            ny += (c[a+2] - c[b+2]) * (c[a]   + c[b]);
            nz += (c[a]   - c[b])   * (c[a+1] + c[b+1]);
        }
        nx = Math.abs(nx); ny = Math.abs(ny); nz = Math.abs(nz);
        if (nz >= nx && nz >= ny) {
            return axes;
        }
        final int drop = (nx >= ny) ? 0 : 1;
        int k = 0;
        for (int i = 0; i < 3; i++) {
            if (i != drop) axes[k++] = i;
        }
        axes[k] = drop;
        return axes;
    }

    /**
     * Builds the result from the triangles found. Only the vertices used by a triangle are kept, in the
     * order of the ring, and the vertices having the same values for all attributes are merged. Those are
     * the last position of the ring, which repeats the first one, and the positions repeated by the cuts
     * joining the holes.
     */
    private Triangulation result() {
        final int count = working.length;
        final boolean[] used = new boolean[count];
        for (final int[] triangle : triangles) {
            for (final int vertex : triangle) {
                used[vertex] = true;
            }
        }
        final DataPointsType type = source.getType();
        final List<String> names = type.getAttributeNames();
        final Map<Key,Integer> distinct = new HashMap<>();
        final List<Integer> kept = new ArrayList<>();
        final int[] remap = new int[count];
        for (int i = 0; i < count; i++) {
            if (used[i]) {
                double[] values = new double[0];
                for (final String name : names) {
                    final double[] value = source.getAttribute(i, name).toArrayDouble();
                    values = Arrays.copyOf(values, values.length + value.length);
                    System.arraycopy(value, 0, values, values.length - value.length, value.length);
                }
                final Integer previous = distinct.putIfAbsent(new Key(values), kept.size());
                if (previous != null) {
                    remap[i] = previous;
                } else {
                    remap[i] = kept.size();
                    kept.add(i);
                }
            }
        }
        final DataPoints vertices = GeometryFactory.DEFAULT.createDataPoints(type, kept.size());
        for (int i = 0; i < kept.size(); i++) {
            for (final String name : names) {
                vertices.setAttribute(i, name, source.getAttribute(kept.get(i), name));
            }
        }
        final int[] indices = new int[triangles.size() * 3];
        for (int i = 0, k = 0; i < triangles.size(); i++) {
            for (final int vertex : triangles.get(i)) {
                indices[k++] = remap[vertex];
            }
        }
        return new Triangulation(vertices, indices);
    }

    /**
     * Triangulates the ring.
     */
    private void run() {
        if (working.length == 0) {
            return;
        }
        final List<Integer> border = new ArrayList<>(working.length);
        for (int i = 0; i < working.length; i++) {
            border.add(i);
        }

        //remove any neighor points overlaping
        Vector<?> t = working[border.get(0)];
        for(int i=1,n=border.size();i<n;i++){
            final Vector<?> candidate = working[border.get(i)];
            if(candidate.equals(t)){
                border.remove(i);
                i--;
                n--;
            }else{
                t = candidate;
            }
        }

        nbCoords = border.size();
        coordType = new Angle[nbCoords];
        Arrays.fill(coordType, Angle.UNKNOWN);
        coords = border.stream().mapToInt(Integer::intValue).toArray();

        //flip coordinates if not clockwise
        if(!Maths.isClockWise(Arrays.stream(coords).mapToObj((i) -> working[i]).toList())){
            ArraysExt.reverse(coords);
        }

        //now cut ears progressively
        int nbcut = 0;
        while(nbCoords > 3){
            nbcut = 0;
            for(index=0;index<nbCoords-2 && nbCoords>3;){
                indexPrevious = (index==0) ? nbCoords-2 : index-1 ;
                indexNext = index+1;

                t1 = working[coords[indexPrevious]];
                t2 = working[coords[index]];
                t3 = working[coords[indexNext]];

                if(isConvex(index) && isEar()){
                    //reset angle type, we remove a point so it might change
                    coordType[indexPrevious]=Angle.UNKNOWN;
                    coordType[indexNext]=Angle.UNKNOWN;

                    triangles.add(new int[]{coords[indexPrevious], coords[index], coords[indexNext]});
                    Maths.removeWithin(coords, index);
                    Maths.removeWithin(coordType, index);
                    nbCoords--;
                    nbcut++;
                    //we do not increment since we have remove the point
                }else{
                    index++;
                }
            }

            if(nbcut == 0){
                //this should not happen if the geometry is correct
                //System.out.println("Triangulation failed. no ear to cut.");
                return;
            }
        }
    }

    /**
     * @return true if the ring is convex at the vertex of given index
     */
    private boolean isConvex(int idx){
        if(coordType[idx]==Angle.UNKNOWN){
            //calculate angle type
            final Vector<?> s1 = working[coords[(idx==0) ? (nbCoords-2) : (idx-1)]];
            final Vector<?> s2 = working[coords[idx]];
            final Vector<?> s3 = working[coords[idx+1]];
            final double side = Maths.lineSide(s1, s3, s2);
            coordType[idx] = side>0 ? Angle.CONVEX : Angle.CONCAVE;
        }
        return coordType[idx] == Angle.CONVEX;
    }

    /**
     * Check this segment triangle is an ear.
     * Does not contain any other point.
     * @return true if no other vertex is inside the triangle
     */
    private boolean isEar() {
        final double[] a = new double[]{t1.get(0), t1.get(1)};
        final double[] b = new double[]{t2.get(0), t2.get(1)};
        final double[] c = new double[]{t3.get(0), t3.get(1)};

        for(int i=0; i<nbCoords-1; i++){
            //test only concave points
            if (!isConvex(i)){
                //check it's not one of the current segment
                if(i!=index && i!=indexNext && i!=indexPrevious){
                    final Vector<?> v = working[coords[i]];
                    final double[] p = new double[]{v.get(0),v.get(1)};
                    //a hole bridge duplicates its end points, a copy located on a triangle vertex does not block the ear
                    if (Arrays.equals(p, a) || Arrays.equals(p, b) || Arrays.equals(p, c)) {
                        continue;
                    }
                    //check if it's in the segment triangle
                    if(Maths.inTriangle(a, b, c, p)){
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
