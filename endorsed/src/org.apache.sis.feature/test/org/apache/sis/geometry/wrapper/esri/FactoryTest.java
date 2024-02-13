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

import com.esri.core.geometry.Polyline;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.geometry.wrapper.GeometriesTestCase;
import org.apache.sis.util.StringBuilders;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
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
     * Tests {@link Factory#createPolyline(boolean, int, Vector...)}.
     */
    @Test
    @Override
    public void testCreatePolyline() {
        super.testCreatePolyline();
        final Polyline poly = (Polyline) geometry;
        assertEquals(2, poly.getPathCount());
    }

    /**
     * Tests {@link Factory#mergePolylines(Iterator)} (or actually tests its strategy).
     */
    @Test
    @Override
    public void testMergePolylines() {
        super.testMergePolylines();
        final Polyline poly = (Polyline) geometry;
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
        final StringBuilder b = new StringBuilder(expected.length() + 7).append("MULTI").append(expected);
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
        assertMessageContains(e, "ESRI", "JAVA2D");
    }
}
