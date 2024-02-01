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
package org.apache.sis.io.stream;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.File;
import java.io.IOException;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link IOUtilities} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public final class IOUtilitiesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public IOUtilitiesTest() {
    }

    /**
     * Tests {@link IOUtilities#filename(Object)}.
     *
     * @throws URISyntaxException if a URI cannot be parsed.
     * @throws MalformedURLException if a URL cannot be parsed.
     */
    @Test
    public void testFilename() throws URISyntaxException, MalformedURLException {
        final URI uri = new URI ("file:/Users/name/Map.png");
        assertEquals("Map.png", IOUtilities.filename(         "/Users/name/Map.png"));
        assertEquals("Map.png", IOUtilities.filename(new File("/Users/name/Map.png")));
        assertEquals("Map.png", IOUtilities.filename(uri));
        assertEquals("Map.png", IOUtilities.filename(uri.toURL()));
        assertEquals("name",    IOUtilities.filename(new URI("file:/Users/name/")));
        assertEquals("",        IOUtilities.filename("/"));
        assertEquals("",        IOUtilities.filename(""));
        assertNull(IOUtilities.filename(Boolean.FALSE));
        assertNull(IOUtilities.filename(null));
    }

    /**
     * Tests {@link IOUtilities#extension(Object)}.
     *
     * @throws URISyntaxException if a URI cannot be parsed.
     * @throws MalformedURLException if a URL cannot be parsed.
     */
    @Test
    @DependsOnMethod("testFilename")
    public void testExtension() throws URISyntaxException, MalformedURLException {
        final URI uri = new URI ("file:/Users/name/Map.png");
        assertEquals("png", IOUtilities.extension(         "/Users/name/Map.png"));
        assertEquals("png", IOUtilities.extension(new File("/Users/name/Map.png")));
        assertEquals("png", IOUtilities.extension(uri));
        assertEquals("png", IOUtilities.extension(uri.toURL()));
        assertEquals("",    IOUtilities.extension(new File("/Users/name/Map")));
        assertEquals("",    IOUtilities.extension(new File("/Users/name/.png")));
        assertNull(IOUtilities.extension(Boolean.FALSE));
        assertNull(IOUtilities.extension(null));
    }

    /**
     * Tests again {@link IOUtilities#filename(Object)} and {@link IOUtilities#extension(Object)}, but with a URI
     * that point to a JAR entry. Such URI are opaque, in which case {@link URI#getPath()} returns {@code null}.
     *
     * @throws URISyntaxException if a URI cannot be parsed.
     * @throws MalformedURLException if a URL cannot be parsed.
     */
    @Test
    @DependsOnMethod({"testFilename", "testExtension"})
    public void testWithOpaqueURI() throws URISyntaxException, MalformedURLException {
        final URI uri = new URI("jar:file:/sis-storage-tests.jar!/org/apache/sis/Any.xml");
        assertTrue(uri.isOpaque()); // This test would be useless if this condition is false.
        assertEquals("Any.xml", IOUtilities.filename (uri));
        assertEquals(    "xml", IOUtilities.extension(uri));

        final URL url = new URI("jar:file:/sis-storage-tests.jar!/org/apache/sis/Any.xml").toURL();
        assertEquals("Any.xml", IOUtilities.filename (url));
        assertEquals(    "xml", IOUtilities.extension(url));
    }

    /**
     * Tests {@link IOUtilities#toString(Object)}.
     *
     * @throws URISyntaxException if a URI cannot be parsed.
     * @throws MalformedURLException if a URL cannot be parsed.
     */
    @Test
    public void testToString() throws URISyntaxException, MalformedURLException {
        // Do not test File because the result is platform-specific.
        final URI uri = new URI ("file:/Users/name/Map.png");
        assertEquals("/Users/name/Map.png",      IOUtilities.toString("/Users/name/Map.png"));
        assertEquals("file:/Users/name/Map.png", IOUtilities.toString(uri));
        assertEquals("file:/Users/name/Map.png", IOUtilities.toString(uri.toURL()));
        assertNull(IOUtilities.toString(Boolean.FALSE));
        assertNull(IOUtilities.toString(null));
    }

    /**
     * Tests {@link IOUtilities#toAuxiliaryURI(URI, String, boolean)}.
     *
     * @throws URISyntaxException if a URI cannot be parsed.
     * @throws MalformedURLException if a URL cannot be parsed.
     */
    @Test
    public void testAuxiliaryURI() throws URISyntaxException, MalformedURLException {
        final var src = new URI("http://localhost/directory/image.tiff?request=ignore.me");
        assertEquals(new URI("http://localhost/directory/image.tfw"),    IOUtilities.toAuxiliaryURI(src, "tfw", true));
        assertEquals(new URI("http://localhost/directory/metadata.xml"), IOUtilities.toAuxiliaryURI(src, "metadata.xml", false));
    }

    /**
     * Tests {@link IOUtilities#filenameWithoutExtension(String)}.
     */
    @Test
    public void testFilenameWithoutExtension() {
        assertEquals("Map",     IOUtilities.filenameWithoutExtension("/Users/name/Map.png"));
        assertEquals("Map",     IOUtilities.filenameWithoutExtension("/Users/name/Map"));
        assertEquals("Map",     IOUtilities.filenameWithoutExtension("Map.png"));
        assertEquals(".hidden", IOUtilities.filenameWithoutExtension("/Users/name/.hidden"));
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
     * Tests {@link IOUtilities#toFileOrURL(String)}.
     * Do not test a Windows-specific path (e.g. {@code "file:///C:/some/path/Map.png"}),
     * because the result is different on Windows or Unix platforms.
     *
     * @throws IOException if a URL cannot be parsed.
     */
    @Test
    @DependsOnMethod("testToFileFromUTF8")
    public void testToFileOrURL() throws IOException {
        assertEquals(new File("/Users/name/Map.png"),        IOUtilities.toFileOrURL("/Users/name/Map.png", null));
        assertEquals(new File("/Users/name/Map.png"),        IOUtilities.toFileOrURL("file:/Users/name/Map.png", null));
        assertEquals(URI.create("http://localhost").toURL(), IOUtilities.toFileOrURL("http://localhost", null));
        assertEquals(new File("/Users/name/Map with spaces.png"),
                IOUtilities.toFileOrURL("file:/Users/name/Map%20with%20spaces.png", "UTF-8"));

        String path = "file:/Users/name/++t--++est.shp";
        var expected = new File("/Users/name/++t--++est.shp");
        assertEquals(expected, IOUtilities.toFileOrURL(path, null));

        path = path.replace("+", "%2B");
        assertEquals(expected, IOUtilities.toFileOrURL(path, "UTF-8"));
    }
}
