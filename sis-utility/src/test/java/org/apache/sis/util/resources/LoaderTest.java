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
package org.apache.sis.util.resources;

import java.util.Locale;
import java.util.List;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link Loader} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 */
public final strictfp class LoaderTest extends TestCase {
    /**
     * Tests the {@link Loader#getCandidateLocales(String, Locale)} method
     * for {@link Locale#US}.
     */
    @Test
    public void testCandidateLocalesForUS() {
        final List<Locale> locales = Loader.INSTANCE.getCandidateLocales(
                "org.apache.sis.util.resources.Vocabulary", Locale.US);
        assertEquals("locales.size()", 3,          locales.size());
        assertEquals("locales[0]", Locale.US,      locales.get(0));
        assertEquals("locales[1]", Locale.ENGLISH, locales.get(1));
        assertEquals("locales[2]", Locale.ROOT,    locales.get(2));
    }
}
