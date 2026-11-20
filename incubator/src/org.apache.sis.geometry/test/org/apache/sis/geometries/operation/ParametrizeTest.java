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
package org.apache.sis.geometries.operation;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Prism;
import org.apache.sis.geometries.Representation;
import org.apache.sis.geometries.curve.CircularString;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link GeometryProcessor#parametrize(Geometry)}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ParametrizeTest {

    private static final CoordinateReferenceSystem CRS2D = CommonCRS.WGS84.normalizedGeographic();

    private static final GeometryFactory F = GeometryFactory.DEFAULT;

    private static final NumberRange<Double> RANGE = NumberRange.create(0., true, 3., true);

    /**
     * Geometries which are not analytic are returned unchanged.
     */
    @Test
    public void testNotAnalytic() {
        final CircularString arc = F.createCircularString(F.createDataPoints(CRS2D, 0,0, 5,5, 10,0));
        assertSame(arc, arc.parametrize());
        final Polygon square = F.createPolygon(F.createLinearRing(F.createDataPoints(CRS2D, 0,0, 2,0, 2,2, 0,2, 0,0)), null);
        assertSame(square, square.parametrize());
    }

    /**
     * A prism is replaced by its extrusion, which is exact.
     */
    @Test
    public void testPrism() {
        final LineString line = F.createLineString(F.createDataPoints(CRS2D, 0,0, 2,0, 2,1));
        final Geometry walls = F.createPrism(line, RANGE, null).parametrize();
        assertInstanceOf(PolyhedralSurface.class, walls);
        assertEquals(Representation.DISCRETE, walls.getRepresentation());

        final Polygon square = F.createPolygon(F.createLinearRing(F.createDataPoints(CRS2D, 0,0, 2,0, 2,2, 0,2, 0,0)), null);
        assertInstanceOf(Polyhedron.class, F.createPrism(square, RANGE, null).parametrize());
    }

    /**
     * The elements of a collection are converted one by one.
     */
    @Test
    public void testCollection() {
        final Polygon square = F.createPolygon(F.createLinearRing(F.createDataPoints(CRS2D, 0,0, 2,0, 2,2, 0,2, 0,0)), null);
        final Prism prism = F.createPrism(square, RANGE, null);
        final GeometryCollection<Geometry> mixed = F.createGeometryCollection(prism, square);
        assertEquals(Representation.ANALYTIC, mixed.getRepresentation());
        final GeometryCollection<?> result = assertInstanceOf(GeometryCollection.class, mixed.parametrize());
        assertInstanceOf(Polyhedron.class, result.getGeometryN(0));
        assertSame(square, result.getGeometryN(1));
    }

    /**
     * A geometry without known exact parametric form causes an exception.
     */
    @Test
    public void testUnsupported() {
        final BBox box = new BBox(CRS2D, 0,0, 10,10);
        assertThrows(UnsupportedOperationException.class, box::parametrize);
    }
}
