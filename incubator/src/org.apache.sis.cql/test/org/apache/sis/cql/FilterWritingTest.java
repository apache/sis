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
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.opengis.filter.Filter;
import org.opengis.feature.Feature;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Test writing in CQL filters.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class FilterWritingTest extends CQLTestCase {

    private final Geometry baseGeometry;

    /**
     * Creates a new test case.
     */
    public FilterWritingTest() {
        baseGeometry = GF.createPolygon(
                GF.createLinearRing(
                    new Coordinate[]{
                        new Coordinate(10, 20),
                        new Coordinate(30, 40),
                        new Coordinate(50, 60),
                        new Coordinate(10, 20)
                    }),
                new LinearRing[0]);
    }

    @Test
    public void testExcludeFilter() throws CQLException {
        final Filter filter = Filter.exclude();
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("1=0", cql);
    }

    @Test
    public void testIncludeFilter() throws CQLException {
        final Filter filter = Filter.include();
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("1=1", cql);
    }

    @Test
    public void testAnd() throws CQLException {
        final Filter filter = FF.and(
                List.<Filter<? super Feature>>of((Filter)
                    FF.equal(FF.property("att1"), FF.literal(15)),
                    FF.equal(FF.property("att2"), FF.literal(30)),
                    FF.equal(FF.property("att3"), FF.literal(50))
                ));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("(\"att1\" = 15 AND \"att2\" = 30 AND \"att3\" = 50)", cql);
    }

    @Test
    public void testOr() throws CQLException {
        final Filter filter = FF.or(
                List.<Filter<? super Feature>>of((Filter)
                    FF.equal(FF.property("att1"), FF.literal(15)),
                    FF.equal(FF.property("att2"), FF.literal(30)),
                    FF.equal(FF.property("att3"), FF.literal(50))
                ));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("(\"att1\" = 15 OR \"att2\" = 30 OR \"att3\" = 50)", cql);
    }

    @Test
    public void testId() throws CQLException {
        final Filter filter = FF.resourceId("test-1");
        try {
            CQL.write(filter);
            fail("ID filter does not exist in CQL");
        } catch (UnsupportedOperationException ex) {
            assertTrue(ex.getMessage().contains("ID"));
        }
    }

    @Test
    public void testNot() throws CQLException {
        final Filter filter = FF.not(FF.equal(FF.property("att"), FF.literal(15)));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("NOT att = 15", cql);
    }

    @Test
    public void testPropertyIsBetween() throws CQLException {
        final Filter filter = FF.between(FF.property("att"), FF.literal(15), FF.literal(30));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att BETWEEN 15 AND 30", cql);
    }

    @Test
    public void testPropertyIsEqualTo() throws CQLException {
        final Filter filter = FF.equal(FF.property("att"), FF.literal(15));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att = 15", cql);
    }

    @Test
    public void testPropertyIsNotEqualTo() throws CQLException {
        final Filter filter = FF.notEqual(FF.property("att"), FF.literal(15));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att <> 15", cql);
    }

    @Test
    public void testPropertyIsGreaterThan() throws CQLException {
        final Filter filter = FF.greater(FF.property("att"), FF.literal(15));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att > 15", cql);
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualTo() throws CQLException {
        final Filter filter = FF.greaterOrEqual(FF.property("att"), FF.literal(15));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att >= 15", cql);
    }

    @Test
    public void testPropertyIsLessThan() throws CQLException {
        final Filter filter = FF.less(FF.property("att"), FF.literal(15));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att < 15", cql);
    }

    @Test
    public void testPropertyIsLessThanOrEqualTo() throws CQLException {
        final Filter filter = FF.lessOrEqual(FF.property("att"), FF.literal(15));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att <= 15", cql);
    }

    @Disabled
    @Test
    public void testPropertyIsLike() throws CQLException {
        final Filter filter = FF.like(FF.property("att"),"%hello");
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att ILIKE '%hello'", cql);
    }

    @Test
    public void testPropertyIsNull() throws CQLException {
        final Filter filter = FF.isNull(FF.property("att"));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att IS NULL", cql);
    }

    @Test
    public void testBBOX() throws CQLException {
        final Filter filter = FF.bbox(FF.property("att"), new Envelope2D(null, 10, 20, 30-10, 40-20));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("BBOX(att, 10.0, 30.0, 20.0, 40.0)", cql);
    }

    @Test
    public void testBeyond() throws CQLException {
        final Filter filter = FF.beyond(FF.property("att"), FF.literal(baseGeometry), Quantities.create(3, Units.METRE));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("BEYOND(att, POLYGON ((10 20, 30 40, 50 60, 10 20)), 3.0, 'meter')", cql);
    }

    @Test
    public void testContains() throws CQLException {
        final Filter filter = FF.contains(FF.property("att"), FF.literal(baseGeometry));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("CONTAINS(att, POLYGON ((10 20, 30 40, 50 60, 10 20)))", cql);
    }

    @Test
    public void testCrosses() throws CQLException {
        final Filter filter = FF.crosses(FF.property("att"), FF.literal(baseGeometry));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("CROSSES(att, POLYGON ((10 20, 30 40, 50 60, 10 20)))", cql);
    }

    @Test
    public void testDisjoint() throws CQLException {
        final Filter filter = FF.disjoint(FF.property("att"), FF.literal(baseGeometry));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("DISJOINT(att, POLYGON ((10 20, 30 40, 50 60, 10 20)))", cql);
    }

    @Test
    public void testDWithin() throws CQLException {
        final Filter filter = FF.within(FF.property("att"), FF.literal(baseGeometry), Quantities.create(2, Units.METRE));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("DWITHIN(att, POLYGON ((10 20, 30 40, 50 60, 10 20)), 2.0, 'meter')", cql);
    }

    @Test
    public void testEquals() throws CQLException {
        final Filter filter = FF.equals(FF.property("att"), FF.literal(baseGeometry));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("EQUALS(att, POLYGON ((10 20, 30 40, 50 60, 10 20)))", cql);
    }

    @Test
    public void testIntersects() throws CQLException {
        final Filter filter = FF.intersects(FF.property("att"), FF.literal(baseGeometry));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("INTERSECTS(att, POLYGON ((10 20, 30 40, 50 60, 10 20)))", cql);
    }

    @Test
    public void testOverlaps() throws CQLException {
        final Filter filter = FF.overlaps(FF.property("att"), FF.literal(baseGeometry));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("OVERLAPS(att, POLYGON ((10 20, 30 40, 50 60, 10 20)))", cql);
    }

    @Test
    public void testTouches() throws CQLException {
        final Filter filter = FF.touches(FF.property("att"), FF.literal(baseGeometry));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("TOUCHES(att, POLYGON ((10 20, 30 40, 50 60, 10 20)))", cql);
    }

    @Test
    public void testWithin() throws CQLException {
        final Filter filter = FF.within(FF.property("att"), FF.literal(baseGeometry));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("WITHIN(att, POLYGON ((10 20, 30 40, 50 60, 10 20)))", cql);
    }

    @Disabled
    @Test
    public void testAfter() throws CQLException, ParseException {
        final Filter filter = FF.after(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att AFTER 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testAnyInteracts() throws CQLException, ParseException {
        final Filter filter = FF.anyInteracts(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att ANYINTERACTS 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testBefore() throws CQLException, ParseException {
        final Filter filter = FF.before(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att BEFORE 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testBegins() throws CQLException, ParseException {
        final Filter filter = FF.begins(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att BEGINS 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testBegunBy() throws CQLException, ParseException {
        final Filter filter = FF.begunBy(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att BEGUNBY 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testDuring() throws CQLException, ParseException {
        final Filter filter = FF.during(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att DURING 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testEndedBy() throws CQLException, ParseException {
        final Filter filter = FF.endedBy(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att ENDEDBY 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testEnds() throws CQLException, ParseException {
        final Filter filter = FF.ends(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att ENDS 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testMeets() throws CQLException, ParseException {
        final Filter filter = FF.meets(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att MEETS 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testMetBy() throws CQLException, ParseException {
        final Filter filter = FF.metBy(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att METBY 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testOverlappedBy() throws CQLException, ParseException {
        final Filter filter = FF.overlappedBy(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att OVERLAPPEDBY 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testTcontains() throws CQLException, ParseException {
        final Filter filter = FF.tcontains(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att TCONTAINS 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testTequals() throws CQLException, ParseException {
        final Filter filter = FF.tequals(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att TEQUALS 2012-03-21T05:42:36Z", cql);
    }

    @Disabled
    @Test
    public void testToverlaps() throws CQLException, ParseException {
        final Filter filter = FF.toverlaps(FF.property("att"), FF.literal(Instant.parse("2012-03-21T05:42:36Z")));
        final String cql = CQL.write(filter);
        assertNotNull(cql);
        assertEquals("att TOVERLAPS 2012-03-21T05:42:36Z", cql);
    }
}
