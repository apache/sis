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
import org.apache.sis.test.DependsOn;
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
@DependsOn(DefaultGeographicBoundingBoxTest.class)
public final strictfp class ExtentsTest extends TestCase {
    /**
     * One minute of angle, in degrees.
     */
    private static final double MINUTE = 1./60;

    /**
     * Tests {@link Extents#intersection(GeographicBoundingBox, GeographicBoundingBox)}.
     */
    @Test
    public void testIntersection() {
        final GeographicBoundingBox b1 = new DefaultGeographicBoundingBox(10, 20, 30, 40);
        final GeographicBoundingBox b2 = new DefaultGeographicBoundingBox(15, 25, 26, 32);
        assertEquals(new DefaultGeographicBoundingBox(15, 20, 30, 32), Extents.intersection(b1, b2));
        assertSame(b1, Extents.intersection(b1,   null));
        assertSame(b2, Extents.intersection(null, b2));
        assertNull(    Extents.intersection(null, null));
    }

    /**
     * Tests {@link Extents#area(GeographicBoundingBox)}.
     */
    @Test
    public void testArea() {
        /*
         * The nautical mile is equals to the length of 1 second of arc along a meridian or parallel at the equator.
         * Since we are using the GRS80 authalic sphere instead than WGS84, and since the nautical mile definition
         * itself is a little bit approximative, we add a slight empirical shift.
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
