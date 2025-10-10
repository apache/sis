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
import org.apache.sis.referencing.datum.AbstractDatum;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.referencing.Code;
import static org.apache.sis.metadata.iso.citation.Citations.EPSG;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.referencing.Assertions.assertRemarksEquals;

// Specific to the geoapi-4.0 branch:
import org.opengis.metadata.Identifier;


/**
 * Tests the {@link AbstractIdentifiedObject} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@SuppressWarnings("exports")
public final class AbstractIdentifiedObjectTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public AbstractIdentifiedObjectTest() {
    }

    /**
     * Creates a map of properties to be given to the {@link AbstractIdentifiedObject} constructor.
     * The values in the map are consistent with the values expected by the {@link #validate} method.
     *
     * @param  identifiers  the value for the {@code "identifiers"} property.
     */
    private static Map<String,Object> properties(final Set<Identifier> identifiers) {
        final var properties = new HashMap<String,Object>(8);
        assertNull(properties.put("name",       "GRS 1980"));
        assertNull(properties.put("identifiers", identifiers.toArray(Identifier[]::new)));
        assertNull(properties.put("codespace",  "EPSG"));
        assertNull(properties.put("version",    "8.3"));
        assertNull(properties.put("alias",      "International 1979"));
        assertNull(properties.put("remarks",    "Adopted by IUGG 1979 Canberra"));
        assertNull(properties.put("remarks_fr", "Adopté par IUGG 1979 Canberra"));
        return properties;
    }

    /**
     * Validates the given object created by the test methods.
     *
     * @param  object       the object to validate.
     * @param  identifiers  the expected value of {@link AbstractIdentifiedObject#getIdentifiers()}.
     * @param  gmlID        the expected value of {@link AbstractIdentifiedObject#getID()}.
     * @return the value of {@link AbstractIdentifiedObject#getIdentifier()}.
     */
    private static Identifier validate(final AbstractIdentifiedObject object,
            final Set<Identifier> identifiers, final String gmlID)
    {
        Validators.validate(object);
        final var name = object.getName();
        assertEquals("GRS 1980",           name.getCode(), "name");
        assertEquals("EPSG",               name.getCodeSpace(), "codespace");
        assertEquals("8.3",                name.getVersion(), "version");
        assertEquals("International 1979", getSingleton(object.getAlias()).toString(), "aliases");
        assertEquals(name,                 getSingleton(object.getNames()), "names");
        assertEquals(identifiers,          object.getIdentifiers(), "identifiers");
        assertEquals(gmlID,                object.getID(), "ID");
        assertRemarksEquals("Adopted by IUGG 1979 Canberra", object, Locale.ENGLISH);
        assertRemarksEquals("Adopté par IUGG 1979 Canberra", object, Locale.FRENCH);
        final Code code = object.getIdentifier();
        return (code != null) ? code.getIdentifier() : null;
    }

    /**
     * Tests the {@link AbstractIdentifiedObject#AbstractIdentifiedObject(Map)} constructor without name.
     * This is invalid and should throw an exception.
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testMissingName() {
        final var properties = new HashMap<String,Object>(4);
        assertNull(properties.put(AbstractIdentifiedObject.REMARKS_KEY, "Not a name."));
        final Executable test = () -> new AbstractIdentifiedObject(properties);
        /*
         * The message may be in any language, but shall
         * contain at least the missing property name.
         */
        IllegalArgumentException exception;
        exception = assertThrows(IllegalArgumentException.class, test, "Should not allow unnamed object.");
        assertMessageContains(exception, "name");

        // Try again, with error messages forced to English.
        assertNull(properties.put(AbstractIdentifiedObject.LOCALE_KEY, Locale.US));
        exception = assertThrows(IllegalArgumentException.class, test, "Should not allow unnamed object.");
        assertEquals("Missing value for the “name” property.", exception.getMessage());

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
        final var identifiers = Set.<Identifier>of();
        final var object      = new AbstractIdentifiedObject(properties(identifiers));
        final var gmlId       = validate(object, identifiers, "GRS1980");
        assertNull(gmlId);
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
    public void testWithSingleIdentifier() {
        final var identifier  = new ImmutableIdentifier(null, "EPSG", "7019");
        final var identifiers = Set.<Identifier>of(identifier);
        final var object      = new AbstractIdentifiedObject(properties(identifiers));
        final var gmlId       = validate(object, identifiers, "epsg-7019");
        assertNotNull(        gmlId);
        assertEquals ("EPSG", gmlId.getCodeSpace());
        assertEquals ("7019", gmlId.getCode());
    }

    /**
     * Tests the {@link AbstractIdentifiedObject#AbstractIdentifiedObject(Map)} constructor
     * with more than one identifier. This method also tries a different identifier implementation
     * than the one used by {@link #testWithSingleIdentifier()}.
     */
    @Test
    public void testWithManyIdentifiers() {
        final var identifiers = new LinkedHashSet<Identifier>(4);
        assertTrue(identifiers.add(new NamedIdentifier(EPSG, "7019")));
        assertTrue(identifiers.add(new NamedIdentifier(EPSG, "IgnoreMe")));
        final var object = new AbstractIdentifiedObject(properties(identifiers));
        final var gmlId  = validate(object, identifiers, "epsg-7019");
        assertNotNull(        gmlId);
        assertEquals ("EPSG", gmlId.getCodeSpace());
        assertEquals ("7019", gmlId.getCode());
    }

    /**
     * Tests {@link AbstractIdentifiedObject#getIdentifier()} with a sub-type of {@code AbstractIdentifiedObject}.
     * The use of a subtype will allow {@code getIdentifier()} to build a URN and {@code getId()} to know what to
     * insert between {@code "epsg-"} and the code.
     */
    @Test
    public void testAsSubtype() {
        final var identifier  = new NamedIdentifier(EPSG, "7019");
        final var identifiers = Set.<Identifier>of(identifier);
        final var object      = new AbstractDatum(properties(identifiers));
        final var gmlId       = validate(object, identifiers, "epsg-datum-7019");
        assertNotNull(        gmlId);
        assertEquals ("EPSG", gmlId.getCodeSpace());
        assertEquals ("7019", gmlId.getCode());
    }

    /**
     * Tests two {@code AbstractIdentifiedObject} declaring the same identifier in the same XML document.
     * The {@code getID()} method should detect the collision and select different identifier.
     */
    @Test
    public void testIdentifierCollision() {
        final var properties = new HashMap<String,Object>(4);
        assertNull(properties.put("name", "GRS 1980"));
        assertNull(properties.put("identifiers", new NamedIdentifier(EPSG, "7019")));
        final var o1 = new AbstractIdentifiedObject(properties);
        final var o2 = new AbstractIdentifiedObject(properties);
        final var o3 = new AbstractIdentifiedObject(properties);
        final var o4 = new AbstractIdentifiedObject(properties);
        final var context = new Context(0, null, null, null, null, null, null, null, null, null, null);
        try {
            final String c1, c2, c3, c4;
            assertEquals("epsg-7019", c1 = o1.getID());
            assertEquals("GRS1980",   c2 = o2.getID());
            assertEquals("GRS1980-1", c3 = o3.getID());
            assertEquals("GRS1980-2", c4 = o4.getID());
            assertSame  (c1, o1.getID());  // Verify that values are remembered.
            assertSame  (c2, o2.getID());
            assertSame  (c3, o3.getID());
            assertSame  (c4, o4.getID());
        } finally {
            context.finish();
        }
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final var identifiers = Set.<Identifier>of();
        final var object = new AbstractIdentifiedObject(properties(identifiers));
        final var actual = assertSerializedEquals(object);
        assertNotSame(object, actual);
        assertNull(validate(actual, identifiers, "GRS1980"), "gmlId");
    }
}
