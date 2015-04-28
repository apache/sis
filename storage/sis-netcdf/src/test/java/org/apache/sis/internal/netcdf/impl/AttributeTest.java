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
package org.apache.sis.internal.netcdf.impl;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link Attribute} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from 0.3)
 * @version 0.5
 * @module
 */
public final strictfp class AttributeTest extends TestCase {
    /**
     * Tests the {@link Attribute#dateToISO(String)} method.
     */
    @Test
    public void testDateToISO() {
        assertEquals("2009-01-01T06:00:00+01:00", Attribute.dateToISO("2009-01-01T06:00:00+01:00"));
        assertEquals("2005-09-22T04:30:15Z",      Attribute.dateToISO("2005-09-22T04:30:15Z"));
        assertEquals("2005-09-22T04:30:15Z",      Attribute.dateToISO("2005-09-22T04:30:15"));
        assertEquals("2005-09-22T04:30:00Z",      Attribute.dateToISO("2005-09-22T04:30"));
        assertEquals("2005-09-22T04:00:00Z",      Attribute.dateToISO("2005-09-22T04"));
        assertEquals("2005-09-22T00:00:00Z",      Attribute.dateToISO("2005-09-22"));
        assertEquals("2005-09-22T00:00:00Z",      Attribute.dateToISO("2005-9-22"));
    }
}
