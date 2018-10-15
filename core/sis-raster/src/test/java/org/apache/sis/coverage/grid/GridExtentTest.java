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
package org.apache.sis.coverage.grid;

import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link GridExtent}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class GridExtentTest extends TestCase {
    /**
     * Tests the {@link GridExtent#GridExtent(AbstractEnvelope)} constructor.
     */
    @Test
    public void testCreateFromEnvelope() {
        final GeneralEnvelope env = new GeneralEnvelope(HardCodedCRS.IMAGE);
        env.setRange(0, -23.01, 30.107);
        env.setRange(1,  12.97, 18.071);
        GridExtent extent = new GridExtent(env);
        assertExtentEquals(extent, 0, -23, 29);
        assertExtentEquals(extent, 1,  13, 17);
        assertEquals(DimensionNameType.COLUMN, extent.getAxisType(0).get());
        assertEquals(DimensionNameType.ROW,    extent.getAxisType(1).get());
    }

    /**
     * Tests the rounding performed by the {@link GridExtent#GridExtent(AbstractEnvelope)} constructor.
     */
    @Test
    public void testRoundings() {
        final GeneralEnvelope env = new GeneralEnvelope(6);
        env.setRange(0, 1.49999, 3.49998);      // Round to [1…3), stored as [1…2].
        env.setRange(1, 1.50001, 3.49998);      // Round to [2…3), stored as [1…2] (not [2…2]) because the span is close to 2.
        env.setRange(2, 1.49998, 3.50001);      // Round to [1…4), stored as [1…2] (not [1…3]) because the span is close to 2.
        env.setRange(3, 1.49999, 3.50002);      // Round to [1…4), stored as [2…3] because the upper part is closer to integer.
        env.setRange(4, 1.2,     3.8);          // Round to [1…4), stores as [1…3] because the span is not close enough to integer.
        GridExtent extent = new GridExtent(env);
        assertExtentEquals(extent, 0, 1, 2);
        assertExtentEquals(extent, 1, 1, 2);
        assertExtentEquals(extent, 2, 1, 2);
        assertExtentEquals(extent, 3, 2, 3);
        assertExtentEquals(extent, 4, 1, 3);
        assertExtentEquals(extent, 5, 0, 0);    // Unitialized envelope values were [0…0].
    }

    /**
     * Verifies the low and high values in the specified dimension of the given extent
     */
    private static void assertExtentEquals(final GridExtent extent, final int dimension, final int low, final int high) {
        assertEquals("low",  low,  extent.getLow (dimension));
        assertEquals("high", high, extent.getHigh(dimension));
    }
}
