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
package org.apache.sis.style.se1;

// Test dependencies
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests for {@link SelectedChannel}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class SelectedChannelTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public SelectedChannelTest() {
    }

    /**
     * Test of {@code ChannelName} property.
     */
    @Test
    public void testChannelName() {
        final var cdt = factory.createSelectedChannel();

        // Check defaults
        assertNull(cdt.getSourceChannelName());

        // Check get/set
        String value = "A random channel";
        cdt.setSourceChannelName(literal(value));
        assertLiteralEquals(value, cdt.getSourceChannelName());
    }

    /**
     * Test of {@code ContrastEnhancement} property.
     */
    @Test
    public void testContrastEnhancement() {
        final var cdt = factory.createSelectedChannel();

        // Check defaults
        assertEmpty(cdt.getContrastEnhancement());

        // Check get/set
        var value = factory.createContrastEnhancement();
        cdt.setContrastEnhancement(value);
        assertOptionalEquals(value, cdt.getContrastEnhancement());
    }
}
