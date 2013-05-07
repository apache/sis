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
package org.apache.sis.metadata.iso.spatial;

import java.util.logging.Filter;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import org.opengis.util.InternationalString;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.test.TestCase;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultGeorectified}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class DefaultGeorectifiedTest extends TestCase implements Filter {
    /**
     * The logger where to warnings are expected to be sent.
     */
    private static final Logger LOGGER = Logging.getLogger(DefaultGeorectified.class);

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
     * Tests {@link DefaultGeorectified#isCheckPointAvailable()} and
     * {@link DefaultGeorectified#setCheckPointAvailable(boolean)}.
     */
    @Test
    public void testCheckPointAvailable() {
        final DefaultGeorectified metadata = new DefaultGeorectified();
        final InternationalString description = new SimpleInternationalString("A check point description.");
        assertFalse("checkPointAvailability", metadata.isCheckPointAvailable());

        // Setting the description shall set automatically the availability.
        metadata.setCheckPointDescription(description);
        assertTrue("checkPointAvailability", metadata.isCheckPointAvailable());

        // Setting the availability flag shall hide the description and logs a message.
        isLogExpected = true;
        metadata.setCheckPointAvailable(false);
        assertNull("checkPointDescription", metadata.getCheckPointDescription());

        // Setting the availability flag shall bring back the description.
        metadata.setCheckPointAvailable(true);
        assertSame("checkPointDescription", description, metadata.getCheckPointDescription());
    }

    /**
     * Invoked (indirectly) when the {@link DefaultGeorectified#setCheckPointAvailable(boolean)} method
     * has emitted a warning. This method verifies if we were expecting a log message, then resets the
     * {@link #isLogExpected} flag to {@code false} (i.e. we expect at most one logging message).
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
