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
package org.apache.sis.math;

import java.util.Set;
import java.util.EnumSet;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link FunctionProperty} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class FunctionPropertyTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public FunctionPropertyTest() {
    }

    /**
     * Tests {@link FunctionProperty#concatenate(Set, Set)}.
     */
    @Test
    public void testConcatenate() {
        var step1  = EnumSet.of(FunctionProperty.INJECTIVE, FunctionProperty.ORDER_PRESERVING);
        var step2  = EnumSet.of(FunctionProperty.VOLATILE,  FunctionProperty.ORDER_REVERSING, FunctionProperty.INVERTIBLE);
        var concat = EnumSet.of(FunctionProperty.VOLATILE,  FunctionProperty.ORDER_REVERSING);
        assertEquals(concat, FunctionProperty.concatenate(step1, step2));

        step1  = EnumSet.of(FunctionProperty.INJECTIVE);
        concat = EnumSet.of(FunctionProperty.VOLATILE);
        assertEquals(concat, FunctionProperty.concatenate(step1, step2));
    }
}
