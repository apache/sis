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
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.test.PlatformDependentTest;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the various {@link URLConverter} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.01)
 * @version 0.3
 * @module
 */
public final strictfp class URLConverterTest extends TestCase {
    /**
     * Tests conversions to string values.
     *
     * @throws UnconvertibleObjectException Should never happen.
     * @throws MalformedURLException Should never happen.
     */
    @Test
    public void testString() throws UnconvertibleObjectException, MalformedURLException {
        final URL    source = new URL("file:/home/user/index.txt");
        final String target = "file:/home/user/index.txt";
        final ObjectConverter<URL,String> c = URLConverter.String.INSTANCE;
        assertEquals("Forward conversion", target, c.convert(source));
        assertEquals("Inverse conversion", source, c.inverse().convert(target));
        assertSame(c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to URI values.
     *
     * @throws UnconvertibleObjectException Should never happen.
     * @throws MalformedURLException Should never happen.
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testURI() throws UnconvertibleObjectException, MalformedURLException, URISyntaxException {
        final URL source = new URL("file:/home/user/index.txt");
        final URI target = new URI("file:/home/user/index.txt");
        final ObjectConverter<URL,URI> c = URLConverter.URI.INSTANCE;
        assertEquals("Forward conversion", target, c.convert(source));
        assertEquals("Inverse conversion", source, c.inverse().convert(target));
        assertSame(c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to File values.
     *
     * @throws UnconvertibleObjectException Should never happen.
     * @throws MalformedURLException Should never happen.
     */
    @Test
    @PlatformDependentTest
    public void testFile() throws UnconvertibleObjectException, MalformedURLException {
        FileConverterTest.assumeUnixRoot();
        final URL  source = new URL("file:/home/user/index.txt");
        final File target = new File("/home/user/index.txt");
        final ObjectConverter<URL,File> c = URLConverter.File.INSTANCE;
        assertEquals("Forward conversion", target, c.convert(source));
        assertEquals("Inverse conversion", source, c.inverse().convert(target));
        assertSame(c, assertSerializedEquals(c));
    }
}
