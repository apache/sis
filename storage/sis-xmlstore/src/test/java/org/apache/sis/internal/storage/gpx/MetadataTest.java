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
package org.apache.sis.internal.storage.gpx;

import java.util.Arrays;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.date;


/**
 * Tests the {@link Metadata} class.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@DependsOn(TypesTest.class)
public final strictfp class MetadataTest extends TestCase {
    /**
     * Tests the {@link Metadata#equals(Object)} and {@link Metadata#hashCode()}.
     *
     * @throws URISyntaxException if a {@link Link} element is constructed with an invalid URI.
     */
    @Test
    public void testEqualsAndHashCode() throws URISyntaxException {
        final Metadata md1 = create();
        final Metadata md2 = create();
        assertEquals("equals", md1, md2);
        assertEquals("hashCode", md1.hashCode(), md2.hashCode());
        md2.author.name = "someone else";
        assertNotEquals("equals", md1, md2);
        assertNotEquals("hashCode", md1.hashCode(), md2.hashCode());
    }

    /**
     * Tests the copy constructor used for converting ISO 19115 metadata to GPX metadata.
     *
     * @throws URISyntaxException if a {@link Link} element is constructed with an invalid URI.
     */
    @Test
    @DependsOnMethod("testEqualsAndHashCode")
    public void testCopyConstructor() throws URISyntaxException {
        final Metadata md1 = create();
        final Metadata md2 = new Metadata(md1, null);
        assertEquals("equals", md1, md2);
        assertEquals("hashCode", md1.hashCode(), md2.hashCode());
    }

    /**
     * Creates a metadata instance with the same data than the one in the {@code "1.1/metadata.xml"} test file.
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
        metadata.keywords    = Arrays.asList("sample", "metadata");
        metadata.bounds      = bounds;
        metadata.time        = date("2010-03-01 00:00:00");
        metadata.links       = Arrays.asList(new Link(new URI("http://first-address.org")),
                                             new Link(new URI("http://second-address.org")),
                                             new Link(new URI("http://third-address.org")));
        metadata.links.get(2).type = "website";
        metadata.links.get(0).text = "first";
        return metadata;
    }
}
