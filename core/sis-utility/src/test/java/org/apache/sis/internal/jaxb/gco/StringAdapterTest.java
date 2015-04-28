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
package org.apache.sis.internal.jaxb.gco;

import java.util.Locale;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link StringAdapter}
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class StringAdapterTest extends TestCase {
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
     * Tests {@link StringAdapter#toString(CharSequence)} for an {@link InternationalString}
     * having loalization in different languages.
     */
    @Test
    @DependsOnMethod("testToUnlocalizedString")
    public void testToLocalizedString() {
        final DefaultInternationalString i18n = new DefaultInternationalString();
        i18n.add(Locale.ENGLISH,  "A word");
        i18n.add(Locale.FRENCH,   "Un mot");
        i18n.add(Locale.JAPANESE, "言葉");
        Context.push(Locale.JAPANESE);
        try {
            assertEquals("言葉", StringAdapter.toString(i18n));
            Context.push(Locale.FRENCH);
            try {
                assertEquals("Un mot", StringAdapter.toString(i18n));
                Context.push(Locale.ENGLISH);
                try {
                    assertEquals("A word", StringAdapter.toString(i18n));
                } finally {
                    Context.pull();
                }
                assertEquals("Un mot", StringAdapter.toString(i18n));
            } finally {
                Context.pull();
            }
            assertEquals("言葉", StringAdapter.toString(i18n));
        } finally {
            Context.pull();
        }
    }
}
