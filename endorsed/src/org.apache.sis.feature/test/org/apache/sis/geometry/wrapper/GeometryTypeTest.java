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
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link GeometryType}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
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
        assertEquals(org.locationtech.jts.geom.LineString.class, name.toJavaType().get());

        name = GeometryType.LINESTRING.getTypeName(org.apache.sis.geometry.wrapper.esri.Factory.INSTANCE);
        assertEquals("OGC:LineString", name.toFullyQualifiedName().toString());
        assertEquals(com.esri.core.geometry.Polyline.class, name.toJavaType().get());
    }

    /**
     * Tests {@link GeometryType#forBinaryType(int)} and verifies {@link GeometryType#binaryType()} values.
     */
    @Test
    public void testBinaryType() {
        for (final GeometryType type : GeometryType.values()) {
            assertSame(type.name(), type, GeometryType.forBinaryType(type.binaryType()));
        }
    }

    /**
     * Tests {@link GeometryType#forName(String)}.
     */
    @Test
    public void testForName() {
        assertSame(GeometryType.MULTIPOLYGON, GeometryType.forName("multi_Polygon"));
        assertSame(GeometryType.MULTIPOLYGON, GeometryType.forName("MULTIPOLYGON"));
        assertSame(GeometryType.GEOMETRYCOLLECTION, GeometryType.forName("GEOMETRY_COLLECTION"));
        assertSame(GeometryType.GEOMETRYCOLLECTION, GeometryType.forName("GeomCollection"));
    }
}
