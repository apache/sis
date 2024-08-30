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
package org.apache.sis.geometry.wrapper;

import java.util.Locale;
import org.opengis.util.TypeName;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link GeometryType}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GeometryTypeTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public GeometryTypeTest() {
    }

    /**
     * Verifies {@link GeometryType#name} values.
     */
    @Test
    public void verifyCamelCaseName() {
        for (final GeometryType type : GeometryType.values()) {
            assertEquals(type.name(), type.name.toUpperCase(Locale.US));
        }
    }

    /**
     * Tests {@link GeometryType#getTypeName(Geometries)}.
     */
    @Test
    public void testTypeName() {
        TypeName name = GeometryType.LINESTRING.getTypeName(org.apache.sis.geometry.wrapper.jts.Factory.INSTANCE);
        assertEquals("OGC:LineString", name.toFullyQualifiedName().toString());

        name = GeometryType.LINESTRING.getTypeName(org.apache.sis.geometry.wrapper.esri.Factory.INSTANCE);
        assertEquals("OGC:LineString", name.toFullyQualifiedName().toString());
    }

    /**
     * Tests {@link GeometryType#forBinaryType(int)}.
     */
    @Test
    public void testForBinaryType() {
        assertSame(GeometryType.GEOMETRY,           GeometryType.forBinaryType( 0));
        assertSame(GeometryType.POINT,              GeometryType.forBinaryType( 1));
        assertSame(GeometryType.LINESTRING,         GeometryType.forBinaryType( 2));
        assertSame(GeometryType.POLYGON,            GeometryType.forBinaryType( 3));
        assertSame(GeometryType.MULTIPOINT,         GeometryType.forBinaryType( 4));
        assertSame(GeometryType.MULTILINESTRING,    GeometryType.forBinaryType( 5));
        assertSame(GeometryType.MULTIPOLYGON,       GeometryType.forBinaryType( 6));
        assertSame(GeometryType.GEOMETRYCOLLECTION, GeometryType.forBinaryType( 7));
        assertSame(GeometryType.CURVE,              GeometryType.forBinaryType(13));
        assertSame(GeometryType.SURFACE,            GeometryType.forBinaryType(14));
        assertSame(GeometryType.POLYHEDRALSURFACE,  GeometryType.forBinaryType(15));
        assertSame(GeometryType.TRIANGLE,           GeometryType.forBinaryType(17));
        assertSame(GeometryType.GEODESICSTRING,     GeometryType.forBinaryType(19));
        assertSame(GeometryType.BREPSOLID,          GeometryType.forBinaryType(25));
    }

    /**
     * Tests {@link GeometryType#forName(String)}.
     */
    @Test
    public void testForName() {
        assertSame(GeometryType.MULTIPOLYGON,       GeometryType.forName("multi_Polygon"));
        assertSame(GeometryType.MULTIPOLYGON,       GeometryType.forName("MULTIPOLYGON"));
        assertSame(GeometryType.GEOMETRYCOLLECTION, GeometryType.forName("GEOMETRY_COLLECTION"));
        assertSame(GeometryType.GEOMETRYCOLLECTION, GeometryType.forName("GeomCollection"));
    }
}
