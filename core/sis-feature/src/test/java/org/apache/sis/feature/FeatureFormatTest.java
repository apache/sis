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

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.apache.sis.test.Assertions.assertMultilinesEquals;

// Branch-dependent import
import org.opengis.feature.PropertyType;


/**
 * Tests {@link FeatureFormat}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.5
 */
@DependsOn({
    DenseFeatureTest.class,
    CharacteristicMapTest.class
})
public final class FeatureFormatTest extends TestCase {
    /**
     * Creates the formatter instance to be used for the tests.
     */
    private static FeatureFormat create() {
        final FeatureFormat format = new FeatureFormat(Locale.US, null);
        format.setAllowedColumns(EnumSet.of(FeatureFormat.Column.NAME,            FeatureFormat.Column.TYPE,
                                            FeatureFormat.Column.CARDINALITY,     FeatureFormat.Column.VALUE,
                                            FeatureFormat.Column.CHARACTERISTICS, FeatureFormat.Column.REMARKS));
        return format;
    }

    /**
     * Tests the formatting of a {@link DefaultFeatureType}.
     */
    @Test
    public void testFeatureType() {
        final DefaultFeatureType feature = DefaultFeatureTypeTest.worldMetropolis();
        final FeatureFormat format = create();
        final String text = format.format(feature);
        assertMultilinesEquals("World metropolis ⇾ Metropolis, University city\n" +
                "┌──────────────┬─────────────────────┬──────────────┬───────────────┬────────────────────────────┐\n" +
                "│ Name         │ Type                │ Multiplicity │ Default value │ Characteristics            │\n" +
                "├──────────────┼─────────────────────┼──────────────┼───────────────┼────────────────────────────┤\n" +
                "│ city         │ String              │      [1 … 1] │ Utopia        │                            │\n" +
                "│ population   │ Integer             │      [1 … 1] │               │                            │\n" +
                "│ region       │ InternationalString │      [1 … 1] │               │                            │\n" +
                "│ isGlobal     │ Boolean             │      [1 … 1] │               │                            │\n" +
                "│ universities │ String              │      [0 … ∞] │               │                            │\n" +
                "│ temperature  │ Float               │      [1 … 1] │               │ accuracy = 0.1, units = °C │\n" +
                "└──────────────┴─────────────────────┴──────────────┴───────────────┴────────────────────────────┘\n", text);
    }

    /**
     * Tests the formatting of a {@link DefaultFeatureType} that contains operations.
     */
    @Test
    @SuppressWarnings("serial")
    public void testFeatureTypeWithOperations() {
        DefaultFeatureType feature = DefaultFeatureTypeTest.city();
        final PropertyType city = feature.getProperty("city");
        feature = new DefaultFeatureType(name("Identified city"), false, new DefaultFeatureType[] {feature},
                FeatureOperations.link(name("someId"), city),
                FeatureOperations.compound(name("anotherId"), ":", "<", ">", city, feature.getProperty("population")),
                AbstractOperationTest.foundCity());

        final FeatureFormat format = create();
        final String text = format.format(feature);
        assertMultilinesEquals("Identified city ⇾ City\n" +
                "┌────────────┬─────────┬──────────────┬─────────────────────┐\n" +
                "│ Name       │ Type    │ Multiplicity │ Default value       │\n" +
                "├────────────┼─────────┼──────────────┼─────────────────────┤\n" +
                "│ city       │ String  │      [1 … 1] │ Utopia              │\n" +
                "│ population │ Integer │      [1 … 1] │                     │\n" +
                "│ someId     │ String  │      [1 … 1] │ = city              │\n" +
                "│ anotherId  │ String  │      [1 … 1] │ = <city:population> │\n" +
                "│ new city   │ String  │      [1 … 1] │ = create(founder)   │\n" +
                "└────────────┴─────────┴──────────────┴─────────────────────┘\n", text);
    }

    /**
     * Convenience method returning the given name in a {@code properties} map.
     */
    private static Map<String,?> name(final String name) {
        return Map.of(DefaultFeatureType.NAME_KEY, name);
    }

