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
package org.apache.sis.converter;

import org.apache.sis.measure.Angle;
import org.apache.sis.util.ObjectConverter;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the various {@link AngleConverter} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class AngleConverterTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public AngleConverterTest() {
    }

    /**
     * Tests conversions to {@link Double}.
     */
    @Test
    public void testDouble() {
        final ObjectConverter<Angle,Double> c1 = AngleConverter.INSTANCE;
        final ObjectConverter<Double,Angle> c2 = AngleConverter.Inverse.INSTANCE;
        final Angle  v1 = new Angle (30.25);
        final Double v2 = 30.25;
        assertEquals(v2, c1.apply(v1));
        assertEquals(v1, c2.apply(v2));
        assertSame(c2, c1.inverse());
        assertSame(c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }
}
