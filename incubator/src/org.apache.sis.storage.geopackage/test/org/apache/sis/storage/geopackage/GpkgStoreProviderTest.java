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
package org.apache.sis.storage.geopackage;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link GpkgStoreProvider}.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public final class GpkgStoreProviderTest {
    /**
     * Creates a new test case.
     */
    public GpkgStoreProviderTest() {
    }

    /**
     * Verifies the values of constants for SQLite signature.
     */
    @Test
    public void verifySQLiteSignature() {
        long expected = 0;
        int actualIndex = 0;
        final long[] actual = {GpkgStoreProvider.SIG1, GpkgStoreProvider.SIG2};
        final String signature = "SQLite format 3\u0000";
        for (int i = 0; i < signature.length();) {
            expected = (expected << Byte.SIZE) | signature.charAt(i);
            if ((++i % Long.BYTES) == 0) {
                assertEquals(expected, actual[actualIndex++]);
                expected = 0;
            }
        }
        assertEquals(0, expected);
        assertEquals(actual.length, actualIndex);
    }
}
