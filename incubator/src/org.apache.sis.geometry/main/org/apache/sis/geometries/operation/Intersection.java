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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.MultiLineString;
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
import static org.apache.sis.maths.Maths.clamp;
import org.apache.sis.maths.Matrices;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Tuple;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Intersection {

    private Intersection(){}


    static Vector<?> subtract(final ReadOnly.Vector<?> a, final ReadOnly.Tuple<?> b) {
        return a.copy().subtract(b);
    }

    static Vector<?> scale(final Tuple<?> a, final double s) {
        return Vectors.castOrWrap(a).copy().scale(s);
    }

    /**
     * Tests whether two bounding boxes overlap, within the given tolerance.
     * Boxes separated by less than the tolerance are still considered as overlapping,
     * which {@link Envelope#intersects Envelope.intersects(…)} cannot express.
     */
    private static boolean boxesOverlap(final Envelope box1, final Envelope box2, final double tol) {
        for (int i = 0, n = box1.getDimension(); i < n; i++) {
            if (box1.getMaximum(i) + tol < box2.getMinimum(i)
             || box2.getMaximum(i) + tol < box1.getMinimum(i)) {
                return false;
            }
        }
        return true;
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

        final AttributesType attributesType = tin.getAttributesType();
        final long nbVertex = primitive.getPositions().getLength();

        final List<String> attributeNames = new ArrayList<>(attributesType.getAttributeNames());
        if (attributeNames.size() == 1) {
            //no attributes to copy
            return;
        }
        attributeNames.remove(AttributesType.ATT_POSITION);
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
                final Optional<Point> opt = evaluator.evaluate(vertex.getPosition());
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
            final Tuple<?> position = cursor.samples();
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
            final Array segment = line.getDataPoints().getAttributeArray(AttributesType.ATT_POSITION);
            final Tuple<?> s1 = segment.get(0);
            final Tuple<?> s2 = segment.get(1);

            try (Stream<Triangle> stream = tin.getPatches(line.getEnvelope())) {
                final Iterator<Triangle> iterator = stream.iterator();

                while (iterator.hasNext()) {
                    final Triangle triangle = iterator.next();
                    final Array corners = triangle.getExteriorRing().getDataPoints().getAttributeArray(AttributesType.ATT_POSITION);
                    final Tuple<?> c0 = corners.get(0);
                    final Tuple<?> c1 = corners.get(1);
                    final Tuple<?> c2 = corners.get(2);
                    final List<Tuple> clip = SutherlandHodgman.clip(Arrays.asList(s1,s2,s1), Arrays.asList(c0,c1,c2,c0));
                    if (clip.size() >= 2) {
                        //inherit attributes
                        final double x1 = c0.get(0);
                        final double y1 = c0.get(1);
                        final double x2 = c1.get(0);
                        final double y2 = c1.get(1);
                        final double x3 = c2.get(0);
                        final double y3 = c2.get(1);
                        final Tuple<?> p1 = clip.get(0);
                        final Tuple<?> p2 = clip.get(1);
                        final double[] bary1 = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, p1.get(0), p1.get(1), 0.0, false);
                        final double[] bary2 = Triangle.getBarycentricValue2D(x1, y1, x2, y2, x3, y3, p2.get(0), p2.get(1), 0.0, false);
                        final Point point1 = triangle.interpolate(bary1);
                        final Point point2 = triangle.interpolate(bary2);
                        segments.add(GeometryFactory.createLineString(new DefaultDataPoints(point1, point2)));
                    }
                }
            } catch (TransformException ex) {
                throw new OperationException(ex.getMessage(), ex);
            }
        }

        final MultiLineString mline = GeometryFactory.createMultiLineString(segments.toArray(LineString[]::new));
        final MeshPrimitive intersection = (MeshPrimitive) new GeometryProcessor().toPrimitive(mline);
        return intersection;
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
            if (!boxesOverlap(c1.getEnvelope(), c2.getEnvelope(), tol)) {
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
            if (!boxesOverlap(s1.getEnvelope(), s2.getEnvelope(), tol)) {
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
