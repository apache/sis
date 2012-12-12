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
package org.apache.sis.util.iso;

import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.content.ImagingCondition;
import org.opengis.metadata.identification.CharacterSet;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Types} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
public final strictfp class TypesTest extends TestCase {
    /**
     * Tests the {@link Types#getStandardName(Class)} method.
     */
    @Test
    public void testGetStandardName() {
        assertEquals("CI_Citation",      Types.getStandardName(Citation     .class));
        assertEquals("CD_Datum",         Types.getStandardName(Datum        .class));
        assertEquals("CS_AxisDirection", Types.getStandardName(AxisDirection.class));
    }

    /**
     * Tests the {@link Types#forStandardName(String)} method.
     */
    @Test
    public void testForStandardName() {
        assertEquals(Citation     .class, Types.forStandardName("CI_Citation"));
        assertEquals(Datum        .class, Types.forStandardName("CD_Datum"));
        assertEquals(Citation     .class, Types.forStandardName("CI_Citation")); // Value should be cached.
        assertEquals(AxisDirection.class, Types.forStandardName("CS_AxisDirection"));
        assertNull  (                     Types.forStandardName("MD_Dummy"));
    }

    /**
     * Tests the {@link Types#forCodeName(Class, String, boolean)} method.
     */
    @Test
    public void testForCodeName() {
        assertSame(ImagingCondition.SEMI_DARKNESS, Types.forCodeName(ImagingCondition.class, "SEMI_DARKNESS", false));
        assertSame(ImagingCondition.SEMI_DARKNESS, Types.forCodeName(ImagingCondition.class, "SEMIDARKNESS",  false));
        assertSame(ImagingCondition.SEMI_DARKNESS, Types.forCodeName(ImagingCondition.class, "semi darkness", false));
        assertSame(ImagingCondition.SEMI_DARKNESS, Types.forCodeName(ImagingCondition.class, "semi-darkness", false));
        assertNull(Types.forCodeName(ImagingCondition.class, "darkness", false));
    }

    /**
     * Tests the {@link Types#getDescription(Class, Locale)} method.
     */
    @Test
    public void testGetDescription() {
        assertEquals("Name of the character coding standard used in the resource.",
                Types.getDescription(CharacterSet.class, Locale.ENGLISH));
        assertEquals("Jeu de caractères.",
                Types.getDescription(CharacterSet.class, Locale.FRENCH));
    }

    /**
     * Tests the {@link Types#getDescription(CodeList, Locale)} method.
     */
    @Test
    public void testGetCodeDescription() {
        assertEquals("ISO/IEC 8859-1, Information technology - 8-bit single byte coded graphic character sets - Part 1 : Latin alphabet No.1.",
                Types.getDescription(CharacterSet.ISO_8859_1, Locale.ENGLISH));
        assertEquals("ISO/IEC 8859-1, alphabet latin 1.",
                Types.getDescription(CharacterSet.ISO_8859_1, Locale.FRENCH));
    }

    /**
     * Tests the examples given in {@link Types#getListName(CodeList)} javadoc.
     */
    @Test
    public void testGetListName() {
        assertEquals("CS_AxisDirection",        Types.getListName(AxisDirection   .NORTH));
        assertEquals("MD_CharacterSetCode",     Types.getListName(CharacterSet    .UTF_8));
        assertEquals("MD_ImagingConditionCode", Types.getListName(ImagingCondition.BLURRED_IMAGE));
    }

    /**
     * Tests the examples given in {@link Types#getCodeName(CodeList)} javadoc.
     */
    @Test
    public void testGetCodeName() {
        assertEquals("north",        Types.getCodeName(AxisDirection   .NORTH));
        assertEquals("utf8",         Types.getCodeName(CharacterSet    .UTF_8));
        assertEquals("blurredImage", Types.getCodeName(ImagingCondition.BLURRED_IMAGE));
    }

    /**
     * Tests the examples given in {@link Types#getCodeTitle(CodeList)} javadoc.
     */
    @Test
    public void testGetCodeTitle() {
        assertEquals("North",         Types.getCodeTitle(AxisDirection   .NORTH));
        assertEquals("UTF-8",         Types.getCodeTitle(CharacterSet    .UTF_8));
        assertEquals("Blurred image", Types.getCodeTitle(ImagingCondition.BLURRED_IMAGE));
    }

    /**
     * Tests {@link Types#getCodeTitle(CodeList, Locale)}.
     */
    @Test
    public void testGetLocalizedCodeTitle() {
        assertEquals("Download",       Types.getCodeTitle(OnLineFunction.DOWNLOAD, Locale.ENGLISH));
        assertEquals("Téléchargement", Types.getCodeTitle(OnLineFunction.DOWNLOAD, Locale.FRENCH));
    }

    /**
     * Tests the {@link Types#getCodeValues(Class)} method.
     */
    @Test
    public void testGetCodeValues() {
        final Set<OnLineFunction> expected = new HashSet<>(Arrays.asList(
                OnLineFunction.INFORMATION, OnLineFunction.SEARCH, OnLineFunction.ORDER,
                OnLineFunction.DOWNLOAD, OnLineFunction.OFFLINE_ACCESS));
        final OnLineFunction[] actual = Types.getCodeValues(OnLineFunction.class);
        assertTrue(expected.containsAll(Arrays.asList(actual)));
    }
}
