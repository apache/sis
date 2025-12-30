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
package org.apache.sis.xml;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link LegacyCodes}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class LegacyCodesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public LegacyCodesTest() {
    }

    /**
     * Tests {@link LegacyCodes#fromIANA(String)} with the given value.
     */
    private static void testFromIANA(final String expected, final String name) {
        assertEquals(expected, LegacyCodes.fromIANA(name), name);
    }

    /**
     * Tests {@link LegacyCodes#fromIANA(String)}.
     */
    @Test
    public void testFromIANA() {
        testFromIANA("utf8",       "UTF-8");
        testFromIANA("utf16",      "UTF-16");
        testFromIANA("8859part1",  "ISO-8859-1");
        testFromIANA("jis",        "JIS_X0201");
        testFromIANA("shiftJIS",   "Shift_JIS");

        // Test non-standard cases.
        testFromIANA("utf8",       "utf-8");
        testFromIANA("shiftJIS",   "shift_jis");
        testFromIANA("shiftJIS",   "SHIFT_JIS");
    }

    /**
     * Tests {@link LegacyCodes#toIANA(String)} with the given value.
     */
    private static void testToIANA(final String expected, final String name) {
        assertEquals(expected, LegacyCodes.toIANA(name), name);
    }

    /**
     * Tests {@link LegacyCodes#toIANA(String)}.
     */
    @Test
    public void testToIANA() {
        testToIANA("UCS-2",       "ucs2");
        testToIANA("UCS-4",       "ucs4");
        testToIANA("UTF-7",       "utf7");
        testToIANA("UTF-8",       "utf8");
        testToIANA("UTF-16",      "utf16");
        testToIANA("ISO-8859-1",  "8859part1");
        testToIANA("ISO-8859-2",  "8859part2");
        testToIANA("ISO-8859-3",  "8859part3");
        testToIANA("ISO-8859-4",  "8859part4");
        testToIANA("ISO-8859-5",  "8859part5");
        testToIANA("ISO-8859-6",  "8859part6");
        testToIANA("ISO-8859-7",  "8859part7");
        testToIANA("ISO-8859-8",  "8859part8");
        testToIANA("ISO-8859-9",  "8859part9");
        testToIANA("ISO-8859-10", "8859part10");
        testToIANA("ISO-8859-11", "8859part11");
        testToIANA("ISO-8859-12", "8859part12");
        testToIANA("ISO-8859-13", "8859part13");
        testToIANA("ISO-8859-14", "8859part14");
        testToIANA("ISO-8859-15", "8859part15");
        testToIANA("ISO-8859-16", "8859part16");
        testToIANA("JIS_X0201",   "jis");
        testToIANA("Shift_JIS",   "shiftJIS");
        testToIANA("EUC-JP",      "eucJP");
        testToIANA("US-ASCII",    "usAscii");
        testToIANA("EBCDIC",      "ebcdic");
        testToIANA("EUC-KR",      "eucKR");
        testToIANA("Big5",        "big5");
        testToIANA("GB2312",      "GB2312");

        // Test non-standard cases.
        testToIANA("UCS-2",  "UCS2");
        testToIANA("EUC-KR", "euckr");
        testToIANA("EUC-KR", "EUCKR");
    }
}
