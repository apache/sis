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
 * All tests from the {@code sis-storage} module, in approximative dependency order.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
@Suite.SuiteClasses({
    org.apache.sis.internal.storage.IOUtilitiesTest.class,
    org.apache.sis.internal.storage.ChannelDataInputTest.class,
    org.apache.sis.internal.storage.ChannelDataOutputTest.class,
    org.apache.sis.internal.storage.ChannelImageInputStreamTest.class,
    org.apache.sis.internal.storage.ChannelImageOutputStreamTest.class,
    org.apache.sis.internal.storage.HyperRectangleReaderTest.class,
    org.apache.sis.storage.ProbeResultTest.class,
    org.apache.sis.storage.StorageConnectorTest.class,
    org.apache.sis.internal.storage.xml.MimeTypeDetectorTest.class,
    org.apache.sis.internal.storage.xml.StoreProviderTest.class,
    org.apache.sis.internal.storage.xml.StoreTest.class,
    org.apache.sis.internal.storage.wkt.StoreProviderTest.class,
    org.apache.sis.internal.storage.wkt.StoreTest.class,
    org.apache.sis.internal.storage.csv.StoreTest.class,
    org.apache.sis.storage.DataStoresTest.class,
    org.apache.sis.index.GeoHashCoderTest.class
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
