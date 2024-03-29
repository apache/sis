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
package org.apache.sis.geometry.wrapper.j2d;

import java.awt.geom.Path2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.apache.sis.geometry.wrapper.GeometriesTestCase;


/**
 * Tests {@link Factory} implementation for Java2D geometries.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class FactoryTest extends GeometriesTestCase {
    /**
     * Creates a new test case.
     */
    public FactoryTest() {
        super(Factory.INSTANCE);
    }

    /**
     * Tests {@link Factory#createPolyline(boolean, int, Vector...)}.
     */
    @Test
    @Override
    public void testCreatePolyline() {
        super.testCreatePolyline();
        assertInstanceOf(Path2D.class, geometry);
    }

    /**
     * Tests {@link Factory#mergePolylines(Iterator)} (or actually tests its strategy).
     */
    @Test
    @Override
    public void testMergePolylines() {
        super.testMergePolylines();
        assertInstanceOf(Path2D.class, geometry);
    }
}
