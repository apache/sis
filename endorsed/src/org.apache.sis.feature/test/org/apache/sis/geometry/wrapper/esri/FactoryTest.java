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
package org.apache.sis.geometry.wrapper.esri;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.MultiPoint;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.util.StringBuilders;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.geometry.wrapper.GeometriesTestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;


/**
 * Tests {@link Factory} implementation for ESRI geometries.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class FactoryTest extends GeometriesTestCase {
    /**
     * Creates a new test case.
     */
    public FactoryTest() {
        super(Factory.INSTANCE);
    }

    /**
     * Tests {@link Factory#getGeometryClass(GeometryType)}.
     */
    @Test
    public void testGetGeometryClass() {
        assertEquals(Point.class,      factory.getGeometryClass(GeometryType.POINT));
        assertEquals(Polygon.class,    factory.getGeometryClass(GeometryType.POLYGON));
        assertEquals(Polyline.class,   factory.getGeometryClass(GeometryType.LINESTRING));
        assertEquals(MultiPoint.class, factory.getGeometryClass(GeometryType.MULTIPOINT));
        assertEquals(Polygon.class,    factory.getGeometryClass(GeometryType.MULTIPOLYGON));
        assertEquals(Polyline.class,   factory.getGeometryClass(GeometryType.MULTILINESTRING));
        assertEquals(Geometry.class,   factory.getGeometryClass(GeometryType.TRIANGLE));
    }

    /**
     * Tests {@link Factory#getGeometryType(Class)}.
     */
    @Test
    public void testGetGeometryType() {
        assertEquals(GeometryType.POINT,           factory.getGeometryType(Point.class));
        assertEquals(GeometryType.POLYGON,         factory.getGeometryType(Polygon.class));
        assertEquals(GeometryType.LINESTRING,      factory.getGeometryType(Polyline.class));
        assertEquals(GeometryType.MULTIPOINT,      factory.getGeometryType(MultiPoint.class));
        assertEquals(GeometryType.MULTIPOINT,      factory.getGeometryType(Point[].class));
        assertEquals(GeometryType.MULTIPOLYGON,    factory.getGeometryType(Polygon[].class));
        assertEquals(GeometryType.MULTILINESTRING, factory.getGeometryType(Polyline[].class));
        assertEquals(GeometryType.MULTIPOINT,      factory.getGeometryType(MultiPoint[].class));
    }

    /**
     * Tests {@link Factory#createPolyline(boolean, int, Vector...)}.
     */
    @Test
    @Override
    public void testCreatePolyline() {
        super.testCreatePolyline();
        final var poly = (Polyline) geometry;
        assertEquals(2, poly.getPathCount());
    }

    /**
     * Tests {@link Factory#mergePolylines(Iterator)} (or actually tests its strategy).
     */
    @Test
    @Override
    public void testMergePolylines() {
        super.testMergePolylines();
        final var poly = (Polyline) geometry;
        assertEquals(3, poly.getPathCount());
    }

    /**
     * Verifies that a WKT is equal to the expected one. This method modifies the expected WKT
     * by transforming single geometries into multi-geometries, since the ESRI library formats
     * geometries that way (at least with the objects that we use).
     */
    @Override
    protected void assertWktEquals(String expected, final String actual) {
        assertTrue(actual.startsWith("MULTI"));
        final var b = new StringBuilder(expected.length() + 7).append("MULTI").append(expected);
        StringBuilders.replace(b, "(", "((");
        StringBuilders.replace(b, ")", "))");
        expected = b.toString();
        super.assertWktEquals(expected, actual);
    }

    /**
     * Verifies that {@link Factory#getGeometry(GeometryWrapper)} does not allow mixing libraries.
     */
    @Test
    public void testGetGeometry() {
        final Geometries<?> other = org.apache.sis.geometry.wrapper.j2d.Factory.INSTANCE;
        final GeometryWrapper ogw = other.castOrWrap(other.createPoint(5, 6));
        assertNotNull(other.getGeometry(ogw));
        var e = assertThrows(ClassCastException.class, () -> factory.getGeometry(ogw));
        assertMessageContains(e, "ESRI", "Java2D");
    }
}
