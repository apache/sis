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
package org.apache.sis.geometry;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.opengis.test.Validators.validate;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import org.apache.sis.referencing.crs.HardCodedCRS;


/**
 * Tests the {@link ImmutableEnvelope} class.
 * Most of tests are actually performed by {@link AbstractEnvelopeTest}.
 * This class adds only some tests that are specific to {@code ImmutableEnvelope} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ImmutableEnvelopeTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ImmutableEnvelopeTest() {
    }

    /**
     * Tests {@code ImmutableEnvelope} serialization.
     */
    @Test
    public void testSerialization() {
        final var e1 = new ImmutableEnvelope(
                new double[] {-20, -10},
                new double[] { 20,  10}, HardCodedCRS.WGS84);
        final ImmutableEnvelope e2 = assertSerializedEquals(e1);
        assertNotSame(e1, e2);
        validate(e2);
    }
}
