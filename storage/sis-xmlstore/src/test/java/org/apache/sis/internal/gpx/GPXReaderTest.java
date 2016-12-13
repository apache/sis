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
package org.apache.sis.internal.gpx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import com.esri.core.geometry.Point;
import org.opengis.geometry.Envelope;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import org.opengis.feature.Feature;


/**
 * Test {@link GPXReader} class.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class GPXReaderTest extends TestCase{

    private static final double DELTA = 0.000001;


    private static GPXReader create(final String resource) throws DataStoreException, IOException, XMLStreamException {
        StorageConnector storage = new StorageConnector(GPXReaderTest.class.getResource(resource));
        return new GPXReader(new GPXStore(storage), storage.getStorage(), storage);
    }

    /**
     * Tests GPX version 1.0.0 metadata tag parsing.
     *
     * @throws Exception if reader failed to be created or failed at reading.
     */
    @Test
    public void testMetadataRead100() throws Exception {
        try (final GPXReader reader = create("/org/apache/sis/gpx/sample_metadata100.xml")) {
            final Metadata data = reader.getMetadata();

            assertEquals("sample", data.name);
            assertEquals("sample gpx test file", data.description);
            assertEquals(parseTime("2010-03-01"), data.time);
            assertEquals("sample,metadata", data.keywords);
            assertEquals(new DefaultGeographicBoundingBox(-20, 30, 10, 40), data.bounds);

            assertEquals("Jean-Pierre", data.author.name);
            assertEquals("jean.pierre@test.com", data.author.email);
            assertNull(data.author.link);

            assertNull(data.copyright);

            assertEquals(1, data.links.size());
            assertEquals("http://first-adress.org", data.links.get(0).toString());
        }
    }

    /**
     * Tests GPX version 1.1.0 metadata tag parsing.
     *
     * @throws Exception if reader failed to be created or failed at reading.
     */
    @Test
    public void testMetadataRead110() throws Exception {
        try (final GPXReader reader = create("/org/apache/sis/gpx/sample_metadata110.xml")) {
            final Metadata data = reader.getMetadata();

            assertEquals("sample", data.name);
            assertEquals("sample gpx test file", data.description);
            assertEquals(parseTime("2010-03-01"), data.time);
            assertEquals("sample,metadata", data.keywords);
            assertEquals(new DefaultGeographicBoundingBox(-20, 30, 10, 40), data.bounds);

            assertEquals("Jean-Pierre", data.author.name);
            assertEquals("jean.pierre@test.com", data.author.email);
            assertEquals("http://someone-site.org", data.author.link.toString());

            assertEquals("gnu", data.copyright.author);
            assertEquals(2010, data.copyright.year.intValue());
            assertEquals("http://www.gnu.org/licenses/lgpl-3.0-standalone.html",
                         data.copyright.license.getLinkage().toString());

            assertEquals(3, data.links.size());
            assertEquals("http://first-adress.org", data.links.get(0).toString());
            assertEquals("http://second-adress.org", data.links.get(1).toString());
            assertEquals("http://third-adress.org", data.links.get(2).toString());
        }
    }


    /**
     * Tests GPX version 1.0.0 way point tag parsing.
     *
     * @throws Exception if reader failed to be created or failed at reading.
     */
    @Test
    public void testWayPointRead100() throws Exception {
        try (final GPXReader reader = create("/org/apache/sis/gpx/sample_waypoint100.xml")) {
            final Metadata data = reader.getMetadata();

            assertNull(data.name);
            assertNull(data.description);
            assertNull(data.time);
            assertNull(data.keywords);
            assertEquals(new DefaultGeographicBoundingBox(-20, 30, 10, 40), data.bounds);
            assertNull(data.author);
            assertNull(data.copyright);
            assertEquals(0, data.links.size());

            Feature f = reader.next();
            checkPoint(f, 0, false);
            f = reader.next();
            checkPoint(f, 1, false);
            f = reader.next();
            checkPoint(f, 2, false);
            assertFalse(reader.hasNext());
        }
    }

    /**
     * Tests GPX version 1.1.0 way point tag parsing.
     *
     * @throws Exception if reader failed to be created or failed at reading.
     */
    @Test
    public void testWayPointRead110() throws Exception {
        try (final GPXReader reader = create("/org/apache/sis/gpx/sample_waypoint110.xml")) {
            final Metadata data = reader.getMetadata();

            assertNull(data.name);
            assertNull(data.description);
            assertNull(data.time);
            assertNull(data.keywords);
            assertEquals(new DefaultGeographicBoundingBox(-20, 30, 10, 40), data.bounds);
            assertNull(data.author);
            assertNull(data.copyright);
            assertEquals(0, data.links.size());

            Feature f = reader.next();
            checkPoint(f, 0, true);
            f = reader.next();
            checkPoint(f, 1 , true);
            f = reader.next();
            checkPoint(f, 2, true);
            assertFalse(reader.hasNext());
        }

    }

    /**
     * Tests GPX version v1.0.0 route tag parsing.
     *
     * @throws Exception if reader failed to be created or failed at reading.
     */
    @Test
    public void testRouteRead100() throws Exception {
        try (final GPXReader reader = create("/org/apache/sis/gpx/sample_route100.xml")) {
            final Metadata data = reader.getMetadata();

            assertNull(data.name);
            assertNull(data.description);
            assertNull(data.time);
            assertNull(data.keywords);
            assertEquals(new DefaultGeographicBoundingBox(-20, 30, 10, 40), data.bounds);
            assertNull(data.author);
            assertNull(data.copyright);
            assertEquals(0, data.links.size());

            Feature f = reader.next();
            assertEquals("route name",          f.getPropertyValue("name"));
            assertEquals("route comment",       f.getPropertyValue("cmt"));
            assertEquals("route description",   f.getPropertyValue("desc"));
            assertEquals("route source",        f.getPropertyValue("src"));
            assertEquals("route type",          f.getPropertyValue("type"));
            assertEquals(7,                     f.getPropertyValue("number"));

            List<Link> links = new ArrayList<>((Collection<Link>) f.getPropertyValue("link"));
            assertEquals(1,links.size());
            assertEquals("http://route-adress1.org", links.get(0).toString());

            List<Feature> points = new ArrayList<>((Collection<Feature>) f.getPropertyValue("rtept"));
            assertEquals(3,points.size());
            checkPoint(points.get(0), 0, false);
            checkPoint(points.get(1), 1, false);
            checkPoint(points.get(2), 2, false);

            Envelope bbox = (Envelope) f.getPropertyValue("@envelope");
            assertEquals(bbox.getMinimum(0), 15.0d, DELTA);
            assertEquals(bbox.getMaximum(0), 35.0d, DELTA);
            assertEquals(bbox.getMinimum(1), 10.0d, DELTA);
            assertEquals(bbox.getMaximum(1), 30.0d, DELTA);


            f = reader.next();
            assertEquals(null,                  f.getPropertyValue("name"));
            assertEquals(null,                  f.getPropertyValue("cmt"));
            assertEquals(null,                  f.getPropertyValue("desc"));
            assertEquals(null,                  f.getPropertyValue("src"));
            assertEquals(null,                  f.getPropertyValue("type"));
            assertEquals(null,                  f.getPropertyValue("number"));

            links = new ArrayList<>((Collection<Link>) f.getPropertyValue("link"));
            assertEquals(0,links.size());

            points = new ArrayList<>((Collection<Feature>) f.getPropertyValue("rtept"));
            assertEquals(0,points.size());

            bbox = (Envelope) f.getPropertyValue("@envelope");
            assertNull(bbox);

            assertFalse(reader.hasNext());
        }
    }

    /**
     * Tests GPX version 1.1.0 route tag parsing.
     *
     * @throws Exception if reader failed to be created or failed at reading.
     */
    @Test
    public void testRouteRead110() throws Exception {
        try (final GPXReader reader = create("/org/apache/sis/gpx/sample_route110.xml")) {
            final Metadata data = reader.getMetadata();

            assertNull(data.name);
            assertNull(data.description);
            assertNull(data.time);
            assertNull(data.keywords);
            assertEquals(new DefaultGeographicBoundingBox(-20, 30, 10, 40), data.bounds);
            assertNull(data.author);
            assertNull(data.copyright);
            assertEquals(0, data.links.size());

            Feature f = reader.next();
            assertEquals("route name",          f.getPropertyValue("name"));
            assertEquals("route comment",       f.getPropertyValue("cmt"));
            assertEquals("route description",   f.getPropertyValue("desc"));
            assertEquals("route source",        f.getPropertyValue("src"));
            assertEquals("route type",          f.getPropertyValue("type"));
            assertEquals(7,                     f.getPropertyValue("number"));

            List<Link> links = new ArrayList<>((Collection<Link>) f.getPropertyValue("link"));
            assertEquals(3,links.size());
            assertEquals("http://route-adress1.org", links.get(0).toString());
            assertEquals("http://route-adress2.org", links.get(1).toString());
            assertEquals("http://route-adress3.org", links.get(2).toString());

            List<Feature> points = new ArrayList<>((Collection<Feature>) f.getPropertyValue("rtept"));
            assertEquals(3,points.size());
            checkPoint(points.get(0), 0, true);
            checkPoint(points.get(1), 1, true);
            checkPoint(points.get(2), 2, true);

            Envelope bbox = (Envelope) f.getPropertyValue("@envelope");
            assertEquals(bbox.getMinimum(0), 15.0d, DELTA);
            assertEquals(bbox.getMaximum(0), 35.0d, DELTA);
            assertEquals(bbox.getMinimum(1), 10.0d, DELTA);
            assertEquals(bbox.getMaximum(1), 30.0d, DELTA);


            f = reader.next();
            assertEquals(null,                  f.getPropertyValue("name"));
            assertEquals(null,                  f.getPropertyValue("cmt"));
            assertEquals(null,                  f.getPropertyValue("desc"));
            assertEquals(null,                  f.getPropertyValue("src"));
            assertEquals(null,                  f.getPropertyValue("type"));
            assertEquals(null,                  f.getPropertyValue("number"));

            links = new ArrayList<>((Collection<Link>) f.getPropertyValue("link"));
            assertEquals(0,links.size());

            points = new ArrayList<>((Collection<Feature>) f.getPropertyValue("rtept"));
            assertEquals(0,points.size());

            bbox = (Envelope) f.getPropertyValue("@envelope");
            assertNull(bbox);

            assertFalse(reader.hasNext());
        }
    }

    /**
     * Tests GPX version 1.0.0 track tag parsing.
     *
     * @throws Exception if reader failed to be created or failed at reading.
     */
    @Test
    public void testTrackRead100() throws Exception {
        try (final GPXReader reader = create("/org/apache/sis/gpx/sample_track100.xml")) {
            final Metadata data = reader.getMetadata();

            assertNull(data.name);
            assertNull(data.description);
            assertNull(data.time);
            assertNull(data.keywords);
            assertEquals(new DefaultGeographicBoundingBox(-20, 30, 10, 40), data.bounds);
            assertNull(data.author);
            assertNull(data.copyright);
            assertEquals(0, data.links.size());

            Feature f = reader.next();
            assertEquals("track name",          f.getPropertyValue("name"));
            assertEquals("track comment",       f.getPropertyValue("cmt"));
            assertEquals("track description",   f.getPropertyValue("desc"));
            assertEquals("track source",        f.getPropertyValue("src"));
            assertEquals("track type",          f.getPropertyValue("type"));
            assertEquals(7,                     f.getPropertyValue("number"));

            List<Link> links = new ArrayList<>((Collection<Link>) f.getPropertyValue("link"));
            assertEquals(1,links.size());
            assertEquals("http://track-adress1.org", links.get(0).toString());

            List<Feature> segments = new ArrayList<>((Collection<Feature>) f.getPropertyValue("trkseg"));
            assertEquals(2,segments.size());
            Feature seg1 = segments.get(0);
            Feature seg2 = segments.get(1);
            List<Feature> points = new ArrayList<>((Collection<Feature>) seg1.getPropertyValue("trkpt"));
            assertEquals(3, points.size());
            checkPoint((Feature) points.get(0), 0, false);
            checkPoint((Feature) points.get(1), 1, false);
            checkPoint((Feature) points.get(2), 2, false);
            points = new ArrayList<>((Collection<Feature>) seg2.getPropertyValue("trkpt"));
            assertEquals(0, points.size());

            Envelope bbox = (Envelope) f.getPropertyValue("@envelope");
            assertEquals(bbox.getMinimum(0), 15.0d, DELTA);
            assertEquals(bbox.getMaximum(0), 35.0d, DELTA);
            assertEquals(bbox.getMinimum(1), 10.0d, DELTA);
            assertEquals(bbox.getMaximum(1), 30.0d, DELTA);

            f = reader.next();
            assertEquals(null,                  f.getPropertyValue("name"));
            assertEquals(null,                  f.getPropertyValue("cmt"));
            assertEquals(null,                  f.getPropertyValue("desc"));
            assertEquals(null,                  f.getPropertyValue("src"));
            assertEquals(null,                  f.getPropertyValue("type"));
            assertEquals(null,                  f.getPropertyValue("number"));

            links = new ArrayList<>((Collection<Link>) f.getPropertyValue("link"));
            assertEquals(0,links.size());

            segments = new ArrayList<>((Collection<Feature>) f.getPropertyValue("trkseg"));
            assertEquals(0,segments.size());

            bbox = (Envelope) f.getPropertyValue("@envelope");
            assertNull(bbox);

            assertFalse(reader.hasNext());
        }
    }

    /**
     * Tests GPX version 1.1.0 track tag parsing.
     *
     * @throws Exception if reader failed to be created or failed at reading
     */
    @Test
    public void testTrackRead110() throws Exception {
        try (final GPXReader reader = create("/org/apache/sis/gpx/sample_track110.xml")) {
            final Metadata data = reader.getMetadata();

            assertNull(data.name);
            assertNull(data.description);
            assertNull(data.time);
            assertNull(data.keywords);
            assertEquals(new DefaultGeographicBoundingBox(-20, 30, 10, 40), data.bounds);
            assertNull(data.author);
            assertNull(data.copyright);
            assertEquals(0, data.links.size());

            Feature f = reader.next();
            assertEquals("track name",          f.getPropertyValue("name"));
            assertEquals("track comment",       f.getPropertyValue("cmt"));
            assertEquals("track description",   f.getPropertyValue("desc"));
            assertEquals("track source",        f.getPropertyValue("src"));
            assertEquals("track type",          f.getPropertyValue("type"));
            assertEquals(7,                     f.getPropertyValue("number"));

            List<Link> links = new ArrayList<>((Collection<Link>) f.getPropertyValue("link"));
            assertEquals(3,links.size());
            assertEquals("http://track-adress1.org", links.get(0).toString());
            assertEquals("http://track-adress2.org", links.get(1).toString());
            assertEquals("http://track-adress3.org", links.get(2).toString());

            List<Feature> segments = new ArrayList<>((Collection<Feature>) f.getPropertyValue("trkseg"));
            assertEquals(2,segments.size());
            Feature seg1 = segments.get(0);
            Feature seg2 = segments.get(1);
            List<Feature> points = new ArrayList<>((Collection<Feature>) seg1.getPropertyValue("trkpt"));
            assertEquals(3, points.size());
            checkPoint(points.get(0), 0,true);
            checkPoint(points.get(1), 1,true);
            checkPoint(points.get(2), 2,true);
            points = new ArrayList<>((Collection<Feature>) seg2.getPropertyValue("trkpt"));
            assertEquals(0, points.size());

            Envelope bbox = (Envelope) f.getPropertyValue("@envelope");
            assertEquals(bbox.getMinimum(0), 15.0d, DELTA);
            assertEquals(bbox.getMaximum(0), 35.0d, DELTA);
            assertEquals(bbox.getMinimum(1), 10.0d, DELTA);
            assertEquals(bbox.getMaximum(1), 30.0d, DELTA);

            f = reader.next();
            assertEquals(null,                  f.getPropertyValue("name"));
            assertEquals(null,                  f.getPropertyValue("cmt"));
            assertEquals(null,                  f.getPropertyValue("desc"));
            assertEquals(null,                  f.getPropertyValue("src"));
            assertEquals(null,                  f.getPropertyValue("type"));
            assertEquals(null,                  f.getPropertyValue("number"));

            links = new ArrayList<>((Collection<Link>) f.getPropertyValue("link"));
            assertEquals(0,links.size());

            segments = new ArrayList<>((Collection<Feature>) f.getPropertyValue("trkseg"));
            assertEquals(0,segments.size());

            bbox = (Envelope) f.getPropertyValue("@envelope");
            assertNull(bbox);


            assertFalse(reader.hasNext());
        }
    }

    private void checkPoint(final Feature f, final int num, final boolean v11) throws Exception {
        if (num == 0) {
            assertEquals(0,                     f.getPropertyValue("@identifier"));
            assertEquals(15.0,                  ((Point)f.getPropertyValue("@geometry")).getX(), DELTA);
            assertEquals(10.0,                  ((Point)f.getPropertyValue("@geometry")).getY(), DELTA);
            assertEquals(140.0,                 f.getPropertyValue("ele"));
            assertEquals(parseTime("2010-01-10"),f.getPropertyValue("time"));
            assertEquals(35.0,                  f.getPropertyValue("magvar"));
            assertEquals(112.32,                f.getPropertyValue("geoidheight"));
            assertEquals("first point",         f.getPropertyValue("name"));
            assertEquals("first comment",       f.getPropertyValue("cmt"));
            assertEquals("first description",   f.getPropertyValue("desc"));
            assertEquals("first source",        f.getPropertyValue("src"));
            assertEquals("first sym",           f.getPropertyValue("sym"));
            assertEquals("first type",          f.getPropertyValue("type"));
            assertEquals("first fix",           f.getPropertyValue("fix"));
            assertEquals(11,                    f.getPropertyValue("sat"));
            assertEquals(15.15,                 f.getPropertyValue("hdop"));
            assertEquals(14.14,                 f.getPropertyValue("vdop"));
            assertEquals(13.13,                 f.getPropertyValue("pdop"));
            assertEquals(55.55,                 f.getPropertyValue("ageofdgpsdata"));
            assertEquals(256,                   f.getPropertyValue("dgpsid"));

            final List<Link> links = new ArrayList<>((Collection<Link>) f.getPropertyValue("link"));
            if (v11) {
                assertEquals(3,links.size());
                assertEquals("http://first-adress1.org", links.get(0).toString());
                assertEquals("http://first-adress2.org", links.get(1).toString());
                assertEquals("http://first-adress3.org", links.get(2).toString());
            } else {
                assertEquals(1,links.size());
                assertEquals("http://first-adress1.org", links.get(0).toString());
            }

            final Envelope bbox = (Envelope) f.getPropertyValue("@envelope");
            assertEquals(bbox.getMinimum(0), 15.0d, DELTA);
            assertEquals(bbox.getMaximum(0), 15.0d, DELTA);
            assertEquals(bbox.getMinimum(1), 10.0d, DELTA);
            assertEquals(bbox.getMaximum(1), 10.0d, DELTA);

        } else if (num == 1) {
            assertEquals(1,                     f.getPropertyValue("@identifier"));
            assertEquals(25.0,                  ((Point)f.getPropertyValue("@geometry")).getX(), DELTA);
            assertEquals(20.0,                  ((Point)f.getPropertyValue("@geometry")).getY(), DELTA);
            assertEquals(null,                  f.getPropertyValue("ele"));
            assertEquals(null,                  f.getPropertyValue("time"));
            assertEquals(null,                  f.getPropertyValue("magvar"));
            assertEquals(null,                  f.getPropertyValue("geoidheight"));
            assertEquals(null,                  f.getPropertyValue("name"));
            assertEquals(null,                  f.getPropertyValue("cmt"));
            assertEquals(null,                  f.getPropertyValue("desc"));
            assertEquals(null,                  f.getPropertyValue("src"));
            assertEquals(null,                  f.getPropertyValue("sym"));
            assertEquals(null,                  f.getPropertyValue("type"));
            assertEquals(null,                  f.getPropertyValue("fix"));
            assertEquals(null,                  f.getPropertyValue("sat"));
            assertEquals(null,                  f.getPropertyValue("hdop"));
            assertEquals(null,                  f.getPropertyValue("vdop"));
            assertEquals(null,                  f.getPropertyValue("pdop"));
            assertEquals(null,                  f.getPropertyValue("ageofdgpsdata"));
            assertEquals(null,                  f.getPropertyValue("dgpsid"));

            final List<Link> links = new ArrayList<>((Collection<Link>) f.getPropertyValue("link"));
            assertEquals(0,links.size());

            final Envelope bbox = (Envelope) f.getPropertyValue("@envelope");
            assertEquals(bbox.getMinimum(0), 25.0d, DELTA);
            assertEquals(bbox.getMaximum(0), 25.0d, DELTA);
            assertEquals(bbox.getMinimum(1), 20.0d, DELTA);
            assertEquals(bbox.getMaximum(1), 20.0d, DELTA);

        } else if (num == 2) {
            assertEquals(2,                     f.getPropertyValue("@identifier"));
            assertEquals(35.0,                  ((Point) f.getPropertyValue("@geometry")).getX(), DELTA);
            assertEquals(30.0,                  ((Point) f.getPropertyValue("@geometry")).getY(), DELTA);
            assertEquals(150.0,                 f.getPropertyValue("ele"));
            assertEquals(parseTime("2010-01-30"),f.getPropertyValue("time"));
            assertEquals(25.0,                  f.getPropertyValue("magvar"));
            assertEquals(142.32,                f.getPropertyValue("geoidheight"));
            assertEquals("third point",         f.getPropertyValue("name"));
            assertEquals("third comment",       f.getPropertyValue("cmt"));
            assertEquals("third description",   f.getPropertyValue("desc"));
            assertEquals("third source",        f.getPropertyValue("src"));
            assertEquals("third sym",           f.getPropertyValue("sym"));
            assertEquals("third type",          f.getPropertyValue("type"));
            assertEquals("third fix",           f.getPropertyValue("fix"));
            assertEquals(35,                    f.getPropertyValue("sat"));
            assertEquals(35.15,                 f.getPropertyValue("hdop"));
            assertEquals(34.14,                 f.getPropertyValue("vdop"));
            assertEquals(33.13,                 f.getPropertyValue("pdop"));
            assertEquals(85.55,                 f.getPropertyValue("ageofdgpsdata"));
            assertEquals(456,                   f.getPropertyValue("dgpsid"));

            final List<Link> links = new ArrayList<>((Collection<Link>) f.getPropertyValue("link"));
            if (v11) {
                assertEquals(2,links.size());
                assertEquals("http://third-adress1.org", links.get(0).toString());
                assertEquals("http://third-adress2.org", links.get(1).toString());
            } else {
                assertEquals(1,links.size());
                assertEquals("http://third-adress1.org", links.get(0).toString());
            }

            final Envelope bbox = (Envelope) f.getPropertyValue("@envelope");
            assertEquals(bbox.getMinimum(0), 35.0d, DELTA);
            assertEquals(bbox.getMaximum(0), 35.0d, DELTA);
            assertEquals(bbox.getMinimum(1), 30.0d, DELTA);
            assertEquals(bbox.getMaximum(1), 30.0d, DELTA);

        } else {
            fail("unexpected point number :" + num);
        }
    }

    private static Temporal parseTime(String str) {
        final DateTimeFormatter format = DateTimeFormatter.ISO_DATE;
        final TemporalAccessor accessor = format.parse(str);
        final LocalDate localDate = LocalDate.from(accessor);
        return localDate;
    }
}
