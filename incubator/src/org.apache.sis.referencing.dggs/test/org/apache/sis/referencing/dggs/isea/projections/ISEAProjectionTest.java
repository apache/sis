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
package org.apache.sis.referencing.dggs.isea.projections;

import org.apache.sis.referencing.dggs.isea.generic.Trigonometric;
import org.apache.sis.referencing.dggs.isea.geometry.FaceCoordinates;
import org.apache.sis.referencing.dggs.isea.geometry.GeoCoordinates;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

/**
 *
 * @author Franz-Benjamin Mocnik
 */
public class ISEAProjectionTest {
    private final double _precision = 1e-9;
    private final int _iterations = 1000000;

    @Test
    public void projectionApartFromPoles() throws Exception {
        ISEAProjection p = new ISEAProjection();
        p.setOrientationSymmetricEquator();
        for (int i = 0; i < this._iterations; i++) {
            GeoCoordinates c = new GeoCoordinates(Math.random() * 179.99 - 89.995, Math.random() * 360);
            FaceCoordinates c2 = p.sphereToIcosahedron(c);
            GeoCoordinates c3 = p.icosahedronToSphere(c2);
            assertTrue(Math.abs(c3.getLat() - c.getLat()) < this._precision);
            assertTrue((Math.abs(c3.getLon() - c.getLon()) % 360) * Trigonometric.cos(c.getLat()) < this._precision);
        }
    }
}
