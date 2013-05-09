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
package org.apache.sis.metadata.iso.maintenance;

import org.apache.sis.metadata.iso.LoggingWatcher;
import org.apache.sis.test.TestCase;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultScopeDescription}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class DefaultScopeDescriptionTest extends TestCase {
    /**
     * A JUnit {@linkplain Rule rule} for listening to log events. This field is public
     * because JUnit requires us to do so, but should be considered as an implementation
     * details (it should have been a private field).
     */
    @Rule
    public final LoggingWatcher listener = new LoggingWatcher() {
        /**
         * Ensures that the logging message contains the name of the exclusive properties.
         */
        @Override
        protected void verifyMessage(final String message) {
            assertTrue(message.contains("dataset"));
            assertTrue(message.contains("other"));
        }
    };

    /**
     * Tests the various setter methods. Since they are exclusive properties,
     * we expect any new property to replace the old one.
     */
    @Test
    public void testSetExclusiveProperties() {
        final DefaultScopeDescription metadata = new DefaultScopeDescription();
        metadata.setDataset("A dataset");
        assertEquals("dataset", "A dataset", metadata.getDataset());

        listener.maximumLogCount = 1;
        metadata.setOther("Other value");
        assertEquals("other", "Other value", metadata.getOther());
        assertNull("dataset", metadata.getDataset());

        metadata.setDataset(null); // Expected to be a no-op.
        assertEquals("other", "Other value", metadata.getOther());
        assertNull("dataset", metadata.getDataset());

        metadata.setOther(null);
        assertNull("other",   metadata.getOther());
        assertNull("dataset", metadata.getDataset());
    }
}
