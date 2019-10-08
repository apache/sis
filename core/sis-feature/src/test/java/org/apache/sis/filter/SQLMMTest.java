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
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import static org.junit.Assert.fail;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class SQLMMTest extends TestCase {
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
            Assert.assertTrue(newGeom instanceof Point);
            final Point trs = (Point) newGeom;
            Assert.assertEquals(outCrs, trs.getUserData());
            Assert.assertEquals(30.0, trs.getX(), 0.0);
            Assert.assertEquals(10.0, trs.getY(), 0.0);
        }

        { //test transform function using crs object
            final Function fct = factory.function("ST_Transform", factory.property("geom"), factory.literal(outCrs));

            //check result
            final Object newGeom = fct.evaluate(feature);
            Assert.assertTrue(newGeom instanceof Point);
            final Point trs = (Point) newGeom;
            Assert.assertEquals(outCrs, trs.getUserData());
            Assert.assertEquals(30.0, trs.getX(), 0.0);
            Assert.assertEquals(10.0, trs.getY(), 0.0);
        }
    }

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

        // TODO: update SIS version then add test cases.
    }
}
