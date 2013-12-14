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

import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link URIParser}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class URIParserTest extends TestCase {
    /**
     * Tests {@link URIParser#parse(String)} on {@code "urn:ogc:def:crs:EPSG:8.2:4326"}.
     * This is a URN without parameters defined by EPSG.
     */
    @Test
    public void testParse() {
        assertNull(URIParser.parse("EPSG:4326"));

        URIParser parsed = URIParser.parse(" urn:ogc:def: crs : EPSG: 8.2 :4326 ");
        assertNotNull("URIParser", parsed);
        assertEquals ("isHTTP",    false,   parsed.isHTTP);
        assertEquals ("type",      "crs",   parsed.type);
        assertEquals ("authority", "EPSG",  parsed.authority);
        assertEquals ("version",   "8.2",   parsed.version);
        assertEquals ("code",      "4326",  parsed.code);
        assertNull   ("parameters",         parsed.parameters);
        assertEquals ("toString()", "urn:ogc:def:crs:EPSG:8.2:4326", parsed.toString());

        parsed = URIParser.parse("URN :X-OGC: Def:crs:EPSG::4326");
        assertNotNull("URIParser", parsed);
        assertEquals ("isHTTP",    false,   parsed.isHTTP);
        assertEquals ("type",      "crs",   parsed.type);
        assertEquals ("authority", "EPSG",  parsed.authority);
        assertNull   ("version",            parsed.version);
        assertEquals ("code",      "4326",  parsed.code);
        assertNull   ("parameters",         parsed.parameters);
        assertEquals ("toString()", "urn:ogc:def:crs:EPSG::4326", parsed.toString());
    }

    /**
     * Tests {@link URIParser#parse(String)} on {@code "urn:ogc:def:crs:OGC:1.3:AUTO42003:1:-100:45"}.
     * This is a URN with parameters defined in WMS specification.
     */
    @Test
    @DependsOnMethod("testParse")
    public void testParseWithParameters() {
        final URIParser parsed = URIParser.parse("urn:ogc:def:crs:OGC:1.3:AUTO42003:1:-100:45");
        assertNotNull("URIParser", parsed);
        assertEquals ("isHTTP",    false,       parsed.isHTTP);
        assertEquals ("type",      "crs",       parsed.type);
        assertEquals ("authority", "OGC",       parsed.authority);
        assertEquals ("version",   "1.3",       parsed.version);
        assertEquals ("code",      "AUTO42003", parsed.code);
        assertNotNull("parameters",             parsed.parameters);
        assertArrayEquals("parameters", new String[] {"1", "-100", "45"}, parsed.parameters);
        assertEquals("toString()", "urn:ogc:def:crs:OGC:1.3:AUTO42003:1:-100:45", parsed.toString());
    }

    /**
     * Tests {@link URIParser#parse(String)} on {@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}.
     */
    @Test
    @DependsOnMethod("testParse")
    public void testParseHTTP() {
        final URIParser parsed = URIParser.parse("http://www.opengis.net/gml/srs/epsg.xml#4326");
        assertNotNull("URIParser", parsed);
        assertEquals ("isHTTP",    true,   parsed.isHTTP);
        assertEquals ("type",      "crs",  parsed.type);
        assertEquals ("authority", "epsg", parsed.authority);
        assertNull   ("version",           parsed.version);
        assertEquals ("code",      "4326", parsed.code);
        assertNull   ("parameters",        parsed.parameters);
        assertEquals ("toString()", "http://www.opengis.net/gml/srs/epsg.xml#4326", parsed.toString());
    }

    /**
     * Tests {@link URIParser#codeOf(String, String, String)} with URI like {@code "EPSG:4326"}.
     */
    @Test
    public void testCodeOfEPSG() {
        assertEquals("4326", URIParser.codeOf("crs", "EPSG", "4326"));
        assertEquals("4326", URIParser.codeOf("crs", "EPSG", "EPSG:4326"));
        assertEquals("4326", URIParser.codeOf("crs", "EPSG", "EPSG::4326"));
        assertNull  (        URIParser.codeOf("crs", "EPSG", "EPSG:::4326"));
        assertEquals("4326", URIParser.codeOf("crs", "EPSG", "EPSG:8.2:4326"));
        assertEquals("4326", URIParser.codeOf("crs", "EPSG", " epsg : 4326 "));
        assertEquals("4326", URIParser.codeOf("crs", "EPSG", " epsg :: 4326 "));
        assertEquals("4326", URIParser.codeOf("crs", "EPSG", " epsg : : 4326 "));
    }

    /**
     * Tests {@link URIParser#codeOf(String, String, String)} with URN like
     * {@code "urn:ogc:def:crs:EPSG::4326"}.
     */
    @Test
    public void testCodeOfURN() {
        assertEquals("4326",  URIParser.codeOf("crs", "EPSG", "urn:ogc:def:crs:EPSG:4326"));
        assertEquals("4326",  URIParser.codeOf("crs", "EPSG", "urn:ogc:def:crs:EPSG::4326"));
        assertEquals("4326",  URIParser.codeOf("crs", "EPSG", "urn:ogc:def:crs:EPSG:8.2:4326"));
        assertEquals("4326",  URIParser.codeOf("crs", "EPSG", "urn:x-ogc:def:crs:EPSG::4326"));
        assertNull  (         URIParser.codeOf("crs", "EPSG", "urn:n-ogc:def:crs:EPSG::4326"));
        assertEquals("4326",  URIParser.codeOf("crs", "EPSG", " urn : ogc : def : crs : epsg : : 4326"));
        assertNull  (         URIParser.codeOf("crs", "EPSG", "urn:ogc:def:uom:EPSG:9102"));
        assertEquals("9102",  URIParser.codeOf("uom", "EPSG", "urn:ogc:def:uom:EPSG:9102"));
        assertNull  (         URIParser.codeOf("crs", "EPSG", "urn:ogc:def:crs:OGC:1.3:CRS84"));
        assertEquals("CRS84", URIParser.codeOf("crs", "OGC",  "urn:ogc:def:crs:OGC:1.3:CRS84"));
        assertNull  (         URIParser.codeOf("crs", "OGC",  "urn:ogc:def:crs:OGC:1.3:AUTO42003:1:-100:45"));
    }

    /**
     * Tests {@link URIParser#codeOf(String, String, String)} with URL like
     * {@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}.
     */
    @Test
    public void testCodeOfHTTP() {
        assertEquals("4326", URIParser.codeOf("crs", "EPSG", "http://www.opengis.net/gml/srs/epsg.xml#4326"));
        assertNull  (        URIParser.codeOf("crs", "OGC",  "http://www.opengis.net/gml/srs/epsg.xml#4326"));
        assertNull  (        URIParser.codeOf("uom", "EPSG", "http://www.opengis.net/gml/srs/epsg.xml#4326"));
        assertNull  (        URIParser.codeOf("uom", "EPSG", "http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"));
    }

    /**
     * Tests {@link URIParser#xpointer(String, String)}.
     */
    @Test
    public void testXPointer() {
        assertEquals("m", URIParser.xpointer("uom", "http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#m"));
        assertEquals("m", URIParser.xpointer("uom", "http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"));
        assertEquals("m", URIParser.xpointer("uom", "http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/uom/ML_gmxUom.xml#xpointer(//*[@gml:id='m'])"));
        assertEquals("m", URIParser.xpointer("uom", "../uom/ML_gmxUom.xml#xpointer(//*[@gml:id='m'])"));
    }
}
