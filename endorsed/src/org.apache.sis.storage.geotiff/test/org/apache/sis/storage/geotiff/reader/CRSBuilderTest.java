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
package org.apache.sis.storage.geotiff.reader;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link CRSBuilder} base class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CRSBuilderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CRSBuilderTest() {
    }

    /**
     * Tests {@link CRSBuilder#splitName(String)}. The string used for this test is:
     *
     * <pre class="text">
     *   GCS Name = wgs84|Datum = unknown|Ellipsoid = WGS_1984|Primem = Greenwich|</pre>
     */
    @Test
    public void testSplitName() {
        final String[] names = CRSBuilder.splitName("GCS Name = wgs84|Datum = unknown|Ellipsoid = WGS_1984|Primem = Greenwich|");
        assertEquals("wgs84",     names[CRSBuilder.GCRS]);
        assertEquals("unknown",   names[CRSBuilder.DATUM]);
        assertEquals("WGS_1984",  names[CRSBuilder.ELLIPSOID]);
        assertEquals("Greenwich", names[CRSBuilder.PRIMEM]);
    }

    /**
     * Tests {@link CRSBuilder#splitName(String)} on a string that should not be splitted.
     */
    @Test
    public void testNoSplit() {
        final String[] names = CRSBuilder.splitName("WGS 84");
        assertEquals("WGS 84", names[CRSBuilder.GCRS]);
        assertNull  (names[CRSBuilder.DATUM]);
        assertNull  (names[CRSBuilder.ELLIPSOID]);
        assertNull  (names[CRSBuilder.PRIMEM]);
    }
}
