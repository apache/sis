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
package org.apache.sis.geometry.wrapper.sis;

import org.apache.sis.geometries.Geometry;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.geometry.wrapper.Geometries;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link Factory}.
 */
public final class FactoryTest {
    /**
     * Creates a new test case.
     */
    public FactoryTest() {
    }

    /**
     * Verifies that {@link Geometries} factory methods can find the {@link Factory} implementation.
     */
    @Test
    public void testFactory() {
        assertTrue(Geometries.isKnownType(Geometry.class));
        assertSame(Factory.INSTANCE, Geometries.factory(Geometry.class));
        assertSame(Factory.INSTANCE, Geometries.factory(GeometryLibrary.SIS));
    }
}
