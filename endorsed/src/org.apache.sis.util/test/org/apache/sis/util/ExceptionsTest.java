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
package org.apache.sis.util;

import java.util.Locale;
import java.io.IOException;
import java.io.FileNotFoundException;

// Test dependencies
import org.junit.Test;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;


/**
 * Tests the {@link Exceptions} utility methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ExceptionsTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ExceptionsTest() {
    }

    /**
     * Tests {@link Exceptions#formatChainedMessages(Locale, String, Throwable)}.
     */
    @Test
    public void testFormatChainedMessages() {
        final String lineSeparator = System.lineSeparator();
        final FileNotFoundException cause = new FileNotFoundException("MisingFile.txt");
        cause.initCause(new IOException("Disk is not mounted."));
        final Exception e = new Exception("Cannot find “MisingFile.txt”.", cause);
        /*
         * The actual sequence of messages (with their cause is):
         *
         *    Cannot find “MisingFile.txt”
         *    MisingFile.txt
         *    Disk is not mounted.
         *
         * But the second line shall be omitted because it duplicates the first line.
         */
        String message = Exceptions.formatChainedMessages(Locale.ENGLISH, null, e);
        assertEquals("Cannot find “MisingFile.txt”." + lineSeparator +
                     "Caused by IOException: Disk is not mounted.",
                     message);
        /*
         * Test again with a header.
         */
        message = Exceptions.formatChainedMessages(Locale.ENGLISH, "Error while creating the data store.", e);
        assertEquals("Error while creating the data store." + lineSeparator +
                     "Caused by Exception: Cannot find “MisingFile.txt”." + lineSeparator +
                     "Caused by IOException: Disk is not mounted.",
                     message);
    }
}
