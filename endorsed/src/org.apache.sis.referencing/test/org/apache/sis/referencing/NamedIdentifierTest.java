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

import java.util.Locale;
import org.opengis.util.InternationalString;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.DefaultInternationalString;
import org.apache.sis.util.iso.DefaultNameFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Tests the {@link NamedIdentifier} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class NamedIdentifierTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public NamedIdentifierTest() {
    }

    /**
     * Tests the {@link NamedIdentifier#NamedIdentifier(Citation, String, CharSequence, String, InternationalString)}
     * constructor.
     */
    @Test
    public void testCreateFromCode() {
        final var identifier = new NamedIdentifier(Citations.EPSG, "EPSG", "4326", "8.3", null);
        Validators.validate((ReferenceIdentifier) identifier);
        Validators.validate((GenericName) identifier);

        // ImmutableIdentifier properties
        assertEquals("4326",         identifier.getCode());
        assertEquals("EPSG",         identifier.getCodeSpace());
        assertSame  (Citations.EPSG, identifier.getAuthority());
        assertEquals("8.3",          identifier.getVersion());
        assertNull  (                identifier.getDescription());

        // NamedIdentifier properties
        assertEquals( 2,          identifier.depth());
        assertEquals("4326",      identifier.tip().toString());
        assertEquals("EPSG",      identifier.head().toString());
        assertEquals("EPSG:4326", identifier.toString());
        assertTrue  (             identifier.scope().isGlobal());
    }

    /**
     * Tests the {@link NamedIdentifier#NamedIdentifier(GenericName)} constructor.
     */
    @Test
    public void testCreateFromName() {
        final NameFactory factory = DefaultNameFactory.provider();
        final NameSpace scope = factory.createNameSpace(factory.createLocalName(null, "IOGP"), null);
        final var identifier = new NamedIdentifier(factory.createGenericName(scope, "EPSG", "4326"));
        Validators.validate((ReferenceIdentifier) identifier);
        Validators.validate((GenericName) identifier);

        // ImmutableIdentifier properties
        assertEquals("4326", identifier.getCode());
        assertEquals("EPSG", identifier.getCodeSpace());
        assertEquals("IOGP", Citations.toCodeSpace(identifier.getAuthority()));
        assertNull  (        identifier.getVersion());
        assertNull  (        identifier.getDescription());

        // NamedIdentifier properties
        assertEquals( 2,          identifier.depth());
        assertEquals("4326",      identifier.tip().toString());
        assertEquals("EPSG",      identifier.head().toString());
        assertEquals("EPSG:4326", identifier.toString());
        assertSame  (scope,       identifier.scope());
        assertFalse (             scope.isGlobal());
        assertEquals("IOGP",      scope.name().toString());
    }

    /**
     * Creates an internationalized name with a code set to "name" localized in English, French and Japanese.
     */
    private NamedIdentifier createI18N() {
        final var i18n = new DefaultInternationalString();
        i18n.add(Locale.ENGLISH,  "name");
        i18n.add(Locale.FRENCH,   "nom");
        i18n.add(Locale.JAPANESE, "名前");
        return new NamedIdentifier(Citations.EPSG, i18n);
    }

    /**
     * Tests the {@link NamedIdentifier#NamedIdentifier(Citation, CharSequence)} constructor.
     */
    @Test
    public void testCreateFromInternationalString() {
        final NamedIdentifier identifier = createI18N();
        Validators.validate((ReferenceIdentifier) identifier);
        Validators.validate((GenericName) identifier);

        // ImmutableIdentifier properties
        assertEquals("name",         identifier.getCode());
        assertEquals("EPSG",         identifier.getCodeSpace());
        assertSame  (Citations.EPSG, identifier.getAuthority());
        assertNull  (                identifier.getVersion());
        assertNull  (                identifier.getDescription());

        // NamedIdentifier properties
        assertEquals( 2,          identifier.depth());
        assertEquals("name",      identifier.tip().toInternationalString().toString(Locale.ENGLISH));
        assertEquals("nom",       identifier.tip().toInternationalString().toString(Locale.FRENCH));
        assertEquals("名前",       identifier.tip().toInternationalString().toString(Locale.JAPANESE));
        assertEquals("EPSG",      identifier.head().toString());
        assertEquals("EPSG:name", identifier.toInternationalString().toString(Locale.ENGLISH));
        assertEquals("EPSG:nom",  identifier.toInternationalString().toString(Locale.FRENCH));
        assertEquals("EPSG:名前",  identifier.toInternationalString().toString(Locale.JAPANESE));
        assertTrue  (             identifier.scope().isGlobal());
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        NamedIdentifier unserial = assertSerializedEquals(new NamedIdentifier(Citations.EPSG, "4326"));
        assertEquals("EPSG:4326", unserial.toInternationalString().toString(Locale.ENGLISH));
        /*
         * Try again with an international string. We would not been able to get back the
         * localized strings if NamedIdentifier.writeObject/readObject(…) didn't worked.
         */
        unserial = assertSerializedEquals(createI18N());
        assertEquals("EPSG:name", unserial.toInternationalString().toString(Locale.ENGLISH));
        assertEquals("EPSG:nom",  unserial.toInternationalString().toString(Locale.FRENCH));
        assertEquals("EPSG:名前",  unserial.toInternationalString().toString(Locale.JAPANESE));
    }
}
