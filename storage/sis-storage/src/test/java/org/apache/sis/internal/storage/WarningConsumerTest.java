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

import java.util.NoSuchElementException;
import java.util.logging.LogRecord;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link WarningConsumer} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class WarningConsumerTest extends TestCase implements WarningListener<String> {
    /**
     * The object to be tested. Its source will be set to the string {@code "source"}.
     */
    private final WarningConsumer<String> consumer;

    /**
     * The warning received by {@link #warningOccured(String, LogRecord)}.
     * This is stored for allowing test methods to verify the properties.
     */
    private LogRecord warning;

    /**
     * Creates a new test case.
     */
    public WarningConsumerTest() {
        consumer = new WarningConsumer<>("source", Logging.getLogger("org.apache.sis.storage"));
    }

    /**
     * Invoked when a warning occurred. The implementation in this test verifies that the {@code source} argument has
     * the expected values, then store the log record in the {@link #warning} field for inspection by the test method.
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
     * Tests {@link WarningProducer#addWarningListener(WarningListener)} followed by
     * {@link WarningProducer#removeWarningListener(WarningListener)}
     */
    @Test
    public void testAddAndRemoveWarningListener() {
        consumer.addWarningListener(this);
        try {
            consumer.addWarningListener(this);
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("TestListener"));
        }
        consumer.removeWarningListener(this);
        try {
            consumer.removeWarningListener(this);
        } catch (NoSuchElementException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("TestListener"));
        }
    }

    /**
     * Tests {@link WarningProducer#warning(String, String, Exception)} with a registered listener.
     */
    @Test
    @DependsOnMethod("testAddAndRemoveWarningListener")
    public void testWarning() {
        consumer.addWarningListener(this);
        consumer.warning("testWarning", "The message", null);
        consumer.removeWarningListener(this);
        assertNotNull("Listener has not been notified.", warning);
        assertEquals("testWarning", warning.getSourceMethodName());
        assertEquals("The message", warning.getMessage());
    }
}
