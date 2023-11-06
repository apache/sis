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
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.storage.DataStoreException;
import org.junit.Test;
import org.locationtech.jts.geom.Point;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.DefaultAttributeType;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ShapefileStoreTest {

    @Test
    public void testStream() throws URISyntaxException, DataStoreException {
        final URL url = ShapefileStoreTest.class.getResource("/org/apache/sis/storage/shapefile/point.shp");
        final ShapefileStore store = new ShapefileStore(Paths.get(url.toURI()));

        //check feature type
        final DefaultFeatureType type = store.getType();
        assertEquals("point", type.getName().toString());
        assertEquals(9, type.getProperties(true).size());
        assertNotNull(type.getProperty("sis:identifier"));
        assertNotNull(type.getProperty("sis:envelope"));
        assertNotNull(type.getProperty("sis:geometry"));
        final var geomProp    = (DefaultAttributeType) type.getProperty("geometry");
        final var idProp      = (DefaultAttributeType) type.getProperty("id");
        final var textProp    = (DefaultAttributeType) type.getProperty("text");
        final var integerProp = (DefaultAttributeType) type.getProperty("integer");
        final var floatProp   = (DefaultAttributeType) type.getProperty("float");
        final var dateProp    = (DefaultAttributeType) type.getProperty("date");
        assertEquals(Point.class, geomProp.getValueClass());
        assertEquals(Long.class, idProp.getValueClass());
        assertEquals(String.class, textProp.getValueClass());
        assertEquals(Long.class, integerProp.getValueClass());
        assertEquals(Double.class, floatProp.getValueClass());
        assertEquals(LocalDate.class, dateProp.getValueClass());

        try (Stream<AbstractFeature> stream = store.features(false)) {
            Iterator<AbstractFeature> iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            AbstractFeature feature1 = iterator.next();
            assertEquals(1L, feature1.getPropertyValue("id"));
            assertEquals("text1", feature1.getPropertyValue("text"));
            assertEquals(10L, feature1.getPropertyValue("integer"));
            assertEquals(20.0, feature1.getPropertyValue("float"));
            assertEquals(LocalDate.of(2023, 10, 27), feature1.getPropertyValue("date"));
            Point pt1 = (Point) feature1.getPropertyValue("geometry");

            assertTrue(iterator.hasNext());
            AbstractFeature feature2 = iterator.next();
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
