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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.ShardingKey;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import javax.sql.DataSource;
import org.apache.sis.metadata.sql.privy.ScriptRunner;
import org.apache.sis.storage.base.ResourceOnFileSystem;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.storage.geopackage.privy.Query;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.storage.tiling.TileMatrix;
import org.apache.sis.util.iso.Names;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.Pragma;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;
import org.apache.sis.storage.geopackage.privy.Record;

/**
 * Geopackage data store.
 * <p>
 * List of pragma can be found at : <a href="https://www.sqlite.org/pragma.html">https://www.sqlite.org/pragma.html</a>
 * <p>
 * Note, after experimentation to improve database creation speed in a single access context
 * the following pragma give excellent results :
 * SYNCHRONOUS=off;JOURNAL_MODE=off;
 * Following one do not work with Hikari connexion poll.
 * LOCKING_MODE=exclusive;
 * Following one improves delete operation on slow hard drives
 * SECURE_DELETE=off;
 *
 * @author Johann Sorel (Geomatys)
 */
public class GpkgStore extends DataStore implements WritableAggregate, ResourceOnFileSystem {

    /**
     * Sort mosaics by scales, biggest scales first (high level in the pyramids).
     */
    public static final Comparator<TileMatrix> MOSAIC_SCALE_COMPARATOR = new Comparator<TileMatrix>() {
        @Override
        public int compare(TileMatrix tm1, TileMatrix tm2) {
            final double[] res1 = tm1.getTilingScheme().getResolution(true);
            final double[] res2 = tm2.getTilingScheme().getResolution(true);
            for (int i = 0; i < res1.length; i++) {
                int cmp = Double.compare(res2[i], res1[i]);
                if (cmp != 0) return cmp;
            }
            return 0;
        }
    };

    /**
     * Synchronization object used for accessing datasource.
     * we do not use 'synchronized' on the method since writing may use thread-pools.
     */
    private final Object SYNC_DATASOURCE = new Object();

    private final Path path;
    private final GenericName name;
    private DataSource dataSource;
    private final Map<Integer,CoordinateReferenceSystem> CRS_CACHE = new HashMap<>();
    private List<Resource> components = null;

    /**
     * We need a read/write lock for sqlite.
     * The connection pool is not a real pool.
     * Sqlite has good support for concurrent read operations but has troubles
     * when write operations are implied.
     * So SQLite busy exception may pop up.
     * To avoid such errors we handle the write locks ourselves.
     */
    private final ReadWriteLock ioLock = new ReentrantReadWriteLock();

    /*
     * Todo : move this configuration somewhere more suitable
     */
    private boolean coverageLzwCompression = true;
    /*
     * Todo : move this configuration somewhere more suitable
     */
    private String rgbFormatName = "png";

    /**
     * Pragma parameters to use at gpkg opening.
     */
    private final Map<String,String> pragmas = new HashMap<>();

    /**
     * Open a Geopackage database.
     *
     * @param path Path to geopackage file
     * @throws DataStoreException if Geopackage tables initialisation failed
     */
    public GpkgStore(Path path) throws DataStoreException {
        this(path, "");
    }

    /**
     * Open a Geopackage database.
     *
     * @param path Path to geopackage file
     * @param pragmas pragma parameters encoded in the form 'name=value;name=value'
     * @throws DataStoreException if Geopackage tables initialisation failed
     */
    public GpkgStore(Path path, String pragmas) throws DataStoreException {
        this(path, splitPragmas(pragmas));
    }

    /**
     * Open a Geopackage database.
     *
     * @param params creation parameters
     * @throws DataStoreException if Geopackage tables initialisation failed
     */
    public GpkgStore(ParameterValueGroup params) throws DataStoreException {
        this(Paths.get(Parameters.castOrWrap(params).getMandatoryValue(GpkgProvider.PATH)),
             Parameters.castOrWrap(params).getValue(GpkgProvider.PRAGMAS)
        );
    }

