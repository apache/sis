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
import javax.measure.unit.NonSI;
import javax.measure.converter.ConversionException;
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link MeasurementRange} class.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.3
 * @version 0.4
 * @module
 */
@DependsOn(NumberRangeTest.class)
public final strictfp class MeasurementRangeTest extends TestCase {
    /**
     * Tests unit conversions by the {@link MeasurementRange#convertTo(Unit)} method.
     *
     * @throws ConversionException Should not happen.
     */
    @Test
    public void testConvertTo() throws ConversionException {
        final MeasurementRange<Float> range = MeasurementRange.create(1000f, true, 2000f, true, SI.METRE);
        assertSame(range, range.convertTo(SI.METRE));
        assertEquals(MeasurementRange.create(1f, true, 2f, true, SI.KILOMETRE), range.convertTo(SI.KILOMETRE));
    }

    /**
     * Tests union and intersection involving a unit conversion.
     */
    @Test
    public void testAutoConversions() {
        final MeasurementRange<Float> r1 = MeasurementRange.create(1000f, true, 2000f, true, SI.METRE);
        final MeasurementRange<Float> r2 = MeasurementRange.create(1.5f, true, 3f, true, SI.KILOMETRE);
        assertEquals(Float.class, r1.getElementType());
        assertEquals(Float.class, r2.getElementType());
        assertEquals(MeasurementRange.create(1000f, true, 3000f, true, SI.METRE ),    r1.union    (r2));
        assertEquals(MeasurementRange.create(   1f, true,    3f, true, SI.KILOMETRE), r2.union    (r1));
        assertEquals(MeasurementRange.create(1500f, true, 2000f, true, SI.METRE ),    r1.intersect(r2));
        assertEquals(MeasurementRange.create( 1.5f, true,    2f, true, SI.KILOMETRE), r2.intersect(r1));
    }

    /**
     * Same tests than {@link #testAutoConversions()} but using the {@code *Any} methods.
     */
    @Test
    public void testAutoConversionsOfAny() {
        final MeasurementRange<?> r1 = MeasurementRange.create(1000f, true, 2000f, true, SI.METRE);
        final MeasurementRange<?> r2 = MeasurementRange.create(1.5f, true, 3f, true, SI.KILOMETRE);
        assertEquals(MeasurementRange.create(1000f, true, 3000f, true, SI.METRE ),    r1.unionAny    (r2));
        assertEquals(MeasurementRange.create(   1f, true,    3f, true, SI.KILOMETRE), r2.unionAny    (r1));
        assertEquals(MeasurementRange.create(1500f, true, 2000f, true, SI.METRE ),    r1.intersectAny(r2));
        assertEquals(MeasurementRange.create( 1.5f, true,    2f, true, SI.KILOMETRE), r2.intersectAny(r1));
    }

    /**
     * Tests {@link MeasurementRange#toString()} method.
     */
    @Test
    public void testToString() {
        MeasurementRange<Float> range = MeasurementRange.create(10f, true, 20f, true, SI.KILOMETRE);
        assertEquals("[10.0 … 20.0] km", range.toString());
        range = MeasurementRange.create(10f, true, 20f, true, NonSI.DEGREE_ANGLE);
        assertEquals("[10.0 … 20.0]°", range.toString());
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        NumberRange<Float> r1 = MeasurementRange.create(1000f, true, 2000f, true, SI.METRE);
        NumberRange<Float> r2 = MeasurementRange.create(1.5f, true, 3f, true, SI.KILOMETRE);
        assertNotSame(r1, assertSerializedEquals(r1));
        assertNotSame(r2, assertSerializedEquals(r2));
    }
}
