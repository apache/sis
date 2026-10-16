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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.util.FactoryException;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link Prism}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class PrismTest extends GeometryTest {
    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetAttributesType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetBase() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetExtrusionRange() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetExtrusionCrs() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetCoordinateReferenceSystem() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testSetCoordinateReferenceSystem() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetMetadata() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDimension() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDimension_DirectPosition() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIs3D() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetSpatialDimension() {
    }

    @Test
    public void testGetTopologicDimension() {
        final Point point = GeometryFactory.DEFAULT.createPoint(CRS_2D, 1, 2);
        final Prism pointPrism = GeometryFactory.DEFAULT.createPrism(point, NumberRange.create(0., true, 10., true), null);
        assertEquals(1, pointPrism.getTopologicDimension(), "Extruding a point gives a curve.");

        final LineString segment = GeometryFactory.DEFAULT.createLineString(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,0));
        final Prism surfacePrism = GeometryFactory.DEFAULT.createPrism(segment, NumberRange.create(0., true, 10., true), null);
        assertEquals(2, surfacePrism.getTopologicDimension(), "Extruding a curve gives a surface.");
    }

    /**
     * Name of the attribute holding {@code x + 10*y} at each position, for checking that it is copied.
     */
    private static final String ATT_TEST = "TEST";

    /**
     * Returns the given positions, each one with a {@value #ATT_TEST} attribute.
     */
    private static DataPoints points(final CoordinateReferenceSystem crs, final double... coordinates) {
        final int dimension = crs.getCoordinateSystem().getDimension();
        final double[] values = new double[coordinates.length / dimension];
        for (int i = 0; i < values.length; i++) {
            values[i] = coordinates[i * dimension] + 10 * coordinates[i * dimension + 1];
        }
        final Map<String,Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, NDArrays.of(crs, coordinates));
        attributes.put(ATT_TEST, NDArrays.of(1, values));
        return GeometryFactory.DEFAULT.createDataPoints(attributes);
    }

    /**
     * Returns a ring of the given positions, each one with a {@value #ATT_TEST} attribute.
     */
    private static LinearRing ring(final CoordinateReferenceSystem crs, final double... coordinates) {
        return GeometryFactory.DEFAULT.createLinearRing(points(crs, coordinates));
    }

    /**
     * Returns a square polygon of the given size, whose positions have a {@value #ATT_TEST} attribute.
     */
    private static Polygon square(final double x, final double y, final double size) {
        return GeometryFactory.DEFAULT.createPolygon(ring(CRS_2D,
                x, y,  x + size, y,  x + size, y + size,  x, y + size,  x, y), null);
    }

    /**
     * Returns the prism of the given base, extruded from the given lower limit to the given upper limit.
     */
    private static Prism prism(final Geometry base, final double min, final double max) {
        return GeometryFactory.DEFAULT.createPrism(base, NumberRange.create(min, true, max, true), null);
    }

    /**
     * Returns the volume enclosed by the exterior shell of the given polyhedron,
     * positive if its faces are oriented outward.
     */
    private static double signedVolume(final Polyhedron polyhedron) {
        final GeometryCollection<Polygon> shell = polyhedron.getExteriorShell();
        double volume = 0;
        for (int f = 0; f < shell.getNumGeometries(); f++) {
            final Polygon face = shell.getGeometryN(f);
            final var rings = new ArrayList<LinearRing>();
            rings.add(face.getExteriorRing());
            rings.addAll(face.getInteriorRings());
            for (final LinearRing ring : rings) {
                final DataPoints points = ring.getDataPoints();
                final Vector<?> a = points.getPosition(0);
                for (int i = 2; i < points.size(); i++) {
                    volume += a.dot(points.getPosition(i-1).cross(points.getPosition(i))) / 6;
                }
            }
        }
        return volume;
    }

    /**
     * Verifies that each position of the given extrusion has the {@value #ATT_TEST} attribute of the base,
     * and that its coordinate on the extrusion axis is one of the given limits.
     */
    private static void assertPositions(final Geometry extrusion, final int axis, final double min, final double max) {
        final DataPoints points = extrusion.getDataPoints();
        assertTrue(points.size() > 0);
        for (int i = 0; i < points.size(); i++) {
            final Vector<?> p = points.getPosition(i);
            final int h0 = (axis == 0) ? 1 : 0;
            final int h1 = (axis == 2) ? 1 : 2;
            assertEquals(p.get(h0) + 10 * p.get(h1), points.getAttribute(i, ATT_TEST).get(0), TOLERANCE);
            final double value = p.get(axis);
            assertTrue(value == min || value == max, "Not an extrusion limit: " + value);
        }
    }

    /**
     * The prism of a point becomes the line string joining the two limits.
     */
    @Test
    public void testToExtrusionOfPoint() {
        final Point point = GeometryFactory.DEFAULT.createPoint(points(CRS_2D, 1, 2));
        final LineString line = assertInstanceOf(LineString.class, prism(point, 0, 5).toExtrusion());
        assertEquals(3, line.getDimension());
        final DataPoints points = line.getDataPoints();
        assertEquals(2, points.size());
        assertArrayEquals(new double[] {1, 2, 0}, points.getPosition(0).toArrayDouble());
        assertArrayEquals(new double[] {1, 2, 5}, points.getPosition(1).toArrayDouble());
        assertPositions(line, 2, 0, 5);
    }

    /**
     * The prism of a line string becomes a polyhedral surface of one quadrilateral by segment.
     * A repeated position adds no quadrilateral.
     */
    @Test
    public void testToExtrusionOfLineString() {
        final LineString base = GeometryFactory.DEFAULT.createLineString(points(CRS_2D, 0,0, 2,0, 2,0, 2,1));
        final PolyhedralSurface<?> surface = assertInstanceOf(PolyhedralSurface.class, prism(base, 0, 3).toExtrusion());
        assertEquals(2, surface.getNumPatches());
        assertEquals(2*3 + 1*3, surface.getArea().getValue().doubleValue(), TOLERANCE);
        for (int i = 0; i < surface.getNumPatches(); i++) {
            assertPositions(surface.getPatchN(i), 2, 0, 3);
        }
    }

    /**
     * The prism of a collection becomes the collection of the extrusions of its elements.
     */
    @Test
    public void testToExtrusionOfCollection() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final GeometryCollection<?> polyhedra = assertInstanceOf(GeometryCollection.class,
                prism(f.createMultiPolygon(CRS_2D, square(0, 0, 1), square(3, 0, 2)), 0, 2).toExtrusion());
        assertEquals(GeometryType.POLYHEDRON, polyhedra.getElementType());
        assertEquals(2, polyhedra.getNumGeometries());
        assertEquals(1*1*2, signedVolume(assertInstanceOf(Polyhedron.class, polyhedra.getGeometryN(0))), TOLERANCE);
        assertEquals(2*2*2, signedVolume(assertInstanceOf(Polyhedron.class, polyhedra.getGeometryN(1))), TOLERANCE);

        final GeometryCollection<?> mixed = assertInstanceOf(GeometryCollection.class, prism(f.createGeometryCollection(CRS_2D,
                f.createPoint(points(CRS_2D, 1, 2)),
                f.createLineString(points(CRS_2D, 0,0, 2,0)),
                square(5, 5, 1)), 0, 1).toExtrusion());
        assertEquals(GeometryType.GEOMETRY, mixed.getElementType());
        assertEquals(3, mixed.getCoordinateReferenceSystem().getCoordinateSystem().getDimension());
        assertInstanceOf(LineString.class,        mixed.getGeometryN(0));
        assertInstanceOf(PolyhedralSurface.class, mixed.getGeometryN(1));
        assertInstanceOf(Polyhedron.class,        mixed.getGeometryN(2));
        for (int i = 0; i < mixed.getNumGeometries(); i++) {
            assertPositions(mixed.getGeometryN(i), 2, 0, 1);
        }
    }

    /**
     * The prism of a square becomes a box whose faces are oriented outward,
     * whatever the orientation of the square.
     */
    @Test
    public void testToExtrusionOfPolygon() {
        for (final boolean clockwise : new boolean[] {false, true}) {
            final LinearRing square = clockwise ? ring(CRS_2D, 0,0, 0,2, 2,2, 2,0, 0,0)
                                                : ring(CRS_2D, 0,0, 2,0, 2,2, 0,2, 0,0);
            final Prism prism = GeometryFactory.DEFAULT.createPrism(
                    GeometryFactory.DEFAULT.createPolygon(square, null), NumberRange.create(1., true, 4., true), null);
            final Polyhedron polyhedron = (Polyhedron) prism.toExtrusion();
            assertEquals(3, polyhedron.getDimension());
            assertEquals(6, polyhedron.getExteriorShell().getNumGeometries(), "Expected 2 caps and 4 walls.");
            assertTrue(polyhedron.getInteriorShells().isEmpty());
            assertEquals(12, signedVolume(polyhedron), TOLERANCE);
            assertPositions(polyhedron, 2, 1, 4);
        }
    }

    /**
     * The holes of the base become tunnels through the polyhedron, still oriented outward.
     */
    @Test
    public void testToExtrusionOfPolygonWithHole() {
        final Polygon base = GeometryFactory.DEFAULT.createPolygon(ring(CRS_2D, 0,0, 4,0, 4,4, 0,4, 0,0),
                List.of(ring(CRS_2D, 1,1, 3,1, 3,3, 1,3, 1,1)));
        final Prism prism = GeometryFactory.DEFAULT.createPrism(base, NumberRange.create(0., true, 2., true), null);
        final Polyhedron polyhedron = (Polyhedron) prism.toExtrusion();
        final GeometryCollection<Polygon> shell = polyhedron.getExteriorShell();
        assertEquals(10, shell.getNumGeometries(), "Expected 2 caps and 8 walls.");
        assertEquals(1, shell.getGeometryN(0).getNumInteriorRing(), "The lower cap shall keep the hole.");
        assertEquals((16 - 4) * 2, signedVolume(polyhedron), TOLERANCE);
        assertPositions(polyhedron, 2, 0, 2);
    }

    /**
     * When the reference system of the base contains the extrusion one,
     * the extrusion replaces the coordinate on the corresponding axis.
     */
    @Test
    public void testToExtrusionAlongBaseAxis() throws FactoryException {
        final VerticalCRS vertical = CommonCRS.Vertical.MEAN_SEA_LEVEL.crs();
        final CoordinateReferenceSystem crs = CRS.compound(CRS_2D, vertical);
        final Polygon base = GeometryFactory.DEFAULT.createPolygon(ring(crs, 0,0,5, 2,0,5, 2,3,5, 0,3,5, 0,0,5), null);
        final Prism prism = GeometryFactory.DEFAULT.createPrism(base, NumberRange.create(-1., true, 1., true), vertical);
        final Polyhedron polyhedron = (Polyhedron) prism.toExtrusion();
        assertEquals(crs, polyhedron.getCoordinateReferenceSystem());
        assertEquals(6, polyhedron.getExteriorShell().getNumGeometries());
        assertEquals(12, signedVolume(polyhedron), TOLERANCE);
        assertPositions(polyhedron, 2, -1, 1);
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetBoundaryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType2() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetEnvelope() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetCentroid() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetRepresentativePoint() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetClosure() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetMaximalComplex() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsEmpty() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsSimple() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsCycle() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsValid() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetBoundary() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testUserProperties() {
    }
}
