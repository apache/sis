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
 * @version 0.8
 * @since   0.3
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
        assertEquals("hashCode", code, MetadataStandardTest.createCyclicMetadata().hashCode());
    }

    /**
     * Tests the {@link AbstractMetadata#toString()} method on an object having cyclic associations.
     * In absence of safety guard against infinite recursivity, this test would produce either a
     * {@link StackOverflowError} or an {@link OutOfMemoryError} (after quite a long time).
     *
     * <p>The tree formatted by this test is:</p>
     * {@preformat text
     *     Acquisition information
     *       └─Platform
     *           ├─Description………………………………… A platform.
     *           └─Instrument
     *               ├─Type………………………………………… An instrument type.
     *               └─Mounted on
     *                   ├─Description…………… A platform.
     *                   └─Instrument
     *                         (omitted cycle)
     * }
     *
     * Note that the cycle detection apparently happens too late since "A platform" has been repeated.
     * This is because that same Platform instance appears in two different metadata property.  We do
     * not stop the formatting on the same metadata instance - we also require that it appears in the
     * same property - for allowing the complete formatting of objects that implement more than one
     * metadata interface.
     */
    @Test
    public void testToStringOnCyclicMetadata() {
        final String text = MetadataStandardTest.createCyclicMetadata().toString();
        /*
         * We can not perform a full comparison of the string since it is locale-dependent.
         * Compare only the tree structure. The full tree in English is shown in javadoc.
         */
        assertTrue(text, text.startsWith("Acquisition information"));
        assertArrayEquals("toTreeStructure", new String[] {
            "",                             // Acquisition information
            "  └─",                         // Platform
            "      ├─",                     // Description
            "      └─",                     // Instrument
            "          ├─",                 // Type
            "          └─",                 // Mounted on
            "              ├─",             // Description
            "              └─",             // Instrument
            "                    (",        // Omitted cycle
            ""}, toTreeStructure(text));
    }
}
