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
package org.apache.sis.metadata.sql.internal.shared;

import java.lang.reflect.Field;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Verifies the constants declared in {@link Supports}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SupportsTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public SupportsTest() {
    }

    /**
     * Verifies that there is no collision between the constant values.
     *
     * @throws IllegalAccessException if a field is not public.
     */
    @Test
    public void ensureNoCollision() throws IllegalAccessException {
        int allFlags = 0;
        for (Field field : Supports.class.getFields()) {
            final int flag = field.getInt(null);
            assertEquals(1, Integer.bitCount(flag));
            assertNotEquals(allFlags, allFlags |= flag);
        }
        assertNotEquals(0, allFlags);
    }
}
