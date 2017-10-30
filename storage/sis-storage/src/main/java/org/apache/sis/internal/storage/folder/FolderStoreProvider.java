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
package org.apache.sis.internal.storage.folder;

import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystemNotFoundException;
import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.IllegalOpenParameterException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.setup.OptionKey;


/**
 * The provider of {@link Store} instances. This provider is intentionally <strong>not</strong> registered
 * in {@code META-INF/services/org.apache.sis.storage.DataStoreProvider} because is will open any directory,
 * which may conflict with other providers opening only directory with some specific content.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class FolderStoreProvider extends DataStoreProvider {
    /**
     * A short name or abbreviation for the data format.
     */
    private static final String NAME = "folder";

    /**
     * Description of the parameter for formating conventions of dates and numbers.
     */
    private static final ParameterDescriptor<Locale> LOCALE;

    /**
     * Description of the parameter for timezone of dates in the data store.
     */
    private static final ParameterDescriptor<TimeZone> TIMEZONE;

    /**
     * Description of the parameter for character encoding used by the data store.
     */
    private static final ParameterDescriptor<Charset> ENCODING;

    /**
     * The group of parameter descriptors to be returned by {@link #getOpenParameters()}.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterDescriptor<Path> location;
        final ParameterBuilder builder = new ParameterBuilder();
        final InternationalString remark = Resources.formatInternational(Resources.Keys.UsedOnlyIfNotEncoded);
        LOCALE     = builder.addName("locale"  ).setDescription(Resources.formatInternational(Resources.Keys.DataStoreLocale  )).setRemarks(remark).create(Locale.class,   null);
        TIMEZONE   = builder.addName("timezone").setDescription(Resources.formatInternational(Resources.Keys.DataStoreTimeZone)).setRemarks(remark).create(TimeZone.class, null);
        ENCODING   = builder.addName("encoding").setDescription(Resources.formatInternational(Resources.Keys.DataStoreEncoding)).setRemarks(remark).create(Charset.class,  null);
        location   = builder.addName( LOCATION ).setDescription(URIDataStore.Provider.LOCATION_PARAM.getDescription())          .setRequired(true) .create(Path.class,     null);
        PARAMETERS = builder.addName( NAME     ).createGroup(location, LOCALE, TIMEZONE, ENCODING);
    }

    /**
     * The unique instance of this provider.
     *
     * @see #open(Path)
     */
    public static final FolderStoreProvider INSTANCE = new FolderStoreProvider();

    /**
     * Creates a new provider.
     */
    private FolderStoreProvider() {
    }

    /**
     * Returns a short name or abbreviation for the data format.
     *
     * @return a short name or abbreviation for the data format.
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * Returns a description of all parameters accepted by this provider for opening a data store.
     *
     * @return description of the parameters for opening a {@link DataStore}.
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS;
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be a folder.
     * Returning {@code SUPPORTED} from this method does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the storage
     * header.
     *
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable as a folder.
     * @throws DataStoreException if an I/O or SQL error occurred.
     */
    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        try {
            final Path path = connector.getStorageAs(Path.class);
            if (path != null && Files.isDirectory(path)) {
                return ProbeResult.SUPPORTED;
            }
        } catch (FileSystemNotFoundException e) {
            Logging.recoverableException(Logging.getLogger(Modules.STORAGE), FolderStoreProvider.class, "probeContent", e);
            // Nothing we can do, may happen often.
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Returns a data store implementation associated with this provider.
     *
     * @param  connector  information about the storage (URL, path, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        try {
            return new Store(this, connector);
        } catch (IOException e) {
            throw new DataStoreException(Resources.format(Resources.Keys.CanNotReadDirectory_1,
                    connector.getStorageName()), e);
        }
    }

    /**
     * Returns a data store implementation associated with this provider for the given parameters.
     *
     * @return a folder data store implementation for the given parameters.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final ParameterValueGroup parameters) throws DataStoreException {
        ArgumentChecks.ensureNonNull("parameter", parameters);
        ParameterNotFoundException cause = null;
        try {
            final Object location = parameters.parameter(LOCATION).getValue();
            if (location != null) {
                final Parameters pg = Parameters.castOrWrap(parameters);
                final StorageConnector connector = new StorageConnector(location);
                connector.setOption(OptionKey.LOCALE,   pg.getValue(LOCALE));
                connector.setOption(OptionKey.TIMEZONE, pg.getValue(TIMEZONE));
                connector.setOption(OptionKey.ENCODING, pg.getValue(ENCODING));
                return open(connector);
            }
        } catch (ParameterNotFoundException e) {
            cause = e;
        }
        throw new IllegalOpenParameterException(Resources.format(Resources.Keys.UndefinedParameter_2,
                getShortName(), LOCATION), cause);
    }

    /**
     * Returns a folder data store for the given path.
     *
     * @param  path  the directory for which to create a data store.
     * @return a data store for the given directory.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    public static DataStore open(final Path path) throws DataStoreException {
        return INSTANCE.open(new StorageConnector(path));
    }
}
