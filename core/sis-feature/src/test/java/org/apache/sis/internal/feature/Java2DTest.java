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
package org.apache.sis.internal.feature;

import java.awt.geom.Path2D;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link Java2D} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class Java2DTest extends GeometriesTestCase {
    /**
     * Creates a new test case.
     */
    public Java2DTest() {
        super(new Java2D());
    }

    /**
     * Tests {@link Java2D#createPolyline(int, Vector...)}.
     */
    @Test
    @Override
    public void testCreatePolyline() {
        super.testCreatePolyline();
        assertInstanceOf("geometry", Path2D.class, geometry);
    }

    /**
     * Tests {@link Geometries#tryMergePolylines(Object, Iterator)}.
     */
    @Test
    @Override
    public void testTryMergePolylines() {
        super.testTryMergePolylines();
        assertInstanceOf("geometry", Path2D.class, geometry);
    }
}
