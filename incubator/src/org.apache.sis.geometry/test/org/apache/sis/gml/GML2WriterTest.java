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
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.sis.geometries.BBox;
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
 * Tests the {@link GML2Writer} class.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GML2WriterTest extends TestCase {
    /**
     * The CRS written as {@code srsName="EPSG:4326"} in all fixture files.
     */
    private final CoordinateReferenceSystem wgs84;

    /**
     * Creates a new test case.
     */
    public GML2WriterTest() throws Exception {
        wgs84 = CRS.forCode("EPSG:4326");
    }

    /**
     * Writes the given geometry and returns the resulting XML document as a string.
     */
    private static String write(final Geometry geometry) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GML2Writer writer = new GML2Writer(out)) {
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
        assertXmlEquals(TestData.V2.openStream(TestData.POINT), write(g), "xmlns:*");
    }

    /**
     * Tests writing a {@code <gml:LineString>} element.
     */
    @Test
    public void testLineString() throws Exception {
        final Geometry g = GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0, 20.0, 0.0));
        assertXmlEquals(TestData.V2.openStream(TestData.LINE_STRING), write(g), "xmlns:*");
    }

    /**
     * Tests writing a standalone {@code <gml:LinearRing>} element.
     */
    @Test
    public void testLinearRing() throws Exception {
        assertXmlEquals(TestData.V2.openStream(TestData.LINEAR_RING), write(ring(0, 0, 10, 10)), "xmlns:*");
    }

    /**
     * Tests writing a {@code <gml:Polygon>} element with an {@code outerBoundaryIs} and one
     * {@code innerBoundaryIs} (hole).
     */
    @Test
    public void testPolygon() throws Exception {
        final Geometry g = GeometryFactory.createPolygon(ring(0, 0, 10, 10), List.of(ring(2, 2, 4, 4)));
        assertXmlEquals(TestData.V2.openStream(TestData.POLYGON), write(g), "xmlns:*");
    }

    /**
     * Tests writing a {@code <gml:MultiPoint>} element.
     */
    @Test
    public void testMultiPoint() throws Exception {
        final Geometry g = GeometryFactory.createMultiPoint(
                GeometryFactory.createPoint(sequence(0.0, 0.0)),
                GeometryFactory.createPoint(sequence(10.0, 10.0)));
        assertXmlEquals(TestData.V2.openStream(TestData.MULTI_POINT), write(g), "xmlns:*");
    }

    /**
     * Tests writing a {@code <gml:MultiLineString>} element.
     */
    @Test
    public void testMultiLineString() throws Exception {
        final Geometry g = GeometryFactory.createMultiLineString(
                GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0)),
                GeometryFactory.createLineString(sequence(20.0, 20.0, 30.0, 30.0, 40.0, 20.0)));
        assertXmlEquals(TestData.V2.openStream(TestData.MULTI_LINE_STRING), write(g), "xmlns:*");
    }

    /**
     * Tests writing a {@code <gml:MultiPolygon>} element.
     */
    @Test
    public void testMultiPolygon() throws Exception {
        final Geometry g = GeometryFactory.createMultiPolygon(
                GeometryFactory.createPolygon(ring(0, 0, 10, 10), null),
                GeometryFactory.createPolygon(ring(20, 20, 30, 30), null));
        assertXmlEquals(TestData.V2.openStream(TestData.MULTI_POLYGON), write(g), "xmlns:*");
    }

    /**
     * Tests writing a heterogeneous {@code <gml:MultiGeometry>} element.
     */
    @Test
    public void testMultiGeometry() throws Exception {
        final Geometry g = GeometryFactory.createGeometryCollection(
                GeometryFactory.createPoint(sequence(0.0, 0.0)),
                GeometryFactory.createLineString(sequence(10.0, 10.0, 20.0, 20.0)));
        assertXmlEquals(TestData.V2.openStream(TestData.MULTI_GEOMETRY), write(g), "xmlns:*");
    }

    /**
     * Tests writing a {@link BBox} as a {@code <gml:Box>} element.
     */
    @Test
    public void testBox() throws Exception {
        final BBox g = new BBox(wgs84, 0.0, 0.0, 10.0, 10.0);
        assertXmlEquals(TestData.V2.openStream(TestData.BOX), write(g), "xmlns:*");
    }

    /**
     * Tests the {@link GML2Writer#writeGeometry(Geometry, CoordinateReferenceSystem)} overload,
     * which uses an explicitly given CRS instead of the geometry's own.
     */
    @Test
    public void testExplicitCRS() throws Exception {
        final Point g = GeometryFactory.createPoint(
                GeometryFactory.createSequence(NDArrays.of(
                        SampleSystem.of(Geometries.getUndefinedCRS(2)), 10.0, 20.0)));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GML2Writer writer = new GML2Writer(out)) {
            writer.writeGeometry(g, wgs84);
        }
        assertXmlEquals(TestData.V2.openStream(TestData.POINT), out.toString(StandardCharsets.UTF_8), "xmlns:*");
    }

    /**
     * Tests that a geometry whose CRS is the placeholder substituted for a missing {@code srsName}
     * is written without any {@code srsName} attribute.
     */
    @Test
    public void testUndefinedCRSOmitsSrsName() throws Exception {
        final Point g = GeometryFactory.createPoint(
                GeometryFactory.createSequence(NDArrays.of(
                        SampleSystem.of(Geometries.getUndefinedCRS(2)), 10.0, 20.0)));
        assertFalse(write(g).contains(GML2Tags.SRS_NAME), "srsName should be omitted for an undefined CRS.");
    }

    /**
     * Tests that a three-dimensional geometry keeps all three ordinates in the
     * {@code gml:coordinates} text, since {@code gml:coordinates} places no width limit on a tuple.
     */
    @Test
    public void testThreeDimensionalCoordinates() throws Exception {
        final CoordinateReferenceSystem crs3D = CRS.forCode("EPSG:4979");
        final Geometry g = GeometryFactory.createLineString(GeometryFactory.createSequence(
                NDArrays.of(SampleSystem.of(crs3D), 0.0, 0.0, 1.0, 10.0, 10.0, 2.0)));
        final String xml = write(g);
        assertTrue(xml.contains("0.0,0.0,1.0 10.0,10.0,2.0"), () -> "Expected all three ordinates in: " + xml);
    }

}
