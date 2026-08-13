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

import org.apache.sis.geometries.AttributesType;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.LineString;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.Polygon;
import org.apache.sis.referencing.CRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Assertions comparing two Apache SIS geometries by value.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class GeometryAssert {
    /**
     * Tolerance for comparing ordinate values read back from a decimal text encoding.
     */
    static final double TOLERANCE = 1E-9;

    /**
     * Do not allow instantiation of this class.
     */
    private GeometryAssert() {
    }

    /**
     * Asserts that the two given geometries have the same structure and, within
     * {@link #TOLERANCE}, the same coordinates.
     */
    static void assertGeometryEquals(final Geometry expected, final Geometry actual) {
        assertGeometryEquals(expected, actual, TOLERANCE);
    }

    /**
     * Asserts that the two given geometries have the same structure and, within the given
     * tolerance, the same coordinates.
     */
    static void assertGeometryEquals(final Geometry expected, final Geometry actual, final double tolerance) {
        assertNotNull(actual, "geometry");
        /*
         * `BBox` is checked first and by Java type: its `getGeometryType()` reports "POLYGON", so a
         * box and a rectangular polygon are indistinguishable by type name alone.
         */
        if (expected instanceof BBox e) {
            final BBox a = assertInstanceOf(BBox.class, actual, "geometry type");
            assertEquals(e.getDimension(), a.getDimension(), "envelope dimension");
            for (int i = 0; i < e.getDimension(); i++) {
                assertEquals(e.getMinimum(i), a.getMinimum(i), tolerance, () -> "lower corner");
                assertEquals(e.getMaximum(i), a.getMaximum(i), tolerance, () -> "upper corner");
            }
            assertCRS(e.getCoordinateReferenceSystem(), a);
            return;
        }
        assertFalse(actual instanceof BBox, () -> "Expected " + expected.getGeometryType() + " but got a BBox.");
        assertEquals(expected.getGeometryType(), actual.getGeometryType(), "geometry type");

        if (expected instanceof Point e) {
            final Point a = (Point) actual;
            assertSequenceEquals(e.asPointSequence(), a.asPointSequence(), tolerance);
        } else if (expected instanceof LineString e) {                  // Also covers LinearRing.
            final LineString a = (LineString) actual;
            assertSequenceEquals(e.getPoints(), a.getPoints(), tolerance);
        } else if (expected instanceof Polygon e) {                     // Also covers Triangle.
            final Polygon a = (Polygon) actual;
            assertEquals(e.getNumInteriorRing(), a.getNumInteriorRing(), "number of interior rings");
            assertGeometryEquals(e.getExteriorRing(), a.getExteriorRing(), tolerance);
            for (int i = 0; i < e.getNumInteriorRing(); i++) {
                assertGeometryEquals(e.getInteriorRingN(i), a.getInteriorRingN(i), tolerance);
            }
        } else if (expected instanceof GeometryCollection<?> e) {
            final GeometryCollection<?> a = (GeometryCollection<?>) actual;
            assertEquals(e.getNumGeometries(), a.getNumGeometries(), "number of geometries");
            for (int i = 0; i < e.getNumGeometries(); i++) {
                assertGeometryEquals(e.getGeometryN(i), a.getGeometryN(i), tolerance);
            }
        } else {
            fail("No comparison implemented for " + expected.getClass().getSimpleName() + '.');
        }
    }

    /**
     * Asserts that the two given point sequences have the same coordinates, within the given
     * tolerance. Delegates to {@code Array.equals(Array, double)}, which also compares the tuple
     * width and the sample system, and therefore the coordinate reference system.
     */
    private static void assertSequenceEquals(final PointSequence expected, final PointSequence actual,
            final double tolerance)
    {
        assertEquals(expected.size(), actual.size(), "number of points");
        assertEquals(expected.getDimension(), actual.getDimension(), "coordinate dimension");
        final var e = expected.getAttributeArray(AttributesType.ATT_POSITION);
        final var a = actual.getAttributeArray(AttributesType.ATT_POSITION);
        assertTrue(e.equals(a, tolerance), () -> "Expected coordinates " + e + " but got " + a);
    }

    /**
     * Asserts that the given geometry uses a coordinate reference system equivalent to the expected
     * one. Uses {@link CRS#equivalent} rather than {@code equals}, because a CRS that the reader
     * had to promote from two to three dimensions is equivalent to, but not the same object as,
     * one built directly.
     */
    static void assertCRS(final CoordinateReferenceSystem expected, final Geometry actual) {
        final CoordinateReferenceSystem crs = actual.getCoordinateReferenceSystem();
        assertNotNull(crs, "An Apache SIS geometry always has a coordinate reference system.");
        assertTrue(CRS.equivalent(expected, crs),
                () -> "Expected CRS \"" + expected.getName().getCode() + "\" but got \"" + crs.getName().getCode() + "\".");
    }

    /**
     * Asserts that the given geometry uses the placeholder CRS that the readers substitute when a
     * document declares no {@code srsName} at any enclosing level.
     */
    static void assertUndefinedCRS(final Geometry actual) {
        final CoordinateReferenceSystem crs = actual.getCoordinateReferenceSystem();
        assertNotNull(crs, "An Apache SIS geometry always has a coordinate reference system.");
        assertTrue(Geometries.isUndefined(crs),
                () -> "Expected an undefined CRS but got \"" + crs.getName().getCode() + "\".");
    }
}
