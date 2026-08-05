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
package org.apache.sis.geometries.math;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link Vector3D#setFromLatLon(double, double) } and
 * {@link Vector3D#toLatLon() }.
 *
 * @author Johann Sorel (Geomatys)
 */
public class Vector3DTest {

    private static final double TOLERANCE = 1e-12;
    private static final double HALF_SQRT2 = Math.sqrt(2) / 2;

    /**
     * setFromLatLon test on remarkable points : equator/meridian origin,
     * poles, and a quadrant point.
     */
    @Test
    public void setFromLatLonTest() {
        Vector3D.Double v = new Vector3D.Double();

        v.setFromLatLon(0, 0);
        assertArrayEquals(new double[]{1, 0, 0}, v.toArrayDouble(), TOLERANCE);

        v.setFromLatLon(0, Math.PI / 2);
        assertArrayEquals(new double[]{0, 1, 0}, v.toArrayDouble(), TOLERANCE);

        v.setFromLatLon(0, Math.PI);
        assertArrayEquals(new double[]{-1, 0, 0}, v.toArrayDouble(), TOLERANCE);

        v.setFromLatLon(Math.PI / 2, 0);
        assertArrayEquals(new double[]{0, 0, 1}, v.toArrayDouble(), TOLERANCE);

        v.setFromLatLon(-Math.PI / 2, 0);
        assertArrayEquals(new double[]{0, 0, -1}, v.toArrayDouble(), TOLERANCE);

        v.setFromLatLon(Math.PI / 4, Math.PI / 4);
        assertArrayEquals(new double[]{0.5, 0.5, HALF_SQRT2}, v.toArrayDouble(), TOLERANCE);
    }

    /**
     * toLatLon test on remarkable unit vectors : axis directions and a
     * quadrant point.
     */
    @Test
    public void toLatLonTest() {
        assertArrayEquals(new double[]{0, 0},
                new Vector3D.Double(1, 0, 0).toLatLon(), TOLERANCE);

        assertArrayEquals(new double[]{0, Math.PI / 2},
                new Vector3D.Double(0, 1, 0).toLatLon(), TOLERANCE);

        assertArrayEquals(new double[]{0, Math.PI},
                new Vector3D.Double(-1, 0, 0).toLatLon(), TOLERANCE);

        assertArrayEquals(new double[]{Math.PI / 2, 0},
                new Vector3D.Double(0, 0, 1).toLatLon(), TOLERANCE);

        assertArrayEquals(new double[]{-Math.PI / 2, 0},
                new Vector3D.Double(0, 0, -1).toLatLon(), TOLERANCE);

        assertArrayEquals(new double[]{Math.PI / 4, Math.PI / 4},
                new Vector3D.Double(0.5, 0.5, HALF_SQRT2).toLatLon(), TOLERANCE);
    }

    /**
     * Round trip test : converting latitude/longitude to a vector and back
     * must return the original angles, for a set of angles away from the
     * poles where longitude stays well defined.
     */
    @Test
    public void latLonRoundTripTest() {
        final double[] latitudes = {-Math.PI / 3, -Math.PI / 6, 0, Math.PI / 6, Math.PI / 3};
        final double[] longitudes = {-3 * Math.PI / 4, -Math.PI / 3, 0, Math.PI / 3, 3 * Math.PI / 4};

        final Vector3D.Double v = new Vector3D.Double();
        for (double lat : latitudes) {
            for (double lon : longitudes) {
                v.setFromLatLon(lat, lon);
                final double[] latLon = v.toLatLon();
                assertEquals(lat, latLon[0], TOLERANCE);
                assertEquals(lon, latLon[1], TOLERANCE);
            }
        }
    }

}
