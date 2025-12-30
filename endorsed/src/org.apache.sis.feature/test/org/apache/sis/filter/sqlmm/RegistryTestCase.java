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

import java.util.List;
import java.nio.DoubleBuffer;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.Optimization;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.filter.base.Node;
import org.apache.sis.geometry.wrapper.Dimensions;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.geometry.wrapper.GeometryWrapper;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.TestCaseWithLogs;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.filter.Expression;
import org.apache.sis.pending.geoapi.filter.Literal;


/**
 * {@link Registry} tests shared by all geometry libraries.
 * Subclasses must specify a geometry library such as JTS, ESRI or Java2D.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <G> root class of geometry implementation.
 */
public abstract class RegistryTestCase<G> extends TestCaseWithLogs {
    /**
     * Name of property value used in test feature instances.
     */
    private static final String P_NAME = "geom";

    /**
     * The factory to use for creating the objects to test.
     */
    private final DefaultFilterFactory<AbstractFeature, G, Object> factory;

    /**
     * The geometry library used by this factory.
     */
    private final Geometries<G> library;

    /**
     * Whether the geometry object can stores CRS information.
     */
    private final boolean supportCRS;

    /**
     * The geometry object used as an input for the function to test.
     * Usually an instance of {@code <G>} unless the geometry is a point.
     *
     * @see #createPolyline(boolean, int...)
     */
    private Object geometry;

    /**
     * The SQL/MM function being tested.
     *
     * @see #evaluate(String, Object...)
     */
    private Expression<AbstractFeature,?> function;

    /**
     * Tolerance threshold for assertions. Default value is 0.
     */
    private double tolerance;

    /**
     * Creates a new test case.
     *
     * @param  rootGeometry  the root geometry class as on of JTS, ESRI or Java2D root class.
     * @param  supportCRS    whether the geometry object can stores CRS information.
     */
    @SuppressWarnings("unchecked")
    protected RegistryTestCase(final Class<G> rootGeometry, final boolean supportCRS) {
        super(Node.LOGGER);
        factory = new DefaultFilterFactory.Features<>(rootGeometry, Object.class, WraparoundMethod.SPLIT);
        library = (Geometries<G>) Geometries.factory(rootGeometry);
        assertEquals(rootGeometry, library.rootClass);
        this.supportCRS = supportCRS;
    }

    /**
     * Verifies that attempts to create a function of the given name fail if no argument is provided.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertRequireArguments(final String functionName) {
        var e = assertThrows(IllegalArgumentException.class,
                () -> factory.function(functionName, new Expression[0]),
                "Creation with no argument shall fail.");
        assertMessageContains(e, "parameters");
    }

    /**
     * Creates a line string or polygon with the specified (x,y) tuples.
     * The geometry is stored in the {@link #geometry} field?
     */
    private void setGeometry(final boolean polygon, final double... coordinates) {
        geometry = library.createPolyline(polygon, false, Dimensions.XY, DoubleBuffer.wrap(coordinates));
    }

    /**
     * Sets the coordinate reference system on the current {@linkplain #geometry}.
     *
     * @param  crs  the coordinate reference system to assign to {@link #geometry}.
     */
    private void setGeometryCRS(final CoordinateReferenceSystem crs) {
        library.castOrWrap(geometry).setCoordinateReferenceSystem(crs);
    }

    /**
     * Creates a feature with a single property named {@value #P_NAME}.
     * The current {@link #geometry} value is assigned to that feature.
     *
     * @param  type  the type of value in the property.
     * @return a feature with a property of the given type.
     */
    private AbstractFeature createFeatureWithGeometry(final Class<?> type) {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.addAttribute(type).setName(P_NAME);
        final var mockType = ftb.setName("Test").build();
        final var feature = mockType.newInstance();
        feature.setPropertyValue(P_NAME, geometry);
        return feature;
    }

    /**
     * Creates a function where all parameters are literal values, then evaluate the function.
     * The function is stored in the {@link #function} field.
     *
     * @param  name    name of the function to create.
     * @param  values  literal values of all parameters.
     * @return function evaluation result.
     */
    private Object evaluate(final String name, final Object... values) {
        @SuppressWarnings({"unchecked","rawtypes"})
        final Expression<AbstractFeature,?>[] parameters = new Literal[values.length];
        for (int i=0; i<values.length; i++) {
            parameters[i] = factory.literal(values[i]);
        }
        function = factory.function(name, parameters);
        return function.apply(null);
    }

