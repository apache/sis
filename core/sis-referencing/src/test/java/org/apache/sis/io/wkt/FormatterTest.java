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
package org.apache.sis.io.wkt;

import org.apache.sis.internal.util.X364;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Formatter} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class FormatterTest extends TestCase {
    /**
     * Verifies the ANSI escape sequences hard-coded in {@link Formatter}.
     */
    @Test
    public void testAnsiEscapeSequences() {
        assertEquals("FOREGROUND_DEFAULT", X364.FOREGROUND_DEFAULT.sequence(), Formatter.FOREGROUND_DEFAULT);
        assertEquals("BACKGROUND_DEFAULT", X364.BACKGROUND_DEFAULT.sequence(), Formatter.BACKGROUND_DEFAULT);
    }
}
