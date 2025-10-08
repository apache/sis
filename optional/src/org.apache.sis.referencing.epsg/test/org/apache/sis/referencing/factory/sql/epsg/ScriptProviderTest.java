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
package org.apache.sis.referencing.factory.sql.epsg;

import java.io.IOException;
import java.io.BufferedReader;
import java.util.ServiceLoader;
import org.apache.sis.setup.InstallationResources;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * Test {@link ScriptProvider}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ScriptProviderTest {
    /**
     * Creates a new test case.
     */
    public ScriptProviderTest() {
    }

    /**
     * Returns the {@link ScriptProvider} instance declared in the {@code module-info.class} file.
     * The provider may coexist with providers defined in other modules, so we need to filter them.
     */
    private static InstallationResources getInstance() {
        assumeTrue(ScriptProvider.class.getResource("LICENSE.txt") != null,
                "EPSG resources not found. See `README.md` for manual installation.");

        InstallationResources provider = null;
        for (InstallationResources candidate : ServiceLoader.load(InstallationResources.class)) {
            if (candidate instanceof ScriptProvider) {
                assertNull(provider, "Expected only one instance.");
                provider = candidate;
            }
        }
        assertNotNull(provider, "Expected an instance.");
        return provider;
    }

    /**
     * Tests fetching the licenses.
     *
     * @throws IOException if an error occurred while reading a license.
     */
    @Test
    public void testLicences() throws IOException {
        final InstallationResources provider = getInstance();
        assertTrue(provider.getLicense("EPSG", null, "text/plain").contains("IOGP"));
        assertTrue(provider.getLicense("EPSG", null, "text/html" ).contains("IOGP"));
    }

    /**
     * Tests fetching the resources. This test does not execute the scripts.
     * It only verifies that if the sentinel file exists, then all resources exist and are non-empty.
     *
     * @throws IOException if an error occurred while reading a resource.
     */
    @Test
    public void testResources() throws IOException {
        final InstallationResources provider = getInstance();
        final String[] names = provider.getResourceNames("EPSG");
        assertArrayEquals(new String[] {"Prepare.sql", "Tables.sql", "Data.sql", "FKeys.sql", "Finish.sql"}, names);
        for (int i=0; i<names.length; i++) {
            try (BufferedReader in = provider.openScript("EPSG", i)) {
                // Just verify that we can read.
                assertFalse(in.readLine().isBlank());
            }
        }
    }
}
