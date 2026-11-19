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
package org.apache.sis.map.service.common;

import java.util.BitSet;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link ContrastStretch}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ContrastStretchTest {
    /**
     * Tolerance on intensities.
     */
    private static final double EPS = 1E-12;

    /**
     * Creates a new test case.
     */
    public ContrastStretchTest() {
    }

    /**
     * Tests the linear stretch of the range of the values.
     */
    @Test
    public void testNormalize() {
        final ContrastStretch stretch = new ContrastStretch(ContrastStretch.Method.NORMALIZE)
                .fit(new double[] {10, 20, Double.NaN, 30});
        assertEquals(0,    stretch.apply(10), EPS);
        assertEquals(0.25, stretch.apply(15), EPS);
        assertEquals(1,    stretch.apply(30), EPS);
        assertEquals(1,    stretch.apply(50), EPS, "Clamped.");
        assertTrue(Double.isNaN(stretch.apply(Double.NaN)));
    }

    /**
     * Tests the scaling from the native range, and the normalization of values outside that range.
     */
    @Test
    public void testNone() {
        ContrastStretch stretch = new ContrastStretch(ContrastStretch.Method.NONE)
                .setNativeRange(0, 255).fit(new double[] {100, 115});
        assertEquals(100 / 255.0, stretch.apply(100), EPS, "Values used as-is.");

        stretch = new ContrastStretch(ContrastStretch.Method.NONE)
                .setNativeRange(0, 255).fit(new double[] {100, 1100});
        assertEquals(0.5, stretch.apply(600), EPS, "Values outside the native range are normalized.");

        stretch = new ContrastStretch(ContrastStretch.Method.NORMALIZE)
                .setNativeRange(0, 255).fit(new double[] {51, 51});
        assertEquals(0.2, stretch.apply(51), EPS, "A single value is scaled from the native range.");
    }

    /**
     * Tests the histogram equalization: the minimum is 0, the maximum is 1,
     * and the intensities are proportional to the cumulative frequencies above the minimum.
     */
    @Test
    public void testHistogram() {
        final double[] values = new double[16];
        java.util.Arrays.fill(values,  8, 12, 10);
        java.util.Arrays.fill(values, 12, 16, 100);
        final ContrastStretch stretch = new ContrastStretch(ContrastStretch.Method.HISTOGRAM).fit(values);
        assertEquals(0,   stretch.apply(0),   EPS);
        assertEquals(0.5, stretch.apply(10),  EPS);
        assertEquals(1,   stretch.apply(100), EPS);
    }

    /**
     * Tests the gamma value applied after the method.
     */
    @Test
    public void testGamma() {
        final ContrastStretch stretch = new ContrastStretch(ContrastStretch.Method.NONE)
                .setNativeRange(0, 255).setGamma(2).fit(new double[] {64, 255});
        assertEquals(Math.sqrt(64 / 255.0), stretch.apply(64), EPS);
        assertThrows(IllegalArgumentException.class, () -> stretch.setGamma(0));
        assertThrows(IllegalArgumentException.class, () -> stretch.setNativeRange(1, 1));
    }

    /**
     * Tests the in-place enhancement of the cells of a channel.
     */
    @Test
    public void testCells() {
        final float[] channel = {0.2f, 0.4f, 0.9f, 0.3f};
        final BitSet cells = new BitSet();
        cells.set(0, 2);
        cells.set(3);
        new ContrastStretch(ContrastStretch.Method.NORMALIZE).fit(channel, cells).apply(channel, cells);
        assertArrayEquals(new float[] {0, 1, 0.9f, 0.5f}, channel, 1E-6f, "The cell 2 is excluded.");
    }
}