    /**
     * Verifies that the given result is a point with the given coordinates.
     *
     * @param  result  the result to verify.
     * @param  crs     expected coordinate reference system.
     * @param  x       expected first coordinate value.
     * @param  y       expected second coordinate value.
     */
    private void assertPointEquals(final Object result, final CoordinateReferenceSystem crs, double x, double y) {
        assertInstanceOf(library.pointClass, result);
        final GeometryWrapper wrapped = library.castOrWrap(result);
        if (supportCRS) {
            assertEquals(crs, wrapped.getCoordinateReferenceSystem());
        }
        assertArrayEquals(new double[] {x, y}, wrapped.getPointCoordinates(), tolerance);
    }

    /**
     * Verifies that the given result is an envelope or a polygon with the given envelope coordinates.
     *
     * @param  result     the result to verify.
     * @param  isPolygon  whether the result object is expected to be a polygon.
     * @param  crs        expected coordinate reference system.
     */
    private void assertEnvelopeEquals(final Object result, final boolean isPolygon,
            final CoordinateReferenceSystem crs, double xmin, double ymin, double xmax, double ymax)
    {
        final GeneralEnvelope env;
        if (isPolygon) {
            assertInstanceOf(library.getGeometryClass(GeometryType.POLYGON), result);
            final GeometryWrapper wrapped = library.castOrWrap(result);
            if (supportCRS) {
                assertEquals(crs, wrapped.getCoordinateReferenceSystem());
            }
            env = wrapped.getEnvelope();
        } else {
            env = (GeneralEnvelope) result;
        }
        if (supportCRS) {
            assertEquals(crs, env.getCoordinateReferenceSystem());
        }
        assertEquals(   2, env.getDimension());
        assertEquals(xmin, env.getLower(0), tolerance);
        assertEquals(xmax, env.getUpper(0), tolerance);
        assertEquals(ymin, env.getLower(1), tolerance);
        assertEquals(ymax, env.getUpper(1), tolerance);
    }

    /**
     * Verifies that the given result is a polyline with the given coordinates.
     *
     * @param  result       the result to verify.
     * @param  crs          expected coordinate reference system.
     * @param  coordinates  expected (x,y) tuples.
     */
    private void assertPolylineEquals(final Object result, final CoordinateReferenceSystem crs, final double... coordinates) {
        assertInstanceOf(library.getGeometryClass(GeometryType.LINESTRING), result);
        final GeometryWrapper wrapped = library.castOrWrap(result);
        if (supportCRS) {
            assertEquals(crs, wrapped.getCoordinateReferenceSystem());
        }
        assertArrayEquals(coordinates, wrapped.getAllCoordinates(), tolerance);
    }

    /**
     * Tests SQL/MM {@link ST_Transform} function.
     * The geometry to transform is obtained from a feature instance.
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
        geometry = library.createPoint(10, 30);
        setGeometryCRS(HardCodedCRS.WGS84);
        var instance = createFeatureWithGeometry(library.pointClass);
        /*
         * Test transform function using the full CRS object, then using only EPSG code.
         */
        final var reference = factory.property(P_NAME, library.pointClass);
        function = factory.function("ST_Transform", reference, factory.literal(HardCodedCRS.WGS84_LATITUDE_FIRST));
        assertPointEquals(function.apply(instance), HardCodedCRS.WGS84_LATITUDE_FIRST, 30, 10);

        function = factory.function("ST_Transform", reference, factory.literal("EPSG:4326"));
        assertPointEquals(function.apply(instance), CommonCRS.WGS84.geographic(), 30, 10);

