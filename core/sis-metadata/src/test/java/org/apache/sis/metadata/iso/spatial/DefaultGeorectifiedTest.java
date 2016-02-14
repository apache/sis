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

import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.TestCase;
import org.junit.After;
import org.junit.Rule;
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
public final strictfp class DefaultGeorectifiedTest extends TestCase {
    /**
     * A JUnit {@link Rule} for listening to log events. This field is public because JUnit requires us to
     * do so, but should be considered as an implementation details (it should have been a private field).
     */
    @Rule
    public final LoggingWatcher loggings = new LoggingWatcher(Context.LOGGER);

    /**
     * Verifies that no unexpected warning has been emitted in any test defined in this class.
     */
    @After
    public void assertNoUnexpectedLog() {
        loggings.assertNoUnexpectedLog();
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
        loggings.assertNoUnexpectedLog();

        // Setting the availability flag shall hide the description and logs a message.
        metadata.setCheckPointAvailable(false);
        assertNull("checkPointDescription", metadata.getCheckPointDescription());
        loggings.assertNextLogContains("checkPointDescription", "checkPointAvailability");
        loggings.assertNoUnexpectedLog();

        // Setting the availability flag shall bring back the description.
        metadata.setCheckPointAvailable(true);
        assertSame("checkPointDescription", description, metadata.getCheckPointDescription());
    }
}
