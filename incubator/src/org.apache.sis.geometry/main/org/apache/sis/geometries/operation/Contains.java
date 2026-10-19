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
package org.apache.sis.geometries.operation;

import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.operation.triangulate.EarClipping;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Cursor;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Vector;


/**
 * Contains processors.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Contains {

    /**
     * The axes of the (x,y) plane, for the tests done in two dimensions.
     */
    private static final int[] XY = {0, 1};

    private Contains(){}

    /**
     * Test if point is within a ring using Winding Number algorithm, in the plane of the two given axes.
     * The ring and the point can have more dimensions: for example a ring in a vertical plane can be tested
     * in the plane given by {@link Maths#projectionAxes(double[])}. A point on the ring gets an arbitrary answer.
     * http://geomalgorithms.com/a03-_inclusion.html
     * http://en.wikipedia.org/wiki/Point_in_polygon
     *
     * @param ring positions of the closed ring, the last one at the location of the first one
     * @param point the point to test
     * @param axes indices of the two coordinates of the plane in which the test is done
     * @return whether the point is inside the ring
     */
    public static boolean contains(Array ring, ReadOnly.Vector<?> point, int[] axes) {
        final long n = ring.getLength();
        if (n == 0) {
            return false;
        }
        final int u = axes[0], v = axes[1];
        final double px = point.get(u), py = point.get(v);
        final Cursor cursor = ring.cursor();
        cursor.moveTo(0);
        Vector<?> current = cursor.samples();
        double x = current.get(u), y = current.get(v);
        int windingNumber = 0;
        for (long i = 1; i < n; i++){
            final double previousX = x, previousY = y;
            cursor.moveTo(i);
            current = cursor.samples();
            x = current.get(u);
            y = current.get(v);

            if (previousY <= py){
                if (y > py){
                    if (Maths.lineSide(previousX, previousY, x, y, px, py) > 0){
                        windingNumber++;
                    }
                }
            } else {
                if (y <= py){
                    if (Maths.lineSide(previousX, previousY, x, y, px, py) < 0){
                        windingNumber--;
                    }
                }
            }
        }

        //if 0 point is outside
        return windingNumber != 0;
    }

    /**
     * Returns the generalized winding number of a surface made of triangles around a point: the sum of the solid
     * angles of the triangles seen from the point, divided by 4π. The number is 1 inside a closed surface whose
     * triangles are oriented outward (counterclockwise seen from outside), −1 if they are oriented inward, and 0
     * outside. The numbers of several surfaces add: a point inside the void of a solid is inside the outward
     * surface of the solid and inside the inward surface of the void, which gives 0. The number of a surface
     * which is not closed is between those values, and the number of a point on a surface is a fraction,
     * for example ½ on a face.
     *
     * <p>The solid angle of each triangle is computed with the formula of Van Oosterom and Strackee.
     * A triangle whose plane contains the point is ignored: seen from its plane, a triangle has no solid
     * angle, unless the point is on it, which is the case of a point on the surface.</p>
     *
     * @param xyz        the coordinates of the vertices, three per vertex.
     * @param triangles  indices of the three vertices of each triangle, one triangle after the other.
     * @param point      the point, of three coordinates.
     * @return the winding number, close to an integer for a point which is not on a closed surface.
     *
     * @see <a href="https://doi.org/10.1145/2461912.2461916">Jacobson et al., Robust inside-outside segmentation using generalized winding numbers</a>
     */
    public static double windingNumber(final double[] xyz, final int[] triangles, final double[] point) {
        double sum = 0;
        for (int i = 0; i + 2 < triangles.length; i += 3) {
            final int a = triangles[i] * 3, b = triangles[i+1] * 3, c = triangles[i+2] * 3;
            final double ax = xyz[a] - point[0], ay = xyz[a+1] - point[1], az = xyz[a+2] - point[2];
            final double bx = xyz[b] - point[0], by = xyz[b+1] - point[1], bz = xyz[b+2] - point[2];
            final double cx = xyz[c] - point[0], cy = xyz[c+1] - point[1], cz = xyz[c+2] - point[2];
            final double la = Math.sqrt(ax*ax + ay*ay + az*az);
            final double lb = Math.sqrt(bx*bx + by*by + bz*bz);
            final double lc = Math.sqrt(cx*cx + cy*cy + cz*cz);
            final double det = ax * (by*cz - bz*cy) + ay * (bz*cx - bx*cz) + az * (bx*cy - by*cx);
            if (det != 0) {
                final double den = la*lb*lc + (ax*bx + ay*by + az*bz) * lc + (ax*cx + ay*cy + az*cz) * lb + (bx*cx + by*cy + bz*cz) * la;
                sum += Math.atan2(det, den);        // half of the solid angle.
            }
        }
        return sum / (2 * Math.PI);
    }

    /**
     * Polyhedron contains Point test. The point is inside the polyhedron if it is inside the exterior shell and
     * outside all the interior shells, as told by the {@linkplain #windingNumber winding number} of each shell.
     * Each shell is tested alone, so the orientation of its faces does not matter as long as it is consistent.
     * A point on the boundary of the polyhedron gets an arbitrary answer.
     *
     * @throws OperationException if the geometries do not have the same CRS, or are not three-dimensional,
     *         or if a face cannot be triangulated.
     */
    public static boolean contains(Polyhedron polyhedron, Point candidate) throws OperationException {
        ProcessorUtils.ensureSameCRS(polyhedron, candidate);
        final double[] point = candidate.getPosition().toArrayDouble();
        if (point.length != 3) {
            throw new OperationException("A polyhedron contains a point in 3 dimensions only.");
        }
        if (!isInside(polyhedron.getExteriorShell(), point)) {
            return false;
        }
        for (final GeometryCollection<Polygon> shell : polyhedron.getInteriorShells()) {
            if (isInside(shell, point)) {
                //point is within a void
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the given point is inside the given closed shell, whatever the orientation of its faces.
     * The winding numbers of the faces are added, each face being split in triangles.
     */
    private static boolean isInside(final GeometryCollection<Polygon> shell, final double[] point) {
        final EarClipping triangulator = new EarClipping();
        double winding = 0;
        for (int i = 0, n = shell.getNumGeometries(); i < n; i++) {
            final EarClipping.Triangulation face = triangulator.triangulateIndexed(shell.getGeometryN(i));
            final double[] xyz = face.vertices().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble();
            winding += windingNumber(xyz, face.triangles(), point);
        }
        return Math.abs(winding) > 0.5;
    }

    /**
     * Polygon contains Point test.
     */
    public static boolean contains(Polygon polygon, Point candidate) throws OperationException {
        ProcessorUtils.ensureSameCRS2D(polygon, candidate);

        { //check exterior
            final Array coords = polygon.getExteriorRing().getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION);
            if (!contains(coords, candidate.getPosition(), XY)) {
                //point is outside the exterior ring
                return false;
            }
        }

        { //check holes
            for (int i = 0, n = polygon.getNumInteriorRing(); i < n; i++) {
                final LineString hole = polygon.getInteriorRingN(i);
                final Array coords = hole.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION);
                if (contains(coords, candidate.getPosition(), XY)) {
                    //point is within a hole
                    return false;
                }
            }
        }

        //point is inside polygon
        return true;
    }

}
