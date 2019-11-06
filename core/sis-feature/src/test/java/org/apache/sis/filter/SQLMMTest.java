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
package org.apache.sis.filter;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link SQLMM} functions implementations.
 * Current implementation tests Java Topology Suite (JTS) implementation only.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class SQLMMTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory factory;

    /**
     * The factory to use for creating Java Topology Suite (JTS) objects.
     */
    private final GeometryFactory geometryFactory;

    /**
     * Creates a new test case.
     */
    public SQLMMTest() {
        factory = new DefaultFilterFactory();
        geometryFactory = new GeometryFactory();
    }

    /**
     * Verifies that attempts to create a function of the given name fail if no argument is provided.
     */
    private void assertRequireArguments(final String functionName) {
        try {
            factory.function(functionName);
            fail("Creation with no argument should fail");
        } catch (IllegalArgumentException ex) {
            final String message = ex.getMessage();
            assertTrue(message, message.contains("parameters"));
        }
    }

    /**
     * Wraps the given geometry in a feature object. The geometry will be stored in a property named {@code "geom"}.
     *
     * @param  geometry  the geometry to wrap in a feature.
     * @param  crs       the coordinate reference system to assign to the geometry.
     */
    private static Feature wrapInFeature(final Geometry geometry, final CoordinateReferenceSystem crs) {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder().setName("test");
        ftb.addAttribute(Point.class).setName("geom").setCRS(crs);
        final FeatureType type = ftb.build();
        geometry.setUserData(crs);
        final Feature feature = type.newInstance();
        feature.setPropertyValue("geom", geometry);
        return feature;
    }

    /**
     * Evaluates the given function and returns its result as an object of the given type.
     *
     * @param  expectedType  the expected type of the result.
     * @param  feature       the feature to use as input value. May be {@code null}.
     * @param  testing       the function to test.
     * @return evaluation result.
     */
    private static <G extends Geometry> G evaluate(final Class<G> expectedType, final Feature feature, final Function testing) {
        final Object result = testing.evaluate(feature);
        assertInstanceOf("Expected JTS geometry.", expectedType, result);
        return expectedType.cast(result);
    }

    /**
     * Test SQL/MM {@link ST_Transform} function.
     */
    @Test
    public void testTransform() {
        /*
         * Verify that creation of a function without arguments is not allowed.
         */
        assertRequireArguments("ST_Transform");
        /*
         * Create a feature to be used for testing purpose. For this test, the CRS transformation
         * will be simply a change of axis order from (λ,φ) to (φ,λ).
         */
        final Point geometry = geometryFactory.createPoint(new Coordinate(10, 30));
        final Feature feature = wrapInFeature(geometry, HardCodedCRS.WGS84);
        /*
         * Test transform function using the full CRS object, then using only EPSG code.
         */
        testTransform(feature, HardCodedCRS.WGS84_φλ, HardCodedCRS.WGS84_φλ);
        testTransform(feature, "EPSG:4326", CommonCRS.WGS84.geographic());
    }

    /**
     * Tests {@link ST_Transform} on the given feature. The feature must have a property named {@code "geom"}.
     * The result is expected to be a geometry using WGS84 datum with (φ,λ) axis order.
     *
     * @param  feature       the feature to use for testing the function.
     * @param  specifiedCRS  the argument to give to the {@code "ST_Transform"} function.
     * @param  expectedCRS   the CRS expected as a result of the transform function.
     */
    private void testTransform(final Feature feature, final Object specifiedCRS, final CoordinateReferenceSystem expectedCRS) {
        final Point result = evaluate(Point.class, feature, factory.function("ST_Transform",
                factory.property("geom"), factory.literal(specifiedCRS)));
        assertEquals("userData", expectedCRS, result.getUserData());
        assertEquals(30, result.getX(), STRICT);
        assertEquals(10, result.getY(), STRICT);
    }

    /**
     * Test SQL/MM {@link ST_Centroid} function.
     */
    @Test
    public void testCentroid() {
        assertRequireArguments("ST_Centroid");
        /*
         * Creates a single linestring for testing the centroid function. The CRS is not used by this computation,
         * but we declare it in order to verify that the information is propagated to the result.
         */
        final LineString geometry = geometryFactory.createLineString(new Coordinate[] {
            new Coordinate(10, 20),
            new Coordinate(30, 20)
        });
        geometry.setSRID(4326);
        geometry.setUserData(HardCodedCRS.WGS84_φλ);
        /*
         * Execute the function and check the result.
         */
        final Point result = evaluate(Point.class, null, factory.function("ST_Centroid", factory.literal(geometry)));
        assertEquals("userData", HardCodedCRS.WGS84_φλ, result.getUserData());
        assertEquals("SRID", 4326, result.getSRID());
        assertEquals(20, result.getX(), STRICT);
        assertEquals(20, result.getY(), STRICT);
    }

    /**
     * Test SQL/MM {@link ST_Buffer} function.
     */
    @Test
    public void ST_BufferTest() {
        assertRequireArguments("ST_Buffer");
        /*
         * Creates a single point for testing the buffer function. The CRS is not used by this computation,
         * but we declare it in order to verify that the information is propagated to the result.
         */
        final Point geometry = geometryFactory.createPoint(new Coordinate(10, 20));
        geometry.setUserData(HardCodedCRS.WGS84_φλ);
        geometry.setSRID(4326);
        /*
         * Execute the function and check the result.
         */
        final Polygon result = evaluate(Polygon.class, null, factory.function("ST_Buffer", factory.literal(geometry), factory.literal(1)));
        assertEquals("userData", HardCodedCRS.WGS84_φλ, result.getUserData());
        assertEquals("SRID", 4326, result.getSRID());
        final Envelope env = result.getEnvelopeInternal();
        assertEquals( 9, env.getMinX(), STRICT);
        assertEquals(11, env.getMaxX(), STRICT);
        assertEquals(19, env.getMinY(), STRICT);
        assertEquals(21, env.getMaxY(), STRICT);
    }

    /**
     * Test SQL/MM {@link ST_Simplify} function.
     */
    @Test
    public void ST_SimplifyTest() {
        assertRequireArguments("ST_Simplify");
        /*
         * Creates a single line for testing the simplify function. The CRS is not used by this computation,
         * but we declare it in order to verify that the information is propagated to the result.
         */
        final LineString geometry = geometryFactory.createLineString(new Coordinate[]{new Coordinate(10, 20), new Coordinate(15, 20), new Coordinate(20, 20)});
        geometry.setUserData(HardCodedCRS.WGS84_φλ);
        geometry.setSRID(4326);
        /*
         * Execute the function and check the result.
         */
        final LineString result = evaluate(LineString.class, null, factory.function("ST_Simplify", factory.literal(geometry), factory.literal(10)));
        assertEquals("userData", HardCodedCRS.WGS84_φλ, result.getUserData());
        assertEquals("SRID", 4326, result.getSRID());
        Coordinate[] coordinates = result.getCoordinates();
        assertEquals(2, coordinates.length);
        assertEquals(10, coordinates[0].x, STRICT);
        assertEquals(20, coordinates[0].y, STRICT);
        assertEquals(20, coordinates[1].x, STRICT);
        assertEquals(20, coordinates[1].y, STRICT);
    }

    /**
     * Test SQL/MM {@link ST_SimplifyPreserveTopology} function.
     */
    @Test
    public void ST_SimplifyPreserveTopologyTest() {
        assertRequireArguments("ST_SimplifyPreserveTopology");
        /*
         * Creates a single line for testing the simplify function. The CRS is not used by this computation,
         * but we declare it in order to verify that the information is propagated to the result.
         */
        final LineString geometry = geometryFactory.createLineString(new Coordinate[]{new Coordinate(10, 20), new Coordinate(15, 20), new Coordinate(20, 20)});
        geometry.setUserData(HardCodedCRS.WGS84_φλ);
        geometry.setSRID(4326);
        /*
         * Execute the function and check the result.
         */
        final LineString result = evaluate(LineString.class, null, factory.function("ST_SimplifyPreserveTopology", factory.literal(geometry), factory.literal(10)));
        assertEquals("userData", HardCodedCRS.WGS84_φλ, result.getUserData());
        assertEquals("SRID", 4326, result.getSRID());
        Coordinate[] coordinates = result.getCoordinates();
        assertEquals(2, coordinates.length);
        assertEquals(10, coordinates[0].x, STRICT);
        assertEquals(20, coordinates[0].y, STRICT);
        assertEquals(20, coordinates[1].x, STRICT);
        assertEquals(20, coordinates[1].y, STRICT);
    }
}
