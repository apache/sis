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
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.DecoderTest;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.test.DependsOn;
import org.opengis.test.dataset.TestData;


/**
 * Tests the {@link ChannelDecoder} implementation. This test shall be executed only if the
 * {@link DecoderTest}, which use the UCAR library has a reference implementation, passed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
@DependsOn(DecoderTest.class)
public final strictfp class ChannelDecoderTest extends DecoderTest {
    /**
     * Creates a new decoder for the specified dataset.
     *
     * @return the decoder for the specified dataset.
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Override
    protected Decoder createDecoder(final TestData file) throws IOException, DataStoreException {
        return createChannelDecoder(file);
    }

    /**
     * Creates a new {@link ChannelDecoder} instance for the specified dataset.
     * The {@code file} parameter can be one of the following values:
     *
     * <ul>
     *   <li>{@link TestData#NETCDF_2D_GEOGRAPHIC} — uses a geographic CRS for global data over the world.</li>
     *   <li>{@link TestData#NETCDF_4D_PROJECTED}  — uses a projected CRS with elevation and time.</li>
     * </ul>
     *
     * @param  file  the file as one of the above-cited constants.
     * @return the decoder for the specified dataset.
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    public static Decoder createChannelDecoder(final TestData file) throws IOException, DataStoreException {
        final InputStream in = file.open();
        final ChannelDataInput input = new ChannelDataInput(file.name(),
                Channels.newChannel(in), ByteBuffer.allocate(4096), false);
        return new ChannelDecoder(input, null, GeometryLibrary.JAVA2D, createListeners());
    }
}
