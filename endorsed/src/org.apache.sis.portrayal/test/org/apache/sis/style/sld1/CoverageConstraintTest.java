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
package org.apache.sis.style.sld1;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for {@link CoverageConstraint}, {@link CoverageExtent} and {@link RangeAxis}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class CoverageConstraintTest extends SLDTestCase {
    /**
     * Creates a new test case.
     */
    public CoverageConstraintTest() {
    }

    /**
     * Creates a range axis with the given name and value.
     */
    private static RangeAxis rangeAxis(final String name, final String value) {
        final var axis = new RangeAxis();
        axis.setName(name);
        axis.setValue(value);
        return axis;
    }

    /**
     * Test of {@code CoverageName} property.
     */
    @Test
    public void testCoverageName() {
        final var cdt = new CoverageConstraint();

        // Check defaults
        assertEmpty(cdt.getCoverageName());

        // Check get/set
        cdt.setCoverageName("the_coverage_name");
        assertOptionalEquals("the_coverage_name", cdt.getCoverageName());
    }

    /**
     * Test of {@code CoverageExtent} property.
     */
    @Test
    public void testCoverageExtent() {
        final var cdt = new CoverageConstraint();

        // Check defaults
        assertEmpty(cdt.getCoverageExtent());

        // Check get/set
        var value = new CoverageExtent();
        value.setTimePeriod("2005-05-17T00:00:00Z/2005-05-23T00:00:00Z");
        cdt.setCoverageExtent(value);
        assertOptionalEquals(value, cdt.getCoverageExtent());
    }

    /**
     * Test of {@code RangeAxis} and {@code TimePeriod} properties of {@link CoverageExtent}.
     */
    @Test
    public void testExtentProperties() {
        final var cdt = new CoverageExtent();

        // Check defaults
        assertTrue(cdt.rangeAxes().isEmpty());
        assertEmpty(cdt.getTimePeriod());

        // Check get/set
        var band = rangeAxis("Band", "band_1");
        var time = rangeAxis("Time", "2005-05-19T09:28:40Z");
        cdt.rangeAxes().add(band);
        cdt.rangeAxes().add(time);
        assertEquals(2, cdt.rangeAxes().size());
        assertSame(band, cdt.rangeAxes().get(0));
        assertSame(time, cdt.rangeAxes().get(1));

        final var period = new CoverageExtent();
        period.setTimePeriod("2005-05-17T00:00:00Z/2005-05-23T00:00:00Z");
        assertOptionalEquals("2005-05-17T00:00:00Z/2005-05-23T00:00:00Z", period.getTimePeriod());
    }

    /**
     * Test of {@code Name} and {@code Value} properties of {@link RangeAxis}.
     */
    @Test
    public void testRangeAxis() {
        final var cdt = new RangeAxis();

        // Check defaults
        assertEmpty(cdt.getName());
        assertEmpty(cdt.getValue());

        // Check get/set
        cdt.setName("Band");
        cdt.setValue("band_1");
        assertOptionalEquals("Band",   cdt.getName());
        assertOptionalEquals("band_1", cdt.getValue());

        final var clone = cdt.clone();
        assertCloneEquals(cdt, clone);
        clone.setValue("band_2");
        assertNotEquals(cdt, clone);
    }

    /**
     * Tests {@code equals}, {@code hashCode} and deep {@code clone}.
     */
    @Test
    public void testClone() {
        final var extent = new CoverageExtent();
        final var axis = rangeAxis("Band", "band_1");
        extent.rangeAxes().add(axis);
        final var cdt = new CoverageConstraint();
        cdt.setCoverageName("the_coverage_name");
        cdt.setCoverageExtent(extent);

        final var clone = cdt.clone();
        assertCloneEquals(cdt, clone);
        final var clonedExtent = clone.getCoverageExtent().orElseThrow();
        assertNotSame(extent, clonedExtent);
        assertNotSame(extent.rangeAxes(), clonedExtent.rangeAxes());
        assertNotSame(axis, clonedExtent.rangeAxes().get(0));
        assertEquals (axis, clonedExtent.rangeAxes().get(0));

        clonedExtent.rangeAxes().clear();
        assertNotEquals(cdt, clone);
        assertEquals(1, extent.rangeAxes().size());
    }
}
