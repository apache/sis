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
package org.apache.sis.resources.embedded;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.sql.MetadataSource;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.metadata.sql.privy.Initializer;
import org.apache.sis.metadata.sql.privy.LocalDataSource;
import org.apache.sis.system.DataDirectory;
import org.apache.sis.system.Shutdown;
import org.apache.sis.referencing.factory.sql.EPSGFactory;
import org.apache.sis.referencing.factory.sql.epsg.ScriptProvider;


/**
 * Generates {@code SpatialMetadata} database with EPSG geodetic dataset.
 * This class is invoked only at build time and should be excluded from the final JAR file.
 * The {@link #main(String[])} method generates resources directly in the {@code target/classes} directory.
 *
 * <p><b>Note:</b>
 * Maven usage is to generate resources in the {@code target/generated-resources} directory.
 * We don't do that for avoiding unnecessary file copy operations before the package phase.
 * Instead we write the files right in their final destination, {@code target/classes}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Generator extends ScriptProvider {
    /**
     * Generates the embedded resources in the {@code target/classes} directory.
     * See class Javadoc for more information.
     *
     * @param  args  ignored. Can be null.
     * @throws Exception if a failure occurred while searching directories,
     *         executing SQL scripts, copying data or any other operation.
     */
    public static void main(String[] args) throws Exception {
        new Generator().run();
        Shutdown.stop(Generator.class);
    }

    /**
     * Generates the embedded resources in the {@code target/classes} directory.
     *
     * @throws Exception if a failure occurred while searching directories,
     *         executing SQL scripts, copying data or any other operation.
     */
    final void run() throws Exception {
        if (dataSource != null) {
            createMetadata();
            createEPSG();
            compress();
            shutdown();
        }
    }

    /**
     * Provides a connection to the "SpatialMetadata" database, or {@code null} if the database already exists.
     * The connection URL will reference the {@code SIS_DATA/Databases/spatial-metadata} directory in the Maven
     * {@code target/classes} directory.
     */
    private final EmbeddedDataSource dataSource;

    /**
     * Creates a new database generator.
     */
    Generator() throws URISyntaxException, IOException {
        Path target = copyLicenseFiles();
        do target = target.getParent();     // Move to the root directory of classes.
        while (!target.getFileName().toString().startsWith("org.apache.sis."));
        target = target.resolve("META-INF").resolve(DataDirectory.ENV);
        if (Files.isDirectory(target)) {
            dataSource = null;
            return;
        }
        /*
         * Creates sub-directory step by step instead of invoking Files.createDirectories(…)
         * for making sure that we do not create undesirable directories.
         */
        target = Files.createDirectory(target);
        target = Files.createDirectory(target.resolve(Path.of("Databases")));
        dataSource = new EmbeddedDataSource();
        dataSource.setDataSourceName(Initializer.DATABASE);
        dataSource.setDatabaseName(target.resolve(EmbeddedResources.EMBEDDED_DATABASE).toString());
        dataSource.setCreateDatabase("create");
    }

    /**
     * Copies the EPSG terms of use from the {@code sis-epsg} module to this {@code sis-embedded-data} module.
     * We copy those files ourselves instead than relying on {@code maven-resources-plugin} because a future
     * version may combine more licenses in a single file.
     *
     * @return the directory where the licenses have been copied.
     */
    private Path copyLicenseFiles() throws URISyntaxException, IOException {
        final Class<?> consumer = EmbeddedResources.class;
        final Path target = Path.of(consumer.getResource(consumer.getSimpleName() + ".class").toURI()).getParent();
        final String[] files = {"LICENSE.txt", "LICENSE.html"};
        for (String file : files) {
            try (InputStream in = openStream(file)) {
                if (in == null) throw new FileNotFoundException(file);
                Files.copy(in, target.resolve(file));
            } catch (FileAlreadyExistsException e) {
                break;
            }
        }
        return target;
    }

    /**
     * Creates the metadata database schema.
     *
     * @throws FactoryException  if an error occurred while creating or querying the database.
     */
    private void createMetadata() throws MetadataStoreException, ReflectiveOperationException {
        try (MetadataSource md = new MetadataSource(MetadataStandard.ISO_19115, dataSource, "metadata", null)) {
            Method install = md.getClass().getDeclaredMethod("install");
            install.setAccessible(true);
            install.invoke(md);
        }
    }

    /**
     * Creates the EPSG database schema.
     *
     * @throws FactoryException  if an error occurred while creating or querying the database.
     */
    private void createEPSG() throws FactoryException {
        final Map<String,Object> properties = new HashMap<>();
        properties.put("dataSource", dataSource);
        properties.put("scriptProvider", this);
        /*
         * Asking for any CRS will trig the database creation.
         * We perform a simple verification on the created CRS as a matter of principle.
         */
        final GeographicCRS crs;
        try (EPSGFactory factory = new EPSGFactory(properties)) {
            crs = factory.createGeographicCRS("4326");
        }
        if (!crs.getName().getCode().equals("WGS 84")) {
            throw new FactoryException("Unexpected CRS: " + crs.getName());
        }
    }

    /**
     * Compresses all tables in the EPSG schema. Compression can save space if there was many update
     * or delete operations in the database. In the case of the database generated by this class,
     * the benefit is very small because the database is fresh. But it is still non-zero.
     */
    private void compress() throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            List<String> tables = new ArrayList<>(80);  // As (schema,table) pairs.
            try (ResultSet r = c.getMetaData().getTables(null, null, null, null)) {
                while (r.next()) {
                    final String schema = r.getString("TABLE_SCHEM");
                    if (!schema.startsWith("SYS")) {
                        tables.add(schema);
                        tables.add(r.getString("TABLE_NAME"));
                    }
                }
            }
            try (CallableStatement cs = c.prepareCall("CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)")) {
                for (int i=0; i<tables.size();) {
                    cs.setString(1, tables.get(i++));       // Schema name.
                    cs.setString(2, tables.get(i++));       // Table name.
                    cs.setShort (3, (short) 1);
                    cs.execute();
                }
            }
        }
    }

    /**
     * Shutdowns the Derby database.
     */
    private void shutdown() throws SQLException {
        dataSource.setCreateDatabase("no");
        dataSource.setShutdownDatabase("shutdown");
        try {
            dataSource.getConnection().close();
        } catch (SQLException e) {
            if (LocalDataSource.isSuccessfulShutdown(e)) {
                return;
            }
            throw e;
        }
        throw new SQLException("Shutdown has not been completed.");
    }
}
