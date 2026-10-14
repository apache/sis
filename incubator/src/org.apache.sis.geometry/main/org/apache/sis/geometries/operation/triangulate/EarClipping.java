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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import static org.apache.sis.maths.Vectors.*;
import org.apache.sis.util.ArraysExt;


/**
 * Origin : Adapted from Unlicense-Lib
 *
 * Triangulation using ear clipping algorithm.
 * http://en.wikipedia.org/wiki/Polygon_triangulation
 *
 * @author Johann Sorel
 */
public class EarClipping {

    private static final class SimplePolygon{
        private LineString outter;
        private final List<LineString> inners = new ArrayList();
        /**
         * direction of the outter loop
         */
        private boolean outterIsClockwise;
    }

    private final List<Vector[]> triangles = new ArrayList<>();

    /**
     * A path can be composed of multiple parts.
     * Each part is a closed polyline with inner holes.
     */
    private final List parts = new ArrayList();

    /** number of coordinates, may be inferior to coords length. */
    private int nbCoords;
    /** current coordinates to triangulate. */
    private Vector[] coords;
    /**
     * store the type of angle made by the coordinate
     * avoid recalculate it each time.
     * 0 : not calculated
     * 1 : convex
     * 2 : concave
     */
    private int[] coordType;

    /**
     * Order of the coordinates used by the triangulation, as indices of the coordinates of the
     * polygon: the triangulation works on the two first ones. The polygon is projected on the
     * coordinate plane where it has its largest area, so that a vertical polygon is not degenerate.
     * {@code null} if the polygon is triangulated in its (x,y) plane, without reordering.
     */
    private int[] axes;

    //current segment analyzed
    private int indexPrevious;
    private int index;
    private int indexNext;
    private Vector t1; //previous point
    private Vector t2; //point at index
    private Vector t3; //next point

    private void reset(){
        //reset values
        triangles.clear();
        nbCoords = 0;
        coords = null;
        coordType = null;
        parts.clear();
    }

    /**
     *
     * @param geometry geometry to triangulate
     * @return List of Coordinate[] for each triangle.
     */
    public List<Vector[]> triangulate(Polygon geometry) {

        reset();

        final SimplePolygon part = new SimplePolygon();
        //copy collection to avoid modifications
        part.outter = geometry.getExteriorRing();

        final int nbHole = geometry.getNumInteriorRing();

        for(int i=0;i<nbHole;i++){
            final LineString inner = geometry.getInteriorRingN(i);
            part.inners.add(inner);
        }

        axes = workingAxes(geometry.getExteriorRing().getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION));
        run(part);

