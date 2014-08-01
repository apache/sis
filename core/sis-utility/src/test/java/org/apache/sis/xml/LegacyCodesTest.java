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

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.xml.LegacyCodes.*;


/**
 * Tests {@link LegacyCodes}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class LegacyCodesTest extends TestCase {
    /**
     * Tests {@link LegacyCodes#fromIANA(String)}.
     */
    @Test
    public void testFromIANA() {
        assertEquals("UTF-8",       "utf8",       fromIANA("UTF-8"));
        assertEquals("UTF-16",      "utf16",      fromIANA("UTF-16"));
        assertEquals("ISO-8859-1",  "8859part1",  fromIANA("ISO-8859-1"));
        assertEquals("JIS_X0201",   "jis",        fromIANA("JIS_X0201"));
        assertEquals("Shift_JIS",   "shiftJIS",   fromIANA("Shift_JIS"));

        // Test non-standard cases.
        assertEquals("utf-8",       "utf8",       fromIANA("utf-8"));
        assertEquals("shift_jis",   "shiftJIS",   fromIANA("shift_jis"));
        assertEquals("SHIFT_JIS",   "shiftJIS",   fromIANA("SHIFT_JIS"));
    }

    /**
     * Tests {@link LegacyCodes#toIANA(String)}.
     */
    @Test
    public void testToIANA() {
        assertEquals("ucs2",       "UCS-2",       toIANA("ucs2"));
        assertEquals("ucs4",       "UCS-4",       toIANA("ucs4"));
        assertEquals("utf7",       "UTF-7",       toIANA("utf7"));
        assertEquals("utf8",       "UTF-8",       toIANA("utf8"));
        assertEquals("utf16",      "UTF-16",      toIANA("utf16"));
        assertEquals("8859part1",  "ISO-8859-1",  toIANA("8859part1"));
        assertEquals("8859part2",  "ISO-8859-2",  toIANA("8859part2"));
        assertEquals("8859part3",  "ISO-8859-3",  toIANA("8859part3"));
        assertEquals("8859part4",  "ISO-8859-4",  toIANA("8859part4"));
        assertEquals("8859part5",  "ISO-8859-5",  toIANA("8859part5"));
        assertEquals("8859part6",  "ISO-8859-6",  toIANA("8859part6"));
        assertEquals("8859part7",  "ISO-8859-7",  toIANA("8859part7"));
        assertEquals("8859part8",  "ISO-8859-8",  toIANA("8859part8"));
        assertEquals("8859part9",  "ISO-8859-9",  toIANA("8859part9"));
        assertEquals("8859part10", "ISO-8859-10", toIANA("8859part10"));
        assertEquals("8859part11", "ISO-8859-11", toIANA("8859part11"));
        assertEquals("8859part12", "ISO-8859-12", toIANA("8859part12"));
        assertEquals("8859part13", "ISO-8859-13", toIANA("8859part13"));
        assertEquals("8859part14", "ISO-8859-14", toIANA("8859part14"));
        assertEquals("8859part15", "ISO-8859-15", toIANA("8859part15"));
        assertEquals("8859part16", "ISO-8859-16", toIANA("8859part16"));
        assertEquals("jis",        "JIS_X0201",   toIANA("jis"));
        assertEquals("shiftJIS",   "Shift_JIS",   toIANA("shiftJIS"));
        assertEquals("eucJP",      "EUC-JP",      toIANA("eucJP"));
        assertEquals("usAscii",    "US-ASCII",    toIANA("usAscii"));
        assertEquals("ebcdic",     "EBCDIC",      toIANA("ebcdic"));
        assertEquals("eucKR",      "EUC-KR",      toIANA("eucKR"));
        assertEquals("big5",       "Big5",        toIANA("big5"));
        assertEquals("GB2312",     "GB2312",      toIANA("GB2312"));

        // Test non-standard cases.
        assertEquals("UCS2",       "UCS-2",       toIANA("UCS2"));
        assertEquals("euckr",      "EUC-KR",      toIANA("euckr"));
        assertEquals("EUCKR",      "EUC-KR",      toIANA("EUCKR"));
    }
}
