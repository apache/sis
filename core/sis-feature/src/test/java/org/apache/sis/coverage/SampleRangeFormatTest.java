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
package org.apache.sis.coverage;

import java.util.Locale;
import org.apache.sis.measure.Units;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assertions.assertMultilinesEquals;


/**
 * Tests {@link SampleDimension#toString(Locale, SampleDimension...)}.
 * Note that the formatting may change in any future version.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final class SampleRangeFormatTest extends TestCase {
    /**
     * Creates a band for temperature data.
     */
    private static SampleDimension temperature(final SampleDimension.Builder builder) {
        builder.clear();
        return builder.setName("Temperature").addQualitative("No data", 0)
               .addQuantitative("Value", 1, 256, 0.15, -5, Units.CELSIUS).build();
    }

    /**
     * Creates a band for salinity data.
     */
    private static SampleDimension salinity(final SampleDimension.Builder builder) {
        builder.clear();
        return builder.setName("Salinity").addQualitative("No data", 0)
               .addQuantitative("Value", 1, 256, 0.01, 20, Units.PSU).build();
    }

    /**
     * Tests formatting of a single band with some categories.
     */
    @Test
    public void testSingleBand() {
        final SampleDimension.Builder builder = new SampleDimension.Builder();
        final String text = SampleDimension.toString(Locale.US, temperature(builder));
        assertMultilinesEquals(
                "┌───────────┬─────────────────┬─────────┐\n" +
                "│  Values   │    Measures     │  Name   │\n" +
                "╞═══════════╧═════════════════╧═════════╡\n" +
                "│ Temperature                           │\n" +
                "├───────────┬─────────────────┬─────────┤\n" +
                "│        0  │ NaN #0          │ No data │\n" +
                "│ [1 … 256) │ [-4.8 … 33.4)°C │ Value   │\n" +
                "└───────────┴─────────────────┴─────────┘\n", text);
    }

    /**
     * Tests formatting of more than one band.
     */
    @Test
    public void testMultiBands() {
        final SampleDimension.Builder builder = new SampleDimension.Builder();
        final String text = SampleDimension.toString(Locale.US, temperature(builder), salinity(builder));
        assertMultilinesEquals(
                "┌───────────┬───────────────────┬─────────┐\n" +
                "│  Values   │     Measures      │  Name   │\n" +
                "╞═══════════╧═══════════════════╧═════════╡\n" +
                "│ Temperature                             │\n" +
                "├───────────┬───────────────────┬─────────┤\n" +
                "│        0  │ NaN #0            │ No data │\n" +
                "│ [1 … 256) │ [-4.8 … 33.4)°C   │ Value   │\n" +
                "╞═══════════╧═══════════════════╧═════════╡\n" +
                "│ Salinity                                │\n" +
                "├───────────┬───────────────────┬─────────┤\n" +
                "│        0  │ NaN #0            │ No data │\n" +
                "│ [1 … 256) │ [20.0 … 22.6) psu │ Value   │\n" +
                "└───────────┴───────────────────┴─────────┘\n", text);
    }

    /**
     * Tests formatting of sample dimensions having only qualitative categories.
     */
    @Test
    public void testQualitativeOnly() {
        final SampleDimension land = new SampleDimension.Builder().setName("Land use")
                .addQualitative("Lake",   0)
                .addQualitative("Forest", 1)
                .addQualitative("Urban",  2).build();
        final String text = SampleDimension.toString(Locale.US, land);
        assertMultilinesEquals(
                "┌────────┬────────┐\n" +
                "│ Values │  Name  │\n" +
                "╞════════╧════════╡\n" +
                "│ Land use        │\n" +
                "├────────┬────────┤\n" +
                "│     0  │ Lake   │\n" +
                "│     1  │ Forest │\n" +
                "│     2  │ Urban  │\n" +
                "└────────┴────────┘\n", text);
    }

    /**
     * Tests formatting of sample dimensions having only names.
     */
    @Test
    public void testNameOnly() {
        final SampleDimension.Builder builder = new SampleDimension.Builder();
        final SampleDimension red   = builder.setName("Red"  ).build(); builder.clear();
        final SampleDimension green = builder.setName("Green").build(); builder.clear();
        final SampleDimension blue  = builder.setName("Blue" ).build(); builder.clear();
        final String text = SampleDimension.toString(Locale.US, red, green, blue);
        assertMultilinesEquals(
                "┌───────┐\n" +
                "│ Name  │\n" +
                "╞═══════╡\n" +
                "│ Red   │\n" +
                "│ Green │\n" +
                "│ Blue  │\n" +
                "└───────┘\n", text);
    }
}
