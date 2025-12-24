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

import java.util.Arrays;
import java.util.Locale;
import java.util.Iterator;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.text.ParseException;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.geometry.Envelope;
import org.opengis.util.CodeList;
import org.opengis.feature.Feature;
import org.opengis.filter.*;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.Quantities;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Test reading CQL filters.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class FilterReadingTest extends CQLTestCase {

    private static final double DELTA = 0.00000001;

    private final Geometry baseGeometry;

    private final Geometry baseGeometryPoint;

    /**
     * Creates a new test case.
     */
    public FilterReadingTest() {
        baseGeometry = GF.createPolygon(
                GF.createLinearRing(
                    new Coordinate[]{
                        new Coordinate(10, 20),
                        new Coordinate(30, 40),
                        new Coordinate(50, 60),
                        new Coordinate(10, 20)
                    }),
                new LinearRing[0]);

        baseGeometryPoint = GF.createPoint(
                new Coordinate(12.1, 28.9));
    }

    @Test
    public void testNullFilter() throws CQLException {
        //this is not true cql but is since in commun use cases.
        String cql = "";
        Filter<?> filter = CQL.parseFilter(cql);
        assertEquals(Filter.include(), filter);

        cql = "*";
        filter = CQL.parseFilter(cql);
        assertEquals(Filter.include(), filter);
    }

    @Test
    public void testAnd() throws CQLException {
        final String cql = "att1 = 15 AND att2 = 30 AND att3 = 50";
        final Filter<?> filter = CQL.parseFilter(cql);
        assertEquals(
                FF.and(
                List.<Filter<? super Feature>>of((Filter)
                    FF.equal(FF.property("att1"), FF.literal(15)),
                    FF.equal(FF.property("att2"), FF.literal(30)),
                    FF.equal(FF.property("att3"), FF.literal(50))
                )),
                filter);
    }

    @Test
    public void testOr() throws CQLException {
        final String cql = "att1 = 15 OR att2 = 30 OR att3 = 50";
        final Filter<?> filter = CQL.parseFilter(cql);
        assertEquals(
                FF.or(
                List.<Filter<? super Feature>>of((Filter)
                    FF.equal(FF.property("att1"), FF.literal(15)),
                    FF.equal(FF.property("att2"), FF.literal(30)),
                    FF.equal(FF.property("att3"), FF.literal(50))
                )),
                filter);
    }

    @Test
    public void testOrAnd1() throws CQLException {
        final String cql = "Title = 'VMAI' OR (Title ILIKE '!$Pa_tt%ern?' AND DWITHIN(BoundingBox, POINT(12.1 28.9), 10, 'meter'))";
        final Filter<?> filter = CQL.parseFilter(cql);
        assertEquals(
                FF.or(
                    FF.equal(FF.property("Title"), FF.literal("VMAI")),
                    FF.and(
                        FF.like(FF.property("Title"), "!$Pa_tt%ern?", '%', '_', '\\', false),
                        FF.within(FF.property("BoundingBox"), FF.literal(baseGeometryPoint), Quantities.create(10, Units.METRE))
                        )
                ),
                filter);
    }

    @Test
    public void testOrAnd2() throws CQLException {
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

        final String cql = "NOT (INTERSECTS(BoundingBox, ENVELOPE(10, 20, 40, 30)) "
                + "OR CONTAINS(BoundingBox, POINT(12.1 28.9))) AND BBOX(BoundingBox, 10,20,30,40)";
        final Filter<?> filter = CQL.parseFilter(cql);
        assertEquals(
                FF.and(
                    FF.not(
                        FF.or(
                            FF.intersects(FF.property("BoundingBox"), FF.literal(geom)),
                            FF.contains(FF.property("BoundingBox"), FF.literal(baseGeometryPoint))
                            )
                    ),
                    FF.bbox(FF.property("BoundingBox"), new GeneralEnvelope(new Envelope2D(null, 10, 20, 30-10, 40-20)))
                ),
                filter);
    }

    @Test
    public void testNot() throws CQLException {
        final String cql = "NOT att = 15";
        final Filter<?> filter = CQL.parseFilter(cql);
        assertEquals(FF.not(FF.equal(FF.property("att"), FF.literal(15))), filter);
    }

    @Test
    public void testPropertyIsBetween() throws CQLException {
        final String cql = "att BETWEEN 15 AND 30";
        final Filter<?> filter = CQL.parseFilter(cql);
        assertEquals(FF.between(FF.property("att"), FF.literal(15), FF.literal(30)), filter);
    }

    @Test
    public void testIn() throws CQLException {
        verifyIn((LogicalOperator<?>) CQL.parseFilter("att IN ( 15, 30, 'hello')"));
    }

    @Test
    public void testNotIn() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att NOT IN ( 15, 30, 'hello')");
        assertEquals(LogicalOperatorName.NOT, filter.getOperatorType());
        LogicalOperator<?> logical = assertInstanceOf(LogicalOperator.class, filter);
        logical = assertInstanceOf(LogicalOperator.class, logical.getOperands().get(0));
        verifyIn(logical);
    }

    private void verifyIn(final LogicalOperator<?> filter) {
        assertInstanceOf(LogicalOperator.class, filter);
        assertEquals(LogicalOperatorName.OR, filter.getOperatorType());
        final Iterator<?> expected = Arrays.asList(15, 30, "hello").iterator();
        for (final Filter<?> operand : filter.getOperands()) {
            assertEquals(FF.equal(FF.property("att"), FF.literal(expected.next())), operand);
        }
        assertFalse(expected.hasNext());
    }

    @Test
    public void testPropertyIsEqualTo1() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att=15");
        assertEquals(FF.equal(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsEqualTo2() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att = 15");
        assertEquals(FF.equal(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsNotEqualTo() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att <> 15");
        assertEquals(FF.notEqual(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsNotEqualTo2() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att <>'15'");
        assertEquals(FF.notEqual(FF.property("att"), FF.literal("15")), filter);
    }

    @Test
    public void testPropertyIsGreaterThan() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att > 15");
        assertEquals(FF.greater(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualTo() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att >= 15");
        assertEquals(FF.greaterOrEqual(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsLessThan() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att < 15");
        assertEquals(FF.less(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsLessThanOrEqualTo() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att <= 15");
        assertEquals(FF.lessOrEqual(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsLike() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att LIKE '%hello_'");
        assertEquals(FF.like(FF.property("att"), "%hello_", '%', '_', '\\', true), filter);
    }

    @Test
    public void testPropertyIsNotLike() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att NOT LIKE '%hello_'");
        assertEquals(FF.not(FF.like(FF.property("att"), "%hello_", '%', '_', '\\', true)), filter);
    }

    @Test
    public void testPropertyIsLikeInsensitive() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att ILIKE '%hello_'");
        assertEquals(FF.like(FF.property("att"),"%hello_", '%', '_', '\\', false), filter);
    }

    @Test
    public void testPropertyIsNull() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att IS NULL");
        assertEquals(FF.isNull(FF.property("att")), filter);
    }

    @Test
    public void testPropertyIsNotNull() throws CQLException {
        final Filter<?> filter = CQL.parseFilter("att IS NOT NULL");
        assertEquals(FF.not(FF.isNull(FF.property("att"))), filter);
    }

    @Test
    public void testBBOX1() throws CQLException {
        final Envelope2D env = new Envelope2D(null, 10, 20, 30-10, 40-20);
        testBBOX("BBOX(\"att\", 10, 20, 30, 40)", "att", env);
    }

    @Test
    public void testBBOX2() throws CQLException {
        final Envelope2D env = new Envelope2D(CommonCRS.WGS84.normalizedGeographic(), 10, 20, 30-10, 40-20);
        testBBOX("BBOX(\"att\", 10, 20, 30, 40, 'CRS:84')", "att", env);
    }

    @Test
    public void testBBOX3() throws CQLException {
        final Envelope2D env = new Envelope2D(CommonCRS.WGS84.normalizedGeographic(), 10, 20, 30-10, 40-20);
        testBBOX("BBOX(att, 10, 20, 30, 40, 'CRS:84')", "att", env);
    }

    @Test
    public void testBBOX4() throws CQLException {
        final Envelope2D env = new Envelope2D(null, -10, -20, 10 - -10, 20 - -20);
        testBBOX("BBOX(geometry,-10,-20,10,20)", "geometry", env);
    }

    private void testBBOX(final String cql, final String att, final Envelope env) throws CQLException {
        final Filter<?> filter = CQL.parseFilter(cql);
        final BinarySpatialOperator<?> bsp = assertInstanceOf(BinarySpatialOperator.class, filter);
        assertEquals(SpatialOperatorName.BBOX, filter.getOperatorType());
        assertEquals(FF.property(att), bsp.getOperand1());
        final Literal<?,?> literal = assertInstanceOf(Literal.class, bsp.getOperand2());
        final Envelope value = assertInstanceOf(Envelope.class, literal.getValue());
        assertTrue(AbstractEnvelope.castOrCopy(value).equals(env, 1e-2, false));
    }

    /**
     * Tests {@code DWITHIN}.
     */
    @Test
    public void testDWithin() throws CQLException {
        final String cql = "DWITHIN(BoundingBox, POINT(12.1 28.9), 10, 'meters')";
        final DistanceOperator<?> filter = assertInstanceOf(DistanceOperator.class, CQL.parseFilter(cql));
        assertEquals(DistanceOperatorName.WITHIN, filter.getOperatorType());

        final var expressions = filter.getExpressions();
        assertEquals(FF.property("BoundingBox"), expressions.get(0));
        final Quantity<Length> distance = filter.getDistance();
        assertEquals(10.0, distance.getValue().doubleValue(), DELTA);
        assertEquals(Units.METRE, distance.getUnit());

        final Literal<?,?> literal = assertInstanceOf(Literal.class, expressions.get(1));
        final Geometry value = assertInstanceOf(Geometry.class, literal.getValue());
        assertTrue(baseGeometryPoint.equalsExact(value));
    }

    @Test
    public void testBinarySpatialOperators() throws CQLException {
        for (final SpatialOperatorName operator : SpatialOperatorName.values()) {
            if (operator == SpatialOperatorName.BBOX) continue;
            testSpatialOperators(operator, "");
            if (operator == SpatialOperatorName.OVERLAPS) break;
        }
    }

    @Test
    public void testDistanceOperators() throws CQLException {
        for (final DistanceOperatorName operator : DistanceOperatorName.values()) {
            final DistanceOperator<?> filter = (DistanceOperator<?>) testSpatialOperators(operator, ", 10, 'meters'");
            final Quantity<Length> distance = filter.getDistance();
            assertEquals(10.0, distance.getValue().doubleValue(), DELTA);
            assertEquals(Units.METRE, distance.getUnit());
            if (operator == DistanceOperatorName.WITHIN) break;
        }
    }

    private Filter<?> testSpatialOperators(final CodeList<?> operator, final String suffix) throws CQLException {
        final String name = operator.identifier().get().toUpperCase(Locale.US);
        final String cql = name + "(\"att\", POLYGON((10 20, 30 40, 50 60, 10 20))" + suffix + ')';
        final Filter<?> filter = CQL.parseFilter(cql);
        assertInstanceOf(SpatialOperator.class, filter, name);
        assertEquals(operator, filter.getOperatorType(), name);

        final var expressions = filter.getExpressions();
        assertEquals(FF.property("att"), expressions.get(0));
        final Literal<?,?> literal = assertInstanceOf(Literal.class, expressions.get(1));
        final Geometry value = assertInstanceOf(Geometry.class, literal.getValue());
        assertTrue(baseGeometry.equalsExact(value));
        return filter;
    }

    @Test
    public void testCombine1() throws CQLException {
        testCombine("NOT att = 15 OR att BETWEEN 15 AND 30");
    }

    @Test
    public void testCombine2() throws CQLException {
        testCombine("(NOT att = 15) OR (att BETWEEN 15 AND 30)");
    }

    private void testCombine(final String cql) throws CQLException {
        final Filter<?> filter = CQL.parseFilter(cql);
        assertInstanceOf(LogicalOperator.class, filter);
        assertEquals(LogicalOperatorName.OR, filter.getOperatorType());
        assertEquals(
                FF.or(
                    FF.not(FF.equal(FF.property("att"), FF.literal(15))),
                    FF.between(FF.property("att"), FF.literal(15), FF.literal(30))
                ),
                filter
                );
    }

    @Test
    public void testCombine3() throws CQLException {
        final String cql = "(NOT att1 = 15) AND (att2 = 15 OR att3 BETWEEN 15 AND 30) AND (att4 BETWEEN 1 AND 2)";
        final Filter<?> filter = CQL.parseFilter(cql);
        assertInstanceOf(LogicalOperator.class, filter);
        assertEquals(LogicalOperatorName.AND, filter.getOperatorType());
        assertEquals(
                FF.and(
                    List.<Filter<? super Feature>>of((Filter)
                        FF.not(FF.equal(FF.property("att1"), FF.literal(15))),
                        FF.or(
                            FF.equal(FF.property("att2"), FF.literal(15)),
                            FF.between(FF.property("att3"), FF.literal(15), FF.literal(30))
                        ),
                        FF.between(FF.property("att4"), FF.literal(1), FF.literal(2))
                    )
                ),
                filter
                );
    }

    @Test
    @Disabled("Mismatched type in `property(…, type)`.")
    public void testCombine4() throws CQLException {
        final String cql = "(x+7) <= (y-9)";
        final Filter<?> filter = CQL.parseFilter(cql);
        assertInstanceOf(BinaryComparisonOperator.class, filter);
        assertEquals(ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO, filter.getOperatorType());
        assertEquals(
                FF.lessOrEqual(
                    FF.add(FF.property("x", Number.class), FF.literal(7)),
                    FF.subtract(FF.property("y", Number.class), FF.literal(9))
                ),
                filter
                );
    }

    @Test
    public void testTemporalOperators() throws CQLException, ParseException {
        for (final TemporalOperatorName operator : TemporalOperatorName.values()) {
            final String name = operator.identifier().get().toUpperCase(Locale.US);
            final String cql  = "att " + name + " 2012-03-21T05:42:36Z";
            final Filter<?> filter = CQL.parseFilter(cql);
            assertInstanceOf(TemporalOperator.class, filter, name);
            assertEquals(operator, filter.getOperatorType(),name);
            final var expressions = filter.getExpressions();

            assertEquals(FF.property("att"), expressions.get(0), name);
            final Literal<?,?> literal = assertInstanceOf(Literal.class, expressions.get(1));
            final TemporalAccessor time = assertInstanceOf(TemporalAccessor.class, literal.getValue());
            assertEquals(Instant.parse("2012-03-21T05:42:36Z"), Instant.from(time), name);

            // Skip any non-standard operators added by user.
            if (operator == TemporalOperatorName.ANY_INTERACTS) break;
        }
    }
}
