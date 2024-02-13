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
package org.apache.sis.storage.aggregate;

import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link CoverageAggregator}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CoverageAggregatorTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CoverageAggregatorTest() {
    }

    /**
     * Tests an empty aggregator.
     *
     * @throws DataStoreException if an error occurred.
     */
    @Test
    public void testEmpty() throws DataStoreException {
        final var aggregator = new CoverageAggregator();
        final var aggregate  = assertInstanceOf(Aggregate.class, aggregator.build(null));
        assertTrue(aggregate.components().isEmpty());
    }
}