    /**
     * Tests the formatting of a {@link DefaultFeatureType} that contains deprecated properties.
     */
    @Test
    @SuppressWarnings("serial")
    public void testFeatureTypeWithDeprecatedProperties() {
        DefaultFeatureType feature = DefaultFeatureTypeTest.city();
        final Map<String,Object> properties = new HashMap<>(name("highway"));
        properties.put(DefaultAttributeType.DEPRECATED_KEY, Boolean.TRUE);
        properties.put(DefaultAttributeType.DESCRIPTION_KEY, "Replaced by pedestrian areas.");
        feature = new DefaultFeatureType(name("City for human"), false, new DefaultFeatureType[] {feature},
                new DefaultAttributeType<>(properties, String.class, 0, 2, null));

        final FeatureFormat format = create();
        final String text = format.format(feature);
        assertMultilinesEquals("City for human ⇾ City\n" +
                "┌────────────┬─────────┬──────────────┬───────────────┬─────────────┐\n" +
                "│ Name       │ Type    │ Multiplicity │ Default value │ Remarks     │\n" +
                "├────────────┼─────────┼──────────────┼───────────────┼─────────────┤\n" +
                "│ city       │ String  │      [1 … 1] │ Utopia        │             │\n" +
                "│ population │ Integer │      [1 … 1] │               │             │\n" +
                "│ highway    │ String  │      [0 … 2] │               │ Deprecated¹ │\n" +
                "└────────────┴─────────┴──────────────┴───────────────┴─────────────┘\n" +
                "¹ Replaced by pedestrian areas.\n", text);
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
        feature.setPropertyValue("population", 13185502);                               // In 2011.
        feature.setPropertyValue("universities", List.of("Waseda", "Keio"));
        feature.setPropertyValue("temperature", Float.NaN);

        final FeatureFormat format = create();
        final String text = format.format(feature);
        assertMultilinesEquals("World metropolis\n" +
                "┌──────────────┬─────────────────────┬─────────────┬──────────────┬────────────────────────────┐\n" +
                "│ Name         │ Type                │ Cardinality │ Value        │ Characteristics            │\n" +
                "├──────────────┼─────────────────────┼─────────────┼──────────────┼────────────────────────────┤\n" +
                "│ city         │ String              │ 1 ∈ [1 … 1] │ Tokyo        │                            │\n" +
                "│ population   │ Integer             │ 1 ∈ [1 … 1] │ 13,185,502   │                            │\n" +
                "│ region       │ InternationalString │ 0 ∉ [1 … 1] │              │                            │\n" +
                "│ isGlobal     │ Boolean             │ 0 ∉ [1 … 1] │              │                            │\n" +
                "│ universities │ String              │ 2 ∈ [0 … ∞] │ Waseda, Keio │                            │\n" +
                "│ temperature  │ Float               │ 1 ∈ [1 … 1] │ NaN          │ accuracy = 0.1, units = °C │\n" +
                "└──────────────┴─────────────────────┴─────────────┴──────────────┴────────────────────────────┘\n", text);
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
        twinTown.setPropertyValue("population", 143240);                    // In 2011.

        final AbstractFeature feature = isSparse ? new SparseFeature(type) : new DenseFeature(type);
        feature.setPropertyValue("city", "Paderborn");
        feature.setPropertyValue("population", 143174);                     // December 31th, 2011
        feature.setPropertyValue("twin town", twinTown);

        final FeatureFormat format = create();
        final String text = format.format(feature);
        assertMultilinesEquals("Twin town\n" +
                "┌────────────┬─────────┬─────────────┬───────────┐\n" +
                "│ Name       │ Type    │ Cardinality │ Value     │\n" +
                "├────────────┼─────────┼─────────────┼───────────┤\n" +
                "│ city       │ String  │ 1 ∈ [1 … 1] │ Paderborn │\n" +
                "│ population │ Integer │ 1 ∈ [1 … 1] │ 143,174   │\n" +
                "│ twin town  │ City    │ 1 ∈ [0 … 1] │ Le Mans   │\n" +
                "└────────────┴─────────┴─────────────┴───────────┘\n", text);
    }
}
