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
package org.apache.sis.util;

import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link ArgumentChecks} static methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(org.apache.sis.util.resources.IndexedResourceBundleTest.class)
public final strictfp class ArgumentChecksTest extends TestCase {
    /**
     * Tests {@link ArgumentChecks#ensureNonNullElement(String, int, Object)}.
     */
    @Test
    public void testEnsureNonNullElement() {
        try {
            ArgumentChecks.ensureNonNullElement("axes", 2, null);
            fail("Expected a NullArgumentException.");
        } catch (NullArgumentException e) {
            assertTrue(e.getMessage().contains("axes[2]"));
        }
        try {
            ArgumentChecks.ensureNonNullElement("axes[#].unit", 2, null);
            fail("Expected a NullArgumentException.");
        } catch (NullArgumentException e) {
            assertTrue(e.getMessage().contains("axes[2].unit"));
        }
    }
}
