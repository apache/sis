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

import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Tests {@link SQLMM} functions implementations.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
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

    /**
     * Test SQL/MM ST_Centroid function.
     */
    @Test
    public void ST_CentroidTest() {

        CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic();

        //test invalid
        try {
            factory.function("ST_Centroid");
            Assert.fail("Creation with no argument should fail");
        } catch (IllegalArgumentException ex) {
            //ok
        }

        final LineString geometry = gf.createLineString(new Coordinate[]{new Coordinate(10, 20), new Coordinate(30, 20)});
        geometry.setSRID(4326);
        geometry.setUserData(crs);


        final Function fct = factory.function("ST_Centroid", factory.literal(geometry));

        //check result
        final Object newGeom = fct.evaluate(null);
        Assert.assertTrue(newGeom instanceof Point);
        final Point trs = (Point) newGeom;
        Assert.assertEquals(crs, trs.getUserData());
        Assert.assertEquals(4326, trs.getSRID());
        Assert.assertEquals(20.0, trs.getX(), 0.0);
        Assert.assertEquals(20.0, trs.getY(), 0.0);

    }

    /**
     * Test SQL/MM ST_Buffer function.
     */
    @Test
    public void ST_BufferTest() {

        CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic();

        //test invalid
        try {
            factory.function("ST_Buffer");
            Assert.fail("Creation with no argument should fail");
        } catch (IllegalArgumentException ex) {
            //ok
        }

        final Point geometry = gf.createPoint(new Coordinate(10, 20));
        geometry.setSRID(4326);
        geometry.setUserData(crs);


        final Function fct = factory.function("ST_Buffer", factory.literal(geometry), factory.literal(1.0));

        //check result
        final Object newGeom = fct.evaluate(null);
        Assert.assertTrue(newGeom instanceof Polygon);
        final Polygon trs = (Polygon) newGeom;
        Assert.assertEquals(crs, trs.getUserData());
        Assert.assertEquals(4326, trs.getSRID());
        Envelope env = trs.getEnvelopeInternal();
        Assert.assertEquals(9.0, env.getMinX(), 0.0);
        Assert.assertEquals(11.0, env.getMaxX(), 0.0);
        Assert.assertEquals(19.0, env.getMinY(), 0.0);
        Assert.assertEquals(21.0, env.getMaxY(), 0.0);

    }
}
