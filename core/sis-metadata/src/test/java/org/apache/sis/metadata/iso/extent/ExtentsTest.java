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
package org.apache.sis.metadata.iso.extent;

import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.internal.metadata.ReferencingServices.NAUTICAL_MILE;
import static org.junit.Assert.*;


/**
 * Tests {@link Extents}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class ExtentsTest extends TestCase {
    /**
     * One minute of angle, in degrees.
     */
    private static final double MINUTE = 1./60;

    /**
     * Tests {@link Extents#area(GeographicBoundingBox)}.
     */
    @Test
    public void testGetArea() {
        /*
         * The nautical mile is equals to the length of 1 second of arc along meridians or parallels at the equator.
         * Since we are using the GRS80 authalic sphere instead than WGS84, we have a slight empirical shift.
         */
        final DefaultGeographicBoundingBox box = new DefaultGeographicBoundingBox(10, 10+MINUTE, 2.9685, 2.9685+MINUTE);
        assertEquals(NAUTICAL_MILE * NAUTICAL_MILE, Extents.area(box), 0.1);
        /*
         * Result should be much smaller near the poles.
         */
        box.setNorthBoundLatitude(90);
        box.setSouthBoundLatitude(90-MINUTE);
        assertEquals(499.5, Extents.area(box), 0.1);
        box.setSouthBoundLatitude(-90);
        box.setNorthBoundLatitude(-90+MINUTE);
        assertEquals(499.5, Extents.area(box), 0.1);
    }
}
