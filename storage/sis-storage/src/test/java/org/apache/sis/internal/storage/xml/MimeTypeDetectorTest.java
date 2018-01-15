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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.metadata.iso.extent.DefaultExtentTest;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.*;


/**
 * Tests {@link MimeTypeDetector}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.4
 * @module
 */
public final strictfp class MimeTypeDetectorTest extends TestCase {
    /**
     * Tests a pseudo-XML file in the default namespace, read from a hard-coded string.
     *
     * @throws IOException if an error occurred while reading the bytes or characters.
     */
    @Test
    public void testInDefaultNamespace() throws IOException {
        testFromString("<?xml version=\"1.0\" standalone=\"yes\"?>\n" +
                       "<MD_Metadata xmlns:xsi=\"" + Namespaces.XSI + "\""  +
                                       " xmlns=\"" + Namespaces.GMD + "\"/>\n");
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
        final StringReader in = new StringReader(xml);
        assertEquals('<', in.read());
        assertEquals('?', in.read());
        final MimeTypeDetector detector = new MimeTypeDetector(singletonMap(Namespaces.GMD, "application/vnd.iso.19139+xml")) {
            @Override int read() throws IOException {
                return in.read();
            }
        };
        final String type = detector.getMimeType();
        assertEquals("application/vnd.iso.19139+xml", type);
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
        try (InputStream in = DefaultExtentTest.getTestFile(true).openStream()) {
            assertEquals('<', in.read());
            assertEquals('?', in.read());
            final MimeTypeDetector detector = new MimeTypeDetector(singletonMap(Namespaces.GMD, "application/vnd.iso.19139+xml")) {
                @Override int read() throws IOException {
                    return in.read();
                }
            };
            type = detector.getMimeType();
        }
        assertEquals("application/vnd.iso.19139+xml", type);
    }
}
