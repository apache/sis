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
package org.apache.sis.geometries.adapter;

import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Empty;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.CircularString;
import org.apache.sis.geometries.curve.CompoundCurve;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.MultiCurve;
import org.apache.sis.geometries.curve.MultiLineString;
import org.apache.sis.geometries.point.MultiPoint;
import org.apache.sis.geometries.surface.CurvePolygon;
import org.apache.sis.geometries.surface.MultiPolygon;
import org.apache.sis.geometries.surface.MultiSurface;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.geometries.surface.TIN;
import org.apache.sis.geometries.surface.Triangle;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link WellKnownText}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class WellKnownTextTest {
    /**
     * The codec under test, writing the shortest representation of every ordinate.
     */
    private final WellKnownText wkt = new WellKnownText();

    /**
     * Creates a new test case.
     */
    public WellKnownTextTest() {
    }

    /**
     * Verifies that the given text decodes to a geometry of the expected type, and that encoding
     * that geometry gives the text back. Every text given here is in the canonical form the
     * encoder writes, so the round trip is an equality and not merely an equivalence.
     */
    private void assertRoundTrip(final Class<? extends Geometry> type, final String text) {
        final Geometry geometry = wkt.decode(text);
        assertInstanceOf(type, geometry, text);
        assertEquals(text, wkt.encode(geometry), "Round trip of " + text);
        assertEquals(text, geometry.asText(), "Geometry.asText() of " + text);
    }

    /**
     * Tests the two dimensional form of every supported geometry type.
     */
    @Test
    public void testRoundTrip2D() {
        assertRoundTrip(Point.class,              "POINT (1 2)");
        assertRoundTrip(LineString.class,         "LINESTRING (0 0, 1 1, 2 0)");
        assertRoundTrip(CircularString.class,     "CIRCULARSTRING (0 0, 1 1, 2 0)");
        assertRoundTrip(CompoundCurve.class,      "COMPOUNDCURVE ((0 0, 1 1), CIRCULARSTRING (1 1, 2 2, 3 1))");
        assertRoundTrip(Polygon.class,            "POLYGON ((0 0, 4 0, 4 4, 0 4, 0 0), (1 1, 2 1, 2 2, 1 2, 1 1))");
        assertRoundTrip(Triangle.class,           "TRIANGLE ((0 0, 1 0, 0 1, 0 0))");
        assertRoundTrip(CurvePolygon.class,       "CURVEPOLYGON (CIRCULARSTRING (0 0, 2 0, 2 2, 0 2, 0 0))");
        assertRoundTrip(MultiPoint.class,         "MULTIPOINT ((1 2), (3 4))");
        assertRoundTrip(MultiLineString.class,    "MULTILINESTRING ((0 0, 1 1), (2 2, 3 3))");
        assertRoundTrip(MultiPolygon.class,       "MULTIPOLYGON (((0 0, 1 0, 1 1, 0 0)), ((2 2, 3 2, 3 3, 2 2)))");
        assertRoundTrip(GeometryCollection.class, "GEOMETRYCOLLECTION (POINT (1 2), LINESTRING (0 0, 1 1))");
    }

    /**
     * Tests the types whose members keep their own keyword, and the surface patch types.
     * Those are the extensions of ISO 13249-3 over OGC Simple Feature Access.
     */
    @Test
    public void testRoundTripCurvedAndPatches() {
        assertRoundTrip(MultiCurve.class,
                "MULTICURVE (LINESTRING (0 0, 1 1), CIRCULARSTRING (1 1, 2 2, 3 1))");
        assertRoundTrip(MultiSurface.class,
                "MULTISURFACE (POLYGON ((0 0, 1 0, 1 1, 0 0)), CURVEPOLYGON (CIRCULARSTRING (0 0, 2 0, 2 2, 0 2, 0 0)))");
        assertRoundTrip(PolyhedralSurface.class,
                "POLYHEDRALSURFACE Z (((0 0 0, 1 0 0, 1 1 0, 0 0 0)), ((0 0 0, 1 1 0, 0 1 0, 0 0 0)))");
        assertRoundTrip(TIN.class,
                "TIN Z (((0 0 0, 1 0 0, 1 1 0, 0 0 0)), ((0 0 0, 1 1 0, 0 1 0, 0 0 0)))");
    }

    /**
     * Tests the {@code Z}, {@code M} and {@code ZM} flags.
     */
    @Test
    public void testDimensionFlags() {
        assertRoundTrip(Point.class,      "POINT Z (1 2 3)");
        assertRoundTrip(Point.class,      "POINT M (1 2 3)");
        assertRoundTrip(Point.class,      "POINT ZM (1 2 3 4)");
        assertRoundTrip(LineString.class, "LINESTRING Z (0 0 0, 1 1 1)");
        assertRoundTrip(LineString.class, "LINESTRING M (0 0 5, 1 1 6)");
        assertRoundTrip(LineString.class, "LINESTRING ZM (0 0 0 5, 1 1 1 6)");
        assertRoundTrip(MultiPoint.class, "MULTIPOINT ZM ((1 2 3 4), (5 6 7 8))");
        assertRoundTrip(Polygon.class,    "POLYGON Z ((0 0 0, 1 0 0, 1 1 0, 0 0 0))");
        /*
         * A geometry collection repeats the flag on each of its members, since each member
         * carries its own keyword.
         */
        assertRoundTrip(GeometryCollection.class, "GEOMETRYCOLLECTION Z (POINT Z (1 2 3))");
    }

    /**
     * Verifies that the measure is read into the dedicated attribute rather than into the
     * position, and that the position keeps the number of dimensions the flag announces.
     */
    @Test
    public void testMeasureIsASeparateAttribute() {
        final LineString line = assertInstanceOf(LineString.class, wkt.decode("LINESTRING ZM (0 1 2 5, 3 4 5 6)"));
        final DataPoints points = line.getDataPoints();
        assertEquals(3, points.getDimension(), "Position dimension");
        assertEquals(2, points.size());
        assertArrayEquals(new double[] {0, 1, 2}, points.getPosition(0).toArrayDouble());
        assertArrayEquals(new double[] {3, 4, 5}, points.getPosition(1).toArrayDouble());
        assertTrue(points.getAttributesType().getAttributeNames().contains(AttributesType.ATT_M));
        assertEquals(5.0, points.getAttribute(0, AttributesType.ATT_M).get(0));
        assertEquals(6.0, points.getAttribute(1, AttributesType.ATT_M).get(0));
        /*
         * Without the flag, a 2-dimensional text carries no measure at all.
         */
        final LineString plain = assertInstanceOf(LineString.class, wkt.decode("LINESTRING (0 1, 3 4)"));
        assertFalse(plain.getDataPoints().getAttributesType().getAttributeNames().contains(AttributesType.ATT_M));
    }

    /**
     * Tests the {@code EMPTY} form of every supported geometry type.
     */
    @Test
    public void testEmpty() {
        assertRoundTrip(LineString.class,         "LINESTRING EMPTY");
        assertRoundTrip(CircularString.class,     "CIRCULARSTRING EMPTY");
        assertRoundTrip(CompoundCurve.class,      "COMPOUNDCURVE EMPTY");
        assertRoundTrip(Polygon.class,            "POLYGON EMPTY");
        assertRoundTrip(Triangle.class,           "TRIANGLE EMPTY");
        assertRoundTrip(CurvePolygon.class,       "CURVEPOLYGON EMPTY");
        assertRoundTrip(TIN.class,                "TIN EMPTY");
        assertRoundTrip(MultiPoint.class,         "MULTIPOINT EMPTY");
        assertRoundTrip(MultiLineString.class,    "MULTILINESTRING EMPTY");
        assertRoundTrip(MultiPolygon.class,       "MULTIPOLYGON EMPTY");
        assertRoundTrip(GeometryCollection.class, "GEOMETRYCOLLECTION EMPTY");
    }

    /**
     * Verifies that an {@link Empty} geometry, which has no type of its own, is written as the
     * type-less empty form of the grammar.
     */
    @Test
    public void testEmptyGeometry() {
        final Geometry empty = GeometryFactory.createEmpty(Geometries.getUndefinedCRS(2));
        assertEquals("GEOMETRYCOLLECTION EMPTY", wkt.encode(empty));
    }

    /**
     * Verifies the one empty form which does not round-trip: the model has no empty point, since
     * a point holds exactly one position, so {@code POINT EMPTY} becomes an {@link Empty}.
     */
    @Test
    public void testEmptyPoint() {
        assertInstanceOf(Empty.class, wkt.decode("POINT EMPTY"));
        assertInstanceOf(Empty.class, wkt.decode("POINT Z EMPTY"));
        assertEquals("GEOMETRYCOLLECTION EMPTY", wkt.encode(wkt.decode("POINT EMPTY")));
        assertMalformed("MULTIPOINT (EMPTY, (1 2))");
    }

    /**
     * Tests the forms which the grammar allows but the encoder does not write:
     * lower case keywords, loose whitespace, a multi point without parentheses around its points,
     * a bare coordinate list as a line string, and the {@code GEOMCOLLECTION} alias.
     */
    @Test
    public void testAlternateForms() {
        assertEquals("POINT (1 2)", wkt.encode(wkt.decode("point(1 2)")));
        assertEquals("POINT (1 2)", wkt.encode(wkt.decode("  Point   (  1   2  )  ")));
        assertEquals("MULTIPOINT ((1 2), (3 4))", wkt.encode(wkt.decode("MULTIPOINT (1 2, 3 4)")));
        assertEquals("MULTICURVE (LINESTRING (0 0, 1 1))", wkt.encode(wkt.decode("MULTICURVE ((0 0, 1 1))")));
        assertEquals("MULTISURFACE (POLYGON ((0 0, 1 0, 1 1, 0 0)))",
                     wkt.encode(wkt.decode("MULTISURFACE (((0 0, 1 0, 1 1, 0 0)))")));
        assertEquals("GEOMETRYCOLLECTION (POINT (1 2))", wkt.encode(wkt.decode("GEOMCOLLECTION (POINT (1 2))")));
        /*
         * Exponents and explicit signs are part of the lexical form of a number.
         */
        assertEquals("POINT (1000 -2)", wkt.encode(wkt.decode("POINT (1e3 -2)")));
        assertEquals("POINT (1.5 2)", wkt.encode(wkt.decode("POINT (+1.5 +2.0)")));
    }

    /**
     * Tests the number of decimal digits given to the constructor.
     */
    @Test
    public void testDecimalPrecision() {
        final Geometry geometry = wkt.decode("POINT (1.23456 2.5)");
        assertEquals("POINT (1.23456 2.5)", new WellKnownText().encode(geometry));
        assertEquals("POINT (1.235 2.5)",   new WellKnownText(3).encode(geometry));
        assertEquals("POINT (1.2 2.5)",     new WellKnownText(1).encode(geometry));
        assertEquals("POINT (1 3)",         new WellKnownText(0).encode(geometry));
        assertThrows(IllegalArgumentException.class, () -> new WellKnownText(-1));
    }

    /**
     * Verifies that a whole ordinate is written without a fractional part,
     * which the model would otherwise expose as {@code 1.0}.
     */
    @Test
    public void testWholeNumbers() {
        final Point point = GeometryFactory.createPoint(Geometries.getUndefinedCRS(3), 1, 2, 3);
        assertEquals("POINT Z (1 2 3)", wkt.encode(point));
    }

    /**
     * Tests decoding in a coordinate reference system given by the caller.
     */
    @Test
    public void testDecodeWithCRS() {
        final CoordinateReferenceSystem crs = Geometries.getUndefinedCRS(3);
        final Geometry geometry = wkt.decode("POINT Z (1 2 3)", crs);
        assertSame(crs, geometry.getCoordinateReferenceSystem());
        /*
         * The number of ordinates in the text and the dimension of the system must agree.
         */
        assertThrows(IllegalArgumentException.class, () -> wkt.decode("POINT (1 2)", crs));
    }

    /**
     * Verifies that the geometry types which the format does not define are rejected
     * rather than written in an invented syntax.
     */
    @Test
    public void testUnsupportedType() {
        final DataPoints points = GeometryFactory.createSequence(
                NDArrays.of(SampleSystem.of(Geometries.getUndefinedCRS(2)), 0, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> wkt.encode(GeometryFactory.createGeodesic(points)));
        assertThrows(IllegalArgumentException.class, () -> wkt.encode(GeometryFactory.createRhumb(points)));
    }

    /**
     * Verifies that a geometry whose positions have more than three dimensions is rejected,
     * since the format has no way to tell them from a measure.
     */
    @Test
    public void testTooManyDimensions() {
        final Point point = GeometryFactory.createPoint(Geometries.getUndefinedCRS(4), 1, 2, 3, 4);
        assertThrows(IllegalArgumentException.class, () -> wkt.encode(point));
    }

    /**
     * Tests the rejection of malformed texts.
     */
    @Test
    public void testMalformed() {
        assertMalformed("");                                    // No keyword at all.
        assertMalformed("FOO (1 2)");                           // Unknown keyword.
        assertMalformed("POINT (1 2");                          // Unbalanced parenthesis.
        assertMalformed("POINT 1 2)");                          // Missing opening parenthesis.
        assertMalformed("POINT ()");                            // No coordinate.
        assertMalformed("POINT (1 2) LINESTRING (0 0, 1 1)");   // Trailing text.
        assertMalformed("POINT (1 2 3 4)");                     // Ambiguous without a flag.
        assertMalformed("LINESTRING (0 0, 1 1 1)");             // Inconsistent tuple width.
        assertMalformed("POINT Z (1 2)");                       // Flag contradicted by the tuple.
        assertMalformed("GEOMETRYCOLLECTION Z (POINT (1 2))");  // Flag contradicted by a member.
        assertMalformed("TRIANGLE ((0 0, 1 0, 0 1, 0 0), (0 0, 1 0, 0 1, 0 0))");   // Interior ring.
        assertMalformed("MULTICURVE (POLYGON ((0 0, 1 0, 1 1, 0 0)))");             // Member is not a curve.
    }

    /**
     * Verifies that the given text is rejected as malformed.
     */
    private void assertMalformed(final String text) {
        assertThrows(IllegalArgumentException.class, () -> wkt.decode(text), text);
    }
}
