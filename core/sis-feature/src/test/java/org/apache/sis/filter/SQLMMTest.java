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

import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;

import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.NullArgumentException;

import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;

import static org.apache.sis.internal.feature.GeometriesTestCase.expectFailFast;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class SQLMMTest extends TestCase {

    private static final String P_NAME = "geom";

    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory2 factory = new DefaultFilterFactory();
    private final GeometryFactory gf = new GeometryFactory();

    /**
     * Test SQL/MM ST_Transform function.
     */
    @Test
    public void ST_TransformTest() {

        CoordinateReferenceSystem inCrs = CommonCRS.WGS84.normalizedGeographic();
        CoordinateReferenceSystem outCrs = CommonCRS.WGS84.geographic();

        //test invalid
        try {
            factory.function("ST_Transform");
            Assert.fail("Creation with no argument should fail");
        } catch (IllegalArgumentException ex) {
            //ok
        }

        //create a test feature
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.addAttribute(Point.class).setName("geom").setCRS(inCrs);
        final FeatureType type = ftb.build();
        final Point geometry = gf.createPoint(new Coordinate(10, 30));
        geometry.setUserData(inCrs);
        final Feature feature = type.newInstance();
        feature.setPropertyValue("geom", geometry);

        { //test transform function using epsg code
            final Function fct = factory.function("ST_Transform", factory.property("geom"), factory.literal("EPSG:4326"));

            //check result
            final Object newGeom = fct.evaluate(feature);
            assertTrue(newGeom instanceof Point);
            final Point trs = (Point) newGeom;
            Assert.assertEquals(outCrs, trs.getUserData());
            Assert.assertEquals(30.0, trs.getX(), 0.0);
            Assert.assertEquals(10.0, trs.getY(), 0.0);
        }

        { //test transform function using crs object
            final Function fct = factory.function("ST_Transform", factory.property("geom"), factory.literal(outCrs));

            //check result
            final Object newGeom = fct.evaluate(feature);
            assertTrue(newGeom instanceof Point);
            final Point trs = (Point) newGeom;
            Assert.assertEquals(outCrs, trs.getUserData());
            Assert.assertEquals(30.0, trs.getX(), 0.0);
            Assert.assertEquals(10.0, trs.getY(), 0.0);
        }
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

        final LineString pt = gf.createLineString(new Coordinate[]{
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
    public void ST_Intersects() {
        try {
            new ST_Intersects(null);
            fail("ST_Intersects operator should accept exactly 2 parameters");
        } catch (NullArgumentException e) {
            // expected behavior
        }

        try {
            new ST_Intersects(new Expression[3]);
            fail("ST_Intersects operator should accept exactly 2 parameters");
        } catch (IllegalArgumentException e) {
            // expected behavior
        }

        final Coordinate start = new Coordinate(0, 0.1);
        final Coordinate second = new Coordinate(1.2, 0.2);
        final LinearRing ring = gf.createLinearRing(new Coordinate[]{
                start, second, new Coordinate(0.7, 0.8), start
        });

        final Literal lring = factory.literal(ring);
        ST_Intersects st = intersects(factory.literal(gf.createPoint(new Coordinate(2, 4))), lring);
        // Ensure argument nullity does not modify behavior
        assertFalse("Unexpected intersection", st.evaluate(null));
        assertFalse("Unexpected intersection", st.evaluate(new Object()));

        // Border should intersect
        final Feature f = mock();
        final Point point = gf.createPoint(second);
        f.setPropertyValue(P_NAME, point);
        final PropertyName geomName = factory.property(P_NAME);
        st = intersects(geomName, lring);
        assertTrue("Border point should intersect triangle", st.evaluate(f));
        // Ensure inverting expression does not modify behavior.
        st = intersects(lring, geomName);
        assertTrue("Border point should intersect triangle", st.evaluate(f));

        // Ensure CRS conversion works as expected (see package-info).
        // Missing
        point.setUserData(CommonCRS.defaultGeographic());
        expectFailFast(() -> intersects(geomName, lring).evaluate(f), IllegalArgumentException.class);
        final Literal lPoint = factory.literal(point);
        expectFailFast(() -> intersects(lPoint, lring).evaluate(null), IllegalArgumentException.class);

        // Disjoint
        final ProjectedCRS nadUtm = CommonCRS.NAD27.universal(32, 37);
        final ProjectedCRS wgsUtm = CommonCRS.WGS84.universal(-2, 4);

        point.setUserData(nadUtm);
        ring.setUserData(wgsUtm);
        expectFailFast(() -> intersects(geomName, lring).evaluate(f), IllegalArgumentException.class);
        expectFailFast(() -> intersects(lPoint, lring).evaluate(null), IllegalArgumentException.class);

        // TODO: activate back after fixing CRS.suggestCommonTarget
        // utm domain contained in CRS:84
//        ring.setUserData(CommonCRS.defaultGeographic());
//        assertTrue("Intersection should be found when CRS are compatible", intersects(geomName, lring).evaluate(f));
//        assertTrue("Intersection should be found when CRS are compatible", intersects(lPoint, lring).evaluate(null));

        // Common base CRS
//        ring.setUserData(CommonCRS.WGS84.universal(0, 0));
//        point.setUserData(CommonCRS.WGS84.universal(7, 8));
//        assertTrue("Intersection should be found when CRS are compatible", intersects(geomName, lring).evaluate(f));
//        assertTrue("Intersection should be found when CRS are compatible", intersects(lPoint, lring).evaluate(null));
    }

    private static ST_Intersects intersects(final Expression left, Expression right) {
        return new ST_Intersects(new Expression[]{left, right});
    }

    /**
     *
     * @return A feature with a single property of {@link Object any} type named after
     */
    private static Feature mock() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder().setName("mock");
        ftb.addAttribute(Object.class).setName(P_NAME);
        final FeatureType mockType = ftb.build();
        return mockType.newInstance();
    }
}
