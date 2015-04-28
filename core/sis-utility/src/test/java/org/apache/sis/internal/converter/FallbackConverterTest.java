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

import java.util.EnumSet;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link FallbackConverter}. This test creates {@code FallbackConverter} instances directly.
 * It shall not use {@link ConverterRegistry}, directly or indirectly, in order to keep the tests
 * isolated. Note that {@link SystemConverter#inverse()} and serialization indirectly use
 * {@code ConverterRegistry}, so those tests shall be avoided.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn({StringConverterTest.class,
    org.apache.sis.util.collection.TreeTableFormatTest.class})
public final strictfp class FallbackConverterTest extends TestCase {
    /**
     * Conversions that are expected to be supported.
     * Greater values imply all conversions identified by lower values.
     */
    private static final int SHORT=0, LONG=1, FLOAT=2, BOOLEAN=3;

    /**
     * Tests a chain of fallback converters. The initial fallback will understand {@link Short}
     * and {@link Long} types. Then we progressively add more types.
     *
     * <p>This test compares the string representations for convenience.  In theory those string
     * representations are not committed API, so if the {@code FallbackConverter} implementation
     * change, it is okay to update this test accordingly.</p>
     */
    @Test
    public void testChain() {
        final EnumSet<FunctionProperty> SURJECTIVE = EnumSet.of(FunctionProperty.SURJECTIVE);
        final EnumSet<FunctionProperty> INVERTIBLE = EnumSet.of(FunctionProperty.SURJECTIVE, FunctionProperty.INVERTIBLE);

        // The "extends Object" part is unnecessary according Java specification, but Eclipse compiler insists for it.
        ObjectConverter<String, ? extends Object> c = new StringConverter.Short();
        assertEquals(String.class, c.getSourceClass());
        assertEquals(Short.class,  c.getTargetClass());
        assertEquals(INVERTIBLE,   c.properties());
        tryConversions(c, SHORT);
        assertMultilinesEquals(
                "Short ← String", c.toString());

        c = FallbackConverter.merge(c, new StringConverter.Long());
        assertEquals(String.class, c.getSourceClass());
        assertEquals(Number.class, c.getTargetClass());
        assertEquals(SURJECTIVE,   c.properties());
        tryConversions(c, LONG);
        assertMultilinesEquals(
                "Number    ← String\n" +
                "  ├─Short ← String\n" +
                "  └─Long  ← String\n", c.toString());

        c = FallbackConverter.merge(c, new StringConverter.Float());
        assertEquals(String.class, c.getSourceClass());
        assertEquals(Number.class, c.getTargetClass());
        assertEquals(SURJECTIVE,   c.properties());
        tryConversions(c, FLOAT);
        assertMultilinesEquals(
                "Number    ← String\n" +
                "  ├─Short ← String\n" +
                "  ├─Long  ← String\n" +
                "  └─Float ← String\n", c.toString());

        c = FallbackConverter.merge(c, new StringConverter.Integer());
        assertEquals(String.class, c.getSourceClass());
        assertEquals(Number.class, c.getTargetClass());
        assertEquals(SURJECTIVE,   c.properties());
        tryConversions(c, FLOAT);
        assertMultilinesEquals(
                "Number      ← String\n" +
                "  ├─Short   ← String\n" +
                "  ├─Long    ← String\n" +
                "  ├─Float   ← String\n" +
                "  └─Integer ← String\n", c.toString());

        c = FallbackConverter.merge(c, new StringConverter.Boolean());
        assertEquals(String.class, c.getSourceClass());
        assertEquals(Object.class, c.getTargetClass());
        assertEquals(SURJECTIVE,   c.properties());
        tryConversions(c, BOOLEAN);
        assertMultilinesEquals(
                "Object          ← String\n" +
                "  ├─Number      ← String\n" +
                "  │   ├─Short   ← String\n" +
                "  │   ├─Long    ← String\n" +
                "  │   ├─Float   ← String\n" +
                "  │   └─Integer ← String\n" +
                "  └─Boolean     ← String\n", c.toString());

        c = FallbackConverter.merge(c, new StringConverter.Double());
        assertEquals(String.class, c.getSourceClass());
        assertEquals(Object.class, c.getTargetClass());
        assertEquals(SURJECTIVE,   c.properties());
        tryConversions(c, BOOLEAN);
        assertMultilinesEquals(
                "Object          ← String\n" +
                "  ├─Number      ← String\n" +
                "  │   ├─Short   ← String\n" +
                "  │   ├─Long    ← String\n" +
                "  │   ├─Float   ← String\n" +
                "  │   ├─Integer ← String\n" +
                "  │   └─Double  ← String\n" +
                "  └─Boolean     ← String\n", c.toString());
    }

    /**
     * Tries conversions. Parsing numbers should returns a Short, Long or Float because
     * they were the first converters declared. Parsing a boolean should succeed only
     * after we registered the boolean converter.
     *
     * <p>Calls to {@link SystemConverter#inverse()} and serialization shall be avoided,
     * because they indirectly use {@link ConverterRegistry} while we want to keep the
     * tests isolated.</p>
     */
    private static void tryConversions(final ObjectConverter<String,?> c, final int supported) {
        assertConvertedEquals(c, (supported >= SHORT)   ? Short.valueOf((short) 5) : null, "5");
        assertConvertedEquals(c, (supported >= LONG )   ? Long .valueOf(    80000) : null, "80000");
        assertConvertedEquals(c, (supported >= FLOAT)   ? Float.valueOf(       5f) : null, "5.0");
        assertConvertedEquals(c, (supported >= FLOAT)   ? Float.valueOf(     2.5f) : null, "2.5");
        assertConvertedEquals(c, (supported >= FLOAT)   ? Float.valueOf(   1E+20f) : null, "1E+20");
        assertConvertedEquals(c, (supported >= FLOAT)   ? Float.POSITIVE_INFINITY  : null, "1E+80");
        assertConvertedEquals(c, (supported >= BOOLEAN) ? Boolean.TRUE             : null, "true");
    }

    /**
     * Converts the given value and compares the result with the expected one.
     *
     * @param  converter The converter to use.
     * @param  expected  The expected result, or {@code null} if the conversion is expected to fail.
     * @param  value     The value to convert.
     * @throws UnconvertibleObjectException if an exception was not expected but occurred.
     */
    private static void assertConvertedEquals(final ObjectConverter<String,?> converter,
            final Object expected, final String value) throws UnconvertibleObjectException
    {
        final Object result;
        try {
            result = converter.apply(value);
        } catch (UnconvertibleObjectException exception) {
            if (expected != null) {
                throw exception;
            }
            return;
        }
        assertNotNull(result);
        if (expected == null) {
            fail("Expected an exception but got " + result);
        }
        assertEquals(expected, result);
    }
}
