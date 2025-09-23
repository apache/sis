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
package org.apache.sis.storage.sql.postgis;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.function.Supplier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.lang.reflect.Method;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.apache.sis.storage.sql.SimpleFeatureStore;
import org.apache.sis.storage.sql.ResourceDefinition;
import org.apache.sis.storage.sql.feature.BinaryEncoding;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.geometry.wrapper.jts.JTS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.Version;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.apache.sis.storage.sql.SQLStoreTest;
import org.apache.sis.storage.sql.feature.GeometryGetterTest;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.metadata.sql.TestDatabase;
import org.apache.sis.referencing.crs.HardCodedCRS;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;


/**
 * Tests {@link Postgres}.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class PostgresTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public PostgresTest() {
    }

    /**
     * Provides a stream for a resource in the same package as this class.
     * The implementation invokes {@code getResourceAsStream(filename)}.
     * This invocation must be done in this module because the invoked
     * method is caller-sensitive.
     */
    private static Supplier<InputStream> resource(final String filename) {
        return new Supplier<>() {
            @Override public String toString() {return filename;}
            @Override public InputStream get() {return PostgresTest.class.getResourceAsStream(filename);}
        };
    }

    /**
     * Tests {@link Postgres#parseVersion(String)}.
     */
    @Test
    public void testParseVersion() {
        final Version version = Postgres.parseVersion("3.1 USE_GEOS=1 USE_PROJ=1 USE_STATS=1");
        assertEquals(3, version.getMajor());
        assertEquals(1, version.getMinor());
        assertNull  (   version.getRevision());
    }

    /**
     * Performs some verification of store metadata.
     *
     * @param  metadata  the metadata to verify.
     */
    private static void validate(final Metadata metadata) {
        final Identification identification = TestUtilities.getSingleton(metadata.getIdentificationInfo());
        assertTrue(identification.getSpatialRepresentationTypes().containsAll(
                Arrays.asList(SpatialRepresentationType.TEXT_TABLE,
                              SpatialRepresentationType.VECTOR,
                              SpatialRepresentationType.GRID)));
    }

    /**
     * Tests reading and writing features and rasters.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    @ResourceLock(TestDatabase.POSTGRESQL)
    public void testSpatialFeatures() throws Exception {
        try (TestDatabase database = TestDatabase.createOnPostgreSQL(SQLStoreTest.SCHEMA, true)) {
            database.executeSQL(List.of(resource("SpatialFeatures.sql")));
            final var connector = new StorageConnector(database.source);
            connector.setOption(OptionKey.GEOMETRY_LIBRARY, GeometryLibrary.JTS);
            final var table = ResourceDefinition.table(null, SQLStoreTest.SCHEMA, "SpatialData");
            try (var store = new SimpleFeatureStore(new SQLStoreProvider(), connector, table)) {
                validate(store.getMetadata());
                /*
                 * Invoke the private `model()` method. We have to use reflection because the class
                 * is not in the same package and we do not want to expose the method in public API.
                 */
                final Method modelAccessor = SQLStore.class.getDeclaredMethod("model");
                modelAccessor.setAccessible(true);
                final var pg = (Postgres<?>) modelAccessor.invoke(store);
                try (Connection connection = database.source.getConnection();
                     ExtendedInfo info = new ExtendedInfo(pg, connection))
                {
                    connection.setReadOnly(true);   // For avoiding accidental changes to "SPATIAL_REF_SYS" table.
                    testInfoStatements(info);
                    testGeometryGetter(info, connection);
                    testRasterReader(TestRaster.USHORT, info, connection);
                }
                /*
                 * Tests through public API.
                 */
                final FeatureSet resource = store.findResource("SpatialData");
                testAllFeatures(resource);
                testFilteredFeatures(resource, false);
                testFilteredFeatures(resource, true);
                testGeometryTransform(resource);
            }
        }
    }

    /**
     * Tests {@link org.apache.sis.storage.sql.feature.InfoStatements}.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    private static void testInfoStatements(final ExtendedInfo info) throws Exception {
        assertEquals(4326, info.findSRID(HardCodedCRS.WGS84));
        final CoordinateReferenceSystem expected = GeometryGetterTest.getExpectedCRS(3395);
        final CoordinateReferenceSystem actual   = info.fetchCRS(3395);
        assertInstanceOf(ProjectedCRS.class, actual);
        if (expected != null) {
            assertSame(expected, actual);
        }
    }

    /**
     * Tests {@link org.apache.sis.storage.sql.feature.GeometryGetter}
     * in the context of querying a database.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    private static void testGeometryGetter(final ExtendedInfo info, final Connection connection) throws Exception {
        final var test = new GeometryGetterTest();
        test.testFromDatabase(connection, info, BinaryEncoding.HEXADECIMAL);
    }

    /**
     * Tests {@link RasterReader} in the context of a database.
     */
    private static void testRasterReader(final TestRaster test, final ExtendedInfo info, final Connection connection)
            throws Exception
    {
        final var encoding = BinaryEncoding.HEXADECIMAL;
        final var reader = new RasterReader(info);
        try (PreparedStatement stmt = connection.prepareStatement("SELECT image FROM features.\"SpatialData\" WHERE filename=?")) {
            stmt.setString(1, test.filename);
            final ResultSet r = stmt.executeQuery();
            assertTrue(r.next());
            final ReadableByteChannel channel = Channels.newChannel(encoding.decode(r.getBinaryStream(1)));
            final var input = new ChannelDataInput(test.filename, channel, ByteBuffer.allocate(50), false);
            RasterReaderTest.compareReadResult(test, reader, input);
            assertFalse(r.next());
        }
    }

    /**
     * Tests iterating over all features without filters.
     * Opportunistically verifies also the bounding boxes of all features.
     *
     * @param  resource  the set of all features.
     * @throws DataStoreException if an error occurred while fetching the envelope of feature instances.
     */
    private static void testAllFeatures(final FeatureSet resource) throws DataStoreException {
        final Envelope envelope = resource.getEnvelope().get();
        assertEquals(-72, envelope.getMinimum(0), 1);
        assertEquals( 43, envelope.getMaximum(1), 1);
        try (Stream<Feature> features = resource.features(false)) {
            features.forEach(PostgresTest::validate);
        }
        try (Stream<Feature> features = resource.features(false)) {
            assertEquals(8, features.count());
        }
    }

    /**
     * Tests iterating over features with a filter applied, with our without coordinate operation.
     * The filter is {@code ST_Intersects(geometry, literal)} where the literal is a polygon.
     * The coordinate operation is simply an axis swapping.
     *
     * @param  resource   the set of all features.
     * @param  transform  whether to apply a coordinate operation.
     * @throws Exception if an error occurred while fetching the envelope of feature instances.
     */
    private static void testFilteredFeatures(final FeatureSet resource, final boolean transform) throws Exception {
        final Coordinate[] coordinates = {
            new Coordinate(-72, 42),
            new Coordinate(-71, 42),
            new Coordinate(-71, 43),
            new Coordinate(-72, 43),
            new Coordinate(-72, 42)
        };
        if (transform) {
            for (Coordinate c : coordinates) {
                double swp = c.x;
                c.x = c.y;
                c.y = swp;
            }
        }
        final Geometry geom = new GeometryFactory().createPolygon(coordinates);
        if (transform) {
            JTS.setCoordinateReferenceSystem(geom, CommonCRS.WGS84.geographic());
        } else {
            geom.setSRID(4326);
        }
        final var factory = DefaultFilterFactory.forFeatures();
        final var filter = factory.intersects(factory.property("geom4326"), factory.literal(geom));
        try (Stream<Feature> features = resource.features(false).filter(filter)) {
            assertEquals(4, features.count());
        }
    }

    /**
     * Tests the {@code ST_Transform} <abbr>SQLMM</abbr> operation.
     *
     * @param  resource  the set of all features.
     */
    private static void testGeometryTransform(final FeatureSet resource) throws Exception {
        final var factory   = DefaultFilterFactory.forFeatures();
        final var targetCRS = factory.literal(GeometryGetterTest.getExpectedCRS(4326));
        final var geometry  = factory.function("ST_Transform", factory.property("geometry"), targetCRS);
        final var alias     = factory.function("ST_Transform", factory.property(AttributeConvention.GEOMETRY), targetCRS);
        final var query     = new FeatureQuery();
        query.setProjection(new FeatureQuery.NamedExpression(factory.property("filename")),
                            new FeatureQuery.NamedExpression(geometry, "transformed"),
                            new FeatureQuery.NamedExpression(alias),
                            new FeatureQuery.NamedExpression(factory.property("image")));
        final FeatureSet subset = resource.subset(query);
        assertNull(getCRSCharacteristic(resource, "geometry"), "Expected no CRS because it is not the same for all rows.");
        assertNull(getCRSCharacteristic(resource, AttributeConvention.GEOMETRY));
        assertEquals(targetCRS.getValue(), getCRSCharacteristic(subset, "transformed"));
        assertEquals(targetCRS.getValue(), getCRSCharacteristic(subset, AttributeConvention.GEOMETRY));
        subset.features(false).forEach(PostgresTest::validateTransformed);
    }

    /**
     * Invoked for each feature instances which is expected to have been transformed to WGS84.
     */
    private static void validateTransformed(final Feature feature) {
        final Geometry geometry;
        switch (feature.getPropertyValue("filename").toString()) {
            case "point-prj": {
                final var p = (Point) feature.getPropertyValue("transformed");
                assertEquals(1.79663056824E-5, p.getX(), 1E-14);
                assertEquals(2.71310843105E-5, p.getY(), 1E-14);
                geometry = p;
                break;
            }
            case "polygon-prj": {
                geometry = (Geometry) feature.getPropertyValue("transformed");
                final var envelope = geometry.getEnvelopeInternal();
                // Tolerance is specified even if zero in order to ignore the sign of zero.
                assertEquals(0, envelope.getMinX(), 0);
                assertEquals(0, envelope.getMinY(), 0);
                assertEquals(8.983152841195214E-6, envelope.getMaxX(), 1E-14);
                assertEquals(9.043694770179478E-6, envelope.getMaxY(), 1E-14);
                break;
            }
            default: {
                validate(feature);
                return;
            }
        }
        verifySRID(4326, geometry);
        assertEquals(geometry, feature.getPropertyValue(AttributeConvention.GEOMETRY));
    }

    /**
     * Invoked for each feature instances for performing some checks on the feature.
     * This method performs only a superficial verification of geometries.
     */
    private static void validate(final Feature feature) {
        final String       filename = feature.getPropertyValue("filename").toString();
        final Geometry     geometry = (Geometry) feature.getPropertyValue("geometry");
        final GridCoverage raster   = (GridCoverage) feature.getPropertyValue("image");
        final int geomSRID;
        switch (filename) {
            case "raster-ushort.wkb": {
                assertNull(geometry);
                RasterReaderTest.compareReadResult(TestRaster.USHORT, raster);
                assertSame(CommonCRS.WGS84.normalizedGeographic(), raster.getCoordinateReferenceSystem());
                return;
            }
            case "point-nocrs": {
                var p = (Point) geometry;
                assertEquals(3, p.getX());
                assertEquals(4, p.getY());
                geomSRID = 0;
                break;
            }
            case "point-prj": {
                var p = (Point) geometry;
                assertEquals(2, p.getX());
                assertEquals(3, p.getY());
                geomSRID = 3395;
                break;
            }
            case "polygon-prj": geomSRID = 3395; break;
            case "linestring":
            case "polygon":
            case "multi-linestring":
            case "multi-polygon": geomSRID = 4326; break;
            default: throw new AssertionError(filename);
        }
        assertNull(raster);
        verifySRID(geomSRID, geometry);
        assertEquals(geometry, feature.getPropertyValue(AttributeConvention.GEOMETRY));
    }

    /**
     * Asserts that the given geometry as the expected <abbr>CRS</abbr>.
     *
     * @param geomSRID  the expected reference system identifier.
     * @param geometry  the geometry to validate.
     */
    private static void verifySRID(final int geomSRID, final Geometry geometry) {
        try {
            final CoordinateReferenceSystem expected = GeometryGetterTest.getExpectedCRS(geomSRID);
            final CoordinateReferenceSystem actual = JTS.getCoordinateReferenceSystem(geometry);
            if (geomSRID == 0) {
                assertNull(actual);
            } else {
                assertNotNull(actual);
                if (expected != null) {
                    assertEquals(expected, actual);
                }
            }
        } catch (FactoryException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns the {@code crs} characteristic of the geometry column of the given resource.
     */
    private static CoordinateReferenceSystem getCRSCharacteristic(final FeatureSet resource, final String property)
            throws DataStoreException
    {
        final var type = resource.getType();
        return AttributeConvention.getCRSCharacteristic(type, type.getProperty(property));
    }
}
