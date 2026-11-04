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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link JTSAdapter}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class JTSAdapterTest {

    private static final GeometryFactory GF = new GeometryFactory();

    /**
     * Tests that the CRS of a view over a JTS geometry can be relabeled,
     * without modifying the coordinates nor the JTS geometry.
     */
    @Test
    public void testRelabelView() {
        final org.locationtech.jts.geom.LineString jts = GF.createLineString(new Coordinate[] {
                new Coordinate(1, 2), new Coordinate(3, 4)});
        final Geometry view = JTSAdapter.fromJTS(jts, false);
        assertInstanceOf(LineString.class, view);
        assertTrue(Geometries.isUndefined(view.getCoordinateReferenceSystem()));

        view.setCoordinateReferenceSystem(CommonCRS.WGS84.normalizedGeographic());
        assertEquals(CommonCRS.WGS84.normalizedGeographic(), view.getCoordinateReferenceSystem());
        assertEquals(CommonCRS.WGS84.normalizedGeographic(),
                view.getDataPoints().getType().getAttributeSystem(DataPointsType.ATT_POSITION).getCoordinateReferenceSystem());
        assertArrayEquals(new double[] {1, 2, 3, 4},
                view.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble());
        assertNull(jts.getUserData());

        // The view still reflects the JTS coordinates.
        jts.getCoordinateSequence().setOrdinate(0, 0, 10);
        assertEquals(10, view.getDataPoints().getPosition(0).get(0));

        assertThrows(IllegalArgumentException.class,
                () -> view.setCoordinateReferenceSystem(CommonCRS.WGS84.geographic3D()));
    }
}
