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
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.GridGeometry;
import org.apache.sis.internal.netcdf.GridGeometryTest;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.test.DependsOn;


/**
 * Tests the {@link GridGeometry} implementation. This test shall be executed only if the
 * {@link GridGeometryTest} tests, which use the UCAR library has a reference implementation,
 * passed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn({VariableInfoTest.class, GridGeometryTest.class})
public final strictfp class GridGeometryInfoTest extends GridGeometryTest {
    /**
     * Creates a new decoder for dataset of the given name.
     *
     * @return The decoder for the given name.
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Override
    protected Decoder createDecoder(final String name) throws IOException, DataStoreException {
        return ChannelDecoderTest.createChannelDecoder(name);
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

    /**
     * Filters out the one-dimensional coordinate systems created by {@code GridGeometry}
     * but not by the UCAR library.
     *
     * @return The filtered grid geometries to test.
     */
    @Override
    protected GridGeometry[] filter(final GridGeometry[] geometries) {
        final GridGeometry[] copy = new GridGeometry[geometries.length];
        int count = 0;
        for (final GridGeometry geometry : geometries) {
            if (geometry.getSourceDimensions() != 1 || geometry.getTargetDimensions() != 1) {
                copy[count++] = geometry;
            }
        }
        return ArraysExt.resize(copy, count);
    }
}
