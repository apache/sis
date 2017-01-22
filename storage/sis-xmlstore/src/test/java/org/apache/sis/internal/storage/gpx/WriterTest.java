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

import java.util.List;
import java.util.Arrays;
import java.net.URI;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import com.esri.core.geometry.Point;
import org.apache.sis.storage.gps.Fix;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.Version;
import org.apache.sis.util.Debug;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import java.time.Instant;
import org.opengis.feature.Feature;


/**
 * Tests (indirectly) the {@link Writer} class.
 * This class creates a {@link Store} instance and uses it in write mode.
 * The {@link Reader} is used for verifying the content.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn({MetadataTest.class, ReaderTest.class})
public final strictfp class WriterTest extends TestCase {
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
     * Where to write the GPX file.
     */
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    /**
     * Creates a new GPX data store which will read and write in memory.
     */
    private Store create() throws DataStoreException {
        return new Store(provider, new StorageConnector(output));
    }

    /**
     * Returns a string representation of the XML data written by the test case.
     * This is a helper method for debugging purpose only.
     *
     * @return the XML document written by the test case.
     */
    @Debug
    @Override
    public String toString() {
        try {
            return output.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Test writing GPX 1.0 metadata. This test creates programmatically the same metadata than the ones
     * found in {@code 1.0/metadata.xml} file, then compares the written XML file with the expected file.
     *
     * @throws Exception if an error occurred while writing the XML data.
     */
    @Test
    public void testMetadata100() throws Exception {
        testMetadata(Store.V1_0, "1.0/metadata.xml");
    }

    /**
     * Test writing GPX 1.1 metadata. This test creates programmatically the same metadata than the ones
     * found in {@code 1.1/metadata.xml} file, then compares the written XML file with the expected file.
     *
     * @throws Exception if an error occurred while writing the XML data.
     */
    @Test
    public void testMetadata110() throws Exception {
        testMetadata(Store.V1_1, "1.1/metadata.xml");
    }

    /**
     * Implementations of {@link #testMetadata100()} and {@link #testMetadata110()}.
     *
     * @param version   either {@link Store#V1_0} or {@link Store#V1_1}.
     * @param expected  name of a test file containing the expected XML result.
     */
    private void testMetadata(final Version version, final String expected) throws Exception {
        final Metadata metadata = MetadataTest.create();
        try (Store store = create()) {
            store.setVersion(version);
            store.write(metadata, null);
        }
        assertXmlEquals(WriterTest.class.getResourceAsStream(expected), toString(), STRICT,
                        new String[] {Tags.NAMESPACE_V11 + ":extensions"},
                        new String[] {"xmlns:xsi", "xsi:schemaLocation", "xsi:type"});
    }

    /**
     * Tests writing various GPX 1.0 way points. This test creates programmatically the same features than
     * the ones found in {@code 1.0/waypoint.xml}, then compares the written XML file with the expected file.
     *
     * @throws Exception if an error occurred while writing the XML data.
     */
    @Test
    @DependsOnMethod("testMetadata100")
    public void testWayPoints100() throws Exception {
        testFeatures(Store.V1_0, Type.WAY_POINT, "1.0/waypoint.xml");
    }

    /**
     * Tests writing various GPX 1.1 way points. This test creates programmatically the same features than
     * the ones found in {@code 1.1/waypoint.xml}, then compares the written XML file with the expected file.
     *
     * @throws Exception if an error occurred while writing the XML data.
     */
    @Test
    @DependsOnMethod("testMetadata110")
    public void testWayPoints110() throws Exception {
        testFeatures(Store.V1_1, Type.WAY_POINT, "1.1/waypoint.xml");
    }

    /**
     * Tests writing various GPX 1.0 routes. This test creates programmatically the same features than
     * the ones found in {@code 1.0/route.xml}, then compares the written XML file with the expected file.
     *
     * @throws Exception if an error occurred while writing the XML data.
     */
    @Test
    @DependsOnMethod("testWayPoints100")
    public void testRoutes100() throws Exception {
        testFeatures(Store.V1_0, Type.ROUTE, "1.0/route.xml");
    }

    /**
     * Tests writing various GPX 1.1 routes. This test creates programmatically the same features than
     * the ones found in {@code 1.1/route.xml}, then compares the written XML file with the expected file.
     *
     * @throws Exception if an error occurred while writing the XML data.
     */
    @Test
    @DependsOnMethod("testWayPoints110")
    public void testRoutes110() throws Exception {
        testFeatures(Store.V1_1, Type.ROUTE, "1.1/route.xml");
    }

    /**
     * Tests writing various GPX 1.0 tracks. This test creates programmatically the same features than
     * the ones found in {@code 1.0/track.xml}, then compares the written XML file with the expected file.
     *
     * @throws Exception if an error occurred while writing the XML data.
     */
    @Test
    @DependsOnMethod("testRoutes100")
    public void testTracks100() throws Exception {
        testFeatures(Store.V1_0, Type.TRACK, "1.0/track.xml");
    }

    /**
     * Tests writing various GPX 1.1 tracks. This test creates programmatically the same features than
     * the ones found in {@code 1.1/track.xml}, then compares the written XML file with the expected file.
     *
     * @throws Exception if an error occurred while writing the XML data.
     */
    @Test
    @DependsOnMethod("testRoutes110")
    public void testTracks110() throws Exception {
        testFeatures(Store.V1_1, Type.TRACK, "1.1/track.xml");
    }

    /**
     * The kind of feature to write.
     */
    private enum Type {
        WAY_POINT, ROUTE, TRACK
    }

    /**
     * Implementation of way points, routes and tracks test methods.
     *
     * @param version   either {@link Store#V1_0} or {@link Store#V1_1}.
     * @param type      the kind of feature to test: way point, route or track.
     * @param expected  name of a test file containing the expected XML result.
     */
    private void testFeatures(final Version version, final Type type, final String expected) throws Exception {
        try (final Store store = create()) {
            store.version = version;
            testFeatures(store, type);
        }
        assertXmlEquals(WriterTest.class.getResourceAsStream(expected), toString(),
                        "xmlns:xsi", "xsi:schemaLocation", "xsi:type");
    }

    /**
     * Writes way points, routes or tracks in the given store.
     *
     * @param store  the store where to write.
     * @param type   the kind of feature to write: way point, route or track.
     */
    private void testFeatures(final Store store, final Type type) throws Exception {
        final Types types = Types.DEFAULT;
        /*
         * Way Points as defined in "waypoint.xml" test file.
         * Appear also in "route.xml" and "track.xml" files.
         */
        final Feature point1 = types.wayPoint.newInstance();
        point1.setPropertyValue("@geometry",     new Point(15, 10));
        point1.setPropertyValue("time",          Instant.parse("2010-01-10T00:00:00Z"));
        point1.setPropertyValue("name",          "first point");
        point1.setPropertyValue("cmt",           "first comment");
        point1.setPropertyValue("desc",          "first description");
        point1.setPropertyValue("src",           "first source");
        point1.setPropertyValue("sym",           "first symbol");
        point1.setPropertyValue("type",          "first type");
        point1.setPropertyValue("ele",           140.0);
        point1.setPropertyValue("magvar",        35.0);
        point1.setPropertyValue("geoidheight",   112.32);
        point1.setPropertyValue("sat",           11);
        point1.setPropertyValue("hdop",          15.15);
        point1.setPropertyValue("vdop",          14.14);
        point1.setPropertyValue("pdop",          13.13);
        point1.setPropertyValue("ageofdgpsdata", 55.55);
        point1.setPropertyValue("dgpsid",        256);
        point1.setPropertyValue("fix",           Fix.NONE);
        point1.setPropertyValue("link",          Arrays.asList(new Link(new URI("http://first-address1.org")),
                                                               new Link(new URI("http://first-address2.org")),
                                                               new Link(new URI("http://first-address3.org"))));
        final Feature point3 = types.wayPoint.newInstance();
        point3.setPropertyValue("@geometry",     new Point(35, 30));
        point3.setPropertyValue("time",          Instant.parse("2010-01-30T00:00:00Z"));
        point3.setPropertyValue("name",          "third point");
        point3.setPropertyValue("cmt",           "third comment");
        point3.setPropertyValue("desc",          "third description");
        point3.setPropertyValue("src",           "third source");
        point3.setPropertyValue("sym",           "third symbol");
        point3.setPropertyValue("type",          "third type");
        point3.setPropertyValue("ele",           150.0);
        point3.setPropertyValue("magvar",        25.0);
        point3.setPropertyValue("geoidheight",   142.32);
        point3.setPropertyValue("sat",           35);
        point3.setPropertyValue("hdop",          35.15);
        point3.setPropertyValue("vdop",          34.14);
        point3.setPropertyValue("pdop",          33.13);
        point3.setPropertyValue("ageofdgpsdata", 85.55);
        point3.setPropertyValue("dgpsid",        456);
        point3.setPropertyValue("fix",           Fix.THREE_DIMENSIONAL);
        point3.setPropertyValue("link",          Arrays.asList(new Link(new URI("http://third-address1.org")),
                                                               new Link(new URI("http://third-address2.org"))));
        final Feature point2 = types.wayPoint.newInstance();
        point2.setPropertyValue("@geometry", new Point(25, 20));
        final List<Feature> wayPoints = Arrays.asList(point1, point2, point3);
        final List<Feature> features;
        switch (type) {
            case WAY_POINT: {
                features = wayPoints;
                break;
            }
            case ROUTE: {
                final Feature route1 = types.route.newInstance();
                route1.setPropertyValue("name",   "Route name");
                route1.setPropertyValue("cmt",    "Route comment");
                route1.setPropertyValue("desc",   "Route description");
                route1.setPropertyValue("src",    "Route source");
                route1.setPropertyValue("type",   "Route type");
                route1.setPropertyValue("number", 7);
                route1.setPropertyValue("rtept",  wayPoints);
                route1.setPropertyValue("link",   Arrays.asList(new Link(new URI("http://route-address1.org")),
                                                                new Link(new URI("http://route-address2.org")),
                                                                new Link(new URI("http://route-address3.org"))));
                final Feature route2 = types.route.newInstance();
                features = Arrays.asList(route1, route2);
                break;
            }
            case TRACK: {
                final Feature seg1 = types.trackSegment.newInstance();
                final Feature seg2 = types.trackSegment.newInstance();
                seg1.setPropertyValue("trkpt", wayPoints);

                final Feature track1 = types.track.newInstance();
                track1.setPropertyValue("name",   "Track name");
                track1.setPropertyValue("cmt",    "Track comment");
                track1.setPropertyValue("desc",   "Track description");
                track1.setPropertyValue("src",    "Track source");
                track1.setPropertyValue("type",   "Track type");
                track1.setPropertyValue("number", 7);
                track1.setPropertyValue("trkseg", Arrays.asList(seg1, seg2));
                track1.setPropertyValue("link",   Arrays.asList(new Link(new URI("http://track-address1.org")),
                                                                new Link(new URI("http://track-address2.org")),
                                                                new Link(new URI("http://track-address3.org"))));
                final Feature track2 = types.track.newInstance();
                features = Arrays.asList(track1, track2);
                break;
            }
            default: throw new AssertionError(type);
        }
        /*
         * Add minimalist metadata and marshal.
         */
        final Bounds bounds = new Bounds();
        bounds.westBoundLongitude = -20;
        bounds.eastBoundLongitude =  30;
        bounds.southBoundLatitude =  10;
        bounds.northBoundLatitude =  40;
        final Metadata metadata = new Metadata();
        metadata.bounds = bounds;
        metadata.creator = "DataProducer";
        store.write(metadata, features.stream());
    }

    /**
     * Tests coexistence of read and write operations by first reading part of a file,
     * then switching in write mode.
     *
     * @throws Exception if an error occurred while creating the temporary test file,
     *         the test data or performing data store operation.
     */
    @Test
    @DependsOnMethod("testRoutes110")
    public void testInputReplacement() throws Exception {
        try (final Store store = new Store(provider, new StorageConnector(
                TestUtilities.createTemporaryFile(ReaderTest.class, "1.1/metadata.xml"))))
        {
            /*
             * Read part of the file. We verify its content as a matter of principle,
             * but the main purpose of following code is to advance in the stream.
             */
            ReaderTest.verifyMetadata((Metadata) store.getMetadata(), 3);
            assertEquals("version", Store.V1_1, store.getVersion());
            /*
             * Replace the metadata content by route content. The data store should rewind
             * to the begining of the file and replace the input stream by an output stream.
             */
            testFeatures(store, Type.ROUTE);
            /*
             * Following should revert the output stream back to an input stream and rewind again.
             */
            ReaderTest.verifyRoute110(store);
        }
    }
}
