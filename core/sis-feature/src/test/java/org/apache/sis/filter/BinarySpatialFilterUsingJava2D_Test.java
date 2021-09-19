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
package org.apache.sis.filter;

import java.awt.Shape;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Tests {@link BinarySpatialFilter} implementations using ESRI library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class BinarySpatialFilterUsingJava2D_Test extends BinarySpatialFilterTestCase<Shape> {
    /**
     * Creates a new test.
     */
    public BinarySpatialFilterUsingJava2D_Test() {
        super(Shape.class);
    }

    /**
     * Test ignored for now (not yet mapped to a Java2D operation).
     */
    @Test
    @Override
    @Ignore("Not yet mapped to a Java2D operation.")
    public void testTouches() {
    }

    /**
     * Test ignored for now (not yet mapped to a Java2D operation).
     */
    @Test
    @Override
    @Ignore("Not yet mapped to a Java2D operation.")
    public void testCrosses() {
    }

    /**
     * Test ignored for now (not yet mapped to a Java2D operation).
     */
    @Test
    @Override
    @Ignore("Not yet mapped to a Java2D operation.")
    public void testOverlaps() {
    }

    /**
     * Test ignored for now (not yet mapped to a Java2D operation).
     */
    @Test
    @Override
    @Ignore("Not yet mapped to a Java2D operation.")
    public void testDWithin() {
    }

    /**
     * Test ignored for now (not yet mapped to a Java2D operation).
     */
    @Test
    @Override
    @Ignore("Not yet mapped to a Java2D operation.")
    public void testBeyond() {
    }

    /**
     * Test ignored for now (not yet mapped to a Java2D operation).
     */
    @Test
    @Override
    @Ignore("Not yet mapped to a Java2D operation.")
    public void testWithReprojection() {
    }

    /**
     * Test ignored for now because {@link java.awt.geom.Path2D} does not implement {@code equals(Object)}.
     */
    @Test
    @Ignore
    @Override
    public void testSerialization() {
    }
}
