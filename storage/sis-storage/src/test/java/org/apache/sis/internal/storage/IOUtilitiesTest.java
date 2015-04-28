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
package org.apache.sis.internal.storage;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.sis.util.CharSequences;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link IOUtilities} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final strictfp class IOUtilitiesTest extends TestCase {
    /**
     * Tests {@link IOUtilities#filename(Object)}.
     *
     * @throws URISyntaxException Should never happen.
     * @throws MalformedURLException Should never happen.
     */
    @Test
    public void testFilename() throws URISyntaxException, MalformedURLException {
        assertEquals("Map.png", IOUtilities.filename(              "/Users/name/Map.png"));
        assertEquals("Map.png", IOUtilities.filename(new File(     "/Users/name/Map.png")));
        assertEquals("Map.png", IOUtilities.filename(new URI ("file:/Users/name/Map.png")));
        assertEquals("Map.png", IOUtilities.filename(new URL ("file:/Users/name/Map.png")));
        assertNull(IOUtilities.filename(Boolean.FALSE));
        assertNull(IOUtilities.filename(null));
    }

    /**
     * Tests {@link IOUtilities#extension(Object)}.
     *
     * @throws URISyntaxException Should never happen.
     * @throws MalformedURLException Should never happen.
     */
    @Test
    @DependsOnMethod("testFilename")
    public void testExtension() throws URISyntaxException, MalformedURLException {
        assertEquals("png", IOUtilities.extension(              "/Users/name/Map.png"));
        assertEquals("png", IOUtilities.extension(new File(     "/Users/name/Map.png")));
        assertEquals("png", IOUtilities.extension(new URI ("file:/Users/name/Map.png")));
        assertEquals("png", IOUtilities.extension(new URL ("file:/Users/name/Map.png")));
        assertEquals("",    IOUtilities.extension(new File(     "/Users/name/Map")));
        assertEquals("",    IOUtilities.extension(new File(     "/Users/name/.png")));
        assertNull(IOUtilities.extension(Boolean.FALSE));
        assertNull(IOUtilities.extension(null));
    }

    /**
     * Tests again {@link IOUtilities#filename(Object)} and {@link IOUtilities#extension(Object)}, but with a URI
     * that point to a JAR entry. Such URI are opaque, in which case {@link URI#getPath()} returns {@code null}.
     *
     * @throws URISyntaxException Should never happen.
     * @throws MalformedURLException Should never happen.
     */
    @Test
    @DependsOnMethod({"testFilename", "testExtension"})
    public void testWithOpaqueURI() throws URISyntaxException, MalformedURLException {
        final URI uri = new URI("jar:file:/sis-storage-tests.jar!/org/apache/sis/Any.xml");
        assertTrue(uri.isOpaque()); // This test would be useless if this condition is false.
        assertEquals("Any.xml", IOUtilities.filename (uri));
        assertEquals(    "xml", IOUtilities.extension(uri));

        final URL url = new URL("jar:file:/sis-storage-tests.jar!/org/apache/sis/Any.xml");
        assertEquals("Any.xml", IOUtilities.filename (url));
        assertEquals(    "xml", IOUtilities.extension(url));
    }

    /**
     * Tests {@link IOUtilities#toString(Object)}.
     *
     * @throws URISyntaxException Should never happen.
     * @throws MalformedURLException Should never happen.
     */
    @Test
    public void testToString() throws URISyntaxException, MalformedURLException {
        // Do not test File because the result is platform-specific.
        assertEquals("/Users/name/Map.png",      IOUtilities.toString(              "/Users/name/Map.png"));
        assertEquals("file:/Users/name/Map.png", IOUtilities.toString(new URI ("file:/Users/name/Map.png")));
        assertEquals("file:/Users/name/Map.png", IOUtilities.toString(new URL ("file:/Users/name/Map.png")));
        assertNull(IOUtilities.toString(Boolean.FALSE));
        assertNull(IOUtilities.toString(null));
    }

    /**
     * Tests {@link IOUtilities#encodeURI(String)}.
     */
    @Test
    public void testEncodeURI() {
        assertEquals("/Users/name/Map%20with%20spaces.png", IOUtilities.encodeURI("/Users/name/Map with spaces.png"));
        assertNull(IOUtilities.encodeURI(null));
    }

    /**
     * Tests {@link IOUtilities#toURI(URL, String)}.
     *
     * @throws IOException Should not happen.
     * @throws URISyntaxException Should not happen.
     */
    @Test
    @DependsOnMethod("testEncodeURI")
    public void testToURI() throws IOException, URISyntaxException {
        assertEquals(new URI("file:/Users/name/Map.png"),
                IOUtilities.toURI(new URL("file:/Users/name/Map.png"), null));
        assertEquals(new URI("file:/Users/name/Map%20with%20spaces.png"),
                IOUtilities.toURI(new URL("file:/Users/name/Map with spaces.png"), null));
        assertEquals(new URI("file:/Users/name/Map%20with%20spaces.png"),
                IOUtilities.toURI(new URL("file:/Users/name/Map%20with%20spaces.png"), "UTF-8"));
        assertEquals(new URI("file:/Users/name/Map%20with%20spaces.png"),
                IOUtilities.toURI(new URL("file:/Users/name/Map%20with%20spaces.png"), "ISO-8859-1"));

        // Here the URL is considered non-encoded, so the method shall encode the % sign.
        assertEquals(new URI("file:/Users/name/Map%2520with%2520spaces.png"),
                IOUtilities.toURI(new URL("file:/Users/name/Map%20with%20spaces.png"), null));
    }

    /**
     * Tests the {@link IOUtilities#toFile(URL, String)} method. Do not test a Windows-specific path
     * (e.g. {@code "file:///C:/some/path/Map.png"}), since the result is different on Windows or
     * Unix platforms.
     *
     * @throws IOException Should not happen.
     */
    @Test
    @DependsOnMethod("testToURI")
    public void testToFile() throws IOException {
        testToFile(null, "+");
    }

    /**
     * Same test than {@link #testToFile()}, but using the UTF-8 encoding.
     *
     * @throws IOException Should never happen.
     */
    @Test
    @DependsOnMethod("testToFile")
    public void testToFileFromUTF8() throws IOException {
        testToFile("UTF-8", "%2B");
    }

    /**
     * Implementation of {@link #testToFile()} using the given encoding.
     * If the encoding is null, then the {@code URLDecoder} will not be used.
     *
     * @param  encoding The encoding, or {@code null} if none.
     * @param  plus The representation for the {@code '+'} sign.
     * @throws IOException Should not happen.
     */
    private void testToFile(final String encoding, final String plus) throws IOException {
        assertEquals("Unix absolute path.", new File("/Users/name/Map.png"),
                IOUtilities.toFile(new URL("file:/Users/name/Map.png"), encoding));
        assertEquals("Path with space.", new File("/Users/name/Map with spaces.png"),
                IOUtilities.toFile(new URL("file:/Users/name/Map with spaces.png"), encoding));
        assertEquals("Path with + sign.", new File("/Users/name/++t--++est.shp"),
                IOUtilities.toFile(new URL(CharSequences.replace("file:/Users/name/++t--++est.shp", "+", plus).toString()), encoding));
    }

    /**
     * Tests {@link IOUtilities#toFileOrURL(String, String)}.
     *
     * @throws IOException Should not happen.
     */
    @Test
    @DependsOnMethod("testToFileFromUTF8")
    public void testToFileOrURL() throws IOException {
        assertEquals(new File("/Users/name/Map.png"), IOUtilities.toFileOrURL("/Users/name/Map.png", null));
        assertEquals(new File("/Users/name/Map.png"), IOUtilities.toFileOrURL("file:/Users/name/Map.png", null));
        assertEquals(new URL("http://localhost"),     IOUtilities.toFileOrURL("http://localhost", null));
        assertEquals(new File("/Users/name/Map with spaces.png"),
                IOUtilities.toFileOrURL("file:/Users/name/Map%20with%20spaces.png", "UTF-8"));
    }
}
