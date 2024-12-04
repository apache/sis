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
package org.apache.sis.geometry.wrapper.jts;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.MultiLineString;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link Wrapper}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WrapperTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public WrapperTest() {
    }

    /**
     * Tests {@link Wrapper#getGeometryClass(GeometryType)}.
     */
    @Test
    public void testGetGeometryClass() {
        assertEquals(Point.class,           Wrapper.getGeometryClass(GeometryType.POINT));
        assertEquals(Polygon.class,         Wrapper.getGeometryClass(GeometryType.POLYGON));
        assertEquals(LineString.class,      Wrapper.getGeometryClass(GeometryType.LINESTRING));
        assertEquals(MultiPoint.class,      Wrapper.getGeometryClass(GeometryType.MULTIPOINT));
        assertEquals(MultiPolygon.class,    Wrapper.getGeometryClass(GeometryType.MULTIPOLYGON));
        assertEquals(MultiLineString.class, Wrapper.getGeometryClass(GeometryType.MULTILINESTRING));
        assertEquals(Geometry.class,        Wrapper.getGeometryClass(GeometryType.TRIANGLE));
    }

    /**
     * Tests {@link Wrapper#getGeometryType(Class)}.
     */
    @Test
    public void testGetGeometryType() {
        assertEquals(GeometryType.POINT,           Wrapper.getGeometryType(Point.class));
        assertEquals(GeometryType.POLYGON,         Wrapper.getGeometryType(Polygon.class));
        assertEquals(GeometryType.LINESTRING,      Wrapper.getGeometryType(LineString.class));
        assertEquals(GeometryType.MULTIPOINT,      Wrapper.getGeometryType(MultiPoint.class));
        assertEquals(GeometryType.MULTIPOLYGON,    Wrapper.getGeometryType(MultiPolygon.class));
        assertEquals(GeometryType.MULTILINESTRING, Wrapper.getGeometryType(MultiLineString.class));
        assertEquals(GeometryType.MULTIPOINT,      Wrapper.getGeometryType(Point[].class));
        assertEquals(GeometryType.MULTIPOLYGON,    Wrapper.getGeometryType(Polygon[].class));
        assertEquals(GeometryType.MULTILINESTRING, Wrapper.getGeometryType(LineString[].class));
        assertEquals(GeometryType.MULTIPOINT,      Wrapper.getGeometryType(MultiPoint[].class));
        assertEquals(GeometryType.MULTIPOLYGON,    Wrapper.getGeometryType(MultiPolygon[].class));
        assertEquals(GeometryType.MULTILINESTRING, Wrapper.getGeometryType(MultiLineString[].class));
    }
}
