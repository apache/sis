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

import java.util.logging.Level;
import org.apache.sis.test.TestCase;
import org.junit.*;

import static org.junit.Assert.*;


/**
 * Tests the {@link LoggerAdapter} class.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class LoggerAdapterTest extends TestCase {
    /**
     * Tests the {@link LoggerAdapter#log(Level,String)} method.
     * This is of special interest because of the switch cases used in implementation.
     */
    @Test
    public void testLog() {
        final DummyLogger logger = new DummyLogger();
        final Object[] levels = new Object[] {
            Level.FINE,    "apple",
            Level.INFO,    "orange",
            Level.FINEST,  "yellow",
            Level.CONFIG,  "yeti",
            Level.SEVERE,  "ouch!",
            Level.WARNING, "caution",
            Level.FINEST,  "don't mind",
        };
        for (int i=0; i<levels.length; i++) {
            final Level  level   = (Level)  levels[i];
            final String message = (String) levels[++i];
            logger.clear();
            logger.log(level, message);
            assertEquals(level, logger.level);
            assertEquals(message, logger.last);
        }
        // Actually, Level.OFF has the highest intValue.
        // LoggerAdapter can easily match this level to a no-op.
        logger.clear();
        logger.log(Level.OFF, "off");
        assertEquals(Level.OFF, logger.level);

        // Actually, Level.ALL has the smallest intValue.
        // LoggerAdapter has no easy match for this level.
        logger.clear();
        logger.log(Level.ALL, "all");
        assertEquals(Level.OFF, logger.level);
    }
}
