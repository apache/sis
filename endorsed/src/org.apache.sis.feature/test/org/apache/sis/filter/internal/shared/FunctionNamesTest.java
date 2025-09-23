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
package org.apache.sis.filter.internal.shared;

import java.lang.reflect.Field;
import org.apache.sis.filter.sqlmm.SQLMM;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Verifies the values declared in {@link FunctionNames}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class FunctionNamesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public FunctionNamesTest() {
    }

    /**
     * Verifies that each field has the same name as its value.
     *
     * @throws IllegalAccessException should never happen.
     */
    @Test
    public void verifyFieldNames() throws IllegalAccessException {
        for (final Field f : FunctionNames.class.getFields()) {
            assertEquals(f.getName(), f.get(null));
        }
    }

    /**
     * Verifies SQLMM names.
     */
    @Test
    public void verifySQLMM() {
        int count = 0;
        for (final Field f : FunctionNames.class.getFields()) {
            final String name = f.getName();
            if (name.startsWith("ST_")) {
                assertEquals(name, SQLMM.valueOf(name).name());
                count++;
            }
        }
        assertEquals(8, count);
    }
}
