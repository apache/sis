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
package org.apache.sis.internal.converter;

import java.util.Locale;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.lang.annotation.ElementType;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.OnLineFunction;
import org.apache.sis.measure.Angle;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.StandardCharsets;


/**
 * Tests the various {@link StringConverter} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(org.apache.sis.measure.AngleTest.class)
public final strictfp class StringConverterTest extends TestCase {
    /**
     * Asserts that conversion of the given {@code source} value produces
     * the given {@code target} value, and tests the inverse conversion.
     */
    private static <T> void runInvertibleConversion(final ObjectConverter<String,T> c,
            final String source, final T target) throws UnconvertibleObjectException
    {
        assertEquals("Forward conversion.", target, c.apply(source));
        assertEquals("Inverse conversion.", source, c.inverse().apply(target));
        assertSame("Inconsistent inverse.", c, c.inverse().inverse());
        assertTrue("Invertible converters shall declare this capability.",
                c.properties().contains(FunctionProperty.INVERTIBLE));
    }

    /**
     * Tries to convert an unconvertible value.
     */
    private static void tryUnconvertibleValue(final ObjectConverter<String,?> c) {
        try {
            c.apply("他の言葉");
            fail("Should not accept a text.");
        } catch (UnconvertibleObjectException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("他の言葉"));
        }
    }

    /**
     * Tests conversions to {@link Number}.
     */
    @Test
    public void testNumber() {
        final ObjectConverter<String,Number> c = new StringConverter.Number();
        runInvertibleConversion(c,    "-4", Byte   .valueOf((byte)   -4));
        runInvertibleConversion(c,   "128", Short  .valueOf((short) 128));
        runInvertibleConversion(c, "40000", Integer.valueOf(      40000));
        runInvertibleConversion(c,   "4.5", Float  .valueOf(       4.5f));
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link Double}.
     */
    @Test
    public void testDouble() {
        final ObjectConverter<String,Double> c = new StringConverter.Double();
        runInvertibleConversion(c, "4.5", Double.valueOf(4.5));
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link Float}.
     */
    @Test
    public void testFloat() {
        final ObjectConverter<String,Float> c = new StringConverter.Float();
        runInvertibleConversion(c, "4.5", Float.valueOf(4.5f));
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link Long}.
     */
    @Test
    public void testLong() {
        final ObjectConverter<String,Long> c = new StringConverter.Long();
        runInvertibleConversion(c, "45000", Long.valueOf(45000));
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link Integer}.
     */
    @Test
    public void testInteger() {
        final ObjectConverter<String,Integer> c = new StringConverter.Integer();
        runInvertibleConversion(c, "45000", Integer.valueOf(45000));
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link Short}.
     */
    @Test
    public void testShort() {
        final ObjectConverter<String,Short> c = new StringConverter.Short();
        runInvertibleConversion(c, "4500", Short.valueOf((short) 4500));
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link Byte}.
     */
    @Test
    public void testByte() {
        final ObjectConverter<String,Byte> c = new StringConverter.Byte();
        runInvertibleConversion(c, "45", Byte.valueOf((byte) 45));
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link BigDecimal}.
     */
    @Test
    public void testBigDecimal() {
        final ObjectConverter<String,BigDecimal> c = new StringConverter.BigDecimal();
        runInvertibleConversion(c, "45000.5", BigDecimal.valueOf(45000.5));
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link BigInteger}.
     */
    @Test
    public void testBigInteger() {
        final ObjectConverter<String,BigInteger> c = new StringConverter.BigInteger();
        runInvertibleConversion(c, "45000", BigInteger.valueOf(45000));
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link Angle}.
     */
    @Test
    public void testAngle() {
        final ObjectConverter<String,Angle> c = new StringConverter.Angle();
        runInvertibleConversion(c, "42°30′",       new Angle(42.5));
        runInvertibleConversion(c, "42°30′56.25″", new Angle(42.515625));
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to boolean values.
     */
    @Test
    public void testBoolean() {
        final ObjectConverter<String,Boolean> c = new StringConverter.Boolean();
        runInvertibleConversion(c, "true",  Boolean.TRUE);
        runInvertibleConversion(c, "false", Boolean.FALSE);
        assertEquals(Boolean.TRUE,  c.apply("yes"));
        assertEquals(Boolean.FALSE, c.apply("no"));
        assertEquals(Boolean.TRUE,  c.apply("ON"));  // Test upper-case.
        assertEquals(Boolean.FALSE, c.apply("OFF"));
        assertEquals(Boolean.TRUE,  c.apply("1"));
        assertEquals(Boolean.FALSE, c.apply("0"));
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link Locale}.
     */
    @Test
    public void testLocale() {
        final ObjectConverter<String,Locale> c = new StringConverter.Locale();
        runInvertibleConversion(c, "fr_CA", Locale.CANADA_FRENCH);
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link Charset}.
     */
    @Test
    public void testCharset() {
        final ObjectConverter<String,Charset> c = new StringConverter.Charset();
        runInvertibleConversion(c, "UTF-8", StandardCharsets.UTF_8);
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link InternationalString}.
     */
    @Test
    public void testInternationalString() {
        final ObjectConverter<String,InternationalString> c = new StringConverter.InternationalString();
        runInvertibleConversion(c, "Some sentence", new SimpleInternationalString("Some sentence"));
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link File}.
     */
    @Test
    public void testFile() {
        final ObjectConverter<String,File> c = new StringConverter.File();
        final String path = "home/user/index.txt".replace('/', File.separatorChar);
        runInvertibleConversion(c, path, new File(path));
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link URI}.
     *
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testURI() throws URISyntaxException {
        final ObjectConverter<String,URI> c = new StringConverter.URI();
        runInvertibleConversion(c, "file:/home/user/index.txt", new URI("file:/home/user/index.txt"));
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link URI}.
     *
     * @throws MalformedURLException Should never happen.
     */
    @Test
    public void testURL() throws MalformedURLException {
        final ObjectConverter<String,URL> c = new StringConverter.URL();
        runInvertibleConversion(c, "file:/home/user/index.txt", new URL("file:/home/user/index.txt"));
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link Unit}.
     */
    @Test
    public void testUnit() {
        final ObjectConverter<String,Unit<?>> c = new StringConverter.Unit();
        runInvertibleConversion(c, "km", SI.KILOMETRE);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link org.opengis.util.CodeList}.
     */
    @Test
    public void testCodeList() {
        final ObjectConverter<String, OnLineFunction> c = new StringConverter.CodeList<OnLineFunction>(OnLineFunction.class);
        runInvertibleConversion(c, "OFFLINE_ACCESS", OnLineFunction.OFFLINE_ACCESS);
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }

    /**
     * Tests conversions to {@link java.lang.Enum}.
     *
     * @since 0.5
     */
    @Test
    public void testEnum() {
        final ObjectConverter<String, ElementType> c = new StringConverter.Enum<ElementType>(ElementType.class);
        runInvertibleConversion(c, "PACKAGE", ElementType.PACKAGE);
        tryUnconvertibleValue(c);
        assertSerializedEquals(c);
    }
}
