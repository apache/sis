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

import java.nio.ByteOrder;
import java.util.Arrays;
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
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link WellKnownBinary}.
 *
 * <p>Geometries are built from their Well-Known Text rather than from the factory, so that a test
 * case says in one readable line what it encodes. {@link WellKnownTextTest} is what verifies that
 * those texts mean what they look like.</p>
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class WellKnownBinaryTest {
    /**
     * Bits of {@link Double#NaN}, which is how an empty point is written.
     */
    private static final String NAN = "7FF8000000000000";

    /**
     * The codec under test, writing in the big endian order of the standard.
     */
    private final WellKnownBinary wkb = new WellKnownBinary();

    /**
     * The text codec, used for building the geometries to encode and for reading back the
     * geometries decoded.
     */
    private final WellKnownText wkt = new WellKnownText();

    /**
     * Creates a new test case.
     */
    public WellKnownBinaryTest() {
    }

    /**
     * Verifies that the geometry of the given text encodes to bytes which decode back to a
     * geometry of the expected type and of the same text. The text is the canonical form of the
     * encoder of {@link WellKnownText}, so the round trip is an equality and not merely an
     * equivalence.
     */
    private void assertRoundTrip(final Class<? extends Geometry> type, final String text) {
        final Geometry source = wkt.decode(text);
        final byte[] bytes = wkb.encode(source);
        final Geometry target = wkb.decode(bytes);
        assertInstanceOf(type, target, text);
        assertEquals(text, wkt.encode(target), "Round trip of " + text);
        assertArrayEquals(bytes, source.asBinary(), "Geometry.asBinary() of " + text);
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
     * Tests the types whose members keep their own header, and the surface patch types.
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
     * Tests the {@code Z}, {@code M} and {@code ZM} flags, which are the thousands of a type code.
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
         * A collection repeats the flag in the type code of each of its members,
         * since each member carries its own header.
         */
        assertRoundTrip(GeometryCollection.class, "GEOMETRYCOLLECTION Z (POINT Z (1 2 3))");
        /*
         * The flags are added to the type code: 1000 for Z, 2000 for M, 3000 for ZM.
         */
        assertEquals("00000003E93FF000000000000040000000000000004008000000000000",
                     hex(wkb.encode(wkt.decode("POINT Z (1 2 3)"))));
    }

    /**
     * Verifies that the measure is read into the dedicated attribute rather than into the
     * position, and that the position keeps the number of dimensions the flag announces.
     */
    @Test
    public void testMeasureIsASeparateAttribute() {
        final byte[] bytes = wkb.encode(wkt.decode("LINESTRING ZM (0 1 2 5, 3 4 5 6)"));
        final LineString line = assertInstanceOf(LineString.class, wkb.decode(bytes));
        final DataPoints points = line.getDataPoints();
        assertEquals(3, points.getDimension(), "Position dimension");
        assertEquals(2, points.size());
        assertArrayEquals(new double[] {0, 1, 2}, points.getPosition(0).toArrayDouble());
        assertArrayEquals(new double[] {3, 4, 5}, points.getPosition(1).toArrayDouble());
        assertTrue(points.getAttributesType().getAttributeNames().contains(AttributesType.ATT_M));
        assertEquals(5.0, points.getAttribute(0, AttributesType.ATT_M).get(0));
        assertEquals(6.0, points.getAttribute(1, AttributesType.ATT_M).get(0));
        /*
         * Without the flag, a 2-dimensional geometry carries no measure at all.
         */
        final LineString plain = assertInstanceOf(LineString.class,
                wkb.decode(wkb.encode(wkt.decode("LINESTRING (0 1, 3 4)"))));
        assertFalse(plain.getDataPoints().getAttributesType().getAttributeNames().contains(AttributesType.ATT_M));
    }

    /**
     * Tests the empty form, a count of zero, of every supported geometry type.
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
        /*
         * An empty geometry is a header and a count of zero, nothing more.
         */
        assertEquals("000000000700000000", hex(wkb.encode(wkt.decode("GEOMETRYCOLLECTION EMPTY"))));
        assertEquals("000000000200000000", hex(wkb.encode(wkt.decode("LINESTRING EMPTY"))));
        assertEquals("000000000300000000", hex(wkb.encode(wkt.decode("POLYGON EMPTY"))));
    }

    /**
     * Verifies that an {@link Empty} geometry, which has no type of its own, is written as the
     * type-less empty form of the format.
     */
    @Test
    public void testEmptyGeometry() {
        final Geometry empty = GeometryFactory.createEmpty(Geometries.getUndefinedCRS(2));
        assertEquals("000000000700000000", hex(wkb.encode(empty)));
        assertEquals("000000000700000000", hex(empty.asBinary()));
    }

    /**
     * Verifies the one empty form which does not round-trip: the format writes a point as a bare
     * coordinate tuple, so an empty point is a tuple of {@link Double#NaN}, and the model has no
     * empty point to decode it into.
     */
    @Test
    public void testEmptyPoint() {
        final byte[] empty2D = bytes("00" + "00000001" + NAN + NAN);
        final byte[] empty3D = bytes("00" + "000003E9" + NAN + NAN + NAN);
        assertInstanceOf(Empty.class, wkb.decode(empty2D));
        assertInstanceOf(Empty.class, wkb.decode(empty3D));
        assertEquals("000000000700000000", hex(wkb.encode(wkb.decode(empty2D))));
        /*
         * A single NaN ordinate is a legitimate coordinate, not an empty point.
         */
        assertInstanceOf(Point.class, wkb.decode(bytes("00" + "00000001" + NAN + "4000000000000000")));
        /*
         * There is therefore no way to put an empty point in a multi point.
         */
        assertMalformed(bytes("00" + "00000004" + "00000001" + "00" + "00000001" + NAN + NAN));
    }

    /**
     * Tests the byte order flag: both orders are written on demand, both are read whatever the
     * order the codec writes, and the order may change from one nested geometry to the next.
     */
    @Test
    public void testByteOrder() {
        final Geometry point = wkt.decode("POINT (1 2)");
        final byte[] bigEndian    = new WellKnownBinary(ByteOrder.BIG_ENDIAN).encode(point);
        final byte[] littleEndian = new WellKnownBinary(ByteOrder.LITTLE_ENDIAN).encode(point);
        assertEquals("00000000013FF00000000000004000000000000000", hex(bigEndian));
        assertEquals("0101000000000000000000F03F0000000000000040", hex(littleEndian));
        assertArrayEquals(bigEndian, wkb.encode(point), "Big endian is the default");
        /*
         * Whichever order the codec writes, it reads both.
         */
        final WellKnownBinary reader = new WellKnownBinary(ByteOrder.LITTLE_ENDIAN);
        assertEquals("POINT (1 2)", wkt.encode(reader.decode(bigEndian)));
        assertEquals("POINT (1 2)", wkt.encode(reader.decode(littleEndian)));
        /*
         * A collection written in one order may hold a member written in the other.
         */
        final byte[] mixed = concat(bytes("00" + "00000007" + "00000001"), littleEndian);
        assertEquals("GEOMETRYCOLLECTION (POINT (1 2))", wkt.encode(wkb.decode(mixed)));
        assertThrows(NullPointerException.class, () -> new WellKnownBinary((ByteOrder) null));
    }

    /**
     * Tests decoding in a coordinate reference system given by the caller.
     */
    @Test
    public void testDecodeWithCRS() {
        final CoordinateReferenceSystem crs = Geometries.getUndefinedCRS(3);
        final byte[] bytes = wkb.encode(wkt.decode("POINT Z (1 2 3)"));
        final Geometry geometry = wkb.decode(bytes, crs);
        assertSame(crs, geometry.getCoordinateReferenceSystem());
        /*
         * The number of ordinates the flags announce and the dimension of the system must agree.
         */
        final byte[] flat = wkb.encode(wkt.decode("POINT (1 2)"));
        assertThrows(IllegalArgumentException.class, () -> wkb.decode(flat, crs));
    }

    /**
     * Verifies that the geometry types which the format does not define are rejected
     * rather than written in an invented structure.
     */
    @Test
    public void testUnsupportedType() {
        final DataPoints points = GeometryFactory.createSequence(
                NDArrays.of(SampleSystem.of(Geometries.getUndefinedCRS(2)), 0, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> wkb.encode(GeometryFactory.createGeodesic(points)));
        assertThrows(IllegalArgumentException.class, () -> wkb.encode(GeometryFactory.createRhumb(points)));
    }

    /**
     * Verifies that a geometry whose positions have more than three dimensions is rejected,
     * since the format has no way to tell them from a measure.
     */
    @Test
    public void testTooManyDimensions() {
        final Point point = GeometryFactory.createPoint(Geometries.getUndefinedCRS(4), 1, 2, 3, 4);
        assertThrows(IllegalArgumentException.class, () -> wkb.encode(point));
    }

    /**
     * Tests the rejection of malformed byte sequences built by altering a valid one.
     */
    @Test
    public void testTruncatedAndTrailing() {
        final byte[] point = wkb.encode(wkt.decode("POINT (1 2)"));
        assertMalformed(new byte[0]);                                   // Not even a byte order flag.
        assertMalformed(Arrays.copyOf(point, 1));                       // No type code.
        assertMalformed(Arrays.copyOf(point, point.length - 1));        // Truncated ordinate.
        assertMalformed(Arrays.copyOf(point, point.length + 1));        // Trailing byte.
        final byte[] badOrder = point.clone();
        badOrder[0] = 2;                                                // Neither 0 nor 1.
        assertMalformed(badOrder);
    }

    /**
     * Tests the rejection of type codes which the format does not define.
     */
    @Test
    public void testMalformedTypeCode() {
        assertMalformed(bytes("00" + "00000000" + "00000000"));         // Code 0.
        assertMalformed(bytes("00" + "0000000D" + "00000000"));         // Abstract Curve type.
        assertMalformed(bytes("00" + "000000FF" + "00000000"));         // Unknown code.
        assertMalformed(bytes("00" + "00001389" + "00000000"));         // Flags of 5000.
        assertMalformed(bytes("00" + "80000001" + "00000000"));         // Extended WKB flags.
    }

    /**
     * Tests the rejection of the structural errors which the counts and the type codes can carry.
     */
    @Test
    public void testMalformedStructure() {
        // A count larger than the bytes which remain.
        assertMalformed(bytes("00" + "00000002" + "7FFFFFFF"));
        // A geometry collection announcing Z, holding a member which does not.
        assertMalformed(concat(bytes("00" + "000003EF" + "00000001"),
                               wkb.encode(wkt.decode("POINT (1 2)"))));
        // A triangle with an interior ring.
        assertMalformed(swapTypeCode(wkb.encode(wkt.decode(
                "POLYGON ((0 0, 4 0, 4 4, 0 0), (1 1, 2 1, 2 2, 1 1))")), 17));
        // A multi curve whose member is a polygon.
        assertMalformed(concat(bytes("00" + "0000000B" + "00000001"),
                               wkb.encode(wkt.decode("POLYGON ((0 0, 1 0, 1 1, 0 0))"))));
        // A multi line string whose member is a circular string.
        assertMalformed(concat(bytes("00" + "00000005" + "00000001"),
                               wkb.encode(wkt.decode("CIRCULARSTRING (0 0, 1 1, 2 0)"))));
    }

    /**
     * Verifies that the given bytes are rejected as malformed.
     */
    private void assertMalformed(final byte[] data) {
        assertThrows(IllegalArgumentException.class, () -> wkb.decode(data), hex(data));
    }

    /**
     * Bits of the ordinates used by the extended tests below.
     */
    private static final String ZERO = "0000000000000000", 
                                ONE = "3FF0000000000000",
                                TWO = "4000000000000000", 
                                THREE = "4008000000000000", 
                                FOUR = "4010000000000000";

    /**
     * Tests the spatial reference identifier which the {@link WellKnownBinary.Flavor#EWKB}
     * dialect adds, and the high order bits which announce it.
     */
    @Test
    public void testExtendedRoundTrip() {
        final WellKnownBinary ewkb = new WellKnownBinary(WellKnownBinary.Flavor.EWKB);
        assertEquals(WellKnownBinary.Flavor.EWKB, ewkb.getFlavor());
        assertEquals(WellKnownBinary.Flavor.OGC,  wkb.getFlavor());
        /*
         * The 0x20000000 bit announces the identifier, which follows the type code.
         * EPSG:4326 is 0x10E6.
         */
        final Geometry point = GeometryFactory.createPoint(CommonCRS.WGS84.geographic(), 1, 2);
        final byte[] bytes = ewkb.encode(point);
        assertEquals("00" + "20000001" + "000010E6" + ONE + TWO, hex(bytes));
        /*
         * Reading gives the geometry its system back, and writing it again gives the same bytes.
         */
        final Geometry back = ewkb.decode(bytes);
        assertEquals("4326", IdentifiedObjects.getIdentifier(
                back.getCoordinateReferenceSystem(), Citations.EPSG).getCode());
        assertEquals("POINT (1 2)", wkt.encode(back));
        assertArrayEquals(bytes, ewkb.encode(back));
    }

    /**
     * Verifies that only the outermost geometry carries the identifier, the nested ones being
     * written with a plain type code.
     */
    @Test
    public void testExtendedIdentifierOnOutermostOnly() {
        final WellKnownBinary ewkb = new WellKnownBinary(WellKnownBinary.Flavor.EWKB);
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic();
        final byte[] bytes = ewkb.encode(GeometryFactory.createMultiPoint(
                GeometryFactory.createPoint(crs, 1, 2),
                GeometryFactory.createPoint(crs, 3, 4)));
        assertEquals("00" + "20000004" + "000010E6" + "00000002"
                   + "00" + "00000001" + ONE   + TWO
                   + "00" + "00000001" + THREE + FOUR, hex(bytes));
        /*
         * The identifier read on the outermost geometry reaches the members as well.
         */
        final MultiPoint<?> decoded = assertInstanceOf(MultiPoint.class, ewkb.decode(bytes));
        assertEquals("MULTIPOINT ((1 2), (3 4))", wkt.encode(decoded));
        assertEquals("4326", IdentifiedObjects.getIdentifier(
                decoded.getGeometryN(0).getCoordinateReferenceSystem(), Citations.EPSG).getCode());
    }

    /**
     * Tests the dimension flags of the extended dialect, which are high order bits rather than
     * the thousands of the type code.
     */
    @Test
    public void testExtendedDimensionFlags() {
        final WellKnownBinary ewkb = new WellKnownBinary(WellKnownBinary.Flavor.EWKB);
        assertEquals("00" + "80000001" + ONE + TWO + THREE,
                     hex(ewkb.encode(wkt.decode("POINT Z (1 2 3)"))));
        assertEquals("00" + "40000001" + ONE + TWO + THREE,
                     hex(ewkb.encode(wkt.decode("POINT M (1 2 3)"))));
        assertEquals("00" + "C0000001" + ONE + TWO + THREE + FOUR,
                     hex(ewkb.encode(wkt.decode("POINT ZM (1 2 3 4)"))));
        assertEquals("00" + "80000002" + "00000002" + ZERO + ZERO + ZERO + ONE + ONE + ONE,
                     hex(ewkb.encode(wkt.decode("LINESTRING Z (0 0 0, 1 1 1)"))));
        /*
         * Every one of them reads back to the geometry it was written from.
         */
        for (final String text : new String[] {
                "POINT Z (1 2 3)", "POINT M (1 2 3)", "POINT ZM (1 2 3 4)",
                "LINESTRING Z (0 0 0, 1 1 1)", "POLYGON ZM ((0 0 0 5, 1 0 0 6, 1 1 0 7, 0 0 0 5))",
                "GEOMETRYCOLLECTION Z (POINT Z (1 2 3))"})
        {
            assertEquals(text, wkt.encode(ewkb.decode(ewkb.encode(wkt.decode(text)))), text);
        }
    }

    /**
     * Verifies that the extended dialect is a superset of the standard one on reading, and that
     * the converse does not hold.
     */
    @Test
    public void testExtendedReadsPlainBinary() {
        final WellKnownBinary ewkb = new WellKnownBinary(WellKnownBinary.Flavor.EWKB);
        for (final String text : new String[] {
                "POINT (1 2)", "POINT Z (1 2 3)", "POINT ZM (1 2 3 4)",
                "POLYGON Z ((0 0 0, 1 0 0, 1 1 0, 0 0 0))",
                "GEOMETRYCOLLECTION (POINT (1 2), LINESTRING (0 0, 1 1))"})
        {
            assertEquals(text, wkt.encode(ewkb.decode(wkb.encode(wkt.decode(text)))), text);
        }
        /*
         * The standard dialect rejects the high order bits of the extended one.
         */
        assertMalformed(ewkb.encode(wkt.decode("POINT Z (1 2 3)")));
        assertMalformed(ewkb.encode(GeometryFactory.createPoint(CommonCRS.WGS84.geographic(), 1, 2)));
    }

    /**
     * Verifies that the system the bytes name yields to one the caller gives, and tests the
     * rejection of the identifiers which cannot be used.
     */
    @Test
    public void testExtendedPrecedenceAndErrors() {
        final WellKnownBinary ewkb = new WellKnownBinary(WellKnownBinary.Flavor.EWKB);
        final byte[] bytes = ewkb.encode(GeometryFactory.createPoint(CommonCRS.WGS84.geographic(), 1, 2));
        final CoordinateReferenceSystem crs = Geometries.getUndefinedCRS(2);
        assertSame(crs, ewkb.decode(bytes, crs).getCoordinateReferenceSystem());
        /*
         * EPSG:4326 is two dimensional, so a Z geometry carrying it contradicts it.
         * 0xA0000001 is the point type with the Z bit and the identifier bit.
         */
        assertExtendedMalformed("00" + "A0000001" + "000010E6" + ONE + TWO + THREE);
        /*
         * An identifier which no authority defines. 999999 is 0xF423F.
         */
        assertExtendedMalformed("00" + "20000001" + "000F423F" + ONE + TWO);
        /*
         * Two geometries of the same sequence cannot name two different systems. 3857 is 0xF11.
         */
        assertExtendedMalformed("00" + "20000004" + "000010E6" + "00000001"
                              + "00" + "20000001" + "00000F11" + ONE + TWO);
    }

    /**
     * Verifies that the given hexadecimal bytes are rejected by the extended dialect.
     */
    private void assertExtendedMalformed(final String text) {
        final WellKnownBinary ewkb = new WellKnownBinary(WellKnownBinary.Flavor.EWKB);
        final byte[] data = bytes(text);
        assertThrows(IllegalArgumentException.class, () -> ewkb.decode(data), text);
    }

    /**
     * Verifies that the dialect which is not implemented yet says so instead of writing
     * something else.
     */
    @Test
    public void testUnimplementedFlavor() {
        final WellKnownBinary twkb = new WellKnownBinary(WellKnownBinary.Flavor.TWKB);
        assertEquals(WellKnownBinary.Flavor.TWKB, twkb.getFlavor());
        final Geometry geometry = wkt.decode("POINT (1 2)");
        final byte[] bytes = wkb.encode(geometry);
        assertThrows(UnsupportedOperationException.class, () -> twkb.encode(geometry));
        assertThrows(UnsupportedOperationException.class, () -> twkb.decode(bytes));
        assertThrows(NullPointerException.class, () -> new WellKnownBinary((WellKnownBinary.Flavor) null));
    }

    /**
     * Returns a copy of the given geometry with the type code of its outermost element replaced
     * by the given one, the dimension flags left untouched.
     */
    private static byte[] swapTypeCode(final byte[] data, final int code) {
        final byte[] copy = data.clone();
        final int flags = ((data[1] & 0xFF) << 24 | (data[2] & 0xFF) << 16
                         | (data[3] & 0xFF) <<  8 | (data[4] & 0xFF)) / 1000 * 1000;
        final int value = flags + code;
        copy[1] = (byte) (value >>> 24);
        copy[2] = (byte) (value >>> 16);
        copy[3] = (byte) (value >>>  8);
        copy[4] = (byte)  value;
        return copy;
    }

    /**
     * Returns the concatenation of the given byte sequences.
     */
    private static byte[] concat(final byte[]... parts) {
        int length = 0;
        for (final byte[] part : parts) {
            length += part.length;
        }
        final byte[] result = new byte[length];
        int offset = 0;
        for (final byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }

    /**
     * Returns the bytes of the given hexadecimal text, which must have an even number of digits.
     */
    private static byte[] bytes(final String text) {
        assertEquals(0, text.length() % 2, text);
        final byte[] data = new byte[text.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(text.substring(i * 2, i * 2 + 2), 16);
        }
        return data;
    }

    /**
     * Returns the given bytes as upper case hexadecimal digits, which is how the expected values
     * of this class are written.
     */
    private static String hex(final byte[] data) {
        final String digits = "0123456789ABCDEF";
        final StringBuilder sb = new StringBuilder(data.length * 2);
        for (final byte b : data) {
            sb.append(digits.charAt((b >>> 4) & 0xF)).append(digits.charAt(b & 0xF));
        }
        return sb.toString();
    }
}
