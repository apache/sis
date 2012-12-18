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

import org.junit.Test;
import org.apache.sis.test.TestCase;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the static methods provided in {@link AbstractDirectPosition}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class AbstractDirectPositionTest extends TestCase {
    /**
     * Tests {@link AbstractDirectPosition#isSimplePrecision(double[])}.
     */
    @Test
    public void testIsSimplePrecision() {
        assertTrue (AbstractDirectPosition.isSimplePrecision(2, 0.5, 0.25, Double.NaN, Double.POSITIVE_INFINITY));
        assertFalse(AbstractDirectPosition.isSimplePrecision(2, 0.5, 1.0 / 3));
    }
}
