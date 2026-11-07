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
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests for {@link Recode} and {@link MapItem}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class RecodeTest extends TestCase {
    /**
     * The factory of filters and expressions.
     */
    private final FilterFactory<Feature,Object,Object> ff;

    /**
     * The type of features used for the tests, with a "kind" and a "code" attribute.
     */
    private final FeatureType type;

    /**
     * Creates a new test case.
     */
    public RecodeTest() {
        ff = DefaultFilterFactory.forFeatures();
        final var ftb = new FeatureTypeBuilder().setName("Road");
        ftb.addAttribute(String.class).setName("kind");
        ftb.addAttribute(Integer.class).setName("code");
        type = ftb.build();
    }

    private Feature road(final String kind, final Integer code) {
        final Feature feature = type.newInstance();
        feature.setPropertyValue("kind", kind);
        feature.setPropertyValue("code", code);
        return feature;
    }

    private Recode.MapItem<Feature> item(final Object data, final Object value) {
        return new Recode.MapItem<>(ff.literal(data), ff.literal(value));
    }

    /**
     * Tests recoding of strings on features, with a fallback value.
     */
    @Test
    public void testStrings() {
        final var cdt = new Recode<>(ff.property("kind"),
                List.of(item("highway", Color.RED), item("street", Color.GRAY)), ff.literal(Color.BLACK));
        assertEquals(Color.RED,   cdt.apply(road("highway", null)));
        assertEquals(Color.GRAY,  cdt.apply(road("street", null)));
        assertEquals(Color.BLACK, cdt.apply(road("path", null)), "Fallback value expected.");
        assertEquals(Color.BLACK, cdt.apply(road(null, null)),   "Fallback value expected.");
        assertEquals("Recode", cdt.getFunctionName().tip().toString());
    }

    /**
     * Tests that numbers are compared by value regardless of their type, and that there is no fallback by default.
     */
    @Test
    public void testNumbers() {
        final var cdt = new Recode<>(ff.property("code"), List.of(item(1.0, "one"), item(2L, "two")), null);
        assertEquals("one", cdt.apply(road(null, 1)));
        assertEquals("two", cdt.apply(road(null, 2)));
        assertNull(cdt.apply(road(null, 3)));
        assertTrue(cdt.getFallbackValue().isEmpty());
    }

    /**
     * Tests that colors are compared by value, including colors encoded as strings.
     */
    @Test
    public void testColors() {
        final var f = new Recode<Feature>(null, List.of(item("#FF0000", Color.BLUE), item(Color.GREEN, "#FFFF00")), null).bind(null);
        assertEquals(Color.BLUE, f.apply(new Color(255, 0, 0)));
        assertEquals(Color.BLUE, f.apply("#ff0000"));
        assertEquals("#FFFF00",  f.apply(Color.GREEN));
        assertNull(f.apply(Color.WHITE));
    }

    /**
     * Tests the creation of the function by name in a filter factory, and the function parameters.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testFilterFactory() {
        final Expression<Feature,?> fn = ff.function("Recode", new Expression[] {ff.property("kind"),
                ff.literal("highway"), ff.literal(Color.RED), ff.literal("street"), ff.literal(Color.GRAY)});
        final var cdt = assertInstanceOf(Recode.class, fn);
        assertEquals(2, cdt.getMapItems().size());
        assertEquals(Color.GRAY, fn.apply(road("street", null)));

        final List<Expression<Feature,?>> parameters = fn.getParameters();
        assertEquals(5, parameters.size());
        assertEquals(fn, ff.function("Recode", parameters.toArray(Expression[]::new)));
    }
}
