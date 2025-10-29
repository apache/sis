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
package org.apache.sis.filter.function.math;

import java.util.Map;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.filter.DefaultFilterFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests expressions using mathematical functions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class RegistryTest {
    /**
     * Type of features used for testing.
     */
    private final DefaultFeatureType feature;

    /**
     * Creates a new test case.
     */
    public RegistryTest() {
        feature = new DefaultFeatureType(Map.of(DefaultFeatureType.NAME_KEY, "Test"), false, null,
                new DefaultAttributeType<>(Map.of(DefaultAttributeType.NAME_KEY, "value"), Double.class, 1, 1, null));
    }

    /**
     * Tests {@link Function#ABS}.
     */
    @Test
    public void testAbs() {
        final var ff = DefaultFilterFactory.forFeatures();
        final var ex = ff.function("abs", ff.property("value"));

        final var f = feature.newInstance();
        f.setPropertyValue("value", 12.5);
        assertEquals(12.5, ex.apply(f));
        f.setPropertyValue("value", -18.25);
        assertEquals(18.25, ex.apply(f));
    }
}
