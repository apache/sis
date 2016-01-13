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
package org.apache.sis.internal.metadata.sql;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link SQLUtilities} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class SQLUtilitiesTest extends TestCase {
    /**
     * Tests {@link SQLUtilities#toLikePattern(String)}.
     */
    @Test
    public void testToLikePattern() {
        assertEquals("WGS84",                       SQLUtilities.toLikePattern("WGS84"));
        assertEquals("WGS%84",                      SQLUtilities.toLikePattern("WGS 84"));
        assertEquals("A%text%with%random%symbols%", SQLUtilities.toLikePattern("A text !* with_random:/symbols;+"));
        assertEquals("*_+_=With%non%letter%start",  SQLUtilities.toLikePattern("*_+%=With non-letter  start"));
    }
}
