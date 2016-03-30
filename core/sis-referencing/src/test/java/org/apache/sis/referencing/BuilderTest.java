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
import org.opengis.util.NameSpace;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.internal.simple.SimpleIdentifier;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Builder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@DependsOn(AbstractIdentifiedObjectTest.class)
public final strictfp class BuilderTest extends TestCase {
    /**
     * Tests {@link Builder#verifyParameterizedType(Class)}.
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testVerifyParameterizedType() {
        final class Invalid extends Builder<BuilderMock> {
        }
        try {
            new Invalid();
            fail("Creation of Invalid builder shall not be allowed.");
        } catch (AssertionError e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains(BuilderMock.class.getName()));
        }
    }

    /**
     * Tests {@link Builder#setCodeSpace(Citation, String)}.
     */
    @Test
    public void testSetCodeSpace() {
        final BuilderMock builder = new BuilderMock();
        builder.setCodeSpace(Citations.EPSG, "EPSG");
        builder.addName("Mercator (variant A)");
        /*
         * Setting the same codespace should have no effect, while attempt to
         * set a new codespace after we added a name shall not be allowed.
         */
        final SimpleCitation IOGP = new SimpleCitation("IOGP");
        builder.setCodeSpace(Citations.EPSG, "EPSG");
        try {
            builder.setCodeSpace(IOGP, "EPSG");
            fail("Setting a different codespace shall not be allowed.");
        } catch (IllegalStateException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains(Identifier.AUTHORITY_KEY));
        }
        /*
         * The failed attempt to set a new codespace shall not have modified builder state.
         */
        assertEquals("EPSG",         builder.properties.get(ReferenceIdentifier.CODESPACE_KEY));
        assertSame  (Citations.EPSG, builder.properties.get(Identifier.AUTHORITY_KEY));
        /*
         * After a cleanup (normally after a createXXX(…) method call), user shall be allowed to
         * set a new codespace again. Note that the cleanup operation shall not clear the codespace.
         */
        builder.onCreate(true);
        assertEquals("EPSG",         builder.properties.get(ReferenceIdentifier.CODESPACE_KEY));
        assertSame  (Citations.EPSG, builder.properties.get(Identifier.AUTHORITY_KEY));
        builder.setCodeSpace(IOGP, "EPSG");
        assertEquals("EPSG", builder.properties.get(ReferenceIdentifier.CODESPACE_KEY));
        assertSame  ( IOGP,  builder.properties.get(Identifier.AUTHORITY_KEY));
    }

    /**
     * Tests {@link Builder#addName(CharSequence)} without codespace.
     */
    @Test
    public void testAddName() {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);

        // Expected values to be used later in the test.
        final String    name   = "Mercator (variant A)";
        final LocalName alias1 = factory.createLocalName(null, "Mercator (1SP)");
        final LocalName alias2 = factory.createLocalName(null, "Mercator_1SP");
        final LocalName alias3 = factory.createLocalName(null, "CT_Mercator");
        assertTrue("That name should not have a scope.", alias1.scope().isGlobal());
        assertTrue("That name should not have a scope.", alias2.scope().isGlobal());
        assertTrue("That name should not have a scope.", alias3.scope().isGlobal());
        assertEquals("Mercator (1SP)", alias1.toString());
        assertEquals("Mercator_1SP",   alias2.toString());
        assertEquals("CT_Mercator",    alias3.toString());

        // The test.
        final BuilderMock builder = new BuilderMock();
        assertSame(builder, builder.addName("Mercator (variant A)"));   // EPSG version 7.6 and later.
        assertSame(builder, builder.addName("Mercator (1SP)"));         // EPSG before version 7.6.
        assertSame(builder, builder.addName("Mercator_1SP"));           // OGC
        assertSame(builder, builder.addName("CT_Mercator"));            // GeoTIFF
        builder.onCreate(false);
        assertEquals(name, builder.getName());
        assertArrayEquals(new GenericName[] {alias1, alias2, alias3}, builder.getAliases());
    }

    /**
     * Creates a {@link Builder} with <cite>"Mercator (variant A)"</cite> projection (EPSG:9804) names and identifiers.
     * This method uses scopes for differentiating the EPSG names from the OGC and GeoTIFF ones.
     *
     * @param  withNames       {@code true} for adding the names in the builder.
     * @param  withIdentifiers {@code true} for adding the identifiers in the builder.
     * @return The builder with Mercator names and/or identifiers.
     */
    private static BuilderMock createMercator(final boolean withNames, final boolean withIdentifiers) {
        final BuilderMock builder = new BuilderMock();
        assertSame(builder, builder.setCodeSpace(Citations.EPSG, "EPSG"));
        if (withNames) {
            assertSame(builder, builder.addName(                   "Mercator (variant A)")); // EPSG version 7.6 and later.
            assertSame(builder, builder.addName(                   "Mercator (1SP)"));       // EPSG before version 7.6.
            assertSame(builder, builder.addName(Citations.OGC,     "Mercator_1SP"));
            assertSame(builder, builder.addName(Citations.GEOTIFF, "CT_Mercator"));
        }
        if (withIdentifiers) {
            assertSame(builder, builder.addIdentifier(                "9804"));
            assertSame(builder, builder.addIdentifier(Citations.GEOTIFF, "7"));
        }
        builder.onCreate(false);
        return builder;
    }

    /**
     * Tests {@link Builder#addName(Citation, CharSequence)} and {@link Builder#addName(CharSequence)} with codespace.
     */
    @Test
    @DependsOnMethod({"testAddName", "testSetCodeSpace"})
    public void testAddNameWithScope() {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);

        // Expected values to be used later in the test.
        final String      name   = "Mercator (variant A)";
        final GenericName alias1 = factory.createLocalName(scope(factory, "EPSG"), "Mercator (1SP)");
        final GenericName alias2 = new NamedIdentifier(Citations.OGC,     "Mercator_1SP");
        final GenericName alias3 = new NamedIdentifier(Citations.GEOTIFF, "CT_Mercator");
        assertTrue ("That name should not have a scope.", alias3.scope().isGlobal());
        assertTrue ("That name should not have a scope.", alias2.scope().isGlobal());
        assertFalse("That name should be in EPSG scope.", alias1.scope().isGlobal());
        assertEquals("EPSG",                 alias1.scope().name().toString());
        assertEquals("Mercator (1SP)",       alias1.toString());
        assertEquals("OGC:Mercator_1SP",     alias2.toString());
        assertEquals("GeoTIFF:CT_Mercator",  alias3.toString());

        // The test.
        final BuilderMock builder = createMercator(true, false);
        assertEquals(name, builder.getName());
        assertArrayEquals(new GenericName[] {alias1, alias2, alias3}, builder.getAliases());
    }

    /**
     * Convenience method creating a namespace for {@link #testScopedName()} purpose.
     */
    private static NameSpace scope(final NameFactory factory, final String codespace) {
        return factory.createNameSpace(factory.createLocalName(null, codespace), null);
    }

    /**
     * Tests {@link Builder#addIdentifier(Citation, String)} and {@link Builder#addIdentifier(String)} with code space.
     */
    @Test
    public void testAddIdentifiers() {
        // Expected values to be used later in the test.
        final Identifier id1 = new ImmutableIdentifier(Citations.EPSG,    "EPSG", "9804");
        final Identifier id2 = new ImmutableIdentifier(Citations.GEOTIFF, "GeoTIFF", "7");
        assertEquals("EPSG:9804", IdentifiedObjects.toString(id1));
        assertEquals("GeoTIFF:7", IdentifiedObjects.toString(id2));

        // The test.
        final BuilderMock builder = createMercator(false, true);
        assertArrayEquals(new Identifier[] {id1, id2}, builder.getIdentifiers());
    }

    /**
     * Tests {@link Builder#addNamesAndIdentifiers(IdentifiedObject)}.
     *
     * @since 0.6
     */
    @Test
    @DependsOnMethod({"testAddNameWithScope", "testAddIdentifiers"})
    public void testAddNamesAndIdentifiers() {
        final BuilderMock builder = createMercator(true, true);
        final AbstractIdentifiedObject object = new AbstractIdentifiedObject(builder.properties);
        builder.onCreate(true);
        for (final Map.Entry<String,?> entry : builder.properties.entrySet()) {
            final Object value = entry.getValue();
            final String key = entry.getKey();
            { // This is a switch(String) in the JDK7 branch.
                if (key.equals(Identifier.AUTHORITY_KEY)) {
                    assertSame("Authority and codespace shall be unchanged.", Citations.EPSG, value);
                } else if (key.equals(ReferenceIdentifier.CODESPACE_KEY)) {
                    assertEquals("Authority and codespace shall be unchanged.", "EPSG", value);
                } else {
                    assertNull("Should not contain any non-null value except the authority.", value);
                }
            }
        }
        assertSame(builder, builder.addNamesAndIdentifiers(object));
        builder.onCreate(false);
        assertSame       ("name",        object.getName(),                  builder.getName());
        assertArrayEquals("aliases",     object.getAlias().toArray(),       builder.getAliases());
        assertArrayEquals("identifiers", object.getIdentifiers().toArray(), builder.getIdentifiers());
    }

    /**
     * Tests {@link Builder#rename(Citation, CharSequence[])}.
     *
     * @since 0.6
     */
    @Test
    @DependsOnMethod("testAddNamesAndIdentifiers")
    public void testRename() {
        final BuilderMock builder = createMercator(true, false);

        // Replace "OGC:Mercator_1SP" and insert a new OGC code before the GeoTIFF one.
        assertSame(builder, builder.rename(Citations.OGC, "Replacement 1", "Replacement 2"));
        builder.onCreate(false);
        assertArrayEquals(new String[] {
            "Mercator (variant A)",
            "Mercator (1SP)",
            "OGC:Replacement 1",
            "OGC:Replacement 2",
            "GeoTIFF:CT_Mercator"
        }, builder.getAsStrings(1));

        // Replace "EPSG:Mercator (variant A)" and "(1SP)", and insert a new EPSG code as an alias.
        assertSame(builder, builder.rename(Citations.EPSG, "Replacement 3", "Replacement 4", "Replacement 5"));
        builder.onCreate(false);
        assertArrayEquals(new String[] {
            "Replacement 3",
            "Replacement 4",
            "Replacement 5",
            "OGC:Replacement 1",
            "OGC:Replacement 2",
            "GeoTIFF:CT_Mercator"
        }, builder.getAsStrings(1));

        // Remove all EPSG codes.
        assertSame(builder, builder.rename(Citations.EPSG, (String[]) null));
        builder.onCreate(false);
        assertArrayEquals(new String[] {
            "OGC:Replacement 1",
            "OGC:Replacement 2",
            "GeoTIFF:CT_Mercator"
        }, builder.getAsStrings(1));
    }

    /**
     * Tests the {@link Builder#Builder(IdentifiedObject)} constructor.
     *
     * @since 0.6
     */
    @Test
    public void testCreationFromObject() {
        final Map<String,Object> properties = new HashMap<String,Object>();
        final Identifier id = new SimpleIdentifier(null, "An identifier", false);
        assertNull(properties.put(AbstractIdentifiedObject.IDENTIFIERS_KEY, id));
        assertNull(properties.put(AbstractIdentifiedObject.ALIAS_KEY,       "An alias"));
        assertNull(properties.put(AbstractIdentifiedObject.NAME_KEY,        "Dummy object"));
        assertNull(properties.put(AbstractIdentifiedObject.REMARKS_KEY,     "Some remarks"));
        final BuilderMock builder = new BuilderMock(new AbstractIdentifiedObject(properties));

        assertEquals("Expected only name, remarks and deprecated status.", 3, builder.properties.size());
        builder.onCreate(false);
        assertEquals("Expected name, aliases, identifiers and remarks.", 5, builder.properties.size());

        assertEquals(AbstractIdentifiedObject.NAME_KEY, "Dummy object",
                builder.properties.get(AbstractIdentifiedObject.NAME_KEY).toString());

        assertEquals(AbstractIdentifiedObject.REMARKS_KEY, "Some remarks",
                builder.properties.get(AbstractIdentifiedObject.REMARKS_KEY).toString());

        assertEquals(AbstractIdentifiedObject.ALIAS_KEY, "An alias",
                ((Object[]) builder.properties.get(AbstractIdentifiedObject.ALIAS_KEY))[0].toString());

        assertSame(AbstractIdentifiedObject.IDENTIFIERS_KEY, id,
                ((Object[]) builder.properties.get(AbstractIdentifiedObject.IDENTIFIERS_KEY))[0]);
    }
}
