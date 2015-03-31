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

import java.net.URI;
import java.util.Date;
import java.util.Locale;
import org.opengis.util.TypeName;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.internal.util.Constants.OGC;


/**
 * Tests the {@link TypeNames} class. Tests are performed through the {@link DefaultNameFactory#toTypeName(Class)}
 * method on the {@link DefaultFactories#SIS_NAMES} instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn({
    TypesTest.class,
    DefaultNameFactoryTest.class
})
public final strictfp class TypeNamesTest extends TestCase {
    /**
     * Verifies that the call to {@link TypeNames#toTypeName(NameFactory, Class)} returns a {@code TypeName} having the
     * given name and namespace, then tests the reverse operation with {@link TypeNames#toClass(String, String)}.
     */
    private static void verifyLookup(final String namespace, final String name, final Class<?> valueClass)
            throws ClassNotFoundException
    {
        final DefaultNameFactory factory = DefaultFactories.forBuildin(NameFactory.class, DefaultNameFactory.class);
        final TypeName type = factory.toTypeName(valueClass);
        assertNotNull(name, type);
        assertSame   (name, valueClass, ((DefaultTypeName) type).toClass());
        assertEquals (name, namespace,  type.scope().name().toString());
        assertEquals (name, name,       type.toString());
        assertEquals (name, valueClass, TypeNames.toClass(namespace, name));
    }

    /**
     * Returns the string representation of the fully qualified path of the type name for the given value.
     */
    private static String toTypeName(final DefaultNameFactory factory, final Class<?> valueClass) {
        return factory.toTypeName(valueClass).toFullyQualifiedName().toString();
    }

    /**
     * Tests the mapping of basic types like strings, URI, dates and numbers.
     *
     * @throws ClassNotFoundException Should not happen since we do invoke {@link Class#forName(String)} in this test.
     */
    @Test
    public void testBasicTypes() throws ClassNotFoundException {
        verifyLookup(OGC, "URI",              URI.class);
        verifyLookup(OGC, "PT_Locale",        Locale.class);
        verifyLookup(OGC, "DateTime",         Date.class);
        verifyLookup(OGC, "FreeText",         InternationalString.class);
        verifyLookup(OGC, "CharacterString",  String.class);
        verifyLookup(OGC, "Boolean",          Boolean.class);
        verifyLookup(OGC, "Real",             Double.class);
        verifyLookup(OGC, "Integer",          Integer.class);
    }

    /**
     * Tests {@link TypeNames#toTypeName(NameFactory, Class)} with numbers.
     */
    @Test
    public void testNumbers() {
        final DefaultNameFactory factory = DefaultFactories.forBuildin(NameFactory.class, DefaultNameFactory.class);
        assertEquals("Short",  OGC+":Integer", toTypeName(factory, Short .class));
        assertEquals("Long",   OGC+":Integer", toTypeName(factory, Long  .class));
        assertEquals("Float",  OGC+":Real",    toTypeName(factory, Float .class));
        assertEquals("Double", OGC+":Real",    toTypeName(factory, Double.class));
    }

    /**
     * Tests the mapping of more complex object that are not basic types.
     *
     * @throws ClassNotFoundException Should not happen since we do invoke {@link Class#forName(String)} in this test.
     */
    @Test
    public void testMetadataClasses() throws ClassNotFoundException {
        verifyLookup(OGC, "MD_Metadata", Metadata.class);
        verifyLookup(OGC, "SC_CRS",      CoordinateReferenceSystem.class);
    }

    /**
     * Tests the mapping of objects not defined by OGC.
     *
     * @throws ClassNotFoundException If the call to {@link Class#forName(String)} failed.
     */
    @Test
    public void testOtherClasses() throws ClassNotFoundException {
        verifyLookup("class", "java.util.Random", java.util.Random.class);
    }

    /**
     * Checks for the sentinel values in case of invalid names.
     *
     * @throws ClassNotFoundException Should not happen since we do invoke {@link Class#forName(String)} in this test.
     */
    @Test
    public void testInvalidNames() throws ClassNotFoundException {
        assertEquals("Dummy:Real", Void.TYPE,    TypeNames.toClass("Dummy", "Real"));
        assertEquals(OGC+":Real",  Double.class, TypeNames.toClass(OGC,     "Real"));
        assertEquals("Real",       Double.class, TypeNames.toClass(null,    "Real"));
        assertEquals("Dummy",      Void.TYPE,    TypeNames.toClass(null,    "Dummy")); // Considered not an error.
        assertNull  (OGC+":Dummy",               TypeNames.toClass(OGC,     "Dummy")); // Considered an error.
    }
}
