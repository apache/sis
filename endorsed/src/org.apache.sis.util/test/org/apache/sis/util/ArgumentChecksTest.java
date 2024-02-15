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
package org.apache.sis.util;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;


/**
 * Tests the {@link ArgumentChecks} static methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@DependsOn(org.apache.sis.util.resources.IndexedResourceBundleTest.class)
public final class ArgumentChecksTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ArgumentChecksTest() {
    }

    /**
     * Tests {@link ArgumentChecks#ensureNonNullElement(String, int, Object)}.
     */
    @Test
    public void testEnsureNonNullElement() {
        NullPointerException exception;
        exception = assertThrows(NullPointerException.class, () -> ArgumentChecks.ensureNonNullElement("axes", 2, null));
        assertMessageContains(exception, "axes[2]");

        exception = assertThrows(NullPointerException.class, () -> ArgumentChecks.ensureNonNullElement("axes[#].unit", 2, null));
        assertTrue(exception.getMessage().contains("axes[2].unit"));
    }

    /**
     * Tests {@link ArgumentChecks#ensureNonEmptyBounded(String, boolean, int, int, int[])}.
     */
    @Test
    public void testEnsureBetweenAndDistinct() {
        ArgumentChecks.ensureNonEmptyBounded("dimensions", true, 0, 4, new int[] {2, 3, 0, 1});
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ArgumentChecks.ensureNonEmptyBounded("dimensions", true, 0, 4, new int[] {2, 3, 3, 1}));
        assertMessageContains(exception);
    }

    /**
     * Tests {@link ArgumentChecks#ensurePositive(java.lang.String, double)}.
     */
    @Test
    public void testEnsurePositive() {
        ArgumentChecks.ensurePositive("length", 4d);
        ArgumentChecks.ensurePositive("length", 0d);
        IllegalArgumentException exception;

        exception = assertThrows(IllegalArgumentException.class, () -> ArgumentChecks.ensurePositive("length", -4d));
        assertMessageContains(exception, "length");

        exception = assertThrows(IllegalArgumentException.class, () -> ArgumentChecks.ensurePositive("length", -0d));
        assertMessageContains(exception, "length");

        exception = assertThrows(IllegalArgumentException.class, () -> ArgumentChecks.ensurePositive("length", -0f));
        assertMessageContains(exception, "length");
    }
}
