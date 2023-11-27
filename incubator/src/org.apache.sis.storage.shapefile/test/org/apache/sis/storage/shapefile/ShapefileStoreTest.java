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
package org.apache.sis.storage.shapefile;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Point;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.FeatureSet;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.Ignore;
import org.junit.Test;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.FilterFactory;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ShapefileStoreTest {

    @Test
    public void testStream() throws URISyntaxException, DataStoreException {
        final URL url = ShapefileStoreTest.class.getResource("/org/apache/sis/storage/shapefile/point.shp");
        try (final ShapefileStore store = new ShapefileStore(Paths.get(url.toURI()))) {

            //check feature type
            final FeatureType type = store.getType();
            assertEquals("point", type.getName().toString());
            assertEquals(9, type.getProperties(true).size());
            assertNotNull(type.getProperty("sis:identifier"));
            assertNotNull(type.getProperty("sis:envelope"));
            assertNotNull(type.getProperty("sis:geometry"));
            final var geomProp    = (AttributeType) type.getProperty("geometry");
            final var idProp      = (AttributeType) type.getProperty("id");
            final var textProp    = (AttributeType) type.getProperty("text");
            final var integerProp = (AttributeType) type.getProperty("integer");
            final var floatProp   = (AttributeType) type.getProperty("float");
            final var dateProp    = (AttributeType) type.getProperty("date");
            assertEquals(Point.class, geomProp.getValueClass());
            assertEquals(Long.class, idProp.getValueClass());
            assertEquals(String.class, textProp.getValueClass());
            assertEquals(Long.class, integerProp.getValueClass());
            assertEquals(Double.class, floatProp.getValueClass());
            assertEquals(LocalDate.class, dateProp.getValueClass());

            try (Stream<Feature> stream = store.features(false)) {
                Iterator<Feature> iterator = stream.iterator();
                assertTrue(iterator.hasNext());
                Feature feature1 = iterator.next();
                assertEquals(1L, feature1.getPropertyValue("id"));
                assertEquals("text1", feature1.getPropertyValue("text"));
                assertEquals(10L, feature1.getPropertyValue("integer"));
                assertEquals(20.0, feature1.getPropertyValue("float"));
                assertEquals(LocalDate.of(2023, 10, 27), feature1.getPropertyValue("date"));
                Point pt1 = (Point) feature1.getPropertyValue("geometry");

                assertTrue(iterator.hasNext());
                Feature feature2 = iterator.next();
                assertEquals(2L, feature2.getPropertyValue("id"));
                assertEquals("text2", feature2.getPropertyValue("text"));
                assertEquals(40L, feature2.getPropertyValue("integer"));
                assertEquals(60.0, feature2.getPropertyValue("float"));
                assertEquals(LocalDate.of(2023, 10, 28), feature2.getPropertyValue("date"));
                Point pt2 = (Point) feature2.getPropertyValue("geometry");

                assertFalse(iterator.hasNext());
            }
        }
    }

    /**
     * Test optimized envelope filter.
     */
    @Test
    public void testEnvelopeFilter() throws URISyntaxException, DataStoreException {
        final URL url = ShapefileStoreTest.class.getResource("/org/apache/sis/storage/shapefile/point.shp");
        try (final ShapefileStore store = new ShapefileStore(Paths.get(url.toURI()))) {

            final FilterFactory<Feature, Object, Object> ff = DefaultFilterFactory.forFeatures();

            final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
            env.setRange(0, 2, 3);
            env.setRange(1, 42, 43);

            final FeatureQuery query = new FeatureQuery();
            query.setSelection(ff.bbox(ff.property("geometry"), env));
            FeatureSet featureset = store.subset(query);
            //ensure we obtained an optimized version
            assertEquals("org.apache.sis.storage.shapefile.ShapefileStore$AsFeatureSet", featureset.getClass().getName());

            try (Stream<Feature> stream = featureset.features(false)) {
                Iterator<Feature> iterator = stream.iterator();
                assertTrue(iterator.hasNext());
                Feature feature = iterator.next();
                assertEquals(2L, feature.getPropertyValue("id"));
                assertEquals("text2", feature.getPropertyValue("text"));
                assertEquals(40L, feature.getPropertyValue("integer"));
                assertEquals(60.0, feature.getPropertyValue("float"));
                assertEquals(LocalDate.of(2023, 10, 28), feature.getPropertyValue("date"));
                Point pt2 = (Point) feature.getPropertyValue("geometry");

                assertFalse(iterator.hasNext());
            }
        }
    }

    /**
     * Test optimized field selection.
     */
    @Test
    public void testFieldFilter() throws URISyntaxException, DataStoreException {
        final URL url = ShapefileStoreTest.class.getResource("/org/apache/sis/storage/shapefile/point.shp");
        try (final ShapefileStore store = new ShapefileStore(Paths.get(url.toURI()))) {
            final FeatureQuery query = new FeatureQuery();
            query.setProjection("text", "float");
            FeatureSet featureset = store.subset(query);
            //ensure we obtained an optimized version
            assertEquals("org.apache.sis.storage.shapefile.ShapefileStore$AsFeatureSet", featureset.getClass().getName());

            try (Stream<Feature> stream = featureset.features(false)) {
                Iterator<Feature> iterator = stream.iterator();
                assertTrue(iterator.hasNext());
                Feature feature1 = iterator.next();
                assertEquals("text1", feature1.getPropertyValue("text"));
                assertEquals(20.0, feature1.getPropertyValue("float"));

                assertTrue(iterator.hasNext());
                Feature feature2 = iterator.next();
                assertEquals("text2", feature2.getPropertyValue("text"));
                assertEquals(60.0, feature2.getPropertyValue("float"));

                assertFalse(iterator.hasNext());
            }
        }
    }

    @Test
    public void testFiles() throws URISyntaxException, DataStoreException {
        final URL url = ShapefileStoreTest.class.getResource("/org/apache/sis/storage/shapefile/point.shp");
        try (final ShapefileStore store = new ShapefileStore(Paths.get(url.toURI()))) {
            Path[] componentFiles = store.getComponentFiles();
            assertEquals(5, componentFiles.length);
            componentFiles[0].toString().endsWith("point.shp");
            componentFiles[1].toString().endsWith("point.shx");
            componentFiles[2].toString().endsWith("point.dbf");
            componentFiles[3].toString().endsWith("point.prj");
            componentFiles[4].toString().endsWith("point.cpg");
        }
    }

    /**
     * Test creating a new shapefile.
     */
    @Ignore
    @Test
    public void testCreate() throws URISyntaxException, DataStoreException {
        //todo
    }

    /**
     * Test adding features to a shapefile.
     */
    @Ignore
    @Test
    public void testAddFeatures() throws URISyntaxException, DataStoreException {
        //todo
    }

    /**
     * Test remove features from a shapefile.
     */
    @Ignore
    @Test
    public void testRemoveFeatures() throws URISyntaxException, DataStoreException {
        //todo
    }

    /**
     * Test replacing features in a shapefile.
     */
    @Ignore
    @Test
    public void testReplaceFeatures() throws URISyntaxException, DataStoreException {
        //todo
    }

}
