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
package org.apache.sis.internal.converter;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.test.PlatformDependent;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assume.*;
import static org.apache.sis.test.Assert.*;


/**
 * Tests the various {@link PathConverter} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(StringConverterTest.class)
public final strictfp class PathConverterTest extends TestCase {
    /**
     * Assumes that the platform file system has a Unix-style root.
     * Windows platform has driver letters instead, like "C:\\",
     * which are not correctly tested by this class.
     */
    static void assumeUnixRoot() {
        assumeTrue(ArraysExt.contains(File.listRoots(), new File("/")));
    }

    /**
     * Asserts that conversion of the given {@code source} value produces
     * the given {@code target} value, and tests the inverse conversion.
     */
    private static <S,T> void runInvertibleConversion(final ObjectConverter<S,T> c,
            final S source, final T target) throws UnconvertibleObjectException
    {
        assertEquals("Forward conversion.", target, c.apply(source));
        assertEquals("Inverse conversion.", source, c.inverse().apply(target));
        assertSame("Inconsistent inverse.", c, c.inverse().inverse());
        assertTrue("Invertible converters shall declare this capability.",
                c.properties().contains(FunctionProperty.INVERTIBLE));
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
     * @throws URISyntaxException Should never happen.
     */
    @Test
    @PlatformDependent
    public void testFile_URI() throws URISyntaxException {
        assumeUnixRoot();
        final ObjectConverter<File,URI> c = PathConverter.FileURI.INSTANCE;
        runInvertibleConversion(c, new File("/home/user/index.txt"), new URI("file:/home/user/index.txt"));
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from File to URL values.
     *
     * @throws MalformedURLException Should never happen.
     */
    @Test
    @PlatformDependent
    public void testFile_URL() throws MalformedURLException {
        assumeUnixRoot();
        final ObjectConverter<File,URL> c = PathConverter.FileURL.INSTANCE;
        runInvertibleConversion(c, new File("/home/user/index.txt"), new URL("file:/home/user/index.txt"));
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from URI to string values.
     *
     * @throws URISyntaxException Should never happen.
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
     * @throws MalformedURLException Should never happen.
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testURI_URL() throws MalformedURLException, URISyntaxException {
        final ObjectConverter<URI,URL> c = PathConverter.URI_URL.INSTANCE;
        runInvertibleConversion(c, new URI("file:/home/user/index.txt"), new URL("file:/home/user/index.txt"));
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from URI to File values.
     *
     * @throws URISyntaxException Should never happen.
     */
    @Test
    @PlatformDependent
    public void testURI_File() throws URISyntaxException {
        assumeUnixRoot();
        final ObjectConverter<URI,File> c = PathConverter.URIFile.INSTANCE;
        runInvertibleConversion(c, new URI("file:/home/user/index.txt"), new File("/home/user/index.txt"));
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from URL to string values.
     *
     * @throws MalformedURLException Should never happen.
     */
    @Test
    public void testURL_String() throws MalformedURLException {
        final ObjectConverter<URL,String> c = new StringConverter.URL().inverse();
        runInvertibleConversion(c, new URL("file:/home/user/index.txt"), "file:/home/user/index.txt");
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from URL to URI values.
     *
     * @throws MalformedURLException Should never happen.
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testURL_URI() throws MalformedURLException, URISyntaxException {
        final ObjectConverter<URL,URI> c = PathConverter.URL_URI.INSTANCE;
        runInvertibleConversion(c, new URL("file:/home/user/index.txt"), new URI("file:/home/user/index.txt"));
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions from URL to File values.
     *
     * @throws MalformedURLException Should never happen.
     */
    @Test
    @PlatformDependent
    public void testURL_File() throws MalformedURLException {
        assumeUnixRoot();
        final ObjectConverter<URL,File> c = PathConverter.URLFile.INSTANCE;
        runInvertibleConversion(c, new URL("file:/home/user/index.txt"), new File("/home/user/index.txt"));
        assertSerializedEquals(c);
    }
}
