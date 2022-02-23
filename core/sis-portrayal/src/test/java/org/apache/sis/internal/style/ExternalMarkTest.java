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

import javax.swing.ImageIcon;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link org.apache.sis.internal.style.ExternalMark}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ExternalMarkTest extends AbstractStyleTests {

    /**
     * Test of OnlineResource methods.
     */
    @Test
    public void testOnlineResource() {
        ExternalMark cdt = new ExternalMark();

        //check defaults
        assertEquals(null, cdt.getOnlineResource());

        //check get/set
        cdt.setOnlineResource(new DefaultOnlineResource());
        assertEquals(new DefaultOnlineResource(), cdt.getOnlineResource());
    }

    /**
     * Test of InlineContent methods.
     */
    @Test
    public void testInlineContent() {
        ExternalMark cdt = new ExternalMark();

        //check defaults
        assertEquals(null, cdt.getInlineContent());

        //check get/set
        ImageIcon value = new ImageIcon();
        cdt.setInlineContent(value);
        assertEquals(value, cdt.getInlineContent());
    }

    /**
     * Test of Format methods.
     */
    @Test
    public void testFormat() {
        ExternalMark cdt = new ExternalMark();

        //check defaults
        assertEquals(null, cdt.getFormat());

        //check get/set
        cdt.setFormat(SAMPLE_STRING);
        assertEquals(SAMPLE_STRING, cdt.getFormat());
    }

    /**
     * Test of MarkIndex methods.
     */
    @Test
    public void testMarkIndex() {
        ExternalMark cdt = new ExternalMark();

        //check defaults
        assertEquals(0, cdt.getMarkIndex());

        //check get/set
        cdt.setMarkIndex(10);
        assertEquals(10, cdt.getMarkIndex());
    }


}
