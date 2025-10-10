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
package org.apache.sis.storage.csv;

import java.util.List;
import java.util.Iterator;
import java.time.Instant;
import java.io.StringReader;
import com.esri.core.geometry.Point2D;
import com.esri.core.geometry.Polyline;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.feature.FoliationRepresentation;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataOptionKey;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.setup.GeometryLibrary;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.test.Assertions.assertSingletonBBox;
import static org.apache.sis.test.Assertions.assertSingletonExtent;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;


/**
 * Tests {@link Store}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class StoreTest extends TestCase {
    /**
     * {@code true} if testing a moving feature, or {@code false} (the default) if testing a static feature.
     */
    private boolean isMovingFeature;

    /**
     * Creates a new test case.
     */
    public StoreTest() {
    }

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
     * Opens a CSV store on the test data for reading the lines as-is, without assembling them in a single trajectory.
     */
    private static Store open(final boolean fragmented) throws DataStoreException {
        final var connector = new StorageConnector(testData());
        if (fragmented) {
            connector.setOption(DataOptionKey.FOLIATION_REPRESENTATION, FoliationRepresentation.FRAGMENTED);
        }
        connector.setOption(OptionKey.GEOMETRY_LIBRARY, GeometryLibrary.ESRI);
        return new Store(null, connector);
    }

    /**
     * Tests {@link Store#getMetadata()}.
     *
     * @throws DataStoreException if an error occurred while parsing the data.
     */
    @Test
    public void testGetMetadata() throws DataStoreException {
        final Metadata metadata;
        try (Store store = open(true)) {
            metadata = store.getMetadata();
        }
        final Extent extent = assertSingletonExtent(metadata);
        final GeographicBoundingBox bbox = assertSingletonBBox(extent);
        assertEquals(50.23, bbox.getWestBoundLongitude());
        assertEquals(50.31, bbox.getEastBoundLongitude());
        assertEquals( 9.23, bbox.getSouthBoundLatitude());
        assertEquals( 9.27, bbox.getNorthBoundLatitude());
        assertTrue(extent.getVerticalElements().isEmpty());
    }

    /**
     * Verifies the feature type, then tests {@link Store#features(boolean)}.
     *
     * @throws DataStoreException if an error occurred while parsing the data.
     */
    @Test
    public void testStaticFeatures() throws DataStoreException {
        try (Store store = open(true)) {
            verifyFeatureType(store.featureType, double[].class, 1);
            assertEquals(Foliation.TIME, store.foliation);
            final Iterator<Feature> it = store.features(false).iterator();
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
     * {@snippet lang="csv" :
     *     a,  10, 150, 11.0 2.0 12.0 3.0, walking, 1
     *     b,  10, 190, 10.0 2.0 11.0 3.0, walking, 2
     *     a, 150, 190, 12.0 3.0 10.0 3.0
     *     c,  10, 190, 12.0 1.0 10.0 2.0 11.0 3.0, vehicle, 1
     *     }
     *
     * the two rows for the "a" features shall be merged in a single trajectory.
     *
     * @throws DataStoreException if an error occurred while parsing the data.
     */
    @Test
    public void testMovingFeatures() throws DataStoreException {
        isMovingFeature = true;
        try (Store store = open(false)) {
            verifyFeatureType(store.featureType, Polyline.class, Integer.MAX_VALUE);
            assertEquals(Foliation.TIME, store.foliation);
            final Iterator<Feature> it = store.features(false).iterator();
            assertPropertyEquals(it.next(), "a", "12:33:51", "12:36:51", new double[] {11, 2, 12, 3, 10, 3}, List.of("walking"), List.of(1, 2));
            assertPropertyEquals(it.next(), "b", "12:33:51", "12:36:51", new double[] {10, 2, 11, 3},        List.of("walking"), List.of(2));
            assertPropertyEquals(it.next(), "c", "12:33:51", "12:36:51", new double[] {12, 1, 10, 2, 11, 3}, List.of("vehicle"), List.of(1));
            assertFalse(it.hasNext());
        }
    }

    /**
     * Verifies that the feature type is equal to the expected one.
     */
    private static void verifyFeatureType(final FeatureType type, final Class<?> geometryType, final int maxOccurs) {
        final Iterator<? extends PropertyType> it = type.getProperties(true).iterator();
        assertPropertyTypeEquals((AttributeType<?>) it.next(), "mfidref",       String.class,   1, 1);
        assertPropertyTypeEquals((AttributeType<?>) it.next(), "startTime",     Instant.class,  1, 1);
        assertPropertyTypeEquals((AttributeType<?>) it.next(), "endTime",       Instant.class,  1, 1);
        assertPropertyTypeEquals((AttributeType<?>) it.next(), "trajectory",    geometryType,   1, 1);
        assertPropertyTypeEquals((AttributeType<?>) it.next(), "state",         String.class,   0, maxOccurs);
        assertPropertyTypeEquals((AttributeType<?>) it.next(), "\"type\" code", Integer.class,  0, maxOccurs);
        assertFalse(it.hasNext());
    }

    /**
     * Asserts that the given property type has the given information.
     */
    private static void assertPropertyTypeEquals(final AttributeType<?> p,
            final String name, final Class<?> valueClass, final int minOccurs, final int maxOccurs)
    {
        assertEquals(name,       p.getName().toString());
        assertEquals(valueClass, p.getValueClass());
        assertEquals(minOccurs,  p.getMinimumOccurs());
        assertEquals(maxOccurs,  p.getMaximumOccurs());
    }

    /**
     * Asserts that the property of the given name in the given feature has expected information.
     */
    private void assertPropertyEquals(final Feature f, final String mfidref,
            final String startTime, final String endTime, final double[] trajectory,
            final Object state, final Object typeCode)
    {
        assertEquals(mfidref,            f.getPropertyValue("mfidref"));
        assertEquals(instant(startTime), f.getPropertyValue("startTime"));
        assertEquals(instant(endTime),   f.getPropertyValue("endTime"));
        assertEquals(state,              f.getPropertyValue("state"));
        assertEquals(typeCode,           f.getPropertyValue("\"type\" code"));
        if (isMovingFeature) {
            assertPolylineEquals(trajectory, (Polyline) f.getPropertyValue("trajectory"));
        } else {
            assertArrayEquals(trajectory, (double[]) f.getPropertyValue("trajectory"));
        }
    }

    /**
     * Asserts that the given polyline contains the expected coordinate values.
     */
    private static void assertPolylineEquals(final double[] trajectory, final Polyline polyline) {
        assertEquals(trajectory.length / 2, polyline.getPointCount());
        for (int i=0; i < trajectory.length;) {
            final Point2D xy = polyline.getXY(i / 2);
            assertEquals(trajectory[i++], xy.x, "x");
            assertEquals(trajectory[i++], xy.y, "y");
        }
    }
}
