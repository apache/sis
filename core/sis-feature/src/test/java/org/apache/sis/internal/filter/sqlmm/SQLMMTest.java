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

import java.util.Arrays;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;

import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.feature.jts.JTS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.TestCase;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opengis.test.Assert.assertEquals;
import static org.opengis.test.Assert.assertInstanceOf;


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

    private static final String P_NAME = "geom";

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
        final org.locationtech.jts.geom.Envelope env = result.getEnvelopeInternal();
        assertEquals( 9, env.getMinX(), STRICT);
        assertEquals(11, env.getMaxX(), STRICT);
        assertEquals(19, env.getMinY(), STRICT);
        assertEquals(21, env.getMaxY(), STRICT);
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

    @Test
    public void ST_Envelope() {
        try {
            new ST_Envelope(new Expression[2]);
            fail("ST_Envelope operator should accept a single parameter");
        } catch (IllegalArgumentException e) {
            // expected behavior
        }

        try {
            new ST_Envelope(null);
            fail("ST_Envelope operator should accept a single parameter");
        } catch (IllegalArgumentException e) {
            // expected behavior
        }

        final LineString pt = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(12, 3.3),
                new Coordinate(13.1, 4.4),
                new Coordinate(12.02, 5.7)
        });
        ST_Envelope operator = new ST_Envelope(new Expression[]{factory.literal(pt)});
        final GeneralEnvelope expectedEnv = new GeneralEnvelope(2);
        expectedEnv.setEnvelope(12, 3.3, 13.1, 5.7);
        Envelope evaluated = (Envelope) operator.evaluate(null);
        assertTrue(String.format("Bad result:%nExpected: %s%nBut got: %s", expectedEnv.toString(), evaluated.toString()), expectedEnv.equals(evaluated, 1e-10, false));
        evaluated = (Envelope) operator.evaluate(null);
        assertTrue(String.format("Bad result:%nExpected: %s%nBut got: %s", expectedEnv.toString(), evaluated.toString()), expectedEnv.equals(evaluated, 1e-10, false));

        // After testing literal data, we'll now try to extract data from a feature.
        final Feature f = mock();
        f.setPropertyValue(P_NAME, pt);
        operator = new ST_Envelope(new Expression[]{factory.property(P_NAME)});
        evaluated = (Envelope) operator.evaluate(f);
        assertTrue(String.format("Bad result:%nExpected: %s%nBut got: %s", expectedEnv.toString(), evaluated.toString()), expectedEnv.equals(evaluated, 1e-10, false));
    }

    @Test
    public void ST_Intersects() throws Exception {

        final Coordinate start = new Coordinate(0, 0.1);
        final Coordinate second = new Coordinate(1.2, 0.2);
        final Polygon ring = geometryFactory.createPolygon(new Coordinate[]{
                start, second, new Coordinate(0.7, 0.8), start
        });

        final Literal lring = factory.literal(ring);
        ST_Intersects st = new ST_Intersects(factory.literal(geometryFactory.createPoint(new Coordinate(2, 4))), lring);
        // Ensure argument nullity does not modify behavior
        assertFalse("Unexpected intersection", (Boolean) st.evaluate(null));
        assertFalse("Unexpected intersection", (Boolean) st.evaluate(new Object()));

        // Border should intersect
        final Feature f = mock();
        Point point = geometryFactory.createPoint(second);
        f.setPropertyValue(P_NAME, point);
        final PropertyName geomName = factory.property(P_NAME);
        st = new ST_Intersects(geomName, lring);
        assertTrue("Border point should intersect triangle", (Boolean) st.evaluate(f));
        // Ensure inverting expression does not modify behavior.
        st = new ST_Intersects(lring, geomName);
        assertTrue("Border point should intersect triangle", (Boolean) st.evaluate(f));

        // Ensure CRS conversion works as expected (see package-info).
        // Missing
        final GeographicCRS crs84 = CommonCRS.defaultGeographic();
        point.setUserData(crs84);
        try {
            new ST_Intersects(geomName, lring).evaluate(f);
            fail("Should have throw an exception.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
        }
        final Literal lPoint = factory.literal(point);
        try {
            new ST_Intersects(lPoint, lring).evaluate(null);
            fail("Should have throw an exception.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
        }
        // utm domain contained in CRS:84
        final ProjectedCRS nadUtm = CommonCRS.NAD27.universal(32, 37);
        // slightly move point, because edge test is likely to fail due to transform roundings.
        point = geometryFactory.createPoint(new Coordinate(0.8, 0.3));
        final Geometry nadPoint = JTS.transform(point, CRS.findOperation(crs84, nadUtm, null));
        f.setPropertyValue(P_NAME, nadPoint);
        ring.setUserData(crs84);
        assertTrue("Intersection should be found when CRS are compatible", (Boolean) new ST_Intersects(geomName, lring).evaluate(f));
        assertTrue("Intersection should be found when CRS are compatible", (Boolean) new ST_Intersects(factory.literal(nadPoint), lring).evaluate(null));

        // Common base CRS
        final ProjectedCRS utm00 = CommonCRS.WGS84.universal(0, 0);
        final Geometry ringUtm00 = JTS.transform(ring, CRS.findOperation(crs84, utm00, null));
        final ProjectedCRS utm78 = CommonCRS.WGS84.universal(7, 8);
        final Geometry pointUtm78 = JTS.transform(ring, CRS.findOperation(crs84, utm78, null));
        f.setPropertyValue(P_NAME, pointUtm78);
        assertTrue("Intersection should be found when CRS are compatible", (Boolean) new ST_Intersects(geomName, factory.literal(ringUtm00)).evaluate(f));
        assertTrue("Intersection should be found when CRS are compatible", (Boolean) new ST_Intersects(factory.literal(pointUtm78), factory.literal(ringUtm00)).evaluate(null));
    }

    /**
     * Test SQL/MM {@link ST_Point} function.
     */
    @Test
    public void ST_PointTest() {
        assertRequireArguments("ST_Point");
        /*
         * Execute the function and check the result.
         */
        final Point result = evaluate(Point.class, null, factory.function("ST_Point",
                factory.literal(10.0),
                factory.literal(20.0),
                factory.literal(HardCodedCRS.WGS84)));
        assertEquals("userData", HardCodedCRS.WGS84, result.getUserData());
        assertEquals(10.0, result.getX(), STRICT);
        assertEquals(20.0, result.getY(), STRICT);
    }

    /**
     * Test SQL/MM {@link ST_Point} function.
     */
    @Test
    public void ST_LineStringTest() {
        assertRequireArguments("ST_LineString");
        /*
         * Execute the function and check the result.
         */
        final LineString result = evaluate(LineString.class, null, factory.function("ST_LineString",
                factory.literal(Arrays.asList(new Coordinate(10.0, 20.0), new Coordinate(30.0, 40.0))),
                factory.literal(HardCodedCRS.WGS84)));
        assertEquals("userData", HardCodedCRS.WGS84, result.getUserData());
        assertEquals(10.0, result.getCoordinates()[0].x, STRICT);
        assertEquals(20.0, result.getCoordinates()[0].y, STRICT);
        assertEquals(30.0, result.getCoordinates()[1].x, STRICT);
        assertEquals(40.0, result.getCoordinates()[1].y, STRICT);
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

    /**
     *
     * @return A feature with a single property of {@link Object any} type named after {@link #P_NAME this constant}.
     */
    private static Feature mock() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder().setName("mock");
        ftb.addAttribute(Object.class).setName(P_NAME);
        final FeatureType mockType = ftb.build();
        return mockType.newInstance();
    }
}
