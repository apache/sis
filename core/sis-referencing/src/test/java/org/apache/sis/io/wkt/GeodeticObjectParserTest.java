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
package org.apache.sis.io.wkt;

import java.util.Date;
import java.util.Iterator;
import java.text.ParsePosition;
import java.text.ParseException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link GeodeticObjectParser}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(MathTransformParserTest.class)
public final strictfp class GeodeticObjectParserTest extends TestCase {
    /**
     * The parser to use for the test.
     */
    private GeodeticObjectParser parser;

    /**
     * Parses the given text.
     *
     * @throws ParseException if an error occurred during the parsing.
     */
    private Object parse(final String text) throws ParseException {
        if (parser == null) {
            parser = new GeodeticObjectParser();
        }
        final ParsePosition position = new ParsePosition(0);
        final Object obj = parser.parseObject(text, position);
        assertEquals("errorIndex", -1, position.getErrorIndex());
        assertEquals("index", text.length(), position.getIndex());
        return obj;
    }

    /**
     * Asserts that the name of the given object is equals to the given string.
     */
    private static void assertNameEquals(final String expected, final IdentifiedObject object) {
        final String message = object.getClass().getSimpleName();
        assertEquals(message, expected, object.getName().getCode());
    }

    /**
     * Tests the parsing of a compound CRS from a WKT 1 string, except the time dimension which is WKT 2.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testCompoundCRS() throws ParseException {
        final CompoundCRS crs = (CompoundCRS) parse(
                "COMPD_CS[“WGS 84 + height + time”,\n" +
                "  GEOGCS[“WGS 84”,\n" +
                "    DATUM[“World Geodetic System 1984”,\n" +
                "      SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "    PRIMEM[“Greenwich”, 0.0],\n" +
                "    UNIT[“degree”, 0.017453292519943295],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]],\n" +
                "  VERT_CS[“Gravity-related height”,\n" +
                "    VERT_DATUM[“Mean Sea Level”, 2005],\n" +
                "    UNIT[“metre”, 1],\n" +
                "    AXIS[“Gravity-related height”, UP]],\n" +
                "  TimeCRS[“Time”,\n" +     // WKT 2
                "    TimeDatum[“Modified Julian”, TimeOrigin[1858-11-17T00:00:00.0Z]],\n" +
                "    Unit[“day”, 86400],\n" +
                "    Axis[“Time”, FUTURE]]]");

        // CompoundCRS parent
        assertNameEquals("WGS 84 + height + time", crs);
        final Iterator<CoordinateReferenceSystem> components = crs.getComponents().iterator();

        // GeographicCRS child
        final GeographicCRS geoCRS    = (GeographicCRS) components.next();
        final GeodeticDatum geoDatum  = geoCRS.getDatum();
        final Ellipsoid     ellipsoid = geoDatum.getEllipsoid();
        assertNameEquals("WGS 84", geoCRS);
        assertNameEquals("World Geodetic System 1984", geoDatum);
        assertNameEquals("WGS84", ellipsoid);
        assertNameEquals("Greenwich", geoDatum.getPrimeMeridian());
        assertEquals("semiMajor", 6378137, ellipsoid.getSemiMajorAxis(), STRICT);
        assertEquals("inverseFlattening", 298.257223563, ellipsoid.getInverseFlattening(), STRICT);

        // VerticalCRS child
        final VerticalCRS vertCRS = (VerticalCRS) components.next();
        assertNameEquals("Gravity-related height", vertCRS);
        assertNameEquals("Mean Sea Level", vertCRS.getDatum());

        // TemporalCRS child
        final TemporalCRS   timeCRS   = (TemporalCRS) components.next();
        final TemporalDatum timeDatum = timeCRS.getDatum();
        assertNameEquals("Time", timeCRS);
        assertNameEquals("Modified Julian", timeDatum);
        assertEquals("epoch", new Date(-40587 * (24*60*60*1000L)), timeDatum.getOrigin());

        // No more CRS.
        assertFalse(components.hasNext());
    }

    /**
     * Tests integration in {@link WKTFormat#parse(CharSequence, ParsePosition)}.
     * This method tests only a simple WKT because it is not the purpose of this
     * method to test the parser itself. We only want to tests its integration in
     * the {@link WKTFormat} class.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    @DependsOnMethod("testCompoundCRS")
    public void testWKTFormat() throws ParseException {
        final WKTFormat format = new WKTFormat(null, null);
        final VerticalCRS crs = (VerticalCRS) format.parseObject(
                "VERT_CS[“Gravity-related height”,\n" +
                "  VERT_DATUM[“Mean Sea Level”, 2005],\n" +
                "  UNIT[“metre”, 1],\n" +
                "  AXIS[“Gravity-related height”, UP]]");

        assertNameEquals("Gravity-related height", crs);
        assertNameEquals("Mean Sea Level", crs.getDatum());
    }
}
