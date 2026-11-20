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
import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.OBBox;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.surface.Capsule;
import org.apache.sis.geometries.surface.Cylinder;
import org.apache.sis.geometries.surface.Ellipsoid;
import org.apache.sis.geometries.surface.NurbSurface;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.Sphere;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;


/**
 * Exact parametric forms of analytic geometries, used by {@link GeometryProcessor#parametrize(Geometry)}.
 *
 * <p>Round shapes become rational NURBS surfaces of degree 2, made by revolving a profile around an axis:
 * rational quadratic arcs represent circles and ellipses exactly. Boxes become polyhedra.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The surfaces are parameterized with <var>u</var> turning counterclockwise around the axis and
 *       <var>v</var> following the profile, so that their up-normal (∂S/∂u × ∂S/∂v) points outward.</li>
 *   <li>Round shapes are parametrized in 3 dimensions only.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
final class Parametrize {

    /**
     * Degree of the NURBS surfaces in both directions.
     */
    private static final int DEGREE = 2;

    /**
     * Weight of the control points at the corners of the quarter arcs: cos(45°).
     */
    private static final double CORNER = Math.sqrt(0.5);

    /**
     * Unit circle made of four rational quadratic quarter arcs, as (x, y, weight) for each control point.
     */
    private static final double[][] UNIT_CIRCLE = {
        { 1,  0, 1}, { 1,  1, CORNER}, { 0,  1, 1}, {-1,  1, CORNER}, {-1,  0, 1},
        {-1, -1, CORNER}, { 0, -1, 1}, { 1, -1, CORNER}, { 1,  0, 1}
    };

    /**
     * Knots of {@link #UNIT_CIRCLE}, one quarter arc per knot interval.
     */
    private static final double[] CIRCLE_KNOTS = {0, 0, 0, 0.25, 0.25, 0.5, 0.5, 0.75, 0.75, 1, 1, 1};

    /**
     * Half meridian from the south pole to the north pole, as (radius, height, weight) for each control point.
     */
    private static final double[][] MERIDIAN = {
        {0, -1, 1}, {1, -1, CORNER}, {1, 0, 1}, {1, 1, CORNER}, {0, 1, 1}
    };

    /**
     * Knots of {@link #MERIDIAN}, one quarter arc per knot interval.
     */
    private static final double[] MERIDIAN_KNOTS = {0, 0, 0, 0.5, 0.5, 1, 1, 1};

    /**
     * Knots of a straight segment given as a quadratic arc of three aligned control points.
     */
    private static final double[] SEGMENT_KNOTS = {0, 0, 0, 1, 1, 1};

    private Parametrize() {
    }

    /**
     * Returns the sphere as a NURBS surface of 9 × 5 control points.
     *
     * @throws UnsupportedOperationException if the sphere is not 3-dimensional.
     */
    static NurbSurface sphere(final Sphere sphere) {
        final double r = sphere.getRadius();
        return ellipsoidal(sphere.getCoordinateReferenceSystem(), sphere.getCenter(), new double[] {r, r, r});
    }

    /**
     * Returns the ellipsoid as a NURBS surface of 9 × 5 control points:
     * the NURBS sphere scaled by the semi-axes, NURBS being invariant under affine transforms.
     *
     * @throws UnsupportedOperationException if the ellipsoid is not 3-dimensional.
     */
    static NurbSurface ellipsoid(final Ellipsoid ellipsoid) {
        return ellipsoidal(ellipsoid.getCoordinateReferenceSystem(), ellipsoid.getCenter(), ellipsoid.getSemiAxes());
    }

    /**
     * Returns the ellipsoid of the given center and semi-axes, its axis of revolution along the third axis.
     */
    private static NurbSurface ellipsoidal(final CoordinateReferenceSystem crs, final Vector<?> center, final double[] semiAxes) {
        ensure3D(center, "An ellipsoid");
        final Frame frame = new Frame(center.copy(DataType.DOUBLE),
                Vectors.create(new double[] {semiAxes[0], 0, 0}, DataType.DOUBLE),
                Vectors.create(new double[] {0, semiAxes[1], 0}, DataType.DOUBLE),
                Vectors.create(new double[] {0, 0, semiAxes[2]}, DataType.DOUBLE));
        return frame.revolve(crs, MERIDIAN, MERIDIAN_KNOTS);
    }

    /**
     * Returns the cylinder as its lateral NURBS surface, followed by the disks closing its ends if it is closed.
     * An end whose radius is zero, the apex of a cone, has no disk.
     *
     * @return the lateral surface if the cylinder is open, or a collection of surfaces otherwise.
     * @throws UnsupportedOperationException if the cylinder is not 3-dimensional.
     * @throws OperationException if the two positions of the cylinder are the same.
     */
    static Geometry cylinder(final Cylinder cylinder) {
        final Vector<?> bottom = cylinder.getDataPoints().getPosition(0).copy(DataType.DOUBLE);
        final Vector<?> top    = cylinder.getDataPoints().getPosition(1).copy(DataType.DOUBLE);
        ensure3D(bottom, "A cylinder");
        final Vector<?> axisVector = top.subtract(bottom);
        final double height = axisVector.length();
        if (!(height > 0)) {
            throw new OperationException("The cylinder has no axis, its two positions are the same.");
        }
        final Frame frame = Frame.alongAxis(bottom, axisVector.scale(1 / height));
        final CoordinateReferenceSystem crs = cylinder.getCoordinateReferenceSystem();
        final double rb = cylinder.getRadiusBottom();
        final double rt = cylinder.getRadiusTop();
        final NurbSurface lateral = frame.revolve(crs,
                new double[][] {{rb, 0, 1}, {(rb + rt) / 2, height / 2, 1}, {rt, height, 1}}, SEGMENT_KNOTS);
        if (!cylinder.isClosed()) {
            return lateral;
        }
        /*
         * The bottom disk goes from the center to the rim and the top disk from the rim to the center,
         * so that both of them face away from the cylinder.
         */
        final List<NurbSurface> patches = new ArrayList<>(3);
        if (rb > 0) {
            patches.add(frame.revolve(crs, new double[][] {{0, 0, 1}, {rb / 2, 0, 1}, {rb, 0, 1}}, SEGMENT_KNOTS));
        }
        patches.add(lateral);
        if (rt > 0) {
            patches.add(frame.revolve(crs, new double[][] {{rt, height, 1}, {rt / 2, height, 1}, {0, height, 1}}, SEGMENT_KNOTS));
        }
        return GeometryFactory.DEFAULT.createGeometryCollection(GeometryType.SURFACE, crs, patches.toArray(NurbSurface[]::new));
    }

    /**
     * Returns the capsule, the convex hull of its two capping spheres, as a NURBS surface.
     * The profile revolved around the axis is an arc of the bottom sphere from the south pole,
     * the segment tangent to both spheres, then an arc of the top sphere up to the north pole.
     * If one sphere contains the other one, the capsule is the largest sphere.
     *
     * @throws UnsupportedOperationException if the capsule is not 3-dimensional.
     */
    static NurbSurface capsule(final Capsule capsule) {
        final Vector<?> bottom = capsule.getDataPoints().getPosition(0).copy(DataType.DOUBLE);
        final Vector<?> top    = capsule.getDataPoints().getPosition(1).copy(DataType.DOUBLE);
        ensure3D(bottom, "A capsule");
        final CoordinateReferenceSystem crs = capsule.getCoordinateReferenceSystem();
        final double rb = capsule.getRadiusBottom();
        final double rt = capsule.getRadiusTop();
        final Vector<?> axisVector = top.subtract(bottom);
        final double height = axisVector.length();
        if (!(height > Math.abs(rb - rt))) {
            return (rb >= rt) ? ellipsoidal(crs, bottom, new double[] {rb, rb, rb})
                              : ellipsoidal(crs, top,    new double[] {rt, rt, rt});
        }
        /*
         * The segment tangent to both spheres touches them where their outward normal is
         * (cos α, sin α) in the (radius, height) plane, with sin α = (rb − rt) / height.
         */
        final double alpha = Math.asin((rb - rt) / height);
        final Profile profile = new Profile(0, -rb);
        profile.arc(0, rb, -Math.PI / 2, alpha);
        profile.line(rt * Math.cos(alpha), height + rt * Math.sin(alpha));
        profile.arc(height, rt, alpha, Math.PI / 2);
        return Frame.alongAxis(bottom, axisVector.scale(1 / height)).revolve(crs, profile.points(), profile.knots());
    }

    /**
     * Returns the box as a polygon in 2 dimensions, or as a polyhedron in 3 dimensions.
     *
     * @throws UnsupportedOperationException if the box has another number of dimensions.
     */
    @SuppressWarnings("unchecked")
    static Geometry box(final BBox box) {
        return switch (box.getDimension()) {
            case 2 -> GeometryFactory.DEFAULT.createPolygon((LinearRing) box.getBoundary(), List.of());
            case 3 -> GeometryFactory.DEFAULT.createPolyhedron((GeometryCollection<Polygon>) box.getBoundary(), List.of());
            default -> throw new UnsupportedOperationException(
                    "A box is parametrized in 2 or 3 dimensions only, not " + box.getDimension() + '.');
        };
    }

    /**
     * Returns the oriented box as a polyhedron.
     */
    @SuppressWarnings("unchecked")
    static Geometry box(final OBBox box) {
        return GeometryFactory.DEFAULT.createPolyhedron((GeometryCollection<Polygon>) box.getBoundary(), List.of());
    }

    /**
     * Ensures that the given position is 3-dimensional.
     */
    private static void ensure3D(final Vector<?> position, final String shape) {
        if (position.getDimension() != 3) {
            throw new UnsupportedOperationException(shape + " is parametrized in 3 dimensions only.");
        }
    }

    /**
     * Local frame in which a profile is revolved: an origin and three axes, which are scaled as needed.
     *
     * @param origin  position of the frame origin.
     * @param e1      first axis of the plane of the circles.
     * @param e2      second axis of the plane of the circles.
     * @param e3      axis of revolution.
     */
    private record Frame(Vector<?> origin, Vector<?> e1, Vector<?> e2, Vector<?> e3) {

        /**
         * Returns the right-handed orthonormal frame whose third axis is the given unit vector.
         * The first axis is derived from the coordinate axis which is the most perpendicular
         * to the given one, for numerical stability.
         *
         * @param  origin  position of the frame origin.
         * @param  axis    unit vector of the axis of revolution.
         */
        static Frame alongAxis(final Vector<?> origin, final Vector<?> axis) {
            int k = 0;
            for (int i = 1; i < 3; i++) {
                if (Math.abs(axis.get(i)) < Math.abs(axis.get(k))) {
                    k = i;
                }
            }
            final double[] unit = new double[3];
            unit[k] = 1;
            final Vector<?> u = Vectors.create(unit, DataType.DOUBLE).subtract(axis.scale(axis.get(k))).normalize();
            return new Frame(origin, u, axis.cross(u), axis);
        }

        /**
         * Returns the position at the given coordinates in this frame.
         */
        Vector<?> at(final double a, final double b, final double c) {
            return origin.add(e1.scale(a)).add(e2.scale(b)).add(e3.scale(c));
        }

        /**
         * Returns the surface swept by the given profile turning once around the third axis.
         * The control points are the tensor product of the unit circle and of the profile,
         * with the products of their weights.
         *
         * @param  crs           reference system of the surface.
         * @param  profile       control points of the profile, as (radius, height, weight).
         * @param  profileKnots  knots of the profile.
         * @return the surface of revolution.
         */
        NurbSurface revolve(final CoordinateReferenceSystem crs, final double[][] profile, final double[] profileKnots) {
            final Vector<?>[][] points = new Vector<?>[UNIT_CIRCLE.length][profile.length];
            final double[][] weights = new double[UNIT_CIRCLE.length][profile.length];
            for (int i = 0; i < UNIT_CIRCLE.length; i++) {
                final double[] c = UNIT_CIRCLE[i];
                for (int j = 0; j < profile.length; j++) {
                    final double[] p = profile[j];
                    points[i][j]  = at(c[0] * p[0], c[1] * p[0], p[1]);
                    weights[i][j] = c[2] * p[2];
                }
            }
            return GeometryFactory.DEFAULT.createNurbSurface(crs, points, weights,
                    CIRCLE_KNOTS.clone(), profileKnots.clone(), DEGREE);
        }
    }

    /**
     * Profile of a surface of revolution in the (radius, height) plane, made of rational quadratic pieces
     * joined end to end. Each piece is an arc of at most 90° or a straight segment, and adds two control
     * points: its middle control point and its end.
     */
    private static final class Profile {
        /**
         * Control points as (radius, height, weight).
         */
        private final List<double[]> points = new ArrayList<>();

        /**
         * Starts a profile at the given position.
         */
        Profile(final double radius, final double height) {
            points.add(new double[] {radius, height, 1});
        }

        /**
         * Appends the arc of the circle centered on the axis at the given height, from angle {@code a0}
         * to angle {@code a1} in radians, measured from the radial direction toward the axis direction.
         * The arc is expected to start at the end of the profile.
         */
        void arc(final double height, final double radius, final double a0, final double a1) {
            final int count = (int) Math.ceil(Math.abs(a1 - a0) / (Math.PI / 2) - 1E-12);
            final double step = (a1 - a0) / Math.max(count, 1);
            for (int i = 0; i < count; i++) {
                final double start = a0 + step * i;
                final double half  = step / 2;
                final double reach = radius / Math.cos(half);       // Distance to the intersection of the end tangents.
                final double end   = start + step;
                points.add(new double[] {reach * Math.cos(start + half), height + reach * Math.sin(start + half), Math.cos(half)});
                points.add(new double[] {radius * Math.cos(end), height + radius * Math.sin(end), 1});
            }
        }

        /**
         * Appends the straight segment from the end of the profile to the given position.
         */
        void line(final double radius, final double height) {
            final double[] start = points.get(points.size() - 1);
            points.add(new double[] {(start[0] + radius) / 2, (start[1] + height) / 2, 1});
            points.add(new double[] {radius, height, 1});
        }

        /**
         * Returns the control points as (radius, height, weight).
         */
        double[][] points() {
            return points.toArray(double[][]::new);
        }

        /**
         * Returns the knots, with one interval per piece and a double knot at each junction.
         */
        double[] knots() {
            final int pieces = (points.size() - 1) / 2;
            final double[] knots = new double[2 * pieces + 4];
            for (int i = 0; i < knots.length; i++) {
                knots[i] = Math.clamp((double) ((i - 1) / 2) / pieces, 0, 1);
            }
            return knots;
        }
    }
}
