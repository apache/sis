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
package org.apache.sis.referencing.datum;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import javax.xml.bind.JAXBException;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.test.xml.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertIdentifierEquals;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.internal.util.StandardDateFormat.MILLISECONDS_PER_DAY;


/**
 * Tests the {@link DefaultTemporalDatum} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.4
 */
public final class DefaultTemporalDatumTest extends TestCase {
    /**
     * An XML file in this package containing a vertical datum definition.
     */
    private static final String XML_FILE = "TemporalDatum.xml";

    /**
     * November 17, 1858 at 00:00 UTC as a Java timestamp.
     */
    private static final long ORIGIN = -40587L * MILLISECONDS_PER_DAY;

    /**
     * Creates the temporal datum to use for testing purpose.
     */
    private static DefaultTemporalDatum create() {
        final Map<String,Object> properties = new HashMap<>(4);
        assertNull(properties.put(DefaultTemporalDatum.IDENTIFIERS_KEY,
                new ImmutableIdentifier(HardCodedCitations.SIS, "SIS", "MJ")));
        assertNull(properties.put(DefaultTemporalDatum.NAME_KEY, "Modified Julian"));
        assertNull(properties.put(DefaultTemporalDatum.SCOPE_KEY, "History."));
        assertNull(properties.put(DefaultTemporalDatum.REMARKS_KEY,
                "Time measured as days since November 17, 1858 at 00:00 UTC."));
        return new DefaultTemporalDatum(properties, new Date(ORIGIN));
    }

    /**
     * Tests the consistency of our test with {@link HardCodedDatum#MODIFIED_JULIAN}.
     *
     * @since 0.5
     */
    @Test
    public void testConsistency() {
        assertEquals(HardCodedDatum.MODIFIED_JULIAN.getOrigin(), new Date(ORIGIN));
    }

    /**
     * Tests {@link DefaultTemporalDatum#toWKT()}.
     *
     * <p><b>Note:</b> ISO 19162 uses ISO 8601:2004 for the dates. Any precision is allowed:
     * the date could have only the year, or only the year and month, <i>etc</i>. The clock
     * part is optional and also have optional fields: can be only hours, or only hours and minutes, <i>etc</i>.
     * ISO 19162 said that the timezone is restricted to UTC but nevertheless allows to specify a timezone.</p>
     *
     * @since 0.5
     */
    @Test
    public void testToWKT() {
        final DefaultTemporalDatum datum = create();
        assertWktEquals(Convention.WKT1, "TDATUM[“Modified Julian”, TIMEORIGIN[1858-11-17], AUTHORITY[“SIS”, “MJ”]]", datum);
        assertWktEquals(Convention.WKT2, "TDATUM[“Modified Julian”, TIMEORIGIN[1858-11-17], ID[“SIS”, “MJ”]]", datum);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "TimeDatum[“Modified Julian”, TimeOrigin[1858-11-17], Id[“SIS”, “MJ”]]", datum);
    }

    /**
     * Tests XML marshalling.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    public void testMarshalling() throws JAXBException {
        final DefaultTemporalDatum datum = create();
        assertMarshalEqualsFile(XML_FILE, datum, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests XML unmarshalling.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        final DefaultTemporalDatum datum = unmarshalFile(DefaultTemporalDatum.class, XML_FILE);
        assertIdentifierEquals("identifier", "Apache Spatial Information System", "SIS", null, "MJ",
                getSingleton(datum.getIdentifiers()));
        assertEquals("name", "Modified Julian",
                datum.getName().getCode());
        assertEquals("remarks", "Time measured as days since November 17, 1858 at 00:00 UTC.",
                datum.getRemarks().toString());
        assertEquals("scope", "History.",
                datum.getScope().toString());
        assertEquals("origin", new Date(ORIGIN), datum.getOrigin());
    }
}
