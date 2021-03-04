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

import java.nio.charset.StandardCharsets;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link StoreUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class StoreUtilitiesTest extends TestCase {
    /**
     * Tests {@link StoreUtilities#basedOnASCII(Charset)}.
     */
    @Test
    public void testBasedOnASCII() {
        assertTrue (StoreUtilities.basedOnASCII(StandardCharsets.US_ASCII));
        assertTrue (StoreUtilities.basedOnASCII(StandardCharsets.ISO_8859_1));
        assertTrue (StoreUtilities.basedOnASCII(StandardCharsets.UTF_8));
        assertFalse(StoreUtilities.basedOnASCII(StandardCharsets.UTF_16));
        assertFalse(StoreUtilities.basedOnASCII(StandardCharsets.UTF_16BE));
        assertFalse(StoreUtilities.basedOnASCII(StandardCharsets.UTF_16LE));
    }
}
