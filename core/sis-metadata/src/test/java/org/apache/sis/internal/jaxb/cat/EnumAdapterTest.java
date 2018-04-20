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
package org.apache.sis.internal.jaxb.cat;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link EnumAdapter} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.6
 * @module
 */
public final strictfp class EnumAdapterTest extends TestCase {
    /**
     * Tests the {@link EnumAdapter#name(String)} method.
     */
    @Test
    public void testEnumAdapterName() {
        assertEquals("IN_OUT",                        EnumAdapter.name("in/out"));
        assertEquals("OCEANS",                        EnumAdapter.name("oceans"));
        assertEquals("ENVIRONMENT",                   EnumAdapter.name("environment"));
        assertEquals("IMAGERY_BASE_MAPS_EARTH_COVER", EnumAdapter.name("imageryBaseMapsEarthCover"));
    }
}
