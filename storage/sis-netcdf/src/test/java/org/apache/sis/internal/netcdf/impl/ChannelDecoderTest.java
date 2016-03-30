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
package org.apache.sis.internal.netcdf.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import org.apache.sis.internal.netcdf.IOTestCase;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.DecoderTest;
import org.apache.sis.internal.storage.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.DependsOn;

import static org.junit.Assume.*;


/**
 * Tests the {@link ChannelDecoder} implementation. This test shall be executed only if the
 * {@link ChannelDecoder} tests, which use the UCAR library has a reference implementation,
 * passed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(DecoderTest.class)
public final strictfp class ChannelDecoderTest extends DecoderTest {
    /**
     * Creates a new decoder for dataset of the given name.
     *
     * @return The decoder for the given name.
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Override
    protected Decoder createDecoder(final String name) throws IOException, DataStoreException {
        return createChannelDecoder(name);
    }

    /**
     * Creates a new {@link ChannelDecoder} instance for dataset of the given name.
     * The {@code name} parameter can be one of the following values:
     *
     * <ul>
     *   <li>{@link #THREDDS} for a NcML file.</li>
     *   <li>{@link #NCEP}    for a NetCDF binary file.</li>
     *   <li>{@link #CIP}     for a NetCDF binary file.</li>
     *   <li>{@link #LANDSAT} for a NetCDF binary file.</li>
     * </ul>
     *
     * @param  name The file name as one of the above-cited constants.
     * @return The decoder for the given name.
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    public static Decoder createChannelDecoder(final String name) throws IOException, DataStoreException {
        final InputStream in = IOTestCase.class.getResourceAsStream(name);
        assumeNotNull(name, in);
        final ChannelDataInput input = new ChannelDataInput(name,
                Channels.newChannel(in), ByteBuffer.allocate(4096), false);
        return new ChannelDecoder(LISTENERS, input);
    }

    /**
     * Unconditionally returns {@code false} since {@link ChannelDecoder}
     * supports only the classic and 64 bits NetCDF formats.
     *
     * @return {@code false}.
     */
    @Override
    protected boolean isSupplementalFormatSupported(final String format) {
        return false;
    }
}
