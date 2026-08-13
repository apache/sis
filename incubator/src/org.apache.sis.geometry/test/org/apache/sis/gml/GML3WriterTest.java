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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.referencing.CRS;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Tests the {@link GML3Writer} class.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GML3WriterTest extends TestCase {
    /**
     * The CRS written as {@code srsName="EPSG:4326"} in all fixture files.
     */
    private final CoordinateReferenceSystem wgs84;

    /**
     * Creates a new test case.
     */
    public GML3WriterTest() throws Exception {
        wgs84 = CRS.forCode("EPSG:4326");
    }

    /**
     * Writes the given geometry and returns the resulting XML document as a string.
     */
    private static String write(final Geometry geometry) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GML3Writer writer = new GML3Writer(out)) {
            writer.writeGeometry(geometry);
        }
        return out.toString(StandardCharsets.UTF_8);
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
     * Tests writing a {@code <gml:Point>} element.
     */
    @Test
    public void testPoint() throws Exception {
        final Point g = GeometryFactory.createPoint(sequence(10.0, 20.0));
        assertXmlEquals(TestData.V3.openStream(TestData.POINT), write(g), "xmlns:*");
    }

    /**
     * Tests writing a {@code <gml:LineString>} element.
     */
    @Test
    public void testLineString() throws Exception {
        final Geometry g = GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0, 20.0, 0.0));
        assertXmlEquals(TestData.V3.openStream(TestData.LINE_STRING_POSLIST), write(g), "xmlns:*");
    }

    /**
     * Tests writing a standalone {@code <gml:LinearRing>} element.
     */
    @Test
    public void testLinearRing() throws Exception {
        assertXmlEquals(TestData.V3.openStream(TestData.LINEAR_RING), write(ring(0, 0, 10, 10)), "xmlns:*");
    }

    /**
     * Tests writing a {@code <gml:Polygon>} element with an {@code exterior} and one {@code interior} (hole).
     */
    @Test
    public void testPolygon() throws Exception {
        final Geometry g = GeometryFactory.createPolygon(ring(0, 0, 10, 10), List.of(ring(2, 2, 4, 4)));
        assertXmlEquals(TestData.V3.openStream(TestData.POLYGON_EXTERIOR), write(g), "xmlns:*");
    }

    /**
     * Tests writing a {@code <gml:MultiPoint>} element.
     */
    @Test
    public void testMultiPoint() throws Exception {
        final Geometry g = GeometryFactory.createMultiPoint(
                GeometryFactory.createPoint(sequence(0.0, 0.0)),
                GeometryFactory.createPoint(sequence(10.0, 10.0)));
        assertXmlEquals(TestData.V3.openStream(TestData.MULTI_POINT), write(g), "xmlns:*");
    }

    /**
     * Tests writing a {@link org.apache.sis.geometries.MultiLineString} as a
     * {@code <gml:MultiCurve>} element (the non-deprecated GML 3.2 spelling).
     */
    @Test
    public void testMultiCurve() throws Exception {
        final Geometry g = GeometryFactory.createMultiLineString(
                GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0)),
                GeometryFactory.createLineString(sequence(20.0, 20.0, 30.0, 30.0, 40.0, 20.0)));
        assertXmlEquals(TestData.V3.openStream(TestData.MULTI_CURVE), write(g), "xmlns:*");
    }

    /**
     * Tests writing a {@link org.apache.sis.geometries.MultiPolygon} as a
     * {@code <gml:MultiSurface>} element (the non-deprecated GML 3.2 spelling).
     */
    @Test
    public void testMultiSurface() throws Exception {
        final Geometry g = GeometryFactory.createMultiPolygon(
                GeometryFactory.createPolygon(ring(0, 0, 10, 10), null),
                GeometryFactory.createPolygon(ring(20, 20, 30, 30), null));
        assertXmlEquals(TestData.V3.openStream(TestData.MULTI_SURFACE), write(g), "xmlns:*");
    }

    /**
     * Tests writing a heterogeneous {@code <gml:MultiGeometry>} element.
     */
    @Test
    public void testMultiGeometry() throws Exception {
        final Geometry g = GeometryFactory.createGeometryCollection(
                GeometryFactory.createPoint(sequence(0.0, 0.0)),
                GeometryFactory.createLineString(sequence(10.0, 10.0, 20.0, 20.0)));
        assertXmlEquals(TestData.V3.openStream(TestData.MULTI_GEOMETRY), write(g), "xmlns:*");
    }

    /**
     * Tests writing a {@link BBox} as a {@code <gml:Envelope>} element — the GML 3 spelling of a
     * {@code gml:Box}.
     */
    @Test
    public void testEnvelope() throws Exception {
        final BBox g = new BBox(wgs84, 0.0, 0.0, 10.0, 10.0);
        assertXmlEquals(TestData.V3.openStream(TestData.ENVELOPE), write(g), "xmlns:*");
    }

    /**
     * Tests the {@link GML3Writer#writeGeometry(Geometry, CoordinateReferenceSystem)} overload,
     * which uses an explicitly given CRS instead of the geometry's own.
     */
    @Test
    public void testExplicitCRS() throws Exception {
        // Built with the placeholder CRS, so that writing it without an explicit CRS emits no srsName.
        final Point g = GeometryFactory.createPoint(
                GeometryFactory.createSequence(NDArrays.of(
                        SampleSystem.of(Geometries.getUndefinedCRS(2)), 10.0, 20.0)));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GML3Writer writer = new GML3Writer(out)) {
            writer.writeGeometry(g, wgs84);
        }
        assertXmlEquals(TestData.V3.openStream(TestData.POINT), out.toString(StandardCharsets.UTF_8), "xmlns:*");
    }

    /**
     * Tests that a geometry whose CRS is the placeholder substituted for a missing {@code srsName}
     * is written without any {@code srsName} attribute, rather than with an invented one.
     */
    @Test
    public void testUndefinedCRSOmitsSrsName() throws Exception {
        final Point g = GeometryFactory.createPoint(
                GeometryFactory.createSequence(NDArrays.of(
                        SampleSystem.of(Geometries.getUndefinedCRS(2)), 10.0, 20.0)));
        assertFalse(write(g).contains(GML2Tags.SRS_NAME), "srsName should be omitted for an undefined CRS.");
    }

    /**
     * Tests that a three-dimensional geometry is written with {@code srsDimension="3"} and all
     * three ordinates.
     */
    @Test
    public void testThreeDimensionalPosList() throws Exception {
        final CoordinateReferenceSystem crs3D = CRS.forCode("EPSG:4979");
        final Geometry g = GeometryFactory.createLineString(GeometryFactory.createSequence(
                NDArrays.of(SampleSystem.of(crs3D), 0.0, 0.0, 1.0, 10.0, 10.0, 2.0)));
        final String xml = write(g);
        assertTrue(xml.contains("srsDimension=\"3\""), () -> "Expected srsDimension=\"3\" in: " + xml);
        assertTrue(xml.contains("0.0 0.0 1.0 10.0 10.0 2.0"), () -> "Expected all three ordinates in: " + xml);
    }

    /**
     * Reads the given GML 3 fixture and writes it back, asserting that the result is the fixture
     * itself.
     */
    private static void assertRoundTrip(final String filename) throws Exception {
        final Geometry g;
        try (InputStream in = TestData.V3.openStream(filename); GML3Reader reader = new GML3Reader(in)) {
            g = reader.readGeometry();
        }
        assertXmlEquals(TestData.V3.openStream(filename), write(g), "xmlns:*");
    }

    /**
     * Reads the given GML 3 fixture, writes it back, and asserts that the root element is the given
     * one — used where the mapping onto an Apache SIS type is deliberately many-to-one, so the
     * original spelling cannot be recovered.
     */
    private static void assertWrittenAs(final String filename, final String expectedRootElement) throws Exception {
        final Geometry g;
        try (InputStream in = TestData.V3.openStream(filename); GML3Reader reader = new GML3Reader(in)) {
            g = reader.readGeometry();
        }
        final String xml = write(g);
        assertTrue(xml.contains("<gml:" + expectedRootElement) || xml.contains('<' + expectedRootElement),
                () -> "Expected a " + expectedRootElement + " root element in: " + xml);
    }

    /**
     * Tests that a {@code gml:Curve} holding one {@code gml:Arc} survives a full read/write cycle,
     * arc and all.
     */
    @Test
    public void testCurveArcRoundTrip() throws Exception {
        assertRoundTrip(TestData.CURVE_ARC);
    }

    /**
     * Tests that {@code gml:CompositeCurve} survives a full read/write cycle.
     */
    @Test
    public void testCompositeCurveRoundTrip() throws Exception {
        assertRoundTrip(TestData.COMPOSITE_CURVE);
    }

    /**
     * Tests that {@code gml:OrientableCurve} keeps its {@code orientation="-"} through a full cycle.
     */
    @Test
    public void testOrientableCurveRoundTrip() throws Exception {
        assertRoundTrip(TestData.ORIENTABLE_CURVE);
    }

    /**
     * Tests that {@code gml:OrientableSurface} keeps its {@code orientation="-"} through a full cycle.
     */
    @Test
    public void testOrientableSurfaceRoundTrip() throws Exception {
        assertRoundTrip(TestData.ORIENTABLE_SURFACE);
    }

    /**
     * Tests that {@code gml:Solid}, with its shells, survives a full read/write cycle.
     */
    @Test
    public void testSolidRoundTrip() throws Exception {
        assertRoundTrip(TestData.SOLID);
    }

    /**
     * Tests that {@code gml:CompositeSolid} survives a full read/write cycle, including the empty
     * {@code <gml:Shell/>} it contains.
     */
    @Test
    public void testCompositeSolidRoundTrip() throws Exception {
        assertRoundTrip(TestData.COMPOSITE_SOLID);
    }

    /*
     * The four cases below do NOT round-trip, and the assertions state what is emitted instead.
     * Each is a deliberate many-to-one mapping: the Apache SIS type keeps the geometry exactly and
     * loses only which of several equivalent GML spellings the document happened to use.
     */

    /**
     * A {@code gml:Curve} wrapping a single {@code gml:LineStringSegment} is read as a plain line
     * string, so it comes back as {@code gml:LineString}. The geometry is identical; the
     * {@code Curve}/{@code segments} wrapper is what is lost.
     */
    @Test
    public void testCurveWrittenAsLineString() throws Exception {
        assertWrittenAs(TestData.CURVE, "LineString");
    }

    /**
     * A {@code gml:Surface} with one {@code gml:PolygonPatch} is read as a plain polygon,
     * so it comes back as {@code gml:Polygon}.
     */
    @Test
    public void testSurfaceWrittenAsPolygon() throws Exception {
        assertWrittenAs(TestData.SURFACE, "Polygon");
    }

    /**
     * A {@code gml:Ring} becomes a compound curve, which is written as {@code gml:CompositeCurve}
     * when it stands alone. (It is written back as {@code gml:Ring} when it bounds a surface,
     * where the schema requires a ring.)
     */
    @Test
    public void testRingWrittenAsCompositeCurve() throws Exception {
        assertWrittenAs(TestData.RING, "CompositeCurve");
    }

    /**
     * A {@code gml:CompositeSurface} becomes a polyhedral surface, which is written as
     * {@code gml:Surface} with {@code gml:patches} — the same set of contiguous polygonal faces,
     * expressed the other legal way.
     */
    @Test
    public void testCompositeSurfaceWrittenAsSurface() throws Exception {
        assertWrittenAs(TestData.COMPOSITE_SURFACE, "Surface");
    }

    /**
     * Tests that a curve-bounded surface is written as a {@code gml:Polygon} whose boundary is a
     * {@code gml:Ring}, not a {@code gml:LinearRing}.
     */
    @Test
    public void testCurvePolygon() throws Exception {
        final Geometry arc = GeometryFactory.createCircularString(sequence(0, 0, 5, 5, 10, 0, 5, -5, 0, 0));
        final Geometry g = GeometryFactory.createCurvePolygon((Curve) arc, null);
        final String xml = write(g);
        assertTrue(xml.contains("Ring"), () -> "Expected a gml:Ring boundary in: " + xml);
        assertTrue(xml.contains("ArcString"), () -> "Expected the arc to be preserved in: " + xml);
        assertFalse(xml.contains("LinearRing"), () -> "A curved boundary must not be written as a LinearRing: " + xml);
    }

    /**
     * Tests that a polyhedral surface of several patches is written as {@code gml:Surface} with one
     * {@code gml:PolygonPatch} per patch.
     */
    @Test
    public void testPolyhedralSurface() throws Exception {
        final Geometry g = GeometryFactory.createPolyhedralSurface(
                GeometryFactory.createPolygon(ring(0, 0, 10, 10), null),
                GeometryFactory.createPolygon(ring(10, 0, 20, 10), null));
        final String xml = write(g);
        assertTrue(xml.contains("patches"), () -> "Expected gml:patches in: " + xml);
        assertEquals(2, countOccurrences(xml, "<PolygonPatch"), () -> "Expected two patches in: " + xml);
    }

    /**
     * Returns how many times the given token appears in the given text.
     */
    private static int countOccurrences(final String text, final String token) {
        int count = 0;
        for (int i = text.indexOf(token); i >= 0; i = text.indexOf(token, i + token.length())) {
            count++;
        }
        return count;
    }
}
