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
package org.apache.sis.xml.bind.gco;

import java.util.Locale;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.DefaultInternationalString;
import org.apache.sis.xml.bind.Context;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link StringAdapter}
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class StringAdapterTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public StringAdapterTest() {
    }

    /**
     * Tests {@link StringAdapter#toString(CharSequence)} for the trivial cases.
     */
    @Test
    public void testToUnlocalizedString() {
        assertNull  (        StringAdapter.toString((CharSequence) null));
        assertEquals("Test", StringAdapter.toString("Test"));
        assertEquals("Test", StringAdapter.toString(new SimpleInternationalString("Test")));
    }

    /**
     * Tests {@link StringAdapter#toString(CharSequence)} for an {@link org.opengis.util.InternationalString}
     * having localizations in different languages.
     */
    @Test
    public void testToLocalizedString() {
        final var i18n = new DefaultInternationalString();
        i18n.add(Locale.ENGLISH,  "A word");
        i18n.add(Locale.FRENCH,   "Un mot");
        i18n.add(Locale.JAPANESE, "言葉");
        final Context context = new Context(0, null, Locale.ENGLISH, null, null, null, null, null, null, null, null);
        try {
            Context.push(Locale.JAPANESE);  assertEquals("言葉",    StringAdapter.toString(i18n));
            Context.push(Locale.FRENCH);    assertEquals("Un mot", StringAdapter.toString(i18n));
            Context.push(Locale.ENGLISH);   assertEquals("A word", StringAdapter.toString(i18n));
            Context.pull();                 assertEquals("Un mot", StringAdapter.toString(i18n));
            Context.pull();                 assertEquals("言葉",    StringAdapter.toString(i18n));
            Context.pull();                 assertEquals("A word", StringAdapter.toString(i18n));
        } finally {
            context.finish();
        }
    }
}
