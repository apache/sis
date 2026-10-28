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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.internal.shared.DefaultDataPoints;
import org.apache.sis.geometries.internal.shared.DefaultNurbCurve;
import org.apache.sis.geometries.internal.shared.DefaultNurbSurface;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MeshPrimitiveVisitor;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.geometries.surface.PreparedTIN;
import org.apache.sis.geometries.surface.Triangle;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Cursor;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.Matrices;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import static org.apache.sis.maths.Maths.clamp;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Intersection {

    private Intersection(){}


    static Vector<?> subtract(final Vector<?> a, final Vector<?> b) {
        return a.subtract(b);
    }

    static Vector<?> scale(final Vector<?> a, final double s) {
        return a.scale(s);
    }

    /**
     * Inherit attributes from TIN.
     * The POSITION attribute is ignored.
     */
    private static void copyAttributes(PreparedTIN tin, MultiMeshPrimitive<?> primitives) {
        for (MeshPrimitive primitive : primitives.getComponents()) {
            copyAttributes(tin, primitive);
        }
    }

    /**
     * Inherit attributes from TIN.
     * The POSITION attribute is ignored.
     */
    private static void copyAttributes(PreparedTIN tin, MeshPrimitive primitive) {

        final DataPointsType attributesType = tin.getDataPointsType();
        final long nbVertex = primitive.getPositions().getLength();

        final List<String> attributeNames = new ArrayList<>(attributesType.getAttributeNames());
        if (attributeNames.size() == 1) {
            //no attributes to copy
            return;
        }
        attributeNames.remove(DataPointsType.ATT_POSITION);
        final String[] templateNames = new String[attributeNames.size()];
        final Array[] attributes = new Array[attributeNames.size()];
        for (int i = 0; i < templateNames.length; i++) {
            templateNames[i] = attributeNames.get(i);
            final Array ta = NDArrays.of(
                    attributesType.getAttributeSystem(templateNames[i]),
                    attributesType.getAttributeType(templateNames[i]),
                    nbVertex);
            attributes[i] = ta;
            primitive.setAttribute(templateNames[i], ta);
        }

        //interpolate attributes from TIN
        final PreparedTIN.Evaluator evaluator = tin.evaluator();
        new MeshPrimitiveVisitor(primitive) {
            @Override
            protected void visit(MeshPrimitive.Vertex vertex) {
                if (isVisited(vertex)) return;
                final Optional<Point> opt = evaluator.evaluate(vertex.getCoordinateReferenceSystem(), vertex.getPosition());
                if (opt.isEmpty()) return;
                final Point point = opt.get();
                final long index = vertex.getIndex();
                for (int i = 0; i < templateNames.length; i++) {
                    attributes[i].set(index, point.getAttribute(templateNames[i]));
                }
            }
        }.visit();
    }

    /**
     * Triangles with points.
     */
    public static MeshPrimitive.Points intersection(MeshPrimitive.Triangles p1, MeshPrimitive.Points p2) {
        ProcessorUtils.ensureSameCRS(p1, p2);

        final PreparedTIN pt = PreparedTIN.create(p1);

        final Array positions = p2.getPositions().copy();

        final MeshPrimitive.Points intersection = new MeshPrimitive.Points();

        //create a copy of the points positions
        intersection.setPositions(positions);

        //create an index only for points which intersect
        final PreparedTIN.Evaluator evaluator = pt.evaluator();
        final Cursor cursor = positions.cursor();
        final List<Integer> values = new ArrayList<>();
        while (cursor.next()) {
            final Vector<?> position = cursor.samples();
            if (evaluator.evaluate(position).isPresent()) {
                values.add(Math.toIntExact(cursor.coordinate()));
            }
        }
        if (!values.isEmpty()) {
            final Array index = NDArrays.ofUnsigned(1, values);
            intersection.setIndex(index);
        }

        //remove unused indices
        Geometries.compact(intersection);

        //copy attributes
        copyAttributes(pt, intersection);

        return intersection;
    }

    /**
     * Triangles with lines.
     */
    public static MeshPrimitive intersection(MeshPrimitive.Triangles triangles, MeshPrimitive.Lines lines) {
        ProcessorUtils.ensureSameCRS(triangles, lines);

        final PreparedTIN tin = PreparedTIN.create(triangles);

        final List<LineString> segments = new ArrayList<>();
        for (int i = 0, n = lines.getNumGeometries(); i < n; i++) {
            final LineString line = lines.getGeometryN(i);
            final Array segment = line.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION);
            final Vector<?> s1 = segment.get(0);
            final Vector<?> s2 = segment.get(1);

            try (Stream<Triangle> stream = tin.getPatches(line.getEnvelope())) {
                final Iterator<Triangle> iterator = stream.iterator();

                while (iterator.hasNext()) {
                    final Triangle triangle = iterator.next();
                    final Array corners = triangle.getExteriorRing().getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION);
                    final Vector<?> c0 = corners.get(0);
                    final Vector<?> c1 = corners.get(1);
                    final Vector<?> c2 = corners.get(2);
                    final List<Vector> clip = SutherlandHodgman.clip(Arrays.asList(s1,s2,s1), Arrays.asList(c0,c1,c2,c0));
                    if (clip.size() >= 2) {
                        //inherit attributes
                        final double x1 = c0.get(0);
                        final double y1 = c0.get(1);
                        final double x2 = c1.get(0);
                        final double y2 = c1.get(1);
                        final double x3 = c2.get(0);
                        final double y3 = c2.get(1);
                        final Vector<?> p1 = clip.get(0);
                        final Vector<?> p2 = clip.get(1);
                        final double[] bary1 = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, p1.get(0), p1.get(1), 0.0, false);
                        final double[] bary2 = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, p2.get(0), p2.get(1), 0.0, false);
                        final Point point1 = triangle.interpolate(bary1);
                        final Point point2 = triangle.interpolate(bary2);
                        segments.add(GeometryFactory.DEFAULT.createLineString(new DefaultDataPoints(point1, point2)));
                    }
                }
            } catch (TransformException ex) {
                throw new OperationException(ex.getMessage(), ex);
            }
        }

        final GeometryCollection<LineString> mline = GeometryFactory.DEFAULT.createMultiLineString(segments.toArray(LineString[]::new));
        final MeshPrimitive intersection = (MeshPrimitive) new GeometryProcessor().toPrimitive(mline, null, null);
        return intersection;
    }

    /**
     * Intersection of two segments in a plane. Each point of the intersection is given by its position on both
     * segments, as a ratio from 0 at the start of the segment to 1 at its end. A ratio is exactly 0 or 1 when the
     * point is an end of the segment.
     *
     * @param type           whether the segments are disjoint, meet at one point, or overlap along their common line.
     * @param startOnFirst   ratio on the first segment of the single point, or of the start of the overlap, NaN if none.
     * @param endOnFirst     ratio on the first segment of the end of the overlap, greater than {@code startOnFirst},
     *                       or {@code startOnFirst} for a single point.
     * @param startOnSecond  ratio on the second segment of the point at {@code startOnFirst}.
     * @param endOnSecond    ratio on the second segment of the point at {@code endOnFirst}.
     *
     * @see #segmentSegment2D(double[], double[], double[], double[], int[])
     */
    public record SegmentSegment(Type type, double startOnFirst, double endOnFirst, double startOnSecond, double endOnSecond) {
        /**
         * Kinds of intersection of two segments.
         */
        public enum Type {
            /** The segments do not intersect. */
            NONE,
            /** The segments cross or touch at a single point. */
            POINT,
            /** The segments are on the same line and share a part of it. */
            OVERLAP
        }

        /**
         * The intersection of disjoint segments.
         */
        public static final SegmentSegment NONE = new SegmentSegment(Type.NONE, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        /**
         * Returns the intersection made of a single point, at the given ratios on the two segments.
         */
        static SegmentSegment point(final double onFirst, final double onSecond) {
            return new SegmentSegment(Type.POINT, onFirst, onFirst, onSecond, onSecond);
        }
    }

    /**
     * Point where a segment touches a triangle, located on both. Edge <var>i</var> of the triangle goes from
     * its corner <var>i</var> to its corner (<var>i</var>+1) modulo 3.
     *
     * @param type     where the point is on the triangle, or why there is no point.
     * @param feature  index of the edge or of the corner of the triangle on which the point is, or -1.
     * @param ratio    position of the point on the segment, from 0 at its start to 1 at its end,
     *                 exactly 0 or 1 if the point is an end of the segment. NaN if there is no point.
     *
     * @see #segmentTriangle(double[], double[], double[], double[], double[], double)
     * @see #segmentInTriangle2D(double[], double[], double[], double[], double[], int[])
     */
    public record SegmentTriangle(Type type, int feature, double ratio) {
        /**
         * Where a segment touches a triangle.
         */
        public enum Type {
            /** The segment does not touch the triangle. */
            NONE,
            /** The segment lies in the plane of the triangle, and shall be intersected with it in that plane. */
            COPLANAR,
            /** The segment touches the triangle inside it. */
            INTERIOR,
            /** The segment touches the triangle on the edge given by the feature index. */
            EDGE,
            /** The segment touches the triangle on the corner given by the feature index. */
            VERTEX
        }

        /**
         * A segment which does not touch the triangle.
         */
        public static final SegmentTriangle NONE = new SegmentTriangle(Type.NONE, -1, Double.NaN);

        /**
         * A segment in the plane of the triangle.
         */
        public static final SegmentTriangle COPLANAR = new SegmentTriangle(Type.COPLANAR, -1, Double.NaN);

        /**
         * Returns the point inside the triangle, on an edge or on a corner, from the number of edges on which
         * the point is. This is used after testing the side of the point relative to the three edges.
         *
         * @param  zeros      number of edges on which the point is.
         * @param  zeroEdge   index of an edge on which the point is.
         * @param  otherEdge  index of an edge on which the point is not.
         * @param  ratio      position of the point on the segment.
         * @return the point, or {@code null} if it is on the three edges, which happens with a degenerate triangle.
         */
        static SegmentTriangle locate(final int zeros, final int zeroEdge, final int otherEdge, final double ratio) {
            switch (zeros) {
                case 0:  return new SegmentTriangle(Type.INTERIOR, -1, ratio);
                case 1:  return new SegmentTriangle(Type.EDGE, zeroEdge, ratio);
                case 2:  return new SegmentTriangle(Type.VERTEX, (otherEdge + 2) % 3, ratio);    // corner shared by the two other edges.
                default: return null;
            }
        }
    }

    /**
     * End of the intersection of two triangles: a point where an edge of one triangle touches the other triangle.
     *
     * @param edgeOfFirst  whether the edge belongs to the first triangle, the point being then on the second triangle.
     * @param edge         index of the edge, which goes from the corner of same index to the next corner.
     * @param contact      where the edge touches the other triangle, with the position of the point on the edge.
     * @param position     coordinates of the point, interpolated on the edge.
     *
     * @see #triangleTriangle(double[], double[], double[], double[], double[], double[], double)
     */
    public record TriangleEdgeContact(boolean edgeOfFirst, int edge, SegmentTriangle contact, double[] position) {
    }

    /**
     * Intersection of two triangles: nothing, a point or a segment, unless the triangles are in the same plane.
     *
     * @param type   kind of intersection.
     * @param start  first end of the intersection, or {@code null} if the triangles are disjoint or coplanar.
     * @param end    second end of the intersection, the same as {@code start} for a single point.
     *
     * @see #triangleTriangle(double[], double[], double[], double[], double[], double[], double)
     */
    public record TriangleTriangle(Type type, TriangleEdgeContact start, TriangleEdgeContact end) {
        /**
         * Kinds of intersection of two triangles.
         */
        public enum Type {
            /** The triangles do not intersect. */
            DISJOINT,
            /** The triangles touch at a single point. */
            POINT,
            /** The triangles cross or touch along a segment. */
            SEGMENT,
            /** The triangles are in the same plane, and shall be intersected in that plane. */
            COPLANAR
        }

        /** Intersection of disjoint triangles. */
        public static final TriangleTriangle DISJOINT = new TriangleTriangle(Type.DISJOINT, null, null);

        /** Intersection of triangles in the same plane. */
        public static final TriangleTriangle COPLANAR = new TriangleTriangle(Type.COPLANAR, null, null);
    }

    /**
     * Computes the intersection of two segments in the plane of the two given axes. The side tests are robust,
     * so whether the segments intersect, and whether a point of the intersection is an end of a segment, are exact.
     * Only the ratios of the points inside the segments are approximations.
     *
     * @param  a0    start of the first segment.
     * @param  a1    end of the first segment.
     * @param  b0    start of the second segment.
     * @param  b1    end of the second segment.
     * @param  axes  indices of the two coordinates of the plane, for example from {@link Maths#projectionAxes(double[])}.
     * @return the intersection of the two segments.
     */
    public static SegmentSegment segmentSegment2D(double[] a0, double[] a1, double[] b0, double[] b1, int[] axes) {
        final int u = axes[0], v = axes[1];
        //sides of the ends of each segment relative to the line of the other segment.
        final double d0 = side(b0, b1, a0, u, v), d1 = side(b0, b1, a1, u, v);
        final double e0 = side(a0, a1, b0, u, v), e1 = side(a0, a1, b1, u, v);
        if (sameSide(d0, d1) || sameSide(e0, e1)) {
            return SegmentSegment.NONE;
        }
        if ((d0 == 0 && d1 == 0) || (e0 == 0 && e1 == 0)) {
            return collinear(a0, a1, b0, b1, u, v);
        }
        return SegmentSegment.point(ratio(d0, d1), ratio(e0, e1));
    }

    /**
     * Computes the intersection of two segments on the same line. The positions are compared along the axis
     * where the segments are the longest, so that a line parallel to one of the axes is not degenerate.
     */
    private static SegmentSegment collinear(double[] a0, double[] a1, double[] b0, double[] b1, int u, int v) {
        final int k = (Math.abs(a1[u] - a0[u]) + Math.abs(b1[u] - b0[u]) >= Math.abs(a1[v] - a0[v]) + Math.abs(b1[v] - b0[v])) ? u : v;
        final double lower = Math.max(Math.min(a0[k], a1[k]), Math.min(b0[k], b1[k]));
        final double upper = Math.min(Math.max(a0[k], a1[k]), Math.max(b0[k], b1[k]));
        if (!(lower <= upper)) {
            return SegmentSegment.NONE;
        }
        double startOnFirst = along(a0[k], a1[k], lower), startOnSecond = along(b0[k], b1[k], lower);
        if (lower == upper) {
            return SegmentSegment.point(startOnFirst, startOnSecond);
        }
        double endOnFirst = along(a0[k], a1[k], upper), endOnSecond = along(b0[k], b1[k], upper);
        if (startOnFirst > endOnFirst) {
            //the first segment goes toward decreasing values along the axis.
            double t = startOnFirst;  startOnFirst  = endOnFirst;  endOnFirst  = t;
                   t = startOnSecond; startOnSecond = endOnSecond; endOnSecond = t;
        }
        return new SegmentSegment(SegmentSegment.Type.OVERLAP, startOnFirst, endOnFirst, startOnSecond, endOnSecond);
    }

    /**
     * Returns the position of a value between the values at the start and at the end of a segment along an axis,
     * exactly 0 or 1 at the ends.
     */
    private static double along(final double start, final double end, final double value) {
        if (value == start) return 0;
        if (value == end)   return 1;
        return (value - start) / (end - start);
    }

    /**
     * Returns the position of the point where a segment crosses a line or a plane, from the signed distances of
     * the segment ends to that line or plane, or any values proportional to those distances. The position is
     * exactly 0 or 1 if an end is on the line or plane.
     */
    private static double ratio(final double start, final double end) {
        if (start == 0) return 0;
        if (end   == 0) return 1;
        return clamp(start / (start - end), 0, 1);
    }

    /**
     * Returns whether two values are both strictly positive or both strictly negative.
     */
    private static boolean sameSide(final double s1, final double s2) {
        return (s1 > 0 && s2 > 0) || (s1 < 0 && s2 < 0);
    }

    /**
     * Returns whether one value is strictly positive and the other one strictly negative.
     */
    private static boolean opposite(final double s1, final double s2) {
        return (s1 > 0 && s2 < 0) || (s1 < 0 && s2 > 0);
    }

    /**
     * Returns the robust side of point r relative to the line from p to q, in the plane of the axes u and v.
     * Positive on the left, negative on the right, zero on the line.
     */
    private static double side(final double[] p, final double[] q, final double[] r, final int u, final int v) {
        return Maths.lineSide(p[u], p[v], q[u], q[v], r[u], r[v]);
    }

    /**
     * Returns the normal (b−a)×(c−a) of a triangle, whose length is twice the area of the triangle.
     */
    private static double[] normal(final double[] a, final double[] b, final double[] c) {
        final double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        final double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        return new double[] {uy*vz - uz*vy, uz*vx - ux*vz, ux*vy - uy*vx};
    }

    /**
     * Computes where a segment touches a triangle, in three dimensions. An end of the segment within the tolerance
     * of the plane of the triangle is on that plane, as decided by {@link Maths#planeSide Maths.planeSide(…)}.
     * If both ends are on the plane, the segment is {@linkplain SegmentTriangle.Type#COPLANAR coplanar} and its
     * intersection with the triangle is given by {@link #segmentInTriangle2D segmentInTriangle2D(…)}.
     *
     * <p>When the segment crosses the plane, where it crosses the triangle is decided by the robust sides of
     * its line relative to the three edges, so that a segment crossing an edge or a corner is found exactly on it.
     * Only the ratio of a point inside the segment is an approximation. When an end of the segment is on the plane,
     * that end is located in the triangle with robust side tests in the plane.</p>
     *
     * @param  s0  start of the segment.
     * @param  s1  end of the segment.
     * @param  a   first corner of the triangle.
     * @param  b   second corner of the triangle.
     * @param  c   third corner of the triangle.
     * @param  tolerance  largest distance to the plane of the triangle for considering an end of the segment on it.
     * @return where the segment touches the triangle.
     */
    public static SegmentTriangle segmentTriangle(double[] s0, double[] s1, double[] a, double[] b, double[] c, double tolerance) {
        final int side0 = Maths.planeSide(a, b, c, s0, tolerance);
        final int side1 = Maths.planeSide(a, b, c, s1, tolerance);
        if (side0 == 0 && side1 == 0) {
            return SegmentTriangle.COPLANAR;
        }
        if (side0 == side1) {
            return SegmentTriangle.NONE;            // both ends strictly on the same side.
        }
        final double[][] corners = {a, b, c};
        final double[] normal = normal(a, b, c);
        if (side0 == 0 || side1 == 0) {
            final int[] axes = Maths.projectionAxes(normal);
            return (side0 == 0) ? locate2D(s0, corners, axes[0], axes[1], 0)
                                : locate2D(s1, corners, axes[0], axes[1], 1);
        }
        /*
         * The segment crosses the plane between its ends. The sign of the tetrahedron made by the segment and
         * an edge tells on which side of that edge the line of the segment passes. The line passes inside the
         * triangle if the three signs are the same, and on an edge or a corner if one or two of them are zero.
         */
        int positive = 0, negative = 0, zeros = 0, zeroEdge = -1, otherEdge = -1;
        for (int i = 0; i < 3; i++) {
            final double[] p = corners[i], q = corners[(i + 1) % 3];
            final double sign = ShewchukPredicates.orient3d(s0[0], s0[1], s0[2], s1[0], s1[1], s1[2],
                                                            p[0], p[1], p[2], q[0], q[1], q[2]);
            if      (sign > 0) {positive++; otherEdge = i;}
            else if (sign < 0) {negative++; otherEdge = i;}
            else               {zeros++;    zeroEdge  = i;}
        }
        if (positive != 0 && negative != 0) {
            return SegmentTriangle.NONE;
        }
        final double d0 = normal[0] * (s0[0] - a[0]) + normal[1] * (s0[1] - a[1]) + normal[2] * (s0[2] - a[2]);
        final double d1 = normal[0] * (s1[0] - a[0]) + normal[1] * (s1[1] - a[1]) + normal[2] * (s1[2] - a[2]);
        final SegmentTriangle point = SegmentTriangle.locate(zeros, zeroEdge, otherEdge, ratio(d0, d1));
        return (point != null) ? point : SegmentTriangle.COPLANAR;
    }

    /**
     * Locates a point of the plane of a triangle relative to that triangle, with robust side tests in the plane
     * of the two given axes.
     *
     * @param  p        the point to locate.
     * @param  corners  the three corners of the triangle.
     * @param  ratio    position of the point on the segment for which the point is located.
     * @return where the point is on the triangle, or {@link SegmentTriangle#NONE} if outside or if the triangle is degenerate.
     */
    private static SegmentTriangle locate2D(final double[] p, final double[][] corners, final int u, final int v, final double ratio) {
        final double orientation = Math.signum(side(corners[0], corners[1], corners[2], u, v));
        if (orientation == 0) {
            return SegmentTriangle.NONE;
        }
        int zeros = 0, zeroEdge = -1, otherEdge = -1;
        for (int i = 0; i < 3; i++) {
            final double s = orientation * side(corners[i], corners[(i + 1) % 3], p, u, v);
            if (s < 0) {
                return SegmentTriangle.NONE;
            }
            if (s == 0) {zeros++; zeroEdge = i;}
            else        {otherEdge = i;}
        }
        final SegmentTriangle point = SegmentTriangle.locate(zeros, zeroEdge, otherEdge, ratio);
        return (point != null) ? point : SegmentTriangle.NONE;
    }

    /**
     * Computes the part of a segment inside a triangle, both in the same plane, with robust side tests in the plane
     * of the two given axes. The result gives the two ends of that part, each with its position on the segment and
     * its location on the triangle: an end is an end of the segment inside the triangle, or a point where the
     * segment crosses an edge or passes through a corner. Whether a point is on an edge or a corner, or is an end
     * of the segment, is exact. Only the ratios of the points inside the segment are approximations.
     *
     * @param  s0    start of the segment.
     * @param  s1    end of the segment.
     * @param  a     first corner of the triangle.
     * @param  b     second corner of the triangle.
     * @param  c     third corner of the triangle.
     * @param  axes  indices of the two coordinates of the plane, for example from {@link Maths#projectionAxes(double[])}.
     * @return the ends of the part of the segment inside the triangle by increasing ratio: none if the segment
     *         does not touch the triangle, one if it touches it at a single point, two otherwise.
     *         Their types are {@code INTERIOR}, {@code EDGE} or {@code VERTEX}.
     */
    public static List<SegmentTriangle> segmentInTriangle2D(double[] s0, double[] s1, double[] a, double[] b, double[] c, int[] axes) {
        final int u = axes[0], v = axes[1];
        final double[][] corners = {a, b, c};
        if (side(a, b, c, u, v) == 0) {
            return List.of();                       // a degenerate triangle has no interior.
        }
        final List<SegmentTriangle> points = new ArrayList<>(4);
        final SegmentTriangle start = locate2D(s0, corners, u, v, 0);
        if (start != SegmentTriangle.NONE) {
            points.add(start);
        }
        final boolean isPoint = (s0[u] == s1[u] && s0[v] == s1[v]);
        if (!isPoint) {
            final SegmentTriangle end = locate2D(s1, corners, u, v, 1);
            if (end != SegmentTriangle.NONE) {
                points.add(end);
            }
            /*
             * The corners on the line of the segment, between its ends.
             * The ends of the segment have been located above.
             */
            final double dx = s1[u] - s0[u], dy = s1[v] - s0[v];
            final double[] sides = new double[3];
            for (int j = 0; j < 3; j++) {
                final double[] p = corners[j];
                sides[j] = side(s0, s1, p, u, v);
                if (sides[j] == 0 && !(p[u] == s0[u] && p[v] == s0[v]) && !(p[u] == s1[u] && p[v] == s1[v])) {
                    final double t = ((p[u] - s0[u]) * dx + (p[v] - s0[v]) * dy) / (dx*dx + dy*dy);
                    if (t > 0 && t < 1) {
                        points.add(new SegmentTriangle(SegmentTriangle.Type.VERTEX, j, t));
                    }
                }
            }
            /*
             * The edges crossed between their corners, by the segment between its ends.
             */
            for (int i = 0; i < 3; i++) {
                final int j = (i + 1) % 3;
                if (opposite(sides[i], sides[j])) {
                    final double d0 = side(corners[i], corners[j], s0, u, v);
                    final double d1 = side(corners[i], corners[j], s1, u, v);
                    if (opposite(d0, d1)) {
                        points.add(new SegmentTriangle(SegmentTriangle.Type.EDGE, i, ratio(d0, d1)));
                    }
                }
            }
        }
        switch (points.size()) {
            case 0:  return List.of();
            case 1:  return List.of(points.get(0));
            default: {
                //the triangle is convex, so the part inside it goes from the first point to the last one.
                points.sort(Comparator.comparingDouble(SegmentTriangle::ratio));
                return List.of(points.get(0), points.get(points.size() - 1));
            }
        }
    }

    /**
     * Computes the intersection of two triangles in three dimensions. A corner of a triangle within the tolerance
     * of the plane of the other triangle is on that plane, as decided by {@link Maths#planeSide Maths.planeSide(…)}.
     * If the three corners of a triangle are on the plane of the other one, the triangles are
     * {@linkplain TriangleTriangle.Type#COPLANAR coplanar} and shall be intersected in that plane,
     * for example with {@link #segmentInTriangle2D segmentInTriangle2D(…)}.
     *
     * <p>Otherwise the intersection is on the line where the two planes meet, and each of its ends is a point
     * where an edge of a triangle touches the other triangle, as given by {@link #segmentTriangle segmentTriangle(…)}.
     * An edge in the plane of the other triangle is intersected with it in that plane.</p>
     *
     * @param  a0  first corner of the first triangle.
     * @param  a1  second corner of the first triangle.
     * @param  a2  third corner of the first triangle.
     * @param  b0  first corner of the second triangle.
     * @param  b1  second corner of the second triangle.
     * @param  b2  third corner of the second triangle.
     * @param  tolerance  largest distance to the plane of a triangle for considering a point on it.
     * @return the intersection of the two triangles.
     */
    public static TriangleTriangle triangleTriangle(double[] a0, double[] a1, double[] a2,
                                                    double[] b0, double[] b1, double[] b2, double tolerance)
    {
        final double[][] first = {a0, a1, a2}, second = {b0, b1, b2};
        final int[] sidesOfSecond = sides(first, second, tolerance);
        final int[] sidesOfFirst  = sides(second, first, tolerance);
        if (allOnSameSide(sidesOfSecond) || allOnSameSide(sidesOfFirst)) {
            return TriangleTriangle.DISJOINT;
        }
        if (Arrays.equals(sidesOfSecond, new int[3]) || Arrays.equals(sidesOfFirst, new int[3])) {
            return TriangleTriangle.COPLANAR;
        }
        final List<TriangleEdgeContact> contacts = new ArrayList<>();
        addEdgeContacts(first, second, true, tolerance, contacts);
        addEdgeContacts(second, first, false, tolerance, contacts);
        if (contacts.isEmpty()) {
            return TriangleTriangle.DISJOINT;
        }
        /*
         * All points are on the line where the two planes meet.
         * The ends of the intersection are the two extreme points along that line.
         */
        final double[] direction = Vectors.cross(normal(a0, a1, a2), normal(b0, b1, b2));
        TriangleEdgeContact start = null, end = null;
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (final TriangleEdgeContact contact : contacts) {
            final double t = Vectors.dot(contact.position(), direction);
            if (t < min) {min = t; start = contact;}
            if (t > max) {max = t; end   = contact;}
        }
        if (start == end || locations(start).equals(locations(end)) || Arrays.equals(start.position(), end.position())) {
            return new TriangleTriangle(TriangleTriangle.Type.POINT, start, start);
        }
        return new TriangleTriangle(TriangleTriangle.Type.SEGMENT, start, end);
    }

    /**
     * Returns the sides of the corners of a triangle relative to the plane of another triangle.
     */
    private static int[] sides(final double[][] plane, final double[][] triangle, final double tolerance) {
        final int[] sides = new int[3];
        for (int i = 0; i < 3; i++) {
            sides[i] = Maths.planeSide(plane[0], plane[1], plane[2], triangle[i], tolerance);
        }
        return sides;
    }

    /**
     * Returns whether the three sides are all strictly positive or all strictly negative.
     */
    private static boolean allOnSameSide(final int[] sides) {
        return sameSide(sides[0], sides[1]) && sameSide(sides[1], sides[2]);
    }

    /**
     * Adds the points where the edges of a triangle touch another triangle.
     *
     * @param edges        the triangle whose edges are tested.
     * @param other        the triangle touched by the edges.
     * @param edgeOfFirst  whether the triangle of the edges is the first one given to {@code triangleTriangle(…)}.
     * @param tolerance    largest distance to the plane of a triangle for considering a point on it.
     * @param contacts     where to add the points.
     */
    private static void addEdgeContacts(final double[][] edges, final double[][] other, final boolean edgeOfFirst,
                                        final double tolerance, final List<TriangleEdgeContact> contacts)
    {
        for (int i = 0; i < 3; i++) {
            final double[] p = edges[i], q = edges[(i + 1) % 3];
            final SegmentTriangle contact = segmentTriangle(p, q, other[0], other[1], other[2], tolerance);
            switch (contact.type()) {
                case NONE: break;
                case COPLANAR: {
                    final int[] axes = Maths.projectionAxes(normal(other[0], other[1], other[2]));
                    for (final SegmentTriangle point : segmentInTriangle2D(p, q, other[0], other[1], other[2], axes)) {
                        contacts.add(new TriangleEdgeContact(edgeOfFirst, i, point, interpolate(p, q, point.ratio())));
                    }
                    break;
                }
                default: {
                    contacts.add(new TriangleEdgeContact(edgeOfFirst, i, contact, interpolate(p, q, contact.ratio())));
                    break;
                }
            }
        }
    }

    /**
     * Returns the three first coordinates of the point at the given ratio between two points,
     * exactly one of them at ratio 0 or 1.
     */
    private static double[] interpolate(final double[] p, final double[] q, final double ratio) {
        if (ratio == 0) return Arrays.copyOf(p, 3);
        if (ratio == 1) return Arrays.copyOf(q, 3);
        final double[] point = new double[3];
        for (int k = 0; k < 3; k++) {
            point[k] = p[k] + ratio * (q[k] - p[k]);
        }
        return point;
    }

    /**
     * Where a point is on a triangle: inside, or on the edge or the corner of the given index.
     */
    private record Location(SegmentTriangle.Type type, int index) {
    }

    /**
     * Returns where the point of the given contact is on the first triangle and on the second triangle.
     * Two contacts found from different edges are the same point if they are at the same locations.
     */
    private static List<Location> locations(final TriangleEdgeContact contact) {
        final double ratio = contact.contact().ratio();
        final int edge = contact.edge();
        final Location onEdge;
        if      (ratio == 0) onEdge = new Location(SegmentTriangle.Type.VERTEX, edge);
        else if (ratio == 1) onEdge = new Location(SegmentTriangle.Type.VERTEX, (edge + 1) % 3);
        else                 onEdge = new Location(SegmentTriangle.Type.EDGE, edge);
        final Location onOther = new Location(contact.contact().type(), contact.contact().feature());
        return contact.edgeOfFirst() ? List.of(onEdge, onOther) : List.of(onOther, onEdge);
    }

    /**
     * Intersection operations on BSpline curves and surfaces.
     */
    public static interface BSpline {
        /**
         * Computes the intersection points between this curve and the given one (both must have the same dimension).
         * Exploits the convex hull property (approximated here by a bounding box, which is simpler and good enough for
         * pruning) in order to subdivide recursively down to nearly straight segments, then refines each candidate with a
         * Gauss-Newton method (numerical derivatives).
         *
         * @param first the first curve
         * @param second the curve to intersect with
         * @param tol tolerance on both the size of the subdivision intervals and the final accepted distance between the
         * two curves
         * @return the intersection points, possibly empty
         */
        public static List<CurveIntersectionPoint> intersect(final DefaultNurbCurve first, final DefaultNurbCurve second, final double tol) {
            final List<CurveIntersectionPoint> results = new ArrayList<>();
            intersectRecursive(first, second, tol, results);
            return results;
        }

        private static void intersectRecursive(final DefaultNurbCurve c1, final DefaultNurbCurve c2, final double tol, final List<CurveIntersectionPoint> results) {
            if (!c1.getEnvelope().intersects(c2.getEnvelope(), tol)) {
                return;
            }

            final double span1 = c1.domainEnd() - c1.domainStart();
            final double span2 = c2.domainEnd() - c2.domainStart();

            if (span1 <= tol && span2 <= tol) {
                refineAndCollect(c1, c2, tol, results);
                return;
            }

            if (span1 >= span2) {
                final double mid = (c1.domainStart() + c1.domainEnd()) / 2;
                final DefaultNurbCurve[] halves = c1.subdivide(mid);
                intersectRecursive(halves[0], c2, tol, results);
                intersectRecursive(halves[1], c2, tol, results);
            } else {
                final double mid = (c2.domainStart() + c2.domainEnd()) / 2;
                final DefaultNurbCurve[] halves = c2.subdivide(mid);
                intersectRecursive(c1, halves[0], tol, results);
                intersectRecursive(c1, halves[1], tol, results);
            }
        }

        /**
         * Initial guess (nearly linear segments) then Gauss-Newton refinement.
         */
        private static void refineAndCollect(final DefaultNurbCurve c1, final DefaultNurbCurve c2, final double tol, final List<CurveIntersectionPoint> results) {
            final double u1a = c1.domainStart(), u1b = c1.domainEnd();
            final double u2a = c2.domainStart(), u2b = c2.domainEnd();

            // Closest points between the two chords, as a starting guess.
            final double[] start1 = c1.evaluate(u1a).toArrayDouble();
            final double[] end1 = c1.evaluate(u1b).toArrayDouble();
            final double[] start2 = c2.evaluate(u2a).toArrayDouble();
            final double[] end2 = c2.evaluate(u2b).toArrayDouble();
            final double[] ratio = new double[2];
            Distance.distanceSquare(start1, end1, new double[start1.length],
                                    start2, end2, new double[start2.length], ratio, 1e-14);
            double u1 = u1a + ratio[0] * (u1b - u1a);
            double u2 = u2a + ratio[1] * (u2b - u2a);

            for (int iter = 0; iter < 10; iter++) {
                final Vector<?> c1u = c1.evaluate(u1), c2u = c2.evaluate(u2);
                final Vector<?> f = subtract(c1u, c2u);

                final Vector<?> d1 = numericDerivative(c1, u1);
                final Vector<?> d2 = scale(numericDerivative(c2, u2), -1);

                final double a = d1.dot(d1), b = d1.dot(d2), cc = d2.dot(d2);
                final double rhs0 = -d1.dot(f), rhs1 = -d2.dot(f);
                final double denom = a * cc - b * b;
                if (Math.abs(denom) < 1e-14) {
                    break;
                }

                final double du1 = (rhs0 * cc - rhs1 * b) / denom;
                final double du2 = (a * rhs1 - b * rhs0) / denom;

                u1 = clamp(u1 + du1, u1a, u1b);
                u2 = clamp(u2 + du2, u2a, u2b);
            }

            final Vector<?> p1 = c1.evaluate(u1), p2 = c2.evaluate(u2);
            final double dist = subtract(p1, p2).length();
            if (dist > Math.max(tol, 1e-6)) {
                return;
            }

            final double u1f = u1, u2f = u2;
            final boolean duplicate = results.stream().anyMatch(r
                    -> Math.abs(r.u1 - u1f) < 1e-4 && Math.abs(r.u2 - u2f) < 1e-4);
            if (!duplicate) {
                results.add(new CurveIntersectionPoint(u1, u2, p1));
            }
        }

        /**
         * Derivative estimated by centered finite difference, staying inside the curve domain.
         */
        private static Vector<?> numericDerivative(final DefaultNurbCurve curve, final double u) {
            final double lo = curve.domainStart(), hi = curve.domainEnd();
            final double h = Math.max((hi - lo) * 1e-4, 1e-8);
            final double uPlus = Math.min(u + h, hi), uMinus = Math.max(u - h, lo);
            final double denom = uPlus - uMinus;
            if (denom == 0) {
                return scale(curve.evaluate(u), 0);
            }
            return scale(subtract(curve.evaluate(uPlus), curve.evaluate(uMinus)), 1.0 / denom);
        }


        // ------------------------------------------------------------------
        // Intersection (bounding box + recursive quadtree subdivision
        // + regularized Gauss-Newton)
        // ------------------------------------------------------------------
        /**
         * Computes a sampling of the intersection curve between this surface and the given one. Same principle as for
         * curves (bounding box + recursive subdivision + Gauss-Newton), with two notable differences:
         *
         * 1) The parameter space is now 4 dimensional (u1,v1,u2,v2), so the subdivision is a quadtree: at each step, the
         * direction (u or v) of the surface having the largest interval is split, among the 4 candidates. 2) The system to
         * solve is under-determined (3 position equations for 4 unknowns), so unlike the curve-curve case there is no
         * isolated solution but a whole curve of solutions. The Gauss-Newton normal equations are therefore regularized
         * (Tikhonov ridge) so as to still get a point that "falls back" on that curve near the starting point — the result
         * is thus a set of points sampling the intersection curve, not isolated points as for two curves.
         *
         * @param first the surface to intersect with
         * @param second the surface to intersect with this one
         * @param tol tolerance on the size of the subdivision patches and on the final accepted distance between the two
         * surfaces
         * @return points sampling the intersection curve, possibly empty
         */
        public static List<SurfaceIntersectionPoint> intersect(final DefaultNurbSurface first, final DefaultNurbSurface second, final double tol) {
            final List<SurfaceIntersectionPoint> results = new ArrayList<>();
            intersectRecursive(first, second, tol, results, 0);
            return results;
        }

        private static void intersectRecursive(final DefaultNurbSurface s1, final DefaultNurbSurface s2, final double tol,
                final List<SurfaceIntersectionPoint> results, final int depth) {
            if (!s1.getEnvelope().intersects(s2.getEnvelope(), tol)) {
                return;
            }

            final double spanU1 = s1.domainEndU() - s1.domainStartU();
            final double spanV1 = s1.domainEndV() - s1.domainStartV();
            final double spanU2 = s2.domainEndU() - s2.domainStartU();
            final double spanV2 = s2.domainEndV() - s2.domainStartV();
            final double maxSpan = Math.max(Math.max(spanU1, spanV1), Math.max(spanU2, spanV2));

            if (maxSpan <= tol || depth > 40) {
                refineAndCollect(s1, s2, tol, results);
                return;
            }

            if (maxSpan == spanU1) {
                final double mid = (s1.domainStartU() + s1.domainEndU()) / 2;
                final DefaultNurbSurface[] halves = s1.subdivideU(mid);
                intersectRecursive(halves[0], s2, tol, results, depth + 1);
                intersectRecursive(halves[1], s2, tol, results, depth + 1);
            } else if (maxSpan == spanV1) {
                final double mid = (s1.domainStartV() + s1.domainEndV()) / 2;
                final DefaultNurbSurface[] halves = s1.subdivideV(mid);
                intersectRecursive(halves[0], s2, tol, results, depth + 1);
                intersectRecursive(halves[1], s2, tol, results, depth + 1);
            } else if (maxSpan == spanU2) {
                final double mid = (s2.domainStartU() + s2.domainEndU()) / 2;
                final DefaultNurbSurface[] halves = s2.subdivideU(mid);
                intersectRecursive(s1, halves[0], tol, results, depth + 1);
                intersectRecursive(s1, halves[1], tol, results, depth + 1);
            } else {
                final double mid = (s2.domainStartV() + s2.domainEndV()) / 2;
                final DefaultNurbSurface[] halves = s2.subdivideV(mid);
                intersectRecursive(s1, halves[0], tol, results, depth + 1);
                intersectRecursive(s1, halves[1], tol, results, depth + 1);
            }
        }

        /**
         * Initial guess (center of the patches) then regularized Gauss-Newton refinement (4 unknowns, 3 equations).
         */
        private static void refineAndCollect(final DefaultNurbSurface s1, final DefaultNurbSurface s2, final double tol,
                final List<SurfaceIntersectionPoint> results) {
            final double u1a = s1.domainStartU(), u1b = s1.domainEndU();
            final double v1a = s1.domainStartV(), v1b = s1.domainEndV();
            final double u2a = s2.domainStartU(), u2b = s2.domainEndU();
            final double v2a = s2.domainStartV(), v2b = s2.domainEndV();

            double u1 = (u1a + u1b) / 2, v1 = (v1a + v1b) / 2;
            double u2 = (u2a + u2b) / 2, v2 = (v2a + v2b) / 2;

            final double lambda = 1e-8; // Tikhonov regularization (under-determined system)
            for (int iter = 0; iter < 15; iter++) {
                final Vector<?> f = subtract(s1.evaluate(u1, v1), s2.evaluate(u2, v2));

                final Vector<?> dU1 = numericDerivativeU(s1, u1, v1);
                final Vector<?> dV1 = numericDerivativeV(s1, u1, v1);
                final Vector<?> dU2 = scale(numericDerivativeU(s2, u2, v2), -1);
                final Vector<?> dV2 = scale(numericDerivativeV(s2, u2, v2), -1);
                final Vector<?>[] cols = {dU1, dV1, dU2, dV2};

                final double[][] JtJ = new double[4][4];
                final double[] Jtf = new double[4];
                for (int a = 0; a < 4; a++) {
                    Jtf[a] = -cols[a].dot(f);
                    for (int b = 0; b < 4; b++) {
                        JtJ[a][b] = cols[a].dot(cols[b]) + (a == b ? lambda : 0);
                    }
                }

                final double[] delta = Matrices.solve(JtJ, Jtf);
                if (delta == null) {
                    break;
                }

                u1 = clamp(u1 + delta[0], u1a, u1b);
                v1 = clamp(v1 + delta[1], v1a, v1b);
                u2 = clamp(u2 + delta[2], u2a, u2b);
                v2 = clamp(v2 + delta[3], v2a, v2b);
            }

            final Vector<?> p1 = s1.evaluate(u1, v1), p2 = s2.evaluate(u2, v2);
            final double dist = subtract(p1, p2).length();
            if (dist > Math.max(tol, 1e-6)) {
                return;
            }

            final double mergeTol = Math.max(tol * 5, 1e-4);
            final boolean duplicate = results.stream().anyMatch(r
                    -> subtract(r.point, p1).length() < mergeTol);
            if (!duplicate) {
                results.add(new SurfaceIntersectionPoint(u1, v1, u2, v2, p1));
            }
        }

        /**
         * Partial derivative along u, estimated by centered finite difference inside the surface domain.
         */
        private static Vector<?> numericDerivativeU(final DefaultNurbSurface s, final double u, final double v) {
            final double lo = s.domainStartU(), hi = s.domainEndU();
            final double h = Math.max((hi - lo) * 1e-4, 1e-8);
            final double uPlus = Math.min(u + h, hi), uMinus = Math.max(u - h, lo);
            final double denom = uPlus - uMinus;
            if (denom == 0) {
                return scale(s.evaluate(u, v), 0);
            }
            return scale(subtract(s.evaluate(uPlus, v), s.evaluate(uMinus, v)), 1.0 / denom);
        }

        /**
         * Partial derivative along v, estimated by centered finite difference inside the surface domain.
         */
        private static Vector<?> numericDerivativeV(final DefaultNurbSurface s, final double u, final double v) {
            final double lo = s.domainStartV(), hi = s.domainEndV();
            final double h = Math.max((hi - lo) * 1e-4, 1e-8);
            final double vPlus = Math.min(v + h, hi), vMinus = Math.max(v - h, lo);
            final double denom = vPlus - vMinus;
            if (denom == 0) {
                return scale(s.evaluate(u, v), 0);
            }
            return scale(subtract(s.evaluate(u, vPlus), s.evaluate(u, vMinus)), 1.0 / denom);
        }

        /**
         * An intersection point which has been found: its two parameters and the point in space.
         */
        public static final class CurveIntersectionPoint {

            public final double u1, u2;
            public final Vector<?> point;

            public CurveIntersectionPoint(final double u1, final double u2, final Vector<?> point) {
                this.u1 = u1;
                this.u2 = u2;
                this.point = point;
            }

            @Override
            public String toString() {
                return String.format("u1=%.6f, u2=%.6f -> %s", u1, u2, point);
            }
        }

        /**
         * An intersection point between two surfaces: its four parameters and the point in space. Unlike the curve-curve
         * intersection (which yields isolated points), the intersection of two surfaces is generally a 3D CURVE (4
         * unknowns, 3 equations => 1 remaining degree of freedom). This class therefore represents a single sample of that
         * curve, not an isolated point in the strict sense.
         */
        public static final class SurfaceIntersectionPoint {

            public final double u1, v1, u2, v2;
            public final Vector<?> point;

            public SurfaceIntersectionPoint(final double u1, final double v1, final double u2, final double v2, final Vector<?> point) {
                this.u1 = u1;
                this.v1 = v1;
                this.u2 = u2;
                this.v2 = v2;
                this.point = point;
            }

            @Override
            public String toString() {
                return String.format("u1=%.4f, v1=%.4f, u2=%.4f, v2=%.4f -> %s", u1, v1, u2, v2, point);
            }
        }
    }

}
