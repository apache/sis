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
package org.apache.sis.cql;

import java.time.Instant;
import java.text.ParseException;
import java.time.LocalDate;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opengis.filter.Expression;
import org.opengis.feature.Feature;

// Test dependencies
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Test writing in CQL expressions.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class ExpressionWritingTest extends CQLTestCase {
    /**
     * Creates a new test case.
     */
    public ExpressionWritingTest() {
    }

    @Test
    public void testValueReference1() throws CQLException {
        final Expression<Feature,?> exp = FF.property("geom");
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("geom", cql);
    }

    @Test
    public void testValueReference2() throws CQLException {
        final Expression<Feature,?> exp = FF.property("the geom");
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("\"the geom\"", cql);
    }

    @Test
    public void testInteger() throws CQLException {
        final Expression<Feature,?> exp = FF.literal(15);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("15", cql);
    }

    @Test
    public void testNegativeInteger() throws CQLException {
        final Expression<Feature,?> exp = FF.literal(-15);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("-15", cql);
    }

    @Test
    public void testDecimal1() throws CQLException {
        final Expression<Feature,?> exp = FF.literal(3.14);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("3.14", cql);
    }

    @Test
    public void testDecimal2() throws CQLException {
        final Expression<Feature,?> exp = FF.literal(9.0E-21);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("9.0E-21", cql);
    }

    @Test
    @Disabled("Unsupported temporal field: Year")
    public void testDateTime() throws CQLException, ParseException{
        final Expression<Feature,?> exp = FF.literal(Instant.parse("2012-03-21T05:42:36Z"));
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("2012-03-21T05:42:36Z", cql);
    }

    @Test
    public void testDate() throws CQLException, ParseException{
        final Expression<Feature,?> exp = FF.literal(LocalDate.parse("2012-03-21"));
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("2012-03-21", cql);
    }


    @Test
    public void testNegativeDecimal() throws CQLException {
        final Expression<Feature,?> exp = FF.literal(-3.14);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("-3.14", cql);
    }

    @Test
    public void testText() throws CQLException {
        final Expression<Feature,?> exp = FF.literal("hello world");
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("'hello world'", cql);
    }

    @Test
    public void testAdd() throws CQLException {
        final Expression<Feature,?> exp = FF.add(FF.literal(3),FF.literal(2));
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("3 + 2", cql);
    }

    @Test
    public void testSubtract() throws CQLException {
        final Expression<Feature,?> exp = FF.subtract(FF.literal(3),FF.literal(2));
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("3 - 2", cql);
    }

    @Test
    public void testMultiply() throws CQLException {
        final Expression<Feature,?> exp = FF.multiply(FF.literal(3),FF.literal(2));
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("3 * 2", cql);
    }

    @Test
    public void testDivide() throws CQLException {
        final Expression<Feature,?> exp = FF.divide(FF.literal(3),FF.literal(2));
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("3 / 2", cql);
    }

    @Test
    @Disabled("Function `max` not yet supported.")
    public void testFunction1() throws CQLException {
        final Expression<Feature,?> exp = FF.function("max", FF.property("att"), FF.literal(15));
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("max(att , 15)", cql);
    }

    @Test
    @Disabled("Function `min` not yet supported.")
    public void testFunction2() throws CQLException {
        final Expression<Feature,?> exp = FF.function("min",FF.property("att"), FF.function("cos", FF.literal(3.14d)));
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("min(att , cos(3.14))", cql);
    }

    @Test
    public void testCombine1() throws CQLException {
        final Expression<Feature,?> exp =
                FF.divide(
                    FF.add(
                        FF.multiply(FF.literal(3), FF.literal(1)),
                        FF.subtract(FF.literal(2), FF.literal(6))
                        ),
                    FF.literal(4));
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("3 * 1 + 2 - 6 / 4", cql);
    }

    @Test
    public void testCombine2() throws CQLException {
        final Expression<Feature,?> exp =
                FF.add(
                        FF.multiply(FF.literal(3), FF.literal(1)),
                        FF.divide(FF.literal(2), FF.literal(4))
                        );
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("3 * 1 + 2 / 4", cql);

    }

    @Test
    @Disabled("Function `max` not yet supported.")
    public void testCombine3() throws CQLException {
        final Expression<Feature,?> exp =
                FF.add(
                        FF.multiply(
                            FF.literal(3),
                            FF.function("max", FF.property("val"), FF.literal(15)).toValueType(Number.class)
                        ),
                        FF.divide(FF.literal(2), FF.literal(4))
                        );
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("3 * max(val , 15) + 2 / 4", cql);
    }

    @Test
    public void testPoint() throws CQLException {
        final Geometry geom = GF.createPoint(new Coordinate(15, 30));
        final Expression<Feature,?> exp = FF.literal(geom);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("POINT (15 30)", cql);
    }

    @Test
    public void testMPoint() throws CQLException {
        final Geometry geom = GF.createMultiPoint(
                new Coordinate[]{
                    new Coordinate(15, 30),
                    new Coordinate(45, 60)
                });
        final Expression<Feature,?> exp = FF.literal(geom);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("MULTIPOINT ((15 30), (45 60))", cql);
    }

    @Test
    public void testLineString() throws CQLException {
        final Geometry geom = GF.createLineString(
                new Coordinate[]{
                    new Coordinate(10, 20),
                    new Coordinate(30, 40),
                    new Coordinate(50, 60)
                });
        final Expression<Feature,?> exp = FF.literal(geom);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("LINESTRING (10 20, 30 40, 50 60)", cql);
    }

    @Test
    public void testMLineString() throws CQLException {
        final Geometry geom = GF.createMultiLineString(
                new LineString[]{
                    GF.createLineString(
                        new Coordinate[]{
                            new Coordinate(10, 20),
                            new Coordinate(30, 40),
                            new Coordinate(50, 60)
                        }),
                    GF.createLineString(
                        new Coordinate[]{
                            new Coordinate(70, 80),
                            new Coordinate(90, 100),
                            new Coordinate(110, 120)
                        })
                    }
                );
        final Expression<Feature,?> exp = FF.literal(geom);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("MULTILINESTRING ((10 20, 30 40, 50 60), (70 80, 90 100, 110 120))", cql);
    }

    @Test
    public void testPolygon() throws CQLException {
        final Geometry geom = GF.createPolygon(
                GF.createLinearRing(
                    new Coordinate[]{
                        new Coordinate(10, 20),
                        new Coordinate(30, 40),
                        new Coordinate(50, 60),
                        new Coordinate(10, 20)
                    }),
                new LinearRing[]{
                    GF.createLinearRing(
                        new Coordinate[]{
                            new Coordinate(70, 80),
                            new Coordinate(90, 100),
                            new Coordinate(110, 120),
                            new Coordinate(70, 80)
                        })
                    }
                );
        final Expression<Feature,?> exp = FF.literal(geom);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("POLYGON ((10 20, 30 40, 50 60, 10 20), (70 80, 90 100, 110 120, 70 80))", cql);
    }

    @Test
    public void testMPolygon() throws CQLException {
        final Polygon geom1 = GF.createPolygon(
                GF.createLinearRing(
                    new Coordinate[]{
                        new Coordinate(10, 20),
                        new Coordinate(30, 40),
                        new Coordinate(50, 60),
                        new Coordinate(10, 20)
                    }),
                new LinearRing[]{
                    GF.createLinearRing(
                        new Coordinate[]{
                            new Coordinate(70, 80),
                            new Coordinate(90, 100),
                            new Coordinate(110, 120),
                            new Coordinate(70, 80)
                        })
                    }
                );
        final Polygon geom2 = GF.createPolygon(
                GF.createLinearRing(
                    new Coordinate[]{
                        new Coordinate(11, 21),
                        new Coordinate(31, 41),
                        new Coordinate(51, 61),
                        new Coordinate(11, 21)
                    }),
                new LinearRing[]{
                    GF.createLinearRing(
                        new Coordinate[]{
                            new Coordinate(71, 81),
                            new Coordinate(91, 101),
                            new Coordinate(111, 121),
                            new Coordinate(71, 81)
                        })
                    }
                );
        final Geometry geom = GF.createMultiPolygon(new Polygon[]{geom1,geom2});
        final Expression<Feature,?> exp = FF.literal(geom);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("MULTIPOLYGON (((10 20, 30 40, 50 60, 10 20), (70 80, 90 100, 110 120, 70 80)), ((11 21, 31 41, 51 61, 11 21), (71 81, 91 101, 111 121, 71 81)))", cql);
    }
}
