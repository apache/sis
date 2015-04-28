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
package org.apache.sis.metadata;

import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.toTreeStructure;


/**
 * Tests the {@link AbstractMetadata} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(MetadataStandardTest.class)
public final strictfp class AbstractMetadataTest extends TestCase {
    /**
     * Tests the {@link AbstractMetadata#hashCode()} method on an object having cyclic associations.
     * This is the same test than {@link MetadataStandardTest#testHashCodeOnCyclicMetadata()}, with
     * only a different entry point.
     *
     * @see MetadataStandardTest#testHashCodeOnCyclicMetadata()
     */
    @Test
    public void testHashCodeOnCyclicMetadata() {
        final int code = MetadataStandardTest.createCyclicMetadata().hashCode();
        assertEquals(code, MetadataStandardTest.createCyclicMetadata().hashCode());
    }

    /**
     * Tests the {@link AbstractMetadata#toString()} method on an object having cyclic associations.
     * In absence of safety guard against infinite recursivity, this test would produce either a
     * {@link StackOverflowError} or an {@link OutOfMemoryError} (after quite a long time).
     *
     * <p>The tree formatted by this test is:</p>
     * {@preformat text
     *     Platform
     *       ├─Description……………………… A platform.
     *       └─Instrument
     *           ├─Type……………………………… An instrument type.
     *           └─Mounted on
     *             (cycle omitted)
     * }
     */
    @Test
    public void testToStringOnCyclicMetadata() {
        final String text = MetadataStandardTest.createCyclicMetadata().toString();
        /*
         * We can not perform a full comparison of the string, since it is locale-dependent.
         * Compare only the tree structure. The full tree is English is shown in javadoc.
         */
        assertTrue(text, text.startsWith("Platform"));
        assertArrayEquals(new String[] {
            "",
            "  ├─",
            "  └─",
            "      ├─",
            "      └─",
            "        (",
            ""}, toTreeStructure(text));
    }
}
