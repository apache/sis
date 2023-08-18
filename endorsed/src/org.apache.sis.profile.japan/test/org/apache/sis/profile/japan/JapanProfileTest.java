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
package org.apache.sis.profile.japan;

import org.apache.sis.internal.earth.netcdf.GCOM_C;
import org.apache.sis.internal.netcdf.Convention;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the Japanese profile.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.0
 */
public final class JapanProfileTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public JapanProfileTest() {
    }

    /**
     * Tests {@link GCOM_C#mapAttributeName(String, int)}.
     */
    @Test
    public void testMapAttributeName() {
        final Convention c = new GCOM_C();
        assertEquals("title",               c.mapAttributeName("title", 0));
        assertEquals("Product_name",        c.mapAttributeName("title", 1));
        assertNull  (                       c.mapAttributeName("title", 2));
        assertEquals("time_coverage_start", c.mapAttributeName("time_coverage_start", 0));
        assertEquals("Scene_start_time",    c.mapAttributeName("time_coverage_start", 1));
        assertEquals("Image_start_time",    c.mapAttributeName("time_coverage_start", 2));
        assertNull  (                       c.mapAttributeName("time_coverage_start", 3));
    }
}
