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
import java.nio.channels.Channels;
import org.apache.sis.internal.netcdf.IOTestCase;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.VariableTest;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.DependsOn;

import static org.junit.Assume.*;


/**
 * Tests the {@link VariableInfo} implementation. This test shall be executed only if the
 * {@link VariableTest} tests, which use the UCAR library has a reference implementation,
 * passed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(VariableTest.class)
public final strictfp class VariableInfoTest extends VariableTest {
    /**
     * Creates a new decoder of the given name.
     */
    @Override
    protected Decoder createDecoder(final String name) throws IOException {
        final InputStream in = IOTestCase.class.getResourceAsStream(name);
        assumeNotNull(name, in);
        try {
            return new ChannelDecoder(null, name, Channels.newChannel(in));
        } catch (DataStoreException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Unconditionally returns {@code false} since {@link ChannelDecoder}
     * supports only the classic and 64 bits NetCDF formats.
     */
    @Override
    protected boolean isSupplementalFormatSupported(final String format) {
        return false;
    }
}
