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
package org.apache.sis.storage.netcdf;

import java.io.IOException;
import ucar.nc2.NetcdfFile;
import org.apache.sis.internal.netcdf.IOTestCase;
import org.apache.sis.internal.netcdf.TestCase;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.ucar.DecoderWrapper;
import org.apache.sis.internal.netcdf.impl.ChannelDecoder;
import org.apache.sis.internal.netcdf.impl.ChannelDecoderTest;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.Version;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link NetcdfStoreProvider}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.4
 * @since   0.3
 * @module
 */
@DependsOn({
    ChannelDecoderTest.class
})
public final strictfp class NetcdfStoreProviderTest extends IOTestCase {
    /**
     * Tests {@link NetcdfStoreProvider#probeContent(StorageConnector)} for an input stream which shall
     * be recognized as a classic netCDF file.
     *
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testProbeContentFromStream() throws DataStoreException {
        final StorageConnector c = new StorageConnector(IOTestCase.getResourceAsStream(NCEP));
        final NetcdfStoreProvider provider = new NetcdfStoreProvider();
        final ProbeResult probe = provider.probeContent(c);
        assertTrue  ("isSupported", probe.isSupported());
        assertEquals("getMimeType", NetcdfStoreProvider.MIME_TYPE, probe.getMimeType());
        assertEquals("getVersion",  new Version("1"), probe.getVersion());
        c.closeAllExcept(null);
    }

    /**
     * Tests {@link NetcdfStoreProvider#probeContent(StorageConnector)} for a UCAR {@link NetcdfFile} object.
     *
     * @throws IOException if an error occurred while opening the netCDF file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testProbeContentFromUCAR() throws IOException, DataStoreException {
        try (NetcdfFile file = open(NCEP)) {
            final StorageConnector c = new StorageConnector(file);
            final NetcdfStoreProvider provider = new NetcdfStoreProvider();
            final ProbeResult probe = provider.probeContent(c);
            assertTrue  ("isSupported", probe.isSupported());
            assertEquals("getMimeType", NetcdfStoreProvider.MIME_TYPE, probe.getMimeType());
            assertNull  ("getVersion",  probe.getVersion());
        }
    }

    /**
     * Tests {@link NetcdfStoreProvider#decoder(WarningListeners, StorageConnector)} for an input stream which
     * shall be recognized as a classic netCDF file. The provider shall instantiate a {@link ChannelDecoder}.
     *
     * @throws IOException if an error occurred while opening the netCDF file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testDecoderFromStream() throws IOException, DataStoreException {
        final StorageConnector c = new StorageConnector(IOTestCase.getResourceAsStream(NCEP));
        try (Decoder decoder = NetcdfStoreProvider.decoder(TestCase.LISTENERS, c)) {
            assertInstanceOf(NCEP, ChannelDecoder.class, decoder);
        }
    }

    /**
     * Tests {@link NetcdfStoreProvider#decoder(WarningListeners, StorageConnector)} for a UCAR
     * {@link NetcdfFile} object. The provider shall instantiate a {@link DecoderWrapper}.
     *
     * @throws IOException if an error occurred while opening the netCDF file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testDecoderFromUCAR() throws IOException, DataStoreException {
        final StorageConnector c = new StorageConnector(open(NCEP));
        try (Decoder decoder = NetcdfStoreProvider.decoder(TestCase.LISTENERS, c)) {
            assertInstanceOf(NCEP, DecoderWrapper.class, decoder);
        }
    }
}
