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
package org.apache.sis.internal.util;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.Double.NaN;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Double.NEGATIVE_INFINITY;
import static org.apache.sis.internal.util.Utilities.*;
import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link Utilities} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final strictfp class UtilitiesTest extends TestCase {
    /**
     * Tests the {@link Utilities#epsilonEqual(double, double)} method.
     */
    @Test
    public void testEpsilonEqual() {
        assertTrue (epsilonEqual(POSITIVE_INFINITY, POSITIVE_INFINITY));
        assertTrue (epsilonEqual(NEGATIVE_INFINITY, NEGATIVE_INFINITY));
        assertFalse(epsilonEqual(POSITIVE_INFINITY, NEGATIVE_INFINITY));
        assertFalse(epsilonEqual(POSITIVE_INFINITY, NaN));
        assertTrue (epsilonEqual(NaN,               NaN));
        assertFalse(epsilonEqual(   0,        COMPARISON_THRESHOLD / 2));
        assertTrue (epsilonEqual(   1,    1 + COMPARISON_THRESHOLD / 2));
        assertFalse(epsilonEqual(   1,    1 + COMPARISON_THRESHOLD * 2));
        assertTrue (epsilonEqual(-100, -100 + COMPARISON_THRESHOLD * 50));
        assertFalse(epsilonEqual( 100,  100 + COMPARISON_THRESHOLD * 150));
    }

    /**
     * Tests the {@link Utilities#toString(Class, Object[])} method.
     */
    @Test
    public void testToString() {
        assertEquals("Number[base=“decimal”, value=20]", Utilities.toString(Number.class, "base", "decimal", "value", 20));
    }
}
