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
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.apache.sis.internal.feature.jts.JTS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;

// Branch-dependent imports
import org.opengis.filter.Literal;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;


/**
 * Apply some validation on the {@link SQLMM} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.3
 * @since   1.1
 */
public final class SQLMMTest extends TestCase {
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

    /**
     * Tests {@link FilterFactory#function(String, Expression...)} where the last argument
     * is an optional CRS. The {@code ST_Point} function is used for this test.
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
                             final BiFunction<Expression<Feature, Double>,
                                              Expression<Feature, Double>,
                                              Expression<Feature, ?>[]> argumentBundler)
            throws FactoryException
    {
        final Literal<Feature, Double> x = factory.literal(1.0);
        final Literal<Feature, Double> y = factory.literal(2.0);
        Expression<Feature, ?> fn = factory.function("ST_Point", argumentBundler.apply(x, y));
        Object rawPoint = fn.apply(null);
        assertInstanceOf("ST_Point should create a Point geometry", Point.class, rawPoint);
        Point point = (Point) rawPoint;
        CoordinateReferenceSystem pointCRS = JTS.getCoordinateReferenceSystem(point);
        assertEquals("Point CRS", expectedCRS, pointCRS);
        assertEquals(point.getX(), x.getValue(), STRICT);
        assertEquals(point.getY(), y.getValue(), STRICT);
    }
}
