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

import java.util.Map;
import org.opengis.test.Validators;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static java.util.Collections.singletonMap;


/**
 * Tests the {@link DefaultCartesianCS} class.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
@DependsOn(DefaultCoordinateSystemAxisTest.class)
public final strictfp class DefaultCartesianCSTest extends TestCase {
    /**
     * Tests the creation of a Cartesian CS with legal axes.
     */
    @Test
    public void testConstructor() {
        final Map<String,?> properties = singletonMap(DefaultCartesianCS.NAME_KEY, "Test");
        DefaultCartesianCS cs;
        /*
         * (E,N) : legal axes for the usual projected CRS.
         */
        cs = new DefaultCartesianCS(properties,
                CommonAxes.EASTING,
                CommonAxes.NORTHING);
        Validators.validate(cs);
        /*
         * (NE,SE) : same CS rotated by 45°
         */
        cs = new DefaultCartesianCS(properties,
                CommonAxes.NORTH_EAST,
                CommonAxes.SOUTH_EAST);
        Validators.validate(cs);
        /*
         * (NE,h) : considered perpendicular.
         */
        cs = new DefaultCartesianCS(properties,
                CommonAxes.NORTH_EAST,
                CommonAxes.ALTITUDE);
        Validators.validate(cs);
    }

    /**
     * Tests the creation of a Cartesian CS with illegal axes.
     */
    @Test
    public void testConstructorArgumentChecks() {
        final Map<String,?> properties = singletonMap(DefaultCartesianCS.NAME_KEY, "Test");
        /*
         * (λ,φ) : illegal units.
         */
        try {
            final DefaultCartesianCS cs = new DefaultCartesianCS(properties,
                    CommonAxes.LONGITUDE,
                    CommonAxes.LATITUDE);
            fail("Angular units should not be accepted for " + cs);
        } catch (IllegalArgumentException e) {
            assertFalse(e.getMessage().isEmpty());
        }
        /*
         * (S,N) : co-linear axes.
         */
        try {
            final DefaultCartesianCS cs = new DefaultCartesianCS(properties,
                    CommonAxes.SOUTHING,
                    CommonAxes.NORTHING);
            fail("Colinear units should not be accepted for " + cs);
        } catch (IllegalArgumentException e) {
            assertFalse(e.getMessage().isEmpty());
        }
        try {
            final DefaultCartesianCS cs = new DefaultCartesianCS(properties,
                    CommonAxes.NORTH_EAST,
                    CommonAxes.EASTING);
            fail("Non-perpendicular axis should not be accepted for " + cs);
        } catch (IllegalArgumentException e) {
            assertFalse(e.getMessage().isEmpty());
        }
    }
}
