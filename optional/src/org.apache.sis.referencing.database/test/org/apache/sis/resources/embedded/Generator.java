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

import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
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
import org.apache.sis.metadata.sql.internal.shared.Initializer;
import org.apache.sis.metadata.sql.internal.shared.LocalDataSource;
import org.apache.sis.system.DataDirectory;
import org.apache.sis.referencing.factory.sql.EPSGFactory;
import org.apache.sis.referencing.factory.sql.epsg.ScriptProvider;


/**
 * Generates {@code SpatialMetadata} database with <abbr>EPSG</abbr> geodetic dataset.
 * This class is invoked only at build time and should be excluded from the final <abbr>JAR</abbr> file.
 * The {@link #createIfAbsent()} method generates resources directly in the {@code target/classes} directory.
 *
 * <p><b>Note:</b>
 * Maven usage is to generate resources in the {@code target/generated-resources} directory.
 * We don't do that for avoiding unnecessary file copy operations before the package phase.
 * Instead we write the files right in their final destination, {@code target/classes}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Generator extends ScriptProvider {
    /**
     * Directory where the {@link EmbeddedResources} class file is located.
     * This is the directory where to copy the license files.
     */
    private final Path classesDirectory;

    /**
     * Directory where to search for <abbr>EPSG</abbr> data.
     */
    private final Path sourceEPSG;

    /**
     * Provides a connection to the "SpatialMetadata" database, or {@code null} if the database already exists.
     * The connection <abbr>URL</abbr> references the following directory in the compilation output directory:
     * <code>{@value EmbeddedResources#DIRECTORY}/Databases/{@value Initializer#DATABASE}</code>
     */
    private final EmbeddedDataSource dataSource;

    /**
     * Creates a new database generator.
     */
    Generator() throws URISyntaxException, IOException {
        classesDirectory = directoryOf(EmbeddedResources.class);
        Path target = classesDirectory;
        do target = target.getParent();     // Move to the root directory of classes.
        while (!target.getFileName().toString().startsWith("org.apache.sis."));
        target = target.resolve("META-INF").resolve(DataDirectory.ENV);
        if (Files.isDirectory(target)) {
            dataSource = null;
            sourceEPSG = null;
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
        dataSource.setDatabaseName(target.resolve(Initializer.DATABASE).toString());
        dataSource.setCreateDatabase("create");
        sourceEPSG = directoryOf(ScriptProvider.class);
    }

    /**
     * Returns the directory which contains the given class.
     */
    private static Path directoryOf(final Class<?> member) throws URISyntaxException {
        return Path.of(member.getResource(member.getSimpleName() + ".class").toURI()).getParent();
    }

    /**
     * Generates the embedded resources in the {@code target/classes} directory if it does not already exists.
     * See class Javadoc for more information.
     *
     * @throws Exception if a failure occurred while searching directories,
     *         executing <abbr>SQL</abbr> scripts, copying data or any other operation.
     */
    final void createIfAbsent() throws Exception {
        if (dataSource != null) {
            copyLicenseFiles();
            createMetadata();
            createEPSG();
            compress();
            shutdown();
        }
    }

    /**
     * Copies the <abbr>EPSG</abbr> terms of use from the {@code sis-epsg} module to this {@code sis-embedded-data} module.
     * If the <abbr>EPSG</abbr> data are not found, then this method does nothing.
     *
     * <p>We copy those files ourselves instead of relying on {@code maven-resources-plugin}
     * because a future version may combine more licenses in a single file.</p>
     */
    private void copyLicenseFiles() throws URISyntaxException, IOException {
        final String[] files = {"LICENSE.txt", "LICENSE.html"};
        for (String file : files) {
            final Path source = sourceEPSG.resolve(file);
            if (Files.exists(source)) {
                final Path target = classesDirectory.resolve(file);
                if (Files.notExists(target)) {
                    Files.createLink(target, source);
                }
            }
        }
    }

    /**
     * Creates the metadata database schema.
     *
     * @throws FactoryException  if an error occurred while creating or querying the database.
     */
    private void createMetadata() throws MetadataStoreException, ReflectiveOperationException {
        try (var md = new MetadataSource(MetadataStandard.ISO_19115, dataSource, "metadata", null)) {
            Method install = md.getClass().getDeclaredMethod("install");
            install.setAccessible(true);
            install.invoke(md);
        }
    }

    /**
     * Creates the <abbr>EPSG</abbr> database schema.
     * This method does nothing if the <abbr>EPSG</abbr> are not available.
     * In the latter case, only the database will contain only free metadata.
     *
     * @throws FactoryException  if an error occurred while creating or querying the database.
     * @return whether the <abbr>EPSG</abbr> data were found.
     */
    private boolean createEPSG() throws FactoryException {
        if (Files.notExists(sourceEPSG.resolve("Data.sql"))) {
            return false;
        }
        final var properties = new HashMap<String,Object>();
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
        return true;
    }

    /**
     * Compresses all tables in all schema. Compression can save space if there was many update
     * or delete operations in the database. In the case of the database generated by this class,
     * the benefit is very small because the database is fresh. But it is still non-zero.
     */
    private void compress() throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            final var tables = new ArrayList<String>(80);  // As (schema,table) pairs.
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
