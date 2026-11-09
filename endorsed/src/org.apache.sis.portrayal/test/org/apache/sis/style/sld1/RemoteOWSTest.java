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
package org.apache.sis.style.sld1;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for {@link RemoteOWS}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class RemoteOWSTest extends SLDTestCase {
    /**
     * Creates a new test case.
     */
    public RemoteOWSTest() {
    }

    /**
     * Test of {@code Service} property.
     */
    @Test
    public void testService() {
        final var cdt = new RemoteOWS();

        // Check defaults
        assertEmpty(cdt.getService());

        // Check get/set
        cdt.setService("WCS");
        assertOptionalEquals("WCS", cdt.getService());
    }

    /**
     * Test of {@code OnlineResource} property.
     */
    @Test
    public void testOnlineResource() {
        final var cdt = new RemoteOWS();

        // Check defaults
        assertEmpty(cdt.getOnlineResource());

        // Check get/set
        var value = anyOnlineResource();
        cdt.setOnlineResource(value);
        assertOptionalEquals(value, cdt.getOnlineResource());
    }

    /**
     * Tests {@code equals}, {@code hashCode} and {@code clone}.
     */
    @Test
    public void testClone() {
        final var cdt = new RemoteOWS();
        cdt.setService("WFS");
        cdt.setOnlineResource(anyOnlineResource());

        final var clone = cdt.clone();
        assertCloneEquals(cdt, clone);
        assertSame(cdt.getOnlineResource().orElseThrow(), clone.getOnlineResource().orElseThrow());

        clone.setService("WCS");
        assertNotEquals(cdt, clone);
    }
}
