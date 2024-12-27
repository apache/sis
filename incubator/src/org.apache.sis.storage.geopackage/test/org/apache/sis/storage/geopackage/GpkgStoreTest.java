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
package org.apache.sis.storage.geopackage;

import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.StorageConnector;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.DefaultAttributeType;


/**
 * Tests the Geopackage store.
 *
 * @author Johann Sorel (Geomatys)
 */
@Execution(ExecutionMode.SAME_THREAD)
public final class GpkgStoreTest {
    /**
     * Creates a new test case.
     */
    public GpkgStoreTest() {
    }

    /**
     * Returns the URL to the {@code FeatureSet.gpkg} file.
     * If that file does not exists, it is created from the SQL script.
     *
     * @return {@link URL} or {@link Path} to the test Geopackage file.
     * @throws DataStoreException if the Geopackage test file cannot be created.
     */
    private static Object getTestFile() throws DataStoreException {
        URL file = GpkgStoreTest.class.getResource("FeatureSet.gpkg");
        if (file == null) try {
            final Path source = Path.of(GpkgStoreTest.class.getResource("FeatureSet.sql").toURI());
            final Path target = source.resolveSibling("FeatureSet.gpkg").toAbsolutePath();
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + target)) {
                try (Statement s = c.createStatement()) {
                    s.executeUpdate("PRAGMA main.application_id = " + GpkgStoreProvider.APPLICATION_ID + ';'
                                  + "PRAGMA main.user_version = " + GpkgStoreProvider.VERSION + ';');
                    s.executeUpdate(Files.readString(source));
                }
            }
            return target;
        } catch (URISyntaxException | IOException | SQLException e) {
            throw new DataStoreException("Cannot create the Geopackage test file.", e);
        }
        return file;
    }

    /**
     * Tests reading a feature set from a Geopackage file.
     *
     * @throws DataStoreException if the Geopackage test file cannot be created or read.
     */
    @Test
    public void testFeatureSet() throws DataStoreException {
        try (DataStore store = DataStores.open(getTestFile())) {
            final var gpkg = assertInstanceOf(GpkgStore.class, store);
            assertEquals(7, gpkg.components().size());
            /*
             * Reads and verifies two features from the Geopackage test file.
             */
            FeatureSet fs = getResource(gpkg, "nogeom");
            assertPropertiesEqual(fs, null, "sis:identifier", "fid", "id");
            {   // For keeping `the features` scope locale.
                final AbstractFeature[] features = getFeatures(fs).toArray(AbstractFeature[]::new);
                assertEquals(2, features.length);
                assertEquals(1, features[0].getPropertyValue("fid"));
                assertEquals(1, features[0].getPropertyValue("id"));
                assertEquals(2, features[1].getPropertyValue("fid"));
                assertEquals(2, features[1].getPropertyValue("id"));
            }
            /*
             * Reads and verifies a feature containing a point.
             */
            fs = getResource(gpkg, "point");
            assertPropertiesEqual(fs, "Point",
                    "sis:identifier",
                    "sis:envelope",
                    "sis:geometry",
                    "fid",
                    "geometry",
                    "text",
                    "int32",
                    "int64",
                    "float",
                    "date",
                    "datetime",
                    "bool");

            assertGeometryEquals(fs, "OGC:CRS84", "POINT (3.8691634241245 43.64618798638135)");
            {   // For keeping the `feature` scope local.
                final AbstractFeature feature = getFeatures(fs).get(0);
                assertEquals("some text",               feature.getPropertyValue("text"));
                assertEquals(123,                       feature.getPropertyValue("int32"));
                assertEquals(123456,                    feature.getPropertyValue("int64"));
                assertEquals(123.456,                   feature.getPropertyValue("float"));
                assertEquals("2024-08-07",              feature.getPropertyValue("date"));
                assertEquals("2024-08-07T12:34:56.000", feature.getPropertyValue("datetime"));
                assertEquals(1,                         feature.getPropertyValue("bool"));
            }
            /*
             * Reads and verifies a feature containing a multi-point geometry.
             */
            fs = getResource(gpkg, "multipoint");
            assertPropertiesEqual(fs, "MultiPoint",
                    "sis:identifier",
                    "sis:envelope",
                    "sis:geometry",
                    "fid",
                    "geom");

            assertGeometryEquals(fs, "EPSG:2154",
                    "MULTIPOINT ((770147.9089501069 6283260.318250759), (770224.707537184 6283233.947140678))");
            /*
             * Reads and verifies a feature containing a line string.
             */
            fs = getResource(gpkg, "line");
            assertPropertiesEqual(fs, "LineString",
                    "sis:identifier",
                    "sis:envelope",
                    "sis:geometry",
                    "fid",
                    "geom");

            assertGeometryEquals(fs, "EPSG:3857",
                    "LINESTRING (430669.3103960207 5411208.981453437, 430655.7744657199 5410643.121416978)");
            /*
             * Reads and verifies a feature containing a multi-lines geometry.
             */
            fs = getResource(gpkg, "multiline");
            assertPropertiesEqual(fs, "MultiLineString",
                    "sis:identifier",
                    "sis:envelope",
                    "sis:geometry",
                    "fid",
                    "geom");

            assertGeometryEquals(fs, "EPSG:3395",
                    "MULTILINESTRING ((430750.52597782505 5381116.187674649, 430967.10086263693 5380950.761655851), "
                            + "(430753.08021745336 5381153.51545164, 430917.9595504581 5381182.577731505))");
            /*
             * Reads and verifies a feature containing a polygon.
             */
            fs = getResource(gpkg, "polygon");
            assertPropertiesEqual(fs, "Polygon",
                    "sis:identifier",
                    "sis:envelope",
                    "sis:geometry",
                    "fid",
                    "geom");

            assertGeometryEquals(fs, "EPSG:3857",
                    "POLYGON ((430696.4716464551 5410782.480781593, 430696.03196823364 5410729.011400482, "
                            + "430716.6968446369 5410730.2266102545, 430718.01587930095 5410780.050348468, "
                            + "430696.4716464551 5410782.480781593))");
            /*
             * Reads and verifies a feature containing a multi-polygons.
             */
            fs = getResource(gpkg, "multipolygon");
            assertPropertiesEqual(fs, "MultiPolygon",
                    "sis:identifier",
                    "sis:envelope",
                    "sis:geometry",
                    "fid",
                    "geom");

            assertGeometryEquals(fs, "EPSG:3395",
                    "MULTIPOLYGON (((430795.5091658133 5381110.830395544, "
                            + "430796.60836136667 5381129.2969556395, 430832.2222972956 5381126.118282802, "
                            + "430831.0131821869 5381108.711284476, 430795.5091658133 5381110.830395544)), "
                            + "((430849.25982837286 5381155.483207256, 430849.1499088174 5381151.396330676, "
                            + "430855.0855648057 5381151.547696444, 430855.4153234717 5381154.877743942, "
                            + "430849.25982837286 5381155.483207256)))");
        }
    }

    /**
     * Returns the resource of the given name.
     *
     * @param  store  the store from which to get the feature set.
     * @param  name   name of the desired feature set.
     * @return the requested feature set.
     * @throws DataStoreException if no feature set of the given name was found.
     */
    private static FeatureSet getResource(final GpkgStore store, final String name) throws DataStoreException {
        return assertInstanceOf(FeatureSet.class, store.findResource(name));
    }

    /**
     * Asserts that the properties of the given feature set have the specified names.
     * The geometry is expected to be always in the property at index 4 for all features
     * (this is the case for the {@code FeatureSet.gpkg} test file).
     *
     * @param  fs             the feature set from which to get the properties.
     * @param  geometryIndex  index of the geometry property.
     * @param  propertyNames  the expected names of all properties.
     */
    private static void assertPropertiesEqual(final FeatureSet fs, final String geometryType,
            final String... propertyNames) throws DataStoreException
    {
        final AbstractIdentifiedType[] properties = fs.getType().getProperties(true).toArray(AbstractIdentifiedType[]::new);
        assertEquals(propertyNames.length, properties.length, "properties.size()");
        for (int i=0; i<propertyNames.length; i++) {
            assertEquals(propertyNames[i], properties[i].getName().toString());
        }
        if (geometryType != null) {
            var attribute = assertInstanceOf(DefaultAttributeType.class, properties[4]);
            assertEquals(geometryType, attribute.getValueClass().getSimpleName());
        }
    }

    /**
     * Returns all feature instances as a list.
     *
     * @param  fs  the feature set from which to get the feature instances.
     * @return all feature instances.
     */
    private static List<AbstractFeature> getFeatures(final FeatureSet fs) throws DataStoreException {
        try (Stream<AbstractFeature> stream = fs.features(false)) {
            return stream.toList();
        }
    }

    /**
     * Asserts that the geometry of the given feature set is equal to the expected WKT.
     * This method opportunistically verifies that the value of the {@code "fid"} property is 1.
     *
     * @param resource     the feature set from which to get a single feature instance.
     * @param expectedCRS  identifier of the expected CRS.
     * @param expectedWKT  expected geometry in WKT format.
     */
    private static void assertGeometryEquals(final FeatureSet resource, final String expectedCRS, final String expectedWKT)
            throws DataStoreException
    {
        final List<AbstractFeature> features = getFeatures(resource);
        assertEquals(1, features.size());
        final AbstractFeature feature = features.get(0);

        GeometryWrapper geometry = Geometries.wrap(feature.getPropertyValue("sis:geometry")).get();
        assertEquals(expectedCRS, IdentifiedObjects.getIdentifierOrName(geometry.getCoordinateReferenceSystem()));
        assertEquals(expectedWKT, geometry.formatWKT(1));
        assertEquals(1, feature.getPropertyValue("fid"));
    }

    /**
     * Tests opening the file from a JDBC URL.
     *
     * @throws Exception if a <abbr>SQL</abbr>, I/O or other error occurred.
     */
    @Test
    public void testOpeningFromJDBC_URL() throws Exception {
        final Object file = getTestFile();
        final Path path = (file instanceof Path) ? (Path) file : Path.of(((URL) file).toURI());
        try (DataStore store = DataStores.open(new URI("jdbc:sqlite:" + path))) {
            final var gpkg = assertInstanceOf(GpkgStore.class, store);
            final var fs   = getResource(gpkg, "polygon");
            final var fi   = fs.features(false).findAny().orElseThrow();
            assertEquals(1, fi.getPropertyValue("fid"));
        }
    }

    /**
     * Tests creating an empty database, then opening it as a read-only file.
     *
     * @throws Exception if a <abbr>SQL</abbr>, I/O or other error occurred.
     */
    @Test
    public void testOpeningReadOnlyDatabase() throws Exception {
        final Path file = Files.createTempFile("sis-test-", ".gpkg");
        try {
            // Create an empty database.
            final var connector = new StorageConnector(file);
            connector.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ
            });
            try (GpkgStore store = new GpkgStore(null, connector)) {
                final var gpkg = assertInstanceOf(GpkgStore.class, store);
                assertTrue(gpkg.components().isEmpty());
            }
            assertNotEquals(0, Files.size(file));

            // Make file read only, skip test if we cannot do that.
            final var asFile = file.toFile();
            assumeTrue(asFile.setWritable(false));
            try (DataStore store = DataStores.open(file)) {
                final var gpkg = assertInstanceOf(GpkgStore.class, store);
                assertTrue(gpkg.components().isEmpty());        // Should not raise any exception.
            } finally {
                assertTrue(asFile.setWritable(true));
            }
        } finally {
            Files.delete(file);
            String filename = file.getFileName().toString();
            Files.delete(file.resolveSibling(filename + "-shm"));
            Files.delete(file.resolveSibling(filename + "-wal"));
        }
    }
}
