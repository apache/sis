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
import org.apache.sis.util.internal.Constants;
import org.apache.sis.metadata.simple.SimpleCitation;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;

// Test dependencies
import org.junit.Test;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.Identifier;


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
     * Tests the {@link Code#Code(Identifier)} constructor with {@code "EPSG:4326"} identifier.
     * This test intentionally uses an identifier with the {@code IOGP} authority instead of
     * EPSG in order to make sure that the {@code codeSpace} attribute is set from
     * {@link Identifier#getCodeSpace()}, not from {@link Identifier#getAuthority()}.
     */
    @Test
    public void testSimple() {
        final SimpleCitation IOGP = new SimpleCitation("IOGP");
        final Identifier id = new ImmutableIdentifier(IOGP, "EPSG", "4326");  // See above javadoc.
        final Code value = new Code(id);
        assertEquals("codeSpace", "EPSG", value.codeSpace);
        assertEquals("code",      "4326", value.code);
        /*
         * Reverse operation. Note that the authority is lost since there is no room for that in a
         * <gml:identifier> element. Current implementation sets the authority to the code space.
         */
        final Identifier actual = value.getIdentifier();
        assertSame  ("authority",  Citations.EPSG, actual.getAuthority());
        assertEquals("codeSpace", "EPSG", actual.getCodeSpace());
        assertNull  ("version",           actual.getVersion());
        assertEquals("code",      "4326", actual.getCode());
    }

    /**
     * Tests the {@link Code#Code(Identifier)} constructor with {@code "EPSG:8.3:4326"} identifier.
     * This test intentionally uses an identifier with the {@code IOGP} authority instead of EPSG
     * for the same reason than {@link #testSimple()}.
     */
    @Test
    @DependsOnMethod("testSimple")
    public void testWithVersion() {
        final SimpleCitation IOGP = new SimpleCitation("IOGP");
        final Identifier id = new ImmutableIdentifier(IOGP, "EPSG", "4326", "8.2", null);  // See above javadoc.
        final Code value = new Code(id);
        assertEquals("codeSpace", "EPSG:8.2", value.codeSpace);
        assertEquals("code",      "4326",     value.code);
        /*
         * Reverse operation. Note that the authority is lost since there is no room for that in a
         * <gml:identifier> element. Current implementation sets the authority to the code space.
         */
        final Identifier actual = value.getIdentifier();
        assertSame  ("authority",  Citations.EPSG, actual.getAuthority());
        assertEquals("codeSpace", "EPSG", actual.getCodeSpace());
        assertEquals("version",   "8.2",  actual.getVersion());
        assertEquals("code",      "4326", actual.getCode());
    }

    /**
     * Tests {@link Code#forIdentifiedObject(Class, Iterable)}.
     */
    @Test
    @DependsOnMethod("testWithVersion")
    public void testForIdentifiedObject() {
        final Identifier id = new ImmutableIdentifier(Citations.EPSG, "EPSG", "4326", "8.2", null);
        final Code value = Code.forIdentifiedObject(GeographicCRS.class, Set.of(id));
        assertNotNull(value);
        assertEquals("codeSpace", Constants.IOGP, value.codeSpace);
        assertEquals("code", "urn:ogc:def:crs:EPSG:8.2:4326", value.code);
    }

    /**
     * Tests {@link Code#forIdentifiedObject(Class, Iterable)} with the legacy "OGP" codespace
     * (instead of "IOGP").
     */
    @Test
    @DependsOnMethod("testForIdentifiedObject")
    public void testLegacyCodeSpace() {
        final DefaultCitation authority = new DefaultCitation("EPSG");
        authority.getIdentifiers().add(new ImmutableIdentifier(null, "OGP", "EPSG"));

        final Identifier id = new ImmutableIdentifier(authority, "EPSG", "4326", "8.2", null);
        final Code value = Code.forIdentifiedObject(GeographicCRS.class, Set.of(id));
        assertNotNull(value);
        assertEquals("codeSpace", "OGP", value.codeSpace);
        assertEquals("code", "urn:ogc:def:crs:EPSG:8.2:4326", value.code);
    }

    /**
     * Tests {@link Code#getIdentifier()} with {@code "urn:ogc:def:crs:EPSG:8.2:4326"}.
     * This test simulates the {@code Code} object state that we get after XML unmarshalling
     * of an object from the EPSG repository.
     */
    @Test
    @DependsOnMethod("testForIdentifiedObject")
    public void testGetIdentifier() {
        final Code value = new Code();
        value.codeSpace = "OGP";
        value.code = "urn:ogc:def:crs:EPSG:8.2:4326";
        final Identifier actual = value.getIdentifier();
        assertSame  ("authority",  Citations.EPSG, actual.getAuthority());
        assertEquals("codeSpace", "EPSG", actual.getCodeSpace());
        assertEquals("version",   "8.2",  actual.getVersion());
        assertEquals("code",      "4326", actual.getCode());
    }
}