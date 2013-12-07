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

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import org.opengis.test.Validators;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.metadata.iso.citation.HardCodedCitations.EPSG;


/**
 * Tests {@link AbstractIdentifiedObject}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
@DependsOn({IdentifiedObjectsTest.class, NamedIdentifierTest.class})
public final strictfp class AbstractIdentifiedObjectTest extends TestCase {
    /**
     * Tests the {@link AbstractIdentifiedObject#AbstractIdentifiedObject(Map)} constructor.
     */
    @Test
    public void testCreateFromMap() {
        final Map<String,Object> properties = new HashMap<String,Object>(10);
        assertNull(properties.put("name",       "This is a name"));
        assertNull(properties.put("remarks",    "There is remarks"));
        assertNull(properties.put("remarks_fr", "Voici des remarques"));

        final AbstractIdentifiedObject object = new AbstractIdentifiedObject(properties);
        Validators.validate(object);

        assertEquals("name",       "This is a name",      object.getName().getCode());
        assertNull  ("codeSpace",                         object.getName().getCodeSpace());
        assertNull  ("version",                           object.getName().getVersion());
        assertTrue  ("aliases",                           object.getAlias().isEmpty());
        assertTrue  ("identifiers",                       object.getIdentifiers().isEmpty());
        assertEquals("ID",         "Thisisaname",         object.getID());
        assertEquals("remarks",    "There is remarks",    object.getRemarks().toString(Locale.ENGLISH));
        assertEquals("remarks_fr", "Voici des remarques", object.getRemarks().toString(Locale.FRENCH));
    }

    /**
     * Tests identifiers getter. The methods of interest to this test are:
     *
     * <ul>
     *   <li>{@link AbstractIdentifiedObject#getIdentifiers()}</li>
     *   <li>{@link AbstractIdentifiedObject#getIdentifier()}</li>
     *   <li>{@link AbstractIdentifiedObject#getID()}</li>
     * </ul>
     *
     * Note that {@code getID()} were also tested in {@link #testCreateFromMap()}
     * but in the absence of identifiers.
     */
    @Test
    @DependsOnMethod("testCreateFromMap")
    public void testGetIdentifiers() {
        final Map<String,Object> properties = new HashMap<String,Object>(8);
        assertNull(properties.put("name", "WGS 84"));
        assertNull(properties.put("identifiers", new NamedIdentifier[] {
            new NamedIdentifier(EPSG, "4326"),
            new NamedIdentifier(EPSG, "IgnoreMe")
        }));

        final AbstractIdentifiedObject object = new AbstractIdentifiedObject(properties);
        Validators.validate(object);

        assertEquals("name",        "WGS 84",                     object.getName().getCode());
        assertEquals("identifiers", "[EPSG:4326, EPSG:IgnoreMe]", object.getIdentifiers().toString());
        assertEquals("ID",          "EPSG4326",                   object.getID());
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testCreateFromMap")
    public void testSerialization() {
        final Map<String,Object> properties = new HashMap<String,Object>(8);
        assertNull(properties.put("code",      "4326"));
        assertNull(properties.put("codeSpace", "EPSG"));
        assertNull(properties.put("remarks",   "There is remarks"));

        final AbstractIdentifiedObject object = new AbstractIdentifiedObject(properties);
        Validators.validate(object);

        assertNotSame(object, assertSerializedEquals(object));
    }
}
