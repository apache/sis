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
package org.apache.sis.geometries;

import java.util.Collections;
import java.util.List;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.privy.AbstractGeometry;
import org.apache.sis.geometries.math.Maths;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.Vector;
import org.apache.sis.geometries.math.Vector2D;
import org.apache.sis.geometries.math.Vector3D;
import org.apache.sis.geometries.math.Vectors;


/**
 * A triangle geometry.
 *
 * TODO : declare and implement all methods from OGC Simple Feature Access
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Triangle", specification=ISO_19107) // section 8.1.6
public interface Triangle extends Polygon {

    @Override
    default String getGeometryType() {
        return "TRIANGLE";
    }

    /**
     * Triangles points.
     * First and last point are identical.
     *
     * @return empty if triangle is empty, or of size 4.
     */
    @Override
    LinearRing getExteriorRing();

    @Override
    default List<Curve> getInteriorRings() {
        return Collections.emptyList();
    }

    @Override
    default CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return getExteriorRing().getCoordinateReferenceSystem();
    }

    @Override
    default void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        getExteriorRing().setCoordinateReferenceSystem(cs);
    }

    @Override
    default Envelope getEnvelope() {
        final PointSequence exterior = getExteriorRing().getPoints();
        final Tuple<?> first = exterior.getPosition(0);
        final BBox env = new BBox(first, first);
        env.add(exterior.getPosition(1));
        env.add(exterior.getPosition(2));
        env.setCoordinateReferenceSystem(getCoordinateReferenceSystem());
        return env;
    }

    @Override
    default double getArea() {
        final PointSequence points = getExteriorRing().getPoints();
        final Tuple<?> a = points.getPosition(0);
        final Tuple<?> b = points.getPosition(1);
        final Tuple<?> c = points.getPosition(2);
        final double area = (
                      a.get(0) * (b.get(1) - c.get(1))
                    + b.get(0) * (c.get(1) - a.get(1))
                    + c.get(0) * (a.get(1) - b.get(1))
                    ) / 2.0;
        return Math.abs(area);
    }

    /**
     * Compute point distance to triangle.
     * @param pt point to evaluate
     * @return point distance to triangle
     */
    default double distance(Tuple pt) {
        final PointSequence exterior = getExteriorRing().getPoints();
        final Tuple<?> p0 = exterior.getPosition(0);
        final Tuple<?> p1 = exterior.getPosition(1);
        final Tuple<?> p2 = exterior.getPosition(2);
        Vector<?> normal = Maths.calculateNormal(p0, p1, p2);
        double planD = normal.dot(p0);
        return Maths.distance(pt, normal, planD);
    }

    /**
     * Interpolate a point in the triangle.
     *
     * @param weights each corner barycentric weights
     * @return interpolated record
     */
    default Point interpolate(double[] weights) {
        return new InterpolatedPoint(this, weights);
    }

    /**
     * Test if point is inside triangle and return it's barycentric weights.
     * This method first test a bounding box intersection then using side algorithm
     * and finally segment distant using given tolerance.
     *
     * Explications provided here :
     * https://totologic.blogspot.com/2014/01/accurate-point-in-triangle-test.html
     *
     * @param x1 triangle first point X
     * @param y1 triangle first point Y
     * @param x2 triangle second point X
     * @param y2 triangle second point Y
     * @param x3 triangle third point X
     * @param y3 triangle third point Y
     * @param x point X
     * @param y point Y
     * @param epsilon bounding box margin
     * @param nullIfOutside return null if point is outside the triangle
     * @return [a,b,c] weights
     */
    public static double[] getBarycentricValue2D(double x1, double y1, double x2, double y2, double x3, double y3, double x, double y, double epsilon, boolean nullIfOutside) {
        if (nullIfOutside) {
            if (!Maths.isPointInTriangle_BoundingBox(x1, y1, x2, y2, x3, y3, x, y, epsilon)) {
                return null;
            }
            final double[] bary = Maths.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, x, y);
            final boolean inTriangle = (bary[1] >= 0.0 && bary[2] >= 0.0 && (bary[1] + bary[2]) <= 1.0);
            if (inTriangle) {
                return bary;
            }

            if (epsilon > 0.0) {
                //point might be very close to segments
                if (Maths.distanceSquare(x1, y1, x2, y2, x, y) <= epsilon) {
                    //compute weights based on distance, clamp to ends
                    double ratio = Maths.clamp(Maths.projectionRatio(x1, y1, x2, y2, x, y), 0.0, 1.0);
                    bary[0] = 1.0 - ratio;
                    bary[1] = ratio;
                    bary[2] = 0.0;
                    return bary;
                }
                if (Maths.distanceSquare(x2, y2, x3, y3, x, y) <= epsilon) {
                    //compute weights based on distance, clamp to ends
                    double ratio = Maths.clamp(Maths.projectionRatio(x2, y2, x3, y3, x, y), 0.0, 1.0);
                    bary[0] = 0.0;
                    bary[1] = 1.0 - ratio;
                    bary[2] = ratio;
                    return bary;
                }
                if (Maths.distanceSquare(x3, y3, x1, y1, x, y) <= epsilon) {
                    //compute weights based on distance, clamp to ends
                    double ratio = Maths.clamp(Maths.projectionRatio(x3, y3, x1, y1, x, y), 0.0, 1.0);
                    bary[0] = ratio;
                    bary[1] = 0.0;
                    bary[2] = 1.0 - ratio;
                    return bary;
                }
            }

            //point is outside triangle
            return null;
        } else {
            return Maths.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, x, y);
        }
    }

    public static Double interpolate2D(Vector3D.Double a, Vector3D.Double b, Vector3D.Double c, Vector2D.Double p){
        final double v0x = b.x-a.x;
        final double v0y = b.y-a.y;
        final double v1x = c.x-a.x;
        final double v1y = c.y-a.y;
        final double v2x = p.x-a.x;
        final double v2y = p.y-a.y;
        final double d00 = v0x * v0x + v0y * v0y;
        final double d01 = v0x * v1x + v0y * v1y;
        final double d11 = v1x * v1x + v1y * v1y;
        final double d20 = v2x * v0x + v2y * v0y;
        final double d21 = v2x * v1x + v2y * v1y;
        final double denom = d00 * d11 - d01 * d01;
        final double v = (d11 * d20 - d01 * d21) / denom;
        final double w = (d00 * d21 - d01 * d20) / denom;
        final double u = 1.0 - v - w;

        if (v >= 0.0 && w >= 0.0 && (v + w) <= 1.0) {
            //point is in this triangle, interpolate Z
            return u * a.z
                 + v * b.z
                 + w * c.z;
         }
        return null;
    }

    @Override
    default String asText() {
        final PointSequence exterior = getExteriorRing().getPoints();
        final StringBuilder sb = new StringBuilder("TRIANGLE ((");
        AbstractGeometry.toText(sb, exterior.getPosition(0));
        sb.append(',');
        AbstractGeometry.toText(sb, exterior.getPosition(1));
        sb.append(',');
        AbstractGeometry.toText(sb, exterior.getPosition(2));
        sb.append("))");
        return sb.toString();
    }

    /**
     */
    default String asTextPolygon() {
        return Polygon.super.asText();
    }

    public static final class InterpolatedPoint implements Point {

        private static final double[][] CORNERS = new double[][]{
            {1,0,0},
            {0,1,0},
            {0,0,1}};

        private final Triangle triangle;
        private final double[] weights;
        private final int cornerIdx;

        InterpolatedPoint(Triangle triangle, int cornerIdx) {
            this.triangle = triangle;
            this.cornerIdx = cornerIdx;
            this.weights = CORNERS[cornerIdx];
        }

        public InterpolatedPoint(Triangle triangle, double[] weights) {
            this.triangle = triangle;
            this.weights = weights;
            this.cornerIdx = -1;
        }

        public Triangle getTriangle() {
            return triangle;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Tuple getPosition() {
            final PointSequence points = triangle.getExteriorRing().getPoints();
            switch (cornerIdx) {
                case -1 :
                    return interpolate(
                        points.getPosition(0),
                        points.getPosition(1),
                        points.getPosition(2),
                        false);
                default :
                    return points.getPosition(cornerIdx);
            }
        }

        /**
         * NORMAL and TANGENT attributes interpolation will be normalized.
         *
         * @param name attribute name, must be one of the sample dimension names
         * @return triangle corner attribute.
         */
        @Override
        public Tuple getAttribute(String name) {
            final PointSequence points = triangle.getExteriorRing().getPoints();
            switch (cornerIdx) {
                case -1 :
                    return interpolate(
                        points.getAttribute(0,name),
                        points.getAttribute(1,name),
                        points.getAttribute(2,name),
                        AttributesType.ATT_NORMAL.equals(name) || AttributesType.ATT_TANGENT.equals(name));
                default :
                    return points.getAttribute(cornerIdx, name);
            }
        }

        @Override
        public void setAttribute(String name, Tuple tuple) {
            throw new UnsupportedOperationException("Not supported on interpolated points.");
        }

        private Tuple interpolate(Tuple a, Tuple b, Tuple c, boolean normalize) {
            if (a == null) {
                //may happen if TIN has 0 sample dimensions.
                return null;
            }
            Tuple buffer = a.copy();
            final int dimension = a.getDimension();
            switch (dimension) {
                default :
                    for (int i = 3; i < dimension; i++) {
                        buffer.set(i, a.get(i) * weights[0] + b.get(i) * weights[1] + c.get(i) * weights[2]);
                    }
                case 3 :
                    buffer.set(2, a.get(2) * weights[0] + b.get(2) * weights[1] + c.get(2) * weights[2]);
                case 2 :
                    buffer.set(1, a.get(1) * weights[0] + b.get(1) * weights[1] + c.get(1) * weights[2]);
                case 1 :
                    buffer.set(0, a.get(0) * weights[0] + b.get(0) * weights[1] + c.get(0) * weights[2]);
                case 0 : //do nothing
            }

            if (normalize) Vectors.castOrWrap(buffer).normalize();
            return buffer;
        }

        @Override
        public AttributesType getAttributesType() {
            return triangle.getAttributesType();
        }

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return triangle.getCoordinateReferenceSystem();
        }

        @Override
        public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

}
