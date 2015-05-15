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
package org.apache.sis.metadata;

import org.opengis.util.InternationalString;
import org.opengis.metadata.ExtendedElementInformation;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Test the {@link SpecialCases} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(PropertyAccessorTest.class)
public final strictfp class SpecialCasesTest extends TestCase {
    /**
     * The accessor instance to be tested.
     */
    private final PropertyAccessor accessor;

    /**
     * An arbitrary bounding box to be used for testing purpose.
     */
    private DefaultGeographicBoundingBox box;

    /**
     * Creates a new test case.
     */
    public SpecialCasesTest() {
        accessor = new SpecialCases(HardCodedCitations.ISO_19115,
                GeographicBoundingBox.class, DefaultGeographicBoundingBox.class);
    }

    /**
     * Initializes the {@link #box} field.
     * Invoked only by the tests that need an actual metadata instance.
     */
    private void createBox() {
        box = new DefaultGeographicBoundingBox(-20, 30, -10, 40);
    }

    /**
     * Invokes {@link SpecialCases#type(int)} and ensure that the result is equals to the expected value.
     */
    private void assertTypeEquals(final String name, final Class<?> expected) {
        assertEquals(name, expected, accessor.type(accessor.indexOf(name, true), TypeValuePolicy.ELEMENT_TYPE));
    }

    /**
     * Invokes {@link SpecialCases#get(int, Object)} and ensure that the result is equals to the expected value.
     */
    private void assertPropertyEquals(final String name, final Object expected) {
        assertEquals(name, expected, accessor.get(accessor.indexOf(name, true), box));
    }

    /**
     * Invokes {@link SpecialCases#set(int, Object, Object, int)} in {@code RETURN_PREVIOUS} mode with the given
     * {@code newValue}, and ensures that the return value is equals to the given {@code oldValue}.
     */
    private void assertPreviousEquals(final String name, final Object oldValue, final Object newValue) {
        final Object value = accessor.set(accessor.indexOf(name, true), box, newValue, PropertyAccessor.RETURN_PREVIOUS);
        assertEquals(name, oldValue, value);
    }

    /**
     * Invokes {@link SpecialCases#set(int, Object, Object, int)} in {@code APPEND} mode with the given
     * {@code newValue}, and ensures that the return value is equals to the given {@code changed}.
     */
    private void assertAppendResultEquals(final String name, final Boolean changed, final Object newValue) {
        final Object value = accessor.set(accessor.indexOf(name, true), box, newValue, PropertyAccessor.APPEND);
        assertEquals(name, changed, value);
    }

    /**
     * Tests {@link SpecialCases#type(int, TypeValuePolicy)}.
     */
    @Test
    public void testType() {
        assertTypeEquals("westBoundLongitude", Longitude.class);
        assertTypeEquals("eastBoundLongitude", Longitude.class);
        assertTypeEquals("southBoundLatitude", Latitude.class);
        assertTypeEquals("northBoundLatitude", Latitude.class);
        assertTypeEquals("extentTypeCode",     Boolean.class);
    }

    /**
     * Tests {@link SpecialCases#get(int, Object)}.
     */
    @Test
    public void testGet() {
        createBox();
        assertPropertyEquals("westBoundLongitude", new Longitude(-20));
        assertPropertyEquals("eastBoundLongitude", new Longitude( 30));
        assertPropertyEquals("southBoundLatitude", new Latitude (-10));
        assertPropertyEquals("northBoundLatitude", new Latitude ( 40));
        assertPropertyEquals("extentTypeCode",     Boolean.TRUE      );
    }

    /**
     * Tests {@link SpecialCases#set(int, Object, Object, int)} in {@code RETURN_PREVIOUS} mode.
     */
    @Test
    @DependsOnMethod("testGet")
    public void testSet() {
        createBox();
        assertPreviousEquals("westBoundLongitude", new Longitude(-20), new Longitude(-15));
        assertPreviousEquals("eastBoundLongitude", new Longitude( 30), new Longitude( 25));
        assertPreviousEquals("southBoundLatitude", new Latitude (-10), new Latitude ( -5));
        assertPreviousEquals("northBoundLatitude", new Latitude ( 40), new Latitude ( 35));
        assertPreviousEquals("extentTypeCode",     Boolean.TRUE,       Boolean.FALSE);

        assertEquals("westBoundLongitude", -15, box.getWestBoundLongitude(), STRICT);
        assertEquals("eastBoundLongitude",  25, box.getEastBoundLongitude(), STRICT);
        assertEquals("southBoundLatitude",  -5, box.getSouthBoundLatitude(), STRICT);
        assertEquals("northBoundLatitude",  35, box.getNorthBoundLatitude(), STRICT);
        assertEquals("extentTypeCode", Boolean.FALSE, box.getInclusion());
    }

    /**
     * Tests {@link SpecialCases#set(int, Object, Object, int)} in {@code RETURN_PREVIOUS} mode
     * with {@link Double} values instead than {@link Longitude} or {@link Latitude}.
     */
    @Test
    @DependsOnMethod("testSet")
    public void testSetAsPrimitive() {
        createBox();
        assertPreviousEquals("westBoundLongitude", new Longitude(-20), -14.0);
        assertPreviousEquals("eastBoundLongitude", new Longitude( 30),  26  );
        assertPreviousEquals("southBoundLatitude", new Latitude (-10),  -7f );
        assertPreviousEquals("northBoundLatitude", new Latitude ( 40), (short) 33);

        assertEquals("westBoundLongitude", -14, box.getWestBoundLongitude(), STRICT);
        assertEquals("eastBoundLongitude",  26, box.getEastBoundLongitude(), STRICT);
        assertEquals("southBoundLatitude",  -7, box.getSouthBoundLatitude(), STRICT);
        assertEquals("northBoundLatitude",  33, box.getNorthBoundLatitude(), STRICT);
        assertEquals("extentTypeCode", Boolean.TRUE, box.getInclusion());
    }

    /**
     * Tests {@link SpecialCases#set(int, Object, Object, int)} in {@code APPEND} mode.
     */
    @Test
    @DependsOnMethod("testSet")
    public void testAppend() {
        createBox();
        assertAppendResultEquals("westBoundLongitude", null, new Longitude(-20));
        assertAppendResultEquals("eastBoundLongitude", null, new Longitude( 24));
        assertAppendResultEquals("southBoundLatitude", null, new Latitude ( -6));
        assertAppendResultEquals("northBoundLatitude", null, 40.0);
        assertAppendResultEquals("extentTypeCode", false, Boolean.TRUE);

        assertEquals("westBoundLongitude", -20, box.getWestBoundLongitude(), STRICT);
        assertEquals("eastBoundLongitude",  30, box.getEastBoundLongitude(), STRICT);
        assertEquals("southBoundLatitude", -10, box.getSouthBoundLatitude(), STRICT);
        assertEquals("northBoundLatitude",  40, box.getNorthBoundLatitude(), STRICT);
        assertEquals("extentTypeCode", Boolean.TRUE, box.getInclusion());
    }

    /**
     * Tests {@link SpecialCases#information(int)}.
     */
    @Test
    public void testPropertyInformation() {
        final ExtendedElementInformation info = accessor.information(accessor.indexOf("westBoundLongitude", true));
        final InternationalString domain = info.getDomainValue();
        assertInstanceOf("Expected numerical information about range.", NumberRange.class, domain);
        final NumberRange<?> range = (NumberRange) domain;
        assertEquals(-180, range.getMinDouble(), STRICT);
        assertEquals(+180, range.getMaxDouble(), STRICT);
    }
}
