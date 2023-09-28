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

import java.util.List;
import java.util.Random;
import org.opengis.util.GenericName;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.InternationalString;
import org.opengis.util.NameSpace;
import org.opengis.util.ScopedName;
import org.apache.sis.util.UnknownNameException;

// Test dependencies
import org.junit.Test;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;


/**
 * Tests the {@link Names} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.5
 */
@DependsOn(DefaultNameFactoryTest.class)
public final class NamesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public NamesTest() {
    }

    /**
     * Tests {@link Names#createScopedName(GenericName, String, CharSequence)}.
     */
    @Test
    @DependsOnMethod("testCreateLocalName")
    public void testCreateScopedName() {
        final LocalName scope = Names.createLocalName("Apache", null, "sis");
        final ScopedName name = Names.createScopedName(scope, null, "identifier");
        assertSame  ("path()",      scope,                   name.path());
        assertEquals("tail()",      "identifier",            name.tail().toString());
        assertEquals("toString()",  "sis:identifier",        name.toString());
        assertEquals("full",        "Apache:sis:identifier", name.toFullyQualifiedName().toString());
        assertEquals("tail().full", "Apache:sis:identifier", name.tail().toFullyQualifiedName().toString());
    }

    /**
     * Tests {@link Names#createLocalName(CharSequence, String, CharSequence)}.
     */
    @Test
    public void testCreateLocalName() {
        final LocalName name = Names.createLocalName("http://www.opengis.net/gml/srs/epsg.xml", "#", "4326");
        assertEquals("http://www.opengis.net/gml/srs/epsg.xml",       name.scope().name().toString());
        assertEquals("4326",                                          name.toString());
        assertEquals("http://www.opengis.net/gml/srs/epsg.xml#4326",  name.toFullyQualifiedName().toString());
        assertEquals("{http://www.opengis.net/gml/srs/epsg.xml}4326", Names.toExpandedString(name));
    }

    /**
     * Tests {@link Names#toClass(TypeName)} with a name in the {@code "class"} scope.
     * If the name is not recognized, then {@code toClass(TypeName)} is expected to throw an exception.
     */
    @Test
    public void testClassFromClassname() {
        final DefaultNameFactory factory = DefaultNameFactory.provider();
        final TypeName type = factory.toTypeName(Random.class);
        assertEquals("class:java.util.Random", type.toFullyQualifiedName().toString());
        assertValueClassEquals(Random.class, type);
        assertValueClassEquals(DefaultNameFactoryTest.class,
                new DefaultTypeName(type.scope(), DefaultNameFactoryTest.class.getName()));
        try {
            new DefaultTypeName(type.scope(), "org.apache.sis.Dummy");
            fail("Expected UnknownNameException.");
        } catch (UnknownNameException e) {
            assertTrue(e.getMessage().contains("org.apache.sis.Dummy"));
        }
    }

    /**
     * Tests {@link Names#toClass(TypeName)} with a name in the {@code "OGC"} scope.
     * If the name is not recognized, then {@code toClass(TypeName)} is expected to throw an exception.
     */
    @Test
    public void testClassFromOGC() {
        final DefaultNameFactory factory = DefaultNameFactory.provider();
        final TypeName type = factory.toTypeName(String.class);
        assertEquals("OGC:CharacterString", type.toFullyQualifiedName().toString());
        assertValueClassEquals(String.class,               type);
        assertValueClassEquals(Double.class,               new DefaultTypeName(type.scope(), "Real"));
        assertValueClassEquals(InternationalString.class,  new DefaultTypeName(type.scope(), "FreeText"));
        try {
            new DefaultTypeName(type.scope(), "Dummy");
            fail("Expected UnknownNameException.");
        } catch (UnknownNameException e) {
            assertTrue(e.getMessage().contains("OGC:Dummy"));
        }
    }

    /**
     * Tests {@link Names#toClass(TypeName)} with in a scope different than {@code "OGC"}.
     * If the name is not recognized, then {@code toClass(TypeName)} is expected to return
     * {@code null} rather than throwing an exception because the namespace is used for too
     * many things - we cannot said that the name is wrong.
     */
    @Test
    public void testClassFromOtherNamespaces() {
        assertValueClassEquals(null,         Names.createTypeName("MyOrg", ":", "CharacterString"));
        assertValueClassEquals(String.class, Names.createTypeName(null,   null, "CharacterString"));
        assertValueClassEquals(null,         Names.createTypeName(null,   null, "Dummy"));
    }

    /**
     * Asserts that calls to {@link Names#toClass(TypeName)} returns the expected value class.
     */
    private static void assertValueClassEquals(final Class<?> expected, final TypeName type) {
        assertEquals(expected, Names.toClass(type));
        /*
         * Tests detection with an implementation which is not the SIS one.
         */
        assertEquals(expected, Names.toClass(new TypeName() {
            @Override public int                       depth()                  {return type.depth();}
            @Override public List<? extends LocalName> getParsedNames()         {return type.getParsedNames();}
            @Override public LocalName                 head()                   {return type.head();}
            @Override public LocalName                 tip()                    {return type.tip();}
            @Override public NameSpace                 scope()                  {return type.scope();}
            @Override public GenericName               toFullyQualifiedName()   {return type.toFullyQualifiedName();}
            @Override public ScopedName                push(GenericName scope)  {return type.push(scope);}
            @Override public String                    toString()               {return type.toString();}
            @Override public InternationalString       toInternationalString()  {return type.toInternationalString();}
            @Override public int                       compareTo(GenericName o) {return type.compareTo(o);}
        }));
    }
}
