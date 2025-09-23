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
package org.apache.sis.util.internal.shared;

import java.util.Locale;
import java.text.ParsePosition;
import org.apache.sis.measure.Angle;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Errors;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link LocalizedParseException} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class LocalizedParseExceptionTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public LocalizedParseExceptionTest() {
    }

    /**
     * Tests the {@link LocalizedParseException} constructor using the default string.
     * This method also tests {@link Exceptions#getLocalizedMessage(Throwable, Locale)}
     * as a side-effect.
     */
    @Test
    public void testAutomaticMessage() {
        final var pos = new ParsePosition(0);
        pos.setErrorIndex(5);
        final var e = new LocalizedParseException(Locale.CANADA, Angle.class, "Some text to parse", pos);
        String message = e.getLocalizedMessage();
        assertTrue(message.contains("Some text to parse"), message);
        assertTrue(message.contains("cannot be parsed"), message);
        assertTrue(message.contains("Angle"), message);

        assertEquals(message, Exceptions.getLocalizedMessage(e, Locale.CANADA));
        message = Exceptions.getLocalizedMessage(e, Locale.FRANCE);
        assertTrue(message.contains("Some text to parse"), message);
        assertTrue(message.contains("n’est pas reconnu"), message);
        assertTrue(message.contains("Angle"), message);
    }

    /**
     * Tests the {@link LocalizedParseException} constructor using a given resource key
     * and the text that we failed to parse.
     */
    @Test
    public void testResourceKeyForText() {
        final var e = new LocalizedParseException(
                Locale.CANADA, Errors.Keys.NodeHasNoParent_1, new Object[] {"text"}, 5);
        String message = e.getLocalizedMessage();
        assertTrue(message.contains("Node “text” has no parent."), message);
    }
}
