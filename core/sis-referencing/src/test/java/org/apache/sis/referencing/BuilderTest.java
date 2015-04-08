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
import org.opengis.util.NameSpace;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.Identifier;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.metadata.iso.citation.HardCodedCitations.*;


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
        builder.setCodeSpace(IOGP, "EPSG");
        builder.addName("Mercator (variant A)");
        /*
         * Setting the same codespace should have no effect, while attempt to
         * set a new codespace after we added a name shall not be allowed.
         */
        builder.setCodeSpace(IOGP, "EPSG");
        try {
            builder.setCodeSpace(EPSG, "EPSG");
            fail("Setting a different codespace shall not be allowed.");
        } catch (IllegalStateException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains(Identifier.AUTHORITY_KEY));
        }
        /*
         * The failed attempt to set a new codespace shall not have modified builder state.
         */
        assertEquals("EPSG", builder.properties.get(Identifier.CODESPACE_KEY));
        assertSame  (IOGP,   builder.properties.get(Identifier.AUTHORITY_KEY));
        /*
         * After a cleanup (normally after a createXXX(…) method call), user shall be allowed to
         * set a new codespace again. Note that the cleanup operation shall not clear the codespace.
         */
        builder.onCreate(true);
        assertEquals("EPSG", builder.properties.get(Identifier.CODESPACE_KEY));
        assertSame  (IOGP,   builder.properties.get(Identifier.AUTHORITY_KEY));
        builder.setCodeSpace(EPSG, "EPSG");
        assertEquals("EPSG", builder.properties.get(Identifier.CODESPACE_KEY));
        assertSame  ( EPSG,  builder.properties.get(Identifier.AUTHORITY_KEY));
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
        assertSame(builder, builder.setCodeSpace(IOGP, "EPSG"));
        if (withNames) {
            assertSame(builder, builder.addName(         "Mercator (variant A)")); // EPSG version 7.6 and later.
            assertSame(builder, builder.addName(         "Mercator (1SP)"));       // EPSG before version 7.6.
            assertSame(builder, builder.addName(OGC,     "Mercator_1SP"));
            assertSame(builder, builder.addName(GEOTIFF, "CT_Mercator"));
        }
        if (withIdentifiers) {
            assertSame(builder, builder.addIdentifier(      "9804"));
            assertSame(builder, builder.addIdentifier(GEOTIFF, "7"));
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
        final GenericName alias2 = new NamedIdentifier(OGC,     "Mercator_1SP");
        final GenericName alias3 = new NamedIdentifier(GEOTIFF, "CT_Mercator");
        assertEquals("Mercator (1SP)",       alias1.toString());
        assertEquals("OGC:Mercator_1SP",     alias2.toString());
        assertEquals("GeoTIFF:CT_Mercator",  alias3.toString());
        assertEquals("EPSG",                 alias1.scope().name().toString());

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
     * Tests {@link Builder#addIdentifier(Citation, String)} and {@link Builder#addIdentifier(String)}
     * with codespace.
     */
    @Test
    public void testAddIdentifiers() {
        // Expected values to be used later in the test.
        final Identifier id1 = new ImmutableIdentifier(IOGP,     "EPSG",    "9804");
        final Identifier id2 = new ImmutableIdentifier(GEOTIFF, "GeoTIFF", "7");
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
            switch (entry.getKey()) {
                case Identifier.AUTHORITY_KEY: {
                    assertSame("Authority and codespace shall be unchanged.", IOGP, value);
                    break;
                }
                case Identifier.CODESPACE_KEY: {
                    assertEquals("Authority and codespace shall be unchanged.", "EPSG", value);
                    break;
                }
                default: {
                    assertNull("Should not contain any non-null value except the authority.", value);
                    break;
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
        assertSame(builder, builder.rename(OGC, "Replacement 1", "Replacement 2"));
        builder.onCreate(false);
        assertArrayEquals(new String[] {
            "Mercator (variant A)",
            "Mercator (1SP)",
            "OGC:Replacement 1",
            "OGC:Replacement 2",
            "GeoTIFF:CT_Mercator"
        }, builder.getAsStrings(1));

        // Replace "EPSG:Mercator (variant A)" and "(1SP)", and insert a new EPSG code as an alias.
        assertSame(builder, builder.rename(IOGP, "Replacement 3", "Replacement 4", "Replacement 5"));
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
        assertSame(builder, builder.rename(IOGP, (String[]) null));
        builder.onCreate(false);
        assertArrayEquals(new String[] {
            "OGC:Replacement 1",
            "OGC:Replacement 2",
            "GeoTIFF:CT_Mercator"
        }, builder.getAsStrings(1));
    }
}
