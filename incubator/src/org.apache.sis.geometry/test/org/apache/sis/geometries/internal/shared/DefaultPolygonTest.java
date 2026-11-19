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
package org.apache.sis.geometries.internal.shared;

import java.util.ArrayList;
import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.surface.PolygonTest;


/**
 * Tests {@link DefaultPolygon}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultPolygonTest extends PolygonTest {

    @Override
    protected Polygon createPolygon(final CoordinateReferenceSystem crs,
                                    final double[] exterior, final double[]... holes)
    {
        final List<LinearRing> interiors = new ArrayList<>(holes.length);
        for (final double[] hole : holes) {
            interiors.add(new DefaultLinearRing(sequence(crs, hole)));
        }
        return new DefaultPolygon(new DefaultLinearRing(sequence(crs, exterior)), interiors);
    }

    /**
     * Tests {@code equals} and {@code hashCode} of polygons and of their rings.
     */
    @Test
    public void testEquals() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();
        final double[] exterior = {0,0, 10,0, 10,10, 0,10, 0,0};
        final double[] hole     = {2,2, 3,2, 3,3, 2,2};
        final Polygon polygon = createPolygon(crs, exterior, hole);
        final Polygon same    = createPolygon(crs, exterior, hole);
        assertNotSame(polygon, same);
        assertEquals(polygon, same);
        assertEquals(polygon.hashCode(), same.hashCode());
        assertEquals(polygon.getExteriorRing(), same.getExteriorRing());
        assertNotEquals(polygon, createPolygon(crs, exterior));
        assertNotEquals(new DefaultLineString(sequence(crs, hole)), new DefaultLinearRing(sequence(crs, hole)),
                        "A line string is not equal to a linear ring.");
    }

    /**
     * Creates the positions of a geometry from a flat list of ordinate values.
     */
    private static DataPoints sequence(final CoordinateReferenceSystem crs, final double... coordinates) {
        return new ArrayDataPoints(NDArrays.of(SampleSystem.of(crs), coordinates));
    }
}
