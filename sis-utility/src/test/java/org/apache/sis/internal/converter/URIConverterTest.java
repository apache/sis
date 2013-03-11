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
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.test.PlatformDependentTest;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the various {@link URIConverter} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.01)
 * @version 0.3
 * @module
 */
public final strictfp class URIConverterTest extends TestCase {
    /**
     * Asserts that conversion of the given {@code source} value produces
     * the given {@code target} value, and tests the inverse conversion.
     */
    private static <T> void runInvertibleConversion(final ObjectConverter<URI,T> c,
            final URI source, final T target) throws UnconvertibleObjectException
    {
        assertEquals("Forward conversion.", target, c.convert(source));
        assertEquals("Inverse conversion.", source, c.inverse().convert(target));
        assertSame("Inconsistent inverse.", c, c.inverse().inverse());
        assertTrue("Invertible converters shall declare this capability.",
                c.properties().contains(FunctionProperty.INVERTIBLE));
    }

    /**
     * Tests conversions to string values.
     *
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testString() throws URISyntaxException {
        final ObjectConverter<URI,String> c = ObjectToString.URI;
        runInvertibleConversion(c, new URI("file:/home/user/index.txt"), "file:/home/user/index.txt");
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to URL values.
     *
     * @throws MalformedURLException Should never happen.
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testURL() throws MalformedURLException, URISyntaxException {
        final ObjectConverter<URI,URL> c = URIConverter.URL.INSTANCE;
        runInvertibleConversion(c, new URI("file:/home/user/index.txt"), new URL("file:/home/user/index.txt"));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to File values.
     *
     * @throws URISyntaxException Should never happen.
     */
    @Test
    @PlatformDependentTest
    public void testFile() throws URISyntaxException {
        FileConverterTest.assumeUnixRoot();
        final ObjectConverter<URI,File> c = URIConverter.File.INSTANCE;
        runInvertibleConversion(c, new URI("file:/home/user/index.txt"), new File("/home/user/index.txt"));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }
}
