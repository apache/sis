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
import org.apache.sis.geometries.CompoundCurve;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurveInterpolation;
import org.apache.sis.geometries.CurvePolygon;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.LinearRing;
import org.apache.sis.geometries.MultiLineString;
import org.apache.sis.geometries.MultiPoint;
import org.apache.sis.geometries.MultiPolygon;
import org.apache.sis.geometries.MultiPolyhedron;
import org.apache.sis.geometries.Orientable;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.geometries.PolyhedralSurface;
import org.apache.sis.geometries.Polyhedron;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.TIN;
import org.apache.sis.geometries.curve.ArcByBulge;
import org.apache.sis.geometries.curve.ArcByCenterPoint;
import org.apache.sis.geometries.conics.CircularString;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.measure.Units;
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
 * Tests the {@link GML3Reader} class.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class GML3ReaderTest {
    /**
     * The CRS that all fixture files declare through {@code srsName="EPSG:4326"}.
     */
    private final CoordinateReferenceSystem wgs84;

    /**
     * Creates a new test case.
     */
    public GML3ReaderTest() throws Exception {
        wgs84 = CRS.forCode("EPSG:4326");
    }

    /**
     * Reads the geometry contained in the given test file, using {@link GML3Reader}.
     */
    private static Geometry read(final TestData data, final String filename) throws Exception {
        try (InputStream in = data.openStream(filename);
             GML3Reader reader = new GML3Reader(in))
        {
            return reader.readGeometry();
        }
    }

    /**
     * Reads the geometry contained in the given XML text, using {@link GML3Reader}.
     */
    private static Geometry readInline(final String xml) throws Exception {
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
             GML3Reader reader = new GML3Reader(in))
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
     * Tests reading a {@code <gml:Point>} element using {@code gml:pos}.
     */
    @Test
    public void testPoint() throws Exception {
        final Geometry g = read(TestData.V3, TestData.POINT);
        assertInstanceOf(Point.class, g);
        assertGeometryEquals(GeometryFactory.createPoint(sequence(10.0, 20.0)), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:LineString>} element using {@code gml:posList}.
     */
    @Test
    public void testLineStringPosList() throws Exception {
        final Geometry g = read(TestData.V3, TestData.LINE_STRING_POSLIST);
        assertInstanceOf(LineString.class, g);
        assertGeometryEquals(GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0, 20.0, 0.0)), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a standalone {@code <gml:LinearRing>} element.
     */
    @Test
    public void testLinearRing() throws Exception {
        final Geometry g = read(TestData.V3, TestData.LINEAR_RING);
        assertInstanceOf(LinearRing.class, g);
        assertGeometryEquals(ring(0, 0, 10, 10), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:Polygon>} element with {@code exterior}/{@code interior} boundaries.
     */
    @Test
    public void testPolygonExteriorInterior() throws Exception {
        final Geometry g = read(TestData.V3, TestData.POLYGON_EXTERIOR);
        assertInstanceOf(Polygon.class, g);
        assertGeometryEquals(GeometryFactory.createPolygon(ring(0, 0, 10, 10), List.of(ring(2, 2, 4, 4))), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:MultiPoint>} element using the per-member {@code pointMember} form.
     */
    @Test
    public void testMultiPoint() throws Exception {
        final Geometry g = read(TestData.V3, TestData.MULTI_POINT);
        assertInstanceOf(MultiPoint.class, g);
        assertGeometryEquals(expectedMultiPoint(), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:MultiPoint>} element using the compact {@code pointMembers} list form.
     */
    @Test
    public void testMultiPointCompactForm() throws Exception {
        final Geometry g = read(TestData.V3, TestData.MULTI_POINT_MEMBERS);
        assertInstanceOf(MultiPoint.class, g);
        assertGeometryEquals(expectedMultiPoint(), g);
        assertCRS(wgs84, g);
    }

    private MultiPoint<?> expectedMultiPoint() {
        return GeometryFactory.createMultiPoint(
                GeometryFactory.createPoint(sequence(0.0, 0.0)),
                GeometryFactory.createPoint(sequence(10.0, 10.0)));
    }

    /**
     * Tests reading a {@code <gml:MultiCurve>} element with linear members. Because every member is
     * a line string, the result is the more specific {@code MultiLineString} rather than a general
     * {@code MultiCurve}.
     */
    @Test
    public void testMultiCurve() throws Exception {
        final Geometry g = read(TestData.V3, TestData.MULTI_CURVE);
        assertInstanceOf(MultiLineString.class, g);
        assertGeometryEquals(GeometryFactory.createMultiLineString(
                GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0)),
                GeometryFactory.createLineString(sequence(20.0, 20.0, 30.0, 30.0, 40.0, 20.0))), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:MultiSurface>} element with planar members. Because every member
     * is a polygon, the result is the more specific {@code MultiPolygon} rather than a general
     * {@code MultiSurface}.
     */
    @Test
    public void testMultiSurface() throws Exception {
        final Geometry g = read(TestData.V3, TestData.MULTI_SURFACE);
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
        final Geometry g = read(TestData.V3, TestData.MULTI_GEOMETRY);
        assertInstanceOf(GeometryCollection.class, g);
        assertGeometryEquals(GeometryFactory.createGeometryCollection(
                GeometryFactory.createPoint(sequence(0.0, 0.0)),
                GeometryFactory.createLineString(sequence(10.0, 10.0, 20.0, 20.0))), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests reading a {@code <gml:Envelope>} element, the GML 3 replacement for {@code gml:Box}.
     * Like a box, it becomes a {@link BBox}.
     */
    @Test
    public void testEnvelope() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Envelope xmlns:gml=\"http://www.opengis.net/gml/3.2\" srsName=\"EPSG:4326\">"
                + "<gml:lowerCorner>0.0 0.0</gml:lowerCorner>"
                + "<gml:upperCorner>10.0 10.0</gml:upperCorner>"
                + "</gml:Envelope>";
        final BBox box = assertInstanceOf(BBox.class, readInline(xml));
        assertEquals(2, box.getDimension());
        assertEquals( 0.0, box.getMinimum(0), GeometryAssert.TOLERANCE);
        assertEquals(10.0, box.getMaximum(1), GeometryAssert.TOLERANCE);
        assertCRS(wgs84, box);
    }

    /**
     * Tests that the new GML 3 spelling ({@code posList}) is tolerated under the
     * unversioned (GML 2.0/3.0/3.1) namespace.
     */
    @Test
    public void testToleranceNewStyleUnderOldNamespace() throws Exception {
        final Geometry g = read(TestData.V3, TestData.TOLERANCE_NEW_STYLE_OLD_NAMESPACE);
        assertInstanceOf(LineString.class, g);
        assertGeometryEquals(GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0, 20.0, 0.0)), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests that the legacy GML 2.0 spelling ({@code coordinates}/{@code outerBoundaryIs}) is
     * tolerated under the versioned GML 3.2 namespace.
     */
    @Test
    public void testToleranceLegacyStyleUnderNewNamespace() throws Exception {
        final Geometry g = read(TestData.V3, TestData.TOLERANCE_LEGACY_STYLE_NEW_NAMESPACE);
        assertInstanceOf(Polygon.class, g);
        assertGeometryEquals(GeometryFactory.createPolygon(ring(0, 0, 10, 10), null), g);
        assertCRS(wgs84, g);
    }

    /**
     * Regression guard: a GML 2.0 {@code <gml:Box>} fixture, read by {@link GML3Reader}
     * instead of {@link GML2Reader}, must produce the exact same result — this is the
     * superset-parity property that the {@link GMLReader} facade's dispatch relies on.
     */
    @Test
    public void testBoxSupersetParity() throws Exception {
        final Geometry expected;
        try (InputStream in = TestData.V2.openStream(TestData.BOX); GML2Reader reader = new GML2Reader(in)) {
            expected = reader.readGeometry();
        }
        final Geometry actual = read(TestData.V2, TestData.BOX);
        assertGeometryEquals(expected, actual);
    }

    /**
     * Regression guard: a GML 2.0 {@code <gml:coord>}-encoded {@code <gml:Point>} fixture,
     * read by {@link GML3Reader} instead of {@link GML2Reader}, must produce the exact same result.
     */
    @Test
    public void testCoordSupersetParity() throws Exception {
        final Geometry expected;
        try (InputStream in = TestData.V2.openStream(TestData.POINT_COORD); GML2Reader reader = new GML2Reader(in)) {
            expected = reader.readGeometry();
        }
        final Geometry actual = read(TestData.V2, TestData.POINT_COORD);
        assertGeometryEquals(expected, actual);
    }

    /**
     * Tests that a {@code posList} whose token count is not a multiple of the declared CRS
     * dimension is read at the width that does divide it, rather than rejected. Real GML documents
     * do carry three-dimensional coordinates under a two-dimensional {@code srsName}, and the
     * {@code 3/solid.gml} fixture is one of them.
     */
    @Test
    public void testPosListDimensionInference() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:LineString xmlns:gml=\"http://www.opengis.net/gml/3.2\" srsName=\"EPSG:4326\">"
                + "<gml:posList>0.0 0.0 0.0 10.0 0.0 0.0 10.0 10.0 0.0</gml:posList>"
                + "</gml:LineString>";
        final LineString g = assertInstanceOf(LineString.class, readInline(xml));
        assertEquals(3, g.getPoints().getDimension(), "inferred tuple width");
        assertEquals(3, g.getPoints().size(), "number of points");
        assertEquals(3, g.getCoordinateReferenceSystem().getCoordinateSystem().getDimension(),
                     "The two-dimensional srsName should have been promoted to three dimensions.");
    }

    /**
     * Tests that an explicit {@code srsDimension} always wins over inference, and that a token
     * count contradicting it is reported rather than silently re-guessed.
     */
    @Test
    public void testSrsDimensionContradictionRejected() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:LineString xmlns:gml=\"http://www.opengis.net/gml/3.2\">"
                + "<gml:posList srsDimension=\"3\">0.0 0.0 10.0 10.0</gml:posList>"
                + "</gml:LineString>";
        assertThrows(DataStoreContentException.class, () -> readInline(xml));
    }

    /**
     * Tests that a {@code srsDimension} greater than three is accepted.
     */
    @Test
    public void testFourDimensionalPosList() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:LineString xmlns:gml=\"http://www.opengis.net/gml/3.2\">"
                + "<gml:posList srsDimension=\"4\">0 0 0 0 1 1 1 1</gml:posList>"
                + "</gml:LineString>";
        final LineString g = assertInstanceOf(LineString.class, readInline(xml));
        assertEquals(4, g.getPoints().getDimension());
        assertEquals(2, g.getPoints().size());
        assertUndefinedCRS(g);
    }

    /**
     * Tests that coordinates disagreeing with the declared {@code srsName} by more than one
     * dimension are reported, rather than silently truncated.
     */
    @Test
    public void testCRSDimensionMismatchRejected() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:LineString xmlns:gml=\"http://www.opengis.net/gml/3.2\" srsName=\"EPSG:4326\">"
                + "<gml:posList srsDimension=\"4\">0 0 0 0 1 1 1 1</gml:posList>"
                + "</gml:LineString>";
        assertThrows(DataStoreContentException.class, () -> readInline(xml));
    }

    /**
     * Tests that members declaring different coordinate reference systems are reported. An Apache
     * SIS collection reports the CRS of its first member, so accepting this document would produce
     * a geometry silently claiming to be entirely in EPSG:4326. Both codes used here name a
     * two-dimensional geographic CRS available from {@code CommonCRS}, so the failure can only come
     * from the CRS comparison and not from a dimension mismatch.
     */
    @Test
    public void testHeterogeneousMemberCRSRejected() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:MultiPoint xmlns:gml=\"http://www.opengis.net/gml/3.2\">"
                + "<gml:pointMember><gml:Point srsName=\"EPSG:4326\"><gml:pos>0.0 0.0</gml:pos></gml:Point></gml:pointMember>"
                + "<gml:pointMember><gml:Point srsName=\"EPSG:4322\"><gml:pos>1.0 1.0</gml:pos></gml:Point></gml:pointMember>"
                + "</gml:MultiPoint>";
        assertThrows(DataStoreContentException.class, () -> readInline(xml));
    }

    /**
     * Tests that a document declaring no {@code srsName} still yields a geometry with a coordinate
     * reference system, since an Apache SIS geometry cannot exist without one.
     */
    @Test
    public void testNoSrsNameUsesUndefinedCRS() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Point xmlns:gml=\"http://www.opengis.net/gml/3.2\">"
                + "<gml:pos>10.0 20.0</gml:pos>"
                + "</gml:Point>";
        final Geometry g = readInline(xml);
        assertInstanceOf(Point.class, g);
        assertUndefinedCRS(g);
    }

    /**
     * Tests that a {@code <gml:Curve>} whose only segment is a {@code gml:LineStringSegment}
     * collapses to a plain {@link LineString}: the {@code Curve}/{@code segments} wrapper adds
     * nothing when there is one linear segment.
     */
    @Test
    public void testCurve() throws Exception {
        final Geometry g = read(TestData.V3, TestData.CURVE);
        assertInstanceOf(LineString.class, g);
        assertGeometryEquals(GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0)), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests that a {@code <gml:Curve>} with a {@code gml:Arc} segment becomes a
     * {@link CircularString}, preserving the circular interpolation instead of pretending the
     * three control points describe a polyline.
     */
    @Test
    public void testCurveArc() throws Exception {
        final Geometry g = read(TestData.V3, TestData.CURVE_ARC);
        final CircularString arc = assertInstanceOf(CircularString.class, g);
        assertEquals(1, arc.getNumArcs());
        assertEquals(3, arc.getPoints().size());
        assertEquals(CurveInterpolation.CIRCULAR, arc.getInterpolation());
        assertCRS(wgs84, g);
    }

    /**
     * Tests that a {@code <gml:ArcByCenterPoint>} segment becomes an {@link ArcByCenterPoint},
     * keeping the centre, radius and bearings the document actually stated rather than being
     * evaluated into points on the arc.
     */
    @Test
    public void testArcByCenterPoint() throws Exception {
        final Geometry g = read(TestData.V3, TestData.CURVE_ARC_BY_CENTER);
        final ArcByCenterPoint arc = assertInstanceOf(ArcByCenterPoint.class, g);
        assertEquals(10.0, arc.getCenter().getPosition().get(0), GeometryAssert.TOLERANCE, "centre x");
        assertEquals(20.0, arc.getCenter().getPosition().get(1), GeometryAssert.TOLERANCE, "centre y");
        assertEquals( 5.0, arc.getRadius(),     GeometryAssert.TOLERANCE, "radius");
        assertEquals( 0.0, arc.getStartAngle(), GeometryAssert.TOLERANCE, "start angle");
        assertEquals(90.0, arc.getEndAngle(),   GeometryAssert.TOLERANCE, "end angle");
        assertEquals(Units.METRE, arc.getRadiusUnit(), "radius unit");
        assertEquals(CurveInterpolation.CIRCULAR, arc.getInterpolation());
        assertCRS(wgs84, g);
    }

    /**
     * Tests that the centre of an arc may be given as a {@code gml:pointProperty} instead of a
     * {@code gml:pos}, and that a radius declaring no {@code uom} is reported with no unit —
     * meaning the units of the coordinate system axes — rather than with an invented one.
     */
    @Test
    public void testArcByCenterPointProperty() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Curve xmlns:gml=\"http://www.opengis.net/gml/3.2\" srsName=\"EPSG:4326\"><gml:segments>"
                + "<gml:ArcByCenterPoint numArc=\"1\">"
                + "<gml:pointProperty><gml:Point><gml:pos>10.0 20.0</gml:pos></gml:Point></gml:pointProperty>"
                + "<gml:radius>5.0</gml:radius>"
                + "<gml:startAngle>0.0</gml:startAngle>"
                + "<gml:endAngle>90.0</gml:endAngle>"
                + "</gml:ArcByCenterPoint>"
                + "</gml:segments></gml:Curve>";
        final ArcByCenterPoint arc = assertInstanceOf(ArcByCenterPoint.class, readInline(xml));
        assertEquals(10.0, arc.getCenter().getPosition().get(0), GeometryAssert.TOLERANCE, "centre x");
        assertEquals( 5.0, arc.getRadius(), GeometryAssert.TOLERANCE, "radius");
        assertNull(arc.getRadiusUnit(), "A radius with no uom must not be given an invented unit.");
    }

    /**
     * Tests that angles declared in a unit other than degrees are converted, since
     * {@link ArcByCenterPoint} reports them in decimal degrees.
     */
    @Test
    public void testArcByCenterPointAngleUnits() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Curve xmlns:gml=\"http://www.opengis.net/gml/3.2\"><gml:segments>"
                + "<gml:ArcByCenterPoint>"
                + "<gml:pos>0 0</gml:pos>"
                + "<gml:radius uom=\"m\">5</gml:radius>"
                + "<gml:startAngle uom=\"rad\">0</gml:startAngle>"
                + "<gml:endAngle uom=\"rad\">" + (Math.PI / 2) + "</gml:endAngle>"
                + "</gml:ArcByCenterPoint>"
                + "</gml:segments></gml:Curve>";
        final ArcByCenterPoint arc = assertInstanceOf(ArcByCenterPoint.class, readInline(xml));
        assertEquals( 0.0, arc.getStartAngle(), GeometryAssert.TOLERANCE, "start angle in degrees");
        assertEquals(90.0, arc.getEndAngle(),   GeometryAssert.TOLERANCE, "end angle in degrees");
    }

    /**
     * Tests that an arc missing its radius is reported, rather than read as an arc of some
     * default radius.
     */
    @Test
    public void testArcByCenterPointWithoutRadiusRejected() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Curve xmlns:gml=\"http://www.opengis.net/gml/3.2\"><gml:segments>"
                + "<gml:ArcByCenterPoint>"
                + "<gml:pos>0 0</gml:pos>"
                + "<gml:startAngle>0</gml:startAngle>"
                + "<gml:endAngle>90</gml:endAngle>"
                + "</gml:ArcByCenterPoint>"
                + "</gml:segments></gml:Curve>";
        assertThrows(DataStoreContentException.class, () -> readInline(xml));
    }

    /**
     * Tests that an arc missing its angles is reported. The element meaning <q>the whole
     * circle</q> is {@code gml:CircleByCenterPoint}, not an angle-less {@code gml:ArcByCenterPoint}.
     */
    @Test
    public void testArcByCenterPointWithoutAnglesRejected() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Curve xmlns:gml=\"http://www.opengis.net/gml/3.2\"><gml:segments>"
                + "<gml:ArcByCenterPoint>"
                + "<gml:pos>0 0</gml:pos>"
                + "<gml:radius uom=\"m\">5</gml:radius>"
                + "</gml:ArcByCenterPoint>"
                + "</gml:segments></gml:Curve>";
        assertThrows(DataStoreContentException.class, () -> readInline(xml));
    }

    /**
     * Tests that a {@code <gml:ArcByBulge>} segment becomes an {@link ArcByBulge}, keeping its two
     * end points, its bulge and its normal.
     */
    @Test
    public void testArcByBulge() throws Exception {
        final Geometry g = read(TestData.V3, TestData.CURVE_ARC_BY_BULGE);
        final ArcByBulge arc = assertInstanceOf(ArcByBulge.class, g);
        assertEquals(2, arc.getPoints().size(), "number of points");
        assertEquals( 0.0, arc.getPoints().getPosition(0).get(0), GeometryAssert.TOLERANCE, "start x");
        assertEquals(10.0, arc.getPoints().getPosition(1).get(0), GeometryAssert.TOLERANCE, "end x");
        assertEquals( 2.0, arc.getBulge(), GeometryAssert.TOLERANCE, "bulge");
        assertEquals(2, arc.getNormal().getDimension(), "normal dimension");
        assertEquals(0.0, arc.getNormal().get(0), GeometryAssert.TOLERANCE, "normal x");
        assertEquals(1.0, arc.getNormal().get(1), GeometryAssert.TOLERANCE, "normal y");
        assertEquals(CurveInterpolation.CIRCULAR, arc.getInterpolation());
        assertCRS(wgs84, g);
    }

    /**
     * Tests that an arc by bulge with no {@code gml:normal} is reported. Without the normal, the
     * two arcs joining the end points cannot be told apart, and picking one would be a guess.
     */
    @Test
    public void testArcByBulgeWithoutNormalRejected() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Curve xmlns:gml=\"http://www.opengis.net/gml/3.2\"><gml:segments>"
                + "<gml:ArcByBulge>"
                + "<gml:posList srsDimension=\"2\">0 0 10 0</gml:posList>"
                + "<gml:bulge>2.0</gml:bulge>"
                + "</gml:ArcByBulge>"
                + "</gml:segments></gml:Curve>";
        assertThrows(DataStoreContentException.class, () -> readInline(xml));
    }

    /**
     * Tests that an arc by bulge with more than two coordinate tuples is reported. Three or more
     * points with one bulge each is {@code gml:ArcStringByBulge}, a different element.
     */
    @Test
    public void testArcByBulgeWithTooManyPointsRejected() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Curve xmlns:gml=\"http://www.opengis.net/gml/3.2\"><gml:segments>"
                + "<gml:ArcByBulge>"
                + "<gml:posList srsDimension=\"2\">0 0 10 0 20 0</gml:posList>"
                + "<gml:bulge>2.0</gml:bulge>"
                + "<gml:normal>0 1</gml:normal>"
                + "</gml:ArcByBulge>"
                + "</gml:segments></gml:Curve>";
        assertThrows(DataStoreContentException.class, () -> readInline(xml));
    }

    /**
     * Tests that a {@code <gml:Surface>} with a single {@code gml:PolygonPatch} collapses to a
     * plain {@link Polygon}.
     */
    @Test
    public void testSurface() throws Exception {
        final Geometry g = read(TestData.V3, TestData.SURFACE);
        assertInstanceOf(Polygon.class, g);
        assertGeometryEquals(GeometryFactory.createPolygon(ring(0, 0, 10, 10), null), g);
        assertCRS(wgs84, g);
    }

    /**
     * Tests that a {@code <gml:Surface>} with several patches becomes a {@link PolyhedralSurface},
     * which -- unlike a {@code MultiSurface} -- carries the guarantee that the patches are contiguous.
     */
    @Test
    public void testSurfaceWithSeveralPatches() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Surface xmlns:gml=\"http://www.opengis.net/gml/3.2\" srsName=\"EPSG:4326\"><gml:patches>"
                + "<gml:PolygonPatch><gml:exterior><gml:LinearRing>"
                + "<gml:posList srsDimension=\"2\">0 0 10 0 10 10 0 10 0 0</gml:posList>"
                + "</gml:LinearRing></gml:exterior></gml:PolygonPatch>"
                + "<gml:PolygonPatch><gml:exterior><gml:LinearRing>"
                + "<gml:posList srsDimension=\"2\">10 0 20 0 20 10 10 10 10 0</gml:posList>"
                + "</gml:LinearRing></gml:exterior></gml:PolygonPatch>"
                + "</gml:patches></gml:Surface>";
        final PolyhedralSurface<?> s = assertInstanceOf(PolyhedralSurface.class, readInline(xml));
        assertEquals(2, s.getNumPatches());
        assertCRS(wgs84, s);
    }

    /**
     * Tests that a {@code <gml:Surface>} whose patches are all {@code gml:Triangle} becomes a
     * {@link TIN}, the most specific type that fits.
     */
    @Test
    public void testSurfaceOfTriangles() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Surface xmlns:gml=\"http://www.opengis.net/gml/3.2\" srsName=\"EPSG:4326\"><gml:patches>"
                + "<gml:Triangle><gml:exterior><gml:LinearRing>"
                + "<gml:posList srsDimension=\"2\">0 0 10 0 0 10 0 0</gml:posList>"
                + "</gml:LinearRing></gml:exterior></gml:Triangle>"
                + "<gml:Triangle><gml:exterior><gml:LinearRing>"
                + "<gml:posList srsDimension=\"2\">10 0 10 10 0 10 10 0</gml:posList>"
                + "</gml:LinearRing></gml:exterior></gml:Triangle>"
                + "</gml:patches></gml:Surface>";
        final TIN tin = assertInstanceOf(TIN.class, readInline(xml));
        assertEquals(2, tin.getNumPatches());
        assertCRS(wgs84, tin);
    }

    /**
     * Tests that a {@code <gml:Ring>} becomes a {@link CompoundCurve}: a ring of arbitrary curve
     * members is a single connected curve, not a collection.
     */
    @Test
    public void testRing() throws Exception {
        final Geometry g = read(TestData.V3, TestData.RING);
        final CompoundCurve ring = assertInstanceOf(CompoundCurve.class, g);
        assertEquals(1, ring.getNumCurves());
        assertInstanceOf(LineString.class, ring.getCurveN(0));
        assertCRS(wgs84, g);
    }

    /**
     * Tests that a {@code <gml:Polygon>} bounded by a non-linear {@code gml:Ring} becomes a
     * {@link CurvePolygon} rather than a {@link Polygon}, whose rings are linear by definition.
     */
    @Test
    public void testPolygonWithRingBoundary() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Polygon xmlns:gml=\"http://www.opengis.net/gml/3.2\" srsName=\"EPSG:4326\">"
                + "<gml:exterior><gml:Ring><gml:curveMember><gml:Curve><gml:segments>"
                + "<gml:ArcString><gml:posList srsDimension=\"2\">0 0 5 5 10 0 5 -5 0 0</gml:posList></gml:ArcString>"
                + "</gml:segments></gml:Curve></gml:curveMember></gml:Ring></gml:exterior>"
                + "</gml:Polygon>";
        final CurvePolygon p = assertInstanceOf(CurvePolygon.class, readInline(xml));
        /*
         * The gml:Ring wraps its single curveMember in a CompoundCurve rather than unwrapping it:
         * a ring with one non-linear member is still a ring, and collapsing it would lose that.
         */
        final CompoundCurve boundary = assertInstanceOf(CompoundCurve.class, p.getExteriorRing());
        assertEquals(1, boundary.getNumCurves());
        assertInstanceOf(CircularString.class, boundary.getCurveN(0));
        assertTrue(p.getInteriorRings().isEmpty());
        assertCRS(wgs84, p);
    }

    /**
     * Tests that {@code <gml:CompositeCurve>} becomes a {@link CompoundCurve}.
     */
    @Test
    public void testCompositeCurve() throws Exception {
        final Geometry g = read(TestData.V3, TestData.COMPOSITE_CURVE);
        final CompoundCurve c = assertInstanceOf(CompoundCurve.class, g);
        assertEquals(1, c.getNumCurves());
        assertGeometryEquals(GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0)), c.getCurveN(0));
        assertCRS(wgs84, g);
    }

    /**
     * Tests that {@code <gml:CompositeSurface>} becomes a {@link PolyhedralSurface}, not a
     * {@code MultiSurface}: a composite surface's members are contiguous and together form one
     * surface, which is exactly {@code PolyhedralSurface}'s contract and precisely what
     * {@code MultiSurface} does not promise.
     */
    @Test
    public void testCompositeSurface() throws Exception {
        final Geometry g = read(TestData.V3, TestData.COMPOSITE_SURFACE);
        final PolyhedralSurface<?> s = assertInstanceOf(PolyhedralSurface.class, g);
        assertEquals(1, s.getNumPatches());
        assertGeometryEquals(GeometryFactory.createPolygon(ring(0, 0, 10, 10), null), s.getPatchN(0));
        assertCRS(wgs84, g);
    }

    /**
     * Tests that {@code <gml:OrientableCurve orientation="-">} keeps its reversed orientation
     * visible, rather than being flattened to its base curve.
     */
    @Test
    public void testOrientableCurve() throws Exception {
        final Geometry g = read(TestData.V3, TestData.ORIENTABLE_CURVE);
        final Curve c = assertInstanceOf(Curve.class, g);
        assertEquals(Orientable.Sign.NEGATIVE, c.getOrientationSign());
        final Geometry base = assertInstanceOf(LineString.class, c.getPrimitive());
        assertGeometryEquals(GeometryFactory.createLineString(sequence(0.0, 0.0, 10.0, 10.0)), base);
        assertCRS(wgs84, g);
    }

    /**
     * Tests that {@code orientation="+"} -- the schema default -- yields the base curve itself,
     * since a positively oriented curve is indistinguishable from the curve.
     */
    @Test
    public void testOrientableCurvePositive() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:OrientableCurve xmlns:gml=\"http://www.opengis.net/gml/3.2\" srsName=\"EPSG:4326\" orientation=\"+\">"
                + "<gml:baseCurve><gml:LineString><gml:posList srsDimension=\"2\">0 0 10 10</gml:posList></gml:LineString></gml:baseCurve>"
                + "</gml:OrientableCurve>";
        final Geometry g = readInline(xml);
        assertInstanceOf(LineString.class, g);
        assertEquals(Orientable.Sign.POSITIVE, ((Curve) g).getOrientationSign());
    }

    /**
     * Tests that {@code <gml:OrientableSurface orientation="-">} keeps its reversed orientation.
     */
    @Test
    public void testOrientableSurface() throws Exception {
        final Geometry g = read(TestData.V3, TestData.ORIENTABLE_SURFACE);
        final Surface s = assertInstanceOf(Surface.class, g);
        assertEquals(Orientable.Sign.NEGATIVE, s.getOrientationSign());
        assertInstanceOf(Polygon.class, s.getPrimitive());
        assertCRS(wgs84, g);
    }

    /**
     * Tests that {@code <gml:Solid>} becomes a {@link Polyhedron}, whose exterior and interior
     * shells map one for one onto GML's. The more abstract {@code Solid} interface is not used:
     * it describes a solid through interpolation and knots, and has no notion of a shell.
     */
    @Test
    public void testSolid() throws Exception {
        final Geometry g = read(TestData.V3, TestData.SOLID);
        final Polyhedron solid = assertInstanceOf(Polyhedron.class, g);
        assertEquals(1, solid.getExteriorShell().getNumGeometries());
        assertTrue(solid.getInteriorShells().isEmpty());
        assertEquals(3, solid.getCoordinateReferenceSystem().getCoordinateSystem().getDimension());
        assertEquals(3, solid.getTopologicDimension());
    }

    /**
     * Tests that {@code <gml:CompositeSolid>} becomes a {@link MultiPolyhedron}, including the
     * empty {@code <gml:Shell/>} that its fixture contains -- which used to make every Apache SIS
     * aggregate throw when asked for its coordinate reference system.
     */
    @Test
    public void testCompositeSolid() throws Exception {
        final Geometry g = read(TestData.V3, TestData.COMPOSITE_SOLID);
        final MultiPolyhedron solids = assertInstanceOf(MultiPolyhedron.class, g);
        assertEquals(1, solids.getNumGeometries());
        assertEquals(0, solids.getGeometryN(0).getExteriorShell().getNumGeometries(), "the empty Shell");
        assertNotNull(solids.getCoordinateReferenceSystem());
    }

    /*
     * Curve and surface kinds whose Apache SIS interface exists but has no implementation, and
     * whose parameterisation is a separate design question. They are reported as deferred, never
     * approximated by a linear substitute.
     */

    /**
     * Tests that a centre-point circle is reported as not implemented rather than approximated.
     */
    @Test
    public void testCircleByCenterPointDeferred() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Curve xmlns:gml=\"http://www.opengis.net/gml/3.2\"><gml:segments>"
                + "<gml:CircleByCenterPoint numArc=\"1\"><gml:pos>0 0</gml:pos><gml:radius>5</gml:radius></gml:CircleByCenterPoint>"
                + "</gml:segments></gml:Curve>";
        final DataStoreContentException e = assertThrows(DataStoreContentException.class, () -> readInline(xml));
        assertTrue(e.getMessage().contains("not implemented"), () -> "Unexpected message: " + e.getMessage());
    }

    /**
     * Tests that a cubic spline segment is reported as not implemented.
     */
    @Test
    public void testCubicSplineDeferred() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Curve xmlns:gml=\"http://www.opengis.net/gml/3.2\"><gml:segments>"
                + "<gml:CubicSpline><gml:posList srsDimension=\"2\">0 0 10 10</gml:posList></gml:CubicSpline>"
                + "</gml:segments></gml:Curve>";
        final DataStoreContentException e = assertThrows(DataStoreContentException.class, () -> readInline(xml));
        assertTrue(e.getMessage().contains("not implemented"), () -> "Unexpected message: " + e.getMessage());
    }

    /**
     * Tests that an unresolvable {@code srsName} attribute is reported as a referencing error.
     */
    @Test
    public void testUnresolvableCRS() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<gml:Point xmlns:gml=\"http://www.opengis.net/gml/3.2\" srsName=\"EPSG:999999999\">"
                + "<gml:pos>10.0 20.0</gml:pos>"
                + "</gml:Point>";
        assertThrows(DataStoreReferencingException.class, () -> readInline(xml));
    }
}
