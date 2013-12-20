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
package org.apache.sis.referencing;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Collections;
import org.opengis.test.Validators;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.metadata.iso.citation.HardCodedCitations.EPSG;


/**
 * Tests {@link AbstractIdentifiedObject}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
@DependsOn({
    IdentifiedObjectsTest.class, NamedIdentifierTest.class,
    org.apache.sis.internal.jaxb.referencing.RS_IdentifierTest.class
})
public final strictfp class AbstractIdentifiedObjectTest extends TestCase {
    /**
     * Tests the {@link AbstractIdentifiedObject#AbstractIdentifiedObject(Map)} constructor.
     */
    @Test
    public void testCreateFromMap() {
        final Map<String,Object> properties = new HashMap<>(8);
        assertNull(properties.put("name",       "GRS 1980"));
        assertNull(properties.put("codespace",  "EPSG"));
        assertNull(properties.put("version",    "8.3"));
        assertNull(properties.put("alias",      "International 1979"));
        assertNull(properties.put("remarks",    "Adopted by IUGG 1979 Canberra"));
        assertNull(properties.put("remarks_fr", "Adopté par IUGG 1979 Canberra"));
        validate(new AbstractIdentifiedObject(properties), Collections.<ReferenceIdentifier>emptySet(), "GRS1980");
        /*
         * Adds an identifier. This should change the choice made by AbstractIdentifiedObject.getID().
         */
        final ReferenceIdentifier identifier = new ImmutableIdentifier(null, "EPSG", "7019");
        assertNull(properties.put("identifiers", identifier));
        validate(new AbstractIdentifiedObject(properties), Collections.singleton(identifier), "epsg-7019");
    }

    /**
     * Validates the given object created by {@link #testCreateFromMap()}.
     */
    private static void validate(final AbstractIdentifiedObject object,
            final Set<ReferenceIdentifier> identifiers, final String gmlID)
    {
        Validators.validate(object);
        final ReferenceIdentifier name = object.getName();
        assertEquals("name",        "GRS 1980",                      name.getCode());
        assertEquals("codespace",   "EPSG",                          name.getCodeSpace());
        assertEquals("version",     "8.3",                           name.getVersion());
        assertEquals("aliases",     "International 1979",            getSingleton(object.getAlias()).toString());
        assertEquals("names",       Collections.singletonList(name), object.getNames());
        assertEquals("identifiers", identifiers,                     object.getIdentifiers());
        assertEquals("ID",          gmlID,                           object.getID());
        assertEquals("remarks",     "Adopted by IUGG 1979 Canberra", object.getRemarks().toString(Locale.ENGLISH));
        assertEquals("remarks_fr",  "Adopté par IUGG 1979 Canberra", object.getRemarks().toString(Locale.FRENCH));
    }

    /**
     * Tests identifiers getter. The methods of interest to this test are:
     *
     * <ul>
     *   <li>{@link AbstractIdentifiedObject#getIdentifiers()}</li>
     *   <li>{@link AbstractIdentifiedObject#getID()}</li>
     * </ul>
     *
     * Note that {@code getID()} were also tested in {@link #testCreateFromMap()}
     * but in the absence of identifiers.
     */
    @Test
    @DependsOnMethod("testCreateFromMap")
    public void testGetIdentifiers() {
        final Map<String,Object> properties = new HashMap<>(8);
        assertNull(properties.put("name", "WGS 84"));
        assertNull(properties.put("identifiers", new NamedIdentifier[] {
            new NamedIdentifier(EPSG, "4326"),
            new NamedIdentifier(EPSG, "IgnoreMe")
        }));

        final AbstractIdentifiedObject object = new AbstractIdentifiedObject(properties);
        Validators.validate(object);

        assertEquals("name",        "WGS 84",                     object.getName().getCode());
        assertEquals("identifiers", "[EPSG:4326, EPSG:IgnoreMe]", object.getIdentifiers().toString());
        assertEquals("ID",          "epsg-4326",                  object.getID());
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testCreateFromMap")
    public void testSerialization() {
        final Map<String,Object> properties = new HashMap<>(8);
        assertNull(properties.put("code",      "4326"));
        assertNull(properties.put("codeSpace", "EPSG"));
        assertNull(properties.put("remarks",   "There is remarks"));

        final AbstractIdentifiedObject object = new AbstractIdentifiedObject(properties);
        Validators.validate(object);

        assertNotSame(object, assertSerializedEquals(object));
    }
}
