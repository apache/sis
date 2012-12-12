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
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.content.ImagingCondition;
import org.opengis.metadata.identification.CharacterSet;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link CodeLists} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(TypesTest.class)
public final strictfp class CodeListsTest extends TestCase {
    /**
     * Tests the examples given in {@link CodeLists#getListName(CodeList)} javadoc.
     */
    @Test
    public void testGetListName() {
        assertEquals("CS_AxisDirection",        CodeLists.getListName(AxisDirection   .NORTH));
        assertEquals("MD_CharacterSetCode",     CodeLists.getListName(CharacterSet    .UTF_8));
        assertEquals("MD_ImagingConditionCode", CodeLists.getListName(ImagingCondition.BLURRED_IMAGE));
    }

    /**
     * Tests the examples given in {@link CodeLists#getCodeName(CodeList)} javadoc.
     */
    @Test
    public void testGetCodeName() {
        assertEquals("north",        CodeLists.getCodeName(AxisDirection   .NORTH));
        assertEquals("utf8",         CodeLists.getCodeName(CharacterSet    .UTF_8));
        assertEquals("blurredImage", CodeLists.getCodeName(ImagingCondition.BLURRED_IMAGE));
    }

    /**
     * Tests the examples given in {@link CodeLists#getCodeTitle(CodeList)} javadoc.
     */
    @Test
    public void testGetCodeTitle() {
        assertEquals("North",         CodeLists.getCodeTitle(AxisDirection   .NORTH));
        assertEquals("UTF-8",         CodeLists.getCodeTitle(CharacterSet    .UTF_8));
        assertEquals("Blurred image", CodeLists.getCodeTitle(ImagingCondition.BLURRED_IMAGE));
    }

    /**
     * Tests {@link CodeLists#getCodeTitle(CodeList, Locale)}.
     */
    @Test
    public void testGetLocalizedCodeTitle() {
        assertEquals("Download",       CodeLists.getCodeTitle(OnLineFunction.DOWNLOAD, Locale.ENGLISH));
        assertEquals("Téléchargement", CodeLists.getCodeTitle(OnLineFunction.DOWNLOAD, Locale.FRENCH));
    }

    /**
     * Tests the {@link CodeLists#getDescription(CodeList, Locale)} method.
     */
    @Test
    public void testGetDescription() {
        assertEquals("ISO/IEC 8859-1, Information technology - 8-bit single byte coded graphic character sets - Part 1 : Latin alphabet No.1.",
                CodeLists.getDescription(CharacterSet.ISO_8859_1, Locale.ENGLISH));
        assertEquals("ISO/IEC 8859-1, alphabet latin 1.",
                CodeLists.getDescription(CharacterSet.ISO_8859_1, Locale.FRENCH));
    }

    /**
     * Tests the {@link CodeLists#values(Class)} method.
     */
    @Test
    public void testValues() {
        final Set<OnLineFunction> expected = new HashSet<>(Arrays.asList(
                OnLineFunction.INFORMATION, OnLineFunction.SEARCH, OnLineFunction.ORDER,
                OnLineFunction.DOWNLOAD, OnLineFunction.OFFLINE_ACCESS));
        final OnLineFunction[] actual = CodeLists.values(OnLineFunction.class);
        assertTrue(expected.containsAll(Arrays.asList(actual)));
    }

    /**
     * Tests the {@link CodeLists#valueOf(Class, String, boolean)} method.
     */
    @Test
    public void testValueOf() {
        assertSame(ImagingCondition.SEMI_DARKNESS, CodeLists.valueOf(ImagingCondition.class, "SEMI_DARKNESS", false));
        assertSame(ImagingCondition.SEMI_DARKNESS, CodeLists.valueOf(ImagingCondition.class, "SEMIDARKNESS",  false));
        assertSame(ImagingCondition.SEMI_DARKNESS, CodeLists.valueOf(ImagingCondition.class, "semi darkness", false));
        assertSame(ImagingCondition.SEMI_DARKNESS, CodeLists.valueOf(ImagingCondition.class, "semi-darkness", false));
        assertNull(CodeLists.valueOf(ImagingCondition.class, "darkness", false));
    }
}
