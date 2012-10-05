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
package org.apache.sis.util.type;

import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.test.TestCase;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Types} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
public final strictfp class TypesTest extends TestCase {
    /**
     * Tests the {@link Types#getStandardName(Class)} method.
     */
    @Test
    public void testGetStandardName() {
        assertEquals("CI_Citation",      Types.getStandardName(Citation     .class));
        assertEquals("CD_Datum",         Types.getStandardName(Datum        .class));
        assertEquals("CS_AxisDirection", Types.getStandardName(AxisDirection.class));
    }

    /**
     * Tests the {@link Types#forStandardName(String)} method.
     */
    @Test
    public void testForStandardName() {
        assertEquals(Citation     .class, Types.forStandardName("CI_Citation"));
        assertEquals(Datum        .class, Types.forStandardName("CD_Datum"));
        assertEquals(Citation     .class, Types.forStandardName("CI_Citation")); // Value should be cached.
        assertEquals(AxisDirection.class, Types.forStandardName("CS_AxisDirection"));
        assertNull  (                     Types.forStandardName("MD_Dummy"));
    }
}
