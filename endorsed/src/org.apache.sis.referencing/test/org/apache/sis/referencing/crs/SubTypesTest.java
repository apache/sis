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
package org.apache.sis.referencing.crs;

import java.util.Arrays;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link SubTypes} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SubTypesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public SubTypesTest() {
    }

    /**
     * Tests the {@link SubTypes#BY_TYPE} comparator.
     */
    @Test
    public void testComparator() {
        final AbstractCRS[] components = {
            HardCodedCRS.IMAGE,
            HardCodedCRS.TIME,
            HardCodedCRS.WGS84,
            HardCodedCRS.GRAVITY_RELATED_HEIGHT
        };
        Arrays.sort(components, SubTypes.BY_TYPE);
        assertArrayEquals(new AbstractCRS[] {
            HardCodedCRS.WGS84,
            HardCodedCRS.GRAVITY_RELATED_HEIGHT,
            HardCodedCRS.TIME,
            HardCodedCRS.IMAGE
        }, components);
    }
}
