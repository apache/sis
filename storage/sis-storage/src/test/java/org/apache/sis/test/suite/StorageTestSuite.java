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
package org.apache.sis.test.suite;

import org.apache.sis.test.TestSuite;
import org.junit.runners.Suite;
import org.junit.BeforeClass;


/**
 * All tests from the {@code sis-storage} module, in rough dependency order.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.2
 * @since   0.3
 * @module
 */
@Suite.SuiteClasses({
    org.apache.sis.internal.storage.CodeTypeTest.class,
    org.apache.sis.internal.storage.StoreUtilitiesTest.class,
    org.apache.sis.internal.storage.io.IOUtilitiesTest.class,
    org.apache.sis.internal.storage.io.ChannelDataInputTest.class,
    org.apache.sis.internal.storage.io.ChannelDataOutputTest.class,
    org.apache.sis.internal.storage.io.ChannelImageInputStreamTest.class,
    org.apache.sis.internal.storage.io.ChannelImageOutputStreamTest.class,
    org.apache.sis.internal.storage.io.HyperRectangleReaderTest.class,
    org.apache.sis.internal.storage.io.RewindableLineReaderTest.class,
    org.apache.sis.internal.storage.MetadataBuilderTest.class,
    org.apache.sis.internal.storage.RangeArgumentTest.class,
    org.apache.sis.internal.storage.MemoryGridResourceTest.class,
    org.apache.sis.storage.FeatureNamingTest.class,
    org.apache.sis.storage.ProbeResultTest.class,
    org.apache.sis.storage.StorageConnectorTest.class,
    org.apache.sis.storage.DataStoreProviderTest.class,
    org.apache.sis.storage.event.StoreListenersTest.class,
    org.apache.sis.storage.CoverageQueryTest.class,
    org.apache.sis.storage.FeatureQueryTest.class,
    org.apache.sis.internal.storage.xml.MimeTypeDetectorTest.class,
    org.apache.sis.internal.storage.xml.StoreProviderTest.class,
    org.apache.sis.internal.storage.xml.StoreTest.class,
    org.apache.sis.internal.storage.wkt.StoreProviderTest.class,
    org.apache.sis.internal.storage.wkt.StoreTest.class,
    org.apache.sis.internal.storage.csv.StoreProviderTest.class,
    org.apache.sis.internal.storage.csv.StoreTest.class,
    org.apache.sis.internal.storage.image.WorldFileStoreTest.class,
    org.apache.sis.internal.storage.image.SelfConsistencyTest.class,
    org.apache.sis.internal.storage.esri.AsciiGridStoreTest.class,
    org.apache.sis.internal.storage.esri.WritableStoreTest.class,
    org.apache.sis.internal.storage.esri.BSQConsistencyTest.class,
    org.apache.sis.internal.storage.esri.BIPConsistencyTest.class,
    org.apache.sis.internal.storage.esri.BILConsistencyTest.class,
    org.apache.sis.internal.storage.folder.StoreTest.class,
    org.apache.sis.internal.storage.JoinFeatureSetTest.class,
    org.apache.sis.internal.storage.ConcatenatedFeatureSetTest.class,
    org.apache.sis.storage.DataStoresTest.class
})
public final strictfp class StorageTestSuite extends TestSuite {
    /**
     * Verifies the list of tests before to run the suite.
     * See {@link #verifyTestList(Class, Class[])} for more information.
     */
    @BeforeClass
    public static void verifyTestList() {
        assertNoMissingTest(StorageTestSuite.class);
        verifyTestList(StorageTestSuite.class);
    }
}
