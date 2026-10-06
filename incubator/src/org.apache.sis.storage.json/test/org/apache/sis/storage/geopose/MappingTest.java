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
package org.apache.sis.storage.geopose;

import java.time.Instant;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.crs.DefaultDerivedCRS;
import org.apache.sis.storage.geopose.binding.FrameSpecification;
import org.apache.sis.storage.geopose.binding.Position;
import org.apache.sis.storage.geopose.binding.Quaternion;
import org.apache.sis.storage.geopose.binding.YawPitchRoll;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the mapping between GeoPose bindings and Apache SIS types.
 *
 * @author Johann Sorel (Geomatys)
 */
public class MappingTest {

    /**
     * Tolerance for angles compared in decimal degrees.
     */
    private static final double TOLERANCE = 1E-9;

    private final GeoPoseMapper mapper = new GeoPoseMapper();

    public MappingTest() {
    }

    /**
     * Verifies that a position becomes a three-dimensional direct position in EPSG::4979,
     * and that the conversion back restores the original values.
     */
    @Test
    public void testPosition() {
        final Position source = new Position(43.6047, 1.4442, 150.0);
        final GeneralDirectPosition position = mapper.toDirectPosition(source);
        assertEquals(3, position.getDimension());
        assertEquals(43.6047, position.getCoordinate(0), TOLERANCE);
        assertEquals( 1.4442, position.getCoordinate(1), TOLERANCE);
        assertEquals(  150.0, position.getCoordinate(2), TOLERANCE);
        assertNotNull(position.getCoordinateReferenceSystem());
        assertEquals(source, mapper.toPosition(position));
    }

    /**
     * Verifies that a position of the wrong dimension is rejected.
     */
    @Test
    public void testPositionWrongDimension() {
        final GeneralDirectPosition position = new GeneralDirectPosition(1.0, 2.0);
        assertThrows(IllegalArgumentException.class, () -> mapper.toPosition(position));
    }

    /**
     * Verifies that a GeoPose quaternion maps to an equal quaternion supporting arithmetic.
     */
    @Test
    public void testQuaternion() {
        final Quaternion source = new Quaternion(0.0, 0.0, 0.7071067811865476, 0.7071067811865476);
        final org.apache.sis.maths.Quaternion orientation = mapper.toOrientation(source);
        assertEquals(1.0, orientation.norm(), TOLERANCE);
        assertEquals(source, mapper.toQuaternion(orientation));
    }

    /**
     * Verifies that yaw, pitch and roll angles survive a conversion to a quaternion and back.
     */
    @Test
    public void testYawPitchRollRoundTrip() {
        final double[][] cases = {
            {  0.0,   0.0,   0.0},
            { 90.0,   0.0,   0.0},
            {  0.0,  45.0,   0.0},
            {  0.0,   0.0,  30.0},
            {-120.0, -35.0, 170.0}
        };
        for (final double[] c : cases) {
            final YawPitchRoll source = new YawPitchRoll(c[0], c[1], c[2]);
            final YawPitchRoll result = mapper.toYawPitchRoll(mapper.toOrientation(source));
            assertEquals(source.getYaw(),   result.getYaw(),   1E-6, "yaw of "   + source);
            assertEquals(source.getPitch(), result.getPitch(), 1E-6, "pitch of " + source);
            assertEquals(source.getRoll(),  result.getRoll(),  1E-6, "roll of "  + source);
        }
    }

    /**
     * Verifies the gimbal lock case, where a pitch of 90 degrees makes yaw and roll
     * indistinguishable. Only the orientation itself can be compared, not the angles.
     */
    @Test
    public void testYawPitchRollGimbalLock() {
        final YawPitchRoll source = new YawPitchRoll(0.0, 90.0, 0.0);
        final org.apache.sis.maths.Quaternion orientation = mapper.toOrientation(source);
        assertEquals(1.0, orientation.norm(), 1E-9);

        final YawPitchRoll result = mapper.toYawPitchRoll(orientation);
        assertEquals(90.0, Math.abs(result.getPitch()), 1E-6);
    }

    /**
     * Verifies the conversion between GeoPose times and java.time instants.
     */
    @Test
    public void testValidTime() {
        final Instant instant = mapper.toInstant(1630560671227L);
        assertEquals(Instant.ofEpochMilli(1630560671227L), instant);
        assertEquals(1630560671227L, mapper.toValidTime(instant).longValue());
        assertNull(mapper.toInstant(null));
        assertNull(mapper.toValidTime(null));
    }

    /**
     * Verifies that a GeoPose frame specification is parsed into its origin.
     */
    @Test
    public void testFrameSpecificationParser() {
        final FrameSpecification specification = new FrameSpecification("/geopose/1.0", "LTP-ENU",
                "longitude=1.4442&latitude=43.6047&height=150.000");
        final LocalTangentPlane plane = new FrameSpecificationParser().parse(specification);
        assertEquals(43.6047, plane.getLatitude(),  TOLERANCE);
        assertEquals( 1.4442, plane.getLongitude(), TOLERANCE);
        assertEquals(  150.0, plane.getHeight(),    TOLERANCE);
    }

    /**
     * Verifies that an unsupported authority or a missing key is rejected.
     */
    @Test
    public void testFrameSpecificationParserErrors() {
        final FrameSpecificationParser parser = new FrameSpecificationParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                new FrameSpecification("/other/1.0", "LTP-ENU", "longitude=0.0&latitude=0.0&height=0.0")));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                new FrameSpecification("/geopose/1.0", "OTHER", "longitude=0.0&latitude=0.0&height=0.0")));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                new FrameSpecification("/geopose/1.0", "LTP-ENU", "longitude=0.0&latitude=0.0")));
    }

    /**
     * Verifies that the local tangent plane builds an east-north-up system over EPSG::4979
     * using the EPSG::9837 conversion method.
     */
    @Test
    public void testLocalTangentPlaneCRS() throws Exception {
        final LocalTangentPlane plane = new LocalTangentPlane(43.6047, 1.4442, 150.0);
        final DefaultDerivedCRS crs = plane.toCoordinateReferenceSystem();

        final CoordinateSystem cs = crs.getCoordinateSystem();
        assertEquals(3, cs.getDimension());
        assertEquals(AxisDirection.EAST,  cs.getAxis(0).getDirection());
        assertEquals(AxisDirection.NORTH, cs.getAxis(1).getDirection());
        assertEquals(AxisDirection.UP,    cs.getAxis(2).getDirection());
        assertEquals(Units.METRE, cs.getAxis(0).getUnit());

        final Conversion conversion = crs.getConversionFromBase();
        assertEquals("Geographic/topocentric conversions", conversion.getMethod().getName().getCode());
        assertEquals(43.6047, conversion.getParameterValues()
                .parameter("Latitude of topocentric origin").doubleValue(Units.DEGREE), TOLERANCE);
        assertEquals(1.4442, conversion.getParameterValues()
                .parameter("Longitude of topocentric origin").doubleValue(Units.DEGREE), TOLERANCE);
        assertEquals(150.0, conversion.getParameterValues()
                .parameter("Ellipsoidal height of topocentric origin").doubleValue(Units.METRE), TOLERANCE);
    }
}
