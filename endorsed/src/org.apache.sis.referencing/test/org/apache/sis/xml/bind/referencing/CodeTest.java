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
package org.apache.sis.xml.bind.referencing;

import java.util.Set;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.metadata.simple.SimpleCitation;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Tests {@link Code}, which is used by {@link RS_Identifier}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CodeTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CodeTest() {
    }

    /**
     * Tests the {@link Code#Code(ReferenceIdentifier)} constructor with {@code "EPSG:4326"} identifier.
     * This test intentionally uses an identifier with the {@code IOGP} authority instead of
     * EPSG in order to make sure that the {@code codeSpace} attribute is set from
     * {@code Identifier.getCodeSpace()}, not from {@code Identifier.getAuthority()}.
     */
    @Test
    public void testSimple() {
        final var IOGP  = new SimpleCitation("IOGP");
        final var id    = new ImmutableIdentifier(IOGP, "EPSG", "4326");  // See above javadoc.
        final var value = new Code(id);
        assertEquals("EPSG", value.codeSpace);
        assertEquals("4326", value.code);
        /*
         * Reverse operation. Note that the authority is lost since there is no room for that in a
         * <gml:identifier> element. Current implementation sets the authority to the code space.
         */
        final var actual = value.getIdentifier();
        assertSame  (Citations.EPSG, actual.getAuthority());
        assertEquals("EPSG", actual.getCodeSpace());
        assertNull  (        actual.getVersion());
        assertEquals("4326", actual.getCode());
    }

    /**
     * Tests the {@link Code#Code(ReferenceIdentifier)} constructor with {@code "EPSG:8.3:4326"} identifier.
     * This test intentionally uses an identifier with the {@code IOGP} authority instead of EPSG
     * for the same reason as {@link #testSimple()}.
     */
    @Test
    public void testWithVersion() {
        final var IOGP  = new SimpleCitation("IOGP");
        final var id    = new ImmutableIdentifier(IOGP, "EPSG", "4326", "8.2", null);  // See above javadoc.
        final var value = new Code(id);
        assertEquals("EPSG:8.2", value.codeSpace);
        assertEquals("4326",     value.code);
        /*
         * Reverse operation. Note that the authority is lost since there is no room for that in a
         * <gml:identifier> element. Current implementation sets the authority to the code space.
         */
        final var actual = value.getIdentifier();
        assertSame  (Citations.EPSG, actual.getAuthority());
        assertEquals("EPSG", actual.getCodeSpace());
        assertEquals("8.2",  actual.getVersion());
        assertEquals("4326", actual.getCode());
    }

    /**
     * Tests {@link Code#forIdentifiedObject(Class, Iterable)}.
     */
    @Test
    public void testForIdentifiedObject() {
        final var id = new ImmutableIdentifier(Citations.EPSG, "EPSG", "4326", "8.2", null);
        final var value = Code.forIdentifiedObject(GeographicCRS.class, Set.of(id));
        assertNotNull(value);
        assertEquals(Constants.IOGP, value.codeSpace);
        assertEquals("urn:ogc:def:crs:EPSG:8.2:4326", value.code);
    }

    /**
     * Tests {@link Code#forIdentifiedObject(Class, Iterable)} with the legacy "OGP" codespace
     * (instead of "IOGP").
     */
    @Test
    public void testLegacyCodeSpace() {
        final var authority = new DefaultCitation("EPSG");
        authority.getIdentifiers().add(new ImmutableIdentifier(null, "OGP", "EPSG"));

        final var id = new ImmutableIdentifier(authority, "EPSG", "4326", "8.2", null);
        final var value = Code.forIdentifiedObject(GeographicCRS.class, Set.of(id));
        assertNotNull(value);
        assertEquals("OGP", value.codeSpace);
        assertEquals("urn:ogc:def:crs:EPSG:8.2:4326", value.code);
    }

    /**
     * Tests {@link Code#getIdentifier()} with {@code "urn:ogc:def:crs:EPSG:8.2:4326"}.
     * This test simulates the {@code Code} object state that we get after XML unmarshalling
     * of an object from the EPSG repository.
     */
    @Test
    public void testGetIdentifier() {
        final var value = new Code();
        value.codeSpace = "OGP";
        value.code = "urn:ogc:def:crs:EPSG:8.2:4326";
        final var actual = value.getIdentifier();
        assertSame  (Citations.EPSG, actual.getAuthority());
        assertEquals("EPSG", actual.getCodeSpace());
        assertEquals("8.2",  actual.getVersion());
        assertEquals("4326", actual.getCode());
    }
}
