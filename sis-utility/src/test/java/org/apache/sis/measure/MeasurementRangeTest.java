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
package org.apache.sis.measure;

import javax.measure.unit.SI;
import javax.measure.converter.ConversionException;
import org.junit.Test;
import org.apache.sis.test.DependsOn;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link MeasurementRange} class.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@DependsOn(NumberRangeTest.class)
public final strictfp class MeasurementRangeTest {
    /**
     * Tests unit conversions.
     *
     * @throws ConversionException Should not happen.
     */
    @Test
    public void testConversion() throws ConversionException {
        final MeasurementRange<Float> range = MeasurementRange.create(1000f, 2000f, SI.METRE);
        assertSame(range, range.convertTo(SI.METRE));
        assertEquals(MeasurementRange.create(1f, 2f, SI.KILOMETRE), range.convertTo(SI.KILOMETRE));
    }

    /**
     * Tests union and intersection involving a unit conversion.
     */
    @Test
    public void testIntersectWithConversion() {
        NumberRange<Float> r1 = MeasurementRange.create(1000f, 2000f, SI.METRE);
        NumberRange<Float> r2 = MeasurementRange.create(1.5f, 3f, SI.KILOMETRE);
        assertEquals(Float.class, r1.getElementType());
        assertEquals(Float.class, r2.getElementType());
        assertEquals(MeasurementRange.create(1000f, 3000f, SI.METRE ),    r1.union    (r2));
        assertEquals(MeasurementRange.create(1f,    3f,    SI.KILOMETRE), r2.union    (r1));
        assertEquals(MeasurementRange.create(1500f, 2000f, SI.METRE ),    r1.intersect(r2));
        assertEquals(MeasurementRange.create(1.5f,  2f,    SI.KILOMETRE), r2.intersect(r1));
    }

    /**
     * Tests {@link MeasurementRange#toString()} method.
     */
    @Test
    public void testToString() {
        final MeasurementRange<Float> range = MeasurementRange.create(10f, 20f, SI.KILOMETRE);
        assertEquals("[10.0 … 20.0] km", range.toString());
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        NumberRange<Float> r1 = MeasurementRange.create(1000f, 2000f, SI.METRE);
        NumberRange<Float> r2 = MeasurementRange.create(1.5f, 3f, SI.KILOMETRE);
        assertNotSame(r1, assertSerializedEquals(r1));
        assertNotSame(r2, assertSerializedEquals(r2));
    }
}
