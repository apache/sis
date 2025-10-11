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
package org.apache.sis.storage.gpx;

import java.util.List;
import java.util.Date;
import java.time.Instant;
import java.net.URI;
import java.net.URISyntaxException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import org.junit.jupiter.api.Disabled;


/**
 * Tests the {@link Metadata} class.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MetadataTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MetadataTest() {
    }

    /**
     * Tests the {@link Metadata#equals(Object)} and {@link Metadata#hashCode()}.
     *
     * @throws URISyntaxException if a {@link Link} element is constructed with an invalid URI.
     */
    @Test
    public void testEqualsAndHashCode() throws URISyntaxException {
        final Metadata md1 = create();
        final Metadata md2 = create();
        assertEquals(md1, md2);
        assertEquals(md1.hashCode(), md2.hashCode());
        md2.author.name = "someone else";
        assertNotEquals(md1, md2);
        assertNotEquals(md1.hashCode(), md2.hashCode());
    }

    /**
     * Tests the copy constructor used for converting ISO 19115 metadata to GPX metadata.
     *
     * @throws URISyntaxException if a {@link Link} element is constructed with an invalid URI.
     */
    @Test
    @Disabled("Can not execute this test on this branch because it depends on Citation.getOnlineResources() "
          + "and Identification.getExtents() methods, which are not present in GeoAPI 3.0 interfaces. "
          + "Despite this test failure, the copy constructor should nevertheless works in practice "
          + "if the Citation and Identification objects are instances of DefaultCitation or AbstractExtent "
          + "(the SIS implementations of GeoAPI interfaces).")
    public void testCopyConstructor() throws URISyntaxException {
        final Metadata md1 = create();
        final Metadata md2 = new Metadata(md1, null);
        assertEquals(md1, md2);
        assertEquals(md1.hashCode(), md2.hashCode());
    }

    /**
     * Creates a metadata instance with the same data as the one in the {@code "1.1/metadata.xml"} test file.
     */
    static Metadata create() throws URISyntaxException {
        final Person person = new Person();
        person.name  = "Jean-Pierre";
        person.email = "jean.pierre@test.com";
        person.link  = new Link(new URI("http://someone-site.org"));

        final Copyright copyright = new Copyright();
        copyright.author  = "Apache";
        copyright.year    = 2004;
        copyright.license = new URI("http://www.apache.org/licenses/LICENSE-2.0");

        final Bounds bounds = new Bounds();
        bounds.westBoundLongitude = -20;
        bounds.eastBoundLongitude =  30;
        bounds.southBoundLatitude =  10;
        bounds.northBoundLatitude =  40;

        final Metadata metadata = new Metadata();
        metadata.name        = "Sample";
        metadata.description = "GPX test file";
        metadata.author      = person;
        metadata.creator     = "DataProducer";
        metadata.copyright   = copyright;
        metadata.keywords    = List.of("sample", "metadata");
        metadata.bounds      = bounds;
        metadata.time        = Date.from(Instant.parse("2010-03-01T00:00:00Z"));
        metadata.links       = List.of(new Link(new URI("http://first-address.org")),
                                       new Link(new URI("http://second-address.org")),
                                       new Link(new URI("http://third-address.org")));
        metadata.links.get(2).type = "website";
        metadata.links.get(0).text = "first";
        return metadata;
    }
}
