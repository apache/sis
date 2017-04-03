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
package org.apache.sis.internal.storage.csv;

import java.util.Iterator;
import java.io.StringReader;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Branch-dependent imports
import java.time.Instant;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;


/**
 * Tests {@link Store}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
public final strictfp class StoreTest extends TestCase {
    /**
     * An example of Moving Features file.
     * Derived from the example provided in OGC 14-084r2.
     */
    static StringReader testData() {
        return new StringReader(
            "@stboundedby, urn:ogc:def:crs:CRS:1.3:84, 2D,  50.23 9.23,  50.31 9.27,  2012-01-17T12:33:41Z, 2012-01-17T12:37:00Z, sec\n" +
            "@columns, mfidref, trajectory, state,xsd:string, \"\"\"type\"\" code\",xsd:integer\n" +
            "@foliation,Time\n" +
            "a,  10, 150, 11.0 2.0 12.0 3.0, walking, 1\n" +
            "b,  10, 190, 10.0 2.0 11.0 3.0, walking, 2\n" +
            "a, 150, 190, 12.0 3.0 10.0 3.0\n" +                        // Omitted values are same as previous line.
            "c,  10, 190, 12.0 1.0 10.0 2.0 11.0 3.0, vehicle, 1\n");
    }

    /**
     * Returns the instant for the given time at the day of the test.
     */
    private static Instant instant(final String time) {
        return date("2012-01-17 " + time).toInstant();
    }

    /**
     * Tests {@link Store#getMetadata()}.
     *
     * @throws DataStoreException if an error occurred while parsing the data.
     */
    @Test
    public void testGetMetadata() throws DataStoreException {
        final Metadata metadata;
        try (Store store = new Store(null, new StorageConnector(testData()))) {
            metadata = store.getMetadata();
        }
        final Extent extent = getSingleton(getSingleton(metadata.getIdentificationInfo()).getExtents());
        final GeographicBoundingBox bbox = (GeographicBoundingBox) getSingleton(extent.getGeographicElements());
        assertEquals("westBoundLongitude", 50.23, bbox.getWestBoundLongitude(), STRICT);
        assertEquals("eastBoundLongitude", 50.31, bbox.getEastBoundLongitude(), STRICT);
        assertEquals("southBoundLatitude",  9.23, bbox.getSouthBoundLatitude(), STRICT);
        assertEquals("northBoundLatitude",  9.27, bbox.getNorthBoundLatitude(), STRICT);
        assertTrue("Should not have a vertical extent.", extent.getVerticalElements().isEmpty());
    }

    /**
     * Verifies the feature type, then tests {@link Store#features()}.
     *
     * @throws DataStoreException if an error occurred while parsing the data.
     */
    @Test
    public void testGetFeatures() throws DataStoreException {
        try (Store store = new Store(null, new StorageConnector(testData()))) {
            verifyFeatureType(store.featureType);
            assertEquals("foliation", Foliation.TIME, store.foliation);
            final Iterator<Feature> it = store.features().iterator();
            assertPropertyEquals(it.next(), "a", "12:33:51", "12:36:11", new double[] {11, 2, 12, 3},        "walking", 1);
            assertPropertyEquals(it.next(), "b", "12:33:51", "12:36:51", new double[] {10, 2, 11, 3},        "walking", 2);
            assertPropertyEquals(it.next(), "a", "12:36:11", "12:36:51", new double[] {12, 3, 10, 3},        "walking", 2);
            assertPropertyEquals(it.next(), "c", "12:33:51", "12:36:51", new double[] {12, 1, 10, 2, 11, 3}, "vehicle", 1);
            assertFalse(it.hasNext());
        }
    }

    /**
     * Verifies that the feature type is equal to the expected one.
     */
    private static void verifyFeatureType(final FeatureType type) {
        final Iterator<? extends PropertyType> it = type.getProperties(true).iterator();
        assertPropertyTypeEquals((AttributeType<?>) it.next(), "mfidref",       String.class,   1);
        assertPropertyTypeEquals((AttributeType<?>) it.next(), "startTime",     Instant.class,  1);
        assertPropertyTypeEquals((AttributeType<?>) it.next(), "endTime",       Instant.class,  1);
        assertPropertyTypeEquals((AttributeType<?>) it.next(), "trajectory",    double[].class, 1);
        assertPropertyTypeEquals((AttributeType<?>) it.next(), "state",         String.class,   0);
        assertPropertyTypeEquals((AttributeType<?>) it.next(), "\"type\" code", Integer.class,  0);
        assertFalse(it.hasNext());
    }

    /**
     * Asserts that the given property type has the given information.
     */
    private static void assertPropertyTypeEquals(final AttributeType<?> p,
            final String name, final Class<?> valueClass, final int minOccurs)
    {
        assertEquals("name",       name,       p.getName().toString());
        assertEquals("valueClass", valueClass, p.getValueClass());
        assertEquals("minOccurs",  minOccurs,  p.getMinimumOccurs());
        assertEquals("maxOccurs",  1,          p.getMaximumOccurs());
    }

    /**
     * Asserts that the property of the given name in the given feature has expected information.
     */
    private static void assertPropertyEquals(final Feature f, final String mfidref,
            final String startTime, final String endTime, final double[] trajectory,
            final String state, final int typeCode)
    {
        assertEquals     ("mfidref",    mfidref,               f.getPropertyValue("mfidref"));
        assertEquals     ("startTime",  instant(startTime),    f.getPropertyValue("startTime"));
        assertEquals     ("endTime",    instant(endTime),      f.getPropertyValue("endTime"));
        assertEquals     ("state",      state,                 f.getPropertyValue("state"));
        assertEquals     ("typeCode",   typeCode,              f.getPropertyValue("\"type\" code"));
        assertArrayEquals("trajectory", trajectory, (double[]) f.getPropertyValue("trajectory"), STRICT);
    }
}
