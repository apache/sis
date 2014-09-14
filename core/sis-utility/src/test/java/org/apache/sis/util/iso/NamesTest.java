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
import org.opengis.util.GenericName;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.InternationalString;
import org.opengis.util.NameSpace;
import org.opengis.util.ScopedName;
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
     * Tests {@link Names#toClass(TypeName)} with a known type name.
     */
    @Test
    public void testToClass() {
        final TypeName type = DefaultFactories.SIS_NAMES.toTypeName(String.class);
        assertEquals("OGC:CharacterString", type.toFullyQualifiedName().toString());

        // Tests detection from the name.
        assertEquals(InternationalString.class, Names.toClass(new DefaultTypeName(type.scope(), "FreeText")));
        assertValueClassEquals(String.class, type);
    }

    /**
     * Tests {@link Names#toClass(TypeName)} with an unknown type name.
     */
    @Test
    public void testUnknownType() {
        assertValueClassEquals(null, Names.createTypeName("MyOrg", ":", "CharacterString"));
    }

    /**
     * Asserts that calls to {@link Names#toClass(TypeName)} returns the expected value class.
     */
    private static void assertValueClassEquals(final Class<?> expected, final TypeName type) {
        assertEquals(expected, Names.toClass(type));

        // Tests detection with an implementation which is not the SIS one.
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
