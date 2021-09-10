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
import org.apache.sis.internal.netcdf.Grid;
import org.apache.sis.internal.netcdf.GridTest;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.test.DependsOn;

// Branch-specific imports
import org.opengis.test.dataset.TestData;


/**
 * Tests the {@link GridInfo} implementation. This test shall be executed only if the
 * {@link GridTest} tests, which use the UCAR library has a reference implementation,
 * passed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.3
 * @module
 */
@DependsOn({VariableInfoTest.class, GridTest.class})
public final strictfp class GridInfoTest extends GridTest {
    /**
     * Creates a new test.
     */
    public GridInfoTest() {
        includeRuntimeDimension = true;
    }

    /**
     * Creates a new decoder for the specified dataset.
     *
     * @return the decoder for the specified dataset.
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Override
    protected Decoder createDecoder(final TestData file) throws IOException, DataStoreException {
        return ChannelDecoderTest.createChannelDecoder(file);
    }

    /**
     * Filters out the one-dimensional coordinate systems created by {@code GridGeometry}
     * but not by the UCAR library.
     *
     * @return the filtered grid geometries to test.
     */
    @Override
    protected Grid[] filter(final Grid[] geometries) {
        final Grid[] copy = new Grid[geometries.length];
        int count = 0;
        for (final Grid geometry : geometries) {
            if (geometry.getSourceDimensions() != 1 || geometry.getTargetDimensions() != 1) {
                copy[count++] = geometry;
            }
        }
        return ArraysExt.resize(copy, count);
    }
}
