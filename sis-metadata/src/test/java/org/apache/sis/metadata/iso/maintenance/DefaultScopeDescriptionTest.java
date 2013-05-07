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

import java.util.logging.Filter;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.test.TestCase;
import org.junit.Before;
import org.junit.After;
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
public final strictfp class DefaultScopeDescriptionTest extends TestCase implements Filter {
    /**
     * The logger where to warnings are expected to be sent.
     */
    private static final Logger LOGGER = Logging.getLogger(DefaultScopeDescription.class);

    /**
     * {@code false} when no logging message is expected, and {@code true} when expected.
     */
    private boolean isLogExpected;

    /**
     * Installs this {@link Filter} for the log messages before the tests are run.
     * This installation will cause the {@link #isLoggable(LogRecord)} method to be
     * invoked when a message is logged.
     *
     * @see #isLoggable(LogRecord)
     */
    @Before
    public void installLogFilter() {
        assertNull(LOGGER.getFilter());
        LOGGER.setFilter(this);
    }

    /**
     * Removes the filter which has been set for testing purpose.
     */
    @After
    public void removeLogFilter() {
        LOGGER.setFilter(null);
    }

    /**
     * Tests the various setter methods. Since they are exclusive properties,
     * we expect any new property to replace the old one.
     */
    @Test
    public void testSetExclusiveProperties() {
        final DefaultScopeDescription metadata = new DefaultScopeDescription();
        metadata.setDataset("A dataset");
        assertEquals("dataset", "A dataset", metadata.getDataset());

        isLogExpected = true;
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

    /**
     * Invoked (indirectly) when a setter method has emitted a warning. This method verifies if we
     * were expecting a log message, then resets the {@link #isLogExpected} flag to {@code false}
     * (i.e. we expect at most one logging message).
     */
    @Override
    public boolean isLoggable(final LogRecord record) {
        final String message = record.getMessage();
        if (!isLogExpected) {
            fail("Unexpected logging: " + message);
        }
        isLogExpected = false;
        return verbose;
    }
}
