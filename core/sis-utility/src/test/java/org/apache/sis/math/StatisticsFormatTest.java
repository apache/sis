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
package org.apache.sis.math;

import java.util.Locale;
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOnMethod;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link StatisticsFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class StatisticsFormatTest extends TestCase {
    /**
     * Tests the formatting of {@code Statistics} without column headers.
     * We instantiate the {@link StatisticsFormat} directly in order to fix the locale
     * to a hard-coded value. But except for the localization, the result should be
     * nearly identical to a call to the {@link Statistics#toString()} method.
     */
    @Test
    public void testFormattingWithoutHeader() {
        final Statistics statistics = Statistics.forSeries(null, null, null);
        statistics.accept(10);
        statistics.accept(15);
        statistics.accept(22);
        statistics.accept(17);
        statistics.accept(12);
        statistics.accept( 3);

        final StatisticsFormat format = StatisticsFormat.getInstance(Locale.US);
        final String text = format.format(statistics);
        assertMultilinesEquals(
                "Number of values:       6     5      4\n" +
                "Minimum value:       3.00 -9.00 -12.00\n" +
                "Maximum value:      22.00  7.00   2.00\n" +
                "Mean value:         13.17 -1.40  -3.50\n" +
                "Root Mean Square:   14.44  6.40   6.40\n" +
                "Standard deviation:  6.49  6.99   6.19\n", text);
    }

    /**
     * Tests the formatting of {@code Statistics} with column headers and a border.
     * This test uses the same numerical values than {@link #testFormattingWithoutHeader()}.
     */
    @Test
    @DependsOnMethod("testFormattingWithoutHeader")
    public void testFormattingWithBorder() {
        final Statistics statistics = Statistics.forSeries("Temperature", "∂T/∂t", "∂²T/∂t²");
        statistics.accept(10);
        statistics.accept(15);
        statistics.accept(22);
        statistics.accept(17);
        statistics.accept(12);
        statistics.accept( 3);

        final StatisticsFormat format = StatisticsFormat.getInstance(Locale.US);
        format.setBorderWidth(1);
        final String text = format.format(statistics);
        assertMultilinesEquals(
                "┌─────────────────────┬─────────────┬───────┬─────────┐\n" +
                "│                     │ Temperature │ ∂T/∂t │ ∂²T/∂t² │\n" +
                "├─────────────────────┼─────────────┼───────┼─────────┤\n" +
                "│ Number of values:   │           6 │     5 │       4 │\n" +
                "│ Minimum value:      │        3.00 │ -9.00 │  -12.00 │\n" +
                "│ Maximum value:      │       22.00 │  7.00 │    2.00 │\n" +
                "│ Mean value:         │       13.17 │ -1.40 │   -3.50 │\n" +
                "│ Root Mean Square:   │       14.44 │  6.40 │    6.40 │\n" +
                "│ Standard deviation: │        6.49 │  6.99 │    6.19 │\n" +
                "└─────────────────────┴─────────────┴───────┴─────────┘\n", text);
    }
}
