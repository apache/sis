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

import java.util.function.BiFunction;
import org.apache.sis.internal.feature.jts.JTS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;
import org.locationtech.jts.geom.Polygon;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.opengis.filter.Literal;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

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

    @Test
    public void testOptionalCrsInSTPoint() throws Exception {
        // Ensure that when argument array is of size 2, the FunctionWithSRID constructor will not fail with an ArrayIndexOutOfBoundsException.
        // This is important. This case has already happen, making it a regression test.
        assertPoint(null, (x, y) -> new Expression[] { x, y });
        // Ensure point function will correctly interpret a literal with a null value as "no crs available"
        assertPoint(null, (x, y) -> new Expression[] { x, y, factory.literal(null) });
        // Ensure CRS is fetched properly
        assertPoint(HardCodedCRS.WGS84, (x, y) -> new Expression[]{ x, y, factory.literal(HardCodedCRS.WGS84) });
    }

    /**
     * Verify that a point function properly build a point with expected CRS and coordinate.
     */
    private void assertPoint(CoordinateReferenceSystem expectedCrs, BiFunction<Expression<Feature, Double>, Expression<Feature, Double>, Expression[]> argumentBundler) throws FactoryException {
        final Literal<Feature, Double> x = factory.literal(1.0);
        final Literal<Feature, Double> y = factory.literal(2.0);
        Expression<Feature, ?> fn = factory.function("ST_Point", argumentBundler.apply(x, y));
        Object rawPoint = fn.apply(null);
        assertInstanceOf("ST_Point should create a Point geometry", Point.class, rawPoint);
        Point point = (Point) rawPoint;
        final CoordinateReferenceSystem pointCrs = JTS.getCoordinateReferenceSystem(point);
        if (expectedCrs == null) assertNull("Point CRS", pointCrs);
        else assertEquals("Point CRS", expectedCrs, pointCrs);
        assertEquals(point.getX(), x.getValue(), 1e-1);
        assertEquals(point.getY(), y.getValue(), 1e-1);
    }
}
