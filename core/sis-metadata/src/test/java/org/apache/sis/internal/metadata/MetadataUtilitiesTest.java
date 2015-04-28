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
package org.apache.sis.internal.metadata;

import java.util.Date;
import org.junit.Test;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;


/**
 * Tests the {@link MetadataUtilities} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final strictfp class MetadataUtilitiesTest extends TestCase {
    /**
     * Tests {@link MetadataUtilities#toMilliseconds(Date)}.
     */
    @Test
    public void testToMilliseconds() {
        assertEquals(1000,           MetadataUtilities.toMilliseconds(new Date(1000)));
        assertEquals(Long.MIN_VALUE, MetadataUtilities.toMilliseconds(null));
    }

    /**
     * Tests {@link MetadataUtilities#toDate(long)}.
     */
    @Test
    public void testToDate() {
        assertEquals(new Date(1000), MetadataUtilities.toDate(1000));
        assertNull(MetadataUtilities.toDate(Long.MIN_VALUE));
    }
}
