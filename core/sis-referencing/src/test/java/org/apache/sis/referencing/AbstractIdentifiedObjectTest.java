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
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Collections;
import org.opengis.test.Validators;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.referencing.datum.AbstractDatum;
import org.apache.sis.internal.jaxb.referencing.Code;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.metadata.iso.citation.Citations.EPSG;


/**
 * Tests the {@link AbstractIdentifiedObject} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@DependsOn({
    IdentifiedObjectsTest.class, NamedIdentifierTest.class,
    org.apache.sis.internal.jaxb.referencing.CodeTest.class
})
public final strictfp class AbstractIdentifiedObjectTest extends TestCase {
    /**
     * Creates a map of properties to be given to the {@link AbstractIdentifiedObject} constructor.
     * The values in the map are consistent with the values expected by the {@link #validate} method.
     *
     * @param identifier The value for the {@code "identifiers"} property.
     */
    private static Map<String,Object> properties(final Set<ReferenceIdentifier> identifiers) {
        final Map<String,Object> properties = new HashMap<String,Object>(8);
        assertNull(properties.put("name",       "GRS 1980"));
        assertNull(properties.put("identifiers", identifiers.toArray(new ReferenceIdentifier[identifiers.size()])));
        assertNull(properties.put("codespace",  "EPSG"));
        assertNull(properties.put("version",    "8.3"));
        assertNull(properties.put("alias",      "International 1979"));
        assertNull(properties.put("remarks",    "Adopted by IUGG 1979 Canberra"));
        assertNull(properties.put("remarks_fr", "Adopté par IUGG 1979 Canberra"));
        return properties;
    }

    /**
     * Validates the given object created by {@link #testCreateFromMap()}.
     *
     * @param  object      The object to validate.
     * @param  identifiers The expected value of {@link AbstractIdentifiedObject#getIdentifiers()}.
     * @param  gmlID       The expected value of {@link AbstractIdentifiedObject#getID()}.
     * @return The value of {@link AbstractIdentifiedObject#getIdentifier()}.
     */
    private static ReferenceIdentifier validate(final AbstractIdentifiedObject object,
            final Set<ReferenceIdentifier> identifiers, final String gmlID)
    {
        Validators.validate(object);
        final ReferenceIdentifier name = object.getName();
        assertEquals("name",        "GRS 1980",                      name.getCode());
        assertEquals("codespace",   "EPSG",                          name.getCodeSpace());
        assertEquals("version",     "8.3",                           name.getVersion());
        assertEquals("aliases",     "International 1979",            getSingleton(object.getAlias()).toString());
        assertEquals("names",       name,                            getSingleton(object.getNames()));
        assertEquals("identifiers", identifiers,                     object.getIdentifiers());
        assertEquals("ID",          gmlID,                           object.getID());
        assertEquals("remarks",     "Adopted by IUGG 1979 Canberra", object.getRemarks().toString(Locale.ENGLISH));
        assertEquals("remarks_fr",  "Adopté par IUGG 1979 Canberra", object.getRemarks().toString(Locale.FRENCH));
        final Code code = object.getIdentifier();
        return (code != null) ? code.getIdentifier() : null;
    }

    /**
     * Tests the {@link AbstractIdentifiedObject#AbstractIdentifiedObject(Map)} constructor without name.
     * This is invalid and should thrown an exception.
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testMissingName() {
        final Map<String,Object> properties = new HashMap<String,Object>(4);
        assertNull(properties.put(AbstractIdentifiedObject.REMARKS_KEY, "Not a name."));
        try {
            new AbstractIdentifiedObject(properties);
            fail("Should not allow unnamed object.");
        } catch (IllegalArgumentException e) {
            // The message may be in any language, but shall
            // contain at least the missing property name.
            final String message = e.getMessage();
            assertTrue(message, message.contains("name"));
        }
        // Try again, with error messages forced to English.
        assertNull(properties.put(AbstractIdentifiedObject.LOCALE_KEY, Locale.US));
        try {
            new AbstractIdentifiedObject(properties);
            fail("Should not allow unnamed object.");
        } catch (IllegalArgumentException e) {
            assertEquals("Missing value for “name” property.", e.getMessage());
        }
        // "code" with String value is accepted as well.
        assertNull(properties.put("code", "Test"));
        assertEquals("Test", new AbstractIdentifiedObject(properties).getName().getCode());
    }

    /**
     * Tests the {@link AbstractIdentifiedObject#AbstractIdentifiedObject(Map)} constructor without identifier.
     * This method compares the property values against the expected values.
     */
    @Test
    public void testWithoutIdentifier() {
        final Set<ReferenceIdentifier> identifiers = Collections.<ReferenceIdentifier>emptySet();
        final AbstractIdentifiedObject object      = new AbstractIdentifiedObject(properties(identifiers));
        final ReferenceIdentifier      gmlId       = validate(object, identifiers, "GRS1980");
        assertNull("gmlId", gmlId);
    }

    /**
     * Tests the {@link AbstractIdentifiedObject#AbstractIdentifiedObject(Map)} constructor
     * with only one identifier. The methods of interest for this test are:
     *
     * <ul>
     *   <li>{@link AbstractIdentifiedObject#getIdentifiers()}</li>
     *   <li>{@link AbstractIdentifiedObject#getIdentifier()}</li>
     *   <li>{@link AbstractIdentifiedObject#getID()}</li>
     * </ul>
     */
    @Test
    @DependsOnMethod("testWithoutIdentifier")
    public void testWithSingleIdentifier() {
        final ReferenceIdentifier      identifier  = new ImmutableIdentifier(null, "EPSG", "7019");
        final Set<ReferenceIdentifier> identifiers = Collections.singleton(identifier);
        final AbstractIdentifiedObject object      = new AbstractIdentifiedObject(properties(identifiers));
        final ReferenceIdentifier      gmlId       = validate(object, identifiers, "epsg-7019");
        assertNotNull("gmlId",                   gmlId);
        assertEquals ("gmlId.codespace", "EPSG", gmlId.getCodeSpace());
        assertEquals ("gmlId.code",      "7019", gmlId.getCode());
    }

    /**
     * Tests the {@link AbstractIdentifiedObject#AbstractIdentifiedObject(Map)} constructor
     * with more than one identifier. This method also tries a different identifier implementation
     * than the one used by {@link #testWithSingleIdentifier()}.
     */
    @Test
    @DependsOnMethod("testWithSingleIdentifier")
    public void testWithManyIdentifiers() {
        final Set<ReferenceIdentifier> identifiers = new LinkedHashSet<ReferenceIdentifier>(4);
        assertTrue(identifiers.add(new NamedIdentifier(EPSG, "7019")));
        assertTrue(identifiers.add(new NamedIdentifier(EPSG, "IgnoreMe")));
        final AbstractIdentifiedObject object = new AbstractIdentifiedObject(properties(identifiers));
        final ReferenceIdentifier      gmlId  = validate(object, identifiers, "epsg-7019");
        assertNotNull("gmlId",                   gmlId);
        assertEquals ("gmlId.codespace", "EPSG", gmlId.getCodeSpace());
        assertEquals ("gmlId.code",      "7019", gmlId.getCode());
    }

    /**
     * Tests {@link AbstractIdentifiedObject#getIdentifier()} with a sub-type of {@code AbstractIdentifiedObject}.
     * The use of a subtype will allow {@code getIdentifier()} to build a URN and {@code getId()} to know what to
     * insert between {@code "epsg-"} and the code.
     */
    @Test
    @DependsOnMethod("testWithManyIdentifiers")
    public void testAsSubtype() {
        final ReferenceIdentifier      identifier  = new NamedIdentifier(EPSG, "7019");
        final Set<ReferenceIdentifier> identifiers = Collections.singleton(identifier);
        final AbstractIdentifiedObject object      = new AbstractDatum(properties(identifiers));
        final ReferenceIdentifier      gmlId       = validate(object, identifiers, "epsg-datum-7019");
        assertNotNull("gmlId",                   gmlId);
        assertEquals ("gmlId.codespace", "EPSG", gmlId.getCodeSpace());
        assertEquals ("gmlId.code",      "7019", gmlId.getCode());
    }

    /**
     * Tests two {@code AbstractIdentifiedObject} declaring the same identifier in the same XML document.
     * The {@code getID()} method should detect the collision and select different identifier.
     *
     * @since 0.7
     */
    @Test
    @DependsOnMethod("testWithManyIdentifiers")
    public void testIdentifierCollision() {
        final Map<String,Object> properties = new HashMap<String,Object>(4);
        assertNull(properties.put("name", "GRS 1980"));
        assertNull(properties.put("identifiers", new NamedIdentifier(EPSG, "7019")));
        final AbstractIdentifiedObject o1 = new AbstractIdentifiedObject(properties);
        final AbstractIdentifiedObject o2 = new AbstractIdentifiedObject(properties);
        final AbstractIdentifiedObject o3 = new AbstractIdentifiedObject(properties);
        final AbstractIdentifiedObject o4 = new AbstractIdentifiedObject(properties);
        final Context context = new Context(0, null, null, null, null, null, null, null);
        try {
            final String c1, c2, c3, c4;
            assertEquals("o1", "epsg-7019", c1 = o1.getID());
            assertEquals("o2", "GRS1980",   c2 = o2.getID());
            assertEquals("o3", "GRS1980-1", c3 = o3.getID());
            assertEquals("o4", "GRS1980-2", c4 = o4.getID());
            assertSame  ("o1", c1, o1.getID());  // Verify that values are remembered.
            assertSame  ("o2", c2, o2.getID());
            assertSame  ("o3", c3, o3.getID());
            assertSame  ("o4", c4, o4.getID());
        } finally {
            context.finish();
        }
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testWithoutIdentifier")
    public void testSerialization() {
        final Set<ReferenceIdentifier> identifiers = Collections.emptySet();
        final AbstractIdentifiedObject object      = new AbstractIdentifiedObject(properties(identifiers));
        final AbstractIdentifiedObject actual      = assertSerializedEquals(object);
        assertNotSame(object, actual);
        assertNull("gmlId", validate(actual, identifiers, "GRS1980"));
    }
}
