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
package org.apache.sis.internal.style;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link org.apache.sis.internal.style.ChannelSelection}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ChannelSelectionTest extends AbstractStyleTests {

    /**
     * Test of RGBChannels methods.
     */
    @Test
    public void testRGBChannels() {
        ChannelSelection cdt = new ChannelSelection();

        //check default
        assertArrayEquals(null, cdt.getRGBChannels());

        //check get/set
        SelectedChannelType[] value = new SelectedChannelType[]{
            new SelectedChannelType("R", null),
            new SelectedChannelType("G", null),
            new SelectedChannelType("B", null)};
        cdt.setRGBChannels(value);
        assertArrayEquals(value, cdt.getRGBChannels());
    }


    /**
     * Test of GrayChannel methods.
     */
    @Test
    public void testGrayChannel() {
        ChannelSelection cdt = new org.apache.sis.internal.style.ChannelSelection();

        //check default
        assertEquals(null, cdt.getGrayChannel());

        //check get/set
        cdt.setGray(new SelectedChannelType("Gr", null));
        assertEquals(new SelectedChannelType("Gr", null), cdt.getGrayChannel());

    }


}
