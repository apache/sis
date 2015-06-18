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

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link FeatureFormat}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
@DependsOn({
    DenseFeatureTest.class,
    CharacteristicMapTest.class
})
public final strictfp class FeatureFormatTest extends TestCase {
    /**
     * Tests the formatting of a {@link DefaultFeatureType}.
     */
    @Test
    public void testFeatureType() {
        final DefaultFeatureType feature = DefaultFeatureTypeTest.worldMetropolis();
        final FeatureFormat format = new FeatureFormat(Locale.US, null);
        final String text = format.format(feature);
        assertMultilinesEquals("World metropolis\n" +
                "┌──────────────┬─────────────────────┬─────────────┬───────────────┬────────────────────────────┐\n" +
                "│ Name         │ Type                │ Cardinality │ Default value │ Characteristics            │\n" +
                "├──────────────┼─────────────────────┼─────────────┼───────────────┼────────────────────────────┤\n" +
                "│ city         │ String              │ [1 … 1]     │ Utopia        │                            │\n" +
                "│ population   │ Integer             │ [1 … 1]     │               │                            │\n" +
                "│ region       │ InternationalString │ [1 … 1]     │               │                            │\n" +
                "│ isGlobal     │ Boolean             │ [1 … 1]     │               │                            │\n" +
                "│ universities │ String              │ [0 … ∞]     │               │                            │\n" +
                "│ temperature  │ Float               │ [1 … 1]     │               │ accuracy = 0.1, units = °C │\n" +
                "└──────────────┴─────────────────────┴─────────────┴───────────────┴────────────────────────────┘\n", text);
    }

    /**
     * Tests the formatting of an {@link AbstractFeature}.
     */
    @Test
    public void testFeature() {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final boolean isSparse = random.nextBoolean();

        final DefaultFeatureType type = DefaultFeatureTypeTest.worldMetropolis();
        final AbstractFeature feature = isSparse ? new SparseFeature(type) : new DenseFeature(type);
        feature.setPropertyValue("city", "Tokyo");
        feature.setPropertyValue("population", 13185502); // In 2011.
        feature.setPropertyValue("universities", Arrays.asList("Waseda", "Keio"));

        final FeatureFormat format = new FeatureFormat(Locale.US, null);
        final String text = format.format(feature);
        assertMultilinesEquals("World metropolis\n" +
                "┌──────────────┬─────────────────────┬─────────────┬──────────────┬─────────────────┐\n" +
                "│ Name         │ Type                │ Cardinality │ Value        │ Characteristics │\n" +
                "├──────────────┼─────────────────────┼─────────────┼──────────────┼─────────────────┤\n" +
                "│ city         │ String              │ [1 … 1]     │ Tokyo        │                 │\n" +
                "│ population   │ Integer             │ [1 … 1]     │ 13,185,502   │                 │\n" +
                "│ region       │ InternationalString │ [1 … 1]     │              │                 │\n" +
                "│ isGlobal     │ Boolean             │ [1 … 1]     │              │                 │\n" +
                "│ universities │ String              │ [0 … ∞]     │ Waseda, Keio │                 │\n" +
                "│ temperature  │ Float               │ [1 … 1]     │              │ accuracy, units │\n" +
                "└──────────────┴─────────────────────┴─────────────┴──────────────┴─────────────────┘\n", text);
    }

    /**
     * Tests the formatting of an {@link AbstractFeature} with an association to another feature of the same type.
     */
    @Test
    public void testFeatureWithAssociation() {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final boolean isSparse = random.nextBoolean();

        final DefaultFeatureType type = DefaultAssociationRoleTest.twinTownCity(false);
        final AbstractFeature twinTown = isSparse ? new SparseFeature(type) : new DenseFeature(type);
        twinTown.setPropertyValue("city", "Le Mans");
        twinTown.setPropertyValue("population", 143240); // In 2011.

        final AbstractFeature feature = isSparse ? new SparseFeature(type) : new DenseFeature(type);
        feature.setPropertyValue("city", "Paderborn");
        feature.setPropertyValue("population", 143174); // December 31th, 2011
        feature.setPropertyValue("twin town", twinTown);

        final FeatureFormat format = new FeatureFormat(Locale.US, null);
        final String text = format.format(feature);
        assertMultilinesEquals("Twin town\n" +
                "┌────────────┬─────────┬─────────────┬───────────┐\n" +
                "│ Name       │ Type    │ Cardinality │ Value     │\n" +
                "├────────────┼─────────┼─────────────┼───────────┤\n" +
                "│ city       │ String  │ [1 … 1]     │ Paderborn │\n" +
                "│ population │ Integer │ [1 … 1]     │ 143,174   │\n" +
                "│ twin town  │ City    │ [0 … 1]     │ Le Mans   │\n" +
                "└────────────┴─────────┴─────────────┴───────────┘\n", text);
    }
}
