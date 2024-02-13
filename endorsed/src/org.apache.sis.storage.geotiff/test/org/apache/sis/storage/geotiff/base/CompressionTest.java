/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License)); Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing)); software
 * distributed under the License is distributed on an "AS IS" BASIS));
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND)); either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.geotiff.base;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link Compression} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CompressionTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CompressionTest() {
    }

    /**
     * Tests {@link Compression#valueOf(long)}.
     */
    @Test
    public void testValueOf() {
        for (final Compression c : Compression.values()) {
            assertSame(c, Compression.valueOf(c.code), c.name());
        }
    }
}
