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
package org.apache.sis.internal.sql.postgis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.lang.reflect.Method;
import org.opengis.referencing.crs.ProjectedCRS;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.apache.sis.storage.sql.ResourceDefinition;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.sql.SQLStoreTest;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.sql.TestDatabase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.Version;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link Postgres}.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
@DependsOn({RasterReaderTest.class, RasterWriterTest.class})
public final strictfp class PostgresTest extends TestCase {
    /**
     * Tests {@link Postgres#parseVersion(String)}.
     */
    @Test
    public void testParseVersion() {
        final Version version = Postgres.parseVersion("3.1 USE_GEOS=1 USE_PROJ=1 USE_STATS=1");
        assertEquals(3, version.getMajor());
        assertEquals(1, version.getMinor());
        assertNull  (   version.getRevision());
    }

    /**
     * Tests reading and writing features and rasters.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testSpatialFeatures() throws Exception {
        try (TestDatabase database = TestDatabase.createOnPostgreSQL(SQLStoreTest.SCHEMA, true)) {
            database.executeSQL(PostgresTest.class, "file:SpatialFeatures.sql");
            final StorageConnector connector = new StorageConnector(database.source);
            connector.setOption(OptionKey.GEOMETRY_LIBRARY, GeometryLibrary.JTS);
            final ResourceDefinition table = ResourceDefinition.table(null, SQLStoreTest.SCHEMA, "SpatialData");
            try (SQLStore store = new SQLStore(new SQLStoreProvider(), connector, table)) {
                /*
                 * Invoke the private `model()` method. We have to use reflection because the class
                 * is not in the same package and we do not want to expose the method in public API.
                 */
                final Method modelAccessor = SQLStore.class.getDeclaredMethod("model");
                modelAccessor.setAccessible(true);
                final Postgres<?> pg = (Postgres<?>) modelAccessor.invoke(store);
                try (Connection connection = database.source.getConnection();
                     ExtendedInfo info = new ExtendedInfo(pg, connection))
                {
                    testInfoStatements(info);
//                  testRasterReader(TestRaster.USHORT, info, connection);
                }
            }
        }
    }

    /**
     * Tests {@link org.apache.sis.internal.sql.feature.InfoStatements}.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    private void testInfoStatements(final ExtendedInfo info) throws Exception {
        assertEquals("findSRID", 4326, info.findSRID(HardCodedCRS.WGS84));
        assertInstanceOf("fetchCRS", ProjectedCRS.class, info.fetchCRS(3395));
    }

    /**
     * Tests {@link RasterReader}.
     */
    private void testRasterReader(final TestRaster test, final ExtendedInfo info, final Connection connection) throws Exception {
        final RasterReader reader = new RasterReader(info);
        try (PreparedStatement stmt = connection.prepareStatement("SELECT image FROM features.\"SpatialData\" WHERE filename=?")) {
            stmt.setString(1, test.filename);
            final ResultSet r = stmt.executeQuery();
            assertTrue(r.next());
            final ChannelDataInput input = new ChannelDataInput(test.filename,
                    Channels.newChannel(r.getBinaryStream(1)), ByteBuffer.allocate(50), false);
            RasterReaderTest.compareReadResult(test, reader, input);
            assertFalse(r.next());
        }
    }
}
