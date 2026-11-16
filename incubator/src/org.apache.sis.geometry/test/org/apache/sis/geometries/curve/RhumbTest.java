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
package org.apache.sis.geometries.curve;

import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.cs.Bearing;
import org.apache.sis.geometries.cs.FixedDirection;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.datum.DefaultEllipsoid;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link Rhumb}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class RhumbTest extends GeometryTest {
    /**
     * Geographic reference system on a sphere, with (longitude, latitude) axes in degrees.
     */
    private static final CoordinateReferenceSystem SPHERE = CommonCRS.SPHERE.normalizedGeographic();

    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetInterpolation() {
    }

    /**
     * Tests the bearing along the equator, a closed parallel, a meridian, across the anti-meridian,
     * and with (latitude, longitude, height) axes.
     */
    @Test
    public void testGetBearing() {
        assertAzimuths(rhumb(CRS_2D, 0,0, 90,0, 0,0), 90, 270);
        assertAzimuths(rhumb(CRS_2D, 0,80, 120,80, 240,80, 0,80), 90, 90, 90);
        assertAzimuths(rhumb(CRS_2D, 10,0, 10,45, 10,0), 0, 180);
        assertAzimuths(rhumb(CRS_2D, 170,10, -170,10, 170,10), 90, 270);
        assertAzimuths(GeometryFactory.DEFAULT.createRhumb(
                GeometryFactory.DEFAULT.createDataPoints(CRS_3D, 0,0,100, 0,90,200)), 90);
        assertEquals(FixedDirection.TRUE_NORTH, rhumb(CRS_2D, 0,0, 90,0).getBearing(0).getReference());
    }

    /**
     * Tests segment indices outside the range of segments.
     */
    @Test
    public void testGetBearingIndex() {
        final Rhumb rhumb = rhumb(CRS_2D, 0,0, 90,0);
        assertThrows(IndexOutOfBoundsException.class, () -> rhumb.getBearing(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> rhumb.getBearing(1));
    }

    /**
     * Tests segments half a turn apart, which go east.
     */
    @Test
    public void testGetBearingHalfTurn() {
        assertAzimuths(rhumb(CRS_2D, 0,10, 180,10, 0,10), 90, 90);
        final Rhumb rhumb = rhumb(CRS_2D, 0,10, 180,20, 0,10);
        final double azimuth = azimuth(rhumb, 0);
        assertTrue(azimuth > 0 && azimuth < 90, () -> "Azimuth " + azimuth + " does not go north-east.");
        assertAzimuths(rhumb, azimuth, 180 - azimuth);
    }

    /**
     * Tests points on rhumb lines of known azimuth, on a sphere and on the WGS84 ellipsoid.
     * On the ellipsoid, the azimuths would differ if computed on a sphere.
     */
    @Test
    public void testGetBearingOnRhumbLine() {
        assertAzimuths(rhumb(SPHERE, rhumbLine(0, 30, 0, 20, 40, 60)), 30, 30, 30);
        assertAzimuths(rhumb(SPHERE, rhumbLine(0, 210, 60, 40, 20, 0)), 210, 210, 210);
        final double eccentricity = DefaultEllipsoid.castOrCopy(CommonCRS.WGS84.ellipsoid()).getEccentricity();
        assertAzimuths(rhumb(CRS_2D, rhumbLine(eccentricity, 60, -20, 10, 40, 70)), 60, 60, 60);
        assertAzimuths(rhumb(CRS_2D, rhumbLine(eccentricity, 300, -20, 10, 40, 70)), 300, 300, 300);
        assertAzimuths(rhumb(CRS_2D, rhumbLine(eccentricity, 120, 70, 40, 10, -20)), 120, 120, 120);
        assertNotEquals(60, azimuth(rhumb(SPHERE, rhumbLine(eccentricity, 60, -20, 10, 40, 70)), 2), 1E-6);
    }

    /**
     * Tests segments from and to a pole, which follow meridians.
     */
    @Test
    public void testGetBearingPole() {
        assertAzimuths(rhumb(CRS_2D, 30,80, 0,90, 120,80), 0, 180);
        assertAzimuths(rhumb(CRS_2D, 30,-80, 0,-90, 0,90), 180, 0);
    }

    /**
     * Tests segments without length, which have no bearing.
     */
    @Test
    public void testGetBearingWithoutLength() {
        assertAzimuths(rhumb(CRS_2D, 0,0, 0,0, 90,0, 450,0), Double.NaN, 90, Double.NaN);
        assertAzimuths(rhumb(CRS_2D, 0,90, 120,90), Double.NaN);
        assertAzimuths(rhumb(Geometries.getUndefinedCRS(2), 10,20, 10,20), Double.NaN);
    }

    /**
     * Tests a Cartesian system, where segments are straight and the bearing is measured from grid north.
     */
    @Test
    public void testGetBearingCartesian() {
        final Rhumb rhumb = rhumb(Geometries.getUndefinedCRS(2), 0,0, 10,10, 20,10);
        assertAzimuths(rhumb, 45, 90);
        assertEquals(FixedDirection.GRID_NORTH, rhumb.getBearing(0).getReference());
    }

    /**
     * Returns the (longitude, latitude) coordinates of points on a rhumb line starting at longitude 0°.
     * The longitude is proportional to the isometric latitude ψ = atanh(sin φ) − ℯ⋅atanh(ℯ⋅sin φ).
     *
     * @param  eccentricity  eccentricity of the ellipsoid, 0 for a sphere.
     * @param  azimuth       azimuth of the rhumb line in degrees, not 90° or 270°.
     * @param  latitudes     latitudes of the points in degrees, the first one at longitude 0°.
     */
    private static double[] rhumbLine(final double eccentricity, final double azimuth, final double... latitudes) {
        final double slope = Math.tan(Math.toRadians(azimuth));
        final double start = isometricLatitude(eccentricity, latitudes[0]);
        final double[] coordinates = new double[latitudes.length * 2];
        for (int i = 0; i < latitudes.length; i++) {
            coordinates[i*2]     = Math.toDegrees(slope * (isometricLatitude(eccentricity, latitudes[i]) - start));
            coordinates[i*2 + 1] = latitudes[i];
        }
        return coordinates;
    }

    private static double isometricLatitude(final double eccentricity, final double latitude) {
        final double sin = Math.sin(Math.toRadians(latitude));
        return atanh(sin) - eccentricity * atanh(eccentricity * sin);
    }

    private static double atanh(final double x) {
        return 0.5 * Math.log((1 + x) / (1 - x));
    }

    private static double azimuth(final Rhumb rhumb, final int segment) {
        final Bearing bearing = rhumb.getBearing(segment);
        assertNotNull(bearing, () -> "No bearing for segment " + segment + " of " + rhumb);
        return bearing.getAzimuth().to(Units.DEGREE).getValue().doubleValue();
    }

    /**
     * Asserts the azimuth of each segment of the rhumb, NaN meaning that the segment has no bearing.
     */
    private static void assertAzimuths(final Rhumb rhumb, final double... expected) {
        assertEquals(expected.length, rhumb.getNumPoints() - 1);
        for (int i = 0; i < expected.length; i++) {
            if (Double.isNaN(expected[i])) {
                assertNull(rhumb.getBearing(i));
            } else {
                assertEquals(expected[i], azimuth(rhumb, i), TOLERANCE);
            }
        }
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumPoints() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetPointN() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetControlPoints() {
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
    public void testGetAttributesType() {
    }

    @Test
    public void testGetTopologicDimension() {
        final Rhumb rhumb = GeometryFactory.DEFAULT.createRhumb(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,10));
        assertEquals(1, rhumb.getTopologicDimension());
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDataPoints() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetLength() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetStartPoint() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetEndPoint() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsClosed() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsCycle() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsRing() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetBoundary() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetKnots() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetStartConstrParam() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetEndConstrParam() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetStartParam() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetEndParam() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumDerivativesInterior() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumDerivativesStart() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumDerivativesEnd() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetReverse() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testAsLine() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testConstrParam() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetLength_DirectPosition_DirectPosition() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetLength_double_double() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testParam() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testParamForPoint() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testTangent_Length() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testTangent_double() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testSubCurve_double_double() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testSubCurve_Length_Length() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetOrientationSign() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetProxy() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetPrimitive() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetBoundaryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetDimension() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetSegments() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetMetadata() {
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
    @Disabled("Not implemented yet.")
    public void testGetGeometryType2() {
    }

    /**
     * Tests the envelope in a Cartesian system and along a parallel, where a rhumb does not bulge.
     */
    @Test
    public void testGetEnvelope() {
        Envelope envelope = GeometryFactory.DEFAULT.createRhumb(
                GeometryFactory.DEFAULT.createDataPoints(Geometries.getUndefinedCRS(2), 0,0, 10,10)).getEnvelope();
        assertRange(envelope, 0, 0, 10);
        assertRange(envelope, 1, 0, 10);

        envelope = rhumb(CRS_2D, 0,45, 90,45).getEnvelope();
        assertRange(envelope, 0, 0, 90);
        assertRange(envelope, 1, 45, 45);
    }

    /**
     * Tests a rhumb crossing the anti-meridian, and segments half a turn apart, which go east.
     */
    @Test
    public void testGetEnvelopeAcrossAntimeridian() {
        Envelope envelope = rhumb(CRS_2D, 170,10, -170,20).getEnvelope();
        assertRange(envelope, 0, 170, -170);
        assertRange(envelope, 1, 10, 20);

        envelope = rhumb(CRS_2D, 0,0, 180,0).getEnvelope();
        assertRange(envelope, 0, 0, 180);
    }

    /**
     * Tests a closed rhumb around the north pole, which crosses all longitudes but does not reach the pole.
     */
    @Test
    public void testGetEnvelopeAroundPole() {
        final Envelope envelope = rhumb(CRS_2D, 0,80, 120,80, 240,80, 0,80).getEnvelope();
        assertRange(envelope, 0, -180, 180);
        assertRange(envelope, 1, 80, 80);
    }

    /**
     * Tests a rhumb ending on a pole, where the longitude is undefined.
     */
    @Test
    public void testGetEnvelopeToPole() {
        final Envelope envelope = rhumb(CRS_2D, 30,80, 0,90).getEnvelope();
        assertRange(envelope, 0, 30, 30);
        assertRange(envelope, 1, 80, 90);
    }

    /**
     * Tests a reference system with (latitude, longitude, height) axes.
     */
    @Test
    public void testGetEnvelopeAxisOrder() {
        final Envelope envelope = GeometryFactory.DEFAULT.createRhumb(
                GeometryFactory.DEFAULT.createDataPoints(CRS_3D, 45,0,100, 50,90,200)).getEnvelope();
        assertRange(envelope, 0, 45, 50);
        assertRange(envelope, 1, 0, 90);
        assertRange(envelope, 2, 100, 200);
    }

    private static Rhumb rhumb(final CoordinateReferenceSystem crs, final double... coordinates) {
        return GeometryFactory.DEFAULT.createRhumb(GeometryFactory.DEFAULT.createDataPoints(crs, coordinates));
    }

    /**
     * Asserts the raw bounds of the envelope in one dimension. The lower bound is greater than the upper one
     * when the range crosses the anti-meridian.
     */
    private static void assertRange(final Envelope envelope, final int dimension, final double lower, final double upper) {
        assertEquals(lower, envelope.getLowerCorner().getCoordinate(dimension), TOLERANCE);
        assertEquals(upper, envelope.getUpperCorner().getCoordinate(dimension), TOLERANCE);
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
    public void testIsValid() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testUserProperties() {
    }
}
