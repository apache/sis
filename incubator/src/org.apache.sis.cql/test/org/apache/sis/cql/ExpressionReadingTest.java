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
import java.time.Duration;
import java.time.Period;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;

// Test dependencies
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Test reading CQL expressions.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class ExpressionReadingTest extends CQLTestCase {
    /**
     * Creates a new test case.
     */
    public ExpressionReadingTest() {
    }

    @Test
    public void testValueReference1() throws CQLException {
        final String cql = "geom";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof ValueReference);
        final ValueReference expression = (ValueReference) obj;
        assertEquals("geom", expression.getXPath());
    }

    @Test
    public void testValueReference2() throws CQLException {
        final String cql = "\"geom\"";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof ValueReference);
        final ValueReference expression = (ValueReference) obj;
        assertEquals("geom", expression.getXPath());
    }

    @Test
    public void testValueReference3() throws CQLException {
        final String cql = "ùthe_$uglY^_pr@perté";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof ValueReference);
        final ValueReference expression = (ValueReference) obj;
        assertEquals("ùthe_$uglY^_pr@perté", expression.getXPath());
    }

    @Test
    public void testInteger() throws CQLException {
        final String cql = "15";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        assertEquals(Integer.valueOf(15), expression.getValue());
    }

    @Test
    public void testNegativeInteger() throws CQLException {
        final String cql = "-15";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        assertEquals(Integer.valueOf(-15), expression.getValue());
    }

    @Test
    public void testDecimal1() throws CQLException {
        final String cql = "3.14";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        assertEquals(Double.valueOf(3.14), expression.getValue());
    }

    @Test
    public void testDecimal2() throws CQLException {
        final String cql = "9e-1";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        assertEquals(Double.valueOf(9e-1), expression.getValue());
    }

    @Test
    public void testNegativeDecimal() throws CQLException {
        final String cql = "-3.14";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        assertEquals(Double.valueOf(-3.14), expression.getValue());
    }

    @Test
    public void testText() throws CQLException {
        final String cql = "'hello world'";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        assertEquals("hello world", expression.getValue());
    }

    @Test
    public void testText2() throws CQLException {
        final String cql = "'Valle d\\'Aosta'";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        assertEquals("Valle d'Aosta", expression.getValue());
    }

    @Test
    public void testText3() throws CQLException {
        final String cql = "'Valle d\\'Aosta/Vallée d\\'Aoste'";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        assertEquals("Valle d'Aosta/Vallée d'Aoste", expression.getValue());
    }

    @Test
    public void testDatetime() throws CQLException, ParseException{
        //dates are expected to be formated in ISO 8601 : yyyy-MM-dd'T'HH:mm:ss'Z'
        final String cql = "2012-03-21T05:42:36Z";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        Object value = expression.getValue();
        assertTrue(value instanceof TemporalAccessor);
        TemporalAccessor acc = (TemporalAccessor) value;
        assertEquals(Instant.parse("2012-03-21T05:42:36Z"), Instant.from(acc));
    }

    @Test
    public void testDate() throws CQLException, ParseException{
        final String cql = "2012-03-21";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        Object value = expression.getValue();
        assertTrue(value instanceof TemporalAccessor);
        TemporalAccessor acc = (TemporalAccessor) value;
        assertEquals(LocalDate.parse("2012-03-21"), LocalDate.from(acc));
    }

    @Test
    public void testDuration() throws CQLException, ParseException{
        final String cql = "P7Y6M5D";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        assertTrue(expression.getValue() instanceof Period);
        final Period period = (Period) expression.getValue();
        assertEquals(7, period.getYears());
        assertEquals(6, period.getMonths());
        assertEquals(5, period.getDays());
    }

    @Test
    public void testDuration2() throws CQLException, ParseException{
        final String cql = "PT4H3M2S";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        assertTrue(expression.getValue() instanceof Duration);
        final Duration duration = (Duration) expression.getValue();
        assertEquals(14582, duration.get(ChronoUnit.SECONDS));
    }

    @Test
    public void testAddition() throws CQLException {
        final String cql = "3 + 2";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Expression);
        final Expression expression = (Expression) obj;
        assertEquals(FF.add(FF.literal(3), FF.literal(2)), expression);
    }

    @Test
    @Disabled("String cannot be cast to Number.")
    public void testAddition2() throws CQLException {
        final String cql = "'test' + '23'";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Expression);
        final Expression expression = (Expression) obj;
        Object res = expression.apply(null);
        assertEquals("test23", res);
    }

    @Test
    public void testSubstract() throws CQLException {
        final String cql = "3 - 2";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Expression);
        final Expression expression = (Expression) obj;
        assertEquals(FF.subtract(FF.literal(3), FF.literal(2)), expression);
    }

    @Test
    public void testMultiply() throws CQLException {
        final String cql = "3 * 2";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Expression);
        final Expression expression = (Expression) obj;
        assertEquals(FF.multiply(FF.literal(3), FF.literal(2)), expression);
    }

    @Test
    public void testDivide() throws CQLException {
        final String cql = "3 / 2";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Expression);
        final Expression expression = (Expression) obj;
        assertEquals(FF.divide(FF.literal(3), FF.literal(2)), expression);
    }

    @Test
    @Disabled("Function `max` not yet supported.")
    public void testFunction1() throws CQLException {
        final String cql = "max(\"att\",15)";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Expression);
        final Expression<Feature,?> expression = (Expression<Feature,?>) obj;
        assertEquals(FF.function("max",FF.property("att"), FF.literal(15)), expression);
    }

    @Test
    @Disabled("Function `min` not yet supported.")
    public void testFunction2() throws CQLException {
        final String cql = "min(\"att\",cos(3.14))";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Expression);
        final Expression<Feature,?> expression = (Expression<Feature,?>) obj;
        assertEquals(FF.function("min",FF.property("att"), FF.function("cos",FF.literal(3.14d))), expression);
    }

    @Test
    public void testGeometryPoint() throws CQLException {
        final String cql = "POINT(15 30)";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom =  GF.createPoint(new Coordinate(15, 30));
        assertTrue(geom.equals((Geometry)expression.getValue()));
    }

    @Test
    public void testGeometryPointEmpty() throws CQLException {
        final String cql = "POINT EMPTY";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom = (Geometry)expression.getValue();
        assertTrue(geom instanceof Point);
        assertTrue(geom.isEmpty());
    }

    @Test
    public void testGeometryMPoint() throws CQLException {
        final String cql = "MULTIPOINT(15 30, 45 60)";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom =  GF.createMultiPoint(
                new Coordinate[]{
                    new Coordinate(15, 30),
                    new Coordinate(45, 60)
                });
        assertTrue(geom.equals((Geometry)expression.getValue()));
    }

    @Test
    public void testGeometryMPointEmpty() throws CQLException {
        final String cql = "MULTIPOINT EMPTY";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom = (Geometry)expression.getValue();
        assertTrue(geom instanceof MultiPoint);
        assertTrue(geom.isEmpty());
    }

    @Test
    public void testGeometryLineString() throws CQLException {
        final String cql = "LINESTRING(10 20, 30 40, 50 60)";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom =  GF.createLineString(
                new Coordinate[]{
                    new Coordinate(10, 20),
                    new Coordinate(30, 40),
                    new Coordinate(50, 60)
                });
        assertTrue(geom.equals((Geometry)expression.getValue()));
    }

    @Test
    public void testGeometryLineStringEmpty() throws CQLException {
        final String cql = "LINESTRING EMPTY";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom = (Geometry)expression.getValue();
        assertTrue(geom instanceof LineString);
        assertTrue(geom.isEmpty());
    }

    @Test
    public void testGeometryMLineString() throws CQLException {
        final String cql = "MULTILINESTRING((10 20, 30 40, 50 60),(70 80, 90 100, 110 120))";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom =  GF.createMultiLineString(
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
        assertTrue(geom.equals((Geometry)expression.getValue()));
    }

    @Test
    public void testGeometryMLineStringEmpty() throws CQLException {
        final String cql = "MULTILINESTRING EMPTY";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom = (Geometry)expression.getValue();
        assertTrue(geom instanceof MultiLineString);
        assertTrue(geom.isEmpty());
    }

    @Test
    public void testGeometryPolygon() throws CQLException {
        final String cql = "POLYGON((10 20, 30 40, 50 60, 10 20), (70 80, 90 100, 110 120, 70 80))";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom =  GF.createPolygon(
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
        assertTrue(geom.equals((Geometry)expression.getValue()));
    }

    @Test
    public void testGeometryPolygonEmpty() throws CQLException {
        final String cql = "POLYGON EMPTY";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom = (Geometry)expression.getValue();
        assertTrue(geom instanceof Polygon);
        assertTrue(geom.isEmpty());
    }

    @Test
    public void testGeometryMPolygon() throws CQLException {
        final String cql = "MULTIPOLYGON("
                + "((10 20, 30 40, 50 60, 10 20), (70 80, 90 100, 110 120, 70 80)),"
                + "((11 21, 31 41, 51 61, 11 21), (71 81, 91 101, 111 121, 71 81))"
                + ")";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
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
        assertTrue(geom.equals((Geometry)expression.getValue()));
    }

    @Test
    public void testGeometryMPolygonEmpty() throws CQLException {
        final String cql = "MULTIPOLYGON EMPTY";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom = (Geometry)expression.getValue();
        assertTrue(geom instanceof MultiPolygon);
        assertTrue(geom.isEmpty());
    }

    @Test
    public void testGeometryCollection() throws CQLException {
        final String cql = "GEOMETRYCOLLECTION( POINT(15 30), LINESTRING(10 20, 30 40, 50 60) )";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom1 =  GF.createPoint(new Coordinate(15, 30));
        final Geometry geom2 =  GF.createLineString(
                new Coordinate[]{
                    new Coordinate(10, 20),
                    new Coordinate(30, 40),
                    new Coordinate(50, 60)
                });
        final GeometryCollection geom =  GF.createGeometryCollection(new Geometry[]{geom1,geom2});
        final GeometryCollection returned = (GeometryCollection)expression.getValue();
        assertEquals(geom.getNumGeometries(), returned.getNumGeometries());
        assertEquals(geom.getGeometryN(0), returned.getGeometryN(0));
        assertEquals(geom.getGeometryN(1), returned.getGeometryN(1));
    }

    @Test
    public void testGeometryCollectionEmpty() throws CQLException {
        final String cql = "GEOMETRYCOLLECTION EMPTY";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom = (Geometry)expression.getValue();
        assertTrue(geom instanceof GeometryCollection);
        assertTrue(geom.isEmpty());
    }

    @Test
    public void testGeometryEnvelope() throws CQLException {
        final String cql = "ENVELOPE(10, 20, 40, 30)";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom =  GF.createPolygon(
                GF.createLinearRing(
                    new Coordinate[]{
                        new Coordinate(10, 40),
                        new Coordinate(20, 40),
                        new Coordinate(20, 30),
                        new Coordinate(10, 30),
                        new Coordinate(10, 40)
                    }),
                new LinearRing[0]
                );
        assertTrue(geom.equals((Geometry)expression.getValue()));
    }

    @Test
    public void testGeometryEnvelopeEmpty() throws CQLException {
        final String cql = "ENVELOPE EMPTY";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Literal);
        final Literal expression = (Literal) obj;
        final Geometry geom = (Geometry)expression.getValue();
        assertTrue(geom instanceof Polygon);
        assertTrue(geom.isEmpty());
    }

    @Test
    public void testCombine1() throws CQLException {
        final String cql = "((3*1)+(2-6))/4";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Expression);
        final Expression expression = (Expression) obj;
        assertEquals(
                FF.divide(
                    FF.add(
                        FF.multiply(FF.literal(3), FF.literal(1)),
                        FF.subtract(FF.literal(2), FF.literal(6))
                        ),
                    FF.literal(4))
                , expression);
    }

    @Test
    public void testCombine2() throws CQLException {
        final String cql = "3*1+2/4";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Expression);
        final Expression rootAdd = (Expression) obj;
        assertEquals(
                    FF.add(
                        FF.multiply(FF.literal(3), FF.literal(1)),
                        FF.divide(FF.literal(2), FF.literal(4))
                        )
                , rootAdd);
    }

    @Test
    @Disabled("Function `max` not yet supported.")
    public void testCombine3() throws CQLException {
        final String cql = "3*max(val,15)+2/4";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Expression);
        final Expression rootAdd = (Expression) obj;
        assertEquals(
                    FF.add(
                        FF.multiply(
                            FF.literal(3),
                            FF.function("max", FF.property("val"), FF.literal(15)).toValueType(Number.class)
                        ),
                        FF.divide(FF.literal(2), FF.literal(4))
                        )
                , rootAdd);
    }

    @Test
    @Disabled("Function `max` not yet supported.")
    public void testCombine4() throws CQLException {
        final String cql = "3 * max ( val , 15 ) + 2 / 4";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Expression);
        final Expression rootAdd = (Expression) obj;
        assertEquals(
                    FF.add(
                        FF.multiply(
                            FF.literal(3),
                            FF.function("max", FF.property("val", Number.class), FF.literal(15)).toValueType(Number.class)
                        ),
                        FF.divide(FF.literal(2), FF.literal(4))
                        )
                , rootAdd);
    }

    @Test
    @Disabled("Difference in the class argument of `property(…)`.")
    public void testCombine5() throws CQLException {
        final String cql = "(\"NB-Curistes\"*50)/12000";
        final Object obj = CQL.parseExpression(cql);
        assertTrue(obj instanceof Expression);
        final Expression result = (Expression) obj;
        assertEquals(
                FF.divide(
                        FF.multiply(
                                FF.property("NB-Curistes", Number.class),
                                FF.literal(50)
                        ),
                        FF.literal(12000)
                )
                , result);
    }
}