        // Tested methods should not log.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests SQL/MM {@code ST_Buffer} function.
     * The geometry is specified as a literal, so there is no need to build feature instances.
     */
    @Test
    public void testBuffer() {
        assertRequireArguments("ST_Buffer");
        /*
         * Create a single point for testing the buffer function. The CRS is not used by this computation,
         * but we declare it in order to verify that the information is propagated to the result.
         */
        geometry = library.createPoint(10, 20);
        setGeometryCRS(HardCodedCRS.WGS84_LATITUDE_FIRST);
        /*
         * Execute the function and check the result.
         */
        tolerance = 1E-12;
        Object result = evaluate("ST_Buffer", geometry, 1);
        assertEnvelopeEquals(result, true, HardCodedCRS.WGS84_LATITUDE_FIRST, 9, 19, 11, 21);

        // Tested methods should not log.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Test SQL/MM {@code ST_Centroid} function.
     */
    @Test
    public void testCentroid() {
        assertRequireArguments("ST_Centroid");
        /*
         * Create a single linestring for testing the centroid function. The CRS is not used by this computation,
         * but we declare it in order to verify that the information is propagated to the result.
         */
        setGeometry(false, 10, 20, 30, 20);
        setGeometryCRS(HardCodedCRS.WGS84_LATITUDE_FIRST);
        /*
         * Execute the function and check the result.
         */
        final Object result = evaluate("ST_Centroid", geometry);
        assertPointEquals(result, HardCodedCRS.WGS84_LATITUDE_FIRST, 20, 20);

        // Tested methods should not log.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Test SQL/MM {@code ST_Envelope} function.
     */
    @Test
    public void testEnvelope() {
        assertRequireArguments("ST_Envelope");
        setGeometry(false, 12, 3.3,  13.1, 4.4,  12.02, 5.7);
        Object result = evaluate("ST_Envelope", geometry);
        assertEnvelopeEquals(result, false, null, 12, 3.3, 13.1, 5.7);

        // After testing literal data, try to extract data from a feature.
        final Class<?> polylineClass = library.getGeometryClass(GeometryType.LINESTRING);
        var instance = createFeatureWithGeometry(polylineClass);
        function = factory.function("ST_Envelope", factory.property(P_NAME, polylineClass));
        result = function.apply(instance);
        assertEnvelopeEquals(result, false, null, 12, 3.3, 13.1, 5.7);

        // Tested methods should not log.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Test SQL/MM {@code ST_Intersects} function.
     */
    @Test
    public void testIntersects() {
        assertRequireArguments("ST_Intersects");
        setGeometry(true, 0, 0.1,  1.2, 0.2,  0.7, 0.8,  0, 0.1);

        Object point = library.createPoint(2, 4);
        assertEquals(Boolean.FALSE, evaluate("ST_Intersects", point, geometry));

        // Border should intersect. Also use Feature instead of Literal as a source.
        final Class<?> polygonClass = library.getGeometryClass(GeometryType.POLYGON);
        final var instance = createFeatureWithGeometry(polygonClass);
        final var reference = factory.property(P_NAME, polygonClass);
        point = library.createPoint(0.2, 0.3);
        function = factory.function("ST_Intersects", reference, factory.literal(point));
        assertEquals(Boolean.TRUE, function.apply(instance));

        // Ensure switching argument order does not modify behavior.
        function = factory.function("ST_Intersects", factory.literal(point), reference);
        assertEquals(Boolean.TRUE, function.apply(instance));

        // Tested methods should not log.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Test SQL/MM {@code ST_Intersects} function with reprojection.
     * This test uses the same polygon as {@link #testIntersects()}.
     */
    @Test
    public void testIntersectsWithReprojection() {
        final GeographicCRS crs84 = CommonCRS.defaultGeographic();
        final ProjectedCRS crsUTM = CommonCRS.WGS84.universal(0, 0);

        // A point inside the polygon.
        geometry = library.createPoint(0.8, 0.3);
        setGeometryCRS(crs84);
        final Object point = geometry;

        // Same point projected to UTM.
        geometry = library.createPoint(255137.84, 33183.64);
        setGeometryCRS(crsUTM);
        final Object pointUTM = geometry;

        setGeometry(true, 0, 0.1,  1.2, 0.2,  0.7, 0.8,  0, 0.1);
        setGeometryCRS(crs84);

        // Test with geometries in same CRS. Order should not matter.
        assertEquals(Boolean.TRUE, evaluate("ST_Intersects", geometry, point));
        assertEquals(Boolean.TRUE, evaluate("ST_Intersects", point, geometry));

        // Test with geometries in different CRS. The CRS of the first geometry is used.
        assertEquals(Boolean.TRUE, evaluate("ST_Intersects", geometry, pointUTM));
        assertEquals(Boolean.TRUE, evaluate("ST_Intersects", pointUTM, geometry));

        // Test with a missing CRS information.
        setGeometryCRS(null);
        assertEquals(Boolean.TRUE, evaluate("ST_Intersects", geometry, point));

        // Tested methods should not log.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Test SQL/MM {@link ST_Point} function.
     */
    @Test
    public void testPoint() {
        assertRequireArguments("ST_Point");

        Object result = evaluate("ST_Point", 10, 20, HardCodedCRS.WGS84);
        assertPointEquals(result, HardCodedCRS.WGS84, 10, 20);

        result = evaluate("ST_Point", 10, 20, "CRS:84");
        assertPointEquals(result, CommonCRS.defaultGeographic(), 10, 20);

        // Tested methods should not log.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Test SQL/MM {@code ST_LineString} function.
     */
    @Test
    public void testLineString() {
        assertRequireArguments("ST_LineString");
        final Object result = evaluate("ST_LineString",
                List.of(library.createPoint(10, 20),
                        library.createPoint(30, 40)), HardCodedCRS.WGS84);
        assertPolylineEquals(result, HardCodedCRS.WGS84, 10, 20, 30, 40);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Test SQL/MM {@code ST_Simplify} function.
     */
    @Test
    public void testSimplify() {
        assertRequireArguments("ST_Simplify");
        /*
         * Creates a single line for testing the simplify function. The CRS is not used by this computation,
         * but we declare it in order to verify that the information is propagated to the result.
         */
        setGeometry(false, 10, 20,  15, 20,  20, 20);
        setGeometryCRS(HardCodedCRS.WGS84_LATITUDE_FIRST);
        /*
         * Execute the function and check the result.
         */
        final Object result = evaluate("ST_Simplify", geometry, 10);
        assertPolylineEquals(result, HardCodedCRS.WGS84_LATITUDE_FIRST, 10, 20, 20, 20);

        // Tested methods should not log.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Test SQL/MM {@code ST_SimplifyPreserveTopology} function.
     */
    @Test
    public void testSimplifyPreserveTopology() {
        assertRequireArguments("ST_SimplifyPreserveTopology");
        setGeometry(false, 10, 20,  15, 20,  20, 20);
        setGeometryCRS(HardCodedCRS.WGS84_LATITUDE_FIRST);
        final Object result = evaluate("ST_SimplifyPreserveTopology", geometry, 10);
        assertPolylineEquals(result, HardCodedCRS.WGS84_LATITUDE_FIRST, 10, 20, 20, 20);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link Optimization} on an arbitrary expression.
     * This method uses data tested by {@link #testTransform()}.
     */
    @Test
    public void testOptimization() {
        geometry = library.createPoint(10, 30);
        setGeometryCRS(HardCodedCRS.WGS84);
        function = factory.function("ST_Transform",
                factory.literal(geometry),
                factory.literal(HardCodedCRS.WGS84_LATITUDE_FIRST));
        assertPointEquals(function.apply(null), HardCodedCRS.WGS84_LATITUDE_FIRST, 30, 10);
        /*
         * Optimization should evaluate the point immediately.
         */
        final var optimized = new Optimization().apply(function);
        assertNotSame(function, optimized, "Optimization should produce a new expression.");
        var literal = assertInstanceOf(Literal.class, optimized, "Expected immediate expression evaluation.");
        assertPointEquals(literal.getValue(), HardCodedCRS.WGS84_LATITUDE_FIRST, 30, 10);

        // Tested methods should not log.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link Optimization} on an arbitrary expression on feature instances.
     */
    @Test
    public void testFeatureOptimization() {
        geometry = library.createPoint(10, 30);
        setGeometryCRS(HardCodedCRS.WGS84_LATITUDE_FIRST);
        function = factory.function("ST_Union", factory.property(P_NAME), factory.literal(geometry));

        final var optimization = new Optimization();
        final var ftb = new FeatureTypeBuilder();
        ftb.addAttribute(library.pointClass).setName(P_NAME).setCRS(HardCodedCRS.WGS84);
        optimization.setFinalFeatureType(ftb.setName("Test").build());
        final var optimized = optimization.apply(function);
        assertNotSame(function, optimized, "Optimization should produce a new expression.");
        /*
         * Get the second parameter, which should be a literal, and get the point coordinates.
         * Verify that the order is swapped compared to the order at the beginning of this method.
         */
        final Object literal = optimized.getParameters().get(1).apply(null);
        final DirectPosition point = library.castOrWrap(literal).getCentroid();
        assertArrayEquals(new double[] {30, 10}, point.getCoordinate());

        // Tested methods should not log.
        loggings.assertNoUnexpectedLog();
    }
}
