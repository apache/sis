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
package org.apache.sis.internal.filter.sqlmm;

import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;
import org.locationtech.jts.geom.Polygon;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Apply some validation on the {@link SQLMM} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public final strictfp class SQLMMTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory<Feature,Object,?> factory;

    /**
     * Creates a new test case.
     */
    public SQLMMTest() {
        factory = DefaultFilterFactory.forFeatures();
    }

    /**
     * Verifies the consistency of parameter count.
     */
    @Test
    public void verifyParameterCount() {
        for (final SQLMM value : SQLMM.values()) {
            final String name = value.name();
            assertTrue(name, value.minParamCount   >= 0);
            assertTrue(name, value.minParamCount   <= value.maxParamCount);
            assertTrue(name, value.geometryCount() <= value.maxParamCount);
        }
    }

    /**
     * Tests {@link FilterFactory#function(String, Expression, Expression)}
     * for function {@code ST_GeomFromText}.
     */
    @Test
    public void testST_GeomFromText() {
        final String wkt = "POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))";
        final Expression<Feature,?> exp = factory.function("ST_GeomFromText", factory.literal(wkt), factory.literal(4326));
        final Object value = exp.apply(null);
        assertInstanceOf("Expected JTS implementation.", Polygon.class, value);
        final Polygon polygon = (Polygon) value;
        assertEquals(wkt, polygon.toText());
        assertEquals(CommonCRS.WGS84.geographic(), polygon.getUserData());
    }
}
