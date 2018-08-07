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

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link CodeType}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class CodeTypeTest  extends TestCase {
    /**
     * The expected value.
     */
    private CodeType expected;

    /**
     * Asserts that {@link CodeType#guess(String)} returns the expected value.
     */
    private void verify(final String code) {
        assertEquals(code, expected, CodeType.guess(code));
    }

    /**
     * Tests {@link CodeType#guess(String)} for the {@link CodeType#IDENTIFIER} type.
     */
    @Test
    public void testCRS() {
        expected = CodeType.IDENTIFIER;
        verify("EPSG:4326");
        verify("epsg:4326");
        verify(" ePsG : 4326 ");
    }

    /**
     * Tests {@link CodeType#guess(String)} for the {@link CodeType#URN} type.
     */
    @Test
    public void testURN() {
        expected = CodeType.URN;
        verify("urn:ogc:def:crs:epsg::4326");
        verify("URN:OGC:DEF:CRS:EPSG::4326");
        verify("  uRn :ogc:def:crs:ogc::84");
    }

    /**
     * Tests {@link CodeType#guess(String)} for the {@link CodeType#URL} type.
     */
    @Test
    public void testURL() {
        expected = CodeType.URL;
        verify("ftp://server/file.txt");
        verify("http://server/file.txt");
        verify("test://server/file.txt");
        verify(" http : // server/file.txt");
        verify("file:./file.txt");
        verify("test:./file.txt");
        verify("jar:file.txt");       // Special case required by some tests.
    }

    /**
     * Tests {@link CodeType#guess(String)} for the {@link CodeType#HTTP_OGC} type.
     */
    @Test
    public void testHTTP() {
        expected = CodeType.HTTP_OGC;
        verify("http: // www.opengis.net/");
        verify("http://www.opengis.net/gml/srs/epsg.xml#4326");
    }

    /**
     * Tests {@link CodeType#guess(String)} for the {@link CodeType#FILE} type.
     */
    @Test
    public void testFile() {
        expected = CodeType.FILE;
        verify("path/file.txt");
        verify("../file.txt");
        verify("..\\file.txt");
        verify("C:file.txt");
        verify("C:.\\file.txt");
        verify("ABC:.\\file.txt");
        verify("C : \\path\\file.txt");
        verify("ABC : \\path\\file.txt");
        verify("C:..");
        verify("ABC:..");
    }

    /**
     * Tests {@link CodeType#guess(String)} for the {@link CodeType#IDENTIFIER} type.
     */
    @Test
    public void testIdentifier() {
        expected = CodeType.IDENTIFIER;
        verify("EPSG:4326");
        verify("AB:C\\D.x");
        verify("AB:C/D.x");
    }

    /**
     * Tests {@link CodeType#guess(String)} for the {@link CodeType#UNKNOWN} type.
     */
    @Test
    public void testUnknown() {
        expected = CodeType.UNKNOWN;
        verify("4326");
        verify("file.txt");
    }
}
