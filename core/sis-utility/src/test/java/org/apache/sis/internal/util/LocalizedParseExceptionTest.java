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

import java.util.Locale;
import java.text.ParseException;
import java.text.ParsePosition;
import org.apache.sis.measure.Angle;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Exceptions;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link LocalizedParseException} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(org.apache.sis.util.resources.IndexedResourceBundleTest.class)
public final strictfp class LocalizedParseExceptionTest extends TestCase {
    /**
     * Tests the {@link LocalizedParseException} constructor using the default string.
     * This method also tests {@link Exceptions#getLocalizedMessage(Throwable, Locale)}
     * as a side-effect.
     */
    @Test
    public void testAutomaticMessage() {
        final ParsePosition pos = new ParsePosition(0);
        pos.setErrorIndex(5);
        final ParseException e = new LocalizedParseException(
                Locale.CANADA, Angle.class, "Some text to parse", pos);
        String message = e.getLocalizedMessage();
        assertTrue(message, message.contains("Some text to parse"));
        assertTrue(message, message.contains("can not be parsed"));
        assertTrue(message, message.contains("Angle"));

        assertEquals(message, Exceptions.getLocalizedMessage(e, Locale.CANADA));
        message = Exceptions.getLocalizedMessage(e, Locale.FRANCE);
        assertTrue(message, message.contains("Some text to parse"));
        assertTrue(message, message.contains("n’est pas reconnu"));
        assertTrue(message, message.contains("Angle"));
    }

    /**
     * Tests the {@link LocalizedParseException} constructor using a given resource key
     * and the text that we failed to parse.
     */
    @Test
    public void testResourceKeyForText() {
        final ParseException e = new LocalizedParseException(
                Locale.CANADA, Errors.Keys.NodeHasNoParent_1, "Some text to parse", 5);
        String message = e.getLocalizedMessage();
        assertTrue(message, message.contains("Node “text” has no parent."));
    }
}
