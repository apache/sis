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
package org.apache.sis.metadata.internal.shared;

import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import static java.util.Locale.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link ImplementationHelper} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ImplementationHelperTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ImplementationHelperTest() {
    }

    /**
     * Tests the {@link ImplementationHelper#setFirst(Collection, Object)} method.
     */
    @Test
    public void testSetFirst() {
        Collection<Locale> locales = ImplementationHelper.setFirst(null, null);
        assertTrue(locales.isEmpty());

        locales = ImplementationHelper.setFirst(null, GERMAN);
        assertArrayEquals(new Locale[] {GERMAN}, locales.toArray());

        locales = Arrays.asList(ENGLISH, JAPANESE, FRENCH);                 // Content will be modified.
        assertSame(locales, ImplementationHelper.setFirst(locales, GERMAN), "Shall set value in-place.");
        assertArrayEquals(new Locale[] {GERMAN, JAPANESE, FRENCH}, locales.toArray());

        locales = new LinkedHashSet<>(List.of(ENGLISH, JAPANESE, FRENCH));
        locales = ImplementationHelper.setFirst(locales, ITALIAN);
        assertArrayEquals(new Locale[] {ITALIAN, JAPANESE, FRENCH}, locales.toArray());

        locales = ImplementationHelper.setFirst(locales, null);
        assertArrayEquals(new Locale[] {JAPANESE, FRENCH}, locales.toArray());
    }
}
