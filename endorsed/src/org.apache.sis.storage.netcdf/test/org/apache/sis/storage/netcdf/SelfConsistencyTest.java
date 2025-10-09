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

import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.StorageConnector;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.OptionalTestData;
import org.apache.sis.storage.test.CoverageReadConsistency;


/**
 * Test consistency of read operations in random domains.
 * Assuming that the code reading the full extent is correct, this class can detect some bugs
 * in the code reading sub-regions or applying sub-sampling. This assumption is reasonable if
 * we consider that the code reading the full extent is usually simpler than the code reading
 * a subset of data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class SelfConsistencyTest extends CoverageReadConsistency<NetcdfStore> {
    /**
     * Opens the test file to be used for all tests.
     *
     * @throws DataStoreException if an error occurred while opening the file.
     */
    public SelfConsistencyTest() throws DataStoreException {
        super(new NetcdfStore(null, new StorageConnector(OptionalTestData.NETCDF.path())));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Override
    protected GridCoverageResource resource() throws DataStoreException {
        return assertInstanceOf(GridCoverageResource.class, store.components().iterator().next());
    }
}
