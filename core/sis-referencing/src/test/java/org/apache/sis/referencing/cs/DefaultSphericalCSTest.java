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
package org.apache.sis.referencing.cs;

import java.util.Collections;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.referencing.cs.HardCodedCS.*;


/**
 * Tests {@link DefaultSphericalCS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(AbstractCSTest.class)
public final strictfp class DefaultSphericalCSTest extends TestCase {
    /**
     * Tests the normalization of a coordinate system.
     */
    @Test
    public void testNormalize() {
        final AbstractCS normalized = SPHERICAL.forConvention(AxesConvention.NORMALIZED);
        assertNotSame(SPHERICAL, normalized);
        assertEquals(new DefaultSphericalCS(
            Collections.singletonMap(AbstractCS.NAME_KEY, "Spherical CS: East (deg), North (deg), Up (m)."),
            HardCodedAxes.SPHERICAL_LONGITUDE,
            HardCodedAxes.SPHERICAL_LATITUDE,
            HardCodedAxes.GEOCENTRIC_RADIUS
        ), normalized);
    }
}