    /**
     * Open a Geopackage database.
     *
     * @param path Path to geopackage file
     * @param pragmas Map of pragma parameters to apply at database opening.
     * @throws DataStoreException if Geopackage tables initialisation failed
     */
    public GpkgStore(Path path, Map<String,String> pragmas) throws DataStoreException {
        this.path = path;
        if (pragmas != null) {
            this.pragmas.putAll(pragmas);
        }
        if (!Files.exists(path)) {
            initializeGeopackage();
        }
        this.name = Names.createLocalName(null, null, IOUtilities.filename(path));
    }

    /**
     * @return geopackage path
     */
    Path getPath() {
        return path;
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.ofNullable(name);
    }

    /**
     * Split pragma parameters encoded in the form 'name=value;name=value'
     * @param str, may be null
     * @return Map of pragmas, never null
     */
    private static Map<String,String> splitPragmas(String str) {
        if (str == null) return Collections.emptyMap();
        final Map<String,String> map = new HashMap<>();
        for (String entry : str.split(";")) {
            entry = entry.trim();
            if (!entry.isEmpty()) {
                final String[] split = entry.split("=");
                if (split.length != 2) throw new IllegalArgumentException("Invalid pragma parameters, must be in the form : 'name=value;name=value'");
                map.put(split[0], split[1]);
            }
        }
        return map;
    }

