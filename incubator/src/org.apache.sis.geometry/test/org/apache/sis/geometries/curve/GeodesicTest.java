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
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.GeodeticCalculator;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link Geodesic}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class GeodesicTest extends GeometryTest {

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
        final Geodesic geodesic = GeometryFactory.DEFAULT.createGeodesic(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,10));
        assertEquals(1, geodesic.getTopologicDimension());
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
     * Tests the envelope in a Cartesian system and on a sphere, where the vertex latitude is known.
     */
    @Test
    public void testGetEnvelope() {
        Envelope envelope = GeometryFactory.DEFAULT.createGeodesic(
                GeometryFactory.DEFAULT.createDataPoints(Geometries.getUndefinedCRS(2), 0,0, 10,10)).getEnvelope();
        assertRange(envelope, 0, 0, 10);
        assertRange(envelope, 1, 0, 10);

        envelope = geodesic(SPHERE, 0,45, 90,45).getEnvelope();
        assertRange(envelope, 0, 0, 90);
        assertRange(envelope, 1, 45, Math.toDegrees(Math.atan(Math.sqrt(2))));

        envelope = geodesic(SPHERE, 0,-45, 90,-45).getEnvelope();
        assertRange(envelope, 1, -Math.toDegrees(Math.atan(Math.sqrt(2))), -45);
    }

    /**
     * Tests that the envelope on the WGS84 ellipsoid contains points sampled along the geodesic.
     */
    @Test
    public void testGetEnvelopeOnEllipsoid() {
        final Envelope envelope = geodesic(CRS_2D, 0,45, 90,45).getEnvelope();
        assertRange(envelope, 0, 0, 90);
        assertEquals(45, envelope.getMinimum(1), TOLERANCE);
        assertEquals(sampledMaximumLatitude(45, 0, 45, 90), envelope.getMaximum(1), 1E-4);
        assertTrue(envelope.getMaximum(1) >= sampledMaximumLatitude(45, 0, 45, 90));
    }

    /**
     * Tests a geodesic crossing the anti-meridian.
     */
    @Test
    public void testGetEnvelopeAcrossAntimeridian() {
        final Envelope envelope = geodesic(CRS_2D, 170,10, -170,10).getEnvelope();
        assertRange(envelope, 0, 170, -170);
        assertEquals(10, envelope.getMinimum(1), TOLERANCE);
        assertTrue(envelope.getMaximum(1) > 10);
    }

    /**
     * Tests a geodesic passing over the north pole, which crosses all longitudes.
     */
    @Test
    public void testGetEnvelopeOverPole() {
        final Envelope envelope = geodesic(SPHERE, 0,80, 180,80).getEnvelope();
        assertRange(envelope, 0, -180, 180);
        assertRange(envelope, 1, 80, 90);
    }

    /**
     * Tests a geodesic ending on a pole, where the longitude is undefined.
     */
    @Test
    public void testGetEnvelopeToPole() {
        final Envelope envelope = geodesic(CRS_2D, 30,80, 0,90).getEnvelope();
        assertRange(envelope, 0, 30, 30);
        assertRange(envelope, 1, 80, 90);
    }

    /**
     * Tests a reference system with (latitude, longitude, height) axes.
     */
    @Test
    public void testGetEnvelopeAxisOrder() {
        final Envelope envelope = GeometryFactory.DEFAULT.createGeodesic(
                GeometryFactory.DEFAULT.createDataPoints(CRS_3D, 45,0,100, 45,90,200)).getEnvelope();
        assertEquals(45, envelope.getMinimum(0), TOLERANCE);
        assertEquals(sampledMaximumLatitude(45, 0, 45, 90), envelope.getMaximum(0), 1E-4);
        assertRange(envelope, 1, 0, 90);
        assertRange(envelope, 2, 100, 200);
    }

    private static Geodesic geodesic(final CoordinateReferenceSystem crs, final double... coordinates) {
        return GeometryFactory.DEFAULT.createGeodesic(GeometryFactory.DEFAULT.createDataPoints(crs, coordinates));
    }

    /**
     * Returns the highest latitude of points sampled along the geodesic between two points of WGS84.
     */
    private static double sampledMaximumLatitude(final double latitude1, final double longitude1,
                                                 final double latitude2, final double longitude2)
    {
        final GeodeticCalculator calculator = GeodeticCalculator.create(CRS_2D);
        calculator.setStartGeographicPoint(latitude1, longitude1);
        calculator.setEndGeographicPoint(latitude2, longitude2);
        final double azimuth  = calculator.getStartingAzimuth();
        final double distance = calculator.getGeodesicDistance();
        double maximum = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= 2000; i++) {
            calculator.setStartGeographicPoint(latitude1, longitude1);
            calculator.setStartingAzimuth(azimuth);
            calculator.setGeodesicDistance(distance * i / 2000);
            maximum = Math.max(maximum, calculator.getEndPoint().getCoordinate(1));
        }
        return maximum;
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
