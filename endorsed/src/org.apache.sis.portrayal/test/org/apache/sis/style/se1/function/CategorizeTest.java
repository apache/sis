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
package org.apache.sis.style.se1.function;

import java.awt.Color;
import java.util.List;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Literal;


/**
 * Tests for {@link Categorize}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class CategorizeTest extends TestCase {
    /**
     * The factory of filters and expressions.
     */
    private final FilterFactory<Feature,Object,Object> ff;

    /**
     * The type of features used for the tests, with a "population" and a "color" attribute.
     */
    private final FeatureType type;

    /**
     * Creates a new test case.
     */
    public CategorizeTest() {
        ff = DefaultFilterFactory.forFeatures();
        final var ftb = new FeatureTypeBuilder().setName("City");
        ftb.addAttribute(Integer.class).setName("population");
        ftb.addAttribute(String.class).setName("color");
        type = ftb.build();
    }

    /**
     * Creates a feature with the given population.
     */
    private Feature city(final Integer population) {
        final Feature feature = type.newInstance();
        feature.setPropertyValue("population", population);
        return feature;
    }

    /**
     * Creates the function used by most tests: green below 1000, yellow below 10000, red above.
     */
    private Categorize<Feature,Color> function(final Categorize.ThresholdsBelongTo belongTo) {
        return new Categorize<>(ff.property("population"),
                List.of(ff.literal(Color.GREEN), ff.literal(Color.YELLOW), ff.literal(Color.RED)),
                List.of(ff.literal(1000), ff.literal(10000)),
                belongTo, ff.literal(Color.BLACK));
    }

    /**
     * Tests the default values of optional properties.
     */
    @Test
    public void testDefaults() {
        final var cdt = new Categorize<Feature,Color>(null, List.of(ff.literal(Color.RED)), List.of(), null, null);
        assertEquals("Rasterdata", ((Literal<?,?>) cdt.getLookupValue()).getValue());
        assertEquals(Categorize.ThresholdsBelongTo.SUCCEEDING, cdt.getThresholdsBelongTo());
        assertTrue(cdt.getFallbackValue().isEmpty());
        assertEquals("Categorize", cdt.getFunctionName().tip().toString());
    }

    /**
     * Tests that the number of values must be the number of thresholds plus one.
     */
    @Test
    public void testInvalidSizes() {
        assertThrows(IllegalArgumentException.class, () -> new Categorize<Feature,Color>(null,
                List.of(ff.literal(Color.RED)), List.of(ff.literal(10)), null, null));
    }

    /**
     * Tests the categorization of features with thresholds belonging to the succeeding interval.
     */
    @Test
    public void testApplySucceeding() {
        final var cdt = function(null);
        assertEquals(Color.GREEN,  cdt.apply(city(500)));
        assertEquals(Color.YELLOW, cdt.apply(city(1000)));
        assertEquals(Color.YELLOW, cdt.apply(city(9999)));
        assertEquals(Color.RED,    cdt.apply(city(10000)));
        assertEquals(Color.BLACK,  cdt.apply(city(null)), "Fallback value expected for missing values.");
    }

    /**
     * Tests the categorization of features with thresholds belonging to the preceding interval.
     */
    @Test
    public void testApplyPreceding() {
        final var cdt = function(Categorize.ThresholdsBelongTo.PRECEDING);
        assertEquals(Color.GREEN,  cdt.apply(city(1000)));
        assertEquals(Color.YELLOW, cdt.apply(city(1001)));
        assertEquals(Color.YELLOW, cdt.apply(city(10000)));
        assertEquals(Color.RED,    cdt.apply(city(10001)));
    }

    /**
     * Tests the categorization of arbitrary values with thresholds and values evaluated once.
     */
    @Test
    public void testBind() {
        final var f = function(null).bind(null);
        assertEquals(Color.GREEN,  f.apply(-5));
        assertEquals(Color.YELLOW, f.apply(5000.5));
        assertEquals(Color.RED,    f.apply(1E9));
        assertEquals(Color.BLACK,  f.apply(Double.NaN));
    }

    /**
     * Tests the categorization of values which are not numbers but are orderable, here strings.
     */
    @Test
    public void testComparable() {
        final var f = new Categorize<Feature,String>(null,
                List.of(ff.literal("low"), ff.literal("middle"), ff.literal("high")),
                List.of(ff.literal("C"), ff.literal("M")), null, ff.literal("none")).bind(null);
        assertEquals("low",    f.apply("Alpha"));
        assertEquals("middle", f.apply("C"));
        assertEquals("middle", f.apply("Delta"));
        assertEquals("high",   f.apply("Zulu"));
        assertEquals("none",   f.apply(5), "Fallback value expected for values not comparable to thresholds.");
    }

    /**
     * Tests values computed from feature properties, converted to colors.
     */
    @Test
    public void testValuesFromFeature() {
        final Categorize<Feature,Object> cdt = new Categorize<>(ff.property("population"),
                List.of(ff.literal("#00FF00"), ff.property("color")), List.of(ff.literal(1000)), null, null);
        final Expression<Feature,Color> colors = cdt.toValueType(Color.class);
        assertInstanceOf(Categorize.class, colors);
        final Feature feature = city(5000);
        feature.setPropertyValue("color", "#0000FF");
        assertEquals(Color.BLUE, colors.apply(feature));
        assertEquals(Color.GREEN, colors.apply(city(10)));
    }

    /**
     * Tests the creation of the function by name in a filter factory, and the function parameters.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testFilterFactory() {
        final Expression<Feature,?> fn = ff.function("Categorize", new Expression[] {ff.property("population"),
                ff.literal(Color.GREEN), ff.literal(1000), ff.literal(Color.YELLOW), ff.literal(10000), ff.literal(Color.RED),
                ff.literal("preceding")});
        final var cdt = assertInstanceOf(Categorize.class, fn);
        assertEquals(Categorize.ThresholdsBelongTo.PRECEDING, cdt.getThresholdsBelongTo());
        assertEquals(3, cdt.getValues().size());
        assertEquals(Color.GREEN, fn.apply(city(1000)));
        assertEquals(Color.RED,   fn.apply(city(20000)));

        // The parameters allow to recreate the same function.
        final List<Expression<Feature,?>> parameters = fn.getParameters();
        assertEquals(7, parameters.size());
        assertEquals(fn, ff.function("Categorize", parameters.toArray(Expression[]::new)));
    }

    /**
     * Tests {@code equals} and {@code hashCode}.
     */
    @Test
    public void testEquals() {
        assertEquals(function(null), function(null));
        assertEquals(function(null).hashCode(), function(null).hashCode());
        assertNotEquals(function(null), function(Categorize.ThresholdsBelongTo.PRECEDING));
    }
}
