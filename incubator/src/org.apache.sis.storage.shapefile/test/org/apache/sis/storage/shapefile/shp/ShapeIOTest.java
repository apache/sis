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
package org.apache.sis.storage.shapefile.shp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Johann Sorel (Geomatys)
 */
public class ShapeIOTest {

    public ShapeIOTest() {}

    private ChannelDataInput openRead(String path) throws DataStoreException {
        final URL url = ShapeIOTest.class.getResource(path);
        final StorageConnector cnx = new StorageConnector(url);
        final ChannelDataInput cdi = cnx.getStorageAs(ChannelDataInput.class);
        cnx.closeAllExcept(cdi);
        return cdi;
    }

    private ChannelDataOutput openWrite(Path path) throws DataStoreException, IOException {
        final StorageConnector cnx = new StorageConnector(path);
        cnx.setOption(OptionKey.OPEN_OPTIONS, new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING});
        final ChannelDataOutput cdo = cnx.getStorageAs(ChannelDataOutput.class);
        cnx.closeAllExcept(cdo);
        return cdo;
    }

    /**
     * Open given shape file, read it and write it to another file the compare them.
     * They must be identical.
     */
    private void testReadAndWrite(String path) throws DataStoreException, IOException, URISyntaxException {
        final ChannelDataInput cdi = openRead(path);

        final Path tempFile = Files.createTempFile("tmp", ".shp");
        final ChannelDataOutput cdo = openWrite(tempFile);

        try {
            try (ShapeReader reader = new ShapeReader(cdi, null);
                 ShapeWriter writer = new ShapeWriter(cdo)) {

                writer.writeHeader(reader.getHeader());

                for (ShapeRecord record = reader.next(); record != null; record = reader.next()) {
                    writer.writeRecord(record);
                }
            }

            //compare files
            final byte[] expected = Files.readAllBytes(Paths.get(ShapeIOTest.class.getResource(path).toURI()));
            final byte[] result = Files.readAllBytes(tempFile);
            assertArrayEquals(expected, result);

        } finally {
            Files.delete(tempFile);
        }
    }


    /**
     * Test reading a point shape type.
     */
    @Test
    public void testPoint() throws Exception {
        final String path = "/org/apache/sis/storage/shapefile/point.shp";

        try (ShapeReader reader = new ShapeReader(openRead(path), null)) {
            final ShapeRecord record1 = reader.next();
            assertEquals(2, record1.bbox.getDimension());
            assertEquals(-38.5, record1.bbox.getMinimum(0), 0.1);
            assertEquals(-38.5, record1.bbox.getMaximum(0), 0.1);
            assertEquals(-13.0, record1.bbox.getMinimum(1), 0.1);
            assertEquals(-13.0, record1.bbox.getMaximum(1), 0.1);
            assertEquals(1, record1.recordNumber);
            final Point pt1 = (Point) record1.geometry;
            assertEquals(-38.5, pt1.getX(), 0.1);
            assertEquals(-13.0, pt1.getY(), 0.1);

            final ShapeRecord record2 = reader.next();
            assertEquals(2, record2.bbox.getDimension());
            assertEquals(2.1, record2.bbox.getMinimum(0), 0.1);
            assertEquals(2.1, record2.bbox.getMaximum(0), 0.1);
            assertEquals(42.5, record2.bbox.getMinimum(1), 0.1);
            assertEquals(42.5, record2.bbox.getMaximum(1), 0.1);
            assertEquals(2, record2.recordNumber);
            final Point pt2 = (Point) record2.geometry;
            assertEquals(2.1, pt2.getX(), 0.1);
            assertEquals(42.5, pt2.getY(), 0.1);

            //no more records
            assertNull(reader.next());
        }

        //test filter, envelope contains record 2
        final Envelope2D filter = new Envelope2D(CommonCRS.WGS84.normalizedGeographic(), 2, 42, 1, 1);
        try (ShapeReader reader = new ShapeReader(openRead(path), filter)) {
            final ShapeRecord record = reader.next();
            assertEquals(2, record.recordNumber);

            //no more records
            assertNull(reader.next());
        }

        testReadAndWrite(path);
    }

    /**
     * Test reading a multipoint shape type.
     */
    @Test
    public void testMultiPoint() throws Exception {
        final String path = "/org/apache/sis/storage/shapefile/multipoint.shp";

        try (ShapeReader reader = new ShapeReader(openRead(path), null)) {
            final ShapeRecord record1 = reader.next();
            assertEquals(2, record1.bbox.getDimension());
            assertEquals(-38.0, record1.bbox.getMinimum(0), 0.1);
            assertEquals(-33.5, record1.bbox.getMaximum(0), 0.1);
            assertEquals(3.3, record1.bbox.getMinimum(1), 0.1);
            assertEquals(6.8, record1.bbox.getMaximum(1), 0.1);
            assertEquals(1, record1.recordNumber);
            final MultiPoint mpt1 = (MultiPoint) record1.geometry;
            assertEquals(2, mpt1.getNumGeometries());
            final Point pt11 = (Point) mpt1.getGeometryN(0);
            final Point pt12 = (Point) mpt1.getGeometryN(1);
            assertEquals(-38.0, pt11.getX(), 0.1);
            assertEquals(3.3, pt11.getY(), 0.1);
            assertEquals(-33.5, pt12.getX(), 0.1);
            assertEquals(6.8, pt12.getY(), 0.1);

            final ShapeRecord record2 = reader.next();
            assertEquals(2, record2.bbox.getDimension());
            assertEquals(2.9, record2.bbox.getMinimum(0), 0.1);
            assertEquals(5.1, record2.bbox.getMaximum(0), 0.1);
            assertEquals(14.6, record2.bbox.getMinimum(1), 0.1);
            assertEquals(16.6, record2.bbox.getMaximum(1), 0.1);
            assertEquals(2, record2.recordNumber);
            final MultiPoint mpt2 = (MultiPoint) record2.geometry;
            final Point pt21 = (Point) mpt2.getGeometryN(0);
            final Point pt22 = (Point) mpt2.getGeometryN(1);
            assertEquals(2, mpt2.getNumGeometries());
            assertEquals(5.1, pt21.getX(), 0.1);
            assertEquals(14.6, pt21.getY(), 0.1);
            assertEquals(2.9, pt22.getX(), 0.1);
            assertEquals(16.6, pt22.getY(), 0.1);

            //no more records
            assertNull(reader.next());
        }

        //test filter, envelope inside record 2
        final Envelope2D filter = new Envelope2D(CommonCRS.WGS84.normalizedGeographic(), 4, 15, 1, 1);
        try (ShapeReader reader = new ShapeReader(openRead(path), filter)) {
            final ShapeRecord record = reader.next();
            assertEquals(2, record.recordNumber);

            //no more records
            assertNull(reader.next());
        }

        testReadAndWrite(path);
    }

    /**
     * Test reading a polyline shape type.
     */
    @Test
    public void testPolyline() throws Exception {
        final String path = "/org/apache/sis/storage/shapefile/polyline.shp";

        try (ShapeReader reader = new ShapeReader(openRead(path), null)) {

            //first record has a single 3 points line
            final ShapeRecord record1 = reader.next();
            assertEquals(2, record1.bbox.getDimension());
            assertEquals(-43.0, record1.bbox.getMinimum(0), 0.1);
            assertEquals(-38.9, record1.bbox.getMaximum(0), 0.1);
            assertEquals(4.8, record1.bbox.getMinimum(1), 0.1);
            assertEquals(7.7, record1.bbox.getMaximum(1), 0.1);
            assertEquals(1, record1.recordNumber);
            final MultiLineString ml1 = (MultiLineString) record1.geometry;
            assertEquals(1, ml1.getNumGeometries());
            final CoordinateSequence l1 = ((LineString) ml1.getGeometryN(0)).getCoordinateSequence();
            assertEquals(3, l1.size());
            assertEquals(-43.0, l1.getX(0), 0.1);
            assertEquals(4.8, l1.getY(0), 0.1);
            assertEquals(-41.9, l1.getX(1), 0.1);
            assertEquals(7.7, l1.getY(1), 0.1);
            assertEquals(-38.9, l1.getX(2), 0.1);
            assertEquals(6.4, l1.getY(2), 0.1);

            //second record has two 2 points lines
            final ShapeRecord record2 = reader.next();
            assertEquals(2, record2.bbox.getDimension());
            assertEquals(-0.9, record2.bbox.getMinimum(0), 0.1);
            assertEquals(5.9, record2.bbox.getMaximum(0), 0.1);
            assertEquals(6.6, record2.bbox.getMinimum(1), 0.1);
            assertEquals(12.1, record2.bbox.getMaximum(1), 0.1);
            assertEquals(2, record2.recordNumber);
            final MultiLineString ml2 = (MultiLineString) record2.geometry;
            assertEquals(2, ml2.getNumGeometries());
            final CoordinateSequence l21 = ((LineString) ml2.getGeometryN(0)).getCoordinateSequence();
            final CoordinateSequence l22 = ((LineString) ml2.getGeometryN(1)).getCoordinateSequence();
            assertEquals(2, l21.size());
            assertEquals(2, l22.size());
            assertEquals(-0.9, l21.getX(0), 0.1);
            assertEquals(12.14, l21.getY(0), 0.1);
            assertEquals(5.1, l21.getX(1), 0.1);
            assertEquals(10.4, l21.getY(1), 0.1);
            assertEquals(0.5, l22.getX(0), 0.1);
            assertEquals(8.4, l22.getY(0), 0.1);
            assertEquals(5.9, l22.getX(1), 0.1);
            assertEquals(6.6, l22.getY(1), 0.1);

            //no more records
            assertNull(reader.next());
        }

        //test filter, envelope intersects record 2
        final Envelope2D filter = new Envelope2D(CommonCRS.WGS84.normalizedGeographic(), 0, 6, 1, 1);
        try (ShapeReader reader = new ShapeReader(openRead(path), filter)) {
            final ShapeRecord record = reader.next();
            assertEquals(2, record.recordNumber);

            //no more records
            assertNull(reader.next());
        }

        testReadAndWrite(path);
    }


    /**
     * Test reading a polygon shape type.
     */
    @Test
    public void testPolygon() throws Exception {
        final String path = "/org/apache/sis/storage/shapefile/polygon.shp";

        try (ShapeReader reader = new ShapeReader(openRead(path), null)) {
            final ShapeRecord record1 = reader.next();
            assertEquals(2, record1.bbox.getDimension());
            assertEquals(-43.8, record1.bbox.getMinimum(0), 0.1);
            assertEquals(-29.7, record1.bbox.getMaximum(0), 0.1);
            assertEquals(-1.6, record1.bbox.getMinimum(1), 0.1);
            assertEquals(14.9, record1.bbox.getMaximum(1), 0.1);
            assertEquals(1, record1.recordNumber);
            final MultiPolygon ml1 = (MultiPolygon) record1.geometry;
            assertEquals(1, ml1.getNumGeometries());
            final Polygon l1 = (Polygon) ml1.getGeometryN(0);
            assertEquals(0, l1.getNumInteriorRing());
            final CoordinateSequence er1 = l1.getExteriorRing().getCoordinateSequence();
            assertEquals(5, er1.size());
            assertEquals(-43.8, er1.getX(0), 0.1);
            assertEquals(2.9, er1.getY(0), 0.1);
            assertEquals(-42.0, er1.getX(1), 0.1);
            assertEquals(14.9, er1.getY(1), 0.1);
            assertEquals(-29.7, er1.getX(2), 0.1);
            assertEquals(10.06, er1.getY(2), 0.1);
            assertEquals(-37.9, er1.getX(3), 0.1);
            assertEquals(-1.6, er1.getY(3), 0.1);

            final ShapeRecord record2 = reader.next();
            assertEquals(2, record2.bbox.getDimension());
            assertEquals(-5.0, record2.bbox.getMinimum(0), 0.1);
            assertEquals(11.6, record2.bbox.getMaximum(0), 0.1);
            assertEquals(3.2, record2.bbox.getMinimum(1), 0.1);
            assertEquals(18.9, record2.bbox.getMaximum(1), 0.1);
            assertEquals(2, record2.recordNumber);
            MultiPolygon ml2 = (MultiPolygon) record2.geometry;
            assertEquals(1, ml2.getNumGeometries());
            final Polygon l2 = (Polygon) ml2.getGeometryN(0);
            assertEquals(1, l2.getNumInteriorRing());
            CoordinateSequence out2 = l2.getExteriorRing().getCoordinateSequence();
            CoordinateSequence inner2 = l2.getInteriorRingN(0).getCoordinateSequence();

            assertEquals(5, out2.size());
            assertEquals(-0.5, out2.getX(0), 0.1);
            assertEquals(18.9, out2.getY(0), 0.1);
            assertEquals(11.6, out2.getX(1), 0.1);
            assertEquals(16.9, out2.getY(1), 0.1);
            assertEquals(8.3, out2.getX(2), 0.1);
            assertEquals(3.2, out2.getY(2), 0.1);
            assertEquals(-5.0, out2.getX(3), 0.1);
            assertEquals(6.2, out2.getY(3), 0.1);

            assertEquals(5, inner2.size());
            assertEquals(2.3, inner2.getX(0), 0.1);
            assertEquals(14.2, inner2.getY(0), 0.1);
            assertEquals(0.0, inner2.getX(1), 0.1);
            assertEquals(9.1, inner2.getY(1), 0.1);
            assertEquals(5.3, inner2.getX(2), 0.1);
            assertEquals(7.9, inner2.getY(2), 0.1);
            assertEquals(6.9, inner2.getX(3), 0.1);
            assertEquals(13.1, inner2.getY(3), 0.1);

            //no more records
            assertNull(reader.next());
        }

        //test filter, envelope intersects record 1
        final Envelope2D filter = new Envelope2D(CommonCRS.WGS84.normalizedGeographic(), -35, 5, 1, 1);
        try (ShapeReader reader = new ShapeReader(openRead(path), filter)) {
            final ShapeRecord record = reader.next();
            assertEquals(1, record.recordNumber);

            //no more records
            assertNull(reader.next());
        }

        testReadAndWrite(path);
    }
}
