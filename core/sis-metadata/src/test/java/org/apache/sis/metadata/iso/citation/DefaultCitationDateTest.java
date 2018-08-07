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

import java.util.Date;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.CitationDate;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DefaultCitationDate}, especially the copy constructor.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 * @since   0.3
 * @module
 */
public final strictfp class DefaultCitationDateTest extends TestCase {
    /**
     * Tests the copy constructor.
     */
    @Test
    public void testCopyConstructor() {
        final CitationDate original = new CitationDate() {
            @Override public Date     getDate()     {return new Date(1305716658508L);}
            @Override public DateType getDateType() {return DateType.CREATION;}
        };
        final DefaultCitationDate copy = new DefaultCitationDate(original);
        assertEquals(new Date(1305716658508L), copy.getDate());
        assertEquals(DateType.CREATION, copy.getDateType());
        assertTrue (copy.equals(original, ComparisonMode.BY_CONTRACT));
        assertFalse(copy.equals(original, ComparisonMode.STRICT)); // Opportunist test.
    }
}
