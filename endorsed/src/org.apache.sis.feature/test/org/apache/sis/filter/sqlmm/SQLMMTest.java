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
package org.apache.sis.filter.sqlmm;

import java.util.function.BiFunction;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.wrapper.jts.JTS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.filter.DefaultFilterFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;

// Specific to the main branch:
import org.apache.sis.filter.Expression;
import org.apache.sis.feature.AbstractFeature;


/**
 * Apply some validation on the {@link SQLMM} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public final class SQLMMTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final DefaultFilterFactory<AbstractFeature, ?, ?> factory;

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
            assertTrue(value.minParamCount   >= 0, name);
            assertTrue(value.minParamCount   <= value.maxParamCount, name);
            assertTrue(value.geometryCount() <= value.maxParamCount, name);
        }
    }

    /**
     * Tests {@code function(String, Expression, Expression)} for function {@code ST_GeomFromText}.
     */
    @Test
    public void testST_GeomFromText() {
        final String wkt = "POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))";
        final var expression = factory.function("ST_GeomFromText", factory.literal(wkt), factory.literal(4326));
        final Object value = expression.apply(null);
        assertInstanceOf(Polygon.class, value);
        final Polygon polygon = (Polygon) value;
        assertEquals(wkt, polygon.toText());
        assertEquals(CommonCRS.WGS84.geographic(), polygon.getUserData());
    }

    /**
     * Tests {@code function(String, Expression...)} where the last argument is an optional CRS.
     * The {@code ST_Point} function is used for this test.
     *
     * @throws FactoryException if an error occurred while fetching the CRS from a JTS geometry.
     */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})        // Because of generic array creation.
    public void testOptionalCrsInSTPoint() throws FactoryException {
        /*
         * Ensure that when argument array is of size 2, the `FunctionWithSRID`
         * constructor will not fail with an `ArrayIndexOutOfBoundsException`.
         * This bug happened in SIS 1.2.
         */
        verifyPoint(null, (x, y) -> new Expression[] { x, y });
        /*
         * Ensure that point function will correctly interpret
         * a literal with a null value as "no CRS available".
         */
        verifyPoint(null, (x, y) -> new Expression[] { x, y, factory.literal(null) });
        /*
         * Ensure that CRS is fetched properly.
         */
        verifyPoint(HardCodedCRS.WGS84, (x, y) -> new Expression[] { x, y, factory.literal(HardCodedCRS.WGS84) });
    }

    /**
     * Verifies that a point function properly build a point with expected CRS and coordinate.
     *
     * @param  expectedCRS      the CRS that should be found in the point created with {@code argumentBundler}.
     * @param  argumentBundler  given (x,y) coordinates, provides the list of arguments for {@code ST_Point(…)}.
     * @throws FactoryException if an error occurred while fetching the CRS from a JTS geometry.
     */
    private void verifyPoint(final CoordinateReferenceSystem expectedCRS,
                             final BiFunction<Expression<AbstractFeature, Double>,
                                              Expression<AbstractFeature, Double>,
                                              Expression<AbstractFeature, ?>[]> argumentBundler)
            throws FactoryException
    {
        final var x = factory.literal(1.0);
        final var y = factory.literal(2.0);
        var expression = factory.function("ST_Point", argumentBundler.apply(x, y));
        Object rawPoint = expression.apply(null);
        Point point = assertInstanceOf(Point.class, rawPoint);
        CoordinateReferenceSystem pointCRS = JTS.getCoordinateReferenceSystem(point);
        assertEquals(expectedCRS, pointCRS);
        assertEquals(point.getX(), x.apply(null));
        assertEquals(point.getY(), y.apply(null));
    }
}
