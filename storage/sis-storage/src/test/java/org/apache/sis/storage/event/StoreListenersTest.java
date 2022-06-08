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
package org.apache.sis.storage.event;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStoreMock;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link StoreListeners} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.0
 * @module
 */
public final strictfp class StoreListenersTest extends TestCase implements StoreListener<WarningEvent> {
    /**
     * Dummy data store used for firing events.
     */
    private final DataStoreMock store;

    /**
     * The warning received by {@link #eventOccured(WarningEvent)}.
     * This is stored for allowing test methods to verify the properties.
     */
    private LogRecord warning;

    /**
     * Creates a new test case.
     */
    public StoreListenersTest() {
        store = new DataStoreMock("Event emitter");
    }

    /**
     * Invoked when a warning occurred. The implementation in this test verifies that the {@code source} property has
     * the expected values, then stores the log record in the {@link #warning} field for inspection by the test method.
     *
     * @param  warning  the warning event emitted by the data store.
     */
    @Override
    public void eventOccured(final WarningEvent warning) {
        assertSame("source", store, warning.getSource());
        this.warning = warning.getDescription();
    }

    /**
     * Tests {@link StoreListeners#addListener(Class, StoreListener)} followed by
     * {@link StoreListeners#removeListener(Class, StoreListener)}.
     */
    @Test
    public void testAddAndRemoveStoreListener() {
        final StoreListeners listeners = store.listeners();
        assertFalse("hasListeners()", listeners.hasListeners(WarningEvent.class));
        assertFalse("hasListener(…)", listeners.hasListener (WarningEvent.class, this));
        listeners.addListener(WarningEvent.class, this);
        assertTrue("hasListeners()", listeners.hasListeners(WarningEvent.class));
        assertTrue("hasListener(…)", listeners.hasListener (WarningEvent.class, this));
        listeners.removeListener(WarningEvent.class, this);
        assertFalse("hasListeners()", listeners.hasListeners(WarningEvent.class));
        assertFalse("hasListener(…)", listeners.hasListener (WarningEvent.class, this));
        listeners.removeListener(WarningEvent.class, this);         // Should be no-op.
    }

    /**
     * Verifies that {@link StoreListeners#addListener(Class, StoreListener)} ignore the given listener
     * when the specified type of event is never fired.
     */
    @Test
    public void testListenerFiltering() {
        final StoreListeners listeners = store.listeners();
        listeners.addListener(StoreEvent.class, (event) -> {});
        listeners.addListener(WarningEvent.class, this);
        assertTrue(listeners.hasListeners(StoreEvent.class));
        assertTrue(listeners.hasListeners(WarningEvent.class));
        listeners.setUsableEventTypes(StoreEvent.class, WarningEvent.class);
        assertTrue(listeners.hasListeners(StoreEvent.class));
        assertTrue(listeners.hasListeners(WarningEvent.class));
        listeners.setUsableEventTypes(StoreEvent.class);
        assertTrue (listeners.hasListeners(StoreEvent.class));
        assertFalse(listeners.hasListeners(WarningEvent.class));
        listeners.addListener(WarningEvent.class, this);
        assertTrue (listeners.hasListeners(StoreEvent.class));
        assertFalse(listeners.hasListeners(WarningEvent.class));
    }

    /**
     * Tests {@link StoreListeners#warning(String, Exception)} with a registered listener.
     */
    @Test
    @DependsOnMethod("testAddAndRemoveStoreListener")
    public void testWarning() {
        final LogRecord record = new LogRecord(Level.WARNING, "The message");
        store.addListener(WarningEvent.class, this);
        store.listeners().warning(record);
        assertSame(record, warning);
    }

    /**
     * Tests {@link StoreListeners#warning(String, Exception)} with a registered listener.
     * This method shall infer the source class name and source method name automatically.
     */
    @Test
    @DependsOnMethod("testWarning")
    public void testWarningWithAutoSource() {
        store.addListener(WarningEvent.class, this);
        store.simulateWarning("The message");
        assertNotNull("Listener has not been notified.", warning);
        assertEquals(DataStoreMock.class.getName(), warning.getSourceClassName());
        assertEquals("simulateWarning", warning.getSourceMethodName());
        assertEquals("The message", warning.getMessage());
    }

    /**
     * Tests {@link StoreListeners#close()}. This event is handled in a special way:
     * close event on the parent resource causes the same event to be fired for all
     * children.
     */
    @Test
    public void testClose() {
        final Resource resource = store.newChild();
        class Listener implements StoreListener<CloseEvent> {
            /** Whether the resource has been closed. */boolean isClosed;

            @Override public void eventOccured(CloseEvent event) {
                assertSame(resource, event.getSource());
                assertFalse(isClosed);
                isClosed = true;
            }
        }
        final Listener listener = new Listener();
        /*
         * First, register and unregister. No event should be received.
         */
        resource.addListener(CloseEvent.class, listener);
        resource.removeListener(CloseEvent.class, listener);
        store.close();
        assertFalse(listener.isClosed);
        /*
         * Register and close. Now the event should be received.
         */
        resource.addListener(CloseEvent.class, listener);
        store.close();
        assertTrue(listener.isClosed);
    }
}
