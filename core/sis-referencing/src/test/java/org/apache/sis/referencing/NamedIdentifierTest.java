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
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.InternationalString;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.test.Validators;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link NamedIdentifier} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
public final strictfp class NamedIdentifierTest extends TestCase {
    /**
     * Tests the {@link NamedIdentifier#NamedIdentifier(Citation, String, CharSequence, String, InternationalString)}
     * constructor.
     */
    @Test
    public void testCreateFromCode() {
        final NamedIdentifier identifier = new NamedIdentifier(Citations.EPSG, "EPSG", "4326", "8.3", null);
        Validators.validate((ReferenceIdentifier) identifier);
        Validators.validate((GenericName) identifier);

        // ImmutableIdentifier properties
        assertEquals("code",       "4326",         identifier.getCode());
        assertEquals("codeSpace",  "EPSG",         identifier.getCodeSpace());
        assertSame  ("authority",  Citations.EPSG, identifier.getAuthority());
        assertEquals("version",    "8.3",          identifier.getVersion());
        assertNull  ("description",                identifier.getDescription());

        // NamedIdentifier properties
        assertEquals("depth",  2,          identifier.depth());
        assertEquals("tip",   "4326",      identifier.tip().toString());
        assertEquals("head",  "EPSG",      identifier.head().toString());
        assertEquals("name",  "EPSG:4326", identifier.toString());
        assertTrue  ("scope.isGlobal",     identifier.scope().isGlobal());
    }

    /**
     * Tests the {@link NamedIdentifier#NamedIdentifier(GenericName)} constructor.
     */
    @Test
    public void testCreateFromName() {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        final NameSpace scope = factory.createNameSpace(factory.createLocalName(null, "IOGP"), null);
        final NamedIdentifier identifier = new NamedIdentifier(factory.createGenericName(scope, "EPSG", "4326"));
        Validators.validate((ReferenceIdentifier) identifier);
        Validators.validate((GenericName) identifier);

        // ImmutableIdentifier properties
        assertEquals("code",      "4326", identifier.getCode());
        assertEquals("codeSpace", "EPSG", identifier.getCodeSpace());
        assertEquals("authority", "IOGP", Citations.getIdentifier(identifier.getAuthority()));
        assertNull  ("version",           identifier.getVersion());
        assertNull  ("description",       identifier.getDescription());

        // NamedIdentifier properties
        assertEquals("depth",  2,          identifier.depth());
        assertEquals("tip",   "4326",      identifier.tip().toString());
        assertEquals("head",  "EPSG",      identifier.head().toString());
        assertEquals("name",  "EPSG:4326", identifier.toString());
        assertSame  ("scope", scope,       identifier.scope());
        assertFalse ("scope.isGlobal",     scope.isGlobal());
        assertEquals("scope", "IOGP",      scope.name().toString());
    }

    /**
     * Creates an internationalized name with a code set to "name" localized in English, French and Japanese.
     */
    private NamedIdentifier createI18N() {
        final DefaultInternationalString i18n = new DefaultInternationalString();
        i18n.add(Locale.ENGLISH,  "name");
        i18n.add(Locale.FRENCH,   "nom");
        i18n.add(Locale.JAPANESE, "名前");
        return new NamedIdentifier(Citations.EPSG, i18n);
    }

    /**
     * Tests the {@link NamedIdentifier#NamedIdentifier(Citation, InternationalString)} constructor.
     */
    @Test
    @DependsOnMethod("testCreateFromCode")
    public void testCreateFromInternationalString() {
        final NamedIdentifier identifier = createI18N();
        Validators.validate((ReferenceIdentifier) identifier);
        Validators.validate((GenericName) identifier);

        // ImmutableIdentifier properties
        assertEquals("code",      "name",         identifier.getCode());
        assertEquals("codeSpace", "EPSG",         identifier.getCodeSpace());
        assertSame  ("authority", Citations.EPSG, identifier.getAuthority());
        assertNull  ("version",                   identifier.getVersion());
        assertNull  ("description",               identifier.getDescription());

        // NamedIdentifier properties
        assertEquals("depth",  2,          identifier.depth());
        assertEquals("tip",   "name",      identifier.tip().toInternationalString().toString(Locale.ENGLISH));
        assertEquals("tip",   "nom",       identifier.tip().toInternationalString().toString(Locale.FRENCH));
        assertEquals("tip",   "名前",       identifier.tip().toInternationalString().toString(Locale.JAPANESE));
        assertEquals("head",  "EPSG",      identifier.head().toString());
        assertEquals("name",  "EPSG:name", identifier.toInternationalString().toString(Locale.ENGLISH));
        assertEquals("name",  "EPSG:nom",  identifier.toInternationalString().toString(Locale.FRENCH));
        assertEquals("name",  "EPSG:名前",  identifier.toInternationalString().toString(Locale.JAPANESE));
        assertTrue  ("scope.isGlobal",     identifier.scope().isGlobal());
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testCreateFromInternationalString")
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
