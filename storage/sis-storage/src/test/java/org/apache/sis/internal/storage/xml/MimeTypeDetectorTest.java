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
package org.apache.sis.internal.storage.xml;

import java.util.Map;
import java.util.Collections;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.metadata.xml.TestUsingFile;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.metadata.iso.extent.DefaultExtentTest.FILENAME;


/**
 * Tests {@link MimeTypeDetector}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.4
 * @module
 */
public final strictfp class MimeTypeDetectorTest extends TestUsingFile {
    /**
     * Tests a pseudo-XML file in the default namespace, read from a hard-coded string.
     *
     * @throws IOException if an error occurred while reading the bytes or characters.
     */
    @Test
    public void testInDefaultNamespace() throws IOException {
        testFromString("<?xml version=\"1.0\" standalone=\"yes\"?>\n" +
                       "<MD_Metadata xmlns:xsi=\"" + Namespaces.XSI + "\""  +
                                       " xmlns=\"" + LegacyNamespaces.GMD + "\"/>\n");
    }

    /**
     * Tests a XML file in the {@value org.apache.sis.xml.Namespaces#GMD} namespace
     * read from a hard-coded string.
     *
     * @throws IOException if an error occurred while reading the bytes or characters.
     */
    @Test
    public void testGMDFromString() throws IOException {
        testFromString(StoreTest.XML);
    }

    /**
     * Implementation of test methods using a hard-coded XML string as a source.
     */
    private static void testFromString(final String xml) throws IOException {
        assertEquals("application/vnd.iso.19139+xml", getMimeType(xml, Collections.emptyMap()));
    }

    /**
     * Returns the MIME type of the given XML, as detected by {@link MimeTypeDetector}.
     */
    private static String getMimeType(final String xml, final Map<String,String> mimeForRootElements) throws IOException {
        final StringReader in = new StringReader(xml);
        assertEquals('<', in.read());
        assertEquals('?', in.read());
        final MimeTypeDetector detector = new MimeTypeDetector(
                Collections.singletonMap(LegacyNamespaces.GMD, "application/vnd.iso.19139+xml"),
                mimeForRootElements)
        {
            @Override int read() throws IOException {
                return in.read();
            }
        };
        return detector.getMimeType();
    }

    /**
     * Tests a XML file in the {@value org.apache.sis.xml.Namespaces#GMD} namespace
     * read from an input stream.
     *
     * @throws IOException if an error occurred while reading the bytes or characters.
     */
    @Test
    @DependsOnMethod("testGMDFromString")
    public void testGMDFromInputStream() throws IOException {
        final String type;
        try (InputStream in = TestUsingFile.class.getResourceAsStream(XML2007+FILENAME)) {
            assertEquals('<', in.read());
            assertEquals('?', in.read());
            final MimeTypeDetector detector = new MimeTypeDetector(
                    Collections.singletonMap(LegacyNamespaces.GMD, "application/vnd.iso.19139+xml"),
                    Collections.emptyMap())
            {
                @Override int read() throws IOException {
                    return in.read();
                }
            };
            type = detector.getMimeType();
        }
        assertEquals("application/vnd.iso.19139+xml", type);
    }

    /**
     * Tests detection for a XML without namespace.
     *
     * @throws IOException if an error occurred while reading the bytes or characters.
     */
    @Test
    public void testWithoutNamespace() throws IOException {
        final String type = getMimeType(
                "<?xml version=\"1.0\" standalone=\"yes\"?>\n" +
                "<MD_Metadata xmlns:xsi=\"" + Namespaces.XSI + "\">\n",
                Collections.singletonMap("MD_Metadata", "application/vnd.iso.19115+xml"));
        assertEquals("application/vnd.iso.19115+xml", type);
    }
}
