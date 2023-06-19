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

import java.util.List;
import javax.swing.ImageIcon;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Tests for {@link ExternalGraphic}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class ExternalGraphicTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public ExternalGraphicTest() {
    }

    /**
     * Test of {@code OnlineResource} property.
     */
    @Test
    public void testOnlineResource() {
        ExternalGraphic cdt = new ExternalGraphic();

        // Check defaults
        assertEmpty(cdt.getOnlineResource());

        // Check get/set
        cdt.setOnlineResource(new DefaultOnlineResource());
        assertOptionalEquals(new DefaultOnlineResource(), cdt.getOnlineResource());
    }

    /**
     * Test of {@code InlineContent} property.
     */
    @Test
    public void testInlineContent() {
        ExternalGraphic cdt = new ExternalGraphic();

        // Check defaults
        assertEmpty(cdt.getInlineContent());

        // Check get/set
        ImageIcon value = new ImageIcon();
        cdt.setInlineContent(value);
        assertOptionalEquals(value, cdt.getInlineContent());
    }

    /**
     * Test of {@code Format} property.
     */
    @Test
    public void testFormat() {
        ExternalGraphic cdt = new ExternalGraphic();

        // Check defaults
        assertEmpty(cdt.getFormat());

        // Check get/set
        String value = "A random format";
        cdt.setFormat(value);
        assertOptionalEquals(value, cdt.getFormat());
    }

    /**
     * Test of {@code ColorReplacements} property.
     */
    @Test
    public void testColorReplacements() {
        ExternalGraphic cdt = new ExternalGraphic();

        // Check defaults
        assertTrue(cdt.colorReplacements().isEmpty());

        // Check get/set
        List<ColorReplacement> value = List.of(new ColorReplacement());
        cdt.colorReplacements().addAll(value);
        assertEquals(value, cdt.colorReplacements());
    }
}
