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
package org.apache.sis.internal.jdk8;

import java.util.Date;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.date;


/**
 * Tests the {@link JDK8} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class JDK8Test extends TestCase {
    /**
     * Tests the {@link Utilities#parseDateTime(String)} method with an explicit time zone.
     */
    @Test
    public void testParseDateTime() {
        final Date expected = date("2009-01-01 05:00:00");
        assertEquals(expected, JDK8.parseDateTime("2009-01-01T06:00:00+01:00", false));
        assertEquals(expected, JDK8.parseDateTime("2009-01-01T06:00:00+01:00", true));
    }

    /**
     * Tests the {@link Utilities#parseDateTime(String)} method for a date in UTC time zone,
     * either explicit or implicit.
     */
    @Test
    public void testParseDateTimeUTC() {
        final Date expected = date("2005-09-22 00:00:00");
        assertEquals(expected, JDK8.parseDateTime("2005-09-22T00:00:00Z", false));
        assertEquals(expected, JDK8.parseDateTime("2005-09-22T00:00:00Z", true));
        assertEquals(expected, JDK8.parseDateTime("2005-09-22T00:00:00",  true));
        assertEquals(expected, JDK8.parseDateTime("2005-09-22T00:00",     true));
        assertEquals(expected, JDK8.parseDateTime("2005-09-22T00",        true));
        assertEquals(expected, JDK8.parseDateTime("2005-09-22",           true));
        assertEquals(expected, JDK8.parseDateTime("2005-9-22",            true));
    }
}
