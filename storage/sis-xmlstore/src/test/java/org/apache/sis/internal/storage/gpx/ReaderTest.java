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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.time.Instant;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.content.FeatureCatalogueDescription;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.gps.Fix;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.metadata.content.FeatureTypeInfo;


/**
 * Tests (indirectly) the {@link Reader} class.
 * This class creates a {@link Store} instance and uses it in read-only mode.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.8
 * @module
 */
@DependsOn(MetadataTest.class)
public final strictfp class ReaderTest extends TestCase {
    /**
     * The provider shared by all data stores created in this test class.
     */
    private static StoreProvider provider;

    /**
     * Creates the provider to be shared by all data stores created in this test class.
     */
    @BeforeClass
    public static void createProvider() {
        provider = new StoreProvider();
    }

    /**
     * Disposes the data store provider after all tests have been completed.
     */
    @AfterClass
    public static void disposeProvider() {
        provider = null;
    }

    /**
     * Creates a new GPX data store which will read the given test file.
     *
     * @param  resource  name of the test file in a directory relative to {@code "org/apache/sis/internal/gpx"}.
     */
    private static Store create(final String resource) throws DataStoreException {
        final StorageConnector connector = new StorageConnector(ReaderTest.class.getResourceAsStream(resource));
        connector.setOption(OptionKey.GEOMETRY_LIBRARY, GeometryLibrary.ESRI);
        return new Store(provider, connector);
    }

    /**
     * Verifies that the given {@code actual} bounding box has the expected values.
     * A strict equality is requested.
     */
    private static void assertBoundsEquals(final double westBoundLongitude,
                                           final double eastBoundLongitude,
                                           final double southBoundLatitude,
                                           final double northBoundLatitude,
                                           final Bounds actual)
    {
        assertEquals("westBoundLongitude", westBoundLongitude, actual.westBoundLongitude, STRICT);
        assertEquals("eastBoundLongitude", eastBoundLongitude, actual.eastBoundLongitude, STRICT);
        assertEquals("southBoundLatitude", southBoundLatitude, actual.southBoundLatitude, STRICT);
        assertEquals("northBoundLatitude", northBoundLatitude, actual.northBoundLatitude, STRICT);
    }

    /**
     * Verifies that the given {@code actual} envelope has the expected values.
     * A strict equality is requested.
     */
    private static void assertEnvelopeEquals(final double xmin, final double xmax,
                                             final double ymin, final double ymax,
                                             final Envelope actual)
    {
        assertEquals("dimension", 2, actual.getDimension());
        assertEquals("xmin", actual.getMinimum(0), xmin, STRICT);
        assertEquals("xmax", actual.getMaximum(0), xmax, STRICT);
        assertEquals("ymin", actual.getMinimum(1), ymin, STRICT);
        assertEquals("ymax", actual.getMaximum(1), ymax, STRICT);
    }

    /**
     * Asserts that the string value of {@code actual} is equal to the expected value.
     *
     * @param  expected  the expected value (can be {@code null}).
     * @param  actual    the actual value (can be {@code null}).
     */
    private static void assertStringEquals(final String expected, final Object actual) {
        assertEquals(expected, (actual != null) ? actual.toString() : null);
    }

    /**
     * Tests parsing of GPX version 1.0.0 metadata.
     *
     * @throws DataStoreException if reader failed to be created or failed at reading.
     */
    @Test
    public void testMetadata100() throws DataStoreException {
        try (Store reader = create("1.0/metadata.xml")) {
            final Metadata md = (Metadata) reader.getMetadata();
            verifyMetadata(md, 1);
            assertNull(md.author.link);
            assertNull(md.copyright);
            assertEquals("version", StoreProvider.V1_0, reader.getVersion());
        }
    }

    /**
     * Tests parsing of GPX version 1.1.0 metadata.
     *
     * @throws DataStoreException if reader failed to be created or failed at reading.
     */
    @Test
    public void testMetadata110() throws DataStoreException {
        try (Store reader = create("1.1/metadata.xml")) {
            final Metadata md = (Metadata) reader.getMetadata();
            verifyMetadata(md, 3);
            assertStringEquals("http://someone-site.org", md.author.link);
            assertEquals("Apache", md.copyright.author);
            assertEquals(2004, md.copyright.year.intValue());
            assertStringEquals("http://www.apache.org/licenses/LICENSE-2.0", md.copyright.license);
            assertEquals("version", StoreProvider.V1_1, reader.getVersion());
        }
    }

    /**
     * Verifies that the given metadata have the expected values.
     * This method verifies only the values that are common to both GPX 1.0 and GPX 1.1 test files.
     */
    @SuppressWarnings("fallthrough")
    static void verifyMetadata(final Metadata md, final int numLinks) {
        assertEquals      ("name",         "Sample",                            md.name);
        assertEquals      ("description",  "GPX test file",                     md.description);
        assertEquals      ("time",         date("2010-03-01 00:00:00"),         md.time);
        assertArrayEquals ("keywords",     new String[] {"sample", "metadata"}, md.keywords.toArray());
        assertBoundsEquals(                -20, 30, 10, 40,                     md.bounds);
        assertEquals      ("author.name",  "Jean-Pierre",                       md.author.name);
        assertEquals      ("author.email", "jean.pierre@test.com",              md.author.email);
        assertEquals      ("links.size()", numLinks,                            md.links.size());
        switch (numLinks) {
            default: // Fallthrough everywhere.
            case 3:  assertStringEquals("website",                   md.links.get(2).type);
                     assertStringEquals("http://third-address.org",  md.links.get(2));
            case 2:  assertStringEquals("http://second-address.org", md.links.get(1));
            case 1:  assertStringEquals("http://first-address.org",  md.links.get(0));
                     assertStringEquals("first",                     md.links.get(0).text);
            case 0:  break;
        }
    }

    /**
     * Verifies that the given metadata contains only bounds information
     * and the hard-coded list of feature types.
     */
    private static void verifyAlmostEmptyMetadata(final Metadata md) {
        assertNull("name",                  md.name);
        assertNull("description",           md.description);
        assertNull("time",                  md.time);
        assertNull("keywords",              md.keywords);
        assertBoundsEquals(-20, 30, 10, 40, md.bounds);
        assertNull("author",                md.author);
        assertNull("copyright",             md.copyright);
        assertNull("links",                 md.links);
        /*
         * Verifies the list of feature types declared in the given metadata. Those features
         * are not listed in GPX files; they are rather hard-coded in Types.metadata constant.
         */
        final FeatureCatalogueDescription content = (FeatureCatalogueDescription) getSingleton(md.getContentInfo());
        assertTrue("isIncludedWithDataset", content.isIncludedWithDataset());
        final Iterator<? extends FeatureTypeInfo> it = content.getFeatureTypeInfo().iterator();
        assertStringEquals("Route",    it.next().getFeatureTypeName().tip());
        assertStringEquals("Track",    it.next().getFeatureTypeName().tip());
        assertStringEquals("WayPoint", it.next().getFeatureTypeName().tip());
        assertFalse("hasNext", it.hasNext());
    }

    /**
     * Tests parsing of GPX version 1.0.0 way point.
     *
     * @throws DataStoreException if reader failed to be created or failed at reading.
     */
    @Test
    @DependsOnMethod("testMetadata100")
    public void testWayPoint100() throws DataStoreException {
        try (Store reader = create("1.0/waypoint.xml")) {
            verifyAlmostEmptyMetadata((Metadata) reader.getMetadata());
            assertEquals("version", StoreProvider.V1_0, reader.getVersion());
            try (Stream<Feature> features = reader.features(false)) {
                final Iterator<Feature> it = features.iterator();
                verifyPoint(it.next(), 0, false);
                verifyPoint(it.next(), 1, false);
                verifyPoint(it.next(), 2, false);
                assertFalse("hasNext", it.hasNext());
            }
        }
    }

    /**
     * Tests parsing of GPX version 1.1.0 way point.
     *
     * @throws DataStoreException if reader failed to be created or failed at reading.
     */
    @Test
    @DependsOnMethod("testMetadata110")
    public void testWayPoint110() throws DataStoreException {
        try (Store reader = create("1.1/waypoint.xml")) {
            verifyAlmostEmptyMetadata((Metadata) reader.getMetadata());
            assertEquals("version", StoreProvider.V1_1, reader.getVersion());
            try (Stream<Feature> features = reader.features(false)) {
                final Iterator<Feature> it = features.iterator();
                verifyPoint(it.next(), 0, true);
                verifyPoint(it.next(), 1, true);
                verifyPoint(it.next(), 2, true);
                assertFalse("hasNext", it.hasNext());
            }
        }
    }

    /**
     * Tests parsing of GPX version 1.0.0 route.
     *
     * @throws DataStoreException if reader failed to be created or failed at reading.
     */
    @Test
    @DependsOnMethod("testWayPoint100")
    public void testRoute100() throws DataStoreException {
        try (Store reader = create("1.0/route.xml")) {
            verifyAlmostEmptyMetadata((Metadata) reader.getMetadata());
            assertEquals("version", StoreProvider.V1_0, reader.getVersion());
            try (Stream<Feature> features = reader.features(false)) {
                final Iterator<Feature> it = features.iterator();
                verifyRoute(it.next(), false, 1);
                verifyEmpty(it.next(), "rtept");
                assertFalse("hasNext", it.hasNext());
            }
        }
    }

    /**
     * Tests parsing of GPX version 1.1.0 route.
     *
     * @throws DataStoreException if reader failed to be created or failed at reading.
     */
    @Test
    @DependsOnMethod("testWayPoint110")
    public void testRoute110() throws DataStoreException {
        try (Store reader = create("1.1/route.xml")) {
            verifyAlmostEmptyMetadata((Metadata) reader.getMetadata());
            assertEquals("version", StoreProvider.V1_1, reader.getVersion());
            verifyRoute110(reader);
        }
    }

    /**
     * Verifies the routes of GPX {@code "1.1/route.xml"} test file.
     * This verification is shared by {@link #testRoute110()} and {@link #testSequentialReads()}.
     */
    static void verifyRoute110(final Store reader) throws DataStoreException {
        try (Stream<Feature> features = reader.features(false)) {
            final Iterator<Feature> it = features.iterator();
            verifyRoute(it.next(), true, 3);
            verifyEmpty(it.next(), "rtept");
            assertFalse("hasNext", it.hasNext());
        }
    }

    /**
     * Verifies property values for the given route.
     *
     * @param  f         the route to verify.
     * @param  v11       {@code true} for GPX 1.1, or {@code false} for GPX 1.0.
     * @param  numLinks  expected number of links.
     */
    @SuppressWarnings("fallthrough")
    private static void verifyRoute(final Feature f, final boolean v11, final int numLinks) {
        assertEquals("name",       "Route name",          f.getPropertyValue("name"));
        assertEquals("cmt",        "Route comment",       f.getPropertyValue("cmt"));
        assertEquals("desc",       "Route description",   f.getPropertyValue("desc"));
        assertEquals("src",        "Route source",        f.getPropertyValue("src"));
        assertEquals("type", v11 ? "Route type" : null,   f.getPropertyValue("type"));
        assertEquals("number",      7,                    f.getPropertyValue("number"));

        final List<?> links = (List<?>) f.getPropertyValue("link");
        assertEquals("links.size()", numLinks, links.size());
        switch (numLinks) {
            default: // Fallthrough everywhere.
            case 3:  assertStringEquals("http://route-address3.org", links.get(2));
            case 2:  assertStringEquals("http://route-address2.org", links.get(1));
            case 1:  assertStringEquals("http://route-address1.org", links.get(0));
            case 0:  break;
        }

        final List<?> points = (List<?>) f.getPropertyValue("rtept");
        assertEquals("points.size()", 3, points.size());
        verifyPoint((Feature) points.get(0), 0, v11);
        verifyPoint((Feature) points.get(1), 1, v11);
        verifyPoint((Feature) points.get(2), 2, v11);

        final Polyline p = (Polyline) f.getPropertyValue("sis:geometry");
        assertEquals("pointCount", 3, p.getPointCount());
        assertEquals("point(0)", new Point(15, 10), p.getPoint(0));
        assertEquals("point(1)", new Point(25, 20), p.getPoint(1));
        assertEquals("point(2)", new Point(35, 30), p.getPoint(2));
        assertEnvelopeEquals(15, 35, 10, 30, (Envelope) f.getPropertyValue("sis:envelope"));
    }

    /**
     * Verifies that all properties of the given route or track are null or empty.
     *
     * @param  f    the route or track to verify.
     * @param  dep  {@code "rtept"} if verifying a route, or {@code "trkseg"} if verifying a track.
     */
    private static void verifyEmpty(final Feature f, final String dep) {
        assertNull("name",   f.getPropertyValue("name"));
        assertNull("cmt",    f.getPropertyValue("cmt"));
        assertNull("desc",   f.getPropertyValue("desc"));
        assertNull("src",    f.getPropertyValue("src"));
        assertNull("type",   f.getPropertyValue("type"));
        assertNull("number", f.getPropertyValue("number"));

        assertTrue( "links.isEmpty()", ((Collection<?>) f.getPropertyValue("link" )).isEmpty());
        assertTrue("points.isEmpty()", ((Collection<?>) f.getPropertyValue(dep)).isEmpty());
        assertNull("sis:envelope",                      f.getPropertyValue("sis:envelope"));
    }

    /**
     * Tests parsing of GPX version 1.0.0 track.
     *
     * @throws DataStoreException if reader failed to be created or failed at reading.
     */
    @Test
    @DependsOnMethod("testRoute100")
    public void testTrack100() throws DataStoreException {
        try (Store reader = create("1.0/track.xml")) {
            verifyAlmostEmptyMetadata((Metadata) reader.getMetadata());
            assertEquals("version", StoreProvider.V1_0, reader.getVersion());
            try (Stream<Feature> features = reader.features(false)) {
                final Iterator<Feature> it = features.iterator();
                verifyTrack(it.next(), false, 1);
                verifyEmpty(it.next(), "trkseg");
                assertFalse("hasNext", it.hasNext());
            }
        }
    }

    /**
     * Tests parsing of GPX version 1.1.0 track.
     *
     * @throws DataStoreException if reader failed to be created or failed at reading
     */
    @Test
    @DependsOnMethod("testRoute110")
    public void testTrack110() throws DataStoreException {
        try (Store reader = create("1.1/track.xml")) {
            verifyAlmostEmptyMetadata((Metadata) reader.getMetadata());
            assertEquals("version", StoreProvider.V1_1, reader.getVersion());
            try (Stream<Feature> features = reader.features(false)) {
                final Iterator<Feature> it = features.iterator();
                verifyTrack(it.next(), true, 3);
                verifyEmpty(it.next(), "trkseg");
                assertFalse("hasNext", it.hasNext());
            }
        }
    }

    /**
     * Verifies property values for the given track.
     *
     * @param  f         the track to verify.
     * @param  v11       {@code true} for GPX 1.1, or {@code false} for GPX 1.0.
     * @param  numLinks  expected number of links.
     */
    @SuppressWarnings("fallthrough")
    private static void verifyTrack(final Feature f, final boolean v11, final int numLinks) {
        assertEquals("name",       "Track name",          f.getPropertyValue("name"));
        assertEquals("cmt",        "Track comment",       f.getPropertyValue("cmt"));
        assertEquals("desc",       "Track description",   f.getPropertyValue("desc"));
        assertEquals("src",        "Track source",        f.getPropertyValue("src"));
        assertEquals("type", v11 ? "Track type" : null,   f.getPropertyValue("type"));
        assertEquals("number",      7,                    f.getPropertyValue("number"));

        final List<?> links = (List<?>) f.getPropertyValue("link");
        assertEquals("links.size()", numLinks, links.size());
        switch (numLinks) {
            default: // Fallthrough everywhere.
            case 3:  assertStringEquals("http://track-address3.org", links.get(2));
            case 2:  assertStringEquals("http://track-address2.org", links.get(1));
            case 1:  assertStringEquals("http://track-address1.org", links.get(0));
            case 0:  break;
        }

        final List<?> segments = (List<?>) f.getPropertyValue("trkseg");
        assertEquals("segments.size()", 2, segments.size());
        final Feature seg1 = (Feature) segments.get(0);
        final Feature seg2 = (Feature) segments.get(1);
        final List<?> points = (List<?>) seg1.getPropertyValue("trkpt");
        assertEquals("points.size()", 3, points.size());
        verifyPoint((Feature) points.get(0), 0, v11);
        verifyPoint((Feature) points.get(1), 1, v11);
        verifyPoint((Feature) points.get(2), 2, v11);
        assertTrue(((Collection<?>) seg2.getPropertyValue("trkpt")).isEmpty());

        final Polyline p = (Polyline) f.getPropertyValue("sis:geometry");
        assertEquals("pointCount", 3, p.getPointCount());
        assertEquals("point(0)", new Point(15, 10), p.getPoint(0));
        assertEquals("point(1)", new Point(25, 20), p.getPoint(1));
        assertEquals("point(2)", new Point(35, 30), p.getPoint(2));
        assertEnvelopeEquals(15, 35, 10, 30, (Envelope) f.getPropertyValue("sis:envelope"));
    }

    /**
     * Verifies values of the given point.
     *
     * @param  f      the point to verify.
     * @param  index  index of the point being verified: 0, 1 or 2.
     * @param  v11    {@code true} for GPX 1.1, or {@code false} for GPX 1.0.
     */
    private static void verifyPoint(final Feature f, final int index, final boolean v11) {
        assertEquals("sis:identifier", index + 1, f.getPropertyValue("sis:identifier"));
        switch (index) {
            case 0: {
                assertEquals(Instant.parse("2010-01-10T00:00:00Z"), f.getPropertyValue("time"));
                assertEquals("x",               15.0,      ((Point) f.getPropertyValue("sis:geometry")).getX(), STRICT);
                assertEquals("y",               10.0,      ((Point) f.getPropertyValue("sis:geometry")).getY(), STRICT);
                assertEquals("ele",            140.0,               f.getPropertyValue("ele"));
                assertEquals("magvar",          35.0,               f.getPropertyValue("magvar"));
                assertEquals("geoidheight",    112.32,              f.getPropertyValue("geoidheight"));
                assertEquals("name",           "first point",       f.getPropertyValue("name"));
                assertEquals("cmt",            "first comment",     f.getPropertyValue("cmt"));
                assertEquals("desc",           "first description", f.getPropertyValue("desc"));
                assertEquals("src",            "first source",      f.getPropertyValue("src"));
                assertEquals("sym",            "first symbol",      f.getPropertyValue("sym"));
                assertEquals("type",           "first type",        f.getPropertyValue("type"));
                assertEquals("fix",            Fix.NONE,            f.getPropertyValue("fix"));
                assertEquals("sat",            11,                  f.getPropertyValue("sat"));
                assertEquals("hdop",           15.15,               f.getPropertyValue("hdop"));
                assertEquals("vdop",           14.14,               f.getPropertyValue("vdop"));
                assertEquals("pdop",           13.13,               f.getPropertyValue("pdop"));
                assertEquals("ageofdgpsdata",  55.55,               f.getPropertyValue("ageofdgpsdata"));
                assertEquals("dgpsid",        256,                  f.getPropertyValue("dgpsid"));
                final List<?> links = (List<?>) f.getPropertyValue("link");
                assertEquals("links.size()", v11 ? 3 : 1, links.size());
                assertStringEquals("http://first-address1.org", links.get(0));
                if (v11) {
                    assertStringEquals("http://first-address2.org", links.get(1));
                    assertStringEquals("http://first-address3.org", links.get(2));
                }
                assertEnvelopeEquals(15, 15, 10, 10, (Envelope) f.getPropertyValue("sis:envelope"));
                break;
            }
            case 1: {
                assertEquals("x", 25, ((Point)f.getPropertyValue("sis:geometry")).getX(), STRICT);
                assertEquals("y", 20, ((Point)f.getPropertyValue("sis:geometry")).getY(), STRICT);
                assertNull("ele",             f.getPropertyValue("ele"));
                assertNull("time",            f.getPropertyValue("time"));
                assertNull("magvar",          f.getPropertyValue("magvar"));
                assertNull("geoidheight",     f.getPropertyValue("geoidheight"));
                assertNull("name",            f.getPropertyValue("name"));
                assertNull("cmt",             f.getPropertyValue("cmt"));
                assertNull("desc",            f.getPropertyValue("desc"));
                assertNull("src",             f.getPropertyValue("src"));
                assertNull("sym",             f.getPropertyValue("sym"));
                assertNull("type",            f.getPropertyValue("type"));
                assertNull("fix",             f.getPropertyValue("fix"));
                assertNull("sat",             f.getPropertyValue("sat"));
                assertNull("hdop",            f.getPropertyValue("hdop"));
                assertNull("vdop",            f.getPropertyValue("vdop"));
                assertNull("pdop",            f.getPropertyValue("pdop"));
                assertNull("ageofdgpsdata",   f.getPropertyValue("ageofdgpsdata"));
                assertNull("dgpsid",          f.getPropertyValue("dgpsid"));
                assertTrue("links.isEmpty()", ((List<?>) f.getPropertyValue("link")).isEmpty());
                assertEnvelopeEquals(25, 25, 20, 20, (Envelope) f.getPropertyValue("sis:envelope"));
                break;
            }
            case 2: {
                assertEquals(Instant.parse("2010-01-30T00:00:00Z"),  f.getPropertyValue("time"));
                assertEquals("x",               35.0,       ((Point) f.getPropertyValue("sis:geometry")).getX(), STRICT);
                assertEquals("y",               30.0,       ((Point) f.getPropertyValue("sis:geometry")).getY(), STRICT);
                assertEquals("ele",            150.0,                f.getPropertyValue("ele"));
                assertEquals("magvar",          25.0,                f.getPropertyValue("magvar"));
                assertEquals("geoidheight",    142.32,               f.getPropertyValue("geoidheight"));
                assertEquals("name",          "third point",         f.getPropertyValue("name"));
                assertEquals("cmt",           "third comment",       f.getPropertyValue("cmt"));
                assertEquals("desc",          "third description",   f.getPropertyValue("desc"));
                assertEquals("src",           "third source",        f.getPropertyValue("src"));
                assertEquals("sym",           "third symbol",        f.getPropertyValue("sym"));
                assertEquals("type",          "third type",          f.getPropertyValue("type"));
                assertEquals("fix",           Fix.THREE_DIMENSIONAL, f.getPropertyValue("fix"));
                assertEquals("sat",            35,                   f.getPropertyValue("sat"));
                assertEquals("hdop",           35.15,                f.getPropertyValue("hdop"));
                assertEquals("vdop",           34.14,                f.getPropertyValue("vdop"));
                assertEquals("pdop",           33.13,                f.getPropertyValue("pdop"));
                assertEquals("ageofdgpsdata",  85.55,                f.getPropertyValue("ageofdgpsdata"));
                assertEquals("dgpsid",        456,                   f.getPropertyValue("dgpsid"));
                final List<?> links = (List<?>) f.getPropertyValue("link");
                assertEquals("links.size()", v11 ? 2 : 1, links.size());
                assertStringEquals("http://third-address1.org", links.get(0));
                if (v11) {
                    assertStringEquals("http://third-address2.org", links.get(1));
                }
                assertEnvelopeEquals(35, 35, 30, 30, (Envelope) f.getPropertyValue("sis:envelope"));
                break;
            }
            default: {
                fail("unexpected index:" + index);
                break;
            }
        }
    }

    /**
     * Tests parsing of GPX version 1.1.0 route without reading the metadata before it.
     * The reader is expected to skip metadata without unmarshalling them.
     *
     * @throws DataStoreException if reader failed to be created or failed at reading.
     */
    @Test
    @DependsOnMethod("testRoute110")
    public void testRouteSkipMetadata() throws DataStoreException {
        try (Store reader = create("1.1/route.xml")) {
            verifyRoute110(reader);
        }
    }

    /**
     * Creates a data store for the {@code "1.1/route.xml"} test files using its URL instead of the input stream.
     * Using the URL makes easier for the data store to read the same data more than once.
     */
    private static Store createFromURL() throws DataStoreException {
        final StorageConnector connector = new StorageConnector(ReaderTest.class.getResource("1.1/route.xml"));
        connector.setOption(OptionKey.GEOMETRY_LIBRARY, GeometryLibrary.ESRI);
        connector.setOption(OptionKey.URL_ENCODING, "UTF-8");
        return new Store(provider, connector);
    }

    /**
     * Tests reading the same data more than once. For this test, the XML resource needs to be obtained
     * as a URL instead of as an input stream.  But Apache SIS implementation will still tries to use
     * mark/reset instead of re-opening a new stream if it can.
     *
     * @throws DataStoreException if reader failed to be created or failed at reading.
     */
    @Test
    @DependsOnMethod("testRouteSkipMetadata")
    public void testSequentialReads() throws DataStoreException {
        final Metadata md;
        try (Store reader = createFromURL()) {
            verifyRoute110(reader);
            /*
             * Ask for metadata only after a first read, for testing the way the store manages readers.
             * The new 'features()' call should reuse the reader created by 'getMetadata()' - this can
             * be verified by stepping in the code with a debugger.
             */
            md = (Metadata) reader.getMetadata();
            verifyRoute110(reader);
            /*
             * One more check.
             */
            assertSame("metadata", md, reader.getMetadata());
            verifyRoute110(reader);
        }
        verifyAlmostEmptyMetadata(md);
    }

    /**
     * Tests reading the same data more than once without waiting
     * that the previous iterator finishes its iteration.
     *
     * @throws DataStoreException if reader failed to be created or failed at reading.
     */
    @Test
    @DependsOnMethod("testSequentialReads")
    public void testConcurrentReads() throws DataStoreException {
        try (Store reader = createFromURL()) {
            final Stream<Feature>   f1 = reader.features(false);
            final Iterator<Feature> i1 = f1.iterator();
            verifyRoute(i1.next(), true, 3);
            final Stream<Feature>   f2 = reader.features(false);
            final Iterator<Feature> i2 = f2.iterator();
            verifyEmpty(i1.next(), "rtept");
            final Stream<Feature>   f3 = reader.features(false);
            final Iterator<Feature> i3 = f3.iterator();
            verifyRoute(i2.next(), true, 3);
            verifyRoute(i3.next(), true, 3);
            verifyEmpty(i3.next(), "rtept");
            verifyEmpty(i2.next(), "rtept");
            assertFalse("hasNext", i3.hasNext());
            assertFalse("hasNext", i1.hasNext());
            assertFalse("hasNext", i2.hasNext());
            f2.close();
            f1.close();
            f3.close();
        }
    }
}
