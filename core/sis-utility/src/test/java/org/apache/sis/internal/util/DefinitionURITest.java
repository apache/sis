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
package org.apache.sis.internal.util;

import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DefinitionURI}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
public final strictfp class DefinitionURITest extends TestCase {
    /**
     * Tests {@link DefinitionURI#parse(String)} on strings that should not be recognized as URN.
     */
    @Test
    public void testParseInvalid() {
        assertNull(DefinitionURI.parse("EPSG:4326"));
        assertNull(DefinitionURI.parse("EPSG::4326"));
        assertNull(DefinitionURI.parse("urn:ogcx:def:CRS:EPSG:8.2:4326"));
    }

    /**
     * Tests {@link DefinitionURI#parse(String)} on {@code "urn:ogc:def:crs:EPSG:8.2:4326"}.
     * This is a URN without parameters defined by EPSG. This test also puts some spaces for
     * testing the parser capability to ignore them.
     */
    @Test
    public void testParse() {
        final DefinitionURI parsed = DefinitionURI.parse(" urn:ogc:def: crs : EPSG: 8.2 :4326 ");
        assertNotNull("DefinitionURI", parsed);
        assertEquals ("isHTTP",    false,   parsed.isHTTP);
        assertEquals ("isGML",     false,   parsed.isGML);
        assertEquals ("type",      "crs",   parsed.type);
        assertEquals ("authority", "EPSG",  parsed.authority);
        assertEquals ("version",   "8.2",   parsed.version);
        assertEquals ("code",      "4326",  parsed.code);
        assertNull   ("parameters",         parsed.parameters);
        assertEquals ("toString()", "urn:ogc:def:crs:EPSG:8.2:4326", parsed.toString());
    }

    /**
     * Tests {@link DefinitionURI#parse(String)} on {@code "urn:ogc:def:crs:EPSG::4326"}.
     * This is a URN without version. This test also mixes lower and upper cases.
     */
    @Test
    public void testParseWithoutVersion() {
        final DefinitionURI parsed = DefinitionURI.parse("URN :X-OGC: Def:crs:EPSG::4326");
        assertNotNull("DefinitionURI", parsed);
        assertEquals ("isHTTP",    false,   parsed.isHTTP);
        assertEquals ("isGML",     false,   parsed.isGML);
        assertEquals ("type",      "crs",   parsed.type);
        assertEquals ("authority", "EPSG",  parsed.authority);
        assertNull   ("version",            parsed.version);
        assertEquals ("code",      "4326",  parsed.code);
        assertNull   ("parameters",         parsed.parameters);
        assertEquals ("toString()", "urn:ogc:def:crs:EPSG::4326", parsed.toString());
    }

    /**
     * Tests {@link DefinitionURI#parse(String)} on {@code "urn:ogc:def:crs:OGC:1.3:AUTO42003:1:-100:45"}.
     * This is a URN with parameters defined in WMS specification.
     */
    @Test
    @DependsOnMethod("testParse")
    public void testParseWithParameters() {
        final DefinitionURI parsed = DefinitionURI.parse("urn:ogc:def:crs:OGC:1.3:AUTO42003:1:-100:45");
        assertNotNull("DefinitionURI", parsed);
        assertEquals ("isHTTP",    false,       parsed.isHTTP);
        assertEquals ("isGML",     false,       parsed.isGML);
        assertEquals ("type",      "crs",       parsed.type);
        assertEquals ("authority", "OGC",       parsed.authority);
        assertEquals ("version",   "1.3",       parsed.version);
        assertEquals ("code",      "AUTO42003", parsed.code);
        assertNotNull("parameters",             parsed.parameters);
        assertArrayEquals("parameters", new String[] {"1", "-100", "45"}, parsed.parameters);
        assertEquals("toString()", "urn:ogc:def:crs:OGC:1.3:AUTO42003:1:-100:45", parsed.toString());
    }

    /**
     * Tests {@link DefinitionURI#parse(String)} on {@code "http://www.opengis.net/def/crs/epsg/0/4326"}.
     */
    @Test
    @DependsOnMethod("testParse")
    public void testParseHTTP() {
        final DefinitionURI parsed = DefinitionURI.parse("http://www.opengis.net/def/crs/epsg/0/4326");
        assertNotNull("DefinitionURI", parsed);
        assertEquals ("isHTTP",    true,   parsed.isHTTP);
        assertEquals ("isGML",     false,  parsed.isGML);
        assertEquals ("type",      "crs",  parsed.type);
        assertEquals ("authority", "epsg", parsed.authority);
        assertNull   ("version",           parsed.version);
        assertEquals ("code",      "4326", parsed.code);
        assertNull   ("parameters",        parsed.parameters);
        assertEquals ("toString()", "http://www.opengis.net/def/crs/epsg/0/4326", parsed.toString());
    }

    /**
     * Tests {@link DefinitionURI#parse(String)} on {@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}.
     */
    @Test
    @DependsOnMethod("testParse")
    public void testParseGML() {
        final DefinitionURI parsed = DefinitionURI.parse("http://www.opengis.net/gml/srs/epsg.xml#4326");
        assertNotNull("DefinitionURI", parsed);
        assertEquals ("isHTTP",    true,   parsed.isHTTP);
        assertEquals ("isGML",     true,   parsed.isGML);
        assertEquals ("type",      "crs",  parsed.type);
        assertEquals ("authority", "epsg", parsed.authority);
        assertNull   ("version",           parsed.version);
        assertEquals ("code",      "4326", parsed.code);
        assertNull   ("parameters",        parsed.parameters);
        assertEquals ("toString()", "http://www.opengis.net/gml/srs/epsg.xml#4326", parsed.toString());

        final DefinitionURI withoutExtension = DefinitionURI.parse("http://www.opengis.net/gml/srs/epsg#4326");
        assertNotNull("Should parse even if the .xml extension is missig.", withoutExtension);
        assertEquals(parsed.toString(), withoutExtension.toString());

        assertNull("Should not parse if no '#' character.",
                DefinitionURI.parse("http://www.opengis.net/gml/srs/epsg?4326"));
    }

    /**
     * Tests {@link DefinitionURI#codeOf(String, String, String)} with URI like {@code "EPSG:4326"}.
     */
    @Test
    public void testCodeOfEPSG() {
        assertEquals("4326", DefinitionURI.codeOf("crs", "EPSG", "4326"));
        assertEquals("4326", DefinitionURI.codeOf("crs", "EPSG", "EPSG:4326"));
        assertEquals("4326", DefinitionURI.codeOf("crs", "EPSG", "EPSG::4326"));
        assertNull  (        DefinitionURI.codeOf("crs", "EPSG", "EPSG:::4326"));
        assertEquals("4326", DefinitionURI.codeOf("crs", "EPSG", "EPSG:8.2:4326"));
        assertEquals("4326", DefinitionURI.codeOf("crs", "EPSG", " epsg : 4326 "));
        assertEquals("4326", DefinitionURI.codeOf("crs", "EPSG", " epsg :: 4326 "));
        assertEquals("4326", DefinitionURI.codeOf("crs", "EPSG", " epsg : : 4326 "));
    }

    /**
     * Tests {@link DefinitionURI#codeOf(String, String, String)} with URN like
     * {@code "urn:ogc:def:crs:EPSG::4326"}.
     */
    @Test
    public void testCodeOfURN() {
        assertEquals("4326",  DefinitionURI.codeOf("crs", "EPSG", "urn:ogc:def:crs:EPSG:4326"));
        assertEquals("4326",  DefinitionURI.codeOf("crs", "EPSG", "urn:ogc:def:crs:EPSG::4326"));
        assertEquals("4326",  DefinitionURI.codeOf("crs", "EPSG", "urn:ogc:def:crs:EPSG:8.2:4326"));
        assertEquals("4326",  DefinitionURI.codeOf("crs", "EPSG", "urn:x-ogc:def:crs:EPSG::4326"));
        assertNull  (         DefinitionURI.codeOf("crs", "EPSG", "urn:n-ogc:def:crs:EPSG::4326"));
        assertEquals("4326",  DefinitionURI.codeOf("crs", "EPSG", " urn : ogc : def : crs : epsg : : 4326"));
        assertNull  (         DefinitionURI.codeOf("crs", "EPSG", "urn:ogc:def:uom:EPSG:9102"));
        assertEquals("9102",  DefinitionURI.codeOf("uom", "EPSG", "urn:ogc:def:uom:EPSG:9102"));
        assertNull  (         DefinitionURI.codeOf("crs", "EPSG", "urn:ogc:def:crs:OGC:1.3:CRS84"));
        assertEquals("CRS84", DefinitionURI.codeOf("crs", "OGC",  "urn:ogc:def:crs:OGC:1.3:CRS84"));
        assertNull  (         DefinitionURI.codeOf("crs", "OGC",  "urn:ogc:def:crs:OGC:1.3:AUTO42003:1:-100:45"));
    }

    /**
     * Tests {@link DefinitionURI#codeOf(String, String, String)} with URL like
     * {@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}.
     */
    @Test
    public void testCodeOfGML() {
        assertEquals("4326", DefinitionURI.codeOf("crs", "EPSG", "http://www.opengis.net/gml/srs/epsg.xml#4326"));
        assertNull  (        DefinitionURI.codeOf("crs", "OGC",  "http://www.opengis.net/gml/srs/epsg.xml#4326"));
        assertNull  (        DefinitionURI.codeOf("uom", "EPSG", "http://www.opengis.net/gml/srs/epsg.xml#4326"));
        assertNull  (        DefinitionURI.codeOf("uom", "EPSG", "http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"));
    }

    /**
     * Tests {@link DefinitionURI#format(String, ReferenceIdentifier)}.
     */
    @Test
    public void testToURN() {
        assertEquals("urn:ogc:def:crs:EPSG::4326", DefinitionURI.format("crs", "EPSG", null, "4326"));
        assertNull  ("Authority is not optional.", DefinitionURI.format("crs", null,   null, "4326"));
    }
}
