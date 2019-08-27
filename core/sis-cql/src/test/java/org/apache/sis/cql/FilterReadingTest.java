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

import java.text.ParseException;
import java.util.Date;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.AnyInteracts;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.MetBy;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Test reading CQL filters.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class FilterReadingTest extends CQLTestCase {

    private static final double DELTA = 0.00000001;
    private final Geometry baseGeometry = GF.createPolygon(
                GF.createLinearRing(
                    new Coordinate[]{
                        new Coordinate(10, 20),
                        new Coordinate(30, 40),
                        new Coordinate(50, 60),
                        new Coordinate(10, 20)
                    }),
                new LinearRing[0]
                );
    private final Geometry baseGeometryPoint = GF.createPoint(
                new Coordinate(12.1, 28.9));


    @Test
    public void testNullFilter() throws CQLException {
        //this is not true cql but is since in commun use cases.
        String cql = "";
        Object obj = CQL.parseFilter(cql);
        assertEquals(Filter.INCLUDE,obj);

        cql = "*";
        obj = CQL.parseFilter(cql);
        assertEquals(Filter.INCLUDE,obj);
    }

    @Test
    public void testAnd() throws CQLException {
        final String cql = "att1 = 15 AND att2 = 30 AND att3 = 50";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Filter);
        final Filter filter = (Filter) obj;
        assertEquals(
                FF.and(
                UnmodifiableArrayList.wrap(new Filter[] {(Filter)
                    FF.equals(FF.property("att1"), FF.literal(15)),
                    FF.equals(FF.property("att2"), FF.literal(30)),
                    FF.equals(FF.property("att3"), FF.literal(50))
                })),
                filter);
    }

    @Test
    public void testOr() throws CQLException {
        final String cql = "att1 = 15 OR att2 = 30 OR att3 = 50";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Filter);
        final Filter filter = (Filter) obj;
        assertEquals(
                FF.or(
                UnmodifiableArrayList.wrap(new Filter[] {(Filter)
                    FF.equals(FF.property("att1"), FF.literal(15)),
                    FF.equals(FF.property("att2"), FF.literal(30)),
                    FF.equals(FF.property("att3"), FF.literal(50))
                })),
                filter);
    }

    @Ignore
    @Test
    public void testOrAnd1() throws CQLException {
        final String cql = "Title = 'VMAI' OR (Title ILIKE 'LO?Li' AND DWITHIN(BoundingBox, POINT(12.1 28.9), 10, meters))";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Filter);
        final Filter filter = (Filter) obj;
        assertEquals(
                FF.or(
                    FF.equals(FF.property("Title"), FF.literal("VMAI")),
                    FF.and(
                        FF.like(FF.property("Title"), "LO?Li","%","_","\\",false),
                        FF.dwithin(FF.property("BoundingBox"), FF.literal(baseGeometryPoint), 10, "meters")
                        )
                ),
                filter);
    }

    @Ignore
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


        final String cql = "NOT (INTERSECTS(BoundingBox, ENVELOPE(10, 20, 40, 30)) OR CONTAINS(BoundingBox, POINT(12.1 28.9))) AND BBOX(BoundingBox, 10,20,30,40)";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Filter);
        final Filter filter = (Filter) obj;
        assertEquals(
                FF.and(
                    FF.not(
                        FF.or(
                            FF.intersects(FF.property("BoundingBox"), FF.literal(geom)),
                            FF.contains(FF.property("BoundingBox"), FF.literal(baseGeometryPoint))
                            )
                    ),
                    FF.bbox("BoundingBox",10,20,30,40,"")
                ),
                filter);
    }

    @Test
    public void testNot() throws CQLException {
        final String cql = "NOT att = 15";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Not);
        final Not filter = (Not) obj;
        assertEquals(FF.not(FF.equals(FF.property("att"), FF.literal(15))), filter);
    }

    @Ignore
    @Test
    public void testPropertyIsBetween() throws CQLException {
        final String cql = "att BETWEEN 15 AND 30";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsBetween);
        final PropertyIsBetween filter = (PropertyIsBetween) obj;
        assertEquals(FF.between(FF.property("att"), FF.literal(15), FF.literal(30)), filter);
    }

    @Test
    public void testIn() throws CQLException {
        final String cql = "att IN ( 15, 30, 'hello')";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Or);
        final Or filter = (Or) obj;
        assertEquals(FF.equals(FF.property("att"), FF.literal(15)), filter.getChildren().get(0));
        assertEquals(FF.equals(FF.property("att"), FF.literal(30)), filter.getChildren().get(1));
        assertEquals(FF.equals(FF.property("att"), FF.literal("hello")), filter.getChildren().get(2));
    }

    @Test
    public void testNotIn() throws CQLException {
        final String cql = "att NOT IN ( 15, 30, 'hello')";
        Object obj = CQL.parseFilter(cql);
        obj = ((Not)obj).getFilter();
        assertTrue(obj instanceof Or);
        final Or filter = (Or) obj;
        assertEquals(FF.equals(FF.property("att"), FF.literal(15)), filter.getChildren().get(0));
        assertEquals(FF.equals(FF.property("att"), FF.literal(30)), filter.getChildren().get(1));
        assertEquals(FF.equals(FF.property("att"), FF.literal("hello")), filter.getChildren().get(2));
    }

    @Test
    public void testPropertyIsEqualTo1() throws CQLException {
        final String cql = "att=15";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsEqualTo);
        final PropertyIsEqualTo filter = (PropertyIsEqualTo) obj;
        assertEquals(FF.equals(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsEqualTo2() throws CQLException {
        final String cql = "att = 15";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsEqualTo);
        final PropertyIsEqualTo filter = (PropertyIsEqualTo) obj;
        assertEquals(FF.equals(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsNotEqualTo() throws CQLException {
        final String cql = "att <> 15";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsNotEqualTo);
        final PropertyIsNotEqualTo filter = (PropertyIsNotEqualTo) obj;
        assertEquals(FF.notEqual(FF.property("att"), FF.literal(15)), filter);
    }

    @Ignore
    @Test
    public void testPropertyIsNotEqualTo2() throws CQLException {
        final String cql = "att <>'15'";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsNotEqualTo);
        final PropertyIsNotEqualTo filter = (PropertyIsNotEqualTo) obj;
        assertEquals(FF.notEqual(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsGreaterThan() throws CQLException {
        final String cql = "att > 15";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsGreaterThan);
        final PropertyIsGreaterThan filter = (PropertyIsGreaterThan) obj;
        assertEquals(FF.greater(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsGreaterThanOrEqualTo() throws CQLException {
        final String cql = "att >= 15";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsGreaterThanOrEqualTo);
        final PropertyIsGreaterThanOrEqualTo filter = (PropertyIsGreaterThanOrEqualTo) obj;
        assertEquals(FF.greaterOrEqual(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsLessThan() throws CQLException {
        final String cql = "att < 15";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsLessThan);
        final PropertyIsLessThan filter = (PropertyIsLessThan) obj;
        assertEquals(FF.less(FF.property("att"), FF.literal(15)), filter);
    }

    @Test
    public void testPropertyIsLessThanOrEqualTo() throws CQLException {
        final String cql = "att <= 15";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsLessThanOrEqualTo);
        final PropertyIsLessThanOrEqualTo filter = (PropertyIsLessThanOrEqualTo) obj;
        assertEquals(FF.lessOrEqual(FF.property("att"), FF.literal(15)), filter);
    }

    @Ignore
    @Test
    public void testPropertyIsLike() throws CQLException {
        final String cql = "att LIKE '%hello_'";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsLike);
        final PropertyIsLike filter = (PropertyIsLike) obj;
        assertEquals(FF.like(FF.property("att"),"%hello_", "%", "_", "\\",true), filter);
    }

    @Ignore
    @Test
    public void testPropertyIsNotLike() throws CQLException {
        final String cql = "att NOT LIKE '%hello_'";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Not);
        final Not filter = (Not) obj;
        assertEquals(FF.not(FF.like(FF.property("att"),"%hello_", "%", "_", "\\",true)), filter);
    }

    @Ignore
    @Test
    public void testPropertyIsLikeInsensitive() throws CQLException {
        final String cql = "att ILIKE '%hello_'";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsLike);
        final PropertyIsLike filter = (PropertyIsLike) obj;
        assertEquals(FF.like(FF.property("att"),"%hello_", "%", "_", "\\",false), filter);
    }

    @Test
    public void testPropertyIsNull() throws CQLException {
        final String cql = "att IS NULL";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsNull);
        final PropertyIsNull filter = (PropertyIsNull) obj;
        assertEquals(FF.isNull(FF.property("att")), filter);
    }

    @Test
    public void testPropertyIsNotNull() throws CQLException {
        final String cql = "att IS NOT NULL";
        Object obj = CQL.parseFilter(cql);
        obj = ((Not)obj).getFilter();
        assertTrue(obj instanceof PropertyIsNull);
        final PropertyIsNull filter = (PropertyIsNull) obj;
        assertEquals(FF.isNull(FF.property("att")), filter);
    }

    @Ignore
    @Test
    public void testBBOX1() throws CQLException {
        final String cql = "BBOX(\"att\" ,10, 20, 30, 40)";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof BBOX);
        final BinarySpatialOperator filter = (BBOX) obj;
        assertEquals(FF.bbox(FF.property("att"), 10,20,30,40, null), filter);
    }

    @Ignore
    @Test
    public void testBBOX2() throws CQLException {
        final String cql = "BBOX(\"att\" ,10, 20, 30, 40, 'CRS:84')";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof BBOX);
        final BBOX filter = (BBOX) obj;
        assertEquals(FF.bbox(FF.property("att"), 10,20,30,40, "CRS:84"), filter);
    }

    @Ignore
    @Test
    public void testBBOX3() throws CQLException {
        final String cql = "BBOX(att ,10, 20, 30, 40, 'CRS:84')";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof BBOX);
        final BBOX filter = (BBOX) obj;
        assertEquals(FF.bbox(FF.property("att"), 10,20,30,40, "CRS:84"), filter);
    }

    @Ignore
    @Test
    public void testBBOX4() throws CQLException {
        final String cql = "BBOX(geometry,-10,-20,10,20)";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof BBOX);
        final BBOX filter = (BBOX) obj;
        assertEquals(FF.bbox(FF.property("geometry"), -10,-20,10,20,null), filter);
    }

    @Ignore
    @Test
    public void testBeyond() throws CQLException {
        final String cql = "BEYOND(\"att\" ,POLYGON((10 20, 30 40, 50 60, 10 20)), 10, meters)";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Beyond);
        final Beyond filter = (Beyond) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Geometry);
        final Geometry filtergeo = (Geometry) ((Literal)filter.getExpression2()).getValue();
        assertTrue(baseGeometry.equalsExact(filtergeo));
    }

    @Ignore
    @Test
    public void testContains() throws CQLException {
        final String cql = "CONTAINS(\"att\" ,POLYGON((10 20, 30 40, 50 60, 10 20)))";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Contains);
        final Contains filter = (Contains) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Geometry);
        final Geometry filtergeo = (Geometry) ((Literal)filter.getExpression2()).getValue();
        assertTrue(baseGeometry.equalsExact(filtergeo));
    }

    @Ignore
    @Test
    public void testCrosses() throws CQLException {
        final String cql = "CROSSES(\"att\" ,POLYGON((10 20, 30 40, 50 60, 10 20)))";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Crosses);
        final Crosses filter = (Crosses) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Geometry);
        final Geometry filtergeo = (Geometry) ((Literal)filter.getExpression2()).getValue();
        assertTrue(baseGeometry.equalsExact(filtergeo));
    }

    @Ignore
    @Test
    public void testDisjoint() throws CQLException {
        final String cql = "DISJOINT(\"att\" ,POLYGON((10 20, 30 40, 50 60, 10 20)))";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Disjoint);
        final Disjoint filter = (Disjoint) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Geometry);
        final Geometry filtergeo = (Geometry) ((Literal)filter.getExpression2()).getValue();
        assertTrue(baseGeometry.equalsExact(filtergeo));
    }

    @Ignore
    @Test
    public void testDWithin() throws CQLException {
        final String cql = "DWITHIN(\"att\" ,POLYGON((10 20, 30 40, 50 60, 10 20)), 10, 'meters')";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof DWithin);
        final DWithin filter = (DWithin) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertEquals(10.0, filter.getDistance(), DELTA);
        assertEquals("meters", filter.getDistanceUnits());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Geometry);
        final Geometry filtergeo = (Geometry) ((Literal)filter.getExpression2()).getValue();
        assertTrue(baseGeometry.equalsExact(filtergeo));
    }

    @Ignore
    @Test
    public void testDWithin2() throws CQLException {
        //there is an error in this syntax, meters is a literal so it should be writen 'meters"
        //but this writing is commun so we tolerate it
        final String cql = "DWITHIN(BoundingBox, POINT(12.1 28.9), 10, meters)";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof DWithin);
        final DWithin filter = (DWithin) obj;

        assertEquals(FF.property("BoundingBox"), filter.getExpression1());
        assertEquals(10.0, filter.getDistance(), DELTA);
        assertEquals("meters", filter.getDistanceUnits());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Geometry);
        final Geometry filtergeo = (Geometry) ((Literal)filter.getExpression2()).getValue();
        assertTrue(baseGeometryPoint.equalsExact(filtergeo));

    }

    @Ignore
    @Test
    public void testEquals() throws CQLException {
        final String cql = "EQUALS(\"att\" ,POLYGON((10 20, 30 40, 50 60, 10 20)))";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Equals);
        final Equals filter = (Equals) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Geometry);
        final Geometry filtergeo = (Geometry) ((Literal)filter.getExpression2()).getValue();
        assertTrue(baseGeometry.equalsExact(filtergeo));
    }

    @Ignore
    @Test
    public void testIntersects() throws CQLException {
        final String cql = "INTERSECTS(\"att\" ,POLYGON((10 20, 30 40, 50 60, 10 20)))";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Intersects);
        final Intersects filter = (Intersects) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Geometry);
        final Geometry filtergeo = (Geometry) ((Literal)filter.getExpression2()).getValue();
        assertTrue(baseGeometry.equalsExact(filtergeo));
    }

    @Ignore
    @Test
    public void testOverlaps() throws CQLException {
        final String cql = "OVERLAPS(\"att\" ,POLYGON((10 20, 30 40, 50 60, 10 20)))";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Overlaps);
        final Overlaps filter = (Overlaps) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Geometry);
        final Geometry filtergeo = (Geometry) ((Literal)filter.getExpression2()).getValue();
        assertTrue(baseGeometry.equalsExact(filtergeo));
    }

    @Ignore
    @Test
    public void testTouches() throws CQLException {
        final String cql = "TOUCHES(\"att\" ,POLYGON((10 20, 30 40, 50 60, 10 20)))";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Touches);
        final Touches filter = (Touches) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Geometry);
        final Geometry filtergeo = (Geometry) ((Literal)filter.getExpression2()).getValue();
        assertTrue(baseGeometry.equalsExact(filtergeo));
    }

    @Ignore
    @Test
    public void testWithin() throws CQLException {
        final String cql = "WITHIN(\"att\" ,POLYGON((10 20, 30 40, 50 60, 10 20)))";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Within);
        final Within filter = (Within) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Geometry);
        final Geometry filtergeo = (Geometry) ((Literal)filter.getExpression2()).getValue();
        assertTrue(baseGeometry.equalsExact(filtergeo));
    }

    @Ignore
    @Test
    public void testCombine1() throws CQLException {
        final String cql = "NOT att = 15 OR att BETWEEN 15 AND 30";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Or);
        final Or filter = (Or) obj;
        assertEquals(
                FF.or(
                    FF.not(FF.equals(FF.property("att"), FF.literal(15))),
                    FF.between(FF.property("att"), FF.literal(15), FF.literal(30))
                ),
                filter
                );
    }

    @Ignore
    @Test
    public void testCombine2() throws CQLException {
        final String cql = "(NOT att = 15) OR (att BETWEEN 15 AND 30)";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Or);
        final Or filter = (Or) obj;
        assertEquals(
                FF.or(
                    FF.not(FF.equals(FF.property("att"), FF.literal(15))),
                    FF.between(FF.property("att"), FF.literal(15), FF.literal(30))
                ),
                filter
                );
    }

    @Ignore
    @Test
    public void testCombine3() throws CQLException {
        final String cql = "(NOT att1 = 15) AND (att2 = 15 OR att3 BETWEEN 15 AND 30) AND (att4 BETWEEN 1 AND 2)";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof And);
        final And filter = (And) obj;
        assertEquals(
                FF.and(
                    UnmodifiableArrayList.wrap(new Filter[] {(Filter)
                        FF.not(FF.equals(FF.property("att1"), FF.literal(15))),
                        FF.or(
                            FF.equals(FF.property("att2"), FF.literal(15)),
                            FF.between(FF.property("att3"), FF.literal(15), FF.literal(30))
                        ),
                        FF.between(FF.property("att4"), FF.literal(1), FF.literal(2))
                    })
                ),
                filter
                );
    }

    @Test
    public void testCombine4() throws CQLException {
        final String cql = "(x+7) <= (y-9)";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof PropertyIsLessThanOrEqualTo);
        final PropertyIsLessThanOrEqualTo filter = (PropertyIsLessThanOrEqualTo) obj;
        assertEquals(
                FF.lessOrEqual(
                    FF.add(FF.property("x"), FF.literal(7)),
                    FF.subtract(FF.property("y"), FF.literal(9))
                ),
                filter
                );
    }

    @Ignore
    @Test
    public void testAfter() throws CQLException, ParseException {
        final String cql = "att AFTER 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof After);
        final After filter = (After) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testAnyInteracts() throws CQLException, ParseException {
        final String cql = "att ANYINTERACTS 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof AnyInteracts);
        final AnyInteracts filter = (AnyInteracts) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testBefore() throws CQLException, ParseException {
        final String cql = "att BEFORE 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Before);
        final Before filter = (Before) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testBegins() throws CQLException, ParseException {
        final String cql = "att BEGINS 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Begins);
        final Begins filter = (Begins) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testBegunBy() throws CQLException, ParseException {
        final String cql = "att BEGUNBY 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof BegunBy);
        final BegunBy filter = (BegunBy) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testDuring() throws CQLException, ParseException {
        final String cql = "att DURING 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof During);
        final During filter = (During) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testEndedBy() throws CQLException, ParseException {
        final String cql = "att ENDEDBY 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof EndedBy);
        final EndedBy filter = (EndedBy) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testEnds() throws CQLException, ParseException {
        final String cql = "att ENDS 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Ends);
        final Ends filter = (Ends) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testMeets() throws CQLException, ParseException {
        final String cql = "att MEETS 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof Meets);
        final Meets filter = (Meets) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testMetBy() throws CQLException, ParseException {
        final String cql = "att METBY 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof MetBy);
        final MetBy filter = (MetBy) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testOverlappedBy() throws CQLException, ParseException {
        final String cql = "att OVERLAPPEDBY 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof OverlappedBy);
        final OverlappedBy filter = (OverlappedBy) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testTcontains() throws CQLException, ParseException {
        final String cql = "att TCONTAINS 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof TContains);
        final TContains filter = (TContains) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testTequals() throws CQLException, ParseException {
        final String cql = "att TEQUALS 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof TEquals);
        final TEquals filter = (TEquals) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

    @Ignore
    @Test
    public void testToverlaps() throws CQLException, ParseException {
        final String cql = "att TOVERLAPS 2012-03-21T05:42:36Z";
        final Object obj = CQL.parseFilter(cql);
        assertTrue(obj instanceof TOverlaps);
        final TOverlaps filter = (TOverlaps) obj;

        assertEquals(FF.property("att"), filter.getExpression1());
        assertTrue(filter.getExpression2() instanceof Literal);
        assertTrue( ((Literal)filter.getExpression2()).getValue() instanceof Date);
        final Date filterdate = (Date) ((Literal)filter.getExpression2()).getValue();
        assertEquals(parseDate("2012-03-21T05:42:36Z"), filterdate);
    }

}
