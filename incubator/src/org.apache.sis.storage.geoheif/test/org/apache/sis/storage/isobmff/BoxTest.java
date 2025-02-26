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
package org.apache.sis.storage.isobmff;

import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import org.apache.sis.storage.isobmff.base.FileType;
import org.apache.sis.storage.isobmff.base.Movie;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests some methods related to {@link Box}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class BoxTest {
    /**
     * Creates a new test case.
     */
    public BoxTest() {
    }

    /**
     * Verifies {@link Box#EPOCH}.
     */
    @Test
    public void verifyEpoch() {
        assertEquals(OffsetDateTime.of(1904, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(), Box.EPOCH);
    }

    /**
     * Verifies a few four-character identifiers.
     */
    @Test
    public void verifyFourCC() {
        assertEquals("ftyp", Box.formatFourCC(FileType.BOXTYPE));
        assertEquals("moov", Box.formatFourCC(Movie.BOXTYPE));
    }
}
