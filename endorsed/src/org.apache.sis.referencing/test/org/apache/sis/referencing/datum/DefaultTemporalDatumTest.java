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

import java.util.HashMap;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.referencing.ImmutableIdentifier;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.test.Assertions.assertSingletonScope;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.referencing.Assertions.assertRemarksEquals;

// Specific to the main and geoapi-3.1 branches:
import org.apache.sis.temporal.TemporalDate;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.referencing.ObjectDomain.*;
import static org.opengis.referencing.IdentifiedObject.*;
import static org.opengis.test.Assertions.assertIdentifierEquals;


/**
 * Tests the {@link DefaultTemporalDatum} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultTemporalDatumTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultTemporalDatumTest() {
    }

    /**
     * Opens the stream to the XML file in this package containing a vertical datum definition.
     *
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile() {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return DefaultTemporalDatumTest.class.getResourceAsStream("TemporalDatum.xml");
    }

    /**
     * November 17, 1858 at 00:00 UTC.
     */
    private static final OffsetDateTime ORIGIN = OffsetDateTime.of(1858, 11, 17, 0, 0, 0, 0, ZoneOffset.UTC);

    /**
     * Creates the temporal datum to use for testing purpose.
     *
     * @param local  whether the datum origin should be a local date.
     */
    private static DefaultTemporalDatum create(final boolean local) {
        final var properties = new HashMap<String,Object>(4);
        assertNull(properties.put(IDENTIFIERS_KEY,
                new ImmutableIdentifier(HardCodedCitations.SIS, "SIS", "MJ")));
        assertNull(properties.put(NAME_KEY, "Modified Julian"));
        assertNull(properties.put(SCOPE_KEY, "History."));
        assertNull(properties.put(REMARKS_KEY,
                "Time measured as days since November 17, 1858 at 00:00 UTC."));
        return new DefaultTemporalDatum(properties, local ? ORIGIN.toLocalDate() : ORIGIN);
    }

    /**
     * Tests the consistency of our test with {@link HardCodedDatum#MODIFIED_JULIAN}.
     */
    @Test
    public void testConsistency() {
        assertEquals(TemporalDate.toTemporal(HardCodedDatum.MODIFIED_JULIAN.getOrigin()), ORIGIN.toInstant());
    }

    /**
     * Tests {@link DefaultTemporalDatum#toWKT()}.
     *
     * <p><b>Note:</b> ISO 19162 uses ISO 8601:2004 for the dates. Any precision is allowed:
     * the date could have only the year, or only the year and month, <i>etc</i>. The clock
     * part is optional and also have optional fields: can be only hours, or only hours and minutes, <i>etc</i>.
     * ISO 19162 said that the timezone is restricted to UTC but nevertheless allows to specify a timezone.</p>
     */
    @Test
    public void testToWKT() {
        final DefaultTemporalDatum datum = create(true);
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
        final DefaultTemporalDatum datum = create(false);
        assertMarshalEqualsFile(openTestFile(), datum, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests XML unmarshalling.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        final DefaultTemporalDatum datum = unmarshalFile(DefaultTemporalDatum.class, openTestFile());
        assertIdentifierEquals("Apache Spatial Information System", "SIS", null, "MJ",
                               assertSingleton(datum.getIdentifiers()), "identifier");
        assertEquals("Modified Julian", datum.getName().getCode());
        assertRemarksEquals("Time measured as days since November 17, 1858 at 00:00 UTC.", datum, null);
        assertEquals("History.", assertSingletonScope(datum));
        assertEquals(ORIGIN, TemporalDate.toTemporal(datum.getOrigin()));
    }
}
