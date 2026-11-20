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
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.OBBox;
import org.apache.sis.geometries.Prism;
import org.apache.sis.geometries.Representation;
import org.apache.sis.geometries.curve.CircularString;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.Capsule;
import org.apache.sis.geometries.surface.Cylinder;
import org.apache.sis.geometries.surface.Ellipsoid;
import org.apache.sis.geometries.surface.NurbSurface;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.geometries.surface.Sphere;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link GeometryProcessor#parametrize(Geometry)}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ParametrizeTest {

    private static final CoordinateReferenceSystem CRS2D = CommonCRS.WGS84.normalizedGeographic();

    private static final CoordinateReferenceSystem CRS3D = Geometries.getUndefinedCRS(3);

    private static final GeometryFactory F = GeometryFactory.DEFAULT;

    /**
     * Tolerance on positions which are exact in theory.
     */
    private static final double TOLERANCE = 1E-12;

    private static final NumberRange<Double> RANGE = NumberRange.create(0., true, 3., true);

    /**
     * Geometries which are not analytic are returned unchanged.
     */
    @Test
    public void testNotAnalytic() {
        final CircularString arc = F.createCircularString(F.createDataPoints(CRS2D, 0,0, 5,5, 10,0));
        assertSame(arc, arc.parametrize());
        final Polygon square = F.createPolygon(F.createLinearRing(F.createDataPoints(CRS2D, 0,0, 2,0, 2,2, 0,2, 0,0)), null);
        assertSame(square, square.parametrize());
    }

    /**
     * A prism is replaced by its extrusion, which is exact.
     */
    @Test
    public void testPrism() {
        final LineString line = F.createLineString(F.createDataPoints(CRS2D, 0,0, 2,0, 2,1));
        final Geometry walls = F.createPrism(line, RANGE, null).parametrize();
        assertInstanceOf(PolyhedralSurface.class, walls);
        assertEquals(Representation.DISCRETE, walls.getRepresentation());

        final Polygon square = F.createPolygon(F.createLinearRing(F.createDataPoints(CRS2D, 0,0, 2,0, 2,2, 0,2, 0,0)), null);
        assertInstanceOf(Polyhedron.class, F.createPrism(square, RANGE, null).parametrize());
    }

    /**
     * The elements of a collection are converted one by one.
     */
    @Test
    public void testCollection() {
        final Polygon square = F.createPolygon(F.createLinearRing(F.createDataPoints(CRS2D, 0,0, 2,0, 2,2, 0,2, 0,0)), null);
        final Prism prism = F.createPrism(square, RANGE, null);
        final GeometryCollection<Geometry> mixed = F.createGeometryCollection(prism, square);
        assertEquals(Representation.ANALYTIC, mixed.getRepresentation());
        final GeometryCollection<?> result = assertInstanceOf(GeometryCollection.class, mixed.parametrize());
        assertInstanceOf(Polyhedron.class, result.getGeometryN(0));
        assertSame(square, result.getGeometryN(1));
    }

    /**
     * A sphere becomes a NURBS surface whose points are all at the radius distance from the center,
     * with an up-normal pointing outward.
     */
    @Test
    public void testSphere() {
        final Sphere sphere = F.createSphere(F.createDataPoints(CRS3D, 1, 2, 3), 2);
        assertEquals(Representation.ANALYTIC, sphere.getRepresentation());
        final NurbSurface nurbs = assertInstanceOf(NurbSurface.class, sphere.parametrize());
        assertEquals(Representation.PARAMETRIC, nurbs.getRepresentation());
        for (final double[] p : samples(nurbs)) {
            assertEquals(2, Math.sqrt(square(p[0] - 1) + square(p[1] - 2) + square(p[2] - 3)), TOLERANCE);
        }
        assertOutward(nurbs, 0.1, 0.3, new double[] {1, 2, 3});
    }

    /**
     * An ellipsoid becomes a NURBS surface whose points all satisfy the ellipsoid equation.
     */
    @Test
    public void testEllipsoid() {
        final Ellipsoid ellipsoid = F.createEllipsoid(F.createDataPoints(CRS3D, 8, 4, 2), 5, 1, 2);
        assertEquals(Representation.ANALYTIC, ellipsoid.getRepresentation());
        final NurbSurface nurbs = assertInstanceOf(NurbSurface.class, ellipsoid.parametrize());
        for (final double[] p : samples(nurbs)) {
            assertEquals(1, square((p[0] - 8) / 5) + square(p[1] - 4) + square((p[2] - 2) / 2), TOLERANCE);
        }
        assertOutward(nurbs, 0.6, 0.7, new double[] {8, 4, 2});
    }

    /**
     * An open cone becomes its lateral surface, whose radius decreases linearly from the bottom to the top.
     */
    @Test
    public void testOpenCone() {
        final Cylinder cone = F.createCylinder(F.createDataPoints(CRS3D, 0,0,0, 0,0,4));
        cone.setClosed(false);
        cone.setRadiusBottom(2);
        cone.setRadiusTop(1);
        final NurbSurface lateral = assertInstanceOf(NurbSurface.class, cone.parametrize());
        for (final double[] p : samples(lateral)) {
            assertEquals(2 - p[2] / 4, Math.hypot(p[0], p[1]), TOLERANCE);
        }
        assertOutward(lateral, 0.3, 0.5, new double[] {0, 0, 2});
    }

    /**
     * A closed cylinder becomes its lateral surface with a disk at each end, all oriented outward,
     * while a closed cone has no disk at its apex.
     */
    @Test
    public void testClosedCylinder() {
        final Cylinder cylinder = F.createCylinder(F.createDataPoints(CRS3D, 1,1,1, 1,1,4));
        final GeometryCollection<?> patches = assertInstanceOf(GeometryCollection.class, cylinder.parametrize());
        assertEquals(3, patches.getNumGeometries());
        final double[] inside = {1, 1, 2.5};
        for (final Geometry patch : patches) {
            assertOutward(assertInstanceOf(NurbSurface.class, patch), 0.3, 0.4, inside);
        }
        for (final double[] p : samples((NurbSurface) patches.getGeometryN(0))) {
            assertEquals(1, p[2], TOLERANCE);
            assertTrue(Math.hypot(p[0] - 1, p[1] - 1) <= 1 + TOLERANCE);
        }
        cylinder.setRadiusTop(0);
        assertEquals(2, ((GeometryCollection<?>) cylinder.parametrize()).getNumGeometries());
    }

    /**
     * A capsule of equal radii becomes a NURBS surface whose points are all at the radius distance
     * from the segment joining the two sphere centers.
     */
    @Test
    public void testCapsule() {
        final Capsule capsule = F.createCapsule(F.createDataPoints(CRS3D, 0,0,0, 0,0,3));
        final NurbSurface nurbs = assertInstanceOf(NurbSurface.class, capsule.parametrize());
        for (final double[] p : samples(nurbs)) {
            final double z = Math.clamp(p[2], 0, 3);
            assertEquals(1, Math.sqrt(square(p[0]) + square(p[1]) + square(p[2] - z)), TOLERANCE);
        }
        final double[] inside = {0, 0, 1.5};
        for (final double v : new double[] {0.2, 0.5, 0.8}) {
            assertOutward(nurbs, 0.3, v, inside);
        }
    }

    /**
     * A capsule of different radii becomes a NURBS surface whose points are on one of the capping spheres
     * or on the cone tangent to both, and never outside their convex hull.
     */
    @Test
    public void testRoundCone() {
        final double[] b = {0, 0, 0}, t = {2, 1, 3};
        final double rb = 1.5, rt = 0.5;
        final Capsule capsule = F.createCapsule(F.createDataPoints(CRS3D, b[0], b[1], b[2], t[0], t[1], t[2]));
        capsule.setRadiusBottom(rb);
        capsule.setRadiusTop(rt);
        final NurbSurface nurbs = assertInstanceOf(NurbSurface.class, capsule.parametrize());
        final double h = Math.sqrt(square(t[0]) + square(t[1]) + square(t[2]));
        final double sin = (rb - rt) / h, cos = Math.sqrt(1 - sin * sin);
        for (final double[] p : samples(nurbs)) {
            final double z = (p[0] * t[0] + p[1] * t[1] + p[2] * t[2]) / h;                     // Height along the axis.
            final double rho = Math.sqrt(Math.max(0, square(p[0]) + square(p[1]) + square(p[2]) - z * z));
            final double toBottom = Math.sqrt(square(p[0]) + square(p[1]) + square(p[2]));
            final double toTop = Math.sqrt(square(p[0] - t[0]) + square(p[1] - t[1]) + square(p[2] - t[2]));
            final double cone = rho * cos + z * sin;          // Equal to rb on the tangent cone, smaller inside.
            final double error = Math.min(Math.abs(cone - rb), Math.min(Math.abs(toBottom - rb), Math.abs(toTop - rt)));
            assertEquals(0, error, 1E-9, "Point on none of the capsule parts.");
            assertTrue(toBottom >= rb - 1E-9 && toTop >= rt - 1E-9 && cone <= rb + 1E-9, "Point not on the convex hull.");
        }
        final double[] inside = {1, 0.5, 1.5};
        for (final double v : new double[] {0.1, 0.5, 0.9}) {
            assertOutward(nurbs, 0.7, v, inside);
        }
    }

    /**
     * A capsule whose bottom sphere contains the top one is the bottom sphere.
     */
    @Test
    public void testCapsuleContainingSphere() {
        final Capsule capsule = F.createCapsule(F.createDataPoints(CRS3D, 0,0,0, 0,0,1));
        capsule.setRadiusBottom(3);
        final NurbSurface nurbs = assertInstanceOf(NurbSurface.class, capsule.parametrize());
        for (final double[] p : samples(nurbs)) {
            assertEquals(3, Math.sqrt(square(p[0]) + square(p[1]) + square(p[2])), TOLERANCE);
        }
    }

    /**
     * A box becomes a polygon in 2 dimensions and a polyhedron in 3 dimensions.
     */
    @Test
    public void testBoxes() {
        assertInstanceOf(Polygon.class, new BBox(CRS2D, 0,0, 10,10).parametrize());
        assertInstanceOf(Polyhedron.class, new BBox(CRS3D, 0,0,0, 1,2,3).parametrize());
        final OBBox oriented = new OBBox(CRS3D);
        oriented.setCenter(Vector.of(1, 2, 3));
        oriented.setXAxis(Vector.of(1, 1, 0));
        oriented.setYAxis(Vector.of(-1, 1, 0));
        oriented.setZAxis(Vector.of(0, 0, 2));
        assertInstanceOf(Polyhedron.class, oriented.parametrize());
    }

    /**
     * A geometry without known exact parametric form causes an exception.
     */
    @Test
    public void testUnsupported() {
        final Vector<?> origin = Vectors.create(new double[] {0, 0}, DataType.DOUBLE);
        final Vector<?> direction = Vectors.create(new double[] {1, 0}, DataType.DOUBLE);
        final Geometry ray = F.createRay(CRS2D, origin, direction);
        assertThrows(UnsupportedOperationException.class, ray::parametrize);
    }

    private static double square(final double x) {
        return x * x;
    }

    /**
     * Returns the positions of the given surface on a 21 × 21 grid of its parameter domain.
     */
    private static List<double[]> samples(final NurbSurface surface) {
        final List<double[]> positions = new ArrayList<>();
        for (int i = 0; i <= 20; i++) {
            for (int j = 0; j <= 20; j++) {
                positions.add(surface.getSurface(i / 20.0, j / 20.0).toArrayDouble());
            }
        }
        return positions;
    }

    /**
     * Asserts that the up-normal of the surface at the given parameters points away from the given inside position.
     * The normal is the cross product of the partial derivatives, estimated by finite differences.
     */
    private static void assertOutward(final NurbSurface surface, final double u, final double v, final double[] inside) {
        final double h = 1E-6;
        final double[] p  = surface.getSurface(u, v).toArrayDouble();
        final double[] pu = surface.getSurface(u + h, v).toArrayDouble();
        final double[] pv = surface.getSurface(u, v + h).toArrayDouble();
        final double[] du = {pu[0] - p[0], pu[1] - p[1], pu[2] - p[2]};
        final double[] dv = {pv[0] - p[0], pv[1] - p[1], pv[2] - p[2]};
        final double[] n = {du[1]*dv[2] - du[2]*dv[1], du[2]*dv[0] - du[0]*dv[2], du[0]*dv[1] - du[1]*dv[0]};
        final double dot = n[0] * (p[0] - inside[0]) + n[1] * (p[1] - inside[1]) + n[2] * (p[2] - inside[2]);
        assertTrue(dot > 0, "The up-normal shall point outward.");
    }
}
