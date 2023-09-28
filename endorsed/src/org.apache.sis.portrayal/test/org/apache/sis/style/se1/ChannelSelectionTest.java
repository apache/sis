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
 * Tests for {@link ChannelSelection}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class ChannelSelectionTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public ChannelSelectionTest() {
    }

    /**
     * Test of {@code RedChannel}, {@code GreenChannel} and {@code BlueChannel} properties.
     */
    @Test
    public void testRGBChannels() {
        final var cdt = factory.createChannelSelection();

        // Check default
        assertNull(cdt.getChannels());

        // Check get/set
        final var red   = factory.createSelectedChannel("R");
        final var green = factory.createSelectedChannel("G");
        final var blue  = factory.createSelectedChannel("B");
        cdt.setChannels(red, green, blue);
        assertArrayEquals(new Object[] {red, green, blue}, cdt.getChannels());
    }


    /**
     * Test of {@code GrayChannel} property.
     */
    @Test
    public void testGrayChannel() {
        final var cdt = factory.createChannelSelection();

        // Check default
        assertNull(cdt.getChannels());

        // Check get/set
        final var gray = factory.createSelectedChannel("Gray");
        cdt.setChannels(gray);
        assertArrayEquals(new Object[] {gray}, cdt.getChannels());
    }
}
