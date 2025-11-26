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

import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.base.URIDataStoreProvider;
import org.apache.sis.metadata.sql.internal.shared.SQLUtilities;
import org.apache.sis.metadata.sql.internal.shared.Reflection;
import org.apache.sis.io.stream.InternalOptionKey;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Version;
import org.apache.sis.setup.OptionKey;


/**
 * The provider of {@link GpkgStore} instances.
 * Given a {@link StorageConnector} input, this class tries to instantiate a {@code GpkgProvider}.
 * the storage can be a {@link DataSource} or an object convertible to a {@link Path}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@StoreMetadata(formatName    = GpkgStoreProvider.NAME,
               fileSuffixes  = {"gpkg"},
               capabilities  = {Capability.READ, Capability.WRITE, Capability.CREATE},
               resourceTypes = {Aggregate.class, FeatureSet.class, GridCoverageResource.class})
public class GpkgStoreProvider extends DataStoreProvider {
    /**
     * The format name.
     */
    static final String NAME = "Geopackage";

    /**
     * The MIME type for Geopackage files.
     * Source: <a href="https://www.iana.org/assignments/media-types/media-types.xhtml">IANA</a>.
     */
    private static final String MIME_TYPE = "application/geopackage+sqlite3";

    /**
     * The SLite signature as long integers. The SQLite signature is the "SQLite format 3" string
     * followed by null terminator (0), for a total of 16 bytes. We compare those bytes as two long
     * integers with big endian byte order.
     */
    static final long SIG1 = 0x53514c6974652066L,
                      SIG2 = 0x6f726d6174203300L;

    /**
     * The application identifier. The value is "GPKG" in ASCII, big-endian byte order.
     * This identifier is located at offset {@value #APPLICATION_ID_OFFSET}.
     */
    static final int APPLICATION_ID = 0x47504B47;

    /**
     * Offset where the application identifier or version is specified.
     */
    private static final int APPLICATION_ID_OFFSET = 68, VERSION_OFFET = 60;

    /**
     * The version written by the Geopackage store.
     * For example, {@code 10200} stands for 1.02.00 and {@code 10300} stands for 1.03.00.
     */
    static final int VERSION = 10400;

    /**
     * The logger used by Geopackage stores.
     *
     * @see #getLogger()
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.geopackage");

    /**
     * Name of an optional parameter for specifying PRAGMA statements as a {@code String}.
     * <a href="https://www.sqlite.org/pragma.html">PRAGMA statements</a> are a SQLite-specific
     * way to configure the database. Examples:
     *
     * <ul>
     *   <li>{@code SYNCHRONOUS=off;JOURNAL_MODE=off;} for single database creation in a single access context.</li>
     *   <li>{@code SECURE_DELETE=off;} for faster delete operation on slow hard drives.</li>
     * </ul>
     */
    public static final String PRAGMAS = "PRAGMAs";

    /**
     * The descriptor for the {@link #PRAGMAS} parameter.
     */
    private static final ParameterDescriptor<CharSequence> PRAGMAS_PARAM;

    /**
     * The parameter descriptor to be returned by {@link #getOpenParameters()}.
     */
    private static final ParameterDescriptorGroup OPEN_DESCRIPTOR;
    static {
        final var builder = new ParameterBuilder();
        PRAGMAS_PARAM = builder.addName(PRAGMAS)
                .setDescription("PRAGMA statements encoded as \"name=value;name=value\".")
                .setRemarks("See https://www.sqlite.org/pragma.html for a list of PRAGMAs.")
                .create(CharSequence.class, null);
        OPEN_DESCRIPTOR = builder.addName(NAME).createGroup(URIDataStoreProvider.LOCATION_PARAM, PRAGMAS_PARAM);
    }

    /**
     * Creates a new provider.
     */
    public GpkgStoreProvider() {
    }

    /**
     * Returns a generic name for this data store, used mostly in warnings or error messages.
     *
     * @return a short name or abbreviation for the data format.
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * Returns a description of all parameters accepted by this provider for opening a Geopackage file.
     * The group contains parameters named {@value #LOCATION} and {@value #PRAGMAS}.
     *
     * @return description of available parameters for opening a Geopackage file.
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return OPEN_DESCRIPTOR;
    }

    /**
     * Returns the MIME type if the given storage appears to be supported by {@link GpkgStore}.
     * This method recognizes the following storage type:
     *
     * <ul>
     *   <li>{@link DataSource}: accepted if the database contains a {@value Content#TABLE_NAME} table
     *       with at least the mandatory columns.</li>
     *   <li>{@link Path}: accepted if the file contains the header signature of a Geopackage file and
     *       the application identifier is "GPKG".</li>
     * </ul>
     *
     * @param  connector  information about the storage (file, <i>etc</i>).
     * @return a {@linkplain ProbeResult#isSupported() supported} status with the MIME type
     *         if the given storage seems to be readable by {@code GpkgStore} instances.
     * @throws DataStoreException if an I/O or SQL error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        /*
         * If the storage is a data source, verifies that the database
         * contains a contents table with at least the mandatory columns.
         */
        final DataSource source = connector.getStorageAs(DataSource.class);
        if (source != null) {
            try (Connection connection = source.getConnection()) {
                final DatabaseMetaData metadata = connection.getMetaData();
                final String table = SQLUtilities.escapeWildcards(Content.TABLE_NAME, metadata.getSearchStringEscape());
                try (ResultSet r = metadata.getColumns(null, null, table, "%")) {
                    boolean hasTable = false, hasType = false;
                    while (r.next()) {
                        final String column = r.getString(Reflection.COLUMN_NAME);
                        hasTable |= Content.PRIMARY_KEY.equalsIgnoreCase(column);
                        hasType  |= Content.DATA_TYPE  .equalsIgnoreCase(column);
                        if (hasTable & hasType) {
                            return new ProbeResult(true, MIME_TYPE, null);
                        }
                    }
                }
            } catch (Exception e) {
                throw GpkgStore.cannotExecute(null, e);
            }
            return ProbeResult.UNSUPPORTED_STORAGE;
        }
        /*
         * Otherwise, the storage shall be a file. Check the file signature and the application identifier.
         * We do not open the database, because checking the application identifier should be sufficient.
         * The byte order shall be big-endian according the SQLite specification.
         */
        return probeContent(connector, ByteBuffer.class, (buffer) -> {
            if (buffer.remaining() < APPLICATION_ID_OFFSET + Integer.BYTES) {
                return ProbeResult.INSUFFICIENT_BYTES;
            }
            if (buffer.getLong() == SIG1 && buffer.getLong() == SIG2) {
                int offset = buffer.position() + (APPLICATION_ID_OFFSET - 2*Long.BYTES);
                if (buffer.getInt(offset) == APPLICATION_ID) {
                    offset += (VERSION_OFFET - APPLICATION_ID_OFFSET);
                    int major = buffer.getInt(offset);
                    int minor = major % 10000; major /= 10000;
                    int fix   = minor %   100; minor /=   100;
                    return new ProbeResult(true, MIME_TYPE, Version.valueOf(major, minor, fix));
                }
            }
            return ProbeResult.UNSUPPORTED_STORAGE;
        });
    }

    /**
     * Creates a {@link GpkgStore} implementation associated with this provider.
     * The specified storage should be a file or {@link URI}, but this method accepts also {@link DataSource}.
     * This flexibility allows connections to database products other than SQLite, but using the same tables
     * as specified in the Geopackage standard.
     *
     * @param  connector  information about the storage (file, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        return new GpkgStore(this, connector);
    }

    /**
     * Returns a data store implementation associated with this provider for the given parameters.
     *
     * @param  parameters  opening parameters as defined by {@link #getOpenParameters()}.
     * @return a data store implementation associated with this provider for the given parameters.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final ParameterValueGroup parameters) throws DataStoreException {
        final StorageConnector connector = URIDataStoreProvider.connector(this, Objects.requireNonNull(parameters));
        final CharSequence pragmas = Parameters.castOrWrap(parameters).getValue(PRAGMAS_PARAM);
        if (pragmas != null) {
            final var map = new HashMap<String,String>();
            for (CharSequence entry : CharSequences.split(pragmas, ';')) {
                CharSequence[] split = CharSequences.split(entry, '=');
                switch (split.length) {
                    case 0: break;
                    case 2: map.put(split[0].toString(), split[1].toString()); break;
                    default: throw new IllegalArgumentException("Invalid pragma parameters, must be in the \"name=value;name=value\" form.");
                }
            }
            connector.setOption(InternalOptionKey.PRAGMAS, map);
            connector.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {StandardOpenOption.WRITE});
        }
        return open(connector);
    }

    /**
     * Returns the logger used by Geopackage stores.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
