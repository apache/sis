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
package org.apache.sis.feature;

import java.util.Locale;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link FeatureFormat}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(DenseFeatureTest.class)
public final strictfp class FeatureFormatTest extends TestCase {
    /**
     * Tests the formatting of a {@link DefaultFeatureType}.
     */
    @Test
    public void testFeatureType() {
        final DefaultFeatureType feature = DefaultFeatureTypeTest.metropolis();
        final FeatureFormat format = new FeatureFormat(Locale.US, null);
        final String text = format.format(feature);
        assertMultilinesEquals("Metropolis\n" +
                "┌────────────┬──────────────┬─────────────┬───────────────┐\n" +
                "│ Name       │ Type         │ Cardinality │ Default value │\n" +
                "├────────────┼──────────────┼─────────────┼───────────────┤\n" +
                "│ city       │ String       │ [1 … 1]     │ Utopia        │\n" +
                "│ population │ Integer      │ [1 … 1]     │               │\n" +
                "│ region     │ CharSequence │ [1 … 1]     │               │\n" +
                "│ isGlobal   │ Boolean      │ [1 … 1]     │               │\n" +
                "└────────────┴──────────────┴─────────────┴───────────────┘\n", text);
    }

    /**
     * Tests the formatting of an {@link AbstractFeature}.
     */
    @Test
    public void testFeature() {
        final AbstractFeature feature = FeatureTestCase.twinTown();
        final FeatureFormat format = new FeatureFormat(Locale.US, null);
        final String text = format.format(feature);
        assertMultilinesEquals("Twin town\n" +
                "┌────────────┬─────────┬─────────────┬───────────┐\n" +
                "│ Name       │ Type    │ Cardinality │ Value     │\n" +
                "├────────────┼─────────┼─────────────┼───────────┤\n" +
                "│ city       │ String  │ [1 … 1]     │ Paderborn │\n" +
                "│ population │ Integer │ [1 … 1]     │ 143,174   │\n" +
                "│ twin town  │ City    │ [0 … ∞]     │ Le Mans   │\n" +
                "└────────────┴─────────┴─────────────┴───────────┘\n", text);
    }
}
