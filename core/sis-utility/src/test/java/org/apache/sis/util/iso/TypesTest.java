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

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Locale;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Address;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.content.ImagingCondition;
import org.opengis.parameter.ParameterDirection;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests the {@link Types} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.5
 * @module
 */
public final strictfp class TypesTest extends TestCase {
    /**
     * Tests the {@link Types#toInternationalString(Map, String)} method.
     */
    @Test
    public void testToInternationalString() {
        testToInternationalString(new HashMap<>());
        testToInternationalString(new TreeMap<>());
    }

    /**
     * Implementation of {@link #testToInternationalString()} using the given map implementation.
     */
    private static void testToInternationalString(final Map<String,Object> properties) {
        assertNull(properties.put("name",       "Some name"));
        assertNull(properties.put("identifier", "Some identifier"));
        assertNull(properties.put("code",       "Some code"));
        assertNull(properties.put("codeSpace",  "Some code space"));
        assertNull(properties.put("authority",  "Some authority"));
        assertNull(properties.put("version",    "Some version"));
        assertNull(properties.put("remarks",    "Some remarks"));
        assertNull(Types.toInternationalString(properties, "dummy"));

        InternationalString i18n = Types.toInternationalString(properties, "remarks");
        assertInstanceOf("Single locale", SimpleInternationalString.class, i18n);
        assertEquals("Some remarks", i18n.toString());

        assertNull(properties.put("remarks_fr", "Une remarque"));
        i18n = Types.toInternationalString(properties, "remarks");
        assertInstanceOf("Two locales", DefaultInternationalString.class, i18n);
        assertEquals("Some remarks", i18n.toString(Locale.ROOT));
        assertEquals("Une remarque", i18n.toString(Locale.FRENCH));

        assertNotNull(properties.remove("remarks"));
        i18n = Types.toInternationalString(properties, "remarks");
        assertInstanceOf("Single locale", SimpleInternationalString.class, i18n);
        assertEquals("Une remarque", i18n.toString());
    }

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
     * Tests the {@link Types#getResources(String)} method.
     */
    @Test
    public void testGetResources() {
        assertEquals("org.opengis.metadata.Descriptions", Types.getResources("org.opengis.metadata.Identifier"));
        assertNull(Types.getResources("org.opengis.metadata2.Identifier"));
    }

    /**
     * Tests the {@link Types#getDescription(Class)} method.
     */
    @Test
    public void testGetDescription() {
        final InternationalString description = Types.getDescription(OnLineFunction.class);
        assertEquals("Function performed by the resource.",
                description.toString(Locale.ROOT));
        assertEquals("Function performed by the resource.",
                description.toString(Locale.ENGLISH));
        assertEquals("Fonctionnalité offerte par la ressource.",
                description.toString(Locale.FRENCH));
    }

    /**
     * Tests the {@link Types#getDescription(Class, String)} method.
     */
    @Test
    public void testGetPropertyDescription() {
        assertEquals("The city of the location.",
                Types.getDescription(Address.class, "city").toString(Locale.ROOT));
        assertEquals("Country of the physical address.",
                Types.getDescription(Address.class, "country").toString(Locale.ENGLISH));
    }

    /**
     * Tests the {@link Types#getDescription(Enumerated)} method.
     */
    @Test
    public void testGetCodeDescription() {
        final InternationalString description = Types.getDescription(OnLineFunction.DOWNLOAD);
        assertEquals("Online instructions for transferring data from one storage device or system to another.",
                description.toString(Locale.ROOT));
        assertEquals("Online instructions for transferring data from one storage device or system to another.",
                description.toString(Locale.ENGLISH));
        assertEquals("Transfert de la ressource d'un système à un autre.",
                description.toString(Locale.FRENCH));
    }

    /**
     * Tests the examples given in {@link Types#getListName(Enumerated)} javadoc.
     */
    @Test
    public void testGetListName() {
        assertEquals("SV_ParameterDirection",   Types.getListName(ParameterDirection.IN_OUT));
        assertEquals("CS_AxisDirection",        Types.getListName(AxisDirection     .NORTH));
        assertEquals("CI_OnLineFunctionCode",   Types.getListName(OnLineFunction    .DOWNLOAD));
        assertEquals("MD_ImagingConditionCode", Types.getListName(ImagingCondition  .BLURRED_IMAGE));
    }

    /**
     * Tests the examples given in {@link Types#getCodeName(Enumerated)} javadoc.
     */
    @Test
    public void testGetCodeName() {
        assertEquals("in/out",       Types.getCodeName(ParameterDirection.IN_OUT));
        assertEquals("north",        Types.getCodeName(AxisDirection     .NORTH));
        assertEquals("download",     Types.getCodeName(OnLineFunction    .DOWNLOAD));
        assertEquals("blurredImage", Types.getCodeName(ImagingCondition  .BLURRED_IMAGE));
    }

    /**
     * Tests the examples given in {@link Types#getCodeLabel(Enumerated)} javadoc.
     */
    @Test
    public void testGetCodeLabel() {
        assertEquals("North",         Types.getCodeLabel(AxisDirection   .NORTH));
        assertEquals("Download",      Types.getCodeLabel(OnLineFunction  .DOWNLOAD));
        assertEquals("Blurred image", Types.getCodeLabel(ImagingCondition.BLURRED_IMAGE));
    }

    /**
     * Tests {@link Types#getCodeTitle(Enumerated)}.
     */
    @Test
    public void testGetCodeTitle() {
        assertEquals("Download",       Types.getCodeTitle(OnLineFunction.DOWNLOAD).toString(Locale.ROOT));
        assertEquals("Download",       Types.getCodeTitle(OnLineFunction.DOWNLOAD).toString(Locale.ENGLISH));
        assertEquals("Téléchargement", Types.getCodeTitle(OnLineFunction.DOWNLOAD).toString(Locale.FRENCH));
    }

    /**
     * Tests the {@link Types#getCodeValues(Class)} method.
     */
    @Test
    public void testGetCodeValues() {
        final OnLineFunction[] actual = Types.getCodeValues(OnLineFunction.class);
        assertTrue(Arrays.asList(actual).containsAll(Arrays.asList(
                OnLineFunction.INFORMATION, OnLineFunction.SEARCH, OnLineFunction.ORDER,
                OnLineFunction.DOWNLOAD, OnLineFunction.OFFLINE_ACCESS)));
    }
}
