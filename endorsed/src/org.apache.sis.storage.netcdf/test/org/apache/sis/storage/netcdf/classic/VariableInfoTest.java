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
package org.apache.sis.storage.netcdf.classic;

import java.io.IOException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.netcdf.base.Decoder;
import org.apache.sis.storage.netcdf.base.VariableTest;

// Test dependencies
import org.apache.sis.test.DependsOn;

// Specific to the main branch:
import org.apache.sis.storage.netcdf.base.TestData;


/**
 * Tests the {@link VariableInfo} implementation. This test shall be executed only if the
 * {@link VariableTest} tests, which use the UCAR library has a reference implementation,
 * passed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.3
 */
@DependsOn({ChannelDecoderTest.class, VariableTest.class})
public final class VariableInfoTest extends VariableTest {
    /**
     * Creates a new test.
     */
    public VariableInfoTest() {
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
}
