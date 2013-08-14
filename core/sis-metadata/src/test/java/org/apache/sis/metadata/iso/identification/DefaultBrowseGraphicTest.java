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
package org.apache.sis.metadata.iso.identification;

import java.net.URI;
import javax.xml.bind.JAXBException;
import org.apache.sis.test.TestCase;
import org.apache.sis.xml.XML;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Test {@link DefaultBrowseGraphic}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class DefaultBrowseGraphicTest extends TestCase {
    /**
     * Tests XML marshalling of {@code <gmx:FileName>} inside {@code <gmd:MD_BrowseGraphic>}.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testFileName() throws JAXBException {
        final URI uri = URI.create("file:/catalog/image.png");
        final DefaultBrowseGraphic browse = new DefaultBrowseGraphic(uri);
        final String xml = XML.marshal(browse);
        assertXmlEquals(
                "<gmd:MD_BrowseGraphic>\n" +
                "  <gmd:fileName>\n" +
                "    <gmx:FileName src=\"file:/catalog/image.png\">image.png</gmx:FileName>\n" +
                "  </gmd:fileName>\n" +
                "</gmd:MD_BrowseGraphic>", xml, "xmlns:*");
        /*
         * Unmarshal the element back to a Java object and compare to the original.
         */
        assertEquals(browse, XML.unmarshal(xml));
    }
}
