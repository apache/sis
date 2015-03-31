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
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;
import org.opengis.util.ScopedName;
import org.apache.sis.util.UnknownNameException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Names} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(DefaultNameFactoryTest.class)
public final strictfp class NamesTest extends TestCase {
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
        final DefaultNameFactory factory = DefaultFactories.forBuildin(NameFactory.class, DefaultNameFactory.class);
        final TypeName type = factory.toTypeName(Random.class);
        assertEquals("class:java.util.Random", type.toFullyQualifiedName().toString());
        assertValueClassEquals(Random.class, type);
        assertValueClassEquals(DefaultNameFactoryTest.class,
                new DefaultTypeName(type.scope(), DefaultNameFactoryTest.class.getName()));
        assertValueClassEquals(UnknownNameException.class,
                new DefaultTypeName(type.scope(), "org.apache.sis.Dummy"));
    }

    /**
     * Tests {@link Names#toClass(TypeName)} with a name in the {@code "OGC"} scope.
     * If the name is not recognized, then {@code toClass(TypeName)} is expected to throw an exception.
     */
    @Test
    public void testClassFromOGC() {
        final DefaultNameFactory factory = DefaultFactories.forBuildin(NameFactory.class, DefaultNameFactory.class);
        final TypeName type = factory.toTypeName(String.class);
        assertEquals("OGC:CharacterString", type.toFullyQualifiedName().toString());
        assertValueClassEquals(String.class,               type);
        assertValueClassEquals(Double.class,               new DefaultTypeName(type.scope(), "Real"));
        assertValueClassEquals(InternationalString.class,  new DefaultTypeName(type.scope(), "FreeText"));
        assertValueClassEquals(UnknownNameException.class, new DefaultTypeName(type.scope(), "Dummy"));
    }

    /**
     * Tests {@link Names#toClass(TypeName)} with in a scope different than {@code "OGC"}.
     * If the name is not recognized, then {@code toClass(TypeName)} is expected to return
     * {@code null} rather than throwing an exception because the namespace is used for too
     * many things - we can not said that the name is wrong.
     */
    @Test
    public void testClassFromOtherNamespaces() {
        assertValueClassEquals(null,         Names.createTypeName("MyOrg", ":", "CharacterString"));
        assertValueClassEquals(String.class, Names.createTypeName(null,    ":", "CharacterString"));
        assertValueClassEquals(null,         Names.createTypeName(null,    ":", "Dummy"));
    }

    /**
     * Invokes {@link Names#toClass(TypeName)}, but catch {@link UnknownNameException}.
     * If the later exception is caught, then this method returns {@code UnknownNameException.class}.
     */
    private static Class<?> toClass(final TypeName type) {
        try {
            return Names.toClass(type);
        } catch (UnknownNameException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains(type.toFullyQualifiedName().toString()));
            return UnknownNameException.class;
        }
    }

    /**
     * Asserts that calls to {@link Names#toClass(TypeName)} returns the expected value class.
     */
    private static void assertValueClassEquals(final Class<?> expected, final TypeName type) {
        assertEquals(expected, toClass(type));

        // Tests detection with an implementation which is not the SIS one.
        assertEquals(expected, toClass(new TypeName() {
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
