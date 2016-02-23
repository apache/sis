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
package org.apache.sis.internal.storage.wkt;

import java.io.StringReader;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests the WKT {@link Store}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class StoreTest extends TestCase {
    /**
     * The WKT to parse.
     */
    public static final String WKT =
            "GEOGCS[“NTF (Paris)”,\n" +
            "  DATUM[“Nouvelle Triangulation Française (Paris)”,\n" +
            "    SPHEROID[“Clarke 1880 (IGN)”, 6378249.2, 293.4660212936269]],\n" +
            "    PRIMEM[“Paris”, 2.5969213, AUTHORITY[“EPSG”, “8903”]],\n" +
            "  UNIT[“grade”, 0.015707963267948967],\n" +
            "  AXIS[“Latitude”, NORTH],\n" +
            "  AXIS[“Longitude”, EAST]]";

    /**
     * Tests {@link Store#getMetadata()}.
     *
     * @throws DataStoreException If an error occurred while reading the metadata.
     */
    @Test
    public void testMetadata() throws DataStoreException {
        final Metadata metadata;
        try (Store store = new Store(new StorageConnector(new StringReader(WKT)))) {
            metadata = store.getMetadata();
            assertSame("Expected cached value.", metadata, store.getMetadata());
        }
        final GeographicCRS crs = (GeographicCRS) TestUtilities.getSingleton(metadata.getReferenceSystemInfo());
        assertEquals("NTF (Paris)", crs.getName().getCode());
        assertEquals("Nouvelle Triangulation Française (Paris)", crs.getDatum().getName().getCode());
        assertAxisDirectionsEqual("EllipsoidalCS", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
    }
}
