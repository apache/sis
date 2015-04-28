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
package org.apache.sis.util.logging;

import java.util.logging.LogRecord;
import java.util.NoSuchElementException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link WarningListeners} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class WarningListenersTest extends TestCase implements WarningListener<String> {
    /**
     * The object to be tested. Its source will be set to the string {@code "source"}.
     */
    private final WarningListeners<String> listeners;

    /**
     * The warning received by {@link #warningOccured(String, LogRecord)}.
     * This is stored for allowing test methods to verify the properties.
     */
    private LogRecord warning;

    /**
     * Creates a new test case.
     */
    public WarningListenersTest() {
        listeners = new WarningListeners<String>("source");
    }

    /**
     * Returns the type warning sources.
     */
    @Override
    public Class<String> getSourceClass() {
        return String.class;
    }

    /**
     * Invoked when a warning occurred. The implementation in this test verifies that the {@code source} argument has
     * the expected values, then stores the log record in the {@link #warning} field for inspection by the test method.
     */
    @Override
    public void warningOccured(final String source, final LogRecord warning) {
        assertEquals("source", source);
        this.warning = warning;
    }

    /**
     * Returns {@code "TestListener"}, for verification of error messages in exceptions.
     */
    @Override
    public String toString() {
        return "TestListener";
    }

    /**
     * Tests {@link WarningListeners#addWarningListener(WarningListener)} followed by
     * {@link WarningListeners#removeWarningListener(WarningListener)}
     */
    @Test
    public void testAddAndRemoveWarningListener() {
        listeners.addWarningListener(this);
        try {
            listeners.addWarningListener(this);
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("TestListener"));
        }
        listeners.removeWarningListener(this);
        try {
            listeners.removeWarningListener(this);
        } catch (NoSuchElementException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("TestListener"));
        }
    }

    /**
     * Tests {@link WarningListeners#warning(String, Exception)} with a registered listener.
     */
    @Test
    @DependsOnMethod("testAddAndRemoveWarningListener")
    public void testWarning() {
        listeners.addWarningListener(this);
        listeners.warning("The message", null);
        listeners.removeWarningListener(this);
        assertNotNull("Listener has not been notified.", warning);
        assertEquals("testWarning", warning.getSourceMethodName());
        assertEquals("The message", warning.getMessage());
    }
}