        if (axes != null) {
            //triangles share their vertices, so each one is restored in a new vector.
            for (final Vector[] triangle : triangles) {
                for (int i = 0; i < triangle.length; i++) {
                    triangle[i] = fromWorking(triangle[i]);
                }
            }
        }
        return triangles;
    }

    /**
     * Returns the order of the coordinates to use for triangulating a polygon with the given positions,
     * or {@code null} if the polygon can be triangulated in its (x,y) plane. The axis along which the
     * normal of the polygon is the largest, computed with Newell's method, is moved after the two
     * axes of the projection plane.
     */
    private static int[] workingAxes(final Array positions) {
        final int dim = positions.getDimension();
        if (dim < 3) {
            return null;
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
            return null;
        }
        final int drop = (nx >= ny) ? 0 : 1;
        final int[] axes = new int[dim];
        int k = 0;
        for (int i = 0; i < 3; i++) {
            if (i != drop) axes[k++] = i;
        }
        axes[k++] = drop;
        for (int i = 3; i < dim; i++) {
            axes[k++] = i;
        }
        return axes;
    }

    /**
     * Returns a copy of the given position with its coordinates in the order used by the triangulation.
     */
    private Vector<?> toWorking(final Vector<?> position) {
        final Vector<?> copy = position.copy();
        if (axes != null) {
            for (int i = 0; i < axes.length; i++) {
                copy.set(i, position.get(axes[i]));
            }
        }
        return copy;
    }

    /**
     * Returns a copy of the given position with its coordinates restored in the order of the polygon.
     */
    private Vector<?> fromWorking(final Vector<?> position) {
        final Vector<?> copy = position.copy();
        for (int i = 0; i < axes.length; i++) {
            copy.set(axes[i], position.get(i));
        }
        return copy;
    }

    /**
     * TODO not efficient, improve performances.
     */
    public MeshPrimitive.Triangles toMesh(Polygon polygon) {
        final List<Vector[]> list = triangulate(polygon);

        final CoordinateReferenceSystem crs = polygon.getCoordinateReferenceSystem();
        final int dimension = crs.getCoordinateSystem().getDimension();
        final Array positions = NDArrays.of(crs, new double[list.size()*3*dimension]);
        for (int i = 0, k = 0, n = list.size(); i < n; i++, k+=3) {
            final Vector[] t = list.get(i);
            positions.set(k+0, t[0]);
            positions.set(k+1, t[1]);
            positions.set(k+2, t[2]);
        }

        final MeshPrimitive.Triangles triangles = new MeshPrimitive.Triangles();
        triangles.setPositions(positions);

        return triangles;
    }

    private void run(SimplePolygon part){

        //build a single geometry linking inner holes.
        //the array stream reuses a single vector for all positions, so each one is copied.
        final List<Vector<?>> borderCoords = new ArrayList<>();
        part.outter.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).stream(false)
                .map(this::toWorking).forEach(borderCoords::add);

        //copy the holes in the same way, in mutable lists since their last point is removed below,
        //and sort them by decreasing minimum x value.
        final List<List<Vector<?>>> holes = new ArrayList<>();
        for (final LineString inner : part.inners) {
            holes.add(inner.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).stream(false)
                    .map(this::toWorking).collect(Collectors.toCollection(ArrayList::new)));
        }
        holes.sort(Comparator.comparingDouble((List<Vector<?>> h) ->
                h.stream().mapToDouble((v) -> v.get(0)).min().orElse(Double.NaN)).reversed());

        //attach holes to the main geometry
        for (final List<Vector<?>> loop : holes) {
            //we must find the minimum x coordinate in the inner loop
            int index = 0;
            Vector min = loop.get(index);
            for(int k=1,p=loop.size();k<p;k++){
                Vector candidate = (Vector) loop.get(k);
                if (candidate.get(0) < min.get(0)) {
                    min = candidate;
                    index = k;
                }
            }

            //now find the closest point on the outter loop
            final List line2 = new ArrayList();
            line2.add(min);line2.add(min);
            final double[] buffer1 = new double[2];
            final double[] buffer2 = new double[2];
            final double[] ratio = new double[2];
            final int[] offset = new int[2];
            nearest(borderCoords, buffer1, line2, buffer2, ratio, offset, 0.000000001);

            //the cut starts on the outter loop segment nearest to the hole. The point found above has only
            //the two working coordinates, all coordinates are interpolated along that segment.
            final Vector<?> start = borderCoords.get(offset[0]);
            final Vector<?> end   = borderCoords.get(offset[0] + 1);
            final double length = Math.hypot(end.get(0) - start.get(0), end.get(1) - start.get(1));
            final double ratioAlong = (length > 0) ? Math.hypot(buffer1[0] - start.get(0), buffer1[1] - start.get(1)) / length : 0;
            final Vector<?> bridge = start.copy();
            for (int d = 0; d < bridge.getDimension(); d++) {
                bridge.set(d, start.get(d) + ratioAlong * (end.get(d) - start.get(d)));
            }

            //make a cut from outter loop to inner loop
            int insertIndex = offset[0]+1;
            borderCoords.add(insertIndex, bridge);
            insertIndex++;
            //remove the duplicateion inner loop end point
            loop.remove(loop.size()-1);
            for(int k=index,p=loop.size();k<p;k++,insertIndex++){
                borderCoords.add(insertIndex,loop.get(k));
            }
            for(int k=0;k<=index;k++,insertIndex++){
                borderCoords.add(insertIndex,loop.get(k));
            }
            borderCoords.add(insertIndex, bridge.copy());

        }

        //remove any neighor points overlaping
        Vector t = borderCoords.get(0);
        for(int i=1,n=borderCoords.size();i<n;i++){
            Vector candidate = borderCoords.get(i);
            if(candidate.equals(t)){
                borderCoords.remove(i);
                i--;
                n--;
            }else{
                t = candidate;
            }
        }



        final boolean clockwise = Maths.isClockWise(borderCoords);

        nbCoords = borderCoords.size();
        coordType = new int[nbCoords];
        coords = borderCoords.toArray(Vector[]::new);

        //flip coordinates if not clockwise
        if(!clockwise){
            ArraysExt.reverse(coords);
        }

        //now cut ears progressively
        int nbcut = 0;
        while(nbCoords > 3){
            nbcut = 0;
            for(index=0;index<nbCoords-2 && nbCoords>3;){
                indexPrevious = (index==0) ? nbCoords-2 : index-1 ;
                indexNext = index+1;

                t1 = coords[indexPrevious];
                t2 = coords[index];
                t3 = coords[indexNext];

                if(isConvex() && isEar()){
                    //reset angle type, we remove a point so it might change
                    coordType[indexPrevious]=0;
                    coordType[indexNext]=0;

                    triangles.add(new Vector[]{t1,t2,t3});
                    removeWithin(coords, index);
                    removeWithin(coordType, index);
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
     * @return true if  currentsegment is convex
     */
    private boolean isConvex(){
        if(coordType[index]==0){
            //calculate angle type
            final double side = Maths.lineSide(t1, t3, t2);
            coordType[index] = side>0 ? 1 : 2;
        }
        return coordType[index] == 1;
    }

    /**
     * @return true if segment is convex
     */
    private boolean isConvex(int idx){
        if(coordType[idx]==0){
            //calculate angle type
            final Vector s1 = coords[(idx==0) ? (nbCoords-2) : (idx-1)];
            final Vector s2 = coords[idx];
            final Vector s3 = coords[idx+1];
            final double side = Maths.lineSide(s1, s3, s2);
            coordType[idx] = side>0 ? 1 : 2;
        }
        return coordType[idx] == 1;
    }

    /**
     * Check this segment triangle is an ear.
     * Does not contain any other point.
     * @return
     * @throws OperationException
     */
    private boolean isEar() throws IllegalArgumentException {
        final double[] a = new double[]{t1.get(0), t1.get(1)};
        final double[] b = new double[]{t2.get(0), t2.get(1)};
        final double[] c = new double[]{t3.get(0), t3.get(1)};

        for(int i=0; i<nbCoords-1; i++){
            //test only concave points
            if (!isConvex(i)){
                //check it's not one of the current segment
                if(i!=index && i!=indexNext && i!=indexPrevious){
                    final double[] p = new double[]{coords[i].get(0),coords[i].get(1)};
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


    /**
     * Remove element at index.
     * Move elements after index by -1.
     * Last element is set to null
     *
     * @param values original array
     * @param index removed element index
     * @return removed object
     */
    public static Object removeWithin(Object[] values, int index) {
        final Object removedValue = values[index];
        if(index+1 < values.length){
            copy(values, index+1, values.length-index-1, values, index);
        }
        values[values.length-1] = null; //remove reference
        return removedValue;
    }

    /**
     * Remove element at index.
     * Move elements after index by -1.
     * Last element is set to zero
     *
     * @param values original array
     * @param index removed element index
     * @return removed value
     */
    public static int removeWithin(int[] values, int index) {
        final int removedValue = values[index];
        if(index+1 < values.length){
            copy(values, index+1, values.length-index-1, values, index);
        }
        values[values.length-1] = 0;
        return removedValue;
    }

    /**
     * Copy a portion of given source array of length elements from soffset index, to
     * the target array beginning to toffset index.
     * If needed, the returned array is padded with zeros to obtain the required
     * length.
     *
     * @param source not null
     * @param soffset offset to start copying
     * @param length number of elemnts to copy
     * @param target array where to insert new elements
     * @param toffset start insert index
     * @return the target array with copied values.
     */
    public static Object[] copy(Object[] source, int soffset, int length, Object[] target, int toffset){
        for(int i=soffset;i<soffset+length;i++,toffset++){
            target[toffset] = source[i];
        }
        return target;
    }

    /**
     * Copy a portion of given source array of length elements from soffset index, to
     * the target array beginning to toffset index.
     * If needed, the returned array is padded with zeros to obtain the required
     * length.
     *
     * @param source not null
     * @param soffset offset to start copying
     * @param length number of elemnts to copy
     * @param target array where to insert new elements
     * @param toffset start insert index
     * @return the target array with copied values.
     */
    public static int[] copy(int[] source, int soffset, int length, int[] target, int toffset){
        for(int i=soffset;i<soffset+length;i++,toffset++){
            target[toffset] = source[i];
        }
        return target;
    }

    /**
     * Copy a portion of given source array of length elements from soffset index, to
     * the target array beginning to toffset index.
     * If needed, the returned array is padded with zeros to obtain the required
     * length.
     *
     * @param source not null
     * @param soffset offset to start copying
     * @param length number of elemnts to copy
     * @param target array where to insert new elements
     * @param toffset start insert index
     * @return the target array with copied values.
     */
    public static double[] copy(double[] source, int soffset, int length, double[] target, int toffset){
        for(int i=soffset;i<soffset+length;i++,toffset++){
            target[toffset] = source[i];
        }
        return target;
    }

    /**
     * Find nearest points between two lines.
     * <br>
     * See Distance.
     *
     * @param line1Coords line 1 coordinates
     * @param buffer1 buffer to store nearest point on line 1
     * @param line2Coords line 2 coordinates
     * @param buffer2 buffer to store nearest point on line 2
     * @param ratio neareast points ratio between line start and end
     * @param offset : will store the segment offset of the nearest points
     * @param epsilon tolerance factor
     */
    public static void nearest(List<Vector<?>> line1Coords, double[] buffer1,
                                List<Vector<?>> line2Coords, double[] buffer2,
                                double[] ratio, int[] offset, double epsilon){

        double distance = Double.MAX_VALUE;

        final double[] tempRatio = new double[2];
        final double[] tempC1 = new double[buffer1.length];
        final double[] tempC2 = new double[buffer1.length];

        final int nb1 = line1Coords.size()-1;
        final int nb2 = line2Coords.size()-1;

        for(int i=0;i<nb1;i++){
            final Vector<?> s1 = line1Coords.get(i);
            final Vector<?> e1 = line1Coords.get(i+1);
            for(int k=0;k<nb2;k++){
                final Vector<?> s2 = line2Coords.get(k);
                final Vector<?> e2 = line2Coords.get(k+1);

                final double dist = Math.sqrt(distanceSquare(
                                new double[]{s1.get(0),s1.get(1)}, new double[]{e1.get(0),e1.get(1)}, tempC1,
                                new double[]{s2.get(0),s2.get(1)}, new double[]{e2.get(0),e2.get(1)}, tempC2,
                                tempRatio,epsilon));
                if(dist<distance){
                    //keep informations
                    distance = dist;
                    offset[0] = i;
                    offset[1] = k;
                    copy(tempC1, 0, tempC1.length, buffer1, 0);
                    copy(tempC2, 0, tempC2.length, buffer2, 0);
                }
            }
        }

    }

    /**
     * Calculate square distance between two line segments.
     *
     * Adapted from book : Real-TimeCollision Detection by Christer Ericson
     * (ClosestPtSegmentSegment p.149)
     *
     * @param line1Start line 1 start point
     * @param line1End line 1 start point
     * @param buffer1 closest point on line 1
     * @param line2Start line 1 start point
     * @param line2End line 1 start point
     * @param buffer2 closest point on line 2
     * @param ratio size 2 , for each line,
     *  ratio [0..1] of the closest point position between start and end points.
     * @param epsilon tolerance
     * @return distance
     */
    public static double distanceSquare(double[] line1Start, double[] line1End, double[] buffer1,
                                   double[] line2Start, double[] line2End, double[] buffer2,
                                   double[] ratio, double epsilon) {
        final double[] d1 = subtract(line1End,line1Start); // Direction vector of segment S1
        final double[] d2 = subtract(line2End,line2Start); // Direction vector of segment S2
        final double[] r = subtract(line1Start, line2Start);
        final double a = dot(d1, d1); // Squared length of segment S1, always nonnegative
        final double e = dot(d2, d2); // Squared length of segment S2, always nonnegative
        final double f = dot(d2, r);
        // Check if either or both segments degenerate into points
        if (a <= epsilon && e <= epsilon) {
            // Both segments degenerate into points
            ratio[0] = 0;
            ratio[1] = 0;
            copy(line1Start,0,line1Start.length,buffer1,0);
            copy(line2Start,0,line2Start.length,buffer2,0);
            final double[] t = subtract(buffer1,buffer2);
            return dot(t,t);
        }
        if (a <= epsilon) {
            // First segment degenerates into a point
            ratio[0] = 0;
            ratio[1] = clip( f/e, 0, 1);
        } else {
            final double c = dot(d1, r);
            if (e <= epsilon) {
                // Second segment degenerates into a point
                ratio[0] = clip( -c/a, 0, 1);
                ratio[1] = 0;
            } else {
                // The general nondegenerate case starts here
                final double b = dot(d1, d2);
                final double denom = a*e-b*b; // Always nonnegative
                // If segments not parallel, compute closest point on L1 to L2 and
                // clamp to segment S1. Else pick arbitrary s (here 0)
                if (denom != 0d) {
                    ratio[0] = clip((b*f - c*e) / denom, 0.0f, 1.0f);
                } else {
                    ratio[0] = 0;
                }
                // Compute point on L2 closest to S1(s) using
                // t = Dot((P1 + D1*s) - P2,D2) / Dot(D2,D2) = (b*s + f) / e
                ratio[1] = (b*ratio[0] + f) / e;

                //If t in [0,1] done. Else clamp t, recompute s for the new value
                //of t using s = Dot((P2 + D2*t) - P1,D1) / Dot(D1,D1)= (t*b - c) / a
                //and clamp s to [0, 1]
                if(ratio[1] < 0){
                    ratio[1] = 0;
                    ratio[0] = clip(-c / a, 0.0f, 1.0f);
                }else if(ratio[1] > 1){
                    ratio[1] = 1;
                    ratio[0] = clip((b - c) / a, 0.0f, 1.0f);
                }

            }
        }

        add(line1Start, scale(d1, ratio[0]), buffer1);
        add(line2Start, scale(d2, ratio[1]), buffer2);
        final double[] t = subtract(buffer1,buffer2);
        return dot(t,t);
    }

    /**
     * Clip returns:
     * <ul>
     * <li>min if the value is lower than min</li>
     * <li>value if the value is between min and max</li>
     * <li>max if the value is higher than max</li>
     * </ul>
     * @param val the value
     * @param min minimum value
     * @param max maximum value
     * @return clipped value
     */
    public static double clip(double val, double min, double max){
        if(val<min) return min;
        if(val>max) return max;
        return val;
    }
}
