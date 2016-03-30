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
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.SpatialTemporalExtent;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.metadata.iso.extent.DefaultSpatialTemporalExtent;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;


/**
 * Tests {@link Store}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class StoreTest extends TestCase {
    /**
     * An example of Moving Features file.
     * Derived from the example provided in OGC 14-084r2.
     */
    private static final String TEST_DATA =
            "@stboundedby, urn:ogc:def:crs:CRS:1.3:84, 2D,  50.23 9.23 , 50.31 9.27,  2012-01-17T12:33:41Z, 2012-01-17T12:37:00Z, sec\n" +
            "@columns, mfidref, trajectory, state,xsd:string, \"\"\"type\"\" code\",xsd:integer\n" +
            "a,10,150,11.0 2.0 12.0 3.0,walking,1\n" +
            "b,10,190,10.0 2.0 11.0 3.0,walking,2\n" +
            "a,150,190,12.0 3.0 10.0 3.0,walking,2\n" +
            "c,10,190,12.0 1.0 10.0 2.0 11.0 3.0,vehicle,1\n";

    /**
     * Tests {@link Store#getMetadata()}.
     *
     * @throws DataStoreException if an error occurred while parsing the data.
     */
    @Test
    @org.junit.Ignore("Pending completion of sis-referencing")
    public void testGetMetadata() throws DataStoreException {
        final Metadata metadata;
        Store store = new Store(new StorageConnector(new StringReader(TEST_DATA)));
        try {
            metadata = store.getMetadata();
        } finally {
            store.close();
        }
        final AbstractIdentification id = (AbstractIdentification) getSingleton(metadata.getIdentificationInfo());
        final SpatialTemporalExtent extent = (SpatialTemporalExtent) getSingleton(getSingleton(id.getExtents()).getTemporalElements());
        final GeographicBoundingBox bbox = (GeographicBoundingBox) getSingleton(extent.getSpatialExtent());
        assertEquals("westBoundLongitude", 50.23, bbox.getWestBoundLongitude(), STRICT);
        assertEquals("eastBoundLongitude", 50.31, bbox.getEastBoundLongitude(), STRICT);
        assertEquals("southBoundLatitude",  9.23, bbox.getSouthBoundLatitude(), STRICT);
        assertEquals("northBoundLatitude",  9.27, bbox.getNorthBoundLatitude(), STRICT);
        assertNull("Should not have a vertical extent.", ((DefaultSpatialTemporalExtent) extent).getVerticalExtent());
        assertNotNull("Should have a temporal extent", extent.getExtent());
    }

    /**
     * Tests {@link Store#getFeatures()}.
     *
     * @throws DataStoreException if an error occurred while parsing the data.
     */
    @Test
    public void testGetFeatures() throws DataStoreException {
        Store store = new Store(new StorageConnector(new StringReader(TEST_DATA)));
        try {
            final Iterator<AbstractFeature> it = store.getFeatures();
            assertFeatureEquals(it.next(), "a", new double[] {11, 2, 12, 3},        "walking", 1);
            assertFeatureEquals(it.next(), "b", new double[] {10, 2, 11, 3},        "walking", 2);
            assertFeatureEquals(it.next(), "a", new double[] {12, 3, 10, 3},        "walking", 2);
            assertFeatureEquals(it.next(), "c", new double[] {12, 1, 10, 2, 11, 3}, "vehicle", 1);
            assertFalse(it.hasNext());
        } finally {
            store.close();
        }
    }

    /**
     * Asserts that the given feature has the given properties.
     */
    private static void assertFeatureEquals(final AbstractFeature f, final String mfidref,
            final double[] trajectory, final String state, final int typeCode)
    {
        assertEquals     ("mfidref",    mfidref,  f.getPropertyValue("mfidref"));
        assertEquals     ("state",      state,    f.getPropertyValue("state"));
        assertEquals     ("typeCode",   typeCode, f.getPropertyValue("\"type\" code"));
        assertArrayEquals("trajectory", trajectory, (double[]) f.getPropertyValue("trajectory"), STRICT);
    }
}
