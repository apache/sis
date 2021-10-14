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
package org.apache.sis.internal.referencing;

import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests {@link WraparoundApplicator}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class WraparoundApplicatorTest extends TestCase {
    /**
     * Tests {@link WraparoundApplicator#range(CoordinateSystem, int)}.
     */
    @Test
    public void testRange() {
        assertTrue  (Double.isNaN(WraparoundApplicator.range(HardCodedCS.GEODETIC_φλ, 0)));
        assertEquals(360, WraparoundApplicator.range(HardCodedCS.GEODETIC_φλ, 1), STRICT);
        assertEquals(400, WraparoundApplicator.range(HardCodedCS.ELLIPSOIDAL_gon, 0), STRICT);
    }
}