    @Override
    public DataStoreProvider getProvider() {
        return GpkgProvider.provider();
    }

    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        final Parameters params = Parameters.castOrWrap(GpkgProvider.PARAMETERS_DESCRIPTOR.createValue());
        params.getOrCreate(GpkgProvider.PATH).setValue(path.toUri());
        return Optional.of(params);
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return new DefaultMetadata();
    }

    /**
     * Define if LZW compression should be used when writing tiff tiles.
     *
     * @param coverageLzwCompression true to use LZW compression in tiff blobs.
     */
    public void setCoverageLzwCompression(boolean coverageLzwCompression) {
        this.coverageLzwCompression = coverageLzwCompression;
    }

    /**
     * @return true if LZW compression is active when writing tiles.
     */
    public boolean getCoverageLzwCompression() {
        return coverageLzwCompression;
    }

    /**
     * Define format name that should be used when writing rgb tiles.
     *
     * @param rgbFormatName
     */
    public void setCoverageRgbFormat(String rgbFormatName) {
        ArgumentChecks.ensureNonNull("rgbFormatName", rgbFormatName);
        this.rgbFormatName = rgbFormatName;
    }

    /**
     *
     * @return rgb tile image format name.
     */
    public String getCoverageRgbFormat() {
        return rgbFormatName;
    }

    /**
     * Get a connection to the database.
     */
    public Connection getConnection(boolean write) throws DataStoreException, SQLException {
        try {
            Connection cnx = getDataSource().getConnection();
            cnx.setAutoCommit(false);
            return new ReadWriteConnection(cnx, write);
        } catch (SQLException ex) {
            throw new SQLException("Failed to create connection on database " + path.toUri(), ex);
        }
    }

    public DataSource getDataSource() throws DataStoreException {
        synchronized (SYNC_DATASOURCE) {
            if (dataSource == null) {
                final boolean newDb = !Files.exists(path);
                final String fileName = path.getFileName().toString();

                /*
                Detect if database is immutable
                Sqlite makes a difference between read-only and immutable.
                In Read-only the driver may still create and use the -wal and -shm files.
                In immutable he is not allowed to touch or create anything.
                https://www.sqlite.org/c3ref/open.html

                For some strange reason EVEN with writing permissions Sqlite may
                fail to open a connection with obscur exception : [SQLITE_CANTOPEN] Unable to open the database file (unable to open database file)
                BUT if we create the -wal and -shm empty files, it will work.
                https://www.sqlite.org/wal.html#read_only_databases

                Note : disabling WAL journal or forcing it to Memory does not solve it either.
                 */
                boolean immutable;
                if (newDb) {
                    immutable = false;
                } else {
                    final Path wal = path.getParent().resolve(fileName+"-wal");
                    final Path shm = path.getParent().resolve(fileName+"-shm");
                    if (Files.exists(wal) || Files.exists(shm)) {
                        immutable = false;
                    } else {
                        try {
                            //see if we can create the wal/shm files
                            Files.write(wal, new byte[0], StandardOpenOption.CREATE);
                            Files.write(shm, new byte[0], StandardOpenOption.CREATE);
                            immutable = false;
                        } catch (IOException ex) {
                            immutable = true;
                        }
                    }
                }

                final boolean isReadOnly = immutable || (!newDb && !Files.isWritable(path));

                final String url;
                if (immutable) {
                    url = "jdbc:sqlite:" + path.toAbsolutePath().toFile().toURI().toString().replace('\\', '/') + "?immutable=1";
                } else {
                    url = "jdbc:sqlite:" + path.toAbsolutePath().toFile().toString().replace('\\', '/');
                }
                final SQLiteConfig config = new SQLiteConfig();
                //config.setSharedCache(true);
                //config.setBusyTimeout(60000);
                config.setBusyTimeout(Integer.MAX_VALUE);
                config.setCacheSize(100000);
                if (!isReadOnly) {
                    // config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
                    config.setJournalMode(SQLiteConfig.JournalMode.WAL);

                    //TODO need to find a list of pragma not causing errors in readonly.
                    for (Entry<String,String> entry : pragmas.entrySet()) {
                        if (GpkgProvider.PRAGMA_HIKARICP.equalsIgnoreCase(entry.getKey())) continue;
                        config.setPragma(Pragma.valueOf(entry.getKey()), entry.getValue());
                    }
                } else {
//                    config.setReadOnly(true);
                }

                final String useHikariValue = pragmas.remove(GpkgProvider.PRAGMA_HIKARICP);
                final boolean useHikari = ("1".equalsIgnoreCase(useHikariValue) || "true".equalsIgnoreCase(useHikariValue));

                final DataSource dataSource;
                if (useHikari) {
                    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
                    /*
                    We use hikari instead of apache dbcp2 because of spring-boot excluding commons-logging
                    Also because hikari is still in activity and from benchmarks has better performances.
                    */
                    HikariConfig hkcfg = new HikariConfig();
                    hkcfg.setPoolName(UUID.randomUUID().toString());
                    hkcfg.setDriverClassName("org.sqlite.JDBC");
                    hkcfg.setJdbcUrl(url);
                    hkcfg.setConnectionTestQuery("SELECT 1");
                    hkcfg.setMaxLifetime(60000); // 60 Sec
                    hkcfg.setIdleTimeout(45000); // 45 Sec
                    //hkcfg.setConnectionTimeout(60000); // 1 min
                    hkcfg.setMaximumPoolSize(50); // 50 Connections (including idle connections)
                    hkcfg.setLeakDetectionThreshold(10000);
//                    hkcfg.setReadOnly(isReadOnly);

                    final Properties sqliteprops = config.toProperties();
                    for (Entry<Object,Object> entry : sqliteprops.entrySet()) {
                        hkcfg.addDataSourceProperty(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                    }
                    dataSource = new HikariDataSource(hkcfg);

//                    //normaly SQlite do not support more then one connection
//                    //or we may obtain errors such as : [SQLITE_BUSY] The database file is locked (database is locked)
//                    //to workaround this limitation it is possible to add a busy timeout :
//                    //https://stackoverflow.com/questions/8559623/sqlite-busy-the-database-file-is-locked-database-is-locked-in-wicket
//                    //dataSource.setConnectionInitSqls(Arrays.asList(
//                    //"PRAGMA busy_timeout=60000;"));
                } else {
                    final SQLiteConnectionPoolDataSource sds = new SQLiteConnectionPoolDataSource(config);
                    sds.setUrl(url);
                    dataSource = sds;
                }

                if (newDb) {
                    try (Connection cnx = dataSource.getConnection()) {
                        cnx.setAutoCommit(false);
                        Query.execute("PRAGMA main.application_id = 0x47504B47;", cnx); //"GPKG" in ASCII
                        Query.execute("PRAGMA main.user_version = 0x000027D8;", cnx); //hexadecimal value for 10200
                        cnx.commit();
                    } catch (SQLException ex) {
                        throw new DataStoreException(ex.getMessage(), ex);
                    }
                }

                this.dataSource = dataSource;
            }
            return dataSource;
        }
    }

    SQLiteConfig getSQLiteConfig() throws DataStoreException {
        getDataSource();

        //use the sqllite data source to load configuration
        final String url = "jdbc:sqlite:" + path.toAbsolutePath().toFile().toString().replace('\\', '/');
        final SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        SQLiteConnectionPoolDataSource ds = new SQLiteConnectionPoolDataSource(config);
        ds.setUrl(url);
        return ds.getConfig();
    }

    @Override
    public synchronized Collection<Resource> components() throws DataStoreException {
        if (components == null) {
            final List<Resource> components = new ArrayList<>();

            try {
                //check if the content table exist
                //databse may be a sqlitedb but not a geopackage

                final boolean contentsExist;
                try (Connection cnx = getConnection(false);
                     Statement stmt = cnx.createStatement();
                     ResultSet rs = stmt.executeQuery(Query.CONTENTS_EXIST.query())) {
                    contentsExist = rs.getInt(1) != 0;
                }

                if (!contentsExist) {
                    Gpkg.LOGGER.log(Level.INFO, "Given database do not have a gpkg_contents table, it is not a geopackage");
                } else {

                    final List<Record.Content> contents = new ArrayList<>();
                    try (Connection cnx = getConnection(false);
                         Statement stmt = cnx.createStatement();
                         ResultSet rs = stmt.executeQuery(Query.CONTENTS_ALL.query())) {
                        while (rs.next()) {
                            final Record.Content row = new Record.Content();
                            row.read(rs);
                            contents.add(row);
                        }
                    }

                    //load all content as resources
                    contentLoop:
                    for (Record.Content content : contents) {
                        for (GpkgContentHandler handler : Gpkg.CONTENT_HANDLERS) {
                            if (handler.canOpen(content)) {
                                components.add(handler.open(this, content));
                                continue contentLoop;
                            }
                        }
                        //no handler found
                        components.add(new GpkgUndefinedResource(this, content));
                    }

                }

            } catch (SQLException ex) {
                throw new DataStoreException(ex.getMessage(), ex);
            }

            this.components = components;
        }

        return Collections.unmodifiableList(components);
    }

    @Override
    public synchronized Resource add(Resource resource) throws DataStoreException {
        final Optional<GenericName> identifier = resource.getIdentifier();
        if (!identifier.isPresent()) {
            throw new DataStoreException("Resource has no identifier");
        }
        final String id = identifier.get().tip().toString();

        //search if this resource already exist
        try {
            findResource(id);
            throw new DataStoreException("A resource with name " + id +" already exist.");
        } catch (IllegalNameException ex) {
            //does not exist ok
        }

        if (!(resource instanceof DataSet)) {
            throw new DataStoreException("Resource is not supported, it is not a DataSet");
        }

        final DataSet dataSet = (DataSet) resource;

        //create content instance
        final Record.Content content = new Record.Content();
        content.identifier = id;
        content.tableName = id;
        content.description = "";
        content.lastChange = Calendar.getInstance();
        final Optional<Envelope> envelope = dataSet.getEnvelope();
        if (envelope.isPresent()) {
            final Envelope env = envelope.get();
            try {
                content.srsId = getOrCreateCRS(env.getCoordinateReferenceSystem());
            } catch (SQLException ex) {
                throw new DataStoreException("Unsupported CRS", ex);
            }
            content.minX = env.getMinimum(0);
            content.minY = env.getMinimum(1);
            content.maxX = env.getMaximum(0);
            content.maxY = env.getMaximum(1);
        }

        Resource created = null;
        for (GpkgContentHandler handler : Gpkg.CONTENT_HANDLERS) {
            if (handler.canAdd(resource)) {
                created = handler.add(this, content, resource);
                components.add(created);
                break;
            }
        }

        if (created == null) {
            throw new DataStoreException("Unsupported resource type");
        }

        return created;
    }

    public synchronized void saveContentRecord(Record.Content content) throws DataStoreException {
        ArgumentChecks.ensureNonNull("content", content);
        ArgumentChecks.ensureNonEmpty("content.tableName", content.tableName);

        //force loading curent resources
        components();

        try (Connection cnx = getConnection(true)) {
            try {
                content.create(cnx);
                cnx.commit();
            } catch (Exception e) {
                cnx.rollback();
                throw e;
            }
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    public synchronized void clearCache() {
        //clear cache
        components = null;
    }

    @Override
    public void remove(Resource resource) throws DataStoreException {
        if (!components.contains(resource)) {
            throw new DataStoreException("Given resource is not from this store");
        }

        if (resource instanceof GpkgUndefinedResource) {
            throw new DataStoreException("Given resource type " + ((GpkgUndefinedResource)resource).row.dataType + " is not supported");
        } else if (resource instanceof GpkgContentResource) {
            final GpkgContentResource cdt = (GpkgContentResource) resource;
            cdt.getHandler().delete(cdt);
            this.components.remove(cdt);
        } else {
            throw new DataStoreException("Unexpected resource");
        }
    }

    /**
     * Get CoordinateReferenceSystem for srid.
     *
     * Specification :
     * [K11] The axis order in WKB is always (x,y{,z}{,m}) where
     * x is easting or longitude,
     * y is northing or latitude,
     * z is optional elevation and
     * m is optional measure.
     *
     * @param srid geopackage SRS id
     * @return decoded CRS, never null
     */
    synchronized CoordinateReferenceSystem toCRS(Connection cnx, int srid) throws SQLException, DataStoreException {
        CoordinateReferenceSystem crs = CRS_CACHE.get(srid);
        if (crs != null) return crs;

        if (srid == -1) {
            //TODO Undefined cartesian SRS, use lon/lat for now
            crs = CommonCRS.WGS84.normalizedGeographic();
        } else if (srid == 0) {
            //Undefined geographic SRS
            try {
                crs = CRS.forCode("EPSG:4030");
            } catch (FactoryException ex) {
                throw new DataStoreException(ex.getMessage(), ex);
            }
        } else {

            ioLock.readLock().lock();
            search:
            try (PreparedStatement stmt = Query.SPATIAL_REF_BY_SRID.createPreparedStatement(cnx, srid);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    //check new WKT definition
                    try {
                        String def2 = rs.getString("definition_12_063");
                        if (def2 != null && !def2.trim().isEmpty() && !def2.equalsIgnoreCase("undefined")) {
                            try {
                                crs = CRS.fromWKT(def2);
                                break search;
                            } catch (FactoryException ex) {
                                Gpkg.LOGGER.log(Level.WARNING, "Failed to parse WKT definition : "+def2+"\n"+ex.getMessage(),ex);
                            }
                        }
                    } catch (SQLException ex) {
                        //fallback on definition field
                    }

                    //check old WKT definition
                    String def1 = rs.getString("definition");
                    if (def1 != null && !def1.trim().isEmpty() && !def1.equalsIgnoreCase("undefined")) {
                        try {
                            crs = CRS.fromWKT(def1);
                            break search;
                        } catch (FactoryException ex) {
                            Gpkg.LOGGER.log(Level.WARNING, "Failed to parse WKT definition : "+def1+"\n"+ex.getMessage(),ex);
                        }
                    }

                    //check authority code
                    final String organization = rs.getString("organization");
                    final int organizationCode = rs.getInt("organization_coordsys_id");
                    try {
                        crs = CRS.forCode(organization+":"+organizationCode);
                        break search;
                    } catch (FactoryException ex) {
                        Gpkg.LOGGER.log(Level.WARNING, "Failed to parse authority identifier : "+organization+":"+organizationCode+"\n"+ex.getMessage(),ex);
                    }
                }
            } finally {
                ioLock.readLock().unlock();
            }
        }

        if (crs != null) {
            crs = AbstractCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);
            CRS_CACHE.put(srid, crs);
            return crs;
        } else {
            throw new DataStoreException("SRID "+srid+" not found in " + Gpkg.TABLE_SPATIAL_REF_SYS + " table.");
        }
    }

    /**
     * Find or insert a new CRS definition in database.
     * Returns the create crs srid.
     *
     * @param crs
     * @return CRS srid.
     */
    public synchronized int getOrCreateCRS(CoordinateReferenceSystem crs) throws SQLException, DataStoreException {

        //force longitude first.
        final CoordinateReferenceSystem crs2 = ((AbstractCRS)crs).forConvention(AxesConvention.RIGHT_HANDED);
        if (crs != crs2) {
            throw new DataStoreException("Only longitude first CRS are supported in geopackage");
        }

        Integer srsId = null;
        String srsName;
        String organisation = "";
        Integer organisationCode = 0;
        String description = "";

        srsName = crs.getName().getCode();
        final Identifier identifier = IdentifiedObjects.getIdentifier(crs, null);

        if (identifier != null) {
            Collection<? extends Identifier> identifiers = identifier.getAuthority().getIdentifiers();
            if (!identifiers.isEmpty()) {
                organisation = identifiers.iterator().next().getCode();
                organisationCode = Integer.valueOf(identifier.getCode());

                //search if this CRS exist
                srsId = getSRID(organisation, organisationCode);
                if (srsId != null) return srsId;
            }
        }

        //see if we can reuse organisation code
        if ("epsg".equalsIgnoreCase(organisation)) {
            try (Connection cnx = getConnection(false)) {
                toCRS(cnx, organisationCode);
            } catch (DataStoreException ex) {
                //code isn't used
                srsId = organisationCode;
            }
        }

        if (srsId == null) {
            //next available srsid in user reserved space from 32768 to 60000000
            try (Connection cnx = getConnection(false);
                 PreparedStatement stmt = Query.SPATIAL_REF_NEXT_SRID.createPreparedStatement(cnx);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    srsId = rs.getInt(1);
                    if (rs.wasNull()) {
                        srsId = 32768;
                    } else {
                        srsId++;
                    }
                } else {
                    srsId = 32768;
                }
            }
        }

        final WKTFormat wktFormat = new WKTFormat(null, null);
        wktFormat.setConvention(Convention.WKT1);
        final String wkt1 = wktFormat.format(crs);
        wktFormat.setConvention(Convention.WKT2);
        final String wkt2 = wktFormat.format(crs);

        try (Connection cnx = getConnection(true)) {
            try (PreparedStatement stmt = Query.SPATIAL_REF_CREATE_EXT.createPreparedStatement(cnx, srsName, srsId, organisation, organisationCode, wkt1, description, wkt2)) {
                stmt.executeUpdate();
            } catch (SQLException ex) {
                cnx.rollback();
                try (PreparedStatement stmt = Query.SPATIAL_REF_CREATE.createPreparedStatement(cnx, srsName, srsId, organisation, organisationCode, wkt1, description)) {
                    stmt.executeUpdate();
                } catch (SQLException ex2) {
                    cnx.rollback();
                    throw new DataStoreException("Failed to insert new CRS,"+ ex.getMessage(), ex);
                }
            }
            cnx.commit();
        }

        return srsId;
    }

    private Integer getSRID(String organisation, Integer organisationCode) throws DataStoreException, SQLException {
        try (Connection cnx = getConnection(false);
             PreparedStatement stmt = Query.SPATIAL_REF_BY_ORGANIZATION.createPreparedStatement(cnx, organisation, organisationCode);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("srs_id");
            }
        }
        return null;
    }

    private void initializeGeopackage() throws DataStoreException {

        try (Connection cnx = getConnection(true)) {
            final String sqlCore = IOUtilities.toString(GpkgStore.class.getResourceAsStream("/org/apache/sis/storage/geopackage/Core.sql"));

            // TODO move this to an extension
            final String sqlWkt = IOUtilities.toString(GpkgStore.class.getResourceAsStream("/org/apache/sis/storage/geopackage/Extension WKT for Coordinate Reference Systems.sql"));

            final ScriptRunner runner = new ScriptRunner(cnx, 100);
            runner.run(sqlCore);
            runner.run(sqlWkt);
            runner.close();
            cnx.commit();

        } catch (IOException | SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }

        //install extensions
        for (GpkgContentHandler handler : Gpkg.CONTENT_HANDLERS) {
            for (GpkgExtension ext : handler.getExtensions(this)) {
                if (!ext.isInstalled()) {
                    ext.install();
                }
            }
        }
    }

    @Override
    public void close() throws DataStoreException {
        //SQLite data source has no dispose or close method
        //each connection instance must be properly closed
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception ex) {
                throw new DataStoreException(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public Path[] getComponentFiles() {
        return new Path[]{path};
    }

    /**
     * Execute a vacuum operation on the database.
     *
     * @throws DataStoreException
     */
    public void vacuum() throws DataStoreException {
        try (Connection cnx = getConnection(true);
             Statement stmt = cnx.createStatement()) {
            cnx.setAutoCommit(true);
            stmt.executeUpdate("VACUUM");
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    private class ReadWriteConnection implements Connection {

        private final Connection cnx;
        private final boolean write;

        public ReadWriteConnection(Connection cnx, boolean write) throws SQLException {
            if (write) {
                ioLock.writeLock().lock();
            } else {
                ioLock.readLock().lock();
            }
            this.cnx = cnx;
            this.write = write;
            //cnx.setReadOnly(!write); //no supported by sqlite
        }

        @Override
        public Statement createStatement() throws SQLException {
            return cnx.createStatement();
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return cnx.prepareStatement(sql);
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return cnx.prepareCall(sql);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return cnx.nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            cnx.setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return cnx.getAutoCommit();
        }

        @Override
        public void commit() throws SQLException {
            if (!write) {
                throw new SQLException("Connection has not been created with writing lock");
            }
            cnx.commit();
        }

        @Override
        public void rollback() throws SQLException {
            if (!write) {
                throw new SQLException("Connection has not been created with writing lock");
            }
            cnx.rollback();
        }

        @Override
        public void close() throws SQLException {
            final boolean closed = cnx.isClosed();
            cnx.close();
            if (!closed) {
                if (write) {
                    ioLock.writeLock().unlock();
                } else {
                    ioLock.readLock().unlock();
                }
            }
        }

        @Override
        public boolean isClosed() throws SQLException {
            return cnx.isClosed();
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return cnx.getMetaData();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            throw new SQLException("Should not be called");
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return cnx.isReadOnly();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            cnx.setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException {
            return cnx.getCatalog();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            cnx.setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return cnx.getTransactionIsolation();
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return cnx.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            cnx.clearWarnings();
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return cnx.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return cnx.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return cnx.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return cnx.getTypeMap();
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            cnx.setTypeMap(map);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            cnx.setHoldability(holdability);
        }

        @Override
        public int getHoldability() throws SQLException {
            return cnx.getHoldability();
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return cnx.setSavepoint();
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return cnx.setSavepoint(name);
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
            cnx.rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            cnx.releaseSavepoint(savepoint);
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return cnx.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return cnx.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return cnx.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return cnx.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return cnx.prepareStatement(sql, columnIndexes);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return cnx.prepareStatement(sql, columnNames);
        }

        @Override
        public Clob createClob() throws SQLException {
            return cnx.createClob();
        }

        @Override
        public Blob createBlob() throws SQLException {
            return cnx.createBlob();
        }

        @Override
        public NClob createNClob() throws SQLException {
            return cnx.createNClob();
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return cnx.createSQLXML();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return cnx.isValid(timeout);
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            cnx.setClientInfo(name, value);
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            cnx.setClientInfo(properties);
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return cnx.getClientInfo(name);
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return cnx.getClientInfo();
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return cnx.createArrayOf(typeName, elements);
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return cnx.createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            cnx.setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException {
            return cnx.getSchema();
        }

        @Override
        public void abort(Executor executor) throws SQLException {
            cnx.abort(executor);
        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
            cnx.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return cnx.getNetworkTimeout();
        }

        @Override
        public void beginRequest() throws SQLException {
            cnx.beginRequest();
        }

        @Override
        public void endRequest() throws SQLException {
            cnx.endRequest();
        }

        @Override
        public boolean setShardingKeyIfValid(ShardingKey shardingKey, ShardingKey superShardingKey, int timeout) throws SQLException {
            return cnx.setShardingKeyIfValid(shardingKey, superShardingKey, timeout);
        }

        @Override
        public boolean setShardingKeyIfValid(ShardingKey shardingKey, int timeout) throws SQLException {
            return cnx.setShardingKeyIfValid(shardingKey, timeout);
        }

        @Override
        public void setShardingKey(ShardingKey shardingKey, ShardingKey superShardingKey) throws SQLException {
            cnx.setShardingKey(shardingKey, superShardingKey);
        }

        @Override
        public void setShardingKey(ShardingKey shardingKey) throws SQLException {
            cnx.setShardingKey(shardingKey);
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return cnx.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return cnx.isWrapperFor(iface);
        }

    }
}
