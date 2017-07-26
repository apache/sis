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

import java.util.Arrays;
import java.util.Iterator;
import java.io.StringReader;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import com.esri.core.geometry.Point2D;
import com.esri.core.geometry.Polyline;

import static org.junit.Assert.*;
import static java.util.Collections.singletonList;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.internal.jdk8.Instant;


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
     * {@code true} if testing a moving feature, or {@code false} (the default) if testing a static feature.
     */
    private boolean isMovingFeature;

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
        return Instant.ofEpochMilli(date("2012-01-17 " + time).getTime());
    }

    /**
     * Tests {@link Store#getMetadata()}.
     *
     * @throws DataStoreException if an error occurred while parsing the data.
     */
    @Test
    public void testGetMetadata() throws DataStoreException {
        final Metadata metadata;
        try (Store store = new Store(null, new StorageConnector(testData()), true)) {
            metadata = store.getMetadata();
        }
        final Extent extent = getSingleton(((AbstractIdentification) getSingleton(metadata.getIdentificationInfo())).getExtents());
        final GeographicBoundingBox bbox = (GeographicBoundingBox) getSingleton(extent.getGeographicElements());
        assertEquals("westBoundLongitude", 50.23, bbox.getWestBoundLongitude(), STRICT);
        assertEquals("eastBoundLongitude", 50.31, bbox.getEastBoundLongitude(), STRICT);
        assertEquals("southBoundLatitude",  9.23, bbox.getSouthBoundLatitude(), STRICT);
        assertEquals("northBoundLatitude",  9.27, bbox.getNorthBoundLatitude(), STRICT);
        assertTrue("Should not have a vertical extent.", extent.getVerticalElements().isEmpty());
    }

    /**
     * Verifies the feature type, then tests {@link Store#features(boolean)}.
     *
     * @throws DataStoreException if an error occurred while parsing the data.
     */
    @Test
    public void testStaticFeatures() throws DataStoreException {
        try (Store store = new Store(null, new StorageConnector(testData()), true)) {
            verifyFeatureType(store.featureType, double[].class, 1);
            assertEquals("foliation", Foliation.TIME, store.foliation);
            final Iterator<AbstractFeature> it = store.features(false).iterator();
            assertPropertyEquals(it.next(), "a", "12:33:51", "12:36:11", new double[] {11, 2, 12, 3},        "walking", 1);
            assertPropertyEquals(it.next(), "b", "12:33:51", "12:36:51", new double[] {10, 2, 11, 3},        "walking", 2);
            assertPropertyEquals(it.next(), "a", "12:36:11", "12:36:51", new double[] {12, 3, 10, 3},        "walking", 2);
            assertPropertyEquals(it.next(), "c", "12:33:51", "12:36:51", new double[] {12, 1, 10, 2, 11, 3}, "vehicle", 1);
            assertFalse(it.hasNext());
        }
    }

    /**
     * Tests reading the data as a moving features. In the following data:
     *
     * {@preformat text
     *     a,  10, 150, 11.0 2.0 12.0 3.0, walking, 1
     *     b,  10, 190, 10.0 2.0 11.0 3.0, walking, 2
     *     a, 150, 190, 12.0 3.0 10.0 3.0
     *     c,  10, 190, 12.0 1.0 10.0 2.0 11.0 3.0, vehicle, 1
     * }
     *
     * the two rows for the "a" features shall be merged in a single trajectory.
     *
     * @throws DataStoreException if an error occurred while parsing the data.
     */
    @Test
    public void testMovingFeatures() throws DataStoreException {
        isMovingFeature = true;
        try (Store store = new Store(null, new StorageConnector(testData()), false)) {
            verifyFeatureType(store.featureType, Polyline.class, Integer.MAX_VALUE);
            assertEquals("foliation", Foliation.TIME, store.foliation);
            final Iterator<AbstractFeature> it = store.features(false).iterator();
            assertPropertyEquals(it.next(), "a", "12:33:51", "12:36:51", new double[] {11, 2, 12, 3, 10, 3}, singletonList("walking"), Arrays.asList(1, 2));
            assertPropertyEquals(it.next(), "b", "12:33:51", "12:36:51", new double[] {10, 2, 11, 3},        singletonList("walking"), singletonList(2));
            assertPropertyEquals(it.next(), "c", "12:33:51", "12:36:51", new double[] {12, 1, 10, 2, 11, 3}, singletonList("vehicle"), singletonList(1));
            assertFalse(it.hasNext());
        }
    }

    /**
     * Verifies that the feature type is equal to the expected one.
     */
    private static void verifyFeatureType(final DefaultFeatureType type, final Class<?> geometryType, final int maxOccurs) {
        final Iterator<? extends AbstractIdentifiedType> it = type.getProperties(true).iterator();
        assertPropertyTypeEquals((DefaultAttributeType<?>) it.next(), "mfidref",       String.class,   1, 1);
        assertPropertyTypeEquals((DefaultAttributeType<?>) it.next(), "startTime",     Instant.class,  1, 1);
        assertPropertyTypeEquals((DefaultAttributeType<?>) it.next(), "endTime",       Instant.class,  1, 1);
        assertPropertyTypeEquals((DefaultAttributeType<?>) it.next(), "trajectory",    geometryType,   1, 1);
        assertPropertyTypeEquals((DefaultAttributeType<?>) it.next(), "state",         String.class,   0, maxOccurs);
        assertPropertyTypeEquals((DefaultAttributeType<?>) it.next(), "\"type\" code", Integer.class,  0, maxOccurs);
        assertFalse(it.hasNext());
    }

    /**
     * Asserts that the given property type has the given information.
     */
    private static void assertPropertyTypeEquals(final DefaultAttributeType<?> p,
            final String name, final Class<?> valueClass, final int minOccurs, final int maxOccurs)
    {
        assertEquals("name",       name,       p.getName().toString());
        assertEquals("valueClass", valueClass, p.getValueClass());
        assertEquals("minOccurs",  minOccurs,  p.getMinimumOccurs());
        assertEquals("maxOccurs",  maxOccurs,  p.getMaximumOccurs());
    }

    /**
     * Asserts that the property of the given name in the given feature has expected information.
     */
    private void assertPropertyEquals(final AbstractFeature f, final String mfidref,
            final String startTime, final String endTime, final double[] trajectory,
            final Object state, final Object typeCode)
    {
        assertEquals("mfidref",   mfidref,            f.getPropertyValue("mfidref"));
        assertEquals("startTime", instant(startTime), f.getPropertyValue("startTime"));
        assertEquals("endTime",   instant(endTime),   f.getPropertyValue("endTime"));
        assertEquals("state",     state,              f.getPropertyValue("state"));
        assertEquals("typeCode",  typeCode,           f.getPropertyValue("\"type\" code"));
        if (isMovingFeature) {
            assertPolylineEquals(trajectory, (Polyline) f.getPropertyValue("trajectory"));
        } else {
            assertArrayEquals("trajectory", trajectory, (double[]) f.getPropertyValue("trajectory"), STRICT);
        }
    }

    /**
     * Asserts that the given polyline contains the expected coordinate values.
     */
    private static void assertPolylineEquals(final double[] trajectory, final Polyline polyline) {
        assertEquals("pointCount", trajectory.length / 2, polyline.getPointCount());
        for (int i=0; i < trajectory.length;) {
            final Point2D xy = polyline.getXY(i / 2);
            assertEquals("x", trajectory[i++], xy.x, STRICT);
            assertEquals("y", trajectory[i++], xy.y, STRICT);
        }
    }
}
