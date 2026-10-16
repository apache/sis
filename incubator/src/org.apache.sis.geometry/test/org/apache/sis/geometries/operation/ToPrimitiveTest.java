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

import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.Prism;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.solid.polyhedron.Hexahedron;
import org.apache.sis.geometries.solid.polyhedron.Icosahedron;
import org.apache.sis.geometries.solid.polyhedron.Tetrahedron;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Array;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ToPrimitiveTest {

    private static final CoordinateReferenceSystem CRS2D = CommonCRS.WGS84.normalizedGeographic();

    @Test
    public void testPoint() {
        final Point point = GeometryFactory.DEFAULT.createPoint(CRS2D, 1, 2);
        final Geometry result = new GeometryProcessor().toPrimitive(point, null, null);
        assertTrue(result instanceof MeshPrimitive);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(MeshPrimitive.Type.POINTS, primitive.getType());
    }

    @Test
    public void testLineString() {
        final Array positions = NDArrays.of(CRS2D, new double[]{0,1,2,3});
        final DataPoints points = GeometryFactory.DEFAULT.createDataPoints(positions);
        final LineString line = GeometryFactory.DEFAULT.createLineString(points);
        final Geometry result = new GeometryProcessor().toPrimitive(line, null, null);
        assertTrue(result instanceof MeshPrimitive);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(MeshPrimitive.Type.LINE_STRIP, primitive.getType());
    }

    /**
     * Tests that a curve is converted to a line strip whose points are on the curve,
     * with the precision given by the offset.
     */
    @Test
    public void testCurve() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final var arc = f.createCircularString(f.createDataPoints(CRS2D, 0,0, 5,5, 10,0));
        final MeshPrimitive coarse = (MeshPrimitive) new GeometryProcessor().toPrimitive(arc, null, Quantities.create(0.1, Units.DEGREE));
        final MeshPrimitive fine   = (MeshPrimitive) new GeometryProcessor().toPrimitive(arc, null, Quantities.create(0.001, Units.DEGREE));
        assertEquals(MeshPrimitive.Type.LINE_STRIP, coarse.getType());
        final Array positions = fine.getPositions();
        assertTrue(positions.getLength() > coarse.getPositions().getLength(), "A smaller offset shall give more points.");
        for (long i = 0; i < positions.getLength(); i++) {
            final double[] p = positions.get(i).toArrayDouble();
            assertEquals(5, Math.hypot(p[0] - 5, p[1]), 1E-9, "Point not on the arc.");
        }
    }

    /**
     * Tests that the spacing and the offset are given to each curve of a collection.
     */
    @Test
    public void testMultiCurve() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final var arc1 = f.createCircularString(f.createDataPoints(CRS2D, 0,0, 5,5, 10,0));
        final var arc2 = f.createCircularString(f.createDataPoints(CRS2D, 20,0, 25,5, 30,0));
        final Geometry result = new GeometryProcessor().toPrimitive(f.createMultiCurve(CRS2D, arc1, arc2),
                Quantities.create(1, Units.DEGREE), null);
        assertTrue(result instanceof MultiMeshPrimitive);
        for (final Object component : ((MultiMeshPrimitive<?>) result).getComponents()) {
            final Array positions = ((MeshPrimitive) component).getPositions();
            // A half circle of radius 5 is 15.7 long, so a spacing of 1 needs at least 16 segments.
            assertTrue(positions.getLength() >= 17, "Spacing not applied: " + positions.getLength() + " points.");
        }
    }

    /**
     * Tests that a spacing densifies the line strings of a collection.
     */
    @Test
    public void testMultiLineStringWithSpacing() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final var lines = f.createMultiLineString(CRS2D,
                f.createLineString(f.createDataPoints(CRS2D, 0,0, 10,0)),
                f.createLineString(f.createDataPoints(CRS2D, 0,5, 10,5)));
        assertEquals(MeshPrimitive.Type.LINES, ((MeshPrimitive) new GeometryProcessor().toPrimitive(lines, null, null)).getType());
        final Geometry result = new GeometryProcessor().toPrimitive(lines, Quantities.create(2.5, Units.DEGREE), null);
        assertTrue(result instanceof MultiMeshPrimitive);
        for (final Object component : ((MultiMeshPrimitive<?>) result).getComponents()) {
            assertEquals(5, ((MeshPrimitive) component).getPositions().getLength());
        }
    }

    @Test
    public void testPolygon() {
        final LinearRing exterior = ring(0,0, 2,0, 2,2, 0,2, 0,0);
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(exterior, null);
        final Geometry result = new GeometryProcessor().toPrimitive(polygon, null, null);
        assertTrue(result instanceof MeshPrimitive);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(MeshPrimitive.Type.TRIANGLES, primitive.getType());
        assertEquals(6, primitive.getPositions().getLength(), "Expected 2 triangles.");
        assertEquals(4, trianglesArea(primitive), 1E-12);
    }

    /**
     * Tests the triangulation of a polygon with a hole, which requires to cut the exterior
     * ring toward the hole. The triangles shall cover the polygon area without the hole.
     */
    @Test
    public void testPolygonWithHole() {
        final LinearRing exterior = ring(0,0, 10,0, 10,10, 0,10, 0,0);
        final LinearRing hole     = ring(2,2, 2,4, 4,4, 4,2, 2,2);
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(exterior, java.util.List.of(hole));
        final MeshPrimitive primitive = (MeshPrimitive) new GeometryProcessor().toPrimitive(polygon, null, null);
        assertEquals(MeshPrimitive.Type.TRIANGLES, primitive.getType());
        assertEquals(96, trianglesArea(primitive), 1E-9);
    }

    /**
     * Same test as {@link #testPolygonWithHole()}, with the hole ring starting at a point which is
     * not its leftmost one. The cut toward the hole shall still start from the leftmost point.
     */
    @Test
    public void testPolygonWithHoleStartingAnywhere() {
        final LinearRing exterior = ring(0,0, 10,0, 10,10, 0,10, 0,0);
        final LinearRing hole     = ring(4,4, 4,2, 2,2, 2,4, 4,4);
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(exterior, java.util.List.of(hole));
        final MeshPrimitive primitive = (MeshPrimitive) new GeometryProcessor().toPrimitive(polygon, null, null);
        assertEquals(96, trianglesArea(primitive), 1E-9);
    }

    /**
     * Tests the triangulation of a three-dimensional polygon, which shall keep the z coordinates.
     */
    @Test
    public void testPolygon3D() {
        final LinearRing exterior = GeometryFactory.DEFAULT.createLinearRing(GeometryFactory.DEFAULT.createDataPoints(
                NDArrays.of(CommonCRS.WGS84.geographic3D(), 0,0,5, 2,0,5, 2,2,7, 0,2,7, 0,0,5)));
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(exterior, null);
        final MeshPrimitive primitive = (MeshPrimitive) new GeometryProcessor().toPrimitive(polygon, null, null);
        assertEquals(MeshPrimitive.Type.TRIANGLES, primitive.getType());
        assertEquals(3, primitive.getPositions().getDimension());
        assertEquals(6, primitive.getPositions().getLength(), "Expected 2 triangles.");
        final double[] c = primitive.getPositions().toArrayDouble();
        for (int i = 0; i < c.length; i += 3) {
            assertEquals(c[i+1] == 0 ? 5 : 7, c[i+2], "z shall follow the polygon plane.");
        }
    }

    /**
     * Tests the triangulation of a vertical polygon with a hole: a wall with a window.
     * The polygon is degenerate in the (x,y) plane, so it shall be triangulated in its own plane.
     */
    @Test
    public void testVerticalPolygonWithHole() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic3D();
        final LinearRing exterior = ring3D(crs, 0,0,0, 6,0,0, 6,0,4, 0,0,4, 0,0,0);
        final LinearRing window   = ring3D(crs, 2,0,1, 2,0,3, 4,0,3, 4,0,1, 2,0,1);
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(exterior, java.util.List.of(window));
        final MeshPrimitive primitive = (MeshPrimitive) new GeometryProcessor().toPrimitive(polygon, null, null);
        assertEquals(20, trianglesArea3D(primitive), 1E-9);
        final double[] c = primitive.getPositions().toArrayDouble();
        for (int i = 1; i < c.length; i += 3) {
            assertEquals(0, c[i], "y shall stay in the wall plane.");
        }
    }

    /**
     * Tests the triangulation of a vertical polygon in the (y,z) plane, which uses another order of
     * coordinates than {@link #testVerticalPolygonWithHole()}. The x coordinate shall be restored.
     */
    @Test
    public void testVerticalPolygonInYZPlane() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic3D();
        final LinearRing exterior = ring3D(crs, 3,0,0, 3,5,0, 3,5,2, 3,0,2, 3,0,0);
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(exterior, null);
        final MeshPrimitive primitive = (MeshPrimitive) new GeometryProcessor().toPrimitive(polygon, null, null);
        assertEquals(10, trianglesArea3D(primitive), 1E-9);
        final double[] c = primitive.getPositions().toArrayDouble();
        for (int i = 0; i < c.length; i += 3) {
            assertEquals(3, c[i], "x shall stay in the wall plane.");
        }
    }

    /**
     * Tests the triangulation of a horizontal three-dimensional polygon with a hole.
     * The points added to cut the exterior ring toward the hole shall keep their z coordinate.
     */
    @Test
    public void testPolygon3DWithHole() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic3D();
        final LinearRing exterior = ring3D(crs, 0,0,5, 10,0,5, 10,10,5, 0,10,5, 0,0,5);
        final LinearRing hole     = ring3D(crs, 2,2,5, 2,4,5, 4,4,5, 4,2,5, 2,2,5);
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(exterior, java.util.List.of(hole));
        final MeshPrimitive primitive = (MeshPrimitive) new GeometryProcessor().toPrimitive(polygon, null, null);
        assertEquals(96, trianglesArea3D(primitive), 1E-9);
        final double[] c = primitive.getPositions().toArrayDouble();
        for (int i = 2; i < c.length; i += 3) {
            assertEquals(5, c[i], "z shall be preserved.");
        }
    }

    /**
     * Tests the conversion of a TIN, which shall give a single mesh of all its triangles.
     */
    @Test
    public void testTIN() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic3D();
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final org.apache.sis.geometries.surface.TIN tin = f.createTIN(crs, new org.apache.sis.geometries.surface.Triangle[] {
            f.createTriangle(ring3D(crs, 0,0,0, 2,0,0, 0,2,1, 0,0,0)),
            f.createTriangle(ring3D(crs, 2,0,0, 2,2,1, 0,2,1, 2,0,0))});
        final Geometry result = new GeometryProcessor().toPrimitive(tin, null, null);
        assertTrue(result instanceof MeshPrimitive.Triangles);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(3, primitive.getPositions().getDimension());
        assertEquals(6, primitive.getPositions().getLength(), "Expected 2 triangles.");
        assertEquals(2 * Math.sqrt(5), trianglesArea3D(primitive), 1E-9);
    }

    /**
     * Tests the conversion of a polyhedral surface made of the five faces of an open box,
     * including vertical ones. All faces shall be triangulated in a single mesh.
     */
    @Test
    public void testPolyhedralSurface() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic3D();
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final Polygon[] faces = {
            f.createPolygon(ring3D(crs, 0,0,0, 0,0,2, 0,2,2, 0,2,0, 0,0,0), null),
            f.createPolygon(ring3D(crs, 2,0,0, 2,2,0, 2,2,2, 2,0,2, 2,0,0), null),
            f.createPolygon(ring3D(crs, 0,0,0, 2,0,0, 2,0,2, 0,0,2, 0,0,0), null),
            f.createPolygon(ring3D(crs, 0,2,0, 0,2,2, 2,2,2, 2,2,0, 0,2,0), null),
            f.createPolygon(ring3D(crs, 0,0,0, 0,2,0, 2,2,0, 2,0,0, 0,0,0), null)};
        final Geometry result = new GeometryProcessor().toPrimitive(f.createPolyhedralSurface(crs, faces), null, null);
        assertTrue(result instanceof MeshPrimitive.Triangles);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(30, primitive.getPositions().getLength(), "Expected 2 triangles per face.");
        assertEquals(20, trianglesArea3D(primitive), 1E-9);
    }

    /**
     * Returns the six faces of the cube between the given corners.
     */
    private static GeometryCollection<Polygon> cube(final CoordinateReferenceSystem crs,
            final double x0, final double y0, final double z0, final double x1, final double y1, final double z1)
    {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        return f.createMultiPolygon(crs,
            f.createPolygon(ring3D(crs, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, x0,y0,z0), null),
            f.createPolygon(ring3D(crs, x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1, x1,y0,z0), null),
            f.createPolygon(ring3D(crs, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1, x0,y0,z0), null),
            f.createPolygon(ring3D(crs, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0), null),
            f.createPolygon(ring3D(crs, x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0, x0,y0,z0), null),
            f.createPolygon(ring3D(crs, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, x0,y0,z1), null));
    }

    /**
     * A polyhedron becomes the triangles of the faces of its shell.
     */
    @Test
    public void testPolyhedron() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic3D();
        final Polyhedron cube = GeometryFactory.DEFAULT.createPolyhedron(cube(crs, 0,0,0, 2,2,2), List.of());
        final Geometry result = new GeometryProcessor().toPrimitive(cube, null, null);
        assertTrue(result instanceof MeshPrimitive.Triangles);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(crs, primitive.getCoordinateReferenceSystem());
        assertEquals(36, primitive.getPositions().getLength(), "Expected 2 triangles per face.");
        assertEquals(24, trianglesArea3D(primitive), 1E-9);
    }

    /**
     * The faces of the interior shells of a polyhedron are triangulated as well as the exterior ones.
     */
    @Test
    public void testPolyhedronWithInteriorShell() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic3D();
        final Polyhedron hollow = GeometryFactory.DEFAULT.createPolyhedron(cube(crs, 0,0,0, 4,4,4), List.of(cube(crs, 1,1,1, 3,3,3)));
        final MeshPrimitive primitive = (MeshPrimitive) new GeometryProcessor().toPrimitive(hollow, null, null);
        assertEquals(72, primitive.getPositions().getLength(), "Expected 2 triangles per face of both shells.");
        assertEquals(96 + 24, trianglesArea3D(primitive), 1E-9);
    }

    /**
     * The regular polyhedra compute their faces from shared vertices.
     */
    @Test
    public void testRegularPolyhedron() {
        for (final Polyhedron polyhedron : new Polyhedron[] {new Tetrahedron(), new Hexahedron(), new Icosahedron()}) {
            final MeshPrimitive primitive = (MeshPrimitive) new GeometryProcessor().toPrimitive(polyhedron, null, null);
            double expected = 0;
            final GeometryCollection<Polygon> shell = polyhedron.getExteriorShell();
            for (int i = 0; i < shell.getNumGeometries(); i++) {
                expected += shell.getGeometryN(i).getArea().getValue().doubleValue();
            }
            assertEquals(expected, trianglesArea3D(primitive), 1E-9, polyhedron.getClass().getSimpleName());
        }
    }

    /**
     * The prism of a polygon is a solid, which becomes the triangles of the faces of its polyhedron.
     */
    @Test
    public void testPrism() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final Polygon square = f.createPolygon(f.createLinearRing(f.createDataPoints(
                NDArrays.of(CommonCRS.WGS84.normalizedGeographic(), new double[] {0,0, 2,0, 2,2, 0,2, 0,0}))), null);
        final Prism prism = f.createPrism(square, NumberRange.create(0., true, 3., true), null);
        final Geometry result = new GeometryProcessor().toPrimitive(prism, null, null);
        assertTrue(result instanceof MeshPrimitive.Triangles);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(3, primitive.getCoordinateReferenceSystem().getCoordinateSystem().getDimension());
        assertEquals(36, primitive.getPositions().getLength(), "Expected 2 triangles per face.");
        assertEquals(2*4 + 4*6, trianglesArea3D(primitive), 1E-9);
    }

    /**
     * The prism of a point becomes a line strip, and the prism of a line string the triangles of its walls.
     */
    @Test
    public void testPrismOfPointAndLineString() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();
        final NumberRange<Double> range = NumberRange.create(0., true, 3., true);

        final Geometry strip = new GeometryProcessor().toPrimitive(f.createPrism(f.createPoint(crs, 1, 2), range, null), null, null);
        assertTrue(strip instanceof MeshPrimitive.LineStrip);
        assertEquals(2, ((MeshPrimitive) strip).getPositions().getLength());

        final LineString line = f.createLineString(f.createDataPoints(NDArrays.of(crs, new double[] {0,0, 2,0, 2,1})));
        final Geometry walls = new GeometryProcessor().toPrimitive(f.createPrism(line, range, null), null, null);
        assertTrue(walls instanceof MeshPrimitive.Triangles);
        assertEquals(12, ((MeshPrimitive) walls).getPositions().getLength(), "Expected 2 triangles per segment.");
        assertEquals(2*3 + 1*3, trianglesArea3D((MeshPrimitive) walls), 1E-9);
    }

    private static LinearRing ring3D(final CoordinateReferenceSystem crs, final double... coordinates) {
        return GeometryFactory.DEFAULT.createLinearRing(
                GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(crs, coordinates)));
    }

    /**
     * Returns the sum of the areas of the triangles of the given non-indexed three-dimensional mesh.
     */
    private static double trianglesArea3D(final MeshPrimitive primitive) {
        final double[] c = primitive.getPositions().toArrayDouble();
        double area = 0;
        for (int i = 0; i + 8 < c.length; i += 9) {
            final double ux = c[i+3] - c[i], uy = c[i+4] - c[i+1], uz = c[i+5] - c[i+2];
            final double vx = c[i+6] - c[i], vy = c[i+7] - c[i+1], vz = c[i+8] - c[i+2];
            final double nx = uy*vz - uz*vy, ny = uz*vx - ux*vz, nz = ux*vy - uy*vx;
            area += Math.sqrt(nx*nx + ny*ny + nz*nz) / 2;
        }
        return area;
    }

    /**
     * Creates a ring in the two-dimensional test CRS.
     */
    private static LinearRing ring(final double... coordinates) {
        return GeometryFactory.DEFAULT.createLinearRing(
                GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(CRS2D, coordinates)));
    }

    /**
     * Returns the sum of the areas of the triangles of the given non-indexed mesh.
     */
    private static double trianglesArea(final MeshPrimitive primitive) {
        final double[] c = primitive.getPositions().toArrayDouble();
        double area = 0;
        for (int i = 0; i + 5 < c.length; i += 6) {
            area += Math.abs((c[i+2] - c[i]) * (c[i+5] - c[i+1]) - (c[i+4] - c[i]) * (c[i+3] - c[i+1])) / 2;
        }
        return area;
    }

    @Test
    public void testMultiLineString() {
        { //one line has 3 points, so we must obtain a MultiPrimitive
            final Array positions1 = NDArrays.of(CRS2D, new double[]{0,1,2,3});
            final DataPoints points1 = GeometryFactory.DEFAULT.createDataPoints(positions1);
            final LineString line1 = GeometryFactory.DEFAULT.createLineString(points1);

            final Array positions2 = NDArrays.of(CRS2D, new double[]{3,4,5,6,7,8});
            final DataPoints points2 = GeometryFactory.DEFAULT.createDataPoints(positions2);
            final LineString line2 = GeometryFactory.DEFAULT.createLineString(points2);

            final GeometryCollection<LineString> mlines = GeometryFactory.DEFAULT.createMultiLineString(line1, line2);

            final Geometry result = new GeometryProcessor().toPrimitive(mlines, null, null);
            assertTrue(result instanceof MultiMeshPrimitive);
            final MultiMeshPrimitive mp = (MultiMeshPrimitive) result;
            assertEquals(2, mp.getNumGeometries());
            assertTrue(mp.getGeometryN(0) instanceof MeshPrimitive);
            assertTrue(mp.getGeometryN(1) instanceof MeshPrimitive);

            final MeshPrimitive primitive1 = (MeshPrimitive) mp.getGeometryN(0);
            assertEquals(MeshPrimitive.Type.LINE_STRIP, primitive1.getType());

            final MeshPrimitive primitive2 = (MeshPrimitive) mp.getGeometryN(1);
            assertEquals(MeshPrimitive.Type.LINE_STRIP, primitive2.getType());
        }
        { //all linestrings are lines, we must obtain a Primitive.Lines
            final Array positions1 = NDArrays.of(CRS2D, new double[]{0,1,2,3});
            final DataPoints points1 = GeometryFactory.DEFAULT.createDataPoints(positions1);
            final LineString line1 = GeometryFactory.DEFAULT.createLineString(points1);

            final Array positions2 = NDArrays.of(CRS2D, new double[]{3,4,5,6});
            final DataPoints points2 = GeometryFactory.DEFAULT.createDataPoints(positions2);
            final LineString line2 = GeometryFactory.DEFAULT.createLineString(points2);

            final GeometryCollection<LineString> mlines = GeometryFactory.DEFAULT.createMultiLineString(line1, line2);

            final Geometry result = new GeometryProcessor().toPrimitive(mlines, null, null);
            assertTrue(result instanceof MeshPrimitive.Lines);
            final MeshPrimitive.Lines mp = (MeshPrimitive.Lines) result;
            assertEquals(2, mp.getNumGeometries());
            LineString l1 = mp.getGeometryN(0);
            LineString l2 = mp.getGeometryN(1);
            assertArrayEquals(new double[]{0,1,2,3}, l1.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble(), 0.0);
            assertArrayEquals(new double[]{3,4,5,6}, l2.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble(), 0.0);
        }
    }

    @Test
    public void testMultiPoint() {
        final Array positions1 = NDArrays.of(CRS2D, new double[]{0,1,2,3});
        final DataPoints points = GeometryFactory.DEFAULT.createDataPoints(positions1);
        final GeometryCollection<Point> mpoints = GeometryFactory.DEFAULT.createMultiPoint(points);

        final Geometry result = new GeometryProcessor().toPrimitive(mpoints, null, null);
        assertTrue(result instanceof MeshPrimitive);
        final MeshPrimitive primitive1 = (MeshPrimitive) result;
        assertEquals(MeshPrimitive.Type.POINTS, primitive1.getType());
    }
}
