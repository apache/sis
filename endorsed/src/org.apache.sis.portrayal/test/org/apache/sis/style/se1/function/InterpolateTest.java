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
import java.util.Optional;
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
 * Tests for {@link Interpolate} and {@link InterpolationPoint}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class InterpolateTest extends TestCase {
    /**
     * The factory of filters and expressions.
     */
    private final FilterFactory<Feature,Object,Object> ff;

    /**
     * The type of features used for the tests, with a "temperature" attribute.
     */
    private final FeatureType type;

    /**
     * Creates a new test case.
     */
    public InterpolateTest() {
        ff = DefaultFilterFactory.forFeatures();
        final var ftb = new FeatureTypeBuilder().setName("Station");
        ftb.addAttribute(Double.class).setName("temperature");
        type = ftb.build();
    }

    /**
     * Creates a feature with the given temperature.
     */
    private Feature station(final Double temperature) {
        final Feature feature = type.newInstance();
        feature.setPropertyValue("temperature", temperature);
        return feature;
    }

    /**
     * Creates an interpolation point.
     */
    private Interpolate.InterpolationPoint<Feature> point(final double data, final Object value) {
        return new Interpolate.InterpolationPoint<>(ff.literal(data), ff.literal(value));
    }

    /**
     * Creates an interpolation of the temperature property.
     */
    private Interpolate<Feature> function(final Interpolate.Mode mode, final Interpolate.Method method,
                                          final List<Interpolate.InterpolationPoint<Feature>> points)
    {
        return new Interpolate<>(ff.property("temperature"), points, mode, method, ff.literal(Color.MAGENTA));
    }

    /**
     * Tests the default values of optional properties.
     */
    @Test
    public void testDefaults() {
        final var cdt = new Interpolate<Feature>(null, List.of(), null, null, null);
        assertEquals("Rasterdata", ((Literal<?,?>) cdt.getLookupValue()).getValue());
        assertEquals(Interpolate.Mode.LINEAR, cdt.getMode());
        assertTrue(cdt.getMethod().isEmpty(), "Method determined from the values.");
        assertTrue(cdt.getFallbackValue().isEmpty());
        assertEquals("Interpolate", cdt.getFunctionName().tip().toString());
        assertNull(cdt.apply(null), "No interpolation point.");
    }

    /**
     * Tests linear interpolation of colors on features, including values outside the range of points.
     */
    @Test
    public void testLinearColor() {
        final var cdt = function(null, null, List.of(point(0, Color.BLACK), point(100, Color.WHITE)));
        assertEquals(new Color(128, 128, 128), cdt.apply(station(50.0)));
        assertEquals(new Color(64, 64, 64),    cdt.apply(station(25.0)));
        assertEquals(Color.BLACK,   cdt.apply(station(-10.0)), "Below the first point.");
        assertEquals(Color.WHITE,   cdt.apply(station(1000.0)), "Above the last point.");
        assertEquals(Color.MAGENTA, cdt.apply(station(null)), "Fallback value expected for missing values.");
    }

    /**
     * Tests interpolation of colors with alpha, and colors given as strings.
     */
    @Test
    public void testColorComponents() {
        final var cdt = function(null, null, List.of(point(0, new Color(255, 0, 0, 0)), point(10, "#FF0000FF")));
        assertEquals(new Color(128, 0, 128, 128), cdt.apply(station(5.0)));
    }

    /**
     * Tests the automatic method: numbers are interpolated as numbers, even when given as strings.
     */
    @Test
    public void testAutomaticMethod() {
        final var cdt = function(null, null, List.of(point(0, "10"), point(10, 30)));
        assertEquals(20.0, cdt.apply(station(5.0)));
    }

    /**
     * Tests linear interpolation of numbers, with points given in arbitrary order.
     */
    @Test
    public void testLinearNumeric() {
        final var cdt = function(null, Interpolate.Method.NUMERIC, List.of(point(10, 30), point(0, 10)));
        assertEquals(20.0, cdt.apply(station(5.0)));
        assertEquals(10.0, cdt.apply(station(-5.0)));
        assertEquals(30.0, cdt.apply(station(15.0)));
    }

    /**
     * Tests cosine interpolation of numbers.
     */
    @Test
    public void testCosine() {
        final var cdt = function(Interpolate.Mode.COSINE, Interpolate.Method.NUMERIC, List.of(point(0, 10), point(10, 30)));
        assertEquals(10 + 20 * (1 - Math.cos(Math.PI / 4)) / 2, (Double) cdt.apply(station(2.5)), 1E-12);
        assertEquals(20.0, (Double) cdt.apply(station(5.0)), 1E-12);
    }

    /**
     * Tests cubic interpolation of numbers: the curve passes through all points and is smooth.
     */
    @Test
    public void testCubic() {
        final var cdt = function(Interpolate.Mode.CUBIC, Interpolate.Method.NUMERIC,
                                 List.of(point(0, 0), point(10, 10), point(20, 0)));
        assertEquals(10.0, (Double) cdt.apply(station(10.0)), 1E-12);
        final double before = (Double) cdt.apply(station(9.0));
        final double after  = (Double) cdt.apply(station(11.0));
        assertTrue(before > 5 && before < 10);
        assertEquals(before, after, 1E-12, "Symmetric curve expected around the middle point.");
        assertTrue(before > 9, "Cubic curve expected to be rounder than linear interpolation (9).");
    }

    /**
     * Tests the conversion to colors and the interpolation of arbitrary values with points evaluated once.
     */
    @Test
    public void testToValueTypeAndBind() {
        final var cdt = function(null, null, List.of(point(0, Color.BLACK), point(100, Color.WHITE)));
        final Expression<Feature,Color> colors = cdt.toValueType(Color.class);
        assertEquals(new Color(128, 128, 128), colors.apply(station(50.0)));
        final var f = cdt.bind(null);
        assertEquals(Color.WHITE,   f.apply(100));
        assertEquals(Color.MAGENTA, f.apply(Double.NaN));
    }

    /**
     * Tests the creation of the function by name in a filter factory, and the function parameters.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testFilterFactory() {
        final Expression<Feature,?> fn = ff.function("Interpolate", new Expression[] {ff.property("temperature"),
                ff.literal(0), ff.literal(10), ff.literal(10), ff.literal(30),
                ff.literal("cosine"), ff.literal("numeric")});
        final var cdt = assertInstanceOf(Interpolate.class, fn);
        assertEquals(Interpolate.Mode.COSINE, cdt.getMode());
        assertEquals(Optional.of(Interpolate.Method.NUMERIC), cdt.getMethod());
        assertEquals(2, cdt.getInterpolationPoints().size());
        assertEquals(20.0, (Double) fn.apply(station(5.0)), 1E-12);

        // The parameters allow to recreate the same function.
        final List<Expression<Feature,?>> parameters = fn.getParameters();
        assertEquals(7, parameters.size());
        assertEquals(fn, ff.function("Interpolate", parameters.toArray(Expression[]::new)));
    }

    /**
     * Tests {@code equals} and {@code hashCode}.
     */
    @Test
    public void testEquals() {
        final var points = List.of(point(0, Color.BLACK), point(100, Color.WHITE));
        assertEquals(function(null, null, points), function(null, null, points));
        assertEquals(function(null, null, points).hashCode(), function(null, null, points).hashCode());
        assertNotEquals(function(null, null, points), function(Interpolate.Mode.CUBIC, null, points));
    }
}
