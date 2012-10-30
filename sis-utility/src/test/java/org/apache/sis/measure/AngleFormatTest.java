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
import org.apache.sis.test.DependsOnMethod;
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
@DependsOn(org.apache.sis.math.MathFunctionsTest.class)
public final strictfp class AngleFormatTest extends TestCase {
    /**
     * Tests a pattern with illegal usage of D, M and S symbols.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalPattern() {
        final AngleFormat f = new AngleFormat(Locale.CANADA);
        f.applyPattern("DD°SS′MM″");
    }

    /**
     * Tests an illegal pattern with illegal symbols for the fraction part.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFractionPattern() {
        final AngleFormat f = new AngleFormat(Locale.CANADA);
        f.applyPattern("DD°MM′SS.m″");
    }

    /**
     * Tests using {@link Locale#CANADA}.
     */
    @Test
    public void testCanadaLocale() {
        final AngleFormat f = new AngleFormat("DD.ddd°", Locale.CANADA);
        assertEquals(3, f.getMinimumFractionDigits());
        assertEquals(3, f.getMaximumFractionDigits());
        assertEquals( "DD.ddd°",  f.toPattern());
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
        assertEquals(3, f.getMinimumFractionDigits());
        assertEquals(3, f.getMaximumFractionDigits());
        assertEquals( "DD.ddd°", f.toPattern());
        assertEquals("19,457°E", formatAndParse(f, new Longitude( 19.457)));
        assertEquals("78,124°S", formatAndParse(f, new Latitude (-78.124)));
    }

    /**
     * Tests with no decimal separator.
     */
    @Test
    public void testNoSeparator() {
        final AngleFormat f = new AngleFormat("DDddd", Locale.CANADA);
        assertEquals(3, f.getMinimumFractionDigits());
        assertEquals(3, f.getMaximumFractionDigits());
        assertEquals("DDddd",  f.toPattern());
        assertEquals("19457E", formatAndParse(f, new Longitude( 19.457)));
        assertEquals("78124S", formatAndParse(f, new Latitude (-78.124)));
    }

    /**
     * Tests with a minute fields.
     */
    @Test
    @DependsOnMethod("testCanadaLocale")
    public void testDegreeMinutes() {
        final AngleFormat f = new AngleFormat("DD°MM.m", Locale.CANADA);
        assertEquals(1, f.getMinimumFractionDigits());
        assertEquals(1, f.getMaximumFractionDigits());
        assertEquals( "DD°MM.m", f.toPattern());
        assertEquals( "12°30.0", formatAndParse(f, new Angle( 12.50)));
        assertEquals("-10°15.0", formatAndParse(f, new Angle(-10.25)));
    }

    /**
     * Tests with a seconds fields.
     */
    @Test
    @DependsOnMethod("testDegreeMinutes")
    public void testDegreeMinutesSeconds() {
        final AngleFormat f = new AngleFormat("DD°MM′SS.sss″", Locale.CANADA);
        assertEquals(3, f.getMinimumFractionDigits());
        assertEquals(3, f.getMaximumFractionDigits());
        assertEquals( "DD°MM′SS.sss″", f.toPattern());
        assertEquals( "12°30′56.250″", formatAndParse(f, new Angle( 12.515625)));
        assertEquals("-12°30′56.250″", formatAndParse(f, new Angle(-12.515625)));
    }

    /**
     * Tests with optional digits.
     */
    @Test
    @DependsOnMethod("testDegreeMinutesSeconds")
    public void testOptionalFractionDigits() {
        final AngleFormat f = new AngleFormat("DD°MM′SS.s##″", Locale.CANADA);
        assertEquals(1, f.getMinimumFractionDigits());
        assertEquals(3, f.getMaximumFractionDigits());
        assertEquals( "DD°MM′SS.s##″", f.toPattern());
        assertEquals( "12°30′56.25″",  formatAndParse(f, new Angle( 12.515625)));
        assertEquals("-12°30′56.25″",  formatAndParse(f, new Angle(-12.515625)));
        assertEquals( "12°31′52.5″",   formatAndParse(f, new Angle( 12.53125 )));
        assertEquals("-12°31′52.5″",   formatAndParse(f, new Angle(-12.53125 )));
        assertEquals( "12°33′45.0″",   formatAndParse(f, new Angle( 12.5625  )));
        assertEquals("-12°33′45.0″",   formatAndParse(f, new Angle(-12.5625  )));

        f.applyPattern("DD°MM′SS.###″");
        assertEquals(0, f.getMinimumFractionDigits());
        assertEquals(3, f.getMaximumFractionDigits());
        assertEquals( "DD°MM′SS.###″", f.toPattern());
        assertEquals( "12°33′45″", formatAndParse(f, new Angle( 12.5625)));
        assertEquals("-12°33′45″", formatAndParse(f, new Angle(-12.5625)));
    }

    /**
     * Tests the {@link AngleFormat#setMaximumWidth(int)} method.
     */
    @Test
    @DependsOnMethod("testOptionalFractionDigits")
    public void testSetMaximumWidth() {
        final AngleFormat f = new AngleFormat(Locale.CANADA);
        assertEquals("D°MM′SS.################″", f.toPattern());

        f.setMaximumWidth(12);
        assertEquals("D°MM′SS.###″", f.toPattern());
        assertEquals("8°07′24.442″", f.format(new Angle( 8.123456)));
        assertEquals("20°07′24.44″", f.format(new Angle(20.123456)));

        f.setMaximumWidth(10);
        assertEquals("D°MM′SS.#″", f.toPattern());
        assertEquals("8°07′24.4″", f.format(new Angle( 8.123456)));
        assertEquals("20°07′24″",  f.format(new Angle(20.123456)));

        f.setMaximumWidth(9);
        assertEquals("D°MM′SS″", f.toPattern());
        f.setMaximumWidth(8);
        assertEquals("D°MM′SS″", f.toPattern());

        // Test the drop of seconds field.
        f.setMaximumFractionDigits(6);
        f.setMaximumWidth(7);
        assertEquals("D°MM.#′",  f.toPattern());
        assertEquals("8°07.4′",  f.format(new Angle( 8.123456)));
        assertEquals("20°07.4′", f.format(new Angle(20.123456)));

        f.setMaximumWidth(6);
        assertEquals("D°MM′",  f.toPattern());
        assertEquals("8°07′",  f.format(new Angle( 8.123456)));
        assertEquals("20°07′", f.format(new Angle(20.123456)));

        // Test the drop of minutes field.
        f.setMaximumFractionDigits(6);
        f.setMaximumWidth(4);
        assertEquals("D.#°", f.toPattern());
        assertEquals("8.1°", f.format(new Angle( 8.123456)));
        assertEquals("20°",  f.format(new Angle(20.123456)));

        f.setMaximumWidth(3);
        assertEquals("D°",  f.toPattern());
        assertEquals("8°",  f.format(new Angle( 8.123456)));
        assertEquals("20°", f.format(new Angle(20.123456)));
    }
}
