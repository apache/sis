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
package org.apache.sis.io.wkt;

import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Duration;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Convention} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.20)
 * @version 0.4
 * @module
 */
public final strictfp class ConventionTest extends TestCase {
    /**
     * Tests {@link Convention#getForcedUnit(Class)}.
     */
    @Test
    public void testGetForcedUnit() {
        assertNull(Convention.WKT2.getForcedUnit(Angle.class));
        assertNull(Convention.WKT1.getForcedUnit(Angle.class));
        assertEquals(NonSI.DEGREE_ANGLE, Convention.WKT1_COMMON_UNITS.getForcedUnit(Angle.class));
        assertNull(Convention.WKT1_COMMON_UNITS.getForcedUnit(Duration.class));
    }
}
