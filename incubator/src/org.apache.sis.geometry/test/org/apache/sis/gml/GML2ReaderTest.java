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
package org.apache.sis.gml;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.MultiLineString;
import org.apache.sis.geometries.MultiPoint;
import org.apache.sis.geometries.MultiPolygon;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.gml.GeometryAssert.assertCRS;
import static org.apache.sis.gml.GeometryAssert.assertGeometryEquals;
import static org.apache.sis.gml.GeometryAssert.assertUndefinedCRS;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Tests the {@link GML2Reader} class.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GML2ReaderTest {
    /**
     * The CRS that all fixture files declare through {@code srsName="EPSG:4326"}.
     */
    private final CoordinateReferenceSystem wgs84;

    /**
     * Creates a new test case.
     */
    public GML2ReaderTest() throws Exception {
        wgs84 = CRS.forCode("EPSG:4326");
    }

    /**
     * Reads the geometry contained in the given test file.
     */
    private static Geometry read(final String filename) throws Exception {
        try (InputStream in = TestData.V2.openStream(filename);
             GML2Reader reader = new GML2Reader(in))
        {
            return reader.readGeometry();
        }
    }

    /**
     * Reads the geometry contained in the given XML text.
     */
    private static Geometry readInline(final String xml) throws Exception {
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
             GML2Reader reader = new GML2Reader(in))
        {
            return reader.readGeometry();
        }
    }

    /**
     * Creates a point sequence in the {@link #wgs84} CRS from a flat list of ordinates.
     */
    private PointSequence sequence(final double... ordinates) {
        return GeometryFactory.createSequence(NDArrays.of(SampleSystem.of(wgs84), ordinates));
    }

    /**
     * Creates a closed rectangular ring in the {@link #wgs84} CRS.
     */
    private LinearRing ring(final double minX, final double minY, final double maxX, final double maxY) {
        return GeometryFactory.createLinearRing(sequence(
                minX, minY,
                maxX, minY,
                maxX, maxY,
                minX, maxY,
                minX, minY));
    }

    /**
     * Tests reading a {@code <gml:Point>} element using the {@code gml:coordinates} form.
     */
    @Test
    public void testPoint() throws Exception {
        final Geometry g = read(TestData.POINT);
        assertInstanceOf(Point.class, g);
        assertGeometryEquals(GeometryFactory.createPoint(sequence(10.0, 20.0)), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:Point>} element using the {@code gml:coord} form.
     */
    @Test
    public void testPointCoordForm() throws Exception {
        final Geometry g = read(TestData.POINT_COORD);
        assertInstanceOf(Point.class, g);
        assertGeometryEquals(GeometryFactory.createPoint(sequence(10.0, 20.0)), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:LineString>} element using the {@code gml:coordinates} form.
     */
    @Test
    public void testLineString() throws Exception {
        final Geometry g = read(TestData.LINE_STRING);
        assertInstanceOf(LineString.class, g);
        assertGeometryEquals(GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0, 20.0, 0.0)), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:LineString>} element using the {@code gml:coord} form.
     */
    @Test
    public void testLineStringCoordForm() throws Exception {
        final Geometry g = read(TestData.LINE_STRING_COORD);
        assertInstanceOf(LineString.class, g);
        assertGeometryEquals(GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0, 20.0, 0.0)), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a standalone {@code <gml:LinearRing>} element.
     */
    @Test
    public void testLinearRing() throws Exception {
        final Geometry g = read(TestData.LINEAR_RING);
        assertInstanceOf(LinearRing.class, g);
        assertGeometryEquals(ring(0, 0, 10, 10), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:Polygon>} element with an outer boundary and one inner boundary (hole).
     */
    @Test
    public void testPolygon() throws Exception {
        final Geometry g = read(TestData.POLYGON);
        assertInstanceOf(Polygon.class, g);
        assertGeometryEquals(GeometryFactory.createPolygon(ring(0, 0, 10, 10), List.of(ring(2, 2, 4, 4))), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:Box>} element, which becomes a {@link BBox}.
     */
    @Test
    public void testBox() throws Exception {
        final Geometry g = read(TestData.BOX);
        final BBox box = assertInstanceOf(BBox.class, g);
        assertEquals(2, box.getDimension());
        assertEquals( 0.0, box.getMinimum(0), GeometryAssert.TOLERANCE);
        assertEquals( 0.0, box.getMinimum(1), GeometryAssert.TOLERANCE);
        assertEquals(10.0, box.getMaximum(0), GeometryAssert.TOLERANCE);
        assertEquals(10.0, box.getMaximum(1), GeometryAssert.TOLERANCE);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:MultiPoint>} element.
     */
    @Test
    public void testMultiPoint() throws Exception {
        final Geometry g = read(TestData.MULTI_POINT);
        assertInstanceOf(MultiPoint.class, g);
        assertGeometryEquals(GeometryFactory.createMultiPoint(
                GeometryFactory.createPoint(sequence(0.0, 0.0)),
                GeometryFactory.createPoint(sequence(10.0, 10.0))), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:MultiLineString>} element.
     */
    @Test
    public void testMultiLineString() throws Exception {
        final Geometry g = read(TestData.MULTI_LINE_STRING);
        assertInstanceOf(MultiLineString.class, g);
        assertGeometryEquals(GeometryFactory.createMultiLineString(
                GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0)),
                GeometryFactory.createLineString(sequence(20.0, 20.0, 30.0, 30.0, 40.0, 20.0))), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:MultiPolygon>} element.
     */
    @Test
    public void testMultiPolygon() throws Exception {
        final Geometry g = read(TestData.MULTI_POLYGON);
        assertInstanceOf(MultiPolygon.class, g);
        assertGeometryEquals(GeometryFactory.createMultiPolygon(
                GeometryFactory.createPolygon(ring(0, 0, 10, 10), null),
                GeometryFactory.createPolygon(ring(20, 20, 30, 30), null)), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a heterogeneous {@code <gml:MultiGeometry>} element.
     */
    @Test
    public void testMultiGeometry() throws Exception {
        final Geometry g = read(TestData.MULTI_GEOMETRY);
        assertInstanceOf(GeometryCollection.class, g);
        assertGeometryEquals(GeometryFactory.createGeometryCollection(
                GeometryFactory.createPoint(sequence(0.0, 0.0)),
                GeometryFactory.createLineString(sequence(10.0, 10.0, 20.0, 20.0))), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a document with no {@code xmlns:gml} declaration at all (unqualified elements).
     * The reader is expected to tolerate the missing namespace.
     */
    @Test
    public void testMissingNamespace() throws Exception {
        final Geometry g = read(TestData.NO_NAMESPACE);
        assertInstanceOf(Point.class, g);
        assertGeometryEquals(GeometryFactory.createPoint(sequence(10.0, 20.0)), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests that a document declaring no {@code srsName} still yields a geometry with a coordinate
     * reference system, since an Apache SIS geometry cannot exist without one. The substituted CRS
     * is the placeholder that {@code Geometries.isUndefined(…)} recognises, so that a writer knows
     * not to invent an {@code srsName} for it.
     */
    @Test
    public void testNoSrsNameUsesUndefinedCRS() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\">"
                + "<gml:coordinates>10.0,20.0</gml:coordinates>"
                + "</gml:Point>";
        final Geometry g = readInline(xml);
        assertInstanceOf(Point.class, g);
        assertUndefinedCRS(g);
        assertEquals(2, g.getCoordinateReferenceSystem().getCoordinateSystem().getDimension());
    }

    /**
     * Tests that a three-dimensional {@code gml:coordinates} tuple is read at its true width,
     * rather than being truncated to the two dimensions of the declared {@code srsName}.
     */
    @Test
    public void testThreeDimensionalCoordinates() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:4326\">"
                + "<gml:coordinates>10.0,20.0,30.0</gml:coordinates>"
                + "</gml:Point>";
        final Geometry g = readInline(xml);
        final Point p = assertInstanceOf(Point.class, g);
        assertEquals(3, p.getPosition().getDimension());
        assertEquals(30.0, p.getPosition().get(2), GeometryAssert.TOLERANCE);
        assertEquals(3, g.getCoordinateReferenceSystem().getCoordinateSystem().getDimension());
    }

    /**
     * Tests that a coordinate list mixing tuple widths is rejected. An Apache SIS coordinate array
     * is strictly rectangular, so such a list cannot be represented; padding it would either invent
     * ordinates or poison every later computation with NaN.
     */
    @Test
    public void testMixedTupleWidthRejected() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:LineString xmlns:gml=\"http://www.opengis.net/gml\">"
                + "<gml:coordinates>0.0,0.0 10.0,10.0,10.0</gml:coordinates>"
                + "</gml:LineString>";
        assertThrows(DataStoreContentException.class, () -> readInline(xml));
    }

    /**
     * Tests that an unresolvable {@code srsName} attribute is reported as a referencing error.
     */
    @Test
    public void testUnresolvableCRS() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:999999999\">"
                + "<gml:coordinates>10.0,20.0</gml:coordinates>"
                + "</gml:Point>";
        assertThrows(DataStoreReferencingException.class, () -> readInline(xml));
    }

    /**
     * Tests that an element outside the GML 2.0 {@code _Geometry} substitution group is rejected.
     */
    @Test
    public void testUnsupportedElement() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Curve xmlns:gml=\"http://www.opengis.net/gml\"/>";
        assertThrows(DataStoreContentException.class, () -> readInline(xml));
    }

    /**
     * Tests that a {@code <gml:Box>} element without exactly two coordinate tuples is rejected.
     */
    @Test
    public void testMalformedBox() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Box xmlns:gml=\"http://www.opengis.net/gml\">"
                + "<gml:coordinates>0.0,0.0</gml:coordinates>"
                + "</gml:Box>";
        assertThrows(DataStoreContentException.class, () -> readInline(xml));
    }
}
