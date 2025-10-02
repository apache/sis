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
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Locale;
import java.util.function.Function;
import java.lang.annotation.ElementType;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Address;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.content.ImagingCondition;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.DefaultInternationalString;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.constraint.Restriction;
import org.opengis.parameter.ParameterDirection;


/**
 * Tests the {@link Types} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class TypesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public TypesTest() {
    }

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
        assertInstanceOf(SimpleInternationalString.class, i18n);
        assertEquals("Some remarks", i18n.toString());

        assertNull(properties.put("remarks_fr", "Une remarque"));
        i18n = Types.toInternationalString(properties, "remarks");
        assertInstanceOf(DefaultInternationalString.class, i18n);
        assertEquals("Some remarks", i18n.toString(Locale.ROOT));
        assertEquals("Une remarque", i18n.toString(Locale.FRENCH));

        assertNotNull(properties.remove("remarks"));
        i18n = Types.toInternationalString(properties, "remarks");
        assertInstanceOf(SimpleInternationalString.class, i18n);
        assertEquals("Une remarque", i18n.toString());
    }

    /**
     * Tests the {@link Types#getStandardName(Class)} method.
     */
    @Test
    public void testGetStandardName() {
        assertEquals("CI_Citation",   Types.getStandardName(Citation     .class));
        assertEquals("Datum",         Types.getStandardName(Datum        .class));
        assertEquals("AxisDirection", Types.getStandardName(AxisDirection.class));
    }

    /**
     * Tests the {@link Types#forStandardName(String)} method.
     */
    @Test
    public void testForStandardName() {
        assertEquals(Citation     .class, Types.forStandardName("CI_Citation"));
        assertEquals(Datum        .class, Types.forStandardName("Datum"));
        assertEquals(Citation     .class, Types.forStandardName("CI_Citation"));            // Value should be cached.
        assertEquals(Citation     .class, Types.forStandardName("Citation"));
        assertEquals(AxisDirection.class, Types.forStandardName("AxisDirection"));
        assertNull  (                     Types.forStandardName("MD_Dummy"));
    }

    /**
     * Tests the {@link Types#forEnumName(Class, String)} method with an enumeration from the JDK.
     * Such enumerations do not implement the {@link org.opengis.util.ControlledVocabulary} interface.
     */
    @Test
    public void testForStandardEnumName() {
        assertSame(ElementType.LOCAL_VARIABLE, Types.forEnumName(ElementType.class, "LOCAL_VARIABLE"));
        assertSame(ElementType.LOCAL_VARIABLE, Types.forEnumName(ElementType.class, "LOCALVARIABLE"));
        assertSame(ElementType.LOCAL_VARIABLE, Types.forEnumName(ElementType.class, "local variable"));
        assertSame(ElementType.LOCAL_VARIABLE, Types.forEnumName(ElementType.class, "local-variable"));
        assertNull(Types.forEnumName(ElementType.class, "variable"));
    }

    /**
     * Tests the {@link Types#forEnumName(Class, String)} method with an enumeration from GeoAPI.
     * Such enumerations implement the {@link org.opengis.util.ControlledVocabulary} interface.
     */
    @Test
    public void testForGeoapiEnumName() {
        assertSame(ParameterDirection.IN_OUT, Types.forEnumName(ParameterDirection.class, "IN_OUT"));
        assertSame(ParameterDirection.IN_OUT, Types.forEnumName(ParameterDirection.class, "INOUT"));
        assertSame(ParameterDirection.IN_OUT, Types.forEnumName(ParameterDirection.class, "in out"));
        assertSame(ParameterDirection.IN_OUT, Types.forEnumName(ParameterDirection.class, "in/out"));
        assertNull(Types.forEnumName(ParameterDirection.class, "out/in"));
    }

    /**
     * Tests the {@link Types#forCodeName(Class, String, Function)} method.
     */
    @Test
    public void testForCodeName() {
        assertSame(ImagingCondition.SEMI_DARKNESS, Types.forCodeName(ImagingCondition.class, "SEMI_DARKNESS", null));
        assertSame(ImagingCondition.SEMI_DARKNESS, Types.forCodeName(ImagingCondition.class, "SEMIDARKNESS",  null));
        assertSame(ImagingCondition.SEMI_DARKNESS, Types.forCodeName(ImagingCondition.class, "semi darkness", null));
        assertSame(ImagingCondition.SEMI_DARKNESS, Types.forCodeName(ImagingCondition.class, "semi-darkness", null));
        assertNull(Types.forCodeName(ImagingCondition.class, "darkness", null));

        assertSame(Restriction.LICENCE, Types.forCodeName(Restriction.class, "licence", null));
        assertSame(Restriction.LICENCE, Types.forCodeName(Restriction.class, "license", null));
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
     * Tests the {@link Types#getDescription(ControlledVocabulary)} method.
     */
    @Test
    public void testGetCodeDescription() {
        final InternationalString description = Types.getDescription(OnLineFunction.DOWNLOAD);
        assertEquals("Online instructions for transferring data from one storage device or system to another.",
                description.toString(Locale.ROOT));
        assertEquals("Online instructions for transferring data from one storage device or system to another.",
                description.toString(Locale.ENGLISH));
        assertEquals("Transfert de la ressource d’un système à un autre.",
                description.toString(Locale.FRENCH));
    }

    /**
     * Tests the examples given in {@link Types#getListName(ControlledVocabulary)} javadoc.
     */
    @Test
    public void testGetListName() {
        assertEquals("SV_ParameterDirection",   Types.getListName(ParameterDirection.IN_OUT));
        assertEquals("AxisDirection",           Types.getListName(AxisDirection     .NORTH));
        assertEquals("CI_OnLineFunctionCode",   Types.getListName(OnLineFunction    .DOWNLOAD));
        assertEquals("MD_ImagingConditionCode", Types.getListName(ImagingCondition  .BLURRED_IMAGE));
    }

    /**
     * Tests the examples given in {@link Types#getCodeName(ControlledVocabulary)} javadoc.
     */
    @Test
    public void testGetCodeName() {
        assertEquals("in/out",       Types.getCodeName(ParameterDirection.IN_OUT));
        assertEquals("north",        Types.getCodeName(AxisDirection     .NORTH));
        assertEquals("download",     Types.getCodeName(OnLineFunction    .DOWNLOAD));
        assertEquals("blurredImage", Types.getCodeName(ImagingCondition  .BLURRED_IMAGE));
    }

    /**
     * Tests the examples given in {@link Types#getCodeLabel(ControlledVocabulary)} javadoc.
     */
    @Test
    public void testGetCodeLabel() {
        assertEquals("North",         Types.getCodeLabel(AxisDirection   .NORTH));
        assertEquals("Download",      Types.getCodeLabel(OnLineFunction  .DOWNLOAD));
        assertEquals("Blurred image", Types.getCodeLabel(ImagingCondition.BLURRED_IMAGE));
    }

    /**
     * Tests {@link Types#getCodeTitle(ControlledVocabulary)}.
     * Also opportunistically tests {@link Types#forCodeTitle(CharSequence)}.
     */
    @Test
    public void testGetCodeTitle() {
        final InternationalString title = Types.getCodeTitle(OnLineFunction.DOWNLOAD);
        assertSame(OnLineFunction.DOWNLOAD, Types.forCodeTitle(title));
        assertEquals("Download",       title.toString(Locale.ROOT));
        assertEquals("Download",       title.toString(Locale.ENGLISH));
        assertEquals("Téléchargement", title.toString(Locale.FRENCH));
    }
}
