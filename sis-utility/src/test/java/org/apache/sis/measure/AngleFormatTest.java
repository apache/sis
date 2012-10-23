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

import java.util.Locale;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.math.MathFunctionsTest;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.*;


/**
 * Tests parsing and formatting done by the {@link AngleFormat} class.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.0)
 * @version 0.3
 * @module
 */
@DependsOn(MathFunctionsTest.class)
public final strictfp class AngleFormatTest extends TestCase {
    /**
     * Tests using {@link Locale#CANADA}.
     */
    @Test
    public void testCanadaLocale() {
        final AngleFormat f = new AngleFormat("DD.ddd°", Locale.CANADA);
        assertEquals( "20.000°",  formatAndParse(f, new Angle   ( 20.000)));
        assertEquals( "20.749°",  formatAndParse(f, new Angle   ( 20.749)));
        assertEquals("-12.247°",  formatAndParse(f, new Angle   (-12.247)));
        assertEquals( "13.214°N", formatAndParse(f, new Latitude( 13.214)));
        assertEquals( "12.782°S", formatAndParse(f, new Latitude(-12.782)));
    }

    /**
     * Tests using {@link Locale#FRANCE}.
     */
    @Test
    public void testFranceLocale() {
        final AngleFormat f = new AngleFormat("DD.ddd°", Locale.FRANCE);
        assertEquals("19,457°E", formatAndParse(f, new Longitude( 19.457)));
        assertEquals("78,124°S", formatAndParse(f, new Latitude (-78.124)));
    }

    /**
     * Tests with no decimal separator.
     */
    @Test
    public void testNoSeparator() {
        final AngleFormat f = new AngleFormat("DDddd", Locale.CANADA);
        assertEquals("19457E", formatAndParse(f, new Longitude( 19.457)));
        assertEquals("78124S", formatAndParse(f, new Latitude (-78.124)));
    }

    /**
     * Tests with the degree separator.
     */
    @Test
    public void testDegreeSeparator() {
        final AngleFormat f = new AngleFormat("DD°MM.m", Locale.CANADA);
        assertEquals( "12°30.0", formatAndParse(f, new Angle( 12.50)));
        assertEquals("-10°15.0", formatAndParse(f, new Angle(-10.25)));
    }
}
