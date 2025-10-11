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
package org.apache.sis.converter;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.condition.DisabledOnOs;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the various {@link PathConverter} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class PathConverterTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public PathConverterTest() {
    }

    /**
     * Asserts that conversion of the given {@code source} value produces
     * the given {@code target} value, and tests the inverse conversion.
     */
    private static <S,T> void runInvertibleConversion(final ObjectConverter<S,T> c,
            final S source, final T target) throws UnconvertibleObjectException
    {
        assertEquals(target, c.apply(source), "Forward conversion.");
        assertEquals(source, c.inverse().apply(target), "Inverse conversion.");
        assertSame(c, c.inverse().inverse(), "Inconsistent inverse.");
        assertTrue(c.properties().contains(FunctionProperty.INVERTIBLE),
                   "Invertible converters shall declare this capability.");
    }

    /**
     * Tests conversions from File to string values.
     */
    @Test
    public void testFile_String() {
        final ObjectConverter<File,String> c = new StringConverter.File().inverse();
        runInvertibleConversion(c, new File("home/user/index.txt"), "home/user/index.txt".replace('/', File.separatorChar));
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from File to URI values.
     *
     * @throws URISyntaxException if this test uses a malformed URI.
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testFile_URI() throws URISyntaxException {
        final ObjectConverter<File,URI> c = PathConverter.FileURI.INSTANCE;
        runInvertibleConversion(c, new File("/home/user/index.txt"), new URI("file:/home/user/index.txt"));
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from File to URL values.
     *
     * @throws MalformedURLException if this test uses a malformed URL.
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testFile_URL() throws MalformedURLException {
        final ObjectConverter<File,URL> c = PathConverter.FileURL.INSTANCE;
        runInvertibleConversion(c, new File("/home/user/index.txt"), URI.create("file:/home/user/index.txt").toURL());
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from URI to string values.
     *
     * @throws URISyntaxException if this test uses a malformed URI.
     */
    @Test
    public void testURI_String() throws URISyntaxException {
        final ObjectConverter<URI,String> c = new StringConverter.URI().inverse();
        runInvertibleConversion(c, new URI("file:/home/user/index.txt"), "file:/home/user/index.txt");
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from URI to URL values.
     *
     * @throws MalformedURLException if this test uses a malformed URL.
     * @throws URISyntaxException if this test uses a malformed URI.
     */
    @Test
    public void testURI_URL() throws MalformedURLException, URISyntaxException {
        final ObjectConverter<URI,URL> c = PathConverter.URI_URL.INSTANCE;
        runInvertibleConversion(c, new URI("file:/home/user/index.txt"), URI.create("file:/home/user/index.txt").toURL());
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from URI to File values.
     *
     * @throws URISyntaxException if this test uses a malformed URI.
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testURI_File() throws URISyntaxException {
        final ObjectConverter<URI,File> c = PathConverter.URIFile.INSTANCE;
        runInvertibleConversion(c, new URI("file:/home/user/index.txt"), new File("/home/user/index.txt"));
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from URL to string values.
     *
     * @throws MalformedURLException if this test uses a malformed URL.
     */
    @Test
    public void testURL_String() throws MalformedURLException {
        final ObjectConverter<URL,String> c = new StringConverter.URL().inverse();
        runInvertibleConversion(c, URI.create("file:/home/user/index.txt").toURL(), "file:/home/user/index.txt");
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from URL to URI values.
     *
     * @throws MalformedURLException if this test uses a malformed URL.
     * @throws URISyntaxException if this test uses a malformed URI.
     */
    @Test
    public void testURL_URI() throws MalformedURLException, URISyntaxException {
        final URI uri = new URI("file:/home/user/index.txt");
        final ObjectConverter<URL,URI> c = PathConverter.URL_URI.INSTANCE;
        runInvertibleConversion(c, uri.toURL(), uri);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from URL to File values.
     *
     * @throws MalformedURLException if this test uses a malformed URL.
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testURL_File() throws MalformedURLException {
        final ObjectConverter<URL,File> c = PathConverter.URLFile.INSTANCE;
        runInvertibleConversion(c, URI.create("file:/home/user/index.txt").toURL(), new File("/home/user/index.txt"));
        assertSerializedEquals(c);
    }
}
