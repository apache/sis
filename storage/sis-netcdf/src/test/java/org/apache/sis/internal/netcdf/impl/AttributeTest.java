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

import static org.junit.Assert.*;


/**
 * Tests the {@link Attribute} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from 0.3)
 * @version 0.8
 * @module
 */
public final strictfp class AttributeTest extends TestCase {
    /**
     * Tests the {@link Attribute#numberValues()} method.
     */
    @Test
    public void testNumberValues() {
        final Attribute a = new Attribute("aName", new float[] {10, 20, 1});
        assertArrayEquals("numberValues", new Number[] { 10f,    20f,    1f  }, a.numberValues());
        assertArrayEquals("stringValues", new String[] {"10.0", "20.0", "1.0"}, a.stringValues());
    }
}
