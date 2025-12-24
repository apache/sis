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
package org.apache.sis.metadata.iso.citation;

import java.time.Instant;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.CitationDate;
import org.apache.sis.util.ComparisonMode;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.time.temporal.Temporal;


/**
 * Tests {@link DefaultCitationDate}, especially the copy constructor.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultCitationDateTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultCitationDateTest() {
    }

    /**
     * Tests the copy constructor.
     */
    @Test
    public void testCopyConstructor() {
        final var original = new CitationDate() {
            @Override public Temporal getReferenceDate() {return Instant.ofEpochMilli(1305716658508L);}
            @Override public DateType getDateType()      {return DateType.CREATION;}
        };
        final var copy = new DefaultCitationDate(original);
        assertEquals(Instant.ofEpochMilli(1305716658508L), copy.getReferenceDate());
        assertEquals(DateType.CREATION, copy.getDateType());
        assertTrue (copy.equals(original, ComparisonMode.DEBUG));
        assertFalse(copy.equals(original, ComparisonMode.STRICT));          // Opportunist test.
    }
}
